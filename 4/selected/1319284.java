package com.rbnb.media.datasink.protocol;

import com.rbnb.sapi.Source;
import com.rbnb.sapi.ChannelMap;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Vector;
import javax.media.Buffer;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.IncompatibleSourceException;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.AudioFormat;
import javax.media.format.H261Format;
import javax.media.format.H263Format;
import javax.media.format.JPEGFormat;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.SourceStream;

public class Handler implements DataSink, BufferTransferHandler {

    private long absoluteStartTime = Long.MIN_VALUE;

    private int audioChannels = 0;

    private Vector buffersWaiting[] = null;

    private String[] channels = null;

    private Format[] formats = null;

    private long framesSoFar = 0;

    private int[] lastKeyFrame = null;

    private double lastCTime = 0.;

    private double lastJMTime = 0.;

    private double lastStartTime = 0.;

    private Vector listeners = new Vector(1);

    private MediaLocator locator = null;

    private PullBufferStream[] pulling = null;

    private PullThread[] pullThreads = null;

    private PushBufferStream[] pushing = null;

    private Source rbnbSource = null;

    private Buffer readBuffer[] = null;

    private boolean registered = false;

    private double requestedRate = 0.;

    private double rolloverGuess = 4294.967296;

    private double rolloverTime = 0.;

    private DataSource source = null;

    private Integer synchLock = new Integer(0);

    private SourceStream[] unfinished = null;

    private boolean useEncoding = false;

    private String[] userData = null;

    private boolean useWallClock = true;

    private int videoChannels = 0;

    private long wallClockBegan = 0;

    public final void addDataSinkListener(DataSinkListener listenerI) {
        if (listenerI != null) {
            listeners.addElement(listenerI);
        }
    }

    public final void close() {
        try {
            stop();
            if (pullThreads != null) {
                for (int idx = 0; idx < pullThreads.length; ++idx) {
                    pullThreads[idx].terminateProcessing();
                }
            }
            if (rbnbSource != null) {
                rbnbSource.CloseRBNBConnection();
                rbnbSource = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final String getContentType() {
        return ((source == null) ? null : source.getContentType());
    }

    public final Object getControl(String controlTypeI) {
        return (null);
    }

    public final Object[] getControls() {
        return (new Object[0]);
    }

    public final MediaLocator getOutputLocator() {
        return (locator);
    }

    public final boolean getUseEncoding() {
        return (useEncoding);
    }

    public final boolean getUseWallClock() {
        return (useWallClock);
    }

    public final double getRequestedRate() {
        return (requestedRate);
    }

    public final void open() throws IOException, SecurityException {
        String remainder = locator.getRemainder(), serverAddress = null, dataPath = null, archiveMode = "none";
        int cache = 100, archive = 0;
        int idx;
        for (idx = 0; remainder.charAt(idx) == '/'; ++idx) {
        }
        remainder = remainder.substring(idx);
        int slash = remainder.indexOf("/");
        if (slash == -1) {
            serverAddress = remainder;
        } else {
            serverAddress = remainder.substring(0, slash);
            int endDP = remainder.length();
            if (remainder.charAt(remainder.length() - 1) == ')') {
                endDP = remainder.indexOf("(");
                String ringbuffer = remainder.substring(endDP + 1, remainder.length() - 1);
                int comma = ringbuffer.indexOf(","), secondComma;
                if (comma == -1) {
                    cache = Integer.parseInt(ringbuffer);
                } else {
                    if (comma > 0) {
                        cache = Integer.parseInt(ringbuffer.substring(0, comma));
                    }
                    if (comma < ringbuffer.length() - 1) {
                        secondComma = ringbuffer.indexOf(",", comma + 1);
                        if (secondComma == -1) {
                            secondComma = ringbuffer.length();
                        }
                        if (comma < secondComma) {
                            archive = Integer.parseInt(ringbuffer.substring(comma + 1, secondComma));
                        }
                        if (secondComma < ringbuffer.length() - 1) {
                            archiveMode = ringbuffer.substring(secondComma + 1);
                        }
                    }
                }
            }
            if (endDP > slash + 1) {
                dataPath = remainder.substring(slash + 1, endDP);
            }
        }
        try {
            rbnbSource = new Source(cache, archiveMode, archive);
            rbnbSource.OpenRBNBConnection(serverAddress, dataPath);
            channels = new String[unfinished.length];
            for (idx = 0; idx < channels.length; ++idx) {
                channels[idx] = null;
            }
            buffersWaiting = new Vector[unfinished.length];
        } catch (Exception e) {
            throw new SecurityException(e.getMessage());
        }
    }

    public final void removeDataSinkListener(DataSinkListener listenerI) {
        if (listenerI != null) {
            listeners.removeElement(listenerI);
        }
    }

    public final void setOutputLocator(MediaLocator locatorI) {
        locator = locatorI;
    }

    public final void setSource(DataSource sourceI) throws IncompatibleSourceException {
        if (sourceI instanceof PushBufferDataSource) {
            pushBufferDataSource((PushBufferDataSource) sourceI);
        } else if (sourceI instanceof PullBufferDataSource) {
            pullBufferDataSource((PullBufferDataSource) sourceI);
        } else {
            throw new IncompatibleSourceException();
        }
        source = sourceI;
    }

    public final void setUseEncoding(boolean useEncodingI) {
        useEncoding = useEncodingI;
    }

    public final void setUseWallClock(boolean useWallClockI) {
        useWallClock = useWallClockI;
    }

    public final void setRequestedRate(double requestedRateI) {
        requestedRate = requestedRateI;
    }

    private final void pushBufferDataSource(PushBufferDataSource sourceI) {
        pushing = sourceI.getStreams();
        unfinished = new SourceStream[pushing.length];
        readBuffer = new Buffer[pushing.length];
        formats = new Format[pushing.length];
        userData = new String[pushing.length];
        for (int idx = 0; idx < pushing.length; ++idx) {
            pushing[idx].setTransferHandler(this);
            unfinished[idx] = pushing[idx];
            readBuffer[idx] = new Buffer();
            formats[idx] = null;
        }
    }

    private final void pullBufferDataSource(PullBufferDataSource sourceI) {
        pulling = sourceI.getStreams();
        unfinished = new SourceStream[pulling.length];
        pullThreads = new PullThread[pulling.length];
        readBuffer = new Buffer[pulling.length];
        formats = new Format[pulling.length];
        userData = new String[pulling.length];
        for (int idx = 0; idx < pulling.length; ++idx) {
            pullThreads[idx] = new PullThread(pulling[idx], this);
            unfinished[idx] = pulling[idx];
            readBuffer[idx] = new Buffer();
            formats[idx] = null;
        }
    }

    public final void start() {
        try {
            source.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (pullThreads != null) {
            for (int idx = 0; idx < pullThreads.length; ++idx) {
                pullThreads[idx].startProcessing();
            }
        }
    }

    public final void stop() {
        try {
            source.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (pullThreads != null) {
            for (int idx = 0; idx < pullThreads.length; ++idx) {
                pullThreads[idx].stopProcessing();
            }
        }
    }

    public final void transferData(PushBufferStream streamI) {
        int idx;
        for (idx = 0; idx < unfinished.length; ++idx) {
            if (streamI == unfinished[idx]) {
                break;
            }
        }
        try {
            streamI.read(readBuffer[idx]);
            sendToDataTurbine(streamI, idx, readBuffer[idx]);
        } catch (Exception e) {
            e.printStackTrace();
            sendEvent(new DataSinkErrorEvent(this, e.getMessage()));
            return;
        }
        if (readBuffer[idx].isEOM() && doneWithStream(streamI)) {
            sendEvent(new EndOfStreamEvent(this));
        }
    }

    public final boolean readPullBuffer(PullBufferStream streamI) {
        int idx;
        for (idx = 0; idx < unfinished.length; ++idx) {
            if (streamI == unfinished[idx]) {
                break;
            }
        }
        try {
            streamI.read(readBuffer[idx]);
            sendToDataTurbine(streamI, idx, readBuffer[idx]);
        } catch (Exception e) {
            e.printStackTrace();
            return (true);
        }
        if (readBuffer[idx].isEOM()) {
            if (doneWithStream(streamI)) {
                close();
            }
            return (true);
        }
        return (false);
    }

    private final void sendToDataTurbine(SourceStream streamI, int idxI, Buffer bufferI) throws Exception {
        synchronized (synchLock) {
            if (bufferI.isDiscard()) {
                return;
            }
            Format format = bufferI.getFormat();
            if (format.getDataType() != Format.byteArray) {
                return;
            } else if ((formats[idxI] != null) && !format.equals(formats[idxI])) {
                throw new Exception("Unsupported format change on stream " + idxI + " from " + formats[idxI] + " to " + format);
            }
            if (absoluteStartTime == Long.MIN_VALUE) {
                absoluteStartTime = System.currentTimeMillis();
            }
            if (channels[idxI] == null) {
                buffersWaiting[idxI] = new Vector(1);
                formats[idxI] = format;
                userData[idxI] = "encoding=" + format.getEncoding();
                if (format instanceof AudioFormat) {
                    AudioFormat aFormat = (AudioFormat) format;
                    if (++audioChannels == 1) {
                        channels[idxI] = "Audio";
                    } else {
                        channels[idxI] = "Audio" + audioChannels;
                    }
                    userData[idxI] += ",content=audio" + ",channels=" + aFormat.getChannels() + ",framerate=" + aFormat.getFrameRate() + ",framesize=" + aFormat.getFrameSizeInBits() + ",samplerate=" + aFormat.getSampleRate() + ",samplesize=" + aFormat.getSampleSizeInBits() + ",endian=" + aFormat.getEndian() + ",signed=" + aFormat.getSigned() + ",startAt=" + absoluteStartTime;
                } else {
                    VideoFormat vFormat = (VideoFormat) format;
                    String cName;
                    if (++videoChannels == 1) {
                        cName = "Video";
                    } else {
                        cName = "Video" + videoChannels;
                    }
                    if (getUseEncoding()) {
                        if (format.getEncoding().equalsIgnoreCase("jpeg")) {
                            cName += ".jpg";
                        }
                    }
                    channels[idxI] = cName;
                    Dimension size = vFormat.getSize();
                    userData[idxI] += ",content=video" + ",framerate=" + vFormat.getFrameRate() + ",maxlength=" + vFormat.getMaxDataLength() + ",height=" + ((int) size.getHeight()) + ",width=" + ((int) size.getWidth());
                    if (vFormat instanceof H261Format) {
                        H261Format h261Format = (H261Format) vFormat;
                        userData[idxI] += ",stillimage=" + h261Format.getStillImageTransmission();
                    } else if (vFormat instanceof H263Format) {
                        H263Format h263Format = (H263Format) vFormat;
                        userData[idxI] += ",advancedprediction=" + h263Format.getAdvancedPrediction() + ",arithmeticcoding=" + h263Format.getArithmeticCoding() + ",errorcompensation=" + h263Format.getErrorCompensation() + ",hrdb=" + h263Format.getHrDB() + ",pbframes=" + h263Format.getPBFrames() + ",unrestrictedvector=" + h263Format.getUnrestrictedVector();
                    } else if (vFormat instanceof JPEGFormat) {
                        JPEGFormat jpegFormat = (JPEGFormat) vFormat;
                        userData[idxI] += ",decimation=" + jpegFormat.getDecimation() + ",qfactor=" + jpegFormat.getQFactor();
                    } else if (vFormat instanceof RGBFormat) {
                        RGBFormat rgbFormat = (RGBFormat) vFormat;
                        userData[idxI] += ",bpp=" + rgbFormat.getBitsPerPixel() + ",blue=" + rgbFormat.getBlueMask() + ",endian=" + rgbFormat.getEndian() + ",flipped=" + rgbFormat.getFlipped() + ",green=" + rgbFormat.getGreenMask() + ",line=" + rgbFormat.getLineStride() + ",pixel=" + rgbFormat.getPixelStride() + ",red=" + rgbFormat.getRedMask();
                    }
                    userData[idxI] += ",startAt=" + absoluteStartTime;
                }
                int count = 0;
                for (int idx1 = 0; idx1 < channels.length; ++idx1) {
                    count += (channels[idx1] != null) ? 1 : 0;
                }
                if (count == channels.length) {
                    ChannelMap channelMap = new ChannelMap();
                    channelMap.PutTime(0., 0.);
                    for (int idx1 = 0; idx1 < channels.length; ++idx1) {
                        channelMap.Add(channels[idx1]);
                        channelMap.PutDataAsString(idx1, userData[idx1]);
                    }
                    rbnbSource.Register(channelMap);
                }
                lastKeyFrame = new int[channels.length];
            }
            if (bufferI.getLength() > 0) {
                Buffer waitBuffer = new Buffer();
                waitBuffer.copy(bufferI);
                byte[] original = (byte[]) waitBuffer.getData(), copied = new byte[original.length];
                System.arraycopy(original, 0, copied, 0, original.length);
                waitBuffer.setData(copied);
                buffersWaiting[idxI].addElement(waitBuffer);
                sendReadyToDataTurbine();
            }
        }
    }

    private final boolean sendReadyToDataTurbine() throws Exception {
        boolean readyToSend = true;
        synchronized (synchLock) {
            boolean ready[] = new boolean[channels.length];
            Buffer readyBuffer = null;
            for (int idx = 0; idx < channels.length; ++idx) {
                if (channels[idx] == null) {
                    readyToSend = false;
                    break;
                } else if (((buffersWaiting[idx] == null) || buffersWaiting[idx].isEmpty()) && (unfinished[idx] != null)) {
                    readyToSend = false;
                    break;
                } else if (!buffersWaiting[idx].isEmpty()) {
                    Buffer buffer = (Buffer) buffersWaiting[idx].firstElement();
                    if (readyBuffer == null) {
                        ready[idx] = true;
                        readyBuffer = buffer;
                    } else if (buffer.getTimeStamp() == readyBuffer.getTimeStamp()) {
                        ready[idx] = true;
                    } else if (buffer.getTimeStamp() < readyBuffer.getTimeStamp()) {
                        for (int idx1 = 0; idx1 < idx; ++idx1) {
                            ready[idx1] = false;
                        }
                        ready[idx] = true;
                        readyBuffer = buffer;
                    }
                }
            }
            readyToSend = readyToSend && (readyBuffer != null);
            if (readyToSend) {
                ChannelMap map = new ChannelMap();
                byte[][] data = new byte[channels.length][];
                double jmTime = -Double.MAX_VALUE;
                for (int idx = 0; idx < channels.length; ++idx) {
                    if (ready[idx]) {
                        Buffer buffer = (Buffer) buffersWaiting[idx].firstElement();
                        data[idx] = new byte[buffer.getLength()];
                        if (buffer.getFormat() instanceof AudioFormat) {
                            lastKeyFrame[idx] = 0;
                        } else if ((buffer.getFlags() & Buffer.FLAG_KEY_FRAME) == 0) {
                            ++lastKeyFrame[idx];
                        } else {
                            lastKeyFrame[idx] = 0;
                        }
                        jmTime = buffer.getTimeStamp() * Math.pow(10., -9);
                        buffersWaiting[idx].removeElementAt(0);
                        System.arraycopy(buffer.getData(), buffer.getOffset(), data[idx], 0, buffer.getLength());
                    }
                }
                double startTime = jmTime + rolloverTime;
                while (startTime < lastStartTime) {
                    if (lastJMTime > rolloverGuess) {
                        rolloverGuess = lastJMTime;
                    }
                    rolloverTime += rolloverGuess;
                    startTime = jmTime + rolloverTime;
                }
                lastJMTime = jmTime;
                long now = System.currentTimeMillis();
                if ((wallClockBegan == 0) || (getRequestedRate() == 0.) || ((now - wallClockBegan) / 1000. * getRequestedRate() > framesSoFar)) {
                    if (wallClockBegan == 0) {
                        wallClockBegan = now;
                    } else {
                        ++framesSoFar;
                    }
                    double currentTime;
                    if (useWallClock) {
                        currentTime = System.currentTimeMillis() / 1000.;
                    } else {
                        currentTime = (absoluteStartTime / 1000. + lastStartTime);
                    }
                    if (lastCTime == 0.) {
                        map.PutTime(currentTime, 0.);
                    } else {
                        map.PutTime(lastCTime, currentTime - lastCTime);
                    }
                    lastCTime = currentTime;
                    lastStartTime = startTime;
                    for (int idx = 0; idx < channels.length; ++idx) {
                        if (data[idx] != null) {
                            map.Add(channels[idx]);
                            map.PutDataAsByteArray(idx, data[idx]);
                        }
                    }
                    rbnbSource.Flush(map);
                }
            }
        }
        return (readyToSend);
    }

    private final boolean doneWithStream(SourceStream streamI) {
        boolean allDoneR = true;
        synchronized (synchLock) {
            for (int idx = 0; idx < unfinished.length; ++idx) {
                if (unfinished[idx] == streamI) {
                    unfinished[idx] = null;
                } else if (unfinished[idx] != null) {
                    allDoneR = false;
                }
            }
            if (allDoneR) {
                try {
                    while (sendReadyToDataTurbine()) {
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return (allDoneR);
    }

    protected final void sendEvent(DataSinkEvent eventI) {
        if (!listeners.isEmpty()) {
            synchronized (listeners) {
                for (int idx = 0; idx < listeners.size(); ++idx) {
                    DataSinkListener listener = (DataSinkListener) listeners.elementAt(idx);
                    listener.dataSinkUpdate(eventI);
                }
            }
        }
    }

    private class PullThread extends Thread {

        private Handler parent = null;

        private PullBufferStream stream = null;

        private boolean stop = true, terminate = false;

        PullThread(PullBufferStream streamI, Handler parentI) {
            stream = streamI;
            parent = parentI;
            this.start();
        }

        public final void run() {
            while (!terminate) {
                try {
                    while (stop && !terminate) {
                        wait();
                    }
                } catch (InterruptedException e) {
                }
                if (!terminate) {
                    if (parent.readPullBuffer(stream)) {
                        stopProcessing();
                    }
                }
            }
        }

        final synchronized void startProcessing() {
            stop = false;
            notify();
        }

        final synchronized void stopProcessing() {
            stop = true;
            notify();
        }

        final synchronized void terminateProcessing() {
            terminate = true;
            notify();
        }
    }
}
