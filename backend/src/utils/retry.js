const logger = require('./logger');

/**
 * Retry utility with exponential backoff
 * Handles transient errors gracefully
 */
class RetryHelper {
  /**
   * Retry a function with exponential backoff
   * 
   * @param {Function} fn - Async function to retry
   * @param {Object} options - Retry configuration
   * @returns {Promise} - Result of successful execution
   */
  async retry(fn, options = {}) {
    const {
      maxAttempts = 3,
      initialDelayMs = 1000,
      maxDelayMs = 10000,
      backoffMultiplier = 2,
      retryableErrors = ['ECONNRESET', 'ETIMEDOUT', 'ENOTFOUND', 'RATE_LIMIT'],
      onRetry = null,
    } = options;

    let lastError;
    let delay = initialDelayMs;

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        const result = await fn();
        
        if (attempt > 1) {
          logger.info(`Retry succeeded on attempt ${attempt}`);
        }
        
        return result;
      } catch (error) {
        lastError = error;

        // Check if error is retryable
        const isRetryable = this.isRetryableError(error, retryableErrors);
        
        if (!isRetryable || attempt === maxAttempts) {
          logger.error(`Failed after ${attempt} attempt(s): ${error.message}`);
          throw error;
        }

        // Calculate delay with jitter
        const jitter = Math.random() * 0.3 * delay; // ±30% jitter
        const delayWithJitter = Math.min(delay + jitter, maxDelayMs);

        logger.warn(
          `Attempt ${attempt}/${maxAttempts} failed: ${error.message}. ` +
          `Retrying in ${Math.round(delayWithJitter)}ms...`
        );

        // Call retry callback if provided
        if (onRetry) {
          onRetry(attempt, error, delayWithJitter);
        }

        // Wait before retry
        await this.sleep(delayWithJitter);

        // Exponential backoff for next attempt
        delay = Math.min(delay * backoffMultiplier, maxDelayMs);
      }
    }

    throw lastError;
  }

  /**
   * Check if error should be retried
   */
  isRetryableError(error, retryableErrors) {
    // Network errors
    if (error.code && retryableErrors.includes(error.code)) {
      return true;
    }

    // HTTP status codes that should be retried
    const retryableStatusCodes = [408, 429, 500, 502, 503, 504];
    if (error.response && retryableStatusCodes.includes(error.response.status)) {
      return true;
    }

    // Plaid-specific errors
    if (error.error_code) {
      const retryablePlaidErrors = [
        'INSTITUTION_DOWN',
        'INSTITUTION_NOT_RESPONDING',
        'INTERNAL_SERVER_ERROR',
      ];
      return retryablePlaidErrors.includes(error.error_code);
    }

    return false;
  }

  /**
   * Sleep for specified milliseconds
   */
  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Retry multiple operations in parallel with individual retry logic
   */
  async retryAll(operations, options = {}) {
    const promises = operations.map(op => 
      this.retry(op.fn, { ...options, ...op.options })
    );

    return Promise.allSettled(promises);
  }
}

module.exports = new RetryHelper();

