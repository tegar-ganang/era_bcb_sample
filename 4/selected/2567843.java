package net.disy.legato.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;

@MojoGoal("concatenate")
@MojoPhase("generate-sources")
public class ConcatenateFilesMojo extends AbstractMojo {

    private MavenProject project;

    @MojoParameter(expression = "${project}", required = true, readonly = true)
    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    private String encoding;

    @MojoParameter(expression = "${project.build.sourceEncoding}", required = false, readonly = true)
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    private File[] fileSetDescriptors;

    @MojoParameter(required = true)
    public File[] getFileSetDescriptors() {
        return fileSetDescriptors;
    }

    public void setFileSetDescriptor(File[] filesDescriptors) {
        this.fileSetDescriptors = filesDescriptors;
    }

    private File sourceDirectory;

    @MojoParameter(required = true)
    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(File directory) {
        this.sourceDirectory = directory;
    }

    private File targetDirectory;

    @MojoParameter(required = true)
    public File getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    private boolean addCompileSourceRoot = false;

    @MojoParameter(defaultValue = "false")
    public boolean getAddCompileSourceRoot() {
        return addCompileSourceRoot;
    }

    public void setAddCompileSourceRoot(boolean sourceRoot) {
        this.addCompileSourceRoot = sourceRoot;
    }

    private boolean addResource = false;

    @MojoParameter(defaultValue = "false")
    public boolean getAddResource() {
        return addResource;
    }

    public void setAddResource(boolean addResource) {
        this.addResource = addResource;
    }

    private String targetFile;

    @MojoParameter(required = true)
    public String getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    private String delimiter = "\n";

    @MojoParameter(defaultValue = "\n")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Encoding is set to: " + getEncoding());
        final File[] fileSetDescriptors = getFileSetDescriptors();
        for (File fileSetDescriptor : fileSetDescriptors) {
            if (!fileSetDescriptor.isFile()) {
                throw new MojoExecutionException("Fileset descriptor [" + fileSetDescriptor.getAbsolutePath() + "] must point to a file.");
            }
        }
        final File sourceDirectory = getSourceDirectory();
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException("Source directory [" + sourceDirectory.getAbsolutePath() + "] must point to a directory.");
        }
        try {
            concatFiles();
            if (getAddCompileSourceRoot()) {
                getProject().addCompileSourceRoot(getTargetDirectory().getAbsolutePath());
            }
            if (getAddResource()) {
                final Resource resource = new Resource();
                resource.setDirectory(getTargetDirectory().getAbsolutePath());
                resource.addInclude(getTargetFile());
                getProject().addResource(resource);
            }
        } catch (IOException ioex) {
            throw new MojoExecutionException("Error concatenating the files.", ioex);
        }
    }

    public List<File> getFiles() throws IOException {
        final List<File> files = new LinkedList<File>();
        final File[] fileSetDescriptors = getFileSetDescriptors();
        if (fileSetDescriptors != null) {
            for (File fileSetDescriptor : fileSetDescriptors) {
                Reader reader = null;
                try {
                    reader = new FileReader(fileSetDescriptor);
                    @SuppressWarnings("unchecked") final List<String> readLines = IOUtils.readLines(reader);
                    for (String name : readLines) {
                        files.add(getFile(fileSetDescriptor, name));
                    }
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        }
        return files;
    }

    public File getFile(File fileSetDescriptor, String name) {
        if (name.startsWith("/")) {
            return new File(getSourceDirectory(), name.substring(1));
        } else {
            return new File(fileSetDescriptor.toURI().resolve(name));
        }
    }

    public void concatFiles() throws IOException {
        Writer writer = null;
        try {
            final File targetFile = new File(getTargetDirectory(), getTargetFile());
            targetFile.getParentFile().mkdirs();
            if (null != getEncoding()) {
                getLog().info("Writing aggregated file with encoding '" + getEncoding() + "'");
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), getEncoding()));
            } else {
                getLog().info("WARNING: writing aggregated file with system encoding");
                writer = new FileWriter(targetFile);
            }
            for (File file : getFiles()) {
                Reader reader = null;
                try {
                    if (null != getEncoding()) {
                        getLog().info("Reading file " + file.getCanonicalPath() + " with encoding  '" + getEncoding() + "'");
                        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), getEncoding()));
                    } else {
                        getLog().info("WARNING: Reading file " + file.getCanonicalPath() + " with system encoding");
                        reader = new FileReader(file);
                    }
                    IOUtils.copy(reader, writer);
                    final String delimiter = getDelimiter();
                    if (delimiter != null) {
                        writer.write(delimiter.toCharArray());
                    }
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
