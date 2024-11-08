package bcry;

import java.util.*;
import java.util.List.*;

class bcGenerator {

    private final String GRAMMAR_START = "START";

    private final String GRAMMAR_TITLE = "TITLE";

    private Random r = new Random();

    private int currentSyllables = 0;

    private String rSeg = "";

    private bcModule module;

    private bcVoice voice;

    public bcGenerator(bcModule m, bcVoice v) {
        voice = v;
        module = m;
    }

    private String parseGrammar(String segment) {
        String output = "";
        try {
            String[] temp;
            String seg = replaceBrackets(module.getGrammar().getProperty(segment, segment));
            if (seg.indexOf(";") == -1) {
                temp = new String[1];
                temp[0] = seg.substring(1, seg.length() - 1);
            } else {
                temp = seg.substring(1, seg.length() - 1).split(";");
            }
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].indexOf("#") != -1) {
                    String[] temp2 = temp[i].split("#");
                    temp[i] = temp2[r.nextInt(temp2.length)];
                }
                if ((temp[i].startsWith("(")) && (temp[i].endsWith(")"))) {
                    temp[i] = temp[i].replace('&', ';');
                    temp[i] = temp[i].replace('|', '#');
                    temp[i] = "{" + temp[i].substring(1, temp[i].length() - 1) + "}";
                    output += parseGrammar(temp[i]);
                    if (i < temp.length - 1) {
                        output += "@";
                    }
                } else if ((temp[i].startsWith("*(")) && (temp[i].endsWith(")"))) {
                    if (r.nextInt(2) == 1) {
                        temp[i] = temp[i].replace('&', ';');
                        temp[i] = temp[i].replace('|', '#');
                        temp[i] = "{" + temp[i].substring(2, temp[i].length() - 1) + "}";
                        output += parseGrammar(temp[i]);
                        if (i < temp.length - 1) {
                            output += "@";
                        }
                    }
                } else if ((temp[i].startsWith("'")) && (temp[i].endsWith("'"))) {
                    if (isWord(temp[i].substring(1, temp[i].length() - 1))) {
                        currentSyllables += getWordFromLists(temp[i].substring(1, temp[i].length() - 1)).getSyllables();
                    }
                    output += temp[i];
                    if (i < temp.length - 1) {
                        output += "@";
                    }
                } else if ((temp[i].startsWith("*'")) && (temp[i].endsWith("'"))) {
                    if (r.nextInt(2) == 1) {
                        if (isWord(temp[i].substring(2, temp[i].length() - 1))) {
                            currentSyllables += getWordFromLists(temp[i].substring(2, temp[i].length() - 1)).getSyllables();
                        }
                        output += temp[i].substring(1, temp[i].length());
                        if (i < temp.length - 1) {
                            output += "@";
                        }
                    }
                } else if ((temp[i].startsWith("[")) && (temp[i].endsWith("]"))) {
                    output += parseGrammar(temp[i].substring(1, temp[i].length() - 1));
                    if (i < temp.length - 1) {
                        output += "@";
                    }
                } else if ((temp[i].startsWith("*[")) && (temp[i].endsWith("]"))) {
                    if (r.nextInt(2) == 1) {
                        output += parseGrammar(temp[i].substring(2, temp[i].length() - 1));
                        if (i < temp.length - 1) {
                            output += "@";
                        }
                    }
                } else if (temp[i].startsWith("*")) {
                    if (r.nextInt(2) == 1) {
                        output += temp[i].substring(1, temp[i].length());
                        if (i < temp.length - 1) {
                            output += "@";
                        }
                    }
                } else {
                    output += temp[i].substring(0, temp[i].length());
                    if (i < temp.length - 1) {
                        output += "@";
                    }
                }
            }
        } catch (Exception e) {
            voice.sysout("Error: Missing or illegal expression in grammar.dat - Segment:" + segment);
            voice.sysout(e.toString());
        }
        if (output.endsWith("@")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    private void parseLayout() {
        String[] temp;
        String[] chorus = null;
        int lines;
        for (int i = 0; i < module.getLayout().size(); i++) {
            if (!(((String) module.getLayout().get(i)).indexOf(":") == -1)) {
                temp = ((String) module.getLayout().get(i)).split(":");
                if (temp[0].equals("lines")) {
                    lines = Integer.parseInt(temp[1]);
                    voice.sing(makeLines(lines));
                } else if (temp[0].equals("verse")) {
                    voice.sing(makeChorus(temp[1]));
                } else if (temp[0].equals("chorus")) {
                    voice.sing("[Chorus]");
                    if (chorus == null) {
                        chorus = makeChorus(temp[1]);
                    }
                    voice.sing(chorus);
                } else if (temp[0].equals("bridge")) {
                    lines = Integer.parseInt(temp[1]);
                    voice.sing(makeBridge(lines));
                }
            } else {
                voice.sing(fill(parseGrammar((String) module.getLayout().get(i))));
            }
        }
    }

    private String[] xLiner(int number) {
        if (number <= 2) {
            number = 2;
        } else {
            number = 3;
        }
        String[] result = new String[number];
        String[] lastSegs = new String[number - 1];
        String lastWord = "";
        String tempWord = "";
        int[] tempSyls = new int[number];
        int tempSyl = 0;
        boolean match;
        bcWordList lst = null;
        bcWord current = null;
        for (int l = 0; l <= number - 2; l++) {
            do {
                match = true;
                if (l == 0) {
                    currentSyllables = 0;
                    result[0] = fill(parseGrammar(GRAMMAR_START));
                    lastWord = getLastWord(result[0]);
                    tempSyls[0] = currentSyllables;
                    currentSyllables = 0;
                } else {
                    lastWord = getLastWord(result[1]);
                }
                result[l + 1] = parseGrammar(GRAMMAR_START);
                if (result[l + 1].indexOf("@") == -1) {
                    lastSegs[l] = result[l + 1];
                    result[l + 1] = "";
                    tempSyls[l + 1] = currentSyllables = 0;
                } else {
                    for (int j = result[l + 1].length() - 1; j >= 0; j--) {
                        if (result[l + 1].charAt(j) == '@') {
                            lastSegs[l] = result[l + 1].substring(j + 1);
                            result[l + 1] = fill(result[l + 1].substring(0, j));
                            tempSyls[l + 1] = currentSyllables;
                            currentSyllables = 0;
                            break;
                        }
                    }
                }
                if (lastSegs[l].startsWith("'") && lastSegs[l].endsWith("'")) {
                    tempWord = lastSegs[l].substring(1, lastSegs[l].length() - 1);
                    tempSyl = getWordFromLists(tempWord).getSyllables();
                    if ((rhyming(tempWord, lastWord) < 1) || !(tempSyl + tempSyls[l + 1] <= tempSyls[0] + module.getSyllableTolerance()) || !(tempSyl + tempSyls[l + 1] >= tempSyls[0] - module.getSyllableTolerance())) {
                        match = false;
                    } else {
                        result[l + 1] += " " + tempWord;
                        tempSyls[l + 1] += tempSyl;
                    }
                } else {
                    lst = module.getWordList(lastSegs[l]);
                    List possibleMatches = new LinkedList();
                    for (int j = 0; j < lst.getNumberOfWords(); j++) {
                        current = lst.getItem(j);
                        tempWord = current.getWord();
                        if ((rhyming(tempWord, lastWord) == 1) && (current.getSyllables() + tempSyls[l + 1] <= tempSyls[0] + module.getSyllableTolerance()) && (current.getSyllables() + tempSyls[l + 1] >= tempSyls[0] - module.getSyllableTolerance())) {
                            possibleMatches.add(current);
                        }
                    }
                    if (possibleMatches.size() == 0) {
                        match = false;
                    } else {
                        current = (bcWord) possibleMatches.get(r.nextInt(possibleMatches.size()));
                        tempWord = current.getWord();
                        result[l + 1] += " " + tempWord;
                        tempSyls[l + 1] += current.getSyllables();
                    }
                }
                if ((module.getSyllableTolerance() == 0) && (!result[l + 1].equals("")) && (!metricMatch(result[0], result[l + 1]))) {
                    match = false;
                }
            } while (!match);
        }
        return result;
    }

    private String[] makeBridge(int lines) {
        String[] result = new String[lines];
        String[] temp;
        if (lines % 2 != 0) {
            lines += 1;
        }
        int i;
        for (i = 0; i < lines; i++) {
            temp = xLiner(2);
            result[i] = temp[0];
            i++;
            result[i] = temp[1];
        }
        return result;
    }

    private String[] makeChorus(String scheme) {
        String[] result = new String[scheme.length()];
        String[][] schemes = new String[5][3];
        scheme = scheme.toLowerCase();
        int[] count = new int[5];
        boolean rhyming;
        for (int i = 0; i < 5; i++) {
            schemes[i] = new String[3];
            for (int j = 0; j < 3; j++) {
                schemes[i][j] = "";
            }
            count[i] = 0;
        }
        for (int i = 0; i < scheme.length(); i++) {
            if (scheme.substring(i).startsWith("a")) {
                count[0]++;
            } else if (scheme.substring(i).startsWith("b")) {
                count[1]++;
            } else if (scheme.substring(i).startsWith("c")) {
                count[2]++;
            } else if (scheme.substring(i).startsWith("d")) {
                count[3]++;
            } else if (scheme.substring(i).startsWith("e")) {
                count[4]++;
            }
        }
        do {
            rhyming = false;
            for (int i = 0; i < 5; i++) {
                if (count[i] == 1) {
                    schemes[i][0] = fill(parseGrammar(GRAMMAR_START));
                } else if (count[i] >= 2) {
                    schemes[i] = xLiner(count[i]);
                }
                for (int j = 0; j <= i; j++) {
                    if ((i != j) && ((rhyming(getLastWord(schemes[i][0]), schemes[j][0]) == 1) || (rhyming(getLastWord(schemes[i][0]), schemes[j][0]) == -2))) {
                        rhyming = true;
                    }
                }
            }
            voice.progressBarDot(".");
        } while (rhyming);
        for (int i = 0; i < 5; i++) {
            count[i] = 0;
        }
        for (int i = 0; i < scheme.length(); i++) {
            if (scheme.substring(i).startsWith("a")) {
                result[i] = schemes[0][count[0]];
                count[0]++;
            } else if (scheme.substring(i).startsWith("b")) {
                result[i] = schemes[1][count[1]];
                count[1]++;
            } else if (scheme.substring(i).startsWith("c")) {
                result[i] = schemes[2][count[2]];
                count[2]++;
            } else if (scheme.substring(i).startsWith("d")) {
                result[i] = schemes[3][count[3]];
                count[3]++;
            } else if (scheme.substring(i).startsWith("e")) {
                result[i] = schemes[4][count[4]];
                count[4]++;
            }
        }
        return result;
    }

    private String[] makeLines(int lines) {
        String[] result = new String[lines];
        for (int i = 0; i < lines; i++) {
            currentSyllables = 0;
            result[i] = fill(parseGrammar(GRAMMAR_START));
        }
        return result;
    }

    private String fill(String seg) {
        String result = "";
        if (seg.indexOf("@") == -1) {
            if (seg.startsWith("'") && seg.endsWith("'")) {
                result = seg.substring(1, seg.length() - 1);
            } else {
                result = getRandomWord(seg);
            }
        } else {
            String[] segs = seg.split("@");
            for (int i = 0; i < segs.length; i++) {
                if (segs[i].startsWith("'") && segs[i].endsWith("'")) {
                    result += segs[i].substring(1, segs[i].length() - 1);
                } else {
                    if (!segs[i].equals("")) {
                        result += getRandomWord(segs[i]);
                    }
                }
                if (i < segs.length - 1) {
                    result += " ";
                }
            }
        }
        return result;
    }

    private String getRandomWord(String listName) {
        String result = "[LIST NOT FOUND]";
        for (int i = 0; i < module.getWordLists().size(); i++) {
            if (((bcWordList) module.getWordLists().get(i)).getFileName().equals(listName)) {
                result = ((bcWordList) module.getWordLists().get(i)).getRandomItem().getWord();
                currentSyllables += getWordFromLists(result).getSyllables();
                break;
            }
        }
        if (result.equals("[LIST NOT FOUND]")) {
            voice.sysout("Error: Word list '" + listName + "' not found, check data files!");
        }
        return result;
    }

    private bcWord getWordFromLists(String word) {
        bcWord result = null;
        for (int i = 0; i < module.getWordLists().size(); i++) {
            if (((bcWordList) module.getWordLists().get(i)).getItem(word) != null) {
                result = ((bcWordList) module.getWordLists().get(i)).getItem(word);
                break;
            }
        }
        if (result == null) voice.sysout("Warning: Word not listed: '" + word + "'");
        return result;
    }

    private String getLastWord(String line) {
        String result = line;
        if (line.indexOf(" ") > 0) {
            String[] temp = line.split(" ");
            int i = 0;
            do {
                i++;
                result = temp[temp.length - i];
            } while (((result.equals("!")) || (result.equals(""))) && (i >= temp.length));
        }
        return result;
    }

    private int rhyming(String lineA, String lineB) {
        int result = 0;
        lineA = getLastWord(lineA.toLowerCase());
        lineB = getLastWord(lineB.toLowerCase());
        if ((lineA.equals("")) || (lineB.equals(""))) {
            result = -1;
        } else if (lineA.equals(lineB)) {
            result = -2;
        } else if (rhymingAux(lineA, lineB)) {
            result = 1;
        } else {
            result = 0;
        }
        return result;
    }

    private boolean rhymingAux(String wordA, String wordB) {
        boolean last = false;
        bcWord A = getWordFromLists(wordA);
        bcWord B = getWordFromLists(wordB);
        if ((A.getSyllables() == 1) || (B.getSyllables() == 1)) {
            last = true;
        }
        wordA = A.getRhymeKey(last).replaceAll("AE", "EH");
        wordB = B.getRhymeKey(last).replaceAll("AE", "EH");
        return wordA.equals(wordB);
    }

    private boolean metricMatch(String lineA, String lineB) {
        boolean result = true;
        int count = 0;
        char A;
        char B;
        lineA = metricCode(lineA);
        lineB = metricCode(lineB);
        if (lineA.length() == lineB.length()) {
            for (int i = 0; i < lineA.length(); i++) {
                A = lineA.charAt(i);
                B = lineB.charAt(i);
                if (((A == '0') && (B == '1')) || ((A == '1') && (B == '0'))) {
                    result = false;
                } else {
                    if (((A == 'O') && ((B == '1') || (B == 'I'))) || ((A == 'I') && ((B == '0') || (B == 'O'))) || ((A == '0') && (B == 'I')) || ((A == '1') && (B == 'O'))) {
                        count++;
                    }
                }
            }
            if (count > module.getMetricTolerance()) result = false;
        } else {
            result = false;
        }
        return result;
    }

    private String metricCode(String line) {
        String metrum = "";
        if (line.indexOf(" ") == -1) {
            metrum += getWordFromLists(line).getMetricCode();
        } else {
            String[] temp = line.split(" ");
            for (int j = 0; j < temp.length; j++) {
                metrum += getWordFromLists(temp[j]).getMetricCode();
            }
        }
        return metrum;
    }

    private String replaceBrackets(String segment) {
        if ((segment.indexOf("(") != -1) && (segment.indexOf(")") != -1)) {
            String brk1 = segment.substring(0, segment.indexOf("("));
            String brk2 = segment.substring(segment.indexOf("("), segment.indexOf(")"));
            String brk3 = segment.substring(segment.indexOf(")") + 1, segment.length());
            brk2 = brk2.replace(';', '&');
            brk2 = brk2.replace('#', '|');
            segment = brk1 + brk2 + ")" + replaceBrackets(brk3);
        }
        return segment;
    }

    private boolean isWord(String word) {
        return ((word.indexOf(",") == -1) && (word.indexOf(";") == -1) && (word.indexOf(".") == -1) && (word.indexOf("(") == -1) && (word.indexOf(")") == -1) && (word.indexOf("'") == -1) && (word.indexOf("*") == -1));
    }

    public List getLyrics() {
        if ((module != null) && (module.isInitialized())) {
            voice.progressBarStart("Generating, please wait...");
            voice.resetLyrics();
            voice.verboseLineFeed();
            voice.sing("*******************************************");
            voice.sing("*             " + fill(parseGrammar(GRAMMAR_TITLE)) + "             *");
            voice.sing("*******************************************");
            parseLayout();
            voice.sing("*******************************************");
            voice.progressBarEnd("done.");
        } else {
            voice.sysout("Error: Battlecry has not been initialized.");
        }
        return voice.getLyrics();
    }

    public void getModuleInfo() {
        if ((module != null) && (module.isInitialized())) {
            voice.sysout("Module Name: " + module.getInfo().getProperty("NAME"));
            voice.sysout("Author: " + module.getInfo().getProperty("AUTHOR"));
            voice.sysout("Version: " + module.getInfo().getProperty("VERSION"));
            voice.sysout("Comment: " + module.getInfo().getProperty("COMMENT"));
        } else {
            voice.sysout("Error: Battlecry has not been initialized.");
        }
    }
}
