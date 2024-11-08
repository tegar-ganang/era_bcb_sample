package net.sourceforge.olduvai.lrac.dataservice.records;

import java.text.SimpleDateFormat;
import net.sourceforge.olduvai.lrac.drawer.strips.StripChannelGroup;

/**
 * This is a set of channel data values, ordered by timestamp.  
 * This object is contained by a SourceChartDataResultSet which contains data as to 
 * which source the data belongs to.  
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class ChannelValueSet {

    public static final int STATTYPE = SourceChartDataResultSet.STATTYPE;

    public static final int ALARMTYPE = SourceChartDataResultSet.ALARMTYPE;

    public static final int AVGTYPE = SourceChartDataResultSet.AVGTYPE;

    public String channelName;

    public String label;

    public String unit;

    public int type;

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

    double[] timeStamps;

    double[] dataValues;

    double minDataValue = Double.POSITIVE_INFINITY;

    double maxDataValue = Double.NEGATIVE_INFINITY;

    public ChannelValueSet(String channelName, String unit, String label, int type) {
        this.channelName = channelName;
        this.label = label;
        this.unit = unit;
        this.type = type;
    }

    public void addData(String[] valuesArray) {
        final int size = (valuesArray.length - SourceChartDataResultSet.FIRSTDATAVALUEINDEX) / 2;
        int oldSize = 0;
        if (timeStamps == null) {
            timeStamps = new double[size];
            dataValues = new double[size];
        } else {
            oldSize = timeStamps.length;
            final int newSize = oldSize + size;
            double[] newTimeStamps = new double[newSize];
            double[] newDataValues = new double[newSize];
            System.arraycopy(timeStamps, 0, newTimeStamps, 0, oldSize);
            System.arraycopy(dataValues, 0, newDataValues, 0, oldSize);
            timeStamps = newTimeStamps;
            dataValues = newDataValues;
        }
        int count = oldSize;
        for (int index = SourceChartDataResultSet.FIRSTDATAVALUEINDEX; index < valuesArray.length; ) {
            try {
                timeStamps[count] = Double.parseDouble(valuesArray[index]);
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
            index++;
            try {
                dataValues[count] = Double.parseDouble(valuesArray[index]);
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
            index++;
            if (dataValues[count] > maxDataValue) maxDataValue = dataValues[count];
            if (dataValues[count] < minDataValue) minDataValue = dataValues[count];
            count++;
        }
    }

    public double[] getTimeStamps() {
        return timeStamps;
    }

    public double[] getDataValues() {
        return dataValues;
    }

    public String toString() {
        int numValues = 0;
        if (dataValues != null) numValues = dataValues.length;
        return "(" + channelName + ", " + label + ", " + unit + ", numValues: " + numValues + ", objid:" + super.toString() + ")";
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setDataValues(double[] dataValues) {
        this.dataValues = dataValues;
    }

    public void setTimeStamps(double[] timeStamps) {
        this.timeStamps = timeStamps;
    }

    public double getMaxDataValue() {
        return maxDataValue;
    }

    public double getMinDataValue() {
        return minDataValue;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    static final String TOTALSUFFIX = "_total";

    static final String MAINSUFFIX = "_main";

    /**
	 * Swift specific code that tries to be intelligent about prioritizing this channelvalueset
	 * @return
	 */
    public int getChannelPriority() {
        final String channelPrefix = StripChannelGroup.getChannelPrefix(getChannelName());
        if (channelPrefix == null) return 0;
        final String channelSuffix = StripChannelGroup.getChannelSuffix(getChannelName());
        if (channelSuffix == null) return 0;
        if (channelSuffix.contains(TOTALSUFFIX) && getType() != AVGTYPE) return 0;
        if (channelSuffix.contains(MAINSUFFIX)) return 0;
        int suffixDigits = 0;
        try {
            suffixDigits = extractFinalDigits(channelSuffix) + 1;
        } catch (NumberFormatException e) {
            suffixDigits = 5;
        }
        return suffixDigits;
    }

    /**
	 * returns the int value of all of the numbers in a string
	 * @param s
	 * @return
	 */
    static int extractFinalDigits(String s) throws NumberFormatException {
        int j = s.length() - 1;
        while (j >= 0 && Character.isDigit(s.charAt(j))) {
            j--;
        }
        return Integer.parseInt(s.substring(j + 1, s.length()));
    }
}
