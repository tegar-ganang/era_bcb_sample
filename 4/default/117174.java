import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Properties;
import java.util.Enumeration;
import org.apache.tools.ant.*;
import java.util.jar.*;
import java.util.*;
import java.io.*;
import java.lang.Runtime;
import java.net.ServerSocket;
import java.net.*;

/**
 * Tuomas Nissi & Marko Niinimaki based on Vesa Sivunen's installer.
 * A simple installer for gb-agent.jar. This is needed in a 
 * jar file, together with Manifest.mf.
 *
 * ./build.sh client should create release/gb-agent.jar
 * for you. It packs there all that's needed + install.class +
 * Manifest.mf 
 *
 * In unix the usage is: "java -jar gb-agent.jar".
 * In win double click the file.
 *
 * @author Vesa Sivunen
 * @author Marko Niinimaki
 * @author Tuomas Nissi
 */
public class install {

    String FILENAME = "gb-agent.jar";

    URL jarlocation = this.getClass().getProtectionDomain().getCodeSource().getLocation();

    /**
     * Construct the application
     */
    public install(String argv[]) {
        System.out.println("gb-agent starting");
        FILENAME = jarlocation.getFile();
        int slashpos = FILENAME.lastIndexOf("/");
        if (slashpos > -1) {
            FILENAME = FILENAME.substring(slashpos + 1);
        }
        System.out.println("Filename is " + FILENAME);
        if (argv.length > 0) {
            org.apache.tools.ant.Main.main(new String[] { "-buildfile", "build.xml", "start" });
            return;
        }
        unpack_and_run();
        return;
    }

    public void unpack_and_run() {
        boolean ok;
        System.out.println("Unpacking junk from " + FILENAME);
        ok = unpack_jar(FILENAME);
        if (!ok) {
            unjarErrorMsg(FILENAME);
        }
        System.out.println("Starting the client.");
        try {
            String jbin = System.getProperty("java.home") + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "java";
            System.out.println("Calling " + jbin + " -classpath .:buildlib/ant.jar -jar " + FILENAME + " start");
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(jbin + " -classpath .:buildlib/ant.jar -jar " + FILENAME + " start");
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isr = new InputStreamReader(stderr);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int exitVal = 0;
            if (System.getProperty("os.name").indexOf("Windows") > -1) {
                System.out.println("This is Windows..");
            } else {
                System.out.println("System starting please wait..");
                while ((line = br.readLine()) != null) System.out.println(line);
                exitVal = proc.waitFor();
                System.out.println("Process exitValue: " + exitVal);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 
   * Prints out an error message and exits
   * 
   * @param filename   the file whose unpacking failed
   */
    protected void unjarErrorMsg(String filename) {
        System.out.println("ERROR: Unpacking " + filename + " failed!");
        System.out.println("Check out http://gridblocks.sourceforge.net and " + "contact developers if necessary.");
        System.exit(0);
    }

    /** 
     * This is needed to bring tomcat out of this package.
     *
     * @param jarname   the name of the jar file (e.g. gb-portal.jar)
     * @param files     file names to be unpacked. If the list is empty 
     *                  everything will be unpacked
     * @return          true if everything's ok, <br>
     *                  false if something went wrong
     */
    protected boolean unpack_jar(String jarname, String[] files) {
        System.out.println("Unpacking " + jarname);
        try {
            File file = new File(jarname);
            if (!(file.exists())) {
                System.out.println("ERROR: File " + jarname + " does not exist!");
                return false;
            }
            JarFile jarfile = new JarFile(file);
            int entries = 0;
            JarEntry[] unpackthese = new JarEntry[0];
            if (files.length == 0) {
                for (Enumeration enu = jarfile.entries(); enu.hasMoreElements(); ) {
                    entries++;
                    Object o = enu.nextElement();
                }
                unpackthese = new JarEntry[entries];
                int j = 0;
                for (Enumeration enu = jarfile.entries(); enu.hasMoreElements(); ) {
                    unpackthese[j] = (JarEntry) enu.nextElement();
                    j++;
                }
            } else {
                unpackthese = new JarEntry[files.length];
                for (int i = 0; i < files.length; i++) {
                    unpackthese[i] = new JarEntry(files[i]);
                }
            }
            System.out.println(unpackthese.length + " files to be unjarred.");
            Comparator _comparebylenght = new Comparebylenght();
            Arrays.sort(unpackthese, _comparebylenght);
            for (int i = 0; i < unpackthese.length; i++) {
                File f = new File(unpackthese[i].getName());
                if (unpackthese[i].isDirectory()) {
                    boolean cre = f.mkdirs();
                    System.out.println("Creation of " + unpackthese[i].getName() + " " + cre);
                } else {
                    String dirname = unpackthese[i].getName();
                    if (dirname.lastIndexOf("/") > -1) {
                        dirname = dirname.substring(0, dirname.lastIndexOf("/"));
                        File f2 = new File(dirname);
                        f2.mkdirs();
                    }
                    InputStream stream = jarfile.getInputStream(unpackthese[i]);
                    FileOutputStream fos = new FileOutputStream(f);
                    byte c[] = new byte[4096];
                    int read = 0;
                    while ((read = stream.read(c)) != -1) fos.write(c, 0, read);
                    fos.close();
                    stream.close();
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR:");
            System.out.println(e);
            return false;
        }
        return true;
    }

    /** 
     * Unpacks everything from the spcified jar file. 
     *
   * @param filename   a file to be unpacked
   * @return           true if everyhing's ok,<br>false if not. 
   */
    protected boolean unpack_jar(String filename) {
        return unpack_jar(filename, new String[0]);
    }

    /**
     * A class for sorting the items. Compares the items 
     * by their length. 
     *
     * @see java.util.Comparator
     */
    protected class Comparebylenght implements Comparator {

        /**
	 * @see java.util.Comparator
	 */
        public int compare(Object o1, Object o2) {
            JarEntry j1 = (JarEntry) o1;
            JarEntry j2 = (JarEntry) o2;
            String s1 = j1.getName();
            String s2 = j2.getName();
            int s1len = s1.length();
            int s2len = s2.length();
            if (s2len > s1len) return 0; else return 1;
        }
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        new install(args);
    }
}
