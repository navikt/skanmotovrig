package no.nav.skanmotovrig.helse;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostService;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.metrics.Metrics;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Slf4j
@Component
public class PostboksHelseService {
    private final OpprettJournalpostPostboksHelseRequestMapper mapper;
    private final OpprettJournalpostService opprettJournalpostService;

    @Inject
    public PostboksHelseService(OpprettJournalpostPostboksHelseRequestMapper mapper,
                                OpprettJournalpostService opprettJournalpostService) {
        this.mapper = mapper;
        this.opprettJournalpostService = opprettJournalpostService;
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "behandleForsendelse"}, percentiles = {0.5, 0.95}, histogram = true, createErrorMetric = true)
    @Handler
    public String behandleForsendelse(@Body PostboksHelseEnvelope envelope) {
        if (envelope.getOcr() == null) {
            log.info("Skanmothelse mangler OCR fil. Fortsetter journalføring. fil=" + envelope.getFilebasename() + ", batch=" + envelope.getSkanningmetadata().getJournalpost().getBatchnavn());
        }
        OpprettJournalpostRequest request = mapper.mapRequest(envelope);
        final OpprettJournalpostResponse opprettJournalpostResponse = opprettJournalpostService.opprettJournalpost(request);
        return opprettJournalpostResponse.getJournalpostId();
    }
}
