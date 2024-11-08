package boogiepants.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import boogiepants.display.Boogiepants;

/**
     * 
     *            OK, this is a truly ugly hack, but of all the ways to solve
     *            this problem, it seemed like the one that worked best under
     *            all conditions. Well, almost all: the code to get the right
     *            path out of the classpath could be beefed up.
     * 
     *            The problem we're solving here is this: WiiRemoteJ.jar is not
     *            open source. boogiepants is open source, and distributed from
     *            open-source support services. boogiepants depends on
     *            WiiRemoteJ.jar. John does not have the patience or interest to
     *            do an open source wii remote driver at this time. So we need
     *            to grab the driver from its original source, and put it in the
     *            right place. This code does that.
     * 
     *            Why not use ant? Well, we already use ant for this during the
     *            build process. This is install-time logic. Note that we obtain
     *            it for compilation, and then exclude it for distribution.
     * 
     *            Why not use Apple's install process? It can accommodate
     *            processes like this, but it doesn't make it easy. Again, John
     *            ran out of patience. This is bureaucracy code, not fun code.
     *            Besides, Apple's install process won't help us on any other
     *            platform.
     * 
     *            I will probably revisit this when my repository of patience is
     *            replenished, and after some of the other glaring holes in this
     *            prototype are filled. Unless I can, you know, seduce someone 
     *            else.
     */
public class WiiJarLoader {

    public static void main(String argv[]) {
        String cp = System.getProperty("java.class.path");
        String sep = System.getProperty("file.separator");
        String dirpath = cp.substring(0, cp.lastIndexOf(sep));
        String jarpath = dirpath + "/WiiRemoteJ.jar";
        if (!new File(jarpath).exists()) {
            try {
                System.out.println("getting WiiRemoteJ file from remote source...");
                URL wiiRemLoc = new URL("http://www.world-of-cha0s.hostrocket.com/WiiRemoteJ/WiiRemoteJ%20v1.5.zip.gz");
                GZIPInputStream input = new GZIPInputStream(wiiRemLoc.openStream());
                String temploc = copyInputStreamToTmpFile(input, ".zip");
                ZipInputStream zipIn = new ZipInputStream(new FileInputStream(temploc));
                ZipEntry i = null;
                while ((i = zipIn.getNextEntry()) != null) {
                    if (i.getName().endsWith("WiiRemoteJ.jar")) {
                        File outjar = new File(jarpath);
                        BufferedOutputStream fs = new BufferedOutputStream(new FileOutputStream(outjar));
                        int bufferSize = 8192;
                        byte[] buffer = new byte[bufferSize];
                        int readSize = 0;
                        while ((readSize = zipIn.read(buffer, 0, bufferSize)) != -1) {
                            fs.write(buffer, 0, readSize);
                        }
                        fs.close();
                    }
                }
                System.out.println("unpacked into classpath.");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Boogiepants.main(argv);
    }

    /**
     * Returns the file name of a temporary copy of <code>input</code> content.
     */
    private static String copyInputStreamToTmpFile(InputStream input, String suffix) throws IOException {
        File tmpFile = File.createTempFile("extension", suffix);
        tmpFile.deleteOnExit();
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(tmpFile));
            byte[] buffer = new byte[8096];
            int size;
            while ((size = input.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
        }
        return tmpFile.toString();
    }
}
