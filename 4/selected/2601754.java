package dream.setup;

import java.util.Properties;
import java.io.*;
import javax.swing.*;
import java.awt.*;

/**
 *
 * This class will only be valuable on Windows systems on which the 'regedit'
 * commands exists. It reads and writes from and to the Windows registry using
 * the different options of this command. All the read methods may display
 * a progress bar if reading the registry takes too long.
 *
 * @author  mike
 * @version 0.2
 *
 * version 0.2: added line separator
 */
public abstract class RegEdit {

    /** Forbidden constructor there is no RegEdit object. */
    protected RegEdit() {
    }

    /** If set, a progress viewer will be shown while reading from the registry.
     *  The string below defines the message to the user. */
    public static Component progressMonitorParent = null;

    /** The string defines the message to the user while showing a progress monitor. */
    public static String progressMessage = "Reading from Windows registry.";

    /** Store local system line separator String for class local access. */
    private static String lineSep = System.getProperty("line.separator");

    /** Gets the whole key contents from the windows registry, that is a collection
     *  of names and their values, packed into a Properties table. */
    public static Properties getKey(String keyName) {
        Runtime run = Runtime.getRuntime();
        Properties answer = null;
        int result = -1;
        try {
            File regeditFile = File.createTempFile("drmread", ".reg");
            Process regProcess = run.exec("regedit.exe /s /e " + regeditFile.getAbsolutePath() + " " + keyName);
            regProcess.waitFor();
            result = regProcess.exitValue();
            answer = getKey(regeditFile, keyName, null);
            regeditFile.delete();
        } catch (Exception ex) {
            result = -1;
            System.err.println(ex);
        }
        return answer;
    }

    /** Filteres unprintable characters from the given line. */
    private static String filterLine(String line) {
        if (line == null) return "";
        StringBuffer filter = new StringBuffer(line);
        for (int i = filter.length() - 1; i >= 0; i--) {
            if (filter.charAt(i) <= 0) filter.deleteCharAt(i);
        }
        return new String(filter);
    }

    /** Gets the whole key contents from the windows registry, that is a collection
     *  of names and their values, packed into a Properties table. */
    private static Properties getKey(File regeditFile, String keyName, String encoding) {
        Properties answer = new Properties();
        String maskedKey = "[" + stripOuterQuotes(keyName) + "]";
        boolean keyFound = false;
        try {
            InputStream pIn = null;
            if (progressMonitorParent != null) {
                pIn = new ProgressMonitorInputStream(progressMonitorParent, progressMessage, new FileInputStream(regeditFile));
                ((ProgressMonitorInputStream) pIn).getProgressMonitor().setMillisToDecideToPopup(100);
            } else {
                pIn = new FileInputStream(regeditFile);
            }
            Reader inStream = null;
            if (encoding == null) {
                inStream = new InputStreamReader(pIn);
            } else {
                System.out.println("Setting encoding to " + encoding);
                inStream = new InputStreamReader(pIn, encoding);
            }
            LineNumberReader regRead = new LineNumberReader(inStream, 8192);
            String line;
            while (regRead.ready()) {
                line = regRead.readLine();
                line = filterLine(line);
                if (line != null) {
                    line = line.trim();
                }
                if ((line != null) && (line.length() > 0)) {
                    if (keyFound) {
                        if (line.charAt(0) == '[') {
                            keyFound = false;
                        } else {
                            int equalPos = line.indexOf('=');
                            if (equalPos > 0) {
                                String value = "";
                                if (equalPos + 1 < line.length()) value = stripOuterQuotes(line.substring(equalPos + 1));
                                answer.setProperty(stripOuterQuotes(line.substring(0, equalPos)), value);
                            }
                        }
                    } else {
                        if (line.compareToIgnoreCase(maskedKey) == 0) {
                            keyFound = true;
                        }
                    }
                }
            }
            regRead.close();
            inStream.close();
        } catch (Exception ex) {
            System.err.println(ex);
            answer = null;
        }
        if (answer != null) {
            if (answer.size() == 0) answer = null;
        }
        return answer;
    }

    /** Returns the value of a subKey if found in the given key inside
     *  the Windows registry or null if is not found. */
    public static String getValue(String key, String subKey) {
        Properties allSubKeys = getKey(key);
        if (allSubKeys != null) {
            return allSubKeys.getProperty(subKey);
        }
        return null;
    }

    /** Deletes a subKey value from the Windows registry under the given key.
     *  According to the Windows registry specification, this is equal to
     *  writing a '-' value. */
    public static String deleteValue(String key, String subKey) {
        writeValue(key, subKey, "-");
        Properties allSubKeys = getKey(key);
        if (allSubKeys != null) {
            return allSubKeys.getProperty(subKey);
        }
        return null;
    }

    /** Writes the given subkey/value pair into the Windows registry under
     * the given key. Values must be given without masking backspaces or
     * quotes, this is done by this method internally.
     * Waits for the external command 'regedit' to return.  */
    public static boolean writeValue(String key, String subkey, String value) {
        Runtime run = Runtime.getRuntime();
        boolean answer = false;
        String regeditPreface = "REGEDIT4" + lineSep + lineSep;
        try {
            File regeditFile = File.createTempFile("drmwrite", ".reg");
            FileWriter regWrite = new FileWriter(regeditFile);
            regWrite.write(regeditPreface);
            regWrite.write("[" + key + "]\r\n");
            if (value.compareTo("-") == 0) {
                regWrite.write("\"" + subkey + "\"=-\r\n");
            } else {
                regWrite.write("\"" + subkey + "\"=\"" + maskQuotes(maskBackspace(value)) + "\"" + lineSep);
            }
            regWrite.close();
            Process regProcess = run.exec("regedit.exe /s " + regeditFile.getAbsolutePath());
            regProcess.waitFor();
            int result = regProcess.exitValue();
            if (result == 0) answer = true;
            regeditFile.delete();
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return answer;
    }

    /** Exchanges backspaces with double backspaces. */
    public static String maskBackspace(String input) {
        int len = input.length();
        int pos = 0;
        int nextPos = 0;
        String answer = "";
        nextPos = input.indexOf("\\", pos);
        while (nextPos >= 0) {
            answer += input.substring(pos, nextPos) + "\\\\";
            pos = nextPos + 1;
            if (pos < len) {
                nextPos = input.indexOf("\\", pos);
            } else {
                break;
            }
        }
        if (pos < len) answer += input.substring(pos);
        return answer;
    }

    /** Exchanges backspaces with double backspaces. */
    public static String maskQuotes(String input) {
        int len = input.length();
        int pos = 0;
        int nextPos = 0;
        String answer = "";
        nextPos = input.indexOf("\"", pos);
        while (nextPos >= 0) {
            answer += input.substring(pos, nextPos) + "\\\"";
            pos = nextPos + 1;
            if (pos < len) {
                nextPos = input.indexOf("\"", pos);
            } else {
                break;
            }
        }
        if (pos < len) answer += input.substring(pos);
        return answer;
    }

    /** Removes leading / trailing quotes if contained. */
    public static String stripOuterQuotes(String input) {
        String answer = input;
        int len = input.length();
        if (len > 2) {
            if ((input.charAt(0) == '"') && (input.charAt(len - 1) == '"')) {
                answer = input.substring(1, len - 1);
            }
        }
        return answer;
    }

    public static void main(String args[]) {
        System.out.println("getKey test: reading HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run");
        System.out.println(RegEdit.getKey("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"));
        System.out.println("\ngetKey test: reading HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion");
        System.out.println(RegEdit.getKey("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion"));
        System.out.println("\ngetValue test: trying to read DreamNode value.");
        System.out.println(RegEdit.getValue("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "DreamNode"));
        System.out.println("\nwriteValue writing subkey \"DreamTest\", then reading it again (should output \"testvalue\"):");
        RegEdit.writeValue("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "DreamTest", "testvalue");
        String getTestVal = RegEdit.getValue("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "DreamTest");
        System.out.println(getTestVal);
        if (getTestVal.compareTo("testvalue") == 0) {
            System.out.println("RegEdit read/write test finished successfully.");
        } else {
            System.out.println("RegEdit read/write test failed.");
        }
        System.out.println("\nNow deleting the key again: (should output 'null')");
        RegEdit.deleteValue("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "DreamTest");
        String getDeletedVal = RegEdit.getValue("HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "DreamTest");
        System.out.println(getDeletedVal);
        if (getDeletedVal == null) {
            System.out.println("RegEdit deletion test finished successfully.");
        } else {
            System.out.println("RegEdit deletion test failed.");
        }
        JFrame testMon = new JFrame();
        RegEdit.progressMonitorParent = testMon;
        System.out.println(lineSep + "testing progress monitoring.");
        RegEdit.progressMessage = "Reading from Windows registry.";
        RegEdit.getValue("HKEY_LOCAL_MACHINE\\SOFTWARE", "DreamTest");
        System.out.println("progress monitoring test finished.");
        RegEdit.progressMonitorParent = null;
        System.exit(0);
    }
}
