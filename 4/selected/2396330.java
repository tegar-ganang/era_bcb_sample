package frost;

import java.util.*;
import java.io.*;

public class mixed {

    /**
     * Compares a String with all elements of an array.
     * Returns true if element exists in array, else false.
     * @param element String to be compared
     * @param array Array of Strings to be compared
     * @return true if element exists in array, else false
     */
    public static boolean isElementOf(String element, String[] array) {
        for (int i = 0; i < array.length; i++) if (element.equals(array[i])) return true;
        return false;
    }

    /**
     * Copys a file from the jar file to disk
     * @param resource This is the file's name in the jar
     * @param file This is the destination file
     */
    public static void copyFromResource(String resource, File file) throws IOException {
        if (!file.isFile()) {
            InputStream input = frame1.class.getResourceAsStream(resource);
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
     * Replaces characters that are not 0-9, a-z or in 'allowedCharacters'
     * with '_' and returns a lowerCase String
     * @param text original String
     * @return modified String
     */
    public static String makeFilename(String text) {
        StringBuffer newText = new StringBuffer();
        text = text.toLowerCase();
        if (frame1.frostSettings.getBoolValue("allowEvilBert")) {
            char[] invalidChars = { '/', '\\', '?', '*', '<', '>', '\"', ':', '|' };
            for (int i = 0; i < invalidChars.length; i++) text = text.replace(invalidChars[i], '_');
            newText.append(text);
        } else {
            String allowedCharacters = "()-!.";
            for (int i = 0; i < text.length(); i++) {
                int value = Character.getNumericValue(text.charAt(i));
                char character = text.charAt(i);
                if ((value >= 0 && value < 36) || allowedCharacters.indexOf(character) != -1) newText.append(character); else newText.append("_");
            }
        }
        return newText.toString();
    }
}
