const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const QRCode = require('qrcode');

// In-memory storage (replace with DB or Redis in production)
let gameSessions = {};

// Host a new multiplayer session
router.get('/host', async (req, res) => {
    const sessionId = uuidv4();
    gameSessions[sessionId] = {
        hostStarted: false,
        players: []
    };

    const joinUrl = `http://34.59.157.47:3000/users/join/${sessionId}`;
    const qrCodeData = await QRCode.toDataURL(joinUrl);

    res.send(`
        <html>
            <body>
                <h2>Multiplayer Game Lobby</h2>
                <p>Session ID: ${sessionId}</p>
                <img src="${qrCodeData}" alt="Scan to join"/>
                <p>Or click: <a href="${joinUrl}">${joinUrl}</a></p>
            </body>
        </html>
    `);
});

// Player joins session via QR or link
router.get('/join/:sessionId', (req, res) => {
    const { sessionId } = req.params;

    if (!gameSessions[sessionId]) {
        return res.status(404).send('Session not found');
    }

    res.send(`
        <html>
            <body>
                <h3>Welcome Player</h3>
                <p>Joined game session: ${sessionId}</p>
                <form action="/users/setname/${sessionId}" method="POST">
                    <input type="text" name="name" placeholder="Enter your name" required />
                    <button type="submit">Join Game</button>
                </form>
            </body>
        </html>
    `);
});

// Capture player name
router.post('/setname/:sessionId', express.urlencoded({ extended: true }), (req, res) => {
    const { sessionId } = req.params;
    const { name } = req.body;

    if (!gameSessions[sessionId]) {
        return res.status(404).send('Session not found');
    }

    gameSessions[sessionId].players.push({ name, score: 0 });

    res.send(`
        <html>
            <body>
                <h3>Hello ${name}, you've joined successfully!</h3>
                <p>Waiting for the host to start the game...</p>
            </body>
        </html>
    `);
});

module.exports = router;
