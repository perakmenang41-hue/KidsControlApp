const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
require("dotenv").config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Endpoint to save FCM token
app.post("/api/parent/save-token", (req, res) => {
  const { parentId, token } = req.body;

  if (!parentId || !token) {
    return res.status(400).json({ message: "parentId and token are required" });
  }

  console.log("Received FCM token:", { parentId, token });

  // TODO: Save to database here

  res.status(200).json({ message: "Token saved successfully" });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
