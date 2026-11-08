package com.nazar.guard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        val d = resources.displayMetrics.density
        fun dp(x: Int) = (x * d).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        status = TextView(this).apply { textSize = 18f; setPadding(0, 0, 0, dp(16)) }

        val name = EditText(this).apply {
            hint = "Aapka naam (message mein jayega)"
            setText(p.getString("name", ""))
        }
        val partner = EditText(this).apply {
            hint = "Bharosemand ka number (+91...)"
            inputType = InputType.TYPE_CLASS_PHONE
            setText(p.getString("partner", ""))
        }

        fun btn(t: String, f: () -> Unit) = Button(this).apply {
            text = t
            setOnClickListener { f() }
        }

        col.addView(status)
        col.addView(btn("1) Accessibility ON karo") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Nazar Guard chuno aur ON karo", Toast.LENGTH_LONG).show()
        })
        col.addView(btn("2) SMS permission do") {
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), 1)
        })
        col.addView(name)
        col.addView(partner)
        col.addView(btn("3) Save karo") {
            p.edit().putString("name", name.text.toString().trim())
                .putString("partner", partner.text.toString().trim()).apply()
            Toast.makeText(this, "Save ho gaya", Toast.LENGTH_SHORT).show()
        })
        col.addView(btn("Reminder screen test karo") {
            startActivity(Intent(this, ReminderActivity::class.java))
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val on = s.contains("$packageName/")
        status.text = if (on) "Guard: ON" else "Guard: OFF (Accessibility chalu karo)"
    }
}
