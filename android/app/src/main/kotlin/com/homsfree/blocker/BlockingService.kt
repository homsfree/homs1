package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent

class BlockingService : AccessibilityService() {
    private val badWords = listOf("youtube", "tiktok", "porn", "xxx", "ممنوع")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val textOnScreen = event.text.joinToString(" ").lowercase()
        val packageName = event.packageName?.toString()?.lowercase() ?: ""

        for (word in badWords) {
            if (textOnScreen.contains(word) || packageName.contains(word)) {
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(homeIntent)
                break
            }
        }
    }
    override fun onInterrupt() {}
}
