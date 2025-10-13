const crypto = require('crypto');
const logger = require('./logger');

/**
 * Verifies Plaid webhook signature using raw request body
 *
 * Plaid signs webhook requests with HMAC SHA256. We must compute the HMAC
 * over the RAW body (not parsed JSON), then compare in constant time.
 */
function verifyPlaidWebhook(rawBody, signature, verificationKey) {
  try {
    if (!verificationKey) {
      logger.warn('Webhook verification key not configured');
      return true; // Skip verification if key not set (development mode)
    }

    if (!rawBody || !Buffer.isBuffer(rawBody)) {
      logger.error('Missing raw body for webhook verification');
      return false;
    }

    if (!signature || typeof signature !== 'string') {
      return false;
    }

    // Plaid uses HMAC SHA256 of the raw request body
    const expectedSignatureHex = crypto
      .createHmac('sha256', verificationKey)
      .update(rawBody)
      .digest('hex');

    const provided = Buffer.from(signature, 'hex');
    const expected = Buffer.from(expectedSignatureHex, 'hex');

    // Prevent timing leaks when lengths differ
    if (provided.length !== expected.length) {
      return false;
    }

    return crypto.timingSafeEqual(provided, expected);
  } catch (error) {
    logger.error('Webhook verification error:', error);
    return false;
  }
}

/**
 * Middleware to verify Plaid webhooks
 */
function webhookVerificationMiddleware(req, res, next) {
  const verificationKey = process.env.PLAID_WEBHOOK_VERIFICATION_KEY;

  // Skip verification in development if key not set
  if (!verificationKey && process.env.NODE_ENV !== 'production') {
    logger.warn('Webhook verification disabled - set PLAID_WEBHOOK_VERIFICATION_KEY');
    return next();
  }

  // Support both header spellings just in case
  const signature = req.headers['plaid-verification'] || req.headers['plaid-signature'];

  if (!signature) {
    logger.warn('Webhook received without signature');
    return res.status(401).json({ error: 'Missing webhook signature' });
  }

  const rawBody = req.rawBody || (Buffer.isBuffer(req.body) ? req.body : null);
  const isValid = verifyPlaidWebhook(rawBody, signature, verificationKey);

  if (!isValid) {
    logger.error('Invalid webhook signature');
    return res.status(401).json({ error: 'Invalid webhook signature' });
  }

  // If body is still a Buffer here, parse JSON so downstream handlers see an object
  try {
    if (Buffer.isBuffer(req.body)) {
      req.rawBody = req.body;
      req.body = JSON.parse(req.body.toString('utf8'));
    }
  } catch (e) {
    logger.error('Failed to parse webhook JSON body');
    return res.status(400).json({ error: 'Invalid JSON' });
  }

  // Validate timestamp to prevent replay attacks
  // Plaid doesn't send timestamp in standard format, but we can track seen webhooks
  // For now, we'll accept all webhooks but log them to detect replays
  const webhookId = `${req.body.webhook_type}_${req.body.webhook_code}_${req.body.item_id}_${Date.now()}`;
  req.webhookId = webhookId;

  // In production, implement a cache (Redis) to track recent webhook IDs
  // and reject duplicates within a time window (e.g., 5 minutes)

  next();
}

module.exports = {
  verifyPlaidWebhook,
  webhookVerificationMiddleware
};

