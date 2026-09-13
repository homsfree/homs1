package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent

class BlockingService : AccessibilityService() {
    private val badWords = listOf(
        "porn", "xxx", "nsfw", "pornhub", "xnxx", "xvideos", 
        "xhamster", "brazzers", "onlyfans", "sex",
        "اباحي", "سكس", "جنس", "للكبار فقط", "نيك"
    )

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
