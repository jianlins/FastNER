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
import edu.utah.bmi.nlp.fastcner.UnicodeChecker;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * <p>
 * This class is an extension of FastRule, so that it supports capturing group within rule
 * </p>
 * <p>
 * Define a group as following format:
 * </p>
 * <p>
 * token1 \( token2 token3 \) token4&lt;TAB&gt;Determinant
 * </p>
 * <p>
 * The "token2 token3" will be considered within the group to be captured. When finding a match from token1-token4,
 * the report will only report token2-token3.
 * </p>
 *
 * @author Jianlin Shi
 */
public class FastRuleWG extends FastRuleWOG {
//    fields are defined in abstract class

    protected HashMap<String, IntervalST> overlapCheckers = new HashMap<>();
    public FastRuleWG() {

    }

    public FastRuleWG(String ruleStr) {
//        support read from OWl file, TSV file or OWL file directory
        super(ruleStr);


    }

    public FastRuleWG(String ruleStr, boolean caseSensitive) {
        super(ruleStr, caseSensitive);
    }

    public FastRuleWG(HashMap<Integer, NERRule> ruleStore) {
        super(ruleStore);
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
            if (rule.containsKey("\\(")) {
                process(contextTokens, getText, getBegin, getEnd, (HashMap) rule.get("\\("), currentPosition, matchEnd, currentPosition, matches);
            }
            if (rule.containsKey("\\)")) {
                process(contextTokens, getText, getBegin, getEnd, (HashMap) rule.get("\\)"), matchBegin, currentPosition - 1, currentPosition, matches);
            }
        } else if (currentPosition == contextTokens.size() && rule.containsKey(END)) {
            // if no () is used in this definition, use the whole rule string
            matchEnd = matchEnd == -1 ? currentPosition - 1 : matchEnd;
            addDeterminants(rule, matches, getBegin.apply(contextTokens, matchBegin), getEnd.apply(contextTokens, matchEnd));
        }
    }

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
                IntervalST<Integer> overlapChecker = overlapCheckers.get(key);
                Object overlappedPos = overlapChecker.get(new Interval1D(currentSpan.begin, currentSpan.end - 1));
                if (overlappedPos != null) {
                    int pos = (int) overlappedPos;
                    Span overlappedSpan = currentSpanList.get(pos);
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("\t\tOverlapped with: " + overlappedSpan.begin + ", " + overlappedSpan.end + "\t" );
//                                text.substring(overlappedSpan.begin - offset, overlappedSpan.end - offset));
                    if (((NERSpan) currentSpan).compareTo((NERSpan) overlappedSpan) <=0) {
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("\t\tSkip this span ...");
                        continue;
                    }
                    currentSpanList.set(pos, currentSpan);
                    overlapChecker.remove(new Interval1D(overlappedSpan.begin, overlappedSpan.end - 1));
                    overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), pos);
                } else {
                    overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), currentSpanList.size());
                    currentSpanList.add(currentSpan);
                }


            } else {
                currentSpanList = new ArrayList<Span>();
                currentSpanList.add(currentSpan);
                IntervalST<Integer> overlapChecker = new IntervalST<Integer>();
                overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), 0);
                overlapCheckers.put((String) key, overlapChecker);
            }
            matches.put((String) key, currentSpanList);
        }
    }



}
