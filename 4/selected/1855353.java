package org.edits;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Milen Kouylekov
 */
public class FileTools {

    public static void checkDirectory(String filename) throws Exception {
        File file = new File(filename);
        if (!validFileName(file)) throw new Exception("Filename not acceptable!");
        if (!file.exists()) throw new Exception("The directory " + filename + " does not exist!");
        if (!file.canRead()) throw new Exception("The system can not read from directory " + filename + "!");
    }

    public static void checkFile(String filename) throws Exception {
        File file = new File(filename);
        if (!validFileName(file)) throw new Exception("Filename not acceptable!");
        if (!file.exists()) throw new Exception("The file " + filename + " does not exist!");
        if (!file.canRead()) throw new Exception("The system can not read from file " + filename + "!");
    }

    public static void checkOutput(String filename, boolean overwrite) throws Exception {
        File file = new File(filename);
        if (!overwrite && file.exists()) throw new Exception("The file " + filename + " already exists!");
        if (file.exists() && (file.isDirectory() || !file.canWrite())) throw new Exception("The system can not write in " + filename + "!");
    }

    public static List<String> inputFiles(List<String> all) throws Exception {
        List<String> out = new ArrayList<String>();
        for (String s : all) getAllFiles(new File(s), out);
        return out;
    }

    public static List<String> inputFiles(String[] all) throws Exception {
        List<String> out = new ArrayList<String>();
        for (String s : all) getAllFiles(new File(s), out);
        return out;
    }

    public static List<List<String>> loadCSV(String filename, String encoding, String delimeter) throws Exception {
        List<List<String>> out = new ArrayList<List<String>>();
        checkFile(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
        String line = null;
        while ((line = in.readLine()) != null) {
            String[] xx = line.split(delimeter);
            List<String> xc = new ArrayList<String>();
            for (String x : xx) xc.add(x.equals("XXX") || x.length() == 0 ? null : x);
            out.add(xc);
        }
        in.close();
        return out;
    }

    public static List<String> loadList(String filename, String encoding) throws Exception {
        checkFile(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
        String line = null;
        List<String> vud = new ArrayList<String>();
        while ((line = in.readLine()) != null) {
            vud.add(line);
        }
        in.close();
        return vud;
    }

    public static Map<String, Double> loadNumberMap(String filename, String encoding) throws Exception {
        checkFile(filename);
        Map<String, Double> out = new HashMap<String, Double>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.length() == 0) continue;
            line = line.trim();
            String key = line.substring(0, line.indexOf("\t"));
            String value = line.substring(line.indexOf("\t") + 1);
            out.put(key, new Double(value));
        }
        in.close();
        return out;
    }

    public static Set<String> loadSet(String filename, String encoding) throws Exception {
        checkFile(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
        String line = null;
        Set<String> vud = new HashSet<String>();
        while ((line = in.readLine()) != null) {
            vud.add(line);
        }
        in.close();
        return vud;
    }

    public static String loadString(String filename) throws Exception {
        return loadString(filename, "UTF8");
    }

    public static String loadString(String filename, String encoding) throws Exception {
        checkFile(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
        String line = null;
        StringBuilder vud = new StringBuilder();
        while ((line = in.readLine()) != null) {
            vud.append(line + "\n");
        }
        in.close();
        return vud.toString();
    }

    public static void main(String[] args) throws Exception {
        saveString("/tcc0/tcc/kouylekov/.edits/txi", "eeeeeeeeeeeeeee", true);
    }

    public static void saveString(String filename, String s, boolean overwrite) throws Exception {
        saveString(filename, s, overwrite, "UTF-8");
    }

    public static void saveString(String filename, String s, boolean overwrite, String encoding) throws Exception {
        try {
            checkOutput(filename, overwrite);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), encoding));
            out.write(s);
            out.close();
        } catch (IOException e) {
            throw new Exception("The system can not write in " + filename + " because:\n" + e.getMessage());
        }
    }

    public static boolean validFileName(File file) {
        return !(file.getName().startsWith(".") || file.getName().endsWith(".save") || file.getName().endsWith("#") || file.getName().endsWith("~"));
    }

    private static void getAllFiles(File file, List<String> all) throws Exception {
        if (!validFileName(file)) return;
        FileTools.checkFile(file.getAbsolutePath());
        if (file.isDirectory()) {
            for (File f : file.listFiles()) getAllFiles(f, all);
            return;
        }
        all.add(file.getAbsolutePath());
    }
}
