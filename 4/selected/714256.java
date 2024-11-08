package com.prolix.editor.dialogs.export;

import java.io.File;
import java.util.Iterator;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import uk.ac.reload.straker.datamodel.learningdesign.LD_CheckListItem;
import com.prolix.editor.GlobalConstants;
import com.prolix.editor.LDT_Constrains;
import com.prolix.editor.dialogs.BasicGLMDialog;
import com.prolix.editor.graph.check.lvla.DiagramToReloadA;
import com.prolix.editor.main.workspace.export.check.treemodel.CheckElement;
import com.prolix.editor.main.workspace.export.check.treemodel.CheckExportErrorList;
import com.prolix.editor.main.workspace.export.check.treemodel.LD_CheckListItemAdapterForCheckElement;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;

public class ExportDialog extends BasicGLMDialog {

    public static final String SUCCESS_MESSAGE = "Your learning design has been successfully checked for validity.\n" + "You are now ready to save the generated unit of learning.";

    public static final String ERROR_MESSAGE = "You must correct the indicated errors before a runnable unit of learning can be generated.";

    private Label lbl_status_message;

    private Text txt_filename;

    private Text txt_dir;

    private Button btn_browse;

    private Button btn_export;

    private LearningDesignDataModel container;

    public ExportDialog(Shell parentShell, LearningDesignDataModel lddm) {
        super(parentShell);
        this.container = lddm;
    }

    protected boolean executeOK() {
        return true;
    }

    protected void finish() {
    }

    protected String getDialogHeadText() {
        return "Export Learning Design";
    }

    protected String getDialogMessageText() {
        return "Using this function, you are able to generate a unit of learning that is conform to the IMS Learning Design specification from the learning " + "design you graphically built. A unit of learning may be imported in learning management systems.";
    }

    protected String getDialogTitleText() {
        return "Generate runnable unit of learning";
    }

    protected Point getSize() {
        return new Point(880, 500);
    }

    protected void setupView(Composite parent) {
        DiagramToReloadA checkDiagram = new DiagramToReloadA(container);
        if (checkDiagram.check()) {
            setupExportView(parent);
            return;
        }
        CheckElement root = new CheckElement("root");
        checkDiagram.getCheckElements(root);
        buildReloadCheck(root);
        checkDiagram.clean();
        Composite check = new Composite(parent, SWT.NONE);
        check.setLayoutData(new GridData(GridData.FILL_BOTH));
        check.setLayout(new GridLayout(1, false));
        Label message = new Label(check, SWT.NONE);
        message.setText("You must correct the indicated errors before a runnable unit of learning can be generated.");
        CheckExportErrorList errorListe = new CheckExportErrorList(check);
        Iterator it = root.getChildren().iterator();
        while (it.hasNext()) {
            CheckElement element = (CheckElement) it.next();
            if (element.isError()) {
                errorListe.addCheckElement(element);
            }
        }
    }

    private void buildReloadCheck(CheckElement root) {
        LD_CheckListItem[] list = container.getLearningDesign().getCheckList().getCheckListItems();
        for (int i = 0; i < list.length; i++) new LD_CheckListItemAdapterForCheckElement(root, list[i]);
    }

    private void setupExportView(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite cmp_image = new Composite(composite, SWT.NONE);
        cmp_image.setLayout(new GridLayout());
        cmp_image.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Composite cmp_content = new Composite(composite, SWT.NONE);
        cmp_content.setLayout(new GridLayout());
        cmp_content.setLayoutData(new GridData(GridData.FILL_BOTH));
        generateImageControls(cmp_image);
        generateStatusMessageControls(cmp_content);
        insertSeparator(cmp_content, 1);
        generateFileControls(cmp_content);
        insertSeparator(composite, 2);
        generateExportButton(composite);
    }

    private void generateImageControls(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Label lbl_image = new Label(composite, SWT.NONE);
        lbl_image.setImage(LDT_Constrains.IMAGE_EXPORT);
    }

    private void generateStatusMessageControls(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData) composite.getLayoutData()).heightHint = 60;
        lbl_status_message = new Label(composite, SWT.NONE | SWT.CENTER | SWT.WRAP);
        lbl_status_message.setFont(GlobalConstants.DIALOG_HEADER_FONT_LARGE);
        lbl_status_message.setForeground(ColorConstants.darkGreen);
        lbl_status_message.setText(SUCCESS_MESSAGE);
        lbl_status_message.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    }

    private void generateFileControls(Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        composite.setLayout(new GridLayout(3, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(composite, SWT.NONE).setText("Filename:");
        txt_filename = new Text(composite, SWT.BORDER);
        txt_filename.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData) txt_filename.getLayoutData()).horizontalSpan = 2;
        txt_filename.setText(container.getRessourceManager().getZipFileNameWithoutExtention() + "_UoL");
        new Label(composite, SWT.NONE).setText("Directory");
        txt_dir = new Text(composite, SWT.BORDER);
        txt_dir.setEditable(false);
        txt_dir.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        txt_dir.setText(container.getRessourceManager().getZipFileLocation());
        btn_browse = new Button(composite, SWT.PUSH);
        btn_browse.setImage(LDT_Constrains.ICON_BROWSE);
        btn_browse.setText("Browse");
        btn_browse.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                handleBrowse();
            }
        });
    }

    private void generateExportButton(Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData) composite.getLayoutData()).horizontalSpan = 2;
        btn_export = new Button(composite, SWT.PUSH);
        btn_export.setImage(LDT_Constrains.ICON_EXPORT);
        btn_export.setFont(GlobalConstants.DIALOG_HEADER_FONT_LARGE);
        btn_export.setText("Generate runnable unit of learning");
        btn_export.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        ((GridData) btn_export.getLayoutData()).heightHint = 60;
        ((GridData) btn_export.getLayoutData()).widthHint = 320;
        btn_export.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                handleExport();
            }
        });
    }

    protected void validate() {
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Close", true);
    }

    private void handleBrowse() {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
        dialog.setText("Choose a Directory where the Zip file will be placed");
        String path = dialog.open();
        if (path != null && !path.isEmpty()) txt_dir.setText(path);
    }

    private void handleExport() {
        if (txt_filename.getText() == null || txt_filename.getText().trim().length() == 0) {
            MessageDialog.openError(getShell(), "Export", "An Export name has not been specified");
            return;
        }
        if (txt_dir.getText() == null || txt_dir.getText().trim().length() == 0) {
            MessageDialog.openError(getShell(), "Export", "An Export destination has not been specified");
            return;
        }
        String exportPath = txt_dir.getText() + File.separator + txt_filename.getText();
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
            container.getRessourceManager().saveGLMIMS(exportPath);
            MessageDialog.openInformation(getShell(), "Export", "Export successful!");
        } catch (Exception ex) {
            MessageDialog.openError(getShell(), "Export", ex.getMessage());
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
