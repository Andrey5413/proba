package com.example.mathlock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Получатель событий Device Admin. Нужен для кнопки «Заблокировать сейчас»
 * (DevicePolicyManager.lockNow()).
 */
class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, R.string.toast_admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, R.string.toast_admin_disabled, Toast.LENGTH_SHORT).show()
    }
}
