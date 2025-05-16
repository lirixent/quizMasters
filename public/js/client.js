const sessionId = window.location.pathname.split('/').pop();
const playerName = localStorage.getItem('playerName') || prompt("Enter your name:");
localStorage.setItem('playerName', playerName);

const isHost = localStorage.getItem('isHost') === 'true';

// Connect to WebSocket server
const socket = new WebSocket(`ws://${location.host}`);

socket.addEventListener('open', () => {
    console.log('WebSocket connected');
    socket.send(JSON.stringify({
      //  type: 'join',
       // sessionId: sessionId,
       // playerName: playerName
        event: 'joinSession',
        sessionId,
        payload: { name: playerName }
    }));

    // Show Start Game button for host
    if (isHost) {
        const startBtn = document.getElementById('startBtn');
        startBtn.style.display = 'block';
        startBtn.onclick = () => {
            // Example questions for demo/testing — ideally pulled from DB/backend
            const questions = [
                {
                    id: "q1",
                    text: "Who built the ark?",
                    options: ["Noah", "Moses", "Abraham", "David"],
                    answer: "Noah",
                    category: "Bible Characters",
                    reference: "Genesis 6:14"
                },
                {
                    id: "q2",
                    text: "Where was Jesus born?",
                    options: ["Nazareth", "Bethlehem", "Jerusalem", "Galilee"],
                    answer: "Bethlehem",
                    category: "Life of Jesus",
                    reference: "Luke 2:4–7"
                }
            ];

            // Transform questions to match server's expected format
            const formattedQuestions = questions.map(q => ({
                id: q.id,
                text: q.text,
                options: q.options,
                correctAnswer: q.answer,
                category: q.category || "General",
                reference: q.reference || "N/A"
            }));


            startGame(formattedQuestions);

        };
    }
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

// Host sends questions to server
function startGame(questions) {
    socket.send(JSON.stringify({
       // type: 'startGame',
        // sessionId: sessionId,
        // questions: questions

        event: 'startGame',
        sessionId,
        payload: { questions }
    }));
}

// Player submits answer
function submitAnswer(answer) {
    socket.send(JSON.stringify({
       
       // type: 'submitAnswer',
       // sessionId: sessionId,
       // playerName: playerName,
       // answer: answer
       
        event: 'submitAnswer',
        sessionId,
        payload: {
            name: playerName,
            answer
        }
    }));
}

// Renders current question and options
function showQuestion(q) {
    const container = document.getElementById('question');
    container.innerHTML = `<h3>${q.text}</h3>`;
    q.options.forEach(opt => {
        const btn = document.createElement('button');
        btn.textContent = opt;
        btn.onclick = () => submitAnswer(opt);
        container.appendChild(btn);
    });


     // Optional: show reference if available
     if (q.reference && q.reference !== "N/A") {
        const ref = document.createElement('p');
        ref.innerHTML = `<em>Reference: ${q.reference}</em>`;
        container.appendChild(ref);
    }


}

// Renders final leaderboard
function showLeaderboard(data) {
    const container = document.getElementById('question');
    container.innerHTML = '<h2>Game Over</h2>';
    data.fullLeaderboard.forEach(p => {
        const div = document.createElement('div');
        div.textContent = `${p.name}: ${p.score} pts (${p.totalTime}s)`;
        container.appendChild(div);
    });
}
