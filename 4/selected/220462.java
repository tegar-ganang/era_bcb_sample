package org.xaware.ide.xadev.richui.editor.service.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.jdom.JDOMException;
import org.xaware.ide.shared.DriverPropsPanel;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.datamodel.SQLBizDriverInfo;
import org.xaware.ide.xadev.gui.ChangeEvent;
import org.xaware.ide.xadev.gui.XAFileChooser;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.ide.xadev.gui.model.DocumentModel;
import org.xaware.ide.xadev.richui.editor.service.AbstractServiceEditorPage;
import org.xaware.ide.xadev.richui.editor.service.SummaryPageEntry;
import org.xaware.ide.xadev.richui.editor.util.RichUIEditorXmlUtil;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.FileUtils;
import org.xaware.shared.util.XAClassLoader;
import org.xaware.shared.util.XAwareConfig;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This page allows the user to define the SQL/JDBC connection information in a simplified manner
 * from the normal SQL wizard, for instance it does not provide ability to define input parameters
 * a JNDI lookup for the datasource, along with choosing an existing bizdriver to load.
 * 
 * @author tferguson
 *
 */
public class SQLSimpleConnectionPage extends AbstractServiceEditorPage implements ModifyListener, SelectionListener {

    /** Text used for the label when the drivers are not found on the class path */
    private static final String DRIVER_NOT_FOUND_TEXT = translator.getString("* Driver not found, use the Required JARs section to add driver classes");

    /** Text used for Connection Information summary page entry */
    private static final String CONNECTION_INFORMATION = translator.getString("Connection Information");

    /** Text used for Jar Information summary page entry */
    private static final String JAR_INFORMATION = translator.getString("JARs Copied");

    /** Text used for User Name summary page entry */
    private static final String USERNAME = translator.getString("User Name");

    /** Text used for value of summary page entry that has been completed*/
    private static final String COMPLETED = translator.getString("Completed");

    /** Text used for Password summary page entry */
    private static final String PASSWORD = translator.getString("Password");

    /**
     * XAware logger instance for logging.
     */
    private XAwareLogger logger = XAwareLogger.getXAwareLogger(SQLSimpleConnectionPage.class.getName());

    /** Object that stores and makes a connection to the database */
    SQLBizDriverInfo info = new SQLBizDriverInfo();

    /** Combo to hold the protocol/url for the connection */
    private Combo protocolCmb;

    /** Hashmap of url templates to drivers */
    private LinkedHashMap<String, String> driverHash;

    /** Holder the driver class for the connection */
    private Combo driverClassCmb;

    /** Text box holding the user name */
    private Text loginText;

    /** Text box holding the password */
    private Text passwordText;

    /** Panel allowing editing of properties for jdbc connection */
    private DriverPropsPanel createPropsPan;

    /** String representing the biz driver reference in the bizcomp */
    private String bizDriverRef;

    /** Table containing the list of jars used by this bizdriver */
    private Table jarTable;

    /** File chooser for adding jars */
    private XAFileChooser jarFileChooser;

    /** Flag indicating that the listeners are active */
    private boolean listenersAdded;

    /** Button that when pressed tests the connection information on the page */
    private Button testConnectionButton;

    /** Browse button to invoke the jarFileChooser */
    private Button browseButton;

    /** Button to remove a jar */
    private Button removeButton;

    /** Radion button indicating the user will define the database connection */
    private Button haveDatabase;

    /** Radion button indicating user wants to use the example bizdriver */
    private Button useSampleDatabase;

    /** Section containing the channel definition */
    private Section channelSection;

    /** Section containing the jar table and buttons to manage */
    private Section jarSection;

    /** Indicates the user has set the url/protocol and not chosen one from the drop down */
    private boolean protocolSetByUser = false;

    /** Indicates the user has set the driver class and not chosen one from the drop down */
    private boolean driverSetByUser = false;

    /** The bizdriver reference has changed from the original value */
    private boolean refChanged = false;

    /** Currently using the example database */
    private boolean usingSampleDatabase = false;

    /** Contains the bizdriver reference to be used for the example bizdriver */
    private static String SQL_EXAMPLE_BIZDRIVER = null;

    public SQLSimpleConnectionPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        SQL_EXAMPLE_BIZDRIVER = XAwareConfig.GetInstance().getExampleBizDriver("SQL");
    }

    @Override
    protected void createContent() {
        TableWrapLayout layout = new TableWrapLayout();
        form.getBody().setLayout(layout);
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = true;
        createTypeSection(form);
        createChannelSection(form);
        createJarSection(form);
        createJdbcPropertiesSection(form);
        testConnectionButton = toolkit.createButton(form.getBody(), translator.getString("Test Connection"), SWT.NONE);
        Composite instructionComposite = toolkit.createComposite(form.getBody());
        GridLayout gridLayout = new GridLayout(1, false);
        instructionComposite.setLayout(gridLayout);
        ImageHyperlink image = toolkit.createImageHyperlink(instructionComposite, SWT.CENTER);
        image.setImage(UserPrefs.getImageIconFor("ServiceWizardNext"));
        image.setText(translator.getString("Click this button to continue"));
        image.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {
                editor.setActivePage(nextId);
            }
        });
        setSelectionFromModel();
        this.populateScreen();
        toolkit.paintBordersFor(form.getBody());
    }

    /**
     * 
     * @param form
     */
    private void createTypeSection(ScrolledForm form) {
        TableWrapData td;
        Section section = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
        td = new TableWrapData(TableWrapData.FILL_GRAB);
        td.indent = 20;
        section.setLayoutData(td);
        section.setText("Connect to a relational data source");
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout(1, false));
        section.setClient(sectionClient);
        useSampleDatabase = toolkit.createButton(sectionClient, translator.getString("I need one (we will define the connection and jump you to creating the SQL statement)"), SWT.RADIO);
        useSampleDatabase.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            public void widgetSelected(SelectionEvent arg0) {
                if (useSampleDatabase.getSelection()) {
                    channelSection.setExpanded(false);
                    provideBizDriver();
                }
            }
        });
        haveDatabase = toolkit.createButton(sectionClient, translator.getString("I have one (you will select the data source JARs and define the connection details)"), SWT.RADIO);
        haveDatabase.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }

            public void widgetSelected(SelectionEvent arg0) {
                if (haveDatabase.getSelection()) {
                    channelSection.setExpanded(true);
                    refChanged = true;
                    usingSampleDatabase = true;
                    setEnablement();
                }
            }
        });
    }

    private void createJdbcPropertiesSection(final ScrolledForm form) {
        Section section = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.TITLE_BAR | Section.COMPACT);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        section.setLayoutData(td);
        section.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        section.setText(translator.getString("JDBC Properties"));
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout(1, false));
        section.setClient(sectionClient);
        createPropsPan = new DriverPropsPanel(sectionClient, SWT.None, "createNewBtn");
        createPropsPan.setAllComponentsEnabled(true);
    }

    private void createJarSection(final ScrolledForm form) {
        jarSection = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.TWISTIE | Section.TITLE_BAR | Section.COMPACT);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        jarSection.setDescription(translator.getString("*Add JARs to the table below, they will automatically be copied to <xaware.home>") + File.separator + "dynamic" + File.separator + "jars");
        jarSection.setLayoutData(td);
        jarSection.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        jarSection.setText(translator.getString("Required JARs"));
        Composite sectionClient = toolkit.createComposite(jarSection);
        sectionClient.setLayout(new GridLayout(2, false));
        createJarTableAndButtons(sectionClient);
        jarSection.setClient(sectionClient);
    }

    private void createJarTableAndButtons(Composite parent) {
        jarFileChooser = new XAFileChooser(XA_Designer_Plugin.getShell(), XA_Designer_Plugin.getPluginRootPath(), SWT.OPEN | SWT.MULTI);
        jarFileChooser.addDefaultFilter(XAFileConstants.JAR);
        jarTable = toolkit.createTable(parent, SWT.NULL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 100;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        jarTable.setLayoutData(gd);
        browseButton = toolkit.createButton(parent, translator.getString("Browse"), SWT.PUSH);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        browseButton.setLayoutData(gd);
        removeButton = toolkit.createButton(parent, translator.getString("Remove"), SWT.PUSH);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        removeButton.setLayoutData(gd);
    }

    protected void checkForDriverClass() {
        String driverClassString = driverClassCmb.getText();
        if (driverClassString != null && driverClassString.length() > 0) {
            boolean found = false;
            try {
                final XAClassLoader dynamicCL = XAClassLoader.getDynamicClassLoader(getClass().getClassLoader());
                Class.forName(driverClassString, false, dynamicCL);
                found = true;
            } catch (ClassNotFoundException e) {
                found = false;
            } finally {
                if (channelSection != null) {
                    String description;
                    if (found) {
                        description = "";
                    } else {
                        description = DRIVER_NOT_FOUND_TEXT;
                    }
                    channelSection.setDescription(description);
                }
                if (jarSection != null) {
                    if (!found) {
                        jarSection.setExpanded(true);
                    }
                }
            }
        }
    }

    private void createChannelSection(final ScrolledForm form) {
        channelSection = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.TITLE_BAR | Section.DESCRIPTION | Section.EXPANDED);
        TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
        channelSection.setLayoutData(td);
        channelSection.addExpansionListener(new ExpansionAdapter() {

            public void expansionStateChanged(ExpansionEvent e) {
                form.reflow(true);
            }
        });
        channelSection.setText(translator.getString("Database Channel"));
        channelSection.getDescriptionControl().setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        Composite sectionClient = toolkit.createComposite(channelSection);
        sectionClient.setLayout(new GridLayout(2, false));
        toolkit.createLabel(sectionClient, translator.getString("Database User:"), SWT.NONE);
        loginText = toolkit.createText(sectionClient, "", SWT.BORDER);
        GridData gridData = new GridData();
        gridData.widthHint = 270;
        gridData.horizontalIndent = 5;
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        loginText.setLayoutData(gridData);
        toolkit.createLabel(sectionClient, translator.getString("Database Password:"), SWT.NONE);
        passwordText = toolkit.createText(sectionClient, "", SWT.BORDER | SWT.PASSWORD);
        gridData = new GridData();
        gridData.widthHint = 270;
        gridData.horizontalIndent = 5;
        gridData.horizontalSpan = 1;
        gridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        passwordText.setLayoutData(gridData);
        toolkit.createLabel(sectionClient, translator.getString("Protocol:"), SWT.NONE);
        protocolCmb = new Combo(sectionClient, SWT.SINGLE);
        driverHash = UserPrefs.getSQLDriverHash();
        for (String p : driverHash.keySet()) {
            protocolCmb.add(p);
        }
        final GridData protocolGridData = new GridData();
        protocolGridData.widthHint = 270;
        protocolGridData.horizontalIndent = 5;
        protocolGridData.horizontalSpan = 1;
        protocolGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        protocolCmb.setLayoutData(protocolGridData);
        protocolCmb.setVisibleItemCount(8);
        toolkit.createLabel(sectionClient, translator.getString("Driver Class:"), SWT.NONE);
        driverClassCmb = new Combo(sectionClient, SWT.SINGLE);
        for (String c : driverHash.values()) {
            driverClassCmb.add(c);
        }
        driverClassCmb.setVisibleItemCount(8);
        final String[] items = protocolCmb.getItems();
        final GridData driverClassGridData = new GridData();
        driverClassGridData.widthHint = 270;
        driverClassGridData.horizontalIndent = 5;
        driverClassGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
        driverClassCmb.setLayoutData(driverClassGridData);
        for (String p : items) {
            if (p.startsWith("jdbc:derby:")) {
                protocolCmb.setText(p.trim());
                break;
            }
        }
        channelSection.setClient(sectionClient);
    }

    /**
     * This method updates the driver class combo box depending upon the selection on protocol combo box
     * and the user has not manually selected a driver class already.
     */
    private void updateDriverClassSelection() {
        if (driverSetByUser) {
            return;
        }
        final String protocolStr = protocolCmb.getText().trim();
        final String driverClassStr = driverHash.get(protocolStr);
        final String d = driverClassCmb.getText().trim();
        if (driverClassStr != null && !driverClassStr.equals(d)) {
            info.setDriverClass(driverClassStr.trim());
            driverClassCmb.setText(driverClassStr.trim());
            checkForDriverClass();
        } else if (!driverClassCmb.getText().trim().equals(info.getDriverClass())) {
            info.setDriverClass(driverClassCmb.getText().trim());
        }
    }

    /**
     * This method updates the protocol combo box depending upon the selection 
     * on the driver class combo and the user has not manually selected a protocol/url
     */
    private void updateProtocolSelection() {
        final String driverClassStr = driverClassCmb.getText().trim();
        final String protocolStr = protocolCmb.getText().trim();
        if (protocolSetByUser) {
            return;
        }
        if (driverClassStr.length() > 0) {
            for (Entry<String, String> e : driverHash.entrySet()) {
                if (e.getValue().equals(driverClassStr)) {
                    String p = e.getKey();
                    if (!p.equals(protocolStr)) {
                        info.setProtocol(e.getKey());
                        protocolCmb.setText(e.getKey());
                    }
                    break;
                }
            }
            if (!protocolCmb.getText().trim().equals(info.getProtocol())) {
                info.setDriverClass(driverClassCmb.getText().trim());
            }
        }
    }

    @Override
    public void setActive(boolean active) {
        if (!active) {
            this.noLongerActive();
        }
        super.setActive(active);
    }

    /**
     * This implementation will check the bizdriver reference, and if it is null, or blank
     * then it will set it to the sample bizdriver, otherwise it will load the specified bizdriver.
     * 
     * @see org.xaware.ide.xadev.richui.editor.service.AbstractServiceEditorPage#setDocumentModel(java.lang.String, org.xaware.ide.xadev.model.DocumentModel)
     */
    @Override
    public void setDocumentModel(String serviceName, DocumentModel model2) {
        super.setDocumentModel(serviceName, model2);
        bizDriverRef = RichUIEditorXmlUtil.getBizDriverRef(model);
        if (bizDriverRef != null && bizDriverRef.length() != 0) {
            try {
                info.loadFromFile(bizDriverRef);
            } catch (JDOMException e1) {
                ControlFactory.showStackTrace(translator.getString("Error parsing bizdriver ") + bizDriverRef, e1);
            } catch (IOException e1) {
            }
            populateScreen();
        } else {
            provideBizDriver();
        }
        setSelectionFromModel();
    }

    /**
     * This will will set the radio button selections appropriately and expand or contract the
     * channel section appropriately.
     */
    private void setSelectionFromModel() {
        if (haveDatabase != null && bizDriverRef != null) {
            if (usingSampleDatabase) {
                if (useSampleDatabase != null) {
                    useSampleDatabase.setSelection(true);
                    haveDatabase.setSelection(false);
                    channelSection.setExpanded(false);
                    provideBizDriver();
                }
            } else {
                if (haveDatabase != null) {
                    useSampleDatabase.setSelection(false);
                    haveDatabase.setSelection(true);
                    channelSection.setExpanded(true);
                }
            }
        }
    }

    private void provideBizDriver() {
        Path bizCompPath = new Path(model.getFilePath());
        IPath bizDriverPath = bizCompPath.removeFileExtension().addFileExtension("xdr");
        try {
            FileUtils.copyFile(ResourceUtils.getAbsolutePath(SQL_EXAMPLE_BIZDRIVER), bizDriverPath.toPortableString());
            model.getDocument().getRootElement().setAttribute(XAwareConstants.BIZCOMPONENT_ATTR_DRIVER, bizDriverPath.lastSegment(), XAwareConstants.xaNamespace);
            String newBizDriverRef = RichUIEditorXmlUtil.getBizDriverRef(model);
            bizDriverRef = newBizDriverRef;
            refChanged = true;
            usingSampleDatabase = true;
            try {
                if (info != null) {
                    info.closeJdbcTemplate();
                }
                info = new SQLBizDriverInfo();
                info.setOverwriteFile(true);
                info.setUseExisting(false);
                info.loadFromFile(bizDriverRef);
            } catch (JDOMException e1) {
                ControlFactory.showStackTrace(translator.getString("Error parsing bizdriver ") + bizDriverRef, e1);
            } catch (IOException e1) {
            }
            populateScreen();
        } catch (IOException e) {
            ControlFactory.showStackTrace(translator.getString("Unable to use the sample bizdriver"), e);
        }
    }

    private void populateScreen() {
        removeListeners();
        protocolSetByUser = false;
        driverSetByUser = false;
        if (this.loginText != null && info != null) {
            if (info.getLoginName() != null) {
                this.loginText.setText(info.getLoginName());
            } else {
                loginText.setText("");
            }
            setUserSummaryPageEntry();
            if (info.getPassword() != null) {
                this.passwordText.setText(info.getPassword());
            } else {
                passwordText.setText("");
            }
            setPasswordSummaryPageEntry();
            String tmp = info.getDriverClass();
            if (tmp != null && tmp.length() > 0) {
                this.driverClassCmb.setText(tmp);
            }
            tmp = info.getProtocol();
            if (tmp != null && tmp.length() > 0) {
                this.protocolCmb.setText(tmp);
            } else {
                String[] items = protocolCmb.getItems();
                for (String p : items) {
                    if (p.startsWith("jdbc:derby:")) {
                        protocolCmb.setText(p.trim());
                        info.setProtocol(p.trim());
                        break;
                    }
                }
                this.updateDriverClassSelection();
            }
            this.createPropsPan.setParametersData(info.getJdbcProperties());
            jarTable.removeAll();
            Set<String> jarSet = info.getJarSet();
            if (jarSet != null && jarSet.size() > 0) {
                for (String s : jarSet) {
                    TableItem item = new TableItem(jarTable, SWT.NONE);
                    item.setText(s);
                    if (!jarExistsInDynamicJars(s)) {
                        item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
                    }
                }
            }
            this.unMarkDirty();
            setEnablement();
            addListeners();
        }
    }

    private void setEnablement() {
        Color color;
        boolean enable = false;
        if (this.useSampleDatabase.getSelection()) {
            color = toolkit.getColors().getInactiveBackground();
            enable = false;
        } else {
            color = toolkit.getColors().getBackground();
            enable = true;
        }
        this.loginText.setBackground(color);
        this.loginText.setEnabled(enable);
        this.passwordText.setBackground(color);
        this.passwordText.setEnabled(enable);
        this.driverClassCmb.setBackground(color);
        this.driverClassCmb.setEnabled(enable);
        this.protocolCmb.setBackground(color);
        this.protocolCmb.setEnabled(enable);
        this.jarTable.setBackground(color);
        this.jarTable.setEnabled(enable);
        this.browseButton.setEnabled(enable);
        this.removeButton.setEnabled(enable);
        this.createPropsPan.setAllComponentsEnabled(enable);
        form.reflow(true);
    }

    private boolean jarExistsInDynamicJars(String jarFileName) {
        File jar = new File(System.getProperty(XA_Designer_Plugin.xahomeStr) + XAwareConstants.DYNAMIC_JARS_PATH, jarFileName);
        return jar.exists();
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == createPropsPan) {
            info.setJdbcProperties(createPropsPan.getParamProps());
            this.markDirty();
        } else if (e.getSource() == model.getDocument()) {
            if (e.getUserData() instanceof String) {
                String userData = (String) e.getUserData();
                if (userData.equals(XAwareConstants.BIZCOMPONENT_ATTR_DRIVER)) {
                    String newBizDriverRef = RichUIEditorXmlUtil.getBizDriverRef(model);
                    bizDriverRef = newBizDriverRef;
                    try {
                        if (info != null) {
                            info.closeJdbcTemplate();
                        }
                        info = new SQLBizDriverInfo();
                        info.setOverwriteFile(true);
                        info.setUseExisting(false);
                        info.loadFromFile(bizDriverRef);
                    } catch (JDOMException e1) {
                        ControlFactory.showStackTrace(translator.getString("Error parsing bizdriver ") + bizDriverRef, e1);
                    } catch (IOException e1) {
                    }
                    populateScreen();
                }
            }
        }
    }

    protected void saveBizDriver() {
        if (isDirty()) {
            if (info != null) {
                if (bizDriverRef != null && bizDriverRef.length() > 0) {
                    try {
                        Set<String> jars = new HashSet<String>();
                        for (TableItem jar : jarTable.getItems()) {
                            jars.add(jar.getText());
                        }
                        info.setJarSet(jars);
                        info.saveBizDriverFileFromSelf();
                    } catch (IOException e) {
                        ControlFactory.showStackTrace(translator.getString("Error saving bizdriver ") + bizDriverRef, e);
                    }
                    List<String> lines = new ArrayList<String>();
                    lines.add(translator.getString("All database connection information is located in a bizdriver at"));
                    lines.add("\t" + bizDriverRef);
                    sendSummaryPageEntry(new SummaryPageEntry(this, CONNECTION_INFORMATION, lines));
                    this.unMarkDirty();
                }
            }
        }
    }

    protected void fireBizDriverRefChanged() {
        this.fireModelChanged(XAwareConstants.BIZCOMPONENT_ATTR_DRIVER);
    }

    private void noLongerActive() {
        if (this.isDirty() || refChanged) {
            this.saveBizDriver();
            fireBizDriverRefChanged();
            refChanged = false;
        }
        List<String> lines = new ArrayList<String>();
        lines.add(translator.getString("All database connection information is located in a bizdriver at"));
        lines.add("\t" + bizDriverRef);
        sendSummaryPageEntry(new SummaryPageEntry(this, CONNECTION_INFORMATION, lines));
    }

    private void addListeners() {
        if (!listenersAdded) {
            driverClassCmb.addModifyListener(this);
            testConnectionButton.addSelectionListener(this);
            browseButton.addSelectionListener(this);
            createPropsPan.addChangeListener(this);
            removeButton.addSelectionListener(this);
            loginText.addModifyListener(this);
            passwordText.addModifyListener(this);
            protocolCmb.addModifyListener(this);
            listenersAdded = true;
        }
    }

    private void removeListeners() {
        if (listenersAdded) {
            driverClassCmb.removeModifyListener(this);
            testConnectionButton.removeSelectionListener(this);
            browseButton.removeSelectionListener(this);
            createPropsPan.removeChangeListener(this);
            removeButton.removeSelectionListener(this);
            loginText.removeModifyListener(this);
            passwordText.removeModifyListener(this);
            protocolCmb.removeModifyListener(this);
            listenersAdded = false;
        }
    }

    /**
     * When a listener for modifications on a control has changed call this method
     */
    public void modifyText(ModifyEvent e) {
        if (e.getSource() == driverClassCmb) {
            Object oldDriverClass = info.getDriverClass();
            if (!driverClassCmb.getText().equals(oldDriverClass)) {
                driverSetByUser = true;
                info.setDriverClass(driverClassCmb.getText());
                checkForDriverClass();
                if (listenersAdded) {
                    protocolCmb.removeModifyListener(this);
                }
                updateProtocolSelection();
                if (listenersAdded) {
                    protocolCmb.addModifyListener(this);
                }
            }
        } else if (e.getSource() == loginText) {
            info.setLoginName(loginText.getText());
            setUserSummaryPageEntry();
        } else if (e.getSource() == passwordText) {
            info.setPassword(passwordText.getText());
            setPasswordSummaryPageEntry();
        } else if (e.getSource() == protocolCmb) {
            Object oldProtocol = info.getProtocol();
            if (oldProtocol != null && !oldProtocol.equals(protocolCmb.getText())) {
                protocolSetByUser = true;
                info.setProtocol(protocolCmb.getText());
                if (listenersAdded) {
                    driverClassCmb.removeModifyListener(this);
                }
                updateDriverClassSelection();
                if (listenersAdded) {
                    driverClassCmb.addModifyListener(this);
                }
            }
        }
        this.markDirty();
    }

    /**
     * 
     */
    private void setUserSummaryPageEntry() {
        if (loginText.getText().equals("")) {
            setSummaryPageEntry(USERNAME, null);
        } else {
            setSummaryPageEntry(USERNAME, COMPLETED);
        }
    }

    /**
     * 
     */
    private void setPasswordSummaryPageEntry() {
        if (passwordText.getText().equals("")) {
            setSummaryPageEntry(PASSWORD, null);
        } else {
            setSummaryPageEntry(PASSWORD, COMPLETED);
        }
    }

    private void setSummaryPageEntry(String entryId, String message) {
        List<String> lines = null;
        if (message != null) {
            lines = new ArrayList<String>();
            lines.add(message);
        }
        sendSummaryPageEntry(new SummaryPageEntry(this, entryId, lines));
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == this.testConnectionButton) {
            try {
                info.closeJdbcTemplate();
                info.testConnectivity();
                ControlFactory.showInformationDialog(translator.getString("Connection successful"));
                info.closeJdbcTemplate();
            } catch (XAwareConfigurationException ex) {
                logger.debug(ex);
                ControlFactory.showStackTrace(translator.getString("Caught WizardException: ") + ex.getLocalizedMessage(), ex);
            }
        } else if (e.getSource() == this.browseButton) {
            jarFileChooser.open();
            String[] selectedJarFilePaths = jarFileChooser.getFileNames();
            if (selectedJarFilePaths != null) {
                for (String selectedJarFilePath : selectedJarFilePaths) {
                    File file = new File(jarFileChooser.getFilterPath(), selectedJarFilePath);
                    String jarFileName = file.getName();
                    if (!jarFileName.equals("")) {
                        TableItem item = new TableItem(jarTable, SWT.NONE);
                        item.setText(jarFileName);
                        copyJarFileToDynamic(file);
                        checkForDriverClass();
                        this.markDirty();
                    }
                }
            }
        } else if (e.getSource() == removeButton) {
            int[] indices = jarTable.getSelectionIndices();
            for (int i : indices) {
                jarTable.remove(i);
            }
            checkForDriverClass();
            this.markDirty();
        }
    }

    protected void copyJarFileToDynamic(File file) {
        String fileName = file.getName();
        String dstFilePath = System.getProperty(XA_Designer_Plugin.xahomeStr);
        if (!dstFilePath.endsWith(File.separator)) {
            dstFilePath += File.separator;
        }
        dstFilePath += XAwareConstants.DYNAMIC_JARS_PATH + File.separator + fileName;
        try {
            FileUtils.copyFile(file.getAbsolutePath(), dstFilePath);
            summaryPage.addLineToEntry(this, JAR_INFORMATION, translator.getString("Copied jar ") + file.getAbsolutePath() + translator.getString(" to ") + dstFilePath);
        } catch (FileNotFoundException e) {
            ControlFactory.showStackTrace(translator.getString("Error copying jar file ") + bizDriverRef, e);
        } catch (IOException e) {
            ControlFactory.showStackTrace(translator.getString("Error copying jar file ") + bizDriverRef, e);
        }
    }

    @Override
    protected void initializeLoggingEntries() {
    }

    @Override
    protected String getFormTitle() {
        return translator.getString("SQL Connection");
    }

    @Override
    protected String getFormImageName() {
        return translator.getString("NewBizDriver");
    }

    @Override
    protected String getContextId() {
        return XAwareConstants.CONTEXT_ID_SQL_SERVICE_EDITOR_CONNECTION;
    }

    @Override
    protected String getContextSearchExpression() {
        return translator.getString("sql bizdriver connection");
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        this.saveBizDriver();
        this.unMarkDirty();
        super.doSave(monitor);
    }
}
