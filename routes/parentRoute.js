// routes/parentRoute.js
const express = require('express');
const router = express.Router();
const Parent = require('../models/Parent');

// Endpoint to register/save Expo push token
router.post('/register-token', async (req, res) => {
    const { parentId, token } = req.body;
    if (!parentId || !token) return res.status(400).json({ message: 'Invalid payload' });

    try {
        const parent = await Parent.findById(parentId);
        if (!parent) return res.status(404).json({ message: 'Parent not found' });

        if (!parent.pushTokens.includes(token)) {
            parent.pushTokens.push(token);
            await parent.save();
        }

        res.json({ success: true, pushTokens: parent.pushTokens });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
