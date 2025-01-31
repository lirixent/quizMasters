// Import required libraries
const express = require('express');
const router = express.Router();
const Question = require('../model/biblechar'); // Import the Question model



const extractOptionsFromQuestion = (questionText) => {
    const options = {};
    
    // Match the pattern for options, e.g., 'a) Option Text'
    const optionPattern = /([a-d])\)\s([^\n]+)/g;
    let match;

    // Extract options and store them as key-value pairs
    while ((match = optionPattern.exec(questionText)) !== null) {
        const key = match[1]; // Extract the option key ('a', 'b', etc.)
        const value = match[2].trim(); // Extract the corresponding value
        options[key] = value;
    }

    // Return the extracted options or null if none were found
    return Object.keys(options).length > 0 ? options : null;

     };


     // Function to remove options from the Question text
const cleanQuestionText = (questionText) => {
    return questionText.replace(/([a-d])\)\s([^\n]+)/g, "").trim();
};


    // Return the extracted options or null if none were found
   // return Object.keys(options).length > 0 ? options : null;

     /*// If no options were found, log and skip the question
     if (Object.keys(options).length === 0) {
        console.log(`No valid options found for question no: ${question.no}`);
        return null; // Indicate that this question should be skipped


        // Validate the answer
    if (!options[question.Answer]) {
        console.log(
            `Invalid or missing answer for question no: ${question.no}. Skipping...`
        );
        return null; // Indicate that this question should be skipped
    }

    // Update the question with the extracted options
    question.Options = options;
    return question;

    */
//};



/**
 * GET route to fetch all questions
 */

router.get('/test', async (req, res) => {
    try {
        // Fetch all questions from the database
        const questions = await Question.find();

        // Check if no questions exist
        if (questions.length === 0) {
            return res.status(404).json({ message: 'No questions found' });
        }





/*
 // Iterate through all documents
 for (const question of questions) {
    // Extract options from the Question field
    const extractedOptions = extractOptionsFromQuestion(question.Question);

    if (extractedOptions) {
        // If options were successfully extracted, update the document
        question.Options = extractedOptions;
        await question.save(); // Save the updated document
        console.log(`Updated options for question no: ${question.no}`);
    } else {
        console.log(`No valid options found for question no: ${question.no}`);
    }
}*/



for (const question of questions) {
    let extractedOptions = extractOptionsFromQuestion(question.Question);

    if (extractedOptions) {
        // If `Options` is already populated, just clean `Question`
        if (question.Options && Object.keys(question.Options).length > 0) {
            console.log(`Options exist for question no: ${question.no}, cleaning question field.`);
        } else {
            // Populate `Options` if missing
            question.Options = extractedOptions;
            console.log(`Extracted options for question no: ${question.no}`);
        }

        // Remove options from `Question` using cleanQuestionText()
        question.Question = cleanQuestionText(question.Question);
        await question.save();
    } else {
        console.log(`No valid options found for question no: ${question.no}`);
    }
}

// Send the updated questions as a response
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
