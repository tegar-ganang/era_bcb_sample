package gov.sns.apps.mpsclient;

import gov.sns.application.*;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.ArrayList;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

/**
 * MPSWindow is the main window for displaying the status of a remote MPS service.
 *
 * @author  tap
 */
class MPSWindow extends XalWindow implements SwingConstants, DataKeys, ScrollPaneConstants, DocumentModelListener {

    /** date formatter for displaying timestamps */
    protected static final DateFormat TIMESTAMP_FORMAT;

    /** Table of MPS tools running on the local network */
    protected JTable mpsTable;

    /** State indicating whether the table has any selected rows */
    protected boolean mpsTableHasSelectedRows;

    /** Field for entering and displaying the update period */
    protected JTextField periodField;

    /** Action for reloading the MPS signals from the global database */
    protected Action _reloadSignalsAction;

    /** Action for shutting down the selected service */
    protected Action _shutdownServiceAction;

    /** main application wide model */
    protected MPSModel _mainModel;

    /** model for this window's document */
    protected DocumentModel _model;

    /**
	 * static initializer
	 */
    static {
        TIMESTAMP_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    }

    /** Creates a new instance of MainWindow */
    public MPSWindow(MPSDocument aDocument, MPSTableModel mpsTableModel) {
        super(aDocument);
        setSize(1000, 500);
        _model = aDocument.getModel();
        _model.addDocumentModelListener(this);
        _mainModel = _model.getMainModel();
        mpsTableHasSelectedRows = false;
        makeContent(mpsTableModel);
        manageActions();
        handleMPSEvents();
        _mainModel.updateServiceList();
    }

    /**
	 * Listen for new MPS status events and update the views accordingly
	 */
    protected void handleMPSEvents() {
        _mainModel.addMPSModelListener(new MPSModelListener() {

            /**
			 * The status of MPS tools have been updated.  Every record specifies information about 
			 * a single application.
			 * @param aModel The model whose services changed
			 * @param records The records of every application found on the local network.
			 */
            public void servicesChanged(MPSModel aModel, java.util.List records) {
                updateView();
            }

            /**
			 * The request handler associated with the specified record has checked for new status
			 * information from the remote service.
			 * @param record The request handler record for which the update has been made
			 * @param timestamp The timestamp of the check
			 */
            public void lastCheck(gov.sns.tools.data.GenericRecord record, Date timestamp) {
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
	 * Update the view to reconcile it with the model.
	 */
    protected void updateView() {
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
    }

    /**
	 * Build the component contents of the window.
	 * @param mpsTableModel The table model for the MPS view
	 */
    protected void makeContent(final MPSTableModel mpsTableModel) {
        Box mainView = new Box(VERTICAL);
        getContentPane().add(mainView);
        mainView.add(makePeriodView());
        Box mpsPanel = new Box(HORIZONTAL);
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setTopComponent(makeMPSTable(mpsTableModel));
        splitPane.setBottomComponent(makeMPSInspector());
        splitPane.setResizeWeight(0);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent event) {
                splitPane.setDividerLocation(0.20);
            }
        });
        mpsPanel.add(splitPane);
        mainView.add(mpsPanel);
        updateView();
    }

    /**
	 * Make a view that displays the period of updates and allows the user to change that period
	 * @return the period view
	 */
    protected JComponent makePeriodView() {
        JPanel periodPanel = new JPanel();
        periodPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        periodPanel.add(new JLabel("update period (sec): "));
        periodField = new JTextField(3);
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
                updateView();
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
	 * Make a table that lists the currently running MPS tools
	 * @param tableModel The table model
	 * @return the table view
	 */
    protected JComponent makeMPSTable(final MPSTableModel tableModel) {
        mpsTable = new JTable(tableModel);
        mpsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableCellRenderer numericCellRenderer = makeNumericCellRenderer();
        TableColumnModel columnModel = mpsTable.getColumnModel();
        JScrollPane mpsScrollPane = new JScrollPane(mpsTable);
        mpsScrollPane.setColumnHeaderView(mpsTable.getTableHeader());
        mpsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mpsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return mpsScrollPane;
    }

    /**
	 * Make an inspector for a selected MPS service
	 * @return the inspector view
	 */
    protected JComponent makeMPSInspector() {
        Box typeBox = new Box(BoxLayout.Y_AXIS);
        typeBox.add(new JLabel("MPS Types: "));
        final JList typeList = new JList();
        typeBox.add(new JScrollPane(typeList));
        typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        typeList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) return;
                int index = typeList.getSelectedIndex();
                _model.setSelectedMPSTypeIndex(index);
            }
        });
        typeList.setModel(new AbstractListModel() {

            {
                _model.addDocumentModelListener(new DocumentModelListener() {

                    public void handlerSelected(DocumentModel model, RequestHandler handler) {
                        typeList.clearSelection();
                        fireContentsChanged(this, 0, getSize());
                    }

                    public void mpsTypeSelected(DocumentModel model, int index) {
                    }

                    public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                    }

                    public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                    }

                    public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                    }

                    public void lastCheck(RequestHandler handler, Date timestamp) {
                    }
                });
            }

            public Object getElementAt(int index) {
                RequestHandler handler = _model.getSelectedHandler();
                return (handler != null) ? handler.getMPSTypes().get(index) : "";
            }

            public int getSize() {
                RequestHandler handler = _model.getSelectedHandler();
                return (handler != null) ? handler.getMPSTypes().size() : 0;
            }
        });
        JTabbedPane tabPane = new JTabbedPane();
        final JSplitPane inspector = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, typeBox, tabPane);
        inspector.setContinuousLayout(true);
        inspector.setResizeWeight(0);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent event) {
                inspector.setDividerLocation(0.2);
            }
        });
        mpsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                int selectedRow = mpsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    MPSTableModel tableModel = (MPSTableModel) mpsTable.getModel();
                    String id = tableModel.getRecord(selectedRow).stringValueForKey(ID_KEY);
                    _model.setSelectedHandler(_mainModel.getHandler(id));
                } else {
                    _model.setSelectedHandler(null);
                }
            }
        });
        tabPane.addTab("Latest Event", makeLatestMPSEventView());
        tabPane.addTab("First Hits", makeFirstHitsView());
        tabPane.addTab("Trip Summary", makeTripSummaryView());
        tabPane.addTab("MPS PVs", makeMPSPVsTab());
        tabPane.addTab("Input PVs", makeInputPVsTab());
        return inspector;
    }

    /**
	 * Make the view that displays the first hit statistics.
	 *
	 * @return the view that displays the first hit statistics
	 */
    protected JComponent makeFirstHitsView() {
        Box statsView = new Box(VERTICAL);
        statsView.add(new JLabel("Daily First Hit Summary:"));
        final JTextArea statsTextView = new JTextArea();
        statsTextView.setEditable(false);
        statsView.add(new JScrollPane(statsTextView, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED));
        Box buttonRow = new Box(HORIZONTAL);
        statsView.add(buttonRow);
        buttonRow.add(Box.createHorizontalGlue());
        final JButton dumpButton = new JButton("dump");
        buttonRow.add(dumpButton);
        dumpButton.setEnabled(false);
        dumpButton.addActionListener(new ActionListener() {

            JFileChooser fileChooser = new JFileChooser();

            public void actionPerformed(ActionEvent event) {
                try {
                    String text = statsTextView.getText();
                    fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "Untitled.txt"));
                    int status = fileChooser.showSaveDialog(MPSWindow.this);
                    switch(status) {
                        case JFileChooser.APPROVE_OPTION:
                            File selectedFile = fileChooser.getSelectedFile();
                            if (selectedFile.exists()) {
                                int confirm = displayConfirmDialog("Warning", "The selected file:  " + selectedFile + " already exists! \n Overwrite selection?");
                                if (confirm == NO_OPTION) return;
                            } else {
                                selectedFile.createNewFile();
                            }
                            FileWriter writer = new FileWriter(selectedFile);
                            writer.write(text, 0, text.length());
                            writer.flush();
                            break;
                        default:
                            break;
                    }
                } catch (Exception exception) {
                    displayError("Save Error", "Error saving file: ", exception);
                }
            }
        });
        _model.addDocumentModelListener(new DocumentModelListener() {

            public void handlerSelected(DocumentModel model, RequestHandler handler) {
                updateLog();
            }

            public void mpsTypeSelected(DocumentModel model, int index) {
                updateLog();
            }

            public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                updateLog();
            }

            public void lastCheck(RequestHandler handler, Date timestamp) {
            }

            protected void updateLog() {
                final int mpsType = _model.getSelectedMPSTypeIndex();
                final RequestHandler handler = _model.getSelectedHandler();
                String text = "";
                if (mpsType >= 0 && handler != null) {
                    text = handler.getFirstHitText(mpsType);
                }
                statsTextView.setText(text);
                statsTextView.setSelectionStart(0);
                statsTextView.moveCaretPosition(0);
                dumpButton.setEnabled(text != "" && text != null);
            }
        });
        return statsView;
    }

    /**
	 * Make the view that displays the daily MPS trips summary.
	 *
	 * @return the view that displays the daily MPS trip summary
	 */
    protected JComponent makeTripSummaryView() {
        Box statsView = new Box(VERTICAL);
        statsView.add(new JLabel("Daily Trip Summary:"));
        final JTextArea statsTextView = new JTextArea();
        statsTextView.setEditable(false);
        statsView.add(new JScrollPane(statsTextView, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED));
        Box buttonRow = new Box(HORIZONTAL);
        statsView.add(buttonRow);
        buttonRow.add(Box.createHorizontalGlue());
        final JButton dumpButton = new JButton("dump");
        buttonRow.add(dumpButton);
        dumpButton.setEnabled(false);
        dumpButton.addActionListener(new ActionListener() {

            JFileChooser fileChooser = new JFileChooser();

            public void actionPerformed(ActionEvent event) {
                try {
                    String text = statsTextView.getText();
                    fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "Untitled.txt"));
                    int status = fileChooser.showSaveDialog(MPSWindow.this);
                    switch(status) {
                        case JFileChooser.APPROVE_OPTION:
                            File selectedFile = fileChooser.getSelectedFile();
                            if (selectedFile.exists()) {
                                int confirm = displayConfirmDialog("Warning", "The selected file:  " + selectedFile + " already exists! \n Overwrite selection?");
                                if (confirm == NO_OPTION) return;
                            } else {
                                selectedFile.createNewFile();
                            }
                            FileWriter writer = new FileWriter(selectedFile);
                            writer.write(text, 0, text.length());
                            writer.flush();
                            break;
                        default:
                            break;
                    }
                } catch (Exception exception) {
                    displayError("Save Error", "Error saving file: ", exception);
                }
            }
        });
        _model.addDocumentModelListener(new DocumentModelListener() {

            public void handlerSelected(DocumentModel model, RequestHandler handler) {
                updateLog();
            }

            public void mpsTypeSelected(DocumentModel model, int index) {
                updateLog();
            }

            public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                updateLog();
            }

            public void lastCheck(RequestHandler handler, Date timestamp) {
            }

            protected void updateLog() {
                final int mpsType = _model.getSelectedMPSTypeIndex();
                final RequestHandler handler = _model.getSelectedHandler();
                String text = "";
                if (mpsType >= 0 && handler != null) {
                    text = handler.getTripSummary(mpsType);
                }
                statsTextView.setText(text);
                statsTextView.setSelectionStart(0);
                statsTextView.moveCaretPosition(0);
                dumpButton.setEnabled(text != "" && text != null);
            }
        });
        return statsView;
    }

    /**
	 * Make the view that displays the latest MPS event.
	 * 
	 * @return the view that displays the latest MPS event
	 */
    protected JComponent makeLatestMPSEventView() {
        final Box eventView = new Box(VERTICAL);
        final JLabel eventTimestampLabel = new JLabel("");
        eventView.add(eventTimestampLabel);
        final MPSEventTableModel eventTableModel = new MPSEventTableModel(null);
        JTable eventTable = new JTable(eventTableModel);
        JScrollPane eventScrollPane = new JScrollPane(eventTable);
        eventView.add(eventScrollPane);
        eventScrollPane.setColumnHeaderView(eventTable.getTableHeader());
        Box eventButtonRow = new Box(HORIZONTAL);
        eventView.add(eventButtonRow);
        eventButtonRow.add(Box.createHorizontalGlue());
        final JButton bufferButton = new JButton("Buffer");
        bufferButton.setEnabled(false);
        eventButtonRow.add(bufferButton);
        bufferButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                int mpsType = _model.getSelectedMPSTypeIndex();
                RequestHandler handler = _model.getSelectedHandler();
                if (mpsType >= 0 && handler != null) {
                    EventBufferDocument bufferDocument = new EventBufferDocument(handler, mpsType);
                    Application.getApp().produceDocument(bufferDocument, false);
                    bufferDocument.getMainWindow().setLocationRelativeTo(MPSWindow.this);
                    bufferDocument.showDocument();
                }
            }
        });
        _model.addDocumentModelListener(new DocumentModelListener() {

            public void handlerSelected(DocumentModel model, RequestHandler handler) {
                updateLog();
            }

            public void mpsTypeSelected(DocumentModel model, int index) {
                updateLog();
            }

            public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
            }

            public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                updateLog();
            }

            public void lastCheck(RequestHandler handler, Date timestamp) {
            }

            protected void updateLog() {
                final int mpsType = _model.getSelectedMPSTypeIndex();
                final RequestHandler handler = _model.getSelectedHandler();
                if (mpsType >= 0 && handler != null) {
                    MPSEvent mpsEvent = handler.getLatestMPSEvent(mpsType);
                    if (mpsEvent != null) {
                        eventTimestampLabel.setText(mpsEvent.getTimestamp().toString());
                        eventTableModel.setEvent(mpsEvent);
                        bufferButton.setEnabled(true);
                    } else {
                        clearEvent();
                    }
                } else {
                    clearEvent();
                }
            }

            protected void clearEvent() {
                eventTimestampLabel.setText("");
                eventTableModel.setEvent(null);
                bufferButton.setEnabled(false);
            }
        });
        return eventView;
    }

    /**
	 * Make a tab that displays the pvs being logged and distinguishes those that are connected
	 * from those that are not connected.
	 * @return the tab view
	 */
    protected JComponent makeMPSPVsTab() {
        Box view = new Box(VERTICAL);
        final JList pvList = new JList();
        view.add(new JScrollPane(pvList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        final java.util.List channelRefs = new ArrayList();
        pvList.setModel(new AbstractListModel() {

            {
                _model.addDocumentModelListener(new DocumentModelListener() {

                    public void handlerSelected(DocumentModel model, RequestHandler handler) {
                        updatePVs(this);
                    }

                    public void mpsTypeSelected(DocumentModel model, int index) {
                        updatePVs(this);
                    }

                    public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                        updatePVs(this);
                    }

                    public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                    }

                    public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                    }

                    public void lastCheck(RequestHandler handler, Date timestamp) {
                    }
                });
            }

            public void updatePVs(final DocumentModelListener source) {
                channelRefs.clear();
                final int mpsType = _model.getSelectedMPSTypeIndex();
                final RequestHandler handler = _model.getSelectedHandler();
                if (mpsType >= 0 && handler != null) {
                    java.util.List refs = new ArrayList(handler.getMPSPVs(mpsType));
                    channelRefs.addAll(refs);
                }
                fireContentsChanged(source, 0, getSize());
            }

            public int getSize() {
                int mpsType = _model.getSelectedMPSTypeIndex();
                RequestHandler handler = _model.getSelectedHandler();
                return (mpsType >= 0 && handler != null) ? handler.getMPSPVs(mpsType).size() : 0;
            }

            public Object getElementAt(int index) {
                try {
                    ChannelRef channelRef = (ChannelRef) channelRefs.get(index);
                    String pv = channelRef.getPV();
                    return (channelRef.isConnected()) ? pv : "<html><body><font COLOR=#ff0000>" + pv + "</body></html>";
                } catch (IndexOutOfBoundsException exception) {
                    return "";
                }
            }
        });
        return view;
    }

    /**
	 * Make a tab that displays the pvs being logged and distinguishes those that are connected
	 * from those that are not connected.
	 * @return the tab view
	 */
    protected JComponent makeInputPVsTab() {
        Box view = new Box(VERTICAL);
        final JList pvList = new JList();
        view.add(new JScrollPane(pvList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER));
        final java.util.List channelRefs = new ArrayList();
        pvList.setModel(new AbstractListModel() {

            {
                _model.addDocumentModelListener(new DocumentModelListener() {

                    public void handlerSelected(DocumentModel model, RequestHandler handler) {
                        updatePVs(this);
                    }

                    public void mpsTypeSelected(DocumentModel model, int index) {
                        updatePVs(this);
                    }

                    public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                    }

                    public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
                        updatePVs(this);
                    }

                    public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
                    }

                    public void lastCheck(RequestHandler handler, Date timestamp) {
                    }
                });
            }

            public void updatePVs(final DocumentModelListener source) {
                channelRefs.clear();
                final int mpsType = _model.getSelectedMPSTypeIndex();
                final RequestHandler handler = _model.getSelectedHandler();
                if (mpsType >= 0 && handler != null) {
                    java.util.List refs = new ArrayList(handler.getInputPVs(mpsType));
                    channelRefs.addAll(refs);
                }
                fireContentsChanged(source, 0, getSize());
            }

            public int getSize() {
                return channelRefs.size();
            }

            public Object getElementAt(int index) {
                try {
                    ChannelRef channelRef = (ChannelRef) channelRefs.get(index);
                    String pv = channelRef.getPV();
                    return (channelRef.isConnected()) ? pv : "<html><body><font COLOR=#ff0000>" + pv + "</body></html>";
                } catch (IndexOutOfBoundsException exception) {
                    return "";
                }
            }
        });
        return view;
    }

    /**
	 * Convenience method for getting the document as an instance of HistoryDocument.
	 * @return The document cast as an instace of HistoryDocument.
	 */
    public MPSDocument getDocument() {
        return (MPSDocument) document;
    }

    /**
	 * Get the MPS model
	 * @return The MPS model
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
            period = Math.min(period, 999);
            _mainModel.setUpdatePeriod(period);
        } catch (NumberFormatException exception) {
            Toolkit.getDefaultToolkit().beep();
        }
        updateView();
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
	 * Add event listeners to manage the enable state of the actions.
	 */
    protected void manageActions() {
        setTableSelectionActionsEnabled(false);
        mpsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                setTableSelectionActionsEnabled(mpsTable.getSelectedRow() >= 0);
            }
        });
    }

    /**
	 * Enable/disable table selection actions depending on the specified state.
	 * @param state enable actions if true and disable actions if false
	 */
    protected void setTableSelectionActionsEnabled(boolean state) {
        mpsTableHasSelectedRows = state;
        updateView();
    }

    /**
     * Register actions specific to this window instance. 
     * @param commander The commander with which to register the custom commands.
     */
    @Override
    protected void customizeCommands(Commander commander) {
        _reloadSignalsAction = new AbstractAction("reload-signals") {

            public void actionPerformed(ActionEvent event) {
                RequestHandler handler = _model.getSelectedHandler();
                handler.reloadSignals();
            }
        };
        _reloadSignalsAction.setEnabled(false);
        commander.registerAction(_reloadSignalsAction);
        _shutdownServiceAction = new AbstractAction("shutdown-service") {

            public void actionPerformed(ActionEvent event) {
                final String message = "Are you sure you want to shutdown the selected service?";
                int result = JOptionPane.showConfirmDialog(MPSWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    RequestHandler handler = _model.getSelectedHandler();
                    handler.shutdownService(0);
                }
            }
        };
        _shutdownServiceAction.setEnabled(false);
        commander.registerAction(_shutdownServiceAction);
    }

    /**
	 * Indicates that a new handler has been selected.
	 * @param model The document model posting the event.
	 * @param handler The selected handler or null if no handler is selected.
	 */
    public void handlerSelected(DocumentModel model, RequestHandler handler) {
        _reloadSignalsAction.setEnabled(handler != null);
        _shutdownServiceAction.setEnabled(handler != null);
    }

    /**
	 * This event is sent to indicate that a new MPS type has been selected.
	 * @param model The model sending the event
	 * @param index The index of the MPS type selected or -1 if none is selected
	 */
    public void mpsTypeSelected(DocumentModel model, int index) {
    }

    /**
	 * Indicates that MPS channels have been updated.
	 * @param handler The handler sending the event
	 * @param mpsTypeIndex index of the MPS type for which the event applies
	 * @param channelRefs The list of the new ChannelRef instances
	 */
    public void mpsChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
    }

    /**
	 * Indicates that input channels have been updated.
	 * @param handler The handler sending the event
	 * @param mpsTypeIndex index of the MPS type for which the event applies
	 * @param channelRefs The list of the new ChannelRef instances
	 */
    public void inputChannelsUpdated(RequestHandler handler, int mpsTypeIndex, java.util.List channelRefs) {
    }

    /**
	 * Indicates that an MPS event has happened.
	 * @param handler The handler sending the event
	 * @param mpsTypeIndex index of the MPS type for which the event applies
	 */
    public void mpsEventsUpdated(RequestHandler handler, int mpsTypeIndex) {
    }

    /**
	 * Indicates that the handler has checked for new status from the MPS service.
	 * @param handler The handler sending the event.
	 * @param timestamp The timestamp of the latest status check
	 */
    public void lastCheck(RequestHandler handler, Date timestamp) {
    }
}
