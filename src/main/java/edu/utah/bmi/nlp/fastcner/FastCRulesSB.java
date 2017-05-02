package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.fastner.IOUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * This class extend FastCRules to support square brackets (not support nested square brackets)
 * Note: in order to make the implementation simpler, parenthesis is only used to capture the target group.
 * Square brackets are used to provide logic OR expression, always use together with "|"
 *
 * @author Jianlin Shi
 *         Created on 5/27/16.
 */
public class FastCRulesSB extends FastCRules {

    public FastCRulesSB() {
    }


    public FastCRulesSB(String ruleStr) {
        initiate(ruleStr, "\t");
    }

    public void initiate(String ruleStr, String splitter) {
        method = "scorewidth";
        ruleStore = IOUtil.parseRuleStr(ruleStr, splitter, true);
        initiate(ruleStore);
    }

    public void initiate(HashMap<Integer, Rule> ruleStore) {
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            addSBRule(ent.getValue());
        }
    }

    public FastCRulesSB(HashMap<Integer, Rule> ruleStore) {
        method = "scorewidth";
//        initiate(ruleStore);
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            addSBRule(ent.getValue());
        }
    }

    public boolean addSBRule(Rule rule) {
        char[] crule = rule.rule.toCharArray();

        if (rule.rule.indexOf("[") != -1) {
            ArrayList<Rule> rules = expandSB(rule);
            for (Rule subrule : rules) {
                addRule(subrule);
            }
        } else {
            addRule(rule);
        }
        return true;
    }

    public ArrayList<Rule> expandSB(Rule rule) {
        ArrayList<Rule> expandedRules = new ArrayList<>();
        ArrayList<StringBuilder> ruleStringBuilders = new ArrayList<>();
        String ruleString = rule.rule;
        ruleStringBuilders.add(new StringBuilder());
        final int OUT = 0, IN = 1;
        int status = OUT;
        char[] ruleChars = ruleString.toCharArray();
//      keep track of previous char, so that this expansion can avoid "\[",
        char preCh = ' ', nextCh = ' ';
        ArrayList<StringBuilder> branches = new ArrayList<>();
        for (int i = 0; i < ruleChars.length; i++) {
            char ch = ruleChars[i];
            if (i > 0)
                preCh = ruleChars[i - 1];
//           save the next char to avoid put '\' before '[' in the ruleMap, only need '['
            if (i < ruleChars.length - 1)
                nextCh = ruleChars[i + 1];
            else
                nextCh = ' ';
            if (status == OUT && (ch != '[' || preCh == '\\')) {
                if (ch == '\\' && (nextCh == '[' || nextCh == ']')) {
                    preCh = ch;
                    continue;
                }
                for (int j = 0; j < ruleStringBuilders.size(); j++) {
                    ruleStringBuilders.get(j).append(ch);
                }
            } else if (status == OUT && ch == '[' && preCh != '\\') {
                status = IN;
                branches = new ArrayList<>();
                branches.add(new StringBuilder());
                continue;
            } else if (status == IN && (ch != ']' || preCh == '\\')) {
                if (ch == '|') {
                    branches.add(new StringBuilder());
                } else if (ch == '\\' && (nextCh == '[' || nextCh == ']')) {
                    preCh = ch;
                    continue;
                } else {
                    branches.get(branches.size() - 1).append(ch);
                }
            } else if (status == IN && ch == ']' && preCh != '\\') {
                status = OUT;
                int previousSize = ruleStringBuilders.size();
                if (ch == '\\' && (nextCh == '[' || nextCh == ']')) {
                    preCh = ch;
                    continue;
                }
                for (int j = 0; j < previousSize; j++) {
                    StringBuilder sb = new StringBuilder(ruleStringBuilders.get(j));
                    ruleStringBuilders.get(j).append(branches.get(0));
                    for (int k = 1; k < branches.size(); k++) {
                        ruleStringBuilders.add(new StringBuilder(sb));
                        ruleStringBuilders.get(ruleStringBuilders.size() - 1).append(branches.get(k));
                    }
                }
            }
        }

        HashSet<String> cleanSet = new HashSet<>();
        for (StringBuilder subRule : ruleStringBuilders) {
            cleanSet.add(subRule.toString());
        }
        for (StringBuilder subRule : ruleStringBuilders) {
            if (debug)
                System.out.println(subRule.toString() + "\t" + rule.ruleName);
            Rule newRule = new Rule(rule.id, subRule.toString(), rule.ruleName, rule.score, rule.type);
            expandedRules.add(newRule);
        }

        return expandedRules;
    }


    /**
     * This implementation is using recursion instead of looping--may be more efficient
     * TODO it's buggish now. check the FastCNERTest.test8--It will build the map, but some innest End Map doesn't contain Determinant
     *
     * @param i
     * @param rule
     * @param crule
     * @param parentMap
     * @return
     */
    protected HashMap buildOneStepRule(int i, Rule rule, char[] crule, HashMap parentMap) {
        HashMap ruleMap2 = new HashMap();
        if (i == crule.length) {
            if (parentMap.containsKey(END)) {
                ((HashMap) parentMap.get(END)).put(rule.ruleName, rule.id);
            } else {
                HashMap ruleMap1 = new HashMap();
                ruleMap1.put(rule.ruleName, rule.id);
                ruleMap2.put(END, ruleMap1.clone());
            }
            setScore(rule.id, rule.score);
        } else {
            if (parentMap.containsKey(crule[i])) {
                parentMap.put(crule[i], buildOneStepRule(i + 1, rule, crule, (HashMap) parentMap.get(crule[i])));
                ruleMap2 = parentMap;
            } else {
                parentMap.put(crule[i], buildOneStepRule(i + 1, rule, crule, new HashMap()));
                ruleMap2 = parentMap;
            }
        }
        return ruleMap2;
    }

    public HashMap<Object, Object> getRulesMap() {
        return this.rulesMap;
    }

}
