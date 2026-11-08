package com.nazar.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GuardService : AccessibilityService() {

    private var lastScan = 0L
    private var lastBlock = 0L

    private val watched = setOf(
        "com.android.chrome", "com.google.android.youtube", "org.mozilla.firefox",
        "com.brave.browser", "com.microsoft.emmx", "com.sec.android.app.sbrowser",
        "com.opera.browser", "com.UCMobile.intl"
    )
    private val browsers = watched - "com.google.android.youtube"

    // Yahan apni marzi se words add/remove kar sakte ho
    private val keywords = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "onlyfans",
        "nude", "naked", "hentai", "sex video", "sexy video", "erotic", "blue film",
        "bf video", "mms leak", "desi mms", "chudai", "nangi", "nanga", "bhabhi sex",
        "boobs", "pussy", "horny", "camgirl", "adult video", "18+ video"
    )

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        if (pkg !in watched) return
        val now = System.currentTimeMillis()
        if (now - lastBlock < 4000 || now - lastScan < 600) return
        lastScan = now
        val root = rootInActiveWindow ?: return
        val reason = scan(root, pkg, 0, IntArray(1)) ?: return
        lastBlock = now
        block(reason)
    }

    private fun scan(n: AccessibilityNodeInfo?, pkg: String, depth: Int, count: IntArray): String? {
        if (n == null || depth > 25 || count[0] > 600) return null
        count[0]++
        val t = ((n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")).lowercase()
        if (t.isNotBlank()) {
            if (pkg in browsers && isIncognito(t)) return "incognito"
            for (k in keywords) if (t.contains(k)) return "keyword"
        }
        for (i in 0 until n.childCount) {
            val r = scan(n.getChild(i), pkg, depth + 1, count)
            if (r != null) return r
        }
        return null
    }

    private fun isIncognito(t: String): Boolean {
        if (t.trim().startsWith("new incognito")) return false
        return t.contains("incognito") || t.contains("inprivate") ||
            t.contains("private tab") || t.contains("गुप्त")
    }

    private fun block(reason: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, ReminderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        val name = prefs().getString("name", "") ?: ""
        val who = if (name.isBlank()) "Aapke dost" else name
        sms(
            "Assalamu alaikum. $who ke Nazar Guard ne abhi ek galat cheez rok di. " +
                "Please unke liye dua karein aur pyaar se hal-chal poochh lein.",
            "lastSms", 30 * 60 * 1000L
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val name = prefs().getString("name", "") ?: ""
        val who = if (name.isBlank()) "Aapke dost" else name
        sms(
            "Assalamu alaikum. $who ne Nazar Guard ki Accessibility band/hata di hai. " +
                "Please unse baat karein.",
            "lastOffSms", 10 * 60 * 1000L
        )
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

    private fun sms(text: String, key: String, gapMs: Long) {
        val p = prefs()
        val num = p.getString("partner", "") ?: ""
        val now = System.currentTimeMillis()
        if (num.isBlank() || now - p.getLong(key, 0) < gapMs) return
        try {
            val sm = if (Build.VERSION.SDK_INT >= 31) getSystemService(SmsManager::class.java)
            else @Suppress("DEPRECATION") SmsManager.getDefault()
            sm.sendMultipartTextMessage(num, null, sm.divideMessage(text), null, null)
            p.edit().putLong(key, now).apply()
        } catch (_: Exception) {
        }
    }
}
