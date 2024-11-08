package org.xaware.ide.xadev.wizardpanels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.ide.xadev.wizard.Wizard;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class is used for getting the Initial context for the Biz Driver.
 * 
 * @author Srinivas Ch
 * @author bhavanikumarg
 * @author Srinivas Reddy D
 * @version 1.0
 */
public class InitialContextBizDriverFileName extends WizardPanelComponent implements SelectionListener, ModifyListener, FocusListener {

    /** XAwareLogger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(InitialContextBizDriverFileName.class.getName());

    /** ns holds the namespace of XAwareConstants */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** Translator used for Localisation */
    public static final Translator translator = XA_Designer_Plugin.getTranslator();

    /** page Composite on which controls to be placed */
    private Composite pageComp;

    private final String bizDriverType = "J2EE BizDriver";

    /** existsBtn represents "Use Existing Biz Driver" radio button. */
    private Button useExistingBtn;

    /** newBtn represents "Create New Biz Driver" radio button. */
    private Button createNewBtn;

    /**
     * Used to create composite on which the biz driver file Name Lbl, biz driver file Name Txt, browseBtn are added.
     */
    private FileChooserWithLabeling fileChooser;

    /** loginLbl is a label for loginTxt. */
    private Label loginLbl;

    /** loginTxt is a Textbox for entering the Login. */
    private Text loginTxt;

    /** passwordLbl is a label for passwordTxt. */
    private Label passwordLbl;

    /** passwordTxt is a Textbox for entering the Password. */
    private Text passwordTxt;

    /** hostLbl is a label for hostCmb. */
    private Label hostLbl;

    /**
     * hostCmb is a Combo which contains list of available Hosts It is populated from the
     * UserPrefs.getContextHostList().
     */
    private Combo hostCmb;

    /** factoryLbl is the label for factoryCmb. */
    private Label factoryLbl;

    /**
     * factoryCmb is a Combo which contains list of available Factories. It is populated from the
     * UserPrefs.getContextFactoryList().
     */
    private Combo factoryCmb;

    /** dataSourceDescriptionLbl is the label for dataSourceDescriptionTxt. */
    private Label dataSourceDescriptionLbl;

    /**
     * dataSourceDescriptionTxt is a textbox for entering the dataSourceDescription.
     */
    private Text dataSourceDescriptionTxt;

    /**
     * singletonBtn is a checkbox button for whether the biz driver should have a single instance.
     */
    private Button singletonBtn;

    /** rootElement has the rootelement of inputParams's hashtable */
    private String rootElement;

    /**
     * Creates a new InitialContextBizDriverFileName object.
     * 
     * @param pageName -
     *            the name of the page
     */
    public InitialContextBizDriverFileName(final String pageName) {
        super(pageName);
    }

    /**
     * Initializes the page from XML
     * 
     * @param inputParams -
     *            input from UserConfig XML file.
     */
    @Override
    public void initFromXML(final Hashtable inputParams) {
        pageComp = getPageComposite();
        pageComp.setLayout(new GridLayout());
        final Composite rootComp = new Composite(pageComp, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        GridData gridData = new GridData(GridData.FILL_BOTH);
        rootComp.setLayout(gridLayout);
        rootComp.setLayoutData(gridData);
        final Composite childComp = new Composite(rootComp, SWT.NONE);
        gridLayout = new GridLayout(3, false);
        gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER);
        gridLayout.verticalSpacing = 10;
        childComp.setLayout(gridLayout);
        childComp.setLayoutData(gridData);
        useExistingBtn = new Button(childComp, SWT.RADIO);
        useExistingBtn.setText(translator.getString("Use Existing BizDriver"));
        useExistingBtn.addSelectionListener(this);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        useExistingBtn.setLayoutData(gridData);
        createNewBtn = new Button(childComp, SWT.RADIO);
        createNewBtn.setText(translator.getString("Create New BizDriver"));
        createNewBtn.addSelectionListener(this);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        createNewBtn.setLayoutData(gridData);
        fileChooser = new FileChooserWithLabeling(childComp, SWT.NONE, translator.getString("BizDriver File Name: "), XA_Designer_Plugin.getActiveEditedFileDirectory(), translator.getString("Browse..."), XAFileConstants.BIZ_DRIVER_TYPE);
        int mode = SWT.OPEN;
        if (!useExistingBtn.getSelection()) {
            mode = SWT.SAVE;
        }
        fileChooser.setMode(mode);
        fileChooser.focusListenerForTextField(this);
        fileChooser.modifyListenerForTextField(this);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        fileChooser.setLayoutData(gridData);
        gridData = new GridData();
        gridData.heightHint = 15;
        gridData.widthHint = 220;
        gridData.horizontalIndent = 23;
        fileChooser.getTextField().setLayoutData(gridData);
        loginLbl = new Label(childComp, SWT.NONE);
        loginLbl.setText(translator.getString("Login: "));
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        loginLbl.setLayoutData(gridData);
        loginTxt = ControlFactory.createText(childComp, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 265;
        loginTxt.setLayoutData(gridData);
        loginTxt.addModifyListener(this);
        passwordLbl = new Label(childComp, SWT.NONE);
        passwordLbl.setText(translator.getString("Password: "));
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        passwordLbl.setLayoutData(gridData);
        passwordTxt = new Text(childComp, SWT.BORDER | SWT.PASSWORD);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 265;
        passwordTxt.setLayoutData(gridData);
        passwordTxt.addModifyListener(this);
        hostLbl = new Label(childComp, SWT.NONE);
        hostLbl.setText(translator.getString("Host(URL): "));
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        hostLbl.setLayoutData(gridData);
        hostCmb = new Combo(childComp, SWT.SINGLE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        hostCmb.setLayoutData(gridData);
        hostCmb.addSelectionListener(this);
        final List hostList = UserPrefs.getContextHostList();
        for (int i = 0; i < hostList.size(); i++) {
            hostCmb.add((String) hostList.get(i), i);
        }
        if (hostCmb.getItemCount() > 0) {
            hostCmb.setText(hostCmb.getItem(0));
        }
        factoryLbl = new Label(childComp, SWT.NONE);
        factoryLbl.setText(translator.getString("Factory: "));
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        factoryLbl.setLayoutData(gridData);
        factoryCmb = new Combo(childComp, SWT.SINGLE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        factoryCmb.setLayoutData(gridData);
        factoryCmb.addSelectionListener(this);
        final List factoryList = UserPrefs.getContextFactoryList();
        for (int j = 0; j < factoryList.size(); j++) {
            factoryCmb.add((String) factoryList.get(j), j);
        }
        if (factoryCmb.getItemCount() > 0) {
            factoryCmb.setText(factoryCmb.getItem(0));
        }
        dataSourceDescriptionLbl = new Label(childComp, SWT.NONE);
        dataSourceDescriptionLbl.setText(translator.getString("Data Source Description: "));
        gridData = new GridData();
        gridData.horizontalSpan = 1;
        dataSourceDescriptionLbl.setLayoutData(gridData);
        dataSourceDescriptionTxt = ControlFactory.createText(childComp, SWT.BORDER);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.heightHint = 15;
        gridData.widthHint = 265;
        dataSourceDescriptionTxt.setLayoutData(gridData);
        singletonBtn = new Button(childComp, SWT.CHECK);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        singletonBtn.setText(translator.getString("Singleton"));
        singletonBtn.setLayoutData(gridData);
        if (inputParams != null) {
            rootElement = (String) inputParams.get(translator.getString("rootelement"));
        } else {
            rootElement = new String(translator.getString("jms"));
        }
        createNewBtn.setSelection(true);
    }

    /**
     * Initializes the page from DependencyData
     * 
     * @param inputData -
     *            Dependency data for the wizard page
     */
    @Override
    public void initFromDependencyData(final Vector inputData) {
    }

    /**
     * returns the missing data message
     * 
     * @return ""
     */
    @Override
    public String getMissingDataMessage() {
        return "";
    }

    /**
     * getData() method returns the file name of the Biz driver used.
     * 
     * @return File name of the Biz driver.
     * 
     * @throws WizardException
     *             wraps exception when retriving data
     */
    @Override
    protected Object getData() throws WizardException {
        if (createNewBtn.getSelection()) {
            try {
                final FileOutputStream fileOut = new FileOutputStream(ResourceUtils.getFile(fileChooser.getFileString()).toString());
                final Element root = new Element(rootElement);
                root.setAttribute(new Attribute(translator.getString("bizdrivertype"), translator.getString("J2EECONTEXT"), ns));
                if (singletonBtn.getSelection()) {
                    root.setAttribute(new Attribute(translator.getString("singleton"), translator.getString("yes"), ns));
                } else {
                    root.setAttribute(new Attribute(translator.getString("singleton"), translator.getString("no"), ns));
                }
                final Element child = new Element(translator.getString("initialcontext"), ns);
                Element toAdd = new Element(translator.getString("uid"), ns);
                toAdd.setText(loginTxt.getText());
                child.addContent(toAdd);
                toAdd = new Element(translator.getString("pwd"), ns);
                toAdd.setText(new String(passwordTxt.getText()));
                child.addContent(toAdd);
                toAdd = new Element(translator.getString("host"), ns);
                toAdd.setText(hostCmb.getText());
                child.addContent(toAdd);
                toAdd = new Element(translator.getString("factory"), ns);
                toAdd.setText(factoryCmb.getText());
                child.addContent(toAdd);
                root.addContent(child);
                toAdd = new Element(translator.getString("description"), ns);
                toAdd.setText(dataSourceDescriptionTxt.getText().trim());
                root.addContent(toAdd);
                UserPrefs.getFileOutputter().output(new Document(root), fileOut);
                fileOut.close();
            } catch (final Exception e) {
                logger.finest("Exception saving BizDriver: " + e);
                logger.printStackTrace(e);
                throw new WizardException(translator.getString("Error Saving BizDriver"));
            }
        }
        return fileChooser.getFileString();
    }

    /**
     * isPanelValid() method checks whether the panel is valid. Validates the Biz driver used.
     * 
     * @return Boolean
     */
    @Override
    protected boolean isPanelValid() {
        String fname = fileChooser.getFileString().trim();
        fileChooser.setFileString(fname);
        if (fname.equals("")) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        File bizDriverFile = ResourceUtils.getFile(fname);
        if (bizDriverFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        if (bizDriverFile.getName().trim().equals(translator.getString(".xdr"))) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        if ((bizDriverFile.getAbsolutePath().indexOf('#') > -1) || (bizDriverFile.getAbsolutePath().indexOf('%') > -1)) {
            ControlFactory.showMessageDialog(translator.getString("Please do not enter # or % in the file path or name."), translator.getString("Information"));
            return false;
        }
        String dirName = bizDriverFile.getParent();
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
                dirName = dirName + File.separator;
            }
            fileChooser.setFileString(dirName + fileChooser.getFileString());
        } else if (dirName.trim().equals(File.separator)) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            fileChooser.setFileString(dirName.substring(0, dirName.length() - 1) + fileChooser.getFileString());
        } else {
            dirName = dirName.trim();
            if (dirName.charAt(dirName.length() - 1) == File.separatorChar) {
                fileChooser.setFileString(dirName + bizDriverFile.getName().trim());
            } else {
                fileChooser.setFileString(fname);
            }
        }
        final File tmpFile = new File(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Invalid directory for BizDriver file."), translator.getString("Information"));
            return false;
        }
        fname = fileChooser.getFileString().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (createNewBtn.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
            if (fileNameStr.indexOf('.') == -1) {
                fname = fname + ".xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            } else if (fileNameStr.indexOf('.') == (fileNameStr.length() - 1)) {
                fname = fname + "xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            }
            if (bizDriverFile.exists()) {
                final int choice = ControlFactory.showConfirmDialog(translator.getString("BizDriver file which you have entered " + "already exists. Overwrite?"));
                if (choice != Window.OK) {
                    return false;
                }
            }
        } else if (useExistingBtn.getSelection()) {
            if (bizDriverFile.isDirectory() || !bizDriverFile.exists()) {
                ControlFactory.showMessageDialog(translator.getString("BizDriver file which you have entered does not exist. Please " + "enter correct file name."), translator.getString("Information"));
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if page complete
     * 
     * @return Boolean
     */
    @Override
    public boolean isComplete() {
        final boolean retVal = true;
        return retVal;
    }

    /**
     * populateScreen() method populates the wizard page with the details of the Biz driver.
     */
    public void populateScreen() {
        SAXBuilder sBuilder = null;
        Document bizDriverDoc = null;
        Element root = null;
        Attribute attr = null;
        Element bizDriverConnElem = null;
        Element bizDriverElem = null;
        final String fname = fileChooser.getFileString().trim();
        final File bizDriverFile = ResourceUtils.getFile(fname);
        String uid = "";
        String pwd = "";
        String host = "";
        String factory = "";
        String singletonStr = "";
        String dataSourceDesc = "";
        if (!fname.equals("") && !bizDriverFile.isDirectory()) {
            sBuilder = new SAXBuilder();
            InputStream input = null;
            try {
                input = new FileInputStream(bizDriverFile.toString());
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
                try {
                    root = bizDriverDoc.getRootElement();
                    bizDriverConnElem = root.getChild(translator.getString("initialcontext"), ns);
                    bizDriverElem = bizDriverConnElem.getChild(translator.getString("uid"), ns);
                    uid = bizDriverElem.getText();
                    bizDriverElem = bizDriverConnElem.getChild(translator.getString("pwd"), ns);
                    pwd = bizDriverElem.getText();
                    bizDriverElem = bizDriverConnElem.getChild(translator.getString("host"), ns);
                    host = bizDriverElem.getText();
                    bizDriverElem = bizDriverConnElem.getChild(translator.getString("factory"), ns);
                    factory = bizDriverElem.getText();
                    attr = root.getAttribute(translator.getString("singleton"), ns);
                    if (attr != null) {
                        singletonStr = attr.getValue();
                    }
                    final Element descElem = root.getChild(translator.getString("description"), ns);
                    if (descElem != null) {
                        dataSourceDesc = descElem.getText();
                    }
                } catch (final Throwable ex) {
                    logger.finest("Error parsing elements from BizDriver file.");
                }
            }
            loginTxt.setText(uid);
            passwordTxt.setText(pwd);
            hostCmb.setText(host);
            factoryCmb.setText(factory);
            if (singletonStr.equals("yes")) {
                singletonBtn.setSelection(true);
            } else {
                singletonBtn.setSelection(false);
            }
            dataSourceDescriptionTxt.setText(dataSourceDesc);
        }
    }

    /**
     * returns true if next pressed.
     * 
     * @param shell
     * 
     * @return Boolean
     */
    @Override
    public boolean nextPressed(final Shell shell) {
        return true;
    }

    /**
     * fileNameTxt's ModifyListener
     * 
     * @param e
     *            modify event
     */
    public void modifyText(final ModifyEvent e) {
        setDataChanged();
        if (e.getSource().equals(loginTxt)) {
        } else if (e.getSource().equals(passwordTxt)) {
        } else if (e.getSource() == fileChooser.getTextField()) {
            if (useExistingBtn.getSelection()) {
                populateScreen();
            }
        }
    }

    /**
     * Initializes the page from BizComponent Root
     */
    @Override
    public void initFromBizCompRoot() {
        final Element root = Wizard.getBizCompRoot();
        try {
            if (root != null) {
                final String bizDriver = root.getAttribute(translator.getString("bizdriver"), ns).getValue();
                fileChooser.setFileString(bizDriver);
                useExistingBtn.setSelection(true);
                useExistingBtn.setFocus();
                createNewBtn.setSelection(false);
                loginTxt.setEnabled(false);
                loginTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                passwordTxt.setEnabled(false);
                passwordTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                hostCmb.setEnabled(false);
                hostCmb.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                factoryCmb.setEnabled(false);
                factoryCmb.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                dataSourceDescriptionTxt.setEnabled(false);
                dataSourceDescriptionTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                singletonBtn.setEnabled(false);
                int mode = SWT.OPEN;
                if (!useExistingBtn.getSelection()) {
                    mode = SWT.SAVE;
                }
                fileChooser.setMode(mode);
                populateScreen();
            }
        } catch (final Throwable ex) {
            final String message = bizDriverType + " data could not be loaded completely. Exception:" + ex.getMessage();
            logger.severe(message);
            ControlFactory.showInfoDialog(translator.getString(bizDriverType + " data could not be loaded completely."), message);
        }
    }

    /**
     * The method widgetSelected() handles selection events for registered controls
     * 
     * @param e
     *            Selection Event
     */
    public void widgetSelected(final SelectionEvent e) {
        setDataChanged();
        if ((e.getSource() == useExistingBtn) && useExistingBtn.getSelection()) {
            loginTxt.setEnabled(false);
            loginTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            passwordTxt.setEnabled(false);
            passwordTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            hostCmb.setEnabled(false);
            hostCmb.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            factoryCmb.setEnabled(false);
            factoryCmb.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            dataSourceDescriptionTxt.setEnabled(false);
            dataSourceDescriptionTxt.setBackground(pageComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            singletonBtn.setEnabled(false);
            int mode = SWT.OPEN;
            if (!useExistingBtn.getSelection()) {
                mode = SWT.SAVE;
            }
            fileChooser.setMode(mode);
            populateScreen();
        } else if ((e.getSource() == createNewBtn) && createNewBtn.getSelection()) {
            loginTxt.setEnabled(true);
            passwordTxt.setEnabled(true);
            hostCmb.setEnabled(true);
            factoryCmb.setEnabled(true);
            dataSourceDescriptionTxt.setEnabled(true);
            singletonBtn.setEnabled(true);
            if (hostCmb.getText().equals("") && (hostCmb.getItemCount() > 0)) {
                hostCmb.setText(hostCmb.getItem(0));
            }
            if (factoryCmb.getText().equals("") && (factoryCmb.getItemCount() > 0)) {
                factoryCmb.setText(factoryCmb.getItem(0));
            }
            int mode = SWT.OPEN;
            if (!useExistingBtn.getSelection()) {
                mode = SWT.SAVE;
            }
            fileChooser.setMode(mode);
        } else if (e.getSource() == hostCmb) {
        } else if (e.getSource() == factoryCmb) {
        }
    }

    /**
     * The method widgetSelected() handles selection events for registered controls
     * 
     * @param e
     *            Selection Event
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }

    /**
     * The method focusGained() handles focus events for registered controls
     * 
     * @param e
     *            Focus Event
     */
    public void focusGained(final FocusEvent e) {
    }

    /**
     * The method focusLost() handles focus events for registered controls
     * 
     * @param e
     *            Focus Event
     */
    public void focusLost(final FocusEvent e) {
        if (useExistingBtn.getSelection()) {
        } else if (createNewBtn.getSelection()) {
        }
    }
}
