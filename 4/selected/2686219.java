package org.xaware.ide.xadev.wizardpanels;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.datamodel.HttpBizDriverInfo;
import org.xaware.ide.xadev.datamodel.InputParameterData;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardFactory;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * BizDriver Panel for Http BizComponent.
 * 
 * @author Satish
 */
public class HttpBizDriverPanel extends WizardPanelComponent implements SelectionListener, ModifyListener, VerifyListener {

    /** ns holds the namespace of XAwareConstants */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** XAwareLogger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(HttpBizDriverPanel.class.getName());

    /** Composite for the page */
    protected Composite bizDriverComp;

    /** Use Existing BizDriver Radio Button */
    protected Button useExistingBtn;

    /** Create New BizDriver Radio Button */
    protected Button createNewBtn;

    /** BizDriver file chooser */
    protected FileChooserWithLabeling fileChooser;

    /** Stores the path */
    private String initPath;

    /** vector which holds input parameters. */
    private Vector inputParams;

    /** save button for the bizDriver. */
    private Button saveBizDriverButton;

    /** Combo for url */
    private Combo urlCombo;

    /** Combo for uid */
    private Combo uidCombo;

    /** Combo for pwd */
    private Combo pwdCombo;

    /** Text for proxy_uid */
    private Text proxyUidText;

    /** Text for proxy_pwd */
    private Text proxyPwdText;

    /** Text for proxy_host */
    private Text proxyHostText;

    /** Text for proxy_port */
    private Text proxyPortText;

    /** Text box for description of bizdriver. */
    private Text descText;

    /**
     * Creates the FileBizDriver Panel instance.
     * 
     * @param name
     */
    public HttpBizDriverPanel(String name) {
        super(name);
    }

    @Override
    protected Object getData() throws WizardException {
        HttpBizDriverInfo info = new HttpBizDriverInfo();
        info.setUseExisting(useExistingBtn.getSelection());
        info.setBizDriverFileName(fileChooser.getFileString());
        info.setUrl(urlCombo.getText());
        info.setUser(uidCombo.getText());
        info.setPwd(pwdCombo.getText());
        info.setProxyUser(proxyUidText.getText());
        info.setProxyPwd(proxyPwdText.getText());
        info.setProxyHost(proxyHostText.getText());
        info.setProxyPort(proxyPortText.getText());
        info.setDescription(descText.getText().trim());
        ArrayList<InputParameterData> bizDriverParams = new ArrayList<InputParameterData>();
        getBizDriverParameter(info.getUrl(), bizDriverParams);
        getBizDriverParameter(info.getUser(), bizDriverParams);
        getBizDriverParameter(info.getPwd(), bizDriverParams);
        info.setBizDriverParams(bizDriverParams);
        return info;
    }

    /**
     * Returns the list of biz driver input parameters, if the fileName is an input parameter.
     * 
     * @param fileName
     *            name of the file
     * @return Arraylist of bizdriver input parameters.
     */
    @SuppressWarnings("unchecked")
    public void getBizDriverParameter(String item, ArrayList<InputParameterData> bizDriverParams) {
        if (item.startsWith("%") && item.endsWith("%") && (item.length() > 2)) {
            Iterator<InputParameterData> inputParamsIterator = inputParams.iterator();
            while (inputParamsIterator.hasNext()) {
                final InputParameterData ipd = (InputParameterData) inputParamsIterator.next();
                if (item.replaceAll("%", "").equalsIgnoreCase(ipd.getName())) {
                    if (!bizDriverParams.contains(ipd)) {
                        bizDriverParams.add(ipd);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public String getMissingDataMessage() {
        return "";
    }

    @Override
    public void initFromDependencyData(Vector inputData) {
        removeAll();
        if (inputData.size() > 0) {
            inputParams = (Vector) inputData.elementAt(0);
        }
        initPath = XA_Designer_Plugin.getActiveEditedFileDirectory();
        if (initPath == null) {
            initPath = XA_Designer_Plugin.getXAwareRootPath();
        }
        Composite pageComposite = getPageComposite();
        GridLayout gridLayout = new GridLayout(1, true);
        GridData gridData = new GridData();
        pageComposite.setLayout(gridLayout);
        pageComposite.setLayoutData(gridData);
        bizDriverComp = new Composite(pageComposite, SWT.NONE);
        bizDriverComp.setLayout(new GridLayout());
        gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER);
        bizDriverComp.setLayoutData(gridData);
        useExistingBtn = new Button(bizDriverComp, SWT.RADIO);
        useExistingBtn.setText(t.getString("Use Existing BizDriver"));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        useExistingBtn.setLayoutData(gridData);
        useExistingBtn.addSelectionListener(this);
        createNewBtn = new Button(bizDriverComp, SWT.RADIO);
        createNewBtn.setText(t.getString("Create New BizDriver"));
        createNewBtn.setSelection(true);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        createNewBtn.setLayoutData(gridData);
        createNewBtn.addSelectionListener(this);
        final Composite containerComposite = new Composite(bizDriverComp, SWT.NONE);
        containerComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        gridLayout = new GridLayout();
        gridLayout.numColumns = 4;
        containerComposite.setLayout(gridLayout);
        fileChooser = new FileChooserWithLabeling(containerComposite, SWT.NONE, "BizDriver File Name: ", "", "Browse...", XAChooser.BIZ_DRIVER_TYPE);
        fileChooser.modifyListenerForTextField(this);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        fileChooser.setLayoutData(gridData);
        gridData = new GridData();
        gridData.widthHint = 217;
        fileChooser.getTextField().setLayoutData(gridData);
        fileChooser.getTextField().setText(XA_Designer_Plugin.getActiveEditedInternalFrameFileDirectory());
        fileChooser.setMode(createNewBtn.getSelection() ? SWT.SAVE : SWT.OPEN);
        saveBizDriverButton = ControlFactory.createButton(containerComposite, t.getString("Save BizDriver"));
        saveBizDriverButton.addSelectionListener(this);
        final Label urlLbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        gridData.widthHint = 75;
        urlLbl.setLayoutData(gridData);
        urlLbl.setText(t.getString("URL:"));
        urlCombo = new Combo(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        urlCombo.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        Label fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        addInputParamsToCombo(urlCombo);
        Label lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("User Id:"));
        uidCombo = new Combo(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        uidCombo.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        addInputParamsToCombo(uidCombo);
        lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("Password:"));
        pwdCombo = new Combo(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        pwdCombo.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        addInputParamsToCombo(pwdCombo);
        lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("Proxy User Id:"));
        proxyUidText = new Text(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        proxyUidText.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("Proxy Password:"));
        proxyPwdText = new Text(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        proxyPwdText.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("Proxy Host:"));
        proxyHostText = new Text(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        proxyHostText.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        lbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        lbl.setLayoutData(gridData);
        lbl.setText(t.getString("Proxy Port:"));
        proxyPortText = new Text(containerComposite, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 200;
        proxyPortText.setLayoutData(gridData);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        fill = new Label(containerComposite, SWT.NONE);
        fill.setLayoutData(gridData);
        final Label descLabel = new Label(containerComposite, SWT.NONE);
        descLabel.setText("Description:");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.BEGINNING;
        gd.verticalAlignment = SWT.BEGINNING;
        descLabel.setLayoutData(gd);
        descText = ControlFactory.createText(containerComposite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 70;
        gridData.horizontalSpan = 3;
        descText.setLayoutData(gridData);
    }

    /**
	 * Adds the input parameters as values for the given combo.
	 * @param combo combo to which values are to be added.
	 */
    private void addInputParamsToCombo(Combo combo) {
        if (inputParams != null && !inputParams.isEmpty()) {
            for (int i = 0; i < inputParams.size(); i++) {
                combo.add("%" + ((InputParameterData) inputParams.get(i)).getName() + "%", i);
            }
        }
    }

    @Override
    public void initFromXML(Hashtable inputParams) {
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean nextPressed(Shell shell) {
        return true;
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
        if ((e.getSource() == useExistingBtn) && useExistingBtn.getSelection()) {
            handleUseExistingFields();
            saveBizDriverButton.setEnabled(false);
            setDataChanged();
        } else if ((e.getSource() == createNewBtn) && createNewBtn.getSelection()) {
            handleCreateFields();
            saveBizDriverButton.setEnabled(true);
            setDataChanged();
        } else if (e.getSource() == saveBizDriverButton) {
            if (isPanelValid()) {
                HttpBizDriverInfo info;
                try {
                    info = (HttpBizDriverInfo) getData();
                    info.saveBizDriverFile();
                    useExistingBtn.setSelection(true);
                    createNewBtn.setSelection(false);
                    handleUseExistingFields();
                } catch (WizardException wizardException) {
                    logger.severe("Error occured while saving bizdriver." + wizardException);
                }
                setDataChanged();
            }
        }
    }

    /** Handles create bizdriver request */
    private void handleCreateFields() {
        fileChooser.setMode(SWT.SAVE);
        descText.setEnabled(true);
        urlCombo.setEnabled(true);
        uidCombo.setEnabled(true);
        pwdCombo.setEnabled(true);
        proxyUidText.setEnabled(true);
        proxyPwdText.setEnabled(true);
        proxyHostText.setEnabled(true);
        proxyPortText.setEnabled(true);
    }

    /** Handles use existing bizdriver request */
    private void handleUseExistingFields() {
        fileChooser.setMode(SWT.OPEN);
        descText.setEnabled(false);
        urlCombo.setEnabled(false);
        uidCombo.setEnabled(false);
        pwdCombo.setEnabled(false);
        proxyUidText.setEnabled(false);
        proxyPwdText.setEnabled(false);
        proxyHostText.setEnabled(false);
        proxyPortText.setEnabled(false);
        populateScreen();
    }

    /** populates the fields as per the bizdriver info. */
    private void populateScreen() {
        SAXBuilder sBuilder = null;
        Document bizDriverDoc = null;
        Element root = null;
        final String fname = fileChooser.getFileString().trim();
        final File bizDriverFile = ResourceUtils.getFile(fname);
        if (!fname.equals("") && !bizDriverFile.isDirectory()) {
            sBuilder = new SAXBuilder();
            InputStream input = null;
            try {
                input = new FileInputStream(ResourceUtils.getAbsolutePath(fname));
            } catch (final Exception e) {
                logger.finest("Exception occured while reading bizdriver:" + e.getMessage(), e);
            }
            if (input != null) {
                try {
                    bizDriverDoc = sBuilder.build(bizDriverFile);
                } catch (JDOMException e) {
                    logger.fine("Error parsing biz driver file.", e);
                } catch (IOException e) {
                    logger.fine("Error parsing biz driver file.", e);
                }
                try {
                    root = bizDriverDoc.getRootElement();
                    String s = root.getChildText("description", XAwareConstants.xaNamespace);
                    if (s != null) {
                        descText.setText(s);
                    }
                    Element bizDriverHttpElement = root.getChild("http", XAwareConstants.xaNamespace);
                    s = bizDriverHttpElement.getChildText("url", XAwareConstants.xaNamespace);
                    if (s != null) {
                        urlCombo.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("user", XAwareConstants.xaNamespace);
                    if (s != null) {
                        uidCombo.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("pwd", XAwareConstants.xaNamespace);
                    if (s != null) {
                        pwdCombo.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("proxy_user", XAwareConstants.xaNamespace);
                    if (s != null) {
                        proxyUidText.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("proxy_pwd", XAwareConstants.xaNamespace);
                    if (s != null) {
                        proxyPwdText.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("proxy_host", XAwareConstants.xaNamespace);
                    if (s != null) {
                        proxyHostText.setText(s);
                    }
                    s = bizDriverHttpElement.getChildText("proxy_port", XAwareConstants.xaNamespace);
                    if (s != null) {
                        proxyPortText.setText(s);
                    }
                } catch (Exception exception) {
                    logger.fine("Error parsing elements from biz driver file.");
                }
            }
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
                handleUseExistingFields();
                setDataChanged();
            }
        }
    }

    /**
     * This method specifies whether the Radio button selected is Select or Insert or Insert/Update or Update and adds
     * the selected type to the Element.
     * 
     * @return Element contains selected type of the Radio button.
     */
    public Element buildDesignerElement() {
        final Element designerElem = new Element("wizard_step", ns);
        return designerElem;
    }

    /**
     * This method checks whether the Driver for the File BizComponent is valid or not.
     * 
     * @return Boolean
     */
    protected boolean isBizDriverPanelValid() {
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
        if (urlCombo.getText().trim().equals("")) {
            ControlFactory.showMessageDialog(t.getString("Please enter a URL."), t.getString("Information"));
            return false;
        }
        String dirName = bizDriverFile.getParent();
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = Path.fromOSString(initPath).addTrailingSeparator().toString();
            fileChooser.setFileString(dirName + fileChooser.getFileString());
            fileChooser.getTextField().selectAll();
        } else if (dirName.trim().equals(File.separator)) {
            dirName = initPath;
            fileChooser.setFileString(dirName.substring(0, dirName.length() - 1) + fileChooser.getFileString());
            fileChooser.getTextField().selectAll();
        } else {
            dirName = dirName.trim();
            if (Path.fromOSString(dirName).hasTrailingSeparator()) {
                fileChooser.setFileString(dirName + bizDriverFile.getName().trim());
                fileChooser.getTextField().selectAll();
            } else {
                fileChooser.getTextField().selectAll();
            }
        }
        final File tmpFile = ResourceUtils.getDirectory(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog(t.getString("Invalid directory for BizDriver file."), t.getString("Information"));
            return false;
        }
        fname = fileChooser.getFileString().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (createNewBtn.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
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
            if (bizDriverFile.exists()) {
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
        return true;
    }

    @Override
    protected boolean isPanelValid() {
        return isBizDriverPanelValid();
    }

    @Override
    public void initFromBizCompRoot() {
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
        try {
        } catch (final Exception e) {
            logger.finest("Error reading the BizDriver file. " + e, "FileBizDriverPanel", "setBizDriverInfo");
            logger.printStackTrace(e);
            fileChooser.setFileString(bizDriverFile);
            fileChooser.getTextField().selectAll();
            ControlFactory.showInfoDialog(t.getString("Error reading BizDriver file."), e.toString(), true);
        } finally {
            handleUseExistingFields();
        }
    }

    /**
     * Fires when modification in the text box
     * 
     * @param e
     *            VerifyEvent
     */
    public void verifyText(final VerifyEvent e) {
        setDataChanged();
    }

    /**
     * To check whether input parameter is used as File name.
     * 
     * @param name
     *            String to be checked.
     * 
     * @return boolean
     */
    public boolean isInputParam(String name) {
        if (name.startsWith("%") && name.endsWith("%") && (name.length() > 2)) {
            name = name.substring(1, name.length() - 1);
            if ((inputParams != null) && (inputParams.size() > 0)) {
                for (int i = 0; i < inputParams.size(); i++) {
                    final InputParameterData param = (InputParameterData) inputParams.get(i);
                    if (param.getName().equals(name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
