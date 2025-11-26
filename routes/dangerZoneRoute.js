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
// ADD NEW DANGER ZONE + ALERT CHILDREN
// ===============================
router.post("/add", async (req, res) => {
  try {
    const { parentId, name, lat, lon, radius } = req.body;

    // Validate input
    if (!parentId || !name || typeof lat !== "number" || typeof lon !== "number" || typeof radius !== "number") {
      return res.status(400).json({ success: false, message: "Invalid payload" });
    }

    // Add danger zone to parent
    const zoneRef = await admin.firestore()
      .collection("Parent_registered")
      .doc(parentId)
      .collection("dangerZones")
      .add({ name, lat, lon, radius });

    // Get all registered children
    const childrenSnap = await admin.firestore()
      .collection("registered_users")
      .get();

    const warningDistance = 50; // meters

    // Check each child's latest location
    for (const childDoc of childrenSnap.docs) {
      const childUID = childDoc.id;

      // Get latest child location
      const locationDoc = await admin.firestore()
        .collection("child_locations")
        .doc(childUID)
        .get();

      if (!locationDoc.exists) continue;

      const { lat: childLat, lon: childLon } = locationDoc.data();

      const distance = getDistance(childLat, childLon, lat, lon);

      if (distance <= radius) {
        await sendNotification(parentId, "⚠️ Danger Zone Alert!", `Your child is inside ${name}`);
      } else if (distance <= radius + warningDistance) {
        await sendNotification(parentId, "⚠️ Approaching Danger Zone", `Your child is approaching ${name}`);
      }
    }

    return res.status(200).json({
      success: true,
      message: "Danger zone added and alerts sent if needed",
      zoneId: zoneRef.id
    });

  } catch (error) {
    console.error("Add Danger Zone Error:", error);
    return res.status(500).json({ success: false, message: "Server error", error: error.message });
  }
});

module.exports = router;
