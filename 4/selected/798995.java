package frost;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.logging.Logger;

public final class Mixed {

    private static Logger logger = Logger.getLogger(Mixed.class.getName());

    /**
     * Copys a file from the jar file to disk
     * @param resource This is the file's name in the jar
     * @param file This is the destination file
     */
    public static void copyFromResource(String resource, File file) throws IOException {
        if (!file.isFile()) {
            InputStream input = MainFrame.class.getResourceAsStream(resource);
            FileOutputStream output = new FileOutputStream(file);
            byte[] data = new byte[128];
            int bytesRead;
            while ((bytesRead = input.read(data)) != -1) output.write(data, 0, bytesRead);
            input.close();
            output.close();
        }
    }

    /**
     * Waits for a specific number of ms
     * @param time Time to wait in ms
     */
    public static void wait(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Makes sure that the string does not contain ]]> - the only 
     * sequence that breaks CDATA, converts them to ___
     * @param text the text to be checked
     * @return the string with ]]> converted to ___
     */
    public static String makeSafeXML(String text) {
        if (text == null) return null;
        int index;
        while ((index = text.indexOf("]]>")) != -1) text = text.substring(0, index) + "___" + text.substring(index + 3, text.length());
        return text;
    }

    /**
     * Replaces characters that are not 0-9, a-z or in 'allowedCharacters'
     * with '_' and returns a lowerCase String
     *
     * NEW: does not allow the '#' char, because that will be used for internal folders
     *      in keypool, e.g. '#unsent#'
     *
     * @param text original String
     * @return modified String
     */
    public static String makeFilename(String text) {
        if (text == null) {
            logger.severe("ERROR: mixed.makeFilename() was called with NULL!");
            return null;
        }
        StringBuffer newText = new StringBuffer();
        char[] invalidChars = { '/', '\\', '?', '*', '<', '>', '\"', ':', '|', '#' };
        if (text.startsWith(".")) newText.append("_");
        for (int i = 0; i < invalidChars.length; i++) text = text.replace(invalidChars[i], '_');
        newText.append(text);
        return newText.toString();
    }

    /**
     * Filters all non-english characters as well as those filtered by makeFilename
     * @param text the text to be filtered
     * @return the filtered text
     */
    public static String makeASCIIFilename(String text) {
        StringBuffer newText = new StringBuffer();
        String allowedCharacters = "()-!.";
        for (int i = 0; i < text.length(); i++) {
            int value = Character.getNumericValue(text.charAt(i));
            char character = text.charAt(i);
            if ((value >= 0 && value < 36) || allowedCharacters.indexOf(character) != -1) newText.append(character); else newText.append("_");
        }
        return makeFilename(newText.toString());
    }

    /**
     * checks if the string contains non-english characters
     * @param text the string
     * @return whether it contains foreign chars
     */
    public static boolean containsForeign(String text) {
        char[] chars = text.toCharArray();
        Character c = new Character(chars[0]);
        return false;
    }

    /**
     * If a string is on the system clipboard, this method returns it;
     * otherwise it returns null.
     *
     * @return String  The String from system clipboard or null 
     */
    public static String getSystemClipboard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                return text;
            }
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * This method writes a string to the system clipboard.
     *
     * @param str  The String to be written to the system clipboard 
     */
    public static void setSystemClipboard(String str) {
        StringSelection ss = new StringSelection(str);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
    }

    public static boolean binaryCompare(byte[] src, int offs, String searchTxt) {
        int searchLen = searchTxt.length();
        for (int x = 0; x < searchLen; x++) {
            byte a = (byte) searchTxt.charAt(x);
            byte b = src[offs + x];
            if (a != b) {
                return false;
            }
        }
        return true;
    }
}
