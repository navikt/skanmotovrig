package no.nav.skanmotovrig.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigSftpFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
public class Sftp{
    private String APPLICATION = "Skanmotovrig";

    private Session jschSession;
    private ChannelSftp channelSftp;
    private String homePath;

    private String host;
    private String username;
    private String port;
    private String privateKey;
    private String hostKey;

    Sftp(String host, String username, String port, String privateKey, String hostKey) {
        this.host = host;
        this.username = username;
        this.port = port;
        this.privateKey = privateKey;
        this.hostKey = hostKey;
    }

    public List<String> listFiles() throws SftpException {
        return listFiles("*");
    }

    public List<String> listFiles(String path){
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error(APPLICATION + " must be connected to list files");
            throw new SkanmotovrigSftpFunctionalException("must be connected to list files", new Exception());
        }
        try {
            Vector<LsEntry> vector = channelSftp.ls(path);
            return vector.stream().map(ChannelSftp.LsEntry::getFilename).collect(Collectors.toList());
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to list files, path: " + path, e);
            throw new SkanmotovrigSftpTechnicalException("failed to list files, path: " + path, e);
        }
    }

    public String presentWorkingDirectory(){
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error(APPLICATION + " Must be connected to get present working directory");
            throw new SkanmotovrigSftpFunctionalException("must be connected to get present working directory", new Exception());
        }
        try {
            return channelSftp.pwd();
        } catch(SftpException e) {
            log.error(APPLICATION + " failed to get present working directory", e);
            throw new SkanmotovrigSftpTechnicalException("failed to get present working directory", e);
        }
    }

    public void changeDirectory(String path){
        if(channelSftp == null || !channelSftp.isConnected()){
            log.warn(APPLICATION + " must be connected to change directory");
            throw new SkanmotovrigSftpFunctionalException("must be connected to change directory", new Exception());
        }
        try {
            channelSftp.cd(path);
        } catch(SftpException e) {
            log.error(APPLICATION + " failed to change directory, path: " + path, e);
            throw new SkanmotovrigSftpTechnicalException("failed to change directory, path: " + path, e);
        }
    }

    public InputStream getFile(String filename){
        if(channelSftp == null || !channelSftp.isConnected()){
            log.warn(APPLICATION + " must be connected to get file");
            throw new SkanmotovrigSftpFunctionalException("Must be connected to get file", new Exception());
        }
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to download " + filename, e);
            throw new SkanmotovrigSftpTechnicalException("failed to download " + filename, e);
        }
    }

    public boolean isConnected() {
        return channelSftp.isConnected();
    }

    public void connect() {

        try{
            JSch jsch = new JSch();
            jschSession = jsch.getSession(username, host, Integer.parseInt(port));
            jsch.addIdentity(privateKey);
            jsch.setKnownHosts(hostKey);

            jschSession.connect();

            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect();
            setHomePath(channelSftp.getHome());
        } catch (JSchException | SftpException e) {
            log.error(APPLICATION + " failed to connect to " + host, e);
            throw new SkanmotovrigSftpTechnicalException("failed to connect to " + host, e);
        }
    }

    public void disconnect() {
        if (channelSftp.isConnected()) {
            try {
                channelSftp.exit();
                jschSession.disconnect();
                log.info(APPLICATION + " disconnected from " + host);
            } catch (Exception e) {
                log.error(APPLICATION + " failed to disconnect from " + host, e);
                throw new SkanmotovrigSftpTechnicalException("failed to connect to " + host, e);
            }
        } else {
            log.warn(APPLICATION + " tried to disconnect while not connected");
        }
    }

    public String getHomePath() {
        return homePath;
    }

    // A bit hacky, but ChannelSftp does not handle windows paths very well.
    public void setHomePath(String homePath) {
        Pattern windowsFileSystemPattern = Pattern.compile("^[a-zA-Z]:/");
        Matcher windowsFileSystemMatcher = windowsFileSystemPattern.matcher(homePath);
        if (windowsFileSystemMatcher.find()) {
            this.homePath = homePath.substring(2);
        } else {
            this.homePath = homePath;
        }
    }
}
