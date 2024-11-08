package bee.editor;

import bee.app.ViewCanvas;
import bee.core.Core;
import bee.core.EntityBuilder;
import bee.core.EntityGroup;
import bee.core.Log;
import bee.core.TextureAtlasContainer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * Main editor class.
 * @author boto
 */
public class Editor extends javax.swing.JFrame {

    private EditorData ed;

    private StateEntities statePaneEntity;

    private StateTexture statePaneTexture;

    private EditorPane currentPane;

    private LogWindow logWnd;

    private HelpFrame helpFrame;

    private String EDITOR_VERSION = "0.8.1";

    private String EDITOR_TITLE = "BeeEditor v" + EDITOR_VERSION;

    private String ABOUT_TEXT = "Bee Editor\n" + "Version " + EDITOR_VERSION + "\n" + "Developed by funmaker group.\n" + "Visti http://www.vr-fun.net for more details.    ";

    /**
    * Create the editor.
    */
    public void Editor() {
    }

    /**
     * Show up the editor.
     */
    public void start() {
        Core.get().setDebugMode(true);
        Log.addSink(System.out);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            Log.error("Could not load LookAndFeel: " + e);
        }
        logWnd = new LogWindow(this);
        initComponents();
        ed.undoManager = new UndoRedoManager(ed, jMenuItemUndo, jMenuItemRedo);
        setIconImage(ed.getAppIcon());
        logWnd.setIconImage(ed.getAppIcon());
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point p = ge.getCenterPoint();
        Dimension d = getSize();
        p.x -= d.width / 2;
        if (p.x < 0) p.x = 0;
        p.y -= d.height / 2;
        if (p.y < 0) p.y = 0;
        setLocation(p);
        jTabbedPaneMain.setEnabled(false);
        canvas.iniComponents();
        canvas.setEnabled(false);
        ed.currentFile = "";
        StateEntities stateEntities = new StateEntities(ed);
        statePaneEntity = stateEntities;
        Core.get().setAppState(statePaneEntity);
        jPaneEntities.onLoad();
        stateEntities.initialize(ed.currentFile);
        canvas.setBackground(ed.entitiesPaneBkgColor);
        jPaneTextures.onLoad();
        statePaneTexture = jPaneTextures.getAppState();
        statePaneTexture.initialize();
        appLoop();
        if (ed.openLogWindowOnStart) logWnd.setVisible(true);
        if (ed.openLastFileOnStart && !ed.lastFile.isEmpty()) {
            File file = new File(ed.lastFile);
            ed.currentFile = file.getAbsolutePath();
            jPaneEntities.onClose();
            jPaneTextures.onClose();
            Core.get().setAppState(statePaneEntity);
            statePaneEntity.initialize(ed.currentFile);
            statePaneTexture = jPaneTextures.getAppState();
            statePaneTexture.initialize();
            jTabbedPaneMain.setSelectedIndex(0);
            jPaneEntities.onLoad();
            jPaneTextures.onLoad();
            jTabbedPaneMain.setEnabled(true);
            canvas.setEnabled(true);
            setTitle(EDITOR_TITLE + " - " + ed.currentFile);
        }
        setVisible(true);
    }

    private ViewCanvas canvas;

    private javax.swing.JTabbedPane jTabbedPaneMain;

    private PaneEntities jPaneEntities;

    private PaneTextures jPaneTextures;

    private javax.swing.JPanel jPanelView;

    private javax.swing.JToolBar jToolbar;

    private javax.swing.JMenuBar jMenuBar;

    private javax.swing.JMenu jMenuFile;

    private javax.swing.JMenu jMenuEdit;

    private javax.swing.JMenuItem jMenuItemOptions;

    private javax.swing.JMenuItem jMenuItemUndo;

    private javax.swing.JMenuItem jMenuItemRedo;

    private javax.swing.JMenu jMenuView;

    private javax.swing.JCheckBoxMenuItem jCBMenuItemLogWnd;

    private javax.swing.JMenu jMenuHelp;

    private javax.swing.JMenuItem jMenuItemAbout;

    private javax.swing.JMenuItem jMenuItemHelp;

    private javax.swing.JMenuItem jMenuItemClose;

    private javax.swing.JMenuItem jMenuItemNew;

    private javax.swing.JMenuItem jMenuItemOpen;

    private javax.swing.JMenuItem jMenuItemQuit;

    private javax.swing.JMenuItem jMenuItemSave;

    private javax.swing.JMenuItem jMenuItemSaveAs;

    private javax.swing.JLabel jLabelStatusbar;

    private javax.swing.JToggleButton jButtonStats;

    private javax.swing.JToggleButton jButtonCam;

    @SuppressWarnings("unchecked")
    private void initComponents() {
        canvas = new ViewCanvas();
        ed = new EditorData(this, canvas);
        ed.loadEditorConfig();
        jTabbedPaneMain = new javax.swing.JTabbedPane();
        jPaneEntities = new PaneEntities(ed);
        jPaneTextures = new PaneTextures(ed);
        jPanelView = new javax.swing.JPanel();
        jLabelStatusbar = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemNew = new javax.swing.JMenuItem();
        jMenuItemOpen = new javax.swing.JMenuItem();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jMenuItemClose = new javax.swing.JMenuItem();
        jMenuItemQuit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuItemUndo = new javax.swing.JMenuItem();
        jMenuItemRedo = new javax.swing.JMenuItem();
        jMenuItemOptions = new javax.swing.JMenuItem();
        jMenuView = new javax.swing.JMenu();
        jCBMenuItemLogWnd = new javax.swing.JCheckBoxMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemAbout = new javax.swing.JMenuItem();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jToolbar = new javax.swing.JToolBar();
        jButtonStats = new JToggleButton();
        jButtonCam = new JToggleButton();
        ed.paneEntities = jPaneEntities;
        ed.paneTextures = jPaneTextures;
        currentPane = jPaneEntities;
        setTitle(EDITOR_TITLE);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(800, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        canvas.addKeyListener(new java.awt.event.KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasKeyTyped(evt);
                }
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasKeyPressed(evt);
                }
            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasKeyReleased(evt);
                }
            }
        });
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasMouseClick(evt);
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasMousePressed(evt);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasMouseReleased(evt);
                }
            }
        });
        canvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasMouseDragged(evt);
                }
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                if (currentPane != null) {
                    currentPane.eventCanvasMouseMoved(evt);
                }
            }
        });
        canvas.addComponentListener(new java.awt.event.ComponentListener() {

            @Override
            public void componentHidden(java.awt.event.ComponentEvent e) {
            }

            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
            }

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (currentPane != null) {
                    currentPane.eventCanvasResized(e);
                }
            }

            public void componentShown(java.awt.event.ComponentEvent e) {
            }
        });
        jToolbar.setFloatable(false);
        jToolbar.setRollover(true);
        setupToolbar(jToolbar);
        jTabbedPaneMain.setPreferredSize(new java.awt.Dimension(260, 500));
        jTabbedPaneMain.setBackground(new java.awt.Color(255, 255, 255));
        jTabbedPaneMain.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPaneMainStateChanged(evt);
            }
        });
        jPanelView.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanelView.setAlignmentY(0.0F);
        javax.swing.GroupLayout jPanelViewLayout = new javax.swing.GroupLayout(jPanelView);
        jPanelView.setLayout(jPanelViewLayout);
        jPanelViewLayout.setHorizontalGroup(jPanelViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(canvas, javax.swing.GroupLayout.PREFERRED_SIZE, 500, Short.MAX_VALUE));
        jPanelViewLayout.setVerticalGroup(jPanelViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(canvas, javax.swing.GroupLayout.PREFERRED_SIZE, 490, Short.MAX_VALUE));
        jTabbedPaneMain.addTab("Entities", jPaneEntities);
        jTabbedPaneMain.addTab("Textures", jPaneTextures);
        jLabelStatusbar.setText("Ready");
        jMenuFile.setText("File");
        jMenuItemNew.setText("New ...");
        jMenuItemNew.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNewActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemNew);
        jMenuItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemOpen.setText("Open ...");
        jMenuItemOpen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpen);
        jMenuItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemSave.setText("Save");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSave);
        jMenuItemSaveAs.setText("Save As ...");
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAs);
        jMenuItemClose.setText("Close");
        jMenuItemClose.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCloseActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemClose);
        jMenuFile.addSeparator();
        jMenuItemQuit.setText("Quit");
        jMenuItemQuit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemQuitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemQuit);
        jMenuBar.add(jMenuFile);
        jMenuEdit.setText("Edit");
        jMenuItemUndo.setText("Undo");
        jMenuItemUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemUndo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUndoActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemUndo);
        jMenuItemRedo.setText("Redo");
        jMenuItemRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItemRedo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRedoActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemRedo);
        jMenuEdit.addSeparator();
        jMenuItemOptions.setText("Options ...");
        jMenuItemOptions.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOptionsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemOptions);
        jMenuBar.add(jMenuEdit);
        jMenuView.setText("View");
        jMenuBar.add(jMenuView);
        jCBMenuItemLogWnd.setSelected(true);
        jCBMenuItemLogWnd.setText("Log Window");
        jCBMenuItemLogWnd.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxMenuItemLogWndItemStateChanged(evt);
            }
        });
        jMenuView.add(jCBMenuItemLogWnd);
        jMenuHelp.setText("Help");
        jMenuHelp.add(jMenuItemHelp);
        jMenuItemHelp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuHelp.add(jMenuItemAbout);
        jMenuBar.add(jMenuHelp);
        jMenuItemHelp.setText("Help");
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpActionPerformed(evt);
            }
        });
        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        setJMenuBar(jMenuBar);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTabbedPaneMain, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPanelView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addGap(8, 8, 8).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(layout.createSequentialGroup().addComponent(jPanelView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGroup(layout.createSequentialGroup().addComponent(jToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jTabbedPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE))).addGap(8, 8, 8)));
        pack();
    }

    private void setupToolbar(javax.swing.JToolBar toolbar) {
        JButton btnopen = new JButton();
        btnopen.setText("Open");
        btnopen.setToolTipText("Open new file");
        btnopen.setFocusable(false);
        btnopen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnopen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnopen.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        jMenuItemOpenActionPerformed(null);
                    }
                });
            }
        });
        toolbar.add(btnopen);
        JButton btnsave = new JButton();
        btnsave.setText("Save");
        btnsave.setToolTipText("Save");
        btnsave.setFocusable(false);
        btnsave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnsave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnsave.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        jMenuItemSaveActionPerformed(null);
                    }
                });
            }
        });
        toolbar.add(btnsave);
        JButton btntile = new JButton();
        btntile.setText("Tile");
        btntile.setToolTipText("Open tile picker");
        btntile.setFocusable(false);
        btntile.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btntile.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btntile.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        jPaneTextures.showTilePicker(true);
                    }
                });
            }
        });
        toolbar.add(btntile);
        jButtonStats.setText("Stats");
        jButtonStats.setToolTipText("Display scene statistics");
        jButtonStats.setFocusable(false);
        jButtonStats.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonStats.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonStats.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (statePaneEntity != null) {
                            boolean en = statePaneEntity.getSceneManager().getEnableStats();
                            statePaneEntity.getSceneManager().setEnableStats(!en);
                        }
                    }
                });
            }
        });
        toolbar.add(jButtonStats);
        jButtonCam.setSelected(true);
        jButtonCam.setText("Cam");
        jButtonCam.setToolTipText("Enable/disable camera control");
        jButtonCam.setFocusable(false);
        jButtonCam.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonCam.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonCam.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Boolean en = ed.camera.getParamValue("processInput");
                            ed.camera.setParamValue("processInput", !en);
                        } catch (Exception e) {
                        }
                    }
                });
            }
        });
        toolbar.add(jButtonCam);
    }

    protected PaneEntities getPaneEntities() {
        return jPaneEntities;
    }

    private void appLoop() {
        new Thread(new Thread() {

            @Override
            public void run() {
                loop();
            }
        }).start();
    }

    private void loop() {
        final float MAX_DELTA_TIME = 1.0f;
        final float MIN_DELTA_TIME = 0.0001f;
        final float UPDATE_PERIOD = 1.0f / 60.0f;
        long currtime = System.currentTimeMillis();
        long lasttime = currtime;
        float deltatime = 0.0f;
        long elapsedtime = 0;
        while (!Core.get().isTerminated()) {
            currtime = System.currentTimeMillis();
            deltatime = (currtime - lasttime) / 1000.0f;
            if (deltatime > MAX_DELTA_TIME) {
                deltatime = MAX_DELTA_TIME;
            } else if (deltatime < MIN_DELTA_TIME) {
                deltatime = MIN_DELTA_TIME;
            }
            try {
                Core.get().getAppState().update(deltatime);
            } catch (Exception e) {
                Log.error("Editor: error occured on updating entities.\n reason: " + e);
                Log.error("Editor: stack trace:");
                e.printStackTrace();
            }
            try {
                java.awt.EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        Graphics g = canvas.beginDrawing();
                        Core.get().getAppState().draw(g);
                        canvas.endDrawing();
                    }
                });
            } catch (Exception e) {
                Log.error("Editor: error occured during drawing\n reason: " + e);
                e.printStackTrace();
            }
            elapsedtime = System.currentTimeMillis() - currtime;
            final long minperiod = (long) (1000.0f * UPDATE_PERIOD);
            if (elapsedtime < minperiod) {
                try {
                    Thread.sleep(minperiod - elapsedtime - 1);
                } catch (Exception e) {
                }
            }
            lasttime = currtime;
        }
        Log.debug("shutting down main loop");
        bee.gui.GUI.shutdown();
        Core.shutdown();
        setVisible(false);
        dispose();
    }

    private void jMenuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {
        if (!ed.currentFile.isEmpty()) {
            int res = JOptionPane.showConfirmDialog(ed.mainFrame, "Save changes before closing?", "Attention", JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                ed.saveFile(new String(ed.currentFile));
            } else if (res == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        ed.setEntityGroup(null);
        jPaneEntities.onClose();
        jPaneTextures.onClose();
        jPaneEntities.onQuit();
        jPaneTextures.onQuit();
        logWnd.setVisible(false);
        logWnd.dispose();
        Core.get().terminate();
    }

    private void jMenuItemNewActionPerformed(java.awt.event.ActionEvent evt) {
        if (!ed.currentFile.isEmpty()) {
            JOptionPane.showMessageDialog(ed.mainFrame, "First, close the running level.", "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a file name");
        fc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(ed.LEVEL_FILE_EXTENSION) || f.isDirectory();
            }

            public String getDescription() {
                return "Level File (*" + ed.LEVEL_FILE_EXTENSION + ")";
            }
        });
        int ret = fc.showOpenDialog(ed.mainFrame);
        if (ret != JFileChooser.CANCEL_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getAbsolutePath();
            if (!filename.endsWith(ed.LEVEL_FILE_EXTENSION)) {
                filename += ed.LEVEL_FILE_EXTENSION;
            }
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(ed.mainFrame, "File aready exists. Overwrite it?", "Attention", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            try {
                EntityBuilder eb = ed.getEntityBuilder();
                EntityGroup grp = new EntityGroup();
                eb.saveEntities(filename, grp);
                TextureAtlasContainer ac = new TextureAtlasContainer();
                ac.saveAtlases(filename + ed.ATLAS_FILE_EXTENSION);
            } catch (Exception e) {
                Log.error("cannot create file " + filename);
                return;
            }
            jButtonCam.setSelected(true);
            jButtonStats.setSelected(false);
            if (statePaneEntity != null) statePaneEntity.clear();
            if (statePaneTexture != null) statePaneTexture.clear();
            ed.currentFile = filename;
            Core.get().setAppState(statePaneEntity);
            statePaneEntity.initialize(ed.currentFile);
            statePaneTexture = jPaneTextures.getAppState();
            statePaneTexture.initialize();
            jPaneEntities.onLoad();
            jPaneTextures.onLoad();
            jTabbedPaneMain.setEnabled(true);
            canvas.setEnabled(true);
            ed.lastFile = new String(ed.currentFile);
            ed.saveEditorConfig();
            setTitle(EDITOR_TITLE + " - " + ed.currentFile);
            ed.undoManager.clear();
        }
    }

    private void jMenuItemOpenActionPerformed(java.awt.event.ActionEvent evt) {
        if (!ed.currentFile.isEmpty()) {
            int ret = JOptionPane.showConfirmDialog(ed.mainFrame, "Do you want to save current file?", "Attention", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                ed.saveFile(new String(ed.currentFile));
            }
        }
        String path = "";
        if (!ed.lastFile.isEmpty()) {
            File f = new File(ed.lastFile);
            path = ed.lastFile.substring(0, ed.lastFile.length() - f.getName().length());
        }
        JFileChooser fc = new JFileChooser(path);
        fc.setDialogTitle("Select a file to open");
        fc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(ed.LEVEL_FILE_EXTENSION) || f.isDirectory();
            }

            public String getDescription() {
                return "Level File (*" + ed.LEVEL_FILE_EXTENSION + ")";
            }
        });
        int ret = fc.showOpenDialog(ed.mainFrame);
        if (ret != JFileChooser.CANCEL_OPTION) {
            ed.setEntityGroup(null);
            if (statePaneEntity != null) statePaneEntity.clear();
            if (statePaneTexture != null) statePaneTexture.clear();
            File file = fc.getSelectedFile();
            ed.currentFile = file.getAbsolutePath();
            jButtonCam.setSelected(true);
            jButtonStats.setSelected(false);
            jPaneEntities.onClose();
            jPaneTextures.onClose();
            Core.get().setAppState(statePaneEntity);
            statePaneEntity.initialize(ed.currentFile);
            statePaneTexture = jPaneTextures.getAppState();
            statePaneTexture.initialize();
            jTabbedPaneMain.setSelectedIndex(0);
            jPaneEntities.onLoad();
            jPaneTextures.onLoad();
            jTabbedPaneMain.setEnabled(true);
            canvas.setEnabled(true);
            ed.lastFile = new String(ed.currentFile);
            ed.saveEditorConfig();
            setTitle(EDITOR_TITLE + " - " + ed.currentFile);
            ed.undoManager.clear();
        }
    }

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {
        if (!ed.currentFile.isEmpty()) {
            jPaneEntities.onSave(ed.currentFile);
            jPaneTextures.onSave(ed.currentFile);
            Log.debug("saving level");
            ed.saveFile(new String(ed.currentFile));
        }
    }

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        if (ed.currentFile.isEmpty()) {
            return;
        }
        String path = "";
        if (!ed.lastFile.isEmpty()) {
            File f = new File(ed.lastFile);
            path = ed.lastFile.substring(0, ed.lastFile.length() - f.getName().length());
        }
        JFileChooser fc = new JFileChooser(path);
        fc.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(ed.LEVEL_FILE_EXTENSION) || f.isDirectory();
            }

            public String getDescription() {
                return "Level File (*" + ed.LEVEL_FILE_EXTENSION + ")";
            }
        });
        int ret = fc.showOpenDialog(ed.mainFrame);
        if (ret != JFileChooser.CANCEL_OPTION) {
            File file = fc.getSelectedFile();
            String filename = file.getAbsolutePath();
            if (!filename.endsWith(ed.LEVEL_FILE_EXTENSION)) {
                filename += ed.LEVEL_FILE_EXTENSION;
            }
            ed.saveFile(filename);
            ed.currentFile = filename;
            jPaneEntities.onSave(ed.currentFile);
            jPaneTextures.onSave(ed.currentFile);
            setTitle(EDITOR_TITLE + " - " + ed.currentFile);
        }
    }

    private void jMenuItemOptionsActionPerformed(java.awt.event.ActionEvent evt) {
        DlgOptions dlg = new DlgOptions(this, ed);
        dlg.setVisible(true);
        if (currentPane == jPaneEntities) canvas.setClearColor(ed.entitiesPaneBkgColor); else if (currentPane == jPaneTextures) canvas.setClearColor(ed.texturesPaneBkgColor);
    }

    private void jMenuItemUndoActionPerformed(java.awt.event.ActionEvent evt) {
        ed.lockData();
        ed.undoManager.undo();
        ed.unlockData();
    }

    private void jMenuItemRedoActionPerformed(java.awt.event.ActionEvent evt) {
        ed.lockData();
        ed.undoManager.redo();
        ed.unlockData();
    }

    private void jMenuItemCloseActionPerformed(java.awt.event.ActionEvent evt) {
        if (ed.currentFile.isEmpty()) {
            jPaneEntities.onClose();
            jPaneTextures.onClose();
            return;
        }
        if (JOptionPane.showConfirmDialog(ed.mainFrame, "Save changes before closing?", "Attention", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ed.saveFile(new String(ed.currentFile));
        }
        jButtonCam.setSelected(true);
        jButtonStats.setSelected(false);
        ed.setEntityGroup(null);
        ed.currentFile = new String("");
        canvas.setIgnoreRepaint(false);
        canvas.repaint();
        canvas.setEnabled(false);
        statePaneEntity.clear();
        statePaneTexture.clear();
        jPaneEntities.onClose();
        jPaneTextures.onClose();
        jTabbedPaneMain.setSelectedIndex(0);
        jTabbedPaneMain.setEnabled(false);
        setTitle(EDITOR_TITLE);
        ed.undoManager.clear();
    }

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(ed.mainFrame, ABOUT_TEXT, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {
        if (helpFrame == null) helpFrame = new HelpFrame(ed);
        helpFrame.setVisible(true);
    }

    /**
     * Hide/show log window.
     */
    public void setLogWndVisible(boolean vis) {
        jCBMenuItemLogWnd.setSelected(vis);
    }

    private void jCheckBoxMenuItemLogWndItemStateChanged(java.awt.event.ItemEvent evt) {
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) logWnd.setVisible(true); else logWnd.setVisible(false);
    }

    /**
     * Called when the close button clicked.
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        if (!ed.currentFile.isEmpty()) {
            int res = JOptionPane.showConfirmDialog(ed.mainFrame, "Save changes before closing?", "Attention", JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                ed.saveFile(new String(ed.currentFile));
            } else if (res == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        jPaneEntities.onClose();
        jPaneTextures.onClose();
        jPaneEntities.onQuit();
        jPaneTextures.onQuit();
        Core.get().terminate();
        logWnd.setVisible(false);
        logWnd.dispose();
        if (helpFrame != null) {
            helpFrame.dispose();
            helpFrame.setVisible(false);
        }
    }

    private void jTabbedPaneMainStateChanged(javax.swing.event.ChangeEvent evt) {
        int sel = jTabbedPaneMain.getSelectedIndex();
        if (sel == 0) {
            Core.get().setAppState(statePaneEntity);
            jPaneEntities.adaptViewSize();
            currentPane = jPaneEntities;
            canvas.setClearColor(ed.entitiesPaneBkgColor);
            if (ed.undoManager != null) ed.undoManager.updateItems();
        } else if (sel == 1) {
            Core.get().setAppState(statePaneTexture);
            currentPane = jPaneTextures;
            canvas.setClearColor(ed.texturesPaneBkgColor);
            if (ed.undoManager != null) {
                jMenuItemUndo.setText("Undo");
                jMenuItemUndo.setEnabled(false);
                jMenuItemRedo.setText("Redo");
                jMenuItemRedo.setEnabled(false);
            }
        }
    }
}
