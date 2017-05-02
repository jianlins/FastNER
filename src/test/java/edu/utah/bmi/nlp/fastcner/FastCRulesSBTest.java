package edu.utah.bmi.nlp.fastcner;


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jianlin Shi
 *         Created on 6/2/16.
 */
public class FastCRulesSBTest {


    @Test
    public void expandSB() throws Exception {
        FastCRulesSB fcruleSB = new FastCRulesSB("src/test/resources/ruleStore/crule_test.tsv");
        fcruleSB = new FastCRulesSB("conf/crule_test.xlsx");
        ArrayList<Rule> rules=fcruleSB.expandSB(new Rule(1,"ab[c|d]e[f|g|h]","R1", 1.5, DeterminantValueSet.Determinants.ACTUAL));
        for(Map.Entry<Integer,Rule> ent:fcruleSB.ruleStore.entrySet()){
            System.out.println(ent.getValue());
        }

    }



    @Test
    public void test() {
        HashMap<Integer,Rule>rules=new HashMap<>();
        rules.put(1,new Rule(1,"ab\\[c|d\\]e[f|g|h]","R1", 1.5, DeterminantValueSet.Determinants.ACTUAL));
        FastCRulesSB fcruleSB = new FastCRulesSB(rules);
        fcruleSB.printRulesMap();
//        fcruleSB.addRule(fcruleSB.ruleStore.get(0));
    }

    @Test
    public void test2() {
        HashMap<Integer,Rule>rules=new HashMap<>();
        rules.put(1,new Rule(1,"abc","R1", 1.5, DeterminantValueSet.Determinants.PSEUDO));
        rules.put(2,new Rule(2,"bc","R1", 1, DeterminantValueSet.Determinants.ACTUAL));
        rules.put(3,new Rule(3,"b","R2", 1, DeterminantValueSet.Determinants.ACTUAL));
        FastCNER fcruleSB = new FastCNER(rules);
        fcruleSB.printRulesMap();
        HashMap<String, ArrayList<Span>> res = fcruleSB.processString("a ab bc.");
        for (Map.Entry<String, ArrayList<Span>>entry: res.entrySet()){
            System.out.println(entry.getValue());
        }
//        fcruleSB.addRule(fcruleSB.ruleStore.get(0));
    }




}