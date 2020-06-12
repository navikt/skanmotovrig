package no.nav.skanmotovrig.filomraade;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import no.nav.skanmotovrig.exceptions.functional.LesZipFilFuntionalException;
import no.nav.skanmotovrig.metrics.Metrics;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Slf4j
@Service
public class FilomraadeService {

    private FilomraadeConsumer filomraadeConsumer;

    @Inject
    public FilomraadeService(FilomraadeConsumer filomraadeConsumer) {
        this.filomraadeConsumer = filomraadeConsumer;
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "getFileNames", }, createErrorMetric = true)
    public List<String> getFileNames() throws LesZipFilFuntionalException, SkanmotovrigSftpTechnicalException {
        return filomraadeConsumer.listZipFiles();
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "uploadFileToFeilomrade", }, createErrorMetric = true)
    public void uploadFileToFeilomrade(byte[] file, String filename, String path) {
        try {
            filomraadeConsumer.uploadFileToFeilomrade(new ByteArrayInputStream(file), filename, path);
        } catch (Exception e) {
            log.error("Skanmotovrig klarte ikke laste opp fil {}", filename, e);
            throw e;
        }
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "moveZipFile", }, createErrorMetric = true)
    public void moveZipFile(String file, String destination) {
        moveFile(file, destination, file + ".processed");
    }

    private void deleteZipFile(String filename) {
        try {
            filomraadeConsumer.deleteFile(filename);
        } catch (Exception e) {
            log.error("Skanmotovrig klarte ikke slette fil {}", filename, e);
        }
    }

    private void moveFile(String from, String to, String newFilename) {
        try {
            filomraadeConsumer.moveFile(from, to, newFilename);
        } catch (Exception e) {
            log.error("Skanmotovrig klarte ikke flytte fil {} til {}/{}", from, to, newFilename, e);
        }
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "getZipFile", }, createErrorMetric = true)
    public byte[] getZipFile(String fileName) throws IOException {
        try {
            return filomraadeConsumer.getFile(fileName);
        } catch (Exception e) {
            log.error("Skanmotovrig klarte ikke hente filen {}", fileName, e);
            throw e;
        }
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "disconnect", }, createErrorMetric = true)
    public void disconnect() {
        filomraadeConsumer.disconnect();
    }
}
