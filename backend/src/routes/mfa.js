const express = require('express');
const { asyncHandler } = require('../middleware/errorHandler');
const { authMiddleware } = require('../middleware/auth');
const mfaService = require('../services/mfaService');

const router = express.Router();

/**
 * Generate FIDO2/WebAuthn challenge
 * 
 * POST /api/v1/mfa/challenge
 */
router.post('/challenge', authMiddleware, asyncHandler(async (req, res) => {
  const challenge = await mfaService.generateChallenge(req.userId);
  
  res.json({
    challenge: challenge.challenge,
    expiresAt: challenge.expiresAt
  });
}));

/**
 * Register FIDO2/WebAuthn credential
 * 
 * POST /api/v1/mfa/register
 */
router.post('/register', authMiddleware, asyncHandler(async (req, res) => {
  const { registrationResponse } = req.body;
  
  const result = await mfaService.verifyRegistration(req.userId, registrationResponse);
  
  res.json({
    credentialId: result.credentialId,
    success: true
  });
}));

/**
 * Authenticate with FIDO2/WebAuthn
 * 
 * POST /api/v1/mfa/authenticate
 */
router.post('/authenticate', authMiddleware, asyncHandler(async (req, res) => {
  const { authenticationResponse } = req.body;
  
  const result = await mfaService.verifyAuthentication(req.userId, authenticationResponse);
  
  res.json({
    success: result
  });
}));

/**
 * Get user's registered credentials
 * 
 * GET /api/v1/mfa/credentials
 */
router.get('/credentials', authMiddleware, asyncHandler(async (req, res) => {
  const credentials = await mfaService.getUserCredentials(req.userId);
  
  // Don't send full public keys, just metadata
  const credentialsInfo = credentials.map(c => ({
    id: c.id,
    credentialId: c.credentialId,
    createdAt: c.createdAt,
    lastUsedAt: c.lastUsedAt,
    type: c.type
  }));
  
  res.json(credentialsInfo);
}));

/**
 * Remove a credential
 * 
 * DELETE /api/v1/mfa/credentials/:credentialId
 */
router.delete('/credentials/:credentialId', authMiddleware, asyncHandler(async (req, res) => {
  const { credentialId } = req.params;
  
  await mfaService.removeCredential(req.userId, credentialId);
  
  res.json({
    success: true,
    message: 'Credential removed'
  });
}));

/**
 * Get MFA status for user
 * 
 * GET /api/v1/mfa/status
 */
router.get('/status', authMiddleware, asyncHandler(async (req, res) => {
  const status = await mfaService.getUserMfaStatus(req.userId);
  
  res.json(status);
}));

module.exports = router;

