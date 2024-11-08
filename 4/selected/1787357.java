package org.xaware.ide.xadev.gui.dialog;

import java.io.File;
import javax.wsdl.WSDLException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.gui.XADialog;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.WSDLFileUtils;

/**
 * WSDLCreateBaseWin - It is a base GUI class for Make WSDL and Make WSDL From Archive modules.
 * 
 * @author G Bharath Kumar
 * @version 1.0
 */
public abstract class WSDLCreateBaseWin extends XADialog {

    /** Translator instance. */
    public static final Translator translator = Translator.getInstance();

    /** Radio button, used to create new service to new WSDL. */
    protected Button createNewRadioBtn;

    /** Radio button, used to add new service to existing WSDL. */
    protected Button useExistingRadioBtn;

    /** Radio buttons for WSDL format */
    protected Button rpcRadioBtn;

    /** Radio buttons for WSDL format */
    protected Button rpcLitRadioBtn;

    /** wrapped doc lit */
    protected Button docLitWrappedRadioBtn;

    /** unwrapped doc lit */
    protected Button docLitUnWrappedRadioBtn;

    /** Text Field, used to hold WSDL file name with absolute path. */
    protected Text wsdlFileTxt;

    /** Push Button, used to select WSDL file name with absolute path. */
    protected Button browseWSDLFileBtn;

    /** Text Field, used to hold service name. */
    protected Text serviceNameTxt;

    /** Text Field, used to hold listener name. */
    protected Text listenerTxt;

    /** Boolean, holds the status, used to overwrite WSDL file or not. */
    private boolean overwriteWSDL = false;

    /** Boolean, holds the status, used to overwrite service or not. */
    private boolean overwriteService = false;

    /** String, holds service name. */
    protected String serviceName;

    /** String, holds WSDL file name. */
    protected String wsdlFileName;

    /** String, holds listener. */
    protected String listener;

    /** Boolean, holds status of create new service radio option. */
    protected boolean createNewRadioBtnSelection = false;

    /** Boolean holds type of WSDL */
    protected boolean wsiRadioBtnSelection = false;

    /**
     * Constuctor
     * 
     * @param parent
     *            the parent Component.
     * @param title
     *            the title String for this window.
     */
    public WSDLCreateBaseWin(final Shell parent, final String title) {
        super(parent, null, null, title, true, true);
    }

    /**
     * This method returns the root composite to the parent shell.
     * 
     * @param parent
     *            parent composite.
     * 
     * @return returns the root composite to the parent shell.
     */
    @Override
    protected Control createContents(final Composite parent) {
        setToShow(initGUI(parent));
        return super.createContents(parent);
    }

    /**
     * Initializes the GUI by creating all the components and setting their positions.
     * 
     * @param parent
     *            parent composite
     * 
     * @return returns the root composite.
     */
    public abstract Composite initGUI(Composite parent);

    /**
     * Shows the dialog box for the window.
     * 
     * @return returns the boolean status of the dialog.
     */
    public boolean showDialog() {
        open();
        return true;
    }

    /**
     * This method checks whether service name is empty or not.
     * 
     * @return returns whether service name is empty or not.
     */
    boolean serviceNameIsEmpty() {
        return (getServiceName().length() == 0);
    }

    /**
     * This method checks whether the service name contains any empty spaces or not.
     * 
     * @return returns the boolean status of existence or non-existence of empty spaces in service name.
     */
    boolean serviceNameContainsSpaces() {
        final String serviceName = getServiceName();
        final char SPACE = ' ';
        return (serviceName.indexOf(SPACE) > -1);
    }

    /**
     * This method checks whether service name exists in WSDL file or not.
     * 
     * @return returns boolean status of existence or non-existence of service name in WSDL.
     * 
     * @throws WSDLException
     *             throws WSDL Exception.
     */
    boolean serviceExistsInWSDL() throws WSDLException {
        return WSDLFileUtils.serviceExists(getServiceName(), getWsdlFile().getAbsolutePath());
    }

    /**
     * This method checks whether WSDL field is empty or not.
     * 
     * @return returns whether WSDL field is empty or not.
     */
    boolean wsdlFileNameIsEmpty() {
        return (getWsdlFileName().length() == 0);
    }

    /**
     * Performs common validations on the serviceName.
     * 
     * @return a boolean indicating whether all of the common validations succeeded.
     */
    boolean serviceNameIsValid() {
        if (serviceNameIsEmpty()) {
            showMessageDialog(translator.getString("Please enter a Service name."));
            return false;
        }
        if (serviceNameContainsSpaces()) {
            showMessageDialog(translator.getString("Service name may not contain spaces"));
            return false;
        }
        return true;
    }

    /**
     * Performs common validations on the WSDL file.
     * 
     * @return a boolean indicating whether all of the common validations succeeded.
     */
    boolean wsdlIsValid() {
        overwriteWSDL = false;
        if (wsdlFileNameIsEmpty()) {
            showMessageDialog(translator.getString("Please enter a WSDL file name."));
            return false;
        }
        File wsdlFile = getWsdlFile();
        if (wsdlFile.isDirectory()) {
            showMessageDialog(translator.getString("Please enter a WSDL file name."));
            return false;
        }
        if (createNewIsSelected()) {
            String wsdlFileName = getWsdlFileName();
            if (!wsdlFileName.endsWith(".wsdl")) {
                if (wsdlFileName.endsWith(".")) {
                    wsdlFileName = wsdlFileName + "wsdl";
                } else {
                    wsdlFileName = wsdlFileName + ".wsdl";
                }
                this.wsdlFileName = wsdlFileName;
                wsdlFileTxt.setText(wsdlFileName);
                wsdlFile = getWsdlFile();
            }
            if (wsdlFile.isDirectory()) {
                showMessageDialog(translator.getString("WSDL file is a directory.  Please enter a new WSDL file name."));
                return false;
            }
            if (wsdlFile.exists()) {
                overwriteWSDL = showConfirmDialog(translator.getString("WSDL file already exists.  Overwrite it?"));
                if (!overwriteWSDL) {
                    return false;
                }
            }
        } else {
            if (!existingWsdlIsValid(wsdlFile)) {
                return false;
            }
            if (!wsdlFile.canWrite()) {
                showMessageDialog(translator.getString("WSDL file is not a writeable file.  Please enter a new WSDL file name."));
                return false;
            }
        }
        return true;
    }

    /**
     * Performs common validations on existing WSDL files.
     * 
     * @param wsdlFile
     *            WSDL file.
     * 
     * @return a boolean indicating whether all of the common validations succeeded.
     */
    boolean existingWsdlIsValid(final File wsdlFile) {
        if (!wsdlFile.exists()) {
            showMessageDialog(translator.getString("WSDL file does not exist.  Please enter a new WSDL file name."));
            return false;
        }
        if (!wsdlFile.isFile() || !wsdlFile.canRead()) {
            showMessageDialog(translator.getString("WSDL file is not a readable file.  Please enter a new WSDL file name."));
            return false;
        }
        try {
            WSDLFileUtils.getDefinition(wsdlFile.getAbsolutePath());
        } catch (final Exception e) {
            showMessageDialog(translator.getString("Error reading WSDL file: ") + e.getLocalizedMessage() + translator.getString(".  Please enter a new WSDL file name."));
        }
        return true;
    }

    /**
     * Checks to see whether the user is specifying to overwrite an existing service within an existing WSDL file, and
     * if so, whether it is OK to overwrite it.
     * 
     * @return whether it is OK to proceed.
     */
    boolean serviceOverwriteOK() {
        overwriteService = false;
        if (addToExistingIsSelected()) {
            final File wsdlFile = getWsdlFile();
            try {
                if (WSDLFileUtils.serviceExists(getServiceName(), wsdlFile.getAbsolutePath())) {
                    overwriteService = showConfirmDialog(translator.getString("Service already exists.  Overwrite it?"));
                    if (!overwriteService) {
                        return false;
                    }
                }
            } catch (final WSDLException e) {
                showMessageDialog(translator.getString("Error reading ") + wsdlFile.getAbsolutePath() + translator.getString(": ") + e.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method for invoking the FileChooser
     * 
     * @param textField
     *            the SWT Text field to be populated with the results of the XAFileChooser.
     * @param fileType
     *            the type of file to be selected by the XAFileChooser.
     * @param mode
     *            DOCUMENT ME!
     */
    void chooseFile(final Text textField, final int fileType, final int mode) {
        String startFile = textField.getText();
        if ((startFile == null) || startFile.equals("")) {
            startFile = XA_Designer_Plugin.getPluginRootPath();
        }
        final XAChooser fileChooser = new XAChooser(Display.getCurrent().getActiveShell(), startFile, mode);
        fileChooser.addDefaultFilter(fileType);
        fileChooser.addFilter(fileType);
        final String selectedFile = fileChooser.open();
        if ((selectedFile != null) && (selectedFile.length() > 0)) {
            textField.setText(selectedFile);
        }
    }

    /**
     * Convenience method for displaying a message dialog.
     * 
     * @param message
     *            the message to be displayed on the dialog
     */
    void showMessageDialog(final String message) {
        ControlFactory.showMessageDialog(translator.getString(message), translator.getString("Information"));
    }

    /**
     * Convenience method for displaying a confirmation dialog.
     * 
     * @param message
     *            the message to be displayed on the dialog
     * 
     * @return a boolean indicating whether the user selected "Yes".
     */
    boolean showConfirmDialog(final String message) {
        final int selectedOption = ControlFactory.showConfirmDialog(translator.getString(message), translator.getString("Confirmation"), false);
        return selectedOption == Window.OK;
    }

    /**
     * This method returns the status of select/un-select of create service to new WSDL radio button option.
     * 
     * @return returns the status of select/un-select.
     */
    public boolean createNewIsSelected() {
        return createNewRadioBtnSelection;
    }

    /**
     * This method returns the status of select/un-select of add service to existing WSDL radio button option.
     * 
     * @return returns the status of select/un-select.
     */
    public boolean addToExistingIsSelected() {
        return useExistingRadioBtn.getSelection();
    }

    /**
     * This method checks whether WSDL file is null or not.
     * 
     * @return returns empty string if WSDL file is null or returns trimmed WSDL file name.
     */
    public String getWsdlFileName() {
        if (wsdlFileName != null) {
            return wsdlFileName.trim();
        } else {
            return "";
        }
    }

    /**
     * This method returns File instance of WSDL file.
     * 
     * @return returns File instance.
     */
    public File getWsdlFile() {
        return new File(getWsdlFileName());
    }

    /**
     * This method checks whether service name is null or not.
     * 
     * @return returns empty string if service name is null or returns trimmed service name.
     */
    public String getServiceName() {
        if (serviceName != null) {
            return serviceName.trim();
        } else {
            return "";
        }
    }

    /**
     * This method checks whether listener is null or not.
     * 
     * @return returns empty string if listener is null or returns trimmed listener.
     */
    public String getListener() {
        if (listener != null) {
            return listener.trim();
        } else {
            return "";
        }
    }

    /**
     * This method returns Boolean, holds the status, used to overwrite service name or not.
     * 
     * @return returns status of overwrite or not.
     */
    public boolean overwriteService() {
        return this.overwriteService;
    }

    /**
     * This method returns Boolean, holds the status, used to overwrite WSDL file or not.
     * 
     * @return returns status of overwrite or not.
     */
    public boolean overwriteWSDL() {
        return this.overwriteWSDL;
    }
}
