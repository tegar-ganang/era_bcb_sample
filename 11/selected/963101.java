package com.luxoft.fitpro.plugin.wizards.newproject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import com.luxoft.fitpro.plugin.config.FitPlugin;
import com.luxoft.fitpro.plugin.help.ContextHelp;
import com.luxoft.fitpro.plugin.properties.DefaultFolderProperties;

/**
 * FitProjectWizardPage is a page of the new-project-wizard. It is used to add new FITpro-projects to the Eclipse
 * workspace.
 */
public class FitProjectWizardPage extends WizardPage {

    private Text projectName;

    private Text fitResults;

    private Text fitTests;

    private Text fitFixtures;

    private Button includeExamplesCheck;

    private static final String FIT_LIBRARY = "fitlibraryRunner.jar";

    private static final String FIT_LIBRARY_PATH = "lib/" + FIT_LIBRARY;

    private static final String[] FIT_SAMPLES = { "Discount.java", "ColumnFixture-Discount.fit", "CalculateDiscount.java", "Discount.suite" };

    private static final String DEFAULT_SOURCE_FOLDER = "src";

    private static final String DEFAULT_OUTPUT_FOLDER = "bin";

    protected FitProjectWizardPage(String pageName) {
        super(pageName);
        setTitle("Create a Java Project with FITpro");
        setDescription("Create a Java project that uses Fit.");
    }

    private static Text createField(String title, Composite container, ModifyListener listener) {
        Label label = new Label(container, SWT.NULL);
        label.setText(title);
        Text text = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        text.setLayoutData(gd);
        text.addModifyListener(listener);
        return text;
    }

    protected void dialogChanged() {
        if (!isValidFolder(projectName.getText())) {
            updateStatus(null);
            setPageComplete(false);
            return;
        } else {
            setPageComplete(true);
        }
        if (!isValidFolder(fitResults.getText())) {
            updateStatus("Fit Results must be set to a folder which exists in the project.");
            return;
        }
        if (!isValidFolder(fitTests.getText())) {
            updateStatus("Fit Tests must be set to a folder which exists in the project");
            return;
        }
        if (!isValidPackage(fitFixtures.getText())) {
            updateStatus("Fit Fixtures must be set to a folder which exists in the project");
            return;
        }
        if (ResourcesPlugin.getWorkspace().getRoot().getProject(projectName.getText()).exists()) {
            updateStatus("A project with this name already exists.");
            return;
        }
        updateStatus(null);
    }

    private static boolean isValidFolder(String name) {
        return (new Path("")).isValidSegment(name);
    }

    private static boolean isValidPackage(String name) {
        if (name.length() == 0 || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        layout.verticalSpacing = 9;
        ModifyListener listener = new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        };
        projectName = createField("Project name:", container, listener);
        Group group = new Group(container, SWT.NULL);
        group.setText("Project folders");
        group.setLayout(layout);
        fitResults = createField("Fit Results:", group, listener);
        fitTests = createField("Fit Tests:", group, listener);
        fitFixtures = createField("Fixtures:", group, listener);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        group = new Group(container, SWT.NULL);
        group.setText("Project options");
        group.setLayout(layout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        includeExamplesCheck = new Button(group, SWT.CHECK);
        includeExamplesCheck.setText("Include examples");
        includeExamplesCheck.setSelection(true);
        setDefaults();
        setControl(container);
        projectName.setFocus();
        ContextHelp.setHelp(container, "new_fit_project_wizard_context");
    }

    private void setDefaults() {
        fitResults.setText("FitResults");
        fitTests.setText("FitTests");
        fitFixtures.setText("Fixtures");
    }

    private static void handleError(Shell shell, IProject project, String message) {
        MessageDialog.openError(shell, "Java Project with FITpro", message);
        if (project.exists()) {
            try {
                project.delete(true, true, null);
            } catch (CoreException e) {
                MessageDialog.openError(shell, "Java Project with FITpro", "Could not clean after error");
                return;
            }
        }
    }

    public boolean finish() {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName.getText());
        try {
            project.create(null);
            project.open(null);
            IProjectDescription desc = project.getDescription();
            desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
            project.setDescription(desc, null);
            IJavaProject javaProject = JavaCore.create(project);
            IPath fitLib = project.getFullPath().append(FIT_LIBRARY);
            javaProject.setRawClasspath(createClassPathEntries(project, fitLib), null);
            copyLibrary(project);
            javaProject.setOutputLocation(createOutputFolder(project, DEFAULT_OUTPUT_FOLDER).getFullPath(), null);
            createOutputFolder(project, fitTests.getText());
            createOutputFolder(project, fitResults.getText());
            if (!DEFAULT_OUTPUT_FOLDER.equals(fitResults.getText())) {
                DefaultFolderProperties.setDefinedOutputLocation(project, fitResults.getText());
            }
            if (!DEFAULT_SOURCE_FOLDER.equals(fitFixtures.getText())) {
                DefaultFolderProperties.setDefinedSourceLocation(project, fitFixtures.getText());
            }
            if (includeExamplesCheck.getSelection()) {
                copySamples(project);
            }
        } catch (CoreException e) {
            handleError(getContainer().getShell(), project, "Could not create project:" + e.getMessage());
            return false;
        } catch (IOException e) {
            handleError(getContainer().getShell(), project, "Could not create project:" + e.getMessage());
            return false;
        }
        return true;
    }

    private IClasspathEntry[] createClassPathEntries(IProject project, IPath fitLib) throws CoreException {
        IClasspathEntry[] classpathEntries = null;
        IFolder srcFolder = createSourceFolder(project, DEFAULT_SOURCE_FOLDER);
        if (!DEFAULT_SOURCE_FOLDER.equals(fitFixtures.getText())) {
            IFolder srcCustomFolder = createSourceFolder(project, fitFixtures.getText());
            classpathEntries = new IClasspathEntry[4];
            classpathEntries[0] = JavaCore.newSourceEntry(srcFolder.getFullPath());
            classpathEntries[1] = JavaCore.newSourceEntry(srcCustomFolder.getFullPath());
            classpathEntries[2] = JavaRuntime.getDefaultJREContainerEntry();
            classpathEntries[3] = JavaCore.newLibraryEntry(fitLib, null, null);
        } else {
            classpathEntries = new IClasspathEntry[3];
            classpathEntries[0] = JavaCore.newSourceEntry(srcFolder.getFullPath());
            classpathEntries[1] = JavaRuntime.getDefaultJREContainerEntry();
            classpathEntries[2] = JavaCore.newLibraryEntry(fitLib, null, null);
        }
        return classpathEntries;
    }

    private IFolder createOutputFolder(IProject project, String path) throws CoreException {
        IFolder binFolder = project.getFolder(new Path(path));
        if (!binFolder.exists()) {
            binFolder.create(true, true, null);
        }
        return binFolder;
    }

    private IFolder createSourceFolder(IProject project, String path) throws CoreException {
        IFolder srcFolder = project.getFolder(new Path(path));
        if (!srcFolder.exists()) {
            srcFolder.create(true, true, null);
        }
        return srcFolder;
    }

    private void copyLibrary(IProject project) throws CoreException, IOException {
        doCopyFile(FIT_LIBRARY_PATH, FIT_LIBRARY, project, false);
    }

    private void copySamples(IProject project) throws CoreException, IOException {
        for (String res : FIT_SAMPLES) {
            if (res.toLowerCase().endsWith(".fit") || res.toLowerCase().endsWith(".suite")) {
                doCopyFile("samples/" + res, fitTests.getText() + "/" + res, project, true);
            } else if (res.toLowerCase().endsWith(".java")) {
                doCopyFile("samples/" + res, fitFixtures.getText() + "/" + res, project, true);
            }
        }
    }

    private void doCopyFile(String source, String dest, IProject project, boolean filter) throws IOException, CoreException {
        Bundle bundle = Platform.getBundle(FitPlugin.getDefault().getPluginId());
        URL fileURL = FileLocator.find(bundle, new Path(source), null);
        if (fileURL != null) {
            InputStream istream = (filter) ? new Filter(fileURL.openStream(), fitFixtures.getText()) : fileURL.openStream();
            project.getFile(dest).create(istream, true, null);
            istream.close();
        } else {
            throw new IOException("Could not find \"" + source + "\".");
        }
    }

    /**
     * Is used for replacing package name in sample files. $PACKAGE is replaced with actual package name.
     */
    private static class Filter extends InputStream {

        private InputStream in;

        private static final int INITIAL_BUFFER_LENGTH = 7;

        private static final String TEMPLATE = "PACKAGE";

        private byte[] buffer = new byte[INITIAL_BUFFER_LENGTH];

        private int pos = 0;

        private boolean templateRead = false;

        private String replacement;

        public Filter(InputStream in, String replacement) {
            this.in = in;
            this.replacement = replacement;
        }

        @Override
        public int read() throws IOException {
            if (templateRead) {
                if (pos < buffer.length) {
                    return buffer[pos++];
                }
                buffer = new byte[INITIAL_BUFFER_LENGTH];
                templateRead = false;
            }
            final int ch = in.read();
            if (ch == '$') {
                templateRead = true;
                if (in.read(buffer) == buffer.length) {
                    if (Arrays.equals(buffer, TEMPLATE.getBytes())) {
                        buffer = new byte[replacement.length()];
                        System.arraycopy(replacement.getBytes(), 0, buffer, 0, replacement.length());
                    }
                    pos = 1;
                    return buffer[0];
                }
            }
            return ch;
        }
    }
}
