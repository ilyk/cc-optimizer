#!/usr/bin/env node

/**
 * Generate secure random keys for JWT and encryption
 * Run this script to generate new keys for production deployment
 */

const crypto = require('crypto');

function generateSecureKey(length = 32) {
  return crypto.randomBytes(length).toString('hex');
}

console.log('='.repeat(80));
console.log('SECURE KEY GENERATOR');
console.log('='.repeat(80));
console.log('\nGenerated secure keys for your .env file:\n');
console.log(`JWT_SECRET=${generateSecureKey(32)}`);
console.log(`ENCRYPTION_KEY=${generateSecureKey(32)}`);
console.log('\n' + '='.repeat(80));
console.log('⚠️  IMPORTANT: Keep these keys secret and never commit them to version control!');
console.log('='.repeat(80) + '\n');

