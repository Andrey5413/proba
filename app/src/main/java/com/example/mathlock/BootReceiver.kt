package com.example.mathlock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Автозапуск сторожевого сервиса после перезагрузки телефона,
 * если в настройках включён пункт «Блокировать при включении экрана».
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            if (Prefs.lockOnScreenOn(context)) {
                LockService.start(context)
            }
        }
    }
}
