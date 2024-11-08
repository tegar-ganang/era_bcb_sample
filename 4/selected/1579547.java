package au.com.gworks.jump.app.wiki.server.mockimpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.javaongems.runtime.io.PathUtils;
import org.javaongems.runtime.lang.StringUtils;
import au.com.gworks.jump.io.PathStatus;
import au.com.gworks.jump.io.ResourceAttributes;
import au.com.gworks.jump.io.SystemFailure;
import au.com.gworks.jump.system.ApplicationManager;

/**
 * Clearly this provider has no versioning, transaction and delta diff support.
 * Its purpose is to provide some basic/hard coded data provisioning for the 
 * sample gems application.
 * 
 * As new widgets are added to Gems, feel free to embed data in here to exercise
 * those widgets.
 */
public class FileSystemProvider {

    private static final Logger LOGGER = Logger.getLogger(FileSystemProvider.class);

    public static final String BASE_DIR = "repository";

    public static final String FS_DIR = "fs.dir";

    public static final String LUCENE_DIR = "lucene.dir";

    public static final Integer HEAD_REVISION = ResourceAttributes.NULL_HEAD_REVISION;

    private static final ThreadLocal fsProvider = new ThreadLocal();

    private static String rootContext;

    public final String namespace;

    public final String requester;

    private final String namespaceRootCtx;

    private final int namespaceRootCtxLen;

    public static FileSystemProvider getInstance() {
        return (FileSystemProvider) fsProvider.get();
    }

    public FileSystemProvider(String ns, String user) {
        namespace = ns;
        requester = user;
        namespaceRootCtx = rootContext + PathUtils.FORWARD_SLASH + namespace + PathUtils.FORWARD_SLASH + FS_DIR;
        namespaceRootCtxLen = namespaceRootCtx.length();
        fsProvider.set(this);
    }

    public void close() {
        fsProvider.set(null);
    }

    public static List listSpaces() {
        File root = new File(rootContext);
        File[] spaces = root.listFiles();
        List ret = new ArrayList();
        for (int i = 0; i < spaces.length; i++) {
            if (spaces[i].isDirectory()) {
                String name = spaces[i].getName();
                ret.add(name);
            }
        }
        return ret;
    }

    public String openFileAsText(String resource) throws IOException {
        StringWriter wtr = new StringWriter();
        InputStreamReader rdr = new InputStreamReader(openFile(resource));
        try {
            IOUtils.copy(rdr, wtr);
        } finally {
            IOUtils.closeQuietly(rdr);
        }
        return wtr.toString();
    }

    public InputStream openFile(String resource) throws IOException {
        File file = prepareFsReferenceAsFile(resource);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        return bis;
    }

    public void writeFile(String resource, InputStream is) throws IOException {
        File f = prepareFsReferenceAsFile(resource);
        FileOutputStream fos = new FileOutputStream(f);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        try {
            IOUtils.copy(is, bos);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(bos);
        }
    }

    public ResourceAttributes getResourceAttributes(String resource) {
        File f = prepareFsReferenceAsFile(resource);
        return fileToResourceAttributes(f);
    }

    public List[] listResources(String folderPath) {
        File folder = prepareFsReferenceAsFile(folderPath);
        File[] list = folder.listFiles();
        ArrayList files = new ArrayList();
        ArrayList subfolders = new ArrayList();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isDirectory()) {
                subfolders.add(fileToFolderName(list[i]));
            } else {
                files.add(fileToResourceAttributes(list[i]));
            }
        }
        ArrayList[] ret = new ArrayList[] { subfolders, files };
        return ret;
    }

    public int getPathStatus(String resource) {
        File res = prepareFsReferenceAsFile(resource);
        if (res.isDirectory()) return PathStatus.IS_FOLDER;
        if (res.exists()) return PathStatus.IS_DOCUMENT;
        return PathStatus.DOESNT_EXIST;
    }

    public boolean isAdministrator() {
        boolean admin = ((requester.equals(namespace) || ApplicationManager.isAdministrator(requester)));
        return admin;
    }

    private String fileToFolderName(File f) {
        String resRef = prepareRepositoryReference(f);
        String name = FilenameUtils.getName(resRef);
        return name;
    }

    private ResourceAttributes fileToResourceAttributes(File f) {
        String repRef = prepareRepositoryReference(f);
        String parent = PathUtils.toParentPath(repRef);
        String name = FilenameUtils.getName(repRef);
        String uuid = "you-got-2-b-joking";
        long lastMod = f.lastModified();
        String lastUpdr = "mickey mouse";
        long size = f.length();
        Integer rev = HEAD_REVISION;
        Properties props = new Properties();
        Integer revLastChg = HEAD_REVISION;
        ResourceAttributes ret = new ResourceAttributes(parent, name, uuid, lastMod, lastUpdr, size, rev, props, revLastChg);
        return ret;
    }

    private String prepareRepositoryReference(File f) {
        String abs = f.getAbsolutePath();
        String ret = abs.substring(namespaceRootCtxLen);
        if (StringUtils.isEmpty(ret)) ret = PathUtils.FORWARD_SLASH; else ret = PathUtils.toUnixStylePath(ret);
        return ret;
    }

    private File prepareFsReferenceAsFile(String resource) {
        String ref = prepareFsReferenceAsStr(resource);
        return new File(ref);
    }

    private String prepareFsReferenceAsStr(String resource) {
        String ret = namespaceRootCtx + PathUtils.prependSlashIfRequired(resource);
        return ret;
    }

    static void initialiseHome(String home) {
        String overrideHome = System.getProperty("juls.working.dir");
        if (!StringUtils.isEmpty(overrideHome)) home = overrideHome;
        String temp = PathUtils.appendSlashIfRequired(home) + BASE_DIR;
        File testCtx = new File(temp);
        if (!testCtx.isDirectory()) throw new SystemFailure(LOGGER, "Invalid mock repository [" + temp + "]. Tomcat should be launched from the tomcat/bin directory", new IllegalStateException("Mock repository"));
        rootContext = temp;
    }
}
