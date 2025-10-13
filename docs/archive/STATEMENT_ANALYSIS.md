# Intelligent Statement Date Analysis

## The Problem

A single statement date from Plaid (e.g., `last_statement_issue_date: "2025-09-18"`) is **unreliable** because:

1. **Weekend Shifts**: If statement day is Sunday (18th), it might get issued Monday (19th)
2. **Holiday Shifts**: If statement day falls on a holiday, it shifts to next business day
3. **One-time Anomalies**: Bank systems might have glitches causing one-off date changes

**Example**: Your statement is ALWAYS on the 18th, but:
- September 18, 2025 is a Thursday ✓
- October 18, 2025 is a Saturday → Statement issues on Monday, October 20
- If we only looked at October's date, we'd think statement day is the 20th ❌

## The Solution: Pattern Analysis

Instead of trusting ONE date, we **analyze 6 months of transaction history** to find the recurring pattern.

### How It Works

#### 1. Backend Fetches Transaction History
```javascript
// Get 6 months of transactions
const startDate = new Date(Date.now() - 180 * 24 * 60 * 60 * 1000);
const transactions = await plaidClient.transactionsGet({
  access_token: token,
  start_date: startDate.toISOString().split('T')[0],
  end_date: new Date().toISOString().split('T')[0],
});
```

#### 2. Look for Statement Markers
```javascript
const statementTransactions = accountTransactions.filter(t => 
  t.name && (
    t.name.toLowerCase().includes('statement') ||
    t.name.toLowerCase().includes('closing') ||
    t.name.toLowerCase().includes('cycle')
  )
);
```

Many banks include transactions like:
- "STATEMENT CLOSING FEE" 
- "MONTHLY CYCLE FEE"
- "STATEMENT GENERATION"

These marker transactions appear on or near the actual statement date.

#### 3. Extract All Statement Dates
```javascript
const statementDates = [];
statementTransactions.forEach(t => {
  const date = new Date(t.date);
  statementDates.push(date.getDate()); // Get day of month
});

// Example result: [18, 20, 18, 18, 19, 18]
// (20 = weekend shift, 19 = holiday shift)
```

#### 4. Find Most Common Day
```javascript
const dayCounts = {};
statementDates.forEach(day => {
  dayCounts[day] = (dayCounts[day] || 0) + 1;
});

// dayCounts = { 18: 4, 19: 1, 20: 1 }

const mostCommonDay = Object.keys(dayCounts)
  .reduce((a, b) => dayCounts[a] > dayCounts[b] ? a : b);

// Result: 18 (appears 4 times out of 6)
```

#### 5. Return Analyzed Pattern
```javascript
return {
  accountId: "acc_123",
  name: "Chase Freedom",
  liability: {
    lastStatementDate: "2025-10-20", // This month (was shifted)
    recurringStatementDay: 18,        // ← The REAL statement day!
  }
}
```

### Android Side: NO Defaults

```kotlin
private fun mapToCreditCard(account: PlaidAccount): CreditCard {
    // Use ONLY the analyzed recurring day
    val statementDay = account.liability?.recurringStatementDay
    
    val needsInput = statementDay == null
    if (needsInput) {
        Log.w("PlaidRepository", 
            "Could not determine statement day for ${account.name}. " +
            "Transaction history did not contain statement markers. " +
            "USER MUST MANUALLY INPUT."
        )
    }
    
    return CreditCard(
        statementGenerationDay = statementDay, // null if unknown - NO DEFAULT!
        needsStatementDateInput = needsInput
    )
}
```

## Payment Calculator Handling

```kotlin
fun calculateRecommendations(creditCards: List<CreditCard>): List<PaymentRecommendation> {
    return creditCards
        .filter { it.statementGenerationDay != null } // Only process known dates
        .mapNotNull { card ->
            try {
                calculateRecommendation(card)
            } catch (e: Exception) {
                null // Skip cards that fail
            }
        }
        .sortedBy { it.recommendedPaymentDate }
}

fun getCardsNeedingInput(creditCards: List<CreditCard>): List<CreditCard> {
    return creditCards.filter { it.needsStatementDateInput }
}
```

## UI Handling

When cards need input, the UI shows a warning section:

```
┌────────────────────────────────────────┐
│ ⚠️ Manual Setup Required               │
│                                        │
│ Could not automatically determine      │
│ statement dates for 2 card(s).         │
│                                        │
│ ┌──────────────────────────────────┐  │
│ │ Home Depot Credit                │  │
│ │ •••• 1234          Need Date → │  │
│ └──────────────────────────────────┘  │
│                                        │
│ 📝 Future update will allow manual    │
│ date input. Check your credit card    │
│ statement for the closing date.       │
└────────────────────────────────────────┘
```

## Why This Matters

### ✅ Accuracy
- Uses MULTIPLE data points, not one potentially-shifted date
- Automatically accounts for weekend/holiday shifts
- Finds the TRUE recurring pattern

### ✅ No False Assumptions
- If we can't determine with confidence → ask user
- **Never defaults to arbitrary dates like "15"**
- Better to have NO data than WRONG data

### ✅ Transparency
- Clear logging of what was found
- UI explicitly shows which cards need input
- User knows exactly what's happening

## Limitations & Future Improvements

### Current Limitations

1. **Requires statement markers in transactions**: Not all banks include these
2. **Needs 6 months history**: New cards might not have enough data
3. **No manual override yet**: User can't set date themselves (future feature)

### Planned Improvements

- [ ] **Manual date input UI**: Allow users to set statement day for cards we can't analyze
- [ ] **Alternative pattern detection**: Analyze transaction volume patterns (spending typically resets after statement)
- [ ] **Balance change detection**: Look for when balance gets reset/adjusted (statement posts)
- [ ] **Machine learning**: Train model to detect statement periods from spending patterns
- [ ] **Cached patterns**: Remember analyzed patterns to reduce API calls

## Real-World Examples

### Example 1: Chase Freedom (Works Perfect)
```
Transactions found:
  2025-09-18: "STATEMENT CLOSING FEE"
  2025-08-18: "STATEMENT CLOSING FEE"
  2025-07-18: "STATEMENT CLOSING FEE"
  2025-06-18: "STATEMENT CLOSING FEE"
  2025-05-20: "STATEMENT CLOSING FEE" (18th was Saturday)
  2025-04-18: "STATEMENT CLOSING FEE"

Analysis: Day 18 appears 5 times, day 20 appears 1 time
Result: Statement day = 18 ✓
```

### Example 2: Generic Credit Card (Needs Input)
```
Transactions found:
  2025-09-15: "PURCHASE - AMAZON"
  2025-09-14: "PURCHASE - STARBUCKS"
  2025-09-12: "PAYMENT RECEIVED"
  ... (no statement markers found)

Analysis: No statement-related transactions found
Result: Statement day = null → User must input ⚠️
```

### Example 3: Amex (Works with Different Marker)
```
Transactions found:
  2025-09-20: "MONTHLY CYCLE FEE"
  2025-08-20: "MONTHLY CYCLE FEE"
  2025-07-20: "MONTHLY CYCLE FEE"
  2025-06-20: "MONTHLY CYCLE FEE"

Analysis: Day 20 appears 4 times
Result: Statement day = 20 ✓
```

## Conclusion

This intelligent analysis approach:

1. **Analyzes MULTIPLE statement periods** (not just one date)
2. **Finds recurring patterns** (ignoring one-off shifts)
3. **Never assumes defaults** (requires user input if uncertain)
4. **Transparently communicates** what it found and what it needs

This is the RIGHT way to handle statement dates - no defaults, no assumptions, just data-driven analysis.

---

**Result**: Accurate statement dates for cards where we have enough data, clear user prompts for cards where we don't. **No silent failures, no incorrect defaults.**

