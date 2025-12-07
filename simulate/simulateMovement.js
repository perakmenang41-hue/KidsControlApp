const axios = require("axios");

const API_URL = "http://localhost:5000/api/child/family/update-location"; // update if needed
const PARENT_ID = "95CVMGOC";

// Define children with initial positions
const children = [
  { childUID: "tdroo9vp", lat: 3.072, lon: 101.518 },
  { childUID: "child2", lat: 3.080, lon: 101.520 } // example second child
];

// Define a simple movement path for each child: OUTSIDE → APPROACHING → INSIDE → EXITED
const path = [
  { latOffset: 0.0, lonOffset: 0.0 },    // OUTSIDE
  { latOffset: 0.0005, lonOffset: 0.0002 }, // APPROACHING
  { latOffset: 0.001, lonOffset: 0.0003 },  // INSIDE
  { latOffset: 0.0, lonOffset: 0.0 }     // EXITED
];

async function simulateMovement() {
  for (const child of children) {
    console.log(`\nSimulating for childUID: ${child.childUID}`);
    for (const step of path) {
      const body = {
        parentId: PARENT_ID,
        childUID: child.childUID,
        lat: child.lat + step.latOffset,
        lon: child.lon + step.lonOffset
      };
      
      try {
        const res = await axios.post(API_URL, body);
        console.log("POST body:", body);
        console.log("Response:", res.data);
      } catch (err) {
        console.error("Error posting location:", err.message);
      }

      // Wait 3 seconds between updates (simulate real-time movement)
      await new Promise(resolve => setTimeout(resolve, 3000));
    }
  }
}

simulateMovement();
