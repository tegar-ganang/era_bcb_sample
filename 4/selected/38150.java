package jhomenet.ui.panel;

import java.util.List;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.HardwareListener;
import jhomenet.commons.hw.HomenetHardware;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.data.AbstractHardwareData;
import jhomenet.ui.action.PollHardwareAction;
import jhomenet.ui.table.model.AbstractDataTableRow;
import jhomenet.ui.table.model.AbstractTableModel;

/**
 * An abstract panel for building data panels.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class AbstractDataPanel<T extends AbstractHardwareData, S extends AbstractDataTableRow> extends AbstractPanel implements HardwareListener {

    /**
     * Define the logging object.
     */
    protected static Logger logger = Logger.getLogger(AbstractDataPanel.class);

    /**
     * Reference to the hardware object.
     */
    private final HomenetHardware hardware;

    /**
     * Reference to the application context.
     */
    private final GeneralApplicationContext serverContext;

    /**
     * Current data text field.
     */
    private final List<JTextField> currentDataTextfields;

    /**
     * The historical data table.
     */
    private JXTable historicalData_t;

    /**
     * The historical data scroll pane.
     */
    private JScrollPane historicalData_sp;

    /**
     * Constructor.
     * 
     * @param hardware Reference to a jHomeNet hardware object
     */
    public AbstractDataPanel(HomenetHardware hardware, GeneralApplicationContext serverContext) {
        super();
        if (hardware == null) throw new IllegalArgumentException("Hardware cannot be null!");
        if (serverContext == null) throw new IllegalArgumentException("Application context cannot be null!");
        this.hardware = hardware;
        this.serverContext = serverContext;
        this.currentDataTextfields = new ArrayList<JTextField>(this.hardware.getNumChannels());
    }

    /**
     * @return the hardware
     */
    final HomenetHardware getHardware() {
        return hardware;
    }

    /**
     * Initialize the GUI components.
     */
    private void initComponents() {
        Highlighter highlighter = HighlighterFactory.createSimpleStriping(HighlighterFactory.QUICKSILVER);
        for (int channel = 0; channel < this.getHardware().getNumChannels(); channel++) {
            this.currentDataTextfields.add(channel, new JTextField());
            this.currentDataTextfields.get(channel).setToolTipText("Current hardware data (channel " + channel + ")");
        }
        historicalData_t = new JXTable();
        historicalData_t.setModel(this.getHistoricalDataTableModel());
        historicalData_t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historicalData_t.setHighlighters(highlighter);
        historicalData_t.setShowHorizontalLines(true);
        historicalData_sp = new JScrollPane();
        historicalData_sp.setViewportView(historicalData_t);
    }

    /**
     * @see jhomenet.ui.panel.CustomPanel#getPanelName()
     */
    public final String getPanelName() {
        return "Data";
    }

    /**
     * @see jhomenet.ui.panel.CustomPanel#buildPanel()
     */
    public final BackgroundPanel buildPanelImpl() {
        initComponents();
        this.hardware.addHardwareListener(this);
        FormLayout panelLayout = new FormLayout("4dlu, fill:default:grow, 4dlu", "4dlu, pref, 4dlu, fill:default:grow, 4dlu");
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.add(getCurrentDataPanel(), cc.xy(2, 2));
        builder.add(getHistoricalDataPanel(), cc.xy(2, 4));
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
     * 
     * @return
     */
    private BackgroundPanel getCurrentDataPanel() {
        StringBuffer rowSpec = new StringBuffer();
        int numChannels = this.getHardware().getNumChannels();
        for (int channel = 0; channel < numChannels; channel++) {
            if (channel == (numChannels - 1)) rowSpec.append("pref"); else rowSpec.append("pref, 4dlu, ");
        }
        FormLayout panelLayout = new FormLayout("right:pref, 4dlu, fill:pref:grow, 4dlu, pref", rowSpec.toString());
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        JXButton b = null;
        for (int channel = 0, row = 1; channel < numChannels; channel++, row += 2) {
            builder.addLabel("Current data [CH-" + channel + "]: ", cc.xy(1, row));
            builder.add(this.currentDataTextfields.get(channel), cc.xy(3, row));
            b = new JXButton("Poll");
            b.addActionListener(new ActionListener() {

                /**
                 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
                 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    new PollHardwareAction(hardware.getHardwareAddr(), serverContext).run();
                }
            });
            builder.add(b, cc.xy(5, row));
        }
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
     * 
     * @return
     */
    private BackgroundPanel getHistoricalDataPanel() {
        FormLayout panelLayout = new FormLayout("fill:default:grow", "fill:default:grow");
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.add(historicalData_sp, cc.xy(1, 1));
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
     * Add data to the data panel.
     * 
     * @param data
     */
    protected final void addHistoricalData(T data) {
        final S tableRow = getHistoricalDataTableRow(data);
        if (SwingUtilities.isEventDispatchThread()) updateOnEDT(tableRow); else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    updateOnEDT(tableRow);
                }
            });
        }
    }

    /**
     * 
     * @param tableRow
     */
    private void updateOnEDT(S tableRow) {
        getHistoricalDataTableModel().addRow(tableRow);
        this.currentDataTextfields.get(tableRow.getChannel()).setText(tableRow.toString());
        this.panelGroupManager.updatePanels();
    }

    /**
     * Get a data table row implementation given the hardware data.
     * 
     * @param data
     * @return
     */
    protected abstract S getHistoricalDataTableRow(T data);

    /**
     * Get the hardware data table model.
     * 
     * @return
     */
    protected abstract AbstractTableModel<S> getHistoricalDataTableModel();
}
