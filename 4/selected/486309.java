package mytest2.wizards;

import hello.StringConstants;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (java).
 */
@SuppressWarnings("restriction")
public class SampleNewWizardPage extends WizardPage {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\s*package\\s+([^\\s;#]+);?", Pattern.DOTALL);

    private static final Pattern IMPORT_PATTERN = Pattern.compile("\\n\\s*import\\s+([^\\s;#]+);?", Pattern.DOTALL);

    private Text containerText;

    private Text fileText;

    private ISelection selection;

    private String packageName;

    private String className;

    private IJavaProject javaProject;

    private IPath containerPath;

    /**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
    public SampleNewWizardPage(ISelection selection) {
        super("wizardPage");
        setTitle("drools unit test File");
        setDescription("This wizard creates a new file with *.java extension that can be used for testing drl files.");
        this.selection = selection;
    }

    /**
	 * @see IDialogPage#createControl(Composite)
	 */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        Label label = new Label(container, SWT.NULL);
        label.setText("&Container:");
        containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        containerText.setLayoutData(gd);
        containerText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                containerDialogChanged();
            }
        });
        Button button = new Button(container, SWT.PUSH);
        button.setText("Browse...");
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });
        label = new Label(container, SWT.NULL);
        label.setText("&File name:");
        fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        fileText.setLayoutData(gd);
        fileText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        initialize();
        dialogChanged();
        setControl(container);
    }

    public void populateDRLElements(String ruleFileContents) {
        Matcher matcher = PACKAGE_PATTERN.matcher(ruleFileContents);
        int startChar = 0;
        int endChar = 0;
        if (matcher.find()) {
            packageName = matcher.group(1);
            System.out.println(packageName);
            startChar = matcher.start(1);
            endChar = matcher.end(1);
        }
        matcher = IMPORT_PATTERN.matcher(ruleFileContents);
        while (matcher.find()) {
            String importName = matcher.group(1);
        }
    }

    private static String readFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    /**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
    private void initialize() {
        IEditorPart editor = null;
        if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel.size() > 1) return;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IResource) {
                IContainer container;
                if (obj instanceof IContainer) container = (IContainer) obj; else container = ((IResource) obj).getParent();
                containerText.setText(container.getFullPath().toString());
            }
        }
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                editor = page.getActiveEditor();
                IEditorInput input = editor.getEditorInput();
                IFile activeFile = ((IFileEditorInput) input).getFile();
                IProject project = activeFile.getProject();
                try {
                    javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
                try {
                    populateDRLElements(readFile(activeFile.getRawLocation().toFile()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                System.out.println(activeFile.getLocation().lastSegment());
                className = activeFile.getLocation().lastSegment().substring(0, activeFile.getLocation().lastSegment().length() - 4) + "DRL";
                System.out.println("hello");
            }
        }
        fileText.setText(className + ".java");
    }

    /**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */
    private void handleBrowse() {
        try {
            SelectionDialog dialog = JavaUI.createPackageDialog(getShell(), javaProject, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
            dialog.setTitle("Package Selection");
            dialog.setMessage("Select Package");
            if (dialog.open() == SelectionDialog.OK) {
                Object[] types = dialog.getResult();
                PackageFragment packageFragment = ((PackageFragment) types[0]);
                containerPath = packageFragment.getCorrespondingResource().getFullPath();
                System.out.println(packageFragment.getCorrespondingResource().getFullPath());
                if (types.length == 1) {
                    containerText.setText(packageFragment.names[0]);
                }
                System.out.println(types[0]);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return;
    }

    /**
	 * Ensures that both text fields are set.
	 */
    private void dialogChanged() {
        IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
        String fileName = getFileName();
        if (getContainerName().length() == 0) {
            updateStatus("File container must be specified");
            return;
        }
        if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
            updateStatus("File container must exist");
            return;
        }
        if (!container.isAccessible()) {
            updateStatus("Project must be writable");
            return;
        }
        if (fileName.length() == 0) {
            updateStatus("File name must be specified");
            return;
        }
        if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
            updateStatus("File name must be valid");
            return;
        }
        int dotLoc = fileName.lastIndexOf('.');
        if (dotLoc != -1) {
            String ext = fileName.substring(dotLoc + 1);
            if (ext.equalsIgnoreCase("java") == false) {
                updateStatus("File extension must be \"java\"");
                return;
            }
        }
        updateStatus(null);
    }

    /**
	 * Ensures that both text fields are set.
	 */
    private void containerDialogChanged() {
        IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(getContainerPackagePath());
        String fileName = getFileName();
        if (getContainerName().length() == 0) {
            updateStatus("File container must be specified");
            return;
        }
        if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
            updateStatus("File container must exist");
            return;
        }
        if (!container.isAccessible()) {
            updateStatus("Project must be writable");
            return;
        }
        if (fileName.length() == 0) {
            updateStatus("File name must be specified");
            return;
        }
        if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
            updateStatus("File name must be valid");
            return;
        }
        int dotLoc = fileName.lastIndexOf('.');
        if (dotLoc != -1) {
            String ext = fileName.substring(dotLoc + 1);
            if (ext.equalsIgnoreCase("java") == false) {
                updateStatus("File extension must be \"java\"");
                return;
            }
        }
        updateStatus(null);
    }

    private void updateStatus(String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    public String getContainerName() {
        return containerText.getText();
    }

    public IPath getContainerPackagePath() {
        return containerPath;
    }

    public String getFileName() {
        return fileText.getText();
    }

    public InputStream openContentStream() {
        String contents = "package " + packageName + ";\n\n\n" + StringConstants.DRL_TEMPLATE_ONE + "public class " + className + " { \n" + StringConstants.DRL_TEMPLATE_TWO;
        return new ByteArrayInputStream(contents.getBytes());
    }
}
