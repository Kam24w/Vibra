package com.vibra.navigation.data.parser

import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.UserAction
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.model.VibrationPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEventParserTest {

    private val parser = NavigationEventParser()

    private fun parse(json: String) = parser.parse(json.encodeToByteArray())

    @Test
    fun `parses the documented contract`() {
        val result = parse(
            """
            {
              "action": "TURN_LEFT",
              "instruction": "Gira a la izquierda en 50 metros",
              "distance_meters": 50,
              "vibration_pattern": { "pulses": 2, "duration_ms": 150, "amplitude": 255 },
              "timestamp": 1778344620000
            }
            """,
        )

        val event = (result as ParseResult.Success).event
        assertEquals(NavigationAction.TURN_LEFT, event.action)
        assertEquals("Gira a la izquierda en 50 metros", event.instruction)
        assertEquals(50, event.distanceMeters)
        assertEquals(VibrationPattern(pulses = 2, durationMs = 150, amplitude = 255), event.vibrationPattern)
        assertEquals(1778344620000L, event.timestamp)
    }

    @Test
    fun `unknown action and extra fields are tolerated`() {
        val result = parse("""{"action":"ROUNDABOUT_EXIT_3","foo":true,"timestamp":1}""")

        val event = (result as ParseResult.Success).event
        assertEquals(NavigationAction.UNKNOWN, event.action)
        assertNull(event.vibrationPattern)
    }

    @Test
    fun `malformed json fails without throwing`() {
        assertTrue(parse("""{"action": "TURN_LEFT", """) is ParseResult.Failure)
        assertTrue(parse("not json") is ParseResult.Failure)
        assertTrue(parser.parse(ByteArray(0)) is ParseResult.Failure)
    }

    @Test
    fun `missing timestamp fails validation`() {
        assertTrue(parse("""{"action":"TURN_LEFT"}""") is ParseResult.Failure)
    }

    @Test
    fun `negative distance fails validation`() {
        assertTrue(parse("""{"action":"TURN_LEFT","distance_meters":-5,"timestamp":1}""") is ParseResult.Failure)
    }

    @Test
    fun `wrong type fails without throwing`() {
        assertTrue(parse("""{"action":"TURN_LEFT","distance_meters":"cincuenta","timestamp":1}""") is ParseResult.Failure)
    }

    @Test
    fun `encodes user action with snake case enum`() {
        val json = parser.encode(UserAction(UserActionType.STOP_NAVIGATION, 42L)).decodeToString()
        assertEquals("""{"action":"STOP_NAVIGATION","timestamp":42}""", json)
    }
    @Test
    fun `encodes pause and resume guidance actions`() {
        assertEquals(
            """{"action":"PAUSE_GUIDANCE","timestamp":1}""",
            parser.encode(UserAction(UserActionType.PAUSE_GUIDANCE, 1L)).decodeToString(),
        )
        assertEquals(
            """{"action":"RESUME_GUIDANCE","timestamp":2}""",
            parser.encode(UserAction(UserActionType.RESUME_GUIDANCE, 2L)).decodeToString(),
        )
    }
}
