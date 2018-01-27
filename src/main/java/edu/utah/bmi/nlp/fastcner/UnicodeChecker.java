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


}
