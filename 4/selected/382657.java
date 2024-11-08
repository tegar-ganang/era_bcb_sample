package com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.MojoInfo;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.NsisActionExecutionException;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.FileItem;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.SetBase;

/**
 * Resolves a file
 *
 * @author <a href="mailto:bindul_bhowmik@mindtree.com">Bindul Bhowmik</a>
 * @version $Revision: 97 $ $Date: 2008-01-08 02:47:32 -0500 (Tue, 08 Jan 2008) $
 *
 * @plexus.component role="com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver.Resolver" role-hint="file"
 */
public class FileItemResolver implements Resolver {

    public void copyFiles(SetBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws NsisActionExecutionException {
        FileItem fileItem = (FileItem) setBase;
        File sourceFile = getFile(fileItem, mojoInfo);
        File destinationDir = new File(archiveTempDir, ((null == fileItem.getOutputDirectory()) ? "" : fileItem.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new NsisActionExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
            }
        }
        File destinationFile = new File(destinationDir, sourceFile.getName());
        try {
            FileUtils.copyFile(sourceFile, destinationFile);
        } catch (IOException e) {
            mojoInfo.getLog().error("Error copying " + sourceFile.getAbsolutePath(), e);
            throw new NsisActionExecutionException("Error copying " + sourceFile.getAbsolutePath(), e);
        }
    }

    public List getRelativeFilePath(SetBase setBase, MojoInfo mojoInfo) throws NsisActionExecutionException {
        File file = getFile((FileItem) setBase, mojoInfo);
        List relativePaths = new ArrayList(1);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        mojoInfo.getLog().debug("Adding: " + file.getAbsolutePath());
        relativePaths.add(relativeBase + file.getName());
        return relativePaths;
    }

    /**
	 * Gets the names of the selected files
	 */
    protected File getFile(FileItem fileItem, MojoInfo mojoInfo) {
        File projectDir = mojoInfo.getProject().getBasedir();
        String sourceName = fileItem.getSource();
        File file = new File(sourceName);
        if (!file.exists()) {
            file = new File(projectDir, sourceName);
        }
        return file;
    }
}
