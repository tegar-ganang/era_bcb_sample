package com.mindtree.techworks.infix.pluginscommon.mojo.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.infix.pluginscommon.mojo.InfixExecutionException;
import com.mindtree.techworks.infix.pluginscommon.mojo.MojoInfo;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.FileItem;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.LocationBase;

/**
 * Resolves a file
 * 
 * @author Bindul Bhowmik
 * @version $Revision: 68 $ $Date: 2010-11-30 02:11:17 -0500 (Tue, 30 Nov 2010) $
 */
@Component(role = Resolver.class, hint = "file")
public class FileItemResolver implements Resolver {

    @Override
    public List<String> getRelativeFilePath(LocationBase setBase, MojoInfo mojoInfo) throws InfixExecutionException {
        File file = getFile((FileItem) setBase, mojoInfo);
        List<String> relativePaths = new ArrayList<String>(1);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        mojoInfo.getLog().debug("Adding: " + file.getAbsolutePath());
        relativePaths.add(relativeBase + file.getName());
        return relativePaths;
    }

    @Override
    public void copyFiles(LocationBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws InfixExecutionException {
        FileItem fileItem = (FileItem) setBase;
        File sourceFile = getFile(fileItem, mojoInfo);
        File destinationDir = new File(archiveTempDir, ((null == fileItem.getOutputDirectory()) ? "" : fileItem.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new InfixExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
            }
        }
        File destinationFile = new File(destinationDir, sourceFile.getName());
        try {
            FileUtils.copyFile(sourceFile, destinationFile);
        } catch (IOException e) {
            mojoInfo.getLog().error("Error copying " + sourceFile.getAbsolutePath(), e);
            throw new InfixExecutionException("Error copying " + sourceFile.getAbsolutePath(), e);
        }
    }

    @Override
    public List<File> resolveFiles(LocationBase setBase, MojoInfo mojoInfo) {
        FileItem fileItem = (FileItem) setBase;
        return Collections.singletonList(getFile(fileItem, mojoInfo));
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
