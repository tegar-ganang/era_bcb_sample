package jhomenet.ui.panel.responsive;

import java.util.List;
import java.util.Set;
import java.awt.Component;
import java.awt.Color;
import javax.swing.JButton;
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
import jhomenet.commons.hw.Channel;
import jhomenet.commons.hw.sensor.ValueSensor;
import jhomenet.commons.responsive.condition.*;
import jhomenet.commons.ui.UIHelper;
import jhomenet.ui.action.*;
import jhomenet.ui.action.responsive.*;
import jhomenet.ui.factories.ImageFactory;
import jhomenet.ui.panel.AbstractEditorPanel;
import jhomenet.ui.panel.BackgroundPanel;
import jhomenet.ui.panel.PanelFactory;

/**
 * TODO: Class description.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class DifferenceSensorConditionPanel extends AbstractEditorPanel<DifferenceValueCondition> {

    /**
     * Define a logging mechanism.
     */
    private static Logger logger = Logger.getLogger(DifferenceSensorConditionPanel.class.getName());

    /**
     * Reference to the server context.
     */
    private final GeneralApplicationContext serverContext;

    /**
     * 
     */
    private static final String title = "Difference sensor condition editor";

    private static final String desc = "Create/edit a difference sensor responsive sensor condition";

    /**
     * 
     */
    private JButton save_b;

    /**
     * The condition description textfield.
     */
    private JTextField conditionName_tf;

    private JTextField testValue_tf;

    /**
     * Test sensor #1
     */
    private JComboBox testSensor1_cb;

    private JComboBox testSensor1Channel_cb;

    /**
     * Test sensor #2
     */
    private JComboBox testSensor2_cb;

    private JComboBox testSensor2Channel_cb;

    private JComboBox testOperators_cb;

    private JComboBox availableUnits_cb;

    /**
     * 
     * @param condition
     * @param serverContext
     */
    public DifferenceSensorConditionPanel(DifferenceValueCondition condition, GeneralApplicationContext serverContext) {
        super(condition);
        if (serverContext == null) throw new IllegalArgumentException("Server context cannot be null!");
        this.serverContext = serverContext;
    }

    /**
     * Constructor.
     * 
     * @param serverContext
     */
    public DifferenceSensorConditionPanel(GeneralApplicationContext serverContext) {
        super();
        if (serverContext == null) throw new IllegalArgumentException("Server context cannot be null!");
        this.serverContext = serverContext;
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
        FormLayout panelLayout = new FormLayout("4dlu, right:pref, 4dlu, pref, 4dlu, right:pref, 4dlu, pref, 4dlu", "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.addLabel("Condition name: ", cc.xy(2, 2));
        builder.add(conditionName_tf, cc.xyw(4, 2, 5));
        builder.addLabel("Sensor #1:", cc.xy(2, 4));
        builder.add(testSensor1_cb, cc.xy(4, 4));
        builder.addLabel("Channel:", cc.xy(6, 4));
        builder.add(testSensor1Channel_cb, cc.xy(8, 4));
        builder.addLabel("Sensor #2:", cc.xy(2, 6));
        builder.add(testSensor2_cb, cc.xy(4, 6));
        builder.addLabel("Channel:", cc.xy(6, 6));
        builder.add(testSensor2Channel_cb, cc.xy(8, 6));
        builder.addLabel("Difference value:", cc.xy(2, 8));
        builder.add(testValue_tf, cc.xy(4, 8));
        builder.addLabel("Unit: ", cc.xy(6, 8));
        builder.add(availableUnits_cb, cc.xy(8, 8));
        builder.addLabel("Test operator: ", cc.xy(2, 10));
        builder.add(testOperators_cb, cc.xy(4, 10));
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
        List<RegisteredHardware> hardwareList = serverContext.getHardwareManager().getRegisteredHardware();
        testSensor1_cb = new JComboBox();
        for (RegisteredHardware hardware : hardwareList) {
            if (hardware instanceof ValueSensor) testSensor1_cb.addItem(new CustomValueSensorListItem((ValueSensor) hardware));
        }
        testSensor1Channel_cb = new JComboBox();
        testSensor1_cb.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
        testSensor2_cb = new JComboBox();
        for (RegisteredHardware hardware : hardwareList) {
            if (hardware instanceof ValueSensor) testSensor2_cb.addItem(new CustomValueSensorListItem((ValueSensor) hardware));
        }
        testSensor2Channel_cb = new JComboBox();
        testSensor2_cb.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                refresh();
            }
        });
        testValue_tf = new JTextField();
        testValue_tf.setToolTipText("<html>Enter a difference value between Sensor #1 and Sensor #2<br>" + "that will trigger a response.</html>");
        availableUnits_cb = new JComboBox();
        testOperators_cb = new JComboBox();
        testOperators_cb.addItem(ValueConditionOperator.GREATER_THAN);
        testOperators_cb.addItem(ValueConditionOperator.LESS_THAN);
        this.save_b = this.panelGroupManager.addGroupButtonAction(saveAction);
        if (testSensor1_cb.getItemCount() > 0) testSensor1_cb.setSelectedIndex(0);
    }

    /**
     * 
     */
    private void refresh() {
        this.testSensor1Channel_cb.removeAllItems();
        CustomValueSensorListItem li_1 = (CustomValueSensorListItem) this.testSensor1_cb.getSelectedItem();
        ValueSensor s1 = li_1.getSensor();
        List<Channel> channels = s1.getChannels();
        for (Channel c : channels) {
            this.testSensor1Channel_cb.addItem(c);
        }
        this.testSensor2Channel_cb.removeAllItems();
        CustomValueSensorListItem li_2 = (CustomValueSensorListItem) this.testSensor2_cb.getSelectedItem();
        ValueSensor s2 = li_2.getSensor();
        channels = s2.getChannels();
        for (Channel c : channels) {
            this.testSensor2Channel_cb.addItem(c);
        }
        if (s1.getHardwareClassname().equals(s2.getHardwareClassname())) {
            testValue_tf.setEnabled(true);
            availableUnits_cb.setEnabled(true);
            testOperators_cb.setEnabled(true);
            availableUnits_cb.removeAllItems();
            Set<Unit> units = s1.getAvailableUnits();
            for (Unit unit : units) {
                availableUnits_cb.addItem(unit);
            }
            this.save_b.setEnabled(true);
        } else {
            testValue_tf.setEnabled(false);
            availableUnits_cb.setEnabled(false);
            testOperators_cb.setEnabled(false);
            this.save_b.setEnabled(false);
        }
    }

    /**
     * @see jhomenet.ui.panel.AbstractEditorPanel#buttonClicked(java.lang.String)
     */
    @Override
    protected void buttonClicked(String buttonId) {
        logger.debug("Received notification of a button click: " + buttonId);
        if (buttonId.equals(AbstractEditorPanel.ACTION_SAVE)) {
            ValueSensor sensor1 = ((CustomValueSensorListItem) testSensor1_cb.getSelectedItem()).getSensor();
            ValueSensor sensor2 = ((CustomValueSensorListItem) testSensor2_cb.getSelectedItem()).getSensor();
            String conditionName = conditionName_tf.getText();
            if (conditionName == null || conditionName.length() <= 0) {
                UIHelper.showErrorDialog(panelGroupManager.getWindowWrapper().getContainer(), "Error", "<html>The condition name may not be blank.<br>Please enter a valid condition name.</html>");
                return;
            }
            String testValueString = testValue_tf.getText();
            if (testValueString == null || testValueString.length() <= 0) {
                UIHelper.showErrorDialog(panelGroupManager.getWindowWrapper().getContainer(), "Error", "<html>The difference value may not be blank.<br>Please enter a difference value.</html>");
                return;
            }
            ValueData testValue = new ValueData(Double.parseDouble(testValueString), (Unit) availableUnits_cb.getSelectedItem());
            DifferenceValueCondition dvc = this.getUserObject();
            if (dvc == null) {
                dvc = new DifferenceValueCondition(conditionName, sensor1, ((Channel) this.testSensor1Channel_cb.getSelectedItem()).getChannelNum(), sensor2, ((Channel) this.testSensor2Channel_cb.getSelectedItem()).getChannelNum(), testValue, (ValueConditionOperator) testOperators_cb.getSelectedItem());
            } else {
                dvc.setTestValue(testValue);
                dvc.setTestOperator((ValueConditionOperator) testOperators_cb.getSelectedItem());
            }
            new StoreConditionAction(dvc, this.serverContext).runWithProgressWindow();
            panelGroupManager.requestWindowDispose();
        }
    }

    /**
     * Custom sensor combobox list item.
     * 
     * @author David Irwin
     */
    class CustomValueSensorListItem {

        private ValueSensor sensor;

        public CustomValueSensorListItem(ValueSensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public String toString() {
            return sensor.getHardwareSetupDescription() + " (" + sensor.getHardwareAddr() + ")";
        }

        public ValueSensor getSensor() {
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
