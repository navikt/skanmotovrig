package no.nav.skanmotovrig.unzipskanningmetadata;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.exceptions.technical.SkanmotovrigUnzipperTechnicalException;
import no.nav.skanmotovrig.utils.Utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

@Slf4j
public class UnzipSkanningmetadataUtils {

    public static List<Filepair> pairFiles(Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return pdfs.keySet().stream().map(pdfName ->
                Filepair.builder()
                        .name(Utils.removeFileExtensionInFilename(pdfName))
                        .pdf(pdfs.get(pdfName))
                        .xml(xmls.get(Utils.changeFiletypeInFilename(pdfName, "xml")))
                        .build()
        ).collect(Collectors.toList());
    }

    public static FilepairWithMetadata extractMetadata(Filepair filepair) {
        return FilepairWithMetadata.builder()
                .skanningmetadata(bytesToSkanningmetadata(filepair.getXml()))
                .pdf(filepair.getPdf())
                .xml(filepair.getXml())
                .build();
    }

    public static Skanningmetadata bytesToSkanningmetadata(byte[] bytes) {
        try {
            JAXBContext jaxbContext;
            jaxbContext = JAXBContext.newInstance(Skanningmetadata.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader xmlStreamReader =  new MetadataStreamReaderDelegate(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(bytes)));

            Skanningmetadata skanningmetadata = (Skanningmetadata) jaxbUnmarshaller.unmarshal(xmlStreamReader);

            skanningmetadata.verifyFields();

            return skanningmetadata;
        } catch (JAXBException | XMLStreamException e) {
            log.error("Skanmotovrig klarte ikke lese metadata i zipfil, feilmedling={}",e.getMessage(), e);
            throw new SkanmotovrigUnzipperTechnicalException("Skanmotovrig klarte ikke lese metadata i zipfil", e);
        } catch (NullPointerException e) {
            throw new SkanmotovrigUnzipperFunctionalException("Xml fil mangler");
        }
    }

    public static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }
}
