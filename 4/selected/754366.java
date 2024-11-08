package com.frinika.project.scripting.gui;

import static com.frinika.localization.CurrentLocale.getMessage;
import com.frinika.gui.AbstractDialog;
import com.frinika.project.gui.ProjectFrame;
import com.frinika.project.scripting.DefaultFrinikaScript;
import com.frinika.project.scripting.FrinikaScript;
import com.frinika.project.scripting.FrinikaScriptingEngine;
import com.frinika.project.scripting.ScriptListener;
import com.frinika.sequencer.gui.menu.ScriptingAction;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;

/**
 * GUI windows for handling scripts. Provides an 'inner desktop' on which 
 * JInternalFrames are displayed.
 *
 * (Created with NetBeans 5.5 gui-editor, see corresponding .form file.)
 *
 * @see com.frinika.project.scripting.FrinikaScriptEngine
 * @see com.frinika.sequencer.gui.menu.ScriptingAction
 * @author Jens Gulden
 */
public class ScriptingDialog extends JDialog implements InternalFrameListener, ScriptListener {

    public static final String INITIAL_JAVASCRIPT = "// add JavaScript here:\n\n";

    private static final String NL = System.getProperty("line.separator");

    private static final String PRESETS_DELIM = "###";

    ScriptEditorInternalFrame activeEditor;

    JMenu scriptingMenu;

    Point addNewPosition = new Point(20, 20);

    ProjectFrame frame;

    FrinikaScriptingEngine engine;

    private Map<String, FrinikaScript> presets;

    /** Creates new form ScriptingDialog */
    public ScriptingDialog(ProjectFrame frame, JMenu scriptingMenu) {
        super(frame, "Frinika " + getMessage(ScriptingAction.actionId), false);
        this.frame = frame;
        this.scriptingMenu = scriptingMenu;
        initComponents();
        consoleTextArea.setEditable(false);
        FileFilter ff = new FileFilter() {

            public boolean accept(File file) {
                return (!file.isFile()) || file.getName().endsWith(".js");
            }

            public String getDescription() {
                return "JavaScript Files (*.js)";
            }
        };
        fileChooser.setFileFilter(ff);
        engine = frame.getProjectContainer().getScriptingEngine();
        setSize(1000, 800);
        Collection<FrinikaScript> scripts = engine.getScripts();
        if (!scripts.isEmpty()) {
            for (FrinikaScript script : scripts) {
                ScriptEditorInternalFrame editor = openEditor(script);
                try {
                    editor.setIcon(true);
                } catch (PropertyVetoException pve) {
                }
            }
        } else {
            newEditor();
        }
        FrinikaScriptingEngine.addScriptListener(this);
        initPresetMenu();
        updateMenus();
        AbstractDialog.centerOnScreen(this);
    }

    public JMenu getScriptingMenu() {
        return scriptingMenu;
    }

    private abstract class SourceActionListener<T> implements ActionListener {

        protected T source;

        SourceActionListener(T source) {
            this.source = source;
        }

        public abstract void actionPerformed(ActionEvent e);
    }

    void updateMenus() {
        JMenuItem selfItem = scriptingMenu.getItem(0);
        scriptingMenu.removeAll();
        scriptingMenu.add(selfItem);
        Collection<FrinikaScript> scripts = frame.getProjectContainer().getScriptingEngine().getScripts();
        if (!scripts.isEmpty()) {
            for (FrinikaScript script : scripts) {
                JMenuItem item = new JMenuItem(new ScriptingAction(frame, script));
                if (script.getSource().equals(INITIAL_JAVASCRIPT)) continue;
                int l = scriptingMenu.getMenuComponentCount();
                if (l == 1) {
                    scriptingMenu.addSeparator();
                }
                scriptingMenu.add(item);
            }
        }
        windowMenu.removeAll();
        ButtonGroup bg = new ButtonGroup();
        JInternalFrame[] frames = desktopPane.getAllFrames();
        int count = 1;
        for (int i = frames.length - 1; i >= 0; i--) {
            ScriptEditorInternalFrame f = (ScriptEditorInternalFrame) frames[i];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(f.getTitle());
            if (count < 10) {
                item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + (count++), java.awt.event.InputEvent.ALT_MASK));
            }
            bg.add(item);
            item.setSelected(f == getActiveEditor());
            item.addActionListener(new SourceActionListener<JInternalFrame>(f) {

                public void actionPerformed(ActionEvent e) {
                    try {
                        source.setIcon(false);
                    } catch (PropertyVetoException pve) {
                    }
                    source.toFront();
                    source.requestFocus();
                }
            });
            windowMenu.add(item);
        }
    }

    ScriptEditorInternalFrame getActiveEditor() {
        return activeEditor;
    }

    FrinikaScript getActiveScript() {
        ScriptEditorInternalFrame editor = getActiveEditor();
        if (editor != null) {
            editor.update();
            return editor.getScript();
        } else {
            return null;
        }
    }

    void newEditor() {
        DefaultFrinikaScript script = new DefaultFrinikaScript();
        script.setLanguage(FrinikaScript.LANGUAGE_JAVASCRIPT);
        script.setSource(INITIAL_JAVASCRIPT);
        ScriptEditorInternalFrame editor = openEditor(script);
        editor.toFront();
        activeEditor = editor;
    }

    void executeScript(FrinikaScript script) {
        engine.executeScript(script, frame, this);
    }

    void stopScript(FrinikaScript script) {
        engine.stopScript(script);
    }

    /**
     * Print to scripting-console (and System.out).
     * @param s
     */
    public void print(String s) {
        consoleTextArea.append(s);
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
    }

    /**
     * Print to scripting-console (and System.out).
     * @param s
     */
    public void println(String s) {
        print(s + NL);
    }

    protected ScriptEditorInternalFrame openEditor(FrinikaScript script) {
        ScriptEditorInternalFrame editor = new ScriptEditorInternalFrame(script, this);
        editor.addInternalFrameListener(this);
        editor.setSize(600, 400);
        editor.setLocation(addNewPosition.x, addNewPosition.y);
        addNewPosition.x += 50;
        addNewPosition.y += 50;
        if (addNewPosition.x > 400) addNewPosition.x = 20;
        if (addNewPosition.y > 300) addNewPosition.y = 20;
        desktopPane.add(editor);
        editor.updateTitle();
        editor.setVisible(true);
        engine.addScript(script);
        updateMenus();
        return editor;
    }

    protected ScriptEditorInternalFrame findByFilename(String filename) {
        JInternalFrame[] f = desktopPane.getAllFrames();
        for (int i = 0; i < f.length; i++) {
            try {
                DefaultFrinikaScript script = (DefaultFrinikaScript) ((ScriptEditorInternalFrame) f[i]).getScript();
                if (script.getFilename().equals(filename)) {
                    return (ScriptEditorInternalFrame) f[i];
                }
            } catch (Throwable t) {
            }
        }
        return null;
    }

    protected ScriptEditorInternalFrame findByScript(FrinikaScript script) {
        JInternalFrame[] f = desktopPane.getAllFrames();
        for (int i = 0; i < f.length; i++) {
            try {
                FrinikaScript sc = ((ScriptEditorInternalFrame) f[i]).getScript();
                if (sc == script) {
                    return (ScriptEditorInternalFrame) f[i];
                }
            } catch (Throwable t) {
            }
        }
        return null;
    }

    public void scriptStarted(FrinikaScript script) {
    }

    public void scriptExited(FrinikaScript script, Object returnValue) {
        if (returnValue == null) {
            println("\nScript " + script.getName() + " exited with an error.\n");
        } else {
            println("Ok.");
        }
    }

    public void internalFrameActivated(InternalFrameEvent e) {
        activeEditor = (ScriptEditorInternalFrame) e.getInternalFrame();
        updateMenus();
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
        if (activeEditor == e.getInternalFrame()) {
            activeEditor = null;
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        ScriptEditorInternalFrame f = (ScriptEditorInternalFrame) e.getInternalFrame();
        FrinikaScript script = f.getScript();
        engine.removeScript(script);
        engine.removeScriptListener(f);
        desktopPane.remove(f);
        desktopPane.validate();
        desktopPane.repaint();
        updateMenus();
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        ScriptEditorInternalFrame f = (ScriptEditorInternalFrame) e.getInternalFrame();
        if (f.hasBeenModifiedWithoutSaving()) {
            if (!frame.confirm("Script has been modified without saving. Close?")) {
                return;
            }
        }
        desktopPane.remove(f);
        f.dispose();
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    private void initComponents() {
        fileChooser = new javax.swing.JFileChooser();
        splitPane = new javax.swing.JSplitPane();
        desktopPane = new javax.swing.JDesktopPane();
        consolePanel = new javax.swing.JPanel();
        consoleScrollPane = new javax.swing.JScrollPane();
        consoleTextArea = new javax.swing.JTextArea();
        consoleButtonPanel = new javax.swing.JPanel();
        clearButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        fileNewMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        fileOpenMenuItem = new javax.swing.JMenuItem();
        fileSaveMenuItem = new javax.swing.JMenuItem();
        fileSaveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        fileCloseMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoMenuItem = new javax.swing.JMenuItem();
        redoMenuItem = new javax.swing.JMenuItem();
        runMenu = new javax.swing.JMenu();
        runExecuteMenuItem = new javax.swing.JMenuItem();
        runStopMenuItem = new javax.swing.JMenuItem();
        windowMenu = new javax.swing.JMenu();
        presetMenu = new javax.swing.JMenu();
        splitPane.setDividerLocation(550);
        splitPane.setDividerSize(8);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        desktopPane.setBackground(new java.awt.Color(204, 204, 255));
        splitPane.setLeftComponent(desktopPane);
        consolePanel.setLayout(new java.awt.BorderLayout());
        consoleTextArea.setColumns(20);
        consoleTextArea.setFont(new java.awt.Font("DialogInput", 0, 12));
        consoleTextArea.setRows(5);
        consoleScrollPane.setViewportView(consoleTextArea);
        consolePanel.add(consoleScrollPane, java.awt.BorderLayout.CENTER);
        consoleButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 1, 1));
        clearButton.setMnemonic('l');
        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        consoleButtonPanel.add(clearButton);
        consolePanel.add(consoleButtonPanel, java.awt.BorderLayout.SOUTH);
        splitPane.setRightComponent(consolePanel);
        getContentPane().add(splitPane, java.awt.BorderLayout.CENTER);
        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileNewMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileNewMenuItem.setMnemonic('N');
        fileNewMenuItem.setText("New");
        fileNewMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNewMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileNewMenuItem);
        fileMenu.add(jSeparator1);
        fileOpenMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileOpenMenuItem.setMnemonic('O');
        fileOpenMenuItem.setText("Open");
        fileOpenMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileOpenMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileOpenMenuItem);
        fileSaveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileSaveMenuItem.setMnemonic('S');
        fileSaveMenuItem.setText("Save");
        fileSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileSaveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileSaveMenuItem);
        fileSaveAsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileSaveAsMenuItem.setMnemonic('A');
        fileSaveAsMenuItem.setText("Save As...");
        fileSaveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileSaveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileSaveAsMenuItem);
        fileMenu.add(jSeparator2);
        fileCloseMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileCloseMenuItem.setMnemonic('C');
        fileCloseMenuItem.setText("Close");
        fileCloseMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileCloseMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(fileCloseMenuItem);
        menuBar.add(fileMenu);
        editMenu.setMnemonic('E');
        editMenu.setText("Edit");
        undoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        undoMenuItem.setMnemonic('U');
        undoMenuItem.setText("Undo");
        undoMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(undoMenuItem);
        redoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        redoMenuItem.setMnemonic('R');
        redoMenuItem.setText("Redo");
        redoMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(redoMenuItem);
        menuBar.add(editMenu);
        runMenu.setMnemonic('R');
        runMenu.setText("Run");
        runExecuteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK));
        runExecuteMenuItem.setMnemonic('E');
        runExecuteMenuItem.setText("Execute");
        runExecuteMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runExecuteMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(runExecuteMenuItem);
        runStopMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.ALT_MASK));
        runStopMenuItem.setMnemonic('S');
        runStopMenuItem.setText("Stop");
        runStopMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runStopMenuItemActionPerformed(evt);
            }
        });
        runMenu.add(runStopMenuItem);
        menuBar.add(runMenu);
        windowMenu.setMnemonic('W');
        windowMenu.setText("Window");
        menuBar.add(windowMenu);
        presetMenu.setMnemonic('P');
        presetMenu.setText("Presets");
        menuBar.add(presetMenu);
        setJMenuBar(menuBar);
        pack();
    }

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {
        consoleTextArea.setText("");
    }

    private void undoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        frame.getProjectContainer().getEditHistoryContainer().getUndoMenuItem().doClick();
    }

    private void redoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        frame.getProjectContainer().getEditHistoryContainer().getRedoMenuItem().doClick();
    }

    private void fileNewMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        newEditor();
    }

    private void fileOpenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        File file = requester(false);
        if (file != null) {
            try {
                String path = file.getCanonicalPath();
                ScriptEditorInternalFrame editor = findByFilename(path);
                if (editor == null) {
                    FrinikaScript script = engine.loadScript(file);
                    editor = openEditor(script);
                    editor.lastSaveTimestamp = file.lastModified();
                } else {
                    editor.toFront();
                }
            } catch (IOException ioe) {
                frame.error(ioe);
            }
        }
    }

    private void fileSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ScriptEditorInternalFrame editor = getActiveEditor();
        if (editor == null) return;
        DefaultFrinikaScript script = (DefaultFrinikaScript) editor.getScript();
        String filename = script.getFilename();
        if (filename != null) {
            try {
                File file = new File(filename);
                engine.saveScript(script, file);
                editor.lastSaveTimestamp = file.lastModified();
                editor.setDirty(false);
            } catch (IOException ioe) {
                frame.error(ioe);
                fileSaveAsMenuItemActionPerformed(evt);
            }
        } else {
            fileSaveAsMenuItemActionPerformed(evt);
        }
    }

    private void fileSaveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ScriptEditorInternalFrame editor = getActiveEditor();
        if (editor == null) return;
        FrinikaScript script = editor.getScript();
        File file = requester(true);
        if (file != null) {
            try {
                if ((!file.exists()) || frame.confirm("File " + file.getCanonicalPath() + " already exists. Overwrite?")) {
                    engine.saveScript(script, file);
                    editor.updateTitle();
                    editor.lastSaveTimestamp = file.lastModified();
                    editor.setDirty(false);
                }
            } catch (IOException ioe) {
                frame.error(ioe);
            }
        }
    }

    private void fileCloseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        hide();
    }

    private void runExecuteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        FrinikaScript script = getActiveScript();
        if (script != null) {
            executeScript(script);
        }
    }

    private void runStopMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        FrinikaScript script = getActiveScript();
        if (script != null) {
            stopScript(script);
        }
    }

    private File requester(boolean save) {
        File f;
        if (!save) {
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                f = fileChooser.getSelectedFile();
            } else {
                f = null;
            }
        } else {
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                f = fileChooser.getSelectedFile();
            } else {
                f = null;
            }
        }
        return f;
    }

    private javax.swing.JButton clearButton;

    private javax.swing.JPanel consoleButtonPanel;

    private javax.swing.JPanel consolePanel;

    private javax.swing.JScrollPane consoleScrollPane;

    private javax.swing.JTextArea consoleTextArea;

    private javax.swing.JDesktopPane desktopPane;

    private javax.swing.JMenu editMenu;

    private javax.swing.JFileChooser fileChooser;

    private javax.swing.JMenuItem fileCloseMenuItem;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JMenuItem fileNewMenuItem;

    private javax.swing.JMenuItem fileOpenMenuItem;

    private javax.swing.JMenuItem fileSaveAsMenuItem;

    private javax.swing.JMenuItem fileSaveMenuItem;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JMenuBar menuBar;

    private javax.swing.JMenu presetMenu;

    private javax.swing.JMenuItem redoMenuItem;

    private javax.swing.JMenuItem runExecuteMenuItem;

    private javax.swing.JMenu runMenu;

    private javax.swing.JMenuItem runStopMenuItem;

    private javax.swing.JSplitPane splitPane;

    private javax.swing.JMenuItem undoMenuItem;

    private javax.swing.JMenu windowMenu;

    private void initPresetMenu() {
        presets = new HashMap<String, FrinikaScript>();
        preset("Hello World", "// Hello World:\n\nprint(\"Hello\");\nprintln(\" World\");\n\n\ns = \"Hello\";\ns += \" World\";\nprintln(s);\n");
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("com/frinika/project/scripting/gui/presets.txt")));
            StringBuffer sb = new StringBuffer();
            String presetName = null;
            String line = r.readLine();
            String doubleDelim = PRESETS_DELIM + PRESETS_DELIM;
            boolean separator = false;
            while (line != null) {
                if (line.indexOf(doubleDelim) != -1) {
                    separator = true;
                } else if (line.startsWith(PRESETS_DELIM)) {
                    if (presetName != null) {
                        preset(presetName, sb.toString());
                    }
                    if (separator) {
                        presetMenu.addSeparator();
                        separator = false;
                    }
                    presetName = line.substring(PRESETS_DELIM.length()).trim();
                    sb = new StringBuffer();
                } else {
                    sb.append(line);
                    sb.append(NL);
                }
                line = r.readLine();
            }
            if (presetName != null) {
                preset(presetName, sb.toString().trim() + NL);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void preset(final String name, final String source) {
        FrinikaScript script = new FrinikaScript() {

            public int getLanguage() {
                return FrinikaScript.LANGUAGE_JAVASCRIPT;
            }

            public String getName() {
                return name;
            }

            public String getSource() {
                return source;
            }
        };
        presets.put(name, script);
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String n = ((JMenuItem) e.getSource()).getText();
                FrinikaScript script = presets.get(n);
                ScriptEditorInternalFrame f = findByScript(script);
                if (f != null) {
                    f.toFront();
                    f.requestFocus();
                } else {
                    openEditor(script);
                }
            }
        });
        presetMenu.add(item);
    }
}
