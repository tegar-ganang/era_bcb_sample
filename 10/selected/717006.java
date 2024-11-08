package com.sheelapps.dbexplorer;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import com.sheelapps.ui.common.BaseDialog;

/**
 * 
 * @author Sheel
 *
 */
public class ImportCSVDialog extends BaseDialog {

    public ImportCSVDialog(Shell parent) {
        super(parent);
    }

    private int currentStep = 1;

    private Text csvFile;

    private Button csvFileBrowser;

    private List DBTable;

    private Text csvDelim;

    private Button csvHeader;

    private Table mappingTable;

    private Composite stacked;

    private Composite page1;

    private Composite page2;

    private StackLayout stackLayout;

    private ScrolledComposite co;

    private Group sc;

    private Vector colNames;

    private Vector colTypes;

    private Vector colTypeInt;

    private Vector csvData;

    private String title = "Import CSV";

    private Button back;

    private Vector colNullAllowed;

    private ProgressBar pBar_status;

    protected boolean isRunning;

    protected CSVImporter importer = null;

    private Label statusLabel;

    private int size;

    protected void setDialogSize() {
        Rectangle displayRect = parent.getBounds();
        dialog.setSize((int) (displayRect.width * 0.5), (int) (displayRect.height * 0.5));
    }

    protected void addComponents() {
        Listener listener = new Listener() {

            public void handleEvent(Event event) {
                if (event.widget == csvFileBrowser) {
                    FileDialog d = new FileDialog(dialog, SWT.OPEN);
                    d.setText("DB Explorer - Import From CSV");
                    d.setFilterExtensions(new String[] { "*.csv", "*.txt", "*.*" });
                    String file = d.open();
                    if (file != null) csvFile.setText(file);
                }
            }
        };
        dialog.setText("Import CSV File");
        stacked = new Composite(getMainComposite(), SWT.NONE);
        stacked.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        stackLayout = new StackLayout();
        stacked.setLayout(stackLayout);
        page1 = new Composite(stacked, SWT.NONE);
        page1.setLayout(new GridLayout(1, true));
        page2 = new Composite(stacked, SWT.NONE);
        page2.setLayout(new GridLayout(1, true));
        Group grp = addGroup("Select CSV File", 2, page1);
        csvFile = new Text(grp, SWT.BORDER);
        csvFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        csvFileBrowser = new Button(grp, SWT.PUSH);
        csvFileBrowser.setText("Browse");
        csvFileBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        csvFileBrowser.addListener(SWT.Selection, listener);
        grp = addGroup("CSV Delimeter and Header Row", 3, page1);
        FormLayout layout = new FormLayout();
        grp.setLayout(layout);
        FormData data1 = new FormData();
        data1.left = new FormAttachment(0, 0);
        data1.top = new FormAttachment(0, 0);
        Label lbl1 = new Label(grp, SWT.None);
        lbl1.setText("Delimeter");
        lbl1.setLayoutData(data1);
        Label lb2 = new Label(grp, SWT.None);
        data1 = new FormData();
        data1.left = new FormAttachment(0, 0);
        data1.top = new FormAttachment(lbl1, 10);
        lb2.setText("Header Row");
        lb2.setLayoutData(data1);
        csvDelim = new Text(grp, SWT.BORDER);
        data1 = new FormData();
        data1.left = new FormAttachment(lb2, 15);
        data1.top = new FormAttachment(0, 0);
        csvDelim.setLayoutData(data1);
        csvDelim.setText(", ");
        csvDelim.setTextLimit(2);
        csvHeader = new Button(grp, SWT.CHECK);
        data1 = new FormData();
        data1.left = new FormAttachment(lb2, 15);
        data1.top = new FormAttachment(lbl1, 10);
        csvHeader.setAlignment(SWT.RIGHT);
        csvHeader.setLayoutData(data1);
        csvHeader.setSelection(true);
        ScrolledComposite sco = new ScrolledComposite(page1, SWT.V_SCROLL | SWT.H_SCROLL);
        sco.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        grp = new Group(sco, SWT.NONE);
        grp.setText("Select Table");
        grp.setLayout(new FillLayout());
        sco.setContent(grp);
        sco.setExpandHorizontal(true);
        sco.setExpandVertical(true);
        DBTable = new List(grp, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY | SWT.SIMPLE);
        if (DBExplorer.allTables.size() == 0) DBExplorer.loadSchemaTables();
        if (DBExplorer.allTables.size() == 0) {
            dialog.close();
            return;
        }
        for (int i = 0; i < DBExplorer.allTables.size(); i++) {
            DBTable.add(DBExplorer.allTables.get(i).toString());
        }
        DBTable.select(0);
        co = new ScrolledComposite(page2, SWT.V_SCROLL | SWT.H_SCROLL);
        co.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sc = new Group(co, SWT.NONE);
        sc.setText("CSV/Table mapping");
        sc.setLayout(new FillLayout(SWT.VERTICAL));
        co.setContent(sc);
        co.setExpandHorizontal(true);
        co.setExpandVertical(true);
        sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        mappingTable = new Table(sc, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        mappingTable.setLinesVisible(true);
        mappingTable.setHeaderVisible(true);
        statusLabel = new Label(page2, SWT.None);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        pBar_status = new ProgressBar(page2, SWT.SMOOTH);
        pBar_status.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        sc.layout();
        stackLayout.topControl = page1;
    }

    protected boolean DoColumnMapping(String tableName) {
        sc.setText("CSV/table mapping - " + tableName);
        while (mappingTable.getColumnCount() > 0) {
            int lastOne = mappingTable.getColumnCount() - 1;
            mappingTable.getColumn(lastOne).dispose();
        }
        mappingTable.removeAll();
        getDBColumns(tableName);
        String[] header = (String[]) csvData.get(0);
        if (header == null || colNames.size() != header.length) {
            showError(title, "CSV Columns/Table Columns does not match.");
            return false;
        }
        String[] firstRow = (String[]) csvData.get(1);
        TableColumn column = new TableColumn(mappingTable, SWT.NONE);
        column.setText("CSV Column");
        column = new TableColumn(mappingTable, SWT.NONE);
        column.setText("Table Column");
        column = new TableColumn(mappingTable, SWT.NONE);
        column.setText("Column Type");
        column = new TableColumn(mappingTable, SWT.NONE);
        column.setText("First Row Data");
        for (int i = 0; i < colNames.size(); i++) {
            TableItem item = new TableItem(mappingTable, SWT.NONE);
            item.setText(0, header[i]);
            item.setText(1, colNames.get(i).toString());
            item.setText(2, colTypes.get(i).toString());
            item.setText(3, firstRow[i]);
        }
        for (int i = 0; i < 4; i++) {
            mappingTable.getColumn(i).pack();
        }
        return true;
    }

    private Vector getDBColumns(String tableName) {
        colNames = new Vector();
        colTypes = new Vector();
        colTypeInt = new Vector();
        colNullAllowed = new Vector();
        Connection conn = null;
        try {
            conn = DBExplorer.getConnection(false);
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, tableName, null);
            while (rs.next()) {
                colNames.add(rs.getString("COLUMN_NAME"));
                colTypeInt.add(rs.getInt("DATA_TYPE") + "");
                colTypes.add(rs.getString("TYPE_NAME"));
                colNullAllowed.add(rs.getString("IS_NULLABLE").equals("YES") ? "true" : "false");
            }
            rs.close();
            conn.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return colNames;
    }

    protected void handleOkAction() {
        if (currentStep == 1) {
            loadCSVData();
            size = csvHeader.getSelection() ? csvData.size() - 1 : csvData.size();
            statusLabel.setText("Total rows " + size);
            if (csvData.size() > 0 && DBTable.getSelection() != null && DBTable.getSelection().length > 0) {
                if (DoColumnMapping(DBTable.getSelection()[0])) {
                    back.setVisible(true);
                    currentStep = 2;
                    stackLayout.topControl = page2;
                    co.layout();
                    stacked.layout();
                }
            } else {
                showError(title, "Failed to load CSV File.");
            }
        } else {
            if (!isRunning) {
                pBar_status.setMaximum(size);
                importer = new CSVImporter(this, DBTable.getSelection()[0], csvHeader.getSelection(), size);
                new Thread(importer).start();
            } else {
                showError(title, "CSV Import is running.");
            }
        }
    }

    public void UpdateProgressBar(final int selection_value) {
        if (pBar_status.isDisposed()) return;
        dialog.getDisplay().asyncExec(new Runnable() {

            public void run() {
                pBar_status.setSelection(selection_value);
                if (selection_value > 0) statusLabel.setText("Importing " + selection_value + " of " + size + " rows");
            }
        });
    }

    private class CSVImporter implements Runnable {

        ImportCSVDialog dlg = null;

        String tableName = null;

        final boolean hasHeader;

        boolean cancel = false;

        private int maxsize;

        public CSVImporter(ImportCSVDialog dlg, String tableName, boolean hasHeader, int size) {
            this.dlg = dlg;
            this.tableName = tableName;
            this.hasHeader = hasHeader;
            this.maxsize = size;
        }

        public void run() {
            isRunning = true;
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            doCSVImport(tableName, hasHeader);
            isRunning = false;
        }

        private boolean doCSVImport(String tableName, final boolean hasHeader) {
            StringBuffer sql = new StringBuffer();
            sql.append("INSERT INTO ");
            sql.append(tableName + "(");
            for (int i = 0; i < colNames.size(); i++) {
                sql.append("" + colNames.get(i) + ",");
            }
            sql.setLength(sql.length() - 1);
            sql.append(") VALUES( ");
            for (int i = 0; i < colNames.size(); i++) {
                sql.append("?,");
            }
            sql.setLength(sql.length() - 1);
            sql.append(")");
            Connection conn = null;
            int lineNumber = 0;
            int colNumber = 0;
            String line[] = null;
            try {
                conn = DBExplorer.getConnection(false);
                conn.setAutoCommit(false);
                PreparedStatement pstmt = conn.prepareStatement(sql.toString());
                for (; lineNumber < csvData.size(); lineNumber++) {
                    if (hasHeader && lineNumber == 0) continue;
                    dlg.UpdateProgressBar(lineNumber);
                    if (cancel) {
                        break;
                    }
                    line = (String[]) csvData.get(lineNumber);
                    pstmt.clearParameters();
                    for (colNumber = 0; colNumber < colTypes.size(); colNumber++) {
                        if (line[colNumber].equals("") && colNullAllowed.get(colNumber).toString().equals("true")) {
                            pstmt.setNull(colNumber + 1, Integer.parseInt(colTypeInt.get(colNumber).toString()));
                        } else {
                            pstmt.setObject(colNumber + 1, line[colNumber], Integer.parseInt(colTypeInt.get(colNumber).toString()));
                        }
                    }
                    pstmt.executeUpdate();
                }
                if (cancel) conn.rollback(); else conn.commit();
                conn.setAutoCommit(true);
                conn.close();
                conn = null;
                dialog.getDisplay().asyncExec(new Runnable() {

                    public void run() {
                        if (!cancel) {
                            dlg.showMessage(title, "Imported " + maxsize + " rows successfully.");
                            statusLabel.setText("Import complete.");
                        } else {
                            dlg.UpdateProgressBar(0);
                            statusLabel.setText("Import aborted.");
                        }
                    }
                });
                return true;
            } catch (final Exception e) {
                if (conn != null) try {
                    conn.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                dialog.getDisplay().asyncExec(new Runnable() {

                    public void run() {
                        statusLabel.setText("Import failed");
                        dlg.showError(title, e.getMessage());
                        dlg.UpdateProgressBar(0);
                    }
                });
                return false;
            } finally {
                if (conn != null) try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadCSVData() {
        csvData = new Vector();
        String filename = csvFile.getText();
        if (filename != null && filename.length() > 0) {
            CSVReader csv = null;
            try {
                if (csvDelim.getText().trim().equals("\t")) csv = new CSVReader(new FileReader(filename), '\t'); else csv = new CSVReader(new FileReader(filename), csvDelim.getText().charAt(0));
                csvData.addAll(csv.readAll());
                csv.close();
            } catch (Exception e) {
                csvData.clear();
            }
        }
    }

    protected void addOkCancelButtons() {
        Composite btnComp = new Composite(dialog, SWT.None);
        GridData gd = new GridData(SWT.END, SWT.END, true, false);
        btnComp.setLayoutData(gd);
        FillLayout l = new FillLayout();
        l.spacing = 10;
        btnComp.setLayout(l);
        back = new Button(btnComp, SWT.PUSH);
        back.setText("Back");
        back.setVisible(false);
        final Button ok = new Button(btnComp, SWT.PUSH);
        ok.setText(getOKButtonText());
        final Button cancel = new Button(btnComp, SWT.PUSH);
        cancel.setText("Cancel");
        Listener listener = new Listener() {

            public void handleEvent(Event event) {
                if (event.widget == ok) {
                    okClicked = true;
                    handleOkAction();
                    if (currentStep == 2) {
                        if (!ok.isDisposed()) ok.setText("Finish");
                    }
                } else if (event.widget == cancel) {
                    okClicked = false;
                    handleCancelAction();
                } else {
                    if (!isRunning) {
                        currentStep = 1;
                        stackLayout.topControl = page1;
                        ok.setText("Next");
                        back.setVisible(false);
                        co.layout();
                        stacked.layout();
                    } else {
                        showError(title, "CSV Import is running.");
                    }
                }
            }
        };
        ok.addListener(SWT.Selection, listener);
        cancel.addListener(SWT.Selection, listener);
        back.addListener(SWT.Selection, listener);
        dialog.setDefaultButton(ok);
    }

    protected String getOKButtonText() {
        if (currentStep == 1) return "Next"; else return "Finish";
    }

    protected void handleCancelAction() {
        if (!isRunning) {
            dialog.close();
        } else {
            MessageBox box = new MessageBox(dialog, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            box.setText(title);
            box.setMessage("CSV Import is running. Do you want to cancel it ?");
            if (box.open() == SWT.YES) {
                importer.cancel = true;
            }
        }
    }
}
