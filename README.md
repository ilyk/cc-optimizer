# Credit Card Payment Optimizer

Android app that calculates optimal credit card payment timing to minimize reported balances and improve credit utilization.

## What it does

Connects to your bank accounts via Plaid and tells you when to pay your credit cards so that statement balances (the ones reported to credit bureaus) are as low as possible.

The problem: credit utilization affects 30% of your credit score. If you pay after your statement closes, even if you pay in full, the high balance still gets reported. This app calculates the exact date to pay before the statement closes, accounting for bank processing times, weekends, and holidays.

Example:
- Statement closes on the 18th
- Bank takes 2-3 days to process payments
- You have a $3,000 balance
- App tells you to pay by the 15th
- Statement closes with $0 balance → 0% utilization reported

## Quick Start

1. Set up the backend:
```bash
cd backend
npm install
cp .env.example .env
# Add your Plaid credentials to .env
npm start
```

2. Open the Android app in Android Studio and run it

3. Use Plaid sandbox credentials: `user_good` / `pass_good`

See [docs/getting-started/quick-start.md](docs/getting-started/quick-start.md) for detailed setup.

## Tech Stack

**Android**
- Kotlin with Jetpack Compose
- MVVM architecture
- Room database
- Retrofit for networking
- Min SDK 26, Target SDK 34

**Backend**
- Node.js + Express
- SQLite (dev) / PostgreSQL (prod)
- Plaid API for bank data
- JWT authentication
- Optional: OpenAI/Anthropic for AI statement date detection

## Features

- Plaid integration for secure bank connectivity
- Automatic statement date detection (AI-powered fallback)
- Business day calculator (handles weekends and federal holidays)
- Multi-card support
- Dark mode
- Offline caching
- Push notifications for payment reminders
- Certificate pinning

## Security

Standard security practices implemented:
- HTTPS with certificate pinning
- JWT authentication
- AES-256 encryption for sensitive data
- ProGuard/R8 code obfuscation
- Input validation and rate limiting
- Root/tamper detection

Compliant with OWASP Mobile Top 10 and API Security Top 10.

## Documentation

Full documentation in [/docs](docs/):

- [Quick Start](docs/getting-started/quick-start.md)
- [System Architecture](docs/architecture/system-overview.md)
- [API Reference](docs/api/overview.md)
- [Security Overview](docs/security/overview.md)
- [Development Guide](docs/development/guide.md)
- [FAQ](docs/development/faq.md)

## Testing

```bash
# Android tests
./gradlew test

# Backend tests
cd backend
npm test

# Coverage report
npm run coverage
```

Coverage requirement: 95%+

## Project Structure

```
.
├── app/              # Android app (Kotlin)
├── backend/          # Node.js API server
├── docs/             # Documentation
└── infra/            # Docker configs
```

## Contributing

See [docs/development/contributing.md](docs/development/contributing.md) for guidelines.

## License

Private project.
