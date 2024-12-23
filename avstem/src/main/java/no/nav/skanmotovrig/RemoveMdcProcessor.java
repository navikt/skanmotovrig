package no.nav.skanmotovrig;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_CALL_ID;
import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_AVSTEMMINGSFIL_NAVN;

public class RemoveMdcProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		MDC.remove(MDC_AVSTEMMINGSFIL_NAVN);
		MDC.remove(MDC_CALL_ID);
	}
}
