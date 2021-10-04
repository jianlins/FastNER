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

package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.fastner.FastRuleFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class extend FastCRule to support square brackets (not support nested square brackets)
 * Note: in order to make the implementation simpler, parenthesis is only used to capture the target group.
 * Square brackets are used to provide logic OR expression, always use together with "|"
 *
 * @author Jianlin Shi
 * Created on 5/27/16.
 */
public class FastCRuleSB extends FastCRule {

    public FastCRuleSB() {
    }


    public FastCRuleSB(String ruleStr) {
        initiate(ruleStr, "\t");
    }

    public void initiate(String ruleStr, String splitter) {
        method = "scorewidth";
        ruleStore = (HashMap<Integer, Rule>) FastRuleFactory.buildRuleStore(ruleStr, null, true, true)[0];
        initiate(ruleStore);
    }

    public void initiate(HashMap<Integer, Rule> ruleStore) {
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            addSBRule(ent.getValue());
        }
    }

    public FastCRuleSB(HashMap<Integer, Rule> ruleStore) {
        method = "scorewidth";
//        initiate(ruleStore);
        this.ruleStore = ruleStore;
        for (Map.Entry<Integer, Rule> ent : ruleStore.entrySet()) {
            addSBRule(ent.getValue());
        }
    }

    public boolean addSBRule(Rule rule) {
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



    // TODO it's buggish now. check the FastCNERTest.test8--It will build the map, but some innest End Map doesn't contain Determinant

    protected HashMap buildOneStepRule(int i, NERRule rule, char[] crule, HashMap parentMap) {
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
