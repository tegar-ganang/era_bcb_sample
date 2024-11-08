package org.xaware.ide.xadev.gui.dialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.wsdl.WSDLException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.shared.util.WSDLFileUtils;
import org.xaware.shared.util.WSDLFileUtils_DocLit;
import org.xaware.shared.util.WSDLFileUtils_RpcLit;
import org.xaware.shared.util.WSDLFileUtils_Unwrapped_DocLit;
import org.xaware.shared.util.ZipFileUtils;

/**
 * WSDLCreateFromArchiveInfoWin - It is a GUI class, holds GUI and Functionality for Make WSDL From Archive module.
 * 
 * @author G Bharath Kumar
 * @version 1.0
 */
public class WSDLCreateFromArchiveInfoWin extends WSDLCreateBaseWin implements SelectionListener, ModifyListener {

    /** Make WSDL button ID */
    private static final int MAKE_WSDL = 1;

    /** Add WSDL To Archive button ID */
    private static final int ADD_WSDL_TO_ARCHIVE = 2;

    /** Close button ID */
    private static final int CLOSE = 3;

    /** Text Field, used to hold archive file name. */
    private Text archiveFileTxt;

    /** List Field, used to hold list of biz documents. */
    protected List bizDocLst;

    /** Push Button, used to select archive file with absolute path. */
    private Button browseArchiveFileBtn;

    /** Push Button, used to close the dialog. */
    private Button closeBtn;

    /** Push Button, used to create or make new WSDL file. */
    private Button makeWsdlBtn;

    /**
     * Push Button, used to add service to new or existing WSDL file, available in archive file.
     */
    private Button addWsdlBtn;

    /** String, used to hold archive file name before update. */
    private String archiveFileNameBeforeUpdate = null;

    /** List, used to hold the list of WSDL files. */
    private final java.util.List wsdlFilesCreatedOrUpdated = new ArrayList();

    /** String, holds the dialog title. */
    private String title;

    /**
     * boolean for preventing multiple triggers of listener
     */
    private boolean isTextSetByRadioButtonSelection = false;

    private String fixedArchiveFile = null;

    /**
     * Constuctor
     * 
     * @param parent
     *            the parent Component
     * @param title
     *            the title String for this window
     */
    public WSDLCreateFromArchiveInfoWin(final Shell parent, final String title) {
        super(parent, title);
        this.title = title;
    }

    /**
     * Initializes the GUI by creating all the components. Invoked by superclass constructor.
     * 
     * @param parent
     *            parent composite.
     * 
     * @return returns root composite.
     */
    @Override
    public Composite initGUI(final Composite parent) {
        return createContent(parent);
    }

    /**
     * Initializes the GUI by creating all the components.
     * 
     * @param rootComp
     *            root composite.
     * 
     * @return returns root composite.
     */
    private Composite createContent(final Composite rootComp) {
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginLeft = 0;
        gridLayout.marginRight = 0;
        wsdlFileName = null;
        GridData gridData = new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER);
        rootComp.getShell().setText(title);
        rootComp.setLayout(gridLayout);
        rootComp.setLayoutData(gridData);
        final Composite mainComp = new Composite(rootComp, SWT.NONE);
        final GridLayout paramGrid = new GridLayout();
        paramGrid.numColumns = 3;
        paramGrid.horizontalSpacing = 10;
        paramGrid.verticalSpacing = 13;
        mainComp.setLayout(paramGrid);
        gridData = new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER);
        mainComp.setLayoutData(gridData);
        useExistingRadioBtn = new Button(mainComp, SWT.RADIO);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        useExistingRadioBtn.setLayoutData(gridData);
        useExistingRadioBtn.setText(translator.getString("Add service to existing WSDL"));
        useExistingRadioBtn.addSelectionListener(this);
        createNewRadioBtn = new Button(mainComp, SWT.RADIO);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        createNewRadioBtn.setLayoutData(gridData);
        createNewRadioBtn.setText(translator.getString("Create service in new WSDL"));
        createNewRadioBtn.setSelection(true);
        createNewRadioBtn.addSelectionListener(this);
        getWsdlTypeButtons(mainComp);
        final Label fileLbl = new Label(mainComp, SWT.NONE);
        fileLbl.setText(translator.getString("Archive File Name: "));
        gridData = new GridData();
        fileLbl.setLayoutData(gridData);
        archiveFileTxt = ControlFactory.createText(mainComp, SWT.BORDER);
        gridData = new GridData();
        File theFile = null;
        if (fixedArchiveFile != null) {
            gridData.widthHint = -1;
            gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
            gridData.horizontalSpan = 2;
            gridData.grabExcessHorizontalSpace = true;
            archiveFileTxt.setLayoutData(gridData);
            archiveFileTxt.setText(fixedArchiveFile);
            archiveFileTxt.setEditable(false);
            archiveFileTxt.addFocusListener(new org.eclipse.swt.events.FocusAdapter() {

                @Override
                public void focusGained(final org.eclipse.swt.events.FocusEvent e) {
                    bizDocLst.setFocus();
                }
            });
            theFile = new File(fixedArchiveFile);
        } else {
            final String defaultFile = XA_Designer_Plugin.getActiveEditedFileDirectory();
            if ((defaultFile == null) || (defaultFile.trim().length() == 0)) {
                theFile = new File(XA_Designer_Plugin.getPluginRootPath());
            } else {
                theFile = new File(defaultFile);
            }
            gridData.widthHint = 230;
            archiveFileTxt.setLayoutData(gridData);
            archiveFileTxt.setText(theFile.getAbsolutePath());
            archiveFileTxt.addModifyListener(this);
            gridData = new GridData();
            gridData.widthHint = 80;
            browseArchiveFileBtn = new Button(mainComp, SWT.NONE);
            browseArchiveFileBtn.setText(translator.getString("Browse..."));
            browseArchiveFileBtn.setLayoutData(gridData);
            browseArchiveFileBtn.addSelectionListener(this);
        }
        final Label bizDocLbl = new Label(mainComp, SWT.NONE);
        bizDocLbl.setText(translator.getString("BizDocuments: "));
        gridData = new GridData();
        gridData.widthHint = 100;
        bizDocLbl.setLayoutData(gridData);
        bizDocLst = new List(mainComp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 310;
        gridData.heightHint = 150;
        gridData.horizontalSpan = 2;
        bizDocLst.setLayoutData(gridData);
        bizDocLst.addSelectionListener(this);
        final Label wsdlFileLbl = new Label(mainComp, SWT.NONE);
        wsdlFileLbl.setText(translator.getString("WSDL File Name: "));
        gridData = new GridData();
        gridData.widthHint = 115;
        wsdlFileLbl.setLayoutData(gridData);
        wsdlFileTxt = ControlFactory.createText(mainComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 230;
        wsdlFileTxt.setLayoutData(gridData);
        wsdlFileTxt.setText(theFile.getAbsolutePath());
        wsdlFileTxt.addModifyListener(this);
        browseWSDLFileBtn = new Button(mainComp, SWT.NONE);
        browseWSDLFileBtn.setText("&" + translator.getString("Browse..."));
        gridData = new GridData();
        gridData.widthHint = 80;
        browseWSDLFileBtn.setLayoutData(gridData);
        browseWSDLFileBtn.addSelectionListener(this);
        final Label serviceNameLbl = new Label(mainComp, SWT.NONE);
        serviceNameLbl.setText(translator.getString("Service Name: "));
        gridData = new GridData();
        gridData.widthHint = 100;
        serviceNameLbl.setLayoutData(gridData);
        serviceNameTxt = ControlFactory.createText(mainComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 320;
        gridData.horizontalSpan = 2;
        serviceNameTxt.setLayoutData(gridData);
        serviceNameTxt.setText(translator.getString("MyService1"));
        serviceNameTxt.addModifyListener(this);
        final Label listenerLbl = new Label(mainComp, SWT.NONE);
        listenerLbl.setText(translator.getString("Listener: "));
        gridData = new GridData();
        gridData.widthHint = 100;
        listenerLbl.setLayoutData(gridData);
        listenerTxt = ControlFactory.createText(mainComp, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 320;
        gridData.horizontalSpan = 2;
        listenerTxt.setLayoutData(gridData);
        listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.DOC_LIT_SOAP_TYPE));
        listenerTxt.addModifyListener(this);
        return rootComp;
    }

    /**
     * Set the archive file name to a fixed name and disable any subsequent changes to the archive file selection
     * 
     * @param filePath -
     *            The absolute path to the archive file
     */
    public void setArchiveFile(final String filePath) {
        fixedArchiveFile = filePath;
    }

    /**
     * Add a WSDL type button group to the composite
     * 
     * @param mainComp
     */
    private void getWsdlTypeButtons(final Composite mainComp) {
        final Group format = new Group(mainComp, SWT.SIMPLE);
        format.setText("Web Services Style");
        final GridLayout compGridLayout = new GridLayout();
        compGridLayout.numColumns = 2;
        compGridLayout.horizontalSpacing = 10;
        compGridLayout.verticalSpacing = 13;
        GridData compGridData = new GridData(GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        compGridData.horizontalSpan = 3;
        compGridData.widthHint = 500;
        format.setLayout(compGridLayout);
        format.setLayoutData(compGridData);
        docLitWrappedRadioBtn = new Button(format, SWT.RADIO);
        docLitWrappedRadioBtn.setText("Doc/Lit Wrapped");
        docLitWrappedRadioBtn.setSelection(true);
        compGridData = new GridData();
        docLitWrappedRadioBtn.setLayoutData(compGridData);
        docLitWrappedRadioBtn.addSelectionListener(this);
        docLitUnWrappedRadioBtn = new Button(format, SWT.RADIO);
        docLitUnWrappedRadioBtn.setText("Doc/Lit Unwrapped");
        docLitUnWrappedRadioBtn.setLayoutData(compGridData);
        docLitUnWrappedRadioBtn.addSelectionListener(this);
        rpcLitRadioBtn = new Button(format, SWT.RADIO);
        rpcLitRadioBtn.setText("RPC/Literal");
        rpcLitRadioBtn.setSelection(false);
        compGridData = new GridData();
        rpcLitRadioBtn.setLayoutData(compGridData);
        rpcLitRadioBtn.addSelectionListener(this);
        rpcRadioBtn = new Button(format, SWT.RADIO);
        rpcRadioBtn.setText("RPC/Encoded");
        rpcRadioBtn.setSelection(false);
        compGridData = new GridData();
        rpcRadioBtn.setLayoutData(compGridData);
        rpcRadioBtn.addSelectionListener(this);
    }

    /**
     * This method acts as the event handler.
     * 
     * @param buttonId
     *            int parameter, holds button ids.
     */
    @Override
    protected void buttonPressed(final int buttonId) {
        boolean flag = true;
        try {
            if (MAKE_WSDL == buttonId) {
                createNewRadioBtnSelection = createNewRadioBtn.getSelection();
                wsiRadioBtnSelection = docLitWrappedRadioBtn.getSelection();
                final boolean makeSuccessful = makeWsdl();
                addWsdlBtn.setEnabled(makeSuccessful);
            } else if (ADD_WSDL_TO_ARCHIVE == buttonId) {
                wsiRadioBtnSelection = docLitWrappedRadioBtn.getSelection();
                boolean addSuccessful = addWsdlToArchive();
                addWsdlBtn.setEnabled(!addSuccessful);
            } else if (CLOSE == buttonId) {
                flag = false;
                this.close();
            }
            if (flag) {
                updateMakeWsdlEnabled();
                updateAddWsdlToArchiveEnabled();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method adds 3 buttons - Make WSDL, Add WSDL to Archive, and Close.
     * 
     * @param parent
     *            Composite parameter, holds buttons.
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        final String makeWSDLLabel = "&" + translator.getString("Make WSDL");
        final String addWSDLToArchiveLabel = "&" + translator.getString("Add WSDL to Archive");
        final String closeLabel = "&" + translator.getString("Close");
        makeWsdlBtn = createButtons(parent, MAKE_WSDL, makeWSDLLabel, false);
        GridData gridData = new GridData();
        gridData.widthHint = 100;
        makeWsdlBtn.setLayoutData(gridData);
        makeWsdlBtn.setEnabled(false);
        addWsdlBtn = createButtons(parent, ADD_WSDL_TO_ARCHIVE, addWSDLToArchiveLabel, false);
        gridData = new GridData();
        gridData.widthHint = 150;
        addWsdlBtn.setLayoutData(gridData);
        addWsdlBtn.setEnabled(false);
        closeBtn = createButtons(parent, CLOSE, closeLabel, false);
        gridData = new GridData();
        gridData.widthHint = 80;
        closeBtn.setLayoutData(gridData);
        final GridLayout layout = new GridLayout(3, false);
        parent.setLayout(layout);
        parent.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_CENTER));
        if (fixedArchiveFile != null) {
            final int extPnt = fixedArchiveFile.lastIndexOf('.') + 1;
            final String newWsdl = fixedArchiveFile.substring(0, extPnt) + "wsdl";
            wsdlFileTxt.setText(newWsdl);
            populateBizDocsFromArchive();
            archiveFileNameBeforeUpdate = getArchiveFileName();
        }
    }

    /**
     * Creates a new button with the given id.
     * 
     * @param parent
     *            the parent composite
     * @param id
     *            the id of the button
     * @param label
     *            the label from the button
     * @param defaultButton
     *            true, if the button is to be the default button, and false, otherwise.
     * 
     * @return the new button
     */
    private Button createButtons(final Composite parent, final int id, final String label, final boolean defaultButton) {
        final Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setData(new Integer(id));
        button.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                buttonPressed(((Integer) event.widget.getData()).intValue());
            }
        });
        if (defaultButton) {
            final Shell shell = parent.getShell();
            if (shell != null) {
                shell.setDefaultButton(button);
            }
        }
        return button;
    }

    /**
     * This method used to open the dialog.
     * 
     * @return returns boolean value.
     */
    @Override
    public boolean showDialog() {
        super.showDialog();
        return true;
    }

    /**
     * Callback method for the SelectionListener interface. Called when a button is pushed.
     * 
     * @param e
     *            holds instance of SelectionEvent.
     */
    public void widgetSelected(final SelectionEvent e) {
        if (e.getSource() == browseArchiveFileBtn) {
            archiveFileNameBeforeUpdate = getArchiveFileName();
            chooseFile(archiveFileTxt, XAFileConstants.XAR, SWT.OPEN);
            populateBizDocsFromArchive();
        } else if (e.getSource() == browseWSDLFileBtn) {
            if (useExistingRadioBtn.getSelection()) {
                chooseFile(wsdlFileTxt, XAFileConstants.WSDL, SWT.OPEN);
            } else {
                chooseFile(wsdlFileTxt, XAFileConstants.WSDL, SWT.SAVE);
            }
        } else if (e.getSource() == this.rpcRadioBtn) {
            isTextSetByRadioButtonSelection = true;
            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.RPC_ENCODED_SOAP_TYPE));
        } else if (e.getSource() == this.rpcLitRadioBtn) {
            isTextSetByRadioButtonSelection = true;
            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.RPC_LIT_SOAP_TYPE));
        } else if (e.getSource() == this.docLitWrappedRadioBtn) {
            isTextSetByRadioButtonSelection = true;
            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.DOC_LIT_SOAP_TYPE));
        } else if (e.getSource() == this.docLitUnWrappedRadioBtn) {
            isTextSetByRadioButtonSelection = true;
            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.DOC_LIT_UNWRAPPED_SOAP_TYPE));
        }
        updateMakeWsdlStyleEnabled();
        updateMakeWsdlEnabled();
        updateAddWsdlToArchiveEnabled();
    }

    /**
     * Callback method for the SelectionListener interface. Called when a button is pushed.
     * 
     * @param e
     *            holds instance of SelectionEvent.
     */
    public void widgetDefaultSelected(final SelectionEvent e) {
        if (e.getSource() == bizDocLst) {
            updateMakeWsdlEnabled();
        }
    }

    /**
     * Callback method for the ModifyListener interface. Called when the content is modified in Text field.
     * 
     * @param e
     *            holds instance of ModifyEvent.
     */
    public void modifyText(final ModifyEvent e) {
        if (e.getSource() == archiveFileTxt) {
            populateBizDocsFromArchive();
            archiveFileNameBeforeUpdate = getArchiveFileName();
        }
        serviceName = serviceNameTxt.getText();
        wsdlFileName = wsdlFileTxt.getText();
        listener = listenerTxt.getText();
        if (isTextSetByRadioButtonSelection) {
            isTextSetByRadioButtonSelection = false;
        } else {
            updateMakeWsdlStyleEnabled();
            updateMakeWsdlEnabled();
            updateAddWsdlToArchiveEnabled();
        }
    }

    /**
     * Performs validations prior to making the WSDL file, and if valid invokes either createNewWsdlFile() or
     * addServiceToExistingWsdlFile() to actually build the WSDL.
     * 
     * @return a boolean indicating success.
     */
    private boolean makeWsdl() {
        if (!wsdlIsValid()) {
            return false;
        }
        if (!serviceNameIsValid()) {
            return false;
        }
        if (!serviceOverwriteOK()) {
            return false;
        }
        if (createNewIsSelected()) {
            return createNewWsdlFile();
        } else {
            return addServiceToExistingWsdlFile();
        }
    }

    /**
     * Invokes the WSDLFileHelper to create the new WSDL file, then creates a pop-up to indicate either success or
     * failure.
     * 
     * @return a boolean indicating success
     */
    private boolean createNewWsdlFile() {
        String wsdlFilePath = null;
        try {
            XA_Designer_Plugin.makeBusy(Display.getCurrent().getActiveShell(), "");
            wsdlFilePath = getWsdlFile().getAbsolutePath();
            WSDLFileUtils wsdlUtils = null;
            if (rpcRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils();
            } else if (rpcLitRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils_RpcLit();
            } else if (docLitUnWrappedRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils_Unwrapped_DocLit();
            } else {
                wsdlUtils = new WSDLFileUtils_DocLit();
            }
            wsdlUtils.createNewWSDL(wsdlFilePath, UserPrefs.getSoapNamespace(), getServiceName(), getBizDocuments(), getListener(), overwriteWSDL(), getArchiveFileName());
            if (!wsdlFilesCreatedOrUpdated.contains(wsdlFilePath)) {
                wsdlFilesCreatedOrUpdated.add(wsdlFilePath);
            }
            showMessageDialog(translator.getString("New WSDL file successfully created."));
            return true;
        } catch (final Exception e) {
            showMessageDialog(translator.getString("Error creating WSDL file: ") + e.getLocalizedMessage());
            return false;
        } finally {
            XA_Designer_Plugin.makeUnBusy();
        }
    }

    /**
     * Invokes the WSDL file helper to add the new service to the existing WSDL file, then creates a pop-up to indicate
     * either success or failure.
     * 
     * @return a boolean indicating success
     */
    private boolean addServiceToExistingWsdlFile() {
        final File wsdlFile = getWsdlFile();
        try {
            XA_Designer_Plugin.makeBusy(Display.getCurrent().getActiveShell(), "");
            final String wsdlFilePath = getWsdlFile().getAbsolutePath();
            WSDLFileUtils wsdlUtils = null;
            if (rpcRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils();
            } else if (rpcLitRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils_RpcLit();
            } else if (docLitUnWrappedRadioBtn.getSelection()) {
                wsdlUtils = new WSDLFileUtils_Unwrapped_DocLit();
            } else {
                wsdlUtils = new WSDLFileUtils_DocLit();
            }
            wsdlUtils.addToExistingWSDL(wsdlFilePath, UserPrefs.getSoapNamespace(), getServiceName(), getBizDocuments(), getListener(), overwriteService(), getArchiveFileName());
            showMessageDialog(translator.getString("Successfully added ") + getServiceName() + translator.getString(" to ") + wsdlFile.getName());
            if (!wsdlFilesCreatedOrUpdated.contains(wsdlFilePath)) {
                wsdlFilesCreatedOrUpdated.add(wsdlFilePath);
            }
            return true;
        } catch (final Exception e) {
            showMessageDialog(translator.getString("Error updating ") + wsdlFile.getAbsolutePath() + translator.getString(": ") + e.getLocalizedMessage());
            return false;
        } finally {
            XA_Designer_Plugin.makeUnBusy();
        }
    }

    /**
     * Performs validations prior to adding the WSDL to the archive, and if valid invokes addFileToArchive() to actually
     * add the WSDL file to the archive.
     * 
     * @return a boolean indicating success.
     */
    private boolean addWsdlToArchive() {
        final String archiveFileName = getArchiveFileName();
        final File wsdlFile = getWsdlFile();
        final String wsdlFileName = wsdlFile.getAbsolutePath();
        try {
            if (!existingWsdlIsValid(wsdlFile)) {
                return false;
            }
            if (!archiveIsValid()) {
                return false;
            }
            if (!archiveOverwriteOK()) {
                return false;
            }
            XA_Designer_Plugin.makeBusy(Display.getCurrent().getActiveShell(), "");
            final String rootWsdlFileName = wsdlFile.getName();
            ZipFileUtils.addFileToZipFile(archiveFileName, wsdlFileName, rootWsdlFileName);
        } catch (final IOException e) {
            showMessageDialog(translator.getString("Error adding ") + wsdlFileName + translator.getString(" to ") + archiveFileName + translator.getString(": ") + e.getLocalizedMessage());
            return false;
        } finally {
            XA_Designer_Plugin.makeUnBusy();
        }
        showMessageDialog(translator.getString("Successfully added ") + wsdlFileName + translator.getString(" to ") + archiveFileName);
        return true;
    }

    /**
     * Performs validations to ensure that an archive file is ready for update.
     * 
     * @return a boolean indicating whether all of the validations succeeded.
     */
    private boolean archiveIsValid() {
        final String archiveFileName = getArchiveFileName();
        if (archiveFileName.length() == 0) {
            showMessageDialog(translator.getString("Please enter an Archive file name."));
            return false;
        }
        final File archiveFile = new File(archiveFileName);
        if (!archiveFile.exists()) {
            showMessageDialog(translator.getString("Archive file does not exist.  Please enter a new Archive file name."));
            return false;
        }
        if (!archiveFile.isFile() || !archiveFile.canWrite()) {
            showMessageDialog(translator.getString("Archive file is not a writeable file.  Please enter a new Archive file name."));
            return false;
        }
        return true;
    }

    /**
     * Checks to see whether the user is specifying to overwrite an existing WSDL file within an existing archive, and
     * if so, whether it is OK to overwrite it.
     * 
     * @return whether it is OK to proceed.
     */
    boolean archiveOverwriteOK() {
        ZipFile archiveZipFile = null;
        final String archiveName = getArchiveFileName();
        try {
            archiveZipFile = getArchiveZipFile();
            final String rootWsdlFileName = getWsdlFile().getName();
            final ZipEntry existingWsdlEntry = archiveZipFile.getEntry(rootWsdlFileName);
            if (existingWsdlEntry != null) {
                boolean overwriteArchiveWSDL = showConfirmDialog(translator.getString("WSDL file already exists in archive.  Overwrite it?"));
                if (!overwriteArchiveWSDL) {
                    return false;
                }
            }
        } catch (final IOException e) {
            showMessageDialog(translator.getString("Error reading contents of ") + archiveName + translator.getString(": ") + e.getLocalizedMessage());
        } finally {
            if (archiveZipFile != null) {
                try {
                    archiveZipFile.close();
                } catch (final IOException e) {
                    showMessageDialog(translator.getString("Error closing ") + archiveName + translator.getString(": ") + e.getLocalizedMessage());
                }
            }
        }
        return true;
    }

    /**
     * Repopulates the BizDoc JList with the BizDocs within an archive file when the archive file name is changed.
     */
    private void populateBizDocsFromArchive() {
        final String archiveName = getArchiveFileName();
        if (archiveName.length() == 0) {
            bizDocLst.removeAll();
        } else if (!archiveName.equals(archiveFileNameBeforeUpdate)) {
            final java.util.List bizDocNames = new ArrayList();
            ZipFile archiveZipFile = null;
            try {
                archiveZipFile = getArchiveZipFile();
                final Enumeration entries = archiveZipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                    final String fileName = zipEntry.getName();
                    if (fileName.endsWith(".xbd")) {
                        bizDocNames.add(fileName);
                    }
                }
                if (bizDocLst.getItemCount() > 0) {
                    bizDocLst.removeAll();
                }
                final Object[] obj = bizDocNames.toArray();
                for (int i = 0; i < obj.length; i++) {
                    bizDocLst.add(obj[i].toString());
                }
                bizDocLst.deselect(bizDocLst.getSelectionIndex());
            } catch (final IOException e) {
                bizDocLst.removeAll();
            } finally {
                if (archiveZipFile != null) {
                    try {
                        archiveZipFile.close();
                    } catch (final IOException e) {
                        showMessageDialog(translator.getString("Error closing ") + archiveName + translator.getString(": ") + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    /**
     * Updates whether the "WSDL Style" buttons are enabled based on the current values of other components.
     */
    private void updateMakeWsdlStyleEnabled() {
        if (wsdlFileName != null) {
            final File wsdlFile = new File(wsdlFileName);
            if (wsdlFile.isFile() && useExistingRadioBtn.getSelection()) {
                try {
                    final String style = WSDLFileUtils.getDocLiteralType(wsdlFileName);
                    if (style != null) {
                        if (style.equals(WSDLFileUtils.LITERAL)) {
                            rpcRadioBtn.setSelection(false);
                            rpcLitRadioBtn.setSelection(false);
                            docLitUnWrappedRadioBtn.setSelection(false);
                            docLitWrappedRadioBtn.setSelection(true);
                            isTextSetByRadioButtonSelection = true;
                            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.DOC_LIT_SOAP_TYPE));
                        } else if (style.equals(WSDLFileUtils.LITERAL_WRAPPED)) {
                            rpcRadioBtn.setSelection(false);
                            rpcLitRadioBtn.setSelection(false);
                            docLitWrappedRadioBtn.setSelection(false);
                            docLitUnWrappedRadioBtn.setSelection(true);
                            isTextSetByRadioButtonSelection = true;
                            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.DOC_LIT_UNWRAPPED_SOAP_TYPE));
                        } else if (style.equals(WSDLFileUtils.RPC_STYLE)) {
                            docLitWrappedRadioBtn.setSelection(false);
                            docLitUnWrappedRadioBtn.setSelection(false);
                            rpcLitRadioBtn.setSelection(false);
                            rpcRadioBtn.setSelection(true);
                            isTextSetByRadioButtonSelection = true;
                            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.RPC_ENCODED_SOAP_TYPE));
                        } else if (style.equals(WSDLFileUtils.RPC_LIT_STYLE)) {
                            docLitWrappedRadioBtn.setSelection(false);
                            docLitUnWrappedRadioBtn.setSelection(false);
                            rpcRadioBtn.setSelection(false);
                            rpcLitRadioBtn.setSelection(true);
                            isTextSetByRadioButtonSelection = true;
                            listenerTxt.setText(UserPrefs.getDefaultServerHost() + "/" + UserPrefs.getDefaultSoapServlet(UserPrefs.RPC_LIT_SOAP_TYPE));
                        } else if (style.equals(WSDLFileUtils.MIXED_STYLE)) {
                            docLitUnWrappedRadioBtn.setEnabled(true);
                            docLitWrappedRadioBtn.setEnabled(true);
                            rpcRadioBtn.setEnabled(true);
                            rpcLitRadioBtn.setEnabled(true);
                            createNewRadioBtn.setSelection(true);
                            useExistingRadioBtn.setSelection(false);
                            showMessageDialog(translator.getString("Mixed WSDL style is not supported.  The WSDL selected has multiple type services."));
                            return;
                        }
                        docLitUnWrappedRadioBtn.setEnabled(false);
                        docLitWrappedRadioBtn.setEnabled(false);
                        rpcRadioBtn.setEnabled(false);
                        rpcLitRadioBtn.setEnabled(false);
                        return;
                    } else {
                        createNewRadioBtn.setSelection(true);
                        useExistingRadioBtn.setSelection(false);
                        translator.getString("WSDL Style not found in WSDL: " + wsdlFileName);
                    }
                } catch (final WSDLException e1) {
                    createNewRadioBtn.setSelection(true);
                    useExistingRadioBtn.setSelection(false);
                    showMessageDialog(translator.getString("The WSDL selected " + wsdlFileName + " cause the following exception: " + e1.getMessage()));
                }
            }
        }
        docLitUnWrappedRadioBtn.setEnabled(true);
        docLitWrappedRadioBtn.setEnabled(true);
        rpcRadioBtn.setEnabled(true);
        rpcLitRadioBtn.setEnabled(true);
    }

    /**
     * Updates whether the "Make WSDL" button is enabled based on the current values of other components.
     */
    private void updateMakeWsdlEnabled() {
        final String wsdlFileName = getWsdlFileName();
        final String serviceName = getServiceName();
        final String listener = getListener();
        final boolean enableMakeWsdl = (!(bizDocLst.getSelectionIndex() < 0)) && (wsdlFileName.length() > 0) && (serviceName.length() > 0) && (listener.length() > 0);
        makeWsdlBtn.setEnabled(enableMakeWsdl);
    }

    /**
     * Updates whether the "Add WSDL to Archive" button is enabled based on the current values of other components.
     */
    private void updateAddWsdlToArchiveEnabled() {
        final String wsdlFileName = getWsdlFileName();
        final boolean enableAddWsdl = (bizDocLst.getItemCount() > 0) && (wsdlFileName.length() > 0);
        addWsdlBtn.setEnabled(enableAddWsdl);
    }

    /**
     * This method checks whether archive file is null or not.
     * 
     * @return returns empty string if archive file is null or returns trimmed archive file name.
     */
    public String getArchiveFileName() {
        final String archiveFileName = archiveFileTxt.getText();
        if (archiveFileName != null) {
            return archiveFileName.trim();
        } else {
            return "";
        }
    }

    /**
     * This method returns of list of selected biz documents.
     * 
     * @return returns of list of selected biz documents.
     */
    public java.util.List getBizDocuments() {
        final String[] selectedItems = bizDocLst.getSelection();
        return Arrays.asList(selectedItems);
    }

    /**
     * This method returns the new archive zip file.
     * 
     * @return returns the new archive zip file.
     * 
     * @throws IOException
     *             throws IO Exception.
     */
    private ZipFile getArchiveZipFile() throws IOException {
        return new ZipFile(getArchiveFileName());
    }

    /**
     * This method returns the list of WSDL files and stored in List collection.
     * 
     * @return returns the list of WSDL files.
     */
    public java.util.List getWsdlFilesCreatedOrUpdated() {
        return this.wsdlFilesCreatedOrUpdated;
    }
}
