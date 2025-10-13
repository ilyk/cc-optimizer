/**
 * Monitoring, Logging, and Analytics Configuration
 * All configurable via environment variables - NO HARDCODING
 */

const config = {
  // ============================================================================
  // LOGGING CONFIGURATION
  // ============================================================================
  logging: {
    level: process.env.LOG_LEVEL || 'info', // trace, debug, info, warn, error
    format: process.env.LOG_FORMAT || 'json', // json, pretty, simple
    destination: process.env.LOG_DESTINATION || 'stdout', // stdout, file, both
    file: {
      enabled: process.env.LOG_FILE_ENABLED === 'true',
      path: process.env.LOG_FILE_PATH || './logs/app.log',
      maxSize: process.env.LOG_FILE_MAX_SIZE || '10M',
      maxFiles: parseInt(process.env.LOG_FILE_MAX_FILES || '7'),
    },
    // Sensitive fields to redact from logs
    redactFields: (process.env.LOG_REDACT_FIELDS || 'password,token,secret,key,authorization').split(','),
    // Enable request/response logging
    http: {
      enabled: process.env.LOG_HTTP_ENABLED !== 'false',
      logBody: process.env.LOG_HTTP_BODY === 'true',
      logHeaders: process.env.LOG_HTTP_HEADERS === 'true',
    }
  },

  // ============================================================================
  // ERROR TRACKING (Sentry, Rollbar, etc.)
  // ============================================================================
  errorTracking: {
    enabled: process.env.ERROR_TRACKING_ENABLED === 'true',
    provider: process.env.ERROR_TRACKING_PROVIDER || 'sentry', // sentry, rollbar, bugsnag
    dsn: process.env.ERROR_TRACKING_DSN || '',
    environment: process.env.NODE_ENV || 'development',
    release: process.env.APP_VERSION || 'unknown',
    sampleRate: parseFloat(process.env.ERROR_TRACKING_SAMPLE_RATE || '1.0'),
    tracesSampleRate: parseFloat(process.env.ERROR_TRACKING_TRACES_SAMPLE_RATE || '0.1'),
    // Sentry specific
    sentry: {
      dsn: process.env.SENTRY_DSN || '',
      serverName: process.env.SENTRY_SERVER_NAME || require('os').hostname(),
      attachStacktrace: process.env.SENTRY_ATTACH_STACKTRACE !== 'false',
    },
    // Rollbar specific
    rollbar: {
      accessToken: process.env.ROLLBAR_ACCESS_TOKEN || '',
      captureUncaught: process.env.ROLLBAR_CAPTURE_UNCAUGHT !== 'false',
      captureUnhandledRejections: process.env.ROLLBAR_CAPTURE_UNHANDLED !== 'false',
    }
  },

  // ============================================================================
  // ANALYTICS (Mixpanel, Amplitude, Segment, etc.)
  // ============================================================================
  analytics: {
    enabled: process.env.ANALYTICS_ENABLED === 'true',
    provider: process.env.ANALYTICS_PROVIDER || 'mixpanel', // mixpanel, amplitude, segment
    // Mixpanel
    mixpanel: {
      token: process.env.MIXPANEL_TOKEN || '',
      secret: process.env.MIXPANEL_SECRET || '',
    },
    // Amplitude
    amplitude: {
      apiKey: process.env.AMPLITUDE_API_KEY || '',
    },
    // Segment
    segment: {
      writeKey: process.env.SEGMENT_WRITE_KEY || '',
    },
    // Event batching
    batch: {
      enabled: process.env.ANALYTICS_BATCH_ENABLED !== 'false',
      maxSize: parseInt(process.env.ANALYTICS_BATCH_SIZE || '50'),
      flushInterval: parseInt(process.env.ANALYTICS_BATCH_FLUSH_MS || '10000'),
    },
    // PII handling
    anonymize: process.env.ANALYTICS_ANONYMIZE === 'true',
    excludeFields: (process.env.ANALYTICS_EXCLUDE_FIELDS || 'email,phone,ssn').split(','),
  },

  // ============================================================================
  // METRICS (Prometheus, StatsD, CloudWatch, etc.)
  // ============================================================================
  metrics: {
    enabled: process.env.METRICS_ENABLED === 'true',
    provider: process.env.METRICS_PROVIDER || 'prometheus', // prometheus, statsd, cloudwatch
    // Prometheus
    prometheus: {
      enabled: process.env.PROMETHEUS_ENABLED === 'true',
      port: parseInt(process.env.PROMETHEUS_PORT || '9090'),
      path: process.env.PROMETHEUS_PATH || '/metrics',
      defaultLabels: {
        app: process.env.APP_NAME || 'cc-optimizer',
        env: process.env.NODE_ENV || 'development',
      }
    },
    // StatsD
    statsd: {
      enabled: process.env.STATSD_ENABLED === 'true',
      host: process.env.STATSD_HOST || 'localhost',
      port: parseInt(process.env.STATSD_PORT || '8125'),
      prefix: process.env.STATSD_PREFIX || 'ccoptimizer.',
    },
    // CloudWatch
    cloudwatch: {
      enabled: process.env.CLOUDWATCH_ENABLED === 'true',
      namespace: process.env.CLOUDWATCH_NAMESPACE || 'CCOptimizer',
      region: process.env.AWS_REGION || 'us-east-1',
    },
    // Custom metrics
    collectDefaultMetrics: process.env.METRICS_COLLECT_DEFAULT !== 'false',
    collectInterval: parseInt(process.env.METRICS_COLLECT_INTERVAL_MS || '10000'),
  },

  // ============================================================================
  // PERFORMANCE MONITORING (APM)
  // ============================================================================
  apm: {
    enabled: process.env.APM_ENABLED === 'true',
    provider: process.env.APM_PROVIDER || 'none', // newrelic, datadog, elastic
    // New Relic
    newrelic: {
      appName: process.env.NEW_RELIC_APP_NAME || 'CC Optimizer',
      licenseKey: process.env.NEW_RELIC_LICENSE_KEY || '',
    },
    // Datadog
    datadog: {
      apiKey: process.env.DATADOG_API_KEY || '',
      service: process.env.DATADOG_SERVICE || 'cc-optimizer-api',
      env: process.env.NODE_ENV || 'development',
    }
  },

  // ============================================================================
  // AUDIT LOGGING
  // ============================================================================
  audit: {
    enabled: process.env.AUDIT_LOG_ENABLED === 'true',
    logAuthEvents: process.env.AUDIT_LOG_AUTH !== 'false',
    logDataChanges: process.env.AUDIT_LOG_DATA_CHANGES !== 'false',
    logAccessEvents: process.env.AUDIT_LOG_ACCESS === 'true',
    destination: process.env.AUDIT_LOG_DESTINATION || 'database', // database, file, both
    retention: parseInt(process.env.AUDIT_LOG_RETENTION_DAYS || '90'),
  },

  // ============================================================================
  // HEALTH CHECKS
  // ============================================================================
  health: {
    enabled: process.env.HEALTH_CHECKS_ENABLED !== 'false',
    interval: parseInt(process.env.HEALTH_CHECK_INTERVAL_MS || '30000'),
    checks: {
      database: process.env.HEALTH_CHECK_DB !== 'false',
      cache: process.env.HEALTH_CHECK_CACHE === 'true',
      plaid: process.env.HEALTH_CHECK_PLAID === 'true',
      llm: process.env.HEALTH_CHECK_LLM === 'true',
    }
  }
};

// Validation
function validateConfig() {
  const errors = [];

  // Error tracking validation
  if (config.errorTracking.enabled && !config.errorTracking.dsn) {
    errors.push('ERROR_TRACKING_DSN is required when error tracking is enabled');
  }

  // Analytics validation
  if (config.analytics.enabled) {
    const provider = config.analytics.provider;
    if (provider === 'mixpanel' && !config.analytics.mixpanel.token) {
      errors.push('MIXPANEL_TOKEN is required when using Mixpanel');
    }
    if (provider === 'amplitude' && !config.analytics.amplitude.apiKey) {
      errors.push('AMPLITUDE_API_KEY is required when using Amplitude');
    }
    if (provider === 'segment' && !config.analytics.segment.writeKey) {
      errors.push('SEGMENT_WRITE_KEY is required when using Segment');
    }
  }

  // APM validation
  if (config.apm.enabled) {
    const provider = config.apm.provider;
    if (provider === 'newrelic' && !config.apm.newrelic.licenseKey) {
      errors.push('NEW_RELIC_LICENSE_KEY is required when using New Relic');
    }
    if (provider === 'datadog' && !config.apm.datadog.apiKey) {
      errors.push('DATADOG_API_KEY is required when using Datadog');
    }
  }

  return errors;
}

// Validate on load
const validationErrors = validateConfig();
if (validationErrors.length > 0 && process.env.NODE_ENV === 'production') {
  console.error('Monitoring configuration errors:');
  validationErrors.forEach(error => console.error(`  - ${error}`));
  console.error('Some monitoring features may not work correctly.');
}

module.exports = config;

