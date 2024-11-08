package com.mindtree.techworks.infix.pluginscommon.mojo.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.FileUtils;
import com.mindtree.techworks.infix.pluginscommon.mojo.InfixExecutionException;
import com.mindtree.techworks.infix.pluginscommon.mojo.MojoInfo;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.DependencySet;
import com.mindtree.techworks.infix.pluginscommon.mojo.locationbase.LocationBase;

/**
 * Resolves the dependencies
 * 
 * @author Bindul Bhowmik
 * @version $Revision: 114 $ $Date: 2012-02-14 10:08:55 -0500 (Tue, 14 Feb 2012) $
 */
@Component(role = Resolver.class, hint = "dependency")
public class DependencyResolver implements Resolver {

    @Override
    public List<String> getRelativeFilePath(LocationBase setBase, MojoInfo mojoInfo) throws InfixExecutionException {
        File[] selectedFiles = resolveDependencies((DependencySet) setBase, mojoInfo);
        List<String> relativePaths = new ArrayList<String>(selectedFiles.length);
        String relativeBase = (null == setBase.getOutputDirectory()) ? "" : setBase.getOutputDirectory() + File.separator;
        for (int i = 0; i < selectedFiles.length; i++) {
            mojoInfo.getLog().debug("Adding: " + selectedFiles[i].getAbsolutePath());
            relativePaths.add(relativeBase + selectedFiles[i].getName());
        }
        return relativePaths;
    }

    @Override
    public void copyFiles(LocationBase setBase, MojoInfo mojoInfo, File archiveTempDir) throws InfixExecutionException {
        DependencySet dependencySet = (DependencySet) setBase;
        File[] selectedFiles = resolveDependencies(dependencySet, mojoInfo);
        File destinationDir = new File(archiveTempDir, ((null == dependencySet.getOutputDirectory()) ? "" : dependencySet.getOutputDirectory()));
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                throw new InfixExecutionException("Could not create " + "destination directory: " + destinationDir.getAbsolutePath());
            }
        }
        for (int i = 0; i < selectedFiles.length; i++) {
            File destinationFile = new File(destinationDir, selectedFiles[i].getName());
            mojoInfo.getLog().debug("Copying: " + selectedFiles[i] + " to " + destinationFile.getAbsolutePath());
            try {
                FileUtils.copyFile(selectedFiles[i], destinationFile);
            } catch (IOException e) {
                mojoInfo.getLog().error("Error copying " + selectedFiles[i], e);
                throw new InfixExecutionException("Error copying " + selectedFiles[i], e);
            }
        }
    }

    @Override
    public List<File> resolveFiles(LocationBase setBase, MojoInfo mojoInfo) {
        DependencySet dependencySet = (DependencySet) setBase;
        File[] resolvedDependencies = resolveDependencies(dependencySet, mojoInfo);
        return Arrays.asList(resolvedDependencies);
    }

    /**
	 * Resolves the dependencies
	 * @return 
	 */
    protected File[] resolveDependencies(DependencySet dependencySet, MojoInfo mojoInfo) {
        MavenProject project = mojoInfo.getProject();
        @SuppressWarnings("unchecked") Set<Artifact> artifactSet = project.getArtifacts();
        ArtifactFilter scopeArtifactFilter = new ScopeArtifactFilter(dependencySet.getScope());
        ArtifactFilter includeArtifactFilter = null;
        ArtifactFilter excludeArtifactFilter = null;
        if (null != dependencySet.getIncludes()) {
            includeArtifactFilter = new IncludesArtifactFilter(dependencySet.getIncludes());
        }
        if (null != dependencySet.getExcludes()) {
            excludeArtifactFilter = new ExcludesArtifactFilter(dependencySet.getExcludes());
        }
        List<File> dependentArtifacts = new ArrayList<File>(artifactSet.size());
        for (Iterator<Artifact> artifacts = artifactSet.iterator(); artifacts.hasNext(); ) {
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
