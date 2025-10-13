const path = require('path');

const dbConfig = {
  // Use PostgreSQL in production, SQLite for development
  type: process.env.DATABASE_TYPE || 'sqlite',
  
  // PostgreSQL configuration
  pg: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432'),
    database: process.env.DB_NAME || 'cc_optimizer',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || '',
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
  },
  
  // SQLite configuration
  sqlite: {
    filename: process.env.SQLITE_DB_PATH || path.join(__dirname, '../../data/database.sqlite'),
  }
};

module.exports = dbConfig;

