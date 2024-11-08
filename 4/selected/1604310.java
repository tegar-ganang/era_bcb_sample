package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Gradients are used for interpolation between at least two colors defined by
 * control points. This structure contains the control points of the gradient
 * (see <code>GradRecord</code> for more details on control points).
 *
 * @see GradRecord
 */
public class Gradient implements Serializable {

    /** TODO: Comments */
    public static final byte SPREAD_PAD = 0;

    /** TODO: Comments */
    public static final byte SPREAD_REFLECT = 1;

    /** TODO: Comments */
    public static final byte SPREAD_REPEAT = 2;

    /** TODO: Comments */
    public static final byte INTERPOLATION_RGB = 0;

    /** TODO: Comments */
    public static final byte INTERPOLATION_LINEAR_RGB = 1;

    private GradRecord[] gradientRecords;

    private byte spreadMethod;

    private byte interpolationMethod;

    /**
   * Creates a new Gradient instance. Supply at least two gradient control
   * points as gradient record array.
   *
   * @param gradientRecords gradient control points
   */
    public Gradient(GradRecord[] gradientRecords) {
        this.gradientRecords = gradientRecords;
    }

    Gradient(InputBitStream stream, boolean hasAlpha) throws IOException {
        spreadMethod = (byte) stream.readUnsignedBits(2);
        interpolationMethod = (byte) stream.readUnsignedBits(2);
        short count = (short) stream.readUnsignedBits(4);
        gradientRecords = new GradRecord[count];
        for (int i = 0; i < count; i++) {
            gradientRecords[i] = new GradRecord(stream, hasAlpha);
        }
    }

    /**
   * TODO: Comments
   *
   * @param gradientRecords TODO: Comments
   */
    public void setGradientRecords(GradRecord[] gradientRecords) {
        this.gradientRecords = gradientRecords;
    }

    /**
   * Returns the gradient's control points.
   *
   * @return gradient control points (as <code>GradRecord</code> array)
   */
    public GradRecord[] getGradientRecords() {
        return gradientRecords;
    }

    /**
   * TODO: Comments
   *
   * @param interpolationMethod TODO: Comments
   */
    public void setInterpolationMethod(byte interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
    public byte getInterpolationMethod() {
        return interpolationMethod;
    }

    /**
   * TODO: Comments
   *
   * @param spreadMethod TODO: Comments
   */
    public void setSpreadMethod(byte spreadMethod) {
        this.spreadMethod = spreadMethod;
    }

    /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
    public byte getSpreadMethod() {
        return spreadMethod;
    }

    void write(OutputBitStream stream) throws IOException {
        stream.writeUnsignedBits(spreadMethod, 2);
        stream.writeUnsignedBits(interpolationMethod, 2);
        int count = gradientRecords.length;
        stream.writeUnsignedBits(count, 4);
        for (int i = 0; i < count; i++) {
            gradientRecords[i].write(stream);
        }
    }
}
