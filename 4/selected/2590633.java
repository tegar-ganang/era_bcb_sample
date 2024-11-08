package org.newdawn.slick.openal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.BufferUtils;
import org.newdawn.slick.util.Log;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * An input stream that can extract ogg data. This class is a bit of an experiment with continuations
 * so uses thread where possibly not required. It's just a test to see if continuations make sense in 
 * some cases.
 *
 * @author kevin
 */
public class OggInputStream extends InputStream implements AudioInputStream {

    /** The conversion buffer size */
    private int convsize = 4096 * 4;

    /** The buffer used to read OGG file */
    private byte[] convbuffer = new byte[convsize];

    /** The stream we're reading the OGG file from */
    private InputStream input;

    /** The audio information from the OGG header */
    private Info oggInfo = new Info();

    /** True if we're at the end of the available data */
    private boolean endOfStream;

    /** The Vorbis SyncState used to decode the OGG */
    private SyncState syncState = new SyncState();

    /** The Vorbis Stream State used to decode the OGG */
    private StreamState streamState = new StreamState();

    /** The current OGG page */
    private Page page = new Page();

    /** The current packet page */
    private Packet packet = new Packet();

    /** The comment read from the OGG file */
    private Comment comment = new Comment();

    /** The Vorbis DSP stat eused to decode the OGG */
    private DspState dspState = new DspState();

    /** The OGG block we're currently working with to convert PCM */
    private Block vorbisBlock = new Block(dspState);

    /** Temporary scratch buffer */
    byte[] buffer;

    /** The number of bytes read */
    int bytes = 0;

    /** The true if we should be reading big endian */
    boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    /** True if we're reached the end of the current bit stream */
    boolean endOfBitStream = true;

    /** True if we're initialise the OGG info block */
    boolean inited = false;

    /** The index into the byte array we currently read from */
    private int readIndex;

    /** The byte array store used to hold the data read from the ogg */
    private ByteBuffer pcmBuffer = BufferUtils.createByteBuffer(4096 * 200);

    /**
	 * Create a new stream to decode OGG data
	 * 
	 * @param input The input stream from which to read the OGG file
	 * @throws IOException Indicates a failure to read from the supplied stream
	 */
    public OggInputStream(InputStream input) throws IOException {
        this.input = input;
        init();
    }

    /**
	 * @see org.newdawn.slick.openal.AudioInputStream#getChannels()
	 */
    public int getChannels() {
        return oggInfo.channels;
    }

    /**
	 * @see org.newdawn.slick.openal.AudioInputStream#getRate()
	 */
    public int getRate() {
        return oggInfo.rate;
    }

    /**
	 * Initialise the streams and thread involved in the streaming of OGG data
	 * 
	 * @throws IOException Indicates a failure to link up the streams
	 */
    private void init() throws IOException {
        initVorbis();
        readPCM();
    }

    /**
	 * @see java.io.InputStream#available()
	 */
    public int available() {
        return endOfStream ? 0 : 1;
    }

    /**
	 * Initialise the vorbis decoding
	 */
    private void initVorbis() {
        syncState.init();
    }

    /**
	 * Get a page and packet from that page
	 *
	 * @return True if there was a page available
	 */
    private boolean getPageAndPacket() {
        int index = syncState.buffer(4096);
        buffer = syncState.data;
        if (buffer == null) {
            endOfStream = true;
            return false;
        }
        try {
            bytes = input.read(buffer, index, 4096);
        } catch (Exception e) {
            Log.error("Failure reading in vorbis");
            Log.error(e);
            endOfStream = true;
            return false;
        }
        syncState.wrote(bytes);
        if (syncState.pageout(page) != 1) {
            if (bytes < 4096) return false;
            Log.error("Input does not appear to be an Ogg bitstream.");
            endOfStream = true;
            return false;
        }
        streamState.init(page.serialno());
        oggInfo.init();
        comment.init();
        if (streamState.pagein(page) < 0) {
            Log.error("Error reading first page of Ogg bitstream data.");
            endOfStream = true;
            return false;
        }
        if (streamState.packetout(packet) != 1) {
            Log.error("Error reading initial header packet.");
            endOfStream = true;
            return false;
        }
        if (oggInfo.synthesis_headerin(comment, packet) < 0) {
            Log.error("This Ogg bitstream does not contain Vorbis audio data.");
            endOfStream = true;
            return false;
        }
        int i = 0;
        while (i < 2) {
            while (i < 2) {
                int result = syncState.pageout(page);
                if (result == 0) break;
                if (result == 1) {
                    streamState.pagein(page);
                    while (i < 2) {
                        result = streamState.packetout(packet);
                        if (result == 0) break;
                        if (result == -1) {
                            Log.error("Corrupt secondary header.  Exiting.");
                            endOfStream = true;
                            return false;
                        }
                        oggInfo.synthesis_headerin(comment, packet);
                        i++;
                    }
                }
            }
            index = syncState.buffer(4096);
            buffer = syncState.data;
            try {
                bytes = input.read(buffer, index, 4096);
            } catch (Exception e) {
                Log.error("Failed to read Vorbis: ");
                Log.error(e);
                endOfStream = true;
                return false;
            }
            if (bytes == 0 && i < 2) {
                Log.error("End of file before finding all Vorbis headers!");
                endOfStream = true;
                return false;
            }
            syncState.wrote(bytes);
        }
        convsize = 4096 / oggInfo.channels;
        dspState.synthesis_init(oggInfo);
        vorbisBlock.init(dspState);
        return true;
    }

    /**
	 * Decode the OGG file as shown in the jogg/jorbis examples
	 * 
	 * @throws IOException Indicates a failure to read from the supplied stream
	 */
    private void readPCM() throws IOException {
        boolean wrote = false;
        while (true) {
            if (endOfBitStream) {
                if (!getPageAndPacket()) {
                    break;
                }
                endOfBitStream = false;
            }
            if (!inited) {
                inited = true;
                return;
            }
            float[][][] _pcm = new float[1][][];
            int[] _index = new int[oggInfo.channels];
            while (!endOfBitStream) {
                while (!endOfBitStream) {
                    int result = syncState.pageout(page);
                    if (result == 0) {
                        break;
                    }
                    if (result == -1) {
                        Log.error("Corrupt or missing data in bitstream; continuing...");
                    } else {
                        streamState.pagein(page);
                        while (true) {
                            result = streamState.packetout(packet);
                            if (result == 0) break;
                            if (result == -1) {
                            } else {
                                int samples;
                                if (vorbisBlock.synthesis(packet) == 0) {
                                    dspState.synthesis_blockin(vorbisBlock);
                                }
                                while ((samples = dspState.synthesis_pcmout(_pcm, _index)) > 0) {
                                    float[][] pcm = _pcm[0];
                                    int bout = (samples < convsize ? samples : convsize);
                                    for (int i = 0; i < oggInfo.channels; i++) {
                                        int ptr = i * 2;
                                        int mono = _index[i];
                                        for (int j = 0; j < bout; j++) {
                                            int val = (int) (pcm[i][mono + j] * 32767.);
                                            if (val > 32767) {
                                                val = 32767;
                                            }
                                            if (val < -32768) {
                                                val = -32768;
                                            }
                                            if (val < 0) val = val | 0x8000;
                                            if (bigEndian) {
                                                convbuffer[ptr] = (byte) (val >>> 8);
                                                convbuffer[ptr + 1] = (byte) (val);
                                            } else {
                                                convbuffer[ptr] = (byte) (val);
                                                convbuffer[ptr + 1] = (byte) (val >>> 8);
                                            }
                                            ptr += 2 * (oggInfo.channels);
                                        }
                                    }
                                    int bytesToWrite = 2 * oggInfo.channels * bout;
                                    pcmBuffer.put(convbuffer, 0, bytesToWrite);
                                    wrote = true;
                                    dspState.synthesis_read(bout);
                                }
                            }
                        }
                        if (page.eos() != 0) {
                            endOfBitStream = true;
                        }
                        if ((!endOfBitStream) && (wrote)) {
                            return;
                        }
                    }
                }
                if (!endOfBitStream) {
                    bytes = 0;
                    int index = syncState.buffer(4096);
                    if (index >= 0) {
                        buffer = syncState.data;
                        try {
                            bytes = input.read(buffer, index, 4096);
                        } catch (Exception e) {
                            Log.error("Failure during vorbis decoding");
                            Log.error(e);
                            endOfStream = true;
                            return;
                        }
                    } else {
                        bytes = 0;
                    }
                    syncState.wrote(bytes);
                    if (bytes == 0) {
                        endOfBitStream = true;
                    }
                }
            }
            streamState.clear();
            vorbisBlock.clear();
            dspState.clear();
            oggInfo.clear();
        }
        syncState.clear();
        endOfStream = true;
    }

    /**
	 * @see java.io.InputStream#read()
	 */
    public int read() throws IOException {
        if (readIndex >= pcmBuffer.position()) {
            pcmBuffer.clear();
            readPCM();
            readIndex = 0;
        }
        if (readIndex >= pcmBuffer.position()) {
            return -1;
        }
        int value = pcmBuffer.get(readIndex);
        if (value < 0) {
            value = 256 + value;
        }
        readIndex++;
        return value;
    }

    /**
	 * @see org.newdawn.slick.openal.AudioInputStream#atEnd()
	 */
    public boolean atEnd() {
        return endOfStream && (readIndex >= pcmBuffer.position());
    }

    /**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            try {
                int value = read();
                if (value >= 0) {
                    b[i] = (byte) value;
                } else {
                    if (i == 0) {
                        return -1;
                    } else {
                        return i;
                    }
                }
            } catch (IOException e) {
                Log.error(e);
                return i;
            }
        }
        return len;
    }

    /**
	 * @see java.io.InputStream#read(byte[])
	 */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
	 * @see java.io.InputStream#close()
	 */
    public void close() throws IOException {
    }
}
