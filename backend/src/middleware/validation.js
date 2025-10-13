const { body, validationResult } = require('express-validator');

/**
 * Validation middleware to check for validation errors
 */
function validate(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ 
      error: 'Validation failed',
      details: errors.array() 
    });
  }
  next();
}

/**
 * Validation rules for user registration
 */
const registerValidation = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Invalid email address'),
  body('password')
    .isLength({ min: 8 })
    .withMessage('Password must be at least 8 characters long')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
    .withMessage('Password must contain uppercase, lowercase, and number'),
  validate
];

/**
 * Validation rules for user login
 */
const loginValidation = [
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Invalid email address'),
  body('password')
    .notEmpty()
    .withMessage('Password is required'),
  validate
];

/**
 * Validation rules for exchanging public token
 */
const exchangeTokenValidation = [
  body('public_token')
    .notEmpty()
    .withMessage('public_token is required')
    .isString()
    .withMessage('public_token must be a string'),
  validate
];

/**
 * Validation rules for getting transactions
 */
const transactionsValidation = [
  body('start_date')
    .notEmpty()
    .withMessage('start_date is required')
    .matches(/^\d{4}-\d{2}-\d{2}$/)
    .withMessage('start_date must be in YYYY-MM-DD format')
    .custom((value, { req }) => {
      const start = new Date(value);
      if (isNaN(start.getTime())) throw new Error('start_date is invalid');
      const now = new Date();
      if (start > now) throw new Error('start_date cannot be in the future');
      return true;
    }),
  body('end_date')
    .notEmpty()
    .withMessage('end_date is required')
    .matches(/^\d{4}-\d{2}-\d{2}$/)
    .withMessage('end_date must be in YYYY-MM-DD format')
    .custom((value, { req }) => {
      const end = new Date(value);
      if (isNaN(end.getTime())) throw new Error('end_date is invalid');
      const now = new Date();
      if (end > now) throw new Error('end_date cannot be in the future');
      const start = new Date(req.body.start_date);
      if (isNaN(start.getTime())) throw new Error('start_date is invalid');
      if (end < start) throw new Error('end_date must be after start_date');
      // Limit range to 1 year to prevent abuse
      const oneYearMs = 365 * 24 * 60 * 60 * 1000;
      if (end.getTime() - start.getTime() > oneYearMs) {
        throw new Error('date range must be <= 365 days');
      }
      return true;
    }),
  validate
];

/**
 * Validation rules for credit card configuration
 */
const creditCardValidation = [
  body('account_id')
    .notEmpty()
    .withMessage('account_id is required')
    .isString()
    .trim()
    .escape(),
  body('name')
    .notEmpty()
    .withMessage('name is required')
    .isLength({ max: 255 })
    .withMessage('name must be less than 255 characters')
    .trim()
    .escape(),
  body('mask')
    .optional()
    .isLength({ max: 10 })
    .withMessage('mask must be <= 10 characters')
    .trim()
    .escape(),
  body('credit_limit')
    .optional()
    .isFloat({ min: 0, max: 10000000 })
    .withMessage('credit_limit must be a positive number'),
  body('statement_date')
    .optional()
    .isInt({ min: 1, max: 31 })
    .withMessage('statement_date must be between 1 and 31'),
  body('due_date')
    .optional()
    .isInt({ min: 1, max: 31 })
    .withMessage('due_date must be between 1 and 31'),
  body('auto_pay_enabled')
    .optional()
    .isBoolean()
    .withMessage('auto_pay_enabled must be a boolean')
    .toBoolean(),
  body('auto_pay_amount')
    .optional()
    .isFloat({ min: 0, max: 10000000 })
    .withMessage('auto_pay_amount must be a positive number'),
  validate
];

module.exports = {
  validate,
  registerValidation,
  loginValidation,
  exchangeTokenValidation,
  transactionsValidation,
  creditCardValidation
};

