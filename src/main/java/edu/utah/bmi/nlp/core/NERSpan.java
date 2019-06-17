package edu.utah.bmi.nlp.core;


import edu.utah.bmi.nlp.fastner.FastRule;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NERSpan extends Span implements Comparable<Span> {
    public static Logger logger = edu.utah.bmi.nlp.core.IOUtil.getLogger(FastRule.class);


    public static final String scoreOnly = "score", scorewidth = "scorewidth", widthscore = "widthscore", widthOnly = "width";
    public static final String byStrWidth = "strWidth", byRuleLength = "ruleLength";
    private String method = scorewidth;
    private String widthCompareMethod = byRuleLength;
    public int ruleLength;

    public NERSpan(int begin, int end) {
        super(begin, end);
    }

    public NERSpan(int begin, int end, String text) {
        super(begin, end, text);
    }

    public NERSpan(int begin, int end, int ruleId) {
        super(begin, end, ruleId);
    }

    public NERSpan(int begin, int end, int ruleId, double score) {
        super(begin, end, ruleId, score);
    }

    public NERSpan(int begin, int end, int ruleId, double score, String text) {
        super(begin, end, ruleId, score, text);
    }

    public NERSpan(int begin, int end, int ruleId, int ruleLength, double score, String text) {
        super(begin, end, ruleId, score, text);
        this.ruleLength = ruleLength;
    }

    public int compareTo(NERSpan o) {
        switch (method) {
            case scoreOnly:
                return compareScoreOnly(o);
            case scorewidth:
                return compareScorePrior(o);
            case widthscore:
                return compareWidthPrior(o);
            default:
                return compareWidthOnly(o);
        }
    }

    public void setCompareMethod(String method) {
        this.method = method;
    }

    public void setWidthCompareMethod(String widthCompareMethod) {
        this.widthCompareMethod = widthCompareMethod;
    }

    protected int compareScoreOnly(Span o) {
        return (int) Math.signum(this.score - o.score);
    }

    protected int compareWidthOnly(Span o) {
        if (widthCompareMethod.equals(byStrWidth)) {
            return Integer.signum(this.width - o.width);
        } else {
            if (o instanceof NERSpan)
                return Integer.signum(this.ruleLength - ((NERSpan) o).ruleLength);
            else {
                System.err.println("Cannot compare NERSpan against Span using matched rule lengths. Return equal as default.");
                return 0;
            }
        }
    }

    protected int compareScorePrior(Span o) {
        if (logger.isLoggable(Level.FINEST))
            logger.finest("\t\tcurrent " + this.ruleId + " score: " + this.score + "\t---\toverlapped " + o.ruleId + " score: " + o.score);
        int scoreResult = compareScoreOnly(o);
        if (scoreResult == 0) {
            return compareWidthOnly(o);
        } else {
            return scoreResult;
        }
    }

    protected int compareWidthPrior(Span o) {
        int widthResult = compareWidthOnly(o);
        if (widthResult == 0) {
            return compareScoreOnly(o);
        } else {
            return widthResult;
        }
    }

    public String toString() {
        return String.format("(%s-%s):%s", begin, end, ruleId);
    }

}
