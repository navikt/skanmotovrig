package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.mdc.MDCConstants;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class NavHeaders {
    public static final String HEADER_NAV_CALL_ID = "Nav-Callid";
    public static final String HEADER_NAV_CONSUMER_ID = "Nav-Consumer-Id";
    public static final String APP_NAME = "skanmotovrig";

    public static HttpHeaders createNavCustomHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (MDC.get(MDCConstants.MDC_CALL_ID) != null) {
            headers.add(NavHeaders.HEADER_NAV_CALL_ID, MDC.get(MDCConstants.MDC_CALL_ID));
        }
        headers.add(NavHeaders.HEADER_NAV_CONSUMER_ID, APP_NAME);
        return headers;
    }
}
