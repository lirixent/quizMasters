const WebSocket = require('ws');

module.exports = (wss, gameSessions) => {
    wss.on('connection', (ws) => {
        console.log('A new player connected');

        ws.on('message', (message) => {
            try {
                const data = JSON.parse(message);
                const { event, sessionId, payload } = data;

                if (!event || !sessionId || !payload) {
                    return ws.send(JSON.stringify({ error: 'Missing event, sessionId, or payload' }));
                }

                if (!gameSessions[sessionId]) {
                    gameSessions[sessionId] = {
                        players: [],
                        playerData: {},
                        questions: [],
                        currentQuestionIndex: 0,
                        startTime: Date.now(),
                        ended: false
                    };
                }

                const session = gameSessions[sessionId];

                switch (event) {
                    case 'joinSession':
                        // Initialize session if it doesn't exist

                        console.log("Incoming data:", data);

    if (!gameSessions[sessionId]) {
        gameSessions[sessionId] = {
            players: [],
            playerData: {},
            questions: [],
            currentQuestionIndex: 0,
            startTime: Date.now(),
            ended: false
        };
    }

    const session = gameSessions[sessionId];



                        ws.roomId = sessionId;
                        ws.playerName = payload.name;
                        session.players.push(ws);

                       
                       /* session.playerData[payload.name] = {
                            name: payload.name,
                            score: 0,
                            totalTime: 0,
                            answers: [],
                            categoryStats: {}
                        };
                        */


                        // Safely initialize playerData if not already present
    if (!session.playerData[payload.name]) {
        session.playerData[payload.name] = {
            name: payload.name,
            score: 0,
            totalTime: 0,
            answers: [],
            categoryStats: {}
        };
    }


                        broadcast(session.players, {
                            event: 'playerJoined',
                            payload: { name: payload.name }
                        });
                        break;

                    case 'startGame':
                        session.questions = payload.questions;
                        session.currentQuestionIndex = 0;
                        session.startTime = Date.now();

                        sendQuestionToRoom(sessionId);
                        break;

                    case 'submitAnswer':
                        if (session.ended) return;

                        const player = session.playerData[payload.name];
                        const question = session.questions[session.currentQuestionIndex];
                        const timestamp = Date.now();
                        const timeTaken = (timestamp - (question.startTime || Date.now())) / 1000;
                        const isCorrect = question.correctAnswer.trim().toLowerCase() === payload.answer.trim().toLowerCase();

                        if (isCorrect) {
                            player.score += 1;
                            player.totalTime += timeTaken;
                        }

                        player.answers.push({
                            questionId: question.id,
                            answer: payload.answer,
                            isCorrect,
                            timeTaken,
                            category: question.category
                        });

                        const cat = question.category || "Unknown";
                        if (!player.categoryStats[cat]) {
                            player.categoryStats[cat] = { correct: 0, total: 0 };
                        }
                        player.categoryStats[cat].total += 1;
                        if (isCorrect) player.categoryStats[cat].correct += 1;

                        broadcast(session.players, {
                            event: 'answerResult',
                            payload: {
                                name: payload.name,
                                correct: isCorrect,
                                score: player.score,
                                timeTaken: timeTaken.toFixed(2)
                            }
                        });

                        session.currentQuestionIndex++;
                        const gameOver = session.currentQuestionIndex >= session.questions.length;

                        if (gameOver) {
                            session.ended = true;
                            const leaderboard = getLeaderboard(session.playerData);
                            broadcast(session.players, {
                                event: 'gameOver',
                                payload: {
                                    leaderboardTop3: leaderboard.slice(0, 3),
                                    fullLeaderboard: leaderboard,
                                    playerStats: session.playerData
                                }
                            });
                        } else {
                            sendQuestionToRoom(sessionId);
                        }

                        break;

                    default:
                        ws.send(JSON.stringify({ error: 'Unknown event type' }));
                }

            } catch (err) {
                console.error('Error handling message:', err);
                ws.send(JSON.stringify({ error: 'Invalid message format' }));
            }
        });

        ws.on('close', () => {
            console.log('Player disconnected');
        });
    });

    function broadcast(players, message) {
        players.forEach(ws => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify(message));
            }
        });
    }

    function sendQuestionToRoom(roomId) {
        const session = gameSessions[roomId];
        if (!session) return;

        const question = session.questions[session.currentQuestionIndex];
        question.startTime = Date.now();

        broadcast(session.players, {
            event: 'newQuestion',
            payload: question
        });
    }

    function getLeaderboard(playerData) {
        return Object.values(playerData).map(p => ({
            name: p.name,
            score: p.score,
            totalTime: p.totalTime.toFixed(2),
            categoryStats: getCategoryPerformance(p.categoryStats)
        })).sort((a, b) => {
            if (b.score !== a.score) return b.score - a.score;
            return parseFloat(a.totalTime) - parseFloat(b.totalTime);
        });
    }

    function getCategoryPerformance(stats) {
        const result = {};
        for (const [cat, { correct, total }] of Object.entries(stats)) {
            result[cat] = total ? ((correct / total) * 100).toFixed(1) : "0.0";
        }
        return result;
    }
};
