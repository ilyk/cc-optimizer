const express = require('express');
const { body, validationResult } = require('express-validator');
const socialAuthService = require('../services/socialAuthService');
const { authMiddleware } = require('../middleware/auth');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * @route POST /api/auth/google
 * @desc Authenticate with Google ID token
 * @access Public
 */
router.post('/google', [
  body('idToken')
    .notEmpty()
    .withMessage('Google ID token is required')
    .isString()
    .withMessage('ID token must be a string'),
], async (req, res) => {
  try {
    // Validate input
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: errors.array(),
      });
    }

    const { idToken } = req.body;

    // Authenticate with Google
    const result = await socialAuthService.authenticateWithGoogle(idToken);

    logger.info(`Google authentication successful for user: ${result.user.email}`);

    res.json({
      success: true,
      message: 'Google authentication successful',
      data: result,
    });
  } catch (error) {
    logger.error('Google authentication route error:', error.message);
    
    res.status(401).json({
      success: false,
      message: error.message || 'Google authentication failed',
    });
  }
});

/**
 * @route POST /api/auth/facebook
 * @desc Authenticate with Facebook access token
 * @access Public
 */
router.post('/facebook', [
  body('accessToken')
    .notEmpty()
    .withMessage('Facebook access token is required')
    .isString()
    .withMessage('Access token must be a string'),
], async (req, res) => {
  try {
    // Validate input
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: errors.array(),
      });
    }

    const { accessToken } = req.body;

    // Authenticate with Facebook
    const result = await socialAuthService.authenticateWithFacebook(accessToken);

    logger.info(`Facebook authentication successful for user: ${result.user.email}`);

    res.json({
      success: true,
      message: 'Facebook authentication successful',
      data: result,
    });
  } catch (error) {
    logger.error('Facebook authentication route error:', error.message);
    
    res.status(401).json({
      success: false,
      message: error.message || 'Facebook authentication failed',
    });
  }
});

/**
 * @route POST /api/auth/social/link
 * @desc Link social provider to existing account
 * @access Private
 */
router.post('/link', [
  authMiddleware,
  body('provider')
    .isIn(['google', 'facebook'])
    .withMessage('Provider must be google or facebook'),
  body('providerUserId')
    .notEmpty()
    .withMessage('Provider user ID is required'),
], async (req, res) => {
  try {
    // Validate input
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: errors.array(),
      });
    }

    const { provider, providerUserId } = req.body;
    const userId = req.user.id;

    // Link social provider
    await socialAuthService.linkSocialProvider(userId, provider, providerUserId);

    logger.info(`Social provider linked for user: ${req.user.email}`);

    res.json({
      success: true,
      message: 'Social provider linked successfully',
    });
  } catch (error) {
    logger.error('Link social provider route error:', error.message);
    
    res.status(400).json({
      success: false,
      message: error.message || 'Failed to link social provider',
    });
  }
});

/**
 * @route DELETE /api/auth/social/unlink
 * @desc Unlink social provider from account
 * @access Private
 */
router.delete('/unlink', [
  authMiddleware,
  body('provider')
    .isIn(['google', 'facebook'])
    .withMessage('Provider must be google or facebook'),
], async (req, res) => {
  try {
    // Validate input
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        errors: errors.array(),
      });
    }

    const { provider } = req.body;
    const userId = req.user.id;

    // Unlink social provider
    await socialAuthService.unlinkSocialProvider(userId, provider);

    logger.info(`Social provider unlinked for user: ${req.user.email}`);

    res.json({
      success: true,
      message: 'Social provider unlinked successfully',
    });
  } catch (error) {
    logger.error('Unlink social provider route error:', error.message);
    
    res.status(400).json({
      success: false,
      message: error.message || 'Failed to unlink social provider',
    });
  }
});

module.exports = router;
