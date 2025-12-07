const express = require("express");
const router = express.Router();
const admin = require("../firebase");
const calculateRisk = require("../aiRiskEngine");
const { getDistance } = require("../utils/geolocation");
const { sendNotification, logNotification } = require("../utils/notification");

const db = admin.firestore();

// ===============================
// Format milliseconds to DD:HH:MM:SS
// ===============================
function formatDuration(ms) {
    let s = Math.floor(ms / 1000);
    const d = Math.floor(s / 86400); s %= 86400;
    const h = Math.floor(s / 3600); s %= 3600;
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${d.toString().padStart(2,"0")}:${h.toString().padStart(2,"0")}:${m.toString().padStart(2,"0")}:${sec.toString().padStart(2,"0")}`;
}

// ===============================
// Update Child Location + AI Alert
// ===============================
router.post("/update-location", async (req, res) => {
    const { parentId, childUID, lat, lon } = req.body;

    if (!parentId || !childUID || typeof lat !== "number" || typeof lon !== "number") {
        return res.status(400).json({ success: false, message: "Invalid payload" });
    }

    console.log("📍 Incoming:", req.body);

    res.json({
        success: true,
        message: "Location received, processing in background"
    });

    (async () => {
        try {
            const now = Date.now();

            // 1️⃣ Calculate Speed
            const lastRef = db.collection("child_locations").doc(childUID);
            const lastDoc = await lastRef.get();
            let speedMS = 0;

            if (lastDoc.exists) {
                const last = lastDoc.data();
                const dt = Math.max(1, (now - last.timestamp) / 1000);
                speedMS = getDistance(last.lat, last.lon, lat, lon) / dt;
            }

            await lastRef.set({ lat, lon, timestamp: now }, { merge: true });

            // 2️⃣ Get previous child states
            const childRef = db.collection("child_position").doc(childUID);
            const childDoc = await childRef.get();
            const prevZones = childDoc.exists ? childDoc.data()?.zoneStates || {} : {};
            const lastAlerts = childDoc.exists ? childDoc.data()?.lastAlertTimes || {} : {};
            const timeInZoneRaw = childDoc.exists ? childDoc.data()?.timeInZoneRaw || {} : {};
            const newStates = {};
            const newAlertTimes = { ...lastAlerts };

            // 3️⃣ Zones
            const zonesSnap = await db.collection("Parent_registered")
                .doc(parentId)
                .collection("dangerZones")
                .get();

            const WARNING = 50;
            const COOLDOWN = 10 * 60 * 1000; // 10 minutes

            for (const zoneDoc of zonesSnap.docs) {
                const zone = zoneDoc.data();
                const zoneId = zone.zoneId || zoneDoc.id;
                if (!timeInZoneRaw[zoneId]) timeInZoneRaw[zoneId] = 0;

                const distance = getDistance(lat, lon, zone.lat, zone.lon);
                const { risk, level, reasons } = calculateRisk({
                    speed: speedMS,
                    hour: new Date().getHours(),
                    distance,
                    radius: zone.radius,
                    timeInZone: timeInZoneRaw[zoneId]
                });

                // Determine current state
                let state = "OUTSIDE";
                if (distance <= zone.radius) state = "INSIDE";
                else if (distance <= zone.radius + WARNING) state = "APPROACHING";
                else if (prevZones[zoneId] === "INSIDE") state = "EXITED";

                // ✅ Log prev/current for debugging
                console.log(`🟢 Child ${childUID} | Zone ${zone.name} | Prev: ${prevZones[zoneId] || "NONE"} | Current: ${state}`);

                if (state === "INSIDE") {
                    timeInZoneRaw[zoneId] += now - (childDoc.data()?.lastUpdatedRaw || now);
                }

                const canAlert =
                    risk >= 70 ||
                    now - (lastAlerts[zoneId] || 0) > COOLDOWN ||
                    state !== prevZones[zoneId];

                if (canAlert && state !== "OUTSIDE") {
                    const type = risk >= 70 ? "AI_ALERT" : state;

                    await sendNotification(
                        parentId,
                        "⚠️ Child Safety Alert",
                        `Child ${type} at ${zone.name}`
                    );

                    await logNotification({
                        parentId,
                        childUID,
                        zoneId,
                        type,
                        level,
                        riskScore: risk,
                        reasons: reasons.join(", "),
                        durationInZone: formatDuration(timeInZoneRaw[zoneId]),
                        zoneLat: zone.lat,
                        zoneLon: zone.lon,
                        childLat: lat,
                        childLon: lon,
                        readStatus: false
                    });

                    newAlertTimes[zoneId] = now;
                }

                newStates[zoneId] = state;
            }

            // Save in Firestore with formatted durations
            const timeInZoneFormatted = {};
            for (const zoneId in timeInZoneRaw) {
                timeInZoneFormatted[zoneId] = formatDuration(timeInZoneRaw[zoneId]);
            }

            await childRef.set({
           		parentId,
                lat,
                lon,
                speedMS,
                speedUnit: "m/s",
                lastUpdated: formatDuration(now),
                lastUpdatedRaw: now,
                zoneStates: newStates,
                lastAlertTimes: newAlertTimes,
                timeInZone: timeInZoneFormatted,
                timeInZoneRaw
            }, { merge: true });

        } catch (err) {
            console.error("❌ Background error:", err);
        }
    })();
});

module.exports = router;
