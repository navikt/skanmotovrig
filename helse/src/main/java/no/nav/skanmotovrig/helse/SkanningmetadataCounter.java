package no.nav.skanmotovrig.helse;

import no.nav.skanmotovrig.helse.domain.Skanninginfo;
import no.nav.skanmotovrig.helse.domain.Skanningmetadata;
import no.nav.skanmotovrig.metrics.DokCounter;
import org.apache.camel.Body;
import org.apache.camel.Handler;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SkanningmetadataCounter {

    private static final String STREKKODEPOSTBOKS = "strekkodePostboks";
    private static final String FYSISKPOSTBOKS = "fysiskPostboks";
    private static final String EMPTY = "empty";

    @Handler
    PostboksHelseEnvelope incrementMetadataCounter(@Body PostboksHelseEnvelope envelope) {
        Skanningmetadata skanningmetadata = envelope.getSkanningmetadata();

        DokCounter.incrementCounter(STREKKODEPOSTBOKS, List.of(
                DokCounter.DOMAIN, DokCounter.HELSE,
                STREKKODEPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanninginfo)
                        .map(Skanninginfo::getStrekkodePostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));

        DokCounter.incrementCounter(FYSISKPOSTBOKS, List.of(
                DokCounter.DOMAIN, DokCounter.HELSE,
                FYSISKPOSTBOKS, Optional.ofNullable(skanningmetadata)
                        .map(Skanningmetadata::getSkanninginfo)
                        .map(Skanninginfo::getFysiskPostboks)
                        .filter(Predicate.not(String::isBlank))
                        .orElse(EMPTY)
        ));

        return envelope;
    }
}
