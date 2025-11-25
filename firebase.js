// firebase.js
const admin = require("firebase-admin");
require("dotenv").config(); // Make sure to load .env in Node.js

// List of required environment variables
const requiredVars = [
  "FIREBASE_TYPE",
  "FIREBASE_PROJECT_ID",
  "FIREBASE_PRIVATE_KEY",
  "FIREBASE_CLIENT_EMAIL",
  "FIREBASE_CLIENT_ID",
  "FIREBASE_AUTH_URI",
  "FIREBASE_TOKEN_URI",
  "FIREBASE_AUTH_PROVIDER_X509_CERT_URL",
  "FIREBASE_CLIENT_X509_CERT_URL",
];

// Check that all required env variables are set
requiredVars.forEach((key) => {
  if (!process.env[key]) {
    throw new Error(`Environment variable ${key} is not set`);
  }
});

// Construct Firebase service account
const serviceAccount = {
  type: process.env.FIREBASE_TYPE,
  project_id: process.env.FIREBASE_PROJECT_ID,
  private_key: process.env.FIREBASE_PRIVATE_KEY
    // Convert escaped \n into real line breaks
    .replace(/\\n/g, "\n")
    // Remove surrounding quotes if present
    .replace(/^"(.*)"$/, "$1"),
  client_email: process.env.FIREBASE_CLIENT_EMAIL,
  client_id: process.env.FIREBASE_CLIENT_ID,
  auth_uri: process.env.FIREBASE_AUTH_URI,
  token_uri: process.env.FIREBASE_TOKEN_URI,
  auth_provider_x509_cert_url: process.env.FIREBASE_AUTH_PROVIDER_X509_CERT_URL,
  client_x509_cert_url: process.env.FIREBASE_CLIENT_X509_CERT_URL,
};

// Initialize Firebase Admin if not already initialized
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

console.log("✅ Firebase Admin initialized successfully");

module.exports = admin;
