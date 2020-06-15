const express = require('express')
const { v4: uuidv4 } = require('uuid');

const expressPort = 3000;

const app = express();

app.all('/', (req, res) => {
    console.log("Oppretter journalpost")
    res.status(200).send({
        journalpostId: "MOCKJOURNALPOST"+uuidv4()
    })
});

app.listen(expressPort, () => console.log(`Example app listening at http://localhost:${expressPort}`));
