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
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastcner.UnicodeChecker;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

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


}
