package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.config.properties.SkanmotovrigProperties;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class HelseRoute extends RouteBuilder {

    @Inject
    public HelseRoute() {

    }

    @Override
    public void configure() throws Exception {
        from("sftp://{{skanmotovrig.sftp.host}}:{{skanmotovrig.sftp.port}}/{{skanmotovrig.helse.inngaaendemappe}}" +
                "?username={{skanmotovrig.sftp.username}}&password=" +
                "&preferredAuthentications=publickey&privateKeyFile={{skanmotovrig.sftp.privateKey}}" +
                "&strictHostKeyChecking=yes&knownHostsFile={{skanmotovrig.sftp.hostKey}}" +
                "&binary=true" +
                "&antInclude=*.zip" +
                "&initialDelay=1000" +
                "&noop=true" +
                "&maxMessagesPerPoll=1")
                .routeId("read_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Starter behandling av ${file:absolute.path}.");
    }
}
