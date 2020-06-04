package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.mdc.MDCConstants;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class HelseRoute extends RouteBuilder {
    public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    static final String HEADER_FORSENDELSE_FILE_EXTENSION = "ForsendelseFileExtension";
    static final int FORVENTET_ANTALL_PER_FORSENDELSE = 3;

    private final HelseService helseService;

    @Inject
    public HelseRoute(HelseService helseService) {
        this.helseService = helseService;
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .handled(true)
                .process(new MdcProcessor())
                .log(LoggingLevel.WARN, log, "Skanmothelse feilet teknisk for forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}. ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.WARN, log, "Skanmothelse skrev forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "} til feilmappe. fil=${header." + Exchange.FILE_NAME_PRODUCED + "}.")
                .process(exchange -> MDC.remove(MDCConstants.MDC_FILENAME));

        // Kjente funksjonelle feil
        onException(AbstractSkanmotovrigFunctionalException.class)
                .handled(true)
                .process(new MdcProcessor())
                .log(LoggingLevel.WARN, log, "Skanmothelse feilet funksjonelt for forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}. ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-funksjonelt.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.WARN, log, "Skanmothelse skrev forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "} til feilmappe. fil=${header." + Exchange.FILE_NAME_PRODUCED + "}.")
                .process(exchange -> MDC.remove(MDCConstants.MDC_FILENAME));

        from("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.inngaaendemappe}}" +
                "?{{skanmotovrig.helse.endpointconfig}}" +
                "&delay=" + TimeUnit.SECONDS.toMillis(60) +
                "&antInclude=*.zip,*.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=1" +
                "&idempotent=true" +
                "&move=processed" +
                "&jailStartingDirectory=false")
                .routeId("read_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Skanmothelse starter behandling av fil=${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .process(new MdcProcessor())
                .split(new ZipSplitter()).streaming()
                .aggregate(simple("${file:name.noext}"), new HelseSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(500)
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(new MdcProcessor())
                .process(exchange -> exchange.getIn().getBody(HelseforsendelseEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .to("direct:process_helse")
                .end() // aggregate
                .end() // split
                .process(exchange -> MDC.remove(MDCConstants.MDC_FILENAME))
                .log(LoggingLevel.INFO, log, "Skanmothelse behandlet ferdig fil=${file:absolute.path}.");

        from("direct:process_helse")
                .routeId("process_helse")
                .log(LoggingLevel.INFO, log, "Skanmothelse behandler forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.")
                .bean(helseService)
                .log(LoggingLevel.INFO, log, "Skanmothelse journalførte forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "} som journalpostId=${body}");

        from("direct:avvik")
                .routeId("avvik")
                .choice().when(body().isInstanceOf(HelseforsendelseEnvelope.class))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.feilmappe}}" +
                        "?{{skanmotovrig.helse.endpointconfig}}")
                .otherwise()
                .log(LoggingLevel.ERROR, log, "Skanmothelse teknisk feil der forsendelse=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "} ikke ble flyttet til feilområde. Må analyseres.")
                .end();
    }
}
