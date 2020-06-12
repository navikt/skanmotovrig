const fs = require('fs');
const crypto = require('crypto');
const constants = require('constants');
const ssh2 = require('ssh2');
const { v4: uuidv4 } = require('uuid');

const OPEN_MODE = ssh2.SFTP_OPEN_MODE;
const STATUS_CODE = ssh2.SFTP_STATUS_CODE;

const ZIPPATH = '../core/src/test/resources/__files/inbound/mockDataSkanmotovrig.zip';

let dirUdid = 0;

function getDirStats() {
    stats = fs.statSync(ZIPPATH);

    return {
        mode: constants.S_IFDIR,
        uid: dirUdid++,
        gid: dirUdid,
        size: 0,
        atime: Date.now() / 1000 | 0,
        mtime: Date.now() / 1000 | 0
    }
}

let key = fs.readFileSync("../core/src/test/resources/sftp/itest_privatekey", {encoding: 'utf8'});
let zipBuffer = fs.readFileSync("../core/src/test/resources/__files/inbound/mockDataSkanmotovrig.zip");
let fd = fs.openSync("../core/src/test/resources/__files/inbound/mockDataSkanmotovrig.zip", "r");

function* fileGenerator() {
    yield "zip1.zip";
    yield "zip2.zip";
    yield "zip3.zip";
}

let files = fileGenerator();

function getLongName(name) {
    return `returndrwxrwxrwx 1 dok 0 1 Jan 1 1970 ${name}`
}

new ssh2.Server({
    hostKeys: [key],
    //privateKeyFile: "../core/src/test/resources/sftp/itest_privatekey"
}, function(client) {
    console.log('Client connected!');

    client.on('authentication', function(ctx) {
        ctx.accept();
    }).on('ready', function() {
        console.log('Client authenticated!');

        client.on('session', function(accept, reject) {
            let session = accept();
            session.on('sftp', function(accept, reject) {
                console.log('Client SFTP session');
                let openFiles = {};
                let sftpStream = accept();


                sftpStream.on('REALPATH', function (reqId, path) {
                    console.log(`REALPATH req:[${reqId}], path:[${path}]`);

                    sftpStream.name(reqId, [
                        { filename: path, longname: getLongName(path) },
                    ]);
                });

                sftpStream.on('SETSTAT', function (reqId, path, attrs) {
                    console.log(attrs)
                    console.log(`SETSTAT req:[${reqId}], path:[${path}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK)
                });

                sftpStream.on('OPENDIR', function (reqId, path) {
                    console.log(`OPENDIR req:[${reqId}], path:[${path}]`);

                    sftpStream.handle(reqId, Buffer.from('root_dir'));
                });

                sftpStream.on('FSETSTAT', function (reqId, handle, attrs) {
                    console.log(`FSETSTAT req:[${reqId}], path:[${handle}]`);
                });


                sftpStream.on('STAT', function (reqId, path, attrs) {
                    console.log(attrs)
                    console.log(`STAT req:[${reqId}], path:[${path}]`);
                    sftpStream.attrs(reqId, getDirStats());

                });
                sftpStream.on('LSTAT', function (reqId, path) {
                    console.log(`LSTAT req:[${reqId}], path:[${path}]`);
                    sftpStream.attrs(reqId, getDirStats());
                });

                sftpStream.on('OPEN', function (reqId, filename, flags, attrs, ...rest) {
                    console.log(`REALPATH req:[${reqId}], path:[${filename}], flags:[${flags}] attrs:[${attrs}]`);
                    let uuid = uuidv4();
                    console.log("attrs", attrs);
                    openFiles[uuid] = {done: false, flags: flags, offset: 0, ...attrs}
                    sftpStream.handle(reqId, new Buffer( uuid ));
                    console.log('Opening file for read')
                });


                sftpStream.on('READ', function(reqid, handle, offset, length) {

                    let stats = fs.statSync(ZIPPATH);
                    if(offset >= stats.size){
                        sftpStream.status(reqid, STATUS_CODE.EOF);
                    }else{
                        let remainder = stats.size - offset;
                        let bufferSize = length > remainder? remainder : length;
                        let buffer = new Buffer(bufferSize);

                        let fd = fs.openSync(ZIPPATH, 'r');
                        fs.readSync(fd, buffer, 0, bufferSize, offset);

                        console.log(`fd=${fd}, length==${bufferSize}, offset=${offset}, fileSize=${stats.size}, remainder=${remainder}`);
                        sftpStream.data(reqid, buffer);
                        fs.closeSync(fd);
                    }
                });

                sftpStream.on('CLOSE', function (reqId, handle) {
                    console.log(`CLOSE req:[${reqId}], handle:[${handle}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK);
                });

                sftpStream.on('WRITE', function (reqId, handle, offset, data) {
                    console.log(`WRITE req:[${reqId}], handle:[${handle}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK);
                });


                sftpStream.on('READDIR', function (reqId, handle) {
                    console.log(`READDIR req:[${reqId}], path:[${handle}]`);

                    let file = files.next();
                    if(file.done !== true){
                        sftpStream.name(reqId, [
                            { filename: file.value, attrs: {}, longname: getLongName(file.value) },
                        ]);
                    } else {
                        files = fileGenerator();
                        console.log(`Sending EOF...`);
                        sftpStream.status(reqId, STATUS_CODE.EOF);
                    }
                });

                sftpStream.on('REMOVE', function (reqId, handle) {
                    console.log(`REMOVE req:[${reqId}], path:[${handle}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK);
                });

                sftpStream.on('RMDIR', function (reqId, handle) {
                    console.log(`RMDIR req:[${reqId}], path:[${handle}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK);
                });

                sftpStream.on('RENAME', function (reqId, handle) {
                    try{
                        console.log(`RENAME req:[${reqId}], path:[${handle}]`);
                        sftpStream.status(reqId, STATUS_CODE.OK);
                    } catch (e) {
                        console.trace(e);
                    }
                });

                function onSTAT(reqid, path) {
                    var mode = constants.S_IFREG; // Regular file
                    mode |= constants.S_IRWXU; // read, write, execute for user
                    mode |= constants.S_IRWXG; // read, write, execute for group
                    mode |= constants.S_IRWXO; // read, write, execute for other
                    sftpStream.attrs(reqid, {
                        mode: mode,
                        uid: 0,
                        gid: 0,
                        size: 3,
                        atime: Date.now(),
                        mtime: Date.now()
                    });
                }
            });
        });
    }).on('end', function() {
        console.log('Client disconnected');
    });
}).listen(3001, 'localhost', function() {
    console.log('Listening on port ' + 3001);
});
