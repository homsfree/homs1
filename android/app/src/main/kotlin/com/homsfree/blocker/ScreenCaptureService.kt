package com.homsfree.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.graphics.Color
import android.widget.FrameLayout

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    private var blurOverlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && resultData != null) {
            // 1. إصلاح أندرويد 14: إخبار النظام فوراً أننا نصور الشاشة حتى لا يقتل التطبيق
            startForegroundServiceNotification()
            
            // 2. بدء محرك التصوير
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            startScreenCapture()
            
            // 3. اختبار التشويش باللون الأحمر
            showRedScreenTest()
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "BlurEngineChannel")
                .setContentTitle("درع الحماية الذكي")
                .setContentText("المحرك يعمل...")
                .setSmallIcon(android.R.drawable.ic_secure)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("درع الحماية الذكي")
                .setContentText("المحرك يعمل...")
                .build()
        }
        
        // السر هنا لمنع الانهيار في أندرويد الحديث
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "BlurEngineChannel",
                "Blur Engine Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun startScreenCapture() {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                // التخلص من الصورة لعدم التسريب
                image.close() 
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun showRedScreenTest() {
        Handler(Looper.getMainLooper()).post {
            try {
                if (blurOverlayView == null) {
                    val metrics = DisplayMetrics()
                    windowManager?.defaultDisplay?.getMetrics(metrics)
                    val width = metrics.widthPixels
                    val height = metrics.heightPixels
                    
                    blurOverlayView = FrameLayout(this).apply {
                        setBackgroundColor(Color.RED) // شاشة حمراء فاقعة
                    }
                    
                    val params = WindowManager.LayoutParams(
                        width,
                        height + 200, // تجاوز أبعاد الشاشة لضمان التغطية
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    )
                    params.gravity = Gravity.TOP or Gravity.START
                    
                    windowManager?.addView(blurOverlayView, params)
                    
                    // إزالة الشاشة الحمراء بعد 4 ثواني
                    Handler(Looper.getMainLooper()).postDelayed({
                        removeBlurOverlay()
                    }, 4000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeBlurOverlay() {
        Handler(Looper.getMainLooper()).post {
            blurOverlayView?.let {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {}
                blurOverlayView = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBlurOverlay()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
