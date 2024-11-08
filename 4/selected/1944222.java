package net.sf.elbe.ui.wizards;

import java.io.File;
import net.sf.elbe.ui.ELBEUIPlugin;
import net.sf.elbe.ui.ELBEUIConstants;
import net.sf.elbe.ui.widgets.BaseWidgetUtils;
import net.sf.elbe.ui.widgets.FileBrowserWidget;
import net.sf.elbe.ui.widgets.WidgetModifyEvent;
import net.sf.elbe.ui.widgets.WidgetModifyListener;
import net.sf.elbe.ui.widgets.search.ConnectionWidget;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class ImportLdifMainWizardPage extends WizardPage {

    public static final String CONTINUE_ON_ERROR_DIALOGSETTING_KEY = ImportLdifMainWizardPage.class.getName() + ".continueOnError";

    private static final String[] EXTENSIONS = new String[] { "*.ldif", "*.*" };

    private ImportLdifWizard wizard;

    private FileBrowserWidget ldifFileBrowserWidget;

    private ConnectionWidget connectionWidget;

    private Button enableLoggingButton;

    private Button useDefaultLogfileButton;

    private Button useCustomLogfileButton;

    private String customLogfileName;

    private FileBrowserWidget logFileBrowserWidget;

    private Button overwriteLogfileButton;

    private Button continueOnErrorButton;

    public ImportLdifMainWizardPage(String pageName, ImportLdifWizard wizard) {
        super(pageName);
        super.setTitle("LDIF Import");
        super.setDescription("Please select a connection and the LDIF to import");
        super.setImageDescriptor(ELBEUIPlugin.getDefault().getImageDescriptor(ELBEUIConstants.IMG_IMPORT_LDIF_WIZARD));
        super.setPageComplete(false);
        this.wizard = wizard;
    }

    public void dispose() {
        super.dispose();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    private void validate() {
        boolean ok = true;
        File ldifFile = new File(ldifFileBrowserWidget.getFilename());
        if ("".equals(ldifFileBrowserWidget.getFilename())) {
            setErrorMessage(null);
            ok = false;
        } else if (!ldifFile.isFile() || !ldifFile.exists()) {
            setErrorMessage("Selected LDIF file doesn't exist.");
            ok = false;
        } else if (!ldifFile.canRead()) {
            setErrorMessage("Selected LDIF file is not readable.");
            ok = false;
        } else if (this.enableLoggingButton.getSelection()) {
            File logFile = new File(logFileBrowserWidget.getFilename());
            File logFileDirectory = logFile.getParentFile();
            if (logFile.equals(ldifFile)) {
                setErrorMessage("LDIF file and Logfile must not be equal.");
                ok = false;
            } else if (logFile.isDirectory()) {
                setErrorMessage("Selected logfile is no file.");
                ok = false;
            } else if (logFile.exists() && !this.overwriteLogfileButton.getSelection()) {
                setErrorMessage("Selected logfile already exists. Select option 'Overwrite existing logfile' if you want to overwrite the logfile.");
                ok = false;
            } else if (logFile.exists() && !logFile.canWrite()) {
                setErrorMessage("Selected logfile is not writeable.");
                ok = false;
            } else if (logFile.getParentFile() == null) {
                setErrorMessage("Selected logfile directory is not writeable.");
                ok = false;
            } else if (!logFile.exists() && (logFileDirectory == null || !logFileDirectory.canWrite())) {
                setErrorMessage("Selected logfile directory is not writeable.");
                ok = false;
            }
        }
        if (wizard.getImportConnection() == null) {
            ok = false;
        }
        if (ok) {
            setErrorMessage(null);
        }
        setPageComplete(ok);
        getContainer().updateButtons();
    }

    public void createControl(Composite parent) {
        Composite composite = BaseWidgetUtils.createColumnContainer(parent, 3, 1);
        BaseWidgetUtils.createLabel(composite, "LDIF file:", 1);
        ldifFileBrowserWidget = new FileBrowserWidget("Select LDIF File", EXTENSIONS, FileBrowserWidget.TYPE_OPEN);
        ldifFileBrowserWidget.createWidget(composite);
        ldifFileBrowserWidget.addWidgetModifyListener(new WidgetModifyListener() {

            public void widgetModified(WidgetModifyEvent event) {
                wizard.setLdifFilename(ldifFileBrowserWidget.getFilename());
                if (useDefaultLogfileButton.getSelection()) {
                    logFileBrowserWidget.setFilename(ldifFileBrowserWidget.getFilename() + ".log");
                }
                validate();
            }
        });
        BaseWidgetUtils.createLabel(composite, "Import into:", 1);
        connectionWidget = new ConnectionWidget(wizard.getImportConnection());
        connectionWidget.createWidget(composite);
        connectionWidget.addWidgetModifyListener(new WidgetModifyListener() {

            public void widgetModified(WidgetModifyEvent event) {
                wizard.setImportConnection(connectionWidget.getConnection());
                validate();
            }
        });
        Composite loggingOuterComposite = BaseWidgetUtils.createColumnContainer(composite, 1, 3);
        Group loggingGroup = BaseWidgetUtils.createGroup(loggingOuterComposite, "Logging", 1);
        Composite loggingContainer = BaseWidgetUtils.createColumnContainer(loggingGroup, 3, 1);
        enableLoggingButton = BaseWidgetUtils.createCheckbox(loggingContainer, "Enable logging", 3);
        enableLoggingButton.setSelection(true);
        wizard.setEnableLogging(enableLoggingButton.getSelection());
        enableLoggingButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                wizard.setEnableLogging(enableLoggingButton.getSelection());
                useDefaultLogfileButton.setEnabled(enableLoggingButton.getSelection());
                useCustomLogfileButton.setEnabled(enableLoggingButton.getSelection());
                logFileBrowserWidget.setEnabled(enableLoggingButton.getSelection() && useCustomLogfileButton.getSelection());
                overwriteLogfileButton.setEnabled(enableLoggingButton.getSelection());
                validate();
            }
        });
        BaseWidgetUtils.createRadioIndent(loggingContainer, 1);
        useDefaultLogfileButton = BaseWidgetUtils.createRadiobutton(loggingContainer, "Use default logfile", 2);
        useDefaultLogfileButton.setSelection(true);
        useDefaultLogfileButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                String temp = customLogfileName;
                logFileBrowserWidget.setFilename(ldifFileBrowserWidget.getFilename() + ".log");
                logFileBrowserWidget.setEnabled(false);
                customLogfileName = temp;
                validate();
            }
        });
        BaseWidgetUtils.createRadioIndent(loggingContainer, 1);
        useCustomLogfileButton = BaseWidgetUtils.createRadiobutton(loggingContainer, "Use custom logfile", 2);
        useCustomLogfileButton.setSelection(false);
        useCustomLogfileButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                logFileBrowserWidget.setFilename(customLogfileName != null ? customLogfileName : "");
                logFileBrowserWidget.setEnabled(true);
                validate();
            }
        });
        BaseWidgetUtils.createRadioIndent(loggingContainer, 1);
        logFileBrowserWidget = new FileBrowserWidget("Select Logfile", null, FileBrowserWidget.TYPE_SAVE);
        logFileBrowserWidget.createWidget(loggingContainer);
        logFileBrowserWidget.addWidgetModifyListener(new WidgetModifyListener() {

            public void widgetModified(WidgetModifyEvent event) {
                customLogfileName = logFileBrowserWidget.getFilename();
                wizard.setLogFilename(customLogfileName);
                validate();
            }
        });
        logFileBrowserWidget.setEnabled(false);
        BaseWidgetUtils.createRadioIndent(loggingContainer, 1);
        overwriteLogfileButton = BaseWidgetUtils.createCheckbox(loggingContainer, "Overwrite existing logfile", 2);
        overwriteLogfileButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                validate();
            }
        });
        continueOnErrorButton = BaseWidgetUtils.createCheckbox(composite, "Continue on error", 3);
        if (ELBEUIPlugin.getDefault().getDialogSettings().get(CONTINUE_ON_ERROR_DIALOGSETTING_KEY) == null) {
            ELBEUIPlugin.getDefault().getDialogSettings().put(CONTINUE_ON_ERROR_DIALOGSETTING_KEY, false);
        }
        continueOnErrorButton.setSelection(ELBEUIPlugin.getDefault().getDialogSettings().getBoolean(CONTINUE_ON_ERROR_DIALOGSETTING_KEY));
        wizard.setContinueOnError(continueOnErrorButton.getSelection());
        continueOnErrorButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                wizard.setContinueOnError(continueOnErrorButton.getSelection());
                validate();
            }
        });
        setControl(composite);
    }

    public void saveDialogSettings() {
        this.ldifFileBrowserWidget.saveDialogSettings();
        ELBEUIPlugin.getDefault().getDialogSettings().put(CONTINUE_ON_ERROR_DIALOGSETTING_KEY, continueOnErrorButton.getSelection());
    }
}
