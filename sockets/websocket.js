const WebSocket = require('ws');

const gameSessions = {}; // roomId/sessionId => session object

module.exports = (wss) => {
    wss.on('connection', (ws) => {
        console.log('A new player connected');

        ws.on('message', (message) => {
            try {
                const data = JSON.parse(message);

                // --- ANSWER SUBMISSION (Index-Based) ---
                if (data.type === 'answer_submission') {
                    const player = data.player;
                    const answerIndex = data.answerIndex;
                    const roomId = ws.roomId;

                    const session = gameSessions[roomId];
                    if (!session) return;

                    session.answers = session.answers || {};
                    session.answers[player] = answerIndex;

                    session.scores = session.scores || {};
                    if (!(player in session.scores)) session.scores[player] = 0;

                    if (answerIndex === session.correctAnswerIndex) {
                        session.scores[player] += 1;
                        ws.send(JSON.stringify({ type: 'answer_result', correct: true }));
                    } else {
                        ws.send(JSON.stringify({ type: 'answer_result', correct: false }));
                    }

                    const allAnswered = Object.keys(session.players).every(p => session.answers[p] !== undefined);

                    if (allAnswered) {
                        broadcastToRoom(roomId, {
                            type: 'round_summary',
                            answers: session.answers,
                            scores: session.scores
                        });

                        setTimeout(() => {
                            sendNextQuestion(roomId);
                        }, 4000);
                    }

                    return; // Avoid processing further
                }

                // --- EVENT-BASED HANDLING ---
                const { event, sessionId, payload } = data;
                if (!sessionId) {
                    ws.send(JSON.stringify({ error: 'Missing sessionId' }));
                    return;
                }

                if (!gameSessions[sessionId]) {
                    gameSessions[sessionId] = {
                        players: [],
                        playerData: {}, // name => { name, score, totalTime, ... }
                        questions: [],
                        currentQuestionIndex: 0,
                        startTime: Date.now(),
                        ended: false
                    };
                }

                const session = gameSessions[sessionId];

                switch (event) {
                    case 'joinSession':
                        ws.roomId = sessionId;
                        session.players.push(ws);

                        session.playerData[payload.name] = {
                            name: payload.name,
                            score: 0,
                            totalTime: 0,
                            answers: [],
                            categoryStats: {}
                        };

                        broadcast(session.players, {
                            event: 'playerJoined',
                            payload: { name: payload.name }
                        });
                        break;

                    case 'nextQuestion':
                        if (session.ended) return;

                        session.questions = payload.questions;
                        session.currentQuestionIndex = 0;
                        session.startTime = Date.now();

                        const firstQ = session.questions[0];
                        firstQ.startTime = Date.now();

                        broadcast(session.players, {
                            event: 'newQuestion',
                            payload: firstQ
                        });
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

                        // Next question or game over
                        session.currentQuestionIndex += 1;
                        const gameOver = session.currentQuestionIndex >= session.questions.length ||
                                         (Date.now() - session.startTime) / 1000 >= 1500;

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
                            const nextQ = session.questions[session.currentQuestionIndex];
                            nextQ.startTime = Date.now();

                            broadcast(session.players, {
                                event: 'newQuestion',
                                payload: nextQ
                            });
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
};

// --- HELPER FUNCTIONS ---
function broadcast(players, message) {
    players.forEach(ws => {
        if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(message));
        }
    });
}

function broadcastToRoom(roomId, message) {
    const session = gameSessions[roomId];
    if (!session) return;

    session.players.forEach(ws => {
        if (ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify(message));
        }
    });
}

function sendNextQuestion(roomId) {
    const session = gameSessions[roomId];
    if (!session) return;

    session.questionIndex = session.questionIndex || 0;
    session.questionIndex++;

    if (session.questionIndex >= session.questions.length) {
        broadcastToRoom(roomId, { type: 'game_over', scores: session.scores });
        return;
    }

    const nextQ = session.questions[session.questionIndex];
    session.correctAnswerIndex = nextQ.correctIndex;
    session.answers = {}; // reset for new round

    broadcastToRoom(roomId, {
        type: 'question',
        question: {
            text: nextQ.text,
            options: nextQ.options
        }
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
