#!/bin/bash

echo "========================================="
echo "Credit Card Optimizer Backend Setup"
echo "========================================="
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18 or higher."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"

# Install dependencies
echo ""
echo "📦 Installing dependencies..."
npm install

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo ""
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    
    echo ""
    echo "🔐 Generating secure keys..."
    node scripts/generateKeys.js
    
    echo ""
    echo "⚠️  IMPORTANT: Please update .env file with:"
    echo "   1. Your Plaid credentials (PLAID_CLIENT_ID, PLAID_SECRET)"
    echo "   2. The generated JWT_SECRET and ENCRYPTION_KEY (shown above)"
    echo ""
else
    echo ""
    echo "✅ .env file already exists"
fi

# Create data directory for SQLite
mkdir -p data
mkdir -p logs

echo ""
echo "========================================="
echo "✅ Setup complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Edit .env file with your Plaid credentials"
echo "  2. Run 'npm start' to start the server"
echo "  3. Visit http://localhost:3000/health to verify"
echo ""

