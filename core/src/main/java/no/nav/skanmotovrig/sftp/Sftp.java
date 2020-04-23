package no.nav.skanmotovrig.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KnownHosts;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.MottaDokumentUtgaaendeSkanningFunctionalException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Base64;


@Slf4j
public class Sftp{
    private JSch jsch;
    private Session jschSession;
    private ChannelSftp channelSftp;
    private String homePath;

    private String host;
    private String username;
    private String port;
    private String privateKey;/* =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIIEpAIBAAKCAQEAxYn197yrBYN4lTgdJN/8DEB/F4Rx9yrG/LehFwyjHAS0O1Fw\n" +
                    "jOazsY1DRLj/FYHKecrMB9b1P/N5mq9PjbxISWrtIhYTU5bLjvktOhZNU7TIB6K5\n" +
                    "lz07lXda3wwa/Owy0r7dNZgpx5+N+YSI/oSCcBoTUmoFDva0pOZ5k/XwLHlrojMp\n" +
                    "FOhIOzoZ5GP12L6lZRRG42Y5J7DMv0JF/0jaBuplvtDVgpqmebzOXAqKezuW4DF/\n" +
                    "0Jl7J1Vhs15HJyKc9vrcfEtuMg7BVkrlvGS1QGaKflbykMeyC27o9rkZ7A72I4a5\n" +
                    "abxrB1Zeaq73+Y3cBJFiXTUCod2FBifzIu5weQIDAQABAoIBAQC3dCi6qr0poa+a\n" +
                    "5UIrvNiXiE5/yMEOiuvCR8eTYGka3EOF01xzfs3jVw3iBOzhTGh+M5jIrxmVCskk\n" +
                    "nyuCthlsFaGkxlushZ8GaONXzt6BnqMmdDln+7YaWX5LJzLpMXMBxcAqxeYHPmbc\n" +
                    "E/omZWm+Mnk/ULbh7rxVaLYAc6dZpMZra9zR0P26R8y/lIg4adv+VBPFQmIbG2mS\n" +
                    "QqY29bBU2fUS6Gu9JR0PuEBDUdo2QBgszcjeqsPuHe3UYNDXjVJY5bhC7r2OiJZs\n" +
                    "Zur8A66o7dpLwMDTsuX+BesP6cmRiEjLIb1R8kFOmvhD/6cb8v0RthEvPsHsNSdH\n" +
                    "bG/jKRsBAoGBAPe10Z98hFsQAjyi/ZghSPpUHjMZbBVVJ5wJ90effpiAcBuB/Xqm\n" +
                    "2B2hRH0ZJyxzLppcB3KozHC8WGvohykBi6bcYI3RdlKiNKUuGlK1lXArdvVcGWlS\n" +
                    "LAP0Re7L/tPdyg2nLRGxhl6gGoCTiuI33yXQeLuf09tU6WcBS4GKKI+/AoGBAMwm\n" +
                    "UIs4XatwYHuCIn9PnFiGZS1BZ4jp90T5ezXasVM/XkCgkjdvvmLvzMalmtS11kez\n" +
                    "YdE1T/2pTED0O+hXQ1YLStLwHpg1YNGjOI06B1nkF62oLvJpdgUaNKYWc16SXdUF\n" +
                    "ukJJX7Ek5OOh4BFVW7IlWE17F3UvYVOaLcaYZg3HAoGAOHE4a4O+0NUL/W8SI5jp\n" +
                    "/QgvvWr8grvdg6ONscc43FzfrpTnAgyET/QQHAUgNPOl2cxAcjLeKo3wA95+9awB\n" +
                    "eyrZ4VaqBFgrcLvZiaEQhPeSaoPq0mHD377INIpM7U+rG4NNNKvjtyn5//QegD9E\n" +
                    "3GPtLqtTZbWqXSshXknxxhcCgYAmAagTEy5VIXnE5KhZfU/FJ1gkwb1tvuka/TtT\n" +
                    "/l/u+KgqbfpqubH/J1e/T8tweF3pQVsfoMZAmkko+o9ApSZTGY0Xkj6P5bgdrz5z\n" +
                    "BG+j65QA74O1+65OKt/MX/egjq1LYGKZvqFDEHRQcK2PbI3Cr7Mt0ZG/bk+3Scfl\n" +
                    "5i9xtwKBgQDE+/sOBq736NnT4Rw9MjWvywwH9T+fN75WMzXhgmraFtLUn0ZvqNEg\n" +
                    "G7KRsOSPv8DIP7ljrgLC9VnMbLdWPigOCC1b5tG81HuGz2z/llYcdJ9eb9+Z7Qnd\n" +
                    "gcqfgp43SCrL/Rkc12v2/7wVW1Fv3tlTbN0OFq+mR8BNmxBN2tWa8Q==\n" +
                    "-----END RSA PRIVATE KEY-----";*/
    private String publicKey;

    Sftp(String host, String username, String port, String privateKey, String publicKey) throws JSchException {
        this.host = host;
        this.username = username;
        this.port = port;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public List<String> listFiles() throws SftpException {
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error("Must be connected to list files");
            throw new MottaDokumentUtgaaendeSkanningFunctionalException("Must be connected to list files", new Exception());
        }
        try {
            Vector<LsEntry> vector = channelSftp.ls("*");
            return vector.stream().map(ChannelSftp.LsEntry::getFilename).collect(Collectors.toList());
        } catch (SftpException e) {
            log.error("Listing files failed", e);
            throw e;
        }
    }

    public String presentWorkingDirectory() throws SftpException {
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error("Must be connected to get present working directory");
            throw new MottaDokumentUtgaaendeSkanningFunctionalException("Must be connected to get present working directory", new Exception());
        }
        try {
            return channelSftp.pwd();
        } catch(SftpException e) {
            log.error("Getting present working directory failed " + e);
            throw e;
        }
    }

    public void changeDirectory(String path) throws SftpException {
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error("Must be connected to change directory");
            throw new MottaDokumentUtgaaendeSkanningFunctionalException("Must be connected to change directory", new Exception());
        }
        try {
            channelSftp.cd(path);
        } catch(SftpException e) {
            log.error("Changing directory failed, path:  "+ path, e);
            throw e;
        }
    }

    public InputStream getFile(String filename) throws SftpException {
        if(channelSftp == null || !channelSftp.isConnected()){
            log.error("Must be connected to get file");
            throw new MottaDokumentUtgaaendeSkanningFunctionalException("Must be connected to get file", new Exception());
        }
        try {
            return channelSftp.get(filename);
        } catch (SftpException e) {
            log.error("Downloading "+filename+" failed", e);
            throw e;
        }
    }

    public boolean isConnected() {
        return channelSftp.isConnected();
    }

    public void connect() {
        try{
            jsch = new JSch();
            jschSession = jsch.getSession(username, host, Integer.parseInt(port));
            jsch.addIdentity(null, privateKey.getBytes(), null, null);
            jsch.setKnownHosts(new ByteArrayInputStream(publicKey.getBytes()));
            jschSession.connect();

            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect();
            setHomePath(channelSftp.getHome());
        } catch (JSchException | SftpException e) {
            log.error("ERROR OCCURED WHILE CONNECTING TO SFTP", e);
        }
    }
    public void disconnect() {
        if (channelSftp.isConnected()) {
            try {
                //channelSftp.disconnect();
                //channelSftp.quit();
                channelSftp.exit();
                jschSession.disconnect();
                log.info("DISCONNECTED FROM SFTP");
            } catch (Exception e) {
                log.error("ERROR OCCURED WHILE DISCONNECTING FROM SFTP", e);
            }
        } else {
            log.warn("SFTP WAS NOT CONNECTED");
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
