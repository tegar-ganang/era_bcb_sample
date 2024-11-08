package gov.sns.apps.scope;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import gov.sns.ca.*;
import gov.sns.tools.swing.*;

/**
 * Controller of a panel which handles the knobs of a single channel.
 *
 * @author  tap
 */
public class ChannelPanel extends Box implements SwingConstants, ChannelModelListener, SettingListener {

    protected final int MAX_SCALE_POWER = 5;

    protected final double signalOffsetResolution = 0.1;

    protected final DecimalFormat offsetFormat = new DecimalFormat("#,##0.0");

    protected ChannelModel channelModel;

    protected ChannelStatusTableModel _channelStatusTableModel;

    protected JLabel channelField;

    protected JLabel _unitsField;

    protected JCheckBox _enableButton;

    protected ScopeScaleControl scaleControl;

    protected TractorKnob signalOffsetTractor;

    protected JTextField signalOffsetValueField;

    /** Create a new channel panel */
    public ChannelPanel() {
        this(null, "");
    }

    /** Creates new form ChannelPanel */
    public ChannelPanel(ChannelModel newModel, String newTitle) {
        super(VERTICAL);
        initComponents();
        setChannelModel(null, newModel);
    }

    /** 
     * Set a new channel model for display.  Buttons are used to select which channel to display.  Each channel is associated with a channel model
     * to manage the channel.  When the user presses a channel button we update the channel panel with the information for the corresponding channel model.
     * @param sender The button that fired the event
     * @param newModel The new channel model to display
     */
    public void setChannelModel(final AbstractButton sender, final ChannelModel newModel) {
        if (channelModel != null) {
            channelModel.removeChannelModelListener(this);
            channelModel.removeSettingListener(this);
        }
        channelModel = newModel;
        if (channelModel != null) {
            channelModel.addChannelModelListener(this);
            channelModel.addSettingListener(this);
        }
        _channelStatusTableModel.setChannelModel(channelModel);
        try {
            final String title = channelModel.getID();
            final TitledBorder border = (TitledBorder) getBorder();
            border.setTitle(title);
            border.setBorder(new LineBorder(sender.getForeground()));
        } catch (Exception exception) {
        }
        updateView();
    }

    /** Reset the keyboard focus to the appropriate control */
    void resetDefaultFocus() {
    }

    /** 
	 * Safely update the view regardless if it is called from the event dispatch thread 
	 * or another application thread 
	 */
    protected void safelyUpdateView() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateView();
        } else {
            dispatchUpdateView();
        }
    }

    /** Update the view in a thread safe way by using the update in the Swing event dispatch queue */
    protected void dispatchUpdateView() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    updateView();
                }
            });
        } catch (InterruptedException exception) {
            System.err.println(exception);
            exception.printStackTrace();
        } catch (java.lang.reflect.InvocationTargetException exception) {
            System.err.println(exception);
            exception.printStackTrace();
        }
    }

    /** Update the view with model information */
    protected void updateView() {
        String channelName = "";
        if (channelModel != null) {
            Channel channel = channelModel.getChannel();
            if (channel != null) {
                channelName = channelModel.getChannelName();
                try {
                    if (channel.isConnected()) {
                        String units = channel.getUnits();
                        _unitsField.setText(units);
                    }
                } catch (ConnectionException exception) {
                } catch (GetException exception) {
                    System.err.println(exception);
                }
            }
            _enableButton.setEnabled(channelModel.getChannel() != null);
            _enableButton.setSelected(channelModel.isEnabled());
            scaleControl.setEnabled(channelModel.isEnabled());
            scaleControl.setValue(1.0 / channelModel.getSignalScale());
            signalOffsetTractor.setEnabled(channelModel.isEnabled());
            double signalOffset = channelModel.getSignalOffset();
            signalOffsetTractor.setValue((long) (signalOffset / signalOffsetResolution));
            signalOffsetValueField.setText(offsetFormat.format(signalOffset));
        } else {
            _enableButton.setEnabled(false);
        }
        channelField.setText(channelName);
        repaint();
    }

    /**
	 * Get the string for displaying the connection status
	 * @param isConnected specifies whether to display a "connected" label or "disconnected" label
	 * @return a green "connected" label if connected and a red "disconnected" label if not
	 */
    protected String connectionString(boolean isConnected) {
        final String header = "<html><body>";
        final String footer = "</body></html>";
        final String body = isConnected ? "<font color=\"#00bb00\">Connected</font>" : "<font color=\"#ff0000\">Disconnected</font>";
        return header + body + footer;
    }

    /** 
     * Create and layout the components on the panel.
     */
    protected void initComponents() {
        setBorder(new TitledBorder(""));
        Box channelRow = new Box(HORIZONTAL);
        _enableButton = new JCheckBox("Enable");
        _enableButton.setMargin(new Insets(1, 1, 1, 1));
        _enableButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                if (channelModel != null) {
                    channelModel.toggleEnable();
                }
            }
        });
        channelRow.add(_enableButton);
        channelRow.add(Box.createHorizontalStrut(10));
        channelField = new JLabel("Waveform PV");
        channelRow.add(channelField);
        channelRow.add(Box.createHorizontalGlue());
        add(channelRow);
        Box unitsRow = new Box(HORIZONTAL);
        unitsRow.add(new JLabel("Units: "));
        _unitsField = new JLabel("Unspecified");
        unitsRow.add(_unitsField);
        unitsRow.add(Box.createHorizontalGlue());
        add(unitsRow);
        add(Box.createVerticalStrut(10));
        _channelStatusTableModel = new ChannelStatusTableModel(channelModel);
        JTable connectionTable = new JTable(_channelStatusTableModel);
        connectionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        final javax.swing.table.TableColumn labelColumn = connectionTable.getColumnModel().getColumn(_channelStatusTableModel.LABEL_COLUMN);
        labelColumn.setMaxWidth(new JLabel("wWaveformw").getPreferredSize().width);
        add(connectionTable.getTableHeader());
        add(connectionTable);
        add(Box.createVerticalStrut(10));
        Box signalRow = new Box(HORIZONTAL);
        scaleControl = new ScopeScaleControl("Units/Div", -MAX_SCALE_POWER, MAX_SCALE_POWER);
        scaleControl.setValue(1.0);
        scaleControl.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent event) {
                if (channelModel != null) {
                    double scale = 1.0 / scaleControl.getValue();
                    channelModel.setSignalScale(scale);
                }
            }
        });
        signalRow.add(Box.createHorizontalGlue());
        signalRow.add(scaleControl);
        signalOffsetTractor = new TractorKnob(VERTICAL, 0, -1000, 1000);
        signalOffsetTractor.addTractorListener(new TractorListener() {

            public void valueChanged(TractorKnob knob, long value) {
                double offset = knob.getValue() * signalOffsetResolution;
                channelModel.setSignalOffset(offset);
                signalOffsetValueField.setText(offsetFormat.format(offset));
            }
        });
        Box signalOffsetPanel = new Box(VERTICAL);
        signalOffsetPanel.setBorder(new TitledBorder("Offset"));
        signalOffsetPanel.add(signalOffsetTractor);
        signalOffsetPanel.add(Box.createVerticalStrut(5));
        signalOffsetValueField = new JTextField(6);
        signalOffsetValueField.setMaximumSize(signalOffsetValueField.getPreferredSize());
        signalOffsetValueField.setEditable(false);
        signalOffsetValueField.setHorizontalAlignment(RIGHT);
        signalOffsetPanel.add(signalOffsetValueField);
        signalRow.add(Box.createHorizontalGlue());
        signalRow.add(Box.createHorizontalGlue());
        signalRow.add(signalOffsetPanel);
        signalRow.add(Box.createHorizontalGlue());
        add(signalRow);
    }

    /**
     * Handle the ChannelModelListener event indicating that the channel is being enabled as a trigger.
     * @param source ChannelModel posting the event.
     * @param channel The channel being enabled as a trigger.
     */
    public void enableTriggerChannel(ChannelModel source, Channel channel) {
    }

    /**
     * Handle the ChannelModelListener event indicating that the channel is being disabled as a trigger.
     * @param source ChannelModel posting the event.
     * @param channel The channel being disabled as a trigger.
     */
    public void disableTriggerChannel(ChannelModel source, Channel channel) {
    }

    /**
     * Handle the ChannelModelListener event indicating that the specified channel is being disabled.
     * @param channelModel ChannelModel posting the event.
     * @param channel The channel being disabled.
     */
    public void disableChannel(ChannelModel channelModel, Channel channel) {
        safelyUpdateView();
    }

    /** 
     * Handle the ChannelModelListener event indicating that the specified channel is being enabled.
     * @param channelModel ChannelModel posting the event.
     * @param channel The channel being enabled.
     */
    public void enableChannel(ChannelModel channelModel, Channel channel) {
        safelyUpdateView();
    }

    /**
     * Event indicating that the channel model has a new channel.
     * @param source ChannelModel posting the event.
     * @param channel The new channel.
     */
    public void channelChanged(ChannelModel source, Channel channel) {
        safelyUpdateView();
    }

    /**
     * Event indicating that the channel model has a new array of element times.
     * @param source ChannelModel posting the event.
     * @param elementTimes The new element times array measured in turns.
     */
    public void elementTimesChanged(ChannelModel source, final double[] elementTimes) {
    }

    /**
     * A setting from the sender has changed.
     * @param source The object whose setting changed.
     */
    public void settingChanged(Object source) {
        safelyUpdateView();
    }
}
