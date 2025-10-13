/**
 * Configurable Error Tracking Service
 * Supports multiple providers: Sentry, Rollbar, Bugsnag
 */

const config = require('../config/monitoring');
const logger = require('../utils/logger');

class ErrorTrackingService {
  constructor() {
    this.client = null;
    this.provider = config.errorTracking.provider;
    this.enabled = config.errorTracking.enabled;
    
    if (this.enabled) {
      this.initialize();
    }
  }

  initialize() {
    try {
      switch (this.provider) {
        case 'sentry':
          this.initializeSentry();
          break;
        case 'rollbar':
          this.initializeRollbar();
          break;
        case 'bugsnag':
          this.initializeBugsnag();
          break;
        default:
          logger.warn(`Unknown error tracking provider: ${this.provider}`);
      }
    } catch (error) {
      logger.error('Failed to initialize error tracking:', error);
    }
  }

  initializeSentry() {
    const Sentry = require('@sentry/node');
    const { ProfilingIntegration } = require('@sentry/profiling-node');
    
    Sentry.init({
      dsn: config.errorTracking.sentry.dsn,
      environment: config.errorTracking.environment,
      release: config.errorTracking.release,
      serverName: config.errorTracking.sentry.serverName,
      attachStacktrace: config.errorTracking.sentry.attachStacktrace,
      sampleRate: config.errorTracking.sampleRate,
      tracesSampleRate: config.errorTracking.tracesSampleRate,
      integrations: [
        new ProfilingIntegration(),
      ],
      beforeSend(event, hint) {
        // Sanitize sensitive data
        if (event.request?.headers) {
          delete event.request.headers.authorization;
          delete event.request.headers.cookie;
        }
        return event;
      }
    });
    
    this.client = Sentry;
    logger.info('Sentry error tracking initialized');
  }

  initializeRollbar() {
    const Rollbar = require('rollbar');
    
    this.client = new Rollbar({
      accessToken: config.errorTracking.rollbar.accessToken,
      environment: config.errorTracking.environment,
      codeVersion: config.errorTracking.release,
      captureUncaught: config.errorTracking.rollbar.captureUncaught,
      captureUnhandledRejections: config.errorTracking.rollbar.captureUnhandledRejections,
    });
    
    logger.info('Rollbar error tracking initialized');
  }

  initializeBugsnag() {
    const Bugsnag = require('@bugsnag/js');
    const BugsnagPluginExpress = require('@bugsnag/plugin-express');
    
    Bugsnag.start({
      apiKey: process.env.BUGSNAG_API_KEY,
      releaseStage: config.errorTracking.environment,
      appVersion: config.errorTracking.release,
      plugins: [BugsnagPluginExpress],
    });
    
    this.client = Bugsnag;
    logger.info('Bugsnag error tracking initialized');
  }

  /**
   * Capture an exception
   */
  captureException(error, context = {}) {
    if (!this.enabled || !this.client) return;

    try {
      switch (this.provider) {
        case 'sentry':
          this.client.captureException(error, { extra: context });
          break;
        case 'rollbar':
          this.client.error(error, context);
          break;
        case 'bugsnag':
          this.client.notify(error, (event) => {
            event.addMetadata('context', context);
          });
          break;
      }
    } catch (e) {
      logger.error('Failed to capture exception:', e);
    }
  }

  /**
   * Capture a message
   */
  captureMessage(message, level = 'info', context = {}) {
    if (!this.enabled || !this.client) return;

    try {
      switch (this.provider) {
        case 'sentry':
          this.client.captureMessage(message, { level, extra: context });
          break;
        case 'rollbar':
          this.client[level](message, context);
          break;
        case 'bugsnag':
          this.client.notify(new Error(message), (event) => {
            event.severity = level;
            event.addMetadata('context', context);
          });
          break;
      }
    } catch (e) {
      logger.error('Failed to capture message:', e);
    }
  }

  /**
   * Set user context
   */
  setUser(userId, userData = {}) {
    if (!this.enabled || !this.client) return;

    try {
      switch (this.provider) {
        case 'sentry':
          this.client.setUser({ id: userId, ...userData });
          break;
        case 'rollbar':
          this.client.configure({ payload: { person: { id: userId, ...userData } } });
          break;
        case 'bugsnag':
          this.client.setUser(userId, userData.email, userData.name);
          break;
      }
    } catch (e) {
      logger.error('Failed to set user context:', e);
    }
  }

  /**
   * Add breadcrumb
   */
  addBreadcrumb(breadcrumb) {
    if (!this.enabled || !this.client) return;

    try {
      switch (this.provider) {
        case 'sentry':
          this.client.addBreadcrumb(breadcrumb);
          break;
        case 'rollbar':
          // Rollbar doesn't have breadcrumbs, use telemetry
          this.client.configure({
            payload: {
              telemetry: [breadcrumb]
            }
          });
          break;
        case 'bugsnag':
          this.client.leaveBreadcrumb(breadcrumb.message, breadcrumb.data);
          break;
      }
    } catch (e) {
      logger.error('Failed to add breadcrumb:', e);
    }
  }

  /**
   * Get Express middleware
   */
  getMiddleware() {
    if (!this.enabled || !this.client) {
      return (req, res, next) => next();
    }

    switch (this.provider) {
      case 'sentry':
        return [
          this.client.Handlers.requestHandler(),
          this.client.Handlers.tracingHandler(),
        ];
      case 'rollbar':
        return this.client.errorHandler();
      case 'bugsnag':
        return this.client.getPlugin('express').requestHandler;
      default:
        return (req, res, next) => next();
    }
  }

  /**
   * Get error handler middleware
   */
  getErrorHandler() {
    if (!this.enabled || !this.client) {
      return (err, req, res, next) => next(err);
    }

    switch (this.provider) {
      case 'sentry':
        return this.client.Handlers.errorHandler();
      case 'rollbar':
        return this.client.errorHandler();
      case 'bugsnag':
        return this.client.getPlugin('express').errorHandler;
      default:
        return (err, req, res, next) => next(err);
    }
  }
}

module.exports = new ErrorTrackingService();

