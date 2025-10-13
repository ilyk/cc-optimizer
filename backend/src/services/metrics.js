/**
 * Configurable Metrics Service
 * Supports multiple providers: Prometheus, StatsD, CloudWatch
 */

const config = require('../config/monitoring');
const logger = require('../utils/logger');

class MetricsService {
  constructor() {
    this.client = null;
    this.provider = config.metrics.provider;
    this.enabled = config.metrics.enabled;
    this.counters = new Map();
    this.gauges = new Map();
    this.histograms = new Map();
    
    if (this.enabled) {
      this.initialize();
    }
  }

  initialize() {
    try {
      switch (this.provider) {
        case 'prometheus':
          this.initializePrometheus();
          break;
        case 'statsd':
          this.initializeStatsD();
          break;
        case 'cloudwatch':
          this.initializeCloudWatch();
          break;
        default:
          logger.warn(`Unknown metrics provider: ${this.provider}`);
      }
    } catch (error) {
      logger.error('Failed to initialize metrics:', error);
    }
  }

  initializePrometheus() {
    const client = require('prom-client');
    
    // Create a Registry
    this.client = new client.Registry();
    
    // Add default labels
    this.client.setDefaultLabels(config.metrics.prometheus.defaultLabels);
    
    // Collect default metrics if enabled
    if (config.metrics.collectDefaultMetrics) {
      client.collectDefaultMetrics({
        register: this.client,
        timeout: config.metrics.collectInterval,
      });
    }
    
    // Store prometheus client for creating custom metrics
    this.prometheusClient = client;
    
    logger.info('Prometheus metrics initialized');
  }

  initializeStatsD() {
    const StatsD = require('node-statsd');
    
    this.client = new StatsD({
      host: config.metrics.statsd.host,
      port: config.metrics.statsd.port,
      prefix: config.metrics.statsd.prefix,
    });
    
    logger.info('StatsD metrics initialized');
  }

  initializeCloudWatch() {
    const AWS = require('aws-sdk');
    
    this.client = new AWS.CloudWatch({
      region: config.metrics.cloudwatch.region,
    });
    
    this.namespace = config.metrics.cloudwatch.namespace;
    
    logger.info('CloudWatch metrics initialized');
  }

  /**
   * Increment a counter
   */
  incrementCounter(name, labels = {}, value = 1) {
    if (!this.enabled) {return;}

    try {
      switch (this.provider) {
        case 'prometheus':
          this.getOrCreateCounter(name, labels).inc(value);
          break;
        case 'statsd':
          this.client.increment(name, value);
          break;
        case 'cloudwatch':
          this.putCloudWatchMetric(name, value, 'Count');
          break;
      }
    } catch (error) {
      logger.error('Failed to increment counter:', error);
    }
  }

  /**
   * Set a gauge value
   */
  setGauge(name, value, labels = {}) {
    if (!this.enabled) {return;}

    try {
      switch (this.provider) {
        case 'prometheus':
          this.getOrCreateGauge(name, labels).set(value);
          break;
        case 'statsd':
          this.client.gauge(name, value);
          break;
        case 'cloudwatch':
          this.putCloudWatchMetric(name, value, 'None');
          break;
      }
    } catch (error) {
      logger.error('Failed to set gauge:', error);
    }
  }

  /**
   * Record histogram value
   */
  recordHistogram(name, value, labels = {}) {
    if (!this.enabled) {return;}

    try {
      switch (this.provider) {
        case 'prometheus':
          this.getOrCreateHistogram(name, labels).observe(value);
          break;
        case 'statsd':
          this.client.timing(name, value);
          break;
        case 'cloudwatch':
          this.putCloudWatchMetric(name, value, 'Milliseconds');
          break;
      }
    } catch (error) {
      logger.error('Failed to record histogram:', error);
    }
  }

  /**
   * Get or create Prometheus counter
   */
  getOrCreateCounter(name, labels) {
    const key = `${name}_${JSON.stringify(labels)}`;
    
    if (!this.counters.has(key)) {
      const counter = new this.prometheusClient.Counter({
        name,
        help: `Counter for ${name}`,
        labelNames: Object.keys(labels),
        registers: [this.client],
      });
      this.counters.set(key, counter);
    }
    
    return this.counters.get(key).labels(labels);
  }

  /**
   * Get or create Prometheus gauge
   */
  getOrCreateGauge(name, labels) {
    const key = `${name}_${JSON.stringify(labels)}`;
    
    if (!this.gauges.has(key)) {
      const gauge = new this.prometheusClient.Gauge({
        name,
        help: `Gauge for ${name}`,
        labelNames: Object.keys(labels),
        registers: [this.client],
      });
      this.gauges.set(key, gauge);
    }
    
    return this.gauges.get(key).labels(labels);
  }

  /**
   * Get or create Prometheus histogram
   */
  getOrCreateHistogram(name, labels) {
    const key = `${name}_${JSON.stringify(labels)}`;
    
    if (!this.histograms.has(key)) {
      const histogram = new this.prometheusClient.Histogram({
        name,
        help: `Histogram for ${name}`,
        labelNames: Object.keys(labels),
        registers: [this.client],
      });
      this.histograms.set(key, histogram);
    }
    
    return this.histograms.get(key).labels(labels);
  }

  /**
   * Put CloudWatch metric
   */
  putCloudWatchMetric(name, value, unit) {
    const params = {
      Namespace: this.namespace,
      MetricData: [{
        MetricName: name,
        Value: value,
        Unit: unit,
        Timestamp: new Date(),
      }]
    };
    
    this.client.putMetricData(params, (err) => {
      if (err) {logger.error('CloudWatch metric error:', err);}
    });
  }

  /**
   * Get metrics (for Prometheus endpoint)
   */
  async getMetrics() {
    if (this.provider === 'prometheus' && this.client) {
      return this.client.metrics();
    }
    return '';
  }

  /**
   * Middleware to track HTTP metrics
   */
  middleware() {
    return (req, res, next) => {
      if (!this.enabled) {return next();}

      const start = Date.now();

      // Track request
      this.incrementCounter('http_requests_total', {
        method: req.method,
        route: req.route?.path || req.path,
      });

      // Track response
      res.on('finish', () => {
        const duration = Date.now() - start;
        
        this.recordHistogram('http_request_duration_ms', duration, {
          method: req.method,
          route: req.route?.path || req.path,
          status: res.statusCode,
        });
        
        this.incrementCounter('http_responses_total', {
          method: req.method,
          route: req.route?.path || req.path,
          status: res.statusCode,
        });
      });

      next();
    };
  }
}

module.exports = new MetricsService();

