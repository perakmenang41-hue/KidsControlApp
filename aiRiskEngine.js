// aiRiskEngine.js
// Explainable AI risk engine

function calculateRisk({ speed = 0, hour = 12, distance = 999999, radius = 50, timeInZone = 0 }) {
  // All inputs in SI units: speed m/s, distance meters, radius meters, timeInZone seconds
  let risk = 0;
  const reasons = [];

  // 1️⃣ Close to zone & moving toward it
  if (distance <= radius * 1.5 && speed > 1.2) { // >1.2 m/s indicates moving
    risk += 30;
    reasons.push("Moving quickly toward a danger zone");
  }

  // 2️⃣ High speed -> possible vehicle
  if (speed > 4.0) { // >4 m/s ~ 14.4 km/h
    risk += 30;
    reasons.push("High speed detected (possible vehicle)");
  }

  // 3️⃣ Night-time or unusual hours
  if (hour < 6 || hour > 22) {
    risk += 20;
    reasons.push("Unusual time (late night / early morning)");
  }

  // 4️⃣ Prolonged stay inside zone
  if (distance <= radius && timeInZone > 300) { // 5 minutes
    risk += 25;
    reasons.push("Prolonged stay inside restricted area");
  }

  // 5️⃣ Approaching modifier (near zone but not inside yet)
  if (distance > radius && distance <= radius + 50) { // 50m buffer
    risk += 10;
    reasons.push("Approaching danger zone");
  }

  // Cap risk at 100
  risk = Math.min(Math.round(risk), 100);

  // Risk level label
  let level = "LOW";
  if (risk >= 70) level = "HIGH";
  else if (risk >= 40) level = "MEDIUM";

  return { risk, level, reasons };
}

module.exports = calculateRisk;
