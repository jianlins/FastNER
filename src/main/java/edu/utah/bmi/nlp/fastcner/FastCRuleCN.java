package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;

import java.util.ArrayList;
import java.util.HashMap;

import static edu.utah.bmi.nlp.fastcner.UnicodeChecker.isChinese;
import static edu.utah.bmi.nlp.fastcner.UnicodeChecker.isDigit;
import static java.lang.Character.isLetter;
import static java.lang.Character.isWhitespace;


/**
 * Redefine wildcard syntax to support Chinese Characters, full width characters
 * use @fastcnercn in the rule to specify
 * \C Chinese Character
 * \c alphabetic letter
 * \p punctuations including full width punctuations
 * \w especial characters (does not include non-English language characters)
 * \s whitespace (include full width whitespace)
 * \d include both full width and half width digits
 * <p>
 * 01/26/2018 Jianlin Shi
 */
public class FastCRuleCN extends FastCRulesSB {

    public FastCRuleCN() {
    }


    public FastCRuleCN(HashMap<Integer, Rule> ruleStore) {
        super(ruleStore);
    }

    public FastCRuleCN(String ruleStr) {
        super(ruleStr);
    }

    protected void processWildCards(String text, char[] textChars, HashMap rule, int matchBegin, int matchEnd, int currentPosition, HashMap<String, ArrayList<Span>> matches, char previousChar, boolean wildcard, char previousKey) {
        char thisChar = textChars[currentPosition];
        for (Object rulechar : rule.keySet()) {
            char thisRuleChar = (Character) rulechar;
            switch (thisRuleChar) {
                case 's':
//                    if (thisChar == ' ' || thisChar == '\t' || (scSupport && !(isLetterOrDigit(thisChar) || isWhitespace(thisChar) || WildCardChecker.isPunctuation(thisChar)))) {
                    if (thisChar == ' ' || thisChar == '\t' || (int) thisChar == 160 || thisChar == '　') {
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
                    if (isDigit(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('d'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'd');
                    }
                    break;
                case 'C':
                    if (isChinese(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('C'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'C');
                    }
                    break;
                case 'c':
                    if (isLetter(thisChar)) {
                        processRules(text, textChars, (HashMap) rule.get('c'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'c');
                    }
                    break;
                case 'p':
                    if (UnicodeChecker.isPunctuation(thisChar)) {
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
                    if (!Character.isWhitespace(thisChar) && (int) thisChar != 160 && thisChar != '　')
//                    if(thisChar!=' ' && thisChar!='\t' && thisChar!='\r' && thisChar!='\n')
                        processRules(text, textChars, (HashMap) rule.get('a'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'a');
                    break;
                case 'u':
                    if (UnicodeChecker.isSpecialChar(thisChar))
                        processRules(text, textChars, (HashMap) rule.get('u'), matchBegin, matchEnd, currentPosition + 1, matches,
                                thisChar, true, 'u');
                    break;

                case 'w':
                    if (isWhitespace(thisChar) || (int) thisChar == 160 || UnicodeChecker.isSpecialChar(thisChar) || thisChar == '　') {
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
                    //                        if (thisChar == ' ' || thisChar == '\t' || (int)thisChar==160 || (scSupport && !(isLetterOrDigit(thisChar) || isWhitespace(thisChar) || UnicodeChecker.isPunctuation(thisChar)))) {
                    if ((thisChar == ' ' || thisChar == '\t' || (int) thisChar == 160)) {
                        while ((thisChar == ' ' || thisChar == '\t' || (int) thisChar == 160) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
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
                    if (isDigit(thisChar)) {
                        while ((isDigit(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'C':
                    if (isChinese(thisChar)) {
                        while ((isChinese(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'c':
                    if (isLetter(thisChar)) {
                        while ((isLetter(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }//
                    }
                    break;
                case 'p':
                    if (UnicodeChecker.isPunctuation(thisChar)) {
                        while ((UnicodeChecker.isPunctuation(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'a':
                    if (!Character.isWhitespace(thisChar) && (int) thisChar != 160 && thisChar != '　') {
                        while ((!Character.isWhitespace(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'u':
                    if (UnicodeChecker.isSpecialChar(thisChar)) {
                        while ((UnicodeChecker.isSpecialChar(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
                            currentPosition++;
                            currentRepeats++;
                            if (currentPosition == textChars.length)
                                break;
                            thisChar = textChars[currentPosition];
                        }
                    }
                    break;
                case 'w':
                    if (isWhitespace(thisChar) || (int) thisChar == 160 || UnicodeChecker.isSpecialChar(thisChar) || thisChar == '　') {
                        while ((isWhitespace(thisChar) || (int) thisChar == 160 || UnicodeChecker.isSpecialChar(thisChar)) && currentRepeats < maxRepeatLength && currentPosition < textChars.length) {
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


}
