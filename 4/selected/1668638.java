package fiswidgets.fisutils;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.io.*;
import fiswidgets.fisgui.FisFrame;

/**
 * TableView is a table that can load in tab-delimited text files for viewing, manipulation, and writing out.
 *
 */
public class FisTableView extends FisFrame implements ActionListener, WindowListener {

    private JTable table, headerColumn;

    private FisTableViewModel tvm = null;

    protected boolean changed = false;

    private Vector allStrings;

    private Vector colNames;

    private Vector rowNames;

    private boolean useRowNames = false;

    private int rowlength = 0;

    private int collength = 0;

    private String infile = null;

    private String savefile = null;

    private boolean writable = true;

    private boolean changeable = false;

    private boolean ignoreColNames = false;

    private String delimiters = "\t";

    private boolean standAlone = false;

    private boolean overwrite = false;

    private Container pane;

    private JScrollPane scroll;

    private JMenuItem save, reload, addcol, removecol, removerow, saveas;

    private boolean savereload = true, addremove = true;

    private boolean ignoreFirstLine = false;

    private boolean createdfromfile = false;

    private String[] titles = null;

    private boolean showit = true;

    private boolean rowselect = false, colselect = true;

    private boolean showMenu = true;

    private TableColumnModel cm = new DefaultTableColumnModel() {

        boolean first = true;

        public void addColumn(TableColumn tc) {
            if (first) {
                first = false;
                return;
            }
            super.addColumn(tc);
        }
    };

    private TableColumnModel rowHeaderModel = new DefaultTableColumnModel() {

        boolean first = true;

        public void addColumn(TableColumn tc) {
            if (first) {
                super.addColumn(tc);
                first = false;
            }
        }
    };

    /**
     * Allows the user to specify if this should be shown right away.
     * default is true.
     * @param showit boolean value for display after creation.
     */
    public void setShow(boolean showit) {
        this.showit = showit;
    }

    /**
     * This returns the JTable that contains the table, so you could place into other
     * containers.
       */
    public JTable getTable() {
        return table;
    }

    /**
     * Constructor to create a TableView instance.
     */
    public FisTableView() {
        super();
        setTitle("Table View");
        allStrings = new Vector();
        colNames = new Vector();
        rowNames = new Vector();
    }

    /**
     * setChangeable is used to set a flag for whether the data in the table is editable or not
     * @param changeable is a boolean for editable data in the table
     */
    public void setChangeable(boolean changeable) {
        this.changeable = changeable;
    }

    /**
     * setOverwrite will set the flag to allow overwriting of the file that is currently open
     * @param overwrite boolean flag for overwriting file, default is false
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * setFile is used to set the file that will be opened into the table
     * @param setFile is a String file name of the file to be opened
     */
    public void setFile(String file) {
        infile = file;
    }

    /**
     * setDelimiters is used to set the delimiters of the file to be put into the table,
     * the default delimiter is '\t' so an example of delimiters is '\t .'
     * @param delimiters a String of delimiters to be used in reading in the file.
     */
    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }

    /**
     * useFirstRowAsColNames will use the first row as column names if it is true, default is false
     * @param use boolean value to use the first row as col names
     */
    public void useFirstRowAsColNames(boolean use) {
        ignoreColNames = !use;
    }

    /**
     * setIgnoreColNames will not use the first row as column names if it is true, default is false
     * @param ignore boolean value to not use the first row as col names
     */
    public void setIgnoreColNames(boolean ignore) {
        ignoreColNames = ignore;
    }

    public boolean ignoreColNames() {
        return ignoreColNames;
    }

    /**
     * createTable sets up, creates, and shows the table from the given file and delimiters.
     */
    public void createTable() throws Exception {
        allStrings.removeAllElements();
        if (infile == null) {
            throw new Exception("There is no file to open!");
        }
        File file = new File(infile);
        if (!file.exists()) {
            throw new Exception("The file " + infile + " does not exist.");
        }
        if (!file.canRead()) {
            throw new Exception("Cannot read file " + infile + ".");
        }
        if (!file.canWrite()) {
            writable = false;
        }
        if (!readInFile(file)) {
            throw new Exception("There was a problem reading in the file.");
        }
        createAndShow();
        createdfromfile = true;
    }

    private int findLongestRowName() {
        int tmp, lsize = 0;
        for (int i = 0; i < rowNames.size(); i++) {
            tmp = rowNames.elementAt(i).toString().length();
            if (lsize < tmp) lsize = tmp;
        }
        return lsize;
    }

    private void createAndShow() {
        addWindowListener(this);
        JMenuBar menu = new JMenuBar();
        JMenu filemenu = new JMenu("File");
        save = new JMenuItem("Save");
        save.addActionListener(this);
        saveas = new JMenuItem("Save As");
        saveas.setActionCommand("saveas");
        saveas.addActionListener(this);
        filemenu.add(save);
        filemenu.add(saveas);
        JMenuItem print = new JMenuItem("Print");
        print.setActionCommand("print");
        print.addActionListener(this);
        reload = new JMenuItem("Reload");
        reload.addActionListener(this);
        filemenu.add(reload);
        filemenu.add(new JSeparator());
        JMenuItem close = new JMenuItem("Close");
        close.addActionListener(this);
        filemenu.add(close);
        menu.add(filemenu);
        JMenu options = new JMenu("Options");
        addcol = new JMenuItem("Add Column");
        addcol.setActionCommand("addcol");
        addcol.addActionListener(this);
        options.add(addcol);
        removecol = new JMenuItem("Remove Selected Column(s)");
        removecol.setActionCommand("removecol");
        removecol.addActionListener(this);
        options.add(removecol);
        removerow = new JMenuItem("Remove Selected Row(s)");
        removerow.setActionCommand("removerow");
        removerow.addActionListener(this);
        options.add(removerow);
        menu.add(options);
        if (showMenu) setJMenuBar(menu);
        addcol.setEnabled(addremove);
        removecol.setEnabled(addremove);
        removerow.setEnabled(addremove);
        save.setEnabled(savereload);
        if (createdfromfile) reload.setEnabled(savereload); else reload.setEnabled(false);
        tvm = new FisTableViewModel(this);
        table = new JTable(tvm);
        table.setRowSelectionAllowed(rowselect);
        table.setColumnSelectionAllowed(colselect);
        table.setCellSelectionEnabled(true);
        new ExcelAdapter(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                int x = table.columnAtPoint(e.getPoint());
                if (!table.isColumnSelected(x)) table.addColumnSelectionInterval(x, x); else table.removeColumnSelectionInterval(x, x);
            }
        });
        TableModel htm = new AbstractTableModel() {

            public int getColumnCount() {
                return 1;
            }

            public int getRowCount() {
                return tvm.getRowCount();
            }

            public Object getValueAt(int r, int c) {
                if (useRowNames && r < rowNames.size()) {
                    return " " + rowNames.elementAt(r).toString() + "  ";
                } else {
                    return ("" + (r + 1) + "  ");
                }
            }
        };
        headerColumn = new JTable(htm, rowHeaderModel);
        headerColumn.createDefaultColumnsFromModel();
        headerColumn.setSelectionModel(table.getSelectionModel());
        headerColumn.setBackground(Color.lightGray);
        headerColumn.setSelectionBackground(Color.lightGray);
        headerColumn.setColumnSelectionAllowed(false);
        headerColumn.setRowSelectionAllowed(false);
        headerColumn.setCellSelectionEnabled(false);
        headerColumn.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int nrows = htm.getRowCount();
        int fw = getFontMetrics(table.getFont()).charWidth('0') + 2;
        if (useRowNames) {
            headerColumn.setPreferredSize(new Dimension(9 * findLongestRowName() + 3, 17 * nrows));
        } else headerColumn.setPreferredSize(new Dimension(fw * Integer.toString(nrows).length() + fw, 17 * nrows));
        JViewport jv = new JViewport();
        jv.setView(headerColumn);
        jv.setPreferredSize(headerColumn.getPreferredSize());
        pane = getContentPane();
        scroll = new JScrollPane(table);
        scroll.setRowHeader(jv);
        pane.add(scroll);
        pack();
        Dimension s = new Dimension(table.getSize().width + headerColumn.getSize().width, table.getSize().height + headerColumn.getSize().height);
        if (s.width > 600) {
            scroll.setPreferredSize(new Dimension(600, s.height + 5));
            s = getSize();
        }
        if (s.height > 500) {
            scroll.setPreferredSize(new Dimension(s.width + 5, 500));
            s = getSize();
        }
        if (s.width < 600 && s.height < 500) {
            scroll.setPreferredSize(new Dimension(s.width + 5, s.height + 5));
        }
        if (showit) {
            setVisible(true);
            setLocation(50, 50);
        }
    }

    /**
     * Display/don't display Menu
     */
    public void setMenuEnabled(boolean value) {
        showMenu = value;
    }

    /**
     * Sets the row selection allowed variable of the table.
     */
    public void setRowSelectionAllowed(boolean value) {
        rowselect = value;
    }

    /**
     * Sets the column selection allowed variable of the table.
     */
    public void setColumnSelectionAllowed(boolean value) {
        colselect = value;
    }

    /**
     * This createTableWith method is used to create a table out of a two dimensional float array that 
     * is already in memory.  This 2d array should be cols, rows.
     * @param floats this is the 2d array of floats in memory to be displayed in the table
     */
    public void createTableWith(float[][] floats) throws Exception {
        allStrings.removeAllElements();
        rowlength = floats.length;
        collength = floats[0].length;
        for (int i = 0; i < floats.length; i++) {
            if (collength < floats[i].length) {
                collength = floats[i].length;
            }
        }
        doTitles();
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (floats[row].length > col) allStrings.addElement("" + floats[row][col]); else allStrings.addElement("");
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        createAndShow();
    }

    /**
     * Returns the Column Names of the table.
     */
    public Vector getColumnNames() {
        return colNames;
    }

    /**
     * Returns the Row Names of the table.
     */
    public Vector getRowNames() {
        return rowNames;
    }

    /**
     * creates a table with a double string array for rows, cols.
     * @param strings the double array of rows, cols
     */
    public void createTableWith(String[][] strings) {
        allStrings.removeAllElements();
        rowlength = strings.length;
        collength = strings[0].length;
        for (int i = 0; i < strings.length; i++) {
            if (collength < strings[i].length) {
                collength = strings[i].length;
            }
        }
        doTitles();
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (strings[row].length > col) allStrings.addElement(strings[row][col]); else allStrings.addElement("");
                }
            }
        } catch (Exception ex) {
        }
        createAndShow();
    }

    private void doTitles() {
        colNames.removeAllElements();
        if (titles == null) {
            for (int i = 0; i < rowlength; i++) colNames.addElement("Column " + i);
        } else {
            for (int i = 0; i < titles.length; i++) {
                if (i < rowlength) {
                    colNames.addElement(titles[i]);
                }
            }
        }
    }

    /**
     * This will set the column titles of the table.
     * @param titles these are the titles of the columns to be used
     */
    public void setColumnTitles(String[] titles) {
        this.titles = titles;
        colNames.removeAllElements();
        for (int i = 0; i < titles.length; i++) {
            if (i < rowlength) {
                colNames.addElement(titles[i]);
            }
        }
    }

    /**
     * This will set the row titles of the table.
     * @param titles these are the titles of the columns to be used
     */
    public void setRowTitles(String[] titles) {
        useRowNames = true;
        rowNames.removeAllElements();
        for (int i = 0; i < titles.length; i++) {
            rowNames.addElement(titles[i]);
        }
    }

    /**
     * This allows the table to be updated without destroying the window, this is to be used with tables
     * that are created with data from memory.
     * @param floats this is the 2d array of floats in memory to update the table with, should be rows, cols.
     */
    public void updateWith(float[][] floats) {
        allStrings.removeAllElements();
        rowlength = floats.length;
        collength = floats[0].length;
        for (int i = 0; i < floats.length; i++) {
            if (collength < floats[i].length) {
                collength = floats[i].length;
            }
        }
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (floats[row].length > col) allStrings.addElement("" + floats[row][col]); else allStrings.addElement("");
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        table.repaint();
    }

    /**
     *  Update table from memory w/ strings.
     *  This method should be used when the number of columns has
     *  changed.
     *  @param strings [][] -- 2D array of strings
     *  @param titles  []   -- column headings
     */
    public void updateWith(String[][] strings, String[] cnames) {
        allStrings.removeAllElements();
        rowlength = strings.length;
        collength = strings[0].length;
        for (int i = 0; i < strings.length; i++) {
            if (collength < strings[i].length) {
                collength = strings[i].length;
            }
        }
        titles = cnames;
        doTitles();
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (strings[row].length > col) allStrings.addElement(strings[row][col]); else allStrings.addElement("");
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        table.setModel(new FisTableViewModel(this));
        table.repaint();
    }

    /**
     *  Update table from memory w/ strings.
     *  This method should be used when the number of columns did not
     *  change.
     *  @param strings[][] -- 2D array of strings 
     */
    public void updateWith(String[][] strings) {
        allStrings.removeAllElements();
        rowlength = strings.length;
        collength = strings[0].length;
        for (int i = 0; i < strings.length; i++) {
            if (collength < strings[i].length) {
                collength = strings[i].length;
            }
        }
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (strings[row].length > col) allStrings.addElement(strings[row][col]); else allStrings.addElement("");
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        table.repaint();
    }

    /**
     * This method will create a Table out of floats in memory with a particular rowlength.
     * @param floats this is the array of floats that the table will be made up with.
     * @param rowlength this is the row length of the table.
     */
    public void createTableWith(float[] floats, int rowlength) {
        allStrings.removeAllElements();
        this.rowlength = rowlength;
        collength = (int) (floats.length / rowlength);
        doTitles();
        for (int i = 0; i < floats.length; i++) {
            allStrings.addElement("" + floats[i]);
        }
        createAndShow();
    }

    /**
     * This method creates a Table out of the Strings in the Vector with a particular rowlength.
     * @param vec this is the vector of Strings that will make up the table.
     * @param rowlength this is the row length of the table.
     */
    public void createTableWith(Vector vec, int rowlength) {
        allStrings.removeAllElements();
        this.rowlength = rowlength;
        collength = (int) (vec.size() / rowlength);
        allStrings = vec;
        doTitles();
        createAndShow();
    }

    /**
     * This createTableWith method is used to create a table out of a two dimensional int array that 
     * is already in memory.  This 2d array should be rows, cols.
     * @param ints this is the 2d array of ints in memory to be displayed in the table
     */
    public void createTableWith(int[][] ints) throws Exception {
        allStrings.removeAllElements();
        rowlength = ints.length;
        collength = ints[0].length;
        for (int i = 0; i < ints.length; i++) {
            if (collength < ints[i].length) {
                collength = ints[i].length;
            }
        }
        doTitles();
        try {
            for (int col = 0; col < collength; col++) {
                for (int row = 0; row < rowlength; row++) {
                    if (ints[row].length > col) allStrings.addElement("" + ints[row][col]); else allStrings.addElement("");
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        createAndShow();
    }

    /**
     * This method will create a Table out of ints in memory with a particular rowlength.
     * @param ints this is the array of ints that the table will be made up with.
     * @param rowlength this is the row length of the table.
     */
    public void createTableWith(int[] ints, int rowlength) {
        allStrings.removeAllElements();
        this.rowlength = rowlength;
        collength = (int) (ints.length / rowlength);
        doTitles();
        for (int i = 0; i < ints.length; i++) {
            allStrings.addElement("" + ints[i]);
        }
        createAndShow();
    }

    public int getRowLength() {
        return rowlength;
    }

    public int getColLength() {
        return collength;
    }

    public Vector getAllStrings() {
        return allStrings;
    }

    /**
     * Will disable/enable the add and remove column file options, default is enabled.
     */
    public void setAddRemove(boolean b) {
        addremove = b;
        if (isShowing()) {
            addcol.setEnabled(b);
            removecol.setEnabled(b);
        }
    }

    /**
     * Will enable diable save and reload in the menu, default is enabled.
     */
    public void setSaveReload(boolean b) {
        savereload = b;
        if (isShowing()) {
            save.setEnabled(b);
            reload.setEnabled(b);
        }
    }

    public void setIgnoreFirstLine(boolean b) {
        ignoreFirstLine = b;
    }

    private boolean readInFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String token;
            boolean firstline = true;
            int count = 0;
            int thecount = 0;
            StringTokenizer st;
            while (reader.ready()) {
                st = new StringTokenizer(reader.readLine(), delimiters, true);
                count = 0;
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (delimiters.indexOf(token) < 0) {
                        allStrings.addElement(token);
                        count++;
                    } else {
                        boolean nodelimit = true;
                        while (nodelimit) {
                            if (st.hasMoreTokens()) {
                                token = st.nextToken();
                                if (delimiters.indexOf(token) >= 0) {
                                    allStrings.addElement("");
                                    count++;
                                } else {
                                    allStrings.addElement(token);
                                    nodelimit = false;
                                    count++;
                                }
                            } else {
                                allStrings.addElement("");
                                nodelimit = false;
                                count++;
                            }
                        }
                    }
                }
                if (firstline && ignoreFirstLine) {
                    rowlength = allStrings.size();
                    thecount = count;
                    ignoreFirstLine = false;
                } else if (firstline && rowlength == 0) {
                    rowlength = allStrings.size();
                    thecount = count;
                    firstline = false;
                } else {
                    for (int i = count; i < thecount; i++) allStrings.addElement("");
                }
            }
            reader.close();
            if (ignoreColNames()) {
                for (int i = 0; i < rowlength; i++) colNames.addElement("Column " + (i + 1));
            } else {
                for (int i = 0; i < rowlength; i++) {
                    colNames.addElement(allStrings.firstElement());
                    allStrings.removeElementAt(0);
                }
            }
            collength = (int) (allStrings.size() / rowlength);
            savefile = file.toString();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isChangeable() {
        return changeable;
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        if (action.equals("Save")) {
            doSave();
        } else if (action.equals("saveas")) {
            doSaveAs();
        } else if (action.equals("Close")) {
            doingClose();
        } else if (action.equals("addcol")) {
            addColumn();
        } else if (action.equals("removecol")) {
            removeColumn();
        } else if (action.equals("removerow")) {
            removeRow();
        } else if (action.equals("print")) {
            printIt();
        } else if (action.equals("Reload")) {
            if (createdfromfile) {
                dispose();
                pane.remove(scroll);
                allStrings.removeAllElements();
                try {
                    createTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Could not reload the table.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "There is no reload for values from memory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void printIt() {
        PrintJob pj = getToolkit().getPrintJob(this, "Print", null);
        if (pj == null) return;
        Graphics g = pj.getGraphics();
        g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
        table.printAll(g);
        g.dispose();
        pj.end();
    }

    public void removeColumn() {
        int[] sc = table.getSelectedColumns();
        for (int i = sc.length; i > 0; i--) {
            try {
                TableColumn col, remcol = null;
                String temp;
                int remindex;
                remcol = table.getColumn(table.getColumnName(sc[i - 1]));
                remindex = remcol.getModelIndex();
                for (int j = 0; j < rowlength; j++) {
                    temp = table.getColumnName(j);
                    col = table.getColumn(temp);
                    if (col.getModelIndex() > remindex) col.setModelIndex(col.getModelIndex() - 1);
                }
                table.removeColumn(remcol);
                colNames.removeElementAt(remindex);
                rowlength--;
                for (int j = 0; j < collength; j++) {
                    allStrings.removeElementAt(remindex + (j * rowlength));
                }
                changed = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeRow() {
        int[] sr = table.getSelectedRows();
        for (int i = sr.length; i > 0; i--) {
            try {
                int remindex = sr[i - 1];
                remindex *= rowlength;
                for (int j = 0; j < rowlength; j++) allStrings.removeElementAt(remindex);
                collength--;
                remindex /= rowlength;
                tvm.fireTableRowsDeleted(remindex, remindex);
                changed = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addColumn() {
        if (!isChangeable()) return;
        String name = JOptionPane.showInputDialog("Please input a column name:");
        if (name != null) {
            colNames.insertElementAt(name, rowlength);
            rowlength++;
            for (int i = 0; i < collength - 1; i++) {
                allStrings.insertElementAt("", rowlength + (i * rowlength) - 1);
            }
            TableColumn col = new TableColumn(rowlength - 1);
            table.addColumn(col);
            changed = true;
        }
    }

    public void doingClose() {
        if (changed && !showMenu) {
            int answer = JOptionPane.showConfirmDialog(this, "Values have changed; are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) dispose();
        } else {
            dispose();
        }
    }

    public void doSave() {
        if (savefile == null) {
            String dir = System.getProperty("user.dir");
            JFileChooser jfc = new JFileChooser(dir);
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int answer = 0;
            if (isShowing()) answer = jfc.showSaveDialog(this); else answer = jfc.showSaveDialog(null);
            if (answer == JFileChooser.APPROVE_OPTION) {
                File file = jfc.getSelectedFile();
                if (infile != null) {
                    if (file.toString().equals(infile) && !overwrite) {
                        if (isShowing()) JOptionPane.showMessageDialog(this, "File already exists, cannot overwrite.", "Error!", JOptionPane.ERROR_MESSAGE); else JOptionPane.showMessageDialog(null, "File already exists, cannot overwrite.", "Error!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                savefile = file.toString();
            }
        }
        if (savefile != null && savefile.length() != 0) {
            writeTabFile(savefile);
        }
    }

    private void writeTabFile(String fileToSaveTo) {
        try {
            FileWriter writer = new FileWriter(fileToSaveTo, false);
            boolean first = true;
            for (int i = 0; i < allStrings.size(); i++) {
                if (i > 0 && i % rowlength == 0) {
                    writer.write("\n");
                    first = true;
                } else if (!first) {
                    writer.write("\t");
                }
                writer.write((String) allStrings.elementAt(i));
                first = false;
            }
            writer.write("\n");
            writer.flush();
            writer.close();
            changed = false;
        } catch (Exception e) {
            if (isShowing()) JOptionPane.showMessageDialog(this, "Could not save the file.", "Error!", JOptionPane.ERROR_MESSAGE); else JOptionPane.showMessageDialog(null, "Could not save the file.", "Error!", JOptionPane.ERROR_MESSAGE);
            infile = null;
        }
    }

    public void doSaveAs() {
        String dir = System.getProperty("user.dir");
        JFileChooser jfc = new JFileChooser(dir);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int answer = 0;
        if (isShowing()) answer = jfc.showSaveDialog(this); else answer = jfc.showSaveDialog(null);
        if (answer == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            if (infile != null) {
                if (file.toString().equals(infile) && !overwrite) {
                    if (isShowing()) JOptionPane.showMessageDialog(this, "File already exists, cannot overwrite.", "Error!", JOptionPane.ERROR_MESSAGE); else JOptionPane.showMessageDialog(null, "File already exists, cannot overwrite.", "Error!", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            savefile = file.toString();
            infile = file.toString();
            writeTabFile(savefile);
            save.setEnabled(true);
        }
    }

    public boolean isStandAlone() {
        return standAlone;
    }

    /**
     *  Get all elements from FisTableView as 2-dim array of Strings.
     */
    public String[][] getElements() {
        int k = 0;
        String[][] elem = new String[rowlength][collength];
        for (int i = 0; i < collength; i++) for (int j = 0; j < rowlength; j++) elem[j][i] = allStrings.elementAt(k++).toString();
        return elem;
    }

    /**
     *  Get selected column.
     */
    public int getSelectedColumn() {
        return table.getSelectedColumn();
    }

    /**
     *  Get selected columns.
     */
    public int[] getSelectedColumns() {
        return table.getSelectedColumns();
    }

    /**
      * setStandAlone is used to set the boolean that is used to determine if
      * this program is a standalone program and System.exit(0) should be used
      * or if it is internal to another and only dispose() should be used
      * @param alone boolean value for stand alone program
      */
    public void setStandAlone(boolean alone) {
        standAlone = alone;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        doingClose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void kill() {
        if (allStrings != null) {
            allStrings.removeAllElements();
            allStrings = null;
        }
        titles = null;
        if (tvm != null) {
            if (tvm.strings != null) {
                tvm.strings.removeAllElements();
                tvm.strings = null;
            }
        }
        if (isShowing()) dispose();
        if (isStandAlone()) {
            System.exit(0);
        }
    }
}

class FisTableViewModel extends AbstractTableModel implements Serializable {

    private FisTableView view;

    public Vector strings;

    private Vector cols;

    public FisTableViewModel(FisTableView tv) {
        view = tv;
        strings = view.getAllStrings();
        cols = view.getColumnNames();
    }

    public boolean isCellEditable(int r, int c) {
        return view.isChangeable();
    }

    public int getRowCount() {
        return view.getColLength();
    }

    public int getColumnCount() {
        return view.getRowLength();
    }

    public String getColumnName(int columnIndex) {
        return (String) cols.elementAt(columnIndex);
    }

    public Object getValueAt(int r, int c) {
        int pos = (r * view.getRowLength()) + c;
        if (pos < strings.size()) return (String) strings.elementAt(pos);
        return "";
    }

    public void setValueAt(Object v, int r, int c) {
        int pos = (r * view.getRowLength()) + c;
        strings.removeElementAt(pos);
        strings.insertElementAt((String) v, pos);
        view.changed = true;
    }
}
