const express = require('express');
const router = express.Router();
const Question = require('../model/biblechar'); // Adjust path if necessary

// Route to delete a question based on the field number
router.delete('/test/:no', async (req, res) => {
    try {
        const { no } = req.params; // Extract the number from request params

        // Validate input
        if (!no) {
            return res.status(400).json({ message: 'Field number is required for deletion.' });
        }

        // Delete the document where "no" matches the given number
        const result = await Question.deleteOne({ no: parseInt(no) });

        // Check if a document was deleted
        if (result.deletedCount === 0) {
            return res.status(404).json({ message: 'No document found with the given number.' });
        }

        // Respond with success message
        res.status(200).json({ message: 'Question deleted successfully' });
    } catch (error) {
        console.error('Error deleting question:', error.message);
        res.status(500).json({ message: 'Internal server error', error: error.message });
    }
});

module.exports = router;
