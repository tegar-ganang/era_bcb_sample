package com.amazonaws.eclipse.wtp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleResource;
import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Responsible for creating archives based on different inputs.
 */
public class ArchiveFileFactory {

    /**
	 * Creates an archive for the specified module from the specified collection
	 * of module resources. The contents of each specified module resource is
	 * included in the returned archive file, associated with the path relative
	 * to the root of the specified module.
	 * 
	 * @param archiveType
	 *            The type of archive file to create. Currently only zip and jar
	 *            are supported.
	 * @param module
	 *            The module containing the specified resources.
	 * @param resources
	 *            The resources to include in the zip archive.
	 * @param monitor
	 *            An optional progress monitor if progress reporting is desired.
	 * @return The file containing the zip archive of the specified module
	 *         resources.
	 * 
	 * @throws IOException
	 *             If any problems were encountered while creating the archive.
	 */
    public File createArchiveFile(String archiveType, IModule module, List<IModuleResource> resources, IProgressMonitor monitor) throws IOException {
        String suffix = ".zip";
        if (archiveType.equalsIgnoreCase("zip")) {
            suffix = ".zip";
        } else if (archiveType.equalsIgnoreCase("jar")) {
            suffix = ".jar";
        }
        if (monitor == null) monitor = new NullProgressMonitor();
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tempDir, module.getName() + suffix);
        FileOutputStream fileOutput = new FileOutputStream(tempFile);
        ZipOutputStream zipOutput = new ZipOutputStream(fileOutput);
        try {
            writeResourcesToZipFile(module, resources.toArray(new IModuleResource[0]), zipOutput);
        } finally {
            zipOutput.close();
        }
        return tempFile;
    }

    /**
	 * Creates a zip file from an array of files. The contents of each file is
	 * included in the returned zip file as the basename of the file. The
	 * returned zip file has a flat namespace, i.e. no directories are included
	 * in the zip file.
	 * 
	 * @param files
	 *            The files to include in the returned zip file.
	 * @param monitor
	 *            An optional progress monitor if progress reporting is desired.
	 * 
	 * @return A zip file containing all of the specified files in a flat
	 *         namespace (i.e. no subdirectories).
	 * 
	 * @throws IOException
	 *             If any problems are encountered while creating the zip file.
	 */
    public File createZipFile(File[] files, IProgressMonitor monitor) throws IOException {
        return this.createZipFile(files, new Path(""), monitor);
    }

    /**
	 * Creates a zip file from an array of files. The contents of each file is
	 * included in the returned zip file as the basename of the file. The
	 * rootFilePath parameter controls what directory the files are placed in
	 * within the archive. If you just want them at the root of the archive, you
	 * should use the other form of createZipFile that doesn't require the
	 * rootFilePath argument.
	 * 
	 * @param files
	 *            The files to include in the returned zip file.
	 * @param rootFilePath
	 *            The root file path at which the specified files should be
	 *            positioned in the returned archive file.
	 * @param monitor
	 *            An optional progress monitor if progress reporting is desired.
	 * 
	 * @return A zip file containing all of the specified files, all listed
	 *         under the path within the zip file that the caller specified.
	 * 
	 * @throws IOException
	 *             If any problems are encountered while creating the zip file.
	 */
    public File createZipFile(File[] files, IPath rootFilePath, IProgressMonitor monitor) throws IOException {
        if (monitor == null) monitor = new NullProgressMonitor();
        File tempFile = File.createTempFile("ec2archive", ".zip");
        FileOutputStream fileOutput = new FileOutputStream(tempFile);
        ZipOutputStream zipOutput = new ZipOutputStream(fileOutput);
        try {
            for (File file : files) {
                IPath zipFilePath = new Path(file.getName());
                if (!rootFilePath.isEmpty()) {
                    zipFilePath = rootFilePath.append(zipFilePath);
                }
                writeFileToZipFile(file, zipFilePath.toString(), zipOutput);
            }
        } finally {
            zipOutput.close();
        }
        return tempFile;
    }

    /**
	 * Writes the specified module resources from the specified module to the
	 * specified zip file output stream.
	 * 
	 * @param module
	 *            The module containing the specified resources to write to the
	 *            zip output stream.
	 * @param moduleResources
	 *            The module resources to write to the zip output stream.
	 * @param zipOutput
	 *            The zip output stream to write the module resources to.
	 * 
	 * @throws IOException
	 *             If any problems were encountered writing to the zip output
	 *             stream.
	 */
    private void writeResourcesToZipFile(IModule module, IModuleResource[] moduleResources, ZipOutputStream zipOutput) throws IOException {
        writeResourcesToZipFile(module, moduleResources, zipOutput, new Path(""));
    }

    /**
	 * Writes the specified module resources from the specified module to the
	 * specified zip file output stream and allows the caller to specify a
	 * moduleRootPath to shift all the resources to a specified path within the
	 * archive.
	 * 
	 * @param module
	 *            The module containing the specified resources to write to the
	 *            zip output stream.
	 * @param moduleResources
	 *            The module resources to write to the zip output stream.
	 * @param zipOutput
	 *            The zip output stream to write the module resources to.
	 * @param moduleRootPath
	 *            The path within the zip file where the module resource should
	 *            be rooted.
	 * @throws IOException
	 *             If any problems were encountered writing to the zip output
	 *             stream.
	 */
    private void writeResourcesToZipFile(IModule module, IModuleResource[] moduleResources, ZipOutputStream zipOutput, IPath moduleRootPath) throws IOException {
        if (moduleResources == null) return;
        for (IModuleResource moduleResource : moduleResources) {
            if (!(moduleResource instanceof IModuleFile)) {
                continue;
            }
            if (moduleRootPath.isEmpty() && moduleRootPath.equals(new Path("/"))) {
                System.out.println("crap!");
            }
            String moduleResourcePath = new String();
            if (!moduleRootPath.isEmpty()) {
                moduleResourcePath += moduleRootPath.toString() + "/";
            }
            IPath moduleRelativePath = moduleResource.getModuleRelativePath();
            if (!moduleRelativePath.isEmpty()) {
                moduleResourcePath += moduleRelativePath.toString() + "/";
            }
            moduleResourcePath += moduleResource.getName();
            IFile ifile = (IFile) moduleResource.getAdapter(IFile.class);
            File file = (File) moduleResource.getAdapter(File.class);
            if (file == null) {
                file = ifile.getLocation().toFile();
            }
            if (file != null) {
                writeFileToZipFile(file, moduleResourcePath, zipOutput);
            } else {
                Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID, "Unable to resolve module resouce to a file. " + "resource: " + moduleResource.getModuleRelativePath() + "/" + moduleResource.getName() + ", " + "class: " + moduleResource.getClass());
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

    /**
	 * Writes the specified file as the specified path in the zip output stream.
	 * 
	 * @param file
	 *            The file whose contents should be written to the zip output
	 *            stream.
	 * @param zipFilePath
	 *            The file path the contents of the specified file should be
	 *            associated with in the zip output stream.
	 * @param zipOutput
	 *            The zip output stream the file contents should be written to.
	 * 
	 * @throws IOException
	 *             If there were any problems writting to the specified zip
	 *             output stream.
	 */
    private void writeFileToZipFile(File file, String zipFilePath, ZipOutputStream zipOutput) throws IOException {
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipOutput.putNextEntry(zipEntry);
        BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(file));
        try {
            byte[] buffer = new byte[1024];
            while (bufferedInput.available() > 0) {
                int read = bufferedInput.read(buffer);
                zipOutput.write(buffer, 0, read);
            }
        } finally {
            try {
                bufferedInput.close();
            } catch (Exception e) {
            }
        }
        zipOutput.closeEntry();
    }
}
