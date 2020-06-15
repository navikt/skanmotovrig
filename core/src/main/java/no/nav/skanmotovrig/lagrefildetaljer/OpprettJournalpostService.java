package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
import no.nav.skanmotovrig.metrics.Metrics;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostRequestMapper.generateRequestBody;
import static no.nav.skanmotovrig.metrics.MetricLabels.DOK_METRIC;
import static no.nav.skanmotovrig.metrics.MetricLabels.PROCESS_NAME;

@Service
public class OpprettJournalpostService {

    private OpprettJournalpostConsumer opprettJournalpostConsumer;
    private STSConsumer stsConsumer;

    @Inject
    public OpprettJournalpostService(OpprettJournalpostConsumer opprettJournalpostConsumer, STSConsumer stsConsumer) {
        this.opprettJournalpostConsumer = opprettJournalpostConsumer;
        this.stsConsumer = stsConsumer;
    }

    public OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request) {
        STSResponse stsResponse = stsConsumer.getSTSToken();
        return opprettJournalpostConsumer.opprettJournalpost(stsResponse.getAccess_token(), request);
    }

    @Metrics(value = DOK_METRIC, extraTags = {PROCESS_NAME, "opprettJournalpost"}, percentiles = {0.5, 0.95}, histogram = true, createErrorMetric = true)
    public OpprettJournalpostResponse opprettJournalpost(Skanningmetadata skanningmetadata, Filepair filePair) {
        OpprettJournalpostRequest request = generateRequestBody(skanningmetadata, filePair);
        return opprettJournalpost(request);
    }
}
