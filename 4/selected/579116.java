package jeco.dmm.tools;

import Individuals.Individual;
import jeco.kernel.algorithm.moge.FitnessMO;
import Util.Constants;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jeco.dmm.lib.DynamicMemoryManager;
import jeco.dmm.lib.Fitness;
import jeco.dmm.lib.Simulator;
import jeco.dmm.lib.allocator.Kingsley;
import jeco.dmm.util.ManagersCreator;
import jeco.dmm.util.ProfilingReport;
import jeco.kernel.algorithm.ge.GrammaticalEvolutionaryAlgorithm;
import jeco.kernel.algorithm.moge.GrammaticalMultiObjectiveEvolutionaryAlgorithm;
import jeco.kernel.util.LoggerFormatter;

/**
 *
 * @author José Luis Risco-Martín, J. Manuel Colmenar
 */
public class DmmExplorerMo {

    private static final Logger logger = Logger.getLogger(DmmExplorerMo.class.getName());

    protected ProfilingReport profile;

    public static enum METHOD {

        KINGSLEY, GE, MOGE, GE_Front
    }

    ;

    /**
     * DMM Method to be explored
     */
    protected METHOD method;

    protected String metrics = null;

    protected String map = null;

    protected String grammar = null;

    protected String generations = null;

    protected String population = null;

    protected String optimum = null;

    private static String DEFAULT_GENERATIONS = "100";

    private static String DEFAULT_POPULATION = "60";

    private static String DEFAULT_OPTIMUM = "0.0";

    public DmmExplorerMo(String pathToProfile, METHOD method, String grammar, String metrics, String map, String accesses) {
        profile = new ProfilingReport(pathToProfile);
        try {
            profile.load();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "Profile: file not found", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Profile: IO error", ex);
        }
        this.method = method;
        this.grammar = grammar;
        this.metrics = metrics;
        this.map = map;
    }

    public DmmExplorerMo(String pathToProfile, METHOD method, String grammar, String metrics, String map, String accesses, String generations, String indiv, String optimum) {
        this(pathToProfile, method, grammar, metrics, map, accesses);
        if (generations == null) {
            this.generations = DEFAULT_GENERATIONS;
        } else {
            this.generations = generations;
        }
        if (indiv == null) {
            this.population = DEFAULT_POPULATION;
        } else {
            this.population = indiv;
        }
        if (optimum == null) {
            this.optimum = DEFAULT_OPTIMUM;
        } else {
            this.optimum = optimum;
        }
    }

    public void explore() throws FileNotFoundException, IOException {
        DynamicMemoryManager manager = null;
        List<Simulator> simulator = new ArrayList<Simulator>();
        double startTime = System.currentTimeMillis();
        if (method.equals(METHOD.KINGSLEY)) {
            manager = new DynamicMemoryManager(new Kingsley());
            simulator.add(new Simulator(profile, manager));
        } else if ((method.equals(METHOD.GE)) || (method.equals(METHOD.GE_Front))) {
            Fitness.evaluated.clear();
            Fitness.profilingReport = profile;
            GrammaticalEvolutionaryAlgorithm optimizer = new GrammaticalEvolutionaryAlgorithm(grammar, Fitness.class.getName());
            optimizer.setProperty(Constants.GENERATION, this.generations);
            optimizer.setProperty(Constants.POPULATION_SIZE, this.population);
            optimizer.setProperty(Constants.MAX_DEPTH, "10");
            optimizer.setProperty(Constants.MUTATION_PROBABILITY, "0.02");
            optimizer.setProperty(Constants.CROSSOVER_PROBABILITY, "0.8");
            logger.info("Initializing gramatical evolution...\n");
            optimizer.initialize();
            logger.info("Executing gramatical evolution (mono-objective optimization) ...\n");
            optimizer.execute();
            logger.info("done.\n");
            if (method.equals(METHOD.GE)) {
                logger.info("Obtaining best individual...\n");
                manager = ManagersCreator.buildManagerFromPhenotype(profile, optimizer.getBestFitness().getIndividual().getPhenotype());
                simulator.add(new Simulator(profile, manager));
            } else {
                logger.info("Obtaining non dominated solutions...\n");
                FitnessMO.evaluated.clear();
                FitnessMO.profilingReport = profile;
                List<Individual> paretoFront = optimizer.getNonDominated();
                Iterator<Individual> iterator = paretoFront.iterator();
                while (iterator.hasNext()) {
                    Individual indiv = iterator.next();
                    manager = ManagersCreator.buildManagerFromPhenotype(profile, indiv.getPhenotype());
                    simulator.add(new Simulator(profile, manager));
                    logger.info(indiv.getPhenotype().getString());
                }
            }
        } else if (method.equals(METHOD.MOGE)) {
            FitnessMO.evaluated.clear();
            FitnessMO.profilingReport = profile;
            GrammaticalMultiObjectiveEvolutionaryAlgorithm optimizer = new GrammaticalMultiObjectiveEvolutionaryAlgorithm(grammar, FitnessMO.class.getName());
            optimizer.setProperty(Constants.GENERATION, this.generations);
            optimizer.setProperty(Constants.POPULATION_SIZE, this.population);
            optimizer.setProperty(Constants.MAX_DEPTH, "10");
            optimizer.setProperty(Constants.MUTATION_PROBABILITY, "0.02");
            optimizer.setProperty(Constants.CROSSOVER_PROBABILITY, "0.8");
            logger.info("Initializing multiobjective gramatical evolution...\n");
            optimizer.initialize();
            logger.info("Executing multiobjective gramatical evolution...\n");
            optimizer.execute();
            logger.info("done.\n");
            List<Individual> paretoFront = optimizer.getParetoFront();
            Iterator<Individual> iterator = paretoFront.iterator();
            while (iterator.hasNext()) {
                Individual indiv = iterator.next();
                manager = ManagersCreator.buildManagerFromPhenotype(profile, indiv.getPhenotype());
                simulator.add(new Simulator(profile, manager));
                logger.info(indiv.getPhenotype().getString());
            }
        }
        Iterator<Simulator> iterSimulator = simulator.iterator();
        int i = 0;
        BufferedWriter moWriter = null;
        if (method.equals(METHOD.MOGE)) {
            moWriter = new BufferedWriter(new FileWriter(new File(metrics + "MO.mtr")));
        }
        if (method.equals(METHOD.GE_Front)) {
            moWriter = new BufferedWriter(new FileWriter(new File(metrics + "GEfront.mtr")));
        }
        while (iterSimulator.hasNext()) {
            Simulator sim = iterSimulator.next();
            sim.initialize();
            sim.simulate();
            String mapAsString = sim.drawAllocatorMap();
            String reportAsString = sim.getMetrics().report();
            if (method.equals(METHOD.GE)) {
                logger.info(mapAsString);
                logger.info(reportAsString);
            }
            BufferedWriter writer = null;
            if (map != null) {
                logger.info("Saving map ...");
                writer = new BufferedWriter(new FileWriter(new File(map + "." + i)));
                writer.write(mapAsString);
                writer.flush();
                writer.close();
            }
            if (metrics != null) {
                logger.info("Saving metrics ...");
                writer = new BufferedWriter(new FileWriter(new File(metrics + "." + i)));
                writer.write(reportAsString);
                writer.flush();
                writer.close();
            }
            if (moWriter != null) {
                String time = Double.toString(sim.getMetrics().getExecutionTime());
                String mem = Double.toString(sim.getMetrics().getMemoryUsage());
                String energy = Double.toString(sim.getMetrics().getEnergy());
                logger.info(time + " " + mem + " " + energy);
                moWriter.write(time + "; " + mem + "; " + energy + "\n");
            }
            if (method.equals(METHOD.GE)) {
                String time = Double.toString(sim.getMetrics().getExecutionTime());
                String mem = Double.toString(sim.getMetrics().getMemoryUsage());
                String energy = Double.toString(sim.getMetrics().getEnergy());
                logger.info(time + "; " + mem + "; " + energy);
            }
            i++;
        }
        if (moWriter != null) {
            moWriter.flush();
            moWriter.close();
        }
        logger.info("Solutions: " + simulator.size());
    }

    public static void main(String[] args) {
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (int index = 0; index < handlers.length; index++) {
            handlers[index].setLevel(Level.INFO);
            handlers[index].setFormatter(LoggerFormatter.formatter);
        }
        if ((args.length < 6) || ((args.length > 6) && (args.length % 2 != 0))) {
            args = new String[8];
            String bench = "cfrac";
            args[0] = "-profile";
            args[1] = "test" + File.separator + bench + ".mem";
            args[2] = "-method";
            args[3] = "MOGE";
            args[4] = "-grammar";
            args[5] = "test" + File.separator + bench + ".bnf";
            args[6] = "-omode";
            args[7] = "AUTO";
            System.out.println("\nUsage:");
            System.out.println("Explorer -profile <profile> -method <method> -grammar <grammar> [-omode <outmode=MANUAL> -ometrics <metrics> -omap <map> -oaccesses <accesses> -gen <NumberOfGenerations> -indiv <NumberOfIndividuals> -opt <GlobalOptimum>]");
            System.out.println("\nExample: Explorer -profile test" + File.separator + "vdrift.mem -method KINGSLEY -grammar test" + File.separator + "vdrift.bnf");
            System.out.println("where:");
            System.out.println("<profile>: Relative path to the profiling report");
            System.out.println("<method>=KINGSLEY: Evaluates the application using the KINGSLEY DMM");
            System.out.println("<method>=GE: Evaluates the application using Grammatical Evolution (mono-objective)");
            System.out.println("<method>=GE_Front: Evaluates the application using Grammatical Evolution (mono-objective). Returns non dominated solutions.");
            System.out.println("<method>=MOGE: Evaluates the application using Multi Ojective optimization over Grammatical Evolution");
            System.out.println("<grammar>: Relative path to the grammar file");
            System.out.println("<outmode>=MANUAL: Metrics are saved in the files specified by user (if any)");
            System.out.println("<outmode>=AUTO: All metrics are saved and output file names are named automatically (slowest).");
            System.out.println("<metrics>=File path where the main metrics are saved.");
            System.out.println("<map>=File path where a draw of the DMM is saved.");
            System.out.println("<accesses>=File path where the frecuency of (read/write) accesses is saved.\n");
            System.out.println("<NumberOfGenerations>=Number of generations for GE (default is 100)");
            System.out.println("<NumberOfIndividuals>=Number of individuals for GE (default is 60)");
            System.out.println("<GlobalOptimum>=GE stops when GlobalOptimum is reached (default is 0.0)\n");
            return;
        }
        String profile = null, method = null, grammar = null, outmode = "MANUAL", metrics = null, map = null, accesses = null;
        String generations = null, indiv = null, optimum = null;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-profile")) {
                profile = args[i + 1];
            } else if (args[i].equals("-method")) {
                method = args[i + 1];
            } else if (args[i].equals("-grammar")) {
                grammar = args[i + 1];
            } else if (args[i].equals("-omode")) {
                outmode = args[i + 1];
            } else if (args[i].equals("-ometrics")) {
                metrics = args[i + 1];
            } else if (args[i].equals("-omap")) {
                map = args[i + 1];
            } else if (args[i].equals("-oaccesses")) {
                accesses = args[i + 1];
            } else if (args[i].equals("-gen")) {
                generations = args[i + 1];
            } else if (args[i].equals("-indiv")) {
                indiv = args[i + 1];
            } else if (args[i].equals("-opt")) {
                optimum = args[i + 1];
            }
        }
        if (outmode.equals("AUTO")) {
            String baseName = null;
            int pos = profile.lastIndexOf(".");
            if (pos < 0) baseName = profile; else baseName = profile.substring(0, pos);
            baseName = baseName + "." + method.toString();
            metrics = baseName + ".mtr";
            map = baseName + ".map";
            accesses = baseName + ".acc";
        }
        DmmExplorerMo explorer = null;
        if (method.equals("KINGSLEY")) {
            explorer = new DmmExplorerMo(profile, DmmExplorerMo.METHOD.KINGSLEY, grammar, metrics, map, accesses);
        } else if (method.equals("GE")) {
            explorer = new DmmExplorerMo(profile, DmmExplorerMo.METHOD.GE, grammar, metrics, map, accesses, generations, indiv, optimum);
        } else if (method.equals("GE_Front")) {
            explorer = new DmmExplorerMo(profile, DmmExplorerMo.METHOD.GE_Front, grammar, metrics, map, accesses, generations, indiv, optimum);
        } else if (method.equals("MOGE")) {
            explorer = new DmmExplorerMo(profile, DmmExplorerMo.METHOD.MOGE, grammar, metrics, map, accesses, generations, indiv, optimum);
        }
        try {
            explorer.explore();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "File not found exception", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IO Exception", ex);
        }
    }
}
