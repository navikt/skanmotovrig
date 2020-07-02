package no.nav.skanmotovrig.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

    private final String PDF_NAME = "1408-005.pdf";
    private final String XML_NAME = "1408-005.xml";

    @Test
    public void shouldConvertPdfFilenameToXmlFilename() {
        String xmlName = Utils.changeFiletypeInFilename(PDF_NAME, "xml");
        assertEquals(XML_NAME, xmlName);
    }
}
