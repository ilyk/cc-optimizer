# Payment Calculation

Core algorithm for calculating optimal credit card payment dates.

## Overview

The Payment Calculator is the heart of the application. It determines when and how much you should pay on each credit card to minimize your statement balance, which improves your credit utilization ratio and credit score.

## The Problem

**Credit Utilization Impact**:
- Credit card companies report your **statement balance** to credit bureaus
- This happens when your statement closes (generates), not when you pay
- High utilization (>30%) negatively impacts your credit score
- Paying after the statement closes doesn't help the reported balance

**Example Problem**:
```
Month of September:
- You spend $3,000 on your credit card
- Statement closes on September 18th
- Balance reported: $3,000 (even if you pay in full on September 20th)
- Credit limit: $10,000
- Utilization reported: 30% 😞
```

## The Solution

**Pay Before Statement Closes**:
```
Month of September:
- You spend $3,000 on your credit card
- You pay $3,000 on September 15th (before statement closes)
- Statement closes on September 18th
- Balance reported: $0
- Credit limit: $10,000
- Utilization reported: 0% 🎉
```

**But wait** - you need to account for bank processing times!

## Algorithm

### Step 1: Get Next Statement Date

```kotlin
fun getNextStatementDate(fromDate: LocalDate, statementDay: Int): LocalDate {
    // Handle month-end edge cases (e.g., day 31 in February)
    val dayThisMonth = minOf(statementDay, fromDate.lengthOfMonth())
    val currentMonth = fromDate.withDayOfMonth(dayThisMonth)
    
    return if (currentMonth.isAfter(fromDate)) {
        // Statement day hasn't occurred yet this month
        currentMonth
    } else {
        // Statement day already passed, use next month
        val nextMonth = fromDate.plusMonths(1)
        val dayNextMonth = minOf(statementDay, nextMonth.lengthOfMonth())
        nextMonth.withDayOfMonth(dayNextMonth)
    }
}
```

**Example**:
```
Today: October 10, 2025
Statement Day: 18
Next Statement Date: October 18, 2025

Today: October 20, 2025
Statement Day: 18
Next Statement Date: November 18, 2025
```

### Step 2: Calculate Target Payment Hit Date

Payment must hit account **1 day before** statement closes:

```kotlin
val targetPaymentHitDate = nextStatementDate.minusDays(1)
```

**Why 1 day before?**
- Ensures payment posts before statement closes
- Buffer for any delays
- Banks typically batch process at end of day

**Example**:
```
Statement closes: October 18, 2025
Payment must hit by: October 17, 2025
```

### Step 3: Subtract Bank Processing Days

Different banks take different amounts of time to process payments:

```kotlin
val processingDays = creditCard.institution.processingDays

val recommendedPaymentDate = BusinessDayCalculator.getBusinessDaysBefore(
    targetPaymentHitDate,
    processingDays
)
```

**Bank Processing Times**:

| Bank | Processing Days | Example |
|------|----------------|---------|
| American Express | 0 (instant) | Pay on 17th → hits 17th |
| Capital One | 1 business day | Pay on 16th → hits 17th |
| Discover | 1 business day | Pay on 16th → hits 17th |
| Chase | 2 business days | Pay on 13th → hits 17th |
| Citi | 2 business days | Pay on 13th → hits 17th |
| Wells Fargo | 2 business days | Pay on 13th → hits 17th |
| US Bank | 2 business days | Pay on 13th → hits 17th |
| Bank of America | 3 business days | Pay on 12th → hits 17th |
| Generic/Unknown | 3 business days | Pay on 12th → hits 17th |

**Example (Chase, 2 business days)**:
```
Target hit date: October 17, 2025 (Friday)
Processing days: 2 business days
Recommended payment date: October 15, 2025 (Wednesday)

Timeline:
- Wednesday, Oct 15: You initiate payment
- Thursday, Oct 16: Processing day 1
- Friday, Oct 17: Payment hits account ✅
- Saturday, Oct 18: Statement closes with $0 balance 🎉
```

### Step 4: Account for Weekends and Holidays

The `BusinessDayCalculator` skips weekends and federal holidays:

```kotlin
fun getBusinessDaysBefore(targetDate: LocalDate, businessDays: Int): LocalDate {
    var date = targetDate
    var daysRemaining = businessDays
    
    while (daysRemaining > 0) {
        date = date.minusDays(1)
        if (!isWeekend(date) && !isFederalHoliday(date)) {
            daysRemaining--
        }
    }
    
    return date
}
```

**Example with Weekend**:
```
Target hit date: Monday, October 20, 2025
Processing days: 2 business days

Calculation:
- Subtract 1 day → Sunday, Oct 19 (weekend, skip)
- Subtract 1 day → Saturday, Oct 18 (weekend, skip)
- Subtract 1 day → Friday, Oct 17 ✅ (business day, count 1)
- Subtract 1 day → Thursday, Oct 16 ✅ (business day, count 2)

Recommended payment date: Thursday, October 16, 2025
```

**Example with Holiday**:
```
Target hit date: Monday, July 7, 2025
Processing days: 2 business days
Federal Holiday: Independence Day (July 4)

Calculation:
- Subtract 1 day → Sunday, July 6 (weekend, skip)
- Subtract 1 day → Saturday, July 5 (weekend, skip)
- Subtract 1 day → Friday, July 4 (federal holiday, skip)
- Subtract 1 day → Thursday, July 3 ✅ (business day, count 1)
- Subtract 1 day → Wednesday, July 2 ✅ (business day, count 2)

Recommended payment date: Wednesday, July 2, 2025
```

### Step 5: Ensure Date is Not in the Past

```kotlin
val finalRecommendedDate = if (recommendedPaymentDate.isBefore(today)) {
    today  // Pay today if recommended date already passed
} else {
    recommendedPaymentDate
}
```

### Step 6: Calculate Recommended Amount

**Strategy**: Pay current balance to zero out statement

```kotlin
val recommendedAmount = creditCard.currentBalance
```

**Why pay full balance?**
- Minimizes statement balance
- Maximizes credit score benefit
- You were going to pay it eventually anyway

**Alternative strategies** (not yet implemented):
- Pay to achieve target utilization (e.g., 10%)
- Pay minimum to free up specific credit limit amount
- Pay custom amount based on user preference

### Step 7: Build Recommendation

```kotlin
return PaymentRecommendation(
    creditCard = creditCard,
    recommendedPaymentDate = finalRecommendedDate,
    recommendedAmount = recommendedAmount,
    statementDueDate = nextStatementDate,
    daysUntilPayment = ChronoUnit.DAYS.between(today, finalRecommendedDate).toInt(),
    reasoning = buildReasoningString(...)
)
```

## Complete Example

**Scenario**:
- Card: Chase Freedom
- Today: October 10, 2025 (Friday)
- Statement closes: 18th of each month
- Current balance: $1,500
- Processing time: 2 business days

**Calculation**:

1. **Next statement date**: October 18, 2025 (Saturday)
2. **Target hit date**: October 17, 2025 (Friday)
3. **Subtract 2 business days**:
   - Oct 17 (Friday) → Oct 16 (Thursday) [day 1]
   - Oct 16 (Thursday) → Oct 15 (Wednesday) [day 2]
4. **Recommended payment date**: October 15, 2025 (Wednesday)
5. **Days until payment**: 5 days
6. **Recommended amount**: $1,500

**Recommendation**:
```
Pay $1,500.00 on Wednesday, October 15, 2025

Reasoning:
"Statement closes on 2025-10-18. Payment must hit account by 2025-10-17. 
Chase takes 2 business day(s) to process. You have 5 days to make the payment."
```

## Federal Holidays

The system accounts for all 11 US federal holidays:

```kotlin
fun isFederalHoliday(date: LocalDate): Boolean {
    return when {
        // New Year's Day (Jan 1)
        date.month == Month.JANUARY && date.dayOfMonth == 1 -> true
        
        // Martin Luther King Jr. Day (3rd Monday in January)
        date.month == Month.JANUARY && 
        date.dayOfWeek == DayOfWeek.MONDAY &&
        date.dayOfMonth in 15..21 -> true
        
        // Presidents' Day (3rd Monday in February)
        date.month == Month.FEBRUARY && 
        date.dayOfWeek == DayOfWeek.MONDAY &&
        date.dayOfMonth in 15..21 -> true
        
        // Memorial Day (Last Monday in May)
        date.month == Month.MAY && 
        date.dayOfWeek == DayOfWeek.MONDAY &&
        date.dayOfMonth >= (date.lengthOfMonth() - 6) -> true
        
        // Juneteenth (June 19)
        date.month == Month.JUNE && date.dayOfMonth == 19 -> true
        
        // Independence Day (July 4)
        date.month == Month.JULY && date.dayOfMonth == 4 -> true
        
        // Labor Day (1st Monday in September)
        date.month == Month.SEPTEMBER && 
        date.dayOfWeek == DayOfWeek.MONDAY &&
        date.dayOfMonth <= 7 -> true
        
        // Columbus Day (2nd Monday in October)
        date.month == Month.OCTOBER && 
        date.dayOfWeek == DayOfWeek.MONDAY &&
        date.dayOfMonth in 8..14 -> true
        
        // Veterans Day (November 11)
        date.month == Month.NOVEMBER && date.dayOfMonth == 11 -> true
        
        // Thanksgiving (4th Thursday in November)
        date.month == Month.NOVEMBER && 
        date.dayOfWeek == DayOfWeek.THURSDAY &&
        date.dayOfMonth in 22..28 -> true
        
        // Christmas (December 25)
        date.month == Month.DECEMBER && date.dayOfMonth == 25 -> true
        
        else -> false
    }
}
```

**Observed Holidays**:
When holidays fall on weekends, they're observed on adjacent weekdays:
- Falls on Saturday → observed Friday
- Falls on Sunday → observed Monday

The algorithm already handles this by skipping weekends, so observed dates are automatically accounted for.

## Edge Cases

### Case 1: Statement Day Doesn't Exist in Month

**Example**: Statement day is 31, but month only has 30 days

**Solution**: Use last day of month

```kotlin
val dayThisMonth = minOf(statementDay, fromDate.lengthOfMonth())
```

```
Statement day: 31
February 2025 (28 days):
  Adjusted statement day: 28
  Statement date: February 28, 2025 ✅
```

### Case 2: Recommended Date in Past

**Example**: Calculated payment date is yesterday

**Solution**: Recommend paying today

```kotlin
if (recommendedPaymentDate.isBefore(today)) {
    return today
}
```

### Case 3: Statement Closes Tomorrow

**Example**: Need to pay before tomorrow's statement

**Solution**: Recommend paying today (urgent)

```kotlin
val daysUntilPayment = ChronoUnit.DAYS.between(today, finalRecommendedDate).toInt()

if (daysUntilPayment == 0) {
    reasoning.append("Payment should be made TODAY!")
}
```

### Case 4: No Statement Date Known

**Example**: New card, statement date not detected

**Solution**: Skip calculation, prompt user for input

```kotlin
fun calculateRecommendations(creditCards: List<CreditCard>): List<PaymentRecommendation> {
    return creditCards
        .filter { it.statementGenerationDay != null }  // Skip cards without date
        .mapNotNull { card ->
            try {
                calculateRecommendation(card)
            } catch (e: Exception) {
                null  // Skip cards that can't be calculated
            }
        }
}
```

## Testing

### Unit Tests

```kotlin
class PaymentCalculatorTest {
    @Test
    fun `calculates payment date correctly with 2 business days`() {
        val card = CreditCard(
            statementGenerationDay = 18,
            institution = BankInstitution.CHASE,  // 2 business days
            currentBalance = 1500.0
        )
        
        val today = LocalDate.of(2025, 10, 10)
        val recommendation = PaymentCalculator.calculateRecommendation(card)
        
        assertEquals(LocalDate.of(2025, 10, 15), recommendation.recommendedPaymentDate)
        assertEquals(1500.0, recommendation.recommendedAmount)
    }
    
    @Test
    fun `skips weekends correctly`() {
        // Target: Monday Oct 20
        // Processing: 2 days
        // Expected: Thursday Oct 16 (skip Sat/Sun)
        
        val result = BusinessDayCalculator.getBusinessDaysBefore(
            LocalDate.of(2025, 10, 20),
            2
        )
        
        assertEquals(LocalDate.of(2025, 10, 16), result)
    }
    
    @Test
    fun `skips federal holidays correctly`() {
        // Target: Monday July 7
        // Holiday: July 4 (Friday)
        // Processing: 2 days
        // Expected: Wednesday July 2 (skip Fri holiday, Sat, Sun)
        
        val result = BusinessDayCalculator.getBusinessDaysBefore(
            LocalDate.of(2025, 7, 7),
            2
        )
        
        assertEquals(LocalDate.of(2025, 7, 2), result)
    }
}
```

## Code Location

**Implementation**:
- `app/src/main/java/com/ccoptimizer/app/business/PaymentCalculator.kt`
- `app/src/main/java/com/ccoptimizer/app/utils/BusinessDayCalculator.kt`

**Models**:
- `app/src/main/java/com/ccoptimizer/app/data/models/CreditCard.kt`
- `app/src/main/java/com/ccoptimizer/app/data/models/PaymentRecommendation.kt`
- `app/src/main/java/com/ccoptimizer/app/data/models/BankInstitution.kt`

**Tests**:
- `app/src/test/java/com/ccoptimizer/app/PaymentCalculatorTest.kt`
- `app/src/test/java/com/ccoptimizer/app/BusinessDayCalculatorTest.kt`

## Performance

**Complexity**: O(n) where n is number of cards
**Typical execution time**: < 1ms per card
**Memory**: Minimal (no caching needed, pure calculation)

## Future Enhancements

1. **Multiple Payment Strategies**
   - Minimum payment
   - Target utilization percentage
   - Custom amount

2. **Payment Scheduling**
   - Integrate with calendar
   - Set up recurring payments
   - Automatic payment reminders

3. **Optimization Across Cards**
   - Prioritize which cards to pay first
   - Optimize total utilization across all cards
   - Balance transfer recommendations

4. **What-If Analysis**
   - "What if I pay $X on date Y?"
   - Show impact on credit utilization
   - Compare different payment strategies

---

**Related Documentation**:
- [System Overview](../architecture/system-overview.md)
- [AI Statement Detection](llm-integration.md)
- [Bank Processing Times](../architecture/system-overview.md#bank-institution)

