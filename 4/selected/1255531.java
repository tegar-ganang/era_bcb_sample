package soundengine;

import graph.GraphEvaluatorFactory;
import graph.IGraphEvaluator;
import gui.RecordedSoundProperties;
import gui.Track0Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;
import preferences.Preference;
import common.Dialogs;
import common.Utils;
import database.DbGraph;
import database.GraphParams;
import database.DbGraph.ILoadGraphResults;

public class RecordedSoundPeerImpl {

    private final RecordedSoundProperties m_properties;

    private GraphParams m_amplitudeGraphParams;

    private final RecordedSoundPeerDouble m_peer;

    private int m_calculationFrequency;

    private double m_calculationIntervalSecs;

    public RecordedSoundPeerImpl(RecordedSoundProperties properties, RecordedSoundPeerDouble peer) {
        m_properties = properties;
        m_peer = peer;
        m_calculationFrequency = Track0Properties.getFrequency().getVal();
        m_calculationIntervalSecs = 1.0d / m_calculationFrequency;
    }

    /**
     * @param amplitudeGraphId
     * @throws Exception
     */
    void loadAmplitudeGraph(int amplitudeGraphId) throws Exception {
        if (m_amplitudeGraphParams == null || m_amplitudeGraphParams.getGraphId() != amplitudeGraphId) DbGraph.loadSynchronously(null, amplitudeGraphId, new ILoadGraphResults() {

            public void setResults(GraphParams params) {
                m_amplitudeGraphParams = params;
            }
        });
    }

    Number getChildrenDuration() {
        return null;
    }

    Number getOwnDuration() {
        return m_properties.getDuration();
    }

    /**
     * @return Returns the volumeGraphParams.
     */
    GraphParams getAmplitudeGraphParams() {
        return m_amplitudeGraphParams;
    }

    public double[] getIntermediateBuf() throws Exception {
        return m_properties.getIntermediateBuf();
    }

    public boolean computeIntermediateFile(Number startTimeOffset, IComputeSound computeSound) throws Exception {
        String rawSoundFileName = m_properties.getRawSoundFile();
        if (Utils.isBlank(rawSoundFileName)) return false;
        File rawSoundFile = new File(rawSoundFileName);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(rawSoundFile);
        } catch (FileNotFoundException e) {
            Dialogs.showNoWayDialog("Unable to find raw sound file");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        FileChannel in = fin.getChannel();
        try {
            int chunkSize = readRiffChunk(in);
            FormatChunkInfo info = readFormatChunk(in);
            int dataChunkSize = readDataChunkHeader(in);
            if (dataChunkSize == 0) {
                return false;
            }
            int numSamples = dataChunkSize / info.m_blockAlign;
            int padByte = 0;
            if (dataChunkSize % 2 != 0) {
                padByte = 1;
            }
            int dataChunkSamplesSize = dataChunkSize + padByte;
            int configBufSize = Preference.getSoundFileBufferSize();
            int bufSize = configBufSize == 0 ? dataChunkSamplesSize : Math.min(configBufSize, dataChunkSamplesSize);
            int targetSampleRate = Preference.getSampleRate();
            double targetSampleInc = 1.0d / targetSampleRate;
            int targetStartOfs = (int) (startTimeOffset.doubleValue() * targetSampleRate);
            int sourceSampleRate = info.m_sampleRate;
            int samplesToSkip = (int) (m_properties.getStartOfs() * sourceSampleRate);
            int sourceStartOfs = samplesToSkip * info.m_blockAlign;
            boolean equalSampleRate = sourceSampleRate == targetSampleRate;
            boolean useLeftChan = m_properties.getLeftChanSelected();
            int numChannels = info.m_numChannels;
            SampleInfo sampleInfo = null;
            int samplesToProcess = numSamples - samplesToSkip;
            double duration = m_peer.getDuration().doubleValue();
            int maxSamples = (int) (duration * sourceSampleRate);
            if (samplesToProcess > maxSamples) {
                samplesToProcess = maxSamples;
            }
            if (samplesToProcess <= 0) {
                return false;
            }
            double curTargetTime = targetSampleInc;
            double prevSourceTime = 0;
            double prevSourceSampleVal = 0;
            double multExtent = m_properties.getAmplitudeGraphExtentMult();
            int amplitudeGraphId = m_properties.getAmplitudeGraphId();
            IGraphEvaluator graphEval = null;
            if (amplitudeGraphId > 0) {
                loadAmplitudeGraph(amplitudeGraphId);
                graphEval = GraphEvaluatorFactory.newInstance(m_amplitudeGraphParams, duration, multExtent, null);
            }
            double graphEvalRes = 1.0d;
            int samplesInCalculationInterval = (int) (m_calculationIntervalSecs * sourceSampleRate);
            int sampleCountDown = 1;
            double[] sampleArr = computeSound.getSampleArr();
            int targetNdx = 0;
            SampleIter sampleIter = new SampleIter(bufSize, info.m_bitsPerSample, numChannels, useLeftChan, sourceSampleRate, samplesToProcess, in, info.m_blockAlign, sourceStartOfs);
            for (; sampleIter.hasNext(); ) {
                sampleInfo = sampleIter.next();
                double sampleVal = sampleInfo.m_sampleVal;
                if (graphEval == null) {
                    sampleVal *= multExtent;
                } else {
                    sampleCountDown--;
                    if (sampleCountDown == 0) {
                        sampleCountDown = samplesInCalculationInterval;
                        graphEvalRes = graphEval.evaluate(sampleInfo.m_sampleTime).doubleValue();
                    }
                    sampleVal *= graphEvalRes;
                }
                if (equalSampleRate) {
                    sampleArr[targetStartOfs + targetNdx] += sampleVal;
                    targetNdx++;
                } else {
                    double curSourceTime = sampleInfo.m_sampleTime;
                    double curSourceSampleVal = sampleVal;
                    while (curSourceTime >= curTargetTime) {
                        double targetSampleVal;
                        if (curSourceTime == curTargetTime) {
                            targetSampleVal = curSourceSampleVal;
                        } else {
                            targetSampleVal = prevSourceSampleVal + ((curSourceSampleVal - prevSourceSampleVal) * (curTargetTime - prevSourceTime)) / (curSourceTime - prevSourceTime);
                        }
                        sampleArr[targetStartOfs + targetNdx] += targetSampleVal;
                        curTargetTime += targetSampleInc;
                        targetNdx++;
                    }
                    prevSourceTime = curSourceTime;
                    prevSourceSampleVal = curSourceSampleVal;
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } finally {
            in.close();
            fin.close();
        }
        return true;
    }

    private static class SampleIter implements Iterator<SampleInfo> {

        private int m_bytesPerSample;

        private final int m_bitsPerSample;

        private final int m_numChannels;

        private final boolean m_useLeftChan;

        private int m_samplesRemaining;

        private final FileChannel m_in;

        private ByteBuffer m_dataChunk;

        private boolean m_readReqd = true;

        private double m_sampleTimeInc;

        private double m_accumTime;

        private final short m_blockAlign;

        private int m_maxAmplitude;

        private int m_bitsPerSampleRem;

        private int m_bitsToShift;

        SampleIter(int bufSize, int bitsPerSample, int numChannels, boolean useLeftChan, int sourceSampleRate, int numSamples, FileChannel in, short blockAlign, int sourceStartOfs) throws IOException {
            m_bitsPerSample = bitsPerSample;
            m_numChannels = numChannels;
            m_useLeftChan = useLeftChan;
            m_in = in;
            if (sourceStartOfs > 0) {
                m_in.position(m_in.position() + sourceStartOfs);
            }
            m_blockAlign = blockAlign;
            m_samplesRemaining = numSamples;
            m_bytesPerSample = (int) Math.ceil(bitsPerSample / 8);
            if (bufSize < blockAlign) {
                bufSize = blockAlign;
            }
            m_dataChunk = ByteBuffer.allocate(bufSize);
            m_dataChunk.order(ByteOrder.LITTLE_ENDIAN);
            m_sampleTimeInc = 1.0d / sourceSampleRate;
            m_maxAmplitude = (int) (Math.pow(2, m_bitsPerSample - 1) - 1);
            m_bitsPerSampleRem = m_bitsPerSample % 8;
            m_bitsToShift = (8 - m_bitsPerSampleRem) % 8;
        }

        public boolean hasNext() {
            return m_samplesRemaining > 0;
        }

        public SampleInfo next() {
            if (m_samplesRemaining == 0) {
                throw new NoSuchElementException("No more samples to read");
            }
            if (m_readReqd) {
                try {
                    if (m_in.read(m_dataChunk) == -1) {
                        throw new IllegalStateException("Problem reading .WAV file: Unexpected EOF");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                m_readReqd = false;
                m_dataChunk.flip();
            }
            SampleInfo info = new SampleInfo();
            if (!m_useLeftChan && m_numChannels == 2) {
                skipSample();
            }
            switch(m_bytesPerSample) {
                case 1:
                    {
                        byte sampleVal = m_dataChunk.get();
                        info.m_sampleVal = sampleVal - m_maxAmplitude;
                        break;
                    }
                case 2:
                    {
                        short sampleVal = m_dataChunk.getShort();
                        if (m_bitsPerSampleRem != 0) sampleVal >>= m_bitsToShift;
                        info.m_sampleVal = sampleVal;
                        break;
                    }
                case 3:
                    {
                        byte loByte = m_dataChunk.get();
                        byte medByte = m_dataChunk.get();
                        byte hiByte = m_dataChunk.get();
                        int sampleVal = loByte + (medByte << 8) + (hiByte << 16);
                        if (m_bitsPerSampleRem != 0) sampleVal >>= m_bitsToShift;
                        info.m_sampleVal = sampleVal;
                        break;
                    }
                case 4:
                    {
                        int sampleVal = m_dataChunk.getInt();
                        if (m_bitsPerSampleRem != 0) sampleVal >>= m_bitsToShift;
                        info.m_sampleVal = sampleVal;
                        break;
                    }
                default:
                    throw new IllegalStateException("Unexpected bytes per sample: " + m_bytesPerSample);
            }
            if (m_useLeftChan && m_numChannels == 2) {
                skipSample();
            }
            if (m_dataChunk.remaining() < m_blockAlign) m_readReqd = true;
            if (m_readReqd) {
                m_dataChunk.compact();
            }
            m_samplesRemaining--;
            m_accumTime += m_sampleTimeInc;
            info.m_sampleTime = m_accumTime;
            return info;
        }

        private final void skipSample() {
            m_dataChunk.position(m_dataChunk.position() + m_bytesPerSample);
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    private static class SampleInfo {

        public int m_sampleVal;

        public double m_sampleTime;
    }

    public static int readDataChunkHeader(FileChannel in) throws Exception {
        ByteBuffer dataChunkHeader = ByteBuffer.allocate(WavSoundFile.DATA_CHUNK_HEADER_SIZE);
        int bytesRead = in.read(dataChunkHeader);
        if (bytesRead != WavSoundFile.DATA_CHUNK_HEADER_SIZE) {
            String msg = "Unrecognised format of raw sound file: data chunk header size = " + bytesRead;
            error(msg);
        }
        dataChunkHeader.flip();
        dataChunkHeader.order(ByteOrder.BIG_ENDIAN);
        byte b = dataChunkHeader.get();
        if (b != 'd') dataChunkError(b, 'd', 1);
        b = dataChunkHeader.get();
        if (b != 'a') dataChunkError(b, 'a', 2);
        b = dataChunkHeader.get();
        if (b != 't') dataChunkError(b, 't', 3);
        b = dataChunkHeader.get();
        if (b != 'a') dataChunkError(b, 'a', 4);
        dataChunkHeader.order(ByteOrder.LITTLE_ENDIAN);
        int dataChunkSize = dataChunkHeader.getInt();
        return dataChunkSize;
    }

    public static class FormatChunkInfo {

        public short m_numChannels;

        public int m_sampleRate;

        public int m_avgBytesPerSec;

        public short m_blockAlign;

        public short m_bitsPerSample;
    }

    public static FormatChunkInfo readFormatChunk(FileChannel in) throws Exception {
        FormatChunkInfo info = new FormatChunkInfo();
        ByteBuffer formatChunk = ByteBuffer.allocate(WavSoundFile.FORMAT_CHUNK_SIZE);
        int bytesRead = in.read(formatChunk);
        if (bytesRead != WavSoundFile.FORMAT_CHUNK_SIZE) {
            String msg = "Unrecognised format of raw sound file: format chunk size = " + bytesRead;
            error(msg);
        }
        formatChunk.flip();
        formatChunk.order(ByteOrder.BIG_ENDIAN);
        byte b = formatChunk.get();
        if (b != 'f') formatChunkError(b, 'f', 1);
        b = formatChunk.get();
        if (b != 'm') formatChunkError(b, 'm', 2);
        b = formatChunk.get();
        if (b != 't') formatChunkError(b, 't', 3);
        b = formatChunk.get();
        if (b != ' ') formatChunkError(b, ' ', 4);
        formatChunk.order(ByteOrder.LITTLE_ENDIAN);
        int restOfFormatChunkBytes = formatChunk.getInt();
        if (restOfFormatChunkBytes != WavSoundFile.REST_OF_FORMAT_CHUNK_BYTES && restOfFormatChunkBytes != WavSoundFile.REST_OF_FORMAT_CHUNK_BYTES + 2) {
            String msg = "Unrecognised format of raw sound file: Rest of format chunk bytes = " + restOfFormatChunkBytes;
            error(msg);
        }
        short audioFormat = formatChunk.getShort();
        if (audioFormat != WavSoundFile.AUDIO_FORMAT) {
            String msg = "Only uncompressed .WAV file formats are currently handled " + " audioFormat detected = " + audioFormat;
            error(msg);
        }
        short numChannels = info.m_numChannels = formatChunk.getShort();
        if (numChannels < 1 || numChannels > 2) {
            String msg = "Only 1 or 2 channels are currently handled. Channels = " + numChannels;
            error(msg);
        }
        info.m_sampleRate = formatChunk.getInt();
        info.m_avgBytesPerSec = formatChunk.getInt();
        info.m_blockAlign = formatChunk.getShort();
        info.m_bitsPerSample = formatChunk.getShort();
        if (restOfFormatChunkBytes == WavSoundFile.REST_OF_FORMAT_CHUNK_BYTES + 2) {
            in.position(in.position() + 2);
        }
        return info;
    }

    private static void error(String msg) {
        Dialogs.showNoWayDialog(msg);
        throw new IllegalStateException(msg);
    }

    public static int readRiffChunk(FileChannel in) throws Exception {
        ByteBuffer riffChunk = ByteBuffer.allocate(WavSoundFile.RIFF_CHUNK_SIZE);
        int bytesRead = in.read(riffChunk);
        if (bytesRead != WavSoundFile.RIFF_CHUNK_SIZE) {
            String msg = "Unrecognised format of raw sound file: riff chunk size = " + bytesRead;
            error(msg);
        }
        riffChunk.flip();
        riffChunk.order(ByteOrder.BIG_ENDIAN);
        byte b = riffChunk.get();
        if (b != 'R') riffChunkError(b, 'R', 1);
        b = riffChunk.get();
        if (b != 'I') riffChunkError(b, 'I', 2);
        b = riffChunk.get();
        if (b != 'F') riffChunkError(b, 'F', 3);
        b = riffChunk.get();
        if (b != 'F') riffChunkError(b, 'F', 4);
        riffChunk.order(ByteOrder.LITTLE_ENDIAN);
        int chunkSize = riffChunk.getInt();
        riffChunk.order(ByteOrder.BIG_ENDIAN);
        b = riffChunk.get();
        if (b != 'W') riffChunkError(b, 'W', 5);
        b = riffChunk.get();
        if (b != 'A') riffChunkError(b, 'A', 6);
        b = riffChunk.get();
        if (b != 'V') riffChunkError(b, 'V', 7);
        b = riffChunk.get();
        if (b != 'E') riffChunkError(b, 'E', 8);
        return chunkSize;
    }

    private static void riffChunkError(byte b, char c, int i) throws Exception {
        wavFileError(b, c, i, "riff");
    }

    private static void formatChunkError(byte b, char c, int i) throws Exception {
        wavFileError(b, c, i, "format");
    }

    private static void dataChunkError(byte b, char c, int i) throws Exception {
        wavFileError(b, c, i, "data");
    }

    private static void wavFileError(byte b, char c, int i, String sectionName) {
        String msg = "Unrecognised format of raw sound file: " + sectionName + " byte at index " + i + " = '" + (char) b + "' expected = '" + c + "'";
        error(msg);
    }
}
