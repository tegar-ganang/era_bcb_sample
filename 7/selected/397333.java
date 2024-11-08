package org.expasy.jpl.core.ms.spectrum.cons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.expasy.jpl.commons.base.Editor;
import org.expasy.jpl.commons.base.EditorException;
import org.expasy.jpl.commons.base.Resetable;
import org.expasy.jpl.commons.base.builder.BuilderException;
import org.expasy.jpl.commons.base.builder.InstanceBuilder;
import org.expasy.jpl.commons.collection.PrimitiveArrayUtils;
import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.PeakListSetter;
import org.expasy.jpl.core.ms.spectrum.PeakListUtils;
import org.expasy.jpl.core.ms.spectrum.editor.PeakGrouper;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;

/**
 * Class for the storage of consensus spectra created from several input spectra
 * 
 * @author Markus Mueller
 * @author nikitin
 * 
 */
public class SpectrumCluster {

    private static final double DEFAULT_MZ_TOLERANCE = 0.2;

    private static final PeakListSetter PEAKLIST_SETTER = PeakListSetter.getInstance();

    /** the cluster of MS spectra */
    private List<PeakList> spectra;

    /** the consensus spectrum built from cluster */
    private PeakList consensusSpectrum;

    /** Table to keep track of how many times a peak is present in all spectra */
    private int[] nrOfOccurences;

    /** Maximal m/z match error for peaks of different spectra */
    private double specAlignMZTol;

    public static class Builder implements InstanceBuilder<SpectrumCluster>, Resetable {

        private List<PeakList> spectra;

        private PeakList consensusSpectrum;

        private int[] nrOfOccurences;

        private double specAlignMZTol;

        private int index = 0;

        private BuilderException e;

        public Builder() {
            spectra = new ArrayList<PeakList>();
            specAlignMZTol = DEFAULT_MZ_TOLERANCE;
        }

        public Builder tol(double tol) {
            specAlignMZTol = tol;
            return this;
        }

        public Builder addSpectrum(PeakList spectrum) {
            spectra.add(spectrum);
            if (!spectrum.hasIntensities() && e == null) {
                e = new BuilderException("clustered peak list #" + index + " has no intensities");
            } else if (spectrum.getPrecursor() == null && e == null) {
                e = new BuilderException("clustered peak list #" + index + " has no precursor");
            }
            index++;
            return this;
        }

        public Builder addSpectra(Collection<PeakList> spectra) {
            for (PeakList spectrum : spectra) {
                addSpectrum(spectrum);
            }
            return this;
        }

        public void reset() {
            spectra.clear();
            consensusSpectrum = null;
            nrOfOccurences = null;
            specAlignMZTol = DEFAULT_MZ_TOLERANCE;
            index = 0;
        }

        private void buildConsensus() {
            int totNrPeaks = 0;
            for (PeakList spectrum : spectra) {
                totNrPeaks += spectrum.size();
            }
            double[] masses = new double[totNrPeaks];
            double[] intensities = new double[totNrPeaks];
            int pos = 0;
            for (PeakList spectrum : spectra) {
                double[] mz = spectrum.getMzs(null);
                double[] h = spectrum.getIntensities(null);
                System.arraycopy(mz, 0, masses, pos, mz.length);
                System.arraycopy(h, 0, intensities, pos, h.length);
                pos += spectrum.size();
            }
            final int[] idx = PrimitiveArrayUtils.sortIndexesUp(masses);
            masses = PrimitiveArrayUtils.mapping(masses, idx);
            intensities = PrimitiveArrayUtils.mapping(intensities, idx);
            PeakGrouper grouper = new PeakGrouper();
            grouper.setMzMaxDiff(specAlignMZTol);
            PeakList expPeakList = new PeakListImpl.Builder(masses).intensities(intensities).build();
            expPeakList = grouper.transform(expPeakList);
            double parentMz = 0.0;
            double parentH = 0.0;
            int cnt = spectra.size();
            int charge = 0;
            for (PeakList spectrum : spectra) {
                parentMz += spectrum.getPrecursor().getMz();
                parentH += spectrum.getPrecursor().getIntensity();
                charge = spectrum.getPrecursor().getCharge();
            }
            PeakImpl precursor = new PeakImpl.Builder(parentMz / cnt).intensity(parentH).charge(charge).build();
            PEAKLIST_SETTER.setPrecursor((PeakListImpl) expPeakList, precursor);
            nrOfOccurences = grouper.getNrOfOccurences();
            consensusSpectrum = expPeakList;
        }

        public boolean isEmpty() {
            if (spectra.size() == 0) {
                return true;
            }
            return false;
        }

        public SpectrumCluster build() throws BuilderException {
            if (e != null) {
                throw e;
            }
            if (isEmpty()) {
                throw new BuilderException("no spectrum found");
            }
            buildConsensus();
            return new SpectrumCluster(this);
        }
    }

    public static class EditorImpl implements Editor<SpectrumCluster> {

        private SpectrumCluster cluster;

        /** Spectra that have to be added to consensus */
        private List<PeakListImpl> toAdd;

        /** Spectra that have to be removed from consensus */
        private List<PeakListImpl> toRemove;

        public EditorImpl(SpectrumCluster cluster) {
            this.cluster = cluster;
            this.toAdd = new ArrayList<PeakListImpl>();
            this.toRemove = new ArrayList<PeakListImpl>();
        }

        public EditorImpl removeSpectrum(PeakListImpl spectrum) {
            toRemove.add(spectrum);
            return this;
        }

        public EditorImpl removeSpectra(List<PeakListImpl> spectra) {
            toRemove.addAll(spectra);
            return this;
        }

        public EditorImpl addSpectrum(PeakListImpl spectrum) {
            toAdd.add(spectrum);
            return this;
        }

        public EditorImpl addSpectra(List<PeakListImpl> spectra) {
            toAdd.addAll(spectra);
            return this;
        }

        /**
		 * Add spectra to consensus spectrum
		 */
        private void addToConsensus() {
            PeakList consensus = cluster.getConsensusSpectrum();
            int totNrPeaks = consensus.size();
            for (PeakList spectrum : toAdd) {
                totNrPeaks += spectrum.size();
            }
            double[] masses = new double[totNrPeaks];
            double[] intensities = new double[totNrPeaks];
            int[] nrOfOccurences = new int[totNrPeaks];
            int pos = 0;
            consensus.getMzs(masses);
            consensus.getIntensities(intensities);
            int[] occ = cluster.getNrOfOccurences();
            if (occ != null) System.arraycopy(occ, 0, nrOfOccurences, pos, occ.length);
            pos += consensus.size();
            for (PeakListImpl spectrum : toAdd) {
                spectrum.getMzs(masses, pos);
                spectrum.getIntensities(intensities, pos);
                for (int i = pos; i < pos + spectrum.size(); i++) nrOfOccurences[i] = 1;
                pos += spectrum.size();
            }
            final int[] idx = PrimitiveArrayUtils.sortIndexesUp(masses);
            masses = PrimitiveArrayUtils.mapping(masses, idx);
            intensities = PrimitiveArrayUtils.mapping(intensities, idx);
            nrOfOccurences = PrimitiveArrayUtils.mapping(nrOfOccurences, idx);
            PeakGrouper grouper = new PeakGrouper();
            grouper.setMzMaxDiff(cluster.getSpecAlignMZTol());
            grouper.setNrOfOccurences(nrOfOccurences);
            PeakList expPeakList = new PeakListImpl.Builder(masses).intensities(intensities).build();
            expPeakList = grouper.transform(expPeakList);
            int nrProcessedSpectra = 0;
            if (cluster.spectra != null) nrProcessedSpectra += cluster.spectra.size();
            double parentMz = consensus.getPrecursor().getMz() * cluster.spectra.size();
            double parentH = consensus.getPrecursor().getIntensity();
            int charge = consensus.getPrecursor().getCharge();
            int cnt = cluster.spectra.size();
            for (PeakList spectrum : cluster.spectra) {
                parentMz += spectrum.getPrecursor().getMz();
                parentH += spectrum.getPrecursor().getIntensity();
                charge = spectrum.getPrecursor().getCharge();
            }
            PeakImpl precursor = new PeakImpl.Builder(parentMz / cnt).intensity(parentH).charge(charge).build();
            PeakListSetter manager = PeakListSetter.getInstance();
            manager.setPrecursor((PeakListImpl) expPeakList, precursor);
            cluster.consensusSpectrum = expPeakList;
        }

        private void removeFromConsensus() {
            PeakList consensus = cluster.getConsensusSpectrum();
            int totNrPeaks = consensus.size();
            double[] mz = consensus.getMzs(null);
            double[] h = consensus.getIntensities(null);
            int[] cnts = cluster.getNrOfOccurences();
            int[] nrOfOccurences = new int[totNrPeaks];
            System.arraycopy(cnts, 0, nrOfOccurences, 0, totNrPeaks);
            double[] masses = new double[totNrPeaks];
            double[] intensities = new double[totNrPeaks];
            System.arraycopy(mz, 0, masses, 0, totNrPeaks);
            System.arraycopy(h, 0, intensities, 0, totNrPeaks);
            for (PeakList spectrum : toRemove) {
                mz = spectrum.getMzs(null);
                h = spectrum.getIntensities(null);
                int cIndex = 0;
                for (int i = 0; i < mz.length; i++) {
                    if (masses[masses.length - 1] < mz[i] - cluster.specAlignMZTol) continue;
                    while (masses[cIndex] <= mz[i] - cluster.specAlignMZTol && cIndex < masses.length - 1) cIndex++;
                    int cMin = cIndex;
                    while (masses[cIndex] <= mz[i] + cluster.specAlignMZTol && cIndex < masses.length - 1) cIndex++;
                    int cMax;
                    if (masses[cIndex] > mz[i] + cluster.specAlignMZTol) cMax = cIndex - 1; else cMax = cIndex;
                    if (cMax >= cMin) {
                        for (int j = cMin; j <= cMax; j++) {
                            if (Math.abs(masses[j] - mz[i]) > cluster.specAlignMZTol) continue;
                            double m = masses[j] * intensities[j] - mz[i] * h[i];
                            if (m >= 0.0) {
                                intensities[j] = Math.max(0.0, intensities[j] - h[i]);
                                if (intensities[j] > 0.0) {
                                    masses[j] = m / intensities[j];
                                }
                                nrOfOccurences[j]--;
                            }
                        }
                    }
                }
            }
            final int[] idx = PrimitiveArrayUtils.sortIndexesUp(masses);
            masses = PrimitiveArrayUtils.mapping(masses, idx);
            intensities = PrimitiveArrayUtils.mapping(intensities, idx);
            nrOfOccurences = PrimitiveArrayUtils.mapping(nrOfOccurences, idx);
            totNrPeaks = 0;
            for (int i = 0; i < masses.length; i++) {
                if (nrOfOccurences[i] > 0) totNrPeaks++;
            }
            double[] newMasses = new double[totNrPeaks];
            double[] newIntensities = new double[totNrPeaks];
            int[] newNrOfOccurences = new int[totNrPeaks];
            int cIndex = 0;
            for (int i = 0; i < masses.length; i++) {
                if (nrOfOccurences[i] > 0) {
                    newMasses[cIndex] = masses[i];
                    newIntensities[cIndex] = intensities[i];
                    newNrOfOccurences[cIndex] = nrOfOccurences[i];
                    cIndex++;
                }
            }
            PeakList expPeakList = new PeakListImpl.Builder(newMasses).intensities(newIntensities).build();
            int nrProcessedSpectra = 0;
            if (cluster.spectra != null) nrProcessedSpectra += cluster.spectra.size();
            double parentMz = consensus.getPrecursor().getMz();
            double parentH = consensus.getPrecursor().getIntensity();
            int charge = consensus.getPrecursor().getCharge();
            int cnt = cluster.spectra.size() - toRemove.size();
            if (cnt > 0) {
                parentMz *= cluster.spectra.size();
                for (PeakList spectrum : toRemove) {
                    parentMz -= spectrum.getPrecursor().getMz();
                    parentH -= spectrum.getPrecursor().getIntensity();
                }
                parentMz = parentMz / cnt;
            }
            for (PeakList spectrum : cluster.spectra) {
                parentMz += spectrum.getPrecursor().getMz();
                parentH += spectrum.getPrecursor().getIntensity();
                charge = spectrum.getPrecursor().getCharge();
            }
            if (cnt > 0 && totNrPeaks > 0) {
                PeakImpl precursor = new PeakImpl.Builder(parentMz / cnt).intensity(parentH).charge(charge).build();
                PeakListSetter manager = PeakListSetter.getInstance();
                manager.setPrecursor((PeakListImpl) expPeakList, precursor);
                nrOfOccurences = newNrOfOccurences;
                cluster.consensusSpectrum = expPeakList;
            } else {
                cluster.consensusSpectrum = null;
            }
        }

        public void edit() throws EditorException {
            if (toAdd.size() == 0 && toRemove.size() == 0) {
                throw new EditorException("no edition to do for cluster " + cluster);
            }
            if (cluster == null) {
                throw new EditorException("no defined cluster");
            }
            if (cluster.consensusSpectrum == null) {
                throw new EditorException("no defined consensus");
            }
            cluster.spectra.addAll(toAdd);
            cluster.spectra.removeAll(toRemove);
            if (toAdd.size() > 0) {
                addToConsensus();
            }
            if (toRemove.size() > 0) {
                removeFromConsensus();
            }
        }
    }

    /**
	 * Default constructor
	 */
    public SpectrumCluster(Builder builder) {
        this.spectra = builder.spectra;
        this.consensusSpectrum = builder.consensusSpectrum;
        this.nrOfOccurences = builder.nrOfOccurences;
        this.specAlignMZTol = builder.specAlignMZTol;
    }

    /**
	 * Get the spectra that belong to this cluster.
	 * 
	 * @return the cluster spectra
	 */
    public final List<PeakList> getSpectra() {
        return spectra;
    }

    /**
	 * Get the consensus spectrum.
	 * 
	 * @return the consensusSpectrum
	 */
    public final PeakList getConsensusSpectrum() {
        return consensusSpectrum;
    }

    /**
	 * Get the table that keep track of how often a peak is present in a bin in
	 * all spectra
	 * 
	 * @return the nrOfOccurences
	 */
    public final int[] getNrOfOccurences() {
        return nrOfOccurences;
    }

    /**
	 * @return the specAlignMZTol
	 */
    public final double getSpecAlignMZTol() {
        return specAlignMZTol;
    }

    /**
	 * Return average parent mz of all member spectra
	 * 
	 * @return average parent mz
	 */
    public double getParentMz() {
        double mz = 0.0;
        if (consensusSpectrum != null) {
            mz = consensusSpectrum.getPrecursor().getMz();
        } else {
            for (PeakList sp : spectra) {
                mz += sp.getPrecursor().getMz();
            }
            mz /= spectra.size();
        }
        return mz;
    }

    /**
	 * Returns parent ion charge of cluster. It is assumed that only one charge
	 * is present (will change)
	 * 
	 * @return parent ion charge
	 */
    public int getParentCharge() {
        int charge = 0;
        if (consensusSpectrum != null) {
            charge = consensusSpectrum.getPrecursor().getCharge();
        } else {
            charge = spectra.get(0).getPrecursor().getCharge();
        }
        return charge;
    }

    /**
	 * Calculates a score between 0 and 1 to evaluate the cluster quality
	 * 
	 * @return cluster quality score
	 */
    public double calcClusterQuality() {
        if (consensusSpectrum == null || nrOfOccurences == null || nrOfOccurences.length == 0) return -1.0;
        double score = 0.0;
        int[] occ = nrOfOccurences;
        occ = Arrays.copyOf(occ, occ.length);
        Arrays.sort(occ);
        int n = Math.min(10, occ.length);
        for (int i = occ.length - 1; i > occ.length - 1 - n; i--) {
            score += Math.min(occ[i], spectra.size());
        }
        return score / (n * spectra.size());
    }

    /**
	 * Selects all peaks that occurred in at least minNrOfOcc spectra
	 * 
	 * @param minNrOfOcc : Minimal number of occurences
	 */
    public PeakList filter(int minNrOfOcc) {
        List<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < nrOfOccurences.length; i++) {
            if (nrOfOccurences[i] >= minNrOfOcc) {
                idx.add(i);
            }
        }
        int[] newNrOfOccurences = new int[idx.size()];
        int j = 0;
        for (int i = 0; i < nrOfOccurences.length; i++) {
            if (nrOfOccurences[i] >= minNrOfOcc) {
                newNrOfOccurences[j] = nrOfOccurences[i];
                j++;
            }
        }
        nrOfOccurences = newNrOfOccurences;
        PeakListUtils plUtility = PeakListUtils.getInstance();
        return plUtility.subList(consensusSpectrum, idx);
    }

    /**
	 * Calculates the mass error between the same peak from different spectra
	 */
    public double calcMassAlignmentError() {
        int totNrPeaks = 0;
        for (PeakList spectrum : spectra) {
            totNrPeaks += spectrum.size();
        }
        double[] masses = new double[totNrPeaks];
        double[] mzDiffSingle = new double[totNrPeaks];
        double[] mzDiffComb = new double[totNrPeaks];
        int pos = 0;
        int j = 0;
        for (PeakList spectrum : spectra) {
            double[] mz = ((PeakListImpl) spectrum).getMzs(masses, pos);
            pos += spectrum.size();
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
            if (mzDiffComb[i] < specAlignMZTol) {
                lambda += mzDiffComb[i];
                cnt++;
            }
        }
        lambda = lambda / cnt;
        if (cnt < 10) {
            throw new IllegalStateException("Not enough matching peaks between spectra!");
        }
        double th = -lambda * Math.log(0.01);
        cnt = 0;
        for (int i = 0; mzDiffSingle[i] < th; i++) cnt++;
        if (cnt / spectra.size() > 5) {
            throw new IllegalStateException("Peaks within same spectrum are too close!");
        }
        return th;
    }
}
