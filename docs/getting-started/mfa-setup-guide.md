# MFA Setup Guide

Step-by-step guide for setting up Multi-Factor Authentication.

## What is MFA?

Multi-Factor Authentication (MFA) adds extra security layers beyond your password. Instead of just "something you know" (password), you also use:
- **Something you are** (fingerprint, face)
- **Something you have** (Yubikey, security key)

## Why Use MFA?

**Benefits**:
- 🔒 **Prevent account takeover** - Even if password is stolen
- 🛡️ **Phishing-resistant** - Hardware keys can't be phished
- ⚡ **Faster login** - Biometric is quicker than typing password
- 🏦 **Bank-grade security** - Same tech used by banks

## Quick Setup (2 minutes)

### 1. Enable Biometric (Recommended)

**Requirements**: Device with fingerprint or face recognition

**Steps**:
1. Open app → Go to **Settings**
2. Tap **Multi-Factor Authentication**
3. Toggle **Biometric Authentication** ON
4. Place finger on sensor (or look at camera)
5. ✅ Done! Biometric enabled

**Now you can**:
- Unlock app with fingerprint
- Quick authenticate without password

### 2. Add Hardware Key (Optional, High Security)

**Requirements**: 
- Yubikey or other FIDO2 key
- Android 14+ 

**Steps**:
1. Open app → **Settings** → **Multi-Factor Authentication**
2. Toggle **Hardware Security Keys** ON
3. Tap **Register Hardware Key**
4. Insert Yubikey (USB-C) or tap NFC
5. Enter PIN if prompted
6. ✅ Done! Yubikey registered

**Now you can**:
- Use Yubikey for high-security actions
- Add extra protection for adding/removing cards

### 3. Create Passkey (Optional, Passwordless)

**Requirements**: Android 14+, Google account

**Steps**:
1. Open app → **Settings** → **Multi-Factor Authentication**
2. Toggle **Passkeys** ON
3. Tap **Create Passkey**
4. Choose save location (device or Google)
5. Verify with biometric or PIN
6. ✅ Done! Passkey created

**Now you can**:
- Login without password (passkey only)
- Sync passkey across devices

## Configure Security Levels

Customize what authentication is required for each action.

**Steps**:
1. **Settings** → **Multi-Factor Authentication**
2. Scroll to **Security Levels per Action**
3. Tap any action (e.g., "Add Card")
4. Select desired security level:
   - **None**: No additional auth
   - **Biometric**: Fingerprint/face
   - **Hardware Key**: Yubikey required
   - **Biometric + Hardware Key**: Both required
   - **Passkey**: Passkey auth
5. ✅ Saved automatically

**Recommended Settings**:
```
✓ Unlock App → Biometric (fast & secure)
✓ Add Card → Biometric + Hardware Key (maximum security)
✓ Remove Card → Biometric (prevent accidents)
✓ Export Data → Biometric + Hardware Key (protect data)
```

## Troubleshooting

### Biometric Not Working

**Problem**: "Biometric authentication not available"

**Solutions**:
1. **Check if enrolled**:
   - Go to device **Settings** → **Security**
   - Add fingerprint or face
2. **Check permissions**:
   - Settings → Apps → CC Optimizer → Permissions
   - Enable "Biometric" permission
3. **Restart app** after enrolling biometric

### Yubikey Not Detected

**Problem**: "Insert your Yubikey" but nothing happens

**Solutions**:
1. **Check Android version**: Need Android 14+
2. **Check connection**:
   - USB-C: Fully insert key
   - NFC: Hold key to back of phone
3. **Check compatibility**: Must be FIDO2-compatible
4. **Try different port** (if using adapter)

### Passkey Creation Fails

**Problem**: "Could not create passkey"

**Solutions**:
1. **Check Android version**: Need Android 14+
2. **Check Google account**: Must be signed in
3. **Check Play Services**: Update Google Play Services
4. **Enable Password Manager**: Settings → Google → Password Manager

## Hardware Key Recommendations

### Best Options

**Yubikey 5C NFC** (~$55):
- ✅ USB-C connection
- ✅ NFC for wireless tap
- ✅ FIDO2 certified
- ✅ Waterproof and durable
- ✅ Multi-protocol support

**Google Titan Security Key** (~$30):
- ✅ USB-C or Bluetooth
- ✅ FIDO2 certified
- ✅ Made by Google
- ✅ Good value

**Feitian ePass FIDO** (~$20):
- ✅ Budget-friendly
- ✅ FIDO2 certified
- ✅ USB-C
- ✅ Good for testing

### Where to Buy
- Amazon: Search "Yubikey 5C NFC"
- Yubico.com: Official store
- Google Store: Titan key

## FAQ

**Q: Is MFA required?**  
A: No, all MFA methods are optional. You can use the app with just password.

**Q: Can I use multiple methods?**  
A: Yes! You can enable biometric + hardware key + passkey.

**Q: What if I lose my Yubikey?**  
A: You can still login with password + biometric, then register a new key.

**Q: Does biometric data leave my device?**  
A: No! Biometric data never leaves your device. Server only knows you passed biometric check.

**Q: Can I disable MFA later?**  
A: Yes, anytime in Settings → Multi-Factor Authentication.

**Q: What's the most secure setup?**  
A: Enable all three methods, set high-risk actions to "Biometric + Hardware Key".

---

**See Also**:
- [Multi-Factor Authentication Documentation](../features/multi-factor-authentication.md)
- [Security Overview](../security/overview.md)
- [Android Security](../security/android.md)

