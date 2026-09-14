package com.homsfree.blocker

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.homsfree.blocker/blur_engine"
    private val SCREEN_RECORD_REQUEST_CODE = 1000
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateSettings") {
                val blurImages = call.argument<Boolean>("blurImages") ?: false
                if (blurImages) {
                    // طلب إذن تصوير الشاشة من المستخدم
                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_RECORD_REQUEST_CODE)
                } else {
                    // إيقاف المحرك
                    stopService(Intent(this, ScreenCaptureService::class.java))
                }
                result.success(true)
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // المستخدم وافق، تشغيل خدمة التقاط الشاشة بالخلفية
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("code", resultCode)
                    putExtra("data", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
