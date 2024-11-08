package geodress.ui.graphical;

import geodress.main.InfoConstants;
import geodress.main.Logging;
import geodress.model.PictureBox;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * Represents the program window for the GUI.
 * 
 * @author Stefan T.
 */
public class GeoDressGui extends JFrame {

    /** Logger object */
    private Logger logger;

    /** the table with pictures */
    private PictureTable table;

    /** the scroll pane that contains the table with pictures */
    private JScrollPane scrolledList;

    /** title of the window */
    private String windowTitle;

    /** the menu bar with listeners */
    private MenuBar menuBar;

    /** the program icon */
    private final File ICON = new File("resources" + File.separator + "geodress_icon.png");

    /**
	 * Initializes GUI.
	 * 
	 * @param windowTitle
	 *            The title of the game window
	 */
    public GeoDressGui(String windowTitle) {
        super(windowTitle);
        this.windowTitle = windowTitle;
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(ICON.getPath()));
        logger = Logging.getLogger(this.getClass().getName());
        createAndShowGui();
    }

    /**
	 * Creates the GUI and shows it.
	 */
    private void createAndShowGui() {
        getContentPane().setLayout(new BorderLayout(1, 1));
        menuBar = new MenuBar(this);
        setJMenuBar(menuBar);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(960, 600);
        setVisible(true);
    }

    /**
	 * Creates a new list with pictures.
	 * 
	 * @param pictureBox
	 *            the PictureBox object
	 */
    public void createPictureList(PictureBox pictureBox) {
        menuBar.setPictureBox(pictureBox);
        if (table == null) {
            table = new PictureTable(pictureBox);
            scrolledList = new JScrollPane(table);
            this.getContentPane().add(scrolledList, BorderLayout.CENTER);
            this.validate();
        } else {
            table.setData(pictureBox);
        }
    }

    /**
	 * Shows an about window.
	 */
    public void showAbout() {
        JOptionPane.showMessageDialog(this, "<html>" + InfoConstants.VERSION_DETAILS_HTML + "</html>", "About", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(ICON.getPath()));
    }

    /**
	 * Shows window with the help file.
	 */
    public void showHelp() {
        File helpFile = new File("resources" + File.separator + "help_en.html");
        try {
            new HelpDialog(this, helpFile, new Dimension(700, 800));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "error while loading help file", ioe);
            JOptionPane.showMessageDialog(this, "The help file could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Shows an error message in a window.
	 * 
	 * @param message
	 *            the error message to show
	 */
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Shows a warning dialog with OK/Cancel buttons.
	 * 
	 * @param message
	 *            the warning message to show
	 * @return <code>true</code> if OK button was pressed<br>
	 *         <code>false</code> if Cancel button was pressed
	 */
    public boolean showWarningDialog(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /**
	 * Shows a dialog to select a directory to load.
	 */
    public void loadDirectory() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load directory");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int state = fc.showOpenDialog(this);
        if (state == JFileChooser.APPROVE_OPTION) {
            if (fc.getSelectedFile().isDirectory()) {
                try {
                    createPictureList(new PictureBox(fc.getSelectedFile()));
                    setTitle(windowTitle + " - " + fc.getSelectedFile().getAbsolutePath());
                } catch (FileNotFoundException fnfe) {
                    logger.log(Level.WARNING, fnfe.getMessage(), fnfe);
                    showError("The directory " + fc.getSelectedFile().getAbsolutePath() + " doesn't exist.");
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "error while loading files from directory", ioe);
                }
            }
        }
    }

    /**
	 * Shows a dialog for saving and exports the table to a file.
	 * 
	 * @param format
	 *            the format of the output file, for example
	 *            {@link PictureBox#FORMAT_TXT} for a text file
	 */
    public void exportTable(int format) {
        Map<Integer, String> fileEndings = new HashMap<Integer, String>(8);
        fileEndings.put(0, ".txt");
        fileEndings.put(1, ".csv");
        fileEndings.put(2, ".txt");
        if (table != null && table.getRowCount() > 0) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Export file as " + fileEndings.get(format));
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            int state = fc.showSaveDialog(this);
            if (state == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (!file.getName().endsWith(fileEndings.get(format))) {
                    file = new File(file.getAbsolutePath() + fileEndings.get(format));
                }
                try {
                    if (!file.exists()) {
                        menuBar.getPictureBox().saveToFile(file, format);
                    } else {
                        int answer = JOptionPane.showConfirmDialog(this, "The file '" + file.getName() + "' does already exist - overwrite it?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (answer == JOptionPane.YES_OPTION) {
                            menuBar.getPictureBox().saveToFile(file, format);
                        }
                        if (answer == JOptionPane.NO_OPTION) {
                            exportTable(format);
                        }
                    }
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "error while exporting file with format " + fileEndings.get(format), ioe);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "There are no data that could be exported.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
	 * Asks for a new Google Maps API key.
	 * 
	 * @return the Google Maps API key of the user
	 */
    public String askForGoogleMapsKey() {
        return JOptionPane.showInputDialog(this, "Enter your own Google Maps API key.", "Google Maps API key", JOptionPane.QUESTION_MESSAGE);
    }

    /**
	 * Asks for the path to ExifTool.
	 * 
	 * @param currentPath
	 *            the current path to ExifTool
	 * @return the path to ExifTool
	 */
    public String askForExifToolPath(String currentPath) {
        return (String) JOptionPane.showInputDialog(this, "Enter the path to the ExifTool executable.", "ExifTool path", JOptionPane.QUESTION_MESSAGE, null, null, currentPath);
    }

    /**
	 * Writes some text to the clipboard of the running OS.
	 * 
	 * @param text
	 *            the text that should be copied to clipboard
	 */
    protected static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
