package ch.sahits.codegen.java.internal.generator.jet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.codegen.CodeGenPlugin;
import org.eclipse.emf.codegen.jet.JETCompiler;
import org.eclipse.emf.codegen.jet.JETEmitter;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.emf.codegen.jet.JETNature;
import org.eclipse.emf.codegen.util.CodeGenUtil;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;
import ch.sahits.codegen.core.Logging;

/**
 * This is a helper class that is an alternative implementation
 * to  JETEmitter.EclipseHelper. The focus of the reimplementation
 * lies in the better handling of errors so these can be made known to the
 * user.
 * @author Andi Hotz
 * @since 0.9.4
 */
public class JETEmitterInitializer {

    /**
	 * reference to the emitter that should be helped
	 */
    private final JETEmitter jetEmitter;

    /**
	 * Constructor initializing the emitter
	 * @param _jetEmitter the Emitter
	 */
    public JETEmitterInitializer(final JETEmitter _jetEmitter) {
        jetEmitter = _jetEmitter;
    }

    /**
	 * Retrieve the templateURI from the emitter
	 * @return template URI
	 */
    private String getTemplateURI() {
        return (String) getFieldValue("templateURI");
    }

    /**
	 * Generic method to access fields of the jetEmitter
	 * @param fieldName name of the field to be accessed
	 * @return field value
	 */
    private Object getFieldValue(String fieldName) {
        try {
            Field f = jetEmitter.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(jetEmitter);
        } catch (Exception e) {
            Logging.log(e);
            return null;
        }
    }

    /**
	 * Retrieve the templateURIPath from the emitter
	 * @return template URI PAth
	 */
    private String[] getTemplateURIPath() {
        return (String[]) getFieldValue("templateURIPath");
    }

    /**
	 * Retrieve the encoding of the emitter
	 * @return encoding
	 */
    private String getEncoding() {
        return (String) getFieldValue("encoding");
    }

    /**
	 * Retrieve the class loader of the emitter
	 * @return class loader
	 */
    private ClassLoader getClassLoader() {
        return (ClassLoader) getFieldValue("classLoader");
    }

    /**
	 * Retrieve the class path entries of the emitter
	 * @return class path entries
	 */
    @SuppressWarnings("unchecked")
    private List<IClasspathEntry> getClassPathEntries() {
        return (List<IClasspathEntry>) getFieldValue("classpathEntries");
    }

    /**
		 * Initialize the JETEmitter
		 * @param monitor
		 * @throws JETException
		 */
    public void initialize(IProgressMonitor monitor) throws JETException {
        IProgressMonitor progressMonitor = monitor;
        progressMonitor.beginTask("", 10);
        progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_GeneratingJETEmitterFor_message", new Object[] { getTemplateURI() }));
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IJavaModel javaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        try {
            final JETCompiler jetCompiler = getTemplateURIPath() == null ? new MyBaseJETCompiler(getTemplateURI(), getEncoding(), getClassLoader()) : new MyBaseJETCompiler(getTemplateURIPath(), getTemplateURI(), getEncoding(), getClassLoader());
            progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETParsing_message", new Object[] { jetCompiler.getResolvedTemplateURI() }));
            jetCompiler.parse();
            progressMonitor.worked(1);
            String packageName = jetCompiler.getSkeleton().getPackageName();
            if (getTemplateURIPath() != null) {
                URI templateURI = URI.createURI(getTemplateURIPath()[0]);
                URLClassLoader theClassLoader = null;
                if (templateURI.isPlatformResource()) {
                    IProject project = workspace.getRoot().getProject(templateURI.segment(1));
                    if (JETNature.getRuntime(project) != null) {
                        List<URL> urls = new ArrayList<URL>();
                        IJavaProject javaProject = JavaCore.create(project);
                        urls.add(new File(project.getLocation() + "/" + javaProject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL());
                        for (IClasspathEntry classpathEntry : javaProject.getResolvedClasspath(true)) {
                            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                                IPath projectPath = classpathEntry.getPath();
                                IProject otherProject = workspace.getRoot().getProject(projectPath.segment(0));
                                IJavaProject otherJavaProject = JavaCore.create(otherProject);
                                urls.add(new File(otherProject.getLocation() + "/" + otherJavaProject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL());
                            }
                        }
                        theClassLoader = new URLClassLoader(urls.toArray(new URL[0])) {

                            @Override
                            public Class<?> loadClass(String className) throws ClassNotFoundException {
                                try {
                                    return super.loadClass(className);
                                } catch (ClassNotFoundException classNotFoundException) {
                                    return getClassLoader().loadClass(className);
                                }
                            }
                        };
                    }
                } else if (templateURI.isPlatformPlugin()) {
                    final Bundle bundle = Platform.getBundle(templateURI.segment(1));
                    if (bundle != null) {
                        theClassLoader = new URLClassLoader(new URL[0], getClassLoader()) {

                            @Override
                            public Class<?> loadClass(String className) throws ClassNotFoundException {
                                try {
                                    return bundle.loadClass(className);
                                } catch (ClassNotFoundException classNotFoundException) {
                                    return super.loadClass(className);
                                }
                            }
                        };
                    }
                }
                if (theClassLoader != null) {
                    String className = (packageName.length() == 0 ? "" : packageName + ".") + jetCompiler.getSkeleton().getClassName();
                    if (className.endsWith("_")) {
                        className = className.substring(0, className.length() - 1);
                    }
                    try {
                        Class<?> theClass = theClassLoader.loadClass(className);
                        Class<?> theOtherClass = null;
                        try {
                            theOtherClass = getClassLoader().loadClass(className);
                        } catch (ClassNotFoundException exception) {
                        }
                        if (theClass != theOtherClass) {
                            String methodName = jetCompiler.getSkeleton().getMethodName();
                            Method[] methods = theClass.getDeclaredMethods();
                            for (int i = 0; i < methods.length; ++i) {
                                if (methods[i].getName().equals(methodName)) {
                                    jetEmitter.setMethod(methods[i]);
                                    break;
                                }
                            }
                            return;
                        }
                    } catch (ClassNotFoundException exception) {
                    }
                }
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            jetCompiler.generate(outputStream);
            final InputStream contents = new ByteArrayInputStream(outputStream.toByteArray());
            if (!javaModel.isOpen()) {
                javaModel.open(new SubProgressMonitor(progressMonitor, 1));
            } else {
                progressMonitor.worked(1);
            }
            final IProject project = workspace.getRoot().getProject(jetEmitter.getProjectName());
            progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETPreparingProject_message", new Object[] { project.getName() }));
            IJavaProject javaProject;
            if (!project.exists()) {
                progressMonitor.subTask("JET creating project " + project.getName());
                project.create(new SubProgressMonitor(progressMonitor, 1));
                progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETCreatingProject_message", new Object[] { project.getName() }));
                IProjectDescription description = workspace.newProjectDescription(project.getName());
                description.setNatureIds(new String[] { JavaCore.NATURE_ID });
                description.setLocation(null);
                project.open(new SubProgressMonitor(progressMonitor, 1));
                project.setDescription(description, new SubProgressMonitor(progressMonitor, 1));
            } else {
                project.open(new SubProgressMonitor(progressMonitor, 5));
                IProjectDescription description = project.getDescription();
                description.setNatureIds(new String[] { JavaCore.NATURE_ID });
                project.setDescription(description, new SubProgressMonitor(progressMonitor, 1));
            }
            javaProject = JavaCore.create(project);
            List<IClasspathEntry> classpath = new UniqueEList<IClasspathEntry>(Arrays.asList(javaProject.getRawClasspath()));
            for (int i = 0, len = classpath.size(); i < len; i++) {
                IClasspathEntry entry = classpath.get(i);
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && ("/" + project.getName()).equals(entry.getPath().toString())) {
                    classpath.remove(i);
                }
            }
            progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETInitializingProject_message", new Object[] { project.getName() }));
            IClasspathEntry classpathEntry = JavaCore.newSourceEntry(new Path("/" + project.getName() + "/src"));
            IClasspathEntry jreClasspathEntry = JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER"));
            classpath.add(classpathEntry);
            classpath.add(jreClasspathEntry);
            classpath.addAll(getClassPathEntries());
            IFolder sourceFolder = project.getFolder(new Path("src"));
            if (!sourceFolder.exists()) {
                sourceFolder.create(false, true, new SubProgressMonitor(progressMonitor, 1));
            }
            IFolder runtimeFolder = project.getFolder(new Path("bin"));
            if (!runtimeFolder.exists()) {
                runtimeFolder.create(false, true, new SubProgressMonitor(progressMonitor, 1));
            }
            javaProject.setRawClasspath(classpath.toArray(new IClasspathEntry[classpath.size()]), new SubProgressMonitor(progressMonitor, 1));
            javaProject.setOutputLocation(new Path("/" + project.getName() + "/bin"), new SubProgressMonitor(progressMonitor, 1));
            javaProject.close();
            progressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETOpeningJavaProject_message", new Object[] { project.getName() }));
            javaProject.open(new SubProgressMonitor(progressMonitor, 1));
            IPackageFragmentRoot[] packageFragmentRoots = javaProject.getPackageFragmentRoots();
            IPackageFragmentRoot sourcePackageFragmentRoot = null;
            for (int j = 0; j < packageFragmentRoots.length; ++j) {
                IPackageFragmentRoot packageFragmentRoot = packageFragmentRoots[j];
                if (packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    sourcePackageFragmentRoot = packageFragmentRoot;
                    break;
                }
            }
            StringTokenizer stringTokenizer = new StringTokenizer(packageName, ".");
            IProgressMonitor subProgressMonitor = new SubProgressMonitor(progressMonitor, 1);
            subProgressMonitor.beginTask("", stringTokenizer.countTokens() + 4);
            subProgressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_CreateTargetFile_message"));
            IContainer sourceContainer = sourcePackageFragmentRoot == null ? project : (IContainer) sourcePackageFragmentRoot.getCorrespondingResource();
            while (stringTokenizer.hasMoreElements()) {
                String folderName = stringTokenizer.nextToken();
                sourceContainer = sourceContainer.getFolder(new Path(folderName));
                if (!sourceContainer.exists()) {
                    ((IFolder) sourceContainer).create(false, true, new SubProgressMonitor(subProgressMonitor, 1));
                }
            }
            IFile targetFile = sourceContainer.getFile(new Path(jetCompiler.getSkeleton().getClassName() + ".java"));
            if (!targetFile.exists()) {
                subProgressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETCreating_message", new Object[] { targetFile.getFullPath() }));
                targetFile.create(contents, true, new SubProgressMonitor(subProgressMonitor, 1));
            } else {
                subProgressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETUpdating_message", new Object[] { targetFile.getFullPath() }));
                targetFile.setContents(contents, true, true, new SubProgressMonitor(subProgressMonitor, 1));
            }
            subProgressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETBuilding_message", new Object[] { project.getName() }));
            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(subProgressMonitor, 1));
            boolean errors = hasErrors(subProgressMonitor, targetFile);
            if (!errors) {
                subProgressMonitor.subTask(CodeGenPlugin.getPlugin().getString("_UI_JETLoadingClass_message", new Object[] { jetCompiler.getSkeleton().getClassName() + ".class" }));
                List<URL> urls = new ArrayList<URL>();
                urls.add(new File(project.getLocation() + "/" + javaProject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL());
                final Set<Bundle> bundles = new HashSet<Bundle>();
                LOOP: for (IClasspathEntry jetEmitterClasspathEntry : jetEmitter.getClasspathEntries()) {
                    IClasspathAttribute[] classpathAttributes = jetEmitterClasspathEntry.getExtraAttributes();
                    if (classpathAttributes != null) {
                        for (IClasspathAttribute classpathAttribute : classpathAttributes) {
                            if (classpathAttribute.getName().equals(CodeGenUtil.EclipseUtil.PLUGIN_ID_CLASSPATH_ATTRIBUTE_NAME)) {
                                Bundle bundle = Platform.getBundle(classpathAttribute.getValue());
                                if (bundle != null) {
                                    bundles.add(bundle);
                                    continue LOOP;
                                }
                            }
                        }
                    }
                    urls.add(new URL("platform:/resource" + jetEmitterClasspathEntry.getPath() + "/"));
                }
                URLClassLoader theClassLoader = new URLClassLoader(urls.toArray(new URL[0]), getClassLoader()) {

                    @Override
                    public Class<?> loadClass(String className) throws ClassNotFoundException {
                        try {
                            return super.loadClass(className);
                        } catch (ClassNotFoundException exception) {
                            for (Bundle bundle : bundles) {
                                try {
                                    return bundle.loadClass(className);
                                } catch (ClassNotFoundException exception2) {
                                }
                            }
                            throw exception;
                        }
                    }
                };
                Class<?> theClass = theClassLoader.loadClass((packageName.length() == 0 ? "" : packageName + ".") + jetCompiler.getSkeleton().getClassName());
                String methodName = jetCompiler.getSkeleton().getMethodName();
                Method[] methods = theClass.getDeclaredMethods();
                for (int i = 0; i < methods.length; ++i) {
                    if (methods[i].getName().equals(methodName)) {
                        jetEmitter.setMethod(methods[i]);
                        break;
                    }
                }
            }
            subProgressMonitor.done();
        } catch (CoreException exception) {
            throw new JETException(exception);
        } catch (Exception exception) {
            throw new JETException(exception);
        } finally {
            progressMonitor.done();
        }
    }

    /**
	    * Check the targetFile for file generation for errors and
	    * log them
	    * @param subProgressMonitor
	    * @param targetFile
	    * @return true if there are errors that don't allow compilation
	    * @throws CoreException
	    */
    private boolean hasErrors(IProgressMonitor subProgressMonitor, IFile targetFile) throws CoreException {
        IMarker[] markers = targetFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        boolean errors = false;
        for (int i = 0; i < markers.length; ++i) {
            IMarker marker = markers[i];
            if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
                errors = true;
                String errorMessage = marker.getAttribute(IMarker.MESSAGE) + " : " + (CodeGenPlugin.getPlugin().getString("jet.mark.file.line", new Object[] { targetFile.getLocation(), marker.getAttribute(IMarker.LINE_NUMBER) }));
                Logging.addMessage(getClass(), errorMessage);
                Logging.log(new RuntimeException(errorMessage));
                subProgressMonitor.subTask(errorMessage);
            }
        }
        return errors;
    }

    /**
		 * Inner class copied from JETEmitter.MyBaseJETCompiler
		 * @author Andi Hotz
		 * @since 0.9.4
		 */
    protected static class MyBaseJETCompiler extends JETCompiler {

        /** The class loader for the needed classes */
        protected ClassLoader classLoader;

        /**
	      * Constructor without special encoding
	      * @param templateURI URI to the JETemplate
	      * @param classLoader class loader
	      * @throws JETException passed through from the superclass
	      */
        public MyBaseJETCompiler(String templateURI, ClassLoader classLoader) throws JETException {
            super(templateURI);
            this.classLoader = classLoader;
        }

        /**
	      * Constructor with encoding
	      * @param templateURI URI to the JETemplate
	      * @param encoding encoding of the JETemplate
	      * @param classLoader class loader
	      * @throws JETException passed through from the superclass
	      */
        public MyBaseJETCompiler(String templateURI, String encoding, ClassLoader classLoader) throws JETException {
            super(templateURI, encoding);
            this.classLoader = classLoader;
        }

        /**
	      * Constructor for several templates
	      * @param templateURIPath Array of template URIs
	      * @param relativeTemplateURI relative path to the templates
	      * @param classLoader class loader
	      * @throws JETException passed through from the superclass
	      */
        public MyBaseJETCompiler(String[] templateURIPath, String relativeTemplateURI, ClassLoader classLoader) throws JETException {
            super(templateURIPath, relativeTemplateURI);
            this.classLoader = classLoader;
        }

        /**
	      * Constructor for several templates
	      * @param templateURIPath Array of template URIs
	      * @param relativeTemplateURI relative path to the templates
	      * @param encoding encoding of the JETemplate
	      * @param classLoader class loader
	      * @throws JETException passed through from the superclass
	      */
        public MyBaseJETCompiler(String[] templateURIPath, String relativeTemplateURI, String encoding, ClassLoader classLoader) throws JETException {
            super(templateURIPath, relativeTemplateURI, encoding);
            this.classLoader = classLoader;
        }

        /**
	      * Setting the class name for the skeleton
	      */
        @Override
        protected void handleNewSkeleton() {
            String packageName = skeleton.getPackageName();
            String skeletonClassName = skeleton.getClassName();
            String qualifiedSkeletonClassName = (packageName.length() == 0 ? "" : packageName + ".") + skeletonClassName;
            if (classLoader != null) {
                try {
                    Class<?> theClass = classLoader.loadClass(qualifiedSkeletonClassName);
                    if (theClass != null) {
                        skeleton.setClassName(skeletonClassName += "_");
                    }
                } catch (Exception exception) {
                }
            }
        }
    }
}
