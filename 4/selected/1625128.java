package maltcms.commands.filters.array;

import java.util.Arrays;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import cross.Logging;
import cross.tools.MathTools;

/**
 * Estimate a local baseline using a windowed median and perform baseline
 * subtraction.
 * 
 * @author Nils.Hoffmann@cebitec.uni-bielefeld.de
 * 
 */
public class MedianBaselineFilter extends AArrayFilter {

    private final Logger log = Logging.getLogger(this);

    private int scans = 0;

    private int channels = 0;

    private int median_window = 0;

    private double snr_minimum = 1.0d;

    public MedianBaselineFilter() {
        super();
    }

    @Override
    public Array apply(final Array a) {
        return apply(new Array[] { a })[0];
    }

    @Override
    public Array[] apply(final Array[] a) {
        Array[] ret = null;
        int i = 0;
        for (final Array arr : a) {
            if (arr instanceof ArrayDouble.D2) {
                if (ret == null) {
                    ret = new Array[a.length];
                }
                final ArrayDouble.D2 c = new ArrayDouble.D2(this.scans, this.channels);
                ret[i] = filterChromatogram(this.scans, this.channels, ((ArrayDouble.D2) a[i]), c, this.median_window);
                i++;
            } else {
                throw new IllegalArgumentException("Only arrays of type ArrayDouble.D2 are supported!");
            }
        }
        if (ret == null) {
            return a;
        }
        return ret;
    }

    @Override
    public void configure(final Configuration cfg) {
        this.channels = cfg.getInt(this.getClass().getName() + ".num_channels", 500);
        this.scans = cfg.getInt(this.getClass().getName() + ".num_scans", 5500);
        this.median_window = cfg.getInt(this.getClass().getName() + ".median_window", 20);
        this.snr_minimum = cfg.getDouble(this.getClass().getName() + ".snr_minimum", 6.0d);
    }

    /**
	 * @param scans1
	 * @param channels1
	 * @param a
	 * @param c
	 * @param median_window1
	 * @return filtered Chromatogram as ArrayDouble.D2
	 */
    protected ArrayDouble.D2 filterChromatogram(final int scans1, final int channels1, final ArrayDouble.D2 a, final ArrayDouble.D2 c, final int median_window1) {
        double lmedian = 0.0d;
        double lstddev = 0.0d;
        for (int j = 0; j < channels1; j++) {
            Array slice;
            try {
                slice = a.section(new int[] { 0, j }, new int[] { scans1, 1 });
                this.log.debug("Shape of slice: {} = {}", j, Arrays.toString(slice.getShape()));
                final Index ind = slice.getIndex();
                final Index cind = c.getIndex();
                double current;
                for (int i = 0; i < slice.getShape()[0]; i++) {
                    this.log.debug("i=" + i);
                    current = slice.getDouble(ind.set(i));
                    this.log.debug("Checking for extremum!");
                    final int lmedian_low = Math.max(0, i - median_window1);
                    final int lmedian_high = Math.min(slice.getShape()[0] - 1, i + median_window1);
                    this.log.debug("Median low: " + lmedian_low + " high: " + lmedian_high);
                    double[] vals;
                    try {
                        vals = (double[]) slice.section(new int[] { lmedian_low }, new int[] { lmedian_high - lmedian_low }, new int[] { 1 }).get1DJavaArray(double.class);
                        double mean = MathTools.average(vals, 0, vals.length - 1);
                        lmedian = MathTools.median(vals);
                        lstddev = Math.abs(vals[vals.length - 1] - vals[0]);
                        this.log.debug("local rel dev={}", lstddev);
                        cind.set(i, j);
                        final double corrected_value = Math.max(current - lmedian, 0);
                        final double lvar = vals[vals.length - 1] - lmedian;
                        final double snr = (mean / lstddev);
                        final double snrdb = 10.0d * Math.log10(snr);
                        if (snrdb > 0.0d) {
                            this.log.debug("Signal : {}, noise: {}, ratio: {}, log(ratio): {}", new Object[] { current, lmedian, snr, snrdb });
                            this.log.debug("{}\t{}\t{}\t{}\t{}\t{} ", new Object[] { current, vals[0], lmedian, vals[vals.length - 1], lstddev, lvar, snr });
                        }
                        c.setDouble(cind, snrdb > this.snr_minimum ? corrected_value : 0.0d);
                    } catch (final InvalidRangeException e) {
                        this.log.error(e.getLocalizedMessage());
                    }
                }
            } catch (final InvalidRangeException e1) {
                this.log.error(e1.getLocalizedMessage());
            }
        }
        return c;
    }

    public int getChannels() {
        return this.channels;
    }

    public int getMedianWindow() {
        return this.median_window;
    }

    public int getScans() {
        return this.scans;
    }

    /**
	 * @return the snr_minimum
	 */
    public double getSnrMinimum() {
        return this.snr_minimum;
    }

    public void setChannels(final int channels1) {
        this.channels = channels1;
    }

    public void setMedianWindow(final int median_window1) {
        this.median_window = median_window1;
    }

    public void setScans(final int scans1) {
        this.scans = scans1;
    }

    /**
	 * @param snr_minimum
	 *            the snr_minimum to set
	 */
    public void setSnrMinimum(final double snr_minimum) {
        this.snr_minimum = snr_minimum;
    }
}
