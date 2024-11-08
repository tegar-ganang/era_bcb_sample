package org.wtc.eclipse.platform.util;

import com.windowtester.runtime.IUIContext;
import junit.framework.TestCase;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.wtc.eclipse.core.util.Timestamp;
import org.wtc.eclipse.platform.PlatformActivator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility to create a Zip file containing the contents of the given locations. If the
 * locations are directories, recurse those directories and add all of the files and
 * folders within those locations. Only add files that should be added according to the
 * given file filters
 */
public class ZipFileUtil {

    /**
     * Create a Zip file containing the contents of the given locations. If the locations
     * are directories, recurse those directories and add all of the files and folders
     * within those locations. Only add files that should be added according to the given
     * file filters
     *
     * <p>A TIMESTAMP WILL BE ADDED TO THE ZIP FILE NAME</p>
     */
    public void createZipCopy(IUIContext ui, String zipFileName, final File[] filesToZip, final FilenameFilter fileFilter) {
        createZipCopy(ui, zipFileName, filesToZip, fileFilter, new Timestamp());
    }

    /**
     * Create a Zip file containing the contents of the given locations. If the locations
     * are directories, recurse those directories and add all of the files and folders
     * within those locations. Only add files that should be added according to the given
     * file filters
     */
    public void createZipCopy(IUIContext ui, final String zipFileName, final File[] filesToZip, final FilenameFilter fileFilter, Timestamp timestamp) {
        TestCase.assertNotNull(ui);
        TestCase.assertNotNull(zipFileName);
        TestCase.assertFalse(zipFileName.trim().length() == 0);
        TestCase.assertNotNull(filesToZip);
        TestCase.assertNotNull(timestamp);
        String nameCopy = zipFileName;
        if (nameCopy.endsWith(".zip")) {
            nameCopy = nameCopy.substring(0, zipFileName.length() - 4);
        }
        nameCopy = nameCopy + "_" + timestamp.toString() + ".zip";
        final String finalZip = nameCopy;
        IWorkspaceRunnable noResourceChangedEventsRunner = new IWorkspaceRunnable() {

            public void run(IProgressMonitor runnerMonitor) throws CoreException {
                try {
                    Map<String, File> projectFiles = new HashMap<String, File>();
                    IPath basePath = new Path("/");
                    for (File nextLocation : filesToZip) {
                        projectFiles.putAll(getFilesToZip(nextLocation, basePath, fileFilter));
                    }
                    if (projectFiles.isEmpty()) {
                        PlatformActivator.logDebug("Zip file (" + zipFileName + ") not created because there were no files to zip");
                        return;
                    }
                    IPath resultsPath = PlatformActivator.getDefault().getResultsPath();
                    File copyRoot = resultsPath.toFile();
                    copyRoot.mkdirs();
                    IPath zipFilePath = resultsPath.append(new Path(finalZip));
                    String zipFileName = zipFilePath.toPortableString();
                    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
                    try {
                        out.setLevel(Deflater.DEFAULT_COMPRESSION);
                        for (String filePath : projectFiles.keySet()) {
                            File nextFile = projectFiles.get(filePath);
                            FileInputStream fin = new FileInputStream(nextFile);
                            try {
                                out.putNextEntry(new ZipEntry(filePath));
                                try {
                                    byte[] bin = new byte[4096];
                                    int bread = fin.read(bin, 0, 4096);
                                    while (bread != -1) {
                                        out.write(bin, 0, bread);
                                        bread = fin.read(bin, 0, 4096);
                                    }
                                } finally {
                                    out.closeEntry();
                                }
                            } finally {
                                fin.close();
                            }
                        }
                    } finally {
                        out.close();
                    }
                } catch (FileNotFoundException e) {
                    Status error = new Status(Status.ERROR, PlatformActivator.PLUGIN_ID, Status.ERROR, e.getLocalizedMessage(), e);
                    throw new CoreException(error);
                } catch (IOException e) {
                    Status error = new Status(Status.ERROR, PlatformActivator.PLUGIN_ID, Status.ERROR, e.getLocalizedMessage(), e);
                    throw new CoreException(error);
                }
            }
        };
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.run(noResourceChangedEventsRunner, workspace.getRoot(), IWorkspace.AVOID_UPDATE, new NullProgressMonitor());
        } catch (CoreException ce) {
            PlatformActivator.logException(ce);
        }
    }

    /**
     * Utility method to find the IFiles contained in the given resource and map the
     * relative path to the given base path to the java.io.File for that resource.
     */
    private Map<String, File> getFilesToZip(File resource, IPath basePath, FilenameFilter fileFilter) {
        Map<String, File> leafFiles = new HashMap<String, File>();
        IPath newRelativePath = basePath.append(new Path("/" + resource.getName()));
        if (resource.isFile()) {
            leafFiles.put(newRelativePath.toPortableString(), resource);
        } else {
            File[] children = resource.listFiles(fileFilter);
            for (File nextChild : children) {
                leafFiles.putAll(getFilesToZip(nextChild, newRelativePath, fileFilter));
            }
        }
        return leafFiles;
    }
}
