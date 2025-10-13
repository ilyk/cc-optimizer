const { param, validationResult } = require('express-validator');

/**
 * Validates URL parameters to prevent injection attacks
 */

/**
 * Validation for alphanumeric IDs (Plaid item IDs, account IDs, etc.)
 */
const validateId = (paramName) => [
  param(paramName)
    .isString()
    .trim()
    .matches(/^[a-zA-Z0-9_-]+$/)
    .withMessage(`${paramName} must be alphanumeric with hyphens/underscores only`)
    .isLength({ min: 1, max: 255 })
    .withMessage(`${paramName} must be between 1 and 255 characters`),
  (req, res, next) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        error: 'Invalid parameter',
        details: errors.array()
      });
    }
    next();
  }
];

/**
 * Validation for item IDs (Plaid format)
 */
const validateItemId = validateId('itemId');

/**
 * Validation for account IDs (Plaid format)
 */
const validateAccountId = validateId('accountId');

module.exports = {
  validateItemId,
  validateAccountId,
  validateId
};

