package fr.ecp.lgi.shared_profiles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections15.set.ListOrderedSet;
import org.decisiondeck.jlp.problem.LpDimension;
import org.decisiondeck.jlp.solution.LpSolverDuration;
import org.decisiondeck.xmcda_oo.services.sorting.ProfilesDistanceResult;
import org.decisiondeck.xmcda_oo.structure.AlternativeEvaluations;
import org.decisiondeck.xmcda_oo.structure.Criterion;
import org.decisiondeck.xmcda_oo.structure.DecisionMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsWriter {

    private static final Logger s_logger = LoggerFactory.getLogger(ResultsWriter.class);

    private final Set<Double> m_approxes = new ListOrderedSet<Double>();

    private final OutputStream m_out;

    private final BufferedWriter m_writer;

    private final NumberFormat m_intFormatter = NumberFormat.getIntegerInstance(Locale.ENGLISH);

    private final Set<Criterion> m_critsOrder = new ListOrderedSet<Criterion>();

    private AlternativeEvaluations m_currentSize;

    private LpDimension m_currentDimension;

    private LpSolverDuration m_mainDuration;

    private ProfilesDistanceResult m_currentDistances;

    private Map<DecisionMaker, Integer> m_necessaryComparisons;

    private Long m_findWeightsDurationSystem_ms;

    private String m_comments;

    private Integer m_currentSolveId;

    private Integer m_currentProblemId;

    private Boolean m_error;

    private Boolean m_timeout;

    private final int m_nbMaxDms;

    private Boolean m_outOfMemory;

    private static final String FIELD_SEPARATOR = ",";

    private static final String REPLACEMENT = " ";

    private Double m_optimalValue;

    public ResultsWriter(Set<Double> approxes, Set<Criterion> sizeCriteriaOrder, OutputStream out, int nbMaxDms) {
        if (approxes == null || sizeCriteriaOrder == null || out == null) {
            throw new NullPointerException("" + approxes + sizeCriteriaOrder + out);
        }
        m_approxes.addAll(approxes);
        m_out = out;
        m_writer = new BufferedWriter(new OutputStreamWriter(m_out));
        m_critsOrder.addAll(sizeCriteriaOrder);
        m_intFormatter.setGroupingUsed(false);
        m_nbMaxDms = nbMaxDms;
        reset();
    }

    public void close() throws IOException {
        if (m_currentProblemId != null) {
            s_logger.warn("Problem id " + m_currentProblemId + " has not been written before closing.");
        }
        m_writer.close();
    }

    public void writeHeaders() throws IOException {
        m_writer.write("Solve id" + FIELD_SEPARATOR);
        m_writer.write("Pbl id" + FIELD_SEPARATOR);
        m_writer.write("Criteria" + FIELD_SEPARATOR);
        m_writer.write("Dms" + FIELD_SEPARATOR);
        m_writer.write("Categories" + FIELD_SEPARATOR);
        m_writer.write("Examples each" + FIELD_SEPARATOR);
        m_writer.write("Examples tot" + FIELD_SEPARATOR);
        m_writer.write("Binaries" + FIELD_SEPARATOR);
        m_writer.write("Continuous" + FIELD_SEPARATOR);
        m_writer.write("Constraints" + FIELD_SEPARATOR);
        m_writer.write("System ms" + FIELD_SEPARATOR);
        m_writer.write("CPU 1 ms" + FIELD_SEPARATOR);
        m_writer.write("cplex System ms" + FIELD_SEPARATOR);
        m_writer.write("cplex CPU ms" + FIELD_SEPARATOR);
        m_writer.write("Prof dist SUM" + FIELD_SEPARATOR);
        m_writer.write("Prof dist MAX");
        for (Double approx : m_approxes) {
            m_writer.write(FIELD_SEPARATOR);
            m_writer.write("Profs " + m_intFormatter.format(approx));
        }
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Distance in alternatives");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Comments");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("System ms find weights");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("DM1");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("DM2");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("DM3");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("DM4");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Error");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Timeout");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Memory");
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write("Optimal");
        m_writer.newLine();
    }

    public void writeResults() throws IOException {
        s_logger.debug("Printing record.");
        m_writer.write(m_intFormatter.format(m_currentSolveId.intValue()));
        m_writer.write(FIELD_SEPARATOR);
        m_writer.write(m_intFormatter.format(m_currentProblemId.intValue()));
        m_writer.write(FIELD_SEPARATOR);
        for (Criterion criterion : m_critsOrder) {
            if (m_currentSize != null) {
                final Double eval = m_currentSize.getEvaluations().get(criterion);
                m_writer.write(m_intFormatter.format(eval));
            }
            m_writer.write(FIELD_SEPARATOR);
        }
        if (m_currentDimension != null) {
            m_writer.write(m_intFormatter.format(m_currentDimension.getBinaries()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_currentDimension != null) {
            m_writer.write(m_intFormatter.format(m_currentDimension.getContinuous()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_currentDimension != null) {
            m_writer.write(m_intFormatter.format(m_currentDimension.getConstraints()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_mainDuration != null && m_mainDuration.getWallDuration_ms() != null) {
            m_writer.write(m_intFormatter.format(m_mainDuration.getWallDuration_ms()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_mainDuration != null && m_mainDuration.getThreadDuration_ms() != null) {
            m_writer.write(m_intFormatter.format(m_mainDuration.getThreadDuration_ms()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_mainDuration != null && m_mainDuration.getSolverWallDuration_ms() != null) {
            m_writer.write(m_intFormatter.format(m_mainDuration.getSolverWallDuration_ms()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_mainDuration != null && m_mainDuration.getSolverCpuDuration_ms() != null) {
            m_writer.write(m_intFormatter.format(m_mainDuration.getSolverCpuDuration_ms()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_currentDistances != null) {
            m_writer.write(m_intFormatter.format(m_currentDistances.getLastSumDist()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_currentDistances != null) {
            m_writer.write(m_intFormatter.format(m_currentDistances.getLastMaxDist()));
        }
        m_writer.write(FIELD_SEPARATOR);
        for (Double approx : m_approxes) {
            final double approxValue = approx.doubleValue();
            if (m_currentDistances != null) {
                m_writer.write(m_intFormatter.format(m_currentDistances.getNbEquals(approxValue)));
            }
            m_writer.write(FIELD_SEPARATOR);
        }
        if (m_currentDistances != null) {
            m_writer.write(m_intFormatter.format(m_currentDistances.getLastDistanceInAlternatives()));
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_comments != null) {
            protectAndWrite(m_comments);
        }
        m_writer.write(FIELD_SEPARATOR);
        if (m_findWeightsDurationSystem_ms != null) {
            m_writer.write(m_intFormatter.format(m_findWeightsDurationSystem_ms));
        }
        int nbDmsExported = 0;
        if (m_necessaryComparisons != null) {
            for (DecisionMaker dm : m_necessaryComparisons.keySet()) {
                m_writer.write(FIELD_SEPARATOR);
                m_writer.write(m_intFormatter.format(m_necessaryComparisons.get(dm)));
                ++nbDmsExported;
            }
        }
        for (int i = nbDmsExported; i < m_nbMaxDms; ++i) {
            m_writer.write(FIELD_SEPARATOR);
        }
        m_writer.write(FIELD_SEPARATOR);
        writeBool(m_error);
        m_writer.write(FIELD_SEPARATOR);
        writeBool(m_timeout);
        m_writer.write(FIELD_SEPARATOR);
        writeBool(m_outOfMemory);
        m_writer.write(FIELD_SEPARATOR);
        if (m_optimalValue != null) {
            m_writer.write(m_optimalValue.toString());
        }
        m_writer.newLine();
        m_writer.flush();
        reset();
        s_logger.info("Ended printing results.");
    }

    private void writeBool(final Boolean boolObj) throws IOException {
        if (boolObj != null) {
            writeBool(boolObj.booleanValue());
        }
    }

    private void writeBool(boolean booleanValue) throws IOException {
        m_writer.write(m_intFormatter.format(booleanValue ? 1 : 0));
    }

    /**
     * Replaces some characters that might be harmful to the CSV export.
     * 
     * @param toWrite
     *            not <code>null</code>.
     * @throws IOException
     *             while writing.
     */
    private void protectAndWrite(final String toWrite) throws IOException {
        final String toWrite2 = toWrite.replace(FIELD_SEPARATOR, REPLACEMENT);
        final String toWrite3 = toWrite2.replace("\n", "\\n");
        final String toWriteFinal = toWrite3.replace("\r", "\\r");
        m_writer.write(toWriteFinal);
    }

    public void reset() {
        m_currentSize = null;
        m_mainDuration = null;
        m_comments = null;
        m_currentProblemId = null;
        m_currentSolveId = null;
        m_currentDimension = null;
        m_currentDistances = null;
        m_findWeightsDurationSystem_ms = null;
        m_necessaryComparisons = null;
        m_error = null;
        m_timeout = null;
        m_outOfMemory = null;
        m_optimalValue = null;
        s_logger.debug("Current record have been reset.");
    }

    public void setCurrentSize(AlternativeEvaluations currentSize) {
        m_currentSize = currentSize;
        s_logger.debug("Size set to {}.", currentSize);
    }

    public void setCurrentDimension(LpDimension currentDimension) {
        m_currentDimension = currentDimension;
        s_logger.debug("Dimension set to {}.", currentDimension);
    }

    public void setMainDuration(LpSolverDuration mainDuration) {
        m_mainDuration = mainDuration;
        s_logger.debug("Main duration set to {}.", mainDuration);
    }

    public void setCurrentDistances(ProfilesDistanceResult currentDistances) {
        m_currentDistances = currentDistances;
        s_logger.debug("Distances set to {}.", currentDistances);
    }

    public void setFindWeightsDuration(Long findWeightsDurationSystem_ms) {
        m_findWeightsDurationSystem_ms = findWeightsDurationSystem_ms;
        s_logger.debug("Find weights duration set to {} ms.", findWeightsDurationSystem_ms);
    }

    public void setComments(String comments) {
        m_comments = comments;
        s_logger.debug("Comments set to {}.", comments);
    }

    public void setCurrentSolveId(Integer currentSolveId) {
        m_currentSolveId = currentSolveId;
        s_logger.debug("Solver id set to {}.", currentSolveId);
    }

    public AlternativeEvaluations getCurrentSize() {
        return m_currentSize;
    }

    public LpDimension getCurrentDimension() {
        return m_currentDimension;
    }

    public LpSolverDuration getMainDuration() {
        return m_mainDuration;
    }

    public ProfilesDistanceResult getCurrentDistances() {
        return m_currentDistances;
    }

    public Long getFindWeightsDuration() {
        return m_findWeightsDurationSystem_ms;
    }

    public String getComments() {
        return m_comments;
    }

    public Integer getCurrentProblemId() {
        return m_currentProblemId;
    }

    public void setNecessaryComparisons(Map<DecisionMaker, Integer> necessaryComparisons) {
        m_necessaryComparisons = new HashMap<DecisionMaker, Integer>(necessaryComparisons);
        s_logger.debug("Necessary comparisons set ( " + necessaryComparisons.size() + " entries).");
    }

    public void setCurrentProblemId(Integer currentProblemId) {
        m_currentProblemId = currentProblemId;
        s_logger.debug("Problem id set to {}.", currentProblemId);
    }

    public Boolean getError() {
        return m_error;
    }

    public void setError(Boolean error) {
        m_error = error;
    }

    public Boolean getOutOfMemory() {
        return m_outOfMemory;
    }

    public void setOutOfMemory(Boolean outOfMemory) {
        m_outOfMemory = outOfMemory;
    }

    public void setTimeout(Boolean timeout) {
        m_timeout = timeout;
    }

    public Boolean getTimeout() {
        return m_timeout;
    }

    public void setOptimalValue(Double optimalValue) {
        m_optimalValue = optimalValue;
    }
}
