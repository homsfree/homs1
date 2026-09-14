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
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureService : Service() {
    private var mediaProjection: android.media.projection.MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && resultData != null) {
            val channelId = "BlurEngineChannel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Blur Engine", NotificationManager.IMPORTANCE_LOW)
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId).setContentTitle("درع الحماية").setContentText("المحرك يعمل...").setSmallIcon(android.R.drawable.ic_secure).build()
            } else {
                Notification.Builder(this).setContentTitle("درع الحماية").setContentText("المحرك يعمل...").build()
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(1, notification)
            }
            
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay("Capture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null)
            
            imageReader?.setOnImageAvailableListener({ it.acquireLatestImage()?.close() }, Handler(Looper.getMainLooper()))
            
            // هنا نرسل الأمر لخدمة Accessibility القوية لتقوم بالرسم!
            Handler(Looper.getMainLooper()).postDelayed({
                val blurIntent = Intent("SHOW_RED_SCREEN")
                blurIntent.setPackage(packageName)
                sendBroadcast(blurIntent)
            }, 1000)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
