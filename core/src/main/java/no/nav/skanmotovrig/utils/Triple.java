package no.nav.skanmotovrig.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Triple<T1, T2, T3> {
    private T1 t1;
    private T2 t2;
    private T3 t3;
}
