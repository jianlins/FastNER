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
package edu.utah.bmi.nlp.fastner.uima;

import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import edu.utah.bmi.nlp.fastner.FastNER;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.common.AnnotationComparator;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;


/**
 * This is a demo to use FastNER.java in UIMA AE. The type system is implemented through reflection.
 *
 * @author Jianlin Shi
 */
public class FastNER_AE_General extends JCasAnnotator_ImplBase {
    public static Logger logger = IOUtil.getLogger(FastNER_AE_General.class);


    //	a list of section names (can use short name if name space is "edu.utah.bmi.nlp.type.system."),
// separated by "|", ",", or ";".
    public static final String PARAM_INCLUDE_SECTIONS = "IncludeSections";

    public static final String PARAM_RULE_FILE_OR_STR = "RuleFileOrString";
    //    @ConfigurationParameter(name = PARAM_RULE_FILE_OR_STR)
//    protected String ruleFileName;
    public static final String PARAM_SENTENCE_TYPE_NAME = "SentenceTypeName";
    //    @ConfigurationParameter(name = PARAM_SENTENCE_TYPE_NAME)
//    protected String sentenceTypeName;
    public static final String PARAM_TOKEN_TYPE_NAME = "TokenTypeName";

    public static final String PARAM_MARK_PSEUDO = "MarkPseudo";

    public static final String PARAM_LOG_RULE_INFO = "LogRuleInfo";

    @Deprecated
    public static final String PARAM_ENABLE_DEBUG = "EnableDebug";

    public static final String PARAM_CASE_SENSITIVE = "CaseSensitive";

    //    @ConfigurationParameter(name = TOKEN_TYPE_NAME)
//    protected String tokenTypeName;
//    public static final String PARAM_CONCEPT_TYPE_NAME = "conceptTypeName";
    //    @ConfigurationParameter(name = CONCEPT_TYPE_NAME)
//    protected String conceptTypeName;
//    @ConfigurationParameter(name = CONCEPT_CATEGORY_FEATURE_NAME)
//    protected String conceptCategoryFeatureName;

    protected FastNER fastNER;
    //    according to different determinant, save the concept in different annotations
//    need to make sure the corresponding types (descriptor and Java classes) are available.
    protected HashMap<String, Constructor<? extends Annotation>> ConceptTypeClasses = new HashMap<String, Constructor<? extends Annotation>>();
    protected HashSet<String> includeSections = new HashSet<>();
    protected Class<? extends Annotation> SentenceType, TokenType;
    protected Constructor<? extends Annotation> SentenceTypeConstructor;
    protected Constructor<? extends Annotation> TokenTypeConstructor;
    protected HashMap<String, Class<? extends Concept>> ConceptTypes = new HashMap<>();
    protected HashMap<String, Constructor<? extends Concept>> ConceptTypeConstructors = new HashMap<>();
    protected boolean markPseudo = false, logRuleInfo = false;
    protected boolean caseSenstive = true;
    @Deprecated
    protected boolean debug = false;


    public void initialize(UimaContext cont) {
        String ruleStr, sentenceTypeName, tokenTypeName;
        ruleStr = (String) (cont
                .getConfigParameterValue(PARAM_RULE_FILE_OR_STR));
        Object obj;
        obj = cont.getConfigParameterValue(PARAM_SENTENCE_TYPE_NAME);
        if (obj == null)
            sentenceTypeName = "edu.utah.bmi.nlp.type.system.Sentence";
        else
            sentenceTypeName = (String) obj;

        obj = cont.getConfigParameterValue(PARAM_TOKEN_TYPE_NAME);
        if (obj == null)
            tokenTypeName = "edu.utah.bmi.nlp.type.system.Token";
        else
            tokenTypeName = (String) obj;


        obj = cont.getConfigParameterValue(PARAM_INCLUDE_SECTIONS);
        if (obj == null || ((String) obj).trim().length() == 0)
            includeSections.add("SourceDocumentInformation");
        else {
            for (String sectionName : ((String) obj).split("[\\|,;]")) {
                sectionName = sectionName.trim();
                includeSections.add(sectionName);
            }
        }

        obj = cont.getConfigParameterValue(PARAM_MARK_PSEUDO);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            markPseudo = true;
        obj = cont.getConfigParameterValue(PARAM_LOG_RULE_INFO);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            logRuleInfo = true;

        obj = cont.getConfigParameterValue(PARAM_ENABLE_DEBUG);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            debug = true;

        obj = cont.getConfigParameterValue(PARAM_CASE_SENSITIVE);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != true)
            caseSenstive = false;

        try {
            SentenceType = Class.forName(sentenceTypeName).asSubclass(Annotation.class);
            TokenType = Class.forName(tokenTypeName).asSubclass(Annotation.class);
            TokenTypeConstructor = TokenType.getConstructor(new Class[]{JCas.class, int.class, int.class});
            SentenceTypeConstructor = SentenceType.getConstructor(new Class[]{JCas.class, int.class, int.class});

            LinkedHashMap<String, TypeDefinition> conceptNames = initFastNER(cont, ruleStr);
            for (Map.Entry<String, TypeDefinition> conceptTypeSuperTypePair : conceptNames.entrySet()) {
                String fullTypeName = conceptTypeSuperTypePair.getValue().fullTypeName;
                Class conceptTypeClass = Class.forName(fullTypeName).asSubclass(Class.forName(conceptTypeSuperTypePair.getValue().getFullSuperTypeName()));
                ConceptTypes.put(conceptTypeSuperTypePair.getKey(), conceptTypeClass);
                ConceptTypeConstructors.put(conceptTypeSuperTypePair.getKey(), ConceptTypes.get(conceptTypeSuperTypePair.getKey()).getConstructor(new Class[]{JCas.class, int.class, int.class}));
            }
        } catch (
                ClassNotFoundException e)

        {
            System.err.println("You need to run this AE through AdaptableUIMACPERunner, " +
                    "which can automatically generate unknown Type classes and type descriptors.\n" +
                    "@see nlp-core project: edu.utah.bmi.uima.AdaptableUIMACPERunner");
            e.printStackTrace();
        } catch (
                NoSuchMethodException e)

        {
            e.printStackTrace();
        }

    }

    protected LinkedHashMap<String, TypeDefinition> initFastNER(UimaContext cont, String ruleStr) {
        fastNER = new FastNER(ruleStr, caseSenstive);
        if (markPseudo)
            fastNER.setRemovePseudo(false);
        return fastNER.getTypeDefinitions();
    }


    public void process(JCas jcas) throws AnalysisEngineProcessException {
        IntervalST<String> sectionTree = new IntervalST<>();
        int totalSections = indexSections(jcas, sectionTree);
        LinkedHashMap<String, ArrayList<Annotation>> sentences = new LinkedHashMap<>();
        ArrayList<Annotation> tokens = new ArrayList<>();
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

        // get token annotations
        annoIndex = jcas.getAnnotationIndex(TokenType);
        annoIter = annoIndex.iterator();
        while (annoIter.hasNext()) {
            tokens.add((Annotation) annoIter.next());
        }

        if (totalSentences > 0) {
            for (String sectionName : sentences.keySet()) {
                // make sure all the annotations are in ascending order regarding span offset
                Collections.sort(sentences.get(sectionName), new AnnotationComparator());
                Collections.sort(tokens, new AnnotationComparator());

//     Construct annotation_id-annotation_id map, easier and faster to find related annotations.
                TreeMap<Integer, TreeSet<Integer>> sentence2TokenMap = new TreeMap<Integer, TreeSet<Integer>>();
                AnnotationOper.buildAnnoMap(sentences.get(sectionName), tokens, sentence2TokenMap);

//        process each sentence that has at least one concept inside
                for (Map.Entry<Integer, TreeSet<Integer>> sentence : sentence2TokenMap.entrySet()) {
                    int sentenceId = sentence.getKey();
                    TreeSet<Integer> sentenceTokenIds = sentence2TokenMap.get(sentenceId);
                    ArrayList<Annotation> tokensInThisSentence = new ArrayList<Annotation>();
                    for (int tokenId : sentenceTokenIds) {
                        tokensInThisSentence.add(tokens.get(tokenId));
                    }
                    HashMap<String, ArrayList<Span>> concepts = fastNER.processAnnotationList(tokensInThisSentence);
//              store found concepts in annotation
                    if (concepts.size() > 0) {
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
                saveAnnotation(jcas, SentenceTypeConstructor, sentence.get(0).begin, sentence.get(sentence.size() - 1).end, null);
                logger.finest("Sentence: " + sentence.get(0).begin + "-" + sentence.get(sentence.size() - 1).end);
                for (Span token : sentence) {
                    saveAnnotation(jcas, TokenTypeConstructor, token.begin, token.end, null);
                }

                HashMap<String, ArrayList<Span>> concepts = fastNER.processSpanList(sentence);
//              store found concepts in annotation
                if (concepts.size() > 0) {
                    saveConcepts(jcas, concepts, null);
                }
            }
        }
    }

    protected int indexSections(JCas jCas, IntervalST<String> sectionTree) {
        int totalSections = 0;
        FSIndex annoIndex = jCas.getAnnotationIndex(SectionBody.class);
        Iterator annoIter = annoIndex.iterator();
        while (annoIter.hasNext()) {
            Annotation section = (Annotation) annoIter.next();
            String sectionName = section.getType().getShortName();
            if (includeSections.contains(sectionName)) {
                sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
            }
            totalSections++;
        }
        annoIndex = jCas.getAnnotationIndex(SectionHeader.class);
        annoIter = annoIndex.iterator();
        while (annoIter.hasNext()) {
            Annotation section = (Annotation) annoIter.next();
            String sectionName = section.getType().getShortName();
            if (includeSections.contains(sectionName)) {
                sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
            }
            totalSections++;
        }
        return totalSections;
    }


    protected void saveConcepts(JCas jcas, HashMap<String, ArrayList<Span>> concepts, String sectionName) {
        for (Map.Entry<String, ArrayList<Span>> entry : concepts.entrySet()) {
            for (Span span : entry.getValue()) {
//                System.out.println(getSpanType(span));
                String conceptTypeName = entry.getKey();
                if (logRuleInfo) {
                    String ruleInfor = getRuleInfo(span);
                    if (getSpanType(span) == Determinants.ACTUAL) {
                        saveAnnotation(jcas, ConceptTypeConstructors.get(conceptTypeName), span.begin, span.end, sectionName, ruleInfor);
                    } else if (markPseudo) {
                        savePseudoConcept(jcas, span, ruleInfor);
                    }

                } else {
                    if (getSpanType(span) == Determinants.ACTUAL) {
                        saveAnnotation(jcas, ConceptTypeConstructors.get(conceptTypeName), span.begin, span.end, sectionName);
                    } else if (markPseudo) {
                        savePseudoConcept(jcas, span);
                    }
                }
            }
        }
    }

    protected void savePseudoConcept(JCas jcas, Span span, String... rule) {
        PseudoConcept concept = new PseudoConcept(jcas, span.begin, span.end);
        concept.setCategory(getMatchedNEName(span));
        if (rule.length > 0)
            concept.setNote(rule[0]);
        concept.addToIndexes();
    }

    protected String getMatchedNEName(Span span) {
        return fastNER.getMatchedNEName(span);
    }

    protected Determinants getSpanType(Span span) {
        return fastNER.getMatchedNEType(span);
    }

    protected String getRuleInfo(Span span) {
        return span.ruleId + ":\t" + fastNER.getMatchedRuleString(span).rule;
    }

    protected void saveAnnotation(JCas jcas, Constructor<? extends Annotation> annoConstructor, int begin, int end, String sectionName, String... rule) {
        Annotation anno = null;
        try {
            anno = annoConstructor.newInstance(jcas, begin, end);
            if (anno instanceof ConceptBASE) {
                if (sectionName != null)
                    ((ConceptBASE) anno).setSection(sectionName);
                if (rule.length > 0) {
                    ((ConceptBASE) anno).setNote(rule[0]);
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        anno.addToIndexes();
    }

    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleFile, boolean caseSenstive) {
        return new FastNER(ruleFile, caseSenstive, false).getTypeDefinitions();
    }
}