package org.nexopenframework.ide.eclipse.ui.dialogs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerCallback;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerTemplate;
import org.nexopenframework.ide.eclipse.commons.xml.XMLUtils;
import org.nexopenframework.ide.eclipse.ui.NexOpenUIActivator;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p> {@link PropertyPage} for Tomcat 5.0.x, 5.5.x, 6.0.x and higher configuration.
 * It follows the NexOpen philosophy of RAD</p>
 * 
 *  @see org.eclipse.ui.IWorkbenchPropertyPage
 * @see org.eclipse.ui.dialogs.PropertyPage
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class TomcatPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    /**where to find the Tomcat Home*/
    private StringDialogField tomcatHome;

    /**Folder of Tomcat location*/
    private SelectionButtonDialogField fBrowseFolder;

    /**which cfg is choosen 5.0.x or 5.5.x and higher*/
    private Button configTomcat55;

    /**The NexOpen project*/
    protected IProject project;

    /**inner tabs for easy configuration*/
    private List<TabControl> controls = new ArrayList<TabControl>();

    /**
	 * <p>Default constructor. It adds the tab controls related 
	 * to configuration of Tomcat</p>
	 */
    public TomcatPropertyPage() {
        controls.add(new DataSourceTabControl());
    }

    @Override
    public void createControl(final Composite parent) {
        this.setDescription("Configuration of properties for Tomcat 5.0.x or 5.5.x and higher. \n" + "Please, select the home location for easy deployment of configuration " + "by NexOpen Framework");
        super.createControl(parent);
    }

    @Override
    public boolean performOk() {
        this.setPropertyValue("tomcat.home", this.tomcatHome.getText());
        this.setPropertyValue("tomcat55.cfg", Boolean.toString(this.configTomcat55.getSelection()));
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                final IFile pom = TomcatPropertyPage.this.getProject().getFile(new Path("pom.xml"));
                if (!pom.exists()) {
                    Logger.log(Logger.INFO, "In Tomcat Property page we have not found the pom.xml " + " in the given project");
                } else {
                    final IFile profiles = TomcatPropertyPage.this.getProject().getFile(new Path("profiles.xml"));
                    final boolean existsProfiles = profiles.exists();
                    ContentHandlerTemplate.handle(pom, new ContentHandlerCallback() {

                        @SuppressWarnings("unchecked")
                        public void processHandle(final Document doc) {
                            final Element root = doc.getDocumentElement();
                            final Element etomcatHome = XMLUtils.getChildElementByTagName(root, "TomcatHome");
                            if (etomcatHome != null) {
                                final String value = etomcatHome.getTextContent();
                                if (existsProfiles && value.startsWith("$")) {
                                    final String new_value = value.replace("${", "").replace("}", "");
                                    ContentHandlerTemplate.handle(profiles, new ContentHandlerCallback() {

                                        public void processHandle(final Document doc) {
                                            final Element root = doc.getDocumentElement();
                                            final Element etomcatHome = XMLUtils.getChildElementByTagName(root, new_value);
                                            etomcatHome.setTextContent(tomcatHome.getText());
                                        }
                                    });
                                } else {
                                    etomcatHome.setTextContent(tomcatHome.getText());
                                }
                            }
                            final Element dependencies = XMLUtils.getChildElementByTagName(root, "dependencies");
                            final List<Element> l_dependencies = XMLUtils.getChildElementsByTagName(dependencies, "dependency");
                            for (final Element elem : l_dependencies) {
                                final Element artifactId = XMLUtils.getChildElementByTagName(elem, "artifactId");
                                if ("geronimo-jta_1.0.1B_spec".equals(artifactId.getTextContent())) {
                                    final Element scope = XMLUtils.getChildElementByTagName(elem, "scope");
                                    scope.setTextContent("compile");
                                    break;
                                }
                            }
                            boolean found = false;
                            for (final Element elem : l_dependencies) {
                                final Element artifactId = XMLUtils.getChildElementByTagName(elem, "artifactId");
                                if ("commons-collections".equals(artifactId.getTextContent())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                createCommonsCollections(doc, dependencies);
                            }
                            if (tomcatHome.getText().indexOf("apache-tomcat-6.0.") > -1 || tomcatHome.getText().indexOf("6.") > -1) {
                                final Element profiles = XMLUtils.getChildElementByTagName(root, "profiles");
                                final List<Element> l_profiles = XMLUtils.getChildElementsByTagName(profiles, "profile");
                                for (final Element elem : l_profiles) {
                                    final Element id = XMLUtils.getChildElementByTagName(elem, "id");
                                    if ("development".equals(id.getTextContent())) {
                                        final Element jclScope = XMLUtils.getChildElementByTagName(elem, "jclScope");
                                        if ("provided".equals(jclScope.getTextContent())) {
                                            jclScope.setTextContent("compile");
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        private void createCommonsCollections(final Document doc, final Element dependencies) {
                            final Element dependency = doc.createElement("dependency");
                            Element groupId = doc.createElement("groupId");
                            groupId.setTextContent("commons-collections");
                            Element artifactId = doc.createElement("artifactId");
                            artifactId.setTextContent("commons-collections");
                            Element version = doc.createElement("version");
                            version.setTextContent("2.1.1");
                            dependency.appendChild(groupId);
                            dependency.appendChild(artifactId);
                            dependency.appendChild(version);
                            dependencies.appendChild(dependency);
                        }
                    });
                }
                for (final TabControl control : controls) {
                    control.performOk(TomcatPropertyPage.this.getProject(), TomcatPropertyPage.this);
                }
            }
        });
        return super.performOk();
    }

    /**
	 * <p></p>
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    protected Control createContents(final Composite parent) {
        this.createTomcatBrowser(parent);
        final String selected = getPropertyValue("tomcat55.cfg", "true");
        this.configTomcat55 = createTomcat55Button(parent, Boolean.parseBoolean(selected), "Configuration for Tomcat 5.5.x or higher");
        this.configTomcat55.setToolTipText("Enabling configuration for Tomcat 5.0.x or Tomcat 5.5.x or higher");
        final Composite panel = new Composite(parent, SWT.FILL);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        panel.setLayout(layout);
        TabFolder folder = new TabFolder(panel, SWT.NULL);
        folder.setLayoutData(new GridData(GridData.FILL_BOTH));
        folder.setFont(parent.getFont());
        for (TabControl control : controls) {
            control.createTab(folder, this);
        }
        return panel;
    }

    /**
	 * @param parent
	 * @param panel
	 */
    protected void createTomcatBrowser(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NONE);
        {
            GridLayout layout = new GridLayout();
            layout.numColumns = 3;
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            panel.setLayout(layout);
        }
        final PixelConverter converter = new PixelConverter(panel);
        tomcatHome = new StringDialogField();
        this.tomcatHome.setLabelText("Home Location");
        final StringBuilder sb = new StringBuilder("");
        IFile pom = this.getProject().getFile(new Path("pom.xml"));
        if (pom.exists()) {
            ContentHandlerTemplate.read(pom, new ContentHandlerCallback() {

                public void processHandle(Document doc) {
                    final Element root = doc.getDocumentElement();
                    final Element wlHome = XMLUtils.getChildElementByTagName(root, "TomcatHome");
                    if (wlHome != null) {
                        final IFile profiles = TomcatPropertyPage.this.getProject().getFile(new Path("profiles.xml"));
                        final boolean existsProfiles = profiles.exists();
                        final String value = wlHome.getTextContent();
                        if (existsProfiles && value.startsWith("${")) {
                            final String new_value = value.replace("${", "").replace("}", "");
                            ContentHandlerTemplate.read(profiles, new ContentHandlerCallback() {

                                @SuppressWarnings("unchecked")
                                public void processHandle(Document doc) {
                                    final Element root = doc.getDocumentElement();
                                    final Element tomcatHome = XMLUtils.getChildElementByTagName(root, new_value);
                                    sb.append(tomcatHome.getTextContent());
                                }
                            });
                        } else {
                            sb.append(value);
                        }
                    }
                }
            });
        } else {
            Logger.log(Logger.INFO, "In Tomcat Property page we have not found the pom.xml " + " in the given project");
        }
        String strTomcatHome = this.getPropertyValue("tomcat.home", sb.toString());
        this.tomcatHome.setText(strTomcatHome != null ? strTomcatHome : "");
        this.tomcatHome.doFillIntoGrid(panel, 2);
        LayoutUtil.setWidthHint(tomcatHome.getTextControl(null), converter.convertWidthInCharsToPixels(43));
        LayoutUtil.setHorizontalGrabbing(tomcatHome.getTextControl(null));
        IDialogFieldListener adapter = new IDialogFieldListener() {

            public void dialogFieldChanged(DialogField field) {
                if (field == fBrowseFolder) {
                    String url = chooseTomcatFolder("Tomcat Home Location Selection", "Select a Tomcat Home Location:");
                    if (url != null) {
                        tomcatHome.setText(url);
                        TomcatPropertyPage.this.setErrorMessage(null);
                    } else {
                        tomcatHome.setText("");
                        TomcatPropertyPage.this.setErrorMessage("Not a Tomcat Home location");
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
	 * @param property
	 * @return
	 */
    public String getPropertyValue(final String property) {
        final IResource resource = (IResource) this.getElement().getAdapter(IResource.class);
        try {
            final String value = resource.getPersistentProperty(new QualifiedName(NexOpenUIActivator.PLUGIN_ID, property));
            return value;
        } catch (CoreException e) {
            return e.getMessage();
        }
    }

    /**
	 * 
	 * @see #getPropertyValue(String)
	 * @param property
	 * @param defaultValue
	 * @return
	 */
    public String getPropertyValue(final String property, final String defaultValue) {
        String value = getPropertyValue(property);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
	 * @param property
	 * @param value
	 */
    public void setPropertyValue(final String property, final String value) {
        final IResource resource = (IResource) this.getElement().getAdapter(IResource.class);
        try {
            resource.setPersistentProperty(new QualifiedName(NexOpenUIActivator.PLUGIN_ID, property), value);
        } catch (CoreException e) {
        }
    }

    /**
	 * @return
	 */
    protected IProject getProject() {
        if (project == null) {
            Object element = getElement();
            if (element == null) {
                return null;
            }
            if (element instanceof IProject) {
                project = (IProject) element;
                return project;
            }
            return null;
        }
        return project;
    }

    /**
	 * @param parent
	 * @param selected
	 * @param text
	 * @return
	 */
    protected Button createTomcat55Button(final Composite parent, final boolean selected, final String text) {
        Composite panel_conf = new Composite(parent, SWT.FILL);
        GridLayout layout_conf = new GridLayout();
        layout_conf.numColumns = 2;
        layout_conf.marginHeight = 0;
        layout_conf.marginWidth = 0;
        panel_conf.setLayout(layout_conf);
        Button tomcat55Cfg = new Button(panel_conf, SWT.CHECK | SWT.LEFT);
        tomcat55Cfg.setSelection(selected);
        tomcat55Cfg.setFont(parent.getFont());
        Label autodeploy = new Label(panel_conf, SWT.LEFT);
        autodeploy.setFont(parent.getFont());
        autodeploy.setText(text);
        return tomcat55Cfg;
    }

    void processTomcatCfg(final String drivername, final String url, final String user, final String password) {
        if (configTomcat55.getSelection()) {
            processTomcat55x(drivername, url, user, password);
        } else {
            processTomcat50x(drivername, url, user, password);
        }
    }

    /**
	 * @param drivername
	 * @param url
	 * @param user
	 * @param password
	 */
    private void processTomcat50x(final String drivername, final String url, final String user, final String password) {
        final IFile ctx = this.project.getFile("web/src/main/webapp/META-INF/context.xml");
        if (ctx.exists()) {
            ContentHandlerTemplate.handle(ctx, new ContentHandlerCallback() {

                @SuppressWarnings("unchecked")
                public void processHandle(final Document doc) {
                    final Element ectx = doc.getDocumentElement();
                    Element resource = XMLUtils.getChildElementByTagName(ectx, "Resource");
                    if (resource != null && resource.getAttributeNode("maxActive") != null) {
                        ectx.removeChild(resource);
                        resource = null;
                    }
                    Element resourceParams = XMLUtils.getChildElementByTagName(ectx, "ResourceParams");
                    if (resourceParams != null) {
                        final List<Element> parameters = XMLUtils.getChildElementsByTagName(resourceParams, "parameter");
                        for (final Element param : parameters) {
                            final Element name = XMLUtils.getChildElementByTagName(param, "name");
                            String value = null;
                            final String txt_name = name.getTextContent();
                            if (txt_name.equals("driverClassName")) {
                                value = drivername;
                            } else if (txt_name.equals("url")) {
                                value = url;
                            } else if (txt_name.equals("username")) {
                                value = user;
                            } else if (txt_name.equals("password")) {
                                value = password;
                            }
                            if (value != null) {
                                final Element e_value = XMLUtils.getChildElementByTagName(param, "value");
                                e_value.setTextContent(value);
                            }
                        }
                    } else {
                        final Comment comment = doc.createComment("Resource for Tomcat 5.0.x");
                        resource = doc.createElement("Resource");
                        resource.setAttribute("name", "jdbc/" + project.getName() + "DS");
                        resource.setAttribute("auth", "Container");
                        resource.setAttribute("type", "javax.sql.DataSource");
                        resource.setAttribute("description", "javax.sql.DataSource for " + project.getName());
                        resourceParams = doc.createElement("ResourceParams");
                        resourceParams.setAttribute("name", "jdbc/" + project.getName() + "DS");
                        createParam(doc, resourceParams, "driverClassName", drivername);
                        createParam(doc, resourceParams, "url", url);
                        createParam(doc, resourceParams, "username", user);
                        createParam(doc, resourceParams, "password", password);
                        ectx.appendChild(comment);
                        ectx.appendChild(resource);
                        ectx.appendChild(resourceParams);
                    }
                }

                protected void createParam(final Document doc, final Element resourceParams, final String name, final String value) {
                    final Element param = doc.createElement("parameter");
                    final Element e_name = doc.createElement("name");
                    e_name.setTextContent(name);
                    final Element e_value = doc.createElement("value");
                    e_value.setTextContent(value);
                    param.appendChild(e_name);
                    param.appendChild(e_value);
                    resourceParams.appendChild(param);
                }
            });
            copyContext(ctx);
        }
    }

    /**
	 * @param ctx
	 */
    private void copyContext(final IFile ctx) {
        try {
            final IFile dev_ctx = this.project.getFile("src/main/resources/" + project.getName() + ".xml");
            final InputStream in = ctx.getContents();
            if (dev_ctx.exists()) {
                dev_ctx.setContents(in, true, false, null);
            } else {
                if (Logger.getLog().isInfoEnabled()) {
                    Logger.getLog().info("Create a new file at ::" + dev_ctx.getLocationURI());
                }
                dev_ctx.create(in, true, null);
            }
            final IFolder webapp = this.project.getFolder("web/src/main/webapp");
            ContentHandlerTemplate.handle(dev_ctx, new ContentHandlerCallback() {

                public void processHandle(final Document doc) {
                    final Element ctx = doc.getDocumentElement();
                    ctx.getAttributeNode("docBase").setValue(webapp.getLocation().toString());
                }
            });
        } catch (final CoreException e) {
            throw new RuntimeException("Detected a CoreException at copyContext method", e);
        }
    }

    /**
	 * @param drivername
	 * @param url
	 * @param user
	 * @param password
	 */
    private void processTomcat55x(final String drivername, final String url, final String user, final String password) {
        final IFile ctx = this.project.getFile("web/src/main/webapp/META-INF/context.xml");
        if (ctx.exists()) {
            ContentHandlerTemplate.handle(ctx, new ContentHandlerCallback() {

                public void processHandle(final Document doc) {
                    final Element ectx = doc.getDocumentElement();
                    Element resource = XMLUtils.getChildElementByTagName(ectx, "Resource");
                    Element resourceParams = XMLUtils.getChildElementByTagName(ectx, "ResourceParams");
                    if (resourceParams != null) {
                        ectx.removeChild(resource);
                        ectx.removeChild(resourceParams);
                        resource = null;
                    }
                    if (resource != null) {
                        resource.getAttributeNode("url").setValue(url);
                        resource.getAttributeNode("username").setValue(user);
                        resource.getAttributeNode("password").setValue(password);
                        resource.getAttributeNode("driverClassName").setValue(drivername);
                    } else {
                        final Comment comment = doc.createComment("Resource for Tomcat 5.5.x and higher");
                        resource = doc.createElement("Resource");
                        resource.setAttribute("name", "jdbc/" + project.getName() + "DS");
                        resource.setAttribute("auth", "Container");
                        resource.setAttribute("type", "javax.sql.DataSource");
                        resource.setAttribute("maxActive", "100");
                        resource.setAttribute("maxIdle", "30");
                        resource.setAttribute("maxWait", "10000");
                        resource.setAttribute("username", user);
                        resource.setAttribute("password", password);
                        resource.setAttribute("driverClassName", drivername);
                        resource.setAttribute("url", url);
                        ectx.appendChild(comment);
                        ectx.appendChild(resource);
                    }
                }
            });
            copyContext(ctx);
        }
    }

    /**
	 * @param text
	 * @param message
	 * @param domain
	 * @return
	 */
    private String chooseTomcatFolder(final String text, final String message) {
        String initPath = "";
        final DirectoryDialog dialog = new DirectoryDialog(this.getShell());
        dialog.setText(text);
        dialog.setMessage(message);
        dialog.setFilterPath(initPath);
        final String result = dialog.open();
        if (result != null) {
            return result;
        }
        return null;
    }

    public static interface TabControl {

        void createTab(TabFolder folder, TomcatPropertyPage page);

        void performOk(IProject project, TomcatPropertyPage page);
    }

    public static class DataSourceTabControl implements TabControl {

        static final String[] DRIVERS = { "oracle.jdbc.driver.OracleDriver", "com.mysql.jdbc.Driver" };

        static final Map<String, String> URLS = new HashMap<String, String>();

        static final Map<String, String> LIBRARIES = new HashMap<String, String>();

        static {
            URLS.put(DRIVERS[0], "jdbc:oracle:thin:@hostname:1521:serviceName");
            URLS.put(DRIVERS[1], "jdbc:mysql://hostname:3306/catalogName?autoReconnect=true");
            LIBRARIES.put(DRIVERS[0], "ojdbc14.jar");
            LIBRARIES.put(DRIVERS[1], "<mysql_driver>");
        }

        /***/
        Combo c_drivers;

        Text url;

        Text username;

        Text password;

        public void createTab(final TabFolder folder, final TomcatPropertyPage page) {
            final TabItem datasource = new TabItem(folder, SWT.NULL);
            datasource.setText("DataSource");
            datasource.setToolTipText("Configuration of a javax.sql.DataSource for Tomcat Web Container");
            final Composite composite = new Composite(folder, SWT.NONE);
            composite.setFont(folder.getFont());
            {
                final GridLayout layout = new GridLayout();
                layout.marginWidth = 0;
                layout.marginHeight = 0;
                layout.numColumns = 2;
                composite.setLayout(layout);
            }
            final Label driver_label = new Label(composite, SWT.NULL);
            driver_label.setText("JDBC Driver");
            c_drivers = new Combo(composite, SWT.NULL);
            c_drivers.setItems(DRIVERS);
            c_drivers.select(Arrays.asList(DRIVERS).indexOf(page.getPropertyValue("tomcat.jdbc.driver", DRIVERS[1])));
            final Label url_label = new Label(composite, SWT.NULL);
            url_label.setText("JDBC URL");
            url = new Text(composite, SWT.BORDER | SWT.LEFT);
            url.setText(page.getPropertyValue("tomcat.jdbc.url", URLS.get(DRIVERS[1])));
            url.setTextLimit(150);
            url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            c_drivers.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                }

                public void widgetSelected(SelectionEvent e) {
                    final String selected = ((Combo) e.getSource()).getText();
                    url.setText(URLS.get(selected));
                }
            });
            final Label user_label = new Label(composite, SWT.NULL);
            user_label.setText("User");
            username = new Text(composite, SWT.BORDER | SWT.LEFT);
            username.setText(page.getPropertyValue("tomcat.jdbc.user", "username"));
            username.setTextLimit(150);
            username.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Label pwd_label = new Label(composite, SWT.NULL);
            pwd_label.setText("Password");
            password = new Text(composite, SWT.BORDER | SWT.LEFT);
            password.setText(page.getPropertyValue("tomcat.jdbc.password", "password"));
            password.setTextLimit(150);
            password.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            datasource.setControl(composite);
        }

        public void performOk(final IProject project, final TomcatPropertyPage page) {
            page.setPropertyValue("tomcat.jdbc.driver", c_drivers.getText());
            page.setPropertyValue("tomcat.jdbc.url", url.getText());
            page.setPropertyValue("tomcat.jdbc.user", username.getText());
            page.setPropertyValue("tomcat.jdbc.password", password.getText());
            File lib = new File(page.tomcatHome.getText(), "lib");
            if (!lib.exists()) {
                lib = new File(new File(page.tomcatHome.getText(), "common"), "lib");
                if (!lib.exists()) {
                    Logger.log(Logger.ERROR, "Not properly location of Tomcat Home at :: " + lib);
                    throw new IllegalStateException("Not properly location of Tomcat Home");
                }
            }
            final File conf = new File(page.tomcatHome.getText(), "conf/Catalina/localhost");
            if (!conf.exists()) {
                final boolean create = NexOpenUIActivator.getDefault().getTomcatConfProperty();
                if (create) {
                    if (Logger.getLog().isDebugEnabled()) {
                        Logger.getLog().debug("Create directory " + conf);
                    }
                    try {
                        conf.mkdirs();
                    } catch (final SecurityException se) {
                        Logger.getLog().error("Retrieved a Security exception creating " + conf, se);
                        Logger.log(Logger.ERROR, "Not created " + conf + " directory. Not enough privilegies. Message :: " + se.getMessage());
                    }
                }
            }
            String str_driverLibrary = LIBRARIES.get(c_drivers.getText());
            if ("<mysql_driver>".equals(str_driverLibrary)) {
                str_driverLibrary = NexOpenUIActivator.getDefault().getMySQLDriver();
            }
            final File driverLibrary = new File(lib, str_driverLibrary);
            if (!driverLibrary.exists()) {
                InputStream driver = null;
                FileOutputStream fos = null;
                try {
                    driver = AppServerPropertyPage.toInputStream(new Path("jdbc/" + str_driverLibrary));
                    fos = new FileOutputStream(driverLibrary);
                    IOUtils.copy(driver, fos);
                } catch (IOException e) {
                    Logger.log(Logger.ERROR, "Could not copy the driver jar file to Tomcat", e);
                } finally {
                    try {
                        if (driver != null) {
                            driver.close();
                            driver = null;
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
            page.processTomcatCfg(c_drivers.getText(), url.getText(), username.getText(), password.getText());
        }
    }
}
