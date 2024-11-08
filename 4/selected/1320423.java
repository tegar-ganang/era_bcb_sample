package org.servebox.flex.mojo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.servebox.flex.mojo.base.AbstractFlexMakeMojo;
import org.servebox.flex.mojo.base.ComponentIncludeListener;
import org.servebox.flex.mojo.util.FlexMojoUtils;
import org.servebox.flex.mojo.util.SourceContentUtil;
import flex2.tools.oem.Builder;
import flex2.tools.oem.Library;

/**
 * Build MXML and AS files to a SWC artifact.
 * 
 * @author J.F.Mathiot
 * @goal makeswc
 * @requiresDependencyResolution compile
 * @execute phase="package" lifecycle="swclifecycle"
 * @requiresProject
 */
public class MakeSWCMojo extends AbstractFlexMakeMojo implements ComponentIncludeListener {

    /**
     * Indicate whether the compiler should create the RSL (SWF file) for the artifact.
     * 
     * @parameter expression="${flex.compiler.makeRSL}" default-value="false"
     */
    protected boolean makeRSL = false;

    /**
     * Indicate whether the compiler should create a SHA-256 Digest used to identify the
     * library when linking it as a RSL.
     * 
     * @parameter expression="${flex.compiler.computeDigest}" default-value="true"
     */
    protected boolean computeDigest = true;

    /**
     * The namespaces to include to the artifact.
     * 
     * @parameter expression="${flex.compiler.includeNamespaces}"
     */
    protected ArrayList<String> includeNamespaces;

    /**
     * Force the compiler to include only classes referenced by component manifests and
     * the includeNamespaces parameter. If there is no namespace defined in the
     * includeNamespaces parameter, all the ActionScript and MXML files from the source
     * directory will be included.
     * 
     * @parameter expression="${flex.compiler.includeNamespaces}" default-value="true"
     */
    protected boolean strictNamespacesIncludes = true;

    private boolean hasNamespaces;

    private Library libraryReference;

    private Set<File> excludedFiles;

    protected String getArtifactExtension() {
        return FlexMojoUtils.getSWCPackagingType();
    }

    protected Builder getCompilerInstance() throws Exception {
        return new Library();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (makeRSL) {
            copyRSL();
        }
    }

    private void copyRSL() throws MojoExecutionException {
        File rslLibFile = new File(FlexMojoUtils.getResourcesDirectory(outputDirectory), "rsl/library.swf");
        if (!rslLibFile.exists()) {
            throw new MojoExecutionException("Could not find generated RSL");
        }
        try {
            FileUtils.copyFile(rslLibFile, getOutputArtifactFile("rsl", FlexMojoUtils.getRSLExtension(false)));
        } catch (Exception e) {
            throw new MojoExecutionException("Could not copy generated RSL file to target directory.");
        }
    }

    @Override
    protected void handleSources(Builder compiler) throws MojoExecutionException {
        super.handleSources(compiler);
        Library lib = (Library) compiler;
        handleSourceDirectoryContents(lib);
    }

    @Override
    protected void handleCompilationOptions(Builder compiler) throws MojoExecutionException {
        super.handleCompilationOptions(compiler);
        if (makeRSL) {
            ((Library) compiler).setDirectory(new File(FlexMojoUtils.getResourcesDirectory(outputDirectory), "rsl"));
        }
        compiler.getConfiguration().enableDigestComputation(computeDigest);
    }

    public void addComponent(File f, String className) throws MojoExecutionException {
        if (strictNamespacesIncludes && hasNamespaces) {
            return;
        }
        getLog().info("Adding component " + f.getName() + " to the library.");
        libraryReference.addComponent(f);
    }

    public void addComponentManifest(File manifestFile, String manifestUri) throws MojoExecutionException {
        try {
            libraryReference.addComponent(new URI(manifestUri));
            hasNamespaces = true;
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Malformed uri for namespace " + manifestUri, e);
        }
    }

    public void setComponentManifestContent(String manifestUri, Set<File> files, Set<String> classNames) throws MojoExecutionException {
        if (excludedFiles == null) {
            excludedFiles = new HashSet<File>();
        }
        excludedFiles.addAll(files);
    }

    private void handleSourceDirectoryContents(Library lib) throws MojoExecutionException {
        libraryReference = lib;
        hasNamespaces = false;
        SourceContentUtil.handleSourceContent(this, sourceDirectory, componentManifests, includeNamespaces, getLog());
        libraryReference = null;
        File defaultCSS = new File("src/main/resources/default.css");
        if (defaultCSS.exists()) {
            StyleSheet styleSheet = new StyleSheet();
            styleSheet.setName("default.css");
            styleSheet.setFile(defaultCSS);
            lib.addArchiveFile(styleSheet.getName(), styleSheet.getFile());
        }
        for (ArchiveFile archive : archiveFiles) {
            if (!archive.getFile().exists()) {
                throw new MojoExecutionException("The archive file : " + archive.getFile() + " does not exist.");
            }
            lib.addArchiveFile(archive.getName(), archive.getFile());
        }
        for (StyleSheet styleSheet : styleSheets) {
            if (!styleSheet.getFile().exists()) {
                throw new MojoExecutionException("The style sheet : " + styleSheet.getFile() + " does not exist.");
            }
            lib.addArchiveFile(styleSheet.getName(), styleSheet.getFile());
        }
    }
}
