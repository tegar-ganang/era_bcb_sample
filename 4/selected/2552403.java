package jpcsp.media;

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.endianSwap32;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.connector.Connector;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Debug;
import jpcsp.util.FIFOByteBuffer;
import com.xuggle.ferry.Logger;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.io.IURLProtocolHandler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class MediaEngine {

    public static org.apache.log4j.Logger log = Modules.log;

    protected static final int AVSEEK_FLAG_BACKWARD = 1;

    protected static final int AVSEEK_FLAG_BYTE = 2;

    protected static final int AVSEEK_FLAG_ANY = 4;

    protected static final int AVSEEK_FLAG_FRAME = 8;

    private static boolean initialized = false;

    private IContainer container;

    private int numStreams;

    private IStreamCoder videoCoder;

    private IStreamCoder audioCoder;

    private int videoStreamID;

    private int audioStreamID;

    private BufferedImage currentImg;

    private FIFOByteBuffer decodedAudioSamples;

    private int currentSamplesSize = 1024;

    private IVideoPicture videoPicture;

    private IAudioSamples audioSamples;

    private IConverter videoConverter;

    private IVideoResampler videoResampler;

    private int[] videoImagePixels;

    private int bufferAddress;

    private int bufferSize;

    private int bufferMpegOffset;

    private byte[] bufferData;

    private StreamState videoStreamState;

    private StreamState audioStreamState;

    private List<IPacket> freePackets = new LinkedList<IPacket>();

    private ExternalDecoder externalDecoder = new ExternalDecoder();

    private byte[] tempBuffer;

    private IContainer extContainer;

    public MediaEngine() {
        initXuggler();
    }

    public static void initXuggler() {
        if (!initialized) {
            try {
                Logger.setGlobalIsLogging(Logger.Level.LEVEL_DEBUG, false);
                Logger.setGlobalIsLogging(Logger.Level.LEVEL_ERROR, false);
                Logger.setGlobalIsLogging(Logger.Level.LEVEL_INFO, false);
                Logger.setGlobalIsLogging(Logger.Level.LEVEL_TRACE, false);
                Logger.setGlobalIsLogging(Logger.Level.LEVEL_WARN, false);
            } catch (NoClassDefFoundError e) {
                log.warn("Xuggler is not available on your platform");
            }
            initialized = true;
        }
    }

    public IContainer getContainer() {
        return container;
    }

    public IContainer getAudioContainer() {
        if (audioStreamState == null) {
            return null;
        }
        return audioStreamState.getContainer();
    }

    public IContainer getExtContainer() {
        return extContainer;
    }

    public int getNumStreams() {
        return numStreams;
    }

    public IStreamCoder getVideoCoder() {
        return videoCoder;
    }

    public IStreamCoder getAudioCoder() {
        return audioCoder;
    }

    public int getVideoStreamID() {
        return videoStreamID;
    }

    public int getAudioStreamID() {
        return audioStreamID;
    }

    public BufferedImage getCurrentImg() {
        return currentImg;
    }

    public int getCurrentAudioSamples(byte[] buffer) {
        if (decodedAudioSamples == null) {
            return 0;
        }
        int length = Math.min(buffer.length, decodedAudioSamples.length());
        if (length > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, length);
            length = decodedAudioSamples.readByteBuffer(byteBuffer);
        }
        return length;
    }

    public int getAudioSamplesSize() {
        return currentSamplesSize;
    }

    public void setAudioSamplesSize(int newSize) {
        currentSamplesSize = newSize;
    }

    public void release(IPacket packet) {
        if (packet != null) {
            freePackets.add(packet);
        }
    }

    public IPacket getPacket() {
        if (!freePackets.isEmpty()) {
            return freePackets.remove(0);
        }
        return IPacket.make();
    }

    private boolean readAu(StreamState state, SceMpegAu au) {
        boolean successful = true;
        if (state == null) {
            au.dts = 0;
            au.pts = 0;
        } else {
            while (true) {
                if (!getNextPacket(state)) {
                    if (state == videoStreamState) {
                        state.incrementTimestamps(sceMpeg.videoTimestampStep);
                    } else if (state == audioStreamState) {
                        state.incrementTimestamps(sceMpeg.audioTimestampStep);
                    }
                    successful = false;
                    break;
                }
                state.updateTimestamps();
                if (state.getPts() >= 90000) {
                    break;
                }
                decodePacket(state, 0);
            }
            state.getTimestamps(au);
        }
        return successful;
    }

    public boolean readVideoAu(SceMpegAu au) {
        boolean successful = readAu(videoStreamState, au);
        if (au.pts >= sceMpeg.videoTimestampStep) {
            au.dts = au.pts - sceMpeg.videoTimestampStep;
        }
        return successful;
    }

    public boolean readAudioAu(SceMpegAu au) {
        boolean successful = readAu(audioStreamState, au);
        au.dts = sceMpeg.UNKNOWN_TIMESTAMP;
        return successful;
    }

    public void getCurrentAudioAu(SceMpegAu au) {
        if (audioStreamState != null) {
            audioStreamState.getTimestamps(au);
        } else {
            au.pts += sceMpeg.audioTimestampStep;
        }
        au.dts = sceMpeg.UNKNOWN_TIMESTAMP;
    }

    public void getCurrentVideoAu(SceMpegAu au) {
        if (videoStreamState != null) {
            videoStreamState.getTimestamps(au);
        } else {
            au.pts += sceMpeg.videoTimestampStep;
        }
        if (au.pts >= sceMpeg.videoTimestampStep) {
            au.dts = au.pts - sceMpeg.videoTimestampStep;
        }
    }

    private int read32(byte[] data, int offset) {
        int n1 = data[offset] & 0xFF;
        int n2 = data[offset + 1] & 0xFF;
        int n3 = data[offset + 2] & 0xFF;
        int n4 = data[offset + 3] & 0xFF;
        return (n4 << 24) | (n3 << 16) | (n2 << 8) | n1;
    }

    public void init(byte[] bufferData) {
        this.bufferData = bufferData;
        this.bufferAddress = 0;
        this.bufferSize = endianSwap32(read32(bufferData, sceMpeg.PSMF_STREAM_SIZE_OFFSET));
        this.bufferMpegOffset = endianSwap32(read32(bufferData, sceMpeg.PSMF_STREAM_OFFSET_OFFSET));
        init();
    }

    public void init(int bufferAddress, int bufferSize, int bufferMpegOffset) {
        this.bufferAddress = bufferAddress;
        this.bufferSize = bufferSize;
        this.bufferMpegOffset = bufferMpegOffset;
        bufferData = new byte[sceMpeg.MPEG_HEADER_BUFFER_MINIMUM_SIZE];
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(bufferAddress, bufferData.length, 1);
        for (int i = 0; i < bufferData.length; i++) {
            bufferData[i] = (byte) memoryReader.readNext();
        }
        init();
    }

    public void init() {
        finish();
        videoStreamID = -1;
        audioStreamID = -1;
    }

    public void init(IURLProtocolHandler channel, boolean decodeVideo, boolean decodeAudio) {
        init();
        container = IContainer.make();
        container.setReadRetryCount(-1);
        if (container.open(channel, IContainer.Type.READ, null) < 0) {
            log.error("MediaEngine: Invalid container format!");
        }
        numStreams = container.getNumStreams();
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();
            if (videoStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamID = i;
                videoCoder = coder;
            } else if (audioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamID = i;
                audioCoder = coder;
            }
        }
        if (decodeVideo) {
            if (videoStreamID == -1) {
                log.error("MediaEngine: No video streams found!");
            } else if (videoCoder.open(null, null) < 0) {
                videoCoder.delete();
                videoCoder = null;
                log.error("MediaEngine: Can't open video decoder!");
            } else {
                videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                videoPicture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                    videoResampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
                    videoPicture = IVideoPicture.make(videoResampler.getOutputPixelFormat(), videoPicture.getWidth(), videoPicture.getHeight());
                }
                videoStreamState = new StreamState(this, videoStreamID, container, 0);
            }
        }
        if (decodeAudio) {
            if (audioStreamID == -1) {
                if (!initExtAudio()) {
                    log.error("MediaEngine: No audio streams found!");
                    audioStreamState = new StreamState(this, -1, null, sceMpeg.audioFirstTimestamp);
                }
            } else if (audioCoder.open(null, null) < 0) {
                audioCoder.delete();
                audioCoder = null;
                log.error("MediaEngine: Can't open audio decoder!");
            } else {
                audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
                decodedAudioSamples = new FIFOByteBuffer();
                audioStreamState = new StreamState(this, audioStreamID, container, 0);
            }
        }
    }

    private boolean getNextPacket(StreamState state) {
        if (state.isPacketEmpty()) {
            state.releasePacket();
            IPacket packet = state.getNextPacket();
            if (packet != null) {
                state.setPacket(packet);
            } else {
                IContainer container = state.getContainer();
                if (container == null) {
                    return false;
                }
                while (state.isPacketEmpty()) {
                    packet = getPacket();
                    if (container.readNextPacket(packet) < 0) {
                        release(packet);
                        return false;
                    }
                    int streamIndex = packet.getStreamIndex();
                    if (packet.getSize() <= 0) {
                        release(packet);
                    } else if (state.isStream(container, streamIndex)) {
                        state.setPacket(packet);
                    } else if (videoCoder != null && videoStreamState.isStream(container, streamIndex)) {
                        videoStreamState.addPacket(packet);
                    } else if (audioCoder != null && audioStreamState.isStream(container, streamIndex)) {
                        audioStreamState.addPacket(packet);
                    } else {
                        release(packet);
                    }
                }
            }
        }
        return true;
    }

    private boolean decodePacket(StreamState state, int requiredAudioBytes) {
        boolean complete = false;
        if (state == videoStreamState) {
            if (videoCoder == null) {
                state.releasePacket();
                complete = true;
            } else {
                complete = decodeVideoPacket(state);
            }
        } else if (state == audioStreamState) {
            if (audioCoder == null) {
                state.releasePacket();
                complete = true;
            } else {
                if (decodeAudioPacket(state)) {
                    if (decodedAudioSamples.length() >= requiredAudioBytes) {
                        complete = true;
                    }
                }
            }
        }
        return complete;
    }

    private boolean decodeVideoPacket(StreamState state) {
        boolean complete = false;
        while (!state.isPacketEmpty()) {
            int decodedBytes = videoCoder.decodeVideo(videoPicture, state.getPacket(), state.getOffset());
            if (decodedBytes < 0) {
                state.releasePacket();
                break;
            }
            state.updateTimestamps();
            state.consume(decodedBytes);
            if (videoPicture.isComplete()) {
                if (videoConverter != null) {
                    currentImg = videoConverter.toImage(videoPicture);
                }
                complete = true;
                break;
            }
        }
        return complete;
    }

    private boolean decodeAudioPacket(StreamState state) {
        boolean complete = false;
        while (!state.isPacketEmpty()) {
            int decodedBytes = audioCoder.decodeAudio(audioSamples, state.getPacket(), state.getOffset());
            if (decodedBytes < 0) {
                state.releasePacket();
                break;
            }
            state.updateTimestamps();
            state.consume(decodedBytes);
            if (audioSamples.isComplete()) {
                updateSoundSamples(audioSamples);
                complete = true;
                break;
            }
        }
        return complete;
    }

    public static String getExtAudioBasePath(int mpegStreamSize) {
        return String.format("%s%s%cMpeg-%d%c", Connector.baseDirectory, State.discId, File.separatorChar, mpegStreamSize, File.separatorChar);
    }

    public static String getExtAudioPath(int mpegStreamSize, String suffix) {
        return String.format("%sExtAudio.%s", getExtAudioBasePath(mpegStreamSize), suffix);
    }

    public boolean stepVideo() {
        return step(videoStreamState, 0);
    }

    public boolean stepAudio(int requiredAudioBytes) {
        boolean success = step(audioStreamState, requiredAudioBytes);
        if (decodedAudioSamples != null && decodedAudioSamples.length() > 0) {
            success = true;
        }
        return success;
    }

    private boolean step(StreamState state, int requiredAudioBytes) {
        boolean complete = false;
        if (state != null) {
            while (!complete) {
                if (!getNextPacket(state)) {
                    break;
                }
                complete = decodePacket(state, requiredAudioBytes);
            }
        }
        return complete;
    }

    private File getExtAudioFile() {
        String supportedFormats[] = { "wav", "mp3", "at3", "raw", "wma", "flac", "m4a" };
        for (int i = 0; i < supportedFormats.length; i++) {
            File f = new File(getExtAudioPath(bufferSize, supportedFormats[i]));
            if (f.canRead() && f.length() > 0) {
                return f;
            }
        }
        return null;
    }

    private boolean initExtAudio() {
        boolean useExtAudio = false;
        File extAudioFile = getExtAudioFile();
        if (extAudioFile == null && ExternalDecoder.isEnabled()) {
            if (bufferAddress == 0) {
                if (bufferData != null) {
                    externalDecoder.decodeExtAudio(bufferData, bufferSize, bufferMpegOffset);
                }
            } else {
                externalDecoder.decodeExtAudio(bufferAddress, bufferSize, bufferMpegOffset, bufferData);
            }
            extAudioFile = getExtAudioFile();
        }
        if (extAudioFile != null) {
            useExtAudio = initExtAudio(extAudioFile.toString());
        }
        return useExtAudio;
    }

    private boolean initExtAudio(String file) {
        extContainer = IContainer.make();
        if (log.isDebugEnabled()) {
            log.debug(String.format("initExtAudio %s", file));
        }
        IURLProtocolHandler fileProtocolHandler = new FileProtocolHandler(file);
        if (extContainer.open(fileProtocolHandler, IContainer.Type.READ, null) < 0) {
            log.error("MediaEngine: Invalid file or container format: " + file);
            extContainer.close();
            extContainer = null;
            return false;
        }
        int extNumStreams = extContainer.getNumStreams();
        audioStreamID = -1;
        audioCoder = null;
        for (int i = 0; i < extNumStreams; i++) {
            IStream stream = extContainer.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();
            if (audioStreamID == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamID = i;
                audioCoder = coder;
            }
        }
        if (audioStreamID == -1) {
            log.error("MediaEngine: No audio streams found in external audio!");
            extContainer.close();
            extContainer = null;
            return false;
        } else if (audioCoder.open(null, null) < 0) {
            log.error("MediaEngine: Can't open audio decoder!");
            extContainer.close();
            extContainer = null;
            return false;
        }
        audioSamples = IAudioSamples.make(getAudioSamplesSize(), audioCoder.getChannels());
        decodedAudioSamples = new FIFOByteBuffer();
        audioStreamState = new StreamState(this, audioStreamID, extContainer, sceMpeg.audioFirstTimestamp);
        audioStreamState.setTimestamps(sceMpeg.mpegTimestampPerSecond);
        log.info(String.format("Using external audio '%s'", file));
        return true;
    }

    public void finish() {
        if (container != null) {
            container.close();
            container = null;
        }
        if (videoStreamState != null) {
            videoStreamState.finish();
            videoStreamState = null;
        }
        if (audioStreamState != null) {
            audioStreamState.finish();
            audioStreamState = null;
        }
        while (!freePackets.isEmpty()) {
            IPacket packet = getPacket();
            packet.delete();
        }
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (videoConverter != null) {
            videoConverter.delete();
            videoConverter = null;
        }
        if (videoPicture != null) {
            videoPicture.delete();
            videoPicture = null;
        }
        if (audioSamples != null) {
            audioSamples.delete();
            audioSamples = null;
        }
        if (videoResampler != null) {
            videoResampler.delete();
            videoResampler = null;
        }
        if (extContainer != null) {
            extContainer.close();
            extContainer = null;
        }
        if (decodedAudioSamples != null) {
            decodedAudioSamples.delete();
            decodedAudioSamples = null;
        }
        tempBuffer = null;
    }

    /**
     * Convert the audio samples to a stereo format with signed 16-bit samples.
     * The converted audio samples are always stored in tempBuffer.
     *
     * @param samples the audio sample container
     * @param buffer  the audio sample bytes
     * @param length  the number of bytes in buffer
     * @return        the new number of bytes in tempBuffer
     */
    private int convertSamples(IAudioSamples samples, byte[] buffer, int length) {
        if (samples.getFormat() != IAudioSamples.Format.FMT_S16) {
            log.error("Unsupported audio samples format: " + samples.getFormat());
            return length;
        }
        if (samples.getChannels() == 2) {
            return length;
        }
        if (samples.getChannels() != 1) {
            log.error("Unsupported number of audio channels: " + samples.getChannels());
            return length;
        }
        int samplesSize = length * 2;
        if (tempBuffer == null || samplesSize > tempBuffer.length) {
            tempBuffer = new byte[samplesSize];
        }
        for (int i = samplesSize - 4, j = length - 2; i >= 0; i -= 4, j -= 2) {
            byte byte1 = buffer[j + 0];
            byte byte2 = buffer[j + 1];
            tempBuffer[i + 0] = byte1;
            tempBuffer[i + 1] = byte2;
            tempBuffer[i + 2] = byte1;
            tempBuffer[i + 3] = byte2;
        }
        return samplesSize;
    }

    /**
     * Add the audio samples to the decoded audio samples buffer.
     * 
     * @param samples          the samples to be added
     */
    private void updateSoundSamples(IAudioSamples samples) {
        int samplesSize = samples.getSize();
        if (tempBuffer == null || samplesSize > tempBuffer.length) {
            tempBuffer = new byte[samplesSize];
        }
        samples.get(0, tempBuffer, 0, samplesSize);
        samplesSize = convertSamples(samples, tempBuffer, samplesSize);
        decodedAudioSamples.write(tempBuffer, 0, samplesSize);
    }

    public void writeVideoImage(int dest_addr, int frameWidth, int videoPixelMode) {
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        if (getCurrentImg() != null) {
            int width = getCurrentImg().getWidth();
            int height = getCurrentImg().getHeight();
            int imageSize = height * width;
            BufferedImage image = getCurrentImg();
            if (image.getColorModel() instanceof ComponentColorModel && image.getRaster().getDataBuffer() instanceof DataBufferByte) {
                byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
                    IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
                    for (int y = 0, i = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int b = imageData[i++] & 0xFF;
                            int g = imageData[i++] & 0xFF;
                            int r = imageData[i++] & 0xFF;
                            int colorABGR = 0xFF000000 | b << 16 | g << 8 | r;
                            memoryWriter.writeNext(colorABGR);
                        }
                        memoryWriter.skip(frameWidth - width);
                    }
                    memoryWriter.flush();
                } else {
                    IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
                    for (int y = 0, i = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int b = imageData[i++] & 0xFF;
                            int g = imageData[i++] & 0xFF;
                            int r = imageData[i++] & 0xFF;
                            int colorABGR = 0xFF000000 | b << 16 | g << 8 | r;
                            int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                            memoryWriter.writeNext(pixelColor);
                        }
                        memoryWriter.skip(frameWidth - width);
                    }
                    memoryWriter.flush();
                }
            } else {
                if (videoImagePixels == null || videoImagePixels.length < imageSize) {
                    videoImagePixels = new int[imageSize];
                }
                videoImagePixels = image.getRGB(0, 0, width, height, videoImagePixels, 0, width);
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest_addr, bytesPerPixel);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int colorARGB = videoImagePixels[y * width + x];
                        int a = (colorARGB >>> 24) & 0xFF;
                        int r = (colorARGB >>> 16) & 0xFF;
                        int g = (colorARGB >>> 8) & 0xFF;
                        int b = colorARGB & 0xFF;
                        int colorABGR = a << 24 | b << 16 | g << 8 | r;
                        int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                        memoryWriter.writeNext(pixelColor);
                    }
                    memoryWriter.skip(frameWidth - width);
                }
                memoryWriter.flush();
            }
        }
    }

    public void writeVideoImageWithRange(int dest_addr, int frameWidth, int videoPixelMode, int x, int y, int w, int h) {
        if (getCurrentImg() != null) {
            if (x == 0 && y == 0 && getCurrentImg().getWidth() == w && getCurrentImg().getHeight() == h) {
                writeVideoImage(dest_addr, frameWidth, videoPixelMode);
                return;
            }
            int imageSize = h * w;
            if (videoImagePixels == null || videoImagePixels.length < imageSize) {
                videoImagePixels = new int[imageSize];
            }
            videoImagePixels = getCurrentImg().getRGB(x, y, w, h, videoImagePixels, 0, w);
            int pixelIndex = 0;
            final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
            for (int i = 0; i < h; i++) {
                int address = dest_addr + i * frameWidth * bytesPerPixel;
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, bytesPerPixel);
                for (int j = 0; j < w; j++, pixelIndex++) {
                    int colorARGB = videoImagePixels[pixelIndex];
                    int a = (colorARGB >>> 24) & 0xFF;
                    int r = (colorARGB >>> 16) & 0xFF;
                    int g = (colorARGB >>> 8) & 0xFF;
                    int b = colorARGB & 0xFF;
                    int colorABGR = a << 24 | b << 16 | g << 8 | r;
                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                    memoryWriter.writeNext(pixelColor);
                }
                memoryWriter.flush();
            }
        }
    }

    public void audioResetPlayPosition(int sample) {
        if (container != null && audioStreamID != -1) {
            if (container.seekKeyFrame(audioStreamID, sample, AVSEEK_FLAG_ANY | AVSEEK_FLAG_FRAME) < 0) {
                log.warn(String.format("Could not reset audio play position to %d", sample));
            }
        }
    }
}
