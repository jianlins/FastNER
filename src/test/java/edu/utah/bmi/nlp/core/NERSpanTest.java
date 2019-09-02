package edu.utah.bmi.nlp.core;

import org.junit.jupiter.api.Test;


public class NERSpanTest {
    @Test
    public void test() {
        NERSpan a = new NERSpan(1, 3, 0, 2, 0.5, "ab");
        NERSpan b = new NERSpan(1, 4, 1, 1, 0.5, "ab");
        System.out.println(a.compareTo(b));

    }

}