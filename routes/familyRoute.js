// routes/familyRoute.js
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

    // -------------------------
    // Ignore Google default fallback coordinates
    // -------------------------
    const DEFAULT_LAT = 37.4219983;
    const DEFAULT_LON = -122.084;

    if (lat === DEFAULT_LAT && lon === DEFAULT_LON) {
        console.warn(`⚠️ Ignoring default fallback coordinates for child ${childUID}`);
        return res.json({ success: false, message: "Default coordinates ignored" });
    }

    console.log("📍 Incoming:", req.body);

    // Respond fast, process in background
    res.json({ success: true, message: "Location received, processing in background" });

    (async () => {
        try {
            const now = Date.now();

            // -------------------------
            // 1️⃣ Calculate speed (m/s)
            // -------------------------
            const lastRef = db.collection("child_locations").doc(childUID);
            const lastDoc = await lastRef.get();
            let speedMS = 0;

            if (lastDoc.exists) {
                const last = lastDoc.data();
                const lastTs = last.timestamp || now;
                const dt = Math.max(1, (now - lastTs) / 1000); // seconds

                const distKm = getDistance(last.lat, last.lon, lat, lon);
                const distM = distKm * 1000; // ✅ meters

                speedMS = distM / dt;
            }

            await lastRef.set({ lat, lon, timestamp: now }, { merge: true });

            // -------------------------
            // 2️⃣ Load previous child_position
            // -------------------------
            const childRef = db.collection("child_position").doc(childUID);
            const childDoc = await childRef.get();
            const childData = childDoc.exists ? childDoc.data() : {};

            const prevZones = childData.zoneStates || {};
            const lastAlerts = childData.lastAlertTimes || {};
            const timeInZoneRaw = childData.timeInZoneRaw || {};
            const exitCandidates = childData.exitCandidates || {};
            const lastUpdatedRaw = childData.lastUpdatedRaw || now;

            // -------------------------
            // 3️⃣ Load zones
            // -------------------------
            const zonesSnap = await db.collection("Parent_registered")
                .doc(parentId)
                .collection("dangerZones")
                .get();

            // Config
            const APPROACH_BUFFER = 20;          // meters
            const PROLONGED_MS = 5 * 60 * 1000;  // 5 min
            const EXIT_CONFIRM_MS = 3 * 1000;    // 3 sec
            const COOLDOWN = 10 * 60 * 1000;     // 10 min

            const newStates = {};
            const newAlertTimes = { ...lastAlerts };

            for (const z of zonesSnap.docs) {
                if (!timeInZoneRaw[z.id]) timeInZoneRaw[z.id] = 0;
            }

            const deltaSinceLast = Math.max(0, now - lastUpdatedRaw);

            // -------------------------
            // 4️⃣ Process zones
            // -------------------------
            for (const zoneDoc of zonesSnap.docs) {
                const zone = zoneDoc.data();
                const zoneId = zone.zoneId || zoneDoc.id;

                const zoneLat = zone.lat;
                const zoneLon = zone.lon;
                const radius = zone.radius; // meters

                if ([zoneLat, zoneLon, radius].some(v => typeof v !== "number")) continue;

                // 🔥 OPTION A FIX: convert KM → METERS
                const distanceKm = getDistance(lat, lon, zoneLat, zoneLon);
                const distance = distanceKm * 1000;

                const { risk, level, reasons } = calculateRisk({
                    speed: speedMS,
                    hour: new Date().getHours(),
                    distance,
                    radius,
                    timeInZone: timeInZoneRaw[zoneId] || 0
                });

                const prevState = prevZones[zoneId] || "OUTSIDE";
                let state = prevState;

                if (distance <= radius) {
                    state = "INSIDE";
                    delete exitCandidates[zoneId];
                } else if (distance <= radius + APPROACH_BUFFER) {
                    state = "APPROACHING";
                    delete exitCandidates[zoneId];
                } else if (prevState === "INSIDE" || prevState === "PROLONGED") {
                    const candidateTs = exitCandidates[zoneId] || now;
                    if (!exitCandidates[zoneId]) {
                        exitCandidates[zoneId] = candidateTs;
                    } else if (now - candidateTs >= EXIT_CONFIRM_MS) {
                        state = "EXITED";
                        delete exitCandidates[zoneId];
                    }
                } else {
                    state = "OUTSIDE";
                    delete exitCandidates[zoneId];
                }

                if (prevState === "INSIDE" || prevState === "PROLONGED") {
                    timeInZoneRaw[zoneId] += deltaSinceLast;
                }

                if ((state === "INSIDE" || state === "PROLONGED") && timeInZoneRaw[zoneId] >= PROLONGED_MS) {
                    state = "PROLONGED";
                }

                console.log(`Zone ${zone.name} dist=${Math.round(distance)}m radius=${radius}m ${prevState}→${state}`);

                const shouldAlert =
                    risk >= 70 ||
                    now - (lastAlerts[zoneId] || 0) > COOLDOWN ||
                    state !== prevState;

                if (shouldAlert && state !== "OUTSIDE") {
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
                        durationInZone: formatDuration(timeInZoneRaw[zoneId] || 0),
                        zoneLat,
                        zoneLon,
                        childLat: lat,
                        childLon: lon,
                        readStatus: false
                    });

                    newAlertTimes[zoneId] = now;
                }

                newStates[zoneId] = state;
            }

            // -------------------------
            // 5️⃣ Persist
            // -------------------------
            const timeInZoneFormatted = {};
            for (const zid in timeInZoneRaw) {
                timeInZoneFormatted[zid] = formatDuration(timeInZoneRaw[zid]);
            }

            await childRef.set({
                parentId,
                lat,
                lon,
                speedMS,
                speedUnit: "m/s",
                lastUpdatedRaw: now,
                zoneStates: newStates,
                lastAlertTimes: newAlertTimes,
                timeInZone: timeInZoneFormatted,
                timeInZoneRaw,
                exitCandidates
            }, { merge: true });

        } catch (err) {
            console.error("❌ Background error:", err);
        }
    })();
});

module.exports = router;