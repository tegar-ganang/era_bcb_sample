package com.mindtree.techworks.infix.pluginscommon.mojo.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.infix.pluginscommon.mojo.InfixExecutionException;
import com.mindtree.techworks.infix.pluginscommon.mojo.MojoInfo;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.FileSet;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.LocationBase;

/**
 * Resolves a file set
 * 
 * @author Bindul Bhowmik
 * @version $Revision: 68 $ $Date: 2010-11-30 02:11:17 -0500 (Tue, 30 Nov 2010) $
 */
@Component(role = Resolver.class, hint = "file-set")
public class FileSetResolver implements Resolver {

    @Override
    public List<String> getRelativeFilePath(LocationBase setBase, MojoInfo mojoInfo) throws InfixExecutionException {
        String[] selectedFiles = getFiles((FileSet) setBase, mojoInfo);
        List<String> relativePaths = new ArrayList<String>(selectedFiles.length);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        for (int i = 0; i < selectedFiles.length; i++) {
            mojoInfo.getLog().debug("Adding: " + selectedFiles[i]);
            relativePaths.add(relativeBase + selectedFiles[i]);
        }
        return relativePaths;
    }

    @Override
    public void copyFiles(LocationBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws InfixExecutionException {
        FileSet fileSet = (FileSet) setBase;
        File projectDir = mojoInfo.getProject().getBasedir();
        String[] selectedFiles = getFiles(fileSet, mojoInfo);
        File basedir = new File(projectDir, ((null == fileSet.getDirectory()) ? "" : fileSet.getDirectory()));
        File destinationDir = new File(archiveTempDir, ((null == fileSet.getOutputDirectory()) ? "" : fileSet.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new InfixExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
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
                throw new InfixExecutionException("Error copying " + selectedFiles[i], e);
            }
        }
    }

    @Override
    public List<File> resolveFiles(LocationBase setBase, MojoInfo mojoInfo) {
        FileSet fileSet = (FileSet) setBase;
        File projectDir = mojoInfo.getProject().getBasedir();
        String[] selectedFiles = getFiles(fileSet, mojoInfo);
        File basedir = new File(projectDir, ((null == fileSet.getDirectory()) ? "" : fileSet.getDirectory()));
        List<File> resolvedFiles = new ArrayList<File>(selectedFiles.length);
        for (String selectedFile : selectedFiles) {
            resolvedFiles.add(new File(basedir, selectedFile));
        }
        return resolvedFiles;
    }

    /**
	 * Gets the names of the selected files
	 */
    protected String[] getFiles(FileSet fileSet, MojoInfo mojoInfo) {
        File projectDir = mojoInfo.getProject().getBasedir();
        File basedir = new File(projectDir, ((null == fileSet.getDirectory()) ? "" : fileSet.getDirectory()));
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(basedir);
        List<String> includes = fileSet.getIncludes();
        List<String> excludes = fileSet.getExcludes();
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
