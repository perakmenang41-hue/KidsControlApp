// routes/dangerZoneRoute.js
const express = require('express');
const router = express.Router();
const DangerZone = require('../models/DangerZone');
const Child = require('../models/Child');
const { sendNotification } = require('../utils/notification');

// Haversine distance
function getDistanceFromLatLonInMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000; // meters
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2)**2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon/2)**2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

// Add Danger Zone
router.post('/add', async (req, res) => {
    const { parentId, name, latitude, longitude, radius } = req.body;

    if (!parentId || !name || typeof latitude !== 'number' || typeof longitude !== 'number' || typeof radius !== 'number') {
        return res.status(400).json({ success: false, message: 'Invalid payload' });
    }

    try {
        // Create danger zone
        const newZone = await DangerZone.create({ parentId, name, latitude, longitude, radius });

        // Check last known child location
        const child = await Child.findOne({ parentId }); // assumes 1 child per parent
        if (child && typeof child.lat === 'number' && typeof child.lng === 'number') {
            const distance = getDistanceFromLatLonInMeters(latitude, longitude, child.lat, child.lng);

            // Debug logs
            console.log('Child location:', child.lat, child.lng);
            console.log('Zone location:', latitude, longitude, 'radius:', radius);
            console.log('Distance child->zone:', distance);

            if (distance <= radius) {
                // Child is already in zone → send notification
                await sendNotification(parentId, 'Danger Zone Alert!', `Your child is already in ${name}`);
                console.log('Notification sent to parent:', parentId);
            }
        }

        res.json({ success: true, message: 'Danger zone added', zone: newZone });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

// Get all danger zones for a parent
router.get('/:parentId', async (req, res) => {
    try {
        const zones = await DangerZone.find({ parentId: req.params.parentId });
        res.json(zones);
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: 'Server error' });
    }
});

module.exports = router;
