//libraries

const express = require('express')
const router = express.Router()

router.get('/', (req, res) =>{

    res.send('The name should display after been set as a participant')
})

router.get('/User1', (req, res) =>{

    res.send('Record point for user1')

})

module.exports = router