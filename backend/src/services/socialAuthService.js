const { OAuth2Client } = require('google-auth-library');
const axios = require('axios');
const userRepository = require('../repositories/userRepository');
const { generateToken } = require('../middleware/auth');
const logger = require('../utils/logger');

class SocialAuthService {
  constructor() {
    // Initialize Google OAuth2 client
    this.googleClient = new OAuth2Client();
    
    // Facebook Graph API base URL
    this.facebookGraphUrl = 'https://graph.facebook.com/v18.0';
  }

  /**
   * Verify Google ID token and authenticate/register user
   */
  async authenticateWithGoogle(idToken) {
    try {
      // Verify the Google ID token
      const ticket = await this.googleClient.verifyIdToken({
        idToken: idToken,
        audience: process.env.GOOGLE_CLIENT_ID, // Your Google OAuth2 client ID
      });

      const payload = ticket.getPayload();
      
      if (!payload) {
        throw new Error('Invalid Google ID token');
      }

      // Extract user information from Google token
      const googleUserId = payload.sub;
      const email = payload.email;
      const name = payload.name;
      const emailVerified = payload.email_verified;

      if (!email || !emailVerified) {
        throw new Error('Google account email not verified');
      }

      logger.info(`Google authentication attempt for: ${email}`);

      // Check if user already exists
      let user = await userRepository.findByEmail(email);
      
      if (user) {
        // User exists, log them in
        logger.info(`Existing user logged in via Google: ${email}`);
      } else {
        // Create new user (without password since they're using Google)
        user = await userRepository.createUserFromSocialAuth(email, name, 'google', googleUserId);
        logger.info(`New user created via Google: ${email}`);
      }

      // Generate JWT token
      const token = generateToken(user.id, user.email);

      return {
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
        },
        token,
        provider: 'google',
      };
    } catch (error) {
      logger.error('Google authentication error:', error.message);
      throw new Error('Google authentication failed');
    }
  }

  /**
   * Verify Facebook access token and authenticate/register user
   */
  async authenticateWithFacebook(accessToken) {
    try {
      // Verify the Facebook access token by calling Graph API
      const response = await axios.get(`${this.facebookGraphUrl}/me`, {
        params: {
          access_token: accessToken,
          fields: 'id,name,email',
        },
        timeout: 10000, // 10 second timeout
      });

      const facebookUser = response.data;
      
      if (!facebookUser || !facebookUser.id) {
        throw new Error('Invalid Facebook access token');
      }

      const facebookUserId = facebookUser.id;
      const email = facebookUser.email;
      const name = facebookUser.name;

      if (!email) {
        // Facebook user might not have provided email permission
        throw new Error('Email permission required for Facebook login');
      }

      logger.info(`Facebook authentication attempt for: ${email}`);

      // Check if user already exists
      let user = await userRepository.findByEmail(email);
      
      if (user) {
        // User exists, log them in
        logger.info(`Existing user logged in via Facebook: ${email}`);
      } else {
        // Create new user (without password since they're using Facebook)
        user = await userRepository.createUserFromSocialAuth(email, name, 'facebook', facebookUserId);
        logger.info(`New user created via Facebook: ${email}`);
      }

      // Generate JWT token
      const token = generateToken(user.id, user.email);

      return {
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
        },
        token,
        provider: 'facebook',
      };
    } catch (error) {
      logger.error('Facebook authentication error:', error.message);
      if (error.response && error.response.status === 401) {
        throw new Error('Invalid Facebook access token');
      }
      throw new Error('Facebook authentication failed');
    }
  }

  /**
   * Link existing account with social provider
   */
  async linkSocialProvider(userId, provider, providerUserId) {
    try {
      const user = await userRepository.findById(userId);
      
      if (!user) {
        throw new Error('User not found');
      }

      // Update user with social provider info
      await userRepository.linkSocialProvider(userId, provider, providerUserId);
      
      logger.info(`Linked ${provider} account for user: ${user.email}`);
      
      return { success: true };
    } catch (error) {
      logger.error('Link social provider error:', error.message);
      throw error;
    }
  }

  /**
   * Unlink social provider from account
   */
  async unlinkSocialProvider(userId, provider) {
    try {
      const user = await userRepository.findById(userId);
      
      if (!user) {
        throw new Error('User not found');
      }

      // Remove social provider link
      await userRepository.unlinkSocialProvider(userId, provider);
      
      logger.info(`Unlinked ${provider} account for user: ${user.email}`);
      
      return { success: true };
    } catch (error) {
      logger.error('Unlink social provider error:', error.message);
      throw error;
    }
  }
}

module.exports = new SocialAuthService();
