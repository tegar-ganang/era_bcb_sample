package org.verus.ngl.z3950client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import newgenlib.marccomponent.conversion.Converter;
import newgenlib.marccomponent.conversion.NewGenLibImplementation;
import newgenlib.marccomponent.marcmodel.CatalogMaterialDescription;
import newgenlib.marccomponent.marcmodel.Field;
import newgenlib.marccomponent.marcmodel.SubField;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.verus.ngl.z3950.utilities.NGLXMLUtility;
import org.verus.ngl.z3950.utilities.NGLUtilities;

/**
 *
 * @author  PIXEL1
 */
public class NGLZ3950ClientPanel extends javax.swing.JPanel implements ItemListener, ListSelectionListener, ActionListener {

    NGLXMLUtility nglXMLUtility = null;

    NGLUtilities nglUtilities = null;

    DefaultTableModel serversListTableModel = null, resultTableModel = null;

    javax.swing.JDialog dialogInstance = null;

    java.awt.Frame frame = new java.awt.Frame();

    Vector categoryVect = new Vector();

    Vector locationVect = new Vector();

    javax.swing.Timer timer = null;

    private String currServer = "";

    private int currrow = 0;

    private int renderingReportStatus = 0;

    int min = 1, max = 100;

    int col = 1;

    int sno = 1;

    int marcCount = 0, marcCountImpl = 0;

    String chek = "";

    /** Creates new form NGLZ3950ClientPanel */
    public NGLZ3950ClientPanel() {
        initComponents();
        nglXMLUtility = NGLXMLUtility.getInstance();
        nglUtilities = NGLUtilities.getInstance();
        setTables();
        applyKeyStrokesToTable();
        getServersDetails();
        setAttributes();
        bnStart.setVisible(false);
        bnStop.setVisible(false);
        bnMore.setVisible(false);
        setServersChecked();
        progress.setStringPainted(true);
        progress.setString("");
        progress.setMinimum(min);
        progress.setMaximum(max);
        timer = new javax.swing.Timer(100, new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progress.setIndeterminate(true);
                progress.setString("Searching....");
            }
        });
    }

    public void setTables() {
        Object[] columns = { " ", "Name", "Status", "XML", "Records" };
        serversListTableModel = new DefaultTableModel(columns, 0) {

            public boolean isCellEditable(int r, int c) {
                if (c == 0) return true; else return false;
            }

            public Class getColumnClass(int column) {
                return getValueAt(0, column).getClass();
            }
        };
        serversListTable.setModel(serversListTableModel);
        ListSelectionModel listMod = serversListTable.getSelectionModel();
        listMod.addListSelectionListener(this);
        serversListTable.getColumnModel().getColumn(0).setMinWidth(50);
        serversListTable.getColumnModel().getColumn(0).setMaxWidth(50);
        TableColumn tc = serversListTable.getColumnModel().getColumn(0);
        tc.setHeaderRenderer(new CheckBoxHeader(new MyItemListener()));
        serversListTable.getColumnModel().getColumn(1).setMinWidth(50);
        serversListTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        serversListTable.getColumnModel().getColumn(2).setMinWidth(50);
        serversListTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        serversListTable.getColumnModel().getColumn(3).setMinWidth(0);
        serversListTable.getColumnModel().getColumn(3).setMaxWidth(0);
        serversListTable.getColumnModel().getColumn(4).setMinWidth(0);
        serversListTable.getColumnModel().getColumn(4).setMaxWidth(0);
        serversListTable.getTableHeader().setReorderingAllowed(false);
        serversListTable.addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable source = (JTable) e.getSource();
                    int row = source.rowAtPoint(e.getPoint());
                    int column = source.columnAtPoint(e.getPoint());
                    if (!source.isRowSelected(row)) source.changeSelection(row, column, false, false);
                    serversPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        String[] moreFunctions1 = { "New", "Edit", "Delete", "MoveUp", "MoveDown" };
        serversPopup.removeAll();
        JMenuItem item = null;
        for (int i = 0; i < moreFunctions1.length; i++) {
            if (moreFunctions1[i].equalsIgnoreCase("New")) {
                item = serversPopup.add(moreFunctions1[i]);
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_MASK));
                item.addActionListener(this);
            }
            if (moreFunctions1[i].equalsIgnoreCase("Edit")) {
                item = serversPopup.add(moreFunctions1[i]);
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_MASK));
                item.addActionListener(this);
            }
            if (moreFunctions1[i].equalsIgnoreCase("Delete")) {
                item = serversPopup.add(moreFunctions1[i]);
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                item.addActionListener(this);
            }
            if (moreFunctions1[i].equalsIgnoreCase("MoveUp")) {
                item = serversPopup.add(moreFunctions1[i]);
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK));
                item.addActionListener(this);
            }
            if (moreFunctions1[i].equalsIgnoreCase("MoveDown")) {
                item = serversPopup.add(moreFunctions1[i]);
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK));
                item.addActionListener(this);
            }
        }
        serversPopup.setSize(200, 500);
        serversListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Object[] rcolumns = { "S.No", "Main Entry", "Title", "Hash", "Group ", "RecordDetails" };
        resultTableModel = new DefaultTableModel(rcolumns, 0) {

            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        resultTable.setModel(resultTableModel);
        resultTable.getColumnModel().getColumn(0).setMinWidth(50);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(50);
        resultTable.getColumnModel().getColumn(1).setMinWidth(50);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        resultTable.getColumnModel().getColumn(2).setMinWidth(50);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        resultTable.getColumnModel().getColumn(3).setMinWidth(0);
        resultTable.getColumnModel().getColumn(3).setMaxWidth(0);
        resultTable.getColumnModel().getColumn(4).setMinWidth(20);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(50);
        resultTable.getColumnModel().getColumn(5).setMinWidth(0);
        resultTable.getColumnModel().getColumn(5).setMaxWidth(0);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void setServersChecked() {
        System.out.println("servers ched method  start");
        java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
        String st = "";
        String sst = "Z3950Servers";
        st = pref.get(sst, "");
        int rc = serversListTable.getRowCount();
        if (!st.equals(null)) {
            String serst[] = st.split(":");
            for (int i = 0; i < serst.length; i++) {
                String s = serst[i];
                for (int j = 0; j < rc; j++) {
                    String ser = serversListTable.getValueAt(j, 1).toString();
                    if (ser.equalsIgnoreCase(s)) serversListTable.setValueAt(new Boolean(true), j, 0);
                }
            }
        }
    }

    private void applyKeyStrokesToTable() {
        try {
            KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            serversListTable.getInputMap().put(delete, "Delete");
            Action deleteAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    deleteServer();
                }
            };
            serversListTable.getActionMap().put("Delete", deleteAction);
            KeyStroke moveup = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK);
            serversListTable.getInputMap().put(moveup, "moveup");
            Action moveupAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    moveUpServer();
                }
            };
            serversListTable.getActionMap().put("moveup", moveupAction);
            KeyStroke movedown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK);
            serversListTable.getInputMap().put(movedown, "MoveDown");
            Action movedownAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    moveDownServer();
                }
            };
            serversListTable.getActionMap().put("MoveDown", movedownAction);
        } catch (Exception e) {
        }
    }

    public void setAttributes() {
        Vector catVect = new Vector();
        Vector locVect = new Vector();
        org.verus.ngl.z3950.query.Bib1UseAttributes bib = new org.verus.ngl.z3950.query.Bib1UseAttributes();
        java.util.Set keyset = bib.getBib1AttributeNoHashKeys();
        String[] keys = new String[bib.getBib1AttributeNoHashSize()];
        keyset.toArray(keys);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < keys.length; i++) {
            cmbAttribute1.addItem(keys[i]);
            cmbAttribute2.addItem(keys[i]);
        }
        cmbAttribute1.setSelectedItem("Title");
        cmbServerType.addItem("All");
        cmbServerType.addItem("Z3950");
        cmbServerType.addItem("SRU/W");
        cmbRelation.addItem("AND");
        cmbRelation.addItem("OR");
        int i, j;
        int flag = 0;
        for (i = 0; i < categoryVect.size(); i++) {
            flag = 0;
            for (j = 0; j < catVect.size(); j++) {
                if (categoryVect.elementAt(i).equals(catVect.elementAt(j))) flag = 1;
            }
            if (flag == 0) catVect.addElement(categoryVect.elementAt(i));
        }
        for (i = 0; i < locationVect.size(); i++) {
            flag = 0;
            for (j = 0; j < locVect.size(); j++) {
                if (locationVect.elementAt(i).equals(locVect.elementAt(j))) flag = 1;
            }
            if (flag == 0) locVect.addElement(locationVect.elementAt(i));
        }
        cmbCategory.addItem("All");
        cmbCountry.addItem("All");
        for (i = 0; i < catVect.size(); i++) cmbCategory.addItem(catVect.elementAt(i));
        for (i = 0; i < locVect.size(); i++) cmbCountry.addItem(locVect.elementAt(i));
        cmbCategory.setSelectedIndex(0);
        cmbCountry.setSelectedIndex(0);
        cmbServerType.setSelectedIndex(0);
        cmbCategory.addItemListener(this);
        cmbCountry.addItemListener(this);
        cmbServerType.addItemListener(this);
        cmbAttribute1.setRenderer(new ComboBoxTooltipRenderer());
        cmbAttribute1.setToolTipText(cmbAttribute1.getSelectedItem().toString());
        cmbAttribute2.setRenderer(new ComboBoxTooltipRenderer());
        cmbAttribute2.setToolTipText(cmbAttribute2.getSelectedItem().toString());
        cmbRelation.setRenderer(new ComboBoxTooltipRenderer());
        cmbRelation.setToolTipText(cmbRelation.getSelectedItem().toString());
        cmbServerType.setRenderer(new ComboBoxTooltipRenderer());
        cmbServerType.setToolTipText(cmbServerType.getSelectedItem().toString());
        cmbCategory.setRenderer(new ComboBoxTooltipRenderer());
        cmbCategory.setToolTipText(cmbCategory.getSelectedItem().toString());
        cmbCountry.setRenderer(new ComboBoxTooltipRenderer());
        cmbCountry.setToolTipText(cmbCountry.getSelectedItem().toString());
    }

    public void setAttributesNew() {
        Vector catVect = new Vector();
        Vector locVect = new Vector();
        int i, j;
        int flag = 0;
        for (i = 0; i < categoryVect.size(); i++) {
            flag = 0;
            for (j = 0; j < catVect.size(); j++) {
                if (categoryVect.elementAt(i).equals(catVect.elementAt(j))) flag = 1;
            }
            if (flag == 0) catVect.addElement(categoryVect.elementAt(i));
        }
        for (i = 0; i < locationVect.size(); i++) {
            flag = 0;
            for (j = 0; j < locVect.size(); j++) {
                if (locationVect.elementAt(i).equals(locVect.elementAt(j))) flag = 1;
            }
            if (flag == 0) locVect.addElement(locationVect.elementAt(i));
        }
        cmbCategory.addItem("All");
        cmbCountry.addItem("All");
        for (i = 0; i < catVect.size(); i++) {
            cmbCategory.addItem(catVect.elementAt(i));
        }
        for (i = 0; i < locVect.size(); i++) {
            cmbCountry.addItem(locVect.elementAt(i));
        }
        cmbCategory.setSelectedIndex(0);
        cmbCountry.setSelectedIndex(0);
        cmbServerType.setSelectedIndex(0);
        cmbCategory.addItemListener(this);
        cmbCountry.addItemListener(this);
        cmbServerType.addItemListener(this);
    }

    public void getServersDetails() {
        try {
            locationVect = new Vector();
            categoryVect = new Vector();
            String fileName = System.getProperty("user.home");
            fileName = fileName.concat("/NGLZ3950Servers.xml");
            File inFile = new File(fileName);
            FileInputStream fis = new FileInputStream(inFile);
            FileChannel inChannel = fis.getChannel();
            ByteBuffer buf = ByteBuffer.allocate((int) inChannel.size());
            inChannel.read(buf);
            inChannel.close();
            String myString = new String(buf.array());
            org.jdom.Element root = nglXMLUtility.getRootElementFromXML(myString);
            java.util.List clist = root.getChildren();
            for (int i = 0; i < clist.size(); i++) {
                java.util.Vector vect = new java.util.Vector();
                org.jdom.Element child = (org.jdom.Element) clist.get(i);
                String xml = nglXMLUtility.generateXML((org.jdom.Element) child.clone());
                vect.addElement(new Boolean(false));
                vect.addElement(child.getChildText("Name"));
                categoryVect.addElement(child.getChildText("Category"));
                locationVect.addElement(child.getChildText("Location"));
                vect.addElement("");
                vect.addElement(xml);
                serversListTableModel.addRow(vect);
            }
        } catch (java.io.FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getServersDetailsNew() {
        serversListTableModel.setRowCount(0);
        String cat = cmbCategory.getSelectedItem().toString();
        String loc = cmbCountry.getSelectedItem().toString();
        String type = cmbServerType.getSelectedItem().toString();
        try {
            String fileName = System.getProperty("user.home");
            fileName = fileName.concat("/NGLZ3950Servers.xml");
            File inFile = new File(fileName);
            FileInputStream fis = new FileInputStream(inFile);
            FileChannel inChannel = fis.getChannel();
            ByteBuffer buf = ByteBuffer.allocate((int) inChannel.size());
            inChannel.read(buf);
            inChannel.close();
            String myString = new String(buf.array());
            org.jdom.Element root = nglXMLUtility.getRootElementFromXML(myString);
            java.util.List clist = root.getChildren();
            for (int i = 0; i < clist.size(); i++) {
                java.util.Vector vect = new java.util.Vector();
                org.jdom.Element child = (org.jdom.Element) clist.get(i);
                String xml = nglXMLUtility.generateXML((org.jdom.Element) child.clone());
                vect.addElement(new Boolean(false));
                vect.addElement(child.getChildText("Name"));
                String category = child.getChildText("Category");
                categoryVect.addElement(category);
                String location = child.getChildText("Location");
                locationVect.addElement(location);
                String type1 = child.getChildText("Type");
                vect.addElement("");
                vect.addElement(xml);
                if (cat.equalsIgnoreCase("All") && loc.equalsIgnoreCase("All") && type.equalsIgnoreCase("All")) serversListTableModel.addRow(vect); else if (cat.equalsIgnoreCase("All") && loc.equalsIgnoreCase("All") && !type.equalsIgnoreCase("All")) {
                    if (type.equalsIgnoreCase(type1)) serversListTableModel.addRow(vect);
                } else if (cat.equalsIgnoreCase("All") && !loc.equalsIgnoreCase("All") && type.equalsIgnoreCase("All")) {
                    if (location.equalsIgnoreCase(loc)) serversListTableModel.addRow(vect);
                } else if (!cat.equalsIgnoreCase("All") && loc.equalsIgnoreCase("All") && type.equalsIgnoreCase("All")) {
                    if (category.equalsIgnoreCase(cat)) serversListTableModel.addRow(vect);
                } else if (!cat.equalsIgnoreCase("All") && loc.equalsIgnoreCase("All") && !type.equalsIgnoreCase("All")) {
                    if (category.equalsIgnoreCase(cat) && type.equalsIgnoreCase(type1)) serversListTableModel.addRow(vect);
                } else if (!cat.equalsIgnoreCase("All") && !loc.equalsIgnoreCase("All") && type.equalsIgnoreCase("All")) {
                    if (category.equalsIgnoreCase(cat) && location.equalsIgnoreCase(loc)) serversListTableModel.addRow(vect);
                } else if (!cat.equalsIgnoreCase("All") && !loc.equalsIgnoreCase("All") && !type.equalsIgnoreCase("All")) {
                    if (category.equalsIgnoreCase(cat) && location.equalsIgnoreCase(loc) && type.equalsIgnoreCase(type1)) serversListTableModel.addRow(vect);
                }
            }
            cmbServerType.setToolTipText(cmbServerType.getSelectedItem().toString());
            cmbCategory.setToolTipText(cmbCategory.getSelectedItem().toString());
            cmbCountry.setToolTipText(cmbCountry.getSelectedItem().toString());
            cmbAttribute1.setToolTipText(cmbAttribute1.getSelectedItem().toString());
            cmbAttribute2.setToolTipText(cmbAttribute2.getSelectedItem().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshServers() {
        serversListTableModel.setRowCount(0);
        cmbCategory.removeItemListener(this);
        cmbCountry.removeItemListener(this);
        cmbServerType.removeItemListener(this);
        cmbCategory.removeAllItems();
        cmbCountry.removeAllItems();
        getServersDetails();
        setAttributesNew();
        getServersDetailsNew();
    }

    public void addServer() {
        if (this.dialogInstance == null) {
            NGLZ3950ServerAddOrEditDialog pane = new NGLZ3950ServerAddOrEditDialog(frame, "Add", "Add Z3950Server", categoryVect, locationVect);
            pane.setVisible(true);
        } else {
            NGLZ3950ServerAddOrEditDialog pane = new NGLZ3950ServerAddOrEditDialog(this.dialogInstance, "Add", "Add Z3950Server", categoryVect, locationVect);
            pane.setVisible(true);
        }
        refreshServers();
    }

    public void editServer() {
        int rc = serversListTable.getRowCount();
        int k = 0;
        if (rc != 0) {
            for (int i = 0; i < rc; i++) {
                String s = serversListTable.getValueAt(i, 0).toString();
                if (s.equals("true")) k++;
            }
            int[] selrows = new int[k];
            if (k <= 1) {
                int selrow = serversListTable.getSelectedRow();
                for (int i = 0; i < rc; i++) {
                    String s = serversListTable.getValueAt(i, 0).toString();
                    if (s.equals("true")) selrow = i;
                }
                if (selrow != -1) {
                    String server = serversListTable.getValueAt(selrow, 3).toString();
                    if (this.dialogInstance == null) {
                        NGLZ3950ServerAddOrEditDialog pane = new NGLZ3950ServerAddOrEditDialog(frame, "Modify", "Modify Z3950Server", categoryVect, locationVect);
                        pane.setServerDetails(server);
                        pane.setVisible(true);
                    } else {
                        NGLZ3950ServerAddOrEditDialog pane = new NGLZ3950ServerAddOrEditDialog(this.dialogInstance, "Modify", "Modify Z3950Server", categoryVect, locationVect);
                        pane.setServerDetails(server);
                        pane.setVisible(true);
                    }
                } else {
                    javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
                    pane.setLocation(nglUtilities.getScreenLocation(pane.getSize()));
                    pane.showMessageDialog(jSplitPane1, "Select Server..");
                }
            } else {
                javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
                pane.setLocation(nglUtilities.getScreenLocation(pane.getSize()));
                pane.showMessageDialog(jSplitPane1, "Select one sever only for editing..");
            }
        } else {
            javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
            pane.setLocation(nglUtilities.getScreenLocation(pane.getSize()));
            pane.showMessageDialog(jSplitPane1, "Invalid operation..");
        }
        refresh();
        refreshServers();
    }

    public void deleteServer() {
        int rc = serversListTable.getRowCount();
        int k = 0;
        if (rc != 0) {
            for (int i = 0; i < rc; i++) {
                String s = serversListTable.getValueAt(i, 0).toString();
                if (s.equals("true")) k++;
            }
            int[] selrows = new int[k];
            if (k > 0) {
                int l = 0;
                for (int i = 0; i < rc; i++) {
                    String s = serversListTable.getValueAt(i, 0).toString();
                    if (s.equals("true")) {
                        selrows[l] = i;
                        l++;
                    }
                }
                javax.swing.JOptionPane jpane = new javax.swing.JOptionPane();
                int dtype = jpane.showConfirmDialog(jSplitPane1, "Do you want to delete[" + k + "] server(s)?", "Question", 0);
                if (dtype == 0) {
                    try {
                        String fileName = System.getProperty("user.home");
                        fileName = fileName.concat("/NGLZ3950Servers.xml");
                        File inFile = new File(fileName);
                        if (inFile.exists()) {
                            FileInputStream fis = new FileInputStream(inFile);
                            SAXBuilder sb = new SAXBuilder();
                            Document doc = null;
                            try {
                                doc = sb.build(fis);
                            } catch (Exception e) {
                            }
                            org.jdom.Element root = doc.getRootElement();
                            org.jdom.Element newRoot = new org.jdom.Element("ZServers");
                            java.util.List childList = root.getChildren();
                            for (int i = 0; i < childList.size(); i++) {
                                org.jdom.Element child = (org.jdom.Element) childList.get(i);
                                String name = child.getChildText("Name");
                                int flag = 0;
                                for (int v = 0; v < selrows.length; v++) {
                                    String oldName = serversListTable.getValueAt(selrows[v], 1).toString();
                                    if (name.equalsIgnoreCase(oldName)) flag = 1;
                                }
                                if (flag == 0) newRoot.addContent((org.jdom.Element) child.clone());
                            }
                            XMLOutputter xout = new XMLOutputter();
                            xout.output(new Document((Element) newRoot.clone()), new FileOutputStream(inFile));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                refresh();
            } else {
                int selrow = serversListTable.getSelectedRow();
                if (selrow != -1) {
                    javax.swing.JOptionPane jpane = new javax.swing.JOptionPane();
                    int dtype = jpane.showConfirmDialog(jSplitPane1, "Do you want to delete[1] server(s)?", "Question", 0);
                    if (dtype == 0) {
                        try {
                            String fileName = System.getProperty("user.home");
                            fileName = fileName.concat("/NGLZ3950Servers.xml");
                            File inFile = new File(fileName);
                            if (inFile.exists()) {
                                FileInputStream fis = new FileInputStream(inFile);
                                SAXBuilder sb = new SAXBuilder();
                                Document doc = null;
                                try {
                                    doc = sb.build(fis);
                                } catch (Exception e) {
                                }
                                org.jdom.Element root = doc.getRootElement();
                                org.jdom.Element newRoot = new org.jdom.Element("ZServers");
                                java.util.List childList = root.getChildren();
                                for (int i = 0; i < childList.size(); i++) {
                                    org.jdom.Element child = (org.jdom.Element) childList.get(i);
                                    String name = child.getChildText("Name");
                                    String oldName = serversListTable.getValueAt(selrow, 1).toString();
                                    if (name.equalsIgnoreCase(oldName)) {
                                    } else {
                                        newRoot.addContent((org.jdom.Element) child.clone());
                                    }
                                }
                                XMLOutputter xout = new XMLOutputter();
                                xout.output(new Document((Element) newRoot.clone()), new FileOutputStream(inFile));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    refresh();
                } else {
                    javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
                    pane.showMessageDialog(jSplitPane1, "Select Server(s)..");
                }
            }
        } else {
            javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
            pane.showMessageDialog(jSplitPane1, "Invalid operation..");
        }
        refreshServers();
    }

    public void moveUpServer() {
        int selrow = serversListTable.getSelectedRow();
        if (selrow != -1) {
            if (selrow != 0) {
                swapRows(selrow, selrow - 1);
                serversListTable.changeSelection(selrow - 1, 0, false, false);
            }
        }
    }

    public void moveDownServer() {
        int selrow = serversListTable.getSelectedRow();
        int rc = serversListTable.getRowCount();
        if (selrow != -1) {
            if (selrow != (rc - 1)) {
                swapRows(selrow, selrow + 1);
                serversListTable.changeSelection(selrow + 1, 0, false, false);
            }
        }
    }

    public void swapRows(int crow, int nrow) {
        Object cck = serversListTable.getValueAt(crow, 0);
        Object nck = serversListTable.getValueAt(nrow, 0);
        Object cname = serversListTable.getValueAt(crow, 1);
        Object nname = serversListTable.getValueAt(nrow, 1);
        Object cxml = serversListTable.getValueAt(crow, 3);
        Object nxml = serversListTable.getValueAt(nrow, 3);
        Object crecords = serversListTable.getValueAt(crow, 4);
        Object nrecords = serversListTable.getValueAt(nrow, 4);
        serversListTable.setValueAt(cck, nrow, 0);
        serversListTable.setValueAt(nck, crow, 0);
        serversListTable.setValueAt(cname, nrow, 1);
        serversListTable.setValueAt(nname, crow, 1);
        serversListTable.setValueAt(cxml, nrow, 3);
        serversListTable.setValueAt(nxml, crow, 3);
        serversListTable.setValueAt(crecords, nrow, 4);
        serversListTable.setValueAt(nrecords, crow, 4);
        clearStatuses();
    }

    public void refresh() {
        serversListTableModel.setRowCount(0);
        getServersDetails();
    }

    public void clearChecks() {
        for (int i = 0; i < serversListTable.getRowCount(); i++) {
            serversListTable.setValueAt(new Boolean(false), i, 0);
        }
    }

    public void getSearchDetails() {
        Vector rVect = new Vector();
        resultTableModel.setRowCount(0);
        org.verus.ngl.z3950.query.Bib1UseAttributes bib = new org.verus.ngl.z3950.query.Bib1UseAttributes();
        Hashtable rhash = new Hashtable();
        int k = 0;
        for (int i = 0; i < serversListTable.getRowCount(); i++) {
            String s = serversListTable.getValueAt(i, 0).toString();
            if (s.equals("true")) k++;
        }
        if (k > 0) {
            java.util.prefs.Preferences pref = java.util.prefs.Preferences.systemRoot();
            String st = "";
            String sst = "Z3950Servers";
            int[] selrows = new int[k];
            int l = 0;
            for (int i = 0; i < serversListTable.getRowCount(); i++) {
                String s = serversListTable.getValueAt(i, 0).toString();
                if (s.equals("true")) {
                    String ss = serversListTable.getValueAt(i, 1).toString();
                    st = st.concat(ss).concat(":");
                    selrows[l] = i;
                    l++;
                }
            }
            pref.put(sst, st);
            if (selrows.length > 0) {
                if (attrValidation()) {
                    for (int i = 0; i < selrows.length; i++) {
                        rhash = new Hashtable();
                        String xml = serversListTable.getValueAt(selrows[i], 3).toString();
                        org.jdom.Element root = nglXMLUtility.getRootElementFromXML(xml);
                        rhash.put("currthread", root.getChildText("Name"));
                        org.jdom.Element zurl = root.getChild("Zurl");
                        rhash.put("ServiceHost", zurl.getChildText("BaseURL"));
                        rhash.put("ServicePort", zurl.getChildText("Port"));
                        rhash.put("database", zurl.getChildText("DataBase"));
                        rhash.put("attribute1", bib.getBib1AttributeNo(cmbAttribute1.getSelectedItem()));
                        rhash.put("value1", tfattribute1.getText().trim());
                        rhash.put("value2", tfAttribute2.getText().trim());
                        rhash.put("attribute2", bib.getBib1AttributeNo(cmbAttribute2.getSelectedItem()));
                        rhash.put("relation", cmbRelation.getSelectedItem());
                        rhash.put("selrow", String.valueOf(selrows[i]));
                        serversListTable.setValueAt("Searching...", selrows[i], 2);
                        rVect.addElement(rhash);
                    }
                    doSearch(rVect);
                    progress.setIndeterminate(false);
                    progress.setString("Completed...");
                    setServerGroups();
                    timer.stop();
                } else {
                    timer.stop();
                    progress.setIndeterminate(false);
                    progress.setString("");
                    javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
                    pane.showMessageDialog(jSplitPane1, "Please give attribute(s)...");
                }
            }
        } else {
            timer.stop();
            progress.setIndeterminate(false);
            progress.setString("");
            javax.swing.JOptionPane pane = new javax.swing.JOptionPane();
            pane.showMessageDialog(jSplitPane1, "Select Server(s)...");
        }
    }

    public boolean attrValidation() {
        String att1 = tfattribute1.getText().trim();
        String att2 = tfAttribute2.getText().trim();
        if (!att1.equals("") || !att2.equals("")) return true; else return false;
    }

    public void setServerGroups() {
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String st = "";
            Vector vect = (Vector) resultTable.getValueAt(i, 4);
            for (int j = 0; j < vect.size(); j++) {
                String st1 = vect.get(j).toString();
                st = st.concat(st1).concat(",");
            }
            resultTable.setValueAt(st, i, 4);
        }
    }

    public void setSearchDetails(Vector recVect) {
        try {
            System.out.println("this is in set serch details method..............");
            System.out.println("vector size:$$$$$$:" + recVect.size());
            for (int i = 0; i < recVect.size(); i++) {
                CatalogMaterialDescription cmd = null;
                Vector resVect = new Vector();
                System.out.println(i + " " + recVect.elementAt(i));
                String isorecord = recVect.elementAt(i).toString();
                try {
                    Converter conv = new Converter();
                    cmd = conv.getMarcModelFromMarc(isorecord);
                } catch (Exception e2) {
                    marcCount++;
                    cmd = null;
                }
                if (cmd == null) {
                    try {
                        NewGenLibImplementation implConv = new NewGenLibImplementation();
                        cmd = implConv.getMarcModelFromMarc(isorecord);
                    } catch (Exception e3) {
                        cmd = null;
                        marcCountImpl++;
                    }
                }
                if (cmd != null) {
                    Hashtable recHash = new Hashtable();
                    resVect.addElement(String.valueOf(sno));
                    String mainEntry = "";
                    Field field = cmd.getField("111");
                    if (field == null) {
                        field = cmd.getField("110");
                        if (field == null) {
                            field = cmd.getField("100");
                            if (field == null) mainEntry = ""; else mainEntry = field.getSubFieldData('a');
                        } else mainEntry = field.getSubFieldData('a');
                    } else mainEntry = field.getSubFieldData('a');
                    resVect.addElement(mainEntry);
                    recHash.put("mainentry", mainEntry);
                    String title = cmd.getTitleSlashResponsibility();
                    resVect.addElement(title);
                    recHash.put("title", title);
                    Field pfield = cmd.getField("260");
                    if (pfield != null) {
                        String pp = pfield.getSubFieldData('a');
                        if (pp == null) pp = "";
                        String np = pfield.getSubFieldData('b');
                        if (np == null) np = "";
                        String dp = pfield.getSubFieldData('c');
                        if (dp == null) dp = "";
                        recHash.put("pop", pp);
                        recHash.put("nop", np);
                        recHash.put("dop", dp);
                    }
                    resVect.addElement(recHash);
                    Vector serverg = new Vector();
                    serverg.addElement(currServer);
                    resVect.addElement(serverg);
                    Hashtable cmdHash = new Hashtable();
                    cmdHash.put(currServer, cmd);
                    resVect.addElement(cmdHash);
                    if (!dupCheck(recHash)) {
                        Vector group = (Vector) resultTable.getValueAt(getCurrrow(), 4);
                        Hashtable cHash = (Hashtable) resultTable.getValueAt(getCurrrow(), 5);
                        if (group == null) {
                            group = new Vector();
                            cHash = new Hashtable();
                            group.addElement(getCurrServer());
                            resultTable.setValueAt(group, getCurrrow(), 4);
                            cHash.put(currServer, cmd);
                        } else {
                            int found = 0;
                            String currserver = getCurrServer();
                            for (int l = 0; l < group.size(); l++) {
                                String currs = group.get(l).toString();
                                if (currs.equalsIgnoreCase(currserver)) found = 1;
                            }
                            if (found == 0) {
                                group.addElement(currserver);
                                cHash.put(currserver, cmd);
                                resultTable.setValueAt(group, getCurrrow(), 4);
                                resultTable.setValueAt(cHash, getCurrrow(), 5);
                            }
                        }
                    } else {
                        resultTableModel.addRow(resVect);
                        sno++;
                    }
                } else {
                    System.out.println("#################################################################");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
        System.out.println("marcCountImpl " + marcCountImpl);
        System.out.println("marcCount " + marcCount);
    }

    public boolean dupCheck(Hashtable recHash) {
        boolean rettype = true;
        int rc = resultTable.getRowCount();
        try {
            if (rc == 0) rettype = true; else {
                for (int i = 0; i < rc; i++) {
                    Hashtable rHash = (Hashtable) resultTable.getValueAt(i, 3);
                    String title1 = rHash.get("title").toString();
                    String title2 = recHash.get("title").toString();
                    String mainentry1 = rHash.get("mainentry").toString();
                    String mainentry2 = recHash.get("mainentry").toString();
                    String pop1 = rHash.get("pop").toString();
                    String pop2 = recHash.get("pop").toString();
                    String nop1 = rHash.get("nop").toString();
                    String nop2 = recHash.get("nop").toString();
                    String dop1 = rHash.get("dop").toString();
                    String dop2 = recHash.get("dop").toString();
                    if (title1.equalsIgnoreCase(title2) && mainentry1.equalsIgnoreCase(mainentry2) && pop1.equalsIgnoreCase(pop2) && nop1.equalsIgnoreCase(nop2) && dop1.equalsIgnoreCase(dop2)) {
                        rettype = false;
                        setCurrrow(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
        return rettype;
    }

    public void doSearch(Vector rVect) {
        System.out.println("vector:" + rVect);
        if (rVect.size() > 0) {
            for (int i = 0; i < rVect.size(); i++) {
                Hashtable rhash = (Hashtable) rVect.get(i);
                String selr = rhash.get("selrow").toString();
                serversListTable.setValueAt("Searching...", Integer.parseInt(selr), 2);
                setCurrServer(rhash.get("currthread").toString());
                org.verus.ngl.z3950.search.NGLZ3950Search jzkit = new org.verus.ngl.z3950.search.NGLZ3950Search();
                Vector recVect = jzkit.getSearchResults(rhash);
                setSearchDetails(recVect);
                serversListTable.setValueAt(recVect, Integer.parseInt(selr), 4);
                String coms = "Completed...".concat("[").concat(String.valueOf(recVect.size())).concat("]");
                serversListTable.setValueAt(coms, Integer.parseInt(selr), 2);
            }
        }
    }

    public void clearStatuses() {
        for (int i = 0; i < serversListTable.getRowCount(); i++) serversListTable.setValueAt("", i, 2);
    }

    public void clearAction() {
        tfAttribute2.setText("");
        tfattribute1.setText("");
        resultTableModel.setRowCount(0);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        serversPopup = new javax.swing.JPopupMenu();
        jPanel1 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        bnNew = new javax.swing.JButton();
        bnEdit = new javax.swing.JButton();
        bnDelete = new javax.swing.JButton();
        bnStop = new javax.swing.JButton();
        bnStart = new javax.swing.JButton();
        bnMore = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        serversListTable = new JTable() {

            public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
                Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    try {
                        jc.setToolTipText((String) getValueAt(rowIndex, vColIndex));
                    } catch (Exception e) {
                    }
                }
                return c;
            }
        };
        jPanel7 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cmbCategory = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        cmbCountry = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cmbServerType = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        cmbAttribute1 = new javax.swing.JComboBox();
        tfattribute1 = new javax.swing.JTextField();
        cmbRelation = new javax.swing.JComboBox();
        cmbAttribute2 = new javax.swing.JComboBox();
        tfAttribute2 = new javax.swing.JTextField();
        bnSearch = new javax.swing.JButton();
        bnCancel = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        resultTable = new JTable() {

            public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
                Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    try {
                        jc.setToolTipText((String) getValueAt(rowIndex, vColIndex));
                    } catch (Exception e) {
                    }
                }
                return c;
            }
        };
        setLayout(new java.awt.BorderLayout());
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));
        add(jPanel1, java.awt.BorderLayout.CENTER);
        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setPreferredSize(new java.awt.Dimension(13, 25));
        bnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/z3950client/images/New16.gif")));
        bnNew.setMnemonic('n');
        bnNew.setToolTipText("New");
        bnNew.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnNewActionPerformed(evt);
            }
        });
        jToolBar1.add(bnNew);
        bnEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/z3950client/images/Edit16.gif")));
        bnEdit.setMnemonic('t');
        bnEdit.setToolTipText("Edit");
        bnEdit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnEditActionPerformed(evt);
            }
        });
        jToolBar1.add(bnEdit);
        bnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/z3950client/images/Delete16.gif")));
        bnDelete.setMnemonic('d');
        bnDelete.setToolTipText("Delete");
        bnDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnDeleteActionPerformed(evt);
            }
        });
        jToolBar1.add(bnDelete);
        bnStop.setText("Stop");
        bnStop.setToolTipText("Stop");
        jToolBar1.add(bnStop);
        bnStart.setText("Start");
        bnStart.setToolTipText("Start");
        jToolBar1.add(bnStart);
        bnMore.setText("More");
        bnMore.setToolTipText("More");
        jToolBar1.add(bnMore);
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/verus/ngl/z3950client/images/Refresh16.gif")));
        jButton1.setMnemonic('r');
        jButton1.setToolTipText("Refresh( show all servers)");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);
        add(jToolBar1, java.awt.BorderLayout.NORTH);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(780, 25));
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.setPreferredSize(new java.awt.Dimension(230, 480));
        jPanel6.setLayout(new java.awt.BorderLayout());
        serversListTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] {}));
        serversListTable.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                serversListTableMouseClicked(evt);
            }
        });
        serversListTable.addAncestorListener(new javax.swing.event.AncestorListener() {

            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }

            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                serversListTableAncestorAdded(evt);
            }

            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jScrollPane1.setViewportView(serversListTable);
        jPanel6.add(jScrollPane1, java.awt.BorderLayout.CENTER);
        jPanel2.add(jPanel6, java.awt.BorderLayout.CENTER);
        jPanel7.setLayout(new java.awt.GridBagLayout());
        jPanel7.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel1.setText("Category");
        jLabel1.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel7.add(jLabel1, gridBagConstraints);
        cmbCategory.setPreferredSize(new java.awt.Dimension(150, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel7.add(cmbCategory, gridBagConstraints);
        jLabel2.setText("Country");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel7.add(jLabel2, gridBagConstraints);
        cmbCountry.setPreferredSize(new java.awt.Dimension(150, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        jPanel7.add(cmbCountry, gridBagConstraints);
        jLabel3.setText("Server type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        jPanel7.add(jLabel3, gridBagConstraints);
        cmbServerType.setPreferredSize(new java.awt.Dimension(150, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        jPanel7.add(cmbServerType, gridBagConstraints);
        jPanel2.add(jPanel7, java.awt.BorderLayout.NORTH);
        jSplitPane1.setLeftComponent(jPanel2);
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));
        jPanel3.setPreferredSize(new java.awt.Dimension(500, 100));
        jPanel4.setLayout(new java.awt.BorderLayout());
        jPanel8.setLayout(new java.awt.GridBagLayout());
        jPanel8.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        cmbAttribute1.setPreferredSize(new java.awt.Dimension(100, 22));
        cmbAttribute1.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbAttribute1ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 0);
        jPanel8.add(cmbAttribute1, gridBagConstraints);
        tfattribute1.setColumns(15);
        tfattribute1.setMinimumSize(new java.awt.Dimension(11, 22));
        tfattribute1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfattribute1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 0);
        jPanel8.add(tfattribute1, gridBagConstraints);
        cmbRelation.setPreferredSize(new java.awt.Dimension(60, 22));
        cmbRelation.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbRelationItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 0);
        jPanel8.add(cmbRelation, gridBagConstraints);
        cmbAttribute2.setPreferredSize(new java.awt.Dimension(100, 22));
        cmbAttribute2.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbAttribute2ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 0);
        jPanel8.add(cmbAttribute2, gridBagConstraints);
        tfAttribute2.setColumns(15);
        tfAttribute2.setMinimumSize(new java.awt.Dimension(11, 22));
        tfAttribute2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfAttribute2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 0);
        jPanel8.add(tfAttribute2, gridBagConstraints);
        bnSearch.setMnemonic('s');
        bnSearch.setText("Search");
        bnSearch.setToolTipText("Search");
        bnSearch.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnSearchActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        jPanel8.add(bnSearch, gridBagConstraints);
        bnCancel.setText("Cancel");
        bnCancel.setToolTipText("Cancel");
        bnCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bnCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        jPanel8.add(bnCancel, gridBagConstraints);
        jPanel4.add(jPanel8, java.awt.BorderLayout.CENTER);
        jPanel9.setLayout(new java.awt.GridBagLayout());
        jPanel9.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel9.setPreferredSize(new java.awt.Dimension(100, 40));
        jLabel4.setText("Progress");
        jPanel9.add(jLabel4, new java.awt.GridBagConstraints());
        progress.setPreferredSize(new java.awt.Dimension(200, 18));
        jPanel9.add(progress, new java.awt.GridBagConstraints());
        jPanel4.add(jPanel9, java.awt.BorderLayout.SOUTH);
        jPanel3.add(jPanel4);
        jPanel5.setLayout(new java.awt.BorderLayout());
        jPanel5.setPreferredSize(new java.awt.Dimension(100, 300));
        resultTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null }, { null, null }, { null, null }, { null, null } }, new String[] { "S.No", "Title/Author" }));
        jScrollPane2.setViewportView(resultTable);
        jPanel5.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        jPanel3.add(jPanel5);
        jSplitPane1.setRightComponent(jPanel3);
        add(jSplitPane1, java.awt.BorderLayout.WEST);
    }

    private void cmbRelationItemStateChanged(java.awt.event.ItemEvent evt) {
        cmbRelation.setToolTipText(cmbRelation.getSelectedItem().toString());
    }

    private void cmbAttribute2ItemStateChanged(java.awt.event.ItemEvent evt) {
        cmbAttribute2.setToolTipText(cmbAttribute2.getSelectedItem().toString());
    }

    private void cmbAttribute1ItemStateChanged(java.awt.event.ItemEvent evt) {
        cmbAttribute1.setToolTipText(cmbAttribute1.getSelectedItem().toString());
    }

    private void serversListTableMouseClicked(java.awt.event.MouseEvent evt) {
        int row = serversListTable.getSelectedRow();
        int col = serversListTable.getSelectedColumn();
        if (row != -1 && col != -1) {
            if (col != 0) {
                String boo = String.valueOf(serversListTable.getValueAt(row, 0));
                if (boo.equalsIgnoreCase("false")) clearChecks();
            }
        }
    }

    private void serversListTableAncestorAdded(javax.swing.event.AncestorEvent evt) {
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        refreshServers();
    }

    private void bnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        tfAttribute2.setText("");
        tfattribute1.setText("");
    }

    private void tfAttribute2ActionPerformed(java.awt.event.ActionEvent evt) {
        bnSearch.doClick();
    }

    private void tfattribute1ActionPerformed(java.awt.event.ActionEvent evt) {
        bnSearch.doClick();
    }

    private void bnSearchActionPerformed(java.awt.event.ActionEvent evt) {
        timer.start();
        clearStatuses();
        resultTableModel.setRowCount(0);
        final org.verus.ngl.z3950client.SwingWorker worker = new org.verus.ngl.z3950client.SwingWorker() {

            public Object construct() {
                System.out.println("**********before calling doSearch***********");
                getSearchDetails();
                return new Integer(1);
            }

            public void finished() {
                System.out.println("Searching finished");
            }
        };
        worker.start();
    }

    private void bnDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        deleteServer();
    }

    private void bnEditActionPerformed(java.awt.event.ActionEvent evt) {
        editServer();
    }

    private void bnNewActionPerformed(java.awt.event.ActionEvent evt) {
        addServer();
    }

    public void itemStateChanged(ItemEvent e) {
        getServersDetailsNew();
    }

    public int getRenderingReportStatus() {
        return renderingReportStatus;
    }

    public void setRenderingReportStatus(int renderingReportStatus) {
        this.renderingReportStatus = renderingReportStatus;
    }

    public void valueChanged(ListSelectionEvent e) {
        int selrow = serversListTable.getSelectedRow();
        if (selrow != -1) {
            sno = 1;
            resultTableModel.setRowCount(0);
            Vector rVect = (Vector) serversListTable.getValueAt(selrow, 4);
            if (rVect != null) {
                setSearchDetails(rVect);
                setServerGroups();
            }
        }
    }

    public String getCurrServer() {
        return currServer;
    }

    public void setCurrServer(String currServer) {
        this.currServer = currServer;
    }

    public int getCurrrow() {
        return currrow;
    }

    public void setCurrrow(int currrow) {
        this.currrow = currrow;
    }

    public int getSelectedRow() {
        return resultTable.getSelectedRow();
    }

    public Object getCmdHash() {
        int selrow = resultTable.getSelectedRow();
        if (selrow != -1) {
            return resultTable.getValueAt(selrow, 5);
        } else return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equalsIgnoreCase("New")) addServer(); else if (e.getActionCommand().equalsIgnoreCase("Edit")) editServer(); else if (e.getActionCommand().equalsIgnoreCase("Delete")) deleteServer(); else if (e.getActionCommand().equalsIgnoreCase("MoveUp")) moveUpServer(); else if (e.getActionCommand().equalsIgnoreCase("MoveDown")) moveDownServer();
    }

    private javax.swing.JButton bnCancel;

    private javax.swing.JButton bnDelete;

    private javax.swing.JButton bnEdit;

    private javax.swing.JButton bnMore;

    private javax.swing.JButton bnNew;

    private javax.swing.JButton bnSearch;

    private javax.swing.JButton bnStart;

    private javax.swing.JButton bnStop;

    private javax.swing.JComboBox cmbAttribute1;

    private javax.swing.JComboBox cmbAttribute2;

    private javax.swing.JComboBox cmbCategory;

    private javax.swing.JComboBox cmbCountry;

    private javax.swing.JComboBox cmbRelation;

    private javax.swing.JComboBox cmbServerType;

    private javax.swing.JButton jButton1;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JPanel jPanel7;

    private javax.swing.JPanel jPanel8;

    private javax.swing.JPanel jPanel9;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JSplitPane jSplitPane1;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JProgressBar progress;

    private javax.swing.JTable resultTable;

    private javax.swing.JTable serversListTable;

    private javax.swing.JPopupMenu serversPopup;

    private javax.swing.JTextField tfAttribute2;

    private javax.swing.JTextField tfattribute1;

    class MyItemListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            Object source = e.getSource();
            if (source instanceof AbstractButton == false) return;
            boolean checked = e.getStateChange() == ItemEvent.SELECTED;
            for (int x = 0, y = serversListTable.getRowCount(); x < y; x++) {
                serversListTable.setValueAt(new Boolean(checked), x, 0);
            }
        }
    }
}

class CheckBoxHeader extends JCheckBox implements TableCellRenderer, MouseListener {

    protected CheckBoxHeader rendererComponent;

    protected int column;

    protected boolean mousePressed = false;

    public CheckBoxHeader(ItemListener itemListener) {
        rendererComponent = this;
        rendererComponent.addItemListener(itemListener);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                rendererComponent.setForeground(header.getForeground());
                rendererComponent.setBackground(header.getBackground());
                rendererComponent.setFont(header.getFont());
                header.addMouseListener(rendererComponent);
            }
            final Font boldFont = header.getFont().deriveFont(Font.BOLD);
            rendererComponent.setFont(boldFont);
            rendererComponent.setBorder(header.getBorder());
            rendererComponent.setBorderPainted(true);
            rendererComponent.setHorizontalAlignment(SwingConstants.CENTER);
        }
        setColumn(column);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return rendererComponent;
    }

    protected void setColumn(int column) {
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    protected void handleClickEvent(MouseEvent e) {
        if (mousePressed) {
            mousePressed = false;
            JTableHeader header = (JTableHeader) (e.getSource());
            JTable tableView = header.getTable();
            TableColumnModel columnModel = tableView.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = tableView.convertColumnIndexToModel(viewColumn);
            if (viewColumn == this.column && e.getClickCount() == 1 && column != -1) {
                doClick();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        handleClickEvent(e);
        ((JTableHeader) e.getSource()).repaint();
    }

    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
