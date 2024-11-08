package com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.MojoInfo;
import com.mindtree.techworks.insight.releng.mvn.nsis.actions.NsisActionExecutionException;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.DependencySet;
import com.mindtree.techworks.insight.releng.mvn.nsis.model.SetBase;

/**
 * Resolves the dependencies
 *
 * @author <a href="mailto:bindul_bhowmik@mindtree.com">Bindul Bhowmik</a>
 * @version $Revision: 97 $ $Date: 2008-01-08 02:47:32 -0500 (Tue, 08 Jan 2008) $
 *
 * @plexus.component role="com.mindtree.techworks.insight.releng.mvn.nsis.actions.resolver.Resolver" role-hint="dependency"
 */
public class DependencyResolver implements Resolver {

    public void copyFiles(SetBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws NsisActionExecutionException {
        DependencySet dependencySet = (DependencySet) setBase;
        File[] selectedFiles = resolveDependencies(dependencySet, mojoInfo);
        File destinationDir = new File(archiveTempDir, ((null == dependencySet.getOutputDirectory()) ? "" : dependencySet.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new NsisActionExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
            }
        }
        for (int i = 0; i < selectedFiles.length; i++) {
            File destinationFile = new File(destinationDir, selectedFiles[i].getName());
            mojoInfo.getLog().debug("Copying: " + selectedFiles[i] + " to " + destinationFile.getAbsolutePath());
            try {
                FileUtils.copyFile(selectedFiles[i], destinationFile);
            } catch (IOException e) {
                mojoInfo.getLog().error("Error copying " + selectedFiles[i], e);
                throw new NsisActionExecutionException("Error copying " + selectedFiles[i], e);
            }
        }
    }

    public List getRelativeFilePath(SetBase setBase, MojoInfo mojoInfo) throws NsisActionExecutionException {
        File[] selectedFiles = resolveDependencies((DependencySet) setBase, mojoInfo);
        List relativePaths = new ArrayList(selectedFiles.length);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        for (int i = 0; i < selectedFiles.length; i++) {
            mojoInfo.getLog().debug("Adding: " + selectedFiles[i].getAbsolutePath());
            relativePaths.add(relativeBase + selectedFiles[i].getName());
        }
        return relativePaths;
    }

    /**
	 * Resolves the dependencies
	 * @return 
	 */
    protected File[] resolveDependencies(DependencySet dependencySet, MojoInfo mojoInfo) {
        MavenProject project = mojoInfo.getProject();
        Set artifactSet = project.getArtifacts();
        ArtifactFilter scopeArtifactFilter = new ScopeArtifactFilter(dependencySet.getScope());
        ArtifactFilter includeArtifactFilter = null;
        ArtifactFilter excludeArtifactFilter = null;
        if (null != dependencySet.getIncludes()) {
            includeArtifactFilter = new IncludesArtifactFilter(dependencySet.getIncludes());
        }
        if (null != dependencySet.getExcludes()) {
            excludeArtifactFilter = new ExcludesArtifactFilter(dependencySet.getExcludes());
        }
        List dependentArtifacts = new ArrayList(artifactSet.size());
        for (Iterator artifacts = artifactSet.iterator(); artifacts.hasNext(); ) {
            Artifact artifact = (Artifact) artifacts.next();
            if (includeArtifact(artifact, scopeArtifactFilter, includeArtifactFilter, excludeArtifactFilter)) {
                mojoInfo.getLog().debug("Including dependency: " + artifact.getId());
                File outputFile = artifact.getFile();
                mojoInfo.getLog().debug("Inluding dependency as: " + outputFile.getAbsolutePath());
                dependentArtifacts.add(outputFile);
            }
        }
        return (File[]) dependentArtifacts.toArray(new File[dependentArtifacts.size()]);
    }

    /**
	 * Checks if an artifact has to be included
	 */
    private boolean includeArtifact(Artifact artifact, ArtifactFilter scopeArtifactFilter, ArtifactFilter includeArtifactFilter, ArtifactFilter excludeArtifactFilter) {
        boolean include = false;
        include = scopeArtifactFilter.include(artifact);
        if (null != includeArtifactFilter) {
            include = includeArtifactFilter.include(artifact);
        }
        if (null != excludeArtifactFilter) {
            include = excludeArtifactFilter.include(artifact);
        }
        return include;
    }
}
