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
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.SimpleParser;
import edu.utah.bmi.nlp.core.Span;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.LogManager;
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
        ruleStore = (HashMap<Integer, Rule>) FastRuleFactory.buildRuleStore(ruleStr, null,caseSensitive,true)[0];
        initiate(ruleStore);
    }


    public void initiate(HashMap<Integer, Rule> ruleStore) {
        rulesMap.clear();
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            addRule(ent.getValue());
        }
        initiateFunctions();
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
            } else if (logger.isLoggable(Level.FINER)){
                logger.finer(offset + "pair: " + ent.getKey() + "->" + ent.getValue());
            }

        }
    }

    public HashMap<Integer, Rule> getRuleStore() {
        return ruleStore;
    }
}
