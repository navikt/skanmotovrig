package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.metrics.DokCounter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ErrorMetricsProcessor implements Processor {
    private final String CAMEL_EXCEPTION_CAUGHT = "CamelExceptionCaught";
    private final DokCounter dokCounter;

    ErrorMetricsProcessor(DokCounter dokCounter){
        this.dokCounter = dokCounter;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object exception = exchange.getProperty(CAMEL_EXCEPTION_CAUGHT);
        if(exception instanceof Throwable){
            dokCounter.incrementError((Throwable) exception, DokCounter.HELSE);
        }
    }
}
