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
import edu.utah.bmi.nlp.uima.ae.RuleBasedAEInf;
import edu.utah.bmi.nlp.uima.common.AnnotationComparator;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.NERSpan.byRuleLength;
import static edu.utah.bmi.nlp.core.NERSpan.scorewidth;


/**
 * This is a demo to use FastNER.java in UIMA AE. The type system is implemented through reflection.
 *
 * @author Jianlin Shi
 */
public class FastNER_AE_General extends JCasAnnotator_ImplBase implements RuleBasedAEInf {
    public static Logger logger = IOUtil.getLogger(FastNER_AE_General.class);


    //	a list of section names (can use short name if name space is "edu.utah.bmi.nlp.type.system."),
// separated by "|", ",", or ";".
    public static final String PARAM_INCLUDE_SECTIONS = "IncludeSections";

//  If IncludeSections is set, the ExcludeSections will only exclude what has been included (e.g. a subclass (subsection type)).
//  If  IncludeSections is not set, the ExcludeSections will include all SectionBody and SectionHeader except the ExcludeSections.
    public static final String PARAM_EXCLUDE_SECTIONS = "ExcludeSections";


//WARNING! include sections and/or exclude sections may miss any positive NEs when sections are not detected properly.
//Unless you are very satified with sections detection, try not to set these two. But use feature inference instead.
// So that all the NEs will be output for review.

    public static final String PARAM_ASSIGN_SECTIONS = "AssignSections";

    public static final String PARAM_RULE_STR = DeterminantValueSet.PARAM_RULE_STR;
    //    @ConfigurationParameter(name = PARAM_RULE_STR)
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

    public static final String ADV_PARAM_SPAN_COMPARE_METHOD = "SpanCompareMethod";
    public static final String ADV_PARAM_WIDTH_COMPARE_METHOD = "WidthCompareMethod";

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
    protected HashSet<Class<? extends Annotation>> includeSectionClasses = new LinkedHashSet<>();
    protected HashSet<String> excludeSections = new HashSet<>();
    protected Class<? extends Annotation> SentenceType, TokenType;
    protected Constructor<? extends Annotation> SentenceTypeConstructor;
    protected Constructor<? extends Annotation> TokenTypeConstructor;
    protected HashMap<String, Class<? extends Concept>> ConceptTypes = new HashMap<>();
    protected HashMap<String, Constructor<? extends Concept>> ConceptTypeConstructors = new HashMap<>();
    protected LinkedHashMap<String, LinkedHashMap<String, Method>> setMethods = new LinkedHashMap<>();
    protected boolean markPseudo = false, logRuleInfo = false;
    protected boolean caseSenstive = true, forceAssignSections = true, assignSection = true;
    private String spanCompareMethod = scorewidth;
    private String widthCompareMethod = byRuleLength;
    @Deprecated
    protected boolean debug = false;


    public void initialize(UimaContext cont) {
        String ruleStr, sentenceTypeName, tokenTypeName;
        ruleStr = (String) (cont
                .getConfigParameterValue(PARAM_RULE_STR));
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
        if (obj != null && ((String) obj).trim().length() > 0) {
            for (String sectionName : ((String) obj).split("[\\|,;]")) {
                sectionName = sectionName.trim();
                includeSections.add(DeterminantValueSet.checkNameSpace(sectionName));
                includeSectionClasses.add(AnnotationOper.getTypeClass(DeterminantValueSet.checkNameSpace(sectionName)));
            }
        }

        obj = cont.getConfigParameterValue(PARAM_EXCLUDE_SECTIONS);
        if (obj != null && ((String) obj).trim().length() > 0) {
            for (String sectionName : ((String) obj).split("[\\|,;]")) {
                sectionName = sectionName.trim();
                excludeSections.add(DeterminantValueSet.checkNameSpace(sectionName));
            }
        }

        obj = cont.getConfigParameterValue(PARAM_MARK_PSEUDO);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            markPseudo = true;
        obj = cont.getConfigParameterValue(PARAM_LOG_RULE_INFO);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != false)
            logRuleInfo = true;

        obj = cont.getConfigParameterValue(PARAM_CASE_SENSITIVE);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != true)
            caseSenstive = false;

        obj = cont.getConfigParameterValue(PARAM_ASSIGN_SECTIONS);
        if (obj != null && obj instanceof Boolean && (Boolean) obj != true)
            forceAssignSections = false;

        obj = cont.getConfigParameterValue(ADV_PARAM_SPAN_COMPARE_METHOD);
        if (obj != null && obj instanceof String)
            spanCompareMethod = (String) obj;

        obj = cont.getConfigParameterValue(ADV_PARAM_WIDTH_COMPARE_METHOD);
        if (obj != null && obj instanceof String)
            widthCompareMethod = (String) obj;

        if (includeSections.size() == 0 && excludeSections.size() == 0)
            assignSection = false;

        try {
            SentenceType = Class.forName(sentenceTypeName).asSubclass(Annotation.class);
            TokenType = Class.forName(tokenTypeName).asSubclass(Annotation.class);
            TokenTypeConstructor = TokenType.getConstructor(new Class[]{JCas.class, int.class, int.class});
            SentenceTypeConstructor = SentenceType.getConstructor(new Class[]{JCas.class, int.class, int.class});
            LinkedHashMap<String, TypeDefinition> conceptNames = initFastNER(cont, ruleStr);
            for (Map.Entry<String, TypeDefinition> conceptTypeDefPair : conceptNames.entrySet()) {
                String shortTypeName = conceptTypeDefPair.getKey();
                TypeDefinition typeDefinition = conceptTypeDefPair.getValue();
                String fullTypeName = typeDefinition.fullTypeName;
                Class<? extends Annotation> superClass = AnnotationOper.getTypeClass(typeDefinition.getFullSuperTypeName());
                Class conceptTypeClass = AnnotationOper.getTypeClass(fullTypeName);
                if (superClass == null) {
                    logger.warning("Super Class " + DeterminantValueSet.getRealClassTypeName(typeDefinition.getFullSuperTypeName()) + " for class: " + fullTypeName + " has not been loaded.");
                    continue;
                }
                if (conceptTypeClass == null) {
                    logger.warning("Class " + fullTypeName + " has not been loaded.");
                    continue;
                }
                ConceptTypes.put(conceptTypeDefPair.getKey(), conceptTypeClass);
                ConceptTypeConstructors.put(shortTypeName, ConceptTypes.get(shortTypeName).
                        getConstructor(new Class[]{JCas.class, int.class, int.class}));
                setMethods.put(shortTypeName, new LinkedHashMap<>());
                for (String featureName : typeDefinition.getFeatureValuePairs().keySet()) {
                    Method setFeature = AnnotationOper.getDefaultSetMethod(conceptTypeClass, featureName);
                    if (setFeature == null) {
                        logger.warning("Set feature: '" + featureName + "' has not been initiated in type: '" + fullTypeName + "'");
                    }
                    setMethods.get(shortTypeName).put(featureName, setFeature);
                }
            }
        } catch (
                ClassNotFoundException e) {
            System.err.println("You need to run this AE through AdaptableUIMACPERunner, " +
                    "which can automatically generate unknown Type classes and type descriptors.\n" +
                    "@see nlp-core project: edu.utah.bmi.uima.AdaptableUIMACPERunner");
            e.printStackTrace();
        } catch (
                NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    protected LinkedHashMap<String, TypeDefinition> initFastNER(UimaContext cont, String ruleStr) {
        fastNER = new FastNER(ruleStr, caseSenstive);
        if (markPseudo)
            fastNER.setRemovePseudo(false);
        fastNER.setCompareMethod(this.spanCompareMethod);
        fastNER.setWidthCompareMethod(this.widthCompareMethod);
        return fastNER.getTypeDefinitions();
    }


    public void process(JCas jcas) throws AnalysisEngineProcessException {
        IntervalST<String> sectionTree = new IntervalST<>();
        int totalSections = 0;
        if (assignSection || forceAssignSections)
            totalSections = indexSections(jcas, sectionTree);
        LinkedHashMap<String, ArrayList<Annotation>> sections = new LinkedHashMap<>();
        ArrayList<Annotation> tokens = new ArrayList<>();
        FSIndex annoIndex = jcas.getAnnotationIndex(SentenceType);
        Iterator annoIter = annoIndex.iterator();
        int totalSentences = 0;
//        align sentences within sections
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

            if (!sections.containsKey(sectionName))
                sections.put(sectionName, new ArrayList<>());
            sections.get(sectionName).add(sentence);

        }

        // get token annotations
        annoIndex = jcas.getAnnotationIndex(TokenType);
        annoIter = annoIndex.iterator();
        while (annoIter.hasNext()) {
            tokens.add((Annotation) annoIter.next());
        }

        if (totalSentences > 0) {
            for (String sectionName : sections.keySet()) {
                // make sure all the annotations are in ascending order regarding span offset
                Collections.sort(sections.get(sectionName), new AnnotationComparator());
                Collections.sort(tokens, new AnnotationComparator());

//     Construct annotation_id-annotation_id map, easier and faster to find related annotations.
                TreeMap<Integer, TreeSet<Integer>> sentence2TokenMap = new TreeMap<Integer, TreeSet<Integer>>();
                AnnotationOper.buildAnnoMap(sections.get(sectionName), tokens, sentence2TokenMap);
//                boolean outsiders = true;
//                if ((includeSections.size() == 0 && excludeSections.size() > 0 && !excludeSections.contains(sectionName))
//                        || (includeSections.size() > 0 && includeSections.contains(sectionName))
//                        || (includeSections.size() == 0 && excludeSections.size() == 0)) {
//                    outsiders = false;
//                }

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
//                        if (outsiders)
//                            saveOutsideScopeConcepts(jcas, concepts, sectionName);
//                        else
                            saveConcepts(jcas, concepts, sectionName);
                    }
                }
            }
        } else {

            Collection<SourceDocumentInformation> docAnnotation = JCasUtil.select(jcas, SourceDocumentInformation.class);
            if (docAnnotation != null && docAnnotation.size() > 0)
                logger.info("Document: " + docAnnotation.iterator().next().getUri() + " has not been properly sentence segmented. Use simple segmenter instead.");

            String text = jcas.getDocumentText();
            ArrayList<ArrayList<Span>> simpleSentences = SimpleParser.tokenizeDecimalSmartWSentences(text, true, caseSenstive);
            for (ArrayList<Span> sentence : simpleSentences) {
                saveConcept(jcas, "Sentence", sentence.get(0).begin, sentence.get(sentence.size() - 1).end, null);
                logger.finest("Sentence: " + sentence.get(0).begin + "-" + sentence.get(sentence.size() - 1).end);
                for (Span token : sentence) {
                    saveConcept(jcas, "Token", token.begin, token.end, null);
                }
                HashMap<String, ArrayList<Span>> concepts = fastNER.processSpanList(sentence);
//              store found concepts in annotation
                if (concepts.size() > 0) {
                    saveConcepts(jcas, concepts, null);
                }
            }
        }
    }


    protected void saveOutsideScopeConcepts(JCas jcas, HashMap<String, ArrayList<Span>> concepts, String sectionName) {
        for (ArrayList<Span> spans : concepts.values()) {
            for (Span span : spans) {
                if (logRuleInfo) {
                    String ruleInfor = getRuleInfo(span);
                    saveOutsideScopeConcept(jcas, span, sectionName, ruleInfor);
                } else {
                    saveOutsideScopeConcept(jcas, span, sectionName);
                }
            }
        }
    }

    protected int indexSections(JCas jCas, IntervalST<String> sectionTree) {
        int totalSections = 0;
        if (includeSections.size() > 0) {
            for (Class<? extends Annotation> sectionCls: includeSectionClasses) {
                for  (Annotation section: JCasUtil.select(jCas, sectionCls)) {
                    String sectionName = section.getType().getName();
                    if (excludeSections.size() ==0 || !excludeSections.contains(sectionName)) {
                        sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
                        totalSections++;
                    }
                }
            }
        } else {
//          if sections are defined as subclass of SectionBody or SectionHeader
            FSIndex annoIndex = jCas.getAnnotationIndex(SectionBody.class);
            Iterator annoIter = annoIndex.iterator();
            for  (Annotation section: JCasUtil.select(jCas, SectionBody.class)) {
                String sectionName = section.getType().getName();
                if (forceAssignSections) {
                    sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
                } else if (excludeSections.size() > 0 && !excludeSections.contains(sectionName)) {
                    sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
                }
                totalSections++;
            }
            //add section header as well.
            for  (Annotation section: JCasUtil.select(jCas,SectionHeader.class)) {
                String sectionName = section.getType().getShortName();
                if (forceAssignSections)
                    sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
                else if (excludeSections.size() > 0 && !excludeSections.contains(sectionName)) {
                    sectionTree.put(new Interval1D(section.getBegin(), section.getEnd()), sectionName);
                }
                totalSections++;
            }
        }
        return totalSections;
    }


    protected void saveConcepts(JCas jcas, HashMap<String, ArrayList<Span>> concepts, String sectionName) {
        for (Map.Entry<String, ArrayList<Span>> entry : concepts.entrySet()) {
            for (Span span : entry.getValue()) {
//                System.out.println(getSpanType(span));
                String conceptTypeName = entry.getKey();
                Rule rule = fastNER.getMatchedRuleString(span);
                if (logRuleInfo) {
                    String ruleInfor = getRuleInfo(span);
                    if (getSpanType(span) == Determinants.ACTUAL) {
                        saveConcept(jcas, conceptTypeName, span.begin, span.end, sectionName, ruleInfor, ((NERRule)rule).attributes.toArray(new String[((NERRule)rule).attributes.size()]));
                    } else if (markPseudo) {
                        savePseudoConcept(jcas, span, ruleInfor);
                    }

                } else {
                    if (getSpanType(span) == Determinants.ACTUAL) {
                        saveConcept(jcas, conceptTypeName, span.begin, span.end, sectionName, null, ((NERRule)rule).attributes.toArray(new String[((NERRule)rule).attributes.size()]));
                    } else if (markPseudo) {
                        savePseudoConcept(jcas, span);
                    }
                }
            }
        }
    }


    protected void saveOutsideScopeConcept(JCas jcas, Span span, String... comments) {
        OutsideScopeConcept concept = new OutsideScopeConcept(jcas, span.begin, span.end);
        concept.setCategory(getMatchedNEName(span));
        if (comments.length > 0)
            concept.setSection(comments[0]);
        if (comments.length > 1)
            concept.setNote(comments[1]);
        concept.addToIndexes();
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


    protected void saveConcept(JCas jcas, String conceptTypeName, int begin, int end, String sectionName, String note, String... featureValues) {
        Class<? extends Annotation> conceptCls=AnnotationOper.getTypeClass(conceptTypeName);
        Annotation anno= AnnotationFactory.createAnnotation(jcas, begin, end, conceptCls);
        if (anno instanceof ConceptBASE) {
            if (sectionName != null)
                ((ConceptBASE) anno).setSection(sectionName);
            if (note != null) {
                ((ConceptBASE) anno).setNote(note);
            }
            if (featureValues.length > 0 && setMethods.containsKey(conceptTypeName)) {
                int i = 0;
                for (String featureName : setMethods.get(conceptTypeName).keySet()) {
                    if (i >= featureValues.length)
                        break;
                    FSUtil.setFeature(anno, featureName, featureValues[i]);
                    i++;
                }
            }
        }
        anno.addToIndexes();

    }

    /**
     * @deprecated the constructor will throw errors while migrating to jdk 11 when using customized class loader, use
     * {@link #saveConcept(JCas, String, int, int, String, String, String...) saveConcept} method instead.
     *
     * @param jcas JCas object
     * @param conceptTypeName concept type to be saved
     * @param annoConstructor the annotation constructor
     * @param begin span begin
     * @param end span end
     * @param sectionName the section that the annotation belongs to
     * @param note the comments to be added
     * @param featureValues feature value pairs to be added to the annotation
     */
    @Deprecated
    protected void saveConcept(JCas jcas, String conceptTypeName, Constructor<? extends Annotation> annoConstructor, int begin, int end, String sectionName, String note, String... featureValues) {
        Annotation anno = null;
        try {
            anno = annoConstructor.newInstance(jcas, begin, end);
            if (anno instanceof ConceptBASE) {
                if (sectionName != null)
                    ((ConceptBASE) anno).setSection(sectionName);
                if (note != null) {
                    ((ConceptBASE) anno).setNote(note);
                }
                if (featureValues.length > 0 && setMethods.containsKey(conceptTypeName)) {
                    int i = 0;
                    for (String featureName : setMethods.get(conceptTypeName).keySet()) {
                        if (i >= featureValues.length)
                            break;
                        setMethods.get(conceptTypeName).get(featureName).invoke(anno, featureValues[i]);
                        i++;
                    }
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

    protected void saveConcept(JCas jcas, String conceptTypeName, int begin, int end, String sectionName) {
        saveConcept(jcas, conceptTypeName, begin, end, sectionName, null);
    }

    @Deprecated
    protected void saveConcept(JCas jcas, String conceptTypeName, Constructor<? extends Annotation> annoConstructor, int begin, int end, String sectionName) {
        saveConcept(jcas, conceptTypeName, begin, end, sectionName, null);
    }

    public static LinkedHashMap<String, TypeDefinition> getTypeDefinitions(String ruleFile, boolean caseSenstive) {
        return new FastNER(ruleFile, caseSenstive, false).getTypeDefinitions();
    }

    @Override
    public LinkedHashMap<String, TypeDefinition> getTypeDefs(String ruleStr) {
        return new FastNER(ruleStr, false, false).getTypeDefinitions();
    }
}