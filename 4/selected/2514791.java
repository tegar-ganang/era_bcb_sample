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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.datamodel.FileBizDriverInfo;
import org.xaware.ide.xadev.datamodel.InputParameterData;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.gui.XAFileChooser;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.ide.xadev.wizard.Wizard;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * BizDriver Panel for File BizComponent.
 * @author Satish
 */
public class FileBizDriverPanel extends WizardPanelComponent implements SelectionListener, ModifyListener, VerifyListener {

    /** ns holds the namespace of XAwareConstants */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** XAwareLogger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(FileBizDriverPanel.class.getName());

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

    /** RecordBrowse Button Control. */
    private Button recordBrowseBtn;

    /** Holds instance of requsttype combo. */
    protected Combo requestTypeCmb;

    /** Holds reference to array of Strings. */
    private String[] requestTypes;

    /** vector which holds input parameters. */
    private Vector inputParams;

    /**save button for the bizDriver.*/
    private Button saveBizDriverButton;

    /**combo for file path.*/
    private Combo filePathCmb;

    /**Text box for description of bizdriver.*/
    private Text descText;

    private boolean saved;

    /**
	 * Creates the FileBizDriver Panel instance.
	 * @param name
	 */
    public FileBizDriverPanel(String name) {
        super(name);
        requestTypes = new String[3];
        requestTypes[0] = FileBizDriverInfo.FILE_READ;
        requestTypes[1] = FileBizDriverInfo.FILE_WRITE;
        requestTypes[2] = FileBizDriverInfo.FILE_APPEND;
    }

    @Override
    protected Object getData() throws WizardException {
        FileBizDriverInfo info = new FileBizDriverInfo(useExistingBtn.getSelection());
        Element designerElement = null;
        info.setBizDriverFileName(this.fileChooser.getFileString());
        info.setFile(this.filePathCmb.getText().trim());
        if (this.requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_READ)) {
            info.setRequest_type(FileBizDriverInfo.FILE_READ);
        } else if (this.requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_WRITE)) {
            info.setRequest_type(FileBizDriverInfo.FILE_WRITE);
        } else if (this.requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_APPEND)) {
            info.setRequest_type(FileBizDriverInfo.FILE_APPEND);
        }
        info.setBizDriverParams(getBizDriverParameters(info.getFile()));
        info.setDescription(descText.getText().trim());
        designerElement = buildDesignerElement();
        info.setDesignerElement(designerElement);
        final Element root = info.getBizDriver();
        info.setDocument(new Document(root));
        return info;
    }

    /**
	 * Returns the list of biz driver input parameters, if the fileName is an input parameter.
	 * @param fileName name of the file 
	 * @return Arraylist of bizdriver input parameters.
	 */
    @SuppressWarnings("unchecked")
    public ArrayList<InputParameterData> getBizDriverParameters(String fileName) {
        ArrayList<InputParameterData> bizDriverParams = new ArrayList<InputParameterData>();
        if (fileName.startsWith("%") && fileName.endsWith("%") && (fileName.length() > 2)) {
            Iterator<InputParameterData> inputParamsIterator = inputParams.iterator();
            while (inputParamsIterator.hasNext()) {
                final InputParameterData ipd = inputParamsIterator.next();
                if (fileName.replaceAll("%", "").equalsIgnoreCase(ipd.getName())) {
                    bizDriverParams.add(ipd);
                    break;
                }
            }
        }
        return bizDriverParams;
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
        final Label fileLbl = new Label(containerComposite, SWT.NONE);
        gridData = new GridData();
        gridData.widthHint = fileChooser.getLabel().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        fileLbl.setLayoutData(gridData);
        fileLbl.setText(t.getString("File:"));
        filePathCmb = new Combo(containerComposite, SWT.BORDER);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gridData.widthHint = 200;
        filePathCmb.addVerifyListener(this);
        filePathCmb.setText(XA_Designer_Plugin.getActiveEditedInternalFrameFileDirectory());
        filePathCmb.setLayoutData(gridData);
        if (inputParams != null && !inputParams.isEmpty()) {
            for (int i = 0; i < inputParams.size(); i++) {
                filePathCmb.add("%" + ((InputParameterData) inputParams.get(i)).getName() + "%", i);
            }
        }
        recordBrowseBtn = ControlFactory.createButton(containerComposite, t.getString("Browse..."));
        recordBrowseBtn.addSelectionListener(this);
        gridData = new GridData();
        gridData.widthHint = 75;
        gridData.horizontalSpan = 2;
        recordBrowseBtn.setLayoutData(gridData);
        final Label reqTypeLbl = new Label(containerComposite, SWT.NONE);
        reqTypeLbl.setText(t.getString("Request type:"));
        gridData = new GridData();
        reqTypeLbl.setLayoutData(gridData);
        requestTypeCmb = new Combo(containerComposite, SWT.READ_ONLY);
        for (int i = 0; i < requestTypes.length; i++) {
            requestTypeCmb.add(requestTypes[i], i);
        }
        requestTypeCmb.select(0);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        requestTypeCmb.setLayoutData(gridData);
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
            setDataChangedLocal();
        } else if ((e.getSource() == createNewBtn) && createNewBtn.getSelection()) {
            handleCreateFields();
            saveBizDriverButton.setEnabled(true);
            setDataChangedLocal();
        } else if (e.getSource() == recordBrowseBtn) {
            XAChooser chooser = null;
            String returnVal = "";
            if (requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_WRITE) || requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_APPEND)) {
                final XAFileChooser fileChooser = new XAFileChooser(Display.getCurrent().getActiveShell(), filePathCmb.getText(), "Open", SWT.SINGLE | SWT.OPEN);
                fileChooser.addDefaultFilter(XAFileConstants.TEXT);
                fileChooser.addFilter(XAFileConstants.ALL_ONLY);
                returnVal = fileChooser.open();
                if (returnVal != null) {
                    filePathCmb.setText(returnVal.replace('\\', '/'));
                    setDataChangedLocal();
                }
            } else {
                if ((filePathCmb.getText() != null) && !filePathCmb.getText().equals("")) {
                    chooser = new XAChooser(getShell(), filePathCmb.getText(), SWT.OPEN);
                    chooser.addDefaultFilter(XAChooser.TEXT);
                    chooser.addFilter(XAChooser.ALL_ONLY);
                } else {
                    chooser = new XAChooser(getShell(), filePathCmb.getText(), SWT.OPEN);
                    chooser.addFilter(XAChooser.ALL_ONLY);
                }
                returnVal = chooser.openResource();
                if (returnVal != null) {
                    filePathCmb.setText(returnVal);
                    setDataChangedLocal();
                }
            }
        } else if (e.getSource() == saveBizDriverButton) {
            if (isPanelValid()) {
                FileBizDriverInfo info;
                try {
                    info = (FileBizDriverInfo) getData();
                    info.saveBizDriverFile();
                } catch (WizardException wizardException) {
                    logger.severe("Error occured while saving bizdriver." + wizardException);
                }
                setDataChangedLocal();
                saved = true;
            }
        }
    }

    /**
     * Sets hasChanged boolean variable
     */
    public void setDataChangedLocal() {
        setDataChanged();
        saved = false;
    }

    /** Handles use existing bizdriver request*/
    private void handleCreateFields() {
        fileChooser.setMode(SWT.SAVE);
        filePathCmb.setEnabled(true);
        recordBrowseBtn.setEnabled(true);
        requestTypeCmb.setEnabled(true);
        descText.setEnabled(true);
    }

    /** Handles create bizdriver request*/
    private void handleUseExistingFields() {
        fileChooser.setMode(SWT.OPEN);
        filePathCmb.setEnabled(false);
        recordBrowseBtn.setEnabled(false);
        requestTypeCmb.setEnabled(false);
        descText.setEnabled(false);
        populateScreen();
    }

    /** populates the fields as per the bizdriver info.*/
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
                    Element bizDriverFileElement = root.getChild("file", XAwareConstants.xaNamespace);
                    filePathCmb.setText(bizDriverFileElement.getChildTextTrim("name", XAwareConstants.xaNamespace));
                    requestTypeCmb.select(requestTypeCmb.indexOf(bizDriverFileElement.getChildTextTrim("request_type", XAwareConstants.xaNamespace)));
                    descText.setText(root.getChildText("description", XAwareConstants.xaNamespace));
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
                setDataChangedLocal();
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
        designerElem.setAttribute("name", "File BizDriver Information", ns);
        if (filePathCmb != null) {
            designerElem.setAttribute("filePath", filePathCmb.getText(), ns);
        }
        if (requestTypeCmb != null) {
            designerElem.setAttribute("requestType", requestTypeCmb.getText(), ns);
        }
        if (descText != null) {
            designerElem.setAttribute("description", descText.getText(), ns);
        }
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
        if (bizDriverFile.isDirectory() && !useExistingBtn.isEnabled()) {
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
        if (filePathCmb.getText().trim().equals("")) {
            ControlFactory.showMessageDialog(t.getString("Please enter a file name."), t.getString("Information"));
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
                fileChooser.setFileString(fname);
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
            if (!saved && bizDriverFile.exists()) {
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
        if (requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_WRITE) || requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_APPEND)) {
            final File file = ResourceUtils.getFile(filePathCmb.getText().trim());
            if (file.isFile() && !useExistingBtn.getSelection()) {
                final int ret = ControlFactory.showConfirmDialog("File exists. Continue?", "Information", false);
                if (ret != MessageDialog.OK) {
                    return false;
                }
            }
        }
        if (!isInputParam(filePathCmb.getText().trim())) {
            if (requestTypeCmb.getText().equals(FileBizDriverInfo.FILE_READ)) {
                final File file = ResourceUtils.getFile(filePathCmb.getText().trim());
                if (!file.isFile()) {
                    ControlFactory.showMessageDialog(t.getString("Please enter existing file name or " + "valid input parameter references."), t.getString("Information"));
                    return false;
                }
            } else {
                final File file = ResourceUtils.getFile(filePathCmb.getText().trim());
                String parentFile = file.getParent();
                if (parentFile == null) {
                    if (!isInputParam(filePathCmb.getText().trim())) {
                        ControlFactory.showMessageDialog(t.getString("Please enter valid file path or" + " valid input parameter references." + "Instead of: " + file.getPath()), t.getString("Information"));
                        return false;
                    }
                    parentFile = ".";
                }
                logger.finest(parentFile);
                if (!new File(parentFile).isDirectory()) {
                    ControlFactory.showMessageDialog(t.getString("Please enter valid file path. Instead of: " + file.getPath()), t.getString("Information"));
                    return false;
                }
                if (file.isDirectory()) {
                    ControlFactory.showMessageDialog(t.getString("Please enter a file name."), t.getString("Information"));
                    return false;
                }
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
        final Element root = Wizard.getBizCompRoot();
        try {
            if (root != null) {
                final Element designerElem = root.getChild("designer", ns);
                final java.util.List steps = designerElem.getChildren("wizard_step", ns);
                for (int i = 0; i < steps.size(); i++) {
                    final Element stepElem = (Element) steps.get(i);
                    if (stepElem.getAttributeValue("name", ns).trim().equals("File BizDriver Information")) {
                        final String bizDriver = root.getAttribute("bizdriver", ns).getValue();
                        setBizDriverInfo(bizDriver, root);
                        final String File = stepElem.getAttributeValue("filePath", ns);
                        final String requestType = stepElem.getAttributeValue("requestType", ns);
                        final String description = stepElem.getAttributeValue("description", ns);
                        final String bizDriverFileName = root.getAttribute("bizdriver", ns).getValue();
                        fileChooser.setFileString(bizDriverFileName);
                        if (File != null) {
                            filePathCmb.setText(File);
                        }
                        if (requestType != null) {
                            requestTypeCmb.setText(requestType);
                        }
                        if (description != null) {
                            descText.setText(description);
                        }
                        useExistingBtn.setSelection(true);
                        useExistingBtn.setFocus();
                        createNewBtn.setSelection(false);
                        Event event = new Event();
                        event.widget = useExistingBtn;
                        widgetSelected(new SelectionEvent(event));
                    }
                }
            }
        } catch (final Exception ex) {
            final String message = "BizDriver data could not be loaded completely. Exception:" + ex.getMessage();
            logger.severe(message);
            ControlFactory.showInfoDialog(t.getString("BizDriver data could not be loaded completely."), message, true);
        }
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
            @SuppressWarnings("unused") final String bizDriverType = elem.getAttribute("bizdrivertype", ns).getValue();
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
        if (e.getSource() == filePathCmb) {
            setDataChangedLocal();
        }
    }

    /**
     * method to get selected requesttype.
     * 
     * @return requesttype string.
     */
    public String getRequestType() {
        return requestTypeCmb.getItem(requestTypeCmb.getSelectionIndex()).toString();
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
        if ((inputParams == null || inputParams.isEmpty())) return false;
        ArrayList<String> enteredParams = extractParams(name);
        if (enteredParams == null || enteredParams.isEmpty()) return false;
        for (String param : enteredParams) {
            boolean paramExists = false;
            for (int i = 0; i < inputParams.size(); i++) {
                final InputParameterData tempParam = (InputParameterData) inputParams.get(i);
                if (tempParam.getName().equals(param)) {
                    paramExists = true;
                    break;
                }
            }
            if (!paramExists) return false;
        }
        return true;
    }

    /**
     * Extracts the values between percentiles and returns the list of values.
     * if */
    private ArrayList<String> extractParams(String name) {
        ArrayList<String> params = new ArrayList<String>();
        String val = "";
        int percentileCount = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '%') {
                percentileCount++;
                final int endIndex = name.indexOf('%', i + 1);
                if (endIndex != -1) {
                    percentileCount++;
                    val = name.substring(i + 1, endIndex);
                    params.add(val);
                    i = endIndex;
                } else {
                    break;
                }
            }
        }
        if (percentileCount % 2 != 0) return null;
        return params;
    }
}
