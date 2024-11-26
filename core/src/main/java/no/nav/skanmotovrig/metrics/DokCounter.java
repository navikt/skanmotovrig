package no.nav.skanmotovrig.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.skanmotovrig.exceptions.functional.AbstractSkanmotovrigFunctionalException;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DokCounter {
	private static final String DOK_SKANMOTOVRIG = "dok_skanmotovrig_";
	private static final String TOTAL = "_total";
	private static final String EXCEPTION = "exception";
	private static final String ERROR_TYPE = "error_type";
	private static final String EXCEPTION_NAME = "exception_name";
	private static final String FUNCTIONAL_ERROR = "functional";
	private static final String TECHNICAL_ERROR = "technical";
	public static final String DOMAIN = "domain";
	public static final String CORE = "core";
	public static final String OVRIG = "ovrig";

	private static MeterRegistry meterRegistry;

	@Autowired
	public DokCounter(MeterRegistry meterRegistry) {
		DokCounter.meterRegistry = meterRegistry;
	}

	public static void incrementCounter(String key, List<String> tags) {
		Counter.builder(DOK_SKANMOTOVRIG + key + TOTAL)
				.tags(tags.toArray(new String[0]))
				.register(meterRegistry)
				.increment();
	}

	public static void incrementError(Throwable throwable, String domain) {
		Counter.builder(DOK_SKANMOTOVRIG + EXCEPTION)
				.tags(ERROR_TYPE, isFunctionalException(throwable) ? FUNCTIONAL_ERROR : TECHNICAL_ERROR)
				.tags(EXCEPTION_NAME, throwable.getClass().getSimpleName())
				.tag(DOMAIN, isEmptyString(domain) ? CORE : domain)
				.register(meterRegistry)
				.increment();
	}

	private static boolean isFunctionalException(Throwable e) {
		return e instanceof AbstractSkanmotovrigFunctionalException || e instanceof PGPException; // Feil for PGP-kryptering
	}

	private static boolean isEmptyString(String string) {
		return string == null || string.isBlank();
	}
}
