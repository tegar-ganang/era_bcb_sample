package admin.astor.tools;

import admin.astor.AstorUtil;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoDs.Except;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

public class EventsTable extends JDialog {

    /**
     *	Subscrib mode definitions
     */
    public static final int SUBSCRIBE_CHANGE = 0;

    public static final int SUBSCRIBE_PERIODIC = 1;

    public static final int SUBSCRIBE_ARCHIVE = 2;

    public static final String strMode[] = { "CHANGE", "PERIODIC", "ARCHIVE" };

    /**
     *	Events Table
     */
    public JTable table;

    private DataTableModel model;

    private JScrollPane scrollPane;

    /**
     *	Names of the columns in the table
     */
    private static String[] col_names = { "Signal names", "Read Value", "Mode", "Last Time", "Delta Time", "Delta Value", "Received" };

    public static final int NAME = 0;

    public static final int VALUE = 1;

    public static final int MODE = 2;

    public static final int TIME = 3;

    public static final int DT = 4;

    public static final int DV = 5;

    public static final int CNT = 6;

    private static int[] col_width = { 140, 100, 45, 85, 60, 70, 50 };

    /**
     *	An array of String array for data to be displayed
     */
    private Vector<SubscribedSignal> signals = new Vector<SubscribedSignal>();

    private boolean first = true;

    private TablePopupMenu menu = null;

    private Component parent = null;

    /**
     *	File Chooser Object used in file menu.
     */
    private static JFileChooser chooser;

    private static final String ColWidthHeader = "Column_width:";

    private static final String FileHeader = "#\n#	EventTester :	event list\n#\n";

    public EventsTable(JFrame parent) throws DevFailed {
        super(parent, false);
        this.parent = parent;
        initializeForm();
    }

    public EventsTable(JDialog parent) throws DevFailed {
        super(parent, false);
        this.parent = parent;
        initializeForm();
    }

    private void initializeForm() throws DevFailed {
        initComponents();
        initMyComponents();
        titleLabel.setText("TANGO  Event Tester");
        if (parent != null && parent.isVisible()) {
            Point p = parent.getLocationOnScreen();
            int h = parent.getHeight();
            p.y += h;
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension scrsize = toolkit.getScreenSize();
            if (p.y > (scrsize.height - getHeight() - 20)) p.y -= (h + getHeight());
            setLocation(p);
        }
        fileMenu.setMnemonic('F');
        openFile.setMnemonic('O');
        openFile.setAccelerator(KeyStroke.getKeyStroke('O', Event.CTRL_MASK));
        saveFile.setMnemonic('S');
        saveFile.setAccelerator(KeyStroke.getKeyStroke('S', Event.CTRL_MASK));
        pack();
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        cancelBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openFile = new javax.swing.JMenuItem();
        saveFile = new javax.swing.JMenuItem();
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        cancelBtn.setText("Dismiss");
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });
        jPanel1.add(cancelBtn);
        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        titleLabel.setFont(new java.awt.Font("Dialog", 1, 18));
        titleLabel.setText("Dialog Title");
        jPanel2.add(titleLabel);
        getContentPane().add(jPanel2, java.awt.BorderLayout.NORTH);
        fileMenu.setText("File");
        openFile.setText("Open");
        openFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileActionPerformed(evt);
            }
        });
        fileMenu.add(openFile);
        saveFile.setText("Save");
        saveFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveFileActionPerformed(evt);
            }
        });
        fileMenu.add(saveFile);
        jMenuBar.add(fileMenu);
        setJMenuBar(jMenuBar);
        pack();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void openFileActionPerformed(java.awt.event.ActionEvent evt) {
        if (chooser == null) {
            String homeDir;
            if ((homeDir = System.getProperty("EVT_DATA_FILES")) == null) homeDir = new File("").getAbsolutePath();
            chooser = new JFileChooser(homeDir);
        }
        chooser.setDialogTitle("Open Configuration File");
        chooser.setApproveButtonText("Open");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null && !file.isDirectory()) openEventList(file.getAbsolutePath());
        }
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void saveFileActionPerformed(java.awt.event.ActionEvent evt) {
        if (signals.size() == 0) {
            Utils.popupError(this, "No subscription to save");
            return;
        }
        if (chooser == null) {
            String homeDir;
            if ((homeDir = System.getProperty("EVT_DATA_FILES")) == null) homeDir = new File("").getAbsolutePath();
            chooser = new JFileChooser(homeDir);
        }
        chooser.setDialogTitle("Save Configuration");
        chooser.setApproveButtonText("Save");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null && !file.isDirectory()) {
                if (file.exists()) if (JOptionPane.showConfirmDialog(this, "This File Already Exists !\n\n" + "Would you like to overwrite ?", "information", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                saveEventList(file.getAbsolutePath());
            }
        }
    }

    private void saveEventList(String filename) {
        String str = FileHeader;
        for (SubscribedSignal signal : signals) {
            str += signal.toString() + "\n";
        }
        int[] width = getColumnWidth();
        str += "#\n" + ColWidthHeader + "	";
        for (int aWidth : width) str += " " + aWidth;
        try {
            FileOutputStream fidout = new FileOutputStream(filename);
            fidout.write(str.getBytes());
            fidout.close();
        } catch (Exception e) {
            Utils.popupError(this, null, e);
        }
    }

    private void openEventList(String filename) {
        String str;
        try {
            FileInputStream fid = new FileInputStream(filename);
            int nb = fid.available();
            byte[] inStr = new byte[nb];
            nb = fid.read(inStr);
            if (nb == 0) return;
            str = new String(inStr);
            fid.close();
        } catch (Exception e) {
            Utils.popupError(this, null, e);
            return;
        }
        if (!str.startsWith(FileHeader)) {
            Utils.popupError(this, "This is not an EventTester Configuration file");
            return;
        }
        str = str.substring(FileHeader.length());
        StringTokenizer stk = new StringTokenizer(str, "\n");
        while (stk.hasMoreTokens()) {
            String line = stk.nextToken();
            if (!line.startsWith("#")) if (!line.startsWith(ColWidthHeader)) createSignalFromLine(line); else readColWidthDefinition(line);
        }
        setColumnWidth(col_width);
        pack();
    }

    private void readColWidthDefinition(String line) {
        StringTokenizer stk = new StringTokenizer(line);
        stk.nextToken();
        col_width = new int[stk.countTokens()];
        for (int i = 0; stk.hasMoreTokens(); i++) {
            try {
                String s = stk.nextToken();
                col_width[i] = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Cannot parse width for column " + i);
                col_width[i] = 50;
            }
        }
    }

    private void createSignalFromLine(String line) {
        StringTokenizer stk = new StringTokenizer(line);
        String name = stk.nextToken();
        String strmode = stk.nextToken().substring(1);
        strmode = strmode.substring(0, strmode.indexOf(']'));
        int mode = -1;
        for (int i = 0; i < strMode.length; i++) if (strmode.equals(strMode[i])) mode = i;
        if (mode == -1) {
            Utils.popupError(this, "mode " + strmode + " is unknown !");
            return;
        }
        add(name, mode);
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {
        doClose();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void closeDialog(java.awt.event.WindowEvent evt) {
        doClose();
    }

    private void doClose() {
        if (parent != null) {
            getColumnWidth();
            setVisible(false);
            dispose();
        } else System.exit(0);
    }

    public void add(String name, int mode) {
        for (int i = 0; i < signals.size(); i++) {
            SubscribedSignal sig = signals.get(i);
            if (sig.name.toLowerCase().equals(name.toLowerCase()) && sig.mode == mode) {
                setVisible(true);
                table.changeSelection(i, 0, false, false);
                Utils.popupError(this, "Event \'" + name + "\'  already subscribed");
                return;
            }
        }
        SubscribedSignal sig = new SubscribedSignal(name, mode);
        sig.subscribe(this);
        signals.add(sig);
        setVisible(true);
        updateTable();
        Dimension tbsize = table.getSize();
        tbsize.height += 40;
        scrollPane.setPreferredSize(new Dimension(tbsize));
        pack();
    }

    @SuppressWarnings({ "UnusedDeclaration" })
    private void buildTitle(ActionEvent evt) {
        String title = "Attribute events";
        String date = SubscribedSignal.getStrDate();
        titleLabel.setText(title + " at " + date);
    }

    void updateTable() {
        model.fireTableDataChanged();
    }

    private void initMyComponents() throws DevFailed {
        try {
            model = new DataTableModel();
            table = new JTable(model) {

                public String getToolTipText(MouseEvent e) {
                    String tip = null;
                    Point p = e.getPoint();
                    int row = rowAtPoint(p);
                    int col = columnAtPoint(p);
                    int realColumnIndex = convertColumnIndexToModel(col);
                    SubscribedSignal signal = signals.get(row);
                    switch(realColumnIndex) {
                        case NAME:
                            tip = signal.name;
                            break;
                        case VALUE:
                            if (signal.except == null) tip = signal.value; else tip = signal.except_str();
                            break;
                        case DT:
                            tip = signal.getTimes();
                            break;
                    }
                    return tip;
                }
            };
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getTableHeader().setFont(new java.awt.Font("Dialog", 1, 14));
            table.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    tableActionPerformed(evt);
                }
            });
            scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(700, 50));
            getContentPane().add(scrollPane, java.awt.BorderLayout.CENTER);
            if (first) {
                int delay = 1000;
                ActionListener taskPerformer = new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        buildTitle(evt);
                    }
                };
                new javax.swing.Timer(delay, taskPerformer).start();
                first = false;
            }
            setColumnWidth(col_width);
            pack();
        } catch (Exception e) {
            e.printStackTrace();
            Except.throw_exception("INIT_ERROR", e.toString(), "TestEventTable.initMyComponents()");
        }
        model.fireTableDataChanged();
    }

    private void tableActionPerformed(java.awt.event.MouseEvent evt) {
        if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
            int row = table.rowAtPoint(new Point(evt.getX(), evt.getY()));
            SubscribedSignal signal = signals.get(row);
            if (signal == null) return;
            if (menu == null) menu = new TablePopupMenu(this);
            menu.showMenu(evt, signal);
        }
    }

    void move(SubscribedSignal signal, int direction) {
        int idx = 0;
        for (int i = 0; i < signals.size(); i++) if (signals.get(i).equals(signal)) idx = i;
        switch(direction) {
            case TablePopupMenu.UP:
                if (idx > 0) {
                    signals.remove(signal);
                    signals.add(idx - 1, signal);
                }
                break;
            case TablePopupMenu.DOWN:
                if (idx < (signals.size() - 1)) {
                    signals.remove(signal);
                    signals.add(idx + 1, signal);
                }
                break;
        }
        updateTable();
    }

    void displayHistory(SubscribedSignal signal) {
        new HistoryDialog(this, signal).setVisible(true);
    }

    void displayInfo(SubscribedSignal signal) {
        if (signal.except != null) Utils.popupError(this, null, signal.except); else Utils.popupError(this, signal.status());
    }

    void remove(SubscribedSignal signal) {
        signal.unsubscribe();
        signals.remove(signal);
        updateTable();
        Dimension tbsize = table.getSize();
        tbsize.height += 40;
        scrollPane.setPreferredSize(new Dimension(tbsize));
        pack();
    }

    public void setColumnWidth(int[] width) {
        final Enumeration cenum = table.getColumnModel().getColumns();
        TableColumn tc;
        for (int i = 0; cenum.hasMoreElements(); i++) {
            tc = (TableColumn) cenum.nextElement();
            tc.setPreferredWidth(width[i]);
        }
    }

    public int[] getColumnWidth() {
        final Enumeration cenum = table.getColumnModel().getColumns();
        Vector<TableColumn> v = new Vector<TableColumn>();
        while (cenum.hasMoreElements()) v.add((TableColumn) cenum.nextElement());
        int[] width = new int[v.size()];
        for (int i = 0; i < v.size(); i++) {
            TableColumn tc = v.get(i);
            width[i] = tc.getPreferredWidth();
        }
        return width;
    }

    private javax.swing.JButton cancelBtn;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenuBar jMenuBar;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JMenuItem openFile;

    private javax.swing.JMenuItem saveFile;

    private javax.swing.JLabel titleLabel;

    static void displaySyntax() {
        System.out.println("Syntax:");
        System.out.println("EventsTable -a  <attribute list>");
        System.out.println("EventsTable -f  <file name>");
        System.exit(0);
    }

    public static void main(String[] args) {
        int mode = SUBSCRIBE_ARCHIVE;
        if (args.length > 0) if (args[0].equals("-?")) EventsTable.displaySyntax();
        try {
            EventsTable table = new EventsTable(new JFrame());
            if (args.length > 1) if (args[0].equals("-a")) {
                for (int i = 1; i < args.length; i++) table.add(args[i], mode);
            } else if (args[0].equals("-f")) table.openEventList(args[1]);
            table.setVisible(true);
        } catch (DevFailed e) {
            Except.print_exception(e);
        }
    }

    public class DataTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return col_names.length;
        }

        public int getRowCount() {
            return signals.size();
        }

        public String getColumnName(int aCol) {
            return col_names[aCol];
        }

        public Object getValueAt(int row, int col) {
            SubscribedSignal sig = signals.get(row);
            switch(col) {
                case NAME:
                    return sig.name;
                case VALUE:
                    return sig.value;
                case MODE:
                    return strMode[sig.mode];
                case TIME:
                    return sig.time;
                case DT:
                    return sig.d_time;
                case DV:
                    return sig.d_value;
                case CNT:
                    return "" + sig.cnt;
            }
            return "";
        }
    }
}
