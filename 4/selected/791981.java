package it.crs4.seal.common;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

public class SequencedFragment implements Writable {

    public static enum BaseQualityEncoding {

        Illumina, Sanger
    }

    ;

    protected Text sequence = new Text();

    protected Text quality = new Text();

    protected String instrument;

    protected Integer runNumber;

    protected String flowcellId;

    protected Integer lane;

    protected Integer tile;

    protected Integer xpos;

    protected Integer ypos;

    protected Integer read;

    protected Boolean filterPassed;

    protected Integer controlNumber;

    protected String indexSequence;

    protected static final int Instrument_Present = 0x0001;

    protected static final int RunNumber_Present = 0x0002;

    protected static final int FlowcellId_Present = 0x0004;

    protected static final int Lane_Present = 0x0008;

    protected static final int Tile_Present = 0x0010;

    protected static final int Xpos_Present = 0x0020;

    protected static final int Ypos_Present = 0x0040;

    protected static final int Read_Present = 0x0080;

    protected static final int FilterPassed_Present = 0x0100;

    protected static final int ControlNumber_Present = 0x0200;

    protected static final int IndexSequence_Present = 0x0400;

    public void clear() {
        sequence.clear();
        quality.clear();
        instrument = null;
        runNumber = null;
        flowcellId = null;
        lane = null;
        tile = null;
        xpos = null;
        ypos = null;
        read = null;
        filterPassed = null;
        controlNumber = null;
        indexSequence = null;
    }

    /**
	 * Get sequence Text object.
	 * Trade encapsulation for efficiency.  Here we expose the internal Text
	 * object so that data may be read and written diretly from/to it.
	 */
    public Text getSequence() {
        return sequence;
    }

    /**
	 * Get quality Text object.
	 * Trade encapsulation for efficiency.  Here we expose the internal Text
	 * object so that data may be read and written diretly from/to it.
	 */
    public Text getQuality() {
        return quality;
    }

    public void setInstrument(String v) {
        instrument = v;
    }

    public void setRunNumber(Integer v) {
        runNumber = v;
    }

    public void setFlowcellId(String v) {
        flowcellId = v;
    }

    public void setLane(Integer v) {
        lane = v;
    }

    public void setTile(Integer v) {
        tile = v;
    }

    public void setXpos(Integer v) {
        xpos = v;
    }

    public void setYpos(Integer v) {
        ypos = v;
    }

    public void setRead(Integer v) {
        read = v;
    }

    public void setFilterPassed(Boolean v) {
        filterPassed = v;
    }

    public void setControlNumber(Integer v) {
        controlNumber = v;
    }

    public void setIndexSequence(String v) {
        indexSequence = v;
    }

    public void setSequence(Text seq) {
        if (seq == null) throw new IllegalArgumentException("can't have a null sequence");
        sequence = seq;
    }

    /**
	 * Set quality.  Quality should be encoded in Sanger Phred+33 format.
	 */
    public void setQuality(Text qual) {
        if (qual == null) throw new IllegalArgumentException("can't have a null quality");
        quality = qual;
    }

    public String getInstrument() {
        return instrument;
    }

    public Integer getRunNumber() {
        return runNumber;
    }

    public String getFlowcellId() {
        return flowcellId;
    }

    public Integer getLane() {
        return lane;
    }

    public Integer getTile() {
        return tile;
    }

    public Integer getXpos() {
        return xpos;
    }

    public Integer getYpos() {
        return ypos;
    }

    public Integer getRead() {
        return read;
    }

    public Boolean getFilterPassed() {
        return filterPassed;
    }

    public Integer getControlNumber() {
        return controlNumber;
    }

    public String getIndexSequence() {
        return indexSequence;
    }

    /**
	 * Recreates a pseudo qseq record with the fields available.
	 */
    public String toString() {
        String delim = "\t";
        StringBuilder builder = new StringBuilder(800);
        builder.append(instrument).append(delim);
        builder.append(runNumber).append(delim);
        builder.append(flowcellId).append(delim);
        builder.append(lane).append(delim);
        builder.append(tile).append(delim);
        builder.append(xpos).append(delim);
        builder.append(ypos).append(delim);
        builder.append(indexSequence).append(delim);
        builder.append(read).append(delim);
        builder.append(sequence).append(delim);
        builder.append(quality).append(delim);
        builder.append((filterPassed == null || filterPassed) ? 1 : 0);
        return builder.toString();
    }

    public boolean equals(Object other) {
        if (other != null && other instanceof SequencedFragment) {
            SequencedFragment otherFrag = (SequencedFragment) other;
            if (instrument == null && otherFrag.instrument != null || instrument != null && !instrument.equals(otherFrag.instrument)) return false;
            if (runNumber == null && otherFrag.runNumber != null || runNumber != null && !runNumber.equals(otherFrag.runNumber)) return false;
            if (flowcellId == null && otherFrag.flowcellId != null || flowcellId != null && !flowcellId.equals(otherFrag.flowcellId)) return false;
            if (lane == null && otherFrag.lane != null || lane != null && !lane.equals(otherFrag.lane)) return false;
            if (tile == null && otherFrag.tile != null || tile != null && !tile.equals(otherFrag.tile)) return false;
            if (xpos == null && otherFrag.xpos != null || xpos != null && !xpos.equals(otherFrag.xpos)) return false;
            if (ypos == null && otherFrag.ypos != null || ypos != null && !ypos.equals(otherFrag.ypos)) return false;
            if (read == null && otherFrag.read != null || read != null && !read.equals(otherFrag.read)) return false;
            if (filterPassed == null && otherFrag.filterPassed != null || filterPassed != null && !filterPassed.equals(otherFrag.filterPassed)) return false;
            if (controlNumber == null && otherFrag.controlNumber != null || controlNumber != null && !controlNumber.equals(otherFrag.controlNumber)) return false;
            if (indexSequence == null && otherFrag.indexSequence != null || indexSequence != null && !indexSequence.equals(otherFrag.indexSequence)) return false;
            if (!sequence.equals(otherFrag.sequence)) return false;
            if (!quality.equals(otherFrag.quality)) return false;
            return true;
        } else return false;
    }

    /**
	 * Convert quality scores in-place.
	 *
	 * @raise FormatException if quality scores are out of the range
	 * allowed by the current encoding.
	 * @raise IllegalArgumentException if current and  target quality encodings are the same.
	 */
    public static void convertQuality(Text quality, BaseQualityEncoding current, BaseQualityEncoding target) {
        if (current == target) throw new IllegalArgumentException("current and target quality encodinds are the same (" + current + ")");
        byte[] bytes = quality.getBytes();
        final int len = quality.getLength();
        final int illuminaSangerDistance = Utils.ILLUMINA_OFFSET - Utils.SANGER_OFFSET;
        if (current == BaseQualityEncoding.Illumina && target == BaseQualityEncoding.Sanger) {
            for (int i = 0; i < len; ++i) {
                if (bytes[i] < Utils.ILLUMINA_OFFSET || bytes[i] > (Utils.ILLUMINA_OFFSET + Utils.ILLUMINA_MAX)) {
                    throw new FormatException("base quality score out of range for Illumina Phred+64 format (found " + (bytes[i] - Utils.ILLUMINA_OFFSET) + " but acceptable range is [0," + Utils.ILLUMINA_MAX + "].\n" + "Maybe qualities are encoded in Sanger format?\n");
                }
                bytes[i] -= illuminaSangerDistance;
            }
        } else if (current == BaseQualityEncoding.Sanger && target == BaseQualityEncoding.Illumina) {
            for (int i = 0; i < len; ++i) {
                if (bytes[i] < Utils.SANGER_OFFSET || bytes[i] > (Utils.SANGER_OFFSET + Utils.SANGER_MAX)) {
                    throw new FormatException("base quality score out of range for Sanger Phred+64 format (found " + (bytes[i] - Utils.SANGER_OFFSET) + " but acceptable range is [0," + Utils.SANGER_MAX + "].\n" + "Maybe qualities are encoded in Illumina format?\n");
                }
                bytes[i] += illuminaSangerDistance;
            }
        } else throw new IllegalArgumentException("unsupported BaseQualityEncoding transformation from " + current + " to " + target);
    }

    /**
	 * Verify that the given quality bytes are within the range allowed for the specified encoding.
	 *
	 * In theory, the Sanger encoding uses the entire
	 * range of characters from ASCII 33 to 126, giving a value range of [0,93].  However, values over 60 are
	 * unlikely in practice, and are more likely to be caused by mistaking a file that uses Illumina encoding
	 * for Sanger.  So, we'll enforce the same range supported by Illumina encoding ([0,62]) for Sanger.
	 *
	 * @return -1 if quality is ok.
	 * @return If an out-of-range value is found the index of the value is returned.
	 */
    public static int verifyQuality(Text quality, BaseQualityEncoding encoding) {
        int max, min;
        if (encoding == BaseQualityEncoding.Illumina) {
            max = Utils.ILLUMINA_OFFSET + Utils.ILLUMINA_MAX;
            min = Utils.ILLUMINA_OFFSET;
        } else if (encoding == BaseQualityEncoding.Sanger) {
            max = Utils.SANGER_OFFSET + Utils.SANGER_MAX;
            min = Utils.SANGER_OFFSET;
        } else throw new IllegalArgumentException("Unsupported base encoding quality " + encoding);
        final byte[] bytes = quality.getBytes();
        final int len = quality.getLength();
        for (int i = 0; i < len; ++i) {
            if (bytes[i] < min || bytes[i] > max) return i;
        }
        return -1;
    }

    public void readFields(DataInput in) throws IOException {
        this.clear();
        sequence.readFields(in);
        quality.readFields(in);
        int presentFlags = WritableUtils.readVInt(in);
        if ((presentFlags & Instrument_Present) != 0) instrument = WritableUtils.readString(in);
        if ((presentFlags & RunNumber_Present) != 0) runNumber = WritableUtils.readVInt(in);
        if ((presentFlags & FlowcellId_Present) != 0) flowcellId = WritableUtils.readString(in);
        if ((presentFlags & Lane_Present) != 0) lane = WritableUtils.readVInt(in);
        if ((presentFlags & Tile_Present) != 0) tile = WritableUtils.readVInt(in);
        if ((presentFlags & Xpos_Present) != 0) xpos = WritableUtils.readVInt(in);
        if ((presentFlags & Ypos_Present) != 0) ypos = WritableUtils.readVInt(in);
        if ((presentFlags & Read_Present) != 0) read = WritableUtils.readVInt(in);
        if ((presentFlags & FilterPassed_Present) != 0) filterPassed = WritableUtils.readVInt(in) == 1;
        if ((presentFlags & ControlNumber_Present) != 0) controlNumber = WritableUtils.readVInt(in);
        if ((presentFlags & IndexSequence_Present) != 0) indexSequence = WritableUtils.readString(in);
    }

    public void write(DataOutput out) throws IOException {
        sequence.write(out);
        quality.write(out);
        int presentFlags = 0;
        if (instrument != null) presentFlags |= Instrument_Present;
        if (runNumber != null) presentFlags |= RunNumber_Present;
        if (flowcellId != null) presentFlags |= FlowcellId_Present;
        if (lane != null) presentFlags |= Lane_Present;
        if (tile != null) presentFlags |= Tile_Present;
        if (xpos != null) presentFlags |= Xpos_Present;
        if (ypos != null) presentFlags |= Ypos_Present;
        if (read != null) presentFlags |= Read_Present;
        if (filterPassed != null) presentFlags |= FilterPassed_Present;
        if (controlNumber != null) presentFlags |= ControlNumber_Present;
        if (indexSequence != null) presentFlags |= IndexSequence_Present;
        WritableUtils.writeVInt(out, presentFlags);
        if (instrument != null) WritableUtils.writeString(out, instrument);
        if (runNumber != null) WritableUtils.writeVInt(out, runNumber);
        if (flowcellId != null) WritableUtils.writeString(out, flowcellId);
        if (lane != null) WritableUtils.writeVInt(out, lane);
        if (tile != null) WritableUtils.writeVInt(out, tile);
        if (xpos != null) WritableUtils.writeVInt(out, xpos);
        if (ypos != null) WritableUtils.writeVInt(out, ypos);
        if (read != null) WritableUtils.writeVInt(out, read);
        if (filterPassed != null) WritableUtils.writeVInt(out, filterPassed ? 1 : 0);
        if (controlNumber != null) WritableUtils.writeVInt(out, controlNumber);
        if (indexSequence != null) WritableUtils.writeString(out, indexSequence);
    }
}
