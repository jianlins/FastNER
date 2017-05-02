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
