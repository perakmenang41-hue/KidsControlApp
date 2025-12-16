// routes/familyRoute.js
// FULL FIXED VERSION — correct distance, fast exit, hard exit

const express = require("express");
const router = express.Router();

// ===== CONFIG =====
const APPROACH_BUFFER = 20; // meters
const EXIT_CONFIRM_MS = 500; // 0.5s debounce (mobile friendly)
const HARD_EXIT_BUFFER = 30; // meters beyond radius = instant OUTSIDE

// ===== STATE MEMORY =====
const zoneStates = {};           // { zoneId: { state, lastTs } }
const timeInZoneRaw = {};        // { zoneId: ms }
const exitCandidates = {};       // { zoneId: timestamp }

// ===== DISTANCE (HAVERSINE, METERS) =====
function getDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000; // meters
  const toRad = (v) => (v * Math.PI) / 180;

  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) ** 2;

  return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ===== MAIN ROUTE =====
router.post("/update-location", async (req, res) => {
  try {
    const { lat, lon, zones } = req.body;
    const now = Date.now();

    const results = [];

    for (const zone of zones) {
      const { id: zoneId, centerLat, centerLon, radius } = zone;

      const distance = getDistanceMeters(lat, lon, centerLat, centerLon);

      if (!zoneStates[zoneId]) {
        zoneStates[zoneId] = { state: "OUTSIDE", lastTs: now };
        timeInZoneRaw[zoneId] = 0;
      }

      const prevState = zoneStates[zoneId].state;
      const delta = now - zoneStates[zoneId].lastTs;
      let state = prevState;

      // ===== STATE MACHINE =====
      if (distance <= radius) {
        state = "INSIDE";
        delete exitCandidates[zoneId];

      } else if (distance <= radius + APPROACH_BUFFER) {
        state = "APPROACHING";
        delete exitCandidates[zoneId];

      } else if (distance > radius + HARD_EXIT_BUFFER) {
        state = "OUTSIDE";
        delete exitCandidates[zoneId];

      } else if (prevState === "INSIDE" || prevState === "PROLONGED") {
        const candidateTs = exitCandidates[zoneId] || now;

        if (!exitCandidates[zoneId]) {
          exitCandidates[zoneId] = candidateTs;
          state = prevState;
        } else if (now - candidateTs >= EXIT_CONFIRM_MS) {
          state = "EXITED";
          delete exitCandidates[zoneId];
        } else {
          state = prevState;
        }

      } else {
        state = "OUTSIDE";
      }

      // ===== TIME ACCUMULATION =====
      if (prevState === "INSIDE" || prevState === "PROLONGED") {
        timeInZoneRaw[zoneId] += delta;
      }

      // ===== SAVE =====
      zoneStates[zoneId] = { state, lastTs: now };

      console.log(
        `Zone ${zoneId} dist=${distance.toFixed(1)}m radius=${radius} prev=${prevState} → ${state}`
      );

      results.push({
        zoneId,
        state,
        distance: Math.round(distance),
        durationMs: timeInZoneRaw[zoneId],
      });
    }

    res.json({ success: true, zones: results });

  } catch (err) {
    console.error("Zone check error:", err);
    res.status(500).json({ error: "Zone processing failed" });
  }
});

module.exports = router;
