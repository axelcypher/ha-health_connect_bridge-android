package de.axelcypher.healthconnectbridge

import java.math.BigDecimal
import java.time.Instant

class DuplicateGuard(private val prefs: AppPrefs) {
    fun hash(weightKg: Double, instant: Instant): String {
        val normalizedWeight = BigDecimal.valueOf(weightKg).stripTrailingZeros().toPlainString()
        return "$instant|$normalizedWeight"
    }

    fun isDuplicate(hash: String): Boolean = prefs.lastWrittenHash() == hash

    fun clear() = prefs.clearDuplicateState()
}

