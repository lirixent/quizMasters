

// Import required libraries
const express = require('express');
const mongoose = require('mongoose');
const bodyParser = require('body-parser');
const http = require('http'); // <-- Required for WebSocket integration
const WebSocket = require('ws'); // npm install ws
const app = express();
const { Server } = require('ws');
const path = require('path')


const server = http.createServer(app); // <-- Create HTTP server from Express app

//const server = http.createServer(app);
const wss = new Server({ server });


//const wss = new WebSocket.Server({ server }); // <-- WebSocket server



const gameSessions = {}; // Shared game state

//app.use(express.static('public'));
// Serve static files if needed
app.use(express.static(path.join(__dirname, 'public')));
// Serve multiplayer.html from the public directory
app.get('/multiplayer', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'multiplayer.html'));
  });


const questionRoute = require('./routes/question'); // Import the question route module
const userRoute = require('./routes/users')(gameSessions); // Import the users route module
const postQuestionRoute = require('./routes/questionP');
const updateQuestionRoute = require('./routes/questionU')
const deleteQuestionRoute = require('./routes/questionD');


// Middleware

app.use(express.json()); // To parse JSON request bodies
app.use('/question', questionRoute);
app.use('/users', userRoute);
app.use('/questionU', updateQuestionRoute)
app.use('/questionP', postQuestionRoute)
app.use('/questionD', deleteQuestionRoute);
app.use(bodyParser.json());

app.use(bodyParser.urlencoded({ extended: false }));


require('./sockets/websocket')(wss, gameSessions); // <-- Load WebSocket logic

// Define the home route
app.get('/', (req, res) => {
    res.send('Home Page');
});

// MongoDB connection string
const MURL = 'mongodb+srv://oduroj01:letJESUSin123*@cluster0.mxkij.mongodb.net/?retryWrites=true&w=majority';

// Connect to MongoDB
mongoose.connect(MURL, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    dbName: 'Bible-Quiz'
})
    .then(() => {
        console.log('MongoDB Connected');
    })
    .catch((err) => {
        console.error('Error connecting to MongoDB:', err);
    });

// Enable Mongoose debug mode
mongoose.set('debug', true);

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is up and running on port ${PORT}...`);
});
