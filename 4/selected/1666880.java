package net.datao.utils;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MozSwingInstaller {

    public MozSwingInstaller() {
    }

    private static void log(String s) {
        System.out.println(s);
    }

    private static void unzip(ZipInputStream zin, File destDir) throws Exception {
        log("unzip begin");
        double progress = 1.0D;
        int numEntries = 0;
        ZipEntry ze;
        while ((ze = zin.getNextEntry()) != null) {
            numEntries++;
            if (ze.isDirectory()) {
                zin.closeEntry();
            } else {
                String fname = (new StringBuilder()).append(destDir.getAbsolutePath()).append(File.separator).append(ze.getName().replace('/', File.separatorChar)).toString();
                File f = new File(fname);
                if (f.getParent() != null) {
                    File parent = f.getParentFile();
                    parent.mkdirs();
                }
                byte buf[] = new byte[2048];
                FileOutputStream fos = new FileOutputStream(f);
                int len;
                while ((len = zin.read(buf)) != -1) fos.write(buf, 0, len);
                fos.close();
                zin.closeEntry();
                progress += 0.33333333333333331D;
                if (progress > 98D) progress = 98D;
            }
        }
        zin.close();
        log((new StringBuilder()).append("unzip end (").append(numEntries).append(" entries)").toString());
    }

    private static String getPlatform() {
        String osname = System.getProperty("os.name");
        if ("Mac OS X".equals(osname)) return "macosx";
        if ("Linux".equals(osname)) return "linux";
        if ("SunOS".equals(osname)) return "solaris"; else return "win32";
    }

    public static String xulrunnerDirName = "/datao/xulrunner-1.9";

    public static void installMozSwing() throws Exception {
        try {
            File xulrunnerHome = getXulrunnerHome();
            File unzipDir = getUnzippingDirectory();
            if (xulrunnerHome != null && xulrunnerHome.exists()) log((new StringBuilder()).append("xulrunner is already installed at: ").append(xulrunnerHome).toString()); else if (!unzipDir.exists() && !unzipDir.mkdirs()) {
                log("unable to create directory " + unzipDir.getAbsolutePath());
                log("maybe xulrunner is already here. if not, then the browser won't initialize");
            } else {
                URL u = MozSwingInstaller.class.getClassLoader().getResource("org/mozilla/browser/jnlp/native.zip");
                log((new StringBuilder()).append("url of installer class: ").append(u).toString());
                log((new StringBuilder()).append("unzipping to: ").append(unzipDir.getAbsolutePath()).toString());
                ZipInputStream zin = new ZipInputStream(new BufferedInputStream(u.openStream()));
                unzip(zin, unzipDir);
                log("instalation succeeded");
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log("failed->" + e.getMessage());
            log((new StringBuilder()).append("instalation failed ").append(sw.toString()).toString());
        }
    }

    public static File getXulrunnerHome() {
        String xulrunnerLocation = getXulrunnerLocation();
        File xulrunnerHome = new File(xulrunnerLocation);
        return xulrunnerHome;
    }

    public static String getXulrunnerLocation() {
        return getUnzippingLocation() + "/native/" + getPlatform() + "/xulrunner";
    }

    private static File getUnzippingDirectory() {
        String unzippingLocation = getUnzippingLocation();
        File unzippingDirectory = new File(unzippingLocation);
        return unzippingDirectory;
    }

    private static String getUnzippingLocation() {
        return System.getProperty("java.io.tmpdir") + xulrunnerDirName;
    }
}
