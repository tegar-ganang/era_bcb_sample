package royere.cwi.appl;

import royere.cwi.framework.*;
import royere.cwi.structure.*;
import royere.cwi.input.*;
import royere.cwi.db.GraphDbMediator;
import royere.cwi.layout.*;
import royere.cwi.output.*;
import royere.cwi.view.*;
import royere.cwi.util.*;
import royere.cwi.input.ui.RGraphChooser;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Collection of action listeners for file commands. Typically, the listeners
 * are defined as local variables, and initialized through an anonymous innner
 * class. These variables can then be used as listeners for menu entries.
 * 
 * @see MenuEntry
 * @see Menu
 * @author Ivan Herman
 * @author yugen
 */
public class FileCommands {

    /** Debug object.  Logs data to various channels. */
    private static Logger logger = Logger.getLogger("royere.cwi.appl.FileCommands");

    /** 
  * File filter which is initialized with a description string and a
  * suffix.
  */
    static class myFileFilter extends javax.swing.filechooser.FileFilter {

        String mask;

        String descr;

        /**
    * @param c description words (to appear in the pull down menu of the file chooser
    * @param m suffix of the file names (without the "." character!)
    */
        myFileFilter(String c, String m) {
            super();
            descr = c;
            mask = m;
        }

        public boolean accept(File aFile) {
            return (aFile.isDirectory() || aFile.getName().endsWith("." + mask));
        }

        public String getDescription() {
            return descr + " files (*." + mask + ")";
        }
    }

    ;

    static class inputTuple {

        String file;

        String parser;
    }

    ;

    /**
  * Accepted input types.
  * If you want to add a new type of input file (and you have the parser), just
  * extend this array. Everything else is done automatically
  */
    static myFileFilter[] importTypes;

    /**
  * Accepted output types.
  * If you want to add a new type of output file (and you have the encoder), just
  * extend this array. Everything else is done automatically
  */
    static myFileFilter[] exportTypesWithJimi = new myFileFilter[] { new myFileFilter("PNG", "png"), new myFileFilter("Windows Bitmap", "bmp"), new myFileFilter("JPEG", "jpg"), new myFileFilter("SVG", "svg") };

    /**
  * Export types without JIMI
  */
    static myFileFilter[] exportTypesWithoutJimi = new myFileFilter[] { new myFileFilter("SVG", "svg") };

    static myFileFilter[] exportTypes = null;

    static myFileFilter[] saveTypes = new myFileFilter[] { new myFileFilter("XML", "xml"), new myFileFilter("Comma-separated", "csv"), new myFileFilter("NeXT Property List", "plist") };

    static myFileFilter[] saveHistoryTypes = new myFileFilter[] { new myFileFilter("Royere Script", "rs") };

    /** 
  * Separate initialization method, called by the main application when all system
  * properties and factories have been initialized.
  */
    void init() {
        Factory.Extension inputs[] = InputFactory.getAvailableInputs();
        importTypes = new myFileFilter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            Input instance = (Input) inputs[i].theInstance;
            importTypes[i] = new myFileFilter(inputs[i].theName, instance.getSuffix());
        }
        try {
            Class jimi = Class.forName("com.sun.jimi.core.Jimi");
            exportTypes = exportTypesWithJimi;
        } catch (Exception e) {
            exportTypes = exportTypesWithoutJimi;
        }
    }

    static boolean checkFileType(File f, myFileFilter[] types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].accept(f) == true) return true;
        }
        return false;
    }

    public ActionListener NewXMLURL = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String url = evt.getActionCommand();
            if (validateUrl(url)) {
                Royere.viewManager.newView(url, "GraphXML", frame);
                return;
            }
            while (true) {
                String currentSelection = url;
                url = (String) JOptionPane.showInputDialog(null, "URL: ", "Input URL for GraphXML Specification", JOptionPane.QUESTION_MESSAGE, null, null, currentSelection);
                if (url == null) {
                    return;
                } else if (validateUrl(url)) {
                    Royere.viewManager.newView(url, "GraphXML", frame);
                    return;
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file type", "Invalid file type", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        public boolean validateUrl(String url) {
            if (url == null) {
                return false;
            }
            if ((url.endsWith(".xml") || url.endsWith(".XML")) && new File(url).exists()) {
                return true;
            }
            return false;
        }
    };

    /**
  * Clone a view. A command is issued to the viewManager.
  *
  * @see ViewManager
  */
    public ActionListener Clone = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            Royere.viewManager.cloneView(source.getFrame());
        }
    };

    /**
  * Quit. A JOptionPane.showConfirmDialog is issued first
  */
    public ActionListener Quit = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            int i = JOptionPane.showConfirmDialog(null, "Are you sure you want to quit Royere?", "Royere warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (i == JOptionPane.YES_OPTION) System.exit(0);
        }
    };

    /**
  * Close a window. Invokes an internal function which looks
  * at the window administration to see if this was the last window to close or not.
  * If yes, the application exists. To avoid problem, a confirm dialog is also 
  * issued first.
  */
    public ActionListener Close = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            closeWindow(source.getFrame());
        }
    };

    /**
  * A window listener which uses the windowClosing event (note that frames are
  * created in such a way that closing has to be generated explicitly.
  * Invokes an internal function which looks
  * at the window administration to see if this was the last window to close or not.
  * If yes, the application exists. To avoid problem, a confirm dialog is also 
  * issued first.
  */
    public WindowListener WindowClose = new WindowAdapter() {

        public void windowClosing(WindowEvent e) {
            closeWindow((RFrame) e.getSource());
        }
    };

    /**
  * Open a graph from a file, in a new window. 
  */
    public ActionListener importFileNewWindow = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String filename = evt.getActionCommand();
            inputTuple file = getNewInputFile(filename);
            if (file != null) {
                Royere.viewManager.newView(file.file, file.parser, null);
            }
        }
    };

    /**
  * Open a graph from a file. 
  */
    public ActionListener importFile = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String filename = evt.getActionCommand();
            inputTuple file = getNewInputFile(filename);
            if (file != null) {
                Royere.viewManager.newView(file.file, file.parser, frame);
            }
        }
    };

    /**
  * Open a graph from the graph database, in a new window. 
  */
    public ActionListener openGraphNewWindow = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String graphName = evt.getActionCommand();
            if (graphName == null || graphName.equals("Open in new window...")) {
                graphName = getNewInputGraph(frame);
            }
            if (graphName != null && graphName.trim().length() > 0) {
                Royere.viewManager.newView(graphName, null, null);
            }
        }
    };

    /**
  * Open a graph from the graph database. 
  */
    public ActionListener openGraph = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String graphName = evt.getActionCommand();
            if (graphName == null || graphName.equals("Open...")) {
                graphName = getNewInputGraph(frame);
            }
            if (graphName != null && graphName.trim().length() > 0) {
                Royere.viewManager.newView(graphName, null, frame);
            }
        }
    };

    /**
  * Export command (for SVG and the available image options). 
  */
    public ActionListener export = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            String name = FileCommands.getNewOutputFile("Export graph drawing", exportTypes);
            if (name != null) {
                ControlViewMessage c;
                c = new ControlViewMessage(this, ControlViewMessage.EXPORT, name);
                frame.theView.triggerThroughMessage(c);
            }
        }
    };

    public static class SaveHistoryActionListener implements ActionListener {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            ViewPanel panel = (ViewPanel) frame.theView.getPanel();
            Graph graph = panel.getGraph();
            String fileName = null;
            if (evt.getActionCommand() != null && !evt.getActionCommand().startsWith("Save History As...")) {
                fileName = evt.getActionCommand();
            }
            logger.log(Priority.DEBUG, "saveHistoryAs.actionPerformed(): fileName = '" + fileName + "'");
            if (fileName == null) {
                String currentFileName = frame.theView.getFileName();
                int index = currentFileName.lastIndexOf(System.getProperty("file.separator"));
                if (index < 0) {
                    index = currentFileName.length() - 1;
                }
                String defaultDir = currentFileName.substring(0, index);
                fileName = FileCommands.getNewOutputFile("Save History", defaultDir, saveHistoryTypes);
            }
            logger.log(Priority.DEBUG, "saveHistoryAs.actionPerformed(): fileName = '" + fileName + "'");
            saveHistoryToScript(frame, graph, fileName);
        }

        public void saveHistoryToScript(RFrame frame, Graph graph, String fileName) {
            logger.log(Priority.DEBUG, "saveHistoryToScript(): graph = '" + graph + "', fileName = '" + fileName + "'");
            History history = frame.theView.getHistory();
            String commandLines = history.toScript();
            logger.log(Priority.DEBUG, "saveHistoryToScript(): commandLines = " + commandLines);
            File script = new File(fileName);
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new BufferedWriter(new FileWriter(script)));
            } catch (IOException ioe) {
                logger.error("saveHistoryToScript(): Couldn't open script for writing", ioe);
            }
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            writer.write(commandLines, 0, commandLines.length());
            writer.close();
            frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    ;

    public ActionListener saveHistoryAs = new SaveHistoryActionListener();

    /**
  * Save command
  *
  * @see GraphDomMediator
  */
    public static class SaveActionListener implements ActionListener {

        private Graph graph = null;

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            ViewPanel panel = (ViewPanel) frame.theView.getPanel();
            if (graph == null) {
                graph = panel.getGraph();
            }
            Layout layout = panel.getGraphLayout();
            saveGraphToDb(frame, null, graph, layout);
        }

        public void saveGraph(RFrame frame, Graph graph, Layout layout) {
            String fileName = null;
            File file = GraphDomMediator.getFileForGraph(graph);
            if (file != null) {
                fileName = file.getAbsolutePath();
            } else {
                fileName = frame.theView.getFileName();
            }
            if (fileName == null) {
                fileName = FileCommands.getNewOutputFile("Save graph", saveTypes);
            } else if (!fileName.endsWith(".xml") && !fileName.endsWith(".XML") && !fileName.endsWith(".csv") && !fileName.endsWith(".CSV") && !fileName.endsWith(".plist") && !fileName.endsWith(".PLIST")) {
                fileName = FileCommands.getNewOutputFile("Save graph", saveTypes);
            }
            FileCommands.saveGraph(frame, fileName, graph, layout);
        }
    }

    ;

    public ActionListener save = new SaveActionListener();

    /**
  * Save As command
  *
  * @see GraphDomMediator
  */
    public ActionListener saveAs = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            ViewPanel panel = (ViewPanel) frame.theView.getPanel();
            String fileName = null;
            if (evt.getActionCommand() != null && !evt.getActionCommand().startsWith("Save As")) {
                fileName = evt.getActionCommand();
            }
            logger.log(Priority.DEBUG, "saveAs.actionPerformed(): fileName = '" + fileName + "'");
            if (fileName == null) {
                String currentFileName = frame.theView.getFileName();
                int index = currentFileName.lastIndexOf(System.getProperty("file.separator"));
                if (index < 0) {
                    index = currentFileName.length() - 1;
                }
                String defaultDir = currentFileName.substring(0, index);
                fileName = FileCommands.getNewOutputFile("Save graph", defaultDir, saveTypes);
            }
            Graph graph = panel.getGraph();
            if (fileName != null) {
                graph.setLabel(fileName);
            }
            FileCommands.saveGraphToDb(frame, null, graph, panel.getGraphLayout());
        }
    };

    /**
   * Helper method to save a graph to an XML file.  
   */
    public static void saveGraph(RFrame frame, String fileName, Graph graph, Layout layout) {
        if (fileName == null || fileName.trim().length() == 0) {
            logger.log(Priority.ERROR, "saveGraph(): Couldn't save graph '" + graph + "' because filename is unknown");
            return;
        }
        logger.log(Priority.DEBUG, "saveGraph(): Saving graph '" + graph + "' to file '" + fileName + "'");
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        if (fileName.endsWith(".xml") || fileName.endsWith(".XML")) {
            GraphDomMediator.saveToXml(new File(fileName), graph, layout);
        } else if (fileName.endsWith(".csv") || fileName.endsWith(".CSV")) {
            GraphCsvMediator.saveToCsv(new File(fileName), graph, layout);
        } else if (fileName.endsWith(".plist") || fileName.endsWith(".PLIST")) {
            GraphPlistMediator.saveToPlist(new File(fileName), graph, layout);
        }
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        int index = fileName.lastIndexOf(System.getProperty("file.separator"));
        if (index < 0) {
            index = fileName.lastIndexOf("/");
        }
        if (index < 0) {
            index = -1;
        }
        index++;
        String title = fileName.substring(index, fileName.length());
        frame.setTitle(title);
    }

    /**
   * Helper method to save a graph to a database.  
   */
    public static void saveGraphToDb(RFrame frame, String dbName, Graph graph, Layout layout) {
        if (dbName == null || dbName.trim().length() == 0) {
            dbName = "gvf";
            logger.log(Priority.DEBUG, "saveGraphToDb(): Database name is unknown; defaulting to '" + dbName + "'");
        }
        logger.log(Priority.DEBUG, "saveGraphToDb(): Saving graph '" + graph + "' to '" + dbName + "'");
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        Graph oldGraph = GraphDbMediator.getGraphByName(graph.getName());
        if (oldGraph != null) {
            logger.log(Priority.DEBUG, "saveGraphToDb(): Database '" + dbName + "'" + " already contains a graph with name '" + graph.getName() + "'; deleting...");
            GraphDbMediator.deleteElement(oldGraph);
        }
        GraphDbMediator.storeLayout(layout);
        GraphDbMediator.storeViewState(frame.theView);
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    private String dumpFile = null;

    private String dumpSuffix = null;

    private int dumpIndex = 1;

    /**
  * Screen dump command
  */
    public ActionListener dump = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            if (dumpFile == null) {
                String name = FileCommands.getNewOutputFile("Export graph drawing", exportTypes);
                if (name == null) return;
                byte[] characters = name.getBytes();
                dumpFile = new String(characters, 0, name.length() - 4);
                dumpSuffix = new String(characters, name.length() - 4, 4);
            }
            String finalName = dumpFile + "_" + dumpIndex + dumpSuffix;
            dumpIndex++;
            ControlViewMessage c;
            c = new ControlViewMessage(this, ControlViewMessage.EXPORT, finalName);
            frame.theView.triggerThroughMessage(c);
        }
    };

    /**
  * Screen dump command
  */
    public ActionListener dumpReset = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            dumpFile = null;
            dumpSuffix = null;
            dumpIndex = 1;
            JOptionPane.showMessageDialog(frame, "Screen dump sequence reset", "Royere message", JOptionPane.INFORMATION_MESSAGE, null);
        }
    };

    /**
  * Print command. Issues a PRINT control message to the
  * view
  */
    public ActionListener Print = new ActionListener() {

        public void actionPerformed(ActionEvent evt) {
            MenuEntry.MenuItem source = (MenuEntry.MenuItem) evt.getSource();
            RFrame frame = source.getFrame();
            Royere.sendCommandToView(this, frame.theView, ControlViewMessage.PRINT);
        }
    };

    /**
  * Close a window. The function looks
  * at the window administration to see if this was the last window to close or not.
  * If yes, the application exists. To avoid problem, a confirm dialog is also 
  * issued first.
  *
  * @param frame the frame to be closed
  */
    private void closeWindow(RFrame frame) {
        ViewManager viewManager = Royere.viewManager;
        HashMap allViews = viewManager.allViews;
        HashMap allTopViews = viewManager.allTopViews;
        if (!(frame instanceof ROverviewFrame) && allTopViews.size() == 1) {
            int i = JOptionPane.showConfirmDialog(null, "You are closing the last window of Royere.\n" + "Are you sure you want to quit the program?", "Royere warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (i == JOptionPane.NO_OPTION) return;
        }
        viewManager.getRidOfOverviews(frame);
        if (frame instanceof ROverviewFrame) {
            RFrame oFrame = ((ROverviewFrame) frame).originalFrame;
            oFrame.theView.properties.setProperty(Keys.VIEW_FILTER, null);
            oFrame.theView.properties.setProperty(Keys.CONTEXT_FILTER, null);
            oFrame.theView.triggerThroughMessage(new ControlViewMessage(this, ControlViewMessage.REFRESH, null));
            oFrame.overviewFrame = null;
        }
        viewManager.killOneFrame(frame);
        if (allTopViews.isEmpty()) System.exit(0);
    }

    private static String dir = null;

    /**
  * Get a file through a file chooser widget
  *
  * @param defaultFilename if not null, we open the file with this filename, and
  * use the file chooser only as a last resort.
  */
    public static inputTuple getNewInputFile(String defaultFilename) {
        File file = null;
        if (defaultFilename != null) {
            file = new File(defaultFilename);
            if (checkFileType(file, importTypes) && file.isFile()) {
                inputTuple retval = new inputTuple();
                retval.file = file.getAbsolutePath();
                String suffix = defaultFilename.substring(defaultFilename.lastIndexOf(".") + 1);
                for (int i = 0; i < importTypes.length; i++) {
                    if (importTypes[i].mask.equalsIgnoreCase(suffix)) {
                        retval.parser = importTypes[i].descr;
                        break;
                    }
                }
                return retval;
            }
        }
        if (dir == null) {
            String startDir = System.getProperty("user.dir");
            String separator = System.getProperty("file.separator");
            String ourFiles = startDir + separator + "royere" + separator + "Graphs";
            File ourDirs = new File(ourFiles);
            if (ourDirs.exists() && ourDirs.isDirectory() && ourDirs.canRead()) {
                dir = ourFiles;
            } else {
                dir = startDir;
            }
        }
        while (true) {
            JFileChooser fileChooser = new JFileChooser(dir);
            for (int i = 0; i < importTypes.length; i++) {
                fileChooser.addChoosableFileFilter(importTypes[i]);
            }
            fileChooser.setFileFilter(importTypes[0]);
            int option = fileChooser.showOpenDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                if (checkFileType(file, importTypes) && file.isFile()) {
                    dir = fileChooser.getCurrentDirectory().getAbsolutePath();
                    inputTuple retval = new inputTuple();
                    retval.file = file.getAbsolutePath();
                    retval.parser = ((myFileFilter) fileChooser.getFileFilter()).descr;
                    return retval;
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid input type", "Invalid file type", JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }
            } else {
                return null;
            }
        }
    }

    /**
  * Get a graph name through a graph chooser widget
  */
    public static String getNewInputGraph(RFrame frame) {
        logger.log(Priority.DEBUG, "getNewInputGraph()");
        String[] labels = GraphDbMediator.getGraphLabels();
        if (labels == null) {
            labels = new String[0];
        } else {
            if (labels.length == 0) {
                RoyereError.print("Graph database contains no graphs.");
                return null;
            }
        }
        logger.log(Priority.DEBUG, "getNewInputGraph(): Found " + labels.length + " labels");
        RGraphChooser graphChooser = new RGraphChooser(frame, labels);
        graphChooser.show();
        return graphChooser.getSelected();
    }

    private static String dirOut = null;

    /**
   * Get a file name for output through a file chooser widget
   */
    static String getNewOutputFile(String title, myFileFilter[] fileTypes) {
        return FileCommands.getNewOutputFile(title, null, fileTypes);
    }

    /**
   * Get a file name for output through a file chooser widget
   */
    static String getNewOutputFile(String title, String defaultDir, myFileFilter[] fileTypes) {
        if (dirOut == null) {
            dirOut = defaultDir;
        }
        if (dirOut == null) {
            dirOut = System.getProperty("user.dir");
        }
        File file = null;
        while (true) {
            JFileChooser fileChooser = new JFileChooser(dirOut);
            fileChooser.setDialogTitle(title);
            for (int i = 0; i < fileTypes.length; i++) {
                fileChooser.addChoosableFileFilter(fileTypes[i]);
            }
            fileChooser.setFileFilter(fileTypes[0]);
            int option = fileChooser.showSaveDialog(null);
            if (option == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                String path = file.getAbsolutePath();
                if (!checkFileType(file, fileTypes)) {
                    try {
                        myFileFilter f = (myFileFilter) fileChooser.getFileFilter();
                        path += ("." + f.mask);
                        file = new File(path);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Invalid input type", "Invalid file type", JOptionPane.INFORMATION_MESSAGE);
                        continue;
                    }
                }
                dirOut = fileChooser.getCurrentDirectory().getAbsolutePath();
                if (file.exists()) {
                    int i = JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (i != JOptionPane.YES_OPTION) continue;
                }
                return path;
            } else {
                return null;
            }
        }
    }
}
