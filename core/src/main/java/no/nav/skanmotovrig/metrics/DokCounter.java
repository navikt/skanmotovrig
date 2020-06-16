package no.nav.skanmotovrig.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class DokCounter {
    private final String DOK_SKANMOTOVRIG = "dok_skanmotovrig_";
    private final String TOTAL = "_total";
    private final String EXCEPTION = "exception";
    private final String ERROR_TYPE = "error_type";
    private final String EXCEPTION_NAME = "exception_name";
    private final String FUNCTIONAL_ERROR = "functional";
    private final String TECHNICAL_ERROR = "technical";
    private final String DOMAIN = "domain";
    public static final String CORE = "core";
    public static final String HELSE = "helse";
    public static final String OVRIG = "ovrig";

    private final MeterRegistry meterRegistry;
    @Inject
    public DokCounter(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementCounter(Map<String, String> metadata){
        metadata.forEach(this::incrementCounter);
    }

    private void incrementCounter(String key, String value) {
        Counter.builder(DOK_SKANMOTOVRIG + key + TOTAL)
                .tags(key, value)
                .register(meterRegistry)
                .increment();
    }

    public void incrementError(Throwable throwable, String domain){
        Counter.builder(DOK_SKANMOTOVRIG + EXCEPTION)
                .tags(ERROR_TYPE, isFunctionalException(throwable) ? FUNCTIONAL_ERROR : TECHNICAL_ERROR)
                .tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
                .tag(DOMAIN, isEmptyString(domain) ? CORE : domain)
                .register(meterRegistry)
                .increment();
    }

    private boolean isFunctionalException(Throwable e) {
        return e instanceof AbstractSkanmotovrigFunctionalException;
    }

    private boolean isEmptyString(String string) {
        return string == null || string.isBlank();
    }
}
