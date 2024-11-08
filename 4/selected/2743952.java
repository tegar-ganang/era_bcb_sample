package ca.ucalgary.cpsc.ase.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class FileUtils.
 */
public class FileUtils {

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static void copyDir(File from, File to) throws IOException {
        assert (from.isDirectory());
        assert (to.isDirectory());
        List<File> files = getAllFiles(from);
        for (File fileFrom : files) {
            String toPath = fileFrom.getAbsolutePath().replace(from.getAbsolutePath(), to.getAbsolutePath());
            File fileTo = new File(toPath);
            fileTo.getParentFile().mkdirs();
            copy(fileFrom, fileTo);
        }
    }

    public static String getSlashNotation(String dotNotation) {
        if (dotNotation.charAt(0) == '.') return "." + File.separatorChar + dotNotation.substring(1); else return dotNotation.replace('.', File.separator.charAt(0));
    }

    public static String readAll(java.io.File file) throws IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader input = new BufferedReader(new FileReader(file));
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
            if (contents.length() >= 2) {
                contents.deleteCharAt(contents.length() - 1);
            }
        } finally {
            input.close();
        }
        return contents.toString();
    }

    public static void write(String text, java.io.File file) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        try {
            output.write(text);
        } finally {
            output.close();
        }
    }

    public static void copy(java.io.File fromFile, java.io.File toFile) throws IOException {
        FileUtils.write(FileUtils.readAll(fromFile), toFile);
    }

    public static List<java.io.File> getAllFiles(java.io.File dir) {
        List<java.io.File> result = new ArrayList<java.io.File>();
        if (dir.isFile()) {
            result.add(dir);
        } else if (dir.isDirectory() && !dir.getName().matches("^\\..*")) {
            for (java.io.File file : dir.listFiles()) {
                result.addAll(getAllFiles(file));
            }
        }
        return result;
    }
}
