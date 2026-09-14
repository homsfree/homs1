package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.util.Log

class BlockingService : AccessibilityService() {
    private val badWords = listOf("porn", "xxx", "nsfw", "pornhub", "xnxx", "xvideos", "sex", "اباحي", "سكس", "جنس")

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlurAI", "تم تفعيل خدمة الذكاء الاصطناعي لتغشيش الصور بالخلفية")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val textOnScreen = event.text.joinToString(" ").lowercase()
        val packageName = event.packageName?.toString()?.lowercase() ?: ""

        // فحص سريع للنصوص أو المواقع كدفاع أول
        for (word in badWords) {
            if (textOnScreen.contains(word) || packageName.contains(word)) {
                // سيتم هنا تطبيق تأثير التشويش (Blur) أو العودة للخلف
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
                break
            }
        }
        
        // ملاحظة: معالجة الصور الكاملة (Blur Engine) يتم ربطها عبر TFLite Image Processor في النسخ القادمة
    }

    override fun onInterrupt() {}
}
