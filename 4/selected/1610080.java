package org.das2;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.*;
import org.das2.util.filesystem.FileSystemSettings;

public class DasProperties extends Properties {

    private RenderingHints hints;

    private boolean antiAlias;

    private boolean visualCues;

    private Logger logger;

    private static ArrayList propertyOrder;

    private static Editor editor;

    private static JFrame jframe;

    private DasProperties() {
        super();
        hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        setDefaults();
        propertyOrder = new ArrayList();
        if (DasApplication.hasAllPermission() != FileSystemSettings.hasAllPermission()) {
            throw new RuntimeException("DasApplication.hasAllPermission()!=FileSystemSettings.hasAllPermission()");
        }
        if (DasApplication.hasAllPermission()) readPersistentProperties();
        logger = Logger.getLogger("das2");
        setPropertyOrder();
    }

    private void setPropertyOrder() {
        propertyOrder.add(0, "username");
        propertyOrder.add(1, "password");
        propertyOrder.add(2, "debugLevel");
        propertyOrder.add(3, "antiAlias");
        propertyOrder.add(4, "visualCues");
        for (Iterator i = this.keySet().iterator(); i.hasNext(); ) {
            String s = (String) i.next();
            if (!propertyOrder.contains(s)) {
                propertyOrder.add(s);
            }
        }
    }

    private void setDefaults() {
        setProperty("username", "");
        setProperty("password", "");
        setProperty("debugLevel", "endUser");
        setProperty("antiAlias", "off");
        setProperty("visualCues", "off");
        setProperty("defaultServer", "http://www-pw.physics.uiowa.edu/das/dasServer");
    }

    public static RenderingHints getRenderingHints() {
        return instance.hints;
    }

    public static Logger getLogger() {
        return instance.logger;
    }

    public static DasProperties getInstance() {
        return instance;
    }

    private static class DasPropertiesTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return instance.size();
        }

        public Object getValueAt(int row, int col) {
            String propertyName = (String) propertyOrder.get(row);
            String value;
            if (col == 0) {
                value = propertyName;
            } else {
                value = instance.getProperty(propertyName);
                if (propertyName.equals("password")) {
                    value = "";
                }
            }
            return value;
        }

        public void setValueAt(Object value, int row, int col) {
            String propertyName = (String) propertyOrder.get(row);
            if (propertyName.equals("password")) {
                if (!value.toString().equals("")) {
                    value = org.das2.util.Crypt.crypt(value.toString());
                }
            } else if (propertyName.equals("debugLevel")) {
                String debugLevel = value.toString();
                if (debugLevel.equals("endUser")) {
                    Logger.getLogger("").setLevel(Level.WARNING);
                    Logger.getLogger("das2").setLevel(Level.WARNING);
                } else if (debugLevel.equals("dasDeveloper")) {
                    Logger.getLogger("").setLevel(Level.FINE);
                    Logger.getLogger("das2").setLevel(Level.FINE);
                } else instance.logger.setLevel(Level.parse(debugLevel));
                org.das2.util.DasDie.setDebugVerbosityLevel(value.toString());
            }
            instance.setProperty(propertyName, value.toString());
            editor.setDirty(true);
        }

        public boolean isCellEditable(int row, int col) {
            return (col == 1);
        }
    }

    private static TableModel getTableModel() {
        return new DasPropertiesTableModel();
    }

    private static JTable getJTable() {
        return new JTable(getTableModel()) {

            {
                setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        String propertyName = (String) propertyOrder.get(row);
                        if (propertyName.equals("password") && column == 1) {
                            return super.getTableCellRendererComponent(table, "* * * *", isSelected, hasFocus, row, column);
                        } else {
                            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        }
                    }
                });
            }

            public TableCellEditor getCellEditor(int row, int col) {
                String propertyName = (String) propertyOrder.get(row);
                if (propertyName.equals("password")) {
                    return new DefaultCellEditor(new JPasswordField());
                } else if (propertyName.equals("debugLevel")) {
                    String[] data = { "endUser", "dasDeveloper" };
                    return new DefaultCellEditor(new JComboBox(data));
                } else if (propertyName.equals("antiAlias")) {
                    String[] data = { "on", "off" };
                    return new DefaultCellEditor(new JComboBox(data));
                } else if (propertyName.equals("visualCues")) {
                    String[] data = { "on", "off" };
                    return new DefaultCellEditor(new JComboBox(data));
                } else {
                    return super.getCellEditor(row, col);
                }
            }
        };
    }

    private static TableCellEditor getTableCellEditor() {
        return new DefaultCellEditor(new JTextField()) {

            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                String propertyName = (String) propertyOrder.get(row);
                if (propertyName.equals("password")) {
                    return new JPasswordField();
                } else if (propertyName.equals("debugLevel")) {
                    String[] data = { "endUser", "dasDeveloper" };
                    return new JList(data);
                } else {
                    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
                }
            }
        };
    }

    public static class Editor extends JPanel implements ActionListener {

        JButton saveButton;

        Editor() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JTable jtable = getJTable();
            add(jtable);
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
            controlPanel.add(Box.createHorizontalGlue());
            JButton b;
            saveButton = new JButton("Save");
            saveButton.setActionCommand("Save");
            saveButton.addActionListener(this);
            saveButton.setToolTipText("save to $HOME/.das2rc");
            controlPanel.add(saveButton);
            b = new JButton("Dismiss");
            b.addActionListener(this);
            controlPanel.add(b);
            add(Box.createVerticalGlue());
            add(controlPanel);
        }

        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (command.equals("Save")) {
                instance.writePersistentProperties();
                setDirty(false);
            } else if (command.equals("Dismiss")) {
                jframe.dispose();
            }
        }

        public void setDirty(boolean dirty) {
            if (dirty) {
                saveButton.setText("Save*");
            } else {
                saveButton.setText("Save");
            }
        }
    }

    public static void showEditor() {
        jframe = new JFrame("Das Properities");
        editor = new Editor();
        jframe.setSize(400, 300);
        jframe.setContentPane(editor);
        jframe.setVisible(true);
    }

    private static final DasProperties instance = new DasProperties();

    public void readPersistentProperties() {
        try {
            String file = System.getProperty("user.home") + System.getProperty("file.separator") + ".das2rc";
            File f = new File(file);
            if (f.canRead()) {
                try {
                    InputStream in = new FileInputStream(f);
                    load(in);
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    org.das2.util.DasExceptionHandler.handle(e);
                }
            } else {
                if (!f.exists() && f.canWrite()) {
                    try {
                        OutputStream out = new FileOutputStream(f);
                        store(out, "");
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        org.das2.util.DasExceptionHandler.handle(e);
                    }
                } else {
                    System.err.println("Unable to read or write " + file + ".  Using defaults.");
                }
            }
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    public void writePersistentProperties() {
        String file = System.getProperty("user.home") + System.getProperty("file.separator") + ".das2rc";
        File f = new File(file);
        if (f.canWrite()) {
            org.das2.util.DasDie.println("Attempt to write .das2rc...");
            try {
                OutputStream out = new FileOutputStream(f);
                store(out, "");
                out.close();
            } catch (IOException e) {
                org.das2.util.DasExceptionHandler.handle(e);
            }
        } else {
            DasException e = new org.das2.DasIOException("Can't write to file " + f);
            org.das2.util.DasExceptionHandler.handle(e);
        }
    }
}
