package com.homsfree.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.util.Log

class BlockingService : AccessibilityService() {
    private val badWords = listOf(
        "porn", "xxx", "nsfw", "pornhub", "xnxx", "xvideos", 
        "xhamster", "brazzers", "onlyfans", "sex",
        "اباحي", "سكس", "جنس", "للكبار فقط", "نيك"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockerAI", "تم تشغيل خدمة الذكاء الاصطناعي بنجاح")
        // هنا سيتم تهيئة TFLite لاحقاً لمعالجة الصور
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. الفحص النصي السريع (كخط دفاع أول)
        val eventText = event.text.joinToString(" ").lowercase()
        val packageName = event.packageName?.toString()?.lowercase() ?: ""

        if (badWords.any { eventText.contains(it) || packageName.contains(it) }) {
            goHome()
            return
        }

        // 2. الفحص العميق (Deep Scan)
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val foundBadWord = checkNode(rootNode)
            rootNode.recycle()
            if (foundBadWord) {
                goHome()
            }
        }
        
        // 3. (قسم تحليل الصور بالذكاء الاصطناعي TFLite سيتم تفعيله هنا بمجرد التأكد من نجاح البناء)
    }

    private fun checkNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (badWords.any { nodeText.contains(it) || nodeDesc.contains(it) }) return true

        for (i in 0 until node.childCount) {
            if (checkNode(node.getChild(i))) return true
        }
        return false
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }

    override fun onInterrupt() {}
}
