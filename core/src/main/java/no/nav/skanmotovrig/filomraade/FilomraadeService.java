package no.nav.skanmotovrig.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import no.nav.skanmotovrig.exceptions.functional.LesZipFilFuntionalException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Service
public class FilomraadeService {

    private FilomraadeConsumer filomraadeConsumer;

    @Inject
    public FilomraadeService(FilomraadeConsumer filomraadeConsumer) {
        this.filomraadeConsumer = filomraadeConsumer;
    }

    public List<String> getFileNames() throws LesZipFilFuntionalException, SkanmotovrigSftpTechnicalException {
        return filomraadeConsumer.listZipFiles();
    }
/*
    public Map<String, byte[]> getZipFiles() throws SkanmotovrigSftpTechnicalException {
        try {
            List<String> fileNames = lesZipfilConsumer.listZipFiles();
            Map<String, byte[]> files = new HashMap<>();

            for (String filename : fileNames) {
                byte[] zipFile = getZipFile(filename);
                if (null != zipFile) {
                    files.put(filename, zipFile);
                }
            }
            log.info("Skanmotutgaaende leser {} fra sftp", fileNames.toString());
            return files;
        } catch (LesZipFilFuntionalException e) {
            log.warn("Skanmotutgaaende klarte ikke hente zipfiler");
            throw e;
        } catch (Exception e) {
            log.warn("Skanmotutgaaende klarte ikke koble til sftp");
            throw new SkanmotovrigSftpTechnicalException("Klarte ikke koble til sftp", e);
        }
    }
 */

    public void uploadFileToFeilomrade(byte[] file, String filename, String path) {
        try {
            filomraadeConsumer.uploadFileToFeilomrade(new ByteArrayInputStream(file), filename, path);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke laste opp fil {}", filename, e);
        }
    }


    public void deleteZipFiles(List<String> zipFiles) {
        zipFiles.stream().forEach(this::deleteZipFile);
    }

    public void moveZipFiles(List<String> files, String destination) {
        files.stream().forEach(file -> {
            moveFile(file, destination, file + ".processed");
        });
    }

    public void moveZipFile(String file, String destination) {
        moveFile(file, destination, file + ".processed");
    }

    private void deleteZipFile(String filename) {
        try {
            filomraadeConsumer.deleteFile(filename);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke slette fil {}", filename, e);
        }
    }

    private void moveFile(String from, String to, String newFilename) {
        try {
            filomraadeConsumer.moveFile(from, to, newFilename);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke flytte fil {} til {}/{}", from, to, newFilename, e);
        }
    }

    public byte[] getZipFile(String fileName) {
        try {
            return filomraadeConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Skanmotutgaaende klarte ikke hente filen {}", fileName, e);
            return null;
        }
    }
}
