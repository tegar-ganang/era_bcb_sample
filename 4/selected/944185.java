package net.sf.dub.application.anttask;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant-Task for dub
 * 
 * @author  dgm
 * @version $Revision: 1.1 $
 */
public class DubTask extends Task {

    private File sqlVersionDir;

    private File configurationFile;

    private File outputFile;

    private class SqlVersionDirFilter implements FileFilter {

        public boolean accept(File pathname) {
            return pathname.isDirectory() && pathname.getName().matches("^[0-9]+[\\.][0-9]+[\\.][0-9]+$");
        }
    }

    private class SqlFileFilter implements FileFilter {

        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().endsWith(".sql");
        }
    }

    public DubTask() {
        super();
    }

    public void setSqlVersionDir(String sqlVersionDir) {
        this.sqlVersionDir = new File(sqlVersionDir);
        if (!this.sqlVersionDir.isDirectory()) {
            throw new BuildException("No valid SQL Version directory specified!");
        }
    }

    public void setConfigurationFile(String configurationFile) {
        this.configurationFile = new File(configurationFile);
        if (!this.configurationFile.isFile()) {
            throw new BuildException("No valid configuration file specified!");
        }
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = new File(outputFile);
        if (this.outputFile.isDirectory()) {
            throw new BuildException("No valid Outputfile specified!");
        }
    }

    public void execute() throws BuildException {
        if (sqlVersionDir == null) {
            throw new BuildException("No valid SQL Version directory specified!");
        }
        if (configurationFile == null) {
            throw new BuildException("No valid configuration file specified!");
        }
        if (outputFile == null) {
            throw new BuildException("No valid Outputfile specified!");
        }
        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new FileOutputStream(outputFile));
            addJarEntry(jos, configurationFile.getName(), new FileInputStream(configurationFile));
            File[] dirs = sqlVersionDir.listFiles(new SqlVersionDirFilter());
            for (int i = 0; i < dirs.length; i++) {
                File[] files = dirs[i].listFiles(new SqlFileFilter());
                for (int j = 0; j < files.length; j++) {
                    addJarEntry(jos, dirs[i].getName() + "/" + files[j].getName(), new FileInputStream(files[j]));
                }
            }
            jos.close();
        } catch (FileNotFoundException fnfe) {
            throw new BuildException("File not found", fnfe);
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Add a jar entry to the deployment archive */
    public void addJarEntry(JarOutputStream outputStream, String entryName, InputStream inputStream) throws IOException {
        outputStream.putNextEntry(new JarEntry(entryName));
        copyStream(outputStream, inputStream);
    }

    /** Copies the input stream to the output stream */
    private void copyStream(OutputStream outputStream, InputStream inputStream) throws IOException {
        byte[] bytes = new byte[4096];
        int read = inputStream.read(bytes, 0, 4096);
        while (read > 0) {
            outputStream.write(bytes, 0, read);
            read = inputStream.read(bytes, 0, 4096);
        }
    }
}
