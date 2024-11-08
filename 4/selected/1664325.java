package src.projects.findPeaks.objects;

import src.lib.Histogram;
import src.lib.Utilities;
import src.lib.ioInterfaces.FileOut;
import src.lib.ioInterfaces.Log_Buffer;
import src.lib.objects.Tuple;
import src.projects.findPeaks.PeakDataSetParent;

/**
 * Used to find peaks using mapped tags from a ChIP-type experiment.
 * 
 * Store peak statistics for use in generating histograms, and thus, FDR
 * 
 * @version $Revision: 2532 $
 * @author Genome Sciences Centre
 */
public class PeakStats {

    private static boolean display_version = true;

    private static Log_Buffer LB;

    private PeakStore pstore;

    private MapStore mstore;

    private Histogram peak_summary;

    private static final int FIXED_WIDTH_FIELD = 15;

    private static final int PERCENTATGE = 100;

    private float tallest = 0;

    private float sum_peak_heights = 0;

    private int last_peak_end = 0;

    /**
	 * Initialize the PeakStats engine over a PeakStore/MapStore dataset.
	 * @param logbuffer
	 * @param pli
	 * @param histogram_size
	 * @param precision
	 */
    public PeakStats(Log_Buffer logbuffer, PeakDataSetParent pli, int histogram_size, int precision) {
        LB = logbuffer;
        if (display_version) {
            LB.Version("PeakStats", "$Revision: 2532 $");
            display_version = false;
        }
        this.mstore = pli.get_map_store();
        this.pstore = pli.get_peak_store();
        this.peak_summary = new Histogram(logbuffer, histogram_size * precision, 0, histogram_size, false);
        for (Peakdesc p : this.pstore) {
            if (p.get_height() > this.tallest) {
                this.tallest = p.get_height();
            }
            if (p.get_length() + p.get_offset() > this.last_peak_end) {
                this.last_peak_end = p.get_length() + p.get_offset();
            }
            sum_peak_heights += p.get_height();
            peak_summary.bin_value(p.get_height());
        }
    }

    public void clear() {
        pstore = null;
        mstore = null;
        peak_summary = null;
    }

    public final int get_total_tags() {
        return pstore.get_reads_total();
    }

    /**
	 * Getter - number of Peaks
	 * @return integer: The number of peaks.
	 */
    public final int get_number_of_peaks() {
        return this.pstore.get_size();
    }

    /**
	 * Getter - overflows
	 * 
	 * @return integer: the number of values that did not fit into the available
	 *         bins, and were larger than the largest available bin.
	 */
    public final int get_overflows() {
        return this.peak_summary.get_overflows();
    }

    /**
	 * Getter - get_underflows
	 * 
	 * @return integer: the number of values that did not fit into the avaiable
	 *         bins, and were smaller than the smallest available bin
	 */
    public final int get_underflows() {
        return this.peak_summary.get_underflows();
    }

    /**
	 * Getter - get_tags_used
	 *	@return integer: the total number of tags used.  
	 */
    public final int get_tags_used() {
        return pstore.get_reads_used();
    }

    /**
	 * Getter - get_tags_used
	 *	@return integer: the total number of tags used.  
	 */
    public final int get_tags_filtered() {
        return pstore.get_reads_filtered();
    }

    /**
	 * Getter - coverage
	 * @return integer: the number of bases which have non-zero coverage values.
	 */
    public final int get_coverage() {
        return mstore.get_coverage();
    }

    /**
	 * Getter - tallest
	 * 
	 * @return float: the height of the tallest peak in the scanned region
	 *         (chromosome/genome/etc)
	 */
    public final float get_tallest() {
        return this.tallest;
    }

    /**
	 * Getter - sum of peaks
	 * @return float: the sum of each peak included in the peaks file.
	 */
    public final float get_sum_of_peaks() {
        return this.sum_peak_heights;
    }

    /**
	 * Getter - count_peaks_at_height
	 * 
	 * @param height
	 * @return The count of peaks at a given height, and not those of greater or
	 *         lesser heights
	 */
    public final long get_count_peaks_at_height(float height) {
        return this.peak_summary.get_hist_value(height);
    }

    /**
	 * Getter - count_peaks_in_bin
	 * 
	 * @param bin
	 * @return The count of peaks in a given bin
	 */
    public final long get_count_peaks_at_bin(int bin) {
        return this.peak_summary.get_bin_value(bin);
    }

    /**
	 * @param filterDupes
	 * @param genome_size
	 * @param fo
	 */
    public void output(boolean filterDupes, float genome_size, FileOut fo) {
        double lambda = (this.get_coverage() / genome_size);
        fo.writeln("****************************");
        fo.writeln("Summary Statistics - Reads");
        fo.writeln("****************************");
        fo.writeln("Total Tags                    " + Utilities.FormatNumberForPrinting(this.get_total_tags(), FIXED_WIDTH_FIELD));
        fo.writeln("Actual Peak Coverage, bases   " + Utilities.FormatNumberForPrinting(this.get_coverage(), FIXED_WIDTH_FIELD));
        fo.writeln("Actual Peak Coverage, percent " + Utilities.DecimalPoints(((float) this.get_coverage() * PERCENTATGE) / genome_size, 3) + "%");
        if (filterDupes) {
            fo.writeln("Unique Tags (used)            " + Utilities.FormatNumberForPrinting(pstore.get_reads_used(), FIXED_WIDTH_FIELD));
            fo.writeln("Duplicate tags in genome      " + Utilities.FormatNumberForPrinting(pstore.get_reads_filtered(), FIXED_WIDTH_FIELD) + " (" + Utilities.DecimalPoints((((float) this.get_tags_filtered() / (float) (this.get_reads_used())) * PERCENTATGE), 3) + "%)*");
        }
        fo.writeln("");
        fo.writeln("****************************");
        fo.writeln("Summary Statistics - Peaks");
        fo.writeln("****************************");
        fo.writeln("Num. peaks          " + Utilities.FormatNumberForPrinting(this.get_number_of_peaks(), FIXED_WIDTH_FIELD));
        fo.writeln("Tallest             " + Utilities.DecimalPoints(this.tallest, 1));
        fo.writeln("Lambda              " + Utilities.DecimalPoints(lambda, 4));
        fo.writeln("Sum of Peak Heights " + Utilities.FormatNumberForPrinting(this.sum_peak_heights, FIXED_WIDTH_FIELD));
        fo.writeln("avg peak height     " + Utilities.FormatNumberForPrinting(this.sum_peak_heights / (float) this.get_number_of_peaks(), FIXED_WIDTH_FIELD));
        fo.writeln("");
    }

    /**
	 * Output for each iteration of the MCFDR
	 * @param iterations
	 * @param fo
	 */
    public void output_per_iteration(int iterations, FileOut fo) {
        fo.writeln("");
        fo.writeln("*******************************************");
        fo.writeln("Summary Statistics - Average per Iteration");
        fo.writeln("*******************************************");
        fo.writeln("Num. peaks          " + Utilities.FormatNumberForPrinting(this.get_reads_used() / iterations, FIXED_WIDTH_FIELD));
        fo.writeln("Sum of Peak Heights " + Utilities.FormatNumberForPrinting(this.sum_peak_heights / iterations, FIXED_WIDTH_FIELD));
        fo.writeln("");
    }

    public void printPeak_heightHist() {
        peak_summary.print_bins();
    }

    public final int get_reads_used() {
        return pstore.get_reads_used();
    }

    public final Tuple<Integer, Integer> get_LW_pair() {
        return pstore.get_LW_pair();
    }

    public final int get_LW_singles() {
        return pstore.get_LW_singles();
    }

    public final int get_LW_doubles() {
        return pstore.get_LW_doubles();
    }

    public final int get_chromosome_end() {
        return this.last_peak_end;
    }

    public final Histogram get_histogram() {
        Histogram h = this.peak_summary;
        return h;
    }
}
