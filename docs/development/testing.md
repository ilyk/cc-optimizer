# Testing Strategy

Comprehensive testing approach for quality assurance.

## Test Pyramid

```
       ┌─────────────┐
       │   E2E Tests │  (Few, slow, high confidence)
       └─────────────┘
      ┌───────────────┐
      │ Integration   │  (Some, medium speed)
      │     Tests     │
      └───────────────┘
   ┌──────────────────────┐
   │    Unit Tests        │  (Many, fast, focused)
   └──────────────────────┘
```

## Android Testing

### Unit Tests

**Location**: `app/src/test/java/`

**Example**:
```kotlin
class PaymentCalculatorTest {
    @Test
    fun `calculates correct payment date`() {
        val card = CreditCard(
            statementGenerationDay = 18,
            institution = BankInstitution.CHASE,
            currentBalance = 1500.0
        )
        
        val recommendation = PaymentCalculator.calculateRecommendation(card)
        
        assertEquals(LocalDate.of(2025, 10, 15), recommendation.recommendedPaymentDate)
        assertEquals(1500.0, recommendation.recommendedAmount)
    }
}
```

**Run**:
```bash
./gradlew test
./gradlew test --tests PaymentCalculatorTest
```

### Instrumented Tests

**Location**: `app/src/androidTest/java/`

**Example**:
```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testMainScreen() {
        composeTestRule.setContent {
            CreditCardOptimizerTheme {
                MainScreen(viewModel)
            }
        }
        
        composeTestRule.onNodeWithText("Connect with Plaid").assertExists()
    }
}
```

**Run**:
```bash
./gradlew connectedAndroidTest
```

## Backend Testing

### Unit Tests

**Location**: `backend/src/__tests__/`

**Example**:
```javascript
describe('AuthService', () => {
    it('should register user', async () => {
        const result = await authService.register(
            'test@example.com',
            'TestPass123'
        );
        
        expect(result.user.email).toBe('test@example.com');
        expect(result.token).toBeDefined();
    });
    
    it('should reject weak password', async () => {
        await expect(
            authService.register('test@example.com', 'weak')
        ).rejects.toThrow();
    });
});
```

**Run**:
```bash
npm test
npm test -- --coverage
```

### Coverage Requirements

**Minimum**: 95%

```bash
npm run coverage

# Should show:
# Statements: 95%+
# Branches: 95%+
# Functions: 95%+
# Lines: 95%+
```

**Enforce in CI**:
```bash
npm run coverage -- --coverageThreshold='{"global":{"lines":95}}'
```

## Test Data

### Plaid Sandbox

**Test credentials**:
- Username: `user_good`
- Password: `pass_good`
- MFA code: `1234`

**Test accounts**:
- Chase: Credit card with $10,000 limit
- Bank of America: Credit card with $15,000 limit

### Mock Data

```kotlin
object TestData {
    val mockCreditCard = CreditCard(
        id = "test-123",
        name = "Test Card",
        statementGenerationDay = 18,
        currentBalance = 1500.0
    )
    
    val mockRecommendation = PaymentRecommendation(
        creditCard = mockCreditCard,
        recommendedPaymentDate = LocalDate.now().plusDays(5)
    )
}
```

## Mocking

### Android

```kotlin
@Test
fun testRepository() {
    val mockApi = mock<PlaidApi>()
    val repository = PlaidRepository(mockApi)
    
    whenever(mockApi.getAccounts()).thenReturn(listOf(mockAccount))
    
    val result = runBlocking { repository.getAccounts() }
    assertEquals(1, result.getOrNull()?.size)
}
```

### Backend

```javascript
jest.mock('../repositories/userRepository');

test('should call repository', async () => {
    userRepository.findByEmail.mockResolvedValue(mockUser);
    
    const user = await authService.login('test@example.com', 'password');
    expect(userRepository.findByEmail).toHaveBeenCalled();
});
```

## Best Practices

1. **Test Naming**: Use descriptive names
   ```kotlin
   fun `should reject payment for card without statement date`()
   ```

2. **Arrange-Act-Assert**:
   ```kotlin
   // Arrange
   val card = createTestCard()
   
   // Act
   val result = calculate(card)
   
   // Assert
   assertEquals(expected, result)
   ```

3. **Test One Thing**: Each test should test one behavior

4. **Mock External Dependencies**: Don't rely on actual APIs

5. **Clean Up**: Reset state after each test

## CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Android Tests
  run: ./gradlew test

- name: Run Backend Tests
  run: cd backend && npm test

- name: Check Coverage
  run: cd backend && npm run coverage
```

---

**See Also**:
- [Development Guide](guide.md)
- [Code Style](code-style.md)

