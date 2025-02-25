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


package com.zynksoftware.documentscanner.ui.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.zynksoftware.documentscanner.common.extensions.rotateBitmap
import com.zynksoftware.documentscanner.databinding.FragmentImageProcessingBinding
import com.zynksoftware.documentscanner.ui.base.BaseFragment
import com.zynksoftware.documentscanner.ui.scan.InternalScanActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ImageProcessingFragment : BaseFragment() {
    private var _binding: FragmentImageProcessingBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = ImageProcessingFragment::class.simpleName
        private const val ANGLE_OF_ROTATION = 90

        fun newInstance(): ImageProcessingFragment {
            return ImageProcessingFragment()
        }
    }

    private var isInverted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageProcessingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)

        initListeners()
    }

    private fun initListeners() {
        binding.closeButton.setOnClickListener {
            closeFragment()
        }
        binding.confirmButton.setOnClickListener {
            selectFinalScannerResults()
        }
        binding.magicButton.setOnClickListener {
            applyGrayScaleFilter()
        }
        binding.rotateButton.setOnClickListener {
            rotateImage()
        }
    }

    private fun getScanActivity(): InternalScanActivity {
        return (requireActivity() as InternalScanActivity)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun rotateImage() {
        Log.d(TAG, "ZDCrotate starts ${System.currentTimeMillis()}")
        showProgressBar()
        GlobalScope.launch(Dispatchers.IO) {
            if (isAdded) {
                getScanActivity().transformedImage =
                    getScanActivity().transformedImage?.rotateBitmap(ANGLE_OF_ROTATION)
                getScanActivity().croppedImage =
                    getScanActivity().croppedImage?.rotateBitmap(ANGLE_OF_ROTATION)
            }

            if (isAdded) {
                getScanActivity().runOnUiThread {
                    hideProgressBar()
                    if (isInverted) {
                        binding.imagePreview.setImageBitmap(getScanActivity().transformedImage)
                    } else {
                        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)
                    }
                }
            }
            Log.d(TAG, "ZDCrotate ends ${System.currentTimeMillis()}")
        }
    }

    private fun closeFragment() {
        getScanActivity().closeCurrentFragment()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun applyGrayScaleFilter() {
        Log.d(TAG, "ZDCgrayscale starts ${System.currentTimeMillis()}")
        showProgressBar()
        GlobalScope.launch(Dispatchers.IO) {
            if (isAdded) {
                if (!isInverted) {
                    val bmpMonochrome = Bitmap.createBitmap(
                        getScanActivity().croppedImage!!.width,
                        getScanActivity().croppedImage!!.height,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bmpMonochrome)
                    val ma = ColorMatrix()
                    ma.setSaturation(0f)
                    val paint = Paint()
                    paint.colorFilter = ColorMatrixColorFilter(ma)
                    getScanActivity().croppedImage?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
                    getScanActivity().transformedImage =
                        bmpMonochrome.config?.let { bmpMonochrome.copy(it, true) }
                    getScanActivity().runOnUiThread {
                        hideProgressBar()
                        binding.imagePreview.setImageBitmap(getScanActivity().transformedImage)
                    }
                } else {
                    getScanActivity().runOnUiThread {
                        hideProgressBar()
                        binding.imagePreview.setImageBitmap(getScanActivity().croppedImage)
                        getScanActivity().transformedImage = null
                    }
                }
                isInverted = !isInverted
                Log.d(TAG, "ZDCgrayscale ends ${System.currentTimeMillis()}")
            }
        }
    }

    private fun selectFinalScannerResults() {
        getScanActivity().finalScannerResult()
    }

    override fun configureEdgeToEdgeInsets(insets: WindowInsetsCompat) {
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        with(binding) {
            imagePreview.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBarsInsets.top
            }

            bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBarsInsets.bottom
            }
        }
    }
}
