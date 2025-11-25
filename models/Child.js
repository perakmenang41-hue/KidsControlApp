const mongoose = require('mongoose');

const childSchema = new mongoose.Schema({
    parentId: { type: String, required: true },
    name: { type: String, required: true },
    lat: { type: Number, required: true },    // current latitude
    lng: { type: Number, required: true },    // current longitude
    updatedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Child', childSchema);
