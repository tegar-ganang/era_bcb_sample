package gov.sns.apps.pvlogger;

import gov.sns.application.*;
import gov.sns.tools.data.*;
import java.util.Iterator;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.Vector;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
 * PVLoggerWindow
 *
 * @author  tap
 */
class PVLoggerWindow extends XalWindow implements SwingConstants, DataKeys, ScrollPaneConstants {

    /** date formatter for displaying timestamps */
    protected static final DateFormat TIMESTAMP_FORMAT;

    /** Table of loggers running on the local network */
    protected JTable loggerTable;

    /** List of session group types */
    protected JList _groupTypesListView;

    /** Table selection action to restart the selected pvlogger */
    protected Action restartSelectionAction;

    /** Table selection action to shutdown the selected pvlogger */
    protected Action shutdownSelectionAction;

    /** Table selection action to stop the selected loggers from logging */
    protected Action stopLoggingSelectionAction;

    /** Table selection action to start the selected loggers logging */
    protected Action resumeLoggingSelectionAction;

    /** Field for entering and displaying the update period */
    protected JTextField periodField;

    /** Label for displaying the latest log event */
    protected JLabel latestLogDateField;

    /** Text view that displays the latest log output */
    protected JTextArea latestLogTextView;

    /** List view displaying connected PVs */
    protected JList _connectedPVList;

    /** List view displaying unconnected PVs */
    protected JList _unconnectedPVList;

    /** field displaying whether the logger is active */
    protected JLabel _loggingStatusField;

    /** field displaying whether the logger's logging period */
    protected JLabel _loggingPeriodField;

    /** main application wide model */
    protected LoggerModel _mainModel;

    /** model for this window's document */
    protected DocumentModel _model;

    /**
	 * static initializer
	 */
    static {
        TIMESTAMP_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    }

    /** Creates a new instance of MainWindow */
    public PVLoggerWindow(PVLoggerDocument aDocument, LoggerTableModel loggerTableModel) {
        super(aDocument);
        setSize(900, 600);
        _model = aDocument.getModel();
        _mainModel = _model.getMainModel();
        makeContent(loggerTableModel);
        handleLoggerEvents();
        _mainModel.updateServiceList();
    }

    /**
	 * Listen for new logger status events and update the views accordingly
	 */
    protected void handleLoggerEvents() {
        _model.addDocumentModelListener(new DocumentModelListener() {

            /**
			 * Notification that a new logger has been selected
			 * @param source the document model managing selections
			 * @param handler the latest handler selection or null if none is selected
			 */
            public void handlerSelected(DocumentModel source, LoggerHandler handler) {
                updateLoggerInspector();
                updateControls();
            }

            /**
			 * Notification that a new logger session has been selected
			 * @param source the document model managing selections
			 * @param handler the latest session handler selection or null if none is selected
			 */
            public void sessionHandlerSelected(DocumentModel source, LoggerSessionHandler handler) {
                updateChannelsInspector();
                updateLogText();
                updateLoggerInfo();
            }

            /**
			 * Notification that the channels of the selected logger have changed
			 * @param model the document model managing selections
			 * @param channelRefs the latest channel refs containing the channel information
			 */
            public void channelsChanged(DocumentModel model, java.util.List channelRefs) {
                updateChannelsInspector();
            }

            /**
			 * Notification that a new machine snapshot has been published
			 * @param model the document model managing selections
			 * @param timestamp the timestamp of the latest machine snapshot
			 * @param snapshotDump the textual dump of the latest machine snapshot
			 */
            public void snapshotPublished(DocumentModel model, Date timestamp, String snapshotDump) {
                updateLogText();
            }

            /**
			 * Notification that a logger record has been updated
			 * @param model the document model managing selections
			 * @param record the updated logger record
			 */
            public void recordUpdated(DocumentModel model, GenericRecord record) {
            }

            /**
			 * Notification that a logger session has been updated
			 * @param model the document model managing selections
			 * @param source the updated logger session
			 */
            public void loggerSessionUpdated(DocumentModel model, LoggerSessionHandler source) {
                updateLoggerInfo();
            }
        });
    }

    /**
	 * Determine whether to display the toolbar.
	 * @return true to display the toolbar and false otherwise.
	 */
    @Override
    public boolean usesToolbar() {
        return true;
    }

    /**
	 * Update the controls to reconcile it with the model.
	 */
    protected void updateControls() {
        int oldPeriod;
        try {
            oldPeriod = Integer.parseInt(periodField.getText());
        } catch (Exception exception) {
            oldPeriod = 0;
        }
        final int period = _mainModel.getUpdatePeriod();
        if (period != oldPeriod) {
            periodField.setText(String.valueOf(period));
            periodField.selectAll();
        }
        boolean hasSelectedHandler = _model.getSelectedHandler() != null;
        shutdownSelectionAction.setEnabled(hasSelectedHandler);
        restartSelectionAction.setEnabled(hasSelectedHandler);
        stopLoggingSelectionAction.setEnabled(hasSelectedHandler);
        resumeLoggingSelectionAction.setEnabled(hasSelectedHandler);
    }

    /**
	 * Update the logger inspector for the selected logger
	 */
    protected void updateLoggerInspector() {
        updateGroupListView();
        updateLoggerInfo();
        updateChannelsInspector();
        updateLogText();
    }

    /**
	 * Update information about the remote logger including logging period 
	 * and logger state.
	 */
    protected void updateLoggerInfo() {
        LoggerSessionHandler session = _model.getSelectedSessionHandler();
        String status = (session != null) ? String.valueOf(session.isLogging()) : "false";
        _loggingStatusField.setText(status);
        String periodText = (session != null) ? String.valueOf(session.getLoggingPeriod()) : "0";
        _loggingPeriodField.setText(periodText);
    }

    /**
	 * Update the list of connected and disconnected channels for the selected logger handler
	 */
    protected void updateChannelsInspector() {
        LoggerSessionHandler handler = _model.getSelectedSessionHandler();
        if (handler != null) {
            Vector connectedPVs = new Vector();
            Vector unconnectedPVs = new Vector();
            Collection channelRefs = handler.getChannelRefs();
            for (Iterator iter = channelRefs.iterator(); iter.hasNext(); ) {
                ChannelRef channelRef = (ChannelRef) iter.next();
                if (channelRef.isConnected()) {
                    connectedPVs.add(channelRef);
                } else {
                    unconnectedPVs.add(channelRef);
                }
            }
            Collections.sort(connectedPVs);
            Collections.sort(unconnectedPVs);
            _connectedPVList.setListData(connectedPVs);
            _unconnectedPVList.setListData(unconnectedPVs);
        } else {
            _connectedPVList.setListData(new Vector());
            _unconnectedPVList.setListData(new Vector());
        }
    }

    /**
	 * Update the the text of the latest log from the selected logger handler
	 */
    protected void updateLogText() {
        LoggerSessionHandler handler = _model.getSelectedSessionHandler();
        if (handler != null) {
            latestLogTextView.setText(handler.getLastPublishedSnapshotDump());
            latestLogTextView.setCaretPosition(0);
            String dateText = TIMESTAMP_FORMAT.format(handler.getTimestampOfLastPublishedSnapshot());
            latestLogDateField.setText(dateText);
        } else {
            latestLogTextView.setText("");
            latestLogDateField.setText("");
        }
    }

    /**
	 * Update the list of logger sessions identified by group
	 */
    protected void updateGroupListView() {
        LoggerHandler handler = _model.getSelectedHandler();
        Vector groups = (handler != null) ? new Vector(handler.getGroupTypes()) : new Vector();
        _groupTypesListView.setListData(groups);
    }

    /**
	 * Build the component contents of the window.
	 * @param loggerTableModel The table model for the logger view
	 */
    protected void makeContent(final LoggerTableModel loggerTableModel) {
        Box mainView = new Box(VERTICAL);
        getContentPane().add(mainView);
        mainView.add(makePeriodView());
        Box loggerPanel = new Box(HORIZONTAL);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(makeLoggerTable(loggerTableModel));
        splitPane.setBottomComponent(makeLoggerInspector());
        splitPane.setResizeWeight(0.5);
        loggerPanel.add(splitPane);
        mainView.add(loggerPanel);
        updateControls();
        updateLoggerInspector();
    }

    /**
	 * Make a view that displays the period of updates and allows the user to change that period
	 * @return the period view
	 */
    protected JComponent makePeriodView() {
        JPanel periodPanel = new JPanel();
        periodPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        periodPanel.add(new JLabel("update period (sec): "));
        periodField = new JTextField(2);
        periodField.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                applyPeriodSetting();
            }
        });
        periodField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent event) {
                periodField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent event) {
                updateControls();
                periodField.setCaretPosition(0);
                periodField.moveCaretPosition(0);
            }
        });
        periodField.setHorizontalAlignment(RIGHT);
        periodPanel.add(periodField);
        periodPanel.setBorder(new EtchedBorder());
        periodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10 + periodField.getHeight()));
        return periodPanel;
    }

    /**
	 * Make a table that lists the currently running loggers
	 * @param tableModel The table model
	 * @return the table view
	 */
    protected JComponent makeLoggerTable(final LoggerTableModel tableModel) {
        loggerTable = new JTable(tableModel);
        loggerTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableCellRenderer numericCellRenderer = makeNumericCellRenderer();
        TableColumnModel columnModel = loggerTable.getColumnModel();
        JScrollPane loggerScrollPane = new JScrollPane(loggerTable);
        loggerScrollPane.setColumnHeaderView(loggerTable.getTableHeader());
        loggerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        loggerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return loggerScrollPane;
    }

    /**
	 * Make an inspector for a selected logger
	 * @return the inspector view
	 */
    protected JComponent makeLoggerInspector() {
        JTabbedPane tabPane = new JTabbedPane();
        loggerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) return;
                int selectedRow = loggerTable.getSelectedRow();
                if (selectedRow >= 0) {
                    LoggerTableModel tableModel = (LoggerTableModel) loggerTable.getModel();
                    String id = tableModel.getRecord(selectedRow).stringValueForKey(ID_KEY);
                    _model.setSelectedHandler(_mainModel.getHandler(id));
                } else {
                    _model.setSelectedHandler(null);
                }
            }
        });
        tabPane.addTab("Info", makeInfoTab());
        tabPane.addTab("Latest Log", makeLatestLogTab());
        tabPane.addTab("PVs", makePVTab());
        JSplitPane inspector = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, makeSessionListView(), tabPane);
        return inspector;
    }

    /**
	 * Make the session list view which displays the list of logger sessions for the selected
	 * logger handler.
	 * @return the view which displays the logger sessions
	 */
    protected JComponent makeSessionListView() {
        Box view = new Box(BoxLayout.Y_AXIS);
        Box labelRow = new Box(BoxLayout.X_AXIS);
        labelRow.add(Box.createHorizontalGlue());
        labelRow.add(new JLabel("Logger Groups"));
        labelRow.add(Box.createHorizontalGlue());
        view.add(labelRow);
        _groupTypesListView = new JList();
        view.add(new JScrollPane(_groupTypesListView));
        _groupTypesListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _groupTypesListView.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    String selectedGroup = (String) _groupTypesListView.getSelectedValue();
                    LoggerHandler handler = _model.getSelectedHandler();
                    LoggerSessionHandler sessionHandler = (handler != null) ? handler.getLoggerSession(selectedGroup) : null;
                    _model.setSelectedSessionHandler(sessionHandler);
                }
            }
        });
        return view;
    }

    /**
	 * Make a tab that displays the information about the latest log
	 * @return the tab view
	 */
    protected JComponent makeLatestLogTab() {
        Box view = new Box(VERTICAL);
        Box row = new Box(HORIZONTAL);
        row.add(new JLabel("Time: "));
        latestLogDateField = new JLabel();
        row.setMinimumSize(new Dimension(0, 3 * row.getPreferredSize().height));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3 * row.getPreferredSize().height));
        row.add(latestLogDateField);
        row.add(Box.createGlue());
        view.add(row);
        latestLogTextView = new JTextArea();
        latestLogTextView.setEditable(false);
        view.add(new JScrollPane(latestLogTextView, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        return view;
    }

    /**
	 * Make a tab that displays the pvs being logged and distinguishes those that are connected
	 * from those that are not connected.
	 * @return the tab view
	 */
    protected JComponent makePVTab() {
        Box view = new Box(HORIZONTAL);
        Box pvBox = new Box(VERTICAL);
        pvBox.add(new JLabel("Connected PVs:"));
        _connectedPVList = new JList();
        pvBox.add(new JScrollPane(_connectedPVList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        view.add(pvBox);
        pvBox = new Box(VERTICAL);
        pvBox.add(new JLabel("Unconnected PVs:"));
        _unconnectedPVList = new JList();
        pvBox.add(new JScrollPane(_unconnectedPVList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        view.add(pvBox);
        return view;
    }

    /**
	 * Make a tab that displays basic information about the pvlogger session.
	 * @return the tab view
	 */
    protected JComponent makeInfoTab() {
        Box tabView = new Box(BoxLayout.Y_AXIS);
        Box loggingStatusRow = new Box(BoxLayout.X_AXIS);
        tabView.add(loggingStatusRow);
        loggingStatusRow.add(new JLabel("Logging Active: "));
        _loggingStatusField = new JLabel();
        _loggingStatusField.setForeground(Color.blue);
        loggingStatusRow.add(_loggingStatusField);
        loggingStatusRow.add(Box.createHorizontalGlue());
        Box loggingPeriodRow = new Box(BoxLayout.X_AXIS);
        tabView.add(loggingPeriodRow);
        loggingPeriodRow.add(new JLabel("Logging Period(sec): "));
        _loggingPeriodField = new JLabel();
        _loggingPeriodField.setForeground(Color.blue);
        loggingPeriodRow.add(_loggingPeriodField);
        loggingPeriodRow.add(Box.createHorizontalGlue());
        return tabView;
    }

    /**
	 * Convenience method for getting the document as an instance of HistoryDocument.
	 * @return The document cast as an instace of HistoryDocument.
	 */
    public PVLoggerDocument getDocument() {
        return (PVLoggerDocument) document;
    }

    /**
	 * Get the logger model
	 * @return The logger model
	 */
    public DocumentModel getModel() {
        return getDocument().getModel();
    }

    /**
	 * Apply to the model the period setting from the period field.
	 */
    protected void applyPeriodSetting() {
        try {
            int period = Integer.parseInt(periodField.getText());
            period = Math.max(period, 1);
            period = Math.min(period, 99);
            _mainModel.setUpdatePeriod(period);
        } catch (NumberFormatException exception) {
            Toolkit.getDefaultToolkit().beep();
        }
        updateControls();
    }

    /**
     * Right justify text associated with numeric values.
     * @return A renderer for numeric values.
     */
    private TableCellRenderer makeNumericCellRenderer() {
        return new DefaultTableCellRenderer() {

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(RIGHT);
                return label;
            }
        };
    }

    /**
     * Register actions specific to this window instance. 
     * @param commander The commander with which to register the custom commands.
     */
    @Override
    protected void customizeCommands(Commander commander) {
        resumeLoggingSelectionAction = new AbstractAction("resume-logging-selections") {

            public void actionPerformed(ActionEvent event) {
                resumeLoggingSelections();
            }
        };
        commander.registerAction(resumeLoggingSelectionAction);
        stopLoggingSelectionAction = new AbstractAction("stop-logging-selections") {

            public void actionPerformed(ActionEvent event) {
                stopLoggingSelections();
            }
        };
        commander.registerAction(stopLoggingSelectionAction);
        restartSelectionAction = new AbstractAction("restart-selections") {

            public void actionPerformed(ActionEvent event) {
                final String message = "Are you sure you want to restart the selected services?";
                int result = JOptionPane.showConfirmDialog(PVLoggerWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    restartSelections();
                }
            }
        };
        commander.registerAction(restartSelectionAction);
        shutdownSelectionAction = new AbstractAction("shutdown-selections") {

            public void actionPerformed(ActionEvent event) {
                final String message = "Are you sure you want to shutdown the selected services?";
                int result = JOptionPane.showConfirmDialog(PVLoggerWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    shutdownSelections();
                }
            }
        };
        commander.registerAction(shutdownSelectionAction);
    }

    /**
	 * Shutdown the loggers corresponding to the selected rows of the logger table.
	 */
    public void shutdownSelections() {
        ((LoggerTableModel) loggerTable.getModel()).shutdownSelections(loggerTable.getSelectedRows());
    }

    /**
	 * Restart the selected loggers by stopping them, reloading their groups and restarting them.
	 */
    public void restartSelections() {
        ((LoggerTableModel) loggerTable.getModel()).restartSelections(loggerTable.getSelectedRows());
    }

    /**
	 * Start the selected loggers logging.
	 */
    public void resumeLoggingSelections() {
        ((LoggerTableModel) loggerTable.getModel()).resumeLoggingSelections(loggerTable.getSelectedRows());
    }

    /**
	 * Stop the selected loggers logging.
	 */
    public void stopLoggingSelections() {
        ((LoggerTableModel) loggerTable.getModel()).stopLoggingSelections(loggerTable.getSelectedRows());
    }
}
