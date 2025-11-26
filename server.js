require("dotenv").config();
const express = require("express");
const cors = require("cors");

const app = express();
const PORT = process.env.PORT || 5000;

// =============================
// Firebase Admin Init
// =============================
const admin = require("./firebase");

// =============================
// Middleware
// =============================

// Enable CORS
app.use(cors());

// Parse JSON bodies (ensure this is before routes)
app.use(express.json());

// Optional API key middleware
const apiKeyMiddleware = require("./middleware/apiKeyMiddleware");
app.use(apiKeyMiddleware);

// Debug middleware for Render deployment
app.use((req, res, next) => {
    console.log(`Incoming request: ${req.method} ${req.url}`);
    if (req.method === "POST" || req.method === "PUT") {
        console.log("Headers:", req.headers);
        let bodyData = '';
        req.on('data', chunk => bodyData += chunk);
        req.on('end', () => {
            console.log("Raw body:", bodyData);
            next();
        });
    } else {
        next();
    }
});

// =============================
// Route Imports
// =============================
const childRoute = require("./routes/childRoute");                  
const familyRoute = require("./routes/familyRoute");                
const dangerZoneRoute = require("./routes/dangerZoneRoute");        
const parentRoute = require("./routes/parentRoute");                
const checkDangerRoute = require("./routes/checkDangerRoute");      

// =============================
// Organized Routes
// =============================

// Child-related routes
app.use("/api/child", childRoute);
app.use("/api/child/family", familyRoute);

// Parent-related routes
app.use("/api/parent", parentRoute);                 
app.use("/api/parent/dangerzone", dangerZoneRoute); 

// Optional route to manually check danger zones
app.use("/api/parent/dangerzone/check", checkDangerRoute);

// =============================
// Error Handling
// =============================
app.use((err, req, res, next) => {
    console.error("Unhandled Error:", err);
    res.status(500).json({ success: false, message: "Server error", error: err.message });
});

// =============================
// Start Server
// =============================
app.listen(PORT, () => console.log(`🚀 Server running on port ${PORT}`));
