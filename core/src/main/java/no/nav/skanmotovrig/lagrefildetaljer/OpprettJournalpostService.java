package no.nav.skanmotovrig.lagrefildetaljer;

import no.nav.skanmotovrig.domain.Filepair;
import no.nav.skanmotovrig.domain.Skanningmetadata;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostRequest;
import no.nav.skanmotovrig.lagrefildetaljer.data.OpprettJournalpostResponse;
import no.nav.skanmotovrig.lagrefildetaljer.data.STSResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.skanmotovrig.lagrefildetaljer.OpprettJournalpostRequestMapper.generateRequestBody;

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

    public OpprettJournalpostResponse opprettJournalpost(Skanningmetadata skanningmetadata, Filepair filePair) {
        OpprettJournalpostRequest request = generateRequestBody(skanningmetadata, filePair);
        return opprettJournalpost(request);
    }
}
