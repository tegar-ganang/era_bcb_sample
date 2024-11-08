package net.sourceforge.olduvai.lrac.ui.stripmanager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.olduvai.lrac.DataGrid;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.DataAggregator;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.StripChannelBinding;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Crayon;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.ui.templatemanager.TemplateManager;
import net.sourceforge.olduvai.lrac.util.ListBackedComboBoxModel;
import net.sourceforge.olduvai.lrac.util.ListBackedListModel;
import net.sourceforge.olduvai.lrac.util.ReorderableJList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventListModel;

/**
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class StripManager extends JFrame {

    static final String TITLE = "Strip Manager";

    static final Double MAXSIZE = 10e7;

    static final Double MINSIZE = -10e7;

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static final Color BORDERCOLOR = Color.DARK_GRAY;

    DataGrid datagrid;

    Strip selectedStrip = null;

    StripChannelBinding selectedStripChannelBinding = null;

    Template selectedTemplate = null;

    EventList<InputChannelItemInterface> channelList;

    ListBackedListModel<Strip> stripListModel;

    ReorderableJList stripList;

    JButton addStripButton = new JButton("Add strip");

    JButton delStripButton = new JButton("Delete strip");

    JTextField stripTitle = new JTextField();

    JCheckBox activeCheck = new JCheckBox();

    JComboBox swatchSigFunctionBox;

    JComboBox templateBox = new JComboBox();

    DefaultListModel boundChannelListModel = new DefaultListModel();

    JList boundChannelList = new JList(boundChannelListModel);

    boolean allChannelListDisable = false;

    JButton connectButton = new JButton("Connect");

    JButton disconnectButton = new JButton("Disconnect");

    StripSliderPanel sliderPanel = new StripSliderPanel(null);

    JTextField channelLabelText = new JTextField();

    FilterList<InputChannelItemInterface> filteredChannelList;

    JList channelJList;

    JTextField searchField = new JTextField(8);

    JDialog channelConfigDialog;

    JDialog dataAggregationDialog;

    JButton dataAggregationButton;

    /**
	 * A panel to which aggregation panels get added & removed
	 */
    AggregatorPanel aggPanel;

    public StripManager(DataGrid dg, List<Strip> strips, List<Template> templates, List<InputChannelItemInterface> channels) {
        setTitle(TITLE);
        stripListModel = new ListBackedListModel<Strip>(strips);
        this.stripList = new ReorderableJList(stripListModel);
        setupWidgets(strips, templates, channels);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.datagrid = dg;
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem quit = new JMenuItem("Quit", KeyEvent.VK_Q);
        quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
        menu.add(quit);
        menubar.add(menu);
        setJMenuBar(menubar);
        final String layoutConstraints = "wrap, fill";
        final String stripColConstraints = "[left]";
        final String stripRowConstraints = "[][grow, fill][][]";
        JPanel stripPanel = new JPanel(new MigLayout(layoutConstraints, stripColConstraints, stripRowConstraints));
        JScrollPane stripListScroll = new JScrollPane(stripList);
        stripPanel.add(new JLabel("Select strip"), "wrap");
        stripPanel.add(stripListScroll, "wrap, grow");
        stripPanel.add(addStripButton, "wrap, growx");
        stripPanel.add(delStripButton, "wrap, growx");
        final String channelSelectColConstraints = "[right][left]";
        final String channelSelectRowConstraints = "[][][][][][][][][grow][]";
        JPanel channelSelectPanel = new JPanel(new MigLayout(layoutConstraints, channelSelectColConstraints, channelSelectRowConstraints));
        TemplateManager.addSeparator(channelSelectPanel, "Strip configuration");
        JScrollPane stripChannelScroll = new JScrollPane(boundChannelList);
        stripChannelScroll.setMinimumSize(new Dimension(200, 100));
        JScrollPane allChannelScroll = new JScrollPane(channelJList);
        allChannelScroll.setAutoscrolls(true);
        channelSelectPanel.add(new JLabel("Strip title"), "right");
        channelSelectPanel.add(stripTitle, "wrap, left, growx");
        channelSelectPanel.add(new JLabel("Active"), "right");
        channelSelectPanel.add(activeCheck, "left, growx, wrap");
        channelSelectPanel.add(new JLabel("Template"), "right");
        channelSelectPanel.add(templateBox, "wrap, left, growx");
        channelSelectPanel.add(new JLabel("Swatch function"), "right");
        channelSelectPanel.add(swatchSigFunctionBox, "wrap, left, growx");
        dataAggregationButton = new JButton("Data aggregation");
        dataAggregationButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dataAggregationDialog.setVisible(true);
            }
        });
        TemplateManager.addSeparator(channelSelectPanel, "Threshold values");
        JScrollPane scrollable = new JScrollPane(sliderPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollable.setMinimumSize(new Dimension(500, 250));
        channelSelectPanel.add(scrollable, "grow, span");
        TemplateManager.addSeparator(channelSelectPanel, "Link template slots");
        channelSelectPanel.add(stripChannelScroll, "wrap, span, grow");
        channelSelectPanel.add(connectButton, "left");
        channelSelectPanel.add(disconnectButton, "left, wrap");
        final String channelConfigColConstraints = "[][grow]";
        final String channelConfigRowConstraints = "[][][][grow][][][]";
        channelConfigDialog = new JDialog(this, "Channel configuration", true);
        ;
        channelConfigDialog.setLayout(new MigLayout(layoutConstraints, channelConfigColConstraints, channelConfigRowConstraints));
        TemplateManager.addSeparator(channelConfigDialog, "Channel configuration");
        channelConfigDialog.add(new JLabel("Channel label"), "right");
        channelConfigDialog.add(channelLabelText, "left, wrap, grow");
        channelConfigDialog.add(new JLabel("Select channel"), "wrap, span, left");
        channelConfigDialog.add(allChannelScroll, "grow, span, wrap");
        channelConfigDialog.add(new JLabel("Search"), "right");
        channelConfigDialog.add(searchField, "left, grow");
        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                channelConfigDialog.setVisible(false);
            }
        });
        channelConfigDialog.add(doneButton, "span, grow");
        channelConfigDialog.pack();
        JSplitPane stripSelectSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, stripPanel, channelSelectPanel);
        this.add(stripSelectSplit);
        enableStripWidgets(false);
        enableCardinalWidgets(false);
        this.validate();
        connectButton.setPreferredSize(new Dimension(connectButton.getWidth(), connectButton.getHeight()));
        this.pack();
        this.setVisible(true);
    }

    private void onClose() {
        int result = JOptionPane.showOptionDialog(this, "Save changes?", "Strip Manager: Save changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "Yes", "No", "Cancel " }, null);
        final int YES = 0;
        @SuppressWarnings("unused") final int NO = 1;
        final int CANCEL = 2;
        if (result == CANCEL) {
            return;
        } else if (result == YES) {
            saveConfig();
            if (datagrid != null) datagrid.resetDisplay();
        }
        dispose();
    }

    private void saveConfig() {
        final ListModel lm = stripList.getModel();
        List<Strip> strips = new ArrayList<Strip>(lm.getSize());
        for (int i = 0; i < lm.getSize(); i++) strips.add((Strip) lm.getElementAt(i));
        datagrid.saveStrips(strips);
    }

    private void setupWidgets(List<Strip> strips, List<Template> templates, List<InputChannelItemInterface> channels) {
        stripList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        stripList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                final Strip newselection = (Strip) stripList.getSelectedValue();
                if (newselection != selectedStrip) selectStrip(newselection);
            }
        });
        addStripButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Strip p = new Strip();
                stripListModel.add(p);
                stripList.setSelectedValue(p, true);
                stripList.repaint();
            }
        });
        delStripButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Strip p = (Strip) stripList.getSelectedValue();
                stripListModel.remove(p);
            }
        });
        stripTitle.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (selectedStrip == null) return;
                selectedStrip.setName(stripTitle.getText());
                stripList.repaint();
            }
        });
        activeCheck.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectedStrip.setActive(activeCheck.isSelected());
            }
        });
        templateBox.setModel(new ListBackedComboBoxModel<Template>(templates));
        templateBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (selectedStrip == null) return;
                final Template t = (Template) templateBox.getSelectedItem();
                selectTemplate(t);
            }
        });
        final String[] functionList = Strip.FUNCTIONNAMES;
        swatchSigFunctionBox = new JComboBox(functionList);
        swatchSigFunctionBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                final int selectedIndex = swatchSigFunctionBox.getSelectedIndex();
                if (selectedStrip == null || selectedStrip.getSigFunction() == selectedIndex) return;
                selectedStrip.setSigFunction(selectedIndex);
            }
        });
        boundChannelList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (selectedStrip == null) return;
                StripChannelBinding binding = (StripChannelBinding) boundChannelList.getSelectedValue();
                if (binding == null || binding.getBoundChannelName() == null) {
                    connectButton.setText("Connect");
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);
                    selectStripChannel(null);
                    return;
                }
                connectButton.setText("Edit");
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(true);
                selectStripChannel(binding);
                enableCardinalWidgets(true);
            }
        });
        connectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectedStrip == null) return;
                StripChannelBinding binding = (StripChannelBinding) boundChannelList.getSelectedValue();
                searchField.setText("");
                applyChannelFilters();
                selectStripChannel(binding);
                boundChannelList.repaint();
                connectButton.setText("Edit");
                disconnectButton.setEnabled(true);
                enableCardinalWidgets(true);
                channelConfigDialog.setVisible(true);
            }
        });
        disconnectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectedStrip == null) return;
                StripChannelBinding binding = (StripChannelBinding) boundChannelList.getSelectedValue();
                selectedStrip.removeChannelBinding(binding);
                selectStripChannel(null);
                boundChannelList.repaint();
                connectButton.setText("Connect");
                disconnectButton.setEnabled(false);
                enableCardinalWidgets(true);
            }
        });
        channelLabelText.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (selectedStripChannelBinding == null) return;
                boundChannelList.repaint();
                selectedStripChannelBinding.setUserLabel(channelLabelText.getText());
            }
        });
        channelList = GlazedLists.eventList(channels);
        filteredChannelList = new FilterList<InputChannelItemInterface>(channelList);
        channelJList = new JList(new EventListModel<InputChannelItemInterface>(filteredChannelList));
        channelJList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        channelJList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (selectedStripChannelBinding == null || allChannelListDisable) return;
                final int selectedIndex = channelJList.getSelectedIndex();
                InputChannelItemInterface pickedListChannel;
                if (selectedIndex >= channelJList.getModel().getSize()) {
                    pickedListChannel = null;
                } else {
                    pickedListChannel = (InputChannelItemInterface) channelJList.getSelectedValue();
                }
                if (pickedListChannel == null) {
                    selectedStripChannelBinding.bind(null);
                    return;
                }
                selectedStripChannelBinding.bind(pickedListChannel);
                channelLabelText.setText(selectedStripChannelBinding.getUserLabel());
                selectedStrip.addChannelBinding(selectedStripChannelBinding);
                repaint();
            }
        });
        searchField.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                applyChannelFilters();
            }
        });
        dataAggregationDialog = new JDialog(this, "Data aggregation", true);
        dataAggregationDialog.setPreferredSize(new Dimension(600, 400));
        dataAggregationDialog.setLayout(new MigLayout("fill", "[][]", "[][grow][]"));
        TemplateManager.addSeparator(dataAggregationDialog, "Data aggregation");
        aggPanel = new AggregatorPanel(new MigLayout("fillx, aligny top", "[]", ""));
        JScrollPane aggScrollPane = new JScrollPane(aggPanel);
        dataAggregationDialog.add(aggScrollPane, "span, grow, wrap");
        JButton addAggregator = new JButton("Add aggregator");
        addAggregator.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DataAggregator da = DataAggregator.createAggregator();
                selectedStrip.addAggregator(da);
                aggPanel.addAggregator(da);
            }
        });
        dataAggregationDialog.add(addAggregator, "span, grow");
        dataAggregationDialog.pack();
    }

    private void applyChannelFilters() {
        final String filterString = searchField.getText().toUpperCase();
        if (filterString.equals("")) {
            filteredChannelList.setMatcher(null);
        } else {
            Matcher<InputChannelItemInterface> matcher = new Matcher<InputChannelItemInterface>() {

                public boolean matches(InputChannelItemInterface channel) {
                    final String internalName = channel.getInternalName().toUpperCase();
                    final String name = channel.getName().toUpperCase();
                    if (internalName.contains(filterString) || name.contains(filterString)) return true;
                    return false;
                }
            };
            filteredChannelList.setMatcher(matcher);
        }
        channelJList.repaint();
    }

    private void selectTemplate(Template t) {
        selectedTemplate = t;
        if (t == null) {
            boundChannelListModel.clear();
            boundChannelList.setEnabled(false);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            enableSeriesWidgets(false);
            sliderPanel.setStrip(null);
            Strip tempStrip = selectedStrip;
            selectedStrip = null;
            templateBox.setSelectedIndex(-1);
            selectedStrip = tempStrip;
            return;
        }
        if (templateBox.getSelectedItem() != t) {
            ItemListener[] listeners = templateBox.getItemListeners();
            templateBox.removeItemListener(listeners[0]);
            templateBox.setSelectedItem(t);
            templateBox.addItemListener(listeners[0]);
        }
        selectedStrip.setTemplate(t);
        boundChannelListModel.removeAllElements();
        List<Crayon> crayonList = t.getCrayons();
        int count = 0;
        while (count < crayonList.size()) {
            StripChannelBinding binding = selectedStrip.getChannelBinding(count);
            boundChannelListModel.addElement(binding);
            count++;
        }
        sliderPanel.setStrip(selectedStrip);
        sliderPanel.repaint();
        enableStripWidgets(true);
        enableCardinalWidgets(true);
    }

    private void selectStrip(Strip s) {
        selectedStrip = s;
        if (s == null) {
            enableStripWidgets(false);
            selectedTemplate = null;
            sliderPanel.setStrip(null);
            return;
        }
        enableStripWidgets(true);
        enableCardinalWidgets(true);
        enableSeriesWidgets(false);
        stripTitle.setText(s.getName());
        activeCheck.setSelected(s.isActive());
        selectTemplate(s.getTemplate());
        swatchSigFunctionBox.setSelectedIndex(selectedStrip.getSigFunction());
        aggPanel.removeAll();
        for (Iterator<DataAggregator> it = selectedStrip.getDataAggregators().iterator(); it.hasNext(); ) {
            DataAggregator da = it.next();
            aggPanel.addAggregator(da);
        }
        pack();
    }

    private void selectStripChannel(StripChannelBinding stripChannel) {
        selectedStripChannelBinding = stripChannel;
        if (stripChannel == null) {
            enableSeriesWidgets(false);
            return;
        }
        channelLabelText.setText(stripChannel.getUserLabel());
        allChannelListDisable = true;
        channelJList.clearSelection();
        int foundIndex = 0;
        for (Iterator<InputChannelItemInterface> it = channelList.iterator(); it.hasNext(); ) {
            final InputChannelItemInterface sc = it.next();
            if (sc.getInternalName().equals(stripChannel.getBoundChannelName())) break;
            foundIndex++;
        }
        channelJList.setSelectedIndex(foundIndex);
        channelJList.ensureIndexIsVisible(foundIndex);
        allChannelListDisable = false;
        enableSeriesWidgets(true);
    }

    private void enableStripWidgets(boolean b) {
        if (b == false) {
            stripTitle.setText("");
            templateBox.setSelectedIndex(-1);
            boundChannelListModel.removeAllElements();
        }
        stripTitle.setEnabled(b);
        activeCheck.setEnabled(b);
        templateBox.setEnabled(b);
        swatchSigFunctionBox.setEnabled(b);
        boundChannelList.setEnabled(b);
        dataAggregationButton.setEnabled(b);
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        enableSeriesWidgets(b);
        enableCardinalWidgets(b);
    }

    private void enableCardinalWidgets(boolean b) {
        if (b == true) if (boundChannelListModel.getSize() < 1) b = false; else {
            try {
                StripChannelBinding icm = (StripChannelBinding) boundChannelListModel.firstElement();
                if (icm == null || icm.getBoundChannelName() == null) b = false;
            } catch (NoSuchElementException e) {
                b = false;
            }
        }
        sliderPanel.setEnabled(b);
    }

    private void enableSeriesWidgets(boolean b) {
        if (selectedStripChannelBinding == null) b = false;
        if (b == false) {
            channelLabelText.setText("");
            allChannelListDisable = true;
            channelJList.clearSelection();
            allChannelListDisable = false;
        }
        channelLabelText.setEnabled(b);
        channelJList.setEnabled(b);
        searchField.setEnabled(b);
    }

    abstract class AggregatorListener implements ActionListener {

        DataAggregator da;

        JPanel thisPanel;

        JButton button;

        public AggregatorListener(DataAggregator da, JPanel aggregatorPanel, JButton remAggregator) {
            this.da = da;
            this.thisPanel = aggregatorPanel;
            this.button = remAggregator;
        }
    }

    class AggregatorPanel extends JPanel {

        public AggregatorPanel(MigLayout layout) {
            super(layout);
        }

        public void addAggregator(DataAggregator da) {
            if (getComponentCount() != 0) {
                JSeparator s = new JSeparator();
                s.setForeground(Color.black);
                add(s, "wrap, growx");
            }
            JPanel newAggPanel = da.createAggregatorJPanel();
            add(newAggPanel, "wrap, growx");
            JButton remAggregator = new JButton("Remove aggregator");
            remAggregator.addActionListener(new AggregatorListener(da, newAggPanel, remAggregator) {

                public void actionPerformed(ActionEvent e) {
                    remove(thisPanel);
                    remove(button);
                    selectedStrip.removeDataAggregator(da);
                    validate();
                    dataAggregationDialog.pack();
                    dataAggregationDialog.repaint();
                }
            });
            add(remAggregator, "growx, wrap");
            validate();
            dataAggregationDialog.pack();
        }

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;
    }

    public static void main(String argv[]) {
        DataGrid grid = DataGrid.getInstance();
        JFrame mainFrame = new JFrame("MainFrame");
        mainFrame.setSize(new Dimension(100, 100));
        mainFrame.pack();
        mainFrame.setVisible(true);
        grid.connectDataInterface(mainFrame);
        new StripManager(grid, grid.loadStripList(), grid.loadTemplateList(), grid.getInputChannelItemList());
    }
}
