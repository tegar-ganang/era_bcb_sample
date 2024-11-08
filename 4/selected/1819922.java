package de.offis.semanticmm4u.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import component_interfaces.semanticmm4u.realization.IQueryObject;
import de.offis.sma.common.MDAContextObject;

/**
 * This class contains "domain independent" methods, not specialized for the
 * framework.
 * 
 * @testcase test.de.offis.semanticmm4u.global.TestUtilities
 */
public class Utilities {

    private static Hashtable extensionToMimetype = null;

    public static int string2Integer(String myString) throws RuntimeException {
        if (Utilities.stringIsNullOrEmpty(myString)) {
            return Constants.UNDEFINED_INTEGER;
        }
        myString = myString.trim();
        boolean negativeNumber = false;
        int value = 0;
        int counter = 0;
        if ((myString.substring(counter, 1)).equals("-")) {
            negativeNumber = true;
            counter = 1;
        }
        for (; counter < myString.length(); counter++) {
            if (Character.isDigit(myString.charAt(counter))) value = (value * 10) + Character.digit(myString.charAt(counter), 10); else throw new RuntimeException("Error in method string2Integer() of class Utilities: bad number '" + myString + "'");
        }
        if (negativeNumber) {
            value = -value;
        }
        return value;
    }

    /**
	 * Converts a strint into a long value. If the string is null or empty it
	 * returns UNDEFINED_LONG. Throws a RuntimException on error.
	 * 
	 * @param longString
	 * @return the long value or UNDEFINED_LONG if string was null or empty.
	 */
    public static long string2Long(String longString) {
        if (Utilities.stringIsNullOrEmpty(longString)) return Constants.UNDEFINED_LONG;
        try {
            long retValue = Long.parseLong(longString);
            return retValue;
        } catch (NumberFormatException excp) {
            throw new RuntimeException("Error in method string2Long() of class Utilities: bad number '" + longString + "'");
        }
    }

    public static boolean isPositiveNumber(int intValue) {
        if (intValue == Constants.UNDEFINED_INTEGER) {
            return false;
        }
        if (intValue <= 0) {
            return false;
        }
        return true;
    }

    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    public static float string2Float(String myString) {
        if (Utilities.stringIsNullOrEmpty(myString)) {
            return Constants.UNDEFINED_FLOAT;
        }
        float returnValue = (new Float(myString)).floatValue();
        return returnValue;
    }

    /**
	 * Converts a string with a double value to a double datatype.
	 * 
	 * @param doubleStr
	 * @return Constants.UNDEFINED_DOUBLE if the string is null or empty, else
	 *         the double value.
	 */
    public static double string2Double(String doubleStr) {
        if (Utilities.stringIsNullOrEmpty(doubleStr)) return Constants.UNDEFINED_DOUBLE;
        return Double.parseDouble(doubleStr);
    }

    public static boolean string2Boolean(String tempString) {
        boolean booleanValue = false;
        if (tempString == null || tempString.equals("-1") || tempString.equals("0") || tempString.equalsIgnoreCase("false")) {
            booleanValue = false;
        } else if (tempString.equals("1") || tempString.equalsIgnoreCase("true")) {
            booleanValue = true;
        } else {
            throw new RuntimeException("Error in method getBooleanValue() of class Utilities : '" + tempString + "' is not a boolean.");
        }
        return booleanValue;
    }

    public static boolean isSubstring(String myString, String mySubstring) {
        if ((myString == null) || (mySubstring == null)) {
            return false;
        }
        String tempString = myString.toLowerCase();
        int index = tempString.indexOf(mySubstring.toLowerCase());
        if (index != -1) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Note: The Java build-in method clone() ist not used, because clone is a
	 * protected method and can only be called within the class itself and clone
	 * doesn�t work for my classes/objects!
	 */
    public static Object cloneArbitraryObject(Object inputObject) {
        Object outputObject = null;
        boolean deleteSuccess = true;
        String filename = Constants.getDefaultLogfilePathAndFilename() + "tempObject.serialized";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(inputObject);
            objectOutputStream.close();
            FileInputStream fileInputStream = new FileInputStream(filename);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            outputObject = objectInputStream.readObject();
            objectInputStream.close();
            File file = new File(filename);
            deleteSuccess = file.delete();
        } catch (IOException exception) {
            System.err.println("Error: IOException while duplicating object.");
        } catch (ClassNotFoundException exception) {
            System.err.println("Error: Class not found exception.");
        }
        if (inputObject == outputObject) {
            throw new RuntimeException("Error in method duplicate() of class Utilities: input and output object are the same!");
        }
        if (!deleteSuccess) {
            throw new RuntimeException("Error in method duplicate() of class Utilities: couldn't delete temporary file.");
        }
        return outputObject;
    }

    public static String getClassName(Object object) {
        return (object.getClass()).getName();
    }

    public static String getClassAndHexHashCode(Object object) {
        return object.getClass() + "@" + getHexHashCode(object);
    }

    public static String getHexHashCode(Object object) {
        String objectString = null;
        if (object != null) {
            objectString = Integer.toHexString(object.hashCode());
        }
        return objectString;
    }

    /**
	 * Return the lower-case extention of the file or URL specified in parameter
	 * 'myString'
	 * 
	 * @param myString
	 *            string describing an URL or file
	 * @return the extension
	 */
    public static String getURISuffix(String myString) {
        if (myString == null) {
            return null;
        }
        myString = myString.trim();
        if (myString.endsWith(".")) {
            return "";
        }
        StringTokenizer tempTokenizer = new StringTokenizer(myString, ".");
        int numberOfTokens = tempTokenizer.countTokens();
        for (int counter = 1; counter < numberOfTokens; counter = counter + 1) {
            String tempToken = tempTokenizer.nextToken();
        }
        String suffix = tempTokenizer.nextToken();
        suffix = suffix.trim();
        suffix = suffix.toLowerCase();
        return suffix;
    }

    public static String addBaseURI(String baseURI, String myURI) {
        String tempURI = null;
        if ((myURI.toLowerCase()).startsWith("http://") || (myURI.toLowerCase()).startsWith("https://") || (myURI.toLowerCase()).startsWith("ftp://") || (myURI.toLowerCase()).startsWith("file://")) {
            tempURI = myURI;
        } else {
            tempURI = baseURI + Constants.URI_SEPARATOR + myURI;
        }
        return tempURI;
    }

    public static int maximumOf(int[] intArray, int begin, int end) {
        if (begin > end) {
            throw new RuntimeException("Error in method maximumOf() of class Utilities: begin = " + begin + " > end = " + end);
        }
        if (begin < 0) {
            begin = 0;
        }
        if (end > intArray.length) {
            end = intArray.length;
        }
        int maximum = intArray[begin];
        for (int counter = begin + 1; counter < end; counter = counter + 1) {
            int value = intArray[counter];
            if (value > maximum) {
                maximum = value;
            }
        }
        return maximum;
    }

    public static int minimumOf(int[] intArray, int begin, int end) {
        if (begin > end) {
            throw new RuntimeException("Error in method minimumOf() of class Utilities: begin = " + begin + " > end = " + end);
        }
        if (begin < 0) {
            begin = 0;
        }
        if (end > intArray.length) {
            end = intArray.length;
        }
        int minimum = intArray[begin];
        for (int counter = begin + 1; counter < end; counter = counter + 1) {
            int value = intArray[counter];
            if (value < minimum) {
                minimum = value;
            }
        }
        return minimum;
    }

    public static boolean stringIsNullOrEmpty(String myString) {
        return ((myString == null) || ((myString.trim()).equals("")));
    }

    public static boolean stringIsNotNullAndNotEmpty(String myString) {
        return (!stringIsNullOrEmpty(myString));
    }

    public static int determineMediumType(String uri) {
        String suffix = Utilities.getURISuffix(uri);
        if ((new String("txt")).indexOf(suffix) != -1) {
            return Constants.MEDIATYPE_TEXT;
        }
        if ((new String("jpg.gif.png")).indexOf(suffix) != -1) {
            return Constants.MEDIATYPE_IMAGE;
        }
        if ((new String("wav.mp3")).indexOf(suffix) != -1) {
            return Constants.MEDIATYPE_AUDIO;
        }
        if ((new String("mpg.rm.avi")).indexOf(suffix) != -1) {
            return Constants.MEDIATYPE_VIDEO;
        }
        if ((new String("fli")).indexOf(suffix) != -1) {
            return Constants.MEDIATYPE_ANIMATION;
        }
        return Constants.MEDIATYPE_UNDEFINED;
    }

    public static StringVector tokenizeStringToStringVector(String myString, String mySeperator) {
        if ((myString == null) || (mySeperator == null)) {
            return null;
        }
        StringVector tempVector = new StringVector();
        StringTokenizer tempStringTokenizer = new StringTokenizer(myString, mySeperator);
        while (tempStringTokenizer.hasMoreTokens()) {
            String token = tempStringTokenizer.nextToken();
            token = token.trim();
            tempVector.add(token);
        }
        return tempVector;
    }

    public static String removeDoubleBackslashes(String myString) {
        StringBuffer buffer = new StringBuffer(myString);
        StringBuffer output = new StringBuffer();
        output.append(buffer.charAt(0));
        for (int counter = 1; counter < buffer.length(); counter = counter + 1) {
            if ((buffer.charAt(counter - 1) == '\\') && (buffer.charAt(counter) == '\\')) {
            } else {
                output.append(buffer.charAt(counter));
            }
        }
        return output.toString();
    }

    public static String removeDoubleSlashes(String myString) {
        StringBuffer buffer = new StringBuffer(myString);
        StringBuffer output = new StringBuffer();
        output.append(buffer.charAt(0));
        for (int counter = 1; counter < buffer.length(); counter = counter + 1) {
            if ((buffer.charAt(counter - 1) == '/') && (buffer.charAt(counter) == '/')) {
            } else {
                output.append(buffer.charAt(counter));
            }
        }
        return output.toString();
    }

    public static String replaceInString(String originalString, String searchFor, String replaceBy) {
        if (originalString == null) {
            return null;
        }
        if (searchFor == null) {
            return originalString;
        }
        String tempOriginalString = originalString;
        int lastIndex = 0;
        while (true) {
            int tempIndex = tempOriginalString.indexOf(searchFor, lastIndex);
            if (tempIndex == -1) {
                break;
            }
            tempOriginalString = tempOriginalString.substring(0, tempIndex) + replaceBy + tempOriginalString.substring(tempIndex + searchFor.length(), tempOriginalString.length());
            lastIndex = tempIndex + replaceBy.length();
        }
        return tempOriginalString;
    }

    /**
	 * Escapes entities in Attributes for xml. & -> &amp; " -> &quot; < -> &lt; > ->
	 * &gt; ....
	 * 
	 * @param mySourceString
	 * @return source string with escaped entities.
	 */
    public static String escapeForXML(String mySourceString) {
        String tempContent = mySourceString;
        tempContent = Utilities.replaceInString(tempContent, "&", "&amp;");
        tempContent = Utilities.replaceInString(tempContent, "\"", "&quot;");
        tempContent = Utilities.replaceInString(tempContent, "<", "&lt;");
        tempContent = Utilities.replaceInString(tempContent, ">", "&gt;");
        return tempContent;
    }

    /**
	 * Escapes voval mutations in a String. � -> ae � -> ue ....
	 * 
	 * @param mySourceString
	 * @return source string with escaped entities.
	 */
    public static String escapeVovalMutations(String mySourceString) {
        String tempContent = mySourceString;
        tempContent = Utilities.replaceInString(tempContent, "�", "ae");
        tempContent = Utilities.replaceInString(tempContent, "�", "Ae");
        tempContent = Utilities.replaceInString(tempContent, "�", "oe");
        tempContent = Utilities.replaceInString(tempContent, "�", "Oe");
        tempContent = Utilities.replaceInString(tempContent, "�", "ue");
        tempContent = Utilities.replaceInString(tempContent, "�", "Ue");
        tempContent = Utilities.replaceInString(tempContent, "�", "ss");
        return tempContent;
    }

    /**
	 * Removes all substrings from a string, that are identified as tags. A tag
	 * is a substring starting with "<" and ending with ">"
	 * 
	 * @param s -
	 *            String that might contain tags
	 * @return - String without tags
	 */
    public static String removeXMLTags(String s) {
        int leftTag = s.indexOf("<");
        int rightTag = s.substring(leftTag + 1).indexOf(">");
        if (rightTag >= 0 && leftTag >= 0) {
            String temp1 = s.substring(0, leftTag);
            String temp2 = s.substring(leftTag + rightTag + 2);
            return removeXMLTags(temp1 + temp2);
        } else return s;
    }

    /**
	 * Replaces all multiple lefts slashs (//) of a string by single left slashs
	 * (/).
	 * 
	 * @param input -
	 *            String that might contain double left slashs
	 * @return String with replaced double left slashs
	 */
    public static String replaceMultipleLeftSlashs(String input) {
        if (input == null) {
            return null;
        }
        int position = input.indexOf("//");
        while (position < input.length() && position != -1) {
            if (position != -1) {
                if (position < 4 || !input.substring(position - 4, position).equals("ftp:")) if (position < 5 || (!input.substring(position - 5, position).equals("http:") && !input.substring(position - 5, position).equals("file:"))) {
                    int newPosition = position + 2;
                    while (input.substring(newPosition - 1, newPosition).equals("/") && newPosition < (input.length())) newPosition++;
                    if (newPosition == input.length()) input = input.substring(0, position + 1); else input = input.substring(0, position + 1) + input.substring(newPosition - 1);
                }
            }
            position = input.indexOf("//", position + 1);
        }
        return input;
    }

    /**
	 * Gets the filename of the given url. <br />
	 * Example: "http://server.com/path1/path2/filename.jpg" -> "filename.jpg"
	 * 
	 * @param an
	 *            url
	 * @return the filename of the url or the url if no filename was found.
	 */
    public static String getFilenameFromURL(String url) {
        int index = url.lastIndexOf('/');
        if (index == -1) return url;
        return url.substring(index + 1);
    }

    /**
	 * Get the mimetype to the given extension.
	 * 
	 * @param the
	 *            extension, e.g. "jgp"
	 * @return if the mimetype is found the mimetype to the extension, e.g.
	 *         "image/jpeg", else <code>null</code>. See also:
	 *         http://selfhtml.teamone.de/diverses/mimetypen.htm
	 */
    public static synchronized String getMimetype(String myExtension) {
        if (myExtension == null) {
            return null;
        }
        if (Utilities.extensionToMimetype == null) {
            String filename = Constants.getValue(Constants.CONFIG_MIME_TYPES);
            if (filename == null) ;
            PropertyList mimetypesPropertyList = new PropertyList();
            try {
                mimetypesPropertyList.load(filename);
            } catch (IOException excp) {
                excp.printStackTrace();
            }
            Utilities.extensionToMimetype = new Hashtable();
            Enumeration keys = mimetypesPropertyList.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = mimetypesPropertyList.getStringValue(key);
                String extensions[] = value.split("\\|");
                for (int i = 0; i < extensions.length; i++) {
                    Utilities.extensionToMimetype.put(extensions[i], key);
                }
            }
        }
        return (String) Utilities.extensionToMimetype.get(myExtension);
    }

    /**
	 * called to copy the content from the input stream to the outputstream.
	 * 
	 * @param inputStream
	 *            the input stream
	 * @param outputStream
	 *            the output stream
	 * @throws IOException
	 *             thrown, if any exception occurs while copying the content
	 */
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[1048576];
        int length = -1;
        while ((length = inputStream.read(buf)) != -1) outputStream.write(buf, 0, length);
        inputStream.close();
        outputStream.close();
    }

    /**
	 * 
	 * wandelt URI- Dateibezeichnungen, wie file:///D: ... in absolute
	 * Dateinamen um: d:\server\...
	 * 

	 * @param uriString
	 * @return String
	 */
    public static String convertURIToPathname(String uriString) {
        String retString;
        if (uriString.startsWith("file:///")) {
            String tempString = uriString.substring(8);
            retString = tempString.replace('/', '\\');
        } else if (uriString.startsWith("file:/")) {
            String tempString = uriString.substring(6);
            retString = tempString.replace('/', '\\');
        } else {
            retString = uriString;
            Debug.println("MDA_ConvertURIToPathName : keine Konvertierung n�tig, oder unbekanntes URI- Stringformat");
        }
        return retString;
    }

    public static MDAContextObject toMDAContextObject(IQueryObject myContextObject) {
        MDAContextObject tempMDAContextObject = new MDAContextObject();
        for (Enumeration e = myContextObject.keys(); e.hasMoreElements(); ) {
            String tempKey = (String) e.nextElement();
            Object tempValue = myContextObject.getObjectValue(tempKey);
            tempMDAContextObject.put(tempKey, tempValue);
        }
        return tempMDAContextObject;
    }

    public static IQueryObject toQueryObject(MDAContextObject myMDAContextObject) {
        IQueryObject tempContextObject = new QueryObject();
        for (Enumeration e = myMDAContextObject.keys(); e.hasMoreElements(); ) {
            String tempKey = (String) e.nextElement();
            Object tempValue = myMDAContextObject.getObjectValue(tempKey);
            tempContextObject.put(tempKey, tempValue);
        }
        return tempContextObject;
    }
}
