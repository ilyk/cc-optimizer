@echo off
echo =========================================
echo Credit Card Optimizer Backend Setup
echo =========================================
echo.

REM Check if Node.js is installed
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo X Node.js is not installed. Please install Node.js 18 or higher.
    exit /b 1
)

echo OK Node.js version:
node --version

REM Install dependencies
echo.
echo Installing dependencies...
call npm install

REM Create .env file if it doesn't exist
if not exist .env (
    echo.
    echo Creating .env file from template...
    copy .env.example .env
    
    echo.
    echo Generating secure keys...
    node scripts/generateKeys.js
    
    echo.
    echo WARNING: Please update .env file with:
    echo    1. Your Plaid credentials (PLAID_CLIENT_ID, PLAID_SECRET)
    echo    2. The generated JWT_SECRET and ENCRYPTION_KEY (shown above)
    echo.
) else (
    echo.
    echo OK .env file already exists
)

REM Create data directory for SQLite
if not exist data mkdir data
if not exist logs mkdir logs

echo.
echo =========================================
echo OK Setup complete!
echo =========================================
echo.
echo Next steps:
echo   1. Edit .env file with your Plaid credentials
echo   2. Run 'npm start' to start the server
echo   3. Visit http://localhost:3000/health to verify
echo.
pause

