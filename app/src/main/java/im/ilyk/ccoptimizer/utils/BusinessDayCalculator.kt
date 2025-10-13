package im.ilyk.ccoptimizer.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

object BusinessDayCalculator {
    
    /**
     * Calculates the business day that is N business days before the target date,
     * accounting for weekends and federal holidays.
     */
    fun getBusinessDaysBefore(targetDate: LocalDate, businessDays: Int): LocalDate {
        var date = targetDate
        var remainingDays = businessDays
        
        while (remainingDays > 0) {
            date = date.minusDays(1)
            if (isBusinessDay(date)) {
                remainingDays--
            }
        }
        
        return date
    }
    
    /**
     * Checks if a given date is a business day (not weekend or federal holiday)
     */
    fun isBusinessDay(date: LocalDate): Boolean {
        return !isWeekend(date) && !isFederalHoliday(date)
    }
    
    /**
     * Checks if a date falls on a weekend
     */
    fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
    
    /**
     * Checks if a date is a US federal holiday
     */
    fun isFederalHoliday(date: LocalDate): Boolean {
        val year = date.year
        val holidays = getFederalHolidays(year)
        return holidays.contains(date)
    }
    
    /**
     * Returns a list of US federal holidays for a given year
     */
    fun getFederalHolidays(year: Int): List<LocalDate> {
        return listOf(
            // New Year's Day (January 1)
            observedDate(LocalDate.of(year, Month.JANUARY, 1)),
            
            // Martin Luther King Jr. Day (3rd Monday in January)
            getNthDayOfMonth(year, Month.JANUARY, DayOfWeek.MONDAY, 3),
            
            // Presidents' Day (3rd Monday in February)
            getNthDayOfMonth(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3),
            
            // Memorial Day (last Monday in May)
            getLastDayOfMonth(year, Month.MAY, DayOfWeek.MONDAY),
            
            // Juneteenth (June 19)
            observedDate(LocalDate.of(year, Month.JUNE, 19)),
            
            // Independence Day (July 4)
            observedDate(LocalDate.of(year, Month.JULY, 4)),
            
            // Labor Day (1st Monday in September)
            getNthDayOfMonth(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1),
            
            // Columbus Day (2nd Monday in October)
            getNthDayOfMonth(year, Month.OCTOBER, DayOfWeek.MONDAY, 2),
            
            // Veterans Day (November 11)
            observedDate(LocalDate.of(year, Month.NOVEMBER, 11)),
            
            // Thanksgiving (4th Thursday in November)
            getNthDayOfMonth(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4),
            
            // Christmas Day (December 25)
            observedDate(LocalDate.of(year, Month.DECEMBER, 25))
        )
    }
    
    /**
     * If a holiday falls on a Saturday, it's observed on Friday.
     * If it falls on a Sunday, it's observed on Monday.
     */
    private fun observedDate(date: LocalDate): LocalDate {
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.minusDays(1)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
    }
    
    /**
     * Gets the Nth occurrence of a specific day of week in a month
     */
    private fun getNthDayOfMonth(year: Int, month: Month, dayOfWeek: DayOfWeek, n: Int): LocalDate {
        var date = LocalDate.of(year, month, 1)
        var count = 0
        
        while (date.month == month) {
            if (date.dayOfWeek == dayOfWeek) {
                count++
                if (count == n) {
                    return date
                }
            }
            date = date.plusDays(1)
        }
        
        throw IllegalStateException("Could not find $n-th $dayOfWeek in $month $year")
    }
    
    /**
     * Gets the last occurrence of a specific day of week in a month
     */
    private fun getLastDayOfMonth(year: Int, month: Month, dayOfWeek: DayOfWeek): LocalDate {
        var date = LocalDate.of(year, month, month.length(isLeapYear(year)))
        
        while (date.month == month) {
            if (date.dayOfWeek == dayOfWeek) {
                return date
            }
            date = date.minusDays(1)
        }
        
        throw IllegalStateException("Could not find last $dayOfWeek in $month $year")
    }
    
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

