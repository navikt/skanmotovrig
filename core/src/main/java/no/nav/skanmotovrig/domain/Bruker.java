package no.nav.skanmotovrig.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlElement;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bruker {

    @XmlElement(required = true, name = "brukerid")
    private String brukerId;

    @XmlElement(required = true, name = "brukertype")
    private String brukerType;
}
