package edu.utah.bmi.nlp.core;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Extend Rule to store additional attributes of rules
 *
 * @author Jianlin Shi
 */
public class NERRule extends Rule {

    public LinkedHashMap<String, String> attributes = new LinkedHashMap<>();


    public NERRule(int id, String rule, String ruleName, DeterminantValueSet.Determinants type, LinkedHashMap<String, String> attributes) {
        super(id, rule, ruleName, type);
        this.attributes = attributes;
    }

    public NERRule(int id, String rule, String ruleName, double score, DeterminantValueSet.Determinants type, LinkedHashMap<String, String> attributes) {
        super(id, rule, ruleName, score, type);
        this.attributes = attributes;
    }

    public NERRule(int id, String rule, String ruleName, double score, DeterminantValueSet.Determinants type, String... attributes) {
        super(id, rule, ruleName, score, type);
        if (attributes.length % 2 != 0) {
            throw new IllegalArgumentException("Attributes to create NERRule need to be pairs. " + Arrays.asList(attributes));
        } else {
            for (int i = 0; i < attributes.length - 1; i++) {
                this.attributes.put(attributes[i], attributes[i + 1]);
            }
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
        for (String attributeName : attributes.keySet()) {
            serialized.append("\t" + attributeName + ":" + attributes.get(attributeName));
        }
        return serialized.toString();
    }


    public NERRule clone() {
        return new NERRule(id, rule, ruleName, score, type, (LinkedHashMap<String, String>) attributes.clone());
    }
}
