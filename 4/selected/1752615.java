package org.servebox.flex.mojo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.servebox.flex.mojo.Module;
import flex2.tools.VersionInfo;

/**
 * Utility class for mojos, allowing to manipulate resources stored in the plugin JAR
 * file, providing output artifacts, copy streams, etc.
 * 
 * @author J.F.Mathiot
 */
public class FlexMojoUtils {

    private static ClassLoader classLoader;

    public static void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    public static File getOutputArtifactFile(File baseDirectory, MavenProject project, String classifier, String extension) {
        if (classifier != null && !classifier.equals("")) {
            return new File(baseDirectory, project.getArtifactId() + "-" + classifier + "-" + project.getVersion() + "." + extension);
        } else {
            return new File(baseDirectory, project.getArtifactId() + "-" + project.getVersion() + "." + extension);
        }
    }

    public static File getOutputArtifactFile(File baseDirectory, MavenProject project, String classifier, String suffix, String extension) {
        if (classifier != null && !classifier.equals("")) {
            return new File(baseDirectory, project.getArtifactId() + "-" + suffix + "-" + classifier + "-" + project.getVersion() + "." + extension);
        } else {
            return new File(baseDirectory, project.getArtifactId() + "-" + suffix + "-" + project.getVersion() + "." + extension);
        }
    }

    public static File getResourcesDirectory(File baseDirectory) {
        File resourcesDirectory = new File(baseDirectory, "compiler-resources");
        if (!resourcesDirectory.exists()) {
            resourcesDirectory.mkdirs();
        }
        return resourcesDirectory;
    }

    public static File getDefaultFlexConfigFile(File baseDirectory) throws IOException {
        return copyRequiredResource(baseDirectory, "default-flex-config.xml", "flex-config.xml");
    }

    public static void copyRequiredResources(File baseDirectory) throws IOException {
        copyRequiredResource(baseDirectory, "winFonts.ser", "winFonts.ser");
        copyRequiredResource(baseDirectory, "macFonts.ser", "macFonts.ser");
        copyRequiredResource(baseDirectory, "localFonts.ser", "localFonts.ser");
    }

    public static File copyRequiredResource(File baseDirectory, String originalName, String newName) throws IOException {
        File resourceFile = new File(getResourcesDirectory(baseDirectory), newName);
        if (resourceFile.exists()) {
            resourceFile.delete();
        }
        resourceFile.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(resourceFile);
        InputStream inputStream = classLoader.getResourceAsStream("flex-config/" + originalName);
        streamToStream(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return resourceFile;
    }

    public static File copyToResourcesDirectory(File baseDirectory, File originalFile, String newName, ClassLoader cLoader) throws IOException {
        File resourceFile = new File(getResourcesDirectory(baseDirectory), newName);
        if (resourceFile.exists()) {
            resourceFile.delete();
        }
        resourceFile.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(resourceFile);
        InputStream inputStream = new FileInputStream(originalFile);
        streamToStream(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        return resourceFile;
    }

    public static File getCompilerLocalFontSnapshot(File baseDirectory) {
        String fontSnapshotFileName;
        if (OSUtil.isOSMac()) {
            fontSnapshotFileName = "macFonts.ser";
        } else if (OSUtil.isOSWindows()) {
            fontSnapshotFileName = "winFonts.ser";
        } else {
            fontSnapshotFileName = "localFonts.ser";
        }
        return new File(getResourcesDirectory(baseDirectory), fontSnapshotFileName);
    }

    private static void streamToStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] streamData = new byte[2048];
        int nbRead = 0;
        while ((nbRead = inputStream.read(streamData)) > 0) {
            outputStream.write(streamData, 0, nbRead);
            outputStream.flush();
        }
    }

    public static String getTemplateContent(String resourceName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream inputStream = classLoader.getResourceAsStream("templates/" + resourceName);
        streamToStream(inputStream, outputStream);
        String result = outputStream.toString();
        inputStream.close();
        outputStream.close();
        return result;
    }

    public static Collection<String> filesToFullyQualifiedClassNames(File baseDirectory, Collection<File> files) {
        Collection<String> classes = new ArrayList<String>();
        for (File classFile : files) {
            classes.add(fileToFullyQualifiedClassName(baseDirectory, classFile));
        }
        return classes;
    }

    public static String fileToFullyQualifiedClassName(File baseDirectory, File classFile) {
        String regex = File.separator.equals("\\") ? "\\\\" : File.separator;
        String[] baseDirectoryPathChunks = baseDirectory.getAbsolutePath().split(regex);
        String[] classFilePathChunks = classFile.getAbsolutePath().split(regex);
        String className = "";
        for (int i = baseDirectoryPathChunks.length; i < classFilePathChunks.length; i++) {
            className += (className.length() > 0 ? "." : "") + classFilePathChunks[i];
        }
        if (className.endsWith(".as")) {
            return className.substring(0, className.length() - 3);
        } else if (className.endsWith(".mxml")) {
            return className.substring(0, className.length() - 5);
        }
        return null;
    }

    public static String directoryToFullyQualifiedPackageName(File baseDirectory, File directory) {
        String regex = File.separator.equals("\\") ? "\\\\" : File.separator;
        String[] baseDirectoryPathChunks = baseDirectory.getAbsolutePath().split(regex);
        String[] directoryPathChunks = directory.getAbsolutePath().split(regex);
        String packageName = "";
        for (int i = baseDirectoryPathChunks.length; i < directoryPathChunks.length; i++) {
            packageName += (packageName.length() > 0 ? "." : "") + directoryPathChunks[i];
        }
        return packageName;
    }

    public static String replaceTokens(String template, Map<String, Object> params) throws ParseErrorException, ResourceNotFoundException, MethodInvocationException, IOException {
        VelocityContext context = new VelocityContext(params);
        StringWriter writer = new StringWriter();
        StringReader reader = new StringReader(template);
        boolean result = Velocity.evaluate(context, writer, "rule-" + System.currentTimeMillis(), reader);
        String strResult = null;
        if (result) {
            strResult = writer.getBuffer().toString();
        }
        return strResult;
    }

    public static File checkDirectoryAndCreate(File directory, String defaultPath) {
        if (directory == null) {
            directory = new File(defaultPath);
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public static File extractTemplate(File baseDirectory, File templateArchive) throws IOException, ArchiverException {
        ZipUnArchiver unarchiver = new ZipUnArchiver();
        unarchiver.enableLogging(new ConsoleLogger(ConsoleLogger.LEVEL_ERROR, "consoleLogger"));
        unarchiver.setSourceFile(templateArchive);
        File destinationDirectory = new File(getResourcesDirectory(baseDirectory), "asdoc-template");
        destinationDirectory.mkdirs();
        unarchiver.setDestDirectory(destinationDirectory);
        unarchiver.extract();
        return destinationDirectory;
    }

    /**
     * Return a path to file that is relative to a given base file.
     */
    public static String relativePath(File file, File baseFile) {
        char[] filePath = file.getAbsolutePath().toCharArray();
        char[] relativePath = baseFile.getAbsolutePath().toCharArray();
        StringBuffer result = new StringBuffer();
        int i;
        for (i = 0; i < filePath.length && i < relativePath.length; i++) {
            if (filePath[i] != relativePath[i]) {
                break;
            }
        }
        if (i < relativePath.length && i < filePath.length || relativePath.length < filePath.length && filePath[i] != File.separatorChar) {
            while (i > 0 && relativePath[i - 1] != File.separatorChar) {
                i--;
            }
        }
        if (i != 0) {
            StringTokenizer tokens = new StringTokenizer(baseFile.getAbsolutePath().substring(i), File.separator);
            while (tokens.hasMoreTokens()) {
                tokens.nextToken();
                result.append("../");
            }
        }
        StringTokenizer tokens = new StringTokenizer(file.getAbsolutePath().substring(i), File.separator);
        if (tokens.hasMoreTokens()) {
            result.append(tokens.nextToken());
        } else if (result.length() > 0 && result.charAt(result.length() - 1) == '/') {
            result.deleteCharAt(result.length() - 1);
        }
        while (tokens.hasMoreTokens()) {
            result.append("/" + tokens.nextToken());
        }
        String res = result.toString();
        return res.equals("") ? "." : res;
    }

    public static String getSWCExtension() {
        return "swc";
    }

    public static String getSWCPackagingType() {
        return "swc";
    }

    public static String getSWFExtension() {
        return "swf";
    }

    public static String getSWFPackagingType() {
        return "swf";
    }

    public static String getRSLExtension(boolean signed) {
        if (signed) {
            return "swz";
        }
        return "swf";
    }

    public static File getModuleOutputArtifactFile(File outputDirectory, Module module, String extension) {
        File moduleOutput = module.getDestinationDirectory();
        if (moduleOutput == null) {
            moduleOutput = new File(outputDirectory, module.getModuleFile().getParentFile().getName());
            module.setDestinationDirectory(moduleOutput);
        }
        if (!moduleOutput.exists()) {
            moduleOutput.mkdirs();
        }
        return new File(moduleOutput, module.getName() + "." + extension);
    }

    /**
     * Returns true if Flex version is 3.5.0
     * 
     * @return
     */
    public static boolean isFlex3() {
        return VersionInfo.FLEX_MAJOR_VERSION.equals("3");
    }

    /**
     * Returns true if Flex version is 4.0.0
     * 
     * @return
     */
    public static boolean isFlex4() {
        return VersionInfo.FLEX_MAJOR_VERSION.equals("4");
    }

    public static boolean isFlex45() {
        return VersionInfo.FLEX_MAJOR_VERSION.equals("4") && VersionInfo.FLEX_MINOR_VERSION.equals("5");
    }
}
