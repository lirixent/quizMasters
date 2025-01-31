const mongoose = require('mongoose');

// Define the schema for the Bible Quiz
const bibleQuizSchema = new mongoose.Schema({
    no: {
        type: Number,
        required: true, // This field is required
    },
    Category: {
        type: String,
        required: true, // This field is required
    },
    Character: {
        type: String,
        required: true, // This field is required
    },
    Question: {
        type: String,
        required: true, // This field is required
    },
    Answer: {
        type: String,
        required: true, // This field is required
    },
    Bible_reference: {
        type: String,
        required: false, // Made optional since it was commented out in your code
    },
    Point: {
        type: Number,
        required: true, // This field is required
    },
    AgPoint: {
        type: Number,
        required: true, // This field is required
    },
    time: {
        type: Number,
        required: true, // This field is required
    },
});

// Export the model
module.exports = mongoose.model('Question', bibleQuizSchema);
