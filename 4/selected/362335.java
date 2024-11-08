package de.ibis.permoto.solver.as.tech;

import java.sql.Timestamp;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.ibis.permoto.gui.solver.panels.AnalyticSolverPanel;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.solver.as.util.AlgorithmInput;
import de.ibis.permoto.solver.as.util.AnalyticQNStation;
import de.ibis.permoto.solver.as.util.HashTable;
import de.ibis.permoto.util.db.ClassResultBean;
import de.ibis.permoto.util.db.ClassStationResultBean;
import de.ibis.permoto.util.db.DBManager;

/**
 * Computes an mixed queueing network with multiple classes without
 * load-dependent stations. The algorithm is analogous to Menasce (Performance
 * by design) at page 372.
 * @author Christian Markl
 * @author Oliver H�hn
 */
public class MixedMM1 implements QNAlgorithm {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(MixedMM1.class);

    /** The complete throughput of the model. */
    private double completeThroughput;

    /** The manager for the interaction with the DB. */
    private DBManager dbManager = DBManager.getInstance();

    /** The input parameters for the algorithm. */
    private AlgorithmInput inputParameter;

    /** Unique id of the solution which is computed in the DB. */
    private int solutionID;

    /** Unique id of the inserted scenario. */
    private int scenarioID;

    /** Start time of the execution. */
    private Timestamp startTime;

    /** The HashTable for the estimator. */
    private HashTable tableMVAEstimatorPerStationAndClass;

    /** The HashTable for the filling. */
    private HashTable tableMVAFillingPerStation;

    /** The HashTable for the filling per class. */
    private HashTable tableMVAFillingPerStationAndClassN;

    /** The HashTable for the filling per class with one customer less. */
    private HashTable tableMVAFillingPerStationAndClassNminus1;

    /** The HashTable for the response time per class. */
    private HashTable tableMVAResponseTimePerClass;

    /** The HashTable for the response time per station and class. */
    private HashTable tableMVAResponseTimePerStationAndClass;

    /** The HashTable for the throughput per class. */
    private HashTable tableMVAThroughputPerClass;

    /** Hashtable for the queue length of a station per class. */
    private HashTable tableQueuelengthPerStationAndClass;

    /** Hashtable for the queue length of a station for all open classes. */
    private HashTable tableQueuelengthPerStationOpen;

    /** Hashtable for the utilization of a station over all classes. */
    private HashTable tableUtilisationPerStation;

    /** Hashtable for the utilization of a station per class. */
    private HashTable tableUtilisationPerStationAndClass;

    /**
	 * The constructor.
	 * @param input The input parameters for the algorithm
	 */
    public MixedMM1(final AlgorithmInput input) {
        startTime = new Timestamp(System.currentTimeMillis());
        this.inputParameter = input;
        this.initialiseDB();
    }

    /**
	 * Adds the response times of subservice calls to the response times of the
	 * corresponding parent.
	 */
    public final void addSubserviceCallMetricsToParent() {
        for (Class currClass : this.inputParameter.getCustomerClasses()) {
            for (int i = this.inputParameter.getMaxSubserviceLevel(); i >= 0; i--) {
                for (int j = this.inputParameter.getSubserviceParentRelationships().size() - 1; j >= 0; j--) {
                    List<Object> entries = this.inputParameter.getSubserviceParentRelationships().get(j);
                    if (entries.get(0).equals(currClass.getClassID()) && ((Integer) entries.get(3)).intValue() == i) {
                        this.tableMVAResponseTimePerStationAndClass.write((String) entries.get(1), currClass.getClassID(), this.tableMVAResponseTimePerStationAndClass.read((String) entries.get(1), currClass.getClassID()) + this.tableMVAResponseTimePerStationAndClass.read((String) entries.get(2), currClass.getClassID()));
                    }
                }
            }
        }
    }

    /**
	 * Adopts the estimators for the number of customer at each station into
	 * tableMVAFillingPerStationAndClassN.
	 */
    private void adoptFillingPerNonLDStationAndClosedClassForNEstimator() {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    if (!station.isLD()) {
                        final double value = this.tableMVAEstimatorPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                        this.tableMVAFillingPerStationAndClassN.write(station.getStationID(), customerClass.getClassID(), value);
                        logger.debug("The estimator for the filling for class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + value);
                    }
                }
            }
        }
        this.tableMVAEstimatorPerStationAndClass = null;
    }

    /**
	 * The approximation of the queueing length for N - 1 customers is done
	 * here. This only works for LI and Delay devices. At LD devices the
	 * probabilities has to be computed.
	 * @param loadfactor Loadfactor
	 */
    private void approximateFillingPerNonLDStationAndClosedClassNMinus1(final double loadfactor) {
        for (int c1 = 0; c1 < this.inputParameter.getCustomerClasses().size(); c1++) {
            final Class customerClass1 = (Class) this.inputParameter.getCustomerClasses().get(c1);
            if (!customerClass1.isOpenClass()) {
                final double nR = customerClass1.getPopulation() * loadfactor;
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    if (!station.isLD()) {
                        final double nIrOfN = this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerClass1.getClassID());
                        for (int t = 0; t < this.inputParameter.getCustomerClasses().size(); t++) {
                            final Class customerClassT = (Class) this.inputParameter.getCustomerClasses().get(t);
                            if (!customerClassT.isOpenClass()) {
                                if (customerClassT.getClassID().equals(customerClass1.getClassID())) {
                                    final double nIrOfNMinus1 = ((nR - 1) / nR) * nIrOfN;
                                    this.tableMVAFillingPerStationAndClassNminus1.write(station.getStationID(), customerClass1.getClassID(), customerClassT.getClassID(), nIrOfNMinus1);
                                    logger.debug("The approximated queueing length of class " + customerClass1.getClassID() + " " + customerClass1.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " when seeing class " + customerClassT.getClassID() + " " + customerClassT.getClassName() + " is: " + nIrOfNMinus1);
                                } else {
                                    final double nIrOfNMinus1 = nIrOfN;
                                    this.tableMVAFillingPerStationAndClassNminus1.write(station.getStationID(), customerClass1.getClassID(), customerClassT.getClassID(), nIrOfNMinus1);
                                    logger.debug("The approximated queueing length of class " + customerClass1.getClassID() + " " + customerClass1.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " when seeing class " + customerClassT.getClassID() + " " + customerClassT.getClassName() + " is: " + nIrOfNMinus1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
	 * Controls the computation of the queueing network. For each loadfactor one
	 * result is computed.
	 */
    public final int compute(final boolean fromWebService) {
        for (int i = 0; i < this.inputParameter.getLoadfactors().length; i++) {
            logger.info("Calculate load factor " + this.inputParameter.getLoadfactors()[i]);
            AnalyticSolverPanel.whatIfNextStepStatusChange();
            this.generateAllHashTables();
            final boolean loadfactorIsAllowed = this.computeUtilisationsForOpenClasses(this.inputParameter.getLoadfactors()[i]);
            if (loadfactorIsAllowed) {
                this.setNewServicedemands();
                this.computeVariablesForClosedClasses(this.inputParameter.getLoadfactors()[i]);
                this.computeResponsetimeAndQueuelengthForOpenClasses(this.inputParameter.getLoadfactors()[i]);
                if (i == 0) {
                    this.solutionID = this.dbManager.insertScenarioAnalysis(this.scenarioID, "MixedMM1", "Description", this.inputParameter.getWhatIFParameters(), startTime);
                }
                logger.debug("SolutionID in Derby DBMS is " + this.solutionID);
                this.addSubserviceCallMetricsToParent();
                this.insertScenarioAndSolution(this.inputParameter.getLoadfactors()[i]);
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

    /**
	 * After the MVA the complete throughput is computed. This is done by
	 * summing all values of the throughput per class.
	 */
    private void computeCompleteThroughput(double loadfactor) {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                this.completeThroughput = this.completeThroughput + this.tableMVAThroughputPerClass.read(customerClass.getClassID());
            } else {
                this.completeThroughput = this.completeThroughput + loadfactor * this.inputParameter.getArrivalrate(customerClass.getClassID());
            }
        }
        logger.debug("The complete throughput is " + this.completeThroughput);
    }

    /**
	 * Computes the error as in the Bard-Schweitzer algorithm.
	 * @return true (next iteration) - false (abort)
	 */
    private boolean computeError() {
        double maxError = 0.0;
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    if (station.isLI()) {
                        final double nIr = this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerClass.getClassID());
                        final double nIrEstimated = this.tableMVAEstimatorPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                        final double error = Math.abs((nIrEstimated - nIr) / nIrEstimated);
                        if (error > maxError) {
                            maxError = error;
                        }
                    }
                }
            }
        }
        if (maxError > this.inputParameter.getEpsilon()) {
            return true;
        }
        return false;
    }

    /**
	 * After the MVA the queue length at each station is computed. This is done
	 * by summing all lengths per class.
	 */
    private void computeFillingPerNonLDStation() {
        for (int q = 0; q < this.inputParameter.getStations().size(); q++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(q);
            if (!station.isLD()) {
                double filling = 0;
                for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                    final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
                    if (!customerClass.isOpenClass()) {
                        filling = filling + this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerClass.getClassID());
                    }
                }
                this.tableMVAFillingPerStation.write(station.getStationID(), filling);
                logger.debug("The station " + station.getStationID() + " " + station.getStationName() + " has got a filling of: " + filling);
            }
        }
    }

    /**
	 * After the throughput the utilizations are recomputed for the next step.
	 */
    private void computeNewUtilisationForClosedClasses() {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    final double throughput = this.tableMVAThroughputPerClass.read(customerClass.getClassID());
                    final double utilisation = throughput * serviceDemand;
                    this.tableUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), utilisation);
                }
            }
        }
    }

    /**
	 * Computes the queue lengths and response times of the open classes.
	 * @param loadfactor actual loadfactor for which the model is solved
	 */
    private void computeResponsetimeAndQueuelengthForOpenClasses(final double loadfactor) {
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double queuelength = 0.0;
            double responsetime = 0.0;
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
                if (customerClass.isOpenClass()) {
                    final double responsetimePerClass = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID()) * (1 + this.tableMVAFillingPerStation.read(station.getStationID()));
                    responsetime = responsetime + responsetimePerClass;
                    this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), responsetimePerClass);
                    logger.debug("Responsetime of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + responsetimePerClass);
                    final double arrival = this.inputParameter.getArrivalrate(customerClass.getClassID());
                    final double queuelengthPerClass = loadfactor * arrival * responsetimePerClass;
                    queuelength = queuelength + queuelengthPerClass;
                    this.tableQueuelengthPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), queuelengthPerClass);
                }
            }
            this.tableQueuelengthPerStationOpen.write(station.getStationID(), queuelength);
        }
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            double responsetime = 0.0;
            if (customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double responsetimePerStation = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID()) * (1 + this.tableMVAFillingPerStation.read(station.getStationID()));
                    responsetime = responsetime + responsetimePerStation;
                }
            }
            this.tableMVAResponseTimePerClass.write(customerClass.getClassID(), responsetime);
        }
    }

    /**
	 * Response time per class is computed.
	 */
    private void computeResponsetimePerClosedClass() {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                double sum = 0;
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    sum = sum + this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                }
                this.tableMVAResponseTimePerClass.write(customerClass.getClassID(), sum);
                logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an response time of: " + sum);
            }
        }
    }

    /**
	 * Mean response time per queue and class. Delay Queue: Service demand LI
	 * Queue: Service demand * (1 + Jobs of this class before) LD Queue: Service
	 * demand * probability.
	 * @param loadfactor Loadfactor
	 */
    private void computeResponsetimePerStationAndClosedClass(final double loadfactor) {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    if (station.isDelay()) {
                        this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                    } else if (station.isLI()) {
                        double sumterm = 0;
                        for (int t = 0; t < this.inputParameter.getCustomerClasses().size(); t++) {
                            final Class customerClassT = (Class) this.inputParameter.getCustomerClasses().get(t);
                            if (!customerClassT.isOpenClass()) {
                                sumterm = sumterm + this.tableMVAFillingPerStationAndClassNminus1.read(station.getStationID(), customerClassT.getClassID(), customerClass.getClassID());
                            }
                        }
                        final double responseTimePerQueueAndClass = serviceDemand * (1 + sumterm);
                        this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                        logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " has got a response time of: " + responseTimePerQueueAndClass);
                    }
                }
            }
        }
    }

    /**
	 * Throughput per class. = number of customers in class / needed time per
	 * class.
	 * @param loadfactor Loadfactor
	 */
    private void computeThroughputPerClosedClass(final double loadfactor) {
        double sumOfAllResponsetimes = 0;
        double sumOfAllThinktimes = 0;
        if (this.inputParameter.getBc().getClassSection().isAreClassesCoupled()) {
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
                if (!customerClass.isOpenClass()) {
                    sumOfAllResponsetimes = sumOfAllResponsetimes + this.tableMVAResponseTimePerClass.read(customerClass.getClassID());
                }
            }
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
                if (!customerClass.isOpenClass()) {
                    sumOfAllThinktimes = sumOfAllThinktimes + customerClass.getThinktime();
                }
            }
        }
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                final double numberOfUsers = (customerClass.getPopulation()) * loadfactor;
                if (this.inputParameter.getBc().getClassSection().isAreClassesCoupled()) {
                    final double throughputPerClass = numberOfUsers / (sumOfAllThinktimes + sumOfAllResponsetimes);
                    this.tableMVAThroughputPerClass.write(customerClass.getClassID(), throughputPerClass);
                    logger.debug("The coupled class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an throughput of: " + throughputPerClass);
                } else {
                    final double responseTimePerClass = this.tableMVAResponseTimePerClass.read(customerClass.getClassID());
                    final double throughputPerClass = numberOfUsers / (customerClass.getThinktime() + responseTimePerClass);
                    this.tableMVAThroughputPerClass.write(customerClass.getClassID(), throughputPerClass);
                    logger.debug("The uncoupled class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an throughput of: " + throughputPerClass);
                }
            }
        }
    }

    /**
	 * Computes the utilization of the open queueing network. If the utilization
	 * is greater or equal to 100% the algorithm returns false.
	 * @param loadfactor actual loadfactor for which the model is solved
	 * @return true if utilization is less than 100% false if utilization is
	 *         greater or equal to 100%
	 */
    private boolean computeUtilisationsForOpenClasses(final double loadfactor) {
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double utilistationOfThisStation = 0.0;
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
                if (customerClass.isOpenClass()) {
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    final double arrivalRate = loadfactor * this.inputParameter.getArrivalrate(customerClass.getClassID());
                    final double utilisationOfThisStationForThisClass = arrivalRate * serviceDemand;
                    if (utilisationOfThisStationForThisClass >= 1) {
                        return false;
                    } else {
                        logger.debug("Utilisation of station " + station.getStationID() + " " + station.getStationName() + " for class  " + customerClass.getClassID() + " " + customerClass.getClassName() + " = " + utilisationOfThisStationForThisClass);
                        this.tableUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), utilisationOfThisStationForThisClass);
                    }
                    utilistationOfThisStation = utilistationOfThisStation + utilisationOfThisStationForThisClass;
                }
            }
            if (utilistationOfThisStation >= 1) {
                return false;
            } else {
                this.tableUtilisationPerStation.write(station.getStationID(), utilistationOfThisStation);
            }
        }
        return true;
    }

    /**
	 * Computes the utilization, response time and so on with the
	 * Bard-Schweitzer algorithm.
	 * @param loadfactor actual loadfactor for which the model is solved
	 */
    private void computeVariablesForClosedClasses(final double loadfactor) {
        this.estimateInitialFillingPerStationAndClosedClassForN(loadfactor);
        boolean errorToBig = true;
        int counter = 0;
        while (errorToBig) {
            counter++;
            this.generateMVAHashTablesForNewIteration();
            this.adoptFillingPerNonLDStationAndClosedClassForNEstimator();
            this.approximateFillingPerNonLDStationAndClosedClassNMinus1(loadfactor);
            this.computeResponsetimePerStationAndClosedClass(loadfactor);
            this.computeResponsetimePerClosedClass();
            this.computeThroughputPerClosedClass(loadfactor);
            this.computeNewUtilisationForClosedClasses();
            this.estimateFillingPerNonLDStationAndClosedClassForN();
            errorToBig = this.computeError();
        }
        logger.debug("For loadfactor " + loadfactor + " " + counter + " MVA iterations were neccessary.");
        this.computeCompleteThroughput(loadfactor);
        this.computeFillingPerNonLDStation();
    }

    /**
	 * For ./M/1 stations this, new estimators are computed at the end of each
	 * MVA iteration. They are written in tableMVAEstimatorPerStationAndClass so
	 * that at the next step the fault tolerance can be checked. If afterwords
	 * another iteration follows these values are copied into
	 * tableMVAFillingPerStationAndClassN.
	 */
    private void estimateFillingPerNonLDStationAndClosedClassForN() {
        this.generateHashTableFillingEstimator();
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                final double throughputPerClass = this.tableMVAThroughputPerClass.read(customerClass.getClassID());
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    if (!station.isLD()) {
                        final double responseTimePerStationAndClass = this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                        final double newEstimator = throughputPerClass * responseTimePerStationAndClass;
                        this.tableMVAEstimatorPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), newEstimator);
                        logger.debug("After this iteration the filling estimator of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + newEstimator);
                    }
                }
            }
        }
    }

    /**
	 * At the beginning of the MVA iteration the first estimate values for the
	 * number of customers at each station are computed. Starting basis is an
	 * assumed uniform distribution of all customers in one class per station
	 * for stations through which the class flows. The table
	 * tableMVAEstimatorPerStationAndClass is just a cache for the values.
	 * @param loadfactor Loadfactor
	 */
    private void estimateInitialFillingPerStationAndClosedClassForN(final double loadfactor) {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (!customerClass.isOpenClass()) {
                final int kR = this.inputParameter.numberOfStationsWithServiceDemandGreaterZeroForClass(customerClass.getClassID());
                final double nR = customerClass.getPopulation() * loadfactor;
                final double estimator = nR / kR;
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    this.tableMVAEstimatorPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), estimator);
                    logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " has got an initial filling estimator of: " + estimator);
                }
            }
        }
    }

    /**
	 * Generates all HashTables in the beginning for each loadfactor.
	 */
    private void generateAllHashTables() {
        this.tableUtilisationPerStationAndClass = new HashTable("AS TEMP Utilisation Per Station And Class");
        this.tableUtilisationPerStation = new HashTable("AS TEMP Utilisation Per Station");
        this.generateMVAHashTablesForNewIteration();
        this.generateHashTableFillingEstimator();
    }

    /**
	 * Generates the HashTable for the estimator of the queue length.
	 */
    private void generateHashTableFillingEstimator() {
        this.tableMVAEstimatorPerStationAndClass = new HashTable("AS TEMP MVA Estimator");
        logger.debug("The HashTable tableMVAEstimatorPerStationAndClass is generated");
    }

    /**
	 * Generates all HashTables at the beginning of each iteration.
	 */
    private void generateMVAHashTablesForNewIteration() {
        this.tableQueuelengthPerStationOpen = new HashTable("AS TEMP Queuelength Per Station For Open Classes");
        this.tableQueuelengthPerStationAndClass = new HashTable("AS TEMP Queuelength Per Station And Open Class");
        this.tableMVAFillingPerStationAndClassN = new HashTable("AS TEMP MVA Filling Per Class (N)");
        this.tableMVAFillingPerStationAndClassNminus1 = new HashTable("AS TEMP MVA Filling Per Class (N-1)");
        this.tableMVAResponseTimePerStationAndClass = new HashTable("AS TEMP MVA Response Time Per Station And Class");
        this.tableMVAResponseTimePerClass = new HashTable("AS TEMP MVA Response Time Per Class");
        this.tableMVAThroughputPerClass = new HashTable("AS TEMP MVA Throughput Per Class");
        this.tableMVAFillingPerStation = new HashTable("AS TEMP MVA Filling");
        logger.debug("All HashTables for new iteration are generated");
    }

    /**
	 * Initializes the database for the algorithm. Writes the model to be solved
	 * into the database.
	 */
    public final void initialiseDB() {
        boolean success;
        if (!this.dbManager.databaseExists()) {
            success = this.dbManager.initializeTables();
            if (success) {
                logger.debug("Initialized the tables");
            } else {
                logger.debug("Tables not initialized!");
            }
        } else {
            logger.debug("DB already initialized!");
        }
        this.scenarioID = this.dbManager.insertScenarioDescription(this.inputParameter.getBc());
    }

    /**
	 * Initializes the database for the algorithm. Writes the model to be solved
	 * into the database.
	 * @param loadfactor The actual loadfactor for which the results are written
	 *            into the database
	 */
    public void insertScenarioAndSolution(final double loadfactor) {
        double arrivalrate;
        double numberOfUsers;
        final long executionID = this.dbManager.insertScenarioDefinitions(this.solutionID, "Description");
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (customerclass.isOpenClass()) {
                arrivalrate = loadfactor * this.inputParameter.getArrivalrate(customerclass.getClassID());
                this.dbManager.insertClassScenario(executionID, customerclass.getClassID(), this.scenarioID, arrivalrate, loadfactor);
            } else {
                numberOfUsers = loadfactor * customerclass.getPopulation();
                this.dbManager.insertClassScenario(executionID, customerclass.getClassID(), this.scenarioID, numberOfUsers, loadfactor);
            }
        }
        this.dbManager.insertAnalyticScenarioResults(executionID, this.completeThroughput);
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
            if (customerclass.isOpenClass()) {
                Vector<ClassResultBean> results = new Vector<ClassResultBean>();
                ClassResultBean crb = new ClassResultBean();
                crb.setClassID(customerclass.getClassID());
                crb.setExecutionID(executionID);
                crb.setResponseTime(this.tableMVAResponseTimePerClass.read(customerclass.getClassID()));
                crb.setThroughput(loadfactor * this.inputParameter.getArrivalrate(customerclass.getClassID()));
                this.dbManager.insertAnalyticTotalClassResultsBatch(results);
            } else {
                Vector<ClassResultBean> results = new Vector<ClassResultBean>();
                ClassResultBean crb = new ClassResultBean();
                crb.setClassID(customerclass.getClassID());
                crb.setExecutionID(executionID);
                crb.setResponseTime(this.tableMVAResponseTimePerClass.read(customerclass.getClassID()));
                crb.setThroughput(this.tableMVAThroughputPerClass.read(customerclass.getClassID()));
                this.dbManager.insertAnalyticTotalClassResultsBatch(results);
            }
        }
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            this.dbManager.insertAnalyticTotalStationResults(station.getStationID(), executionID, this.tableQueuelengthPerStationOpen.read(station.getStationID()), this.tableUtilisationPerStation.read(station.getStationID()), this.tableMVAFillingPerStation.read(station.getStationID()));
            for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
                final Class customerclass = (Class) this.inputParameter.getCustomerClasses().get(c);
                if (customerclass.isOpenClass()) {
                    Vector<ClassStationResultBean> results = new Vector<ClassStationResultBean>();
                    ClassStationResultBean csrb = new ClassStationResultBean();
                    csrb.setClassID(customerclass.getClassID());
                    csrb.setExecutionID(executionID);
                    csrb.setQueueLength(this.tableQueuelengthPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                    csrb.setResponseTime(this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                    csrb.setStationID(station.getStationID());
                    csrb.setUtilization(this.tableUtilisationPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                    this.dbManager.insertAnalyticClassStationResultsBatch(results);
                } else {
                    Vector<ClassStationResultBean> results = new Vector<ClassStationResultBean>();
                    ClassStationResultBean csrb = new ClassStationResultBean();
                    csrb.setClassID(customerclass.getClassID());
                    csrb.setExecutionID(executionID);
                    csrb.setQueueLength(this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerclass.getClassID()));
                    csrb.setResponseTime(this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                    csrb.setStationID(station.getStationID());
                    csrb.setUtilization(this.tableUtilisationPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                    this.dbManager.insertAnalyticClassStationResultsBatch(results);
                }
            }
        }
    }

    /**
	 * Sets the new service demands, which are changed because of the
	 * interference with the open classes.
	 */
    private void setNewServicedemands() {
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputParameter.getCustomerClasses().get(c);
            for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                final double newValue = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID()) / (1 - this.tableUtilisationPerStation.read(station.getStationID()));
                logger.debug("Servicedemand of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + newValue);
                this.inputParameter.setServiceDemand(station.getStationID(), customerClass.getClassID(), newValue);
            }
        }
    }
}
