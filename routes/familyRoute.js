// routes/update-location.js  (replace contents of your existing file)
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

    // respond fast, process in background
    res.json({
        success: true,
        message: "Location received, processing in background"
    });

    (async () => {
        try {
            const now = Date.now();

            // -------------------------
            // 1️⃣ Calculate speed (m/s) using last saved in child_locations
            // -------------------------
            const lastRef = db.collection("child_locations").doc(childUID);
            const lastDoc = await lastRef.get();
            let speedMS = 0;

            if (lastDoc.exists) {
                const last = lastDoc.data();
                const lastTs = last.timestamp || now;
                const dt = Math.max(1, (now - lastTs) / 1000); // seconds
                const dist = getDistance(last.lat, last.lon, lat, lon); // meters
                speedMS = dist / dt;
            }

            // persist latest simple location for speed calc and debugging
            await lastRef.set({ lat, lon, timestamp: now }, { merge: true });

            // -------------------------
            // 2️⃣ Load previous child_position doc (zone states, timers, etc.)
            // -------------------------
            const childRef = db.collection("child_position").doc(childUID);
            const childDoc = await childRef.get();
            const childData = childDoc.exists ? childDoc.data() : {};

            const prevZones = childData?.zoneStates || {};           // { zoneId: "INSIDE" }
            const lastAlerts = childData?.lastAlertTimes || {};      // { zoneId: timestamp }
            const timeInZoneRaw = childData?.timeInZoneRaw || {};    // { zoneId: ms }
            const exitCandidates = childData?.exitCandidates || {};  // { zoneId: candidateTs }
            const lastUpdatedRaw = childData?.lastUpdatedRaw || now; // timestamp of previous processing

            // -------------------------
            // 3️⃣ Load zones for this parent
            // -------------------------
            const zonesSnap = await db.collection("Parent_registered")
                .doc(parentId)
                .collection("dangerZones")
                .get();

            // thresholds & config
            const APPROACH_BUFFER = 20;            // meters (your choice)
            const PROLONGED_MS = 5 * 60 * 1000;   // 5 minutes
            const EXIT_CONFIRM_MS = 3 * 1000;     // 3 seconds to confirm exit (debounce)
            const WARNING = 50;                   // notification proximity buffer (unchanged)
            const COOLDOWN = 10 * 60 * 1000;      // 10 minutes alert cooldown

            const newStates = {};
            const newAlertTimes = { ...lastAlerts };

            // ensure timeInZoneRaw has entries for all zones
            for (const z of zonesSnap.docs) {
                const zid = z.id;
                if (!timeInZoneRaw[zid]) timeInZoneRaw[zid] = 0;
            }

            // compute delta since last update for accumulation
            const deltaSinceLast = Math.max(0, now - (lastUpdatedRaw || now));

            // -------------------------
            // 4️⃣ Process each zone
            // -------------------------
            for (const zoneDoc of zonesSnap.docs) {
                const zone = zoneDoc.data();
                const zoneId = zone.zoneId || zoneDoc.id;
                if (!timeInZoneRaw[zoneId]) timeInZoneRaw[zoneId] = 0;

                // Validate zone fields
                const zoneLat = zone.lat;
                const zoneLon = zone.lon;
                const radius = zone.radius;
                if (typeof zoneLat !== "number" || typeof zoneLon !== "number" || typeof radius !== "number") {
                    console.warn(`Skipping malformed zone ${zoneDoc.id}`);
                    continue;
                }

                const distance = getDistance(lat, lon, zoneLat, zoneLon); // meters

                // risk calculation (AI)
                const { risk, level, reasons } = calculateRisk({
                    speed: speedMS,
                    hour: new Date().getHours(),
                    distance,
                    radius,
                    timeInZone: timeInZoneRaw[zoneId] || 0
                });

                // Determine state with robust ordering and exit confirmation
                // Default to previous state so we don't flip-flop unnecessarily
                const prevState = prevZones[zoneId] || "OUTSIDE";
                let state = prevState;

                // 1) INSIDE
                if (distance <= radius) {
                    state = "INSIDE";
                    // clear any exit candidate
                    if (exitCandidates[zoneId]) delete exitCandidates[zoneId];
                }
                // 2) APPROACHING
                else if (distance <= radius + APPROACH_BUFFER) {
                    state = "APPROACHING";
                    if (exitCandidates[zoneId]) delete exitCandidates[zoneId];
                }
                // 3) Candidate EXIT: previously INSIDE but now outside approach buffer
                else if (prevState === "INSIDE" || prevState === "PROLONGED") {
                    const candidateTs = exitCandidates[zoneId] || now;
                    if (!exitCandidates[zoneId]) {
                        // mark candidate, don't change state yet (avoid immediate EXIT)
                        exitCandidates[zoneId] = candidateTs;
                        state = prevState; // keep previous until confirmed
                    } else {
                        // confirm exit only after EXIT_CONFIRM_MS elapsed
                        if (now - candidateTs >= EXIT_CONFIRM_MS) {
                            state = "EXITED";
                            delete exitCandidates[zoneId];
                        } else {
                            // within confirmation window: keep previous state
                            state = prevState;
                        }
                    }
                }
                // 4) OUTSIDE (default)
                else {
                    state = "OUTSIDE";
                    if (exitCandidates[zoneId]) delete exitCandidates[zoneId];
                }

                // Accumulate timeInZoneRaw correctly:
                // If previously INSIDE (or PROLONGED), they were inside during the last interval
                if (prevState === "INSIDE" || prevState === "PROLONGED") {
                    timeInZoneRaw[zoneId] = (timeInZoneRaw[zoneId] || 0) + deltaSinceLast;
                }

                // If new state becomes INSIDE and prev wasn't, we start counting on next cycles (we don't add retro delta)
                // Determine PROLONGED
                if ((state === "INSIDE" || state === "PROLONGED") && (timeInZoneRaw[zoneId] >= PROLONGED_MS)) {
                    state = "PROLONGED";
                }

                // Log for debugging
                console.log(`Zone ${zoneId} (${zone.name}) dist=${Math.round(distance)}m r=${radius} prev=${prevState} -> new=${state} timeInZoneMs=${timeInZoneRaw[zoneId]}`);

                // Decide whether to alert
                const shouldAlert =
                    risk >= 70 || // high AI risk
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
            } // end zones loop

            // -------------------------
            // 5️⃣ Persist results
            // -------------------------
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
                timeInZoneRaw,
                exitCandidates
            }, { merge: true });

        } catch (err) {
            console.error("❌ Background error:", err);
        }
    })();
});

module.exports = router;
