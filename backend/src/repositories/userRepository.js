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
}

module.exports = new UserRepository();

