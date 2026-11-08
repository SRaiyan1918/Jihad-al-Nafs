package com.nazar.guard

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    // Strong: screen par kahin bhi (visible) dikhe to block
    private val strong = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "onlyfans",
        "hentai", "sex video", "blue film", "mms leak", "desi mms", "chudai", "camgirl", "pussy"
    )

    // Weak: sirf search box / URL bar (jahan type hota hai) mein block
    private val weak = listOf(
        "nude", "naked", "nangi", "nanga", "erotic", "horny", "sexy video",
        "bf video", "adult video", "18+ video", "boobs", "bhabhi sex"
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
        block(reason, pkg)
    }

    private fun scan(n: AccessibilityNodeInfo?, pkg: String, depth: Int, count: IntArray): String? {
        if (n == null || depth > 25 || count[0] > 700) return null
        count[0]++
        if (!n.isVisibleToUser) return null // chhupe hue nodes ignore
        val txt = (n.text?.toString() ?: "").lowercase().trim()
        val desc = (n.contentDescription?.toString() ?: "").lowercase().trim()
        val t = "$txt $desc"
        if (t.isNotBlank()) {
            if (pkg in browsers && isIncognito(txt, desc)) return "incognito | $pkg | $txt / $desc"
            for (k in strong) if (t.contains(k)) return "keyword '$k' | $pkg | $t"
            if (n.isEditable) for (k in weak) if (t.contains(k)) return "typed '$k' | $pkg | $t"
        }
        for (i in 0 until n.childCount) {
            val r = scan(n.getChild(i), pkg, depth + 1, count)
            if (r != null) return r
        }
        return null
    }

    private fun isIncognito(txt: String, desc: String): Boolean {
        val all = "$txt $desc"
        if (all.contains("gone incognito")) return true
        val exact = setOf("incognito", "incognito mode", "private browsing", "inprivate")
        return txt in exact || desc in exact
    }

    private fun block(reason: String, pkg: String) {
        prefs().edit().putString("lastReason", reason.take(200)).apply()
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, ReminderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        killApp(pkg)
        val name = prefs().getString("name", "") ?: ""
        val who = if (name.isBlank()) "Aapke dost" else name
        sms(
            "Assalamu alaikum. $who ke Nazar Guard ne abhi ek galat cheez rok di. " +
                "Please unke liye dua karein aur pyaar se hal-chal poochh lein.",
            "lastSms", 30 * 60 * 1000L
        )
    }

    // App ko background mein jate hi kill karta hai (incognito tabs bhi chale jate hain)
    private fun killApp(pkg: String) {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val h = Handler(Looper.getMainLooper())
        for (d in longArrayOf(600, 1500, 3000, 6000)) {
            h.postDelayed({ try { am.killBackgroundProcesses(pkg) } catch (_: Exception) {} }, d)
        }
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
