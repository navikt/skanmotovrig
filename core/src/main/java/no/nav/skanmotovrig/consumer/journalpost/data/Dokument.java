package no.nav.skanmotovrig.consumer.journalpost.data;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Dokument {
    String tittel;
    String brevkode;
    String dokumentKategori;
    List<DokumentVariant> dokumentVarianter;
}


