const { spawn } = require('child_process');
const path = require('path');

console.log('Starting Credit Card Optimizer servers...\n');

// Start the main backend server on port 12080
console.log('1. Starting backend server on port 12080...');
const backend = spawn('node', ['server.js'], {
  stdio: 'inherit',
  cwd: __dirname,
  env: { ...process.env, PORT: '12080' }
});

// Start the HTTPS proxy on port 12443
console.log('2. Starting HTTPS proxy on port 12443...');
const proxy = spawn('node', ['https-proxy.js'], {
  stdio: 'inherit',
  cwd: __dirname
});

// Handle process termination
const cleanup = () => {
  console.log('\nShutting down servers...');
  backend.kill();
  proxy.kill();
  process.exit(0);
};

process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);

// Handle child process errors
backend.on('error', (err) => {
  console.error('Backend server error:', err);
});

proxy.on('error', (err) => {
  console.error('HTTPS proxy error:', err);
});

console.log('\n✅ Both servers are starting...');
console.log('📱 Backend: http://localhost:12080');
console.log('🔒 HTTPS Proxy: https://localhost:12443');
console.log('🔄 Press Ctrl+C to stop both servers\n');
