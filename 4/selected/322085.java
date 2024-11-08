package soundengine;

import gui.BaseProperties;
import gui.EditHandlerFactory;
import gui.EditViewFocusManager;
import gui.SequenceBox;
import gui.SequenceBoxComponentLayer;
import gui.TimeBoxManager;
import gui.TrackProperties;
import java.awt.Component;
import java.awt.Container;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.JComponent;
import preferences.Preference;
import common.Dialogs;
import common.IProgress;

public class WavSoundFile {

    public static final int RIFF_CHUNK_SIZE = 12;

    public static final int FORMAT_CHUNK_SIZE = 24;

    public static final int DATA_CHUNK_HEADER_SIZE = 8;

    public static final int REST_OF_FORMAT_CHUNK_BYTES = 16;

    public static final short AUDIO_FORMAT = 1;

    private static final WavSoundFile m_wavSoundF = new WavSoundFile();

    private short m_blockAlign;

    private int m_dataChunkBufSize;

    private int m_sampleRate;

    private int m_numSamples;

    private WavSoundFile() {
    }

    static void generateSoundFile(File tempSoundFile, String intermediateFileName, BaseProperties selectedBaseProp, JComponent selectedComponent, boolean quickCompute, IProgress progress) throws Exception {
        m_wavSoundF.generateSoundF(tempSoundFile, intermediateFileName, selectedBaseProp, selectedComponent, progress, quickCompute);
    }

    void generateSoundF(File tempSoundFile, String intermediateFileName, BaseProperties selectedBaseProp, JComponent selectedComponent, IProgress progress, boolean quickCompute) throws Exception {
        progress.setProgressNote("Creating peer tree");
        PeerFactory.initialise();
        IPeer selectedPeer = createPeerTree(selectedBaseProp, selectedComponent);
        progress.setProgressNote("Computing stats");
        Statistics stats = selectedPeer.computeStats();
        Progress progressWrapper = new Progress(progress, stats);
        System.out.println("No of instruments = " + stats.getNoOfInstruments());
        int noOfMillisecs = stats.getNoOfMillisecs();
        System.out.println("No of millisecs = " + noOfMillisecs);
        int noOfMillisecsDuration = stats.getNoOfMillisecsDuration();
        System.out.println("No of millisecs duration = " + noOfMillisecsDuration);
        progress.setProgressMax(noOfMillisecs);
        ByteBuffer formatChunk = setupFormatChunk();
        ByteBuffer dataChunk = setupDataChunk(selectedPeer.getDuration());
        ByteBuffer riffChunk = setupRiffChunk();
        IComputeSound computeSound = new ComputeWavSoundDouble(dataChunk, m_numSamples);
        progressWrapper.updateNote();
        selectedPeer.computeSound(computeSound, progressWrapper, quickCompute);
        if (selectedBaseProp.isKeepIntermediateFile()) {
            double[] sampleArr = computeSound.getSampleArr();
            if (sampleArr.length > 0) {
                progress.setProgressNote("Writing intermediate file...");
                File intMedSoundFile = new File(intermediateFileName);
                intMedSoundFile.createNewFile();
                FileOutputStream fout = new FileOutputStream(intMedSoundFile);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fout));
                for (int i = 0; i < sampleArr.length; i++) {
                    out.writeDouble(sampleArr[i]);
                }
                out.close();
                fout.close();
                selectedBaseProp.setIntermediateFile(intermediateFileName);
            }
        }
        progress.setProgressNote("Populating data buffer...");
        computeSound.populateDataBuf();
        writeSoundFile(tempSoundFile, progress, formatChunk, dataChunk, riffChunk);
    }

    public static void writeSoundFile(File tempSoundFile, IProgress progress, ByteBuffer formatChunk, ByteBuffer dataChunk, ByteBuffer riffChunk) {
        try {
            FileOutputStream fout = new FileOutputStream(tempSoundFile);
            FileChannel out = fout.getChannel();
            riffChunk.flip();
            out.write(riffChunk);
            formatChunk.flip();
            out.write(formatChunk);
            dataChunk.position(dataChunk.limit());
            dataChunk.flip();
            out.write(dataChunk);
            out.close();
            fout.close();
            progress.setProgressNote("Sound computation complete");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Dialogs.showErrorDialog(null, "Error " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    static IPeer createPeerTree(BaseProperties selectedBaseProp, JComponent selectedComponent) {
        IPeer selectedPeer = selectedBaseProp.createPeer();
        IPeer prevPeer = selectedPeer;
        JComponent prevComp = selectedComponent;
        Container c;
        boolean foundTopParent = false;
        do {
            c = prevComp.getParent();
            if (c instanceof JComponent) {
                JComponent curComp = (JComponent) c;
                if (curComp instanceof SequenceBoxComponentLayer) {
                    prevComp = curComp;
                    continue;
                }
                BaseProperties baseProp = (BaseProperties) curComp.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
                if (baseProp == null) foundTopParent = true; else {
                    IPeer curPeer = baseProp.createPeer();
                    if (curPeer instanceof IChordGroup) {
                        setupEarlierChildren((IChordGroup) curPeer, curComp, prevComp);
                    }
                    prevPeer.setParent(curPeer);
                    prevComp = curComp;
                    prevPeer = curPeer;
                }
            } else foundTopParent = true;
        } while (!foundTopParent);
        if (selectedBaseProp instanceof TrackProperties) {
            TrackProperties trackProp = (TrackProperties) selectedBaseProp;
            setTrackChildren(selectedPeer, trackProp.getTrackNo());
        } else {
            setChildren(selectedPeer, selectedComponent);
        }
        return selectedPeer;
    }

    private static void setupEarlierChildren(IChordGroup chordGrpPeer, JComponent chordComp, JComponent chordChildComp) {
        int componentCount = chordComp.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            Component curComponent = chordComp.getComponent(i);
            if (curComponent == chordChildComp) {
                break;
            }
            JComponent curComp = (JComponent) curComponent;
            BaseProperties baseProp = (BaseProperties) curComp.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
            if (baseProp != null) {
                IPeer curPeer = baseProp.createPeer();
                curPeer.setParent(chordGrpPeer);
                chordGrpPeer.addChild(curPeer);
                setChildrenToSoundLevel(curPeer, curComp);
            }
        }
    }

    private static void setTrackChildren(IPeer parentPeer, int trackNo) {
        TimeBoxManager timeBoxMgr = EditHandlerFactory.getTimeBoxManager(trackNo);
        Collection<SequenceBox> seqBoxes = timeBoxMgr.getAllComponents();
        Iterator<SequenceBox> seqIter = seqBoxes.iterator();
        for (; seqIter.hasNext(); ) {
            SequenceBox seqBox = seqIter.next();
            BaseProperties baseProp = (BaseProperties) seqBox.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
            if (baseProp != null) {
                IPeer curPeer = baseProp.createPeer();
                curPeer.setParent(parentPeer);
                parentPeer.addChild(curPeer);
                setChildren(curPeer, seqBox);
            }
        }
    }

    private static void setChildren(IPeer parentPeer, JComponent parentComponent) {
        int componentCount = parentComponent.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            Component component = parentComponent.getComponent(i);
            if (component instanceof JComponent) {
                JComponent curComp = (JComponent) component;
                if (curComp instanceof SequenceBoxComponentLayer) {
                    setChildren(parentPeer, curComp);
                    break;
                }
                BaseProperties baseProp = (BaseProperties) curComp.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
                if (baseProp != null) {
                    IPeer curPeer = baseProp.createPeer();
                    curPeer.setParent(parentPeer);
                    parentPeer.addChild(curPeer);
                    setChildren(curPeer, curComp);
                }
            }
        }
    }

    private static void setChildrenToSoundLevel(IPeer parentPeer, JComponent parentComponent) {
        int componentCount = parentComponent.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            Component component = parentComponent.getComponent(i);
            if (component instanceof JComponent) {
                JComponent curComp = (JComponent) component;
                BaseProperties baseProp = (BaseProperties) curComp.getClientProperty(EditViewFocusManager.USER_PROPERTIES);
                if (baseProp != null) {
                    IPeer curPeer = baseProp.createPeer();
                    boolean isSound = curPeer instanceof ISound;
                    curPeer.setParent(parentPeer);
                    parentPeer.addChild(curPeer);
                    if (!isSound) setChildrenToSoundLevel(curPeer, curComp);
                }
            }
        }
    }

    public static ByteBuffer setupDataChunkExternal(Number duration) {
        return m_wavSoundF.setupDataChunk(duration);
    }

    private ByteBuffer setupDataChunk(Number duration) {
        m_numSamples = (int) Math.ceil(m_sampleRate * duration.doubleValue());
        int dataChunkSize = m_numSamples * m_blockAlign;
        int padByte = 0;
        if (dataChunkSize % 2 != 0) {
            padByte = 1;
        }
        m_dataChunkBufSize = dataChunkSize + 8 + padByte;
        ByteBuffer dataChunk = ByteBuffer.allocate(m_dataChunkBufSize);
        dataChunk.order(ByteOrder.BIG_ENDIAN);
        dataChunk.put((byte) 'd');
        dataChunk.put((byte) 'a');
        dataChunk.put((byte) 't');
        dataChunk.put((byte) 'a');
        dataChunk.order(ByteOrder.LITTLE_ENDIAN);
        dataChunk.putInt(dataChunkSize);
        return dataChunk;
    }

    public static ByteBuffer setupFormatChunkExternal() {
        return m_wavSoundF.setupFormatChunk();
    }

    private ByteBuffer setupFormatChunk() {
        ByteBuffer formatChunk = ByteBuffer.allocate(FORMAT_CHUNK_SIZE);
        formatChunk.order(ByteOrder.BIG_ENDIAN);
        formatChunk.put((byte) 'f');
        formatChunk.put((byte) 'm');
        formatChunk.put((byte) 't');
        formatChunk.put((byte) ' ');
        formatChunk.order(ByteOrder.LITTLE_ENDIAN);
        formatChunk.putInt(REST_OF_FORMAT_CHUNK_BYTES);
        formatChunk.putShort(AUDIO_FORMAT);
        short numChannels = (short) Preference.getNumChannels();
        formatChunk.putShort(numChannels);
        m_sampleRate = Preference.getSampleRate();
        formatChunk.putInt(m_sampleRate);
        short bitsPerSample = (short) Preference.getBitsPerSample();
        m_blockAlign = (short) (numChannels * Math.ceil(bitsPerSample / 8.0f));
        int averageBytesPerSec = m_sampleRate * m_blockAlign;
        formatChunk.putInt(averageBytesPerSec);
        formatChunk.putShort(m_blockAlign);
        formatChunk.putShort(bitsPerSample);
        return formatChunk;
    }

    public static ByteBuffer setupRiffChunkExternal() {
        return m_wavSoundF.setupRiffChunk();
    }

    /**
     * @param chunkSize TODO
     * @return
     */
    private ByteBuffer setupRiffChunk() {
        ByteBuffer riffChunk = ByteBuffer.allocate(RIFF_CHUNK_SIZE);
        riffChunk.order(ByteOrder.BIG_ENDIAN);
        riffChunk.put((byte) 'R');
        riffChunk.put((byte) 'I');
        riffChunk.put((byte) 'F');
        riffChunk.put((byte) 'F');
        riffChunk.order(ByteOrder.LITTLE_ENDIAN);
        int chunkSize = FORMAT_CHUNK_SIZE + m_dataChunkBufSize + 4;
        riffChunk.putInt(chunkSize);
        riffChunk.order(ByteOrder.BIG_ENDIAN);
        riffChunk.put((byte) 'W');
        riffChunk.put((byte) 'A');
        riffChunk.put((byte) 'V');
        riffChunk.put((byte) 'E');
        return riffChunk;
    }
}
