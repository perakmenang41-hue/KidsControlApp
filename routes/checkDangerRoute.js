const express = require("express");
const router = express.Router();
const admin = require("../firebase"); // initialized Firebase Admin

// Haversine distance calculation
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000; // meters
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) ** 2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// Send notification helper
async function sendNotification(parentId, title, body) {
  const parentRef = admin.firestore().collection("Parent_registered").doc(parentId);
  const parentDoc = await parentRef.get();
  if (!parentDoc.exists) return;

  const fcmToken = parentDoc.data()?.fcmToken;
  if (!fcmToken) return;

  await admin.messaging().send({
    token: fcmToken,
    notification: { title, body },
  });
}

// Check child location against danger zones
router.post("/check-child", async (req, res) => {
  try {
    const { parentId, childUID } = req.body;

    if (!parentId || !childUID) {
      return res.status(400).json({ success: false, message: "Missing parentId or childUID" });
    }

    // Get child location
    const childLocRef = admin.firestore().collection("child_locations").doc(childUID);
    const childLocDoc = await childLocRef.get();
    if (!childLocDoc.exists) {
      return res.status(404).json({ success: false, message: "Child location not found" });
    }

    const childLoc = childLocDoc.data();

    // Get all danger zones for this parent
    const dangerZonesSnap = await admin.firestore()
      .collection("Parent_registered")
      .doc(parentId)
      .collection("dangerZones")
      .get();

    if (dangerZonesSnap.empty) {
      return res.status(200).json({ success: true, message: "No danger zones set for this parent." });
    }

    const warningDistance = 50; // meters
    const triggeredZones = [];

    for (const zoneDoc of dangerZonesSnap.docs) {
      const zone = zoneDoc.data();
      const distance = getDistance(childLoc.latitude, childLoc.longitude, zone.lat, zone.lon);

      if (distance <= zone.radius) {
        await sendNotification(parentId, "⚠️ Danger Zone Alert!", `Your child is already in ${zone.name}`);
        triggeredZones.push({ zone: zone.name, status: "inside" });
      } else if (distance <= zone.radius + warningDistance) {
        await sendNotification(parentId, "⚠️ Approaching Danger Zone", `Your child is approaching ${zone.name}`);
        triggeredZones.push({ zone: zone.name, status: "approaching" });
      }
    }

    res.status(200).json({
      success: true,
      message: "Checked danger zones",
      triggeredZones
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Server error", error: err.message });
  }
});

module.exports = router;
