package playground.southafrica.gauteng;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author nagel
 *
 */
public class GautengOwnController {

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

    private static final Logger log = Logger.getLogger(GautengOwnController.class);

    private CollectLogMessagesAppender collectLogMessagesAppender;

    private String dtdFileName;

    protected Throwable uncaughtException;

    private Config config;

    private Scenario scenarioData;

    private Network network;

    private Population population;

    private Thread shutdownHook = new Thread() {

        @Override
        public void run() {
            shutdown(true);
        }
    };

    private String outputPath;

    private ControlerIO controlerIO;

    private boolean overwriteFiles;

    private boolean dumpDataAtEnd;

    private EventsManager events;

    private TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();

    private TravelDisutilityFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();

    private TravelTimeCalculator travelTimeCalculator;

    private PersonalizableTravelDisutility travelCostCalculator;

    private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

    private ScoringFunctionFactory scoringFunctionFactory;

    private CalcLinkStats linkStats;

    private VolumesAnalyzer volumes;

    private CalcLegTimes legTimes;

    private StrategyManager strategyManager;

    private PlansScoring plansScoring;

    private RoadPricing roadPricing;

    public static void main(String[] args) {
        GautengOwnController gautengOwnController = new GautengOwnController();
        gautengOwnController.run();
    }

    GautengOwnController() {
        this.collectLogMessagesAppender = new CollectLogMessagesAppender();
        Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
        Gbl.printSystemInfo();
        Gbl.printBuildInfo();
        log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
        this.dtdFileName = null;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.warn("Getting uncaught Exception in Thread " + t.getName(), e);
                GautengOwnController.this.uncaughtException = e;
            }
        });
        this.config = ConfigUtils.createConfig();
        this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
        checkConfigConsistencyAndWriteToLog("Complete config dump after reading the config file:");
        this.scenarioData = ScenarioUtils.loadScenario(config);
        this.network = this.scenarioData.getNetwork();
        this.population = this.scenarioData.getPopulation();
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    void run() {
        init();
        log.error("yyyyyy check if we strictly need this infrastructure (core ctrl listeners?)");
        this.checkConfigConsistencyAndWriteToLog("Config dump before doIterations:");
        log.error("yyyyyy iterations not yet implemented.  Skipping ...");
        shutdown(false);
    }

    void init() {
        setUpOutputDir();
        initEvents();
        initLogging();
        setUp();
        loadCoreListeners();
        log.error("loadControlerListeners needs to be implemented.  Skipping ...");
    }

    /**
	 * in particular select if single cpu handler to use or parallel
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
            this.events = EventsUtils.createEventsManager();
        }
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
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
    protected void setUp() {
        this.travelTimeCalculator = this.travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
        this.travelCostCalculator = this.travelCostCalculatorFactory.createTravelDisutility(this.travelTimeCalculator, this.config.planCalcScore());
        this.events.addHandler(this.travelTimeCalculator);
        this.leastCostPathCalculatorFactory = new DijkstraFactory();
        this.linkStats = new CalcLinkStats(this.network);
        this.volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
        this.events.addHandler(this.volumes);
        this.legTimes = new CalcLegTimes();
        this.events.addHandler(this.legTimes);
        this.scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(this.config.planCalcScore(), this.getNetwork());
        this.strategyManager = new StrategyManager();
        throw new RuntimeException("this will not work without the above line.  aborting ...");
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
        this.addCoreControlerListener(new EventsHandling((EventsManagerImpl) this.events));
    }

    void shutdown(final boolean unexpected) {
        if (unexpected) {
            log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
        } else {
            log.info("S H U T D O W N   ---   start regular shutdown.");
        }
        if (this.uncaughtException != null) {
            log.warn("Shutdown probably caused by the following Exception.", this.uncaughtException);
        }
        log.error("check if we need the controler listener infrastructure");
        if (this.dumpDataAtEnd) {
            new PopulationWriter(this.population, this.network).write(this.controlerIO.getOutputFilename(FILENAME_POPULATION));
            new NetworkWriter(this.network).write(this.controlerIO.getOutputFilename(FILENAME_NETWORK));
            new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(FILENAME_CONFIG));
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
	 * Add a core ControlerListener to the Controler instance
	 * 
	 * @param l
	 */
    protected final void addCoreControlerListener(final ControlerListener l) {
        throw new RuntimeException("will not work without above line; aborting ...");
    }

    public Config getConfig() {
        return config;
    }

    public Scenario getScenario() {
        return scenarioData;
    }

    public Network getNetwork() {
        return network;
    }

    public Population getPopulation() {
        return population;
    }

    public final int getLastIteration() {
        return this.config.controler().getLastIteration();
    }
}
