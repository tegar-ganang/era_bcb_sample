package net.sf.sail.core.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.sail.common.apps.PreviewCurnitFile;
import net.sf.sail.core.beans.Pod;
import net.sf.sail.core.beans.UnknownPodException;
import net.sf.sail.core.curnit.Curnit;
import net.sf.sail.core.curnit.CurnitArchive;
import net.sf.sail.core.curnit.CurnitArchiveResolver;
import net.sf.sail.core.curnit.CurnitFile;
import net.sf.sail.core.curnit.PodArchiveResolver;
import net.sf.sail.core.uuid.CurnitUuid;
import net.sf.sail.core.uuid.PodUuid;
import org.apache.commons.io.IOUtils;

/**
 * 
 * @author turadg
 * AUDIT07-
 */
public class BinaryUtils {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(BinaryUtils.class.getName());

    /**
	 * Takes a URL that points to a resource within a JAR, unzips it onto the
	 * filesystem, and returns a file: url to it
	 * 
	 * @param location
	 *            points to a resource within a JAR
	 * @return file: URL to the specified jar: URL resource
	 * @throws IOException
	 */
    public static URL toFileUrl(URL location) throws IOException {
        String protocol = location.getProtocol().intern();
        if (protocol != "jar") throw new IOException("cannot explode " + location);
        JarURLConnection juc = (JarURLConnection) location.openConnection();
        String path = juc.getEntryName();
        String parentPath = parentPathOf(path);
        File tempDir = createTempDir("jartemp");
        JarFile jarFile = juc.getJarFile();
        for (Enumeration<JarEntry> en = jarFile.entries(); en.hasMoreElements(); ) {
            ZipEntry entry = en.nextElement();
            if (entry.isDirectory()) continue;
            String entryPath = entry.getName();
            if (entryPath.startsWith(parentPath)) {
                File dest = new File(tempDir, entryPath);
                dest.getParentFile().mkdirs();
                InputStream in = jarFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(dest);
                IOUtils.copy(in, out);
                dest.deleteOnExit();
            }
        }
        File realFile = new File(tempDir, path);
        return realFile.toURL();
    }

    /**
	 * @param path
	 * @return
	 */
    private static String parentPathOf(String path) {
        int from = 0;
        int to = path.indexOf("/");
        if (to < 0) return "";
        String parentPath = path.substring(from, to);
        return parentPath;
    }

    /**
	 * @param tempDirName
	 *            TODO
	 * @throws IOException
	 */
    public static File createTempDir(String tempDirName) throws IOException {
        File tempDir = File.createTempFile("sail-" + tempDirName, "dir");
        boolean success = tempDir.delete();
        if (!success || tempDir.exists()) {
            throw new IOException("Failed to delete file to be converted to dir");
        }
        success = tempDir.mkdirs();
        if (!success || !tempDir.isDirectory()) {
            throw new IOException("failed to create temp dir");
        }
        tempDir.deleteOnExit();
        return tempDir;
    }

    public static URL makeJarUrl(URL jarUrl, String pathInJar) {
        try {
            return new URL("jar:" + jarUrl + "!/" + pathInJar);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @param externalForm
	 * @return
	 * @throws IOException
	 */
    public static File tempFileForUrl(String externalForm) throws IOException {
        URL url = new URL(externalForm);
        return tempFileForUrl(url);
    }

    /**
	 * @param curnitArchiveUrl
	 * @param deleteOnExit
	 * @return
	 * @throws IOException
	 */
    public static File tempFileForUrl(URL url, boolean deleteOnExit) throws IOException {
        File tempFile = File.createTempFile("tempFileForUrl-", ".jar");
        if (deleteOnExit) {
            tempFile.deleteOnExit();
        }
        org.apache.commons.io.FileUtils.copyURLToFile(url, tempFile);
        return tempFile;
    }

    /**
	 * @param curnitArchiveUrl
	 * @return
	 * @throws IOException
	 */
    public static File tempFileForUrl(URL url) throws IOException {
        return tempFileForUrl(url, true);
    }

    /**
	 * @param path
	 * @return
	 * @throws IOException
	 */
    public static File fileForCommandArgument(String path) throws IOException {
        if (path.startsWith("http://")) {
            logger.info("opening url " + path);
            return tempFileForUrl(path);
        }
        logger.info("opening file " + path);
        return new File(path);
    }

    /**
	 * @param path
	 * @return
	 * @throws IOException
	 */
    public static URL urlForCommandArgument(String path) throws IOException {
        if (path.startsWith("http://")) {
            logger.info("opening url " + path);
            return new URL(path);
        }
        logger.info("opening file " + path);
        return new File(path).toURL();
    }

    public static URL addToArchive(Pod pod, ZipOutputStream podArchiveOutputStream, String filename, InputStream source) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        podArchiveOutputStream.putNextEntry(entry);
        IOUtils.copy(source, podArchiveOutputStream);
        podArchiveOutputStream.closeEntry();
        return PodArchiveResolver.withinPodArchive(pod, filename);
    }

    /**
	 * Note: ignores the previous archive associated with the pod
	 * 
	 * @param pod
	 * @return a new JarOutputStream to which to write to the new archive
	 * @throws IOException
	 */
    public static JarOutputStream newPodArchiveFor(Pod pod) throws IOException {
        File podArchiveFile = File.createTempFile("podarchive", ".jar");
        PodArchiveResolver.getSystemResolver().put(pod.getPodId(), podArchiveFile.toURL());
        JarOutputStream podArchiveOut = new JarOutputStream(new FileOutputStream(podArchiveFile));
        return podArchiveOut;
    }

    /**
	 * TODO think of way to avoid this hack
	 * 
	 * @param pod
	 * @param podToArchive
	 * @throws IOException
	 */
    public static void cleanClosePodArchive(Pod pod, JarOutputStream podArchive) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pod.getPodId().toByteArray());
        BinaryUtils.addToArchive(pod, podArchive, "POD-ID.TXT", inputStream);
        podArchive.close();
    }

    /**
	 * @param args
	 * @return
	 * @throws IOException
	 */
    public static CurnitFile curnitFileForArgs(String[] args) throws IOException {
        URL curnitUrl = PreviewCurnitFile.getCurnitArchiveUrl(args);
        File localfile = tempFileForUrl(curnitUrl);
        CurnitFile curnitFile = new CurnitFile(localfile);
        return curnitFile;
    }

    /**
	 * Write to the output stream a curnit archive of a curnit with the
	 * specified curnit id, title and root pod
	 * 
	 * @return
	 * @throws Exception
	 */
    public static void writeCurnit(CurnitUuid curnitId, String curnitTitle, Pod rootPod, OutputStream out) throws Exception {
        Curnit curnit = new Curnit();
        curnit.setCurnitId(curnitId);
        curnit.setTitle(curnitTitle);
        curnit.setRootPodId(rootPod.getPodId());
        CurnitArchive.writeArchive(curnit, out);
    }

    /**
	 * Transform integer id into a valid curnit uuid
	 * 
	 * @param someInt
	 * @return uuid of curnit for given serial id
	 */
    public static CurnitUuid curnitIdFromInt(int someInt) {
        String PRE = "cccccccc-";
        String POST = "-0000-0000-000000000000";
        DecimalFormat df = new DecimalFormat("0000");
        String middle = df.format(someInt % 1000);
        String idStr = PRE + middle + POST;
        return new CurnitUuid(idStr);
    }

    /**
	 * loads a curnit from a file, returns the curnit object
	 * 
	 * @param file - curnit archive file
	 * @return
	 * @throws IOException
	 * @throws UnknownPodException 
	 */
    public static Curnit loadCurnit(File file) throws IOException, UnknownPodException {
        CurnitFile curnitFile = new CurnitFile(file);
        Curnit curnit = curnitFile.getCurnit();
        CurnitUuid curnitId = curnit.getCurnitId();
        CurnitArchiveResolver.getSystemResolver().put(curnitId, file.toURL());
        curnit.assemble();
        return curnit;
    }

    /**
	 * @param rootPodId
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws MalformedURLException
	 */
    public static File tempCurnitArchive(PodUuid rootPodId) throws IOException, FileNotFoundException, MalformedURLException {
        File tempFile = File.createTempFile("tempCurnit", ".jar");
        Curnit curnit = new Curnit();
        curnit.setCurnitId(curnitIdFromInt((int) Math.random()));
        curnit.setTitle("PREVIEW" + new Date());
        try {
            curnit.setRootPodId(rootPodId);
        } catch (UnknownPodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
        CurnitArchive.writeArchive(curnit, out);
        return tempFile;
    }
}
