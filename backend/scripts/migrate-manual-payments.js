#!/usr/bin/env node

/**
 * Migration script for manual_payments table
 * Epic 4: Free Tier - Manual Payment Tracking
 */

const db = require('../src/database/connection');

async function runMigration() {
  console.log('Running manual_payments migration...');
  
  try {
    // Create manual_payments table
    await db.query(`
      CREATE TABLE IF NOT EXISTS manual_payments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        account_id TEXT NOT NULL,
        payment_date TEXT NOT NULL,
        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
        source TEXT NOT NULL DEFAULT 'manual',
        created_at TEXT DEFAULT (datetime('now')),
        updated_at TEXT DEFAULT (datetime('now')),
        
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT unique_manual_payment UNIQUE (user_id, account_id, payment_date, amount_cents)
      )
    `);
    console.log('✓ Created manual_payments table');
    
    // Create index on user_id and account_id
    await db.query(`
      CREATE INDEX IF NOT EXISTS idx_manual_payments_user_account 
      ON manual_payments (user_id, account_id)
    `);
    console.log('✓ Created index: idx_manual_payments_user_account');
    
    // Create index on payment_date
    await db.query(`
      CREATE INDEX IF NOT EXISTS idx_manual_payments_date 
      ON manual_payments (payment_date)
    `);
    console.log('✓ Created index: idx_manual_payments_date');
    
    console.log('\n✅ Migration completed successfully!\n');
    process.exit(0);
    
  } catch (error) {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  }
}

runMigration();

