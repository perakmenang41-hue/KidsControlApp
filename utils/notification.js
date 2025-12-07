const admin = require("../firebase");
const db = admin.firestore();

// Send push notification to parent
async function sendNotification(parentId, title, body, data = {}) {
    try {
        const parentDoc = await db.collection("Parent_registered").doc(parentId).get();
        if (!parentDoc.exists) return;

        const fcmToken = parentDoc.data()?.fcmToken;
        if (!fcmToken) return;

        const safeData = {};
        for (const key in data) {
            safeData[key] = data[key] != null ? String(data[key]) : "";
        }

        await admin.messaging().send({
            token: fcmToken,
            notification: { title, body },
            data: safeData
        });

        console.log("✅ Push sent:", title);
    } catch (err) {
        console.error("❌ Push error:", err);
    }
}

// Log notification in Firestore
async function logNotification(notification) {
    try {
        const {
            parentId,
            childUID,
            zoneId,
            type,
            level,
            riskScore,
            reasons,
            durationInZone,
            zoneLat,
            zoneLon,
            childLat,
            childLon,
            readStatus
        } = notification;

        const payload = {
            childUID: String(childUID),
            zoneId: String(zoneId),
            type: String(type),
            level: String(level),
            riskScore: Number(riskScore),
            reasons: reasons || "",
            durationInZone: durationInZone || "",
            zoneLat: Number(zoneLat),
            zoneLon: Number(zoneLon),
            childLat: Number(childLat),
            childLon: Number(childLon),
            readStatus: Boolean(readStatus),
            timestamp: new Date()
        };

        await db.collection("notifications_log")
            .doc(parentId)
            .set(
                { notifications: admin.firestore.FieldValue.arrayUnion(payload) },
                { merge: true }
            );

        console.log("✅ Firestore log saved for parent:", parentId);
    } catch (err) {
        console.error("❌ Firestore log error:", err);
    }
}

module.exports = { sendNotification, logNotification };
