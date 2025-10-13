# API Versioning Strategy 📋

## Overview

Comprehensive API versioning implementation for production-ready backward compatibility and smooth upgrades.

---

## Versioning Scheme

### Current Version: **v1**

All API endpoints are prefixed with version number:
```
/api/v1/auth/login
/api/v1/plaid/accounts
/api/v1/cards
```

### Why Versioning?

| Benefit | Description |
|---------|-------------|
| **Backward Compatibility** | Old clients continue working during updates |
| **Safe Deployments** | Roll out breaking changes without breaking existing apps |
| **Gradual Migration** | Users upgrade at their own pace |
| **A/B Testing** | Test new features with subset of users |
| **Documentation** | Clear separation between versions |

---

## Implementation

### Backend (Express Router)

**File**: `backend/server.js`

```javascript
// Create versioned router
const apiV1 = express.Router();

// Add all routes to v1 router
apiV1.post('/auth/login', authMiddleware, handler);
apiV1.get('/plaid/accounts', authMiddleware, handler);
// ... more routes

// Mount v1 router
app.use('/api/v1', apiV1);
```

**Result**:
- All routes automatically prefixed with `/api/v1`
- Easy to create v2 in the future
- Clean separation of versions

### Android Client

**File**: `app/.../PlaidApi.kt`

```kotlin
interface PlaidApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("api/v1/plaid/accounts")
    suspend fun getAccounts(@Header("Authorization") token: String): Response<List<PlaidAccount>>
}
```

**Result**:
- All API calls use versioned endpoints
- Future-proof for v2 migration

---

## Backward Compatibility

### Deprecation Strategy

Old `/api/*` endpoints are supported temporarily with:
- **Deprecation warnings** in logs
- **HTTP headers** indicating deprecation
- **90-day sunset** period

```javascript
// Backward compatibility middleware
app.use('/api', (req, res, next) => {
  if (req.path.startsWith('/v1')) return next();
  
  // Warn about deprecated endpoint
  logger.warn(`Deprecated: ${req.method} ${req.path} - Use /api/v1${req.path}`);
  
  // Add deprecation headers
  res.setHeader('X-API-Deprecated', 'true');
  res.setHeader('X-API-Version', 'v1');
  res.setHeader('Deprecation', 'version="legacy"');
  res.setHeader('Sunset', '2025-04-15'); // 90 days from now
  
  // Redirect to v1
  req.url = `/v1${req.url}`;
  apiV1(req, res, next);
});
```

### Migration Timeline

```
Week 1-4:   Deploy v1 endpoints
            Old /api/* still works (deprecated)
            Monitor deprecation header usage

Week 5-8:   Send notifications to users still on old endpoints
            Update documentation
            Update client examples

Week 9-12:  Final warnings
            Prepare for sunset

Week 13:    Remove backward compatibility
            Only /api/v1/* works
```

---

## Version Discovery

### Health Check Endpoint

```bash
GET /health

Response:
{
  "status": "ok",
  "message": "Credit Card Optimizer Backend is running",
  "timestamp": "2025-01-15T10:30:00Z",
  "environment": "production",
  "apiVersion": "v1",
  "supportedVersions": ["v1"]
}
```

Clients can check:
- Current default version
- All supported versions
- Version-specific capabilities

---

## Response Headers

All API responses include version information:

```http
X-API-Version: v1
X-API-Deprecated: false
```

Deprecated endpoints additionally include:

```http
X-API-Deprecated: true
Deprecation: version="legacy"
Sunset: Tue, 15 Apr 2025 00:00:00 GMT
```

---

## Future Versions

### Creating v2

When v2 is needed:

1. **Create new router**:
```javascript
const apiV2 = express.Router();

// New/changed endpoints
apiV2.post('/auth/login', newAuthHandler);
apiV2.get('/plaid/accounts', newAccountsHandler);

// Mount v2
app.use('/api/v2', apiV2);
```

2. **Update health check**:
```javascript
res.json({
  apiVersion: 'v2',
  supportedVersions: ['v1', 'v2']
});
```

3. **Keep v1 running** (parallel versions)

4. **Deprecate v1** after v2 is stable

### Version Migration Checklist

When creating a new version:

- [ ] Create new router (`apiV2`)
- [ ] Implement changed endpoints
- [ ] Update health check
- [ ] Update documentation
- [ ] Test both versions in parallel
- [ ] Monitor usage metrics
- [ ] Announce new version to users
- [ ] Set deprecation timeline for old version
- [ ] Migrate clients gradually
- [ ] Remove old version after sunset period

---

## Breaking Changes

### What Qualifies as Breaking?

Changes that require a new version:

✅ **Breaking** (needs new version):
- Changing endpoint URLs
- Removing fields from responses
- Changing field types (string → number)
- Changing authentication mechanism
- Removing endpoints
- Changing error response format

❌ **Non-Breaking** (can stay in same version):
- Adding new endpoints
- Adding optional fields to requests
- Adding new fields to responses
- Improving performance
- Bug fixes
- Better error messages

### Example: v1 → v2 Migration

**v1 Response**:
```json
{
  "cards": [
    {
      "id": "123",
      "name": "Chase Freedom",
      "balance": "1500.00"
    }
  ]
}
```

**v2 Response** (breaking change):
```json
{
  "data": [
    {
      "id": "123",
      "cardName": "Chase Freedom",
      "currentBalance": {
        "amount": 1500.00,
        "currency": "USD"
      }
    }
  ],
  "meta": {
    "total": 1,
    "updated": "2025-01-15T10:30:00Z"
  }
}
```

Changes:
- Different root structure (`cards` → `data`)
- Renamed field (`name` → `cardName`)
- Changed type (`balance` string → object)
- Added metadata

**Solution**: Keep both versions running in parallel!

---

## Monitoring

### Metrics to Track

1. **Version Usage**:
```javascript
// Log version usage
logger.info({
  version: req.baseUrl.includes('v2') ? 'v2' : 'v1',
  endpoint: req.path,
  userId: req.userId
});
```

2. **Deprecation Warnings**:
```javascript
// Count deprecated API usage
metrics.increment('api.deprecated.calls', {
  endpoint: req.path,
  userId: req.userId
});
```

3. **Version Adoption Rate**:
```sql
SELECT 
  api_version,
  COUNT(DISTINCT user_id) as users,
  COUNT(*) as requests
FROM api_logs
GROUP BY api_version;
```

### Alerts

Set up alerts for:
- Deprecated endpoint usage > 10% after 60 days
- New version error rate > 1%
- Version adoption < 50% after 30 days

---

## Client Implementation

### Android Version Handling

**Automatic version detection**:
```kotlin
class ApiVersionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Check deprecation headers
        if (response.header("X-API-Deprecated") == "true") {
            val sunset = response.header("Sunset")
            Log.w("API", "Deprecated endpoint: ${request.url}, Sunset: $sunset")
            
            // Notify user or app update
            notifyAppUpdateAvailable()
        }
        
        return response
    }
}
```

**Version-specific error handling**:
```kotlin
fun handleVersionError(error: ApiError) {
    when (error.code) {
        410 -> {
            // Gone - version no longer supported
            showForceUpdateDialog()
        }
        426 -> {
            // Upgrade Required
            showUpdateRecommendation()
        }
    }
}
```

---

## Testing

### Test Matrix

| Test | v1 | v2 | Legacy |
|------|----|----|--------|
| All endpoints work | ✅ | ✅ | ✅ |
| Response format correct | ✅ | ✅ | ✅ |
| Headers set correctly | ✅ | ✅ | ✅ |
| Deprecation warnings | N/A | N/A | ✅ |

### Automated Tests

```javascript
describe('API Versioning', () => {
  it('should serve v1 endpoints correctly', async () => {
    const res = await request(app).get('/api/v1/plaid/accounts');
    expect(res.status).toBe(200);
    expect(res.headers['x-api-version']).toBe('v1');
  });
  
  it('should redirect legacy endpoints to v1', async () => {
    const res = await request(app).get('/api/plaid/accounts');
    expect(res.status).toBe(200);
    expect(res.headers['x-api-deprecated']).toBe('true');
    expect(res.headers['x-api-version']).toBe('v1');
  });
  
  it('should include sunset header for deprecated endpoints', async () => {
    const res = await request(app).get('/api/plaid/accounts');
    expect(res.headers['sunset']).toBeDefined();
  });
});
```

---

## Documentation

### API Docs Structure

```
docs/
├── api/
│   ├── v1/
│   │   ├── README.md
│   │   ├── auth.md
│   │   ├── plaid.md
│   │   └── cards.md
│   ├── v2/ (future)
│   │   └── ...
│   └── migration-guide.md
└── versioning-policy.md
```

### Version-Specific Documentation

Each version has its own documentation:
- **v1 docs**: Frozen at time of v2 release
- **v2 docs**: Latest version, actively maintained
- **Migration guide**: How to upgrade from v1 to v2

---

## Best Practices

### DO ✅

1. **Always version new APIs** from the start
2. **Keep old versions running** during migration period
3. **Communicate changes** clearly to users
4. **Provide migration guides** when creating new versions
5. **Monitor usage** of deprecated endpoints
6. **Set clear sunset dates** for old versions
7. **Test all versions** in CI/CD

### DON'T ❌

1. **Don't break existing APIs** without versioning
2. **Don't remove old versions** without warning
3. **Don't version unnecessarily** (minor changes don't need new versions)
4. **Don't forget to update** documentation
5. **Don't ignore deprecation warnings** in logs

---

## Current Status

### v1 (Current)

**Status**: ✅ **Active**  
**Stability**: Production-ready  
**Support**: Full support  
**Sunset**: TBD (at least 90 days after v2 release)

**Endpoints**:
- `/api/v1/auth/*` - Authentication
- `/api/v1/plaid/*` - Plaid integration
- `/api/v1/cards/*` - Card management

### Legacy (Deprecated)

**Status**: ⚠️ **Deprecated**  
**Support**: Backward compatibility only  
**Sunset**: 90 days from now  
**Action**: Migrate to `/api/v1/*`

---

## FAQ

### Q: Why not use query parameters (`?version=1`)?

**A**: URL-based versioning is clearer and works better with:
- Caching (different versions cached separately)
- Routing (easier to route to different handlers)
- Monitoring (version in URL path is more visible)
- RESTful principles (version is part of resource identifier)

### Q: Should we version every endpoint change?

**A**: No. Only breaking changes require new versions. Adding fields or fixing bugs can stay in the same version.

### Q: How long should we support old versions?

**A**: Minimum 90 days after new version release. Ideally 6-12 months for major versions.

### Q: What if a user refuses to upgrade?

**A**: After sunset period:
- Return `410 Gone` status
- Provide upgrade instructions in error message
- Force update in mobile app if needed

### Q: Can we run v1 and v2 in parallel?

**A**: Yes! That's the whole point. Both versions run simultaneously during migration period.

---

## Summary

### What We Built

✅ **v1 API** - All endpoints versioned  
✅ **Backward compatibility** - Legacy endpoints supported (90 days)  
✅ **Deprecation headers** - Clear communication  
✅ **Health check** - Version discovery  
✅ **Android client** - Uses v1 endpoints  
✅ **Documentation** - Complete versioning strategy

### Benefits

- 🛡️ **Backward compatible** - Old apps keep working
- 🚀 **Safe deployments** - No breaking changes
- 📊 **Trackable** - Monitor version adoption
- 🔄 **Flexible** - Easy to add v2 when needed
- 📱 **Mobile-friendly** - Gradual client updates

### Next Steps

When v2 is needed:
1. Create `apiV2` router
2. Implement changes
3. Test in parallel with v1
4. Update documentation
5. Announce to users
6. Monitor adoption
7. Deprecate v1 after 90+ days

---

*Production-ready API versioning for long-term maintainability!* 🎯

