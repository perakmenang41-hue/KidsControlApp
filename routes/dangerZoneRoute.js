// routes/dangerZoneRoute.js
const express = require("express");
const router = express.Router();
const admin = require("../firebase");
const { getDistance } = require("../utils/geolocation");
const { sendNotification } = require("../utils/notification");

const db = admin.firestore();

// ------------------------
// Add new danger zone
// ------------------------
router.post("/add", async (req, res) => {
    try {
        const { parentId, name, lat, lon, radius } = req.body;

        // Validate request
        if (!parentId || !name || typeof lat !== "number" || typeof lon !== "number" || typeof radius !== "number") {
            return res.status(400).json({
                success: false,
                message: "Invalid payload"
            });
        }

        // Find parent document in Firestore
        const parentSnap = await db
            .collection("Parent_registered")
            .where("parentId", "==", parentId)
            .limit(1)
            .get();

        if (parentSnap.empty) {
            return res.status(404).json({
                success: false,
                message: "Parent not found in Parent_registered"
            });
        }

        const parentDocId = parentSnap.docs[0].id;

        // Add danger zone under correct subcollection
        const zoneRef = await db
            .collection("Parent_registered")
            .doc(parentDocId)
            .collection("dangerZones")
            .add({
                name,
                lat,
                lon,
                radius,
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });

        console.log("Danger Zone Created ID:", zoneRef.id);

        // ------------------------
        // ✅ Respond immediately to client
        // ------------------------
        res.status(200).json({
            success: true,
            message: "Danger zone added successfully",
            zoneId: zoneRef.id,
            parentFirestoreDocId: parentDocId
        });

        // ------------------------
        // Process notifications asynchronously (non-blocking)
        // ------------------------
        (async () => {
            try {
                const childrenSnap = await db.collection("registered_users").get();
                const warningDistance = 50; // meters

                for (const childDoc of childrenSnap.docs) {
                    const childUID = childDoc.id;
                    const locationDoc = await db.collection("child_locations").doc(childUID).get();
                    if (!locationDoc.exists) continue;

                    const { lat: childLat, lon: childLon } = locationDoc.data();
                    const distance = getDistance(childLat, childLon, lat, lon);

                    if (distance <= radius) {
                        await sendNotification(parentId, "⚠️ Danger Zone Alert!", `Your child is inside ${name}`);
                    } else if (distance <= radius + warningDistance) {
                        await sendNotification(parentId, "⚠️ Approaching Danger Zone", `Your child is approaching ${name}`);
                    }
                }
            } catch (err) {
                console.error("Background notification error:", err);
            }
        })();

    } catch (err) {
        console.error("Add Danger Zone Error:", err);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: err.message
        });
    }
});

module.exports = router;
