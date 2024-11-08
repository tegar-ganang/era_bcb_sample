package jhomenet.ui.panel;

import java.util.List;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXCollapsiblePane;
import javax.measure.unit.Unit;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.hw.sensor.ValueSensor;
import jhomenet.commons.polling.PollingIntervals;
import jhomenet.ui.factories.IconFactory;

/**
 * TODO: Class description.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class HardwareInfoPanel extends AbstractEditorPanel {

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(HardwareInfoPanel.class.getName());

    /**
	 * Apply action.
	 */
    protected final AbstractAction applyAction = new ApplyAction();

    /**
	 * 
	 */
    private static final String applyButtonId = "apply-button";

    /**
	 * Text fields.
	 */
    private JTextField hardwareAddr_tf;

    private JTextField hardwareDesc_tf;

    private JTextField setupDesc_tf;

    private JTextField classname_tf;

    private JTextField numChannels_tf;

    /**
	 * Polling interval visual objects.
	 */
    private JLabel pollingInterval_l;

    private JComboBox pollingInterval_cb;

    /**
	 * Preferred data unit visual objects. 
	 */
    private JLabel preferredUnit_l;

    private JComboBox preferredUnit_cb;

    /**
	 * Channel description objects.
	 */
    private JButton channelEditting_b;

    private JXCollapsiblePane channelEditting_cp;

    private final List<JTextField> channelTextFields;

    /**
	 * Panel header information
	 */
    private final String title = "Hardware editor";

    private final String description = "Edit hardware properties";

    /**
	 * Reference to the hardware object.
	 */
    private final RegisteredHardware hardware;

    /**
	 * 
	 */
    private final GeneralApplicationContext serverContext;

    /**
	 * Constructor.
	 * 
	 * @param hardware Hardware object
	 * @param serverContext 
	 */
    public HardwareInfoPanel(RegisteredHardware hardware, GeneralApplicationContext serverContext) {
        super();
        if (hardware == null) throw new IllegalArgumentException("Hardware cannot be null!");
        if (serverContext == null) throw new IllegalArgumentException("Server context cannot be null!");
        this.hardware = hardware;
        this.serverContext = serverContext;
        channelTextFields = new ArrayList<JTextField>(hardware.getNumChannels());
    }

    /**
	 * @see jhomenet.ui.panel.CustomPanel#getPanelName()
	 */
    public String getPanelName() {
        return "Info";
    }

    /**
	 * @see jhomenet.ui.panel.AbstractPanel#getHeaderPanel()
	 */
    @Override
    protected JPanel getHeaderPanel() {
        String iconFilename = IconFactory.getIconFilename(this.hardware.getHardwareClassname()).trim();
        return PanelFactory.buildHeader(title, description, iconFilename);
    }

    /**
	 * Initialize the GUI components.
	 */
    private void initComponents() {
        hardwareAddr_tf = new JTextField();
        hardwareAddr_tf.setEditable(false);
        hardwareAddr_tf.setText(hardware.getHardwareAddr());
        hardwareDesc_tf = new JTextField();
        hardwareDesc_tf.setEditable(false);
        hardwareDesc_tf.setText(hardware.getAppHardwareDescription());
        setupDesc_tf = new JTextField();
        setupDesc_tf.setText(hardware.getHardwareSetupDescription());
        classname_tf = new JTextField();
        classname_tf.setEditable(false);
        classname_tf.setText(hardware.getHardwareClassname());
        numChannels_tf = new JTextField();
        numChannels_tf.setEditable(false);
        numChannels_tf.setText(hardware.getNumChannels().toString());
        channelEditting_cp = new JXCollapsiblePane();
        channelEditting_b = new JButton(channelEditting_cp.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        channelEditting_b.setText("Expand");
        channelEditting_cp.add("Center", buildChannelEdittingPanel());
        channelEditting_b.addActionListener(new ActionListener() {

            /**
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
            public void actionPerformed(ActionEvent event) {
                System.out.println("Button action");
                if (channelEditting_cp.isCollapsed()) channelEditting_b.setText("Hide"); else channelEditting_b.setText("Expand");
            }
        });
        channelEditting_cp.setCollapsed(true);
        pollingInterval_l = new JLabel("Polling interval:");
        pollingInterval_cb = new JComboBox();
        if (hardware instanceof Sensor) {
            PollingIntervals[] pollingIntervals = PollingIntervals.values();
            for (int i = 0; i < pollingIntervals.length; i++) pollingInterval_cb.addItem(pollingIntervals[i]);
            pollingInterval_cb.setSelectedItem(((Sensor) hardware).getPollingInterval());
        } else {
            pollingInterval_l.setEnabled(false);
            pollingInterval_cb.setEnabled(false);
        }
        preferredUnit_l = new JLabel("Preferred unit:");
        preferredUnit_cb = new JComboBox();
        if (hardware instanceof ValueSensor) {
            for (Unit unit : ((ValueSensor) hardware).getAvailableUnits()) preferredUnit_cb.addItem(unit);
            preferredUnit_cb.setSelectedItem(((ValueSensor) hardware).getPreferredDataUnit());
        } else {
            preferredUnit_l.setEnabled(false);
            preferredUnit_cb.setEnabled(false);
        }
    }

    /**
	 * 
	 * @return
	 */
    private JComponent buildChannelEdittingPanel() {
        StringBuffer rowDef = new StringBuffer();
        JTextField temp_tf;
        for (int channel = 0; channel < hardware.getNumChannels(); channel++) {
            if (channel == 0) rowDef.append("4dlu, pref"); else rowDef.append(", 4dlu, pref");
            temp_tf = new JTextField(hardware.getChannelDescription(channel));
            channelTextFields.add(temp_tf);
        }
        rowDef.append(", 4dlu");
        FormLayout panelLayout = new FormLayout("4dlu, pref, 4dlu, fill:default:grow, 4dlu", rowDef.toString());
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout);
        for (int channel = 0; channel < hardware.getNumChannels(); channel++) {
            builder.addLabel("Channel " + channel + ":", cc.xy(2, channel * 2 + 2));
            builder.add(channelTextFields.get(channel), cc.xy(4, channel * 2 + 2));
        }
        JPanel panel = builder.getPanel();
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Channel descriptions"));
        return panel;
    }

    /**
	 * @see jhomenet.ui.panel.CustomPanel#buildPanel()
	 */
    protected final BackgroundPanel buildPanelImpl() {
        logger.debug("Building panel model");
        initComponents();
        BackgroundPanel panel = new BackgroundPanel();
        FormLayout panelLayout = new FormLayout("4dlu, right:pref, 4dlu, 100dlu, 4dlu, fill:default:grow, 4dlu", "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.addLabel("Address:", cc.xy(2, 2));
        builder.add(hardwareAddr_tf, cc.xyw(4, 2, 3));
        builder.addLabel("Type:", cc.xy(2, 4));
        builder.add(hardwareDesc_tf, cc.xyw(4, 4, 3));
        builder.addLabel("Description:", cc.xy(2, 6));
        builder.add(setupDesc_tf, cc.xyw(4, 6, 3));
        builder.addLabel("Classname:", cc.xy(2, 8));
        builder.add(classname_tf, cc.xyw(4, 8, 3));
        builder.addLabel("# of channels:", cc.xy(2, 10));
        builder.add(numChannels_tf, cc.xy(4, 10));
        builder.add(channelEditting_b, cc.xy(6, 10));
        builder.add(channelEditting_cp, cc.xyw(4, 12, 3));
        builder.add(pollingInterval_l, cc.xy(2, 14));
        builder.add(pollingInterval_cb, cc.xyw(4, 14, 3));
        builder.add(preferredUnit_l, cc.xy(2, 16));
        builder.add(preferredUnit_cb, cc.xyw(4, 16, 3));
        builder.add(ButtonBarFactory.buildRightAlignedBar(new JButton(applyAction)), cc.xyw(2, 18, 5));
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
	 * @see jhomenet.ui.panel.AbstractEditorPanel#buttonClicked(java.lang.String)
	 */
    @Override
    protected void buttonClicked(String buttonId) {
        if (buttonId.equals(applyButtonId)) {
            hardware.setHardwareSetupDescription(this.setupDesc_tf.getText());
            if (hardware instanceof Sensor) ((Sensor) hardware).setPollingInterval((PollingIntervals) pollingInterval_cb.getSelectedItem());
            if (hardware instanceof ValueSensor) ((ValueSensor) hardware).setPreferredDataUnit((Unit) preferredUnit_cb.getSelectedItem());
            for (int channel = 0; channel < hardware.getNumChannels(); channel++) hardware.setChannelDescription(channel, channelTextFields.get(channel).getText());
        }
    }

    /**
	 * The default "Apply" action class.
	 */
    private final class ApplyAction extends AbstractAction {

        private ApplyAction() {
            putValue(Action.NAME, "Apply");
        }

        /**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
        public void actionPerformed(ActionEvent actionEvent) {
            buttonClicked(applyButtonId);
        }
    }

    /**
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
    }
}
