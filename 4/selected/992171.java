package org.omegat.filters2.po;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.Instance;
import org.omegat.filters2.TranslationException;
import org.omegat.util.OStrings;

/**
 * Filter to support po files (in various encodings).
 * 
 * Format described on
 * http://www.gnu.org/software/hello/manual/gettext/PO-Files.html
 * 
 * Filter is not thread-safe !
 * 
 * @author Keith Godfrey
 * @author Maxym Mykhalchuk
 * @author Thomas Huriaux
 * @author Martin Fleurke
 * @author Alex Buloichik (alex73mail@gmail.com)
 */
public class PoFilter extends AbstractFilter {

    protected static Pattern COMMENT_FUZZY = Pattern.compile("#, fuzzy");

    protected static Pattern COMMENT_FUZZY_OTHER = Pattern.compile("#,.* fuzzy.*");

    protected static Pattern COMMENT_NOWRAP = Pattern.compile("#,.* no-wrap.*");

    protected static Pattern MSG_ID = Pattern.compile("msgid(_plural)?\\s+\"(.*)\"");

    protected static Pattern MSG_STR = Pattern.compile("msgstr(\\[[0-9]+\\])?\\s+\"(.*)\"");

    protected static Pattern MSG_CTX = Pattern.compile("msgctxt\\s+\"(.*)\"");

    protected static Pattern MSG_OTHER = Pattern.compile("\"(.*)\"");

    enum MODE {

        MSGID, MSGSTR, MSGID_PLURAL, MSGSTR_PLURAL, MSGCTX
    }

    ;

    private StringBuilder[] sources, targets;

    private boolean nowrap, fuzzy;

    private BufferedWriter out;

    public String getFileFormatName() {
        return OStrings.getString("POFILTER_FILTER_NAME");
    }

    public Instance[] getDefaultInstances() {
        return new Instance[] { new Instance("*.po"), new Instance("*.pot") };
    }

    public boolean isSourceEncodingVariable() {
        return true;
    }

    public boolean isTargetEncodingVariable() {
        return true;
    }

    public String getFuzzyMark() {
        return "PO-fuzzy";
    }

    public void processFile(File inFile, String inEncoding, File outFile, String outEncoding) throws IOException, TranslationException {
        BufferedReader reader = createReader(inFile, inEncoding);
        try {
            BufferedWriter writer;
            if (outFile != null) {
                writer = createWriter(outFile, outEncoding);
            } else {
                writer = null;
            }
            try {
                processFile(reader, writer);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } finally {
            reader.close();
        }
    }

    @Override
    protected void alignFile(BufferedReader sourceFile, BufferedReader translatedFile) throws Exception {
        translatedFile.mark(1);
        int ch = translatedFile.read();
        if (ch != 0xFEFF) translatedFile.reset();
        this.out = null;
        processPoFile(translatedFile);
    }

    public void processFile(BufferedReader in, BufferedWriter out) throws IOException {
        in.mark(1);
        int ch = in.read();
        if (ch != 0xFEFF) in.reset();
        this.out = out;
        processPoFile(in);
    }

    private void processPoFile(BufferedReader in) throws IOException {
        fuzzy = false;
        nowrap = false;
        MODE currentMode = null;
        int currentPlural = 0;
        sources = new StringBuilder[2];
        sources[0] = new StringBuilder();
        sources[1] = new StringBuilder();
        targets = new StringBuilder[2];
        targets[0] = new StringBuilder();
        targets[1] = new StringBuilder();
        String s;
        while ((s = in.readLine()) != null) {
            if (COMMENT_FUZZY.matcher(s).matches()) {
                fuzzy = true;
                flushTranslation(currentMode);
                continue;
            } else if (COMMENT_FUZZY_OTHER.matcher(s).matches()) {
                fuzzy = true;
                flushTranslation(currentMode);
                s = s.replaceAll("(.*), fuzzy(.*)", "$1$2");
            }
            if (COMMENT_NOWRAP.matcher(s).matches()) {
                flushTranslation(currentMode);
                nowrap = true;
                eol(s);
                continue;
            }
            Matcher m;
            if ((m = MSG_ID.matcher(s)).matches()) {
                String text = m.group(2);
                if (m.group(1) == null) {
                    currentMode = MODE.MSGID;
                    sources[0].append(text);
                } else {
                    currentMode = MODE.MSGID_PLURAL;
                    sources[1].append(text);
                }
                eol(s);
                continue;
            }
            if ((m = MSG_STR.matcher(s)).matches()) {
                String text = m.group(2);
                if (m.group(1) == null) {
                    currentMode = MODE.MSGSTR;
                    targets[0].append(text);
                } else {
                    currentMode = MODE.MSGSTR_PLURAL;
                    if ("[0]".equals(m.group(1))) {
                        targets[0].append(text);
                        currentPlural = 0;
                    } else if ("[1]".equals(m.group(1))) {
                        targets[1].append(text);
                        currentPlural = 1;
                    }
                }
                continue;
            }
            if ((m = MSG_CTX.matcher(s)).matches()) {
                currentMode = MODE.MSGCTX;
                eol(s);
                continue;
            }
            if ((m = MSG_OTHER.matcher(s)).matches()) {
                String text = m.group(1);
                if (currentMode == null) {
                    throw new IOException("Invalid file format");
                }
                switch(currentMode) {
                    case MSGID:
                        sources[0].append(text);
                        eol(s);
                        break;
                    case MSGID_PLURAL:
                        sources[1].append(text);
                        eol(s);
                        break;
                    case MSGSTR:
                        targets[0].append(text);
                        break;
                    case MSGSTR_PLURAL:
                        targets[currentPlural].append(text);
                        break;
                    case MSGCTX:
                        eol(s);
                        break;
                }
                continue;
            }
            flushTranslation(currentMode);
            eol(s);
        }
        flushTranslation(currentMode);
    }

    protected void eol(String s) throws IOException {
        if (out != null) {
            out.write(s);
            out.write('\n');
        }
    }

    protected void align(int pair) {
        String s = unescape(sources[pair].toString());
        String t = unescape(targets[pair].toString());
        align(s, t);
    }

    protected void align(String source, String translation) {
        if (translation.length() == 0) {
            translation = null;
        }
        if (entryParseCallback != null) {
            entryParseCallback.addEntry(null, source, translation, fuzzy, null, this);
        } else if (entryAlignCallback != null) {
            entryAlignCallback.addTranslation(null, source, translation, fuzzy, null, this);
        }
    }

    protected void alignHeader(String header) {
        if (entryParseCallback != null) {
            entryParseCallback.addEntry(null, unescape(header), null, false, null, this);
        }
    }

    protected void flushTranslation(MODE currentMode) throws IOException {
        if (sources[0].length() == 0) {
            if (targets[0].length() == 0) {
                return;
            } else {
                if (out != null) {
                    out.write("msgstr " + getTranslation(targets[0]) + "\n");
                } else {
                    alignHeader(targets[0].toString());
                }
            }
            fuzzy = false;
        } else {
            if (sources[1].length() == 0) {
                if (out != null) {
                    out.write("msgstr " + getTranslation(sources[0]) + "\n");
                } else {
                    align(0);
                }
            } else {
                if (out != null) {
                    out.write("msgstr[0] " + getTranslation(sources[0]) + "\n");
                    out.write("msgstr[1] " + getTranslation(sources[1]) + "\n");
                } else {
                    align(0);
                    align(1);
                }
            }
            fuzzy = false;
        }
        sources[0].setLength(0);
        sources[1].setLength(0);
        targets[0].setLength(0);
        targets[1].setLength(0);
    }

    protected static final Pattern R1 = Pattern.compile("(?<!\\\\)((\\\\\\\\)*)\\\\\"");

    protected static final Pattern R2 = Pattern.compile("(?<!\\\\)((\\\\\\\\)*)\\\\n");

    protected static final Pattern R3 = Pattern.compile("(?<!\\\\)((\\\\\\\\)*)\\\\t");

    protected static final Pattern R4 = Pattern.compile("^\\\\n");

    /**
     * Private processEntry to do pre- and postprocessing.<br>
     * The given entry is interpreted to a string (e.g. escaped quotes are
     * unescaped, '\n' is translated into newline character, '\t' into tab
     * character.) then translated and then returned as a PO-string-notation
     * (e.g. double quotes escaped, newline characters represented as '\n' and
     * surrounded by double quotes, possibly split up over multiple lines)<Br>
     * Long translations are not split up over multiple lines as some PO editors
     * do, but when there are newline characters in a translation, it is split
     * up at the newline markers.<Br>
     * If the nowrap parameter is true, a translation that exists of multiple
     * lines starts with an empty string-line to left-align all lines. [With
     * nowrap set to true, long lines are also never wrapped (except for at
     * newline characters), but that was already not done without nowrap.] [
     * 1869069 ] Escape support for PO
     * 
     * @param entry
     *            The entire source text, without it's surrounding double
     *            quotes, but otherwise not-interpreted
     * @param nowrap
     *            gives indication if the translation should not be wrapped over
     *            multiple lines and all lines be left-aligned.
     * @return The translated entry, within double quotes on each line (thus
     *         ready to be printed to target file immediately)
     **/
    private String getTranslation(StringBuilder en) {
        String entry = unescape(en.toString());
        String translation = entryTranslateCallback.getTranslation(null, entry);
        if (translation != null) {
            return "\"" + escape(translation) + "\"";
        } else {
            return "\"\"";
        }
    }

    /**
     * Unescape text from .po format.
     */
    private String unescape(String entry) {
        entry = R1.matcher(entry).replaceAll("$1\"");
        entry = R2.matcher(entry).replaceAll("$1\n");
        entry = R3.matcher(entry).replaceAll("$1\t");
        entry = R4.matcher(entry).replaceAll("\\\n");
        entry = entry.replace("\\\\", "\\");
        return entry;
    }

    /**
     * Escape text to .po format.
     */
    private String escape(String translation) {
        translation = translation.replace("\\", "\\\\");
        translation = translation.replace("\"", "\\\"");
        translation = translation.replace("\\\\r", "\\r");
        translation = translation.replace("\n", "\\n\"\n\"");
        if (translation.endsWith("\"\n\"")) {
            translation = translation.substring(0, translation.length() - 3);
        }
        if (nowrap && translation.contains("\n")) {
            translation = "\"\n\"" + translation;
        }
        translation = translation.replace("\t", "\\t");
        return translation;
    }
}
