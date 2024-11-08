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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import org.xaware.ide.xadev.wizard.WizardException;
import org.xaware.ide.xadev.wizard.WizardPanelComponent;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * To Create the CorbaBizDriverFileName.
 * 
 * @author Govind, Bharath
 * @version 1.0
 */
public class CorbaBizDriverFileName extends WizardPanelComponent implements VerifyListener, ModifyListener, SelectionListener, FocusListener {

    /** XAwareLogger instance */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(CorbaBizDriverFileName.class.getName());

    /** ns holds the namespace of XAwareConstants */
    private static final Namespace ns = XAwareConstants.xaNamespace;

    /** Translator used for Localisation */
    private static final Translator translator = XA_Designer_Plugin.getTranslator();

    /** Root Composite for all the widgets */
    private Composite rootComp;

    /** useExisting represents "Use Existing Biz Driver" radio button. */
    private Button useExistingBtn;

    /** createNew represents "Create New Biz Driver" radio button. */
    private Button createNewBtn;

    /** This Composite is added to the rootComp */
    private Composite childComp;

    /** titleLbl is the heading of the wizard page. */
    private Label titleLbl;

    /**
     * Used to create composite on which the biz driver file Name Lbl, biz driver file Name Txt, browseBtn are added.
     */
    private FileChooserWithLabeling fileChooser;

    /**
     * Composite on which the remaining widgets are placed except the singletonBtn
     */
    private Composite nameComp;

    /** nameLab is a label for nameTxt. */
    private Label nameLbl;

    /** nameTxt is a Textbox for entering the Name Service. */
    private Text nameTxt;

    /** nameIdLab is a label for nameIDTxt. */
    private Label nameIdLbl;

    /** nameIDTxt is a Textbox for entering the Name ID. */
    private Text nameIdTxt;

    /** hostLab is a label for hostCmb. */
    private Label hostLbl;

    /**
     * hostCmb is a Combo which contains list of available Hosts It is populated from the
     * UserPrefs.getContextHostList().
     */
    private Combo hostCmb;

    /** portLab is a label for portTxt. */
    private Label portLbl;

    /** portTxt is a Textbox for entering the Port. */
    private Text portTxt;

    /**
     * singletonBtn is a checkbox button for whether the biz driver should have a single instance.
     */
    private Button singletonChk;

    /**
     * Creates a new CorbaBizDriverFileName object.
     * 
     * @param pageName
     */
    public CorbaBizDriverFileName(final String pageName) {
        super(pageName);
        setTitle(translator.getString("Initial Context"));
        setDescription(translator.getString("Contains information about Initial Context."));
    }

    /**
     * Initializes from XML Document
     * 
     * @param inputParams
     *            Input Parameters
     */
    @Override
    public void initFromXML(final Hashtable inputParams) {
        removeAll();
        final Composite parentComp = new Composite(getPageComposite(), SWT.NULL);
        parentComp.setLayout(new GridLayout());
        rootComp = new Composite(parentComp, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.verticalSpacing = 10;
        gridLayout.horizontalSpacing = 10;
        gridLayout.marginLeft = 50;
        gridLayout.marginRight = 80;
        gridLayout.marginTop = 80;
        gridLayout.marginBottom = 100;
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER);
        rootComp.setLayout(gridLayout);
        rootComp.setLayoutData(gridData);
        childComp = new Composite(rootComp, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER);
        childComp.setLayout(gridLayout);
        childComp.setLayoutData(gridData);
        titleLbl = new Label(childComp, SWT.NONE);
        titleLbl.setText(translator.getString("CORBA BizDriver"));
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gridData.heightHint = 20;
        titleLbl.setLayoutData(gridData);
        useExistingBtn = new Button(childComp, SWT.RADIO);
        gridData = new GridData();
        gridData.heightHint = 20;
        useExistingBtn.setText(translator.getString("Use Existing BizDriver"));
        useExistingBtn.setLayoutData(gridData);
        createNewBtn = new Button(childComp, SWT.RADIO);
        gridData = new GridData();
        gridData.heightHint = 20;
        createNewBtn.setText(translator.getString("Create New BizDriver"));
        createNewBtn.setLayoutData(gridData);
        createNewBtn.setSelection(true);
        fileChooser = new FileChooserWithLabeling(childComp, SWT.NONE, translator.getString("BizDriver File Name:"), XA_Designer_Plugin.getActiveEditedFileDirectory(), translator.getString("Browse..."), XAFileConstants.BIZ_DRIVER_TYPE);
        int mode = SWT.OPEN;
        if (!useExistingBtn.getSelection()) {
            mode = SWT.SAVE;
        }
        fileChooser.setMode(mode);
        fileChooser.focusListenerForTextField(this);
        fileChooser.modifyListenerForTextField(this);
        gridData = new GridData();
        gridData.horizontalIndent = 4;
        gridData.heightHint = 25;
        fileChooser.setLayoutData(gridData);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint = 220;
        gridData.horizontalIndent = 5;
        fileChooser.getTextField().setLayoutData(gridData);
        nameComp = new Composite(childComp, SWT.NONE);
        gridLayout = new GridLayout(2, false);
        gridLayout.verticalSpacing = 10;
        gridLayout.horizontalSpacing = 10;
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        nameComp.setLayout(gridLayout);
        nameComp.setLayoutData(gridData);
        nameLbl = new Label(nameComp, SWT.NONE);
        nameLbl.setText(translator.getString("Name: "));
        gridData = new GridData();
        gridData.widthHint = 80;
        nameLbl.setLayoutData(gridData);
        nameTxt = ControlFactory.createText(nameComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 220;
        gridData.horizontalIndent = 12;
        nameTxt.setLayoutData(gridData);
        nameTxt.addVerifyListener(this);
        nameTxt.setBackground(parentComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        nameIdLbl = new Label(nameComp, SWT.NONE);
        nameIdLbl.setText(translator.getString("Name ID: "));
        gridData = new GridData();
        nameIdLbl.setLayoutData(gridData);
        nameIdTxt = ControlFactory.createText(nameComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 220;
        gridData.horizontalIndent = 12;
        nameIdTxt.setLayoutData(gridData);
        nameIdTxt.addVerifyListener(this);
        nameIdTxt.setBackground(parentComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        hostLbl = new Label(nameComp, SWT.NONE);
        hostLbl.setText(translator.getString("Host(URL): "));
        gridData = new GridData();
        hostLbl.setLayoutData(gridData);
        hostCmb = new Combo(nameComp, SWT.SINGLE);
        gridData = new GridData();
        gridData.widthHint = 100;
        gridData.horizontalIndent = 12;
        hostCmb.setLayoutData(gridData);
        hostCmb.setBackground(parentComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        final List hostList = UserPrefs.getCorbaHostList();
        for (int i = 0; i < hostList.size(); i++) {
            hostCmb.add((String) hostList.get(i), i);
        }
        if (hostCmb.getItemCount() > 0) {
            hostCmb.setText(hostCmb.getItem(0));
        }
        portLbl = new Label(nameComp, SWT.NONE);
        portLbl.setText(translator.getString("Port: "));
        gridData = new GridData();
        portLbl.setLayoutData(gridData);
        portTxt = ControlFactory.createText(nameComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 220;
        gridData.horizontalIndent = 12;
        portTxt.setLayoutData(gridData);
        portTxt.setBackground(parentComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        singletonChk = new Button(childComp, SWT.CHECK);
        gridData = new GridData();
        gridData.horizontalIndent = 5;
        singletonChk.setText(translator.getString("Singleton"));
        singletonChk.setLayoutData(gridData);
        useExistingBtn.addSelectionListener(this);
        createNewBtn.addSelectionListener(this);
        refresh();
    }

    /**
     * Initializes from Dependency Data
     * 
     * @param inputData
     *            input data
     */
    @Override
    public void initFromDependencyData(final Vector inputData) {
    }

    /**
     * Returns Missing data message
     * 
     * @return Returns Missing data message
     */
    @Override
    public String getMissingDataMessage() {
        return "";
    }

    /**
     * Returns the dependency data of the this page
     * 
     * @return Returns the dependency data of the this page
     * 
     * @throws WizardException
     *             wrap any Exception when getting the page data
     */
    @Override
    protected Object getData() throws WizardException {
        if (createNewBtn.getSelection()) {
            try {
                final FileOutputStream fileOut = new FileOutputStream(ResourceUtils.getFile(fileChooser.getFileString()).toString());
                final Element root = new Element("corba");
                root.setAttribute(new Attribute("bizdrivertype", "CORBA", ns));
                if (singletonChk.getSelection()) {
                    root.setAttribute(new Attribute("singleton", "yes", ns));
                } else {
                    root.setAttribute(new Attribute("singleton", "no", ns));
                }
                final Element child = new Element("connection", ns);
                Element toAdd = new Element("namingservice", ns);
                toAdd.setAttribute("name", nameTxt.getText(), ns);
                toAdd.setAttribute("name_id", nameIdTxt.getText(), ns);
                child.addContent(toAdd);
                toAdd = new Element("host", ns);
                toAdd.setText(hostCmb.getText());
                child.addContent(toAdd);
                toAdd = new Element("port", ns);
                toAdd.setText(portTxt.getText());
                child.addContent(toAdd);
                root.addContent(child);
                UserPrefs.getFileOutputter().output(new Document(root), fileOut);
                fileOut.close();
            } catch (final Exception e) {
                logger.finest("Exception saving BizDriver: " + e.toString());
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
        String name = "";
        String nameid = "";
        String host = "";
        String port = "";
        String singletonStr = "";
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
                } catch (final IOException ioe) {
                    logger.finest("Error parsing biz driver file.");
                }
                try {
                    root = bizDriverDoc.getRootElement();
                    bizDriverConnElem = root.getChild("connection", ns);
                    bizDriverElem = bizDriverConnElem.getChild("namingservice", ns);
                    attr = bizDriverElem.getAttribute("name", ns);
                    if (attr != null) {
                        name = attr.getValue();
                    }
                    attr = bizDriverElem.getAttribute("name_id", ns);
                    if (attr != null) {
                        nameid = attr.getValue();
                    }
                    attr = root.getAttribute("singleton", ns);
                    if (attr != null) {
                        singletonStr = attr.getValue();
                    }
                    bizDriverElem = bizDriverConnElem.getChild("port", ns);
                    port = bizDriverElem.getText();
                    bizDriverElem = bizDriverConnElem.getChild("host", ns);
                    host = bizDriverElem.getText();
                } catch (final Throwable ex) {
                    logger.finest("Error parsing elements from biz driver file.");
                }
            }
            nameTxt.setText(name);
            nameIdTxt.setText(nameid);
            hostCmb.setText(host);
            portTxt.setText(port);
            if (singletonStr.equals("yes")) {
                singletonChk.setSelection(true);
            } else {
                singletonChk.setSelection(false);
            }
        }
    }

    /**
     * The method widgetDefaultSelected() must be implemented as it is abstract in SelectionListener.
     * 
     * @param e
     *            holds instance of SelectionEvent.
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
    }

    /**
     * The method widgetSelected() handles events for useExistingBtn, createNewBtn, and browseBtn.
     * 
     * @param e
     *            holds instance of SelectionEvent.
     */
    public void widgetSelected(final SelectionEvent e) {
        if ((e.getSource() == useExistingBtn) && useExistingBtn.getSelection()) {
            setDataChanged();
            nameTxt.setEnabled(false);
            nameIdTxt.setEnabled(false);
            hostCmb.setEnabled(false);
            portTxt.setEnabled(false);
            singletonChk.setEnabled(false);
            int mode = SWT.OPEN;
            if (!useExistingBtn.getSelection()) {
                mode = SWT.SAVE;
            }
            fileChooser.setMode(mode);
            nameTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            nameIdTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            hostCmb.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            portTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            populateScreen();
        } else if ((e.getSource() == createNewBtn) && createNewBtn.getSelection()) {
            setDataChanged();
            nameTxt.setEnabled(true);
            nameIdTxt.setEnabled(true);
            hostCmb.setEnabled(true);
            portTxt.setEnabled(true);
            singletonChk.setEnabled(true);
            nameTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            nameIdTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            hostCmb.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            portTxt.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            if (hostCmb.getText().equals("")) {
                hostCmb.select(0);
            }
            int mode = SWT.OPEN;
            if (!useExistingBtn.getSelection()) {
                mode = SWT.SAVE;
            }
            fileChooser.setMode(mode);
        }
    }

    /**
     * nameTxt and nameIdTxt Verify listeners.
     * 
     * @param e
     *            verify event
     */
    public void verifyText(final VerifyEvent e) {
        if (e.getSource().equals(nameTxt)) {
            setDataChanged();
        }
        if (e.getSource().equals(nameIdTxt)) {
            setDataChanged();
        }
    }

    /**
     * Biz driver file Name's ModifyListener
     * 
     * @param e
     *            modify event
     */
    public void modifyText(final ModifyEvent e) {
        setDataChanged();
        if (useExistingBtn.getSelection()) {
            populateScreen();
        }
    }

    /**
     * The method focusGained() must be implemented as it is abstract in FocusListener.
     * 
     * @param e
     *            holds instance of FocusEvent.
     */
    public void focusGained(final FocusEvent e) {
    }

    /**
     * The method focusLost() handles events for class Text field. If class text field is blank or contains invalid
     * class then method list contents are removed.
     * 
     * @param e
     *            holds instance of FocusEvent.
     */
    public void focusLost(final FocusEvent e) {
    }

    /**
     * performs any processing that is required when return button is pressed
     * 
     * @param shell
     *            shell instance
     * 
     * @return returns true if processing is successful
     */
    @Override
    public boolean nextPressed(final Shell shell) {
        return true;
    }
}
