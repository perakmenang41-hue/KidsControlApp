// utils/notification.js
const admin = require('../firebase');

// Send notification to a parent
async function sendNotification(parentId, title, body) {
  try {
    const parentRef = admin.firestore().collection('parents').doc(parentId);
    const doc = await parentRef.get();

    if (!doc.exists) {
      console.warn(`Parent ${parentId} not found`);
      return;
    }

    const parentData = doc.data();
    const fcmToken = parentData?.fcmToken;

    if (!fcmToken) {
      console.warn(`Parent ${parentId} has no FCM token`);
      return;
    }

    const message = {
      token: fcmToken,
      notification: { title, body }
    };

    const response = await admin.messaging().send(message);
    console.log("✅ Notification sent:", response);
    return response;

  } catch (err) {
    console.error("❌ Error sending notification:", err);
    throw err;
  }
}

module.exports = { sendNotification };
