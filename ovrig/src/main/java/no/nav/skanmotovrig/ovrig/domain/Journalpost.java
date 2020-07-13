package no.nav.skanmotovrig.ovrig.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.skanmotovrig.utils.LocalDateAdapter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import java.util.Date;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journalpost {

    @XmlElement(required = false, name = "tema")
    private String tema;

    @XmlElement(required = false, name = "brevKode")
    private String brevKode;

    @XmlElement(required = false, name = "bruker")
    private Bruker bruker;

    @XmlElement(required = false, name = "journalforendeEnhet")
    private String journalforendeEnhet;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlElement(required = true, name = "datoMottatt")
    private LocalDate datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchnavn;

    @XmlElement(required = false, name = "filnavn")
    private String filnavn;

    @XmlElement(required = false, name = "endorsernr")
    private String endorsernr;

    @XmlElement(required = false, name = "land")
    private String land;

    @XmlElement(required = false, name = "antallSider")
    private String antallSider;
}
