# Contributing Guide

Guidelines for contributing to the Credit Card Payment Optimizer.

## Getting Started

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/cc.git
   cd cc
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/original/cc.git
   ```
4. **Install dependencies**: See [Installation Guide](../getting-started/installation.md)

## Development Process

### 1. Create Branch

```bash
git checkout -b feature/my-feature
```

**Branch naming**:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation
- `refactor/` - Code refactoring
- `test/` - Test additions

### 2. Make Changes

- Follow [Code Style Guide](code-style.md)
- Write tests (95%+ coverage required)
- Update documentation
- Keep commits atomic

### 3. Commit

```bash
git add .
git commit -m "feat: add payment notification feature"
```

**Commit message format**:
```
<type>: <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Tests
- `chore`: Maintenance

**Examples**:
```
feat: add dark mode support

fix: correct payment calculation for leap years

docs: update API documentation for v1.1

test: add tests for payment calculator
```

### 4. Test

```bash
# Android
./gradlew test
./gradlew ktlintCheck

# Backend
npm test
npm run lint:check
```

**All tests must pass!**

### 5. Push

```bash
git push origin feature/my-feature
```

### 6. Create Pull Request

1. Go to GitHub repository
2. Click "New Pull Request"
3. Select your branch
4. Fill in PR template:
   - Description of changes
   - Related issues
   - Testing done
   - Screenshots (if UI changes)

## Pull Request Guidelines

### PR Checklist

- [ ] Tests pass
- [ ] Linting passes
- [ ] Documentation updated
- [ ] Commit messages follow convention
- [ ] PR description is clear
- [ ] No merge conflicts

### PR Review

All PRs require:
- ✅ Automated checks passing
- ✅ Code review approval
- ✅ Test coverage maintained (95%+)
- ✅ No linting errors

## Code Review Process

1. **Submit PR**
2. **Automated checks run**
3. **Reviewer assigned**
4. **Address feedback**
5. **Approve & merge**

### Addressing Feedback

```bash
# Make changes
git add .
git commit -m "refactor: address PR feedback"
git push origin feature/my-feature
```

## Areas for Contribution

### High Priority
- [ ] iOS app (Swift/SwiftUI)
- [ ] Web dashboard
- [ ] Payment scheduling
- [ ] Multiple payment strategies

### Medium Priority
- [ ] Credit score tracking
- [ ] Rewards optimization
- [ ] Bill splitting
- [ ] Spending insights

### Low Priority
- [ ] Multi-language support
- [ ] Accessibility improvements
- [ ] Additional bank integrations

## Reporting Issues

### Bug Reports

Include:
- **Description**: What happened?
- **Expected**: What should happen?
- **Steps to reproduce**
- **Environment**: Android version, app version
- **Logs**: Relevant error messages
- **Screenshots**: If applicable

**Template**:
```markdown
**Description**
Brief description of the bug

**To Reproduce**
1. Open app
2. Tap on ...
3. See error

**Expected Behavior**
What should have happened

**Environment**
- Android version: 14
- App version: 1.0.0
- Device: Pixel 6

**Logs**
```
Error logs here
```

**Screenshots**
If applicable
```

### Feature Requests

Include:
- **Description**: What feature?
- **Use case**: Why is it needed?
- **Proposed solution**: How should it work?
- **Alternatives**: Other approaches considered?

## Questions?

- Check [FAQ](faq.md)
- Check [Documentation](../README.md)
- Open a GitHub Discussion
- Ask in community chat

## Code of Conduct

- Be respectful
- Be constructive
- Be collaborative
- Be patient

## License

By contributing, you agree that your contributions will be licensed under the project's license.

---

**See Also**:
- [Development Guide](guide.md)
- [Code Style](code-style.md)
- [Testing](testing.md)

