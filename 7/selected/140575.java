package org.processmining.mining.patternsmining;

import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.processmining.framework.log.AuditTrailEntries;
import org.processmining.framework.log.AuditTrailEntry;
import org.processmining.framework.log.LogEvent;
import org.processmining.framework.log.LogEvents;
import org.processmining.framework.log.LogFilter;
import org.processmining.framework.log.LogReader;
import org.processmining.framework.log.LogSummary;
import org.processmining.framework.log.ProcessInstance;
import org.processmining.framework.models.ModelGraph;
import org.processmining.framework.ui.MainUI;
import org.processmining.framework.ui.Message;
import org.processmining.framework.ui.Progress;
import org.processmining.framework.util.MethodsForWorkflowLogDataStructures;
import org.processmining.mining.MiningPlugin;
import org.processmining.mining.MiningResult;
import org.processmining.mining.logabstraction.LogAbstraction;
import org.processmining.mining.logabstraction.LogAbstractionImpl;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * @author WALID
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class WorkflowPatternsMiner implements MiningPlugin {

    private LogEvents events;

    private DependenciesTables Deptab;

    private DoubleMatrix2D directPrecedents;

    private DoubleMatrix2D directfollowers;

    private DoubleMatrix2D causalPrecedents;

    private DoubleMatrix2D causalSuccession;

    private DoubleMatrix1D ACWWidth;

    private LogEvents modelElements;

    private LogReader log;

    private LogFilter filter;

    private LogRelationEditor relationsEditor;

    private JFrame frametest;

    public WorkflowPatternsMiner() {
    }

    public String getName() {
        return ("Workflow patterns miner");
    }

    public JPanel getOptionsPanel(LogSummary arg0) {
        return null;
    }

    private void message(String msg, int stage, Progress progress) {
        Message.add(msg, Message.DEBUG);
        if (progress != null) {
            progress.setNote(msg);
            progress.setProgress(stage);
        }
    }

    public MiningResult mine(LogReader log) {
        LogAbstraction logAbstraction;
        logAbstraction = new LogAbstractionImpl(log);
        events = log.getLogSummary().getLogEvents();
        Deptab = new DependenciesTables(log);
        PatternsResult resultdebug = new PatternsResult(this, log, Deptab);
        ModelGraph epc = resultdebug.build();
        relationsEditor = new LogRelationEditor(MainUI.getInstance(), Deptab);
        return resultdebug;
    }

    public void editRelations(LogEvent event) {
        relationsEditor.edit(frametest, Deptab, event);
    }

    /**
	 * @param log
	 * @return first matrix to do: factorize logeevents param modelElements to
	 *         do: division by act freq may be done in the last not here
	 */
    private DoubleMatrix2D getDirectPreviousInfo(LogReader log) {
        int numSimilarPIs = 0;
        log.reset();
        modelElements = log.getLogSummary().getLogEvents();
        System.out.println("walid fonc1");
        int s = modelElements.size();
        DoubleMatrix2D D = DoubleFactory2D.sparse.make(s, s, 0);
        while (log.hasNext()) {
            ProcessInstance pi = log.next();
            numSimilarPIs = MethodsForWorkflowLogDataStructures.getNumberSimilarProcessInstances(pi);
            AuditTrailEntries ates = pi.getAuditTrailEntries();
            AuditTrailEntry ate = ates.next();
            int memory = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
            while (ates.hasNext()) {
                ate = ates.next();
                int index = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
                D.set(index, memory, D.get(index, memory) + numSimilarPIs);
                memory = index;
            }
        }
        for (int j = 0; j < s; j++) {
            for (int k = 0; k < j; k++) {
                if (D.get(j, k) > 0 && D.get(k, j) > 0) {
                    D.set(j, k, -D.get(j, k));
                    D.set(k, j, -D.get(k, j));
                }
            }
        }
        return D;
    }

    /**
	 * @param log
	 * @return first matrix to do: factorize logeevents param modelElements to
	 *         do: division by act freq may be done in the last not here
	 */
    private DoubleMatrix2D getDirectFollowerInfo(LogReader log) {
        int numSimilarPIs = 0;
        log.reset();
        modelElements = log.getLogSummary().getLogEvents();
        int s = modelElements.size();
        DoubleMatrix2D D = DoubleFactory2D.sparse.make(s, s, 0);
        while (log.hasNext()) {
            ProcessInstance pi = log.next();
            numSimilarPIs = MethodsForWorkflowLogDataStructures.getNumberSimilarProcessInstances(pi);
            AuditTrailEntries ates = pi.getAuditTrailEntries();
            AuditTrailEntry ate = ates.next();
            int memory = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
            while (ates.hasNext()) {
                ate = ates.next();
                int index = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
                D.set(memory, index, D.get(memory, index) + numSimilarPIs);
                memory = index;
            }
        }
        for (int j = 0; j < s; j++) {
            for (int k = 0; k < j; k++) {
                if (D.get(j, k) > 0 && D.get(k, j) > 0) {
                    D.set(j, k, -D.get(j, k));
                    D.set(k, j, -D.get(k, j));
                }
            }
        }
        return D;
    }

    private DoubleMatrix2D getCausalPrecedents(DoubleMatrix2D directPrecedents) {
        DoubleMatrix2D D = directPrecedents.copy();
        int s = modelElements.size();
        for (int j = 0; j < s; j++) {
            int freq = modelElements.getEvent(j).getOccurrenceCount();
            for (int k = 0; k < s; k++) {
                D.set(j, k, D.get(j, k) / freq);
            }
        }
        return D;
    }

    private DoubleMatrix2D getCausalSuccession(DoubleMatrix2D directPrecedents) {
        DoubleMatrix2D D = directPrecedents.copy();
        int s = modelElements.size();
        for (int j = 0; j < s; j++) {
            int freq = modelElements.getEvent(j).getOccurrenceCount();
            for (int k = 0; k < s; k++) {
                D.set(k, j, D.get(k, j) / freq);
            }
        }
        return D;
    }

    private DoubleMatrix1D getBackwardACWWidth(DoubleMatrix2D directPrecedents) {
        int s = modelElements.size();
        DoubleMatrix1D D = DoubleFactory1D.sparse.make(s, 1);
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < i; j++) {
                if (directPrecedents.get(i, j) < 0) {
                    D.set(i, D.get(i) + 1);
                    D.set(j, D.get(j) + 1);
                }
            }
        }
        DoubleMatrix1D D1 = D.copy();
        for (int k = 0; k < s; k++) {
            if (D.get(k) == 1) {
                for (int l = 0; l < s; l++) {
                    if (directPrecedents.get(k, l) > 0 && D1.get(l) > D1.get(k)) {
                        D.set(k, D.get(l));
                    }
                }
            }
        }
        return D;
    }

    private DoubleMatrix1D getForwardACWWidth(DoubleMatrix2D directPrecedents) {
        int s = modelElements.size();
        DoubleMatrix1D D = DoubleFactory1D.sparse.make(s, 1);
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < i; j++) {
                if (directPrecedents.get(i, j) < 0) {
                    D.set(i, D.get(i) + 1);
                    D.set(j, D.get(j) + 1);
                }
            }
        }
        DoubleMatrix1D D1 = D.copy();
        for (int k = 0; k < s; k++) {
            if (D.get(k) == 1) {
                for (int l = 0; l < s; l++) {
                    if (directPrecedents.get(k, l) > 0 && D1.get(l) > D1.get(k)) {
                        D.set(k, D.get(l));
                    }
                }
            }
        }
        return D;
    }

    private DoubleMatrix2D getfinalSuccession(LogReader log, DoubleMatrix1D ACW) {
        int numSimilarPIs = 0;
        log.reset();
        int s = modelElements.size();
        DoubleMatrix2D D = DoubleFactory2D.sparse.make(s, s, 0);
        while (log.hasNext()) {
            ProcessInstance pi = log.next();
            numSimilarPIs = MethodsForWorkflowLogDataStructures.getNumberSimilarProcessInstances(pi);
            AuditTrailEntries ates = pi.getAuditTrailEntries();
            ArrayList instanceList = ates.toArrayList();
            int i = 0;
            while (i < instanceList.size() - 1) {
                AuditTrailEntry ate = (AuditTrailEntry) instanceList.get(i);
                int courantActivity = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
                int ACWwidth = (int) ACW.get(courantActivity);
                for (int k = 1; k < ACWwidth + 1; k++) {
                    if (i + k < instanceList.size()) {
                        AuditTrailEntry atefollow = (AuditTrailEntry) instanceList.get(i + k);
                        int followerActivity = modelElements.findLogEventNumber(atefollow.getElement(), atefollow.getType());
                        D.set(courantActivity, followerActivity, D.get(courantActivity, followerActivity) + numSimilarPIs);
                    }
                }
                i++;
            }
        }
        for (int j = 0; j < s; j++) {
            for (int k = 0; k < j; k++) {
                if (D.get(j, k) > 0 && D.get(k, j) > 0) {
                    D.set(j, k, -D.get(j, k));
                    D.set(k, j, -D.get(k, j));
                }
            }
        }
        return D;
    }

    public DoubleMatrix2D getPrecedentInfo(int distance, LogReader log) {
        int numSimilarPIs = 0;
        log.reset();
        modelElements = log.getLogSummary().getLogEvents();
        System.out.println("walid");
        boolean update = false;
        int s = modelElements.size();
        DoubleMatrix2D D = DoubleFactory2D.sparse.make(s, s, 0);
        while (log.hasNext()) {
            ProcessInstance pi = log.next();
            numSimilarPIs = MethodsForWorkflowLogDataStructures.getNumberSimilarProcessInstances(pi);
            int[] memory = new int[distance];
            int i = 0;
            AuditTrailEntries ates = pi.getAuditTrailEntries();
            while (ates.hasNext()) {
                AuditTrailEntry ate = ates.next();
                if (i < distance) {
                    memory[i] = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
                    i++;
                    continue;
                }
                int index = modelElements.findLogEventNumber(ate.getElement(), ate.getType());
                if (distance == 0) {
                    D.set(index, index, D.get(index, index) + numSimilarPIs);
                } else {
                    D.set(index, memory[0], D.get(index, memory[0]) + numSimilarPIs);
                    for (int j = 0; j < distance - 1; j++) {
                        memory[j] = memory[j + 1];
                    }
                    memory[distance - 1] = index;
                }
            }
        }
        return D;
    }

    public String getHtmlDescription() {
        return "<h1>" + getName() + "</h1>" + "<p>" + "For more information, please refer to publication:<br>" + "&nbsp;&nbsp;&nbsp;Walid Gaaloul et al.<br>" + "&nbsp;&nbsp;&nbsp;<i>Towards Mining Structural Workflow Patterns</i><br>" + "&nbsp;&nbsp;&nbsp;in 16th International Conference on Database and Expert Systems Applications DEXA'05 August 22-26, 2005, Copenhagen Danemark.<br>" + "at http://www.loria.fr/~gaaloul" + "</p>";
    }
}
