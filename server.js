require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const PORT = process.env.PORT || 5000;

// =============================
// Firebase Admin Init
// =============================
const admin = require("./firebase");
const db = admin.firestore();

// =============================
// Middleware
// =============================
app.use(cors());
app.use(express.json());

// Optional: API key middleware
const apiKeyMiddleware = require("./middleware/apiKeyMiddleware");
app.use(apiKeyMiddleware);

// Simple request logger
app.use((req, res, next) => {
    console.log(`Incoming request: ${req.method} ${req.url}`);
    if (req.method === "POST" || req.method === "PUT") {
        console.log("Body:", req.body);
    }
    next();
});

// =============================
// Route Imports
// =============================
const familyRoute = require("./routes/familyRoute");
const dangerZoneRoute = require("./routes/dangerZoneRoute");

// =============================
// Routes
// =============================
app.use("/api/child/family", familyRoute);
app.use("/api/danger-zone", dangerZoneRoute);

// =============================
// Helper Functions
// =============================

// Haversine distance
function getDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1 * Math.PI / 180) *
        Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) ** 2;
    return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

// Push notification
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

// Format duration in DD:HH:MM:SS
function formatDuration(ms) {
    let s = Math.floor(ms / 1000);
    const d = Math.floor(s / 86400); s %= 86400;
    const h = Math.floor(s / 3600); s %= 3600;
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${d.toString().padStart(2,"0")}:${h.toString().padStart(2,"0")}:${m.toString().padStart(2,"0")}:${sec.toString().padStart(2,"0")}`;
}

// =============================
// Background Interval for Duration Updates
// =============================
const CHECK_INTERVAL = 5000; // every 5 seconds

setInterval(async () => {
    try {
        const now = Date.now();
        const childSnap = await db.collection("child_position").get(); // use child_position

        for (const childDoc of childSnap.docs) {
            const childData = childDoc.data();
            const childUID = childDoc.id;

            // 🔹 Fetch parentId safely
            let parentId = childData.parentId;
            if (!parentId) {
                // fallback: try to get from child_locations
                const locDoc = await db.collection("child_locations").doc(childUID).get();
                if (locDoc.exists) {
                    parentId = locDoc.data()?.parentId;
                }
            }

            if (!parentId) {
                console.warn(`⚠️ No parentId found for child ${childUID}, skipping`);
                continue; // skip this child
            }

            const zones = childData.zoneStates || {};
            const timeInZoneRaw = childData.timeInZoneRaw || {};
            const lastUpdateRaw = childData.lastUpdatedRaw || now;

            let updated = false;

            for (const zoneId in zones) {
                if (zones[zoneId] === "INSIDE") {
                    const delta = now - lastUpdateRaw;
                    timeInZoneRaw[zoneId] = (timeInZoneRaw[zoneId] || 0) + delta;

                    const PROLONGED = 5 * 60 * 1000; // 1 minute for fast testing
                    if (timeInZoneRaw[zoneId] >= PROLONGED) {
                        console.log(`⚠️ PROLONGED stay detected: Child ${childUID} in zone ${zoneId}`);

                        await logNotification({
                            parentId,
                            childUID,
                            zoneId,
                            type: "PROLONGED",
                            level: "HIGH",
                            riskScore: 100,
                            reasons: "Prolonged stay inside restricted area",
                            durationInZone: formatDuration(timeInZoneRaw[zoneId]),
                            zoneLat: childData.zoneLat || 0,
                            zoneLon: childData.zoneLon || 0,
                            childLat: childData.lat,
                            childLon: childData.lon,
                            readStatus: false
                        });

                        await sendNotification(
                            parentId,
                            "⚠️ Prolonged Stay Alert",
                            `Child ${childUID} has stayed too long in zone ${zoneId}`
                        );

                        timeInZoneRaw[zoneId] = 0; // reset timer
                    }

                    updated = true;
                }
            }

            if (updated) {
                const timeInZoneFormatted = {};
                for (const zoneId in timeInZoneRaw) {
                    timeInZoneFormatted[zoneId] = formatDuration(timeInZoneRaw[zoneId]);
                }

                await childDoc.ref.set({
                    timeInZone: timeInZoneFormatted,
                    timeInZoneRaw,
                    lastUpdated: formatDuration(now),
                    lastUpdatedRaw: now
                }, { merge: true });
            }
        }
    } catch (err) {
        console.error("❌ Error in background duration update:", err);
    }
}, CHECK_INTERVAL);

// =============================
// Error Handling
// =============================
app.use((err, req, res, next) => {
    console.error("Unhandled Error:", err);
    res.status(500).json({ success: false, message: "Server error", error: err.message });
});

// =============================
// Start Server
// =============================
app.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));