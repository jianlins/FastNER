/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.fastner;

import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.fastcner.FastCRuleSB;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jianlin Shi on 7/25/17.
 */
public class FastRuleWGNTest {

    private FastNER fastNER;

    @Test
    public void processTokens() throws Exception {
        String text = "Exam was done 4 yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "was done \\> 3	0	Concept	ACTUAL";
        ArrayList<String> tokens = new ArrayList<>();
        tokens.addAll(Arrays.asList(text.split("[\\s\\.]+")));
        System.out.println(tokens);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processStringList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + "\t");
            entry.getValue().forEach((span) -> {
                System.out.println(tokens.subList(span.getBegin(), span.getEnd() + 1));
            });
        }
    }


    @Test
    public void processSpans() throws Exception {

        String text = "Exam was done 5 yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "was done \\< 6 \\> 3	0	Concept	ACTUAL";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
        assert (res.containsKey("Concept") && res.get("Concept").size() == 1);
    }

    @Test
    public void processSpans2() throws Exception {

        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n" +
                "pulmonary emboli protocol	0	Concept	PSEUDO\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
        assert (res.containsKey("Concept") && res.get("Concept").size() == 0);
    }


    @Test
    public void processSpans3() throws Exception {

        String text = "Exam was done yesterday. Positive when HbA1C 105 but not HbA1C 50. No further treatment needed.";
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n" +
                "HbA 1 C \\> 100 	0	Concept	ACTUAL\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
    }

    @Test
    public void processSpans4() throws Exception {

        String text = "There are 13,092 patients enrolled.";
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n" +
                "\\d+ , \\d+ 	0	Concept	ACTUAL\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
    }

    @Test
    public void processSpans5() throws Exception {

        String text = "and Dec 31, 2008, 8164 patients";
        String rule = "@fastner\n" +
                "\\d+\tCLUE\n" +
                "\\d+ , \\d+\tCLUE\n" +
                "\\d+ , \\d+ , \\d+\tCLUE\n" +
                "Dec \\d+ , \\d+\t2\tCLUE\tPSEUDO";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        assert (res.size() == 1);
        assert (res.get("CLUE").size() == 1);
        Span span = res.get("CLUE").get(0);
        assert (text.substring(span.begin, span.end).equals("8164"));

    }

    @Test
    public void processSpans6() throws Exception {

        String text = "The trial was intended to enroll 8500 patients, but in conjunction with the US Food and Drug Administration enrollment was modified to 5745 patients presenting from 296 hospitals in 17 countries from July 13, 2004, to May 11, 2006.";
        String rule = "@fastner\n" +
                "\\d+\tCLUE\n" +
                "\\d+ , \\d+\tCLUE\n" +
                "\\d+ , \\d+ , \\d+\tCLUE\n" +

                "#Between Dec 19, 2006, and April 2, 2010, patients were enrolled\n" +
                "May \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "June \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "July \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "\n" +
                "May \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "Jun \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "Jul \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "\n" +
                "May . \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "Jun . \\d+ , \\d+\tCLUE\tPSEUDO\n" +
                "Jul . \\d+ , \\d+\tCLUE\tPSEUDO\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);


    }

    @Test
    public void processSpans7() throws Exception {

        String text = "89 years old";
        String rule = "@fastner\n" +
                "\\d+ \\) years old\tCLUE\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        assert (res.size() == 1);
        assert (res.get("CLUE").size() == 1);
        Span span = res.get("CLUE").get(0);
        System.out.println(span);

    }

    @Test
    public void processSpans8() throws Exception {

        String text = "he is 89 years old";
        String rule = "@fastner\n" +
                "\\d+ \\) [year|years] old\tCLUE\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        assert (res.size() == 1);
        assert (res.get("CLUE").size() == 1);
        Span span = res.get("CLUE").get(0);
        System.out.println(span);
    }

    @Test
    public void processSpans9() throws Exception {
        String rule = "\\d+ \\) [year|years] old";
        ArrayList<Rule> rules = new FastCRuleSB(rule).expandSB(new NERRule(0, rule, "CLUE", DeterminantValueSet.Determinants.ACTUAL, new ArrayList<>()));
        System.out.println(rules);
    }


    @Test
    public void processSpans10() throws Exception {

        String text =   "{{ CANCER, COLON }} 30 - 50 s diagnosed ; ";
        String rule = "@fastner\n" +
                "\\> 19 \\< 100 \\) s diagnosed\tONSET_RANGE\n" +
                "\\( \\> 19 \\< 100 \\) -\tONSET_RANGE\n" +
                "\\> 19 \\< 100 \\( - \\> 19 \\< 100 \\)\tONSET_RANGE\tPSEUDO\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
        assert (res.size()==1);
        assert (res.getOrDefault("ONSET_RANGE", new ArrayList<>()).size()==1);
    }
    @Test
    public void processSpans11() throws Exception {

        String text =   "{{ CANCER, COLON }} 30 - 50 s diagnosed ; ";
        String rule = "@fastner\n" +
                "\\> 19 \\< 100 \\) s diagnosed\tONSET_RANGE\n" +
                "\\( \\> 19 \\< 100 \\) -\tONSET_RANGE\n" +
                "#\\> 19 \\< 100 \\( - \\> 19 \\< 100 \\)\tONSET_RANGE\tPSEUDO\n";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, true).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey() + ":\t");
            entry.getValue().forEach((span) -> {
                System.out.println("\t" + text.substring(span.getBegin(), span.getEnd()));
            });
        }
        assert (res.size()==1);
        assert (res.getOrDefault("ONSET_RANGE", new ArrayList<>()).size()==2);
    }
}