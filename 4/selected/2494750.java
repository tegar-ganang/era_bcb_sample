package org.xaware.ide.xadev.richui.editor.pages.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.shared.DriverPropsPanel;
import org.xaware.ide.shared.GUIConstants;
import org.xaware.ide.shared.PropertiesPanel;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.InterfaceHelper;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.datamodel.IBizDriverInfo;
import org.xaware.ide.xadev.datamodel.InputParameterData;
import org.xaware.ide.xadev.datamodel.SQLBizDriverInfo;
import org.xaware.ide.xadev.gui.ChangeEvent;
import org.xaware.ide.xadev.gui.ChangeListener;
import org.xaware.ide.xadev.gui.DebugInputParameterEditorPanel;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.ide.xadev.gui.InputParameterDataEditorPanel;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.richui.editor.util.RichUIEditorUtil;
import org.xaware.ide.xadev.wizard.AbstractBizDriverWizardPanelComponent;
import org.xaware.ide.xadev.wizard.Wizard;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * @author satishk
 *
 */
public class SQLBizDriverFormPage extends FormPage implements ModifyListener, SelectionListener, FocusListener, ChangeListener {

    private FormToolkit toolkit;

    /**
	 * @param id
	 * @param title
	 */
    public SQLBizDriverFormPage(String id, String title) {
        super(id, title);
    }

    /**
	 * @param editor
	 * @param id
	 * @param title
	 */
    public SQLBizDriverFormPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        ScrolledForm form = managedForm.getForm();
        toolkit = managedForm.getToolkit();
        form.setText("SQL BizDriver");
        form.setBackgroundImage(RichUIEditorUtil.getFormImage());
        form.setImage(UserPrefs.getImageIconFor("NewBizComp"));
        toolkit.decorateFormHeading(form.getForm());
        RichUIEditorUtil.contributeToToolbar(form);
        initFromXML(new Hashtable<String, String>());
    }

    /**String constant for 'Use default intial context on server'*/
    public static final String USE_DEFAULT_CONTEXT_ON_SERVER = "Use default initial context on server";

    /**Sting constant for 'remote information is always required by the Designer' string.*/
    public static final String REMOTE_INFO_ALWAYS_REQ_BY_DESIGNER = "Note: remote information is always required by Designer.";

    /**
     * Stored Procedure boolean value contains true if the screen is for Strored Procedure
     */
    private boolean isStoredProc;

    /** Use Existing BizDriver Radio Button */
    protected Button useExistingBtn;

    /** Create New BizDriver Radio Button */
    protected Button createNewBtn;

    /** Login TextField */
    protected Text loginTxt;

    /** Password TextField */
    protected Text passwordTxt;

    /** Protocol Combo */
    protected Combo protocolCmb;

    /** Driver Class Combo */
    protected Combo driverClassCmb;

    /** BizDriver Type Label */
    protected Label bizDriverTypeLbl;

    /** Login Label */
    protected Label loginLabel;

    /** Password Label */
    protected Label passwordLabel;

    /** Protocol Label */
    protected Label protocolLabel;

    /** Driver Class Label */
    protected Label driverClassLabel;

    /** Hashtable which contains drivers */
    private LinkedHashMap<String, String> driverHash;

    /** Checks whether the BizDocument selected is Excel or SQL */
    private boolean isExcel;

    /** Driver Properties Label */
    protected Label driverPropLabel;

    /** Composite which contains Driver Properties */
    protected DriverPropsPanel createPropsPan;

    /** Composite which contains Driver Properties */
    protected DriverPropsPanel useExistPropsPan;

    /** Composite for the page */
    protected Composite parent;

    /** Query Type composite for SQL or Excel */
    protected Composite qTypeGroup;

    /** Stores the path */
    protected String initPath;

    /** Composite for driver properties */
    private Composite driverComp;

    /** JDBC BizDriver Button */
    protected Button jdbcBizDriverBtn;

    /** JNDI BizDriver Button */
    protected Button jndiBizDriverBtn;

    /** StackLayout to swap between J2EE and JDBC BizDriver fields */
    private StackLayout stackLayout;

    /** JDBC BizDriver Composite */
    Composite jdbcBizDriverComp;

    /** J2EE BizDriver Composite */
    Composite j2eeBizDriverComp;

    /** BizDriver Type Hashmap to swap fields */
    HashMap<String, Composite> bizDriverTypeFieldsMap;

    /**
     * Composite that holds the composites for jdbc and j2ee fields using stack layout
     */
    Composite bizDriverComposite;

    /** BizDriver file chooser */
    protected FileChooserWithLabeling fileChooser;

    /** J2EE login text field */
    Text j2eeLoginTxt;

    /** J2EE password field */
    Text j2eePasswordTxt;

    /** J2EE Host Combo Box */
    Combo j2eeHostCmb;

    /** J2EE Factory Combo Box */
    Combo j2eeFactoryCmb;

    /** Root element name for J2EE BizDriver */
    String j2eeBizDriverRootElementName;

    /** JNDI Name field */
    Text j2eeJndiNameTxt;

    /** JNDI User Transaction Name field */
    protected Text j2eeJndiUserTransactionNameTxt;

    /** Pooling Tab */
    private CTabItem poolingTab;

    /** Create version of Pool Properties Panel*/
    private PropertiesPanel createPoolPropertiesPanel;

    /** Use Existing version of Pool Properties Panel*/
    private PropertiesPanel useExistPoolPropertiesPanel;

    /** Pool Properties composite */
    private Composite poolPropertiesTable;

    /** Save BizDriver Button instance*/
    protected Button saveBizDriverBtn;

    /** JNDI Properties Label */
    protected Label jndiPropLabel;

    /** JNDI Properties panel */
    protected Composite jndiPropComp;

    /** Properties panel for JNDI create mode parameters*/
    protected DriverPropsPanel createJNDIPropsPan;

    /** Properties panel for JNDI existing mode parameters*/
    protected DriverPropsPanel useExistJNDIPropsPan;

    /** Transaction Manager Lookup Name */
    protected Text j2eeTransactionManagerLookupNameTxt;

    /**use default context on server check button.*/
    protected Button useDefContextOnServer;

    /**boolean indication that changes made are yet to be saved.*/
    private boolean changesToBeSaved;

    protected IBizDriverInfo bizDriverInfo = null;

    private ScrolledComposite scrollableComposite;

    /**
     * Initializes the page from XML and creates all the widgets that are to be created on the wizard page.
     * 
     * @param inputParams -
     *            input from UserConfig XML file.
     */
    public void initFromXML(final Hashtable<String, String> inputParams) {
        initPath = XA_Designer_Plugin.getActiveEditedFileDirectory();
        if (initPath == null) {
            initPath = XA_Designer_Plugin.getXAwareRootPath();
        }
        if (initPath != null && !initPath.endsWith(File.separator)) {
            initPath += File.separator;
        }
        if ((inputParams.get("isExcel") != null) && ((String) inputParams.get("isExcel")).equalsIgnoreCase("yes")) {
            isExcel = true;
        } else {
            isExcel = false;
        }
        isStoredProc = false;
        if (inputParams.get("isStoredProc") != null) {
            final String tmp = (String) inputParams.get("isStoredProc");
            isStoredProc = Boolean.valueOf(tmp).booleanValue();
        }
        parent = getManagedForm().getForm().getBody();
        parent.setLayout(new FormLayout());
        Composite scrollCompContainer = toolkit.createComposite(parent);
        scrollCompContainer.setLayout(new FillLayout());
        scrollableComposite = new ScrolledComposite(scrollCompContainer, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        toolkit.adapt(scrollableComposite);
        final Composite group = toolkit.createComposite(scrollableComposite, SWT.FLAT);
        toolkit.paintBordersFor(group);
        GridLayout gridLayout = new GridLayout();
        GridData gridData = new GridData(GridData.FILL_BOTH);
        group.setLayout(gridLayout);
        group.setLayoutData(gridData);
        final Composite bizDriverComp = toolkit.createComposite(group, SWT.FLAT);
        gridLayout = new GridLayout(3, false);
        if (isExcel) {
            gridLayout.verticalSpacing = 12;
        } else {
            gridLayout.verticalSpacing = 5;
        }
        gridData = new GridData(GridData.FILL_BOTH);
        bizDriverComp.setLayout(gridLayout);
        bizDriverComp.setLayoutData(gridData);
        useExistingBtn = toolkit.createButton(bizDriverComp, "", SWT.RADIO);
        useExistingBtn.setText(t.getString("Use Existing BizDriver"));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 3;
        useExistingBtn.setLayoutData(gridData);
        useExistingBtn.addSelectionListener(this);
        useExistingBtn.addFocusListener(this);
        createNewBtn = toolkit.createButton(bizDriverComp, "", SWT.RADIO);
        createNewBtn.setText(t.getString("Create New BizDriver"));
        createNewBtn.setSelection(true);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 3;
        createNewBtn.setLayoutData(gridData);
        createNewBtn.addSelectionListener(this);
        fileChooser = new FileChooserWithLabeling(bizDriverComp, SWT.FLAT, "BizDriver File Name: ", XA_Designer_Plugin.getActiveEditedFileDirectory(), "Browse...", XAChooser.BIZ_DRIVER_TYPE, toolkit);
        toolkit.adapt(fileChooser);
        toolkit.paintBordersFor(fileChooser);
        fileChooser.setMode(SWT.SAVE);
        fileChooser.focusListenerForTextField(this);
        fileChooser.modifyListenerForTextField(this);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        fileChooser.setLayoutData(gridData);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        fileChooser.getTextField().setLayoutData(gridData);
        toolkit.adapt(fileChooser.getTextField().getParent());
        toolkit.paintBordersFor(fileChooser.getTextField().getParent());
        saveBizDriverBtn = new Button(bizDriverComp, SWT.FLAT);
        toolkit.adapt(saveBizDriverBtn, true, true);
        saveBizDriverBtn.setText("Save BizDriver");
        saveBizDriverBtn.addSelectionListener(this);
        bizDriverTypeLbl = toolkit.createLabel(bizDriverComp, "BizDriver Type:", SWT.FLAT);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        bizDriverTypeLbl.setLayoutData(gridData);
        bizDriverTypeLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        final Composite bizDriverButtonsComp = toolkit.createComposite(bizDriverComp, SWT.FLAT);
        final GridLayout bizDriverGridLayout = new GridLayout(2, false);
        final GridData bizDriverButtonsGridData = new GridData();
        bizDriverButtonsGridData.horizontalAlignment = GridData.BEGINNING;
        bizDriverButtonsGridData.horizontalSpan = 2;
        bizDriverButtonsComp.setLayout(bizDriverGridLayout);
        bizDriverButtonsComp.setLayoutData(bizDriverButtonsGridData);
        jdbcBizDriverBtn = toolkit.createButton(bizDriverButtonsComp, "", SWT.RADIO);
        jdbcBizDriverBtn.setText(GUIConstants.JDBC_BIZDRIVER);
        jdbcBizDriverBtn.setSelection(true);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        jdbcBizDriverBtn.setLayoutData(gridData);
        jdbcBizDriverBtn.addSelectionListener(this);
        jndiBizDriverBtn = toolkit.createButton(bizDriverButtonsComp, "", SWT.RADIO);
        jndiBizDriverBtn.setText(GUIConstants.JNDI_BIZDRIVER);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        jndiBizDriverBtn.setLayoutData(gridData);
        jndiBizDriverBtn.addSelectionListener(this);
        tabFolder = new CTabFolder(bizDriverComp, SWT.BORDER);
        tabFolder.setSelectionBackground(new Color[] { tabFolder.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND), toolkit.getColors().getBackground() }, new int[] { 70 });
        toolkit.adapt(tabFolder, true, true);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        tabFolder.setLayoutData(gridData);
        CTabItem channelTab = new CTabItem(tabFolder, SWT.FLAT);
        channelTab.setText("Channel");
        poolingTab = new CTabItem(tabFolder, SWT.FLAT);
        poolingTab.setText("Pooling");
        createPoolingComposite();
        inputTab = new CTabItem(tabFolder, SWT.FLAT);
        inputTab.setText("Input Parameters");
        createInputComposite();
        executionInputTab = new CTabItem(tabFolder, SWT.FLAT);
        executionInputTab.setText("Wizard Execution Input");
        createExecutionInputComposite();
        stackLayout = new StackLayout();
        bizDriverComposite = toolkit.createComposite(tabFolder, SWT.FLAT);
        bizDriverComposite.setLayout(stackLayout);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        bizDriverComposite.setLayoutData(gridData);
        bizDriverTypeFieldsMap = new HashMap<String, Composite>();
        createJDBCFields(GUIConstants.JDBC_BIZDRIVER, bizDriverComposite);
        createJNDIFields(GUIConstants.JNDI_BIZDRIVER, inputParams, bizDriverComposite);
        stackLayout.topControl = jdbcBizDriverComp;
        tabFolder.setSelection(channelTab);
        bizDriverComposite.layout();
        if (driverClassCmb.getText().equals("") && (driverClassCmb.getItemCount() > 0)) {
            driverClassCmb.setText(driverClassCmb.getItem(0).trim());
        }
        if (protocolCmb.getText().equals("") && (protocolCmb.getItemCount() > 0)) {
            protocolCmb.setText(protocolCmb.getItem(0).trim());
        }
        driverClassCmb.addSelectionListener(this);
        if (isExcel) {
            bizDriverTypeLbl.setVisible(false);
            jdbcBizDriverBtn.setVisible(false);
            jndiBizDriverBtn.setVisible(false);
        }
        channelTab.setControl(bizDriverComposite);
        toolkit.paintBordersFor(bizDriverComp);
        toolkit.paintBordersFor(parent);
        scrollableComposite.setExpandHorizontal(true);
        scrollableComposite.setExpandVertical(true);
        scrollableComposite.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrollableComposite.setContent(group);
        Composite progressComp = RichUIEditorUtil.createOverallProgress(parent, toolkit);
        FormData progressCompFormData = new FormData();
        progressCompFormData.bottom = new FormAttachment(100, -5);
        progressComp.setLayoutData(progressCompFormData);
        FormData scrollCompContainerFormData = new FormData();
        scrollCompContainerFormData.bottom = new FormAttachment(progressComp, -5);
        scrollCompContainerFormData.right = new FormAttachment(100, -5);
        scrollCompContainerFormData.top = new FormAttachment(0, 10);
        scrollCompContainerFormData.left = new FormAttachment(0, 10);
        scrollCompContainer.setLayoutData(scrollCompContainerFormData);
    }

    /**
     * Creates the pooling composite and adds the pooling components.
     *
     */
    private void createPoolingComposite() {
        Composite poolTabComposite = toolkit.createComposite(tabFolder, SWT.FLAT);
        poolTabComposite.setLayout(new GridLayout(2, false));
        Label poolPropLabel = toolkit.createLabel(poolTabComposite, "", SWT.HORIZONTAL);
        poolPropLabel.setText(t.getString("Pool Properties:              "));
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        poolPropLabel.setLayoutData(gridData);
        poolPropLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        poolPropertiesTable = toolkit.createComposite(poolTabComposite, SWT.FLAT);
        gridData = new GridData(GridData.FILL_BOTH);
        poolPropertiesTable.setLayoutData(gridData);
        poolPropertiesTable.setLayout(new StackLayout());
        String tableHeader = "Pool Properties";
        String tableHeaderDescription = "Define any Pool-specific parameters required to fine tune the database connection.";
        URI schemaFile = null;
        try {
            String schemaFileName = "/org/xaware/server/engine/channel/SQLBizDriver.xsd";
            logger.finest("Trying to Loading schema file from:" + schemaFileName);
            URL url = getClass().getResource(schemaFileName);
            logger.finest("Loaded schema file from URL:" + url);
            schemaFile = URI.createURI(url.toURI().toString());
            logger.finest("Loaded schema file from URI:" + schemaFile);
        } catch (Exception exception) {
            logger.finest("Unable to load JDBC schema file:" + exception.getMessage(), exception);
        }
        String elementName = "PoolingParameters";
        createPoolPropertiesPanel = new PropertiesPanel(toolkit, poolPropertiesTable, SWT.FLAT, tableHeader, tableHeaderDescription, schemaFile, elementName);
        useExistPoolPropertiesPanel = new PropertiesPanel(toolkit, poolPropertiesTable, SWT.FLAT, tableHeader, tableHeaderDescription, schemaFile, elementName);
        ((StackLayout) poolPropertiesTable.getLayout()).topControl = createPoolPropertiesPanel;
        createPoolPropertiesPanel.setComponentsEnabled(true);
        poolingTab.setControl(poolTabComposite);
        toolkit.adapt(createPoolPropertiesPanel);
        toolkit.adapt(useExistPoolPropertiesPanel);
    }

    /**
     * Create JDBC BizDriver fields
     * 
     * @param key
     *            DOCUMENT ME!
     * @param parentComp
     *            DOCUMENT ME!
     */
    public void createJDBCFields(final String key, final Composite parentComp) {
        GridData gridData;
        jdbcBizDriverComp = new Composite(parentComp, SWT.FLAT);
        toolkit.adapt(jdbcBizDriverComp, true, true);
        final GridLayout gridLayout = new GridLayout(3, false);
        if (isExcel) {
            gridLayout.verticalSpacing = 12;
        } else {
            gridLayout.verticalSpacing = 5;
        }
        gridData = new GridData(GridData.FILL_BOTH);
        jdbcBizDriverComp.setLayout(gridLayout);
        jdbcBizDriverComp.setLayoutData(gridData);
        if (isExcel) {
            driverHash = UserPrefs.getExcelDriverHash();
        } else {
            driverHash = UserPrefs.getSQLDriverHash();
        }
        final Vector<String> classes = new Vector<String>();
        final Vector<String> protocols = new Vector<String>();
        final Iterator<String> keys = driverHash.keySet().iterator();
        while (keys.hasNext()) {
            final String protcolName = (String) keys.next();
            protocols.add(protcolName);
            classes.add(driverHash.get(protcolName).toString());
        }
        if (!isExcel) {
            loginLabel = new Label(jdbcBizDriverComp, SWT.FLAT);
            loginLabel.setText(t.getString("Login:"));
            toolkit.adapt(loginLabel, true, true);
            final GridData loginGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            loginGridData.horizontalSpan = 1;
            loginLabel.setLayoutData(loginGridData);
            loginLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
            loginTxt = ControlFactory.createText(jdbcBizDriverComp, SWT.BORDER);
            final GridData loginTxtGridData = new GridData(GridData.FILL_HORIZONTAL);
            loginTxtGridData.widthHint = 270;
            loginTxtGridData.horizontalIndent = 5;
            loginTxtGridData.horizontalSpan = 2;
            loginTxt.setLayoutData(loginTxtGridData);
            toolkit.adapt(loginTxt, true, true);
            passwordLabel = new Label(jdbcBizDriverComp, SWT.FLAT);
            toolkit.adapt(passwordLabel, true, true);
            passwordLabel.setText(t.getString("Password:"));
            final GridData passwordGridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            passwordGridData.horizontalSpan = 1;
            passwordLabel.setLayoutData(passwordGridData);
            passwordLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
            passwordTxt = new Text(jdbcBizDriverComp, SWT.BORDER | SWT.PASSWORD);
            toolkit.adapt(passwordTxt, true, true);
            final GridData passwordTxtGridData = new GridData(GridData.FILL_HORIZONTAL);
            passwordTxtGridData.horizontalSpan = 2;
            passwordTxtGridData.widthHint = 270;
            passwordTxtGridData.horizontalIndent = 5;
            passwordTxt.setLayoutData(passwordTxtGridData);
            loginTxt.addModifyListener(this);
            passwordTxt.addModifyListener(this);
        }
        protocolLabel = new Label(jdbcBizDriverComp, SWT.FLAT);
        toolkit.adapt(protocolLabel, true, true);
        if (isExcel) {
            protocolLabel.setText(t.getString("Protocol(for Excel only):"));
        } else {
            protocolLabel.setText(t.getString("Protocol:"));
        }
        protocolLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        protocolLabel.setLayoutData(gridData);
        protocolCmb = new Combo(jdbcBizDriverComp, SWT.FLAT | SWT.SINGLE);
        toolkit.adapt(protocolCmb, true, true);
        for (int i = 0; i < protocols.size(); i++) {
            protocolCmb.add(protocols.get(i));
        }
        final GridData protocolGridData = new GridData(GridData.FILL_HORIZONTAL);
        protocolGridData.widthHint = 270;
        protocolGridData.horizontalSpan = 2;
        protocolGridData.horizontalIndent = 5;
        protocolCmb.setLayoutData(protocolGridData);
        protocolCmb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        protocolCmb.setVisibleItemCount(8);
        protocolCmb.addSelectionListener(this);
        driverClassLabel = new Label(jdbcBizDriverComp, SWT.FLAT);
        toolkit.adapt(driverClassLabel, true, true);
        driverClassLabel.setText(t.getString("Driver Class: "));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        driverClassLabel.setLayoutData(gridData);
        driverClassLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        driverClassCmb = new Combo(jdbcBizDriverComp, SWT.FLAT | SWT.SINGLE);
        driverClassCmb.setVisibleItemCount(8);
        if (!isExcel) {
            final String[] items = protocolCmb.getItems();
            for (int protocolIndex = 0; protocolIndex < items.length; protocolIndex++) {
                if (items[protocolIndex].startsWith("jdbc:hsqldb:")) {
                    protocolCmb.setText(items[protocolIndex].trim());
                    updateDriverClassSelection();
                    break;
                }
            }
        } else {
            protocolGridData.widthHint = 150;
        }
        for (int i = 0; i < classes.size(); i++) {
            driverClassCmb.add(classes.get(i).trim());
        }
        driverClassCmb.addSelectionListener(this);
        final GridData driverClassGridData = new GridData(GridData.FILL_HORIZONTAL);
        driverClassGridData.horizontalSpan = 2;
        driverClassGridData.widthHint = 270;
        if (isExcel) {
            driverClassGridData.widthHint = 150;
        }
        driverClassGridData.horizontalIndent = 5;
        driverClassCmb.setLayoutData(driverClassGridData);
        driverClassCmb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        driverPropLabel = new Label(jdbcBizDriverComp, SWT.FLAT);
        toolkit.adapt(driverPropLabel, false, false);
        driverPropLabel.setText(t.getString("Driver Properties:"));
        gridData = new GridData(GridData.CENTER);
        gridData.horizontalSpan = 1;
        driverPropLabel.setLayoutData(gridData);
        driverPropLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        driverComp = new Composite(jdbcBizDriverComp, SWT.FLAT);
        driverComp.setBackground(toolkit.getColors().getBackground());
        toolkit.adapt(driverComp, true, true);
        toolkit.paintBordersFor(driverComp);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        driverComp.setLayout(new StackLayout());
        driverComp.setLayoutData(gridData);
        createTable(driverComp);
        bizDriverTypeFieldsMap.put(key, jdbcBizDriverComp);
        protocolCmb.addModifyListener(this);
        driverClassCmb.addModifyListener(this);
        toolkit.paintBordersFor(jdbcBizDriverComp);
    }

    /**
     * Create J2EE BizDriver fields
     * 
     * @param key
     *            DOCUMENT ME!
     * @param inputParams
     *            DOCUMENT ME!
     * @param parentComp
     *            DOCUMENT ME!
     */
    @SuppressWarnings("unchecked")
    public void createJNDIFields(final String key, final Hashtable<String, String> inputParams, final Composite parentComp) {
        GridData gridData;
        j2eeBizDriverComp = new Composite(parentComp, SWT.FLAT);
        toolkit.adapt(j2eeBizDriverComp);
        toolkit.paintBordersFor(j2eeBizDriverComp);
        final GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.verticalSpacing = 10;
        gridData = new GridData(GridData.FILL_BOTH);
        j2eeBizDriverComp.setLayout(gridLayout);
        j2eeBizDriverComp.setLayoutData(gridData);
        Composite containerComp = new Composite(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(containerComp);
        containerComp.setLayout(new GridLayout(2, true));
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        containerComp.setLayoutData(gridData);
        Composite defContextOnServerComp = new Composite(containerComp, SWT.FLAT);
        toolkit.adapt(defContextOnServerComp);
        defContextOnServerComp.setLayout(new GridLayout());
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        defContextOnServerComp.setLayoutData(gridData);
        useDefContextOnServer = new Button(defContextOnServerComp, SWT.CHECK | SWT.FLAT);
        toolkit.adapt(useDefContextOnServer, true, true);
        useDefContextOnServer.setText(USE_DEFAULT_CONTEXT_ON_SERVER);
        useDefContextOnServer.setSelection(true);
        Label designerNote = new Label(defContextOnServerComp, SWT.FLAT);
        toolkit.adapt(designerNote, true, true);
        designerNote.setText(REMOTE_INFO_ALWAYS_REQ_BY_DESIGNER);
        designerNote.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        final Label jndiNameLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(jndiNameLbl, true, true);
        jndiNameLbl.setText("JNDI Name: ");
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        jndiNameLbl.setLayoutData(gridData);
        jndiNameLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeJndiNameTxt = ControlFactory.createText(j2eeBizDriverComp, SWT.BORDER);
        toolkit.adapt(j2eeJndiNameTxt, true, true);
        j2eeJndiNameTxt.addModifyListener(this);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeJndiNameTxt.setLayoutData(gridData);
        final Label jndiUserTransactionNameLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(jndiUserTransactionNameLbl, true, true);
        jndiUserTransactionNameLbl.setText("JNDI User Transaction Name: ");
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        jndiUserTransactionNameLbl.setLayoutData(gridData);
        jndiUserTransactionNameLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeJndiUserTransactionNameTxt = ControlFactory.createText(j2eeBizDriverComp, SWT.BORDER);
        toolkit.adapt(j2eeJndiUserTransactionNameTxt, true, true);
        j2eeJndiUserTransactionNameTxt.addModifyListener(this);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeJndiUserTransactionNameTxt.setLayoutData(gridData);
        final Label jndiTransactionManagerLookupNameLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(jndiTransactionManagerLookupNameLbl, true, true);
        jndiTransactionManagerLookupNameLbl.setText("Transaction Manager Lookup Name: ");
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        jndiTransactionManagerLookupNameLbl.setLayoutData(gridData);
        jndiTransactionManagerLookupNameLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeTransactionManagerLookupNameTxt = ControlFactory.createText(j2eeBizDriverComp, SWT.BORDER);
        toolkit.adapt(j2eeTransactionManagerLookupNameTxt, true, true);
        j2eeTransactionManagerLookupNameTxt.addModifyListener(this);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeTransactionManagerLookupNameTxt.setLayoutData(gridData);
        final Label loginLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(loginLbl, true, true);
        loginLbl.setText(getJNDILoginLabel());
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        loginLbl.setLayoutData(gridData);
        loginLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeLoginTxt = ControlFactory.createText(j2eeBizDriverComp, SWT.BORDER);
        toolkit.adapt(j2eeLoginTxt, true, true);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeLoginTxt.setLayoutData(gridData);
        j2eeLoginTxt.addModifyListener(this);
        final Label passwordLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(passwordLbl, true, true);
        passwordLbl.setText(getJNDIPasswordLabel());
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        passwordLbl.setLayoutData(gridData);
        passwordLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eePasswordTxt = new Text(j2eeBizDriverComp, SWT.BORDER | SWT.PASSWORD);
        toolkit.adapt(j2eePasswordTxt, true, true);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eePasswordTxt.setLayoutData(gridData);
        j2eePasswordTxt.addModifyListener(this);
        final Label hostLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(hostLbl, true, true);
        hostLbl.setText("Host(URL): ");
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        hostLbl.setLayoutData(gridData);
        hostLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeHostCmb = new Combo(j2eeBizDriverComp, SWT.SINGLE | SWT.FLAT);
        toolkit.adapt(j2eeHostCmb, true, true);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeHostCmb.setLayoutData(gridData);
        j2eeHostCmb.addSelectionListener(this);
        j2eeHostCmb.addModifyListener(this);
        final List<String> hostList = UserPrefs.getContextHostList();
        for (int i = 0; i < hostList.size(); i++) {
            j2eeHostCmb.add((String) hostList.get(i), i);
        }
        if (j2eeHostCmb.getItemCount() > 0) {
            j2eeHostCmb.setText(j2eeHostCmb.getItem(0));
        }
        final Label factoryLbl = new Label(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(factoryLbl, true, true);
        factoryLbl.setText("Factory: ");
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        factoryLbl.setLayoutData(gridData);
        factoryLbl.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        j2eeFactoryCmb = new Combo(j2eeBizDriverComp, SWT.SINGLE | SWT.FLAT);
        toolkit.adapt(j2eeFactoryCmb, true, true);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        j2eeFactoryCmb.setLayoutData(gridData);
        j2eeFactoryCmb.addSelectionListener(this);
        j2eeFactoryCmb.addModifyListener(this);
        final List factoryList = UserPrefs.getContextFactoryList();
        for (int j = 0; j < factoryList.size(); j++) {
            j2eeFactoryCmb.add((String) factoryList.get(j), j);
        }
        if (j2eeFactoryCmb.getItemCount() > 0) {
            j2eeFactoryCmb.setText(j2eeFactoryCmb.getItem(0));
        }
        jndiPropLabel = new Label(j2eeBizDriverComp, SWT.HORIZONTAL | SWT.FLAT);
        toolkit.adapt(jndiPropLabel, true, true);
        jndiPropLabel.setText(t.getString("Driver Properties:"));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 1;
        jndiPropLabel.setLayoutData(gridData);
        jndiPropLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        jndiPropComp = new Composite(j2eeBizDriverComp, SWT.FLAT);
        toolkit.adapt(jndiPropComp, true, true);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        jndiPropComp.setLayout(new StackLayout());
        jndiPropComp.setLayoutData(gridData);
        createJNDIPropTable(jndiPropComp);
        if (inputParams != null) {
            j2eeBizDriverRootElementName = (String) inputParams.get("rootelement");
        } else {
            j2eeBizDriverRootElementName = new String("jms");
        }
        bizDriverTypeFieldsMap.put(key, j2eeBizDriverComp);
    }

    /**
     * Returns the missing data message
     * 
     * @return ""
     */
    public String getMissingDataMessage() {
        return "";
    }

    protected Object getData() {
        this.getSqlBizDriverInfo();
        return bizDriverInfo;
    }

    /**
     * This method returns the file name of the Biz driver used.
     * 
     * @return File name of the Biz driver.
     * 
     * @throws WizardException
     *             wraps the exception when retriving data
     * @throws  
     */
    protected SQLBizDriverInfo getSqlBizDriverInfo() {
        Element designerElement = null;
        if (bizDriverInfo == null) {
            bizDriverInfo = new SQLBizDriverInfo();
        }
        ((SQLBizDriverInfo) bizDriverInfo).setFileName(ResourceUtils.getAbsolutePath(this.fileChooser.getFileString()));
        if (jdbcBizDriverBtn.getSelection()) {
            ((SQLBizDriverInfo) bizDriverInfo).setBizDriverType(GUIConstants.JDBC_BIZDRIVER);
            ((SQLBizDriverInfo) bizDriverInfo).setLoginName(loginTxt.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setPassword(passwordTxt.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setDriverClass(driverClassCmb.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setProtocol(protocolCmb.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setJdbcProperties(createPropsPan.getParamProps());
            ((SQLBizDriverInfo) bizDriverInfo).setDataSourceDescription(this.inputParamEditor.getDescription());
        } else {
            ((SQLBizDriverInfo) bizDriverInfo).setBizDriverType(GUIConstants.JNDI_BIZDRIVER);
            if (useDefContextOnServer.getSelection()) ((SQLBizDriverInfo) bizDriverInfo).setDefaultInitialContext(true); else ((SQLBizDriverInfo) bizDriverInfo).setDefaultInitialContext(false);
            ((SQLBizDriverInfo) bizDriverInfo).setJndiLoginName(j2eeLoginTxt.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setJndiTransactionManagerLookupName(j2eeTransactionManagerLookupNameTxt.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setJndiPassword(j2eePasswordTxt.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setHostName(j2eeHostCmb.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setFactoryName(j2eeFactoryCmb.getText());
            ((SQLBizDriverInfo) bizDriverInfo).setJndiName(this.j2eeJndiNameTxt.getText().trim());
            ((SQLBizDriverInfo) bizDriverInfo).setJndiUserTransactionName(j2eeJndiUserTransactionNameTxt.getText().trim());
            ((SQLBizDriverInfo) bizDriverInfo).setJndiProperties(createJNDIPropsPan.getParamProps());
        }
        ((SQLBizDriverInfo) bizDriverInfo).setPoolProperties(createPoolPropertiesPanel.getProperties());
        if (createNewBtn.getSelection()) {
            ((SQLBizDriverInfo) bizDriverInfo).setUseExisting(false);
        } else {
            ((SQLBizDriverInfo) bizDriverInfo).setUseExisting(true);
        }
        ((SQLBizDriverInfo) bizDriverInfo).setIsExcel(isExcel);
        ((SQLBizDriverInfo) bizDriverInfo).setStoredProc(isStoredProc);
        Vector<InputParameterData> inputParams = this.executionInputParamEditor.getInputParams();
        ((SQLBizDriverInfo) bizDriverInfo).setInputParameters(inputParams);
        ((SQLBizDriverInfo) bizDriverInfo).setInputXml(this.executionInputParamEditor.getAdditionalXML());
        IDialogSettings dialogSettings = this.getWizardDialogSettings();
        if (dialogSettings != null) {
            IDialogSettings bdSection = dialogSettings.getSection(((SQLBizDriverInfo) bizDriverInfo).getFileName());
            if (bdSection == null) {
                bdSection = dialogSettings.addNewSection(((SQLBizDriverInfo) bizDriverInfo).getFileName());
            }
            for (InputParameterData param : inputParams) {
                String value = param.getWorkingValue();
                if (value != null) {
                    bdSection.put(param.getName(), value);
                }
            }
            IDialogSettings inputSection = bdSection.getSection(INPUT_SECTION);
            if (inputSection == null) {
                inputSection = bdSection.addNewSection(INPUT_SECTION);
            }
            int inputType = executionInputParamEditor.getType();
            inputSection.put(INPUT_TYPE, inputType);
            if (inputType != DebugInputParameterEditorPanel.NONE) {
                inputSection.put(INPUT_XML, executionInputParamEditor.getAdditionalXML());
            }
            this.saveWizardDialogSettings(dialogSettings);
        }
        final Element root = ((SQLBizDriverInfo) bizDriverInfo).constructBizDriverRootElement();
        ((SQLBizDriverInfo) bizDriverInfo).setDocument(new Document(root));
        designerElement = buildDesignerElement();
        ((SQLBizDriverInfo) bizDriverInfo).setDesignerElement(designerElement);
        return ((SQLBizDriverInfo) bizDriverInfo);
    }

    /**
     * This method checks whether the Driver for the SQL or Excel or Stored Procedure is valid or not.
     * 
     * @return Boolean
     */
    protected boolean isPanelValid() {
        if (jdbcBizDriverBtn.getSelection()) {
            return isJDBCPanelValid();
        } else {
            return isJ2EEPanelValid();
        }
    }

    /**
     * This method checks whether the Driver for the SQL or Excel or Stored Procedure is valid or not.
     * 
     * @return Boolean
     */
    protected boolean isJDBCPanelValid() {
        String fname = fileChooser.getFileString().trim();
        fileChooser.getTextField().selectAll();
        if (fname.equals("")) {
            ControlFactory.showMessageDialog(t.getString("Enter BizDriver file name."), t.getString("Information"));
            return false;
        }
        File bizDriverFile = ResourceUtils.getFile(fname);
        if (bizDriverFile.isDirectory()) {
            ControlFactory.showMessageDialog(t.getString("Enter BizDriver file name."), t.getString("Information"));
            return false;
        }
        if (bizDriverFile.getName().trim().equals(".xdr")) {
            ControlFactory.showMessageDialog(t.getString("Enter BizDriver file name."), t.getString("Information"));
            return false;
        }
        if ((bizDriverFile.getAbsolutePath().indexOf('#') > -1) || (bizDriverFile.getAbsolutePath().indexOf('%') > -1)) {
            ControlFactory.showMessageDialog(t.getString("Please do not enter # or % in the file path or name."), t.getString("Information"));
            return false;
        }
        String dirName = bizDriverFile.getParent();
        fileChooser.getTextField().removeModifyListener(this);
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = initPath;
            fileChooser.setFileString(dirName + fileChooser.getFileString());
            fileChooser.getTextField().selectAll();
        } else if (dirName.trim().equals(File.separator)) {
            dirName = initPath;
            fileChooser.setFileString(dirName.substring(0, dirName.length() - 1) + fileChooser.getFileString());
            fileChooser.getTextField().selectAll();
        } else {
            dirName = dirName.trim();
            if (dirName.charAt(dirName.length() - 1) == File.separatorChar) {
                fileChooser.setFileString(dirName + bizDriverFile.getName().trim());
                fileChooser.getTextField().selectAll();
            } else {
                fileChooser.setFileString(fname);
                fileChooser.getTextField().selectAll();
            }
        }
        fileChooser.getTextField().addModifyListener(this);
        final File tmpFile = ResourceUtils.getFile(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog(t.getString("Invalid directory for BizDriver file."), t.getString("Information"));
            return false;
        }
        fname = fileChooser.getFileString().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (createNewBtn.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
            fileChooser.getTextField().removeModifyListener(this);
            if (fileNameStr.indexOf('.') == -1) {
                fname = fname + ".xdr";
                fileChooser.setFileString(fname);
                fileChooser.getTextField().selectAll();
                bizDriverFile = new File(fname);
            } else if (fileNameStr.indexOf('.') == (fileNameStr.length() - 1)) {
                fname = fname + "xdr";
                fileChooser.setFileString(fname);
                fileChooser.getTextField().selectAll();
                bizDriverFile = new File(fname);
            }
            fileChooser.getTextField().addModifyListener(this);
            if (bizDriverFile.exists() && changesToBeSaved) {
                final int choice = ControlFactory.showConfirmDialog("BizDriver file which you have entered already exists. Overwrite?");
                if (choice != MessageDialog.OK) {
                    return false;
                }
            }
        } else if (useExistingBtn.getSelection()) {
            if (bizDriverFile.isDirectory() || !bizDriverFile.exists()) {
                ControlFactory.showMessageDialog(t.getString("BizDriver file which you have entered does not exist. Please enter correct file name."), t.getString("Information"));
                return false;
            }
        }
        if (hasChanged() && !testConnectivity()) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the page complete
     * 
     * @return Boolean
     */
    public boolean isComplete() {
        final boolean retVal = true;
        return retVal;
    }

    /**
     * This method populates the wizard page with the details of the JDBC BizDriver
     */
    @SuppressWarnings("unchecked")
    public void populateJDBCScreen(Element root) {
        Element connectionElement = null;
        String uid = "";
        String pwd = "";
        String protocol = "";
        String driverClass = "";
        if (root != null) {
            try {
                populatePoolingParameters(root);
                connectionElement = root.getChild(XAwareConstants.BIZDRIVER_CONNECTION, ns);
                uid = connectionElement.getChild(XAwareConstants.BIZDRIVER_USER, ns).getText();
                pwd = connectionElement.getChild(XAwareConstants.BIZDRIVER_PWD, ns).getText();
                protocol = connectionElement.getChild(XAwareConstants.BIZDRIVER_URL, ns).getText();
                driverClass = connectionElement.getChild(XAwareConstants.BIZDRIVER_FACTORY, ns).getText();
                final Element propertiesElement = connectionElement.getChild(XAwareConstants.ELEMENT_PROPERTIES, ns);
                final List<Element> paramList = propertiesElement.getChildren(XAwareConstants.ELEMENT_PROPERTY, ns);
                if (paramList.size() > 0) {
                    final Vector<Vector<String>> dataVec = new Vector<Vector<String>>();
                    for (int i = 0; i < paramList.size(); i++) {
                        final Vector<String> rowVec = new Vector<String>();
                        final Element paramElem = (Element) paramList.get(i);
                        rowVec.add(paramElem.getAttributeValue(XAwareConstants.XAWARE_ATTR_NAME, ns));
                        rowVec.add(paramElem.getAttributeValue(XAwareConstants.XAWARE_ATTR_VALUE, ns));
                        dataVec.add(rowVec);
                    }
                    createPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                    useExistPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                } else {
                    final Vector dataVec = new Vector();
                    createPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                    useExistPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                }
            } catch (final Throwable ex) {
                logger.fine("Error parsing elements from biz driver file.");
            }
        } else {
            final Vector dataVec = new Vector();
            createPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
            useExistPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
        }
        try {
            if (!isExcel) {
                loginTxt.removeModifyListener(this);
                passwordTxt.removeModifyListener(this);
                loginTxt.setText(uid);
                passwordTxt.setText(pwd);
                loginTxt.addModifyListener(this);
                passwordTxt.addModifyListener(this);
            }
        } catch (final Throwable ex) {
        }
        protocolCmb.setText(protocol.trim());
        driverClassCmb.setText(driverClass.trim());
    }

    /**
     * Populates the pooling parameters in to the table
     * @param root bizdriver root element.
     */
    @SuppressWarnings("unchecked")
    private void populatePoolingParameters(Element root) {
        final Element poolPropertiesElement = root.getChild(XAwareConstants.ELEMENT_POOLINGPARAMS, ns);
        if (poolPropertiesElement.getChildren().size() > 0) {
            final Vector<Vector<String>> dataVec = new Vector<Vector<String>>();
            for (int i = 0; i < poolPropertiesElement.getChildren().size(); i++) {
                final Vector<String> rowVec = new Vector<String>();
                final Element paramElem = (Element) poolPropertiesElement.getChildren().get(i);
                rowVec.add(paramElem.getName());
                rowVec.add(paramElem.getValue());
                dataVec.add(rowVec);
            }
            createPoolPropertiesPanel.setParametersData(dataVec);
            useExistPoolPropertiesPanel.setParametersData(dataVec);
        } else {
            final Vector dataVec = new Vector();
            createPoolPropertiesPanel.setParametersData(dataVec);
            useExistPoolPropertiesPanel.setParametersData(dataVec);
        }
    }

    /**
     * This method populates the wizard page with the details of J2EE the BizDriver.
     */
    @SuppressWarnings("unchecked")
    public void populateJ2EEScreen(Element root) {
        String jndiName = "";
        String userTransactionLookupName = "";
        String transactionManagerLookupName = "";
        String uid = "";
        String pwd = "";
        String host = "";
        String factory = "";
        boolean isDefaultInitialContext = true;
        if (root != null) {
            try {
                populatePoolingParameters(root);
                Element initialContent = root.getChild(XAwareConstants.BIZDRIVER_INITIALCONTEXT, ns);
                if (initialContent == null) return;
                Attribute tempAttribute = initialContent.getAttribute(XAwareConstants.BIZDRIVER_ATTR_INITIALCONTEXT_TYPE, ns);
                if (tempAttribute != null && tempAttribute.getValue().equals(XAwareConstants.BIZDRIVER_ATTR_INITIALCONTEXT_TYPE_REMOTE)) isDefaultInitialContext = false;
                Element tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_URL, ns);
                host = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_USER, ns);
                uid = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_PWD, ns);
                pwd = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_FACTORY, ns);
                factory = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_INITIALCONTEXT_LOOKUPNAME, ns);
                jndiName = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_INITIALCONTEXT_USERTRANSACTIONOLOOKUPNAME, ns);
                userTransactionLookupName = tempElement.getText();
                tempElement = initialContent.getChild(XAwareConstants.BIZDRIVER_INITIALCONTEXT_TRANSACTIONMANAGEROLOOKUPNAME, ns);
                transactionManagerLookupName = tempElement.getText();
                final Element propertiesElement = initialContent.getChild(XAwareConstants.ELEMENT_PROPERTIES, ns);
                final List paramList = propertiesElement.getChildren(XAwareConstants.ELEMENT_PROPERTY, ns);
                if (paramList.size() > 0) {
                    final Vector<Vector<String>> dataVec = new Vector<Vector<String>>();
                    for (int i = 0; i < paramList.size(); i++) {
                        final Vector<String> rowVec = new Vector<String>();
                        final Element paramElem = (Element) paramList.get(i);
                        rowVec.add(paramElem.getAttributeValue(XAwareConstants.XAWARE_ATTR_NAME, ns));
                        rowVec.add(paramElem.getAttributeValue(XAwareConstants.XAWARE_ATTR_VALUE, ns));
                        dataVec.add(rowVec);
                    }
                    createJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                    useExistJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                } else {
                    final Vector dataVec = new Vector();
                    createJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                    useExistJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
                }
            } catch (final Exception ex) {
                logger.finest("Error parsing elements from J2EE BizDriver file.");
            }
        } else {
            final Vector dataVec = new Vector();
            createJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
            useExistJNDIPropsPan.setParametersData(dataVec, useExistingBtn.getSelection());
        }
        if (isDefaultInitialContext) {
            useDefContextOnServer.setSelection(true);
        } else {
            useDefContextOnServer.setSelection(false);
        }
        j2eeJndiNameTxt.setText(jndiName);
        j2eeJndiUserTransactionNameTxt.setText(userTransactionLookupName);
        j2eeTransactionManagerLookupNameTxt.setText(transactionManagerLookupName);
        j2eeLoginTxt.setText(uid);
        j2eePasswordTxt.setText(pwd);
        j2eeHostCmb.setText(host);
        j2eeFactoryCmb.setText(factory);
    }

    /**
     * Populate both JDBC and J2EE screens
     */
    public void populateScreen() {
        Document rootDoc = this.loadBizDriverFile(this.fileChooser.getFileString().trim());
        Element root = null;
        if (rootDoc != null) {
            root = rootDoc.getRootElement();
        }
        populateJDBCScreen(root);
        populateJ2EEScreen(root);
        setupInputParams(root, this.fileChooser.getFileString().trim());
    }

    /**
     * This method specifies whether the Radio button selected is Select or Insert or Insert/Update or Update and adds
     * the selected type to the Element.
     * 
     * @return Element contains selected type of the Radio button.
     */
    public Element buildDesignerElement() {
        final Element designerElem = new Element(XAwareConstants.DESIGNER_ELEMENT_WIZARD_STEP, ns);
        designerElem.setAttribute(XAwareConstants.DESIGNER_ATTR_NAME, "Biz Driver", ns);
        return designerElem;
    }

    /**
     * This method establishes the connection with the details of the driver information and returns the status.
     * 
     * @return boolean true if Connection is Successful, otherwise false
     */
    private boolean testConnectivity() {
        try {
            this.getData();
            bizDriverInfo.testConnectivity();
        } catch (XAwareConfigurationException e) {
            logger.debug(e);
            ControlFactory.showStackTrace("Caught WizardException: " + e.toString(), e);
            return false;
        }
        if (t != null) {
            return true;
        }
        return false;
    }

    /**
     * Initializes the page from BizComponent Root
     */
    public void initFromBizCompRoot() {
        final Element root = Wizard.getBizCompRoot();
        try {
            if (root != null) {
                final String bizDriver = root.getAttribute(XAwareConstants.BIZCOMPONENT_ATTR_DRIVER, ns).getValue();
                setBizDriverInfo(bizDriver, root);
                useExistingBtn.setSelection(true);
                useExistingBtn.setFocus();
                createNewBtn.setSelection(false);
                Event event = new Event();
                event.widget = useExistingBtn;
                widgetSelected(new SelectionEvent(event));
            }
        } catch (final Exception ex) {
            final String message = "BizDriver data could not be loaded completely. Exception:" + ex.getMessage();
            logger.severe(message);
            ControlFactory.showInfoDialog(t.getString("BizDriver data could not be loaded completely."), message, true);
        }
    }

    /**
     * This method is called when the driver file name is entered or populated from browse button in the fileChooserTxt
     * TextField
     * 
     * @param e
     *            ModifyEvent of file name TextField
     */
    public void modifyText(final ModifyEvent e) {
        if (e.getSource() == fileChooser.getTextField()) {
            if (useExistingBtn.getSelection()) {
                this.populateScreen();
            }
        } else if (e.getSource() == j2eeJndiNameTxt) {
        }
        internalSetDataChanged(true);
    }

    /**
     * This method is called for selectBtn,insertBtn,insertUpdateBtn,updateBtn,useExistingBtn,
     * createNewBtn,protocolCmb,driverClassCmb and browseBtn Selection Listeners.
     * 
     * @param e
     *            SelectionEvent of the widget
     */
    public void widgetSelected(final SelectionEvent e) {
        boolean localChangesToBeSaved = true;
        if ((e.getSource() == useExistingBtn) && useExistingBtn.getSelection()) {
            fileChooser.setMode(SWT.OPEN);
            handleUseExistingActionForJDBCFields();
            handleUseExistingActionForJ2EEFields();
            inputParamEditor.setComponentsEnabled(false);
            populateScreen();
            if (useExistingBtn.getSelection()) saveBizDriverBtn.setEnabled(false); else saveBizDriverBtn.setEnabled(true);
        } else if ((e.getSource() == createNewBtn) && createNewBtn.getSelection()) {
            fileChooser.setMode(SWT.SAVE);
            handleCreateNewActionForJDBCFields();
            handleCreateNewActionForJ2EEFields();
            inputParamEditor.setComponentsEnabled(true);
            if (createNewBtn.getSelection()) saveBizDriverBtn.setEnabled(true); else saveBizDriverBtn.setEnabled(false);
        } else if (e.getSource() == protocolCmb) {
            updateDriverClassSelection();
        } else if (e.getSource() == jdbcBizDriverBtn) {
            handleJDBCBizDriverButton();
        } else if (e.getSource() == jndiBizDriverBtn) {
            handleJ2EEBizDriverButton();
        } else if (e.getSource() == saveBizDriverBtn) {
            if (isPanelValid()) {
                SQLBizDriverInfo info;
                try {
                    info = this.getSqlBizDriverInfo();
                    info.saveBizDriverFile(true);
                    localChangesToBeSaved = false;
                } catch (Exception exception) {
                    logger.info("Exception writing SQLBizDriver file: ", exception);
                    ControlFactory.showErrorDialog(t.getString("Error saving BizDriver file: ") + exception.getMessage(), t.getString("Error"));
                }
            }
        }
        internalSetDataChanged(localChangesToBeSaved);
    }

    /**
	 * Calls the setDataChanged of wizard panel component and updates 
	 * the changesToBeSaved variable with the parameter value.
	 * @param localChangesToBeSaved boolean value indicating if changes are yet to be saved.
	 */
    private void internalSetDataChanged(boolean localChangesToBeSaved) {
        setDataChanged();
        changesToBeSaved = localChangesToBeSaved;
    }

    /**
     * Perform action upon selecting JDBC BizDriver option
     */
    public void handleJDBCBizDriverButton() {
        final Composite jdbcComp = bizDriverTypeFieldsMap.get(GUIConstants.JDBC_BIZDRIVER);
        stackLayout.topControl = jdbcComp;
        bizDriverComposite.layout();
    }

    /**
     * Perform action upon selecting J2EE BizDriver option
     */
    public void handleJ2EEBizDriverButton() {
        final Composite j2eeComp = bizDriverTypeFieldsMap.get(GUIConstants.JNDI_BIZDRIVER);
        stackLayout.topControl = j2eeComp;
        bizDriverComposite.layout();
        if (useExistingBtn.getSelection()) {
            fileChooser.setMode(SWT.OPEN);
        } else {
            fileChooser.setMode(SWT.SAVE);
        }
    }

    /**
     * This method is used to update the JDBC fields when the user selects "USe Existing BizDriver" option.
     */
    public void handleUseExistingActionForJDBCFields() {
        if (!isExcel) {
            loginTxt.setEnabled(false);
            passwordTxt.setEnabled(false);
        }
        protocolCmb.setEnabled(false);
        driverClassCmb.setEnabled(false);
        useExistPropsPan.setComponentsEnabled(false);
        useExistPropsPan.setEnabled(false);
        useExistPoolPropertiesPanel.setComponentsEnabled(false);
        useExistPoolPropertiesPanel.setEnabled(false);
        createPropsPan.setVisible(false);
        useExistPropsPan.setVisible(true);
        createPoolPropertiesPanel.setVisible(false);
        useExistPoolPropertiesPanel.setVisible(true);
    }

    /**
     * This method is used to update the J2EE fields when the user selects "Use Existing BizDriver" option.
     */
    public void handleUseExistingActionForJ2EEFields() {
        useDefContextOnServer.setEnabled(false);
        j2eeJndiNameTxt.setEnabled(false);
        j2eeJndiNameTxt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eeJndiUserTransactionNameTxt.setEnabled(false);
        j2eeJndiUserTransactionNameTxt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eeTransactionManagerLookupNameTxt.setEnabled(false);
        j2eeTransactionManagerLookupNameTxt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eeLoginTxt.setEnabled(false);
        j2eeLoginTxt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eePasswordTxt.setEnabled(false);
        j2eePasswordTxt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eeHostCmb.setEnabled(false);
        j2eeHostCmb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        j2eeFactoryCmb.setEnabled(false);
        j2eeFactoryCmb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        createJNDIPropsPan.setVisible(false);
        useExistJNDIPropsPan.setComponentsEnabled(false);
        useExistJNDIPropsPan.setVisible(true);
        fileChooser.setMode(SWT.OPEN);
    }

    /**
     * This method is used to update the JDBC fields when the user selects "Create New BizDriver" option.
     */
    public void handleCreateNewActionForJDBCFields() {
        if (!isExcel) {
            loginTxt.setEnabled(true);
            passwordTxt.setEnabled(true);
        }
        protocolCmb.setEnabled(true);
        driverClassCmb.setEnabled(true);
        createPropsPan.setComponentsEnabled(true);
        createPoolPropertiesPanel.setComponentsEnabled(true);
        useExistPropsPan.setVisible(false);
        useExistPoolPropertiesPanel.setVisible(false);
        createPropsPan.setVisible(true);
        createPoolPropertiesPanel.setVisible(true);
        if (driverClassCmb.getText().equals("")) {
            driverClassCmb.select(0);
        }
        if (protocolCmb.getText().equals("")) {
            protocolCmb.select(0);
        }
    }

    /**
     * This method is used to update the J2EE fields when the user selects "Create New BizDriver" option.
     */
    public void handleCreateNewActionForJ2EEFields() {
        useDefContextOnServer.setEnabled(true);
        j2eeJndiNameTxt.setEnabled(true);
        j2eeJndiUserTransactionNameTxt.setEnabled(true);
        j2eeTransactionManagerLookupNameTxt.setEnabled(true);
        j2eeLoginTxt.setEnabled(true);
        j2eePasswordTxt.setEnabled(true);
        j2eeHostCmb.setEnabled(true);
        j2eeFactoryCmb.setEnabled(true);
        if (j2eeHostCmb.getText().equals("") && (j2eeHostCmb.getItemCount() > 0)) {
            j2eeHostCmb.setText(j2eeHostCmb.getItem(0));
        }
        if (j2eeFactoryCmb.getText().equals("") && (j2eeFactoryCmb.getItemCount() > 0)) {
            j2eeFactoryCmb.setText(j2eeFactoryCmb.getItem(0));
        }
        fileChooser.setMode(SWT.SAVE);
        useExistJNDIPropsPan.setVisible(false);
        createJNDIPropsPan.setVisible(true);
        createJNDIPropsPan.setComponentsEnabled(true);
    }

    /**
     * This method updates the driver class combo box depending upon the selection on protocol combo box.
     */
    public void updateDriverClassSelection() {
        final String protocolStr = protocolCmb.getText();
        final String driverClassStr = driverHash.get(protocolStr);
        if (driverClassStr != null) {
            driverClassCmb.setText(driverClassStr.trim());
        }
    }

    /**
     * This method is called for widgets of the Selection Listerners if the widget is selected by default.
     * 
     * @param e
     *            SelectionEvent of the widget
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }

    /**
     * This method is called when the use existing radio option gains focus.
     * 
     * @param e
     *            FocusEvent of use existing radio button.
     */
    public void focusGained(final FocusEvent e) {
        if (useExistingBtn.getSelection()) {
            if (!isExcel) {
                loginTxt.setEnabled(false);
                passwordTxt.setEnabled(false);
            }
            protocolCmb.setEnabled(false);
            driverClassCmb.setEnabled(false);
            useExistPropsPan.setComponentsEnabled(false);
            useExistPoolPropertiesPanel.setComponentsEnabled(false);
            createPropsPan.setVisible(false);
            useExistPropsPan.setVisible(true);
            ((StackLayout) driverComp.getLayout()).topControl = useExistPropsPan;
            createJNDIPropsPan.setVisible(false);
            useExistJNDIPropsPan.setVisible(true);
            ((StackLayout) jndiPropComp.getLayout()).topControl = useExistJNDIPropsPan;
            createPoolPropertiesPanel.setVisible(false);
            useExistPoolPropertiesPanel.setVisible(true);
            ((StackLayout) poolPropertiesTable.getLayout()).topControl = useExistPoolPropertiesPanel;
        }
    }

    /**
     * This method is implemented as part of Focus Listener.
     * 
     * @param e
     *            instance of FocusEvent.
     */
    public void focusLost(final FocusEvent e) {
    }

    /**
     * This method creates Driver properties table for Create and Use Existing Drivers.
     * 
     * @param driverComp
     *            Composite for Driver properties table.
     */
    private void createTable(final Composite driverComp) {
        createPropsPan = new DriverPropsPanel(driverComp, SWT.FLAT, "createNewBtn", toolkit);
        createPropsPan.setBackground(toolkit.getColors().getBackground());
        useExistPropsPan = new DriverPropsPanel(driverComp, SWT.FLAT, "useExistingBtn", toolkit);
        useExistPropsPan.setBackground(toolkit.getColors().getBackground());
        ((StackLayout) driverComp.getLayout()).topControl = createPropsPan;
        createPropsPan.setComponentsEnabled(true);
    }

    /**
     * This method creates JNDI Driver properties table for Create and Use Existing Drivers.
     * 
     * @param driverComp
     *            Composite for Driver properties table.
     */
    protected void createJNDIPropTable(final Composite driverComp) {
        String headerName = "JNDI Properties";
        String headerDescription = "Define any JNDI Driver specific parameters required to fine tune the database\nconnection.";
        createJNDIPropsPan = new DriverPropsPanel(driverComp, SWT.FLAT, "createNewBtn", headerName, headerDescription, toolkit);
        toolkit.adapt(createJNDIPropsPan);
        useExistJNDIPropsPan = new DriverPropsPanel(driverComp, SWT.FLAT, "useExistingBtn", headerName, headerDescription, toolkit);
        toolkit.adapt(useExistJNDIPropsPan);
        ((StackLayout) driverComp.getLayout()).topControl = createJNDIPropsPan;
        createJNDIPropsPan.setComponentsEnabled(true);
    }

    /**
     * Check whether the J2EE panel is valid.
     * 
     * @return Boolean
     */
    protected boolean isJ2EEPanelValid() {
        String fname = fileChooser.getFileString().trim();
        fileChooser.getTextField().removeModifyListener(this);
        fileChooser.setFileString(fname);
        fileChooser.getTextField().addModifyListener(this);
        if (fname.equals("")) {
            ControlFactory.showMessageDialog("Enter BizDriver file name.", "Information");
            return false;
        }
        File bizDriverFile = ResourceUtils.getFile(fname);
        if (bizDriverFile.isDirectory()) {
            ControlFactory.showMessageDialog("Enter BizDriver file name.", "Information");
            return false;
        }
        if (bizDriverFile.getName().trim().equals(".xdr")) {
            ControlFactory.showMessageDialog("Enter BizDriver file name.", "Information");
            return false;
        }
        if ((bizDriverFile.getAbsolutePath().indexOf('#') > -1) || (bizDriverFile.getAbsolutePath().indexOf('%') > -1)) {
            ControlFactory.showMessageDialog("Please do not enter # or % in the file path or name.", "Information");
            return false;
        }
        String dirName = bizDriverFile.getParent();
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
                dirName = dirName + File.separator;
            }
            fileChooser.getTextField().removeModifyListener(this);
            fileChooser.setFileString(dirName + fileChooser.getFileString());
            fileChooser.getTextField().addModifyListener(this);
        } else if (dirName.trim().equals(File.separator)) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            fileChooser.getTextField().removeModifyListener(this);
            fileChooser.setFileString(dirName.substring(0, dirName.length() - 1) + fileChooser.getFileString());
            fileChooser.getTextField().addModifyListener(this);
        } else {
            dirName = dirName.trim();
            fileChooser.getTextField().removeModifyListener(this);
            if (dirName.charAt(dirName.length() - 1) == File.separatorChar) {
                fileChooser.setFileString(dirName + bizDriverFile.getName().trim());
            } else {
                fileChooser.setFileString(fname);
            }
            fileChooser.getTextField().addModifyListener(this);
        }
        final File tmpFile = ResourceUtils.getFile(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog("Invalid directory for BizDriver file.", "Information");
            return false;
        }
        fname = fileChooser.getFileString().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (createNewBtn.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
            fileChooser.getTextField().removeModifyListener(this);
            if (fileNameStr.indexOf('.') == -1) {
                fname = fname + ".xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            } else if (fileNameStr.indexOf('.') == (fileNameStr.length() - 1)) {
                fname = fname + "xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            }
            fileChooser.getTextField().addModifyListener(this);
            if (bizDriverFile.exists() && changesToBeSaved) {
                final int choice = ControlFactory.showConfirmDialog("BizDriver file which you have entered " + "already exists. Overwrite?");
                if (choice != MessageDialog.OK) {
                    return false;
                }
            }
        } else if (useExistingBtn.getSelection()) {
            if (bizDriverFile.isDirectory() || !bizDriverFile.exists()) {
                ControlFactory.showMessageDialog("BizDriver file which you have entered does not exist. Please " + "enter correct file name.", "Information");
                return false;
            }
        }
        if (this.j2eeJndiNameTxt.getText().trim().equals("")) {
            ControlFactory.showMessageDialog("Enter JNDI name.", "Information");
            this.j2eeJndiNameTxt.forceFocus();
            return false;
        }
        if (this.j2eeHostCmb.getText().trim().equals("")) {
            ControlFactory.showMessageDialog("Enter Host(URL).", "Information");
            this.j2eeHostCmb.forceFocus();
            return false;
        }
        if (this.j2eeFactoryCmb.getText().trim().equals("")) {
            ControlFactory.showMessageDialog("Enter Factory.", "Information");
            this.j2eeFactoryCmb.forceFocus();
            return false;
        }
        if (hasChanged() && !testConnectivity()) {
            return false;
        }
        return true;
    }

    /**
     * Set BizDriver fields based on the type of the BizDriver
     * 
     * @param bizDriverFile
     *            BizDriver file name
     * @param root
     *            Root element for the BizComp
     */
    public void setBizDriverInfo(final String bizDriverFile, final Element root) {
        final SAXBuilder sb = new SAXBuilder();
        try {
            final Document doc = sb.build(ResourceUtils.getAbsolutePath(bizDriverFile));
            final Element elem = doc.getRootElement();
            final String bizDriverType = elem.getAttribute("bizdrivertype", ns).getValue();
            if (bizDriverType.equals(XAwareConstants.BIZDRIVER_ATTR_SQL_JNDI)) {
                if ((!isExcel)) {
                    fileChooser.setFileString(bizDriverFile);
                    fileChooser.getTextField().setSelection(fileChooser.getTextField().getCharCount());
                    jndiBizDriverBtn.setSelection(true);
                    jdbcBizDriverBtn.setSelection(false);
                    handleJ2EEBizDriverButton();
                } else {
                    fileChooser.setFileString(bizDriverFile);
                    fileChooser.getTextField().selectAll();
                    jdbcBizDriverBtn.setSelection(true);
                    ControlFactory.showMessageDialog("The BizDriver referenced in the BizComponent is not a JDBC BizDriver.", "Information");
                }
            } else if (bizDriverType.equals(XAwareConstants.BIZDRIVER_ATTR_SQL_JDBC)) {
                fileChooser.setFileString(bizDriverFile);
                fileChooser.getTextField().selectAll();
                jdbcBizDriverBtn.setSelection(true);
                handleJDBCBizDriverButton();
            } else {
                if ((!isExcel)) {
                    fileChooser.setFileString(bizDriverFile);
                    fileChooser.getTextField().selectAll();
                    jdbcBizDriverBtn.setSelection(true);
                    ControlFactory.showMessageDialog("The BizDriver referenced in the BizComponent is not a JDBC or JNDI BizDriver.", "Information");
                } else {
                    fileChooser.setFileString(bizDriverFile);
                    fileChooser.getTextField().selectAll();
                    jdbcBizDriverBtn.setSelection(true);
                    ControlFactory.showMessageDialog("The BizDriver referenced in the BizComponent is not a JDBC BizDriver.", "Information");
                }
            }
            populateScreen();
        } catch (final Exception e) {
            logger.finest("Error reading the BizDriver file. " + e, "SQLBizDriverFileName", "setBizDriverInfo");
            logger.printStackTrace(e);
            fileChooser.setFileString(bizDriverFile);
            fileChooser.getTextField().selectAll();
            jdbcBizDriverBtn.setSelection(true);
            ControlFactory.showInfoDialog(t.getString("Error reading BizDriver file."), e.toString(), true);
        } finally {
            handleUseExistingActionForJDBCFields();
            handleUseExistingActionForJ2EEFields();
        }
    }

    /**
     * Get BizDriver type
     * 
     * @return String BizDriver type
     */
    public String getBizDriverType() {
        if (jdbcBizDriverBtn.getSelection()) {
            return GUIConstants.JDBC_BIZDRIVER;
        } else {
            return GUIConstants.JNDI_BIZDRIVER;
        }
    }

    /**
     * Get JNDI name
     * 
     * @return String JNDI name to lookup data source
     */
    public String getJndiName() {
        return j2eeJndiNameTxt.getText().trim();
    }

    /**
     * returns the jndi password label to be displayed.
     * @return string for the jndi password label.
     */
    protected String getJNDIPasswordLabel() {
        return "Password: ";
    }

    /**
     * returns the jndi login label to be displayed.
     * @return string for the jndi login label.
     */
    protected String getJNDILoginLabel() {
        return "Login: ";
    }

    /** XAwareLogger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(AbstractBizDriverWizardPanelComponent.class.getName());

    protected InputParameterDataEditorPanel inputParamEditor;

    protected DebugInputParameterEditorPanel executionInputParamEditor;

    protected String INPUT_TYPE = "input_type";

    protected String INPUT_XML = "input_xml";

    protected String INPUT_SECTION = "input";

    /** ns holds the namespace of XAwareConstants */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** Channel and Pooling tabbed folder.*/
    protected CTabFolder tabFolder;

    /** Input Tab */
    protected CTabItem inputTab;

    /** Wizard execution input tab */
    protected CTabItem executionInputTab;

    /**
     * Creates the bizdriver input parameters composite and adds the.
     *
     */
    @SuppressWarnings("unchecked")
    protected void createInputComposite() {
        Element selElem = getAdditionalNamespaces();
        if (selElem == null) {
            selElem = new Element("AdditionalNamespaces", XAwareConstants.xaNamespace);
            setAdditionalNamespaces(selElem);
        }
        Composite inputTabComposite = new Composite(tabFolder, SWT.FLAT);
        toolkit.adapt(inputTabComposite);
        toolkit.paintBordersFor(inputTabComposite);
        inputTabComposite.setLayout(new GridLayout());
        inputTabComposite.setLayoutData(new GridData());
        inputParamEditor = new InputParameterDataEditorPanel(inputTabComposite, new Vector(), false, false, false, selElem, toolkit);
        toolkit.adapt(inputParamEditor);
        toolkit.paintBordersFor(inputParamEditor);
        inputTab.setControl(inputTabComposite);
    }

    /**
     * Creates the pooling composite and adds the pooling components.
     *
     */
    protected void createExecutionInputComposite() {
        Element selElem = getAdditionalNamespaces();
        if (selElem == null) {
            selElem = new Element("AdditionalNamespaces", XAwareConstants.xaNamespace);
            setAdditionalNamespaces(selElem);
        }
        Composite executionInputTabComposite = new Composite(tabFolder, SWT.FLAT);
        toolkit.adapt(executionInputTabComposite);
        executionInputTabComposite.setLayout(new GridLayout());
        executionInputTabComposite.setLayoutData(new GridData());
        final Vector<InputParameterData> newExecInputParams = new Vector<InputParameterData>();
        executionInputParamEditor = new DebugInputParameterEditorPanel(executionInputTabComposite, newExecInputParams, "", DebugInputParameterEditorPanel.NONE, true, false, toolkit);
        this.executionInputTab.setControl(executionInputTabComposite);
    }

    /**
     * Called when the InputParametersDataEditor detectes a change in the value.
     */
    public void stateChanged(ChangeEvent e) {
        this.executionInputParamEditor.createInputParams(this.inputParamEditor.getInputParameterDataVec(), true);
    }

    protected IDialogSettings getWizardDialogSettings() {
        IDialogSettings settings = new DialogSettings("Top");
        try {
            settings.load(this.getWizardDialogSettingsFilename());
        } catch (IOException e) {
            logger.debug("Unable to load the wizards dialog settings, most likely because they haven't been saved yet", e);
        }
        return settings;
    }

    protected void saveWizardDialogSettings(IDialogSettings dialogSettings) {
        try {
            dialogSettings.save(this.getWizardDialogSettingsFilename());
        } catch (IOException e) {
            logger.debug("Unable to save the wizard dialog settings", e);
        }
    }

    protected String getWizardDialogSettingsFilename() {
        IPath path = XA_Designer_Plugin.getDefault().getStateLocation();
        return path.append("xaware_bizdriver.settings").toOSString();
    }

    protected Document loadBizDriverFile(String fileName) {
        SAXBuilder sBuilder = null;
        Document bizDriverDoc = null;
        final File bizDriverFile = ResourceUtils.getFile(fileName);
        if (!fileName.equals("") && !bizDriverFile.isDirectory()) {
            sBuilder = new SAXBuilder();
            InputStream input = null;
            try {
                input = new FileInputStream(ResourceUtils.getAbsolutePath(fileName));
            } catch (final Exception e) {
            }
            if (input != null) {
                try {
                    bizDriverDoc = sBuilder.build(input);
                } catch (final JDOMException jde) {
                    logger.finest("Error parsing BizDriver file.");
                } catch (final IOException jde) {
                    logger.finest("Error parsing BizDriver file.");
                }
            }
        }
        return bizDriverDoc;
    }

    protected void setupInputParams(Element root, String bizDriverName) {
        if (root != null) {
            final Element inputParamElem = root.getChild("input", ns);
            Vector<InputParameterData> inputParamVec = InterfaceHelper.prepareIPDVectorFromInputElement(inputParamElem, root);
            inputParamEditor.setInputParameterData(inputParamVec, false, false);
            final Element descElem = root.getChild(XAwareConstants.XAWARE_ELEMENT_DESCRIPTION, ns);
            if (descElem != null) {
                inputParamEditor.setDescription(descElem.getText());
            }
            IDialogSettings dialogSettings = this.getWizardDialogSettings();
            String inputXml = null;
            int inputType = DebugInputParameterEditorPanel.NONE;
            if (dialogSettings != null) {
                IDialogSettings bdSection = dialogSettings.getSection(bizDriverName);
                if (bdSection != null) {
                    for (InputParameterData param : inputParamVec) {
                        String value = bdSection.get(param.getName());
                        if (value != null) {
                            param.setWorkingValue(value);
                        }
                    }
                    IDialogSettings inputSection = dialogSettings.getSection(INPUT_SECTION);
                    if (inputSection != null) {
                        inputType = inputSection.getInt(INPUT_TYPE);
                        inputXml = inputSection.get(INPUT_XML);
                    }
                }
            }
            executionInputParamEditor.createInputParams(inputParamVec, true);
            if (inputType == DebugInputParameterEditorPanel.FILE) {
                executionInputParamEditor.setIncludeXmlFileName(inputXml);
            } else if (inputType == DebugInputParameterEditorPanel.TEXT) {
                executionInputParamEditor.setIncludeXmlText(inputXml);
            }
        }
    }

    /** Translator instance */
    public static final Translator t = XA_Designer_Plugin.getTranslator();

    /** hasChanged boolean */
    private boolean hasChanged = true;

    /** HelpURL */
    protected String helpURL;

    /** Element to hold any additional namespaces to be added to the root of the biz comp */
    private Element additionalNamespaces = null;

    /**
     * Sets hasChanged boolean variable
     */
    public final void setDataChanged() {
        hasChanged = true;
    }

    /**
     * Sets hasChanged boolean variable
     * 
     * @param status
     *            True if screen has changed
     */
    public final void setDataChanged(final boolean status) {
        hasChanged = status;
    }

    /**
     * returns true if page changed
     * 
     * @return returns true if page changed
     */
    protected final boolean hasChanged() {
        return hasChanged;
    }

    /**
     * Set the control for the receiver.
     * @param newControl


    /**
     * @return Returns the additionalNamespaces.
     */
    public Element getAdditionalNamespaces() {
        return additionalNamespaces;
    }

    /**
     * @param additionalNamespaces
     *            The additionalNamespaces to set.
     */
    public void setAdditionalNamespaces(final Element additionalNamespaces) {
        this.additionalNamespaces = additionalNamespaces;
    }

    @SuppressWarnings("unchecked")
    protected void storeAdditionalNamespaces(final Element root) {
        Element selElem = getAdditionalNamespaces();
        if (selElem == null) {
            selElem = new Element("AdditionalNamespaces", XAwareConstants.xaNamespace);
            setAdditionalNamespaces(selElem);
        }
        final List additionalNSList = root.getAdditionalNamespaces();
        if (additionalNSList != null && !additionalNSList.isEmpty()) {
            for (final Iterator addNSIter = additionalNSList.iterator(); addNSIter.hasNext(); ) {
                final Namespace ns = (Namespace) addNSIter.next();
                selElem.addNamespaceDeclaration(ns);
            }
        }
    }

    /**
     * Fires the specific event on specified widget.
     * 
     * @param eventType event type.
     * @param widget widget to trigger.
     */
    protected void fireEvent(int eventType, Widget widget) {
        Event event = new Event();
        event.widget = widget;
        widget.notifyListeners(eventType, event);
    }
}
