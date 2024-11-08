package com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.MojoInfo;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.NsisActionExecutionException;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.FileSet;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.SetBase;

/**
 * Resolves a file set
 *
 * @author <a href="mailto:bindul_bhowmik@mindtree.com">Bindul Bhowmik</a>
 * @version $Revision: 97 $ $Date: 2008-01-08 02:47:32 -0500 (Tue, 08 Jan 2008) $
 *
 * @plexus.component role="com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver.Resolver" role-hint="file-set"
 */
public class FileSetResolver implements Resolver {

    public void copyFiles(SetBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws NsisActionExecutionException {
        FileSet fileSet = (FileSet) setBase;
        File projectDir = mojoInfo.getProject().getBasedir();
        String[] selectedFiles = getFiles(fileSet, mojoInfo);
        File basedir = new File(projectDir, ((null == fileSet.getDirectory()) ? "" : fileSet.getDirectory()));
        File destinationDir = new File(archiveTempDir, ((null == fileSet.getOutputDirectory()) ? "" : fileSet.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new NsisActionExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
            }
        }
        for (int i = 0; i < selectedFiles.length; i++) {
            File sourceFile = new File(basedir, selectedFiles[i]);
            File destinationFile = new File(destinationDir, selectedFiles[i]);
            mojoInfo.getLog().debug("Copying: " + selectedFiles[i] + " to " + destinationFile.getAbsolutePath());
            try {
                FileUtils.copyFile(sourceFile, destinationFile);
            } catch (IOException e) {
                mojoInfo.getLog().error("Error copying " + selectedFiles[i], e);
                throw new NsisActionExecutionException("Error copying " + selectedFiles[i], e);
            }
        }
    }

    public List getRelativeFilePath(SetBase setBase, MojoInfo mojoInfo) throws NsisActionExecutionException {
        String[] selectedFiles = getFiles((FileSet) setBase, mojoInfo);
        List relativePaths = new ArrayList(selectedFiles.length);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        for (int i = 0; i < selectedFiles.length; i++) {
            mojoInfo.getLog().debug("Adding: " + selectedFiles[i]);
            relativePaths.add(relativeBase + selectedFiles[i]);
        }
        return relativePaths;
    }

    /**
	 * Gets the names of the selected files
	 */
    protected String[] getFiles(FileSet fileSet, MojoInfo mojoInfo) {
        File projectDir = mojoInfo.getProject().getBasedir();
        File basedir = new File(projectDir, ((null == fileSet.getDirectory()) ? "" : fileSet.getDirectory()));
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(basedir);
        List includes = fileSet.getIncludes();
        List excludes = fileSet.getExcludes();
        if (null != includes && !includes.isEmpty()) {
            directoryScanner.setIncludes((String[]) includes.toArray(new String[includes.size()]));
        }
        if (null != excludes && !excludes.isEmpty()) {
            directoryScanner.setExcludes((String[]) excludes.toArray(new String[includes.size()]));
        }
        if (fileSet.isUseDefaultExcludes()) {
            directoryScanner.addDefaultExcludes();
        }
        directoryScanner.scan();
        return directoryScanner.getIncludedFiles();
    }
}
