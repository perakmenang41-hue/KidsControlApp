const DangerZone = require("../models/DangerZone");

exports.addDangerZone = async (req, res) => {
  try {
    const dz = await DangerZone.create(req.body);
    res.status(201).json(dz);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.removeDangerZone = async (req, res) => {
  try {
    await DangerZone.findByIdAndDelete(req.params.id);
    res.json({ message: "Danger zone removed" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.getDangerZones = async (req, res) => {
  try {
    const zones = await DangerZone.find({ childUID: req.params.childUID });
    res.json(zones);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
