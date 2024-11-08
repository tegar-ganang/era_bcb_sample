package de.ibis.permoto.solver.as.tech;

import java.sql.Timestamp;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.ibis.permoto.gui.solver.panels.AnalyticSolverPanel;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.solver.as.util.AlgorithmInput;
import de.ibis.permoto.solver.as.util.AnalyticQNStation;
import de.ibis.permoto.solver.as.util.Population;
import de.ibis.permoto.util.db.DBManager;

/**
 * Computes a closed queueing network with multiple classes and load dependent
 * stations. The algorithm is analogous to Menasce (Performance by design) at
 * page 389 and Bolch (Leistungsbewertung von Rechensystemen) at page 112. The
 * idea behind this class is that first all populations for each load factor is
 * calculated and written in a arraylist. For the computation this array list is
 * then used. Each population is an object of the same name. There the
 * provisional results are saved in Hash tables. After the last iteration the
 * results are saved in the MVA Hash tables and then the normal formulas are
 * used to compute the searched magnitudes.
 * @author Christian Markl
 * @author Oliver Huehn
 */
public class ClosedMcExact extends MVA implements QNAlgorithm {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(ClosedMcExact.class);

    /**
	 * In this array the current population is saved. This is done by saving the
	 * number of customers at each station at one time.
	 */
    private double[] actualLevelInClass;

    /** Array list with all possible populations. */
    private ArrayList<Population> allPopulations;

    /** The manager for the interaction with the db. */
    private DBManager dbManager = DBManager.getInstance();

    /**
	 * In this array it is saved how much different populations are possible for
	 * one customer less in the network.
	 */
    private double[] distanceToPredecessorPopulationWith1CustomerLessInClass;

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

    /**
	 * The constructor of the class.
	 * @param input The inputparameters for the algorithm
	 */
    public ClosedMcExact(final AlgorithmInput input) {
        super(input);
        startTime = new Timestamp(System.currentTimeMillis());
        this.scenarioID = this.initialiseDBForClosedAlgorithms(input, this.dbManager);
    }

    /**
	 * Controls the computation of the queueing network. For each loadfactor one
	 * result is computed.
	 * @return int analysisID to initialize resultGUI
	 */
    public final int compute(final boolean fromWebService) {
        for (int i = 0; i < this.getInputParameters().getLoadfactors().length; i++) {
            logger.info("Calculate load factor " + this.getInputParameters().getLoadfactors()[i]);
            AnalyticSolverPanel.whatIfNextStepStatusChange();
            this.computeAllPossiblePopulations(this.getInputParameters().getLoadfactors()[i]);
            this.setInitialProbabilitiesForLDDevicesOfZeroPopulation();
            this.setInitialFillingOfZeroPopulation();
            for (int a = 1; a < this.allPopulations.size(); a++) {
                final Population pop = (Population) this.allPopulations.get(a);
                this.computeResponseTimePerStationAndClass(pop);
                this.computeThroughputPerClass(pop);
                this.computeProbabilitiesAndFillingsForLDDevices(pop);
                this.computeFillingPerNonLDDevice(pop);
            }
            final Population pop = (Population) this.allPopulations.get(this.allPopulations.size() - 1);
            this.generateAllMVAHashTables();
            this.copyPopulationHashTablesIntoMVATables(pop);
            this.computeResponsetimePerClass();
            this.computeUtilityOfTheStationsPerClass();
            final boolean loadfactorIsAllowed = this.computeCompleteUtilityOfEachStation();
            if (loadfactorIsAllowed) {
                this.computeCompleteThroughput();
                final int station = this.isQueueLengthOfOneStationGreaterThanAllowed();
                if (station > -1) {
                    logger.info("The station " + ((AnalyticQNStation) this.getInputParameters().getStations().get(station)).getStationID() + " " + ((AnalyticQNStation) this.getInputParameters().getStations().get(station)).getStationName() + " has got a greater queue length than the allowed one! The computation is aborted.");
                    if (!fromWebService) {
                        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "The station " + ((AnalyticQNStation) this.getInputParameters().getStations().get(station)).getStationID() + " " + ((AnalyticQNStation) this.getInputParameters().getStations().get(station)).getStationName() + " has got a greater queue length than the allowed one! The computation is aborted.", "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    if (i == 0) {
                        this.dbManager.deleteScenarioDescription(this.scenarioID);
                        return -1;
                    } else {
                        String newWhatIFParameters = "MaxLoadFactor: " + this.getInputParameters().getLoadfactors()[i - 1] + " NrOfSteps: " + i;
                        this.dbManager.updateWhatIFParameters(this.solutionID, newWhatIFParameters);
                        break;
                    }
                } else {
                    if (i == 0) {
                        this.solutionID = this.dbManager.insertScenarioAnalysis(this.scenarioID, "ClosedMcExact", "Description", this.getInputParameters().getWhatIFParameters(), startTime);
                    }
                    logger.debug("SolutionID in Derby DBMS is " + this.solutionID);
                    this.addSubserviceCallMetricsToParent(this.getInputParameters());
                    this.removeAddedStationsOfClosingAlgorithm(this.getInputParameters());
                    this.insertScenarioAndSolutionForClosedAlgorithms(this.solutionID, this.scenarioID, this.getInputParameters(), this.dbManager, this.getInputParameters().getLoadfactors()[i]);
                }
            } else {
                if (i == 0) {
                    logger.info("Stations utilisation is at 100% for the model!");
                    if (!fromWebService) {
                        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Stations utilisation is at 100% for the model!", "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    i++;
                    while (i < this.getInputParameters().getLoadfactors().length) {
                        AnalyticSolverPanel.whatIfNextStepStatusChange();
                        i++;
                    }
                    this.dbManager.deleteScenarioDescription(this.scenarioID);
                    return -1;
                } else {
                    logger.info("Stations utilisation is at 100%! The computation is aborted..." + " Last complete computed loadfactor was " + this.getInputParameters().getLoadfactors()[i - 1]);
                    if (!fromWebService) {
                        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Stations utilisation is at 100%! The computation is aborted..." + " Last complete computed loadfactor was " + this.getInputParameters().getLoadfactors()[i - 1], "Information", JOptionPane.INFORMATION_MESSAGE);
                    }
                    String newWhatIFParameters = "MaxLoadFactor: " + this.getInputParameters().getLoadfactors()[i - 1] + " NrOfSteps: " + i;
                    this.dbManager.updateWhatIFParameters(this.solutionID, newWhatIFParameters);
                    i++;
                    while (i < this.getInputParameters().getLoadfactors().length) {
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
        this.maxIndexClass = this.getInputParameters().getCustomerClasses().size() - 1;
        this.actualLevelInClass = new double[this.getInputParameters().getCustomerClasses().size()];
        this.maximumLevelInClass = new double[this.getInputParameters().getCustomerClasses().size()];
        double productForLogger = 1;
        for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
            this.maximumLevelInClass[c] = customerClass.getPopulation() * loadfactor;
            productForLogger = productForLogger * (this.maximumLevelInClass[c] + 1);
        }
        double productForDistanceComputation = 1.0;
        this.distanceToPredecessorPopulationWith1CustomerLessInClass = new double[this.getInputParameters().getCustomerClasses().size()];
        for (int c = this.maxIndexClass; c >= 0; c--) {
            this.distanceToPredecessorPopulationWith1CustomerLessInClass[c] = productForDistanceComputation;
            productForDistanceComputation = productForDistanceComputation * (this.maximumLevelInClass[c] + 1);
        }
        logger.debug("Loadfactor " + loadfactor + ": " + productForLogger + " populations has to be computed.");
        this.computePopulationsRecursivly(0);
    }

    /**
	 * Computes the filling of each Non-LD Station for the actual population.
	 * Formula: Filling = Throughput * ResponsetimePerStationAndClass Output:
	 * pop.fillingPerStationAndClass (just result) pop.fillingPerStation (is
	 * used later)
	 * @param p Actual Population.
	 */
    private void computeFillingPerNonLDDevice(final Population p) {
        final Population pop = p;
        for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
            if (!station.isLD()) {
                double sumOfFilling = 0;
                for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                    final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                    if (pop.getNumberPerClass().read(customerClass.getClassID()) > 0) {
                        final double throughput = pop.getThroughputPerClass().read(customerClass.getClassID());
                        final double responsetimePerStationAndClass = pop.getResponsetimePerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                        final double filling = throughput * responsetimePerStationAndClass;
                        pop.getFillingPerStationAndClass().write(station.getStationID(), customerClass.getClassID(), filling);
                        sumOfFilling = sumOfFilling + filling;
                    }
                }
                pop.getFillingPerStation().write(station.getStationID(), sumOfFilling);
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
                for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                    final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                    pop.getNumberPerClass().write(customerClass.getClassID(), this.actualLevelInClass[c]);
                    pop.setCompleteNumberOfJobs(pop.getCompleteNumberOfJobs() + this.actualLevelInClass[c]);
                    if (this.actualLevelInClass[c] != 0) {
                        pop.getPredecessor().write(customerClass.getClassID(), numberOfActualPopulation - this.distanceToPredecessorPopulationWith1CustomerLessInClass[c]);
                    }
                }
                this.allPopulations.add(pop);
            }
        }
    }

    /**
	 * Computes the probabilites for LD Devices.
	 * @param p Actual population
	 */
    private void computeProbabilitiesAndFillingsForLDDevices(final Population p) {
        final Population pop = p;
        for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
            if (station.isLD()) {
                double fillingOfTheStation = 0;
                double sumOverAllJ = 0;
                for (int j = 1; j <= pop.getCompleteNumberOfJobs(); j++) {
                    final double alphaIOfJ = station.getServiceRateMultiplier(j);
                    double sumOfTerm = 0;
                    for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                        final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                        final double numberInClass = pop.getNumberPerClass().read(customerClass.getClassID());
                        if (numberInClass > 0) {
                            final double throughput = pop.getThroughputPerClass().read(customerClass.getClassID());
                            final double serviceDemand = this.getInputParameters().getServiceDemand(station.getStationID(), customerClass.getClassID());
                            final int predecessor = (int) pop.getPredecessor().read(customerClass.getClassID());
                            final Population populationWithOneJobOfClassLess = (Population) this.allPopulations.get(predecessor);
                            final double probabilityOfJMinusAtPredecessorPopulation = populationWithOneJobOfClassLess.getProbabilityPerStation().read(station.getStationID(), j - 1);
                            sumOfTerm = sumOfTerm + ((throughput * serviceDemand) / alphaIOfJ) * probabilityOfJMinusAtPredecessorPopulation;
                            pop.getFillingPerStationAndClass().write(station.getStationID(), customerClass.getClassID(), pop.getFillingPerStationAndClass().read(station.getStationID(), customerClass.getClassID()) + j * sumOfTerm);
                        }
                    }
                    pop.getProbabilityPerStation().write(station.getStationID(), j, sumOfTerm);
                    fillingOfTheStation = fillingOfTheStation + (j * sumOfTerm);
                    sumOverAllJ = sumOverAllJ + sumOfTerm;
                }
                pop.getProbabilityPerStation().write(station.getStationID(), 0, 1 - sumOverAllJ);
                pop.getFillingPerStation().write(station.getStationID(), fillingOfTheStation);
            }
        }
    }

    /**
	 * Mean response time per station and class. Delay Station: Service demand
	 * LI Station: Service demand * (1 + number of jobs in the class before this
	 * job) LD Station: Service demand * probability.
	 * @param population Actual population
	 */
    public final void computeResponseTimePerStationAndClass(final Population population) {
        for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
            final double numberInClass = population.getNumberPerClass().read(customerClass.getClassID());
            for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
                if (numberInClass > 0) {
                    final double serviceDemand = this.getInputParameters().getServiceDemand(station.getStationID(), customerClass.getClassID());
                    if (station.isDelay()) {
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                    }
                    if (station.isLI()) {
                        final int predecessor = (int) population.getPredecessor().read(customerClass.getClassID());
                        final Population populationWithOneJobOfClassLess = (Population) this.allPopulations.get(predecessor);
                        final double responseTimePerQueueAndClass = serviceDemand * (1 + populationWithOneJobOfClassLess.getFillingPerStation().read(station.getStationID()));
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                    }
                    if (station.isLD()) {
                        final int predecessor = (int) population.getPredecessor().read(customerClass.getClassID());
                        final Population populationWithOneJobOfClassLess = (Population) this.allPopulations.get(predecessor);
                        double sumTerm = 0;
                        for (int j = 1; j <= population.getCompleteNumberOfJobs(); j++) {
                            final double probabilityAtStationForJMinus1 = populationWithOneJobOfClassLess.getProbabilityPerStation().read(station.getStationID(), j - 1);
                            final double alphaOfJ = station.getServiceRateMultiplier(j);
                            final double loopValue = (j / alphaOfJ) * probabilityAtStationForJMinus1;
                            sumTerm = sumTerm + loopValue;
                        }
                        final double responseTimePerQueueAndClass = serviceDemand * sumTerm;
                        population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                    }
                } else {
                    population.getResponsetimePerStationAndClass().write(station.getStationID(), customerClass.getClassID(), 0);
                }
            }
        }
    }

    /**
	 * Computes the throughput of each class for the actual population.
	 * Formulas: Classes coupled: throughputPerClass = numberInClass /
	 * (sumOfAllThinktimes + sumOfAllResponsetimes) else: throughputPerClass =
	 * numberInClass / (customerClass.getThinkTime() +
	 * summeResponseTimePerClass) Output: pop.throughputPerClass
	 * @param p Actual population
	 */
    private void computeThroughputPerClass(final Population p) {
        final Population pop = p;
        double sumOfAllResponsetimes = 0;
        double sumOfAllThinktimes = 0;
        if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
            for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
                    final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
                    sumOfAllResponsetimes = sumOfAllResponsetimes + pop.getResponsetimePerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                }
            }
            for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                sumOfAllThinktimes = sumOfAllThinktimes + customerClass.getThinktime();
            }
        }
        for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
            final double numberPerClass = pop.getNumberPerClass().read(customerClass.getClassID());
            if (numberPerClass > 0) {
                if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
                    final double throughputPerClass = numberPerClass / (sumOfAllThinktimes + sumOfAllResponsetimes);
                    pop.getThroughputPerClass().write(customerClass.getClassID(), throughputPerClass);
                } else {
                    double sumResponseTimePerClass = 0;
                    for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
                        final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
                        final double responseTimePerQueueAndClass = pop.getResponsetimePerStationAndClass().read(station.getStationID(), customerClass.getClassID());
                        sumResponseTimePerClass = sumResponseTimePerClass + responseTimePerQueueAndClass;
                    }
                    final double throughputPerClass = numberPerClass / (customerClass.getThinktime() + sumResponseTimePerClass);
                    pop.getThroughputPerClass().write(customerClass.getClassID(), throughputPerClass);
                }
            }
        }
    }

    /**
	 * So that the normal MVA methods can be used the results are copied into
	 * the MVA HashTables.
	 * @param p Actual population
	 */
    private void copyPopulationHashTablesIntoMVATables(final Population p) {
        final Population pop = p;
        this.setTableMVAResponseTimePerStationAndClass(pop.getResponsetimePerStationAndClass());
        this.setTableMVAThroughputPerClass(pop.getThroughputPerClass());
        this.setTableMVAProbabilitiesLdDevices(pop.getProbabilityPerStation());
        this.setTableMVAFillingPerStation(pop.getFillingPerStation());
        this.setTableMVAFillingPerStationAndClassN(pop.getFillingPerStationAndClass());
    }

    /**
	 * Displays the population for debugging purpose.
	 */
    public final void displayPopulations() {
        for (int i = 0; i < this.allPopulations.size(); i++) {
            final Population population = (Population) this.allPopulations.get(i);
            String displayString = i + " Number in classes: ";
            for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                final int value = (int) population.getNumberPerClass().read(customerClass.getClassID());
                displayString = displayString + value + " ";
            }
            logger.debug(displayString);
            displayString = i + " Predecessor        : ";
            for (int c = 0; c < this.getInputParameters().getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.getInputParameters().getCustomerClasses().get(c);
                if (population.getNumberPerClass().read(customerClass.getClassID()) > 0) {
                    final int value = (int) population.getPredecessor().read(customerClass.getClassID());
                    displayString = displayString + value + " ";
                }
            }
            logger.debug(displayString);
            logger.debug("---------------------------------------");
        }
    }

    /**
	 * Sets the filling of the population 0 to 0 at the beginning of MVA.
	 */
    private void setInitialFillingOfZeroPopulation() {
        final Population population = (Population) this.allPopulations.get(0);
        for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
            population.getFillingPerStation().write(station.getStationID(), 0);
        }
    }

    /**
	 * Sets the probability of the population 0 to 1 at the beginning of MVA.
	 */
    private void setInitialProbabilitiesForLDDevicesOfZeroPopulation() {
        final Population population = (Population) this.allPopulations.get(0);
        for (int s = 0; s < this.getInputParameters().getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.getInputParameters().getStations().get(s);
            population.getProbabilityPerStation().write(station.getStationID(), 0, 1);
        }
    }
}
