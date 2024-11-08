package org.systemsbiology.chem.app;

import org.systemsbiology.chem.*;
import org.systemsbiology.chem.Model;
import org.systemsbiology.gui.*;
import org.systemsbiology.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.OptionPaneUI;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.io.*;

public class EditorPane extends JPanel {

    private static final long serialVersionUID = 1L;

    static final long TIMESTAMP_BUFFER_LAST_CHANGE_NULL = -1;

    private static final String LABEL_FILE = "file: ";

    private static final String LABEL_PARSER = "parser: ";

    private static final String LABEL_LINE = "line: ";

    private static final String COMPILER_OUT = "Compiler Out: ";

    private static final String MISSING_SEMICOLON = "Missing end-of-line semicolon";

    private static final String UNKNOWN_KEYWORD = "Unknown keyword";

    private static final String UNKNOWN_STATEMENT_TYPE = "Unknown statement type";

    private static final String SPECIES_NOT_DECLARED_PREFIX = "Species ";

    private static final String SPECIES_NOT_DECLARED_SUFFIX = " has not been declared but is referenced";

    private static final String EXPECTED_SYMBOL_OR_QUOTED_STRING_PREFIX = "Expected symbol or quoted string but found ";

    private static final String EXPECTED_SYMBOL_OR_QUOTED_STRING_SUFFIX = ", causing error";

    private static final String UNEXPECTED_TOKEN_TYPE_PREFIX = "Invalid token, ";

    private static final String UNEXPECTED_TOKEN_TYPE_SUFFIX = ", in reaction definition (likely to be missing some punctuation)";

    private static final String XML_FORMAT_ERROR = "Unable to load XML file.  Invalid formatting";

    private Component mMainFrame;

    private DizzyTextArea mEditorPaneTextArea;

    private MainApp mMainApp;

    private JPanel mLabelPanel;

    private JLabel mFileNameLabel;

    private JPanel mTestPanel;

    private JLabel mParserAliasLabel;

    private JLabel mLineNumberLabel;

    private JLabel mCompilerOutputLabel;

    private JPanel mBottomPanel;

    private Color mBottomPanelBackground;

    private String mFileName;

    private String mParserAlias;

    private boolean mBufferDirty;

    private long mTimestampLastChange;

    private IModelBuilder mModelBuilder;

    private ImageIcon mCompilerGreenTick;

    private ImageIcon mCompilerRedCross;

    private VerticalErrorPane vep;

    public int currentErrorLine = -1;

    public UndoManager undo;

    public UndoAction undoAction;

    public RedoAction redoAction;

    public int lastFoundPoint = 0;

    EditorPane() {
        initialize();
        mMainApp = MainApp.getApp();
        mMainFrame = MainApp.getMainFrame();
        setFileNameLabel(null);
        setBufferDirty(false);
        setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
        CompileThread t = new CompileThread(this);
        t.start();
    }

    public DizzyTextArea getDTA() {
        return mEditorPaneTextArea;
    }

    private void initialize() {
        setBorder(BorderFactory.createEtchedBorder());
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setLayout(new BorderLayout());
        mLabelPanel = new JPanel();
        mLabelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mLabelPanel.setLayout(new BoxLayout(mLabelPanel, BoxLayout.Y_AXIS));
        mFileNameLabel = new JLabel(LABEL_FILE + "(none)");
        Font plainFont = mFileNameLabel.getFont().deriveFont(Font.PLAIN);
        mFileNameLabel.setFont(plainFont);
        mParserAliasLabel = new JLabel(LABEL_PARSER + "(none)");
        mParserAliasLabel.setFont(plainFont);
        mLabelPanel.add(mFileNameLabel);
        mLabelPanel.add(mParserAliasLabel);
        initializeEditorTextArea();
        mBottomPanel = new JPanel();
        mBottomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mBottomPanel.setLayout(new BoxLayout(mBottomPanel, BoxLayout.Y_AXIS));
        mLineNumberLabel = new JLabel(LABEL_LINE + "???");
        mLineNumberLabel.setFont(plainFont);
        mCompilerGreenTick = new ImageIcon("./images/tick.jpg");
        mCompilerRedCross = new ImageIcon("./images/cross.jpg");
        mCompilerOutputLabel = new JLabel(COMPILER_OUT, mCompilerGreenTick, SwingConstants.CENTER);
        mCompilerOutputLabel.setFont(plainFont);
        mCompilerOutputLabel.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                if (currentErrorLine != -1) {
                    mEditorPaneTextArea.setCaretToLineNumber(currentErrorLine);
                }
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
        mCompilerOutputLabel.addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                if (currentErrorLine == -1) {
                    mCompilerOutputLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                } else {
                    mCompilerOutputLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }
        });
        mBottomPanel.add(mLineNumberLabel);
        mBottomPanel.add(mCompilerOutputLabel);
        mBottomPanel.setBackground(mBottomPanelBackground);
        vep = new VerticalErrorPane(this);
        vep.setLayout(new BorderLayout());
        mTestPanel = new JPanel();
        mTestPanel.setLayout(null);
        mEditorPaneTextArea.setBorder(new BevelBorder(1));
        mTestPanel.add(mEditorPaneTextArea);
        JPanel buffer1 = new JPanel();
        buffer1.setLayout(new BorderLayout());
        vep.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        buffer1.setBorder(BorderFactory.createEmptyBorder());
        buffer1.add(vep, BorderLayout.CENTER);
        buffer1.setPreferredSize(new Dimension(10, 20));
        add(mLabelPanel, BorderLayout.NORTH);
        add(mTestPanel, BorderLayout.CENTER);
        add(buffer1, BorderLayout.EAST);
        add(mBottomPanel, BorderLayout.SOUTH);
        handleCaretPositionChange();
        undo = new UndoManager();
        mEditorPaneTextArea.getDocument().addUndoableEditListener(new DizzyUndoableEditListener(this));
        undoAction = new UndoAction(this);
        redoAction = new RedoAction(this);
    }

    private void initializeEditorTextArea() {
        DocumentListener docListener = new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                handleEditorDocumentChange(e);
            }

            public void insertUpdate(DocumentEvent e) {
                handleEditorDocumentChange(e);
            }

            public void removeUpdate(DocumentEvent e) {
                handleEditorDocumentChange(e);
            }
        };
        CaretListener carListener = new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                handleCaretPositionChange();
            }
        };
        mEditorPaneTextArea = new DizzyTextArea();
        mEditorPaneTextArea.setFilename(mFileName);
        mEditorPaneTextArea.setEditable(true);
        mEditorPaneTextArea.getDocument().addDocumentListener(docListener);
        mEditorPaneTextArea.addCaretListener(carListener);
        mEditorPaneTextArea.setScrolls(false, false);
    }

    public boolean close() {
        boolean doClose = false;
        String fileName = getFileName();
        if (getBufferDirty()) {
            SimpleTextArea textArea;
            String msg;
            if (fileName != null) {
                String[] nastyArray = fileName.split("/");
                fileName = nastyArray[nastyArray.length - 1];
                msg = "Unsaved changes have been made to " + fileName + ".  \nAre you sure you wish to close this file?";
            } else {
                msg = "The current file has not been saved.  \nAre you sure you wish to close this file?";
            }
            textArea = new SimpleTextArea(msg);
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(textArea);
            optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
            optionPane.setOptionType(JOptionPane.YES_NO_OPTION);
            optionPane.createDialog(mMainFrame, "Are you sure wish to close this file?").setVisible(true);
            Integer response = (Integer) optionPane.getValue();
            if (null != response && response.intValue() == JOptionPane.YES_OPTION) {
                doClose = true;
            } else {
                if (null == response) {
                    handleCancel("close");
                }
            }
        } else {
            doClose = true;
        }
        if (doClose) {
            setFileNameLabel(null);
            setTimestampLastChange(TIMESTAMP_BUFFER_LAST_CHANGE_NULL);
            clearEditorText();
            mMainApp.updateMenus();
            setParserAliasLabel(null);
            setModelBuilder(null);
            setBufferDirty(false);
        }
        return (doClose);
    }

    public void open() {
        FileChooser fileChooser = new FileChooser();
        javax.swing.filechooser.FileFilter fileFilter = new ChemFileFilter();
        File currentDirectory = mMainApp.getCurrentDirectory();
        if (null != currentDirectory) {
            fileChooser.setCurrentDirectory(currentDirectory);
        }
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Please select a file to open");
        int result = fileChooser.showOpenDialog(mMainFrame);
        if (JFileChooser.APPROVE_OPTION == result) {
            File inputFile = fileChooser.getSelectedFile();
            String fileName = inputFile.getAbsolutePath();
            if (inputFile.exists()) {
                File parentFile = inputFile.getParentFile();
                if (parentFile.isDirectory()) {
                    mMainApp.setCurrentDirectory(parentFile);
                }
                loadFileToEditBuffer(fileName);
                HistoryUtilImpl.updateFileHistory(inputFile);
                MainApp.getApp().initializeMainMenu(MainApp.getMainFrame());
            } else {
                SimpleTextArea textArea = new SimpleTextArea("The file you selected does not exist:\n" + fileName + "\n");
                JOptionPane.showMessageDialog(mMainFrame, textArea, "Open cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void open(File aFile) {
        String fileName = aFile.getAbsolutePath();
        if (aFile.exists()) {
            File parentFile = aFile.getParentFile();
            if (parentFile.isDirectory()) {
                mMainApp.setCurrentDirectory(parentFile);
                mMainApp.setCurrentFile(aFile);
            }
            loadFileToEditBuffer(fileName);
        } else {
            SimpleTextArea textArea = new SimpleTextArea("The file you selected does not exist:\n" + fileName + "\n");
            JOptionPane.showMessageDialog(mMainFrame, textArea, "Open cancelled", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void save() {
        try {
            saveEditBufferToFile(getFileName());
            mMainApp.updateMenus();
            File tempFile = new File(getFileName());
            HistoryUtilImpl.updateFileHistory(tempFile);
        } catch (NullPointerException e) {
            saveAs();
        }
    }

    public void commentSelection() {
        this.mEditorPaneTextArea.commentSelection(this.getParserAlias());
    }

    public void unCommentSelection() {
        this.mEditorPaneTextArea.unCommentSelection(this.getParserAlias());
    }

    public void saveAs() {
        FileChooser fileChooser = new FileChooser();
        javax.swing.filechooser.FileFilter fileFilter = new ChemFileFilter();
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setApproveButtonText("Save");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Please select a file name to save as");
        String curFileName = getFileName();
        File curFile = null;
        if (null != curFileName) {
            curFile = new File(curFileName);
            if (curFile.exists()) {
                fileChooser.setSelectedFile(curFile);
            }
        }
        int result = fileChooser.showSaveDialog(mMainFrame);
        if (JFileChooser.APPROVE_OPTION == result) {
            boolean doSave = false;
            File outputFile = fileChooser.getSelectedFile();
            String fileName = outputFile.getAbsolutePath();
            if (outputFile.exists()) {
                if (null == curFile || curFileName.equals(fileName)) {
                    doSave = true;
                } else {
                    SimpleTextArea textArea = new SimpleTextArea("The file you selected already exists:\n" + fileName + "\nThe save operation will overwrite this file.\nAre you sure you want to proceed?");
                    JOptionPane optionPane = new JOptionPane();
                    optionPane.setMessage(textArea);
                    optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
                    optionPane.setOptionType(JOptionPane.YES_NO_OPTION);
                    optionPane.createDialog(mMainFrame, "Overwrite existing file?").setVisible(true);
                    Integer response = (Integer) optionPane.getValue();
                    if (null != response && response.intValue() == JOptionPane.YES_OPTION) {
                        doSave = true;
                    } else {
                        if (null == response) {
                            handleCancel("save");
                        }
                    }
                }
            } else {
                doSave = true;
            }
            if (doSave && null != curFileName && !(curFileName.equals(fileName))) {
                setParserAliasLabel(null);
                setModelBuilder(null);
            }
            if (doSave) {
                saveEditBufferToFile(fileName);
                mMainApp.updateMenus();
                mMainApp.setCurrentDirectory(outputFile.getParentFile());
                mMainApp.setCurrentFile(outputFile);
                HistoryUtilImpl.updateFileHistory(outputFile);
            }
        }
    }

    private void handleEditorDocumentChange(DocumentEvent e) {
        if (!getBufferDirty()) {
            setBufferDirty(true);
        }
        setTimestampLastChange(System.currentTimeMillis());
        mMainApp.updateMenus();
    }

    /**
	 * Alex
	 * Bobby - added horizontal scroll bar. Altered method calls to setting scroll bars
	           visibility as well.
	 */
    public void updateScrolls() {
        updateTextArea();
        mEditorPaneTextArea.recalculateVisibleLines();
        if (mEditorPaneTextArea.getLineCount() > mEditorPaneTextArea.getVisibleLines()) {
            mEditorPaneTextArea.setVerticalScroll(true);
        } else {
            mEditorPaneTextArea.setVerticalScroll(false);
        }
        String lineText;
        int lineWidth, editorPaneWidth;
        editorPaneWidth = mEditorPaneTextArea.getPainter().getWidth();
        for (int i = 0; i < mEditorPaneTextArea.getLineCount(); i++) {
            lineText = mEditorPaneTextArea.getLineText(i);
            lineWidth = mEditorPaneTextArea.getPainter().getFontMetrics().stringWidth(lineText);
            if (lineWidth >= editorPaneWidth) {
                mEditorPaneTextArea.setHorizontalScroll(true);
                break;
            } else {
                mEditorPaneTextArea.setHorizontalScroll(false);
            }
        }
        updateTextArea();
    }

    /**
	 * Alex I need this to be able to resize the text area to cover or not to
	 * cover the scroll bars.
	 */
    private void updateTextArea() {
        mEditorPaneTextArea.updateTextArea(mTestPanel.getBounds());
    }

    private void handleCaretPositionChange() {
        int linePosition = mEditorPaneTextArea.getLineOfOffset(mEditorPaneTextArea.getCaretPosition()) + 1;
        mLineNumberLabel.setText(LABEL_LINE + linePosition);
    }

    void clearEditorText() {
        int textLen = mEditorPaneTextArea.getText().length();
        mEditorPaneTextArea.replaceRange(null, 0, textLen);
    }

    private IModelBuilder queryModelBuilder(String pFileName) {
        IModelBuilder modelBuilder = null;
        ParserPicker parserPicker = new ParserPicker(mMainFrame);
        String parserAlias = null;
        if (null != pFileName) {
            parserAlias = parserPicker.selectParserAliasFromFileName(pFileName);
        } else {
            parserAlias = parserPicker.selectParserAliasManually();
        }
        if (null != parserAlias) {
            ClassRegistry modelBuilderRegistry = mMainApp.getModelBuilderRegistry();
            try {
                modelBuilder = (IModelBuilder) modelBuilderRegistry.getInstance(parserAlias);
            } catch (DataNotFoundException e) {
                throw new IllegalStateException("error creating model builder for alias: " + parserAlias);
            }
            if (null != modelBuilder) {
                setModelBuilder(modelBuilder);
                setParserAliasLabel(parserAlias);
            }
        }
        return modelBuilder;
    }

    public Model processModel() {
        Model model = null;
        try {
            IModelBuilder modelBuilder = getModelBuilder();
            String fileName = getFileName();
            if (null == modelBuilder) {
                modelBuilder = queryModelBuilder(fileName);
            }
            if (null == modelBuilder) {
                JOptionPane.showMessageDialog(mMainFrame, "Your model processing has been cancelled", "Model processing cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String modelText = mEditorPaneTextArea.getText();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                modelBuilder.writeModel(modelText, outputStream);
                byte[] bytes = outputStream.toByteArray();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                IncludeHandler includeHandler = new IncludeHandler();
                File currentDirectory = mMainApp.getCurrentDirectory();
                if (null != currentDirectory || null != fileName) {
                    if (null != fileName) {
                        File file = new File(fileName);
                        if (file.exists()) {
                            currentDirectory = file.getParentFile();
                        }
                    }
                    includeHandler.setDirectory(currentDirectory);
                }
                model = modelBuilder.buildModel(inputStream, includeHandler);
            }
        } catch (InvalidInputException e) {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, "Sorry, there was an error processing the model.  The specific error message is:");
            optionPane.createDialog(mMainFrame, "error in model definition").setVisible(true);
            return (model);
        } catch (IOException e) {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, "Sorry, there was an error processing the model.  The specific error message is:");
            optionPane.createDialog(mMainFrame, "I/O error in processing model definition").setVisible(true);
            return (model);
        }
        return model;
    }

    boolean editorBufferIsEmpty() {
        return (0 == mEditorPaneTextArea.getText().length());
    }

    public void saveEditBufferToFile(String pFileName) {
        File file = new File(pFileName);
        String shortFileName = file.getName();
        try {
            IModelBuilder modelBuilder = getModelBuilder();
            if (null == modelBuilder) {
                modelBuilder = queryModelBuilder(pFileName);
            }
            if (null == modelBuilder) {
                JOptionPane.showMessageDialog(mMainFrame, "Your model saving has been cancelled", "Model saving cancelled", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String fileContents = mEditorPaneTextArea.getText();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            modelBuilder.writeModel(fileContents, fileOutputStream);
            setBufferDirty(false);
        } catch (Exception e) {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, "Sorry, there was an error saving the model.  The specific error message is:");
            optionPane.createDialog(mMainFrame, "error saving file: " + shortFileName).setVisible(true);
            return;
        }
        setFileNameLabel(pFileName);
    }

    public void loadFileToEditBuffer(String pFileName) {
        boolean didClose = close();
        if (!didClose) {
            return;
        }
        File file = new File(pFileName);
        MainApp.getApp().setCurrentFile(new File(pFileName));
        String shortFileName = file.getName();
        try {
            IModelBuilder modelBuilder = queryModelBuilder(pFileName);
            if (null == modelBuilder) {
                JOptionPane.showMessageDialog(mMainFrame, "Your file loading has been cancelled", "File loading cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                InputStream inputStream = new FileInputStream(file);
                String modelText = modelBuilder.readModel(inputStream);
                clearEditorText();
                mEditorPaneTextArea.append(modelText);
                setFileNameLabel(pFileName);
                setBufferDirty(false);
                setTimestampLastChange(System.currentTimeMillis());
                mMainApp.updateMenus();
            }
        } catch (Exception e) {
            ExceptionNotificationOptionPane optionPane = new ExceptionNotificationOptionPane(e, "Sorry, there was an error loading the model.  The specific error message is:");
            optionPane.createDialog(mMainFrame, "error loading file: " + shortFileName).setVisible(true);
            return;
        }
    }

    private void setParserAliasLabel(String pParserAlias) {
        setParserAlias(pParserAlias);
        if (null != pParserAlias) {
            mParserAliasLabel.setText(LABEL_PARSER + pParserAlias);
        } else {
            mParserAliasLabel.setText(LABEL_PARSER + "(none)");
        }
    }

    private void setFileNameLabel(String pFileName) {
        setFileName(pFileName);
        if (null != pFileName) {
            mFileNameLabel.setText(LABEL_FILE + pFileName);
        } else {
            mFileNameLabel.setText(LABEL_FILE + "(none)");
        }
        mEditorPaneTextArea.setFilename(pFileName);
    }

    interface EditorStateUpdater {

        public void updateEditorState();
    }

    String getParserAlias() {
        return (mParserAlias);
    }

    public DizzyTextArea getTextArea() {
        return mEditorPaneTextArea;
    }

    void setParserAlias(String pParserAlias) {
        mParserAlias = pParserAlias;
    }

    String getFileName() {
        return (mFileName);
    }

    private void setFileName(String pFileName) {
        mFileName = pFileName;
    }

    boolean getBufferDirty() {
        return (mBufferDirty);
    }

    private void setBufferDirty(boolean pBufferDirty) {
        mBufferDirty = pBufferDirty;
    }

    private void setTimestampLastChange(long pTimestampLastChange) {
        mTimestampLastChange = pTimestampLastChange;
    }

    long getTimestampLastChange() {
        return (mTimestampLastChange);
    }

    private void setModelBuilder(IModelBuilder pModelBuilder) {
        mModelBuilder = pModelBuilder;
    }

    private IModelBuilder getModelBuilder() {
        return (mModelBuilder);
    }

    private void handleCancel(String pOperation) {
        JOptionPane.showMessageDialog(mMainFrame, "Your " + pOperation + " operation has been cancelled", pOperation + " cancelled", JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleCut() {
        mEditorPaneTextArea.cut();
    }

    /**
	 * When "go to line" is clicked this handles the user input, reports an
	 * invalid line number if necessary and then moves the cursor to the
	 * required line number.
	 * 
	 */
    public void handleGoToLine() {
        String message = "Enter line number";
        String title = "Go to line";
        String line = JOptionPane.showInputDialog(mMainFrame, message, title, JOptionPane.QUESTION_MESSAGE);
        if (null == line) {
            return;
        }
        line = line.trim();
        if (line.length() > 0) {
            try {
                int lineNum = Integer.parseInt(line);
                if (validLineNum(lineNum)) {
                    goToLine(lineNum);
                } else {
                    handleGoToLine();
                }
            } catch (NumberFormatException ex) {
                showErrorMessage("Please enter a valid number.", "Incorrect number format!");
                handleGoToLine();
            }
        } else {
            showErrorMessage("Number was blank, try again.", "Blank number!");
            handleGoToLine();
        }
    }

    public void goToLine(int lineNum) {
        mEditorPaneTextArea.setCaretToLineNumber(lineNum);
    }

    /**
	 * When "Search" is clicked this handles the user input, reports an
	 * invalid search if necessary and then moves the cursor to the
	 * required line with the search result.
	 * 
	 *  Author: Kumarsamy Shanmuganathan
	 *  (c) Dizzy 2006
	 */
    public void handleSearch() {
        String message = "Search for:";
        String title = "Search";
        String searchValue = JOptionPane.showInputDialog(mMainFrame, message, title, JOptionPane.QUESTION_MESSAGE);
        if (searchValue == null) {
            return;
        }
        searchValue = searchValue.trim();
        if (searchValue.length() > 0) {
            boolean searchResult = handleFind(searchValue, 1, false);
            if (searchResult == true) {
            } else {
                showErrorMessage("Search not Found, try again.", "Invalid Search!");
                handleSearch();
            }
        } else {
            showErrorMessage("Search was blank, try again.", "Blank Search!");
            handleSearch();
        }
    }

    public void handleSNReplace() {
        new FindReplaceWindow(this).setVisible(true);
    }

    private boolean validLineNum(int lineNum) {
        if (lineNum > mEditorPaneTextArea.getLineCount()) {
            showErrorMessage("That line number is greater than " + "the number of lines in the document. Please " + "enter another value", "Invalid line number!");
            return false;
        } else if (lineNum <= 0) {
            showErrorMessage("Line numbers less than or equal to zero are not valid. Enter a valid number", "Invalid line number!");
            return false;
        } else {
            return true;
        }
    }

    private void showErrorMessage(String error, String title) {
        JOptionPane.showMessageDialog(mMainFrame, error, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
	 * Selects the first location of str after point start Returns true if it
	 * found an occurance, false otherwise
	 */
    public boolean handleFind(String str, int start, boolean caseSensitive) {
        String text = mEditorPaneTextArea.getText();
        int strLength = str.length();
        int textLength = text.length();
        for (int i = start; i < textLength; i++) {
            char leftChar = text.charAt(i);
            char rightChar = str.charAt(0);
            if (caseSensitive == false) {
                leftChar = Character.toLowerCase(leftChar);
                rightChar = Character.toLowerCase(rightChar);
            }
            strIf: if (leftChar == rightChar && i + strLength < textLength) {
                for (int j = 0; j < strLength; j++) {
                    leftChar = text.charAt(i + j);
                    rightChar = str.charAt(j);
                    if (caseSensitive == false) {
                        leftChar = Character.toLowerCase(leftChar);
                        rightChar = Character.toLowerCase(rightChar);
                    }
                    if (leftChar != rightChar) {
                        break strIf;
                    }
                }
                mEditorPaneTextArea.select(i, i + strLength);
                mEditorPaneTextArea.scrollTo(mEditorPaneTextArea.getSelectionStartLine(), 0);
                return true;
            }
        }
        return false;
    }

    /**
	 * Continues to handleFind from the end of the currently selected text
	 * Returns true if it found an occurance, false otherwise
	 */
    public boolean handleFindNext(String str, boolean caseSensitive) {
        int newStart = mEditorPaneTextArea.getSelectionEnd();
        return handleFind(str, newStart, caseSensitive);
    }

    /**
	 * It's ugly I know, but it implements Find Previous i.e. bastardized
	 * version of handleFind that works backwards from the character before the
	 * selected text. Returns true if it finds an occurance, false otherwise
	 */
    public boolean handleFindPrevious(String str, boolean caseSensitive) {
        int start = mEditorPaneTextArea.getSelectionStart() - 1;
        String text = mEditorPaneTextArea.getText();
        int strLength = str.length();
        int textLength = text.length();
        for (int i = start; i >= 0; i--) {
            char leftChar = text.charAt(i);
            char rightChar = str.charAt(0);
            if (caseSensitive == false) {
                leftChar = Character.toLowerCase(leftChar);
                rightChar = Character.toLowerCase(rightChar);
            }
            strIf: if (leftChar == rightChar && i + strLength < textLength) {
                for (int j = 0; j < strLength; j++) {
                    leftChar = text.charAt(i + j);
                    rightChar = str.charAt(j);
                    if (caseSensitive == false) {
                        leftChar = Character.toLowerCase(leftChar);
                        rightChar = Character.toLowerCase(rightChar);
                    }
                    if (leftChar != rightChar) {
                        break strIf;
                    }
                }
                mEditorPaneTextArea.select(i, i + strLength);
                mEditorPaneTextArea.scrollTo(mEditorPaneTextArea.getSelectionStartLine(), 0);
                return true;
            }
        }
        return false;
    }

    /**
	 * Replaces the selected text (if there is any) with newStr
	 */
    public void handleReplace(String newStr) {
        if (mEditorPaneTextArea.getSelectedText() != null) {
            mEditorPaneTextArea.setSelectedText(newStr);
        }
    }

    /**
	 * Finds all occurances of str from start and replaces them with newStr
	 */
    public void handleReplaceAll(String oldStr, String newStr, int start, boolean caseSensitive) {
        boolean carryOn = handleFind(oldStr, start, caseSensitive);
        while (carryOn) {
            handleReplace(newStr);
            carryOn = handleFindNext(oldStr, caseSensitive);
        }
    }

    /**
	 * Trivial method that selects all the text in the pane
	 */
    public void handleSelectAll() {
        mEditorPaneTextArea.selectAll();
    }

    public void handlePaste() {
        mEditorPaneTextArea.paste();
    }

    public void handleCopy() {
        mEditorPaneTextArea.copy();
    }

    public void handleDelete() {
        mEditorPaneTextArea.processKeyEvent(new KeyEvent(mEditorPaneTextArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED));
    }

    public void paint(Graphics g) {
        updateScrolls();
        super.paint(g);
    }

    /**
	 * Cat and Dave Function to compile contents of text area in the background
	 * and indicate errors accordingly.
	 */
    String backgroundCompile() {
        try {
            IModelBuilder modelBuilder = getModelBuilder();
            String fileName = getFileName();
            if (fileName == null) {
                modelBuilder = queryModelBuilder("filename.cmdl");
            }
            if (null == modelBuilder) {
                modelBuilder = queryModelBuilder(fileName);
            }
            if (null == modelBuilder) {
                return "model processing cancelled";
            }
            String modelText = mEditorPaneTextArea.getText();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            modelBuilder.writeModel(modelText, outputStream);
            byte[] bytes = outputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            IncludeHandler includeHandler = new IncludeHandler();
            File currentDirectory = mMainApp.getCurrentDirectory();
            if (null != currentDirectory || null != fileName) {
                if (null != fileName) {
                    File file = new File(fileName);
                    if (file.exists()) {
                        currentDirectory = file.getParentFile();
                    }
                }
                includeHandler.setDirectory(currentDirectory);
            }
            modelBuilder.buildModel(inputStream, includeHandler);
        } catch (InvalidInputException e) {
            return e.getMessage();
        } catch (IOException e) {
            return "ERROR in IO: " + e.getMessage();
        }
        return null;
    }

    private String parseMessage(String m) {
        String parsedError = "";
        int lineNo = -1;
        if (mParserAlias.equals("command-language")) {
            String[] s = m.split(" ");
            int i = 0;
            int l = s.length;
            while (i < l) {
                String w = s[i];
                if (w.equals("line") && i < (l - 1)) {
                    try {
                        lineNo = Integer.parseInt(s[i + 1]);
                        lineNo--;
                        int width = 0;
                        FontMetrics fm = getFontMetrics(mEditorPaneTextArea.getPainter().getFont());
                        String text = "";
                        try {
                            text = mEditorPaneTextArea.getLineText(lineNo);
                            width = fm.stringWidth(mEditorPaneTextArea.getLineText(lineNo));
                            if (width == 0) {
                                lineNo--;
                                text = mEditorPaneTextArea.getLineText(lineNo);
                                width = fm.stringWidth(mEditorPaneTextArea.getLineText(lineNo));
                                if (width == 0) {
                                    lineNo += 2;
                                    text = mEditorPaneTextArea.getLineText(lineNo);
                                    width = fm.stringWidth(mEditorPaneTextArea.getLineText(lineNo));
                                    if (width == 0) {
                                        lineNo--;
                                        width = 20;
                                    }
                                }
                            }
                            width = fm.stringWidth(mEditorPaneTextArea.getLineText(lineNo));
                        } catch (Exception e) {
                            if (lineNo >= 0) {
                                System.out.println("Error highlighting failed at line " + lineNo + ": " + mEditorPaneTextArea.getLineText(lineNo));
                            } else {
                                System.out.println("Error highlighting failed");
                            }
                        }
                        if (m.contains("semicolon")) {
                            lineNo++;
                            highlightSemicolon(lineNo - 1, width);
                            parsedError = MISSING_SEMICOLON;
                        } else if (m.contains("species")) {
                            try {
                                lineNo++;
                                highlightToken(lineNo - 1, m, text);
                                parsedError = SPECIES_NOT_DECLARED_PREFIX + s[1] + SPECIES_NOT_DECLARED_SUFFIX;
                            } catch (HighlightLineException e1) {
                                try {
                                    highlightToken(lineNo - 1, m, mEditorPaneTextArea.getLineText(lineNo - 1));
                                } catch (HighlightLineException e2) {
                                    highlightLine(lineNo, width);
                                }
                            }
                        } else if (m.contains("unknown statement type")) {
                            parsedError = UNKNOWN_STATEMENT_TYPE;
                            lineNo++;
                            if (text.endsWith(";")) {
                                dashline(lineNo - 1, width);
                            } else if (parsedError.equals(MISSING_SEMICOLON) == false) {
                                highlightSemicolon(lineNo - 1, width);
                                parsedError = MISSING_SEMICOLON;
                            }
                        } else {
                            if (m.contains("unknown keyword")) {
                                parsedError = UNKNOWN_KEYWORD;
                            } else if (m.contains("expected symbol or quoted string")) {
                                parsedError = EXPECTED_SYMBOL_OR_QUOTED_STRING_PREFIX + s[s.length - 4] + EXPECTED_SYMBOL_OR_QUOTED_STRING_SUFFIX;
                            } else if (m.contains("invalid token type")) {
                                parsedError = UNEXPECTED_TOKEN_TYPE_PREFIX + s[s.length - 4] + UNEXPECTED_TOKEN_TYPE_SUFFIX;
                            }
                            lineNo++;
                            if (text.endsWith(";")) {
                                dashline(lineNo - 1, width);
                            } else if (parsedError.equals(MISSING_SEMICOLON) == false) {
                                highlightSemicolon(lineNo - 1, width);
                                parsedError = MISSING_SEMICOLON;
                            }
                        }
                    } catch (NumberFormatException e) {
                        i++;
                    }
                    break;
                }
                i++;
            }
        } else if (mParserAlias.equals("markup-language")) {
            parsedError = XML_FORMAT_ERROR;
        } else {
        }
        currentErrorLine = lineNo;
        return parsedError;
    }

    private void highlightLine(int line, int width) {
        if (width - startPoint() > getWidth()) {
            width = getWidth() - 3;
        }
        mEditorPaneTextArea.highlightLine(line, width);
    }

    private void highlightSemicolon(int line, int width) {
        int end = width - startPoint();
        int space = getWidth() - startPoint();
        if (space > end) {
            mEditorPaneTextArea.highlightToken(line, width - 10, 10);
        } else {
            dashline(line, space);
        }
    }

    private void highlightToken(int line, String m, String t) throws HighlightLineException {
        String[] s = m.split(" ");
        String[] st = t.split(" ");
        FontMetrics fm = getFontMetrics(mEditorPaneTextArea.getPainter().getFont());
        int space = getWidth() - startPoint();
        int i = 0;
        int l = s.length;
        String token = " ";
        while (i < l) {
            String w = s[i];
            if (w.equalsIgnoreCase("species") && i < (l - 1)) {
                token = s[i + 1];
                token = token.replace("\"", "");
                break;
            }
            i++;
        }
        if (i == l - 1) {
            throw new HighlightLineException();
        }
        String before = "";
        i = 0;
        l = st.length;
        while (i < l) {
            String w = st[i];
            if (w.startsWith(token)) {
                break;
            }
            before = before + w + " ";
            i++;
        }
        int start = fm.stringWidth(before) - startPoint();
        int wid = fm.stringWidth(token);
        if (space < start + wid) {
            wid = space - start;
        }
        mEditorPaneTextArea.highlightToken(line, start, wid);
    }

    private void dashline(int line, int width) {
        mEditorPaneTextArea.dashline(line, width);
    }

    protected void displayError(String e) {
        if (e == null || e.length() < 1) {
            mCompilerOutputLabel.setIcon(mCompilerGreenTick);
            mCompilerOutputLabel.setText(COMPILER_OUT + "No compiler errors");
            currentErrorLine = -1;
        } else {
            mCompilerOutputLabel.setIcon(mCompilerRedCross);
            String parsedError = parseMessage(e);
            if (parsedError.equals("") == false) {
                if (currentErrorLine != -1) {
                    parsedError = parsedError + " at line " + currentErrorLine;
                } else {
                    parsedError = parsedError + " at unknown line";
                }
            } else {
                parsedError = e;
            }
            mCompilerOutputLabel.setText(COMPILER_OUT + parsedError);
        }
        vep.repaint();
    }

    private int startPoint() {
        return 0;
    }
}

class DizzyUndoableEditListener implements UndoableEditListener {

    private EditorPane editorPane;

    DizzyUndoableEditListener(EditorPane editorPane) {
        super();
        this.editorPane = editorPane;
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        editorPane.undo.addEdit(e.getEdit());
        editorPane.undoAction.updateUndoState();
        editorPane.redoAction.updateRedoState();
    }
}

class UndoAction extends AbstractAction {

    EditorPane editorPane;

    public UndoAction(EditorPane editorPane) {
        super("Undo");
        setEnabled(false);
        this.editorPane = editorPane;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            editorPane.undo.undo();
        } catch (CannotUndoException ex) {
            System.out.println("Unable to undo: " + ex);
            ex.printStackTrace();
        }
        updateUndoState();
        editorPane.redoAction.updateRedoState();
    }

    protected void updateUndoState() {
        if (editorPane.undo.canUndo()) {
            setEnabled(true);
            putValue(Action.NAME, editorPane.undo.getUndoPresentationName());
        } else {
            setEnabled(false);
            putValue(Action.NAME, "Undo");
        }
    }
}

class RedoAction extends AbstractAction {

    EditorPane editorPane;

    public RedoAction(EditorPane editorPane) {
        super("Redo");
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            editorPane.undo.redo();
        } catch (CannotRedoException ex) {
            System.out.println("Unable to redo: " + ex);
            ex.printStackTrace();
        }
        updateRedoState();
        editorPane.undoAction.updateUndoState();
    }

    protected void updateRedoState() {
        try {
            if (editorPane.undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, editorPane.undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        } catch (NullPointerException e) {
        }
    }
}
