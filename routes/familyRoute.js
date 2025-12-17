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
// Update Child Location
// ===============================
router.post("/update-location", async (req, res) => {
  const { parentId, childUID, lat, lon } = req.body;

  if (!parentId || !childUID || typeof lat !== "number" || typeof lon !== "number") {
    return res.status(400).json({ success: false, message: "Invalid payload" });
  }

  res.json({ success: true });

  (async () => {
    try {
      const now = Date.now();

      // -------------------------
      // Speed calculation
      // -------------------------
      const lastRef = db.collection("child_locations").doc(childUID);
      const lastDoc = await lastRef.get();
      let speedMS = 0;

      if (lastDoc.exists) {
        const last = lastDoc.data();
        const dt = Math.max(1, (now - (last.timestamp || now)) / 1000);
        speedMS = getDistance(last.lat, last.lon, lat, lon) / dt;
      }

      await lastRef.set({ lat, lon, timestamp: now }, { merge: true });

      // -------------------------
      // Load child state
      // -------------------------
      const childRef = db.collection("child_position").doc(childUID);
      const childDoc = await childRef.get();
      const data = childDoc.exists ? childDoc.data() : {};

      const prevZones = data.zoneStates || {};
      const exitCandidates = data.exitCandidates || {};
      const timeInZoneRaw = data.timeInZoneRaw || {};
      const lastAlerts = data.lastAlertTimes || {};
      const lastUpdatedRaw = data.lastUpdatedRaw || now;

      const delta = Math.max(0, now - lastUpdatedRaw);

      // -------------------------
      // Load zones
      // -------------------------
      const zonesSnap = await db.collection("Parent_registered")
        .doc(parentId)
        .collection("dangerZones")
        .get();

      const APPROACH_BUFFER = 20;
      const HARD_EXIT_BUFFER = 30;
      const EXIT_CONFIRM_MS = 3000;
      const PROLONGED_MS = 5 * 60 * 1000;
      const COOLDOWN = 10 * 60 * 1000;

      const newStates = {};
      const newAlertTimes = { ...lastAlerts };

      for (const zoneDoc of zonesSnap.docs) {
        const zone = zoneDoc.data();
        const zoneId = zone.zoneId || zoneDoc.id;

        const radius = zone.radius;
        const distance = getDistance(lat, lon, zone.lat, zone.lon);

        const prevState = prevZones[zoneId] || "OUTSIDE";
        let state = "OUTSIDE"; // default SAFE

        // ===============================
        // STATE MACHINE (FIXED)
        // ===============================

        // 1️⃣ Hard OUTSIDE always wins
        if (distance > radius + HARD_EXIT_BUFFER) {
          state = "OUTSIDE";
          delete exitCandidates[zoneId];
        }

        // 2️⃣ INSIDE
        else if (distance <= radius) {
          state = "INSIDE";
          delete exitCandidates[zoneId];
        }

        // 3️⃣ APPROACHING
        else if (distance <= radius + APPROACH_BUFFER) {
          state = "APPROACHING";
          delete exitCandidates[zoneId];
        }

        // 4️⃣ EXIT CONFIRMATION
        else if (prevState === "INSIDE" || prevState === "PROLONGED") {
          if (!exitCandidates[zoneId]) {
            exitCandidates[zoneId] = now;
            state = prevState;
          } else if (now - exitCandidates[zoneId] >= EXIT_CONFIRM_MS) {
            state = "EXITED";
            delete exitCandidates[zoneId];
          } else {
            state = prevState;
          }
        }

        // ===============================
        // Time accumulation
        // ===============================
        if (prevState === "INSIDE" || prevState === "PROLONGED") {
          timeInZoneRaw[zoneId] = (timeInZoneRaw[zoneId] || 0) + delta;
        }

        if (
          (state === "INSIDE" || state === "PROLONGED") &&
          (timeInZoneRaw[zoneId] || 0) >= PROLONGED_MS
        ) {
          state = "PROLONGED";
        }

        // ===============================
        // Alerts
        // ===============================
        const { risk, level, reasons } = calculateRisk({
          speed: speedMS,
          hour: new Date().getHours(),
          distance,
          radius,
          timeInZone: timeInZoneRaw[zoneId] || 0
        });

        if (
          state !== prevState &&
          now - (lastAlerts[zoneId] || 0) > COOLDOWN
        ) {
          await sendNotification(
            parentId,
            "⚠️ Child Safety Alert",
            `Child ${state} at ${zone.name}`
          );

          await logNotification({
            parentId,
            childUID,
            zoneId,
            type: state,
            level,
            riskScore: risk,
            reasons: reasons.join(", "),
            durationInZone: formatDuration(timeInZoneRaw[zoneId] || 0),
            readStatus: false
          });

          newAlertTimes[zoneId] = now;
        }

        newStates[zoneId] = state;

        console.log(
          `ZONE ${zoneId} dist=${Math.round(distance)}m prev=${prevState} → ${state}`
        );
      }

      // -------------------------
      // Save state
      // -------------------------
      await childRef.set({
        parentId,
        lat,
        lon,
        speedMS,
        lastUpdatedRaw: now,
        zoneStates: newStates,
        timeInZoneRaw,
        lastAlertTimes: newAlertTimes,
        exitCandidates
      }, { merge: true });

    } catch (err) {
      console.error("❌ update-location error:", err);
    }
  })();
});

module.exports = router;
