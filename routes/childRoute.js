// routes/childRoute.js
const express = require('express');
const router = express.Router();
const Child = require('../models/Child');

// Update child location (or create if not exist)
router.post('/update-location', async (req, res) => {
    const { childId, lat, lng, parentId, name } = req.body;

    if (!childId || typeof lat !== 'number' || typeof lng !== 'number') {
        return res.status(400).json({ success: false, message: 'Invalid payload' });
    }

    try {
        const child = await Child.findByIdAndUpdate(
            childId,
            { lat, lng, updatedAt: Date.now(), parentId, name },
            { new: true, upsert: true, setDefaultsOnInsert: true }
        );

        res.json({ success: true, message: "Child location updated", child });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "Server error" });
    }
});

module.exports = router;
