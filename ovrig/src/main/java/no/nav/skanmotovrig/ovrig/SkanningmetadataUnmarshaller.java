package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException;
import no.nav.skanmotovrig.metrics.DokCounter;
import no.nav.skanmotovrig.ovrig.domain.Journalpost;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.ovrig.domain.Skanningmetadata;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class SkanningmetadataUnmarshaller {

    @Handler
    PostboksOvrigEnvelope unmarshal(@Body PostboksOvrigEnvelope envelope) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
            SchemaFactory schemaFactory = createXEEProtectedSchemaFactory();
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            jaxbUnmarshaller.setSchema(schemaFactory.newSchema(new StreamSource(this.getClass().getResourceAsStream("/postboks-ovrig-2.0.1.xsd"))));
            XMLInputFactory xmlInputFactory = createXEEProtectedXMLInputFactory();
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(envelope.getXml()));
            final Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(xmlStreamReader);
            envelope.setSkanningmetadata(skanningmetadata);
            return envelope;
        } catch (JAXBException | XMLStreamException | SAXException e) {
            final String message = ExceptionUtils.getRootCauseMessage(e);
            throw new SkanningmetadataValidationException("Kunne ikke unmarshalle xml: " + message, e);
        }
    }

    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    private XMLInputFactory createXEEProtectedXMLInputFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return xmlInputFactory;
    }

    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    private SchemaFactory createXEEProtectedSchemaFactory() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory schemaFactory = SchemaFactory.newDefaultInstance();
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return schemaFactory;
    }
}
