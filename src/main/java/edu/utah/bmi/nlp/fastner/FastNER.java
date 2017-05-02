/*******************************************************************************
 * Copyright  April 20, 2016  Department of Biomedical Informatics, University of Utah
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package edu.utah.bmi.nlp.fastner;

import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * TODO need to support multiple groups (concepts) in one rule
 * e.g. F-scores of 89.8, 84.6 and 89.4 respectively for alcohol, drug, and nicotine
 * Process the input tokens against rulesMap.
 *
 * @author Jianlin Shi
 */
public class FastNER {
    protected FastRule fastRule;
    protected boolean caseSenstive = false;
    protected LinkedHashMap<String, TypeDefinition> typeDefinition;

    protected FastNER() {
    }


    public FastNER(String ruleFile) {
        initiate(ruleFile, caseSenstive);
    }

    public FastNER(String ruleFile, boolean caseSenstive) {
        this.caseSenstive = caseSenstive;
        initiate(ruleFile, caseSenstive);
    }

    /**
     * Automatically decide which FastRule will be initiated (whether supports group capture or not)
     *
     * @param ruleStr
     */
    protected void initiate(String ruleStr, boolean caseSenstive) {
        typeDefinition = new LinkedHashMap<>();
        fastRule = FastRuleFactory.createFastRule(this.getClass(), ruleStr, typeDefinition, "\t", caseSenstive);
    }


    public HashMap<String, ArrayList<Span>> processStringList(ArrayList<String> tokens) {
        return fastRule.processTokens(tokens);
    }

    public HashMap<String, ArrayList<Span>> processSpanList(ArrayList<Span> tokens) {
        return fastRule.processSpans(tokens);
    }

    public HashMap<String, ArrayList<Span>> processAnnotationList(ArrayList<Annotation> tokens) {
        ArrayList<Span> spans = new ArrayList<Span>();
        for (Annotation token : tokens) {
            if (caseSenstive)
                spans.add(new Span(token.getBegin(), token.getEnd(), token.getCoveredText()));
            else
                spans.add(new Span(token.getBegin(), token.getEnd(), token.getCoveredText().toLowerCase()));
        }
        return processSpanList(spans);
    }

    public String getMatchedNEName(int ruleId) {
        return fastRule.ruleStore.get(ruleId).ruleName;
    }

    public String getMatchedNEName(Span matchedSpan) {
        return getMatchedNEName(matchedSpan.ruleId);
    }

    public Determinants getMatchedNEType(Span matchedSpan) {
        return getMatchedNEType(matchedSpan.ruleId);
    }

    public Determinants getMatchedNEType(int ruleId) {
        return fastRule.ruleStore.get(ruleId).type;
    }

    public LinkedHashMap<String, TypeDefinition> getTypeDefinition() {
        return typeDefinition;
    }

    public String getRuleString(int ruleId) {
        return fastRule.ruleStore.get(ruleId).rule;
    }

    public String getRuleName(int ruleId) {
        return fastRule.getRule(ruleId).ruleName;
    }


    public Rule getRule(int ruleId) {
        return fastRule.getRule(ruleId);
    }

    public void printRulesMap() {
        this.fastRule.printRulesMap();
    }

    public Rule getMatchedRuleString(Span matchedSpan) {
        return fastRule.ruleStore.get(matchedSpan.ruleId);
    }

    public void setDebug(boolean debug) {
        fastRule.setDebug(debug);
    }

    public double getRuleScore(int ruleId) {
        return fastRule.getRule(ruleId).score;
    }

    public HashMap<Integer, Rule> getRuleStore() {
        return fastRule.getRuleStore();
    }

}
