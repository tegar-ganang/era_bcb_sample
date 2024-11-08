package com.safi.workshop.audio.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.tritonus.share.sampled.FloatSampleBuffer;
import com.safi.workshop.audio.utils.AudioUtils.LineAndStream;
import com.safi.workshop.audio.utils.MyByteArrayOutputStream.NewDataListener;
import com.safi.workshop.preferences.AudioDevicesPrefPage;

public class RecordableClip {

    private static final int RECORD_BUFFER_SIZE = 16777216;

    private static final int MAX_QUEUE_SIZE = 20;

    private static final int DEFAULT_PROMPT_MINUTES_LEN = 2;

    private File currentFile;

    private final AudioFormat audioFormat;

    private int streamLength;

    private ByteArray streamData;

    private List<ClipListener> clipListeners = new ArrayList<ClipListener>();

    private DataLine player;

    private int start = -1;

    private int end = -1;

    private LinkedList<UndoableCommand> undoQueue = new LinkedList<UndoableCommand>();

    private LinkedList<UndoableCommand> redoQueue = new LinkedList<UndoableCommand>();

    /**
   * Used for recording audio.
   */
    private RecordThread recordThread;

    private AudioPositionTracker audioPositionTracker;

    private float currentVolume = 0.5f;

    private FloatSampleBuffer floatBuffer;

    public static int bytesToInt(byte[] intBytes, int offset, int length) {
        ByteBuffer bb = ByteBuffer.wrap(intBytes, offset, length);
        bb.order(AudioCommon.SYSTEM_BYTE_ORDER);
        return bb.getShort();
    }

    public RecordableClip(File file, AudioFormat format) throws IOException, AudioException {
        if (file == null) currentFile = File.createTempFile("audioFile", ".wav"); else currentFile = file;
        if (!currentFile.exists()) {
            IOUtils.create(currentFile);
        }
        int len = (int) Math.ceil(format.getFrameSize() * format.getFrameRate() * DEFAULT_PROMPT_MINUTES_LEN);
        streamData = new ByteArray(len);
        streamLength = IOUtils.load(currentFile, streamData);
        audioFormat = format;
        LineAndStream recordingStream = AudioUtils.getRecordingStream(audioFormat);
        recordThread = new RecordThread(recordingStream);
    }

    public RecordableClip(RecordableClip copy) {
        this.currentFile = copy.currentFile;
        this.streamData = new ByteArray(copy.streamData);
        this.streamLength = copy.streamLength;
        this.audioFormat = copy.audioFormat;
        if (copy.player != null && copy.player.isOpen()) copy.player.close();
    }

    private AudioFormat getPlaybackFormat() {
        if (player != null) return player.getFormat(); else return audioFormat;
    }

    public void dispose() {
        if (player != null) {
            for (ClipListener l : clipListeners) {
                l.playerDisposed(player);
            }
            try {
                player.stop();
                player.drain();
                player.close();
            } catch (Exception e) {
            }
        }
        if (recordThread != null) recordThread.stop();
        stopAudioTracker();
        streamData = null;
    }

    public float getVolume() {
        return currentVolume;
    }

    private void initAudioTracker() {
        stopAudioTracker();
        audioPositionTracker = new AudioPositionTracker();
        new Thread(audioPositionTracker).start();
    }

    private void stopAudioTracker() {
        if (audioPositionTracker != null) audioPositionTracker.stop();
        audioPositionTracker = null;
    }

    public void addClipListener(ClipListener cl) {
        clipListeners.add(cl);
    }

    public void setPositionInSamples(int samples) {
        if (player != null) {
            ((Clip) player).setFramePosition((int) (samples / (double) audioFormat.getFrameSize()));
        }
    }

    public int getSample(long idx) {
        long byteStartIdx = idx * getFrameSize();
        return bytesToInt(streamData.get(), (int) byteStartIdx, audioFormat.getFrameSize());
    }

    public int getLastSample() {
        int lengthInSamples = getLengthInSamples();
        if (lengthInSamples == 0) return 0;
        return getSample(lengthInSamples - 1);
    }

    public int getSampleIdxByOffset(int offset) {
        return (int) (offset / (float) getFrameSize());
    }

    public float getFrameRate() {
        return audioFormat.getFrameRate();
    }

    public int getFrameSize() {
        return audioFormat.getFrameSize();
    }

    public int getSampleByOffset(int offset, int length) {
        return bytesToInt(streamData.get(), offset, length);
    }

    public int getLengthInBytes() {
        return streamLength;
    }

    public boolean isSaveable() {
        return streamData != null && streamLength > 0 && !isRecording();
    }

    private ByteArrayInputStream makeInputStream() {
        return new ByteArrayInputStream(streamData.get(), 0, streamLength);
    }

    private AudioInputStream makeStream() {
        return new AudioInputStream(makeInputStream(), audioFormat, getLengthInSamples());
    }

    public void setSelection(int start, int end) {
        if (start >= 0 && start < getLengthInSamples() && end >= 0 && end <= getLengthInSamples()) {
            try {
                stop();
                if (player != null) player.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            player = null;
            this.start = start;
            this.end = end;
        }
    }

    public byte[] getSelection(int start, int end) {
        byte[] buffer = new byte[(end - start) * audioFormat.getFrameSize()];
        System.arraycopy(streamData.get(), start * audioFormat.getFrameSize(), buffer, 0, buffer.length);
        return buffer;
    }

    public void cut(int start, int end) {
        Cut cut = new Cut();
        cut.start = start;
        cut.end = end;
        cut.execute();
        undoQueue.add(cut);
        if (undoQueue.size() > MAX_QUEUE_SIZE) undoQueue.removeFirst();
    }

    public void mute(int start, int end) {
        Mute mute = new Mute();
        mute.start = start;
        mute.end = end;
        mute.execute();
        undoQueue.add(mute);
        if (undoQueue.size() > MAX_QUEUE_SIZE) undoQueue.removeFirst();
    }

    public void paste(int start, byte[] data) {
        Paste paste = new Paste();
        paste.start = start;
        paste.data = data;
        paste.execute();
        undoQueue.add(paste);
        if (undoQueue.size() > MAX_QUEUE_SIZE) undoQueue.removeFirst();
    }

    public void changeAmplitude(int start, int end, float delta) {
        ChangeAmplitude cmd = new ChangeAmplitude();
        cmd.start = start;
        cmd.end = end;
        cmd.multiplier = delta;
        cmd.execute();
        undoQueue.add(cmd);
        if (undoQueue.size() > MAX_QUEUE_SIZE) undoQueue.removeFirst();
    }

    public void undo() {
        if (undoQueue.isEmpty()) return;
        UndoableCommand cmd = undoQueue.removeLast();
        cmd.undo();
        redoQueue.add(cmd);
    }

    public void redo() {
        if (redoQueue.isEmpty()) return;
        UndoableCommand cmd = redoQueue.removeLast();
        cmd.redo();
        undoQueue.addLast(cmd);
    }

    public byte[] getAudioData() {
        return streamData.get();
    }

    public void setAudioData(byte[] data) {
        try {
            stop();
            if (player != null) player.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        streamData = new ByteArray(data.length);
        streamData.add(data, data.length);
        streamLength = data.length;
        clearSelection();
    }

    public void deleteSelection(int start, int end) {
        try {
            stop();
            if (player != null) player.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int len = getLengthInSamples();
        byte[] newData = new byte[(len - (end - start)) * audioFormat.getFrameSize()];
        byte[] currdata = streamData.get();
        int idx = 0;
        if (start > 0) {
            idx = start * audioFormat.getFrameSize();
            System.arraycopy(currdata, 0, newData, 0, idx);
        }
        if (end < len - 1) {
            System.arraycopy(currdata, end * audioFormat.getFrameSize(), newData, idx, (len - end) * audioFormat.getFrameSize());
        }
        streamData = new ByteArray(newData);
        streamLength = newData.length;
        clearSelection();
    }

    private void insertSelection(int start, byte[] buffer) {
        try {
            stop();
            if (player != null) player.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] newData = new byte[streamLength + buffer.length];
        byte[] currentData = streamData.get();
        int startIdx = 0;
        if (start > 0) {
            startIdx = start * audioFormat.getFrameSize();
            System.arraycopy(currentData, 0, newData, 0, startIdx);
        }
        System.arraycopy(buffer, 0, newData, startIdx, buffer.length);
        if (start < getLengthInSamples()) {
            System.arraycopy(currentData, start * audioFormat.getFrameSize(), newData, startIdx + buffer.length, getLengthInBytes() - startIdx);
        }
        streamData = new ByteArray(newData);
        streamLength = newData.length;
    }

    private void muteSelection(int start, int end) {
        for (int i = start * audioFormat.getFrameSize(); i < getLengthInBytes() && i < end * audioFormat.getFrameSize(); i++) {
            streamData.get()[i] = 0;
        }
    }

    private void changeAmplitude(int start, int end, float delta, boolean divide) {
        float change = 1.0f + delta;
        if (floatBuffer == null) floatBuffer = new FloatSampleBuffer();
        int byteStart = start * audioFormat.getFrameSize();
        int byteEnd = end * audioFormat.getFrameSize();
        floatBuffer.initFromByteArray(streamData.get(), byteStart, byteEnd - byteStart, audioFormat);
        for (int nChannel = 0; nChannel < floatBuffer.getChannelCount(); nChannel++) {
            float afBuffer[] = floatBuffer.getChannel(nChannel);
            for (int nSample = 0; nSample < floatBuffer.getSampleCount(); nSample++) {
                if (divide) afBuffer[nSample] /= change; else afBuffer[nSample] *= change;
            }
        }
        floatBuffer.convertToByteArray(streamData.get(), byteStart, audioFormat);
    }

    public void writeSelection(int start, byte[] data) {
        streamData.addToPos(data, start * audioFormat.getFrameSize());
    }

    public void clearSelection() {
        start = end = -1;
        try {
            stop();
            if (player != null) player.close();
        } catch (AudioException e) {
            e.printStackTrace();
        }
        player = null;
    }

    public void start() throws AudioException {
        if (player == null) {
            try {
                Line.Info clipInfo = new Line.Info(Clip.class);
                Mixer outputMixer = null;
                try {
                    outputMixer = AudioDevicesPrefPage.getOutputMixer();
                } catch (Exception exe) {
                    for (Mixer.Info mInfo : AudioSystem.getMixerInfo()) {
                        Mixer m = AudioSystem.getMixer(mInfo);
                        if (m.isLineSupported(clipInfo)) {
                            outputMixer = m;
                            break;
                        }
                    }
                }
                if (outputMixer == null) {
                    throw new AudioException("No input device found");
                }
                player = (Clip) outputMixer.getLine(clipInfo);
                player.addLineListener(new LineListener() {

                    @Override
                    public void update(LineEvent event) {
                        if (event.getType() == LineEvent.Type.STOP) {
                            for (ClipListener l : clipListeners) {
                                l.playerStopped(player);
                            }
                        }
                    }
                });
                if (end != -1 && start != -1 && start <= end) {
                    if (end <= start) end = getLengthInSamples();
                    end = Math.min(end, getLengthInSamples());
                    start = Math.max(start, 0);
                    int bufLen = end * audioFormat.getFrameSize();
                    ((Clip) player).open(audioFormat, streamData.get(), 0, bufLen);
                    ((Clip) player).setFramePosition(start);
                } else {
                    AudioInputStream stream = makeStream();
                    ((Clip) player).open(stream);
                }
                for (ClipListener l : clipListeners) {
                    l.playerCreated(player);
                }
            } catch (Exception lue) {
                throw new AudioException(lue);
            }
        }
        if (audioPositionTracker == null) initAudioTracker();
        player.start();
    }

    private void printshit() {
        for (Mixer.Info mInfo : AudioSystem.getMixerInfo()) {
            Mixer m = AudioSystem.getMixer(mInfo);
            for (Line.Info lInfo : m.getTargetLineInfo()) {
                try {
                    if (lInfo.getLineClass().equals(TargetDataLine.class)) {
                        DataLine.Info dInfo = (DataLine.Info) lInfo;
                        for (AudioFormat f : dInfo.getFormats()) {
                            System.out.println("This shit ibe " + f);
                        }
                    }
                } catch (Exception lue) {
                }
            }
        }
    }

    public boolean isPlaying() {
        return player != null && player.isActive() && player.isRunning();
    }

    /**
   * Return the index (in samples) of the play head.
   */
    public long getPlayHead() {
        if (player == null) return 0;
        return player.getLongFramePosition();
    }

    public void stop() throws AudioException {
        this.stop(true);
    }

    /**
   * Stops recording if we're recording. Stops playing if we're playing.
   * 
   * @throws AudioException
   */
    public void stop(boolean flush) throws AudioException {
        if (player != null) {
            player.stop();
            if (flush) {
                player.flush();
                ((Clip) player).close();
                player = null;
            }
        }
        if (recordThread != null) recordThread.stop();
        stopAudioTracker();
    }

    /**
   * Move the playhead to the beginning of the clip.
   * 
   * @throws AudioException
   */
    public void reset() throws AudioException {
        if (player != null) {
            ((Clip) player).setMicrosecondPosition(0);
            for (ClipListener l : clipListeners) {
                l.newPlayHead(0);
            }
        }
    }

    public int getLengthInSamples() {
        return (int) (getLengthInBytes() / (double) audioFormat.getFrameSize());
    }

    public float getLengthInSeconds() {
        return Math.round(getLengthInSamples() / audioFormat.getSampleRate());
    }

    public float getSampleRate() {
        return audioFormat.getSampleRate();
    }

    public int getMaxSampleHeight() {
        return (int) Math.round(Math.pow(2, audioFormat.getSampleSizeInBits()));
    }

    private OutputStream getOutputStream() throws IOException {
        MyByteArrayOutputStream os = new MyByteArrayOutputStream(streamData);
        os.addListener(new NewDataListener() {

            public void newData(int offset, int length) {
                for (ClipListener l : clipListeners) {
                    l.newData(offset, length);
                    streamLength = offset + length;
                }
            }
        });
        return os;
    }

    /**
   * Start recording data. Sends Newevents to NewDataListeners when new data is read.
   * Recording happens in a separate thread, and new data listeners are activated in the
   * record thread.
   * 
   * @throws AudioException
   */
    public void record() throws AudioException {
        try {
            streamLength = 0;
            if (player != null) {
                for (ClipListener l : clipListeners) {
                    l.playerDisposed(player);
                }
                try {
                    player.stop();
                    player.flush();
                    player.drain();
                    player.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                player = null;
            }
            if (audioPositionTracker == null) initAudioTracker();
            recordThread.start(getOutputStream());
        } catch (Exception e) {
            throw new AudioException(e);
        }
    }

    /**
   * Clears data from the clip.
   */
    public void clear() {
        streamLength = 0;
        streamData.reset();
    }

    public boolean isRecording() {
        return recordThread != null && recordThread.isRecording();
    }

    private static class RecordThread {

        private final LineAndStream mSource;

        private OutputStream mDest;

        private AtomicBoolean mStop = new AtomicBoolean(false);

        private Thread mThread;

        public RecordThread(LineAndStream source) {
            mSource = source;
        }

        public synchronized void stop() {
            mStop.set(true);
        }

        public boolean isRecording() {
            return mThread != null && mThread.isAlive() && !mStop.get();
        }

        public void start(OutputStream dest) {
            if (isRecording()) {
                throw new IllegalStateException("Already recording.  Thread is " + mThread);
            }
            mStop.set(false);
            mDest = dest;
            mThread = new Thread(new Runnable() {

                public void run() {
                    RecordThread.this.run();
                }
            });
            mThread.start();
        }

        private Exception mException;

        public Exception getLastException() {
            return mException;
        }

        public void run() {
            try {
                byte[] buffer = new byte[1024];
                if (!mSource.getLine().isOpen()) mSource.getLine().open();
                mSource.getLine().start();
                while (!mStop.get()) {
                    int read = mSource.getStream().read(buffer, 0, buffer.length);
                    mDest.write(buffer, 0, read);
                }
                mSource.getLine().stop();
                mSource.getLine().flush();
                mDest.flush();
                mDest.close();
            } catch (Exception e) {
                e.printStackTrace();
                mException = e;
            }
        }
    }

    public interface ClipListener {

        public void newData(int offset, int length);

        public void playerStopped(DataLine player);

        public void newPlayHead(long playhead);

        public void playerCreated(DataLine player);

        public void playerDisposed(DataLine player);
    }

    /**
   * Saves the file in the given format. If the type extension is "raw" it saves it
   * without a header - just writes the bytes to the file in the current format.
   * 
   * @throws IOException
   */
    public void save(File f, AudioFileFormat.Type t) throws IOException {
        if (t.getExtension().equals("raw")) {
            IOUtils.copy(makeInputStream(), new FileOutputStream(f));
        } else {
            AudioSystem.write(makeStream(), t, f);
        }
    }

    class AudioPositionTracker implements Runnable {

        private boolean halt;

        public AudioPositionTracker() {
        }

        public void stop() {
            halt = true;
        }

        public void run() {
            long lastFramePos = -1;
            while (!halt) {
                if (player != null) {
                    long newFramePos = getPlayHead();
                    if (lastFramePos != newFramePos) {
                        lastFramePos = newFramePos;
                        for (ClipListener l : clipListeners) {
                            if (halt) return;
                            l.newPlayHead(newFramePos);
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    interface UndoableCommand {

        void execute();

        void undo();

        void redo();
    }

    class Cut implements UndoableCommand {

        byte[] oldBuffer;

        int start;

        int end;

        @Override
        public void execute() {
            oldBuffer = new byte[(end - (start + 1)) * audioFormat.getFrameSize()];
            System.arraycopy(streamData.get(), start * audioFormat.getFrameSize(), oldBuffer, 0, oldBuffer.length);
            deleteSelection(start, end);
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public void undo() {
            insertSelection(start, oldBuffer);
            oldBuffer = null;
        }
    }

    class Paste implements UndoableCommand {

        byte[] data;

        int start;

        @Override
        public void execute() {
            insertSelection(start, data);
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public void undo() {
            deleteSelection(start, start + data.length / audioFormat.getFrameSize());
        }
    }

    class Mute implements UndoableCommand {

        byte[] oldBuffer;

        int start;

        int end;

        @Override
        public void execute() {
            oldBuffer = new byte[(end - start) * audioFormat.getFrameSize()];
            System.arraycopy(streamData.get(), start * audioFormat.getFrameSize(), oldBuffer, 0, oldBuffer.length);
            muteSelection(start, end);
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public void undo() {
            writeSelection(start, oldBuffer);
            oldBuffer = null;
        }
    }

    class ChangeAmplitude implements UndoableCommand {

        byte[] oldBuffer;

        float multiplier;

        int start;

        int end;

        @Override
        public void execute() {
            oldBuffer = new byte[(end - start) * audioFormat.getFrameSize()];
            System.arraycopy(streamData.get(), start * audioFormat.getFrameSize(), oldBuffer, 0, oldBuffer.length);
            changeAmplitude(start, end, multiplier, false);
        }

        @Override
        public void redo() {
            execute();
        }

        @Override
        public void undo() {
            writeSelection(start, oldBuffer);
            oldBuffer = null;
        }
    }

    public File getCurrentFile() {
        return currentFile;
    }
}
