// firebase.js
const admin = require("firebase-admin");
require("dotenv").config();

let serviceAccount;

// --------------------------------------------------
// 1️⃣ Prefer SINGLE JSON from Render SERVICE_ACCOUNT_KEY
// --------------------------------------------------
if (process.env.SERVICE_ACCOUNT_KEY) {
  try {
    serviceAccount = JSON.parse(process.env.SERVICE_ACCOUNT_KEY);

    // 🔹 Fix the private_key line breaks for PEM format
    if (serviceAccount.private_key) {
      serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, "\n");
    }

    console.log("🔥 Using SERVICE_ACCOUNT_KEY from Render");
  } catch (err) {
    console.error("❌ Failed to parse SERVICE_ACCOUNT_KEY:", err);
    process.exit(1); // Stop server if key is invalid
  }
}

// --------------------------------------------------
// 2️⃣ Fallback: Use individual ENV vars (local development)
// --------------------------------------------------
if (!serviceAccount) {
  serviceAccount = {
    type: process.env.FIREBASE_TYPE,
    project_id: process.env.FIREBASE_PROJECT_ID,
    private_key: process.env.FIREBASE_PRIVATE_KEY
      ? process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n")
      : undefined,
    client_email: process.env.FIREBASE_CLIENT_EMAIL,
    client_id: process.env.FIREBASE_CLIENT_ID,
    auth_uri: process.env.FIREBASE_AUTH_URI,
    token_uri: process.env.FIREBASE_TOKEN_URI,
    auth_provider_x509_cert_url: process.env.FIREBASE_AUTH_PROVIDER_X509_CERT_URL,
    client_x509_cert_url: process.env.FIREBASE_CLIENT_X509_CERT_URL,
  };

  console.log("🔥 Using individual FIREBASE_* ENV variables");
}

// --------------------------------------------------
// 3️⃣ Initialize Firebase Admin
// --------------------------------------------------
if (!admin.apps.length) {
  try {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    console.log("✅ Firebase Admin initialized successfully");
  } catch (err) {
    console.error("❌ Firebase Admin initialization failed:", err);
    process.exit(1);
  }
}

module.exports = admin;
