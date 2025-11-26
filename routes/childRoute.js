// routes/childRoute.js
const express = require("express");
const router = express.Router();
const admin = require("../firebase"); // initialized Firebase Admin

// Haversine distance
function getDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000; // meters
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dLat/2)**2 + Math.cos(lat1*Math.PI/180)*Math.cos(lat2*Math.PI/180)*Math.sin(dLon/2)**2;
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

// Send notification helper
async function sendNotification(parentUID, title, body) {
  const parentRef = admin.firestore().collection("registered_users").doc(parentUID);
  const parentDoc = await parentRef.get();
  if (!parentDoc.exists) return;

  const fcmToken = parentDoc.data().fcmToken;
  if (!fcmToken) return;

  const message = { token: fcmToken, notification: { title, body } };
  await admin.messaging().send(message);
}

// Update child location
router.post("/update-location", async (req, res) => {
  try {
    const { childUID, lat, lon, parentUID, name } = req.body;
    if (!childUID || typeof lat !== "number" || typeof lon !== "number") {
      return res.status(400).json({ success:false, message:"Invalid payload" });
    }

    const childRef = admin.firestore().collection("child_locations").doc(childUID);

    await childRef.set({
      parentUID,
      name,
      lat,
      lon,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Get all danger zones of parent
    const zonesSnapshot = await admin.firestore()
      .collection("dangerZones")
      .where("parentUID", "==", parentUID)
      .get();

    const warningDistance = 50; // meters

    for (const zoneDoc of zonesSnapshot.docs) {
      const zone = zoneDoc.data();
      const distance = getDistance(lat, lon, zone.lat, zone.lon);

      if (distance <= zone.radius) {
        await sendNotification(parentUID, "⚠️ Danger Zone Alert!", `Your child entered ${zone.name}`);
      } else if (distance <= zone.radius + warningDistance) {
        await sendNotification(parentUID, "⚠️ Approaching Danger Zone", `Your child is approaching ${zone.name}`);
      }
    }

    res.json({ success:true, message:"Child location updated" });

  } catch (err) {
    console.error(err);
    res.status(500).json({ success:false, message:"Server error", error: err.message });
  }
});

module.exports = router;
