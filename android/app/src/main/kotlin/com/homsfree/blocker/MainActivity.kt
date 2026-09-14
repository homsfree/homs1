package com.homsfree.blocker

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.homsfree.blocker/blur_engine"
    private val SCREEN_RECORD_REQUEST_CODE = 1000
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // تجهيز مدير التقاط الشاشة
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "updateSettings" -> {
                    // عندما يتم تفعيل الخيار من التطبيق
                    val blurImages = call.argument<Boolean>("blurImages") ?: false
                    if (blurImages) {
                        Log.d("BlurEngine", "جاري طلب إذن تسجيل الشاشة...")
                        // هذا السطر هو الذي يظهر رسالة أندرويد "هل تريد بدء التسجيل..."
                        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                        startActivityForResult(captureIntent, SCREEN_RECORD_REQUEST_CODE)
                    } else {
                        Log.d("BlurEngine", "جاري إيقاف الخدمة...")
                        stopService(Intent(this, ScreenCaptureService::class.java))
                    }
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
    }

    // استلام رد المستخدم (موافق أو رفض)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d("BlurEngine", "المستخدم وافق على تسجيل الشاشة! جاري تشغيل المحرك.")
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                Log.d("BlurEngine", "المستخدم رفض إذن تسجيل الشاشة.")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
