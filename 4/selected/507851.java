package com.ienjinia.vc.devkit;

import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import com.ienjinia.vc.env.Environment;
import com.ienjinia.vc.hardware.ExceptionHandler;
import com.ienjinia.vc.hardware.IAVC;
import com.ienjinia.vc.hardware.VideoCanvas;
import com.ienjinia.vc.util.PrintUtil;
import com.ienjinia.vc.util.Selector;

public class DevKit extends JFrame implements ExceptionHandler {

    private Environment environment;

    private String projectName;

    private VideoCanvas videoCanvas;

    private IAVC iavc;

    private JConsole jConsole;

    private JLabel editorLabel;

    private JLabel caretLabel;

    private JTextArea editor;

    private JButton playBtn;

    private JButton packageBtn;

    private JButton resetIAVCBtn;

    private JButton seeVBIExceptionBtn;

    private JButton seeSFIExceptionBtn;

    private JButton newBtn;

    private JButton openBtn;

    private JButton saveBtn;

    private JButton saveAsBtn;

    private JButton executeBtn;

    private JButton printBtn;

    private Interpreter interpreter;

    private String editorFileName;

    private boolean modified = true;

    private Throwable vbiError;

    private Throwable sfiError;

    private Thread thread;

    private UndoManager undo = new UndoManager();

    public DevKit(Environment env, String projName) throws IOException, EvalError {
        this.environment = env;
        this.projectName = projName;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                if (modified) {
                    int option = JOptionPane.showConfirmDialog(DevKit.this, "There are unsaved changes in " + editorFileName + ". Exit anyway?", "Unsaved changes!", JOptionPane.YES_NO_OPTION);
                    if (option != JOptionPane.YES_OPTION) return;
                }
                System.exit(0);
            }

            public void windowOpened(WindowEvent e) {
                try {
                    loadSource("main.bsh");
                } catch (IOException ex) {
                }
                thread.start();
                videoCanvas.start();
            }
        });
        setTitle("IENJINIA DevKit: " + projectName);
        Container c = getContentPane();
        SpringLayout layout = new SpringLayout();
        c.setLayout(layout);
        videoCanvas = new VideoCanvas();
        iavc = videoCanvas.getIAVC();
        iavc.setSaveFile(environment.toSavePath(projectName));
        iavc.setExceptionHandler(this);
        c.add(videoCanvas);
        jConsole = new JConsole();
        c.add(jConsole);
        editorLabel = new JLabel("(no name)");
        c.add(editorLabel);
        caretLabel = new JLabel(":");
        c.add(caretLabel);
        editor = new JTextArea();
        c.add(editor);
        editor.setLineWrap(true);
        editor.setFont(new Font("monospaced", Font.PLAIN, 12));
        editor.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(editor);
        c.add(scrollPane);
        JPanel consoleBtns = new JPanel();
        c.add(consoleBtns);
        consoleBtns.setLayout(new BoxLayout(consoleBtns, BoxLayout.X_AXIS));
        playBtn = new JButton("Play");
        consoleBtns.add(playBtn);
        packageBtn = new JButton("Package");
        consoleBtns.add(packageBtn);
        resetIAVCBtn = new JButton("Reset IAVC");
        consoleBtns.add(resetIAVCBtn);
        seeVBIExceptionBtn = new JButton("VBI");
        seeVBIExceptionBtn.setForeground(Color.red);
        seeVBIExceptionBtn.setEnabled(false);
        consoleBtns.add(seeVBIExceptionBtn);
        seeSFIExceptionBtn = new JButton("SFI");
        seeSFIExceptionBtn.setForeground(Color.red);
        seeSFIExceptionBtn.setEnabled(false);
        consoleBtns.add(seeSFIExceptionBtn);
        JPanel editorBtns = new JPanel();
        c.add(editorBtns);
        editorBtns.setLayout(new BoxLayout(editorBtns, BoxLayout.X_AXIS));
        newBtn = new JButton("New");
        editorBtns.add(newBtn);
        openBtn = new JButton("Open...");
        editorBtns.add(openBtn);
        saveBtn = new JButton("Save");
        editorBtns.add(saveBtn);
        saveAsBtn = new JButton("Save As...");
        editorBtns.add(saveAsBtn);
        executeBtn = new JButton("Execute");
        editorBtns.add(executeBtn);
        printBtn = new JButton("Print");
        editorBtns.add(printBtn);
        layout.putConstraint(SpringLayout.WEST, videoCanvas, 2, SpringLayout.WEST, c);
        layout.putConstraint(SpringLayout.NORTH, videoCanvas, 2, SpringLayout.NORTH, c);
        layout.putConstraint(SpringLayout.NORTH, consoleBtns, 2, SpringLayout.SOUTH, videoCanvas);
        layout.putConstraint(SpringLayout.EAST, consoleBtns, 2, SpringLayout.EAST, videoCanvas);
        layout.putConstraint(SpringLayout.WEST, consoleBtns, 2, SpringLayout.WEST, c);
        layout.putConstraint(SpringLayout.NORTH, jConsole, 2, SpringLayout.SOUTH, consoleBtns);
        layout.putConstraint(SpringLayout.EAST, jConsole, 2, SpringLayout.EAST, videoCanvas);
        layout.putConstraint(SpringLayout.WEST, jConsole, 2, SpringLayout.WEST, c);
        layout.putConstraint(SpringLayout.WEST, editorLabel, 2, SpringLayout.EAST, videoCanvas);
        layout.putConstraint(SpringLayout.NORTH, editorLabel, 2, SpringLayout.NORTH, c);
        layout.putConstraint(SpringLayout.WEST, caretLabel, -100, SpringLayout.EAST, c);
        layout.putConstraint(SpringLayout.NORTH, caretLabel, 2, SpringLayout.NORTH, c);
        layout.putConstraint(SpringLayout.SOUTH, scrollPane, 2, SpringLayout.NORTH, editorBtns);
        layout.putConstraint(SpringLayout.NORTH, scrollPane, 2, SpringLayout.SOUTH, editorLabel);
        layout.putConstraint(SpringLayout.WEST, scrollPane, 2, SpringLayout.EAST, videoCanvas);
        layout.putConstraint(SpringLayout.EAST, editorBtns, 2, SpringLayout.EAST, c);
        layout.putConstraint(SpringLayout.WEST, editorBtns, 2, SpringLayout.EAST, jConsole);
        layout.putConstraint(SpringLayout.NORTH, editorBtns, 2, SpringLayout.SOUTH, scrollPane);
        layout.putConstraint(SpringLayout.SOUTH, editorBtns, 2, SpringLayout.SOUTH, c);
        layout.putConstraint(SpringLayout.SOUTH, c, 2, SpringLayout.SOUTH, jConsole);
        layout.putConstraint(SpringLayout.EAST, c, 2, SpringLayout.EAST, scrollPane);
        interpreter = new Interpreter(jConsole);
        interpreter.eval("cd(\"" + escapeBackslash(environment.toProjectPath(projectName)) + "\");");
        interpreter.eval("addClassPath(\".\");");
        interpreter.eval("printBanner() {" + "bsh.console.print(\"IENJINIA Virtual Console Dev Kit\", new Font(\"SansSerif\", Font.BOLD, 12), new Color(20, 100, 20));" + "bsh.console.println();" + "bsh.console.print(\"Copyright (C) 2004 Gerardo Horvilleur\", new Font(\"SansSerif\", Font.PLAIN, 12), new Color(20, 100, 20));" + "bsh.console.println();" + "bsh.console.print(\"http://www.ienjinia.com\", new Font(\"SansSerif\", Font.PLAIN, 12), new Color(20, 100, 20));" + "bsh.console.println();}");
        interpreter.eval("getBshPrompt() {return \"dk> \";}");
        interpreter.set("___iavc", iavc);
        interpreter.eval("vbi() {}");
        interpreter.eval("___iavc.setVBI(this)");
        interpreter.eval("sfi() {}");
        interpreter.eval("___iavc.setSFI(this)");
        interpreter.eval("poke(address, value) { ___iavc.store(address, value); }");
        interpreter.eval("peek(address) { return ___iavc.read(address); }");
        interpreter.eval("arrayPoke(a, s, o, n) {___iavc.arrayStore(a, s, o, n);}");
        interpreter.eval("importCommands(\"com/ienjinia/vc/bsh\");");
        interpreter.eval("show();");
        playBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                playBtn.setEnabled(false);
                executeBtn.setEnabled(false);
                videoCanvas.requestFocus();
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            interpreter.eval("vbi() {}");
                            interpreter.eval("sfi() {}");
                            iavc.reset();
                            interpreter.source("main.bsh");
                        } catch (Exception ex) {
                            showError("Play error", ex);
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                playBtn.setEnabled(true);
                                executeBtn.setEnabled(true);
                            }
                        });
                    }
                }).start();
            }
        });
        packageBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    packageProject();
                } catch (Exception ex) {
                    showError("Package error", ex);
                }
            }
        });
        resetIAVCBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    interpreter.eval("vbi() {}");
                    interpreter.eval("sfi() {}");
                    iavc.reset();
                    interpreter.eval("reloadClasses();");
                } catch (Exception ex) {
                }
            }
        });
        seeVBIExceptionBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showError("VBI error", vbiError);
            }
        });
        seeSFIExceptionBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showError("SFI error", sfiError);
            }
        });
        InputMap inputMap = editor.getInputMap();
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        Action newAction = new AbstractAction("New") {

            public void actionPerformed(ActionEvent e) {
                if (modified) {
                    int option = JOptionPane.showConfirmDialog(DevKit.this, "There are unsaved changes in " + editorFileName + ". Discard?", "Unsaved changes!", JOptionPane.YES_NO_OPTION);
                    if (option != JOptionPane.YES_OPTION) return;
                }
                editorFileName = "";
                editor.setText("");
                setModified(false);
            }
        };
        newBtn.setAction(newAction);
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, modifier);
        inputMap.put(key, newAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK);
        inputMap.put(key, newAction);
        Action openAction = new AbstractAction("Open...") {

            public void actionPerformed(ActionEvent e) {
                if (modified) {
                    int option = JOptionPane.showConfirmDialog(DevKit.this, "There are unsaved changes in " + editorFileName + ". Discard?", "Unsaved changes!", JOptionPane.YES_NO_OPTION);
                    if (option != JOptionPane.YES_OPTION) return;
                }
                String newName = Selector.select(environment.getProjectFilenames(projectName, ".bsh"), "Open...", "Select an existing file --> ", "or type the name of a file -->");
                if (newName.length() == 0) return;
                try {
                    loadSource(newName);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(DevKit.this, "Can't open file: " + newName, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        openBtn.setAction(openAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_O, modifier);
        inputMap.put(key, openAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK);
        inputMap.put(key, openAction);
        saveBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    saveSourceAs(editorFileName);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(DevKit.this, "Can't save file: " + editorFileName, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        saveAsBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String newName = Selector.select(environment.getProjectFilenames(projectName, ".bsh"), "Save As...", "Select an existing file --> ", "or type the name of a new file -->");
                if (newName.length() == 0) return;
                try {
                    saveSourceAs(newName);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(DevKit.this, "Can't save file: " + newName, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        Action executeAction = new AbstractAction("Execute") {

            public void actionPerformed(ActionEvent e) {
                final boolean playEnabled = playBtn.isEnabled();
                playBtn.setEnabled(false);
                newBtn.setEnabled(false);
                openBtn.setEnabled(false);
                final boolean saveEnabled = saveBtn.isEnabled();
                saveBtn.setEnabled(false);
                saveAsBtn.setEnabled(false);
                executeBtn.setEnabled(false);
                printBtn.setEnabled(false);
                editor.setEnabled(false);
                videoCanvas.requestFocus();
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            interpreter.eval(editor.getText());
                        } catch (Exception ex) {
                            showError("Execute error", ex);
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                playBtn.setEnabled(playEnabled);
                                newBtn.setEnabled(true);
                                openBtn.setEnabled(true);
                                saveBtn.setEnabled(saveEnabled);
                                saveAsBtn.setEnabled(true);
                                executeBtn.setEnabled(true);
                                printBtn.setEnabled(true);
                                editor.setEnabled(true);
                            }
                        });
                    }
                }).start();
            }
        };
        executeBtn.addActionListener(executeAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_E, modifier);
        inputMap.put(key, executeAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_E, Event.CTRL_MASK);
        inputMap.put(key, executeAction);
        Action printAction = new AbstractAction("Print") {

            public void actionPerformed(ActionEvent e) {
                Properties props = new Properties();
                PrintJob pjob = getToolkit().getPrintJob(DevKit.this, "Prueba de impresion", props);
                if (pjob != null) {
                    Graphics pg = pjob.getGraphics();
                    if (pg != null) {
                        PrintUtil.print(pjob, pg, editor.getText(), "IENJINIA Dev Kit. Project: " + projectName + "   Module: " + editorFileName, "http://www.ienjinia.com");
                        pg.dispose();
                    }
                    pjob.end();
                }
            }
        };
        printBtn.addActionListener(printAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_P, modifier);
        inputMap.put(key, printAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK);
        inputMap.put(key, printAction);
        editor.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }

            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });
        editor.getDocument().addUndoableEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                undo.addEdit(e.getEdit());
            }
        });
        Action undoAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    undo.undo();
                } catch (CannotUndoException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier);
        inputMap.put(key, undoAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
        inputMap.put(key, undoAction);
        Action redoAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    undo.redo();
                } catch (CannotRedoException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, modifier | Event.SHIFT_MASK);
        inputMap.put(key, redoAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK | Event.SHIFT_MASK);
        inputMap.put(key, redoAction);
        Action saveAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (editorFileName != null && editorFileName.length() > 0) {
                    try {
                        saveSourceAs(editorFileName);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(DevKit.this, "Can't save file: " + editorFileName, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    String newName = Selector.select(environment.getProjectFilenames(projectName, ".bsh"), "Save As...", "Select an existing file --> ", "or type the name of a new file -->");
                    if (newName.length() == 0) return;
                    try {
                        saveSourceAs(newName);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(DevKit.this, "Can't save file: " + newName, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        key = KeyStroke.getKeyStroke(KeyEvent.VK_S, modifier);
        inputMap.put(key, saveAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK);
        inputMap.put(key, saveAction);
        Action deleteNextCharAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int pos = editor.getCaretPosition();
                    if (pos != editor.getDocument().getLength() - 1) editor.getDocument().remove(pos, 1);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        };
        key = KeyStroke.getKeyStroke(KeyEvent.VK_D, modifier);
        inputMap.put(key, deleteNextCharAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK);
        inputMap.put(key, deleteNextCharAction);
        Action deleteLineAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int pos = editor.getCaretPosition();
                    Element root = editor.getDocument().getDefaultRootElement();
                    int line = root.getElementIndex(pos);
                    int lineEnd = root.getElement(line).getEndOffset();
                    int len = editor.getDocument().getLength();
                    int offset = lineEnd - pos;
                    if (lineEnd - 1 == len) {
                        Toolkit.getDefaultToolkit().beep();
                    } else if (offset == 1) {
                        editor.getDocument().remove(pos, 1);
                    } else if (lineEnd - 1 < len) {
                        editor.getDocument().remove(pos, offset - 1);
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        };
        key = KeyStroke.getKeyStroke(KeyEvent.VK_K, modifier);
        inputMap.put(key, deleteLineAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK);
        inputMap.put(key, deleteLineAction);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        editor.getKeymap().removeKeyStrokeBinding(key);
        inputMap.put(key, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int pos = editor.getCaretPosition();
                    Element root = editor.getDocument().getDefaultRootElement();
                    int line = root.getElementIndex(pos);
                    int lineStart = root.getElement(line).getStartOffset();
                    int lineEnd = root.getElement(line).getEndOffset();
                    String txt = editor.getDocument().getText(lineStart, lineEnd - lineStart - 1);
                    char[] chars = txt.toCharArray();
                    int offset = pos - lineStart;
                    int i;
                    for (i = 0; i < chars.length; i++) {
                        if (chars[i] != ' ' || i == offset) break;
                    }
                    StringBuffer sb = new StringBuffer();
                    sb.append('\n');
                    for (int j = 0; j < i; j++) {
                        sb.append(' ');
                    }
                    editor.getDocument().insertString(pos, sb.toString(), null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        key = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        editor.getKeymap().removeKeyStrokeBinding(key);
        inputMap.put(key, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int pos = editor.getCaretPosition();
                    Element root = editor.getDocument().getDefaultRootElement();
                    int line = root.getElementIndex(pos);
                    int lineStart = root.getElement(line).getStartOffset();
                    int lineEnd = root.getElement(line).getEndOffset();
                    String txt = editor.getDocument().getText(lineStart, lineEnd - lineStart - 1);
                    char[] chars = txt.toCharArray();
                    int i;
                    for (i = 0; i < chars.length; i++) {
                        if (chars[i] != ' ') break;
                    }
                    int tab = 4 - (i % 4);
                    StringBuffer sb = new StringBuffer();
                    for (int j = 0; j < tab; j++) {
                        sb.append(' ');
                    }
                    editor.getDocument().insertString(lineStart, sb.toString(), null);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        key = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, Event.SHIFT_MASK);
        editor.getKeymap().removeKeyStrokeBinding(key);
        inputMap.put(key, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int pos = editor.getCaretPosition();
                    Element root = editor.getDocument().getDefaultRootElement();
                    int line = root.getElementIndex(pos);
                    int lineStart = root.getElement(line).getStartOffset();
                    int lineEnd = root.getElement(line).getEndOffset();
                    String txt = editor.getDocument().getText(lineStart, lineEnd - lineStart - 1);
                    char[] chars = txt.toCharArray();
                    int i;
                    for (i = 0; i < chars.length; i++) {
                        if (chars[i] != ' ') break;
                    }
                    if (i == 0) return;
                    int tab = i % 4;
                    tab = (tab == 0) ? 4 : tab;
                    editor.getDocument().remove(lineStart, tab);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });
        editor.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                int pos = e.getDot();
                Element root = editor.getDocument().getDefaultRootElement();
                int line = root.getElementIndex(pos);
                int lineStart = root.getElement(line).getStartOffset();
                caretLabel.setText((line + 1) + ":" + (pos - lineStart + 1));
            }
        });
        thread = new Thread(interpreter);
        setSize(1000, 750);
        setVisible(true);
    }

    private void packageProject() throws IOException {
        String filename = environment.toGamePath(projectName);
        FileOutputStream fos = new FileOutputStream(filename);
        ZipOutputStream zos = new ZipOutputStream(fos);
        String[] filenames = environment.getProjectFilenames(projectName);
        for (int i = 0; i < filenames.length; i++) packageFile(zos, filenames[i]);
        zos.finish();
        fos.close();
    }

    private void packageFile(ZipOutputStream zos, String filename) throws IOException {
        String filepath = environment.toProjectFile(projectName, filename);
        File file = new File(filepath);
        byte[] data = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        int bytesRead = 0;
        while (bytesRead < data.length) {
            int n = bis.read(data, bytesRead, data.length - bytesRead);
            bytesRead += n;
        }
        zos.putNextEntry(new ZipEntry(filename));
        zos.write(data, 0, data.length);
        zos.closeEntry();
    }

    private void loadSource(String fileName) throws IOException {
        if (!fileName.endsWith(".bsh")) fileName = fileName + ".bsh";
        editorFileName = fileName;
        editor.setText("");
        Document document = editor.getDocument();
        FileReader in = new FileReader(environment.toProjectFile(projectName, fileName));
        char[] buff = new char[4096];
        int nch;
        try {
            while ((nch = in.read(buff, 0, buff.length)) != -1) document.insertString(document.getLength(), new String(buff, 0, nch), null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        editor.setCaretPosition(0);
        setModified(false);
        undo.discardAllEdits();
    }

    private void saveSourceAs(String fileName) throws IOException {
        if (!fileName.endsWith(".bsh")) fileName = fileName + ".bsh";
        String path = environment.toProjectFile(projectName, fileName);
        if (!fileName.equals(editorFileName)) {
            if (new File(path).exists()) {
                int option = JOptionPane.showConfirmDialog(DevKit.this, "There is alreaddy a file named " + fileName + ". Overwrite?", "Overwrite file?", JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) return;
            }
        }
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(editor.getText().getBytes());
        fos.close();
        editorFileName = fileName;
        setModified(false);
    }

    private void setModified(boolean v) {
        if (v == true) {
            if (modified == false) {
                playBtn.setEnabled(false);
                packageBtn.setEnabled(false);
                if (editorFileName.length() > 0) saveBtn.setEnabled(true);
                editorLabel.setText(editorFileName + " (modified) ");
                modified = true;
            }
        } else {
            if (modified = true) {
                playBtn.setEnabled(true);
                packageBtn.setEnabled(true);
                saveBtn.setEnabled(false);
                editorLabel.setText(editorFileName);
                modified = false;
            }
        }
    }

    public void vbiException(Throwable t) {
        if (t != null) {
            if (vbiError == null) seeVBIExceptionBtn.setEnabled(true);
        } else {
            if (vbiError != null) seeVBIExceptionBtn.setEnabled(false);
        }
        vbiError = t;
    }

    public void sfiException(Throwable t) {
        if (t != null) {
            if (sfiError == null) seeSFIExceptionBtn.setEnabled(true);
        } else {
            if (sfiError != null) seeSFIExceptionBtn.setEnabled(false);
        }
        sfiError = t;
    }

    private String escapeBackslash(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') sb.append(ch);
            sb.append(ch);
        }
        return sb.toString();
    }

    private void showError(String title, Throwable t) {
        JTextArea jta = new JTextArea();
        jta.setColumns(40);
        jta.setEditable(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        Throwable cause = t.getCause();
        jta.setText((cause != null) ? cause.toString() : t.toString());
        JScrollPane jsp = new JScrollPane(jta);
        JOptionPane.showMessageDialog(this, jsp, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws IOException, EvalError {
        Environment env = new Environment();
        env.extractProjects("/demos.zip");
        String projectName = Selector.select(env.getProjectNames(), "IENJINIA DEV KIT", "Select an existing project --> ", "or type the name of a new project -->");
        if (projectName.length() == 0) System.exit(0);
        if (!env.projectExists(projectName)) env.createProject(projectName);
        new DevKit(env, projectName);
    }
}
