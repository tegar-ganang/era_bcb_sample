package net.sourceforge.jhelpdev.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Collection class for several static helper methods that
 * are used by different classes. 
 * @author <a href="mailto:mk@mk-home.de">Markus Kraetzig</a>
 */
public final class HelperMethods {

    /**
	 * Copies a file <code>resourceFileName</code>
	 * to a given location defined in <code>destFileName</code>.
	 
	 * @param destFileName String containing the target filename
	 * @param resourceFileName String containing the resource filename relative
	 *        to the resource directory
	 * @throws IllegalArgumentException if 
	 * <code>destFileName == null || resourceFileName == null</code>
	 */
    public static void copyFileTo(String destFileName, String resourceFileName) {
        if (destFileName == null || resourceFileName == null) throw new IllegalArgumentException("Argument cannot be null.");
        try {
            FileInputStream in = null;
            FileOutputStream out = null;
            File resourceFile = new File(resourceFileName);
            if (!resourceFile.isFile()) {
                System.out.println(resourceFileName + " cannot be opened.");
                return;
            }
            in = new FileInputStream(resourceFile);
            out = new FileOutputStream(new File(destFileName));
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Copies a file that resides in the resource directory
	 * to a given location defined in <code>destFileName</code>.
	 
	 * @param destFileName String containing the target filename
	 * @param resourceFileName String containing the resource filename relative
	 *        to the resource directory
	 * @throws IllegalArgumentException if 
	 * <code>destFileName == null || resourceFileName == null</code>
	 */
    public static void copyResourceFileTo(String destFileName, String resourceFileName) {
        if (destFileName == null || resourceFileName == null) throw new IllegalArgumentException("Argument cannot be null.");
        try {
            FileInputStream in = null;
            FileOutputStream out = null;
            URL url = HelperMethods.class.getResource(resourceFileName);
            if (url == null) {
                System.out.println("URL " + resourceFileName + " cannot be created.");
                return;
            }
            String fileName = url.getFile();
            fileName = fileName.replaceAll("%20", " ");
            File resourceFile = new File(fileName);
            if (!resourceFile.isFile()) {
                System.out.println(fileName + " cannot be opened.");
                return;
            }
            in = new FileInputStream(resourceFile);
            out = new FileOutputStream(new File(destFileName));
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
