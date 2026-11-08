package com.nazar.guard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ReminderActivity : Activity() {

    private val messages = listOf(
        "Allah ka farman: \"Momin mardon se kaho apni nazrein jhuka lein aur apni sharmgahon ki hifazat karein, ye unke liye zyada pakeezah hai.\" (Surah An-Noor 24:30)",
        "Allah ka farman: \"Zina ke qareeb bhi mat jao, beshak ye behayai aur bura raasta hai.\" (Surah Al-Isra 17:32)",
        "Allah ka farman: \"Kya wo nahi jaanta ki Allah dekh raha hai?\" (Surah Al-Alaq 96:14)",
        "Nabi ﷺ ne Hazrat Ali (RA) se farmaya: pehli nazar maaf hai, doosri nazar mat daalo. (Abu Dawud, Tirmizi)",
        "Hadees: Jo shakhs Allah ki wajah se kisi cheez ko chhod deta hai, Allah use usse behtar cheez ata farmata hai. (Musnad Ahmad)",
        "Allah ka farman: \"Beshak Allah taubah karne walon aur paak rehne walon se mohabbat karta hai.\" (Surah Al-Baqarah 2:222)",
        "Ye urge ek lehar hai, 15-20 minute mein utar jati hai. Abhi wudu karo, 2 rakaat padho, jagah badal do.",
        "Phone rakh do, thanda paani piyo, 20 pushups lagao, aur dua karo: \"Ya Muqallibal quloob, thabbit qalbi ala deenik.\"",
        "Jo gunah chhodne ki koshish mein har baar ruk jata hai, Allah uski himmat dekh raha hai. Tum akele nahi ho. Astaghfirullah.",
        "Socho: ye ek pal ka maza aur baad ki sharminda-gi. Aaj ruk gaye to kal ka tum shukr karoge."
    )

    private var timer: CountDownTimer? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val d = resources.displayMetrics.density
        fun dp(x: Int) = (x * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B3D2E"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val title = TextView(this).apply {
            text = "Ruk jao. Allah dekh raha hai."
            setTextColor(Color.parseColor("#F5E6A8"))
            textSize = 24f
            gravity = Gravity.CENTER
        }
        val msg = TextView(this).apply {
            text = messages.random()
            setTextColor(Color.WHITE)
            textSize = 19f
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(28))
        }
        val count = TextView(this).apply {
            text = "15"
            setTextColor(Color.parseColor("#F5E6A8"))
            textSize = 56f
            gravity = Gravity.CENTER
        }
        val btn = Button(this).apply {
            text = "Intezar karo..."
            isEnabled = false
        }
        btn.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
        root.addView(title)
        root.addView(msg)
        root.addView(count)
        root.addView(btn)
        setContentView(root)

        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(ms: Long) {
                count.text = ((ms + 999) / 1000).toString()
            }

            override fun onFinish() {
                count.text = "0"
                btn.isEnabled = true
                btn.text = "Theek hai, Alhamdulillah"
            }
        }.start()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // 15 second tak back band
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
