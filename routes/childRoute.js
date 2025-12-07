// routes/childRoute.js
const express = require("express");
const router = express.Router();
const admin = require("../firebase");
const { getDistance, formatDuration } = require("../utils/geolocation");
const { sendNotification } = require("../utils/notification");
const calculateRisk = require("../aiRiskEngine");

// Update child location + AI risk check
router.post("/update-location", async (req, res) => {
  const { parentId, childId, lat, lon } = req.body;
  if (!parentId || !childId || typeof lat !== "number" || typeof lon !== "number")
    return res.status(400).json({ success:false, message:"Invalid payload" });

  res.json({ success:true, message:"Location received" });

  (async () => {
    try {
      const now = Date.now();
      const childRef = admin.firestore().collection("children").doc(childId);
      const childDoc = await childRef.get();
      const prevData = childDoc.exists ? childDoc.data() : {};

      // 1️⃣ Speed calculation
      let speed = 0;
      if (prevData.lastLocation) {
        const dt = Math.max(1, (now - prevData.lastLocation.timestamp)/1000);
        speed = getDistance(prevData.lastLocation.lat, prevData.lastLocation.lon, lat, lon) / dt;
      }

      // 2️⃣ Get parent danger zones
      const zonesSnap = await admin.firestore().collection("parents").doc(parentId).collection("dangerZones").get();
      const WARNING = 50;
      const COOLDOWN = 10 * 60 * 1000;
      const PROLONGED = 10 * 60 * 1000;

      const zoneStates = {};
      const timeInZoneRaw = prevData.timeInZoneRaw || {};
      const lastAlerts = prevData.lastAlertTimes || {};

      for (const zoneDoc of zonesSnap.docs) {
        const zone = zoneDoc.data();
        const zoneId = zoneDoc.id;
        if (!timeInZoneRaw[zoneId]) timeInZoneRaw[zoneId] = 0;

        const distance = getDistance(lat, lon, zone.lat, zone.lon);
        const { risk, level, reasons } = calculateRisk({ speed, hour: new Date().getHours(), distance, radius: zone.radius, timeInZone: timeInZoneRaw[zoneId] });

        let state = "OUTSIDE";
        if (distance <= zone.radius) state = "INSIDE";
        else if (distance <= zone.radius + WARNING) state = "APPROACHING";

        if (state === "INSIDE") timeInZoneRaw[zoneId] += now - (prevData.lastUpdatedRaw || now);

        const canAlert = risk >= 70 || now - (lastAlerts[zoneId] || 0) > COOLDOWN || state !== (prevData.zoneStates?.[zoneId] || "OUTSIDE");

        if (canAlert && state !== "OUTSIDE") {
          await sendNotification(parentId, "⚠️ Child Safety Alert", `Child ${state} at ${zone.name}`);
          await admin.firestore().collection("notifications_log").doc(parentId).set({
            notifications: admin.firestore.FieldValue.arrayUnion({
              childId, zoneId, type: state, level, risk, reasons: reasons.join(","), duration: formatDuration(timeInZoneRaw[zoneId]),
              zoneLat: zone.lat, zoneLon: zone.lon, childLat: lat, childLon: lon, read: false, timestamp: formatDuration(now)
            })
          }, { merge:true });

          lastAlerts[zoneId] = now;
        }

        zoneStates[zoneId] = state;
      }

      // 3️⃣ Save child data
      await childRef.set({
        lastLocation: { lat, lon, timestamp: now },
        speed,
        zoneStates,
        lastAlertTimes: lastAlerts,
        timeInZoneRaw,
        lastUpdatedRaw: now
      }, { merge:true });

    } catch (err) {
      console.error("Background error:", err);
    }
  })();
});

module.exports = router;
