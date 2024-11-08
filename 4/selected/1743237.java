package javaclient3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javaclient3.structures.PlayerMsgHdr;
import javaclient3.structures.audio.PlayerAudioMixerChannel;
import javaclient3.structures.audio.PlayerAudioMixerChannelDetail;
import javaclient3.structures.audio.PlayerAudioMixerChannelList;
import javaclient3.structures.audio.PlayerAudioMixerChannelListDetail;
import javaclient3.structures.audio.PlayerAudioSample;
import javaclient3.structures.audio.PlayerAudioSeq;
import javaclient3.structures.audio.PlayerAudioSeqItem;
import javaclient3.structures.audio.PlayerAudioState;
import javaclient3.structures.audio.PlayerAudioWav;
import javaclient3.xdr.OncRpcException;
import javaclient3.xdr.XdrBufferDecodingStream;
import javaclient3.xdr.XdrBufferEncodingStream;

/**
 * Interface to an audio system. The audio interface is used to control sound
 * hardware. The interface provides four sets of functionality:
 * <UL>
 * <LI> raw waveform playback and recording
 * <LI> audio sample playback and loading
 * <LI> sequencer support (tone playback and recording)
 * <LI> mixer interface (control of sound levels)
 * </UL>
 * @author Jorge Santos Simon
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class AudioInterface extends PlayerDevice {

    private static final boolean isDebugging = PlayerClient.isDebugging;

    private Logger logger = Logger.getLogger(AudioInterface.class.getName());

    private PlayerAudioWav paWavData;

    private boolean readyPaWavData = false;

    private PlayerAudioSample paSmpData;

    private boolean readyPaSmpData = false;

    private PlayerAudioSeq paSeqData;

    private boolean readyPaSeqData = false;

    private PlayerAudioMixerChannelList paMclData;

    private boolean readyPaMclData = false;

    private PlayerAudioMixerChannelListDetail paMcdData;

    private boolean readyPaMcdData = false;

    private PlayerAudioState paSttData;

    private boolean readyPaSttData = false;

    /**
     * Constructor for AudioInterface.
     * @param pc A reference to the PlayerClient object.
     */
    public AudioInterface(PlayerClient pc) {
        super(pc);
    }

    /**
     * Read audio data. The audio interface can read four kinds of data:
     * <UL>
     * <LI> raw waveform playback and recording
     * <LI> audio sample playback and loading
     * <LI> sequencer support (tone playback and recording)
     * <LI> mixer interface (control of sound levels)
     * </UL>
     * @param header Player message header.
     */
    public synchronized void readData(PlayerMsgHdr header) {
        try {
            switch(header.getSubtype()) {
                case PLAYER_AUDIO_DATA_WAV_REC:
                    {
                        this.timestamp = header.getTimestamp();
                        paWavData = readWaveform();
                        readyPaWavData = true;
                        break;
                    }
                case PLAYER_AUDIO_DATA_SEQ:
                    {
                        this.timestamp = header.getTimestamp();
                        paSeqData = new PlayerAudioSeq();
                        byte[] buffer = new byte[8];
                        is.readFully(buffer, 0, 8);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        int tonesCount = xdr.xdrDecodeInt();
                        xdr.endDecoding();
                        xdr.close();
                        buffer = new byte[tonesCount * 16];
                        is.readFully(buffer, 0, tonesCount * 16);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        PlayerAudioSeqItem[] tones = new PlayerAudioSeqItem[tonesCount];
                        for (int i = 0; i < tonesCount; i++) {
                            PlayerAudioSeqItem seqItem = new PlayerAudioSeqItem();
                            seqItem.setFrequency(xdr.xdrDecodeFloat());
                            seqItem.setDuration(xdr.xdrDecodeFloat());
                            seqItem.setAmplitude(xdr.xdrDecodeFloat());
                            seqItem.setLink(xdr.xdrDecodeBoolean());
                            tones[i] = seqItem;
                        }
                        xdr.endDecoding();
                        xdr.close();
                        paSeqData.setTones(tones);
                        readyPaSeqData = true;
                        break;
                    }
                case PLAYER_AUDIO_DATA_MIXER_CHANNEL:
                    {
                        this.timestamp = header.getTimestamp();
                        readMixerChannels();
                        break;
                    }
                case PLAYER_AUDIO_DATA_STATE:
                    {
                        this.timestamp = header.getTimestamp();
                        paSttData = new PlayerAudioState();
                        byte[] buffer = new byte[4];
                        is.readFully(buffer, 0, 4);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        paSttData.setState(xdr.xdrDecodeInt());
                        xdr.endDecoding();
                        xdr.close();
                        readyPaSttData = true;
                        break;
                    }
            }
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }

    /**
     * Get digitized waveform data.
     * @return An object of type PlayerAudioWav containing the requested data.
     */
    public PlayerAudioWav getWaveformData() {
        return this.paWavData;
    }

    /**
     * Check if waveform data is available.
     * @return True if ready, false if not ready
     */
    public boolean isWaveformDataReady() {
        if (readyPaWavData) {
            readyPaWavData = false;
            return true;
        }
        return false;
    }

    /**
     * Get pre-stored audio sample data.
     * @return An object of type PlayerAudioSample containing the requested data.
     */
    public PlayerAudioSample getSampleData() {
        return this.paSmpData;
    }

    /**
     * Check if audio sample data is available.
     * @return True if ready, false if not ready
     */
    public boolean isSampleDataReady() {
        if (readyPaSmpData) {
            readyPaSmpData = false;
            return true;
        }
        return false;
    }

    /**
     * Get sequence of tones data.
     * @return An object of type PlayerAudioSeq containing the requested data.
     */
    public PlayerAudioSeq getSequenceData() {
        return this.paSeqData;
    }

    /**
     * Check if sequence data is available.
     * @return True if ready, false if not ready.
     */
    public boolean isSequenceDataReady() {
        if (readyPaSeqData) {
            readyPaSeqData = false;
            return true;
        }
        return false;
    }

    /**
     * Get the mixer channels sound level data.
     * @return An object of type PlayerAudioMixerChannelList containing the
     *  requested data.
     */
    public PlayerAudioMixerChannelList getChannelsData() {
        return this.paMclData;
    }

    /**
     * Check if channel levels data is available.
     * @return True if ready, false if not ready.
     */
    public boolean isChannelsDataReady() {
        if (readyPaMclData) {
            readyPaMclData = false;
            return true;
        }
        return false;
    }

    /**
     * Get data containing the list of mixer channels details.
     * @return An object of type PlayerAudioMixerChannelListDetail containing the
     *  requested data.
     */
    public PlayerAudioMixerChannelListDetail getDetailsData() {
        return this.paMcdData;
    }

    /**
     * Check if mixer channels details data is available.
     * @return True if ready, false if not ready
     */
    public boolean isDetailsDataReady() {
        if (readyPaMcdData) {
            readyPaMcdData = false;
            return true;
        }
        return false;
    }

    /**
     * Get the state of target audio device data.
     * @return An object of type PlayerAudioState containing the requested data.
     */
    public PlayerAudioState getStateData() {
        return this.paSttData;
    }

    /**
     * Check if device state data is available.
     * @return True if ready, false if not ready
     */
    public boolean isStateDataReady() {
        if (readyPaSttData) {
            readyPaSttData = false;
            return true;
        }
        return false;
    }

    /**
     * Command subtype: wav_play_cmd, play a raw data block.
     * @param pacmd  A PlayerAudioWav structure holding the data to send.
     */
    public void playWaveform(PlayerAudioWav pacmd) {
        try {
            int leftOvers = 0;
            if ((pacmd.getData_count() % 4) != 0) leftOvers = 4 - (pacmd.getData_count() % 4);
            int size = 8 + pacmd.getData_count() + leftOvers + 4;
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_WAV_PLAY, size);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(size);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pacmd.getData_count());
            xdr.xdrEncodeInt(pacmd.getData_count());
            xdr.xdrEncodeOpaque(pacmd.getData());
            xdr.xdrEncodeInt(pacmd.getFormat());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Command subtype: wav_stream_rec_cmd, start/stop recording, data will be
     * returned as data blocks.
     * @param state  PLAYER_AUDIO_STATE_STOPPED or PLAYER_AUDIO_STATE_RECORDING.
     */
    public void recordWavStream(int state) {
        try {
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_WAV_STREAM_REC, 4);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(4);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(state);
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Command subtype: sample_play_cmd, play a pre-stored audio sample.
     * @param index  Index of the pre-stored audio sample to play.
     */
    public void playSample(int index) {
        try {
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_SAMPLE_PLAY, 4);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(4);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(index);
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Command subtype: seq_play_cmd, play a sequence of tones.
     * @param pacmd A PlayerAudioSeq structure holding the data to send.
     */
    public void playSequence(PlayerAudioSeq pacmd) {
        try {
            int size = 8 + (pacmd.getTones_count() * 16);
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_SEQ_PLAY, size);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(size);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pacmd.getTones_count());
            xdr.xdrEncodeInt(pacmd.getTones_count());
            PlayerAudioSeqItem[] tones = pacmd.getTones();
            for (int i = 0; i < tones.length; i++) {
                xdr.xdrEncodeFloat(tones[i].getFrequency());
                xdr.xdrEncodeFloat(tones[i].getDuration());
                xdr.xdrEncodeFloat(tones[i].getAmplitude());
                xdr.xdrEncodeBoolean(tones[i].getLink());
            }
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Command subtype: mixer_channel_cmd, set level for an audio channel.
     * @param pacmd  A PlayerAudioMixerChannel structure holding the data to send.
     */
    public void mixerChannel(PlayerAudioMixerChannel pacmd) {
        try {
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_MIXER_CHANNEL, 20);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(20);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(1);
            xdr.xdrEncodeInt(1);
            xdr.xdrEncodeFloat(pacmd.getAmplitude());
            xdr.xdrEncodeBoolean(pacmd.getActive());
            xdr.xdrEncodeInt(pacmd.getIndex());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Command subtype: set mixer levels for multiple channels.
     * @param pacmd  A PlayerAudioMixerChannelList structure holding the data to send.
     */
    public void mixerMultiChannels(PlayerAudioMixerChannelList pacmd) {
        try {
            int size = 8 + pacmd.getChannels_count() * 12;
            sendHeader(PLAYER_MSGTYPE_CMD, PLAYER_AUDIO_CMD_MIXER_CHANNEL, size);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(size);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pacmd.getChannels_count());
            xdr.xdrEncodeInt(pacmd.getChannels_count());
            PlayerAudioMixerChannel[] channels = pacmd.getChannels();
            for (int i = 0; i < channels.length; i++) {
                xdr.xdrEncodeFloat(channels[i].getAmplitude());
                xdr.xdrEncodeBoolean(channels[i].getActive());
                xdr.xdrEncodeInt(channels[i].getIndex());
            }
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: wav_rec_req, record a fixed size data block, in structure
     * player_audio_wav_t.
     */
    public void recordWaveform() {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_WAV_REC, 0);
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: sample_load_req, store a sample.
     * @param pareq  A PlayerAudioSample structure holding the data to send.
     */
    public void loadSample(PlayerAudioSample pareq) {
        try {
            PlayerAudioWav sample = pareq.getSample();
            int size = sample.getData_count() + 12 + 4;
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_SAMPLE_LOAD, size);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(size);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(sample.getData_count());
            xdr.xdrEncodeDynamicOpaque(sample.getData());
            xdr.xdrEncodeInt(sample.getFormat());
            xdr.xdrEncodeInt(pareq.getIndex());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding request: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: sample_retrieve_req, retrieve a stored sample.
     * @param index  Index of the sample to retrieve.
     */
    public void retrieveSample(int index) {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_SAMPLE_RETRIEVE, 4);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(4);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(index);
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding request: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: sample_rec_req, record a new sample.
     * @param index  Index of the sample to record.
     * @param length  Length of the sample to record.
     */
    public void recordSample(int index, int length) {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_SAMPLE_REC, 8);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(8);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(index);
            xdr.xdrEncodeInt(length);
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-encoding request: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: mixer_channel_list_req, request the list of channels.
     */
    public void getMixerDetails() {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_MIXER_CHANNEL_LIST, 0);
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        }
    }

    /**
     * Request subtype: mixer_channel_level_req, request the channel levels.
     */
    public void getMixerLevels() {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIO_REQ_MIXER_CHANNEL_LEVEL, 0);
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Couldn't send request: " + e.toString(), e);
        }
    }

    /**
     * Handle acknowledgement response messages.
     * @param header Player message header.
     */
    public void handleResponse(PlayerMsgHdr header) {
        try {
            switch(header.getSubtype()) {
                case PLAYER_AUDIO_REQ_WAV_REC:
                    {
                        this.timestamp = header.getTimestamp();
                        paWavData = readWaveform();
                        readyPaWavData = true;
                        break;
                    }
                case PLAYER_AUDIO_REQ_SAMPLE_LOAD:
                    {
                        break;
                    }
                case PLAYER_AUDIO_REQ_SAMPLE_RETRIEVE:
                    {
                        this.timestamp = header.getTimestamp();
                        PlayerAudioSample paSmpData = new PlayerAudioSample();
                        paSmpData.setSample(readWaveform());
                        byte[] buffer = new byte[4];
                        is.readFully(buffer, 0, 4);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        paSmpData.setIndex(xdr.xdrDecodeInt());
                        xdr.endDecoding();
                        xdr.close();
                        readyPaSmpData = true;
                        break;
                    }
                case PLAYER_AUDIO_REQ_SAMPLE_REC:
                    {
                        break;
                    }
                case PLAYER_AUDIO_REQ_MIXER_CHANNEL_LEVEL:
                    {
                        this.timestamp = header.getTimestamp();
                        readMixerChannels();
                        break;
                    }
                case PLAYER_AUDIO_REQ_MIXER_CHANNEL_LIST:
                    {
                        this.timestamp = header.getTimestamp();
                        paMcdData = new PlayerAudioMixerChannelListDetail();
                        byte[] buffer = new byte[8];
                        is.readFully(buffer, 0, 8);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        int detailsCount = xdr.xdrDecodeInt();
                        xdr.endDecoding();
                        xdr.close();
                        PlayerAudioMixerChannelDetail[] details = new PlayerAudioMixerChannelDetail[detailsCount];
                        for (int i = 0; i < detailsCount; i++) {
                            details[i] = new PlayerAudioMixerChannelDetail();
                            buffer = new byte[4];
                            is.readFully(buffer, 0, 4);
                            xdr = new XdrBufferDecodingStream(buffer);
                            xdr.beginDecoding();
                            int nameCount = xdr.xdrDecodeInt();
                            xdr.endDecoding();
                            xdr.close();
                            buffer = new byte[4 + nameCount + 4];
                            is.readFully(buffer, 0, 4 + nameCount + 4);
                            xdr = new XdrBufferDecodingStream(buffer);
                            xdr.beginDecoding();
                            details[i].setName(xdr.xdrDecodeString());
                            details[i].setCaps(xdr.xdrDecodeByte());
                            xdr.endDecoding();
                            xdr.close();
                        }
                        paMcdData.setDetails(details);
                        buffer = new byte[8];
                        is.readFully(buffer, 0, 8);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        paMcdData.setDefaultOutput(xdr.xdrDecodeInt());
                        paMcdData.setDefaultInput(xdr.xdrDecodeInt());
                        xdr.endDecoding();
                        xdr.close();
                        readyPaMcdData = true;
                        break;
                    }
                default:
                    {
                        if (isDebugging) logger.log(Level.FINEST, "[Audio][Debug] : " + "Unexpected response " + header.getSubtype() + " of size = " + header.getSize());
                        break;
                    }
            }
        } catch (IOException e) {
            throw new PlayerException("[Audio] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Audio] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }

    /**
     * Read recorded digitized waveform from the server.
     * @return The recorded digitized waveform.
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    private PlayerAudioWav readWaveform() throws IOException, OncRpcException {
        PlayerAudioWav wav = new PlayerAudioWav();
        byte[] buffer = new byte[8];
        is.readFully(buffer, 0, 8);
        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
        xdr.beginDecoding();
        int dataCount = xdr.xdrDecodeInt();
        xdr.endDecoding();
        xdr.close();
        buffer = new byte[dataCount + 4];
        is.readFully(buffer, 0, dataCount + 4);
        xdr = new XdrBufferDecodingStream(buffer);
        xdr.beginDecoding();
        wav.setData(xdr.xdrDecodeOpaque(dataCount));
        wav.setFormat(xdr.xdrDecodeInt());
        xdr.endDecoding();
        xdr.close();
        return wav;
    }

    /**
     * Read mixer channels sound level.
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    private void readMixerChannels() throws IOException, OncRpcException {
        paMclData = new PlayerAudioMixerChannelList();
        byte[] buffer = new byte[8];
        is.readFully(buffer, 0, 8);
        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
        xdr.beginDecoding();
        int channelsCount = xdr.xdrDecodeInt();
        xdr.endDecoding();
        xdr.close();
        buffer = new byte[channelsCount * 12];
        is.readFully(buffer, 0, channelsCount * 12);
        xdr = new XdrBufferDecodingStream(buffer);
        xdr.beginDecoding();
        PlayerAudioMixerChannel[] channels = new PlayerAudioMixerChannel[channelsCount];
        for (int i = 0; i < channelsCount; i++) {
            PlayerAudioMixerChannel channel = new PlayerAudioMixerChannel();
            channel.setAmplitude(xdr.xdrDecodeFloat());
            channel.setActive(xdr.xdrDecodeBoolean());
            channel.setIndex(xdr.xdrDecodeInt());
            channels[i] = channel;
        }
        xdr.endDecoding();
        xdr.close();
        paMclData.setChannels(channels);
        readyPaMclData = true;
    }
}
