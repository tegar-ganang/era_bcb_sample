package edu.hawaii.ics.csdl.jupiter.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import edu.hawaii.ics.csdl.jupiter.ReviewPlugin;

/**
 * Provides project mock up used in the plug-in junit test.
 * 
 * @author Takuya Yamashita
 * @version $Id: ProjectMockup.java 170 2009-10-08 08:38:34Z jsakuda $
 */
public class ProjectMockup {

    private IProject project;

    /**
   * Instantiates the project mock up.
   * @param projectName the project name.
   * @param outputpath the output path.
   * @throws CoreException if problems occur.
   */
    public ProjectMockup(String projectName, String outputpath) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        project = root.getProject(projectName);
        project.create(null);
        project.open(null);
    }

    /**
   * Gets the project.
   * @return the project.
   */
    public IProject getProject() {
        return project;
    }

    /**
   * Deletes all project.
   * 
   * @exception CoreException if this method fails. Reasons include:
   * <ul>
   * <li> This project could not be deleted.</li>
   * <li> This project's contents could not be deleted.</li>
   * <li> Resource changes are disallowed during certain types of resource change 
   *       event notification. See <code>IResourceChangeEvent</code> for more details.</li>
   * </ul>
   */
    public void dispose() throws CoreException {
        project.delete(true, true, null);
    }

    /**
   * creates the class output folder.
   * @param outputPath the class output path.
   * @return the class output folder.
   * @exception CoreException if this method fails. Reasons include:
   * <ul>
   * <li> This resource already exists in the workspace.</li>
   * <li> The workspace contains a resource of a different type 
   *      at the same path as this resource.</li>
   * <li> The parent of this resource does not exist.</li>
   * <li> The parent of this resource is a project that is not open.</li>
   * <li> The parent contains a resource of a different type 
   *      at the same path as this resource.</li>
   * <li> The name of this resource is not valid (according to 
   *    <code>IWorkspace.validateName</code>).</li>
   * <li> The corresponding location in the local file system is occupied
   *    by a file (as opposed to a directory).</li>
   * <li> The corresponding location in the local file system is occupied
   *    by a folder and <code>force </code> is <code>false</code>.</li>
   * <li> Resource changes are disallowed during certain types of resource change 
   *       event notification.  See <code>IResourceChangeEvent</code> for more details.</li>
   * </ul>
   * @see IFolder#create(int,boolean,IProgressMonitor)
   */
    private IFolder createClassOutputFolder(String outputPath) throws CoreException {
        IFolder binFolder = project.getFolder(outputPath);
        binFolder.create(false, true, null);
        return binFolder;
    }

    /**
   * Finds the file path in the plug-in.
   * @param plugin the plug-in name.
   * @param file the file string in the plug-in
   * @return The file path in the plug-in.
   * @throws MalformedURLException  if no protocol is specified, or an unknown protocol is found.
   * @throws IOException if unable to resolve URL
   */
    private Path findFileInPlugin(String plugin, String file) throws MalformedURLException, IOException {
        URL pluginURL = ReviewPlugin.getInstance().getBundle().getEntry("/");
        URL jarURL = new URL(pluginURL, file);
        URL localJarURL = FileLocator.toFileURL(jarURL);
        return new Path(localJarURL.getPath());
    }

    /**
   * Gets file instance from the given project and the relative path to the review file to be
   * created. Returns <code>null</code> if problems occur on the new file creation.
   *
   * @param relativePath the relative path to the review file to be created.
   *
   * @return the file instance which is guaranteed to exist unless <code>null</code> is returned.
   *
   * @throws CoreException thrown if problems occur.
   * @throws IOException thrown if problems occur.
   */
    public IFile createIFile(String relativePath) throws IOException, CoreException {
        if (project == null) {
            return null;
        }
        IFile file = project.getFile(relativePath);
        File reviewFile = file.getLocation().toFile();
        if (file.exists()) {
            if (!reviewFile.exists()) {
                reviewFile.getParentFile().mkdirs();
                reviewFile.createNewFile();
                file.refreshLocal(IResource.DEPTH_ONE, null);
                file.setContents(new FileInputStream(reviewFile), true, false, null);
            }
        } else {
            if (!reviewFile.exists()) {
                reviewFile.getParentFile().mkdirs();
                reviewFile.createNewFile();
                file.refreshLocal(IResource.DEPTH_ONE, null);
                file.setContents(new FileInputStream(reviewFile), true, false, null);
            } else {
                file.create(new FileInputStream(reviewFile), true, null);
            }
        }
        return file;
    }

    /**
   * Copy the source relative path file in this project to the destination path file in the
   * test project.
   * @param sourceRelativePathFile the source relative path file in this project
   * @param destinationRelativePathFile the destination path file in the test project.
   * @return <code>true</code> if the copy is success. <code>false</code> otherwise.
   */
    public boolean copy(String sourceRelativePathFile, String destinationRelativePathFile) {
        URL pluginUrl = ReviewPlugin.getInstance().getBundle().getEntry("/");
        try {
            IFile distinationIFile = this.createIFile(destinationRelativePathFile);
            URL xmlUrl = FileLocator.toFileURL(new URL(pluginUrl, sourceRelativePathFile));
            File sourceXmlFile = new File(xmlUrl.getFile());
            copy(sourceXmlFile, distinationIFile.getLocation().toFile());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
   * Copy the source file to the destination file.
   * @param srouceFile the source <code>File</code>.
   * @param destinationFile the destination <code>File</code>.
   * @throws IOException if problems occur.
   */
    private void copy(File srouceFile, File destinationFile) throws IOException {
        FileChannel sourceChannel = new FileInputStream(srouceFile).getChannel();
        FileChannel destinationChannel = new FileOutputStream(destinationFile).getChannel();
        destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        destinationChannel.close();
    }
}
