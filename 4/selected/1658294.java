package org.semanticgov.ui.admin.repositoryclient;

import org.apache.axis2.AxisFault;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.semanticgov.ui.admin.goaltree.Activator;
import org.semanticgov.ui.admin.repositoryclient.GoalTreeRepositoryStub.*;
import org.wsmostudio.runtime.LogManager;
import org.wsmostudio.runtime.WsmoImageRegistry;

public class PublishGoalTreeDialog {

    public static final String PORTAL_ENDPOINT_PROP = "$portalEndpoint$";

    private Shell shell;

    private Text endpointField;

    private String wsmlData, xmlData, ontoID;

    public PublishGoalTreeDialog(Shell parentShell, String wsmlData, String xmlData, String ontoID) {
        this.wsmlData = wsmlData;
        this.xmlData = xmlData;
        this.ontoID = ontoID;
        shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.SYSTEM_MODAL);
        Image icon = JFaceResources.getImageRegistry().get(WsmoImageRegistry.DEFAULT_WINDOW_ICON);
        shell.setImage(icon);
        shell.setText("Goal Tree Upload");
        shell.setSize(500, 150);
        shell.setLayout(new GridLayout(1, false));
        Group endpointGroup = new Group(shell, SWT.NONE);
        endpointGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        endpointGroup.setLayout(new GridLayout(1, false));
        new Label(endpointGroup, SWT.NONE).setText("Portal service endpoint :");
        this.endpointField = new Text(endpointGroup, SWT.BORDER);
        this.endpointField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        String endpointData = Activator.getDefault().getPreferenceStore().getString(PORTAL_ENDPOINT_PROP);
        if (endpointData == null || endpointData.trim().length() == 0) {
            endpointData = "http://localhost:8085/portaladmin/services/GoalTreeRepository";
        }
        this.endpointField.setText(endpointData);
        Composite buttons = new Composite(shell, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        FillLayout layout = new FillLayout(SWT.HORIZONTAL);
        layout.spacing = 3;
        buttons.setLayout(layout);
        Button uploadButton = new Button(buttons, SWT.PUSH);
        uploadButton.setText("Upload");
        uploadButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                try {
                    doUploadService();
                } catch (Throwable ime) {
                    if (ime instanceof AxisFault && ime.getCause() != null) {
                        ime = ime.getCause();
                    }
                    MessageDialog.openError(shell, "Remote Server Error", ime.getClass().getSimpleName() + " - " + ime.getMessage());
                    LogManager.logError(ime);
                    return;
                }
            }
        });
        Button closeButton = new Button(buttons, SWT.PUSH);
        closeButton.setText("Close");
        closeButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                shell.dispose();
            }
        });
        Point pLocation = parentShell.getLocation();
        Point pSize = parentShell.getSize();
        shell.setLocation(pLocation.x + pSize.x / 2 - shell.getSize().x / 2, pLocation.y + pSize.y / 2 - shell.getSize().y / 2);
    }

    public void open() {
        shell.open();
        Display display = shell.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void doUploadService() throws Exception {
        String endpoint = this.endpointField.getText().trim();
        if (endpoint.length() == 0) {
            throw new Exception("No portal service endpoint specified!");
        }
        Activator.getDefault().getPreferenceStore().setValue(PORTAL_ENDPOINT_PROP, endpoint);
        GoalTreeRepositoryStub stub = new GoalTreeRepositoryStub(endpoint);
        ListRegisteredTreeIDsResponse response = stub.listRegisteredTreeIDs();
        boolean hasToReplace = false;
        if (response != null && response.get_return() != null) {
            for (String id : response.get_return()) {
                if (id.equals(this.ontoID)) {
                    if (MessageDialog.openConfirm(this.shell, "Goal Tree Overwrite?", "Goal Tree with id '" + id + "' already exists in the Portal repository." + "\n\nPlease confirm overwriting!")) {
                        hasToReplace = true;
                        break;
                    } else {
                        return;
                    }
                }
            }
        }
        if (hasToReplace) {
            UnregisterGoalTree arg = new UnregisterGoalTree();
            arg.setParam0(this.ontoID);
            stub.unregisterGoalTree(arg);
        }
        RegisterGoalTree args = new RegisterGoalTree();
        args.setParam0(this.wsmlData);
        args.setParam1(this.xmlData);
        stub.registerGoalTree(args);
        MessageDialog.openInformation(this.shell, "Result", "Goal Tree successfully uploaded!");
        shell.dispose();
    }
}
