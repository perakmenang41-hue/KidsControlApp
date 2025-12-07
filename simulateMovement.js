const axios = require("axios");

const API_URL = "http://localhost:5000/api/child/family/update-location";
const PARENT_ID = "95CVMGOC";
const API_KEY = "supersecretapikey"; // put the same key as in your .env

const children = [
  { childUID: "tdroo9vp", lat: 3.072, lon: 101.518 },
  { childUID: "child2", lat: 3.080, lon: 101.520 }
];

const path = [
  { latOffset: 0.0, lonOffset: 0.0 },    
  { latOffset: 0.0005, lonOffset: 0.0002 }, 
  { latOffset: 0.001, lonOffset: 0.0003 },  
  { latOffset: 0.0, lonOffset: 0.0 }     
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
        const res = await axios.post(API_URL, body, {
          headers: {
            'Authorization': `Bearer ${API_KEY}` // <- API key here
          }
        });
        console.log("POST body:", body);
        console.log("Response:", res.data);
      } catch (err) {
        console.error("Error posting location:", err.message);
      }

      await new Promise(resolve => setTimeout(resolve, 3000));
    }
  }
}

simulateMovement();
