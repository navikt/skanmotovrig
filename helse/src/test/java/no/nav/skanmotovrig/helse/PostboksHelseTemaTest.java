package no.nav.skanmotovrig.helse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
class PostboksHelseTemaTest {

    @Test
    void shouldLookupPostboks() {
        assertThat(PostboksHelseTema.lookup("1411")).isEqualTo(PostboksHelseTema.PostboksHelse.PB_1411);
    }

    @Test
    void shouldReturnEmptyWhenPostboksNotExists() {
        assertThat(PostboksHelseTema.lookup("1409")).isNull();
    }
}