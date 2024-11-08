package org.expasy.jpl.experimental.ms.peaklist;

import java.util.ArrayList;
import java.util.Arrays;
import org.expasy.jpl.experimental.ms.lcmsms.filtering.filter.JPLPeakGrouper;
import org.expasy.jpl.experimental.ms.peak.JPLExpMSPeakLC;
import org.expasy.jpl.utils.builder.JPLBuilder;
import org.expasy.jpl.utils.exceptions.JPLBuilderException;
import org.expasy.jpl.utils.sort.SimpleTypeArray;

public class JPLConsensusSpectrumBuilder implements JPLBuilder<JPLConsensusSpectrum> {

    /** Consensus spectrum */
    private JPLConsensusSpectrum consensus;

    /** Spectra that were used to create consensus (eventual not used)*/
    private ArrayList<? extends JPLExpPeakList> cluster;

    /** Mass error between the same peaks in different spectra */
    private double mzAlignError;

    /**
	 * Default constructor
	 */
    public JPLConsensusSpectrumBuilder() {
        consensus = null;
        cluster = null;
        mzAlignError = 0.3;
    }

    /**
	 * Set consensus during construction:
	 * new JPLConsensusSpectrumBuilder().consensus(consensus)
	 * @param consensus: Consensus spectrum
	 * @return Consensus spectrum builder
	 */
    public final JPLConsensusSpectrumBuilder consensus(JPLConsensusSpectrum consensus) {
        this.consensus = consensus;
        return this;
    }

    /**
	 * Set consensus during construction:
	 * new JPLConsensusSpectrumBuilder().cluster(cluster)
	 * @param cluster: Spectra that were used to create consensus
	 * @return Consensus spectrum builder
	 */
    public final JPLConsensusSpectrumBuilder cluster(ArrayList<? extends JPLExpPeakList> cluster) {
        this.cluster = cluster;
        return this;
    }

    public JPLConsensusSpectrum build() throws JPLBuilderException {
        Class<? extends JPLExpPeakList> c = cluster.get(0).getClass();
        for (JPLExpPeakList spectrum : cluster) {
            if (!spectrum.getClass().equals(c)) {
                throw new JPLConsensusBuilderException("Spectra not compatible for building consensus");
            }
        }
        if (consensus != null) {
            if (!consensus.getSpectrum().getClass().equals(c)) {
                throw new JPLConsensusBuilderException("Spectra not compatible for building consensus");
            }
        }
        startBuild();
        return consensus;
    }

    /**
	 * Set list of spectra in cluster, which forms consensus spectrum
	 * @param fragSpectra
	 */
    public void setFragSpectra(ArrayList<JPLExpPeakList> cluster) {
        this.cluster = cluster;
    }

    /**
	 * Get list of spectra in cluster
	 * @return List of spectra in cluster
	 */
    public ArrayList<? extends JPLExpPeakList> getFragSpectra() {
        return cluster;
    }

    /**
	 * Set consensus spectrum
	 * @param consensus: Consensus Spectrum
	 */
    public void setConsensus(JPLConsensusSpectrum consensus) {
        this.consensus = consensus;
    }

    /**
	 * Get consensus spectrum
	 * @return Consensus spectrum
	 */
    public JPLConsensusSpectrum getConsensus() {
        return consensus;
    }

    /**
	 * Starts build and creates or adds to consensus spectrum
	 */
    private void startBuild() throws JPLConsensusBuilderException {
        if (consensus == null && cluster != null && !cluster.isEmpty()) {
            buildConsensus();
        } else if (consensus != null && cluster != null && !cluster.isEmpty()) {
            addToConsensus();
        } else {
            throw new JPLConsensusBuilderException();
        }
    }

    /**
	 * Creates new consensus spectrum from spectrum list
	 */
    private void buildConsensus() {
        int totNrPeaks = 0;
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (JPLExpPeakList spectrum : cluster) {
            totNrPeaks += spectrum.getNbPeak();
            ids.add((int) spectrum.getId());
        }
        double[] masses = new double[totNrPeaks];
        double[] intensities = new double[totNrPeaks];
        int pos = 0;
        for (JPLExpPeakList spectrum : cluster) {
            double[] mz = spectrum.getMzs();
            double[] h = spectrum.getIntensities();
            System.arraycopy(mz, 0, masses, pos, mz.length);
            System.arraycopy(h, 0, intensities, pos, h.length);
            pos += spectrum.getNbPeak();
        }
        int[] idx = SimpleTypeArray.sortIndexesUp(masses);
        masses = SimpleTypeArray.mapping(masses, idx);
        intensities = SimpleTypeArray.mapping(intensities, idx);
        JPLPeakGrouper grouper = new JPLPeakGrouper();
        grouper.setMzMaxDiff(mzAlignError);
        JPLExpPeakList sp = cluster.get(0);
        if (sp instanceof JPLFragmentationSpectrum) {
            JPLFragmentationSpectrum expPeakList = new JPLFragmentationSpectrum(masses, intensities);
            expPeakList = (JPLFragmentationSpectrum) grouper.process(expPeakList);
            double parentMz = 0.0;
            double parentH = 0.0;
            int cnt = cluster.size();
            for (JPLExpPeakList entry : cluster) {
                JPLFragmentationSpectrum spectrum = (JPLFragmentationSpectrum) entry;
                parentMz += spectrum.getPrecursor().getMz();
                parentH += spectrum.getPrecursor().getMz();
            }
            JPLExpMSPeakLC precursor = new JPLExpMSPeakLC(parentMz / cnt, parentH / cnt);
            expPeakList.setPrecursor(precursor);
            consensus = new JPLConsensusSpectrum(expPeakList, grouper.getNrOfOccurences(), ids);
        } else if (sp instanceof JPLExpPeakList) {
            JPLExpPeakList expPeakList = new JPLExpPeakList(masses, intensities);
            expPeakList = (JPLExpPeakList) grouper.process(expPeakList);
            consensus = new JPLConsensusSpectrum(expPeakList, grouper.getNrOfOccurences(), ids);
        }
    }

    /**
	 * Add spectra to consensus spectrum
	 */
    private void addToConsensus() {
        int totNrPeaks = consensus.getSpectrum().getMzs().length;
        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (JPLExpPeakList spectrum : cluster) {
            totNrPeaks += spectrum.getNbPeak();
            ids.add((int) spectrum.getId());
        }
        double[] masses = new double[totNrPeaks];
        double[] intensities = new double[totNrPeaks];
        int[] nrOfOccurences = new int[totNrPeaks];
        int pos = 0;
        double[] mz = consensus.getSpectrum().getMzs();
        double[] h = consensus.getSpectrum().getIntensities();
        System.arraycopy(mz, 0, masses, pos, mz.length);
        System.arraycopy(h, 0, intensities, pos, h.length);
        System.arraycopy(consensus.getNrOfOccurences(), 0, nrOfOccurences, pos, mz.length);
        pos += consensus.getSpectrum().getNbPeak();
        for (JPLExpPeakList spectrum : cluster) {
            mz = spectrum.getMzs();
            h = spectrum.getIntensities();
            System.arraycopy(mz, 0, masses, pos, mz.length);
            System.arraycopy(h, 0, intensities, pos, h.length);
            for (int i = pos; i < pos + mz.length; i++) nrOfOccurences[i] = 1;
            pos += spectrum.getNbPeak();
        }
        int[] idx = SimpleTypeArray.sortIndexesUp(masses);
        masses = SimpleTypeArray.mapping(masses, idx);
        intensities = SimpleTypeArray.mapping(intensities, idx);
        nrOfOccurences = SimpleTypeArray.mapping(nrOfOccurences, idx);
        JPLPeakGrouper grouper = new JPLPeakGrouper();
        grouper.setMzMaxDiff(mzAlignError);
        grouper.setNrOfOccurences(nrOfOccurences);
        JPLExpPeakList sp = cluster.get(0);
        if (sp instanceof JPLFragmentationSpectrum) {
            JPLFragmentationSpectrum expPeakList = new JPLFragmentationSpectrum(masses, intensities);
            expPeakList = (JPLFragmentationSpectrum) grouper.process(expPeakList);
            double parentMz = 0.0;
            double parentH = 0.0;
            int cnt = cluster.size();
            for (JPLExpPeakList entry : cluster) {
                JPLFragmentationSpectrum spectrum = (JPLFragmentationSpectrum) entry;
                parentMz += spectrum.getPrecursor().getMz();
                parentH += spectrum.getPrecursor().getMz();
            }
            JPLExpMSPeakLC precursor = new JPLExpMSPeakLC(parentMz / cnt, parentH / cnt);
            expPeakList.setPrecursor(precursor);
            consensus = new JPLConsensusSpectrum(expPeakList, grouper.getNrOfOccurences(), ids);
        } else if (sp instanceof JPLExpPeakList) {
            JPLExpPeakList expPeakList = new JPLExpPeakList(masses, intensities);
            expPeakList = (JPLExpPeakList) grouper.process(expPeakList);
            consensus = new JPLConsensusSpectrum(expPeakList, grouper.getNrOfOccurences(), ids);
        }
    }

    /**
	 * Calculates the mass error between the same peak from different spectra
	 */
    public void calcMassAlignmentError() throws JPLConsensusBuilderException {
        int totNrPeaks = 0;
        for (JPLExpPeakList spectrum : cluster) {
            totNrPeaks += spectrum.getNbPeak();
        }
        double[] masses = new double[totNrPeaks];
        double[] mzDiffSingle = new double[totNrPeaks];
        double[] mzDiffComb = new double[totNrPeaks];
        int pos = 0;
        int j = 0;
        for (JPLExpPeakList spectrum : cluster) {
            double[] mz = spectrum.getMzs();
            System.arraycopy(mz, 0, masses, pos, mz.length);
            pos += spectrum.getNbPeak();
            for (int i = 1; i < mz.length; i++) {
                mzDiffSingle[j] = mz[i] - mz[i - 1];
                j++;
            }
        }
        for (; j < totNrPeaks; j++) mzDiffSingle[j] = 10000.0;
        Arrays.sort(mzDiffSingle);
        Arrays.sort(masses);
        double lambda = 0;
        int cnt = 0;
        for (int i = 0; i < masses.length - 1; i++) {
            mzDiffComb[i] = masses[i + 1] - masses[i];
            if (mzDiffComb[i] < mzAlignError) {
                lambda += mzDiffComb[i];
                cnt++;
            }
        }
        lambda = lambda / cnt;
        if (cnt < 10) {
            throw new JPLConsensusBuilderException("Not enough matching peaks between spectra!");
        }
        double th = -lambda * Math.log(0.01);
        cnt = 0;
        for (int i = 0; mzDiffSingle[i] < th; i++) cnt++;
        if (cnt / cluster.size() > 5) {
            throw new JPLConsensusBuilderException("Peaks within same spectrum are too close!");
        }
        mzAlignError = th;
    }

    /**
	 * Set mass alignment error
	 * @param mzAlignError: Mass alignment error
	 */
    public void setMzAlignError(double mzAlignError) {
        this.mzAlignError = mzAlignError;
    }

    /**
	 * Get mass alignment error
	 * @return Mass alignment error
	 */
    public double getMzAlignError() {
        return mzAlignError;
    }
}
