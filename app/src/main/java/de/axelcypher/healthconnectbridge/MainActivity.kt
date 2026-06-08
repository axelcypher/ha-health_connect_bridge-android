package de.axelcypher.healthconnectbridge

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import de.axelcypher.healthconnectbridge.ui.MainScreen
import de.axelcypher.healthconnectbridge.ui.MainUiState
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {
    private lateinit var appPrefs: AppPrefs
    private lateinit var healthConnect: HealthConnectManager
    private var uiState = MainUiState()

    @Suppress("UNCHECKED_CAST")
    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
            as ActivityResultContract<Set<String>, Set<String>>,
    ) {
        lifecycleScope.launch { refreshStatus() }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        runOnUiThread { render() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPrefs = AppPrefs(this)
        healthConnect = HealthConnectManager(this)
        appPrefs.registerListener(prefsListener)
        render()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { refreshStatus() }
    }

    override fun onDestroy() {
        appPrefs.unregisterListener(prefsListener)
        super.onDestroy()
    }

    private fun render() {
        uiState = uiState.copy(status = appPrefs.snapshot())
        setContent {
            MainScreen(
                state = uiState,
                onGrantPermission = {
                    if (healthConnect.availability() == HealthConnectAvailability.AVAILABLE) {
                        permissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                    } else {
                        appPrefs.recordError("Health Connect is not available for permission request.")
                    }
                },
                onOpenSettings = {
                    runCatching { startActivity(healthConnect.settingsIntent()) }
                        .onFailure {
                            appPrefs.recordError("Could not open Health Connect settings.")
                        }
                },
                onSendTestWeight = ::sendTestWeight,
                onClearDuplicateState = { DuplicateGuard(appPrefs).clear() },
            )
        }
    }

    private suspend fun refreshStatus() {
        val availability = healthConnect.availability()
        val permissionGranted = runCatching {
            availability == HealthConnectAvailability.AVAILABLE &&
                healthConnect.hasWritePermission()
        }.getOrDefault(false)
        uiState = uiState.copy(
            availability = availability,
            permissionGranted = permissionGranted,
            status = appPrefs.snapshot(),
        )
        render()
    }

    private fun sendTestWeight() {
        val intent = Intent(WeightIntentParser.ACTION_WRITE_WEIGHT)
            .setPackage(packageName)
            .putExtra("weight_kg", 89.4)
            .putExtra("timestamp", Instant.now().toString())
            .putExtra("source", "homeassistant")
        sendBroadcast(intent)
    }
}

