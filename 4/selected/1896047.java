package sushmu.sted.fontmap;

import sushmu.sted.Main;
import sushmu.sted.ui.FilePanel;
import sushmu.sted.util.Constants;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class Converter extends Thread {

    private boolean reverseTransliterate = false;

    private boolean isHTMLAware = true;

    private boolean isParseMode = true;

    private int converted = 0;

    private String prevWord = null;

    private FontMap fontMap = null;

    private boolean stopRequested = false;

    private JProgressBar progressBar = null;

    private FilePanel filePanel = null;

    public Converter(final FilePanel filePanel) {
        super();
        this.filePanel = filePanel;
        this.fontMap = filePanel.getFontMap();
        this.progressBar = filePanel.getStatusPanel().getProgressBar();
    }

    public void run() {
        stopRequested = false;
        Main.busy();
        convertFile();
        Main.relax();
    }

    boolean isReady() {
        return fontMap != null && !fontMap.getEntries().isEmpty();
    }

    FilePanel getFilePanel() {
        return filePanel;
    }

    public void setHTMLAware(final boolean flag) {
        isHTMLAware = flag;
    }

    private void convertFile() {
        final File fileToConvert = filePanel.getInputFile();
        final File convertedFile = filePanel.getOutputFile();
        if (fileToConvert == null || convertedFile == null) {
            Main.showMessage("Select valid files for both input and output");
            return;
        }
        if (fileToConvert.getName().equals(convertedFile.getName())) {
            Main.showMessage("Input and Output files are same.. select different files");
            return;
        }
        final int len = (int) fileToConvert.length();
        progressBar.setMinimum(0);
        progressBar.setMaximum(len);
        progressBar.setValue(0);
        try {
            fileCopy(fileToConvert, fileToConvert.getAbsolutePath() + ".bakup");
        } catch (IOException e) {
            Main.showMessage("Unable to Backup input file");
            return;
        }
        final BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileToConvert));
        } catch (FileNotFoundException e) {
            Main.showMessage("Unable to create reader - file not found");
            return;
        }
        final BufferedWriter bufferedWriter;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(convertedFile));
        } catch (IOException e) {
            Main.showMessage("Unable to create writer for output file");
            return;
        }
        String input;
        try {
            while ((input = bufferedReader.readLine()) != null) {
                if (stopRequested) {
                    break;
                }
                bufferedWriter.write(parseLine(input));
                bufferedWriter.newLine();
                progressBar.setValue(progressBar.getValue() + input.length());
            }
        } catch (IOException e) {
            Main.showMessage("Unable to convert " + e.getMessage());
            return;
        } finally {
            try {
                bufferedReader.close();
                bufferedWriter.close();
            } catch (IOException e) {
                Main.showMessage("Unable to close reader/writer " + e.getMessage());
                return;
            }
        }
        if (!stopRequested) {
            filePanel.readOutputFile();
            progressBar.setValue(progressBar.getMaximum());
            Main.setStatus("Transliterate Done.");
        }
        progressBar.setValue(progressBar.getMinimum());
    }

    String parseLine(final String input) {
        final StringBuffer output = new StringBuffer();
        final StringTokenizer stringTokenizer = new StringTokenizer(input, " ", true);
        while (stringTokenizer.hasMoreTokens()) {
            final String word = stringTokenizer.nextToken();
            final StringTokenizer st = new StringTokenizer(word, Constants.HTML_TAG_START + Constants.HTML_TAG_END + Constants.HTML_TAG_START_ESCAPE + Constants.HTML_TAG_END_ESCAPE + Constants.SPACE, true);
            while (st.hasMoreTokens()) {
                final String currWord = st.nextToken();
                parseWord(currWord, prevWord, output);
                prevWord = currWord;
            }
        }
        return output.toString();
    }

    private void parseWord(final String word, final String prevWord, final StringBuffer output) {
        if (Constants.SPACE.equals(word)) {
            output.append(word);
            return;
        }
        if ((Constants.HTML_TAG_START.equals(word) || Constants.HTML_TAG_START_ESCAPE.equals(word)) && isHTMLAware) {
            isParseMode = false;
            output.append(word);
            return;
        } else if ((Constants.HTML_TAG_END.equals(word) || Constants.HTML_TAG_END_ESCAPE.equals(word)) && isHTMLAware) {
            isParseMode = true;
            if (Constants.HTML_TAG_END_ESCAPE.equals(word) && "lt".equals(prevWord)) {
                isParseMode = false;
            }
            output.append(word);
            return;
        }
        if (!(Constants.HTML_TAG_END.equals(word) || Constants.HTML_TAG_END_ESCAPE.equals(word)) && !isParseMode) {
            output.append(word);
            return;
        }
        converted = 0;
        convertWord(word, output, word, word.length(), Constants.EMPTY_STRING, Constants.EMPTY_STRING, word);
    }

    private void convertWord(final String word, final StringBuffer output, final String chopped, final int wordLen, String translated, String leftover, final String original) {
        if (converted == wordLen) return;
        if (translate(word, output, translated, leftover, original)) {
            converted += word.length();
        } else {
            String remaining = word;
            if (word.length() > 1) {
                remaining = word.substring(0, word.length() - 1);
                leftover = word.substring(word.length() - 1, word.length()) + leftover;
            }
            convertWord(remaining, output, chopped, wordLen, translated, leftover, original);
            translated += remaining;
            final String remaining2 = chopped.substring(remaining.length());
            leftover = Constants.EMPTY_STRING;
            convertWord(remaining2, output, remaining2, wordLen, translated, leftover, original);
        }
    }

    private boolean translate(final String word, final StringBuffer output, final String translated, final String leftover, final String original) {
        final char[] result;
        if (isParseMode) {
            result = translateWord(word, translated, leftover, original);
        } else {
            result = word.toCharArray();
        }
        if (result != null) {
            output.append(result);
            return true;
        }
        return false;
    }

    /**
     * for forward translate looks in the values , for backward translate looks in the keyset
     * 
     * @param word       is the _inputFileSelectorPanel character word
     * @param translated 
     * @param leftover   
     * @param original   
     * 
     * @return char[] the translated character word
     */
    private char[] translateWord(final String word, final String translated, final String leftover, final String original) {
        final String wordToConvert = applyIndirectMappingIfAny(word, translated, leftover, original);
        char[] chars = null;
        if (isWordMapped(wordToConvert)) {
            if (reverseTransliterate) {
                final FontMapEntry entry = fontMap.getEntries().getReverseMapping(wordToConvert);
                chars = (entry == null) ? wordToConvert.toCharArray() : entry.getFrom().toCharArray();
            } else {
                final FontMapEntry entry = fontMap.getEntries().getDirectMapping(wordToConvert);
                chars = (entry == null) ? wordToConvert.toCharArray() : entry.getTo().toCharArray();
            }
        }
        if (chars == null && wordToConvert.length() == 1) {
            chars = new char[] { wordToConvert.charAt(0) };
        }
        return chars;
    }

    private String applyIndirectMappingIfAny(final String word, final String translated, final String leftover, final String template) {
        String result = word;
        List list = fontMap.getEntries().isRuleFound(word);
        if (!list.isEmpty()) {
            final Iterator iterator = list.iterator();
            result = word;
            while (iterator.hasNext()) {
                final FontMapEntry entry = (FontMapEntry) iterator.next();
                result = indirectMap(entry, result, translated, leftover, template);
            }
        } else if (fontMap.getEntries().isInWord1(word)) {
            int len = word.length();
            while (len > 0) {
                final String nword = result.substring(0, len--);
                list = fontMap.getEntries().isRuleFound(nword);
                if (!list.isEmpty()) {
                    final Iterator iterator = list.iterator();
                    result = word;
                    while (iterator.hasNext()) {
                        final FontMapEntry entry = (FontMapEntry) iterator.next();
                        if (shouldBeginsWithIndirectMappingApplied(entry, translated, template) || shouldPrecededByIndirectMappingApplied(entry, translated)) {
                            result = indirectMap(entry, result, translated, leftover, template);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param entry      
     * @param word       
     * @param translated 
     * @param leftover   
     * @param template   
     * 
     * @return 
     */
    private static String indirectMap(final FontMapEntry entry, final String word, final String translated, final String leftover, final String template) {
        String result = word;
        if (shouldBeginsWithIndirectMappingApplied(entry, translated, template)) {
            if (!template.startsWith(entry.getTo())) result = word.replaceFirst(entry.getFrom(), entry.getTo());
        }
        if (entry.isEndsWith() && (translated != null && (template.length() - translated.length() == 1))) {
            if (!template.endsWith(entry.getTo())) result = word.replaceFirst(entry.getFrom(), entry.getTo());
        }
        if (entry.getFollowedBy() != null && entry.getFollowedBy().length() > 0 && leftover.startsWith(entry.getFollowedBy())) {
            if (word.equals(entry.getFrom()) || (word.length() >= entry.getTo().length() && !word.substring(word.indexOf(entry.getFrom()), entry.getTo().length()).equals(entry.getTo()))) result = word.replaceFirst(entry.getFrom(), entry.getTo());
        }
        if (shouldPrecededByIndirectMappingApplied(entry, translated)) {
            if (word.equals(entry.getFrom()) || (word.length() >= entry.getTo().length() && !word.substring(word.indexOf(entry.getFrom()), entry.getTo().length()).equals(entry.getTo()))) result = word.replaceFirst(entry.getFrom(), entry.getTo());
        }
        return result;
    }

    private static boolean shouldBeginsWithIndirectMappingApplied(final FontMapEntry entry, final String translated, final String template) {
        if (entry.isBeginsWith() && template.startsWith(entry.getFrom()) && (translated == null || translated.length() == 0)) {
            return true;
        }
        return false;
    }

    private static boolean shouldPrecededByIndirectMappingApplied(final FontMapEntry entry, final String translated) {
        if (entry.getPrecededBy() != null && entry.getPrecededBy().length() > 0 && (translated != null && translated.endsWith(entry.getPrecededBy()))) {
            return true;
        }
        return false;
    }

    private boolean isWordMapped(final String word) {
        if (reverseTransliterate) {
            return fontMap.getEntries().isInWord2(word);
        }
        return fontMap.getEntries().isInWord1(word);
    }

    private static void fileCopy(final File source, final String dest) throws FileNotFoundException, IOException {
        final File newFile = new File(dest);
        final byte[] buffer = new byte[4096];
        final FileInputStream inputStream = new FileInputStream(source);
        final FileOutputStream outputStream = new FileOutputStream(newFile);
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        inputStream.close();
        outputStream.close();
    }

    public void setReverseTransliterate(final boolean flag) {
        reverseTransliterate = flag;
    }

    public void setFontMap(final FontMap fontMap) {
        this.fontMap = fontMap;
    }

    public synchronized void setStopRequested(final boolean flag) {
        stopRequested = flag;
    }
}
