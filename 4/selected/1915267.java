package net.assimilator.resources.util;

import net.assimilator.core.provision.SoftwareDownloadRecord;
import net.assimilator.core.provision.SoftwareLoad;
import net.assimilator.core.provision.SoftwareLoad.PostInstallAttributes;
import net.assimilator.core.provision.SoftwareLoad.SoftwareDownloadAttributes;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The SoftwareLoadManager class provides support to manage the download and
 * installation of a SoftwareLoad.
 *
 * @version $Id: SoftwareLoadManager.java 125 2007-04-17 00:47:13Z khartig $
 */
public class SoftwareLoadManager {

    /**
     * The root directory to download the software
     */
    private String installPath;

    /**
     * The SoftwareLoad
     */
    private SoftwareLoad softwareLoad;

    /**
     * The SoftwareDownloadRecord of the downloaded software
     */
    private SoftwareDownloadRecord softwareRecord;

    /**
     * The post-install SoftwareDownloadRecord
     */
    private SoftwareDownloadRecord postInstallRecord;

    /**
     * The files extracted during post-install
     */
    private List postInstallExtractList;

    /**
     * Logger
     */
    private static transient Logger logger = Logger.getLogger("net.assimilator.resources.util");

    /**
     * Create an instance of the SoftwareLoadManager.
     *
     * @param installPath  The root directory to download the software. If the
     *                     directory does not exist, it will be created. Read and write access
     *                     permissions are required to the directory
     * @param softwareLoad The SoftwareLoad
     */
    public SoftwareLoadManager(String installPath, SoftwareLoad softwareLoad) {
        if (installPath == null) throw new NullPointerException("installPath is null");
        if (softwareLoad == null) throw new NullPointerException("softwareLoad is null");
        this.installPath = installPath;
        this.softwareLoad = softwareLoad;
        logger.finest("Successfully constructed SoftwareLoadManager.");
    }

    /**
     * Performs software download for a SoftwareLoad.
     *
     * @return The SoftwareDownloadRecord based on
     *         attributes from the downloaded software.
     * @throws java.io.IOException if the dowload fails.
     */
    public SoftwareDownloadRecord download() throws IOException {
        if (softwareRecord != null) return (softwareRecord);
        SoftwareDownloadAttributes downloadAttrs = softwareLoad.getSoftwareDownloadAttributes();
        return (doDownload(downloadAttrs, false));
    }

    /**
     * Performs software download for SoftwareDownloadAttributes
     *
     * @param downloadAttrs The SoftwareDownloadAttributes
     * @param postInstall   Whether this is for the post-install task
     * @return The SoftwareDownloadRecord based on
     *         attributes from the downloaded software.
     * @throws java.io.IOException if the download fails.
     */
    private SoftwareDownloadRecord doDownload(SoftwareDownloadAttributes downloadAttrs, boolean postInstall) throws IOException {
        String installRoot;
        int extractedSize = 0;
        long extractTime = 0;
        boolean unarchived = false;
        if (downloadAttrs == null) {
            logger.warning("downloadAttrs is null in SoftwareLoadManager");
            throw new NullPointerException("downloadAttrs is null");
        }
        logger.finest("Doing a Software download from location = " + downloadAttrs.getLocation());
        URL location = downloadAttrs.getLocation();
        String extension = downloadAttrs.getInstallRoot();
        boolean unarchive = downloadAttrs.unarchive();
        if (extension.indexOf("/") != -1) installRoot = extension.replace('/', File.separatorChar); else installRoot = extension.replace('\\', File.separatorChar);
        File targetPath = new File(makeFileName(installPath, installRoot));
        if (!targetPath.exists()) {
            targetPath.mkdirs();
            if (!targetPath.exists()) throw new IOException("Failed to create : " + installPath);
        }
        if (!targetPath.canWrite()) throw new IOException("Can not write to : " + installPath);
        String source = location.toExternalForm();
        int index = source.lastIndexOf("/");
        if (index == -1) throw new IllegalArgumentException("Dont know how to install : " + source);
        String software = source.substring(index + 1);
        String target = targetPath.getCanonicalPath();
        logger.finest("Installing software at " + target);
        File targetFile = new File(makeFileName(target, software));
        long t0 = System.currentTimeMillis();
        URLConnection con = location.openConnection();
        int downloadedSize = writeFileFromInputStream(con.getInputStream(), targetFile);
        long t1 = System.currentTimeMillis();
        long downloadTime = t1 - t0;
        Date downloadDate = new Date();
        if (unarchive) {
            t0 = System.currentTimeMillis();
            Object[] results = extract(targetPath, targetFile);
            t1 = System.currentTimeMillis();
            extractedSize = (Integer) results[0];
            if (postInstall) postInstallExtractList = (List) results[1];
            extractTime = t1 - t0;
            unarchived = true;
        }
        softwareRecord = new SoftwareDownloadRecord(location, target, software, downloadDate, downloadedSize, extractedSize, unarchived, downloadTime, extractTime);
        return (softwareRecord);
    }

    /**
     * Given an InputStream this method will write the contents to the desired
     * File.
     *
     * @param in InputStream
     * @param file The File object to write to
     * @return The size of what was written
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
        logger.finest("Total bytes written = " + totalWrote + ", for file = " + file.getCanonicalPath());
        return (totalWrote);
    }

    /**
     * Extract the archive.
     *
     * @param directory Directory location where archive is extracted.
     * @param archive   File object representing archive.
     * @return An Object array with 2 elements. The first element being the
     *         size of what was extracted, the second element being a List of what was
     *         extracted
     * @throws java.io.IOException If the extraction fails.
     */
    private Object[] extract(File directory, File archive) throws IOException {
        int extractSize = 0;
        ZipFile zipFile = null;
        List<File> extractList = new ArrayList<File>();
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
                        if (!targetPath.exists()) throw new IOException("Failed to create : " + installPath);
                    }
                    if (!targetPath.canWrite()) throw new IOException("Can not write to : " + installPath);
                    InputStream in = zipFile.getInputStream(zipEntry);
                    extractSize += writeFileFromInputStream(in, file);
                }
            }
        } finally {
            if (zipFile != null) zipFile.close();
        }
        return (new Object[] { extractSize, extractList });
    }

    /**
     * Perform post-install task(s) as described by the PostInstallAttributes
     * object.
     *
     * @return A SoftwareDownloadRecord for the post
     *         install if software was downloaded to perform the post install task(s).
     *         If no software was downloaded to perform the task(s), return null.
     * @throws java.io.IOException if the post install execution fails.
     */
    public SoftwareDownloadRecord postInstall() throws IOException {
        if (softwareRecord == null) throw new IllegalStateException("Software has not been downloaded.");
        PostInstallAttributes postInstall = softwareLoad.getPostInstallAttributes();
        if (postInstall == null) return (null);
        String path = softwareRecord.getPath();
        try {
            SoftwareDownloadAttributes downloadAttrs = postInstall.getSoftwareDownloadAttributes();
            if (downloadAttrs != null) {
                postInstallRecord = doDownload(downloadAttrs, true);
                path = postInstallRecord.getPath();
            }
            String executionTarget = postInstall.getExecutionTarget();
            if (executionTarget != null) {
                String cmd = getCommandLine(makeFileName(path, executionTarget));
                if (System.getProperty("net.assimilator.debug") != null) System.out.println("Executing : " + cmd);
                Process process = Runtime.getRuntime().exec(cmd);
                OutputStream stdout = null;
                OutputStream stderr = null;
                if (System.getProperty("net.assimilator.debug") != null) {
                    stdout = System.out;
                    stderr = System.err;
                }
                new StreamRedirector(process.getInputStream(), stdout).start();
                new StreamRedirector(process.getErrorStream(), stderr).start();
                int result = 0;
                try {
                    result = process.waitFor();
                } catch (InterruptedException ie) {
                    if (System.getProperty("net.assimilator.debug") != null) {
                        System.out.println("post installation facility [" + cmd + "] interrupted");
                    }
                }
                if (result != 0) throw new RuntimeException("executing post installation facility [" + cmd + "] failed");
            }
            if (postInstall.removeOnCompletion()) {
                if (postInstallRecord != null) {
                    remove(new File(makeFileName(postInstallRecord.getPath(), postInstallRecord.getName())));
                }
                if (postInstallExtractList != null) {
                    for (Object aPostInstallExtractList : postInstallExtractList) remove((File) aPostInstallExtractList);
                }
            }
        } catch (IOException e) {
            if (postInstallRecord != null) remove(postInstallRecord);
            throw e;
        }
        return (postInstallRecord);
    }

    /**
     * Get the command line for the execution target.
     *
     * @param arg The command to run.
     * @return The command line string used for execution. It is adjusted to the
     *         the current operating system syntax.
     */
    private String getCommandLine(String arg) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            int ndx = arg.indexOf(" ");
            if (ndx != -1) arg = "\"" + arg + "\"";
        }
        String test = arg.toLowerCase();
        if (test.endsWith(".jar") || test.endsWith(".jar\"")) {
            arg = "java -jar " + arg;
        }
        String cmd;
        if (osName.indexOf("nt") > -1 || osName.indexOf("windows 2000") > -1 || osName.indexOf("windows 2003") > -1 || osName.indexOf("windows xp") > -1 || osName.indexOf("windows vista") > -1) {
            cmd = "cmd.exe /C " + arg;
        } else if (osName.indexOf("windows 9") > -1) {
            cmd = "command.com /C " + arg;
        } else {
            cmd = arg;
        }
        return (cmd);
    }

    /**
     * Remove installed software.
     */
    public void remove() {
        if (softwareRecord == null) throw new IllegalStateException("software has not been downloaded");
        remove(softwareRecord);
        if (postInstallRecord != null) remove(postInstallRecord);
    }

    /**
     * Remove installed software.
     *
     * @param record The SoftwareDownloadRecord to remove
     */
    public static void remove(SoftwareDownloadRecord record) {
        if (record == null) throw new NullPointerException("record is null");
        File software = new File(makeFileName(record.getPath(), record.getName()));
        if (!software.exists()) {
            System.err.println("Software recorded at " + "[" + software.getAbsolutePath() + "] does not exist " + "on the file system, removal aborted");
            return;
        }
        if (record.unarchived()) {
            try {
                ZipFile zipFile = new ZipFile(software);
                Enumeration zipEntries = zipFile.entries();
                while (zipEntries.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
                    File file = new File(record.getPath() + File.separator + zipEntry.getName());
                    remove(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            remove(software);
            File softwareDirectory = new File(record.getPath());
            String[] list = softwareDirectory.list();
            if (list.length == 0 && !softwareDirectory.getName().equals("native")) remove(softwareDirectory);
        } else {
            remove(software);
        }
    }

    /**
     * Remove a File.
     *
     * @param file A File object to remove.
     */
    static void remove(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) remove(f); else {
                    f.delete();
                }
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    /**
     * Make a directory or file name
     *
     * @param root      The root directory name
     * @param extension The name of the extension
     * @return A directory name
     */
    public static String makeFileName(String root, String extension) {
        String name;
        if (root.endsWith(File.separator)) name = root + extension; else name = root + File.separator + extension;
        return (name);
    }

    public static void main(String args[]) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: net.assimilator.qos.SoftwareLoadManager download-URL install-root");
                System.exit(-1);
            }
            String downloadFrom = args[0];
            String installPath = args[1];
            System.setSecurityManager(new java.rmi.RMISecurityManager());
            SoftwareLoad load = new SoftwareLoad(new SoftwareDownloadAttributes(new URL(downloadFrom), "Test", true));
            SoftwareLoadManager slm = new SoftwareLoadManager(installPath, load);
            SoftwareDownloadRecord record = slm.download();
            System.out.println("Details");
            System.out.println("-------");
            System.out.println(record.toString());
            SoftwareLoadManager.remove(record);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
