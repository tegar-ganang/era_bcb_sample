package com.frinika.project.gui;

import static com.frinika.localization.CurrentLocale.getMessage;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.state.DockingState;
import org.flexdock.view.View;
import org.flexdock.view.Viewport;
import uk.org.toot.swingui.audioui.mixerui.CompactMixerPanel;
import com.frinika.About;
import com.frinika.FrinikaMain;
import com.frinika.project.FrinikaAudioSystem;
import com.frinika.tootX.gui.FrinikaMixerPanel;
import com.frinika.tootX.gui.MidiLearnPanel;
import com.frinika.global.ConfigError;
import com.frinika.global.ConfigListener;
import com.frinika.global.FrinikaConfig;
import com.frinika.tootX.midi.MidiInDeviceManager;
import com.frinika.mod.MODImporter;
import com.frinika.notation.NotationPanel;
import com.frinika.radio.LocalOGGHttpRadio;
import com.frinika.project.ProjectContainer;
import com.frinika.project.mididevices.gui.MidiDevicesPanel;
import com.frinika.sequencer.FrinikaSequencer;
import com.frinika.sequencer.gui.GlobalToolBar;
import com.frinika.sequencer.gui.menu.CreateAudioLaneAction;
import com.frinika.sequencer.gui.menu.CreateMidiDrumLaneAction;
import com.frinika.sequencer.gui.menu.CreateMidiLaneAction;
import com.frinika.sequencer.gui.menu.CreateProjectAction;
import com.frinika.sequencer.gui.menu.CreateTextLaneAction;
import com.frinika.sequencer.gui.menu.CutPasteMenu;
import com.frinika.sequencer.gui.menu.DisplayStructureAction;
import com.frinika.sequencer.gui.menu.GuiCursorUpdateMenu;
import com.frinika.sequencer.gui.menu.ImportAudioAction;
import com.frinika.sequencer.gui.menu.ImportMidiToLaneAction;
import com.frinika.sequencer.gui.menu.MidiIMonitorAction;
import com.frinika.sequencer.gui.menu.MidiInSetupAction;
import com.frinika.sequencer.gui.menu.RepeatAction;
import com.frinika.sequencer.gui.menu.SetAudioOutputAction;
import com.frinika.sequencer.gui.menu.SplitSelectedPartsAction;
import com.frinika.sequencer.gui.menu.TempoListEditAction;
import com.frinika.sequencer.gui.menu.TimeSignatureEditAction;
import com.frinika.sequencer.gui.menu.ToggleShowVoiceViewAction;
import com.frinika.sequencer.gui.menu.WarpToLeftAction;
import com.frinika.sequencer.gui.menu.WarpToRightAction;
import com.frinika.sequencer.gui.menu.midi.GroovePatternCreateFromMidiPartAction;
import com.frinika.sequencer.gui.menu.midi.GroovePatternManagerAction;
import com.frinika.sequencer.gui.menu.midi.MidiDurationAction;
import com.frinika.sequencer.gui.menu.midi.MidiInsertControllersAction;
import com.frinika.sequencer.gui.menu.midi.MidiQuantizeAction;
import com.frinika.sequencer.gui.menu.midi.MidiReverseAction;
import com.frinika.sequencer.gui.menu.midi.MidiShiftAction;
import com.frinika.sequencer.gui.menu.midi.MidiStepRecordAction;
import com.frinika.sequencer.gui.menu.midi.MidiTimeStretchAction;
import com.frinika.sequencer.gui.menu.midi.MidiTransposeAction;
import com.frinika.sequencer.gui.menu.midi.MidiVelocityAction;
import com.frinika.sequencer.gui.mixer.SynthWrapper;
import com.frinika.sequencer.gui.partview.VoicePartViewSplitPane;
import com.frinika.sequencer.gui.pianoroll.PianoControllerSplitPane;
import com.frinika.sequencer.gui.tracker.TrackerPanel;
import com.frinika.sequencer.gui.transport.RecordAction;
import com.frinika.sequencer.gui.transport.RewindAction;
import com.frinika.sequencer.gui.transport.StartStopAction;
import com.frinika.sequencer.model.AudioLane;
import com.frinika.sequencer.model.EditHistoryAction;
import com.frinika.sequencer.model.EditHistoryListener;
import com.frinika.sequencer.model.Lane;
import com.frinika.sequencer.model.Part;
import com.frinika.tools.BufferedPlayback;
import com.frinika.tracker.MidiFileFilter;
import com.frinika.tracker.ProjectFileFilter;
import com.frinika.tracker.filedialogs.BounceToLane;
import com.frinika.tracker.filedialogs.ExportWavDialog;
import com.frinika.tootX.midi.MidiLearnIF;
import com.frinika.versionmanagement.AutomaticVersionCheck;
import java.awt.event.ItemListener;
import javax.swing.event.AncestorListener;

/**
 * Frinika is designed to have one basis frame per project. A projectframe is
 * the main window for a project.
 * 
 * @author Peter Johan Salomonsen
 */
public class ProjectFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /**
	 * File-filter for midi standard files. To be used with promptFile().
	 * 
	 * @see promptFile
	 */
    public static final String[][] FILE_FILTER_MIDIFILES = new String[][] { { "mid", "Midi standard files" } };

    /**
	 * Vector containing currently open project frames
	 */
    private static Vector<ProjectFrame> openProjectFrames = new Vector<ProjectFrame>();

    private static ProjectFrame focusFrame = null;

    private static Vector<ProjectFocusListener> projectFocusListeners = new Vector<ProjectFocusListener>();

    public static boolean doNotQuit = false;

    public static ProjectFrame findFrame(ProjectContainer project) {
        for (ProjectFrame pr : openProjectFrames) {
            if (pr.project == project) return pr;
        }
        return null;
    }

    TrackerPanel trackerPanel;

    PianoControllerSplitPane pianoControllerPane;

    NotationPanel notationPanel;

    VoicePartViewSplitPane partViewEditor;

    GlobalToolBar globalToolBar;

    ProjectContainer project;

    private File midiFile;

    private StartStopAction startStop;

    private RewindAction rewind;

    private WarpToLeftAction warpToLeft;

    private WarpToRightAction warpToRight;

    private Viewport viewport;

    private RecordAction record;

    private Rectangle position;

    private MidiLearnPanel midiLearnFrame;

    private boolean withRef = false;

    public JPopupMenu newLaneMenu = new JPopupMenu();

    private StatusBar statusBar;

    public ProjectFrame(final ProjectContainer project) throws Exception {
        this(project, null);
    }

    public ProjectFrame(final ProjectContainer project, Rectangle position) throws Exception {
        this.position = position;
        project.resetEndTick();
        final Receiver receiver = project.getSequencer().getReceiver();
        midiLearnFrame = new MidiLearnPanel(project.getMidiDeviceRouter());
        MidiInDeviceManager.setProject(project);
        addWindowListener(new WindowListener() {

            public void windowOpened(WindowEvent arg0) {
            }

            public void windowClosing(WindowEvent evt) {
                if (openProjectFrames.size() == 1 && !doNotQuit) tryQuit(); else {
                    if (canClose()) evt.getWindow().dispose();
                }
            }

            public void windowClosed(WindowEvent arg0) {
            }

            public void windowIconified(WindowEvent arg0) {
            }

            public void windowDeiconified(WindowEvent arg0) {
            }

            public void windowActivated(WindowEvent arg0) {
                ProjectFrame.setFocusFrame(ProjectFrame.this);
                ProjectFrame.notifyProjectFocusListeners();
                MidiInDeviceManager.setProject(project);
            }

            public void windowDeactivated(WindowEvent arg0) {
            }
        });
        if (false) addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
                info();
            }

            public void componentResized(ComponentEvent e) {
                info();
            }

            public void componentShown(ComponentEvent e) {
            }

            void info() {
                GraphicsConfiguration gc = ProjectFrame.this.getGraphicsConfiguration();
                System.out.println(gc);
                System.out.println(ProjectFrame.this.getBounds());
            }
        });
        this.project = project;
        Dimension frameSize;
        int x;
        int y;
        if (position == null) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Rectangle windowSize;
            Insets windowInsets;
            GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
            if (gc == null) gc = getGraphicsConfiguration();
            if (gc != null) {
                windowSize = gc.getBounds();
                windowInsets = toolkit.getScreenInsets(gc);
            } else {
                windowSize = new java.awt.Rectangle(toolkit.getScreenSize());
                windowInsets = new java.awt.Insets(0, 0, 0, 0);
            }
            windowSize.x = 0;
            int w = (windowSize.width - windowSize.x) - (windowInsets.left + windowInsets.right + 10);
            int h = (windowSize.height - windowSize.y) - (windowInsets.top + windowInsets.bottom + 10);
            y = windowInsets.top + ((windowSize.height - windowSize.y) - (windowInsets.top + windowInsets.bottom) - h) / 2;
            x = windowInsets.left + ((windowSize.width - windowSize.x) - (windowInsets.left + windowInsets.right) - w) / 2;
            frameSize = new Dimension(w, h);
            setSize(new Dimension(w - 1, h - 1));
        } else {
            frameSize = position.getSize();
            y = position.y;
            x = position.x;
        }
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setIconImage(new javax.swing.ImageIcon(ProjectFrame.class.getResource("/icons/frinika.png")).getImage());
        String name = "Frinika - Copyright (c) Frinika developers - Licensed under GNU General Public License";
        File file = project.getProjectFile();
        if (file != null) name = file.getName();
        setTitle(name);
        project.getEditHistoryContainer().addEditHistoryListener(new EditHistoryListener() {

            /**
					 * An asterix sign next to the project title indicating if
					 * edits have been done
					 */
            boolean showAsterix = false;

            public void fireSequenceDataChanged(EditHistoryAction[] edithistoryEntries) {
                if (project.getEditHistoryContainer().hasChanges() != showAsterix) {
                    showAsterix = project.getEditHistoryContainer().hasChanges();
                    if (showAsterix) setTitle(getTitle() + "*"); else setTitle(getTitle().substring(0, getTitle().length() - 1));
                }
            }
        });
        if (System.getProperty("frinika.window.flexdock") == null) {
            DockingManager.setFloatingEnabled(true);
            viewport = new Viewport();
        }
        createMenu();
        globalToolBar = new GlobalToolBar(this);
        JPanel content = new JPanel(new BorderLayout());
        setContentPane(content);
        content.add(globalToolBar, BorderLayout.NORTH);
        if (viewport == null) {
            JTabbedPane midPanels = new JTabbedPane();
            content.add(midPanels, BorderLayout.CENTER);
            midPanels.addTab(getMessage("project.maintabs.tracks"), createSplit(frameSize));
            midPanels.addTab(getMessage("project.maintabs.midiout_devices"), new JScrollPane(midiDevicesPanel = new MidiDevicesPanel(this)));
            midPanels.addTab(getMessage("project.maintabs.audiomixer"), new FrinikaMixerPanel(project.getMixerControls(), midiLearnFrame));
        } else {
            content.add(viewport, BorderLayout.CENTER);
            initViews();
            perspectivePreset1();
        }
        statusBar = new StatusBar();
        content.add(statusBar, BorderLayout.SOUTH);
        validate();
        setVisible(true);
        setSize(frameSize);
        setLocation(x, y);
        openProjectFrames.add(this);
        overRideKeys();
        if (FrinikaConfig.MAXIMIZE_WINDOW) {
            this.setExtendedState(MAXIMIZED_BOTH);
        }
    }

    private static Icon maximize_icon = new javax.swing.ImageIcon(ProjectFrame.class.getResource("/icons/maximize.gif"));

    private static Icon minimize_icon = new javax.swing.ImageIcon(ProjectFrame.class.getResource("/icons/minimize.gif"));

    public MidiLearnIF getMidiLearnIF() {
        return midiLearnFrame;
    }

    public void resetViews() {
        Iterator iter = views.values().iterator();
        while (iter.hasNext()) {
            View view = (View) iter.next();
            if (view.isMinimized()) DockingManager.setMinimized((Dockable) view, false);
            DockingManager.undock((Dockable) view);
        }
        viewport.grabFocus();
    }

    public void repaintViews() {
        partViewEditor.repaint();
        repaintPartView();
    }

    public void repaintPartView() {
        partViewEditor.getPartview().repaint();
        partViewEditor.getPartview().repaintItems();
    }

    JMenu perspectivemenu;

    JMenu perspective_showmenu;

    JMenu perspectiveMenu() {
        JMenu menu = perspectivemenu = new JMenu(getMessage("project.menu.perspectives"));
        menu.setMnemonic(KeyEvent.VK_P);
        JMenuItem item = new JMenuItem(getMessage("project.menu.perspectives.Default_Perspective"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                perspectivePreset1();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(item);
        item = new JMenuItem(getMessage("project.menu.perspectives.Mastering_Perspective"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                perspectivePreset2();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(item);
        item = new JMenuItem(getMessage("project.menu.perspectives.Minimal_Perspective"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                perspectivePreset3();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(item);
        menu.addSeparator();
        perspective_showmenu = new JMenu("Show View");
        menu.add(perspective_showmenu);
        return menu;
    }

    private class LateRunnable implements Runnable {

        Runnable runnable;

        public LateRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        public void run() {
            try {
                SwingUtilities.invokeAndWait(runnable);
                Thread.sleep(200);
                SwingUtilities.invokeAndWait(runnable);
                Thread.sleep(200);
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runReallyLater(Runnable runnable) {
        new Thread(new LateRunnable(runnable)).start();
    }

    public void perspectivePreset1() {
        resetViews();
        viewport.dock(getView("tracks"));
        getView("tracks").dock(getView("pianoroll"), DockingConstants.SOUTH_REGION);
        getView("tracks").dock(getView("voice"), DockingConstants.WEST_REGION);
        getView("pianoroll").dock(getView("tracker"));
        getView("pianoroll").dock(getView("notation"));
        getView("tracks").dock(getView("midiout"));
        getView("tracks").dock(getView("mixer"));
        ((JSplitPane) getView("voice").getParent().getParent()).setDividerLocation(300);
        ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
        ((JTabbedPane) getView("pianoroll").getParent()).setSelectedIndex(0);
        viewport.grabFocus();
        validate();
        repaint();
        Runnable runnable = new Runnable() {

            public void run() {
                ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
                ((JTabbedPane) getView("pianoroll").getParent()).setSelectedIndex(0);
                viewport.grabFocus();
            }
        };
        runReallyLater(runnable);
    }

    public void perspectivePreset2() {
        resetViews();
        viewport.dock(getView("tracks"));
        getView("tracks").dock(getView("mixer"), DockingConstants.EAST_REGION, 0.25f);
        getView("tracks").dock(getView("pianoroll"), DockingConstants.SOUTH_REGION);
        getView("tracks").dock(getView("voice"), DockingConstants.WEST_REGION, 0.3f);
        getView("pianoroll").dock(getView("tracker"));
        getView("pianoroll").dock(getView("notation"));
        getView("tracks").dock(getView("midiout"));
        ((JSplitPane) getView("voice").getParent().getParent()).setDividerLocation(300);
        ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
        ((JTabbedPane) getView("pianoroll").getParent()).setSelectedIndex(0);
        viewport.grabFocus();
        validate();
        repaint();
        Runnable runnable = new Runnable() {

            public void run() {
                ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
                ((JTabbedPane) getView("pianoroll").getParent()).setSelectedIndex(0);
                viewport.grabFocus();
            }
        };
        runReallyLater(runnable);
    }

    public void perspectivePreset3() {
        resetViews();
        viewport.dock(getView("tracks"));
        getView("tracks").dock(getView("voice"), DockingConstants.WEST_REGION);
        getView("tracks").dock(getView("pianoroll"));
        getView("tracks").dock(getView("tracker"));
        getView("tracks").dock(getView("notation"));
        getView("tracks").dock(getView("midiout"));
        getView("tracks").dock(getView("mixer"));
        ((JSplitPane) getView("voice").getParent().getParent()).setDividerLocation(300);
        ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
        DockingManager.setMinimized(getView("voice"), true, DockingState.LEFT);
        viewport.grabFocus();
        validate();
        repaint();
        Runnable runnable = new Runnable() {

            public void run() {
                ((JTabbedPane) getView("tracks").getParent()).setSelectedIndex(0);
                viewport.grabFocus();
            }
        };
        runReallyLater(runnable);
    }

    public void initViews() {
        partViewEditor = new VoicePartViewSplitPane(this, true);
        JComponent partViewEditor = this.partViewEditor.getPartViewEditor();
        JComponent laneView = this.partViewEditor.getLaneView();
        notationPanel = new NotationPanel(this);
        pianoControllerPane = new PianoControllerSplitPane(this);
        trackerPanel = new TrackerPanel(project.getSequence(), this);
        trackerPanel.setOpaque(false);
        JPanel mixer = new JPanel();
        mixer.setLayout(new BorderLayout());
        try {
            mixer.add(new FrinikaMixerPanel(project.getMixerControls(), midiLearnFrame));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        project.getPartSelection().addSelectionListener(trackerPanel);
        laneView.setPreferredSize(new Dimension(0, 0));
        partViewEditor.setPreferredSize(new Dimension(0, 0));
        pianoControllerPane.setPreferredSize(new Dimension(0, 0));
        pianoControllerPane.setOpaque(false);
        trackerPanel.setPreferredSize(new Dimension(0, 0));
        mixer.setPreferredSize(new Dimension(0, 0));
        laneView.setMinimumSize(new Dimension(0, 0));
        partViewEditor.setMinimumSize(new Dimension(0, 0));
        pianoControllerPane.setMinimumSize(new Dimension(0, 0));
        trackerPanel.setMinimumSize(new Dimension(0, 0));
        mixer.setMinimumSize(new Dimension(0, 0));
        createView("tracks", getMessage("project.maintabs.tracks"), partViewEditor, dockicon_tracks);
        createView("voice", getMessage("project.maintabs.lane_properties"), laneView, dockicon_voice);
        createView("pianoroll", getMessage("project.maintabs.piano_roll"), pianoControllerPane, dockicon_pianoroll);
        createView("tracker", getMessage("project.maintabs.tracker"), trackerPanel, dockicon_tracker);
        createView("notation", getMessage("project.maintabs.notation"), notationPanel, dockicon_notation);
        createView("midiout", getMessage("project.maintabs.midimixer"), new JScrollPane(midiDevicesPanel = new MidiDevicesPanel(this)), dockicon_midiout);
        createView("mixer", getMessage("project.maintabs.audiomixer"), mixer, dockicon_mixer);
    }

    public static Icon getIconResource(String name) {
        try {
            Icon icon = new javax.swing.ImageIcon(ProjectFrame.class.getResource("/icons/" + name));
            return icon;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Icon dockicon_tracks = getIconResource("track.gif");

    private static Icon dockicon_tracker = getIconResource("tracker.gif");

    private static Icon dockicon_midiout = getIconResource("midi_mixer.gif");

    private static Icon dockicon_voice = getIconResource("properties.gif");

    private static Icon dockicon_pianoroll = getIconResource("piano.png");

    private static Icon dockicon_mixer = getIconResource("mixer.gif");

    private static Icon dockicon_notation = getIconResource("midilane.png");

    private static Icon default_midi_icon = getIconResource("midi.png");

    public static Icon getMidiDeviceIcon(MidiDevice dev) {
        Icon icon = getMidiDeviceLargeIcon(dev);
        if (icon.getIconHeight() > 16 || icon.getIconWidth() > 16) {
            BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = img.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            Image im = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            icon = new ImageIcon(im);
        }
        return icon;
    }

    public static Icon getMidiDeviceLargeIcon(MidiDevice dev) {
        if (dev instanceof SynthWrapper) {
            dev = ((SynthWrapper) dev).getRealDevice();
        }
        Icon icon = default_midi_icon;
        try {
            Method icon_method = dev.getClass().getMethod("getIcon");
            icon = (Icon) icon_method.invoke(dev);
        } catch (Exception e) {
        }
        return icon;
    }

    private Map views = new HashMap();

    private View getView(String id) {
        return (View) views.get(id);
    }

    private View createView(String id, String text, JComponent content, Icon icon) {
        final View view = new View(id, text);
        if (icon != null) view.setIcon(icon);
        if (icon != null) view.setTabIcon(icon);
        views.put(id, view);
        JMenuItem menuitem = new JMenuItem(text);
        menuitem.setIcon(icon);
        menuitem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                DockingManager.undock((Dockable) view);
                Set dockables = viewport.getDockables();
                if (dockables.size() == 0) viewport.dock(view); else ((View) dockables.iterator().next()).dock(view);
                validate();
                repaint();
            }
        });
        perspective_showmenu.add(menuitem);
        Action max_action = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public Object getValue(String key) {
                if (key.equals("Name")) return "maximize";
                if (key.equals("ShortDescription")) return "Maximize";
                if (key.equals("SmallIcon")) return maximize_icon;
                return super.getValue(key);
            }

            public void actionPerformed(ActionEvent e) {
                DockingManager.toggleMaximized((Dockable) view);
                focusFrame.repaint();
                DockingManager.display((Dockable) view);
            }
        };
        view.addAction(View.CLOSE_ACTION);
        view.getTitlebar().addAction(max_action);
        view.addAction(View.PIN_ACTION);
        view.getTitlebar().addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DockingManager.toggleMaximized((Dockable) view);
                    ProjectFrame.this.repaint();
                    DockingManager.display((Dockable) view);
                    return;
                }
                super.mouseClicked(e);
            }
        });
        view.setContentPane(content);
        return view;
    }

    protected static void setFocusFrame(ProjectFrame frame) {
        if (frame != focusFrame) {
            focusFrame = frame;
            System.out.println(" Setting focus project " + frame.getProjectContainer().getFile());
        }
    }

    public static ProjectFrame getFocusFrame() {
        return focusFrame;
    }

    /**
	 * Grab some keys before the focus manager gets them !!!! TODO discuss this.
	 * Maybe better to have a focus panes and grab the keyboard for each of
	 * these ?
	 */
    void overRideKeys() {
        startStop = new StartStopAction(this);
        rewind = new RewindAction(this);
        record = new RecordAction(this);
        warpToLeft = new WarpToLeftAction(this);
        warpToRight = new WarpToRightAction(this);
        final SelectAllAction selectAllAction = new SelectAllAction(this);
        KeyboardFocusManager kbm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kbm.addKeyEventDispatcher(new KeyEventDispatcher() {

            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!ProjectFrame.this.isActive()) return false;
                if (e.getSource() instanceof JTextComponent) return false;
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            startStop.actionPerformed(null);
                        }
                        return true;
                    case KeyEvent.VK_HOME:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            rewind.actionPerformed(null);
                        }
                        return true;
                    case KeyEvent.VK_MULTIPLY:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            record.actionPerformed(null);
                        }
                        return true;
                    case KeyEvent.VK_NUMPAD1:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            warpToLeft.actionPerformed(null);
                        }
                        return true;
                    case KeyEvent.VK_NUMPAD2:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            warpToRight.actionPerformed(null);
                        }
                        return true;
                    case KeyEvent.VK_A:
                        if ((e.getID() == KeyEvent.KEY_PRESSED)) {
                            if (e.isControlDown()) {
                                return selectAllAction.selectAll(e);
                            }
                        }
                    default:
                        return false;
                }
            }
        });
    }

    public static void openLocalProject() {
    }

    JMenu fileMenu() {
        JMenu fileMenu = new JMenu(getMessage("project.menu.file"));
        final JMenuItem newProjectMenuItem = new JMenuItem(getMessage("project.menu.file.new_project"));
        newProjectMenuItem.setIcon(getIconResource("new.gif"));
        newProjectMenuItem.addActionListener(new CreateProjectAction());
        fileMenu.add(newProjectMenuItem);
        final JMenuItem openProjectMenuItem = new JMenuItem(getMessage("project.menu.file.open_project"));
        openProjectMenuItem.setIcon(getIconResource("open.gif"));
        openProjectMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(getMessage("project.menu.file.open_project.dialogtitle"));
                    chooser.setFileFilter(new ProjectNewFileFilter());
                    if (project.getProjectFile() != null) chooser.setSelectedFile(project.getProjectFile());
                    if (chooser.showOpenDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION) {
                        File newProject = chooser.getSelectedFile();
                        new ProjectFrame(ProjectContainer.loadProject(newProject), position);
                    }
                    ;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.add(openProjectMenuItem);
        final JMenuItem closeMenuItem = new JMenuItem(getMessage("close"));
        closeMenuItem.setMnemonic(KeyEvent.VK_C);
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        closeMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (openProjectFrames.size() == 1) tryQuit(); else if (canClose()) dispose();
            }
        });
        fileMenu.add(closeMenuItem);
        fileMenu.add(new JSeparator());
        final JMenuItem saveProjectMenuItem = new JMenuItem(getMessage("project.menu.file.save_project"));
        saveProjectMenuItem.setIcon(getIconResource("save.gif"));
        saveProjectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveProjectMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    if (project.getProjectFile() != null) {
                        project.saveProject(project.getProjectFile());
                        FrinikaConfig.setLastProjectFilename(project.getProjectFile().getAbsolutePath());
                    } else {
                        withRef = false;
                        openSaveProjectDialog();
                    }
                } catch (Exception ex) {
                    error("Error while saving", ex);
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.add(saveProjectMenuItem);
        final JMenuItem saveProjectAsMenuItem = new JMenuItem(getMessage("project.menu.file.save_project_as"));
        saveProjectAsMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                withRef = false;
                openSaveProjectDialog();
            }
        });
        fileMenu.add(saveProjectAsMenuItem);
        final JMenuItem saveProjectMenuItemWithRef = new JMenuItem(getMessage("project.menu.file.save_project_with_ref"));
        saveProjectMenuItemWithRef.setIcon(getIconResource("save.gif"));
        saveProjectMenuItemWithRef.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    if (project.getProjectFile() != null) {
                        project.saveProject(project.getProjectFile());
                        FrinikaConfig.setLastProjectFilename(project.getProjectFile().getAbsolutePath());
                    } else {
                        withRef = true;
                        openSaveProjectDialog();
                    }
                } catch (Exception ex) {
                    error("Error while saving", ex);
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.add(saveProjectMenuItemWithRef);
        final JMenuItem saveProjectAsMenuItemWithRef = new JMenuItem(getMessage("project.menu.file.save_project_as_with_ref"));
        saveProjectAsMenuItemWithRef.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                withRef = true;
                openSaveProjectDialog();
            }
        });
        fileMenu.add(saveProjectAsMenuItemWithRef);
        fileMenu.addSeparator();
        final JMenuItem importMidiMenuItem = new JMenuItem(getMessage("project.menu.file.import_midi"));
        importMidiMenuItem.setIcon(getIconResource("import.gif"));
        importMidiMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(getMessage("project.menu.file.import_midi"));
                    chooser.setFileFilter(new MidiFileFilter());
                    if (midiFile != null) chooser.setSelectedFile(midiFile);
                    if (chooser.showOpenDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION) {
                        File newMidiFile = chooser.getSelectedFile();
                        MidiDevice mididdevice = selectMidiDevice();
                        ProjectFrame frame = new ProjectFrame(new ProjectContainer(MidiSystem.getSequence(newMidiFile), mididdevice));
                        midiFile = newMidiFile;
                    }
                    ;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.add(importMidiMenuItem);
        JMenuItem item = new JMenuItem("Import Module...");
        item.setIcon(getIconResource("import.gif"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                MODImporter.load(ProjectFrame.this);
            }
        });
        fileMenu.add(item);
        final JMenuItem exportMidiMenuItem = new JMenuItem(getMessage("project.menu.file.export_midi"));
        exportMidiMenuItem.setIcon(getIconResource("export.gif"));
        exportMidiMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle(getMessage("project.menu.file.export_midi"));
                    chooser.setFileFilter(new MidiFileFilter());
                    chooser.setSelectedFile(midiFile);
                    if (chooser.showSaveDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION) {
                        if (chooser.getFileFilter() instanceof MidiFileFilter) if (!chooser.getFileFilter().accept(chooser.getSelectedFile())) chooser.setSelectedFile(new File(chooser.getSelectedFile().getPath() + ".mid"));
                        File newMidiFile = chooser.getSelectedFile();
                        MidiSystem.write(project.getSequence().export(), 1, newMidiFile);
                        midiFile = newMidiFile;
                    }
                    ;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.addSeparator();
        fileMenu.add(exportMidiMenuItem);
        final JMenuItem exportWavMenuItem = new JMenuItem(getMessage("project.menu.file.export_wav"));
        exportWavMenuItem.setIcon(getIconResource("export.gif"));
        exportWavMenuItem.addActionListener(new ActionListener() {

            class AudioFileFilter extends FileFilter {

                Type type;

                AudioFileFilter(Type type) {
                    this.type = type;
                }

                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    return f.getName().toLowerCase().endsWith("." + type.getExtension().toLowerCase());
                }

                public String getDescription() {
                    return type.toString() + " (*." + type.getExtension() + ")";
                }

                public Type getType() {
                    return type;
                }
            }

            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Export Audio");
                    byte[] data = new byte[16];
                    AudioFormat format = new AudioFormat((float) FrinikaConfig.sampleRate, 16, 2, true, true);
                    AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format, 4);
                    ;
                    Type[] types = AudioSystem.getAudioFileTypes(ais);
                    AudioFileFilter[] filters = new AudioFileFilter[types.length];
                    for (int i = 0; i < types.length; i++) filters[i] = new AudioFileFilter(types[i]);
                    for (int i = 0; i < filters.length; i++) chooser.setFileFilter(filters[i]);
                    for (int i = 0; i < filters.length; i++) if (filters[i].getType().getExtension().toUpperCase().equals("WAV")) chooser.setFileFilter(filters[i]);
                    if (chooser.showSaveDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION) {
                        if (chooser.getFileFilter() instanceof AudioFileFilter) if (!chooser.getFileFilter().accept(chooser.getSelectedFile())) chooser.setSelectedFile(new File(chooser.getSelectedFile().getPath() + "." + ((AudioFileFilter) chooser.getFileFilter()).getType().getExtension().toLowerCase()));
                        FrinikaSequencer sequencer = project.getSequencer();
                        long startTick = sequencer.getLoopStartPoint();
                        long endTick = sequencer.getLoopEndPoint();
                        Type type = ((AudioFileFilter) chooser.getFileFilter()).getType();
                        new ExportWavDialog(ProjectFrame.this, project, type, chooser.getSelectedFile(), startTick, endTick);
                        project.getSequencer().stop();
                    }
                    ;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        fileMenu.add(exportWavMenuItem);
        if (!FrinikaMain.isMac()) {
            fileMenu.addSeparator();
            final JMenuItem quitMenuItem = new JMenuItem(getMessage("quit"));
            quitMenuItem.setMnemonic(KeyEvent.VK_Q);
            quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            quitMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    tryQuit();
                }
            });
            fileMenu.add(quitMenuItem);
        }
        return fileMenu;
    }

    JMenu debugMenu() {
        JMenu debugMenu = new JMenu("debug");
        JMenuItem item = new JMenuItem(new DisplayStructureAction(this));
        debugMenu.add(item);
        item = new JMenuItem("Buffered Playback");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                BufferedPlayback bp = new BufferedPlayback(ProjectFrame.this, getProjectContainer());
                bp.start();
            }
        });
        debugMenu.add(item);
        item = new JMenuItem(new MidiIMonitorAction());
        debugMenu.add(item);
        if (false) {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            try {
                Class clazz = loader.loadClass("uk.co.simphoney.music.Plug");
                Class params[] = { ProjectFrame.class };
                Constructor plug = clazz.getConstructor(params);
                Object args[] = { this };
                plug.newInstance(args);
            } catch (ClassNotFoundException e1) {
                System.out.println(" info: Unable to load Simphoney Menus. Your probably using java < 1.6 ?");
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        item = new JMenuItem("ConfigListener");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ConfigListener l = new ConfigListener() {

                    public void configurationChanged(ChangeEvent event) {
                        FrinikaConfig.Meta meta = (FrinikaConfig.Meta) event.getSource();
                        try {
                            System.out.println("configuration changed: " + meta.getField().getName() + "=" + meta.getField().get(null));
                        } catch (IllegalAccessException iae) {
                            iae.printStackTrace();
                        }
                    }
                };
                FrinikaConfig.addConfigListener(l);
            }
        });
        debugMenu.add(item);
        return debugMenu;
    }

    JMenu renderMenu() {
        JMenu menu = new JMenu(getMessage("project.menu.render"));
        menu.setMnemonic(KeyEvent.VK_R);
        JMenuItem item = new JMenuItem("Render Selected Timeline");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (project.getSequencer().isRunning()) {
                    project.getSequencer().stop();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (project.getSequencer().getLoopStartPoint() == project.getSequencer().getLoopEndPoint()) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "Please select timeline using loop markers!", "Render Selected Timeline", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                project.getRenderer().render(ProjectFrame.this, project.getSequencer().getLoopStartPoint(), project.getSequencer().getLoopEndPoint());
            }
        });
        menu.add(item);
        item = new JMenuItem("Rerender Selected Timeline");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (project.getSequencer().isRunning()) {
                    project.getSequencer().stop();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (project.getSequencer().getLoopStartPoint() == project.getSequencer().getLoopEndPoint()) {
                    JOptionPane.showMessageDialog(ProjectFrame.this, "Please select timeline using loop markers!", "Rerender Selected Timeline", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                project.getRenderer().rerender(ProjectFrame.this, project.getSequencer().getLoopStartPoint(), project.getSequencer().getLoopEndPoint());
            }
        });
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Clear Render Cache");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (project.getSequencer().isRunning()) {
                    project.getSequencer().stop();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                project.getRenderer().purgeRenderCache();
            }
        });
        menu.add(item);
        menu.addSeparator();
        final JCheckBoxMenuItem item_supress_realtime = new JCheckBoxMenuItem("Suppress RealTime Devices while playing");
        item_supress_realtime.setSelected(true);
        item_supress_realtime.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                project.getRenderer().setSupressRealTime(item_supress_realtime.isSelected());
            }
        });
        menu.add(item_supress_realtime);
        return menu;
    }

    private ActionListener addMidiDevices_listener = null;

    private MidiDevice addMidiDevices_selected = null;

    private Icon addMidiDevices_selected_icon = null;

    public void addMidiDevices(JComponent menu, List<MidiDevice.Info> infos, List<Icon> icons) {
        Iterator<Icon> icon_iterator = icons.iterator();
        for (MidiDevice.Info info : infos) {
            JMenuItem item = new JMenuItem(info.toString());
            item.setIcon(icon_iterator.next());
            final Icon icon = item.getIcon();
            final MidiDevice.Info f_info = info;
            item.addActionListener(new ActionListener() {

                MidiDevice.Info m_info = f_info;

                public void actionPerformed(ActionEvent e) {
                    MidiDevice midiDevice = null;
                    try {
                        midiDevice = MidiSystem.getMidiDevice(m_info);
                        if (addMidiDevices_listener != null) {
                            addMidiDevices_selected = midiDevice;
                            addMidiDevices_selected_icon = icon;
                            addMidiDevices_listener.actionPerformed(null);
                            return;
                        }
                        midiDevice = new SynthWrapper(project, midiDevice);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    try {
                        project.getEditHistoryContainer().mark("X");
                        project.addMidiOutDevice(midiDevice);
                        project.getEditHistoryContainer().notifyEditHistoryListeners();
                    } catch (MidiUnavailableException e2) {
                        e2.printStackTrace();
                    }
                    midiDevicesPanel.updateDeviceTabs();
                }
            });
            menu.add(item);
        }
    }

    public void addMidiDevices(JComponent menu) {
        List<MidiDevice.Info> infos = new ArrayList<MidiDevice.Info>();
        List<Icon> icons = new ArrayList<Icon>();
        List<MidiDevice.Info> infos1 = new ArrayList<MidiDevice.Info>();
        List<Icon> icons1 = new ArrayList<Icon>();
        List<MidiDevice.Info> infos2 = new ArrayList<MidiDevice.Info>();
        List<Icon> icons2 = new ArrayList<Icon>();
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice dev = MidiSystem.getMidiDevice(info);
                if (dev.getMaxReceivers() != 0) {
                    Icon icon = getMidiDeviceIcon(dev);
                    if (dev instanceof Synthesizer) {
                        infos1.add(info);
                        icons1.add(icon);
                    } else if (icon == default_midi_icon) {
                        infos2.add(info);
                        icons2.add(icon);
                    } else {
                        infos.add(info);
                        icons.add(icon);
                    }
                }
            } catch (Exception e) {
            }
        }
        addMidiDevices(menu, infos1, icons1);
        menu.add(new JSeparator());
        addMidiDevices(menu, infos, icons);
        menu.add(new JSeparator());
        addMidiDevices(menu, infos2, icons2);
    }

    public MidiDevice selectMidiDevice() {
        try {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("MIDI Out Device:"));
            final JButton but = new JButton("No Device Selected");
            but.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JPopupMenu popup = new JPopupMenu();
                    addMidiDevices(popup);
                    popup.show(but, 0, 0);
                }
            });
            addMidiDevices_selected = null;
            addMidiDevices_listener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    but.setText(addMidiDevices_selected.getDeviceInfo().getName());
                    but.setIcon(addMidiDevices_selected_icon);
                }
            };
            panel.add(but);
            int opt = JOptionPane.showConfirmDialog(this, panel, "Select MIDI Output", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            addMidiDevices_listener = null;
            if (opt == JOptionPane.OK_OPTION) return addMidiDevices_selected;
        } finally {
            addMidiDevices_listener = null;
        }
        return null;
    }

    JMenu newDeviceMenu() {
        final JMenu newDeviceMenu = new JMenu(getMessage("newdevice.menu"));
        newDeviceMenu.addMenuListener(new MenuListener() {

            public void menuSelected(MenuEvent e) {
                newDeviceMenu.removeAll();
                addMidiDevices(newDeviceMenu);
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });
        return newDeviceMenu;
    }

    JMenu editMenu() {
        JMenu editMenu = new CutPasteMenu(project);
        editMenu.add(new JSeparator());
        JMenuItem item = new JMenuItem(new CreateMidiLaneAction(this));
        item.setIcon(getIconResource("new_track_midi.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        editMenu.add(item);
        item = new JMenuItem(new CreateMidiDrumLaneAction(this));
        item.setIcon(getIconResource("new_track_midi_drum.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        editMenu.add(item);
        item = new JMenuItem(new CreateAudioLaneAction(this));
        item.setIcon(getIconResource("new_track_audio.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        editMenu.add(item);
        item = new JMenuItem(new CreateTextLaneAction(this));
        item.setIcon(getIconResource("new_track_text.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        editMenu.add(item);
        editMenu.addSeparator();
        editMenu.add(newDeviceMenu());
        item = new JMenuItem(new CreateMidiLaneAction(this));
        item.setIcon(getIconResource("new_track_midi.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        newLaneMenu.add(item);
        item = new JMenuItem(new CreateMidiDrumLaneAction(this));
        item.setIcon(getIconResource("new_track_midi_drum.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        newLaneMenu.add(item);
        item = new JMenuItem(new CreateAudioLaneAction(this));
        item.setIcon(getIconResource("new_track_audio.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        newLaneMenu.add(item);
        item = new JMenuItem(new CreateTextLaneAction(this));
        item.setIcon(getIconResource("new_track_text.gif"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        newLaneMenu.add(item);
        newLaneMenu.addSeparator();
        newLaneMenu.add(newDeviceMenu());
        editMenu.addSeparator();
        final JMenuItem bounceToLane = new JMenuItem(getMessage("project.menu.file.bounce_to_lane"));
        bounceToLane.setIcon(getIconResource("new_track_audio.gif"));
        bounceToLane.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Lane lane = project.getLaneSelection().getFocus();
                project.getEditHistoryContainer().mark(getMessage("sequencer.audiolane.record"));
                if (lane == null || !(lane instanceof AudioLane)) {
                    lane = project.createAudioLane();
                }
                File file = ((AudioLane) lane).newFilename();
                FrinikaSequencer sequencer = project.getSequencer();
                long startTick = sequencer.getLoopStartPoint();
                long endTick = sequencer.getLoopEndPoint();
                new BounceToLane(ProjectFrame.this, project, file, startTick, endTick, (AudioLane) lane);
                project.getSequencer().stop();
            }
        });
        editMenu.add(bounceToLane);
        item = new JMenuItem(new ImportAudioAction(this));
        item.setIcon(getIconResource("import.gif"));
        editMenu.add(item);
        item = new JMenuItem(new ImportMidiToLaneAction(this));
        item.setIcon(getIconResource("import.gif"));
        editMenu.add(item);
        editMenu.add(new JSeparator());
        item = new JMenuItem(new RepeatAction(this));
        item.setMnemonic(KeyEvent.VK_R);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        editMenu.add(item);
        editMenu.add(new JMenuItem(new SplitSelectedPartsAction(this)));
        editMenu.add(new JSeparator());
        editMenu.add(new TimeSignatureEditAction(this));
        editMenu.add(new TempoListEditAction(this));
        return editMenu;
    }

    JMenu settingsMenu() {
        JMenu settingsMenu = new JMenu(getMessage("project.menu.settings"));
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        JMenuItem item;
        item = new JMenuItem(new MidiInSetupAction());
        item.setIcon(getIconResource("midi.png"));
        settingsMenu.add(item);
        settingsMenu.addSeparator();
        item = new JMenuItem(new SetAudioOutputAction(this));
        settingsMenu.add(item);
        item.setIcon(getIconResource("output.gif"));
        item = new CheckBoxMenuItemConfig(getMessage("project.menu.settings.direct_monitoring"), FrinikaConfig._DIRECT_MONITORING);
        settingsMenu.add(item);
        item = new CheckBoxMenuItemConfig(getMessage("project.menu.settings.multiplexed_audio"), FrinikaConfig._MULTIPLEXED_AUDIO);
        settingsMenu.add(item);
        item = new JMenuItem(getMessage("project.menu.settings.audio_latency"));
        item.setIcon(getIconResource("output.gif"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FrinikaAudioSystem.latencyMeasureSet();
            }
        });
        settingsMenu.add(item);
        settingsMenu.add(item);
        item = new JMenuItem(getMessage("project.menu.settings.configure_audio_server"));
        item.setIcon(getIconResource("output.gif"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FrinikaAudioSystem.configure();
            }
        });
        settingsMenu.add(item);
        settingsMenu.addSeparator();
        item = new CheckBoxMenuItemConfig(getMessage("project.menu.settings.jack_autoconnect"), FrinikaConfig._JACK_AUTO_CONNECT);
        settingsMenu.add(item);
        settingsMenu.addSeparator();
        settingsMenu.add(new GuiCursorUpdateMenu(project));
        settingsMenu.addSeparator();
        item = new JMenuItem(getMessage("project.menu.settings.show"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FrinikaConfig.showDialog(ProjectFrame.this);
            }
        });
        settingsMenu.add(item);
        return settingsMenu;
    }

    JMenu toolsMenu() {
        JMenu toolsMenu = new JMenu(getMessage("project.menu.tools"));
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        JMenuItem item = new JMenuItem(new MidiQuantizeAction(this));
        item.setMnemonic(KeyEvent.VK_Q);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiTransposeAction(this));
        item.setMnemonic(KeyEvent.VK_T);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiVelocityAction(this));
        item.setMnemonic(KeyEvent.VK_V);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiShiftAction(this));
        item.setMnemonic(KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiDurationAction(this));
        item.setMnemonic(KeyEvent.VK_D);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiTimeStretchAction(this));
        item.setMnemonic(KeyEvent.VK_M);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiReverseAction(this));
        item.setMnemonic(KeyEvent.VK_E);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        item = new JMenuItem(new MidiInsertControllersAction(this));
        item.setMnemonic(KeyEvent.VK_I);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0));
        toolsMenu.add(item);
        project.getMidiSelection().addMenuItem(item);
        toolsMenu.addSeparator();
        item = new JMenuItem(new MidiStepRecordAction(this));
        item.setText(item.getText() + "...");
        item.setMnemonic(KeyEvent.VK_INSERT);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
        toolsMenu.add(item);
        toolsMenu.addSeparator();
        JMenu submenu = new JMenu(getMessage("sequencer.midi.groovepattern"));
        item = new JMenuItem(new GroovePatternManagerAction(this));
        item.setText(item.getText() + "...");
        item.setMnemonic(KeyEvent.VK_M);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        submenu.add(item);
        item = new JMenuItem(new GroovePatternCreateFromMidiPartAction(this));
        item.setMnemonic(KeyEvent.VK_C);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_MASK));
        submenu.add(item);
        toolsMenu.add(submenu);
        toolsMenu.addSeparator();
        submenu = new JMenu(getMessage(com.frinika.sequencer.gui.menu.ScriptingAction.actionId));
        com.frinika.sequencer.gui.menu.ScriptingAction sa = new com.frinika.sequencer.gui.menu.ScriptingAction(this);
        item = new JMenuItem(sa);
        item.setText(item.getText() + "...");
        item.setMnemonic(KeyEvent.VK_G);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        submenu.add(item);
        sa.initDialog(submenu);
        toolsMenu.add(submenu);
        return toolsMenu;
    }

    JMenu helpMenu() {
        JMenu helpMenu = new JMenu(getMessage("project.menu.help"));
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutMenuItem = new JMenuItem(getMessage("project.menu.help.about"));
        aboutMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                About.about(ProjectFrame.this);
            }
        });
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        helpMenu.add(aboutMenuItem);
        final JCheckBoxMenuItem automaticCheckNewVersionMenuItem = new JCheckBoxMenuItem(getMessage("project.menu.help.automatickcheckfornewversion"), FrinikaConfig.getAutomaticCheckForNewVersion());
        automaticCheckNewVersionMenuItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent itemEvent) {
                FrinikaConfig.setAutomatickCheckForNewVersion(automaticCheckNewVersionMenuItem.getState());
                if (automaticCheckNewVersionMenuItem.getState()) AutomaticVersionCheck.checkLatestVersion(ProjectFrame.this, automaticCheckNewVersionMenuItem);
            }
        });
        helpMenu.add(automaticCheckNewVersionMenuItem);
        AutomaticVersionCheck.checkLatestVersion(ProjectFrame.this, automaticCheckNewVersionMenuItem);
        return helpMenu;
    }

    JMenu viewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);
        JMenuItem item = new JMenuItem(new ToggleShowVoiceViewAction(this));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu.add(item);
        return menu;
    }

    JMenu connectedMenu() {
        JMenu menu = new JMenu(getMessage("project.menu.connected.connected"));
        menu.setMnemonic(KeyEvent.VK_C);
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(getMessage("project.menu.connected.radio"));
        item.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (item.getState()) {
                    LocalOGGHttpRadio.startRadio(project);
                } else {
                    LocalOGGHttpRadio.stopRadio();
                }
            }
        });
        menu.add(item);
        return menu;
    }

    /**
	 * Create menus
	 * 
	 * @return
	 */
    JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        menuBar.add(fileMenu());
        menuBar.add(editMenu());
        if (viewport == null) menuBar.add(viewMenu()); else menuBar.add(perspectiveMenu());
        menuBar.add(settingsMenu());
        menuBar.add(toolsMenu());
        menuBar.add(renderMenu());
        menuBar.add(connectedMenu());
        if (!FrinikaMain.isMac()) menuBar.add(helpMenu());
        menuBar.add(debugMenu());
        Part.addPluginRightButtonMenu(new MultiPartMenuPlugin(getProjectContainer()));
        return menuBar;
    }

    /**
	 * The splitpane is created here
	 * 
	 * @param frameSize
	 * @return
	 */
    JSplitPane createSplit(Dimension frameSize) {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setOneTouchExpandable(true);
        pianoControllerPane = new PianoControllerSplitPane(this);
        TrackerPanel trackerPanel = new TrackerPanel(project.getSequence(), this);
        frameSize.height = frameSize.height / 3;
        partViewEditor = new VoicePartViewSplitPane(this, false);
        partViewEditor.setPreferredSize(frameSize);
        project.getPartSelection().addSelectionListener(trackerPanel);
        JTabbedPane editorTabs = new JTabbedPane();
        editorTabs.setPreferredSize(frameSize);
        editorTabs.addTab("Piano roll", pianoControllerPane);
        editorTabs.addTab("Tracker", trackerPanel);
        JPanel mixer = new CompactMixerPanel(project.getMixerControls());
        mixer.setMinimumSize(new Dimension(0, 0));
        editorTabs.addTab(getMessage("project.maintabs.audiomixer"), mixer);
        split.setTopComponent(partViewEditor);
        split.setBottomComponent(editorTabs);
        return split;
    }

    public void tryQuit() {
        while (openProjectFrames.size() > 1) {
            System.out.println("Open project count:" + openProjectFrames.size());
            ProjectFrame frame = openProjectFrames.firstElement();
            if (frame == this) frame = openProjectFrames.lastElement();
            if (frame.canClose()) frame.dispose();
        }
        String quitMessage;
        if (project.getEditHistoryContainer().hasChanges()) quitMessage = getMessage("quit.doYouReallyWantTo.unsaved"); else quitMessage = getMessage("quit.doYouReallyWantTo");
        if (JOptionPane.showOptionDialog(this, quitMessage, getMessage("quit.really"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == 0) {
            MidiInDeviceManager.close();
            FrinikaConfig.storeAndQuit();
        }
    }

    private boolean canClose() {
        if (project.getEditHistoryContainer().hasChanges()) {
            if (JOptionPane.showOptionDialog(this, getMessage("close.unsavedChanges"), getMessage("close.unsavedChanges.dialogTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == 0) return true; else return false;
        } else return true;
    }

    @Override
    public void dispose() {
        project.close();
        openProjectFrames.remove(this);
        super.dispose();
    }

    public ProjectContainer getProjectContainer() {
        return project;
    }

    /**
	 * 
	 * @param string
	 * @deprecated
	 */
    public void infoMessage(String string) {
        message(string);
    }

    MidiDevicesPanel midiDevicesPanel;

    public MidiDevicesPanel getMidiDevicesPanel() {
        return midiDevicesPanel;
    }

    public VoicePartViewSplitPane getVoicePartViewSplitPane() {
        return partViewEditor;
    }

    public static void midiInDeviceChange() {
        System.out.println("MIDIIN CHANGER");
        MidiInDeviceManager.reset(FrinikaConfig.getMidiInDeviceList());
    }

    public void setStatusBarMessage(String msg) {
        statusBar.setMessage(msg);
    }

    public void message(String msg, int type) {
        JOptionPane.showMessageDialog(this, msg, "Frinika Message", type);
    }

    public void message(String msg) {
        message(msg, JOptionPane.INFORMATION_MESSAGE);
    }

    public void error(String msg) {
        message(msg, JOptionPane.ERROR_MESSAGE);
    }

    public void error(String msg, Throwable t) {
        t.printStackTrace();
        error(msg + " - " + t.getMessage());
    }

    public void error(Throwable t) {
        error(t.getClass().getName(), t);
    }

    public boolean confirm(String msg) {
        int result = JOptionPane.showConfirmDialog(this, msg, "Frinika Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return (result == JOptionPane.OK_OPTION);
    }

    public String prompt(String msg, String initialValue) {
        if (initialValue == null) initialValue = "";
        String result = JOptionPane.showInputDialog(this, msg, initialValue);
        return result;
    }

    public String prompt(String msg) {
        return prompt(msg, null);
    }

    public String promptFile(String defaultFilename, String[][] suffices, boolean saveMode, boolean directoryMode) {
        JFileChooser fc = new JFileChooser();
        if (!directoryMode) {
            final boolean save = saveMode;
            if (suffices != null) {
                for (int i = 0; i < suffices.length; i++) {
                    final String suffix = suffices[i][0];
                    final String description = suffices[i][1];
                    FileFilter ff = new FileFilter() {

                        public boolean accept(File file) {
                            if (file.isDirectory()) return true;
                            String name = file.getName();
                            return suffix.equals("*") || name.endsWith("." + suffix) || (save && fileDoesntExistAndDoesntEndWithAnySuffix(file));
                        }

                        public String getDescription() {
                            return "." + suffix + " - " + description;
                        }
                    };
                    fc.addChoosableFileFilter(ff);
                }
            }
        } else {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int r;
        if (defaultFilename != null) {
            File file = new File(defaultFilename);
            fc.setSelectedFile(file);
        }
        if (saveMode) {
            r = fc.showSaveDialog(this);
        } else {
            r = fc.showOpenDialog(this);
        }
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String name = file.getName();
            String extraSuffix = "";
            if (name.indexOf('.') == -1) {
                if (suffices != null && suffices.length > 0) {
                    extraSuffix = "." + suffices[0][0];
                }
            }
            String filename = file.getAbsolutePath() + extraSuffix;
            if (saveMode) {
                File fl = new File(filename);
                if (fl.exists()) {
                    if (!confirm("File " + filename + " already exists. Overwrite?")) {
                        return null;
                    }
                }
            }
            return filename;
        } else {
            return null;
        }
    }

    public String promptFile(String defaultFilename, String[][] suffices, boolean saveMode) {
        return promptFile(defaultFilename, suffices, saveMode, false);
    }

    public String promptFile(String defaultFilename, String[][] suffices) {
        return promptFile(defaultFilename, suffices, false);
    }

    private static boolean fileDoesntExistAndDoesntEndWithAnySuffix(File file) {
        if (file.exists()) return false;
        String name = file.getName();
        return (name.indexOf('.') == -1);
    }

    /**
	 * Opens the save-project dialog
	 * 
	 */
    void openSaveProjectDialog() {
        try {
            ProjectFileFilter no_compression = new ProjectFileFilter();
            ProjectFileFilter zip_compressed = new ProjectFileFilter() {

                public String getDescription() {
                    return "Frinika project (ZIP Compressed)";
                }
            };
            ProjectFileFilter lzma_compressed = new ProjectFileFilter() {

                public String getDescription() {
                    return "Frinika project (LZMA Compressed)";
                }
            };
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Frinika project");
            chooser.setFileFilter(no_compression);
            chooser.setFileFilter(zip_compressed);
            chooser.setFileFilter(lzma_compressed);
            chooser.setFileFilter(no_compression);
            chooser.setSelectedFile(project.getProjectFile());
            if (chooser.showSaveDialog(ProjectFrame.this) == JFileChooser.APPROVE_OPTION) {
                File newProject = chooser.getSelectedFile();
                if (chooser.getFileFilter() instanceof ProjectFileFilter) if (!chooser.getFileFilter().accept(newProject)) newProject = new File(newProject.getPath() + ".frinika");
                project.setSaveReferencedData(withRef);
                if (chooser.getFileFilter() == lzma_compressed) project.compression_level = 2; else if (chooser.getFileFilter() == zip_compressed) project.compression_level = 1; else project.compression_level = 0;
                project.saveProject(newProject);
                FrinikaConfig.setLastProjectFilename(newProject.getAbsolutePath());
                setTitle(newProject.getName());
            }
        } catch (Exception ex) {
            error("Error while saving", ex);
            ex.printStackTrace();
        }
    }

    public void showRightButtonPartPopup(Component invoker, int x, int y) {
        Part part = project.getPartSelection().getFocus();
        if (part == null) return;
        if (part.showRightButtonMenu(invoker, x, y)) return;
        part.showContextMenu(this, invoker, x, y);
    }

    public static void addProjectFocusListener(ProjectFocusListener l) {
        projectFocusListeners.add(l);
    }

    public static void removeProjectFocusListener(ProjectFocusListener l) {
        projectFocusListeners.remove(l);
    }

    public static void notifyProjectFocusListeners() {
        for (ProjectFocusListener l : projectFocusListeners) l.projectFocusNotify(focusFrame.getProjectContainer());
    }

    public static void staticMessage(ProjectContainer container, String string) {
        for (ProjectFrame f : openProjectFrames) {
            if (f.project == container) {
                f.message(string);
                return;
            }
        }
    }

    public static List<ProjectFrame> getOpenProjectFrames() {
        return Collections.unmodifiableList(openProjectFrames);
    }

    /**
	 * CheckBoxMenuItem which is directly bound to a boolean config value.
	 * 
	 * @author Jens Gulden
	 */
    static class CheckBoxMenuItemConfig extends JCheckBoxMenuItem implements ActionListener, ConfigListener {

        private FrinikaConfig.Meta configOption;

        public CheckBoxMenuItemConfig(String text, FrinikaConfig.Meta configOption) {
            super(text);
            this.configOption = configOption;
            if ((configOption.getType() == boolean.class) || (configOption.getType() == Boolean.class)) {
                refresh();
            } else {
                throw new ConfigError("cannot bind checkbox-menu-item to option '" + configOption.getName() + "' which is of type " + configOption.getType().getName());
            }
            this.addActionListener(this);
            FrinikaConfig.addConfigListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            update();
        }

        public void configurationChanged(ChangeEvent e) {
            if (e.getSource() == configOption) {
                refresh();
            }
        }

        private void refresh() {
            boolean b = Boolean.valueOf(configOption.get().toString());
            this.setSelected(b);
        }

        private void update() {
            boolean b = this.isSelected();
            configOption.set(b);
        }
    }
}
