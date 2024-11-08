package com.prolix.editor.main.workspace.export.check;

import java.io.File;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import com.prolix.editor.GlobalConstants;
import com.prolix.editor.LDT_Constrains;
import com.prolix.editor.graph.check.lvla.DiagramToReloadA;

public class ExportComposite extends Composite {

    private CheckGLMComposite parent;

    private Text txt_exportpath;

    private Button browse;

    private Button export;

    public ExportComposite(CheckGLMComposite parent, Composite composite) {
        super(composite, SWT.NONE);
        this.parent = parent;
        setupView();
        genListener();
    }

    private void setupView() {
        setLayout(new FillLayout());
        Group group = new Group(this, SWT.NONE);
        group.setText(" Export your Learning Design to an IMS Unit of Learning ");
        group.setFont(GlobalConstants.DIALOG_HEADER_FONT);
        group.setLayout(new GridLayout(2, false));
        Label lbl_instructions = new Label(group, SWT.WRAP);
        lbl_instructions.setText("Please specify the filename and the location for the Learning Design to be exported");
        lbl_instructions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        export = new Button(group, SWT.PUSH);
        export.setText("Export");
        export.setFont(GlobalConstants.DIALOG_HEADER_FONT);
        export.setImage(LDT_Constrains.ICON_EXPORT);
        export.setLayoutData(new GridData(200, SWT.DEFAULT));
        export.setEnabled(false);
        insertSeparator(group, 2);
        Composite composite = new Composite(group, SWT.NONE);
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData) composite.getLayoutData()).horizontalSpan = 2;
        Label label = new Label(composite, SWT.NONE);
        label.setText("Export File:");
        txt_exportpath = new Text(composite, SWT.BORDER);
        txt_exportpath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        browse = new Button(composite, SWT.PUSH);
        browse.setText("Browse");
        browse.setFont(GlobalConstants.DIALOG_HEADER_FONT);
        browse.setImage(LDT_Constrains.ICON_BROWSE);
    }

    private void genListener() {
        browse.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });
        export.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                handleExport();
            }
        });
        txt_exportpath.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                boolean status = txt_exportpath.getText() != null;
                status = status && !txt_exportpath.getText().trim().isEmpty();
                export.setEnabled(status);
            }
        });
    }

    private void handleBrowse() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setText("Choose a file name for the Zip file");
        dialog.setFilterExtensions(new String[] { "*.zip" });
        String path = dialog.open();
        if (path != null) {
            if (!path.toLowerCase().endsWith(".zip")) {
                path += ".zip";
            }
            this.txt_exportpath.setText(path);
        }
    }

    private boolean checkExport() {
        DiagramToReloadA checkDiagram = new DiagramToReloadA(parent.getContainer());
        if (!checkDiagram.check()) {
            MessageBox errormessage = new MessageBox(getShell(), SWT.OK | SWT.ICON_ERROR);
            errormessage.setText("Export Error");
            errormessage.setMessage("Sorry can't export the Diagram because it contains errors!\nFix them please.");
            errormessage.open();
            return false;
        }
        return true;
    }

    private void handleExport() {
        if (!checkExport()) return;
        String exportPath = txt_exportpath.getText();
        if (exportPath != null && exportPath.trim().length() > 0) {
            if (!exportPath.toLowerCase().endsWith(".zip")) {
                exportPath += ".zip";
            }
            File isDa = new File(exportPath);
            if (isDa.exists()) {
                MessageBox errormessage = new MessageBox(getShell(), SWT.OK | SWT.CANCEL | SWT.ICON_WARNING);
                errormessage.setText("Export Warning");
                errormessage.setMessage("The given file already exists. Do you really want to overwrite it?");
                int status = errormessage.open();
                if (status == SWT.CANCEL) return;
            }
            try {
                parent.getContainer().getRessourceManager().saveGLMIMS(exportPath);
                MessageDialog.openInformation(getShell(), "Export", "Export successful!");
            } catch (Exception ex) {
                MessageDialog.openError(getShell(), "Export", ex.getMessage());
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            MessageDialog.openError(getShell(), "Export", "An Export destination has not been specified");
        }
    }

    /**
	 * Insert Separator on desired location
	 */
    protected Label insertSeparator(Composite composite, int span) {
        Label dummy = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_SOLID);
        dummy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData) dummy.getLayoutData()).horizontalSpan = span;
        return dummy;
    }
}
