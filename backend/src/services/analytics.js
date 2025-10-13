/**
 * Configurable Analytics Service
 * Supports multiple providers: Mixpanel, Amplitude, Segment
 */

const config = require('../config/monitoring');
const logger = require('../utils/logger');

class AnalyticsService {
  constructor() {
    this.client = null;
    this.provider = config.analytics.provider;
    this.enabled = config.analytics.enabled;
    this.eventQueue = [];
    this.flushTimer = null;
    
    if (this.enabled) {
      this.initialize();
    }
  }

  initialize() {
    try {
      switch (this.provider) {
        case 'mixpanel':
          this.initializeMixpanel();
          break;
        case 'amplitude':
          this.initializeAmplitude();
          break;
        case 'segment':
          this.initializeSegment();
          break;
        default:
          logger.warn(`Unknown analytics provider: ${this.provider}`);
      }

      // Start batch flushing if enabled
      if (config.analytics.batch.enabled) {
        this.startBatchFlushing();
      }
    } catch (error) {
      logger.error('Failed to initialize analytics:', error);
    }
  }

  initializeMixpanel() {
    const Mixpanel = require('mixpanel');
    
    this.client = Mixpanel.init(config.analytics.mixpanel.token, {
      secret: config.analytics.mixpanel.secret,
    });
    
    logger.info('Mixpanel analytics initialized');
  }

  initializeAmplitude() {
    const Amplitude = require('@amplitude/node');
    
    this.client = Amplitude.init(config.analytics.amplitude.apiKey);
    
    logger.info('Amplitude analytics initialized');
  }

  initializeSegment() {
    const Analytics = require('analytics-node');
    
    this.client = new Analytics(config.analytics.segment.writeKey);
    
    logger.info('Segment analytics initialized');
  }

  /**
   * Sanitize event properties
   */
  sanitizeProperties(properties) {
    if (!config.analytics.anonymize) {return properties;}

    const sanitized = { ...properties };
    
    // Remove excluded fields
    config.analytics.excludeFields.forEach(field => {
      delete sanitized[field];
    });

    return sanitized;
  }

  /**
   * Track an event
   */
  track(userId, event, properties = {}) {
    if (!this.enabled || !this.client) {return;}

    const sanitizedProps = this.sanitizeProperties(properties);

    try {
      if (config.analytics.batch.enabled) {
        // Add to queue
        this.eventQueue.push({ userId, event, properties: sanitizedProps });
        
        // Flush if queue is full
        if (this.eventQueue.length >= config.analytics.batch.maxSize) {
          this.flush();
        }
      } else {
        // Send immediately
        this.sendEvent(userId, event, sanitizedProps);
      }
    } catch (error) {
      logger.error('Failed to track event:', error);
    }
  }

  /**
   * Send event immediately
   */
  sendEvent(userId, event, properties) {
    switch (this.provider) {
      case 'mixpanel':
        this.client.track(event, {
          distinct_id: userId,
          ...properties
        });
        break;
      case 'amplitude':
        this.client.logEvent({
          user_id: userId,
          event_type: event,
          event_properties: properties,
        });
        break;
      case 'segment':
        this.client.track({
          userId,
          event,
          properties,
        });
        break;
    }
  }

  /**
   * Identify a user
   */
  identify(userId, traits = {}) {
    if (!this.enabled || !this.client) {return;}

    const sanitizedTraits = this.sanitizeProperties(traits);

    try {
      switch (this.provider) {
        case 'mixpanel':
          this.client.people.set(userId, sanitizedTraits);
          break;
        case 'amplitude':
          this.client.setUserId(userId);
          this.client.setUserProperties(sanitizedTraits);
          break;
        case 'segment':
          this.client.identify({
            userId,
            traits: sanitizedTraits,
          });
          break;
      }
    } catch (error) {
      logger.error('Failed to identify user:', error);
    }
  }

  /**
   * Flush event queue
   */
  flush() {
    if (this.eventQueue.length === 0) {return;}

    const events = [...this.eventQueue];
    this.eventQueue = [];

    events.forEach(({ userId, event, properties }) => {
      this.sendEvent(userId, event, properties);
    });

    logger.debug(`Flushed ${events.length} analytics events`);
  }

  /**
   * Start batch flushing timer
   */
  startBatchFlushing() {
    this.flushTimer = setInterval(() => {
      this.flush();
    }, config.analytics.batch.flushInterval);
  }

  /**
   * Stop batch flushing
   */
  stopBatchFlushing() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flush(); // Flush remaining events
    }
  }

  /**
   * Common events
   */
  get events() {
    return {
    // Authentication
      USER_REGISTERED: 'User Registered',
      USER_LOGGED_IN: 'User Logged In',
      USER_LOGGED_OUT: 'User Logged Out',
    
      // Plaid
      PLAID_LINK_STARTED: 'Plaid Link Started',
      PLAID_LINK_SUCCESS: 'Plaid Link Success',
      PLAID_LINK_EXIT: 'Plaid Link Exit',
      PLAID_ACCOUNTS_FETCHED: 'Plaid Accounts Fetched',
    
      // Cards
      CREDIT_CARDS_VIEWED: 'Credit Cards Viewed',
      CARD_DETAILS_VIEWED: 'Card Details Viewed',
      STATEMENT_DATE_SET: 'Statement Date Set Manually',
    
      // Recommendations
      RECOMMENDATIONS_GENERATED: 'Recommendations Generated',
      RECOMMENDATIONS_VIEWED: 'Recommendations Viewed',
    
      // API
      API_REQUEST: 'API Request',
      API_ERROR: 'API Error',
    
      // Performance
      STATEMENT_ANALYSIS_COMPLETED: 'Statement Analysis Completed',
      LLM_ANALYSIS_USED: 'LLM Analysis Used',
      CACHE_HIT: 'Cache Hit',
      CACHE_MISS: 'Cache Miss',
    };
  }
}

module.exports = new AnalyticsService();

