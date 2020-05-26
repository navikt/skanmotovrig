package no.nav.skanmotovrig.lagrefildetaljer.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {
    private String nokkel;

    private String verdi;
}
