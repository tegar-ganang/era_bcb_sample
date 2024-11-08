package de.ibis.permoto.solver.as.tech;

import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import de.ibis.permoto.model.basic.scenario.Class;
import de.ibis.permoto.model.basic.scenario.ClassPart;
import de.ibis.permoto.model.definitions.impl.PerMoToBusinessCase;
import de.ibis.permoto.solver.as.util.AlgorithmInput;
import de.ibis.permoto.solver.as.util.AnalyticQNStation;
import de.ibis.permoto.solver.as.util.HashTable;
import de.ibis.permoto.util.db.ClassResultBean;
import de.ibis.permoto.util.db.ClassStationResultBean;
import de.ibis.permoto.util.db.DBManager;

/**
 * Basic functions for the analytical solution of a closed queueing network with
 * multiple customer classes. have a look at Menasc� et alt. - Performance by
 * Design, chapter 12.3 "The MVA Algorithm", page 323
 * @author Christian Markl
 * @author Oliver Huehn
 */
public class WhatIfMVA {

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(WhatIfMVA.class);

    /** The complete throughput over all stations. */
    private double completeThroughput;

    /** The input parameters for the MVA. */
    private AlgorithmInput inputparameters;

    /** The HashTable for the estimator. */
    private HashTable tableMVAEstimatorPerStationAndClass;

    /** The HashTable for the filling. */
    private HashTable tableMVAFillingPerStation;

    /** The HashTable for the filling per class. */
    private HashTable tableMVAFillingPerStationAndClassN;

    /** The HashTable for the filling per class with one customer less. */
    private HashTable tableMVAFillingPerStationAndClassNminus1;

    /** The HashTable for the probability of LD devices. */
    private HashTable tableMVAProbabilitiesLdDevices;

    /** The HashTable for the probability of LD devices with on customer less. */
    private HashTable tableMVAProbabilitiesLdDevicesNminus1;

    /** The HashTable for the queue length. */
    private HashTable tableMVAQueueLengthPerStation;

    /** The HashTable for the queue length per class. */
    private HashTable tableMVAQueueLengthPerStationAndClass;

    /** The HashTable for the response time per class. */
    private HashTable tableMVAResponseTimePerClass;

    /** The HashTable for the response time per station and class. */
    private HashTable tableMVAResponseTimePerStationAndClass;

    /** The HashTable for the throughput per class. */
    private HashTable tableMVAThroughputPerClass;

    /** The HashTable for the estimator of the throughput per class. */
    private HashTable tableMVAThroughputPerClassEstimator;

    /** The HashTable for the utilization. */
    private HashTable tableMVAUtilisationPerStation;

    /** The HashTable for the utilization per class. */
    private HashTable tableMVAUtilisationPerStationAndClass;

    /**
	 * Constructor of the MVA class.
	 * @param i The input parameters for this basic MVA algorithm
	 */
    public WhatIfMVA(final AlgorithmInput i) {
        this.inputparameters = i;
    }

    /**
	 * Adds the responsetimes of subservice calls to the responsetimes of the
	 * corresponding parent.
	 * @param input The AlgorithmInput including the table with the
	 *            relationships of subservice calls to the parents
	 */
    public final void addSubserviceCallMetricsToParent(final AlgorithmInput input) {
        for (Class currClass : input.getCustomerClasses()) {
            for (int i = input.getMaxSubserviceLevel(); i >= 0; i--) {
                for (int j = input.getSubserviceParentRelationships().size() - 1; j >= 0; j--) {
                    List<Object> entries = input.getSubserviceParentRelationships().get(j);
                    if (entries.get(0).equals(currClass.getClassID()) && ((Integer) entries.get(3)).intValue() == i) {
                        this.tableMVAResponseTimePerStationAndClass.write((String) entries.get(1), currClass.getClassID(), this.tableMVAResponseTimePerStationAndClass.read((String) entries.get(1), currClass.getClassID()) + this.tableMVAResponseTimePerStationAndClass.read((String) entries.get(2), currClass.getClassID()) / input.getVisitsMap().read((String) entries.get(2), currClass.getClassID()));
                    }
                }
            }
        }
    }

    public final void visualizeAggregatedResponseTimes() {
        for (Class currClass : this.getInputParameters().getCustomerClasses()) {
            for (int j = this.getInputParameters().getSubserviceParentRelationships().size() - 1; j >= 0; j--) {
                List<Object> entries = this.getInputParameters().getSubserviceParentRelationships().get(j);
                if (entries.get(0).equals(currClass.getClassID()) && ((Integer) entries.get(3)).intValue() == 1) {
                    logger.error(";" + "Response Time " + currClass.getClassID() + ";" + PerMoToBusinessCase.getInstance().getStationSection().getStationName((String) entries.get(1)) + ";" + this.tableMVAResponseTimePerStationAndClass.read((String) entries.get(1), currClass.getClassID()));
                }
            }
        }
    }

    public final void visualizeDelayItems() {
        for (Class currClass : this.getInputParameters().getCustomerClasses()) {
            for (AnalyticQNStation currStation : this.getInputParameters().getStations()) {
                if (currStation.getStationName().contains("Delay") && this.tableMVAResponseTimePerStationAndClass.read(currStation.getStationID(), currClass.getClassID()) != 0) {
                    logger.debug("Responsetime in class " + currClass.getClassID() + " for Station " + currStation.getStationName() + " = " + this.tableMVAResponseTimePerStationAndClass.read(currStation.getStationID(), currClass.getClassID()));
                }
            }
        }
    }

    /**
	 * Adopts the estimators for the number of customer at each station into
	 * tableMVAFillingPerStationAndClassN.
	 */
    public final void adoptFillingPerNonLDStationAndClassForNEstimator() {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                if (!station.isLD()) {
                    final double value = this.tableMVAEstimatorPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                    this.tableMVAFillingPerStationAndClassN.write(station.getStationID(), customerClass.getClassID(), value);
                    logger.debug("The estimator for the filling for class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + value);
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
    public final void approximateFillingPerNonLDStationAndClassNMinus1(final double loadfactor) {
        for (int c1 = 0; c1 < this.inputparameters.getCustomerClasses().size(); c1++) {
            final Class customerClass1 = (Class) this.inputparameters.getCustomerClasses().get(c1);
            final double nR = customerClass1.getPopulation();
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                if (!station.isLD()) {
                    final double nIrOfN = this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerClass1.getClassID());
                    for (int t = 0; t < this.inputparameters.getCustomerClasses().size(); t++) {
                        final Class customerClassT = (Class) this.inputparameters.getCustomerClasses().get(t);
                        if (customerClassT.getClassID().equals(customerClass1.getClassID())) {
                            final double nIrOfNMinus1 = ((nR - 1) / nR) * nIrOfN;
                            this.tableMVAFillingPerStationAndClassNminus1.write(station.getStationID(), customerClass1.getClassID(), customerClassT.getClassID(), nIrOfNMinus1);
                            logger.debug("The approximated filling of class " + customerClass1.getClassID() + " " + customerClass1.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " when seeing class " + customerClassT.getClassID() + " " + customerClassT.getClassName() + " is: " + nIrOfNMinus1);
                        } else {
                            final double nIrOfNMinus1 = nIrOfN;
                            this.tableMVAFillingPerStationAndClassNminus1.write(station.getStationID(), customerClass1.getClassID(), customerClassT.getClassID(), nIrOfNMinus1);
                            logger.debug("The approximated filling of class " + customerClass1.getClassID() + " " + customerClass1.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " when seeing class " + customerClassT.getClassName() + " " + customerClassT.getClassName() + " is: " + nIrOfNMinus1);
                        }
                    }
                }
            }
        }
    }

    /**
	 * After the MVA the complete throughput is computed. This is done by
	 * summing all values of the throughput per class.
	 */
    public final void computeCompleteThroughput() {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            this.completeThroughput = this.completeThroughput + this.tableMVAThroughputPerClass.read(customerClass.getClassID());
        }
        logger.debug("The complete throughput is " + this.completeThroughput);
    }

    /**
	 * After the MVA the complete utility of each station is computed. This is
	 * done by summing all values of the utility per class.
	 * @return true if the utility at each station is less than 1; else false
	 */
    public final boolean computeCompleteUtilityOfEachStation() {
        for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
            double sumOfUtilityOfStationAtAllClasses = 0;
            for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                sumOfUtilityOfStationAtAllClasses = sumOfUtilityOfStationAtAllClasses + this.tableMVAUtilisationPerStationAndClass.read(station.getStationID(), customerClass.getClassID());
            }
            if (sumOfUtilityOfStationAtAllClasses < 1) {
                this.tableMVAUtilisationPerStation.write(station.getStationID(), sumOfUtilityOfStationAtAllClasses);
                logger.debug("The station " + station.getStationID() + " " + station.getStationName() + " has got an utility of: " + sumOfUtilityOfStationAtAllClasses);
            } else {
                logger.debug("The station " + station.getStationID() + " " + station.getStationName() + " has got an utility over 100% with its utilisation of " + sumOfUtilityOfStationAtAllClasses);
                if (!station.getStationID().startsWith("station")) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * After the MVA the queue length at each station is computed. This is done
	 * by summing all lengths per class.
	 */
    public final void computeFillingPerNonLDStation() {
        for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
            if (!station.isLD()) {
                double filling = 0;
                for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                    final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                    filling = filling + this.tableMVAFillingPerStationAndClassN.read(station.getStationID(), customerClass.getClassID());
                }
                this.tableMVAFillingPerStation.write(station.getStationID(), filling);
                logger.debug("The station " + station.getStationID() + " " + station.getStationName() + " has got a filling of: " + filling);
            }
        }
    }

    /**
	 * Computes the value of N (= number of users at all classes).
	 * @param loadfactor Loadfactor
	 * @return Die Anzahl der Kunden.
	 */
    public final double computeNumberOfCustomersInAllClassesForActivLoadfactor(final double loadfactor) {
        double returnValue = 0;
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            returnValue = returnValue + (customerClass.getPopulation());
        }
        return returnValue;
    }

    /**
	 * Response time per class is computed.
	 */
    public final void computeResponsetimePerClass() {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            double sum = 0;
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                sum = sum + this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
            }
            this.tableMVAResponseTimePerClass.write(customerClass.getClassID(), sum);
            logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " has got an response time of: " + sum);
        }
    }

    /**
	 * Mean response time per queue and class. Delay Queue: Service demand LI
	 * Queue: Service demand * (1 + Jobs of this class before) LD Queue: Service
	 * demand * Probabilty.
	 * @param loadfactor Loadfactor
	 */
    public final void computeResponsetimePerStationAndClass(final double loadfactor) {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                double serviceDemand = 0.0;
                if (station.getStationID().startsWith("station")) {
                    serviceDemand = this.inputparameters.getServiceDemand(station.getStationID(), customerClass.getClassID()) / loadfactor;
                } else {
                    serviceDemand = this.inputparameters.getServiceDemand(station.getStationID(), customerClass.getClassID());
                }
                if (station.isDelay()) {
                    this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), serviceDemand);
                } else if (station.isLI()) {
                    double sumterm = 0;
                    for (int t = 0; t < this.inputparameters.getCustomerClasses().size(); t++) {
                        final Class customerClassT = (Class) this.inputparameters.getCustomerClasses().get(t);
                        sumterm = sumterm + this.tableMVAFillingPerStationAndClassNminus1.read(station.getStationID(), customerClassT.getClassID(), customerClass.getClassID());
                    }
                    final double responseTimePerQueueAndClass = serviceDemand * (1 + sumterm);
                    this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                    logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " has got a response time of: " + responseTimePerQueueAndClass);
                } else if (station.isLD()) {
                    final double normOfN = this.computeNumberOfCustomersInAllClassesForActivLoadfactor(loadfactor);
                    double sumterm = 0;
                    for (int j = 1; j <= normOfN; j++) {
                        final double probabilityAtStationForJMinus1 = this.tableMVAProbabilitiesLdDevices.read(station.getStationID(), j - 1);
                        final double alphaOfJ = station.getServiceRateMultiplier(j);
                        final double loopvalue = (j / alphaOfJ) * probabilityAtStationForJMinus1;
                        sumterm = sumterm + loopvalue;
                    }
                    final double responseTimePerQueueAndClass = serviceDemand * sumterm;
                    this.tableMVAResponseTimePerStationAndClass.write(station.getStationID(), customerClass.getClassID(), responseTimePerQueueAndClass);
                    logger.debug("The class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " has got a response time of: " + responseTimePerQueueAndClass);
                }
            }
        }
    }

    /**
	 * Throughput per class. = number of customers in class / needed time per
	 * class.
	 * @param loadfactor Loadfactor
	 */
    public final void computeThroughputPerClass(final double loadfactor) {
        double sumOfAllResponsetimes = 0;
        double sumOfAllThinktimes = 0;
        if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
            for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                sumOfAllResponsetimes = sumOfAllResponsetimes + this.tableMVAResponseTimePerClass.read(customerClass.getClassID());
            }
            for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                sumOfAllThinktimes = sumOfAllThinktimes + customerClass.getThinktime();
            }
        }
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            final double numberOfUsers = (customerClass.getPopulation());
            if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
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

    /**
	 * Throughput per class for the SCAT algorithm. = number of customers in
	 * class / needed time per class.
	 * @param loadfactor Loadfactor
	 */
    public final void computeThroughputPerClassSCAT(final double loadfactor, final int usersLess) {
        double sumOfAllResponsetimes = 0;
        double sumOfAllThinktimes = 0;
        if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
            for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                sumOfAllResponsetimes = sumOfAllResponsetimes + this.tableMVAResponseTimePerClass.read(customerClass.getClassID());
            }
            for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
                final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
                sumOfAllThinktimes = sumOfAllThinktimes + customerClass.getThinktime();
            }
        }
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            final double numberOfUsers = customerClass.getPopulation() - usersLess;
            if (this.getInputParameters().getBc().getClassSection().isAreClassesCoupled()) {
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

    /**
	 * Utility at each station is computed. = Throughput per class * service
	 * demand
	 */
    public final void computeUtilityOfTheStationsPerClass(double loadfactor) {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            final double throughputPerClass = this.tableMVAThroughputPerClass.read(customerClass.getClassID());
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                double serviceDemand = 0.0;
                if (station.getStationID().startsWith("station")) {
                    serviceDemand = this.inputparameters.getServiceDemand(station.getStationID(), customerClass.getClassID()) / loadfactor;
                } else {
                    serviceDemand = this.inputparameters.getServiceDemand(station.getStationID(), customerClass.getClassID());
                }
                double utilityOfStationPerClass = serviceDemand * throughputPerClass;
                if (station.isLD()) {
                    utilityOfStationPerClass = utilityOfStationPerClass / station.getNrParallelWorkers();
                } else if (station.isDelay()) {
                    utilityOfStationPerClass = 0;
                }
                this.tableMVAUtilisationPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), utilityOfStationPerClass);
            }
        }
    }

    /**
	 * For ./M/1 stations this new estimators are computed at the end of each
	 * MVA iteration. They are written in tableMVAEstimatorPerStationAndClass so
	 * that at the next step the fault tolerance can be checked. If afterwords
	 * another iteration follows these values are copied into
	 * tableMVAFillingPerStationAndClassN.
	 */
    public final void estimateFillingPerNonLDStationAndClassForN() {
        this.generateHashTableFillingEstimator();
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            final double throughputPerClass = this.tableMVAThroughputPerClass.read(customerClass.getClassID());
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                if (!station.isLD()) {
                    final double responseTimePerStationAndClass = this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerClass.getClassID());
                    final double newEstimator = throughputPerClass * responseTimePerStationAndClass;
                    this.tableMVAEstimatorPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), newEstimator);
                    logger.debug("After this iteration the filling estimator of class " + customerClass.getClassID() + " " + customerClass.getClassName() + " at station " + station.getStationID() + " " + station.getStationName() + " is: " + newEstimator);
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
	 * @param loadfactor double value of the load factor to be calculated
	 */
    public final void estimateInitialFillingPerStationAndClassForN(final double loadfactor) {
        for (int c = 0; c < this.inputparameters.getCustomerClasses().size(); c++) {
            final Class customerClass = (Class) this.inputparameters.getCustomerClasses().get(c);
            final int kR = this.inputparameters.numberOfStationsWithServiceDemandGreaterZeroForClass(customerClass.getClassID());
            final double nR = customerClass.getPopulation();
            final double estimator = nR / kR;
            for (int q = 0; q < this.inputparameters.getStations().size(); q++) {
                final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(q);
                this.tableMVAEstimatorPerStationAndClass.write(station.getStationID(), customerClass.getClassID(), estimator);
            }
        }
    }

    /**
	 * Initializes all needed HashTables for each single load factor to be
	 * calculated.
	 */
    public final void generateAllMVAHashTables() {
        this.generateHashTablesForNewIteration();
        this.generateHashTableThroughputEstimator();
        this.generateHashTableFillingEstimator();
    }

    /**
	 * Initializes the HashTable for the estimator of the queue length.
	 */
    public final void generateHashTableFillingEstimator() {
        this.tableMVAEstimatorPerStationAndClass = new HashTable("AS TEMP MVA Estimator");
    }

    /**
	 * Initializes needed HashTables at the beginning of each new iteration.
	 */
    public final void generateHashTablesForNewIteration() {
        this.completeThroughput = 0;
        this.tableMVAFillingPerStationAndClassN = new HashTable("AS TEMP MVA Filling Per Class (N)");
        this.tableMVAFillingPerStationAndClassNminus1 = new HashTable("AS TEMP MVA Filling Per Class (N-1)");
        this.tableMVAResponseTimePerStationAndClass = new HashTable("AS TEMP MVA Response Time Per Station And Class");
        this.tableMVAResponseTimePerClass = new HashTable("AS TEMP MVA Response Time Per Class");
        this.tableMVAThroughputPerClass = new HashTable("AS TEMP MVA Throughput Per Class");
        this.tableMVAUtilisationPerStationAndClass = new HashTable("AS TEMP MVA Utilisation Per Class");
        this.tableMVAProbabilitiesLdDevices = new HashTable("AS TEMP MVA Probabilities for LD devices");
        this.tableMVAProbabilitiesLdDevicesNminus1 = new HashTable("AS TEMP MVA Probabilities for LD devices (Population N - 1r)");
        this.tableMVAUtilisationPerStation = new HashTable("AS TEMP MVA Utilisation");
        this.tableMVAFillingPerStation = new HashTable("AS TEMP MVA Filling");
        this.tableMVAQueueLengthPerStationAndClass = new HashTable("AS TEMP MVA Queue Length Per Class");
        this.tableMVAQueueLengthPerStation = new HashTable("AS TEMP MVA Queue Length");
    }

    /**
	 * Initializes HashTable "ThroughputEstimator".
	 */
    public final void generateHashTableThroughputEstimator() {
        this.tableMVAThroughputPerClassEstimator = new HashTable("AS TEMP MVA Throughput Estimator");
    }

    /**
	 * Returns the complete throughput of the queueing network.
	 * @return The complete throughput as double
	 */
    public final double getCompleteThroughput() {
        return this.completeThroughput;
    }

    /**
	 * Returns the input parameters for the algorithm.
	 * @return The input parameters of the algorithm
	 */
    public final AlgorithmInput getInputParameters() {
        return this.inputparameters;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAEstimatorPerStationAndClass() {
        return this.tableMVAEstimatorPerStationAndClass;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAFillingPerStation() {
        return this.tableMVAFillingPerStation;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAFillingPerStationAndClassN() {
        return this.tableMVAFillingPerStationAndClassN;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAFillingPerStationAndClassNminus1() {
        return this.tableMVAFillingPerStationAndClassNminus1;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAProbabilitiesLdDevices() {
        return this.tableMVAProbabilitiesLdDevices;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAProbabilitiesLdDevicesNminus1() {
        return this.tableMVAProbabilitiesLdDevicesNminus1;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAQueueLengthPerStation() {
        return this.tableMVAQueueLengthPerStation;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAQueueLengthPerStationAndClass() {
        return this.tableMVAQueueLengthPerStationAndClass;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAResponseTimePerClass() {
        return this.tableMVAResponseTimePerClass;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAResponseTimePerStationAndClass() {
        return this.tableMVAResponseTimePerStationAndClass;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAThroughputPerClass() {
        return this.tableMVAThroughputPerClass;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAThroughputPerClassEstimator() {
        return this.tableMVAThroughputPerClassEstimator;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAUtilisationPerStation() {
        return this.tableMVAUtilisationPerStation;
    }

    /**
	 * Returns the given Hashtable.
	 * @return The HashTable
	 */
    public final HashTable getTableMVAUtilisationPerStationAndClass() {
        return this.tableMVAUtilisationPerStationAndClass;
    }

    /**
	 * Initialize the Database for the algorithm. Writes the model to be solved
	 * into the database.
	 * @param input The Input of the algorithm
	 * @param dbManager The manager for the interaction with the db
	 * @return int: The scenarioID of the scenario
	 */
    public final int initialiseDBForClosedAlgorithms(final AlgorithmInput input, final DBManager dbManager) {
        boolean success;
        if (!dbManager.databaseExists()) {
            success = dbManager.initializeTables();
            if (success) {
                logger.debug("Initialized the tables");
            } else {
                logger.debug("Tables not initialized!");
            }
        } else {
            logger.debug("DB already initialized!");
        }
        return dbManager.insertScenarioDescription(input.getBc());
    }

    /**
	 * Initialize the Database for the algorithm. Writes the model to be solved
	 * into the database.
	 * @param input The Input of the algorithm
	 * @param dbManager The manager for the interaction with the db
	 * @return int: The scenarioID for the scenario
	 */
    public final int initialiseDBForSingleAlgorithms(final AlgorithmInput input, final DBManager dbManager) {
        boolean success;
        if (!dbManager.databaseExists()) {
            success = dbManager.initializeTables();
            if (success) {
                logger.debug("Initialized the tables");
            } else {
                logger.debug("Tables not initialized!");
            }
        } else {
            logger.debug("DB already initialized!");
        }
        return dbManager.insertScenarioDescription(input.getBc());
    }

    /**
	 * Inserts the scenario definition and solutions into the db.
	 * @param solutionID The id of the current model analysis
	 * @param scenarioID The id of the scenario
	 * @param input The Input of the algorithm
	 * @param dbManager The manager for the interaction with the db
	 * @param loadfactor The actual load factor for which the results are
	 *            written into the database
	 */
    public void insertScenarioAndSolutionForClosedAlgorithms(final int solutionID, final int scenarioID, final AlgorithmInput input, final DBManager dbManager, final double loadfactor) {
        double numberOfUsers;
        final long executionID = dbManager.insertScenarioDefinitions(solutionID, "Loadfactor " + loadfactor);
        for (int c = 0; c < input.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) input.getCustomerClasses().get(c);
            numberOfUsers = customerclass.getPopulation();
            dbManager.insertClassScenario(executionID, customerclass.getClassID(), scenarioID, numberOfUsers, loadfactor);
        }
        dbManager.insertAnalyticScenarioResults(executionID, this.completeThroughput);
        Vector<ClassResultBean> crbVector = new Vector<ClassResultBean>();
        for (int c = 0; c < input.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) input.getCustomerClasses().get(c);
            ClassResultBean crb = new ClassResultBean();
            crb.setClassID(customerclass.getClassID());
            crb.setExecutionID(executionID);
            crb.setResponseTime(this.tableMVAResponseTimePerClass.read(customerclass.getClassID()));
            crb.setThroughput(loadfactor * input.getArrivalrate(customerclass.getClassID()));
            crbVector.add(crb);
        }
        dbManager.insertAnalyticTotalClassResultsBatch(crbVector);
        for (int s = 0; s < input.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) input.getStations().get(s);
            dbManager.insertAnalyticTotalStationResults(station.getStationID(), executionID, -1, this.tableMVAUtilisationPerStation.read(station.getStationID()), this.tableMVAFillingPerStation.read(station.getStationID()));
            Vector<ClassStationResultBean> csrbVector = new Vector<ClassStationResultBean>();
            for (int c = 0; c < input.getCustomerClasses().size(); c++) {
                ClassStationResultBean csrb = new ClassStationResultBean();
                final Class customerclass = (Class) input.getCustomerClasses().get(c);
                csrb.setClassID(customerclass.getClassID());
                csrb.setExecutionID(executionID);
                csrb.setQueueLength(this.tableMVAQueueLengthPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                csrb.setResponseTime(this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                csrb.setStationID(station.getStationID());
                csrb.setUtilization(this.tableMVAUtilisationPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
                csrbVector.add(csrb);
            }
            dbManager.insertAnalyticClassStationResultsBatch(csrbVector);
        }
    }

    /**
	 * Inserts the scenario definition and solutions into the db.
	 * @param solutionID The id of the current model analysis
	 * @param scenarioID The id of the scenario
	 * @param input The Input of the algorithm
	 * @param dbManager The manager for the interaction with the db
	 * @param loadfactor The actual loadfactor for which the results are written
	 *            into the database
	 */
    public void insertScenarioAndSolutionForSingleAlgorithms(final int solutionID, final int scenarioID, final AlgorithmInput input, final DBManager dbManager, final double loadfactor) {
        double arrivalrate = 0;
        final long executionID = dbManager.insertScenarioDefinitions(solutionID, "Description");
        for (int c = 0; c < input.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) input.getCustomerClasses().get(c);
            arrivalrate = loadfactor * this.inputparameters.getArrivalrate(customerclass.getClassID());
            dbManager.insertClassScenario(executionID, customerclass.getClassID(), scenarioID, arrivalrate, loadfactor);
        }
        dbManager.insertAnalyticScenarioResults(executionID, arrivalrate);
        Vector<ClassResultBean> crbVector = new Vector<ClassResultBean>();
        for (int c = 0; c < input.getCustomerClasses().size(); c++) {
            final Class customerclass = (Class) input.getCustomerClasses().get(c);
            ClassResultBean crb = new ClassResultBean();
            crb.setClassID(customerclass.getClassID());
            crb.setExecutionID(executionID);
            crb.setResponseTime(this.tableMVAResponseTimePerClass.read(customerclass.getClassID()));
            crb.setThroughput(loadfactor * input.getArrivalrate(customerclass.getClassID()));
            crbVector.add(crb);
        }
        dbManager.insertAnalyticTotalClassResultsBatch(crbVector);
        final AnalyticQNStation station = (AnalyticQNStation) input.getStations().get(0);
        dbManager.insertAnalyticTotalStationResults(station.getStationID(), executionID, this.tableMVAQueueLengthPerStation.read(station.getStationID()), this.tableMVAUtilisationPerStation.read(station.getStationID()), -1);
        Vector<ClassStationResultBean> csrbVector = new Vector<ClassStationResultBean>();
        for (int c = 0; c < input.getCustomerClasses().size(); c++) {
            ClassStationResultBean csrb = new ClassStationResultBean();
            final Class customerclass = (Class) input.getCustomerClasses().get(c);
            csrb.setClassID(customerclass.getClassID());
            csrb.setExecutionID(executionID);
            csrb.setQueueLength(this.tableMVAQueueLengthPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
            csrb.setResponseTime(this.tableMVAResponseTimePerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
            csrb.setStationID(station.getStationID());
            csrb.setUtilization(this.tableMVAUtilisationPerStationAndClass.read(station.getStationID(), customerclass.getClassID()));
            csrbVector.add(csrb);
        }
        dbManager.insertAnalyticClassStationResultsBatch(csrbVector);
    }

    /**
	 * Checks if one station has a queue length greater than the allowed one.
	 * Queue length is filling - customers in service
	 * @return The number of the station or -1
	 */
    public final int isQueueLengthOfOneStationGreaterThanAllowed() {
        for (int s = 0; s < this.inputparameters.getStations().size(); s++) {
            final AnalyticQNStation station = (AnalyticQNStation) this.inputparameters.getStations().get(s);
            if (station.getQueueLength() != -1 && (this.getTableMVAFillingPerStation().read(station.getStationID()) - station.getNrParallelWorkers() > station.getQueueLength())) {
                logger.debug("queuelength = " + station.getQueueLength());
                return s;
            }
        }
        return -1;
    }

    /**
	 * Copies the source HashTable into this HashTable.
	 * @param source The source HashTable
	 */
    public final void setTableMVAFillingPerStation(final HashTable source) {
        this.tableMVAFillingPerStation = source;
    }

    /**
	 * Copies the source HashTable into this HashTable.
	 * @param source The source HashTable
	 */
    public final void setTableMVAFillingPerStationAndClassN(final HashTable source) {
        this.tableMVAFillingPerStationAndClassN = source;
    }

    /**
	 * Copies the source HashTable into this HashTable.
	 * @param source The source HashTable
	 */
    public final void setTableMVAProbabilitiesLdDevices(final HashTable source) {
        this.tableMVAProbabilitiesLdDevices = source;
    }

    /**
	 * Copies the source HashTable into this HashTable.
	 * @param source The source HashTable
	 */
    public final void setTableMVAResponseTimePerStationAndClass(final HashTable source) {
        this.tableMVAResponseTimePerStationAndClass = source;
    }

    /**
	 * Copies the source HashTable into this HashTable.
	 * @param source The source HashTable
	 */
    public final void setTableMVAThroughputPerClass(final HashTable source) {
        this.tableMVAThroughputPerClass = source;
    }

    /**
	 * Helper method to visualisize the responsetimes of a station for a
	 * specific class having added the responsetimes of the subservice call
	 * station to their parents.
	 * @param input
	 */
    public final void visualisizeNewResponseTimesPerStationAndClass(final AlgorithmInput input) {
        for (Class currClass : input.getCustomerClasses()) {
            for (AnalyticQNStation currStation : input.getStations()) {
                logger.debug("Responsetime von Klasse " + currStation.getStationID() + " an Station " + currStation.getStationID() + " = " + this.tableMVAResponseTimePerStationAndClass.read(currStation.getStationID(), currClass.getClassID()));
            }
        }
    }

    public final void removeAddedStationsOfClosingAlgorithm(final AlgorithmInput input) {
        for (Class currClass : input.getCustomerClasses()) {
            for (AnalyticQNStation currStation : input.getStations()) {
                for (ClassPart currClassPart : currClass.getClassParts().getClassPart()) {
                    if (currClassPart.getStationID().startsWith("station") && currClassPart.getStationID().equals(currStation.getStationID())) {
                        double newClassResponseTime = this.getTableMVAResponseTimePerClass().read(currClass.getClassID()) - this.getTableMVAResponseTimePerStationAndClass().read(currStation.getStationID(), currClass.getClassID());
                        this.getTableMVAResponseTimePerClass().write(currClass.getClassID(), newClassResponseTime);
                    }
                }
            }
        }
    }
}
