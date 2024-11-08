package org.expasy.jpl.tools.delib;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.FileUtils;
import org.expasy.jpl.commons.base.io.JPLFile;
import org.expasy.jpl.commons.base.process.ProcessException;
import org.expasy.jpl.commons.base.render.ImageRenderingException;
import org.expasy.jpl.commons.base.task.TerminalProgressBar;
import org.expasy.jpl.commons.collection.PrimitiveArrayUtils;
import org.expasy.jpl.commons.collection.ExtraIterable.AbstractExtraIterator;
import org.expasy.jpl.commons.collection.render.HistogramDataSetRenderer;
import org.expasy.jpl.commons.collection.stat.HistogramDataSet;
import org.expasy.jpl.commons.collection.stat.HistogramDataSetExporter;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmentProcessorImpl;
import org.expasy.jpl.core.ms.export.MSRenderer;
import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.annot.ExperimentalFragmentAnnotation;
import org.expasy.jpl.core.ms.spectrum.annot.FragmentAnnotations;
import org.expasy.jpl.core.ms.spectrum.filter.SpectrumProcessorManager;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.expasy.jpl.core.ms.spectrum.stat.MS1MS2PeakDists;
import org.expasy.jpl.io.hadoop.ms.hist.reader.PeakHistMapFileReader;
import org.expasy.jpl.io.ms.MSScan;
import org.expasy.jpl.io.ms.reader.MSPReader;
import org.expasy.jpl.io.ms.reader.MSReaderFacade;
import org.expasy.jpl.io.ms.reader.SPTXTReader;
import org.expasy.jpl.io.ms.writer.AbstractMSWriter;
import org.expasy.jpl.io.ms.writer.MSPWriter;
import org.expasy.jpl.io.ms.writer.SPTXTWriter;
import org.expasy.jpl.msmatch.PeakListMatcherImpl;
import org.expasy.jpl.msmatch.scorer.NCorrScorer;

/**
 * This application creates a decoy library from a MS annotated library. This
 * implementation is a reimplementation of DeLiberator v0.9 developed by Yuki
 * Ohta in java.
 * 
 * @author nikitin
 */
public final class Deliberator2 {

    private static Transformer<Object, PeakList> PL_TRANSFORMER = new Transformer<Object, PeakList>() {

        public final PeakList transform(Object scan) {
            return ((MSScan) scan).getPeakList();
        }
    };

    private static final Pattern MODIF_PATTERN = Pattern.compile("(Mods=\\S+)");

    private static final Random RANDOM;

    private static final PeptideFragmentProcessorImpl FRAGMENT_PROCESSOR = PeptideFragmentProcessorImpl.newInstance();

    private static final int MAX_SHUFFLING_TRIES = 10;

    private static final MSRenderer MS_RENDERER;

    private static final HistogramDataSetRenderer HISTO_RENDERER;

    private static final PeakHistMapFileReader MS1MS2_PEAKS_READER;

    private static final PeakListMatcherImpl MS_MATCHER;

    private static final SequenceShuffler SEQ_SHUFFLER;

    private static final AnnotatedNotPrecPeakListFilter ANNOTATED_PL_FILTER;

    private static final SpectrumProcessorManager FILTERS_MANAGER;

    private final DelibParamManager paramManager;

    private BufferedWriter logWriter;

    private AbstractExtraIterator<MSScan> mslibIter;

    private MS1MS2PeakDists dists;

    private TerminalProgressBar pb;

    private int totalNumberOfRatios;

    private static int count = 0;

    private double[] ratios;

    static {
        RANDOM = new Random(new Date().getTime());
        MS_MATCHER = PeakListMatcherImpl.newInstance();
        MS_MATCHER.setScorer(NCorrScorer.getInstance());
        MS_RENDERER = MSRenderer.newInstance();
        MS_RENDERER.setChartColor(0, Color.BLUE);
        MS1MS2_PEAKS_READER = new PeakHistMapFileReader();
        HISTO_RENDERER = HistogramDataSetRenderer.newInstance();
        ANNOTATED_PL_FILTER = new AnnotatedNotPrecPeakListFilter();
        FILTERS_MANAGER = new SpectrumProcessorManager();
        FILTERS_MANAGER.add(ANNOTATED_PL_FILTER);
        SEQ_SHUFFLER = new SequenceShuffler();
    }

    Deliberator2(final String[] args) {
        paramManager = new DelibParamManager(this.getClass(), args);
    }

    public static void main(final String[] args) {
        long time = System.currentTimeMillis();
        long elapsedTime;
        final Deliberator2 app = new Deliberator2(args);
        try {
            AbstractMSWriter writer = null;
            String outFileExtension = JPLFile.getExtension(app.paramManager.getOutputFileName()).toLowerCase();
            if (outFileExtension.equals(SPTXTReader.EXTENSION)) {
                writer = app.new SPTXTDecoyWriter();
            } else if (outFileExtension.equals(MSPReader.EXTENSION)) {
                writer = app.new MSPDecoyWriter();
            } else {
                System.err.println("no writer for file " + app.paramManager.getOutputFileName());
                System.exit(1);
            }
            File inFile = new File(app.paramManager.getFilename());
            File outFile = new File(app.paramManager.getOutputFileName());
            if (app.paramManager.isConcatLibs()) {
                FileUtils.copyFile(inFile, outFile);
            }
            writer.enableAppendingMode(true);
            writer.open(outFile);
            if (app.paramManager.getLogFileName().length() > 0) {
                app.logWriter = new BufferedWriter(new FileWriter(app.paramManager.getLogFileName()));
            }
            if (app.paramManager.isRenderingEnabled()) {
                if (app.paramManager.isVerbose()) {
                    System.out.println("preparing filesystem for rendering...");
                }
                app.prepareFilesystem();
                elapsedTime = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
                if (app.paramManager.isVerbose()) {
                    System.out.println("done [" + elapsedTime + " ms]");
                }
            }
            app.mslibIter = app.parseMSLibFile();
            if (app.paramManager.getSamplingProbability() == -1) {
                if (app.paramManager.getDistFile() != null) {
                    MS1MS2_PEAKS_READER.parse(app.paramManager.getDistFile());
                    if (app.paramManager.isVerbose()) {
                        System.out.println(" computing max ratios from " + app.paramManager.getDistFile() + "...");
                    }
                    app.dists = MS1MS2_PEAKS_READER.toMS1MS2PeakRatioDists(app.paramManager.getDistFile());
                } else {
                    if (app.paramManager.isVerbose()) {
                        System.out.println("compute MS1/MS2 distributions...");
                    }
                    app.dists = new MS1MS2PeakDists.Builder(app.mslibIter, PL_TRANSFORMER).ms2BinWidth(1).ms1BinWidth(50).smoothingLevel(2).build();
                    elapsedTime = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();
                    if (app.paramManager.isVerbose()) {
                        System.out.println("done [" + elapsedTime + " ms]");
                    }
                }
                if (app.paramManager.isVerbose()) {
                    System.out.println("compute MS1/MS2 max ratios...");
                }
                app.computeMS1MS2Ratios();
                elapsedTime = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
                if (app.paramManager.isVerbose()) {
                    System.out.println("done [" + elapsedTime + " ms]");
                }
            }
            if (app.paramManager.isVerbose()) {
                System.out.println("sampling na peaks from mslib...");
            }
            app.mslibIter = app.parseMSLibFile();
            Map<Integer, HistogramDataSet> naPeaksSample = app.samplingNAPeaks(app.mslibIter);
            elapsedTime = System.currentTimeMillis() - time;
            time = System.currentTimeMillis();
            if (app.paramManager.isVerbose()) {
                System.out.println("done [" + elapsedTime + " ms]");
            }
            MS_MATCHER.setTolerance(app.paramManager.getTol());
            int countScan = 0;
            app.pb.setIndeterminate(false);
            app.pb.setMinimum(0);
            app.pb.setMaximum(count);
            if (app.paramManager.isVerbose()) {
                System.out.println("creating decoy mslib (" + count + " scans)...");
            }
            ((DecoyWriter) writer).enableDecoyTag(true);
            time = System.currentTimeMillis();
            app.mslibIter = app.parseMSLibFile();
            while (app.mslibIter.hasNext()) {
                MSScan scan = app.mslibIter.next();
                double score = 1.0;
                PeakList scanPl = scan.getPeakList();
                MSScan scanDecoy = null;
                PeakList decoyPl = null;
                PeakList decoyPlMin = null;
                double minScore = Double.MAX_VALUE;
                int countShuffling = 1;
                while ((countShuffling < MAX_SHUFFLING_TRIES) && (score >= app.paramManager.getDpt())) {
                    if (app.logWriter != null) {
                        app.logWriter.append("> scan " + scan.getScanNum() + ":\n");
                        final Peptide pept = scanPl.getPrecursor().getPeptide();
                        app.logWriter.append(pept.toAAString());
                    }
                    scanDecoy = app.createDecoySpectrum(scan, naPeaksSample);
                    decoyPl = scanDecoy.getPeakList();
                    if (app.logWriter != null) {
                        app.logWriter.append(" -> " + scanDecoy.getPeakList().getPrecursor().getPeptide().toAAString() + "\n");
                    }
                    final PeakList scanAnnotPl = FILTERS_MANAGER.transform(scanPl);
                    final PeakList decoyAnnotPl = FILTERS_MANAGER.transform(decoyPl);
                    MS_MATCHER.computeMatch(scanAnnotPl, decoyAnnotPl);
                    score = MS_MATCHER.getScore();
                    if (score < minScore) {
                        minScore = score;
                        decoyPlMin = decoyPl;
                    }
                    if (app.logWriter != null) {
                        app.logWriter.append("score = " + score + "\n");
                    }
                    countShuffling++;
                }
                decoyPl = decoyPlMin;
                if (app.logWriter != null) {
                    app.logWriter.append("min score = " + minScore + "\n");
                }
                if (app.paramManager.isRenderingEnabled()) {
                    MS_RENDERER.exportChart(scanPl, app.paramManager.getRenderDir() + "/scan/Delib-scan" + scan.getScanNum() + "_" + scanPl.getPrecursor().getPeptide().toAAString(), "scan-" + scan.getScanNum());
                    MS_RENDERER.exportChart(decoyPl, app.paramManager.getRenderDir() + "/scan/Delib-scan" + scan.getScanNum() + "_" + decoyPl.getPrecursor().getPeptide().toAAString() + "_shuffled", "decoy scan-" + scan.getScanNum());
                }
                writer.add(scanDecoy);
                writer.flush();
                countScan++;
                app.pb.setValue(countScan);
            }
            elapsedTime = System.currentTimeMillis() - time;
            time = System.currentTimeMillis();
            if (app.paramManager.isVerbose()) {
                System.out.println("decoy mslib done [" + elapsedTime + " ms]");
            }
            writer.close();
            if (app.logWriter != null) {
                app.logWriter.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void computeMS1MS2Ratios() throws IOException, ImageRenderingException, ParseException {
        mslibIter = parseMSLibFile();
        ratios = MS1MS2PeakDists.computeMaxRatios(dists, mslibIter, MSScan.TO_PEAKLIST);
        totalNumberOfRatios = ratios.length;
        if (paramManager.isRenderingEnabled()) {
            if (paramManager.isVerbose()) {
                System.out.println(" export ratios in " + paramManager.getRenderDir() + "...");
            }
            final HistogramDataSetExporter exporter = HistogramDataSetExporter.newInstance();
            exporter.export(ratios, paramManager.getRenderDir() + "/ratios.txt");
        }
    }

    /**
	 * Create directories to store histograms and spectrograms.
	 */
    private void prepareFilesystem() {
        File renderDir = new File(paramManager.getRenderDir());
        if (!renderDir.exists()) {
            if (paramManager.isVerbose()) {
                System.out.print(" mkdir " + renderDir.getPath() + "...");
            }
            renderDir.mkdir();
        }
        renderDir = new File(paramManager.getRenderDir() + "/hist");
        if (!renderDir.exists()) {
            if (paramManager.isVerbose()) {
                System.out.println(" mkdir " + renderDir.getPath() + "...");
            }
            renderDir.mkdir();
        }
        renderDir = new File(paramManager.getRenderDir() + "/scan");
        if (!renderDir.exists()) {
            if (paramManager.isVerbose()) {
                System.out.println(" mkdir " + renderDir.getPath() + "...");
            }
            renderDir.mkdir();
        }
        renderDir = new File(paramManager.getRenderDir() + "/scatterplot");
        if (!renderDir.exists()) {
            if (paramManager.isVerbose()) {
                System.out.println(" mkdir " + renderDir.getPath() + "...");
            }
            renderDir.mkdir();
        }
    }

    /**
	 * Parse msp/sptxt file.
	 * 
	 * @return a list of parsed scans.
	 * 
	 * @throws IOException if file cannot be read.
	 * @throws JPLParseException if parsing error while reading file.
	 */
    private AbstractExtraIterator<MSScan> parseMSLibFile() throws IOException, ParseException {
        Pattern fileExtPattern = Pattern.compile("msp|sptxt", Pattern.CASE_INSENSITIVE);
        MSReaderFacade parser = MSReaderFacade.withExtension(fileExtPattern);
        if (paramManager.isVerbose()) {
            pb = TerminalProgressBar.indeterminate();
            pb.setBarLength(20);
            pb.setSegmentLength(12);
            parser.setProgressBar(pb);
        }
        parser.parse(new File(paramManager.getFilename()));
        return parser.iterator();
    }

    /**
	 * Pick every non-annotated peaks and sample the distribution at given
	 * charges.
	 * 
	 * @param mslib the list of annotated MS spectra.
	 * @param charges create one histogram by charge.
	 * @return a map over histograms.
	 */
    private Map<Integer, HistogramDataSet> samplingNAPeaks(Iterator<MSScan> iter) {
        Map<Integer, HistogramDataSet.Builder> builders = new HashMap<Integer, HistogramDataSet.Builder>();
        Map<Integer, HistogramDataSet> dists = new HashMap<Integer, HistogramDataSet>();
        count = 0;
        while (iter.hasNext()) {
            count++;
            MSScan scan = iter.next();
            PeakList pl = scan.getPeakList();
            int charge = pl.getPrecursor().getCharge();
            if (!builders.containsKey(charge)) {
                builders.put(charge, new HistogramDataSet.Builder().keepValues().binWidth(paramManager.getNaPeakIntervalWidthForSampling()));
            }
            for (int i = 0; i < pl.size(); i++) {
                if (!pl.hasAnnotationsAt(i)) {
                    builders.get(charge).addValue(pl.getMzAt(i));
                }
            }
        }
        for (final int charge : builders.keySet()) {
            dists.put(charge, builders.get(charge).build());
        }
        if (paramManager.isRenderingEnabled()) {
            if (paramManager.isVerbose()) {
                System.out.println(" rendering na samples...");
            }
            for (final int charge : builders.keySet()) {
                if (paramManager.isVerbose()) {
                    System.out.println("  " + dists.get(charge).getValues(null).length + " NA peaks from " + charge + "+ precursors");
                }
                HISTO_RENDERER.exportChart(dists.get(charge), "histo +" + charge, paramManager.getRenderDir() + "/hist/naPeaksSample_z" + charge);
            }
            if (paramManager.isVerbose()) {
                System.out.println(" rendering done");
            }
        }
        return dists;
    }

    /**
	 * Create a decoy spectrum by shuffling precursor sequence.
	 * 
	 * @param mslibIter the list of annotated MS spectra.
	 * @param charges create one histogram by charge.
	 * @return a map over histograms.
	 * @throws IOException
	 * @throws ProcessException
	 */
    private MSScan createDecoySpectrum(final MSScan spectrum, final Map<Integer, HistogramDataSet> naSample) throws IOException, ProcessException {
        final PeakList pl = spectrum.getPeakList();
        final int precCharge = pl.getPrecursor().getCharge();
        final double precMz = pl.getPrecursor().getMz();
        Peptide shuffledPept = pl.getPrecursor().getPeptide();
        shuffledPept = SEQ_SHUFFLER.shuffle(shuffledPept);
        FRAGMENT_PROCESSOR.process(shuffledPept);
        final double[] oriMzs = pl.getMzs(null);
        final double[] newMzs = new double[pl.size()];
        final FragmentAnnotations newAnnotations = FragmentAnnotations.newInstance(pl.size());
        double pValue = 0;
        boolean isThresholdToCompute = true;
        if (paramManager.getSamplingProbability() >= 0) {
            pValue = paramManager.getSamplingProbability();
            isThresholdToCompute = false;
        }
        for (int i = 0; i < pl.size(); i++) {
            ExperimentalFragmentAnnotation annot = null;
            if (pl.hasAnnotationsAt(i)) {
                annot = (ExperimentalFragmentAnnotation) pl.getAnnotationsAt(i).get(0);
            }
            if (pl.hasAnnotationsAt(i) && annot.getModifications().getCount() == 0) {
                newAnnotations.addAnnotationAtPeakIndex(annot, i);
                newMzs[i] = FRAGMENT_PROCESSOR.getFragmentMzFromAnnot(annot);
                newMzs[i] += annot.getMzDiff();
            } else {
                if (isThresholdToCompute) {
                    final double ratio = dists.computeMaxIntensityRatio(oriMzs[i], precCharge, precMz);
                    int numberOfRightValues = Arrays.binarySearch(ratios, ratio);
                    if (numberOfRightValues < 0) {
                        numberOfRightValues = (-numberOfRightValues) - 1;
                    }
                    numberOfRightValues = totalNumberOfRatios - numberOfRightValues;
                    pValue = ((double) numberOfRightValues) / totalNumberOfRatios;
                }
                if (pl.hasAnnotationsAt(i)) {
                    final double rand = RANDOM.nextDouble();
                    if (rand < pValue / 2.0) {
                        newAnnotations.addAnnotationAtPeakIndex(annot, i);
                        newMzs[i] = FRAGMENT_PROCESSOR.getFragmentMzFromAnnot(annot);
                        newMzs[i] += annot.getMzDiff();
                    } else {
                        newMzs[i] = oriMzs[i];
                    }
                } else {
                    if (pValue == 0) {
                        newMzs[i] = oriMzs[i];
                    } else if (pValue == 1) {
                        newMzs[i] = pickRandomPeakMz(naSample.get(precCharge), oriMzs[i]);
                    } else {
                        final double rand = RANDOM.nextDouble();
                        if (rand < pValue / 3.0) {
                            newMzs[i] = pickRandomPeakMz(naSample.get(precCharge), oriMzs[i]);
                        } else {
                            newMzs[i] = oriMzs[i];
                        }
                    }
                }
            }
        }
        final List<Integer> indices = PrimitiveArrayUtils.asIntList(PrimitiveArrayUtils.sortIndices(newMzs));
        final PeakList peakList = new PeakListImpl.Builder(newMzs).intensities(pl.getIntensities(null)).annotations(newAnnotations).discardInvalidMzPeaks().msLevel(pl.getMSLevel()).precursor(new PeakImpl.Builder(shuffledPept).charge(precCharge).build()).indices(indices).build();
        return new MSScan.Builder(spectrum.getScanNum(), peakList).comment(spectrum.getComment()).build();
    }

    private double pickRandomPeakMz(final HistogramDataSet histo, final double mz) {
        int binIndex = histo.getBinIndex(mz);
        final double[] values = histo.getValuesAtBin(binIndex);
        final int randIdx = RANDOM.nextInt(values.length);
        return values[randIdx];
    }

    public static String addDecoyTagInComment(final String comment, final String decoyTag) {
        Pattern protPattern = Pattern.compile("Protein=([^\\s]+)\\s+");
        Matcher matcher = protPattern.matcher(comment);
        StringBuilder sb = new StringBuilder();
        int from = 0;
        while (matcher.find()) {
            String group = matcher.group(1);
            int to = matcher.start(1);
            if (group.charAt(0) == '"') {
                to++;
            }
            sb.append(comment.substring(from, to));
            sb.append(decoyTag);
            from = to;
        }
        if (sb.length() == 0) {
            sb.append("Protein=\"");
            sb.append(decoyTag);
            sb.append("\" ");
        }
        sb.append(comment.substring(from));
        return sb.toString();
    }

    public static String updateModifsInComment(final String comment, final String modifs) {
        final Matcher matcher = MODIF_PATTERN.matcher(comment);
        if (matcher.find()) {
            final int start = matcher.start(1);
            final int end = matcher.end(1);
            final StringBuilder sb = new StringBuilder();
            sb.append(comment.substring(0, start));
            sb.append(modifs);
            sb.append(comment.substring(end));
            return sb.toString();
        }
        throw new IllegalArgumentException("could not update comments");
    }

    interface DecoyWriter {

        void enableDecoyTag(final boolean bool);
    }

    class SPTXTDecoyWriter extends SPTXTWriter implements DecoyWriter {

        private boolean isDecoy;

        private SPTXTDecoyWriter() {
            super();
        }

        public void enableDecoyTag(final boolean bool) {
            isDecoy = bool;
        }

        @Override
        public String toString(final MSScan entry) {
            count = 0;
            final StringBuilder sb = new StringBuilder();
            final PeakList pl = entry.getPeakList();
            final Peak precursor = pl.getPrecursor();
            final Peptide peptide = precursor.getPeptide();
            double pepMw = 0;
            if (!peptide.isAmbiguous()) {
                pepMw = paramManager.getMassCalc().getMass(peptide);
            }
            final int numOfPeaks = pl.size();
            sb.append("Name: ");
            sb.append(peptide.getSymbolSequence() + "/" + precursor.getCharge() + "\n");
            sb.append(LIB_ID);
            sb.append(": ");
            sb.append(count++);
            sb.append("\n");
            sb.append("MW: " + paramManager.getFormatter().format(pepMw) + "\n");
            sb.append("PrecursorMZ: " + paramManager.getFormatter().format(precursor.getMz()) + "\n");
            sb.append("FullName: " + peptide.toStringNoCharge() + "/" + precursor.getCharge() + "\n");
            String comment = "Comment: ";
            if ((paramManager.getDecoyTag() != null) && isDecoy) {
                comment += addDecoyTagInComment(entry.getComment(), paramManager.getDecoyTag());
            } else {
                comment += entry.getComment();
            }
            if (peptide.hasModifs()) {
                final String modifs = SPTXTWriter.convert2ModsComment(peptide);
                comment = updateModifsInComment(comment, modifs);
            }
            if (peptide.isAmbiguous()) {
                comment += " PrecursorPeptideAmbiguous=true";
            }
            sb.append(comment + "\n");
            sb.append(SPTXTReader.NUMBER_PEAKS_TOKEN + ": " + numOfPeaks + "\n");
            for (int i = 0; i < pl.size(); i++) {
                sb.append(paramManager.getFormatter().format(pl.getMzAt(i)));
                sb.append("\t");
                sb.append(pl.getIntensityAt(i));
                sb.append("\t");
                if (!entry.hasAnnotationsAt(i)) {
                    sb.append("?");
                } else {
                    final ExperimentalFragmentAnnotation annot = (ExperimentalFragmentAnnotation) entry.getAnnotationsAt(i).get(0);
                    final String annotStr = annot.toString();
                    sb.append(annotStr);
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    class MSPDecoyWriter extends MSPWriter implements DecoyWriter {

        private boolean isDecoy;

        private MSPDecoyWriter() {
            super();
        }

        public void enableDecoyTag(final boolean bool) {
            isDecoy = bool;
        }

        @Override
        public String toString(final MSScan entry) {
            final StringBuilder sb = new StringBuilder();
            final PeakList pl = entry.getPeakList();
            final Peak precursor = pl.getPrecursor();
            final Peptide peptide = precursor.getPeptide();
            double pepMw = 0;
            if (!peptide.isAmbiguous()) {
                pepMw = paramManager.getMassCalc().getMass(peptide);
            }
            final int numOfPeaks = pl.size();
            sb.append("Name: ");
            sb.append(peptide.getSymbolSequence() + "/" + precursor.getCharge() + "\n");
            sb.append("MW: " + paramManager.getFormatter().format(pepMw) + "\n");
            String comment = "Comment: ";
            if ((paramManager.getDecoyTag() != null) && isDecoy) {
                comment += addDecoyTagInComment(entry.getComment(), paramManager.getDecoyTag());
            } else {
                comment += entry.getComment();
            }
            if (peptide.hasModifs()) {
                final String modifs = SPTXTWriter.convert2ModsComment(peptide);
                comment = updateModifsInComment(comment, modifs);
            }
            if (peptide.isAmbiguous()) {
                comment += " PrecursorPeptideAmbiguous=true";
            }
            sb.append(comment + "\n");
            sb.append(MSPReader.NUMBER_PEAKS_TOKEN + ": " + numOfPeaks + "\n");
            for (int i = 0; i < pl.size(); i++) {
                sb.append(paramManager.getFormatter().format(pl.getMzAt(i)));
                sb.append("\t");
                sb.append(pl.getIntensityAt(i));
                sb.append("\t\"");
                if (!entry.hasAnnotationsAt(i)) {
                    sb.append("?");
                } else {
                    final ExperimentalFragmentAnnotation annot = (ExperimentalFragmentAnnotation) entry.getAnnotationsAt(i).get(0);
                    final String annotStr = annot.toString();
                    sb.append(annotStr);
                }
                sb.append("\"\n");
            }
            return sb.toString();
        }
    }
}
