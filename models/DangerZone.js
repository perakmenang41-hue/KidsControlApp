const mongoose = require('mongoose');

const dangerZoneSchema = new mongoose.Schema({
    parentId: { type: String, required: true },
    name: { type: String, required: true },
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true },
    radius: { type: Number, required: true },
    createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('DangerZone', dangerZoneSchema);
