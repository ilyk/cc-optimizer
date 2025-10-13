# Smart Caching System

Multi-layer caching for 100x faster performance and 90% API reduction.

## Overview

The app implements intelligent caching to provide:
- **100x faster** subsequent loads (from seconds to milliseconds)
- **90% reduction** in API calls
- **Offline support** with Room database
- **Smart rate limiting** that's transparent to users

## Cache Layers

```
┌─────────────────────────────────────────────────────┐
│ Layer 1: In-Memory Cache                            │
│ - Location: Repository                              │
│ - Duration: 5 minutes                               │
│ - Storage: HashMap/MutableMap                       │
│ - Speed: Instant (~1ms)                             │
└─────────────────────────────────────────────────────┘
                      ↓ Miss
┌─────────────────────────────────────────────────────┐
│ Layer 2: Room Database (Offline)                    │
│ - Location: Local SQLite                            │
│ - Duration: Until manually refreshed                │
│ - Storage: CreditCardEntity                         │
│ - Speed: Very fast (~10ms)                          │
└─────────────────────────────────────────────────────┘
                      ↓ Miss
┌─────────────────────────────────────────────────────┐
│ Layer 3: API Call                                    │
│ - Location: Backend server                           │
│ - Duration: Real-time                                │
│ - Storage: N/A                                       │
│ - Speed: Slow (~2-3s)                                │
└─────────────────────────────────────────────────────┘
```

## Implementation

### In-Memory Cache

```kotlin
class PlaidRepository {
    private val inMemoryCache = mutableMapOf<String, CachedData>()
    private val cacheDuration = 5.minutes
    
    suspend fun getAccounts(): Result<List<CreditCard>> {
        val cacheKey = "accounts_${userId}"
        
        // Check in-memory cache
        inMemoryCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) {
                return Result.success(cached.data)
            }
        }
        
        // Cache miss - fetch from API
        val accounts = fetchFromAPI()
        
        // Update cache
        inMemoryCache[cacheKey] = CachedData(
            data = accounts,
            timestamp = System.currentTimeMillis()
        )
        
        return Result.success(accounts)
    }
}

data class CachedData<T>(
    val data: T,
    val timestamp: Long
) {
    fun isExpired(duration: Duration = 5.minutes): Boolean {
        return System.currentTimeMillis() - timestamp > duration.inWholeMilliseconds
    }
}
```

### Room Database Cache

```kotlin
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val accountId: String,
    val name: String,
    val mask: String,
    val currentBalance: Double,
    val statementDay: Int?,
    val cachedAt: Long
)

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards")
    suspend fun getAll(): List<CreditCardEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CreditCardEntity>)
    
    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}

class CacheManager(private val dao: CreditCardDao) {
    suspend fun getCachedAccounts(): List<CreditCard>? {
        return dao.getAll().map { it.toDomainModel() }
    }
    
    suspend fun cacheAccounts(accounts: List<CreditCard>) {
        dao.deleteAll()
        dao.insertAll(accounts.map { it.toEntity() })
    }
}
```

### Smart Rate Limiting

When rate limited, serve cached data instead of failing:

```kotlin
suspend fun getAccounts(strategy: String = "normal"): Result<List<CreditCard>> {
    return when (strategy) {
        "cache-only" -> {
            // Rate limited - serve from cache
            val cached = cacheManager.getCachedAccounts()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(Exception("No cached data available"))
            }
        }
        "stale-while-revalidate" -> {
            // Return cached data immediately
            // Fetch fresh data in background
            val cached = cacheManager.getCachedAccounts()
            scope.launch { fetchAndUpdateCache() }
            Result.success(cached ?: emptyList())
        }
        else -> {
            // Normal flow with caching
            fetchWithCache()
        }
    }
}
```

## Cache Strategies

### 1. Cache-First (Default)
```
Request → Check Cache → Return if fresh → API if expired
```
- **Use case**: Normal app usage
- **Speed**: Fast (cache) or slow (API)
- **Freshness**: 5 minutes

### 2. Cache-Only (Rate Limited)
```
Request → Return Cache (even if stale) → Never call API
```
- **Use case**: Rate limited
- **Speed**: Always fast
- **Freshness**: May be stale

### 3. Stale-While-Revalidate
```
Request → Return Cache immediately → Fetch in background
```
- **Use case**: Pull-to-refresh
- **Speed**: Always fast
- **Freshness**: Updates in background

### 4. Network-First (Force Refresh)
```
Request → API Call → Update Cache → Return fresh data
```
- **Use case**: User-initiated refresh
- **Speed**: Slow
- **Freshness**: Always fresh

## Cache Invalidation

**Time-based**:
```kotlin
// Auto-expire after 5 minutes
if (cached.timestamp + 5.minutes < now()) {
    invalidate()
}
```

**Event-based**:
```kotlin
// Invalidate when user connects new account
fun onPlaidSuccess() {
    inMemoryCache.clear()
    fetchFreshData()
}
```

**Manual**:
```kotlin
// Pull-to-refresh
fun onRefresh() {
    inMemoryCache.clear()
    fetchFreshData(forceRefresh = true)
}
```

## Performance Gains

| Scenario | Without Cache | With Cache | Improvement |
|----------|--------------|------------|-------------|
| **First Load** | ~3s | ~3s | 0% (needs API) |
| **Second Load** | ~3s | ~30ms | **100x faster** |
| **Offline** | ❌ Fails | ✅ Works | Infinite |
| **Rate Limited** | ❌ Error | ✅ Success | Infinite |

## API Reduction

**Before caching**:
- Every screen load = API call
- 10 screen loads/session = 10 API calls
- 1000 users = 10,000 API calls/day

**After caching**:
- First load = API call
- Next 9 loads = Cache hits
- 1000 users = 1,000 API calls/day
- **90% reduction** 🎉

## Cache Headers

Backend sends cache metadata:

```http
X-Cache-Strategy: cache-only
X-Rate-Limit-Info: Using cached data due to rate limit
Cache-Control: max-age=300
```

App uses these to inform cache behavior.

## Offline Support

Full offline functionality:
- ✅ View cached accounts
- ✅ View payment recommendations
- ✅ View statement dates
- ❌ Cannot fetch new data
- ❌ Cannot connect Plaid

```kotlin
fun getAccounts(): Result<List<CreditCard>> {
    try {
        // Try API
        return api.getAccounts()
    } catch (e: IOException) {
        // Offline - use cache
        val cached = cacheManager.getCachedAccounts()
        return if (cached != null) {
            Result.success(cached)
        } else {
            Result.failure(Exception("No internet and no cached data"))
        }
    }
}
```

## Cache Size Management

```kotlin
class CacheManager {
    private val maxCacheSize = 10.MB
    
    fun checkCacheSize() {
        val size = database.getDatabaseSize()
        if (size > maxCacheSize) {
            // Delete oldest entries
            dao.deleteOldest(limit = 100)
        }
    }
}
```

## Best Practices

1. **Cache aggressively** - 5 min is good for financial data
2. **Invalidate appropriately** - Clear on user actions
3. **Handle offline** - Always have fallback
4. **Monitor size** - Prevent database bloat
5. **Test thoroughly** - Cache bugs are subtle

## Monitoring

Track cache performance:

```kotlin
class CacheMetrics {
    var hits = 0
    var misses = 0
    
    val hitRate: Float
        get() = hits.toFloat() / (hits + misses)
}

// Log metrics
logger.info("Cache hit rate: ${metrics.hitRate * 100}%")
```

---

**See Also**:
- [Smart Rate Limiting](../features/smart-rate-limiting.md)
- [Performance Optimization](../development/optimization.md)
- [Offline Support](../features/offline.md)

