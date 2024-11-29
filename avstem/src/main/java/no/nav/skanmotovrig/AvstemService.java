package no.nav.skanmotovrig;

import no.nav.skanmotovrig.consumer.journalpost.JournalpostConsumer;
import no.nav.skanmotovrig.consumer.journalpost.data.AvstemmingReferanser;
import no.nav.skanmotovrig.consumer.journalpost.data.FeilendeAvstemmingReferanser;
import no.nav.skanmotovrig.jira.OpprettJiraService;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;

import static org.springframework.util.CollectionUtils.isEmpty;

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

		if (feilendeAvstemmingReferanser == null || CollectionUtils.isEmpty(feilendeAvstemmingReferanser.referanserIkkeFunnet())) {
			OpprettJiraService.prettifySummary(avstemReferenser.size(), 0);
			return null;
		}
		return feilendeAvstemmingReferanser.referanserIkkeFunnet();
	}
}
