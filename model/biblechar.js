const mongoose = require('mongoose');

// Define the schema for the Bible Quiz
const bibleQuizSchema = new mongoose.Schema(
    {
        no: {
            type: Number,
            required: true,
        },
        Category: {
            type: String,
            required: true,
        },
        Character: {
            type: String,
            required: true,
        },
       // Question: 
        
       Question:  {
            type: String,
            required: true,
        },

    Options: {
        type: Map, // Map to store key-value pairs for options
        of: String,
        required: true
    },
        Answer: {
            type: String,
            required: true,
        },
        Bible_reference: {
            type: String,
            default: null,
        },
        Point: {
            type: Number,
            default: 0,
        },
        AgPoint: {
            type: Number,
            default: 0,
        },
        time: {
            type: Number,
            required: true,
            default: 30,
        },
    }
    /*,
    {
        timestamps: true,
        collection: 'questions', // Explicitly set the collection name
    }
        */


);

/*

// Adding the extractOption method to the schema to get the options in a readable format
bibleQuizSchema.methods.extractOption = function () {
    let options = [];
    // Go through the map of options and extract the key-value pairs as a formatted list
    this.Options.forEach((value, key) => {
        options.push({ option: key, text: value });
    });
    return options;
};
*/


// Export the model
//module.exports = mongoose.model('questions', bibleQuizSchema);

module.exports = mongoose.model('Question', bibleQuizSchema);

