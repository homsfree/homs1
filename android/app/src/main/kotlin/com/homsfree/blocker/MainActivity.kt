package com.homsfree.blocker

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.homsfree.blocker/blur_engine"
    private val SCREEN_RECORD_REQUEST_CODE = 1000
    private val OVERLAY_PERMISSION_REQ_CODE = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "updateSettings" -> {
                    val blurImages = call.argument<Boolean>("blurImages") ?: false
                    if (blurImages) {
                        checkAndRequestPermissions()
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

    // فحص وطلب إذن الظهور فوق التطبيقات
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.d("BlurEngine", "طلب إذن الظهور فوق التطبيقات...")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // إذا كان الإذن ممنوحاً مسبقاً، ننتقل لطلب مشاركة الشاشة
            startScreenCaptureRequest()
        }
    }

    private fun startScreenCaptureRequest() {
        Log.d("BlurEngine", "جاري طلب إذن تسجيل الشاشة...")
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_RECORD_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            // بعد عودة المستخدم من شاشة الإعدادات
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startScreenCaptureRequest() // الإذن تم منحه، الآن نطلب تسجيل الشاشة
            } else {
                Log.d("BlurEngine", "تم رفض إذن الظهور فوق التطبيقات!")
            }
        } else if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
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
