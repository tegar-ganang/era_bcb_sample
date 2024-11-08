package src.projects.findPeaks;

import src.lib.Histogram;
import src.lib.Utilities;
import src.lib.ioInterfaces.FileOut;
import src.projects.findPeaks.objects.PeakStats;

/**
 * @version $Revision: 2532 $
 * @author 
 */
public class FP_Output {

    private FP_Output() {
    }

    public static void output_pkstats(PeakStats[] sim_stats, PeakStats exp_stats, FileOut fo) {
        int sets = sim_stats.length;
        double avg_tags = 0;
        double sd_avg_tags = 0;
        double avg_coverage = 0;
        double sd_avg_coverage = 0;
        double avg_peak_count = 0;
        double sd_avg_peak_count = 0;
        double avg_tallest = 0;
        double sd_avg_tallest = 0;
        double avg_peak_sum = 0;
        double sd_avg_peak_sum = 0;
        for (int i = 0; i < sets; i++) {
            avg_tags += sim_stats[i].get_total_tags();
            avg_coverage += sim_stats[i].get_coverage();
            avg_peak_count += sim_stats[i].get_number_of_peaks();
            avg_tallest += sim_stats[i].get_tallest();
            avg_peak_sum += sim_stats[i].get_sum_of_peaks();
        }
        avg_tags /= sets;
        avg_coverage /= sets;
        avg_peak_count /= sets;
        avg_tallest /= sets;
        avg_peak_sum /= sets;
        for (int i = 0; i < sets; i++) {
            double tmp = (sim_stats[i].get_total_tags() - avg_tags);
            sd_avg_tags += (tmp * tmp);
            tmp = (sim_stats[i].get_coverage() - avg_coverage);
            sd_avg_coverage += (tmp * tmp);
            tmp = (sim_stats[i].get_number_of_peaks() - avg_peak_count);
            sd_avg_peak_count += (tmp * tmp);
            tmp = (sim_stats[i].get_tallest() - avg_tallest);
            sd_avg_tallest += (tmp * tmp);
            tmp = (sim_stats[i].get_sum_of_peaks() - avg_peak_sum);
            sd_avg_peak_sum += (tmp * tmp);
        }
        sd_avg_tags /= sets;
        sd_avg_tags = Math.sqrt(sd_avg_tags);
        sd_avg_coverage /= sets;
        sd_avg_coverage = Math.sqrt(sd_avg_coverage);
        sd_avg_peak_count /= sets;
        sd_avg_peak_count = Math.sqrt(sd_avg_peak_count);
        sd_avg_tallest /= sets;
        sd_avg_tallest = Math.sqrt(sd_avg_tallest);
        sd_avg_peak_sum /= sets;
        sd_avg_peak_sum = Math.sqrt(sd_avg_peak_sum);
        fo.writeln("------------------------------------------------------------------------");
        fo.writeln("                        experiment        sim. average         std.dev.");
        fo.writeln("------------------------------------------------------------------------");
        fo.writeln("Tags used:            " + Utilities.FormatNumberForPrinting(exp_stats.get_total_tags(), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting((int) avg_tags, FPConstants.FIELD_WIDTH_2) + " " + Utilities.FormatNumberForPrinting(sd_avg_tags, FPConstants.FIELD_WIDTH_3));
        fo.writeln("Coverage:             " + Utilities.FormatNumberForPrinting(exp_stats.get_coverage(), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting((int) avg_coverage, FPConstants.FIELD_WIDTH_2) + " " + Utilities.FormatNumberForPrinting(sd_avg_coverage, FPConstants.FIELD_WIDTH_3));
        fo.writeln("Peaks count :         " + Utilities.FormatNumberForPrinting(exp_stats.get_number_of_peaks(), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting((int) avg_peak_count, FPConstants.FIELD_WIDTH_2) + " " + Utilities.FormatNumberForPrinting(sd_avg_peak_count, FPConstants.FIELD_WIDTH_3));
        fo.writeln("Tallest :             " + Utilities.FormatNumberForPrinting(exp_stats.get_tallest(), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting(avg_tallest, FPConstants.FIELD_WIDTH_2) + " " + Utilities.FormatNumberForPrinting(sd_avg_tallest, FPConstants.FIELD_WIDTH_3));
        fo.writeln("Sum of peak heights : " + Utilities.FormatNumberForPrinting(exp_stats.get_sum_of_peaks(), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting(avg_peak_sum, FPConstants.FIELD_WIDTH_2) + " " + Utilities.FormatNumberForPrinting(sd_avg_peak_sum, FPConstants.FIELD_WIDTH_3));
        fo.writeln("Average peak height : " + Utilities.FormatNumberForPrinting((exp_stats.get_sum_of_peaks() / exp_stats.get_number_of_peaks()), FPConstants.FIELD_WIDTH_1) + " " + Utilities.FormatNumberForPrinting(avg_peak_sum / avg_peak_count, FPConstants.FIELD_WIDTH_2));
        fo.writeln("------------------------------------------------------------------------");
        fo.writeln("Estimaged Signal to noise ratio :  " + Utilities.FormatNumberForPrinting(exp_stats.get_total_tags() - avg_tags, 0) + " : " + Utilities.FormatNumberForPrinting(avg_tags, 0));
        fo.writeln("Enrichment of :                    " + Utilities.DecimalPoints(((exp_stats.get_total_tags() - avg_tags) / avg_tags), 2));
        fo.writeln("Signal in percent of reads:        " + Utilities.DecimalPoints((((float) exp_stats.get_total_tags() - avg_tags) / exp_stats.get_total_tags()) * FPConstants.PERCENTAGE, 2) + "%");
        fo.writeln("");
    }

    public static void calculate_FDR(PeakStats[] sim_stats, PeakStats exp_stats, int number_of_bins, int precision, String chromosome, FileOut fo) {
        fo.writeln("========================== chr" + chromosome + " ==========================");
        fo.writeln("ht.\tObs. >=\tRand. >=\tFDR");
        int datasets = sim_stats.length;
        int size = (number_of_bins * precision) + 1;
        long[] cumulative_ran = new long[size];
        long[] cumulative_obs = new long[size];
        for (int q = 0; q < datasets; q++) {
            cumulative_ran[size - 1] = sim_stats[q].get_overflows();
        }
        cumulative_obs[size - 1] = exp_stats.get_overflows();
        for (int bins = size - 2; bins >= 0; bins--) {
            cumulative_ran[bins] = cumulative_ran[bins + 1];
            for (int q = 0; q < datasets; q++) {
                cumulative_ran[bins] += sim_stats[q].get_count_peaks_at_bin(bins);
            }
            cumulative_obs[bins] = cumulative_obs[bins + 1] + exp_stats.get_count_peaks_at_bin(bins);
        }
        for (int i = 1; i < size - 1; i++) {
            float height = i / (float) precision;
            long count_of_peaks = 0;
            for (PeakStats x : sim_stats) {
                count_of_peaks += x.get_count_peaks_at_height(height);
            }
            fo.writeln(Utilities.DecimalPoints(height, precision / 10) + "\t" + Long.toString(cumulative_obs[i]) + "\t" + Utilities.DecimalPoints((double) cumulative_ran[i] / datasets, 4) + "\t" + Utilities.DecimalPoints((double) cumulative_ran[i] / (datasets * (double) cumulative_obs[i]), FPConstants.DECIMAL_PLACES_8));
        }
    }

    /**
	 * Calculate and output FDR from a cumulative number of random peak and an observed Pk_stats object.
	 * @param cumNumRndPeaks An array containing cummulative number of peak for each bin.
	 * but the size is actually number_of_bins * precision
	 * @param exp_stats The Pk_stats object containing the experimental peak.
	 * @param Size_Hist the maximum of the histogram. the actual number of bin is Size_Hist*precision.
	 * @param precision The precision of the histogram.
	 * @param chromosome
	 * @param fo the output file.
	 */
    public static void calculate_FDR(double[] cumNumRndPeaks, Histogram exp_stats, int Size_Hist, int precision, String chromosome, FileOut fo) {
        fo.writeln("========================== chr" + chromosome + " ==========================");
        fo.writeln("ht.\tObs. >=\tRand. >=\tFDR");
        long[] cumulative_obs = new long[(Size_Hist * precision) + 1];
        cumulative_obs[(Size_Hist * precision)] = exp_stats.get_overflows();
        for (int bins = (Size_Hist * precision) - 1; bins >= 0; bins--) {
            cumulative_obs[bins] = cumulative_obs[bins + 1] + exp_stats.get_bin_value(bins);
        }
        for (int i = 0; i < cumNumRndPeaks.length; i++) {
            double height = i / (double) precision;
            fo.writeln(Utilities.DecimalPoints(height, 2) + "\t" + Utilities.DecimalPoints(cumulative_obs[i], 1) + "\t" + Utilities.DecimalPoints(cumNumRndPeaks[i], 1) + "\t" + Utilities.DecimalPoints(cumNumRndPeaks[i] / cumulative_obs[i], 8));
        }
    }
}
