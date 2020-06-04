package no.nav.skanmotovrig.helse.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Skanninginfo {

    @XmlElement(required = true, name = "fysiskpostboks")
    private String fysiskPostboks;

    @XmlElement(required = true, name = "strekkodepostboks")
    private String strekkodePostboks;
}
