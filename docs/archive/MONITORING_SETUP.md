# Monitoring, Logging, and Analytics Setup Guide 📊

## Overview

Comprehensive monitoring system with **ZERO hardcoding** - everything configurable via environment variables!

**Supported Providers**:
- 🔍 **Error Tracking**: Sentry, Rollbar, Bugsnag
- 📊 **Analytics**: Mixpanel, Amplitude, Segment, Firebase
- 📈 **Metrics**: Prometheus, StatsD, CloudWatch
- 🔬 **APM**: New Relic, Datadog, Elastic APM
- 📝 **Logging**: Winston (file/stdout/both)

---

## Quick Start

### 1. Configure Environment Variables

Copy and edit `.env`:
```bash
cp backend/.env.example backend/.env
# Edit .env with your credentials
```

### 2. Enable Services

```bash
# Enable error tracking
ERROR_TRACKING_ENABLED=true
ERROR_TRACKING_PROVIDER=sentry
SENTRY_DSN=https://your-dsn@sentry.io/project

# Enable analytics
ANALYTICS_ENABLED=true
ANALYTICS_PROVIDER=mixpanel
MIXPANEL_TOKEN=your_mixpanel_token

# Enable metrics
METRICS_ENABLED=true
METRICS_PROVIDER=prometheus
PROMETHEUS_PORT=9090
```

### 3. Start Backend

```bash
cd backend
npm install
npm start
```

That's it! No code changes needed. 🎉

---

## Configuration Reference

### Logging Configuration

```bash
# Log level: trace, debug, info, warn, error
LOG_LEVEL=info

# Format: json, pretty, simple
LOG_FORMAT=json

# Destination: stdout, file, both
LOG_DESTINATION=stdout

# File logging
LOG_FILE_ENABLED=true
LOG_FILE_PATH=./logs/app.log
LOG_FILE_MAX_SIZE=10M
LOG_FILE_MAX_FILES=7

# Redact sensitive fields
LOG_REDACT_FIELDS=password,token,secret,key

# HTTP logging
LOG_HTTP_ENABLED=true
LOG_HTTP_BODY=false
LOG_HTTP_HEADERS=false
```

**Example Output (JSON)**:
```json
{
  "level": "info",
  "time": "2025-01-15T10:30:00.000Z",
  "pid": 12345,
  "hostname": "server-1",
  "msg": "API request completed",
  "method": "GET",
  "url": "/api/v1/plaid/accounts",
  "status": 200,
  "duration": 123
}
```

---

## Error Tracking

### Option 1: Sentry (Recommended)

```bash
ERROR_TRACKING_ENABLED=true
ERROR_TRACKING_PROVIDER=sentry
SENTRY_DSN=https://xxx@sentry.io/123
ERROR_TRACKING_SAMPLE_RATE=1.0
ERROR_TRACKING_TRACES_SAMPLE_RATE=0.1
```

**Features**:
- Automatic error capture
- Stack traces with source maps
- Breadcrumbs (user actions before error)
- Performance monitoring
- Release tracking

### Option 2: Rollbar

```bash
ERROR_TRACKING_ENABLED=true
ERROR_TRACKING_PROVIDER=rollbar
ROLLBAR_ACCESS_TOKEN=your_token
ROLLBAR_CAPTURE_UNCAUGHT=true
```

### Option 3: Bugsnag

```bash
ERROR_TRACKING_ENABLED=true
ERROR_TRACKING_PROVIDER=bugsnag
BUGSNAG_API_KEY=your_api_key
```

### Usage in Code

```javascript
const errorTracking = require('./services/errorTracking');

// Automatic (via middleware)
app.use(errorTracking.getMiddleware());
app.use(errorTracking.getErrorHandler());

// Manual
try {
  // risky operation
} catch (error) {
  errorTracking.captureException(error, {
    userId: req.userId,
    endpoint: req.path
  });
}

// Set user context
errorTracking.setUser(userId, {
  email: user.email,
  plan: 'premium'
});
```

---

## Analytics

### Option 1: Mixpanel

```bash
ANALYTICS_ENABLED=true
ANALYTICS_PROVIDER=mixpanel
MIXPANEL_TOKEN=your_project_token
MIXPANEL_SECRET=your_api_secret
```

### Option 2: Amplitude

```bash
ANALYTICS_ENABLED=true
ANALYTICS_PROVIDER=amplitude
AMPLITUDE_API_KEY=your_api_key
```

### Option 3: Segment

```bash
ANALYTICS_ENABLED=true
ANALYTICS_PROVIDER=segment
SEGMENT_WRITE_KEY=your_write_key
```

### Usage

```javascript
const analytics = require('./services/analytics');

// Track event
analytics.track(userId, analytics.events.USER_LOGGED_IN, {
  method: 'email',
  source: 'mobile'
});

// Identify user
analytics.identify(userId, {
  email: user.email,
  plan: 'free',
  createdAt: user.createdAt
});

// Batch configuration
ANALYTICS_BATCH_ENABLED=true
ANALYTICS_BATCH_SIZE=50
ANALYTICS_BATCH_FLUSH_MS=10000
```

### PII Protection

```bash
# Anonymize data
ANALYTICS_ANONYMIZE=true

# Exclude sensitive fields
ANALYTICS_EXCLUDE_FIELDS=email,phone,ssn,address
```

---

## Metrics

### Option 1: Prometheus (Recommended)

```bash
METRICS_ENABLED=true
METRICS_PROVIDER=prometheus
PROMETHEUS_ENABLED=true
PROMETHEUS_PORT=9090
PROMETHEUS_PATH=/metrics
```

**Access metrics**: `http://localhost:9090/metrics`

**Example metrics**:
```
# HELP http_requests_total Total HTTP requests
# TYPE http_requests_total counter
http_requests_total{method="GET",route="/api/v1/plaid/accounts",status="200"} 1234

# HELP http_request_duration_ms HTTP request duration in milliseconds
# TYPE http_request_duration_ms histogram
http_request_duration_ms_bucket{le="100"} 850
http_request_duration_ms_bucket{le="500"} 980
http_request_duration_ms_sum 123456
http_request_duration_ms_count 1000
```

### Option 2: StatsD

```bash
METRICS_ENABLED=true
METRICS_PROVIDER=statsd
STATSD_HOST=localhost
STATSD_PORT=8125
STATSD_PREFIX=ccoptimizer.
```

### Option 3: CloudWatch

```bash
METRICS_ENABLED=true
METRICS_PROVIDER=cloudwatch
CLOUDWATCH_NAMESPACE=CCOptimizer
AWS_REGION=us-east-1
```

### Usage

```javascript
const metrics = require('./services/metrics');

// Counter
metrics.incrementCounter('api.requests', { 
  endpoint: '/plaid/accounts' 
});

// Gauge
metrics.setGauge('active.users', 42);

// Histogram (timing)
const start = Date.now();
// ... operation ...
metrics.recordHistogram('operation.duration', Date.now() - start);

// Automatic HTTP metrics (via middleware)
app.use(metrics.middleware());
```

---

## APM (Application Performance Monitoring)

### Option 1: New Relic

```bash
APM_ENABLED=true
APM_PROVIDER=newrelic
NEW_RELIC_APP_NAME=CC Optimizer
NEW_RELIC_LICENSE_KEY=your_license_key
```

### Option 2: Datadog

```bash
APM_ENABLED=true
APM_PROVIDER=datadog
DATADOG_API_KEY=your_api_key
DATADOG_SERVICE=cc-optimizer-api
```

---

## Audit Logging

```bash
AUDIT_LOG_ENABLED=true
AUDIT_LOG_AUTH=true              # Log auth events
AUDIT_LOG_DATA_CHANGES=true      # Log data changes
AUDIT_LOG_ACCESS=false           # Log data access
AUDIT_LOG_DESTINATION=database   # database, file, both
AUDIT_LOG_RETENTION_DAYS=90
```

**What gets logged**:
- User login/logout
- Credit card additions/deletions
- Statement date overrides
- Plaid item connections/disconnections

---

## Health Checks

```bash
HEALTH_CHECKS_ENABLED=true
HEALTH_CHECK_INTERVAL_MS=30000
HEALTH_CHECK_DB=true
HEALTH_CHECK_CACHE=false
HEALTH_CHECK_PLAID=false
HEALTH_CHECK_LLM=false
```

**Access**: `GET /health`

**Response**:
```json
{
  "status": "ok",
  "timestamp": "2025-01-15T10:30:00Z",
  "checks": {
    "database": "ok",
    "plaid": "ok",
    "llm": "degraded"
  },
  "apiVersion": "v1",
  "uptime": 86400
}
```

---

## Android Configuration

Add to `app/build.gradle.kts`:

```kotlin
defaultConfig {
    // Analytics
    buildConfigField("Boolean", "ANALYTICS_ENABLED", "false")
    buildConfigField("String", "ANALYTICS_PROVIDER", "\"mixpanel\"")
    buildConfigField("String", "ANALYTICS_KEY", "\"\"")
    
    // Crash Reporting
    buildConfigField("Boolean", "CRASH_REPORTING_ENABLED", "false")
    buildConfigField("String", "CRASH_REPORTING_PROVIDER", "\"sentry\"")
    buildConfigField("String", "SENTRY_DSN", "\"\"")
}

buildTypes {
    release {
        // Enable for production
        buildConfigField("Boolean", "ANALYTICS_ENABLED", "true")
        buildConfigField("String", "ANALYTICS_KEY", "\"${project.findProperty("MIXPANEL_TOKEN")}\"")
        
        buildConfigField("Boolean", "CRASH_REPORTING_ENABLED", "true")
        buildConfigField("String", "SENTRY_DSN", "\"${project.findProperty("SENTRY_DSN")}\"")
    }
}
```

Add to `local.properties`:
```properties
MIXPANEL_TOKEN=your_token
SENTRY_DSN=https://your-dsn@sentry.io/project
```

### Usage in Android

```kotlin
// Analytics
val analytics = AnalyticsManager.getInstance(context)
analytics.track(AnalyticsManager.Events.USER_LOGGED_IN, mapOf(
    "method" to "email"
))

// Crash Reporting
val crashReporting = CrashReporting.getInstance(context)
crashReporting.setUser(userId, email)
crashReporting.logException(exception, mapOf(
    "screen" to "MainScreen"
))
```

---

## Production Deployment Checklist

### Backend

- [ ] Set `NODE_ENV=production`
- [ ] Configure error tracking (Sentry/Rollbar)
- [ ] Configure analytics (Mixpanel/Amplitude)
- [ ] Enable metrics (Prometheus)
- [ ] Set appropriate log level (`info` or `warn`)
- [ ] Enable file logging
- [ ] Configure log rotation
- [ ] Set audit log retention
- [ ] Configure health checks

### Android

- [ ] Enable crash reporting
- [ ] Enable analytics
- [ ] Set production API keys
- [ ] Test crash reporting
- [ ] Test analytics events
- [ ] Verify no sensitive data in logs

---

## Monitoring Dashboards

### Prometheus + Grafana

1. **Install Grafana**:
```bash
docker run -d -p 3000:3000 grafana/grafana
```

2. **Add Prometheus data source**: `http://localhost:9090`

3. **Import dashboard**: Use Grafana ID `1860` (Node Exporter Full)

### Sentry Dashboard

- Navigate to Sentry dashboard
- View errors by:
  - Environment (dev/staging/prod)
  - Release version
  - User
  - Endpoint

### Mixpanel Dashboard

- Track user journeys
- Funnel analysis (signup → link → cards viewed)
- Retention analysis
- Cohort analysis

---

## Cost Estimates

### Free Tiers

| Service | Free Tier | Typical Cost (1000 users) |
|---------|-----------|---------------------------|
| **Sentry** | 5K errors/month | $26/month (10K errors) |
| **Mixpanel** | 100K events/month | $0 (likely within free tier) |
| **Amplitude** | 10M events/month | $0 (within free tier) |
| **Prometheus** | Self-hosted | ~$10/month (server) |
| **Firebase** | Free (Analytics + Crashlytics) | $0 |

**Recommended Free Stack**:
- Sentry (error tracking)
- Amplitude (analytics)
- Prometheus (metrics - self-hosted)
- **Total**: $0-26/month

---

## Troubleshooting

### Issue: Metrics not appearing

**Check**:
```bash
curl http://localhost:9090/metrics
```

**Solution**: Ensure `METRICS_ENABLED=true` and provider is configured

### Issue: Analytics events not sending

**Check**:
```javascript
console.log('Analytics enabled:', config.analytics.enabled);
console.log('Provider:', config.analytics.provider);
```

**Solution**: Verify API keys and provider configuration

### Issue: Errors not captured

**Check**:
```javascript
errorTracking.captureMessage('Test message');
```

**Solution**: Verify DSN is correct and enabled in production

---

## Best Practices

### DO ✅

1. **Use environment variables** for all credentials
2. **Enable sampling** in production (traces: 10%, errors: 100%)
3. **Redact sensitive data** from logs
4. **Set up alerts** for critical errors
5. **Monitor costs** (set billing alerts)
6. **Test in staging** before production
7. **Document custom events**

### DON'T ❌

1. **Don't hardcode** credentials
2. **Don't log passwords** or tokens
3. **Don't enable verbose logging** in production
4. **Don't ignore errors** in monitoring dashboards
5. **Don't send PII** to analytics without consent
6. **Don't enable everything** at once (start with error tracking)

---

## Summary

### What We Built

✅ **Configurable Logging** - Winston with file rotation  
✅ **Error Tracking** - Sentry/Rollbar/Bugsnag support  
✅ **Analytics** - Mixpanel/Amplitude/Segment support  
✅ **Metrics** - Prometheus/StatsD/CloudWatch support  
✅ **APM** - New Relic/Datadog support  
✅ **Audit Logging** - Compliance-ready  
✅ **Health Checks** - System monitoring  
✅ **Android Integration** - Crash reporting & analytics  

### Configuration Files

- `backend/src/config/monitoring.js` - Central configuration
- `backend/.env.example` - All configuration options
- `backend/src/services/errorTracking.js` - Error tracking service
- `backend/src/services/analytics.js` - Analytics service
- `backend/src/services/metrics.js` - Metrics service
- `app/.../monitoring/AnalyticsManager.kt` - Android analytics
- `app/.../monitoring/CrashReporting.kt` - Android crash reporting

### Zero Hardcoding ✅

Every service is:
- 🔧 Configurable via environment variables
- 🔌 Pluggable (swap providers easily)
- 🎚️ Adjustable (sampling rates, batch sizes, etc.)
- 🔒 Secure (credentials in env, not code)
- 📊 Observable (logs, metrics, traces)

---

*Production-ready monitoring with complete configurability!* 📊✨

