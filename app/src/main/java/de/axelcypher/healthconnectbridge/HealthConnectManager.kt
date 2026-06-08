package de.axelcypher.healthconnectbridge

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE,
}

class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }

    suspend fun hasWritePermission(): Boolean {
        if (availability() != HealthConnectAvailability.AVAILABLE) return false
        return client().permissionController.getGrantedPermissions().contains(WRITE_WEIGHT_PERMISSION)
    }

    suspend fun writeWeight(weightKg: Double, instant: Instant) {
        val zoneOffset: ZoneOffset = ZoneOffset.systemDefault().rules.getOffset(instant)
        val record = WeightRecord(
            time = instant,
            zoneOffset = zoneOffset,
            weight = Mass.kilograms(weightKg),
            metadata = Metadata.manualEntry(),
        )
        client().insertRecords(listOf(record))
    }

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.getHealthConnectSettingsAction())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(appContext)

    companion object {
        val WRITE_WEIGHT_PERMISSION: String =
            HealthPermission.getWritePermission(WeightRecord::class)
        val REQUIRED_PERMISSIONS: Set<String> = setOf(WRITE_WEIGHT_PERMISSION)
    }
}

