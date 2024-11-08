package org.expasy.jpl.tools.msproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.expasy.jpl.commons.app.AbstractApplicationParameters;
import org.expasy.jpl.commons.app.JPLTerminalApplication;
import org.expasy.jpl.commons.base.Registrable;
import org.expasy.jpl.commons.base.TypedDatum.IncompatibleTypedDatumException;
import org.expasy.jpl.commons.base.cond.Condition;
import org.expasy.jpl.commons.base.cond.ConditionImpl;
import org.expasy.jpl.commons.base.cond.operator.impl.OperatorBelongs;
import org.expasy.jpl.commons.base.math.StraightLineFitter;
import org.expasy.jpl.commons.collection.PrimitiveArrayUtils;
import org.expasy.jpl.commons.collection.graph.Edge;
import org.expasy.jpl.commons.collection.graph.KPartiteGraphImpl;
import org.expasy.jpl.commons.collection.graph.Node;
import org.expasy.jpl.core.mol.chem.ChemicalFacade;
import org.expasy.jpl.core.mol.chem.MassCalculator;
import org.expasy.jpl.core.mol.modif.ModificationFactory;
import org.expasy.jpl.core.ms.spectrum.filter.SpectrumProcessorManager;
import org.expasy.jpl.io.ms.MSScan;
import org.expasy.jpl.io.ms.reader.MSDispatcher;
import org.expasy.jpl.msident.format.pepxml.io.MSnRun;
import org.expasy.jpl.msident.format.pepxml.io.PepXMLReader;
import org.expasy.jpl.msident.model.api.AbstractMSIdentNode;
import org.expasy.jpl.msident.model.impl.MSSCandidate;
import org.expasy.jpl.msident.model.impl.PeptideCandidate;
import org.expasy.jpl.msident.model.impl.result.PeptideMatchingScore;
import org.expasy.jpl.msmatch.PeakListMatcherImpl;
import org.expasy.jpl.msmatch.scorer.NCorrScorer;
import edu.uci.ics.jung.graph.util.Pair;

public class SpectrumProcessor implements JPLTerminalApplication {

    /** Logger */
    Log log = LogFactory.getLog(SpectrumProcessor.class);

    private static MassCalculator MONO_MASS_CALC = MassCalculator.getMonoAccuracyInstance();

    private static double CALIB_SLOPE = 1.0;

    private static double CALIB_OFFSET = 0.0;

    private Parameters params;

    public SpectrumProcessor(String[] args) {
        params = new Parameters(SpectrumProcessor.class, args);
    }

    /**
	 * main() of clustering package
	 * 
	 * @param args Command line arguments args[0] Filename of xml parameter file
	 * @throws Exception
	 * @throws JPLReaderException, IOException
	 */
    public static void main(String[] args) throws Exception {
        SpectrumProcessor app = new SpectrumProcessor(args);
        app.run();
    }

    private static Set<String> readPSM(File pepXMLFile, double probScoreThreshold) throws ParseException {
        PepXMLReader pepXMLReader = null;
        Set<String> peptides = new HashSet<String>();
        try {
            pepXMLReader = new PepXMLReader();
            pepXMLReader.parse(new File(pepXMLFile.getAbsolutePath()));
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
        }
        for (MSnRun msnRun : pepXMLReader) {
            KPartiteGraphImpl<Node, Edge> graph = msnRun.getGraph();
            Map<String, PeptideCandidate> map = msnRun.getPeptideCandidates();
            Set<String> peptideIds = map.keySet();
            for (String peptideId : peptideIds) {
                PeptideCandidate peptide = map.get(peptideId);
                if (peptide.getPeptide().hasModifs() && peptide.getPeptide().getModifs().getNumOfModif(ModificationFactory.valueOf("79.9799")) >= 1) {
                    Collection<Edge> psms = msnRun.getGraph().getIncidentEdges(peptide, AbstractMSIdentNode.MS_NODE_PREDICATE);
                    for (Edge psm : psms) {
                        Pair<Node> pair = graph.getEndpoints(psm);
                        Registrable msNode = null;
                        if (pair.getFirst() instanceof MSSCandidate) {
                            msNode = pair.getFirst();
                        } else {
                            msNode = pair.getSecond();
                        }
                        if (msNode instanceof MSSCandidate) {
                            MSSCandidate ms = (MSSCandidate) msNode;
                            try {
                                if (psm.getTypedVariable("peptideprophet", PeptideMatchingScore.NUMBER_TYPE).getValue().doubleValue() >= probScoreThreshold) {
                                    peptides.add(ms.getName());
                                }
                            } catch (IncompatibleTypedDatumException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return peptides;
    }

    private static Map<String, ArrayList<Double>> readPSM4Calib(File pepXMLFile, File spectrumFile, double probScoreThreshold, double mzTol) throws ParseException {
        PepXMLReader pepXMLReader = null;
        String fileId = spectrumFile.getName();
        fileId = fileId.replaceFirst("\\.[^\\.]+$", "");
        double massH = 0.0;
        try {
            massH = MONO_MASS_CALC.getMass(ChemicalFacade.getMolecule("H(+)"));
        } catch (ParseException e) {
        }
        try {
            pepXMLReader = new PepXMLReader();
            pepXMLReader.parse(new File(pepXMLFile.getAbsolutePath()));
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
        }
        ArrayList<Double> refMzs = new ArrayList<Double>();
        ArrayList<Double> measMzs = new ArrayList<Double>();
        for (MSnRun msnRun : pepXMLReader) {
            KPartiteGraphImpl<Node, Edge> graph = msnRun.getGraph();
            Map<String, PeptideCandidate> map = msnRun.getPeptideCandidates();
            Set<String> peptideIds = map.keySet();
            for (String peptideId : peptideIds) {
                PeptideCandidate peptide = map.get(peptideId);
                Collection<Edge> psms = msnRun.getGraph().getIncidentEdges(peptide, AbstractMSIdentNode.MS_NODE_PREDICATE);
                for (Edge psm : psms) {
                    Pair<Node> pair = graph.getEndpoints(psm);
                    Registrable msNode = null;
                    if (pair.getFirst() instanceof MSSCandidate) {
                        msNode = pair.getFirst();
                    } else {
                        msNode = pair.getSecond();
                    }
                    if (msNode instanceof MSSCandidate) {
                        MSSCandidate ms = (MSSCandidate) msNode;
                        if (ms.getName().startsWith(fileId) && ((Double) psm.getVariable("peptideprophet").getValue()) >= probScoreThreshold) {
                            double mzRef = MONO_MASS_CALC.getMz(peptide.getPeptide());
                            double mzMeas = ms.getPrecursorMass() / ms.getPrecursorCharge() + massH;
                            if (Math.abs(mzRef - mzMeas) < mzTol) {
                                refMzs.add(mzRef);
                                measMzs.add(mzMeas);
                            }
                        }
                    }
                }
            }
        }
        HashMap<String, ArrayList<Double>> container = new HashMap<String, ArrayList<Double>>();
        container.put("reference", refMzs);
        container.put("measured", measMzs);
        return container;
    }

    private static void createMS2SimMatrix(MSDispatcher dispatcher, ConverterXMLReader reader, PrintStream out) throws ParseException, IOException {
        out.println("Create similarity matrix between runs");
        dispatcher.setCombineFilesFlag(false);
        double precMzTol = reader.getPrecursorMzTol();
        double fragMzTol = reader.getFragmentMzTol();
        ArrayList<MSScan> spectrumBatch_i = new ArrayList<MSScan>();
        ArrayList<MSScan> spectrumBatch_j = new ArrayList<MSScan>();
        ArrayList<File> files = dispatcher.getFiles();
        double[][] simMatrix = new double[files.size()][files.size()];
        for (int i = 0; i < files.size(); i++) {
            simMatrix[i][i] = 1.0;
            dispatcher.getNextSpectrumBatch(spectrumBatch_i, files.get(i));
            HashMap<Integer, ArrayList<MSScan>> sorted_i = sortSpectraByChargeMass(spectrumBatch_i);
            for (int j = i + 1; j < files.size(); j++) {
                out.println("Calculate similarty between : " + files.get(i).getName() + " - " + files.get(j).getName());
                dispatcher.getNextSpectrumBatch(spectrumBatch_j, files.get(j));
                HashMap<Integer, ArrayList<MSScan>> sorted_j = sortSpectraByChargeMass(spectrumBatch_j);
                simMatrix[i][j] = calcMS2Similarity(sorted_i, sorted_j, precMzTol, fragMzTol);
                simMatrix[j][i] = simMatrix[i][j];
            }
        }
        FileWriter siMatFile;
        try {
            siMatFile = new FileWriter(reader.getMGFDir() + reader.getOutputFilePrefix() + "SimilarityMatrix.csv", false);
            siMatFile.write("");
            siMatFile.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        try {
            siMatFile = new FileWriter(reader.getMGFDir() + reader.getOutputFilePrefix() + "SimilarityMatrix.csv", true);
            for (int i = 0; i < files.size(); i++) {
                siMatFile.write(files.get(i).getName());
                if (i < files.size() - 1) {
                    siMatFile.write(",");
                } else {
                    siMatFile.write("\n");
                    siMatFile.flush();
                }
            }
            for (int i = 0; i < files.size(); i++) {
                for (int j = 0; j < files.size(); j++) {
                    siMatFile.write(String.format("%.5f", simMatrix[i][j]));
                    if (j < files.size() - 1) {
                        siMatFile.write(",");
                    } else {
                        siMatFile.write("\n");
                        siMatFile.flush();
                    }
                }
            }
            siMatFile.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    private static void writeSpectra2Mgf(ArrayList<MSScan> spectra, String batchTag, ConverterXMLReader reader, MSDispatcher dispatcher, boolean append) throws IOException {
        String name;
        if (dispatcher.getCombineFilesFlag()) {
            name = reader.getOutputFilePrefix() + batchTag + ".mgf";
        } else {
            String filename = dispatcher.getProcessedFile().getName();
            filename = Pattern.compile("mzXML$", Pattern.CASE_INSENSITIVE).matcher(filename).replaceAll("mgf");
            name = reader.getOutputFilePrefix() + batchTag + filename;
        }
        FileWriter mgf;
        if (!append) {
            try {
                mgf = new FileWriter(reader.getMGFDir() + name, false);
                mgf.write("");
                mgf.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }
        try {
            mgf = new FileWriter(reader.getMGFDir() + name, true);
            boolean begin = true;
            for (MSScan spectrum : spectra) {
                if (!begin) mgf.write("\n\n");
                if (spectrum.getPeakList().getPrecursor() == null) continue;
                mgf.write("BEGIN IONS" + "\n");
                mgf.write("TITLE=" + spectrum.getTitle() + "\n");
                double mz = spectrum.getPeakList().getPrecursor().getMz();
                mz = CALIB_OFFSET + CALIB_SLOPE * mz;
                mgf.write("PEPMASS=" + mz + "\n");
                mgf.write("CHARGE=" + spectrum.getPeakList().getPrecursor().getCharge() + "\n");
                int n = spectrum.getPeakList().size();
                for (int q = 0; q < n; q++) {
                    mgf.write("" + String.format("%.3f", spectrum.getPeakList().getMzAt(q)) + "\t" + String.format("%.3f", spectrum.getPeakList().getIntensityAt(q)) + "\n");
                }
                mgf.write("END IONS" + "\n");
                mgf.flush();
                begin = false;
            }
            mgf.flush();
            mgf.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static ArrayList<MSScan> mappSpectra(ArrayList<MSScan> spectra, int[] indices) {
        if (indices.length > spectra.size()) {
            throw new IllegalArgumentException("too many indices");
        }
        ArrayList<MSScan> values = new ArrayList<MSScan>(indices.length);
        int i = 0;
        for (int index : indices) {
            values.add(i, spectra.get(index));
            i++;
        }
        return values;
    }

    private static ArrayList<MSScan> sortSpectraByMass(ArrayList<MSScan> toBeSortedSpectra) {
        double[] parentMasses = new double[toBeSortedSpectra.size()];
        int i = 0;
        for (MSScan spectrum : toBeSortedSpectra) {
            parentMasses[i] = spectrum.getPeakList().getPrecursor().getMz();
            i++;
            if (spectrum.getPeakList().size() == 0) {
                continue;
            }
        }
        final int[] indices = PrimitiveArrayUtils.sortIndexesUp(parentMasses);
        return mappSpectra(toBeSortedSpectra, indices);
    }

    private static HashMap<Integer, ArrayList<MSScan>> sortSpectraByChargeMass(ArrayList<MSScan> spectra) {
        HashMap<Integer, ArrayList<MSScan>> sorted = new HashMap<Integer, ArrayList<MSScan>>();
        for (MSScan spectrum : spectra) {
            int charge = spectrum.getPeakList().getPrecursor().getCharge();
            if (sorted.containsKey(charge)) {
                sorted.get(charge).add(spectrum);
            } else {
                ArrayList<MSScan> specPerCharge = new ArrayList<MSScan>();
                specPerCharge.add(spectrum);
                sorted.put(charge, specPerCharge);
            }
        }
        Set<Integer> charges = sorted.keySet();
        for (Integer ch : charges) {
            ArrayList<MSScan> sortedSpectra = sortSpectraByMass(sorted.get(ch));
            sorted.put(ch, sortedSpectra);
        }
        return sorted;
    }

    private static double calcMS2Similarity(HashMap<Integer, ArrayList<MSScan>> sortedSpectra1, HashMap<Integer, ArrayList<MSScan>> sortedSpectra2, double precMzTol, double fragMzTol) {
        if (sortedSpectra1 == null || sortedSpectra2 == null || sortedSpectra1.size() == 0 || sortedSpectra2.size() == 0) {
            return 0.0;
        }
        PeakListMatcherImpl matcher = PeakListMatcherImpl.withTol(fragMzTol);
        matcher.setScorer(NCorrScorer.getInstance());
        double matchScore = 0.0;
        int nrSpectra = 0;
        Set<Integer> charges = sortedSpectra1.keySet();
        for (Integer ch : charges) {
            ArrayList<MSScan> spectra1 = sortedSpectra1.get(ch);
            ArrayList<MSScan> spectra2 = sortedSpectra2.get(ch);
            if (spectra1 == null) continue;
            if (spectra2 == null) continue;
            int jMin = 0;
            double mzMin = spectra2.get(jMin).getPeakList().getPrecursor().getMz();
            int jMax = 0;
            double mzMax = mzMin;
            for (int i = 0; i < spectra1.size(); i++) {
                MSScan sp1 = spectra1.get(i);
                double mz1 = sp1.getPeakList().getPrecursor().getMz();
                while (jMin < spectra2.size() && mzMin < mz1 - precMzTol) {
                    jMin++;
                    if (jMin < spectra2.size()) {
                        mzMin = spectra2.get(jMin).getPeakList().getPrecursor().getMz();
                    }
                }
                jMax = Math.max(jMin, jMax);
                mzMax = Math.max(mzMin, mzMax);
                while (jMax < spectra2.size() && mzMax < mz1 + precMzTol) {
                    jMax++;
                    if (jMax < spectra2.size()) {
                        mzMax = spectra2.get(jMax).getPeakList().getPrecursor().getMz();
                    }
                }
                for (int j = jMin; j < jMax; j++) {
                    matcher.computeMatch(spectra1.get(i).getPeakList(), spectra2.get(j).getPeakList());
                    matchScore += matcher.getScore();
                }
            }
            nrSpectra += spectra1.size() + spectra2.size();
        }
        matchScore = matchScore / nrSpectra;
        return matchScore;
    }

    private static Condition<MSScan> removeTitleFilter(Set<String> titles) {
        Transformer<MSScan, String> sp2title = new Transformer<MSScan, String>() {

            public String transform(MSScan spec) {
                String title = spec.getTitle();
                if (title == null) {
                    return "null";
                }
                return title;
            }
        };
        return new ConditionImpl.Builder<MSScan, Set<String>>(titles).accessor(sp2title).operator(OperatorBelongs.newInstance()).not().build();
    }

    private static Condition<MSScan> retainTitleFilter(Set<String> titles) {
        Transformer<MSScan, String> sp2title = new Transformer<MSScan, String>() {

            public String transform(MSScan spec) {
                String title = spec.getTitle();
                if (title == null) {
                    return "null";
                }
                return title;
            }
        };
        return new ConditionImpl.Builder<MSScan, Set<String>>(titles).accessor(sp2title).operator(OperatorBelongs.newInstance()).build();
    }

    @Override
    public AbstractApplicationParameters getParameters() {
        return null;
    }

    @Override
    public void run() throws Exception {
        ConverterXMLReader reader = new ConverterXMLReader(params.getParamFile().getAbsolutePath());
        PrintStream out = reader.getPrintOutputDevice();
        Date dateNow = new Date();
        out.println("************************************************************************");
        out.println("*                                                                      *");
        out.println("*                         JPLSpectrumConverter                         *");
        out.println("*               MS/MS spectrum filtering and editing tool              *");
        out.println("*                              Version 1.0                             *");
        out.println("*                                                                      *");
        out.println("************************************************************************");
        out.println();
        out.println("Start time: " + dateNow.toString());
        out.println();
        String directory = reader.getInputSpectrumFileDirectory();
        String regex = reader.getInputSpectrumFileRegExp();
        SpectrumProcessorManager spectrumTransformer = reader.getSpectrumFilterManager();
        MSDispatcher dispatcher = new MSDispatcher(directory, regex);
        dispatcher.setSpectrumTransformer(spectrumTransformer);
        dispatcher.setMaxNrOfSpectra(reader.getMaximalNrOfSpectraPerBatch());
        boolean combineSpectra = reader.getCombineFilesFlag();
        if (reader.calibrateSpectra) combineSpectra = false;
        dispatcher.setCombineFilesFlag(combineSpectra);
        dispatcher.setSpectrumQualityFilter(reader.getSpectrumQualityFilter());
        dispatcher.setPrecursorCondition(reader.getPrecursorCondition());
        dispatcher.setMSLevelCondition(reader.getMSLevelCondition());
        if (reader.getRemoveIdentifiedSpectraFlag()) {
            System.out.println("Remove Spectra from " + reader.getRemovePepXMLFile().getAbsolutePath());
            Set<String> specIdents = readPSM(reader.getRemovePepXMLFile(), reader.getRemoveProbScoreThreshold());
            dispatcher.setTitleCondition(removeTitleFilter(specIdents));
        }
        if (reader.getRetainIdentifiedSpectraFlag()) {
            System.out.println("Retain Spectra from " + reader.getRemovePepXMLFile().getAbsolutePath());
            Set<String> specIdents = readPSM(reader.getRetainPepXMLFile(), reader.getRetainProbScoreThreshold());
            dispatcher.setTitleCondition(retainTitleFilter(specIdents));
        }
        dispatcher.buildSpectrumCondition();
        int cnt = 1;
        int totCnt = 0;
        if (reader.getWriteMGFFileFlag()) {
            ArrayList<MSScan> spectrumBatch = new ArrayList<MSScan>();
            while (dispatcher.getNextSpectrumBatch(spectrumBatch)) {
                totCnt += spectrumBatch.size();
                out.println();
                out.println("Batch " + cnt + " (" + spectrumBatch.size() + " spectra)" + " of file " + dispatcher.getProcessedFile().getAbsolutePath() + " is being processed.");
                String batchStr = "";
                if (!dispatcher.getReadInOneBatchFlag()) {
                    batchStr = "Batch" + cnt + "_";
                }
                if (reader.getCalibrateSpectraFlag()) {
                    System.out.println("Calibrate Spectra from " + dispatcher.getProcessedFile().getAbsolutePath());
                    Map<String, ArrayList<Double>> mzMap = readPSM4Calib(reader.getCalibratePepXMLFile(), dispatcher.getProcessedFile(), reader.getCalibrateProbScoreThreshold(), reader.getCalibratePrecError());
                    FileWriter mzFile = null;
                    try {
                        String name = dispatcher.getProcessedFile().getName();
                        name = name.replace(".mgf", "_calib.txt");
                        mzFile = new FileWriter(reader.getMGFDir() + name, false);
                        mzFile.write("measuredMz,theorMz,calibMz\n");
                    } catch (Exception e) {
                    }
                    ArrayList<Double> refMzs = mzMap.get("reference");
                    ArrayList<Double> measMzs = mzMap.get("measured");
                    StraightLineFitter fitter = new StraightLineFitter(measMzs, refMzs);
                    CALIB_SLOPE = fitter.getFitSlope();
                    CALIB_OFFSET = fitter.getFitOffset();
                    for (int i = 0; i < refMzs.size(); i++) {
                        double mz = measMzs.get(i);
                        mz = CALIB_OFFSET + CALIB_SLOPE * mz;
                        mzFile.write(measMzs.get(i).toString() + "," + refMzs.get(i).toString() + "," + mz + "\n");
                        mzFile.flush();
                    }
                    mzFile.close();
                    System.out.println("mz = " + CALIB_OFFSET + " + mz*" + CALIB_SLOPE);
                }
                writeSpectra2Mgf(spectrumBatch, batchStr, reader, dispatcher, false);
                spectrumBatch.clear();
                cnt++;
            }
            out.println();
            if (!dispatcher.getReadInOneBatchFlag()) out.println(totCnt + " spectra were processed.");
            dispatcher.reset();
        }
        if (reader.getCreateMS2SimMatrixFlag()) {
            createMS2SimMatrix(dispatcher, reader, out);
        }
        out.println();
        dateNow = new Date();
        out.println("End time: " + dateNow.toString());
    }
}
