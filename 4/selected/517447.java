package net.sourceforge.olduvai.lrac.ui.templatemanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.jglchartutil.GLChart;
import net.sourceforge.jglchartutil.StipplePattern;
import net.sourceforge.jglchartutil.chartlab.ChartLab;
import net.sourceforge.jglchartutil.chartlab.ChartLabClosingEvent;
import net.sourceforge.jglchartutil.chartlab.ChartLabClosingListener;
import net.sourceforge.jglchartutil.chartlab.ChartTestData;
import net.sourceforge.jglchartutil.datamodels.AbstractDrawableDataSeries;
import net.sourceforge.jglchartutil.datamodels.DataSeries2D;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.Strip;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Crayon;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Representation;
import net.sourceforge.olduvai.lrac.drawer.structure.templates.Template;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.util.IntegerEditor;
import net.sourceforge.olduvai.lrac.util.JComponentCellEditor;
import net.sourceforge.olduvai.lrac.util.JComponentCellRenderer;
import org.xml.sax.SAXException;

/**
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class TemplateEditorPanel extends JPanel {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2548287765219577871L;

    private static final String[] COLNAMES = { "Representation", "Width", "Height", "Crayons" };

    static final double[] timeArray = ChartTestData.genTimestamps(ChartTestData.getDefaultDataPoints(GLChart.BARCHART), ChartTestData.DEFAULTBEGINTIME, ChartTestData.DEFAULTENDTIME);

    static final String TEMPLATEEDITORPANENAME = "Template editor";

    protected static final String CHANNEL_IN_USE_TITLE = "Error: This channel is currently being used by one or more Strips.  Use the Strip Manager to remove this channel or template from use and try again.";

    protected static final String CHANNEL_IN_USE_MSG = "Error: Channel in use by one or more strips";

    static final float FONTTITLESIZE = 16;

    TemplateManager templateManager;

    Template currentTemplate;

    JLabel templateNameLabel = new JLabel("Template name");

    JTextField templateName = new JTextField();

    JTable repTable = new JTable();

    JButton addRep = new JButton("Add representation");

    JButton delRep = new JButton("Delete representation");

    CrayonPanel crayonPanel;

    SwatchColorPicker swatchPicker;

    /**
	 * Linked to CrayonPanel.metaSeriesList & model
	 * @see CrayonPanel#completeChannelList 
	 */
    ArrayList<AbstractDrawableDataSeries> sampleSeriesList = new ArrayList<AbstractDrawableDataSeries>();

    DefaultListModel crayonListModel = new DefaultListModel();

    JList crayonList = new JList(crayonListModel);

    RepresentationTableModel repModel = new RepresentationTableModel();

    TableModelListener tableModelListener = new TableModelListener() {

        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE) {
                repModel.removeTableModelListener(this);
                repModel.sortRows();
                repModel.addTableModelListener(this);
            }
        }
    };

    /**
	 * 
	 */
    public TemplateEditorPanel(TemplateManager bb) {
        this.templateManager = bb;
        setName(TEMPLATEEDITORPANENAME);
        String layoutConstraints = "wrap, fill";
        String colConstraints = "[right][left, grow]";
        String rowConstraints = "[][][][][][][fill,grow][]";
        MigLayout layout = new MigLayout(layoutConstraints, colConstraints, rowConstraints);
        setLayout(layout);
        setupWidgets();
        TemplateManager.addSeparator(this, "Template properties");
        add(templateNameLabel);
        add(templateName, "growx, span, wrap");
        swatchPicker = new SwatchColorPicker(currentTemplate);
        add(new JLabel("Threshold colors"));
        add(swatchPicker, "growx, span, wrap");
        TemplateManager.addSeparator(this, "Crayon slots by priority");
        crayonPanel = new CrayonPanel();
        add(crayonPanel, "span, growx, wrap");
        TemplateManager.addSeparator(this, "Representations");
        JScrollPane tableScroll = new JScrollPane(repTable);
        add(tableScroll, "span, growx, wrap");
        add(addRep, "left");
        add(delRep);
    }

    static final DataSeries2D createDataSeries(Crayon smd) {
        double[] dataArray = ChartTestData.genRandomData(ChartTestData.getDefaultDataPoints(GLChart.BARCHART), ChartTestData.DEFAULTMIN, ChartTestData.DEFAULTMAX);
        DataSeries2D newSeries = new DataSeries2D(timeArray, dataArray);
        newSeries.setCrayonColor(smd.getColor());
        newSeries.setLabel(smd.getName());
        newSeries.setStipplePattern(smd.getStipplePattern());
        newSeries.setShadeUnder(smd.isShadeUnder());
        newSeries.setShadeOpacity(smd.getShadeOpacity());
        return newSeries;
    }

    /**
	 * Initial widget setup
	 *
	 */
    private void setupWidgets() {
        delRep.setEnabled(false);
        templateName.addKeyListener(new KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                currentTemplate.setTemplateName(templateName.getText());
                templateManager.templateJList.repaint();
            }
        });
        addRep.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Representation newRep = currentTemplate.createRep();
                RepresentationRow r = new RepresentationRow(newRep, currentTemplate.getCrayons(), getEditorPanel());
                repModel.addRow(r);
                delRep.setEnabled(true);
            }
        });
        delRep.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                RepresentationRow r = repModel.delRow(repTable.getSelectedRow());
                currentTemplate.removeRep(r.rep);
                if (repModel.getRowCount() == 0) delRep.setEnabled(false);
            }
        });
        repTable.setBackground(Color.WHITE);
        repTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repTable.setModel(repModel);
        repTable.setDefaultRenderer(JComponent.class, new JComponentCellRenderer());
        repTable.setDefaultEditor(JComponent.class, new JComponentCellEditor());
        repTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        repTable.getColumnModel().getColumn(0).setCellEditor(new GraphicCellEditor());
        repTable.getColumnModel().getColumn(1).setPreferredWidth(10);
        repTable.getColumnModel().getColumn(1).setCellEditor(new IntegerEditor(1, Integer.MAX_VALUE));
        repTable.getColumnModel().getColumn(2).setPreferredWidth(10);
        repTable.getColumnModel().getColumn(2).setCellEditor(new IntegerEditor(1, Integer.MAX_VALUE));
        repModel.addTableModelListener(tableModelListener);
    }

    private void clearComponents() {
        templateName.setText("");
        repModel.clear();
    }

    Crayon getCurrentCrayon() {
        return (Crayon) crayonList.getSelectedValue();
    }

    int getCrayonIndex(Crayon smd) {
        return crayonListModel.indexOf(smd);
    }

    /**
	 * Retrieves the dataSeries object associated with 
	 * @return
	 */
    AbstractDrawableDataSeries getDataSeries(Crayon smd) {
        final int index = getCrayonIndex(smd);
        if (index == -1) return null;
        return sampleSeriesList.get(index);
    }

    void removeDataSeries(Crayon smd) {
        final int index = getCrayonIndex(smd);
        if (index == -1) {
            System.out.println("TemplateEditorPanel: Could not find crayon");
            return;
        }
        sampleSeriesList.remove(index);
    }

    /**
	 * Configures the widgets for the parameter of this template
	 * 
	 * @param template A LiveRAC template object
	 */
    protected void configWidgets(Template template) {
        if (template == null) {
            clearComponents();
            enableAllComponents(false);
            return;
        }
        if (template == currentTemplate) return;
        this.currentTemplate = template;
        String templateName = template.getTemplateName();
        this.templateName.setText(templateName);
        swatchPicker.setTemplate(template);
        crayonListModel.clear();
        sampleSeriesList.clear();
        List<Crayon> dl = template.getCrayons();
        Iterator<Crayon> smit = dl.iterator();
        Crayon smd;
        while (smit.hasNext()) {
            smd = smit.next();
            crayonListModel.addElement(smd);
            sampleSeriesList.add(createDataSeries(smd));
        }
        repModel.clear();
        List<Representation> repLevels = template.getRepLevels();
        Iterator<Representation> it = repLevels.iterator();
        Representation rep;
        while (it.hasNext()) {
            rep = it.next();
            RepresentationRow rr = new RepresentationRow(rep, dl, this);
            repModel.addRow(rr);
        }
        templateManager.pack();
    }

    class RepresentationTableModel extends AbstractTableModel {

        final int colCount = COLNAMES.length;

        final int tableMargin = 2;

        ArrayList<RepresentationRow> mData = new ArrayList<RepresentationRow>();

        /**
		 * 
		 */
        private static final long serialVersionUID = -7625045508537451886L;

        public String getColumnName(int col) {
            if (col >= colCount) {
                System.err.println("RepresentationTableModel: Invalid column index");
                return ("INVALID");
            }
            return COLNAMES[col];
        }

        /**
		 * Clear all rows for this model
		 *
		 */
        public void clear() {
            for (int i = getRowCount(); i > 0; i--) {
                delRow(i - 1);
            }
        }

        public int getColumnCount() {
            return colCount;
        }

        public int getRowCount() {
            return mData.size();
        }

        public boolean isCellEditable(int row, int col) {
            if (col < colCount) {
                return true;
            } else {
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        public Class getColumnClass(int c) {
            Object v = getValueAt(0, c);
            if (v == null) return null;
            return v.getClass();
        }

        /**
		 * Rebuilds all of the chart icons
		 *
		 */
        public void rebuildIcons() {
            for (int i = 0; i < getRowCount(); i++) {
                getRow(i).createIcon();
                fireTableCellUpdated(i, 0);
            }
        }

        public Object getValueAt(int row, int col) {
            RepresentationRow r = mData.get(row);
            return r.getColumn(col);
        }

        public void setValueAt(Object value, int row, int col) {
            RepresentationRow r = mData.get(row);
            r.setColValue(value, col);
            fireTableCellUpdated(row, col);
        }

        public RepresentationRow getRow(int row) {
            return mData.get(row);
        }

        public void tableModified() {
            fireTableDataChanged();
            for (int i = 0; i < getRowCount(); i++) {
                repTable.setRowHeight(i, PackableTable.getPreferredRowHeight(repTable, i, tableMargin));
            }
            repTable.repaint();
        }

        public void addRow(RepresentationRow r) {
            Iterator<RepresentationRow> it = mData.iterator();
            if (mData.size() == 0) {
                mData.add(r);
                fireTableRowsInserted(0, 0);
                repTable.setRowHeight(0, PackableTable.getPreferredRowHeight(repTable, 0, tableMargin));
                return;
            }
            int rowPos = 0;
            RepresentationRow cr;
            while (it.hasNext()) {
                cr = it.next();
                if (r.compareTo(cr) == -1 || r.compareTo(cr) == 0) break;
                rowPos++;
            }
            if (rowPos == mData.size()) mData.add(r); else mData.add(rowPos, r);
            fireTableRowsInserted(rowPos, rowPos);
            repTable.setRowHeight(rowPos, PackableTable.getPreferredRowHeight(repTable, rowPos, tableMargin));
        }

        public RepresentationRow delRow(int rowIndex) {
            RepresentationRow r = mData.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
            return r;
        }

        public Representation getRowRep(int rowIndex) {
            return mData.get(rowIndex).getRep();
        }

        public void sortRows() {
            if (getRowCount() == 0) return;
            Collections.sort(mData);
            fireTableRowsUpdated(0, getRowCount());
        }
    }

    class GraphicCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

        /**
		 * 
		 */
        private static final long serialVersionUID = -6910359476195442535L;

        JButton button;

        protected static final String EDIT = "edit";

        public GraphicCellEditor() {
            button = new JButton();
            button.setActionCommand(EDIT);
            button.addActionListener(this);
            button.setBorderPainted(true);
            button.setBackground(null);
            button.setForeground(null);
        }

        public void actionPerformed(ActionEvent e) {
            if (EDIT.equals(e.getActionCommand())) {
                fireEditingStopped();
            }
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            RepresentationRow rr = repModel.getRow(row);
            Representation rep = rr.getRep();
            ChartLab lab = new ChartLabSingleton(rep, rr.getChart());
            templateManager.disableAllComponents();
            lab.addChartLabClosingListener(new RowAwareChartLabClosingListener(row));
            return button;
        }
    }

    private TemplateEditorPanel getEditorPanel() {
        return this;
    }

    public void enableAllComponents(boolean b) {
        Component[] cs = this.getComponents();
        for (int i = 0; i < cs.length; i++) {
            cs[i].setEnabled(b);
        }
        if (repTable.getRowCount() == 0) delRep.setEnabled(false);
        crayonPanel.enableAllComponents(b);
    }

    class RowAwareChartLabClosingListener implements ChartLabClosingListener {

        int row = 0;

        public RowAwareChartLabClosingListener(int row) {
            this.row = row;
        }

        public void chartLabClosing(ChartLabClosingEvent e) {
            System.out.println("Chart Lab closing!");
            GLChart[] charts = e.getFinalCharts();
            if (charts == null || charts.length == 0) return;
            String chartXML = null;
            try {
                chartXML = GLChart.getXMLSettings(charts[0]);
            } catch (SAXException e1) {
                System.err.println("TemplateEditorPanel: SAX Exception");
                e1.printStackTrace();
            } catch (Exception e1) {
                System.err.println("TemplateEditorPanel: Exception");
                e1.printStackTrace();
            }
            Representation r = repModel.getRowRep(row);
            final int chartType = charts[0].getChartType();
            r.setChartXML(chartXML);
            r.setChartType(chartType);
            repModel.getRow(row).createChart();
            repModel.getRow(row).createIcon();
            templateManager.enableAllComponents();
        }
    }

    class CrayonPanel extends JPanel {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        JButton addButton = new JButton("Add");

        JButton delButton = new JButton("Delete");

        JCheckBox setShadeUnder = new JCheckBox();

        JLabel setShadeOpacityLabel = new JLabel("Shade opacity");

        static final double SHADEOPACITYMIN = 0d;

        static final double SHADEOPACITYMAX = 1d;

        static final double SHADEOPACITYSTEP = .05;

        static final double SHADEOPACITYDEFAULT = .5d;

        JSpinner setShadeOpacity = new JSpinner(new SpinnerNumberModel(SHADEOPACITYDEFAULT, SHADEOPACITYMIN, SHADEOPACITYMAX, SHADEOPACITYSTEP));

        JLabel setColorLabel = new JLabel("Series color");

        JButton setColor = new JButton();

        JLabel setStipplePatternLabel = new JLabel("Stipple pattern");

        JComboBox setStipplePattern = new JComboBox();

        boolean listen = true;

        public void enableAllComponents(Boolean b) {
            Component[] clist = this.getComponents();
            for (int i = 0; i < clist.length; i++) {
                clist[0].setEnabled(b);
            }
            addButton.setEnabled(b);
            delButton.setEnabled(b);
            crayonList.setEnabled(b);
            enableSeriesComponents(b);
        }

        public CrayonPanel() {
            final String layoutConstraints = "wrap, fill";
            final String colConstraints = "[right][fill,sizegroup]unrel[right][fill,sizegroup]";
            final String rowConstraints = "[][][][]";
            MigLayout layout = new MigLayout(layoutConstraints, colConstraints, rowConstraints);
            setLayout(layout);
            setupComponents();
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new MigLayout(layoutConstraints, "[][]", "[][]"));
            leftPanel.add(new JScrollPane(crayonList), "grow, span, wrap");
            leftPanel.add(addButton, "growx");
            leftPanel.add(delButton, "growx");
            add(leftPanel, "dock west, wmin 150, hmin 150, gapright 10px");
            TemplateManager.addSeparator(this, "Crayon settings");
            add(setColorLabel);
            add(setColor);
            add(setStipplePatternLabel);
            add(setStipplePattern, "wrap");
            add(setShadeUnder, "span 2");
            add(setShadeOpacityLabel);
            add(setShadeOpacity, "wrap");
        }

        private void enableSeriesComponents(Boolean b) {
            if (getCurrentCrayon() == null) {
                b = false;
            }
            setColor.setEnabled(b);
            setStipplePattern.setEnabled(b);
            setShadeUnder.setEnabled(b);
            setShadeOpacity.setEnabled(b);
        }

        void configWidgets(Crayon s) {
            if (s == null) {
                enableSeriesComponents(false);
                return;
            }
            listen = false;
            enableSeriesComponents(true);
            setColor.setBackground(s.getColor());
            setShadeUnder.setSelected(s.isShadeUnder());
            setShadeOpacity.setValue((double) s.getShadeOpacity());
            StipplePattern p = s.getStipplePattern();
            if (p == null) p = StipplePattern.solidPattern();
            setStipplePattern.setSelectedItem(StipplePattern.LINESTIPPLETYPES[p.getStippleType()]);
            listen = true;
        }

        private void setupComponents() {
            setShadeUnder.setText("Shade under");
            crayonList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            for (int i = 0; i < StipplePattern.LINESTIPPLETYPES.length; i++) {
                setStipplePattern.addItem(StipplePattern.LINESTIPPLETYPES[i]);
            }
            crayonList.addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    configWidgets((Crayon) crayonList.getSelectedValue());
                }
            });
            addButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String seriesName = "Slot " + crayonListModel.getSize();
                    Crayon smd = currentTemplate.createCrayon(seriesName);
                    crayonListModel.addElement(smd);
                    sampleSeriesList.add(createDataSeries(smd));
                    for (int i = 0; i < repModel.getRowCount(); i++) {
                        repModel.getRow(i).addCrayon(smd);
                    }
                    repTable.repaint();
                }
            });
            delButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final Crayon selectedCrayon = (Crayon) crayonList.getSelectedValue();
                    if (selectedCrayon == null) return;
                    final int crayonIndex = crayonList.getSelectedIndex();
                    if (templateManager.getDataGrid() != null) {
                        List<Strip> strips = templateManager.getDataGrid().getStripList();
                        for (Iterator<Strip> it = strips.iterator(); it.hasNext(); ) {
                            final Strip s = it.next();
                            InputChannelItemInterface channel = s.getChannel(crayonIndex);
                            if (channel != null) {
                                JOptionPane.showMessageDialog(templateManager, CHANNEL_IN_USE_MSG, CHANNEL_IN_USE_TITLE, JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    }
                    for (int i = 0; i < repModel.getRowCount(); i++) {
                        repModel.getRow(i).delCrayon(selectedCrayon);
                    }
                    removeDataSeries(selectedCrayon);
                    crayonListModel.removeElement(selectedCrayon);
                    currentTemplate.removeCrayon(selectedCrayon);
                    repTable.repaint();
                }
            });
            setColor.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (listen == false) return;
                    JColorChooser chooser = new JColorChooser();
                    JColorChooser.createDialog(templateManager, setColorLabel.getText(), true, chooser, new ColorActionListener(chooser) {

                        public void actionPerformed(ActionEvent e) {
                            setColor.setBackground(this.chooser.getColor());
                            Crayon smd = getCurrentCrayon();
                            smd.setColor(this.chooser.getColor());
                            getDataSeries(smd).setCrayonColor(this.chooser.getColor());
                            repModel.rebuildIcons();
                        }
                    }, null).setVisible(true);
                }
            });
            setShadeUnder.addItemListener(new ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    if (listen == false) return;
                    Crayon smd = getCurrentCrayon();
                    smd.setShadeUnder(setShadeUnder.isSelected());
                    getDataSeries(smd).setShadeUnder(setShadeUnder.isSelected());
                    repModel.rebuildIcons();
                }
            });
            setShadeOpacity.getModel().addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    if (listen == false) return;
                    Crayon smd = getCurrentCrayon();
                    smd.setShadeOpacity(((Double) setShadeOpacity.getValue()).floatValue());
                    getDataSeries(smd).setShadeOpacity(((Double) setShadeOpacity.getValue()).floatValue());
                    repModel.rebuildIcons();
                }
            });
            setStipplePattern.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (listen == false) return;
                    Crayon smd = getCurrentCrayon();
                    StipplePattern p = StipplePattern.getStippleByName((String) setStipplePattern.getSelectedItem());
                    smd.setStipplePattern(p);
                    getDataSeries(smd).setStipplePattern(p);
                    repModel.rebuildIcons();
                }
            });
        }
    }

    /**
	 * A simple class that defines some needed functionality for the color chooser
	 * @author Peter McLachlan <spark343@cs.ubc.ca>
	 *
	 */
    public abstract class ColorActionListener implements ActionListener {

        protected JColorChooser chooser;

        public ColorActionListener(JColorChooser chooser) {
            this.chooser = chooser;
        }
    }
}
