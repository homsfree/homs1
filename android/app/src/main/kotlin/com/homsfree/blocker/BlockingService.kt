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

class BlockingService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var blurOverlayView: View? = null
    
    // مستقبل الأوامر من محرك فحص الشاشة
    private val blurCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.homsfree.blocker.SHOW_BLUR") {
                showBlurOverlay()
                // إزالة التشويش بعد 3 ثواني للاختبار
                Handler(Looper.getMainLooper()).postDelayed({
                    removeBlurOverlay()
                }, 3000)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // تسجيل الاستماع لأوامر التشويش
        val filter = IntentFilter("com.homsfree.blocker.SHOW_BLUR")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(blurCommandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(blurCommandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // لا نحتاج لعمل شيء هنا حالياً، الأوامر ستأتي من Broadcast
    }

    private fun showBlurOverlay() {
        if (blurOverlayView != null) return
        
        blurOverlayView = View(this).apply {
            setBackgroundColor(Color.argb(230, 0, 0, 0)) // أسود شبه كامل
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, // أقوى نوع نافذة
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
        try {
            unregisterReceiver(blurCommandReceiver)
        } catch (e: Exception) {}
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        removeBlurOverlay()
    }
}
