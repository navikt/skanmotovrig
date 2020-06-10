package no.nav.skanmotovrig.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Component
public class MetadataCounter {
    private final MeterRegistry meterRegistry;
    @Inject
    public MetadataCounter(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    public void incrementMetadata(Map<String, String> metadata){
        metadata.forEach(this::incrementMetadata);
    }

    private void incrementMetadata(String key, String value) {
        Counter.builder("dok_skanmotovrig_metadata")
                .tags("metadataType", key)
                .tags("metadataValue", value)
                .register(meterRegistry)
                .increment();
    }
}
