// routes/familyRoute.js
const express = require("express");
const router = express.Router();
const { getDistanceMeters } = require("../utils/distance");
const db = require("../db"); // your database module
const admin = require("../firebaseAdmin"); // for FCM notifications

// Constants
const APPROACH_BUFFER = 10;     // meters
const HARD_EXIT_BUFFER = 30;    // meters
const EXIT_CONFIRM_MS = 500;    // ms

// In-memory state
let zoneStates = {}; // { [childId_zoneId]: { state, lastTs } }
let exitCandidates = {}; 
let timeInZoneRaw = {}; // { [childId_zoneId]: durationMs }

async function sendNotificationToParent(childId, zoneId, state, distance) {
  try {
    // Fetch parent FCM token from DB
    const { rows } = await db.query("SELECT parent_fcm_token FROM children WHERE id = $1", [childId]);
    const token = rows[0]?.parent_fcm_token;
    if (!token) return;

    let title = "Zone Alert";
    let body = `Child ${state} zone ${zoneId} (${Math.round(distance)}m)`;

    await admin.messaging().send({
      token,
      notification: { title, body },
    });

    console.log(`Notification sent: Child ${childId} is ${state} zone ${zoneId}`);
  } catch (err) {
    console.error("FCM send error:", err);
  }
}

router.post("/checkZones", async (req, res) => {
  try {
    const { childId, lat, lon } = req.body;
    const now = Date.now();
    const results = [];

    // Fetch zones from DB
    const { rows: zones } = await db.query("SELECT * FROM zones");

    for (const zone of zones) {
      const { id: zoneId, centerLat, centerLon, radius, isDangerZone } = zone;

      const distance = getDistanceMeters(lat, lon, centerLat, centerLon);
      const key = `${childId}_${zoneId}`;
      const prevState = zoneStates[key]?.state || "OUTSIDE";
      let state = prevState;

      // ===== STATE MACHINE =====
      if (distance <= radius) {
        state = "INSIDE";
        delete exitCandidates[key];
      } else if (distance <= radius + APPROACH_BUFFER) {
        state = "APPROACHING";
        delete exitCandidates[key];
      } else if (distance > radius + HARD_EXIT_BUFFER) {
        state = "OUTSIDE";
        delete exitCandidates[key];
      } else if (prevState === "INSIDE" || prevState === "PROLONGED") {
        const candidateTs = exitCandidates[key] || now;
        if (!exitCandidates[key]) {
          exitCandidates[key] = candidateTs;
          state = prevState;
        } else if (now - candidateTs >= EXIT_CONFIRM_MS) {
          state = "EXITED";
          delete exitCandidates[key];
        } else {
          state = prevState;
        }
      } else {
        state = "OUTSIDE";
      }

      // ===== TIME ACCUMULATION =====
      if (prevState === "INSIDE" || prevState === "PROLONGED") {
        timeInZoneRaw[key] = (timeInZoneRaw[key] || 0) + (now - (zoneStates[key]?.lastTs || now));
      }

      // ===== SEND NOTIFICATION ON ANY STATE CHANGE =====
      if (isDangerZone && state !== prevState) {
        await sendNotificationToParent(childId, zoneId, state, distance);
      }

      // ===== SAVE STATE =====
      zoneStates[key] = { state, lastTs: now };

      // ===== UPDATE DATABASE =====
      await db.query(
        "UPDATE child_position SET zone_state = $1, last_lat = $2, last_lon = $3 WHERE child_id = $4 AND zone_id = $5",
        [state, lat, lon, childId, zoneId]
      );

      console.log(
        `Child ${childId} Zone ${zoneId} dist=${distance.toFixed(1)}m radius=${radius} prev=${prevState} → ${state}`
      );

      results.push({
        zoneId,
        state,
        distance: Math.round(distance),
        durationMs: timeInZoneRaw[key] || 0,
      });
    }

    res.json({ success: true, zones: results });
  } catch (err) {
    console.error("Zone check error:", err);
    res.status(500).json({ error: "Zone processing failed" });
  }
});

module.exports = router;
