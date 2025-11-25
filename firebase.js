const admin = require('firebase-admin');
const serviceAccount = require('./firebaseServiceAccountKey.json'); // <- your file

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

module.exports = admin;
