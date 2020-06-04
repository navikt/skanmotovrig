package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.mdc.MDCConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotovrig.helse.HelseRoute.PROPERTY_FORSENDELSE_FILEBASENAME;
import static no.nav.skanmotovrig.helse.HelseRoute.PROPERTY_FORSENDELSE_ZIPNAME;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class MdcProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        final String zipId = exchange.getProperty(PROPERTY_FORSENDELSE_ZIPNAME, String.class);
        if (zipId != null) {
            MDC.put(MDCConstants.MDC_ZIP_ID, zipId);
        }
        final String filename = exchange.getProperty(PROPERTY_FORSENDELSE_FILEBASENAME, String.class);
        if (filename != null) {
            MDC.put(MDCConstants.MDC_FILENAME, filename);
        }
    }
}
