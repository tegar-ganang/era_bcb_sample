package gui;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import soundengine.IPeer;
import soundengine.PeerFactory;
import soundengine.RecordedSoundPeerImpl;
import soundengine.RecordedSoundPeerImpl.FormatChunkInfo;
import common.Dialogs;
import common.Utils;
import database.DbRecordedSound;

/**
 * @author Roo and Joey
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RecordedSoundProperties extends BoxPropertiesAdapter {

    public static final byte RECORDED_SOUND_FLD_RAW_SOUND_FILE = 0;

    public static final byte RECORDED_SOUND_FLD_START_OFS = 1;

    public static final byte RECORDED_SOUND_FLD_DURATION = 2;

    public static final byte RECORDED_SOUND_FLD_LEFT_CHAN = 3;

    public static final byte RECORDED_SOUND_FLD_RIGHT_CHAN = 4;

    public static final byte RECORDED_SOUND_FLD_AMPLITUDE_EXTENT_MULT = 5;

    public static final byte RECORDED_SOUND_FLD_AMPLITUDE_GRAPH = 6;

    private String m_rawSoundFile = "";

    private double m_duration;

    private double m_startOfs;

    private boolean m_leftChanSelected = true;

    private double m_amplitudeGraphExtentMult = 1.0f;

    private String m_amplitudeGraph = "";

    private int m_amplitudeGraphId;

    private IDirtyable m_dirtyAble;

    public RecordedSoundProperties() {
        m_commonProperties = new CommonProperties(true);
    }

    /**
     * @param origSoundProperties
     */
    public RecordedSoundProperties(RecordedSoundProperties prop) {
        super(prop);
        m_rawSoundFile = prop.m_rawSoundFile;
        m_duration = prop.m_duration;
        m_startOfs = prop.m_startOfs;
        m_leftChanSelected = prop.m_leftChanSelected;
        m_amplitudeGraphExtentMult = prop.m_amplitudeGraphExtentMult;
        m_amplitudeGraph = prop.m_amplitudeGraph;
        m_amplitudeGraphId = prop.m_amplitudeGraphId;
        m_commonProperties = new CommonProperties(prop.getCommonProperties());
    }

    /**
     * @return Returns the commonProperties.
     */
    public CommonProperties getCommonProperties() {
        return m_commonProperties;
    }

    /**
     * @return Returns the duration.
     */
    public double getDuration() {
        return m_duration;
    }

    /**
     * @param duration The duration to set.
     */
    public void setDuration(double duration) {
        m_duration = duration;
    }

    public ValueChangeListener newValueChangeListener(EditViewHandler viewHandler, JComponent component, IBoxProperties boxProperties, byte fieldId, JTextField formattedField, IDirtyable dirtyAble) {
        return new RecordedSoundValueChangeListener(viewHandler, component, (RecordedSoundProperties) boxProperties, fieldId, formattedField, dirtyAble);
    }

    public ActionListener newActionListener(EditViewHandler viewHandler, JComponent selectedComponent, IBoxProperties boxProperties, byte fieldId, JComponent component, IDirtyable dirtyAble) {
        return new RecordedSoundValueChangeListener(viewHandler, selectedComponent, (RecordedSoundProperties) boxProperties, fieldId, (JComponent) component, dirtyAble);
    }

    @Override
    public ItemListener newItemListener(EditViewHandler viewHandler, JComponent selectedComponent, IBoxProperties boxProperties, final byte fieldId, JCheckBox cb, IDirtyable dirtyAble) {
        m_dirtyAble = dirtyAble;
        return new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                m_dirtyAble.setDirty();
                switch(fieldId) {
                    default:
                        throw new IllegalStateException("Unexpected value " + fieldId + " in itemStateChanged");
                }
            }
        };
    }

    /**
     * @return Returns the pitchGraph.
     */
    @Override
    public IPeer createPeer() {
        return PeerFactory.newRecordedSoundPeer(this);
    }

    public String getRawSoundFile() {
        return m_rawSoundFile;
    }

    /**
     * @param rawSoundFile the rawSoundFile to set
     */
    public void setRawSoundFile(String rawSoundFile) {
        m_rawSoundFile = rawSoundFile;
    }

    public double getStartOfs() {
        return m_startOfs;
    }

    /**
     * @param startOfs the startOfs to set
     */
    public void setStartOfs(double startOfs) {
        m_startOfs = startOfs;
    }

    public boolean getLeftChanSelected() {
        return m_leftChanSelected;
    }

    /**
     * @param leftChanSelected the leftChanSelected to set
     */
    public void setLeftChanSelected(boolean leftChanSelected) {
        m_leftChanSelected = leftChanSelected;
    }

    public double getAmplitudeGraphExtentMult() {
        return m_amplitudeGraphExtentMult;
    }

    /**
     * @param amplitudeGraphExtentMult the amplitudeGraphExtentMult to set
     */
    public void setAmplitudeGraphExtentMult(double amplitudeGraphExtentMult) {
        m_amplitudeGraphExtentMult = amplitudeGraphExtentMult;
    }

    public String getAmplitudeGraph() {
        return m_amplitudeGraph;
    }

    /**
     * @param amplitudeGraph the amplitudeGraph to set
     */
    public void setAmplitudeGraph(String amplitudeGraph) {
        m_amplitudeGraph = amplitudeGraph;
    }

    /**
     * @return the amplitudeGraphId
     */
    public int getAmplitudeGraphId() {
        return m_amplitudeGraphId;
    }

    /**
     * @param amplitudeGraphId the amplitudeGraphId to set
     */
    public void setAmplitudeGraphId(int amplitudeGraphId) {
        m_amplitudeGraphId = amplitudeGraphId;
    }

    @Override
    public String getDurationAdditionalMsg() throws Exception {
        if (!Utils.isBlank(m_rawSoundFile)) {
            File rawSoundFile = new File(m_rawSoundFile);
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
                int chunkSize = RecordedSoundPeerImpl.readRiffChunk(in);
                FormatChunkInfo info = RecordedSoundPeerImpl.readFormatChunk(in);
                int dataChunkSize = RecordedSoundPeerImpl.readDataChunkHeader(in);
                StringBuffer msg = new StringBuffer();
                msg.append("Raw file: Sample rate = ");
                msg.append(info.m_sampleRate);
                msg.append(" Avg bytes/sec = ");
                msg.append(info.m_avgBytesPerSec);
                msg.append(" Bits/sample = ");
                msg.append(info.m_bitsPerSample);
                msg.append(" Bytes/frame = ");
                msg.append(info.m_blockAlign);
                msg.append(" Channels = ");
                msg.append(info.m_numChannels);
                msg.append("\n               Data chunk size = ");
                msg.append(dataChunkSize);
                int numSamples = dataChunkSize / info.m_blockAlign;
                msg.append(" Samples = ");
                msg.append(numSamples);
                double duration = (double) dataChunkSize / info.m_avgBytesPerSec;
                msg.append(" Duration = ");
                msg.append(duration);
                return msg.toString();
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } finally {
                in.close();
                fin.close();
            }
        }
        return null;
    }

    @Override
    public void save(Connection conn, Component frame, int indexInParent, int parentDbId, JComponent component, boolean parentIsSequence) throws SQLException {
        DbRecordedSound.save(this, conn, frame, indexInParent, parentDbId, parentIsSequence);
    }

    @Override
    public String getBoxType() {
        return MainProg.RECORDED_SOUND;
    }
}
