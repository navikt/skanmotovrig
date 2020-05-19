package no.nav.skanmotovrig.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigSftpFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigSftpTechnicalException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Sftp{
    private String APPLICATION = "Skanmotovrig";

    private JSch jsch = new JSch();
    private Session jschSession;
    private ChannelSftp channelSftp;
    private String homePath;

    private String host;
    private String username;
    private String port;
    private String privateKey;
    private String hostKey;
    private int timeout;

    Sftp(String host, String username, String port, String privateKey, String hostKey) {
        this.host = host;
        this.username = username;
        this.port = port;
        this.privateKey = privateKey;
        this.hostKey = hostKey;
    }

    public Sftp(SkanmotovrigProperties properties) {
        this.host = properties.getSftp().getHost();
        this.username = properties.getSftp().getUsername();
        this.port = properties.getSftp().getPort();
        this.privateKey = properties.getSftp().getPrivateKey();
        this.hostKey = properties.getSftp().getHostKey();
        this.timeout = Integer.parseInt(properties.getSftp().getTimeout()) * 100;
    }

    public List<String> listFiles() {
        return listFiles("*");
    }

    public List<String> listFiles(String path){
        if(channelSftp == null || !channelSftp.isConnected()){
            connect();
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
            connect();
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
            connect();
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
            connect();
            //log.warn(APPLICATION + " must be connected to get file");
            //throw new SkanmotovrigSftpFunctionalException("Must be connected to get file", new Exception());
        }
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            log.error(APPLICATION + " failed to download " + filename, e);
            throw new SkanmotovrigSftpTechnicalException("failed to download " + filename, e);
        }
    }

    public boolean isConnected() {
        return channelSftp.isConnected() && jschSession.isConnected();
    }

    public void connect() {
        try{
            jschSession = jsch.getSession(username, host, Integer.parseInt(port));
            jsch.addIdentity(privateKey);
            jsch.setKnownHosts(hostKey);

            jschSession.connect();
            //jschSession.setServerAliveInterval(timeout);
            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect();
            setHomePath(channelSftp.getHome());
        } catch (JSchException | SftpException e) {
            log.error(APPLICATION + " failed to connect to " + host, e);
            throw new SkanmotovrigSftpTechnicalException("failed to connect to " + host, e);
        } catch (Exception e) {
            e.printStackTrace();
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
        if(channelSftp == null || !channelSftp.isConnected()){
            connect();
        }
        return homePath;
    }


    public void deleteFile(String directory, String filename) {
        String filePath = directory + "/" + filename;
        try {
            channelSftp.rm(filePath);
        } catch (SftpException e) {
            log.error("{} klarte ikke slette {}", APPLICATION, filePath, e);
            throw new SkanmotovrigSftpTechnicalException("Klarte ikke slette " + filePath, e);
        }
    }

    public void uploadFile(InputStream file, String path, String filename) {
        createDirectoryIfNotExisting(path);
        try {
            channelSftp.put(file, path + "/" + filename);
        } catch (SftpException e) {
            log.error("{} klarte ikke laste opp fil {} til {}", APPLICATION, filename, path, e);
            throw new SkanmotovrigSftpTechnicalException("Klarte ikke laste opp fil", e);
        }
    }

    public void moveFile(String from, String to, String newFilename) {
        try {
            createDirectoryIfNotExisting(to);
            channelSftp.rename(from, to + "/" + newFilename);
        } catch (SftpException e) {
            log.error("{} klarte ikke flytte fil {} til {}", APPLICATION, from ,to);
            throw new SkanmotovrigSftpTechnicalException("Klarte ikke flytte fil", e);
        }
    }

    private void createDirectoryIfNotExisting(String path) {
        try {
            channelSftp.lstat(path);
        } catch (SftpException mappeFinnesIkke) {
            // Path finnes ikke, så vi lager den. Kan bare lage en og en mappe
            String existingPath = "";
            for (String subPath : path.split("/")) {
                try {
                    channelSftp.lstat(existingPath + subPath);
                    existingPath += subPath + "/";
                } catch (SftpException delmappeFinnesIkke) {
                    try {
                        channelSftp.mkdir(existingPath + subPath);
                        existingPath += subPath + "/";
                    } catch (SftpException e) {
                        log.error("{} klarte ikke lage en ny mappe: {}", APPLICATION, path, e);
                        throw new SkanmotovrigSftpTechnicalException("Klarte ikke lage en ny mappe: " + path, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("{} klarte ikke lage en ny mappe: {}", APPLICATION, path, e);
            throw new SkanmotovrigSftpTechnicalException("Klarte ikke lage en ny mappe: " + path, e);
        }
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
