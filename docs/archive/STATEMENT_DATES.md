# Automatic Statement Date Fetching

## Overview

The app automatically retrieves your **actual statement closing dates** directly from your bank via Plaid's Liabilities API. No manual configuration or guesswork needed!

## Why This Matters

Credit card statement dates are critical for optimizing payments:
- **Too Early**: You pay money that could earn interest in your account
- **Too Late**: Your statement shows a high balance, increasing your credit utilization ratio
- **Just Right**: Payment hits account 1 day before statement closes → nearly $0 reported balance

## How It Works

### 1. Plaid Link Token Request
When you connect your bank account, the backend requests a Plaid Link token with the `liabilities` product:

```javascript
// backend/src/services/plaidService.js
products: ['auth', 'transactions', 'liabilities']
```

### 2. Bank Connection
User connects their credit card through Plaid Link. Plaid retrieves:
- Account balances
- Transaction history
- **Credit card liability data** (the key part!)

### 3. Liabilities API Call
Backend fetches detailed liability information:

```javascript
const liabilitiesResponse = await this.client.liabilitiesGet({
  access_token: tokenData.accessToken,
});
```

### 4. Statement Date Extraction
The liability data includes:
```javascript
{
  last_statement_issue_date: "2025-09-18",  // ← We extract the day (18)
  next_payment_due_date: "2025-10-15",
  last_statement_balance: 1247.50,
  minimum_payment_amount: 35.00
}
```

### 5. Android App Processing
The Android app extracts the day of month from the statement date:

```kotlin
// PlaidRepository.kt
val statementDay = account.liability?.lastStatementDate?.let { dateString ->
    try {
        val date = java.time.LocalDate.parse(dateString)
        date.dayOfMonth  // Returns: 18
    } catch (e: Exception) {
        null
    }
}

// Use extracted day or default to 15
statementGenerationDay = statementDay ?: 15
```

### 6. Payment Calculation
The `PaymentCalculator` uses this **actual** statement day to calculate:
1. Next statement date (e.g., October 18th)
2. Target payment arrival date (October 17th)
3. When to initiate payment (accounting for bank processing time)
4. Business days adjustment (skipping weekends/holidays)

## Data Flow Diagram

```
User Connects Bank
       ↓
Plaid Link (with 'liabilities' product)
       ↓
Backend: plaidService.getAccounts()
       ↓
Plaid API: /liabilities/get
       ↓
Returns: {
  last_statement_issue_date: "2025-09-18"
}
       ↓
Backend: Include liability data in response
       ↓
Android: PlaidRepository.mapToCreditCard()
       ↓
Extract day: 18
       ↓
CreditCard(statementGenerationDay: 18)
       ↓
PaymentCalculator uses actual statement day
       ↓
Recommendation: "Pay by Oct 16 for Oct 18 statement"
```

## Supported Banks

Most major credit card issuers provide liability data through Plaid:
- ✅ American Express
- ✅ Bank of America
- ✅ Chase
- ✅ Citi
- ✅ Capital One
- ✅ Discover
- ✅ Wells Fargo
- ⚠️ Some smaller banks may not provide this data

## Fallback Behavior

If a bank doesn't provide liability data:
1. The app logs a warning
2. Defaults to the 15th of the month
3. User can still use the app (but with less accuracy)

**Future enhancement:** Add manual override UI for users whose banks don't provide data.

## Code Files Modified

### Backend
- **`src/services/plaidService.js`**
  - Line 36: Added `'liabilities'` to products array
  - Lines 108-145: Fetch and include liability data in account response

### Android
- **`app/src/main/java/com/ccoptimizer/app/data/models/PlaidModels.kt`**
  - Lines 10: Added `liability: PlaidLiability?` to `PlaidAccount`
  - Lines 19-24: New `PlaidLiability` data class

- **`app/src/main/java/com/ccoptimizer/app/data/repository/PlaidRepository.kt`**
  - Lines 72-100: Updated `mapToCreditCard()` to extract statement day from liability data

## Testing

### Sandbox Testing
Plaid Sandbox provides mock liability data:
```json
{
  "last_statement_issue_date": "2025-09-15",
  "last_statement_balance": 1500.00,
  "minimum_payment_amount": 50.00,
  "next_payment_due_date": "2025-10-12"
}
```

### Production Testing
With real bank connections:
1. Connect credit card via Plaid Link
2. Check app logs for liability data
3. Verify statement day matches your actual credit card statement
4. Confirm payment recommendations align with your card's billing cycle

## Benefits

### ✅ Accuracy
- Uses actual statement dates, not estimates or user input
- Eliminates human error in date entry

### ✅ Maintenance-Free
- No manual updates needed
- Automatically adapts if bank changes your statement date

### ✅ Better UX
- No forms to fill out
- Works immediately upon account connection
- One-time setup per card

### ✅ Credit Score Optimization
- Precise timing ensures minimal reported balance
- Lowers credit utilization ratio
- Improves credit score over time

## Limitations

1. **Bank Support**: Not all banks provide liability data through Plaid
2. **Data Freshness**: Uses last statement date, not next (we calculate next)
3. **First Statement**: New accounts might not have liability data yet

## Future Enhancements

- [ ] Manual override UI for banks without liability data
- [ ] Detect and alert if statement date changes
- [ ] Use `next_payment_due_date` for additional validation
- [ ] Cache statement dates to reduce API calls
- [ ] Allow users to adjust if their bank's date is inconsistent

## API Reference

### Plaid Liabilities Object (Credit Cards)
```typescript
{
  account_id: string;
  last_statement_issue_date: string | null;  // ISO 8601 date
  last_statement_balance: number | null;
  minimum_payment_amount: number | null;
  next_payment_due_date: string | null;     // ISO 8601 date
  is_overdue: boolean | null;
}
```

See: https://plaid.com/docs/api/products/liabilities/

## Security Considerations

- Liability data is sensitive financial information
- Access tokens are encrypted in database
- Data is only fetched when user explicitly connects account
- Statement dates are stored locally on device (not sent to external servers)

## Troubleshooting

### Issue: Statement day is always 15
**Cause**: Bank doesn't provide liability data
**Solution**: Check backend logs for "Liabilities not available" warning

### Issue: Wrong statement date
**Cause**: Bank provided incorrect data or last statement was irregular
**Solution**: Implement manual override feature (future enhancement)

### Issue: No liability data in response
**Cause**: Plaid Link token wasn't created with 'liabilities' product
**Solution**: Verify `products` array includes `'liabilities'` in `createLinkToken()`

## Conclusion

By leveraging Plaid's Liabilities API, the Credit Card Optimizer provides **accurate, maintenance-free statement date tracking**. This is a core feature that differentiates the app from manual tracking tools and ensures optimal payment timing for credit score improvement.

---

**Last Updated**: After implementing automatic statement date fetching  
**Impact**: Critical feature - eliminates manual configuration and improves accuracy

