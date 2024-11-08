package org.waveprotocol.wave.concurrencycontrol.channel;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService.WaveViewServiceUpdate;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a view channel. This is a one off object. Once you've connected to the server
 * you cannot reconnect to the server using the same connection again.
 *
 * @see org.waveprotocol.wave.concurrencycontrol.channel.WaveletDeltaChannelImpl
 */
public class ViewChannelImpl implements ViewChannel, WaveViewService.OpenCallback {

    /**
   * Just a place holder value for {@link #debugLastSubmit} when we are submitting a delta.
   */
    private static final String SUBMITTING = "Submitting";

    /** Id of the wave being viewed. */
    private final WaveId waveId;

    /** Service through which rpcs are made. */
    private final WaveViewService waveService;

    /** Logger. */
    private final LoggerBundle logger;

    /** Counts the view channels for each wave. */
    private static final Map<WaveId, Integer> viewChannelsPerWave = new HashMap<WaveId, Integer>();

    /**
   * For 2 pairs of wave + playback channel. The second pair is used in
   * recovery whilest the first pair is closing.
   */
    private static final int DEFAULT_MAX_VIEW_CHANNELS_PER_WAVE = 4;

    private static int maxViewChannelsPerWave = DEFAULT_MAX_VIEW_CHANNELS_PER_WAVE;

    /**
   * This holds map of WaveletId to its last submit request id.
   */
    private final Map<WaveletId, String> debugLastSubmit = new HashMap<WaveletId, String>();

    private static enum State {

        /** Post-constructor state. */
        INITIAL, /** An open request has been sent. */
        CONNECTING, /** A response (and a channelId) have been received. */
        CONNECTED, /**
     * A close request is waiting to be sent to the server. You enter this state, when you've
     * opened the view channel and didn't get the channel id before closing it.
     */
        CLOSING, /** Channel is closed (either successfully or due to error). */
        CLOSED
    }

    /** State this channel is in. */
    private State state;

    /**
   * Channel id, which must be provided for all delta submission (including the
   * first submit on a new wavelet).  This channel id is supplied in the first
   * message received from the server.  As a corollary:
   *   INITIAL | CONNECTING => !hasChannelId()
   *   CONNECTED => hasChannelId()
   * Even in the CLOSING and CLOSED states, we retain the channel id, in order
   * that a ViewClose RPC can be sent from those states in some situations.
   */
    private String channelId;

    /**
   * Listens for connection lifecycle events.
   * Set on opening, and retained until closed.
   *
   * Also listens for incoming updates from the server stream.
   * Set on opening, and retained until closed.
   */
    private Listener openListener;

    /**
   * Creates a factory for view channels.
   *
   * @param waveService server with which to back channels
   * @param logger logger for channels
   */
    public static ViewChannelFactory factory(final WaveViewService waveService, final LoggerBundle logger) {
        return new ViewChannelFactory() {

            @Override
            public ViewChannel create(WaveId viewWaveId) {
                return new ViewChannelImpl(viewWaveId, waveService, logger);
            }
        };
    }

    /**
   * Constructs a view channel.
   *
   * @param waveId           id of the wave for which this channel is a view
   * @param service          service through which RPCs are made
   * @param logger           logger for error messages
   */
    public ViewChannelImpl(WaveId waveId, WaveViewService service, LoggerBundle logger) {
        this.waveId = waveId;
        this.waveService = service;
        this.logger = logger;
        this.state = State.INITIAL;
        registerChannel();
    }

    @Override
    public void open(Listener openListener, IdFilter waveletFilter, Map<WaveletId, List<HashedVersion>> knownWavelets) {
        Preconditions.checkState(state == State.INITIAL, "Cannot re-open view channel: %s", this);
        state = State.CONNECTING;
        this.openListener = openListener;
        logger.trace().log("connect: new view channel initialized");
        doOpen(waveletFilter, knownWavelets);
    }

    /**
   * Makes the appropriate server call to open a stream of deltas to the client.
   */
    private void doOpen(final IdFilter waveletFilter, final Map<WaveletId, List<HashedVersion>> knownWavelets) {
        waveService.viewOpen(waveletFilter, knownWavelets, this);
    }

    @Override
    public void close() {
        terminate(null);
    }

    @Override
    public void submitDelta(WaveletId waveletId, WaveletDelta delta, SubmitCallback callback) {
        Preconditions.checkState(state == State.CONNECTED, "Cannot submit to disconnected view channel: %s, delta version %s", this, delta.getTargetVersion());
        doSubmitDelta(waveletId, delta, callback);
    }

    /**
   * Makes the appropriate RPC call to the server to submit a delta.
   */
    private void doSubmitDelta(final WaveletId waveletId, final WaveletDelta delta, final SubmitCallback callback) {
        debugLastSubmit.put(waveletId, SUBMITTING);
        final String channelId = this.channelId;
        final WaveId waveId = this.waveId;
        String requestId = waveService.viewSubmit(WaveletName.of(waveId, waveletId), delta, channelId, new WaveViewService.SubmitCallback() {

            @Override
            public void onSuccess(HashedVersion version, int opsApplied, String errorMessage, ResponseCode responseCode) {
                debugLastSubmit.remove(waveletId);
                try {
                    callback.onSuccess(opsApplied, version, responseCode, errorMessage);
                } catch (ChannelException e) {
                    handleException("onSuccess", e, waveletId);
                }
            }

            @Override
            public void onFailure(String failure) {
                debugLastSubmit.remove(waveletId);
                try {
                    callback.onFailure(failure);
                } catch (ChannelException e) {
                    handleException("onFailure", e, waveletId);
                }
            }

            private void handleException(String methodName, ChannelException e, WaveletId waveletId) {
                debugLastSubmit.remove(waveletId);
                triggerOnException(e, waveletId);
                terminate("View submit [" + methodName + "] for wavelet " + waveId + "/" + waveletId + " raised exception: " + e);
            }
        });
        if (debugLastSubmit.containsKey(waveletId)) {
            debugLastSubmit.put(waveletId, requestId);
        }
    }

    /**
   * @return true if a channel id has been received from the server.
   */
    private boolean hasChannelId() {
        return channelId != null;
    }

    private void checkUpdateProtocolRestrictions(WaveViewServiceUpdate update) throws ChannelException {
        if (update.hasChannelId() && (update.hasMarker() || update.hasWaveletSnapshot() || update.hasDeltas() || update.hasLastCommittedVersion() || update.hasCurrentVersion())) {
            StringBuilder whichData = new StringBuilder();
            if (update.hasMarker()) {
                whichData.append("marker, ");
            }
            if (update.hasWaveletSnapshot()) {
                whichData.append("snapshot, ");
            }
            if (update.hasDeltas()) {
                whichData.append("deltas, ");
            }
            if (update.hasLastCommittedVersion()) {
                whichData.append("lastCommittedVersion, ");
            }
            if (update.hasCurrentVersion()) {
                whichData.append("currentVersion");
            }
            throw new ChannelException("An update contained a channel id AND other data: " + whichData, Recoverable.NOT_RECOVERABLE);
        }
        if ((update.hasWaveletSnapshot() || update.hasDeltas() || update.hasLastCommittedVersion() || update.hasCurrentVersion()) && !update.hasWaveletId()) {
            throw new ChannelException("An update lacked a required wavelet id.", Recoverable.NOT_RECOVERABLE);
        }
        if (update.hasWaveletSnapshot() && update.hasDeltas()) {
            throw new ChannelException("Message has both snapshot and deltas", Recoverable.NOT_RECOVERABLE);
        }
    }

    @Override
    public void onUpdate(WaveViewServiceUpdate update) {
        try {
            checkUpdateProtocolRestrictions(update);
        } catch (ChannelException e) {
            triggerOnException(e, update.hasWaveletId() ? update.getWaveletId() : null);
            terminate("View update raised exception: " + e.toString());
        }
        switch(state) {
            case INITIAL:
                Preconditions.illegalState("Unexpected update before view channel opened: %s, update: %s", this, update);
                break;
            case CONNECTING:
                if (!update.hasChannelId()) {
                    onException(new ChannelException("First update did not contain channel id. Wave id: " + waveId + ", update: " + update, Recoverable.NOT_RECOVERABLE));
                    return;
                }
                channelId = update.getChannelId();
                state = State.CONNECTED;
                if (openListener != null) {
                    openListener.onConnected();
                }
                break;
            case CONNECTED:
                if (update.hasChannelId()) {
                    logger.trace().log("A non-first update contained a channel id: " + update);
                }
                if (openListener != null) {
                    WaveletId waveletId = update.hasWaveletId() ? update.getWaveletId() : null;
                    HashedVersion lastCommittedVersion = update.hasLastCommittedVersion() ? update.getLastCommittedVersion() : null;
                    HashedVersion currentVersion = update.hasCurrentVersion() ? update.getCurrentVersion() : null;
                    try {
                        if (update.hasWaveletSnapshot()) {
                            openListener.onSnapshot(waveletId, update.getWaveletSnapshot(), lastCommittedVersion, currentVersion);
                        } else if (update.hasDeltas() || update.hasLastCommittedVersion() || update.hasCurrentVersion()) {
                            openListener.onUpdate(waveletId, update.getDeltaList(), lastCommittedVersion, currentVersion);
                        }
                        if (update.hasMarker()) {
                            openListener.onOpenFinished();
                        }
                    } catch (ChannelException e) {
                        triggerOnException(e, waveletId);
                        terminate("View update raised exception: " + e.toString());
                    }
                }
                break;
            case CLOSING:
                if (!hasChannelId()) {
                    if (!update.hasChannelId()) {
                        onException(new ChannelException("First update did not contain channel id. Wave id: " + waveId + ", update: " + update, Recoverable.NOT_RECOVERABLE));
                    }
                    channelId = update.getChannelId();
                    requestViewClose();
                }
                state = State.CLOSED;
                break;
            case CLOSED:
                break;
            default:
                Preconditions.illegalState("update in unknown state" + state);
        }
    }

    @Override
    public void onSuccess(String response) {
        boolean fatal = response != null;
        String errorMessage = fatal ? response : "<no remote error specified>";
        switch(state) {
            case INITIAL:
                Preconditions.illegalState("View channel received success before open: %s, response: %s", this, response);
                break;
            case CONNECTING:
            case CONNECTED:
                if (fatal) {
                    triggerOnException(new ChannelException("Server unexpectedly closed channel" + " with error: " + errorMessage, Recoverable.NOT_RECOVERABLE), null);
                }
                terminate("Received onSuccess in state " + state + " with message: " + errorMessage);
                break;
            case CLOSING:
                state = State.CLOSED;
                break;
            case CLOSED:
                break;
            default:
                Preconditions.illegalState("success in unknown state" + state);
        }
    }

    @Override
    public void onFailure(String failure) {
        switch(state) {
            case INITIAL:
                Preconditions.illegalState("View channel received failure before open: %s, response: %s", this, failure);
                break;
            case CONNECTING:
            case CONNECTED:
                terminate("Received failure: " + failure);
                break;
            case CLOSING:
                state = State.CLOSED;
                break;
            case CLOSED:
                break;
            default:
                Preconditions.illegalState("failure in unknown state" + state);
        }
    }

    @Override
    public void onException(ChannelException e) {
        triggerOnException(e, null);
        terminate("WaveService raised exception (probably in translation): " + e);
    }

    @Override
    public String toString() {
        return "[ViewChannel id: " + channelId + "\n waveId: " + waveId + "\n state: " + state + "]";
    }

    /**
   * Tells the listener of an exception on handling server messages. Wave and
   * wavelet id context is attached to the exception.
   *
   * @param e exception causing failure
   * @param waveletId associated wavelet id (may be null)
   */
    private void triggerOnException(ChannelException e, WaveletId waveletId) {
        if (openListener != null) {
            openListener.onException(new ChannelException(e.getResponseCode(), "Exception in view channel, state " + this, e, e.getRecoverable(), waveId, waveletId));
        }
    }

    /**
   * Terminates the connection, whichever state it's in. The lifecycle of this
   * channel always terminates with a call to this method.
   *
   * @param failure failure message (or null)
   */
    private void terminate(String failure) {
        switch(state) {
            case CLOSED:
                return;
            case INITIAL:
                state = State.CLOSED;
                break;
            default:
                if (hasChannelId()) {
                    state = State.CLOSED;
                    requestViewClose();
                } else {
                    state = State.CLOSING;
                }
        }
        if (logger.trace().shouldLog()) {
            logger.trace().log(this.toString() + " terminated: " + failure);
        }
        if (openListener != null) {
            openListener.onClosed();
        }
        openListener = null;
        unregisterChannel();
    }

    /**
   * Sends a request to close the stream.
   */
    private void requestViewClose() {
        Preconditions.checkState(hasChannelId(), "ViewClose requested without a channel id");
        waveService.viewClose(waveId, channelId, new WaveViewService.CloseCallback() {

            public void onFailure(String failure) {
            }

            public void onSuccess() {
            }
        });
    }

    /**
   * Track the current channel globally.
   */
    private void registerChannel() {
        synchronized (viewChannelsPerWave) {
            Integer viewChannelsForWave = viewChannelsPerWave.get(waveId);
            if (viewChannelsForWave == null) {
                viewChannelsPerWave.put(waveId, 1);
            } else if (viewChannelsForWave >= maxViewChannelsPerWave) {
                Preconditions.illegalState("Cannot create more than " + maxViewChannelsPerWave + " channels per wave. Wave id: " + waveId);
            } else {
                viewChannelsPerWave.put(waveId, viewChannelsForWave + 1);
            }
        }
    }

    /**
   * Untrack the current channel globally.
   */
    private void unregisterChannel() {
        synchronized (viewChannelsPerWave) {
            Integer viewChannelsForWave = viewChannelsPerWave.get(waveId);
            if (viewChannelsForWave != null) {
                if (viewChannelsForWave <= 1) {
                    viewChannelsPerWave.remove(waveId);
                } else {
                    viewChannelsPerWave.put(waveId, viewChannelsForWave - 1);
                }
            }
        }
    }

    /**
   * Set the number of view channels we can have per wave.
   */
    public static void setMaxViewChannelsPerWave(int size) {
        maxViewChannelsPerWave = size;
    }

    @Override
    public String debugGetProfilingInfo(WaveletId waveletId) {
        if (!debugLastSubmit.containsKey(waveletId)) {
            return "ViewChannelImpl: No submits to the server for " + waveletId;
        }
        return waveService.debugGetProfilingInfo(debugLastSubmit.get(waveletId));
    }
}
