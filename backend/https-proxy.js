const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

// Generate self-signed certificate if it doesn't exist
const generateSelfSignedCert = () => {
  const { execSync } = require('child_process');
  const certDir = path.join(__dirname, 'certs');
  
  if (!fs.existsSync(certDir)) {
    fs.mkdirSync(certDir, { recursive: true });
  }
  
  const keyPath = path.join(certDir, 'server.key');
  const certPath = path.join(certDir, 'server.crt');
  
  if (!fs.existsSync(keyPath) || !fs.existsSync(certPath)) {
    console.log('Generating self-signed certificate...');
    try {
      execSync(`openssl req -x509 -newkey rsa:4096 -keyout "${keyPath}" -out "${certPath}" -days 365 -nodes -subj "/C=US/ST=State/L=City/O=Organization/CN=192.168.86.78"`, { stdio: 'inherit' });
      console.log('Certificate generated successfully');
    } catch (error) {
      console.error('Failed to generate certificate:', error.message);
      process.exit(1);
    }
  }
  
  return { keyPath, certPath };
};

const { keyPath, certPath } = generateSelfSignedCert();

// Create HTTPS server
const options = {
  key: fs.readFileSync(keyPath),
  cert: fs.readFileSync(certPath)
};

const httpsPort = 12443;
const httpPort = 12080;

// Create HTTPS proxy server
const httpsServer = https.createServer(options, (req, res) => {
  // Add CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  
  // Handle preflight requests
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }
  
  // Proxy to HTTP backend
  const proxyReq = http.request({
    hostname: 'localhost',
    port: httpPort,
    path: req.url,
    method: req.method,
    headers: {
      ...req.headers,
      host: `localhost:${httpPort}`
    }
  }, (proxyRes) => {
    // Copy response headers
    Object.keys(proxyRes.headers).forEach(key => {
      res.setHeader(key, proxyRes.headers[key]);
    });
    
    res.writeHead(proxyRes.statusCode);
    proxyRes.pipe(res);
  });
  
  proxyReq.on('error', (err) => {
    console.error('Proxy error:', err.message);
    res.writeHead(500);
    res.end('Proxy Error');
  });
  
  // Forward request body
  req.pipe(proxyReq);
});

httpsServer.listen(httpsPort, '0.0.0.0', () => {
  console.log(`HTTPS proxy running on https://0.0.0.0:${httpsPort}`);
  console.log(`Proxying to http://localhost:${httpPort}`);
  console.log('Self-signed certificate warnings are normal in browsers');
});

// Handle graceful shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down HTTPS proxy...');
  httpsServer.close(() => {
    console.log('HTTPS proxy closed');
    process.exit(0);
  });
});
