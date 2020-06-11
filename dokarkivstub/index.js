const express = require('express')
const { v4: uuidv4 } = require('uuid');
const SFTPServer = require('node-sftp-server');
const fs = require('fs');

const expressPort = 3000;
const sftpPort = 3001;

const app = express();
const sftp = new SFTPServer({
    privateKeyFile: "../core/src/test/resources/sftp/itest_privatekey",
    debug: true
});

function* fileGenerator() {
    yield "zip1.zip";
    yield "zip2.zip";
    yield "zip3.zip";
}

app.all('/', (req, res) => {
    res.status(500).send({
        error: "foobar!"
        //journalpostId: "MOCKJOURNALPOST"+uuidv4()
    })
});

app.listen(expressPort, () => console.log(`Example app listening at http://localhost:${expressPort}`));

/*sftp.listen(sftpPort);

sftp.on("connect", (auth, info) => {
    return auth.accept((session) => {
        session.on("readdir", (path, responder) => {
            let files = fileGenerator();
            responder.on("dir", () => {
                let file = files.next();
                if (file.done) {
                    return responder.end();
                } else {
                    console.warn("Returning directory: " + file);
                    responder.file(file.value);
                }
            });
            return responder.on("end", () => {
                return;
            });
        });
        session.on("readfile", (path, writestream) => {
            return fs.createReadStream("tmp/mockDataSkanmotovrig.zip").pipe(writestream);
        });
        return session.on("writefile", (path, readstream) => {
            let something = fs.createWriteStream("tmp/garbage");
            readstream.on("end", () => {});
            return readstream.pipe(something);
        });
    });
});

sftp.on("error", () => {
    return console.warn("server encountered an error");
});
sftp.on("end", () => {
    return console.warn("says user disconnected");
});
*/