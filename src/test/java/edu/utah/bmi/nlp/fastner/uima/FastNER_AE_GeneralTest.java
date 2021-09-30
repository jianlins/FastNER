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

import edu.utah.bmi.nlp.compiler.MemoryClassLoader;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.type.system.*;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.ae.AnnotationCountEvaluator;
import edu.utah.bmi.nlp.uima.ae.AnnotationPrinter;
import edu.utah.bmi.nlp.uima.ae.SimpleParser_AE;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class FastNER_AE_GeneralTest {
    private AnalysisEngine fastNER_AE, simpleParser_AE, annotationPrinter;
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private Object[] configurationData;

    @BeforeEach
    public void setUp() {
        String typeDescriptor = "desc/type/All_Types";
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        Collection<TypeDefinition> cons = FastNER_AE_General.getTypeDefinitions("conf/rules.xlsx", true).values();
        runner.addConceptTypes(cons);
        cons = FastNER_AE_General.getTypeDefinitions("conf/rules_g.tsv", true).values();
        runner.addConceptTypes(cons);
        runner.addConceptType(new TypeDefinition("Impression", "SectionBody"));
        runner.addConceptType(new TypeDefinition("Plan", "SectionBody"));
        runner.reInitTypeSystem("target/generated-test-sources/customized");
        jCas = runner.initJCas();
//      Set up the parameters
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "conf/rules.xlsx",
                FastNER_AE_General.PARAM_SENTENCE_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Sentence",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        try {
            fastNER_AE = createEngine(FastNER_AE_General.class,
                    configurationData);
            simpleParser_AE = createEngine(SimpleParser_AE.class, new Object[]{});
            annotationPrinter = createEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "ConceptBASE"});

        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test1() throws ResourceInitializationException,
            AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {
        String text = "The patient denies any problem with visual changes or hearing changes.";
        jCas.setDocumentText(text);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);


        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        assertTrue(concepts.size() == 1,"Didn't get the right number of concepts");
        assertTrue( concepts.get(0).getCoveredText().equals("patient denies"),"Didn't get the right concept: 'patient denies'");


        annoIndex = jCas.getAnnotationIndex(Token.type);
        annoIter = annoIndex.iterator();
        ArrayList<Token> tokens = new ArrayList<Token>();
        while (annoIter.hasNext()) {
            tokens.add((Token) annoIter.next());
//            System.out.println(tokens.get(tokens.size()-1).getCoveredText());
        }
        assertTrue(tokens.size() == 11,"Didn't get the right number of concepts");

//        ##print the assertions below
//        for (int i=0;i<tokens.size();i++){
//            Token token=tokens.get(i);
//            int begin=token.getBegin();
//            int end=token.getEnd();
//            System.out.println("assertTrue(\"Didn't get the right token: \'"+token.getCoveredText()+"\'\", tokens.get("+i+").getBegin()=="+begin+" && tokens.get("+i+").getEnd()=="+end+" && text.substring("+begin+","+end+").equals(tokens.get("+i+").getCoveredText()));");
//        }
        assertTrue(tokens.get(0).getBegin() == 0 && tokens.get(0).getEnd() == 3 && text.substring(0, 3).equals(tokens.get(0).getCoveredText()),"Didn't get the right token: 'The'");
        assertTrue(tokens.get(1).getBegin() == 4 && tokens.get(1).getEnd() == 11 && text.substring(4, 11).equals(tokens.get(1).getCoveredText()),"Didn't get the right token: 'patient'");
        assertTrue(tokens.get(2).getBegin() == 12 && tokens.get(2).getEnd() == 18 && text.substring(12, 18).equals(tokens.get(2).getCoveredText()),"Didn't get the right token: 'denies'");
        assertTrue(tokens.get(3).getBegin() == 19 && tokens.get(3).getEnd() == 22 && text.substring(19, 22).equals(tokens.get(3).getCoveredText()),"Didn't get the right token: 'any'");
        assertTrue(tokens.get(4).getBegin() == 23 && tokens.get(4).getEnd() == 30 && text.substring(23, 30).equals(tokens.get(4).getCoveredText()),"Didn't get the right token: 'problem'");
        assertTrue(tokens.get(5).getBegin() == 31 && tokens.get(5).getEnd() == 35 && text.substring(31, 35).equals(tokens.get(5).getCoveredText()),"Didn't get the right token: 'with'");
        assertTrue(tokens.get(6).getBegin() == 36 && tokens.get(6).getEnd() == 42 && text.substring(36, 42).equals(tokens.get(6).getCoveredText()),"Didn't get the right token: 'visual'");
        assertTrue(tokens.get(7).getBegin() == 43 && tokens.get(7).getEnd() == 50 && text.substring(43, 50).equals(tokens.get(7).getCoveredText()),"Didn't get the right token: 'changes'");
        assertTrue(tokens.get(8).getBegin() == 51 && tokens.get(8).getEnd() == 53 && text.substring(51, 53).equals(tokens.get(8).getCoveredText()),"Didn't get the right token: 'or'");
        assertTrue(tokens.get(9).getBegin() == 54 && tokens.get(9).getEnd() == 61 && text.substring(54, 61).equals(tokens.get(9).getCoveredText()),"Didn't get the right token: 'hearing'");
        assertTrue(tokens.get(10).getBegin() == 62 && tokens.get(10).getEnd() == 69 && text.substring(62, 69).equals(tokens.get(10).getCoveredText()),"Didn't get the right token: 'changes'");
    }


    @Test
    public void test2() throws ResourceInitializationException,
            AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {

        String text = "a fever of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .";
        jCas.setDocumentText(text);
        configurationData[1] = "conf/rules_g.tsv";
        fastNER_AE = createEngine(FastNER_AE_General.class, configurationData);

        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);

        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        for (Concept concept : concepts) {
            System.out.println(concept.getBegin() + "-" + concept.getEnd() + "\t" + concept.getType().getShortName() + ": >" + concept.getCoveredText() + "<");
        }
    }

    //	test section scope
    @Test
    public void test3() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "HISTORY: a fever of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .\nIMPRESSION: no fever currently.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, text.indexOf("a fever of 103.8"), text.indexOf("IMPRESSION") - 1);
        sectionBody.addToIndexes();
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "@fastner\nfever\t0\tEntity\tACTUAL",
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "SectionBody",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        for (Concept concept : concepts) {
            System.out.println(concept.getType().getShortName() + concept.getBegin() + "-" + concept.getEnd() + "\t" + concept.getSection() + ": >" + concept.getCoveredText() + "<");
        }
    }

    //	test section scope
    @Test
    public void test4() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "HISTORY: a  of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .\nIMPRESSION: no fever currently.";
        jCas.reset();
        jCas.setDocumentText(text);
        Class cls = AnnotationOper.getTypeClass("Impression").asSubclass(SectionBody.class);
        Annotation impression = AnnotationFactory.createAnnotation(jCas, text.indexOf("IMPRESSION") + 12, text.length(), cls);
        impression.addToIndexes();

        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "@fastner\ntachycardia\t0\tEntity\tACTUAL",
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "SectionBody",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        Concept concept = null;
        if (annoIter.hasNext()) {
            concept = (Concept) annoIter.next();
        }
//        System.out.println(concept);
        assert (concept == null);
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "@fastner\ntachycardia\t0\tEntity\tACTUAL",
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "Impression",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        annoIndex = jCas.getAnnotationIndex(Concept.type);
        annoIter = annoIndex.iterator();
        concept = null;
        if (annoIter.hasNext()) {
            concept = (Concept) annoIter.next();
        }
//        System.out.println(concept);
        assert (concept == null);
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "@fastner\nfever\t0\tEntity\tACTUAL",
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "SectionBody",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        annoIndex = jCas.getAnnotationIndex(Concept.type);
        annoIter = annoIndex.iterator();
        concept = null;
        if (annoIter.hasNext()) {
            concept = (Concept) annoIter.next();
        }
//        System.out.println(concept);
        assert (concept != null);

    }


    //	test section scope
    @Test
    public void test5() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "HISTORY: a  fever of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .\nIMPRESSION: no fever currently.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, 9, text.indexOf("IMPRESSION") - 1);
        sectionBody.addToIndexes();
        Class cls = AnnotationOper.getTypeClass("Impression").asSubclass(SectionBody.class);
        Annotation impression = AnnotationFactory.createAnnotation(jCas, text.indexOf("IMPRESSION") + 12, text.length(), cls);
        impression.addToIndexes();

        annotationPrinter = createEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "SectionBody"});
        annotationPrinter.process(jCas);

        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, "@fastner\nfever\t0\tEntity\tACTUAL",
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "Impression",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        assert (concepts.size() == 1);
        for (Concept concept : concepts) {
            System.out.println(concept.getType().getShortName() + concept.getBegin() + "-" + concept.getEnd() + "\t" + concept.getSection() + ": >" + concept.getCoveredText() + "<");
            System.out.println(concept);
            assert (concept.getBegin() == 114);
        }
    }

    @Test
    public void testPseudo() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, text.indexOf("Positive"), text.indexOf(" No ") - 1);
        sectionBody.addToIndexes();
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n" +
                "emboli protocol	0	Concept	PSEUDO";
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, rule,
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "SectionBody",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true,
                FastNER_AE_General.PARAM_ENABLE_DEBUG, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        AnalysisEngine annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{
                AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 0});
        annotationEval1.process(jCas);
        assert (AnnotationCountEvaluator.pass);
//        annotationPrinter.process(jCas);
        annotationPrinter = createEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "PseudoConcept"});
        annotationPrinter.process(jCas);
        annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{
                AnnotationCountEvaluator.PARAM_TYPE_NAME, "PseudoConcept",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1});
        annotationEval1.process(jCas);
        assert (AnnotationCountEvaluator.pass);
    }

    @Test
    public void testExclusion() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol.\n\n\n Plan: No further emboli treatment needed.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, text.indexOf("Positive"), text.indexOf(" Plan") - 4);
        createAnnotation("Plan", SectionBody.class, text.indexOf("Plan"), text.length());
        sectionBody.addToIndexes();
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n";
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, rule,
                FastNER_AE_General.PARAM_EXCLUDE_SECTIONS, "Plan",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true,
                FastNER_AE_General.PARAM_ASSIGN_SECTIONS, false};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        AnalysisEngine annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{
                AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1});
        annotationEval1.process(jCas);
//        assert(AnnotationCountEvaluator.pass);
//        annotationPrinter.process(jCas);
        annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{
                AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptBASE",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1});
        annotationEval1.process(jCas);
        assert (AnnotationCountEvaluator.pass);

    }


    @Test
    public void testForceAssign() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "pulmonary emboli protocol. \n\n Plan: No further emboli treatment needed.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, text.indexOf("Positive"), text.indexOf(" Plan") - 1);
        createAnnotation("Plan", SectionBody.class, text.indexOf("Plan"), text.length());
        sectionBody.addToIndexes();
        String rule = "@fastner\n" +
                "emboli	0	Concept	ACTUAL\n";
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, rule,
                FastNER_AE_General.PARAM_EXCLUDE_SECTIONS, "Plan",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true,
                FastNER_AE_General.PARAM_ASSIGN_SECTIONS, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        AnalysisEngine annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{AnnotationCountEvaluator.PARAM_TYPE_NAME, "Concept",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1});
        annotationEval1.process(jCas);
//        annotationPrinter.process(jCas);
        AnalysisEngine annotationEval2 = createEngine(AnnotationCountEvaluator.class, new Object[]{AnnotationCountEvaluator.PARAM_TYPE_NAME, "OutsideScopeConcept",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1});
        annotationEval2.process(jCas);
    }


    @Test
    public void testGroup() throws ResourceInitializationException, AnalysisEngineProcessException {
        String text = "Exam was done yesterday. Positive for pulmonary emboli protocol. No further treatment needed.";
        jCas.reset();
        jCas.setDocumentText(text);
        SectionBody sectionBody = new SectionBody(jCas, text.indexOf("Positive"), text.indexOf(" No ") - 1);
        sectionBody.addToIndexes();
        String rule = "@fastner\n" +
                "emboli \\( protocol \\)	0	Concept	ACTUAL";
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, rule,
                FastNER_AE_General.PARAM_INCLUDE_SECTIONS, "SectionBody",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);

        annotationPrinter = createEngine(AnnotationPrinter.class, new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, "ConceptBASE"});
        annotationPrinter.process(jCas);
        AnalysisEngine annotationEval1 = createEngine(AnnotationCountEvaluator.class, new Object[]{AnnotationCountEvaluator.PARAM_TYPE_NAME, "ConceptBASE",
                AnnotationCountEvaluator.PARAM_TYPE_COUNT, 1, AnnotationCountEvaluator.PARAM_FEATURE_VALUES, "Text,protocol"});
        annotationEval1.process(jCas);
        assert (AnnotationCountEvaluator.pass);
    }

    @Test
    public void testTypeGen() {
        String rule = "@fastner\nfever\t0\tEntity\tACTUAL";
        LinkedHashMap<String, TypeDefinition> typeDefinition = FastNER_AE_General.getTypeDefinitions(rule, false);
        for (String type : typeDefinition.keySet()) {
            System.out.println(type + "\t" + typeDefinition.get(type));
        }
    }


    public void createAnnotation(String typeName, Class superClass, int begin, int end) {
        Class cls = AnnotationOper.getTypeClass(typeName).asSubclass(superClass);
        Annotation annotation = AnnotationFactory.createAnnotation(jCas, begin,end, cls);
        annotation.addToIndexes();
    }

    @Test
    public void testAdditionalFeatures() throws ResourceInitializationException, AnalysisEngineProcessException, ClassNotFoundException {
        String text = "HISTORY: a  fever of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .\nIMPRESSION: no fever currently.";
        String ruleStr = "@fastner\n" +
                "@CONCEPT_FEATURES\tNewUmlsConcept\t\tCui\tPreferredText\n" +
                "fever\t0\tNewUmlsConcept\t\tC302837\thyperthermia";
        String typeDescriptor = "desc/type/All_Types";
        runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
        runner.addConceptTypes(new FastNER_AE_General().getTypeDefs(ruleStr).values());
        runner.reInitTypeSystem("target/generated-test-sources/test_type.xml");
        jCas = runner.initJCas();
        jCas.setDocumentText(text);

        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_STR, ruleStr,
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        fastNER_AE = createEngine(FastNER_AE_General.class,
                configurationData);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        concepts.addAll(JCasUtil.select(jCas, Concept.class));
        for (Concept concept : concepts) {
            System.out.println(concept);
        }
        assertTrue(concepts.size() == 2);
        System.out.println(concepts.get(0).getClass());
        assertTrue(concepts.get(0).getType().getShortName().equals("NewUmlsConcept"));
        assertTrue(concepts.get(0).toString().contains("C302837"));
        assertTrue(concepts.get(0).toString().contains("hyperthermia"));
    }
}