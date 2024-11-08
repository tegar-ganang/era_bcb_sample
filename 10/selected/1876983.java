package gov.sns.apps.jeri.apps.dbdimport;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import gov.sns.apps.jeri.Main;
import gov.sns.apps.jeri.data.SignalFieldType;
import gov.sns.apps.jeri.data.SignalFieldMenu;
import gov.sns.apps.jeri.data.EpicsRecordType;
import gov.sns.apps.jeri.application.JeriInternalFrame;
import gov.sns.apps.jeri.apps.dbimport.ImportResultsDialog;

/**
 * Provides an interface for importing .dbd files.
 * 
 * @author Chris Fowlkes
 */
public class DBDImportFrame extends JeriInternalFrame {

    private JPanel statusBarPanel = new JPanel();

    private BorderLayout statusBarPanelLayout = new BorderLayout();

    private JLabel progressLabel = new JLabel();

    private JProgressBar progressBar = new JProgressBar();

    private JPanel centerPanel = new JPanel();

    private BorderLayout centerPanelLayout = new BorderLayout();

    private JPanel filePanel = new JPanel();

    private BorderLayout filePanelLayout = new BorderLayout();

    private JTextField fileField = new JTextField();

    private JButton browseButton = new JButton();

    private JScrollPane scrollPane = new JScrollPane();

    private JList list = new JList();

    private BorderLayout frameLayout = new BorderLayout();

    private JPanel listPanel = new JPanel();

    private BorderLayout listPanelLayout = new BorderLayout();

    private JPanel outerButtonPanel = new JPanel();

    private JPanel innerButtonPanel = new JPanel();

    private JButton okButton = new JButton();

    private BorderLayout outerButtonPanelLayout = new BorderLayout();

    private JButton cancelButton = new JButton();

    private GridLayout innerButtonPanelLayout = new GridLayout();

    private JLabel fileLabel = new JLabel();

    /**
   * Holds the dialog used to browse for files.
   */
    private JFileChooser fileDialog;

    /**
   * Flag used to tell if the import has been canceled.
   */
    private boolean importCanceled = false;

    /**
   * Flag used to tell if the import is ongoing.
   */
    private boolean importing = false;

    private Pattern commentLine = Pattern.compile("\\A#");

    private Pattern blankLine = Pattern.compile("\\A\\s*\\Z");

    private Pattern includeLine = Pattern.compile("include\\s*\"(.+)\"");

    private Pattern menuLine = Pattern.compile("menu\\((.+)\\)");

    private Pattern driverLine = Pattern.compile("driver\\s*\\(.+\\)");

    private Pattern deviceLine = Pattern.compile("device\\s*\\((\\w+)\\s*,\\s*\\w+\\s*,\\s*\\w+\\s*,\\s*\"(.+)\"\\s*\\)");

    private Pattern choiceLine = Pattern.compile("choice\\s*\\(.+,\\s*\"(.+)\"\\s*\\)");

    private Pattern recordTypeLine = Pattern.compile("recordtype\\s*\\((.+)\\s*\\)");

    private Pattern fieldLine = Pattern.compile("field\\s*\\((.+)\\s*,\\s*(\\w+)\\s*\\)");

    private Pattern textPropertyLine = Pattern.compile("([a-z]+)\\s*\\(\"(.+)\"\\s*\\)");

    private Pattern propertyLine = Pattern.compile("([a-z]+)\\s*\\((.+)\\s*\\)");

    private Pattern closingCurlyBraceLine = Pattern.compile("\\A\\s*}\\s*\\Z");

    private final int idleState = 0;

    private final int menuState = 1;

    private final int recordState = 2;

    private final int fieldState = 3;

    private final String[] stateNames = new String[] { "idle", "menu", "record", "field" };

    private int state = idleState;

    private SignalFieldMenu currentMenu;

    private EpicsRecordType currentRecordType;

    private SignalFieldType currentFieldType;

    private int currentPromptOrder;

    private HashMap fileMenus = new HashMap();

    private ArrayList fileFieldTypes = new ArrayList();

    private HashMap fileRecordTypes = new HashMap();

    private HashMap databaseMenus = new HashMap();

    private ArrayList databaseFieldTypes = new ArrayList();

    private HashMap databaseRecordTypes = new HashMap();

    private ArrayList invalidFieldTypes = new ArrayList();

    private ArrayList deviceLineFieldTypes = new ArrayList();

    private DBDImportResultsDialog resultDialog;

    private DefaultListModel messagesListModel = new DefaultListModel();

    /**
   * Creates a new <CODE>DBDImportFrame</CODE>.
   */
    public DBDImportFrame() {
        try {
            jbInit();
            fileField.getDocument().addDocumentListener(new DocumentListener() {

                public void changedUpdate(DocumentEvent e) {
                    enableOKButton();
                }

                public void insertUpdate(DocumentEvent e) {
                    enableOKButton();
                }

                public void removeUpdate(DocumentEvent e) {
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
   * @throws java.lang.Exception Thrown on initialization error.
   */
    private void jbInit() throws Exception {
        this.setSize(new Dimension(400, 300));
        this.setTitle("Import .dbd File");
        this.getContentPane().setLayout(frameLayout);
        statusBarPanel.setLayout(statusBarPanelLayout);
        progressLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        progressLabel.setText(" ");
        progressBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        centerPanel.setLayout(centerPanelLayout);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        centerPanelLayout.setVgap(5);
        filePanel.setLayout(filePanelLayout);
        filePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        filePanelLayout.setHgap(5);
        browseButton.setText("Browse...");
        browseButton.setMnemonic('B');
        browseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                browseButton_actionPerformed(e);
            }
        });
        list.setModel(messagesListModel);
        frameLayout.setVgap(5);
        listPanel.setLayout(listPanelLayout);
        listPanel.setBorder(BorderFactory.createTitledBorder("Messages"));
        outerButtonPanel.setLayout(outerButtonPanelLayout);
        innerButtonPanel.setLayout(innerButtonPanelLayout);
        okButton.setText("Import...");
        okButton.setMnemonic('O');
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
        innerButtonPanelLayout.setHgap(5);
        fileLabel.setText("DBD File:");
        fileLabel.setDisplayedMnemonic('e');
        fileLabel.setLabelFor(fileField);
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        filePanel.add(fileLabel, BorderLayout.WEST);
        statusBarPanel.add(progressLabel, BorderLayout.CENTER);
        statusBarPanel.add(progressBar, BorderLayout.EAST);
        this.getContentPane().add(filePanel, BorderLayout.NORTH);
        scrollPane.getViewport().add(list, null);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(listPanel, BorderLayout.CENTER);
        innerButtonPanel.add(okButton, null);
        innerButtonPanel.add(cancelButton, null);
        outerButtonPanel.add(innerButtonPanel, BorderLayout.EAST);
        centerPanel.add(outerButtonPanel, BorderLayout.SOUTH);
        this.getContentPane().add(centerPanel, BorderLayout.CENTER);
        this.getContentPane().add(statusBarPanel, BorderLayout.SOUTH);
    }

    private void cancelButton_actionPerformed(ActionEvent e) {
        if (importing) importCanceled = true; else {
            setVisible(false);
            fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        }
    }

    private void okButton_actionPerformed(ActionEvent e) {
        progressBar.setIndeterminate(true);
        okButton.setEnabled(false);
        cancelButton.setText("Cancel");
        browseButton.setEnabled(false);
        fileField.setEnabled(false);
        messagesListModel.clear();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Thread importThread = new Thread(new Runnable() {

            public void run() {
                try {
                    importing = true;
                    parseFiles();
                    if (importCanceled) return;
                    findDataInDatabase();
                    if (importCanceled) return;
                    if (hasErrorMessages()) try {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                int response = JOptionPane.showConfirmDialog(DBDImportFrame.this, "Errors occurred while parsing the .dbd files. Do you want to continue the import?", "Error Parsing Files", JOptionPane.YES_NO_OPTION);
                                if (response == JOptionPane.NO_OPTION) importCanceled = true;
                            }
                        });
                    } catch (java.lang.reflect.InvocationTargetException ex) {
                        ex.printStackTrace();
                    } catch (java.lang.InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    if (importCanceled) return;
                    if (!displayResults() || importCanceled) return;
                    postData();
                } catch (java.sql.SQLException ex) {
                    ex.printStackTrace();
                    showErrorMessage(ex.getMessage(), "SQL Error");
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            okButton.setEnabled(true);
                            cancelButton.setText("Close");
                            browseButton.setEnabled(true);
                            fileField.setEnabled(true);
                            progressBar.setValue(0);
                            progressLabel.setText(" ");
                            progressBar.setIndeterminate(false);
                            setCursor(Cursor.getDefaultCursor());
                        }
                    });
                    importing = false;
                }
            }
        });
        importCanceled = false;
        importThread.start();
    }

    /**
   * Gets the instances of <CODE>File</CODE> that represent the files named in 
   * the file text box.
   * 
   * @return An <CODE>ArrayList</CODE> containing the instancess of <CODE>File</CODE> for the files listed.
   */
    private ArrayList getFiles() {
        StringTokenizer fileNames = new StringTokenizer(fileField.getText(), ";");
        ArrayList files = new ArrayList();
        while (fileNames.hasMoreTokens()) {
            String currentFileName = fileNames.nextToken().trim();
            if (!currentFileName.equals("")) files.add(new File(currentFileName));
        }
        return files;
    }

    private void browseButton_actionPerformed(ActionEvent e) {
        Properties settings = getApplicationProperties();
        if (fileDialog == null) {
            fileDialog = new JFileChooser();
            fileDialog.setMultiSelectionEnabled(true);
            fileDialog.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public boolean accept(File chosenFile) {
                    if (chosenFile.isDirectory() || chosenFile.getName().toLowerCase().endsWith(".dbd")) return true; else return false;
                }

                @Override
                public String getDescription() {
                    return "Data Files (*.dbd)";
                }
            });
            String directory = settings.getProperty("DBDImportFrame.fileDirectory");
            if (directory != null) fileDialog.setCurrentDirectory(new File(directory));
        }
        if (fileDialog.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFiles(fileDialog.getSelectedFiles());
            String path = fileDialog.getCurrentDirectory().getAbsolutePath();
            settings.setProperty("DBDImportFrame.fileDirectory", path);
        }
    }

    /**
   * Adds the given instances of <CODE>File</CODE> to the file text box.
   * 
   * @param selectedFiles The instances of <CODE>File</CODE> to add to the list to parse.
   */
    private void openFiles(File[] selectedFiles) {
        StringBuffer fileNames = new StringBuffer(fileField.getText());
        for (int i = 0; i < selectedFiles.length; i++) {
            String currentFileName = selectedFiles[i].getAbsolutePath().trim();
            if (fileNames.indexOf(currentFileName) < 0) {
                if (fileNames.length() > 0) fileNames.append("; ");
                fileNames.append(currentFileName);
            }
        }
        fileField.setText(fileNames.toString());
    }

    /**
   * Parses the data files listed in the file text field.
   */
    private void parseFiles() {
        setMessage("Parsing Files");
        fileMenus.clear();
        fileFieldTypes.clear();
        fileRecordTypes.clear();
        ArrayList files = getFiles();
        int totalFileSize = 0;
        for (int i = files.size() - 1; i >= 0; i--) {
            File currentFile = (File) files.get(i);
            if (currentFile.exists()) totalFileSize += (int) currentFile.length(); else {
                StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Unable to open file '");
                errorMessage.append(currentFile.getName());
                errorMessage.append("'.</FONT></HTML>");
                addMessage(errorMessage.toString());
                files.remove(i);
            }
            if (importCanceled) return;
        }
        setProgressMaximum(totalFileSize);
        int progress = 0;
        setProgressValue(progress);
        setProgressIndeterminate(false);
        for (int i = 0; i < files.size(); i++) {
            File currentFile = (File) files.get(i);
            try {
                BufferedReader iStream = new BufferedReader(new FileReader(currentFile));
                String currentLine = iStream.readLine();
                int lineNumber = 1;
                while (currentLine != null) {
                    try {
                        if (commentLine.matcher(currentLine).find()) continue;
                        if (blankLine.matcher(currentLine).find()) continue;
                        Matcher includeMatcher = includeLine.matcher(currentLine);
                        if (includeMatcher.find()) {
                            String newFileName = includeMatcher.group(1);
                            File newFile = new File(currentFile.getParent(), newFileName);
                            if (!newFile.exists()) newFile = new File(newFileName);
                            if (newFile.exists()) {
                                if (!files.contains(newFile)) {
                                    totalFileSize += (int) newFile.length();
                                    setProgressMaximum(totalFileSize);
                                }
                            } else {
                                StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Unable to open file '");
                                errorMessage.append(newFileName);
                                errorMessage.append("'.</FONT></HTML>");
                                addMessage(errorMessage.toString());
                            }
                            continue;
                        }
                        Matcher menuMatcher = menuLine.matcher(currentLine);
                        if (state == idleState && menuMatcher.find()) {
                            String menuID = menuMatcher.group(1);
                            currentMenu = findMenu(menuID);
                            state = menuState;
                            continue;
                        }
                        if (driverLine.matcher(currentLine).find()) {
                            if (state != idleState) {
                                StringBuffer errorMessage = new StringBuffer("Found driver while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            continue;
                        }
                        Matcher deviceMatcher = deviceLine.matcher(currentLine);
                        if (deviceMatcher.find()) {
                            if (state != idleState) {
                                StringBuffer errorMessage = new StringBuffer("Found device while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            EpicsRecordType recordType = new EpicsRecordType(deviceMatcher.group(1));
                            SignalFieldType newType = findFieldType("DTYP", recordType, true);
                            SignalFieldMenu newMenu = findMenu(recordType.getID() + "DTYP");
                            newType.setMenu(newMenu);
                            String newMenuItem = deviceMatcher.group(2);
                            if (!newMenu.containsItem(newMenuItem)) newMenu.addMenuItem(newMenuItem);
                            continue;
                        }
                        Matcher choiceMatcher = choiceLine.matcher(currentLine);
                        if (choiceMatcher.find()) {
                            if (state != menuState) {
                                StringBuffer errorMessage = new StringBuffer("Found choice while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            String newItem = choiceMatcher.group(1);
                            if (!currentMenu.containsItem(newItem)) currentMenu.addMenuItem(newItem);
                            continue;
                        }
                        Matcher recordTypeMatcher = recordTypeLine.matcher(currentLine);
                        if (recordTypeMatcher.find()) {
                            if (state != idleState) {
                                StringBuffer errorMessage = new StringBuffer("Found record definition while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            currentRecordType = findEpicsRecordType(recordTypeMatcher.group(1));
                            currentRecordType.setCode("E");
                            currentRecordType.setDescription("Record type from DBD file");
                            currentPromptOrder = 0;
                            state = recordState;
                            continue;
                        }
                        Matcher fieldMatcher = fieldLine.matcher(currentLine);
                        if (fieldMatcher.find()) {
                            if (state != recordState) {
                                StringBuffer errorMessage = new StringBuffer("Found field while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            currentFieldType = findFieldType(fieldMatcher.group(1), currentRecordType, false);
                            currentFieldType.setEpicsFieldTypeID(fieldMatcher.group(2));
                            currentFieldType.setPromptOrder(++currentPromptOrder);
                            state = fieldState;
                            continue;
                        }
                        Matcher textPropertyMatcher = textPropertyLine.matcher(currentLine);
                        Matcher propertyMatcher = propertyLine.matcher(currentLine);
                        Matcher validMatcher = null;
                        if (textPropertyMatcher.find()) validMatcher = textPropertyMatcher; else if (propertyMatcher.find()) validMatcher = propertyMatcher;
                        if (validMatcher != null) {
                            if (state != fieldState) {
                                StringBuffer errorMessage = new StringBuffer("Found property '");
                                errorMessage.append(currentLine);
                                errorMessage.append("' while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            String propertyName = validMatcher.group(1);
                            if (propertyName.equals("prompt")) currentFieldType.setDescription(validMatcher.group(2)); else if (propertyName.equals("initial")) currentFieldType.setDescription(validMatcher.group(2)); else if (propertyName.equals("promptgroup")) currentFieldType.setPromptGroup(validMatcher.group(2)); else if (propertyName.equals("menu")) {
                                SignalFieldMenu newMenu = new SignalFieldMenu(validMatcher.group(2));
                                currentFieldType.setMenu(newMenu);
                            }
                            continue;
                        }
                        if (closingCurlyBraceLine.matcher(currentLine).find()) {
                            if (state == menuState || state == recordState) state = idleState; else if (state == fieldState) state = recordState; else {
                                StringBuffer errorMessage = new StringBuffer("Found end of item ('}') while in ");
                                errorMessage.append(stateNames[state]);
                                errorMessage.append(" state.");
                                throw new java.lang.IllegalStateException(errorMessage.toString());
                            }
                            continue;
                        }
                        StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Error parsing file ");
                        errorMessage.append(currentFile.getAbsoluteFile());
                        errorMessage.append(": Cannot handle line ");
                        errorMessage.append(lineNumber);
                        errorMessage.append(" '");
                        errorMessage.append(currentLine);
                        errorMessage.append("'</FONT></HTML>");
                        addMessage(errorMessage.toString());
                    } catch (java.lang.IllegalStateException ex) {
                        StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Error parsing file ");
                        errorMessage.append(currentFile.getAbsoluteFile());
                        errorMessage.append(" at line ");
                        errorMessage.append(lineNumber);
                        errorMessage.append(" '");
                        errorMessage.append(currentLine);
                        errorMessage.append("' : ");
                        errorMessage.append(ex.getMessage());
                        errorMessage.append("</FONT></HTML>");
                        addMessage(errorMessage.toString());
                    } finally {
                        progress += currentLine.length();
                        setProgressValue(progress);
                        currentLine = iStream.readLine();
                        lineNumber++;
                    }
                    if (importCanceled) break;
                }
            } catch (java.io.FileNotFoundException ex) {
                StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Unable to open file '");
                errorMessage.append(currentFile.getAbsoluteFile());
                errorMessage.append("'.</FONT></HTML>");
                addMessage(errorMessage.toString());
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
                StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>IO Error: ");
                errorMessage.append(ex.getMessage());
                errorMessage.append("</FONT></HTML>");
                addMessage(errorMessage.toString());
            }
            if (importCanceled) break;
        }
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

    private SignalFieldMenu findMenu(String menuID) {
        SignalFieldMenu menu = (SignalFieldMenu) fileMenus.get(menuID);
        if (menu == null) {
            menu = new SignalFieldMenu(menuID);
            fileMenus.put(menuID, menu);
        }
        return menu;
    }

    private int findFieldTypeIndex(ArrayList fieldTypes, String id, EpicsRecordType recordType) {
        int fieldTypeCount = fieldTypes.size();
        String recordTypeID = recordType.getID();
        int index = -1;
        for (int i = 0; i < fieldTypeCount; i++) {
            SignalFieldType currentFieldType = (SignalFieldType) fieldTypes.get(i);
            if (currentFieldType.getID().equals(id) && currentFieldType.getRecordType().getID().equals(recordTypeID)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private SignalFieldType findFieldType(String id, EpicsRecordType recordType, boolean deviceLine) {
        int index = findFieldTypeIndex(fileFieldTypes, id, recordType);
        SignalFieldType fieldType;
        if (index >= 0) {
            fieldType = (SignalFieldType) fileFieldTypes.get(index);
            if (!deviceLine) {
                int deviceFieldTypeIndex = findFieldTypeIndex(deviceLineFieldTypes, id, recordType);
                if (deviceFieldTypeIndex >= 0) deviceLineFieldTypes.remove(deviceFieldTypeIndex);
            }
        } else {
            fieldType = new SignalFieldType(id, recordType);
            fileFieldTypes.add(fieldType);
            if (deviceLine) deviceLineFieldTypes.add(fieldType);
        }
        return fieldType;
    }

    private EpicsRecordType findEpicsRecordType(String id) {
        EpicsRecordType recordType = (EpicsRecordType) fileRecordTypes.get(id);
        if (recordType == null) {
            recordType = new EpicsRecordType(id);
            fileRecordTypes.put(id, recordType);
        }
        return recordType;
    }

    private void findDataInDatabase() throws java.sql.SQLException {
        setMessage("Looking in RDB for existing records.");
        databaseMenus.clear();
        databaseFieldTypes.clear();
        databaseRecordTypes.clear();
        invalidFieldTypes.clear();
        setProgressMaximum(fileMenus.size() + fileFieldTypes.size() + fileRecordTypes.size());
        int progress = 0;
        setProgressValue(progress);
        Connection oracleConnection = getDataSource().getConnection();
        try {
            StringBuffer sql = new StringBuffer("SELECT * FROM ");
            sql.append(Main.SCHEMA);
            sql.append(".SGNL_FLD_MENU WHERE SGNL_FLD_MENU_ID = ?");
            PreparedStatement menuQuery = oracleConnection.prepareStatement(sql.toString());
            try {
                Iterator menuIDIterator = fileMenus.keySet().iterator();
                while (menuIDIterator.hasNext()) {
                    String menuID = menuIDIterator.next().toString();
                    menuQuery.setString(1, menuID);
                    ResultSet result = menuQuery.executeQuery();
                    try {
                        if (result.next()) {
                            SignalFieldMenu newMenu = new SignalFieldMenu(menuID);
                            do {
                                newMenu.addMenuItem(result.getString("FLD_MENU_VAL"));
                                if (importCanceled) return;
                            } while (result.next());
                            databaseMenus.put(menuID, newMenu);
                            SignalFieldMenu currentFileMenu = (SignalFieldMenu) fileMenus.get(menuID);
                            int fileMenuItemCount = currentFileMenu.getSize();
                            for (int i = 0; i < fileMenuItemCount; i++) {
                                String menuItem = currentFileMenu.getMenuItemAt(i);
                                if (!newMenu.containsItem(menuItem)) currentFileMenu.setMenuItemInDatabase(i, false);
                                if (importCanceled) return;
                            }
                        } else ((SignalFieldMenu) fileMenus.get(menuID)).setInDatabase(false);
                    } finally {
                        result.close();
                    }
                    setProgressValue(++progress);
                    if (importCanceled) return;
                }
            } finally {
                menuQuery.close();
            }
            sql = new StringBuffer("SELECT * FROM ");
            sql.append(Main.SCHEMA);
            sql.append(".SGNL_FLD_DEF WHERE REC_TYPE_ID = ? AND FLD_ID = ?");
            PreparedStatement fieldTypeQuery = oracleConnection.prepareStatement(sql.toString());
            try {
                sql = new StringBuffer("SELECT REC_TYPE_ID FROM ");
                sql.append(Main.SCHEMA);
                sql.append(".SGNL_REC_TYPE WHERE REC_TYPE_ID = ?");
                PreparedStatement recordTypeQuery = oracleConnection.prepareStatement(sql.toString());
                try {
                    Iterator fieldTypeIterator = fileFieldTypes.iterator();
                    while (fieldTypeIterator.hasNext()) {
                        SignalFieldType fieldType = (SignalFieldType) fieldTypeIterator.next();
                        EpicsRecordType recordType = fieldType.getRecordType();
                        String recordTypeID = recordType.getID();
                        String id = fieldType.getID();
                        fieldTypeQuery.setString(1, recordTypeID);
                        fieldTypeQuery.setString(2, id);
                        ResultSet result = fieldTypeQuery.executeQuery();
                        if (result.next()) {
                            String description = result.getString("FLD_DESC");
                            SignalFieldType newFieldType = new SignalFieldType(id, recordType, description);
                            newFieldType.setMenu(new SignalFieldMenu(result.getString("SGNL_FLD_MENU_ID")));
                            newFieldType.setEpicsFieldTypeID(result.getString("FLD_TYPE_ID"));
                            newFieldType.setPromptOrder(result.getInt("PRMPT_ORD"));
                            newFieldType.setInitial(result.getString("FLD_INIT"));
                            newFieldType.setPromptGroup(result.getString("FLD_PRMT_GRP"));
                            databaseFieldTypes.add(newFieldType);
                        } else fieldType.setInDatabase(false);
                        if (fileRecordTypes.get(recordTypeID) == null) {
                            recordTypeQuery.setString(1, recordTypeID);
                            ResultSet recordTypeResult = recordTypeQuery.executeQuery();
                            try {
                                if (!recordTypeResult.next()) {
                                    StringBuffer errorMessage = new StringBuffer("<HTML><FONT COLOR=RED>Record type '");
                                    errorMessage.append(recordTypeID);
                                    errorMessage.append("' is unknown.</FONT></HTML>");
                                    addMessage(errorMessage.toString());
                                    invalidFieldTypes.add(recordTypeID);
                                }
                            } finally {
                                recordTypeResult.close();
                            }
                        }
                        setProgressValue(++progress);
                        if (importCanceled) return;
                    }
                } finally {
                    recordTypeQuery.close();
                }
            } finally {
                fieldTypeQuery.close();
            }
            sql = new StringBuffer("SELECT * FROM ");
            sql.append(Main.SCHEMA);
            sql.append(".SGNL_REC_TYPE WHERE REC_TYPE_ID = ?");
            PreparedStatement recordTypeQuery = oracleConnection.prepareStatement(sql.toString());
            try {
                Iterator recordTypeIDIterator = fileRecordTypes.keySet().iterator();
                while (recordTypeIDIterator.hasNext()) {
                    String recordTypeID = recordTypeIDIterator.next().toString();
                    recordTypeQuery.setString(1, recordTypeID);
                    ResultSet result = recordTypeQuery.executeQuery();
                    try {
                        if (result.next()) {
                            String code = result.getString("REC_TYPE_CODE");
                            String description = result.getString("TYPE_DESC");
                            EpicsRecordType newRecordType = new EpicsRecordType(recordTypeID, code, description);
                            databaseRecordTypes.put(recordTypeID, newRecordType);
                        } else ((EpicsRecordType) fileRecordTypes.get(recordTypeID)).setInDatabase(false);
                    } finally {
                        result.close();
                    }
                    setProgressValue(++progress);
                    if (importCanceled) return;
                }
            } finally {
                recordTypeQuery.close();
            }
        } finally {
            oracleConnection.close();
            setProgressValue(0);
        }
    }

    private void showErrorMessage(final String message, final String title) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(DBDImportFrame.this, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
   * Compiles and displays the results of the file import.
   */
    private boolean displayResults() {
        setMessage("Preparing data for display");
        int fileRecordCount = fileMenus.size() + fileFieldTypes.size() + fileRecordTypes.size();
        int databaseRecordCount = databaseMenus.size() + databaseFieldTypes.size() + databaseRecordTypes.size();
        ArrayList fileRecords = new ArrayList(fileRecordCount);
        fileRecords.addAll(fileMenus.values());
        fileRecords.addAll(fileFieldTypes);
        fileRecords.addAll(fileRecordTypes.values());
        ArrayList databaseRecords = new ArrayList(databaseRecordCount);
        databaseRecords.addAll(databaseMenus.values());
        databaseRecords.addAll(databaseFieldTypes);
        databaseRecords.addAll(databaseRecordTypes.values());
        if (resultDialog == null) {
            resultDialog = new DBDImportResultsDialog(getMainWindow(), "Import Results", true);
            resultDialog.setApplicationProperties(getApplicationProperties());
            resultDialog.center();
        }
        resultDialog.setFileData(fileRecords, invalidFieldTypes);
        resultDialog.setDatabaseData(databaseRecords);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    resultDialog.setVisible(true);
                }
            });
        } catch (java.lang.reflect.InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (java.lang.InterruptedException ex) {
            ex.printStackTrace();
        }
        return resultDialog.getResult() == ImportResultsDialog.OK;
    }

    private void postData() throws java.sql.SQLException {
        setProgressMaximum(fileMenus.size() + fileRecordTypes.size() + fileFieldTypes.size());
        int progress = 0;
        setProgressValue(0);
        setMessage("Saving to RDB");
        Connection oracleConnection = getDataSource().getConnection();
        try {
            oracleConnection.setAutoCommit(false);
            boolean deleteMenuItems = resultDialog.isRemoveExistingMenus();
            PreparedStatement deleteMenuQuery = null;
            try {
                PreparedStatement insertMenuQuery = null;
                try {
                    Iterator menuIterator = fileMenus.values().iterator();
                    while (menuIterator.hasNext()) {
                        SignalFieldMenu currentMenu = (SignalFieldMenu) menuIterator.next();
                        String currentMenuID = currentMenu.getID();
                        if (deleteMenuItems) {
                            if (deleteMenuQuery == null) {
                                StringBuffer sql = new StringBuffer("DELETE FROM ");
                                sql.append(Main.SCHEMA);
                                sql.append(".SGNL_FLD_MENU WHERE SGNL_FLD_MENU_ID = ?");
                                deleteMenuQuery = oracleConnection.prepareStatement(sql.toString());
                            } else deleteMenuQuery.clearParameters();
                            deleteMenuQuery.setString(1, currentMenuID);
                            deleteMenuQuery.execute();
                        }
                        int menuItemCount = currentMenu.getSize();
                        for (int i = 0; i < menuItemCount; i++) {
                            if (deleteMenuItems || !currentMenu.isMenuItemInDatabase(i)) {
                                if (insertMenuQuery == null) {
                                    StringBuffer sql = new StringBuffer("INSERT INTO ");
                                    sql.append(Main.SCHEMA);
                                    sql.append(".SGNL_FLD_MENU (SGNL_FLD_MENU_ID, FLD_MENU_VAL) VALUES (?, ?)");
                                    insertMenuQuery = oracleConnection.prepareStatement(sql.toString());
                                } else insertMenuQuery.clearParameters();
                                insertMenuQuery.setString(1, currentMenuID);
                                insertMenuQuery.setString(2, currentMenu.getMenuItemAt(i));
                                insertMenuQuery.execute();
                            }
                            if (importCanceled) return;
                        }
                        if (importCanceled) return;
                        setProgressValue(++progress);
                    }
                } finally {
                    if (insertMenuQuery != null) insertMenuQuery.close();
                }
            } finally {
                if (deleteMenuQuery != null) deleteMenuQuery.close();
            }
            PreparedStatement recordTypeInsertQuery = null;
            try {
                PreparedStatement recordTypeUpdateQuery = null;
                try {
                    Iterator recordTypeIterator = fileRecordTypes.values().iterator();
                    while (recordTypeIterator.hasNext()) {
                        EpicsRecordType currentRecordType = (EpicsRecordType) recordTypeIterator.next();
                        if (currentRecordType.isInDatabase()) {
                            if (recordTypeUpdateQuery == null) {
                                StringBuffer sql = new StringBuffer("UPDATE ");
                                sql.append(Main.SCHEMA);
                                sql.append(".SGNL_REC_TYPE SET REC_TYPE_CODE = ?, TYPE_DESC = ? WHERE REC_TYPE_ID = ?");
                                recordTypeUpdateQuery = oracleConnection.prepareStatement(sql.toString());
                            } else recordTypeUpdateQuery.clearParameters();
                            recordTypeUpdateQuery.setString(1, currentRecordType.getCode());
                            recordTypeUpdateQuery.setString(2, currentRecordType.getDescription());
                            recordTypeUpdateQuery.setString(3, currentRecordType.getID());
                            recordTypeUpdateQuery.execute();
                        } else {
                            if (recordTypeInsertQuery == null) {
                                StringBuffer sql = new StringBuffer("INSERT INTO ");
                                sql.append(Main.SCHEMA);
                                sql.append(".SGNL_REC_TYPE (REC_TYPE_ID, REC_TYPE_CODE, TYPE_DESC) VALUES (?, ?, ?)");
                                recordTypeInsertQuery = oracleConnection.prepareStatement(sql.toString());
                            } else recordTypeInsertQuery.clearParameters();
                            recordTypeInsertQuery.setString(1, currentRecordType.getID());
                            recordTypeInsertQuery.setString(2, currentRecordType.getCode());
                            recordTypeInsertQuery.setString(3, currentRecordType.getDescription());
                            recordTypeInsertQuery.execute();
                        }
                        if (importCanceled) return;
                        setProgressValue(++progress);
                    }
                } finally {
                    if (recordTypeUpdateQuery != null) recordTypeUpdateQuery.close();
                }
            } finally {
                if (recordTypeInsertQuery != null) recordTypeInsertQuery.close();
            }
            PreparedStatement fieldTypeInsertQuery = null;
            try {
                PreparedStatement fieldTypeUpdateQuery = null;
                try {
                    PreparedStatement deviceFieldTypeUpdateQuery = null;
                    try {
                        Iterator fieldTypeIterator = fileFieldTypes.iterator();
                        while (fieldTypeIterator.hasNext()) {
                            SignalFieldType currentFieldType = (SignalFieldType) fieldTypeIterator.next();
                            if (!invalidFieldTypes.contains(currentFieldType.getRecordType().getID())) if (currentFieldType.isInDatabase()) {
                                String currentFieldID = currentFieldType.getID();
                                EpicsRecordType currentRecordType = currentFieldType.getRecordType();
                                if (findFieldTypeIndex(deviceLineFieldTypes, currentFieldID, currentRecordType) >= 0) {
                                    if (deviceFieldTypeUpdateQuery == null) {
                                        StringBuffer sql = new StringBuffer("UPDATE ");
                                        sql.append(Main.SCHEMA);
                                        sql.append(".SGNL_FLD_DEF SET SGNL_FLD_MENU_ID = ? WHERE REC_TYPE_ID = ? AND FLD_ID = ?");
                                        fieldTypeUpdateQuery = oracleConnection.prepareStatement(sql.toString());
                                    } else deviceFieldTypeUpdateQuery.clearParameters();
                                    fieldTypeUpdateQuery.setString(1, currentFieldType.getMenu().getID());
                                    fieldTypeUpdateQuery.setString(2, currentRecordType.getID());
                                    fieldTypeUpdateQuery.setString(3, currentFieldID);
                                    fieldTypeUpdateQuery.execute();
                                } else {
                                    if (fieldTypeUpdateQuery == null) {
                                        StringBuffer sql = new StringBuffer("UPDATE ");
                                        sql.append(Main.SCHEMA);
                                        sql.append(".SGNL_FLD_DEF SET FLD_TYPE_ID = ?, PRMPT_ORD = ?, FLD_DESC = ?, FLD_INIT = ?, FLD_PRMT_GRP = ?, SGNL_FLD_MENU_ID = ? WHERE REC_TYPE_ID = ? AND FLD_ID = ?");
                                        fieldTypeUpdateQuery = oracleConnection.prepareStatement(sql.toString());
                                    } else fieldTypeUpdateQuery.clearParameters();
                                    fieldTypeUpdateQuery.setString(1, currentFieldType.getEpicsFieldTypeID());
                                    fieldTypeUpdateQuery.setInt(2, currentFieldType.getPromptOrder());
                                    fieldTypeUpdateQuery.setString(3, currentFieldType.getDescription());
                                    fieldTypeUpdateQuery.setString(4, currentFieldType.getInitial());
                                    fieldTypeUpdateQuery.setString(5, currentFieldType.getPromptGroup());
                                    SignalFieldMenu currentMenu = currentFieldType.getMenu();
                                    if (currentMenu == null) fieldTypeUpdateQuery.setString(6, null); else fieldTypeUpdateQuery.setString(6, currentMenu.getID());
                                    fieldTypeUpdateQuery.setString(7, currentRecordType.getID());
                                    fieldTypeUpdateQuery.setString(8, currentFieldID);
                                    fieldTypeUpdateQuery.executeUpdate();
                                }
                            } else {
                                if (fieldTypeInsertQuery == null) {
                                    StringBuffer sql = new StringBuffer("INSERT INTO ");
                                    sql.append(Main.SCHEMA);
                                    sql.append(".SGNL_FLD_DEF (REC_TYPE_ID, FLD_ID, FLD_TYPE_ID, PRMPT_ORD, FLD_DESC, FLD_INIT, FLD_PRMT_GRP, SGNL_FLD_MENU_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                                    fieldTypeInsertQuery = oracleConnection.prepareStatement(sql.toString());
                                } else fieldTypeInsertQuery.clearParameters();
                                fieldTypeInsertQuery.setString(1, currentFieldType.getRecordType().getID());
                                fieldTypeInsertQuery.setString(2, currentFieldType.getID());
                                fieldTypeInsertQuery.setString(3, currentFieldType.getEpicsFieldTypeID());
                                fieldTypeInsertQuery.setInt(4, currentFieldType.getPromptOrder());
                                fieldTypeInsertQuery.setString(5, currentFieldType.getDescription());
                                fieldTypeInsertQuery.setString(6, currentFieldType.getInitial());
                                fieldTypeInsertQuery.setString(7, currentFieldType.getPromptGroup());
                                SignalFieldMenu currentMenu = currentFieldType.getMenu();
                                if (currentMenu == null) fieldTypeInsertQuery.setString(8, null); else fieldTypeInsertQuery.setString(8, currentMenu.getID());
                                fieldTypeInsertQuery.execute();
                            }
                            if (importCanceled) return;
                            setProgressValue(++progress);
                        }
                    } finally {
                        if (deviceFieldTypeUpdateQuery != null) deviceFieldTypeUpdateQuery.close();
                    }
                } finally {
                    if (fieldTypeUpdateQuery != null) fieldTypeUpdateQuery.close();
                }
            } finally {
                if (fieldTypeInsertQuery != null) fieldTypeInsertQuery.close();
            }
            if (importCanceled) oracleConnection.rollback(); else oracleConnection.commit();
        } catch (java.lang.RuntimeException ex) {
            oracleConnection.rollback();
            throw ex;
        } catch (java.sql.SQLException ex) {
            oracleConnection.rollback();
            throw ex;
        } finally {
            oracleConnection.close();
            setProgressValue(0);
        }
    }

    /**
   * Enables or disables the ok button. If the file field contains anything, the
   * ok button is enabled. If it is empty the ok button is disabled.
   */
    private void enableOKButton() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                okButton.setEnabled(fileField.getText().length() > 0);
            }
        });
    }

    /**
   * Uses <CODE>SwingUtilities.invokeLater</CODE> to add a message to the 
   * message list. This method provides a thread safe way to update the 
   * messages.
   * 
   * @param message The message to add to the messages tab.
   */
    private void addMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (!messagesListModel.contains(message)) messagesListModel.addElement(message);
            }
        });
    }

    /**
   * Tries to determine if errors are shown in the messages list. If any of the 
   * messages displayed contain "COLOR=RED", they are considered to be error 
   * messages.
   * 
   * @return <CODE>true</CODE> if the message list contains errors, <CODE>false</CODE> if not.
   */
    private boolean hasErrorMessages() {
        int messageCount = messagesListModel.size();
        for (int i = 0; i < messageCount; i++) if (messagesListModel.get(i).toString().indexOf("COLOR=RED") >= 0) return true;
        return false;
    }
}
