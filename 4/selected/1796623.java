package com.gdj.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import com.gdj.gui.frames.SearchAndReplace;

/**
 * @author Glenn Dejaeger
 */
public class ReplacementTools {

    private static final String newline = "\n";

    private static final String newfileline = "\r\n";

    /**
	 * Search for files in specified dir that match the pattern If
	 * replace==true, edit the files in the dir
	 */
    public static int searchOrEditFiles(File dir, String pattern, boolean replace) {
        List files;
        int totalFiles = 0;
        try {
            if (dir != null) {
                files = getFileListing(dir, pattern);
                Iterator filesIter = files.iterator();
                while (filesIter.hasNext()) {
                    String fileName = filesIter.next().toString();
                    String replaceText = checkFile(fileName, SearchAndReplace.userActionPanel.getToReplaceArea().getText(), SearchAndReplace.userActionPanel.getToReplaceByArea().getText(), replace);
                    if (replaceText != null && !replaceText.equals("Failure") && !fileName.endsWith(".bak")) {
                        SearchAndReplace.actionLogArea.append(fileName + ": " + replaceText + newline);
                        totalFiles++;
                    } else if (replaceText != null && replaceText.equals("Failure")) {
                        SearchAndReplace.actionLogArea.append("Failure while performing action!");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
        }
        return totalFiles;
    }

    /**
	 * Count the files and print them out in action log area
	 */
    public static int countFiles(File dir, String pattern, boolean showInArea) {
        List files;
        int totalFiles = 0;
        try {
            if (dir != null) {
                files = getFileListing(dir, pattern);
                Iterator filesIter = files.iterator();
                while (filesIter.hasNext()) {
                    String fileName = filesIter.next().toString();
                    if (showInArea) {
                        SearchAndReplace.actionLogArea.append(fileName + newline);
                    }
                    totalFiles++;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
        }
        return totalFiles;
    }

    /**
	 * Get all the files in a directory that match the pattern
	 */
    private static List getFileListing(File aStartingDir, String pattern) throws FileNotFoundException {
        if (pattern == null) {
            pattern = "";
        }
        validateDirectory(aStartingDir);
        List result = new ArrayList();
        File[] filesAndDirs = aStartingDir.listFiles();
        List filesDirs = Arrays.asList(filesAndDirs);
        Iterator filesIter = filesDirs.iterator();
        File file = null;
        while (filesIter.hasNext()) {
            file = (File) filesIter.next();
            if (FileFilter.isCorrectFilePattern(file, pattern)) {
                result.add(file);
            }
            if (!file.isFile() && SearchAndReplace.userActionPanel.getSubDirCheckBox().isSelected()) {
                List deeperList = getFileListing(file, pattern);
                result.addAll(deeperList);
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
	 * Validate the specified directory
	 */
    private static void validateDirectory(File dir) throws FileNotFoundException {
        if (dir == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!dir.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + dir);
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + dir);
        }
        if (!dir.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + dir);
        }
    }

    /**
	 * Check file, and replace input eventually (if replace==true)
	 */
    private static String checkFile(String path, String toReplace, String toReplaceBy, boolean replace) throws FileNotFoundException {
        String out = "";
        try {
            File file = new File(path);
            if (!file.isDirectory()) {
                if (!file.canRead()) {
                    out = "Cannot read from file!";
                }
                if (replace) {
                    if (!file.canWrite()) {
                        out = "Cannot write to file!";
                    }
                    if (file.canRead() && file.canWrite()) {
                        if (lookForInput(file, toReplace, toReplaceBy, true)) {
                            out = "Text replaced successfully.";
                        } else {
                            out = null;
                        }
                    }
                } else {
                    if (file.canRead()) {
                        if (lookForInput(file, toReplace, toReplaceBy, false)) {
                            out = "Text found in file.";
                        } else {
                            out = null;
                        }
                    }
                }
            } else {
                out = null;
            }
        } catch (FileNotFoundException e) {
            out = "Failure";
            System.out.println("FAILURE");
        } catch (IOException ioe) {
            out = "Failure";
            System.out.println("IO FAILUE");
        }
        return out;
    }

    /**
	 * Create a backup of the file (.bak file)
	 */
    private static void backupFile(File file) {
        FileChannel in = null, out = null;
        try {
            if (!file.getName().endsWith(".bak")) {
                in = new FileInputStream(file).getChannel();
                out = new FileOutputStream(new File(file.toString() + ".bak")).getChannel();
                long size = in.size();
                MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                out.write(buf);
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                System.gc();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    /**
	 * Look for input in the file and replace eventually
	 */
    private static boolean lookForInput(File file, String toReplace, String toReplaceBy, boolean replace) throws FileNotFoundException, IOException {
        boolean success = false;
        StringBuffer fileContent = new StringBuffer();
        FileInputStream ifstream = new FileInputStream(file);
        ResourceBundle userBundle = ResourceBundle.getBundle("config/user");
        String backupNeeded = userBundle.getString("backup_files");
        if (backupNeeded != null && (backupNeeded.equals("true") || backupNeeded.equals(""))) {
            backupFile(file);
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(ifstream));
            String line = "";
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (i != 0) {
                    fileContent.append(newline);
                }
                fileContent.append(line);
                i++;
            }
            br.close();
            String fileContentStr = fileContent.toString();
            if (fileContentStr.indexOf(toReplace) >= 0) {
                success = true;
            } else if (fileContentStr.indexOf(toReplace) < 0) {
                success = false;
            }
            if (replace) {
                try {
                    if ((SearchAndReplace.userActionPanel.getRegexRadio().isSelected() || (SearchAndReplace.userActionPanel.getPlainTextRadio().isSelected() && fileContentStr.indexOf(toReplace) >= 0)) && !file.getName().endsWith(".bak")) {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                        if (SearchAndReplace.userActionPanel.getPlainTextRadio().isSelected()) {
                            toReplace = forRegex(toReplace);
                        }
                        fileContentStr = fileContentStr.replaceAll(toReplace, toReplaceBy);
                        fileContentStr = fileContentStr.replaceAll(newline, newfileline);
                        bw.write(fileContentStr, 0, fileContentStr.length());
                        success = true;
                        bw.close();
                    } else if (fileContentStr.indexOf(toReplace) < 0) {
                        success = false;
                    }
                } catch (IOException e) {
                    success = false;
                    System.out.println("IOException");
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException fex) {
            success = false;
            System.out.println("FileNotFound");
        } catch (IOException ioe) {
            success = false;
            System.out.println("IOException");
        }
        return success;
    }

    /**
	 * If regular expressions are not used, all special literals should be
	 * escaped by double backslashes
	 */
    private static String forRegex(String aRegexFragment) {
        final StringBuffer result = new StringBuffer();
        final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
        char character = iterator.current();
        while (character != StringCharacterIterator.DONE) {
            if (character == '.') {
                result.append("\\.");
            } else if (character == '\\') {
                result.append("\\\\");
            } else if (character == '?') {
                result.append("\\?");
            } else if (character == '*') {
                result.append("\\*");
            } else if (character == '+') {
                result.append("\\+");
            } else if (character == '&') {
                result.append("\\&");
            } else if (character == ':') {
                result.append("\\:");
            } else if (character == '{') {
                result.append("\\{");
            } else if (character == '}') {
                result.append("\\}");
            } else if (character == '[') {
                result.append("\\[");
            } else if (character == ']') {
                result.append("\\]");
            } else if (character == '(') {
                result.append("\\(");
            } else if (character == ')') {
                result.append("\\)");
            } else if (character == '^') {
                result.append("\\^");
            } else if (character == '$') {
                result.append("\\$");
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }
}
