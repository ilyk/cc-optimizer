const crypto = require('crypto');

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 16;
const SALT_LENGTH = 64;
const TAG_LENGTH = 16;
const KEY_LENGTH = 32;
const ITERATIONS = 100000;

class Encryption {
  constructor() {
    this.secret = process.env.ENCRYPTION_KEY;
    if (!this.secret) {
      throw new Error('ENCRYPTION_KEY environment variable is required');
    }
  }

  /**
   * Derives a key from the secret using PBKDF2
   */
  deriveKey(salt) {
    return crypto.pbkdf2Sync(
      this.secret,
      salt,
      ITERATIONS,
      KEY_LENGTH,
      'sha512'
    );
  }

  /**
   * Encrypts text using AES-256-GCM
   */
  encrypt(text) {
    const salt = crypto.randomBytes(SALT_LENGTH);
    const iv = crypto.randomBytes(IV_LENGTH);
    const key = this.deriveKey(salt);

    const cipher = crypto.createCipheriv(ALGORITHM, key, iv);
    
    let encrypted = cipher.update(text, 'utf8', 'hex');
    encrypted += cipher.final('hex');
    
    const tag = cipher.getAuthTag();

    // Combine salt + iv + tag + encrypted data
    return Buffer.concat([
      salt,
      iv,
      tag,
      Buffer.from(encrypted, 'hex')
    ]).toString('base64');
  }

  /**
   * Decrypts text encrypted with encrypt()
   */
  decrypt(encryptedData) {
    const buffer = Buffer.from(encryptedData, 'base64');

    const salt = buffer.subarray(0, SALT_LENGTH);
    const iv = buffer.subarray(SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
    const tag = buffer.subarray(SALT_LENGTH + IV_LENGTH, SALT_LENGTH + IV_LENGTH + TAG_LENGTH);
    const encrypted = buffer.subarray(SALT_LENGTH + IV_LENGTH + TAG_LENGTH);

    const key = this.deriveKey(salt);

    const decipher = crypto.createDecipheriv(ALGORITHM, key, iv);
    decipher.setAuthTag(tag);

    let decrypted = decipher.update(encrypted, null, 'utf8');
    decrypted += decipher.final('utf8');

    return decrypted;
  }

  /**
   * Hashes a password using bcrypt-compatible method
   */
  async hashPassword(password) {
    const bcrypt = require('bcryptjs');
    return bcrypt.hash(password, 12);
  }

  /**
   * Verifies a password against a hash
   */
  async verifyPassword(password, hash) {
    const bcrypt = require('bcryptjs');
    return bcrypt.compare(password, hash);
  }
}

module.exports = new Encryption();

