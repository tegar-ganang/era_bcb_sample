package gov.sns.apps.jeri.apps.dbexport;

import gov.sns.apps.jeri.apps.mpsfileeditor.MPSFileEditDialog;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.JList;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import gov.sns.apps.jeri.Main;
import gov.sns.apps.jeri.data.SignalField;
import gov.sns.apps.jeri.data.Signal;
import gov.sns.apps.jeri.data.IOC;
import gov.sns.apps.jeri.data.DBFile;
import gov.sns.apps.jeri.application.JeriInternalFrame;

/**
 * Provides an interface for the user to export data to a db file.
 * 
 * @author Chris Fowlkes
 */
public class DBExportFrame extends JeriInternalFrame {

    private JScrollPane scrollPane = new JScrollPane();

    private BorderLayout frameLayout = new BorderLayout();

    private JPanel southPanel = new JPanel();

    private JPanel buttonPanel = new JPanel();

    private JButton okButton = new JButton();

    private JButton cancelButton = new JButton();

    private BorderLayout southPanelLayout = new BorderLayout();

    private GridLayout buttonPanelLayout = new GridLayout();

    private JPanel statusBarPanel = new JPanel();

    private BorderLayout statusBarPanelLayout = new BorderLayout();

    private JLabel progressLabel = new JLabel();

    private JProgressBar progressBar = new JProgressBar();

    /**
   * Holds the file names the user can use to select signals to export.
   */
    private String[] fileNames;

    /**
   * Holds the group names the user can use to select signals to export.
   */
    private String[] iocNames;

    /**
   * Flag used to determine if the export thread is running.
   */
    private boolean threadActive = false;

    private JPanel listPanel = new JPanel();

    private BorderLayout listPanelLayout = new BorderLayout();

    private JPanel eastPanel = new JPanel();

    private BorderLayout eastPanelLayout = new BorderLayout();

    private JPanel addButtonPanel = new JPanel();

    private GridLayout addButtonPanelLayout = new GridLayout();

    private JButton fileButton = new JButton();

    private JButton iocButton = new JButton();

    private JButton clearButton = new JButton();

    private JPanel exportOptionsPanel = new JPanel();

    private BorderLayout exportOptionsPanelLayout = new BorderLayout();

    private JPanel versionPanel = new JPanel();

    private JLabel versionLabel = new JLabel();

    private JComboBox versionComboBox = new JComboBox();

    private JList list = new JList();

    private DefaultListModel listModel = new DefaultListModel();

    /**
   * Holds the dialog used to locate an output file.
   */
    private JFileChooser fileDialog;

    private JButton locationButton = new JButton();

    /**
   * Holds the dialog used by the user to review the files before they are 
   * written.
   */
    private MPSFileEditDialog fileReviewDialog;

    /**
   * Holds the icon for the print button.
   */
    private Icon printIcon;

    /**
   * Holds a reference to a <CODE>DBExport</CODE>. All the functionality should
   * go into this class.
   */
    private DBExport dbExport = new DBExport();

    /**
   * Creates a new <CODE>ExportFrame</CODE>.
   */
    public DBExportFrame() {
        try {
            jbInit();
            list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    enableOKButton();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Component initialization.
   * 
   * @throws java.lanng.Exception Thrown on initialization error.
   */
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(frameLayout);
        this.setSize(new Dimension(400, 300));
        this.setTitle("Export DB File");
        frameLayout.setVgap(5);
        frameLayout.setHgap(5);
        southPanel.setLayout(southPanelLayout);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        okButton.setText("Export...");
        okButton.setMnemonic('x');
        okButton.setEnabled(false);
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        cancelButton.setText("Close");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        southPanelLayout.setVgap(5);
        southPanelLayout.setHgap(5);
        buttonPanelLayout.setHgap(5);
        statusBarPanel.setLayout(statusBarPanelLayout);
        progressLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        progressLabel.setText(" ");
        progressBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        listPanel.setLayout(listPanelLayout);
        listPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        listPanelLayout.setHgap(5);
        listPanelLayout.setVgap(5);
        eastPanel.setLayout(eastPanelLayout);
        addButtonPanel.setLayout(addButtonPanelLayout);
        addButtonPanelLayout.setRows(4);
        addButtonPanelLayout.setColumns(1);
        addButtonPanelLayout.setVgap(5);
        fileButton.setText("Export Source File...");
        fileButton.setMnemonic('u');
        fileButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileButton_actionPerformed(e);
            }
        });
        iocButton.setText("Export IOC...");
        iocButton.setMnemonic('I');
        iocButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                iocButton_actionPerformed(e);
            }
        });
        clearButton.setText("Clear");
        clearButton.setMnemonic('e');
        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clearButton_actionPerformed(e);
            }
        });
        exportOptionsPanel.setLayout(exportOptionsPanelLayout);
        exportOptionsPanelLayout.setHgap(5);
        versionLabel.setText("Epics Version:");
        versionLabel.setDisplayedMnemonic('p');
        versionLabel.setLabelFor(versionComboBox);
        list.setModel(listModel);
        listModel.addListDataListener(new ListDataListener() {

            public void intervalAdded(ListDataEvent e) {
                listModel_intervalAdded(e);
            }

            public void intervalRemoved(ListDataEvent e) {
                listModel_intervalRemoved(e);
            }

            public void contentsChanged(ListDataEvent e) {
                listModel_contentsChanged(e);
            }
        });
        locationButton.setText("Change Location...");
        locationButton.setMnemonic('L');
        locationButton.setEnabled(false);
        locationButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                locationButton_actionPerformed(e);
            }
        });
        dbExport.setMessageLabel(progressLabel);
        dbExport.setProgressBar(progressBar);
        eastPanel.add(addButtonPanel, BorderLayout.NORTH);
        addButtonPanel.add(fileButton, null);
        addButtonPanel.add(iocButton, null);
        addButtonPanel.add(clearButton, null);
        addButtonPanel.add(locationButton, null);
        statusBarPanel.add(progressLabel, BorderLayout.CENTER);
        statusBarPanel.add(progressBar, BorderLayout.EAST);
        buttonPanel.add(okButton, null);
        buttonPanel.add(cancelButton, null);
        southPanel.add(buttonPanel, BorderLayout.EAST);
        southPanel.add(statusBarPanel, BorderLayout.SOUTH);
        scrollPane.getViewport().add(list, null);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        listPanel.add(eastPanel, BorderLayout.EAST);
        versionPanel.add(versionLabel, null);
        versionPanel.add(versionComboBox, null);
        exportOptionsPanel.add(versionPanel, BorderLayout.EAST);
        listPanel.add(exportOptionsPanel, BorderLayout.SOUTH);
        this.getContentPane().add(southPanel, BorderLayout.SOUTH);
        this.getContentPane().add(listPanel, BorderLayout.CENTER);
    }

    /**
   * Called when the file button is clicked. This method allows the user to 
   * choose a source file from the database to export.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocation of this method.
   */
    private void fileButton_actionPerformed(ActionEvent e) {
        Thread dataLoadThread = new Thread(new Runnable() {

            public void run() {
                synchronized (dbExport) {
                    dbExport.setCanceled(false);
                    threadActive = true;
                    enableOKButton();
                    try {
                        if (fileNames == null) fileNames = dbExport.loadDBFileNames();
                        if (!dbExport.isCanceled()) SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                Object selectedFileName = JOptionPane.showInputDialog(DBExportFrame.this, "Which file do you wish to export?", "Export File", JOptionPane.QUESTION_MESSAGE, null, fileNames, fileNames[0]);
                                if (selectedFileName != null) addDBFile(selectedFileName.toString());
                            }
                        });
                    } catch (java.sql.SQLException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        threadActive = false;
                        enableOKButton();
                    }
                }
            }
        });
        dataLoadThread.start();
    }

    /**
   * Adds the given DB files to the dialog.
   * 
   * @param selectedFileName The name of the DB file that will be exported.
   */
    public void addDBFile(final String selectedFileName) {
        Thread dataLoadThread = new Thread(new Runnable() {

            public void run() {
                synchronized (dbExport) {
                    dbExport.setCanceled(false);
                    threadActive = true;
                    enableOKButton();
                    try {
                        IOC dbFileIOC = findIOCForDBFile(selectedFileName);
                        if (dbFileIOC != null) addIOCToList(dbFileIOC); else addDBFileToList(new DBFile(selectedFileName));
                    } catch (java.sql.SQLException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        threadActive = false;
                        enableOKButton();
                    }
                }
            }
        });
        dataLoadThread.start();
    }

    /**
   * Adds the DB files associated with the given IOC IDs to the dialog.
   * 
   * @param iocID The IOC ID to add to the dialog.
   */
    public void addIOC(final String iocID) {
        Thread dataLoadThread = new Thread(new Runnable() {

            public void run() {
                synchronized (dbExport) {
                    dbExport.setCanceled(false);
                    threadActive = true;
                    enableOKButton();
                    try {
                        IOC selectedIOC = findDBFilesForIOC(iocID);
                        if (!dbExport.isCanceled()) addIOCToList(selectedIOC);
                    } catch (java.sql.SQLException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        threadActive = false;
                        enableOKButton();
                    }
                }
            }
        });
        dataLoadThread.start();
    }

    /**
   * Adds the given DB file to the list. This method prompts the user for a 
   * location. This method uses <CODE>SwingUtilities.invokeLater</CODE> to make 
   * it thread safe.
   * 
   * @param dbFileName The <CODE>DBFile</CODE> to add to the list.
   */
    private void addDBFileToList(final DBFile file) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                listModel.addElement(file);
            }
        });
    }

    /**
   * Asks the user to select a directory.
   * 
   * @param defaultValue The default directory. This will be the value initially selected when the user is prompted.
   * @return The <CODE>File</CODE> representing the directory the user has chosen, or <CODE>null</CODE> if the user canceled.
   */
    private File promptForDirectory(String defaultDirectory) {
        if (fileDialog == null) {
            fileDialog = new JFileChooser();
            fileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileDialog.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public boolean accept(File chosenFile) {
                    if (chosenFile.isDirectory()) return true; else return false;
                }

                @Override
                public String getDescription() {
                    return "Save Location (File Name Will Be Ignored)";
                }
            });
        }
        Properties settings = getApplicationProperties();
        if (defaultDirectory != null) fileDialog.setCurrentDirectory(new File(defaultDirectory)); else {
            String directoryName = settings.getProperty("ExportFrame.fileDirectory");
            fileDialog.setCurrentDirectory(new File(directoryName));
        }
        int option = fileDialog.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File path = fileDialog.getSelectedFile();
            settings.setProperty("ExportFrame.fileDirectory", path.getAbsolutePath());
            return path;
        } else return null;
    }

    /**
   * Adds all of the files associated with the given <CODE>IOC</CODE> to the 
   * list. This method uses <CODE>SwingUtilities.invokeLater</CODE> to make it 
   * thread safe.
   * 
   * @param ioc The <CODE>IOC</CODE> for which to add the associated DB files.
   */
    private void addIOCToList(IOC ioc) {
        try {
            setMessage("Adding DB files to the list");
            setProgressIndeterminate(true);
            int dbFileCount = ioc.getDBFileCount();
            setProgressMaximum(dbFileCount);
            setProgressValue(0);
            setProgressIndeterminate(false);
            for (int i = 0; i < dbFileCount; i++) {
                addDBFileToList(ioc.getDBFileAt(i));
                setProgressValue(i + 1);
            }
        } finally {
            setProgressValue(0);
            setProgressIndeterminate(false);
            setMessage(" ");
        }
    }

    /**
   * Gets the instance of <CODE>IOC</CODE> associated with the given DB file.
   * If a DB file does not have an IOC association, the value returned will be 
   * <CODE>null</CODE>. This method does not look for multiple IOC associations
   * for the DB file since there should be only one. If more than one exists in 
   * the database for a DB file, only the first is returned. The corresponding
   * DB file will have been added to the instance of <CODE>IOC</CODE> that is 
   * returned.
   * 
   * @param dbFileName The name of the DB files for which to look up the IOC relationships.
   * @return <CODE>IOC</CODE> representing the IOC associations in the database for the given files.
   * @throws java.sql.SQLException Thrown on sql error.
   */
    private IOC findIOCForDBFile(String dbFileName) throws java.sql.SQLException {
        IOC ioc;
        Connection oracleConnection = getDataSource().getConnection();
        try {
            Statement query = oracleConnection.createStatement();
            try {
                StringBuffer sql = new StringBuffer("SELECT DVC_ID, EXT_SRC_DIR_NM FROM ");
                sql.append(Main.SCHEMA);
                sql.append(".IOC_DB_FILE_ASGN WHERE EXT_SRC_FILE_NM = '");
                sql.append(dbFileName);
                sql.append("'");
                ResultSet result = query.executeQuery(sql.toString());
                try {
                    if (result.next()) {
                        ioc = new IOC(result.getString("DVC_ID"));
                        String directoryName = result.getString("EXT_SRC_DIR_NM");
                        DBFile dbFile = new DBFile(dbFileName, directoryName);
                        ioc.addDBFile(dbFile);
                    } else ioc = null;
                } finally {
                    result.close();
                }
            } finally {
                query.close();
            }
        } finally {
            oracleConnection.close();
        }
        return ioc;
    }

    /**
   * Loads the DB file associations for the <CODE>IOC</CODE>.
   * 
   * @param iocID The ID of the <CODE>IOC</CODE> for which to load the DB file associations.
   * @return The <CODE>IOC</CODE> containing the DB file associations from the database.
   * @throws java.sql.SQLException Thrown on SQL error.
   */
    private IOC findDBFilesForIOC(String iocID) throws java.sql.SQLException {
        try {
            setProgressIndeterminate(true);
            setMessage("Loading DB file associations from the RDB");
            IOC ioc = new IOC(iocID);
            Connection oracleConnection = getDataSource().getConnection();
            try {
                Statement query = oracleConnection.createStatement();
                try {
                    StringBuffer sql = new StringBuffer("SELECT COUNT(*) FROM ");
                    sql.append(Main.SCHEMA);
                    sql.append(".IOC_DB_FILE_ASGN WHERE DVC_ID = '");
                    sql.append(iocID);
                    sql.append("'");
                    ResultSet result = query.executeQuery(sql.toString());
                    try {
                        result.next();
                        setProgressMaximum(result.getInt(1));
                        sql = new StringBuffer("SELECT EXT_SRC_DIR_NM, EXT_SRC_FILE_NM FROM ");
                        sql.append(Main.SCHEMA);
                        sql.append(".IOC_DB_FILE_ASGN WHERE DVC_ID = '");
                        sql.append(iocID);
                        sql.append("'");
                        result = query.executeQuery(sql.toString());
                        int progress = 0;
                        setProgressValue(0);
                        setProgressIndeterminate(false);
                        while (result.next()) {
                            String fileName = result.getString("EXT_SRC_FILE_NM");
                            String directoryName = result.getString("EXT_SRC_DIR_NM");
                            DBFile newDBFile = new DBFile(fileName, directoryName);
                            ioc.addDBFile(newDBFile);
                            setProgressValue(++progress);
                            if (dbExport.isCanceled()) return null;
                        }
                    } finally {
                        result.close();
                    }
                } finally {
                    query.close();
                }
            } finally {
                oracleConnection.close();
            }
            return ioc;
        } finally {
            setProgressValue(0);
            setProgressIndeterminate(false);
            setMessage(" ");
        }
    }

    /**
   * Called when the export group button is clicked. It allows the user to 
   * select a group of signals to export.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocation of this method.
   */
    private void iocButton_actionPerformed(ActionEvent e) {
        Thread dataLoadThread = new Thread(new Runnable() {

            public void run() {
                synchronized (dbExport) {
                    dbExport.setCanceled(false);
                    threadActive = true;
                    enableOKButton();
                    try {
                        if (iocNames == null) loadIOCNames();
                        if (!dbExport.isCanceled()) SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                Object selectedIOCID = JOptionPane.showInputDialog(DBExportFrame.this, "Which IOC do you wish to export?", "Export IOC", JOptionPane.QUESTION_MESSAGE, null, iocNames, iocNames[0]);
                                if (selectedIOCID != null) addIOC(selectedIOCID.toString());
                            }
                        });
                    } catch (java.sql.SQLException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        threadActive = false;
                        enableOKButton();
                    }
                }
            }
        });
        dataLoadThread.start();
    }

    /**
   * Loads the IOC IDs from the database.
   * 
   * @throws java.sql.SQLException Thrown on SQL error.
   */
    private void loadIOCNames() throws java.sql.SQLException {
        setMessage("Loading IOC list from RDB");
        setProgressIndeterminate(true);
        Connection oracleConnection = getDataSource().getConnection();
        try {
            Statement query = oracleConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            try {
                StringBuffer sql = new StringBuffer("SELECT DVC_ID FROM ");
                sql.append(Main.SCHEMA);
                sql.append(".IOC_DB_FILE_ASGN GROUP BY DVC_ID ORDER BY DVC_ID");
                ResultSet result = query.executeQuery(sql.toString());
                try {
                    if (result.last()) setProgressMaximum(result.getRow()); else setProgressMaximum(0);
                    result.beforeFirst();
                    ArrayList iocIDs = new ArrayList();
                    int progress = 0;
                    setProgressValue(0);
                    setProgressIndeterminate(false);
                    while (result.next()) {
                        iocIDs.add(result.getString("DVC_ID"));
                        setProgressValue(++progress);
                        if (dbExport.isCanceled()) break;
                    }
                    if (!dbExport.isCanceled()) {
                        iocNames = new String[iocIDs.size()];
                        iocIDs.toArray(iocNames);
                    }
                } finally {
                    result.close();
                }
            } finally {
                query.close();
            }
        } finally {
            setProgressValue(0);
            setProgressIndeterminate(false);
            setMessage(" ");
            oracleConnection.close();
        }
    }

    /**
   * Called when the clear button is clicked. This method clears the 
   * <CODE>JList</CODE>.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocation of this method.
   */
    private void clearButton_actionPerformed(ActionEvent e) {
        clearList();
    }

    /**
   * Called when the eport button is clicked. This method starts the data export
   * process.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocatin of this method.
   */
    private void okButton_actionPerformed(ActionEvent e) {
        Thread dataLoadThread = new Thread(new Runnable() {

            public void run() {
                synchronized (dbExport) {
                    dbExport.setCanceled(false);
                    threadActive = true;
                    enableOKButton();
                    try {
                        setProgressIndeterminate(true);
                        DBFile[] listFiles = new DBFile[listModel.getSize()];
                        for (int i = 0; i < listFiles.length; i++) listFiles[i] = (DBFile) listModel.getElementAt(i);
                        if (dbExport.isCanceled()) return;
                        dbExport.loadSignals(listFiles);
                        if (dbExport.isCanceled()) return;
                        setProgressIndeterminate(true);
                        setProgressValue(0);
                        int progress = 0;
                        setProgressMaximum(listFiles.length);
                        HashMap fileContents = new HashMap();
                        final ArrayList emptyFiles = new ArrayList();
                        setProgressIndeterminate(false);
                        for (int i = 0; i < listFiles.length; i++) {
                            if (listFiles[i].getSignalCount() <= 0) emptyFiles.add(listFiles[i].getFile().getAbsolutePath()); else {
                                String currentFileContents = generateDBFile(listFiles[i].getSignals());
                                if (!dbExport.isCanceled()) fileContents.put(listFiles[i].getFile().getAbsolutePath(), currentFileContents);
                            }
                            if (dbExport.isCanceled()) return;
                            setProgressValue(++progress);
                        }
                        if (fileReviewDialog == null) {
                            fileReviewDialog = new MPSFileEditDialog(getMainWindow(), "Export Files", true);
                            fileReviewDialog.setPrintIcon(getPrintIcon());
                            fileReviewDialog.setApplicationProperties(getApplicationProperties());
                            fileReviewDialog.center();
                        }
                        fileReviewDialog.setFiles(fileContents);
                        if (dbExport.isCanceled()) return;
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                int emptyFileCount = emptyFiles.size();
                                if (emptyFileCount > 0) {
                                    StringBuffer message = new StringBuffer("<HTML>The following files do not have any records in them and will not be created:<UL>");
                                    for (int i = 0; i < emptyFileCount; i++) {
                                        message.append("<LI>");
                                        message.append(emptyFiles.get(i));
                                        message.append("</LI>");
                                        if (dbExport.isCanceled()) return;
                                    }
                                    message.append("</UL></HTML>");
                                    showMessage(message.toString(), "Empty Files", JOptionPane.WARNING_MESSAGE);
                                }
                                fileReviewDialog.setVisible(true);
                            }
                        });
                        if (fileReviewDialog.getResult() == MPSFileEditDialog.OK && !dbExport.isCanceled()) {
                            fileContents = fileReviewDialog.getFiles();
                            String[] filesExported = saveFiles(fileContents);
                            StringBuffer message = new StringBuffer("<HTML>Successfully exported the data to the following files:<UL>");
                            for (int i = 0; i < filesExported.length; i++) {
                                message.append("<LI>");
                                message.append(filesExported[i]);
                                message.append("</LI>");
                            }
                            message.append("</UL></HTML>");
                            showMessage(message.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (java.sql.SQLException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } catch (java.io.IOException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                    } catch (java.lang.reflect.InvocationTargetException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (java.lang.InterruptedException ex) {
                        ex.printStackTrace();
                        showMessage(ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        threadActive = false;
                        setProgressValue(0);
                        setProgressIndeterminate(false);
                        setMessage(" ");
                        enableOKButton();
                    }
                }
            }
        });
        dataLoadThread.start();
    }

    /**
   * Saves the files being exported. 
   *  
   * @param files The file names and contents to save.
   * @return The names and locations of the files exported.
   * @throws java.io.IOException Thrown on IO error.
   */
    private String[] saveFiles(final HashMap files) throws java.io.IOException {
        ArrayList filesExported = new ArrayList();
        try {
            setMessage("Writing Files");
            setProgressMaximum(files.size());
            int progress = 0;
            setProgressValue(progress);
            Iterator fileNames = files.keySet().iterator();
            int result = 0;
            while (fileNames.hasNext()) {
                String currentFileName = fileNames.next().toString();
                File currentFile = new File(currentFileName);
                if (currentFile.exists()) {
                    if (result != 1 && result != 3) {
                        String[] options = { "Yes", "Yes To All", "No", "No To All", "Cancel" };
                        StringBuffer message = new StringBuffer(currentFile.getAbsolutePath());
                        message.append(" already exists. Do you want to overwrite it?");
                        result = JOptionPane.showOptionDialog(null, message.toString(), "File Exists", 0, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    }
                    if (result == 2 || result == 3) continue;
                    if (result == 4) break;
                }
                BufferedWriter oStream = new BufferedWriter(new FileWriter(currentFile));
                try {
                    oStream.write(files.get(currentFileName).toString());
                    oStream.flush();
                } finally {
                    oStream.close();
                }
                filesExported.add(currentFileName);
                setProgressValue(++progress);
            }
        } finally {
            setProgressValue(0);
            setMessage(" ");
        }
        return (String[]) filesExported.toArray(new String[filesExported.size()]);
    }

    /**
   * Called when the cancel button is clicked. This method cancels the current 
   * thread if one has been started, otherwise it closes the dialog.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocatin of this method.
   */
    private void cancelButton_actionPerformed(ActionEvent e) {
        if (threadActive) dbExport.setCanceled(true); else {
            setVisible(false);
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        }
    }

    /**
   * Enables or disables the OK button. If there are tiems in the list and text 
   * in the file name text box. This method uses 
   * <CODE>SwingUtilities.invokeLater</CODE> to be thread safe.
   */
    public void enableOKButton() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (threadActive) {
                    cancelButton.setText("Cancel");
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                } else {
                    cancelButton.setText("Close");
                    setCursor(Cursor.getDefaultCursor());
                }
                okButton.setEnabled(!threadActive && listModel.size() > 0);
                fileButton.setEnabled(!threadActive);
                iocButton.setEnabled(!threadActive);
                clearButton.setEnabled(!threadActive);
                versionComboBox.setEnabled(!threadActive);
                versionLabel.setEnabled(!threadActive);
                locationButton.setEnabled(list.getSelectedIndices().length > 0);
            }
        });
    }

    /**
   * Uses <CODE>SwingUtilities.invokeLater</CODE> to set the indeterminate 
   * property of the progress bar.
   * 
   * @param indeterminate The new value of the indeterminate property of the progress bar.
   */
    private void setProgressIndeterminate(final boolean indeterminate) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressBar.setIndeterminate(indeterminate);
            }
        });
    }

    /**
   * Uses <CODE>SwingUtilities.invokeLater</CODE> to safely set the value of the 
   * progress bar from a <CODE>Thread</CODE>.
   * 
   * @param progressValue The value to pass to the <CODE>setValue</CODE> method of the progress bar.
   */
    private void setProgressValue(final int progressValue) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressBar.setValue(progressValue);
            }
        });
    }

    /**
   * Uses <CODE>SwingUtilities.invokeLater</CODE> to safely set the maximum 
   * value of the progress bar from a <CODE>Thread</CODE>.
   * 
   * @param progressMaximum The value to pass to the <CODE>setMaximum</CODE> method of the progress bar.
   */
    private void setProgressMaximum(final int progressMaximum) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressBar.setMaximum(progressMaximum);
            }
        });
    }

    /**
   * Uses <CODE>SwingUtilities.invokeLater</CODE> to safely set the text of the 
   * label in the status bar from a <CODE>Thread</CODE>.
   * 
   * @param message The value to pass to the <CODE>setText</CODE> method of the label.
   */
    private void setMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressLabel.setText(message);
            }
        });
    }

    /**
   * Sets the data source property. This method loads the EPICS versions into 
   * the combo box using the given <CODE>DataSource</CODE> to connect to the 
   * database.
   * 
   * @param connectionPool The <CODE>DataSource</CODE> used to connect to the database.
   * @throws java.sql.SQLException Thrown on SQL error.
   */
    @Override
    public void setDataSource(DataSource connectionPool) throws SQLException {
        dbExport.setDataSource(connectionPool);
        Connection oracleConnection = connectionPool.getConnection();
        try {
            Statement query = oracleConnection.createStatement();
            try {
                StringBuffer sql = new StringBuffer("SELECT EPICS_VER FROM ");
                sql.append(Main.SCHEMA);
                sql.append(".EPICS_LIBOBJS ORDER BY MOD_DTE");
                ResultSet result = query.executeQuery(sql.toString());
                try {
                    versionComboBox.removeAllItems();
                    while (result.next()) versionComboBox.addItem(result.getString("EPICS_VER"));
                    int itemCount = versionComboBox.getModel().getSize();
                    if (itemCount > 0) versionComboBox.setSelectedIndex(itemCount - 1);
                } finally {
                    result.close();
                }
            } finally {
                query.close();
            }
        } finally {
            oracleConnection.close();
        }
        super.setDataSource(connectionPool);
    }

    /**
   * Clears the contents of the list.
   */
    private void clearList() {
        listModel.clear();
    }

    /**
   * Called when the change location button is clicked. This method allows the 
   * user to choose a new location for the selected files to be exported to.
   * 
   * @param e The <CODE>ActionEvent</CODE> that caused the invocation of this method.
   */
    private void locationButton_actionPerformed(ActionEvent e) {
        int[] selectedRows = list.getSelectedIndices();
        DBFile firstFile = (DBFile) listModel.getElementAt(selectedRows[(0)]);
        String defaultLocation = firstFile.getDirectoryName();
        File location = promptForDirectory(defaultLocation);
        if (location != null) {
            String path = location.getAbsolutePath();
            for (int i = 0; i < selectedRows.length; i++) {
                DBFile currentFile = (DBFile) listModel.getElementAt(selectedRows[(i)]);
                currentFile.setDirectoryName(path);
            }
        }
    }

    /**
   * Called when the contents of the list changes. This method calls the 
   * <CODE>enableOKButton</CODE> method.
   * 
   * @param e The <CODE>ListDataEvent</CODE> that caused the invocation of this method.
   */
    private void listModel_contentsChanged(ListDataEvent e) {
        enableOKButton();
    }

    /**
   * Called when items are added to the list. This method calls the 
   * <CODE>enableOKButton</CODE> method.
   * 
   * @param e The <CODE>ListDataEvent</CODE> that caused the invocation of this method.
   */
    private void listModel_intervalAdded(ListDataEvent e) {
        enableOKButton();
    }

    /**
   * Called when items are removed from the list. This method calls the 
   * <CODE>enableOKButton</CODE> method.
   * 
   * @param e The <CODE>ListDataEvent</CODE> that caused the invocation of this method.
   */
    private void listModel_intervalRemoved(ListDataEvent e) {
        enableOKButton();
    }

    /**
   * Generates the contents for a DB file.
   * 
   * @param signalData The instances of <CODE>Signal</CODE> with which to create the DB file contents.
   * @return The contents for the DB file.
   * @throws java.io.IOException Thrown on IO Error.
   */
    private String generateDBFile(Signal[] signalData) throws java.io.IOException {
        StringWriter dbWriter = new StringWriter();
        try {
            String newLine = System.getProperty("line.separator");
            for (int i = 0; i < signalData.length; i++) {
                String currentSignalType = signalData[i].getType().getRecordType().getID();
                dbWriter.write("record(");
                dbWriter.write(currentSignalType);
                dbWriter.write(", \"");
                dbWriter.write(signalData[i].getID());
                dbWriter.write("\"){");
                dbWriter.write(newLine);
                int fieldCount = signalData[i].getFieldCount();
                for (int j = 0; j < fieldCount; j++) {
                    SignalField currentField = signalData[i].getFieldAt(j);
                    String currentValue = currentField.getValue();
                    if (currentValue != null) {
                        dbWriter.write("\tfield(");
                        dbWriter.write(currentField.getType().getID());
                        dbWriter.write(", \"");
                        dbWriter.write(currentValue);
                        dbWriter.write("\")");
                        dbWriter.write(newLine);
                    }
                }
                dbWriter.write("}");
                dbWriter.write(newLine);
                if (dbExport.isCanceled()) return null;
            }
            dbWriter.flush();
        } finally {
            dbWriter.close();
        }
        return dbWriter.toString();
    }

    /**
   * Gets the image for the print button.
   * 
   * @return The <CODE>Icon</CODE> from the print button.
   */
    public Icon getPrintIcon() {
        return printIcon;
    }

    /**
   * Puts an image on the print button.
   * 
   * @param printIcon The <CODE>Icon</CODE> for the print toolbar button.
   */
    public void setPrintIcon(Icon printIcon) {
        this.printIcon = printIcon;
    }
}
