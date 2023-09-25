package no.nav.skanmotovrig.ovrig.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@XmlRootElement(name = "skanningmetadata")
public class Skanningmetadata {

    @XmlElement(required = true, name = "journalpost")
    private Journalpost journalpost;

    @XmlElement(required = true, name = "skanningInfo")
    private SkanningInfo skanningInfo;
}
