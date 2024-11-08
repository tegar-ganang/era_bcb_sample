package org.sf.pomreader;

import org.apache.maven.bootstrap.compile.CompilerConfiguration;
import org.apache.maven.bootstrap.download.ArtifactResolver;
import org.apache.maven.bootstrap.download.RepositoryMetadata;
import org.apache.maven.bootstrap.model.Dependency;
import org.apache.maven.bootstrap.model.Model;
import org.apache.maven.bootstrap.model.Repository;
import org.apache.maven.bootstrap.util.FileUtils;
import org.apache.maven.bootstrap.util.JarMojo;
import org.xml.sax.SAXException;
import org.sf.jlaunchpad.util.ChecksumCalculator;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is project installer class. It reads pom.xml file and then installs it into
 * the local repository. If there are dependencies declared, they will be downloaded
 * (if required) and installed into the local repository.
 *
 * @author Alexander Shvets
 * @version 1.0 12/16/2006
 */
public class ProjectInstaller {

    private Set<String> inProgress = new HashSet<String>();

    private ChecksumCalculator checksumCalculator = new ChecksumCalculator();

    private PomReader pomReader;

    private ArtifactResolver resolver;

    private boolean fullDownload;

    /**
   * Creates new project installer.
   *
   * @throws Exception the exception 
   */
    public ProjectInstaller() throws Exception {
        this(false);
    }

    /**
   * Creates new project installer.
   *
   * @param fullDownload full download
   * @throws Exception the exception
   */
    public ProjectInstaller(boolean fullDownload) throws Exception {
        this.fullDownload = fullDownload;
        pomReader = new PomReader();
        pomReader.init();
        resolver = pomReader.getResolver();
    }

    /**
   * Installs the project.
   *
   * @param args the command line argumemts
   * @throws Exception the exception
   */
    public void install(String[] args) throws Exception {
        String basedir = System.getProperty("basedir");
        if (args.length > 1 && args[0].equals("-basedir")) {
            basedir = args[1];
        }
        boolean build = true;
        String buildStr = System.getProperty("build.required");
        if (args.length > 3 && args[2].equals("-build.required")) {
            buildStr = args[3];
        }
        if (buildStr != null && buildStr.trim().toLowerCase().equals("false")) {
            build = false;
        }
        install(basedir, build);
    }

    /**
   * Installs the project for specified basedir.
   *
   * @param basedir the basedir
   * @param build specify whether make the build (compile, jar etc.) or not
   * @throws Exception the exception
   */
    public void install(String basedir, boolean build) throws Exception {
        File pom = new File(basedir, "pom.xml");
        install(basedir, build, pom);
    }

    /**
   * Installs the project for specified basedir and pom.xml file.
   *
   * @param basedir the basedir
   * @param build specify whether make the build (compile, jar etc.) or not
   * @param pom the pom file
   * @throws Exception the exception
   */
    public void install(String basedir, boolean build, File pom) throws Exception {
        Model model = pomReader.readModel(pom, true);
        File jarFileToInstall;
        if (build) {
            jarFileToInstall = buildProject(model);
        } else {
            String artifactId = model.getArtifactId();
            File buildDirFile = new File(basedir, "target");
            String buildDir = buildDirFile.getAbsolutePath();
            jarFileToInstall = new File(buildDir, artifactId + ".jar");
        }
        install(model, pom, jarFileToInstall);
        String type = model.getPackaging();
        if (type.equalsIgnoreCase("pom")) {
            for (Object o : model.getModules()) {
                String module = (String) o;
                File modulePom = new File(pom.getParent(), module + File.separatorChar + "pom.xml");
                Model moduleReader = pomReader.readModel(modulePom, false);
                installPomFile(moduleReader, modulePom);
            }
        }
    }

    /**
   * Perform installation for specified model.
   *
   * @param model the mode
   * @param pom the pom file
   * @param jar jar file to install
   * @throws Exception the exception
   */
    private void install(Model model, File pom, File jar) throws Exception {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();
        String groupId = model.getGroupId();
        String type = model.getPackaging();
        Repository localRepository = resolver.getLocalRepository();
        File file = localRepository.getArtifactFile(new Dependency(groupId, artifactId, version, type, Collections.EMPTY_LIST));
        if (!type.equalsIgnoreCase("pom")) {
            copyFile(jar, file);
        }
        installPomFile(model, pom);
        RepositoryMetadata metadata = new RepositoryMetadata();
        metadata.setReleaseVersion(version);
        metadata.setLatestVersion(version);
        file = localRepository.getMetadataFile(groupId, artifactId, null, type, "maven-metadata-local.xml");
        metadata.write(file);
        metadata = new RepositoryMetadata();
        metadata.setLocalCopy(true);
        metadata.setLastUpdated(getCurrentUtcDate());
        file = localRepository.getMetadataFile(groupId, artifactId, version, type, "maven-metadata-local.xml");
        metadata.write(file);
    }

    /**
   * Installs pom file.
   *
   * @param model the model
   * @param source the source
   * @throws IOException I/O exception
   */
    private void installPomFile(Model model, File source) throws IOException {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();
        String groupId = model.getGroupId();
        Repository localRepository = resolver.getLocalRepository();
        File pom = localRepository.getMetadataFile(groupId, artifactId, version, model.getPackaging(), artifactId + "-" + version + ".pom");
        copyFile(source, pom);
    }

    /**
   * Copies dest from one location to another.
   * @param src source file
   * @param dest destination file
   * @throws IOException I/O exception
   */
    private void copyFile(File src, File dest) throws IOException {
        FileUtils.copyFile(src, dest);
        checksumCalculator.calculateForFile(dest);
    }

    /**
   * Builds the project.
   *
   * @param basedir the basedir
   * @param buildModules build modules or not
   * @throws Exception exception
   */
    public void buildProject(File basedir, boolean buildModules) throws Exception {
        if (buildModules) {
            cacheModels(basedir);
        }
        File file = new File(basedir, "pom.xml");
        Model model = pomReader.readModel(file, true, fullDownload);
        String key = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getPackaging();
        if (inProgress.contains(key)) {
            return;
        }
        if (model.getPackaging().equalsIgnoreCase("pom")) {
            if (buildModules) {
                for (Object o : model.getModules()) {
                    String module = (String) o;
                    buildProject(new File(basedir, module), true);
                }
            }
            return;
        }
        inProgress.add(key);
        if (resolver.isAlreadyBuilt(key)) {
            return;
        }
        buildProject(model);
        inProgress.remove(key);
    }

    /**
   * Builds the project.
   *
   * @param model the model
   * @return the project
   * @throws Exception exception
   */
    private File buildProject(Model model) throws Exception {
        File basedir = model.getProjectFile().getParentFile();
        String sources = new File(basedir, "src/main/java").getAbsolutePath();
        String resources = new File(basedir, "src/main/resources").getAbsolutePath();
        String classes = new File(basedir, "target/classes").getAbsolutePath();
        File buildDirFile = new File(basedir, "target");
        String buildDir = buildDirFile.getAbsolutePath();
        for (Object o : model.getAllDependencies()) {
            Dependency dep = (Dependency) o;
            dep.getRepositories().addAll(model.getRepositories());
            if (pomReader.getCachedModelFile(dep.getId()) != null) {
                buildProject(resolver.getArtifactFile(dep.getPomDependency()).getParentFile(), false);
            }
        }
        resolver.downloadDependencies(model.getAllDependencies());
        File jarFile;
        if (model.getPackaging().equalsIgnoreCase("jar")) {
            System.out.println("Compiling sources ...");
            compile(model.getAllDependencies(), sources, classes, null, null, Dependency.SCOPE_COMPILE, resolver);
            System.out.println("Packaging resources ...");
            copyResources(resources, classes);
            jarFile = createJar(new File(basedir, "pom.xml"), classes, buildDir, model);
            System.out.println("Packaging " + jarFile + " ...");
            resolver.addBuiltArtifact(model.getGroupId(), model.getArtifactId(), "jar", jarFile);
        } else {
            jarFile = new File(basedir, "pom.xml");
        }
        return jarFile;
    }

    /**
   * Creates new jar file.
   * @param pomFile the pom file
   * @param classesDir the classesDir dir
   * @param buildDir the buildDir
   * @param model the model
   * @return jar file
   * @throws Exception the exception
   */
    private File createJar(File pomFile, String classesDir, String buildDir, Model model) throws Exception {
        JarMojo jarMojo = new JarMojo();
        String artifactId = model.getArtifactId();
        Properties p = new Properties();
        p.setProperty("groupId", model.getGroupId());
        p.setProperty("artifactId", model.getArtifactId());
        p.setProperty("version", model.getVersion());
        File pomPropertiesDir = new File(new File(classesDir), "META-INF/maven/" + model.getGroupId() + "/" + model.getArtifactId());
        pomPropertiesDir.mkdirs();
        File pomPropertiesFile = new File(pomPropertiesDir, "pom.properties");
        OutputStream os = new FileOutputStream(pomPropertiesFile);
        p.store(os, "Generated by Maven");
        os.close();
        FileUtils.copyFile(pomFile, new File(pomPropertiesDir, "pom.xml"));
        File jarFile = new File(buildDir, artifactId + ".jar");
        jarMojo.execute(new File(classesDir), jarFile);
        return jarFile;
    }

    public String getCurrentUtcDate() {
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        DateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(timezone);
        return fmt.format(new Date());
    }

    private void copyResources(String sourceDirectory, String destinationDirectory) throws Exception {
        File sd = new File(sourceDirectory);
        if (!sd.exists()) {
            return;
        }
        List files = FileUtils.getFiles(sd, "**/**", "**/CVS/**,**/.svn/**", false);
        for (Object file : files) {
            File f = (File) file;
            File source = new File(sourceDirectory, f.getPath());
            File dest = new File(destinationDirectory, f.getPath());
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            FileUtils.copyFile(source, dest);
        }
    }

    protected static String formatTime(long ms) {
        long secs = ms / 1000;
        long min = secs / 60;
        secs = secs % 60;
        if (min > 0) {
            return min + " minutes " + secs + " seconds";
        } else {
            return secs + " seconds";
        }
    }

    private void compile(Collection dependencies, String sourceDirectory, String outputDirectory, String extraClasspath, File generatedSources, String scope, ArtifactResolver resolver) throws Exception {
        JavacCompiler compiler = new JavacCompiler();
        String[] sourceDirectories = null;
        if (generatedSources != null) {
            if (new File(sourceDirectory).exists()) {
                sourceDirectories = new String[] { sourceDirectory, generatedSources.getAbsolutePath() };
            } else {
                sourceDirectories = new String[] { generatedSources.getAbsolutePath() };
            }
        } else {
            if (new File(sourceDirectory).exists()) {
                sourceDirectories = new String[] { sourceDirectory };
            }
        }
        if (sourceDirectories != null) {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.setOutputLocation(outputDirectory);
            List classpathEntries = classpath(dependencies, extraClasspath, scope, resolver);
            compilerConfiguration.setNoWarn(true);
            compilerConfiguration.setClasspathEntries(classpathEntries);
            compilerConfiguration.setSourceLocations(Arrays.asList(sourceDirectories));
            Map<String, String> compilerOptions = new HashMap<String, String>();
            compilerOptions.putAll(compilerConfiguration.getCompilerOptions());
            String javaSpecificationVersionLevel = System.getProperty("java.specification.version.level");
            System.out.println("Java Specification Version Level: " + javaSpecificationVersionLevel);
            compilerOptions.put("-source", javaSpecificationVersionLevel);
            compilerOptions.put("-target", javaSpecificationVersionLevel);
            compilerConfiguration.setCompilerOptions(compilerOptions);
            String debugAsString = System.getProperty("maven.compiler.debug", "true");
            if (!Boolean.valueOf(debugAsString)) {
                compilerConfiguration.setDebug(false);
            } else {
                compilerConfiguration.setDebug(true);
            }
            List messages = compiler.compile(compilerConfiguration);
            for (Object message : messages) {
                System.out.println(message);
            }
            if (messages.size() > 0) {
                throw new Exception("Compilation error.");
            }
        }
    }

    private List classpath(Collection dependencies, String extraClasspath, String scope, ArtifactResolver resolver) {
        List<String> classpath = new ArrayList<String>(dependencies.size() + 1);
        for (Object dependency : dependencies) {
            Dependency d = (Dependency) dependency;
            String element = resolver.getArtifactFile(d).getAbsolutePath();
            if (Dependency.SCOPE_COMPILE.equals(scope)) {
                if (d.getScope().equals(Dependency.SCOPE_COMPILE)) {
                    classpath.add(element);
                }
            } else if (Dependency.SCOPE_RUNTIME.equals(scope)) {
                if (d.getScope().equals(Dependency.SCOPE_COMPILE) || d.getScope().equals(Dependency.SCOPE_RUNTIME)) {
                    classpath.add(element);
                }
            } else if (Dependency.SCOPE_TEST.equals(scope)) {
                classpath.add(element);
            }
        }
        if (extraClasspath != null) {
            classpath.add(extraClasspath);
        }
        return classpath;
    }

    private void cacheModels(File basedir) throws IOException, ParserConfigurationException, SAXException {
        Model model = pomReader.readModel(new File(basedir, "pom.xml"), false);
        for (Object o : model.getModules()) {
            String module = (String) o;
            cacheModels(new File(basedir, module));
        }
    }

    /**
   * Launches the installer from the command line.
   *
   * @param args The application command-line arguments.
   * @throws Exception exception
   */
    public static void main(String[] args) throws Exception {
        ProjectInstaller projectInstaller = new ProjectInstaller();
        projectInstaller.install(args);
    }
}
