package no.nav.skanmotovrig.ovrig;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksOvrigService {
    private final OpprettJournalpostPostboksOvrigRequestMapper mapper;
    private final OpprettJournalpostService opprettJournalpostService;

    @Inject
    public PostboksOvrigService(OpprettJournalpostPostboksOvrigRequestMapper mapper,
                                OpprettJournalpostService opprettJournalpostService) {
        this.mapper = mapper;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Handler
    public String behandleForsendelse(@Body PostboksOvrigEnvelope envelope) {
        OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        final OpprettJournalpostResponse opprettJournalpostResponse = opprettJournalpostService.opprettJournalpost(request);
        return opprettJournalpostResponse.getJournalpostId();
    }
}
