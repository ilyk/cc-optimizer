# Linter & Code Style Configuration 📝

Complete linting setup for consistent code quality across the project.

---

## Overview

**Backend (Node.js)**: ESLint + Prettier  
**Android (Kotlin)**: ktlint + EditorConfig  
**Both**: EditorConfig for cross-IDE consistency

---

## Backend - ESLint Configuration

### Setup

```bash
cd backend
npm install
```

### Usage

```bash
# Check for issues
npm run lint

# Auto-fix issues
npm run lint:fix

# Check with zero warnings (CI/CD)
npm run lint:check

# Format code
npm run format

# Check formatting
npm run format:check
```

### Configuration Files

**`.eslintrc.js`** - ESLint rules
- Error prevention (no-console in production, no-debugger)
- Best practices (eqeqeq, curly, no-eval)
- Async/await rules
- Code style (indent, quotes, semi)
- Complexity limits
- Security rules (optional plugins)

**`.eslintignore`** - Ignored files
- `node_modules/`
- `dist/`, `build/`
- `coverage/`
- `.env` files

**`.prettierrc.js`** - Formatting rules
- Print width: 100
- Tab width: 2
- Single quotes: true
- Semi: true
- Trailing commas: es5

### Customization

Environment variables:
```bash
# Prettier
PRETTIER_PRINT_WIDTH=120
PRETTIER_TAB_WIDTH=2
PRETTIER_SINGLE_QUOTE=true
PRETTIER_SEMI=true
PRETTIER_TRAILING_COMMA=es5
```

### Example Output

```bash
$ npm run lint

/backend/server.js
  12:1   error    'foo' is not defined              no-undef
  23:5   warning  Unexpected console statement      no-console
  45:10  error    Missing semicolon                 semi

✖ 3 problems (2 errors, 1 warning)
  1 error and 0 warnings potentially fixable with the --fix option
```

---

## Android - ktlint Configuration

### Setup

Add to `app/build.gradle.kts`:
```kotlin
apply(from = "config/ktlint.gradle.kts")
```

### Usage

```bash
# Check Kotlin code style
./gradlew ktlintCheck

# Auto-fix violations
./gradlew ktlintFormat

# Run with check task
./gradlew check
```

### Configuration Files

**`config/ktlint.gradle.kts`** - Gradle integration
- Defines ktlint tasks
- Integrates with check task

**`.ktlint.editorconfig`** - ktlint rules
- Kotlin official style
- Android Studio code style
- Max line length: 120
- Indent: 4 spaces
- Trailing commas: enabled
- Import ordering

**`app/.editorconfig`** - Editor settings
- Kotlin-specific settings
- Inherits from root

### Example Output

```bash
$ ./gradlew ktlintCheck

> Task :app:ktlintCheck
/app/src/main/java/com/ccoptimizer/app/MainActivity.kt:12:1: 
  Unexpected blank line(s) before "}"
/app/src/main/java/com/ccoptimizer/app/MainActivity.kt:45:120: 
  Exceeded max line length (120)

BUILD FAILED
2 errors
```

---

## EditorConfig

### What is EditorConfig?

Maintains consistent coding styles across different editors and IDEs.

### Files

**`/.editorconfig`** (root)
- Default for all files
- JavaScript: 2 spaces
- Kotlin: 4 spaces
- YAML: 2 spaces
- Markdown: no trim whitespace

**`/app/.editorconfig`** (Android)
- Kotlin-specific settings
- Inherits from root

### Supported Editors

- VS Code (install EditorConfig extension)
- IntelliJ IDEA (built-in)
- Android Studio (built-in)
- Sublime Text (install EditorConfig package)
- Vim (install editorconfig-vim)

---

## CI/CD Integration

### GitHub Actions

```yaml
# .github/workflows/lint.yml
name: Lint

on: [push, pull_request]

jobs:
  lint-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install dependencies
        run: cd backend && npm ci
      - name: Run ESLint
        run: cd backend && npm run lint:check
      - name: Check formatting
        run: cd backend && npm run format:check

  lint-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Run ktlint
        run: ./gradlew ktlintCheck
```

### Pre-commit Hooks (Optional)

Install husky:
```bash
cd backend
npm install --save-dev husky lint-staged

# Initialize husky
npx husky install

# Add pre-commit hook
npx husky add .husky/pre-commit "cd backend && npm run lint-staged"
```

**`package.json`**:
```json
{
  "lint-staged": {
    "*.js": [
      "eslint --fix",
      "prettier --write"
    ]
  }
}
```

---

## Rules Summary

### ESLint Rules

| Category | Rule | Level | Reason |
|----------|------|-------|--------|
| **Errors** | no-console | warn (prod) | Prevent console in production |
| | no-debugger | error (prod) | No debugger in production |
| | no-unused-vars | error | Clean code |
| **Best Practices** | eqeqeq | error | Use === instead of == |
| | curly | error | Always use braces |
| | no-eval | error | Security |
| **Async** | require-await | warn | Async without await |
| | no-await-in-loop | warn | Performance |
| **Style** | indent | error (2 spaces) | Consistency |
| | quotes | error (single) | Consistency |
| | semi | error (always) | Consistency |
| **Complexity** | max-depth | warn (4) | Readability |
| | max-lines-per-function | warn (100) | Maintainability |
| | complexity | warn (15) | Complexity limit |

### ktlint Rules

| Category | Rule | Setting |
|----------|------|---------|
| **Style** | Code style | android_studio |
| | Max line length | 120 |
| | Indent | 4 spaces |
| **Imports** | Wildcard imports | Allowed (configurable) |
| | Star import count | 5 |
| **Formatting** | Trailing commas | Enabled |
| | Blank lines in code | Max 1 |
| **Experimental** | Experimental rules | Enabled |

---

## Customization Guide

### Disable Specific Rules

**ESLint** (`.eslintrc.js`):
```javascript
rules: {
  'no-console': 'off',  // Disable
  'semi': 'warn',       // Downgrade to warning
}
```

**ktlint** (`.ktlint.editorconfig`):
```ini
disabled_rules = no-wildcard-imports,import-ordering
```

### Project-Specific Rules

**Override for specific files**:
```javascript
// .eslintrc.js
overrides: [
  {
    files: ['*.test.js'],
    rules: {
      'max-lines-per-function': 'off'
    }
  }
]
```

### Team Configuration

Create **`.eslintrc.local.js`** (gitignored):
```javascript
module.exports = {
  extends: './.eslintrc.js',
  rules: {
    // Team-specific overrides
    'indent': ['error', 4],  // Use 4 spaces
  }
};
```

---

## Best Practices

### DO ✅

1. **Run linter before commit**
   ```bash
   npm run lint:fix
   ./gradlew ktlintFormat
   ```

2. **Use auto-fix for simple issues**
   - Saves time
   - Consistent formatting

3. **Configure your IDE**
   - Install ESLint extension (VS Code)
   - Enable "Format on Save"
   - Use EditorConfig

4. **Add linting to CI/CD**
   - Fail build on errors
   - Run on every PR

5. **Review linter warnings**
   - Fix before they become errors
   - Address complexity warnings

### DON'T ❌

1. **Don't disable all rules**
   - Only disable specific problematic rules
   - Document why you disabled them

2. **Don't ignore linter errors**
   - Fix them, don't suppress them
   - Use `// eslint-disable-next-line` sparingly

3. **Don't commit with linter errors**
   - Use pre-commit hooks to prevent

4. **Don't use different styles**
   - Stick to project configuration
   - Discuss changes with team

5. **Don't over-configure**
   - Start with defaults
   - Add rules as needed

---

## Troubleshooting

### Issue: ESLint not running

**Check**:
```bash
cd backend
npm list eslint
```

**Solution**: Reinstall
```bash
npm install --save-dev eslint
```

### Issue: ktlint not found

**Check**:
```bash
./gradlew tasks --all | grep ktlint
```

**Solution**: Ensure `apply(from = "config/ktlint.gradle.kts")` in `build.gradle.kts`

### Issue: EditorConfig not working

**Solution**:
1. Install EditorConfig extension/plugin
2. Restart editor
3. Check `.editorconfig` is in project root

### Issue: Too many errors

**Solution**: Fix incrementally
```bash
# Fix auto-fixable issues first
npm run lint:fix

# Then address remaining manually
npm run lint
```

---

## Migration Guide

### Existing Codebase

1. **Install linters**
   ```bash
   cd backend && npm install
   ```

2. **Check current state**
   ```bash
   npm run lint > lint-report.txt
   ```

3. **Auto-fix what you can**
   ```bash
   npm run lint:fix
   ```

4. **Fix remaining issues**
   - Review `lint-report.txt`
   - Fix manually or disable rules temporarily

5. **Gradual enforcement**
   ```javascript
   // Start with warnings
   rules: {
     'no-console': 'warn',  // Later: 'error'
   }
   ```

6. **Document exceptions**
   ```javascript
   // Disable with comment
   // eslint-disable-next-line no-console -- Debugging production issue
   console.log('Debug info');
   ```

---

## IDE Integration

### VS Code

**Extensions**:
- ESLint
- Prettier - Code formatter
- EditorConfig for VS Code
- Kotlin Language

**Settings** (`.vscode/settings.json`):
```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "eslint.autoFixOnSave": true,
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[kotlin]": {
    "editor.defaultFormatter": "fwcd.kotlin"
  }
}
```

### Android Studio / IntelliJ

**Settings**:
1. Preferences → Editor → Code Style
2. Import from `.editorconfig`
3. Enable ktlint inspection
4. Configure "Optimize imports on save"

---

## Summary

### What We Configured

✅ **ESLint** - JavaScript linting (100+ rules)  
✅ **Prettier** - Code formatting  
✅ **ktlint** - Kotlin linting  
✅ **EditorConfig** - Cross-IDE consistency  
✅ **Scripts** - Easy commands (`npm run lint`)  
✅ **Configuration** - All customizable via env vars  

### Files Created

- `backend/.eslintrc.js` - ESLint configuration
- `backend/.eslintignore` - ESLint ignore patterns
- `backend/.prettierrc.js` - Prettier configuration
- `backend/.prettierignore` - Prettier ignore patterns
- `app/config/ktlint.gradle.kts` - ktlint Gradle configuration
- `app/.ktlint.editorconfig` - ktlint rules
- `.editorconfig` - Root editor configuration
- `app/.editorconfig` - Android editor configuration
- `LINTER_SETUP.md` - This guide

### Quick Commands

**Backend**:
```bash
npm run lint        # Check
npm run lint:fix    # Fix
npm run format      # Format
```

**Android**:
```bash
./gradlew ktlintCheck   # Check
./gradlew ktlintFormat  # Fix
```

---

*Consistent code quality, enforced automatically!* ✨

