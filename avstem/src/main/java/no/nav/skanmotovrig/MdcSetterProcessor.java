package no.nav.skanmotovrig;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_CALL_ID;
import static no.nav.skanmotovrig.mdc.MDCConstants.PROPERTY_AVSTEM_FILNAVN;

public class MdcSetterProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		String avstemFilnavn = exchange.getProperty(PROPERTY_AVSTEM_FILNAVN, String.class);
		if (avstemFilnavn != null) {
			MDC.put(MDC_CALL_ID, avstemFilnavn);
		}
	}
}
