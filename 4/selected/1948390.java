package net.sourceforge.entrainer.eeg.core.signalprocessing;

import static net.sourceforge.entrainer.eeg.utils.EEGUtils.mean;
import static net.sourceforge.entrainer.eeg.utils.EEGUtils.standardDeviation;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import net.sourceforge.entrainer.eeg.core.AbstractEEGSignalProcessor;
import net.sourceforge.entrainer.eeg.core.EEGSignalProcessor;

/**
 * This implementation of {@link AbstractEEGSignalProcessor} calculates the mean
 * and standard deviation of the calibration values. Applying the calibration
 * results in zero equaling the (mean - standard deviation), with the mean
 * equaling 10% of the signal.
 * 
 * @author burton
 */
public class StandardDeviationSignalProcessor extends AbstractEEGSignalProcessor {

    private static final long serialVersionUID = 2141123412029231038L;

    private double mean;

    private double standardDeviation;

    public StandardDeviationSignalProcessor() {
        super();
    }

    public String getDescription() {
        return "Standard Deviation Signal Processor";
    }

    public double applyCalibration(double channelSignal) {
        if (getStandardDeviation() == 0) {
            return channelSignal;
        }
        MathContext mc = new MathContext(5, RoundingMode.HALF_UP);
        BigDecimal standardDeviation = new BigDecimal(getStandardDeviation(), mc);
        BigDecimal mean = new BigDecimal(getMean(), mc);
        BigDecimal lower = mean.subtract(standardDeviation);
        BigDecimal strength = new BigDecimal(channelSignal, mc);
        BigDecimal signal = strength.subtract(lower);
        return signal.divide(standardDeviation, mc).divide(new BigDecimal(10, mc)).doubleValue();
    }

    public void clearCalibration() {
        setMean(0);
        setStandardDeviation(0);
    }

    @Override
    protected void calculateCalibrationImpl() {
        double[] d = new double[getValuesForCalibration().size()];
        for (int i = 0; i < getValuesForCalibration().size(); i++) {
            d[i] = getValuesForCalibration().get(i).getChannelStrength();
        }
        setMean(mean(d));
        setStandardDeviation(standardDeviation(d));
    }

    protected double getMean() {
        return mean;
    }

    protected void setMean(double mean) {
        this.mean = mean;
    }

    protected double getStandardDeviation() {
        return standardDeviation;
    }

    protected void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public EEGSignalProcessor getNewSignalProcessor() {
        return new StandardDeviationSignalProcessor();
    }
}
