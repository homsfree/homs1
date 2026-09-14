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
import android.text.TextUtils
import android.widget.Toast

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.homsfree.blocker/blur_engine"
    private val SCREEN_RECORD_REQUEST_CODE = 1000

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "updateSettings") {
                val blurImages = call.argument<Boolean>("blurImages") ?: false
                if (blurImages) {
                    // التحقق من الصلاحيات بالترتيب
                    if (!isAccessibilityEnabled()) {
                        Toast.makeText(this, "يرجى تفعيل (درع الحماية الذكي) من إعدادات إمكانية الوصول", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "يرجى تفعيل (الظهور فوق التطبيقات)", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    } else {
                        // كل الأذونات جاهزة، نطلب تسجيل الشاشة
                        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_RECORD_REQUEST_CODE)
                    }
                } else {
                    stopService(Intent(this, ScreenCaptureService::class.java))
                }
                result.success(true)
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expectedName = "$packageName/${BlockingService::class.java.name}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedName, ignoreCase = true)) return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
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
        super.onActivityResult(requestCode, resultCode, data)
    }
}
