const sessionId = window.location.pathname.split('/').pop();
const playerName = localStorage.getItem('playerName') || prompt("Enter your name:");
localStorage.setItem('playerName', playerName);

// Connect to WebSocket server
const socket = new WebSocket(`ws://${location.host}`);

socket.addEventListener('open', () => {
    console.log('WebSocket connected');
    socket.send(JSON.stringify({
        event: 'joinSession',
        sessionId,
        payload: { name: playerName }
    }));
});

socket.addEventListener('message', (event) => {
    const message = JSON.parse(event.data);
    console.log('Received:', message);

    if (message.event === 'newQuestion') {
        const question = message.payload;
        showQuestion(question);
    }

    if (message.event === 'answerResult') {
        alert(`${message.payload.correct ? "Correct" : "Wrong"}! Score: ${message.payload.score}`);
    }

    if (message.event === 'gameOver') {
        showLeaderboard(message.payload);
    }

    if (message.event === 'playerJoined') {
        console.log(`${message.payload.name} joined the game`);
    }
    


});

// When "Start Game" button is clicked
function startGame(questions) {
    socket.send(JSON.stringify({
        event: 'nextQuestion',
        sessionId,
        payload: { questions }
    }));
}

function submitAnswer(answer) {
    socket.send(JSON.stringify({
        event: 'submitAnswer',
        sessionId,
        payload: {
            name: playerName,
            answer
        }
    }));
}

function showQuestion(q) {
    const container = document.getElementById('question');
    container.innerHTML = `<h3>${q.text}</h3>`;
    q.options.forEach(opt => {
        const btn = document.createElement('button');
        btn.textContent = opt;
        btn.onclick = () => submitAnswer(opt);
        container.appendChild(btn);
    });
}

function showLeaderboard(data) {
    const container = document.getElementById('question');
    container.innerHTML = '<h2>Game Over</h2>';
    data.fullLeaderboard.forEach(p => {
        const div = document.createElement('div');
        div.textContent = `${p.name}: ${p.score} pts (${p.totalTime}s)`;
        container.appendChild(div);
    });
}
