package com.javable.dataview;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import ucar.nc2.NetcdfFileWriteable;
import com.javable.dataview.legend.LegendTree;

/**
 * Class that contains different data views
 */
public class ViewContent extends JPanel {

    /** Specify Plot tab Id */
    public static final int PLOT_TAB = 0;

    /** Specify Table tab Id */
    public static final int TABLE_TAB = 1;

    private JTabbedPane tabs;

    private DataStorage storage;

    private LegendTree legend;

    private DataView view;

    private javax.swing.JTextField comment;

    /**
     * Creates new ViewContent
     */
    public ViewContent() {
        super();
        this.setLayout(new java.awt.GridLayout());
        storage = new DataStorage();
        createLegend();
    }

    /**
     * Returns Plot/View tabbed pane
     * 
     * @return tabbed pane
     */
    public JTabbedPane getTabs() {
        return tabs;
    }

    /**
     * Sets current DataStorage
     * 
     * @param s new DataStorage
     */
    public void setStorage(DataStorage s) {
        storage = s;
    }

    /**
     * Returns current DataStorage
     * 
     * @return DataStorage
     */
    public DataStorage getStorage() {
        return storage;
    }

    /**
     * Returns current LegendTree
     * 
     * @return LegendTree
     */
    public LegendTree getLegend() {
        return legend;
    }

    /**
     * Returns current DataView
     * 
     * @return DataView
     */
    public DataView getView() {
        return view;
    }

    /**
     * Returns comment
     * 
     * @return comment
     */
    public String getComment() {
        return comment.getText();
    }

    /**
     * Exports the content of <code>DataStorage</code> to ASCII file
     * 
     * @param fileName name of the file to export data
     * @throws IOException if i/o problem occurs
     */
    public synchronized void exportASCII(String fileName) throws java.io.IOException {
        java.io.PrintWriter datExp = new java.io.PrintWriter(new java.io.FileWriter(fileName));
        datExp.print(storage.exportDataASCII());
        datExp.flush();
        datExp.close();
    }

    /**
     * Exports the content of <code>DataStorage</code> to ATF file
     * 
     * @param fileName name of the file to export data
     * @throws IOException if i/o problem occurs
     */
    public synchronized void exportATF(String fileName) throws java.io.IOException {
        java.io.PrintWriter datExp = new java.io.PrintWriter(new java.io.FileWriter(fileName));
        datExp.print(buildATFHeader());
        datExp.print(storage.exportDataASCII());
        datExp.flush();
        datExp.close();
    }

    /**
     * Exports the content of <code>DataStorage</code> to netCDF file
     * 
     * @param fileName name of the file to export data
     * @throws IOException if i/o problem occurs
     */
    public synchronized void exportNetCDF(String fileName) throws java.io.IOException {
        NetcdfFileWriteable datExp = new NetcdfFileWriteable();
        datExp.setName(fileName);
        datExp.addGlobalAttribute("title", comment.getText());
        storage.exportDataMarray(datExp);
        datExp.flush();
        datExp.close();
    }

    /**
     * Exports the content of <code>DataView</code> to PNG graphics file
     * 
     * @param fileName name of the file to export data
     * @throws IOException if i/o problem occurs
     */
    public synchronized void exportPNG(String fileName) throws java.io.IOException {
        BufferedImage bi = new BufferedImage(view.getScrollPane().getWidth(), view.getScrollPane().getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bi.createGraphics();
        view.getScrollPane().paint(g2);
        g2.dispose();
        ImageIO.write(bi, "png", new File(fileName));
    }

    private JScrollPane createDataView() {
        view = new DataView(storage);
        return view.getScrollPane();
    }

    private JScrollPane createDataTable() {
        JTable table = new javax.swing.JTable(new DataTableModel(storage));
        JScrollPane tableScrl = new JScrollPane(table);
        tableScrl.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        RowHeaderRenderer rowHeader = new RowHeaderRenderer();
        table.setDefaultRenderer(Integer.class, rowHeader);
        return tableScrl;
    }

    private void createLegend() {
        tabs = new javax.swing.JTabbedPane();
        tabs.setTabPlacement(javax.swing.SwingConstants.BOTTOM);
        tabs.addTab(ResourceManager.getResource("Plot"), createDataView());
        tabs.addTab(ResourceManager.getResource("Table"), createDataTable());
        legend = new LegendTree(view);
        JPanel legendPanel = new JPanel(new java.awt.BorderLayout());
        JScrollPane legendScrl = new JScrollPane(legend.getTreeComponent());
        legendPanel.add(legendScrl, "Center");
        Box commentPanel = Box.createHorizontalBox();
        commentPanel.add(Box.createHorizontalStrut(5));
        commentPanel.add(new javax.swing.JLabel(com.javable.dataview.ResourceManager.getResource("Comment_")));
        comment = new javax.swing.JTextField(20);
        commentPanel.add(comment);
        legendPanel.add(commentPanel, "South");
        javax.swing.JSplitPane vSplitPane = new javax.swing.JSplitPane(javax.swing.SwingConstants.VERTICAL);
        vSplitPane.setOneTouchExpandable(true);
        vSplitPane.setRightComponent(tabs);
        vSplitPane.setLeftComponent(legendPanel);
        vSplitPane.setDividerLocation(0.0D);
        this.add(vSplitPane);
    }

    private String buildATFHeader() {
        DataChannel channel = null;
        StringBuffer hdr = new StringBuffer();
        int chans = 0;
        try {
            for (int i = 0; i < storage.getGroupsSize(); i++) {
                for (int j = 0; j < storage.getChannelsSize(i); j++) {
                    channel = storage.getChannel(i, j);
                    if (!channel.getAttribute().isHidden()) chans++;
                }
            }
            hdr.append("ATF" + storage.getDelimiter() + "1.0");
            hdr.append('\n');
            hdr.append(1 + storage.getDelimiter() + chans);
            hdr.append('\n');
            hdr.append("\"Comment=" + comment.getText() + "\"");
            hdr.append('\n');
        } catch (Exception e) {
        }
        return hdr.toString();
    }
}
