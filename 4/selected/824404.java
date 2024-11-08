package com.patientis.client.deploy;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.commons.io.FileUtils;
import com.patientis.framework.utility.FileSystemUtil;

/**
 * One line class description
 *
 * 
 * <br/>  
 */
public class JarFile {

    /**
	 * Name of file
	 */
    private String libraryFile = null;

    /**
	 * Library root directory
	 */
    private String rootDir = null;

    /**
	 * Subdirectory of root
	 */
    private String subDir = null;

    /**
	 * Deploy to client
	 */
    private String client = null;

    /**
	 * Deploy to server
	 */
    private String server = null;

    /**
	 * Deploy in production
	 */
    private boolean production = false;

    /**
	 * Source file
	 */
    private File sourceFile = null;

    private String version = null;

    /**
	 * @return the libraryFile
	 */
    public String getLibraryFile() {
        return libraryFile;
    }

    /**
	 * @param libraryFile the libraryFile to set
	 */
    public void setLibraryFile(String libraryFile) {
        this.libraryFile = libraryFile;
    }

    /**
	 * @return the rootDir
	 */
    public String getRootDir() {
        return rootDir;
    }

    /**
	 * @param rootDir the rootDir to set
	 */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    /**
	 * @return the subDir
	 */
    public String getSourceDir() {
        return subDir;
    }

    /**
	 * @param subDir the subDir to set
	 */
    public void setSubDir(String subDir) {
        this.subDir = subDir;
    }

    /**
	 * @return the client
	 */
    public String getClient() {
        return client;
    }

    /**
	 * @param client the client to set
	 */
    public void setClient(String client) {
        this.client = client;
    }

    /**
	 * @return the server
	 */
    public String getServer() {
        return server;
    }

    /**
	 * @param server the server to set
	 */
    public void setServer(String server) {
        this.server = server;
    }

    /**
	 * @return the production
	 */
    public boolean isProduction() {
        return production;
    }

    /**
	 * @param production the production to set
	 */
    public void setProduction(boolean production) {
        this.production = production;
    }

    /**
	 * @return the sourceFile
	 */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
	 * @param sourceFile the sourceFile to set
	 */
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public static List<JarFile> createTargetJars(String jarFileText, String sourceDir, String targetDir, boolean loadClient, boolean loadServer) throws Exception {
        List<JarFile> jars = load(jarFileText, loadClient, loadServer);
        setSourceFiles(jars, sourceDir);
        createTargetFiles(jars, targetDir, loadClient);
        return jars;
    }

    /**
	 * Set the source file on the jar
	 * 
	 * @param jars
	 * @param sourceDir
	 * @throws Exception
	 */
    private static void createTargetFiles(List<JarFile> jars, String targetDir, boolean client) throws Exception {
        String sep = FileSystemUtil.getFilePathSeparator();
        for (JarFile jar : jars) {
            if (jar.getSourceFile() != null) {
                File targetFile = new File(targetDir + sep + jar.getServer() + sep + jar.getLibraryFile());
                if (client) {
                    targetFile = new File(targetDir + sep + jar.getClient() + sep + jar.getLibraryFile());
                }
                if (!targetFile.exists() || jar.getSourceFile().length() != targetFile.length()) {
                    FileUtils.copyFile(jar.getSourceFile(), targetFile);
                }
            }
        }
    }

    /**
	 * Load the list of files
	 * 
	 * @param jarFileText
	 * @param targetDir
	 * @return
	 */
    public static List<JarFile> load(String jarFileText, boolean loadClient, boolean loadServer) throws Exception {
        String contents = FileSystemUtil.getTextContents(jarFileText);
        StringTokenizer st = new StringTokenizer(contents, "\n");
        st.nextToken();
        List<JarFile> jars = new ArrayList<JarFile>();
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            JarFile jar = new JarFile();
            StringTokenizer tabs = new StringTokenizer(line, "\t");
            int i = 0;
            while (tabs.hasMoreTokens()) {
                String s = tabs.nextToken();
                switch(i) {
                    case 0:
                        jar.setLibraryFile(s);
                        break;
                    case 1:
                        jar.setSubDir(s);
                        break;
                    case 2:
                        jar.setClient(s);
                        break;
                    case 3:
                        jar.setServer(s);
                        break;
                    case 5:
                        jar.setVersion(s);
                        break;
                }
                i++;
            }
            if ((loadClient && !jar.getClient().equalsIgnoreCase("N")) || (loadServer && !jar.getServer().equalsIgnoreCase("N"))) {
                jars.add(jar);
            }
        }
        return jars;
    }

    /**
	 * Set the source file on the jar
	 * 
	 * @param jars
	 * @param sourceDir
	 * @throws Exception
	 */
    private static void setSourceFiles(List<JarFile> jars, String sourceDir) throws Exception {
        String sep = FileSystemUtil.getFilePathSeparator();
        for (JarFile jar : jars) {
            File file = new File(sourceDir + sep + jar.getSourceDir() + sep + jar.getLibraryFile());
            if (!file.exists()) {
                System.err.println("not found " + file.getAbsolutePath());
            } else {
                jar.setSourceFile(file);
            }
        }
    }

    /**
	 * @return the version
	 */
    public String getVersion() {
        return version;
    }

    /**
	 * @param version the version to set
	 */
    public void setVersion(String version) {
        this.version = version;
    }
}
