package de.sciss.eisenkraut.session;

import java.awt.EventQueue;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import de.sciss.eisenkraut.Main;
import de.sciss.eisenkraut.edit.BasicCompoundEdit;
import de.sciss.eisenkraut.edit.EditSetTimelineLength;
import de.sciss.eisenkraut.edit.TimelineVisualEdit;
import de.sciss.eisenkraut.edit.UndoManager;
import de.sciss.eisenkraut.gui.BlendingAction;
import de.sciss.eisenkraut.io.AudioTrail;
import de.sciss.eisenkraut.io.BlendContext;
import de.sciss.eisenkraut.io.DecimatedSonaTrail;
import de.sciss.eisenkraut.io.DecimatedTrail;
import de.sciss.eisenkraut.io.DecimatedWaveTrail;
import de.sciss.eisenkraut.io.MarkerTrail;
import de.sciss.eisenkraut.net.OSCRoot;
import de.sciss.eisenkraut.net.OSCRouter;
import de.sciss.eisenkraut.net.OSCRouterWrapper;
import de.sciss.eisenkraut.net.RoutedOSCMessage;
import de.sciss.eisenkraut.realtime.Transport;
import de.sciss.eisenkraut.render.FilterDialog;
import de.sciss.eisenkraut.render.RenderPlugIn;
import de.sciss.eisenkraut.render.Replace;
import de.sciss.eisenkraut.timeline.AudioTrack;
import de.sciss.eisenkraut.timeline.AudioTracks;
import de.sciss.eisenkraut.timeline.MarkerTrack;
import de.sciss.eisenkraut.timeline.Timeline;
import de.sciss.eisenkraut.timeline.Track;
import de.sciss.gui.GUIUtil;
import de.sciss.gui.MenuAction;
import de.sciss.gui.ParamField;
import de.sciss.gui.SpringPanel;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.Span;
import de.sciss.timebased.Trail;
import de.sciss.util.DefaultUnitTranslator;
import de.sciss.util.Flag;
import de.sciss.util.Param;
import de.sciss.util.ParamSpace;
import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractCompoundEdit;
import de.sciss.common.BasicDocument;
import de.sciss.common.BasicWindowHandler;
import de.sciss.common.ProcessingThread;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 16-Oct-08
 *
 *	@todo		try get rid of the GUI stuff in here
 */
public class Session extends BasicDocument implements OSCRouter {

    private final AudioFileDescr displayAFD;

    private AudioFileDescr[] afds;

    private String name;

    public final Timeline timeline;

    protected AudioTrail at = null;

    private DecimatedWaveTrail dwt = null;

    private DecimatedSonaTrail dst = null;

    public final MarkerTrail markers;

    public final MarkerTrack markerTrack;

    private static final int[] waveDecims = { 8, 12, 16 };

    /**
	 *  Bitmask for putting a lock on the <code>timeline</code>
	 */
    public static final int DOOR_TIME = 0x01;

    /**
	 *  Bitmask for putting a lock on a <code>AudioTrail</code>
	 */
    public static final int DOOR_MTE = 0x02;

    /**
	 *  Bitmask for putting a lock on the channel tracks
	 */
    public static final int DOOR_TRACKS = 0x04;

    public static final int DOOR_ALL = DOOR_TIME | DOOR_TRACKS | DOOR_MTE;

    public final SessionCollection tracks = new SessionCollection();

    public final AudioTracks audioTracks;

    public final SessionCollection selectedTracks = new SessionCollection();

    private Transport transport = null;

    private DocumentFrame frame = null;

    private final ActionSave actionSave;

    private final ActionCut actionCut;

    protected final ActionCopy actionCopy;

    private final ActionPaste actionPaste;

    private final ActionDelete actionDelete;

    private final ActionSilence actionSilence;

    private final ActionTrim actionTrim;

    private final UndoManager undo = new UndoManager(this);

    private boolean dirty = false;

    public static final int EDIT_INSERT = 0;

    public static final int EDIT_OVERWRITE = 1;

    public static final int EDIT_MIX = 2;

    private static final String[] EDITMODES = { "insert", "overwrite", "mix" };

    private int editMode = EDIT_INSERT;

    private static int nodeIDAlloc = 0;

    private final int nodeID;

    private final OSCRouterWrapper osc;

    private final BlendingAction blending;

    protected ProcessingThread pt = null;

    private Session(AudioFileDescr[] afds, boolean createOSC) throws IOException {
        this(afds, null, createOSC);
    }

    private Session(AudioFileDescr afd, boolean createOSC) throws IOException {
        this(new AudioFileDescr[] { afd }, afd, createOSC);
    }

    private Session(AudioFileDescr[] afds, AudioFileDescr displayAFD, boolean createOSC) throws IOException {
        super();
        this.afds = afds;
        if (displayAFD == null) {
            this.displayAFD = new AudioFileDescr();
            autoCreateDisplayDescr();
        } else {
            this.displayAFD = displayAFD;
            name = displayAFD.file == null ? null : displayAFD.file.getName();
        }
        nodeID = ++nodeIDAlloc;
        if (createOSC) {
            osc = new OSCRouterWrapper(null, this);
        } else {
            osc = null;
        }
        timeline = new Timeline(this);
        markerTrack = new MarkerTrack(this);
        markers = (MarkerTrail) markerTrack.getTrail();
        markers.copyFromAudioFile(afds[0]);
        tracks.add(null, markerTrack);
        audioTracks = new AudioTracks(this);
        actionSave = new ActionSave();
        actionCut = new ActionCut();
        actionCopy = new ActionCopy();
        actionPaste = new ActionPaste();
        actionDelete = new ActionDelete();
        actionSilence = new ActionSilence();
        actionTrim = new ActionTrim();
        timeline.setRate(this, this.displayAFD.rate);
        timeline.setLength(this, this.displayAFD.length);
        timeline.setVisibleSpan(this, new Span(0, this.displayAFD.length));
        selectedTracks.add(this, markerTrack);
        blending = new BlendingAction(timeline, null);
    }

    /**
	 * 	Checks if a process is currently running. This method should be called
	 * 	before launching a process using the <code>start()</code> method.
	 * 	If a process is ongoing, this method waits for a default timeout period
	 * 	for the thread to finish.
	 * 
	 *	@return	<code>true</code> if a new process can be launched, <code>false</code>
	 *			if a previous process is ongoing and a new process cannot be launched
	 *	@throws	IllegalMonitorStateException	if called from outside the event thread
	 *	@synchronization	must be called in the event thread
	 */
    public boolean checkProcess() {
        return checkProcess(500);
    }

    /**
	 * 	Checks if a process is currently running. This method should be called
	 * 	before launching a process using the <code>start()</code> method.
	 * 	If a process is ongoing, this method waits for a given timeout period
	 * 	for the thread to finish.
	 * 
	 * 	@param	timeout	the maximum duration in milliseconds to wait for an ongoing process
	 *	@return	<code>true</code> if a new process can be launched, <code>false</code>
	 *			if a previous process is ongoing and a new process cannot be launched
	 *	@throws	IllegalMonitorStateException	if called from outside the event thread
	 *	@synchronization	must be called in the event thread
	 */
    public boolean checkProcess(int timeout) {
        if (!EventQueue.isDispatchThread()) throw new IllegalMonitorStateException();
        if (pt == null) return true;
        if (timeout == 0) return false;
        pt.sync(timeout);
        return ((pt == null) || !pt.isRunning());
    }

    public void cancelProcess(boolean sync) {
        if (!EventQueue.isDispatchThread()) throw new IllegalMonitorStateException();
        if (pt == null) return;
        pt.cancel(sync);
    }

    public String getProcessName() {
        if (!EventQueue.isDispatchThread()) throw new IllegalMonitorStateException();
        if (pt == null) return null;
        return pt.getName();
    }

    /**
	 * 	Starts a <code>ProcessingThread</code>. Only one thread
	 * 	can exist at a time. To ensure that no other thread is running,
	 * 	call <code>checkProcess()</code>.
	 * 
	 * 	@param	pt	the thread to launch
	 * 	@throws	IllegalMonitorStateException	if called from outside the event thread
	 * 	@throws	IllegalStateException			if another process is still running
	 * 	@see	#checkProcess()
	 * 	@synchronization	must be called in the event thread
	 */
    public void start(ProcessingThread process) {
        if (!EventQueue.isDispatchThread()) throw new IllegalMonitorStateException();
        if (this.pt != null) throw new IllegalStateException("Process already running");
        pt = process;
        pt.addListener(new ProcessingThread.Listener() {

            public void processStarted(ProcessingThread.Event e) {
            }

            public void processStopped(ProcessingThread.Event e) {
                pt = null;
            }
        });
        pt.start();
    }

    public BlendingAction getBlendingAction() {
        return blending;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setEditMode(int mode) {
        editMode = mode;
    }

    public int getEditMode() {
        return editMode;
    }

    public void createTransport() {
        if (transport != null) throw new IllegalStateException("Transport already exists");
        transport = new Transport(this);
    }

    public Transport getTransport() {
        return transport;
    }

    public void createFrame() {
        if (frame != null) throw new IllegalStateException("Frame already exists");
        frame = new DocumentFrame(this);
    }

    public DocumentFrame getFrame() {
        return frame;
    }

    private void setAudioTrail(Object source, AudioTrail at) {
        this.at = at;
        if (!audioTracks.isEmpty()) throw new IllegalStateException("Cannot call repeatedly");
        final List collNewTracks = new ArrayList();
        final int numChannels = at.getChannelNum();
        final double deltaAngle = 360.0 / numChannels;
        final double startAngle = numChannels < 2 ? 0.0 : -deltaAngle / 2;
        AudioTrack t;
        audioTracks.setTrail(at);
        for (int ch = 0; ch < at.getChannelNum(); ch++) {
            t = new AudioTrack(audioTracks, ch);
            t.setName(String.valueOf(ch + 1));
            t.getMap().putValue(source, AudioTrack.MAP_KEY_PANAZIMUTH, new Double(startAngle + ch * deltaAngle));
            collNewTracks.add(t);
        }
        audioTracks.addAll(source, collNewTracks);
        tracks.addAll(source, collNewTracks);
        selectedTracks.addAll(source, collNewTracks);
        updateTitle();
    }

    public AudioTrail getAudioTrail() {
        return at;
    }

    public DecimatedWaveTrail getDecimatedWaveTrail() {
        return dwt;
    }

    public DecimatedSonaTrail getDecimatedSonaTrail() {
        return dst;
    }

    public void setDescr(AudioFileDescr[] afds) {
        this.afds = afds;
        autoCreateDisplayDescr();
        updateTitle();
    }

    private void autoCreateDisplayDescr() {
        if (afds.length == 0) {
            displayAFD.file = null;
            name = null;
        } else {
            final AudioFileDescr proto = afds[0];
            displayAFD.type = proto.type;
            displayAFD.rate = proto.rate;
            displayAFD.bitsPerSample = proto.bitsPerSample;
            displayAFD.sampleFormat = proto.sampleFormat;
            displayAFD.length = proto.length;
            displayAFD.channels = proto.channels;
            if (proto.file == null) {
                displayAFD.file = null;
                name = null;
            } else {
                final String pname = proto.file.getName();
                int left = pname.length();
                int right = pname.length();
                String name2;
                int trunc;
                displayAFD.type = proto.type;
                displayAFD.rate = proto.rate;
                displayAFD.bitsPerSample = proto.bitsPerSample;
                displayAFD.sampleFormat = proto.sampleFormat;
                displayAFD.length = proto.length;
                displayAFD.channels = proto.channels;
                for (int i = 1; i < afds.length; i++) {
                    name2 = afds[i].file.getName();
                    displayAFD.channels += afds[i].channels;
                    for (trunc = 0; trunc < Math.min(name2.length(), left); trunc++) {
                        if (!(name2.charAt(trunc) == pname.charAt(trunc))) break;
                    }
                    left = trunc;
                    for (trunc = 0; trunc < Math.min(name2.length(), right); trunc++) {
                        if (!(name2.charAt(name2.length() - trunc - 1) == pname.charAt(pname.length() - trunc - 1))) break;
                    }
                    right = trunc;
                }
                if (left >= pname.length() - right) {
                    displayAFD.file = afds[0].file;
                    name = displayAFD.file.getName();
                } else {
                    final StringBuffer strBuf = new StringBuffer();
                    strBuf.append(pname.substring(0, left));
                    for (int i = 0; i < afds.length; i++) {
                        strBuf.append(i == 0 ? '[' : ',');
                        name2 = afds[i].file.getName();
                        strBuf.append(name2.substring(left, name2.length() - right));
                    }
                    strBuf.append(']');
                    final int idxBraClose = strBuf.length();
                    strBuf.append(pname.substring(pname.length() - right));
                    displayAFD.file = new File(afds[0].file.getParentFile(), strBuf.toString());
                    if ((idxBraClose - left) > 25) {
                        strBuf.replace(left + 12, idxBraClose - 13, "…");
                    }
                    name = strBuf.toString();
                }
            }
        }
    }

    public DecimatedWaveTrail createDecimatedWaveTrail() throws IOException {
        if (dwt == null) {
            dwt = new DecimatedWaveTrail(at, DecimatedTrail.MODEL_FULLWAVE_PEAKRMS, waveDecims);
        }
        return dwt;
    }

    public DecimatedSonaTrail createDecimatedSonaTrail() throws IOException {
        if (dst == null) {
            dst = new DecimatedSonaTrail(at, DecimatedTrail.MODEL_SONA);
        }
        return dst;
    }

    public AudioFileDescr getDisplayDescr() {
        return displayAFD;
    }

    public String getName() {
        return name;
    }

    public AudioFileDescr[] getDescr() {
        return afds;
    }

    private void updateTitle() {
        if (frame != null) frame.updateTitle();
    }

    public ProcessingThread procDelete(String procName, Span span, int mode) {
        return actionDelete.initiate(procName, span, mode);
    }

    public ProcessingThread procSave(String procName, Span span, AudioFileDescr[] targetAFDs, int[] channelMap, boolean saveMarkers, boolean asCopy) {
        return actionSave.initiate(procName, span, targetAFDs, channelMap, saveMarkers, asCopy);
    }

    public MenuAction getCutAction() {
        return actionCut;
    }

    public MenuAction getCopyAction() {
        return actionCopy;
    }

    public MenuAction getPasteAction() {
        return actionPaste;
    }

    public MenuAction getDeleteAction() {
        return actionDelete;
    }

    public MenuAction getSilenceAction() {
        return actionSilence;
    }

    public MenuAction getTrimAction() {
        return actionTrim;
    }

    public ProcessingThread insertSilence(long pos, long numFrames) {
        return actionSilence.initiate(pos, numFrames);
    }

    public ProcessingThread closeDocument(boolean force, Flag wasClosed) {
        return frame.closeDocument(force, wasClosed);
    }

    public ClipboardTrackList getSelectionAsTrackList() {
        return actionCopy.getSelectionAsTrackList();
    }

    public ProcessingThread pasteTrackList(ClipboardTrackList tl, long insertPos, String procName, int mode) {
        return actionPaste.initiate(tl, insertPos, procName, mode);
    }

    public static Session newEmpty(AudioFileDescr afd) throws IOException {
        return newEmpty(afd, true, true);
    }

    public static Session newEmpty(AudioFileDescr afd, boolean createTransport, boolean createOSC) throws IOException {
        final Session doc = new Session(afd, createOSC);
        final AudioTrail at = AudioTrail.newFrom(afd);
        doc.setAudioTrail(null, at);
        if (createTransport) doc.createTransport();
        return doc;
    }

    public static Session newFrom(File path) throws IOException {
        return newFrom(path, true, true);
    }

    public static Session newFrom(File path, boolean createTransport, boolean createOSC) throws IOException {
        final AudioFile af = AudioFile.openAsRead(path);
        final AudioFileDescr afd = af.getDescr();
        Session doc = null;
        AudioTrail at = null;
        try {
            af.readMarkers();
            at = AudioTrail.newFrom(af);
            doc = new Session(afd, createOSC);
            doc.setAudioTrail(null, at);
            if (createTransport) doc.createTransport();
            return doc;
        } catch (IOException e1) {
            if (at != null) {
                at.dispose();
            } else {
                af.cleanUp();
            }
            throw e1;
        }
    }

    public static Session newFrom(File[] paths) throws IOException {
        return newFrom(paths, true, true);
    }

    public static Session newFrom(File[] paths, boolean createTransport, boolean createOSC) throws IOException {
        final AudioFile[] afs = new AudioFile[paths.length];
        final AudioFileDescr[] afds = new AudioFileDescr[paths.length];
        AudioTrail at = null;
        Session doc = null;
        try {
            for (int i = 0; i < paths.length; i++) {
                afs[i] = AudioFile.openAsRead(paths[i]);
                afds[i] = afs[i].getDescr();
                if (i > 0) {
                    if ((afds[i].length != afds[0].length) || (afds[i].rate != afds[0].rate) || (afds[i].bitsPerSample != afds[0].bitsPerSample) || (afds[i].sampleFormat != afds[0].sampleFormat)) {
                        throw new IOException(getResourceString("errHeadersNotMatching"));
                    }
                }
                afs[i].readMarkers();
            }
            at = AudioTrail.newFrom(afs);
            doc = new Session(afds, createOSC);
            doc.setAudioTrail(null, at);
            if (createTransport) doc.createTransport();
            return doc;
        } catch (IOException e1) {
            if (at != null) {
                at.dispose();
            } else {
                for (int i = 0; i < paths.length; i++) {
                    if (afs[i] != null) afs[i].cleanUp();
                }
            }
            throw e1;
        }
    }

    protected static String getResourceString(String key) {
        return AbstractApplication.getApplication().getResourceString(key);
    }

    public BlendContext createBlendContext(long maxLeft, long maxRight, boolean hasSelectedAudio) {
        if (!hasSelectedAudio || ((maxLeft == 0L) && (maxRight == 0L))) {
            return null;
        } else {
            return blending.createBlendContext(maxLeft, maxRight);
        }
    }

    protected void discardEditsAndClipboard() {
        undo.discardAllEdits();
        ClipboardTrackList.disposeAll(this);
    }

    public String oscGetPathComponent() {
        return null;
    }

    public void oscRoute(RoutedOSCMessage rom) {
        osc.oscRoute(rom);
    }

    public void oscAddRouter(OSCRouter subRouter) {
        if (osc != null) osc.oscAddRouter(subRouter);
    }

    public void oscRemoveRouter(OSCRouter subRouter) {
        if (osc != null) osc.oscRemoveRouter(subRouter);
    }

    public void oscCmd_close(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }
        final ProcessingThread proc;
        final boolean force;
        try {
            if (rom.msg.getArgCount() > 1) {
                force = ((Number) rom.msg.getArg(1)).intValue() != 0;
            } else {
                force = false;
            }
            proc = closeDocument(force, new Flag(false));
            if (proc != null) start(proc);
        } catch (IndexOutOfBoundsException e1) {
            OSCRoot.failedArgCount(rom);
            return;
        } catch (ClassCastException e1) {
            OSCRoot.failedArgType(rom, 1);
        }
    }

    public void oscCmd_activate(RoutedOSCMessage rom) {
        if (frame == null) {
            OSCRoot.failed(rom.msg, getResourceString("errWindowNotFound"));
        }
        frame.setVisible(true);
        frame.toFront();
    }

    public void oscCmd_cut(RoutedOSCMessage rom) {
        actionCut.perform();
    }

    public void oscCmd_copy(RoutedOSCMessage rom) {
        actionCopy.perform();
    }

    public void oscCmd_paste(RoutedOSCMessage rom) {
        actionPaste.perform();
    }

    public void oscCmd_delete(RoutedOSCMessage rom) {
        actionDelete.perform();
    }

    /**
	 *	"insertSilence", <numFrames>
	 */
    public void oscCmd_insertSilence(RoutedOSCMessage rom) {
        final long pos, numFrames;
        final ProcessingThread proc;
        int argIdx = 1;
        try {
            pos = timeline.getPosition();
            numFrames = Math.max(0, Math.min(timeline.getLength() - pos, ((Number) rom.msg.getArg(argIdx)).longValue()));
        } catch (IndexOutOfBoundsException e1) {
            OSCRoot.failedArgCount(rom);
            return;
        } catch (ClassCastException e1) {
            OSCRoot.failedArgType(rom, argIdx);
            return;
        }
        proc = actionSilence.initiate(pos, numFrames);
        if (proc != null) start(proc);
    }

    public void oscCmd_trim(RoutedOSCMessage rom) {
        actionTrim.perform();
    }

    /**
	 *	"editMode", <modeName>
	 */
    public void oscCmd_editMode(RoutedOSCMessage rom) {
        final String mode;
        int argIdx = 1;
        try {
            mode = rom.msg.getArg(argIdx).toString();
            for (int i = 0; i < EDITMODES.length; i++) {
                if (EDITMODES[i].equals(mode)) {
                    setEditMode(i);
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e1) {
            OSCRoot.failedArgCount(rom);
            return;
        } catch (ClassCastException e1) {
            OSCRoot.failedArgType(rom, argIdx);
            return;
        }
    }

    /**
	 *	Replaces the currently selected span with
	 *	the contents of a given audio file, applying
	 *	blending if activated.
	 *
	 *	"replace", <fileName>[, <fileOffset> ]
	 *
	 *	@todo	XXX FilterDialog should return a ProcessingThread, so we can properly close the file after pasting
	 */
    public void oscCmd_replace(RoutedOSCMessage rom) {
        final RenderPlugIn plugIn;
        final String fileName;
        final long startFrame;
        AudioFile af = null;
        int argIdx = 1;
        FilterDialog filterDlg;
        try {
            fileName = rom.msg.getArg(argIdx).toString();
            argIdx++;
            if (argIdx < rom.msg.getArgCount()) {
                startFrame = ((Number) rom.msg.getArg(argIdx)).longValue();
            } else {
                startFrame = 0;
            }
            af = AudioFile.openAsRead(new File(fileName));
            af.seekFrame(startFrame);
            plugIn = new Replace(af);
        } catch (IndexOutOfBoundsException e1) {
            OSCRoot.failedArgCount(rom);
            return;
        } catch (ClassCastException e1) {
            OSCRoot.failedArgType(rom, argIdx);
            return;
        } catch (IOException e1) {
            if (af != null) af.cleanUp();
            OSCRoot.failed(rom, e1);
            return;
        }
        filterDlg = (FilterDialog) AbstractApplication.getApplication().getComponent(Main.COMP_FILTER);
        if (filterDlg == null) {
            filterDlg = new FilterDialog();
        }
        filterDlg.process(plugIn, this, false, false);
    }

    public Object oscQuery_id() {
        return new Integer(getNodeID());
    }

    public Object oscQuery_dirty() {
        return new Integer(isDirty() ? 1 : 0);
    }

    public Object oscQuery_editMode() {
        return EDITMODES[getEditMode()];
    }

    public Object oscQuery_name() {
        return getName();
    }

    public Object oscQuery_file() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < afds.length; i++) {
            if (i > 0) sb.append(File.pathSeparator);
            sb.append(afds[i].file.getAbsolutePath());
        }
        return sb.toString();
    }

    public de.sciss.app.Application getApplication() {
        return AbstractApplication.getApplication();
    }

    public de.sciss.app.UndoManager getUndoManager() {
        return undo;
    }

    public void dispose() {
        discardEditsAndClipboard();
        if (osc != null) {
            osc.remove();
        }
        if (transport != null) {
            transport.quit();
            transport.dispose();
            transport = null;
        }
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        if (at != null) {
            at.dispose();
            at = null;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        if (!this.dirty == dirty) {
            this.dirty = dirty;
            updateTitle();
        }
    }

    private class ActionSave implements ProcessingThread.Client {

        protected ActionSave() {
        }

        /**
		 *  Initiate the save process.
		 *  Transport is stopped before, if it was running.
		 *  On success, undo history is purged and
		 *  <code>setModified</code> and <code>updateTitle</code>
		 *  are called, and the file is added to
		 *  the Open-Recent menu. Note that returned
		 *	process has not yet been started, as to allow
		 *	other objects to add listeners. So it's the
		 *	job of the caller to invoke the processing thread's
		 *	<code>start</code> method.
		 *
		 *  @synchronization	this method is to be called in the event thread
		 */
        protected ProcessingThread initiate(String procName, Span span, AudioFileDescr[] descrs, int[] channelMap, boolean saveMarkers, boolean asCopy) {
            final ProcessingThread proc;
            getTransport().stop();
            if (!checkProcess()) return null;
            proc = new ProcessingThread(this, getFrame(), procName);
            proc.putClientArg("afds", descrs);
            proc.putClientArg("doc", Session.this);
            proc.putClientArg("asCopy", new Boolean(asCopy));
            proc.putClientArg("chanMap", channelMap);
            proc.putClientArg("markers", new Boolean(saveMarkers));
            proc.putClientArg("span", span == null ? new Span(0, timeline.getLength()) : span);
            return proc;
        }

        /**
		 *	- wenn das audio file format marker unterstuetzt, kopiere die marker in das erste file
		 *	- fuer jedes file: wenn ein file gleichen namens existiert, erzeuge zunaechst ein temporaeres file
		 *	- oeffne alle files zum schreiben (AudioFile.openAsWrite)
		 *	- schreibe alle files (at.flatten)
		 *	- liefere ein array aller geschriebener files in client arg "afs"
		 */
        public int processRun(ProcessingThread context) throws IOException {
            final AudioFileDescr[] clientAFDs = (AudioFileDescr[]) context.getClientArg("afds");
            final int numFiles = clientAFDs.length;
            final Session doc = (Session) context.getClientArg("doc");
            final boolean saveMarkers = ((Boolean) context.getClientArg("markers")).booleanValue();
            final Span span = (Span) context.getClientArg("span");
            final int[] channelMap = (int[]) context.getClientArg("chanMap");
            final AudioTrail audioTrail = doc.getAudioTrail();
            final AudioFile[] afs = new AudioFile[numFiles];
            AudioFileDescr afdTemp;
            File tempF;
            context.putClientArg("afs", afs);
            if (saveMarkers) {
                if (clientAFDs[0].isPropertySupported(AudioFileDescr.KEY_MARKERS)) {
                    doc.markers.copyToAudioFile(clientAFDs[0], span);
                } else if (!doc.markers.isEmpty()) {
                    System.err.println("WARNING: markers are not saved in this file format!!!");
                }
            } else {
                clientAFDs[0].setProperty(AudioFileDescr.KEY_MARKERS, null);
            }
            for (int i = 0; i < numFiles; i++) {
                if (clientAFDs[i].file.exists()) {
                    tempF = File.createTempFile("eis", null, clientAFDs[i].file.getParentFile());
                    afdTemp = new AudioFileDescr(clientAFDs[i]);
                    afdTemp.file = tempF;
                    afs[i] = AudioFile.openAsWrite(afdTemp);
                } else {
                    afs[i] = AudioFile.openAsWrite(clientAFDs[i]);
                }
            }
            audioTrail.flatten(afs, span, channelMap);
            return DONE;
        }

        public void processFinished(ProcessingThread context) {
            final AudioFileDescr[] clientAFDs = (AudioFileDescr[]) context.getClientArg("afds");
            final AudioFile[] afs = (AudioFile[]) context.getClientArg("afs");
            final Session doc = (Session) context.getClientArg("doc");
            final boolean asCopy = ((Boolean) context.getClientArg("asCopy")).booleanValue();
            File tempF;
            if (context.getReturnCode() == DONE) {
                if (asCopy) {
                    for (int i = 0; i < afs.length; i++) {
                        try {
                            afs[i].close();
                        } catch (IOException e1) {
                            System.err.println("File '" + afs[i].getFile().getName() + "' could not be closed (" + e1.getClass().getName() + " : " + e1.getLocalizedMessage() + ")");
                        }
                        if (!clientAFDs[i].file.equals(afs[i].getFile())) {
                            if (clientAFDs[i].file.delete()) {
                                if (!afs[i].getFile().renameTo(clientAFDs[i].file)) {
                                    System.err.println("Newly saved file '" + afs[i].getFile().getName() + "' " + "could not be renamed!");
                                }
                            } else {
                                System.err.println("Previous file '" + clientAFDs[i].file.getAbsolutePath() + "' " + "could not be deleted! Newly saved file is '" + afs[i].getFile().getName() + "'!");
                            }
                        }
                    }
                } else {
                    doc.discardEditsAndClipboard();
                    try {
                        at.closeAll();
                    } catch (IOException e1) {
                        System.err.println("Previous audio files could not be closed (" + e1.getClass().getName() + " : " + e1.getLocalizedMessage() + ")");
                    }
                    for (int i = 0; i < afs.length; i++) {
                        final File f;
                        try {
                            afs[i].close();
                        } catch (IOException e1) {
                            System.err.println("File '" + afs[i].getFile().getName() + "' could not be closed (" + e1.getClass().getName() + " : " + e1.getLocalizedMessage() + ")");
                        }
                        if (clientAFDs[i].file.equals(afs[i].getFile())) {
                            f = afs[i].getFile();
                        } else {
                            if (clientAFDs[i].file.delete()) {
                                if (afs[i].getFile().renameTo(clientAFDs[i].file)) {
                                    f = clientAFDs[i].file;
                                } else {
                                    System.err.println("New current working file '" + afs[i].getFile().getName() + "' " + "could not be renamed!");
                                    f = afs[i].getFile();
                                }
                            } else tryRename: {
                                try {
                                    tempF = File.createTempFile("eis", null, clientAFDs[i].file.getParentFile());
                                } catch (IOException e1) {
                                    System.err.println("Previous file '" + clientAFDs[i].file.getAbsolutePath() + "' could neither be " + "deleted nor renamed. New current working file is '" + afs[i].getFile().getName() + "'!");
                                    f = afs[i].getFile();
                                    break tryRename;
                                }
                                if (clientAFDs[i].file.renameTo(tempF)) {
                                    System.err.println("Previous file '" + clientAFDs[i].file.getAbsolutePath() + "' could not be " + "deleted. It was renamed to '" + tempF.getName() + "'!");
                                    if (afs[i].getFile().renameTo(clientAFDs[i].file)) {
                                        f = clientAFDs[i].file;
                                    } else {
                                        System.err.println("New current working file '" + afs[i].getFile().getName() + "' " + "could not be renamed!");
                                        f = afs[i].getFile();
                                    }
                                } else {
                                    System.err.println("Previous file '" + clientAFDs[i].file.getAbsolutePath() + "' could neither be " + "deleted nor renamed. New current working file is '" + afs[i].getFile().getName() + "'!");
                                    f = afs[i].getFile();
                                }
                            }
                        }
                        try {
                            afs[i] = AudioFile.openAsRead(f);
                        } catch (IOException e1) {
                            System.err.println("File '" + f.getName() + "' could not be opened (" + e1.getClass().getName() + " : " + e1.getLocalizedMessage() + ")");
                        }
                        clientAFDs[i] = afs[i].getDescr();
                    }
                    try {
                        at.exchange(afs);
                    } catch (IOException e1) {
                        System.err.println("Audio files could not be exchanged (" + e1.getClass().getName() + " : " + e1.getLocalizedMessage() + ")");
                    }
                    doc.setDescr(clientAFDs);
                }
            } else {
                if (afs != null) {
                    for (int i = 0; i < afs.length; i++) {
                        if (afs[i] != null) {
                            afs[i].cleanUp();
                            if (!afs[i].getFile().delete()) {
                                System.err.println("The file '" + afs[i].getFile().getAbsolutePath() + "' " + "(created during saving) could not be deleted!");
                            }
                        }
                    }
                }
            }
        }

        public void processCancel(ProcessingThread context) {
        }
    }

    private class ActionCut extends MenuAction {

        protected ActionCut() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        protected void perform() {
            final ProcessingThread proc;
            if (actionCopy.perform()) {
                proc = procDelete(getValue(NAME).toString(), timeline.getSelectionSpan(), getEditMode());
                if (proc != null) start(proc);
            }
        }
    }

    private class ActionCopy extends MenuAction {

        protected ActionCopy() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        protected ClipboardTrackList getSelectionAsTrackList() {
            final Span span;
            span = timeline.getSelectionSpan();
            if (span.isEmpty()) return null;
            return new ClipboardTrackList(Session.this);
        }

        protected boolean perform() {
            boolean success = false;
            final ClipboardTrackList tl = getSelectionAsTrackList();
            if (tl == null) return success;
            try {
                AbstractApplication.getApplication().getClipboard().setContents(tl, tl);
                success = true;
            } catch (IllegalStateException e1) {
                System.err.println(getResourceString("errClipboard"));
            }
            return success;
        }
    }

    private class ActionPaste extends MenuAction implements ProcessingThread.Client {

        protected ActionPaste() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        protected void perform() {
            perform(getValue(NAME).toString(), getEditMode());
        }

        private void perform(String procName, int mode) {
            final Transferable t;
            final ClipboardTrackList tl;
            try {
                t = AbstractApplication.getApplication().getClipboard().getContents(this);
                if (t == null) return;
                if (!t.isDataFlavorSupported(ClipboardTrackList.trackListFlavor)) return;
                tl = (ClipboardTrackList) t.getTransferData(ClipboardTrackList.trackListFlavor);
            } catch (IOException e11) {
                System.err.println(e11.getLocalizedMessage());
                return;
            } catch (UnsupportedFlavorException e11) {
                System.err.println(e11.getLocalizedMessage());
                return;
            } catch (IllegalStateException e11) {
                System.err.println(getResourceString("errClipboard"));
                return;
            }
            if (!checkProcess()) return;
            final ProcessingThread proc = initiate(tl, timeline.getPosition(), procName, mode);
            if (proc != null) {
                start(proc);
            }
        }

        protected ProcessingThread initiate(ClipboardTrackList tl, long insertPos, String procName, int mode) {
            if (!checkProcess()) return null;
            if ((insertPos < 0) || (insertPos > timeline.getLength())) throw new IllegalArgumentException(String.valueOf(insertPos));
            final ProcessingThread proc;
            final Span oldSelSpan, insertSpan, copySpan, cutTimelineSpan;
            final AbstractCompoundEdit edit;
            final Flag hasSelectedAudio;
            final List tis;
            final boolean expTimeline, cutTimeline;
            final long docLength, pasteLength, preMaxLen, postMaxLen;
            final BlendContext bcPre, bcPost;
            hasSelectedAudio = new Flag(false);
            tis = Track.getInfos(selectedTracks.getAll(), tracks.getAll());
            if (!AudioTracks.checkSyncedAudio(tis, mode == EDIT_INSERT, null, hasSelectedAudio)) return null;
            expTimeline = (mode == EDIT_INSERT) && hasSelectedAudio.isSet();
            docLength = timeline.getLength();
            pasteLength = expTimeline ? tl.getSpan().getLength() : Math.min(tl.getSpan().getLength(), docLength - insertPos);
            if (pasteLength == 0) return null;
            if (mode == EDIT_INSERT) {
                if (insertPos < (docLength - insertPos)) {
                    postMaxLen = Math.min(insertPos, pasteLength >> 1);
                    preMaxLen = Math.min(postMaxLen << 1, Math.min(docLength - insertPos, pasteLength - postMaxLen));
                } else {
                    preMaxLen = Math.min(docLength - insertPos, pasteLength >> 1);
                    postMaxLen = Math.min(preMaxLen << 1, Math.min(insertPos, pasteLength - preMaxLen));
                }
            } else {
                preMaxLen = pasteLength >> 1;
                postMaxLen = pasteLength - preMaxLen;
            }
            bcPre = createBlendContext(preMaxLen, 0, hasSelectedAudio.isSet());
            bcPost = createBlendContext(postMaxLen, 0, hasSelectedAudio.isSet());
            insertSpan = new Span(insertPos, insertPos + pasteLength);
            copySpan = new Span(tl.getSpan().start, tl.getSpan().start + pasteLength);
            cutTimeline = (mode == EDIT_INSERT) && !hasSelectedAudio.isSet();
            cutTimelineSpan = cutTimeline ? new Span(docLength, docLength + pasteLength) : null;
            edit = new BasicCompoundEdit(procName);
            oldSelSpan = timeline.getSelectionSpan();
            if (!oldSelSpan.isEmpty()) {
                edit.addPerform(TimelineVisualEdit.select(this, Session.this, new Span()));
            }
            proc = new ProcessingThread(this, getFrame(), procName);
            proc.putClientArg("tl", tl);
            proc.putClientArg("pos", new Long(insertPos));
            proc.putClientArg("mode", new Integer(mode));
            proc.putClientArg("tis", tis);
            proc.putClientArg("pasteLen", new Long(pasteLength));
            proc.putClientArg("exp", new Boolean(expTimeline));
            proc.putClientArg("bcPre", bcPre);
            proc.putClientArg("bcPost", bcPost);
            proc.putClientArg("insertSpan", insertSpan);
            proc.putClientArg("copySpan", copySpan);
            proc.putClientArg("cut", new Boolean(cutTimeline));
            proc.putClientArg("cutSpan", cutTimelineSpan);
            proc.putClientArg("edit", edit);
            return proc;
        }

        /**
		 *  This method is called by ProcessingThread
		 */
        public int processRun(ProcessingThread context) throws IOException {
            final ClipboardTrackList tl = (ClipboardTrackList) context.getClientArg("tl");
            final long insertPos = ((Long) context.getClientArg("pos")).longValue();
            final int mode = ((Integer) context.getClientArg("mode")).intValue();
            final List tis = (List) context.getClientArg("tis");
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            final BlendContext bcPre = (BlendContext) context.getClientArg("bcPre");
            final BlendContext bcPost = (BlendContext) context.getClientArg("bcPost");
            final Span insertSpan = (Span) context.getClientArg("insertSpan");
            final Span copySpan = (Span) context.getClientArg("copySpan");
            final boolean cutTimeline = ((Boolean) context.getClientArg("cut")).booleanValue();
            final Span cutTimelineSpan = (Span) context.getClientArg("cutSpan");
            final long delta = insertPos - tl.getSpan().start;
            Track.Info ti;
            Trail srcTrail;
            AudioTrail audioTrail;
            boolean[] trackMap;
            boolean isAudio, pasteAudio;
            for (int i = 0; i < tis.size(); i++) {
                ti = (Track.Info) tis.get(i);
                if (ti.selected) {
                    try {
                        ti.trail.editBegin(edit);
                        isAudio = ti.trail instanceof AudioTrail;
                        srcTrail = tl.getSubTrail(ti.trail.getClass());
                        if (isAudio) {
                            pasteAudio = (srcTrail != null) && (((AudioTrail) srcTrail).getChannelNum() > 0);
                        } else {
                            pasteAudio = false;
                        }
                        if (mode == EDIT_INSERT) {
                            ti.trail.editInsert(this, insertSpan, edit);
                            if (cutTimeline) ti.trail.editRemove(this, cutTimelineSpan, edit);
                        } else if (pasteAudio || ((mode == EDIT_OVERWRITE) && !isAudio)) {
                            ti.trail.editClear(this, insertSpan, edit);
                        }
                        if (pasteAudio) {
                            audioTrail = (AudioTrail) ti.trail;
                            trackMap = tl.getTrackMap(ti.trail.getClass());
                            int[] trackMap2 = new int[audioTrail.getChannelNum()];
                            for (int j = 0, k = 0; j < trackMap2.length; j++) {
                                if (ti.trackMap[j]) {
                                    for (; (k < trackMap.length) && !trackMap[k]; k++) ;
                                    if (k < trackMap.length) {
                                        trackMap2[j] = k++;
                                    } else if (tl.getTrackNum(ti.trail.getClass()) > 0) {
                                        for (k = 0; !trackMap[k]; k++) ;
                                        trackMap2[j] = k++;
                                    } else {
                                        trackMap2[j] = -1;
                                    }
                                } else {
                                    trackMap2[j] = -1;
                                }
                            }
                            if (!audioTrail.copyRangeFrom((AudioTrail) srcTrail, copySpan, insertPos, mode, this, edit, trackMap2, bcPre, bcPost)) return CANCELLED;
                        } else if ((ti.numTracks == 1) && (tl.getTrackNum(ti.trail.getClass()) == 1)) {
                            ti.trail.editAddAll(this, srcTrail.getCuttedRange(copySpan, true, srcTrail.getDefaultTouchMode(), delta), edit);
                        }
                    } finally {
                        ti.trail.editEnd(edit);
                    }
                }
            }
            return DONE;
        }

        public void processFinished(ProcessingThread context) {
            final ProcessingThread.Client doneAction = (ProcessingThread.Client) context.getClientArg("doneAction");
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            final boolean expTimeline = ((Boolean) context.getClientArg("exp")).booleanValue();
            final long pasteLength = ((Long) context.getClientArg("pasteLen")).longValue();
            final Span insertSpan = (Span) context.getClientArg("insertSpan");
            if ((context.getReturnCode() == DONE)) {
                if (expTimeline && (pasteLength != 0)) {
                    edit.addPerform(new EditSetTimelineLength(this, Session.this, timeline.getLength() + pasteLength));
                    if (timeline.getVisibleSpan().isEmpty()) {
                        edit.addPerform(TimelineVisualEdit.scroll(this, Session.this, insertSpan));
                    }
                }
                if (!insertSpan.isEmpty()) {
                    edit.addPerform(TimelineVisualEdit.select(this, Session.this, insertSpan));
                    edit.addPerform(TimelineVisualEdit.position(this, Session.this, insertSpan.stop));
                }
                edit.perform();
                edit.end();
                getUndoManager().addEdit(edit);
            } else {
                edit.cancel();
            }
            if (doneAction != null) doneAction.processFinished(context);
        }

        public void processCancel(ProcessingThread context) {
        }
    }

    /**
	 *	@todo	when a cutted region spans entire view,
	 *			selecting undo results in empty visible span
	 */
    private class ActionDelete extends MenuAction implements ProcessingThread.Client {

        protected ActionDelete() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        protected void perform() {
            final Span span = timeline.getSelectionSpan();
            if (span.isEmpty()) return;
            final ProcessingThread proc = initiate(getValue(NAME).toString(), span, getEditMode());
            if (proc != null) start(proc);
        }

        protected ProcessingThread initiate(String procName, Span span, int mode) {
            if (!checkProcess()) return null;
            final BlendContext bc;
            final long cutLength, docLength, newDocLength, maxLen;
            final Flag hasSelectedAudio;
            final List tis;
            final AbstractCompoundEdit edit;
            final boolean cutTimeline;
            final Span cutTimelineSpan, selSpan;
            Span visiSpan;
            hasSelectedAudio = new Flag(false);
            tis = Track.getInfos(selectedTracks.getAll(), tracks.getAll());
            if (!AudioTracks.checkSyncedAudio(tis, mode == EDIT_INSERT, null, hasSelectedAudio)) return null;
            docLength = timeline.getLength();
            cutLength = span.getLength();
            if (mode == EDIT_INSERT) {
                maxLen = Math.min(cutLength, Math.min(span.start, docLength - span.stop) << 1);
                bc = createBlendContext(maxLen >> 1, (maxLen + 1) >> 1, hasSelectedAudio.isSet());
            } else {
                maxLen = cutLength >> 1;
                bc = createBlendContext(maxLen, 0, hasSelectedAudio.isSet());
            }
            edit = new BasicCompoundEdit(procName);
            cutTimeline = (mode == EDIT_INSERT) && hasSelectedAudio.isSet();
            newDocLength = cutTimeline ? docLength - cutLength : docLength;
            cutTimelineSpan = cutTimeline ? new Span(newDocLength, docLength) : null;
            selSpan = timeline.getSelectionSpan();
            if ((mode == EDIT_INSERT) && !selSpan.isEmpty()) {
                edit.addPerform(TimelineVisualEdit.position(this, Session.this, span.start));
                edit.addPerform(TimelineVisualEdit.select(this, Session.this, new Span()));
            }
            if (cutTimeline) {
                visiSpan = timeline.getVisibleSpan();
                if (visiSpan.stop > span.start) {
                    if (visiSpan.stop > newDocLength) {
                        visiSpan = new Span(Math.max(0, newDocLength - visiSpan.getLength()), newDocLength);
                        TimelineVisualEdit tve = TimelineVisualEdit.scroll(this, Session.this, visiSpan);
                        edit.addPerform(tve);
                    }
                }
                edit.addPerform(new EditSetTimelineLength(this, Session.this, newDocLength));
            }
            final ProcessingThread proc = new ProcessingThread(this, getFrame(), procName);
            proc.putClientArg("span", span);
            proc.putClientArg("mode", new Integer(mode));
            proc.putClientArg("tis", tis);
            proc.putClientArg("edit", edit);
            proc.putClientArg("bc", bc);
            proc.putClientArg("cut", new Boolean(cutTimeline));
            proc.putClientArg("cutSpan", cutTimelineSpan);
            return proc;
        }

        /**
		 *  This method is called by ProcessingThread
		 */
        public int processRun(ProcessingThread context) throws IOException {
            final Span span = (Span) context.getClientArg("span");
            final int mode = ((Integer) context.getClientArg("mode")).intValue();
            final List tis = (List) context.getClientArg("tis");
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            final BlendContext bc = (BlendContext) context.getClientArg("bc");
            final long left = bc == null ? 0L : bc.getLeftLen();
            final long right = bc == null ? 0L : bc.getRightLen();
            final boolean cutTimeline = ((Boolean) context.getClientArg("cut")).booleanValue();
            final Span cutTimelineSpan = (Span) context.getClientArg("cutSpan");
            AudioTrail audioTrail;
            Track.Info ti;
            boolean isAudio;
            for (int i = 0; i < tis.size(); i++) {
                ti = (Track.Info) tis.get(i);
                try {
                    ti.trail.editBegin(edit);
                    isAudio = ti.trail instanceof AudioTrail;
                    if (ti.selected) {
                        if (mode == EDIT_INSERT) {
                            if (isAudio) {
                                if (bc == null) {
                                    ti.trail.editRemove(this, span, edit);
                                } else {
                                    ti.trail.editRemove(this, new Span(span.start - left, span.stop + right), edit);
                                    ti.trail.editInsert(this, new Span(span.start - left, span.start + right), edit);
                                }
                                audioTrail = (AudioTrail) ti.trail;
                                audioTrail.clearRange(span, EDIT_INSERT, this, edit, ti.trackMap, bc);
                            } else {
                                ti.trail.editRemove(this, span, edit);
                            }
                        } else {
                            ti.trail.editClear(this, span, edit);
                            if (isAudio) {
                                audioTrail = (AudioTrail) ti.trail;
                                audioTrail.clearRange(span, EDIT_OVERWRITE, this, edit, ti.trackMap, bc);
                            }
                        }
                    } else if (cutTimeline) {
                        ti.trail.editRemove(this, cutTimelineSpan, edit);
                    }
                } finally {
                    ti.trail.editEnd(edit);
                }
            }
            return DONE;
        }

        public void processFinished(ProcessingThread context) {
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            if (context.getReturnCode() == DONE) {
                edit.perform();
                edit.end();
                getUndoManager().addEdit(edit);
            } else {
                edit.cancel();
            }
        }

        public void processCancel(ProcessingThread context) {
        }
    }

    private class ActionTrim extends MenuAction {

        protected ActionTrim() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        protected void perform() {
            final Span selSpan, deleteBefore, deleteAfter;
            final BasicCompoundEdit edit;
            final List tis;
            Track.Info ti;
            boolean success = false;
            edit = new BasicCompoundEdit(getValue(NAME).toString());
            try {
                selSpan = timeline.getSelectionSpan();
                tis = Track.getInfos(selectedTracks.getAll(), tracks.getAll());
                deleteBefore = new Span(0, selSpan.start);
                deleteAfter = new Span(selSpan.stop, timeline.getLength());
                edit.addPerform(TimelineVisualEdit.select(this, Session.this, new Span()));
                edit.addPerform(TimelineVisualEdit.position(this, Session.this, 0));
                if (!deleteAfter.isEmpty() || !deleteBefore.isEmpty()) {
                    for (int i = 0; i < tis.size(); i++) {
                        ti = (Track.Info) tis.get(i);
                        ti.trail.editBegin(edit);
                        try {
                            if (!deleteAfter.isEmpty()) ti.trail.editRemove(this, deleteAfter, edit);
                            if (!deleteBefore.isEmpty()) ti.trail.editRemove(this, deleteBefore, edit);
                        } finally {
                            ti.trail.editEnd(edit);
                        }
                    }
                }
                edit.addPerform(new EditSetTimelineLength(this, Session.this, selSpan.getLength()));
                edit.addPerform(TimelineVisualEdit.select(this, Session.this, selSpan.shift(-selSpan.start)));
                edit.perform();
                edit.end();
                getUndoManager().addEdit(edit);
                success = true;
            } finally {
                if (!success) edit.cancel();
            }
        }
    }

    /**
	 *	@todo	when edit mode != EDIT_INSERT, audio tracks are cleared which should be bypassed and vice versa
	 *	@todo	waveform display not automatically updated when edit mode != EDIT_INSERT
	 */
    private class ActionSilence extends MenuAction implements ProcessingThread.Client {

        private Param value = null;

        private ParamSpace space = null;

        protected ActionSilence() {
        }

        public void actionPerformed(ActionEvent e) {
            perform();
        }

        private void perform() {
            final SpringPanel msgPane;
            final int result;
            final ParamField ggDuration;
            final Param durationSmps;
            final DefaultUnitTranslator timeTrans;
            msgPane = new SpringPanel(4, 2, 4, 2);
            timeTrans = new DefaultUnitTranslator();
            ggDuration = new ParamField(timeTrans);
            ggDuration.addSpace(ParamSpace.spcTimeHHMMSS);
            ggDuration.addSpace(ParamSpace.spcTimeSmps);
            ggDuration.addSpace(ParamSpace.spcTimeMillis);
            ggDuration.addSpace(ParamSpace.spcTimePercentF);
            msgPane.gridAdd(ggDuration, 0, 0);
            msgPane.makeCompactGrid();
            GUIUtil.setInitialDialogFocus(ggDuration);
            timeTrans.setLengthAndRate(timeline.getLength(), timeline.getRate());
            if (value == null) {
                ggDuration.setValue(new Param(1.0, ParamSpace.TIME | ParamSpace.SECS));
            } else {
                ggDuration.setSpace(space);
                ggDuration.setValue(value);
            }
            final JOptionPane op = new JOptionPane(msgPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            result = BasicWindowHandler.showDialog(op, getFrame() == null ? null : getFrame().getWindow(), getValue(NAME).toString());
            if (result == JOptionPane.OK_OPTION) {
                value = ggDuration.getValue();
                space = ggDuration.getSpace();
                durationSmps = timeTrans.translate(value, ParamSpace.spcTimeSmps);
                if (durationSmps.val > 0.0) {
                    final ProcessingThread proc;
                    proc = initiate(timeline.getPosition(), (long) durationSmps.val);
                    if (proc != null) start(proc);
                }
            }
        }

        public ProcessingThread initiate(long pos, long numFrames) {
            if (!checkProcess() || (numFrames == 0)) return null;
            if (numFrames < 0) throw new IllegalArgumentException(String.valueOf(numFrames));
            if ((pos < 0) || (pos > timeline.getLength())) throw new IllegalArgumentException(String.valueOf(pos));
            final ProcessingThread proc;
            final AbstractCompoundEdit edit;
            final Span oldSelSpan, insertSpan;
            proc = new ProcessingThread(this, getFrame(), getValue(NAME).toString());
            edit = new BasicCompoundEdit(proc.getName());
            oldSelSpan = timeline.getSelectionSpan();
            insertSpan = new Span(pos, pos + numFrames);
            if (!oldSelSpan.isEmpty()) {
                edit.addPerform(TimelineVisualEdit.select(this, Session.this, new Span()));
            }
            proc.putClientArg("tis", Track.getInfos(selectedTracks.getAll(), tracks.getAll()));
            proc.putClientArg("edit", edit);
            proc.putClientArg("span", insertSpan);
            return proc;
        }

        /**
		 *  This method is called by ProcessingThread
		 */
        public int processRun(ProcessingThread context) throws IOException {
            final List tis = (List) context.getClientArg("tis");
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            final Span insertSpan = (Span) context.getClientArg("span");
            Track.Info ti;
            AudioTrail audioTrail;
            for (int i = 0; i < tis.size(); i++) {
                ti = (Track.Info) tis.get(i);
                ti.trail.editBegin(edit);
                try {
                    ti.trail.editInsert(this, insertSpan, edit);
                    if (ti.trail instanceof AudioTrail) {
                        audioTrail = (AudioTrail) ti.trail;
                        audioTrail.editAdd(this, audioTrail.allocSilent(insertSpan), edit);
                    }
                } finally {
                    ti.trail.editEnd(edit);
                }
            }
            return DONE;
        }

        public void processFinished(ProcessingThread context) {
            final AbstractCompoundEdit edit = (AbstractCompoundEdit) context.getClientArg("edit");
            final Span insertSpan = (Span) context.getClientArg("span");
            if (context.getReturnCode() == DONE) {
                if (!insertSpan.isEmpty()) {
                    edit.addPerform(new EditSetTimelineLength(this, Session.this, timeline.getLength() + insertSpan.getLength()));
                    if (timeline.getVisibleSpan().isEmpty()) {
                        edit.addPerform(TimelineVisualEdit.scroll(this, Session.this, insertSpan));
                    }
                }
                if (!insertSpan.isEmpty()) {
                    edit.addPerform(TimelineVisualEdit.select(this, Session.this, insertSpan));
                    edit.addPerform(TimelineVisualEdit.position(this, Session.this, insertSpan.stop));
                }
                edit.perform();
                edit.end();
                getUndoManager().addEdit(edit);
            } else {
                edit.cancel();
            }
        }

        public void processCancel(ProcessingThread context) {
        }
    }
}
