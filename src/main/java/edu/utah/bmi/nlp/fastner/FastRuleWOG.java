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

import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.NERSpan;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastcner.UnicodeChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;

import static edu.utah.bmi.nlp.core.NERSpan.byRuleLength;
import static edu.utah.bmi.nlp.core.NERSpan.scorewidth;

/**
 * This is a class extended from FastRule, which apply the full rule as a match (does not consider group capturing)
 *
 * @author Jianlin Shi
 */
@SuppressWarnings("rawtypes")
public class FastRuleWOG extends FastRule {
    //    fields are defined in abstract class
    protected HashMap<Integer, Integer> ruleLengths = new HashMap<Integer, Integer>();
    protected String spanCompareMethod = scorewidth;
    protected String widthCompareMethod = byRuleLength;

    public FastRuleWOG() {
    }

    public FastRuleWOG(String ruleStr) {
        initiate(ruleStr, false);
    }

    public FastRuleWOG(String ruleStr, boolean caseSensitive) {
        initiate(ruleStr, caseSensitive);
    }

    public FastRuleWOG(HashMap<Integer, NERRule> ruleStore) {
        initiate(ruleStore);
    }

    public void setCompareMethod(String method) {
        this.spanCompareMethod = method;
    }

    public void setWidthCompareMethod(String widthCompareMethod) {
        this.widthCompareMethod = widthCompareMethod;
    }

    protected boolean addRule(Rule rule) {
        // use to store the HashMap sub-chain that have the key chain that meet
        // the rule[]
        ArrayList<HashMap> rules_tmp = new ArrayList<HashMap>();
        HashMap rule1 = rulesMap;
        HashMap rule2 = new HashMap();
        HashMap rule_t;
        String[] ruleContent = rule.rule.split("\\s+");
        ruleLengths.put(rule.id, ruleContent.length);
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

    public HashMap<String, ArrayList<Span>> processTokens(ArrayList<String> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
            // System.out.println(contextTokens.get(i));
            processTokens(contextTokens, rulesMap, i, -1, i, matches);
        }
        if (removePseudo)
            removePseudoMatches(matches);
        return matches;
    }


    protected void processTokens(ArrayList<String> contextTokens, HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                                 HashMap<String, ArrayList<Span>> matches) {
        process(contextTokens, getStringText, getBeginId, getEndId,
                rule, matchBegin, matchEnd, currentPosition, matches);
    }

    public HashMap<String, ArrayList<Span>> processSpans(ArrayList<Span> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
//            System.out.println(contextTokens.get(i));
            processSpans(contextTokens, rulesMap, i, -1, i, matches);
        }
        if (removePseudo)
            removePseudoMatches(matches);
        return matches;
    }


    protected void processSpans(ArrayList<Span> contextTokens,
                                HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                                HashMap<String, ArrayList<Span>> matches) {
        process(contextTokens, getSpanText, getSpanBegin, getSpanEnd,
                rule, matchBegin, matchEnd, currentPosition, matches);
    }


    protected void process(ArrayList<?> contextTokens,
                           BiFunction<ArrayList, Integer, String> getText,
                           BiFunction<ArrayList, Integer, Integer> getBegin,
                           BiFunction<ArrayList, Integer, Integer> getEnd,
                           HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                           HashMap<String, ArrayList<Span>> matches) {
        // when reach the end of the tunedcontext, end the iteration
        if (currentPosition < contextTokens.size()) {
            // start processing the tunedcontext tokens
            String thisToken = getText.apply(contextTokens, currentPosition);
//			System.out.println("thisToken-"+thisToken);
            if (rule.containsKey("\\w+")) {
                process(contextTokens, getText, getBegin, getEnd, (HashMap) rule.get("\\w+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            // if the end of a rule is met
            if (rule.containsKey(END)) {
                // if no () is used in this definition, use the whole rule string
                addDeterminants(rule, matches, getBegin.apply(contextTokens, matchBegin), getEnd.apply(contextTokens, (matchEnd == -1 ? currentPosition - 1 : matchEnd)));
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisToken)) {
                process(contextTokens, getText, getBegin, getEnd, (HashMap) rule.get(thisToken), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\d+") && UnicodeChecker.isNumber(thisToken)) {
                process(contextTokens, getText, getBegin, getEnd, (HashMap) rule.get("\\d+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
        } else if (currentPosition == contextTokens.size() && rule.containsKey(END)) {
            // if no () is used in this definition, use the whole rule string
            matchEnd = matchEnd == -1 ? currentPosition - 1 : matchEnd;
            addDeterminants(rule, matches, getBegin.apply(contextTokens, matchBegin), getEnd.apply(contextTokens, matchEnd));
        }
    }


    @SuppressWarnings("unchecked")
    protected void addDeterminants(HashMap rule, HashMap<String, ArrayList<Span>> matches, int matchBegin, int matchEnd) {
        HashMap<String, Integer> deterRule = (HashMap<String, Integer>) rule.get(END);
        Span currentSpan;
        ArrayList<Span> currentSpanList;
        for (Object key : deterRule.keySet()) {
//          claim as Span instance, to be compatible with old methods
            int ruleId = deterRule.get(key);
            boolean contain = ruleLengths.containsKey(ruleId);
            contain = ruleStore.containsKey(ruleId);
            currentSpan = new NERSpan(matchBegin, matchEnd, ruleId, ruleLengths.get(ruleId), ruleStore.get(ruleId).score, "");
            ((NERSpan) currentSpan).setCompareMethod(spanCompareMethod);
            ((NERSpan) currentSpan).setWidthCompareMethod(widthCompareMethod);
            logger.finest(getRule(currentSpan.ruleId).toString());
            if (matches.containsKey((String) key)) {
//              because the ruleStore are all processed at the same time from the input left to the input right,
//                it becomes more efficient to compare the overlaps
                currentSpanList = matches.get((String) key);
                Span lastSpan = currentSpanList.get(currentSpanList.size() - 1);

//                  Since there is no directional preference, assume the span is not exclusive within each determinant.
                if (currentSpan.end < lastSpan.end) {
//                      if currentSpan is within lastSpan
                    continue;
                } else if (lastSpan.end > currentSpan.begin) {
//                      if overlap and current span has priority than last span
                    if (((NERSpan) currentSpan).compareTo((NERSpan) lastSpan) > 0) {
                        currentSpanList.remove(currentSpanList.size() - 1);
                    } else {
                        continue;
                    }
                }
                currentSpanList.add(currentSpan);
            } else {
                currentSpanList = new ArrayList<Span>();
                currentSpanList.add(currentSpan);
            }
            matches.put((String) key, currentSpanList);
        }
    }


}
