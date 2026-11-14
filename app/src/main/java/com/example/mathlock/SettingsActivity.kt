package com.example.mathlock

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Экран настроек: операции, сложность, поведение блокировки.
 * Всё сразу сохраняется в SharedPreferences (PreferenceManager.getDefaultSharedPreferences).
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        setContentView(R.layout.activity_settings)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        findViewById<MaterialCheckBox>(R.id.checkAdd).apply {
            isChecked = prefs.getBoolean(Prefs.OP_ADD, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(Prefs.OP_ADD, checked).apply()
            }
        }
        findViewById<MaterialCheckBox>(R.id.checkSub).apply {
            isChecked = prefs.getBoolean(Prefs.OP_SUB, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(Prefs.OP_SUB, checked).apply()
            }
        }
        findViewById<MaterialCheckBox>(R.id.checkMul).apply {
            isChecked = prefs.getBoolean(Prefs.OP_MUL, false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(Prefs.OP_MUL, checked).apply()
            }
        }
        findViewById<MaterialCheckBox>(R.id.checkDiv).apply {
            isChecked = prefs.getBoolean(Prefs.OP_DIV, false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(Prefs.OP_DIV, checked).apply()
            }
        }

        val difficultyGroup = findViewById<RadioGroup>(R.id.radioDifficulty)
        when (prefs.getString("difficulty", Prefs.DIFF_EASY)) {
            Prefs.DIFF_MEDIUM -> difficultyGroup.check(R.id.radioMedium)
            Prefs.DIFF_HARD -> difficultyGroup.check(R.id.radioHard)
            else -> difficultyGroup.check(R.id.radioEasy)
        }
        difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radioMedium -> Prefs.DIFF_MEDIUM
                R.id.radioHard -> Prefs.DIFF_HARD
                else -> Prefs.DIFF_EASY
            }
            prefs.edit().putString("difficulty", value).apply()
        }

        findViewById<MaterialSwitch>(R.id.switchLockOnScreenOn).apply {
            isChecked = prefs.getBoolean("lock_on_screen_on", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("lock_on_screen_on", checked).apply()
            }
        }
    }
}
