package edu.utah.bmi.nlp.fastner;

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.IOUtil;
import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPEDescriptorRunner;
import edu.utah.bmi.nlp.uima.common.AnnotationOper;
import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class FastRuleFactoryTest {

    @Test
    public void createFastRule() {
        String ruleStr = "@fastner\n" +
                "@CONCEPT_FEATURES\tNewConcept\t\tF1\tF2\n" +
                "test string\tNewConcept\t\tVa\tVb";
        LinkedHashMap<String, TypeDefinition> typeDefinition = new LinkedHashMap<>();
        Object[] res = FastRuleFactory.buildRuleStore(ruleStr, typeDefinition, true, true);
        Object ruleStore = res[0];
        assert (ruleStore instanceof HashMap);
        assert (((HashMap) ruleStore).get(3) instanceof NERRule);
        assert (((NERRule) ((HashMap) ruleStore).get(3)).attributes.size() == 2);
        assert (((NERRule) ((HashMap) ruleStore).get(3)).attributes.get(0).equals("Va"));
        assert (((NERRule) ((HashMap) ruleStore).get(3)).attributes.get(1).equals("Vb"));
    }

//    @Test
//    public void testComplexTypeSystem() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
//        String ruleStr = "@fastner\n" +
//                "@CONCEPT_FEATURES\tNewConcept\torg.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation\tPreferredText\tCui\tontologyConceptArr\n" +
//                "test string\tNewConcept\t\tVa\tVb";
//        LinkedHashMap<String, TypeDefinition> typeDefinition = new LinkedHashMap<>();
//        Object[] res = FastRuleFactory.buildRuleStore(ruleStr, typeDefinition, true, true);
////        Object ruleStore = res[0];
//
//
//        AdaptableCPEDescriptorRunner runner = AdaptableCPEDescriptorRunner.getInstance(
//                "src/test/resources/desc/testCPE.xml", "test");
//        runner.addConceptType(typeDefinition.get("NewConcept"));
//        runner.reInitTypeSystem("target/generated-test-sources/test_types.xml");
//        runner.attachTypeDescriptorToReader();
//        Class newClass = Class.forName(DeterminantValueSet.checkNameSpace("NewConcept"));
//        Method m = AnnotationOper.getDefaultGetMethod(newClass, "ontologyConceptArr");
//        JCas jcas = runner.initJCas();
//        jcas.setDocumentText("this is a test text.");
//        Constructor<? extends IdentifiedAnnotation> con = newClass.getConstructor(JCas.class, int.class, int.class);
//        IdentifiedAnnotation anno = con.newInstance(jcas, 1, 5);
//        Object value = m.invoke(anno);
////        System.out.println(value);
//        UmlsConcept concept = new UmlsConcept(jcas);
//        concept.setCui("C289723");
//        FSArray conceptArr = new FSArray(jcas, 1);
//        conceptArr.set(0, concept);
//        anno.setOntologyConceptArr(conceptArr);
//        value = m.invoke(anno);
//        System.out.println(((FSArray) value).get(0));
//        assert (((FSArray) value).get(0) instanceof UmlsConcept);
//        assert (((UmlsConcept) ((FSArray) value).get(0)).getCui().equals("C289723"));
//    }
}