package protoj.lang.internal;

import java.io.File;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import protoj.core.ArgRunnable;
import protoj.core.ProjectLayout;
import protoj.core.ResourceFeature;
import protoj.core.internal.AntTarget;
import protoj.core.internal.ProtoCore;
import protoj.lang.ArchiveFeature;
import protoj.lang.ClassesArchive;
import protoj.lang.JavadocArchive;
import protoj.lang.ProjectArchive;
import protoj.lang.SourceArchive;
import protoj.lang.StandardProject;
import protoj.lang.ClassesArchive.ClassesEntry;

/**
 * Represents the use-cases available to be carried out against the protoj
 * delegate.
 * 
 * @author Ashley Williams
 * 
 */
public final class ProtoProject {

    private final class TarConfig implements ArgRunnable<ProjectArchive> {

        public void run(ProjectArchive feature) {
            feature.addFileSet("777", "777", "example/** README.txt LICENSE.txt NOTICE.txt", "example/**/lib/*.jar example/**/protoj.log example/**/classes/**/* example/**/target/**/*");
        }
    }

    /**
	 * See {@link ProtoProject}.
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        new ProtoProject(args).dispatchCommands();
    }

    /**
	 * See {@link #getDelegate()}.
	 */
    private StandardProject delegate;

    /**
	 * See {@link #getReleaseCommand()}.
	 */
    private ReleaseCommand releaseCommand;

    /**
	 * See {@link #getReleaseFeature()}.
	 */
    private ReleaseFeature releaseFeature;

    private ProtoCore core;

    private SiteCommand siteCommand;

    /**
	 * Constructor that uses system properties protoj.rootDir and
	 * protoj.scriptName.
	 */
    public ProtoProject() {
        this(new File(System.getProperty("protoj.rootDir")), System.getProperty("protoj.scriptName"));
    }

    /**
	 * See the
	 * <code>StandardProject.StandardProject(String[], String, String)</code>
	 * constructor for a discussion of the arguments to main().
	 * 
	 * @param args
	 */
    public ProtoProject(String[] args) {
        this(new ProtoCore(args));
    }

    /**
	 * Create with the specified project root directory and launch script.
	 * 
	 * @param rootDir
	 * @param scriptName
	 */
    public ProtoProject(File rootDir, String scriptName) {
        this(new ProtoCore(rootDir, scriptName));
    }

    /**
	 * Create with the underlying bootstrap instance.
	 * 
	 * @param core
	 */
    public ProtoProject(ProtoCore core) {
        this.core = core;
        delegate = new StandardProject(core.getCore(), createVersionInfo());
        releaseFeature = new ReleaseFeature(this);
        PropertiesConfiguration replacements = new PropertiesConfiguration();
        replacements.addProperty("protoj.version", getVersionNumber());
        replacements.addProperty("protoj.groupid", getGroupId());
        core.getCore().getResourceFeature().initReplacements(replacements);
        initNoDependenciesArchive();
        initDependenciesArchive();
        delegate.getArchiveFeature().initProjectArchive(getProjectArchiveName(), getProjectName(), null, null, new TarConfig());
        delegate.initConfig(true);
        delegate.initJunit("32m");
        delegate.initUploadGoogleCode("protoj");
        delegate.initPublish("scp://shell.sourceforge.net:/home/groups/p/pr/protojrepo/htdocs/mavensync");
        delegate.getPublishFeature().initProvider("wagon-ssh", "1.0-beta-2");
        PublishCommand publishCommand = delegate.getCommands().getPublishCommand();
        publishCommand.getDelegate().setMemory("32m");
        ArchiveCommand archiveCommand = delegate.getCommands().getArchiveCommand();
        archiveCommand.getDelegate().setMemory("32m");
        releaseCommand = new ReleaseCommand(this, this.getClass().getName());
        siteCommand = new SiteCommand(this);
    }

    /**
	 * Configures the artifacts for the no-dependencies archive.
	 */
    private void initNoDependenciesArchive() {
        final String projectVersion = getVersionNumber();
        ClassesArchive classes = delegate.getArchiveFeature().getClassesArchive();
        String jarName = getNoDepJarName();
        classes.addArchive(jarName, "MANIFEST", null, null, new ArgRunnable<ClassesArchive>() {

            public void run(ClassesArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
                archive.initExecutableJar(ProtoExecutableMain.class.getName());
            }
        });
        classes.initExcludeArchives(jarName, "aspectjtools.jar", "aspectjweaver.jar");
        classes.initPublish(jarName, "/protoj/" + jarName + "-publish.xml");
        SourceArchive source = delegate.getArchiveFeature().getSourceArchive();
        source.addArchive(jarName, null, null, null, new ArgRunnable<SourceArchive>() {

            public void run(SourceArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
            }
        });
        JavadocArchive archive = delegate.getArchiveFeature().getJavadocArchive();
        archive.addArchive(jarName, null, null, null, "16m", new ArgRunnable<JavadocArchive>() {

            public void run(JavadocArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
            }
        });
    }

    /**
	 * Configures the artifacts for the dependencies archive.
	 */
    private void initDependenciesArchive() {
        final String projectVersion = getVersionNumber();
        ClassesArchive classes = delegate.getArchiveFeature().getClassesArchive();
        String jarName = getJarName();
        classes.addArchive(jarName, "MANIFEST", null, null, new ArgRunnable<ClassesArchive>() {

            public void run(ClassesArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
                archive.initExecutableJar(ProtoExecutableMain.class.getName());
            }
        });
        classes.initIncludeArchives(jarName, "ant-googlecode-0.0.1.jar", "jsch-0.1.41.jar", "aspectjrt.jar");
        classes.initPublish(jarName, "/protoj/" + jarName + "-publish.xml");
        SourceArchive source = delegate.getArchiveFeature().getSourceArchive();
        source.addArchive(jarName, null, null, null, new ArgRunnable<SourceArchive>() {

            public void run(SourceArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
            }
        });
        JavadocArchive archive = delegate.getArchiveFeature().getJavadocArchive();
        archive.addArchive(jarName, null, null, null, "16m", new ArgRunnable<JavadocArchive>() {

            public void run(JavadocArchive archive) {
                archive.getCurrentAssembleTask().initManifest("ProtoJ-Version", projectVersion);
            }
        });
    }

    /**
	 * Copies the example helloworld project to the target directory and returns
	 * a representative project instance.
	 * 
	 * @return
	 */
    public StandardProject createHelloWorldDelegate() {
        return new SubjectProjectFeature().createTestProject(this, "example/helloworld", "helloworld", null, "*.jar");
    }

    /**
	 * Copies the example alien project to the target directory and returns a
	 * representative project instance.
	 * 
	 * @return
	 */
    public StandardProject createAlienProjectDelegate() {
        return new SubjectProjectFeature().createTestProject(this, "example/alien", "alien", null, "*.jar");
    }

    /**
	 * Ensures the no dependencies jar file is present and executes it so that
	 * the requested sample project gets created. See
	 * {@link ProtoExecutableMain} for a list of the available sample projects.
	 * 
	 * @param projectName
	 *            the name of the sample project to create
	 * @param includeAjc
	 *            for aspectj sample projects specify true to include the
	 *            aspectj jars in the lib directory
	 * 
	 * @return
	 */
    public StandardProject createSampleProjectDelegate(String projectName, boolean includeAjc) {
        createNoDepArchive();
        AntTarget target = new AntTarget("sample");
        ProjectLayout layout = delegate.getLayout();
        target.initLogging(layout.getLogFile(), Project.MSG_INFO);
        Java java = new Java();
        target.addTask(java);
        java.setTaskName("sample-java");
        java.setJar(getNoDepFile());
        java.createArg().setValue("-sample");
        java.createArg().setValue(projectName);
        java.setFork(true);
        java.setFailonerror(true);
        java.setDir(layout.getTargetDir());
        target.execute();
        File rootDir = new File(layout.getTargetDir(), projectName);
        StandardProject sampleProject = new StandardProject(rootDir, projectName, null);
        File protoLibDir = core.getCore().getLayout().getLibDir();
        File sampleLibDir = sampleProject.getLayout().getLibDir();
        if (includeAjc) {
            File aspectjtools = new File(protoLibDir, "aspectjtools.jar");
            FileUtils.copyFileToDirectory(aspectjtools, sampleLibDir);
            File aspectjweaver = new File(protoLibDir, "aspectjweaver.jar");
            FileUtils.copyFileToDirectory(aspectjweaver, sampleLibDir);
            File aspectjrt = new File(protoLibDir, "aspectjrt.jar");
            FileUtils.copyFileToDirectory(aspectjrt, sampleLibDir);
        }
        return sampleProject;
    }

    /**
	 * Copies the example acme project to the target directory and returns a
	 * representative project instance.
	 * 
	 * @return
	 */
    public StandardProject createAcmeProjectDelegate() {
        return new SubjectProjectFeature().createTestProject(this, "src/testproject/acme", "acme", "aspectjtools.jar,aspectjweaver.jar,aspectjrt.jar", null);
    }

    /**
	 * Dispatches any commands held by this project.
	 */
    public void dispatchCommands() {
        delegate.getDispatchFeature().dispatchCommands();
    }

    /**
	 * The command used to invoke release functionality.
	 * 
	 * @return
	 */
    public ReleaseCommand getReleaseCommand() {
        return releaseCommand;
    }

    /**
	 * The command used to create documentation for the protoj googlecode
	 * website.
	 * 
	 * @return
	 */
    public SiteCommand getSiteCommand() {
        return siteCommand;
    }

    /**
	 * Delegate helper responsible for implementing the release procedure.
	 * 
	 * @return
	 */
    public ReleaseFeature getReleaseFeature() {
        return releaseFeature;
    }

    /**
	 * The main protoj domain object.
	 * 
	 * @return
	 */
    public StandardProject getDelegate() {
        return delegate;
    }

    /**
	 * The name of the project that includes the version number.
	 * 
	 * @return
	 */
    public String getProjectName() {
        return "protoj-" + getVersionNumber();
    }

    /**
	 * The name of the protoj jar file that has been merged with all third party
	 * jar files.
	 * 
	 * @return
	 */
    public String getNoDepJarName() {
        return "protoj-nodep";
    }

    /**
	 * The name of the protoj jar file that contains just a few third-party jar
	 * merges. They are restricted to those libraries that can't be found at the
	 * maven central repository.
	 * 
	 * @return
	 */
    public String getJarName() {
        return "protoj";
    }

    /**
	 * The name of the generated tar file.
	 * 
	 * @return
	 */
    public String getProjectArchiveName() {
        return "protoj";
    }

    /**
	 * The group id to use when publish to the maven repository.
	 * 
	 * @return
	 */
    public String getGroupId() {
        return "com.google.code.protoj";
    }

    /**
	 * Nice to factor this into a method of its own.
	 * 
	 * @return
	 */
    private String createVersionInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProtoJ version " + getVersionNumber());
        return builder.toString();
    }

    /**
	 * The project version number.
	 * 
	 * @return
	 */
    public String getVersionNumber() {
        return core.getVersionNumber();
    }

    /**
	 * Creates the no dependencies archive in the archive directory. Used by the
	 * tests that need to create test projects.
	 */
    public void createNoDepArchive() {
        ArchiveFeature feature = getDelegate().getArchiveFeature();
        ClassesArchive classes = feature.getClassesArchive();
        classes.createArchive(getNoDepJarName());
    }

    public File getNoDepFile() {
        ArchiveFeature feature = getDelegate().getArchiveFeature();
        ClassesArchive classes = feature.getClassesArchive();
        ClassesEntry entry = classes.getEntry(getNoDepJarName());
        return entry.getArchiveEntry().getArtifact();
    }

    /**
	 * Extracts the googlecode website files to the target directory.
	 */
    public void extractSite() {
        ResourceFeature feature = getDelegate().getResourceFeature();
        File targetDir = getDelegate().getLayout().getTargetDir();
        feature.extractToDir("/protoj/site/AlternativeProjects.txt", targetDir);
        feature.extractToDir("/protoj/site/BasicConcepts.txt", targetDir);
        feature.extractToDir("/protoj/site/BuildingFromSource.txt", targetDir);
        feature.extractToDir("/protoj/site/CommandSetup.txt", targetDir);
        feature.extractToDir("/protoj/site/project-summary.txt", targetDir);
        feature.extractToDir("/protoj/site/QuickStart.txt", targetDir);
        feature.extractToDir("/protoj/site/Sidebar.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseCompile.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseConfigure.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseDebug.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseDependencies.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseDeploy.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseHelp.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseLog.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCasePackage.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCasePackageRelationships.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseSpecifyProperties.txt", targetDir);
        feature.extractToDir("/protoj/site/UseCaseTest.txt", targetDir);
    }
}
