package de.axelcypher.healthconnectbridge

import android.content.Intent
import java.time.Instant

data class ReceivedWeight(
    val weightKg: Double,
    val instant: Instant,
    val source: String?,
    val usedTimestampFallback: Boolean,
)

sealed interface ParseResult {
    data class Success(val value: ReceivedWeight) : ParseResult
    data class Error(val message: String) : ParseResult
}

object WeightIntentParser {
    fun parse(intent: Intent, now: () -> Instant = Instant::now): ParseResult {
        val weightKg = parseWeight(intent.extras?.get(EXTRA_WEIGHT))
            ?: return ParseResult.Error("Broadcast ignored: missing or invalid weight_kg.")

        if (weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            return ParseResult.Error(
                "Broadcast ignored: weight must be between 20 and 300 kg.",
            )
        }

        val source = intent.getStringExtra(EXTRA_SOURCE)?.trim()?.takeIf(String::isNotEmpty)
        if (source != null && !source.equals(EXPECTED_SOURCE, ignoreCase = true)) {
            return ParseResult.Error("Broadcast ignored: unsupported source '$source'.")
        }

        val timestampText = intent.extras?.get(EXTRA_TIMESTAMP)?.toString()
        val parsedTimestamp = timestampText?.let { runCatching { Instant.parse(it.trim()) }.getOrNull() }
        val usedFallback = parsedTimestamp == null

        return ParseResult.Success(
            ReceivedWeight(
                weightKg = weightKg,
                instant = parsedTimestamp ?: now(),
                source = source,
                usedTimestampFallback = usedFallback,
            ),
        )
    }

    internal fun parseWeight(raw: Any?): Double? {
        val value = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().replace(',', '.').toDoubleOrNull()
            else -> null
        }
        return value?.takeIf(Double::isFinite)
    }

    const val ACTION_WRITE_WEIGHT =
        "de.axelcypher.healthconnectbridge.WRITE_WEIGHT"
    private const val EXTRA_WEIGHT = "weight_kg"
    private const val EXTRA_TIMESTAMP = "timestamp"
    private const val EXTRA_SOURCE = "source"
    private const val EXPECTED_SOURCE = "homeassistant"
    private const val MIN_WEIGHT_KG = 20.0
    private const val MAX_WEIGHT_KG = 300.0
}

