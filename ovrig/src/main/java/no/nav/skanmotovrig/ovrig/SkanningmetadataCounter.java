package no.nav.skanmotovrig.ovrig;

import no.nav.skanmotovrig.metrics.DokCounter;
import no.nav.skanmotovrig.ovrig.domain.Journalpost;
import no.nav.skanmotovrig.ovrig.domain.SkanningInfo;
import no.nav.skanmotovrig.ovrig.domain.Skanningmetadata;
import org.apache.camel.Body;
import org.apache.camel.Handler;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SkanningmetadataCounter {

    private static final String TEMA = "tema";
    private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private static final String FYSISKPOSTBOKS = "fysiskPostboks";
    private static final String EMPTY = "empty";

    @Handler
    PostboksOvrigEnvelope incrementMetadataCounter(@Body PostboksOvrigEnvelope envelope) {
        Skanningmetadata skanningmetadata = envelope.getSkanningmetadata();

        DokCounter.incrementCounter(TEMA, List.of(
                DokCounter.DOMAIN, DokCounter.OVRIG,
                TEMA, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getJournalpost)
                        .map(Journalpost::getTema)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));

        DokCounter.incrementCounter(STREKKODEPOSTBOKS, List.of(
                DokCounter.DOMAIN, DokCounter.OVRIG,
                STREKKODEPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getStrekkodePostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));

        DokCounter.incrementCounter(FYSISKPOSTBOKS, List.of(
                DokCounter.DOMAIN, DokCounter.OVRIG,
                FYSISKPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanningInfo)
                        .map(SkanningInfo::getFysiskPostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));

        return envelope;
    }
}
