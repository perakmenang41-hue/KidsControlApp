function apiKeyMiddleware(req, res, next) {
    // Allow all GET requests (e.g., for browser testing)
    if (req.method === "GET") return next();

    const key = req.headers['authorization'];
    if (!key || key !== `Bearer ${process.env.API_KEY}`) {
        return res.status(403).json({ message: 'Forbidden: Invalid API Key' });
    }

    next();
}

module.exports = apiKeyMiddleware;
