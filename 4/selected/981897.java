package uk.ac.city.soi.everest.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import uk.ac.city.soi.everest.er.LogEvent;
import uk.ac.city.soi.everest.monitor.simplex.Simplex;

/**
 * This class serves as in memory storage for past events.
 * 
 * @author Khaled Mahbub
 * 
 */
public class PastEventStorer implements Serializable {

    private static Logger logger = Logger.getLogger(PastEventStorer.class);

    protected ArrayList<EventTriplet> eventLog = new ArrayList<EventTriplet>();

    private ConstraintMatrixStorer cms;

    /**
     * Constructor for a given ConstraintMatrixStorer.
     * 
     * @param cms
     */
    public PastEventStorer(ConstraintMatrixStorer cms) {
        this.cms = cms;
    }

    /**
     * Adds an event to the storer.
     * 
     * @param event
     */
    public void addEvent(LogEvent event, Template t, Predicate pred) {
        ConstraintMatrix cm = cms.getConstraintMatrix(t.getFormulaId());
        if (cm != null) {
            long A[][] = cm.getA();
            long B[] = cm.getB();
            int colIndex = cm.getColumnIndex(pred.getTimeVarName());
            for (int i = 0; i < cm.getRowCount(); i++) {
                if (A[i][colIndex] != 0) {
                    B[i] = B[i] - (A[i][colIndex] * event.getTimestamp());
                    A[i][colIndex] = 0;
                }
            }
            String chN = "";
            double maxE = 0;
            boolean found = false;
            for (int i = 0; i < t.totalPredicates(); i++) {
                Predicate p = t.getPredicate(i);
                if (!p.getTimeVarName().equals(pred.getTimeVarName())) {
                    Simplex LP = new Simplex(cm.getColumnCount(), cm.getRowCount());
                    float coefficients[] = new float[cm.getColumnCount()];
                    float rhs;
                    if (!p.getTimeVarName().equals(pred.getTimeVarName())) {
                        int col = cm.getColumnIndex(p.getTimeVarName());
                        for (int c = 0; c < cm.getColumnCount(); c++) if (c == col) coefficients[c] = 1; else coefficients[c] = 0;
                        LP.specifyObjective(coefficients, false);
                        for (int r = 0; r < cm.getRowCount(); r++) {
                            int sign = 1;
                            if (B[r] < 0) sign = -1;
                            for (int c = 0; c < cm.getColumnCount(); c++) coefficients[c] = A[r][c] * sign;
                            rhs = B[r] * sign;
                            LP.addConstraint(coefficients, rhs, 0);
                        }
                        LP.preprocess(cm.getColumnCount(), cm.getRowCount());
                        int SolveStatus;
                        boolean done = false;
                        double maxT = 0;
                        while (!done) {
                            SolveStatus = LP.iterate();
                            if (SolveStatus == LP.Unbounded) {
                                done = true;
                            } else if (SolveStatus == LP.Optimal) {
                                if (LP.artificialPresent == false) {
                                    done = true;
                                    found = true;
                                    maxT = LP.objectiveValue;
                                } else {
                                    if (LP.calculateObjective() == 0) {
                                        LP.getRidOfArtificials();
                                    } else {
                                        done = true;
                                    }
                                }
                            }
                        }
                        if (found) {
                            if (maxT > maxE) {
                                maxE = maxT;
                                chN = cm.getChannelName(p.getTimeVarName());
                            }
                        }
                    }
                }
            }
            if (!chN.equals("")) {
                eventLog.add(new EventTriplet(event, maxE, chN));
            }
        }
    }

    /**
     * Searches for an event in the storer that can be matched with a given predicate and returns the matched event.
     * 
     * @param pred
     * @return
     */
    public LogEvent getMtachedEvent(Predicate pred) {
        LogEvent event = null;
        for (int i = 0; i < eventLog.size(); i++) {
            LogEvent e = ((EventTriplet) eventLog.get(i)).getEvent();
            if (e.getHashCode().equals(pred.getHashCode()) && (pred.getRangeLB() <= e.getTimestamp() && e.getTimestamp() <= pred.getRangeUB())) {
                event = e;
                break;
            }
        }
        return event;
    }

    /**
     * Deletes an event from the storer that has a given hash uk.ac.city.soi.everest.
     * 
     * @param hashCode
     */
    public void deleteEvents(ClockMap clockMap) {
        ArrayList<EventTriplet> toDelete = new ArrayList<EventTriplet>();
        for (int i = 0; i < eventLog.size(); i++) {
            EventTriplet et = eventLog.get(i);
            if (clockMap.containsSource(et.getChName())) {
                long lt = clockMap.getLatestTime(et.getChName());
                if (lt > et.getLifeTime()) toDelete.add(et);
            }
        }
        eventLog.removeAll(toDelete);
    }
}

/**
 * Inner class for modelling event info necessary to the past event storer.
 * 
 */
class EventTriplet {

    private LogEvent event;

    private double lifeTime;

    private String chName;

    /**
     * Constructor for given event, life time and channel name.
     * 
     * @param event
     * @param lifeTime
     * @param chName
     */
    public EventTriplet(LogEvent event, double lifeTime, String chName) {
        this.event = event;
        this.lifeTime = lifeTime;
        this.chName = chName;
    }

    /**
     * Returns the channel name.
     * 
     * @return
     */
    public String getChName() {
        return chName;
    }

    /**
     * Returns the event.
     * 
     * @return
     */
    public LogEvent getEvent() {
        return event;
    }

    /**
     * Returns the life time.
     * 
     * @return
     */
    public double getLifeTime() {
        return lifeTime;
    }
}
