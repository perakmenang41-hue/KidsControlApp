const express = require("express");
const router = express.Router();
const admin = require("../firebase");

// Save FCM token for parent
router.post("/save-token", async (req, res) => {
  try {
    const { parentId, fcmToken } = req.body;

    if (!parentId || !fcmToken) {
      return res.status(400).json({ success: false, message: "parentId and fcmToken are required" });
    }

    const parentRef = admin.firestore().collection("Parent_registered").doc(parentId);
    const parentDoc = await parentRef.get();

    if (!parentDoc.exists) {
      return res.status(404).json({ success: false, message: "Parent not found" });
    }

    await parentRef.update({ fcmToken });

    return res.status(200).json({ success: true, message: "FCM token saved successfully" });

  } catch (error) {
    console.error("Save FCM Token Error:", error);
    return res.status(500).json({ success: false, message: "Server error", error: error.message });
  }
});

module.exports = router;
