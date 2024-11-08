package org.mobicents.eclipslee.servicecreation.popup.actions;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.mobicents.eclipslee.servicecreation.ServiceCreationPlugin;
import org.mobicents.eclipslee.util.maven.MavenProjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class AddMavenDependencyAction implements IObjectActionDelegate {

    public AddMavenDependencyAction() {
        super();
    }

    public AddMavenDependencyAction(String moduleName) {
        super();
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void run(IAction action) {
        initialize();
        if (!initialized) {
            MessageDialog.openError(new Shell(), "Error Deleting Module", getLastError());
            return;
        }
        CaptureMavenDependencyIdWizard wizard = new CaptureMavenDependencyIdWizard();
        WizardDialog dialog = new WizardDialog(new Shell(), wizard);
        dialog.create();
        dialog.open();
    }

    /**
   * Get the EventXML data object for the current selection.
   * 
   */
    private void initialize() {
        if (selection == null && selection.isEmpty()) {
            setLastError("Please select a JAIN SLEE project first.");
            return;
        }
        if (!(selection instanceof IStructuredSelection)) {
            setLastError("Please select a JAIN SLEE project first.");
            return;
        }
        IStructuredSelection ssel = (IStructuredSelection) selection;
        if (ssel.size() > 1) {
            setLastError("This plugin only supports editing of one project at a time.");
            return;
        }
        Object obj = ssel.getFirstElement();
        if (obj instanceof IFile) {
            pomFile = (IFile) obj;
            project = pomFile.getProject();
        } else {
            setLastError("Unsupported object type: " + obj.getClass().toString());
            return;
        }
        initialized = true;
        return;
    }

    /**
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    private void setLastError(String error) {
        if (error == null) {
            lastError = "Success";
        } else {
            lastError = error;
        }
    }

    private String getLastError() {
        String error = lastError;
        setLastError(null);
        return error;
    }

    private ISelection selection;

    private String lastError;

    private IProject project;

    private IFile pomFile;

    private boolean initialized = false;

    private final String TEMPLATES_SEPARATOR_STRING = "<><><><> ";

    private class CaptureMavenDependencyIdWizard extends Wizard {

        ModuleNamePage moduleNamePage;

        public void addPages() {
            moduleNamePage = new ModuleNamePage("Module Name Page");
            addPage(moduleNamePage);
        }

        MavenExecutionResult mavenResult = null;

        public void runMobicentsEclipsePlugin() {
            try {
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
                dialog.run(true, false, new IRunnableWithProgress() {

                    public void run(IProgressMonitor monitor) {
                        monitor.beginTask("Updating classpath. This may take a few seconds ...", 100);
                        mavenResult = null;
                        mavenResult = MavenProjectUtils.runMavenTask(project.getFile("pom.xml"), new String[] { "mobicents:eclipse" }, monitor);
                        monitor.done();
                    }
                });
            } catch (Exception e) {
            }
        }

        public boolean performFinish() {
            try {
                String depArtifactId = moduleNamePage.getArtifactId();
                String depGroupId = moduleNamePage.getGroupId();
                String depVersion = moduleNamePage.getVersion();
                String depScope = moduleNamePage.getScope();
                if (!depArtifactId.equals("")) {
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = reader.read(new InputStreamReader(pomFile.getContents()));
                    Dependency dependency = new Dependency();
                    dependency.setArtifactId(depArtifactId);
                    dependency.setGroupId(depGroupId.equals("") ? "${pom.groupId}" : depGroupId);
                    dependency.setVersion(depVersion.equals("") ? "${pom.version}" : depVersion);
                    if (!depScope.equals("")) {
                        dependency.setScope(depScope);
                    }
                    boolean added = MavenProjectUtils.addDependency(model, dependency);
                    if (added) {
                        MavenProjectUtils.writePomFile(model, pomFile.getLocation().toOSString());
                        if (moduleNamePage.getAddToClasspath()) {
                            IJavaProject javaProject = JavaCore.create(pomFile.getProject());
                            IClasspathEntry[] classpath = javaProject.getRawClasspath();
                            IClasspathEntry[] extendedCP = new IClasspathEntry[classpath.length + 1];
                            String path = "M2_REPO/" + depGroupId.replaceAll("\\.", "/") + "/" + depArtifactId + "/" + depVersion + "/" + depArtifactId + "-" + depVersion + ".jar";
                            extendedCP[extendedCP.length - 1] = JavaCore.newVariableEntry(new Path(path), null, null);
                            System.arraycopy(classpath, 0, extendedCP, 0, classpath.length);
                            runMobicentsEclipsePlugin();
                            if (mavenResult == null || mavenResult.hasExceptions()) {
                                javaProject.setRawClasspath(extendedCP, null);
                            }
                        }
                    }
                } else {
                    MessageDialog.openError(new Shell(), "Error Adding Dependency", "The dependency Artifact ID must be specified.");
                    return false;
                }
            } catch (Exception e) {
                MessageDialog.openError(new Shell(), "Error Adding Dependency", "Failure trying to add the new dependency, please refresh the project and try again.");
            }
            return true;
        }
    }

    private class ModuleNamePage extends WizardPage {

        Text depGroupId;

        Text depArtifactId;

        Text depVersion;

        Combo depScope;

        Button depToClasspath;

        protected ModuleNamePage(String pageName) {
            super(pageName);
            setTitle("Maven Dependency");
            setDescription("Please specify the Maven dependency to be added");
            initialize();
        }

        public String getArtifactId() {
            return depArtifactId.getText();
        }

        public String getGroupId() {
            return depGroupId.getText();
        }

        public String getVersion() {
            return depVersion.getText();
        }

        public String getScope() {
            return depScope.getText();
        }

        public boolean getAddToClasspath() {
            return depToClasspath.getSelection();
        }

        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            composite.setLayout(layout);
            setControl(composite);
            VerifyListener vl = new VerifyListener() {

                public void verifyText(VerifyEvent event) {
                    event.doit = true;
                    char myChar = event.character;
                    String text = ((Text) event.widget).getText();
                    if (myChar == ' ' && text.length() == 0) event.doit = false;
                }
            };
            parseDependenciesFile(composite);
            new Label(composite, SWT.NONE);
            new Label(composite, SWT.NONE);
            new Label(composite, SWT.NONE).setText("Dependency Group Id");
            depGroupId = new Text(composite, SWT.BORDER | SWT.SINGLE);
            depGroupId.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
            depGroupId.addVerifyListener(vl);
            new Label(composite, SWT.NONE).setText("Dependency Artifact Id");
            depArtifactId = new Text(composite, SWT.BORDER | SWT.SINGLE);
            depArtifactId.addVerifyListener(vl);
            depArtifactId.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
            new Label(composite, SWT.NONE).setText("Dependency Version");
            depVersion = new Text(composite, SWT.BORDER | SWT.SINGLE);
            depVersion.addVerifyListener(vl);
            depVersion.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
            new Label(composite, SWT.NONE).setText("Dependency Scope");
            depScope = new Combo(composite, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
            depScope.add("compile");
            depScope.add("provided");
            depScope.add("runtime");
            depScope.add("test");
            depScope.add("system");
            depScope.add("import");
            depScope.select(0);
            new Label(composite, SWT.NONE).setText("Add to classpath?");
            depToClasspath = new Button(composite, SWT.CHECK | SWT.BORDER);
            depToClasspath.setSelection(true);
            depToClasspath.setText("");
        }

        private void parseDependenciesFile(Composite composite) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                URL url = FileLocator.find(ServiceCreationPlugin.getDefault().getBundle(), new Path("/eclipslee-maven-dependencies.xml"), null);
                URL furl = FileLocator.toFileURL(url);
                Document doc = db.parse(furl.openStream());
                NodeList compNodeList = doc.getDocumentElement().getElementsByTagName("sbb");
                if (compNodeList.getLength() != 1) {
                } else {
                    NodeList elemChilds = compNodeList.item(0).getChildNodes();
                    new Label(composite, SWT.NONE).setText("Components");
                    final Combo componentTemplatesCombo = new Combo(composite, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
                    componentTemplatesCombo.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
                    final HashMap<String, String[]> descToIds = new HashMap<String, String[]>();
                    for (int i = 0; i < elemChilds.getLength(); i++) {
                        if (elemChilds.item(i) instanceof Element) {
                            Element e = (Element) elemChilds.item(i);
                            componentTemplatesCombo.add(TEMPLATES_SEPARATOR_STRING + (e.hasAttribute("description") ? e.getAttribute("description") : e.getNodeName()) + TEMPLATES_SEPARATOR_STRING);
                            componentTemplatesCombo.addModifyListener(new ModifyListener() {

                                public void modifyText(ModifyEvent event) {
                                    String selected = componentTemplatesCombo.getItem(componentTemplatesCombo.getSelectionIndex());
                                    if (selected.startsWith(TEMPLATES_SEPARATOR_STRING)) {
                                        componentTemplatesCombo.pack();
                                    } else {
                                        String[] values = descToIds.get(componentTemplatesCombo.getItem(componentTemplatesCombo.getSelectionIndex()));
                                        depGroupId.setText(values[0]);
                                        depArtifactId.setText(values[1]);
                                        depVersion.setText(values[2]);
                                    }
                                }
                            });
                            NodeList dependencies = e.getChildNodes();
                            for (int j = 0; j < dependencies.getLength(); j++) {
                                if (dependencies.item(j) instanceof Element) {
                                    Element depElem = (Element) dependencies.item(j);
                                    String depGroupId = depElem.getElementsByTagName("groupId").item(0).getTextContent();
                                    String depArtifactId = depElem.getElementsByTagName("artifactId").item(0).getTextContent();
                                    String depVersion = depElem.getElementsByTagName("version").item(0).getTextContent();
                                    String depDesc = depElem.getElementsByTagName("description").getLength() >= 1 ? depElem.getElementsByTagName("description").item(0).getTextContent() : depGroupId + " : " + depArtifactId + " : " + depVersion;
                                    componentTemplatesCombo.add(depDesc);
                                    descToIds.put(depDesc, new String[] { depGroupId, depArtifactId, depVersion });
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
