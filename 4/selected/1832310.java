package de.ibis.permoto.solver.as.tech;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.ibis.permoto.gui.solver.panels.AnalyticSolverPanel;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.solver.as.util.AlgorithmInput;
import de.ibis.permoto.solver.as.util.AnalyticQNStation;
import de.ibis.permoto.solver.as.util.HashTable;
import de.ibis.permoto.solver.as.util.Population;
import de.ibis.permoto.util.db.ClassResultBean;
import de.ibis.permoto.util.db.ClassStationResultBean;
import de.ibis.permoto.util.db.DBManager;

/**
 * Computes an mixed queueing network with multiple classes without
 * loaddependent stations. The algorithm is analogous to Bolch
 * (Leistungsbewertung von Rechensystemen) at page 228 et seq.
 * @author Christian Markl
 * @author Oliver H�hn
 */
public class MixedMM1PrioPR implements QNAlgorithm {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(MixedMM1PrioPR.class);

    /**
	 * In this array the current population is saved. This is done by saving the
	 * number of customers at each station at one time.
	 */
    private double[] actualLevelInClass;

    /** Arraylist with all possible populations. */
    private ArrayList<Population> allPopulations;

    /** The complete throughput of the model. */
    private double completeThroughput;

    /**
	 * ArrayList in which the customerclasses are sorted by there priority.
	 * Highest priority is in the front.
	 */
    private ArrayList<Class> customerClasses = new ArrayList<Class>();

    /** The manager for the interaction with the db. */
    private DBManager dbManager = DBManager.getInstance();

    /**
	 * In this array it is saved how much different populations are possible for
	 * one customer less in the network.
	 */
    private double[] distanceToPredecessorPopulationWith1CustomerLessInClass;

    /** The inputparameters for the algorithm. */
    private AlgorithmInput inputParameter;

    /** In this array the maximum customers for each class are saved. */
    private double[] maximumLevelInClass;

    /** Number of classes. */
    private int maxIndexClass;

    /** Unique id of the solution which is computed in the db. */
    private int solutionID;

    /** Unique id of the inserted scenario. */
    private int scenarioID;

    /** Start time of the execution. */
    private Timestamp startTime;

    /** The HashTable for the filling. */
    private HashTable tableMVAFillingPerStation;

    /** The HashTable for the filling per class. */
    private HashTable tableMVAFillingPerStationAndClassN;

    /** The HashTable for the response time per class. */
    private HashTable tableMVAResponseTimePerClass;

    /** The HashTable for the response time per station and class. */
    private HashTable tableMVAResponseTimePerStationAndClass;

    /** The HashTable for the throughput per class. */
    private HashTable tableMVAThroughputPerClass;

    /** Hashtable for the queuelength of a station per class. */
    private HashTable tableQueuelengthPerStationAndClass;

    /** Hashtable for the quelength of a station for all open classes. */
    private HashTable tableQueuelengthPerStationOpen;

    /** Hashtable for the utilisation of a station over all classes. */
    private HashTable tableUtilisationPerStation;

    /** Hashtable for the utilisation of a station per class. */
    private HashTable tableUtilisationPerStationAndClass;

    /**
	 * The constructor of the class.
	 * @param input The inputparameters for the algorithm
	 */
    public MixedMM1PrioPR(final AlgorithmInput input) {
        startTime = new Timestamp(System.currentTimeMillis());
        this.inputParameter = input;
        this.sortClasses();
        this.initialiseDB();
    }

    /**
	 * Adds the response times of subservice calls to the responsetimes of the
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
	 * Controls the computation of the queueing network. For each loadfactor one
	 * result is computed.
	 * @return int analysisID to initialize resultGUI
	 */
    public final int compute(final boolean fromWebService) {
        for (int i = 0; i < this.inputParameter.getLoadfactors().length; i++) {
            logger.info("Calculate load factor " + this.inputParameter.getLoadfactors()[i]);
            AnalyticSolverPanel.whatIfNextStepStatusChange();
            this.generateAllHashTables();
            final boolean loadfactorIsAllowed = this.computeUtilisationsForOpenClasses(this.inputParameter.getLoadfactors()[i]);
            if (loadfactorIsAllowed) {
                this.setNewServicedemands();
                this.computeAllPossiblePopulations(this.inputParameter.getLoadfactors()[i]);
                this.setInitialFillingOfZeroPopulation();
                this.setResponseTimeForOpenClassesForZeroPopulation();
                for (int a = 1; a < this.allPopulations.size(); a++) {
                    final Population pop = (Population) this.allPopulations.get(a);
                    this.computeResponseTimePerStationAndClosedClass(pop);
                    this.computeResponsetimeForClosedClasses(pop);
                    this.computeThroughputPerClosedClass(this.inputParameter.getLoadfactors()[i], pop);
                    this.computeNewUtilisationForClosedClasses(pop);
                    this.computeFillingPerNonLDDevice(pop);
                    this.computeQueueLengthPerOpenClass(pop);
                }
                final Population pop = (Population) this.allPopulations.get(this.allPopulations.size() - 1);
                this.copyPopulationHashTablesIntoMVATables(pop);
                this.computeResponsetimeAndQueuelengthForOpenClasses(this.inputParameter.getLoadfactors()[i]);
                if (i == 0) {
                    this.solutionID = this.dbManager.insertScenarioAnalysis(this.scenarioID, "MixedMM1PrioPR", "Description", this.inputParameter.getWhatIFParameters(), startTime);
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
	 * Sets all parameters and starts the recursion.
	 * @param loadfactor Loadfactor
	 */
    private void computeAllPossiblePopulations(final double loadfactor) {
        this.allPopulations = new ArrayList<Population>();
        int closedClassesCount = 0;
        for (int i = 0; i < this.customerClasses.size(); i++) {
            if (!this.customerClasses.get(i).isOpenClass()) {
                closedClassesCount++;
            }
        }
        this.maxIndexClass = closedClassesCount - 1;
        this.actualLevelInClass = new double[closedClassesCount];
        this.maximumLevelInClass = new double[closedClassesCount];
        double productForLogger = 1;
        closedClassesCount = 0;
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (!customerClass.isOpenClass()) {
                this.maximumLevelInClass[closedClassesCount] = customerClass.getPopulation() * loadfactor;
                productForLogger = productForLogger * (this.maximumLevelInClass[closedClassesCount] + 1);
                closedClassesCount++;
            }
        }
        double productForDistanceComputation = 1.0;
        this.distanceToPredecessorPopulationWith1CustomerLessInClass = new double[closedClassesCount];
        for (int c = this.maxIndexClass; c >= 0; c--) {
            this.distanceToPredecessorPopulationWith1CustomerLessInClass[c] = productForDistanceComputation;
            productForDistanceComputation = productForDistanceComputation * (this.maximumLevelInClass[c] + 1);
        }
        logger.debug("Loadfactor " + loadfactor + ": " + productForLogger + " populations has to be computed.");
        this.computePopulationsRecursivly(0);
    }

    /**
	 * Step 2.3 of the algorithm. The fillings are computed with formula 5.67
	 * @param population The actual population
	 */
    private void computeFillingPerNonLDDevice(final Population population) {
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double throughput = population.getThroughputPerClass().read(customerClass.getClassID());
                    final double responsetime = population.getResponsetimePerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                    final double filling = throughput * responsetime;
                    population.getFillingPerStationAndClass().write(station.getStationID(), customerClass.getClassID(), filling);
                }
            }
        }
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double filling = 0;
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (!customerClass.isOpenClass()) {
                    filling = filling + population.getFillingPerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                }
            }
            population.getFillingPerStation().write(station.getStationID(), filling);
        }
    }

    /**
	 * After the throughput the utilisations are recomputed for the next step.
	 * @param population The actual population
	 */
    private void computeNewUtilisationForClosedClasses(final Population population) {
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    final double throughput = population.getThroughputPerClass().read(customerClass.getClassID());
                    final double utilisation = throughput * serviceDemand;
                    this.tableUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), utilisation);
                }
            }
        }
    }

    /**
	 * Recursive method which computes all populations for the computation of
	 * the queueing network. It writes them in an array list.
	 * @param computationClass Actual Class for which the populations are
	 *            computed
	 */
    private void computePopulationsRecursivly(final int computationClass) {
        for (this.actualLevelInClass[computationClass] = 0; this.actualLevelInClass[computationClass] <= this.maximumLevelInClass[computationClass]; this.actualLevelInClass[computationClass]++) {
            if (computationClass < this.maxIndexClass) {
                this.computePopulationsRecursivly(computationClass + 1);
            } else {
                final int numberOfActualPopulation = this.allPopulations.size();
                final Population pop = new Population(numberOfActualPopulation);
                int closedClassesCount = 0;
                for (int c = 0; c < this.customerClasses.size(); c++) {
                    final Class customerClass = (Class) this.customerClasses.get(c);
                    if (!customerClass.isOpenClass()) {
                        pop.getNumberPerClass().write(customerClass.getClassID(), this.actualLevelInClass[closedClassesCount]);
                        pop.setCompleteNumberOfJobs(pop.getCompleteNumberOfJobs() + this.actualLevelInClass[closedClassesCount]);
                        if (this.actualLevelInClass[closedClassesCount] != 0) {
                            pop.getPredecessor().write(customerClass.getClassID(), numberOfActualPopulation - this.distanceToPredecessorPopulationWith1CustomerLessInClass[closedClassesCount]);
                        }
                        closedClassesCount++;
                    }
                }
                this.allPopulations.add(pop);
            }
        }
    }

    /**
	 * Step 2.4 of the algorithm. The queuelengths of the open classes are
	 * computed with formula 8.6 and 8.10.
	 * @param population The actual populatoin
	 */
    private void computeQueueLengthPerOpenClass(final Population population) {
        double r;
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    r = customerClass.getPriority();
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    if (station.isDelay()) {
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                    }
                    if (station.isLI()) {
                        double sum1 = 0;
                        double sum2 = 0;
                        for (int c2 = 0; c2 < this.customerClasses.size(); c2++) {
                            final Class customerClass2 = (Class) this.customerClasses.get(c2);
                            if (!customerClass2.isOpenClass()) {
                                if (customerClass2.getPriority() <= r) {
                                    sum1 = sum1 + serviceDemand * population.getFillingPerStationAndClass().read(station.getStationID(), customerClass2.getClassID());
                                }
                            }
                            if (customerClass2.getPriority() < r) {
                                sum2 = sum2 + this.tableUtilisationPerStationAndClass.read(station.getStationID(), customerClass2.getClassID());
                            }
                        }
                        final double responseTimePerStationAndClass = (serviceDemand + sum1) / (1 - sum2);
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), responseTimePerStationAndClass);
                        final double queuelengthPerStationAndClass = this.inputParameter.getArrivalrate(customerClass.getClassID()) * responseTimePerStationAndClass;
                        this.tableQueuelengthPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), queuelengthPerStationAndClass);
                    }
                }
            }
        }
    }

    /**
	 * Computes the queuelengths and responstimes of the open classes.
	 * @param loadfactor actual loadfactor for which the model is solved
	 */
    private void computeResponsetimeAndQueuelengthForOpenClasses(final double loadfactor) {
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double queuelength = 0.0;
            double responsetime = 0.0;
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (customerClass.isOpenClass()) {
                    final double responsetimePerStationAndClass = this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                    responsetime = responsetime + responsetimePerStationAndClass;
                    logger.debug("Responsetime of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + responsetimePerStationAndClass);
                    final double queuelengthPerStationAndClass = this.tableQueuelengthPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                    queuelength = queuelength + queuelengthPerStationAndClass;
                }
            }
            this.tableQueuelengthPerStationOpen.write(station.getStationID(), queuelength);
        }
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            double responsetime = 0.0;
            if (customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double responsetimePerStationAndClass = this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                    responsetime = responsetime + responsetimePerStationAndClass;
                }
            }
            this.tableMVAResponseTimePerClass.write(customerClass.getClassID(), responsetime);
        }
    }

    /**
	 * Computes the queuelengths and responstimes of the open classes.
	 * @param population The actual population
	 */
    private void computeResponsetimeForClosedClasses(final Population population) {
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            double responsetime = 0.0;
            if (!customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double responsetimePerStationAndClass = population.getResponsetimePerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                    responsetime = responsetime + responsetimePerStationAndClass;
                    logger.debug("Responsetime of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + responsetimePerStationAndClass);
                }
                logger.debug("Responsetime of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " is: " + responsetime);
                population.getResponsetimePerClass().write(customerClass.getClassID(), responsetime);
            }
        }
    }

    /**
	 * Step 2.1 of the algorithm. The responsetimes are computed with the
	 * formula 8.8
	 * @param population The actual poplation
	 */
    private void computeResponseTimePerStationAndClosedClass(final Population population) {
        double r;
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (!customerClass.isOpenClass()) {
                r = customerClass.getPriority();
                final double numberInClass = population.getNumberPerClass().read(customerClass.getClassID());
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    if (numberInClass > 0) {
                        final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                        if (station.isDelay()) {
                            population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                        }
                        if (station.isLI()) {
                            final int predecessor = (int) population.getPredecessor().read(customerClass.getClassID());
                            final Population populationWithOneJobOfClassLess = (Population) this.allPopulations.get(predecessor);
                            double sum1 = 0;
                            double sum2 = 0;
                            for (int c2 = 0; c2 < this.customerClasses.size(); c2++) {
                                final Class customerClass2 = (Class) this.customerClasses.get(c2);
                                if (!customerClass2.isOpenClass()) {
                                    if (customerClass2.getPriority() <= r) {
                                        sum1 = sum1 + serviceDemand * populationWithOneJobOfClassLess.getFillingPerStationAndClass().read(station.getStationID(), customerClass2.getClassID());
                                    }
                                }
                                if (customerClass2.getPriority() < r) {
                                    sum2 = sum2 + this.tableUtilisationPerStationAndClass.read(station.getStationID(), customerClass2.getClassID());
                                }
                            }
                            final double responseTimePerQueueAndClass = (serviceDemand + sum1) / (1 - sum2);
                            population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                        }
                    } else {
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), 0);
                    }
                }
            }
        }
    }

    /**
	 * Step 2.2 of the algorithm. Computes the throughpute with formula 5.66
	 * @param loadfactor Loadfactor
	 * @param population The actual population
	 */
    private void computeThroughputPerClosedClass(final double loadfactor, final Population population) {
        double sumOfAllResponsetimes = 0;
        double sumOfAllThinktimes = 0;
        if (this.inputParameter.getBc().getClassSection().isAreClassesCoupled()) {
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (!customerClass.isOpenClass()) {
                    sumOfAllResponsetimes = sumOfAllResponsetimes + population.getResponsetimePerClass().read(customerClass.getClassID());
                }
            }
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (!customerClass.isOpenClass()) {
                    sumOfAllThinktimes = sumOfAllThinktimes + customerClass.getThinktime();
                }
            }
        }
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (!customerClass.isOpenClass()) {
                final double numberOfUsers = customerClass.getPopulation() * loadfactor;
                if (this.inputParameter.getBc().getClassSection().isAreClassesCoupled()) {
                    final double throughputPerClass = numberOfUsers / (sumOfAllThinktimes + sumOfAllResponsetimes);
                    population.getThroughputPerClass().write(customerClass.getClassID(), throughputPerClass);
                    logger.debug("The coupled class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an throughput of: " + throughputPerClass);
                } else {
                    final double responseTimePerClass = population.getResponsetimePerClass().read(customerClass.getClassID());
                    final double throughputPerClass = numberOfUsers / (customerClass.getThinktime() + responseTimePerClass);
                    population.getThroughputPerClass().write(customerClass.getClassID(), throughputPerClass);
                    logger.debug("The uncoupled class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an throughput of: " + throughputPerClass);
                }
            }
        }
    }

    /**
	 * Computes the utilisation of the open queueing network. If the utilisation
	 * is greater or equal to 100% the algorithm returns false.
	 * @param loadfactor actual loadfactor for which the model is solved
	 * @return true if utilisation is less than 100% false if utilisation is
	 *         greater or equal to 100%
	 */
    private boolean computeUtilisationsForOpenClasses(final double loadfactor) {
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            double utilistationOfThisStation = 0.0;
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (customerClass.isOpenClass()) {
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    final double arrivalRate = loadfactor * this.inputParameter.getArrivalrate(customerClass.getClassID());
                    final double utilisationOfThisStationForThisClass = arrivalRate * serviceDemand;
                    if (utilisationOfThisStationForThisClass >= 1) {
                        return false;
                    } else {
                        logger.info("Utilisation of station " + station.getStationID() + " " + station.getStationName() + " for class " + customerClass.getClassID() + " " + customerClass.getClassName() + " = " + utilisationOfThisStationForThisClass);
                        this.tableUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), utilisationOfThisStationForThisClass);
                    }
                    utilistationOfThisStation = utilistationOfThisStation + utilisationOfThisStationForThisClass;
                } else {
                    this.tableUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), 0);
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
	 * So that the normal MVA methods can be used the results are copied into
	 * the MVA HashTables.
	 * @param p Actual population
	 */
    private void copyPopulationHashTablesIntoMVATables(final Population p) {
        final Population pop = p;
        this.tableMVAResponseTimePerStationAndClass = pop.getResponsetimePerStationAndClass();
        this.tableMVAThroughputPerClass = pop.getThroughputPerClass();
        this.tableMVAFillingPerStation = pop.getFillingPerStation();
        this.tableMVAResponseTimePerClass = pop.getResponsetimePerClass();
        this.tableMVAFillingPerStationAndClassN = pop.getFillingPerStationAndClass();
    }

    /**
	 * Generates all HashTables in the beginning for each loadfactor.
	 */
    private void generateAllHashTables() {
        this.tableUtilisationPerStationAndClass = new HashTable("AS TEMP Utilisation Per Station And Class");
        this.tableUtilisationPerStation = new HashTable("AS TEMP Utilisation Per Station");
        this.generateMVAHashTablesForNewIteration();
    }

    /**
	 * Generates all HashTables at the beginning of each iteration.
	 */
    private void generateMVAHashTablesForNewIteration() {
        this.tableQueuelengthPerStationOpen = new HashTable("AS TEMP Queuelength Per Station For Open Classes");
        this.tableQueuelengthPerStationAndClass = new HashTable("AS TEMP Queuelength Per Station And Open Class");
        this.tableMVAFillingPerStationAndClassN = new HashTable("AS TEMP MVA Filling Per Class (N)");
        this.tableMVAResponseTimePerStationAndClass = new HashTable("AS TEMP MVA Response Time Per Station And Class");
        this.tableMVAResponseTimePerClass = new HashTable("AS TEMP MVA Response Time Per Class");
        this.tableMVAThroughputPerClass = new HashTable("AS TEMP MVA Throughput Per Class");
        this.tableMVAFillingPerStation = new HashTable("AS TEMP MVA Filling");
        logger.debug("All HashTables for new iteration are generated");
    }

    /**
	 * Initiales the Database for the algorithm. Writes the model to be solved
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
	 * Initiales the Database for the algorithm. Writes the model to be solved
	 * into the database.
	 * @param loadfactor The actual loadfactor for which the results are written
	 *            into the database
	 */
    public void insertScenarioAndSolution(final double loadfactor) {
        double arrivalrate;
        double numberOfUsers;
        final long executionID = this.dbManager.insertScenarioDefinitions(this.solutionID, "Description");
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerclass = (Class) this.customerClasses.get(c);
            if (customerclass.isOpenClass()) {
                arrivalrate = loadfactor * this.inputParameter.getArrivalrate(customerclass.getClassID());
                this.dbManager.insertClassScenario(executionID, customerclass.getClassID(), this.scenarioID, arrivalrate, loadfactor);
            } else {
                numberOfUsers = loadfactor * customerclass.getPopulation();
                this.dbManager.insertClassScenario(executionID, customerclass.getClassID(), this.scenarioID, numberOfUsers, loadfactor);
            }
        }
        this.dbManager.insertAnalyticScenarioResults(executionID, this.completeThroughput);
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerclass = (Class) this.customerClasses.get(c);
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
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerclass = (Class) this.customerClasses.get(c);
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
	 * Sets the filling of the population 0 to 0 at the beginning of MVA.
	 */
    private void setInitialFillingOfZeroPopulation() {
        final Population population = (Population) this.allPopulations.get(0);
        for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
            for (int c = 0; c < this.customerClasses.size(); c++) {
                final Class customerClass = (Class) this.customerClasses.get(c);
                if (!customerClass.isOpenClass()) {
                    population.getFillingPerStationAndClass().write(station.getStationID(), customerClass.getClassID(), 0);
                }
            }
        }
    }

    /**
	 * Sets the new servicedemands which are changed because of the interference
	 * with the open classes.
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

    /**
	 * Sets the responsetime of the closed classes at the zero population to 0.
	 * Needed because we skip the computation of the zero population.
	 */
    private void setResponseTimeForOpenClassesForZeroPopulation() {
        final Population population = (Population) this.allPopulations.get(0);
        for (int c = 0; c < this.customerClasses.size(); c++) {
            final Class customerClass = (Class) this.customerClasses.get(c);
            if (customerClass.isOpenClass()) {
                for (int s = 0; s < this.inputParameter.getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.inputParameter.getStations().get(s);
                    final double serviceDemand = this.inputParameter.getServiceDemand(station.getStationID(), customerClass.getClassID());
                    if (station.isDelay()) {
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                    }
                    if (station.isLI()) {
                        final double numerator = this.inputParameter.getArrivalrate(customerClass.getClassID()) * serviceDemand;
                        final double denumerator = 1 - this.tableUtilisationPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                        final double responseTimePerQueueAndClass = numerator / denumerator;
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                    }
                }
            }
        }
    }

    /**
	 * This funktions sorts the customerclasses by there priority. Needed
	 * because the algorithm computes the model with the beginning of the
	 * highest priority.
	 */
    private void sortClasses() {
        final HashTable alreadySortedClasses = new HashTable("SortedClasses");
        for (int c = 0; c < this.inputParameter.getCustomerClasses().size(); c++) {
            double highestPriority = 100000;
            Class actualClassWithHighestPriority = null;
            for (int c2 = 0; c2 < this.inputParameter.getCustomerClasses().size(); c2++) {
                final Class customerClass2 = (Class) this.inputParameter.getCustomerClasses().get(c2);
                if (alreadySortedClasses.read(customerClass2.getClassID()) == 0) {
                    if (customerClass2.getPriority() < highestPriority) {
                        highestPriority = customerClass2.getPriority();
                        actualClassWithHighestPriority = customerClass2;
                    }
                }
            }
            this.customerClasses.add(actualClassWithHighestPriority);
            alreadySortedClasses.write(actualClassWithHighestPriority.getClassID(), highestPriority);
        }
    }
}
