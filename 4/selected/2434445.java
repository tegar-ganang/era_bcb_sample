package mipt.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;
import mipt.common.Const;

/**
 * Converts ListResourceBundle to Propert*ResourceBundle with transformation
 *  root_packages/package/[/gui]*.java -> package.properties
 *  without relocation (or, optionally, with relocation to the given directory).
 * By default (if dir is not absolute path) seeks the file (*.java)
 *   in ../<project>/src/<dir>; and file name pattern is "Bundle*". 
 * Can perform group operation with all non-abstract sublcasses of the given Bundle class
 *  (e.g. mipt.common.Bundle) in the given project and in its given root dir.
 * Assumes the only input java source format yet (see comments to JavaReader embedded class
 *   (the main is that no more than one entry (or array element!) is allowed on each line).
 * Converts comments too ("//" -> "# "). Both after-entry and before-entry comments in *.java
 *  become before-entry ones in *.properties.
 *  Several comments to one multi-line value are merged to one line.
 * The output encoding is UTF-16, but it can be changed.
 * See showArgumentsHelp() for more details.
 * TO DO?: Convertion to XML format (in that case native encoding can be used).
 * TO DO??: Reverse convertion - from PropertyResourceBundle to ListResourceBundle.
 * @author Evdokimov
 */
public class BundleConverter extends AbstractUtil implements Const {

    static {
        clazz = BundleConverter.class;
    }

    protected String dir;

    protected String project = null;

    protected String file = "Bundle.*";

    protected String superClass = null;

    protected String dest = null;

    protected String encoding = "UTF-16";

    /**
	 * @see mipt.util.AbstractUtil#processOption(java.lang.String, java.lang.String)
	 */
    public boolean processOption(String option, String value) {
        if (super.processOption(option, value)) {
            if (option.equals("file")) {
                file = file.replace("*", ".*");
            }
            return true;
        }
        if (option.equals("super")) {
            superClass = value;
        } else {
            return false;
        }
        return true;
    }

    /**
	 * @see mipt.util.AbstractUtil#processArgument(java.lang.String, int)
	 */
    public void processArgument(String arg, int index) {
        if (index == 0) dir = arg;
    }

    /**
	 * @see mipt.util.AbstractUtil#getSupportedOptions()
	 */
    public String[] getSupportedOptions() {
        return new String[] { "?", "project", "file", "super", "dest", "encoding" };
    }

    /**
	 * @see mipt.util.AbstractUtil#getUsageHelp()
	 */
    protected String getUsageHelp() {
        return "java " + getClass().getName() + " dir [-option <arg>]\r\n" + "where dir is relative or absolute path to bundle(s)";
    }

    /**
	 * @see mipt.util.AbstractUtil#showArgumentsHelp(java.io.PrintStream)
	 */
    public void showArgumentsHelp(PrintStream out) {
        out.println("**** Converts ResourceBundles: from *.java to *.properties ****");
        out.println(" Usage: " + getUsageHelp());
        out.println(" Options:");
        out.println("  '-?': shows this help on command line arguments;");
        out.println("  '-project': uses '../project/src/dir' as dir");
        out.println("    (if it isn't set and if dir is relative, './src/dir' is used);");
        out.println("  '-file': sets file name (without extension!) or its pattern (with * as wildcard)");
        out.println("    (by default 'Bundle*' is used);");
        out.println("  '-super': all non-abstract subclasses of this class in project.dir are processed");
        out.println("     (if project is not set, ./src/dir is used as root to scan)");
        out.println("     (note that 'mipt' is assumed to be the root package to form class name from file path)");
        out.println("     (note that CLASSPATH should be set properly for this option to work);");
        out.println("  '-dest': sets destination directory path (absolute path or relative to project dir or to . dir)");
        out.println("    (if it isn't set the source dir is used; if it's set but doesn't exist in FS - created);");
        out.println("  '-encoding': sets encoding of destination file ('UTF-16' by default).");
        out.println(" Example args: mipt\\crec\\lab\\compmath -project ALES\\NumLabs -super mipt.common.Bundle -dest labs\\strings");
    }

    /**
	 * Converts from *.java to *.properties.
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        java2properties();
    }

    /**
	 * This implementation can be called only once (it changed the fields).
	 */
    public void java2properties() {
        convert("new Object[][]", new JavaReader(), new PropertiesWriter());
    }

    /**
	 * This implementation can be called only once (it changed the fields).
	 */
    public void convert(String startWith, BundleReader reader, BundleWriter writer) {
        if (dir == null) {
            showArgumentsHelp(System.err);
            return;
        }
        if (project != null) {
            project = new File(userDir).getParent() + fileSep + project;
            dir = project + fileSep + "src" + fileSep + dir;
        }
        if (!new File(dir).isAbsolute()) dir = userDir + fileSep + "src" + fileSep + dir;
        if (!new File(dir).exists()) throw new IllegalStateException(dir + " does not exist");
        if (dest != null && !new File(dest).isAbsolute()) {
            dest = project == null ? userDir + fileSep + dest : project + fileSep + dest;
            File d = new File(dest);
            if (!d.exists()) d.mkdir();
        }
        Iterator files = getFiles(dir).iterator();
        int i = 0, j = 0;
        while (files.hasNext()) {
            try {
                file2file(files.next(), startWith, reader, writer);
                j++;
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            i++;
        }
        System.out.println(i + " classes found, " + j + " of them converted to properties");
    }

    /**
	 * Return Files
	 */
    protected LinkedList getFiles(String dir) {
        LinkedList files = new LinkedList();
        if (superClass == null) {
            if (!file.contains("*")) addFile(files, dir, file); else addFiles(files, dir, false);
        } else {
            try {
                scanDir(files, dir, getRootDir(dir), Class.forName(superClass));
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
            }
        }
        return files;
    }

    /**
	 * Returns first directory part (ending with fileSep) - without package subdirectories.
	 * Is needed only for -super option.
	 * @param dir - full path
	 */
    protected final String getRootDir(String dir) {
        String last;
        do {
            if (dir.endsWith(fileSep)) dir = dir.substring(0, dir.length() - 1);
            int j = dir.lastIndexOf(fileSep) + 1;
            if (j == 0) break;
            last = dir.substring(j);
            dir = dir.substring(0, j);
        } while (!isRootDir(dir, last));
        return dir;
    }

    /**
	 * For overriding. This implementation assumes that the root package is "mipt". 
	 */
    protected boolean isRootDir(String path, String removedDir) {
        return removedDir.equals("mipt");
    }

    /**
	 * Is needed only for -super option.
	 * @param files - the result - Files matching both (name and class) criteria
	 * @param dir - full path
	 * @param rootDir - its first part upto package name
	 * @param supClass - class to check inheritance from
	 */
    protected void scanDir(LinkedList files, String dir, String rootDir, Class supClass) throws ClassNotFoundException {
        LinkedList current = new LinkedList();
        addFiles(current, dir, true);
        String className = dir.substring(rootDir.length());
        className = className.replace(fileSep.charAt(0), '.');
        if (!className.endsWith(".")) className += ".";
        Iterator iter = current.iterator();
        while (iter.hasNext()) {
            File file = (File) iter.next();
            if (file.isDirectory()) scanDir(files, file.getPath(), rootDir, supClass); else {
                String name = file.getName();
                String shortName = name.substring(0, name.length() - ".java".length());
                Class cls = Class.forName(className + shortName);
                if (supClass.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers())) {
                    if (hasContents(cls)) files.add(file); else {
                        int j = shortName.indexOf('_');
                        if (j < 0) continue;
                        shortName = shortName.substring(0, j);
                        File sourceFile = new File(file.getParent(), shortName + ".java");
                        files.add(new File[] { sourceFile, file });
                    }
                }
            }
        }
    }

    /**
	 * By default return true if initContents() method is declared in a class
	 * If returns false, file with name.java (if it exist) is assumed to have bundle contents:
	 *  name_**.java is the name of this class file. 
	 */
    protected boolean hasContents(Class cls) {
        try {
            cls.getDeclaredMethod("initContents", new Class[0]);
            return true;
        } catch (SecurityException e) {
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
	 * Is needed if -file option value is name pattern.
	 * @param files - the result - Files matching name criterion
	 * @param dir - full path
	 */
    protected void addFiles(LinkedList files, String dir, final boolean acceptDir) {
        File dirFile = new File(dir);
        File[] f = dirFile.listFiles(new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) return acceptDir && !f.isHidden();
                String name = f.getName();
                if (!name.endsWith(".java")) return false;
                Pattern p = Pattern.compile(file, Pattern.CASE_INSENSITIVE);
                return p.matcher(name).matches();
            }
        });
        for (int i = 0; i < f.length; i++) files.add(f[i]);
    }

    /**
	 * Is needed if -file option value is file name but NOT name pattern.
	 * @param files - the result
	 * @param dir - full path
	 */
    protected final void addFile(LinkedList files, String dir, String file) {
        files.add(new File(dir, file + ".java"));
    }

    /**
	 * Returns path to save properties to. 
	 * @param source - path to source ListResourceBundle.
	 */
    protected String getPropertiesPath(File source) {
        String destination = dest == null ? source.getParent() : dest;
        return destination + fileSep + getPropertiesFileName(source) + ".properties";
    }

    /**
	 * Returns short file name (without extension) to save properties to.
	 * Files are names by package name (so avoid having two bundles of one locale in one package!).
	 * This implementation avoids naming file by "gui" (in "gui" case it names by parent package).
	 * @param source - path to source ListResourceBundle.
	 */
    protected String getPropertiesFileName(File source) {
        File pack = source.getParentFile();
        if (pack.getName().equals("gui")) pack = pack.getParentFile();
        String p = pack.getName(), name = source.getName();
        int j = name.lastIndexOf('_');
        if (j >= 0) p += name.substring(j, j + 3);
        return p;
    }

    /**
	 * Conversion from *.java to a file of format defined by BundleWriter
	 * @param file - can be File or (if source file is not equals to dest file)
	 */
    public void file2file(Object file, String startWith, BundleReader reader, BundleWriter writer) throws IOException {
        File source, dest;
        if (file instanceof File) source = dest = (File) file; else {
            File[] arr = (File[]) file;
            source = arr[0];
            dest = arr[1];
        }
        BufferedReader br = new BufferedReader(new FileReader(source));
        try {
            writer.createWriter(getPropertiesPath(dest), encoding);
            boolean header = true;
            Entry entry = new Entry();
            while (br.ready()) {
                String line = br.readLine();
                if (line == null) break;
                if (header) {
                    header = reader.isHeader(line, startWith);
                    continue;
                }
                if (header) continue;
                if (!reader.parseEntry(br, line, entry)) break;
                writer.write(entry);
            }
        } finally {
            br.close();
            writer.close();
        }
    }

    /**
	 * Extracted to support different directions conversion in future (not only from *.java)
	 * @see file2file
	 */
    public interface BundleReader {

        /**
		 * Returns false if the next line is already not header
		 * After returning false first time, the method is never called for this 
		 */
        boolean isHeader(String line, String startWith);

        /**
		 * Fills Entry from line (and maybe the next lines of reader).
		 * @param br - could be needed because of empty lines or multi-line values or arrays
		 * @param line - (first) line to parse
		 * @param entry - the result; all fields must be set? but if key==null, value and array does not matter
		 * @return false if no more entry lines in br
		 */
        boolean parseEntry(BufferedReader br, String line, Entry entry) throws IOException;
    }

    /**
	 * Extracted to support different (at first, XML) formats in future
	 * @see file2file
	 */
    public interface BundleWriter {

        /**
		 * Created the real writer and stors it to use in other methods
		 */
        void createWriter(String path, String encoding) throws IOException;

        /**
		 * Defines bundle format. Is not called if createWriter failed
		 */
        void write(Entry entry) throws IOException;

        /**
		 * Can be called even if createWriter failed
		 */
        void close() throws IOException;
    }

    /**
	 * Implements *.java format presented by mipt.common.Bundle: 
	 *  initContents() or similar method that returns "new Object[][]{...};".
	 * In case of String[] values in *.java format of *.properties is extended: key=[[s1[[s2[[.. 
	 * In case of String values with '\n' in them format is also extended: "\\n" string is used instead
	 *  (and '\r's are simply removed).
	 */
    public static class JavaReader implements BundleReader {

        /**
		 * @see mipt.util.BundleConverter.BundleReader#isHeader(java.lang.String, java.lang.String)
		 */
        public boolean isHeader(String line, String startWith) {
            return line.lastIndexOf(startWith) < 0;
        }

        /**
		 * Supports multi-line value lines separated by "+" (as well as String[]).
		 * Assumes that there is no more than one entry (or array element) on each line.
		 * Assumes that '{"', '};', 'String[]', etc. are without spaces between symbols;
		 *  that '};' is in its separate line and that 'String[]{' and its elements are separate; 
		 *  and that there is no \" symbols in keys (in values they can exist)  
		 * @see mipt.util.BundleConverter.BundleReader#parseEntry(java.io.BufferedReader, java.lang.String, mipt.util.BundleConverter.Entry)
		 */
        public boolean parseEntry(BufferedReader br, String line, Entry entry) throws IOException {
            int jK = line.indexOf("{\""), jC = line.lastIndexOf("//"), jEnd = line.lastIndexOf("};");
            while (jK < 0 && jC < 0 && jEnd < 0 && br.ready()) {
                line = br.readLine();
                jK = line.indexOf("{\"");
                jC = line.lastIndexOf("//");
                jEnd = line.lastIndexOf("};");
            }
            if (jK < 0 && jC < 0) return false;
            if (jC >= 0 && jK >= 0) jC = checkComment(jC, line);
            if (jC < 0) {
                entry.comment = null;
            } else {
                entry.comment = trim(line.substring(jC + 2));
                line = line.substring(0, jC);
                jK = line.indexOf("{\"");
            }
            if (jK < 0) entry.key = null; else {
                jK += 2;
                int j = line.indexOf('"', jK);
                if (j < 0) throw new IOException("Wrong source format: unclosed \" in key");
                entry.key = line.substring(jK, j);
                parseValue(br, line.substring(j + 1), entry);
            }
            return true;
        }

        private int checkComment(int jC, String line) {
            int jV = line.lastIndexOf('"');
            if (jC < jV && jC > line.lastIndexOf('"', jV - 1)) return -1;
            int jC2 = line.lastIndexOf("//", jC - 1);
            if (jC2 >= 0) jC2 = checkComment(jC2, line);
            if (jC2 >= 0) return jC2;
            return jC;
        }

        /**
		 * Parses value (including multi-line String or String[]).
		 * Both value and array must be set here
		 * If non-first lines have comments and if entry.comment (first linr comment)
		 *  is not null - they must be merged (but without \r or \n symbols;
		 *   '.' and capital letter is used now to denote separation of comments).
		 * @param line - first line, is already without key.
		 */
        protected void parseValue(BufferedReader br, String line, Entry entry) throws IOException {
            String element[] = new String[1];
            int j = line.lastIndexOf("String[]");
            if (j >= 0) {
                LinkedList list = new LinkedList();
                while (true) {
                    boolean end = !parseValue(element, br, line, entry, true);
                    if (element[0] == null) break;
                    list.add(element[0]);
                    if (end) break;
                    line = br.readLine();
                }
                entry.array = new String[list.size()];
                list.toArray(entry.array);
                entry.value = null;
            } else {
                parseValue(element, br, line, entry, false);
                if (element[0] == null) throw new IOException("Wrong source format: " + line + " is neither String (constant) nor String[] (array of constants)");
                entry.value = element[0];
                entry.array = null;
            }
        }

        /**
		 * Parses non-array values and stores the result (null if nothing found) in argument.
		 * If finds '}' return false (useful for array elements).
		 * Entry is for setting (or merging) comment only.
		 * @param inLoop - true indicates that recursion and call to br.readLine() is prohibited
		 *  (except for the empty line case: result[0] is null): br.readLine() is called in loop instead of recursion.
		 */
        protected boolean parseValue(String[] result, BufferedReader br, String line, Entry entry, boolean inLoop) throws IOException {
            int jC = line.indexOf("//");
            if (jC >= 0) jC = checkComment(jC, line);
            if (jC >= 0) {
                String comment = line.substring(jC + 2);
                comment = trim(comment);
                if (entry.comment == null) entry.comment = comment; else {
                    if (comment.length() > 1) comment = Character.toUpperCase(comment.charAt(0)) + comment.substring(1);
                    entry.comment += ". " + comment;
                }
                line = line.substring(0, jC);
            }
            line = trim(line, false);
            int j1 = line.indexOf('"'), jEndValue = line.lastIndexOf('}');
            if (j1 < 0) {
                if (jEndValue < 0) return parseValue(result, br, br.readLine(), entry, inLoop);
                result[0] = null;
                return false;
            }
            int j2 = line.lastIndexOf('"');
            if (j2 == j1) throw new IOException("Wrong source format: unclosed \" in value");
            boolean continued = line.charAt(line.length() - 1) == '+';
            result[0] = correctValue(line.substring(j1 + 1, j2));
            if (continued) {
                String[] end = new String[1];
                boolean res = parseValue(end, br, br.readLine(), entry, inLoop);
                if (end[0] != null) result[0] += end[0];
                return res;
            }
            if (jEndValue >= 0 && jEndValue > j2) return false;
            if (inLoop) return true;
            return parseValue(result, br, br.readLine(), entry, false);
        }

        /**
		 * This implementation only: a) removes '\r' and converts '\n'
		 * b) replaces " " unicode symbols representation to one char.
		 */
        protected String correctValue(String value) {
            int j = value.indexOf("\\u");
            while (j >= 0) {
                int ch = Integer.parseInt(value.substring(j + 2, j + 6), 16);
                value = value.substring(0, j) + (char) ch + value.substring(j + 6);
                j = value.indexOf("\\u", j + 1);
            }
            value = value.replace("\r", "");
            return value.replace("\n", "\\n");
        }

        /**
		 * Eliminates leading and trailing empty space (see trim(String, boolean))   
		 */
        public final String trim(String s) {
            s = trim(s, true);
            return trim(s, false);
        }

        /**
		 * Eliminates leading or trailing empty space not only ' 's
		 *  but '\t's and '/'s too   
		 */
        public String trim(String s, boolean leading) {
            int n = leading ? s.length() - 1 : 0, m = s.length() - 1 - n, j, i = leading ? 1 : -1;
            for (j = m; j != n; j += i) {
                char c = s.charAt(j);
                if (c != ' ' && c != '\t' && c != '/') break;
            }
            if (j == m) return s;
            return leading ? s.substring(j) : s.substring(0, j + 1);
        }
    }

    /**
	 * Outputs: empty line | key=value | comment | comment followed by key=value (2 lines).
	 * In addition to standard Properties format, supports String[] (format: [s0[s1[s2..).
	 */
    public static class PropertiesWriter implements BundleWriter {

        protected BufferedWriter bw;

        /**
		 * @see mipt.util.BundleConverter.BundleWriter#createWriter(java.lang.String, java.lang.String)
		 */
        public void createWriter(String path, String encoding) throws IOException {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), encoding));
        }

        /**
		 * @see mipt.util.BundleConverter.BundleWriter#write(mipt.util.BundleConverter.Entry)
		 */
        public void write(Entry entry) throws IOException {
            bw.write(entry2property(entry));
            bw.write(lineSep);
            bw.flush();
        }

        /**
		 * @see mipt.util.BundleConverter.BundleWriter#close()
		 */
        public void close() throws IOException {
            if (bw != null) bw.close();
        }

        /**
		 * Implements *.properties file format 
		 */
        protected String entry2property(Entry entry) {
            String line = "";
            if (entry.key != null) {
                if (entry.value != null) line = entry.value; else for (int i = 0; i < entry.array.length; i++) {
                    line = line + "[[" + entry.array[i];
                }
                line = entry.key + "=" + line;
            }
            if (entry.comment == null) return line; else if (line == "") return "# " + entry.comment; else return "# " + entry.comment + lineSep + line;
        }
    }

    /**
	 * Entry data holder - to exchange data between BundleReader and BundleWriter.
	 * Is not created many times (for perfomance reasons) so has no constructor. 
	 */
    public static class Entry {

        public String key, value, array[], comment;
    }
}
