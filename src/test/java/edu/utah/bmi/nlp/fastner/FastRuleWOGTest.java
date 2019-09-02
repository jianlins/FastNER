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

import edu.utah.bmi.nlp.core.SimpleParser;
import edu.utah.bmi.nlp.core.Span;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jianlin Shi on 7/25/17.
 */
public class FastRuleWOGTest {

    private FastNER fastNER;

    @Test
    public void processTokens() throws Exception {
        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "was \\( done yesterday \\)	0	Concept	ACTUAL";
        ArrayList<String> tokens = new ArrayList<>();
        tokens.addAll(Arrays.asList(text.split("[\\s\\.]+")));
        System.out.println(tokens);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processStringList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey()+"\t");
            entry.getValue().forEach((span)->{System.out.println(tokens.get(span.getBegin()));});
        }
    }

    @Test
    public void processSpans() throws Exception {

        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "emboli \\( protocol \\)	0	Concept	ACTUAL";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, false).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey()+"\t");
            entry.getValue().forEach((span)->{System.out.println(text.substring(span.getBegin(),span.getEnd()));});
        }
    }


    @Test
    public void processSpans2() throws Exception {

        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n" +
                "emboli protocol	0	Concept	PSEUDO";
        ArrayList<Span> tokens = SimpleParser.tokenizeDecimalSmartWSentences(text, false).get(0);

        fastNER = new FastNER(rule);
        HashMap<String, ArrayList<Span>> res = fastNER.processSpanList(tokens);
        for (Map.Entry<String, ArrayList<Span>> entry : res.entrySet()) {
            System.out.println(entry.getKey()+"\t");
            entry.getValue().forEach((span)->{System.out.println(text.substring(span.getBegin(),span.getEnd()));});
        }
    }
}