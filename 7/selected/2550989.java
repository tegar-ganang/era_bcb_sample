package net.sf.wwusmart.gui;

import net.sf.wwusmart.algorithms.framework.*;
import net.sf.wwusmart.browsing.BrowsingController;
import net.sf.wwusmart.configuration.ConfigManager;
import net.sf.wwusmart.database.*;
import net.sf.wwusmart.gui.java3dview.JoglOffViewerPanel;
import net.sf.wwusmart.helper.*;
import java.awt.dnd.DragSource;
import java.io.*;
import java.sql.SQLException;
import java.util.logging.*;
import javax.swing.*;
import net.sf.wwusmart.main.Smart;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.table.DefaultTableModel;

/**
 * <p>
 * This class handles all program logic caused by the usage of the gui. It takes
 * requests to display results and information from all other parts of the program
 * and renders the changes to the gui.
 * </p>
 *
 * <p>
 * <em>comment:</em>
 * </p>
 * <p>
 * This class could still use some threading optimization concerning
 * access to gui elemetns via the SwingUtilities.invokeLater and
 * SwingUtitlities.invokeAndWait methods.
 * </p>
 *
 * @author Armin
 * @version $Rev: 777 $
 */
public class GuiController {

    /** Singelton reference for this class */
    private static final GuiController INSTANCE = new GuiController();

    /** Top left corner of the MainWindow on the screen. */
    private Point location;

    /** Size of the MainWindow. */
    private Dimension size;

    /** Source handle for all drag and drop operations. */
    private DragSource dragSource;

    /** Shared shape pinboard for all tabs in the MainWindow. */
    private ShapePinboard pinboard;

    /** Main window reference. */
    protected MainWindow gui;

    /** Log Dialog reference. */
    private static HTMLDialog logWindow = null;

    /** Filter for ".smart" files. */
    public static FilenameFilter smartFileFilter;

    /** Filter for ".html" files. */
    public static FilenameFilter htmlFileFilter;

    /** Filter for the shape files according to the connected database. */
    public static FilenameFilter dbModeFileFilter;

    static {
        GuiController.smartFileFilter = new FilenameFilter() {

            String ext = new String(".smart");

            public boolean accept(File dir, String name) {
                if (name.length() < ext.length()) {
                    return false;
                }
                String extension = name.substring(name.length() - ext.length());
                if (extension.toLowerCase().equals(ext)) {
                    return true;
                }
                return false;
            }
        };
        GuiController.htmlFileFilter = new FilenameFilter() {

            String ext = new String(".html");

            public boolean accept(File dir, String name) {
                if (name.length() < ext.length()) {
                    return false;
                }
                String extension = name.substring(name.length() - ext.length());
                if (extension.toLowerCase().equals(ext)) {
                    return true;
                }
                return false;
            }
        };
        GuiController.dbModeFileFilter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                String tmp = name.toLowerCase();
                for (String ext : DataManager.getInstance().getDatabaseMode().getFileExtensions()) {
                    if (tmp.endsWith(ext)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * {@code private} to disable instancing of singleton.
     */
    private GuiController() {
    }

    /**
     * Opens up a dialog presenting the user the algorithms new to the database
     * and the algorithms which's hash values changed. User can select via
     * check- or radioboxes which algorithms descriptors shall be generated.
     * For new algorithms, the user can select to compute desriptors or not
     * to compute at that moment, for changed algorithms he can also choose to
     * use the old descriptors already present in the database.
     *
     * @param changes A list of algorithm change information.
     */
    public void askForNewAlgorithms(java.util.List<ChangedAlgorithmInfo> changes) {
        ChangedAlgorithmsDialog dialog = new ChangedAlgorithmsDialog(gui, true, changes);
        dialog.setVisible(true);
    }

    /**
     * Sets the position and size of the MainWindow and the look and feel of
     * the gui.
     *
     * @param posX X Position of the MainWindow.
     * @param posY Y Position of the MainWindow.
     * @param width Width of the MainWindow.
     * @param height Height of the MainWindow.
     * @param lnf A Look and Feel classname.
     */
    public void applySettings(int posX, int posY, int width, int height, String lnf) {
        location = new Point(posX, posY);
        size = new Dimension(width, height);
        try {
            UIManager.setLookAndFeel(lnf);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Look and Feel not found unsing default", ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Error loading Look and Feel: " + lnf.getClass().getSimpleName(), ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Error loading Look and Feel: " + lnf.getClass().getSimpleName(), ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Look and Feel: '" + lnf.getClass().getSimpleName() + "' not supported!", ex);
        }
    }

    /**
     * <p>Constructs an Array containing the following information:</p>
     * <ul>
     * <li>[0] - MainWindow x position.
     * <li>[1] - MainWindow y position.
     * <li>[2] - MainWindow width.
     * <li>[3] - MainWindow height.
     * <li>[4] - Current look and feel classname.
     * </ul>
     *
     * @return An array the with length 5.
     */
    public String[] getGuiSettings() {
        String[] settings = new String[5];
        settings[0] = "" + gui.getLocation().x;
        settings[1] = "" + gui.getLocation().y;
        settings[2] = "" + gui.getSize().width;
        settings[3] = "" + gui.getSize().height;
        settings[4] = "" + UIManager.getLookAndFeel().getClass().getName();
        return settings;
    }

    /**
     * Getter for the shared shape pinboard reference.
     * @return The shared reference.
     */
    public ShapePinboard getPinboard() {
        return this.pinboard;
    }

    /**
     * Sets up all shared objects and ensures a database connection by displaying
     * a database connetion dialog as long as it takes to connect to a working
     * database.
     *
     * @param splash reference to the splash screen so it can be closed if
     *        still showing.
     */
    public void initProgram(net.sf.wwusmart.gui.SplashScreen splash) {
        this.dragSource = new DragSource();
        this.pinboard = new ShapePinboard();
        this.gui = new MainWindow();
        this.gui.setVisible(true);
        this.gui.requestFocus();
        if (splash != null) {
            splash.close();
        }
        boolean showLicense = ConfigManager.getSetting(ConfigManager.LICENSE_ACCEPTED).equals(ConfigManager.DISABLED);
        if (showLicense) showLicense();
        boolean connected = false;
        boolean autoConnected = false;
        DatabaseDialog diag = new DatabaseDialog(gui, true);
        do {
            if (!autoConnected && (ConfigManager.getSetting(ConfigManager.AUTO_CONNECT_DB)).equals(ConfigManager.ENABLED)) {
                autoConnected = true;
                diag.applySettings();
            } else {
                diag.setVisible(true);
            }
            connected = DataManager.getInstance().isConnected() || connectDB(diag.getURL(), diag.getDriver(), diag.usesLogin(), diag.getLogin(), diag.getPassword());
        } while (!DataManager.getInstance().isConnected());
        GuiController.getInstance().newTab();
        try {
            this.gui.initFileChoosers();
        } catch (Throwable t) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Initalizing the FileChoosers failed due to: " + t.getMessage(), t);
        }
        this.gui.setTitle("");
    }

    /**
     * This displays the information from the record in the statusBar of the Application
     * @param record The LogRecord of which the inforamtion will be displayed in the
     *        statusbar.
     */
    public void log(LogRecord record) {
        if (gui == null) {
            return;
        }
        gui.log(record);
    }

    /**
     * Creates a new ContourFileViewer representing the data in {@code data}
     * with a titled border displaying the given {@code label} and adds it to
     * the given {@code destination}.
     *
     * @param label The String displayed in the TitledBorder surrounding the
     *        constructed Component.
     * @param data Must only contain a String in Contour-File format.
     * @param destination The JPanel to which the newly created Component will
     *        be added.
     */
    public void renderContourDescriptor(String label, byte[] data, final JPanel destination) {
        ContourFileViewerPanel cfvp = new ContourFileViewerPanel(data);
        final JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPanel.setBorder(BorderFactory.createTitledBorder(label));
        descPanel.add(cfvp);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(descPanel);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * This method is called when a DescriptorAlgorithm wants a csv descriptor
     * rendered to a specific destiation. The data is rendered in a JTable
     * using the fist row in the csv values string as column headers.
     *
     * @param label A String to display as table title.
     * @param csv The table data in a csv formatted String.
     * @param destination The Panel to which to add the generated JTable.
     */
    public void renderCsvDescriptor(String label, String csv, final JPanel destination) {
        String[] rows = csv.split("\n");
        String[] columnNames = rows[0].split(";");
        String[][] tableData = new String[rows.length - 1][];
        for (int i = 0; i < rows.length - 1; ++i) {
            tableData[i] = rows[i + 1].split(";");
        }
        JTable csvTable = new JTable();
        DefaultTableModel csvModel = new DefaultTableModel(tableData, columnNames) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        csvTable.setModel(csvModel);
        final JScrollPane scrollTable = new JScrollPane(csvTable);
        scrollTable.setBorder(BorderFactory.createTitledBorder(label));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(scrollTable);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * Renders the given data in a JTextArea interpreted as HEX values.
     *
     * @param label A String to display as title for this descriptor.
     * @param data Hex values using lower and upper bits.
     * @param destination
     */
    public void renderHexDescriptor(String label, byte[] data, final JPanel destination) {
        String hex = ByteArrayHelper.toHexString(data);
        final JTextArea ta = new JTextArea(hex);
        ta.setOpaque(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createTitledBorder(label));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(ta);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * Reders the given image with the given label as title to the given
     * destination.
     *
     * @param label A String to display as title for this descriptor.
     * @param image The image to reder.
     * @param destination Where to render the descriptor to.
     */
    public void renderImageDescriptor(String label, Image image, final JPanel destination) {
        JLabel img = new JLabel(new ImageIcon(image));
        final JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descPanel.setBorder(BorderFactory.createTitledBorder(label));
        descPanel.add(img);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(descPanel);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * Renders the given data expecting it to be a off file format formatted
     * String using an {@link JoglOffViewerPanel}.
     *
     * @param label A String to display as title for this descriptor.
     * @param data A off file format formatted String.
     * @param destination Where to render the desctiptor to.
     */
    public void renderOffDescriptor(String label, byte[] data, final JPanel destination) {
        try {
            final JoglOffViewerPanel ofvp = new JoglOffViewerPanel(new OffParser(new String(data)), false);
            ofvp.setBorder(BorderFactory.createTitledBorder(label));
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    destination.add(ofvp);
                    destination.validate();
                    destination.repaint();
                }
            });
            return;
        } catch (IOException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ShapeFileFormatException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
        final JLabel l = new JLabel("Error parsing off data! See program log for details.");
        l.setBorder(BorderFactory.createTitledBorder(label));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(l);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * Renders the given Text in an {@code  JTextArea} using the given label as
     * title. The generated {@code JTextArea} will be added to the given
     * destination.
     *
     * @param label A String to display as title for this descriptor.
     * @param text The desctiptor as text.
     * @param destination Where to render the descriptor to.
     */
    public void renderTextDescriptor(String label, String text, final JPanel destination) {
        final JTextArea ta = new JTextArea(text);
        ta.setOpaque(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createTitledBorder(label));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                destination.add(ta);
                destination.validate();
                destination.repaint();
            }
        });
    }

    /**
     * Informs the user, that all Algorithms in {@code a} have been lost
     * @param a The lost algorithms
     */
    public void reportLostAlgorithms(java.util.List<LostAlgorithmInfo> a) {
        JDialog dialog = new LostAlgorithmsDialog(gui, true, a);
        dialog.setVisible(true);
    }

    /**
     * Hides the gui and initiates the shutdown sequence for the application
     */
    public void close() {
        gui.setVisible(false);
        Smart.getInstance().shutdown();
    }

    /**
     * Tries to connect to the given database using the given dbDriver and the
     * given credentials if {@code useLogin} is given as {@code true}.
     * Displays a indeterminate progress dialog while building up the 
     * connection.
     *
     * @param url The URL to the database.
     * @param driver Identifies the database driver.
     * @param useLogin Whether to use login credentials.
     * @param username The username can be {@code null} if {@code useLogin} is
     *        {@code false}.
     * @param password The password can be {@code null} if {@code useLogin} is
     *        {@code false}.
     * @return true if successful, false else.
     */
    public boolean connectDB(final String url, final dbDriver driver, final boolean useLogin, final String username, final char[] password) {
        final ProgressDialog progressDialog = new ProgressDialog(GuiController.getInstance().getMainWindow(), "Connecting to database...", "Please wait...", DialogFactory.DialogType.INFO);
        progressDialog.setIndeterminate(true);
        progressDialog.setAbortVisible(false);
        Thread job = new Thread() {

            private static final int SLEEP_MILLI_SECS = 100;

            @Override
            public void run() {
                while (!progressDialog.isReady()) {
                    try {
                        sleep(SLEEP_MILLI_SECS);
                    } catch (InterruptedException ex) {
                    }
                }
                try {
                    DataManager.getInstance().connectDatabase(driver, url, useLogin, username, password);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Unsupported database driver", ex);
                    progressDialog.setAborted(true);
                    displayMessage("Database driver not found.\nPlease choose a different database.", true);
                } catch (SQLException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
                    progressDialog.setAborted(true);
                    if (ex.getSQLState().equals("XJ040")) {
                        String msg = "Database is already connected to another application";
                        displayMessage(msg, true);
                    } else {
                        displayMessage(ex.getMessage() + " (" + ex.getSQLState() + ")", true);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
                    progressDialog.setAborted(true);
                    displayMessage("Database files not accessible.\nPlease choose a different database.", true);
                } catch (InstantiationException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
                    progressDialog.setAborted(true);
                    displayMessage("Database driver broken.\nPlease choose a different database.", true);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
                    progressDialog.setAborted(true);
                    displayMessage("Database rights deny access.\nPlease choose a different database.", true);
                } finally {
                    progressDialog.finish();
                }
                ConfigManager.pushRecentConnection(url);
                gui.setStausbarDBMode("Database Mode: " + DataManager.getInstance().getDatabaseMode());
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Connected to Database with url: " + url, url);
            }
        };
        job.start();
        progressDialog.setVisible(true);
        return !progressDialog.isAborted();
    }

    /**
     * Creates a new instance of LogDialog if it not already exists. And set it
     * to visible.
     */
    public void showLog() {
        if (logWindow != null) {
            logWindow.setVisible(true);
        } else {
            JFrame parent = new JFrame();
            parent.setIconImage(new ImageIcon(getClass().getResource("/net/sf/wwusmart/gui/res/iconlogo.png")).getImage());
            logWindow = new LogDialog(parent, Smart.logFilePath);
            logWindow.setVisible(true);
        }
    }

    /**
     * Browses recursively through the given files and collects all files
     * matching the current database mode.
     * @param inputFiles File objects referencing either normal files (which might contain shapes)
     * or directories (which will be search recursively for files containing shapes).
     * @return all shape files found by browsing through inputFiles
     */
    private LinkedList collectShapeFiles(File[] inputFiles) {
        LinkedList<File> files = new LinkedList<File>();
        for (File f : inputFiles) {
            if (f.exists() && f.canRead()) {
                if (f.isFile() && dbModeFileFilter.accept(null, f.getName())) {
                    files.add(f);
                } else if (f.isDirectory()) {
                    files.addAll(collectShapeFiles(f.listFiles()));
                }
            }
        }
        return files;
    }

    /**
     * Adds the Shapes found in the given File objects to database
     * and computes it's descriptors.
     * @param inputFiles File objects referencing either normal files (which might contain shapes)
     * or directories (which will be search recursively for files containing shapes).
     * @throws UserAbortionException When the user aborts the operation.
     */
    public void addShapes(File[] inputFiles) throws UserAbortionException {
        try {
            final LinkedList<File> files = new LinkedList<File>();
            files.addAll(collectShapeFiles(inputFiles));
            if (files.size() == 0) {
                GuiController.getInstance().displayMessage("No files found for current database mode.\n" + "(Mode: " + DataManager.getInstance().getDatabaseMode() + ", " + "allowed filenames: " + DataManager.getInstance().getDatabaseMode().fileExtensionsToString() + ")");
                return;
            }
            final Vector<Shape> shapes = new Vector(files.size());
            final ProgressDialog progressDialog = new ProgressDialog(GuiController.getInstance().getMainWindow(), "Adding shapes...", "Please wait...", DialogFactory.DialogType.INFO);
            progressDialog.setMaximum(files.size());
            Thread job = new Thread() {

                private static final int SLEEP_MILLI_SECS = 100;

                @Override
                public void run() {
                    while (!progressDialog.isReady()) {
                        try {
                            sleep(SLEEP_MILLI_SECS);
                        } catch (InterruptedException ex) {
                        }
                    }
                    ListIterator<File> fileIter = files.listIterator();
                    while (fileIter.hasNext()) {
                        Shape newShape = null;
                        try {
                            newShape = DataManager.getInstance().addShape(fileIter.next());
                        } catch (IOException ex) {
                            Logger.getLogger(GuiController.class.getName()).log(Level.WARNING, "Coundn't add shape file due to IO problems.", ex);
                            GuiController.getInstance().displayException(ex, "Coundn't add shape file due to IO problems.");
                        } catch (SQLException ex) {
                            Logger.getLogger(GuiController.class.getName()).log(Level.WARNING, "Couldn't add shape due to DB problems.", ex);
                            GuiController.getInstance().displayException(ex, "Couldn't add shape due to DB problems.");
                        }
                        if (newShape != null) {
                            shapes.add(newShape);
                        }
                        progressDialog.setProgress(fileIter.nextIndex());
                        if (progressDialog.isAborted()) {
                            break;
                        }
                    }
                    progressDialog.finish();
                }
            };
            job.start();
            progressDialog.setVisible(true);
            if (progressDialog.isAborted()) {
                throw new UserAbortionException();
            }
            AlgorithmManager.getInstance().computeDescriptors(null, shapes);
        } catch (SQLException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Calls database disconnection and fixes gui display
     */
    public void disconnectDB() {
        DataManager.getInstance().disconnectDatabase();
        gui.clearALL();
    }

    /**
     * Saves the currently active {@link BrowsingController} to
     * the given file. If the file already exists the program waits for user
     * input and either overwirtes the file or aborts the operation.
     *
     * @param file The file to which data will be saved.
     */
    public void save(File file) {
        try {
            if (!GuiController.smartFileFilter.accept(null, file.getName())) {
                file = new File(file.getPath() + ".smart");
            }
            if (file.exists()) {
                int ret = JOptionPane.showConfirmDialog(gui, "Overwrite existing file?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            file.createNewFile();
            if (!file.exists()) {
                return;
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.getActiveBrowsingController());
            oos.close();
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, "Couldn't save browsing correctly ", ex);
        }
    }

    /**
     * Loads a previously saved .smart file. If the file does not contain
     * a {@link BrowsingController} as first object the loading
     * fails and an information message is presented to the user.
     * 
     * @param file The file which should be loaded.
     */
    public void load(File file) {
        try {
            if (!file.exists()) {
                return;
            }
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            ois.close();
            fis.close();
            if (BrowsingController.class.isInstance(obj)) {
                final BrowsingController browsingCtrl = (BrowsingController) obj;
                BrowsingInstance browsingInst = new BrowsingInstance(browsingCtrl);
                BrowsingPanel bp = browsingInst.getBrowsingPanel();
                try {
                    bp.display();
                } catch (SQLException ex) {
                    GuiController.getInstance().displayException(ex, "Error loading saved browsing");
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, "Error loading saved browsing", ex);
                }
                gui.addTab("Browsing", bp);
                GuiController.getInstance().gui.validate();
            } else {
                throw new IOException("Wrong file format!");
            }
        } catch (IOException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates a new browsing tab
     */
    public void newTab() {
        BrowsingPanel bp = new BrowsingInstance().getBrowsingPanel();
        gui.addTab("Browsing", bp);
    }

    /**
     * Resets the unterlying {@link BrowsingController} of the
     * given BrowsingPanel.
     * 
     * @param bp The BrowsingPanal of which the underlying BrowsingController
     *           will be resetted.
     */
    public void closeBrowsing(BrowsingPanel bp) {
        bp.getBrowsingInstance().dispose();
        gui.removeBrowsingPanel(bp);
    }

    /**
     * Creates a new Frame with an {@linkplain ShapeInspectionPanel} showing
     * details of the given shape and presents it to the user.
     * 
     * @param shape The shape of which details will be shown.
     */
    public void inspectShape(Shape shape) {
        JFrame sif = new JFrame("Details of " + shape.getName());
        sif.setIconImage(new ImageIcon(getClass().getResource("/net/sf/wwusmart/gui/res/iconlogo.png")).getImage());
        sif.setLayout(new BorderLayout());
        ShapeInspectionPanel sip = new ShapeInspectionPanel(sif, shape);
        sif.add(sip, BorderLayout.CENTER);
        sif.pack();
        sif.setSize(750, 400);
        sif.setLocationRelativeTo(gui);
        sif.setVisible(true);
    }

    /**
     * Creates an {@linkplain OptionsDialog} and presents it to the user.
     */
    public void showOptions() {
        OptionsDialog opdiag = new OptionsDialog(gui, true);
        int ret = opdiag.showOptionsDialog();
        if (ret != OptionsDialog.APPROVE_OPTION) {
            return;
        }
        ConfigManager.setSetting(ConfigManager.LOOK_AND_FEEL, opdiag.getLnfClassName());
        ConfigManager.setPluginDirs(opdiag.getPluginDirs());
        ConfigManager.setSetting(ConfigManager.USE_NATIVE_PLUGINS, opdiag.getNativePluginsEnabled());
        ConfigManager.setSetting(ConfigManager.AUTO_CONNECT_DB, opdiag.getAutoDBEnabled());
    }

    /**
     * opens the Algorithm Manager.
     */
    public void showAlgorithManager() {
        try {
            JDialog am = new JDialog(gui, "Algorithm Manager", true);
            am.setLayout(new BorderLayout());
            am.add(new AlgorithmManagementPanel(am), BorderLayout.CENTER);
            am.pack();
            am.setLocationRelativeTo(gui);
            am.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Saves a HTML representation of the active BrowsingController to the
     * given file. The active BrowsingController is determined by {@link
     * #getActiveBrowsingController() }.
     *
     * @param file The file to which  the HTML representation will be saved.
     */
    public void saveHTML(File file) {
        if (!GuiController.htmlFileFilter.accept(null, file.getName())) {
            file = new File(file.getPath() + ".html");
        }
        HTMLReportGenerator hrg = new HTMLReportGenerator();
        hrg.saveHTML(file, getActiveBrowsingController());
    }

    /**
     * Getter for the singleton instance of this class.
     * @return The singleton instance of this class.
     */
    public static GuiController getInstance() {
        return INSTANCE;
    }

    /**
     * Setter for {@link MainWindow} reference.
     * @param gui MainWindow reference.
     */
    public void setGUI(MainWindow gui) {
        this.gui = gui;
        this.gui.setLocation(location);
        this.gui.setSize(size);
    }

    /**
     * Displays an Exception to the user including Stack Trace and Message
     * if displaying it grafically fails it will print the Information to the
     * Standard error output Stream at {@code System.err}
     * @param ex the Exception of which information is to be displayed
     */
    public void displayException(Exception ex) {
        displayException(ex, ex.getLocalizedMessage());
    }

    /**
     * Displays an Exception to the user including StackTrace and an additional
     * Message. If displaying grafically fails a printout of the Information
     * to the standard error output Stream at {@code System.err} is attempted.
     *
     * @param ex the Exception of which information is to be displayed
     * @param msg An additional message displayed before the exception
     *        information
     */
    public void displayException(Exception ex, String msg) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ex.printStackTrace(new PrintStream(baos));
        try {
            baos.flush();
        } catch (IOException ioe) {
            System.err.println("Exception handling failed because:" + ioe.getLocalizedMessage());
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace();
            return;
        }
        DialogFactory.showScrollMessageDialog(gui, msg, baos.toString(), "Exception caught", DialogFactory.DialogType.ERROR, true);
        ex.printStackTrace();
    }

    /**
     * Displays a given Message to the user. And blocks the gui if modal is true.
     * @param msg The message to be displayed.
     * @param modal Wether this message should be displayed modal.
     */
    public void displayMessage(String msg, boolean modal) {
        Component display = modal ? gui : new JFrame("Empty Frame for Exceptions display");
        JOptionPane.showMessageDialog(display, msg, "Just to let you know", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a given Message to the user.
     * @param msg The message to be displayed.
     */
    public void displayMessage(String msg) {
        displayMessage(msg, false);
    }

    /**
     * Displays a given Message to the user.
     *
     * @param msg The message to be displayed.
     * @param headline A String to use as headline.
     */
    public void displayLongMessage(String msg, String headline) {
        DialogFactory.showScrollMessageDialog(gui, headline, msg, "Just to let you know", DialogFactory.DialogType.INFO);
    }

    /**
     * Getter for the currently active BrowsingController. The active BrowsingController
     * is the one currently visible in the gui.
     * @return The currently active BrowsingController.
     */
    public BrowsingController getActiveBrowsingController() {
        return gui.getActiveBrowsingController();
    }

    /**
     * Getter for the global DragSource instance.
     * @return The global DragSource instance.
     */
    public DragSource getDragSource() {
        return this.dragSource;
    }

    /**
     * Creates a dialog box giving miscellaneous information on progress of
     * computation of several descriptors.
     * 
     * @param algorithms List of DesctiptorAlgorithm Coupled with a Collection
     *        of shapes and an Integer to Count the progress.
     * @param descriptorsToGo The amount of descriptors to be calculated.
     * @return The dialog used to render the progress.
     */
    public ComputeDescriptorsDialog createComputationProgress(java.util.List<Couple<DescriptorAlgorithm, Couple<Collection<Shape>, Integer>>> algorithms, int descriptorsToGo) {
        JFrame parent = new JFrame();
        parent.setIconImage(new ImageIcon(getClass().getResource("/net/sf/wwusmart/gui/res/iconlogo.png")).getImage());
        ComputeDescriptorsDialog dialog = new ComputeDescriptorsDialog(parent, true, algorithms, descriptorsToGo);
        return dialog;
    }

    /**
     * This method is for easier handling of userio. The user will be presented
     * with an YES/NO Option dialog questing the given String.
     * If the user chooses YES true is returned, flase, else.
     *
     * @param question A yes/no question.
     * @return true - if the user answers {@code question with yes}<br>
     *         false - else.
     */
    public boolean getConfirmation(String question) {
        int ret = JOptionPane.showConfirmDialog(gui, question, "Confirm before proceed", JOptionPane.YES_NO_OPTION);
        return ret == JOptionPane.OK_OPTION;
    }

    /**
     * Getter for the MainWindow reference e.g. to be used for blockin in modal
     * dialogs.
     *
     * @return Reference to the main window.
     */
    public MainWindow getMainWindow() {
        return gui;
    }

    /**
     * Invokes a {@linkplain BrowsingPanel#clear() clear} of the currently
     * showing/active tab.
     */
    public void clearActiveBrowsing() {
        gui.getSelectedBrowsingPanel().clear();
    }

    /**
     * Setter for the logWindow.
     * @param logDialog The logWindow dialog.
     */
    static void setlogWindow(LogDialog logDialog) {
        GuiController.logWindow = logDialog;
    }

    /**
     * Opens a save dialog requesting the location to where the off files of the
     * shpes in the given list should be exported. This will also export the
     * thumbnails. Creating a folder for each shape.
     *
     * @param list A collectoin of shapes.
     */
    public void exportShapes(List<Shape> list) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setMultiSelectionEnabled(false);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = fc.showSaveDialog(gui);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File path = fc.getSelectedFile();
        if (path.isFile()) {
            path = path.getParentFile();
        }
        if (path == null || path.isFile() || !path.canWrite()) {
            displayMessage("Cannot save to this location");
            return;
        }
        StringBuffer exported = new StringBuffer("Shapes Exported:\n\n");
        StringBuffer skipped = new StringBuffer("Skipped shapes:\n\n");
        for (Shape shape : list) {
            try {
                exportShape(shape, path);
                exported.append(String.format("%s: %s\n", shape.getPrimaryKey(), shape.getName()));
            } catch (FileExistsException fee) {
                skipped.append(String.format("%s: %s | %s\n", shape.getPrimaryKey(), shape.getName(), fee.getMessage()));
            }
        }
        displayLongMessage(exported.toString() + "\n\n" + skipped, "Export result:");
    }

    /**
     * Creates a folder dependent on the given shape in the given directory
     * path. And saves the shape data, the shape thumbnail and its tags to
     * seperate files in the generated directory.
     * 
     * @param shape An shape.
     * @param path path under wich to save
     */
    private void exportShape(Shape shape, File path) throws FileExistsException {
        path = new File(path.getPath() + File.separator + shape.getPrimaryKey() + "_" + shape.getName());
        path.mkdirs();
        String[] ext = DataManager.getInstance().getDatabaseMode().getFileExtensions();
        File data = new File(path.getPath() + File.separator + shape.getName() + ext[0]);
        File thumb = new File(path.getPath() + File.separator + shape.getName() + ".png");
        File tags = new File(path.getPath() + File.separator + shape.getName() + ".txt");
        FileWriter dataFw = null;
        FileWriter tagFw = null;
        try {
            dataFw = new FileWriter(data);
            dataFw.write(new String(shape.getData()));
            dataFw.flush();
            ImageIO.write(ImageOps.imageIconToBufferdImage(shape.getThumbnail()), "PNG", thumb);
            tagFw = new FileWriter(tags);
            tagFw.write(shape.getTags());
            tagFw.flush();
            Logger.getLogger(GuiController.class.getName()).info("Shape exported: " + shape);
        } catch (IOException ex) {
            throw new FileExistsException(data);
        } finally {
            try {
                dataFw.close();
            } catch (Exception ignore) {
            }
            try {
                tagFw.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Exports all shapes in the current result.
     */
    public void exportCurrentResultShapes() {
        try {
            exportShapes(getActiveBrowsingController().getCurrentResult().getListOfShapes());
        } catch (SQLException ex) {
            displayMessage("Error retrieving all shapes in the current result.");
        }
    }

    /**
     * Presents a {@link VersionDialog} to the user.
     */
    public void showVersionDialog() {
        new VersionDialog(gui, true).setVisible(true);
    }

    /**
     * Presents a {@link LicenseDialog} to the user.
     */
    protected void showLicense() {
        LicenseDialog lic = new LicenseDialog(gui, GuiController.class.getResource("res/license.html"));
        lic.setVisible(true);
        if (!lic.hasAgreed()) {
            ConfigManager.setSetting(ConfigManager.LICENSE_ACCEPTED, ConfigManager.DISABLED);
            Smart.getInstance().shutdown();
        }
        ConfigManager.setSetting(ConfigManager.LICENSE_ACCEPTED, ConfigManager.ENABLED);
    }

    /**
     * private class to handle file overwrite occurences
     */
    private class FileExistsException extends Exception {

        private final File file;

        public FileExistsException(File f) {
            super("File exisits: " + f.getName());
            this.file = f;
        }

        public File getFile() {
            return this.file;
        }
    }
}
