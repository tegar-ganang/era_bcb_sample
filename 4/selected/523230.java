package de.ibis.permoto.solver.as.tech;

import java.sql.Timestamp;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.ibis.permoto.gui.solver.panels.AnalyticSolverPanel;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.model.basic.scenario.ClassPart;
import de.ibis.permoto.model.basic.scenario.OutgoingConnections;
import de.ibis.permoto.model.definitions.impl.PerMoToBusinessCase;
import de.ibis.permoto.solver.as.util.AlgorithmInput;
import de.ibis.permoto.solver.as.util.AnalyticQNStation;
import de.ibis.permoto.solver.as.util.HashTable;
import de.ibis.permoto.util.db.ClassResultBean;
import de.ibis.permoto.util.db.ClassStationResultBean;
import de.ibis.permoto.util.db.DBManager;

/**
 * Algorithm for the solution of an open non-product-form queueing network. The
 * algorithm works with load-dependent as well as non-load-dependent stations.It
 * is analogous to Bolch (Queueing Networks and Markov Chains, Modeling and
 * Performance Evaluation with Computer Science Applications) page 479-483.
 * @author Gefei Fu
 * @author Christian Markl
 */
public class DecompositionPujolle implements QNAlgorithm {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(DecompositionPujolle.class);

    /** The manager for the interaction with the db. */
    private DBManager dbManager = DBManager.getInstance();

    /** The input parameters for the MVA. */
    private AlgorithmInput inputParameter;

    /** Unique id of the solution which is computed in the db. */
    private int solutionID;

    /** Unique id of the inserted scenario. */
    private int scenarioID;

    /** Start time of the execution. */
    private Timestamp startTime;

    /** Hash table for the queue length of a station over all classes. */
    private HashTable tableQueuelengthPerStation;

    /** Hash table for the queue length of a station per class. */
    private HashTable tableQueuelengthPerStationAndClass;

    /** Hash table for the response time of a class over all station. */
    private HashTable tableResponsetimePerClass;

    /** Hash table for the response time of a class per station. */
    private HashTable tableResponsetimePerClassAndStation;

    /** Hash table for the utilization of a station over all classes. */
    private HashTable tableUtilizationPerStation;

    /** Hash table for the utilization of a station per class. */
    private HashTable tableUtilizationPerStationAndClass;

    /** Hash table for the arrival Rate of each station. */
    private HashTable tableArrRate_i;

    /** Hash table for the arrival Rate of each class at each station. */
    private HashTable tableArrRate_ir;

    /**
	 * Hash table for the coefficient of variation of the service time at each
	 * station.
	 */
    private HashTable tableSquCVofServiceTime_i;

    /**
	 * Hash table for the coefficient of variation of the service time for each
	 * station per class.
	 */
    private HashTable tableSquCVofServiceTime_ir;

    /**
	 * Hash table for the coefficient of variation of the incoming from station
	 * i to j per class.
	 */
    private HashTable tableSquCVofIncoming_ijr;

    /**
	 * Hash table for the coefficient of variation of the inter-arrival times at
	 * each station.
	 */
    private HashTable tableSquCVOfInterArrivalTimes_i;

    /**
	 * Hash table for the coefficient of variation of the inter-arrival times
	 * per class at each station.
	 */
    private HashTable tableSquCVOfInterArrivalTimes_ir;

    /**
	 * The constructor of the DecompositionPujolle algorithm class.
	 * @param input AlgorithmInput the input parameters for the algorithm
	 */
    public DecompositionPujolle(final AlgorithmInput input) {
        startTime = new Timestamp(System.currentTimeMillis());
        this.inputParameter = input;
        this.initialiseDB();
    }

    /**
	 * Adds the response times of sub-service calls to the response times of the
	 * corresponding parent.
	 */
    public final void addSubserviceCallMetricsToParent() {
        for (Class currClass : this.inputParameter.getCustomerClasses()) {
            for (int i = this.inputParameter.getMaxSubserviceLevel(); i >= 0; i--) {
                for (int j = this.inputParameter.getSubserviceParentRelationships().size() - 1; j >= 0; j--) {
                    List<Object> entries = this.inputParameter.getSubserviceParentRelationships().get(j);
                    if (entries.get(0).equals(currClass.getClassID()) && ((Integer) entries.get(3)).intValue() == i) {
                        this.tableResponsetimePerClassAndStation.write(currClass.getClassID(), (String) entries.get(1), this.tableResponsetimePerClassAndStation.read(currClass.getClassID(), (String) entries.get(1)) + this.tableResponsetimePerClassAndStation.read(currClass.getClassID(), (String) entries.get(2)));
                    }
                }
            }
        }
    }

    public final int compute(final boolean fromWebService) {
        for (int i = 0; i < this.inputParameter.getLoadfactors().length; i++) {
            logger.info("Calculate load factor " + this.inputParameter.getLoadfactors()[i]);
            AnalyticSolverPanel.whatIfNextStepStatusChange();
            this.generateHashtablesForNewIteration();
            final boolean loadfactorIsAllowed = this.computeUtilizations(this.inputParameter.getLoadfactors()[i]);
            logger.debug("After utilization");
            if (loadfactorIsAllowed) {
                final int longQstation = this.computeQlength(this.inputParameter.getLoadfactors()[i]);
                logger.debug("after queue length computation");
                if (longQstation == -1) {
                    this.computeCompleteResponsetimes();
                    if (i == 0) {
                        this.solutionID = this.dbManager.insertScenarioAnalysis(this.scenarioID, "DecompositionPujolle", "Description", this.inputParameter.getWhatIFParameters(), startTime);
                    }
                    logger.debug("SolutionID in Derby DBMS is " + this.solutionID);
                    this.addSubserviceCallMetricsToParent();
                    visualizeAggregatedResponseTimes();
                    this.insertScenarioAndSolution(this.inputParameter.getLoadfactors()[i]);
                    System.gc();
                } else {
                    if (i == 0) {
                        logger.info("The model can�t be solved because the maximum queue-length is exceeded at least" + " at one station!");
                        if (!fromWebService) {
                            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "The station " + ((AnalyticQNStation) this.inputParameter.getStations().get(longQstation)).getStationID() + " " + ((AnalyticQNStation) this.inputParameter.getStations().get(longQstation)).getStationName() + " has got a greater queue length than the allowed one! The computation is aborted.", "Information", JOptionPane.INFORMATION_MESSAGE);
                        }
                        i++;
                        while (i < this.inputParameter.getLoadfactors().length) {
                            AnalyticSolverPanel.whatIfNextStepStatusChange();
                            i++;
                        }
                        this.dbManager.deleteScenarioDescription(this.scenarioID);
                        return -1;
                    } else {
                        logger.info("The model can�t be solved because the maximum queuelength is exceeded at least" + " at one station!");
                        logger.info("Last complete computed loadfactor was " + this.inputParameter.getLoadfactors()[i - 1]);
                        if (!fromWebService) {
                            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "The station " + ((AnalyticQNStation) this.inputParameter.getStations().get(longQstation)).getStationID() + " " + ((AnalyticQNStation) this.inputParameter.getStations().get(longQstation)).getStationName() + " has got a greater queue length than the allowed one! The computation is aborted.", "Information", JOptionPane.INFORMATION_MESSAGE);
                        }
                        String newWhatIFParameters = "MaxLoadFactor: " + this.inputParameter.getLoadfactors()[i - 1] + " NrOfSteps: " + i;
                        this.dbManager.updateWhatIFParameters(this.solutionID, newWhatIFParameters);
                        i++;
                        while (i < this.inputParameter.getLoadfactors().length) {
                            AnalyticSolverPanel.whatIfNextStepStatusChange();
                            i++;
                        }
                        break;
                    }
                }
            } else {
                if (i == 0) {
                    logger.info("Stations utilisation is at 100% for the model!");
                    if (!fromWebService) {
                        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Stations utilisation is at 100% for the model!", "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    i++;
                    while (i < this.inputParameter.getLoadfactors().length) {
                        AnalyticSolverPanel.whatIfNextStepStatusChange();
                        i++;
                    }
                    this.dbManager.deleteScenarioDescription(this.scenarioID);
                    return -1;
                } else {
                    logger.info("Stations utilisation is at 100%! The computation is aborted..." + " Last complete computed loadfactor was " + this.inputParameter.getLoadfactors()[i - 1]);
                    if (!fromWebService) {
                        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Stations utilisation is at 100%! The computation is aborted..." + " Last complete computed loadfactor was " + this.inputParameter.getLoadfactors()[i - 1], "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    String newWhatIFParameters = "MaxLoadFactor: " + this.inputParameter.getLoadfactors()[i - 1] + " NrOfSteps: " + i;
                    this.dbManager.updateWhatIFParameters(this.solutionID, newWhatIFParameters);
                    i++;
                    while (i < this.inputParameter.getLoadfactors().length) {
                        AnalyticSolverPanel.whatIfNextStepStatusChange();
                        i++;
                    }
                    break;
                }
            }
        }
        return (int) this.solutionID;
    }

    private boolean computeUtilizations(final double loadfactor) {
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double arrRate_i = 0.0;
            double utilization_i = 0.0;
            double serviceRate_i = 0.0;
            double sumTerm_i = 0.0;
            double squCoefVarOfServTime_i = 0.0;
            for (int r = 0; r < this.inputParameter.getCustomerClasses().size(); r++) {
                final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(r);
                double arrRate_ir = 0.0;
                double serviceTime_ir = 0.0;
                double utilization_ir = 0.0;
                arrRate_ir = loadfactor * this.inputParameter.getArrivalrate(currClass.getClassID()) * this.inputParameter.getVisitsMap().read(station.getStationID(), currClass.getClassID());
                serviceTime_ir = this.inputParameter.getServiceTime(station.getStationID(), currClass.getClassID());
                this.tableArrRate_ir.write(station.getStationID(), currClass.getClassID(), arrRate_ir);
                arrRate_i = arrRate_i + arrRate_ir;
                if (station.isDelay()) {
                    this.tableUtilizationPerStationAndClass.write(station.getStationID(), currClass.getClassID(), utilization_ir);
                } else {
                    utilization_ir = arrRate_ir * serviceTime_ir / station.getNrParallelWorkers();
                    if (utilization_ir >= 1) {
                        System.out.println("illegal: the utilization of station " + station.getStationID() + "of class " + currClass.getClassID() + "is already " + utilization_ir + "!");
                        return false;
                    } else {
                        this.tableUtilizationPerStationAndClass.write(station.getStationID(), currClass.getClassID(), utilization_ir);
                    }
                    utilization_i += utilization_ir;
                }
                double coefVarOfServiceTime_ir = 0.0;
                double SquCoefVarOfServTime_ir = 0.0;
                if (!station.isDelay() && (serviceTime_ir > 0.0)) {
                    coefVarOfServiceTime_ir = this.inputParameter.getStandardDeviation(station.getStationID(), currClass.getClassID()) / serviceTime_ir;
                    SquCoefVarOfServTime_ir = Math.pow(coefVarOfServiceTime_ir, 2);
                }
                this.tableSquCVofServiceTime_ir.write(station.getStationID(), currClass.getClassID(), SquCoefVarOfServTime_ir);
                double sumTerm_ir = utilization_ir * serviceTime_ir * (1 + SquCoefVarOfServTime_ir);
                sumTerm_i += sumTerm_ir;
            }
            if (utilization_i / station.getServiceMultiplier() >= 1) {
                return false;
            } else {
                this.tableUtilizationPerStation.write(station.getStationID(), utilization_i);
            }
            System.out.println("Utilization of station " + station.getStationID() + " = " + utilization_i);
            logger.debug("After utilization ouput!");
            this.tableArrRate_i.write(station.getStationID(), arrRate_i);
            logger.debug("after table array filling!");
            if (utilization_i > 0.0) {
                serviceRate_i = arrRate_i / utilization_i;
                squCoefVarOfServTime_i = -1 + serviceRate_i * sumTerm_i / (utilization_i * Math.pow(station.getNrParallelWorkers(), 2));
            }
            this.tableSquCVofServiceTime_i.write(station.getStationID(), squCoefVarOfServTime_i);
        }
        return true;
    }

    /**
	 * Computes the coefficient of variation of the inter-arrival times at each
	 * station. It is necessary for the computation of the queue length in an
	 * open non-product-form queueing network.
	 * @param loadfactor double the load factor for which utilization should be
	 *            calculated
	 * @return boolean true if computed utilization is less than 100% - false if
	 *         utilization is greater or equal to 100%
	 */
    private double[] computeCoefArrivalTimes(final double loadfactor) {
        int stationNrs = this.inputParameter.getStations().size();
        double[] squCVAcurrent = new double[stationNrs];
        double[] squCVAprevious = new double[stationNrs];
        int count = 0;
        while (true) {
            for (int s = 0; s < stationNrs; s++) {
                double squCVOfInterArrivalTimes_i = 0.0;
                double squCVOfInterDepartureTimes_i = 0.0;
                final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                for (int r = 0; r < this.inputParameter.getCustomerClasses().size(); r++) {
                    final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(r);
                    double squCVOfInterArrivalTimes_ir = 0.0;
                    double arrRate_ir = this.tableArrRate_ir.read(station.getStationID(), currClass.getClassID());
                    double visits_ir = this.inputParameter.getVisitsMap().read(station.getStationID(), currClass.getClassID());
                    if (visits_ir == 0) {
                        this.tableSquCVOfInterArrivalTimes_ir.write(station.getStationID(), currClass.getClassID(), squCVOfInterArrivalTimes_ir);
                    } else {
                        int currClassPartsNr = currClass.getClassParts().getClassPart().size();
                        ClassPart source = currClass.getClassParts().getClassPart().get(0);
                        double routingP0i_r = this.inputParameter.getProbabilityMap().read(currClass.getClassID(), source.getStationID(), station.getStationID());
                        double srcPart = 0.0;
                        if (routingP0i_r > 0) {
                            double squCVofIncoming_0i = this.inputParameter.getCoefVariationArrivalrate(currClass.getClassID());
                            double arrivalRate_r = loadfactor * this.inputParameter.getArrivalrate(currClass.getClassID());
                            srcPart = arrivalRate_r * routingP0i_r * squCVofIncoming_0i;
                        }
                        for (int cp = 1; cp < currClassPartsNr - 1; cp++) {
                            ClassPart currCP = currClass.getClassParts().getClassPart().get(cp);
                            double routingPji_r = this.inputParameter.getProbabilityMap().read(currClass.getClassID(), currCP.getStationID(), station.getStationID());
                            if (routingPji_r > 0) {
                                double squCVofIncoming_jir = 0.0;
                                if (count == 0) {
                                    squCVofIncoming_jir = 1.0;
                                } else {
                                    squCVofIncoming_jir = this.tableSquCVofIncoming_ijr.read(currClass.getClassID(), currCP.getStationID(), station.getStationID());
                                }
                                double arrRate_jr = this.tableArrRate_ir.read(currCP.getStationID(), currClass.getClassID());
                                squCVOfInterArrivalTimes_ir = squCVOfInterArrivalTimes_ir + arrRate_jr * routingPji_r * squCVofIncoming_jir;
                            }
                        }
                        if (arrRate_ir > 0.0) {
                            squCVOfInterArrivalTimes_ir = (squCVOfInterArrivalTimes_ir + srcPart) / arrRate_ir;
                        }
                        this.tableSquCVOfInterArrivalTimes_ir.write(station.getStationID(), currClass.getClassID(), squCVOfInterArrivalTimes_ir);
                    }
                    squCVOfInterArrivalTimes_i = squCVOfInterArrivalTimes_i + squCVOfInterArrivalTimes_ir * arrRate_ir;
                }
                if (this.tableArrRate_i.read(station.getStationID()) > 0.0) {
                    squCVOfInterArrivalTimes_i = squCVOfInterArrivalTimes_i / this.tableArrRate_i.read(station.getStationID());
                }
                this.tableSquCVOfInterArrivalTimes_i.write(station.getStationID(), squCVOfInterArrivalTimes_i);
                squCVAcurrent[s] = squCVOfInterArrivalTimes_i;
                if (station.isDelay()) {
                    squCVOfInterDepartureTimes_i = squCVOfInterArrivalTimes_i;
                } else {
                    double squUtilization = Math.pow(this.tableUtilizationPerStation.read(station.getStationID()), 2);
                    double divisionPart = squUtilization * (-1 + this.tableSquCVofServiceTime_i.read(station.getStationID())) / Math.sqrt(station.getNrParallelWorkers());
                    squCVOfInterDepartureTimes_i = 1 + divisionPart + (1 - squUtilization) * (squCVOfInterArrivalTimes_i - 1);
                }
                for (int r = 0; r < this.inputParameter.getCustomerClasses().size(); r++) {
                    final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(r);
                    int currClassPartsNr = currClass.getClassParts().getClassPart().size();
                    for (int cp = 1; cp < currClassPartsNr; cp++) {
                        if (station.getStationID().equals(currClass.getClassParts().getClassPart().get(cp).getStationID())) {
                            OutgoingConnections cons = currClass.getClassParts().getClassPart().get(cp).getOutgoingConnections();
                            int targetsNr = cons.getOutgoingConnection().size();
                            for (int t = 0; t < targetsNr; t++) {
                                String tClassPartID = cons.getOutgoingConnection().get(t).getTarget();
                                String tStationID = this.inputParameter.getBc().getClassSection().getStationIDOfClassPart(tClassPartID);
                                double routingP_ijr = this.inputParameter.getProbabilityMap().read(currClass.getClassID(), station.getStationID(), tStationID);
                                double SquCVofIncoming_ijr = 1 + routingP_ijr * (squCVOfInterDepartureTimes_i - 1);
                                this.tableSquCVofIncoming_ijr.write(currClass.getClassID(), station.getStationID(), tStationID, SquCVofIncoming_ijr);
                            }
                        }
                    }
                }
                if (count == 0) {
                    squCVAprevious[s] = 5.0;
                }
            }
            double[] difRatio = new double[stationNrs];
            Vector<Double> satisfied = new Vector<Double>();
            for (int s = 0; s < stationNrs; s++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                if (station.isDelay()) {
                    satisfied.addElement(difRatio[s]);
                } else {
                    if (squCVAprevious[s] > 0.0) {
                        difRatio[s] = Math.abs(squCVAcurrent[s] - squCVAprevious[s]) / squCVAprevious[s];
                        if (difRatio[s] <= 0.01) {
                            satisfied.addElement(difRatio[s]);
                        }
                    } else if ((squCVAprevious[s] == 0) && (squCVAcurrent[s] > 0)) {
                        difRatio[s] = squCVAcurrent[s];
                        if (difRatio[s] <= 0.001) {
                            satisfied.addElement(difRatio[s]);
                        }
                    }
                }
            }
            if (satisfied.size() == stationNrs) {
                return squCVAcurrent;
            } else {
                squCVAprevious = squCVAcurrent;
            }
            count++;
            logger.debug("Count = " + count);
        }
    }

    private int computeQlength(final double loadfactor) {
        double[] QueueKLB_i = new double[this.inputParameter.getStations().size()];
        this.computeCoefArrivalTimes(loadfactor);
        System.out.println("computeCoefArrivalTimes is finished");
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            if (station.isDelay()) {
                for (int r = 0; r < this.inputParameter.getCustomerClasses().size(); r++) {
                    final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(r);
                    this.tableQueuelengthPerStationAndClass.write(station.getStationID(), currClass.getClassID(), 0.0);
                }
                QueueKLB_i[s] = 0.0;
            } else {
                double utilization_i = this.tableUtilizationPerStation.read(station.getStationID());
                double divisor = 1.0;
                double sum1 = 1.0;
                double sum2 = 0.0;
                for (int k = 1; k < station.getNrParallelWorkers(); k++) {
                    double dividend = Math.pow(station.getNrParallelWorkers() * utilization_i, k);
                    divisor = divisor * k;
                    double sumFactor = dividend / divisor;
                    sum1 = sum1 + sumFactor;
                }
                sum2 = Math.pow(station.getNrParallelWorkers() * utilization_i, station.getNrParallelWorkers()) / ((1 - utilization_i) * divisor * station.getNrParallelWorkers());
                double steadyPI0_i = 1 / (sum1 + sum2);
                double waitP_i = sum2 * steadyPI0_i;
                for (int r = 0; r < this.inputParameter.getCustomerClasses().size(); r++) {
                    final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(r);
                    double utilization_ir = this.tableUtilizationPerStationAndClass.read(station.getStationID(), currClass.getClassID());
                    if (utilization_ir == 0.0) {
                        this.tableQueuelengthPerStationAndClass.write(station.getStationID(), currClass.getClassID(), 0.0);
                    } else {
                        double QMMm_ir = utilization_ir * waitP_i / (1 - utilization_i);
                        System.out.println("------waitP_i---" + waitP_i + "--------");
                        System.out.println("------QMMm_ir---" + QMMm_ir + "--------");
                        double squCVofServiceTime_ir = this.tableSquCVofServiceTime_ir.read(station.getStationID(), currClass.getClassID());
                        double squCVOfInterArrivalTimes_ir = this.tableSquCVOfInterArrivalTimes_ir.read(station.getStationID(), currClass.getClassID());
                        double QAC_ir = QMMm_ir * (squCVOfInterArrivalTimes_ir + squCVofServiceTime_ir) / 2;
                        double FactorKLB_ir = 1.0;
                        double multiplyPart = (1 - utilization_i) / (squCVOfInterArrivalTimes_ir + squCVofServiceTime_ir);
                        if ((squCVOfInterArrivalTimes_ir <= 1) && (squCVOfInterArrivalTimes_ir >= 0)) {
                            FactorKLB_ir = Math.exp(((-2) * multiplyPart * Math.pow((1 - squCVOfInterArrivalTimes_ir), 2)) / (3 * waitP_i));
                        } else if (squCVOfInterArrivalTimes_ir > 1) {
                            FactorKLB_ir = Math.exp((-1) * multiplyPart * (squCVOfInterArrivalTimes_ir - 1));
                        } else {
                            logger.debug("The squared coefficient of variation of interarrival time is negative and invalid.");
                        }
                        double exampleFactorKLB = Math.exp(((-2) * (1 - 0.02508303) / (1 + 0.99441952) * Math.pow((1 - 0.99441952), 2)) / (3 * 1.424E-8));
                        System.out.println("---------------------------" + exampleFactorKLB);
                        double minFactorKLB = 1E-10;
                        double QKLB_ir = 0.0;
                        if (FactorKLB_ir > minFactorKLB) {
                            QKLB_ir = QAC_ir * FactorKLB_ir;
                        } else {
                            QKLB_ir = QAC_ir * minFactorKLB;
                            logger.debug("The utilization of the station " + station.getStationName() + " is too small so that the factor G_KLB has no difference from zero and loses its function to modify the resluts Q_AC. ");
                        }
                        this.tableQueuelengthPerStationAndClass.write(station.getStationID(), currClass.getClassID(), QKLB_ir);
                        QueueKLB_i[s] += QKLB_ir;
                    }
                }
            }
            this.tableQueuelengthPerStation.write(station.getStationID(), QueueKLB_i[s]);
            if (station.getQueueLength() != -1 && QueueKLB_i[s] > station.getQueueLength()) {
                logger.debug("The maximum allowed queuelength " + station.getQueueLength() + " at the station " + station.getStationID() + " " + station.getStationName() + " is " + QueueKLB_i[s] + ", exceeded for loadfactor " + loadfactor);
                return s;
            }
        }
        return -1;
    }

    private void computeCompleteResponsetimes() {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class currClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            double responsetime = 0.0;
            for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                double responsetime_ir = 0.0;
                if (station.isDelay()) {
                    responsetime_ir = this.inputParameter.getServiceDemand(station.getStationID(), currClass.getClassID());
                } else {
                    double queueLength_ir = this.tableQueuelengthPerStationAndClass.read(station.getStationID(), currClass.getClassID());
                    double utilization_ir = this.tableUtilizationPerStationAndClass.read(station.getStationID(), currClass.getClassID());
                    double arrRate_ir = this.tableArrRate_ir.read(station.getStationID(), currClass.getClassID());
                    double meanNrJobs_ir = queueLength_ir + utilization_ir * station.getNrParallelWorkers();
                    if (arrRate_ir == 0) {
                        responsetime_ir = 0;
                    } else {
                        responsetime_ir = meanNrJobs_ir / arrRate_ir;
                    }
                }
                this.tableResponsetimePerClassAndStation.write(currClass.getClassID(), station.getStationID(), responsetime_ir);
                responsetime = responsetime + responsetime_ir;
            }
            this.tableResponsetimePerClass.write(currClass.getClassID(), responsetime);
        }
    }

    /**
	 * Generates all needed HashTables at the beginning of each iteration.
	 */
    public final void generateHashtablesForNewIteration() {
        this.tableUtilizationPerStationAndClass = new HashTable("AS TEMP Utilization Per Station And Class");
        this.tableUtilizationPerStation = new HashTable("AS TEMP Utilization Per Station");
        this.tableQueuelengthPerStationAndClass = new HashTable("AS TEMP Queuelength Per Station And Class");
        this.tableQueuelengthPerStation = new HashTable("AS TEMP MVA Queuelength Per Station");
        this.tableResponsetimePerClassAndStation = new HashTable("AS TEMP Responsetime Per Class And Station");
        this.tableResponsetimePerClass = new HashTable("AS TEMP Responsetime Per Class");
        this.tableArrRate_i = new HashTable("AS TEMP arrival rate of each Station");
        this.tableArrRate_ir = new HashTable("AS TEMP arrival rate of each Class at each Station");
        this.tableSquCVofServiceTime_i = new HashTable("AS TEMP squared Coefficient of Variation of Service Time at each Station");
        this.tableSquCVofServiceTime_ir = new HashTable("AS TEMP squared Coefficient of Variation of Service Time Per Class And Station");
        this.tableSquCVofIncoming_ijr = new HashTable("AS TEMP squared Coefficient of Variation of incoming from station i to j per class");
        this.tableSquCVOfInterArrivalTimes_i = new HashTable("AS TEMP squared Coefficient of Variation of Interarrival Times Per Station");
        this.tableSquCVOfInterArrivalTimes_ir = new HashTable("AS TEMP squared Coefficient of Variation of Interarrival Times Per Class And Station");
    }

    /**
	 * Initializes the result DB for the results of this DecompositonPujolle
	 * algorithm. Writes the model into the database that should be solved by
	 * this algorithm.
	 */
    public final void initialiseDB() {
        boolean success;
        if (!this.dbManager.databaseExists()) {
            success = this.dbManager.initializeTables();
            if (success) {
                logger.debug("Tables are successfully initialized in result DB for algorithm DecompositonPujolle.");
            } else {
                logger.debug("Tables in result DB for algorithm DecompositonPujolle not initialized!");
            }
        } else {
            logger.debug("Result DB for algorithm DecompositonPujolle already initialized!");
        }
        this.scenarioID = this.dbManager.insertScenarioDescription(this.inputParameter.getBc());
    }

    /**
	 * Initializes the Database for the algorithm. Writes the model to be solved
	 * into the database.
	 * @param loadfactor The actual load factor for which the results are
	 *            written into the database
	 */
    public void insertScenarioAndSolution(final double loadfactor) {
        double arrivalrate = 0;
        final long executionID = this.dbManager.insertScenarioDefinitions(this.solutionID, "Description");
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
            arrivalrate = loadfactor * this.inputParameter.getArrivalrate(customerclass.getClassID());
            this.dbManager.insertClassScenario(executionID, customerclass.getClassID(), this.scenarioID, arrivalrate, loadfactor);
        }
        this.dbManager.insertAnalyticScenarioResults(executionID, arrivalrate);
        Vector<ClassResultBean> crbVector = new Vector<ClassResultBean>();
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            ClassResultBean crb = new ClassResultBean();
            Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
            crb.setClassID(customerclass.getClassID());
            crb.setExecutionID(executionID);
            double responseTime = this.tableResponsetimePerClass.read(customerclass.getClassID());
            crb.setResponseTime(responseTime);
            double throughput = loadfactor * this.inputParameter.getArrivalrate(customerclass.getClassID());
            crb.setThroughput(throughput);
            crbVector.add(crb);
        }
        this.dbManager.insertAnalyticTotalClassResultsBatch(crbVector);
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            this.dbManager.insertAnalyticTotalStationResults(station.getStationID(), executionID, this.tableQueuelengthPerStation.read(station.getStationID()), this.tableUtilizationPerStation.read(station.getStationID()), -1);
            Vector<ClassStationResultBean> csrbVector = new Vector<ClassStationResultBean>();
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
                ClassStationResultBean csrBean = new ClassStationResultBean();
                csrBean.setClassID(customerclass.getClassID());
                csrBean.setExecutionID(executionID);
                csrBean.setQueueLength(this.tableQueuelengthPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                csrBean.setResponseTime(this.tableResponsetimePerClassAndStation.read(customerclass.getClassID(), station.getStationID()));
                csrBean.setStationID(station.getStationID());
                csrBean.setUtilization(this.tableUtilizationPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                csrbVector.add(csrBean);
            }
            this.dbManager.insertAnalyticClassStationResultsBatch(csrbVector);
        }
    }

    public final void visualizeAggregatedResponseTimes() {
        logger.debug("Logger;Measure;Server;MeanValue");
        for (Class currClass : this.inputParameter.getCustomerClasses()) {
            for (int j = this.inputParameter.getSubserviceParentRelationships().size() - 1; j >= 0; j--) {
                List<Object> entries = this.inputParameter.getSubserviceParentRelationships().get(j);
                if (entries.get(0).equals(currClass.getClassID()) && ((Integer) entries.get(3)).intValue() == 1) {
                    if (currClass.getClassID().equals("class2")) {
                        logger.error(";" + "Response Time " + currClass.getClassID() + ";" + PerMoToBusinessCase.getInstance().getStationSection().getStationName((String) entries.get(1)) + ";" + this.tableResponsetimePerClassAndStation.read(currClass.getClassID(), (String) entries.get(1)));
                    }
                }
            }
        }
    }
}
