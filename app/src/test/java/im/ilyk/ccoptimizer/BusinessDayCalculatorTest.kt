package im.ilyk.ccoptimizer

import im.ilyk.ccoptimizer.utils.BusinessDayCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class BusinessDayCalculatorTest {
    
    @Test
    fun testIsWeekend() {
        val saturday = LocalDate.of(2025, 10, 18) // Saturday
        val sunday = LocalDate.of(2025, 10, 19)   // Sunday
        val monday = LocalDate.of(2025, 10, 13)   // Monday
        
        assertTrue(BusinessDayCalculator.isWeekend(saturday))
        assertTrue(BusinessDayCalculator.isWeekend(sunday))
        assertFalse(BusinessDayCalculator.isWeekend(monday))
    }
    
    @Test
    fun testFederalHolidays2025() {
        val holidays = BusinessDayCalculator.getFederalHolidays(2025)
        
        // New Year's Day (January 1, 2025 is Wednesday)
        assertTrue(holidays.contains(LocalDate.of(2025, Month.JANUARY, 1)))
        
        // Independence Day (July 4, 2025 is Friday)
        assertTrue(holidays.contains(LocalDate.of(2025, Month.JULY, 4)))
        
        // Christmas (December 25, 2025 is Thursday)
        assertTrue(holidays.contains(LocalDate.of(2025, Month.DECEMBER, 25)))
    }
    
    @Test
    fun testObservedHolidayOnWeekend() {
        // Test year where some holidays fall on weekends
        val holidays = BusinessDayCalculator.getFederalHolidays(2026)
        
        // July 4, 2026 is Saturday, should be observed on Friday July 3
        assertFalse(holidays.contains(LocalDate.of(2026, Month.JULY, 4)))
        assertTrue(holidays.contains(LocalDate.of(2026, Month.JULY, 3)))
    }
    
    @Test
    fun testGetBusinessDaysBefore() {
        // Start from Wednesday October 15, 2025
        val targetDate = LocalDate.of(2025, Month.OCTOBER, 15)
        
        // 1 business day before should be Tuesday October 14
        val oneDayBefore = BusinessDayCalculator.getBusinessDaysBefore(targetDate, 1)
        assertEquals(LocalDate.of(2025, Month.OCTOBER, 14), oneDayBefore)
        
        // 3 business days before should be Friday October 10 (skips weekend)
        val threeDaysBefore = BusinessDayCalculator.getBusinessDaysBefore(targetDate, 3)
        assertEquals(LocalDate.of(2025, Month.OCTOBER, 10), threeDaysBefore)
    }
    
    @Test
    fun testGetBusinessDaysBeforeWithHoliday() {
        // Test around a holiday
        // If we're looking at the Monday after July 4 (Friday)
        // July 4, 2025 is Friday, July 5-6 is weekend, July 7 is Monday
        val targetDate = LocalDate.of(2025, Month.JULY, 7) // Monday
        
        // 1 business day before Monday should be Thursday July 3
        // (skipping Friday July 4 holiday and weekend)
        val oneDayBefore = BusinessDayCalculator.getBusinessDaysBefore(targetDate, 1)
        assertEquals(LocalDate.of(2025, Month.JULY, 3), oneDayBefore)
    }
    
    @Test
    fun testIsBusinessDay() {
        // Regular weekday
        val monday = LocalDate.of(2025, Month.OCTOBER, 13)
        assertTrue(BusinessDayCalculator.isBusinessDay(monday))
        
        // Weekend
        val saturday = LocalDate.of(2025, Month.OCTOBER, 18)
        assertFalse(BusinessDayCalculator.isBusinessDay(saturday))
        
        // Holiday (July 4, 2025 is Friday)
        val july4 = LocalDate.of(2025, Month.JULY, 4)
        assertFalse(BusinessDayCalculator.isBusinessDay(july4))
    }
}

