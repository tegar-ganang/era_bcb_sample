package org.xaware.ide.xadev.wizardpanels.soap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.GlobalConstants;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.conversion.WSDLSOAPHelper;
import org.xaware.ide.xadev.conversion.WSDLUtil;
import org.xaware.ide.xadev.datamodel.soap.SOAPBizDriverInfo;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.wizard.Wizard;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.ide.xadev.wizardpanels.SOAPInfo;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * 
 * BizDriver Panel for SOAP BizComponent.
 * 
 * @author Vasu Thadaka
 */
public class SOAPBizDriverPanel extends WizardPanelComponent implements SelectionListener, ModifyListener, FocusListener {

    /** Logger Instance. */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(SOAPInfo.class.getName());

    /** Instance of Text that holds WSDL file path. */
    private Text wsdlFilePathTxt;

    /** Instance of Button. */
    private Button browseBtn;

    /** Instance of Button. */
    private Button loadBtn;

    /** Namespace instance holding XAware Namespace. */
    private static final Namespace ns = GlobalConstants.XAWARE_NAMESPACE;

    /** Instance of Combo. */
    private Combo serviceTypeCmb;

    /** Instance of Combo. */
    private Combo portCmb;

    /** Instance of Text. */
    private Text serviceURLTxt;

    /** Instance of Map */
    private Map portMap;

    /** Instance of object used to manipulate WSDL */
    private WSDLSOAPHelper wsdlHelper;

    /** FileChooserWithLabeling instance */
    private FileChooserWithLabeling fileChooser;

    /** Save BizDriver Button instance */
    private Button saveBizDriverBtn;

    /** Use Existing BizDriver Radio Button */
    protected Button useExistingBtn;

    /** Create New BizDriver Radio Button */
    protected Button createNewBtn;

    /** Stores the path */
    protected String initPath;

    /** Description text field instance */
    private Text descriptionTxt;

    /** Default constructor for SOAO BizComponent */
    public SOAPBizDriverPanel(String name) {
        super(name);
    }

    @Override
    protected Object getData() throws WizardException {
        SOAPBizDriverInfo info = new SOAPBizDriverInfo();
        info.setUseExisting(useExistingBtn.getSelection());
        info.setBizDriverFileName(fileChooser.getFileString());
        info.setService((Service) serviceTypeCmb.getData(serviceTypeCmb.getItem(serviceTypeCmb.getSelectionIndex())));
        info.setPort((Port) portCmb.getData(portCmb.getItem(portCmb.getSelectionIndex())));
        info.setURL(serviceURLTxt.getText());
        info.setWsdlFileName(wsdlFilePathTxt.getText());
        info.setDesignerElement(buildDesignerElement());
        info.setWsdlSOAPHelper(wsdlHelper);
        info.setDescription(descriptionTxt.getText());
        info.setMyDoc(new Document(info.constructBizDriverRootElement()));
        return info;
    }

    @Override
    public String getMissingDataMessage() {
        return null;
    }

    @Override
    public void initFromDependencyData(Vector inputData) {
    }

    @Override
    public void initFromXML(Hashtable inputParams) {
        initPath = XA_Designer_Plugin.getActiveEditedFileDirectory();
        if (initPath == null) {
            initPath = XA_Designer_Plugin.getXAwareRootPath();
        }
        final Composite editorPanelComp = new Composite(getPageComposite(), SWT.NONE);
        editorPanelComp.setLayout(new GridLayout());
        final Composite wsdlComp = new Composite(editorPanelComp, SWT.NONE);
        GridData gridData = new GridData(GridData.CENTER, GridData.CENTER, true, true);
        wsdlComp.setLayoutData(gridData);
        final GridLayout wsdlGridLayout = new GridLayout();
        wsdlGridLayout.numColumns = 4;
        wsdlComp.setLayout(wsdlGridLayout);
        useExistingBtn = new Button(wsdlComp, SWT.RADIO);
        useExistingBtn.setText(t.getString("Use Existing BizDriver"));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        useExistingBtn.setLayoutData(gridData);
        useExistingBtn.addSelectionListener(this);
        useExistingBtn.addFocusListener(this);
        createNewBtn = new Button(wsdlComp, SWT.RADIO);
        createNewBtn.setText(t.getString("Create New BizDriver"));
        createNewBtn.setSelection(true);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gridData.horizontalSpan = 4;
        createNewBtn.setLayoutData(gridData);
        createNewBtn.addSelectionListener(this);
        fileChooser = new FileChooserWithLabeling(wsdlComp, SWT.NONE, "BizDriver File Name: ", XA_Designer_Plugin.getActiveEditedFileDirectory(), "Browse...", XAChooser.BIZ_DRIVER_TYPE);
        fileChooser.setMode(SWT.SAVE);
        fileChooser.getTextField().addModifyListener(this);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        fileChooser.setLayoutData(gridData);
        gridData = new GridData();
        gridData.heightHint = 15;
        gridData.widthHint = 270;
        fileChooser.getTextField().setLayoutData(gridData);
        saveBizDriverBtn = new Button(wsdlComp, SWT.NONE);
        saveBizDriverBtn.setText("Save BizDriver");
        saveBizDriverBtn.addSelectionListener(this);
        Label dummyLabel = new Label(wsdlComp, SWT.NONE);
        gridData = new GridData();
        gridData.heightHint = 30;
        gridData.horizontalSpan = 4;
        dummyLabel.setLayoutData(gridData);
        final Label wsdlLbl = new Label(wsdlComp, SWT.NONE);
        wsdlLbl.setText(t.getString("WSDL:"));
        wsdlFilePathTxt = new Text(wsdlComp, SWT.BORDER);
        final GridData wsdlGridData = new GridData(GridData.FILL_HORIZONTAL);
        wsdlGridData.widthHint = 300;
        wsdlFilePathTxt.setLayoutData(wsdlGridData);
        browseBtn = ControlFactory.createButton(wsdlComp, t.getString("Browse..."));
        browseBtn.addSelectionListener(this);
        loadBtn = ControlFactory.createButton(wsdlComp, t.getString("Load"));
        loadBtn.addSelectionListener(this);
        final Label serviceLbl = new Label(wsdlComp, SWT.NONE);
        serviceLbl.setText(t.getString("Service:"));
        serviceTypeCmb = new Combo(wsdlComp, SWT.NONE | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.widthHint = 320;
        serviceTypeCmb.setLayoutData(gd);
        serviceTypeCmb.addSelectionListener(this);
        final Label portLbl = new Label(wsdlComp, SWT.NONE);
        portLbl.setText(t.getString("Port:"));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        portCmb = new Combo(wsdlComp, SWT.NONE | SWT.READ_ONLY);
        portCmb.setLayoutData(gd);
        portCmb.addSelectionListener(this);
        final Label urlLbl = new Label(wsdlComp, SWT.NONE);
        urlLbl.setText(t.getString("Service URL:"));
        final GridData widthGridData = new GridData(GridData.FILL_HORIZONTAL);
        widthGridData.horizontalSpan = 3;
        widthGridData.widthHint = 150;
        serviceURLTxt = new Text(wsdlComp, SWT.BORDER | SWT.READ_ONLY);
        serviceURLTxt.setLayoutData(widthGridData);
        serviceURLTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
        serviceURLTxt.setEditable(false);
        Menu m = new Menu(wsdlComp);
        serviceURLTxt.setMenu(m);
        final Label descriptionLabel = new Label(wsdlComp, SWT.NONE);
        descriptionLabel.setText(t.getString("Description:"));
        descriptionTxt = new Text(wsdlComp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.widthHint = 320;
        gd.heightHint = 80;
        descriptionTxt.setLayoutData(gd);
        descriptionTxt.addSelectionListener(this);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean nextPressed(Shell shell) {
        return true;
    }

    @Override
    protected boolean isPanelValid() {
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
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = initPath;
            if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
                dirName = dirName + File.separator;
            }
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
        final File tmpFile = ResourceUtils.getFile(dirName);
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
        final String wsdlFileName = wsdlFilePathTxt.getText();
        if (wsdlFileName.trim().equals("")) {
            ControlFactory.showMessageDialog(t.getString("Select a WSDL file to create SOAP BizComponent."), t.getString("Information"));
            return false;
        } else {
            try {
                final WSDLFactory fact = WSDLFactory.newInstance();
                final WSDLReader reader = fact.newWSDLReader();
                reader.readWSDL(ResourceUtils.getAbsolutePath(wsdlFilePathTxt.getText().trim()));
            } catch (final Throwable ex) {
                logger.printStackTrace(ex);
                final String message = "Invalid WSDL file. Exception: " + ex.getMessage();
                logger.severe(message);
                ControlFactory.showInfoDialog(t.getString("Invalid WSDL file."), message);
                return false;
            }
        }
        return true;
    }

    @Override
    public void initFromBizCompRoot() {
        final Element root = Wizard.getBizCompRoot();
        try {
            if (root != null) {
                fileChooser.getTextField().setText(root.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_DRIVER, ns));
                useExistingBtn.setSelection(true);
                createNewBtn.setSelection(false);
                populateScreenFromBizDriver();
            }
        } catch (final Exception ex) {
            logger.printStackTrace(ex);
            final String message = "BizDriver File path could not be loaded completely. Exception:" + ex.getMessage();
            logger.severe(message);
            ControlFactory.showInfoDialog(t.getString("BizDriver File Path is incorrect."), message, true);
        }
    }

    /**
	 * Method from SelectionListener. Performs different operations for each of
	 * button, text and combo control in the page.
	 * 
	 * @param e
	 *            Instance of SelectionEvent.
	 */
    public void widgetSelected(final SelectionEvent e) {
        if (e.getSource() == browseBtn) {
            final XAChooser fileChooser = new XAChooser(getShell(), System.getProperty("xaware.home"), t.getString("Open"), SWT.OPEN);
            fileChooser.addDefaultFilter(XAChooser.WSDL);
            fileChooser.addFilter(XAChooser.WSDL);
            final String fileName = fileChooser.openResource();
            wsdlFilePathTxt.setText(fileName);
            try {
                loadWSDL();
                populateFromWSDL();
            } catch (final Throwable ex) {
                clearFields();
            }
            setDataChanged();
        } else if (e.getSource() == loadBtn) {
            try {
                loadWSDL();
                populateFromWSDL();
            } catch (final Throwable ex) {
                clearFields();
            }
            setDataChanged();
        } else if (e.getSource() == wsdlFilePathTxt) {
            setDataChanged();
        } else if (e.getSource() == serviceTypeCmb) {
            final Service selectedService = (Service) serviceTypeCmb.getData(serviceTypeCmb.getText());
            portMap = WSDLUtil.getPorts(selectedService);
            populatePortCombo(portCmb, portMap.values().toArray());
            poulateStaticTextElements();
        } else if (e.getSource() == portCmb) {
            poulateStaticTextElements();
        } else if (e.getSource() == createNewBtn) {
            fileChooser.setMode(SWT.SAVE);
            doUseExistingNewRadioButtonDisablingWork();
        } else if (e.getSource() == useExistingBtn) {
            fileChooser.setMode(SWT.OPEN);
            doUseExistingNewRadioButtonDisablingWork();
        } else if (e.getSource() == saveBizDriverBtn) {
            if (isPanelValid()) {
                SOAPBizDriverInfo info;
                try {
                    info = (SOAPBizDriverInfo) getData();
                    info.saveBizDriverFile(true);
                } catch (Exception exception) {
                    logger.info("Exception writing SOAP BizDriver file: ", exception);
                    ControlFactory.showErrorDialog(t.getString("Error saving BizDriver file: ") + exception.getMessage(), t.getString("Error"));
                }
            }
        }
    }

    /**
	 * Builds the Designer Element
	 * 
	 * @return Built Element
	 */
    public Element buildDesignerElement() {
        final Element designerElement = new Element("wizard_step", ns);
        designerElement.setAttribute("name", "SOAP BizDriver", ns);
        return designerElement;
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    /**
	 * Populates Port Combo with values.
	 * 
	 * @param sourceCmb
	 *            Instance of Combo.
	 * @param values
	 *            Object array representing values within combo.
	 */
    private void populatePortCombo(final Combo sourceCmb, final Object[] values) {
        sourceCmb.removeAll();
        for (int index = 0; index < values.length; index++) {
            final String displayName = ((Port) values[index]).getName();
            sourceCmb.add(displayName);
            sourceCmb.setData(displayName, values[index]);
        }
        if (values.length >= 0) {
            sourceCmb.select(0);
        }
    }

    /**
	 * Populates Text controls with data.
	 */
    private void poulateStaticTextElements() {
        serviceURLTxt.setText(SOAPBizDriverInfo.getServiceAddress((Port) portMap.get(portCmb.getItem(portCmb.getSelectionIndex()))));
        setDataChanged();
    }

    /**
	 * Load the WSDL
	 */
    public void loadWSDL() {
        clearFields();
        try {
            wsdlHelper = new WSDLSOAPHelper(ResourceUtils.getAbsolutePath(wsdlFilePathTxt.getText()));
        } catch (final Throwable ex) {
            final String message = "Invalid WSDL file. Exception: " + ex.getMessage();
            logger.severe(message);
            ControlFactory.showInfoDialog(Translator.getInstance().getString("Invalid WSDL file."), message);
            clearFields();
        }
        setDataChanged();
    }

    /**
	 * Clears fields with in the page.
	 */
    public void clearFields() {
        serviceTypeCmb.removeSelectionListener(this);
        portCmb.removeSelectionListener(this);
        serviceTypeCmb.removeAll();
        portCmb.removeAll();
        serviceTypeCmb.addSelectionListener(this);
        portCmb.addSelectionListener(this);
        serviceURLTxt.setText("");
        setDataChanged();
    }

    /**
	 * Reads WSDL file and populates the data into controls within the page.
	 * 
	 * @throws Exception
	 */
    private void populateFromWSDL() throws Exception {
        try {
            final Definition theDef = wsdlHelper.getDef();
            final List serviceList = WSDLUtil.getServices(theDef);
            if (serviceList.size() > 0) {
                populateServiceCombo(serviceTypeCmb, serviceList.toArray());
                final Service curService = (Service) serviceTypeCmb.getData(serviceTypeCmb.getItem(serviceTypeCmb.getSelectionIndex()));
                portMap = curService.getPorts();
            } else {
                final String message = "No Services are available!";
                logger.finer(message);
                ControlFactory.showInfoDialog(Translator.getInstance().getString(message), message);
                clearFields();
                throw new XAwareException(t.getString(message));
            }
            populatePortCombo(portCmb, portMap.values().toArray());
            poulateStaticTextElements();
        } catch (final Exception e) {
            throw e;
        }
        setDataChanged();
    }

    /**
	 * Populates Service Combo with values.
	 * 
	 * @param sourceCmb
	 *            Instance of Combo.
	 * @param values
	 *            Object array representing values within combo.
	 */
    private void populateServiceCombo(final Combo sourceCmb, final Object[] values) {
        sourceCmb.removeAll();
        for (int index = 0; index < values.length; index++) {
            final String displayName = ((Service) values[index]).getQName().getLocalPart();
            sourceCmb.add(displayName);
            sourceCmb.setData(displayName, values[index]);
        }
        if (values.length >= 0) {
            sourceCmb.select(0);
        }
    }

    /**
	 * This method is called when the driver file name is entered or populated
	 * from browse button in the fileChooserTxt TextField
	 * 
	 * @param e
	 *            ModifyEvent of file name TextField
	 */
    public void modifyText(final ModifyEvent e) {
        if (e.getSource() == fileChooser.getTextField() && useExistingBtn.getSelection()) {
            populateScreenFromBizDriver();
        }
        setDataChanged();
    }

    /**
	 * This method is called when the use existing radio option gains focus.
	 * 
	 * @param e
	 *            FocusEvent of use existing radio button.
	 */
    public void focusGained(final FocusEvent e) {
        doUseExistingNewRadioButtonDisablingWork();
    }

    /** Disables/Enables depening up on use existing or create new radio button */
    private void doUseExistingNewRadioButtonDisablingWork() {
        if (useExistingBtn.getSelection()) {
            wsdlFilePathTxt.setEnabled(false);
            serviceTypeCmb.setEnabled(false);
            portCmb.setEnabled(false);
            serviceURLTxt.setEnabled(false);
            descriptionTxt.setEnabled(false);
            saveBizDriverBtn.setEnabled(false);
            browseBtn.setEnabled(false);
            loadBtn.setEnabled(false);
            createNewBtn.setSelection(false);
        } else {
            wsdlFilePathTxt.setEnabled(true);
            serviceTypeCmb.setEnabled(true);
            portCmb.setEnabled(true);
            serviceURLTxt.setEnabled(true);
            descriptionTxt.setEnabled(true);
            saveBizDriverBtn.setEnabled(true);
            browseBtn.setEnabled(true);
            loadBtn.setEnabled(true);
            useExistingBtn.setSelection(false);
        }
    }

    public void focusLost(FocusEvent e) {
    }

    /**
	 * Populates the values from bizDriver.
	 */
    public void populateScreenFromBizDriver() {
        SAXBuilder sBuilder = null;
        Document bizDriverDoc = null;
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
                    bizDriverDoc = sBuilder.build(input);
                } catch (final JDOMException jde) {
                    logger.fine("Error parsing biz driver file.");
                } catch (final IOException jde) {
                    logger.fine("Error parsing biz driver file.");
                }
                Element root = bizDriverDoc.getRootElement();
                Element httpElement = root.getChild("http", ns);
                Element urlElement = httpElement.getChild("url", ns);
                final Element connectionElement = getConnectionElemFromDesignerElem(root);
                try {
                    descriptionTxt.setText(root.getChild(XAwareConstants.XAWARE_ELEMENT_DESCRIPTION, ns).getValue());
                    wsdlFilePathTxt.setText(connectionElement.getAttributeValue(XAwareConstants.BIZDRIVER_WSDL_URL, ns));
                    fireEvent(SWT.Selection, loadBtn);
                    serviceTypeCmb.setText(connectionElement.getAttributeValue(XAwareConstants.BIZDRIVER_ATTR_SERVICE, ns));
                    portCmb.setText(connectionElement.getAttributeValue(XAwareConstants.BIZDRIVER_ATTR_PORT, ns));
                    serviceURLTxt.setText(urlElement.getText());
                } catch (final Throwable ex) {
                    logger.fine("Error parsing elements from biz driver file.");
                }
            }
        }
    }

    /**
	 * Returns the connection child element from the wizard_step element with 
	 * 			name attribute value as 'Soap Connection Info'
	 * @param root root of the bizdriver.
	 */
    @SuppressWarnings("unchecked")
    private Element getConnectionElemFromDesignerElem(Element root) {
        Element designerElement = root.getChild("designer", ns);
        List wizardSteps = designerElement.getChildren("wizard_step", ns);
        Iterator<Element> wizardStepsIterator = wizardSteps.iterator();
        while (wizardStepsIterator.hasNext()) {
            Element wizardStepElement = wizardStepsIterator.next();
            if (wizardStepElement.getAttributeValue("name", ns).equals(SOAPBizDriverInfo.SOAP_CONNECTION_INFO_WIZARD_STEP)) return wizardStepElement.getChild("connection", ns);
        }
        return null;
    }
}
