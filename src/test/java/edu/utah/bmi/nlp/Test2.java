package edu.utah.bmi.nlp;

import edu.utah.bmi.nlp.fastcner.UnicodeChecker;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class Test2 {
    @Test
    public void test() {
        int d = '　';
        System.out.println(d);
    }

    @Test

    public void test2() {
        System.out.println((int) '\u4E00');
        System.out.println((int) '\u9FCB');
    }

    @Test
    public void test3() {
        String all = "!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~！“”＃￥％＆‘'||’（）×＋，－。／：；《＝》？＠·、「」…—｀｛｜｝～'";
        HashSet<Character> set = new HashSet<>();
        for (char ch : all.toCharArray()) {
            set.add(ch);
        }
        System.out.println(all.length());
        System.out.println(set.size());

        for (char ch : set) {
            System.out.println("c=='" + ch + "' ||");
        }

        System.out.println(UnicodeChecker.isSpecialChar('　'));

    }

    @Test
    public void testDigit() {
        String input = "１２３４５６７８９０";
        for (char ch : input.toCharArray()) {
            System.out.println((int) ch);
        }
    }

    public static void main(String[] args) {
        System.out.println("Title Case");
        for (char ch = Character.MIN_VALUE; ch < Character.MAX_VALUE; ch++) {
            if (Character.TITLECASE_LETTER == Character.getDirectionality(ch)) {
                String s = String.format("\\u%04x", (int) ch);
                System.out.println(s+"\t"+ch);
            }
        }
        System.out.println("MODIFIER_LETTER");
        for (char ch = Character.MIN_VALUE; ch < Character.MAX_VALUE; ch++) {
            if (Character.MODIFIER_LETTER == Character.getDirectionality(ch)) {
                String s = String.format("\\u%04x", (int) ch);
                System.out.println(s+"\t"+ch);
            }
        }
    }

}
