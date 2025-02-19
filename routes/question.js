// Import required libraries
const express = require('express');
const router = express.Router();
const Question = require('../model/biblechar'); // Import the Question model

// Function to extract options from a question text
const extractOptionsFromQuestion = (questionText) => {
    const options = {};
    const optionPattern = /([A-D])\)\s([^\n]+)/g;
    let match;

    while ((match = optionPattern.exec(questionText)) !== null) {
        const key = match[1]; // Extract option key ('A', 'B', etc.)
        const value = match[2].trim(); // Extract corresponding value
        options[key] = value;
    }

    return Object.keys(options).length > 0 ? options : null;
};

// Function to clean the Question text by removing options
const cleanQuestionText = (questionText) => {
    return questionText.replace(/([A-D])\)\s([^\n]+)/g, "").trim();
};

// Function to validate and remove duplicate questions
const removeDuplicates = async () => {
    try {
        const questions = await Question.find();
        let seenNumbers = new Set();
        let seenQuestions = new Set();
        let duplicatesToDelete = [];

        for (let question of questions) {
            let uniqueKey = `${question.no}-${question.Question}`;

            // If exact duplicate (same `no` and `Question`), delete duplicates
            if (seenNumbers.has(question.no) && seenQuestions.has(question.Question)) {
                duplicatesToDelete.push(question._id);
                continue;
            }

            // If `no` is duplicated but `Question` is different, increment `no`
            while (seenNumbers.has(question.no)) {
                question.no += 1;
            }

            // Save updated number if changed
            await question.save();

            // Mark as seen
            seenNumbers.add(question.no);
            seenQuestions.add(question.Question);
        }

        // Delete exact duplicate documents
        if (duplicatesToDelete.length > 0) {
            await Question.deleteMany({ _id: { $in: duplicatesToDelete } });
            console.log(`Deleted ${duplicatesToDelete.length} duplicate questions.`);
        }

        return await Question.find(); // Return updated questions
    } catch (error) {
        console.error('Error removing duplicates:', error);
    }
};

/**
 * GET route to fetch and clean questions
 */
router.get('/test', async (req, res) => {
    try {
        // Remove duplicates first
        let questions = await removeDuplicates();

        if (questions.length === 0) {
            return res.status(404).json({ message: 'No questions found' });
        }

        for (const question of questions) {
            let extractedOptions = extractOptionsFromQuestion(question.Question);

            if (extractedOptions) {
                if (!question.Options || Object.keys(question.Options).length === 0) {
                    question.Options = extractedOptions;
                    console.log(`Extracted options for question no: ${question.no}`);
                }

                // Clean Question text
                question.Question = cleanQuestionText(question.Question);
                await question.save();
            } else {
                console.log(`No valid options found for question no: ${question.no}`);
            }
        }

        const updatedQuestions = await Question.find();
        res.status(200).json({
            message: 'Options field updated successfully based on Question field.',
            questions: updatedQuestions,
        });
    } catch (error) {
        console.error('Error updating Options field:', error.message);
        res.status(500).json({
            message: 'Failed to update Options field. Please try again later.',
            error: error.message,
        });
    }
});

module.exports = router;
