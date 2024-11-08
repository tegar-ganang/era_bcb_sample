package net.sf.fmj.media;

import javax.media.*;
import javax.media.format.*;
import net.sf.fmj.filtergraph.*;
import net.sf.fmj.media.renderer.audio.*;
import net.sf.fmj.media.rtp.util.*;
import net.sf.fmj.media.util.*;

/**
 * BasicRenderer is a module which have InputConnectors and no OutputConnectors.
 * It receives data from its input connector and put its output in output device
 * such as file, URL, screen, audio device, output DataSource or null.<br>
 * MediaRenderers can be either Pull driven (as AudioPlayer) or Push driven (as
 * File renderer). VideoRenderer might be implemented as either Push or Pull.<br>
 * MediaRenderers are stopAtTime aware (so that the audio renderer would stop at
 * the correct time) and are responsible to stop the player at the required time
 * (no separate thread for poling TimeBase). <br>
 * There is no need to define buffers allocation and connectors behavior here,
 * as it is done in module. <br>
 * <br>
 * <i>Common functionality of renderers would be put here as we start the
 * implementation</i><br>
 * <i>We need the level 3 design to continue working on this class</i>
 * 
 */
public class BasicRendererModule extends BasicSinkModule implements RTPTimeReporter {

    protected PlaybackEngine engine;

    protected Renderer renderer;

    protected InputConnector ic;

    protected int framesPlayed = 0;

    protected float frameRate = 30f;

    protected boolean framesWereBehind = false;

    protected boolean prefetching = false;

    protected boolean started = false;

    private boolean opened = false;

    private int chunkSize = Integer.MAX_VALUE;

    private long lastDuration = 0;

    private RTPTimeBase rtpTimeBase = null;

    private String rtpCNAME = null;

    RenderThread renderThread;

    private Object prefetchSync = new Object();

    private ElapseTime elapseTime = new ElapseTime();

    private long LEEWAY = 10;

    private long lastRendered = 0;

    private boolean failed = false;

    private boolean notToDropNext = false;

    private Buffer storedBuffer = null;

    private boolean checkRTP = false;

    private boolean noSync = false;

    final float MAX_RATE = 1.05f;

    final float RATE_INCR = .01f;

    final int FLOW_LIMIT = 20;

    boolean overMsg = false;

    int overflown = FLOW_LIMIT / 2;

    float rate = 1.0f;

    long systemErr = 0L;

    static final long RTP_TIME_MARGIN = 2000000000L;

    boolean rtpErrMsg = false;

    long lastTimeStamp;

    static final int MAX_CHUNK_SIZE = 16;

    AudioFormat ulawFormat = new AudioFormat(AudioFormat.ULAW);

    AudioFormat linearFormat = new AudioFormat(AudioFormat.LINEAR);

    protected BasicRendererModule(Renderer r) {
        setRenderer(r);
        ic = new BasicInputConnector();
        if (r instanceof javax.media.renderer.VideoRenderer) ic.setSize(4); else ic.setSize(1);
        ic.setModule(this);
        registerInputConnector("input", ic);
        setProtocol(Connector.ProtocolSafe);
    }

    @Override
    public void abortPrefetch() {
        renderThread.pause();
        renderer.close();
        prefetching = false;
        opened = false;
    }

    private int computeChunkSize(Format format) {
        if (format instanceof AudioFormat && (ulawFormat.matches(format) || linearFormat.matches(format))) {
            AudioFormat af = (AudioFormat) format;
            int units = af.getSampleSizeInBits() * af.getChannels() / 8;
            if (units == 0) units = 1;
            int chunks = (int) af.getSampleRate() * units / MAX_CHUNK_SIZE;
            return (chunks / units * units);
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public void doClose() {
        renderThread.kill();
        if (renderer != null) renderer.close();
        if (rtpTimeBase != null) {
            RTPTimeBase.remove(this, rtpCNAME);
            rtpTimeBase = null;
        }
    }

    @Override
    public void doDealloc() {
        renderer.close();
    }

    @Override
    public void doFailedPrefetch() {
        renderThread.pause();
        renderer.close();
        opened = false;
        prefetching = false;
    }

    /**
     * Handles the aftermath of prefetching.
     */
    private void donePrefetch() {
        synchronized (prefetchSync) {
            if (!started && prefetching) renderThread.pause();
            prefetching = false;
        }
        if (moduleListener != null) moduleListener.bufferPrefetched(this);
    }

    @Override
    public void doneReset() {
        renderThread.pause();
    }

    @Override
    public boolean doPrefetch() {
        super.doPrefetch();
        if (!opened) {
            try {
                renderer.open();
            } catch (ResourceUnavailableException e) {
                prefetchFailed = true;
                return false;
            }
            prefetchFailed = false;
            opened = true;
        }
        if (!((PlaybackEngine) controller).prefetchEnabled) return true;
        prefetching = true;
        renderThread.start();
        return true;
    }

    /**
     * The loop to process the data. It handles the getting and putting back of
     * the data buffers. It in turn calls scheduleBuffer(Buffer) to do the bulk
     * of processing.
     */
    protected boolean doProcess() {
        if ((started || prefetching) && stopTime > -1 && elapseTime.value >= stopTime) {
            if (renderer instanceof Drainable) ((Drainable) renderer).drain();
            doStop();
            if (moduleListener != null) moduleListener.stopAtTime(this);
        }
        Buffer buffer;
        if (storedBuffer != null) buffer = storedBuffer; else {
            buffer = ic.getValidBuffer();
        }
        if (!checkRTP) {
            if ((buffer.getFlags() & Buffer.FLAG_RTP_TIME) != 0) {
                String key = engine.getCNAME();
                if (key != null) {
                    rtpTimeBase = RTPTimeBase.find(this, key);
                    rtpCNAME = key;
                    if (ic.getFormat() instanceof AudioFormat) {
                        Log.comment("RTP master time set: " + renderer + "\n");
                        rtpTimeBase.setMaster(this);
                    }
                    checkRTP = true;
                    noSync = false;
                } else {
                    noSync = true;
                }
            } else checkRTP = true;
        }
        lastTimeStamp = buffer.getTimeStamp();
        if (failed || resetted) {
            if ((buffer.getFlags() & Buffer.FLAG_FLUSH) != 0) {
                resetted = false;
                renderThread.pause();
                if (moduleListener != null) moduleListener.resetted(this);
            }
            storedBuffer = null;
            ic.readReport();
            return true;
        }
        boolean rtn = scheduleBuffer(buffer);
        if (storedBuffer == null && buffer.isEOM()) {
            if (prefetching) donePrefetch();
            if ((buffer.getFlags() & Buffer.FLAG_NO_WAIT) == 0 && buffer.getTimeStamp() > 0 && buffer.getDuration() > 0 && buffer.getFormat() != null && !(buffer.getFormat() instanceof AudioFormat) && !noSync) {
                waitForPT(buffer.getTimeStamp() + lastDuration);
            }
            storedBuffer = null;
            ic.readReport();
            doStop();
            if (moduleListener != null) moduleListener.mediaEnded(this);
            return true;
        }
        if (storedBuffer == null) ic.readReport();
        return rtn;
    }

    @Override
    public boolean doRealize() {
        chunkSize = computeChunkSize(ic.getFormat());
        renderThread = new RenderThread(this);
        engine = (PlaybackEngine) getController();
        return true;
    }

    @Override
    public void doStart() {
        super.doStart();
        if (!(renderer instanceof Clock)) renderer.start();
        prerolling = false;
        started = true;
        synchronized (prefetchSync) {
            prefetching = false;
            renderThread.start();
        }
    }

    @Override
    public void doStop() {
        started = false;
        prefetching = true;
        super.doStop();
        if (renderer != null && !(renderer instanceof Clock)) renderer.stop();
    }

    @Override
    public Object getControl(String s) {
        return renderer.getControl(s);
    }

    @Override
    public Object[] getControls() {
        return renderer.getControls();
    }

    public int getFramesPlayed() {
        return framesPlayed;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public long getRTPTime() {
        if (ic.getFormat() instanceof AudioFormat) {
            if (renderer instanceof AudioRenderer) {
                return lastTimeStamp - ((AudioRenderer) renderer).getLatency();
            } else {
                return lastTimeStamp;
            }
        } else {
            return lastTimeStamp;
        }
    }

    private long getSyncTime(long pts) {
        if (rtpTimeBase != null) {
            if (rtpTimeBase.getMaster() == getController()) return pts;
            long ts = rtpTimeBase.getNanoseconds();
            if (ts > pts + RTP_TIME_MARGIN || ts < pts - RTP_TIME_MARGIN) {
                if (!rtpErrMsg) {
                    Log.comment("Cannot perform RTP sync beyond a difference of: " + (ts - pts) / 1000000L + " msecs.\n");
                    rtpErrMsg = true;
                }
                return pts;
            } else return ts;
        } else return getMediaNanoseconds();
    }

    /**
     * Handles mid-stream format change.
     */
    private boolean handleFormatChange(Format format) {
        if (!reinitRenderer(format)) {
            storedBuffer = null;
            failed = true;
            if (moduleListener != null) moduleListener.formatChangedFailure(this, ic.getFormat(), format);
            return false;
        }
        Format oldFormat = ic.getFormat();
        ic.setFormat(format);
        if (moduleListener != null) moduleListener.formatChanged(this, oldFormat, format);
        if (format instanceof VideoFormat) {
            float fr = ((VideoFormat) format).getFrameRate();
            if (fr != Format.NOT_SPECIFIED) frameRate = fr;
        }
        return true;
    }

    /**
     * Handle the prerolling a buffer. It will preroll until the media has reach
     * the current media time before displaying.
     */
    protected boolean handlePreroll(Buffer buf) {
        if (buf.getFormat() instanceof AudioFormat) {
            if (!hasReachAudioPrerollTarget(buf)) return false;
        } else if ((buf.getFlags() & Buffer.FLAG_NO_SYNC) != 0 || buf.getTimeStamp() < 0) {
        } else if (buf.getTimeStamp() < getSyncTime(buf.getTimeStamp())) {
            return false;
        }
        prerolling = false;
        return true;
    }

    /**
     * Return true if given the input buffer, the audio will reach the target
     * preroll time -- the current media time.
     */
    private boolean hasReachAudioPrerollTarget(Buffer buf) {
        long target = getSyncTime(buf.getTimeStamp());
        elapseTime.update(buf.getLength(), buf.getTimeStamp(), buf.getFormat());
        if (elapseTime.value >= target) {
            long remain = ElapseTime.audioTimeToLen(elapseTime.value - target, (AudioFormat) buf.getFormat());
            int offset = buf.getOffset() + buf.getLength() - (int) remain;
            if (offset >= 0) {
                buf.setOffset(offset);
                buf.setLength((int) remain);
            }
            elapseTime.setValue(target);
            return true;
        }
        return false;
    }

    @Override
    public boolean isThreaded() {
        return true;
    }

    @Override
    protected void process() {
    }

    /**
     * Break down one larger buffer into smaller pieces so the processing won't
     * take that long to block.
     */
    public int processBuffer(Buffer buffer) {
        int remain = buffer.getLength();
        int offset = buffer.getOffset();
        int len, rc = PlugIn.BUFFER_PROCESSED_OK;
        boolean isEOM = false;
        if (renderer instanceof Clock) {
            if ((buffer.getFlags() & Buffer.FLAG_BUF_OVERFLOWN) != 0) overflown++; else overflown--;
            if (overflown > FLOW_LIMIT) {
                if (rate < MAX_RATE) {
                    rate += RATE_INCR;
                    renderer.stop();
                    ((Clock) renderer).setRate(rate);
                    renderer.start();
                    if (!overMsg) {
                        Log.comment("Data buffers overflown.  Adjust rendering speed up to 5 % to compensate");
                        overMsg = true;
                    }
                }
                overflown = FLOW_LIMIT / 2;
            } else if (overflown <= 0) {
                if (rate > 1.0f) {
                    rate -= RATE_INCR;
                    renderer.stop();
                    ((Clock) renderer).setRate(rate);
                    renderer.start();
                }
                overflown = FLOW_LIMIT / 2;
            }
        }
        do {
            if (stopTime > -1 && elapseTime.value >= stopTime) {
                if (prefetching) donePrefetch();
                return PlugIn.INPUT_BUFFER_NOT_CONSUMED;
            }
            if (remain <= chunkSize || prerolling) {
                if (isEOM) {
                    isEOM = false;
                    buffer.setEOM(true);
                }
                len = remain;
            } else {
                if (buffer.isEOM()) {
                    isEOM = true;
                    buffer.setEOM(false);
                }
                len = chunkSize;
            }
            buffer.setLength(len);
            buffer.setOffset(offset);
            if (prerolling && !handlePreroll(buffer)) {
                offset += len;
                remain -= len;
                continue;
            }
            try {
                rc = renderer.process(buffer);
            } catch (Throwable e) {
                Log.dumpStack(e);
                if (moduleListener != null) moduleListener.internalErrorOccurred(this);
            }
            if ((rc & PlugIn.PLUGIN_TERMINATED) != 0) {
                failed = true;
                if (moduleListener != null) moduleListener.pluginTerminated(this);
                return rc;
            }
            if ((rc & PlugIn.BUFFER_PROCESSED_FAILED) != 0) {
                buffer.setDiscard(true);
                if (prefetching) donePrefetch();
                return rc;
            }
            if ((rc & PlugIn.INPUT_BUFFER_NOT_CONSUMED) != 0) {
                len -= buffer.getLength();
            }
            offset += len;
            remain -= len;
            if (prefetching && (!(renderer instanceof Prefetchable) || ((Prefetchable) renderer).isPrefetched())) {
                isEOM = false;
                buffer.setEOM(false);
                donePrefetch();
                break;
            }
            elapseTime.update(len, buffer.getTimeStamp(), buffer.getFormat());
        } while (remain > 0 && !resetted);
        if (isEOM) buffer.setEOM(true);
        buffer.setLength(remain);
        buffer.setOffset(offset);
        if (rc == PlugIn.BUFFER_PROCESSED_OK) framesPlayed++;
        return rc;
    }

    /**
     * Attempt to re-initialize the renderer given a new input format.
     */
    protected boolean reinitRenderer(Format input) {
        if (renderer != null) {
            if (renderer.setInputFormat(input) != null) {
                return true;
            }
        }
        if (started) {
            renderer.stop();
            renderer.reset();
        }
        renderer.close();
        renderer = null;
        Renderer r;
        if ((r = SimpleGraphBuilder.findRenderer(input)) == null) return false;
        setRenderer(r);
        if (started) renderer.start();
        chunkSize = computeChunkSize(input);
        return true;
    }

    @Override
    public void reset() {
        super.reset();
        prefetching = false;
    }

    public void resetFramesPlayed() {
        framesPlayed = 0;
    }

    /**
     * Handed a buffer, this function does the scheduling of the buffer
     * processing. It in turn calls processBuffer to do the real processing.
     */
    protected boolean scheduleBuffer(Buffer buf) {
        int rc = PlugIn.BUFFER_PROCESSED_OK;
        Format format = buf.getFormat();
        if (format == null) {
            format = ic.getFormat();
            buf.setFormat(format);
        }
        if (format != ic.getFormat() && !format.equals(ic.getFormat()) && !buf.isDiscard()) {
            if (!handleFormatChange(format)) return false;
        }
        if ((buf.getFlags() & Buffer.FLAG_SYSTEM_MARKER) != 0 && moduleListener != null) {
            moduleListener.markedDataArrived(this, buf);
            buf.setFlags(buf.getFlags() & ~Buffer.FLAG_SYSTEM_MARKER);
        }
        if (prefetching || (format instanceof AudioFormat) || buf.getTimeStamp() <= 0 || (buf.getFlags() & Buffer.FLAG_NO_SYNC) == Buffer.FLAG_NO_SYNC || noSync) {
            if (!buf.isDiscard()) rc = processBuffer(buf);
        } else {
            long mt = getSyncTime(buf.getTimeStamp());
            long lateBy = mt / 1000000L - buf.getTimeStamp() / 1000000L - getLatency() / 1000000L;
            if (storedBuffer == null && lateBy > 0) {
                if (buf.isDiscard()) {
                    notToDropNext = true;
                } else {
                    if (buf.isEOM()) {
                        notToDropNext = true;
                    } else {
                        if (moduleListener != null && format instanceof VideoFormat) {
                            float fb = lateBy * frameRate / 1000f;
                            if (fb < 1f) fb = 1f;
                            moduleListener.framesBehind(this, fb, ic);
                            framesWereBehind = true;
                        }
                    }
                    if ((buf.getFlags() & Buffer.FLAG_NO_DROP) != 0) {
                        rc = processBuffer(buf);
                    } else {
                        if (lateBy < LEEWAY || notToDropNext || (buf.getTimeStamp() - lastRendered) > 1000000000L) {
                            rc = processBuffer(buf);
                            lastRendered = buf.getTimeStamp();
                            notToDropNext = false;
                        } else {
                        }
                    }
                }
            } else {
                if (moduleListener != null && framesWereBehind && format instanceof VideoFormat) {
                    moduleListener.framesBehind(this, 0f, ic);
                    framesWereBehind = false;
                }
                if (!buf.isDiscard()) {
                    if ((buf.getFlags() & Buffer.FLAG_NO_WAIT) == 0) waitForPT(buf.getTimeStamp());
                    if (!resetted) {
                        rc = processBuffer(buf);
                        lastRendered = buf.getTimeStamp();
                    }
                }
            }
        }
        if ((rc & PlugIn.BUFFER_PROCESSED_FAILED) != 0) {
            storedBuffer = null;
        } else if ((rc & PlugIn.INPUT_BUFFER_NOT_CONSUMED) != 0) {
            storedBuffer = buf;
        } else {
            storedBuffer = null;
            if (buf.getDuration() >= 0) lastDuration = buf.getDuration();
        }
        return true;
    }

    @Override
    public void setFormat(Connector connector, Format format) {
        renderer.setInputFormat(format);
        if (format instanceof VideoFormat) {
            float fr = ((VideoFormat) format).getFrameRate();
            if (fr != Format.NOT_SPECIFIED) frameRate = fr;
        }
    }

    /**
     * Enable prerolling.
     */
    @Override
    public void setPreroll(long wanted, long actual) {
        super.setPreroll(wanted, actual);
        elapseTime.setValue(actual);
    }

    protected void setRenderer(Renderer r) {
        renderer = r;
        if (renderer instanceof Clock) setClock((Clock) renderer);
    }

    @Override
    public void triggerReset() {
        if (renderer != null) renderer.reset();
        synchronized (prefetchSync) {
            prefetching = false;
            if (resetted) renderThread.start();
        }
    }

    /**
     * If the presentation time has not been reached, this function will wait
     * until that happens.
     */
    private boolean waitForPT(long pt) {
        long mt = getSyncTime(pt);
        long aheadBy, lastAheadBy = -1;
        long interval;
        long before, slept;
        int beenHere = 0;
        aheadBy = (pt - mt) / 1000000L;
        if (rate != 1.0f) aheadBy = (long) (aheadBy / rate);
        while (aheadBy > systemErr && !resetted) {
            if (aheadBy == lastAheadBy) {
                interval = aheadBy + (5 * beenHere);
                if (interval > 33L) interval = 33L; else beenHere++;
            } else {
                interval = aheadBy;
                beenHere = 0;
            }
            interval = (interval > 125L ? 125L : interval);
            before = System.currentTimeMillis();
            interval -= systemErr;
            try {
                if (interval > 0) Thread.sleep(interval);
            } catch (InterruptedException e) {
            }
            slept = System.currentTimeMillis() - before;
            systemErr = (slept - interval + systemErr) / 2;
            if (systemErr < 0) systemErr = 0; else if (systemErr > interval) systemErr = interval;
            mt = getSyncTime(pt);
            lastAheadBy = aheadBy;
            aheadBy = (pt - mt) / 1000000L;
            if (rate != 1.0f) aheadBy = (long) (aheadBy / rate);
            if (getState() != Controller.Started) break;
        }
        return true;
    }
}

class RenderThread extends LoopThread {

    BasicRendererModule module;

    public RenderThread(BasicRendererModule module) {
        this.module = module;
        setName(getName() + ": " + module.renderer);
        useVideoPriority();
    }

    @Override
    protected boolean process() {
        return module.doProcess();
    }
}
