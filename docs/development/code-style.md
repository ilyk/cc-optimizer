# Code Style Guide

Coding standards and linting configuration.

## Android (Kotlin)

### Linter: ktlint

**Configuration**: `.editorconfig`

```properties
[*.{kt,kts}]
indent_size = 4
max_line_length = 120
insert_final_newline = true
```

**Run**:
```bash
./gradlew ktlintFormat  # Auto-fix
./gradlew ktlintCheck   # Check only
```

### Style Rules

**Naming**:
```kotlin
// Classes: PascalCase
class PaymentCalculator

// Functions: camelCase
fun calculatePayment()

// Constants: UPPER_SNAKE_CASE
const val MAX_RETRIES = 3

// Variables: camelCase
val userName = "John"
```

**Formatting**:
```kotlin
// ✅ Good
fun calculate(
    card: CreditCard,
    date: LocalDate
): PaymentRecommendation {
    return PaymentRecommendation(...)
}

// ❌ Avoid
fun calculate(card: CreditCard, date: LocalDate): PaymentRecommendation {
    return PaymentRecommendation(...)
}
```

## Backend (JavaScript)

### Linter: ESLint + Prettier

**Configuration**: `.eslintrc.js`

```javascript
module.exports = {
    extends: ['eslint:recommended'],
    rules: {
        'indent': ['error', 2],
        'quotes': ['error', 'single'],
        'semi': ['error', 'always']
    }
};
```

**Run**:
```bash
npm run lint:fix    # Auto-fix
npm run lint:check  # Check only
npm run format      # Prettier format
```

### Style Rules

**Naming**:
```javascript
// Functions: camelCase
function calculatePayment() {}

// Classes: PascalCase
class AuthService {}

// Constants: UPPER_SNAKE_CASE
const MAX_RETRIES = 3;

// Variables: camelCase
const userName = 'John';
```

**Formatting**:
```javascript
// ✅ Good: 2 spaces
function calculate(card, date) {
  return {
    date: date,
    amount: card.balance
  };
}

// ❌ Avoid: 4 spaces or tabs
function calculate(card, date) {
    return {
        date: date,
        amount: card.balance
    };
}
```

## General Best Practices

### Comments

```kotlin
// ✅ Good: Explain WHY
// Payment must hit account 1 day before statement closes
// to ensure it's reflected in the statement balance
val targetDate = statementDate.minusDays(1)

// ❌ Avoid: Explain WHAT (code is self-explanatory)
// Subtract 1 day from statement date
val targetDate = statementDate.minusDays(1)
```

### Error Handling

```kotlin
// ✅ Good: Specific exceptions
throw IllegalArgumentException("Statement day must be 1-31")

// ❌ Avoid: Generic exceptions
throw Exception("Invalid input")
```

### Magic Numbers

```kotlin
// ✅ Good: Named constants
const val DAYS_BEFORE_STATEMENT = 1
val targetDate = statementDate.minusDays(DAYS_BEFORE_STATEMENT)

// ❌ Avoid: Magic numbers
val targetDate = statementDate.minusDays(1)
```

## Pre-commit Hooks

**Setup** (optional):
```bash
# Install husky
npm install --save-dev husky

# Add pre-commit hook
npx husky add .husky/pre-commit "npm run lint && ./gradlew ktlintCheck"
```

## CI/CD Enforcement

```yaml
# Fail build if linting fails
- name: Lint Android
  run: ./gradlew ktlintCheck --max-warnings 0

- name: Lint Backend
  run: cd backend && npm run lint:check
```

## IDE Setup

### Android Studio

1. **Install ktlint plugin**
2. **Enable auto-format on save**:
   - Settings > Tools > Actions on Save
   - ✅ Reformat code
   - ✅ Optimize imports

### VS Code

1. **Install extensions**:
   - ESLint
   - Prettier

2. **Settings**:
   ```json
   {
     "editor.formatOnSave": true,
     "editor.codeActionsOnSave": {
       "source.fixAll.eslint": true
     }
   }
   ```

---

**See Also**:
- [Development Guide](guide.md)
- [Contributing](contributing.md)

