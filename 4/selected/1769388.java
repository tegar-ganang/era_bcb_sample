package org.dcm4cheri.srom;

import org.dcm4che.srom.*;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import java.util.Date;

/**
 *
 * @author  gunter.zeilinger@tiani.com
 * @version 1.0
 */
class WaveformContentImpl extends CompositeContentImpl implements WaveformContent {

    private static final int[] NULL_CHANNELNUMBER = {};

    protected int[] channelNumbers;

    WaveformContentImpl(KeyObject owner, Date obsDateTime, Template template, Code name, RefSOP refSOP, int[] channelNumbers) {
        super(owner, obsDateTime, template, name, refSOP);
        setChannelNumbers(channelNumbers);
    }

    Content clone(KeyObject newOwner, boolean inheritObsDateTime) {
        return new WaveformContentImpl(newOwner, getObservationDateTime(inheritObsDateTime), template, name, refSOP, channelNumbers);
    }

    public String toString() {
        StringBuffer sb = prompt().append(refSOP);
        for (int i = 0; i < channelNumbers.length; ++i) sb.append(",[").append(channelNumbers[0]).append("]");
        return sb.append(')').toString();
    }

    public final ValueType getValueType() {
        return ValueType.WAVEFORM;
    }

    public final int[] getChannelNumbers() {
        return (int[]) channelNumbers.clone();
    }

    public final void setChannelNumbers(int[] channelNumbers) {
        if (channelNumbers != null) {
            if ((channelNumbers.length & 1) != 0) {
                throw new IllegalArgumentException("L=" + channelNumbers.length);
            }
            this.channelNumbers = (int[]) channelNumbers.clone();
        } else {
            this.channelNumbers = NULL_CHANNELNUMBER;
        }
    }

    public void toDataset(Dataset ds) {
        super.toDataset(ds);
        if (channelNumbers.length != 0) {
            ds.get(Tags.RefSOPSeq).getItem().putUS(Tags.RefWaveformChannels, channelNumbers);
        }
    }
}
