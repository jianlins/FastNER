package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants.ACTUAL;

/**
 * Created by
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class FastCNERTest extends TestCase {
    private boolean print = false;
    private boolean debug = false;

    public void testProcessString() throws Exception {
        HashMap<Integer, Rule> rules = new HashMap<>();

//        (\c.)\n+	0.5	rule5
//        (\c)\e	0.5	rule3
//        \c(.)\s+C	0.5	rule5
//        a\c+	0.5	rule5
        rules.put(1, new Rule(1, "(\\c.)\\n+", "rule5", 0.5, ACTUAL));
        rules.put(2, new Rule(2, "(\\c)\\e", "rule3", 0.5, ACTUAL));
        rules.put(3, new Rule(3, "\\c(.)\\s+C", "rule5", 0.5, ACTUAL));
        rules.put(4, new Rule(4, "a\\c+", "rule5", 0.5, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);
        String a = "abe.\ne. Cdacddecf";
        HashMap<String, ArrayList<Span>> result = fcrp.processString(a);
        System.out.println(a.length());
        printMatches(result, a);
        evalMatch(result.get("rule5"), new Span[]{new Span(0, 3), new Span(6, 7), new Span(10, 17)});
        evalMatch(result.get("rule3"), new Span[]{new Span(16, 17)});
    }

    public void test2() {
        FastCNER fcrp = new FastCNER("conf/crule_test.xlsx");
        fcrp.setReplicationSupport(true);
        String a = "The patient denies any problem with visual changes or hearing changes. he att";
        HashMap<String, ArrayList<Span>> result = fcrp.processString(a);
        printMatches(result, a, fcrp);
    }


    public void test3() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "b\\c+)d", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(3);
        fcrp.setReplicationSupport(true);

        String input = "abzid ok";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input);

        input = "abziad ok";
        result = fcrp.processString(input);
        printMatches(result, input);
//        TODO maximum replicants doesn't work in FastCRules and FastCRules2
        input = "abccccd ok";
        result = fcrp.processString(input);
        printMatches(result, input);

        input = " dbcccdd ok";
        result = fcrp.processString(input);
        printMatches(result, input);
    }


    public void test3_5() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        rules.put(0,new Rule(0, "b\\c+)d", "TEST", 0.1, ACTUAL));
        rules.put(0, new Rule(0, "bc+)d", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(3);
        fcrp.setReplicationSupport(true);

        String input = "abzid ok";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(0, result.size());

        input = "abziad ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(0, result.size());

        input = "abccccd ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        evalMatch(result.get("TEST"), new Span[]{new Span(1, 6)});


        input = " dbcccdd ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        evalMatch(result.get("TEST"), new Span[]{new Span(2, 6)});

        input = " dbcccccdd ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(0, result.size());

    }

    public void test4() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "b\\c+)D", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(3);
        fcrp.setReplicationSupport(true);


        String input = "abziD ok";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("bzi", result.get("TEST").get(0).text);

        input = "abziaD ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("bzia", result.get("TEST").get(0).text);

//        TODO maximum replicants not tested
        input = "abccccD ok";
        result = fcrp.processString(input);
        printMatches(result, input);

        input = "abcccccD ok";
        result = fcrp.processString(input);
        printMatches(result, input);

        input = " dbcccDD ok";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("bccc", result.get("TEST").get(0).text);
    }

    public void test5() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "c(\\d[|\\d][|\\d][|\\d])", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(3);
        fcrp.setReplicationSupport(true);

        String input = "c23";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("23", result.get("TEST").get(0).text);

        input = "c3467";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("3467", result.get("TEST").get(0).text);

        input = "abc3e";
        result = fcrp.processString(input);
        printMatches(result, input);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("3", result.get("TEST").get(0).text);

    }

//    \C[\c+|\C+] [|\C\c+ |\C+ ][|\C\c+ |\C+ ][|\C\c+ |\C+ ][|\C\c+ |\C+ ][|\C\c+ |\C+ ][Division|


    public void test6() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        ruleStore.add(new Rule(0, "\\C[\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Division|divsion|ICU|SCIU|CCU|DEPARTMENT|Department|department|Dept|DEPT]", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        rules.put(0, new Rule(0, "\\C[\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Neurosurgery|Surgery]", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "Eddd Addd Doo Surgery";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("Eddd Addd Doo Surgery", result.get("TEST").get(0).text);

        input = "addd Addd Doo Surgery";
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("Addd Doo Surgery", result.get("TEST").get(0).text);

    }

    public void test7() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        ruleStore.add(new Rule(0, "\\C[\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Division|divsion|ICU|SCIU|CCU|DEPARTMENT|Department|department|Dept|DEPT]", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        rules.put(0, new Rule(0, "\\C[\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ]Surgery", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ S", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
//        fcrp.setReplicationSupport(true);

        String input = "Doodd DdDdd Addd Doo Surgery";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(0, result.size());


    }


    public void test8() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        ruleStore.add(new Rule(0, "\\C[debug\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Division|divsion|ICU|SCIU|CCU|DEPARTMENT|Department|department|Dept|DEPT]", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        rules.put(0, new Rule(0, "\\C+[| \\C+][| \\C+][| \\C+][| \\C+][| \\C+][| \\C+][| \\C+][| \\C+] +HOS", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ S", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "we DA CO  HOSPITAL";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(1, result.get("TEST").size());
        assertEquals("DA CO  HOS", result.get("TEST").get(0).text);

    }

    public void test9() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        ruleStore.add(new Rule(0, "\\C[debug\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Division|divsion|ICU|SCIU|CCU|DEPARTMENT|Department|department|Dept|DEPT]", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        rules.put(0, new Rule(0, "Dr. +(\\C\\c+)", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ S", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);
//        fcrp.printRulesMap();

        String input = "Dr. Ice";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assert (evalMatch(result.get("TEST"), new Span[]{new Span(4, 7)}));
    }

    public void test10() {
        HashMap<Integer, Rule> rules = new HashMap<>();
//        ruleStore.add(new Rule(0, "\\C[debug\\c+|\\C+] [|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][|\\C\\c+ |\\C+ ][Division|divsion|ICU|SCIU|CCU|DEPARTMENT|Department|department|Dept|DEPT]", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "ccd", "TEST", 0.1, ACTUAL));

        rules.put(0, new Rule(0, "([|\\C+ +|\\C\\c+ +]\\C. +[\\C+|\\C\\c+])[ +|, +|,]MD", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ \\C\\c+ S", "TEST", 0.1, ACTUAL));
//        ruleStore.add(new Rule(0, "\\C\\c+ S", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "Emmerson F. Carpenter, MD";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assert (evalMatch(result.get("TEST"), new Span[]{new Span(0, 21)}));
    }

    public void test11() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "[([|\\c+|\\C+]\\d\\d)]", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "Em[ad13]";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assert (evalMatch(result.get("TEST"), new Span[]{new Span(3, 7)}));
    }

    public void test12() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "\\[([|\\c+|\\C+]\\d\\d+)\\]", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "Em[ad1345]";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assert (evalMatch(result.get("TEST"), new Span[]{new Span(3, 9)}));
    }

    public void test13() {

        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "([|\\c+|\\C+] \\d\\d+)", "TEST", 0.1, ACTUAL));
        FastCNER fcrp;
        String input = "Vital signs temperature 100.6 .\n";
        HashMap<String, ArrayList<Span>> result;


        fcrp = new FastCNER("conf/crule_test.tsv");
        fcrp.setMaxRepeatLength(30);
        fcrp.setDebug(debug);


        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(1, result.get("edu.utah.bmi.type.system.Fever_present").size());
        assertEquals("temperature 100.6", result.get("edu.utah.bmi.type.system.Fever_present").get(0).text);

        input = "PT 14.2 , PTT 108.5 , INR 1.3 .";
        fcrp = new FastCNER(rules);
        fcrp.setReplicationSupport(true);
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(2, result.get("TEST").size());
        assertEquals("PT 14", result.get("TEST").get(0).text);
        assertEquals("PTT 108", result.get("TEST").get(1).text);


        String rule = "([|\\c+|\\C+] \\d\\d+)\t0.1\tTEST";
        fcrp = new FastCNER(rule);
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        assertEquals(1, result.size());
        assertEquals(2, result.get(DeterminantValueSet.defaultNameSpace + "TEST").size());
        assertEquals("PT 14", result.get(DeterminantValueSet.defaultNameSpace + "TEST").get(0).text);
        assertEquals("PTT 108", result.get(DeterminantValueSet.defaultNameSpace + "TEST").get(1).text);


    }

    public void test14() {

        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(0, new Rule(0, "patient", "TEST", 0.1, ACTUAL));
        FastCNER fcrp = new FastCNER(rules);
        fcrp.setMaxRepeatLength(30);
        fcrp.setReplicationSupport(true);
        fcrp.setDebug(debug);

        String input = "PT 14.2 , PTT 108.5 , INR 1.3 .";
        input = "The patient vital signs temperature 100.6 .\n";
        HashMap<String, ArrayList<Span>> result;
        result = fcrp.processString(input);
        printMatches(result, input, fcrp);
        evalMatch(result.get("TEST"), new Span[]{new Span(4, 11)});


    }


    private void printMatches(HashMap<String, ArrayList<Span>> result, String input) {
        if (print) {
            System.out.println("Results for: " + input);
            for (Map.Entry<String, ArrayList<Span>> ent : result.entrySet()) {
                System.out.println(ent.getKey());
                for (Span span : ent.getValue()) {
                    System.out.println("\t" + span.begin + "-" + span.end + "\t" + input.substring(span.begin, span.end) + "\t" + span.text + "\t" + span.ruleId);
                }

            }
        }
    }

    private void printMatches(HashMap<String, ArrayList<Span>> result, String input, FastCNER fastCNER) {
        if (print) {
            System.out.println("Results for: " + input);
            for (Map.Entry<String, ArrayList<Span>> ent : result.entrySet()) {
                System.out.println(ent.getKey());
                for (Span span : ent.getValue()) {
                    System.out.println("\t" + span.begin + "-" + span.end + "\t" + input.substring(span.begin, span.end) + "\t" + span.text + "\t" + fastCNER.getMatchedRuleString(span));
                }

            }
        }
    }

    private boolean evalMatch(ArrayList<Span> results, Span[] answer) {
        if (results == null || results.size() != answer.length)
            return false;
        for (int i = 0; i < answer.length; i++) {
            Span reSpan = results.get(i);
            Span anSpan = answer[i];
            if (reSpan.begin != anSpan.begin || reSpan.end != anSpan.end)
                return false;
        }
        return true;
    }
}