const express = require('express')
const { v4: uuidv4 } = require('uuid');

const expressPort = 3000;

const app = express();

app.all('/', (req, res) => {
    res.status(500).send({
        error: "foobar!"
        //journalpostId: "MOCKJOURNALPOST"+uuidv4()
    })
});

app.listen(expressPort, () => console.log(`Example app listening at http://localhost:${expressPort}`));
