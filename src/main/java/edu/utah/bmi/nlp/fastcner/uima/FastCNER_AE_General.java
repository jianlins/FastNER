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
package edu.utah.bmi.nlp.fastcner.uima;

import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastner.uima.FastNER_AE_General;
import edu.utah.bmi.nlp.uima.ae.RuleBasedAEInf;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.*;


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
        if (markPseudo)
            fastNER.setRemovePseudo(false);
        return fastNER.getTypeDefinitions();
    }

    public void process(JCas jcas) throws AnalysisEngineProcessException {
        IntervalST<String> sectionTree = new IntervalST<>();
        int totalSections = 0;
        if (assignSection || forceAssignSections)
            totalSections = indexSections(jcas, sectionTree);

        LinkedHashMap<String, ArrayList<Annotation>> sentences = new LinkedHashMap<>();
        FSIndex annoIndex = jcas.getAnnotationIndex(SentenceType);
        Iterator annoIter = annoIndex.iterator();
        int totalSentences = 0;
        while (annoIter.hasNext()) {
            Annotation sentence = (Annotation) annoIter.next();
            totalSentences++;
            String sectionName = sectionTree.get(new Interval1D(sentence.getBegin(), sentence.getEnd()));
            if (sectionName == null) {
                if (totalSections == 0)
                    sectionName = SourceDocumentInformation.class.getSimpleName();
                else
                    continue;
            }

            if (!sentences.containsKey(sectionName))
                sentences.put(sectionName, new ArrayList<>());
            sentences.get(sectionName).add(sentence);

        }
        if (totalSentences > 0) {
            for (String sectionName : sentences.keySet()) {
                boolean outsiders = true;
                if ((includeSections.size() == 0 && excludeSections.size() > 0 && !excludeSections.contains(sectionName))
                        || (includeSections.size() > 0 && includeSections.contains(sectionName))
                        || (includeSections.size() == 0 && excludeSections.size() == 0)) {
                    outsiders = false;
                }

                for (Annotation sentence : sentences.get(sectionName)) {
                    HashMap<String, ArrayList<Span>> concepts = ((FastCNER) fastNER).processAnnotation(sentence);
//              store found concepts in annotation
                    if (concepts.size() > 0) {
                        if (outsiders)
                            saveOutsideScopeConcepts(jcas, concepts, sectionName);
                        else
                            saveConcepts(jcas, concepts, sectionName);
                    }
                }
            }
        } else {
            Collection<SourceDocumentInformation> docAnnotation = JCasUtil.select(jcas, SourceDocumentInformation.class);
            if (docAnnotation != null && docAnnotation.size() > 0)
                logger.info("Document: " + docAnnotation.iterator().next().getUri() + " has not been properly sentence segmented. Use simple segmenter instead.");

            String text = jcas.getDocumentText();
            ArrayList<ArrayList<Span>> simpleSentences = SimpleParser.tokenizeDecimalSmartWSentences(text, true);
            for (ArrayList<Span> sentence : simpleSentences) {
                Span sentenceSpan = new Span(sentence.get(0).begin, sentence.get(sentence.size() - 1).end);
                sentenceSpan.text = text.substring(sentenceSpan.begin, sentenceSpan.end);
                saveConcept(jcas, "Sentence", sentenceSpan.begin, sentenceSpan.end, null);
                for (Span token : sentence) {
                    saveConcept(jcas, "Token", token.begin, token.end, null);
                }
                HashMap<String, ArrayList<Span>> concepts = ((FastCNER) fastNER).processSpan(sentenceSpan);
//              store found concepts in annotation
                if (concepts.size() > 0) {
                    saveConcepts(jcas, concepts, null);
                }
            }
        }
    }

}