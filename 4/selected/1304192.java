package org.statcato.file;

import org.statcato.*;
import org.statcato.spreadsheet.*;
import org.statcato.utils.HelperFunctions;
import org.statcato.utils.SetProjectAutoSaveTimer;
import org.statcato.graph.StatcatoChartFrame;
import org.jfree.chart.JFreeChart;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.ArrayList;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * A project file that stores the LogWindow instance, DatasheetTabbedPane
 * instance,as well as generated charts of this application.
 * 
 * @author Margaret Yau
 * @version %I%, %G%
 * @since 1.0
 */
public class Project {

    private LogWindow Log;

    private DatasheetTabbedPane DatasheetPane;

    private Statcato app;

    private File savedFile;

    public static String extension = "stc";

    private File tempFile;

    private int AUTOSAVEINTERVAL = 300;

    private boolean DELETEAUTOSAVEFILE = true;

    SetProjectAutoSaveTimer timer;

    /**
     * Constructor.
     * 
     * @param app Parent Statcato application
     */
    public Project(Statcato app) {
        this.Log = app.getLogTextPane();
        this.DatasheetPane = app.getDatasheetTabbedPane();
        this.app = app;
        tempFile = new File("~statcato.tmp." + HelperFunctions.getCurrentTimeString() + ".stc");
        timer = new SetProjectAutoSaveTimer(this, AUTOSAVEINTERVAL);
    }

    /**
     * Sets the auto save interval variable to the given value.
     * 
     * @param seconds number of seconds
     */
    public void setAutoSaveInterval(int seconds) {
        AUTOSAVEINTERVAL = seconds;
        timer.setInterval(seconds);
    }

    /**
     * Sets the delete auto save file boolean to the given value.
     *
     * @param delete boolean indicating whether to delete auto save file 
     */
    public void setDeleteAutoSaveFile(boolean delete) {
        DELETEAUTOSAVEFILE = delete;
    }

    /**
     * Returns whether this project exists (is valid).
     *
     * @return whether this project exists (is valid)
     */
    public boolean exists() {
        return !getName().equals("");
    }

    /**
     * Returns true if the project is modified since the last save.
     * 
     * @return true if the project is modified since the last save or 
     * false otherwise
     */
    public boolean isModified() {
        return (Log.getChangedStatus() || DatasheetPane.getChangedStatus());
    }

    /**
     * Writes this project to a file.  Opens a file chooser for the user 
     * to select file for save as or if the project has not been saved before.
     *   
     * @param saveAs whether it is an save as operation
     * @return true if the project is saved successfully or false otherwise.
     */
    public boolean writeToFile(boolean saveAs) {
        String path = "";
        if (savedFile != null && !saveAs) {
            path = savedFile.getPath();
            writeFileHelper(path, false);
            return true;
        } else {
            JFileChooser fc = new JFileChooser(FileOperations.getRecentProject() == null ? null : FileOperations.getRecentProject().getParentFile());
            ExtensionFileFilter filter = new ExtensionFileFilter("Statcato project file (*.stc)", extension);
            fc.addChoosableFileFilter(filter);
            fc.setAcceptAllFileFilterUsed(false);
            int returnValue = fc.showSaveDialog(app);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                path = file.getPath();
                if (!path.toLowerCase().endsWith("." + extension)) {
                    path += "." + extension;
                    file = new File(path);
                }
                if (file.exists()) {
                    Object[] options = { "Overwrite file", "Cancel" };
                    int choice = JOptionPane.showOptionDialog(app, "The specified file already exists.  Overwrite existing file?", "Overwrite file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (choice != 0) return false;
                }
                boolean success = writeFileHelper(path, false);
                if (!success) return false;
                savedFile = file;
                return true;
            }
            return false;
        }
    }

    /**
     * Returns the saved project file.
     * 
     * @return project file
     */
    public File getFile() {
        return savedFile;
    }

    /**
     * Called by writeToFile function to perform writing of the project file
     * to the given file path.
     * 
     * @param path file path
     * @paren autoSave whether this is a write for auto save
     * @return whether the write was successful
     */
    private boolean writeFileHelper(String path, boolean autoSave) {
        try {
            String htmlSource = Log.getText();
            String htmlSource2 = "";
            if (htmlSource.indexOf("<body>") != -1) {
                int start = htmlSource.indexOf("<body>") + 6;
                htmlSource = htmlSource.substring(start);
                while (htmlSource.contains("<body>")) {
                    start = htmlSource.indexOf("<body>") + 6;
                    htmlSource = htmlSource.substring(start);
                }
                int end = htmlSource.indexOf("</body>");
                if (end == -1) end = htmlSource.length();
                htmlSource = htmlSource.substring(0, end);
            }
            String[] lines = htmlSource.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                htmlSource2 += lines[i];
            }
            htmlSource = "\n<html>\n<head>\n</head>\n<body>\n" + htmlSource2 + "\n</body>\n</html>\n";
            FileOutputStream fos = new FileOutputStream(path);
            GZIPOutputStream gz = new GZIPOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(gz);
            oos.writeObject(htmlSource2);
            if (!autoSave) Log.setUnchangedStatus();
            int count = DatasheetPane.getTabCount();
            oos.writeObject(new Integer(count));
            for (int i = 0; i < count; ++i) {
                Spreadsheet s = ((SpreadsheetScrollPane) DatasheetPane.getComponentAt(i)).getSpreadsheet();
                if (!autoSave) {
                    s.setUnchangedStatus();
                    s.closeFile();
                }
                oos.writeObject(((SpreadsheetModel) s.getModel()).getTabDelimitedValues());
            }
            if (!autoSave) DatasheetPane.resetTabTitles();
            count = app.getChartFrames().size();
            if (count != 0) {
                oos.writeObject(new Integer(count));
                for (int i = 0; i < count; ++i) {
                    String title = app.getChartFrames().get(i).getChartTitle();
                    JFreeChart chart = app.getChartFrames().get(i).getChart();
                    try {
                        oos.writeObject(title);
                        oos.writeObject(chart);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            oos.flush();
            oos.close();
            gz.close();
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Writes project file to the temporary file path.
     *
     */
    public void autoSaveWrite() {
        if (writeFileHelper(tempFile.getPath(), true)) {
            app.restoreStatusTimer(1);
            app.setStatus("Auto save completed.");
        } else {
            app.restoreStatusTimer(1);
            app.setStatus("Auto save failed.");
        }
    }

    /**
     * Returns the name of this project.
     *
     * @return name, or empty string if this project has no name
     */
    public String getName() {
        if (savedFile == null) return "";
        return savedFile.getName();
    }

    /**
     * Read a project file from the given path.  The read log is appended
     * to the existing log, and the datasheets are added to the existing
     * datasheet pane.
     * 
     * @param path  project file path
     * @return  true if a project file is read successfully, or false otherwise
     */
    public boolean readFile(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            GZIPInputStream gs = new GZIPInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(gs);
            Object o = ois.readObject();
            String htmlSource;
            if (o instanceof String) {
                htmlSource = (String) o;
            } else return false;
            int count;
            o = ois.readObject();
            if (o instanceof Integer) {
                count = ((Integer) o).intValue();
            } else return false;
            String[] sheets = new String[count];
            for (int i = 0; i < count; ++i) {
                o = ois.readObject();
                if (o instanceof String) {
                    String s = (String) o;
                    sheets[i] = s;
                } else return false;
            }
            ArrayList<String> titles = new ArrayList<String>();
            ArrayList<JFreeChart> charts = new ArrayList<JFreeChart>();
            int countCharts = 0;
            try {
                o = ois.readObject();
                if (o != null) {
                    if (o instanceof Integer) {
                        countCharts = ((Integer) o).intValue();
                    } else {
                        System.err.println("num charts not an integer");
                        return false;
                    }
                    for (int i = 0; i < countCharts; ++i) {
                        try {
                            o = ois.readObject();
                            if (o instanceof String) {
                                titles.add((String) o);
                            } else {
                                System.err.println("not a chart title");
                                return false;
                            }
                        } catch (IOException ex) {
                            System.err.println("failed to read chart title");
                            return false;
                        }
                        try {
                            o = ois.readObject();
                            if (o instanceof JFreeChart) {
                                charts.add((JFreeChart) o);
                            } else {
                                System.err.println("not a chart object");
                                return false;
                            }
                        } catch (IOException ex) {
                            System.err.println("failed to read chart object");
                            return false;
                        }
                    }
                }
            } catch (EOFException e) {
                System.err.println("end of file");
            }
            ois.close();
            gs.close();
            fis.close();
            if (close()) {
                Log.overwrite(htmlSource);
                Log.setUnchangedStatus();
                for (int i = 0; i < count; ++i) {
                    DatasheetPane.addDatasheet(sheets[i], null);
                }
                for (int i = 0; i < countCharts; ++i) {
                    StatcatoChartFrame frame = new StatcatoChartFrame(titles.get(i), charts.get(i), app);
                    frame.pack();
                    frame.setVisible(true);
                }
                savedFile = new File(path);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * If the project is not yet saved, prompts the user to save.
     * Closes the open project (log and datasheets).
     * 
     * @return whether the project can be closed.
     */
    public boolean close() {
        Object[] options = { "Save Project", "Close Without Saving" };
        int choice;
        if (Log.getChangedStatus() || DatasheetPane.getChangedStatus()) {
            choice = JOptionPane.showOptionDialog(app, "This project is not yet saved.", "Unsaved project...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
            if (choice == 0) {
                if (!writeToFile(false)) return false;
            }
        }
        Log.clear();
        Log.setUnchangedStatus();
        DatasheetPane.removeAll();
        savedFile = null;
        app.removeChartFrames();
        return true;
    }

    /**
     * Deletes temporary file if it exists.
     */
    public void exit() {
        if (DELETEAUTOSAVEFILE) tempFile.delete();
    }
}
