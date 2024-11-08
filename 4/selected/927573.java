package name.manana.jarina;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import name.manana.jarina.CacheJars.OneJarEntry;
import org.apache.log4j.Logger;

/**
 * 
 * @author jan marcis
 */
public class FileUtil {

    private static final Logger log = Logger.getLogger(FileUtil.class);

    private String eclipseProjectDir;

    private String suffix;

    private String[] haveToContain;

    private String[] doNotContains;

    private static final FileUtil instance = new FileUtil();

    protected FileUtil() {
    }

    public static FileUtil getInstance(String projectDir) {
        instance.eclipseProjectDir = projectDir;
        return instance;
    }

    public static FileUtil getInstance() {
        return instance;
    }

    public HashSet<File> getAllFiles(String directory, String suffix, String[] musiObsahovat, String[] doNotContains) {
        log.debug("nacitani vsech souboru z adresare: " + directory);
        this.suffix = suffix;
        this.haveToContain = musiObsahovat;
        this.doNotContains = doNotContains;
        File adr = new File(directory);
        HashSet<File> jars = new HashSet<File>();
        processDirectory(jars, adr);
        return jars;
    }

    private HashSet<File> processDirectory(HashSet<File> jars, File adr) {
        File[] allEntries = adr.listFiles();
        for (File entry : allEntries) {
            if (entry.isDirectory()) {
                processDirectory(jars, entry);
            } else {
                File[] poleJaru = adr.listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        if (!name.endsWith(suffix)) {
                            return false;
                        }
                        for (String ano : haveToContain) {
                            if (!name.contains(ano)) {
                                return false;
                            }
                        }
                        for (String ne : doNotContains) {
                            if (name.contains(ne)) {
                                return false;
                            }
                        }
                        return true;
                    }
                });
                jars.addAll(Arrays.asList(poleJaru));
            }
        }
        return jars;
    }

    /**
     * Copied from www.java2s.com
     * 
     * @param fromFile
     * @param toFile
     */
    public void backupFile(File fromFile, File toFile) {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * FIXME THIS 	should be rewritten!!
     * 
     * @param fileNameWithPath
     * @return
     * 		string array
     */
    public String[] getFileStrings(String fileNameWithPath) {
        ArrayList<String> semiResult = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileNameWithPath));
            String line = "";
            while (null != (line = reader.readLine())) {
                semiResult.add(line);
            }
            reader.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return semiResult.toArray(new String[200]);
    }

    void checkProjectFiles(String eclipseProjectDir) throws FileNotFoundException {
        File projectFile = new File(eclipseProjectDir + "/.project");
        if (!projectFile.exists() || !getClasspathFile().exists()) {
            throw new FileNotFoundException("Given directory (" + eclipseProjectDir + ") does not contain consistent eclipse project (.classpath, .project).");
        } else {
            log.debug("both project files found.");
        }
    }

    File getClasspathFile() {
        return new File(eclipseProjectDir + "/.classpath");
    }

    String getProjectSourceDirectory(String eclipseProjectDir) {
        XMLClasspathUtil xmlutil = null;
        try {
            xmlutil = new XMLClasspathUtil(getClasspathFile());
        } catch (IOException e) {
            log.error("Problem parsing/reading classpath file.");
        }
        return eclipseProjectDir + "/" + xmlutil.getSourceDirs().get(0);
    }

    public void completeClasspath(Set<OneJarEntry> result) {
        XMLClasspathUtil xmlutil = null;
        try {
            xmlutil = new XMLClasspathUtil(getClasspathFile());
            for (OneJarEntry jarEntry : result) {
                xmlutil.addClasspathEntry(jarEntry.getJarPath());
            }
            xmlutil.saveXMLDocument();
        } catch (IOException e) {
            log.error("Problem parsing/reading classpath file.");
        }
    }
}
