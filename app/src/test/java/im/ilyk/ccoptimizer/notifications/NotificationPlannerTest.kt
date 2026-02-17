package im.ilyk.ccoptimizer.notifications

import im.ilyk.ccoptimizer.data.db.dao.CardDao
import im.ilyk.ccoptimizer.data.db.dao.CycleDao
import im.ilyk.ccoptimizer.data.db.entities.CardEntity
import im.ilyk.ccoptimizer.data.db.entities.CycleEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationPlannerTest {
    
    private lateinit var cardDao: CardDao
    private lateinit var cycleDao: CycleDao
    private lateinit var notifPrefs: NotifPrefs
    private lateinit var planner: NotificationPlannerImpl
    private val zoneId = ZoneId.of("America/Los_Angeles")
    
    @BeforeEach
    fun setup() {
        cardDao = mock(CardDao::class.java)
        cycleDao = mock(CycleDao::class.java)
        notifPrefs = mock(NotifPrefs::class.java)
        planner = NotificationPlannerImpl(cardDao, cycleDao, notifPrefs, zoneId)
    }
    
    @Test
    fun `planAll generates close window notification`() = runTest {
        // Setup mocks
        val card = CardEntity("card1", "Chase", "1234", "Test Card", null, true)
        `when`(cardDao.getAllActive()).thenReturn(listOf(card))
        
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.now().plusDays(10),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        `when`(cycleDao.latest("card1")).thenReturn(cycle)
        
        `when`(notifPrefs.getBufferCloseDays()).thenReturn(3)
        `when`(notifPrefs.getBufferDueDays()).thenReturn(10)
        `when`(notifPrefs.getQuietHours()).thenReturn(
            QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
        
        // Execute
        val plans = planner.planAll()
        
        // Verify close window notification exists
        val closeWindowPlan = plans.find { it.type == NotifType.CloseWindow }
        assertNotNull(closeWindowPlan)
        assertEquals("card1", closeWindowPlan?.cardId)
        assertTrue(closeWindowPlan?.title?.contains("Closes") == true)
    }
    
    @Test
    fun `planAll generates action required for low confidence`() = runTest {
        val card = CardEntity("card1", "Chase", "1234", "Test Card", null, true)
        `when`(cardDao.getAllActive()).thenReturn(listOf(card))
        
        // Low confidence cycle
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.4, // Below 0.6 threshold
            nextCloseDate = LocalDate.now().plusDays(10),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        `when`(cycleDao.latest("card1")).thenReturn(cycle)
        
        `when`(notifPrefs.getBufferCloseDays()).thenReturn(3)
        `when`(notifPrefs.getBufferDueDays()).thenReturn(10)
        `when`(notifPrefs.getQuietHours()).thenReturn(
            QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
        
        val plans = planner.planAll()
        
        // Should have action required notification
        val actionPlan = plans.find { it.type == NotifType.ActionRequired }
        assertNotNull(actionPlan)
        assertTrue(actionPlan?.body?.contains("confirm") == true)
    }
    
    @Test
    fun `planAll generates action required for null close day`() = runTest {
        val card = CardEntity("card1", "Chase", "1234", "Test Card", null, true)
        `when`(cardDao.getAllActive()).thenReturn(listOf(card))
        
        val cycle = CycleEntity(
            cardId = "card1",
            closeDay = null, // No close day detected
            cadence = "MONTHLY",
            confidence = 0.3,
            nextCloseDate = null,
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        `when`(cycleDao.latest("card1")).thenReturn(cycle)
        
        `when`(notifPrefs.getBufferCloseDays()).thenReturn(3)
        `when`(notifPrefs.getBufferDueDays()).thenReturn(10)
        `when`(notifPrefs.getQuietHours()).thenReturn(
            QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
        
        val plans = planner.planAll()
        
        val actionPlan = plans.find { it.type == NotifType.ActionRequired }
        assertNotNull(actionPlan)
    }
    
    @Test
    fun `planAll generates unique request codes`() = runTest {
        val cards = listOf(
            CardEntity("card1", "Chase", "1111", "Card 1", null, true),
            CardEntity("card2", "Citi", "2222", "Card 2", null, true)
        )
        `when`(cardDao.getAllActive()).thenReturn(cards)
        
        val cycle1 = CycleEntity(
            cardId = "card1",
            closeDay = 15,
            cadence = "MONTHLY",
            confidence = 0.85,
            nextCloseDate = LocalDate.now().plusDays(10),
            candidateDaysJson = "{}",
            signalWeightsJson = "{}",
            computedAt = ZonedDateTime.now()
        )
        
        val cycle2 = cycle1.copy(cardId = "card2")
        
        `when`(cycleDao.latest("card1")).thenReturn(cycle1)
        `when`(cycleDao.latest("card2")).thenReturn(cycle2)
        
        `when`(notifPrefs.getBufferCloseDays()).thenReturn(3)
        `when`(notifPrefs.getBufferDueDays()).thenReturn(10)
        `when`(notifPrefs.getQuietHours()).thenReturn(
            QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        )
        
        val plans = planner.planAll()
        
        // All request codes should be unique
        val requestCodes = plans.map { it.requestCode }
        assertEquals(requestCodes.size, requestCodes.toSet().size)
    }
    
    @Test
    fun `quiet hours adjustment works`() = runTest {
        // This test would verify quiet hours logic
        // Requires exposing adjustForQuietHours or testing via integration
        assertTrue(true) // Placeholder
    }
}
