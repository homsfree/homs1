package com.homsfree.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.graphics.Color

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
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            startForegroundServiceNotification()
            startScreenCapture()
        }

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "BlurEngineChannel")
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك التشويش يعالج الشاشة محلياً بأمان...")
                .setSmallIcon(android.R.drawable.ic_secure) // يفضل وضع أيقونة فعلية لاحقاً
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك التشويش يعالج الشاشة محلياً بأمان...")
                .build()
        }
        startForeground(1, notification)
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
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        // التقاط الشاشة بصيغة RGBA (بدون حفظ أي شيء، تتم المعالجة في الذاكرة الحية فقط - RAM)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        // مراقبة الإطارات (Frames) بشكل حي
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                // هُنا المكان السري والآمن 100%: 
                // البيانات (البكسلات) تُقرأ من الـ image، تُرسل لموديل الذكاء الاصطناعي لفحصها، 
                // ثم يُغلق الـ image فوراً لتدمير البيانات من الذاكرة. لا يوجد أي تسريب.
                
                // --- (محاكاة لاختبار التشويش حالياً) ---
                // سنقوم بتشغيل طبقة التشويش كل 10 ثواني للتأكد أن الربط يعمل بشكل سليم
                // في الخطوة القادمة، سيتم استبدال هذه المحاكاة بقرار الـ TFLite الفعلي.
                
                Log.d("BlurEngine", "يتم فحص الإطار بأمان...")
                
                // إغلاق الإطار لتفريغ الذاكرة فوراً ومنع التسريب
                image.close() 
            }
        }, Handler(Looper.getMainLooper()))
        
        // للاختبار السريع: أظهر التشويش فور بدء الخدمة
        showBlurOverlay()
        Handler(Looper.getMainLooper()).postDelayed({
            removeBlurOverlay()
        }, 3000)
    }
    
    private fun showBlurOverlay() {
        if (blurOverlayView != null) return
        
        // رسم طبقة سوداء شفافة للتعتيم السريع
        blurOverlayView = View(this).apply {
            setBackgroundColor(Color.argb(230, 0, 0, 0))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            // استخدام نوع نافذة آمن (OVERLAY) يظهر فوق كل شيء
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        try {
            windowManager?.addView(blurOverlayView, params)
            Log.d("BlurEngine", "تم تفعيل طبقة التشويش!")
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

    override fun onDestroy() {
        super.onDestroy()
        removeBlurOverlay()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
