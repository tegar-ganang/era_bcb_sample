package org.xaware.ide.xadev.wizardpanels;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
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
import org.xaware.ide.xadev.datamodel.SocketBizDriverInfo;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Biz Driver Page of Socket BizComponent Wizard
 * 
 * @author Balaji C S
 * @version 1.0
 */
public class SocketBizDriver extends WizardPanelComponent implements ModifyListener, SelectionListener {

    /** Name space */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** Translator */
    private final Translator translator = XA_Designer_Plugin.getTranslator();

    /** Logger */
    private final XAwareLogger logger = XAwareLogger.getXAwareLogger(SocketBizDriver.class.getName());

    /** shell */
    private Shell shell;

    /** Use Existing Radio Button */
    private Button existsRdo;

    /** Create new Radio Button */
    private Button newRdo;

    /** File Name Label */
    private Label fileNameLbl;

    /** File Name Text */
    private Text fileNameTxt;

    /** Browse Button */
    private Button browseBtn;

    /** Host Label */
    private Label lblHost;

    /** Host combo */
    private Combo hostCmb;

    /** port Label */
    private Label portLbl;

    /** port Text */
    private Text portTxt;

    /** Parent Composite */
    private Composite parent;

    /**
     * Creates a new SocketBizDriver object.
     * 
     * @param pageName
     *            reference to String
     */
    public SocketBizDriver(final String pageName) {
        super(pageName);
    }

    /**
     * initializes the wizard page from the xml file
     * 
     * @param inputParams
     *            Input Parameters
     */
    @Override
    public void initFromXML(final Hashtable inputParams) {
        parent = getPageComposite();
        final Composite editorComp = new Composite(parent, SWT.NONE);
        shell = parent.getShell();
        editorComp.setLayout(new GridLayout());
        final Composite socketGrp = new Composite(editorComp, SWT.NONE);
        socketGrp.setLayout(new GridLayout());
        socketGrp.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_CENTER);
        final Composite childComp = new Composite(socketGrp, SWT.NONE);
        GridLayout gridLayout = new GridLayout(3, false);
        childComp.setLayout(gridLayout);
        childComp.setLayoutData(gridData);
        existsRdo = new Button(childComp, SWT.RADIO);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        existsRdo.setText(translator.getString("Use Existing BizDriver"));
        existsRdo.setLayoutData(gridData);
        newRdo = new Button(childComp, SWT.RADIO);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        newRdo.setText(translator.getString("Create New BizDriver"));
        newRdo.setLayoutData(gridData);
        newRdo.setSelection(true);
        fileNameLbl = new Label(childComp, SWT.NONE);
        fileNameLbl.setText(translator.getString("File: "));
        fileNameTxt = new Text(childComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 240;
        fileNameTxt.setLayoutData(gridData);
        fileNameTxt.setText(XA_Designer_Plugin.getActiveEditedInternalFrameFileDirectory());
        fileNameTxt.setSelection(fileNameTxt.getCharCount());
        fileNameTxt.addModifyListener(this);
        browseBtn = ControlFactory.createButton(childComp, translator.getString("Browse"));
        browseBtn.addSelectionListener(this);
        gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 10;
        gridLayout.horizontalSpacing = 10;
        gridData = new GridData();
        lblHost = new Label(childComp, SWT.NONE);
        lblHost.setText(translator.getString("Host: "));
        gridData = new GridData();
        gridData.widthHint = 40;
        lblHost.setLayoutData(gridData);
        hostCmb = new Combo(childComp, SWT.SINGLE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        hostCmb.setLayoutData(gridData);
        final Vector socketTypes = UserPrefs.getSocketTypes(UserPrefs.SOCKET_HOST);
        for (int i = 0; i < socketTypes.size(); i++) {
            hostCmb.add((String) socketTypes.get(i));
        }
        hostCmb.setText(hostCmb.getItem(0));
        portLbl = new Label(childComp, SWT.NONE);
        portLbl.setText(translator.getString("Port: "));
        gridData = new GridData();
        gridData.widthHint = 40;
        portLbl.setLayoutData(gridData);
        portTxt = new Text(childComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 240;
        portTxt.setLayoutData(gridData);
        existsRdo.addSelectionListener(this);
        newRdo.addSelectionListener(this);
        setControl(editorComp);
    }

    /**
     * initializes the wizard page from the dependancy data from other screens
     * 
     * @param inputData
     *            Input Data
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
        final SocketBizDriverInfo retVal = new SocketBizDriverInfo();
        final Element root = new Element("socket_driver");
        root.setAttribute(new Attribute("bizdrivertype", "Socket", ns));
        final Element child = new Element("initialcontext", ns);
        final Element host = new Element("host", ns);
        if (hostCmb.getSelectionIndex() < 0) {
            host.setText(hostCmb.getText());
        } else {
            host.setText(hostCmb.getItem(hostCmb.getSelectionIndex()));
        }
        final Element port = new Element("port", ns);
        port.setText(portTxt.getText());
        child.addContent(host);
        child.addContent(port);
        root.addContent(child);
        retVal.setDocument(new Document(root));
        retVal.setFileName(fileNameTxt.getText());
        if (existsRdo.getSelection()) {
            retVal.setUseExisting(true);
        } else {
            retVal.setUseExisting(false);
        }
        return retVal;
    }

    /**
     * isPanelValid() method checks whether the panel is valid. Validates the Biz driver used.
     * 
     * @return Boolean true if valid and can proceed to next screen, false otherwise
     */
    @Override
    protected boolean isPanelValid() {
        int style = 0;
        style = SWT.OK | SWT.ICON_INFORMATION;
        String fname = fileNameTxt.getText().trim();
        fileNameTxt.setText(fname);
        fileNameTxt.setSelection(fileNameTxt.getCharCount());
        if (fname.equals("")) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"), style);
            return false;
        }
        File bizDriverFile = ResourceUtils.getFile(fname);
        if (bizDriverFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"), style);
            return false;
        }
        if (bizDriverFile.getName().trim().equals(".xdr")) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"), style);
            return false;
        }
        if ((bizDriverFile.getAbsolutePath().indexOf('#') > -1) || (bizDriverFile.getAbsolutePath().indexOf('%') > -1)) {
            ControlFactory.showMessageDialog(translator.getString("Please do not enter # or % in the file path or name."), translator.getString("Information"), style);
            return false;
        }
        String dirName = bizDriverFile.getParent();
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = XA_Designer_Plugin.getPluginRootPath();
            if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
                dirName = dirName + File.separator;
            }
            fileNameTxt.setText(dirName + fileNameTxt.getText());
            fileNameTxt.setSelection(fileNameTxt.getCharCount());
        } else if (dirName.trim().equals(File.separator)) {
            dirName = XA_Designer_Plugin.getPluginRootPath();
            fileNameTxt.setText(dirName.substring(0, dirName.length() - 1) + fileNameTxt.getText());
            fileNameTxt.setSelection(fileNameTxt.getCharCount());
        } else {
            dirName = dirName.trim();
            if (dirName.charAt(dirName.length() - 1) == File.separatorChar) {
                fileNameTxt.setText(dirName + bizDriverFile.getName().trim());
                fileNameTxt.setSelection(fileNameTxt.getCharCount());
            } else {
                fileNameTxt.setText(fname);
                fileNameTxt.setSelection(fileNameTxt.getCharCount());
            }
        }
        final File tmpFile = new File(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Invalid directory for BizDriver file."), translator.getString("Information"), style);
            return false;
        }
        fname = fileNameTxt.getText().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (newRdo.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
            if (fileNameStr.indexOf('.') == -1) {
                fname = fname + ".xdr";
                fileNameTxt.setText(fname);
                fileNameTxt.setSelection(fileNameTxt.getCharCount());
                bizDriverFile = new File(fname);
            } else if (fileNameStr.indexOf('.') == (fileNameStr.length() - 1)) {
                fname = fname + "xdr";
                fileNameTxt.setText(fname);
                fileNameTxt.setSelection(fileNameTxt.getCharCount());
                bizDriverFile = new File(fname);
            }
            if (bizDriverFile.exists()) {
                final int choice = ControlFactory.showConfirmDialog(translator.getString("BizDriver file which you have entered already exists. Overwrite?"), false);
                if (choice != Window.OK) {
                    return false;
                }
            }
        } else if (existsRdo.getSelection()) {
            if (bizDriverFile.isDirectory() || !bizDriverFile.exists()) {
                ControlFactory.showMessageDialog(translator.getString("BizDriver file which you have entered does not exist. Please enter correct file name."), translator.getString("Information"), style);
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if page complete
     * 
     * @return Boolean true if page operations are complete, false otherwise
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
        Element bizDriverElem = null;
        Element bizDriverConnElem = null;
        final String fname = fileNameTxt.getText().trim();
        final File bizDriverFile = ResourceUtils.getFile(fname);
        String host = "";
        String port = "";
        if (!fname.equals("") && !bizDriverFile.isDirectory()) {
            sBuilder = new SAXBuilder();
            InputStream input = null;
            try {
                input = new FileInputStream(ResourceUtils.getFile(fname).toString());
            } catch (final Exception e) {
            }
            if (input != null) {
                try {
                    bizDriverDoc = sBuilder.build(input);
                } catch (final JDOMException jde) {
                    logger.finest("Error parsing biz driver file.");
                } catch (final IOException jde) {
                    logger.finest("Error parsing biz driver file.");
                }
                try {
                    root = bizDriverDoc.getRootElement();
                    bizDriverConnElem = root.getChild("initialcontext", ns);
                    bizDriverElem = bizDriverConnElem.getChild("host", ns);
                    host = bizDriverElem.getText();
                    bizDriverElem = bizDriverConnElem.getChild("port", ns);
                    port = bizDriverElem.getText();
                } catch (final Throwable ex) {
                    logger.finest("Error parsing elements from biz driver file.");
                }
            }
            portTxt.setText(port);
            final int index = hostCmb.indexOf(host);
            hostCmb.select(index);
            if (index < 0) {
                hostCmb.setText("");
            }
            fileNameTxt.setFocus();
            if (fileNameTxt.getText() != null) {
                fileNameTxt.setSelection(fileNameTxt.getText().length(), fileNameTxt.getText().length());
            } else {
                fileNameTxt.setSelection(0, 0);
            }
        }
    }

    /**
     * returns true if next pressed.
     * 
     * @param shell
     *            Shell
     * 
     * @return Boolean returns true if next pressed.
     */
    @Override
    public boolean nextPressed(final Shell shell) {
        return false;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
     */
    public void modifyText(final ModifyEvent e) {
        if (existsRdo.getSelection()) {
            portTxt.removeModifyListener(this);
            populateScreen();
            portTxt.addModifyListener(this);
        }
        setDataChanged();
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetSelected(final SelectionEvent e) {
        try {
            if (e.getSource() == browseBtn) {
                XAChooser fileChooser = null;
                int mode = SWT.OPEN;
                if (!existsRdo.getSelection()) {
                    mode = SWT.SAVE;
                }
                if ((fileNameTxt.getText() != null) && !fileNameTxt.getText().equals("")) {
                    fileChooser = new XAChooser(parent.getShell(), fileNameTxt.getText(), mode);
                } else {
                    fileChooser = new XAChooser(parent.getShell(), XA_Designer_Plugin.getActiveEditedInternalFrame().getEditorFileDirectory(), mode);
                }
                fileChooser.addFilter(XAFileConstants.BIZ_DRIVER_TYPE);
                fileChooser.addDefaultFilter(XAFileConstants.BIZ_DRIVER_TYPE);
                final String fileName = fileChooser.openResource();
                fileNameTxt.setText(fileName);
                fileNameTxt.setSelection(fileNameTxt.getCharCount());
            } else if ((e.getSource() == existsRdo) && existsRdo.getSelection()) {
                setDataChanged();
                hostCmb.setEnabled(false);
                portTxt.setEnabled(false);
                populateScreen();
            } else if ((e.getSource() == newRdo) && newRdo.getSelection()) {
                setDataChanged();
                hostCmb.setEnabled(true);
                portTxt.setEnabled(true);
                if (hostCmb.getText().equals("") && (hostCmb.getItemCount() > 0)) {
                    hostCmb.setText(hostCmb.getItem(0));
                }
                fileNameTxt.setFocus();
                if (fileNameTxt.getText() != null) {
                    fileNameTxt.setSelection(fileNameTxt.getText().length(), fileNameTxt.getText().length());
                } else {
                    fileNameTxt.setSelection(0, 0);
                }
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }
}
