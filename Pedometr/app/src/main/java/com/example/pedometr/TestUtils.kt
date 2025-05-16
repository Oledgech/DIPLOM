package com.example.pedometr

import android.widget.Toast import androidx.test.espresso.IdlingResource import java.lang.ref.WeakReference

class TestUtils {
    companion object {
        fun setToastForTesting(toast: Toast?) {
            if (toast != null) {
                toastIdlingResource?.setToast(toast)
            }
        }

        var toastIdlingResource: ToastIdlingResource? = null
    }
    fun setToastForTesting(toast: Toast) {
        toastIdlingResource?.setToast(toast)
    }
}

class ToastIdlingResource : IdlingResource {
    private var callback: IdlingResource.ResourceCallback? = null
    private var isIdle = true
    private var toastRef: WeakReference<Toast>? = null
    fun setToast(toast: Toast) {
        toastRef = WeakReference(toast)
        isIdle = false
    }

    override fun getName(): String = "ToastIdlingResource"

    override fun isIdleNow(): Boolean {
        val toast = toastRef?.get()
        if (toast == null || toast.view == null) {
            if (!isIdle) {
                isIdle = true
                callback?.onTransitionToIdle()
            }
        }
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }
}