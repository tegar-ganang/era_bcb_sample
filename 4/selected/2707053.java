package org.drftpd.tools.ant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.java.plugin.registry.Library;
import org.java.plugin.registry.PluginAttribute;
import org.java.plugin.registry.PluginDescriptor;

/**
 * @author djb61
 * @version $Id: LibCopyTask.java 1887 2008-03-02 16:10:37Z djb61 $
 */
public class LibCopyTask extends Task {

    public static final boolean isWin32 = System.getProperty("os.name").startsWith("Windows");

    private boolean _slavePlugin;

    private String _distDir;

    private String _installDir;

    private FileSet _slaveFiles;

    /**
	 * @see org.apache.tools.ant.Task#execute()
	 */
    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws BuildException {
        _distDir = getProject().getProperty("basedir");
        _installDir = getProject().getProperty("installdir");
        _slavePlugin = getProject().getProperty("slave.plugin").equalsIgnoreCase("true");
        _slaveFiles = (FileSet) getProject().getReference("slave.fileset");
        TreeSet<String> missingLibs = (TreeSet<String>) getProject().getReference("libs.missing");
        PluginDescriptor descriptor = (PluginDescriptor) getProject().getReference("plugin.descriptor");
        Collection<Library> jpfLibs = descriptor.getLibraries();
        for (Library lib : jpfLibs) {
            if (lib.getPath().equalsIgnoreCase(getProject().getName() + ".jar")) {
                continue;
            }
            File initial = new File(getProject().getProperty("pluginbase"), lib.getPath());
            try {
                File actual = initial.getCanonicalFile();
                if (actual.exists()) {
                    try {
                        File dest = copyFile(actual);
                        logCopy(dest);
                    } catch (IOException e2) {
                    }
                } else {
                    String relativePath = actual.getPath().substring(_distDir.length() + 1);
                    missingLibs.add(relativePath);
                }
            } catch (IOException e) {
                log("Error resolving path for library from plugin manifest: " + lib.getPath(), Project.MSG_ERR);
            }
        }
        PluginAttribute nativeDeps = descriptor.getAttribute("Native");
        if (nativeDeps != null) {
            for (PluginAttribute natDep : nativeDeps.getSubAttributes()) {
                String path = natDep.getValue();
                if (path != null) {
                    File actual = new File(_distDir, path);
                    if (actual.exists()) {
                        try {
                            File dest = copyFile(actual);
                            logCopy(dest);
                            PluginAttribute exec = natDep.getSubAttribute("Executable");
                            if (exec != null) {
                                String executable = exec.getValue();
                                if (executable != null) {
                                    if (executable.equalsIgnoreCase("true") && !isWin32) {
                                        String[] cmdArray = { "chmod", "755", dest.getAbsolutePath() };
                                        try {
                                            Process p = Runtime.getRuntime().exec(cmdArray);
                                            p.waitFor();
                                            if (p.exitValue() != 0) {
                                                log("Error chmodding file: " + dest.getAbsolutePath(), Project.MSG_ERR);
                                            }
                                        } catch (IOException e) {
                                            log("Error chmodding file: " + dest.getAbsolutePath(), Project.MSG_ERR);
                                        } catch (InterruptedException e) {
                                            log("Chmod process was interrupted on file: " + dest.getAbsolutePath(), Project.MSG_ERR);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                        }
                    } else {
                        String relativePath = actual.getPath().substring(_distDir.length() + 1);
                        missingLibs.add(relativePath);
                    }
                }
            }
        }
    }

    private File copyFile(File currFile) throws IOException {
        String relativePath = currFile.getPath().substring(_distDir.length() + 1);
        File targetFile = new File(_installDir, relativePath);
        if (targetFile.exists()) {
            log(targetFile.getPath() + " already exists, skipping libcopy", Project.MSG_INFO);
            return targetFile;
        } else {
            if (!targetFile.getParentFile().exists()) {
                if (!targetFile.getParentFile().mkdirs()) {
                    log("Unable to create target dir tree for " + targetFile.getPath(), Project.MSG_ERR);
                    throw new IOException();
                }
            }
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(currFile);
        } catch (FileNotFoundException e) {
            log("Library from plugin manifest appears to have been deleted: " + currFile.getPath(), Project.MSG_ERR);
            throw new IOException();
        }
        try {
            fos = new FileOutputStream(targetFile);
        } catch (FileNotFoundException e) {
            log("Unable to create target file to write to: " + targetFile.getPath(), Project.MSG_ERR);
            throw new IOException();
        }
        BufferedInputStream bis = new BufferedInputStream(fis);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int read = 0;
        byte[] buff = new byte[65536];
        boolean success = true;
        while (read != -1 && success) {
            try {
                read = bis.read(buff, 0, 65536);
            } catch (IOException e) {
                log("Read error whilst reading from: " + currFile.getPath(), Project.MSG_ERR);
                success = false;
            }
            if (read != -1 && success) {
                try {
                    bos.write(buff, 0, read);
                } catch (IOException e) {
                    log("Write error whilst writing to: " + targetFile.getPath(), Project.MSG_ERR);
                    success = false;
                }
            }
        }
        try {
            bis.close();
        } catch (IOException e) {
        }
        try {
            bos.close();
        } catch (IOException e) {
        }
        try {
            fis.close();
        } catch (IOException e) {
        }
        try {
            fos.close();
        } catch (IOException e) {
        }
        if (!success) {
            throw new IOException();
        }
        return targetFile;
    }

    private void logCopy(File copiedFile) {
        if (_slavePlugin) {
            String relativeInstallPath = (copiedFile.getPath()).substring(_installDir.length() + 1);
            _slaveFiles.appendIncludes(new String[] { relativeInstallPath });
        }
    }
}
