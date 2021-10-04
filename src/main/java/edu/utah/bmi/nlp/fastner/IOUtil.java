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

import edu.utah.blulab.domainontology.Anchor;
import edu.utah.blulab.domainontology.DomainOntology;
import edu.utah.blulab.domainontology.LogicExpression;
import edu.utah.blulab.domainontology.Variable;
import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import edu.utah.bmi.nlp.core.NERRule;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.TypeDefinition;
import edu.utah.bmi.nlp.fastcner.UnicodeChecker;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static edu.utah.bmi.nlp.core.DeterminantValueSet.checkNameSpace;
import static edu.utah.bmi.nlp.fastcner.UnicodeChecker.isChinese;

/**
 * @author Jianlin Shi on 4/20/16.
 */
@Deprecated
public class IOUtil {
    public static Logger logger = edu.utah.bmi.nlp.core.IOUtil.getLogger(IOUtil.class);

    @Deprecated
    public static HashMap<Integer, Rule> parseRuleStr(String ruleStr, String splitter, boolean caseSensitive) {
        HashMap<Integer, Rule> rules = new HashMap<>();
        int strLength = ruleStr.trim().length();
        String testFileStr = ruleStr.trim().substring(strLength - 4).toLowerCase();
        boolean[] thisRuleType = new boolean[]{false, false, false};
        LinkedHashMap<String, TypeDefinition> typeDefinition = new LinkedHashMap<>();
        if (testFileStr.equals(".tsv") || testFileStr.equals(".csv") || testFileStr.equals("xlsx") || testFileStr.equals(".owl")) {
            thisRuleType = IOUtil.readAgnosticFile(ruleStr, rules, typeDefinition, caseSensitive);
        } else {
            thisRuleType = IOUtil.readCSVString(ruleStr, rules, typeDefinition, splitter, caseSensitive, thisRuleType);
        }
        return rules;
    }

    public static boolean[] readOwlFile(String owlFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] ruleSupports) {
        int ruleType = 0;
        int id = 0;
        try {
            DomainOntology domain = new DomainOntology(owlFileName, true);
            ArrayList<Variable> domainVariables = domain.getAllEvents();
            for (Variable var : domainVariables) {
                ArrayList<LogicExpression<Anchor>> logicExpressions = var.getAnchor();
                for (LogicExpression<Anchor> logicExpression : logicExpressions) {
                    if (logicExpression.isSingleExpression()) {
                        for (Anchor term : logicExpression) {
                            String preferredTerm = term.getPrefTerm();
                            if (preferredTerm == null || preferredTerm.trim().length() == 0) {
                                System.err.println("Error in owl file at: " + logicExpression.toString());
                                continue;
                            }
//                            TODO enable annotating at variable name level and/or semantic type level
                            String nameEntityClass = term.getSemanticType().get(0);
                            nameEntityClass = nameEntityClass.replaceAll(" +", "_").toUpperCase();
                            ruleSupports = addRule(rules, typeDefinition, new NERRule(++id, caseSensitive ? preferredTerm : preferredTerm.toLowerCase(), nameEntityClass, 0, Determinants.ACTUAL), ruleSupports);

                            if (term.getSynonym().size() > 0) {
                                for (String s : term.getSynonym()) {
                                    ruleSupports = addRule(rules, typeDefinition, new NERRule(++id, caseSensitive ? s : s.toLowerCase(), nameEntityClass, 0, Determinants.ACTUAL), ruleSupports);
                                }
                            }
                            if (term.getAbbreviation().size() > 0) {
                                for (String s : term.getAbbreviation())
                                    ruleSupports = addRule(rules, typeDefinition, new NERRule(++id, caseSensitive ? s : s.toLowerCase(), nameEntityClass, 0, Determinants.ACTUAL), ruleSupports);
                            }
                            if (term.getMisspelling().size() > 0) {
                                for (String s : term.getMisspelling())
                                    ruleSupports = addRule(rules, typeDefinition, new NERRule(++id, caseSensitive ? s : s.toLowerCase(), nameEntityClass, 0, Determinants.ACTUAL), ruleSupports);
                            }
                            if (term.getPseudos().size() > 0) {
                                for (String s : term.getMisspelling())
                                    ruleSupports = addRule(rules, typeDefinition, new NERRule(++id, caseSensitive ? s : s.toLowerCase(), nameEntityClass, 0, Determinants.PSEUDO), ruleSupports);
                            }
                        }
                    } else {
                        logger.info("Current FastRule does not support complex NER:\n\t" + logicExpression);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }


    public static boolean[] readOwlDirectory(String owlFileDirectory, HashMap<Integer, Rule> rules, boolean caseSensitive) {
        Collection<File> files = FileUtils.listFiles(new File(owlFileDirectory), new String[]{"owl"}, true);
        LinkedHashMap<String, TypeDefinition> typeDefinition = new LinkedHashMap<>();
        boolean[] thisRuleType = new boolean[]{false, false, false};
        for (File file : files) {
            thisRuleType = readOwlFile(file.getAbsolutePath(), rules, typeDefinition, caseSensitive, thisRuleType);
        }
        return thisRuleType;
    }

    public static boolean[] readAgnosticFile(String agnosticFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive) {
        boolean[] thisRuleType = new boolean[]{false, false, false, false, false, false};
        readAgnosticFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
        return thisRuleType;
    }

    public static boolean[] readAgnosticFile(String agnosticFileName, HashMap<Integer, Rule> rules,
                                             LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive,
                                             boolean[] thisRuleType) {
        File agnosticFile = new File(agnosticFileName);

        if (agnosticFile.exists()) {
            if (agnosticFile.isDirectory()) {
                thisRuleType = readOwlDirectory(agnosticFileName, rules, caseSensitive);
            } else if (FilenameUtils.getExtension(agnosticFileName).equals("owl")) {
                thisRuleType = readOwlFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
            } else if (FilenameUtils.getExtension(agnosticFileName).equals("xlsx")) {
                thisRuleType = readXLSXRuleFile(agnosticFileName, rules, typeDefinition, caseSensitive, thisRuleType);
            } else if (FilenameUtils.getExtension(agnosticFileName).equals("csv")) {
                thisRuleType = readCSVFile(agnosticFileName, rules, typeDefinition, CSVFormat.DEFAULT, caseSensitive, thisRuleType);
            } else if (FilenameUtils.getExtension(agnosticFileName).equals("tsv")) {
                thisRuleType = readCSVFile(agnosticFileName, rules, typeDefinition, CSVFormat.TDF, caseSensitive, thisRuleType);
            }
        }
        return thisRuleType;
    }

//    public static HashMap<Integer, NERRule> readXLSXRuleFile(String xlsxFileName) {
//        HashMap<Integer, NERRule> rules = new HashMap<Integer, NERRule>();
//        readXLSXRuleFile(xlsxFileName, rules, FASTRULEFILE, true);
//        return rules;
//    }


    public static boolean[] readXLSXRuleFile(String xlsxFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] ruleSupports) {
        try {
            FileInputStream inputStream = new FileInputStream(new File(xlsxFileName));
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet firstSheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = firstSheet.iterator();
            int id = 0;
            while (iterator.hasNext()) {
                Row nextRow = iterator.next();
                Iterator<Cell> cellIterator = nextRow.cellIterator();
                ArrayList<String> cells = new ArrayList<>();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    cell.setCellType(CellType.STRING);
                    cells.add(cell.getStringCellValue());
                }
                if (cells.size() > 0)
                    ruleSupports = parseCells(cells, id, rules, typeDefinition, caseSensitive, ruleSupports);
                id++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }

    public static boolean[] readCSVFile(String csvFileName, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, CSVFormat csvFormat, boolean caseSensitive, boolean[] ruleSupports) {
        try {
            Iterable<CSVRecord> recordsIterator = CSVParser.parse(new File(csvFileName), StandardCharsets.UTF_8, csvFormat);
            ruleSupports = readCSV(recordsIterator, rules, typeDefinition, caseSensitive, ruleSupports);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }

    public static boolean[] readCSVString(String csvString, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, String splitter, boolean caseSensitive, boolean[] ruleSupports) {
        CSVFormat csvFormat = CSVFormat.DEFAULT;
        if (splitter.equals("\t")) {
            csvFormat = CSVFormat.TDF;
        }
        ruleSupports = readCSVString(csvString, rules, typeDefinition, csvFormat, caseSensitive, ruleSupports);
        return ruleSupports;
    }

    public static boolean[] readCSVString(String csvString, HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, CSVFormat csvFormat, boolean caseSensitive, boolean[] ruleSupports) {
        try {
            Iterable<CSVRecord> recordsIterator = CSVParser.parse(csvString, csvFormat);
            ruleSupports = readCSV(recordsIterator, rules, typeDefinition, caseSensitive, ruleSupports);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ruleSupports;
    }

    private static boolean[] readCSV(Iterable<CSVRecord> recordsIterator, HashMap<Integer, Rule> rules,
                                     LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive, boolean[] ruleSupports) {
        int id = 0;
        for (CSVRecord record : recordsIterator) {
            ArrayList<String> cells = new ArrayList<>();
            for (String cell : record) {
                cells.add(cell);
            }
//          to be back compatible
            if ((!cells.get(0).startsWith("@") && !cells.get(0).startsWith("&")) && cells.size() > 1 && !UnicodeChecker.isNumber(cells.get(1)))
                cells.add(1, "1");
            ruleSupports = parseCells(cells, id, rules, typeDefinition, caseSensitive, ruleSupports);
            id++;
        }
        return ruleSupports;
    }

    private static boolean[] parseCells(ArrayList<String> cells, int id, HashMap<
            Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, boolean caseSensitive,
                                        boolean[] ruleSupports) {
        if (cells.get(0).startsWith("#") || cells.get(0).trim().length() == 0)
            return ruleSupports;
        if (cells.get(0).startsWith("@") || cells.get(0).startsWith("&")) {
//          Rule type should be defined in the 1st line that starting with '@':  '@fastner' or '@fastcner'
            if (cells.size() == 1) {
                ruleSupports = checkFastCRule(cells.get(0));
            } else if (cells.size() > 1) {
//          new UIMA type definition with '@typeName superTypeName'
//                or '@typeName superTypeName   newFeature1    newFeature2  newFeature3...'
                cells.set(0, cells.get(0).substring(1));
                typeDefinition.put(getShortName(cells.get(0)), new TypeDefinition(cells));
            }
            return ruleSupports;
        }
        if (cells.size() >= 2) {
//            if (cells.get(2).indexOf(".") == -1)
//                cells.set(2, checkNameSpace(cells.get(2)));
            String rule = cells.get(0);
            if (UnicodeChecker.isNumber(cells.get(1))) {
                String conceptShortName = getShortName(cells.get(2).trim());
                if (!typeDefinition.containsKey(conceptShortName)) {
                    typeDefinition.put(conceptShortName, new TypeDefinition(cells.get(2).trim(), DeterminantValueSet.defaultSuperTypeName, new ArrayList<>()));
                }
                ruleSupports = addRule(rules, typeDefinition, new NERRule(id, caseSensitive ? rule : rule.toLowerCase(), cells.get(2).trim(), Double.parseDouble(cells.get(1)), cells.size() > 3 ? Determinants.valueOf(cells.get(3)) : Determinants.ACTUAL), ruleSupports);
            } else {
                String conceptShortName = getShortName(cells.get(1).trim());
                if (!typeDefinition.containsKey(conceptShortName)) {
                    typeDefinition.put(conceptShortName, new TypeDefinition(cells.get(1).trim(), DeterminantValueSet.defaultSuperTypeName, new ArrayList<>()));
                }
                ruleSupports = addRule(rules, typeDefinition, new NERRule(id, caseSensitive ? rule : rule.toLowerCase(), cells.get(1).trim(), 0d, cells.size() > 2 ? Determinants.valueOf(cells.get(2)) : Determinants.ACTUAL), ruleSupports);
            }
        } else
            logger.info("Definition format error: line " + id + "\t\t" + cells);
        return ruleSupports;
    }


    public static HashMap<Integer, NERRule> readCRuleString(String ruleString, String splitter) {
        int id = 0;
        HashMap<Integer, NERRule> rules = new HashMap<>();
        for (String rule : ruleString.split("\n")) {
            rule = rule.trim();
            id++;
            if (rule.length() < 1 || rule.startsWith("#"))
                continue;
            String[] definition = rule.split(splitter);
            Determinants determinant = Determinants.ACTUAL;

            if (definition.length > 3)
                determinant = Determinants.valueOf(definition[3]);
            if (definition.length > 2) {
                definition[2] = checkNameSpace(definition[2]);
            } else if (!rule.trim().startsWith("#")) {
                logger.info("Definition format error: line " + id + "\t\t" + rule);
                continue;
            }
            rules.put(id, new NERRule(id, definition[0], definition[2].trim(), Double.parseDouble(definition[1]), determinant));
        }
        return rules;
    }


    private static boolean[] addRule(HashMap<Integer, Rule> rules, LinkedHashMap<String, TypeDefinition> typeDefinition, NERRule rule, boolean[] ruleSupports) {
//        support grouping
        if (ruleSupports[1] == false && rule.rule.indexOf("(") != -1) {
            ruleSupports[1] = true;
        }
//        support square bracket
        if (ruleSupports[2] == false && rule.rule.indexOf("[") != -1) {
            ruleSupports[2] = true;
        }
//        support replication grammar '+'
        if (ruleSupports[3] == false && rule.rule.indexOf("+") != -1) {
            ruleSupports[3] = true;
        }

//        support numeric handler
        if (ruleSupports[4] == false && ((rule.rule.indexOf("\\>") != -1) || (rule.rule.indexOf("\\<") != -1))) {
            ruleSupports[4] = true;
        }

        if (ruleSupports[5] == false && isChinese(rule.rule.toCharArray()[0])) {
            ruleSupports[5] = true;
        }

        rules.put(rule.id, rule);
        return ruleSupports;
    }

    /**
     * Rule type should be defined in the 1st line that starting with '@':  '@fastner' or '@fastcner' or '@fastcnercn'
     *
     * @param ruleString input String that contains rule definitions
     * @return if the rule is for FastCNER
     */
    private static boolean[] checkFastCRule(String ruleString) {
        int begin = ruleString.indexOf("@");
        if (begin == -1)
            begin = ruleString.indexOf("&");
        int end = ruleString.indexOf("\n", begin);
        boolean[] ruleSupports = new boolean[]{false, false, false, false, false, false};
        String definition = ruleString.substring(begin, end == -1 ? ruleString.length() : end).toLowerCase();
        if (definition.indexOf("fastcnercn") != -1) {
            ruleSupports[5] = true;
        } else if (definition.indexOf("fastcner") != -1) {
            ruleSupports[0] = true;
        }
        return ruleSupports;
    }

    private static String getShortName(String fullName) {
        int dot = fullName.lastIndexOf(".");
        if (dot != -1) {
            fullName = fullName.substring(dot + 1);
        }
        return fullName;
    }
}


