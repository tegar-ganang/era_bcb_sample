package de.kugihan.jarCreator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import de.kugihan.dictionaryformids.dataaccess.DictionaryDataFile;
import de.kugihan.dictionaryformids.general.DictionaryException;
import de.kugihan.dictionaryformids.general.Util;
import de.kugihan.dictionaryformids.general.UtilWin;
import de.kugihan.dictionaryformids.hmi_java_me.lcdui_extension.ResourceHandler;
import de.kugihan.dictionaryformids.hmi_java_me.lcdui_extension.ResourceHandler.IconSize;
import de.kugihan.dictionaryformids.hmi_java_me.uidisplaytext.LanguageUI;

public class JarCreator {

    public static final String EXTENSION_JAR = ".jar";

    public static final String EXTENSION_JAD = ".jad";

    public static final String FILE_EMPTY_JAR_NAME = DictionaryDataFile.applicationFileNamePrefix + EXTENSION_JAR;

    public static final String FILE_EMPTY_JAD_NAME = DictionaryDataFile.applicationFileNamePrefix + EXTENSION_JAD;

    public static void main(String[] args) throws FileNotFoundException, IOException, DictionaryException {
        UtilWin utilObj = new UtilWin();
        Util.setUtil(utilObj);
        printCopyrightNotice();
        if (args.length != 3) {
            System.err.println("USAGE: java -jar JarCreator.jar dictionarydirectory emptyjar outputdirectory\n\n" + "dictionarydirectory: directory containing the dictionary files and the file DictionaryForMIDs.properties\n" + "emptydictionaryformids: directory of the empty DictionaryForMIDs.jar/.jad without dictionary files\n" + "outputdirectory: directory where the generated JAR/JAD files are written to\n\n");
            System.exit(1);
        }
        String dictionarydirectory = getPathName(args[0]);
        String emptydictionaryformids = getPathName(args[1]);
        String outputdirectory = getPathName(args[2]);
        if (!dictionarydirectory.endsWith(DictionaryDataFile.pathNameDataFiles + File.separator)) {
            System.err.println("Argument 1 (dictionarydirectory) must end with " + DictionaryDataFile.pathNameDataFiles);
            System.exit(1);
        }
        String applicationUniqueIdentifier = buildApplicationUniqueIdentifier(dictionarydirectory);
        String midletName = DictionaryDataFile.applicationFileNamePrefix + applicationUniqueIdentifier;
        String midletNameShort = "DfM" + applicationUniqueIdentifier;
        int maxMidletNameLength = 32;
        if (midletName.length() > maxMidletNameLength) midletName = midletName.substring(0, maxMidletNameLength);
        String fileNameOutputJar = outputdirectory + midletName + EXTENSION_JAR;
        String fileNameOutputJad = outputdirectory + midletName + EXTENSION_JAD;
        String propertyPath = dictionarydirectory;
        if (!utilObj.readProperties(propertyPath, false)) {
            System.err.println("Property-file cannot be accessed: " + utilObj.buildPropertyFileName(propertyPath));
            System.exit(1);
        }
        File dictDir = new File(dictionarydirectory);
        String fileNameEmptyJar = emptydictionaryformids + FILE_EMPTY_JAR_NAME;
        JarInputStream emptyJar = new JarInputStream(new FileInputStream(new File(fileNameEmptyJar)));
        File jarOutputFile = new File(fileNameOutputJar);
        long jarSize = writeJAR(midletName, midletNameShort, dictDir, emptyJar, jarOutputFile);
        System.out.println("Written JAR-file: " + fileNameOutputJar);
        File jadInputFile = new File(emptydictionaryformids + FILE_EMPTY_JAD_NAME);
        File jadOutputFile = new File(fileNameOutputJad);
        writeJAD(jarSize, midletNameShort, midletName, jadInputFile, jadOutputFile);
        System.out.println("Written JAD-file: " + fileNameOutputJad);
        System.out.println("\nYou may now create " + "DictionaryForMIDs_VVVVV_XXXYYY_ZZZ.zip\n" + "VVVVV: version of DictionaryForMIDs\n" + "XXX: language1FilePostfix\n" + "YYY: language2FilePostfix\n" + "ZZZ: dictionaryAbbreviation");
    }

    static void writeJAD(long jarSize, String midletNameShort, String midletName, File jadInputFile, File jadOutputFile) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(jadInputFile)));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jadOutputFile)));
        boolean sizeSuccessful = false;
        boolean midletNameSuccessful = false;
        boolean midlet1Successful = false;
        String line = null;
        while ((line = in.readLine()) != null) {
            boolean writeNewLine = true;
            if (line.startsWith("MIDlet-Jar-Size")) {
                out.write("MIDlet-Jar-Size: " + jarSize);
                sizeSuccessful = true;
            } else if (line.startsWith("MIDlet-Name")) {
                out.write("MIDlet-Name: " + midletNameShort);
                midletNameSuccessful = true;
            } else if (line.startsWith("MIDlet-1")) {
                out.write("MIDlet-1: " + buildMidlet1Name(midletName));
                midlet1Successful = true;
            } else if (line.startsWith("MIDlet-Jar-URL")) {
                out.write("MIDlet-Jar-URL: " + midletName + EXTENSION_JAR);
            } else if (line.indexOf("javax.microedition.io.Connector.file.read") != -1) {
                writeNewLine = false;
            } else {
                out.write(line);
            }
            if (writeNewLine) out.newLine();
        }
        in.close();
        out.close();
        if (!sizeSuccessful) {
            throw new RuntimeException("MIDlet-Jar-Size entry in jad file couldn't be changed");
        } else if (!midletNameSuccessful) {
            throw new RuntimeException("MIDlet-Name entry in jad file couldn't be changed");
        } else if (!midlet1Successful) {
            throw new RuntimeException("MIDlet-1 entry in jad file couldn't be changed");
        }
    }

    static long writeJAR(String midletName, String midletNameShort, File dictDir, JarInputStream in, File jarOutputFile) throws IOException {
        Manifest manifest = new Manifest(in.getManifest());
        Attributes manifestAttributes = manifest.getMainAttributes();
        manifestAttributes.putValue("MIDlet-Name", midletNameShort);
        manifestAttributes.putValue("MIDlet-1", buildMidlet1Name(midletName));
        JarOutputStream out = new JarOutputStream(new FileOutputStream(jarOutputFile), manifest);
        byte[] b = new byte[3000];
        int readBytes;
        JarEntry nextOne;
        while ((nextOne = in.getNextJarEntry()) != null) {
            boolean includeEntry = true;
            String[] excludeEntries = { "de/kugihan/dictionaryformids/dataaccess/zip" };
            for (int i = 0; i < excludeEntries.length; ++i) {
                if (nextOne.getName().startsWith(excludeEntries[i])) {
                    includeEntry = false;
                    break;
                }
            }
            if (isLanguageIconFileNotNeeded(nextOne.getName())) includeEntry = false;
            if (includeEntry) {
                out.putNextEntry(nextOne);
                while ((readBytes = in.read(b, 0, 3000)) != -1) {
                    out.write(b, 0, readBytes);
                }
            }
        }
        in.close();
        addDirectory(out, dictDir, "");
        out.close();
        return jarOutputFile.length();
    }

    static void addDirectory(JarOutputStream out, File dictDir, String subDir) throws IOException {
        byte[] b = new byte[3000];
        int readBytes;
        File[] dictFiles = dictDir.listFiles();
        for (int i = 0; i < dictFiles.length; i++) {
            if (!dictFiles[i].isDirectory()) {
                FileInputStream fis = new FileInputStream(dictFiles[i]);
                out.putNextEntry(new ZipEntry(DictionaryDataFile.pathNameDataFiles + "/" + subDir + dictFiles[i].getName()));
                while ((readBytes = fis.read(b)) != -1) {
                    out.write(b, 0, readBytes);
                }
                fis.close();
            } else {
                File subDirFile = dictFiles[i];
                String newSubDir = subDir + subDirFile.getName() + '/';
                addDirectory(out, subDirFile, newSubDir);
            }
        }
    }

    static String getPathName(String pathName) {
        String completePathName = pathName;
        if (!completePathName.endsWith(File.separator)) {
            completePathName = completePathName + File.separator;
        }
        return completePathName;
    }

    static String buildApplicationUniqueIdentifier(String propertyPath) throws DictionaryException {
        UtilWin utilObj = new UtilWin();
        Util.setUtil(utilObj);
        String applicationUniqueIdentifier = new String("_");
        if (!utilObj.readProperties(propertyPath, false)) {
            System.err.println("Property-file cannot be accessed: " + utilObj.buildPropertyFileName(propertyPath));
            System.exit(1);
        }
        for (int indexLanguage = 0; indexLanguage < DictionaryDataFile.numberOfAvailableLanguages; ++indexLanguage) {
            applicationUniqueIdentifier = applicationUniqueIdentifier + DictionaryDataFile.supportedLanguages[indexLanguage].languageFilePostfix;
        }
        if (DictionaryDataFile.dictionaryAbbreviation != null) {
            applicationUniqueIdentifier = applicationUniqueIdentifier + "_" + DictionaryDataFile.dictionaryAbbreviation;
        } else {
            System.err.println("Warning: property dictionaryAbbreviation is not set");
        }
        return applicationUniqueIdentifier;
    }

    static String buildMidlet1Name(String midletName) {
        return midletName + ", /icons/Application/DictionaryForMIDs.png, de.kugihan.dictionaryformids.hmi_java_me.DictionaryForMIDs";
    }

    static boolean isLanguageIconFileNotNeeded(String fileName) {
        boolean fileIsLanguageIconFile = false;
        boolean fileIsNeeded = true;
        int iconSizeCount = 0;
        ResourceHandler resourceHandlerObj = ResourceHandler.getResourceHandlerObj();
        IconSize[] iconSizes = resourceHandlerObj.iconSizes;
        boolean searchDone = false;
        while ((iconSizeCount < iconSizes.length) && (!searchDone)) {
            IconSize iconSize = iconSizes[iconSizeCount];
            for (int sizesInPixelCount = 0; sizesInPixelCount < iconSize.sizesInPixel.length; ++sizesInPixelCount) {
                int iconSizeInPixel = iconSize.sizesInPixel[sizesInPixelCount];
                String languagePrefix = resourceHandlerObj.buildIconPathName(iconSize.iconArea, iconSize.iconSizeGroup, iconSizeInPixel) + ResourceHandler.pathSeparator + LanguageUI.getUI().uiDisplayTextItemPrefixLanguage;
                if (fileName.startsWith(languagePrefix)) {
                    fileIsLanguageIconFile = true;
                    fileIsNeeded = false;
                }
                if (fileIsLanguageIconFile) {
                    for (int language = 0; language < DictionaryDataFile.numberOfAvailableLanguages; ++language) {
                        String languageDisplayText = DictionaryDataFile.supportedLanguages[language].languageDisplayText;
                        String languageIconLocation = resourceHandlerObj.getResourceLocation(resourceHandlerObj.buildIconPathName(iconSize.iconArea, iconSize.iconSizeGroup, iconSizeInPixel), resourceHandlerObj.buildIconFileName(LanguageUI.getUI().uiDisplayTextItemPrefixLanguage + languageDisplayText));
                        languageIconLocation = languageIconLocation.substring(1);
                        if (languageIconLocation.equalsIgnoreCase(fileName)) {
                            fileIsNeeded = true;
                            searchDone = true;
                            break;
                        }
                    }
                    if (searchDone) break;
                }
            }
            ++iconSizeCount;
        }
        return !fileIsNeeded;
    }

    public static void printCopyrightNotice() throws DictionaryException {
        System.out.print("\n\nDictionaryForMIDs/JarCreator, Copyright (C) 2005-2009 Mathis Karmann et al\n" + "Version : " + Util.getUtil().getApplicationVersionString() + "\n\n" + "This program comes with ABSOLUTELY NO WARRANTY\n\n" + "This program is free software under the terms and conditions of the GPL " + "(GNU \nGeneral Public License) version 2. See file COPYING. " + "If you did not receive the\nGNU General Public License along with this program " + "(file COPYING), write\nto the Free Software Foundation, Inc., " + "59 Temple Place, Suite 330, Boston,\nMA  02111-1307  USA\n\n" + "Documentation and source code is available from http://dictionarymid.sourceforge.net\n\n\n");
    }
}
