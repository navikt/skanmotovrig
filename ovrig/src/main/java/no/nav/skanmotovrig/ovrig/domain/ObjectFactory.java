package no.nav.skanmotovrig.ovrig.domain;

import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    public ObjectFactory() {
    }

    public Skanningmetadata createSkanningmetadata() {
        return new Skanningmetadata();
    }
}
