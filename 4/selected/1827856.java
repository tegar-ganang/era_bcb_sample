package net.assimilator.substrates.sca.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * JarResources maps all resources included in a Zip or Jar file. Additionaly,
 * it provides a methods to: - extract one resource as an array of bytes -
 * extract one resource and save it to a file created 06 August 2003
 *
 * @author Kevin Hartig
 * @version $Id: JarResources.java 2544 2006-08-26 23:33:35Z kevin.hartig $
 */
public final class JarResources {

    public boolean debug = true;

    private Hashtable htSizes = new Hashtable();

    private Hashtable htJarContents = new Hashtable();

    private String jarFile;

    /**
     * Creates a JarResources. It extracts all resources from a Jar into an
     * internal hashtable, keyed by resource names.
     *
     * @param jarFile a jar or zip file
     */
    public JarResources(String jarFile) {
        System.out.println("Jar file to open = " + jarFile);
        this.jarFile = jarFile;
        init();
    }

    /**
     * Initialize internal hash tables with Jar file resources.
     */
    private void init() {
        ZipInputStream zis = null;
        try {
            if (debug) {
                System.out.println("Reading Jar file in init...");
            }
            ZipFile zf = new ZipFile(jarFile);
            Enumeration e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                if (debug) {
                    System.out.println(dumpZipEntry(ze));
                }
                htSizes.put(ze.getName(), new Integer((int) ze.getSize()));
            }
            zf.close();
            FileInputStream fis = new FileInputStream(jarFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                if (debug) {
                    System.out.println("ze.getName()=" + ze.getName() + "," + "getSize()=" + ze.getSize());
                }
                int size = (int) ze.getSize();
                if (size == -1) {
                    size = ((Integer) htSizes.get(ze.getName())).intValue();
                }
                byte[] b = new byte[(int) size];
                int rb = 0;
                int chunk = 0;
                while (((int) size - rb) > 0) {
                    chunk = zis.read(b, rb, (int) size - rb);
                    if (chunk == -1) {
                        break;
                    }
                    rb += chunk;
                }
                htJarContents.put(ze.getName(), b);
                if (debug) {
                    System.out.println(ze.getName() + "  rb=" + rb + ",size=" + size + ",csize=" + ze.getCompressedSize());
                }
            }
        } catch (NullPointerException e) {
            System.out.println("done.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Extract an entire archive
     *
     * @param directory The directory to extract files
     * @param archive The archive filename
     * @return Object[] - An array with 2 elements. The first element being the
     * size of what was extracted, the second element being a List of what was
     * extracted
     */
    public Object[] extract(File directory, File archive) throws IOException {
        int extractSize = 0;
        ZipFile zipFile = null;
        List extractList = new ArrayList();
        try {
            zipFile = new ZipFile(archive);
            Enumeration zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
                if (zipEntry.isDirectory()) {
                    File file = new File(directory.getCanonicalPath() + File.separator + zipEntry.getName());
                    file.mkdirs();
                } else {
                    File file = new File(directory.getCanonicalPath() + File.separator + zipEntry.getName());
                    extractList.add(file);
                    String fullPath = file.getCanonicalPath();
                    int index = fullPath.lastIndexOf(File.separatorChar);
                    String installPath = fullPath.substring(0, index);
                    File targetPath = new File(installPath);
                    if (!targetPath.exists()) {
                        targetPath.mkdirs();
                        if (!targetPath.exists()) {
                            throw new IOException("Failed to create : " + installPath);
                        }
                    }
                    if (!targetPath.canWrite()) {
                        throw new IOException("Can not write to : " + installPath);
                    }
                    InputStream in = zipFile.getInputStream(zipEntry);
                    extractSize += writeFileFromInputStream(in, file);
                }
            }
        } finally {
            if (zipFile != null) zipFile.close();
        }
        return (new Object[] { new Integer(extractSize), extractList });
    }

    /**
     * Extracts a jar resource as a byte array.
     *
     * @param name a resource name.
     */
    public byte[] getResource(String name) {
        byte[] buff = null;
        buff = (byte[]) htJarContents.get(name);
        if (buff == null) {
            System.out.println("Could not find " + name);
        } else {
            System.out.println("Found " + name + " (length=" + buff.length + ")");
        }
        return buff;
    }

    /**
     * Extract resource and save to file.
     *
     * @param name resource name in archive
     * @param file save extracted resource in this File
     */
    public void extractResource(String name, File file) throws FileNotFoundException, IOException {
        writeFileFromInputStream(new ByteArrayInputStream(getResource(name)), file);
    }

    /**
     * Dumps a zip entry into a string.
     *
     * @param ze a ZipEntry
     */
    private String dumpZipEntry(ZipEntry ze) {
        StringBuffer sb = new StringBuffer();
        if (ze.isDirectory()) {
            sb.append("d ");
        } else {
            sb.append("f ");
        }
        if (ze.getMethod() == ZipEntry.STORED) {
            sb.append("stored   ");
        } else {
            sb.append("defalted ");
        }
        sb.append(ze.getName());
        sb.append("\t");
        sb.append("" + ze.getSize());
        if (ze.getMethod() == ZipEntry.DEFLATED) {
            sb.append("/" + ze.getCompressedSize());
        }
        return (sb.toString());
    }

    /**
     * Given an InputStream this method will write the contents to the desired
     * File.
     *
     * @param newIn - InputStream
     * @param file - The File object to write to
     * @return int - The size of what was written
     */
    private int writeFileFromInputStream(InputStream newIn, File file) throws FileNotFoundException, IOException {
        int totalWrote = 0;
        InputStream in = newIn;
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int read = 0;
            byte[] buf = new byte[2048];
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                totalWrote += read;
            }
        } catch (FileNotFoundException e) {
            file.delete();
            throw e;
        } catch (IOException e) {
            file.delete();
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        return (totalWrote);
    }

    /**
     * Test driver. Given a jar file and a resource name, it trys to extract the
     * resource and indicates success or failure. <strong>Example </strong>
     * Using JarResources you can extract, create, and display those images
     * on-the-fly.
     *
     * <pre>
     *     ...
     *     JarResources JR=new JarResources("GifBundle.jar");
     *     Image image=Toolkit.createImage(JR.getResource("logo.gif");
     *     Image logo=Toolkit.getDefaultToolkit().createImage(
     *                   JR.getResources("logo.gif")
     *                   );
     *     ...
     * </pre>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: java JarResources <jar file name> <resource name> <save file name>");
            System.exit(1);
        }
        try {
            JarResources jr = new JarResources(args[0]);
            jr.extractResource(args[1], new File(args[2]));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
