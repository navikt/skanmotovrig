package no.nav.skanmotovrig;

import no.nav.skanmotovrig.consumer.journalpost.JournalpostConsumer;
import no.nav.skanmotovrig.consumer.journalpost.data.AvstemmingReferanser;
import no.nav.skanmotovrig.consumer.journalpost.data.FeilendeAvstemmingReferanser;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AvstemJobbService {
	private final JournalpostConsumer journalpostConsumer;

	public AvstemJobbService(JournalpostConsumer journalpostConsumer) {
		this.journalpostConsumer = journalpostConsumer;
	}

	@Handler
	public List<String> avstemSkandokument(Set<String> avstemReferenser) {
		FeilendeAvstemmingReferanser feilendeAvstemmingReferanser = journalpostConsumer.avstemReferanser(new AvstemmingReferanser(avstemReferenser));
		return feilendeAvstemmingReferanser.referanserIkkeFunnet();
	}

}
