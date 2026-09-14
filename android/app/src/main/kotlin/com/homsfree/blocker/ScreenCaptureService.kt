package com.homsfree.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class ScreenCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        
        // إنشاء إشعار النظام الإجباري للخدمات الأمامية
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "BlurEngineChannel")
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك الذكاء الاصطناعي يراقب الشاشة...")
                .setSmallIcon(android.R.drawable.ic_secure) // أيقونة افتراضية
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("درع الحماية الذكي")
                .setContentText("محرك الذكاء الاصطناعي يراقب الشاشة...")
                .build()
        }
        
        // إخبار النظام أن هذه خدمة التقاط شاشة
        startForeground(1, notification)

        // في الخطوة القادمة سنقوم بتشغيل ImageReader لاستخراج الصور من هنا
        
        return START_STICKY
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
}
