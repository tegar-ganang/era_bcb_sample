package core.preprocess.corpus;

import java.util.Arrays;
import java.util.Vector;
import java.util.HashMap;
import core.util.Constant;

public class BadWordHandler {

    private static final String[] VERB_ABBR = { "isn't", "aren't", "wasn't", "weren't", "hasn't", "haven't", "won't", "wouldn't", "shan't", "shouldn't", "don't", "didn't", "oughtn't", "daren't", "usedn't", "can't", "couldn't", "mightn't", "mustn't" };

    private static final String[] VERB_ABBR_UNFOLDING = { "is not", "are not", "was not", "were not", "has not", "have not", "will not", "would not", "shall not", "should not", "do not", "did not", "ought not", "dare not", "used not to", "can not", "could not", "might not", "must not" };

    private boolean timeToConst;

    private boolean numToConst;

    private boolean[] isMark;

    private boolean[] isBadMark;

    private HashMap<String, String[]> verbAbbrMap;

    /**
	 * 
	 * @param timeToConst
	 *            whether turn all time format to Constant.TIME_FEATURE
	 * @param numToConst
	 *            whether turn all number format to Constant.NUM_FEATURE
	 */
    public BadWordHandler(boolean timeToConst, boolean numToConst) {
        this.isMark = new boolean[256];
        Arrays.fill(isMark, true);
        for (int i = '0'; i <= '9'; i++) {
            isMark[i] = false;
        }
        for (int i = 'a'; i <= 'z'; i++) {
            isMark[i] = false;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            isMark[i] = false;
        }
        this.isBadMark = Arrays.copyOf(isMark, 256);
        isBadMark['&'] = false;
        isBadMark['\''] = false;
        isBadMark['-'] = false;
        isBadMark['.'] = false;
        isBadMark[':'] = false;
        isBadMark['@'] = false;
        isBadMark['_'] = false;
        this.verbAbbrMap = new HashMap<String, String[]>(32);
        for (int i = 0; i < VERB_ABBR.length; i++) {
            String[] value = VERB_ABBR_UNFOLDING[i].split(" ");
            this.verbAbbrMap.put(VERB_ABBR[i], value);
        }
        this.timeToConst = timeToConst;
        this.numToConst = numToConst;
    }

    /**
	 * invoked by "process" to process the word in "str" recursively.
	 * after the processing, email-address-like string will be reserved,
	 * while urls will be separated into multiple parts according to '\'
	 * and other punctuation.
	 * time-like format extraction is still a problem.
	 * 
	 * @param str
	 *            the char sequence containing the word to be processed
	 * @param l
	 *            the start index of the word in "str"
	 * @param r
	 *            the end index (inclusive) of the word in "str"
	 * @param vs
	 *            the result vector to which the result should be added
	 */
    private void dfsProcess(char[] str, int l, int r, Vector<String> vs) {
        String[] value = this.verbAbbrMap.get(String.copyValueOf(str, l, r - l + 1).toLowerCase());
        if (value != null) {
            for (int i = 0; i < value.length; i++) {
                if (value[i].length() > 0) vs.add(value[i]);
            }
            return;
        }
        int id = l;
        while (true) {
            while (id <= r && !isMark[str[id]]) id++;
            if (id > r) {
                break;
            }
            if (!isMark[str[id + 1]]) {
                id++;
                continue;
            }
            dfsProcess(str, l, id - 1, vs);
            while (isMark[str[id]]) id++;
            l = id;
        }
        for (int i = r - 1; i > l; i--) {
            if (str[i] != '.' && str[i] != ',') continue;
            if (Character.isDigit(str[i - 1]) && Character.isDigit(str[i + 1])) {
                for (int j = i; j < r; j++) str[j] = str[j + 1];
                r--;
            }
        }
        int len = r - l + 1;
        while (len > 2 && (str[r - 1] == '\'' || str[r - 1] == '"') && (str[r] == 's' || str[r] == 'S' || str[r] == 't' || str[r] == 'd')) {
            r -= 2;
            len -= 2;
        }
        while (len > 3 && (str[r - 2] == '\'' || str[r - 2] == '"') && str[r - 1] == 'l' && str[r] == 'l') {
            r -= 3;
            len -= 3;
        }
        int letterOrDigitRequired = 3;
        id = l + 1;
        while (true) {
            for (; id < r && str[id] != '-'; id++) ;
            if (id >= r) break;
            int isLetter = 2;
            for (int i = id - 1; i >= id - letterOrDigitRequired; i--) {
                if (i < l || !Character.isLetterOrDigit(str[i])) {
                    isLetter--;
                    break;
                }
            }
            for (int i = id + 1; i <= id + letterOrDigitRequired; i++) {
                if (i > r || !Character.isLetterOrDigit(str[i])) {
                    isLetter--;
                    break;
                }
            }
            if (isLetter > 0) {
                dfsProcess(str, l, id - 1, vs);
                l = id + 1;
            }
            id += 1;
        }
        letterOrDigitRequired = 3;
        id = l + 1;
        while (true) {
            for (; id < r && str[id] != '.' && str[id] != ':' && str[id] != '\''; id++) ;
            if (id >= r) break;
            boolean isLetter = true;
            for (int i = id - 1; i >= id - letterOrDigitRequired; i--) {
                if (i < l || !Character.isLetterOrDigit(str[i])) {
                    isLetter = false;
                    break;
                }
            }
            for (int i = id + 1; i <= id + letterOrDigitRequired; i++) {
                if (i > r || !Character.isLetterOrDigit(str[i])) {
                    isLetter = false;
                    break;
                }
            }
            if (isLetter) {
                dfsProcess(str, l, id - 1, vs);
                l = id + 1;
            }
            id += 1;
        }
        if (this.timeToConst) {
            if (r - l + 1 == 5) {
                if (str[l + 2] == ':' && Character.isDigit(str[l]) && Character.isDigit(str[l + 1]) && Character.isDigit(str[l + 3]) && Character.isDigit(str[l + 4])) {
                    boolean valid = true;
                    if (str[l] > '2') valid = false;
                    if (str[l] == '2' && str[l + 1] > '4') valid = false;
                    if (str[l + 3] > '5') valid = false;
                    if (valid) {
                        vs.add(Constant.TIME_FEATURE);
                        return;
                    }
                }
            }
        }
        len = r - l + 1;
        id = l;
        while (true) {
            for (; id <= r && !this.isBadMark[str[id]]; id++) ;
            if (id > r) break;
            if (str[id] == '/' && len <= 4) {
                id++;
                continue;
            }
            dfsProcess(str, l, id - 1, vs);
            id = l = id + 1;
            len = r - l + 1;
        }
        if (this.numToConst) {
            for (id = l; id <= r; id++) {
                if (!Character.isDigit(str[id]) && !this.isMark[str[id]]) break;
            }
            if (id > r) {
                vs.add(Constant.NUMBER_FEATURE);
                return;
            }
        }
        vs.add(String.copyValueOf(str, l, r - l + 1));
    }

    /**
	 * given a word (may be a bad word), process it according to some syntax
	 * rule, and add the result to a vector
	 * 
	 * @param word
	 *            the given word
	 * @param vs
	 *            the result vector
	 */
    public void process(String word, Vector<String> vs) {
        char[] str = word.toCharArray();
        int l, r, len = str.length;
        for (int i = 0; i < len; i++) {
            if ((byte) (str[i]) < (byte) 0) str[i] = '?';
        }
        for (l = 0; l < len && isMark[str[l]]; l++) ;
        for (r = len - 1; r >= 0 && isMark[str[r]]; r--) ;
        if (l > r) return;
        dfsProcess(str, l, r, vs);
    }
}
