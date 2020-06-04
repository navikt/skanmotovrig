package no.nav.skanmotovrig.helse.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.skanmotovrig.utils.LocalDateAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Journalpost {

    @XmlElement(required = false, name = "bruker")
    private Bruker bruker;

    @XmlElement(required = true, name = "mottakskanal")
    private String mottakskanal;

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlElement(required = true, name = "datomottatt")
    private LocalDate datoMottatt;

    @XmlElement(required = true, name = "batchnavn")
    private String batchNavn;

    @XmlElement(required = false, name = "filnavn")
    private String filNavn;

    @XmlElement(required = false, name = "endorsernr")
    private String endorsernr;

    @XmlElement(required = false, name = "antallsider")
    private String antallSider;
}
