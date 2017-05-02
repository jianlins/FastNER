package edu.utah.bmi.nlp.fastner;

import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastcner.FastCRules;
import edu.utah.bmi.nlp.fastcner.FastCRulesSB;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * @author Jianlin Shi
 *         Created on 3/5/17.
 */
public class FastRuleFactory {

    public FastRuleFactory() {

    }


    public static FastRule createFastRule(Class fastNER, String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive) {
        FastRule fastRule;
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        HashMap<Integer, Rule> rules = new HashMap<>();
        boolean[] thisRuleType = new boolean[]{false, false, false, false};
        if (testFileStr.equals(".tsv") || testFileStr.equals(".csv") || testFileStr.equals("xlsx") || testFileStr.equals(".owl")) {
            thisRuleType = IOUtil.readAgnosticFile(ruleStr, rules, typeDefinition, caseSensitive, thisRuleType);
        } else {
            thisRuleType = IOUtil.readCSVString(ruleStr, rules, typeDefinition, splitter, caseSensitive, thisRuleType);
        }
        if (thisRuleType[0] || fastNER== FastCNER.class) {
//            support square bracket
            if (thisRuleType[2])
                fastRule = new FastCRulesSB(rules);
            else
                fastRule = new FastCRules(rules);
            if (thisRuleType[3])
                ((FastCRules) fastRule).setReplicationSupport(true);
        } else {
//            support group
            if (thisRuleType[1]) {
                fastRule = new FastRuleWG(rules);
            } else {
                fastRule = new FastRuleWOG(rules);
            }
        }
        return fastRule;
    }


}
