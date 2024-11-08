package de.blitzcoder.collide.engine.parser;

import de.blitzcoder.collide.util.Log;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 *
 * @author blitzcoder
 */
public class CodeTokenizer {

    private static char[] seperators = (" :,#%$()|[]{}.?<>=!;+-/*" + (char) 9).toCharArray();

    public TokenizedCode tokenize(String str, boolean ignoreSpaces) {
        return tokenize(str.split("\n"), ignoreSpaces);
    }

    public TokenizedCode tokenize(String str) {
        return tokenize(str, true);
    }

    public TokenizedCode tokenize(String[] str) {
        return tokenize(str, true);
    }

    public TokenizedCode tokenize(String[] str, boolean ignoreSpaces) {
        String[] origText = str.clone();
        Object[] ret = fixContinuedLines(str);
        str = (String[]) ret[0];
        int[] reallines = (int[]) ret[1];
        String[][] token = split(str, ignoreSpaces);
        token = fixComments(token);
        ret = fixMultiCommandPerLine(token, reallines);
        token = (String[][]) ret[0];
        reallines = (int[]) ret[1];
        return new TokenizedCode(token, reallines, origText);
    }

    private String[][] fixComments(String[][] code) {
        for (int l = 0; l < code.length; l++) {
            for (int t = 1; t < code[l].length; t++) {
                if (code[l][t].startsWith("'")) {
                    code[l] = Arrays.copyOfRange(code[l], 0, t);
                    break;
                }
            }
        }
        return code;
    }

    private String[][] split(String[] code, boolean ignoreSpaces) {
        String[][] output = new String[code.length][];
        for (int l = 0; l < code.length; l++) {
            LinkedList<String> lineList = new LinkedList<String>();
            char[] chars = code[l].toCharArray();
            int tokenStart = 0;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == 34) {
                    int pos = chars.length - 1;
                    for (int i_ = i + 1; i_ < chars.length; i_++) if (chars[i_] == 34) {
                        pos = i_;
                        break;
                    }
                    lineList.addLast(code[l].substring(i, pos + 1));
                    i = pos;
                    tokenStart = i + 1;
                    continue;
                }
                if (isSeparator(c)) {
                    if (tokenStart != i) lineList.addLast(code[l].substring(tokenStart, i));
                    if (c != 32 && c != 9) lineList.addLast("" + c);
                    tokenStart = i + 1;
                    continue;
                }
                if (i == chars.length - 1) {
                    lineList.addLast(code[l].substring(tokenStart, i + 1));
                    continue;
                }
            }
            output[l] = lineList.toArray(new String[0]);
        }
        return output;
    }

    private boolean isSeparator(char c) {
        for (int i = 0; i < seperators.length; i++) if (c == seperators[i]) return true;
        return false;
    }

    private Object[] fixMultiCommandPerLine(String[][] code, int[] reallines) {
        LinkedList<String[]> output = new LinkedList<String[]>();
        LinkedList<Integer> reallineslist = new LinkedList<Integer>();
        for (int l = 0; l < code.length; l++) {
            boolean ok = true;
            int pos = 0;
            for (int i = 0; i < code[l].length; i++) {
                if (code[l][i].length() != 0 && code[l][i].charAt(0) == 59 && pos != i) {
                    output.addLast(Arrays.copyOfRange(code[l], pos, i - 1));
                    reallineslist.addLast(new Integer(reallines[l]));
                    pos = i + 1;
                    ok = false;
                }
            }
            if (!ok && pos != code[l].length) {
                output.addLast(Arrays.copyOfRange(code[l], pos, code[l].length));
                reallineslist.addLast(new Integer(reallines[l]));
            }
            if (ok) {
                output.addLast(code[l]);
                reallineslist.addLast(new Integer(reallines[l]));
            }
        }
        reallines = new int[reallineslist.size()];
        ListIterator<Integer> it = reallineslist.listIterator();
        int i = 0;
        while (it.hasNext()) {
            reallines[i] = it.next().intValue();
            i++;
        }
        return new Object[] { output.toArray(new String[0][0]), reallines };
    }

    private Object[] fixContinuedLines(String[] code) {
        int arrayLen = code.length;
        int[] reallines = new int[code.length];
        for (int i = 0; i < reallines.length; i++) reallines[i] = i;
        for (int i = 0; i < arrayLen; i++) {
            if (code[i].endsWith("..")) {
                if (code.length - 1 > i + 1) {
                    code[i] = code[i].substring(0, code[i].length() - 2) + code[i + 1];
                    arrayLen--;
                    for (int l = i + 1; l < arrayLen; l++) {
                        code[l] = code[l + 1];
                        reallines[l]++;
                    }
                    i--;
                }
            }
        }
        return new Object[] { Arrays.copyOf(code, arrayLen), reallines };
    }
}
