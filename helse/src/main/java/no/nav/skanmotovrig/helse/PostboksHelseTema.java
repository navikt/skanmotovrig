package no.nav.skanmotovrig.helse;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PostboksHelseTema {
    private static final Map<String, PostboksHelse> postbokser = new HashMap<>();

    static {
        postbokser.put(PostboksHelse.PB_1411.postboks, PostboksHelse.PB_1411);
    }

    public static PostboksHelse lookup(final String postboks) {
        return postbokser.getOrDefault(postboks, null);
    }

    @Getter
    public enum PostboksHelse
    {
        PB_1411("1411", "NAV 08-07.04", "SYM", "Papirsykmelding", "Papirsykmelding", null);

        final String postboks;
        final String brevkode;
        final String tema;
        final String tittel;
        final String dokumentTittel;
        final String behandlingstema;

        PostboksHelse(String postboks, String brevkode, String tema, String tittel, String dokumentTittel, String behandlingstema)
        {
            this.postboks = postboks;
            this.brevkode = brevkode;
            this.tema = tema;
            this.tittel = tittel;
            this.dokumentTittel = dokumentTittel;
            this.behandlingstema = behandlingstema;
        }
    }
}
