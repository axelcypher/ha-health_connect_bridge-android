package de.axelcypher.healthconnectbridge

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeightSyncRepository(context: Context) {
    private val prefs = AppPrefs(context)
    private val duplicateGuard = DuplicateGuard(prefs)
    private val healthConnect = HealthConnectManager(context)

    suspend fun process(received: ReceivedWeight) = writeMutex.withLock {
        prefs.recordReceived(received.weightKg, received.instant, received.source)
        if (received.usedTimestampFallback) {
            prefs.addLog("Invalid or missing timestamp; current time used")
        }

        when (healthConnect.availability()) {
            HealthConnectAvailability.UNAVAILABLE -> {
                prefs.recordError("Health Connect is not available on this device.")
                return@withLock
            }
            HealthConnectAvailability.UPDATE_REQUIRED -> {
                prefs.recordError("Health Connect is missing or must be updated.")
                return@withLock
            }
            HealthConnectAvailability.AVAILABLE -> Unit
        }

        if (!healthConnect.hasWritePermission()) {
            prefs.recordError(
                "Weight received but Health Connect WRITE_WEIGHT permission is missing.",
            )
            return@withLock
        }

        val hash = duplicateGuard.hash(received.weightKg, received.instant)
        if (duplicateGuard.isDuplicate(hash)) {
            prefs.addLog("Duplicate ignored.")
            return@withLock
        }

        try {
            healthConnect.writeWeight(received.weightKg, received.instant)
            prefs.recordWritten(received.weightKg, received.instant, hash)
        } catch (exception: Exception) {
            prefs.recordError(
                "Health Connect write failed: ${exception.message ?: exception.javaClass.simpleName}",
            )
        }
    }

    companion object {
        private val writeMutex = Mutex()
    }
}

