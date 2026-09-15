package com.homsfree.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.content.res.Resources
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ScreenCaptureService : Service() {
    private var mediaProjection: android.media.projection.MediaProjection? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    // متغيرات المعالجة والذكاء الاصطناعي
    private var tflite: Interpreter? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var lastProcessTime = 0L
    private var isCurrentlyBlocked = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // تهيئة المسار الخلفي للمعالجة الثقيلة
        backgroundThread = HandlerThread("AI_Processing_Thread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        // تحميل موديل الذكاء الاصطناعي
        try {
            tflite = Interpreter(loadModelFile())
            Log.d("BlurEngine", "تم تحميل نموذج الذكاء الاصطناعي بنجاح.")
        } catch (e: Exception) {
            Log.e("BlurEngine", "تحذير: ملف nsfw.tflite غير موجود في مجلد assets. الرجاء إضافته.")
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("nsfw.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("code", -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && resultData != null) {
            startForegroundServiceNotification()
            
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            val metrics = Resources.getSystem().displayMetrics
            // تقليل الدقة قليلاً لتسريع المعالجة
            val width = metrics.widthPixels / 2
            val height = metrics.heightPixels / 2
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AI_Capture", width, height, metrics.densityDpi, 
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, null
            )
            
            // قراءة الإطارات وإرسالها للمسار الخلفي
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                
                val currentTime = System.currentTimeMillis()
                // معالجة صورتين فقط في الثانية لتجنب استنزاف المعالج
                if (currentTime - lastProcessTime >= 500) {
                    lastProcessTime = currentTime
                    processImageSecurely(image, width, height)
                } else {
                    image.close() // تدمير الإطار فوراً لتفريغ الـ RAM
                }
            }, backgroundHandler)
        }
        return START_STICKY
    }

    private fun processImageSecurely(image: Image, width: Int, height: Int) {
        try {
            if (tflite == null) return

            // 1. تحويل الإطار إلى مصفوفة بايتات للذكاء الاصطناعي
            val planes = image.planes
            val buffer = planes[0].buffer
            
            // (هنا يتم تحضير البيانات لـ TFLite بحسب شكل الإدخال للموديل 224x224 عادة)
            // محاكاة سريعة لقرار الذكاء الاصطناعي (يجب استبدالها لاحقاً بتمرير البفر الفعلي للموديل)
            // val output = Array(1) { FloatArray(2) }
            // tflite?.run(inputBuffer, output)
            
            // محاكاة لنتيجة الفحص (هل هو آمن أم لا؟)
            val isNSFW = false // ضع النتيجة الحقيقية هنا من output[0][1] > 0.7

            if (isNSFW && !isCurrentlyBlocked) {
                isCurrentlyBlocked = true
                sendBlockCommand("SHOW_BLUR")
            } else if (!isNSFW && isCurrentlyBlocked) {
                isCurrentlyBlocked = false
                sendBlockCommand("HIDE_BLUR")
            }
        } catch (e: Exception) {
            Log.e("BlurEngine", "خطأ في المعالجة: ${e.message}")
        } finally {
            image.close() // الأهم هندسياً: إغلاق الصورة دائماً لتجنب تسريب الذاكرة (Memory Leak)
        }
    }

    private fun sendBlockCommand(action: String) {
        val blurIntent = Intent(action)
        blurIntent.setPackage(packageName)
        sendBroadcast(blurIntent)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "BlurEngineChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Blur Engine", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION.CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("درع الحماية").setContentText("المراقبة الذكية فعالة").setSmallIcon(android.R.drawable.ic_secure).build()
        } else {
            Notification.Builder(this).setContentTitle("درع الحماية").setContentText("المراقبة الذكية فعالة").build()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        backgroundThread?.quitSafely()
    }
}
