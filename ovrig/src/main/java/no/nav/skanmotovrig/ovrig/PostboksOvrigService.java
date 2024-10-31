package no.nav.skanmotovrig.ovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.consumer.journalpost.JournalpostConsumer;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.consumer.journalpost.data.OpprettJournalpostResponse;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostboksOvrigService {
    private final OpprettJournalpostPostboksOvrigRequestMapper mapper;
    private final JournalpostConsumer journalpostConsumer;

    @Autowired
    public PostboksOvrigService(OpprettJournalpostPostboksOvrigRequestMapper mapper,
                                JournalpostConsumer journalpostConsumer) {
        this.mapper = mapper;
        this.journalpostConsumer = journalpostConsumer;
    }

    @Handler
    public String behandleForsendelse(@Body PostboksOvrigEnvelope envelope) {
        OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        final OpprettJournalpostResponse opprettJournalpostResponse = journalpostConsumer.opprettJournalpost(request);
        return opprettJournalpostResponse.getJournalpostId();
    }
}
