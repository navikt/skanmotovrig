package no.nav.skanmotovrig.ovrig.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotovrig.utils.LocalDateAdapter;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(name = "tema")
    private String tema;

    @XmlElement(name = "brevKode")
    private String brevKode;

    @XmlElement(name = "bruker")
    private Bruker bruker;

    @XmlElement(name = "journalforendeEnhet")
    private String journalforendeEnhet;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlElement(required = true, name = "datoMottatt")
    private LocalDate datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchnavn;

    @XmlElement(name = "filnavn")
    private String filnavn;

    @XmlElement(name = "endorsernr")
    private String endorsernr;

    @XmlElement(name = "land")
    private String land;

    @XmlElement(name = "antallSider")
    private String antallSider;
}
