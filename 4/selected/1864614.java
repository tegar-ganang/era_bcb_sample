package net.sf.elbe.ui.wizards;

import java.io.File;
import net.sf.elbe.ui.widgets.BaseWidgetUtils;
import net.sf.elbe.ui.widgets.FileBrowserWidget;
import net.sf.elbe.ui.widgets.WidgetModifyEvent;
import net.sf.elbe.ui.widgets.WidgetModifyListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public abstract class ExportBaseToPage extends WizardPage {

    protected ExportBaseWizard wizard;

    protected FileBrowserWidget fileBrowserWidget;

    protected Button overwriteFileButton;

    public ExportBaseToPage(String pageName, ExportBaseWizard wizard) {
        super(pageName);
        super.setPageComplete(false);
        super.setTitle(getFileType() + " File");
        super.setDescription("Please enter the target " + getFileType() + " file.");
        this.wizard = wizard;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    protected void validate() {
        boolean ok = true;
        File file = new File(fileBrowserWidget.getFilename());
        File fileDirectory = file.getParentFile();
        if ("".equals(fileBrowserWidget.getFilename())) {
            setErrorMessage(null);
            ok = false;
        } else if (file.isDirectory()) {
            setErrorMessage("Selected " + getFileType() + " is no file.");
            ok = false;
        } else if (file.exists() && !this.overwriteFileButton.getSelection()) {
            setErrorMessage("Selected " + getFileType() + " file already exists. Select option 'Overwrite existing " + getFileType() + " file' if you want to overwrite the " + getFileType() + " file.");
            ok = false;
        } else if (file.exists() && !file.canWrite()) {
            setErrorMessage("Selected " + getFileType() + " file is not writeable.");
            ok = false;
        } else if (file.getParentFile() == null) {
            setErrorMessage("Selected " + getFileType() + " file directory is not writeable.");
            ok = false;
        } else if (!file.exists() && (fileDirectory == null || !fileDirectory.canWrite())) {
            setErrorMessage("Selected " + getFileType() + " file directory is not writeable.");
            ok = false;
        }
        if (ok) {
            setErrorMessage(null);
        }
        setPageComplete(ok && wizard.getExportFilename() != null && !"".equals(wizard.getExportFilename()));
    }

    public void createControl(Composite composite) {
        BaseWidgetUtils.createLabel(composite, getFileType() + " File:", 1);
        fileBrowserWidget = new FileBrowserWidget("Select " + getFileType() + " File", getExtensions(), FileBrowserWidget.TYPE_SAVE);
        fileBrowserWidget.createWidget(composite);
        fileBrowserWidget.addWidgetModifyListener(new WidgetModifyListener() {

            public void widgetModified(WidgetModifyEvent event) {
                wizard.setExportFilename(fileBrowserWidget.getFilename());
                validate();
            }
        });
        BaseWidgetUtils.createRadioIndent(composite, 1);
        overwriteFileButton = BaseWidgetUtils.createCheckbox(composite, "O&verwrite existing " + getFileType() + " file", 2);
        overwriteFileButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                validate();
            }
        });
        fileBrowserWidget.setFocus();
        setControl(composite);
        validate();
    }

    protected abstract String[] getExtensions();

    protected abstract String getFileType();

    public void saveDialogSettings() {
        this.fileBrowserWidget.saveDialogSettings();
    }
}
