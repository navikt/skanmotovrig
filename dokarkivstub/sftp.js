const fs = require('fs');
const crypto = require('crypto');
const constants = require('constants');
const ssh2 = require('ssh2');
const { v4: uuidv4 } = require('uuid');

const OPEN_MODE = ssh2.SFTP_OPEN_MODE;
const STATUS_CODE = ssh2.SFTP_STATUS_CODE;

var ROOT_DIR_HANDLE = Buffer.from('rood_dir');
var ReadDirs = 0;

var dirUdid = 0;

let writeTimeout;

function getDirStats() {
    return {
        mode: constants.S_IFDIR,
        uid: dirUdid++,
        gid: dirUdid,
        size: 0,
        atime: new Date(0),
        mtime: new Date(0)
    }
}
let key = fs.readFileSync("../core/src/test/resources/sftp/itest_privatekey", {encoding: 'utf8'});
let zipBuffer = fs.readFileSync("../core/src/test/resources/__files/inbound/mockDataSkanmotovrig.zip");

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
                        { filename: '/', longname: getLongName('root') },
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
                    console.log(attrs)
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

                sftpStream.on('READ', function (reqId, handle, offset, length) {
                    console.log(`READ req:[${reqId}], handle:[${handle}], offset:[${offset}], length[${length}]`);
                    let end = (offset + length) > zipBuffer.length ? zipBuffer.length : offset + length;
                    console.log(zipBuffer.length, offset, length, end);
                    if(offset < end){
                        console.log("READ sending data");
                       //let buffer = Buffer(end-offset);
                        //zipBuffer.copy(buffer, 0, offset, end);

                            //Buffer.from(zipBuffer.slice(offset, end))


                        sftpStream.data(reqId, "buffer");
                    }
                    if(end === zipBuffer.length){
                        console.log("READ sending EOF");
                        sftpStream.status(reqId, STATUS_CODE.EOF)
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
                    console.log(`RENAME req:[${reqId}], path:[${handle}]`);
                    sftpStream.status(reqId, STATUS_CODE.OK);
                });



                /*
                REMOVE(< integer >reqID, < string >path)

Respond using:

status() - Use this to indicate success/failure of the removal of the file at path.
RMDIR(< integer >reqID, < string >path)

Respond using:

status() - Use this to indicate success/failure of the removal of the directory at path.
                 */

                /*
                READDIR(< integer >reqID, < Buffer >handle)

Respond using one of the following:

name() - Use this to send one or more directory listings for the open directory back to the client.

status() - Use this to indicate either end of directory contents (STATUS_CODE.EOF) or if an error occurred while reading the directory contents.

                 */
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
