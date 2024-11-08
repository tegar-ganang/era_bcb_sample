package org.iqual.chaplin.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.iqual.chaplin.ProjectClassesTransformer;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Goal which transforms module classes for using in Chaplin ACT.
 * <p/>
 * 
 * @phase process-test-classes
 * @goal transform
 */
public class ChaplinMojo extends AbstractMojo {

    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * Directory relative to the output directory where compiled classes are stored.
     *
     * @parameter expression="classes"
     */
    private String classes;

    /**
     * @parameter default-value="${project.compileClasspathElements}"
     * @requiresDependencyResolution compile
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * List of package containing the transformed classes.
     *
     * @parameter
     */
    private List transformedPackages;

    /**
     * @parameter default-value="${project.groupId}"
     * @required
     * @readonly
     */
    private String groupId;

    private static final int BUFFER_SIZE = 10 * 1000;

    private File classesDirectory;

    public ChaplinMojo() {
    }

    ChaplinMojo(File outputDirectory, List transfPckgs) {
        this.classesDirectory = outputDirectory;
        transformedPackages = transfPckgs;
    }

    public void execute() throws MojoExecutionException {
        if (transformedPackages == null) {
            transformedPackages = Collections.singletonList(groupId);
        }
        classesDirectory = new File(outputDirectory, classes);
        printIntro();
        if (!classesDirectory.exists()) {
            throw new MojoExecutionException("Directory " + classesDirectory.getAbsolutePath() + " does not exist.");
        }
        DirectoriesInitializer directoriesInitializer = new DirectoriesInitializer().invoke();
        File outputClasses = directoriesInitializer.getOutputClasses();
        File sourceClasses = directoriesInitializer.getSourceClasses();
        final Set<String> packageSet = getTransformedPackages();
        ClassLoader origCL = Thread.currentThread().getContextClassLoader();
        try {
            URLClassLoader classLoader = getClasspathLoader(sourceClasses);
            ProjectClassesTransformer transformer = getTransformer(outputClasses, sourceClasses, packageSet);
            Thread.currentThread().setContextClassLoader(classLoader);
            int numberOfTransformed = transformer.transform();
            copyFile(outputClasses, null);
            System.out.println("Transformed " + numberOfTransformed + " output classes. Classes stored to " + classesDirectory.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Error transforming classes in " + classesDirectory.getAbsolutePath(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(origCL);
            try {
                deleteTree(outputClasses);
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        }
    }

    private URLClassLoader getClasspathLoader(File sourceClasses) throws MalformedURLException {
        List<URL> classpathUrls = new ArrayList<URL>();
        for (Object classpathElement : classpathElements) {
            URL url = new File(classpathElement.toString()).toURL();
            classpathUrls.add(url);
        }
        classpathUrls.add(sourceClasses.toURL());
        return new URLClassLoader(classpathUrls.toArray(new URL[classpathUrls.size()]));
    }

    private ProjectClassesTransformer getTransformer(File outputClasses, File sourceClasses, final Set<String> packageSet) {
        return ProjectClassesTransformer.create(sourceClasses, outputClasses, new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                for (String pckg : packageSet) {
                    if (pathname.getPath().startsWith(pckg)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private Set<String> getTransformedPackages() throws MojoExecutionException {
        final Set<String> packageSet = new HashSet<String>();
        if (transformedPackages == null || transformedPackages.isEmpty()) {
            throw new MojoExecutionException("No package to be transformed by Chaplin MOJO specified");
        }
        for (Object trPckg : transformedPackages) {
            String trPckgAsString = trPckg.toString().replace('.', '/');
            packageSet.add(classesDirectory.getPath() + File.separator + trPckgAsString);
        }
        return packageSet;
    }

    private void printIntro() {
        getLog().info("***�Chaplin Mojo ***");
        getLog().info("Output directory:" + classesDirectory);
        getLog().info("Transformed packages:" + transformedPackages);
        getLog().info("Classpath:" + classpathElements);
    }

    private void copyFile(File file, String contextPath) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                copyFile(f, contextPath == null ? "" : contextPath + file.getName() + File.separator);
            }
        } else if (file.isFile()) {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            File dest = new File(classesDirectory, contextPath + file.getName());
            OutputStream os = new BufferedOutputStream(new FileOutputStream(dest));
            copyStream(is, os);
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
        } finally {
            try {
                is.close();
            } finally {
                os.close();
            }
        }
    }

    private void deleteTree(File file) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteTree(f);
            }
        }
        file.delete();
    }

    private class DirectoriesInitializer {

        private File sourceClasses;

        private File outputClasses;

        public File getSourceClasses() {
            return sourceClasses;
        }

        public File getOutputClasses() {
            return outputClasses;
        }

        public DirectoriesInitializer invoke() throws MojoExecutionException {
            try {
                sourceClasses = classesDirectory;
                outputClasses = new File(classesDirectory, "../___chaplin");
                if (outputClasses.exists()) {
                    deleteTree(outputClasses);
                }
                outputClasses.mkdir();
            } catch (Exception e) {
                throw new MojoExecutionException("Error transforming classes in " + classesDirectory.getAbsolutePath(), e);
            }
            return this;
        }
    }
}
