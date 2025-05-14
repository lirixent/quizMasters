const express = require('express');
const { v4: uuidv4 } = require('uuid');
const QRCode = require('qrcode');
const axios = require('axios');

module.exports = function (gameSessions) {
    const router = express.Router();

    // Route: Host a new multiplayer session
    router.get('/host', async (req, res) => {
        const sessionId = uuidv4();

        gameSessions[sessionId] = {
            hostStarted: false,
            players: [],
            scores: {},
            currentQuestion: null,
            currentQuestionIndex: 0,
            questions: []
        };

        const joinUrl = `http://34.59.157.47:3000/users/join/${sessionId}`;
        const qrCodeData = await QRCode.toDataURL(joinUrl);

        res.send(`
            <html>
                <body>
                    <h2>Multiplayer  Quiz Lobby</h2>
                    <p><strong>Session ID:</strong> ${sessionId}</p>
                    <img src="${qrCodeData}" alt="Scan to join"/>
                    <p>Or click: <a href="${joinUrl}">${joinUrl}</a></p>
                    <form action="/users/start/${sessionId}" method="POST">
                        <button type="submit">Start Game</button>
                    </form>
                </body>
            </html>
        `);
    });

    // Route: Player joins via QR or link
    router.get('/join/:sessionId', (req, res) => {
        const { sessionId } = req.params;

        if (!gameSessions[sessionId]) {
            return res.status(404).send('Session not found.');
        }

        res.send(`
            <html>
                <body>
                    <h3>Welcome Player</h3>
                    <p>Game Session: ${sessionId}</p>
                    <form action="/users/setname/${sessionId}" method="POST">
                        <input type="text" name="name" placeholder="Enter your name" required />
                        <button type="submit">Join Game</button>
                    </form>
                </body>
            </html>
        `);
    });

    // Route: Register player and inject WebSocket join logic
    router.post('/setname/:sessionId', express.urlencoded({ extended: true }), (req, res) => {
        const { sessionId } = req.params;
        const { name } = req.body;

        const session = gameSessions[sessionId];
        if (!session) {
            return res.status(404).send('Session not found.');
        }

        const existing = session.players.find(p => p.name === name);
        if (existing) {
            return res.send(`<p>Name "${name}" already taken in this session. Please go back and choose another.</p>`);
        }

        session.players.push({ name });
        session.scores[name] = 0;

        res.send(`
            <html>
                <body>
                    <h3>Hello ${name}, you have joined the game!</h3>
                    <p>Keep this tab open. The game will begin soon...</p>
                    <script>
                        const ws = new WebSocket('ws://' + location.hostname + ':3000');
                        ws.onopen = () => {
                            ws.send(JSON.stringify({
                                event: 'joinSession',
                                sessionId: '${sessionId}',
                                payload: { name: '${name}' }
                            }));
                        };
                    </script>
                </body>
            </html>
        `);
    });

    // Route: Host starts the game and loads questions
    router.post('/start/:sessionId', async (req, res) => {
        const { sessionId } = req.params;
        const session = gameSessions[sessionId];

        if (!session) {
            return res.status(404).send('Session not found.');
        }

        try {
            const response = await axios.get('http://34.59.157.47:3000/question/test');
            const allQuestions = response.data.questions;

            if (!Array.isArray(allQuestions) || allQuestions.length === 0) {
                return res.status(500).send('No questions found.');
            }

            const shuffled = allQuestions.sort(() => 0.5 - Math.random());
            const selectedQuestions = shuffled.slice(0, 50);

            session.questions = selectedQuestions;
            session.currentQuestionIndex = 0;
            session.currentQuestion = selectedQuestions[0];
            session.hostStarted = true;

            // Broadcast first question
            session.players.forEach(player => {
                if (player.ws && player.ws.readyState === 1) {
                    player.ws.send(JSON.stringify({
                        event: 'newQuestion',
                        payload: session.currentQuestion
                    }));
                }
            });

            res.redirect(`/users/play/${sessionId}`);
        } catch (err) {
            console.error('Error fetching questions:', err.message);
            res.status(500).send('Error retrieving quiz questions.');
        }
    });

    // Route: Inform host or players game has started
    router.get('/play/:sessionId', (req, res) => {
        const { sessionId } = req.params;
        const session = gameSessions[sessionId];

        if (!session || !session.hostStarted) {
            return res.status(404).send('Game not started or session invalid.');
        }

        res.send(`
            <html>
                <body>
                    <h2>Bible Quiz Game Started!</h2>
                    <p>WebSocket will handle real-time gameplay. Check the console or client for question events.</p>
                    <script>
                        const sessionId = "${sessionId}";
                        const socket = new WebSocket("ws://" + window.location.hostname + ":3000");
                        let playerName = prompt("Enter your name") || "Guest";
        
                        socket.onopen = () => {
                            socket.send(JSON.stringify({
                                event: 'joinSession',
                                sessionId: sessionId,
                                payload: { name: playerName }
                            }));
                        };
        
                        socket.onmessage = (event) => {
                            const data = JSON.parse(event.data);
                            switch (data.event) {
                                case 'playerJoined':
                                    console.log(data.payload.name + ' joined the game');
                                    break;
        
                                case 'newQuestion':
                                    const question = data.payload;
                                    document.body.innerHTML += '
                                        <h3>${question.text}</h3>
                                        <div id="answers"></div>
                                    ';
                                    const answersDiv = document.getElementById('answers');
                                    question.options.forEach(option => {
                                        const btn = document.createElement('button');
                                        btn.textContent = option;
                                        btn.onclick = () => {
                                            socket.send(JSON.stringify({
                                                event: 'submitAnswer',
                                                sessionId: sessionId,
                                                payload: {
                                                    name: playerName,
                                                    answer: option
                                                }
                                            }));
                                        };
                                        answersDiv.appendChild(btn);
                                    });
                                    break;
        
                                case 'answerResult':
                                    alert(data.payload.correct ? 'Correct!' : 'Wrong!');
                                    break;
        
                                case 'gameOver':
                                    alert("Game Over! Top Players:");
                                    console.log(data.payload.leaderboardTop3);
                                    break;
        
                                default:
                                    console.warn('Unhandled event:', data.event);
                            }
                        };
                    </script>
                </body>
            </html>
        `);
        
    });

    return router;
};
