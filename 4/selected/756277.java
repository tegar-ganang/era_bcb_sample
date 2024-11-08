package net.sourceforge.olduvai.lrac.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;

/**
 * 
 * Various static utility functions that don't belong in their own files
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public final class Util {

    /**
	 * Debugging routines 
	 */
    static int debuglevel = 10;

    /**
	 * read the supplied reader in its entirety into a string
	 *
	 * @param the InputStream to read characters from
	 * @return a string holding the file contents
	 * @exception IOException reports problems that occurred
	 */
    public String readCharacterStream(InputStream stream) throws IOException {
        if (stream == null) throw new IllegalArgumentException("supplied stream was null");
        try {
            StringWriter sw = new StringWriter();
            int n;
            while ((n = stream.read()) >= 0) sw.write(n);
            return sw.toString();
        } finally {
            if (stream != null) stream.close();
        }
    }

    /**
	 * Retrieve an InputStream for the named resource which could be 
	 * coming from the filesystem, or from within the LiveRAC jar file.
	 * The filesystem is tried first, and then the jar file.  
	 *   
	 * @param cl Any valid classloader object
	 * @param fileName Name of the filesystem resource to load
	 * @return An InputStream for the object or null. 
	 */
    public static final InputStream chooseStream(ClassLoader cl, String systemResource, String userResource, boolean forceDefault) {
        InputStream inputStream = null;
        if (!forceDefault) {
            inputStream = getFileResource(userResource);
        }
        if (inputStream == null) inputStream = getJARresource(cl, systemResource);
        return inputStream;
    }

    public static final InputStream getFileResource(String resourceName) {
        InputStream inputStream = null;
        File f = new File(resourceName);
        try {
            inputStream = new FileInputStream(f);
        } catch (Exception e) {
            inputStream = null;
        }
        return inputStream;
    }

    public static final InputStream getJARresource(ClassLoader cl, String resourceName) {
        InputStream inputStream = cl.getResourceAsStream(resourceName);
        return inputStream;
    }

    /**
	 * Returns null if unable to create or open file
	 * @param filePath
	 * @return
	 */
    public static final File getCreateFile(String filePath) {
        File f = new File(filePath);
        return getCreateFile(f);
    }

    public static final boolean folderExists(String folderPath) {
        File f = new File(folderPath);
        if (f.isDirectory()) {
            return true;
        }
        return false;
    }

    public static final boolean createFolder(String folderPath) {
        File f = new File(folderPath);
        try {
            return f.mkdir();
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Returns null if unable to Create or open file
	 * @param f
	 * @return
	 */
    public static final File getCreateFile(File f) {
        if (!f.canWrite()) {
            try {
                if (!f.getParentFile().exists()) if (!f.getParentFile().mkdirs()) throw new IOException("Unable to create folder for:" + f);
                if (!f.createNewFile() || !f.canWrite()) {
                    throw new IOException("Unable to create file:" + f);
                }
            } catch (IOException io) {
                System.err.println(io.getMessage());
                return null;
            }
        }
        return f;
    }

    public static void dprint(int level, Object obj) {
        dprint(level, "SWIFT", obj);
    }

    public static void dprint(int level, Object t, Object obj) {
        dprint(level, t.getClass().getName(), obj);
    }

    public static void dprint(int level, String label, Object obj) {
        if (debuglevel >= level) {
            System.err.println(label + " " + obj);
        }
    }

    public static void dprint(Object obj) {
        dprint(0, "SWIFT", obj);
    }

    public static void dprint(Object t, Object obj) {
        dprint(0, t.getClass().getName(), obj);
    }

    /** Return a string representing memory usage */
    public static String getmemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.freeMemory() + "/" + rt.totalMemory();
    }

    /**
	 * Unfortunately the standard tokenizer 'ignores' null elements and thus 
	 * the size of the array can vary even when the same number of token seperators is 
	 * present.  Eg.  foo|bar|now returns 3 tokens, but foo||now returns 2 tokens.
	 * 
	 *  This method always returns the same number of tokens given the same number of 
	 *  seperators.  Note that the | seperator must be escaped: \\| 
	 * @param str
	 * @param tok
	 * @return
	 */
    public static String[] myTokenize(String str, String tok) {
        String[] result = str.split(tok);
        return result;
    }

    public static void setdebuglevel(int level) {
        debuglevel = level;
    }

    /** Split a string by white space into an array of strings */
    public static String[] tokenize(String str) {
        return tokenize(str, " \t\n\r");
    }

    /** Split a string by a given token into an array of strings */
    public static String[] tokenize(String str, String tok) {
        StringTokenizer st = new StringTokenizer(str, tok);
        int len = st.countTokens();
        String result[] = new String[len];
        for (int i = 0; i < len; i++) {
            result[i] = st.nextToken();
        }
        return result;
    }

    /** Create a string out of an array of strings and a token separator */
    public static String untokenize(String[] strs, String tok) {
        String res = new String();
        for (int i = 0; i < strs.length; i++) res += tok + strs[i];
        return res;
    }

    /**
	 * Tries to find a pretty way to output an array or collection. 
	 * 
	 * @param array
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static String arrayToString(Object array) {
        if (array == null) {
            return "[NULL]";
        } else {
            Object obj = null;
            if (array instanceof Hashtable) {
                array = ((Hashtable) array).entrySet().toArray();
            } else if (array instanceof HashSet) {
                array = ((HashSet) array).toArray();
            } else if (array instanceof Collection) {
                array = ((Collection) array).toArray();
            }
            int length = Array.getLength(array);
            int lastItem = length - 1;
            StringBuffer sb = new StringBuffer("[");
            for (int i = 0; i < length; i++) {
                obj = Array.get(array, i);
                if (obj != null) {
                    sb.append(obj);
                } else {
                    sb.append("[NULL]");
                }
                if (i < lastItem) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static final URL getResource(final String filename) {
        URL url = ClassLoader.getSystemResource(filename);
        if (url == null) {
            try {
                url = new URL("file", "localhost", filename);
            } catch (Exception urlException) {
            }
        }
        return url;
    }

    /**
	 * Reads in specified text file to a String.   
	 * 
	 * Throws an IOException if there is an error reading the file.  
	 * 
	 * @param file
	 * @return
	 */
    public static String readFile(File file) throws IOException {
        StringBuilder contents = new StringBuilder();
        BufferedReader input = new BufferedReader(new FileReader(file));
        try {
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
        } finally {
            input.close();
        }
        return contents.toString();
    }

    public static final String getInputStreamAsString(InputStream stream) {
        if (stream == null) return null;
        StringBuilder b = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        int chr = 0;
        try {
            while ((chr = br.read()) != -1) {
                b.append((char) chr);
            }
            br.close();
        } catch (IOException e) {
            System.err.println("IO exception");
        }
        return b.toString();
    }

    public static final boolean saveStringToFile(String fileName, String string) {
        File f = getCreateFile(fileName);
        try {
            FileWriter fw = new FileWriter(f);
            fw.write(string);
            fw.close();
        } catch (IOException e) {
            System.err.println("Exception saving file: " + fileName);
            return false;
        }
        return true;
    }

    /**
	 * This is a really stupid method to convert a List with the object type
	 * Double to an array of primitive double. 
	 * 
	 * @param doublelist
	 * @return array of primitive doubles containing the values of the input List
	 */
    public static final double[] toPrimitiveDouble(List<Double> doublelist) {
        double[] result = new double[doublelist.size()];
        Iterator<Double> it = doublelist.iterator();
        int count = 0;
        while (it.hasNext()) {
            result[count] = it.next();
            count++;
        }
        return result;
    }

    /**
	 * In-place reversal of an double[] array.  Return value is the same pointer
	 * @param array
	 * @return Same pointer as input parameter
	 */
    public static double[] reverseArray(double[] array) {
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            double temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    /**
	 * In-place reversal of an int[] array.  Return value is the same pointer 
	 * as was passed in.  
	 * @param array
	 * @return Same pointer as input parameter
	 */
    public static int[] reverseArray(int[] array) {
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            int temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    /**
	 * Returns an array with the objects in reversed order.  
	 * 
	 * Note: reverse is done IN PLACE, to avoid the typing issue of the 
	 * return value, simply continue to use the same reference as was
	 * passed to the parameter instead of the return value.  
	 * 
	 * @param array
	 * @return
	 */
    public static Object[] reverseArray(Object[] array) {
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            Object temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    public static final double subSquare(double d1, double d2) {
        return Math.pow((d1 - d2), 2d);
    }

    public static final double rgbEuclideanDistance(Color c1, Color c2) {
        double distance = Math.sqrt(subSquare(c1.getRed(), c2.getRed()) + subSquare(c1.getGreen(), c2.getGreen()) + subSquare(c1.getBlue(), c2.getBlue()));
        return distance;
    }

    public static final void distanceSort(Color[] colors) {
        Arrays.sort(colors, new Comparator<Color>() {

            public int compare(Color o1, Color o2) {
                return new Double(rgbEuclideanDistance(Color.black, o1)).compareTo(new Double(rgbEuclideanDistance(Color.black, o2)));
            }
        });
    }

    public static void main(String[] args) {
        Color c1 = new Color(166, 86, 40);
        Color c2 = new Color(77, 175, 74);
        Color c3 = new Color(55, 126, 184);
        Color c4 = new Color(228, 26, 28);
        Color c5 = new Color(152, 78, 163);
        Color c6 = new Color(153, 153, 153);
        Color c7 = new Color(255, 127, 0);
        Color c8 = new Color(247, 129, 191);
        Color c9 = new Color(255, 255, 51);
        Color[] testColors = { c1, c2, c3, c4, c5, c6, c7, c8, c9 };
        distanceSort(testColors);
        for (Color color : testColors) System.out.println(color);
    }

    /***
	 * Upload a file to the specified web url using HTTP POST method and application/octet-stream
	 * encoding.  Can also do HTTP BASIC authentication if both username and password are not null.   
	 *  
	 * @param f File to upload
	 * @param uploadURL Target URL of upload server script.
	 * @param username Optional HTTP BASIC authentication username
	 * @param password Optional HTTP BASIC authenitcation password
	 * @return
	 * @throws IOException
	 */
    public static int uploadFile(File f, String uploadURL, String username, String password) throws IOException {
        final PostMethod filePost = new PostMethod(uploadURL);
        final Part[] parts = { new FilePart("lrfile", f) };
        filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
        HttpClient client = new HttpClient();
        if (username != null && password != null) {
            final String idpass = username + ":" + password;
            final String idEncoding = new sun.misc.BASE64Encoder().encode(idpass.getBytes());
            filePost.setRequestHeader("Authorization", "Basic " + idEncoding);
        }
        final int status = client.executeMethod(filePost);
        return status;
    }
}
