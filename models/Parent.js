// models/Parent.js
const mongoose = require('mongoose');

const parentSchema = new mongoose.Schema({
    name: { type: String, required: true },
    email: { type: String, required: true },
    pushTokens: { type: [String], default: [] } // array of Expo push tokens
});

module.exports = mongoose.model('Parent', parentSchema);
