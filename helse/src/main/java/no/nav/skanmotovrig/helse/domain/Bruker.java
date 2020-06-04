package no.nav.skanmotovrig.helse.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Bruker {

    @ToString.Exclude
    @XmlElement(required = true, name = "brukerid")
    private String brukerId;

    @XmlElement(required = true, name = "brukertype")
    private String brukerType;
}
