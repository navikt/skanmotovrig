package no.nav.skanmotovrig.unzipskanningmetadata;

import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.FilepairWithMetadata;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.exceptions.functional.SkanmotovrigUnzipperFunctionalException;
import no.nav.skanmotovrig.utils.Utils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class UnzipSkanningmetadataUtils {

    public static List<FilepairWithMetadata> pairFiles(List<Skanningmetadata> skanningmetadataList, Map<String, byte[]> pdfs, Map<String, byte[]> xmls) {
        return skanningmetadataList.stream().map(metadata -> {
            String pdfFilnavn = metadata.getJournalpost().getFilNavn();
            String xmlFilnavn = Utils.changeFiletypeInFilename(pdfFilnavn, "xml");
            if (!pdfs.containsKey(pdfFilnavn)) {
                throw new SkanmotovrigUnzipperFunctionalException("Skanmotovrig fant ikke tilhørende pdf-fil til journalpost ");
            }
            if (!xmls.containsKey(xmlFilnavn)) {
                throw new SkanmotovrigUnzipperFunctionalException("Skanmotovrig fant ikke tilhørende xml-fil til journalpost ");
            }
            return FilepairWithMetadata.builder()
                    .skanningmetadata(metadata)
                    .pdf(pdfs.get(pdfFilnavn))
                    .xml(xmls.get(xmlFilnavn))
                    .build();
        }).collect(Collectors.toList());
    }

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
            throw new SkanmotovrigUnzipperFunctionalException("Skanmotovrig klarte ikke lese metadata i zipfil", e);
        } catch (NullPointerException e) {
            throw new SkanmotovrigUnzipperFunctionalException("Xml fil mangler");
        }
    }

    public static String getFileType(ZipEntry file) {
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }
}
