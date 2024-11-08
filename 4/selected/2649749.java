package javazoom.jlgui.player.amp;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import javazoom.jlgui.player.amp.equalizer.ui.EqualizerUI;
import javazoom.jlgui.player.amp.playlist.Playlist;
import javazoom.jlgui.player.amp.playlist.PlaylistFactory;
import javazoom.jlgui.player.amp.playlist.PlaylistItem;
import javazoom.jlgui.player.amp.playlist.ui.PlaylistUI;
import javazoom.jlgui.player.amp.skin.AbsoluteLayout;
import javazoom.jlgui.player.amp.skin.DropTargetAdapter;
import javazoom.jlgui.player.amp.skin.ImageBorder;
import javazoom.jlgui.player.amp.skin.PopupAdapter;
import javazoom.jlgui.player.amp.skin.Skin;
import javazoom.jlgui.player.amp.skin.UrlDialog;
import javazoom.jlgui.player.amp.tag.ui.TagSearch;
import javazoom.jlgui.player.amp.util.Config;
import javazoom.jlgui.player.amp.util.FileSelector;
import javazoom.jlgui.player.amp.util.ui.Preferences;
import javazoom.jlgui.player.amp.visual.ui.SpectrumTimeAnalyzer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jpark.jamse.player.api.DetailInfo;
import com.jpark.jamse.player.api.IPlaylist;
import com.jpark.jamse.player.api.SongInfo;
import com.jpark.jamse.player.api.IPlayer.Buttons;
import java.net.ConnectException;

public class PlayerUI extends JPanel implements ActionListener, ChangeListener, BasicPlayerListener {

    private static Log log = LogFactory.getLog(PlayerUI.class);

    public static final int INIT = 0;

    public static final int OPEN = 1;

    public static final int PLAY = 2;

    public static final int PAUSE = 3;

    public static final int STOP = 4;

    public static final int TEXT_LENGTH_MAX = 30;

    public static final long SCROLL_PERIOD = 250;

    private long currentSeconds;

    private long total;

    private Skin ui = null;

    private Loader loader = null;

    private Config config = null;

    private JPopupMenu mainpopup = null;

    private JPopupMenu ejectpopup = null;

    private JCheckBoxMenuItem miPlaylist = null;

    private JCheckBoxMenuItem miEqualizer = null;

    private JMenuItem miPlayFile = null;

    private JMenuItem miPlayLocation = null;

    private PopupAdapter popupAdapter = null;

    private PopupAdapter ejectpopupAdapter = null;

    private BasicController theSoundPlayer = null;

    private Map audioInfo = null;

    private int playerState = INIT;

    private String titleText = Skin.TITLETEXT.toUpperCase();

    private String currentTitle = Skin.TITLETEXT.toUpperCase();

    private String[] titleScrollLabel = null;

    private int scrollIndex = 0;

    private long lastScrollTime = 0L;

    private boolean scrollRight = true;

    private long secondsAmount = 0;

    private Playlist playlist = null;

    private PlaylistUI playlistUI = null;

    private String currentFileOrURL = null;

    private String currentSongName = null;

    private PlaylistItem currentPlaylistItem = null;

    private boolean currentIsFile;

    private boolean posValueJump = false;

    private boolean posDragging = false;

    private double posValue = 0.0;

    private EqualizerUI equalizerUI = null;

    private DetailInfo dInfo;

    Worker worker;

    public PlayerUI() {
        super();
        setDoubleBuffered(true);
        ui = new Skin();
        worker = new Worker();
    }

    public void setEqualizerUI(EqualizerUI eq) {
        equalizerUI = eq;
    }

    public EqualizerUI getEqualizerUI() {
        return equalizerUI;
    }

    public PlaylistUI getPlaylistUI() {
        return playlistUI;
    }

    public void setPlaylistUI(PlaylistUI playlistUI) {
        this.playlistUI = playlistUI;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    /**
     * Return config.
     * 
     * @return
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Return skin.
     * 
     * @return
     */
    public Skin getSkin() {
        return ui;
    }

    /**
     * Return parent loader.
     * 
     * @return
     */
    public Loader getLoader() {
        return loader;
    }

    /**
     * A handle to the BasicPlayer, plugins may control the player through the
     * controller (play, stop, ...)
     * 
     * @param controller
     */
    public void setController(BasicController controller) {
        theSoundPlayer = controller;
    }

    /**
     * Return player controller.
     * 
     * @return
     */
    public BasicController getController() {
        return theSoundPlayer;
    }

    /**
     * Load main player.
     * 
     * @param loader
     */
    public void loadUI(Loader loader) {
        this.loader = loader;
        setLayout(new AbsoluteLayout());
        config = Config.getInstance();
        ui.setConfig(config);
        playlistUI = new PlaylistUI();
        playlistUI.setSkin(ui);
        playlistUI.setPlayer(this);
        equalizerUI = new EqualizerUI();
        equalizerUI.setSkin(ui);
        loadSkin();
        DropTargetAdapter dnd = new DropTargetAdapter() {

            public void processDrop(Object data) {
                processDnD(data);
            }
        };
        DropTarget dt = new DropTarget(this, DnDConstants.ACTION_COPY, dnd, true);
    }

    public void loadSkin() {
        log.info("Load PlayerUI (EDT=" + SwingUtilities.isEventDispatchThread() + ")");
        removeAll();
        if (ui.getPath() != null) {
            log.info("Load default skin from " + ui.getPath());
            ui.loadSkin(ui.getPath());
            config.setDefaultSkin(ui.getPath());
        } else if ((config.getDefaultSkin() != null) && (!config.getDefaultSkin().trim().equals(""))) {
            log.info("Load default skin from " + config.getDefaultSkin());
            ui.loadSkin(config.getDefaultSkin());
        } else {
            ClassLoader cl = getClass().getClassLoader();
            InputStream sis = cl.getResourceAsStream("javazoom/jlgui/player/amp/nucleo.wsz");
            log.info("Load default skin for JAR");
            ui.loadSkin(sis);
        }
        ImageBorder border = new ImageBorder();
        border.setImage(ui.getMainImage());
        setBorder(border);
        add(ui.getAcPrevious(), ui.getAcPrevious().getConstraints());
        ui.getAcPrevious().removeActionListener(this);
        ui.getAcPrevious().addActionListener(this);
        add(ui.getAcPlay(), ui.getAcPlay().getConstraints());
        ui.getAcPlay().removeActionListener(this);
        ui.getAcPlay().addActionListener(this);
        add(ui.getAcPause(), ui.getAcPause().getConstraints());
        ui.getAcPause().removeActionListener(this);
        ui.getAcPause().addActionListener(this);
        add(ui.getAcStop(), ui.getAcStop().getConstraints());
        ui.getAcStop().removeActionListener(this);
        ui.getAcStop().addActionListener(this);
        add(ui.getAcNext(), ui.getAcNext().getConstraints());
        ui.getAcNext().removeActionListener(this);
        ui.getAcNext().addActionListener(this);
        add(ui.getAcEject(), ui.getAcEject().getConstraints());
        ui.getAcEject().removeActionListener(this);
        ui.getAcEject().addActionListener(this);
        add(ui.getAcEqualizer(), ui.getAcEqualizer().getConstraints());
        ui.getAcEqualizer().removeActionListener(this);
        ui.getAcEqualizer().addActionListener(this);
        add(ui.getAcPlaylist(), ui.getAcPlaylist().getConstraints());
        ui.getAcPlaylist().removeActionListener(this);
        ui.getAcPlaylist().addActionListener(this);
        add(ui.getAcShuffle(), ui.getAcShuffle().getConstraints());
        ui.getAcShuffle().removeActionListener(this);
        ui.getAcShuffle().addActionListener(this);
        add(ui.getAcRepeat(), ui.getAcRepeat().getConstraints());
        ui.getAcRepeat().removeActionListener(this);
        ui.getAcRepeat().addActionListener(this);
        add(ui.getAcVolume(), ui.getAcVolume().getConstraints());
        ui.getAcVolume().removeChangeListener(this);
        ui.getAcVolume().addChangeListener(this);
        add(ui.getAcBalance(), ui.getAcBalance().getConstraints());
        ui.getAcBalance().removeChangeListener(this);
        ui.getAcBalance().addChangeListener(this);
        add(ui.getAcPosBar(), ui.getAcPosBar().getConstraints());
        ui.getAcPosBar().removeChangeListener(this);
        ui.getAcPosBar().addChangeListener(this);
        add(ui.getAcMonoIcon(), ui.getAcMonoIcon().getConstraints());
        add(ui.getAcStereoIcon(), ui.getAcStereoIcon().getConstraints());
        add(ui.getAcTitleLabel(), ui.getAcTitleLabel().getConstraints());
        add(ui.getAcSampleRateLabel(), ui.getAcSampleRateLabel().getConstraints());
        add(ui.getAcBitRateLabel(), ui.getAcBitRateLabel().getConstraints());
        add(ui.getAcPlayIcon(), ui.getAcPlayIcon().getConstraints());
        add(ui.getAcTimeIcon(), ui.getAcTimeIcon().getConstraints());
        add(ui.getAcMinuteH(), ui.getAcMinuteH().getConstraints());
        add(ui.getAcMinuteL(), ui.getAcMinuteL().getConstraints());
        add(ui.getAcSecondH(), ui.getAcSecondH().getConstraints());
        add(ui.getAcSecondL(), ui.getAcSecondL().getConstraints());
        add(ui.getAcTitleBar(), ui.getAcTitleBar().getConstraints());
        add(ui.getAcMinimize(), ui.getAcMinimize().getConstraints());
        ui.getAcMinimize().removeActionListener(this);
        ui.getAcMinimize().addActionListener(this);
        add(ui.getAcExit(), ui.getAcExit().getConstraints());
        ui.getAcExit().removeActionListener(this);
        ui.getAcExit().addActionListener(this);
        if (ui.getAcAnalyzer() != null) {
            add(ui.getAcAnalyzer(), ui.getAcAnalyzer().getConstraints());
        }
        mainpopup = new JPopupMenu(ui.getResource("popup.title"));
        JMenuItem mi = new JMenuItem(Skin.TITLETEXT + "- JavaZOOM");
        mainpopup.add(mi);
        mainpopup.addSeparator();
        JMenu playSubMenu = new JMenu(ui.getResource("popup.play"));
        miPlayFile = new JMenuItem(ui.getResource("popup.play.file"));
        miPlayFile.setActionCommand(PlayerActionEvent.MIPLAYFILE);
        miPlayFile.removeActionListener(this);
        miPlayFile.addActionListener(this);
        miPlayLocation = new JMenuItem(ui.getResource("popup.play.location"));
        miPlayLocation.setActionCommand(PlayerActionEvent.MIPLAYLOCATION);
        miPlayLocation.removeActionListener(this);
        miPlayLocation.addActionListener(this);
        playSubMenu.add(miPlayFile);
        playSubMenu.add(miPlayLocation);
        mainpopup.add(playSubMenu);
        mainpopup.addSeparator();
        miPlaylist = new JCheckBoxMenuItem(ui.getResource("popup.playlist"));
        miPlaylist.setActionCommand(PlayerActionEvent.MIPLAYLIST);
        if (config.isPlaylistEnabled()) {
            miPlaylist.setState(true);
        }
        miPlaylist.removeActionListener(this);
        miPlaylist.addActionListener(this);
        mainpopup.add(miPlaylist);
        miEqualizer = new JCheckBoxMenuItem(ui.getResource("popup.equalizer"));
        miEqualizer.setActionCommand(PlayerActionEvent.MIEQUALIZER);
        if (config.isEqualizerEnabled()) {
            miEqualizer.setState(true);
        }
        miEqualizer.removeActionListener(this);
        miEqualizer.addActionListener(this);
        mainpopup.add(miEqualizer);
        mainpopup.addSeparator();
        mi = new JMenuItem(ui.getResource("popup.preferences"));
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK, false));
        mi.setActionCommand(PlayerActionEvent.MIPREFERENCES);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        mainpopup.add(mi);
        JMenu skinsSubMenu = new JMenu(ui.getResource("popup.skins"));
        mi = new JMenuItem(ui.getResource("popup.skins.browser"));
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK, false));
        mi.setActionCommand(PlayerActionEvent.MISKINBROWSER);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        skinsSubMenu.add(mi);
        mi = new JMenuItem(ui.getResource("popup.skins.load"));
        mi.setActionCommand(PlayerActionEvent.MILOADSKIN);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        skinsSubMenu.add(mi);
        mainpopup.add(skinsSubMenu);
        JMenu playbackSubMenu = new JMenu(ui.getResource("popup.playback"));
        mi = new JMenuItem(ui.getResource("popup.playback.jump"));
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0, false));
        mi.setActionCommand(PlayerActionEvent.MIJUMPFILE);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        playbackSubMenu.add(mi);
        mi = new JMenuItem(ui.getResource("popup.playback.stop"));
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, false));
        mi.setActionCommand(PlayerActionEvent.MISTOP);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        playbackSubMenu.add(mi);
        mainpopup.add(playbackSubMenu);
        mainpopup.addSeparator();
        mi = new JMenuItem(ui.getResource("popup.exit"));
        mi.setActionCommand(PlayerActionEvent.ACEXIT);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        mainpopup.add(mi);
        ui.getAcTitleBar().removeMouseListener(popupAdapter);
        popupAdapter = new PopupAdapter(mainpopup);
        ui.getAcTitleBar().addMouseListener(popupAdapter);
        ui.getAcAnalyzer().addMouseListener(popupAdapter);
        ui.getAcTitleLabel().addMouseListener(popupAdapter);
        ui.getAcBitRateLabel().addMouseListener(popupAdapter);
        ui.getAcMinuteH().addMouseListener(popupAdapter);
        ui.getAcMinuteL().addMouseListener(popupAdapter);
        ui.getAcTimeIcon().addMouseListener(popupAdapter);
        ui.getAcMonoIcon().addMouseListener(popupAdapter);
        ui.getAcSampleRateLabel().addMouseListener(popupAdapter);
        ui.getAcPosBar().addMouseListener(popupAdapter);
        ui.getAcStereoIcon().addMouseListener(popupAdapter);
        ui.getAcSecondH().addMouseListener(popupAdapter);
        ui.getAcSecondL().addMouseListener(popupAdapter);
        ejectpopup = new JPopupMenu();
        mi = new JMenuItem(ui.getResource("popup.eject.openfile"));
        mi.setActionCommand(PlayerActionEvent.MIPLAYFILE);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        ejectpopup.add(mi);
        mi = new JMenuItem(ui.getResource("popup.eject.openlocation"));
        mi.setActionCommand(PlayerActionEvent.MIPLAYLOCATION);
        mi.removeActionListener(this);
        mi.addActionListener(this);
        ejectpopup.add(mi);
        ui.getAcEject().removeMouseListener(ejectpopupAdapter);
        ejectpopupAdapter = new PopupAdapter(ejectpopup);
        ui.getAcEject().addMouseListener(ejectpopupAdapter);
        if (equalizerUI != null) {
            equalizerUI.loadUI();
        }
        if (playlistUI != null) {
            playlistUI.loadUI();
        }
        validate();
        loader.loaded();
    }

    /**
     * Load playlist.
     * 
     * @param playlistName
     * @return
     */
    public boolean loadPlaylist(String playlistName) {
        boolean loaded = false;
        PlaylistFactory plf = PlaylistFactory.getInstance();
        playlist = plf.getPlaylist();
        if (playlist == null) {
            config.setPlaylistClassName("javazoom.jlgui.player.amp.playlist.BasePlaylist");
            playlist = plf.getPlaylist();
        }
        playlistUI.setPlaylist(playlist);
        if ((playlistName != null) && (!playlistName.equals(""))) {
            if ((playlistName.toLowerCase().endsWith(ui.getResource("playlist.extension.m3u"))) || (playlistName.toLowerCase().endsWith(ui.getResource("playlist.extension.pls")))) {
                loaded = playlist.load(playlistName);
            } else {
                String name = playlistName;
                if (!Config.startWithProtocol(playlistName)) {
                    int indn = playlistName.lastIndexOf(java.io.File.separatorChar);
                    if (indn != -1) {
                        name = playlistName.substring(indn + 1);
                    }
                    PlaylistItem pli = new PlaylistItem(name, playlistName, -1, true);
                    playlist.appendItem(pli);
                    loaded = true;
                } else {
                    PlaylistItem pli = new PlaylistItem(name, playlistName, -1, false);
                    playlist.appendItem(pli);
                    loaded = true;
                }
            }
        }
        return loaded;
    }

    public void stateChanged(ChangeEvent e) {
        Object src = e.getSource();
        if (src == ui.getAcVolume()) {
            Object[] args = { String.valueOf(ui.getAcVolume().getValue()) };
            String volumeText = MessageFormat.format(ui.getResource("slider.volume.text"), args);
            ui.getAcTitleLabel().setAcText(volumeText);
            try {
                int gainValue = ui.getAcVolume().getValue();
                int maxGain = ui.getAcVolume().getMaximum();
                if (gainValue == 0) {
                    theSoundPlayer.setGain(0);
                } else {
                    theSoundPlayer.setGain(((double) gainValue / (double) maxGain));
                }
                config.setVolume(gainValue);
            } catch (BasicPlayerException ex) {
                log.debug("Cannot set gain", ex);
            }
        } else if (src == ui.getAcBalance()) {
            Object[] args = { String.valueOf(Math.abs(ui.getAcBalance().getValue() * 100 / Skin.BALANCEMAX)) };
            String balanceText = null;
            if (ui.getAcBalance().getValue() > 0) {
                balanceText = MessageFormat.format(ui.getResource("slider.balance.text.right"), args);
            } else if (ui.getAcBalance().getValue() < 0) {
                balanceText = MessageFormat.format(ui.getResource("slider.balance.text.left"), args);
            } else {
                balanceText = MessageFormat.format(ui.getResource("slider.balance.text.center"), args);
            }
            ui.getAcTitleLabel().setAcText(balanceText);
            try {
                float balanceValue = ui.getAcBalance().getValue() * 1.0f / Skin.BALANCEMAX;
                theSoundPlayer.setPan(balanceValue);
            } catch (BasicPlayerException ex) {
                log.debug("Cannot set pan", ex);
            }
        } else if (src == ui.getAcPosBar()) {
            if (ui.getAcPosBar().getValueIsAdjusting() == false) {
                if (posDragging == true) {
                    posDragging = false;
                    posValue = ui.getAcPosBar().getValue() * 1.0 / Skin.POSBARMAX;
                    processSeek(posValue);
                }
            } else {
                posDragging = true;
                posValueJump = true;
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        final ActionEvent evt = e;
        if (e.getActionCommand().equals(PlayerActionEvent.ACPAUSE)) {
            processActionEvent(e);
        } else if ((e.getActionCommand().equals(PlayerActionEvent.ACPLAY)) && (playerState == PAUSE)) {
            processActionEvent(e);
        } else {
            new Thread("PlayerUIActionEvent") {

                public void run() {
                    processActionEvent(evt);
                }
            }.start();
        }
    }

    /**
     * Process action event.
     * 
     * @param e
     */
    public void processActionEvent(final ActionEvent e) {
        String cmd = e.getActionCommand();
        log.debug("Action=" + cmd + " (EDT=" + SwingUtilities.isEventDispatchThread() + ")");
        if (cmd.equalsIgnoreCase(PlayerActionEvent.MIPREFERENCES)) {
            processPreferences(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.MISKINBROWSER)) {
            processSkinBrowser(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.MIJUMPFILE)) {
            processJumpToFile(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.MISTOP)) {
            processStop(MouseEvent.BUTTON1_MASK);
        } else if (e.getActionCommand().equals(PlayerActionEvent.MILOADSKIN)) {
            File[] file = FileSelector.selectFile(loader, FileSelector.OPEN, false, ui.getResource("skin.extension"), ui.getResource("loadskin.dialog.filtername"), new File(config.getLastDir()));
            if (FileSelector.getInstance().getDirectory() != null) {
                config.setLastDir(FileSelector.getInstance().getDirectory().getPath());
            }
            if (file != null) {
                String fsFile = file[0].getName();
                ui.setPath(config.getLastDir() + fsFile);
                loadSkin();
                config.setDefaultSkin(ui.getPath());
            }
        } else if (cmd.equals(PlayerActionEvent.ACSHUFFLE)) {
            if (ui.getAcShuffle().isSelected()) {
                config.setShuffleEnabled(true);
                if (playlist != null) {
                    playlist.shuffle();
                    playlistUI.initPlayList();
                    PlaylistItem pli = playlist.getCursor();
                    setCurrentSong(pli);
                }
            } else {
                config.setShuffleEnabled(false);
            }
        } else if (cmd.equals(PlayerActionEvent.ACREPEAT)) {
            if (ui.getAcRepeat().isSelected()) {
                config.setRepeatEnabled(true);
            } else {
                config.setRepeatEnabled(false);
            }
        } else if (cmd.equals(PlayerActionEvent.MIPLAYFILE)) {
            processEject(MouseEvent.BUTTON1_MASK);
        } else if (cmd.equals(PlayerActionEvent.MIPLAYLOCATION)) {
            processEject(MouseEvent.BUTTON3_MASK);
        } else if (cmd.equals(PlayerActionEvent.MIPLAYLIST)) {
            ui.getAcPlaylist().setSelected(miPlaylist.getState());
            togglePlaylist();
        } else if (cmd.equals(PlayerActionEvent.ACPLAYLIST)) {
            togglePlaylist();
        } else if (cmd.equals(PlayerActionEvent.MIEQUALIZER)) {
            ui.getAcEqualizer().setSelected(miEqualizer.getState());
            toggleEqualizer();
        } else if (cmd.equals(PlayerActionEvent.ACEQUALIZER)) {
            toggleEqualizer();
        } else if (cmd.equals(PlayerActionEvent.ACEXIT)) {
            closePlayer();
        } else if (cmd.equals(PlayerActionEvent.ACMINIMIZE)) {
            loader.minimize();
        } else if (cmd.equals(PlayerActionEvent.ACEJECT)) {
            processEject(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.ACPLAY) && playerState != PLAY) {
            IWork w = new IWork() {

                public void execute() {
                    processPlay(e.getModifiers());
                }
            };
            doWork(w);
        } else if (cmd.equals(PlayerActionEvent.ACPAUSE)) {
            processPause(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.ACSTOP)) {
            processStop(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.ACNEXT)) {
            processNext(e.getModifiers());
        } else if (cmd.equals(PlayerActionEvent.ACPREVIOUS)) {
            processPrevious(e.getModifiers());
        }
    }

    public void opened(Object stream, Map properties) {
        audioInfo = properties;
        log.debug(properties.toString());
    }

    public void stateUpdated(final BasicPlayerEvent event) {
        processStateUpdated(event);
    }

    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
        processProgress(bytesread, microseconds, pcmdata, properties);
    }

    /**
     * Process PREFERENCES event.
     * 
     * @param modifiers
     */
    protected void processPreferences(int modifiers) {
        Preferences preferences = Preferences.getInstance(this);
        preferences.setLocation(loader.getLocation().x, loader.getLocation().y);
        preferences.setSize(512, 350);
        preferences.setVisible(true);
    }

    /**
     * Process SKINS BROWSER event.
     * 
     * @param modifiers
     */
    protected void processSkinBrowser(int modifiers) {
        Preferences preferences = Preferences.getInstance(this);
        preferences.selectSkinBrowserPane();
        preferences.setLocation(loader.getLocation().x, loader.getLocation().y);
        preferences.setSize(512, 350);
        preferences.setVisible(true);
    }

    /**
     * Process JUMP FILE event.
     * 
     * @param modifiers
     */
    protected void processJumpToFile(int modifiers) {
        TagSearch ts = new TagSearch(this);
        ts.setIconImage(config.getIconParent().getImage());
        ts.setSize(400, 300);
        ts.setLocation(loader.getLocation());
        ts.display();
    }

    /**
     * Process EJECT event.
     * 
     * @param modifiers
     */
    protected void processEject(int modifiers) {
        if ((playerState == PLAY) || (playerState == PAUSE)) {
            try {
                if (theSoundPlayer != null) {
                    theSoundPlayer.stop();
                }
            } catch (BasicPlayerException e) {
                log.info("Cannot stop", e);
            }
            playerState = STOP;
        }
        if ((playerState == INIT) || (playerState == STOP) || (playerState == OPEN)) {
            PlaylistItem pli = null;
            if (modifiers == MouseEvent.BUTTON1_MASK) {
                File[] file = FileSelector.selectFile(loader, FileSelector.OPEN, false, config.getExtensions(), ui.getResource("button.eject.filedialog.filtername"), new File(config.getLastDir()));
                if (FileSelector.getInstance().getDirectory() != null) {
                    config.setLastDir(FileSelector.getInstance().getDirectory().getPath());
                }
                if (file != null) {
                    String fsFile = file[0].getName();
                    if (fsFile != null) {
                        if ((fsFile.toLowerCase().endsWith(ui.getResource("playlist.extension.m3u"))) || (fsFile.toLowerCase().endsWith(ui.getResource("playlist.extension.pls")))) {
                            if (loadPlaylist(config.getLastDir() + fsFile)) {
                                config.setPlaylistFilename(config.getLastDir() + fsFile);
                                playlist.begin();
                                playlistUI.initPlayList();
                                setCurrentSong(playlist.getCursor());
                                playlistUI.repaint();
                            }
                        } else if (fsFile.toLowerCase().endsWith(ui.getResource("skin.extension"))) {
                            ui.setPath(config.getLastDir() + fsFile);
                            loadSkin();
                            config.setDefaultSkin(ui.getPath());
                        } else {
                            pli = new PlaylistItem(fsFile, config.getLastDir() + fsFile, -1, true);
                        }
                    }
                }
            } else if (modifiers == MouseEvent.BUTTON3_MASK) {
                UrlDialog UD = new UrlDialog(config.getTopParent(), ui.getResource("button.eject.urldialog.title"), loader.getLocation().x, loader.getLocation().y + 10, config.getLastURL());
                UD.show();
                if (UD.getFile() != null) {
                    showTitle(ui.getResource("title.loading"));
                    if ((UD.getURL().toLowerCase().endsWith(ui.getResource("playlist.extension.m3u"))) || (UD.getURL().toLowerCase().endsWith(ui.getResource("playlist.extension.pls")))) {
                        if (loadPlaylist(UD.getURL())) {
                            config.setPlaylistFilename(UD.getURL());
                            playlist.begin();
                            playlistUI.initPlayList();
                            setCurrentSong(playlist.getCursor());
                            playlistUI.repaint();
                        }
                    } else {
                        pli = new PlaylistItem(UD.getFile(), UD.getURL(), -1, false);
                    }
                    config.setLastURL(UD.getURL());
                }
            }
            if ((pli != null) && (playlist != null)) {
                playlist.removeAllItems();
                playlist.appendItem(pli);
                playlist.nextCursor();
                playlistUI.initPlayList();
                setCurrentSong(pli);
                playlistUI.repaint();
            }
        }
        ui.getAcPlayIcon().setIcon(2);
        ui.getAcTimeIcon().setIcon(1);
    }

    /**
     * Process PLAY event.
     * 
     * @param modifiers
     */
    protected void processPlay(int modifiers) {
        if (playlist.isModified()) {
            PlaylistItem pli = playlist.getCursor();
            if (pli == null) {
                playlist.begin();
                pli = playlist.getCursor();
            }
            setCurrentSong(pli);
            playlist.setModified(false);
            playlistUI.repaint();
        }
        if (playerState == PAUSE) {
            try {
                theSoundPlayer.resume();
            } catch (BasicPlayerException e) {
                log.error("Cannot resume", e);
            }
            playerState = PLAY;
            ui.getAcPlayIcon().setIcon(0);
            ui.getAcTimeIcon().setIcon(0);
        } else if (playerState == PLAY) {
            try {
                theSoundPlayer.stop();
            } catch (BasicPlayerException e) {
                log.error("Cannot stop", e);
            }
            playerState = PLAY;
            secondsAmount = 0;
            ui.getAcMinuteH().setAcText("0");
            ui.getAcMinuteL().setAcText("0");
            ui.getAcSecondH().setAcText("0");
            ui.getAcSecondL().setAcText("0");
            if (currentFileOrURL != null) {
                try {
                    if (currentIsFile == true) {
                        theSoundPlayer.open(openFile(currentFileOrURL));
                    } else {
                        theSoundPlayer.open(new URL(currentFileOrURL));
                    }
                    theSoundPlayer.play();
                } catch (Exception ex) {
                    log.error("Cannot read file : " + currentFileOrURL, ex);
                    if (ex instanceof ConnectException) {
                        showMessage(ui.getResource("title.connect.error"));
                    } else {
                        showMessage(ui.getResource("title.invalidfile"));
                    }
                }
            }
        } else if ((playerState == STOP) || (playerState == OPEN)) {
            try {
                theSoundPlayer.stop();
            } catch (BasicPlayerException e) {
                log.error("Stop failed", e);
            }
            if (currentFileOrURL != null) {
                try {
                    if (currentIsFile == true) {
                        theSoundPlayer.open(openFile(currentFileOrURL));
                    } else {
                        theSoundPlayer.open(new URL(currentFileOrURL));
                    }
                    theSoundPlayer.play();
                    titleText = currentSongName.toUpperCase();
                    int bitRate = -1;
                    if (currentPlaylistItem != null) {
                        bitRate = currentPlaylistItem.getBitrate();
                    }
                    if ((bitRate <= 0) && (audioInfo.containsKey("bitrate"))) {
                        bitRate = ((Integer) audioInfo.get("bitrate")).intValue();
                    }
                    if ((bitRate <= 0) && (audioInfo.containsKey("audio.framerate.fps")) && (audioInfo.containsKey("audio.framesize.bytes"))) {
                        float FR = ((Float) audioInfo.get("audio.framerate.fps")).floatValue();
                        int FS = ((Integer) audioInfo.get("audio.framesize.bytes")).intValue();
                        bitRate = Math.round(FS * FR * 8);
                    }
                    int channels = -1;
                    if (currentPlaylistItem != null) {
                        channels = currentPlaylistItem.getChannels();
                    }
                    if ((channels <= 0) && (audioInfo.containsKey("audio.channels"))) {
                        channels = ((Integer) audioInfo.get("audio.channels")).intValue();
                    }
                    float sampleRate = -1.0f;
                    if (currentPlaylistItem != null) {
                        sampleRate = currentPlaylistItem.getSamplerate();
                    }
                    if ((sampleRate <= 0) && (audioInfo.containsKey("audio.samplerate.hz"))) {
                        sampleRate = ((Float) audioInfo.get("audio.samplerate.hz")).floatValue();
                    }
                    long lenghtInSecond = -1L;
                    if (currentPlaylistItem != null) {
                        lenghtInSecond = currentPlaylistItem.getLength();
                    }
                    if ((lenghtInSecond <= 0) && (audioInfo.containsKey("duration"))) {
                        lenghtInSecond = ((Long) audioInfo.get("duration")).longValue() / 1000000;
                    }
                    if ((lenghtInSecond <= 0) && (audioInfo.containsKey("audio.length.bytes"))) {
                        lenghtInSecond = (long) Math.round(getTimeLengthEstimation(audioInfo) / 1000);
                        if (lenghtInSecond > 0) {
                            int minutes = (int) Math.floor(lenghtInSecond / 60);
                            int hours = (int) Math.floor(minutes / 60);
                            minutes = minutes - hours * 60;
                            int seconds = (int) (lenghtInSecond - minutes * 60 - hours * 3600);
                            if (seconds >= 10) {
                                titleText = "(" + minutes + ":" + seconds + ") " + titleText;
                            } else {
                                titleText = "(" + minutes + ":0" + seconds + ") " + titleText;
                            }
                        }
                    }
                    bitRate = Math.round((bitRate / 1000));
                    ui.getAcSampleRateLabel().setAcText(String.valueOf(Math.round((sampleRate / 1000))));
                    if (bitRate > 999) {
                        bitRate = (int) (bitRate / 100);
                        ui.getAcBitRateLabel().setAcText(bitRate + "H");
                    } else {
                        ui.getAcBitRateLabel().setAcText(String.valueOf(bitRate));
                    }
                    if (channels == 2) {
                        ui.getAcStereoIcon().setIcon(1);
                        ui.getAcMonoIcon().setIcon(0);
                    } else if (channels == 1) {
                        ui.getAcStereoIcon().setIcon(0);
                        ui.getAcMonoIcon().setIcon(1);
                    }
                    showTitle(titleText);
                    ui.getAcMinuteH().setAcText("0");
                    ui.getAcMinuteL().setAcText("0");
                    ui.getAcSecondH().setAcText("0");
                    ui.getAcSecondL().setAcText("0");
                    ui.getAcPlayIcon().setIcon(0);
                    ui.getAcTimeIcon().setIcon(0);
                    dInfo = new DetailInfo(bitRate, (int) sampleRate, channels);
                } catch (BasicPlayerException bpe) {
                    log.error("Stream error :" + currentFileOrURL, bpe);
                    showMessage(ui.getResource("title.invalidfile"));
                } catch (MalformedURLException mue) {
                    log.error("Stream error :" + currentFileOrURL, mue);
                    showMessage(ui.getResource("title.invalidfile"));
                }
                try {
                    theSoundPlayer.setGain(((double) ui.getAcVolume().getValue() / (double) ui.getAcVolume().getMaximum()));
                    theSoundPlayer.setPan((float) ui.getAcBalance().getValue() / 10.0f);
                } catch (BasicPlayerException e) {
                    log.error("Cannot set control", e);
                }
                playerState = PLAY;
                log.info(titleText);
            }
        }
    }

    /**
     * Process PAUSE event.
     * 
     * @param modifiers
     */
    public void processPause(int modifiers) {
        if (playerState == PLAY) {
            try {
                theSoundPlayer.pause();
            } catch (BasicPlayerException e) {
                log.error("Cannot pause", e);
            }
            playerState = PAUSE;
            ui.getAcPlayIcon().setIcon(1);
            ui.getAcTimeIcon().setIcon(1);
        } else if (playerState == PAUSE) {
            try {
                theSoundPlayer.resume();
            } catch (BasicPlayerException e) {
                log.error("Cannot resume", e);
            }
            playerState = PLAY;
            ui.getAcPlayIcon().setIcon(0);
            ui.getAcTimeIcon().setIcon(0);
        }
    }

    /**
     * Process STOP event.
     * 
     * @param modifiers
     */
    public void processStop(int modifiers) {
        if ((playerState == PAUSE) || (playerState == PLAY)) {
            try {
                theSoundPlayer.stop();
            } catch (BasicPlayerException e) {
                log.info("Cannot stop", e);
            }
            playerState = STOP;
            secondsAmount = 0;
            ui.getAcPosBar().setValue(0);
            ui.getAcPlayIcon().setIcon(2);
            ui.getAcTimeIcon().setIcon(1);
        }
    }

    /**
     * Process NEXT event.
     * 
     * @param modifiers
     */
    public void processNext(int modifiers) {
        playlist.nextCursor();
        playlistUI.nextCursor();
        PlaylistItem pli = playlist.getCursor();
        setCurrentSong(pli);
    }

    /**
     * Process PREVIOUS event.
     * 
     * @param modifiers
     */
    public void processPrevious(int modifiers) {
        playlist.previousCursor();
        playlistUI.nextCursor();
        PlaylistItem pli = playlist.getCursor();
        setCurrentSong(pli);
    }

    /**
     * Process STATEUPDATED event.
     * 
     * @param event
     */
    public void processStateUpdated(BasicPlayerEvent event) {
        log.debug("Player:" + event + " (EDT=" + SwingUtilities.isEventDispatchThread() + ")");
        int state = event.getCode();
        Object obj = event.getDescription();
        if (state == BasicPlayerEvent.EOM) {
            if ((playerState == PAUSE) || (playerState == PLAY)) {
                playlist.nextCursor();
                playlistUI.nextCursor();
                PlaylistItem pli = playlist.getCursor();
                setCurrentSong(pli);
            }
        } else if (state == BasicPlayerEvent.PLAYING) {
            lastScrollTime = System.currentTimeMillis();
            posValueJump = false;
            if (audioInfo.containsKey("basicplayer.sourcedataline")) {
                if (ui.getAcAnalyzer() != null) {
                    ui.getAcAnalyzer().setupDSP((SourceDataLine) audioInfo.get("basicplayer.sourcedataline"));
                    ui.getAcAnalyzer().startDSP((SourceDataLine) audioInfo.get("basicplayer.sourcedataline"));
                }
            }
        } else if (state == BasicPlayerEvent.SEEKING) {
            posValueJump = true;
        } else if (state == BasicPlayerEvent.SEEKED) {
            try {
                theSoundPlayer.setGain(((double) ui.getAcVolume().getValue() / (double) ui.getAcVolume().getMaximum()));
                theSoundPlayer.setPan((float) ui.getAcBalance().getValue() / 10.0f);
            } catch (BasicPlayerException e) {
                log.debug(e);
            }
        } else if (state == BasicPlayerEvent.OPENING) {
            if ((obj instanceof URL) || (obj instanceof InputStream)) {
                showTitle(ui.getResource("title.buffering"));
            }
        } else if (state == BasicPlayerEvent.STOPPED) {
            if (ui.getAcAnalyzer() != null) {
                ui.getAcAnalyzer().stopDSP();
                ui.getAcAnalyzer().repaint();
            }
        }
    }

    /**
     * Process PROGRESS event.
     * 
     * @param bytesread
     * @param microseconds
     * @param pcmdata
     * @param properties
     */
    public void processProgress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
        int byteslength = -1;
        total = -1;
        if (currentPlaylistItem != null) {
            total = currentPlaylistItem.getLength();
        }
        if (total <= 0) {
            total = (long) Math.round(getTimeLengthEstimation(audioInfo) / 1000);
        }
        if (total <= 0) {
            total = -1;
        }
        if (audioInfo.containsKey("basicplayer.sourcedataline")) {
            if (ui.getAcAnalyzer() != null) {
                ui.getAcAnalyzer().writeDSP(pcmdata);
            }
        }
        if (audioInfo.containsKey("audio.length.bytes")) {
            byteslength = ((Integer) audioInfo.get("audio.length.bytes")).intValue();
        }
        float progress = -1.0f;
        if ((bytesread > 0) && ((byteslength > 0))) {
            progress = bytesread * 1.0f / byteslength * 1.0f;
        }
        if (audioInfo.containsKey("audio.type")) {
            String audioformat = (String) audioInfo.get("audio.type");
            if (audioformat.equalsIgnoreCase("mp3")) {
                if (properties.containsKey("mp3.shoutcast.metadata.StreamTitle")) {
                    String shoutTitle = ((String) properties.get("mp3.shoutcast.metadata.StreamTitle")).trim();
                    if (shoutTitle.length() > 0) {
                        if (currentPlaylistItem != null) {
                            String sTitle = " (" + currentPlaylistItem.getFormattedDisplayName() + ")";
                            if (!currentPlaylistItem.getFormattedName().equals(shoutTitle + sTitle)) {
                                currentPlaylistItem.setFormattedDisplayName(shoutTitle + sTitle);
                                showTitle((shoutTitle + sTitle).toUpperCase());
                                playlistUI.paintList();
                            }
                        }
                    }
                }
                if (properties.containsKey("mp3.equalizer")) {
                    equalizerUI.setBands((float[]) properties.get("mp3.equalizer"));
                }
                if (total > 0) {
                    secondsAmount = (long) (total * progress);
                } else {
                    secondsAmount = -1;
                }
            } else if (audioformat.equalsIgnoreCase("wave")) {
                secondsAmount = (long) (total * progress);
            } else {
                secondsAmount = (long) Math.round(microseconds / 1000000);
                equalizerUI.setBands(null);
            }
        } else {
            secondsAmount = (long) Math.round(microseconds / 1000000);
            equalizerUI.setBands(null);
        }
        if (secondsAmount < 0) {
            secondsAmount = (long) Math.round(microseconds / 1000000);
        }
        int secondD = 0, second = 0, minuteD = 0, minute = 0;
        currentSeconds = (int) secondsAmount;
        int minutes = (int) Math.floor(currentSeconds / 60);
        int hours = (int) Math.floor(minutes / 60);
        minutes = minutes - hours * 60;
        currentSeconds = currentSeconds - minutes * 60 - hours * 3600;
        if (currentSeconds < 10) {
            secondD = 0;
            second = (int) currentSeconds;
        } else {
            secondD = ((int) currentSeconds / 10);
            second = ((int) (currentSeconds - (((int) currentSeconds / 10)) * 10));
        }
        if (minutes < 10) {
            minuteD = 0;
            minute = minutes;
        } else {
            minuteD = ((int) minutes / 10);
            minute = ((int) (minutes - (((int) minutes / 10)) * 10));
        }
        ui.getAcMinuteH().setAcText(String.valueOf(minuteD));
        ui.getAcMinuteL().setAcText(String.valueOf(minute));
        ui.getAcSecondH().setAcText(String.valueOf(secondD));
        ui.getAcSecondL().setAcText(String.valueOf(second));
        if (total != 0) {
            if (posValueJump == false) {
                int posValue = ((int) Math.round(secondsAmount * Skin.POSBARMAX / total));
                ui.getAcPosBar().setValue(posValue);
            }
        } else {
            ui.getAcPosBar().setValue(0);
        }
        long ctime = System.currentTimeMillis();
        long lctime = lastScrollTime;
        if ((titleScrollLabel != null) && (titleScrollLabel.length > 0)) {
            if (ctime - lctime > SCROLL_PERIOD) {
                lastScrollTime = ctime;
                if (scrollRight == true) {
                    scrollIndex++;
                    if (scrollIndex >= titleScrollLabel.length) {
                        scrollIndex--;
                        scrollRight = false;
                    }
                } else {
                    scrollIndex--;
                    if (scrollIndex <= 0) {
                        scrollRight = true;
                    }
                }
                ui.getAcTitleLabel().setAcText(titleScrollLabel[scrollIndex]);
            }
        }
    }

    /**
     * Process seek feature.
     * 
     * @param rate
     */
    protected void processSeek(double rate) {
        try {
            if ((audioInfo != null) && (audioInfo.containsKey("audio.type"))) {
                String type = (String) audioInfo.get("audio.type");
                if ((type.equalsIgnoreCase("mp3")) && (audioInfo.containsKey("audio.length.bytes"))) {
                    long skipBytes = (long) Math.round(((Integer) audioInfo.get("audio.length.bytes")).intValue() * rate);
                    log.debug("Seek value (MP3) : " + skipBytes);
                    theSoundPlayer.seek(skipBytes);
                } else if ((type.equalsIgnoreCase("wave")) && (audioInfo.containsKey("audio.length.bytes"))) {
                    long skipBytes = (long) Math.round(((Integer) audioInfo.get("audio.length.bytes")).intValue() * rate);
                    log.debug("Seek value (WAVE) : " + skipBytes);
                    theSoundPlayer.seek(skipBytes);
                } else {
                    posValueJump = false;
                }
            } else {
                posValueJump = false;
            }
        } catch (BasicPlayerException ioe) {
            log.error("Cannot skip", ioe);
            posValueJump = false;
        }
    }

    /**
     * Process Drag&Drop
     * 
     * @param data
     */
    public void processDnD(Object data) {
        log.debug("Player DnD");
        if (data instanceof List) {
            List al = (List) data;
            if ((al != null) && (al.size() > 0)) {
                ArrayList fileList = new ArrayList();
                ArrayList folderList = new ArrayList();
                ListIterator li = al.listIterator();
                while (li.hasNext()) {
                    File f = (File) li.next();
                    if ((f.exists()) && (f.canRead())) {
                        if (f.isFile()) {
                            fileList.add(f);
                        } else if (f.isDirectory()) {
                            folderList.add(f);
                        }
                    }
                }
                playFiles(fileList);
            }
        } else if (data instanceof String) {
            String files = (String) data;
            if ((files.length() > 0)) {
                ArrayList fileList = new ArrayList();
                ArrayList folderList = new ArrayList();
                StringTokenizer st = new StringTokenizer(files, System.getProperty("line.separator"));
                while (st.hasMoreTokens()) {
                    String path = st.nextToken();
                    if (path.startsWith("file://")) {
                        path = path.substring(7, path.length());
                        if (path.endsWith("\r")) {
                            path = path.substring(0, (path.length() - 1));
                        }
                    }
                    File f = new File(path);
                    if ((f.exists()) && (f.canRead())) {
                        if (f.isFile()) {
                            fileList.add(f);
                        } else if (f.isDirectory()) {
                            folderList.add(f);
                        }
                    }
                }
                playFiles(fileList);
            }
        } else {
            log.info("Unknown dropped objects");
        }
    }

    /**
     * Play files from a list.
     * 
     * @param files
     */
    protected void playFiles(List files) {
        if (files.size() > 0) {
            playlist.removeAllItems();
            ListIterator li = files.listIterator();
            while (li.hasNext()) {
                File file = (File) li.next();
                PlaylistItem pli = null;
                if (file != null) {
                    pli = new PlaylistItem(file.getName(), file.getAbsolutePath(), -1, true);
                    if (pli != null) {
                        playlist.appendItem(pli);
                    }
                }
            }
            playlist.nextCursor();
            playlistUI.initPlayList();
            setCurrentSong(playlist.getCursor());
        }
    }

    /**
     * Sets the current song to play and start playing if needed.
     * 
     * @param pli
     */
    public void setCurrentSong(PlaylistItem pli) {
        int playerStateMem = playerState;
        if ((playerState == PAUSE) || (playerState == PLAY)) {
            try {
                theSoundPlayer.stop();
            } catch (BasicPlayerException e) {
                log.error("Cannot stop", e);
            }
            playerState = STOP;
            secondsAmount = 0;
            ui.getAcPlayIcon().setIcon(2);
            ui.getAcTimeIcon().setIcon(0);
        }
        playerState = OPEN;
        if (pli != null) {
            pli.getTagInfo();
            currentSongName = pli.getFormattedName();
            currentFileOrURL = pli.getLocation();
            currentIsFile = pli.isFile();
            currentPlaylistItem = pli;
        } else {
            if (config.isRepeatEnabled()) {
                if (playlist != null) {
                    if (playlist.getPlaylistSize() > 0) {
                        playlist.begin();
                        PlaylistItem rpli = playlist.getCursor();
                        if (rpli != null) {
                            rpli.getTagInfo();
                            currentSongName = rpli.getFormattedName();
                            currentFileOrURL = rpli.getLocation();
                            currentIsFile = rpli.isFile();
                            currentPlaylistItem = rpli;
                        }
                    } else {
                        currentSongName = Skin.TITLETEXT;
                        currentFileOrURL = null;
                        currentIsFile = false;
                        currentPlaylistItem = null;
                    }
                }
            } else {
                currentSongName = Skin.TITLETEXT;
                currentFileOrURL = null;
                currentIsFile = false;
                currentPlaylistItem = null;
            }
        }
        if (currentIsFile == true) {
            ui.getAcPosBar().setEnabled(true);
            ui.getAcPosBar().setHideThumb(false);
        } else {
            config.setLastURL(currentFileOrURL);
            ui.getAcPosBar().setEnabled(false);
            ui.getAcPosBar().setHideThumb(true);
        }
        titleText = currentSongName.toUpperCase();
        showMessage(titleText);
        if ((playerStateMem == PLAY && playerState != PLAY) || (playerStateMem == PAUSE)) {
            IWork w = new IWork() {

                public void execute() {
                    processPlay(MouseEvent.BUTTON1_MASK);
                }
            };
            doWork(w);
        }
    }

    /**
     * Display text in title area.
     * 
     * @param str
     */
    public void showTitle(String str) {
        if (str != null) {
            currentTitle = str;
            titleScrollLabel = null;
            scrollIndex = 0;
            scrollRight = true;
            if (str.length() > TEXT_LENGTH_MAX) {
                int a = ((str.length()) - (TEXT_LENGTH_MAX)) + 1;
                titleScrollLabel = new String[a];
                for (int k = 0; k < a; k++) {
                    String sText = str.substring(k, TEXT_LENGTH_MAX + k);
                    titleScrollLabel[k] = sText;
                }
                str = str.substring(0, TEXT_LENGTH_MAX);
            }
            ui.getAcTitleLabel().setAcText(str);
        }
    }

    /**
     * Shows message in title an updates bitRate,sampleRate, Mono/Stereo,time
     * features.
     * 
     * @param txt
     */
    public void showMessage(String txt) {
        showTitle(txt);
        ui.getAcSampleRateLabel().setAcText("  ");
        ui.getAcBitRateLabel().setAcText("   ");
        ui.getAcStereoIcon().setIcon(0);
        ui.getAcMonoIcon().setIcon(0);
        ui.getAcMinuteH().setAcText("0");
        ui.getAcMinuteL().setAcText("0");
        ui.getAcSecondH().setAcText("0");
        ui.getAcSecondL().setAcText("0");
    }

    /**
     * Toggle playlistUI.
     */
    protected void togglePlaylist() {
        if (ui.getAcPlaylist().isSelected()) {
            miPlaylist.setState(true);
            config.setPlaylistEnabled(true);
            loader.togglePlaylist(true);
        } else {
            miPlaylist.setState(false);
            config.setPlaylistEnabled(false);
            loader.togglePlaylist(false);
        }
    }

    /**
     * Toggle equalizerUI.
     */
    protected void toggleEqualizer() {
        if (ui.getAcEqualizer().isSelected()) {
            miEqualizer.setState(true);
            config.setEqualizerEnabled(true);
            loader.toggleEqualizer(true);
        } else {
            miEqualizer.setState(false);
            config.setEqualizerEnabled(false);
            loader.toggleEqualizer(false);
        }
    }

    /**
     * Returns a File from a filename.
     * 
     * @param file
     * @return
     */
    protected File openFile(String file) {
        return new File(file);
    }

    /**
     * Free resources and close the player.
     */
    protected void closePlayer() {
        try {
            if (theSoundPlayer != null) {
                fade(1500, true);
                if (playerState == PAUSE || playerState == PLAY) {
                    theSoundPlayer.stop();
                }
            }
        } catch (BasicPlayerException e) {
            log.error("Cannot stop", e);
        }
        if (theSoundPlayer != null) {
            config.setAudioDevice(((BasicPlayer) theSoundPlayer).getMixerName());
        }
        if (ui.getAcAnalyzer() != null) {
            if (ui.getAcAnalyzer().getDisplayMode() == SpectrumTimeAnalyzer.DISPLAY_MODE_OFF) {
                config.setVisualMode("off");
            } else if (ui.getAcAnalyzer().getDisplayMode() == SpectrumTimeAnalyzer.DISPLAY_MODE_SCOPE) {
                config.setVisualMode("oscillo");
            } else {
                config.setVisualMode("spectrum");
            }
        }
        if (playlist != null) {
            playlist.save("default.m3u");
            config.setPlaylistFilename("default.m3u");
        }
        loader.close();
    }

    /**
     * Return current title in player.
     * 
     * @return
     */
    public String getCurrentTitle() {
        return currentTitle;
    }

    /**
     * Try to compute time length in milliseconds.
     * @param properties
     * @return
     */
    public long getTimeLengthEstimation(Map properties) {
        long milliseconds = -1;
        int byteslength = -1;
        if (properties != null) {
            if (properties.containsKey("audio.length.bytes")) {
                byteslength = ((Integer) properties.get("audio.length.bytes")).intValue();
            }
            if (properties.containsKey("duration")) {
                milliseconds = (int) (((Long) properties.get("duration")).longValue()) / 1000;
            } else {
                int bitspersample = -1;
                int channels = -1;
                float samplerate = -1.0f;
                int framesize = -1;
                if (properties.containsKey("audio.samplesize.bits")) {
                    bitspersample = ((Integer) properties.get("audio.samplesize.bits")).intValue();
                }
                if (properties.containsKey("audio.channels")) {
                    channels = ((Integer) properties.get("audio.channels")).intValue();
                }
                if (properties.containsKey("audio.samplerate.hz")) {
                    samplerate = ((Float) properties.get("audio.samplerate.hz")).floatValue();
                }
                if (properties.containsKey("audio.framesize.bytes")) {
                    framesize = ((Integer) properties.get("audio.framesize.bytes")).intValue();
                }
                if (bitspersample > 0) {
                    milliseconds = (int) (1000.0f * byteslength / (samplerate * channels * (bitspersample / 8)));
                } else {
                    milliseconds = (int) (1000.0f * byteslength / (samplerate * framesize));
                }
            }
        }
        return milliseconds;
    }

    /**
     * Simulates "Play" selection.
     */
    public void pressStart() {
        ui.getAcPlay().doClick();
    }

    /**
     * Simulates "Pause" selection.
     */
    public void pressPause() {
        ui.getAcPause().doClick();
    }

    /**
     * Simulates "Stop" selection.
     */
    public void pressStop() {
        ui.getAcStop().doClick();
    }

    /**
     * Simulates "Next" selection.
     */
    public void pressNext() {
        ui.getAcNext().doClick();
    }

    /**
     * Simulates "Previous" selection.
     */
    public void pressPrevious() {
        ui.getAcPrevious().doClick();
    }

    /**
     * Simulates "Eject" selection.
     */
    public void pressEject() {
        ui.getAcEject().doClick();
    }

    public String getCurrentSongName() {
        return currentSongName;
    }

    public int getCurrentSeconds() {
        return (secondsAmount > 0) ? (int) secondsAmount : 0;
    }

    public int getTotalTimeSeconds() {
        return (total > 0) ? (int) total : 0;
    }

    public boolean isPlaying() {
        return playerState == PLAY;
    }

    public boolean isPaused() {
        return playerState == PAUSE;
    }

    boolean repeat;

    boolean shuffle;

    public SongInfo getSongInfo() {
        System.out.format("current:%s,total:%s", getCurrentSeconds(), getTotalTimeSeconds());
        SongInfo info = new SongInfo(getCurrentSeconds(), getTotalTimeSeconds(), chmp(getCurrentTitle()), isPlaying(), isPaused());
        return info;
    }

    public boolean getRepeat() {
        return getConfig().isRepeatEnabled();
    }

    public boolean getShuffle() {
        return getConfig().isShuffleEnabled();
    }

    public int getVolume() {
        int max = getSkin().getAcVolume().getMaximum();
        int vol = getSkin().getAcVolume().getValue();
        return (vol * 255) / max;
    }

    public void performButtonAction(Buttons action) {
        switch(action) {
            case NEXT:
                fade(1000, true);
                pressNext();
                break;
            case PREVIOUS:
                fade(1000, true);
                pressPrevious();
                break;
            case PAUSE:
                fade(1000, true);
                pressPause();
                break;
            case STOP:
                fade(1000, true);
                pressStop();
                break;
            case PLAY:
                if (!isPlaying()) {
                    fade(1000, false);
                    pressStart();
                }
                break;
            default:
        }
    }

    public void setRepeat(boolean value) {
        getSkin().getAcRepeat().setSelected(value);
        getConfig().setRepeatEnabled(value);
    }

    public void setShuffle(boolean value) {
        getSkin().getAcShuffle().setSelected(value);
        getConfig().setShuffleEnabled(value);
    }

    public void setVolume(int value) {
        int volume = (value * getSkin().getAcVolume().getMaximum()) / 255;
        getSkin().getAcVolume().setValue(volume);
    }

    public void open() {
    }

    @SuppressWarnings("unchecked")
    public IPlaylist getIPlaylist() {
        Playlist plst = getPlaylist();
        Iterator<PlaylistItem> itemIterator = plst.getAllItems().iterator();
        List<com.jpark.jamse.player.api.PlaylistItem> items = new ArrayList<com.jpark.jamse.player.api.PlaylistItem>();
        while (itemIterator.hasNext()) {
            PlaylistItem item = itemIterator.next();
            System.out.println(String.format("item:%s,name:%s", item.getFormattedDisplayName(), item.getName()));
            String title = (item.getFormattedDisplayName() != null) ? item.getFormattedDisplayName() : item.getName();
            if (title == null) {
                title = item.getLocation();
            }
            items.add(new com.jpark.jamse.player.api.PlaylistItem(title, item.getLocation()));
        }
        com.jpark.jamse.player.api.Playlist list = new com.jpark.jamse.player.api.Playlist(items, plst.getSelectedIndex());
        return list;
    }

    public void setPlaylistIndex(int index) {
        fade(1000, true);
        getPlaylist().setCursor(index);
        setCurrentSong(getPlaylist().getCursor());
        getPlaylistUI().nextCursor();
        if (!isPlaying()) {
            pressStart();
        } else {
            fade(1000, false);
        }
    }

    public void seek(int seconds) {
        System.out.println("Seek:" + seconds + " sec");
        if (getSongInfo() != null && getSongInfo().getTotalTime() > 0) {
            fade(1000, true);
            double total = getSongInfo().getTotalTime();
            if (seconds >= 0 && seconds <= total) {
                double pos = (seconds > 0) ? (double) seconds / total : 0;
                processSeek(pos);
            }
            fade(2000, false);
        }
    }

    public DetailInfo getDetailInfo() {
        if (dInfo == null) {
            dInfo = new DetailInfo(0, 0, 0);
        }
        return dInfo;
    }

    public void forward() {
        fade(1000, true);
        movePos(true);
        fade(2000, false);
    }

    public void rewind() {
        fade(1000, true);
        movePos(false);
        fade(2000, false);
    }

    public void stopAtEndOfTrack() {
        posValueJump = true;
        posDragging = true;
        ui.getAcPosBar().setValue(Skin.POSBARMAX);
        pressStop();
    }

    private void movePos(boolean forward) {
        int posOld = ui.getAcPosBar().getValue();
        int posNew = 0;
        if (forward) {
            posNew = posOld + (Skin.POSBARMAX / 10);
            if (posNew > Skin.POSBARMAX) {
                posNew = Skin.POSBARMAX - (Skin.POSBARMAX * 2 / 100);
            }
        } else {
            posNew = posOld - (Skin.POSBARMAX / 10);
            if (posNew < 0) {
                posNew = 0;
            }
        }
        posValueJump = true;
        posDragging = true;
        ui.getAcPosBar().setValue(posNew);
    }

    private String chmp(String title) {
        return title.trim();
    }

    public void fade(long time, boolean out) {
        int gainValue = ui.getAcVolume().getValue();
        int maxGain = ui.getAcVolume().getMaximum();
        double vol = (double) gainValue / (double) maxGain;
        double x = Math.sqrt(vol);
        double cycles = time / 100;
        for (int i = 1; i <= cycles; i++) {
            double gain = out ? (vol - vol / cycles * i) : Math.pow(x * i / cycles, 2);
            if (i == cycles) {
                gain = out ? 0 : vol;
            }
            try {
                theSoundPlayer.setGain(gain);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void doWork(IWork work) {
        worker.getQueue().clear();
        worker.getQueue().add(work);
    }

    class Worker extends Thread {

        private ArrayBlockingQueue<IWork> queue;

        public Worker() {
            queue = new ArrayBlockingQueue<IWork>(2);
            this.setDaemon(true);
            this.start();
        }

        ArrayBlockingQueue<IWork> getQueue() {
            return queue;
        }

        public void run() {
            while (true) {
                try {
                    IWork work = queue.take();
                    work.execute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
