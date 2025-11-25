// utils/notification.js
const { Expo } = require('expo-server-sdk');
let expo = new Expo();

const Parent = require('../models/Parent');

// Fetch parent push tokens from DB
async function getParentPushTokens(parentId) {
    const parent = await Parent.findById(parentId);
    return parent?.pushTokens || [];
}

// Send push notification to parent devices
async function sendNotification(parentId, title, body, data = {}) {
    const tokens = await getParentPushTokens(parentId);
    if (!tokens || tokens.length === 0) {
        console.log('No parent push tokens found for', parentId);
        return;
    }

    const messages = [];
    for (const token of tokens) {
        if (!Expo.isExpoPushToken(token)) {
            console.warn('Skipping invalid Expo token:', token);
            continue;
        }
        messages.push({ to: token, sound: 'default', title, body, data });
    }

    const chunks = expo.chunkPushNotifications(messages);
    for (const chunk of chunks) {
        try {
            const receipts = await expo.sendPushNotificationsAsync(chunk);
            console.log('Notification receipts:', receipts);
        } catch (err) {
            console.error('Error sending push notifications:', err);
        }
    }
}

module.exports = { sendNotification };
