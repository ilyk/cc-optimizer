# Troubleshooting Guide

Complete troubleshooting guide for common issues.

## Table of Contents

- [Backend Issues](#backend-issues)
- [Android App Issues](#android-app-issues)
- [Plaid Integration Issues](#plaid-integration-issues)
- [Network Issues](#network-issues)
- [Build Issues](#build-issues)
- [Database Issues](#database-issues)
- [Security Issues](#security-issues)
- [Performance Issues](#performance-issues)

## Backend Issues

### Backend won't start

**Error**: `Port 3000 is already in use`

**Solution**:
```bash
# Find process using port 3000
lsof -i :3000

# Kill the process
kill -9 <PID>

# Or use a different port
PORT=3001 npm start
```

---

**Error**: `Cannot find module 'dotenv'`

**Solution**:
```bash
cd backend
npm install
```

---

**Error**: `PLAID_CLIENT_ID is not defined`

**Solution**:
1. Create `.env` file in `backend/` directory
2. Add Plaid credentials:
```env
PLAID_CLIENT_ID=your_client_id
PLAID_SECRET=your_sandbox_secret
PLAID_ENV=sandbox
JWT_SECRET=your-secret-key
```

---

**Error**: `Database connection failed`

**Solution**:
```bash
# Check database file exists
ls -la backend/data/

# Create data directory if missing
mkdir -p backend/data

# Restart server (will recreate database)
npm start
```

---

### Backend crashes randomly

**Check logs**:
```bash
# Enable debug logging
LOG_LEVEL=debug npm start

# Check for errors
npm start 2>&1 | grep -i error
```

**Common causes**:
1. Database corruption - Delete `backend/data/*.db` and restart
2. Memory leak - Restart server periodically
3. Plaid API errors - Check Plaid dashboard for issues

---

### JWT token errors

**Error**: `JsonWebTokenError: invalid signature`

**Solution**:
- JWT_SECRET changed - Users need to re-login
- Check `.env` has consistent JWT_SECRET

**Error**: `TokenExpiredError: jwt expired`

**Solution**:
- Normal behavior (tokens expire after 7 days)
- User needs to login again
- Check token expiration in `authService.js`

---

## Android App Issues

### App won't build

**Error**: `Gradle sync failed`

**Solution**:
```bash
# Invalidate caches
# Android Studio: File > Invalidate Caches > Invalidate and Restart

# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

---

**Error**: `Unsupported Java version`

**Solution**:
- This project requires JDK 17
- Check current version: `java -version`
- Set JDK in Android Studio:
  - File > Project Structure > SDK Location > JDK location

---

**Error**: `Cannot resolve symbol 'R'`

**Solution**:
```bash
# Clean project
./gradlew clean

# Sync Gradle
# Android Studio: File > Sync Project with Gradle Files

# Rebuild
Build > Rebuild Project
```

---

### App crashes on startup

**Check logcat**:
```bash
adb logcat | grep AndroidRuntime
```

**Common crashes**:

1. **NetworkSecurityException**: Certificate pinning issue
   - Use debug build (pins not required)
   - Or extract pins: `./gradlew getCertificatePins`

2. **SQLiteException**: Database error
   - Clear app data: `adb shell pm clear com.ccoptimizer.app`

3. **SecurityException**: Permission issue
   - Check AndroidManifest.xml permissions
   - Grant runtime permissions on device

---

### App freezes/hangs

**Check**:
1. Network request blocking UI thread?
   - All network calls should use coroutines
2. Database query on main thread?
   - Use Room's suspend functions
3. Heavy computation?
   - Move to background thread

**Debug**:
```kotlin
// Add logging
Log.d("CC_OPTIMIZER", "Before operation")
// operation here
Log.d("CC_OPTIMIZER", "After operation")
```

---

### Memory leaks

**Symptoms**:
- App gets slower over time
- Eventually crashes with OutOfMemoryError

**Debug**:
- Android Studio: Profiler > Memory
- Look for growing heap
- Check for static references to Context
- Ensure coroutines are cancelled

**Fix**:
```kotlin
// In ViewModel
override fun onCleared() {
    super.onCleared()
    viewModelScope.cancel() // Cancel all coroutines
}
```

---

## Plaid Integration Issues

### Plaid Link doesn't open

**Check**:
1. Backend is running: `curl http://localhost:3000/health`
2. Backend URL correct in `RetrofitClient.kt`
3. Link token created successfully

**Debug**:
```kotlin
// Add logging in ViewModel
linkToken.collect { token ->
    Log.d("PLAID", "Link token: $token")
    // ...
}
```

**Test backend**:
```bash
# Create link token
curl -X POST http://localhost:3000/api/v1/plaid/create_link_token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### "invalid_credentials" error

**Cause**: Using wrong Plaid environment

**Check `.env`**:
```env
PLAID_ENV=sandbox  # For testing
# PLAID_ENV=development  # For Development environment
# PLAID_ENV=production  # For Production
```

**Sandbox credentials**:
- Username: `user_good`
- Password: `pass_good`

---

### No accounts returned

**Causes**:
1. No accounts selected in Plaid Link
2. Account type filter (only credit cards)
3. API error

**Debug**:
```bash
# Test accounts endpoint
curl http://localhost:3000/api/v1/plaid/accounts \
  -H "Authorization: Bearer YOUR_JWT"
```

**Check**:
- Selected credit card accounts in Plaid Link?
- Account type is "credit"?
- Check backend logs for errors

---

### Transactions empty

**Causes**:
1. No transactions in date range
2. Date range too narrow
3. Plaid sync not complete

**Solution**:
```kotlin
// Use wider date range
val endDate = LocalDate.now()
val startDate = endDate.minusDays(90) // 90 days
```

---

### "item_login_required" webhook

**Meaning**: User needs to re-authenticate via Plaid Link

**Causes**:
- Bank password changed
- Bank requires multi-factor auth
- Plaid item expired

**Solution**:
1. Create new link token with update mode
2. Open Plaid Link for user to re-authenticate
3. Access token remains valid after re-auth

---

## Network Issues

### Cannot connect to backend from emulator

**Error**: `Failed to connect to localhost:3000`

**Solution**: Use `10.0.2.2` instead of `localhost`

```kotlin
// RetrofitClient.kt
private const val BASE_URL = "http://10.0.2.2:3000/"
```

**Why**: Android emulator uses `10.0.2.2` to access host machine's `localhost`

---

### Cannot connect from physical device

**Error**: `Failed to connect to 192.168.x.x:3000`

**Checklist**:
1. Device and computer on same WiFi?
2. Firewall blocking port 3000?
3. Backend listening on `0.0.0.0` not just `127.0.0.1`?

**Find computer IP**:
```bash
# Linux/Mac
ip addr show | grep "inet "
# or
ifconfig | grep "inet "

# Windows
ipconfig
```

**Test from device browser**:
```
http://YOUR_IP:3000/health
```

**Configure backend to listen on all interfaces**:
```javascript
// server.js
app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
});
```

---

### SSL/Certificate errors

**Error**: `javax.net.ssl.SSLHandshakeException`

**For production** (with certificate pinning):
1. Extract pins from your backend:
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
```
2. Rebuild app with pins

**For development** (without pins):
- Use debug build variant (pins not enforced)

---

### "Rate Limited" errors

**Error**: `429 Too Many Requests`

**Cause**: Exceeded rate limits

**Limits**:
- Auth endpoints: 5 requests/15 minutes
- Plaid endpoints: 20 requests/minute
- General API: 100 requests/15 minutes

**Solution**:
- Wait for rate limit window to reset
- App should show cached data automatically (smart rate limiting)
- Check for request loops in code

---

## Build Issues

### ProGuard/R8 errors

**Error**: `R8: Unexpected exception while processing`

**Solution**:
1. Check `proguard-rules.pro` for correct rules
2. Add keep rules for libraries:
```proguard
# Retrofit
-keepattributes Signature
-keep class retrofit2.** { *; }

# Plaid
-keep class com.plaid.** { *; }
```

3. Try disabling minification temporarily:
```kotlin
// build.gradle.kts
buildTypes {
    release {
        isMinifyEnabled = false  // Disable temporarily
    }
}
```

---

### Certificate pins missing (production build)

**Error**: `Certificate pins are required for production builds`

**Solution**:
1. Ensure backend is running
2. Extract pins:
```bash
./gradlew getCertificatePins -PBACKEND_URL=https://your-backend.com
```
3. Pins saved to `local.properties`
4. Rebuild

**For CI/CD**:
```bash
# Set environment variable
export BACKEND_URL=https://your-backend.com

# Build will automatically extract pins
./gradlew assembleRelease
```

---

### Kotlin compiler errors

**Error**: `Kotlin: Unresolved reference`

**Solution**:
1. Sync Gradle: `File > Sync Project with Gradle Files`
2. Invalidate caches: `File > Invalidate Caches > Invalidate and Restart`
3. Check Kotlin version in `build.gradle.kts`

---

## Database Issues

### Database locked

**Error**: `SQLiteDatabaseLockedException: database is locked`

**Cause**: Multiple simultaneous writes

**Solution**:
```kotlin
// Use transactions
database.withTransaction {
    // Multiple operations here
}

// Or ensure single write at a time
mutex.withLock {
    dao.insert(item)
}
```

---

### Migration failed

**Error**: `IllegalStateException: Migration didn't properly handle`

**Solution**:
```kotlin
// During development, can destroy and recreate
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()  // Temporary!
    .build()

// For production, add proper migration
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Migration logic here
    }
}
```

---

### Data not persisting

**Check**:
1. Are you using correct DAO methods?
2. Is transaction committed?
3. Is database instance singleton?

**Debug**:
```kotlin
// Verify data written
val result = dao.getAll()
Log.d("DB", "Items in DB: ${result.size}")
```

---

## Security Issues

### Root detection false positive

**Issue**: App warns about root on non-rooted device

**Causes**:
- Certain custom ROMs
- Emulators
- Developer tools

**Solution** (for testing only):
```kotlin
// SecurityManager.kt
fun isDeviceSafe(): Boolean {
    if (BuildConfig.DEBUG) return true  // Skip checks in debug
    return !isRooted() && !isTampered()
}
```

**Never disable in production!**

---

### Tamper detection false positive

**Issue**: App shows tamper warning on legitimate APK

**Causes**:
- Debug APK on production code
- Unsigned APK
- APK signed with different key

**Solution**:
- Use debug build variant for development
- Use release build with proper signing for production
- Check signature in `SecurityManager.kt` matches expected

---

### Certificate pinning blocking requests

**Issue**: All HTTPS requests fail with `CertificateException`

**Causes**:
1. Wrong pins in `network_security_config.xml`
2. Backend SSL certificate changed
3. Using self-signed certificate

**Debug**:
```bash
# Get current certificate pins
openssl s_client -connect your-backend.com:443 \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

**Solution**:
1. Re-extract pins: `./gradlew getCertificatePins`
2. Rebuild app

---

## Performance Issues

### Slow initial load

**Expected**: 2-3 seconds (fetching from Plaid)

**If slower than 5 seconds**:
1. Check network speed
2. Check Plaid API status
3. Enable parallel requests (already enabled)

**Optimize**:
```kotlin
// Fetch accounts and transactions in parallel
coroutineScope {
    val accountsDeferred = async { getAccounts() }
    val transactionsDeferred = async { getTransactions() }
    val accounts = accountsDeferred.await()
    val transactions = transactionsDeferred.await()
}
```

---

### Slow subsequent loads

**Expected**: ~100ms (from cache)

**If slower than 500ms**:
1. Check cache implementation
2. Check database query performance
3. Check if actually using cache

**Debug**:
```kotlin
// Add timing logs
val startTime = System.currentTimeMillis()
val data = repository.getData()
val endTime = System.currentTimeMillis()
Log.d("PERF", "Load took ${endTime - startTime}ms")
```

---

### High memory usage

**Check**:
```bash
# In Android Studio
View > Tool Windows > Profiler > Memory
```

**Common causes**:
1. Loading too many transactions
2. Not clearing lists/caches
3. Memory leaks (static Context references)

**Solutions**:
- Implement pagination for transactions
- Clear caches when appropriate
- Use weak references where needed
- Profile and fix leaks

---

### High battery drain

**Causes**:
1. Too many background syncs
2. WakeLocks not released
3. Constant network requests

**Check**:
- Settings > Battery > App battery usage

**Solutions**:
- Reduce sync frequency
- Use WorkManager for background tasks
- Implement smart sync (only when needed)

---

## Logging & Debugging

### Enable verbose logging

**Android**:
```kotlin
// In RetrofitClient.kt
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
```

**Backend**:
```bash
LOG_LEVEL=debug npm start
```

---

### View all logs

**Android**:
```bash
# All logs
adb logcat

# Filter by tag
adb logcat -s "CC_OPTIMIZER"

# Filter by level
adb logcat *:E  # Errors only

# Save to file
adb logcat > logs.txt
```

**Backend**:
```bash
# Console output
npm start

# Save to file
npm start > logs.txt 2>&1
```

---

### Analyze network requests

**Android Studio**:
1. View > Tool Windows > Profiler
2. Click "+" > Select app
3. Network tab
4. View all requests, responses, timing

**Charles Proxy** (for deeper inspection):
1. Install Charles Proxy
2. Configure device to use proxy
3. Install Charles SSL certificate on device
4. View all HTTP/HTTPS traffic

---

## Getting Help

If you're still stuck:

1. **Check documentation**:
   - [FAQ](faq.md)
   - [API Documentation](../api/overview.md)
   - [Architecture](../architecture/system-overview.md)

2. **Check logs**:
   - Android logcat
   - Backend server logs
   - Chrome DevTools (for network)

3. **Search issues**:
   - GitHub issues
   - Stack Overflow
   - Plaid documentation

4. **Create an issue**:
   - Provide logs
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (Android version, etc.)

---

**Most issues can be resolved by**:
1. Checking logs for specific errors
2. Verifying configuration (`.env`, URLs, etc.)
3. Ensuring backend is running
4. Clearing app data and cache
5. Rebuilding the app

**When in doubt**: Clean, rebuild, restart!

