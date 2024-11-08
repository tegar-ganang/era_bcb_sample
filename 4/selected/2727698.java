package gate.util;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/** This class is used to merge a set of Jar/Zip Files in a Jar File
  * It is ignored the manifest.
  */
public class JarFiles {

    /** Debug flag */
    private static final boolean DEBUG = false;

    private StringBuffer dbgString = new StringBuffer();

    private boolean warning = false;

    String buggyJar = null;

    private static final int BUFF_SIZE = 65000;

    private Set directorySet = null;

    private byte buffer[] = null;

    public JarFiles() {
        directorySet = new HashSet();
        buffer = new byte[BUFF_SIZE];
    }

    /** This method takes the content of all jar/zip files from the set
    * jarFileNames and put them in a file with the name outputFileName.
    * If the jar entry is manifest then this information isn't added.
    * @param jarFileNames is a set of names of files (jar/zip)
    * @param destinationJarName is the name of the file which contains all the
    * classes of jarFilesNames
    */
    public void merge(Set jarFileNames, String destinationJarName) throws GateException {
        String sourceJarName = null;
        JarOutputStream jarFileDestination = null;
        JarFile jarFileSource = null;
        try {
            jarFileDestination = new JarOutputStream(new FileOutputStream(destinationJarName));
            dbgString.append("Creating " + destinationJarName + " from these JARs:\n");
            Iterator jarFileNamesIterator = jarFileNames.iterator();
            while (jarFileNamesIterator.hasNext()) {
                sourceJarName = (String) jarFileNamesIterator.next();
                jarFileSource = new JarFile(sourceJarName);
                addJar(jarFileDestination, jarFileSource);
                if (jarFileSource.getName().equals(buggyJar)) dbgString.append(sourceJarName + "...problems occured ! \n"); else dbgString.append(sourceJarName + "...added OK ! \n");
                jarFileSource.close();
            }
            jarFileDestination.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(Err.getPrintWriter());
        }
        if (warning == true) Out.prln(dbgString);
    }

    /**
    * This method adds all entries from sourceJar to destinationJar
    * NOTE: that manifest information is not added, method will throw
    * a gate Exception if a duplicate entry file is found.
    * @param destinationJar the jar that will collect all the entries
    * from source jar
    * @param sourceJar doesn't need any explanation ... DOES it?
    */
    private void addJar(JarOutputStream destinationJar, JarFile sourceJar) throws GateException {
        try {
            Enumeration jarFileEntriesEnum = sourceJar.entries();
            JarEntry currentJarEntry = null;
            while (jarFileEntriesEnum.hasMoreElements()) {
                currentJarEntry = (JarEntry) jarFileEntriesEnum.nextElement();
                if (currentJarEntry.getName().equalsIgnoreCase("META-INF/") || currentJarEntry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) continue;
                if (currentJarEntry.isDirectory() && directorySet.contains(currentJarEntry.getName())) continue;
                try {
                    if (currentJarEntry.isDirectory()) directorySet.add(currentJarEntry.getName());
                    destinationJar.putNextEntry(new JarEntry(currentJarEntry.getName()));
                    InputStream currentEntryStream = sourceJar.getInputStream(currentJarEntry);
                    int bytesRead = 0;
                    while ((bytesRead = currentEntryStream.read(buffer, 0, BUFF_SIZE)) != -1) destinationJar.write(buffer, 0, bytesRead);
                    currentEntryStream.close();
                    destinationJar.flush();
                    destinationJar.closeEntry();
                } catch (java.util.zip.ZipException ze) {
                    if (!currentJarEntry.isDirectory()) {
                        warning = true;
                        buggyJar = sourceJar.getName();
                        Out.prln("WARNING: Duplicate file entry " + currentJarEntry.getName() + " (this file will be discarded)..." + "It happened while adding " + sourceJar.getName() + " !\n");
                        dbgString.append(currentJarEntry.getName() + " file from " + sourceJar.getName() + " was discarded :( !\n");
                    }
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace(Err.getPrintWriter());
        }
    }

    /** args[0] is the final jar file and the other are the set of
    * jar file names
    * e.g. java gate.util.JarFiles libs.jar ../lib/*.jar ../lib/*.zip
    * will create a file calls libs.jar which will contain all
    * jar files and zip files
    */
    public static void main(String[] args) {
        if (args.length < 2) {
            Err.println("USAGE : JarFiles arg0 arg1 ... argN" + "(must be at least 2 args)");
        } else {
            JarFiles jarFiles = new JarFiles();
            Set filesToMerge = new HashSet();
            for (int i = 1; i < args.length; i++) {
                filesToMerge.add(args[i]);
            }
            try {
                jarFiles.merge(filesToMerge, args[0]);
            } catch (GateException ge) {
                ge.printStackTrace(Err.getPrintWriter());
            }
        }
    }
}
