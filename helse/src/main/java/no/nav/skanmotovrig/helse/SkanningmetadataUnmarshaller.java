package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.exceptions.functional.SkanningmetadataValidationException;
import no.nav.skanmotovrig.helse.domain.Skanningmetadata;
import no.nav.skanmotovrig.unzipskanningmetadata.MetadataStreamReaderDelegate;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class SkanningmetadataUnmarshaller {
    @Handler
    HelseforsendelseEnvelope unmarshal(@Body HelseforsendelseEnvelope envelope) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
            SchemaFactory schemaFactory = SchemaFactory.newDefaultInstance();
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            jaxbUnmarshaller.setSchema(schemaFactory.newSchema(new StreamSource(this.getClass().getResourceAsStream("/postboks-helse.xsd"))));
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader xmlStreamReader = new MetadataStreamReaderDelegate(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(envelope.getXml())));
            final Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(xmlStreamReader);
            envelope.setSkanningmetadata(skanningmetadata);
            return envelope;
        } catch (JAXBException | XMLStreamException | SAXException e) {
            throw new SkanningmetadataValidationException("Kunne ikke unmarshalle xml: " + e.getMessage(), e);
        }
    }
}
