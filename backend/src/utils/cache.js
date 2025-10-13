const logger = require('./logger');

/**
 * Simple in-memory cache with TTL support
 * For production, consider Redis for distributed caching
 */
class Cache {
  constructor() {
    this.cache = new Map();
    this.ttls = new Map();
  }

  /**
   * Set a value with optional TTL (in milliseconds)
   */
  set(key, value, ttl = null) {
    this.cache.set(key, value);
    
    if (ttl) {
      const expiresAt = Date.now() + ttl;
      this.ttls.set(key, expiresAt);
    }
    
    logger.debug(`Cache set: ${key} (TTL: ${ttl ? ttl/1000 + 's' : 'none'})`);
  }

  /**
   * Get a value from cache (returns null if expired or not found)
   */
  get(key) {
    // Check if key exists
    if (!this.cache.has(key)) {
      return null;
    }

    // Check if expired
    if (this.ttls.has(key)) {
      const expiresAt = this.ttls.get(key);
      if (Date.now() > expiresAt) {
        // Expired - delete and return null
        this.delete(key);
        logger.debug(`Cache expired: ${key}`);
        return null;
      }
    }

    logger.debug(`Cache hit: ${key}`);
    return this.cache.get(key);
  }

  /**
   * Delete a key from cache
   */
  delete(key) {
    this.cache.delete(key);
    this.ttls.delete(key);
    logger.debug(`Cache deleted: ${key}`);
  }

  /**
   * Check if key exists and is not expired
   */
  has(key) {
    return this.get(key) !== null;
  }

  /**
   * Clear all cache
   */
  clear() {
    this.cache.clear();
    this.ttls.clear();
    logger.info('Cache cleared');
  }

  /**
   * Clear expired entries
   */
  clearExpired() {
    const now = Date.now();
    let count = 0;

    for (const [key, expiresAt] of this.ttls.entries()) {
      if (now > expiresAt) {
        this.delete(key);
        count++;
      }
    }

    if (count > 0) {
      logger.info(`Cleared ${count} expired cache entries`);
    }

    return count;
  }

  /**
   * Get cache statistics
   */
  getStats() {
    return {
      size: this.cache.size,
      keysWithTTL: this.ttls.size,
      keys: Array.from(this.cache.keys())
    };
  }
}

// Singleton instance
const cache = new Cache();

// Clear expired entries every hour
setInterval(() => {
  cache.clearExpired();
}, 60 * 60 * 1000);

module.exports = cache;

