package edu.utah.bmi.nlp.fastcner;

import edu.utah.bmi.nlp.core.NERRule;
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
public class FastCRuleCN extends FastCRuleSB {

    public FastCRuleCN() {
    }


    public FastCRuleCN(HashMap<Integer, Rule> ruleStore) {
        super(ruleStore);
    }

    public FastCRuleCN(String ruleStr) {
        super(ruleStr);
    }


    protected boolean iss(char thisChar) {
        return (thisChar == ' ' || thisChar == '\t' || (int) thisChar == 160);
    }

    protected boolean isd(char thisChar) {
        return UnicodeChecker.isDigit(thisChar);
    }
    protected boolean isC(char thisChar) {
        return UnicodeChecker.isChinese(thisChar);
    }

    protected boolean isc(char thisChar) {
        return UnicodeChecker.isAlphabetic(thisChar);
    }

    protected boolean isp(char thisChar) {
        return UnicodeChecker.isPunctuation(thisChar);
    }

    protected boolean isu(char thisChar) {
        return UnicodeChecker.isSpecialChar(thisChar);
    }

    protected boolean isw(char thisChar) {
        return isWhitespace(thisChar) || (int) thisChar == 160 || UnicodeChecker.isSpecialChar(thisChar) || thisChar == '　';
    }

    protected boolean isa(char thisChar) {
        return !isWhitespace(thisChar) && (int) thisChar != 160 && thisChar != '　';
    }



}
