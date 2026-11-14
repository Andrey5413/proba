package com.example.mathlock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * Главный экран: краткое объяснение работы, кнопки управления
 * и индикаторы выданных разрешений.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var overlayStatus: TextView
    private lateinit var adminStatus: TextView

    // Запрос разрешения Device Admin (результат не важен — статус обновим в onResume).
    private val adminRequester =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // Запрос разрешения на уведомления (Android 13+).
    private val notificationRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayStatus = findViewById(R.id.textOverlayStatus)
        adminStatus = findViewById(R.id.textAdminStatus)

        findViewById<Button>(R.id.buttonPermissions).setOnClickListener {
            requestAllPermissions()
        }
        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonLockNow).setOnClickListener {
            lockDeviceNow()
        }
        findViewById<Button>(R.id.buttonTest).setOnClickListener {
            showLockScreen()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                notificationRequester.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    /** Обновляет индикаторы «Поверх других приложений» и Device Admin. */
    private fun updateStatus() {
        val overlayGranted = Settings.canDrawOverlays(this)
        overlayStatus.text = getString(
            R.string.overlay_status,
            getString(if (overlayGranted) R.string.status_granted else R.string.status_not_granted)
        )
        val dpm = getSystemService<DevicePolicyManager>()
        val adminActive = dpm != null &&
            dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))
        adminStatus.text = getString(
            R.string.admin_status,
            getString(if (adminActive) R.string.status_granted else R.string.status_not_granted)
        )
    }

    /** Открывает настройки overlay, запрашивает Device Admin и оптимизацию батареи. */
    private fun requestAllPermissions() {
        // 1. Разрешение «Поверх других приложений».
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(this, R.string.toast_open_settings_failed, Toast.LENGTH_LONG).show()
            }
        }

        // 2. Device Admin (нужен для мгновенной блокировки экрана).
        val dpm = getSystemService<DevicePolicyManager>()
        if (dpm != null) {
            val admin = ComponentName(this, AdminReceiver::class.java)
            if (!dpm.isAdminActive(admin)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.device_admin_explanation)
                    )
                try {
                    adminRequester.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.toast_open_settings_failed, Toast.LENGTH_LONG).show()
                }
            }
        }

        // 3. Отключение оптимизации батареи (сервис должен жить постоянно).
        val pm = getSystemService<PowerManager>()
        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                @Suppress("BatteryLife")
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (e2: Exception) {
                    Toast.makeText(this, R.string.toast_open_settings_failed, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Заодно запускаем сервис блокировки.
        LockService.start(this)
    }

    /** Мгновенная блокировка экрана через Device Admin. */
    private fun lockDeviceNow() {
        val dpm = getSystemService<DevicePolicyManager>()
        val admin = ComponentName(this, AdminReceiver::class.java)
        if (dpm != null && dpm.isAdminActive(admin)) {
            dpm.lockNow()
        } else {
            Toast.makeText(this, R.string.toast_need_admin, Toast.LENGTH_LONG).show()
        }
    }

    /** Тестовый показ экрана с примером («Тест примера»). */
    private fun showLockScreen() {
        LockService.ensureVisible(this)
    }
}
