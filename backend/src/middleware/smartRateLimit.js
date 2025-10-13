const logger = require('../utils/logger');

/**
 * Smart rate limiting that's transparent to users
 * 
 * Instead of blocking requests, we:
 * 1. Track request patterns per user
 * 2. Return cached data for rapid requests
 * 3. Prioritize background refresh over real-time for excessive requests
 * 4. Never show "rate limited" errors to users
 */

class SmartRateLimiter {
  constructor() {
    // Track request counts per user per endpoint
    this.requestCounts = new Map();
    
    // Track last request time per user per endpoint
    this.lastRequests = new Map();
    
    // Thresholds (requests per minute)
    this.thresholds = {
      'accounts': 10,        // 10 requests/minute for accounts
      'liabilities': 10,     // 10 requests/minute for liabilities  
      'transactions': 5,     // 5 requests/minute for transactions (heavy)
      'default': 20          // 20 requests/minute for other endpoints
    };
    
    // Window size (1 minute)
    this.windowMs = 60 * 1000;
    
    // Clean up old entries every 5 minutes
    setInterval(() => this.cleanup(), 5 * 60 * 1000);
  }
  
  /**
   * Get key for tracking
   */
  getKey(userId, endpoint) {
    return `${userId}:${endpoint}`;
  }
  
  /**
   * Check if request should be throttled
   * Returns { shouldThrottle: boolean, strategy: string }
   */
  checkRequest(userId, endpoint) {
    const key = this.getKey(userId, endpoint);
    const now = Date.now();
    
    // Get or initialize request tracking
    if (!this.requestCounts.has(key)) {
      this.requestCounts.set(key, []);
      this.lastRequests.set(key, now);
    }
    
    const requests = this.requestCounts.get(key);
    const lastRequest = this.lastRequests.get(key);
    
    // Remove old requests outside the window
    const validRequests = requests.filter(time => now - time < this.windowMs);
    this.requestCounts.set(key, validRequests);
    
    // Add current request
    validRequests.push(now);
    this.lastRequests.set(key, now);
    
    // Get threshold for this endpoint
    const threshold = this.thresholds[endpoint] || this.thresholds.default;
    
    // Calculate request rate
    const requestRate = validRequests.length;
    const timeSinceLastRequest = now - lastRequest;
    
    // Determine strategy
    if (requestRate > threshold * 1.5) {
      // Excessive requests - serve cache only, no refresh
      logger.warn(`Excessive requests for ${userId} on ${endpoint}: ${requestRate}/${threshold}`);
      return { 
        shouldThrottle: true, 
        strategy: 'cache-only',
        reason: 'excessive-requests'
      };
    } else if (requestRate > threshold) {
      // Approaching limit - serve cache, refresh in background
      logger.info(`High request rate for ${userId} on ${endpoint}: ${requestRate}/${threshold}`);
      return { 
        shouldThrottle: true, 
        strategy: 'stale-while-revalidate',
        reason: 'high-rate'
      };
    } else if (timeSinceLastRequest < 1000) {
      // Rapid successive requests (< 1s apart) - deduplicate
      logger.info(`Rapid requests for ${userId} on ${endpoint}: ${timeSinceLastRequest}ms apart`);
      return { 
        shouldThrottle: true, 
        strategy: 'deduplicate',
        reason: 'rapid-requests'
      };
    }
    
    // Normal request - proceed
    return { 
      shouldThrottle: false, 
      strategy: 'normal',
      reason: 'ok'
    };
  }
  
  /**
   * Cleanup old entries
   */
  cleanup() {
    const now = Date.now();
    
    for (const [key, requests] of this.requestCounts.entries()) {
      const validRequests = requests.filter(time => now - time < this.windowMs);
      
      if (validRequests.length === 0) {
        this.requestCounts.delete(key);
        this.lastRequests.delete(key);
      } else {
        this.requestCounts.set(key, validRequests);
      }
    }
    
    logger.debug(`Rate limiter cleanup: ${this.requestCounts.size} active users`);
  }
  
  /**
   * Reset rate limit for a user (useful for testing)
   */
  reset(userId) {
    const keysToDelete = [];
    for (const key of this.requestCounts.keys()) {
      if (key.startsWith(userId + ':')) {
        keysToDelete.push(key);
      }
    }
    keysToDelete.forEach(key => {
      this.requestCounts.delete(key);
      this.lastRequests.delete(key);
    });
  }
}

const rateLimiter = new SmartRateLimiter();

/**
 * Middleware for smart rate limiting
 */
function smartRateLimit(endpoint) {
  return (req, res, next) => {
    const userId = req.user?.id || req.ip;
    
    const { shouldThrottle, strategy, reason } = rateLimiter.checkRequest(userId, endpoint);
    
    // Add rate limit info to request for handler to use
    req.rateLimit = {
      shouldThrottle,
      strategy,
      reason,
      userId,
      endpoint
    };
    
    // Log if throttled
    if (shouldThrottle) {
      logger.info(`Rate limit strategy for ${userId} on ${endpoint}: ${strategy} (${reason})`);
    }
    
    next();
  };
}

module.exports = {
  smartRateLimit,
  rateLimiter
};

