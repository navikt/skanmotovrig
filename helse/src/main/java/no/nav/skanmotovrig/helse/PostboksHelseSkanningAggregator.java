package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigTechnicalException;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
public class PostboksHelseSkanningAggregator implements AggregationStrategy {
    public static final String XML_EXTENSION = "xml";
    public static final String OCR_EXTENSION = "ocr";
    public static final String PDF_EXTENSION = "pdf";

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        try {
            if (oldExchange == null) {
                final PostboksHelseforsendelseEnvelope envelope = new PostboksHelseforsendelseEnvelope(newExchange.getProperty(PostboksHelseRoute.PROPERTY_FORSENDELSE_ZIPNAME, String.class), getBaseName(newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class)));
                applyOnEnvelope(newExchange, envelope);
                newExchange.getIn().setBody(envelope);
                return newExchange;
            }

            final PostboksHelseforsendelseEnvelope envelope = oldExchange.getIn().getBody(PostboksHelseforsendelseEnvelope.class);
            applyOnEnvelope(newExchange, envelope);
            return oldExchange;
        } catch (IOException e) {
            throw new SkanmotovrigTechnicalException("Klarte ikke lese fil", e);
        }
    }

    private void applyOnEnvelope(Exchange newExchange, PostboksHelseforsendelseEnvelope envelope) throws IOException {
        final String extension = getExtension(newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class));
        if (XML_EXTENSION.equals(extension)) {
            final InputStream inputStream = newExchange.getIn().getBody(InputStream.class);
            final byte[] xml = IOUtils.toByteArray(inputStream);
            envelope.setXml(xml);
        } else if (OCR_EXTENSION.equals(extension)) {
            final InputStream inputStream = newExchange.getIn().getBody(InputStream.class);
            envelope.setOcr(IOUtils.toByteArray(inputStream));
        } else if (PDF_EXTENSION.equals(extension)) {
            final InputStream inputStream = newExchange.getIn().getBody(InputStream.class);
            envelope.setPdf(IOUtils.toByteArray(inputStream));
        }
    }
}
