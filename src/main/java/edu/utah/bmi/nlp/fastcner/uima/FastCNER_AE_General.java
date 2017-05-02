/*******************************************************************************
 * Copyright  Apr 11, 2015  Department of Biomedical Informatics, University of Utah
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
package edu.utah.bmi.nlp.fastcner.uima;

import edu.utah.bmi.nlp.core.SimpleParser;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastner.TypeDefinition;
import edu.utah.bmi.nlp.fastner.uima.FastNER_AE_General;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;


/**
 * This is a demo to use FastCNER.java in UIMA AE. The type system is implemented through reflection.
 *
 * @author Jianlin Shi
 */
public class FastCNER_AE_General extends FastNER_AE_General {


    public static final String PARAM_REPLICATION_SUPPORT = "ReplicationSupport";
    protected boolean replicationSupport;

    public static final String PARAM_MAXREPEATLENGTH = "MaxRepeatLength";


    protected int maxRepeatLength;


    public void initialize(UimaContext cont) {
        super.initialize(cont);
    }

    protected LinkedHashMap<String, TypeDefinition> initFastNER(UimaContext cont, String ruleStr) {
        Object obj;

        obj = cont.getConfigParameterValue(PARAM_REPLICATION_SUPPORT);
        if (obj == null)
            replicationSupport = true;
        else
            replicationSupport = (Boolean) obj;
        obj = cont.getConfigParameterValue(PARAM_MAXREPEATLENGTH);
        if (obj == null)
            maxRepeatLength = 50;
        else
            maxRepeatLength = (int) obj;
        fastNER = new FastCNER(ruleStr);
        ((FastCNER) fastNER).setReplicationSupport(replicationSupport);
        ((FastCNER) fastNER).setMaxRepeatLength(maxRepeatLength);
        if (debug) {
            fastNER.setDebug(true);
        }
        return fastNER.getTypeDefinition();
    }

    public void process(JCas jcas) throws AnalysisEngineProcessException {
        ArrayList<Annotation> sentences = new ArrayList<>();
        FSIndex annoIndex = jcas.getAnnotationIndex(sentenceTypeId);
        Iterator annoIter = annoIndex.iterator();

        while (annoIter.hasNext()) {
            sentences.add((Annotation) annoIter.next());
        }
        if (sentences.size() > 0) {
            for (Annotation sentence : sentences) {
                HashMap<String, ArrayList<Span>> concepts = ((FastCNER) fastNER).processAnnotation(sentence);
//              store found concepts in annotation
                if (concepts.size() > 0) {
                    saveConcepts(jcas, concepts);
                }
            }
        } else {
            System.out.println("This document has not been sentence segmented. Use simple segmenter instead.");
            String text = jcas.getDocumentText();
            ArrayList<ArrayList<Span>> simpleSentences = SimpleParser.tokenizeDecimalSmartWSentences(text, true);
            for (ArrayList<Span> sentence : simpleSentences) {
                Span sentenceSpan = new Span(sentence.get(0).begin, sentence.get(sentence.size() - 1).end);
                sentenceSpan.text = text.substring(sentenceSpan.begin, sentenceSpan.end);
                saveAnnotation(jcas, SentenceTypeConstructor, sentenceSpan.begin, sentenceSpan.end);
                HashMap<String, ArrayList<Span>> concepts = ((FastCNER) fastNER).processSpan(sentenceSpan);
//              store found concepts in annotation
                if (concepts.size() > 0) {
                    saveConcepts(jcas, concepts);
                }
            }
        }
    }

}