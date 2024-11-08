package tool_panels.atktable;

import fr.esrf.tangoatk.widget.attribute.Trend;
import fr.esrf.tangoatk.widget.util.ErrorPane;
import fr.esrf.tangoatk.widget.util.Splash;
import tool_panels.tools.Utils;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

public class TableScalarViewer extends JPanel {

    /**
	 *	File Chooser Object used in file menu.
	 */
    private static JFileChooser chooser = null;

    private static AtkTableFilter file_filter;

    private static final String[] FileExt = { "atp" };

    static final String AppName = "atktable";

    private boolean editable = true;

    private ScrolledScalarViewer viewer = null;

    private DevBrowser browser = null;

    private JButton errorBtn;

    private JLabel titleLabel;

    public static final int FROM_FILE = 0;

    public static final int FROM_DEVICE = 1;

    public TableScalarViewer() {
        initComponents();
        TableConfig config = new TableConfig(null, 3, 2);
        initScalarViewer(config);
    }

    public TableScalarViewer(String name, int type) {
        initComponents();
        TableConfig config;
        try {
            config = readConfig(name, type);
        } catch (Exception e) {
            ErrorPane.showErrorMessage(this, null, e);
            config = new TableConfig(null, 3, 2);
        }
        if (config != null) {
            initScalarViewer(config);
            titleLabel.setText(config.title);
        }
    }

    public TableScalarViewer(String[] rownames, String[] colnames, String[][] attnames) {
        this(rownames, colnames, attnames, null);
    }

    public TableScalarViewer(String[] rownames, String[] colnames, String[][] attnames, Dimension d) {
        initComponents();
        TableConfig config = new TableConfig(rownames, colnames, attnames);
        if (d != null) {
            config.width = d.width;
            config.height = d.height;
        }
        initScalarViewer(config);
        titleLabel.setText(config.title);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        titleLabel = new JLabel(new TableConfig().title);
        titleLabel.setFont(new Font("Dialog", 1, 18));
        errorBtn = new JButton("");
        errorBtn.setBorderPainted(false);
        errorBtn.setContentAreaFilled(false);
        errorBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        errorBtn.setVisible(false);
        errorBtn.setIcon(Utils.getInstance().getIcon("redball.gif"));
        errorBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorBtnActionPerformed(evt);
            }
        });
        JPanel panel = new JPanel();
        panel.add(titleLabel);
        panel.add(new JLabel("      "));
        panel.add(errorBtn);
        add(panel, BorderLayout.NORTH);
        viewer = new ScrolledScalarViewer(this);
        add(viewer, BorderLayout.CENTER);
    }

    public void errorChanged(boolean on_error) {
        errorBtn.setVisible(on_error);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        editable = b;
    }

    public void setPanelTitle(String title) {
        if (viewer != null) {
            viewer.config.title = title;
            titleLabel.setText(title);
        }
    }

    public void setPanelTitleFont(Font font) {
        titleLabel.setFont(font);
    }

    public void setPanelTitleVisible(boolean b) {
        titleLabel.getParent().setVisible(b);
    }

    public void setSize(Dimension d) {
        viewer.table.setPreferredScrollableViewportSize(d);
        getMainContainer(this).pack();
    }

    private void initScalarViewer(TableConfig config) {
        viewer.initializeViewer(config);
        titleLabel.setText(config.title);
    }

    public TableConfig readConfig(String name, int src) {
        try {
            return new TableConfig(name, src);
        } catch (Exception e) {
            if (src == FROM_FILE) ErrorPane.showErrorMessage(this, "Reading " + name, e); else ErrorPane.showErrorMessage(this, null, e);
            return null;
        }
    }

    public void writeConfig(String filename) {
        try {
            viewer.updateConfigSize();
            viewer.config.save(filename);
            Utils.popupMessage(this, "Configuration has been saved in \n" + filename);
        } catch (Exception e) {
            ErrorPane.showErrorMessage(this, "Writing " + filename, e);
        }
    }

    private void errorBtnActionPerformed(java.awt.event.ActionEvent evt) {
        showErrorHistory();
    }

    private void initChooser() {
        String path = System.getProperty("ATK_TABLE_HOME");
        if (path == null) path = "";
        chooser = new JFileChooser(new File(path).getAbsolutePath());
        file_filter = new AtkTableFilter(FileExt, "AtkTable Files");
        chooser.addChoosableFileFilter(file_filter);
    }

    boolean openConfigFile() {
        if (chooser == null) initChooser();
        chooser.setFileFilter(file_filter);
        int retval = chooser.showOpenDialog(this);
        if (retval == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                if (!file.isDirectory()) {
                    String filename = file.getAbsolutePath();
                    TableConfig config = readConfig(filename, FROM_FILE);
                    if (config != null) {
                        initScalarViewer(config);
                        getMainContainer(this).pack();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void saveConfigFile() {
        if (chooser == null) initChooser();
        chooser.setFileFilter(file_filter);
        int retval = chooser.showSaveDialog(this);
        if (retval == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                String filename = file.toString();
                if (!filename.endsWith("." + FileExt[0])) {
                    filename += "." + FileExt[0];
                    file = new File(filename);
                }
                if (file.exists()) {
                    if (JOptionPane.showConfirmDialog(this, "File " + filename + "\nAlready exists !     Overwrite it  ?", "Overwrite ?", JOptionPane.YES_NO_OPTION) != JOptionPane.OK_OPTION) return;
                }
                writeConfig(filename);
            }
        }
    }

    static Window getMainContainer(Component c) {
        Container parent = c.getParent();
        while ((parent instanceof JFrame) == false && (parent instanceof JDialog) == false && (parent instanceof JWindow) == false) {
            parent = parent.getParent();
        }
        return (Window) parent;
    }

    boolean newTable() {
        Window parent = getMainContainer(this);
        TablePreference dialog;
        if (parent instanceof JFrame) dialog = new TablePreference((JFrame) parent); else dialog = new TablePreference((JDialog) parent);
        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            int nbcol = dialog.getNbCol();
            int nbrows = dialog.getNbRows();
            String title = dialog.getProjectTitle();
            TableConfig config = new TableConfig(title, nbrows, nbcol);
            initScalarViewer(config);
            parent.pack();
            return true;
        }
        return false;
    }

    void atkDiagnostic() {
        fr.esrf.tangoatk.widget.util.ATKDiagnostic.showDiagnostic();
    }

    void showErrorHistory() {
        viewer.err_history.setVisible(true);
    }

    void changeRowName(int row) {
        String rowname = viewer.config.rownames[row];
        if ((rowname = (String) JOptionPane.showInputDialog(this, "New Row Name ?", "", JOptionPane.QUESTION_MESSAGE, null, null, rowname)) != null) viewer.updateRows(row, rowname);
    }

    void changeColumnName(int col) {
        String colname = viewer.config.colnames[col - 1];
        if ((colname = (String) JOptionPane.showInputDialog(this, "New Column Name ?", "", JOptionPane.QUESTION_MESSAGE, null, null, colname)) != null) viewer.updateColumns(col, colname);
    }

    void changeAttributeAt(int row, int col) {
        col--;
        if (browser == null) browser = new DevBrowser(this);
        browser.setPosition(row, col, viewer.config.rownames[row], viewer.config.colnames[col]);
        browser.setVisible(true);
    }

    void changeAttributeAt(int row, int col, String attname) {
        viewer.changeAttributeAt(row, col, attname);
    }

    void deleteRow(int row) {
        TableConfig config = viewer.config;
        if (JOptionPane.showConfirmDialog(this, "Delete row " + config.rownames[row] + "  ?", "Delete ?", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            config.deleteRow(row);
            initScalarViewer(config);
        }
    }

    void deleteColumn(int col) {
        col--;
        TableConfig config = viewer.config;
        if (JOptionPane.showConfirmDialog(this, "Delete column " + config.colnames[col] + "  ?", "Delete ?", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            config.deleteColumn(col);
            initScalarViewer(config);
        }
    }

    void addRow() {
        TableConfig config = viewer.config;
        config.addRow();
        initScalarViewer(config);
    }

    void addColumn() {
        TableConfig config = viewer.config;
        config.addColumn();
        initScalarViewer(config);
    }

    public void setColWidth(int title_width, int[] col_width) {
        viewer.config.setColWidth(title_width, col_width);
        viewer.setColWidth(title_width, col_width);
    }

    public void setColWidth(int[] column_width) {
        int title_width = column_width[0];
        int[] col_width = new int[column_width.length - 1];
        System.arraycopy(column_width, 1, col_width, 0, column_width.length - 1);
        setColWidth(title_width, col_width);
    }

    public void setTableFont(Font font) {
        viewer.setTableFont(font);
    }

    public void stopTrend() {
        if (trend != null) {
            atkmoni.setVisible(false);
            atkmoni.dispose();
            if (trend.getModel() != null) trend.getModel().stopRefresher();
            trend = null;
            atkmoni = null;
        }
    }

    private static JDialog atkmoni = null;

    private static Trend trend = null;

    public void showTrend() {
        if (atkmoni == null) {
            Splash splash = new Splash();
            splash.setTitle("Trend");
            splash.setMessage("Starting....");
            splash.setVisible(true);
            atkmoni = new JDialog(new JFrame(), false);
            atkmoni.addWindowListener(new java.awt.event.WindowAdapter() {

                public void windowClosing(java.awt.event.WindowEvent evt) {
                    atkmoni.setVisible(false);
                    atkmoni.dispose();
                }
            });
            JButton dismissBtn = new JButton("Dismiss");
            dismissBtn.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    atkmoni.setVisible(false);
                    atkmoni.dispose();
                }
            });
            JPanel panel = new JPanel();
            panel.add(dismissBtn);
            atkmoni.getContentPane().add(panel, java.awt.BorderLayout.SOUTH);
            atkmoni.setTitle("Attributes monitoring");
            trend = new Trend();
            for (int i = -0; i < viewer.config.size(); i++) {
                TableConfig.Attribute att = viewer.config.attributeAt(i);
                if (att != null && att.name != null) {
                    splash.progress(100 * i / viewer.config.size());
                    splash.setMessage("Adding " + att.name);
                    try {
                        trend.addAttribute(att.name);
                    } catch (ClassCastException e) {
                        System.out.println("Attribute " + att.name + "\n" + e + "  Not Supported !");
                    }
                }
            }
            atkmoni.getContentPane().add(trend, java.awt.BorderLayout.CENTER);
            atkmoni.setSize(768, 512);
            splash.progress(100);
            splash.setVisible(false);
        }
        atkmoni.setVisible(true);
    }

    /**
	 * A convenience implementation of FileFilter that filters out
	 * all files except for those type extensions that it knows about.
	 *
	 * Extensions are of the type ".foo", which is typically found on
	 * Windows and Unix boxes, but not on Macinthosh. Case is ignored.
	 *
	 * Example - create a new filter that filerts out all files
	 * but gif and jpg image files:
	 *
	 *     JFileChooser chooser = new JFileChooser();
	 *     AtkTableFilter filter = new AtkTableFilter(
	 *                   new String{"gif", "jpg"}, "JPEG & GIF Images")
	 *     chooser.addChoosableFileFilter(filter);
	 *     chooser.showOpenDialog(this);
	 *
	 * @version 1.8 08/26/98
	 * @author Jeff Dinkins
	 */
    public class AtkTableFilter extends FileFilter {

        private Hashtable filters = null;

        private String description = null;

        private String fullDescription = null;

        private boolean useExtensionsInDescription = true;

        /**
    	 * Creates a file filter. If no filters are added, then all
    	 * files are accepted.
    	 *
    	 * @see #addExtension
    	 */
        public AtkTableFilter() {
            this.filters = new Hashtable();
        }

        /**
    	 * Creates a file filter that accepts files with the given extension.
    	 * Example: new AtkTableFilter("jpg");
    	 *
    	 * @see #addExtension
    	 */
        public AtkTableFilter(String extension) {
            this(extension, null);
        }

        /**
    	 * Creates a file filter that accepts the given file type.
    	 * Example: new AtkTableFilter("jpg", "JPEG Image Images");
    	 *
    	 * Note that the "." before the extension is not needed. If
    	 * provided, it will be ignored.
    	 *
    	 * @see #addExtension
    	 */
        public AtkTableFilter(String extension, String description) {
            this();
            if (extension != null) addExtension(extension);
            if (description != null) setDescription(description);
        }

        /**
    	 * Creates a file filter from the given string array.
    	 * Example: new AtkTableFilter(String {"gif", "jpg"});
    	 *
    	 * Note that the "." before the extension is not needed adn
    	 * will be ignored.
    	 *
    	 * @see #addExtension
    	 */
        public AtkTableFilter(String[] filters) {
            this(filters, null);
        }

        /**
    	 * Creates a file filter from the given string array and description.
    	 * Example: new AtkTableFilter(String {"gif", "jpg"}, "Gif and JPG Images");
    	 *
    	 * Note that the "." before the extension is not needed and will be ignored.
    	 *
    	 * @see #addExtension
    	 */
        public AtkTableFilter(String[] filters, String description) {
            this();
            for (int i = 0; i < filters.length; i++) {
                addExtension(filters[i]);
            }
            if (description != null) setDescription(description);
        }

        /**
    	 * Return true if this file should be shown in the directory pane,
    	 * false if it shouldn't.
    	 *
    	 * Files that begin with "." are ignored.
    	 *
    	 * @see #getExtension
    	 * @see FileFilter
    	 */
        public boolean accept(File f) {
            if (f != null) {
                if (f.isDirectory()) return true;
                String extension = getExtension(f);
                if (extension != null && filters.get(getExtension(f)) != null) {
                    return true;
                }
            }
            return false;
        }

        /**
    	 * Return the extension portion of the file's name .
    	 *
    	 * @see #getExtension
    	 * @see FileFilter#accept
    	 */
        public String getExtension(File f) {
            if (f != null) {
                String filename = f.getName();
                return getExtension(filename);
            }
            return null;
        }

        /**
    	 * Return the extension portion of the file's name .
    	 *
    	 * @see #getExtension
    	 * @see FileFilter#accept
    	 */
        public String getExtension(String filename) {
            if (filename != null) {
                int i = filename.lastIndexOf('.');
                if (i > 0 && i < filename.length() - 1) return filename.substring(i + 1).toLowerCase();
            }
            return null;
        }

        /**
    	 * Adds a filetype "dot" extension to filter against.
    	 *
    	 * For example: the following code will create a filter that filters
    	 * out all files except those that end in ".jpg" and ".tif":
    	 *
    	 *   AtkTableFilter filter = new AtkTableFilter();
    	 *   filter.addExtension("jpg");
    	 *   filter.addExtension("tif");
    	 *
    	 * Note that the "." before the extension is not needed and will be ignored.
    	 */
        public void addExtension(String extension) {
            if (filters == null) filters = new Hashtable(5);
            filters.put(extension.toLowerCase(), this);
            fullDescription = null;
        }

        /**
    	 * Returns the human readable description of this filter. For
    	 * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
    	 *
    	 * @see FileFilter#getDescription
    	 */
        public String getDescription() {
            if (fullDescription == null) {
                if (description == null || isExtensionListInDescription()) {
                    fullDescription = description == null ? "(" : description + " (";
                    Enumeration extensions = filters.keys();
                    if (extensions != null) {
                        fullDescription += "." + extensions.nextElement();
                        while (extensions.hasMoreElements()) fullDescription += ", " + extensions.nextElement();
                    }
                    fullDescription += ")";
                } else fullDescription = description;
            }
            return fullDescription;
        }

        /**
    	 * Sets the human readable description of this filter. For
    	 * example: filter.setDescription("Gif and JPG Images");
    	 *
    	 */
        public void setDescription(String description) {
            this.description = description;
            fullDescription = null;
        }

        /**
    	 * Determines whether the extension list (.jpg, .gif, etc) should
    	 * show up in the human readable description.
    	 *
    	 * Only relevent if a description was provided in the constructor
    	 * or using setDescription();
    	 *
    	 */
        public void setExtensionListInDescription(boolean b) {
            useExtensionsInDescription = b;
            fullDescription = null;
        }

        /**
    	 * Returns whether the extension list (.jpg, .gif, etc) should
    	 * show up in the human readable description.
    	 *
    	 * Only relevent if a description was provided in the constructor
    	 * or using setDescription();
    	 *
    	 */
        public boolean isExtensionListInDescription() {
            return useExtensionsInDescription;
        }
    }
}
