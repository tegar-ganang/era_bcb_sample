package yaw.core.wizard;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import yaw.core.util.StringUtil;

/**
 * 
 * 
 */
public class WizardFile {

    public static final String CREATEFOLDER = ".createfolder";

    private static final String TEMP_PRAEFIX = "YAW";

    private static final String ID = "$Id:";

    private static final String DOLLAR = "$";

    /**
   * Fehler bei der Dateiverarbeitung.
   * 
   */
    public static class FailedException extends WizardException {

        private static final long serialVersionUID = 1L;

        private String file;

        public FailedException(Throwable e) {
            super(e);
        }

        public FailedException(String msg) {
            super(msg);
        }

        public FailedException(String msg, Throwable e) {
            super(msg, e);
        }

        public FailedException setFile(String file) {
            this.file = file;
            return this;
        }

        public String getFile() {
            return this.file;
        }
    }

    /**
   * 
   */
    public static class Item {

        private final String name;

        private final String header;

        private final String flags;

        private StringBuilder entry;

        public Item(String name, String header, String flags) {
            this.name = name;
            this.header = header;
            this.flags = flags;
        }

        public void addLine(String line, String lineDelimiter) {
            if (this.entry == null) this.entry = new StringBuilder(24); else this.entry.append(lineDelimiter);
            this.entry.append(line);
        }

        public String getHeader() {
            return header;
        }

        public String getFlags() {
            return flags;
        }
    }

    /**
   * 
   */
    public static class Section {

        private final String name;

        private final String text;

        private final Hashtable<String, Item> items = new Hashtable<String, Item>();

        public Section(String name, String text, String lineDelimiter) {
            this.name = name;
            this.text = text;
            splitItems(lineDelimiter);
        }

        public void splitItems(String lineDelimiter) {
            BufferedReader reader = new BufferedReader(new StringReader(this.text));
            String line;
            Item lastItem = null;
            try {
                while ((line = reader.readLine()) != null) {
                    Item item = makeItem(line);
                    if (item != null) {
                        endItem(lastItem);
                        lastItem = item;
                    } else if (lastItem != null) {
                        lastItem.addLine(line, lineDelimiter);
                    }
                }
                endItem(lastItem);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void endItem(Item lastItem) {
            if (lastItem != null) {
                this.items.put(lastItem.name, lastItem);
            }
        }

        private Item makeItem(String line) {
            Item item = null;
            return item;
        }

        public boolean hasItem(String pattern) {
            return StringUtil.isValid(pattern) && (this.text.indexOf(pattern) >= 0 || StringUtil.find(new StringBuilder(this.text), pattern)[0] != -1);
        }

        public boolean hasItem(Pattern patern) {
            Matcher m = patern.matcher(text);
            return m.find();
        }
    }

    /**
   * 
   */
    public static class LineSection {

        private final String begin;

        private final String end;

        private String text;

        public LineSection(String begin, String end) {
            this.begin = begin;
            this.end = end;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getBegin() {
            return this.begin;
        }

        public String getEnd() {
            return this.end;
        }

        public String getText() {
            return this.text;
        }
    }

    private static File tempPath = null;

    private final HashMap<String, Section> userSections = new HashMap<String, Section>();

    private final HashMap<String, LineSection> registeredSections = new HashMap<String, LineSection>();

    private final String charset;

    private final String lineDelimiter;

    private final String fileName;

    private final File fileOrg;

    private File fileTmp;

    private boolean fileCopyed = false;

    public WizardFile(String file, String charset, String lineDelimiter) {
        this(new File(file), charset, lineDelimiter);
    }

    public WizardFile(File file, String charset, String lineDelimiter) {
        this.fileOrg = file;
        this.fileName = file.getName();
        this.fileTmp = createPath(createTempFile(fileName));
        this.charset = charset;
        this.lineDelimiter = lineDelimiter;
    }

    private String createTempFile(String fileName) {
        if (fileName == null || fileName.length() == 0) return null;
        String suffix = null;
        int idx = fileName.lastIndexOf('.');
        if (idx != -1) suffix = fileName.substring(idx);
        if (suffix == null || suffix.length() == 0) suffix = "tmp";
        try {
            return File.createTempFile(TEMP_PRAEFIX, suffix, getTempPath()).getPath();
        } catch (IOException e) {
            throw new FailedException("Failed to create tempfile", e);
        }
    }

    public String getAbsolutePath() {
        return this.fileOrg.getAbsolutePath();
    }

    public void beginTemplate() {
        if (fileOrg.exists()) {
            registerPartSection(ID, DOLLAR);
            saveUserPart();
        }
        this.fileCopyed = true;
    }

    public static File getTempPath() {
        if (tempPath == null || !tempPath.exists()) {
            try {
                tempPath = File.createTempFile(TEMP_PRAEFIX, ".tmp").getParentFile();
                if (!tempPath.exists() && !tempPath.mkdirs()) tempPath = null;
            } catch (Exception e) {
                tempPath = null;
            }
        }
        return tempPath;
    }

    public static void removeTempFiles() {
        File tempPath = getTempPath();
        if (tempPath == null) return;
        String[] files = tempPath.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith(TEMP_PRAEFIX);
            }
        });
        if (files == null) return;
        for (String file : files) {
            try {
                new File(tempPath, file).delete();
            } catch (Exception e) {
            }
        }
    }

    public void endTemplate() {
        endTemplate(null);
    }

    public void endTemplate(Set<String> ignoreSection) {
        setUserPart(ignoreSection);
    }

    public void finish(ISourceFormatter formatter) {
        if (this.fileTmp != null) {
            if (formatter != null) formatter.format(this.fileTmp, this.charset, this.lineDelimiter); else reformatLineDelimiter();
        }
    }

    public String getOldRange(String range) {
        if (!fileOrg.exists()) return null;
        try {
            BufferedReader br = new BufferedReader(createOrgReader());
            StringBuilder sb = new StringBuilder();
            boolean in = false;
            String line = "";
            while ((line = br.readLine()) != null) {
                String lineTrimmed = line.trim();
                if (in) {
                    if (matchTagEnd(lineTrimmed, range)) {
                        br.close();
                        return sb.toString();
                    } else {
                        sb.append(line).append(this.lineDelimiter);
                    }
                } else if (matchTagBegin(lineTrimmed, range)) {
                    in = true;
                }
            }
            br.close();
            if (in) throw new FailedException("Missing section end for // {{" + range + " in file " + getAbsolutePath() + "\n").setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException("getOldRange failed.", e).setFile(getAbsolutePath());
        }
        return null;
    }

    public LineSection hasRegisteredLineSection(String line) {
        Iterator<String> iter = this.registeredSections.keySet().iterator();
        while (iter.hasNext()) {
            String begin = iter.next();
            LineSection section = (LineSection) this.registeredSections.get(begin);
            int beginIndex = line.indexOf(section.getBegin());
            int endIndex = line.indexOf(section.getEnd(), beginIndex + 1);
            if (beginIndex != -1 && endIndex != -1) return section;
        }
        return null;
    }

    public void registerPartSection(String begin, String end) {
        registeredSections.put(begin, new LineSection(begin, end));
    }

    /**
   * Setzt User-Code in eine Map. UserX-Code wird wie folgt deklariert:<br>
   * <code>
   * //{{USER-XYZ<br>
   * Ihr-Code<br>
   * Ihr-Code<br>
   * //}}USER-XYZ<br>
   * </code>
   */
    public void saveUserPart() {
        try {
            BufferedReader br = new BufferedReader(createOrgReader());
            boolean fillMapEntry = false;
            StringBuilder sb = null;
            String actualKey = "";
            String line = "";
            while ((line = br.readLine()) != null) {
                LineSection lineSection = hasRegisteredLineSection(line);
                if (lineSection != null) {
                    lineSection.setText(line);
                } else if (fillMapEntry) {
                    String lineTrimmed = line.trim();
                    if (matchTagEnd(lineTrimmed, "USER" + actualKey)) {
                        userSections.put(actualKey, new Section(actualKey, sb.toString(), this.lineDelimiter));
                        actualKey = "";
                        sb = null;
                        fillMapEntry = false;
                    } else sb.append(line).append(this.lineDelimiter);
                } else {
                    String[] found = isUserSectionBegin(line);
                    if (found != null) {
                        fillMapEntry = true;
                        sb = new StringBuilder();
                        actualKey = found[0];
                        if (found.length == 2) sb.append(this.lineDelimiter).append(found[1]);
                    }
                }
            }
            br.close();
            if (fillMapEntry) throw new FailedException("Missing section end for // {{USER" + actualKey + " in file " + getAbsolutePath() + "\n").setFile(getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
    }

    private static String[] isUserSectionBegin(String line) {
        int keyBegin = line.indexOf("//{{USER");
        if (keyBegin != -1) keyBegin += "//{{USER".length(); else {
            keyBegin = line.indexOf("// {{USER");
            if (keyBegin != -1) keyBegin += "// {{USER".length();
        }
        if (keyBegin != -1) {
            String textAfterTag = null;
            int keyEnd = line.indexOf(" ", keyBegin);
            if (keyEnd == -1) keyEnd = line.length(); else textAfterTag = line.substring(keyEnd);
            String key = line.substring(keyBegin, keyEnd);
            if (textAfterTag != null) return new String[] { key, textAfterTag };
            return new String[] { key };
        } else return null;
    }

    private static boolean matchTagBegin(String line, String tag) {
        return (line.equals("//{{" + tag) || line.equals("// {{" + tag));
    }

    private static boolean matchTagEnd(String line, String tag) {
        return (line.equals("//}}" + tag) || line.equals("// }}" + tag));
    }

    public void setUserPart(Set<String> ignoreSection) {
        List<String> allUserRanges = getAllUserRanges();
        if (allUserRanges == null) return;
        for (String fileRange : allUserRanges) {
            Section userSection = userSections.get(fileRange);
            if (userSection != null) {
                if (ignoreSection == null || !ignoreSection.contains(userSection.name)) {
                    replace("USER" + fileRange, userSection.text);
                }
            }
        }
        for (String lineSectionBegin : this.registeredSections.keySet()) {
            LineSection lineSection = (LineSection) this.registeredSections.get(lineSectionBegin);
            if (lineSection.getText() != null) insert(lineSection);
        }
    }

    public List<String> getAllUserRanges() {
        if (!this.fileOrg.exists()) return null;
        List<String> allRanges = new ArrayList<String>();
        BufferedReader br;
        try {
            br = new BufferedReader(createOrgReader());
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] found = isUserSectionBegin(line);
                if (found != null) allRanges.add(found[0]);
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException ioe) {
            throw new FailedException(ioe).setFile(getAbsolutePath());
        }
        return allRanges;
    }

    public File createPath(String path) {
        File f = null;
        String extension = "";
        if (path.indexOf(".") != -1) extension = path.substring(path.indexOf("."));
        StringTokenizer tok = new StringTokenizer(path, String.valueOf(File.separatorChar));
        String value = "";
        while (tok.hasMoreTokens()) {
            value += File.separatorChar + tok.nextToken();
            File file = new File(value);
            if (!file.exists()) {
                try {
                    if (value.endsWith(extension)) {
                        if (!value.endsWith(CREATEFOLDER)) file.createNewFile();
                        f = file;
                    } else {
                        file.mkdir();
                    }
                } catch (IOException ex) {
                    throw new FailedException(ex).setFile(getAbsolutePath());
                }
            } else if (value.endsWith(extension)) {
                f = file;
            }
        }
        return f;
    }

    private void insert(LineSection lineSection) {
        File fileIn = this.fileOrg;
        if (this.fileCopyed) {
            fileIn = this.fileTmp;
        }
        File fileOut = this.fileTmp;
        boolean isFileExisting = fileIn.exists();
        try {
            if (isFileExisting) {
                BufferedReader br = new BufferedReader(createReader(fileIn));
                String line = "";
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    int beginIndex = line.indexOf(lineSection.getBegin());
                    int endIndex = line.indexOf(lineSection.getEnd(), beginIndex + 1);
                    if (beginIndex != -1 && endIndex != -1) {
                        sb.append(lineSection.getText()).append(this.lineDelimiter);
                    } else {
                        sb.append(line).append(this.lineDelimiter);
                    }
                }
                br.close();
                Writer writer = createWriter(fileOut);
                writer.write(sb.toString());
                writer.close();
                fileCopyed = true;
            }
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
    }

    public Writer createTMPWriter() throws IOException {
        return createWriter(this.fileTmp);
    }

    private Writer createWriter(File file) throws FileNotFoundException, IOException {
        try {
            if (this.charset != null) return new OutputStreamWriter(new FileOutputStream(file), this.charset);
        } catch (UnsupportedEncodingException e) {
        }
        return new FileWriter(file);
    }

    public Reader createOrgReader() throws FileNotFoundException {
        return this.fileOrg == null ? null : createReader(this.fileOrg);
    }

    private Reader createReader(File fileIn) throws FileNotFoundException {
        try {
            if (charset != null) return new InputStreamReader(new FileInputStream(fileIn), charset);
        } catch (UnsupportedEncodingException e) {
        }
        return new FileReader(fileIn);
    }

    private BufferedReader createContentReader() throws FileNotFoundException {
        File fileIn;
        if (this.fileCopyed) fileIn = this.fileTmp; else fileIn = this.fileOrg;
        if (!fileIn.exists()) return null;
        return new BufferedReader(createReader(fileIn));
    }

    /**
   * Der Range wird durch das Value komplett ersetzt.<br>
   * 
   * @param range Name des Ranges
   * @param value Einzufügender Wert
   */
    public void replace(String range, String value) {
        boolean isImportRange = range.matches("USER-IMPORT");
        boolean inRange = false;
        value = prepareValue(isImportRange, value);
        try {
            BufferedReader br = createContentReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                String lineTrimmed = line.trim();
                if (inRange) {
                    if (matchTagEnd(lineTrimmed, range)) {
                        inRange = false;
                        if (value.length() > 0) {
                            sb.append(value);
                        } else if (isImportRange) sb.append(this.lineDelimiter);
                    } else continue;
                } else if (matchTagBegin(lineTrimmed, range)) {
                    inRange = true;
                }
                sb.append(line);
                sb.append(this.lineDelimiter);
            }
            br.close();
            if (inRange) throw new FailedException("Missing section end for // {{" + range + " in file " + getAbsolutePath() + "\n").setFile(getAbsolutePath());
            Writer writer = createTMPWriter();
            writer.write(sb.toString());
            writer.close();
            fileCopyed = true;
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
    }

    /**
   * Eine existierende Range löschen
   * @param range
   * @return
   */
    public boolean removeUserRange(final String range) {
        StringBuilder sb = new StringBuilder();
        String line;
        boolean lineBegin = false;
        boolean found = false;
        try {
            BufferedReader br = createContentReader();
            while ((line = br.readLine()) != null) {
                String lineTrimmed = line.trim();
                if (lineBegin) {
                    if (matchTagEnd(lineTrimmed, range)) {
                        found = true;
                        lineBegin = false;
                    } else {
                        continue;
                    }
                } else if (matchTagBegin(lineTrimmed, range)) {
                    lineBegin = true;
                } else {
                    sb.append(line);
                    sb.append(this.lineDelimiter);
                }
            }
            br.close();
            if (lineBegin) throw new FailedException("Missing section end for // {{" + range + " in file " + getAbsolutePath() + "\n").setFile(getAbsolutePath());
            if (!found) return false;
            Writer writer = createTMPWriter();
            writer.write(sb.toString());
            writer.close();
            fileCopyed = true;
            return true;
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
    }

    private String prepareValue(boolean addnewLine, String value) {
        if (value == null) return "";
        try {
            BufferedReader reader = new BufferedReader(new StringReader(value.trim()));
            StringBuilder buff = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) buff.append(line).append(lineDelimiter);
            reader.close();
            if (addnewLine) buff.append(this.lineDelimiter);
            return buff.toString();
        } catch (IOException e) {
            throw new WizardException(e);
        }
    }

    /**
   * Value wird an das Ende der Range eingesetzt.<br>
   * Wenn der Value bereits vorhanden ist, wird der Value nicht erneut eingetragen. Diese Methode vergleicht
   * zeilenweise.
   * 
   * @param range Name des Ranges
   * @param value Einzufügender Wert
   */
    public void insert(String range, String value) {
        if (StringUtil.isNull(value)) return;
        boolean isImportRange = range.matches("USER-IMPORT");
        boolean inRange = false;
        value = prepareValue(isImportRange, value);
        try {
            BufferedReader br = createContentReader();
            StringBuilder sb = new StringBuilder();
            String line;
            int emptyLines = 0;
            StringBuilder sbRange = new StringBuilder();
            while ((line = br.readLine()) != null) {
                String lineTrimmed = line.trim();
                if (inRange) {
                    if (matchTagEnd(lineTrimmed, range)) {
                        inRange = false;
                        int[] idx = StringUtil.find(sbRange, value);
                        if (idx[0] != -1) sbRange.replace(idx[0], idx[1], value); else sbRange.append(value);
                        sb.append(sbRange);
                    } else if (StringUtil.isNull(lineTrimmed)) {
                        ++emptyLines;
                        continue;
                    } else {
                        while (emptyLines-- > 0) sbRange.append(this.lineDelimiter);
                        sbRange.append(line).append(this.lineDelimiter);
                        continue;
                    }
                } else if (matchTagBegin(lineTrimmed, range)) {
                    inRange = true;
                    sbRange.setLength(0);
                    emptyLines = 0;
                }
                sb.append(line);
                sb.append(this.lineDelimiter);
            }
            br.close();
            if (inRange) throw new FailedException("Missing section end for // {{" + range + " in file " + getAbsolutePath() + "\n").setFile(getAbsolutePath());
            Writer writer = createTMPWriter();
            writer.write(sb.toString());
            writer.close();
            fileCopyed = true;
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
    }

    private String removeSingleLineComments(final String line) {
        if (line.contains("//")) {
            final String pattern = "//\\s*+(?!\\{\\{|\\}\\}).*";
            if (line.contains("\"")) {
                String result = line.replaceAll(pattern, "");
                if (result.split("\"").length % 2 != 0) {
                    return result;
                }
            } else {
                return line.replaceAll(pattern, "");
            }
        }
        return line;
    }

    public boolean isChanged() {
        if (!this.fileTmp.exists() || !this.fileOrg.exists()) return false;
        boolean equals = false;
        StringBuilder sbFileNew = null;
        StringBuilder sbFileOld = null;
        try {
            BufferedReader br = new BufferedReader(createReader(this.fileTmp));
            String line;
            sbFileNew = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sbFileNew.append(removeSingleLineComments(line));
            }
            br.close();
            br = new BufferedReader(createOrgReader());
            sbFileOld = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sbFileOld.append(removeSingleLineComments(line));
            }
            String pattern = "\\s*//\\s*\\{\\{\\s*USER-IMPORT.+\\s*//\\s*\\}\\}\\s*USER-IMPORT\\s*";
            String sNew = sbFileNew.toString();
            String sOld = sbFileOld.toString();
            sNew = sNew.replaceAll(pattern, "");
            sOld = sOld.replaceAll(pattern, "");
            pattern = "\\s|/\\*.*?\\*/";
            sNew = sNew.replaceAll(pattern, "");
            sOld = sOld.replaceAll(pattern, "");
            equals = sNew.equals(sOld);
            br.close();
        } catch (FileNotFoundException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        } catch (IOException e) {
            throw new FailedException(e).setFile(getAbsolutePath());
        }
        return !equals;
    }

    public void copyToOrg() {
        if (this.fileCopyed) {
            if (!this.fileOrg.exists()) createPath(this.fileOrg.getPath());
            copyFile(this.fileTmp.getPath(), this.fileOrg.getPath(), true);
            this.fileCopyed = false;
        } else if (fileName != null && fileName.equals(CREATEFOLDER)) createPath(this.fileOrg.getPath());
    }

    /**
   * Binäres Kopieren einer Datei unabhängig von File-Format
   * 
   * @param source Ursprungsdatei
   * @param destination Zeildatei
   * @param replace gibt an, ob die Datei überschrieben werden darf
   * @return gibt an, ob die Datei kopiert wurde
   */
    public static boolean copyFile(String source, String destination, boolean replace) {
        File sourceFile = new File(source);
        File destinationFile = new File(destination);
        if (sourceFile.isDirectory() || destinationFile.isDirectory()) return false;
        if (destinationFile.isFile() && !replace) return false;
        if (!sourceFile.isFile()) return false;
        if (replace) destinationFile.delete();
        try {
            File dir = destinationFile.getParentFile();
            while (dir != null && !dir.exists()) {
                dir.mkdir();
            }
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destinationFile), 10240));
            DataInputStream inStream = new DataInputStream(new BufferedInputStream(new FileInputStream(sourceFile), 10240));
            try {
                while (inStream.available() > 0) {
                    outStream.write(inStream.readUnsignedByte());
                }
            } catch (EOFException eof) {
            }
            inStream.close();
            outStream.close();
        } catch (IOException ex) {
            throw new FailedException("Failed to copy file " + sourceFile.getAbsolutePath() + " to " + destinationFile.getAbsolutePath(), ex).setFile(destinationFile.getAbsolutePath());
        }
        return true;
    }

    public File getFileTMP() {
        return this.fileTmp;
    }

    public File getFileOrg() {
        return this.fileOrg;
    }

    public boolean isNew() {
        return !this.fileOrg.exists();
    }

    public String toString() {
        return this.fileOrg.toString();
    }

    public boolean hasItem(String sectionID, String pattern) {
        Section section = this.userSections.get(sectionID);
        if (section != null) return section.hasItem(pattern);
        return false;
    }

    public boolean hasItem(String sectionID, Pattern pattern) {
        Section section = this.userSections.get(sectionID);
        if (section != null) return section.hasItem(pattern);
        return false;
    }

    public boolean canWriteOrg() {
        return this.fileOrg.canWrite();
    }

    public boolean findInUserMemberSection(String memberName) {
        if (memberName == null || memberName.length() == 0) return false;
        boolean isInUserMemberSection = false;
        WizardFile.Section sectionFile = userSections.get("-MEMBER");
        if (sectionFile == null) return false;
        String section = sectionFile.text;
        String variante1 = " " + memberName + ";";
        if (section.indexOf(variante1) != -1) isInUserMemberSection = true;
        return isInUserMemberSection;
    }

    public boolean findInUserMemberSection2(String memberName) {
        if (memberName == null || memberName.length() == 0) return false;
        WizardFile.Section sectionFile = userSections.get("-MEMBER");
        if (sectionFile == null) return false;
        boolean isInUserMemberSection = false;
        String section = sectionFile.text;
        String variante1 = " " + memberName + "=";
        String variante2 = " " + memberName + " ";
        if (section.indexOf(variante1) != -1) {
            isInUserMemberSection = true;
        } else if (section.indexOf(variante2) != -1) {
            section = section.substring(section.indexOf(variante2) + variante2.length()).trim();
            if (section.startsWith("=")) isInUserMemberSection = true;
        }
        return isInUserMemberSection;
    }

    public void replaceFile(File file) {
        this.fileTmp = file;
    }

    public String getCharset() {
        return charset;
    }

    public String getLineDelimiter() {
        return lineDelimiter;
    }

    private void reformatLineDelimiter() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.fileTmp), charset));
            StringBuilder buff = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) buff.append(line).append(lineDelimiter);
            reader.close();
            Writer out = new OutputStreamWriter(new FileOutputStream(this.fileTmp), charset);
            try {
                out.write(buff.toString());
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            throw new FailedException("Failed to reformat file " + this.fileTmp.getAbsolutePath(), e).setFile(this.fileTmp.getAbsolutePath());
        }
    }
}
