package edu.utah.bmi.nlp.fastner;

import edu.utah.bmi.nlp.core.DeterminantValueSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by
 *
 * @author Jianlin Shi on 5/1/17.
 */
public class TypeDefinition {
    public String fullSuperTypeName;
    public String fullTypeName;
    public List<String> newFeatureNames = new ArrayList<>();

    public TypeDefinition(String typeName, String superTypeName, ArrayList<String> newFeatureNames) {
        this.fullTypeName = DeterminantValueSet.checkNameSpace(typeName);
        this.fullSuperTypeName = DeterminantValueSet.checkNameSpace(superTypeName);
        this.newFeatureNames = newFeatureNames;
    }

    public TypeDefinition(List<String> definition) {
        this.fullTypeName = DeterminantValueSet.checkNameSpace(definition.get(0));
        this.fullSuperTypeName = DeterminantValueSet.checkNameSpace(definition.get(1));
        if (definition.size() > 2)
            this.newFeatureNames = definition.subList(2, definition.size());

    }

    public String getFullSuperTypeName() {
        return fullSuperTypeName;
    }

    public void setFullSuperTypeName(String fullSuperTypeName) {
        this.fullSuperTypeName = DeterminantValueSet.checkNameSpace(fullSuperTypeName);
    }

    public String getFullTypeName() {
        return fullTypeName;
    }

    public void setFullTypeName(String fullTypeName) {
        this.fullTypeName = DeterminantValueSet.checkNameSpace(fullTypeName);
    }

    public List<String> getNewFeatureNames() {
        return newFeatureNames;
    }

    public void setNewFeatureNames(ArrayList<String> newFeatureNames) {
        this.newFeatureNames = newFeatureNames;
    }
}
