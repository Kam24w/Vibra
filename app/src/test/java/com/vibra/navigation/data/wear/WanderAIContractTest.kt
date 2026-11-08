package com.vibra.navigation.data.wear

import com.vibra.navigation.data.model.NavigationAction
import com.vibra.navigation.data.model.UserAction
import com.vibra.navigation.data.model.UserActionType
import com.vibra.navigation.data.model.VibrationPattern
import com.vibra.navigation.data.parser.NavigationEventParser
import com.vibra.navigation.data.parser.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Payloads tal y como los genera WanderAI (`WearProtocol` del teléfono) y como este
 * los espera. Si alguno cambia en un lado, estos tests deben fallar.
 */
class WanderAIContractTest {

    private val parser = NavigationEventParser()

    /** Salida literal de `WearProtocol.encodeNavigationEvent` en WanderAI. */
    private fun phonePayload(action: String) =
        """{"action":"$action","instruction":"Atención: revisa tu ruta en el teléfono","distance_meters":0,""" +
            """"vibration_pattern":{"pulses":5,"duration_ms":100,"amplitude":255},"timestamp":1778344620000}"""

    @Test
    fun `every action the phone sends is recognised`() {
        // NavigationAction de WanderAI: TURN_LEFT, TURN_RIGHT, U_TURN, STRAIGHT, ARRIVED, ALERT.
        listOf("TURN_LEFT", "TURN_RIGHT", "U_TURN", "STRAIGHT", "ARRIVED", "ALERT").forEach { name ->
            val result = parser.parse(phonePayload(name).encodeToByteArray())
            val event = (result as ParseResult.Success).event
            assertNotEquals("$name no debe llegar como UNKNOWN", NavigationAction.UNKNOWN, event.action)
            assertEquals(name, event.action.name)
        }
    }

    @Test
    fun `alert keeps the phone vibration pattern and text`() {
        val event = (parser.parse(phonePayload("ALERT").encodeToByteArray()) as ParseResult.Success).event

        assertEquals(NavigationAction.ALERT, event.action)
        assertEquals("Atención: revisa tu ruta en el teléfono", event.instruction)
        assertEquals(0, event.distanceMeters)
        assertEquals(VibrationPattern(pulses = 5, durationMs = 100, amplitude = 255), event.vibrationPattern)
        assertEquals(1_778_344_620_000L, event.timestamp)
    }

    @Test
    fun `user actions use the names WanderAI decodes`() {
        // WatchUserAction del teléfono (ACKNOWLEDGE se traduce a ACK allí).
        val expected = mapOf(
            UserActionType.REPEAT_INSTRUCTION to "REPEAT_INSTRUCTION",
            UserActionType.PAUSE_GUIDANCE to "PAUSE_GUIDANCE",
            UserActionType.RESUME_GUIDANCE to "RESUME_GUIDANCE",
            UserActionType.STOP_NAVIGATION to "STOP_NAVIGATION",
            UserActionType.ACKNOWLEDGE to "ACKNOWLEDGE",
        )
        assertEquals(UserActionType.entries.toSet(), expected.keys)
        expected.forEach { (type, name) ->
            assertEquals(
                """{"action":"$name","timestamp":7}""",
                parser.encode(UserAction(type, 7L)).decodeToString(),
            )
        }
    }

    @Test
    fun `paths match the phone protocol`() {
        assertEquals("/wanderai/navigation_event", DataLayerContract.PATH_NAVIGATION_EVENT)
        assertEquals("/wanderai/active_route", DataLayerContract.PATH_ACTIVE_ROUTE)
        assertEquals("/vibra/user_action", DataLayerContract.PATH_USER_ACTION)
    }
}
