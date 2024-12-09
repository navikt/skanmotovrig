package no.nav.skanmotovrig.consumer.journalpost.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class Tilleggsopplysning {
    String nokkel;
    String verdi;
}
