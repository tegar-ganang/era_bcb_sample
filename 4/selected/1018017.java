package spindles.api.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spindles.api.db.DAO;
import spindles.api.db.DBGateway;
import spindles.api.db.PersonDAO;
import spindles.api.db.SessionPartDAO;
import spindles.api.db.SleepSessionDAO;
import spindles.api.domain.CutoffFrequency;
import spindles.api.domain.Epoch;
import spindles.api.domain.Interval;
import spindles.api.domain.PerformanceEvaluation;
import spindles.api.domain.Person;
import spindles.api.domain.SamplingRate;
import spindles.api.domain.SessionPart;
import spindles.api.domain.Settings;
import spindles.api.domain.SleepSession;
import spindles.api.domain.Spindle;
import spindles.api.domain.SpindleIndication;
import spindles.api.domain.ThresholdGroup;
import spindles.api.domain.User;
import spindles.api.domain.ThresholdGroup.ThresholdGroupName;
import spindles.api.util.ApplicationException;
import spindles.api.util.ErrorMessages;
import spindles.api.util.FileUtil;
import spindles.api.util.UserException;
import spindles.api.util.Util;
import com.mathworks.toolbox.javabuilder.webfigures.WebFigure;

public class ExportService {

    final Logger log = LoggerFactory.getLogger(ExportService.class);

    private DBGateway db = new DBGateway();

    private PersonDAO personDAO = new PersonDAO();

    private DAO<Epoch> epochDAO = new DAO<Epoch>(Epoch.class, "sessionPartID");

    private SessionPartDAO partDAO = new SessionPartDAO();

    private SleepSessionDAO sleepDAO = new SleepSessionDAO();

    private SortedMap<Integer, CutoffFrequency> freq = new TreeMap<Integer, CutoffFrequency>();

    private boolean singleEpochPerPage;

    private boolean[] filtered = new boolean[3];

    private int spindleType;

    public ExportService() {
        this.spindleType = Spindle.SPINDLE_INDICATION;
    }

    public ExportService(int spindleType) {
        this.spindleType = spindleType;
    }

    public ExportService(SortedMap<Integer, CutoffFrequency> freq, boolean singleEpochPerPage, int spindleType) {
        Validate.notEmpty(freq);
        this.freq = freq;
        this.singleEpochPerPage = singleEpochPerPage;
        this.spindleType = spindleType;
        setFiltered();
    }

    public ExportService(CutoffFrequency first, CutoffFrequency second, CutoffFrequency third, boolean singleEpochPerPage, int spindleType) {
        Validate.notNull(first);
        Validate.notNull(second);
        Validate.notNull(third);
        freq.put(1, first);
        freq.put(2, second);
        freq.put(3, third);
        this.singleEpochPerPage = singleEpochPerPage;
        this.spindleType = spindleType;
        setFiltered();
    }

    public static ExportService newExportServiceWithFilteredData(SamplingRate sr) {
        return new ExportService(CutoffFrequency.ALL_FREQ, CutoffFrequency.getCutoffFreq_6_21(sr), CutoffFrequency.getCutoffFreq(sr), true, Spindle.SPINDLE_INDICATION);
    }

    public static ExportService newExportServiceWithRawData() {
        SortedMap<Integer, CutoffFrequency> map = new TreeMap<Integer, CutoffFrequency>();
        map.put(1, CutoffFrequency.ALL_FREQ);
        return new ExportService(map, true, Spindle.SPINDLE_INDICATION);
    }

    public void setSpindleType(int spindleType) {
        this.spindleType = spindleType;
    }

    protected void setFiltered() {
        int counter = 0;
        for (CutoffFrequency fr : freq.values()) {
            filtered[counter++] = !(fr == CutoffFrequency.ALL_FREQ);
        }
    }

    protected WebFigure doWebExport(WebFigureExport exp) throws UserException {
        User user = db.getUser(User.DEFAULT_USERNAME);
        int samplesCount = Epoch.getSamplesCount(SamplingRate.toEnum(db.getSamplingRate(exp.getEpoch(0).getSessionPartID())));
        double[][] data = new double[exp.size()][samplesCount];
        Object[] spindles = new Object[exp.size()];
        String[] plotTitles = new String[exp.size()];
        Epoch e = null;
        for (int i = exp.size() - 1; i >= 0; i--) {
            e = exp.getEpoch(i);
            data[i] = exp.getData(i);
            if (spindleType == Spindle.SPINDLE_INDICATION) {
                spindles[i] = e.getSpindleIndicationsForMatlab(db.getDefaultThresholdGroup(), exp.withSoftCriterion());
            } else {
                spindles[i] = e.getVDSpindlesForMatlab(user);
            }
            plotTitles[i] = getPlotTitle(exp.getLabel(i), e);
        }
        Settings settings = new SettingsService().getSettings();
        WebFigure result = MatlabGateway.plotEEGWeb(data, db.getSamplingRate(e.getSessionPartID()), spindles, plotTitles, getReportTitle(e.getSessionPartID()), settings.getPlotWidth(), settings.getPlotHeight(), new boolean[] { filtered[2], filtered[1], filtered[0] });
        return result;
    }

    private String getPlotTitle(String label, Epoch e) {
        StringBuilder sb = new StringBuilder();
        sb.append(label);
        sb.append(e.toPrettyString());
        return sb.toString();
    }

    protected int doPlotExport(PlotExport exp, String fileName, boolean append, int index) throws UserException {
        User user = db.getUser(User.DEFAULT_USERNAME);
        int samplesCount = Epoch.getSamplesCount(SamplingRate.toEnum(db.getSamplingRate(exp.getEpoch(0).getSessionPartID())));
        double[][] data = new double[exp.size()][samplesCount];
        Object[] spindles = new Object[exp.size()];
        String[] plotTitles = new String[exp.size()];
        Epoch e = null;
        for (int i = exp.size() - 1; i >= 0; i--) {
            e = exp.getEpoch(i);
            data[i] = exp.getData(i);
            if (spindleType == Spindle.SPINDLE_INDICATION) {
                spindles[i] = e.getSpindleIndicationsForMatlab(db.getDefaultThresholdGroup(), exp.withSoftCriterion());
            } else {
                spindles[i] = e.getVDSpindlesForMatlab(user);
            }
            plotTitles[i] = getPlotTitle(singleEpochPerPage ? index : index + i, exp.getLabel(i), e);
        }
        MatlabGateway.plotEEG(data, db.getSamplingRate(e.getSessionPartID()), spindles, plotTitles, getReportTitle(e.getSessionPartID()), new String[] { exp.getFormat().toString() }, fileName, append, new boolean[] { filtered[2], filtered[1], filtered[0] });
        return index + (exp.size() - 1);
    }

    protected void doDataExport(String dirName, String extension, Person person, DataExport exp) throws UserException {
        String dir = FileUtil.writeToDir(FilenameUtils.getFullPath(dirName) + person.getLastName() + person.getId() + System.getProperty("file.separator"));
        SortedSet<SessionPart> parts = db.findAllSorted(person.getId());
        int nparts = 0;
        for (SessionPart part : parts) {
            nparts++;
            SortedSet<Epoch> epochs = epochDAO.findAllSorted(part.getId());
            int counter = 0;
            for (Epoch e : epochs) {
                counter++;
                File file = new File(FilenameUtils.getFullPath(dir) + nparts + "-" + counter + extension);
                exp.writeData(exp.getData(e), file);
            }
        }
    }

    private String getSpindlesReport(String reportBody, int epochsCount, int spindlesCount) {
        String header = "Epoch Start Time\t" + "Spindle Absolute Start Time\t" + "Spindle Relative Start Time\t" + "Spindle Absolute End Time\t" + "Spindle Relative End Time\t" + "Spindle Duration\t" + "ISI Duration" + System.getProperty("line.separator");
        String trailer = "No of Epochs: " + epochsCount + "\t" + "No of spindles: " + spindlesCount;
        return header + reportBody + trailer;
    }

    public List<PerformanceEvaluation> getPerfEvalForGroup(ThresholdGroup group, boolean softCriterion) {
        List<PerformanceEvaluation> result = new ArrayList<PerformanceEvaluation>();
        SortedSet<Person> persons = personDAO.findAllSorted();
        List<String> kopsExcludedEpochs = Arrays.asList(new String[] { "01:15:22 - 01:15:38", "01:15:54 - 01:16:10", "03:26:32 - 03:26:48", "06:32:31 - 06:32:47", "06:35:59 - 06:36:15" });
        List<String> nikasExcludedEpochs = Arrays.asList(new String[] { "06:25:26 - 06:25:42", "06:25:58 - 06:26:14", "09:28:59 - 09:29:15", "09:29:31 - 09:29:47", "11:27:17 - 11:27:33", "11:31:49 - 11:32:05" });
        List<String> pierExcludedEpochs = Arrays.asList(new String[] { "01:52:51 - 01:53:07", "01:54:27 - 01:54:43", "04:32:08 - 04:32:24", "04:35:20 - 04:35:36", "06:28:10 - 06:28:26", "06:31:06 - 06:31:22" });
        for (Person p : persons) {
            SortedSet<SessionPart> parts = db.findAllSorted(p.getId());
            int partIndex = 0;
            String partName = null;
            for (SessionPart part : parts) {
                partIndex++;
                switch(partIndex) {
                    case 1:
                        partName = "1st";
                        break;
                    case 2:
                        partName = "2nd";
                        break;
                    case 3:
                        partName = "3rd";
                        break;
                    default:
                        throw new ApplicationException("Three parts expected");
                }
                SortedSet<Epoch> epochs = epochDAO.findAllSorted(part.getId());
                for (Epoch epoch : epochs) {
                    if (p.getLastName().equals("Kopsaftis") && kopsExcludedEpochs.contains(epoch.toPrettyString())) {
                        continue;
                    }
                    if (p.getLastName().equals("Valogiannis") && nikasExcludedEpochs.contains(epoch.toPrettyString())) {
                        continue;
                    }
                    if (p.getLastName().equals("Pierros") && pierExcludedEpochs.contains(epoch.toPrettyString())) {
                        continue;
                    }
                    PerformanceEvaluation pe = epoch.evaluatePerformance(group, db.getUser(User.DEFAULT_USERNAME), softCriterion);
                    pe.setPersonName(p.getName());
                    pe.setPart(partName);
                    pe.setEpoch(epoch.toPrettyString());
                    result.add(pe);
                }
            }
        }
        return result;
    }

    public void exportDetailedPerfEvaluation(String dirName) {
        DAO<ThresholdGroup> thresholdDAO = new DAO<ThresholdGroup>(ThresholdGroup.class, String.class, "groupName");
        SortedSet<ThresholdGroup> groups = thresholdDAO.findAllSorted();
        ExportService exp = new ExportService();
        List<String> groupNames = Arrays.asList(new String[] { ThresholdGroupName.GROUP_5_3.name(), ThresholdGroupName.GROUP_6_3.name(), ThresholdGroupName.GROUP_7_3.name(), ThresholdGroupName.GROUP_8_3.name(), ThresholdGroupName.GROUP_9_3.name() });
        for (ThresholdGroup group : groups) {
            if (groupNames.contains(group.getGroupName())) {
                exp.exportDetailedPerfEvaluation(dirName, "detailed_perf_eval_" + group.getGroupName() + "_soft.xls", group, true);
            }
            exp.exportDetailedPerfEvaluation(dirName, "detailed_perf_eval_" + group.getGroupName() + ".xls", group, false);
        }
    }

    public void exportDetailedPerfEvaluation(String dirName, String filename, ThresholdGroup group, boolean softCriterion) {
        try {
            String title = softCriterion ? group.getSoftCriterionDescription() : group.getHardCriterionDescription();
            String header = title + System.getProperty("line.separator") + "Person\t" + "Part\t" + "Epoch\t" + "Hits\t" + "Misses\t" + "FP1\t" + "FP2" + System.getProperty("line.separator");
            StringBuilder body = new StringBuilder();
            List<PerformanceEvaluation> peData = getPerfEvalForGroup(group, softCriterion);
            String line = null;
            for (PerformanceEvaluation pe : peData) {
                line = pe.getPersonName() + "\t" + pe.getPart() + "\t" + pe.getEpoch() + "\t" + pe.getHits() + "\t" + pe.getMisses() + "\t" + pe.getFP1() + "\t" + pe.getFP2() + System.getProperty("line.separator");
                body.append(line);
            }
            FileUtils.writeStringToFile(new File(FilenameUtils.getFullPath(dirName) + filename), header + body);
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public void exportAggregatedPerfEvaluation(String dirName) {
        DAO<ThresholdGroup> thresholdDAO = new DAO<ThresholdGroup>(ThresholdGroup.class, String.class, "groupName");
        SortedSet<ThresholdGroup> groups = thresholdDAO.findAllSorted();
        BigDecimal spindleDurations[] = new BigDecimal[] { BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.4), BigDecimal.valueOf(0.5) };
        boolean softDone = false;
        for (BigDecimal duration : spindleDurations) {
            List<ThresholdGroup> result = new ArrayList<ThresholdGroup>();
            for (ThresholdGroup group : groups) {
                if (group.getScalarMinSpindleDuration().equals(duration)) {
                    result.add(group);
                }
            }
            Collections.sort(result, new Comparator<ThresholdGroup>() {

                public int compare(ThresholdGroup o1, ThresholdGroup o2) {
                    return o1.getVt().compareTo(o2.getVt());
                }
            });
            if (!softDone) {
                exportAggregatedPerfEvaluation(dirName, "aggr_perf_eval_" + duration + "_soft.xls", result, true);
                softDone = true;
            }
            exportAggregatedPerfEvaluation(dirName, "aggr_perf_eval_" + duration + ".xls", result, false);
        }
    }

    public void exportAggregatedPerfEvaluation(String dirName, String filename, List<ThresholdGroup> groups, boolean softCriterion) {
        try {
            String title = softCriterion ? "Soft Criterion" : "Hard Criterion - Minimum Spindle Duration: " + groups.get(0).getScalarMinSpindleDuration();
            String header = title + System.getProperty("line.separator") + "\t" + "1st Third" + "\t\t\t\t" + "2nd Third" + "\t\t\t\t" + "3rd Third" + "\t\t\t\t" + "Total" + System.getProperty("line.separator") + "Vt\t" + "% Hits\t" + "% totFP\t" + "% FP1\t" + "% FP2\t" + "% Hits\t" + "% totFP\t" + "% FP1\t" + "% FP2\t" + "% Hits\t" + "% totFP\t" + "% FP1\t" + "% FP2\t" + "% Hits\t" + "% totFP\t" + "% FP1\t" + "% FP2\t" + System.getProperty("line.separator");
            StringBuilder body = new StringBuilder();
            for (ThresholdGroup group : groups) {
                List<PerformanceEvaluation> peData = getPerfEvalForGroup(group, softCriterion);
                String line = null;
                line = group.getVt() + "\t";
                line += calcPercentages(peData, "1st", false);
                line += calcPercentages(peData, "2nd", false);
                line += calcPercentages(peData, "3rd", false);
                line += calcPercentages(peData, null, true);
                line += System.getProperty("line.separator");
                body.append(line);
            }
            FileUtils.writeStringToFile(new File(FilenameUtils.getFullPath(dirName) + filename), header + body);
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    private String calcPercentages(List<PerformanceEvaluation> peData, String part, boolean all) {
        PerformanceEvaluation aggrPE = new PerformanceEvaluation();
        String line = "";
        if (all) {
            for (PerformanceEvaluation pe : peData) {
                aggrPE.addFP1(pe.getFP1());
                aggrPE.addFP2(pe.getFP2());
                aggrPE.addHits(pe.getHits());
                aggrPE.addMisses(pe.getMisses());
            }
        } else {
            for (PerformanceEvaluation pe : peData) {
                if (pe.getPart().equals(part)) {
                    aggrPE.addFP1(pe.getFP1());
                    aggrPE.addFP2(pe.getFP2());
                    aggrPE.addHits(pe.getHits());
                    aggrPE.addMisses(pe.getMisses());
                }
            }
        }
        line += aggrPE.getHitsPercentage() + "\t" + aggrPE.getTotalFPPercentage() + "\t" + aggrPE.getFP1Percentage() + "\t" + aggrPE.getFP2Percentage() + "\t";
        return line;
    }

    public String epochSpindles(long epochID, boolean soft) {
        StringBuilder sb = new StringBuilder();
        Epoch e = epochDAO.get(epochID);
        int spindlesCount = e.getSpindleIndications(db.getDefaultThresholdGroup(), soft).size();
        sb.append(e.toTSV(db.getDefaultThresholdGroup(), soft));
        return getSpindlesReport(sb.toString(), 1, spindlesCount);
    }

    public String partSpindles(long partID, boolean soft) {
        StringBuilder sb = new StringBuilder();
        int epochsCount = 0;
        int spindlesCount = 0;
        SessionPart part = partDAO.get(partID);
        SortedSet<Epoch> epochs = epochDAO.findAllSorted(part.getId());
        for (Epoch epoch : epochs) {
            epochsCount++;
            spindlesCount += epoch.getSpindleIndications(db.getDefaultThresholdGroup(), soft).size();
            sb.append(epoch.toTSV(db.getDefaultThresholdGroup(), soft));
        }
        return getSpindlesReport(sb.toString(), epochsCount, spindlesCount);
    }

    public String sleepSpindles(long sleepID, boolean soft) {
        StringBuilder sb = new StringBuilder();
        int epochsCount = 0;
        int spindlesCount = 0;
        SleepSession sleep = sleepDAO.get(sleepID);
        SortedSet<SessionPart> parts = partDAO.findAllSorted(sleep.getId());
        for (SessionPart part : parts) {
            SortedSet<Epoch> epochs = epochDAO.findAllSorted(part.getId());
            for (Epoch epoch : epochs) {
                epochsCount++;
                spindlesCount += epoch.getSpindleIndications(db.getDefaultThresholdGroup(), soft).size();
                sb.append(epoch.toTSV(db.getDefaultThresholdGroup(), soft));
            }
        }
        return getSpindlesReport(sb.toString(), epochsCount, spindlesCount);
    }

    public String personSpindles(long personID, boolean soft) {
        StringBuilder sb = new StringBuilder();
        int epochsCount = 0;
        int spindlesCount = 0;
        Person p = personDAO.get(personID);
        SortedSet<SessionPart> parts = db.findAllSorted(p.getId());
        for (SessionPart part : parts) {
            SortedSet<Epoch> epochs = epochDAO.findAllSorted(part.getId());
            for (Epoch epoch : epochs) {
                epochsCount++;
                spindlesCount += epoch.getSpindleIndications(db.getDefaultThresholdGroup(), soft).size();
                sb.append(epoch.toTSV(db.getDefaultThresholdGroup(), soft));
            }
        }
        return getSpindlesReport(sb.toString(), epochsCount, spindlesCount);
    }

    public void exportSpindles(String dirName, Person p) throws UserException {
        try {
            String dir = FileUtil.writeToDir(FilenameUtils.getFullPath(dirName) + p.getLastName() + p.getId() + System.getProperty("file.separator"));
            FileUtils.writeStringToFile(new File(FilenameUtils.getFullPath(dir) + "soft_spindles.tsv"), personSpindles(p.getId(), true));
            FileUtils.writeStringToFile(new File(FilenameUtils.getFullPath(dir) + "hard_spindles.tsv"), personSpindles(p.getId(), false));
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    private String getPlotTitle(Date from, Date to) {
        String fromText = DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(from);
        String toText = DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(to);
        return "EEG interval: " + fromText + " - " + toText;
    }

    public WebFigure exportSpindlesDensityToWeb(Long partID, boolean softCriterion) {
        String reportTitle = getReportTitle(partID);
        SessionPart sp = partDAO.get(partID);
        String plotTitle = getPlotTitle(sp.getStart(), sp.getEnd());
        int duration = sp.duration().intValue();
        List<SpindleIndication> sis = db.getSpindleIndications(partID, softCriterion);
        double[] spindlesOccurrences = new double[sis.size()];
        for (int i = 0; i < sis.size(); i++) {
            Date spindleStart = sis.get(i).getStart();
            spindlesOccurrences[i] = Util.subtract(spindleStart, sp.getStart());
        }
        Settings settings = new SettingsService().getSettings();
        return MatlabGateway.plotSpindlesDensityWeb(duration, spindlesOccurrences, plotTitle, reportTitle, settings.getPlotWidth(), settings.getPlotHeight());
    }

    public WebFigure exportSpindlesDensityToWeb(Long partID, String fromTime, String toTime, boolean softCriterion) throws UserException {
        String reportTitle = getReportTitle(partID);
        SessionPart sp = partDAO.get(partID);
        Interval partInterval = new Interval(sp.getStart(), fromTime, toTime);
        String plotTitle = getPlotTitle(partInterval.getStart(), partInterval.getEnd());
        int duration = partInterval.duration().intValue();
        List<SpindleIndication> sis = db.getSpindleIndications(partID, partInterval, softCriterion);
        double[] spindlesOccurrences = new double[sis.size()];
        for (int i = 0; i < sis.size(); i++) {
            Date spindleStart = sis.get(i).getStart();
            spindlesOccurrences[i] = Util.subtract(spindleStart, partInterval.getStart());
        }
        Settings settings = new SettingsService().getSettings();
        return MatlabGateway.plotSpindlesDensityWeb(duration, spindlesOccurrences, plotTitle, reportTitle, settings.getPlotWidth(), settings.getPlotHeight());
    }

    public WebFigure exportToWeb(Long epochID, boolean softCriterion) throws UserException {
        return preparePlotExport(new WebFigureExport(softCriterion), epochDAO.get(epochID), false);
    }

    public WebFigure exportRawToWeb(Long epochID, boolean softCriterion) throws UserException {
        return preparePlotExport(new WebFigureExport(softCriterion), epochDAO.get(epochID), true);
    }

    public void exportAllToPNG(String dirName, Person p, boolean softCriterion) throws UserException {
        preparePlotExport(new ExportPNG(softCriterion), p, dirName);
    }

    public void exportAllToPs(String dirName, Person p, boolean softCriterion) throws UserException {
        preparePlotExport(new ExportPostScript(softCriterion), p, dirName);
    }

    public void exportFiltered(String dirName, Person p) throws UserException {
        doDataExport(dirName, ".txt", p, new FilteredDataExport());
    }

    public void exportASCII(String dirName, Person p) throws UserException {
        doDataExport(dirName, ".txt", p, new ASCIIDataExport());
    }

    public void exportASCII(String filename, Epoch e, Date start, Date end) {
        DataExport exp = new ASCIIDataExport();
        double result[] = e.getEEGSamples(start, end);
        log.info("Exporting {} spindle raw data: epochID={}, from={}, to={}", new Object[] { result.length, e.getId(), start, end });
        exp.writeData(result, new File(filename));
    }

    public void exportMat(String dirName, Person p) throws UserException {
        doDataExport(dirName, "", p, new MatDataExport());
    }

    public class ASCIIDataExport extends DataExport {

        @Override
        public double[] getData(Epoch e) throws UserException {
            return e.getEEGSamples();
        }
    }

    public class FilteredDataExport extends DataExport {

        @Override
        public double[] getData(Epoch e) throws UserException {
            SamplingRate sr = SamplingRate.toEnum(db.getSamplingRate(e.getSessionPartID()));
            return e.getFilteredEEGSamples(CutoffFrequency.getCutoffFreq(sr));
        }
    }

    public class MatDataExport extends ASCIIDataExport {

        @Override
        public void writeData(double[] data, File file) {
            MatlabGateway.writeToMAT(data, file.getPath());
        }
    }

    public class ExportPNG extends PlotExport {

        public ExportPNG(boolean softCriterion) {
            super(softCriterion);
        }

        public int export(int npart, int nepoch) throws UserException {
            return doPlotExport(this, FilenameUtils.getFullPath(path) + npart + "-" + nepoch + ".png", false, nepoch);
        }

        @Override
        Format getFormat() {
            return Format.png;
        }
    }

    public class WebFigureExport extends PlotExport {

        public WebFigureExport(boolean softCriterion) {
            super(softCriterion);
        }

        public int export(int npart, int nepoch) throws UserException {
            throw new UnsupportedOperationException();
        }

        public WebFigure export() throws UserException {
            return doWebExport(this);
        }

        @Override
        Format getFormat() {
            throw new UnsupportedOperationException("Format is not applicable for Web Figures.");
        }
    }

    public class ExportPostScript extends PlotExport {

        private boolean append = true;

        public ExportPostScript(boolean softCriterion) {
            super(softCriterion);
        }

        public int export(int npart, int nepoch) throws UserException {
            return doPlotExport(this, FilenameUtils.getFullPath(path) + npart + ".ps", append, nepoch);
        }

        public void init(String path, Person person) throws UserException {
            String dirName = FilenameUtils.getFullPath(FilenameUtils.getFullPath(path) + person.getLastName() + person.getId() + System.getProperty("file.separator"));
            if (Util.isEmpty(dirName)) {
                throw new UserException(ErrorMessages.FILENAME_INVALID);
            }
            File dir = new File(dirName);
            if (!dir.exists()) {
                dir.mkdirs();
            } else {
                Iterator it = FileUtils.iterateFiles(dir, new String[] { "ps" }, false);
                while (it.hasNext()) {
                    ((File) it.next()).delete();
                }
            }
            this.path = dirName;
        }

        @Override
        Format getFormat() {
            return Format.ps;
        }
    }

    protected WebFigure preparePlotExport(WebFigureExport exp, Epoch e, boolean raw) throws UserException {
        if (raw) {
            exp.addEpoch(e, freq.get(1));
        } else {
            exp.addEpoch(e, freq.get(3));
            exp.addEpoch(e, freq.get(2));
            exp.addEpoch(e, freq.get(1));
        }
        WebFigure result = exp.export();
        exp.clear();
        return result;
    }

    protected void preparePlotExport(PlotExport exp, Person person, String path) throws UserException {
        exp.init(path, person);
        SortedSet<SessionPart> parts = db.findAllSorted(person.getId());
        int nparts = 0;
        for (SessionPart part : parts) {
            nparts++;
            SamplingRate sr = SamplingRate.toEnum(db.getSamplingRate(part.getId()));
            Iterator<Epoch> it = exp.getData(part.getId());
            int index = 0;
            if (singleEpochPerPage) {
                while (it.hasNext()) {
                    Epoch e = it.next();
                    index++;
                    exp.addEpoch(e, CutoffFrequency.getCutoffFreq(freq.get(3), sr));
                    exp.addEpoch(e, CutoffFrequency.getCutoffFreq(freq.get(2), sr));
                    exp.addEpoch(e, CutoffFrequency.getCutoffFreq(freq.get(1), sr));
                    exp.export(nparts, index);
                    exp.clear();
                }
            } else {
                while (it.hasNext()) {
                    index++;
                    for (int i = 3; i > 0; i--) {
                        if (it.hasNext()) {
                            exp.addEpoch(it.next(), CutoffFrequency.getCutoffFreq(freq.get(i), sr));
                        }
                    }
                    index = exp.export(nparts, index);
                    exp.clear();
                }
            }
        }
    }

    private String getPlotTitle(int count, String label, Epoch e) {
        StringBuilder sb = new StringBuilder();
        sb.append(count + ". ");
        sb.append(label);
        sb.append(e.toPrettyString());
        return sb.toString();
    }

    private String getReportTitle(long sessionPartID) {
        String spindles;
        if (spindleType == Spindle.SPINDLE_INDICATION) {
            spindles = "Spindle Indications";
        } else {
            spindles = "Visually Detected Spindles";
        }
        return db.getName(sessionPartID) + ", " + "Sampling Rate: " + db.getSamplingRate(sessionPartID) + ", " + "Channel: " + db.getChannel(sessionPartID) + "  -  " + db.getSleepDate(sessionPartID) + ", " + spindles;
    }
}
