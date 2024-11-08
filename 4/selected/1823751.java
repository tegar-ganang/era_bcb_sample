package net.sourceforge.pmd.build.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.pmd.build.PmdBuildException;

/**
 * @author Romain Pelisse <belaran@gmail.com>
 *
 */
public final class FileUtil {

    public static String pathToParent = "..";

    private FileUtil() {
    }

    ;

    public static Set<File> listFilesFrom(File dir) {
        return filterFilesFrom(dir, null);
    }

    public static Set<File> filterFilesFrom(File dir, FilenameFilter filter) {
        Set<File> filteredFiles = new HashSet<File>(0);
        if (dir != null) {
            File[] files = dir.listFiles(filter);
            if (files != null && files.length > 0) for (int fileIterator = 0; fileIterator < files.length; fileIterator++) filteredFiles.add(files[fileIterator]);
        }
        return filteredFiles;
    }

    public static File existAndIsADirectory(String dirname) {
        File rulesDir = new File(dirname);
        return (rulesDir.exists() && rulesDir.isDirectory()) ? rulesDir : null;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public static void ensureTargetDirectoryExist(File filename) throws PmdBuildException {
        File parentDir = filename.getParentFile();
        if (parentDir == null) throw new PmdBuildException("No parent directory for " + filename.getAbsolutePath());
        if (!parentDir.exists()) parentDir.mkdirs();
    }

    public static File createDirIfMissing(String dirname) {
        File dir = new File(dirname);
        if ((!dir.exists() && !dir.mkdirs())) {
            throw new IllegalStateException("Target directory '" + dir.getAbsolutePath() + "' does not exist and can't be created");
        } else if (dir.exists() && dir.isFile()) {
            throw new IllegalStateException("Target directory '" + dir.getAbsolutePath() + "' already exist and is a file.");
        }
        return dir;
    }

    public static void deleteFile(File file) {
        if (!file.isDirectory()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for (int nbFile = 0; nbFile < files.length; nbFile++) FileUtil.deleteFile(files[nbFile]);
            file.delete();
        }
    }

    public static void replaceAllInFile(File file, String pattern, String replacement) {
        File tmp = new File(file + ".tmp");
        try {
            String line;
            FileWriter fw = new FileWriter(tmp);
            FileReader fr = new FileReader(file);
            BufferedWriter bw = new BufferedWriter(fw);
            BufferedReader br = new BufferedReader(fr);
            while (br.ready()) {
                line = br.readLine();
                line = line.replaceAll(pattern, replacement);
                bw.write(line);
            }
            fr.close();
            bw.flush();
            fw.close();
            FileUtil.copy(tmp, file);
            tmp.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File createTempFile(String filename) {
        try {
            return File.createTempFile(filename + "-", ".tmp");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static File move(File source, File target) {
        if (target.exists()) {
            if (!target.canWrite()) throw new IllegalArgumentException("Can't write on existing file " + target.getAbsolutePath());
        } else {
            if (target.delete()) throw new IllegalStateException("Can't delete file" + target.getAbsolutePath());
        }
        try {
            target.createNewFile();
            copy(source, target);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't copy " + source.getAbsolutePath() + " over " + target.getAbsolutePath());
        }
        return target;
    }

    public static InputStream createInputStream(String filepath) {
        if (filepath == null || "".equals(filepath)) return null;
        File file = new File(filepath);
        if (!file.exists()) return null;
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
