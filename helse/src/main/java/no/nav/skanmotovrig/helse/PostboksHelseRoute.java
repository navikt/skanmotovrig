package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksHelseRoute extends RouteBuilder {
    public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
    public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
    static final String HEADER_FORSENDELSE_FILE_EXTENSION = "ForsendelseFileExtension";
    static final int FORVENTET_ANTALL_PER_FORSENDELSE = 3;

    private final PostboksHelseService postboksHelseService;

    @Inject
    public PostboksHelseRoute(PostboksHelseService postboksHelseService) {
        this.postboksHelseService = postboksHelseService;
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .log(LoggingLevel.ERROR, log, "Skanmothelse feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.ERROR, log, "Skanmothelse skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        // Kjente funksjonelle feil
        onException(AbstractSkanmotovrigFunctionalException.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .log(LoggingLevel.WARN, log, "Skanmothelse feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-funksjonelt.zip"))
                .to("direct:avvik")
                .log(LoggingLevel.WARN, log, "Skanmothelse skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        from("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.inngaaendemappe}}" +
                "?{{skanmotovrig.helse.endpointconfig}}" +
                "&delay=" + TimeUnit.SECONDS.toMillis(60) +
                "&antInclude=*.zip,*.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=1" +
                "&idempotent=true" +
                "&move=processed" +
                "&jailStartingDirectory=false"+
                "&scheduler=spring&scheduler.cron={{skanmotovrig.helse.schedule}}")
                .routeId("read_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Skanmothelse starter behandling av fil=${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .process(new MdcSetterProcessor())
                .split(new ZipSplitter()).streaming()
                .aggregate(simple("${file:name.noext}"), new PostboksHelseSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(TimeUnit.SECONDS.toMillis(1))
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(new MdcSetterProcessor())
                .process(exchange -> exchange.getIn().getBody(PostboksHelseforsendelseEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
                .to("direct:process_helse")
                .end() // aggregate
                .end() // split
                .process(new MdcRemoverProcessor())
                .log(LoggingLevel.INFO, log, "Skanmothelse behandlet ferdig fil=${file:absolute.path}.");

        from("direct:process_helse")
                .routeId("process_helse")
                .process(new MdcSetterProcessor())
                .log(LoggingLevel.INFO, log, "Skanmothelse behandler " + KEY_LOGGING_INFO + ".")
                .bean(postboksHelseService)
                .log(LoggingLevel.INFO, log, "Skanmothelse journalførte journalpostId=${body}. " + KEY_LOGGING_INFO + ".")
                .process(new MdcRemoverProcessor());

        from("direct:avvik")
                .routeId("avvik")
                .choice().when(body().isInstanceOf(PostboksHelseforsendelseEnvelope.class))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotovrig.helse.endpointuri}}/{{skanmotovrig.helse.filomraade.feilmappe}}" +
                        "?{{skanmotovrig.helse.endpointconfig}}")
                .otherwise()
                .log(LoggingLevel.ERROR, log, "Skanmothelse teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
                .end()
                .process(new MdcRemoverProcessor());
    }
}
