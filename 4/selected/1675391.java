package com.yerihyo.yeritools.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.yerihyo.yeritools.collections.CollectionsToolkit;
import com.yerihyo.yeritools.debug.YeriDebug;
import com.yerihyo.yeritools.io.IOToolkit.CharSequenceWriter;
import com.yerihyo.yeritools.text.StringToolkit;
import com.yerihyo.yeritools.text.StringToolkit.StringComparator;

public class FileToolkit {

    public static final String PLATFORM_FILE_SEPARATOR = System.getProperty("line.separator");

    public static final String SINGLE_SLASH = "/";

    public static final String CVS_Folder = "CVS";

    public static final String SVN_Folder = ".svn";

    public static final String bakFileExtension = ".bak";

    public static final String[] CVS_SVN_Folders = new String[] { CVS_Folder, SVN_Folder };

    public static class FilterSetFileFilter extends HashSet<FileFilter> implements FileFilter {

        public static final long serialVersionUID = 1;

        public boolean accept(File f) {
            for (FileFilter filter : this) {
                if (filter.accept(f)) return true;
            }
            return false;
        }
    }

    public static class ExtensionListFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

        private List<String> extensionList = new ArrayList<String>();

        private boolean folderEnabled = true;

        public ExtensionListFileFilter(String[] extensionArray, boolean folderEnabled) {
            this.folderEnabled = folderEnabled;
            CollectionsToolkit.addAll(extensionList, extensionArray);
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory() && folderEnabled) {
                return true;
            }
            for (String extension : extensionList) {
                if (file.getAbsolutePath().toLowerCase().endsWith(extension.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Extension File Filter (" + StringToolkit.toString(extensionList.toArray(new String[0]), ",") + ")";
        }
    }

    public static ExtensionListFileFilter createExtensionListFileFilter(String[] extensionArray, boolean folderEnabled) {
        return new ExtensionListFileFilter(extensionArray, folderEnabled);
    }

    private static String addBackSlashesToFileSeparator(String s) {
        if (s.equals("\\")) {
            return "\\\\";
        }
        return s;
    }

    public static void writeTo(File file, CharSequence content) throws IOException {
        writeTo(file, content, false);
    }

    public static void writeTo(File file, CharSequence content, Charset charset) throws IOException {
        writeTo(file, content, charset, false);
    }

    public static void writeTo(File file, CharSequence content, boolean append) throws IOException {
        writeTo(file, content, Charset.defaultCharset(), append);
    }

    public static void writeTo(File file, CharSequence content, Charset charset, boolean append) throws IOException {
        CharSequenceWriter writer = new CharSequenceWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
        writer.write(content);
        writer.close();
    }

    public static void writeTo(File file, Collection<? extends CharSequence> tokenCollection, CharSequence delim) throws IOException {
        writeTo(file, tokenCollection, delim, Charset.defaultCharset(), false);
    }

    public static void writeTo(File file, Collection<? extends CharSequence> tokenCollection, CharSequence delim, Charset charset, boolean append) throws IOException {
        CharSequenceWriter writer = new CharSequenceWriter(new OutputStreamWriter(new FileOutputStream(file, append), charset));
        boolean isFirst = true;
        for (CharSequence token : tokenCollection) {
            if (isFirst) {
                isFirst = false;
            } else {
                writer.write(delim);
            }
            writer.write(token);
        }
        writer.close();
    }

    public static String[] defaultNoCopyFilenameList = new String[] { "thumbs.db" };

    private static void copyFile(File inputFile, File outputFile, String[] noCopyFilenameList) {
        if (CollectionsToolkit.containsByComparator(noCopyFilenameList, inputFile.getName(), new StringComparator(true))) {
            return;
        }
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
            FileOutputStream out = new FileOutputStream(outputFile);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyFileOrFolder(File infile, File ofile, String[] noCopyFilenameList) {
        if (ofile.exists()) {
            boolean infileIsFile = infile.isFile();
            boolean ofileIsFile = ofile.isFile();
            YeriDebug.ASSERT(infileIsFile == ofileIsFile, "One is a file, the other is a folder");
        }
        if (infile.isFile()) {
            copyFile(infile, ofile, noCopyFilenameList);
        } else {
            if (!ofile.exists()) {
                YeriDebug.ASSERT(ofile.mkdirs(), "Cannot create folder!");
            }
            for (File file : infile.listFiles()) {
                File childOFile = new File(ofile, file.getName());
                copyFileOrFolder(file, childOFile, noCopyFilenameList);
            }
        }
    }

    public static void concatenateToDestFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            if (!destFile.createNewFile()) {
                throw new IllegalArgumentException("Could not create destination file:" + destFile.getName());
            }
        }
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        byte[] buffer = new byte[1024];
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(destFile, true));
            bufferedInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            while (true) {
                int readByte = bufferedInputStream.read(buffer, 0, buffer.length);
                if (readByte == -1) {
                    break;
                }
                bufferedOutputStream.write(buffer, 0, readByte);
            }
        } finally {
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
        }
    }

    public static List<File> filter(File[] fileArray, FileFilter fileFilter) {
        List<File> returnList = new ArrayList<File>();
        for (File file : fileArray) {
            if (!fileFilter.accept(file)) {
                continue;
            }
            returnList.add(file);
        }
        return returnList;
    }

    public static List<String> filterFilename(Collection<? extends String> filenames, String extension) {
        List<String> returnList = new ArrayList<String>();
        for (String filename : filenames) {
            if (!filename.toLowerCase().endsWith(extension.toLowerCase())) {
                continue;
            }
            returnList.add(filename);
        }
        return returnList;
    }

    public static String fileSeparatorFromSlashToPlatform(String path) {
        if (PLATFORM_FILE_SEPARATOR.equals(SINGLE_SLASH)) {
            return path;
        }
        String quotedPlatformSeparator = addBackSlashesToFileSeparator(PLATFORM_FILE_SEPARATOR);
        String returnString = path.replaceAll(SINGLE_SLASH, quotedPlatformSeparator);
        return returnString;
    }

    public static String fileSeparatorFromPlatformToSlash(String path) {
        if (PLATFORM_FILE_SEPARATOR.equals(SINGLE_SLASH)) {
            return path;
        }
        String quotedPlatformSeparator = addBackSlashesToFileSeparator(PLATFORM_FILE_SEPARATOR);
        String returnString = path.replaceAll(quotedPlatformSeparator, SINGLE_SLASH);
        return returnString;
    }

    public static String getRelativePath(File rootFolder, File targetFile) {
        String targetPath, rootPath;
        try {
            targetPath = targetFile.getCanonicalPath();
            rootPath = rootFolder.getCanonicalPath();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        String relativePath;
        if (targetPath.startsWith(rootPath)) {
            relativePath = targetPath.substring(rootPath.length());
        } else {
            relativePath = targetPath;
        }
        String fileSeparatorString = System.getProperty("file.separator");
        if (relativePath.startsWith(fileSeparatorString)) {
            return relativePath.substring(fileSeparatorString.length());
        } else return relativePath;
    }

    public static String getFileNameWithDifferntExtension(File original, String newExtension, boolean lastDotStartOfExtension, boolean dotIncluded) {
        String originalName = original.getName();
        int dotIndex;
        if (lastDotStartOfExtension) {
            dotIndex = originalName.lastIndexOf('.');
        } else {
            dotIndex = originalName.indexOf('.');
        }
        return originalName.substring(0, dotIndex) + (dotIncluded ? "." : "") + newExtension;
    }

    public static File getFileWithDifferntExtension(File original, String newExtension, boolean lastDotStartOfExtension, boolean dotIncluded) {
        File folder = original.getParentFile();
        String filename = getFileNameWithDifferntExtension(original, newExtension, lastDotStartOfExtension, dotIncluded);
        return new File(folder, filename);
    }

    public static String getFilename(String filepath) {
        int lastSlashIndex = filepath.lastIndexOf('/');
        int lastBackslashIndex = filepath.lastIndexOf('\\');
        int lastIndex = Math.max(lastSlashIndex, lastBackslashIndex);
        return filepath.substring(lastIndex + 1);
    }

    public static CharSequence readFrom(File file) {
        return readFrom(file, Charset.defaultCharset());
    }

    public static CharSequence readFrom(File file, Charset charset) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            char[] buf = new char[1024];
            for (int readSize = 0; (readSize = reader.read(buf)) >= 0; ) {
                builder.append(buf, 0, readSize);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    public static File getFileOfLastestVersion(File root, Pattern pattern) {
        File returnValue = null;
        String returnValueName = null;
        for (File file : root.listFiles()) {
            String fileName = file.getName();
            if (!pattern.matcher(fileName).matches()) {
                continue;
            }
            if (returnValue == null || returnValueName.compareTo(fileName) < 0) {
                returnValue = file;
                returnValueName = fileName;
                continue;
            }
        }
        return returnValue;
    }

    public static String getAbsoluteCanonicalPath(File file) {
        String s = null;
        try {
            s = file.getCanonicalFile().toURI().toURL().toExternalForm();
        } catch (Exception e) {
        }
        return s;
    }

    public static String getName(File file) {
        String currentPath = getAbsoluteCanonicalPath(file);
        if (currentPath.charAt(currentPath.length() - 1) == '/') {
            currentPath = currentPath.substring(0, currentPath.length() - 1);
        }
        int lastIndex = currentPath.lastIndexOf('/');
        return currentPath.substring(lastIndex + 1);
    }

    public static void deleteAllFilesAndSubFolder(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            file.delete();
            return;
        }
        for (File child : file.listFiles()) {
            deleteAllFilesAndSubFolder(child);
        }
        file.delete();
    }

    public static class FolderFilter implements FileFilter {

        private String[] folderNameArray;

        public FolderFilter(String[] folderNameArray) {
            this.folderNameArray = folderNameArray;
        }

        public boolean accept(File file) {
            if (!file.isDirectory()) {
                return false;
            }
            return Arrays.binarySearch(folderNameArray, file.getName()) >= 0;
        }
    }

    public static class NormalFileFilter implements FileFilter {

        public boolean accept(File file) {
            return file.isFile();
        }
    }

    public static void deleteSubFilesMatchingFilter(File root, FileFilter fileFilter) {
        deleteSubFilesMatchingFilter(root, fileFilter, false);
    }

    public static void deleteSubFilesMatchingFilter(File root, FileFilter fileFilter, boolean removeFolder) {
        if (root == null || !root.exists()) {
            return;
        }
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                if (removeFolder && fileFilter.accept(file)) {
                    deleteAllFilesAndSubFolder(file);
                } else {
                    deleteSubFilesMatchingFilter(file, fileFilter, removeFolder);
                }
            } else {
                if (fileFilter.accept(file)) {
                    file.delete();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        test07();
    }

    protected static void test07() {
        System.out.println("hello");
    }

    protected static void test06() throws IOException {
        StringBuilder builder = new StringBuilder();
        File file = new File("c:/temp/test.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String oneline; (oneline = reader.readLine()) != null; ) {
            oneline = oneline.trim();
            if (oneline.length() == 0) {
                continue;
            }
            builder.append(oneline).append(StringToolkit.newLine());
        }
        reader.close();
        FileToolkit.writeTo(file, builder);
    }

    protected static void test05() throws IOException {
        File here = new File(".");
        File target = new File(here, "hello\\bye");
        File root = new File(here.getCanonicalPath());
        System.out.println(getRelativePath(root, target));
    }

    protected static void test04() throws IOException {
        File file01 = new File(".");
        File file02 = new File(file01.getCanonicalPath());
        System.out.println(file02.getCanonicalPath());
        System.out.println(file02.getPath());
    }

    protected static void test03() {
        File wikiFolder = new File("C:/yeri/work/courses/200901/StatisticalMachineLearning/project/program/data/wikipedia/");
        for (File folder : wikiFolder.listFiles()) {
            if (!folder.isDirectory()) {
                continue;
            }
            if (folder.getName().startsWith("Copy of")) {
                continue;
            }
            System.out.println("Count of " + folder.getName() + ":" + folder.listFiles().length);
        }
    }

    public static List<File> getSubfolderFileList(File rootFolder, FileFilter fileFilter) {
        List<File> list = new ArrayList<File>();
        for (File file : rootFolder.listFiles()) {
            if (file.isDirectory()) {
                list.addAll(getSubfolderFileList(file, fileFilter));
            } else {
                if (fileFilter.accept(file)) {
                    list.add(file);
                }
            }
        }
        return list;
    }

    protected static void test02(String[] args) {
        File[] sourceFileArray = new File[] { new File("C:/yeri/work/courses/200901/InformationRetrieval/homework/hw03/program/code/src/com/yerihyo/cmu/ir/hw03/NetflixToolkit.java"), new File("C:/yeri/projects/yeritools/program/code/src/base/main/java/com/yerihyo/yeritools/CalendarToolkit.java"), new File("C:/yeri/projects/yeritools/program/code/src/base/main/java/com/yerihyo/yeritools/math/StatisticsToolkit.java"), new File("C:/yeri/projects/yeritools/program/code/src/base/main/java/com/yerihyo/yeritools/collections/CollectionsToolkit.java"), new File("C:/yeri/projects/yeritools/program/code/src/base/main/java/com/yerihyo/yeritools/collections/MapToolkit.java"), new File("C:/yeri/projects/yeritools/program/code/src/base/main/java/com/yerihyo/yeritools/text/StringToolkit.java") };
        File destFolder = new File("C:/yeri/work/courses/200901/InformationRetrieval/homework/hw03/program/sample_code");
        createReadableSourceCode(sourceFileArray, destFolder);
    }

    public static void runToolkit(String[] args) {
        String key = args[0];
        if (key.equalsIgnoreCase("clear source code")) {
        }
    }

    public static long getLineCount(File file) throws IOException {
        long lineCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while ((reader.readLine()) != null) {
            lineCount++;
        }
        reader.close();
        return lineCount;
    }

    public static void createReadableSourceCode(File file, File ofile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        PrintWriter writer = new PrintWriter(new FileWriter(ofile));
        String oneline = null;
        while ((oneline = reader.readLine()) != null) {
            writer.println(oneline.replace('\t', ' '));
        }
        reader.close();
        writer.close();
    }

    public static void createReadableSourceCode(File[] fileArray, File destFolder) {
        if (!destFolder.isDirectory()) {
            return;
        }
        for (File file : fileArray) {
            File ofile = new File(destFolder, file.getName());
            try {
                createReadableSourceCode(file, ofile);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public static void copy(File from, File to) throws IOException {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(to);
        int length;
        byte[] data = new byte[1024];
        while ((length = in.read(data)) > 0) {
            out.write(data, 0, length);
        }
        in.close();
        out.close();
    }

    public static void copyFiles(File rootFolder, File destinationFolder, String[] relativePaths) throws FileNotFoundException, IOException {
        if (!rootFolder.isDirectory()) {
            YeriDebug.ASSERT("Root Folder not a directory");
        }
        if (!destinationFolder.isDirectory()) {
            YeriDebug.ASSERT("Destination Folder not a directory");
        }
        for (String relativePath : relativePaths) {
            File sourceFile = new File(rootFolder, relativePath);
            File destinationFile = new File(destinationFolder, relativePath);
            destinationFile.getParentFile().mkdirs();
            copy(sourceFile, destinationFile);
        }
    }

    public static String[] getWordTypeArray(File file) {
        CharSequence cs = FileToolkit.readFrom(file);
        Pattern pattern = Pattern.compile("\\p{Space}");
        return pattern.split(cs);
    }

    public static Map<String, Integer> fileToStringIntegerMap(File file) throws IOException {
        Map<String, Integer> map = new HashMap<String, Integer>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String oneline; (oneline = reader.readLine()) != null; ) {
            oneline = oneline.trim();
            if (oneline.length() == 0) {
                continue;
            }
            String[] onelineArray = oneline.split("\\p{Space}+");
            map.put(onelineArray[0], Integer.parseInt(onelineArray[1]));
        }
        reader.close();
        return map;
    }

    public static void writeTo(File file, Map<String, ?> map) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);
        for (String key : map.keySet()) {
            writer.println(key + " " + map.get(key));
        }
        writer.close();
    }

    public static boolean compareString(Reader r1, Reader r2) throws IOException {
        char[] b1 = new char[1024];
        char[] b2 = new char[1024];
        BufferedReader reader1 = new BufferedReader(r1);
        BufferedReader reader2 = new BufferedReader(r2);
        while (true) {
            int l1 = reader1.read(b1);
            int l2 = reader2.read(b2);
            if (l1 != l2) {
                return false;
            }
            if (l1 < 0) {
                break;
            }
            if (!b1.equals(b2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean compareString(File f1, File f2, Charset charset) throws IOException {
        Reader r1 = new InputStreamReader(new FileInputStream(f1), charset);
        Reader r2 = new InputStreamReader(new FileInputStream(f2), charset);
        return compareString(r1, r2);
    }

    public static void mergeFiles(File[] fileArray, File ofile, boolean append) throws IOException {
        if (!append && ofile.exists()) {
            ofile.delete();
        }
        for (File file : fileArray) {
            concatenateToDestFile(file, ofile);
        }
    }

    public static File addExtensionIfNecessary(File file, String[] allowedExtensionArray, String preferredExtension) {
        String path = null;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int lastDotIndex = path.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return new File(path + "." + preferredExtension);
        } else if (CollectionsToolkit.indexOfEndsWith(allowedExtensionArray, path, true) < 0) {
            return new File(path + "." + preferredExtension);
        } else {
            return file;
        }
    }

    public static List<String> readAsStringList(File file, boolean trim, boolean removeEmptyLine) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> stringArray = new ArrayList<String>();
        for (String oneline = null; (oneline = reader.readLine()) != null; ) {
            if (trim) {
                oneline = oneline.trim();
            }
            if (removeEmptyLine && oneline.length() == 0) {
                continue;
            }
            stringArray.add(oneline);
        }
        reader.close();
        return stringArray;
    }

    public static void serializeTo(File file, Serializable s) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(s);
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object deserializeFrom(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
