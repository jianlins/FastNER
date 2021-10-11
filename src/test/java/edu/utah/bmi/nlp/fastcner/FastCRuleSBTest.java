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


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jianlin Shi
 *         Created on 6/2/16.
 */
public class FastCRuleSBTest {


    @Test
    public void expandSB() throws Exception {
//        FastCRuleSB fcruleSB = new FastCRuleSB("src/test/resources/ruleStore/crule_test.tsv");
        FastCRuleSB fcruleSB = new FastCRuleSB("conf/crule_test.xlsx");
        ArrayList<Rule> rules=fcruleSB.expandSB(new NERRule(1,"ab[c|d]e[f|g|h]","R1", 1.5, DeterminantValueSet.Determinants.ACTUAL));
        for(Map.Entry<Integer, Rule> ent:fcruleSB.ruleStore.entrySet()){
            System.out.println(ent.getValue());
        }

    }



    @Test
    public void test() {
        HashMap<Integer, Rule>rules=new HashMap<>();
        rules.put(1,new NERRule(1,"ab\\[c|d\\]e[f|g|h]","R1", 1.5, DeterminantValueSet.Determinants.ACTUAL));
        FastCRuleSB fcruleSB = new FastCRuleSB(rules);
        fcruleSB.printRulesMap();
//        fcruleSB.addRule(fcruleSB.ruleStore.get(0));
    }

    @Test
    public void test2() {
        HashMap<Integer, Rule>rules=new HashMap<>();
        rules.put(1,new NERRule(1,"abc","R1", 1.5, DeterminantValueSet.Determinants.PSEUDO));
        rules.put(2,new NERRule(2,"bc","R1", 1, DeterminantValueSet.Determinants.ACTUAL));
        rules.put(3,new NERRule(3,"b","R2", 1, DeterminantValueSet.Determinants.ACTUAL));
        FastCNER fcruleSB = new FastCNER(rules);
        fcruleSB.printRulesMap();
        HashMap<String, ArrayList<Span>> res = fcruleSB.processString("a abc bc.");
        for (Map.Entry<String, ArrayList<Span>>entry: res.entrySet()){
            System.out.println(entry.getValue());
        }
//        fcruleSB.addRule(fcruleSB.ruleStore.get(0));
    }




}