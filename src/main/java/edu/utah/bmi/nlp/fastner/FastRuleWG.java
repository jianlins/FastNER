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

import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;

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

    public FastRuleWG() {

    }

    public FastRuleWG(String ruleStr) {
//        support read from OWl file, TSV file or OWL file directory
        super(ruleStr);
    }

    public FastRuleWG(String ruleStr, boolean caseSensitive) {
        super(ruleStr, caseSensitive);
    }

    public FastRuleWG(HashMap<Integer, Rule> ruleStore) {
        super(ruleStore);
    }



    public HashMap<String, ArrayList<Span>> processTokens(ArrayList<String> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
            // System.out.println(contextTokens.get(i));
            processTokens(contextTokens, rulesMap, i, 0, i, matches);
        }
        removePseudoMatches(matches);
        return matches;
    }


    protected void processTokens(ArrayList<String> contextTokens, HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                                 HashMap<String, ArrayList<Span>> matches) {
        // when reach the end of the tunedcontext, end the iteration
        if (currentPosition < contextTokens.size()) {
            // start processing the tunedcontext tokens
            String thisToken = contextTokens.get(currentPosition);
//			System.out.println("thisToken-"+thisToken);
            if (rule.containsKey("\\w+")) {
                processTokens(contextTokens, (HashMap) rule.get("\\w+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            // if the end of a rule is met
            if (rule.containsKey(END)) {
                // if no () is used in this definition, use the whole rule string
                addDeterminants(rule, matches, matchBegin, matchEnd == 0 ? currentPosition - 1 : matchEnd);
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisToken)) {
                processTokens(contextTokens, (HashMap) rule.get(thisToken), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\d+") && StringUtils.isNumeric(thisToken)) {
                processTokens(contextTokens, (HashMap) rule.get("\\d+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\(")) {
                processTokens(contextTokens, (HashMap) rule.get("\\("), currentPosition, matchEnd, currentPosition, matches);
            }
            if (rule.containsKey("\\)")) {
                processTokens(contextTokens, (HashMap) rule.get("\\)"), matchBegin, currentPosition - 1, currentPosition, matches);
            }
        } else if (currentPosition == contextTokens.size() && rule.containsKey(END)) {
            // if no () is used in this definition, use the whole rule string
            addDeterminants(rule, matches, matchBegin, matchEnd == 0 ? currentPosition - 1 : matchEnd);
        }
    }

    public HashMap<String, ArrayList<Span>> processSpans(ArrayList<Span> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
//            System.out.println(contextTokens.get(i));
            processSpans(contextTokens, rulesMap, i, 0, i, matches);
        }
        removePseudoMatches(matches);
        return matches;
    }


    protected void processSpans(ArrayList<Span> contextTokens, HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                                HashMap<String, ArrayList<Span>> matches) {
        // when reach the end of the tunedcontext, end the iteration
        if (currentPosition < contextTokens.size()) {
            // start processing the tunedcontext tokens
            String thisToken = contextTokens.get(currentPosition).text;
//			System.out.println("thisToken-"+thisToken);
            if (rule.containsKey("\\w+")) {
                processSpans(contextTokens, (HashMap) rule.get("\\w+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            // if the end of a rule is met
            if (rule.containsKey(END)) {
                // if no () is used in this definition, use the whole rule string
                addDeterminants(rule, matches, contextTokens.get(matchBegin).begin, contextTokens.get(matchEnd == 0 ? currentPosition - 1 : matchEnd).end);
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisToken)) {
                processSpans(contextTokens, (HashMap) rule.get(thisToken), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\d+") && NumberUtils.isNumber(thisToken)) {
                processSpans(contextTokens, (HashMap) rule.get("\\d+"), matchBegin, matchEnd, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\(")) {
                processSpans(contextTokens, (HashMap) rule.get("\\("), currentPosition, matchEnd, currentPosition, matches);
            }
            if (rule.containsKey("\\)")) {
                processSpans(contextTokens, (HashMap) rule.get("\\)"), matchBegin, currentPosition - 1, currentPosition, matches);
            }
        } else if (currentPosition == contextTokens.size() && rule.containsKey(END)) {
            // if no () is used in this definition, use the whole rule string
            matchEnd = matchEnd == 0 ? currentPosition - 1 : matchEnd;
            addDeterminants(rule, matches, contextTokens.get(matchBegin).begin, contextTokens.get(matchEnd).end);
        }
    }


}
