package org.mcisb.beacon.analysis;

import org.apache.commons.math.linear.*;
import java.util.*;

public class CellData {

    RealMatrixImpl cytointensity;

    RealMatrixImpl nucintensity;

    RealMatrixImpl cellintensity;

    RealMatrixImpl ratio;

    String cellName;

    String[] channelNames;

    double[] times;

    ArrayList<Double> timesArrayList;

    ArrayList<double[]> cytArrayList;

    ArrayList<double[]> nucArrayList;

    ArrayList<double[]> cellArrayList;

    ArrayList<double[]> ratioArrayList;

    boolean finalised = false;

    boolean intensity = false;

    public CellData(String[] channels, String name) {
        cellName = name;
        channelNames = channels;
        cytArrayList = new ArrayList<double[]>();
        nucArrayList = new ArrayList<double[]>();
        cellArrayList = new ArrayList<double[]>();
        ratioArrayList = new ArrayList<double[]>();
        timesArrayList = new ArrayList<Double>();
    }

    public CellData(String[] channels, double[][] cyt, double[][] nuc, double[][] cell) {
        channelNames = channels;
        cytointensity = new RealMatrixImpl(cyt);
        nucintensity = new RealMatrixImpl(nuc);
        cellintensity = new RealMatrixImpl(cell);
        calcRatios();
    }

    private void calcRatios() {
        int numberOfChannels = channelNames.length;
        double[][] tempArray = new double[cytointensity.getRowDimension()][numberOfChannels];
        for (int i = 0; i < cytointensity.getRowDimension(); i++) {
            for (int j = 0; j < numberOfChannels; j++) {
                tempArray[i][j] = nucintensity.getEntry(i, j) / cytointensity.getEntry(i, j);
            }
        }
        ratio = new RealMatrixImpl(tempArray);
        finalised = true;
    }

    public double[] getRatio(int channelNumber) throws NotFinalisedException {
        if (finalised) {
            return ratio.getColumn(channelNumber);
        } else throw new NotFinalisedException();
    }

    public double[] getWholeCellIntensity(int channelNumber) throws NotFinalisedException {
        if (finalised) {
            return cellintensity.getColumn(channelNumber);
        } else throw new NotFinalisedException();
    }

    public String getName() {
        return cellName;
    }

    public String getChannelName(int channel) {
        return channelNames[channel];
    }

    public double[] getTimes() {
        return times;
    }

    public void addCyt(double[] d) {
        cytArrayList.add(d);
    }

    public void addNuc(double[] d) {
        nucArrayList.add(d);
    }

    public void addCell(double[] d) {
        cellArrayList.add(d);
    }

    public void addRatio(double[] d) {
        ratioArrayList.add(d);
    }

    public void addTime(double d) {
        timesArrayList.add(new Double(d));
    }

    public int numberOfChannels() {
        return channelNames.length;
    }

    public void finalise() {
        double[][] cytDouble = new double[cytArrayList.size()][];
        double[][] nucDouble = new double[nucArrayList.size()][];
        times = new double[timesArrayList.size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = timesArrayList.get(i).doubleValue();
        }
        for (int i = 0; i < cytArrayList.size(); i++) {
            cytDouble[i] = (double[]) cytArrayList.get(i);
            nucDouble[i] = (double[]) nucArrayList.get(i);
        }
        cytointensity = new RealMatrixImpl(cytDouble);
        nucintensity = new RealMatrixImpl(nucDouble);
        if (cellArrayList.size() == cytArrayList.size()) {
            double[][] cellDouble = new double[cellArrayList.size()][];
            for (int i = 0; i < cellArrayList.size(); i++) {
                cellDouble[i] = (double[]) cellArrayList.get(i);
            }
            cellintensity = new RealMatrixImpl(cellDouble);
            intensity = true;
        }
        if (ratioArrayList.size() == cytArrayList.size()) {
            double[][] ratioDouble = new double[ratioArrayList.size()][];
            for (int i = 0; i < ratioArrayList.size(); i++) {
                ratioDouble[i] = (double[]) ratioArrayList.get(i);
            }
            ratio = new RealMatrixImpl(ratioDouble);
            finalised = true;
        } else {
            calcRatios();
        }
    }

    public boolean hasIntensity() {
        return intensity;
    }

    public boolean isFinalised() {
        return finalised;
    }
}
