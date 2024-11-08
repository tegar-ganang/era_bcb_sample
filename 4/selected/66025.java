package net.emotivecloud.vrmm.vtm.resourcefabrics.repositoryimagecore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.emotivecloud.vrmm.vtm.VtMException;
import net.emotivecloud.vrmm.vtm.commons.ExecuteScript;
import net.emotivecloud.vrmm.vtm.commons.Lock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contacts and manage the Store
 * Singleton class
 */
public class ImageStoreManager {

    private static ImageStoreManager instance = null;

    private ExecuteScript es;

    private Lock sl;

    private String script;

    private Log log = LogFactory.getLog(ImageStoreManager.class);

    private String path = "/home/greig/ResourceManager";

    private String imageBuilderScript = "sh " + path + "/scripts/ImageBuilder.sh";

    /**
	 * @throws Exception
	 */
    private ImageStoreManager() throws VtMException {
        es = new ExecuteScript();
        URL url = ImageStoreManager.class.getResource("ImageStoreManager.class");
        if (url != null) {
            path = url.toString();
            if (path.startsWith("jar:")) {
                path = path.replaceFirst("jar:", "");
                if (path.startsWith("file:")) path = path.replaceFirst("file:", "");
                System.out.println("[ImageStoreManager] path replace first file:");
                System.out.println(path);
                String jarPath = path.substring(0, path.indexOf("!"));
                imageBuilderScript = "sh " + unJar(jarPath, "bin/ImageBuilder.sh");
                unJar(jarPath, "bin/ImageBuilder.sh");
                unJar(jarPath, "bin/deb");
                unJar(jarPath, "bin/functions");
                unJar(jarPath, "bin/storeEnv.cfg");
                log.info("Using ImageBuilder script: " + imageBuilderScript);
            }
        } else log.error("Searching ImageBuilder script. Using default: " + imageBuilderScript + " URL " + url);
    }

    /**
	 * @return ImageStoreManager
	 * @throws Exception
	 */
    public static ImageStoreManager getInstance() throws VtMException {
        if (instance == null) {
            instance = new ImageStoreManager();
        }
        return instance;
    }

    /**
	 * Asks Store for a system image
	 * @param base
	 * @param release
	 * @param kernel
	 * @param version
	 * @param constraint
	 * @param packages
	 * @param mirror
	 * @param arch
	 * @param format
	 * @return String Returns image location path
	 * @throws Exception
	 */
    public String buildImage(String name, boolean base, String release, boolean kernel, String version, String constraint, String packages, String mirror, String arch, String format) throws VtMException {
        String command;
        command = imageBuilderScript + " create-image";
        command += " --arch " + arch;
        command += " --name " + name;
        if (packages != null) {
            command += " --packages " + packages;
        }
        if (kernel) {
            command += " --kernel";
            if (version != null) {
                command += " " + version;
            }
            if (constraint != null) {
                command += " --constraint " + constraint;
            }
        }
        if (format != null) {
            command += " --release " + release;
            command += " --format " + format;
        }
        if (base) {
            command += " --base-system";
        }
        if (mirror != null) {
            command += " --mirror " + mirror;
        }
        System.out.println("[RepoCore:ImageBuild] command: " + command);
        return es.mutexOperation(command);
    }

    /**
	 * Removes store if exists and creates a new store directory tree.
	 * @throws Exception
	 */
    public void createStore() throws VtMException {
        es.mutexOperation(script + " create-store");
    }

    /**
	 * Extract a given entry from its JAR file.
	 * @param jarPath
	 * @param jarEntry
	 */
    private String unJar(String jarPath, String jarEntry) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntry.substring(0, jarEntry.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            ZipEntry ze = jar.getEntry(jarEntry);
            InputStream finput = jar.getInputStream(ze);
            File bin = new File(path + "/" + jarEntry);
            FileOutputStream foutput = new FileOutputStream(bin);
            byte[] mBuffer = new byte[1024];
            int n;
            while ((n = finput.read(mBuffer)) > 0) foutput.write(mBuffer, 0, n);
            foutput.close();
            finput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path + "/" + jarEntry;
    }
}
