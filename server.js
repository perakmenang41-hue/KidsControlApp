require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const bodyParser = require('body-parser');
const apiKeyMiddleware = require('./middleware/apiKeyMiddleware');
const admin = require('firebase-admin');
const serviceAccount = require('./firebaseServiceAccountKey.json');

// Routes
const childRoute = require('./routes/childRoute');
const dangerZoneRoute = require('./routes/dangerZoneRoute');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(express.json());

// Secure API routes with API Key
app.use(apiKeyMiddleware);

// --------------------
// Initialize Firebase Admin (only once)
// --------------------
if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

// --------------------
// MongoDB connection
// --------------------
mongoose.connect(process.env.MONGO_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(() => console.log('MongoDB connected'))
  .catch(err => console.error('MongoDB connection error:', err));

// --------------------
// Child & DangerZone routes
// --------------------
app.use('/api/child', childRoute);
app.use('/api/dangerzone', dangerZoneRoute);

// --------------------
// Save FCM token endpoint
// --------------------
app.post("/api/parent/save-token", (req, res) => {
    const { parentId, fcmToken } = req.body;

    if (!parentId || !fcmToken) {
        return res.status(400).json({ message: "parentId and fcmToken are required" });
    }

    console.log("Received FCM token:", { parentId, fcmToken });

    // TODO: Save token to MongoDB if needed

    res.status(200).json({ message: "Token saved successfully" });
});

// --------------------
// Send test notification endpoint
// --------------------
app.post("/api/test/send-notification", async (req, res) => {
    const { fcmToken, title, body } = req.body;

    if (!fcmToken) return res.status(400).json({ success: false, message: "FCM token required" });

    const message = {
        token: fcmToken,
        notification: {
            title: title || "Test Notification",
            body: body || "This is a test notification from backend"
        }
    };

    try {
        const response = await admin.messaging().send(message);
        console.log("Notification sent:", response);
        res.json({ success: true, response });
    } catch (err) {
        console.error("Error sending notification:", err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// --------------------
// Start server
// --------------------
app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
