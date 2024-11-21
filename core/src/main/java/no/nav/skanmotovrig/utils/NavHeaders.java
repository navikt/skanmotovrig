package no.nav.skanmotovrig.utils;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

import static no.nav.skanmotovrig.mdc.MDCConstants.MDC_CALL_ID;

public class NavHeaders {
	public static final String NAV_CALL_ID = "Nav-Callid";
	public static HttpHeaders createNavCustomHeaders(HttpHeaders headers) {
		if (MDC.get(MDC_CALL_ID) != null) {
			headers.add(NAV_CALL_ID, MDC.get(MDC_CALL_ID));
		}
		return headers;
	}
}
