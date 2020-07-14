package no.nav.skanmotovrig.ovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static no.nav.skanmotovrig.ovrig.PostboksOvrigRoute.PROPERTY_FORSENDELSE_ZIPNAME;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
public class PostboksOvrigSkanningAggregator implements AggregationStrategy {
    public static final String XML_EXTENSION = "xml";
    public static final String PDF_EXTENSION = "pdf";

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        try {
            if (oldExchange == null) {
                final PostboksOvrigEnvelope envelope = new PostboksOvrigEnvelope(newExchange.getProperty(PROPERTY_FORSENDELSE_ZIPNAME, String.class), getBaseName(newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class)));
                applyOnEnvelope(newExchange, envelope);
                newExchange.getIn().setBody(envelope);
                return newExchange;
            }

            final PostboksOvrigEnvelope envelope = oldExchange.getIn().getBody(PostboksOvrigEnvelope.class);
            applyOnEnvelope(newExchange, envelope);
            return oldExchange;
        } catch (IOException e) {
            throw new SkanmotovrigTechnicalException("Klarte ikke lese fil", e);
        }
    }

    @Override
    public void timeout(Exchange exchange, int index, int total, long timeout) {
        final String fil = exchange.getProperty(Exchange.AGGREGATED_CORRELATION_KEY, String.class);
        log.info("Skanmotovrig fant ikke 2 filer under aggreggering av zipfil innen timeout={}ms. Fortsetter behandling. fil={}.", timeout, fil);
    }

    private void applyOnEnvelope(Exchange newExchange, PostboksOvrigEnvelope envelope) throws IOException {
        final String extension = getExtension(newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
        if (XML_EXTENSION.equalsIgnoreCase(extension)) {
            final InputStream inputStream = newExchange.getIn().getBody(InputStream.class);
            final byte[] xml = IOUtils.toByteArray(inputStream);
            envelope.setXml(xml);
        } else if (PDF_EXTENSION.equalsIgnoreCase(extension)) {
            final InputStream inputStream = newExchange.getIn().getBody(InputStream.class);
            envelope.setPdf(IOUtils.toByteArray(inputStream));
        }
    }
}
