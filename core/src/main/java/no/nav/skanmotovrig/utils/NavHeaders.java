package no.nav.skanmotovrig.utils;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_CALL_ID;

public class NavHeaders {
	public static final String HEADER_NAV_CALL_ID = "Nav-Callid";
	public static final String HEADER_NAV_CONSUMER_ID = "Nav-Consumer-Id";
	public static final String APP_NAME = "skanmotovrig";

	public static HttpHeaders createNavCustomHeaders(HttpHeaders headers) {
		if (MDC.get(MDC_CALL_ID) != null) {
			headers.add(HEADER_NAV_CALL_ID, MDC.get(MDC_CALL_ID));
		}
		headers.add(HEADER_NAV_CONSUMER_ID, APP_NAME);
		return headers;
	}
}
