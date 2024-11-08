package org.matsim.core.controler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.corelisteners.LinkStatsControlerListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.MultiModalLegRouter;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.BikeTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeCalculatorWithBufferFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.WalkTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.EnsureActivityReachability;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.MultiModalNetworkCreator;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.NonCarRouteDropper;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.InvertedNetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.knowledges.Knowledges;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.population.VspPlansCleaner;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.TransitControlerListener;
import org.matsim.pt.counts.PtCountControlerListener;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;
import org.matsim.vis.snapshotwriters.VisMobsim;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 * 
 * @author mrieser
 */
public class Controler {

    public static final String DIRECTORY_ITERS = "ITERS";

    public static final String FILENAME_EVENTS_TXT = "events.txt.gz";

    public static final String FILENAME_EVENTS_XML = "events.xml.gz";

    public static final String FILENAME_LINKSTATS = "linkstats.txt.gz";

    public static final String FILENAME_SCORESTATS = "scorestats";

    public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats";

    public static final String FILENAME_POPULATION = "output_plans.xml.gz";

    public static final String FILENAME_NETWORK = "output_network.xml.gz";

    public static final String FILENAME_HOUSEHOLDS = "output_households.xml.gz";

    public static final String FILENAME_LANES = "output_lanes.xml.gz";

    public static final String FILENAME_CONFIG = "output_config.xml.gz";

    private enum ControlerState {

        Init, Running, Shutdown, Finished
    }

    private ControlerState state = ControlerState.Init;

    private String outputPath = null;

    public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");

    private boolean overwriteFiles = false;

    private Integer iteration = null;

    /** The Config instance the Controler uses. */
    protected final Config config;

    private final String configFileName;

    private final String dtdFileName;

    protected EventsManagerImpl events = null;

    protected Network network = null;

    protected Population population = null;

    private Counts counts = null;

    protected TravelTimeCalculator travelTimeCalculator = null;

    private PersonalizableTravelDisutility travelCostCalculator = null;

    protected ScoringFunctionFactory scoringFunctionFactory = null;

    protected StrategyManager strategyManager = null;

    int writeEventsInterval = -1;

    int writePlansInterval = -1;

    CalcLinkStats linkStats = null;

    CalcLegTimes legTimes = null;

    VolumesAnalyzer volumes = null;

    private boolean createGraphs = true;

    private boolean dumpDataAtEnd = true;

    public final IterationStopWatch stopwatch = new IterationStopWatch();

    protected final ScenarioImpl scenarioData;

    protected boolean scenarioLoaded = false;

    private PlansScoring plansScoring = null;

    private RoadPricing roadPricing = null;

    private ScoreStats scoreStats = null;

    private TravelDistanceStats travelDistanceStats = null;

    /**
	 * This variable is used to store the log4j output before it can be written
	 * to a file. This is needed to set the output directory before logging.
	 */
    private CollectLogMessagesAppender collectLogMessagesAppender = null;

    private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

    /**
	 * Attribute for the routing factory
	 */
    private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

    /**
	 * This instance encapsulates all behavior concerning the
	 * ControlerEvents/Listeners
	 */
    private final ControlerListenerManager controlerListenerManager = new ControlerListenerManager(this);

    private static final Logger log = Logger.getLogger(Controler.class);

    private final List<MobsimListener> simulationListener = new ArrayList<MobsimListener>();

    private Thread shutdownHook = new Thread() {

        @Override
        public void run() {
            shutdown(true);
        }
    };

    private TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();

    private MultiModalTravelTimeWrapperFactory multiModalTravelTimeFactory = new MultiModalTravelTimeWrapperFactory();

    private TravelDisutilityFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();

    private ControlerIO controlerIO;

    private MobsimFactory mobsimFactory = null;

    private SignalsControllerListenerFactory signalsFactory = new DefaultSignalsControllerListenerFactory();

    private TransitRouterFactory transitRouterFactory = null;

    volatile Throwable uncaughtException = null;

    private MobsimFactoryRegister mobsimFactoryRegister;

    private SnapshotWriterFactoryRegister snapshotWriterRegister;

    /** initializes Log4J */
    static {
        final String logProperties = "log4j.xml";
        URL url = Loader.getResource(logProperties);
        if (url != null) {
            PropertyConfigurator.configure(url);
        } else {
            Logger root = Logger.getRootLogger();
            root.setLevel(Level.INFO);
            ConsoleAppender consoleAppender = new ConsoleAppender(DEFAULTLOG4JLAYOUT, "System.out");
            consoleAppender.setName("A1");
            root.addAppender(consoleAppender);
            consoleAppender.setLayout(DEFAULTLOG4JLAYOUT);
            log.error("");
            log.error("Could not find configuration file " + logProperties + " for Log4j in the classpath.");
            log.error("A default configuration is used, setting log level to INFO with a ConsoleAppender.");
            log.error("");
            log.error("");
        }
    }

    /**
	 * Initializes a new instance of Controler with the given arguments.
	 * 
	 * @param args
	 *            The arguments to initialize the controler with.
	 *            <code>args[0]</code> is expected to contain the path to a
	 *            configuration file, <code>args[1]</code>, if set, is expected
	 *            to contain the path to a local copy of the DTD file used in
	 *            the configuration file.
	 */
    public Controler(final String[] args) {
        this(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null, null, null);
    }

    public Controler(final String configFileName) {
        this(configFileName, null, null, null);
    }

    public Controler(final Config config) {
        this(null, null, config, null);
    }

    public Controler(final Scenario scenario) {
        this(null, null, null, scenario);
    }

    private Controler(final String configFileName, final String dtdFileName, final Config config, final Scenario scenario) {
        this.collectLogMessagesAppender = new CollectLogMessagesAppender();
        Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
        Gbl.printSystemInfo();
        Gbl.printBuildInfo();
        log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
        this.configFileName = configFileName;
        this.dtdFileName = dtdFileName;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.warn("Getting uncaught Exception in Thread " + t.getName(), e);
                Controler.this.uncaughtException = e;
            }
        });
        if (scenario != null) {
            this.scenarioLoaded = true;
            this.scenarioData = (ScenarioImpl) scenario;
            this.config = scenario.getConfig();
        } else {
            if (this.configFileName == null) {
                if (config == null) {
                    throw new IllegalArgumentException("Either the config or the filename of a configfile must be set to initialize the Controler.");
                }
                this.config = config;
            } else {
                this.config = ConfigUtils.loadConfig(this.configFileName);
                this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
            }
            this.scenarioData = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
        }
        this.network = this.scenarioData.getNetwork();
        this.population = this.scenarioData.getPopulation();
        MobsimRegistrar mobsimRegistrar = new MobsimRegistrar();
        this.mobsimFactoryRegister = mobsimRegistrar.getFactoryRegister();
        SnapshotWriterRegistrar snapshotWriterRegistrar = new SnapshotWriterRegistrar();
        this.snapshotWriterRegister = snapshotWriterRegistrar.getFactoryRegister();
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    /**
	 * Starts the simulation.
	 */
    public void run() {
        if (this.state == ControlerState.Init) {
            init();
            this.controlerListenerManager.fireControlerStartupEvent();
            this.checkConfigConsistencyAndWriteToLog("Config dump before doIterations:");
            doIterations();
            shutdown(false);
        } else {
            log.error("Controler in wrong state to call 'run()'. Expected state: <Init> but was <" + this.state + ">");
        }
    }

    private void init() {
        loadConfig();
        setUpOutputDir();
        if (this.config.multiModal().isMultiModalSimulationEnabled()) {
            setupMultiModalSimulation();
        }
        if (this.config.scenario().isUseTransit()) {
            log.warn("setting up the transit config _after_ the config dump :-( ...");
            setupTransitSimulation();
        }
        initEvents();
        initLogging();
        loadData();
        setUp();
        loadCoreListeners();
        loadControlerListeners();
    }

    private final void setupMultiModalSimulation() {
        log.info("setting up multi modal simulation");
        TravelTimeCalculatorWithBufferFactory timeFactory = new TravelTimeCalculatorWithBufferFactory();
        setTravelTimeCalculatorFactory(timeFactory);
        LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
        for (String mode : CollectionUtils.stringToArray(this.config.multiModal().getSimulatedModes())) {
            ((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(mode, factory);
        }
    }

    private final void setupTransitSimulation() {
        log.info("setting up transit simulation");
        if (!this.config.scenario().isUseVehicles()) {
            log.warn("Your are using Transit but not Vehicles. This most likely won't work.");
        }
        Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
        formats.add(EventsFileFormat.xml);
        this.config.controler().setEventsFileFormats(formats);
        ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
        transitActivityParams.setTypicalDuration(120.0);
        this.config.planCalcScore().addActivityParams(transitActivityParams);
    }

    /**
	 * select if single cpu handler to use or parallel
	 */
    private void initEvents() {
        final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
        final String NUMBER_OF_THREADS = "numberOfThreads";
        final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
        String numberOfThreads = this.config.findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
        String estimatedNumberOfEvents = this.config.findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);
        if (numberOfThreads != null) {
            int numOfThreads = Integer.parseInt(numberOfThreads);
            if (estimatedNumberOfEvents != null) {
                int estNumberOfEvents = Integer.parseInt(estimatedNumberOfEvents);
                this.events = new ParallelEventsManagerImpl(numOfThreads, estNumberOfEvents);
            } else {
                this.events = new ParallelEventsManagerImpl(numOfThreads);
            }
        } else {
            this.events = (EventsManagerImpl) EventsUtils.createEventsManager();
        }
    }

    private void doIterations() {
        ParallelPersonAlgorithmRunner.run(this.getPopulation(), this.config.global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {

            @Override
            public AbstractPersonAlgorithm getPersonAlgorithm() {
                return new PersonPrepareForSim(createRoutingAlgorithm(), Controler.this.scenarioData);
            }
        });
        int firstIteration = this.config.controler().getFirstIteration();
        int lastIteration = this.config.controler().getLastIteration();
        this.state = ControlerState.Running;
        String divider = "###################################################";
        String marker = "### ";
        for (this.iteration = firstIteration; (this.iteration <= lastIteration) && (this.state == ControlerState.Running); this.iteration++) {
            log.info(divider);
            log.info(marker + "ITERATION " + this.iteration + " BEGINS");
            this.stopwatch.setCurrentIteration(this.iteration);
            this.stopwatch.beginOperation("iteration");
            makeIterationPath(this.iteration);
            resetRandomNumbers();
            this.controlerListenerManager.fireControlerIterationStartsEvent(this.iteration);
            if (this.iteration > firstIteration) {
                this.stopwatch.beginOperation("replanning");
                this.controlerListenerManager.fireControlerReplanningEvent(this.iteration);
                this.stopwatch.endOperation("replanning");
            }
            this.controlerListenerManager.fireControlerBeforeMobsimEvent(this.iteration);
            this.stopwatch.beginOperation("mobsim");
            resetRandomNumbers();
            runMobSim();
            this.stopwatch.endOperation("mobsim");
            log.info(marker + "ITERATION " + this.iteration + " fires after mobsim event");
            this.controlerListenerManager.fireControlerAfterMobsimEvent(this.iteration);
            log.info(marker + "ITERATION " + this.iteration + " fires scoring event");
            this.controlerListenerManager.fireControlerScoringEvent(this.iteration);
            log.info(marker + "ITERATION " + this.iteration + " fires iteration end event");
            this.controlerListenerManager.fireControlerIterationEndsEvent(this.iteration);
            this.stopwatch.endOperation("iteration");
            this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
            log.info(marker + "ITERATION " + this.iteration + " ENDS");
            log.info(divider);
        }
        this.iteration = null;
    }

    protected void shutdown(final boolean unexpected) {
        ControlerState oldState = this.state;
        this.state = ControlerState.Shutdown;
        if (oldState == ControlerState.Running) {
            if (unexpected) {
                log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
            } else {
                log.info("S H U T D O W N   ---   start regular shutdown.");
            }
            if (this.uncaughtException != null) {
                log.warn("Shutdown probably caused by the following Exception.", this.uncaughtException);
            }
            this.controlerListenerManager.fireControlerShutdownEvent(unexpected);
            if (this.dumpDataAtEnd) {
                Knowledges kk;
                if (this.config.scenario().isUseKnowledges()) {
                    kk = (this.getScenario()).getKnowledges();
                } else {
                    kk = this.getScenario().retrieveNotEnabledKnowledges();
                }
                new PopulationWriter(this.population, this.network, kk).write(this.controlerIO.getOutputFilename(FILENAME_POPULATION));
                new NetworkWriter(this.network).write(this.controlerIO.getOutputFilename(FILENAME_NETWORK));
                new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(FILENAME_CONFIG));
                ActivityFacilities facilities = this.getFacilities();
                if (facilities != null) {
                    new FacilitiesWriter((ActivityFacilitiesImpl) facilities).write(this.controlerIO.getOutputFilename("output_facilities.xml.gz"));
                }
                if (((NetworkFactoryImpl) this.network.getFactory()).isTimeVariant()) {
                    new NetworkChangeEventsWriter().write(this.controlerIO.getOutputFilename("output_change_events.xml.gz"), ((NetworkImpl) this.network).getNetworkChangeEvents());
                }
                if (this.config.scenario().isUseHouseholds()) {
                    new HouseholdsWriterV10(this.scenarioData.getHouseholds()).writeFile(this.controlerIO.getOutputFilename(FILENAME_HOUSEHOLDS));
                }
                if (this.config.scenario().isUseLanes()) {
                    new LaneDefinitionsWriter20(this.scenarioData.getScenarioElement(LaneDefinitions20.class)).write(this.controlerIO.getOutputFilename(FILENAME_LANES));
                }
                if (!unexpected && this.getConfig().vspExperimental().isWritingOutputEvents()) {
                    File toFile = new File(this.controlerIO.getOutputFilename("output_events.xml.gz"));
                    File fromFile = new File(this.controlerIO.getIterationFilename(this.getLastIteration(), "events.xml.gz"));
                    IOUtils.copyFile(fromFile, toFile);
                }
            }
            if (unexpected) {
                log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
            } else {
                log.info("S H U T D O W N   ---   regular shutdown completed.");
            }
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException e) {
                log.info("Cannot remove shutdown hook. " + e.getMessage());
            }
            this.shutdownHook = null;
            this.collectLogMessagesAppender = null;
            IOUtils.closeOutputDirLogging();
        }
    }

    /**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
    protected void setUp() {
        if (this.config.multiModal().isMultiModalSimulationEnabled()) multiModalSetUp();
        if (this.travelTimeCalculator == null) {
            this.travelTimeCalculator = this.travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
        }
        if (this.travelCostCalculator == null) {
            this.travelCostCalculator = this.travelCostCalculatorFactory.createTravelDisutility(this.travelTimeCalculator, this.config.planCalcScore());
        }
        this.events.addHandler(this.travelTimeCalculator);
        if (this.leastCostPathCalculatorFactory != null) {
            log.info("leastCostPathCalculatorFactory already set, ignoring RoutingAlgorithmType specified in config");
        } else {
            if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
                this.leastCostPathCalculatorFactory = new DijkstraFactory();
            } else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
                this.leastCostPathCalculatorFactory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()), this.config.global().getNumberOfThreads());
            } else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.FastDijkstra)) {
                this.leastCostPathCalculatorFactory = new FastDijkstraFactory();
            } else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.FastAStarLandmarks)) {
                this.leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
            } else {
                throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
            }
        }
        this.linkStats = new CalcLinkStats(this.network);
        this.volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
        this.events.addHandler(this.volumes);
        this.legTimes = new CalcLegTimes();
        this.events.addHandler(this.legTimes);
        if (this.scoringFunctionFactory == null) {
            this.scoringFunctionFactory = loadScoringFunctionFactory();
        }
        this.strategyManager = loadStrategyManager();
    }

    private void multiModalSetUp() {
        if (this.config.multiModal().isCreateMultiModalNetwork()) {
            log.info("Creating multi modal network.");
            new MultiModalNetworkCreator(this.config.multiModal()).run(this.scenarioData.getNetwork());
        }
        if (this.config.multiModal().isEnsureActivityReachability()) {
            log.info("Relocating activities that cannot be reached by the transport modes of their from- and/or to-legs...");
            new EnsureActivityReachability(this.scenarioData).run(this.scenarioData.getPopulation());
        }
        if (this.config.multiModal().isDropNonCarRoutes()) {
            log.info("Dropping existing routes of modes which are simulated with the multi modal mobsim.");
            new NonCarRouteDropper(this.config.multiModal()).run(this.scenarioData.getPopulation());
        }
        this.travelTimeCalculator = this.travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
        TravelTimeFactoryWrapper wrapper = new TravelTimeFactoryWrapper(this.getTravelTimeCalculator());
        PlansCalcRouteConfigGroup configGroup = this.config.plansCalcRoute();
        multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, wrapper);
        multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.walk, new WalkTravelTimeFactory(configGroup));
        multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.bike, new BikeTravelTimeFactory(configGroup, new WalkTravelTimeFactory(configGroup)));
        multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.ride, new RideTravelTimeFactory(wrapper, new WalkTravelTimeFactory(configGroup)));
        multiModalTravelTimeFactory.setPersonalizableTravelTimeFactory(TransportMode.pt, new PTTravelTimeFactory(configGroup, wrapper, new WalkTravelTimeFactory(configGroup)));
    }

    /**
	 * Initializes log4j to write log output to files in output directory.
	 */
    private void initLogging() {
        Logger.getRootLogger().removeAppender(this.collectLogMessagesAppender);
        try {
            IOUtils.initOutputDirLogging(this.config.controler().getOutputDirectory(), this.collectLogMessagesAppender.getLogEvents(), this.config.controler().getRunId());
            this.collectLogMessagesAppender.close();
            this.collectLogMessagesAppender = null;
        } catch (IOException e) {
            log.error("Cannot create logfiles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
	 * Loads the configuration object with the correct settings.
	 */
    private void loadConfig() {
        if (this.configFileName != null) {
            new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
        }
        checkConfigConsistencyAndWriteToLog("Complete config dump directly after reading the config file.  " + "See later for config dump after setup.");
        if (this.writeEventsInterval == -1) {
            this.writeEventsInterval = this.config.controler().getWriteEventsInterval();
        }
        if (this.writePlansInterval == -1) {
            this.writePlansInterval = this.config.controler().getWritePlansInterval();
        }
    }

    /**
	 * Design decisions:
	 * <ul>
	 * <li>I extracted this method since it is now called <i>twice</i>: once
	 * directly after reading, and once before the iterations start. The second
	 * call seems more important, but I wanted to leave the first one there in
	 * case the program fails before that config dump. Might be put into the
	 * "unexpected shutdown hook" instead. kai, dec'10
	 * </ul>
	 * 
	 * @param message
	 *            the message that is written just before the config dump
	 */
    private void checkConfigConsistencyAndWriteToLog(final String message) {
        log.info(message);
        String newline = System.getProperty("line.separator");
        StringWriter writer = new StringWriter();
        new ConfigWriter(this.config).writeStream(new PrintWriter(writer), newline);
        log.info(newline + newline + writer.getBuffer().toString());
        log.info("Complete config dump done.");
        log.info("Checking consistency of config...");
        this.config.checkConsistency();
        log.info("Checking consistency of config done.");
    }

    private final void setUpOutputDir() {
        this.outputPath = this.config.controler().getOutputDirectory();
        if (this.outputPath.endsWith("/")) {
            this.outputPath = this.outputPath.substring(0, this.outputPath.length() - 1);
        }
        if (this.config.controler().getRunId() != null) {
            this.controlerIO = new ControlerIO(this.outputPath, this.scenarioData.createId(this.config.controler().getRunId()));
        } else {
            this.controlerIO = new ControlerIO(this.outputPath);
        }
        File outputDir = new File(this.outputPath);
        if (outputDir.exists()) {
            if (outputDir.isFile()) {
                throw new RuntimeException("Cannot create output directory. " + this.outputPath + " is a file and cannot be replaced by a directory.");
            }
            if (outputDir.list().length > 0) {
                if (this.overwriteFiles) {
                    System.out.flush();
                    log.warn("###########################################################");
                    log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
                    log.warn("### " + this.outputPath);
                    log.warn("###########################################################");
                    System.err.flush();
                } else {
                    throw new RuntimeException("The output directory " + this.outputPath + " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
                }
            }
        } else {
            if (!outputDir.mkdirs()) {
                throw new RuntimeException("The output directory path " + this.outputPath + " could not be created. Check pathname and permissions!");
            }
        }
        File tmpDir = new File(this.controlerIO.getTempPath());
        if (!tmpDir.mkdir() && !tmpDir.exists()) {
            throw new RuntimeException("The tmp directory " + this.controlerIO.getTempPath() + " could not be created.");
        }
        File itersDir = new File(this.outputPath + "/" + DIRECTORY_ITERS);
        if (!itersDir.mkdir() && !itersDir.exists()) {
            throw new RuntimeException("The iterations directory " + (this.outputPath + "/" + DIRECTORY_ITERS) + " could not be created.");
        }
    }

    /**
	 * Load all the required data. Currently, this only loads the Scenario if it
	 * was not given in the Constructor.
	 */
    protected void loadData() {
        if (!this.scenarioLoaded) {
            ScenarioUtils.loadScenario(this.scenarioData);
            this.network = this.scenarioData.getNetwork();
            this.population = this.scenarioData.getPopulation();
            this.scenarioLoaded = true;
        }
    }

    /**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
    protected StrategyManager loadStrategyManager() {
        StrategyManager manager = new StrategyManager();
        StrategyManagerConfigLoader.load(this, manager);
        return manager;
    }

    /**
	 * Loads the {@link ScoringFunctionFactory} to be used for plans-scoring.
	 * This method will only be called if the user has not yet manually set a
	 * custom scoring function with
	 * {@link #setScoringFunctionFactory(ScoringFunctionFactory)}.
	 * 
	 * @return The ScoringFunctionFactory to be used for plans-scoring.
	 */
    protected ScoringFunctionFactory loadScoringFunctionFactory() {
        return new CharyparNagelScoringFunctionFactory(this.config.planCalcScore(), this.getNetwork());
    }

    /**
	 * Loads a default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide basic functionality. <b>Note:</b> Be very
	 * careful if you overwrite this method! The order how the listeners are
	 * added is very important. Check the comments in the source file before
	 * overwriting this method!
	 */
    protected void loadCoreListeners() {
        this.plansScoring = new PlansScoring();
        this.addCoreControlerListener(this.plansScoring);
        if (this.config.scenario().isUseRoadpricing()) {
            this.roadPricing = new RoadPricing();
            this.addCoreControlerListener(this.roadPricing);
        }
        this.addCoreControlerListener(new PlansReplanning());
        this.addCoreControlerListener(new PlansDumping());
        this.addCoreControlerListener(new EventsHandling(this.events));
    }

    /**
	 * Loads the default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide some more basic functionality. Unlike the
	 * core ControlerListeners the order in which the listeners of this method
	 * are added must not affect the correctness of the code.
	 */
    protected void loadControlerListeners() {
        this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));
        this.scoreStats = new ScoreStats(this.population, this.controlerIO.getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
        this.addControlerListener(this.scoreStats);
        this.travelDistanceStats = new TravelDistanceStats(this.population, this.network, this.controlerIO.getOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.createGraphs);
        this.addControlerListener(this.travelDistanceStats);
        if (this.config.counts().getCountsFileName() != null) {
            CountControlerListener ccl = new CountControlerListener(this.config.counts());
            this.addControlerListener(ccl);
            this.counts = ccl.getCounts();
        }
        if (this.config.linkStats().getWriteLinkStatsInterval() > 0) {
            this.addControlerListener(new LinkStatsControlerListener(this.config.linkStats()));
        }
        if (this.config.scenario().isUseTransit()) {
            addControlerListener(new TransitControlerListener());
            if (this.config.ptCounts().getAlightCountsFileName() != null) {
                addControlerListener(new PtCountControlerListener(this.config));
            }
        }
        if (this.config.scenario().isUseSignalSystems()) {
            addControlerListener(this.signalsFactory.createSignalsControllerListener());
        }
        if (!this.config.vspExperimental().getActivityDurationInterpretation().equals(VspExperimentalConfigGroup.MIN_OF_DURATION_AND_END_TIME)) {
            addControlerListener(new VspPlansCleaner());
        }
    }

    /**
	 * Creates the path where all iteration-related data should be stored.
	 * 
	 * @param iteration
	 */
    private void makeIterationPath(final int iteration) {
        File dir = new File(this.controlerIO.getIterationPath(iteration));
        if (!dir.mkdir()) {
            if (this.overwriteFiles && dir.exists()) {
                log.info("Iteration directory " + this.controlerIO.getIterationPath(iteration) + " exists already.");
            } else {
                log.warn("Could not create iteration directory " + this.controlerIO.getIterationPath(iteration) + ".");
            }
        }
    }

    private void resetRandomNumbers() {
        MatsimRandom.reset(this.config.global().getRandomSeed() + this.iteration);
        MatsimRandom.getRandom().nextDouble();
    }

    Mobsim getNewMobsim() {
        if (this.mobsimFactory != null) {
            Mobsim simulation = this.mobsimFactory.createMobsim(this.getScenario(), this.getEvents());
            enrichSimulation(simulation);
            return simulation;
        } else if (this.config.simulation() != null && this.config.simulation().getExternalExe() != null) {
            ExternalMobsim simulation = new ExternalMobsim(this.scenarioData, this.events);
            simulation.setControlerIO(this.controlerIO);
            simulation.setIterationNumber(this.getIterationNumber());
            return simulation;
        } else if (this.config.controler().getMobsim() != null) {
            String mobsim = this.config.controler().getMobsim();
            MobsimFactory f = this.mobsimFactoryRegister.getInstance(mobsim);
            Mobsim simulation = f.createMobsim(this.getScenario(), this.getEvents());
            enrichSimulation(simulation);
            return simulation;
        } else {
            log.warn("Please specify which mobsim should be used in the configuration (see module 'controler', parameter 'mobsim'). Now trying to detect which mobsim to use from other parameters...");
            MobsimFactory mobsimFactory;
            if (config.getModule(QSimConfigGroup.GROUP_NAME) != null) {
                mobsimFactory = new QSimFactory();
            } else if (config.getModule("JDEQSim") != null) {
                mobsimFactory = new JDEQSimulationFactory();
            } else if (config.getModule(SimulationConfigGroup.GROUP_NAME) != null) {
                mobsimFactory = new QueueSimulationFactory();
            } else {
                log.warn("There is no configuration for a mobility simulation in the config. The Controler " + "uses the default `Simulation'.  Add a (possibly empty) `Simulation' module to your config file " + "to avoid this warning");
                config.addSimulationConfigGroup(new SimulationConfigGroup());
                mobsimFactory = new QueueSimulationFactory();
            }
            Mobsim simulation = mobsimFactory.createMobsim(this.getScenario(), this.getEvents());
            enrichSimulation(simulation);
            return simulation;
        }
    }

    private void enrichSimulation(final Mobsim simulation) {
        if (simulation instanceof ObservableMobsim) {
            for (MobsimListener l : this.getQueueSimulationListener()) {
                ((ObservableMobsim) simulation).addQueueSimulationListeners(l);
            }
        }
        if (simulation instanceof VisMobsim) {
            int itNumber = this.getIterationNumber();
            if (config.controler().getWriteSnapshotsInterval() != 0 && itNumber % config.controler().getWriteSnapshotsInterval() == 0) {
                SnapshotWriterManager manager = new SnapshotWriterManager(config);
                for (String snapshotFormat : this.config.controler().getSnapshotFormat()) {
                    SnapshotWriterFactory snapshotWriterFactory = this.snapshotWriterRegister.getInstance(snapshotFormat);
                    String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
                    String fileName = this.controlerIO.getIterationFilename(itNumber, baseFileName);
                    SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, this.scenarioData);
                    manager.addSnapshotWriter(snapshotWriter);
                }
                ((ObservableMobsim) simulation).addQueueSimulationListeners(manager);
            }
        }
        if (simulation instanceof QSim) {
            if (config.multiModal().isMultiModalSimulationEnabled()) {
                log.info("Using MultiModalMobsim...");
                QSim qSim = (QSim) simulation;
                MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, this.multiModalTravelTimeFactory);
                qSim.addMobsimEngine(multiModalEngine);
                qSim.addDepartureHandler(new MultiModalDepartureHandler(qSim, multiModalEngine, config.multiModal()));
            }
        }
    }

    protected void runMobSim() {
        Mobsim sim = getNewMobsim();
        sim.run();
    }

    /**
	 * Add a core ControlerListener to the Controler instance
	 * 
	 * @param l
	 */
    protected final void addCoreControlerListener(final ControlerListener l) {
        this.controlerListenerManager.addCoreControlerListener(l);
    }

    /**
	 * Add a ControlerListener to the Controler instance
	 * 
	 * @param l
	 */
    public final void addControlerListener(final ControlerListener l) {
        this.controlerListenerManager.addControlerListener(l);
    }

    /**
	 * Removes a ControlerListener from the Controler instance
	 * 
	 * @param l
	 */
    public final void removeControlerListener(final ControlerListener l) {
        this.controlerListenerManager.removeControlerListener(l);
    }

    /**
	 * Sets whether the Controler is allowed to overwrite files in the output
	 * directory or not. <br>
	 * When starting, the Controler can check that the output directory is empty
	 * or does not yet exist, so no files will be overwritten (default setting).
	 * While useful in a productive environment, this security feature may be
	 * interfering in test cases or while debugging. <br>
	 * <strong>Use this setting with caution, as it can result in data
	 * loss!</strong>
	 * 
	 * @param overwrite
	 *            whether files and directories should be overwritten (true) or
	 *            not (false)
	 */
    public final void setOverwriteFiles(final boolean overwrite) {
        this.overwriteFiles = overwrite;
    }

    /**
	 * Returns whether the Controler is currently allowed to overwrite files in
	 * the output directory.
	 * 
	 * @return true if the Controler is currently allowed to overwrite files in
	 *         the output directory, false if not.
	 */
    public final boolean getOverwriteFiles() {
        return this.overwriteFiles;
    }

    /**
	 * Sets in which iterations events should be written to a file. If set to
	 * <tt>1</tt>, the events will be written in every iteration. If set to
	 * <tt>2</tt>, the events are written every second iteration. If set to
	 * <tt>10</tt>, the events are written in every 10th iteration. To disable
	 * writing of events completely, set the interval to <tt>0</tt> (zero).
	 * 
	 * @param interval
	 *            in which iterations events should be written
	 */
    public final void setWriteEventsInterval(final int interval) {
        this.writeEventsInterval = interval;
    }

    public final int getWriteEventsInterval() {
        return this.writeEventsInterval;
    }

    /**
	 * Sets whether graphs showing some analyses should automatically be
	 * generated during the simulation. The generation of graphs usually takes a
	 * small amount of time that does not have any weight in big simulations,
	 * but add a significant overhead in smaller runs or in test cases where the
	 * graphical output is not even requested.
	 * 
	 * @param createGraphs
	 *            true if graphs showing analyses' output should be generated.
	 */
    public final void setCreateGraphs(final boolean createGraphs) {
        this.createGraphs = createGraphs;
    }

    /**
	 * @return true if analyses should create graphs showing there results.
	 */
    public final boolean getCreateGraphs() {
        return this.createGraphs;
    }

    /**
	 * @param dumpData
	 *            <code>true</code> if at the end of a run, plans, network,
	 *            config etc should be dumped to a file.
	 */
    public final void setDumpDataAtEnd(final boolean dumpData) {
        this.dumpDataAtEnd = dumpData;
    }

    public final PersonalizableTravelDisutility createTravelCostCalculator() {
        return this.travelCostCalculatorFactory.createTravelDisutility(this.travelTimeCalculator, this.config.planCalcScore());
    }

    public final PersonalizableTravelTime getTravelTimeCalculator() {
        return this.travelTimeCalculator;
    }

    /**
	 * Sets a new {@link org.matsim.core.scoring.ScoringFunctionFactory} to use.
	 * <strong>Note:</strong> This will reset all scores calculated so far! Only
	 * call this before any events are generated in an iteration.
	 * 
	 * @param factory
	 *            The new ScoringFunctionFactory to be used.
	 */
    public final void setScoringFunctionFactory(final ScoringFunctionFactory factory) {
        this.scoringFunctionFactory = factory;
    }

    /**
	 * @return the currently used
	 *         {@link org.matsim.core.scoring.ScoringFunctionFactory} for
	 *         scoring plans.
	 */
    public final ScoringFunctionFactory getScoringFunctionFactory() {
        return this.scoringFunctionFactory;
    }

    /**
	 * @return Returns the {@link org.matsim.core.replanning.StrategyManager}
	 *         used for the replanning of plans.
	 */
    public final StrategyManager getStrategyManager() {
        return this.strategyManager;
    }

    public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
        return this.leastCostPathCalculatorFactory;
    }

    public void setLeastCostPathCalculatorFactory(final LeastCostPathCalculatorFactory factory) {
        this.leastCostPathCalculatorFactory = factory;
    }

    /**
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the default (= the current from the last or current
	 *         iteration) travel costs and travel times. Only to be used by a
	 *         single thread, use multiple instances for multiple threads!
	 */
    public PlanAlgorithm createRoutingAlgorithm() {
        return createRoutingAlgorithm(this.createTravelCostCalculator(), this.getTravelTimeCalculator());
    }

    /**
	 * @param travelCosts
	 *            the travel costs to be used for the routing
	 * @param travelTimes
	 *            the travel times to be used for the routing
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the specified travelCosts and travelTimes. Only to
	 *         be used by a single thread, use multiple instances for multiple
	 *         threads!
	 */
    public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelDisutility travelCosts, final PersonalizableTravelTime travelTimes) {
        PlansCalcRoute plansCalcRoute = null;
        ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
        if (this.getScenario().getConfig().scenario().isUseRoadpricing() && (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.scenarioData.getRoadPricingScheme().getType()))) {
            plansCalcRoute = new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, this.scenarioData.getRoadPricingScheme());
            log.warn("As roadpricing with area toll is enabled a leg router for area tolls is used. Other features, " + "e.g. transit or multimodal simulation may not work as expected.");
        } else if (this.config.scenario().isUseTransit()) {
            plansCalcRoute = new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, this.config.transit(), this.transitRouterFactory.createTransitRouter(), this.scenarioData.getTransitSchedule());
            log.warn("As simulation of public transit is enabled a leg router for area tolls is used. Other features, " + "e.g. multimodal simulation, may not work as expected.");
        } else if (this.config.multiModal().isMultiModalSimulationEnabled()) {
            MultiModalTravelTime travelTime = multiModalTravelTimeFactory.createTravelTime();
            plansCalcRoute = new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTime, this.getLeastCostPathCalculatorFactory(), routeFactory);
            IntermodalLeastCostPathCalculator routeAlgo = (IntermodalLeastCostPathCalculator) this.getLeastCostPathCalculatorFactory().createPathCalculator(network, travelCosts, travelTime);
            MultiModalLegRouter multiModalLegHandler = new MultiModalLegRouter(this.network, travelTime, travelCosts, routeAlgo);
            plansCalcRoute.addLegHandler(TransportMode.car, multiModalLegHandler);
            for (String mode : CollectionUtils.stringToArray(this.config.multiModal().getSimulatedModes())) {
                plansCalcRoute.addLegHandler(mode, multiModalLegHandler);
            }
        } else {
            plansCalcRoute = new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory);
        }
        if (this.getScenario().getConfig().controler().isLinkToLinkRoutingEnabled()) {
            InvertedNetworkLegRouter invertedNetLegRouter = new InvertedNetworkLegRouter(this.getScenario(), this.getLeastCostPathCalculatorFactory(), this.getTravelDisutilityFactory(), travelTimes);
            plansCalcRoute.addLegHandler(TransportMode.car, invertedNetLegRouter);
            log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");
        }
        return plansCalcRoute;
    }

    public final int getFirstIteration() {
        return this.config.controler().getFirstIteration();
    }

    public final int getLastIteration() {
        return this.config.controler().getLastIteration();
    }

    public final Config getConfig() {
        return this.config;
    }

    public final ActivityFacilities getFacilities() {
        return this.scenarioData.getActivityFacilities();
    }

    public final Network getNetwork() {
        return this.network;
    }

    public final Population getPopulation() {
        return this.population;
    }

    public final EventsManager getEvents() {
        return this.events;
    }

    public final ScenarioImpl getScenario() {
        return this.scenarioData;
    }

    /**
	 * @return real-world traffic counts if available, <code>null</code> if no
	 *         data is available.
	 */
    public final Counts getCounts() {
        return this.counts;
    }

    /**
	 * @deprecated Do not use this, as it may not contain values in every
	 *             iteration
	 * @return
	 */
    @Deprecated
    public final CalcLinkStats getLinkStats() {
        return this.linkStats;
    }

    public CalcLegTimes getLegTimes() {
        return this.legTimes;
    }

    public VolumesAnalyzer getVolumes() {
        return this.volumes;
    }

    /**
	 * @return Returns the RoadPricing-ControlerListener, or null if no road
	 *         pricing is simulated.
	 */
    public final RoadPricing getRoadPricing() {
        return this.roadPricing;
    }

    /**
	 * @return Returns the scoreStats.
	 */
    public ScoreStats getScoreStats() {
        return this.scoreStats;
    }

    public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
        return this.facilityPenalties;
    }

    public static void main(final String[] args) {
        if ((args == null) || (args.length == 0)) {
            System.out.println("No argument given!");
            System.out.println("Usage: Controler config-file [dtd-file]");
            System.out.println();
        } else {
            final Controler controler = new Controler(args);
            controler.run();
        }
        System.exit(0);
    }

    public List<MobsimListener> getQueueSimulationListener() {
        return this.simulationListener;
    }

    public PlansScoring getPlansScoring() {
        return this.plansScoring;
    }

    public TravelTimeCalculatorFactory getTravelTimeCalculatorFactory() {
        return this.travelTimeCalculatorFactory;
    }

    public void setTravelTimeCalculatorFactory(final TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
        this.travelTimeCalculatorFactory = travelTimeCalculatorFactory;
    }

    public TravelDisutilityFactory getTravelDisutilityFactory() {
        return this.travelCostCalculatorFactory;
    }

    public void setTravelDisutilityFactory(final TravelDisutilityFactory travelCostCalculatorFactory) {
        this.travelCostCalculatorFactory = travelCostCalculatorFactory;
    }

    public MultiModalTravelTimeWrapperFactory getMultiModalTravelTimeWrapperFactory() {
        return this.multiModalTravelTimeFactory;
    }

    public ControlerIO getControlerIO() {
        return this.controlerIO;
    }

    /**
	 * @return the iteration number of the current iteration when the Controler
	 *         is iterating, null if the Controler is in the startup/shutdown
	 *         process
	 */
    public Integer getIterationNumber() {
        return this.iteration;
    }

    public MobsimFactory getMobsimFactory() {
        return this.mobsimFactory;
    }

    public void setMobsimFactory(final MobsimFactory mobsimFactory) {
        this.mobsimFactory = mobsimFactory;
    }

    /**
	 * Register a {@link MobsimFactory} with a given name.
	 * 
	 * @param mobsimName
	 * @param mobsimFactory
	 * 
	 * @see ControlerConfigGroup#getMobsim()
	 */
    public void addMobsimFactory(final String mobsimName, final MobsimFactory mobsimFactory) {
        this.mobsimFactoryRegister.register(mobsimName, mobsimFactory);
    }

    public void addSnapshotWriterFactory(final String snapshotWriterName, final SnapshotWriterFactory snapshotWriterFactory) {
        this.snapshotWriterRegister.register(snapshotWriterName, snapshotWriterFactory);
    }

    public SignalsControllerListenerFactory getSignalsControllerListenerFactory() {
        return this.signalsFactory;
    }

    public void setSignalsControllerListenerFactory(final SignalsControllerListenerFactory signalsFactory) {
        this.signalsFactory = signalsFactory;
    }

    public TransitRouterFactory getTransitRouterFactory() {
        return this.transitRouterFactory;
    }

    public void setTransitRouterFactory(final TransitRouterFactory transitRouterFactory) {
        this.transitRouterFactory = transitRouterFactory;
    }

    public int getWritePlansInterval() {
        return this.writePlansInterval;
    }
}
