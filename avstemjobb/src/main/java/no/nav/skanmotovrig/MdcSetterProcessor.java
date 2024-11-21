package no.nav.skanmotovrig;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_CALL_ID;

public class MdcSetterProcessor implements Processor {
	@Override
	public void process(Exchange exchange) {
		String exchangeId = exchange.getExchangeId();

		if (exchangeId != null) {
			MDC.put(MDC_CALL_ID, exchangeId);
		}
	}
}
