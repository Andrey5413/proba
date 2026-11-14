package com.example.mathlock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.IBinder
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

/**
 * Полноэкранный экран блокировки с математическим примером.
 *  - без ActionBar, показывается даже на заблокированном экране;
 *  - неверный ответ: вибрация, сообщение «Неверно», НОВЫЙ пример;
 *  - аппаратная кнопка «Назад» перехвачена через onBackPressedDispatcher
 *    и ничего не делает.
 */
class LockActivity : AppCompatActivity() {

    private lateinit var problemText: TextView
    private lateinit var answerInput: EditText
    private lateinit var messageText: TextView

    private var currentProblem: MathProblem? = null

    private var service: LockService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? LockService.LocalBinder)?.getService()
            service?.onLockActivityShown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Показ поверх замка и включение экрана.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_lock)

        problemText = findViewById(R.id.textProblem)
        answerInput = findViewById(R.id.editAnswer)
        messageText = findViewById(R.id.textMessage)

        findViewById<Button>(R.id.buttonUnlock).setOnClickListener { checkAnswer() }

        // Перехват кнопки «Назад» — не делает ничего.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Намеренно пусто: с экрана блокировки нельзя выйти кнопкой «Назад».
            }
        })

        nextProblem()
    }

    override fun onStart() {
        super.onStart()
        // Поднимаем сторожевой сервис и привязываемся к нему.
        LockService.start(this)
        val intent = Intent(this, LockService::class.java)
        bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        // Экран ушёл на задний план — сервис покажет overlay-заглушку.
        if (bound) {
            try {
                service?.onLockActivityHidden()
            } catch (e: Exception) {
                // Сервис мог быть остановлен — игнорируем.
            }
            try {
                unbindService(connection)
            } catch (e: Exception) {
                // Уже отвязаны — игнорируем.
            }
            bound = false
        }
        super.onStop()
    }

    /** Генерирует новый пример по текущим настройкам. */
    private fun nextProblem() {
        currentProblem = MathGenerator.generate(
            Prefs.selectedOperations(this),
            Prefs.difficulty(this)
        ).also { problemText.text = it.text }
        answerInput.setText("")
        messageText.text = ""
    }

    /** Проверяет введённый ответ. */
    private fun checkAnswer() {
        val problem = currentProblem ?: return
        val entered = answerInput.text.toString().trim().toIntOrNull()
        if (entered != null && entered == problem.answer) {
            finishAndGoHome()
        } else {
            vibrate()
            messageText.text = getString(R.string.wrong_answer)
            nextProblem()
        }
    }

    /** Короткая вибрация (400 мс). */
    private fun vibrate() {
        val vibrator = getSystemService<Vibrator>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION_MS)
        }
    }

    /** Разблокировал — закрываем экран и уходим на домашний. */
    private fun finishAndGoHome() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (e: Exception) {
            // Домашний экран недоступен — просто закрываем активность.
        }
        finish()
    }

    companion object {
        private const val VIBRATION_DURATION_MS = 400L
    }
}
