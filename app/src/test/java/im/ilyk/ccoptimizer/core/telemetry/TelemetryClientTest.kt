package im.ilyk.ccoptimizer.core.telemetry

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for TelemetryClient
 */
@RunWith(MockitoJUnitRunner::class)
class TelemetryClientTest {

    @Mock
    private lateinit var mockConfig: TelemetryConfig

    private lateinit var context: Context
    private lateinit var telemetryClient: TelemetryClient

    @Before
    fun setup() {
        context = mock(Context::class.java)
        telemetryClient = TelemetryClient(context, mockConfig)
    }

    @Test
    fun `preview generates valid JSON`() {
        val event = TelemetryEvent.ScreenView("test_screen")
        val preview = telemetryClient.preview(event)

        assertTrue(preview.contains("\"event\""))
        assertTrue(preview.contains("\"screen_view\""))
        assertTrue(preview.contains("\"screen_name\""))
        assertTrue(preview.contains("\"test_screen\""))
        assertTrue(preview.contains("\"timestamp\""))
        assertTrue(preview.contains("\"app_version\""))
    }

    @Test
    fun `preview action event includes metadata`() {
        val metadata = mapOf("action_type" to "delete", "confirmed" to "true")
        val event = TelemetryEvent.Action("data_deleted", metadata)
        val preview = telemetryClient.preview(event)

        assertTrue(preview.contains("\"action\""))
        assertTrue(preview.contains("\"action_name\""))
        assertTrue(preview.contains("\"data_deleted\""))
        assertTrue(preview.contains("\"metadata\""))
        assertTrue(preview.contains("\"action_type\""))
    }

    @Test
    fun `preview does not contain PII`() {
        val event = TelemetryEvent.ScreenView("card_details")
        val preview = telemetryClient.preview(event)

        // Should NOT contain any sensitive fields
        assertFalse(preview.contains("balance"))
        assertFalse(preview.contains("cardId"))
        assertFalse(preview.contains("email"))
        assertFalse(preview.contains("token"))
        assertFalse(preview.contains("device_id"))
    }

    @Test
    fun `preview includes only minimal fields`() {
        val event = TelemetryEvent.ScreenView("main")
        val preview = telemetryClient.preview(event)

        // Count number of top-level fields (should be exactly 4)
        val fieldCount = preview.lines()
            .filter { it.trim().startsWith("\"") && it.contains(":") }
            .count()

        // Should have: event, timestamp, app_version, screen_name
        assertEquals(4, fieldCount)
    }
}

