const mongoose = require('mongoose');

const childSchema = new mongoose.Schema({
    parentId: { type: String, required: true },
    name: { type: String, required: true },
    lat: { type: Number, required: true },
    lng: { type: Number, required: true },
    updatedAt: { type: Date, default: Date.now },
    inZone: [{ type: mongoose.Schema.Types.ObjectId, ref: 'DangerZone' }]
});

module.exports = mongoose.model('Child', childSchema);
