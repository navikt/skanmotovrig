package no.nav.skanmotovrig.ovrig.decrypt;

import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostboksOvrigRoutePGPEncryptedTest {

	@Test
	void exceptionNameReturnererUkjentNaarExceptionMangler() {
		Exchange exchange = mock(Exchange.class);
		when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class)).thenReturn(null);

		assertThat(PostboksOvrigRoutePGPEncrypted.exceptionName(exchange)).isEqualTo("ukjent");
	}

	@Test
	void exceptionNameReturnererKlassenavnNaarExceptionFinnes() {
		Exchange exchange = mock(Exchange.class);
		when(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class)).thenReturn(new IllegalStateException("boom"));

		assertThat(PostboksOvrigRoutePGPEncrypted.exceptionName(exchange)).isEqualTo(IllegalStateException.class.getName());
	}
}
