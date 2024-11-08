package com.google.code.b0rx0r.program;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.tritonus.share.sampled.FloatSampleBuffer;
import com.google.code.b0rx0r.ColorScheme;
import com.google.code.b0rx0r.advancedSamplerEngine.ChannelOutputMap;
import com.sun.media.sound.WaveFileReader;

@SuppressWarnings("serial")
public class Sample extends AbstractTriggerable implements Serializable, Sequencable, SampleDataContainer, OffsetSampleDataContainer {

    private static final long serialVersionUID = "Sample 1.0.1".hashCode();

    private File file;

    private Set<OffsetSampleDataContainer> asSet = Collections.<OffsetSampleDataContainer>singleton(this);

    private Set<SnapPoint> snapPoints = new HashSet<SnapPoint>();

    private Set<Slice> slices = new HashSet<Slice>();

    private ChannelOutputMap outputMap = new ChannelOutputMap();

    private transient List<SampleListener> observers = new ArrayList<SampleListener>();

    private transient FloatSampleBuffer fsb;

    private int startOffset = 0;

    private int endOffset = 0;

    private SnapPoint startSnapPoint;

    private SnapPoint endSnapPoint;

    private float playbackSpeed = 1.0f;

    public Sample() {
        this.file = new File("foo.bar");
        setName("foo.bar");
        setColor(ColorScheme.DEFAULT_SAMPLE_COLOR);
        fsb = new FloatSampleBuffer(1, 1000, 44100);
        for (int i = 0; i < 1000; i++) {
            float value = i % 100 == 0 ? 1f : 0.1f;
            fsb.getChannel(0)[i] = value;
        }
        addStartAndEndSnapPoints();
        addSnapPointAt(100);
        addSnapPointAt(500);
        addSnapPointAt(900);
        for (int channel = 0; channel < fsb.getChannelCount(); channel++) {
            outputMap.addMapping(channel, channel);
        }
    }

    public Sample(File f) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        this.file = f;
        setName(f.getName());
        setColor(ColorScheme.DEFAULT_SAMPLE_COLOR);
        readSampleData(f);
        addStartAndEndSnapPoints();
        for (int channel = 0; channel < fsb.getChannelCount(); channel++) {
            outputMap.addMapping(channel, channel);
        }
    }

    private void readSampleData(File f) throws UnsupportedAudioFileException, IOException {
        WaveFileReader r = new WaveFileReader();
        AudioInputStream s = r.getAudioInputStream(f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(s, baos);
        s.close();
        fsb = new FloatSampleBuffer();
        byte[] data = baos.toByteArray();
        fsb.initFromByteArray(data, 0, data.length, s.getFormat());
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int read = in.read(buffer);
            if (read == -1) break;
            out.write(buffer, 0, read);
        }
    }

    public Set<OffsetSampleDataContainer> getSampleData() {
        return asSet;
    }

    public FloatSampleBuffer getFloatSampleBuffer() {
        return fsb;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        if (fsb == null) return 0;
        return fsb.getSampleCount() - endOffset;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void addSampleObserver(SampleListener observer) {
        observers.add(observer);
    }

    public void removeSampleObserver(SampleListener observer) {
        observers.remove(observer);
    }

    public void addSnapPoint(SnapPoint point) {
        snapPoints.add(point);
        fireSnapPointAdded(point);
    }

    public void deleteSnapPoint(SnapPoint point) {
        if (point == startSnapPoint || point == endSnapPoint) return;
        snapPoints.remove(point);
        fireSnapPointRemoved(point);
    }

    private void fireSnapPointAdded(SnapPoint point) {
        for (SampleListener observer : observers) {
            observer.snapPointAdded(point);
        }
    }

    private void fireSnapPointRemoved(SnapPoint point) {
        for (SampleListener observer : observers) {
            observer.snapPointRemoved(point);
        }
    }

    public void fireSnapPointMoved(SnapPoint point) {
        for (SampleListener observer : observers) {
            observer.snapPointMoved(point);
        }
    }

    public Set<SnapPoint> getSnapPoints() {
        return snapPoints;
    }

    public void addSnapPointAtNearestZeroCrossing(int offset) {
        addSnapPointAt(getNearestZeroCrossing(offset));
    }

    public int getNearestZeroCrossing(int offset) {
        return offset;
    }

    public void addSnapPointAt(int offset) {
        AbsoluteSnapPoint sp = new AbsoluteSnapPoint(this, offset);
        addSnapPoint(sp);
    }

    private interface BoundChecker extends Serializable {

        int getStartBound();

        boolean isBetter(int currentBound, int startOffset, int checkThisOffset);
    }

    BoundChecker BC_BEFORE = new BoundChecker() {

        @Override
        public int getStartBound() {
            return 0;
        }

        @Override
        public boolean isBetter(int currentBound, int startOffset, int checkThisOffset) {
            return (checkThisOffset < startOffset && checkThisOffset > currentBound);
        }
    };

    BoundChecker BC_AFTER = new BoundChecker() {

        @Override
        public int getStartBound() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isBetter(int currentBound, int startOffset, int checkThisOffset) {
            return (checkThisOffset > startOffset && checkThisOffset < currentBound);
        }
    };

    public SnapPoint findSnapPointBefore(int offset) {
        return findSnapPoint(offset, BC_BEFORE);
    }

    public SnapPoint findSnapPointAfter(int offset) {
        return findSnapPoint(offset, BC_AFTER);
    }

    private SnapPoint findSnapPoint(int offset, BoundChecker checker) {
        int currentBest = checker.getStartBound();
        SnapPoint retVal = null;
        for (SnapPoint sp : snapPoints) {
            if (checker.isBetter(currentBest, offset, sp.getOffset())) {
                currentBest = sp.getOffset();
                retVal = sp;
            }
        }
        return retVal;
    }

    public Slice addSliceBetween(SnapPoint start, SnapPoint end) {
        Slice s = new Slice(this, start, end);
        s.setName("slice");
        s.setColor(ColorScheme.DEFAULT_SLICE_COLOR);
        addSlice(s);
        return s;
    }

    public void addSlice(Slice slice) {
        slices.add(slice);
        fireSliceAdded(slice);
    }

    public void deleteSlice(Slice slice) {
        slices.remove(slice);
        fireSliceRemoved(slice);
    }

    private void fireSliceAdded(Slice slice) {
        for (SampleListener observer : observers) {
            observer.sliceAdded(slice);
        }
    }

    private void fireSliceRemoved(Slice slice) {
        for (SampleListener observer : observers) {
            observer.sliceRemoved(slice);
        }
    }

    public Set<Slice> getSlices() {
        return slices;
    }

    public Slice findSlice(int offset) {
        for (Slice s : slices) {
            if (offset >= s.getStartOffset() && offset <= s.getEndOffset()) {
                return s;
            }
        }
        return null;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        observers = new ArrayList<SampleListener>();
        try {
            readSampleData(file);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getLength() {
        return getEndOffset() - getStartOffset();
    }

    @Override
    public Iterable<SnapPoint> getRelativeSnapPoints() {
        return snapPoints;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public SampleDataContainer getSampleDataContainer() {
        return this;
    }

    public void clearAllSnapPoints() {
        for (Slice slice : new ArrayList<Slice>(slices)) {
            deleteSlice(slice);
        }
        for (SnapPoint sp : new ArrayList<SnapPoint>(snapPoints)) {
            deleteSnapPoint(sp);
        }
        addStartAndEndSnapPoints();
    }

    private void addStartAndEndSnapPoints() {
        if (this.startSnapPoint == null) this.startSnapPoint = new SampleStartSnapPoint(this);
        if (this.endSnapPoint == null) this.endSnapPoint = new SampleEndSnapPoint(this);
        snapPoints.add(startSnapPoint);
        snapPoints.add(endSnapPoint);
    }

    @Override
    public ChannelOutputMap getOutputMap() {
        return outputMap;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = fsb.getSampleCount() - endOffset;
    }

    private static class SampleStartSnapPoint extends SnapPoint {

        Sample sample;

        public SampleStartSnapPoint(Sample sample) {
            super(sample, "sample start");
            this.sample = sample;
        }

        @Override
        public int getOffset() {
            return sample.getStartOffset();
        }

        @Override
        public void setOffset(int offset) {
            sample.setStartOffset(offset);
        }
    }

    private static class SampleEndSnapPoint extends SnapPoint {

        Sample sample;

        public SampleEndSnapPoint(Sample sample) {
            super(sample, "sample start");
            this.sample = sample;
        }

        @Override
        public int getOffset() {
            return sample.getEndOffset();
        }

        @Override
        public void setOffset(int offset) {
            sample.setEndOffset(offset);
        }
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    @Override
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }
}
