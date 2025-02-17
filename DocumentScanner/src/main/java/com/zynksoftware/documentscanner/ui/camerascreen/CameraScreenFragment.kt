/**
Copyright 2020 ZynkSoftware SRL

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynksoftware.documentscanner.ui.camerascreen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.allShouldShowRationale
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.zynksoftware.documentscanner.R
import com.zynksoftware.documentscanner.common.extensions.hide
import com.zynksoftware.documentscanner.common.extensions.show
import com.zynksoftware.documentscanner.common.utils.FileUriUtils
import com.zynksoftware.documentscanner.databinding.FragmentCameraScreenBinding
import com.zynksoftware.documentscanner.model.DocumentScannerErrorModel
import com.zynksoftware.documentscanner.ui.base.BaseFragment
import com.zynksoftware.documentscanner.ui.components.scansurface.ScanSurfaceListener
import com.zynksoftware.documentscanner.ui.scan.InternalScanActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException


internal class CameraScreenFragment : BaseFragment(), ScanSurfaceListener {
    private var _binding: FragmentCameraScreenBinding? = null
    private val binding get() = _binding!!

    private val job = Job()  // Job to manage the coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)  // Create CoroutineScope

    companion object {
        private val TAG = CameraScreenFragment::class.simpleName

        fun newInstance(): CameraScreenFragment {
            return CameraScreenFragment()
        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    handleGalleryResult(result.data)
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "FileNotFoundException", e)
                    onError(
                        DocumentScannerErrorModel(
                            DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR, e
                        )
                    )
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scanSurfaceView.lifecycleOwner = this
        binding.scanSurfaceView.listener = this
        binding.scanSurfaceView.originalImageFile = getScanActivity().originalImageFile
        if (getScanActivity().galleryButtonEnabled) {
            binding.galleryButton.visibility = View.VISIBLE
        } else {
            binding.galleryButton.visibility = View.GONE
        }

        checkForCameraPermissions()
        initListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (getScanActivity().shouldCallOnClose) {
            getScanActivity().onClose()
        }
        job.cancel()  // Cancel the job when the fragment is destroyed
    }

    override fun onResume() {
        super.onResume()
        getScanActivity().reInitOriginalImageFile()
        binding.scanSurfaceView.originalImageFile = getScanActivity().originalImageFile
    }

    private fun initListeners() {
        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }
        binding.cancelButton.setOnClickListener {
            finishActivity()
        }
        binding.flashButton.setOnClickListener {
            switchFlashState()
        }
        binding.galleryButton.setOnClickListener {
            checkForStoragePermissions()
        }
        binding.autoButton.setOnClickListener {
            toggleAutoManualButton()
        }
    }

    private fun toggleAutoManualButton() {
        binding.scanSurfaceView.isAutoCaptureOn = !binding.scanSurfaceView.isAutoCaptureOn
        if (binding.scanSurfaceView.isAutoCaptureOn) {
            binding.autoButton.text = getString(R.string.zdc_auto)
        } else {
            binding.autoButton.text = getString(R.string.zdc_manual)
        }
    }

    private fun checkForCameraPermissions() {
        permissionsBuilder(Manifest.permission.CAMERA)
            .build()
            .send { result ->
                if (result.allGranted()) {
                    startCamera()
                } else if (result.allShouldShowRationale()) {
                    onError(DocumentScannerErrorModel(DocumentScannerErrorModel.ErrorMessage.CAMERA_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
                } else {
                    onError(DocumentScannerErrorModel(DocumentScannerErrorModel.ErrorMessage.CAMERA_PERMISSION_REFUSED_GO_TO_SETTINGS))
                }
            }
    }

    private fun checkForStoragePermissions() {
        permissionsBuilder(getStoragePermission())
            .build()
            .send { result ->
                if (result.allGranted()) {
                    selectImageFromGallery()
                } else if (result.allShouldShowRationale()) {
                    onError(DocumentScannerErrorModel(DocumentScannerErrorModel.ErrorMessage.STORAGE_PERMISSION_REFUSED_WITHOUT_NEVER_ASK_AGAIN))
                } else {
                    onError(DocumentScannerErrorModel(DocumentScannerErrorModel.ErrorMessage.STORAGE_PERMISSION_REFUSED_GO_TO_SETTINGS))
                }
            }
    }

    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun startCamera() {
        binding.scanSurfaceView.start()
    }

    private fun takePhoto() {
        binding.scanSurfaceView.takePicture()
    }

    private fun getScanActivity(): InternalScanActivity {
        return (requireActivity() as InternalScanActivity)
    }

    private fun finishActivity() {
        getScanActivity().finish()
    }

    private fun switchFlashState() {
        binding.scanSurfaceView.switchFlashState()
    }

    override fun showFlash() {
        binding.flashButton.show()
    }

    override fun hideFlash() {
        binding.flashButton.hide()
    }

    private fun selectImageFromGallery() {
        val photoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
        photoPickerIntent.type = "image/*"
        //photoPickerIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        resultLauncher.launch(photoPickerIntent)
    }

    private fun handleGalleryResult(data: Intent?) {
        val imageUri = data?.data
        if (imageUri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    if (isUriValid(imageUri)) {
                        val realPath = FileUriUtils.getRealPath(getScanActivity(), imageUri)
                        withContext(Dispatchers.Main) {
                            if (realPath != null) {
                                getScanActivity().reInitOriginalImageFile()
                                getScanActivity().originalImageFile = File(realPath)
                                startCroppingProcess()
                            } else {
                                Log.e(
                                    TAG,
                                    DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR.error
                                )
                                onError(
                                    DocumentScannerErrorModel(
                                        DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR,
                                        null
                                    )
                                )
                            }
                        }
                    } else {
                        Log.e(TAG, "Invalid URI or null URI")
                        withContext(Dispatchers.Main) {
                            onError(
                                DocumentScannerErrorModel(
                                    DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR,
                                    null
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing URI", e)
                    withContext(Dispatchers.Main) {
                        onError(
                            DocumentScannerErrorModel(
                                DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR,
                                e
                            )
                        )
                    }
                }
            }
        } else {
            Log.e(TAG, "Invalid URI or null URI")
            onError(
                DocumentScannerErrorModel(
                    DocumentScannerErrorModel.ErrorMessage.TAKE_IMAGE_FROM_GALLERY_ERROR,
                    null
                )
            )
        }
    }


    private suspend fun isUriValid(uri: android.net.Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            requireContext().contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URI", e)
            false
        }
    }


    override fun scanSurfacePictureTaken() {
        startCroppingProcess()
    }

    private fun startCroppingProcess() {
        if (isAdded) {
            getScanActivity().showImageCropFragment()
        }
    }

    override fun scanSurfaceShowProgress() {
        showProgressBar()
    }

    override fun scanSurfaceHideProgress() {
        hideProgressBar()
    }

    override fun onError(error: DocumentScannerErrorModel) {
        if (isAdded) {
            getScanActivity().onError(error)
        }
    }

    override fun showFlashModeOn() {
        binding.flashButton.setImageResource(R.drawable.zdc_flash_on)
    }

    override fun showFlashModeOff() {
        binding.flashButton.setImageResource(R.drawable.zdc_flash_off)
    }

    override fun configureEdgeToEdgeInsets(insets: WindowInsetsCompat) {
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        with(binding) {
            topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBarsInsets.top
            }

            bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBarsInsets.bottom
            }
        }
    }
}
