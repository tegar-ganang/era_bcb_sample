package tudresden.ocl20.pivot.ocl2parser.testcasegenerator.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.codegen.ecore.CodeGenEcorePlugin;
import org.eclipse.emf.codegen.util.CodeGenUtil;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.Bundle;
import tudresden.ocl20.pivot.ocl2parser.testcasegenerator.Activator;
import de.hunsicker.jalopy.Jalopy;

/**
 * The code builder generates the test plugin with its elements (MANIFEST.MF, etc.) and
 * also the test case files. 
 * @author Nils
 *
 */
public class CodeBuilder implements ICodeBuilder {

    private String testName;

    private String metamodel;

    private String modelFile;

    private List<TestcaseStringElement> testcaseStringElements;

    private TestcaseStringElement actualTestcaseStringElement;

    private String testsuiteName;

    private List<String> testClassNames;

    private String projectname;

    private IProject project;

    /**
	 * This is the package name that is used for generating
	 * the code for testcase and test suites.
	 */
    private String packagename;

    /**
	 * While creating the code location we save us the source folder to
	 * add new files.
	 */
    private IFolder sourceContainer;

    /**
	 * This is the default folder with the name of the project.
	 */
    private IFolder defaultFolder;

    public CodeBuilder(String projectname) throws Exception {
        this.projectname = projectname;
        this.packagename = projectname;
        try {
            Velocity.init();
        } catch (Exception ex) {
            throw new BuilderException("An error occurred while initializing the velocity engine.", ex);
        }
        createCodeLocation();
        createDefaultFolder();
        createComparator();
        createActivatorFile();
        createManifestMF();
        createBuildProperties();
    }

    /**
	 * The following code has been adopted from
	 * tudresden.ocl20.pivot.codegen.adapter$GenModelPivotAdapterGeneratorAdapter.ensureProjectExists
	 */
    public void createCodeLocation() {
        List<IClasspathEntry> classpathEntries = new UniqueEList<IClasspathEntry>();
        project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectname);
        try {
            IProjectDescription projectDescription = null;
            IJavaProject javaProject = JavaCore.create(project);
            if (project.exists()) {
                project.delete(true, null);
            }
            projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectname);
            project.create(projectDescription, new NullProgressMonitor());
            String[] natureIds = projectDescription.getNatureIds();
            if (natureIds == null) {
                natureIds = new String[] { JavaCore.NATURE_ID };
            } else {
                boolean hasJavaNature = false;
                boolean hasPDENature = false;
                for (int i = 0; i < natureIds.length; ++i) {
                    if (JavaCore.NATURE_ID.equals(natureIds[i])) {
                        hasJavaNature = true;
                    }
                    if ("org.eclipse.pde.PluginNature".equals(natureIds[i])) {
                        hasPDENature = true;
                    }
                }
                if (!hasJavaNature) {
                    String[] oldNatureIds = natureIds;
                    natureIds = new String[oldNatureIds.length + 1];
                    System.arraycopy(oldNatureIds, 0, natureIds, 0, oldNatureIds.length);
                    natureIds[oldNatureIds.length] = JavaCore.NATURE_ID;
                }
                if (!hasPDENature) {
                    String[] oldNatureIds = natureIds;
                    natureIds = new String[oldNatureIds.length + 1];
                    System.arraycopy(oldNatureIds, 0, natureIds, 0, oldNatureIds.length);
                    natureIds[oldNatureIds.length] = "org.eclipse.pde.PluginNature";
                }
            }
            projectDescription.setNatureIds(natureIds);
            ICommand[] builders = projectDescription.getBuildSpec();
            if (builders == null) {
                builders = new ICommand[0];
            }
            boolean hasManifestBuilder = false;
            boolean hasSchemaBuilder = false;
            for (int i = 0; i < builders.length; ++i) {
                if ("org.eclipse.pde.ManifestBuilder".equals(builders[i].getBuilderName())) {
                    hasManifestBuilder = true;
                }
                if ("org.eclipse.pde.SchemaBuilder".equals(builders[i].getBuilderName())) {
                    hasSchemaBuilder = true;
                }
            }
            if (!hasManifestBuilder) {
                ICommand[] oldBuilders = builders;
                builders = new ICommand[oldBuilders.length + 1];
                System.arraycopy(oldBuilders, 0, builders, 0, oldBuilders.length);
                builders[oldBuilders.length] = projectDescription.newCommand();
                builders[oldBuilders.length].setBuilderName("org.eclipse.pde.ManifestBuilder");
            }
            if (!hasSchemaBuilder) {
                ICommand[] oldBuilders = builders;
                builders = new ICommand[oldBuilders.length + 1];
                System.arraycopy(oldBuilders, 0, builders, 0, oldBuilders.length);
                builders[oldBuilders.length] = projectDescription.newCommand();
                builders[oldBuilders.length].setBuilderName("org.eclipse.pde.SchemaBuilder");
            }
            projectDescription.setBuildSpec(builders);
            project.open(new NullProgressMonitor());
            project.setDescription(projectDescription, new NullProgressMonitor());
            sourceContainer = project.getFolder("src");
            sourceContainer.create(false, true, new NullProgressMonitor());
            IClasspathEntry sourceClasspathEntry = JavaCore.newSourceEntry(new Path("/" + projectname + "/src"));
            classpathEntries.add(0, sourceClasspathEntry);
            String jreContainer = JavaRuntime.JRE_CONTAINER;
            String complianceLevel = CodeGenUtil.EclipseUtil.getJavaComplianceLevel(project);
            if ("1.5".equals(complianceLevel)) {
                jreContainer += "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5";
            } else if ("1.6".equals(complianceLevel)) {
                jreContainer += "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.6";
            }
            classpathEntries.add(JavaCore.newContainerEntry(new Path(jreContainer)));
            classpathEntries.add(JavaCore.newContainerEntry(new Path("org.eclipse.pde.core.requiredPlugins")));
            javaProject.setOutputLocation(new Path("/" + projectname + "/bin"), new NullProgressMonitor());
            javaProject.setRawClasspath(classpathEntries.toArray(new IClasspathEntry[classpathEntries.size()]), new NullProgressMonitor());
        } catch (CoreException e) {
            e.printStackTrace();
            CodeGenEcorePlugin.INSTANCE.log(e);
        }
    }

    /**
	 * Creates the file buildProperties.
	 * @throws Exception thrown if the build.properties file isn't found or an error occurs while creating the file in the new project
	 */
    public void createBuildProperties() throws Exception {
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template/build.properties"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File buildPropertiesFile = new File(fileLocatorFileURL.toURI());
        if (!(buildPropertiesFile.exists())) throw new FileNotFoundException("The template file build.properties was not found in the directory template.");
        InputStream inputStream = new FileInputStream(buildPropertiesFile);
        IFile projectBuildProperties = project.getFile("build.properties");
        projectBuildProperties.create(inputStream, true, null);
    }

    public void generateTestcase() throws Exception {
        HashMap templateMap = new HashMap();
        if ((packagename != null) && (!(packagename.equals("")))) {
            templateMap.put("packagename", packagename);
        } else {
            templateMap.put("packagename", packagename);
        }
        String packagePathName = packagename.replace('.', '/');
        templateMap.put("testname", testName);
        templateMap.put("metamodel", metamodel);
        templateMap.put("modelfile", modelFile);
        templateMap.put("defaultpackage", projectname + ".internal");
        templateMap.put("testcaseelementsmap", testcaseStringElements);
        File packageDirectory = new File(sourceContainer.getLocation().toFile().getCanonicalPath() + "/" + packagePathName);
        packageDirectory.mkdirs();
        File newJavaFile = new File(packageDirectory, testName + ".java");
        if (newJavaFile.exists()) {
            newJavaFile.delete();
        }
        newJavaFile.createNewFile();
        VelocityContext ctx = new VelocityContext(templateMap);
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File templateDirectory = new File(fileLocatorFileURL.toURI());
        Properties veloProperties = new Properties();
        veloProperties.setProperty("file.resource.loader.path", templateDirectory.getAbsolutePath());
        VelocityEngine velo = new VelocityEngine(veloProperties);
        Template templ = velo.getTemplate("NamedTestcase.java");
        Writer writer = new BufferedWriter(new FileWriter(newJavaFile));
        templ.merge(ctx, writer);
        writer.flush();
        writer.close();
        createTestdata(modelFile);
    }

    public void generateTestsuite() throws Exception {
        HashMap templateMap = new HashMap();
        templateMap.put("packagename", packagename);
        templateMap.put("testsuitename", testsuiteName);
        String testclassString = BuildingCodeUtilClass.concatElements(testClassNames);
        templateMap.put("testclassnames", testclassString);
        String localProjectName = projectname.replace('.', '/');
        File testsuitePath = null;
        if (packagename.equals("")) {
            testsuitePath = new File(sourceContainer.getLocation().toString() + "/" + localProjectName);
            templateMap.put("packagename", projectname);
        } else {
            String packagePath = packagename.replace('.', '/');
            testsuitePath = new File(sourceContainer.getLocation().toString() + "/" + packagePath);
        }
        if (!(testsuitePath.exists())) testsuitePath.mkdirs();
        File testsuiteFile = new File(testsuitePath.getAbsolutePath() + "/" + testsuiteName + ".java");
        testsuiteFile.createNewFile();
        FileWriter fileWriter = new FileWriter(testsuiteFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        VelocityContext ctx = new VelocityContext(templateMap);
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File templateDirectory = new File(fileLocatorFileURL.toURI());
        Properties veloProperties = new Properties();
        veloProperties.setProperty("file.resource.loader.path", templateDirectory.getAbsolutePath());
        VelocityEngine velo = new VelocityEngine(veloProperties);
        Template tmpl = velo.getTemplate("TemplateSuite.java");
        tmpl.merge(ctx, bufferedWriter);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    /**
	 * Initialize the builder with a valid projectname.
	 * If <i>projectname</i> is null an {@link IllegalArgumentException}
	 * will be thrown.
	 * 
	 * @param projectname the name of the project
	 */
    public void initialize(String projectname) {
        if (projectname == null) throw new IllegalArgumentException("The projectname must not be null");
        this.projectname = projectname;
    }

    /**
	 * Create the activator file of the new plugin. An exception
	 * can occur if a problem occur with the file system or if
	 * the velocity engine has a problem For more details see the error
	 * message of the exception.
	 * @throws Exception is thrown while accessing the file system or through the velocity engine
	 */
    private void createActivatorFile() throws Exception {
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File templateDirectory = new File(fileLocatorFileURL.toURI());
        Properties veloProperties = new Properties();
        veloProperties.setProperty("file.resource.loader.path", templateDirectory.getAbsolutePath());
        VelocityEngine velo = new VelocityEngine(veloProperties);
        String localProjectname = projectname.replace('.', '/');
        File activatorFilePath = new File(sourceContainer.getLocation().toFile().getCanonicalPath() + "/" + localProjectname);
        activatorFilePath.mkdirs();
        File activatorFile = new File(activatorFilePath.getAbsolutePath() + "/Activator.java");
        activatorFile.createNewFile();
        FileWriter activatorFileWriter = new FileWriter(activatorFile);
        BufferedWriter bufWriter = new BufferedWriter(activatorFileWriter, 2048);
        VelocityContext ctx = new VelocityContext();
        ctx.put("projectpackage", projectname);
        Template templ = velo.getTemplate("Activator.java");
        templ.merge(ctx, bufWriter);
        bufWriter.flush();
        bufWriter.close();
    }

    /**
	 * Creates the manifest file of the new project. An exception can be thrown
	 * if an access to the file system isn't possible  or the velocity engine
	 * report an error. For more information see the
	 * exception message.
	 * @throws Exception can be thrown if a file system error occurs or the velocity report an error
	 */
    private void createManifestMF() throws Exception {
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File templateDirectory = new File(fileLocatorFileURL.toURI());
        Properties veloProperties = new Properties();
        veloProperties.setProperty("file.resource.loader.path", templateDirectory.getAbsolutePath());
        VelocityEngine velo = new VelocityEngine(veloProperties);
        IFolder metaInfFolder = project.getFolder("META-INF");
        metaInfFolder.create(true, true, null);
        IFile projectManifestFile = metaInfFolder.getFile("MANIFEST.MF");
        projectManifestFile.create(null, true, null);
        File manifestFile = projectManifestFile.getLocation().toFile();
        FileWriter manifestWriter = new FileWriter(manifestFile);
        BufferedWriter bufWriter = new BufferedWriter(manifestWriter);
        VelocityContext ctx = new VelocityContext();
        ctx.put("projectname", projectname);
        ctx.put("activatorpackage", projectname);
        ctx.put("bundlename", projectname);
        Template templ = velo.getTemplate("MANIFEST.MF");
        templ.merge(ctx, bufWriter);
        bufWriter.flush();
        bufWriter.close();
    }

    /**
	 * Creates the classes for the compare package.
	 * @throws Exception is thrown if an error occurs while creating the files and folders
	 */
    private void createComparator() throws Exception {
        IFolder internalFolder = defaultFolder.getFolder("internal");
        internalFolder.create(true, true, null);
        IFolder destCompareDirectory = internalFolder.getFolder("compare");
        destCompareDirectory.create(true, true, null);
        IFolder srcStringTreeFolder = destCompareDirectory.getFolder("stringTree");
        srcStringTreeFolder.create(true, true, null);
        VelocityContext ctx = new VelocityContext();
        ctx.put("packagename", projectname + ".internal");
        URL fileLocatorURL = FileLocator.find(Activator.getDefault().getBundle(), new Path("template/compare"), null);
        URL fileLocatorFileURL = FileLocator.toFileURL(fileLocatorURL);
        File srcCompareDirectory = new File(fileLocatorFileURL.toURI());
        Properties veloProperties = new Properties();
        veloProperties.setProperty("file.resource.loader.path", srcCompareDirectory.getAbsolutePath());
        VelocityEngine velo = new VelocityEngine(veloProperties);
        File[] contentCompareDirectory = srcCompareDirectory.listFiles();
        for (File sourceFile : contentCompareDirectory) {
            if (sourceFile.isDirectory()) continue;
            IFile destinationFile = destCompareDirectory.getFile(sourceFile.getName());
            destinationFile.create(null, true, null);
            Writer destWriter = new FileWriter(destinationFile.getLocation().toFile());
            Writer bufWriter = new BufferedWriter(destWriter);
            Template templ = velo.getTemplate(sourceFile.getName());
            templ.merge(ctx, bufWriter);
            bufWriter.flush();
            bufWriter.close();
        }
        URL stringTreeDirectoryURL = FileLocator.toFileURL(FileLocator.find(Activator.getDefault().getBundle(), new Path("template/compare/stringTree"), null));
        File srcStringTreeDirectory = new File(stringTreeDirectoryURL.toURI());
        File[] contentStringTreeDirectory = srcStringTreeDirectory.listFiles();
        for (File sourceFile : contentStringTreeDirectory) {
            if (sourceFile.isDirectory()) continue;
            IFile destinationFile = srcStringTreeFolder.getFile(sourceFile.getName());
            destinationFile.create(null, true, null);
            Writer destWriter = new FileWriter(destinationFile.getLocation().toFile());
            Writer bufWriter = new BufferedWriter(destWriter);
            Template templ = velo.getTemplate("./stringTree/" + sourceFile.getName());
            templ.merge(ctx, bufWriter);
            bufWriter.flush();
            bufWriter.close();
        }
    }

    /**
	 * Creates the default package (folder) for the project. It is named
	 * by the projectname.
	 * @throws Exception is thrown if an error occurs while creating the new folder
	 */
    private void createDefaultFolder() throws Exception {
        IPath defaultFolderPath = new Path(projectname.replace(".", "/"));
        IFolder tempFolder = sourceContainer;
        for (int i = 0; i < defaultFolderPath.segmentCount(); i++) {
            String segment = defaultFolderPath.segment(i);
            tempFolder = tempFolder.getFolder(segment);
            tempFolder.create(true, true, null);
        }
        defaultFolder = tempFolder;
    }

    private void beautifyCode(File toBeautify) throws Exception {
        Jalopy beautifier = new Jalopy();
        beautifier.setInput(toBeautify);
        beautifier.setOutput(toBeautify);
        beautifier.format();
        System.out.println(beautifier.getState());
    }

    private void createTestdata(String testFilePath) throws Exception {
        File srcTestFile = new File(testFilePath);
        if (testFilePath.charAt(0) == '.') {
            File parentFile = srcTestFile.getParentFile();
            List<String> srcPath = new ArrayList<String>();
            while (parentFile != null) {
                if (!parentFile.getName().equals(".")) {
                    srcPath.add(parentFile.getName());
                }
                parentFile = parentFile.getParentFile();
            }
            IFolder destElement = null;
            if (srcPath.size() == 0) {
                destElement = project.getFolder(".");
            } else {
                destElement = project.getFolder(srcPath.get(srcPath.size() - 1));
                if (!destElement.exists()) destElement.create(true, true, null);
                for (int i = srcPath.size() - 1; i >= 1; i--) {
                    destElement = destElement.getFolder(srcPath.get(i - 1));
                    if (!destElement.exists()) destElement.create(true, true, null);
                }
            }
            InputStream srcStream = new FileInputStream(srcTestFile);
            IFile testFile = destElement.getFile(srcTestFile.getName());
            if (!testFile.exists()) testFile.create(srcStream, true, null);
        } else {
            if (testFilePath.indexOf("/") == -1) {
                IFolder destFolder = project.getFolder(".");
                InputStream srcStream = new FileInputStream(srcTestFile);
                IFile testFile = destFolder.getFile(srcTestFile.getName());
                if (!testFile.exists()) testFile.create(srcStream, true, null);
            }
        }
    }

    /**
	 * Adds a class name to the code builder.
	 * @param className class name to be added
	 */
    public void addTestClassName(String className) {
        if (testClassNames == null) testClassNames = new ArrayList<String>();
        this.testClassNames.add(className);
    }

    public void addCodeSnippet(String codeSnippet) {
        initActualTestcaseStringElement();
        actualTestcaseStringElement.addCode(codeSnippet);
    }

    public void setTestcaseName(String name) {
        initActualTestcaseStringElement();
        actualTestcaseStringElement.setTestcaseName(name);
    }

    public void setOclExpression(String oclExp) {
        initActualTestcaseStringElement();
        actualTestcaseStringElement.setOclExpression(oclExp);
    }

    public void addVarDeclSnippet(String varDecl) {
        initActualTestcaseStringElement();
        actualTestcaseStringElement.addVarDeclSnippet(varDecl);
    }

    public void setErrorElement(boolean elementValue) {
        initActualTestcaseStringElement();
        actualTestcaseStringElement.setErrorElement(elementValue);
    }

    public void newTestMethod() {
        actualTestcaseStringElement = new TestcaseStringElement();
        testcaseStringElements.add(actualTestcaseStringElement);
    }

    public void newTestCase() {
        testcaseStringElements = new ArrayList<TestcaseStringElement>();
        testName = "";
        metamodel = "";
        modelFile = "";
    }

    /**
	 * Initializes the code builder for the next test suite to be built.
	 */
    public void newTestSuite() {
        testClassNames = new ArrayList<String>();
    }

    /**
	 * Initializes the actual testcase string element if necessary. That means:
	 * if the actualTestcaseStringElement variable is null, then an element will
	 * be created, the actualTestcaseStringElement variable is set to it and
	 * the new element is added to the list of testcase string elements. If
	 * an actual testcase string element exists the method will do nothing.
	 */
    private void initActualTestcaseStringElement() {
        if (actualTestcaseStringElement == null) {
            actualTestcaseStringElement = new TestcaseStringElement();
            testcaseStringElements.add(actualTestcaseStringElement);
        }
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getMetamodel() {
        return metamodel;
    }

    public void setMetamodel(String metamodel) {
        this.metamodel = metamodel;
    }

    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    public String getTestsuiteName() {
        return testsuiteName;
    }

    public void setTestsuiteName(String testsuiteName) {
        this.testsuiteName = testsuiteName;
    }

    public List<String> getTestClassNames() {
        return testClassNames;
    }

    public void setTestClassNames(List<String> testClassNames) {
        this.testClassNames = testClassNames;
    }

    public String getPackagename() {
        return packagename;
    }

    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }

    /**
	 * This class holds all elements that are used to generate
	 * a testcase-method.
	 * @author Nils
	 *
	 */
    public class TestcaseStringElement {

        private String testcaseName;

        private String oclExpression;

        private StringBuffer code;

        private StringBuffer variableDeclaration;

        private boolean errorElement;

        public TestcaseStringElement() {
            code = new StringBuffer();
            variableDeclaration = new StringBuffer();
        }

        public String getTestcaseName() {
            return testcaseName;
        }

        public void setTestcaseName(String testcaseName) {
            this.testcaseName = testcaseName;
        }

        public String getOclExpression() {
            return oclExpression;
        }

        public void setOclExpression(String oclExpression) {
            this.oclExpression = oclExpression;
        }

        public String getCode() {
            return code.toString();
        }

        public void addCode(String codeSnippet) {
            code.append(codeSnippet);
        }

        public String getVariableDeclaration() {
            return variableDeclaration.toString();
        }

        public void addVarDeclSnippet(String variableDeclaration) {
            this.variableDeclaration.append(variableDeclaration);
        }

        public boolean containsErrorElement() {
            return errorElement;
        }

        public void setErrorElement(boolean element) {
            errorElement = element;
        }
    }
}
