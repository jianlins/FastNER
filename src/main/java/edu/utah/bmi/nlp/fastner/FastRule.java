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


import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.SimpleParser;
import edu.utah.bmi.nlp.core.Span;

import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an abstract class, which implements initiation methods and processTokens abstract method.
 * <p>
 * This class is to construct the chained-up HashMaps structure for the rulesMap, and provide methods to
 * process the rulesMap
 *
 * @author Jianlin Shi
 * The results will be added to the input HashMap&lt;String, ArrayList&lt;Span&gt;&gt;, because there might be more than one applicable rule.
 * -The Span ( @see Span#Span(int, int) ) stores the span information of the evidence support the corresponding Determinants
 * -Determinants are defined in ContextValueSet.Determinants ( @see ContextValueSet#ContextValueSet()), which is corresponding
 * to the last two elements in each rule defined in the rule CSV file.
 */
public abstract class FastRule {

    public static Logger logger = edu.utah.bmi.nlp.core.IOUtil.getLogger(FastRule.class);

    protected boolean removePseudo = true;
    protected HashMap rulesMap = new HashMap();
    protected final Determinants END = Determinants.END;
    public HashMap<Integer, Rule> ruleStore = new HashMap<>();

    protected BiFunction<ArrayList, Integer, Integer> getSpanBegin, getSpanEnd, getBeginId, getEndId;
    protected BiFunction<ArrayList, Integer, String> getSpanText, getStringText;


    public FastRule() {

    }

    public FastRule(HashMap<Integer, Rule> ruleStore) {
        initiate(ruleStore);
    }

    public void initiate(String ruleStr, boolean caseSensitive) {
        ruleStore = (HashMap<Integer, Rule>) FastRuleFactory.buildRuleStore(ruleStr, null, caseSensitive, true)[0];
        initiate(ruleStore);
    }


    public void initiate(HashMap<Integer, Rule> ruleStore) {
        rulesMap.clear();
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            Rule rule = ent.getValue();
            if (rule.rule.indexOf("[") != -1) {
                ArrayList<Rule> rules = expandSB(rule);
                for (Rule subrule : rules) {
                    addRule(subrule);
                }
            } else {
                addRule(rule);
            }
        }
        initiateFunctions();
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
            logger.logp(Level.FINEST, getClass().getCanonicalName(), "expandSB", subRule.toString() + "\t" + rule.ruleName);
            NERRule newRule = new NERRule(rule.id, subRule.toString(), rule.ruleName, rule.score, rule.type);
            expandedRules.add(newRule);
        }

        return expandedRules;
    }

    protected void initiateFunctions() {
        getSpanEnd = (list, id) -> ((Span) list.get(id)).getEnd();
        getSpanBegin = (list, id) -> ((Span) list.get(id)).getBegin();
        getSpanText = (list, id) -> ((Span) list.get(id)).getText();
        getBeginId = (list, id) -> id;
        getEndId = (list, id) -> id;
        getStringText = (list, id) -> ((String) list.get(id));
    }


    protected boolean addRule(Rule rule) {
        // use to store the HashMap sub-chain that have the key chain that meet
        // the rule[]
        ArrayList<HashMap> rules_tmp = new ArrayList<HashMap>();
        HashMap rule1 = rulesMap;
        HashMap rule2 = new HashMap();
        HashMap rule_t;
        String[] ruleContent = rule.rule.split("\\s+");
        int length = ruleContent.length;
        int i = 0;
        rules_tmp.add(rulesMap);
        while (i < length && rule1 != null && rule1.containsKey(ruleContent[i])) {
            rule1 = (HashMap) rule1.get(ruleContent[i]);
            i++;
        }
        // if the rule has been included
        if (i > length)
            return false;
        // start with the determinant, construct the last descendant HashMap
        // <Determinant, null>
        if (i == length) {
            if (rule1.containsKey(END)) {
                ((HashMap) rule1.get(END)).put(rule.ruleName, rule.id);
            } else {
                rule2.put(rule.ruleName, rule.id);
                rule1.put(END, rule2.clone());
            }
            return true;
        } else {
            rule2.put(rule.ruleName, rule.id);
            rule2.put(END, rule2.clone());
            rule2.remove(rule.ruleName);
            // filling the HashMap chain which ruleStore doesn't have the key chain
            for (int j = length - 1; j > i; j--) {
                rule_t = (HashMap) rule2.clone();
                rule2.clear();
                rule2.put(ruleContent[j], rule_t);
            }
        }
        rule1.put(ruleContent[i], rule2.clone());
        return true;
    }

    public HashMap<String, ArrayList<Span>> processString(String text, int begin, int end) {
        String sentence = text.substring(begin, end);
        return processString(sentence);
    }

    public HashMap<String, ArrayList<Span>> processString(String text) {
        ArrayList<Span> tokens = SimpleParser.tokenize2Spans(text, false);
        return processSpans(tokens);
    }

    protected abstract HashMap<String, ArrayList<Span>> processTokens(ArrayList<String> tokens);

    protected abstract HashMap<String, ArrayList<Span>> processSpans(ArrayList<Span> tokens);

    protected void removePseudoMatches(HashMap<String, ArrayList<Span>> matches) {
        for (Map.Entry<String, ArrayList<Span>> entry : matches.entrySet()) {
            Iterator<Span> spanIterator = entry.getValue().iterator();
            while (spanIterator.hasNext()) {
                Span thisSpan = spanIterator.next();
                if (ruleStore.get(thisSpan.ruleId).type == Determinants.PSEUDO)
                    spanIterator.remove();
            }
        }
    }

    public void setRemovePseudo(boolean removePseudo) {
        this.removePseudo = removePseudo;
    }

    public String getRuleString(int ruleId) {
        return ruleStore.get(ruleId).rule;
    }


    public Rule getRule(int pos) {
        return ruleStore.get(pos);
    }

    public void printRulesMap() {
        printEmbededMap(rulesMap, "");
    }

    private void printEmbededMap(HashMap<Object, Object> ruleMap, String offset) {
        for (Map.Entry<Object, Object> ent : ruleMap.entrySet()) {
            if (logger.isLoggable(Level.FINER))
                logger.finer(offset + "key: " + ent.getKey());
            if (ent.getValue().getClass() == HashMap.class) {
                printEmbededMap((HashMap<Object, Object>) ent.getValue(), offset + "\t");
            } else if (logger.isLoggable(Level.FINER)) {
                logger.finer(offset + "pair: " + ent.getKey() + "->" + ent.getValue());
            }

        }
    }

    public HashMap<Integer, Rule> getRuleStore() {
        return ruleStore;
    }


}
