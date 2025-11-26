const mongoose = require('mongoose');

const parentSchema = new mongoose.Schema({
    name: { type: String },
    email: { type: String },
    pushTokens: { type: [String], default: [] } // array of FCM tokens
});

module.exports = mongoose.model('Parent', parentSchema);
