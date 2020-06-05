package no.nav.skanmotovrig.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import no.nav.skanmotovrig.sftp.Sftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class FilomraadeConsumer {

    private Sftp sftp;
    private String inboundDirectory;
    private String feilDirectory;

    @Autowired
    public FilomraadeConsumer(Sftp sftp, SkanmotovrigProperties skanmotovrigProperties) {
        this.sftp = sftp;
        this.inboundDirectory = skanmotovrigProperties.getOvrig().getFilomraade().getInngaaendemappe();
        this.feilDirectory = skanmotovrigProperties.getOvrig().getFilomraade().getFeilmappe();
    }

    public List<String> listZipFiles() {
        try {
            log.info("Skanmotovrig henter zipfiler fra {}", sftp.getHomePath() + inboundDirectory);
            List<String> files = sftp.listFiles(inboundDirectory + "/*.zip");
            return files;
        } catch (Exception e) {
            throw new LesZipFilFuntionalException("Skanmotovrig klarte ikke hente zipfiler", e);
        }
    }

    public byte[] getFile(String filename) throws SkanmotovrigSftpTechnicalException, IOException {
        InputStream fileStream = sftp.getFile(inboundDirectory + "/" + filename);
        byte[] file = fileStream.readAllBytes();
        fileStream.close();
        return file;
    }


    public void deleteFile(String filename) {
        log.info("Skanmotovrig sletter fil {}", filename);
        sftp.deleteFile(inboundDirectory, filename);
        log.info("Skanmotovrig slettet fil {}", filename);
    }

    public void uploadFileToFeilomrade(InputStream file, String filename, String path) {
        log.info("Skanmotovrig laster opp fil {} til feilområde", filename);
        sftp.uploadFile(file, feilDirectory + "/" + path, filename);
        log.info("Skanmotovrig har lastet opp fil {} til feilområde", filename);
    }

    public void moveFile(String from, String to, String newFilename) {
        String fromPath = inboundDirectory + "/" + from;
        String toPath = inboundDirectory + "/" + to;
        log.info("Skanmotovrig flytter fil {} til {}", fromPath, toPath);
        sftp.moveFile(fromPath, toPath, newFilename);
        log.info("Skanmotovrig flyttet fil {} til {}", fromPath, toPath);
    }

    public void disconnect() {
        sftp.disconnect();
    }
}
