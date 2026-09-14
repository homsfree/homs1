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
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.graphics.Color
import android.widget.FrameLayout
import android.util.Log

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var blurOverlayView: View? = null
    private var customWindowContext: Context? = null // السياق المخصص لأندرويد 16

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && resultData != null) {
            startForegroundServiceNotification()
            
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            startScreenCapture()
            
            // تجربة الشاشة الحمراء بالطريقة المتوافقة مع أندرويد 16
            showRedScreenTest()
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "BlurEngineChannel")
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك المراقبة يعمل...")
                .setSmallIcon(android.R.drawable.ic_secure)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك المراقبة يعمل...")
                .build()
        }
        
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
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
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
            image?.close() 
        }, Handler(Looper.getMainLooper()))
    }

    private fun showRedScreenTest() {
        Handler(Looper.getMainLooper()).post {
            try {
                if (blurOverlayView == null) {
                    // بناء السياق المرئي (WindowContext) المخصص لأنظمة أندرويد 12 و 13 و 14 و 15 و 16
                    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                    
                    val contextToUse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        customWindowContext = createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
                        customWindowContext!!
                    } else {
                        this
                    }

                    val wm = contextToUse.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    
                    blurOverlayView = FrameLayout(contextToUse).apply {
                        setBackgroundColor(Color.RED)
                    }
                    
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                    }
                    
                    wm.addView(blurOverlayView, params)
                    Log.d("BlurEngine", "نجح رسم الشاشة الحمراء في أندرويد 16!")
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        removeBlurOverlay()
                    }, 4000)
                }
            } catch (e: Exception) {
                Log.e("BlurEngine", "فشل الرسم: ${e.message}")
            }
        }
    }

    private fun removeBlurOverlay() {
        Handler(Looper.getMainLooper()).post {
            blurOverlayView?.let { view ->
                try {
                    val wm = (customWindowContext ?: this).getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm.removeView(view)
                } catch (e: Exception) {}
                blurOverlayView = null
                customWindowContext = null
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
