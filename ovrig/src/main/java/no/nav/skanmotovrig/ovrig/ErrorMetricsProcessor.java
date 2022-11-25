package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ErrorMetricsProcessor implements Processor {
    private final String CAMEL_EXCEPTION_CAUGHT = "CamelExceptionCaught";

    @Override
    public void process(Exchange exchange) {
        Object exception = exchange.getProperty(CAMEL_EXCEPTION_CAUGHT);
        if(exception instanceof Throwable){
            DokCounter.incrementError((Throwable) exception, DokCounter.OVRIG);
        }
    }
}
