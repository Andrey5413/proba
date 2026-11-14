package com.example.mathlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Фоновый сервис-«сторож».
 *  - работает как foreground-сервис (тип specialUse) с уведомлением «Открыть»;
 *  - показывает overlay-заглушку поверх всех приложений;
 *  - слушает ACTION_SCREEN_ON / ACTION_USER_PRESENT и при включении экрана
 *    вызывает LockActivity (если включено в настройках);
 *  - если LockActivity сворачивают на домашний экран, overlay появляется снова,
 *    поэтому пример всегда виден поверх системы.
 */
class LockService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var activityVisible = false

    /** Простой локальный binder — позволяет LockActivity управлять overlay. */
    inner class LocalBinder : Binder() {
        fun getService(): LockService = this@LockService
    }

    /** Приёмник событий экрана. Регистрируется внутри сервиса. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> maybeShowLock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        showOverlay()
        registerScreenReceiver()
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Приёмник мог не успеть зарегистрироваться — игнорируем.
        }
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    /** Вызывается из LockActivity: считать экран блокировки видимым, спрятать overlay. */
    fun onLockActivityShown() {
        activityVisible = true
        hideOverlay()
    }

    /** Вызывается из LockActivity: экран ушёл на задний план — показываем overlay снова. */
    fun onLockActivityHidden() {
        activityVisible = false
        showOverlay()
    }

    private fun maybeShowLock() {
        if (!Prefs.lockOnScreenOn(this)) return
        if (activityVisible) return
        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_lock_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ещё не зарегистрирован — это нормально.
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_need_overlay, Toast.LENGTH_LONG).show()
            return
        }
        val wm = windowManager ?: return
        val view = LayoutInflater.from(this)
            .inflate(R.layout.overlay_lock, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // ВАЖНО: флага FLAG_NOT_FOCUSABLE НЕТ — экран получает фокус ввода
            // и пользователь может ввести ответ.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        try {
            wm.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: Exception) {
            // View могла быть уже удалена — игнорируем.
        }
    }

    private fun startAsForeground() {
        createNotificationChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService<NotificationManager>() ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "mathlock_service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val serviceIntent = Intent(context, LockService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }

        /**
         * Показывает экран блокировки (для кнопки «Тест примера» и для overlay).
         * Если overlay ещё не показан — поднимает сервис.
         */
        fun ensureVisible(context: Context) {
            start(context)
            val lockIntent = Intent(context, LockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            }
            try {
                context.startActivity(lockIntent)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.toast_lock_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
