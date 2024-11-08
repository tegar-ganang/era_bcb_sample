package itjava.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

public class Convertor {

    public static String FileToString(String fileSource) throws Exception {
        String fileContent = "";
        BufferedReader bReader = new BufferedReader(new FileReader(fileSource));
        String currLine = bReader.readLine();
        while (currLine != null) {
            if (!currLine.trim().startsWith("//")) {
                fileContent += currLine;
            }
            currLine = bReader.readLine();
        }
        bReader.close();
        return fileContent;
    }

    /**
	 * Removes empty lines from the code. Also replaces multiple whitespace characters with single space.
	 * @param rawCode
	 * @return
	 */
    public static String FormatCode(String rawCode) {
        String formattedCode = "";
        try {
            BufferedReader bReader = new BufferedReader(new StringReader(rawCode));
            String currLine = bReader.readLine();
            while (currLine != null) {
                if (currLine.trim().length() > 0) {
                    if (!currLine.trim().startsWith("//")) {
                        if (currLine.contains("//")) {
                            int commentStartIndex = currLine.indexOf("//");
                            currLine = currLine.substring(0, commentStartIndex);
                        }
                        formattedCode += currLine.trim().replaceAll("\\s+", " ");
                    }
                }
                currLine = bReader.readLine();
            }
            bReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedCode;
    }

    public static ArrayList<String> StringToArrayListOfStrings(String linesOfCode) throws IOException {
        ArrayList<String> _linesOfCode = new ArrayList<String>();
        BufferedReader bReader = new BufferedReader(new StringReader(linesOfCode.replace("{", "{\n").replace(";", ";\n").replace("}", "}\n")));
        String currLine = bReader.readLine();
        while (currLine != null) {
            _linesOfCode.add(currLine);
            currLine = bReader.readLine();
        }
        return _linesOfCode;
    }

    public static String ArrayListOfStringsToString(ArrayList<String> linesOfCodes) {
        String stringOfCode = null;
        Iterator<String> itLines = linesOfCodes.iterator();
        if (itLines.hasNext()) {
            stringOfCode = itLines.next();
        }
        while (itLines.hasNext()) {
            stringOfCode += "\n" + itLines.next();
        }
        return stringOfCode;
    }

    public static ArrayList<String> TrimArrayListOfString(ArrayList<String> linesOfCode) {
        ArrayList<String> _linesOfCode = new ArrayList<String>();
        for (String line : linesOfCode) {
            _linesOfCode.add(line.trim().replace("\n", ""));
        }
        return _linesOfCode;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }
}
