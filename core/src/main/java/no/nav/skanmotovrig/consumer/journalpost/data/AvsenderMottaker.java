package no.nav.skanmotovrig.consumer.journalpost.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AvsenderMottaker {
    String id;
    String idType;
    String navn;
    String land;
}
