package shu.cms.lcd;

import java.util.*;
import shu.cms.colorspace.depend.*;
import shu.cms.util.*;
import shu.math.array.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class AlterableDisplayLUT extends DisplayLUT {

    public boolean addOutputValue(RGBBase.Channel ch, int index, double addValue) {
        checkSync();
        canUndo = false;
        undoChannel = ch;
        undoIndex = index;
        undoAddValue = addValue;
        if (ch == RGBBase.Channel.W) {
            double[] results = new double[3];
            results[0] = rgbOutput[0][index] + addValue;
            results[1] = rgbOutput[1][index] + addValue;
            results[2] = rgbOutput[2][index] + addValue;
            for (int x = 0; x < 3; x++) {
                if (results[x] > 255 || results[x] < 0) {
                    return false;
                }
            }
            for (int x = 0; x < 3; x++) {
                rgbOutput[x][index] = results[x];
                outputRGBArray[index].setValue(ch.getChannelByArrayIndex(x), results[x], RGB.MaxValue.Double255);
            }
        } else {
            double result = rgbOutput[ch.getArrayIndex()][index] + addValue;
            if (result > 255 || result < 0) {
                return false;
            }
            rgbOutput[ch.getArrayIndex()][index] = result;
            outputRGBArray[index].setValue(ch, result, RGB.MaxValue.Double255);
            canUndo = true;
        }
        return true;
    }

    /**
     * ��^�W�@�����ץ�
     * @return boolean
     */
    public boolean undo() {
        if (canUndo) {
            if (!addOutputValue(undoChannel, undoIndex, -undoAddValue)) {
                canUndo = false;
                return false;
            }
            canUndo = false;
            return true;
        } else {
            return false;
        }
    }

    private double undoAddValue;

    private RGBBase.Channel undoChannel;

    private int undoIndex;

    private boolean canUndo = false;

    public boolean minusOutputValue(RGBBase.Channel ch, int index, double addValue) {
        return addOutputValue(ch, index, -addValue);
    }

    private double[][] memoryRGBOutput;

    private RGB[] memoryOutputRGBArray;

    /**
     * �N��Ӫ�{�p�O�а_��
     * LUT -> memory
     */
    public void storeToMemory() {
        memoryRGBOutput = DoubleArray.copy(rgbOutput);
        memoryOutputRGBArray = RGBArray.deepClone(outputRGBArray);
    }

    /**
     * �q�O�м��X�åB��J���Ӫ�
     * memory -> LUT
     */
    public void loadFromMemory() {
        if (memoryRGBOutput == null || memoryOutputRGBArray == null) {
            return;
        }
        int rgbOutputSize = rgbOutput.length;
        for (int x = 0; x < rgbOutputSize; x++) {
            DoubleArray.copy(memoryRGBOutput[x], rgbOutput[x]);
        }
        outputRGBArray = RGBArray.deepClone(memoryOutputRGBArray);
    }

    protected void checkSync() {
        if (!isSync()) {
            throw new IllegalStateException("rgbOutput and outputRGBArray is not sync!");
        }
    }

    /**
     * rgbOutput�PoutputRGBArray�O�_�P�B
     * @return boolean
     */
    public boolean isSync() {
        int size = outputRGBArray.length;
        for (int x = 0; x < size; x++) {
            double[] values = new double[] { rgbOutput[0][x], rgbOutput[1][x], rgbOutput[2][x] };
            RGB rgb = outputRGBArray[x];
            double[] rgbValues = rgb.getValues();
            boolean eq = Arrays.equals(values, rgbValues);
            if (!eq) {
                return false;
            }
        }
        return true;
    }

    protected void printLut() {
        super.printLut();
        if (!isSync()) {
            System.out.println("rgbOutput and outputRGBArray is not sync!");
        }
    }
}
