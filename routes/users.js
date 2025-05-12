const express = require('express');
const router = express.Router();
const { v4: uuidv4 } = require('uuid');
const QRCode = require('qrcode');
const axios = require('axios');


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

// Route: GET /users/lobby/:sessionId
router.get('/lobby/:sessionId', (req, res) => {
    const { sessionId } = req.params;

    if (!gameSessions[sessionId]) {
        return res.status(404).send('Session not found');
    }

    const players = gameSessions[sessionId].players;

    res.send(`
        <html>
            <head>
                <meta http-equiv="refresh" content="5"> <!-- Refresh every 5s -->
            </head>
            <body>
                <h2>Lobby for Session: ${sessionId}</h2>
                <p>Players joined:</p>
                <ul>
                    ${players.map(p => `<li>${p.name}</li>`).join('')}
                </ul>
                <form method="POST" action="/users/start/${sessionId}">
                    <button type="submit">Start Game</button>
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

router.post('/start/:sessionId', async (req, res) => {
    const { sessionId } = req.params;

    if (!gameSessions[sessionId]) {
        return res.status(404).send('Session not found');
    }

    try {
        // Fetch question bank from your server
        const response = await axios.get('http://34.59.157.47:3000/question/test');
        const allQuestions = response.data;

        // Select 50 random questions
        const shuffled = allQuestions.sort(() => 0.5 - Math.random());
        const selectedQuestions = shuffled.slice(0, 50);

        // Save questions to the session
        gameSessions[sessionId].questions = selectedQuestions;
        gameSessions[sessionId].hostStarted = true;

        // Redirect to play screen
        res.redirect(`/users/play/${sessionId}`);
    } catch (error) {
        console.error('Failed to fetch questions:', error.message);
        res.status(500).send('Could not fetch questions.');
    }
    
});



module.exports = router;

/*
    // Mark game as started
    gameSessions[sessionId].hostStarted = true;

    // For now, just return a confirmation (later redirect to questions)
    res.send(`
        <html>
            <body>
                <h2>Game Started!</h2>
                <p>Questions are being sent to players...</p>
            </body>
        </html>
    `);
    try {
        // Fetch question bank from your server
        const response = await axios.get('http://34.59.157.47:3000/question/test'); // Use IP version on GCP
        const allQuestions = response.data;

        // Select 50 random questions
        const shuffled = allQuestions.sort(() => 0.5 - Math.random());
        const selectedQuestions = shuffled.slice(0, 50);

        // Save questions to the session
        gameSessions[sessionId].questions = selectedQuestions;
        gameSessions[sessionId].hostStarted = true;

        res.redirect(`/users/play/${sessionId}`);
    } catch (error) {
        console.error('Failed to fetch questions:', error.message);
        res.status(500).send('Could not fetch questions.');
    }


*/



