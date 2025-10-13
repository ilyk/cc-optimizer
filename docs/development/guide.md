# Development Guide

Complete developer workflow and best practices.

## Setup

1. **Install Prerequisites**
   - Android Studio Hedgehog+
   - JDK 17
   - Node.js 18+
   - Git

2. **Clone & Setup**
   ```bash
   git clone <repo-url>
   cd cc
   
   # Backend
   cd backend
   npm install
   cp .env.example .env
   # Edit .env with your Plaid credentials
   npm start
   
   # Android
   # Open in Android Studio
   # Sync Gradle
   # Run on device/emulator
   ```

## Development Workflow

### 1. Create Branch

```bash
git checkout -b feature/my-feature
```

### 2. Make Changes

**Android**:
- Follow MVVM architecture
- Use Kotlin coroutines
- Write unit tests
- Update documentation

**Backend**:
- Follow layered architecture
- Use async/await
- Write tests
- Update API docs

### 3. Test

```bash
# Android
./gradlew test
./gradlew connectedAndroidTest

# Backend
cd backend
npm test
npm run coverage  # Must be 95%+
```

### 4. Lint

```bash
# Android
./gradlew ktlintFormat
./gradlew ktlintCheck

# Backend
npm run lint:fix
npm run format
```

### 5. Commit

```bash
git add .
git commit -m "feat: add my feature"
```

**Commit Message Format**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Tests
- `chore`: Maintenance

### 6. Push & PR

```bash
git push origin feature/my-feature
```

Create pull request on GitHub.

## Project Structure

**Android**:
```
app/src/main/java/com/ccoptimizer/app/
├── business/           # Business logic
├── data/              # Data layer
├── ui/                # UI layer
├── utils/             # Utilities
└── MainActivity.kt    # Entry point
```

**Backend**:
```
backend/src/
├── config/            # Configuration
├── middleware/        # Express middleware
├── services/          # Business logic
├── repositories/      # Data access
└── utils/            # Utilities
```

## Best Practices

### Android

**1. Use Kotlin Idioms**:
```kotlin
// ✅ Good
val name = user?.name ?: "Unknown"

// ❌ Avoid
val name = if (user != null) user.name else "Unknown"
```

**2. Use Coroutines**:
```kotlin
// ✅ Good
viewModelScope.launch {
    val data = repository.getData()
}

// ❌ Avoid
Thread {
    val data = repository.getData()
}.start()
```

**3. State Management**:
```kotlin
// ✅ Good: StateFlow
private val _state = MutableStateFlow<UiState>(UiState.Loading)
val state: StateFlow<UiState> = _state.asStateFlow()

// ❌ Avoid: LiveData in new code
private val _state = MutableLiveData<UiState>()
```

### Backend

**1. Use Async/Await**:
```javascript
// ✅ Good
async function getData() {
    const data = await db.query('SELECT * FROM users');
    return data;
}

// ❌ Avoid
function getData() {
    return db.query('SELECT * FROM users').then(data => data);
}
```

**2. Error Handling**:
```javascript
// ✅ Good
try {
    const data = await api.getData();
    return data;
} catch (error) {
    logger.error('Error:', error);
    throw error;
}
```

**3. Input Validation**:
```javascript
// ✅ Always validate
const validation = [
    body('email').isEmail(),
    body('password').isLength({ min: 8 })
];
```

## Debugging

### Android

**Logcat**:
```bash
adb logcat | grep "CC_OPTIMIZER"
```

**Breakpoints**:
- Set in Android Studio
- Run in Debug mode (Shift+F9)

### Backend

**Logging**:
```javascript
logger.debug('Debug message');
logger.info('Info message');
logger.error('Error message');
```

**Node Inspector**:
```bash
node --inspect server.js
```

## Testing

### Android

**Unit Tests**:
```kotlin
@Test
fun `test payment calculation`() {
    val card = CreditCard(statementDay = 18, ...)
    val recommendation = PaymentCalculator.calculateRecommendation(card)
    assertEquals(LocalDate.of(2025, 10, 15), recommendation.recommendedPaymentDate)
}
```

**UI Tests**:
```kotlin
@Test
fun testMainScreen() {
    composeTestRule.setContent {
        MainScreen(viewModel)
    }
    
    composeTestRule.onNodeWithText("Connect with Plaid").assertExists()
}
```

### Backend

**Unit Tests**:
```javascript
describe('AuthService', () => {
    it('should register user', async () => {
        const result = await authService.register('test@example.com', 'password');
        expect(result.user.email).toBe('test@example.com');
    });
});
```

## Documentation

Update docs when adding features:
- API changes → Update `docs/api/`
- New features → Update `docs/features/`
- Architecture changes → Update `docs/architecture/`

## Common Tasks

**Run backend**:
```bash
cd backend
npm start
```

**Build Android debug**:
```bash
./gradlew assembleDebug
```

**Build Android release**:
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
./gradlew assembleRelease
```

**Run tests**:
```bash
./gradlew test              # Android unit tests
npm test                    # Backend tests
```

**Fix linting**:
```bash
./gradlew ktlintFormat      # Android
npm run lint:fix            # Backend
```

---

**See Also**:
- [Installation Guide](../getting-started/installation.md)
- [Testing Guide](testing.md)
- [Code Style](code-style.md)
- [Contributing](contributing.md)

