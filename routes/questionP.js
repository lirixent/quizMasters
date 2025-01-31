const express = require('express');
const router = express.Router();
const Question = require('../model/biblechar'); // ✅ Correctly import model

// Route to post a new question
router.post('/test', async (req, res) => {
    try {
        // Extract the request body with renamed Question field
        const {
            no,
            Category,
            Character,
            Question: questionText, // ✅ Renamed to avoid conflict
            Options,
            Answer,
            Bible_reference,
            Point = 0,
            AgPoint = 0,
            time
        } = req.body;

        // Validate required fields
        if (!no || !Category || !Character || !questionText || !Options || !Answer || !time) {
            return res.status(400).json({ message: 'Missing required fields' });
        }

        // ✅ Ensure `Options` is an object (not an array)
        if (!Options || typeof Options !== 'object' || Object.keys(Options).length === 0) {
            return res.status(400).json({ message: '`Options` must be a non-empty object with key-value pairs' });
        }

        // ✅ Convert `Options` object to a Map
        const optionsMap = new Map(Object.entries(Options));

        // Create a new question document
        const newQuestion = new Question({
            no,
            Category,
            Character,
            Question: questionText,
            Options: optionsMap, // ✅ Store as a Map
            Answer,
            Bible_reference,
            Point,
            AgPoint,
            time
        });

        // Save the question to the database
        const savedQuestion = await newQuestion.save();
        res.status(201).json({ message: 'Question added successfully', question: savedQuestion });
    } catch (error) {
        console.error('Error inserting question:', error.message);
        res.status(500).json({ message: 'Error inserting question', error: error.message });
    }
});

module.exports = router;
