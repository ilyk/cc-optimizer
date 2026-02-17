# Privacy Policy

**Last Updated:** October 17, 2025

## Our Commitment

CC Optimizer is built with privacy as a core principle. We believe your financial data is yours alone.

**We do not sell or share your personal data.** Your information stays on your device.

## What We Collect (Locally on Your Device)

All data is stored **locally on your device** using encrypted storage. Nothing is sent to external servers by default.

We store:

- **Credit card metadata:** Issuer name (e.g., "Chase Sapphire"), last 4 digits, credit limit
- **Statement dates:** Statement closing date and payment due date
- **Balance snapshots:** Current balance, available credit (at the time you sync)
- **Payment cycles:** Calculated optimal payment dates based on your card data
- **Sync tokens:** If you connect to Plaid or your self-hosted backend, auth tokens are stored encrypted

## What We DO NOT Collect

We deliberately avoid collecting:

- ❌ **Purchase categories** or transaction details
- ❌ **Merchant names** or spending patterns
- ❌ **Your identity:** No name, email, or phone required to use core features
- ❌ **Analytics or tracking:** Default OFF; see Telemetry section below

## Data Storage & Encryption

### Local Encryption

All sensitive data is encrypted using Android's best practices:

1. **Database encryption:** Room database encrypted with SQLCipher using AES-256
2. **Secure preferences:** Tokens and settings use `EncryptedSharedPreferences` with AES-GCM via Android Keystore
3. **Master key:** Keys are generated and stored in Android Keystore (hardware-backed on supported devices)

### No Cloud Sync (By Default)

By default, your data **never leaves your device**. 

**Data retention:** Your data persists only on your device until you delete it. We do not maintain server copies by default.

Optional sync features:

- **Plaid Link:** If you link bank accounts, connection tokens are stored encrypted. Plaid's privacy policy applies to their service.
- **Self-hosted backend:** You can optionally connect to your own server (see SECURITY.md). We do not operate any cloud backend.

## Data Deletion

You have complete control over your data:

### Export Your Data

Go to **Settings → Privacy & Security → Export Data** to save all your cards and cycles as JSON.

### Delete All Data

Go to **Settings → Privacy & Security → Delete All Data**. This:

1. Requires biometric or device credential authentication
2. Permanently wipes:
   - All credit cards and payment cycles
   - All encrypted preferences and tokens
   - All cached data and temp files
   - All background jobs
3. Cannot be undone

## Telemetry (Opt-In, Self-Hosted Only)

**Default: OFF**

We do not include any third-party analytics or tracking SDKs (no Google Analytics, no Firebase Analytics, no Crashlytics by default).

### If You Enable Self-Hosted Analytics

You can optionally send minimal usage events to **your own server**:

1. Go to **Settings → Privacy & Security → Self-Hosted Analytics**
2. Provide your HTTPS endpoint URL
3. Preview the exact JSON payload we send
4. Enable the toggle

**What we send:**
```json
{
  "event": "screen_view",
  "screen_name": "main_screen",
  "timestamp": 1697500800000,
  "app_version": "1.0.0"
}
```

**We do NOT send:**
- Financial data (balances, limits, dates)
- Personal identifiers (device ID, email, IP address)
- Transaction details

## Third-Party Services

### Plaid (Optional)

If you choose to link your bank accounts via Plaid:

- Plaid's privacy policy applies: https://plaid.com/legal/#privacy-policy
- We store Plaid access tokens encrypted on your device
- We do not have access to your Plaid credentials

### Social Auth (Optional)

If you choose to log in with Google or Facebook:

- Only used for account authentication (if using self-hosted backend)
- We do not access your social graph or post on your behalf
- Their privacy policies apply

## Children's Privacy

This app is not intended for children under 13. We do not knowingly collect data from children.

## Changes to This Policy

We will update this document when our privacy practices change. Check the "Last Updated" date at the top.

## Contact

For privacy questions or to report concerns:

**Privacy inquiries:** privacy@ccoptimizer.app  
**Security issues:** security@ccoptimizer.app (see SECURITY.md for responsible disclosure)

We will respond to privacy inquiries within 5 business days.

## Your Rights

Depending on your location, you may have rights under GDPR, CCPA, or other privacy laws. Since we don't collect data on servers, most rights (access, deletion, portability) are already built into the app:

- **Access:** Your data is always visible in the app
- **Deletion:** Use "Delete All Data" feature
- **Portability:** Use "Export Data" feature

### Data Subject Requests

For formal data subject requests (access, erasure, export), email **privacy@ccoptimizer.app** with your request. We will respond within 5 business days.

If you have questions about your rights, contact us at the email above.

---

**Summary:** Your financial data stays on your device, encrypted. We don't track you. You can export or delete everything at any time.

