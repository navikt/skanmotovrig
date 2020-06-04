package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class HelseRoute extends RouteBuilder {
    static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    static final String PROPERTY_FORSENDELSE_XML = "ForsendelseXml";
    static final String HEADER_FORSENDELSE_FILE_EXTENSION = "ForsendelseFileExtension";
    private static final int FORVENTET_ANTALL_PER_FORSENDELSE = 3;

    private final HelseService helseService;

    @Inject
    public HelseRoute(HelseService helseService) {
        this.helseService = helseService;
    }

    @Override
    public void configure() throws Exception {
        // Kjente funksjonelle feil
        onException(AbstractSkanmotovrigFunctionalException.class)
                .handled(true)
                .log(LoggingLevel.WARN, log, "${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-funksjoneltavvik.zip"))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.feilmappe}}" +
                        "?{{skanmotovrig.helse.endpointconfig}}")
                .log(LoggingLevel.WARN, log, "Skrev til feilmappe ${header." + Exchange.FILE_NAME_PRODUCED + "}.");

        from("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.inngaaendemappe}}" +
                "?{{skanmotovrig.helse.endpointconfig}}" +
                "&delay=10000" +
                "&antInclude=*.zip,*.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=1")
                .routeId("read_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Starter behandling av ${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .split(new ZipSplitter()).streaming()
                .aggregate(simple("${file:name.noext}"), new HelseSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(500)
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(exchange -> exchange.getIn().getBody(HelseforsendelseEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .to("direct:process_helse")
                .end() // aggregate
                .end() // split
                .log(LoggingLevel.INFO, log, "Behandlet ferdig ${file:absolute.path}.");

        from("direct:process_helse")
                .routeId("process_helse")
                .log(LoggingLevel.INFO, log, "Behandler ${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.")
                .bean(helseService)
                .log(LoggingLevel.INFO, log, "Journalførte ${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "} som journalpostId=${body}");
    }
}
