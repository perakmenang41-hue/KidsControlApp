const Child = require("../models/Child");

exports.registerChild = async (req, res) => {
  try {
    const child = await Child.create(req.body);
    res.status(201).json(child);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.updatePushToken = async (req, res) => {
  try {
    const { uid, pushToken } = req.body;
    const updated = await Child.findOneAndUpdate(
      { uid },
      { pushToken },
      { new: true }
    );
    res.json(updated);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.updateLocation = async (req, res) => {
  try {
    const { uid, lat, lon } = req.body;
    const updated = await Child.findOneAndUpdate(
      { uid },
      {
        lastLocation: { lat, lon, updatedAt: new Date() }
      },
      { new: true }
    );
    res.json(updated);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.getChild = async (req, res) => {
  try {
    const child = await Child.findOne({ uid: req.params.uid });
    res.json(child);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
