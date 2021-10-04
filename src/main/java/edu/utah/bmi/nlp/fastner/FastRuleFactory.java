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

import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastcner.*;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.getShortName;

/**
 * @author Jianlin Shi
 * Created on 3/5/17.
 */
public class FastRuleFactory {
    public static Logger logger = edu.utah.bmi.nlp.core.IOUtil.getLogger(FastRuleFactory.class);

    public FastRuleFactory() {

    }

    @Deprecated
    public static FastRule createFastRule(Class fastNER, String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive) {
        return createFastRule(fastNER, ruleStr, typeDefinition, splitter, caseSensitive);
    }

    public static Object[] buildRuleStore(String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition,
                                          boolean caseSensitive, boolean constructRuleMap) {
        Object[] output = new Object[3];
        HashMap<Integer, Rule> rules = new HashMap<>();
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        File agnosticFile = new File(ruleStr);
        String ruleType;
        String concatenated;
        ArrayList<ArrayList<String>> allCells = new ArrayList<>();
        if (testFileStr.equals(".owl")) {
            concatenated = OWLUtil.readOwlFile(ruleStr, allCells, caseSensitive);
            ruleType = getRuleType(concatenated);
        } else if (agnosticFile.exists() && agnosticFile.isDirectory()) {
            concatenated = OWLUtil.readOwlDirectory(ruleStr, allCells, caseSensitive);
            ruleType = getRuleType(concatenated);
        } else {
            edu.utah.bmi.nlp.core.IOUtil ioUtil = new edu.utah.bmi.nlp.core.IOUtil(ruleStr);
            for (ArrayList<String> cells : ioUtil.getInitiations()) {
                if (cells.get(1).startsWith(DeterminantValueSet.CONCEPT_FEATURES1) || cells.get(1).startsWith(DeterminantValueSet.CONCEPT_FEATURES2)) {
                    String conceptName = cells.get(2).trim();
                    String conceptShortName = getShortName(conceptName);
                    if (typeDefinition != null && !typeDefinition.containsKey(conceptShortName)) {
                        typeDefinition.put(conceptShortName, new TypeDefinition(cells.subList(2, cells.size())));
                    }
                } else if (Character.isUpperCase(cells.get(1).charAt(1))) {
//                  back compatibility
                    String conceptName = cells.get(1).substring(1);
                    String conceptShortName = getShortName(conceptName);
                    String superTypeName = cells.get(2);
                    if (typeDefinition != null && !typeDefinition.containsKey(conceptShortName)) {
                        if (cells.size() > 3)
                            typeDefinition.put(conceptShortName, new TypeDefinition(conceptName, cells.get(2), cells.subList(3, cells.size())));
                        else
                            typeDefinition.put(conceptShortName, new TypeDefinition(conceptName, cells.get(2), new ArrayList<>()));
                    }

                } else if (cells.size() > 3) {
                    System.err.println("Unrecognized rule initialization: " + cells);
                }
            }
            allCells = ioUtil.getRuleCells();
            ruleType = getRuleType(ioUtil);
            concatenated = ioUtil.getConcatenatedRuleStr();
        }

        for (ArrayList<String> cells : allCells) {
            ArrayList<String> featureValues = new ArrayList<>();
            String[] featureValuesArray;
            logger.finest("Add rule: " + cells);
            int id = Integer.parseInt(cells.get(0));
            String rule = cells.get(1);
            String conceptName;
            double score = 0;
            DeterminantValueSet.Determinants determinant = DeterminantValueSet.Determinants.ACTUAL;
            boolean scoreSet = false;
            if (cells.size() < 3) {
                logger.info("Rule format error: " + cells + ". Will skip it.");
                continue;
            } else if (cells.size() < 4) {
//                1 rule_string ConceptType
                conceptName = cells.get(2).trim();
                determinant = DeterminantValueSet.Determinants.ACTUAL;
            } else {
                int featureCellBegin = 5;
                if (UnicodeChecker.isNumber(cells.get(2))) {
                    conceptName = cells.get(3).trim();
                    score = Double.parseDouble(cells.get(2));
                    scoreSet = true;
                    if (cells.size() > 4 && cells.get(4).trim().length() > 0)
                        determinant = DeterminantValueSet.Determinants.valueOf(cells.get(4));
                } else {
                    conceptName = cells.get(2).trim();
                    if (cells.size() > 3 && cells.get(3).trim().length() > 0)
                        determinant = DeterminantValueSet.Determinants.valueOf(cells.get(3));
                    featureCellBegin = 4;
                }
                if (cells.size() > featureCellBegin) {
                    for (String featureName : typeDefinition.get(conceptName).getFeatureValuePairs().keySet()) {
                        featureValues.add(cells.get(featureCellBegin));
                        featureCellBegin++;
                        if (featureCellBegin >= cells.size())
                            break;
                    }
                }
            }
            String conceptShortName = getShortName(conceptName);
            if (typeDefinition != null && !typeDefinition.containsKey(conceptName)) {
                typeDefinition.put(conceptShortName, new TypeDefinition(conceptName, DeterminantValueSet.defaultSuperTypeName, new ArrayList<>()));
            }
            if (!scoreSet && determinant == DeterminantValueSet.Determinants.PSEUDO)
                score = 1d;
            if (constructRuleMap) {
                if (featureValues.size() > 0) {
                    featureValuesArray = new String[featureValues.size()];
                    featureValuesArray = featureValues.toArray(featureValuesArray);
                    rules.put(id, new NERRule(id, caseSensitive ? rule : rule.toLowerCase(), conceptName, score, determinant, featureValuesArray));
                } else
                    rules.put(id, new NERRule(id, caseSensitive ? rule : rule.toLowerCase(), conceptName, score, determinant));
            }
        }
        output[0] = rules;
        output[1] = ruleType;
        output[2] = concatenated;
        return output;
    }

    public static FastRule createFastRule(String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition,
                                          boolean caseSensitive, boolean constructRuleMap) {
        FastRule fastRule = null;
        Object[] output = buildRuleStore(ruleStr, typeDefinition, caseSensitive, constructRuleMap);
        String ruleType = (String) output[1];
        String concatenated = (String) output[2];
        boolean supportReplication = concatenated.indexOf("+") != -1 ? true : false;
        if (constructRuleMap) {
            HashMap<Integer, Rule> rules = (HashMap<Integer, Rule>) output[0];
            switch (ruleType) {
                case "FastCRuleCN":
                    fastRule = new FastCRuleCN(rules);
                    ((FastCRule) fastRule).setReplicationSupport(supportReplication);
                    break;
                case "FastCRuleSB":
                    fastRule = new FastCRuleSB(rules);
                    ((FastCRule) fastRule).setReplicationSupport(supportReplication);
                    break;
                case "FastCRule":
                    fastRule = new FastCRule(rules);
                    ((FastCRule) fastRule).setReplicationSupport(supportReplication);
                    break;
                case "FastRuleWGN":
                    fastRule = new FastRuleWGN(rules);
                    break;
                case "FastRuleWG":
                    fastRule = new FastRuleWG(rules);
                    break;
                default:
                    fastRule = new FastRuleWOG(rules);
                    break;
            }
        }
        return fastRule;
    }

    @Deprecated
    public static FastRule createFastRule(Class fastNER, String ruleStr, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive, boolean constructRuleMap) {
        FastRule fastRule = null;
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        HashMap<Integer, Rule> rules = new HashMap<>();
        boolean[] thisRuleType = new boolean[]{false, false, false, false, false, false};
        if (testFileStr.equals(".tsv") || testFileStr.equals(".csv") || testFileStr.equals("xlsx") || testFileStr.equals(".owl")) {
            thisRuleType = IOUtil.readAgnosticFile(ruleStr, rules, typeDefinition, caseSensitive, thisRuleType);
        } else {
            thisRuleType = IOUtil.readCSVString(ruleStr, rules, typeDefinition, splitter, caseSensitive, thisRuleType);
        }
        if (constructRuleMap) {
            if (thisRuleType[0] || fastNER == FastCNER.class) {
//              Support Chinese
                if (thisRuleType[5]) {
                    fastRule = new FastCRuleCN(rules);
//            support square bracket
                } else if (thisRuleType[2])
                    fastRule = new FastCRuleSB(rules);
                else
                    fastRule = new FastCRule(rules);
                if (thisRuleType[3])
                    ((FastCRule) fastRule).setReplicationSupport(true);
            } else {
                if (thisRuleType[4]) {
                    fastRule = new FastRuleWGN(rules);
//            support group
                } else if (thisRuleType[1]) {
                    fastRule = new FastRuleWG(rules);
                } else {
                    fastRule = new FastRuleWOG(rules);
                }
            }
        }
        return fastRule;
    }

    private static String getRuleType(String concatenated) {
        String ruleType = "";
        for (char ch : concatenated.toCharArray()) {
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                ruleType = "FastCRuleCN";
                break;
            }
        }
        if (ruleType.length() == 0) {
            if (concatenated.indexOf("[") != -1 && concatenated.indexOf("]") != -1) {
                ruleType = "FastCRuleSB";
            } else if (concatenated.indexOf("\\a") != -1 || concatenated.indexOf("\\d") != -1 || concatenated.indexOf("\\s") != -1) {
                ruleType = "FastCRule";
            } else if (concatenated.indexOf("\\>") != -1 || concatenated.indexOf("\\<") != -1) {
                ruleType = "FastRuleWGN";
            } else if (concatenated.indexOf("(") != -1 || concatenated.indexOf(")") != -1) {
                ruleType = "FastRuleWG";
            } else {
                ruleType = "FastRuleWOG";
            }
        }
        return ruleType;
    }

    private static String getRuleType(edu.utah.bmi.nlp.core.IOUtil ioUtil) {
        String ruleType = "";
        String concatenated = ioUtil.getConcatenatedRuleStr();
        if (ioUtil.settings.containsKey("fastcnercn")) {
            ruleType = "FastCRuleCN";
        } else if (ioUtil.settings.containsKey("fastcner")) {
            if (concatenated.indexOf("[") != -1 && concatenated.indexOf("]") != -1) {
                ruleType = "FastCRuleSB";
            } else {
                ruleType = "FastCRule";
            }
        } else if (ioUtil.settings.containsKey("fastner")) {
            if (concatenated.indexOf("\\>") != -1 || concatenated.indexOf("\\<") != -1) {
                ruleType = "FastRuleWGN";
            } else if (concatenated.indexOf("(") != -1 || concatenated.indexOf(")") != -1) {
                ruleType = "FastRuleWG";
            } else {
                ruleType = "FastRuleWOG";
            }
        } else {
            ruleType = getRuleType(concatenated);
        }
        return ruleType;
    }
}
