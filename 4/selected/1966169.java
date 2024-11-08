package net.sourceforge.tuned;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public final class FileUtilities {

    public static File moveRename(File source, File destination) throws IOException {
        destination = resolveDestination(source, destination);
        if (source.isDirectory()) {
            moveFolderIO(source, destination);
        } else {
            try {
                moveFileNIO2(source, destination);
            } catch (LinkageError e) {
                moveFileIO(source, destination);
            }
        }
        return destination;
    }

    private static void moveFileNIO2(File source, File destination) throws IOException {
        java.nio.file.Files.move(source.toPath(), destination.toPath());
    }

    private static void moveFileIO(File source, File destination) throws IOException {
        org.apache.commons.io.FileUtils.moveFile(source, destination);
    }

    private static void moveFolderIO(File source, File destination) throws IOException {
        org.apache.commons.io.FileUtils.moveDirectory(source, destination);
    }

    public static File copyAs(File source, File destination) throws IOException {
        destination = resolveDestination(source, destination);
        if (source.isDirectory()) {
            org.apache.commons.io.FileUtils.copyDirectory(source, destination);
        } else {
            org.apache.commons.io.FileUtils.copyFile(source, destination);
        }
        return destination;
    }

    public static File resolveDestination(File source, File destination) throws IOException {
        if (!destination.isAbsolute()) {
            destination = new File(source.getParentFile(), destination.getPath());
        }
        File destinationFolder = destination.getParentFile();
        if (!destinationFolder.isDirectory() && !destinationFolder.mkdirs()) {
            throw new IOException("Failed to create folder: " + destinationFolder);
        }
        return destination;
    }

    public static byte[] readFile(File source) throws IOException {
        InputStream in = new FileInputStream(source);
        try {
            byte[] data = new byte[(int) source.length()];
            int position = 0;
            int read = 0;
            while (position < data.length && (read = in.read(data, position, data.length - position)) >= 0) {
                position += read;
            }
            return data;
        } finally {
            in.close();
        }
    }

    public static String readAll(Reader source) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[2048];
        int read = 0;
        while ((read = source.read(buffer)) >= 0) {
            text.append(buffer, 0, read);
        }
        return text.toString();
    }

    public static void writeFile(ByteBuffer data, File destination) throws IOException {
        FileChannel fileChannel = new FileOutputStream(destination).getChannel();
        try {
            fileChannel.write(data);
        } finally {
            fileChannel.close();
        }
    }

    public static Reader createTextReader(File file) throws IOException {
        CharsetDetector detector = new CharsetDetector();
        detector.setDeclaredEncoding("UTF-8");
        detector.setText(new BufferedInputStream(new FileInputStream(file)));
        CharsetMatch charset = detector.detect();
        if (charset != null) return charset.getReader();
        return new InputStreamReader(new FileInputStream(file), "UTF-8");
    }

    public static String getText(ByteBuffer data) throws IOException {
        CharsetDetector detector = new CharsetDetector();
        detector.setDeclaredEncoding("UTF-8");
        detector.setText(new ByteBufferInputStream(data));
        CharsetMatch charset = detector.detect();
        if (charset != null) {
            try {
                return charset.getString();
            } catch (RuntimeException e) {
                throw new IOException("Failed to read text", e);
            }
        }
        return Charset.forName("UTF-8").decode(data).toString();
    }

    /**
	 * Pattern used for matching file extensions.
	 * 
	 * e.g. "file.txt" -> match "txt", ".hidden" -> no match
	 */
    public static final Pattern EXTENSION = Pattern.compile("(?<=.[.])\\p{Alnum}+$");

    public static String getExtension(File file) {
        if (file.isDirectory()) return null;
        return getExtension(file.getName());
    }

    public static String getExtension(String name) {
        Matcher matcher = EXTENSION.matcher(name);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static boolean hasExtension(File file, String... extensions) {
        return hasExtension(file.getName(), extensions) && !file.isDirectory();
    }

    public static boolean hasExtension(String filename, String... extensions) {
        String extension = getExtension(filename);
        for (String value : extensions) {
            if ((extension == null && value == null) || (extension != null && extension.equalsIgnoreCase(value))) return true;
        }
        return false;
    }

    public static String getNameWithoutExtension(String name) {
        Matcher matcher = EXTENSION.matcher(name);
        if (matcher.find()) {
            return name.substring(0, matcher.start() - 1);
        }
        return name;
    }

    public static String getName(File file) {
        if (file.isDirectory()) return getFolderName(file);
        return getNameWithoutExtension(file.getName());
    }

    public static String getFolderName(File file) {
        String name = file.getName();
        if (!name.isEmpty()) return name;
        return file.toString();
    }

    public static boolean isDerived(File derivate, File prime) {
        return isDerived(getName(derivate), prime);
    }

    public static boolean isDerived(String derivate, File prime) {
        String base = getName(prime).trim().toLowerCase();
        derivate = derivate.trim().toLowerCase();
        return derivate.startsWith(base);
    }

    public static boolean isDerivedByExtension(File derivate, File prime) {
        return isDerivedByExtension(getName(derivate), prime);
    }

    public static boolean isDerivedByExtension(String derivate, File prime) {
        String base = getName(prime).trim().toLowerCase();
        derivate = derivate.trim().toLowerCase();
        if (derivate.equals(base)) return true;
        while (derivate.length() > base.length() && getExtension(derivate) != null) {
            derivate = getNameWithoutExtension(derivate);
            if (derivate.equals(base)) return true;
        }
        return false;
    }

    public static boolean containsOnly(Iterable<File> files, FileFilter filter) {
        for (File file : files) {
            if (!filter.accept(file)) return false;
        }
        return true;
    }

    public static List<File> filter(Iterable<File> files, FileFilter... filters) {
        List<File> accepted = new ArrayList<File>();
        for (File file : files) {
            for (FileFilter filter : filters) {
                if (filter.accept(file)) {
                    accepted.add(file);
                    break;
                }
            }
        }
        return accepted;
    }

    public static List<File> flatten(Iterable<File> roots, int maxDepth, boolean listHiddenFiles) {
        List<File> files = new ArrayList<File>();
        for (File root : roots) {
            if (root.isDirectory()) {
                listFiles(root, 0, files, maxDepth, listHiddenFiles);
            } else {
                files.add(root);
            }
        }
        return files;
    }

    public static List<File> listPath(File file) {
        LinkedList<File> nodes = new LinkedList<File>();
        for (File node = file; node != null; node = node.getParentFile()) {
            nodes.addFirst(node);
        }
        return nodes;
    }

    public static List<File> listFiles(Iterable<File> folders, int maxDepth, boolean listHiddenFiles) {
        List<File> files = new ArrayList<File>();
        for (File folder : folders) {
            listFiles(folder, 0, files, maxDepth, listHiddenFiles);
        }
        return files;
    }

    private static void listFiles(File folder, int depth, List<File> files, int maxDepth, boolean listHiddenFiles) {
        if (depth > maxDepth) return;
        for (File file : folder.listFiles()) {
            if (!listHiddenFiles && file.isHidden()) continue;
            if (file.isDirectory()) {
                listFiles(file, depth + 1, files, maxDepth, listHiddenFiles);
            } else {
                files.add(file);
            }
        }
    }

    public static SortedMap<File, List<File>> mapByFolder(Iterable<File> files) {
        SortedMap<File, List<File>> map = new TreeMap<File, List<File>>();
        for (File file : files) {
            File key = file.getParentFile();
            if (key == null) {
                throw new IllegalArgumentException("Parent is null: " + file);
            }
            List<File> valueList = map.get(key);
            if (valueList == null) {
                valueList = new ArrayList<File>();
                map.put(key, valueList);
            }
            valueList.add(file);
        }
        return map;
    }

    public static Map<String, List<File>> mapByExtension(Iterable<File> files) {
        Map<String, List<File>> map = new HashMap<String, List<File>>();
        for (File file : files) {
            String key = getExtension(file);
            if (key != null) {
                key = key.toLowerCase();
            }
            List<File> valueList = map.get(key);
            if (valueList == null) {
                valueList = new ArrayList<File>();
                map.put(key, valueList);
            }
            valueList.add(file);
        }
        return map;
    }

    /**
	 * Invalid file name characters: \, /, :, *, ?, ", <, >, |, \r and \n
	 */
    public static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\r\\n]");

    /**
	 * Strip file name of invalid characters
	 * 
	 * @param filename original filename
	 * @return valid file name stripped of invalid characters
	 */
    public static String validateFileName(CharSequence filename) {
        return ILLEGAL_CHARACTERS.matcher(filename).replaceAll("");
    }

    public static boolean isInvalidFileName(CharSequence filename) {
        return ILLEGAL_CHARACTERS.matcher(filename).find();
    }

    public static File validateFileName(File file) {
        if (!isInvalidFileName(file.getName())) return file;
        return new File(file.getParentFile(), validateFileName(file.getName()));
    }

    public static File validateFilePath(File path) {
        Iterator<File> nodes = listPath(path).iterator();
        File validatedPath = validateFileName(nodes.next());
        while (nodes.hasNext()) {
            validatedPath = new File(validatedPath, validateFileName(nodes.next().getName()));
        }
        return validatedPath;
    }

    public static boolean isInvalidFilePath(File path) {
        for (File node = path; node != null; node = node.getParentFile()) {
            if (isInvalidFileName(node.getName())) return true;
        }
        return false;
    }

    public static String normalizePathSeparators(String path) {
        return path.replace('\\', '/');
    }

    public static String replacePathSeparators(CharSequence path) {
        return replacePathSeparators(path, " ");
    }

    public static String replacePathSeparators(CharSequence path, String replacement) {
        return Pattern.compile("\\s*[\\\\/]+\\s*").matcher(path).replaceAll(replacement);
    }

    public static String getXmlString(Document dom) throws TransformerException {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter buffer = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(buffer));
        return buffer.toString();
    }

    public static final long KILO = 1024;

    public static final long MEGA = KILO * 1024;

    public static final long GIGA = MEGA * 1024;

    public static String formatSize(long size) {
        if (size >= MEGA) return String.format("%,d MB", size / MEGA); else if (size >= KILO) return String.format("%,d KB", size / KILO); else return String.format("%,d Byte", size);
    }

    public static final FileFilter FOLDERS = new FileFilter() {

        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    public static final FileFilter FILES = new FileFilter() {

        @Override
        public boolean accept(File file) {
            return file.isFile();
        }
    };

    public static final FileFilter TEMPORARY = new FileFilter() {

        private final String tmpdir = System.getProperty("java.io.tmpdir");

        @Override
        public boolean accept(File file) {
            return file.getAbsolutePath().startsWith(tmpdir);
        }
    };

    public static class ParentFilter implements FileFilter {

        private final File folder;

        public ParentFilter(File folder) {
            this.folder = folder;
        }

        @Override
        public boolean accept(File file) {
            return listPath(file).contains(folder);
        }
    }

    public static class ExtensionFileFilter implements FileFilter {

        private final String[] extensions;

        public ExtensionFileFilter(String... extensions) {
            this.extensions = extensions;
        }

        public ExtensionFileFilter(Collection<String> extensions) {
            this.extensions = extensions.toArray(new String[0]);
        }

        @Override
        public boolean accept(File file) {
            return hasExtension(file, extensions);
        }

        public boolean accept(String name) {
            return hasExtension(name, extensions);
        }

        public boolean acceptExtension(String extension) {
            for (String other : extensions) {
                if (other.equalsIgnoreCase(extension)) return true;
            }
            return false;
        }

        public String extension() {
            return extensions[0];
        }

        public String[] extensions() {
            return extensions.clone();
        }
    }

    /**
	 * Dummy constructor to prevent instantiation.
	 */
    private FileUtilities() {
        throw new UnsupportedOperationException();
    }
}
