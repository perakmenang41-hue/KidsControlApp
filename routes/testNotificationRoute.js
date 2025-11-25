const express = require('express');
const router = express.Router();
const admin = require('../firebase'); // <- path to firebase.js

router.post('/send-test', async (req, res) => {
    const { fcmToken } = req.body;

    if (!fcmToken) return res.status(400).json({ success: false, message: 'FCM token required' });

    const message = {
        token: fcmToken,
        notification: {
            title: 'Test Notification',
            body: 'This is a test push notification from backend!'
        }
    };

    try {
        const response = await admin.messaging().send(message);
        console.log('Message sent:', response);
        res.json({ success: true, response });
    } catch (err) {
        console.error('Error sending message:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

module.exports = router;
