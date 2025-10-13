const jwt = require('jsonwebtoken');
const logger = require('../utils/logger');

const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET environment variable is required');
}

/**
 * Generates a JWT token for a user
 */
function generateToken(userId, email) {
  return jwt.sign(
    { userId, email },
    JWT_SECRET,
    { 
      expiresIn: JWT_EXPIRES_IN,
      algorithm: 'HS256', // Explicit algorithm
      notBefore: 0 // Valid immediately
    }
  );
}

/**
 * Verifies a JWT token
 * Explicitly specifies algorithm to prevent algorithm confusion attacks
 */
function verifyToken(token) {
  try {
    return jwt.verify(token, JWT_SECRET, {
      algorithms: ['HS256'], // Prevent alg=none attack
      clockTolerance: 0, // No clock skew tolerance
      maxAge: JWT_EXPIRES_IN // Enforce expiration
    });
  } catch (error) {
    return null;
  }
}

/**
 * Authentication middleware
 * Validates JWT token from Authorization header
 */
function authMiddleware(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'No token provided' });
    }

    const token = authHeader.substring(7); // Remove 'Bearer ' prefix
    const decoded = verifyToken(token);

    if (!decoded) {
      return res.status(401).json({ error: 'Invalid or expired token' });
    }

    // Attach user info to request
    req.userId = decoded.userId;
    req.userEmail = decoded.email;
    
    next();
  } catch (error) {
    logger.error('Auth middleware error:', error);
    res.status(401).json({ error: 'Authentication failed' });
  }
}

/**
 * Optional authentication middleware
 * Attaches user info if token is valid, but doesn't reject if missing
 */
function optionalAuthMiddleware(req, res, next) {
  try {
    const authHeader = req.headers.authorization;
    
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = verifyToken(token);
      
      if (decoded) {
        req.userId = decoded.userId;
        req.userEmail = decoded.email;
      }
    }
    
    next();
  } catch (error) {
    next();
  }
}

module.exports = {
  authMiddleware,
  optionalAuthMiddleware,
  generateToken,
  verifyToken
};

