const express = require('express');
const router = express.Router();
const Question = require('../model/biblechar'); // Adjust path if necessary

// Route to backfill and update the Options field
router.put('/update-options', async (req, res) => {
    try {
        const { defaultOptions } = req.body;

      

      // Validate defaultOptions
      if (
        !Array.isArray(defaultOptions) ||
        defaultOptions.length === 0 ||
        !defaultOptions.every(option => typeof option === 'string')
    ) {
        return res.status(400).json({
            message: '`defaultOptions` must be a non-empty array of strings.'
        });
    }

    // Map the array of options into an object (key-value pairs)
    const optionsObject = defaultOptions.reduce((acc, option, index) => {
        const key = String.fromCharCode(97 + index);  // Generate keys like 'a', 'b', 'c', ...
        acc[key] = option;
        return acc;
    }, {});

    // Add the Options field to all documents in the database
    const result = await Question.updateMany(
        {}, // Match all documents
        { $set: { Options: optionsObject } }, // Add the Options field with the structured options
        { upsert: false } // Do not create new documents
    );

    // Respond with success
    res.status(200).json({
        message: 'Options field added to all documents successfully',
        modifiedCount: result.modifiedCount, // Number of documents updated
    });
} catch (error) {
    console.error('Error updating Options field:', error.message);
    res.status(500).json({ message: 'Internal server error', error: error.message });
}
});

module.exports = router;
