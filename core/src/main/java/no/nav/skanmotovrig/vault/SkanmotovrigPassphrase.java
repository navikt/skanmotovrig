package no.nav.skanmotovrig.vault;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkanmotovrigPassphrase {

    private String passphrase;
}
