package javaclient3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javaclient3.structures.PlayerMsgHdr;
import javaclient3.structures.audiodsp.PlayerAudiodspCmd;
import javaclient3.structures.audiodsp.PlayerAudiodspConfig;
import javaclient3.structures.audiodsp.PlayerAudiodspData;
import javaclient3.xdr.OncRpcException;
import javaclient3.xdr.XdrBufferDecodingStream;
import javaclient3.xdr.XdrBufferEncodingStream;

/**
 * The audiodsp interface is used to control sound hardware, if equipped.
 * @deprecated Functionality moved to {@link AudioInterface audio} interface.
 * @author Radu Bogdan Rusu
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class AudioDSPInterface extends PlayerDevice {

    private static final boolean isDebugging = PlayerClient.isDebugging;

    private Logger logger = Logger.getLogger(AudioDSPInterface.class.getName());

    private PlayerAudiodspData padata;

    private boolean readyPadata = false;

    private PlayerAudiodspConfig paconfig;

    private boolean readyPaconfig = false;

    /**
     * Constructor for AudioDSPInterface.
     * @param pc a reference to the PlayerClient object
     */
    public AudioDSPInterface(PlayerClient pc) {
        super(pc);
    }

    /**
     * The audiodsp interface reads the audio stream from /dev/dsp (which is
     * assumed to be associated with a sound card connected to a microphone)
     * and performs some analysis on it. PLAYER_AUDIODSP_MAX_FREQS number of
     * frequency/amplitude pairs are then returned as data.
     */
    public synchronized void readData(PlayerMsgHdr header) {
        try {
            switch(header.getSubtype()) {
                case PLAYER_AUDIODSP_DATA_TONES:
                    {
                        this.timestamp = header.getTimestamp();
                        byte[] buffer = new byte[4];
                        is.readFully(buffer, 0, 4);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        int frequencyCount = xdr.xdrDecodeInt();
                        xdr.endDecoding();
                        xdr.close();
                        buffer = new byte[PLAYER_AUDIODSP_MAX_FREQS * 4];
                        is.readFully(buffer, 0, frequencyCount * 4);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        float[] frequencies = xdr.xdrDecodeFloatVector();
                        xdr.endDecoding();
                        xdr.close();
                        buffer = new byte[4];
                        is.readFully(buffer, 0, 4);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        int amplitudeCount = xdr.xdrDecodeInt();
                        xdr.endDecoding();
                        xdr.close();
                        buffer = new byte[PLAYER_AUDIODSP_MAX_FREQS * 4];
                        is.readFully(buffer, 0, amplitudeCount * 4);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        float[] amplitudes = xdr.xdrDecodeFloatVector();
                        xdr.endDecoding();
                        xdr.close();
                        padata = new PlayerAudiodspData();
                        padata.setFrequency_count(frequencyCount);
                        padata.setFrequency(frequencies);
                        padata.setAmplitude_count(amplitudeCount);
                        padata.setAmplitude(amplitudes);
                        readyPadata = true;
                        break;
                    }
            }
        } catch (IOException e) {
            throw new PlayerException("[AudioDSP] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[AudioDSP] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }

    /**
     * Get the data.
     * @return an object of type PlayerAudiodspData containing the requested data
     */
    public PlayerAudiodspData getData() {
        return this.padata;
    }

    /**
     * Check if data is available.
     * @return true if ready, false if not ready
     */
    public boolean isDataReady() {
        if (readyPadata) {
            readyPadata = false;
            return true;
        }
        return false;
    }

    /**
     * Get the configuration data.
     * @return an object of type PlayerAudiodspConfig containing the requested data
     */
    public PlayerAudiodspConfig getConfig() {
        return this.paconfig;
    }

    /**
     * Check if configuration data is available.
     * @return true if ready, false if not ready
     */
    public boolean isConfigReady() {
        if (readyPaconfig) {
            readyPaconfig = false;
            return true;
        }
        return false;
    }

    /**
     * The audiodsp interface accepts commands to produce fixed-frequency
     * tones or binary phase shift keyed (BPSK) chirps through /dev/dsp
     * (which is assumed to be associated with a sound card to which a
     * speaker is attached). The command subtype, which should be
     * PLAYER_AUDIODSP_PLAY_TONE, PLAYER_AUDIODSP_PLAY_CHIRP, or
     * PLAYER_AUDIODSP_REPLAY determines what to do.
     * @param subtype The packet subtype. Set to PLAYER_AUDIODSP_PLAY_TONE to play
     * a single frequency; bitString and bitStringLen do not need to be set. Set to
     * PLAYER_AUDIODSP_PLAY_CHIRP to play a BPSKeyed chirp; bitString should contain
     * the binary string to encode, and bitStringLen set to the length of the
     * bitString. Set to PLAYER_AUDIODSP_REPLAY to replay the last sound.
     * @param pacmd a PlayerAudiodspCmd structure holding the data to send
     */
    public void playTone(int subtype, PlayerAudiodspCmd pacmd) {
        try {
            int leftOvers = 0;
            if ((pacmd.getBit_string_count() % 4) != 0) leftOvers = 4 - (pacmd.getBit_string_count() % 4);
            int size = 16 + 4 + pacmd.getBit_string().length + leftOvers;
            sendHeader(PLAYER_MSGTYPE_CMD, subtype, size);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(size);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeFloat(pacmd.getFrequency());
            xdr.xdrEncodeFloat(pacmd.getAmplitude());
            xdr.xdrEncodeFloat(pacmd.getDuration());
            xdr.xdrEncodeInt(pacmd.getBit_string_count());
            xdr.xdrEncodeByte((byte) pacmd.getBit_string_count());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.write(pacmd.getBit_string());
            byte[] buf = new byte[leftOvers];
            os.write(buf, 0, leftOvers);
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[AudioDSP] : Couldn't send command: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[AudioDSP] : Error while XDR-encoding command: " + e.toString(), e);
        }
    }

    /**
     * Configuration request : Get audio properties.
     * <br><br>
     * The audiodsp configuration can be queried using the PLAYER_AUDIODSP_GET_CONFIG
     * request and modified using the PLAYER_AUDIODSP_SET_CONFIG request.
     * <br><br>
     * The sample format is defined in sys/soundcard.h, and defines the byte size and
     * endian format for each sample.
     * <br><br>
     * The sample rate defines the Hertz at which to sample.
     * <br><br>
     * Mono or stereo sampling is defined in the channels parameter where 1==mono and
     * 2==stereo.<br /><br />
     * See the player_audiodsp_config structure from player.h
     */
    public void getAudioProperties() {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIODSP_GET_CONFIG, 0);
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[AudioDSP] : Couldn't send PLAYER_AUDIODSP_GET_CONFIG " + "command: " + e.toString(), e);
        }
    }

    /**
     * Configuration request : Set audio properties.
     * <br><br>
     * The audiodsp configuration can be queried using the PLAYER_AUDIODSP_GET_CONFIG
     * request and modified using the PLAYER_AUDIODSP_SET_CONFIG request.
     * <br><br>
     * The sample format is defined in sys/soundcard.h, and defines the byte size and
     * endian format for each sample.
     * <br><br>
     * The sample rate defines the Hertz at which to sample.
     * <br><br>
     * Mono or stereo sampling is defined in the channels parameter where 1==mono and
     * 2==stereo.<br /><br />
     * See the player_audiodsp_config structure from player.h
     * @param paconfig a PlayerAudiodspConfig structure holding the data to send
     */
    public void setAudioProperties(PlayerAudiodspConfig paconfig) {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_AUDIODSP_SET_CONFIG, 12);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(12);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(paconfig.getFormat());
            xdr.xdrEncodeFloat(paconfig.getFrequency());
            xdr.xdrEncodeInt(paconfig.getChannels());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[AudioDSP] : Couldn't send PLAYER_AUDIODSP_SET_CONFIG " + "request: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[AudioDSP] : Error while XDR-encoding SET_CONFIG request: " + e.toString(), e);
        }
    }

    /**
     * Handle acknowledgement response messages
     * @param header Player header
     */
    public void handleResponse(PlayerMsgHdr header) {
        try {
            switch(header.getSubtype()) {
                case PLAYER_AUDIODSP_GET_CONFIG:
                    {
                        paconfig = new PlayerAudiodspConfig();
                        byte[] buffer = new byte[12];
                        is.readFully(buffer, 0, 12);
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        paconfig.setFormat(xdr.xdrDecodeInt());
                        paconfig.setFrequency(xdr.xdrDecodeFloat());
                        paconfig.setChannels(xdr.xdrDecodeInt());
                        xdr.endDecoding();
                        xdr.close();
                        readyPaconfig = true;
                        break;
                    }
                case PLAYER_AUDIODSP_SET_CONFIG:
                    {
                        break;
                    }
                default:
                    {
                        if (isDebugging) logger.log(Level.FINEST, "[AudioDSP][Debug] : " + "Unexpected response " + header.getSubtype() + " of size = " + header.getSize());
                        break;
                    }
            }
        } catch (IOException e) {
            throw new PlayerException("[AudioDSP] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[AudioDSP] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }
}
