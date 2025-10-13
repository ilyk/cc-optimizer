const { Pool } = require('pg');
const Database = require('better-sqlite3');
const fs = require('fs');
const path = require('path');
const dbConfig = require('../config/database');
const logger = require('../utils/logger');

class DatabaseConnection {
  constructor() {
    this.type = dbConfig.type;
    this.db = null;
    this.initialized = false;
  }

  async initialize() {
    if (this.initialized) return;

    try {
      if (this.type === 'postgres') {
        this.db = new Pool(dbConfig.pg);
        await this.db.query('SELECT NOW()');
        logger.info('PostgreSQL database connected');
      } else {
        // Ensure data directory exists
        const dataDir = path.dirname(dbConfig.sqlite.filename);
        if (!fs.existsSync(dataDir)) {
          fs.mkdirSync(dataDir, { recursive: true });
        }
        
        this.db = new Database(dbConfig.sqlite.filename);
        this.db.pragma('journal_mode = WAL');
        logger.info('SQLite database connected');
      }
      
      await this.runMigrations();
      this.initialized = true;
    } catch (error) {
      logger.error('Database initialization failed:', error);
      throw error;
    }
  }

  async runMigrations() {
    logger.info('Running database migrations...');
    
    const migrations = [
      `CREATE TABLE IF NOT EXISTS users (
        id ${this.type === 'postgres' ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        created_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'},
        updated_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'}
      )`,
      
      `CREATE TABLE IF NOT EXISTS plaid_items (
        id ${this.type === 'postgres' ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
        user_id INTEGER NOT NULL,
        item_id VARCHAR(255) UNIQUE NOT NULL,
        access_token TEXT NOT NULL,
        created_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'},
        updated_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'},
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      )`,
      
      `CREATE TABLE IF NOT EXISTS credit_cards (
        id ${this.type === 'postgres' ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
        user_id INTEGER NOT NULL,
        account_id VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        mask VARCHAR(10),
        credit_limit DECIMAL(10, 2),
        statement_date INTEGER,
        due_date INTEGER,
        auto_pay_enabled BOOLEAN DEFAULT FALSE,
        auto_pay_amount DECIMAL(10, 2),
        created_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'},
        updated_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'},
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        UNIQUE(user_id, account_id)
      )`,
      
      `CREATE TABLE IF NOT EXISTS webhook_logs (
        id ${this.type === 'postgres' ? 'SERIAL PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT'},
        webhook_type VARCHAR(50),
        webhook_code VARCHAR(50),
        item_id VARCHAR(255),
        payload TEXT,
        created_at ${this.type === 'postgres' ? 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP' : 'DATETIME DEFAULT CURRENT_TIMESTAMP'}
      )`
    ];

    for (const migration of migrations) {
      await this.query(migration);
    }
    
    logger.info('Migrations completed successfully');
  }

  async query(sql, params = []) {
    if (!this.initialized) {
      await this.initialize();
    }

    if (this.type === 'postgres') {
      const result = await this.db.query(sql, params);
      return result.rows;
    } else {
      // Convert PostgreSQL $1, $2 style to SQLite ? style
      const sqliteSQL = sql.replace(/\$\d+/g, '?');
      
      if (sqliteSQL.trim().toLowerCase().startsWith('select') || 
          sqliteSQL.trim().toLowerCase().startsWith('returning')) {
        return this.db.prepare(sqliteSQL).all(...params);
      } else if (sqliteSQL.trim().toLowerCase().startsWith('insert')) {
        const info = this.db.prepare(sqliteSQL).run(...params);
        return [{ id: info.lastInsertRowid }];
      } else {
        this.db.prepare(sqliteSQL).run(...params);
        return [];
      }
    }
  }

  async close() {
    if (this.db) {
      if (this.type === 'postgres') {
        await this.db.end();
      } else {
        this.db.close();
      }
      logger.info('Database connection closed');
    }
  }
}

module.exports = new DatabaseConnection();

