package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.content.Context
import android.content.Intent
import android.graphics.Color

class BlockingService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var blurOverlayView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // يمكنك هنا ربط عرض طبقة التشويش بالذكاء الاصطناعي لاحقاً
    }

    private fun showBlurOverlay() {
        if (blurOverlayView != null) return
        
        blurOverlayView = View(this).apply {
            setBackgroundColor(Color.argb(200, 50, 50, 50))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        try {
            windowManager?.addView(blurOverlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBlurOverlay() {
        blurOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            blurOverlayView = null
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        removeBlurOverlay()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        removeBlurOverlay()
    }
}
