package com.application.areca.launcher.gui;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import com.application.areca.AbstractTarget;
import com.application.areca.CheckParameters;
import com.application.areca.ResourceManager;
import com.application.areca.impl.AbstractIncrementalFileSystemMedium;
import com.application.areca.launcher.gui.common.AbstractWindow;
import com.application.areca.launcher.gui.common.ArecaPreferences;
import com.application.areca.launcher.gui.common.SavePanel;
import com.application.areca.launcher.gui.common.SecuredRunner;
import com.myJava.util.log.Logger;

public class CheckWindow extends AbstractWindow {

    private static final ResourceManager RM = ResourceManager.instance();

    private AbstractTarget target;

    private Text txtLocation;

    private Button btnBrowse;

    private Button chkCheckSelectedEntries;

    private Button radUseDefaultLocation;

    private Button radUseSpecificLocation;

    private Button btnSave;

    private Label result;

    private Table table;

    private TableViewer viewer;

    private Application.ProcessRunner runner;

    public CheckWindow(AbstractTarget target) {
        super();
        this.target = target;
    }

    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 10;
        composite.setLayout(layout);
        final Group grpLocation = new Group(composite, SWT.NONE);
        grpLocation.setText(RM.getLabel("check.location.label"));
        grpLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout grpLayout = new GridLayout(3, false);
        grpLayout.verticalSpacing = 0;
        grpLocation.setLayout(grpLayout);
        radUseDefaultLocation = new Button(grpLocation, SWT.RADIO);
        radUseDefaultLocation.setText(RM.getLabel("check.default.label"));
        radUseDefaultLocation.setToolTipText(RM.getLabel("check.default.tt"));
        radUseDefaultLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
        monitorControl(SWT.Selection, radUseDefaultLocation);
        radUseSpecificLocation = new Button(grpLocation, SWT.RADIO);
        radUseSpecificLocation.setText(RM.getLabel("check.specific.label"));
        radUseSpecificLocation.setToolTipText(RM.getLabel("check.specific.tt"));
        radUseSpecificLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        monitorControl(SWT.Selection, radUseSpecificLocation);
        txtLocation = new Text(grpLocation, SWT.BORDER);
        GridData mainData2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
        mainData2.widthHint = computeWidth(200);
        txtLocation.setLayoutData(mainData2);
        monitorControl(txtLocation);
        btnBrowse = new Button(grpLocation, SWT.PUSH);
        btnBrowse.setText(RM.getLabel("common.browseaction.label"));
        btnBrowse.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                String path = Application.getInstance().showDirectoryDialog(txtLocation.getText(), CheckWindow.this);
                if (path != null) {
                    txtLocation.setText(path);
                }
            }
        });
        btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        chkCheckSelectedEntries = new Button(composite, SWT.CHECK);
        chkCheckSelectedEntries.setText(RM.getLabel("check.checkselected.label"));
        chkCheckSelectedEntries.setToolTipText(RM.getLabel("check.checkselected.tt"));
        chkCheckSelectedEntries.setLayoutData(new GridData());
        monitorControl(SWT.Selection, chkCheckSelectedEntries);
        radUseDefaultLocation.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                switchLocation();
            }
        });
        radUseSpecificLocation.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                switchLocation();
            }
        });
        SavePanel pnlSave = new SavePanel(RM.getLabel("check.check.label"), this);
        pnlSave.setShowCancel(false);
        pnlSave.buildComposite(composite).setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        btnSave = pnlSave.getBtnSave();
        viewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE);
        table = viewer.getTable();
        table.setLinesVisible(AbstractWindow.getTableLinesVisible());
        table.setHeaderVisible(true);
        TableColumn col1 = new org.eclipse.swt.widgets.TableColumn(table, SWT.NONE);
        col1.setMoveable(true);
        TableColumn col2 = new org.eclipse.swt.widgets.TableColumn(table, SWT.NONE);
        col2.setMoveable(true);
        table.getColumn(0).setWidth(AbstractWindow.computeWidth(400));
        table.getColumn(1).setWidth(AbstractWindow.computeWidth(100));
        GridData dtTable = new GridData(SWT.FILL, SWT.FILL, true, true);
        dtTable.heightHint = computeHeight(200);
        table.setLayoutData(dtTable);
        result = new Label(composite, SWT.NONE);
        result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        txtLocation.setText(ArecaPreferences.getCheckSpecificLocation(application.getCurrentWorkspaceItem().getUid()));
        if (ArecaPreferences.getCheckUseSpecificLocation(application.getCurrentWorkspaceItem().getUid())) {
            radUseSpecificLocation.setSelection(true);
        } else {
            radUseDefaultLocation.setSelection(true);
        }
        AbstractIncrementalFileSystemMedium medium = (AbstractIncrementalFileSystemMedium) target.getMedium();
        chkCheckSelectedEntries.setEnabled(medium.getHandler().autonomousArchives());
        chkCheckSelectedEntries.setSelection(medium.getHandler().autonomousArchives());
        switchLocation();
        composite.pack();
        return composite;
    }

    public void closeInError(Exception e) {
        SecuredRunner.execute(new Runnable() {

            public void run() {
                application.disableWaitCursor(CheckWindow.this);
                CheckWindow.this.close();
            }
        });
    }

    public void setResult(final List errorFiles, final List uncheckedFiles, final List unrecoveredFiles, final long nbChecked) {
        final String invalidMsg = RM.getLabel("check.invalid.label");
        final String uncheckedMsg = RM.getLabel("check.unchecked.label");
        final String unrecoveredMsg = RM.getLabel("check.unrecovered.label");
        SecuredRunner.execute(new Runnable() {

            public void run() {
                try {
                    application.disableWaitCursor(CheckWindow.this);
                    Iterator iter = unrecoveredFiles.iterator();
                    while (iter.hasNext()) {
                        TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(0, (String) iter.next());
                        item.setText(1, unrecoveredMsg);
                    }
                    iter = errorFiles.iterator();
                    while (iter.hasNext()) {
                        TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(0, (String) iter.next());
                        item.setText(1, invalidMsg);
                    }
                    iter = uncheckedFiles.iterator();
                    while (iter.hasNext()) {
                        TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(0, (String) iter.next());
                        item.setText(1, uncheckedMsg);
                    }
                    if (!errorFiles.isEmpty() || !unrecoveredFiles.isEmpty()) {
                        result.setText(RM.getLabel("check.invalid.message"));
                    } else if (!uncheckedFiles.isEmpty()) {
                        result.setText(RM.getLabel("check.unchecked.message"));
                    } else {
                        result.setText(RM.getLabel("check.ok.message") + " (" + RM.getLabel("check.checked.label", new Object[] { "" + nbChecked }) + ")");
                    }
                } catch (SWTException e) {
                    Logger.defaultLogger().error(e);
                }
            }
        });
    }

    private void switchLocation() {
        boolean defaultLocation = radUseDefaultLocation.getSelection();
        txtLocation.setEnabled(!defaultLocation);
        btnBrowse.setEnabled(!defaultLocation);
        checkBusinessRules();
    }

    public String getTitle() {
        return RM.getLabel("check.dialog.title") + " (" + target.getName() + ")";
    }

    protected boolean checkBusinessRules() {
        this.resetErrorState(txtLocation);
        if (radUseSpecificLocation.getSelection()) {
            if (this.txtLocation.getText() == null || this.txtLocation.getText().length() == 0) {
                this.setInError(txtLocation, RM.getLabel("error.field.mandatory"));
                return false;
            }
        }
        return true;
    }

    protected void saveChanges() {
        ArecaPreferences.setCheckUseSpecificLocation(radUseSpecificLocation.getSelection(), application.getCurrentWorkspaceItem().getUid());
        ArecaPreferences.setCheckSpecificLocation(txtLocation.getText(), application.getCurrentWorkspaceItem().getUid());
        this.viewer.setItemCount(0);
        this.result.setText("");
        this.hasBeenUpdated = false;
        CheckParameters checkParams = new CheckParameters(true, this.chkCheckSelectedEntries.getSelection(), true, this.radUseSpecificLocation.getSelection(), this.txtLocation.getText());
        this.runner = application.launchArchiveCheck(checkParams, target, this);
        if (runner != null) {
            application.enableWaitCursor(this);
        }
    }

    protected void updateState(boolean rulesSatisfied) {
        this.btnSave.setEnabled(rulesSatisfied);
    }

    public boolean close() {
        boolean closed = super.close();
        if (closed && runner != null && runner.getChannel() != null && runner.getChannel().isRunning() && runner.getChannel().getTaskMonitor() != null) {
            runner.getChannel().getTaskMonitor().setCancelRequested();
        }
        return closed;
    }
}
