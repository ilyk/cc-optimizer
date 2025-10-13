# Database Setup Guide

The backend supports both PostgreSQL (production) and SQLite (development).

## Quick Start - SQLite (Development)

SQLite works out of the box with zero configuration:

```bash
# Just start the server - database auto-created
npm start
```

Database file created at: `data/cc_optimizer.db`

## Production - PostgreSQL

### 1. Install PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

### 2. Create Database and User

```bash
# Login to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE cc_optimizer;
CREATE USER cc_optimizer WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE cc_optimizer TO cc_optimizer;
\q
```

### 3. Configure Environment

Add to your `.env` file:

```env
# Database Configuration
DB_TYPE=postgres
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cc_optimizer
DB_USER=cc_optimizer
DB_PASSWORD=your_secure_password
DB_SSL=false

# For production, enable SSL
# DB_SSL=true
# DB_SSL_REJECT_UNAUTHORIZED=true
```

### 4. Initialize Database

```bash
# Migrations run automatically on first start
npm start
```

## Database Schema

### Tables

#### users
- `id` - Primary key
- `email` - Unique user email
- `password_hash` - Bcrypt hashed password
- `created_at`, `updated_at` - Timestamps

#### plaid_items
- `id` - Primary key
- `user_id` - Foreign key to users
- `item_id` - Plaid item ID (unique)
- `access_token` - Encrypted Plaid access token
- `created_at`, `updated_at` - Timestamps

#### credit_cards
- `id` - Primary key
- `user_id` - Foreign key to users
- `account_id` - Plaid account ID
- `name` - Card name (e.g., "Chase Freedom")
- `mask` - Last 4 digits
- `credit_limit` - Credit limit
- `statement_date` - Day of month statement generates
- `due_date` - Day of month payment is due
- `auto_pay_enabled` - Auto-pay feature flag
- `auto_pay_amount` - Auto-pay amount
- `created_at`, `updated_at` - Timestamps

#### webhook_logs
- `id` - Primary key
- `webhook_type` - Plaid webhook type
- `webhook_code` - Plaid webhook code
- `item_id` - Related Plaid item
- `payload` - Full webhook payload (JSON)
- `created_at` - Timestamp

## Security Features

✅ **Encryption**: Access tokens encrypted at rest using AES-256-GCM
✅ **Hashing**: Passwords hashed with bcrypt (12 rounds)
✅ **Foreign Keys**: Cascade deletes protect data integrity
✅ **Unique Constraints**: Prevent duplicate items/cards

## Backup & Restore

### PostgreSQL Backup

```bash
# Backup
pg_dump -U cc_optimizer cc_optimizer > backup.sql

# Restore
psql -U cc_optimizer cc_optimizer < backup.sql
```

### SQLite Backup

```bash
# Just copy the file
cp data/cc_optimizer.db data/cc_optimizer.backup.db
```

## Common Operations

### View All Users

```sql
SELECT id, email, created_at FROM users;
```

### View User's Plaid Connections

```sql
SELECT u.email, p.item_id, p.created_at 
FROM plaid_items p 
JOIN users u ON p.user_id = u.id;
```

### View User's Credit Cards

```sql
SELECT u.email, c.name, c.mask, c.statement_date 
FROM credit_cards c 
JOIN users u ON c.user_id = u.id;
```

## Troubleshooting

### Connection Refused

```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Start if stopped
sudo systemctl start postgresql
```

### Permission Denied

```bash
# Grant permissions
sudo -u postgres psql
GRANT ALL PRIVILEGES ON DATABASE cc_optimizer TO cc_optimizer;
\q
```

### Migration Errors

Migrations run automatically. If they fail:

1. Check database connection
2. Check user permissions
3. Review logs in console

## Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_TYPE` | `sqlite` | Database type: `postgres` or `sqlite` |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `cc_optimizer` | Database name |
| `DB_USER` | `cc_optimizer` | Database user |
| `DB_PASSWORD` | *(required)* | Database password |
| `DB_SSL` | `false` | Enable SSL for PostgreSQL |

## Production Checklist

- [ ] Use PostgreSQL (not SQLite)
- [ ] Enable SSL (`DB_SSL=true`)
- [ ] Use strong database password
- [ ] Set up automated backups
- [ ] Configure connection pooling
- [ ] Monitor database performance
- [ ] Set up replication (optional)

---

**Status**: ✅ Database fully implemented and ready to use!

