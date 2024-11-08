import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.swing.table.AbstractTableModel;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.*;

public class GoenDB extends JFrame implements ActionListener {

    private JTree tree;

    private JTable table;

    private MyToolbar toolbar;

    private JPopupMenu popup;

    private JButton nextButton, backButton, firstButton, lastButton;

    private JPanel dataPanel;

    private JPanel mydataPanel;

    private JTabbedPane myTab;

    private JScrollPane scrolltree;

    private JScrollPane scrollpane;

    private JScrollPane scroolText;

    private JSplitPane splitSQL;

    private JSplitPane splitMe;

    private JComboBox tableNames;

    private boolean showm = true;

    private ArrayList fields;

    private Connection con;

    private Statement stmt;

    private DatabaseMetaData md;

    private ResultSet rs;

    private ResultSet mrs;

    private MyTableSQL qtm;

    public String tabelData = "";

    private static String myURL = "";

    private static String myDatabase = "";

    private int rowData = 0;

    private int numTabel = 0;

    private int numRecord = 1;

    private JLabel nrecLabel;

    private JTextArea jtextArea;

    private JPanel movePanel;

    private static Properties goenDBprops;

    private static String sfile = "GoenDB.properties";

    final JTextField driverTextfield = new JTextField(20);

    final JTextField urlTextfield = new JTextField(20);

    final JTextField databaseTextfield = new JTextField(20);

    final JTextField useridTextfield = new JTextField(20);

    final JTextField passwdTextfield = new JTextField(20);

    private String MY_TABEL = "";

    public GoenDB() {
        super("GoenDB 1.0 - Database Tool");
        initComponents();
    }

    public void initComponents() {
        fields = new ArrayList();
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
        setIconImage(image);
        final Hashtable hashTable = new Hashtable();
        try {
            con = getConnection();
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            md = con.getMetaData();
            mrs = md.getTables(null, null, null, new String[] { "TABLE" });
            String columnName = " ";
            while (mrs.next()) try {
                rs = stmt.executeQuery("SELECT * FROM " + mrs.getString(3));
                ResultSetMetaData rsmd = rs.getMetaData();
                String[] myTabfield = new String[rsmd.getColumnCount()];
                rowData = rsmd.getColumnCount();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    columnName = rsmd.getColumnLabel(i + 1);
                    myTabfield[i] = columnName;
                }
                hashTable.put(mrs.getString(3), myTabfield);
            } catch (Exception e) {
                warnme("Error due to " + e.getMessage());
            }
            mrs.close();
        } catch (Exception e) {
            warnme("no database open");
        }
        MouseListener mlo = new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                myTab.revalidate();
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                numRecord = 1;
                nrecLabel.setText("Record=" + numRecord);
                try {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    MY_TABEL = selPath.getLastPathComponent().toString();
                    System.out.println("datasel-Path " + MY_TABEL);
                    tabelData = selPath.getLastPathComponent().toString();
                    if (tabelData != myDatabase && tabelData != null) loadLah();
                    if (tabelData != myDatabase) getSomeData(tabelData);
                    System.out.println("datasel-PathChild " + selPath.getPathCount());
                } catch (Exception nu) {
                    ;
                }
            }
        };
        Hashtable worldHash = new Hashtable();
        worldHash.put(myDatabase, hashTable);
        tree = new JTree(worldHash);
        tree.addMouseListener(mlo);
        tree.setRowHeight(20);
        tree.expandRow(0);
        scrolltree = new JScrollPane(tree);
        qtm = new MyTableSQL();
        table = new JTable(qtm);
        scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 300));
        jtextArea = new JTextArea();
        jtextArea.setText(" ");
        JPanel inPanel = new JPanel();
        JButton SQLbutton = new JButton("Run SQL");
        SQLbutton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String mySQL = jtextArea.getText();
                runSQL(mySQL);
            }
        });
        inPanel.setLayout(new BorderLayout());
        mydataPanel = new JPanel();
        mydataPanel.setLayout(new BorderLayout());
        dataPanel = new JPanel();
        nextButton = new JButton("Next >");
        nextButton.addActionListener(this);
        backButton = new JButton("Back <");
        backButton.addActionListener(this);
        firstButton = new JButton("First <<");
        firstButton.addActionListener(this);
        lastButton = new JButton("Last >>");
        lastButton.addActionListener(this);
        movePanel = new JPanel();
        movePanel.setBorder(new TitledBorder(new EtchedBorder(), "Option"));
        movePanel.add(nextButton);
        movePanel.add(backButton);
        nrecLabel = new JLabel("Record=" + numRecord);
        movePanel.add(nrecLabel);
        movePanel.add(firstButton);
        movePanel.add(lastButton);
        mydataPanel.add(movePanel, "South");
        scroolText = new JScrollPane(jtextArea);
        scroolText.setMinimumSize(new Dimension(150, 50));
        scrollpane.setMinimumSize(new Dimension(150, 50));
        splitSQL = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollpane, scroolText);
        inPanel.add("Center", splitSQL);
        inPanel.add("South", SQLbutton);
        myTab = new JTabbedPane();
        myTab.setSize(200, 400);
        myTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        myTab.setBorder(new TitledBorder(new EtchedBorder(), null));
        myTab.addTab("View All", inPanel);
        myTab.addTab("View Some", mydataPanel);
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);
        toolbar = new MyToolbar();
        getContentPane().add(toolbar, BorderLayout.NORTH);
        for (int i = 0; i < toolbar.imageName.length; i++) {
            toolbar.button[i].addActionListener(this);
        }
        scrolltree.setMinimumSize(new Dimension(150, 550));
        myTab.setMinimumSize(new Dimension(150, 550));
        splitMe = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrolltree, myTab);
        getContentPane().add(splitMe, BorderLayout.CENTER);
        pack();
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new java.awt.Dimension(600, 600));
        setLocation((screenSize.width - 600) / 2, (screenSize.height - 600) / 2);
    }

    private void getSomeData(String myTable) {
        remove(dataPanel);
        dataPanel = new JPanel();
        fields.clear();
        try {
            if (rs != null) rs.close();
            rs = stmt.executeQuery("SELECT * FROM " + myTable);
            ResultSetMetaData rsmd = rs.getMetaData();
            dataPanel.setLayout(new GridLayout(rsmd.getColumnCount(), 2, 1, 1));
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String columnName = rsmd.getColumnLabel(i);
                int columnWidth = 20;
                JTextField tb = new JTextField(columnWidth);
                tb.getPreferredSize();
                fields.add(tb);
                dataPanel.add(new JLabel(columnName));
                dataPanel.add(tb);
                tb.isValidateRoot();
            }
        } catch (Exception e) {
            warnme("Error due to " + e.getMessage());
        }
        mydataPanel.add(dataPanel, "Center");
        mydataPanel.revalidate();
        showNextRow();
    }

    protected JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        JMenu mFile = new JMenu("File");
        mFile.setMnemonic('f');
        JMenu mCommand = new JMenu("Command");
        mCommand.setMnemonic('c');
        JMenu mTool = new JMenu("Tool");
        mTool.setMnemonic('t');
        JMenu mAbout = new JMenu("Help");
        mAbout.setMnemonic('h');
        JMenuItem item = new JMenuItem("About GoenDB 1.0");
        item.setMnemonic('A');
        ActionListener lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFrame frame = new JFrame("About GoenDB 1.0");
                ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource("logo.gif"));
                JLabel label1 = new JLabel(icon);
                frame.add("North", label1);
                JLabel label2 = new JLabel("<html><li>GoenDB 1.0� " + "</li><li><p>Ver# 1.0 </li>" + "<li><p>Develop by: Goen-Ghin</li><li><p> http://www.javageo.com </li><li>" + "<p>Copyright<font size=\"2\">�</font> Juli 2007 @Pekanbaru</li></html>");
                label2.setFont(new Font("Tahoma", Font.PLAIN, 11));
                frame.add(label2);
                Toolkit kit = Toolkit.getDefaultToolkit();
                Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
                frame.setIconImage(image);
                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(new java.awt.Dimension(270, 150));
                frame.setLocation((screenSize.width - 270) / 2, (screenSize.height - 150) / 2);
                frame.setVisible(true);
            }
        };
        item.addActionListener(lst);
        mAbout.add(item);
        item = new JMenuItem("Setting");
        item.setMnemonic('S');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFrame frame = new JFrame("Set Configuration");
                frame.setContentPane(JPanelSetting(frame));
                Toolkit kit = Toolkit.getDefaultToolkit();
                Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
                frame.setIconImage(image);
                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                frame.setSize(new java.awt.Dimension(360, 240));
                frame.setLocation((screenSize.width - 360) / 2, (screenSize.height - 240) / 2);
                frame.setVisible(true);
            }
        };
        item.addActionListener(lst);
        mFile.add(item);
        item = new JMenuItem("Exit");
        item.setMnemonic('x');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        item.addActionListener(lst);
        mFile.add(item);
        item = new JMenuItem("SELECT");
        item.setMnemonic('S');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jtextArea.setText("SELECT * FROM " + MY_TABEL);
            }
        };
        item.addActionListener(lst);
        mCommand.add(item);
        item = new JMenuItem("INSERT");
        item.setMnemonic('I');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jtextArea.setText("INSERT INTO " + MY_TABEL + " FROM TABEL");
            }
        };
        item.addActionListener(lst);
        mCommand.add(item);
        item = new JMenuItem("UPDATE");
        item.setMnemonic('U');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jtextArea.setText("UPDATE " + MY_TABEL + " set");
            }
        };
        item.addActionListener(lst);
        mCommand.add(item);
        item = new JMenuItem("DELETE");
        item.setMnemonic('D');
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jtextArea.setText("DELETE from " + MY_TABEL + " [WHERE Expression]");
            }
        };
        item.addActionListener(lst);
        mCommand.add(item);
        item = new JMenuItem("CREATE");
        item.setMnemonic('C');
        mCommand.add(item);
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jtextArea.setText("CREATE TABLE name");
            }
        };
        item.addActionListener(lst);
        mCommand.add(item);
        item = new JMenuItem("Dump");
        item.setMnemonic('D');
        mTool.add(item);
        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dump();
            }
        };
        item.addActionListener(lst);
        mTool.add(item);
        menuBar.add(mFile);
        menuBar.add(mCommand);
        menuBar.add(mTool);
        menuBar.add(mAbout);
        return menuBar;
    }

    public JPanel JPanelSetting(final JFrame j) {
        final JPanel MyPanel = new JPanel();
        MyPanel.setLayout(new BorderLayout());
        MyPanel.setBorder(new TitledBorder(new EtchedBorder(), "Option"));
        JFileChooser filechooser;
        JPanel MyLabelPanel = new JPanel();
        JLabel driverLabel = new JLabel(" Driver : ");
        JLabel urlLabel = new JLabel(" URL : ");
        JLabel databaseLabel = new JLabel(" Database : ");
        JLabel useridLabel = new JLabel(" User Name: ");
        JLabel passwdLabel = new JLabel(" Password: ");
        JPanel MyTextFieldPanel = new JPanel();
        MyLabelPanel.setLayout(new GridLayout(5, 1, 5, 5));
        MyLabelPanel.add(driverLabel);
        MyLabelPanel.add(urlLabel);
        MyLabelPanel.add(databaseLabel);
        MyLabelPanel.add(useridLabel);
        MyLabelPanel.add(passwdLabel);
        MyPanel.add("West", MyLabelPanel);
        MyTextFieldPanel.setLayout(new GridLayout(5, 1, 5, 5));
        MyTextFieldPanel.add(driverTextfield);
        MyTextFieldPanel.add(urlTextfield);
        MyTextFieldPanel.add(databaseTextfield);
        MyTextFieldPanel.add(useridTextfield);
        MyTextFieldPanel.add(passwdTextfield);
        MyPanel.add("East", MyTextFieldPanel);
        JPanel ButtonPanel = new JPanel();
        ButtonPanel.setLayout(new GridLayout(1, 4, 5, 5));
        JButton ApplyButton = new JButton("Apply");
        ApplyButton.addActionListener(this);
        JButton DefaultButton = new JButton("Default");
        DefaultButton.addActionListener(this);
        JButton ResetButton = new JButton("Reset");
        ResetButton.addActionListener(this);
        JButton CloseButton = new JButton("Close");
        CloseButton.addActionListener(this);
        ButtonPanel.add(ApplyButton);
        ButtonPanel.add(DefaultButton);
        ButtonPanel.add(ResetButton);
        ButtonPanel.add(CloseButton);
        MyPanel.add("South", ButtonPanel);
        ApplyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveSetting();
            }
        });
        DefaultButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadSetting();
            }
        });
        ResetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                driverTextfield.setText("");
                urlTextfield.setText("");
                databaseTextfield.setText("");
                useridTextfield.setText("");
                passwdTextfield.setText("");
            }
        });
        CloseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                j.dispose();
            }
        });
        loadSetting();
        return MyPanel;
    }

    public void loadSetting() {
        try {
            goenDBprops = new Properties();
            FileInputStream in = new FileInputStream(sfile);
            goenDBprops.load(in);
            driverTextfield.setText(goenDBprops.getProperty("jdbc.drivers"));
            urlTextfield.setText(goenDBprops.getProperty("jdbc.url"));
            databaseTextfield.setText(goenDBprops.getProperty("jdbc.database"));
            useridTextfield.setText(goenDBprops.getProperty("jdbc.username"));
            passwdTextfield.setText(goenDBprops.getProperty("jdbc.password"));
        } catch (Exception ex) {
            warnme("" + ex.getMessage());
        }
    }

    public void saveSetting() {
        try {
            FileOutputStream outfl = new FileOutputStream(sfile);
            goenDBprops.setProperty("jdbc.drivers", driverTextfield.getText());
            goenDBprops.setProperty("jdbc.url", urlTextfield.getText());
            goenDBprops.setProperty("jdbc.database", databaseTextfield.getText());
            goenDBprops.setProperty("jdbc.username", useridTextfield.getText());
            goenDBprops.setProperty("jdbc.password", passwdTextfield.getText());
            goenDBprops.save(outfl, "GoenDB Ver 1.0 Pekanbaru 2007 - Properties File ");
            outfl.close();
        } catch (IOException ex) {
            warnme("" + ex.getMessage());
        }
    }

    public void showNextRow() {
        if (rs == null) return;
        {
            try {
                if (rs.next()) {
                    for (int i = 1; i <= fields.size(); i++) {
                        String field = rs.getString(i);
                        JTextField tb = (JTextField) fields.get(i - 1);
                        tb.setText(field);
                    }
                } else {
                    rs.previous();
                }
                numTabel = fields.size();
            } catch (Exception e) {
                warnme("Error " + e);
            }
        }
    }

    public void showBackRow() {
        if (rs == null) return;
        {
            try {
                if (rs.previous()) {
                    for (int i = 1; i <= fields.size(); i++) {
                        String field = rs.getString(i);
                        JTextField tb = (JTextField) fields.get(i - 1);
                        tb.setText(field);
                    }
                }
            } catch (Exception e) {
                warnme("Error " + e);
            }
        }
    }

    public void showFirstRow() {
        if (rs == null) return;
        {
            try {
                if (rs.first()) {
                    for (int i = 1; i <= fields.size(); i++) {
                        String field = rs.getString(i);
                        JTextField tb = (JTextField) fields.get(i - 1);
                        tb.setText(field);
                    }
                } else {
                    rs.close();
                    rs = null;
                }
            } catch (Exception e) {
                warnme("Error " + e);
            }
        }
    }

    public void showLastRow() {
        if (rs == null) return;
        {
            try {
                if (rs.last()) {
                    for (int i = 1; i <= fields.size(); i++) {
                        String field = rs.getString(i);
                        JTextField tb = (JTextField) fields.get(i - 1);
                        tb.setText(field);
                    }
                } else {
                    rs.close();
                    rs = null;
                }
            } catch (Exception e) {
                warnme("Error " + e);
            }
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == toolbar.button[0]) {
            Thread runner0 = new Thread() {

                public void run() {
                    try {
                        if (showm == true) {
                            scrolltree.hide();
                            scrolltree.revalidate();
                            splitMe.validate();
                            showm = false;
                        } else if (showm == false) {
                            scrolltree.show();
                            scrolltree.validate();
                            splitMe.validate();
                            splitMe.resetToPreferredSizes();
                            showm = true;
                        }
                    } catch (Exception e) {
                        warnme("Error due to " + e.getMessage());
                    }
                }
            };
            runner0.start();
        }
        if (ae.getSource() == toolbar.button[1] || ae.getSource() == firstButton) {
            try {
                showFirstRow();
                numRecord = rs.getRow();
                nrecLabel.setText("Record=" + numRecord);
            } catch (SQLException esql) {
                warnme("" + esql);
            }
            ;
        }
        if (ae.getSource() == toolbar.button[2] || ae.getSource() == nextButton) {
            try {
                showNextRow();
                numRecord = rs.getRow();
                nrecLabel.setText("Record=" + numRecord);
            } catch (SQLException esql) {
                warnme("" + esql);
            }
            ;
        }
        if (ae.getSource() == toolbar.button[3] || ae.getSource() == backButton) {
            try {
                showBackRow();
                numRecord = rs.getRow();
                if (numRecord > 0) nrecLabel.setText("Record=" + numRecord);
            } catch (SQLException esql) {
                warnme("" + esql);
            }
            ;
        }
        if (ae.getSource() == toolbar.button[4] || ae.getSource() == lastButton) {
            try {
                showLastRow();
                numRecord = rs.getRow();
                nrecLabel.setText("Record=" + numRecord);
            } catch (SQLException esql) {
                warnme("" + esql);
            }
            ;
        }
        if (ae.getSource() == toolbar.button[5]) {
            dump();
        }
        if (ae.getSource() == toolbar.button[6]) {
            Thread runner6 = new Thread() {

                public void run() {
                    try {
                        JFrame frame = new JFrame("Set Configuration");
                        frame.setContentPane(JPanelSetting(frame));
                        Toolkit kit = Toolkit.getDefaultToolkit();
                        Image image = kit.getImage(ClassLoader.getSystemResource("logo.gif"));
                        frame.setIconImage(image);
                        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                        frame.setSize(new java.awt.Dimension(360, 240));
                        frame.setLocation((screenSize.width - 360) / 2, (screenSize.height - 240) / 2);
                        frame.setVisible(true);
                    } catch (Exception e) {
                        warnme("Error due to " + e.getMessage());
                    }
                }
            };
            runner6.start();
        }
        if (ae.getSource() == toolbar.button[7]) {
            Thread runner6 = new Thread() {

                public void run() {
                    try {
                        System.exit(0);
                    } catch (Exception e) {
                        warnme("Error due to " + e.getMessage());
                    }
                }
            };
            runner6.start();
        }
    }

    public static Connection getConnection() throws SQLException, IOException {
        goenDBprops = new Properties();
        FileInputStream in = new FileInputStream(sfile);
        goenDBprops.load(in);
        String drivers = goenDBprops.getProperty("jdbc.drivers");
        if (drivers != null) System.setProperty("jdbc.drivers", drivers);
        myDatabase = goenDBprops.getProperty("jdbc.database");
        myURL = goenDBprops.getProperty("jdbc.url");
        String username = goenDBprops.getProperty("jdbc.username");
        String password = goenDBprops.getProperty("jdbc.password");
        return DriverManager.getConnection(myURL + myDatabase, username, password);
    }

    public void warnme(String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, "Warning", JOptionPane.INFORMATION_MESSAGE);
    }

    public void dump() {
        if (jtextArea.getText() != " ") {
            try {
                File f2 = new File("dump.txt");
                FileWriter fw = new FileWriter(f2);
                if (rs != null) rs.close();
                rs = stmt.executeQuery(jtextArea.getText());
                ResultSetMetaData rsmd = rs.getMetaData();
                fw.write("---------------------------------\n");
                while (rs.next()) {
                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                        String columnName = rsmd.getColumnLabel(i);
                        InputStream bis = rs.getAsciiStream(columnName);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        int ch = 0;
                        while ((ch = bis.read()) != -1) bos.write(ch);
                        System.out.println("DUMP DATA:" + new String(bos.toByteArray()) + "\n");
                        fw.write(columnName + " = " + new String(bos.toByteArray()));
                        fw.write("\n");
                    }
                    fw.write("---------------------------------\n");
                }
                fw.close();
            } catch (Throwable e) {
                warnme("" + e.getMessage());
            }
            warnme("Done dump to file dump.txt");
        }
    }

    void loadLah() {
        qtm.setHostURL(myURL + myDatabase);
        if (tabelData != myDatabase) qtm.setQuery("SELECT * FROM " + tabelData);
    }

    void runSQL(String mySQL) {
        qtm.setHostURL(myURL + myDatabase);
        qtm.setQuery(mySQL);
    }

    class MyToolbar extends JToolBar {

        public JButton[] button;

        public String[] imageName = { "tampilkan.png", "rumah.png", "maju.png", "mundur.png", "endrumah.png", "cetak.png", "atur.png", "keluar.png" };

        public String[] tipText = { "Show/Hide", "Home", "Forward ", "Backward", "End", "Dump", "Option", "Exit" };

        public MyToolbar() {
            button = new JButton[8];
            for (int i = 0; i < imageName.length; i++) {
                add(button[i] = new JButton(new ImageIcon(ClassLoader.getSystemResource(imageName[i]))));
                button[i].setToolTipText(tipText[i]);
            }
        }
    }

    class MyTableSQL extends AbstractTableModel {

        Vector cache;

        int colCount;

        String[] headers;

        Connection db;

        Statement statement;

        String currentURL;

        public MyTableSQL() {
            cache = new Vector();
        }

        public String getColumnName(int i) {
            return headers[i];
        }

        public int getColumnCount() {
            return colCount;
        }

        public int getRowCount() {
            return cache.size();
        }

        public Object getValueAt(int row, int col) {
            return ((String[]) cache.elementAt(row))[col];
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public void setValueAt(Object value, int row, int col) {
            ((String[]) cache.elementAt(row))[col] = (String) value;
            fireTableCellUpdated(row, col);
        }

        protected void testDriver() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                System.out.println("MySQL Driver Found");
            } catch (java.lang.ClassNotFoundException e) {
                System.out.println("MySQL JDBC Driver not found ... ");
            }
        }

        public void setHostURL(String url) {
            if (url.equals(currentURL)) {
                return;
            }
            closeDB();
            initDB(url);
            currentURL = url;
        }

        public void setQuery(String q) {
            cache = new Vector();
            try {
                statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet rs = statement.executeQuery(q);
                ResultSetMetaData meta = rs.getMetaData();
                colCount = meta.getColumnCount();
                headers = new String[colCount];
                for (int h = 1; h <= colCount; h++) {
                    headers[h - 1] = meta.getColumnName(h);
                }
                while (rs.next()) {
                    String[] record = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        record[i] = rs.getString(i + 1);
                    }
                    cache.addElement(record);
                }
                fireTableChanged(null);
            } catch (Exception e) {
                cache = new Vector();
            }
        }

        public void initDB(String url) {
            try {
                db = DriverManager.getConnection(url);
                statement = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } catch (Exception e) {
                System.out.println("Could not initialize the database.");
            }
        }

        public void closeDB() {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (db != null) {
                    db.close();
                }
            } catch (Exception e) {
                System.out.println("Could not close the current connection.");
            }
        }
    }

    public static void main(String args[]) {
        new GoenDB().show();
    }
}
