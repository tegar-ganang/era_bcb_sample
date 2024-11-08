package gov.sns.apps.pvlogger;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.sql.*;
import gov.sns.application.*;
import gov.sns.xal.smf.application.AcceleratorDocument;
import gov.sns.xal.smf.*;
import gov.sns.ca.Channel;
import gov.sns.tools.bricks.WindowReference;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.data.KeyValueSorting;
import gov.sns.tools.database.*;
import gov.sns.tools.swing.*;
import gov.sns.xal.tools.widgets.NodeChannelSelector;

/** document for managing the PV Logger configuration */
public class ConfigurationDocument extends AcceleratorDocument {

    /** signals pending addition to the database for the currently selected group */
    private final List<String> PENDING_GROUP_SIGNALS;

    /** list of available signals */
    private final List<String> AVAILABLE_SIGNALS;

    /** PV Logger */
    private final LoggerConfiguration LOGGER_CONFIGURATION;

    /** table of channel groups */
    private JTable GROUP_TABLE;

    /** table model for the channel groups */
    private KeyValueTableModel<ChannelGroup> GROUP_TABLE_MODEL;

    /** table model for the channels */
    private KeyValueFilteredTableModel<String> CHANNEL_TABLE_MODEL;

    /** selector for channels */
    private KeyValueRecordSelector<String> _channelSelector;

    /** indicates whether the channel selector needs to be updated before display */
    private boolean _channelSelectorNeedsUpdate;

    /** button to revert channel additions */
    private JButton REVERT_ADDITIONS_BUTTON;

    /** button to publish channel additions */
    private JButton PUBLISH_ADDITIONS_BUTTON;

    /** Constructor */
    public ConfigurationDocument() {
        _channelSelectorNeedsUpdate = true;
        PENDING_GROUP_SIGNALS = new ArrayList<String>();
        AVAILABLE_SIGNALS = new ArrayList<String>();
        LOGGER_CONFIGURATION = new LoggerConfiguration(null);
    }

    /** Make a main window by instantiating the my custom window. */
    public void makeMainWindow() {
        final WindowReference windowReference = getDefaultWindowReference("ConfigurationWindow", this);
        mainWindow = (XalWindow) windowReference.getWindow();
        GROUP_TABLE = (JTable) windowReference.getView("Group Table");
        GROUP_TABLE_MODEL = new KeyValueTableModel<ChannelGroup>(new ArrayList<ChannelGroup>(), "label", "serviceID", "defaultLoggingPeriod", "retention", "description");
        GROUP_TABLE_MODEL.setColumnName("defaultLoggingPeriod", "Default Logging Period");
        GROUP_TABLE.setModel(GROUP_TABLE_MODEL);
        GROUP_TABLE.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        GROUP_TABLE.getSelectionModel().addListSelectionListener(new GroupSelectionHandler());
        final JTable channelTable = (JTable) windowReference.getView("Signal Table");
        CHANNEL_TABLE_MODEL = new KeyValueFilteredTableModel<String>(new ArrayList<String>(), "toString");
        CHANNEL_TABLE_MODEL.setMatchingKeyPaths("toString");
        CHANNEL_TABLE_MODEL.setColumnName("toString", "Channel");
        channelTable.setModel(CHANNEL_TABLE_MODEL);
        final JTextField channelFilterField = (JTextField) windowReference.getView("ChannelFilterField");
        channelFilterField.putClientProperty("JTextField.variant", "search");
        channelFilterField.putClientProperty("JTextField.Search.Prompt", "Filter channels");
        CHANNEL_TABLE_MODEL.setInputFilterComponent(channelFilterField);
        final JButton channelFilterClearButton = (JButton) windowReference.getView("ChannelFilterClearButton");
        channelFilterClearButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                channelFilterField.setText(null);
            }
        });
        final JButton addChannelsButton = (JButton) windowReference.getView("AddChannelsButton");
        addChannelsButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                handleChannelSelection();
            }
        });
        REVERT_ADDITIONS_BUTTON = (JButton) windowReference.getView("RevertAdditionsButton");
        REVERT_ADDITIONS_BUTTON.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                clearPendingSignals();
            }
        });
        PUBLISH_ADDITIONS_BUTTON = (JButton) windowReference.getView("PublishAdditionsButton");
        PUBLISH_ADDITIONS_BUTTON.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                publishSignals();
            }
        });
    }

    /** show this document */
    public void showDocument() {
        super.showDocument();
        updateControls();
        if (requestConnectionIfNeeded()) {
            fetchChannelGroups();
        } else {
            closeDocument();
        }
    }

    /** handle the event in which the accelerator is changed  */
    public void acceleratorChanged() {
        if (accelerator != null && selectedSequence == null) {
            _channelSelectorNeedsUpdate = true;
        }
    }

    /** handle the event in which the sequence is changed */
    public void selectedSequenceChanged() {
        if (accelerator != null && selectedSequence != null) {
            _channelSelectorNeedsUpdate = true;
        }
    }

    /** request a database connection if needed */
    private boolean requestConnectionIfNeeded() {
        final Connection originalConnection = LOGGER_CONFIGURATION.getConnection();
        if (originalConnection != null) {
            try {
                if (originalConnection.isClosed()) {
                    return requestConnection();
                } else {
                    return true;
                }
            } catch (Exception exception) {
                return requestConnection();
            }
        } else {
            return requestConnection();
        }
    }

    /** request a database connection */
    private boolean requestConnection() {
        final ConnectionDictionary dictionary = ConnectionDictionary.getPreferredInstance("personal");
        final ConnectionDialog dialog = ConnectionDialog.getInstance(mainWindow, dictionary);
        final Connection connection = dialog.showConnectionDialog(dictionary.getDatabaseAdaptor());
        LOGGER_CONFIGURATION.setConnection(connection);
        if (connection != null) {
            try {
                return !connection.isClosed();
            } catch (Exception exception) {
                return false;
            }
        } else {
            return false;
        }
    }

    /** update available signals */
    private void updateAvailableSignalsIfNeeded() {
        if (_channelSelectorNeedsUpdate) {
            final AcceleratorSeq sequence = accelerator != null ? selectedSequence != null ? selectedSequence : accelerator : null;
            AVAILABLE_SIGNALS.clear();
            if (sequence != null) {
                final List<AcceleratorNode> nodes = sequence.getAllNodes(true);
                final Set<String> signals = new HashSet<String>();
                for (final AcceleratorNode node : nodes) {
                    final List<String> handles = new ArrayList<String>(node.getHandles());
                    for (final String handle : handles) {
                        final Channel channel = node.findChannel(handle);
                        if (channel != null) {
                            signals.add(channel.channelName());
                        }
                    }
                }
                AVAILABLE_SIGNALS.addAll(signals);
                AVAILABLE_SIGNALS.removeAll(PENDING_GROUP_SIGNALS);
                Collections.sort(AVAILABLE_SIGNALS);
                buildChannelSelector();
            } else {
                _channelSelector = null;
            }
            _channelSelectorNeedsUpdate = false;
        }
    }

    /** construct a channel selector from the specified sequence */
    private void buildChannelSelector() {
        final ChannelGroup group = getSelectedChannelGroup();
        if (group != null && !AVAILABLE_SIGNALS.isEmpty()) {
            final List<String> signals = new ArrayList<String>(AVAILABLE_SIGNALS);
            final Collection<Channel> groupChannels = group.getChannels();
            for (final Channel channel : groupChannels) {
                signals.remove(channel.channelName());
            }
            _channelSelector = KeyValueRecordSelector.getInstanceWithFilterPrompt(signals, mainWindow, "Add Selected Channels", "Channel Filter", "toString");
            _channelSelector.getRecordTableModel().setColumnName("toString", "Channel");
        } else {
            _channelSelector = null;
        }
    }

    /** remove any pending signals */
    private void clearPendingSignals() {
        PENDING_GROUP_SIGNALS.clear();
        _channelSelectorNeedsUpdate = true;
        updateGroupChannels();
        updateControls();
    }

    /** publish the pending signals */
    private void publishSignals() {
        final ChannelGroup group = getSelectedChannelGroup();
        if (group != null) {
            try {
                requestConnectionIfNeeded();
                LOGGER_CONFIGURATION.publishChannelsToGroup(PENDING_GROUP_SIGNALS, group.getLabel());
                fetchChannelGroups();
                clearPendingSignals();
                _channelSelectorNeedsUpdate = true;
            } catch (Exception exception) {
                exception.printStackTrace();
                displayError("Publish Error", "Error publishing new signals.", exception);
            }
        }
    }

    /** update the group channels to include pending signals */
    private void updateGroupChannels() {
        final ChannelGroup group = getSelectedChannelGroup();
        if (group != null) {
            final List<String> channelNames = new ArrayList<String>();
            for (final Channel channel : group.getChannels()) {
                channelNames.add(channel.channelName());
            }
            channelNames.addAll(PENDING_GROUP_SIGNALS);
            Collections.sort(channelNames);
            CHANNEL_TABLE_MODEL.setRecords(channelNames);
        } else {
            CHANNEL_TABLE_MODEL.setRecords(new ArrayList<String>());
        }
    }

    /** update controls */
    private void updateControls() {
        final boolean hasAdditions = PENDING_GROUP_SIGNALS.size() > 0;
        REVERT_ADDITIONS_BUTTON.setEnabled(hasAdditions);
        PUBLISH_ADDITIONS_BUTTON.setEnabled(hasAdditions);
    }

    /** Get the selected channel group */
    private ChannelGroup getSelectedChannelGroup() {
        final int selectedRow = GROUP_TABLE.getSelectedRow();
        return selectedRow >= 0 ? GROUP_TABLE_MODEL.getRecordAtRow(selectedRow) : null;
    }

    /** fetch the channel groups from the database */
    private void fetchChannelGroups() {
        try {
            requestConnectionIfNeeded();
            final List<ChannelGroup> groups = LOGGER_CONFIGURATION.fetchChannelGroups();
            GROUP_TABLE_MODEL.setRecords(groups);
        } catch (Exception exception) {
            exception.printStackTrace();
            displayError("Fetch Error", "Error fetching channel groups.", exception);
        }
    }

    /**
	 * Get a custom menu definition for this document
     * @return The menu definition properties file path in classpath notation
	 * @see ApplicationAdaptor#getPathToResource
     */
    protected String getCustomMenuDefinitionPath() {
        return Application.getAdaptor().getPathToResource("configuration-menu");
    }

    /** implement save command to do nothing */
    public void saveDocumentAs(final java.net.URL theURL) {
    }

    /** handle channel selection */
    public void handleChannelSelection() {
        final int selectedRow = GROUP_TABLE.getSelectedRow();
        final ChannelGroup group = getSelectedChannelGroup();
        if (group != null) {
            updateAvailableSignalsIfNeeded();
            if (_channelSelector != null) {
                final List<String> channelNames = _channelSelector.showDialog();
                System.out.println("Channels: " + channelNames);
                if (channelNames != null) {
                    PENDING_GROUP_SIGNALS.addAll(channelNames);
                    Collections.sort(PENDING_GROUP_SIGNALS);
                    _channelSelectorNeedsUpdate = true;
                    updateGroupChannels();
                    updateControls();
                }
            } else {
                displayError("Channel Selection Error", "You must first select an accelerator before you can select channels.");
            }
        } else {
            displayError("Channel Selection Error", "You must first select a channel group before you can select the channels to add to it.");
        }
    }

    /** Handler of group selection events */
    class GroupSelectionHandler implements ListSelectionListener {

        public void valueChanged(final ListSelectionEvent event) {
            if (!event.getValueIsAdjusting()) {
                PENDING_GROUP_SIGNALS.clear();
                updateGroupChannels();
                _channelSelectorNeedsUpdate = true;
                ;
            }
        }
    }

    /**
	 * Test whether a connection is good
	 * @param connection the connection to test
	 * @return true if the connection is good and false if not
	 */
    private static boolean testConnection(final Connection connection) {
        try {
            return !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }
}
