package net.sourceforge.xuse.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.sourceforge.xuse.build.BuildException;
import net.sourceforge.xuse.build.XuseOpenSourcePropertiesReader;
import net.sourceforge.xuse.log.Logger;
import com.xmlsolutions.annotation.Requirement;

public class FileUtils {

    private static final FileUtils LOG_INSTANCE = new FileUtils();

    public static final File WORKING_DIR = new File(XuseOpenSourcePropertiesReader.getXuseWorkingDir());

    public static void copy(FileInputStream in, File destination) throws IOException {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = in.getChannel();
            dstChannel = new FileOutputStream(destination).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            if (srcChannel != null) {
                srcChannel.close();
            }
            if (dstChannel != null) {
                dstChannel.close();
            }
        }
    }

    public static final boolean fileExists(String path) {
        if (path != null) {
            String safePath = path;
            try {
                safePath = URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                Logger.warn(LOG_INSTANCE, "Could not safely decode file path", uee);
            }
            File file = new File(safePath);
            return file.exists();
        }
        return false;
    }

    public static void copyLocalDirectory(File sourceLocation, File targetLocation) throws IOException {
        Logger.debug(LOG_INSTANCE, "Copying " + sourceLocation + " to " + targetLocation);
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdirs();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyLocalDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            copyFile(sourceLocation, targetLocation);
        }
    }

    public static void copyFileToDir(String file, File destDir) throws IOException {
        if (file != null) {
            if (destDir != null) {
                if (!destDir.exists()) {
                    destDir.mkdirs();
                } else if (!destDir.isDirectory()) {
                    Logger.warn(LOG_INSTANCE, "Cannot copy file " + file + " to " + destDir.getAbsolutePath() + "; destination is not a directory");
                }
                Logger.debug(LOG_INSTANCE, "Copying " + file + " to " + destDir.getAbsolutePath());
                File input = new File(file);
                String fileName = input.getName();
                File destFile = new File(destDir, fileName);
                copyFile(input, destFile);
            } else {
                Logger.warn(LOG_INSTANCE, "Cannot copy file " + file + "; destination was null");
            }
        } else {
            Logger.warn(LOG_INSTANCE, "Cannot copy file as file was null");
        }
    }

    public static void copyFile(File input, File destination) throws IOException {
        if (input != null) {
            if (input.exists()) {
                Logger.debug(LOG_INSTANCE, "Copying " + input.getAbsolutePath() + " to " + destination.getAbsolutePath());
                copy(new FileInputStream(input), destination);
            } else {
                Logger.warn(LOG_INSTANCE, "Cannot copy " + input.getAbsolutePath() + " to " + destination.getAbsolutePath() + ", file does not exist");
            }
        } else {
            Logger.warn(LOG_INSTANCE, "Cannot copy file to source or destination was null");
        }
    }

    public static void copy(InputStream in, File destination) throws IOException {
        Logger.debug(LOG_INSTANCE, "Writing: " + destination.getAbsolutePath());
        OutputStream out = new FileOutputStream(destination);
        byte[] buf = new byte[1024];
        int len;
        int totalSize = 0;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            totalSize += len;
        }
        in.close();
        out.flush();
        out.close();
        Logger.debug(LOG_INSTANCE, "Wrote: " + totalSize + " bytes");
    }

    public static void extractStyleSheets() throws IOException {
        File xslDir = new File(WORKING_DIR, XuseOpenSourcePropertiesReader.getXslDirectory());
        xslDir.mkdirs();
        copyDirectory(XuseOpenSourcePropertiesReader.getXslDirectory(), WORKING_DIR);
    }

    public static void extractResources() throws IOException {
        File resourcesDir = new File(WORKING_DIR, XuseOpenSourcePropertiesReader.getXuseResourcesDirectory());
        resourcesDir.mkdirs();
        copyDirectory(XuseOpenSourcePropertiesReader.getXuseResourcesDirectory(), WORKING_DIR);
    }

    public static void extractSchemas() throws IOException {
        File dest = new File(WORKING_DIR, XuseOpenSourcePropertiesReader.getXuseSchemaDir());
        copyDirectory("xsd", dest, false);
    }

    public static void extractSchemasToLocalDir() throws IOException {
        extractSchemas();
        copyLocalDirectory(new File(WORKING_DIR, XuseOpenSourcePropertiesReader.getXuseSchemaDir()), new File("xusexsds"));
    }

    public static void extractThemes() throws IOException {
        File dest = new File(WORKING_DIR, "themes");
        copyDirectory("themes", dest, false);
    }

    @Requirement(traceTo = { "RQ40" })
    public static void extractTemplateProject() throws IOException {
        File dest = new File(System.getProperty("user.dir"));
        Logger.debug(LOG_INSTANCE, "Extracting template project to " + dest.getCanonicalPath());
        copyDirectory(XuseOpenSourcePropertiesReader.getTemplateProjectPath(), dest, false);
    }

    public static void extractResourceFromJar(String directoryInJar, String filename, File destination) throws IOException {
        if (directoryInJar != null && destination != null) {
            JarFile jarFile = getJarFile();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            destination.getParentFile().mkdirs();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = (JarEntry) enumeration.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(directoryInJar + "/" + filename) && !entryName.endsWith("/")) {
                    InputStream in = getInputStreamFromSystemOrClasspath(entry.getName());
                    copy(in, destination);
                }
            }
        }
    }

    public static boolean hasBeenExtracted() throws IOException {
        File xslDir = new File(WORKING_DIR, XuseOpenSourcePropertiesReader.getXslDirectory());
        return xslDir.exists() && xslDir.isDirectory() && xslDir.list() != null && xslDir.list().length > 0;
    }

    public static JarFile getJarFile() throws BuildException {
        String path = "unknown";
        try {
            URL jarPath = FileUtils.class.getResource("FileUtils.class");
            Logger.debug(LOG_INSTANCE, "Getting Jar file URL: " + jarPath.toString());
            path = jarPath.toString().replaceAll("%20", " ");
            String os = System.getProperty("os.name");
            Logger.debug(LOG_INSTANCE, "OS: " + os);
            if (os != null && os.contains("windows")) {
                path = path.replaceAll("jar:file:/", "");
            } else {
                path = path.replaceAll("jar:file:", "");
            }
            path = path.substring(0, path.indexOf("!"));
            Logger.debug(LOG_INSTANCE, "Jar file location: " + path);
            return new JarFile(path);
        } catch (Throwable t) {
            throw new BuildException("Could not locate Xuse jar file: found path " + path, t);
        }
    }

    @Requirement(traceTo = { "RQ19", "RQ60" })
    public static void copyHTMLResources(String outputDir) throws IOException {
        if (outputDir != null) {
            copyHTMLResources(new File(outputDir));
        }
    }

    public static void copyPDFResources(File outputDir) throws IOException {
        if (outputDir != null) {
            extractCoreFilesPDF(outputDir);
            copyThemeFilesPDF(outputDir);
            copyCustomResourcesPDF(outputDir);
        }
    }

    public static final void extractCoreFilesPDF(File outputDir) throws IOException {
        File completeOutputDir = new File(outputDir, "themes/core/images");
        if (completeOutputDir != null) {
            if (completeOutputDir.exists()) {
                if (!completeOutputDir.isDirectory()) {
                    throw new IOException("Cannot write core PDF styles to " + completeOutputDir + " check that it is a directory");
                }
            } else {
                completeOutputDir.mkdirs();
            }
            copyLocalDirectory(new File(XuseOpenSourcePropertiesReader.getXuseThemesDir(), "core/images"), completeOutputDir);
        }
    }

    private static void copyThemeFilesPDF(File outputDir) throws IOException {
        if (outputDir != null) {
            String theme = XuseOpenSourcePropertiesReader.getHTMLTheme();
            Logger.debug(LOG_INSTANCE, "Copying " + theme + " theme files");
            File themeDir = new File(XuseOpenSourcePropertiesReader.getXuseThemesDir() + "/" + theme);
            if (XuseOpenSourcePropertiesReader.isCustomTheme()) {
                if (!themeDir.exists()) {
                    Logger.warn(LOG_INSTANCE, "Theme files for theme " + theme + " are not installed at " + themeDir.getAbsolutePath());
                }
            }
            copyLocalDirectory(new File(themeDir, "images"), new File(outputDir, "themes/" + theme + "/images"));
        }
    }

    public static void copyCustomResourcesPDF(File outputDir) throws IOException {
        Logger.debug(LOG_INSTANCE, "in copyCustomResourcesPDF()");
        File imageDir = new File(outputDir, "themes/core/images");
        if (!XuseOpenSourcePropertiesReader.isDefaultCustomerLogo()) {
            String customerLogo = XuseOpenSourcePropertiesReader.getCustomerLogo();
            Logger.debug(LOG_INSTANCE, "Customer logo: " + customerLogo);
            copyFileToDir(customerLogo, imageDir);
        }
        if (!XuseOpenSourcePropertiesReader.isDefaultCompanyLogo()) {
            String companyLogo = XuseOpenSourcePropertiesReader.getCompanyLogo();
            Logger.debug(LOG_INSTANCE, "Company logo: " + companyLogo);
            copyFileToDir(companyLogo, imageDir);
        }
        if (!XuseOpenSourcePropertiesReader.isDefaultProjectLogo()) {
            String projectLogo = XuseOpenSourcePropertiesReader.getProjectLogo();
            Logger.debug(LOG_INSTANCE, "Project logo: " + projectLogo);
            copyFileToDir(projectLogo, imageDir);
        }
    }

    public static void copyHTMLResources(File outputDir) throws IOException {
        if (outputDir != null) {
            extractCoreFiles(new File(outputDir, "themes/core"));
            copyThemeFiles(outputDir);
            copyCustomResources(outputDir);
        }
    }

    private static void copyThemeFiles(File outputDir) throws IOException {
        if (outputDir != null) {
            String theme = XuseOpenSourcePropertiesReader.getHTMLTheme();
            Logger.debug(LOG_INSTANCE, "Copying " + theme + " theme files");
            File themeDir = new File(XuseOpenSourcePropertiesReader.getXuseThemesDir() + "/" + theme);
            if (XuseOpenSourcePropertiesReader.isCustomTheme()) {
                if (!themeDir.exists()) {
                    Logger.warn(LOG_INSTANCE, "Theme files for theme " + theme + " are not installed at " + themeDir.getAbsolutePath());
                }
            }
            copyLocalDirectory(themeDir, new File(outputDir, "themes/" + theme));
        }
    }

    public static final void extractCoreFiles(String outputDir) throws IOException {
        if (outputDir != null) {
            extractCoreFiles(new File(outputDir));
        }
    }

    public static final void extractCoreFiles(File outputDir) throws IOException {
        if (outputDir != null) {
            if (outputDir.exists()) {
                if (!outputDir.isDirectory()) {
                    throw new IOException("Cannot write core HTML styles to " + outputDir + " check that it is a directory");
                }
            } else {
                outputDir.mkdirs();
            }
            copyLocalDirectory(new File(XuseOpenSourcePropertiesReader.getXuseThemesDir(), "core"), outputDir);
        }
    }

    public static void cloneTheme(String newThemeName) throws IOException {
        if (newThemeName != null) {
            File themeOutDir = new File("src/themes");
            File themeDir = new File(themeOutDir, newThemeName);
            if (themeOutDir.exists()) {
                if (!themeOutDir.isDirectory()) {
                    throw new IOException("Cannot write new theme contents to " + themeOutDir + " check that it is a directory");
                } else if (themeDir.exists()) {
                    Logger.error(LOG_INSTANCE, "Theme directory already exists - aborting");
                    return;
                }
            } else {
                themeOutDir.mkdirs();
            }
            copyLocalDirectory(new File(XuseOpenSourcePropertiesReader.getXuseThemesDir(), "xuse-standard"), themeDir);
            Logger.info(LOG_INSTANCE, "New \"" + newThemeName + "\" theme files created at " + themeOutDir.getAbsolutePath());
        }
    }

    public static void copyCustomResources(File outputDir) throws IOException {
        Logger.debug(LOG_INSTANCE, "in copyCustomResources()");
        File imageDir = new File(outputDir, "themes/core/images");
        if (XuseOpenSourcePropertiesReader.useThemeCreationMode()) {
            imageDir = new File("src/themes/core/images");
        }
        if (!XuseOpenSourcePropertiesReader.isDefaultCustomerLogo()) {
            String customerLogo = XuseOpenSourcePropertiesReader.getCustomerLogo();
            Logger.debug(LOG_INSTANCE, "Customer logo: " + customerLogo);
            copyFileToDir(customerLogo, imageDir);
        }
        if (!XuseOpenSourcePropertiesReader.isDefaultCompanyLogo()) {
            String companyLogo = XuseOpenSourcePropertiesReader.getCompanyLogo();
            Logger.debug(LOG_INSTANCE, "Company logo: " + companyLogo);
            copyFileToDir(companyLogo, imageDir);
        }
        if (!XuseOpenSourcePropertiesReader.isDefaultProjectLogo()) {
            String projectLogo = XuseOpenSourcePropertiesReader.getProjectLogo();
            Logger.debug(LOG_INSTANCE, "Project logo: " + projectLogo);
            copyFileToDir(projectLogo, imageDir);
        }
    }

    public static void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (sourceDir != null && sourceDir.isDirectory() && destDir != null) {
            if (destDir.exists() && destDir.isFile()) {
                throw new IOException("Cannot write files to location " + destDir.getCanonicalPath());
            } else if (!destDir.exists()) {
                destDir.mkdir();
            }
            List<File> srcFiles = Arrays.asList(sourceDir.listFiles());
            Iterator<File> iter = srcFiles.iterator();
            File currentFile = null;
            while (iter.hasNext()) {
                currentFile = (File) iter.next();
                if (currentFile.isDirectory()) {
                    copyDirectory(currentFile, new File(destDir, currentFile.getName()));
                } else {
                    copy(new FileInputStream(currentFile), new File(destDir, currentFile.getName()));
                }
            }
        }
    }

    public static void copyDirectory(String directoryInJar, File destination, boolean mirrorDir) throws IOException {
        Logger.debug(LOG_INSTANCE, "Copying directory from " + directoryInJar + " in jar file to " + destination.getCanonicalPath());
        if (directoryInJar != null && destination != null) {
            JarFile jarFile = getJarFile();
            Enumeration<JarEntry> enumeration = jarFile.entries();
            File destDir = mirrorDir ? new File(destination, directoryInJar) : destination;
            destDir.mkdirs();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = (JarEntry) enumeration.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(directoryInJar + "/") && !entryName.endsWith("/")) {
                    InputStream in = getInputStreamFromSystemOrClasspath(entry.getName());
                    String outputFileName = null;
                    if (mirrorDir) {
                        outputFileName = entry.getName();
                    } else {
                        int index = entry.getName().indexOf(directoryInJar) + directoryInJar.length();
                        outputFileName = entry.getName().substring(index);
                    }
                    File dest = new File(destination, outputFileName);
                    File folder = dest.getParentFile();
                    if (!folder.exists()) {
                        if (!folder.mkdirs()) {
                            throw new BuildException("Could not create folder: " + folder.getAbsolutePath());
                        }
                    }
                    if (dest.exists() && dest.isFile()) {
                        Logger.debug(LOG_INSTANCE, "Destination " + dest.getAbsolutePath() + " already exists");
                        dest.delete();
                    }
                    copy(in, dest);
                }
            }
        }
    }

    public static void copyDirectory(String directoryInJar, File destination) throws IOException {
        if (directoryInJar != null && destination != null) {
            copyDirectory(directoryInJar, destination, true);
        }
    }

    public static void copyResource(String file, String directory, File destDir) throws IOException {
        File destDirF = new File(destDir, directory);
        if (!destDirF.exists()) {
            destDirF.mkdirs();
        }
        File outfile = new File(destDirF, file);
        InputStream in = getInputStreamFromSystemOrClasspath(directory + "/" + file);
        copy(in, outfile);
    }

    public static void archive(File file) throws Exception {
        if (file != null && file.isFile() && file.canWrite() && file.exists()) {
            File newFile = new File(file.getParent(), file.getName() + "." + System.currentTimeMillis() + ".old");
            Logger.info(LOG_INSTANCE, "Archiving old file: " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
            if (!(file.renameTo(newFile))) {
                throw new IOException("Could not rename: " + file.getAbsolutePath());
            }
        }
    }

    public static void renameUpgraded(File file) throws Exception {
        renameUpgraded(file, ".xml");
    }

    public static void renameUpgraded(File file, String extension) throws Exception {
        if (file != null && file.isFile() && file.canWrite()) {
            Logger.info(LOG_INSTANCE, "Renaming upgraded file: " + file.getCanonicalPath());
            String newName = Utils.substringBefore(file.getName(), "-upgrade.xml");
            file.renameTo(new File(file.getParent(), newName + extension));
        }
    }

    public static List<String> getFilesInDir(File dir, FileFilter filter) {
        if (dir != null && dir.isDirectory()) {
            Logger.debug(LOG_INSTANCE, "Looking for files in directory: " + dir.getAbsolutePath());
            List<String> allFiles = new ArrayList<String>();
            File[] children = dir.listFiles(filter);
            Logger.debug(LOG_INSTANCE, "Found " + children.length + " children");
            for (int i = 0; i < children.length; i++) {
                if (children[i].isDirectory()) {
                    allFiles.addAll(getFilesInDir(children[i], filter));
                } else {
                    Logger.debug(LOG_INSTANCE, "Adding child: " + children[i].getAbsolutePath());
                    allFiles.add(children[i].getAbsolutePath());
                }
            }
            return allFiles;
        } else {
            Logger.warn(LOG_INSTANCE, "Could not find any files at " + dir);
        }
        return null;
    }

    public static void writeFileContents(File output, String contents) throws BuildException {
        if (output != null) {
            try {
                if (!output.exists()) {
                    output.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(output);
                fileWriter.append(contents);
                fileWriter.close();
            } catch (IOException ioe) {
                throw new BuildException("Could not write file contents", ioe);
            }
        }
    }

    private static InputStream getInputStreamFromSystemOrClasspath(String name) throws BuildException {
        File file = new File(name);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            try {
                URL url = FileUtils.class.getClassLoader().getResource(name);
                File in = new File(url.getPath());
                if (!in.exists()) {
                    InputStream inStream = FileUtils.class.getClassLoader().getResourceAsStream(name);
                    if (inStream == null) {
                        throw new BuildException("Cannot find file " + name);
                    }
                    return inStream;
                }
                return new FileInputStream(in.getAbsolutePath());
            } catch (Throwable t) {
                throw new BuildException("Error finding the file - " + name + " - in the classpath.", t);
            }
        } catch (Throwable t) {
            throw new BuildException("Error finding the file - " + name + " - in the classpath.", t);
        }
    }
}
