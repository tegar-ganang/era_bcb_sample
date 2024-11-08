package org.waveprotocol.box.webclient.client;

import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.box.common.comms.jso.ProtocolOpenRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitRequestJsoImpl;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;
import java.util.Map;

/**
 * Distributes the incoming update stream (from wave-in-a-box's client/server
 * protocol) into per-wave streams.
 */
public final class RemoteViewServiceMultiplexer implements WaveWebSocketCallback {

    /** Per-wave streams. */
    private final Map<WaveId, WaveWebSocketCallback> streams = CollectionUtils.newHashMap();

    private final Map<WaveId, String> knownChannels = CollectionUtils.newHashMap();

    /** Underlying socket. */
    private final WaveWebSocketClient socket;

    /** Identity, for authoring messages. */
    private final String userId;

    /**
   * Creates a multiplexer.
   *
   * @param socket communication object
   * @param userId identity of viewer
   */
    public RemoteViewServiceMultiplexer(WaveWebSocketClient socket, String userId) {
        this.socket = socket;
        this.userId = userId;
        socket.attachHandler(this);
    }

    /** Dispatches an update to the appropriate wave stream. */
    @Override
    public void onWaveletUpdate(ProtocolWaveletUpdate message) {
        WaveletName wavelet = deserialize(message.getWaveletName());
        WaveWebSocketCallback stream = streams.get(wavelet.waveId);
        if (stream != null) {
            boolean drop;
            String knownChannelId = knownChannels.get(wavelet.waveId);
            if (knownChannelId != null) {
                drop = message.hasChannelId() && !message.getChannelId().equals(knownChannelId);
            } else {
                if (message.hasChannelId()) {
                    knownChannels.put(wavelet.waveId, message.getChannelId());
                }
                drop = false;
            }
            if (!drop) {
                stream.onWaveletUpdate(message);
            }
        } else {
        }
    }

    /**
   * Opens a wave stream.
   *
   * @param id wave to open
   * @param stream handler to updates directed at that wave
   */
    public void open(WaveId id, IdFilter filter, WaveWebSocketCallback stream) {
        streams.put(id, stream);
        ProtocolOpenRequestJsoImpl request = ProtocolOpenRequestJsoImpl.create();
        request.setWaveId(ModernIdSerialiser.INSTANCE.serialiseWaveId(id));
        request.setParticipantId(userId);
        for (String prefix : filter.getPrefixes()) {
            request.addWaveletIdPrefix(prefix);
        }
        for (WaveletId wid : filter.getIds()) {
            request.addWaveletIdPrefix(wid.getId());
        }
        socket.open(request);
    }

    /**
   * Closes a wave stream.
   *
   * @param id wave to close
   * @param stream stream previously registered against that wave
   */
    public void close(WaveId id, WaveWebSocketCallback stream) {
        if (streams.get(id) == stream) {
            streams.remove(id);
            knownChannels.remove(id);
        }
    }

    /**
   * Submits a delta.
   *
   * @param request delta to submit
   * @param callback callback for submit response
   */
    public void submit(ProtocolSubmitRequestJsoImpl request, SubmitResponseCallback callback) {
        request.getDelta().setAuthor(userId);
        socket.submit(request, callback);
    }

    public static WaveletName deserialize(String name) {
        try {
            return ModernIdSerialiser.INSTANCE.deserialiseWaveletName(name);
        } catch (InvalidIdException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String serialize(WaveletName name) {
        return ModernIdSerialiser.INSTANCE.serialiseWaveletName(name);
    }
}
