package tw.edu.shu.im.iccio.tagtype;

import tw.edu.shu.im.iccio.datatype.Signature;
import tw.edu.shu.im.iccio.datatype.UInt32Number;
import tw.edu.shu.im.iccio.datatype.XYZNumber;
import tw.edu.shu.im.iccio.datatype.Response16Number;
import tw.edu.shu.im.iccio.Streamable;
import tw.edu.shu.im.iccio.ICCUtils;
import tw.edu.shu.im.iccio.ICCProfileException;

/**
 * CurveStructure is a class used by ResponseCurveSet16Type to define a count response curve structure.
 *
 * @see ResponseCurveSet16Type for more description.
 *
 * <code>
 * 0..3		4 	measurement unit signature see Table 52 below
 * 4..m 		number of measurements for each channel:
 *			This is an array with one entry for each channel.
 *			Each array element specifies the count of
 *			measurements for each channel.			uInt32Number[...]
 * m+1..n 		number-of-channels measurements of patch
 *			with the maximum colorant value. PCS values
 *			shall be relative colorimetric. 		XYZNumber[...]
 * n+1..p 		number-of-channels response arrays. Each
 *			array contains number-of-measurements
 *			response16Numbers appropriate to the
 *			channel. The arrays shall be ordered in the
 *			channel order specified in Table 31 for the
 *			appropriate colour space.			response16Number[...]
 * </code>
 */
public class CurveStructure implements Streamable {

    private Signature measureUnitSignature_;

    private UInt32Number[] channelMeasures_;

    private XYZNumber[] patchMeasures_;

    private Response16Number[] responseArray_;

    public CurveStructure() {
    }

    /**
	 * Construct a CurveStructure object by parsing a byte array and make CurveStructure object.
	 * @param byteArray - byte array to parse
	 * @param offset - starting position in the array to parse
	 * @param len - NUMBER OF CHANNELS from ResponseCurveSet16Type dataset.
	 */
    public CurveStructure(byte[] byteArray, int offset, int len) throws ICCProfileException {
        fromByteArray(byteArray, offset, len);
    }

    /**
	 * Parse a byte array and make CurveStructure object.
	 * @param byteArray - byte array to parse
	 * @param offset - starting position in the array to parse
	 * @param channels - NUMBER OF CHANNELS from ResponseCurveSet16Type dataset.
	 */
    public void fromByteArray(byte[] byteArray, int offset, int channels) throws ICCProfileException {
        if (byteArray == null) throw new ICCProfileException("byte array null", ICCProfileException.NullPointerException);
        if (offset < 0) throw new ICCProfileException("index out of range", ICCProfileException.IndexOutOfBoundsException);
        if (channels <= 0) throw new ICCProfileException("channels <= 0", ICCProfileException.WrongSizeException);
        this.measureUnitSignature_ = new Signature(byteArray, offset);
        this.channelMeasures_ = new UInt32Number[channels];
        int idx = offset + 4;
        for (int i = 0; i < channels; i++) {
            this.channelMeasures_[i] = new UInt32Number(byteArray, idx);
            idx += UInt32Number.SIZE;
        }
        this.patchMeasures_ = new XYZNumber[channels];
        for (int i = 0; i < channels; i++) {
            this.patchMeasures_[i] = new XYZNumber(byteArray, idx);
            idx += XYZNumber.SIZE;
        }
        this.responseArray_ = new Response16Number[channels];
        for (int i = 0; i < channels; i++) {
            this.responseArray_[i] = new Response16Number(byteArray, idx);
            idx += Response16Number.SIZE;
        }
    }

    public byte[] toByteArray() throws ICCProfileException {
        if (this.measureUnitSignature_ == null) throw new ICCProfileException("data not set", ICCProfileException.InvalidDataValueException);
        int channels = this.channelMeasures_.length;
        if (this.patchMeasures_.length != channels || this.responseArray_.length != channels) throw new ICCProfileException("not same number of channels", ICCProfileException.InvalidDataValueException);
        int len = 4 + channels * (UInt32Number.SIZE + XYZNumber.SIZE + Response16Number.SIZE);
        byte[] all = new byte[len];
        ICCUtils.appendByteArray(all, 0, this.measureUnitSignature_);
        int idx = 4;
        for (int i = 0; i < channels; i++) {
            ICCUtils.appendByteArray(all, idx, this.channelMeasures_[i]);
            idx += UInt32Number.SIZE;
        }
        for (int i = 0; i < channels; i++) {
            ICCUtils.appendByteArray(all, idx, this.patchMeasures_[i]);
            idx += XYZNumber.SIZE;
        }
        for (int i = 0; i < channels; i++) {
            ICCUtils.appendByteArray(all, idx, this.responseArray_[i]);
            idx += Response16Number.SIZE;
        }
        return all;
    }

    public int size() {
        int channels = this.channelMeasures_.length;
        return 4 + channels * (UInt32Number.SIZE + XYZNumber.SIZE + Response16Number.SIZE);
    }

    public Signature getMeasureUnitSignature() {
        return this.measureUnitSignature_;
    }

    public void setMeasureUnitSignature(Signature sig) {
        this.measureUnitSignature_ = sig;
    }

    public UInt32Number[] getChannelMeasures() {
        return this.channelMeasures_;
    }

    public void setChannelMeasures(UInt32Number[] data) {
        this.channelMeasures_ = data;
    }

    public XYZNumber[] getPatchMeasures() {
        return this.patchMeasures_;
    }

    public void setPatchMeasures(XYZNumber[] data) {
        this.patchMeasures_ = data;
    }

    public Response16Number[] getResponseArray() {
        return this.responseArray_;
    }

    public void setResponseArray(Response16Number[] data) {
        this.responseArray_ = data;
    }

    /**
	 * Return XML element of this object.
	 * @param name - attribute name on element
	 * @return XML fragment as a string
	 */
    public String toXmlString(String name) {
        StringBuffer sb = new StringBuffer();
        if (name == null || name.length() < 1) sb.append("<curveStructure>"); else sb.append("<curveStructure name=\"" + name + "\">");
        sb.append(measureUnitSignature_.toXmlString());
        sb.append("<array name=\"channel_measures\" dims=\"1\"><dim index=\"0\">");
        for (int i = 0; i < channelMeasures_.length; i++) {
            sb.append(channelMeasures_[i].toXmlString());
        }
        sb.append("</dim></array>");
        sb.append("<array name=\"patch measures\" dims=\"1\"><dim index=\"0\">");
        for (int i = 0; i < patchMeasures_.length; i++) {
            sb.append(patchMeasures_[i].toXmlString());
        }
        sb.append("</dim></array>");
        sb.append("<array name=\"response array\" dims=\"1\"><dim index=\"0\">");
        for (int i = 0; i < responseArray_.length; i++) {
            sb.append(responseArray_[i].toXmlString());
        }
        sb.append("</dim></array>");
        sb.append("</curveStructure>");
        return sb.toString();
    }

    public String toXmlString() {
        return toXmlString(null);
    }
}
