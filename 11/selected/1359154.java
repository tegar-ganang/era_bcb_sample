package net.sf.rcer.jcoimport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * The runnable that performs the actual project generation.
 * @author vwegert
 *
 */
@SuppressWarnings("restriction")
public class ProjectGenerator implements IRunnableWithProgress {

    /**
	 * The ID of the plug-in project nature as used by the PDE. 
	 */
    public static final String PLUGIN_NATURE_ID = "org.eclipse.pde.PluginNature";

    private ProjectGeneratorSettings settings;

    private IWorkspaceRoot workspaceRoot;

    private ArrayList<IPluginModelBase> exportableBundles = new ArrayList<IPluginModelBase>();

    private PluginModelManager modelManager;

    /**
	 * Default constructor.
	 * @param generatorSettings
	 */
    public ProjectGenerator(ProjectGeneratorSettings generatorSettings) {
        this.settings = generatorSettings;
        workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        modelManager = PDECore.getDefault().getModelManager();
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        String sourceArchive;
        exportableBundles.clear();
        monitor.beginTask(Messages.ProjectGenerator_TaskDescription, getNumberOfSteps());
        if (settings.getWin32FileName().length() > 0) {
            sourceArchive = settings.getWin32FileName();
        } else if (settings.getWin64IAFileName().length() > 0) {
            sourceArchive = settings.getWin64IAFileName();
        } else if (settings.getWin64x86FileName().length() > 0) {
            sourceArchive = settings.getWin64x86FileName();
        } else if (settings.getLinux32FileName().length() > 0) {
            sourceArchive = settings.getLinux32FileName();
        } else if (settings.getLinux64IAFileName().length() > 0) {
            sourceArchive = settings.getLinux64IAFileName();
        } else if (settings.getLinux64x86FileName().length() > 0) {
            sourceArchive = settings.getLinux64x86FileName();
        } else if (settings.getDarwin32FileName().length() > 0) {
            sourceArchive = settings.getDarwin32FileName();
        } else if (settings.getDarwin64FileName().length() > 0) {
            sourceArchive = settings.getDarwin64FileName();
        } else {
            sourceArchive = "";
        }
        try {
            if (settings.isPluginProjectSelected()) {
                if (sourceArchive.length() > 0) {
                    createJCoPluginProject(monitor, sourceArchive, IProjectNames.PLUGIN_JCO);
                } else {
                    throw new InvocationTargetException(null, Messages.ProjectGenerator_NoInputFileError);
                }
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isDocPluginProjectSelected()) {
                if (sourceArchive.length() > 0) {
                    createDocPluginProject(monitor, sourceArchive, IProjectNames.PLUGIN_JCO_DOC);
                } else {
                    throw new InvocationTargetException(null, Messages.ProjectGenerator_NoInputFileError);
                }
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isWin32FragmentSelected()) {
                createFragmentProject(monitor, settings.getWin32FileName(), IProjectNames.FRAGMENT_WINDOWS_32, "sapjco3.dll", Messages.ProjectGenerator_Win32Description, "(& (osgi.os=win32) (osgi.arch=x86))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isWin64IAFragmentSelected()) {
                createFragmentProject(monitor, settings.getWin64IAFileName(), IProjectNames.FRAGMENT_WINDOWS_64IA, "sapjco3.dll", Messages.ProjectGenerator_Win64IADescription, "(& (osgi.os=win32) (osgi.arch=ia64n))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isWin64x86FragmentSelected()) {
                createFragmentProject(monitor, settings.getWin64x86FileName(), IProjectNames.FRAGMENT_WINDOWS_64X86, "sapjco3.dll", Messages.ProjectGenerator_Win64x86Description, "(& (osgi.os=win32) (osgi.arch=x86_64))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isLinux32FragmentSelected()) {
                createFragmentProject(monitor, settings.getLinux32FileName(), IProjectNames.FRAGMENT_LINUX_32, "libsapjco3.so", Messages.ProjectGenerator_Linux32Description, "(& (osgi.os=linux) (osgi.arch=x86))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isLinux64IAFragmentSelected()) {
                createFragmentProject(monitor, settings.getLinux64IAFileName(), IProjectNames.FRAGMENT_LINUX_64IA, "libsapjco3.so", Messages.ProjectGenerator_Linux64IADescription, "(& (osgi.os=linux) (osgi.arch=ia64n))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isLinux64x86FragmentSelected()) {
                createFragmentProject(monitor, settings.getLinux64x86FileName(), IProjectNames.FRAGMENT_LINUX_64X86, "libsapjco3.so", Messages.ProjectGenerator_Linux64x86Description, "(& (osgi.os=linux) (osgi.arch=x86_64))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isDarwin32FragmentSelected()) {
                createFragmentProject(monitor, settings.getDarwin32FileName(), IProjectNames.FRAGMENT_DARWIN_32, "libsapjco3.jnilib", Messages.ProjectGenerator_Darwin32Description, "(& (osgi.os=macosx) (osgi.arch=x86))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isDarwin64FragmentSelected()) {
                createFragmentProject(monitor, settings.getDarwin64FileName(), IProjectNames.FRAGMENT_DARWIN_64, "libsapjco3.jnilib", Messages.ProjectGenerator_Darwin64Description, "(& (osgi.os=macosx) (osgi.arch=x86_64))");
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isIDocPluginProjectSelected()) {
                createIDocPluginProject(monitor, settings.getIDocFileName(), IProjectNames.PLUGIN_IDOC, IProjectNames.PLUGIN_JCO);
            }
            if (monitor.isCanceled()) throw new InterruptedException();
            if (settings.isBundleExportSelected()) {
                exportPlugins(monitor);
            }
        } catch (CoreException e) {
            throw new InvocationTargetException(e);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }

    /**
	 * Creates a plug-in project for the SAP JCo from the source file specified.
	 * @param monitor
	 * @param sourceFileName
	 * @param pluginName
	 * @throws CoreException 
	 * @throws IOException 
	 */
    private void createJCoPluginProject(IProgressMonitor monitor, String sourceFileName, String pluginName) throws CoreException, IOException {
        monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CreatePluginTaskDescription, pluginName));
        final Map<String, byte[]> files = readArchiveFile(sourceFileName);
        monitor.worked(10);
        IProject project = workspaceRoot.getProject(pluginName);
        if (project.exists()) {
            project.delete(true, true, new SubProgressMonitor(monitor, 5));
        } else {
            monitor.worked(5);
        }
        project.create(new SubProgressMonitor(monitor, 5));
        project.open(new SubProgressMonitor(monitor, 5));
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID, PLUGIN_NATURE_ID });
        project.setDescription(description, new SubProgressMonitor(monitor, 5));
        IJavaProject javaProject = JavaCore.create(project);
        IFolder binDir = project.getFolder("bin");
        IPath binPath = binDir.getFullPath();
        javaProject.setOutputLocation(binPath, new SubProgressMonitor(monitor, 5));
        project.getFolder("jni").create(true, true, new SubProgressMonitor(monitor, 5));
        project.getFile("sapjco3.jar").create(new ByteArrayInputStream(files.get("sapjco3.jar")), true, new SubProgressMonitor(monitor, 10));
        IFolder metaInfFolder = project.getFolder("META-INF");
        metaInfFolder.create(true, true, new SubProgressMonitor(monitor, 5));
        StringBuilder manifest = new StringBuilder();
        manifest.append("Manifest-Version: 1.0\n");
        manifest.append("Bundle-ManifestVersion: 2\n");
        manifest.append("Bundle-Name: SAP Java Connector v3\n");
        manifest.append(MessageFormat.format("Bundle-SymbolicName: {0}\n", pluginName));
        manifest.append("Bundle-Version: 7.11.0\n");
        manifest.append("Bundle-ClassPath: bin/,\n");
        manifest.append(" sapjco3.jar,\n");
        manifest.append(" jni/\n");
        manifest.append("Bundle-Vendor: SAP AG, Walldorf (packaged using RCER)\n");
        manifest.append("Bundle-RequiredExecutionEnvironment: J2SE-1.5\n");
        manifest.append("Export-Package: com.sap.conn.jco,\n");
        manifest.append(" com.sap.conn.jco.ext,\n");
        manifest.append(" com.sap.conn.jco.monitor,\n");
        manifest.append(" com.sap.conn.jco.rt,\n");
        manifest.append(" com.sap.conn.jco.server\n");
        manifest.append("Bundle-ActivationPolicy: lazy\n");
        writeTextFile(monitor, manifest, metaInfFolder.getFile("MANIFEST.MF"));
        final IPath jcoPath = new Path(MessageFormat.format("/{0}/sapjco3.jar", pluginName));
        IClasspathEntry jcoEntry = JavaCore.newLibraryEntry(jcoPath, Path.EMPTY, Path.EMPTY, true);
        final IPath jniPath = new Path(MessageFormat.format("/{0}/jni", pluginName));
        IClasspathEntry jniEntry = JavaCore.newLibraryEntry(jniPath, Path.EMPTY, Path.EMPTY, true);
        javaProject.setRawClasspath(new IClasspathEntry[] { jcoEntry, jniEntry }, new SubProgressMonitor(monitor, 5));
        StringBuilder buildProperties = new StringBuilder();
        buildProperties.append("bin.includes = META-INF/,\\\n");
        buildProperties.append("               sapjco3.jar,\\\n");
        buildProperties.append("               jni/,\\\n");
        buildProperties.append("               .\n");
        writeTextFile(monitor, buildProperties, project.getFile("build.properties"));
        exportableBundles.add(modelManager.findModel(project));
    }

    /**
	 * Creates a plug-in project for the SAP JCo documentation from the source file specified.
	 * @param monitor
	 * @param sourceFileName
	 * @param pluginName
	 * @throws CoreException 
	 * @throws IOException 
	 */
    private void createDocPluginProject(IProgressMonitor monitor, String sourceFileName, String pluginName) throws CoreException, IOException {
        monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CreatePluginTaskDescription, pluginName));
        final Map<String, byte[]> files = readArchiveFile(sourceFileName);
        monitor.worked(10);
        IProject project = workspaceRoot.getProject(pluginName);
        if (project.exists()) {
            project.delete(true, true, new SubProgressMonitor(monitor, 5));
        } else {
            monitor.worked(5);
        }
        project.create(new SubProgressMonitor(monitor, 5));
        project.open(new SubProgressMonitor(monitor, 5));
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { PLUGIN_NATURE_ID });
        project.setDescription(description, new SubProgressMonitor(monitor, 5));
        copyPluginFile(monitor, project, "/docfiles/plugin.xml", "plugin.xml");
        copyPluginFile(monitor, project, "/docfiles/toc.xml", "toc.xml");
        copyPluginFile(monitor, project, "/docfiles/build.properties", "build.properties");
        project.getFolder("html").create(true, true, null);
        copyPluginFile(monitor, project, "/docfiles/book.css", "html/book.css");
        copyPluginFile(monitor, project, "/docfiles/note.html", "html/note.html");
        monitor.subTask(Messages.ProjectGenerator_CopyDocumentationTaskDescription);
        for (final String filename : files.keySet()) {
            if ((filename.startsWith("examples") || filename.startsWith("javadoc")) && !(filename.endsWith("/"))) {
                monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CopyingFileTaskDescription, filename));
                IFolder currentFolder = null;
                final String[] parts = filename.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (currentFolder == null) {
                        currentFolder = project.getFolder(parts[i]);
                    } else {
                        currentFolder = currentFolder.getFolder(parts[i]);
                    }
                    if (!currentFolder.exists()) {
                        currentFolder.create(true, true, null);
                    }
                }
                project.getFile("/" + filename).create(new ByteArrayInputStream(files.get(filename)), true, null);
            }
        }
        monitor.worked(25);
        IFolder metaInfFolder = project.getFolder("META-INF");
        metaInfFolder.create(true, true, new SubProgressMonitor(monitor, 5));
        StringBuilder manifest = new StringBuilder();
        manifest.append("Manifest-Version: 1.0\n");
        manifest.append("Bundle-ManifestVersion: 2\n");
        manifest.append("Bundle-Name: SAP Java Connector v3 Documentation\n");
        manifest.append(MessageFormat.format("Bundle-SymbolicName: {0};singleton:=true\n", pluginName));
        manifest.append("Bundle-Version: 7.11.0\n");
        manifest.append("Bundle-Vendor: SAP AG, Walldorf (packaged using RCER)\n");
        manifest.append("Bundle-RequiredExecutionEnvironment: J2SE-1.5\n");
        manifest.append("Bundle-ActivationPolicy: lazy\n");
        manifest.append("Require-Bundle: net.sf.rcer.doc\n");
        writeTextFile(monitor, manifest, metaInfFolder.getFile("MANIFEST.MF"));
        exportableBundles.add(modelManager.findModel(project));
    }

    /**
	 * Auxiliary method to copy a file from the generator plug-in to the generated plug-in. 
	 * @param monitor
	 * @param project
	 * @param sourceFileName
	 * @param targetFileName
	 * @throws CoreException
	 * @throws IOException
	 */
    private void copyPluginFile(IProgressMonitor monitor, IProject project, String sourceFileName, String targetFileName) throws CoreException, IOException {
        monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CopyingFileTaskDescription, targetFileName));
        project.getFile(targetFileName).create(FileLocator.openStream(Activator.getDefault().getBundle(), new Path(sourceFileName), false), true, null);
        monitor.worked(1);
    }

    /**
	 * Creates a fragment project from the source file specified.
	 * @param monitor 
	 * @param sourceFileName 
	 * @param fragmentName 
	 * @param nativeLibraryFilename 
	 * @param platformName 
	 * @param platformFilter 
	 * @throws CoreException 
	 * @throws IOException 
	 */
    private void createFragmentProject(IProgressMonitor monitor, String sourceFileName, String fragmentName, String nativeLibraryFilename, String platformName, String platformFilter) throws CoreException, IOException {
        monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CreateFragmentTaskDescription, fragmentName));
        final Map<String, byte[]> files = readArchiveFile(sourceFileName);
        monitor.worked(10);
        IProject project = workspaceRoot.getProject(fragmentName);
        if (project.exists()) {
            project.delete(true, true, new SubProgressMonitor(monitor, 5));
        } else {
            monitor.worked(5);
        }
        project.create(new SubProgressMonitor(monitor, 5));
        project.open(new SubProgressMonitor(monitor, 5));
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { PLUGIN_NATURE_ID });
        project.setDescription(description, new SubProgressMonitor(monitor, 5));
        IFolder jniFolder = project.getFolder("jni");
        jniFolder.create(true, true, new SubProgressMonitor(monitor, 5));
        jniFolder.getFile(nativeLibraryFilename).create(new ByteArrayInputStream(files.get(nativeLibraryFilename)), true, new SubProgressMonitor(monitor, 10));
        IFolder metaInfFolder = project.getFolder("META-INF");
        metaInfFolder.create(true, true, new SubProgressMonitor(monitor, 5));
        StringBuilder manifest = new StringBuilder();
        manifest.append("Manifest-Version: 1.0\n");
        manifest.append("Bundle-ManifestVersion: 2\n");
        manifest.append(MessageFormat.format("Bundle-Name: SAP Java Connector v3 - Native Libraries for {0}\n", platformName));
        manifest.append(MessageFormat.format("Bundle-SymbolicName: {0}\n", fragmentName));
        manifest.append("Bundle-Version: 7.11.0\n");
        manifest.append("Bundle-Vendor: SAP AG, Walldorf (packaged using RCER)\n");
        manifest.append("Fragment-Host: com.sap.conn.jco;bundle-version=\"7.11.0\"\n");
        manifest.append("Bundle-RequiredExecutionEnvironment: J2SE-1.5\n");
        manifest.append(MessageFormat.format("Bundle-NativeCode: jni/{0}\n", nativeLibraryFilename));
        manifest.append(MessageFormat.format("Eclipse-PlatformFilter: {0}\n", platformFilter));
        writeTextFile(monitor, manifest, metaInfFolder.getFile("MANIFEST.MF"));
        StringBuilder buildProperties = new StringBuilder();
        buildProperties.append("bin.includes = META-INF/,\\\n");
        buildProperties.append("               jni/\n");
        writeTextFile(monitor, buildProperties, project.getFile("build.properties"));
        exportableBundles.add(modelManager.findModel(project));
    }

    /**
	 * Creates a plug-in project for the SAP IDoc library from the source file specified.
	 * @param monitor
	 * @param sourceFileName
	 * @param pluginName
	 * @throws CoreException 
	 * @throws IOException 
	 */
    private void createIDocPluginProject(IProgressMonitor monitor, String sourceFileName, String pluginName, String pluginNameJCo) throws CoreException, IOException {
        monitor.subTask(MessageFormat.format(Messages.ProjectGenerator_CreatePluginTaskDescription, pluginName));
        final Map<String, byte[]> files = readArchiveFile(sourceFileName);
        monitor.worked(10);
        IProject project = workspaceRoot.getProject(pluginName);
        if (project.exists()) {
            project.delete(true, true, new SubProgressMonitor(monitor, 5));
        } else {
            monitor.worked(5);
        }
        project.create(new SubProgressMonitor(monitor, 5));
        project.open(new SubProgressMonitor(monitor, 5));
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID, PLUGIN_NATURE_ID });
        project.setDescription(description, new SubProgressMonitor(monitor, 5));
        IJavaProject javaProject = JavaCore.create(project);
        IFolder binDir = project.getFolder("bin");
        IPath binPath = binDir.getFullPath();
        javaProject.setOutputLocation(binPath, new SubProgressMonitor(monitor, 5));
        project.getFile("sapidoc3.jar").create(new ByteArrayInputStream(files.get("sapidoc3.jar")), true, new SubProgressMonitor(monitor, 15));
        IFolder metaInfFolder = project.getFolder("META-INF");
        metaInfFolder.create(true, true, new SubProgressMonitor(monitor, 5));
        StringBuilder manifest = new StringBuilder();
        manifest.append("Manifest-Version: 1.0\n");
        manifest.append("Bundle-ManifestVersion: 2\n");
        manifest.append("Bundle-Name: SAP IDoc Library v3\n");
        manifest.append(MessageFormat.format("Bundle-SymbolicName: {0}\n", pluginName));
        manifest.append("Bundle-Version: 7.11.0\n");
        manifest.append("Bundle-ClassPath: bin/,\n");
        manifest.append(" sapidoc3.jar\n");
        manifest.append("Bundle-Vendor: SAP AG, Walldorf (packaged using RCER)\n");
        manifest.append("Bundle-RequiredExecutionEnvironment: J2SE-1.5\n");
        manifest.append("Export-Package: com.sap.conn.idoc,\n");
        manifest.append(" com.sap.conn.idoc.jco,\n");
        manifest.append(" com.sap.conn.idoc.rt.cp,\n");
        manifest.append(" com.sap.conn.idoc.rt.record,\n");
        manifest.append(" com.sap.conn.idoc.rt.record.impl,\n");
        manifest.append(" com.sap.conn.idoc.rt.trace,\n");
        manifest.append(" com.sap.conn.idoc.rt.util,\n");
        manifest.append(" com.sap.conn.idoc.rt.xml\n");
        manifest.append("Bundle-ActivationPolicy: lazy\n");
        manifest.append(MessageFormat.format("Require-Bundle: {0}\n", pluginNameJCo));
        writeTextFile(monitor, manifest, metaInfFolder.getFile("MANIFEST.MF"));
        final IPath jcoPath = new Path(MessageFormat.format("/{0}/sapidoc3.jar", pluginName));
        IClasspathEntry jcoEntry = JavaCore.newLibraryEntry(jcoPath, Path.EMPTY, Path.EMPTY, true);
        javaProject.setRawClasspath(new IClasspathEntry[] { jcoEntry }, new SubProgressMonitor(monitor, 5));
        StringBuilder buildProperties = new StringBuilder();
        buildProperties.append("bin.includes = META-INF/,\\\n");
        buildProperties.append("               sapidoc3.jar,\\\n");
        buildProperties.append("               .\n");
        writeTextFile(monitor, buildProperties, project.getFile("build.properties"));
        exportableBundles.add(modelManager.findModel(project));
    }

    /**
	 * Exports the generated plug-ins and fragments to the selected location.
	 * @param monitor
	 */
    private void exportPlugins(IProgressMonitor monitor) {
        FeatureExportInfo info = new FeatureExportInfo();
        info.toDirectory = true;
        info.useJarFormat = true;
        info.exportSource = false;
        info.destinationDirectory = settings.getExportPath();
        info.items = exportableBundles.toArray();
        PluginExportOperation job = new PluginExportOperation(info, "");
        job.setUser(true);
        job.schedule();
        job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
    }

    /**
	 * Auxiliary method to dump a {@link StringBuilder} to a file. 
	 * @param monitor
	 * @param source
	 * @param file
	 * @throws CoreException
	 */
    private void writeTextFile(IProgressMonitor monitor, StringBuilder source, IFile file) throws CoreException {
        file.create(new ByteArrayInputStream(source.toString().getBytes()), true, new SubProgressMonitor(monitor, 5));
    }

    /**
	 * @return the estimated number of steps
	 */
    private int getNumberOfSteps() {
        final int PLUGIN_STEPS = 70;
        final int FRAGMENT_STEPS = 60;
        int steps = 0;
        if (settings.isPluginProjectSelected()) steps += PLUGIN_STEPS;
        if (settings.isDocPluginProjectSelected()) steps += PLUGIN_STEPS;
        if (settings.isWin32FragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isWin64IAFragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isWin64x86FragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isLinux32FragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isLinux64IAFragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isLinux64x86FragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isDarwin32FragmentSelected()) steps += FRAGMENT_STEPS;
        if (settings.isDarwin64FragmentSelected()) steps += FRAGMENT_STEPS;
        return steps;
    }

    /**
	 * Reads an archive file into memory, guessing its type according to its extension.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
    private Map<String, byte[]> readArchiveFile(String filename) throws IOException {
        if (filename.toLowerCase().endsWith(".zip")) {
            return readZIPFile(filename);
        } else if (filename.toLowerCase().endsWith(".tgz")) {
            return readTGZFile(filename);
        } else if (filename.toLowerCase().endsWith(".tar.gz")) {
            return readTGZFile(filename);
        } else {
            throw new UnsupportedOperationException(MessageFormat.format(Messages.ProjectGenerator_UnknownFileTypeMessage, filename));
        }
    }

    /**
	 * Reads a .tgz or .tar.gz file into memory.
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
    private Map<String, byte[]> readTGZFile(String filename) throws IOException {
        HashMap<String, byte[]> result = new HashMap<String, byte[]>();
        TarInputStream tin = new TarInputStream(new GZIPInputStream(new FileInputStream(new File(filename))));
        TarEntry tarEntry = tin.getNextEntry();
        while (tarEntry != null) {
            if (!tarEntry.isDirectory()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                tin.copyEntryContents(os);
                os.close();
                result.put(tarEntry.getName(), os.toByteArray());
            }
            tarEntry = tin.getNextEntry();
        }
        tin.close();
        return result;
    }

    /**
	 * Reads a .zip file into memory.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
    private Map<String, byte[]> readZIPFile(String filename) throws IOException {
        HashMap<String, byte[]> result = new HashMap<String, byte[]>();
        byte[] buf = new byte[32 * 1024];
        ZipFile file = new ZipFile(new File(filename));
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            InputStream is = file.getInputStream(entry);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            while (true) {
                int numRead = is.read(buf, 0, buf.length);
                if (numRead == -1) {
                    break;
                }
                os.write(buf, 0, numRead);
            }
            is.close();
            os.close();
            result.put(entry.getName(), os.toByteArray());
        }
        file.close();
        return result;
    }
}
