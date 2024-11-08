package jhomenet.ui.panel.responsive;

import java.util.List;
import java.util.ArrayList;
import java.awt.Component;
import java.awt.Color;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.measure.unit.Unit;
import org.apache.log4j.Logger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.data.ValueData;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.hw.sensor.ValueSensor;
import jhomenet.commons.hw.sensor.StateSensor;
import jhomenet.commons.hw.states.State;
import jhomenet.commons.responsive.condition.*;
import jhomenet.commons.ui.UIHelper;
import jhomenet.ui.action.*;
import jhomenet.ui.factories.ImageFactory;
import jhomenet.ui.action.responsive.*;
import jhomenet.ui.panel.AbstractEditorPanel;
import jhomenet.ui.panel.BackgroundPanel;
import jhomenet.ui.panel.PanelFactory;

/**
 * TODO: Class description.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class SensorConditionPanel extends AbstractEditorPanel<SensorCondition> {

    /**
     * Define a logging mechanism.
     */
    private static Logger logger = Logger.getLogger(SensorConditionPanel.class.getName());

    /**
     * Reference to the server context.
     */
    private final GeneralApplicationContext serverContext;

    /**
     * 
     */
    private static final String title = "Sensor condition editor";

    private static final String desc = "Create/edit a sensor responsive sensor condition";

    /**
     * The condition description textfield.
     */
    private JTextField conditionName_tf;

    private JTextField testValue_tf;

    /**
     * Combo boxes
     */
    private JComboBox sensors_cb;

    private JComboBox channel_cb;

    private JComboBox testOperators_cb;

    private JComboBox availableUnits_cb;

    private JComboBox testState_cb;

    /**
     * Labels.
     */
    private JLabel conditionType_l;

    private JLabel operator_l;

    private JLabel testValue_l;

    /**
     * Get the list of binary operators
     */
    private final List<State> availableStates = new ArrayList<State>();

    /**
     * 
     * @param condition
     * @param serverContext
     */
    public SensorConditionPanel(SensorCondition condition, GeneralApplicationContext serverContext) {
        super(condition);
        if (serverContext == null) throw new IllegalArgumentException("Server context cannot be null!");
        this.serverContext = serverContext;
        availableStates.add(State.ONSTATE);
        availableStates.add(State.OFFSTATE);
    }

    /**
     * Constructor.
     * 
     * @param serverContext
     */
    public SensorConditionPanel(GeneralApplicationContext serverContext) {
        this(null, serverContext);
    }

    /**
     * @see jhomenet.ui.panel.AbstractPanel#getHeaderPanel()
     */
    @Override
    protected JPanel getHeaderPanel() {
        return PanelFactory.buildHeader(title, desc, ImageFactory.toolsIcon);
    }

    /**
     * @see jhomenet.ui.panel.CustomPanel#buildPanel()
     */
    public BackgroundPanel buildPanelImpl() {
        initComponents();
        FormLayout panelLayout = new FormLayout("4dlu, right:pref, 4dlu, 100dlu, 4dlu, 50dlu, 4dlu", "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.addLabel("Condition name: ", cc.xy(2, 2));
        builder.add(conditionName_tf, cc.xyw(4, 2, 3));
        builder.addLabel("Sensor:", cc.xy(2, 4));
        builder.add(sensors_cb, cc.xyw(4, 4, 3));
        builder.addLabel("Channel:", cc.xy(2, 6));
        builder.add(channel_cb, cc.xyw(4, 6, 3));
        builder.addLabel("Condition type:", cc.xy(2, 8));
        builder.add(conditionType_l, cc.xyw(4, 8, 3));
        builder.add(operator_l, cc.xy(2, 10));
        builder.add(testOperators_cb, cc.xyw(4, 10, 3));
        builder.add(testValue_l, cc.xy(2, 12));
        builder.add(testValue_tf, cc.xy(4, 12));
        builder.add(testState_cb, cc.xy(4, 12));
        builder.add(availableUnits_cb, cc.xy(6, 12));
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
     * @see jhomenet.ui.panel.CustomPanel#getPanelName()
     */
    public String getPanelName() {
        return "Sensor Condition Editor";
    }

    /**
     * Initialize the GUI components.
     */
    private void initComponents() {
        conditionName_tf = new JTextField();
        conditionType_l = new JLabel("");
        testValue_l = new JLabel("Test value:");
        testValue_tf = new JTextField();
        testOperators_cb = new JComboBox();
        availableUnits_cb = new JComboBox();
        sensors_cb = new JComboBox();
        for (RegisteredHardware hardware : serverContext.getHardwareManager().getRegisteredHardware()) {
            if (hardware instanceof Sensor) sensors_cb.addItem(new CustomSensorListItem((Sensor) hardware));
        }
        sensors_cb.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
        operator_l = new JLabel("Operator: ");
        channel_cb = new JComboBox();
        testState_cb = new JComboBox();
        this.panelGroupManager.addGroupButtonAction(saveAction);
        if (sensors_cb.getItemCount() > 0) sensors_cb.setSelectedIndex(0);
        this.loadEditor();
    }

    /**
     * 
     */
    private void loadEditor() {
        SensorCondition condition = this.getUserObject();
        if (condition != null) {
            this.conditionName_tf.setText(condition.getConditionName());
            this.conditionName_tf.setEditable(false);
            this.sensors_cb.setSelectedItem(condition.getSensor());
            this.sensors_cb.setEnabled(false);
            this.channel_cb.setSelectedItem(condition.getChannel());
            this.channel_cb.setEnabled(false);
            refresh();
            if (condition instanceof ValueCondition) {
                this.testOperators_cb.setSelectedItem(((ValueCondition) condition).getTestOperator());
                this.testValue_tf.setText(((ValueCondition) condition).getTestValue().getValue().toString());
                this.availableUnits_cb.setSelectedItem(((ValueCondition) condition).getTestValue().getUnit());
            } else if (condition instanceof StateCondition) {
                this.testState_cb.setSelectedItem(((StateCondition) condition).getTestState());
            } else {
                logger.error("Incompatible condition type: " + condition.getClass().getName());
            }
        }
    }

    /**
     * 
     */
    private void refresh() {
        CustomSensorListItem selectedSensor = (CustomSensorListItem) sensors_cb.getSelectedItem();
        if (selectedSensor == null) return;
        Sensor sensor = selectedSensor.getSensor();
        testOperators_cb.removeAllItems();
        availableUnits_cb.removeAllItems();
        if (sensor instanceof ValueSensor) {
            for (ValueConditionOperator operator : ValueConditionOperator.values()) testOperators_cb.addItem(operator);
            testOperators_cb.setEnabled(true);
            for (Unit unit : ((ValueSensor) sensor).getAvailableUnits()) availableUnits_cb.addItem(unit);
            conditionType_l.setText("value condition");
            operator_l.setEnabled(true);
            testValue_l.setText("Test value:");
            testValue_tf.setVisible(true);
            testState_cb.setVisible(false);
            availableUnits_cb.setVisible(true);
        } else if (sensor instanceof StateSensor) {
            testState_cb.removeAllItems();
            for (State state : ((StateSensor) sensor).getAvailableStates()) testState_cb.addItem(state);
            operator_l.setEnabled(false);
            testOperators_cb.setEnabled(false);
            conditionType_l.setText("state condition");
            testValue_l.setText("Test state:");
            testValue_tf.setVisible(false);
            testState_cb.setVisible(true);
            availableUnits_cb.setVisible(false);
        }
        channel_cb.removeAllItems();
        for (int i = 0; i < sensor.getNumChannels(); i++) channel_cb.addItem(i);
    }

    /**
     * @see jhomenet.ui.panel.AbstractEditorPanel#buttonClicked(java.lang.String)
     */
    @Override
    protected void buttonClicked(String buttonId) {
        logger.debug("Received notification of a button click: " + buttonId);
        if (buttonId.equals(AbstractEditorPanel.ACTION_SAVE)) {
            String conditionName = this.conditionName_tf.getText().trim();
            if (conditionName.equals("")) {
                UIHelper.showErrorDialog(panelGroupManager.getWindowWrapper().getContainer(), "Error", "Please enter a condition name!");
                return;
            }
            CustomSensorListItem selectedSensor = (CustomSensorListItem) sensors_cb.getSelectedItem();
            Sensor sensor = selectedSensor.getSensor();
            if (sensor instanceof ValueSensor) {
                if (testValue_tf.getText().equals("")) {
                    UIHelper.showErrorDialog(panelGroupManager.getWindowWrapper().getContainer(), "Error", "Please enter a test value!");
                    return;
                }
                ValueCondition vc = (ValueCondition) this.getUserObject();
                if (vc == null) {
                    vc = new ValueCondition(conditionName, (ValueSensor) sensor, (ValueConditionOperator) testOperators_cb.getSelectedItem(), new ValueData(Double.parseDouble(testValue_tf.getText()), (Unit) availableUnits_cb.getSelectedItem()));
                } else {
                    vc.setTestValue(new ValueData(Double.parseDouble(testValue_tf.getText()), (Unit) availableUnits_cb.getSelectedItem()));
                    vc.setTestOperator((ValueConditionOperator) testOperators_cb.getSelectedItem());
                }
                new StoreConditionAction(vc, this.serverContext).runWithProgressWindow();
            } else if (sensor instanceof StateSensor) {
                StateCondition sc = (StateCondition) this.getUserObject();
                if (sc == null) {
                } else {
                }
            }
            panelGroupManager.requestWindowDispose();
        }
    }

    /**
     * Custom sensor combobox list item.
     * 
     * @author David Irwin
     */
    class CustomSensorListItem {

        private Sensor sensor;

        public CustomSensorListItem(Sensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public String toString() {
            return sensor.getHardwareSetupDescription() + " (" + sensor.getHardwareAddr() + ")";
        }

        public Sensor getSensor() {
            return sensor;
        }
    }

    class CustomSensorListCellRenderer extends JLabel implements ListCellRenderer {

        /**
         * Constructor.
         */
        CustomSensorListCellRenderer() {
            setOpaque(true);
        }

        /**
         * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) setText(value.toString());
            Color background;
            Color foreground;
            JList.DropLocation dropLocation = list.getDropLocation();
            if (dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index) {
                background = Color.BLUE;
                foreground = Color.WHITE;
            } else if (isSelected) {
                background = Color.RED;
                foreground = Color.WHITE;
            } else {
                background = Color.WHITE;
                foreground = Color.BLACK;
            }
            setBackground(background);
            setForeground(foreground);
            return this;
        }
    }
}
