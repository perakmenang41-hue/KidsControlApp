require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const dangerZoneRoute = require('./routes/dangerZoneRoute');
const childRoute = require('./routes/childRoute');

const app = express();
app.use(cors());
app.use(express.json());

// Mount routes
app.use('/api/dangerzone', dangerZoneRoute);
app.use('/api/child', childRoute);

const PORT = process.env.PORT || 3000;

mongoose.connect(process.env.MONGO_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(() => {
    console.log('MongoDB connected');
    app.listen(PORT, () => console.log(`Server running on port ${PORT}`));
}).catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
});
