package org.jsresources.apps.jam.style;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.jsresources.apps.jmvp.manager.RM;
import org.jsresources.apps.jmvp.model.FileModel;
import org.jsresources.apps.jmvp.model.Model;
import org.jsresources.apps.jmvp.model.ModelEvent;
import org.jsresources.apps.jmvp.selection.AbstractSelection;
import org.jsresources.apps.jmvp.selection.Selection;
import org.jsresources.apps.jmvp.util.Matrix;
import org.jsresources.apps.jam.Debug;
import org.jsresources.apps.jam.util.XmlError;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class Style extends FileModel {

    public static final int INSERT_AT_END = -1;

    private String m_strSessionName;

    private int m_nBeatsPerMinute;

    /** AudioFormat for all binary audio data here.
	 */
    private AudioFormat m_audioFormat;

    private int m_nTrackCount;

    private int m_nPhaseCount;

    private Matrix<Cell> m_mCells;

    private List<Track> m_vTracks;

    private List<Phase> m_vPhases;

    public Style(int nBeatsPerMinute, AudioFormat audioFormat) {
        this(nBeatsPerMinute, audioFormat, 1, 1, "");
    }

    public Style(int nBeatsPerMinute, AudioFormat audioFormat, int nTrackCount, int nPhaseCount, String sessionName) {
        m_strSessionName = sessionName;
        m_nBeatsPerMinute = nBeatsPerMinute;
        m_audioFormat = audioFormat;
        m_nTrackCount = nTrackCount;
        m_nPhaseCount = nPhaseCount;
        m_mCells = new Matrix<Cell>(getTrackCount(), getPhaseCount());
        for (int nTrack = 0; nTrack < getTrackCount(); nTrack++) {
            for (int nPhase = 0; nPhase < getPhaseCount(); nPhase++) {
                Cell cell = new Cell();
                m_mCells.set(nTrack, nPhase, cell);
            }
        }
        m_vTracks = new ArrayList<Track>();
        for (int nTrack = 0; nTrack < getTrackCount(); nTrack++) {
            Track track = new Track();
            m_vTracks.add(track);
        }
        m_vPhases = new ArrayList<Phase>();
        for (int nPhase = 0; nPhase < getPhaseCount(); nPhase++) {
            Phase phase = new Phase();
            m_vPhases.add(phase);
        }
    }

    public String getSessionName() {
        return m_strSessionName;
    }

    public void setSessionName(String sessionName) {
        m_strSessionName = sessionName;
    }

    public int getBeatsPerMinute() {
        return m_nBeatsPerMinute;
    }

    public AudioFormat getAudioFormat() {
        return m_audioFormat;
    }

    public int getAudioDataSampleCount(int nPhase) {
        float sampleRate = getAudioFormat().getSampleRate();
        float bpm = getBeatsPerMinute();
        float durationInSeconds = 60 / bpm * getPhaseBeatsPerMeasure(nPhase) * getPhaseMeasures(nPhase);
        return (int) (sampleRate * durationInSeconds);
    }

    private int getAudioDataByteLength(int nPhase) {
        AudioFormat format = getAudioFormat();
        return getAudioDataSampleCount(nPhase) * ((format.getSampleSizeInBits() + 7) / 8) * format.getChannels();
    }

    /** gets the number of tracks
	 *
	 *	@return number of tracks
	 */
    public int getTrackCount() {
        return m_nTrackCount;
    }

    /** gets the number of phases
	 *
	 *	@return number of phases
	 */
    public int getPhaseCount() {
        return m_nPhaseCount;
    }

    /** returns the cell at the specified position.
	 */
    private Cell getCell(int nTrack, int nPhase) {
        if (Debug.getTraceStyle()) {
            Debug.out("Style.getCell(): begin");
        }
        if (Debug.getTraceStyle()) {
            Debug.out("Style.getCell(): track: " + nTrack);
        }
        if (Debug.getTraceStyle()) {
            Debug.out("Style.getCell(): phase: " + nPhase);
        }
        if (nTrack < 0 || nTrack >= getTrackCount() || nPhase < 0 || nPhase >= getPhaseCount()) {
            return null;
        }
        Cell cell = m_mCells.get(nTrack, nPhase);
        if (Debug.getTraceStyle()) {
            Debug.out("Style.getCell(): end");
        }
        return cell;
    }

    private void setCell(int nTrack, int nPhase, Cell cell) {
        m_mCells.set(nTrack, nPhase, cell);
    }

    /**
	 */
    public void setCellAudioData(int nTrack, int nPhase, byte[] abData) {
        int sampleCount = getAudioDataSampleCount(nPhase);
        AudioFormat format = getAudioFormat();
        FloatSampleBuffer fsb = getCellAudioData(nTrack, nPhase);
        if (fsb == null) {
            fsb = new FloatSampleBuffer(format.getChannels(), sampleCount, format.getSampleRate());
        }
        int byteSize = getAudioDataByteLength(nPhase);
        if (byteSize > abData.length) {
            byteSize = abData.length;
        }
        fsb.initFromByteArray(abData, 0, byteSize, format, true);
        setCellAudioData(nTrack, nPhase, fsb);
    }

    public void setCellAudioData(int nTrack, int nPhase, FloatSampleBuffer fsb) {
        Cell cell = getCell(nTrack, nPhase);
        if (cell != null) {
            fsb.changeSampleCount(getAudioDataSampleCount(nPhase), true);
            cell.setAudioData(fsb);
            setModified();
            notifyCellChange(nTrack, nPhase);
        }
    }

    public FloatSampleBuffer getCellAudioData(int nTrack, int nPhase) {
        Cell cell = getCell(nTrack, nPhase);
        if (cell != null) {
            return cell.getAudioData();
        }
        return null;
    }

    public synchronized void addTrack(String title, String peer) {
        insertTrack(INSERT_AT_END, title, peer);
    }

    /** Inserts a track at the position given.
	Saying "INSERT_AT_END" for the track position adds a track after the last
	existing track. This function is not complete and not tested.
	*/
    private synchronized void insertTrack(int nBeforeTrack, String title, String peer) {
        Track track = new Track();
        track.setTitle(title);
        track.setPeer(peer);
        if (nBeforeTrack == INSERT_AT_END) {
            m_vTracks.add(track);
            m_mCells.appendRow();
            nBeforeTrack = m_mCells.getRowCount() - 1;
        } else {
            m_vTracks.add(nBeforeTrack, track);
            m_mCells.insertRow(nBeforeTrack);
        }
        for (int i = 0; i < getPhaseCount(); i++) {
            Cell cell = new Cell();
            m_mCells.set(nBeforeTrack, i, cell);
        }
        m_nTrackCount++;
        setModified();
        notifyCellArrayChange();
    }

    /**	Gives a description of the track.
	 *
	 *	@param nTrack the track about which information is desired
	 *
	 *	@return A <code>Track</code> object describing the track
	 *
	 */
    private Track getTrack(int nTrack) {
        return m_vTracks.get(nTrack);
    }

    private void setTrack(int nTrack, Track track) {
        m_vTracks.set(nTrack, track);
    }

    public String getTrackTitle(int nTrack) {
        return getTrack(nTrack).getTitle();
    }

    public void setTrackTitle(int nTrack, String sTitle) {
        getTrack(nTrack).setTitle(sTitle);
        setModified();
        notifyTrackChange(nTrack);
    }

    public String getTrackPeer(int nTrack) {
        return getTrack(nTrack).getPeer();
    }

    public void setTrackPeer(int nTrack, String sPeer) {
        getTrack(nTrack).setPeer(sPeer);
        setModified();
        notifyTrackChange(nTrack);
    }

    public boolean getTrackMute(int nTrack) {
        return getTrack(nTrack).getMute();
    }

    public void setTrackMute(int nTrack, boolean bMute) {
        getTrack(nTrack).setMute(bMute);
        setModified();
        notifyTrackChange(nTrack);
    }

    public boolean getTrackSolo(int nTrack) {
        return getTrack(nTrack).getSolo();
    }

    public synchronized void setTrackSolo(int nTrack, boolean bSolo) {
        if (bSolo == getTrack(nTrack).getSolo()) {
            return;
        }
        if (getTrack(nTrack).getSolo()) {
            getTrack(nTrack).setSolo(false);
            for (int nT = 0; nT < getTrackCount(); nT++) {
                getTrack(nT).setMuteBySolo(false);
            }
        } else {
            getTrack(nTrack).setSolo(true);
            for (int nT = 0; nT < getTrackCount(); nT++) {
                getTrack(nT).setMuteBySolo(true);
            }
            getTrack(nTrack).setMuteBySolo(false);
        }
        setModified();
        notifyTrackChange(nTrack);
    }

    public boolean getTrackActuallyMuted(int nTrack) {
        return getTrack(nTrack).getActuallyMuted();
    }

    public double getTrackGain(int nTrack) {
        return getTrack(nTrack).getGain();
    }

    public void setTrackGain(int nTrack, double dGain) {
        getTrack(nTrack).setGain(dGain);
        setModified();
        notifyTrackChange(nTrack);
    }

    public double getTrackPan(int nTrack) {
        return getTrack(nTrack).getPan();
    }

    public void setTrackPan(int nTrack, double dPan) {
        getTrack(nTrack).setPan(dPan);
        setModified();
        notifyTrackChange(nTrack);
    }

    /** adds a Phase at the end
	*/
    public synchronized void addPhase(int nMeasures, int nRepeat, int nBeatsPerMeasure) {
        Phase phase = new Phase();
        phase.setMeasures(nMeasures);
        phase.setRepeat(nRepeat);
        phase.setBeatsPerMeasure(nBeatsPerMeasure);
        m_vPhases.add(phase);
        m_mCells.appendColumn();
        int nPhase = m_mCells.getColumnCount() - 1;
        for (int t = 0; t < getTrackCount(); t++) {
            Cell newCell = new Cell();
            m_mCells.set(t, nPhase, newCell);
        }
        m_nPhaseCount++;
        setModified();
        notifyCellArrayChange();
    }

    /**	Gives a description of the phase.
	 *
	 *	@param nPhase the phase about which information is desired
	 *
	 *	@return A <code>Phase</code> object describing the phase
	 */
    private Phase getPhase(int nPhase) {
        return m_vPhases.get(nPhase);
    }

    private void setPhase(int nPhase, Phase phase) {
        m_vPhases.set(nPhase, phase);
    }

    public int getPhaseMeasures(int nPhase) {
        return getPhase(nPhase).getMeasures();
    }

    public void setPhaseMeasures(int nPhase, int nMeasures) {
        Phase phase = getPhase(nPhase);
        phase.setMeasures(nMeasures);
        setModified();
        notifyPhaseChange(nPhase);
    }

    public int getPhaseRepeat(int nPhase) {
        return getPhase(nPhase).getRepeat();
    }

    public void setPhaseRepeat(int nPhase, int nRepeat) {
        Phase phase = getPhase(nPhase);
        phase.setRepeat(nRepeat);
        setModified();
        notifyPhaseChange(nPhase);
    }

    public int getPhaseBeatsPerMeasure(int nPhase) {
        return getPhase(nPhase).getBeatsPerMeasure();
    }

    public void setPhaseBeatsPerMeasure(int nPhase, int nBeatsPerMeasure) {
        Phase phase = getPhase(nPhase);
        phase.setBeatsPerMeasure(nBeatsPerMeasure);
        setModified();
        notifyPhaseChange(nPhase);
    }

    public int getPhaseDurationMillis(int nPhase) {
        float sampleRate = getAudioFormat().getSampleRate();
        float bpm = getBeatsPerMinute();
        float durationInMillis = 60000 / bpm * getPhaseBeatsPerMeasure(nPhase) * getPhaseMeasures(nPhase);
        return (int) durationInMillis;
    }

    private void writeXml(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" ?>\n");
        writer.write("<Session\n");
        writer.write("  beatsPerMinute=\"" + getBeatsPerMinute() + "\"\n");
        writer.write("  sessionName=\"" + getSessionName() + "\"\n");
        AudioFormat format = getAudioFormat();
        writer.write("<AudioFormat\n");
        writer.write("  sampleRate=\"" + format.getSampleRate() + "\"\n");
        writer.write("  sampleSizeInBits=\"" + format.getSampleSizeInBits() + "\"\n");
        writer.write("  channels=\"" + format.getChannels() + "\"\n");
        writer.write("  signed=\"" + (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) + "\"\n");
        writer.write("  bigEndian=\"" + format.isBigEndian() + "\" />\n");
        writer.write("<Tracks>\n");
        for (int nTrack = 0; nTrack < getTrackCount(); nTrack++) {
            getTrack(nTrack).writeXml(writer);
        }
        writer.write("</Tracks>\n");
        writer.write("<Phases>\n");
        for (int nPhase = 0; nPhase < getPhaseCount(); nPhase++) {
            getPhase(nPhase).writeXml(writer);
        }
        writer.write("</Phases>\n");
        writer.write("<Cells>\n");
        for (int nTrack = 0; nTrack < getTrackCount(); nTrack++) {
            for (int nPhase = 0; nPhase < getPhaseCount(); nPhase++) {
                getCell(nTrack, nPhase).writeXml(writer);
            }
        }
        writer.write("</Cells>\n");
        writer.write("</Session>\n");
    }

    public void write(File file) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        writeXml(fileWriter);
        fileWriter.close();
        setModified(false);
        notifyDataChange();
    }

    private static Style readXml(File file) {
        if (Debug.getTraceXmlParsing()) {
            Debug.out("Style.readXml(): called");
        }
        XmlError.nErrorCode = XmlError.NONE;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            Debug.out("XMLReader: " + parser.getXMLReader());
            StyleHandler handler = new StyleHandler();
            parser.parse(file, handler);
            if (Debug.getTraceXmlParsing()) {
                Debug.out("Style.readXml(): after parsing");
            }
            Style style = handler.getStyle();
            return style;
        } catch (SAXParseException e) {
            Debug.out("** SAXParseException:");
            Debug.out("message: " + e.getMessage());
            Debug.out("uri:" + e.getSystemId());
            Debug.out("line: " + e.getLineNumber());
            Debug.out("column: " + e.getColumnNumber());
            if (e.getException() != null) {
                Debug.out("embedded exception:");
                Debug.out(e.getException());
            }
            return null;
        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
            return null;
        } catch (Throwable t) {
            Debug.out(t);
            return null;
        }
    }

    private static class StyleHandler extends DefaultHandler {

        Style m_style;

        int m_nBeatsPerMinute;

        int m_nBeatsPerMeasure;

        String m_sessionName;

        int m_nCurrentTrack;

        int m_nCurrentPhase;

        int m_nPhaseCount;

        boolean m_bAudioDataExpected;

        byte[] m_abAudioData;

        int m_nBytePosition;

        boolean m_bHasSavedChar;

        char m_cSavedChar;

        public StyleHandler() {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String strValue;
            if (qName.equals("Session")) {
                strValue = attributes.getValue("beatsPerMinute");
                m_nBeatsPerMinute = Integer.parseInt(strValue);
                strValue = attributes.getValue("sessionName");
                m_sessionName = strValue;
            } else if (qName.equals("AudioFormat")) {
                strValue = attributes.getValue("sampleRate");
                float fSampleRate = Float.parseFloat(strValue);
                strValue = attributes.getValue("sampleSizeInBits");
                int nSampleSizeInBits = Integer.parseInt(strValue);
                strValue = attributes.getValue("channels");
                int nChannels = Integer.parseInt(strValue);
                strValue = attributes.getValue("signed");
                boolean bSigned = Boolean.valueOf(strValue).booleanValue();
                strValue = attributes.getValue("bigEndian");
                boolean bBigEndian = Boolean.valueOf(strValue).booleanValue();
                AudioFormat format = new AudioFormat(fSampleRate, nSampleSizeInBits, nChannels, bSigned, bBigEndian);
                m_style = new Style(m_nBeatsPerMinute, format, 1, 1, m_sessionName);
            } else if (qName.equals("Track")) {
                strValue = attributes.getValue("mute");
                boolean bMute = Boolean.valueOf(strValue).booleanValue();
                strValue = attributes.getValue("solo");
                boolean bSolo = Boolean.valueOf(strValue).booleanValue();
                String title = attributes.getValue("title");
                String peer = attributes.getValue("peer");
                if (m_nCurrentTrack != 0) {
                    m_style.addTrack(title, peer);
                    m_nCurrentTrack++;
                } else {
                    m_style.setTrackTitle(m_nCurrentTrack, title);
                    m_style.setTrackPeer(m_nCurrentTrack, peer);
                }
                m_style.setTrackMute(m_nCurrentTrack, bMute);
                m_style.setTrackSolo(m_nCurrentTrack, bSolo);
            } else if (qName.equals("Phase")) {
                strValue = attributes.getValue("measures");
                int nMeasures = Integer.parseInt(strValue);
                strValue = attributes.getValue("repeat");
                int nRepeat = Integer.parseInt(strValue);
                strValue = attributes.getValue("beatsPerMeasure");
                int nBeatsPerMeasure = Integer.parseInt(strValue);
                if (m_nCurrentPhase != 0) {
                    m_style.addPhase(nMeasures, nRepeat, nBeatsPerMeasure);
                    m_nCurrentPhase++;
                } else {
                    m_style.setPhaseMeasures(0, nMeasures);
                    m_style.setPhaseRepeat(0, nRepeat);
                    m_style.setPhaseBeatsPerMeasure(0, nBeatsPerMeasure);
                }
            } else if (qName.equals("Cells")) {
                m_nCurrentTrack = 0;
                m_nCurrentPhase = 0;
                m_nPhaseCount = m_style.getPhaseCount();
            } else if (qName.equals("AudioData")) {
                strValue = attributes.getValue("length");
                int nLength = Integer.parseInt(strValue);
                m_bAudioDataExpected = true;
                m_abAudioData = new byte[nLength];
                m_nBytePosition = 0;
            }
        }

        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("Cell")) {
                if (m_abAudioData != null) {
                    FloatSampleBuffer fsb = new FloatSampleBuffer(m_abAudioData, 0, m_abAudioData.length, m_style.getAudioFormat());
                    m_style.setCellAudioData(m_nCurrentTrack, m_nCurrentPhase, fsb);
                    m_abAudioData = null;
                }
                m_nCurrentPhase++;
                if (m_nCurrentPhase == m_nPhaseCount) {
                    m_nCurrentPhase = 0;
                    m_nCurrentTrack++;
                }
            } else if (qName.equals("AudioData")) {
                m_bAudioDataExpected = false;
            }
        }

        public void characters(char[] ch, int start, int length) {
            if (m_bAudioDataExpected) {
                while (length > 0) {
                    while (length > 0 && ch[start] == '\n') {
                        start++;
                        length--;
                    }
                    if (length == 0) {
                        break;
                    }
                    int nPos = 0;
                    while (ch[start + nPos] != '\n') {
                        nPos++;
                        if (nPos == length) {
                            break;
                        }
                    }
                    String strHex = new String(ch, start, nPos);
                    start += nPos;
                    length -= nPos;
                    if (m_bHasSavedChar) {
                        strHex = m_cSavedChar + strHex;
                        m_bHasSavedChar = false;
                    }
                    if ((strHex.length() % 2) != 0) {
                        m_cSavedChar = strHex.charAt(strHex.length() - 1);
                        m_bHasSavedChar = true;
                        strHex = strHex.substring(0, strHex.length() - 1);
                    }
                    int nNumBytes = strHex.length() / 2;
                    for (int i = 0; i < nNumBytes; i++) {
                        String strValue = strHex.substring(i * 2, i * 2 + 2);
                        m_abAudioData[i + m_nBytePosition] = (byte) Integer.parseInt(strValue, 16);
                    }
                    m_nBytePosition += nNumBytes;
                }
            }
        }

        public Style getStyle() {
            return m_style;
        }
    }

    public static Style read(File file) {
        Style style = readXml(file);
        if (style != null) {
            style.setFile(file);
            style.setModified(false);
        }
        return style;
    }

    private void setModified() {
        setModified(true);
    }

    /**	Notifies a change in the data of the style.
	 *	This function should be used if data, but not
	 *	the structure of the style are changing.
	 */
    protected void notifyDataChange() {
        fireModelEvent(new ModelEvent(this));
    }

    /**	Notifies a change in the structure of the style.
	 *	Note that the difference to notifyDataChange is
	 *	necessary to enable views to do a "lightwight"
	 *	update if only data has changed.
	 */
    protected void notifyCellArrayChange() {
        notifyCellArrayChange(0, 0, getTrackCount() - 1, getPhaseCount() - 1);
    }

    /**
	 *	notifies a structure change
	 */
    protected void notifyCellArrayChange(int nFirstTrack, int nFirstPhase, int nLastTrack, int nLastPhase) {
        fireModelEvent(new CellArrayChangeEvent(this, nFirstTrack, nFirstPhase, nLastTrack, nLastPhase, true));
    }

    /**
	 *	notifies a track change
	 */
    protected void notifyTrackChange(int nTrack) {
        fireModelEvent(new TrackChangeEvent(this, nTrack));
    }

    /**
	 *	notifies a phase change
	 */
    protected void notifyPhaseChange(int nPhase) {
        fireModelEvent(new PhaseChangeEvent(this, nPhase));
    }

    /**
	 *	notifies a cell change
	 */
    protected void notifyCellChange(int nTrack, int nPhase) {
        fireModelEvent(new CellChangeEvent(this, nTrack, nPhase));
    }

    /** this class contains the information of a single cell in the cell array
	 */
    public class Cell {

        private FloatSampleBuffer m_audioData;

        public Cell() {
            m_audioData = null;
        }

        public void setAudioData(FloatSampleBuffer fsb) {
            m_audioData = fsb;
        }

        public FloatSampleBuffer getAudioData() {
            if (Debug.getTraceStyle()) {
                Debug.out("Style.Cell.getAudioData(): begin");
            }
            if (Debug.getTraceStyle()) {
                Debug.out("Style.Cell.getAudioData(): end");
            }
            return m_audioData;
        }

        public void writeXml(Writer writer) throws IOException {
            writer.write("<Cell>\n");
            byte[] abData = null;
            if (m_audioData != null) {
                abData = m_audioData.convertToByteArray(getAudioFormat());
            }
            if (abData != null) {
                writer.write("<AudioData\n");
                writer.write("  length=\"" + abData.length + "\" >\n");
                int nByteCount = 0;
                for (int i = 0; i < abData.length; i++) {
                    byte bHigh = (byte) ((abData[i] & 0xf0) >> 4);
                    byte bLow = (byte) (abData[i] & 0x0f);
                    writer.write((char) (bHigh > 9 ? bHigh + 'A' - 10 : bHigh + '0'));
                    writer.write((char) (bLow > 9 ? bLow + 'A' - 10 : bLow + '0'));
                    nByteCount++;
                    if (nByteCount == 16) {
                        writer.write("\n");
                        nByteCount = 0;
                    }
                }
                writer.write("\n");
                writer.write("</AudioData>\n");
            }
            writer.write("</Cell>\n");
        }
    }

    /** this class contains the information about a track (head)
	 */
    public class Track {

        private String m_strTitle;

        private String m_strPeer;

        private boolean m_bMute;

        private boolean m_bSolo;

        private boolean m_bMuteBySolo;

        private double m_dGain;

        private double m_dPan;

        public Track() {
            m_strTitle = "";
            m_strPeer = "";
            m_bMute = false;
            m_bSolo = false;
            m_bMuteBySolo = false;
            m_dGain = 0.0;
            m_dPan = 0.0;
        }

        public String getTitle() {
            return m_strTitle;
        }

        public void setTitle(String strTitle) {
            m_strTitle = strTitle;
        }

        public String getPeer() {
            return m_strPeer;
        }

        public void setPeer(String strPeer) {
            m_strPeer = strPeer;
        }

        public boolean getMute() {
            return m_bMute;
        }

        public void setMute(boolean bMute) {
            m_bMute = bMute;
        }

        public boolean getSolo() {
            return m_bSolo;
        }

        public void setSolo(boolean bSolo) {
            m_bSolo = bSolo;
        }

        public boolean getMuteBySolo() {
            return m_bMuteBySolo;
        }

        public void setMuteBySolo(boolean bMuteBySolo) {
            m_bMuteBySolo = bMuteBySolo;
        }

        public boolean getActuallyMuted() {
            return m_bMute || m_bMuteBySolo;
        }

        public double getGain() {
            return m_dGain;
        }

        public void setGain(double dGain) {
            m_dGain = dGain;
        }

        public double getPan() {
            return m_dPan;
        }

        public void setPan(double dPan) {
            m_dPan = dPan;
        }

        public void writeXml(Writer writer) throws IOException {
            writer.write("<Track\n");
            if (getTitle() != null) {
                writer.write("title=\"" + getTitle() + "\"\n");
            }
            if (getPeer() != null) {
                writer.write("peer=\"" + getPeer() + "\"\n");
            }
            writer.write("gain=\"" + getGain() + "\"\n");
            writer.write("pan=\"" + getPan() + "\"\n");
            writer.write("mute=\"" + getMute() + "\"\n");
            writer.write("solo=\"" + getSolo() + "\" />\n");
        }
    }

    /** this class contains the information about a phase (head)
	 */
    public class Phase {

        private int m_nMeasures;

        private int m_nRepeat;

        private int m_nBeatsPerMeasure;

        public Phase() {
        }

        public int getMeasures() {
            return m_nMeasures;
        }

        public void setMeasures(int m) {
            m_nMeasures = m;
        }

        public int getRepeat() {
            return m_nRepeat;
        }

        public void setRepeat(int r) {
            m_nRepeat = r;
        }

        public int getBeatsPerMeasure() {
            return m_nBeatsPerMeasure;
        }

        public void setBeatsPerMeasure(int bpme) {
            m_nBeatsPerMeasure = bpme;
        }

        public void writeXml(Writer writer) throws IOException {
            writer.write("<Phase\n");
            writer.write("measures=\"" + getMeasures() + "\"\n");
            writer.write("repeat=\"" + getRepeat() + "\" />\n");
            writer.write("beatsPerMeasure=\"" + getBeatsPerMeasure() + "\" />\n");
        }
    }
}
