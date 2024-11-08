package no.nav.skanmotovrig.consumer.journalpost.data;

import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotNull;

@Value
@Builder
public class DokumentVariant {
    @NotNull(message = "filtype kan ikke være null")
    String filtype;

    @NotNull(message = "fysiskDokument kan ikke være null")
    byte[] fysiskDokument;

    @NotNull(message = "variantformat kan ikke være null")
    String variantformat;

    @NotNull(message = "navn kan ikke være null")
    String filnavn;

    String batchnavn;
}
