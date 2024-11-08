package net.assimilator.tools.webster;

import junit.framework.TestCase;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Webster Tester.
 *
 * @author Kevin Hartig
 * @version $Id: WebsterTest.java 365 2007-11-08 15:32:57Z khartig $
 */
public class WebsterTest extends TestCase {

    WebsterImpl webster;

    public WebsterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("net.assimilator.tools.webster.port", "9000");
        webster = new WebsterImpl();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        webster.terminate();
    }

    public void testGetPort() throws Exception {
        assertEquals(webster.getPort(), 9000);
    }

    public void testGetMinNumServerThreads() throws Exception {
        assertEquals(webster.getMinNumServerThreads(), 10);
    }

    public void testGetMaxNumServerThreads() throws Exception {
        assertEquals(webster.getMaxNumServerThreads(), 50);
    }

    public void testGetWebsterRoots() throws Exception {
        File largestFile = new File("tmp.txt");
        List<String> roots = webster.getRoots();
        assertEquals(roots.size(), 1);
        File dir = new File(System.getProperty("user.dir"));
        File[] children = dir.listFiles();
        if (children == null) {
            System.out.println("Either directory " + dir.getName() + " does not exist or it is not a directory.");
        } else {
            for (File file : children) {
                if (file.length() > largestFile.length()) {
                    largestFile = file;
                }
            }
            File targetFile = new File("tmpTargetFile");
            URL location = new URL("http://localhost:9000/" + largestFile.getName());
            URLConnection connection = location.openConnection();
            writeFileFromInputStream(connection.getInputStream(), targetFile);
            targetFile.delete();
        }
    }

    /**
     * Given an InputStream this method will write the contents to the desired
     * File.
     *
     * @param in   InputStream
     * @param file The File object to write to.
     * @return The size of what was written.
     * @throws java.io.IOException if the File is not found or if the input stream
     *                             can't be read.
     */
    private int writeFileFromInputStream(InputStream in, File file) throws IOException {
        int totalWrote = 0;
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int bytes_read;
            byte[] buf = new byte[2048];
            while ((bytes_read = in.read(buf)) != -1) {
                out.write(buf, 0, bytes_read);
                totalWrote += bytes_read;
            }
        } catch (FileNotFoundException e) {
            file.delete();
            throw e;
        } catch (IOException e) {
            file.delete();
            throw e;
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (totalWrote);
    }
}
