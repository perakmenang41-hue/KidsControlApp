const express = require("express");
const router = express.Router();
const admin = require("../firebase"); // Firebase Admin SDK

// ===============================
// Haversine Distance Calculator
// ===============================
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000; // meters
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) *
      Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) ** 2;

  return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

// ===============================
// Push Notification Helper
// ===============================
async function sendNotification(parentId, title, body) {
  try {
    const parentRef = admin.firestore()
      .collection("Parent_registered")
      .doc(parentId);

    const parentDoc = await parentRef.get();
    if (!parentDoc.exists) return;

    const fcmToken = parentDoc.data()?.fcmToken;
    if (!fcmToken) return;

    await admin.messaging().send({
      token: fcmToken,
      notification: { title, body },
    });

  } catch (error) {
    console.error("Notification error:", error);
  }
}

// ===============================
// UPDATE CHILD LOCATION
// ===============================
router.post("/update-location", async (req, res) => {
  try {
    const { parentId, childUID, lat, lon } = req.body;

    if (!parentId || !childUID || typeof lat !== "number" || typeof lon !== "number") {
      return res.status(400).json({ success: false, message: "Invalid payload" });
    }

    // -------------------------------
    // 1. Check parent exists
    // -------------------------------
    const parentRef = admin.firestore().collection("Parent_registered").doc(parentId);
    const parentDoc = await parentRef.get();
    if (!parentDoc.exists) {
      return res.status(404).json({ success: false, message: "Parent not found" });
    }

    // -------------------------------
    // 2. Get child data
    // -------------------------------
    const childRef = admin.firestore().collection("registered_users").doc(childUID);
    const childDoc = await childRef.get();
    if (!childDoc.exists) {
      return res.status(404).json({ success: false, message: "Child not found in registered_users" });
    }
    const childData = childDoc.data();
    const { name, age } = childData;

    // -------------------------------
    // 3. Get last alerted zones
    // -------------------------------
    const childLocationRef = admin.firestore().collection("child_locations").doc(childUID);
    const childLocationDoc = await childLocationRef.get();
    const lastAlertedZones = childLocationDoc.exists
      ? childLocationDoc.data()?.lastAlertedZones || []
      : [];

    // -------------------------------
    // 4. Get parent danger zones
    // -------------------------------
    const dangerZonesSnap = await parentRef.collection("dangerZones").get();
    const dangerZonesArray = [];
    const warningDistance = 50; // meters

    const newAlertedZones = [...lastAlertedZones]; // copy previous alerts

    for (const zoneDoc of dangerZonesSnap.docs) {
      const zone = zoneDoc.data() || {};
      const zoneId = zone.zoneId || zoneDoc.id;
      const zoneName = zone.name || "Unknown Zone";
      const D_lat = zone.lat ?? 0;
      const D_lon = zone.lon ?? 0;
      const radius = zone.radius ?? 0;

      dangerZonesArray.push({ zoneId, name: zoneName, D_lat, D_lon, radius });

      // Skip if this child already alerted for this zone
      if (lastAlertedZones.includes(zoneId)) continue;

      // Calculate distance
      if (typeof D_lat === "number" && typeof D_lon === "number" && typeof radius === "number") {
        const distance = getDistance(lat, lon, D_lat, D_lon);

        if (distance <= radius) {
          await sendNotification(parentId, "⚠️ Danger Zone Alert!", `Your child is inside ${zoneName}`);
          newAlertedZones.push(zoneId);
        } else if (distance <= radius + warningDistance) {
          await sendNotification(parentId, "⚠️ Approaching Danger Zone", `Your child is approaching ${zoneName}`);
          newAlertedZones.push(zoneId);
        }
      }
    }

    // -------------------------------
    // 5. Update family collection
    // -------------------------------
    await admin.firestore().collection("family").doc(childUID).set({
      parentId,
      name,
      age,
      lat,
      lon,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      dangerZones: dangerZonesArray
    }, { merge: true });

    // -------------------------------
    // 6. Update child location and lastAlertedZones
    // -------------------------------
    await childLocationRef.set({
      lat,
      lon,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      lastAlertedZones: newAlertedZones
    }, { merge: true });

    // -------------------------------
    // 7. Success response
    // -------------------------------
    return res.status(200).json({
      success: true,
      message: "Child location updated successfully with alerts tracked"
    });

  } catch (error) {
    console.error("Update-Location Error:", error);
    return res.status(500).json({
      success: false,
      message: "Server error",
      error: error.message
    });
  }
});

module.exports = router;
