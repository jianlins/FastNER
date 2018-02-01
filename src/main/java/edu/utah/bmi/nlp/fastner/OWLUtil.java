package edu.utah.bmi.nlp.fastner;

import edu.utah.blulab.domainontology.Anchor;
import edu.utah.blulab.domainontology.DomainOntology;
import edu.utah.blulab.domainontology.LogicExpression;
import edu.utah.blulab.domainontology.Variable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

public class OWLUtil {
    public static Logger logger = edu.utah.bmi.nlp.core.IOUtil.getLogger(IOUtil.class);

    public static String readOwlDirectory(String owlFileDirectory, ArrayList<ArrayList<String>> cells, boolean caseSensitive) {
        Collection<File> files = FileUtils.listFiles(new File(owlFileDirectory), new String[]{"owl"}, true);
        StringBuilder concatenated = new StringBuilder();
        for (File file : files) {
            concatenated.append(readOwlFile(file.getAbsolutePath(), cells, caseSensitive));
        }
        return concatenated.toString();
    }

    public static String readOwlFile(String owlFileName, ArrayList<ArrayList<String>> cells, boolean caseSensitive) {
        StringBuilder concatenated = new StringBuilder();
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

                            addRow(preferredTerm, caseSensitive, nameEntityClass, ++id, cells, "ACTUAL", concatenated);

                            if (term.getSynonym().size() > 0) {
                                for (String s : term.getSynonym()) {
                                    addRow(s, caseSensitive, nameEntityClass, ++id, cells, "ACTUAL", concatenated);
                                }
                            }
                            if (term.getAbbreviation().size() > 0) {
                                for (String s : term.getAbbreviation()) {
                                    addRow(s, caseSensitive, nameEntityClass, ++id, cells, "ACTUAL", concatenated);
                                }
                            }
                            if (term.getMisspelling().size() > 0) {
                                for (String s : term.getMisspelling()) {
                                    addRow(s, caseSensitive, nameEntityClass, ++id, cells, "ACTUAL", concatenated);
                                }
                            }
                            if (term.getPseudos().size() > 0) {
                                for (String s : term.getMisspelling()) {
                                    addRow(s, caseSensitive, nameEntityClass, ++id, cells, "PSEUDO", concatenated);
                                }
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
        return concatenated.toString();
    }

    private static void addRow(String s, boolean caseSensitive, String nameEntityClass, int id,
                               ArrayList<ArrayList<String>> cells, String type, StringBuilder concatenated) {
        String rulestr = caseSensitive ? s : s.toLowerCase();
        concatenated.append(rulestr + "\n");
        addRow(cells, new String[]{id + "", rulestr, nameEntityClass, "０", type});
    }

    private static void addRow(ArrayList<ArrayList<String>> cells, String[] newRow) {
        ArrayList<String> row = new ArrayList<>();
        for (String cell : newRow) {
            row.add(cell);
        }
        cells.add(row);
    }
}
