package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.widget.FrameLayout

class BlockingService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var blurOverlayView: View? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "SHOW_BLUR" -> showBlurScreen()
                "HIDE_BLUR" -> removeOverlay()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val filter = IntentFilter().apply {
            addAction("SHOW_BLUR")
            addAction("HIDE_BLUR")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun showBlurScreen() {
        if (blurOverlayView != null) return
        
        Handler(Looper.getMainLooper()).post {
            try {
                blurOverlayView = FrameLayout(this).apply {
                    setBackgroundColor(Color.parseColor("#E6000000")) // أسود معتم بنسبة 90% للحجب التام
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                windowManager?.addView(blurOverlayView, params)
            } catch (e: Exception) { }
        }
    }

    private fun removeOverlay() {
        blurOverlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            blurOverlayView = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { removeOverlay() }
    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        try { unregisterReceiver(receiver) } catch (e: Exception) {}
    }
}
