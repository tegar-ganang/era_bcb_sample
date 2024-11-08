package org.nexopenframework.ide.eclipse.ui.dialogs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerCallback;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerTemplate;
import org.nexopenframework.ide.eclipse.commons.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p> {@link PropertyPage} for WebLogic 9.2.x and 10.x configuration.
 * It follows the NexOpen philosophy of RAD</p>
 * 
 * @see org.eclipse.ui.IWorkbenchPropertyPage
 * @see org.nexopenframework.ide.eclipse.ui.dialogs.AppServerPropertyPage
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class WLPropertyPage extends AppServerPropertyPage implements IWorkbenchPropertyPage {

    /**where to find the Bea Home*/
    private StringDialogField beaHome;

    /**where to find the Bea Home*/
    private StringDialogField domainDirectory;

    /**Folder of Bea location*/
    private SelectionButtonDialogField fBrowseFolder;

    /**Folder of Bea location*/
    private SelectionButtonDialogField fBrowseDDFolder;

    /**if copy of JCL libraries (and log4j library and xml) is enabled or not*/
    private Button enabledJCLCopy;

    /**if copy of JSTL 1.1 libraries is enabled or not*/
    private Button enabledJSTLCopy;

    /**The {@link TabControl}'s list*/
    private List<TabControl> tabControls = new ArrayList<TabControl>();

    public WLPropertyPage() {
        tabControls.add(new WLMySQLTabControl());
        tabControls.add(new WLOracleTabControl());
    }

    public boolean isEnabledJCLCopy() {
        return this.enabledJCLCopy.getSelection();
    }

    public boolean isEnabledJSTLCopy() {
        return this.enabledJSTLCopy.getSelection();
    }

    public String getDomainDirectory() {
        return this.domainDirectory.getText();
    }

    @Override
    public void createControl(final Composite parent) {
        this.setDescription("Configuration of properties for Bea WebLogic 9.x. " + "Please, select the home location and domain directory for deployment of libraries and configuration " + "by NexOpen Framework");
        super.createControl(parent);
    }

    @Override
    public boolean performOk() {
        this.setPropertyValue("bea.home", this.beaHome.getText());
        this.setPropertyValue("bea.domain", this.domainDirectory.getText());
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                saveWLHome();
                for (final TabControl control : tabControls) {
                    control.performOk(WLPropertyPage.this.getProject(), WLPropertyPage.this);
                }
                if (isEnabledJCLCopy()) {
                    final File url = new File(WLPropertyPage.this.domainDirectory.getText());
                    File lib = new File(url, "lib");
                    File log4jLibrary = new File(lib, "log4j-1.2.13.jar");
                    if (!log4jLibrary.exists()) {
                        InputStream srcFile = null;
                        FileOutputStream fos = null;
                        try {
                            srcFile = toInputStream(new Path("jcl/log4j-1.2.13.jar"));
                            fos = new FileOutputStream(log4jLibrary);
                            IOUtils.copy(srcFile, fos);
                            srcFile.close();
                            fos.flush();
                            fos.close();
                            srcFile = toInputStream(new Path("/jcl/commons-logging-1.0.4.jar"));
                            File jcl = new File(lib, "commons-logging-1.0.4.jar");
                            fos = new FileOutputStream(jcl);
                            IOUtils.copy(srcFile, fos);
                        } catch (IOException e) {
                            Logger.log(Logger.ERROR, "Could not copy JCL jars file to Bea WL", e);
                        } finally {
                            try {
                                if (srcFile != null) {
                                    srcFile.close();
                                    srcFile = null;
                                }
                                if (fos != null) {
                                    fos.flush();
                                    fos.close();
                                    fos = null;
                                }
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                if (isEnabledJSTLCopy()) {
                    File url = new File(WLPropertyPage.this.domainDirectory.getText());
                    File lib = new File(url, "lib");
                    File jstlLibrary = new File(lib, "jstl.jar");
                    if (!jstlLibrary.exists()) {
                        InputStream srcFile = null;
                        FileOutputStream fos = null;
                        try {
                            srcFile = toInputStream(new Path("jstl/jstl.jar"));
                            fos = new FileOutputStream(jstlLibrary);
                            IOUtils.copy(srcFile, fos);
                        } catch (IOException e) {
                            Logger.log(Logger.ERROR, "Could not copy the JSTL 1.1 jar file to Bea WL", e);
                        } finally {
                            try {
                                if (srcFile != null) {
                                    srcFile.close();
                                    srcFile = null;
                                }
                                if (fos != null) {
                                    fos.flush();
                                    fos.close();
                                    fos = null;
                                }
                            } catch (final IOException e) {
                                Logger.getLog().debug("I/O exception closing resources", e);
                            }
                        }
                    }
                }
            }
        });
        return super.performOk();
    }

    @Override
    public String getMySQLText() {
        return "Configuration of the MySQL DataSource for Bea WebLogic 9.x";
    }

    @Override
    public String getOracleText() {
        return "Configuration of the Oracle DataSource for Bea WebLogic 9.x";
    }

    /**
	 * <p>Saves the JBoss location to the <code>profiles.xml</code></p>
	 */
    protected void saveWLHome() {
        IFile pom = this.getProject().getFile(new Path("pom.xml"));
        if (!pom.exists()) {
            Logger.log(Logger.INFO, "In WebLogic Property page we have not found the pom.xml " + " in the given project");
            return;
        }
        ContentHandlerTemplate.handle(pom, new ContentHandlerCallback() {

            public void processHandle(final Document doc) {
                final Element root = doc.getDocumentElement();
                final Element wlHome = XMLUtils.getChildElementByTagName(root, "WLHome");
                if (wlHome != null) {
                    final IFile profiles = WLPropertyPage.this.getProject().getFile(new Path("profiles.xml"));
                    final boolean existsProfiles = profiles.exists();
                    final String value = wlHome.getTextContent();
                    if (existsProfiles && value.startsWith("$")) {
                        final String new_value = value.replace("${", "").replace("}", "");
                        ContentHandlerTemplate.handle(profiles, new ContentHandlerCallback() {

                            public void processHandle(final Document doc) {
                                final Element root = doc.getDocumentElement();
                                final Element e_wlHome = XMLUtils.getChildElementByTagName(root, new_value);
                                e_wlHome.setTextContent(beaHome.getText());
                            }
                        });
                    } else {
                        wlHome.setTextContent(beaHome.getText());
                    }
                } else {
                    Logger.log(Logger.INFO, "Not <WLHome/> found in project " + getProject().getName());
                }
                modifyPomRootDependenciesIfNecessary(root);
            }
        });
    }

    /**
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    protected Control createContents(final Composite parent) {
        this.createBeaBrowser(parent);
        this.createBeaDomain(parent);
        this.createSeparator(parent, 1);
        this.enabledJCLCopy = this.createCheckButton(parent, true, "Add logging support (Commons Logging and Log4J libraries)");
        this.enabledJSTLCopy = this.createCheckButton(parent, true, "Add JSTL 1.1 library");
        final Composite panel = new Composite(parent, SWT.FILL);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        panel.setLayout(layout);
        TabFolder folder = new TabFolder(panel, SWT.NULL);
        folder.setLayoutData(new GridData(GridData.FILL_BOTH));
        folder.setFont(parent.getFont());
        for (TabControl control : tabControls) {
            control.createTab(folder, this);
        }
        return panel;
    }

    /**
	 * @param parent
	 * @param panel
	 */
    protected void createBeaBrowser(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        {
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            panel.setLayout(layout);
        }
        PixelConverter converter = new PixelConverter(panel);
        beaHome = new StringDialogField();
        this.beaHome.setLabelText("Home Location");
        final StringBuilder sb = new StringBuilder("");
        IFile pom = this.getProject().getFile(new Path("pom.xml"));
        if (pom.exists()) {
            ContentHandlerTemplate.read(pom, new ContentHandlerCallback() {

                public void processHandle(Document doc) {
                    Element root = doc.getDocumentElement();
                    Element wlHome = XMLUtils.getChildElementByTagName(root, "WLHome");
                    if (wlHome != null) {
                        final String value = wlHome.getTextContent();
                        final IFile profiles = getProject().getFile(new Path("profiles.xml"));
                        final boolean existsProfiles = profiles.exists();
                        if (existsProfiles && value.startsWith("$")) {
                            final String new_value = value.replace("${", "").replace("}", "");
                            ContentHandlerTemplate.read(profiles, new ContentHandlerCallback() {

                                @SuppressWarnings("unchecked")
                                public void processHandle(Document doc) {
                                    final Element root = doc.getDocumentElement();
                                    final Element wlHome = XMLUtils.getChildElementByTagName(root, new_value);
                                    sb.append(wlHome.getTextContent());
                                }
                            });
                        } else {
                            sb.append(wlHome.getTextContent());
                        }
                    }
                }
            });
        } else {
            Logger.log(Logger.INFO, "In WebLogic Property page we have not found the pom.xml " + " in the given project");
        }
        String strBeaHome = this.getPropertyValue("bea.home", sb.toString());
        this.beaHome.setText(strBeaHome != null ? strBeaHome : "");
        this.beaHome.doFillIntoGrid(panel, 2);
        LayoutUtil.setWidthHint(beaHome.getTextControl(null), converter.convertWidthInCharsToPixels(43));
        LayoutUtil.setHorizontalGrabbing(beaHome.getTextControl(null));
        IDialogFieldListener adapter = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == fBrowseFolder) {
                    String url = chooseBeaWebLogicFolder("Bea WebLogic 9.x Home Location Selection", "Select a Bea WebLogic 9.x Home Location:", false);
                    if (url != null) {
                        beaHome.setText(url);
                        WLPropertyPage.this.setErrorMessage(null);
                    } else {
                        beaHome.setText("");
                        WLPropertyPage.this.setErrorMessage("Not a Bea WebLogic Home location");
                    }
                }
            }
        };
        fBrowseFolder = new SelectionButtonDialogField(SWT.PUSH);
        fBrowseFolder.setDialogFieldListener(adapter);
        fBrowseFolder.setLabelText("&Browse ...");
        fBrowseFolder.doFillIntoGrid(panel, 1);
    }

    /**
	 * @param parent
	 * @param panel
	 */
    protected void createBeaDomain(Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        {
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            panel.setLayout(layout);
        }
        PixelConverter converter = new PixelConverter(panel);
        domainDirectory = new StringDialogField();
        this.domainDirectory.setLabelText("Domain Directory");
        String strBeaHome = this.getPropertyValue("bea.domain");
        this.domainDirectory.setText(strBeaHome != null ? strBeaHome : "");
        this.domainDirectory.doFillIntoGrid(panel, 2);
        LayoutUtil.setWidthHint(domainDirectory.getTextControl(null), converter.convertWidthInCharsToPixels(43));
        LayoutUtil.setHorizontalGrabbing(domainDirectory.getTextControl(null));
        IDialogFieldListener adapter = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == fBrowseDDFolder) {
                    String url = chooseBeaWebLogicFolder("Bea WebLogic 9.x Domain Directory Selection", "Select a Bea WebLogic 9.x Domain Directory:", true);
                    if (url != null) {
                        domainDirectory.setText(url);
                        WLPropertyPage.this.setErrorMessage(null);
                    } else {
                        domainDirectory.setText("");
                        WLPropertyPage.this.setErrorMessage("Not a Bea WebLogic Domain Directory");
                    }
                }
            }
        };
        fBrowseDDFolder = new SelectionButtonDialogField(SWT.PUSH);
        fBrowseDDFolder.setDialogFieldListener(adapter);
        fBrowseDDFolder.setLabelText("&Browse ...");
        fBrowseDDFolder.doFillIntoGrid(panel, 1);
    }

    /**
	 * @param composite
	 * @param nColumns
	 */
    protected void createSeparator(Composite composite, int nColumns) {
        (new Separator(SWT.SEPARATOR | SWT.HORIZONTAL)).doFillIntoGrid(composite, nColumns, convertHeightInCharsToPixels(1));
    }

    /**
	 * @param text
	 * @param message
	 * @param domain
	 * @return
	 */
    private String chooseBeaWebLogicFolder(final String text, final String message, final boolean domain) {
        String initPath = "";
        final DirectoryDialog dialog = new DirectoryDialog(this.getShell());
        dialog.setText(text);
        dialog.setMessage(message);
        dialog.setFilterPath(initPath);
        final String result = dialog.open();
        if (result != null) {
            if (domain) {
                File url = new File(result);
                File config = new File(url, "config");
                File server = new File(url, "servers");
                File autodeploy = new File(url, "autodeploy");
                if (!config.exists() && !server.exists() && !autodeploy.exists()) {
                    return null;
                }
            }
            return result;
        }
        return null;
    }
}
