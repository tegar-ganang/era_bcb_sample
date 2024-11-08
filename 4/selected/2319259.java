package playground.wrashid.sschieffer.DSC;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import lpsolve.LpSolveException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.integration.RombergIntegrator;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegratorImpl;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.geotools.referencing.factory.AllAuthoritiesFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DSC.LP.LPEV;
import playground.wrashid.sschieffer.DSC.LP.LPPHEV;
import playground.wrashid.sschieffer.DSC.Reading.AgentTimeIntervalReader;
import playground.wrashid.sschieffer.DSC.SlotDistribution.ChargingSlotDistributor;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.GeneralSource;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubLoadDistributionReader;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeDataCollector;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeInterval;
import playground.wrashid.sschieffer.SetUp.VehicleDefinition.Battery;
import playground.wrashid.sschieffer.SetUp.VehicleDefinition.GasType;
import playground.wrashid.sschieffer.SetUp.VehicleDefinition.VehicleTypeCollector;
import playground.wrashid.sschieffer.V2G.ContractTypeAgent;
import playground.wrashid.sschieffer.V2G.V2G;

/**berlinInput
 * Controls the charging algorithm
	 * 1) determining and sorting agents schedules
	 * 2) LP
	 * 3) charging slot optimization
	 * 4) V2G 
	 * 
 * @author Stella
 *
 */
public class DecentralizedSmartCharger {

    public static boolean debug = false;

    public static final double SECONDSPERMIN = 60;

    public static final double SECONDSPER15MIN = 15 * 60;

    public static final double SECONDSPERDAY = 24 * 60 * 60;

    public static final int MINUTESPERDAY = 24 * 60;

    public static final double KWHPERJOULE = 1.0 / (3600.0 * 1000.0);

    public static double STANDARDCONNECTIONSWATT;

    public static String outputPath;

    final Controler controler;

    public static HubLoadDistributionReader myHubLoadReader;

    public static ChargingSlotDistributor myChargingSlotDistributor;

    public static AgentTimeIntervalReader myAgentTimeReader;

    private LPEV lpev;

    private LPPHEV lpphev;

    private static VehicleTypeCollector myVehicleTypes;

    public static LinkedListValueHashMap<Id, Vehicle> vehicles;

    public ParkingTimesPlugin parkingTimesPlugin;

    public EnergyConsumptionPlugin energyConsumptionPlugin;

    private static HashMap<Id, Schedule> agentParkingAndDrivingSchedules;

    public HashMap<Id, Schedule> agentChargingSchedules;

    private double averageChargingCostsAgent, averageChargingCostsAgentEV, averageChargingCostsAgentPHEV;

    private double averageChargingTimeAgent, averageChargingTimeAgentEV, averageChargingTimeAgentPHEV;

    public double minChargingLength;

    public double emissionCounter;

    public LinkedList<Id> chargingFailureEV;

    public LinkedList<Id> agentsWithEV;

    public LinkedList<Id> agentsWithPHEV;

    public LinkedList<Id> agentsWithCombustion;

    public LinkedList<Id> deletedAgents;

    public LinkedList<Id> deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge = new LinkedList<Id>();

    public HashMap<Id, Double> agentChargingCosts;

    public static V2G myV2G;

    private HashMap<Id, ContractTypeAgent> agentContracts;

    private double xPercentDown, xPercentDownUp;

    private DifferentiableMultivariateVectorialOptimizer optimizer;

    private VectorialConvergenceChecker checker = new SimpleVectorialValueChecker(10000, -10000);

    public static SimpsonIntegrator functionSimpsonIntegrator = new SimpsonIntegrator();

    public static UnivariateRealIntegratorImpl realFuncIntegrator;

    public static PolynomialFitter polyFit;

    public static final DrawingSupplier supplier = new DefaultDrawingSupplier();

    /**
	 * times to track time use of different calculations
	 */
    private double startTime, agentReadTime, LPTime, distributeTime, wrapUpTime;

    private double startV2G, timeCheckVehicles, timeCheckHubSources, timeCheckRemainingSources;

    private static double cutoff = 0.00001;

    /**
	 * initialization of object with basic parameters
	 */
    public DecentralizedSmartCharger(Controler controler, ParkingTimesPlugin parkingTimesPlugin, EnergyConsumptionPlugin energyConsumptionPlugin, String outputPath, VehicleTypeCollector myVehicleTypes, double STANDARDCONNECTIONSWATT) throws IOException, OptimizationException {
        this.controler = controler;
        DecentralizedSmartCharger.outputPath = outputPath;
        DecentralizedSmartCharger.STANDARDCONNECTIONSWATT = STANDARDCONNECTIONSWATT;
        myAgentTimeReader = new AgentTimeIntervalReader(parkingTimesPlugin, energyConsumptionPlugin);
        this.myVehicleTypes = myVehicleTypes;
        initialize();
    }

    private void initialize() {
        emissionCounter = 0.0;
        optimizer = new GaussNewtonOptimizer(true);
        optimizer.setMaxIterations(10000);
        optimizer.setConvergenceChecker(checker);
        polyFit = new PolynomialFitter(20, optimizer);
        chargingFailureEV = new LinkedList<Id>();
        agentsWithEV = new LinkedList<Id>();
        agentsWithPHEV = new LinkedList<Id>();
        agentsWithCombustion = new LinkedList<Id>();
        deletedAgents = new LinkedList<Id>();
        agentChargingCosts = new HashMap<Id, Double>();
        agentParkingAndDrivingSchedules = new HashMap<Id, Schedule>();
        agentChargingSchedules = new HashMap<Id, Schedule>();
    }

    /**
	 * sets Agent contracts, these are relevant for the V2G procedure
	 * @param agentContracts
	 */
    public void setAgentContracts(HashMap<Id, ContractTypeAgent> agentContracts) {
        this.agentContracts = agentContracts;
    }

    /**
	 * turns extra output on or off, that can be helpful for debugging
	 * i.e. understanding because of which agent the simulation was shut down, etc.
	 * @param onOff
	 */
    public void setDebug(boolean onOff) {
        debug = onOff;
    }

    /**
	 * initialize LPs for EV, PHEV and combustion vehicle
	 * <li>the buffer is important for the EV calculation
	 * <li> the boolean regulates if SOC graphs for all agents over the day are printed after the LPs
	 * @param buffer
	 * @param output
	 */
    public void initializeLP(double buffer, boolean output) {
        lpev = new LPEV(buffer, output);
        lpphev = new LPPHEV(output);
    }

    /**
	 * initialize ChargingSlotDistributor by setting the standard charging length
	 * <p> the standard charging length is the time interval in which charging slots are usually booked. 
	 * Only if the time is very constrained within in interval and if the allocation of a sufficient number of charging slots can not be achieved
	 * one long charging slot can be assigned. Smaller charging slots can also be assigned in order to book the required full charging time for an interval</p>
	 * @param minChargingLength
	 */
    public void initializeChargingSlotDistributor(double minChargingLength) {
        this.minChargingLength = minChargingLength;
        myChargingSlotDistributor = new ChargingSlotDistributor(minChargingLength);
    }

    public void setLinkedListValueHashMapVehicles(LinkedListValueHashMap<Id, Vehicle> vehicles) {
        this.vehicles = vehicles;
        myV2G = new V2G(this);
    }

    public HashMap<Id, ContractTypeAgent> getAgentContracts() {
        return agentContracts;
    }

    public LPEV getLPEV() {
        return lpev;
    }

    public LPPHEV getLPPHEV() {
        return lpphev;
    }

    /**
	 * initializes HubLoadDistributionReader with its basic parameters
	 * @param hubLinkMapping
	 * @param deterministicHubLoadDistribution
	 * @param pricingHubDistribution
	 * @throws OptimizationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public void initializeHubLoadDistributionReader(HubLinkMapping hubLinkMapping, HashMap<Integer, Schedule> deterministicHubLoadDistribution, HashMap<Integer, Schedule> pricingHubDistribution, double minPricePerkWhAllHubs, double maxPricePerkWhAllHubs) throws OptimizationException, IOException, InterruptedException {
        myHubLoadReader = new HubLoadDistributionReader(controler, hubLinkMapping, deterministicHubLoadDistribution, pricingHubDistribution, myVehicleTypes, outputPath, minPricePerkWhAllHubs, maxPricePerkWhAllHubs);
    }

    /**
	 * sets the stochastic loads in the hubDistributionReader
	 * @param stochasticHubLoadDistribution
	 * @param locationSourceMapping
	 * @param agentVehicleSourceMapping
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
    public void setStochasticSources(HashMap<Integer, Schedule> stochasticHubLoadDistribution, HashMap<Id, GeneralSource> locationSourceMapping, HashMap<Id, Schedule> agentVehicleSourceMapping) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException {
        myHubLoadReader.setStochasticSources(stochasticHubLoadDistribution, locationSourceMapping, agentVehicleSourceMapping);
    }

    /**
	 * get agent schedules, find required charging times, assign charging times
	 * @throws Exception 
	 */
    public void run() throws Exception {
        startTime = System.currentTimeMillis();
        System.out.println("\n Reading agent Schedules");
        readAgentSchedules();
        agentReadTime = System.currentTimeMillis();
        System.out.println("\n Starting LP");
        findRequiredChargingTimes();
        LPTime = System.currentTimeMillis();
        System.out.println("\n Assigning charging times");
        assignChargingTimes();
        distributeTime = System.currentTimeMillis();
        System.out.println("\n Update loads ");
        updateDeterministicLoad();
        System.out.println("\n Update costs ");
        calculateAverageChargingCostsAllAgents();
        calculateAverageChargingTimesAllAgents();
        wrapUpTime = System.currentTimeMillis();
        System.out.println("Decentralized Smart Charger DONE");
        writeSummaryDSCHTML("DSC" + vehicles.getKeySet().size() + "agents_" + minChargingLength + "chargingLength");
        writeSummaryDSCPerAgent("DSCPerAgent");
        deleteAgentsInList(chargingFailureEV);
    }

    /**
	 * Loops over all agents
	 * Calls AgentChargingTimeReader to read in their schedule
	 * saves the schedule in agentParkingAndDrivingSchedules
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 * @throws IOException 
	 */
    public void readAgentSchedules() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException {
        for (Id id : vehicles.getKeySet()) {
            if (DecentralizedSmartCharger.debug) {
                System.out.println("getAgentSchedule: " + id.toString());
            }
            agentParkingAndDrivingSchedules.put(id, myAgentTimeReader.readParkingAndDrivingTimes(id));
            if (agentParkingAndDrivingSchedules.get(id).getNumberOfEntries() == 0 || agentParkingAndDrivingSchedules.get(id).timesInSchedule.get(0).getStartTime() != 0.0 || agentParkingAndDrivingSchedules.get(id).timesInSchedule.get(agentParkingAndDrivingSchedules.get(id).getNumberOfEntries() - 1).getEndTime() != SECONDSPERDAY) {
                deletedAgents.add(id);
                System.out.println("deleted agent : " + id.toString());
            }
        }
        deleteAgentsInList(deletedAgents);
    }

    /**
	 * deletes all agent Ids in list 
	 * from 
	 * <li>agentParkingAndDrivingSchedules
	 * <li>vehicles
	 * 
	 * @param list
	 */
    public void deleteAgentsInList(LinkedList<Id> list) {
        for (int i = 0; i < list.size(); i++) {
            Id id = list.get(i);
            agentParkingAndDrivingSchedules.remove(id);
            vehicles.getKeySet().remove(id);
            if (agentsWithEV.contains(id)) {
                agentsWithEV.remove(id);
            }
            if (agentsWithPHEV.contains(id)) {
                agentsWithPHEV.remove(id);
            }
        }
    }

    /**
	 * deletes all agent Ids in list 
	 * from 
	 * <li>agentParkingAndDrivingSchedules,
	 * <li>vehicles,
	 * <li>agentsWithEV,
	 * <li>myHubLoadReader.agentVehicleSourceMapping
	 * @param list
	 */
    public void deleteFailedEVAgentsForV2G(LinkedList<Id> list) {
        for (int i = 0; i < list.size(); i++) {
            Id id = list.get(i);
            agentParkingAndDrivingSchedules.remove(id);
            vehicles.getKeySet().remove(id);
            if (agentsWithEV.contains(id)) {
                agentsWithEV.remove(id);
            }
            if (agentsWithPHEV.contains(id)) {
                agentsWithPHEV.remove(id);
            }
        }
    }

    /**
	 * finds the required charging times for all parking intervals based on the daily plan of the agent
	 * the required charging times are then stored within the existing parkingAndDrivingSchedule of the agent
	 * @throws LpSolveException
	 * @throws IOException
	 */
    public void findRequiredChargingTimes() throws LpSolveException, IOException {
        for (Id id : vehicles.getKeySet()) {
            if (debug) {
                System.out.println("Find required charging times - LP - agent" + id.toString());
            }
            String type = "";
            double joulesFromEngine = 0;
            double batterySize = getBatteryOfAgent(id).getBatterySize();
            double batteryMin = getBatteryOfAgent(id).getMinSOC();
            double batteryMax = getBatteryOfAgent(id).getMaxSOC();
            if (hasAgentPHEV(id)) {
                agentsWithPHEV.add(id);
                type = "PHEVVehicle";
            } else {
                agentsWithEV.add(id);
                type = "EVVehicle";
            }
            Schedule scheduleAfterLP = lpev.solveLP(agentParkingAndDrivingSchedules.get(id), id, batterySize, batteryMin, batteryMax, type);
            if (scheduleAfterLP != null) {
                agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
            } else {
                scheduleAfterLP = lpphev.solveLP(agentParkingAndDrivingSchedules.get(id), id, batterySize, batteryMin, batteryMax, type);
                agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
                if (scheduleAfterLP == null) {
                    System.out.println("too restrictive even for PHEV - can happen if imposing how muhc to charge boundaries");
                    deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge.add(id);
                } else {
                    joulesFromEngine = lpphev.getEnergyFromCombustionEngine();
                    if (hasAgentEV(id)) {
                        chargingFailureEV.add(id);
                    } else {
                        emissionCounter += joulesToEmissionInKg(id, joulesFromEngine);
                    }
                }
            }
        }
        System.out.println("total phevs deleted: " + deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge.size());
        deleteAgentsInList(deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge);
    }

    /**
	 * passes schedule with required charging information to
	 * ChargingSlotDistributor to obtain exact charging Slots
	 * Saves charging slots in agentChargignSchedule
	 * @throws Exception 
	 */
    public void assignChargingTimes() throws Exception {
        for (Id id : vehicles.getKeySet()) {
            System.out.println("Assign charging times agent " + id.toString());
            Schedule chargingSchedule = myChargingSlotDistributor.distribute(id, agentParkingAndDrivingSchedules.get(id));
            agentChargingSchedules.put(id, chargingSchedule);
            if (id.toString().equals(Integer.toString(1))) {
                visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), agentChargingSchedules.get(id), id);
            }
        }
        printGraphChargingTimesEVAndPHEVAgents();
        if (debug || vehicles.getKeySet().size() < 1000) {
            printGraphChargingTimesAllAgents();
        }
    }

    /**
	 * visualizes the daily plans (parking driving charging)of all agents and saves the files in the format
	 * outputPath+ "DecentralizedCharger/agentPlans/"+ id.toString()+"_dayPlan.png"
	 * @throws IOException
	 */
    public void visualizeDailyPlanForAllAgents() throws IOException {
        for (Id id : vehicles.getKeySet()) {
            visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), agentChargingSchedules.get(id), id);
        }
    }

    /**
	 *  visualizes the daily plan for agent with given id and saves the file in the format
	 * outputPath+ "DecentralizedCharger/agentPlans/"+ id.toString()+"_dayPlan.png"
	 * @param id
	 * @throws IOException
	 */
    public void visualizeDailyPlanForAgent(Id id) throws IOException {
        visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), agentChargingSchedules.get(id), id);
    }

    /**
	 * records the parking times of all agents at hubs and visualizes the distribition for each hub over the day
	 * @throws Exception 
	 */
    private void findConnectivityDistribution() throws Exception {
        for (int i = 0; i < MINUTESPERDAY; i++) {
            double thisSecond = i * SECONDSPERMIN;
            for (Id id : vehicles.getKeySet()) {
                Schedule thisAgentParkAndDrive = agentParkingAndDrivingSchedules.get(id);
                int interval = thisAgentParkAndDrive.timeIsInWhichInterval(thisSecond);
                if (thisAgentParkAndDrive.timesInSchedule.get(interval).isParking()) {
                    int hub = myHubLoadReader.getHubForLinkId(((ParkingInterval) thisAgentParkAndDrive.timesInSchedule.get(interval)).getLocation());
                    myHubLoadReader.recordParkingAgentAtHubInMinute(i, hub);
                }
            }
        }
        myHubLoadReader.calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader();
    }

    /**
	 * for every agent's charging times the deterministic hub load is updated within the HubLoadreader,
	 * the new load distribution is then visualized: visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger();
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
    public void updateDeterministicLoad() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException {
        for (Id id : vehicles.getKeySet()) {
            System.out.println("update deterministic hub load with agent " + id.toString());
            Schedule thisAgent = agentParkingAndDrivingSchedules.get(id);
            for (int i = 0; i < thisAgent.getNumberOfEntries(); i++) {
                if (thisAgent.timesInSchedule.get(i).isParking()) {
                    ParkingInterval p = (ParkingInterval) thisAgent.timesInSchedule.get(i);
                    if (p.getRequiredChargingDuration() != 0.0) {
                        Schedule charging = p.getChargingSchedule();
                        for (int c = 0; c < charging.getNumberOfEntries(); c++) {
                            myHubLoadReader.updateLoadAfterDeterministicChargingDecision((charging.timesInSchedule.get(c)).getStartTime(), (charging.timesInSchedule.get(c)).getEndTime(), p.getLocation(), p.getChargingSpeed(), false);
                        }
                    }
                }
            }
        }
        visualizeDeterministicLoadXYSeriesBeforeAfterDecentralizedSmartCharger();
    }

    /**
	 * calculates and stores the final average charging costs for all agent schedules and their associated charging times
	 * </br> </br>
	 * the results are stored in agentChargingCosts
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
    public void calculateAverageChargingCostsAllAgents() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException {
        for (Id id : vehicles.getKeySet()) {
            Schedule s = agentParkingAndDrivingSchedules.get(id);
            double thisChargingCost = calculateChargingCostForAgentSchedule(id, s);
            agentChargingCosts.put(id, thisChargingCost);
            if (!chargingFailureEV.contains(id)) {
                averageChargingCostsAgent += thisChargingCost;
                if (hasAgentEV(id)) {
                    averageChargingCostsAgentEV += thisChargingCost;
                } else {
                    averageChargingCostsAgentPHEV += thisChargingCost;
                }
            }
        }
        int numEVsNotfail = agentsWithEV.size() - chargingFailureEV.size();
        int numPHEVs = agentsWithPHEV.size();
        averageChargingCostsAgent = averageChargingCostsAgent / (numEVsNotfail + numPHEVs);
        if (agentsWithEV.size() == 0) {
            averageChargingCostsAgentEV = 0;
        } else {
            averageChargingCostsAgentEV = averageChargingCostsAgentEV / (numEVsNotfail);
        }
        if (agentsWithPHEV.size() == 0) {
            averageChargingCostsAgentPHEV = 0;
        } else {
            averageChargingCostsAgentPHEV = averageChargingCostsAgentPHEV / (numPHEVs);
        }
    }

    public void calculateAverageChargingTimesAllAgents() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException {
        for (Id id : vehicles.getKeySet()) {
            if (!chargingFailureEV.contains(id)) {
                double time = agentChargingSchedules.get(id).getTotalTimeOfIntervalsInSchedule();
                averageChargingTimeAgent += time;
                if (hasAgentEV(id)) {
                    averageChargingTimeAgentEV += time;
                } else {
                    averageChargingTimeAgentPHEV += time;
                }
            }
        }
        int numEVsNotfail = agentsWithEV.size() - chargingFailureEV.size();
        int numPHEVs = agentsWithPHEV.size();
        averageChargingTimeAgent = averageChargingTimeAgent / (numEVsNotfail + numPHEVs);
        if (agentsWithEV.size() == 0) {
            averageChargingTimeAgentEV = 0;
        } else {
            averageChargingTimeAgentEV = averageChargingTimeAgentEV / (numEVsNotfail);
        }
        if (agentsWithPHEV.size() == 0) {
            averageChargingTimeAgentPHEV = 0;
        } else {
            averageChargingTimeAgentPHEV = averageChargingTimeAgentPHEV / (numPHEVs);
        }
    }

    /**
	 * calculates the costs of charging for a schedule
	 * <li> integrates all charging tims with price functions
	 * <li> for PHEVS: adds gas costs for joules that had to be taken from engine
	 * <li> for EVs: adds Double.MAX_VALUE to the costs, in case the LP was not successful and a battery swap was necessary
	 * @param id
	 * @param s
	 * @return
	 */
    public double calculateChargingCostForAgentSchedule(Id id, Schedule s) {
        double totalCost = 0;
        for (int i = 0; i < s.getNumberOfEntries(); i++) {
            TimeInterval t = s.timesInSchedule.get(i);
            if (t.isParking() && ((ParkingInterval) t).getChargingSchedule() != null) {
                Id linkId = ((ParkingInterval) t).getLocation();
                ArrayList<LoadDistributionInterval> pricingLoadsDuringInterval = myHubLoadReader.getPricingLoadDistributionIntervalsAtLinkAndTime(linkId, t);
                Schedule charging = ((ParkingInterval) t).getChargingSchedule();
                for (int priceIntervalX = 0; priceIntervalX < pricingLoadsDuringInterval.size(); priceIntervalX++) {
                    LoadDistributionInterval currentpricingInterval = pricingLoadsDuringInterval.get(priceIntervalX);
                    PolynomialFunction currentPriceFunc = currentpricingInterval.getPolynomialFunction();
                    Schedule currentOverlapCharging = new Schedule();
                    currentOverlapCharging = charging.getOverlapWithLoadDistributionInterval(currentpricingInterval);
                    for (int c = 0; c < currentOverlapCharging.getNumberOfEntries(); c++) {
                        try {
                            totalCost += functionSimpsonIntegrator.integrate(currentPriceFunc, currentOverlapCharging.timesInSchedule.get(c).getStartTime(), currentOverlapCharging.timesInSchedule.get(c).getEndTime());
                        } catch (Exception e) {
                            System.out.println("ERROR - Method: calculateChargingCostForAgentSchedule");
                            System.out.println("current charging Schedule");
                            currentOverlapCharging.printSchedule();
                            System.out.println("Agent Schedule");
                            s.printSchedule();
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (hasAgentPHEV(id)) {
                if (t.isDriving() && ((DrivingInterval) t).getExtraConsumption() > 0) {
                    totalCost += joulesExtraConsumptionToGasCosts(id, ((DrivingInterval) t).getExtraConsumption());
                }
            }
            if (hasAgentEV(id)) {
                if (t.isDriving() && ((DrivingInterval) t).getExtraConsumption() > 0) {
                    if (DecentralizedSmartCharger.debug) {
                        System.out.println("extra consumption EV price calculation ");
                        System.out.println("assigned " + Double.MAX_VALUE + " for agent " + id.toString());
                    }
                    totalCost = Double.MAX_VALUE;
                }
            }
        }
        return totalCost;
    }

    /**
	 * get the percentage of agents with EV or PHEV who have the contract type: regulation up and down
	 * @return
	 */
    public double getPercentDownUp() {
        return xPercentDownUp;
    }

    /**
	 * get the percentage of agents with EV or PHEV who have the contract type: regulation down
	 * @return
	 */
    public double getPercentDown() {
        return xPercentDown;
    }

    public void setV2GRegUpAndDownStats(double xPercentDown, double xPercentDownUp) {
        this.xPercentDownUp = xPercentDownUp;
        this.xPercentDown = xPercentDown;
    }

    public void initializeAndRunV2G(double xPercentDown, double xPercentDownUp) throws Exception {
        System.out.println("\n Find Connectivty Curve for V2G");
        findConnectivityDistribution();
        myV2G.initializeAgentStats();
        setV2GRegUpAndDownStats(xPercentDown, xPercentDownUp);
        startV2G = System.currentTimeMillis();
        System.out.println("START CHECKING VEHICLE SOURCES");
        checkVehicleSources();
        timeCheckVehicles = System.currentTimeMillis();
        System.out.println("START CHECKING HUB SOURCES");
        checkHubSources();
        timeCheckHubSources = System.currentTimeMillis();
        System.out.println("START CHECKING STOCHASTIC HUB LOADS");
        myHubLoadReader.recalculateStochasticHubLoadCurveAfterVehicleAndHubSources();
        checkRemainingStochasticHubLoads();
        timeCheckRemainingSources = System.currentTimeMillis();
        myV2G.calcV2GVehicleStats();
        myV2G.calcHubSourceStats();
        System.out.println("DONE V2G");
        writeSummaryV2G("V2G" + vehicles.getKeySet().size() + "agents_" + minChargingLength + "chargingLength");
        writeSummaryV2GTXT("V2G" + vehicles.getKeySet().size() + "agents_" + minChargingLength + "chargingLength");
    }

    /**
	 * loops over all vehicle sources and tries to provide regulation up or down if this function is activated by the agent
	 * @throws Exception 
	 */
    public void checkVehicleSources() throws Exception {
        if (myHubLoadReader.agentVehicleSourceMapping != null) {
            for (Id id : myHubLoadReader.agentVehicleSourceMapping.keySet()) {
                if (vehicles.containsKey(id)) {
                    Schedule electricSource = myHubLoadReader.agentVehicleSourceMapping.get(id);
                    for (int i = 0; i < electricSource.getNumberOfEntries(); i++) {
                        LoadDistributionInterval electricSourceInterval = (LoadDistributionInterval) electricSource.timesInSchedule.get(i);
                        int intervals = (int) Math.ceil(electricSourceInterval.getIntervalLength() / minChargingLength);
                        for (int intervalNum = 0; intervalNum < intervals; intervalNum++) {
                            double bit = 0;
                            if (intervalNum < intervals - 1) {
                                bit = minChargingLength;
                            } else {
                                bit = electricSourceInterval.getIntervalLength() - (intervals - 1) * minChargingLength;
                            }
                            if (bit > 30.0) {
                                double start = electricSourceInterval.getStartTime() + intervalNum * minChargingLength;
                                double end = start + bit;
                                PolynomialFunction func = new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
                                LoadDistributionInterval currentStochasticLoadInterval = new LoadDistributionInterval(start, end, func, electricSourceInterval.isOptimal());
                                double joulesFromSource = functionSimpsonIntegrator.integrate(func, currentStochasticLoadInterval.getStartTime(), currentStochasticLoadInterval.getEndTime());
                                String type;
                                if (hasAgentPHEV(id)) {
                                    type = "PHEVRescheduleVehicleSourceAgent_" + id.toString();
                                } else {
                                    type = "EVRescheduleVehicleSourceAgent_" + id.toString();
                                }
                                double compensation;
                                if (Math.abs(joulesFromSource) > cutoff * STANDARDCONNECTIONSWATT) {
                                    if (joulesFromSource > 0) {
                                        compensation = myHubLoadReader.getMinPricePerKWHAllHubs() * KWHPERJOULE * joulesFromSource;
                                        myV2G.regulationUpDownVehicleLoad(id, currentStochasticLoadInterval, compensation, joulesFromSource, type);
                                    } else {
                                        compensation = 0.0;
                                        myV2G.regulationUpDownVehicleLoad(id, currentStochasticLoadInterval, compensation, joulesFromSource, type);
                                    }
                                }
                            }
                        }
                    }
                    visualizeStochasticLoadVehicleBeforeAfterV2G(id);
                }
            }
        }
    }

    public void checkHubSources() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException {
        if (myHubLoadReader.locationSourceMapping != null) {
            for (Id linkId : myHubLoadReader.locationSourceMapping.keySet()) {
                if (DecentralizedSmartCharger.debug) {
                    System.out.println("check Hub Sources for" + linkId.toString());
                }
                Schedule electricSource = myHubLoadReader.locationSourceMapping.get(linkId).getLoadSchedule();
                for (int i = 0; i < electricSource.getNumberOfEntries(); i++) {
                    LoadDistributionInterval electricSourceInterval = (LoadDistributionInterval) electricSource.timesInSchedule.get(i);
                    double joulesFromSource = functionSimpsonIntegrator.integrate(electricSourceInterval.getPolynomialFunction(), electricSourceInterval.getStartTime(), electricSourceInterval.getEndTime());
                    double compensation;
                    if (Math.abs(joulesFromSource) > cutoff * STANDARDCONNECTIONSWATT) {
                        if (joulesFromSource > 0) {
                            compensation = myHubLoadReader.locationSourceMapping.get(linkId).getFeedInCompensationPerKWH() * KWHPERJOULE * joulesFromSource;
                            myV2G.feedInHubSource(linkId, electricSourceInterval, compensation, joulesFromSource);
                        } else {
                            if (joulesFromSource < 0) {
                                myV2G.hubSourceChargeExtra(linkId, electricSourceInterval, joulesFromSource);
                            }
                        }
                    }
                }
                visualizeStochasticLoadHubSourceBeforeAfterV2G(linkId);
            }
        }
    }

    /**
	 * loops over all hubs and the stochastic loads in small time intervals of the same length as the standard charging slot length
	 * </br> 
	 * if agents are activated for V2G, it will be checked if the agent can perform V2G (i.e. is he at the right hub and parking, economic decision?)
	 *  </br>   </br> 
	 * the result is then visualized </br> 
	 * visualizeStochasticLoadBeforeAfterV2G();
	 * @throws Exception 
	 */
    public void checkRemainingStochasticHubLoads() throws Exception {
        if (myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources != null) {
            for (Integer h : myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.keySet()) {
                if (DecentralizedSmartCharger.debug) {
                    System.out.println("check hubSource for Hub " + h.toString());
                }
                Schedule hubStochasticSchedule = myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.get(h);
                for (int j = 0; j < hubStochasticSchedule.getNumberOfEntries(); j++) {
                    LoadDistributionInterval stochasticLoad = (LoadDistributionInterval) hubStochasticSchedule.timesInSchedule.get(j);
                    PolynomialFunction func = stochasticLoad.getPolynomialFunction();
                    int intervals = (int) Math.ceil(stochasticLoad.getIntervalLength() / minChargingLength);
                    for (int i = 0; i < intervals; i++) {
                        double bit = 0;
                        if (i < intervals - 1) {
                            bit = minChargingLength;
                        } else {
                            bit = stochasticLoad.getIntervalLength() - (intervals - 1) * minChargingLength;
                        }
                        if (bit > 30.0) {
                            double start = stochasticLoad.getStartTime() + i * minChargingLength;
                            double end = start + bit;
                            double joulesFromSource = functionSimpsonIntegrator.integrate(func, start, end);
                            System.out.println("joules from source =" + joulesFromSource + " is big enough to be considered for V2G:  " + (Math.abs(joulesFromSource) > cutoff * STANDARDCONNECTIONSWATT));
                            if (Math.abs(joulesFromSource) > cutoff * STANDARDCONNECTIONSWATT) {
                                if (joulesFromSource < 0) {
                                    double expectedNumberOfParkingAgents = getPercentDownUp() * myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(h, start);
                                    if (expectedNumberOfParkingAgents > 0.0) {
                                        func = func.multiply(new PolynomialFunction(new double[] { 1 / expectedNumberOfParkingAgents }));
                                        LoadDistributionInterval currentStochasticLoadInterval = new LoadDistributionInterval(start, end, func, stochasticLoad.isOptimal());
                                        for (Id agentId : vehicles.getKeySet()) {
                                            String type;
                                            if (hasAgentPHEV(agentId)) {
                                                type = "PHEVStochasticLoadRegulationUp";
                                            } else {
                                                type = "EVStochasticLoadRegulationUp";
                                            }
                                            if (isAgentRegulationUp(agentId)) {
                                                myV2G.regulationUpDownHubLoad(agentId, currentStochasticLoadInterval, agentParkingAndDrivingSchedules.get(agentId), type, h);
                                            }
                                        }
                                    }
                                }
                                if (joulesFromSource > 0) {
                                    double expectedNumberOfParkingAgents = (getPercentDown() + getPercentDownUp()) * myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(h, start);
                                    if (debug) {
                                        System.out.println("expected number of agents: " + myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(h, start));
                                        System.out.println("of which reg only down" + getPercentDown() + " of which reg up and down" + getPercentDownUp());
                                        System.out.println("expected number of agents: " + myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(h, start));
                                    }
                                    if (expectedNumberOfParkingAgents > 0.0) {
                                        func = func.multiply(new PolynomialFunction(new double[] { 1 / expectedNumberOfParkingAgents }));
                                        LoadDistributionInterval currentStochasticLoadInterval = new LoadDistributionInterval(start, end, func, stochasticLoad.isOptimal());
                                        for (Id agentId : vehicles.getKeySet()) {
                                            String type;
                                            if (hasAgentPHEV(agentId)) {
                                                type = "PHEVStochasticLoadRegulationDown";
                                            } else {
                                                type = "EVStochasticLoadRegulationDown";
                                            }
                                            if (isAgentRegulationDown(agentId)) {
                                                myV2G.regulationUpDownHubLoad(agentId, currentStochasticLoadInterval, agentParkingAndDrivingSchedules.get(agentId), type, h);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                visualizeStochasticLoadBeforeAfterV2G();
            }
        }
    }

    public static double integrateTrapezoidal(PolynomialFunction func, double start, double end) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
        realFuncIntegrator = new TrapezoidIntegrator(func);
        realFuncIntegrator.setMaximalIterationCount(64);
        realFuncIntegrator.setRelativeAccuracy(STANDARDCONNECTIONSWATT * cutoff);
        double test = realFuncIntegrator.integrate(start, end);
        return test;
    }

    public static double integrateRomberg(PolynomialFunction func, double start, double end) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
        realFuncIntegrator = new RombergIntegrator(func);
        realFuncIntegrator.setMaximalIterationCount(32);
        realFuncIntegrator.setRelativeAccuracy(STANDARDCONNECTIONSWATT * cutoff);
        double test = realFuncIntegrator.integrate(start, end);
        return test;
    }

    /**
	 * checks the total revenue from V2G of the agent
	 * @param id
	 * @return
	 */
    public double getV2GRevenueForAgent(Id id) {
        return myV2G.getAgentV2GRevenues(id);
    }

    /**
	 * checks if agents contract allows regulation up
	 * @param id
	 * @return
	 */
    public boolean isAgentRegulationUp(Id id) {
        return agentContracts.get(id).isUp();
    }

    /**
	 * checks if agents contract allows regulation down
	 * @param id
	 * @return
	 */
    public boolean isAgentRegulationDown(Id id) {
        return agentContracts.get(id).isDown();
    }

    /**
	 * checks if agent has a PHEV
	 * @param id
	 * @return
	 */
    public static boolean hasAgentPHEV(Id id) {
        Vehicle v = vehicles.getValue(id);
        if (v.getClass().equals(PlugInHybridElectricVehicle.class)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isAgentAtHub(Id id, Integer hubNumber) {
        boolean isAtHub = false;
        Schedule s = agentParkingAndDrivingSchedules.get(id);
        for (int i = 0; i < s.getNumberOfEntries(); i++) {
            if (s.timesInSchedule.get(i).isParking()) {
                ParkingInterval p = (ParkingInterval) s.timesInSchedule.get(i);
                if (myHubLoadReader.getHubForLinkId(p.getLocation()) == hubNumber) {
                    isAtHub = true;
                    return isAtHub;
                }
            }
        }
        return isAtHub;
    }

    /**
	 * checks if agent has a EV
	 * @param id
	 * @return
	 */
    public static boolean hasAgentEV(Id id) {
        Vehicle v = vehicles.getValue(id);
        if (v.getClass().equals(ElectricVehicle.class)) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * returns the HashMap<Id, Double> with all charging costs of all agents from charging and V2G
	 * @return
	 */
    public HashMap<Id, Double> getChargingCostsForAgents() {
        return agentChargingCosts;
    }

    /**
	 * returns the chronological sequences/schedules of parking and driving intervals for all agents
	 * @return
	 */
    public HashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules() {
        return agentParkingAndDrivingSchedules;
    }

    /**
	 * returns HashMap<Id, Schedule> agentChargingSchedules
	 */
    public HashMap<Id, Schedule> getAllAgentChargingSchedules() {
        return agentChargingSchedules;
    }

    /**
	 * returns a linkedList of all agents with an EV
	 * @return
	 */
    public LinkedList<Id> getAllAgentsWithEV() {
        return agentsWithEV;
    }

    /**
	 * returns a linkedList of all agents with a PHEV
	 * @return
	 */
    public LinkedList<Id> getAllAgentsWithPHEV() {
        return agentsWithPHEV;
    }

    /**
	 * get charging schedule of agent with Id id
	 * @param id
	 * @return
	 */
    public Schedule getAgentChargingSchedule(Id id) {
        return agentChargingSchedules.get(id);
    }

    /**
	 * returns a linkedList of all agents with an EV, where the linear optimization was not successful, i.e. a battery swap would have been necessary
	 * @return
	 */
    public LinkedList<Id> getIdsOfEVAgentsWithFailedOptimization() {
        return chargingFailureEV;
    }

    /**
	 * returns the total Consumption in joules of the agent for normal electric driving and from the engine
	 * @param id
	 * @return
	 */
    public double getTotalDrivingConsumptionOfAgent(Id id) {
        return agentParkingAndDrivingSchedules.get(id).getTotalBatteryConsumption() + agentParkingAndDrivingSchedules.get(id).getExtraConsumptionFromEngine();
    }

    /**
	 * returns the total consumption in joules from the battery
	 * @param id
	 * @return
	 */
    public double getTotalDrivingConsumptionOfAgentFromBattery(Id id) {
        return agentParkingAndDrivingSchedules.get(id).getTotalBatteryConsumption();
    }

    /**
	 * returns the total consumption in joules from other sources than battery
	 * <li> for PHEVs engine
	 * <li> for EVs missing energy that would have needed a battery swap
	 * @param id
	 * @return
	 */
    public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id id) {
        return agentParkingAndDrivingSchedules.get(id).getExtraConsumptionFromEngine();
    }

    /**
	 * returns total emissions caused from PHEVs
	 * @return
	 */
    public double getTotalEmissions() {
        return emissionCounter;
    }

    /**
	 * return average charging  cost   of agents
	 * @return
	 */
    public double getAverageChargingCostAgents() {
        return averageChargingCostsAgent;
    }

    /**
	 * return average charging cost   of EV agents
	 * @return
	 */
    public double getAverageChargingCostEV() {
        return averageChargingCostsAgentEV;
    }

    /**
	 * return average charging cost  of PHEV agents
	 * @return
	 */
    public double getAverageChargingCostPHEV() {
        return averageChargingCostsAgentPHEV;
    }

    /**
	 * returns the average charging time of all agents
	 * @return
	 */
    public double getAverageChargingTimeAgents() {
        return averageChargingTimeAgent;
    }

    public double getAverageChargingTimePHEV() {
        return averageChargingTimeAgentPHEV;
    }

    public double getAverageChargingTimeEV() {
        return averageChargingTimeAgentEV;
    }

    /**
	 * plots daily schedule and charging times of agent 
	 * and save it in: 
	 * outputPath+ "DecentralizedCharger/agentPlans/"+ id.toString()+"_dayPlan.png"
		  
	 * @param dailySchedule
	 * @param chargingSchedule
	 * @param id
	 * @throws IOException
	 */
    private void visualizeAgentChargingProfile(Schedule dailySchedule, Schedule chargingSchedule, Id id) throws IOException {
        XYSeriesCollection agentOverview = new XYSeriesCollection();
        if (vehicles.getValue(id).getClass().equals(PlugInHybridElectricVehicle.class)) {
            for (int i = 0; i < dailySchedule.getNumberOfEntries(); i++) {
                if (dailySchedule.timesInSchedule.get(i).isDriving()) {
                    DrivingInterval thisD = (DrivingInterval) dailySchedule.timesInSchedule.get(i);
                }
            }
        }
        for (int i = 0; i < dailySchedule.getNumberOfEntries(); i++) {
            if (dailySchedule.timesInSchedule.get(i).isDriving()) {
                XYSeries drivingTimesSet = new XYSeries("driving times");
                drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getStartTime(), 4);
                drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getEndTime(), 4);
                agentOverview.addSeries(drivingTimesSet);
            } else {
                ParkingInterval p = (ParkingInterval) dailySchedule.timesInSchedule.get(i);
                if (p.isInSystemOptimalChargingTime()) {
                    XYSeries parkingOptimalTimesSet = new XYSeries("parking times during optimal charging time");
                    parkingOptimalTimesSet.add(p.getStartTime(), 3);
                    parkingOptimalTimesSet.add(p.getEndTime(), 3);
                    agentOverview.addSeries(parkingOptimalTimesSet);
                } else {
                    XYSeries parkingSuboptimalTimesSet = new XYSeries("parking times during suboptimal charging time");
                    parkingSuboptimalTimesSet.add(p.getStartTime(), 2);
                    parkingSuboptimalTimesSet.add(p.getEndTime(), 2);
                    agentOverview.addSeries(parkingSuboptimalTimesSet);
                }
            }
        }
        for (int i = 0; i < chargingSchedule.getNumberOfEntries(); i++) {
            XYSeries chargingTimesSet = new XYSeries("charging times");
            chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getStartTime(), 1);
            chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getEndTime(), 1);
            agentOverview.addSeries(chargingTimesSet);
        }
        JFreeChart chart = ChartFactory.createXYLineChart("Travel pattern agent : " + id.toString(), "time [s]", "charging, off-peak parking, peak-parking, driving times", agentOverview, PlotOrientation.VERTICAL, true, true, false);
        chart.setBackgroundPaint(Color.white);
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);
        XYTextAnnotation txt1 = new XYTextAnnotation("Charging time", 20000, 1.1);
        XYTextAnnotation txt2 = new XYTextAnnotation("Driving time", 20000, 4.1);
        XYTextAnnotation txt3 = new XYTextAnnotation("Optimal parking time", 20000, 3.1);
        XYTextAnnotation txt4 = new XYTextAnnotation("Suboptimal parking time", 20000, 2.1);
        XYTextAnnotation txt5 = new XYTextAnnotation("Driving with engine power", 20000, 3.85);
        txt1.setFont(new Font("Arial", Font.PLAIN, 14));
        txt2.setFont(new Font("Arial", Font.PLAIN, 14));
        txt3.setFont(new Font("Arial", Font.PLAIN, 14));
        txt4.setFont(new Font("Arial", Font.PLAIN, 14));
        txt5.setFont(new Font("Arial", Font.PLAIN, 14));
        txt5.setPaint(Color.red);
        plot.addAnnotation(txt1);
        plot.addAnnotation(txt2);
        plot.addAnnotation(txt3);
        plot.addAnnotation(txt4);
        plot.addAnnotation(txt5);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, SECONDSPERDAY);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 5);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setVisible(false);
        int numSeries = dailySchedule.getNumberOfEntries() + chargingSchedule.getNumberOfEntries();
        for (int j = 0; j < numSeries; j++) {
            plot.getRenderer().setSeriesPaint(j, Color.black);
            setSeriesStroke(plot, j, 1.0f, 1.0f, 0.0f);
        }
        ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger/agentPlans/" + id.toString() + "_dayPlan.png"), chart, 1000, 1000);
    }

    /**
	 * Deterministic free load before and after
	 * @throws IOException
	 */
    private void visualizeDeterministicLoadXYSeriesBeforeAfterDecentralizedSmartCharger() throws IOException {
        System.out.println();
        for (Integer i : myHubLoadReader.deterministicHubLoadAfter15MinBins.keySet()) {
            visualizeTwoXYLoadSeriesBeforeAfter(myHubLoadReader.deterministicHubLoadDistribution.get(i), myHubLoadReader.deterministicHubLoadAfter15MinBins.get(i).getXYSeries("Free Load at Hub " + i.toString() + " after"), "Free Load at Hub " + i.toString() + " before", outputPath + "Hub/deterministicLoadBeforeAfter_hub" + i.toString() + ".png", "DeterministicLoadBeforeAfter_hub" + i.toString(), true);
        }
    }

    private void visualizeStochasticLoadBeforeAfterV2G() throws IOException {
        for (Integer i : myHubLoadReader.stochasticHubLoadAfterVehicleAndHubSources.keySet()) {
            XYSeries sBefore = myHubLoadReader.getXYSeriesForHubXFromStochasticLoadAfterVehicleAndHubSources15MinBins(i);
            XYSeries sAfter = myHubLoadReader.stochasticHubLoadAfter15MinBins.get(i).getXYSeries("Stochastic free load at hub " + i.toString() + " after V2G");
            visualizeTwoXYLoadSeriesBeforeAfter(sBefore, sAfter, outputPath + "V2G/stochasticLoadBeforeAfterV2G_hub" + i.toString() + ".png", "StochasticLoadBeforeAfter_hub" + i.toString());
        }
    }

    /**
	 * visualize vehicle load 15min bin after and original 
	 * @param id
	 * @throws IOException
	 */
    private void visualizeStochasticLoadVehicleBeforeAfterV2G(Id id) throws IOException {
        Schedule sBefore = myHubLoadReader.agentVehicleSourceMapping.get(id);
        XYSeries sAfter = myHubLoadReader.agentVehicleSourceAfter15MinBins.get(id).getXYSeries("Stochastic free load Agent " + id.toString() + " after V2G");
        visualizeTwoXYLoadSeriesBeforeAfter(sBefore, sAfter, "Stochastic free load Agent " + id.toString() + " before V2G", outputPath + "V2G/stochasticLoadBeforeAfter_agentVehicle" + id.toString() + ".png", "StochasticLoadBeforeAfter_agentVehicle" + id.toString(), false);
    }

    /**
	 * original hub source load and 15 bin hub source load
	 * @param linkId
	 */
    private void visualizeStochasticLoadHubSourceBeforeAfterV2G(Id linkId) {
        Schedule sBefore = myHubLoadReader.locationSourceMapping.get(linkId).getLoadSchedule();
        String name = myHubLoadReader.locationSourceMapping.get(linkId).getName();
        XYSeries sAfter = myHubLoadReader.locationSourceMappingAfter15MinBins.get(linkId).getXYSeries("Hub Source '" + name + "' at Link " + linkId.toString() + " after V2G");
        visualizeTwoXYLoadSeriesBeforeAfter(sBefore, sAfter, "Hub Source '" + name + "' at Link " + linkId.toString() + "before V2G", outputPath + "V2G/stochasticHubSourceBeforeAfter15MinBin_" + name + ".png", "StochasticHubSourceBeforeAfter15MinBin_" + name, false);
    }

    /**
	 * visualizes two load schedules before and after and writes out a txt file with data values in 15 min bins
	 * 
	 * @param before
	 * @param after
	 * @param XYBefore
	 * @param XYafter
	 * @param outputFile
	 * @param graphTitle
	 */
    public void visualizeTwoLoadSchedulesBeforeAfter(Schedule before, Schedule after, String XYBefore, String XYAfter, String outputFile, String graphTitle) {
        try {
            FileWriter fstream = new FileWriter(outputPath + graphTitle + ".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("time \t before \t after \n");
            XYSeriesCollection load = new XYSeriesCollection();
            XYSeries beforeXY = before.makeXYSeries1MinBinFromLoadSchedule(XYBefore);
            XYSeries afterXY = after.makeXYSeries1MinBinFromLoadSchedule(XYAfter);
            load.addSeries(afterXY);
            load.addSeries(beforeXY);
            for (int i = 0; i < afterXY.getItemCount(); i++) {
                out.write(afterXY.getX(i) + " \t " + beforeXY.getY(i) + " \t " + afterXY.getY(i) + " \n");
            }
            JFreeChart chart = ChartFactory.createXYLineChart(graphTitle, "time [s]", "available load [W]", load, PlotOrientation.VERTICAL, true, true, false);
            chart.setBackgroundPaint(Color.white);
            final XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.gray);
            plot.setRangeGridlinePaint(Color.gray);
            plot.getRenderer().setSeriesPaint(0, Color.red);
            plot.getRenderer().setSeriesPaint(1, Color.black);
            setSeriesStroke(plot, 0, 1.0f, 1.0f, 0.0f);
            setSeriesStroke(plot, 1, 5.0f, 1.0f, 0.0f);
            ChartUtilities.saveChartAsPNG(new File(outputFile), chart, 1000, 1000);
            out.close();
        } catch (Exception e) {
        }
    }

    public static void visualizeTwoXYLoadSeriesBeforeAfter(Schedule beforeSchedule, XYSeries after, String XYBefore, String outputFile, String graphTitle, boolean txt) {
        try {
            String filename = outputPath + graphTitle + ".txt";
            FileWriter fstream = new FileWriter(filename);
            BufferedWriter out = new BufferedWriter(fstream);
            if (txt) {
                out.write("time \t before \t after \n");
            }
            XYSeriesCollection load = new XYSeriesCollection();
            XYSeries beforeXY = beforeSchedule.makeXYSeriesXSecBinBinFromLoadSchedule(XYBefore, SECONDSPER15MIN);
            load.addSeries(after);
            load.addSeries(beforeXY);
            for (int i = 0; i < after.getItemCount(); i++) {
                if (txt) {
                    out.write(after.getX(i) + " \t " + beforeXY.getY(i) + " \t " + after.getY(i) + " \n");
                }
            }
            JFreeChart chart = ChartFactory.createXYLineChart(graphTitle, "time [s]", "available load [W]", load, PlotOrientation.VERTICAL, true, true, false);
            chart.setBackgroundPaint(Color.white);
            final XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.gray);
            plot.setRangeGridlinePaint(Color.gray);
            plot.getRenderer().setSeriesPaint(0, Color.red);
            plot.getRenderer().setSeriesPaint(1, Color.black);
            setSeriesStroke(plot, 0, 5.0f, 3.0f, 3.0f);
            setSeriesStroke(plot, 1, 5.0f, 1.0f, 0.0f);
            ChartUtilities.saveChartAsPNG(new File(outputFile), chart, 1000, 1000);
            if (txt) {
                out.close();
            }
            File f = new File(filename);
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void visualizeTwoXYLoadSeriesBeforeAfter(XYSeries before, XYSeries after, String outputFile, String graphTitle) {
        try {
            FileWriter fstream = new FileWriter(outputPath + graphTitle + ".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("time \t before \t after \n");
            XYSeriesCollection load = new XYSeriesCollection();
            load.addSeries(after);
            load.addSeries(before);
            for (int i = 0; i < after.getItemCount(); i++) {
                out.write(after.getX(i) + " \t " + before.getY(i) + " \t " + after.getY(i) + " \n");
            }
            JFreeChart chart = ChartFactory.createXYLineChart(graphTitle, "time [s]", "available load [W]", load, PlotOrientation.VERTICAL, true, true, false);
            chart.setBackgroundPaint(Color.white);
            final XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.gray);
            plot.setRangeGridlinePaint(Color.gray);
            plot.getRenderer().setSeriesPaint(0, Color.red);
            plot.getRenderer().setSeriesPaint(1, Color.black);
            setSeriesStroke(plot, 0, 5.0f, 3.0f, 3.0f);
            setSeriesStroke(plot, 1, 5.0f, 1.0f, 0.0f);
            ChartUtilities.saveChartAsPNG(new File(outputFile), chart, 1000, 1000);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printGraphChargingTimesAllAgents() throws IOException {
        XYSeriesCollection allAgentsOverview = new XYSeriesCollection();
        int seriesCount = 0;
        for (Id id : vehicles.getKeySet()) {
            Schedule s1 = agentChargingSchedules.get(id);
            for (int i = 0; i < s1.getNumberOfEntries(); i++) {
                String strId = id.toString();
                int intId = Integer.parseInt(strId);
                XYSeries chargingTimesSet = new XYSeries("charging time");
                chargingTimesSet.add(s1.timesInSchedule.get(i).getStartTime(), intId);
                chargingTimesSet.add(s1.timesInSchedule.get(i).getEndTime(), intId);
                allAgentsOverview.addSeries(chargingTimesSet);
                seriesCount++;
            }
        }
        JFreeChart chart = ChartFactory.createXYLineChart("Distribution of charging times for all agents by agent Id number", "time [s]", "charging times", allAgentsOverview, PlotOrientation.VERTICAL, false, true, false);
        final XYPlot plot = chart.getXYPlot();
        setPlotWhite(plot, 0, vehicles.getKeySet().size(), 0, SECONDSPERDAY);
        for (int j = 0; j < seriesCount; j++) {
            plot.getRenderer().setSeriesPaint(j, Color.black);
            setSeriesStroke(plot, j, 1.0f, 1.0f, 0.0f);
        }
        chart.setTitle(new TextTitle("Distribution of charging times for all agents by agent Id number", new Font("Arial", Font.BOLD, 20)));
        ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger/allAgentsChargingTimes.png"), chart, 2000, (int) (20.0 * (vehicles.getKeySet().size())));
    }

    /**
	 * makes two graphs 
	 * <li> graph of all EVs in system and their charging times
	 * <li> graph of all PHEVs in system and their charging times
	 * 
	 * @throws IOException
	 */
    private void printGraphChargingTimesEVAndPHEVAgents() throws IOException {
        XYSeriesCollection allEVAgentsOverview = new XYSeriesCollection();
        XYSeriesCollection allPHEVAgentsOverview = new XYSeriesCollection();
        int seriesCountEV = 0;
        int seriesCountPHEV = 0;
        int vehicleEV = 1;
        int vehiclePHEV = 1;
        Schedule s1;
        for (Id id : vehicles.getKeySet()) {
            if (hasAgentEV(id)) {
                s1 = agentChargingSchedules.get(id);
                for (int i = 0; i < s1.getNumberOfEntries(); i++) {
                    String strId = id.toString();
                    int intId = Integer.parseInt(strId);
                    XYSeries chargingTimesSet = new XYSeries("charging time");
                    chargingTimesSet.add(s1.timesInSchedule.get(i).getStartTime(), vehicleEV);
                    chargingTimesSet.add(s1.timesInSchedule.get(i).getEndTime(), vehicleEV);
                    allEVAgentsOverview.addSeries(chargingTimesSet);
                    seriesCountEV++;
                }
                if (vehicleEV > 100) {
                    break;
                } else {
                    vehicleEV++;
                }
            }
            if (hasAgentPHEV(id)) {
                s1 = agentChargingSchedules.get(id);
                for (int i = 0; i < s1.getNumberOfEntries(); i++) {
                    String strId = id.toString();
                    int intId = Integer.parseInt(strId);
                    XYSeries chargingTimesSet = new XYSeries("charging time");
                    chargingTimesSet.add(s1.timesInSchedule.get(i).getStartTime(), vehiclePHEV);
                    chargingTimesSet.add(s1.timesInSchedule.get(i).getEndTime(), vehiclePHEV);
                    allPHEVAgentsOverview.addSeries(chargingTimesSet);
                    seriesCountPHEV++;
                }
                if (vehiclePHEV > 100) {
                    break;
                } else {
                    vehiclePHEV++;
                }
            }
        }
        if (vehicleEV > 0) {
            JFreeChart chartEV = ChartFactory.createXYLineChart("Distribution of charging times for all EV agents by agent Id number", "time [s]", "", allEVAgentsOverview, PlotOrientation.VERTICAL, false, true, false);
            final XYPlot plotEV = chartEV.getXYPlot();
            setPlotWhite(plotEV, 0, vehicleEV + 1, 0, SECONDSPERDAY);
            for (int j = 0; j < seriesCountEV; j++) {
                plotEV.getRenderer().setSeriesPaint(j, Color.black);
                setSeriesStroke(plotEV, j, 1, 1.0f, 0.0f);
            }
            chartEV.setTitle(new TextTitle("Charging times for EV agents", new Font("Arial", Font.BOLD, 20)));
            ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger/EVAgentsChargingTimes.png"), chartEV, 2000, (int) (20.0 * (vehicleEV)));
        }
        if (vehiclePHEV > 0) {
            JFreeChart chartPHEV = ChartFactory.createXYLineChart("Distribution of charging times for all PHEV agents by agent Id number", "time [s]", "", allPHEVAgentsOverview, PlotOrientation.VERTICAL, false, true, false);
            final XYPlot plotPHEV = chartPHEV.getXYPlot();
            setPlotWhite(plotPHEV, 0, vehiclePHEV + 1, 0, SECONDSPERDAY);
            for (int j = 0; j < seriesCountPHEV; j++) {
                plotPHEV.getRenderer().setSeriesPaint(j, Color.black);
                setSeriesStroke(plotPHEV, j, 1, 1.0f, 0.0f);
            }
            chartPHEV.setTitle(new TextTitle("Charging times for PHEV agents", new Font("Arial", Font.BOLD, 20)));
            ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger/PHEVAgentsChargingTimes.png"), chartPHEV, 2000, (int) (20.0 * (vehiclePHEV)));
        }
    }

    /**
	 * convenience class to set stroke of Series in plot
	 * @param plot
	 * @param seriesNumber
	 * @param width
	 * @param dash1
	 * @param dash2
	 */
    public static void setSeriesStroke(XYPlot plot, int seriesNumber, float width, float dash1, float dash2) {
        plot.getRenderer().setSeriesStroke(seriesNumber, new BasicStroke(width * 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] { dash1 * 1.0f, dash2 * 1.0f }, 1.0f));
    }

    public void setPlotWhite(XYPlot plot, double yAxisMin, double yAxisMax, double XAxisMin, double XAxisMax) {
        plot.setDrawingSupplier(supplier);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(XAxisMin, XAxisMax);
        NumberAxis yaxis = (NumberAxis) plot.getRangeAxis();
        yaxis.setRange(yAxisMin, yAxisMax);
    }

    /**
	 * transforms joules to emissions according to the vehicle and gas type data stored in the vehicleCollector
	 * <p>When converting the joules needed for driving into liters of gas, the engine efficiency stored in the vehicle type
	 * is considered, i.e.
	 * </br>
	 * joulesUsed = numLiter * joulesPerLiter/efficiency
	 * </p>
	 * 
	 * @param agentId
	 * @param joules
	 * @return
	 */
    public double joulesToEmissionInKg(Id agentId, double joules) {
        Vehicle v = vehicles.getValue(agentId);
        GasType vGT = myVehicleTypes.getGasType(v);
        double liter = 1 / (vGT.getJoulesPerLiter()) * 1 / myVehicleTypes.getEfficiencyOfEngine(v) * joules;
        double emission = vGT.getEmissionsPerLiter() * liter;
        return emission;
    }

    public static double getWattOfVehicleOfAgent(Id id) {
        Vehicle v = vehicles.getValue(id);
        return myVehicleTypes.getWattOfEngine(v);
    }

    /**
	 * converts the extra joules into costs
	 * <p>When converting the joules needed for driving into liters of gas, the engine efficiency stored in the vehicle type
	 * is considered, i.e.
	 * </br>
	 * joulesUsed = numLiter * joulesPerLiter/efficiency
	 * </p>
	 * @param agentId
	 * @param joules
	 * @return
	 */
    public double joulesExtraConsumptionToGasCosts(Id agentId, double joules) {
        Vehicle v = vehicles.getValue(agentId);
        GasType vGT = myVehicleTypes.getGasType(v);
        double liter = 1 / (vGT.getJoulesPerLiter()) * 1 / myVehicleTypes.getEfficiencyOfEngine(v) * joules;
        double cost = vGT.getPricePerLiter() * liter;
        return cost;
    }

    /**
	 * gets the battery object of the vehicle of the agent
	 * @param agentId
	 * @return
	 */
    public Battery getBatteryOfAgent(Id agentId) {
        return myVehicleTypes.getBattery(vehicles.getValue(agentId));
    }

    /**
	 * gets the gasType object of the vehicle of the agent
	 * @param agentId
	 * @return
	 */
    public GasType getGasTypeOfAgent(Id agentId) {
        return myVehicleTypes.getGasType(vehicles.getValue(agentId));
    }

    public void writeOutLoadDistribution(String configName) {
        try {
            String title = (outputPath + configName + "_summary.txt");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Schedule loadSchedule= new Schedule()");
            for (int i = 0; i < myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.get(1).getNumberOfEntries(); i++) {
                LoadDistributionInterval l = (LoadDistributionInterval) myHubLoadReader.deterministicHubLoadDistributionAfterContinuous.get(1).timesInSchedule.get(i);
                out.write("LoadDistributionInterval l" + i + " = new LoadDistributionInterval(" + l.getStartTime() + ", " + l.getEndTime() + ", " + "new PolynomialFunction (new double[]{" + turnDoubleInString(l.getPolynomialFunction().getCoefficients()) + "} " + ")" + ", " + l.isOptimal() + "); \n ");
                out.write("loadSchedule.addTimeInterval()l" + i + "); \n");
            }
            out.close();
        } catch (Exception e) {
        }
    }

    public String turnDoubleInString(double[] d) {
        String s = "";
        for (int i = 0; i < d.length; i++) {
            s = s.concat(Double.toString(d[i]));
            if (i < d.length - 1) {
                s = s.concat(", ");
            }
        }
        return s;
    }

    /**
	 * writes a summary after the decentralized charging process is complete with data on:
	 * <li>  number of vehicles
	 * <li> standard charging slot length
	 * <li> time of calculations:  for reading agents, for LP, for slot distribution
	 * <li> total emissions
	 * <li> summary of vehicle types used
	 * <li> graph of price distribution over the day for every hub
	 * <li> graph of deterministic hub load before and after for every hub
	 * @param configName
	 */
    public void writeSummaryDSCHTML(String configName) {
        try {
            String title = (outputPath + configName + "_summary.html");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<html>");
            out.write("<body>");
            out.write("<h1>Summary of run:  </h1>");
            out.write("<p>");
            out.write("Decentralized Smart Charger </br> </br>");
            out.write("Number of PHEVs: " + getAllAgentsWithPHEV().size() + "</br>");
            out.write("Number of EVs: " + getAllAgentsWithEV().size() + " of which " + chargingFailureEV.size() + " could not complete their trip" + "</br>");
            out.write("</br>");
            out.write("Time </br> </br>");
            out.write("Standard charging slot length [s]:" + minChargingLength + "</br>");
            out.write("Time [ms] reading agent schedules:" + (agentReadTime - startTime) + "</br>");
            out.write("Time [ms] LP:" + (LPTime - agentReadTime) + "</br>");
            out.write("Time [ms] slot distribution:" + (distributeTime - LPTime) + "</br>");
            out.write("Time [ms] wrapping up:" + (wrapUpTime - distributeTime) + "</br>");
            for (int i = 0; i < deletedAgents.size(); i++) {
                out.write("</br>");
                out.write("DELETED AGENT: ");
                out.write("id: " + deletedAgents.get(i).toString());
                out.write("</br>");
            }
            out.write("</br>");
            out.write("CHARGING COSTS </br>");
            out.write("DSC Average charging cost of agents: " + getAverageChargingCostAgents() + "</br>");
            out.write("DSC Average charging cost of EV agents: " + getAverageChargingCostEV() + "</br>");
            out.write("DSC Average charging cost of PHEV agents: " + getAverageChargingCostPHEV() + "</br>");
            out.write("</br>");
            out.write("</br>");
            out.write("CHARGING TIME </br>");
            out.write("Average charging time of agents: " + getAverageChargingTimeAgents() + "</br>");
            out.write("Average charging time of EV agents: " + getAverageChargingTimeEV() + "</br>");
            out.write("Average charging time of PHEV agents: " + getAverageChargingTimePHEV() + "</br>");
            out.write("</br>");
            out.write("TOTAL EMISSIONS: " + getTotalEmissions() + "</br>");
            out.write("</br>");
            for (Integer hub : myHubLoadReader.deterministicHubLoadDistribution.keySet()) {
                out.write("HUB" + hub.toString() + "</br> </br>");
                out.write("Prices </br>");
                String picPrices = outputPath + "Hub/pricesHub_" + hub.toString() + ".png";
                out.write("<img src='" + picPrices + "' alt='' width='80%'");
                out.write("</br> </br>");
                out.write("Load Before and after </br>");
                String picBeforeAfter = outputPath + "Hub/deterministicLoadBeforeAfter_hub1.png";
                out.write("<img src='" + picBeforeAfter + "' alt='' width='80%'");
                out.write("</br> </br>");
            }
            out.write("</p>");
            out.write("</body>");
            out.write("</html>");
            out.close();
        } catch (Exception e) {
        }
    }

    /**
	 * wrutes .txt summary with results from Decentralized Smart Charging algorithm
	 * statistics
	 * @param configName
	 */
    public void writeSummaryTXT(String configName) {
        try {
            String title = (outputPath + configName + "_summary.txt");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Number of PHEVs: \t");
            out.write("Number of EVs: \t");
            out.write(" EV with failure \t");
            out.write("Number of deleted EV agents \t");
            out.write("Number of deleted PHEV agents \t");
            out.write("DSC Average charging cost of agents: \t");
            out.write("DSC Average charging cost of EV agents: \t");
            out.write("DSC Average charging cost of PHEV agents: " + getAverageChargingCostPHEV() + "\t");
            out.write("DSC Average charging time of agents: \t");
            out.write("DSC Average charging time of EV agents: \t");
            out.write("DSC Average charging time of PHEV agents: \t");
            out.write("DSC TOTAL EMISSIONS: \n");
            out.write(getAllAgentsWithPHEV().size() + "\t");
            out.write(getAllAgentsWithEV().size() + "\t");
            out.write(chargingFailureEV.size() + "\t");
            out.write(deletedAgents.size() + "\t");
            out.write(deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge.size() + "\t");
            out.write(getAverageChargingCostAgents() + "\t");
            out.write(getAverageChargingCostEV() + "\t");
            out.write(getAverageChargingCostPHEV() + "\t");
            out.write(getAverageChargingTimeAgents() + "\t");
            out.write(getAverageChargingTimeEV() + "\t");
            out.write(getAverageChargingTimePHEV() + "\t");
            out.write(getTotalEmissions() + "\t");
            out.close();
        } catch (Exception e) {
        }
    }

    /**
	 * writes out html summary of results from V2G
	 * @param configName
	 */
    public void writeSummaryV2G(String configName) {
        try {
            String title = (outputPath + configName + "_summary.html");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<html>");
            out.write("<body>");
            out.write("<h1>Summary of V2G:  </h1>");
            out.write("<p>");
            out.write("TIME </br>");
            out.write("Time [ms] checking vehicle sources:" + (timeCheckVehicles - startV2G) + "</br>");
            out.write("Time [ms] checking hub sources:" + (timeCheckHubSources - timeCheckVehicles) + "</br>");
            out.write("Time [ms] checking remaining sources:" + (timeCheckRemainingSources - timeCheckHubSources) + "</br>");
            out.write(" </br> </br>");
            out.write("V2G INPUT </br>");
            out.write("% of agents with contract 'regulation down': " + getPercentDown() + " </br>");
            out.write("% of agents with contract 'regulation up and down': " + getPercentDownUp() + " </br>");
            out.write(" </br> </br>");
            out.write("V2G REVENUE </br>");
            out.write("Average revenue per agent: " + myV2G.getAverageV2GRevenueAgent() + " </br>");
            out.write("Average revenue per EV agent: " + myV2G.getAverageV2GRevenueEV() + " </br>");
            out.write("Average revenue per PHEV agent: " + myV2G.getAverageV2GRevenuePHEV() + " </br>");
            out.write(" </br> </br>");
            out.write("V2G CHARGING </br>");
            out.write("Total V2G Up provided by all Agents: " + myV2G.getTotalRegulationUp() + " </br>");
            out.write("      of which EV: " + myV2G.getTotalRegulationUpEV() + " </br>");
            out.write("      of which PHEV: " + myV2G.getTotalRegulationUpPHEV() + " </br> </br>");
            out.write("Total V2G Down provided by all Agents: " + myV2G.getTotalRegulationDown() + " </br>");
            out.write("      of which EV: " + myV2G.getTotalRegulationDownEV() + " </br>");
            out.write("      of which PHEV: " + myV2G.getTotalRegulationDownPHEV() + " </br>");
            if (myHubLoadReader.agentVehicleSourceMapping != null) {
                out.write("Vehicle Load of agent: 1 </br> </br>");
                out.write("Stochastic load before and after </br>");
                String picBeforeAfter = outputPath + "V2G/stochasticLoadBeforeAfter_agentVehicle1.png";
                out.write("<img src='" + picBeforeAfter + "' alt='' width='80%'");
                out.write("</br> </br>");
            } else {
                out.write("No Vehicle Loads </br> </br>");
            }
            for (Integer hub : myHubLoadReader.stochasticHubLoadDistribution.keySet()) {
                out.write("HUB" + hub.toString() + "</br> </br>");
                out.write("Stochastic load before and after </br>");
                String picBeforeAfter = outputPath + "V2G/stochasticLoadBeforeAfter_hub" + hub.toString() + ".png";
                out.write("<img src='" + picBeforeAfter + "' alt='' width='80%'");
                out.write("</br> </br>");
            }
            out.write("</p>");
            out.write("</body>");
            out.write("</html>");
            out.close();
        } catch (Exception e) {
        }
    }

    /**
	 * writes out text file with statistics from V2G 
	 * @param configName
	 */
    public void writeSummaryV2GTXT(String configName) {
        try {
            String title = (outputPath + configName + "_summary.txt");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Number of PHEVs: \t");
            out.write("Number of EVs: \t");
            out.write(" EV with failure \t");
            out.write("Number of deleted EV agents \t");
            out.write("Number of deleted PHEV agents \t");
            out.write("DSC Average charging cost of agents: \t");
            out.write("DSC Average charging cost of EV agents: \t");
            out.write("DSC Average charging cost of PHEV agents: \t");
            out.write("DSC Average charging time of agents: \t");
            out.write("DSC Average charging time of EV agents: \t");
            out.write("DSC Average charging time of PHEV agents: \t");
            out.write("DSC TOTAL EMISSIONS: \t ");
            out.write("% of agents with contract 'regulation down': \t");
            out.write("% of agents with contract 'regulation up and down': \t");
            out.write("Average revenue per agent: \t");
            out.write("Average revenue per EV agent: \t");
            out.write("Average revenue per PHEV agent: \t");
            out.write("Total V2G Up provided by all Agents: \t");
            out.write("      of which EV: \t");
            out.write("      of which PHEV: \t");
            out.write("Total V2G Down provided by all Agents: \t");
            out.write("      of which EV: \t");
            out.write("      of which PHEV: \t ");
            out.write("Total direct compensation V2G EV: \t ");
            out.write("Total direct compensation V2G PHEV: \t ");
            out.write("Total indirect saving rescheduling V2G EV: \t ");
            out.write("Total indirect saving rescheduling V2G PHEV: \t \n");
            out.write(getAllAgentsWithPHEV().size() + "\t");
            out.write(getAllAgentsWithEV().size() + "\t");
            out.write(chargingFailureEV.size() + "\t");
            out.write(deletedAgents.size() + "\t");
            out.write(deleterestrictedPHEVAgentsFromLimitBatteryTotalCharge.size() + "\t");
            out.write(getAverageChargingCostAgents() + "\t");
            out.write(getAverageChargingCostEV() + "\t");
            out.write(getAverageChargingCostPHEV() + "\t");
            out.write(getAverageChargingTimeAgents() + "\t");
            out.write(getAverageChargingTimeEV() + "\t");
            out.write(getAverageChargingTimePHEV() + "\t");
            out.write(getTotalEmissions() + "\t");
            out.write(getPercentDown() + "\t");
            out.write(getPercentDownUp() + "\t");
            out.write(myV2G.getAverageV2GRevenueAgent() + "\t");
            out.write(myV2G.getAverageV2GRevenueEV() + "\t");
            out.write(myV2G.getAverageV2GRevenuePHEV() + "\t");
            out.write(myV2G.getTotalRegulationUp() + "\t");
            out.write(myV2G.getTotalRegulationUpEV() + "\t");
            out.write(myV2G.getTotalRegulationUpPHEV() + "\t");
            out.write(myV2G.getTotalRegulationDown() + "\t");
            out.write(myV2G.getTotalRegulationDownEV() + "\t");
            out.write(myV2G.getTotalRegulationDownPHEV() + "\t");
            out.write(myV2G.getTotalDirectCompensationEV() + "\t");
            out.write(myV2G.getTotalDirectCompensationPHEV() + "\t");
            out.write(myV2G.getIndirectSavingReschedulingEV() + "\t");
            out.write(myV2G.getIndirectSavingReschedulingPHEV() + "\n");
            out.close();
        } catch (Exception e) {
        }
    }

    /**
	 * writes out txt file with basic results for every agent
	 * id, EV, charging cost, charging total time, joules from engine, joules from other source
	 * @param configName
	 */
    public void writeSummaryDSCPerAgent(String configName) {
        try {
            String title = (outputPath + configName + "_summary.txt");
            FileWriter fstream = new FileWriter(title);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Agent Id: \t");
            out.write("EV?: \t");
            out.write("charging cost:\t");
            out.write("charging time: \t");
            out.write("number of charging slots: \t");
            out.write("Joules from battery [J]: \t");
            out.write("Joules not from engine [J]: \t");
            out.write("Emissions [kg]: \n");
            for (Id id : vehicles.getKeySet()) {
                out.write(id.toString() + "\t");
                out.write(hasAgentEV(id) + "\t");
                out.write(agentChargingCosts.get(id) + "\t");
                out.write(agentChargingSchedules.get(id).getTotalTimeOfIntervalsInSchedule() + "\t");
                out.write(agentChargingSchedules.get(id).getNumberOfEntries() + " \t");
                out.write(getTotalDrivingConsumptionOfAgentFromBattery(id) + "\t");
                double joulesNotEngine = getTotalDrivingConsumptionOfAgentFromOtherSources(id);
                out.write(joulesNotEngine + "\t");
                out.write(joulesToEmissionInKg(id, joulesNotEngine) + "\n");
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setV2G(V2G setV2G) {
        myV2G = setV2G;
    }

    public static PolynomialFunction fitCurve(double[][] data) throws Exception {
        TimeDataCollector timeC = new TimeDataCollector(data);
        return timeC.getFunction();
    }

    public static void linkedListIdPrinter(HashMap<Id, Schedule> list, String info) {
        System.out.println("Print LinkedList " + info);
        for (Id id : list.keySet()) {
            list.get(id).printSchedule();
        }
    }

    public static void linkedListIntegerPrinter(HashMap<Integer, Schedule> list, String info) {
        System.out.println("Print LinkedList " + info);
        for (Integer id : list.keySet()) {
            list.get(id).printSchedule();
        }
    }
}
