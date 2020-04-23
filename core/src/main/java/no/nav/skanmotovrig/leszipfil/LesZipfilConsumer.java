package no.nav.skanmotovrig.leszipfil;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.nav.skanmotovrig.sftp.Sftp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@Component
public class LesZipfilConsumer {
    /*@Value("${skanmotovrig.sftp.host}")
    private String sftpHost;
    @Value("${skanmotovrig.sftp.directory}")
    private String sftpDirectory;
    @Value("${skanmotovrig.sftp.username}")
    private String sftpUsername;
    @Value("${skanmotovrig.sftp.password}")
    private String sftpPassword;
    @Value("${skanmotovrig.sftp.port}")
    private String sftpPort;*/


    public File hentZipfil() {


        // TODO: Hent zipfil bestående av par av pdf'er og xml'er med metadata fra skyfilområde
        return new File("core/src/main/resources/tmp/__files/SKAN_NETS.zip");
    }

    @Inject
    public List<String> listZipFiles(Sftp sftp) throws Exception {
        try{
            sftp.connect();
            List<String> files = sftp.listFiles();
            sftp.disconnect();
            return files;
        } catch(Exception e) {
            throw e;
        }
    }

}
