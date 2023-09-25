package no.nav.skanmotovrig.ovrig.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bruker {

    @XmlElement(required = true, name = "brukerId")
    private String brukerId;

    @XmlElement(required = true, name = "brukertype")
    private String brukertype;
}
