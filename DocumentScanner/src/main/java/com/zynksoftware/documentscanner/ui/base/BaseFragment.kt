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

package com.zynksoftware.documentscanner.ui.base

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.zynksoftware.documentscanner.common.extensions.hide
import com.zynksoftware.documentscanner.common.extensions.show
import com.zynksoftware.documentscanner.R

internal abstract class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(view ?: return) { _, insets ->
            configureEdgeToEdgeInsets(insets)
            insets
        }
    }

    /**
     * Allows child fragments to customize how insets are applied.
     *
     * @param insets The system window insets to be applied.
     */
    protected abstract fun configureEdgeToEdgeInsets(insets: WindowInsetsCompat)

    fun showProgressBar() {
        view?.findViewById<RelativeLayout>(R.id.progressLayout)?.show()
    }

    fun hideProgressBar() {
        view?.findViewById<RelativeLayout>(R.id.progressLayout)?.hide()
    }
}
