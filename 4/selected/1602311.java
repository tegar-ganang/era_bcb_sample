package org.dcm4chex.wado.mbean.ecg;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.wado.mbean.ecg.xml.SVGCreator;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WaveformGroup {

    private int grpIndex;

    private int nrOfChannels;

    private int nrOfSamples;

    private int bitsAlloc;

    private float sampleFreq;

    private String muxGrpLabel;

    private String sampleInterpretation;

    private WaveFormChannel[] channels = null;

    private ByteBuffer data = null;

    private String cuid;

    private static Logger log = Logger.getLogger(SVGCreator.class.getName());

    /**
	 * @param elem
	 */
    public WaveformGroup(String cuid, DcmElement elem, int grpIndex, float fCorr) {
        this.cuid = cuid;
        if (elem == null) throw new NullPointerException("WaveFormSequence missing!");
        Dataset ds = elem.getItem(grpIndex);
        this.grpIndex = grpIndex;
        nrOfChannels = ds.getInt(Tags.NumberOfWaveformChannels, 0);
        nrOfSamples = ds.getInt(Tags.NumberOfWaveformSamples, 0);
        sampleFreq = ds.getFloat(Tags.SamplingFrequency, 0f);
        muxGrpLabel = ds.getString(Tags.MultiplexGroupLabel);
        bitsAlloc = ds.getInt(Tags.WaveformBitsAllocated, 0);
        sampleInterpretation = ds.getString(Tags.WaveformSampleInterpretation);
        data = ds.getByteBuffer(Tags.WaveformData);
        if (nrOfSamples < 1) {
            int nrOfSamples_recalc = data.limit() / nrOfChannels;
            if (bitsAlloc > 8) {
                nrOfSamples_recalc /= bitsAlloc / 8;
            }
            log.warn("NumberOfWaveformSamples (" + nrOfSamples + ") not valid! Recalc with WaveformData:" + nrOfSamples_recalc);
            nrOfSamples = nrOfSamples_recalc;
            if (log.isDebugEnabled()) log.debug("Recalculated NumberOfWaveformSamples is " + nrOfSamples + "! ( WaveformData.size:" + data.limit() + " nrOfChannels:" + nrOfChannels + " bitsAlloc:" + bitsAlloc + " )");
        }
        prepareChannels(ds.get(Tags.ChannelDefinitionSeq), fCorr);
    }

    /**
	 * Returns the SOP Class UID of this waveform.
	 * 
	 * @return
	 */
    public String getCUID() {
        return cuid;
    }

    /**
	 * @return Returns the nrOfSamples.
	 */
    public int getNrOfSamples() {
        return nrOfSamples;
    }

    /**
	 * @return Returns the nrOfChannels.
	 */
    public int getNrOfChannels() {
        return nrOfChannels;
    }

    /**
	 * @return Returns the bitsAlloc.
	 */
    public int getBitsAlloc() {
        return bitsAlloc;
    }

    /**
	 * @return Returns the sampleFreq.
	 */
    public float getSampleFreq() {
        return sampleFreq;
    }

    public WaveFormChannel getChannel(int idx) {
        return channels[idx];
    }

    /**
	 * @return
	 */
    public String getFilterText() {
        if (channels[0].getLowFreq() != null) return channels[0].getLowFreq() + "-" + channels[0].getHighFreq() + " Hz"; else return "No Filter!";
    }

    /**
	 * @param element
	 */
    private void prepareChannels(DcmElement chDefs, float fCorr) {
        int len = chDefs.countItems();
        channels = new WaveFormChannel[len];
        WaveFormChannel ch;
        for (int i = 0; i < len; i++) {
            ch = new WaveFormChannel(this, chDefs.getItem(i), getWaveFormBuffer(i), fCorr);
            channels[i] = ch;
        }
    }

    /**
	 * @param i
	 * @return
	 */
    private WaveFormBuffer getWaveFormBuffer(int idx) {
        if (this.bitsAlloc == 8) {
            return new WaveForm8Buffer(data, idx, nrOfChannels, sampleInterpretation);
        } else if (bitsAlloc == 16) {
            return new WaveForm16Buffer(data, idx, nrOfChannels, sampleInterpretation);
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("WaveFormGroup(").append(grpIndex).append("):").append(muxGrpLabel);
        sb.append(" channels:").append(nrOfChannels).append(" samples:").append(nrOfSamples);
        sb.append(" sampleFreq:").append(sampleFreq).append(" channelDefs:").append(channels);
        return sb.toString();
    }
}
