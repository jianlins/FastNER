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
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastner.FastNER;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is an extension of FastRulesProcessor, so that it supports capturing group within rule.
 * Process the input tokens against ruleStore.
 *
 * @author Jianlin Shi
 */
public class FastCNER extends FastNER {


    public FastCNER(String ruleFile) {
        // read ruleStore from ruleFile, initiate Patterns
        initiate(ruleFile, true);
    }

    public FastCNER(String ruleFile, boolean constructRuleMap) {
        // read ruleStore from ruleFile, initiate Patterns
        initiate(ruleFile, true, constructRuleMap);
    }

    public FastCNER(HashMap<Integer, Rule> ruleStore) {
        initiate(ruleStore);

    }

    protected void initiate(HashMap<Integer, Rule> ruleStore) {
        fastRule = new FastCRuleSB(ruleStore);
    }

    public HashMap<String, ArrayList<Span>> processString(String text) {
        return fastRule.processString(text);
    }

    /**
     * The span here is referring to a sentence span, or paragraph span or document span
     *
     * @param span input Span (a range of text for process)
     * @return matched results
     */
    public HashMap<String, ArrayList<Span>> processSpan(Span span) {
        return ((FastCRule) fastRule).processSpan(span);
    }

    public HashMap<String, ArrayList<Span>> processAnnotation(Annotation sentence) {
        Span span = new Span(sentence.getBegin(), sentence.getEnd(), sentence.getCoveredText());
        return processSpan(span);
    }


    public void setReplicationSupport(boolean support) {
        ((FastCRule) fastRule).setReplicationSupport(support);
    }

    public void setCompareMethod(String method) {
        ((FastCRule) fastRule).setCompareMethod(method);
    }

    public void setSpecialCharacterSupport(Boolean scSupport) {
        ((FastCRule) fastRule).setSpecialCharacterSupport(scSupport);
    }


    public void setMaxRepeatLength(int maxRepeatLength) {
        ((FastCRule) fastRule).setMaxRepeatLength(maxRepeatLength);
    }


}


