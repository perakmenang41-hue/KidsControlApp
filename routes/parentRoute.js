// routes/parentRoute.js
const express = require("express");
const router = express.Router();
const admin = require("../firebase");

// Save parent FCM token
router.post("/save-token", async (req, res) => {
  try {
    const { parentId, fcmToken } = req.body;
    if (!parentId || !fcmToken) return res.status(400).json({ success:false, message:"Missing parentId or fcmToken" });

    const parentRef = admin.firestore().collection("parents").doc(parentId);
    const parentDoc = await parentRef.get();
    if (!parentDoc.exists) return res.status(404).json({ success:false, message:"Parent not found" });

    await parentRef.set({ fcmToken }, { merge: true });
    return res.json({ success:true, message:"FCM token saved" });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ success:false, message:"Server error", error: err.message });
  }
});

module.exports = router;
