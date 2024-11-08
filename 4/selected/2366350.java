package cvosteen.sqltool.gui;

import cvosteen.sqltool.database.*;
import cvosteen.sqltool.memory.*;
import cvosteen.sqltool.task.*;
import cvosteen.sqltool.tasks.*;
import cvosteen.sqltool.gui.components.*;
import cvosteen.sqltool.gui.syntax.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

public class ConcreteDatabasePanel extends JSplitPane implements DatabasePanel {

    private DatabasePanelParent parent;

    private Database database;

    private Connection connection;

    private JTree tree;

    private JComboBox queryCombo;

    private JTextPane sqlField;

    protected JTable table;

    protected String executedQueryName = "";

    private JPopupMenu popup;

    protected JButton runButton;

    protected JButton saveButton;

    protected JLabel queryStatusLabel;

    private Task queryTask;

    private ActionListener runQueryActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            runQuery();
        }
    };

    private ActionListener stopQueryActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            stopQuery();
            runButton.setEnabled(false);
        }
    };

    private LowMemoryListener lowMemoryListener = new LowMemoryListener() {

        public void memoryLow() {
            if (queryTask != null) {
                stopQuery();
                JOptionPane.showMessageDialog(null, "Not enough memory.  Query aborted.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    };

    /**
	 * Creates the instance with the specified database and parent.
	 */
    public ConcreteDatabasePanel(DatabasePanelParent parent, Database database) throws SQLException, ClassNotFoundException {
        super(HORIZONTAL_SPLIT);
        LowMemoryMonitor monitor = LowMemoryMonitor.getInstance();
        monitor.addListener(lowMemoryListener);
        this.parent = parent;
        if (database == null) throw new NullPointerException("Cannot open database, none specified!");
        this.database = database;
        connection = database.connect();
        createComponents();
    }

    private void createComponents() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(database.getName());
        dbNode.add(new DefaultMutableTreeNode("Placeholder"));
        rootNode.add(dbNode);
        tree = new JTree(rootNode);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {

            public void treeWillCollapse(TreeExpansionEvent event) {
            }

            public void treeWillExpand(TreeExpansionEvent event) {
                if (event.getPath().getPathCount() == 2) expandDatabaseTree((DefaultMutableTreeNode) event.getPath().getLastPathComponent()); else if (event.getPath().getPathCount() == 3) expandTableTree((DefaultMutableTreeNode) event.getPath().getLastPathComponent());
            }
        });
        JScrollPane treeScroll = new JScrollPane(tree);
        setLeftComponent(treeScroll);
        setDividerLocation(0.25);
        setResizeWeight(0.1);
        JPanel topPanel = new JPanel();
        JPanel bottomPanel = new JPanel();
        JSplitPane panel = new JSplitPane(VERTICAL_SPLIT);
        panel.setTopComponent(topPanel);
        panel.setBottomComponent(bottomPanel);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 3, 3, 3);
        c.weighty = 0.0;
        c.gridheight = 1;
        JLabel label = new JLabel("Query:");
        gridbag.setConstraints(label, c);
        topPanel.add(label);
        c.weightx = 0.5;
        queryCombo = new JComboBox(database.getAllQueries().toArray());
        queryCombo.setSelectedIndex(-1);
        queryCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sqlField.setText(database.getQuerySql((String) queryCombo.getSelectedItem()));
                sqlField.setCaretPosition(0);
                saveButton.setEnabled(false);
            }
        });
        gridbag.setConstraints(queryCombo, c);
        topPanel.add(queryCombo);
        c.weightx = 0.0;
        JButton newButton = new JButton(new ImageIcon(this.getClass().getResource("icons/new_icon.png")));
        newButton.setToolTipText("Create a new query.");
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newQuery("");
            }
        });
        gridbag.setConstraints(newButton, c);
        topPanel.add(newButton);
        JButton renameButton = new JButton(new ImageIcon(this.getClass().getResource("icons/edit_icon.png")));
        renameButton.setToolTipText("Rename the selected query.");
        renameButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                renameQuery();
            }
        });
        gridbag.setConstraints(renameButton, c);
        topPanel.add(renameButton);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        JButton deleteButton = new JButton(new ImageIcon(this.getClass().getResource("icons/delete_icon.png")));
        deleteButton.setToolTipText("Delete the selected query.");
        deleteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteQuery();
            }
        });
        gridbag.setConstraints(deleteButton, c);
        topPanel.add(deleteButton);
        c.gridx = 6;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        runButton = new JButton("Run Query", new ImageIcon(this.getClass().getResource("icons/run_icon.png")));
        runButton.setToolTipText("Run the current query.");
        runButton.addActionListener(runQueryActionListener);
        gridbag.setConstraints(runButton, c);
        topPanel.add(runButton);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        label = new JLabel("SQL:");
        gridbag.setConstraints(label, c);
        topPanel.add(label);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = GridBagConstraints.RELATIVE;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 5;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        SyntaxHighlightedDocument doc = new SyntaxHighlightedDocument();
        doc.setFontFamily("Courier");
        doc.setFontSize(13);
        doc.setSyntax(new SqlSyntax());
        doc.setColorScheme(new StandardColorScheme());
        sqlField = new JTextPane(doc);
        sqlField.setPreferredSize(new Dimension(100, 100));
        sqlField.setFont(new Font("Courier", Font.PLAIN, 13));
        sqlField.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }

            public void insertUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }

            public void removeUpdate(DocumentEvent e) {
                saveButton.setEnabled(true);
            }
        });
        JScrollPane sqlScroll = new JScrollPane(sqlField);
        sqlScroll.setMinimumSize(new Dimension(sqlScroll.getPreferredSize()));
        gridbag.setConstraints(sqlScroll, c);
        topPanel.add(sqlScroll);
        c.weightx = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        saveButton = new JButton("Save Query", new ImageIcon(this.getClass().getResource("icons/save_icon.png")));
        saveButton.setToolTipText("Save changes to the current query.");
        saveButton.setEnabled(false);
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveQuery();
            }
        });
        gridbag.setConstraints(saveButton, c);
        topPanel.add(saveButton);
        c.gridx = 6;
        c.gridy = 2;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.SOUTHWEST;
        queryStatusLabel = new JLabel("Ready");
        gridbag.setConstraints(queryStatusLabel, c);
        topPanel.add(queryStatusLabel);
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        bottomPanel.setLayout(gridbag);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        popup = new JPopupMenu();
        JMenuItem printMenuItem = new JMenuItem("Print...");
        printMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                printTable();
            }
        });
        popup.add(printMenuItem);
        table.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY());
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK), "doPrint");
        table.getActionMap().put("doPrint", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                printTable();
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        gridbag.setConstraints(tableScroll, c);
        bottomPanel.add(tableScroll);
        setRightComponent(panel);
    }

    /**
	 * Called when the "New Query" button is pressed, or when the
	 * "Save Query" button is pressed for a brand new query.
	 * This method will prompt the user to name the query and
	 * then save it to the database object.
	 */
    private void newQuery(String sql) {
        boolean okay = true;
        String newQuery = JOptionPane.showInputDialog(this, "Enter Query Name:", "New Query", JOptionPane.PLAIN_MESSAGE);
        if (database.getQuerySql(newQuery) != null) okay = JOptionPane.showConfirmDialog(this, newQuery + " already exists.  Overwrite it?", "New Query", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        if (newQuery != null && okay) {
            database.saveQuery(newQuery, sql);
            queryCombo.setModel(new DefaultComboBoxModel(database.getAllQueries().toArray()));
            sqlField.setText(sql);
            queryCombo.setSelectedItem(newQuery);
            if (parent != null) parent.saveRequested(this);
            saveButton.setEnabled(false);
        }
    }

    /**
	 * Called when the "Rename Query" button is pressed.
	 * This method will prompt the user to enter a new name for the specified query.
	 */
    private void renameQuery() {
        boolean okay = true;
        String oldQuery = (String) queryCombo.getSelectedItem();
        if (oldQuery == null) return;
        String newQuery = (String) JOptionPane.showInputDialog(this, "Enter Query Name:", "Rename Query", JOptionPane.PLAIN_MESSAGE, null, null, oldQuery);
        if (database.getQuerySql(newQuery) != null) okay = JOptionPane.showConfirmDialog(this, newQuery + " already exists.  Overwrite it?", "Rename Query", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        if (newQuery != null && okay) {
            database.saveQuery(newQuery, database.getQuerySql(oldQuery));
            database.deleteQuery(oldQuery);
            String currentSql = sqlField.getText();
            queryCombo.setModel(new DefaultComboBoxModel(database.getAllQueries().toArray()));
            queryCombo.setSelectedItem(newQuery);
            if (!currentSql.equals(sqlField.getText())) sqlField.setText(currentSql);
            if (parent != null) parent.saveRequested(this);
        }
    }

    /**
	 * Called when the "Delete Query" button is pressed.
	 * The user will be asked if they are sure first, then the query will be removed
	 * from the database object.
	 */
    private void deleteQuery() {
        String delQuery = (String) queryCombo.getSelectedItem();
        if (delQuery != null && JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + delQuery + "?", "Delete Query", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            database.deleteQuery(delQuery);
            if (parent != null) parent.saveRequested(this);
            queryCombo.setModel(new DefaultComboBoxModel(database.getAllQueries().toArray()));
            sqlField.setText(database.getQuerySql((String) queryCombo.getSelectedItem()));
            saveButton.setEnabled(false);
        }
    }

    /**
	 * Called when the "Run Query" button is pressed.
	 * This will start the QueryTask for the current query and
	 * reset the results table.
	 */
    private void runQuery() {
        makeStopButton();
        queryStatusLabel.setText("Working...");
        executedQueryName = (String) queryCombo.getSelectedItem();
        if (executedQueryName == null) executedQueryName = "";
        table.setModel(new NonEditableTableModel());
        try {
            queryTask = new QueryTask(connection, sqlField.getText());
            queryTask.addTaskListener(new QueryTaskListener());
            queryTask.start();
        } catch (SQLException e) {
            queryStatusLabel.setText("Error");
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            makeRunButton();
        }
    }

    /**
	 * Change the "Stop Button" into a "Run Button".
	 * Presumably after a query completes or the Stop Button is pressed.
	 */
    public void makeRunButton() {
        runButton.setIcon(new ImageIcon(this.getClass().getResource("icons/run_icon.png")));
        runButton.setText("Run Query");
        runButton.setToolTipText("Run the current query.");
        runButton.removeActionListener(stopQueryActionListener);
        runButton.addActionListener(runQueryActionListener);
    }

    /**
	 * Change the "Run Button" into a "Stop Button".
	 * Presumably after the Run Button is pressed.
	 */
    public void makeStopButton() {
        runButton.setIcon(new ImageIcon(this.getClass().getResource("icons/stop_icon.png")));
        runButton.setText("Stop Query");
        runButton.setToolTipText("Cancel the current query.");
        runButton.removeActionListener(runQueryActionListener);
        runButton.addActionListener(stopQueryActionListener);
    }

    /**
	 * Called when either the stop button is pressed or memory is low.
	 * This will request that the running query task cancel.
	 */
    public void stopQuery() {
        if (queryTask != null) {
            queryTask.cancel();
        }
    }

    /**
	 * Called when either Ctrl+P or "Print..." from the context menu are executed.
	 * This will inform the parent that a print has been requested by the user.
	 */
    private void printTable() {
        if (parent != null) parent.printRequested(this);
    }

    /**
	 * Manually adjust the columns in the JTable.
	 * Since the JTable is set NOT to auto-adjust. This method should be called
	 * at the end of a query to make all of the columns fit nicely.  The size
	 * of the strings in each column will be used to determine the correct
	 * width of each column.
	 */
    protected void adjustTableColumns(JTable theTable) {
        TableModel model = theTable.getModel();
        TableCellRenderer headerRenderer = theTable.getTableHeader().getDefaultRenderer();
        for (int col = 0; col < theTable.getColumnModel().getColumnCount(); col++) {
            TableColumn column = theTable.getColumnModel().getColumn(col);
            Component comp = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(), false, false, 0, 0);
            int headerWidth = comp.getPreferredSize().width;
            int sampleSize = model.getRowCount();
            String longString = "";
            for (int row = 0; row < sampleSize; row++) {
                Object sampleObject = model.getValueAt(row, col);
                String sampleString = null;
                if (sampleObject != null) sampleString = sampleObject.toString();
                if (sampleString != null && sampleString.length() > longString.length()) longString = sampleString;
            }
            comp = theTable.getDefaultRenderer(String.class).getTableCellRendererComponent(theTable, longString, false, false, 0, col);
            int cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headerWidth, cellWidth) + 10);
        }
    }

    /**
	 * Called when the "Save Query" button is pressed.
	 * This will save the changes to an existing query, and prompt
	 * for a query name (via newQuery())if it is a new query.
	 */
    private void saveQuery() {
        String saveQuery = (String) queryCombo.getSelectedItem();
        if (saveQuery == null) {
            newQuery(sqlField.getText());
        } else {
            database.saveQuery(saveQuery, sqlField.getText());
            if (parent != null) parent.saveRequested(this);
            saveButton.setEnabled(false);
        }
    }

    /**
	 * Called by the JTreeView listener when the user expands the "Database"
	 * section.  This method will lookup a list of tables from the database
	 * and use that to populate the JTreeView.
	 */
    private void expandDatabaseTree(DefaultMutableTreeNode dbNode) {
        try {
            dbNode.removeAllChildren();
            java.util.List<String> tables = database.getTables(connection);
            for (String table : tables) {
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(table);
                tableNode.add(new DefaultMutableTreeNode("Placeholder"));
                dbNode.add(tableNode);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Called by the JTreeView listener when the user expands the "Table"
	 * section.  This method will lookup a list of columns from the specified
	 * table from the database and use that to populate the JTreeView.
	 */
    private void expandTableTree(DefaultMutableTreeNode tableNode) {
        try {
            tableNode.removeAllChildren();
            java.util.List<String> columns = database.getColumns(connection, (String) tableNode.getUserObject());
            for (String column : columns) {
                DefaultMutableTreeNode columnNode = new DefaultMutableTreeNode(column);
                tableNode.add(columnNode);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Implementing the DatabasePanel interface.
	 * Provides the JTable wrapped in a JTablePrintable
	 */
    public Printable getPrintableComponent() {
        JTablePrintable jtp = new JTablePrintable(table);
        String dbName = database.getName();
        String queryName = executedQueryName;
        String title;
        if (queryName == null || queryName.length() == 0) {
            title = dbName + " - (Ad-hoc SQL)";
        } else {
            title = dbName + " - " + queryName;
        }
        if (title != null) jtp.setTitle(title);
        return jtp;
    }

    /**
	 * Implementing the DatabasePanel interface.
	 * Perform shutdown and release any potential global references.
	 */
    public void shutdown() {
        try {
            table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
            table.getActionMap().remove("doPrint");
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            table.setModel(new DefaultTableModel());
            CloseConnectionTask cct = new CloseConnectionTask(connection);
            cct.start();
            LowMemoryMonitor monitor = LowMemoryMonitor.getInstance();
            monitor.removeListener(lowMemoryListener);
        } catch (Exception f) {
        }
    }

    /**
	 * Implementing the DatabasePanel interface.
	 * Tell the database to commit, display errors, if any, to the user.
	 */
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException f) {
            JOptionPane.showMessageDialog(this, f.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (UnsupportedOperationException f) {
            JOptionPane.showMessageDialog(this, f.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Implementing the DatabasePanel interface.
	 * Tell the database to rollback, display errors, if any, to the user.
	 */
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException f) {
            JOptionPane.showMessageDialog(this, f.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (UnsupportedOperationException f) {
            JOptionPane.showMessageDialog(this, f.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * A task listener designed to listen to a running QueryTask.
	 * It will correctly update the UI depending on the status
	 * of the QueryTask.
	 */
    private class QueryTaskListener implements TaskListener {

        private int rowsReceived = 0;

        /**
		 * When the task has finished, the table will be adjusted
		 * and the "Stop Button" will return to a "Run Button"
		 */
        public void taskFinished() {
            queryTask = null;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        adjustTableColumns(table);
                        makeRunButton();
                        runButton.setEnabled(true);
                    }
                });
            } catch (Exception f) {
            }
        }

        /**
		 * If the task reports status, results are being returned.
		 * The table should be updated to reflect the data.
		 */
        public void taskStatus(final Object obj) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        if (obj instanceof Vector && ((Vector) obj).size() > 0) {
                            Vector vector = (Vector) obj;
                            if (vector.get(0) instanceof String) {
                                for (Object column : vector) {
                                    model.addColumn(column);
                                }
                                queryStatusLabel.setText("0 records");
                            } else {
                                for (Object row : vector) {
                                    if (row instanceof Vector) {
                                        rowsReceived += 1;
                                        model.addRow((Vector) row);
                                    }
                                }
                                if (rowsReceived == 1) {
                                    queryStatusLabel.setText("1 record");
                                } else {
                                    queryStatusLabel.setText("" + rowsReceived + " records");
                                }
                            }
                        }
                    }
                });
            } catch (Exception f) {
            }
        }

        /**
		 * If the task has returned a result, that means an
		 * update was performed, and the user will be alerted.
		 */
        public void taskResult(final Object obj) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        queryStatusLabel.setText("Ready");
                        JOptionPane.showMessageDialog(null, "" + obj + " records updated.", "Update Executed", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception f) {
            }
        }

        /**
		 * If the task reports an error, the error should be reported
		 * to the user.
		 */
        public void taskError(final Exception e) {
            try {
                if (queryTask != null && !queryTask.isCancelled()) {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            queryStatusLabel.setText("Error");
                            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            } catch (Exception f) {
            }
        }
    }
}
