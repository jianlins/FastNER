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
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastcner.FastCRuleCN;
import edu.utah.bmi.nlp.fastcner.FastCRules;
import edu.utah.bmi.nlp.fastcner.FastCRulesSB;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * @author Jianlin Shi
 * Created on 3/5/17.
 */
public class FastRuleFactory {

    public FastRuleFactory() {

    }

    public static FastRule createFastRule(Class fastNER, String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive) {
        return createFastRule(fastNER, ruleStr, typeDefinition, splitter, caseSensitive);
    }

    public static FastRule createFastRule(Class fastNER, String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive, boolean constructRuleMap) {
        FastRule fastRule = null;
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        HashMap<Integer, Rule> rules = new HashMap<>();
        boolean[] thisRuleType = new boolean[]{false, false, false, false, false, false};
        if (testFileStr.equals(".tsv") || testFileStr.equals(".csv") || testFileStr.equals("xlsx") || testFileStr.equals(".owl")) {
            thisRuleType = IOUtil.readAgnosticFile(ruleStr, rules, typeDefinition, caseSensitive, thisRuleType);
        } else {
            thisRuleType = IOUtil.readCSVString(ruleStr, rules, typeDefinition, splitter, caseSensitive, thisRuleType);
        }
        if (constructRuleMap) {
            if (thisRuleType[0] || fastNER == FastCNER.class) {
//              Support Chinese
                if (thisRuleType[5]) {
                    fastRule = new FastCRuleCN(rules);
//            support square bracket
                } else if (thisRuleType[2])
                    fastRule = new FastCRulesSB(rules);
                else
                    fastRule = new FastCRules(rules);
                if (thisRuleType[3])
                    ((FastCRules) fastRule).setReplicationSupport(true);
            } else {
                if (thisRuleType[4]) {
                    fastRule = new FastRuleWGN(rules);
//            support group
                } else if (thisRuleType[1]) {
                    fastRule = new FastRuleWG(rules);
                } else {
                    fastRule = new FastRuleWOG(rules);
                }
            }
        }
        return fastRule;
    }


}
