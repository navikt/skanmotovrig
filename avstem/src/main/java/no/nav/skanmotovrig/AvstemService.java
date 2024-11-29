package no.nav.skanmotovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.consumer.journalpost.JournalpostConsumer;
import no.nav.skanmotovrig.consumer.journalpost.data.AvstemmingReferanser;
import no.nav.skanmotovrig.consumer.journalpost.data.FeilendeAvstemmingReferanser;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.Set;

import static no.nav.skanmotovrig.jira.OpprettJiraService.prettifySummary;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class AvstemService {
	private final JournalpostConsumer journalpostConsumer;

	public AvstemService(JournalpostConsumer journalpostConsumer) {
		this.journalpostConsumer = journalpostConsumer;
	}

	@Handler
	public Set<String> avstemmingsReferanser(Set<String> avstemReferenser) {
		if (isEmpty(avstemReferenser)) {
			return Set.of();
		}
		FeilendeAvstemmingReferanser feilendeAvstemmingReferanser = journalpostConsumer.avstemReferanser(new AvstemmingReferanser(avstemReferenser));
		if (feilendeAvstemmingReferanser == null || isEmpty(feilendeAvstemmingReferanser.referanserIkkeFunnet())) {
			log.info(prettifySummary("Skanmotovrig avstemmingsrapport:", avstemReferenser.size(), 0));
			return null;
		}
		return feilendeAvstemmingReferanser.referanserIkkeFunnet();
	}
}
