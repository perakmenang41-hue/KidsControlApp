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

        if (!parentId || !name || typeof lat !== "number" || typeof lon !== "number" || typeof radius !== "number") {
            return res.status(400).json({ success: false, message: "Invalid payload" });
        }

        // Find parent document
        const parentSnap = await db
            .collection("Parent_registered")
            .where("parentId", "==", parentId)
            .limit(1)
            .get();

        if (parentSnap.empty) {
            return res.status(404).json({ success: false, message: "Parent not found" });
        }

        const parentDocId = parentSnap.docs[0].id;

        // Add danger zone under parent
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

        // Initialize zone for all children of this parent
        const childrenSnap = await db.collection("child_locations")
            .where("parentId", "==", parentId)
            .get();

        for (const childDoc of childrenSnap.docs) {
            const childRef = childDoc.ref;

            await childRef.set({
                zoneStates: { [zoneRef.id]: "OUTSIDE" },
                timeInZone: { [zoneRef.id]: "00:00:00:00" },
                timeInZoneRaw: { [zoneRef.id]: 0 }
            }, { merge: true });

            console.log(`✅ Zone ${zoneRef.id} added to child ${childDoc.id}`);
        }

        // Respond to client
        res.status(200).json({
            success: true,
            message: "Danger zone added successfully",
            zoneId: zoneRef.id,
            parentFirestoreDocId: parentDocId
        });

        // Background notifications
        (async () => {
            try {
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
        res.status(500).json({ success: false, message: "Server error", error: err.message });
    }
});

// ------------------------
// Delete a danger zone
// ------------------------
router.delete("/delete/:zoneId", async (req, res) => {
    try {
        const { zoneId } = req.params;
        const { parentId } = req.body;

        if (!zoneId || !parentId) {
            return res.status(400).json({ success: false, message: "Missing zoneId or parentId" });
        }

        // Find parent document
        const parentSnap = await db
            .collection("Parent_registered")
            .where("parentId", "==", parentId)
            .limit(1)
            .get();

        if (parentSnap.empty) {
            return res.status(404).json({ success: false, message: "Parent not found" });
        }

        const parentDocId = parentSnap.docs[0].id;

        // Delete danger zone from parent
        await db
            .collection("Parent_registered")
            .doc(parentDocId)
            .collection("dangerZones")
            .doc(zoneId)
            .delete();

        console.log(`✅ Danger zone ${zoneId} deleted from parent ${parentId}`);

        // Remove zone references from all children
        const childrenSnap = await db
            .collection("child_locations")
            .where("parentId", "==", parentId)
            .get();

        for (const childDoc of childrenSnap.docs) {
            const childRef = childDoc.ref;
            const childData = childDoc.data();

            const updatedZoneStates = { ...childData.zoneStates };
            const updatedTimeInZone = { ...childData.timeInZone };
            const updatedTimeInZoneRaw = { ...childData.timeInZoneRaw };

            delete updatedZoneStates[zoneId];
            delete updatedTimeInZone[zoneId];
            delete updatedTimeInZoneRaw[zoneId];

            await childRef.set({
                zoneStates: updatedZoneStates,
                timeInZone: updatedTimeInZone,
                timeInZoneRaw: updatedTimeInZoneRaw
            }, { merge: true });

            console.log(`✅ Zone ${zoneId} removed from child ${childDoc.id}`);
        }

        res.status(200).json({ success: true, message: "Zone deleted from parent and all children" });

    } catch (err) {
        console.error("Delete Danger Zone Error:", err);
        res.status(500).json({ success: false, message: "Server error", error: err.message });
    }
});

module.exports = router;
