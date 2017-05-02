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
/**
 * The FastNER is an efficient token-based rule processing engineer. Instead of iteratively checking rulesMap, FastNER
 * constructs the rulesMap in a set of chained-up HashMaps. Each hinge is built by a HashMap, with the key representing the
 * token at the corresponding place of the rule, and the value representing the following chained-up HashMaps. Each rule 
 * in the chain will be line up at the end in 2-hinge-chain HashMap: 
 * <<end>, Determinants>
 * <Determinants, null>
 *
 * This structure allows process the rulesMap in a close-to-linear time. Instead of being dependent to the number of rulesMap, length of rulesMap
 * (in number of tokens) and the length of input (in number of tokens), FastNER only depends on the length of input and partly
 * on the length of rulesMap.
 *
 *    The best running time is 1 x number of input tokens.
 *    The worst running time is << longest rule length x number of input tokens.
 *
 *
 * Note:
 * Because FastContext does not implement real regular expression. Rather, it implements a few regular expression capability instead.
 *  1. use \w+ to represent a word (corresponding to any input element in the ArrayList<String> or ArrayList<Annotation>)
 *  2. use "> number" (there is a whitespace between them) to represent any number greater than the given "number", and "< number" to
 *  represent any number smaller than the given "number".
 *     As a results, you will need consider input digit as single token annotation/word, which is annotated differently in some parsers.
 * Please referring to the example rule file: "conf/rulesMap.csv"
 *
 *
 */
/**
 * @author Jianlin Shi
 *
 */
package edu.utah.bmi.nlp.fastner;


