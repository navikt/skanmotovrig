package no.nav.skanmotovrig.helse.domain;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@XmlRegistry
public class ObjectFactory {
    public ObjectFactory() {
    }

    public Skanningmetadata createSkanningmetadata() {
        return new Skanningmetadata();
    }
}
