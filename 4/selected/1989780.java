package org.mobicents.media.server.connection;

import java.util.ArrayList;
import org.mobicents.media.CheckPoint;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.BaseEndpointImpl;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.video.VideoMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.format.VideoFormat;

/**
 * Implements connection management subsystem.
 *
 * Procedure of joining endponts must work very fast however dynamic connection
 * creation upon request cause long and unpredictable delays. Preallocated
 * connection objects gives better result.
 *
 *
 * @author kulikov
 */
public class Connections {

    protected BaseEndpointImpl endpoint;

    protected Scheduler scheduler;

    protected ArrayList<BaseConnection> localConnections;

    protected ArrayList<BaseConnection> rtpConnections;

    protected ArrayList<BaseConnection> activeConnections;

    private BaseConnection connection;

    protected Channel audioChannel;

    protected Channel videoChannel;

    /**
     * active local channels
     */
    private ArrayList<LocalChannel> localChannels = new ArrayList();

    private Formats audioFormats = new Formats();

    private Formats videoFormats = new Formats();

    protected RTPManager rtpManager;

    /** Signaling processors factory */
    protected DspFactory dspFactory;

    /**
     * Creates new connections subsystem.
     *
     * @param endpoint the endpoint running connections
     * @param poolSize the number of available connections.
     */
    public Connections(BaseEndpointImpl endpoint, int poolSize) throws Exception {
        this.endpoint = endpoint;
        this.scheduler = endpoint.getScheduler();
        this.rtpManager = endpoint.getRtpManager();
        this.dspFactory = endpoint.getDspFactory();
        audioFormats.add(FormatFactory.createAudioFormat("linear", 8000, 16, 1));
        videoFormats.add(FormatFactory.createVideoFormat("unknown"));
        int count = 1;
        localConnections = new ArrayList(poolSize);
        for (int i = 0; i < poolSize; i++) {
            localConnections.add(new LocalConnectionImpl(Integer.toString(count++), this));
        }
        rtpConnections = new ArrayList(poolSize);
        for (int i = 0; i < poolSize; i++) {
            rtpConnections.add(new RtpConnectionImpl(Integer.toString(count++), this));
        }
        activeConnections = new ArrayList(2 * poolSize);
        audioChannel = new Channel(new AudioMixer(scheduler), new Splitter(scheduler), MediaType.AUDIO);
        videoChannel = new Channel(new VideoMixer(scheduler), new Splitter(scheduler), MediaType.VIDEO);
    }

    /**
     * Creates connection with specified type.
     *
     * @param type the type of connection
     * @return connection instance.
     */
    public synchronized Connection createConnection(ConnectionType type) throws ResourceUnavailableException {
        switch(type) {
            case LOCAL:
                return poll(localConnections);
            case RTP:
                return poll(rtpConnections);
            default:
                throw new ResourceUnavailableException("Unknown connection type");
        }
    }

    /**
     * Gets the intermediate audio format.
     *
     * @return the audio format descriptor.
     */
    public AudioFormat getAudioFormat() {
        return (AudioFormat) this.audioFormats.get(0);
    }

    /**
     * Sets the intermediate audio format.
     *
     * @param audioFormat the audio format descriptor.
     */
    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormats.clean();
        this.audioFormats.add(audioFormat);
    }

    /**
     * Gets the intermediate video format.
     *
     * @return the video format descriptor.
     */
    public VideoFormat getVideoFormat() {
        return (VideoFormat) this.videoFormats.get(0);
    }

    /**
     * Sets the intermediate video format.
     *
     * @param videoFormat the video format descriptor.
     */
    public void setVideoFormat(VideoFormat videoFormat) {
        this.videoFormats.clean();
        this.videoFormats.add(videoFormat);
    }

    /**
     * Gets intermediate as collection.
     * 
     * @param mediaType the media type 
     * @return the collection wich contains single element with intermediate format.
     */
    protected Formats getFormats(MediaType mediaType) {
        switch(mediaType) {
            case AUDIO:
                return audioFormats;
            case VIDEO:
                return videoFormats;
            default:
                return null;
        }
    }

    /**
     * Polls connection from specified pool.
     *
     * @param pool the pool to poll.
     * @return connection instance.
     */
    private BaseConnection poll(ArrayList<BaseConnection> pool) throws ResourceUnavailableException {
        if (pool.isEmpty()) {
            throw new ResourceUnavailableException("Connections limit exceeded");
        }
        connection = pool.remove(0);
        activeConnections.add(connection);
        return connection;
    }

    /**
     * Closes all activities connections.
     */
    public void release() {
        ArrayList<BaseConnection> temp = new ArrayList();
        temp.addAll(activeConnections);
        for (BaseConnection con : temp) {
            con.close();
        }
    }

    /**
     * Get access to the mixer.
     *
     * @param mediaType the type of the mixer: audio or video
     * @return mixer component
     */
    public Mixer getMixer(MediaType mediaType) {
        switch(mediaType) {
            case AUDIO:
        }
        return audioChannel.mixer;
    }

    /**
     * Gets access to the splitter.
     *
     * @param mediaType the type of the splitter: audio or video
     * @return splitter component.
     */
    public Splitter getSplitter(MediaType mediaType) {
        return audioChannel.splitter;
    }

    /**
     * Gets the channel of specified media type
     *
     * @param mediaType the media type of stream
     * @return the channel streaming media with mediType content
     */
    private Channel getChannel(MediaType mediaType) {
        switch(mediaType) {
            case AUDIO:
                return this.audioChannel;
            case VIDEO:
                return this.videoChannel;
            default:
                return null;
        }
    }

    /**
     * Determines transmission mode between endpoint and connections.
     *
     * This method is called when mode of any connection has been changed.
     * It checks modes of all active connections and determines endpoint
     * transition mode as a combination of connections modes.
     */
    public synchronized void updateMode(MediaType mediaType) throws ModeNotSupportedException {
        boolean send = false;
        boolean recv = false;
        boolean loop = false;
        for (int i = 0; i < activeConnections.size(); i++) {
            if (loop) {
                break;
            }
            connection = activeConnections.get(i);
            switch(connection.getMode(mediaType)) {
                case SEND_ONLY:
                    send = true;
                    break;
                case RECV_ONLY:
                    recv = true;
                    break;
                case SEND_RECV:
                case CONFERENCE:
                    send = true;
                    recv = true;
                    break;
                case LOOPBACK:
                    loop = true;
                    break;
            }
        }
        if (loop) {
            getChannel(mediaType).setMode(ConnectionMode.LOOPBACK);
            return;
        }
        if (send && !recv) {
            getChannel(mediaType).setMode(ConnectionMode.SEND_ONLY);
            return;
        }
        if (!send && recv) {
            getChannel(mediaType).setMode(ConnectionMode.RECV_ONLY);
            return;
        }
        if (send && recv) {
            getChannel(mediaType).setMode(ConnectionMode.SEND_RECV);
            return;
        }
        if (!send && !recv) {
            getChannel(mediaType).setMode(null);
            return;
        }
    }

    protected void addToConference(BaseConnection connection) {
        for (BaseConnection c : activeConnections) {
            if (c.getMode(MediaType.AUDIO) == ConnectionMode.CONFERENCE && connection != c) {
                LocalChannel channel = new LocalChannel();
                channel.join(connection, c);
                localChannels.add(channel);
            }
        }
    }

    protected void removeFromConference(BaseConnection connection) {
        LocalChannel channel = null;
        for (LocalChannel c : localChannels) {
            if (c.match(connection)) {
                channel = c;
                break;
            }
        }
        if (channel != null) {
            localChannels.remove(channel);
            channel.unjoin();
        }
    }

    public String report() {
        StringBuilder builder = new StringBuilder();
        builder.append(audioChannel.splitter.report());
        builder.append(audioChannel.mixer.report());
        return builder.toString();
    }

    /**
     * Reads data from specified check point.
     * 
     * @param n the identifier of check point
     * @return the data collected by checkpoint.
     */
    public CheckPoint getCheckPoint(MediaType mediaType, int n) {
        switch(n) {
            case 1:
                MediaSource s = endpoint.getSource(mediaType);
                return s == null ? new CheckPointImpl(0, 0) : new CheckPointImpl(s.getPacketsTransmitted(), s.getBytesTransmitted());
            case 2:
                MediaSink sink = endpoint.getSink(mediaType);
                return sink == null ? new CheckPointImpl(0, 0) : new CheckPointImpl(sink.getPacketsReceived(), sink.getBytesReceived());
            case 3:
                sink = this.getChannel(mediaType).splitter.getInput();
                return new CheckPointImpl(sink.getPacketsReceived(), sink.getBytesReceived());
            case 4:
                s = this.getChannel(mediaType).mixer.getOutput();
                return new CheckPointImpl(s.getPacketsTransmitted(), s.getBytesTransmitted());
            default:
                throw new IllegalArgumentException("Unknown check point");
        }
    }

    /**
     * Transmission channel between connections and endpoint
     */
    protected class Channel {

        protected Mixer mixer;

        protected Splitter splitter;

        private Mode mode;

        private Mode sendOnly;

        private Mode recvOnly;

        private Mode sendRecv;

        private Mode loopback;

        private MediaType mediaType;

        /**
         * Creates new channel.
         *
         * @param mixer the mixer used for mixing signals from connections
         * @param splitter the splitter for splitting signals between connections
         * @param mediaType the media type of the stream
         */
        protected Channel(Mixer mixer, Splitter splitter, MediaType mediaType) {
            this.mixer = mixer;
            this.splitter = splitter;
            this.mediaType = mediaType;
            sendOnly = new SendOnly(this);
            recvOnly = new RecvOnly(this);
            sendRecv = new SendRecv(this);
            loopback = new Loopback(this);
        }

        /**
         * Gets the media type of this channel
         *
         * @return the media type identifier
         */
        protected MediaType getMediaType() {
            return mediaType;
        }

        /**
         * Enables transmission in specified mode.
         *
         * @param mode the mode of transmission
         * @throws ModeNotSupportedException
         */
        protected void setMode(ConnectionMode mode) throws ModeNotSupportedException {
            if (this.mode != null) {
                this.mode.off();
            }
            if (mode == null) {
                return;
            }
            switch(mode) {
                case SEND_ONLY:
                    this.mode = sendOnly;
                    break;
                case RECV_ONLY:
                    this.mode = recvOnly;
                    break;
                case SEND_RECV:
                    this.mode = sendRecv;
                    break;
                case LOOPBACK:
                    this.mode = loopback;
                    break;
            }
            if (this.mode != null) {
                this.mode.on();
            }
        }
    }

    /**
     * Transmission mode
     */
    private abstract class Mode {

        protected Channel channel;

        /**
         * Creates mode.
         * 
         * @param channel transmission channel
         */
        protected Mode(Channel channel) {
            this.channel = channel;
        }

        /** 
         * Switches off transmission in this mode
         */
        protected abstract void off();

        /**
         * Switches on transmission in this mode
         */
        protected abstract void on() throws ModeNotSupportedException;

        /**
         * The number of frames received by endpoint.
         *
         * @return the number of frames.
         */
        protected abstract int rxPackets();

        /**
         * The number of frames transmitted by endpoint.
         *
         * @return the number of frames.
         */
        protected abstract int txPackets();
    }

    /**
     * This mode defines transmission from endpoint to connections.
     */
    private class SendOnly extends Mode {

        private long mediaTime;

        private MediaSource source;

        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates new mode switch.
         *
         * @param channel the channel to wich this switch applies
         */
        protected SendOnly(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            pipe.stop();
            pipe.disconnect();
            mediaTime = source.getMediaTime();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            source = endpoint.getSource(channel.getMediaType());
            source.setMediaTime(mediaTime);
            if (source == null) {
                throw new ModeNotSupportedException("SEND_ONLY");
            }
            try {
                channel.splitter.getInput().setFormats(source.getFormats());
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }
            pipe.connect(source);
            pipe.connect(channel.splitter.getInput());
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return 0;
        }

        @Override
        protected int txPackets() {
            return pipe.getTxPackets();
        }
    }

    /**
     * This mode defines the transmission to the endpoint
     */
    private class RecvOnly extends Mode {

        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates this switch
         * @param channel the transmission channel
         */
        protected RecvOnly(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            channel.mixer.stop();
            pipe.stop();
            pipe.disconnect();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            MediaSink sink = endpoint.getSink(channel.getMediaType());
            if (sink == null) {
                throw new ModeNotSupportedException("RECV_ONLY");
            }
            try {
                channel.mixer.getOutput().setFormats(sink.getFormats());
            } catch (FormatNotSupportedException e) {
                throw new ModeNotSupportedException(e.getMessage());
            }
            pipe.connect(sink);
            pipe.connect(channel.mixer.getOutput());
            channel.mixer.start();
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return pipe.getRxPackets();
        }

        @Override
        protected int txPackets() {
            return 0;
        }
    }

    /**
     * This mode defines transmission from and to endpoint.
     */
    private class SendRecv extends Mode {

        private Mode sendOnly;

        private Mode recvOnly;

        /**
         * Creates switch.
         *
         * @param channel transmission channel
         */
        protected SendRecv(Channel channel) {
            super(channel);
            sendOnly = new SendOnly(channel);
            recvOnly = new RecvOnly(channel);
        }

        @Override
        protected void off() {
            sendOnly.off();
            recvOnly.off();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            recvOnly.on();
            sendOnly.on();
        }

        @Override
        protected int rxPackets() {
            return recvOnly.rxPackets();
        }

        @Override
        protected int txPackets() {
            return sendOnly.txPackets();
        }
    }

    /**
     * In this mode the endpoint transmits media to itself.
     */
    private class Loopback extends Mode {

        private PipeImpl pipe = new PipeImpl();

        /**
         * Creates switch.
         *
         * @param channel not used.
         */
        protected Loopback(Channel channel) {
            super(channel);
        }

        @Override
        protected void off() {
            pipe.stop();
            pipe.disconnect();
        }

        @Override
        protected void on() throws ModeNotSupportedException {
            MediaSink sink = endpoint.getSink(channel.getMediaType());
            MediaSource source = endpoint.getSource(channel.getMediaType());
            if (sink == null || source == null) {
                throw new ModeNotSupportedException("LOOPBACK");
            }
            pipe.connect(sink);
            pipe.connect(source);
            pipe.start();
        }

        @Override
        protected int rxPackets() {
            return pipe.getRxPackets();
        }

        @Override
        protected int txPackets() {
            return pipe.getRxPackets();
        }
    }

    /**
     * Implements connection's check points
     */
    private class CheckPointImpl implements CheckPoint {

        private int frames, bytes;

        /**
         * Creates check point instance.
         *
         * @param frames
         * @param bytes
         */
        protected CheckPointImpl(long frames, long bytes) {
            this.frames = (int) frames;
            this.bytes = (int) bytes;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.CheckPoint#getFrames()
         */
        public int getFrames() {
            return this.frames;
        }

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.CheckPoint#getBytes()
         */
        public int getBytes() {
            return this.bytes;
        }

        @Override
        public String toString() {
            return String.format("frame=%d, bytes=%d", frames, bytes);
        }
    }

    /**
     * Channel for joining connections in CNF mode.
     */
    private class LocalChannel {

        private Party party1 = new Party();

        private Party party2 = new Party();

        private PipeImpl audioRxPipe = new PipeImpl();

        private PipeImpl audioTxPipe = new PipeImpl();

        private void join(BaseConnection connection1, BaseConnection connection2) {
            party1.connection = connection1;
            party2.connection = connection2;
            party1.source = connection1.audioChannel.splitter.newOutput();
            party2.sink = connection2.audioChannel.mixer.newInput();
            audioRxPipe.connect(party2.sink);
            audioRxPipe.connect(party1.source);
            party2.source = connection2.audioChannel.splitter.newOutput();
            party1.sink = connection1.audioChannel.mixer.newInput();
            audioTxPipe.connect(party1.sink);
            audioTxPipe.connect(party2.source);
            audioRxPipe.start();
            audioTxPipe.start();
        }

        public boolean match(BaseConnection connection) {
            return connection == party1.connection || connection == party2.connection;
        }

        public void unjoin() {
            audioTxPipe.stop();
            audioTxPipe.stop();
            party1.release();
            party2.release();
        }

        public void setDebug(boolean isDebug) {
            audioRxPipe.setDebug(isDebug);
        }

        private class Party {

            private BaseConnection connection;

            private MediaSource source;

            private MediaSink sink;

            private void release() {
                connection.audioChannel.splitter.release(source);
                connection.audioChannel.mixer.release(sink);
            }
        }
    }
}
