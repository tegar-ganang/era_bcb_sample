package eu.haslgruebler.darsens.service.features.component.pojo;

import java.util.Vector;
import eu.haslgruebler.darsens.service.features.component.BeforeAfterShiftListener;

/**
 * @author Michael Haslgrübler, uni-michael@haslgruebler.eu
 *
 */
public class DataFeature extends Feature {

    private double data[];

    private Vector basl;

    public static final String NAME = "acc";

    public static final int ID = 0;

    public DataFeature(int winSize) {
        data = new double[winSize];
        basl = new Vector();
    }

    public DataFeature() {
        this(128);
    }

    /**
	 * @return the data
	 */
    public double[] getData() {
        return data;
    }

    public void push(double d) {
        for (int i = 0; i < basl.size(); i++) {
            ((BeforeAfterShiftListener) basl.elementAt(i)).beforeShift();
        }
        DataFeature.shift(data);
        getData()[data.length - 1] = d;
        for (int i = 0; i < basl.size(); i++) {
            ((BeforeAfterShiftListener) basl.elementAt(i)).afterShift();
        }
    }

    /**
	 * @param basl the basl to set
	 */
    public void addBasl(BeforeAfterShiftListener listener) {
        basl.addElement(listener);
    }

    public void removeBasl(BeforeAfterShiftListener listener) {
        basl.removeElement(listener);
    }

    public static double[] shift(double[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            data[i] = data[i + 1];
        }
        return data;
    }

    public static boolean[] shift(boolean[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            data[i] = data[i + 1];
        }
        return data;
    }

    public int getFeatureID() {
        return ID;
    }

    public String getFeatureName() {
        return NAME;
    }

    public double getLast() {
        return data[data.length - 1];
    }
}
