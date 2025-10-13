const userRepository = require('../repositories/userRepository');
const { generateToken } = require('../middleware/auth');
const logger = require('../utils/logger');

class AuthService {
  /**
   * Register a new user
   */
  async register(email, password) {
    try {
      // Check if user already exists
      const existingUser = await userRepository.findByEmail(email);
      
      if (existingUser) {
        // Generic error message to prevent user enumeration
        const error = new Error('Registration failed. Please try a different email.');
        error.statusCode = 400;
        throw error;
      }

      // Create user
      const user = await userRepository.createUser(email, password);

      // Generate JWT token
      const token = generateToken(user.id, user.email);

      logger.info(`New user registered: ${email}`);

      return {
        user: {
          id: user.id,
          email: user.email,
        },
        token,
      };
    } catch (error) {
      logger.error('Registration error:', error.message);
      throw error;
    }
  }

  /**
   * Login user
   */
  async login(email, password) {
    try {
      // Verify credentials (uses constant-time comparison)
      const user = await userRepository.verifyPassword(email, password);
      
      if (!user) {
        // Generic error message to prevent user enumeration
        const error = new Error('Invalid email or password');
        error.statusCode = 401;
        throw error;
      }

      // Generate JWT token
      const token = generateToken(user.id, user.email);

      logger.info(`User logged in: ${email}`);

      return {
        user: {
          id: user.id,
          email: user.email,
        },
        token,
      };
    } catch (error) {
      logger.error('Login error:', error.message);
      throw error;
    }
  }

  /**
   * Get user profile
   */
  async getProfile(userId) {
    try {
      const user = await userRepository.findById(userId);
      
      if (!user) {
        throw new Error('User not found');
      }

      return {
        id: user.id,
        email: user.email,
        created_at: user.created_at,
      };
    } catch (error) {
      logger.error('Get profile error:', error);
      throw error;
    }
  }
}

module.exports = new AuthService();

