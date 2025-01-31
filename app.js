

// Import required libraries
const express = require('express');
const mongoose = require('mongoose');
const bodyParser = require('body-parser');


const app = express();
const questionRoute = require('./routes/question'); // Import the question route module
const userRoute = require('./routes/users'); // Import the users route module
const postQuestionRoute = require('./routes/questionP');
const updateQuestionRoute = require('./routes/questionU')



// Middleware
app.use(express.json()); // To parse JSON request bodies
app.use('/question', questionRoute);
app.use('/users', userRoute);
app.use('/questionU', updateQuestionRoute)
app.use('/questionP', postQuestionRoute)
app.use(bodyParser.json());


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
