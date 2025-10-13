const db = require('../database/connection');
const encryption = require('../utils/encryption');

class UserRepository {
  /**
   * Create a new user
   */
  async createUser(email, password) {
    const passwordHash = await encryption.hashPassword(password);
    
    const result = await db.query(
      'INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id, email, created_at',
      [email, passwordHash]
    );
    
    return result[0];
  }

  /**
   * Find user by email
   */
  async findByEmail(email) {
    const result = await db.query(
      'SELECT id, email, password_hash, created_at FROM users WHERE email = $1',
      [email]
    );
    
    return result[0];
  }

  /**
   * Find user by ID
   */
  async findById(userId) {
    const result = await db.query(
      'SELECT id, email, created_at FROM users WHERE id = $1',
      [userId]
    );
    
    return result[0];
  }

  /**
   * Verify user password with constant-time check to prevent timing attacks
   */
  async verifyPassword(email, password) {
    const user = await this.findByEmail(email);
    
    // Always run bcrypt comparison even if user doesn't exist
    // This prevents timing attacks that reveal valid emails
    const hash = user ? user.password_hash : '$2a$12$invalidhashxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';
    const isValid = await encryption.verifyPassword(password, hash);
    
    // Only return user if both user exists AND password is valid
    if (!user || !isValid) {
      return null;
    }

    // Don't return password hash
    const { password_hash, ...userWithoutPassword } = user;
    return userWithoutPassword;
  }

  /**
   * Update user - SECURE: whitelisted columns only
   */
  async updateUser(userId, updates) {
    // Whitelist of allowed columns to prevent SQL injection
    const ALLOWED_COLUMNS = ['email'];
    
    const setClause = [];
    const values = [];
    let paramIndex = 1;

    for (const [key, value] of Object.entries(updates)) {
      // CRITICAL: Only allow whitelisted columns
      if (!ALLOWED_COLUMNS.includes(key)) {
        throw new Error(`Invalid update field: ${key}`);
      }
      
      setClause.push(`${key} = $${paramIndex}`);
      values.push(value);
      paramIndex++;
    }

    if (setClause.length === 0) {
      throw new Error('No valid fields to update');
    }

    values.push(userId);
    
    const result = await db.query(
      `UPDATE users SET ${setClause.join(', ')}, updated_at = CURRENT_TIMESTAMP 
       WHERE id = $${paramIndex} RETURNING id, email, created_at`,
      values
    );
    
    return result[0];
  }

  /**
   * Delete user
   */
  async deleteUser(userId) {
    await db.query('DELETE FROM users WHERE id = $1', [userId]);
  }
  
  // ========== MFA Methods ==========
  
  /**
   * Store FIDO2/WebAuthn challenge
   */
  async storeChallenge(userId, challenge, expiresAt) {
    await db.query(
      `INSERT INTO mfa_challenges (user_id, challenge, expires_at) 
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id) DO UPDATE 
       SET challenge = $2, expires_at = $3`,
      [userId, challenge, new Date(expiresAt)]
    );
  }
  
  /**
   * Get stored challenge
   */
  async getChallenge(userId) {
    const result = await db.query(
      'SELECT challenge, expires_at FROM mfa_challenges WHERE user_id = $1',
      [userId]
    );
    
    if (!result[0]) return null;
    
    return {
      challenge: result[0].challenge,
      expiresAt: new Date(result[0].expires_at).getTime()
    };
  }
  
  /**
   * Clear challenge after use
   */
  async clearChallenge(userId) {
    await db.query('DELETE FROM mfa_challenges WHERE user_id = $1', [userId]);
  }
  
  /**
   * Store FIDO2 credential
   */
  async storeCredential(userId, credential) {
    const result = await db.query(
      `INSERT INTO mfa_credentials 
       (user_id, credential_id, public_key, counter, type, created_at) 
       VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
       RETURNING id`,
      [
        userId,
        credential.credentialId,
        credential.publicKey,
        credential.counter || 0,
        credential.type || 'fido2'
      ]
    );
    
    return result[0];
  }
  
  /**
   * Get credential by ID
   */
  async getCredential(userId, credentialId) {
    const result = await db.query(
      `SELECT * FROM mfa_credentials 
       WHERE user_id = $1 AND credential_id = $2`,
      [userId, credentialId]
    );
    
    return result[0];
  }
  
  /**
   * Get all user credentials
   */
  async getUserCredentials(userId) {
    return db.query(
      'SELECT id, credential_id, type, created_at, last_used_at FROM mfa_credentials WHERE user_id = $1',
      [userId]
    );
  }
  
  /**
   * Update credential counter (anti-replay)
   */
  async updateCredentialCounter(credentialDbId, newCounter) {
    await db.query(
      `UPDATE mfa_credentials 
       SET counter = $1, last_used_at = CURRENT_TIMESTAMP 
       WHERE id = $2`,
      [newCounter, credentialDbId]
    );
  }
  
  /**
   * Remove credential
   */
  async removeCredential(userId, credentialId) {
    const result = await db.query(
      'DELETE FROM mfa_credentials WHERE user_id = $1 AND credential_id = $2',
      [userId, credentialId]
    );
    
    return result.rowCount > 0;
  }
  
  /**
   * Get biometric enabled status
   */
  async getBiometricEnabled(userId) {
    const result = await db.query(
      'SELECT biometric_enabled FROM user_mfa_settings WHERE user_id = $1',
      [userId]
    );
    
    return result[0]?.biometric_enabled || false;
  }
  
  /**
   * Set biometric enabled status
   */
  async setBiometricEnabled(userId, enabled) {
    await db.query(
      `INSERT INTO user_mfa_settings (user_id, biometric_enabled) 
       VALUES ($1, $2)
       ON CONFLICT (user_id) DO UPDATE 
       SET biometric_enabled = $2`,
      [userId, enabled]
    );
  }
}

module.exports = new UserRepository();

