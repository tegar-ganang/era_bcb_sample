package dmxeffects.sound;

import com.trolltech.qt.core.QObject;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QWidget;
import dmxeffects.Main;
import dmxeffects.Module;
import dmxeffects.dmx.ControlChannel;
import dmxeffects.dmx.InvalidChannelValueException;

/**
 * Sound module main file. Provides all interfaces from external classes to the
 * functionality provided by this module.
 * 
 * @author chris
 * 
 */
public class SoundModule extends QObject implements Module {

    private static final int CHANNELS_REQUIRED = 2;

    private static final String MODULE_NAME = "Sound Module";

    private transient int firstChannel;

    private final transient SoundTrack[] trackArray;

    private final transient ControlChannel[] controls = new ControlChannel[CHANNELS_REQUIRED];

    public transient Signal1<Integer> trackCueSignal;

    public transient Signal1<Integer> playSignal;

    public transient Signal1<Integer> stopSignal;

    public transient Signal1<SoundTrack> playerCueSignal;

    public transient Signal0 playerPlaySignal;

    public transient Signal0 playerStopSignal;

    private final transient Player soundPlayer;

    private transient QMenu soundMenu;

    private transient QAction chAssocAction;

    private transient QAction testSoundAction;

    private transient QAction addTrackAction;

    private transient QAction editTrackAction;

    private transient QAction deleteTrackAction;

    private transient QAction clearTracksAction;

    /**
     * Create new SoundModule.
     * 
     */
    public SoundModule() {
        super();
        firstChannel = -1;
        trackArray = new SoundTrack[256];
        playerCueSignal = new Signal1<SoundTrack>();
        playerPlaySignal = new Signal0();
        playerStopSignal = new Signal0();
        soundPlayer = new Player();
        playerCueSignal.connect(soundPlayer, "cueTrack(SoundTrack)");
        playerPlaySignal.connect(soundPlayer, "play()");
        playerStopSignal.connect(soundPlayer, "stop()");
        final Thread playerThread = new Thread(soundPlayer);
        soundPlayer.moveToThread(playerThread);
        playerThread.start();
        controls[0] = new ControlChannel(1, MODULE_NAME);
        trackCueSignal = new Signal1<Integer>();
        trackCueSignal.connect(this, "cueTrack(Integer)");
        for (int i = 0; i < 256; i++) {
            try {
                controls[0].setSignal(i, trackCueSignal);
            } catch (InvalidChannelValueException e) {
                e.printStackTrace(System.err);
            }
        }
        controls[1] = new ControlChannel(2, MODULE_NAME);
        playSignal = new Signal1<Integer>();
        playSignal.connect(this, "startPlayback(Integer)");
        stopSignal = new Signal1<Integer>();
        stopSignal.connect(this, "stopPlayback(Integer)");
        try {
            controls[1].setSignal(10, playSignal);
            controls[1].setSignal(20, stopSignal);
        } catch (InvalidChannelValueException e1) {
            e1.printStackTrace(System.err);
        }
        try {
            createActions();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        createMenus();
    }

    public final void createActions() {
        chAssocAction = new QAction(tr("&Change DMX Association"), this);
        chAssocAction.setStatusTip(tr("Change DMX Channel Association for the sound module"));
        chAssocAction.triggered.connect(this, "setAssoc()");
        chAssocAction.setEnabled(false);
        testSoundAction = new QAction(tr("&Test Sound Output"), this);
        testSoundAction.setStatusTip(tr("Perform a test of the sound setup on this system"));
        testSoundAction.triggered.connect(this, "soundTest()");
        testSoundAction.setEnabled(false);
        addTrackAction = new QAction(tr("&Add Track"), this);
        addTrackAction.setStatusTip(tr("Add a new track the show"));
        addTrackAction.triggered.connect(this, "addTrack()");
        addTrackAction.setEnabled(false);
        editTrackAction = new QAction(tr("&Edit Track"), this);
        editTrackAction.setStatusTip(tr("Edit an existing track within the show"));
        editTrackAction.triggered.connect(this, "editTrack()");
        editTrackAction.setEnabled(false);
        deleteTrackAction = new QAction(tr("&Delete Track"), this);
        deleteTrackAction.setStatusTip(tr("Remove a track from the show"));
        deleteTrackAction.triggered.connect(this, "deleteTrack()");
        deleteTrackAction.setEnabled(false);
        clearTracksAction = new QAction(tr("&Clear Tracks"), this);
        clearTracksAction.setStatusTip(tr("Remove all tracks from the show"));
        clearTracksAction.triggered.connect(this, "clearTracks()");
        clearTracksAction.setEnabled(false);
    }

    public final void createMenus() {
        soundMenu = new QMenu(tr("&Sound"));
        soundMenu.addAction(chAssocAction);
        soundMenu.addAction(testSoundAction);
        soundMenu.addSeparator();
        soundMenu.addAction(addTrackAction);
        soundMenu.addAction(editTrackAction);
        soundMenu.addAction(deleteTrackAction);
        soundMenu.addAction(clearTracksAction);
    }

    public QMenu getMenu() {
        return soundMenu;
    }

    public String getName() {
        return MODULE_NAME;
    }

    public QWidget getWidget() {
        return null;
    }

    public String getWidgetTitle() {
        return null;
    }

    /**
     * Provide the number of channels required for this module.
     * 
     * @return The requisite number of channels.
     */
    public int getChannelsRequired() {
        return CHANNELS_REQUIRED;
    }

    public void dmxListenerEnabled() {
        setAssoc();
        Main.getInstance().getDMX().getUniverse().dmxValueUpdater.connect(this, "dmxInput(Integer, Integer)");
        Main.getInstance().getDMX().getUniverse().assocRemUpdater.connect(this, "assocUpdate(Integer, Integer)");
        if (Main.getInstance().getProgramMode()) {
            chAssocAction.setEnabled(true);
        }
    }

    /**
     * Handle DMX Input signals sent by the Universe. All input is assumed
     * to have been validated due to the signal being sent by the Universe
     * class.
     * 
     * @param channelNumber
     *                Number of the channel for which input has been
     *                receieved
     * @param channelValue
     *                Value this channel now holds.
     */
    public void dmxInput(final Integer channelNumber, final Integer channelValue) {
        final int chanNum = channelNumber.intValue();
        if ((firstChannel != -1) && (chanNum >= firstChannel) && (chanNum < firstChannel + CHANNELS_REQUIRED)) {
            final int chanVal = channelValue.intValue();
            try {
                controls[chanNum].trigger(chanVal);
            } catch (ArrayIndexOutOfBoundsException AOB) {
            } catch (NullPointerException NPE) {
            } catch (InvalidChannelValueException ICVE) {
                ICVE.printStackTrace(System.err);
            }
        }
    }

    /**
     * Handle channel association removal signals sent by Universe. This
     * allows the module to remove its own if appropriate.
     * 
     * @param firstChannel
     *                The first of the channels being revoked.
     * @param range
     *                The range of channels being revoked.
     */
    public void assocUpdate(final Integer firstChannel, final Integer range) {
    }

    public void programMode() {
        try {
            if (Main.getInstance().getDMX().getListenerStatus()) {
                chAssocAction.setEnabled(true);
            }
        } catch (NullPointerException npe) {
        }
        addTrackAction.setEnabled(true);
        editTrackAction.setEnabled(true);
        deleteTrackAction.setEnabled(true);
        clearTracksAction.setEnabled(true);
    }

    public void runMode() {
        chAssocAction.setEnabled(false);
        addTrackAction.setEnabled(false);
        editTrackAction.setEnabled(false);
        deleteTrackAction.setEnabled(false);
        clearTracksAction.setEnabled(false);
    }

    /**
     * Set the channel association for this module
     * 
     */
    public void setAssoc() {
    }

    /**
     * Test the sound system.
     * 
     */
    public void soundTest() {
    }

    /**
     * Add a new track into the show.
     * 
     */
    public void addTrack() {
    }

    /**
     * Edit an existing track within the show.
     * 
     */
    public void editTrack() {
    }

    /**
     * Remove a track from the show.
     * 
     */
    public void deleteTrack() {
    }

    /**
     * Remove all tracks from the show.
     * 
     */
    public void clearTracks() {
    }

    /**
     * Cue a track for playback
     * 
     * @param val
     *                DMX Value of the track to be cued.
     */
    public void cueTrack(final Integer val) {
        try {
            playerCueSignal.emit(trackArray[val.intValue()]);
        } catch (ArrayIndexOutOfBoundsException AOB) {
            AOB.printStackTrace(System.err);
        }
    }

    /**
     * Start playback.
     * 
     * @param val
     *                Not used.
     */
    public void startPlayback(final Integer val) {
        playerPlaySignal.emit();
    }

    /**
     * Stop playback
     * 
     * @param val
     *                Not used.
     */
    public void stopPlayback(final Integer val) {
        playerStopSignal.emit();
    }
}
