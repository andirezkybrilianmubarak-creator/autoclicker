package com.example.autoclicker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var accessibilityStatusText: TextView
    private lateinit var sessionStatusText: TextView
    private lateinit var openSettingsButton: Button
    private lateinit var toggleSessionButton: Button

    private val notificationPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accessibilityStatusText = findViewById(R.id.accessibilityStatusText)
        sessionStatusText = findViewById(R.id.sessionStatusText)
        openSettingsButton = findViewById(R.id.openSettingsButton)
        toggleSessionButton = findViewById(R.id.toggleSessionButton)

        openSettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        toggleSessionButton.setOnClickListener {
            onToggleSessionClicked()
        }

        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibilityStatus()
        refreshSessionStatus()
    }

    private fun onToggleSessionClicked() {
        if (!isAccessibilityServiceEnabled()) {
            accessibilityStatusText.text = getString(R.string.status_disabled)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        if (SessionState.isSessionActive) {
            SessionState.stopSession()
            val stopIntent = Intent(this, AutomationForegroundService::class.java)
            stopService(stopIntent)
        } else {
            SessionState.startSession()
            val startIntent = Intent(this, AutomationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        }
        refreshSessionStatus()
    }

    private fun refreshSessionStatus() {
        if (SessionState.isSessionActive) {
            sessionStatusText.text = getString(R.string.session_active)
            sessionStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            toggleSessionButton.text = getString(R.string.btn_stop_session)
            toggleSessionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_red))
        } else {
            sessionStatusText.text = getString(R.string.session_inactive)
            sessionStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            toggleSessionButton.text = getString(R.string.btn_start_session)
            toggleSessionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_green))
        }
    }

    private fun refreshAccessibilityStatus() {
        if (isAccessibilityServiceEnabled()) {
            accessibilityStatusText.text = getString(R.string.status_enabled)
            accessibilityStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            openSettingsButton.isEnabled = false
        } else {
            accessibilityStatusText.text = getString(R.string.status_disabled)
            accessibilityStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            openSettingsButton.isEnabled = true

            // If the service got disabled externally while a session was marked active,
            // force the session off so gestures can never be attempted without the service.
            if (SessionState.isSessionActive) {
                SessionState.stopSession()
                stopService(Intent(this, AutomationForegroundService::class.java))
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${MyAccessibilityService::class.java.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    notificationPermissionRequestCode
                )
            }
        }
    }
}
