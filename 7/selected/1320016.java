package org.systemsbiology.chem.app;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *  
 * @author s0342694 and s0347096 Mik and Thomas
 * /* ok... I know it looks like I changed everything, but that's all a lie
 *  * all I did was change some stuff... a lie! a LIE!! aaaaah...
 *  * The only thing I needed was to get back an array with it's size
 *  * equal to the number of real elements in the file, and not the
 *  * generic 10. Now I can .getSize on it, and know how big to make the
 *  * menu on the gui.
 *  * Again, no disrespect my homies. Biggee up, and I owe you a tequila.
 *  * Alex.
 *  
 *
 *This class is intended to implement the file history back end and provides read and write methods to the 
 *xml file which stores the last 10 used files.
 */
public class HistoryUtilImpl implements HistoryUtil {

    private static int maxHistorySize = 10;

    public static int historySize = maxHistorySize;

    private static File[] filename = new File[maxHistorySize];

    private static String userDir = "user.dir";

    private static String historyFile = "/lib/FileHistory.xml";

    /**This method is used to save a file to the recent history file.
	 * static method.
	 * If the file is allready in the history, the latter will be moved
	 * to the top of the array.
	 * The array is then stored to an xml file.
	 * @param File wanted to be stored.
	 * @return void
	 */
    public static void updateFileHistory(File latestFilename) {
        read();
        boolean alreadyExists = false;
        for (int h = historySize - 1; h >= 0; h--) {
            if (latestFilename.getPath().equals(filename[h].getPath())) {
                alreadyExists = true;
                File file = filename[h];
                for (int y = h; y < historySize - 1; y++) filename[y] = filename[y + 1];
                filename[historySize - 1] = file;
                write();
            }
        }
        if (!alreadyExists) {
            if (historySize < maxHistorySize) {
                filename[historySize] = latestFilename;
                historySize++;
            } else {
                for (int i = 0; i < historySize - 1; i++) filename[i] = filename[i + 1];
                filename[historySize - 1] = latestFilename;
            }
            write();
        }
    }

    private static void write() {
        try {
            String curDir = System.getProperty(userDir);
            OutputStream fileOut = new FileOutputStream(curDir + historyFile);
            OutputStream bufferedOut = new BufferedOutputStream(fileOut);
            OutputStreamWriter out = new OutputStreamWriter(bufferedOut);
            out.write("<?xml version=\"1.0\"?>");
            out.write("\n");
            out.write("<filehistory>");
            for (int x = 0; x < maxHistorySize; x++) {
                out.write("\n");
                out.write("   <history>");
                out.write("\n");
                if (!(filename[x] == null)) out.write("      <filename>" + filename[x].getPath() + "</filename>"); else {
                    out.write("      <filename>" + "null" + "</filename>");
                    x = maxHistorySize;
                }
                out.write("\n");
                out.write("   </history>");
            }
            out.write("\n");
            out.write("</filehistory>");
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println("IO ERROR");
            System.out.println(e.getMessage());
        }
    }

    private static void read() {
        try {
            String curDir = System.getProperty(userDir);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File tempFile = new File(curDir + historyFile);
            if (tempFile.createNewFile()) {
                historySize = 0;
                write();
                return;
            }
            Document histDoc = builder.parse(tempFile);
            NodeList filenames = histDoc.getElementsByTagName("filename");
            for (int x = 0; x < historySize; x++) {
                String in = filenames.item(x).getFirstChild().getNodeValue();
                filename[x] = new File(in);
                if (in.equals("null")) historySize = x;
            }
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfigurationException");
            System.out.println(e.getMessage());
        } catch (IOException f) {
            System.out.println("Could not write the file!!! ");
            historySize = 0;
        } catch (SAXException g) {
            System.out.println("SAXException");
            System.out.println(g.getMessage());
        }
    }

    /** Used to get the array of all files recently opened in the project
	 * This method returns an array of size equal to the number of items in it.
	 * @param none
	 * @return An array of Files.
	 */
    public static File[] getFileNameHistory() {
        read();
        File[] returnFiles = new File[historySize];
        for (int i = 0; i < historySize; i++) {
            returnFiles[i] = filename[i];
        }
        return returnFiles;
    }

    /** Used to get a file from the array at position
	 * pos.
	 * 
	 * @returns a File element.
	 * @param the position of the element, starting at 0. 
	 */
    public static File getFile(int pos) {
        return filename[pos];
    }
}
