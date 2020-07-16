package no.nav.skanmotovrig.ovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import no.nav.skanmotovrig.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksOvrigRoute extends RouteBuilder {
    public static final String PROPERTY_FORSENDELSE_ZIPNAME = "ForsendelseZipname";
    public static final String PROPERTY_FORSENDELSE_BATCHNAVN = "ForsendelseBatchNavn";
    public static final String PROPERTY_FORSENDELSE_FILEBASENAME = "ForsendelseFileBasename";
    public static final String KEY_LOGGING_INFO = "fil=${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}, batch=${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}";
    static final int FORVENTET_ANTALL_PER_FORSENDELSE = 2;

    private final PostboksOvrigService postboksOvrigService;

    @Inject
    public PostboksOvrigRoute(PostboksOvrigService postboksOvrigService) {
        this.postboksOvrigService = postboksOvrigService;
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(new ErrorMetricsProcessor())
                .log(LoggingLevel.ERROR, log, "Skanmotovrig feilet teknisk for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}-teknisk.zip"))
                .to("direct:avvik_ovrig")
                .log(LoggingLevel.ERROR, log, "Skanmotovrig skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        // Kjente funksjonelle feil
        onException(AbstractSkanmotovrigFunctionalException.class)
                .handled(true)
                .process(new MdcSetterProcessor())
                .process(new ErrorMetricsProcessor())
                .log(LoggingLevel.WARN, log, "Skanmotovrig feilet funksjonelt for " + KEY_LOGGING_INFO + ". ${exception}")
                .setHeader(Exchange.FILE_NAME, simple("${exchangeProperty." + PROPERTY_FORSENDELSE_BATCHNAVN + "}/${exchangeProperty." + PROPERTY_FORSENDELSE_FILEBASENAME + "}.zip"))
                .to("direct:avvik_ovrig")
                .log(LoggingLevel.WARN, log, "Skanmotovrig skrev feiletzip=${header." + Exchange.FILE_NAME_PRODUCED + "} til feilmappe. " + KEY_LOGGING_INFO + ".");

        from("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.inngaaendemappe}}" +
                "?{{skanmotovrig.ovrig.endpointconfig}}" +
                "&delay=" + TimeUnit.SECONDS.toMillis(60) +
                "&antInclude=*.zip,*.ZIP" +
                "&initialDelay=1000" +
                "&maxMessagesPerPoll=10" +
                "&move=processed" +
                "&jailStartingDirectory=false"+
                "&scheduler=spring&scheduler.cron={{skanmotovrig.ovrig.schedule}}")
                .routeId("read_ovrig_zip_from_sftp")
                .log(LoggingLevel.INFO, log, "Skanmotovrig starter behandling av fil=${file:absolute.path}.")
                .setProperty(PROPERTY_FORSENDELSE_ZIPNAME, simple("${file:name}"))
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${file:name.noext.single}"))
                .process(new MdcSetterProcessor())
                .split(new ZipSplitter()).streaming()
                .aggregate(simple("${file:name.noext.single}"), new PostboksOvrigSkanningAggregator())
                .completionSize(FORVENTET_ANTALL_PER_FORSENDELSE)
                .completionTimeout(TimeUnit.SECONDS.toMillis(1))
                .setProperty(PROPERTY_FORSENDELSE_FILEBASENAME, simple("${exchangeProperty.CamelAggregatedCorrelationKey}"))
                .process(new MdcSetterProcessor())
                .process(exchange -> DokCounter.incrementCounter("antall_innkommende", List.of(DokCounter.DOMAIN, DokCounter.OVRIG)))
                .process(exchange -> exchange.getIn().getBody(PostboksOvrigEnvelope.class).validate())
                .bean(new SkanningmetadataUnmarshaller())
                .bean(new SkanningmetadataCounter())
                .setProperty(PROPERTY_FORSENDELSE_BATCHNAVN, simple("${body.skanningmetadata.journalpost.batchnavn}"))
                .to("direct:process_ovrig")
                .end() // aggregate
                .end() // split
                .process(new MdcRemoverProcessor())
                .log(LoggingLevel.INFO, log, "Skanmotovrig behandlet ferdig fil=${file:absolute.path}.");

        from("direct:process_ovrig")
                .routeId("process_ovrig")
                .process(new MdcSetterProcessor())
                .log(LoggingLevel.INFO, log, "Skanmotovrig behandler " + KEY_LOGGING_INFO + ".")
                .bean(postboksOvrigService)
                .log(LoggingLevel.INFO, log, "Skanmotovrig journalførte journalpostId=${body}. " + KEY_LOGGING_INFO + ".")
                .process(exchange -> DokCounter.incrementCounter("antall_vellykkede", List.of(DokCounter.DOMAIN, DokCounter.OVRIG)))
                .process(new MdcRemoverProcessor());

        from("direct:avvik_ovrig")
                .routeId("avvik_ovrig")
                .choice().when(body().isInstanceOf(PostboksOvrigEnvelope.class))
                .setBody(simple("${body.createZip}"))
                .to("{{skanmotovrig.ovrig.endpointuri}}/{{skanmotovrig.ovrig.filomraade.feilmappe}}" +
                        "?{{skanmotovrig.ovrig.endpointconfig}}")
                .otherwise()
                .log(LoggingLevel.ERROR, log, "Skanmotovrig teknisk feil der " + KEY_LOGGING_INFO + ". ikke ble flyttet til feilområde. Må analyseres.")
                .end()
                .process(new MdcRemoverProcessor());
    }
}
