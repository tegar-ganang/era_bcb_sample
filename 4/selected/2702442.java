package net.sf.buildbox.maven;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;

public class MvnUtil {

    public static final String[] DEFAULT_INCLUDES = { "**/**" };

    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    public static String[] getFilesToCopy(Resource resource) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(resource.getDirectory());
        if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
            scanner.setIncludes((String[]) resource.getIncludes().toArray(new String[resource.getIncludes().size()]));
        } else {
            scanner.setIncludes(DEFAULT_INCLUDES);
        }
        if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
            scanner.setExcludes((String[]) resource.getExcludes().toArray(new String[resource.getExcludes().size()]));
        }
        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static Map getFilterProperties(MavenProject project) throws MojoExecutionException {
        final Map filterProperties = new Properties();
        filterProperties.putAll(project.getProperties());
        filterProperties.putAll(System.getProperties());
        filterProperties.put("project.groupId", project.getGroupId());
        filterProperties.put("project.artifactId", project.getArtifactId());
        filterProperties.put("project.version", project.getVersion());
        return filterProperties;
    }

    /**
     * Copies webapp webResources from the specified directory.
     *
     * @param resource the resource to copy
     * @param filterProperties
     * @throws java.io.IOException            if an error occured while copying the resources
     * @throws org.apache.maven.plugin.MojoExecutionException if an error occured while retrieving the filter properties
     */
    public static void copyResources(Resource resource, File destination, Log logger, Map<String, String> filterProperties) throws IOException, MojoExecutionException {
        final File directory = new File(resource.getDirectory());
        if (!directory.exists()) {
            logger.debug("Ignoring resources under " + directory.getAbsolutePath());
            return;
        }
        logger.info("Copying resources from [" + directory + "] to [" + destination.getAbsolutePath() + "]");
        destination.mkdirs();
        if (!destination.exists()) {
            throw new IOException("Failed to create target directory:" + destination);
        }
        String[] fileNames = getFilesToCopy(resource);
        for (String targetFileName : fileNames) {
            if (resource.getTargetPath() != null) {
                targetFileName = resource.getTargetPath() + File.separator + targetFileName;
            }
            final File sourceFile = new File(directory, targetFileName);
            final File targetFile = new File(destination, targetFileName);
            if (resource.isFiltering()) {
                copyFilteredFile(logger, sourceFile, targetFile, filterProperties);
            } else {
                copyFile(sourceFile, targetFile, targetFileName, true, logger);
            }
        }
    }

    /**
     * Copy file from source to destination. The directories up to <code>destination</code>
     * will be created if they don't already exist. if the <code>onlyIfModified</code> flag
     * is <tt>false</tt>, <code>destination</code> will be overwritten if it already exists. If the
     * flag is <tt>true</tt> destination will be overwritten if it's not up to date.
     * <p/>
     *
     * @param source         an existing non-directory <code>File</code> to copy bytes from
     * @param destination    a non-directory <code>File</code> to write bytes to (possibly overwriting).
     * @param targetFilename the relative path of the file from the webapp root directory
     * @param onlyIfModified if true, copy the file only if the source has changed, always copy otherwise
     * @return true if the file has been copied/updated, false otherwise
     * @throws java.io.IOException if <code>source</code> does not exist, <code>destination</code> cannot
     *                     be written to, or an IO error occurs during copying
     */
    public static boolean copyFile(File source, File destination, String targetFilename, boolean onlyIfModified, Log logger) throws IOException {
        logger.debug("MvnUtil.copyFile: " + source + ", " + destination + ", " + targetFilename);
        if (onlyIfModified && destination.lastModified() >= source.lastModified()) {
            logger.debug(" * " + targetFilename + " is up to date (" + destination.lastModified() + " >= " + source.lastModified() + ")");
            return false;
        } else {
            FileUtils.copyFile(source.getCanonicalFile(), destination);
            destination.setLastModified(source.lastModified());
            logger.debug(" + " + targetFilename + " has been copied.");
            return true;
        }
    }

    private static boolean copyFilteredFile(Log logger, File sourceFile, File targetFile, Map<String, String> filterProperties) throws IOException, MojoExecutionException {
        Reader fileReader = null;
        Writer fileWriter = null;
        try {
            targetFile.getParentFile().mkdirs();
            fileReader = new BufferedReader(new FileReader(sourceFile));
            fileWriter = new FileWriter(targetFile);
            Reader reader = new InterpolationFilterReader(fileReader, filterProperties, "${", "}");
            IOUtil.copy(reader, fileWriter);
        } finally {
            IOUtil.close(fileReader);
            IOUtil.close(fileWriter);
        }
        logger.debug(" + " + targetFile.getAbsolutePath() + " has been filtercopied");
        return true;
    }
}
