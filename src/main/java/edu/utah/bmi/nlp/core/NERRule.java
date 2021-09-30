package edu.utah.bmi.nlp.core;

import java.util.ArrayList;

/**
 * Extend Rule to store additional attributes of rules
 *
 * @author Jianlin Shi
 */
public class NERRule extends Rule {

    public ArrayList<Object> attributes = new ArrayList<>();


    public NERRule(int id, String rule, String ruleName, DeterminantValueSet.Determinants type, ArrayList<Object> attributes) {
        super(id, rule, ruleName, type);
        this.attributes = attributes;
    }

    public NERRule(int id, String rule, String ruleName, double score, DeterminantValueSet.Determinants type, ArrayList<Object> attributes) {
        super(id, rule, ruleName, score, type);
        this.attributes = attributes;
    }

    public NERRule(int id, String rule, String ruleName, double score, DeterminantValueSet.Determinants type, String... attributes) {
        super(id, rule, ruleName, score, type);
        for (int i = 0; i < attributes.length; i++) {
            this.attributes.add(attributes[i]);
        }
    }


    public String toString() {
        StringBuilder serialized = new StringBuilder();
        serialized.append("Rule ");
        serialized.append(id);
        serialized.append(":\n");
        serialized.append("\trule content:\t");
        serialized.append(rule);
        serialized.append("\trule name:\t");
        serialized.append(ruleName);
        serialized.append("\trule score:\t");
        serialized.append(score);
        serialized.append("\trule type:\t");
        serialized.append(type);
        for (Object value : attributes) {
            serialized.append("\t" + value);
        }
        return serialized.toString();
    }


    public NERRule clone() {
        return new NERRule(id, rule, ruleName, score, type, (ArrayList<Object>) attributes.clone());
    }
}
