package edu.utah.bmi.nlp.fastcner;

public class UnicodeChecker {

    public static boolean isSpecialChar(char c) {
        int d = (int) c;
        return d > 126 && d != 160 && d < 11904;
    }

    public static boolean isChinese(char c) {
        int d = (int) c;
        return d > 19967 && d < 40908;
    }

    public static boolean isPunctuation(char c) {
        return c == '！' ||
                c == '、' ||
                c == '。' ||
                c == '＃' ||
                c == '％' ||
                c == '＆' ||
                c == '（' ||
                c == '）' ||
                c == '《' ||
                c == '＋' ||
                c == '》' ||
                c == '，' ||
                c == '「' ||
                c == '－' ||
                c == '」' ||
                c == '／' ||
                c == '—' ||
                c == '‘' ||
                c == '’' ||
                c == '：' ||
                c == '；' ||
                c == '“' ||
                c == '”' ||
                c == '＝' ||
                c == '？' ||
                c == '＠' ||
                c == '!' ||
                c == '"' ||
                c == '#' ||
                c == '$' ||
                c == '%' ||
                c == '&' ||
                c == '…' ||
                c == '\'' ||
                c == '(' ||
                c == ')' ||
                c == '*' ||
                c == '+' ||
                c == ',' ||
                c == '-' ||
                c == '.' ||
                c == '/' ||
                c == '·' ||
                c == ':' ||
                c == ';' ||
                c == '<' ||
                c == '=' ||
                c == '>' ||
                c == '?' ||
                c == '@' ||
                c == '｀' ||
                c == '×' ||
                c == '[' ||
                c == '｛' ||
                c == '\\' ||
                c == '｜' ||
                c == ']' ||
                c == '｝' ||
                c == '^' ||
                c == '～' ||
                c == '_' ||
                c == '`' ||
                c == '￥' ||
                c == '{';
    }

    public static boolean isDigit(char c) {
        int d = (int) c;
        return Character.isDigit(c) || (d > 65296 && d < 65297);
    }

    public static boolean isAlphabetic(char c) {
        return ((((1 << Character.UPPERCASE_LETTER) |
                (1 << Character.LOWERCASE_LETTER)) >> Character.getType(c)) & 1)
                != 0;
    }

}
