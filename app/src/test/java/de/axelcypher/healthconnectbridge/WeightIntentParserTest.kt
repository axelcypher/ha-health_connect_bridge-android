package de.axelcypher.healthconnectbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightIntentParserTest {
    @Test
    fun parsesPointDecimalString() {
        assertEquals(89.4, WeightIntentParser.parseWeight("89.4")!!, 0.0)
    }

    @Test
    fun parsesCommaDecimalString() {
        assertEquals(89.4, WeightIntentParser.parseWeight("89,4")!!, 0.0)
    }

    @Test
    fun parsesNumericTypes() {
        assertEquals(89.4, WeightIntentParser.parseWeight(89.4f)!!, 0.0001)
        assertEquals(89.4, WeightIntentParser.parseWeight(89.4)!!, 0.0)
    }

    @Test
    fun rejectsInvalidAndNonFiniteValues() {
        assertNull(WeightIntentParser.parseWeight("not-a-number"))
        assertNull(WeightIntentParser.parseWeight(Double.NaN))
        assertNull(WeightIntentParser.parseWeight(Double.POSITIVE_INFINITY))
    }
}
