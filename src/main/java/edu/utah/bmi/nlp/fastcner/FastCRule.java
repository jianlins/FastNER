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
package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.DeterminantValueSet.Determinants;
import edu.utah.bmi.nlp.core.*;
import edu.utah.bmi.nlp.fastner.FastRuleWG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import static java.lang.Character.*;

/**
 * This class is an extension of FastRules. Instead of handling string-element ruleStore, it handles char-element ruleStore
 * Wildcard definition:
 * <p>
 * (   Beginning of capturing a group
 * )   End of capturing a group
 * \p   A punctuation
 * <p>
 * \ plus following characters
 * +   An addition symbol (to distinguish the "+" after a wildcard)
 * (   A left parentheses symbol
 * )   A right parentheses symbol
 * d   A digit
 * C   A capital letter
 * c   A lowercase letter
 * s   A whitespace
 * a   A Non-whitespace character
 * u   A unusual character: not a letter, not a number, not a punctuation, not a whitespace
 * n   A return
 * <p>
 * The wildcard plus "+": 1 or more wildcard
 * <p>
 * NOTE: FastCRule is slightly different to FastCRule when handling replicates.
 * FastCRule uses loops (usually is much faster)
 * <p>
 * Because it use loops, it doesn't check any other possible ruleStore that might match the replicates at the same time.
 * The rule like "\\c+w" won't work as expected: the rule will be matched all the way to "w" when checking "\\c+".
 * </p>
 * FastCRule uses iterations.
 * Once FastCRule is fully tested and compared with FastCRule, FastCRule might be deprecated.
 *
 * @author Jianlin Shi
 */
//TODO need to support | (e.g. to detect MR #146-55-23-5)

public class FastCRule extends FastRuleWG {
    //  other  fields are defined in abstract class
    protected HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
    protected final Determinants END = Determinants.END;
    //  max length of repeat char---to prevent overflow 25 works perfect, 10 is optimized for speed
    protected int maxRepeatLength = 30;
    protected boolean supportReplications = false, scSupport = false;
    protected String method = "width";
    protected int offset = 0;

    //    Because the match branches caused by wildcards, some right matches can be found before left matches
//    A segment tree is maintained to check the overlapping among matches within a same type of concept
    protected HashMap<String, IntervalST> overlapCheckers = new HashMap<>();


    protected FastCRule() {
    }


    public FastCRule(String ruleStr) {
//        support read from OWl file, TSV file or OWL file directory
        super(ruleStr);
    }

    public FastCRule(HashMap<Integer, Rule> ruleStore) {
        initiate(ruleStore);
    }


    /**
     * Override addRule method
     *
     * @return true: if the rule is added
     * false: if the rule is a duplicate
     */
    @SuppressWarnings("unchecked")
    protected boolean addRule(Rule rule) {
//      use to store the HashMap sub-chain that have the key chain that overlap with the current rule
//      rule1 to temporally store the hinges of existing HashMap chain that overlap with current rule
        char[] crule = rule.rule.toCharArray();
        String determinant = rule.ruleName;
        HashMap rule1 = rulesMap;
//      rule2 to construct the new HashMap sub-chain that doesn't overlap with existing chain
        HashMap rule2 = new HashMap();
        HashMap rulet = new HashMap();
        int length = crule.length;
        int i = 0;

        while (i < length && rule1 != null && rule1.containsKey(crule[i])) {
            rule1 = (HashMap) rule1.get(crule[i]);
            i++;
        }
        // if the rule has been included
        if (i == length && rule1.containsKey(END) && rule1.get(END) == determinant) {
            logger.info("This rule has been included");
            return false;
        }
        // start with the determinant, construct the last descendant HashMap
        // <Determinant.end, <Determinant, pos>>
        if (i == length) {
            if (rule1.containsKey(END)) {
                ((HashMap) rule1.get(END)).put(determinant, rule.id);
            } else {
                rule2.put(determinant, rule.id);
                rule1.put(END, rule2.clone());
            }
            setScore(rule.id, rule.score);
            return true;
        } else {
            rule2.put(determinant, rule.id);
            rule2.put(END, rule2.clone());
            rule2.remove(determinant);

            // filling the HashMap chain which ruleStore doesn't have the key chain
            for (int j = length - 1; j > i; j--) {
                rulet = (HashMap) rule2.clone();
                rule2.clear();
                rule2.put(crule[j], rulet);
            }
        }
//      map rule to score;
        setScore(rule.id, rule.score);

        rule1.put(crule[i], rule2.clone());
        return true;
    }

    public HashMap<String, ArrayList<Span>> processTokens(ArrayList<String> contextTokens) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("This method is not used in character-based ruleStore");
        return null;
    }

    public HashMap<String, ArrayList<Span>> processSpans(ArrayList<Span> contextTokens) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("This method is not used in character-based ruleStore");
        return null;
    }

    public HashMap<String, ArrayList<Span>> processString(String text) {
        offset = 0;
        return processRules(text);
    }

    public HashMap<String, ArrayList<Span>> processString(String text, int offset) {
        this.offset = offset;
        return processRules(text);
    }

    public HashMap<String, ArrayList<Span>> processSpan(Span span) {
        return processString(span.text, span.begin);
    }


    public HashMap<String, ArrayList<Span>> processRules(String text) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<>();
        char[] textChars = text.toCharArray();
        for (int i = 0; i < textChars.length; i++) {
            char previousChar = i > 0 ? textChars[i - 1] : ' ';
            processRules(text, textChars, rulesMap, i, -1, i, matches, previousChar, false, ' ');
        }
        if (removePseudo)
            removePseudoMatches(matches);
        return matches;

    }


    protected void processRules(String text, char[] textChars, HashMap rule, int matchBegin, int matchEnd, int currentPosition,
                                HashMap<String, ArrayList<Span>> matches,
                                char previousChar, boolean wildcard, char previousKey) {
        // when reach the end of the tunedcontext, end the iteration
        if (currentPosition < textChars.length) {
            char thisChar = textChars[currentPosition];

            if (rule.containsKey('\\')) {
                processWildCards(text, textChars, (HashMap) rule.get('\\'), matchBegin, matchEnd, currentPosition, matches, previousChar, true, '\\');
            }
            if (rule.containsKey('(') && previousKey != '\\') {
                processRules(text, textChars, (HashMap) rule.get('('), currentPosition, matchEnd, currentPosition, matches,
                        previousChar, false, '(');
            }
            if (rule.containsKey(')') && previousKey != '\\') {
                processRules(text, textChars, (HashMap) rule.get(')'), matchBegin, currentPosition, currentPosition, matches,
                        previousChar, false, ')');

            }
            // if the end of a rule is met

            if (rule.containsKey(END)) {
                addDeterminants(text, rule, matches, matchBegin, matchEnd, currentPosition);
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisChar) && (thisChar != ')' && thisChar != '(')) {
                processRules(text, textChars, (HashMap) rule.get(thisChar), matchBegin, matchEnd, currentPosition + 1, matches,
                        thisChar, false, thisChar);
            }

//            if(currentRepeats>0)
//                currentRepeats=currentRepeats;
//          Replications of current char
            if (supportReplications && rule.containsKey('+')) {
//                processRules(textChars, (HashMap) rule.get('+'), matchBegin, matchEnd, currentPosition, matches,
//                        previousChar, false, ' ');
                processRules(text, textChars, (HashMap) rule.get('+'), matchBegin, matchEnd, currentPosition, matches,
                        thisChar, false, '+');
                processReplicants(text, textChars, (HashMap) rule.get('+'), matchBegin, matchEnd, currentPosition, matches,
                        thisChar, wildcard, previousKey);
            }


        } else if (currentPosition == textChars.length && rule.containsKey(END)) {
            if (matchEnd == -1)
                addDeterminants(text, rule, matches, matchBegin, currentPosition, currentPosition);
            else
                addDeterminants(text, rule, matches, matchBegin, matchEnd, currentPosition);
        } else if (currentPosition == textChars.length && rule.containsKey('\\') && ((HashMap) rule.get('\\')).containsKey('e')) {
            HashMap deterRule = ((HashMap) ((HashMap) rule.get('\\')).get('e'));
            if (matchEnd == -1)
                addDeterminants(text, deterRule, matches, matchBegin, currentPosition, currentPosition);
            else
                addDeterminants(text, deterRule, matches, matchBegin, matchEnd, currentPosition);
        } else if (currentPosition == textChars.length && rule.containsKey(')')) {
            HashMap deterRule = (HashMap) rule.get(')');
            if (deterRule.containsKey(END)) {
                addDeterminants(text, deterRule, matches, matchBegin, currentPosition, currentPosition);
            } else if (deterRule.containsKey('\\') && ((HashMap) deterRule.get('\\')).containsKey('e'))
                processRules(text, textChars, (HashMap) ((HashMap) deterRule.get('\\')).get('e'), matchBegin, matchEnd, currentPosition, matches, previousChar, false, ' ');
        } else if (currentPosition == textChars.length && rule.containsKey('+')) {
            HashMap deterRule = (HashMap) rule.get('+');
            processRules(text, textChars, deterRule, matchBegin, matchEnd, currentPosition, matches, previousChar, wildcard, previousKey);
        }
    }

    protected boolean iss(char thisChar) {
        return (thisChar == ' ' || thisChar == '\t' || (int) thisChar == 160);
    }

    protected boolean isd(char thisChar) {
        return isDigit(thisChar);
    }

    protected boolean isC(char thisChar) {
        return isUpperCase(thisChar);
    }

    protected boolean isc(char thisChar) {
        return isLowerCase(thisChar);
    }

    protected boolean isp(char thisChar) {
        return WildCardChecker.isPunctuation(thisChar);
    }

    protected boolean isu(char thisChar) {
        return WildCardChecker.isSpecialChar(thisChar);
    }

    protected boolean isw(char thisChar) {
        return isWhitespace(thisChar) || (int) thisChar == 160 || WildCardChecker.isSpecialChar(thisChar);
    }

    protected boolean isa(char thisChar) {
        return !isWhitespace(thisChar) && !((int) thisChar == 160);
    }


    protected void processWildCards(String text, char[] textChars, HashMap rule, int matchBegin, int matchEnd, int currentPosition, HashMap<String, ArrayList<Span>> matches, char previousChar, boolean wildcard, char previousKey) {
        char thisChar = textChars[currentPosition];
        for (Object rulechar : rule.keySet()) {
            char thisRuleChar = (Character) rulechar;
            switch (thisRuleChar) {
                case 's':
//                    if (thisChar == ' ' || thisChar == '\t' || (scSupport && !(isLetterOrDigit(thisChar) || isWhitespace(thisChar) || WildCardChecker.isPunctuation(thisChar)))) {
                    if (iss(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('s'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 's');
                    }
                    break;
                case 'n':
                    if (thisChar == '\n' || thisChar == '\r') {
                        processRules(text, textChars, (HashMap) rule.get('n'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'n');
                    }
                    break;
                case '(':
                    if (thisChar == '(')
                        processRules(text, textChars, (HashMap) rule.get('('), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, '(');
                    break;
                case ')':
                    if (thisChar == ')')
                        processRules(text, textChars, (HashMap) rule.get(')'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, ')');
                    break;
                case 'd':
                    if (isd(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('d'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'd');
                    }
                    break;
                case 'C':
                    if (isC(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('C'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'C');
                    }
                    break;
                case 'c':
                    if (isc(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('c'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'c');
                    }
                    break;
                case 'p':
                    if (isp(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('p'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'p');
                    }
                    break;
                case '+':
                    if (thisChar == '+') {
                        processRules(text, textChars, (HashMap) rule.get('+'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, '+');
                    }
                    break;
                case '\\':
                    if (thisChar == '\\') {
                        processRules(text, textChars, (HashMap) rule.get('\\'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, false, '\\');
                    }
                    break;
                case 'b':
                    if (currentPosition == 0)
                        processRules(text, textChars, (HashMap) rule.get('b'), matchBegin, matchEnd, currentPosition, matches,
                                previousChar, false, 'b');
                    break;
                case 'a':
                    if (isa(thisChar))
//                    if(thisChar!=' ' && thisChar!='\t' && thisChar!='\r' && thisChar!='\n')
                        processRules(text, textChars, (HashMap) rule.get('a'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'a');
                    break;
                case 'u':
                    if (isu(thisChar))
                        processRules(text, textChars, (HashMap) rule.get('u'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'u');
                    break;

                case 'w':
                    if (isw(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('w'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'w');
                    }
                    break;
//                TODO negation rule
//                case '^':
//                    break;

            }
        }

    }

    protected void processReplicants(String text, char[] textChars, HashMap rule, int matchBegin, int matchEnd, int currentPosition, HashMap<String, ArrayList<Span>> matches, char previousChar, boolean wildcard, char previousKey) {
        char thisChar = textChars[currentPosition];
        int currentRepeats = 0;
        if (wildcard) {
            switch (previousKey) {
                case 's':
                    //                        if (thisChar == ' ' || thisChar == '\t' || (int)thisChar==160 || (scSupport && !(isLetterOrDigit(thisChar) || isWhitespace(thisChar) || WildCardChecker.isPunctuation(thisChar)))) {
                    if (iss(thisChar)) {
                        while (iss(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'n':
                    if ((thisChar == '\n' || thisChar == '\r')) {
                        while ((thisChar == '\n' || thisChar == '\r') && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'd':
                    if (isd(thisChar)) {
                        while (isd(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'C':
                    if (isC(thisChar)) {
                        while (isC(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'c':
                    if (isc(thisChar)) {
                        while (isc(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }//
                    }
                    break;
                case 'p':
                    if (isp(thisChar)) {
                        while (isp(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'a':
                    if (isa(thisChar)) {
                        while (isa(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'u':
                    if (isu(thisChar)) {
                        while (isu(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'w':
                    if (isw(thisChar)) {
                        while (isw(thisChar) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
            }
            processRules(text, textChars, rule, matchBegin, matchEnd, currentPosition, matches,
                    previousChar, false, '+');
        } else if (thisChar == previousKey) {
            while ((thisChar == previousKey) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                currentPosition++;
                currentRepeats++;
                if (currentPosition == textChars.length)
                    break;
                thisChar = textChars[currentPosition];

            }
            processRules(text, textChars, rule, matchBegin, matchEnd, currentPosition, matches,
                    previousChar, false, '+');
        }
    }


    protected void addDeterminants(String text, HashMap rule, HashMap<String, ArrayList<Span>> matches,
                                   int matchBegin, int matchEnd, int currentPosition) {
        HashMap<Determinants, Integer> deterRule = (HashMap<Determinants, Integer>) rule.get(END);
        int end = matchEnd == -1 ? currentPosition : matchEnd;
        if (matchBegin > end) {
            StringBuilder sb = new StringBuilder();
            for (Object key : deterRule.keySet()) {
                int rulePos = deterRule.get(key);
                sb.append(getRule(rulePos).toString());
                sb.append("\n");
            }
            logger.warning("Rule definition error ----matched begin > matched end\n" +
                    "check the following rules: \n" + sb.toString());
            int snippetBegin = matchBegin - 100;
            snippetBegin = snippetBegin < 0 ? 0 : snippetBegin;
            int snippetEnd = end + 100;
            snippetEnd = snippetEnd > text.length() ? text.length() : snippetEnd;
            logger.warning("try to match span: " + text.substring(snippetBegin, end) + "<*>"
                    + text.substring(end, matchBegin) + "<*>" + text.substring(matchBegin, snippetEnd));
            return;

        }
        Span currentSpan = new Span(matchBegin + offset, end + offset, text.substring(matchBegin, end));
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Try to addDeterminants: " + currentSpan.begin + ", " + currentSpan.end + "\t" + currentSpan.text);

        for (Object key : deterRule.keySet()) {
            ArrayList<Span> currentSpanList = new ArrayList<>();
            int rulePos = deterRule.get(key);
            double score = getScore(rulePos);
            currentSpan.ruleId = rulePos;
            currentSpan.score = score;
            if (logger.isLoggable(Level.FINEST))
                logger.finest("\t\tRule Id: " + rulePos + "\t" + key + "\t" + getRule(rulePos).type + "\t" + getRuleString(rulePos));
//          If needed, implement your own selection ruleStore and score updating logic below
            if (matches.containsKey(key)) {
//              because the ruleStore are all processed at the same time from the input left to the input right,
//                it becomes more efficient to compare the overlaps
                currentSpanList = matches.get(key);
                IntervalST<Integer> overlapChecker = overlapCheckers.get(key);
                Object overlappedPos = overlapChecker.get(new Interval1D(currentSpan.begin, currentSpan.end - 1));
                if (overlappedPos != null) {
                    int pos = (int) overlappedPos;
                    Span overlappedSpan = currentSpanList.get(pos);
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest("\t\tOverlapped with: " + overlappedSpan.begin + ", " + overlappedSpan.end + "\t" +
                                text.substring(overlappedSpan.begin - offset, overlappedSpan.end - offset));
                    if (!compareSpan(currentSpan, overlappedSpan)) {
                        if (logger.isLoggable(Level.FINEST))
                            logger.finest("\t\tSkip this span ...");
                        continue;
                    }
                    currentSpanList.set(pos, currentSpan);
                    overlapChecker.remove(new Interval1D(overlappedSpan.begin, overlappedSpan.end - 1));
                    overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), pos);
                } else {
                    overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), currentSpanList.size());
                    currentSpanList.add(currentSpan);
                }
            } else {
                currentSpanList.add(currentSpan);
                matches.put((String) key, currentSpanList);
                IntervalST<Integer> overlapChecker = new IntervalST<Integer>();
                overlapChecker.put(new Interval1D(currentSpan.begin, currentSpan.end - 1), 0);
                overlapCheckers.put((String) key, overlapChecker);
            }
        }
    }

    public double getScore(Span span) {
        return scores.get(span.ruleId);
    }

    public double getScore(int ruleId) {
        return scores.get(ruleId);
    }

    public void setScore(int ruleId, double score) {
        scores.put(ruleId, score);
    }

    /**
     * Using "+" to support replications might slow down the performance of FastCRule,
     * try to avoid using it as much as possible.
     *
     * @param support support replications
     */
    public void setReplicationSupport(boolean support) {
        this.supportReplications = support;
    }

    public void setCompareMethod(String method) {
        this.method = method;
    }

    protected boolean compareScoreOnly(Span a, Span b) {
        if (getScore(a) < 0)
            return true;
        if (getScore(b) < 0)
            return false;
        return getScore(a) > getScore(b);
    }

    protected boolean compareWidthOnly(Span a, Span b) {
        return a.width > b.width;
    }

    protected boolean compareScorePrior(Span a, Span b) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("\t\tcurrent " + a.ruleId + " score: " + getScore(a.ruleId) + "\t---\toverlapped " + b.ruleId + " score: " + getScore(b.ruleId));
        if (getScore(a) < 0)
            return true;
        if (getScore(b) < 0)
            return false;
        if (getScore(a) > getScore(b)) {
            return true;
        } else if (getScore(a) >= getScore(b) && a.width > b.width
//                && getRule(b.ruleId).type != Determinants.PSEUDO
                ) {
            return true;
        }
        return false;
    }

    protected boolean compareWidthPrior(Span a, Span b) {
        if (a.width > b.width) {
            return true;
        } else if (a.width == b.width && getScore(a) > getScore(b)) {
            return true;
        }
        return false;
    }

    protected boolean compareSpan(Span a, Span b) {
        switch (method) {
            case "score":
                return compareScoreOnly(a, b);
            case "scorewidth":
                return compareScorePrior(a, b);
            case "widthscore":
                return compareWidthPrior(a, b);
            default:
                return compareWidthOnly(a, b);
        }
    }

    public void setSpecialCharacterSupport(Boolean scSupport) {
        this.scSupport = scSupport;
    }

    public void setMaxRepeatLength(int maxRepeatLength) {
        this.maxRepeatLength = maxRepeatLength;
    }

}
