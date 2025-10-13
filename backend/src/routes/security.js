const express = require('express');
const router = express.Router();
const fs = require('fs');
const crypto = require('crypto');
const https = require('https');
const { asyncHandler } = require('../middleware/errorHandler');

/**
 * Public endpoint to serve certificate pins
 * Certificate pins are PUBLIC information (SHA-256 of public key)
 * This allows mobile apps to fetch pins dynamically
 * 
 * Endpoint: GET /api/security/certificate-pins
 */
router.get('/certificate-pins', asyncHandler(async (req, res) => {
  const serverUrl = process.env.PUBLIC_URL || req.protocol + '://' + req.get('host');
  
  res.json({
    note: "Certificate pins are PUBLIC information (SHA-256 of public key)",
    instructions: {
      automated: "Run: ./gradlew getCertificatePins -PBACKEND_URL=" + serverUrl,
      manual: "Use: openssl s_client -connect " + req.get('host') + " | openssl x509 -pubkey -noout | openssl rsa -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64"
    },
    security: {
      why_public: "Certificate pins are derived from public SSL certificates",
      safe_to_share: true,
      purpose: "Prevent MITM attacks even with rogue CA certificates"
    },
    setup: {
      step1: "Extract pins using automated Gradle task or openssl",
      step2: "Add pins to app/local.properties as CERT_PIN_1 and CERT_PIN_2",
      step3: "Build production APK - it will automatically use these pins",
      step4: "App will ONLY trust connections with matching certificate"
    },
    production_requirement: "Production builds REQUIRE certificate pins or they will fail to build",
    timestamp: new Date().toISOString()
  });
}));

module.exports = router;

