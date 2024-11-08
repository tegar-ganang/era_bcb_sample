package org.web3d.x3d.tools;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.regex.*;

/**
 *
 * @author  brutzman
 */
public class X3dDoctypeChecker {

    public static String warningToken = "[Warning]";

    public static String errorToken = "[Error]";

    static String UsageMessage = "usage: java X3dDoctypeChecker sceneName.x3d [-verbose | -setFinalDTD | -setTransitionalDTD]";

    static boolean setTransitionalDTD = false;

    static boolean setFinalDTD = false;

    static boolean foundNo_DTD = false;

    static boolean foundTransitional_30_DTD = false;

    static boolean foundTransitional_31_DTD = false;

    static boolean foundFinal_30_DTD = false;

    static boolean foundFinal_31_DTD = false;

    static boolean foundFinal_32_DTD = false;

    static boolean foundFinal_33_DTD = false;

    static boolean readOnlyFile = false;

    static boolean saveFile = true;

    static boolean verbose = false;

    String sceneText = new String();

    String headerText = new String();

    String revisedScene = new String();

    String outputLog = new String();

    final String TRANSITIONAL_30_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"http://www.web3d.org/specifications/x3d-3.0.dtd\" \"file:///www.web3d.org/TaskGroups/x3d/translation/x3d-3.0.dtd\"";

    final String TRANSITIONAL_31_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"http://www.web3d.org/specifications/x3d-3.1.dtd\" \"file:///www.web3d.org/TaskGroups/x3d/translation/x3d-3.1.dtd\"";

    final String FINAL_30_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.0//EN\" \"http://www.web3d.org/specifications/x3d-3.0.dtd\"";

    final String FINAL_31_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.1//EN\" \"http://www.web3d.org/specifications/x3d-3.1.dtd\"";

    final String FINAL_32_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.2//EN\" \"http://www.web3d.org/specifications/x3d-3.2.dtd\"";

    final String FINAL_33_DOCTYPE = "<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.3//EN\" \"http://www.web3d.org/specifications/x3d-3.3.dtd\"";

    final String WarningComment = "<!--Warning:  transitional DOCTYPE in source .x3d file-->\n";

    final String WarningRegex = "<!--Warning:  transitional DOCTYPE in source \\.x3d file-->(\\s)*";

    FileInputStream fis;

    RandomAccessFile raf;

    FileChannel fc;

    ByteBuffer bb;

    /**
 * extracts the content of a file
 * @param fileName the name of the file to extract
 * @return String representing contents of the file
 */
    public String getFileContent(String fileName) {
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException fnf) {
            addLogEntryLine(errorToken + " [X3dDoctypeChecker] scene \"" + fileName + "\" not found.");
            addLogEntryLine(UsageMessage);
            saveFile = false;
            return "";
        }
        try {
            raf = new RandomAccessFile(fileName, "rwd");
            fc = raf.getChannel();
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
            bb.flip();
        } catch (IOException ioe) {
            readOnlyFile = true;
        }
        if (raf == null) try {
            raf = new RandomAccessFile(fileName, "r");
            fc = raf.getChannel();
            bb = ByteBuffer.allocate((int) fc.size());
            fc.read(bb);
            bb.flip();
            addLogEntryLine(warningToken + " [X3dDoctypeChecker] " + fileName + " file is read-only.");
        } catch (IOException ioe) {
            addLogEntryLine(errorToken + " [X3dDoctypeChecker] unable to read scene \"" + fileName + "\".");
            System.out.println(outputLog);
            ioe.printStackTrace();
            return "";
        }
        String returnString = new String(bb.array());
        bb = null;
        return returnString;
    }

    /**
 * resets the content of a file
 * @param revisedScene content being reset
 */
    public void setFileContent(String revisedScene) {
        try {
            bb = ByteBuffer.wrap(revisedScene.getBytes());
            fc.truncate(revisedScene.length());
            fc.position(0);
            fc.write(bb);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
 * @param args the command line arguments
 */
    public static void main(String[] args) {
        String x3dFile = new String();
        if ((args != null) && (args.length >= 1) && (args.length <= 2)) {
            x3dFile = args[0];
        } else {
            System.out.println(UsageMessage);
            return;
        }
        if (args != null && args.length > 1) {
            for (int i = 1; i <= args.length - 1; i++) {
                if ((args[i].compareTo("-v") == 0) || (args[i].compareTo("-verbose") == 0)) {
                    verbose = true;
                } else if ((args[i].compareTo("-f") == 0) || (args[i].compareTo("-setFinalDTD") == 0)) {
                    setFinalDTD = true;
                } else if ((args[i].compareTo("-t") == 0) || (args[i].compareTo("-setTransitionalDTD") == 0)) {
                    setTransitionalDTD = true;
                } else {
                    System.out.println(errorToken + " [X3dDoctypeChecker] unrecognized command-line option \"" + args[i] + "\"");
                    System.out.println(UsageMessage);
                    return;
                }
            }
        }
        if (setFinalDTD && setTransitionalDTD) {
            System.out.println(errorToken + " [X3dDoctypeChecker] both -setFinalDTD and -setTransitionalDTD specified,");
            System.out.println("        only one operation allowed.");
            System.out.println(UsageMessage);
            return;
        }
        X3dDoctypeChecker doctypeChecker = new X3dDoctypeChecker();
        String log = doctypeChecker.processScene(x3dFile);
        System.out.println(log);
    }

    /**
 * processes the scene to check DOCTYPE
 * @param x3dFileName is path and file name of X3D content to be processed
 * @return outputLog providing processing results
 */
    public String processScene(String x3dFileName) {
        outputLog = "";
        sceneText = getFileContent(x3dFileName);
        if (sceneText == null) {
            addLogEntryLine("[X3dDoctypeChecker] failure: file read unsuccessful for " + x3dFileName);
            return outputLog;
        } else if (sceneText.length() == 0) {
            addLogEntryLine("[X3dDoctypeChecker] failure: empty file " + x3dFileName);
            return outputLog;
        }
        int indexX3D = sceneText.indexOf("<X3D", 0);
        if (indexX3D > 0) headerText = sceneText.substring(0, indexX3D).trim(); else {
            addLogEntryLine(errorToken + "[X3dDoctypeChecker] failure: no <X3D> element found");
            headerText = sceneText;
        }
        String regexXmlHeader = "<\\?xml version=(\"|')1.(0|1)(\"|') encoding=(\"|')UTF-8(\"|')\\?>";
        Pattern patternXmlHeader = Pattern.compile(regexXmlHeader);
        Matcher matcherXmlHeader = patternXmlHeader.matcher(sceneText);
        String regexXmlHeaderUtfIgnoreCase = "<\\?xml version=(\"|')1.(0|1)(\"|') encoding=(\"|')(U|u)(T|t)(F|f)-8(\"|')\\?>";
        Pattern patternXmlHeaderUtfIgnoreCase = Pattern.compile(regexXmlHeaderUtfIgnoreCase);
        Matcher matcherXmlHeaderUtfIgnoreCase = patternXmlHeaderUtfIgnoreCase.matcher(sceneText);
        if (matcherXmlHeader.find()) {
            addLogEntry("[X3dDoctypeChecker] success: valid XML declaration found; ");
        } else if (matcherXmlHeaderUtfIgnoreCase.find()) {
            addLogEntry("[X3dDoctypeChecker] failure: invalid XML declaration found (encoding='UTF-8' must be upper case); ");
        } else {
            addLogEntryLine(errorToken + " [X3dDoctypeChecker] failure: no valid XML declaration found in scene!");
            addLogEntryLine(headerText);
            foundNo_DTD = true;
            addLogEntryLine(UsageMessage);
            if (!setFinalDTD && !setTransitionalDTD) {
                return outputLog;
            }
        }
        String regexFinal30Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"ISO//Web3D//DTD X3D 3.0//EN\"(\\s)+\"http://www.web3d.org/specifications/x3d-3.0.dtd\"(\\s)*(>|\\[)";
        Pattern patternFinal30Doctype = Pattern.compile(regexFinal30Doctype);
        Matcher matcherFinal30Doctype = patternFinal30Doctype.matcher(sceneText);
        String regexFinal31Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"ISO//Web3D//DTD X3D 3.1//EN\"(\\s)+\"http://www.web3d.org/specifications/x3d-3.1.dtd\"(\\s)*(>|\\[)";
        Pattern patternFinal31Doctype = Pattern.compile(regexFinal31Doctype);
        Matcher matcherFinal31Doctype = patternFinal31Doctype.matcher(sceneText);
        String regexFinal32Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"ISO//Web3D//DTD X3D 3.2//EN\"(\\s)+\"http://www.web3d.org/specifications/x3d-3.2.dtd\"(\\s)*(>|\\[)";
        Pattern patternFinal32Doctype = Pattern.compile(regexFinal32Doctype);
        Matcher matcherFinal32Doctype = patternFinal32Doctype.matcher(sceneText);
        String regexFinal33Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"ISO//Web3D//DTD X3D 3.3//EN\"(\\s)+\"http://www.web3d.org/specifications/x3d-3.3.dtd\"(\\s)*(>|\\[)";
        Pattern patternFinal33Doctype = Pattern.compile(regexFinal33Doctype);
        Matcher matcherFinal33Doctype = patternFinal33Doctype.matcher(sceneText);
        String regexTransitional30Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"http://www.web3d.org/specifications/x3d-3.0.dtd\"(\\s)+\"file:///www.web3d.org/TaskGroups/x3d/translation/x3d-3.0.dtd\"(\\s)*(>|\\[)";
        Pattern patternTransitional30Doctype = Pattern.compile(regexTransitional30Doctype);
        Matcher matcherTransitional30Doctype = patternTransitional30Doctype.matcher(sceneText);
        String regexTransitional31Doctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC(\\s)+\"http://www.web3d.org/specifications/x3d-3.1.dtd\"(\\s)+\"file:///www.web3d.org/TaskGroups/x3d/translation/x3d-3.1.dtd\"(\\s)*(>|\\[)";
        Pattern patternTransitional31Doctype = Pattern.compile(regexTransitional31Doctype);
        Matcher matcherTransitional31Doctype = patternTransitional31Doctype.matcher(sceneText);
        String regexAnyDoctype = "[^<][^!][^-][^-](\\s)?<!DOCTYPE X3D PUBLIC";
        Pattern patternAnyDoctype = Pattern.compile(regexAnyDoctype);
        Matcher matcherAnyDoctype = patternAnyDoctype.matcher(sceneText);
        if (matcherFinal33Doctype.find()) {
            foundFinal_33_DTD = true;
            addLogEntryLine("success: final X3D 3.3 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherFinal32Doctype.find()) {
            foundFinal_32_DTD = true;
            addLogEntryLine("success: final X3D 3.2 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherFinal31Doctype.find()) {
            foundFinal_31_DTD = true;
            addLogEntryLine("success: final X3D 3.1 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherFinal30Doctype.find()) {
            foundFinal_30_DTD = true;
            addLogEntryLine("success: final X3D 3.0 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherTransitional30Doctype.find()) {
            foundTransitional_30_DTD = true;
            addLogEntryLine("warning: transitional X3D 3.0 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherTransitional31Doctype.find()) {
            foundTransitional_31_DTD = true;
            addLogEntryLine("warning: transitional X3D 3.1 DOCTYPE found.");
            if (verbose) addLogEntryLine(headerText);
        } else if (matcherAnyDoctype.find()) {
            addLogEntryLine("\n" + errorToken + " failure: nonstandard X3D DOCTYPE found!");
            addLogEntryLine(headerText);
            return outputLog;
        } else {
            addLogEntryLine("\n" + errorToken + " failure: no X3D DOCTYPE found!");
            addLogEntryLine(headerText);
            foundNo_DTD = true;
            if (!setFinalDTD && !setTransitionalDTD) {
                return outputLog;
            }
        }
        matcherAnyDoctype.reset();
        int matchCount = 0;
        while (matcherAnyDoctype.find()) {
            matchCount++;
        }
        if (matchCount > 1) {
            addLogEntryLine(warningToken + " Multiple X3D DOCTYPEs found (" + matchCount + " total).");
            if ((setFinalDTD || setTransitionalDTD) && (readOnlyFile == false)) {
                addLogEntryLine("No DTD conversion attempted.");
            }
            addLogEntryLine(headerText);
            return outputLog;
        }
        if (readOnlyFile) {
            return outputLog;
        }
        if (setFinalDTD) System.out.print("[X3dDoctypeChecker] set final DTD:  "); else if (setTransitionalDTD) System.out.print("[X3dDoctypeChecker] set transitional DTD:  ");
        if (setFinalDTD && foundTransitional_30_DTD) {
            matcherTransitional30Doctype.reset();
            revisedScene = matcherTransitional30Doctype.replaceFirst(FINAL_30_DOCTYPE);
            revisedScene = revisedScene.replaceAll(WarningRegex, "");
            addLogEntryLine("scene reset to final X3D 3.0 DTD.");
            addLogEntryLine(FINAL_30_DOCTYPE + ">");
            saveFile = true;
        } else if (setFinalDTD && foundTransitional_31_DTD) {
            matcherTransitional31Doctype.reset();
            revisedScene = matcherTransitional31Doctype.replaceFirst(FINAL_31_DOCTYPE);
            revisedScene = revisedScene.replaceAll(WarningRegex, "");
            addLogEntryLine("scene reset to final X3D 3.1 DTD.");
            addLogEntryLine(FINAL_31_DOCTYPE + ">");
            saveFile = true;
        } else if (setTransitionalDTD && foundFinal_30_DTD) {
            matcherFinal30Doctype.reset();
            revisedScene = matcherFinal30Doctype.replaceFirst(WarningComment + TRANSITIONAL_30_DOCTYPE);
            addLogEntryLine("scene reset to transitional X3D DTD.");
            addLogEntryLine(TRANSITIONAL_30_DOCTYPE + ">");
            saveFile = true;
        } else if (setTransitionalDTD && foundFinal_31_DTD) {
            matcherFinal31Doctype.reset();
            revisedScene = matcherFinal31Doctype.replaceFirst(WarningComment + TRANSITIONAL_31_DOCTYPE);
            addLogEntryLine("scene reset to transitional X3D DTD.");
            addLogEntryLine(TRANSITIONAL_31_DOCTYPE + ">");
            saveFile = true;
        } else if (foundNo_DTD) {
            addLogEntryLine("no action taken, functionality not implemented...");
            saveFile = false;
        } else if (setFinalDTD || setTransitionalDTD) {
            addLogEntryLine("no action necessary.");
            saveFile = false;
        }
        saveFileIfSet();
        return outputLog.trim();
    }

    public void saveFileIfSet() {
        if (saveFile == false) return;
        if (setFinalDTD || setTransitionalDTD) setFileContent(revisedScene);
        try {
            if (fc != null) {
                raf.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    protected void addLogEntry(String newString) {
        outputLog += newString;
    }

    protected void addLogEntryLine(String newString) {
        outputLog += newString + "\n";
    }
}
