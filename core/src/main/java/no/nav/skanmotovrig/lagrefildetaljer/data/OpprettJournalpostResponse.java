package no.nav.skanmotovrig.lagrefildetaljer.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpprettJournalpostResponse {
    private String journalpostId;
}
