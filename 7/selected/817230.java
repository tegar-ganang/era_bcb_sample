package shotgun;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.io.File;

/**
 * Main class of the Shotgun project.
 *
 * Any method of this class should operate on Model that abstracts either a set of predictions
 * or an N-class ensemble.
**/
public class ModelSelection {

    private Model[] models;

    private int numModels;

    private Model ensemble;

    private Model bestEnsemble;

    private ModelCounts modelCounts;

    private double bagPercent;

    private static Model[] modelLibrary;

    private static Random r;

    private static boolean weightDecay = false;

    private static boolean bootstrap = false;

    private static boolean updateBest = false;

    private static boolean writeCount = false;

    private static boolean reuse = false;

    private static boolean report = true;

    private static boolean allSets = true;

    private static boolean verbose = false;

    private static boolean stepwiseBagging = false;

    private static boolean stepwiseSortInitAll = false;

    private static boolean incrementalReporting = false;

    /**
   * Default Constructor just initializes main variables
   *
   * @param bagPercent the p value for bagging
   */
    public ModelSelection(double bp) {
        bagPercent = bp;
        ensemble = new Model();
        populateModelSet(true);
        if (writeCount) {
            Model[] libAlias = stepwiseBagging ? modelLibrary : models;
            modelCounts = new ModelCounts();
            for (int i = 0; i < libAlias.length; ++i) {
                modelCounts.add(libAlias[i].getName());
            }
        }
    }

    /**
   * This function is responsible for selecting a random subset of
   * the models in the modelLibrary and assigning them to the currently
   * used set of models
   **/
    public void populateModelSet(boolean initialModelSet) {
        numModels = modelLibrary.length;
        if ((bagPercent != 1.0) && !(stepwiseSortInitAll && initialModelSet)) {
            numModels *= bagPercent;
            for (int j = 0; j < modelLibrary.length; j++) {
                int swap = r.nextInt(modelLibrary.length - j) + j;
                Model temp = modelLibrary[j];
                modelLibrary[j] = modelLibrary[swap];
                modelLibrary[swap] = temp;
            }
        }
        models = new Model[numModels];
        for (int i = 0; i < numModels; i++) models[i] = modelLibrary[i];
        Arrays.sort(models);
    }

    /**
   * Initialize the environment and performs a couple of sanity checks.
   *
   * @param predictionsFolder is a folder where to find all the predictions.
   * @param trainFolder is the name of the folder we are hillclimbing on.
   * @param modelFilter a Model Filter to apply when listing files
   * @param loader a loader to use for loading models from disk
   * @param datathresh Set threshold based on percent positive in hillclimb set?
   * @param baseline is model selection computing baseline performance?
  **/
    public static void init(String predictionsFolder, String trainFolder, ModelFilter modelFilter, LibraryLoader loader, boolean datathresh, boolean baseline) {
        File[] folder = new File(predictionsFolder).listFiles();
        int numSets = folder.length;
        int trainIndex = -1;
        String[] testName = new String[numSets];
        File[][] modelFiles = new File[numSets][];
        File[] targets = new File[numSets];
        int[] count = new int[numSets];
        ModelFilter targetFilter = new ModelFilter();
        targetFilter.setUseFilter(new String[] { "targets" });
        modelFilter.appendIgnoreFilter(new String[] { "targets" });
        for (int i = 0; i < numSets; ++i) {
            testName[i] = folder[i].getName();
            if (testName[i].equals(trainFolder)) trainIndex = i;
        }
        if (trainIndex == -1) {
            System.out.println("Error : could not find the train folder.");
            System.exit(0);
        }
        for (int i = 0; i < numSets; ++i) {
            File[] temp = folder[i].listFiles(targetFilter);
            if (temp == null || temp.length <= 0) {
                System.out.println("Error: folder " + testName[i] + " does not contain targets file.");
                System.exit(0);
            }
            if (temp.length > 1) {
                System.out.println("Error: folder " + testName[i] + " contains multiple files with 'targets' string.");
                System.exit(0);
            }
            if (LibraryLoader.getModelName(temp[0]).equals("targets")) {
                targets[i] = temp[0];
            } else {
                System.out.println("Error: folder " + testName[i] + " does not contain 'targets' file.");
                System.exit(0);
            }
        }
        for (int i = 0; i < numSets; ++i) {
            modelFiles[i] = folder[i].listFiles(modelFilter);
            if (modelFiles[i] == null) {
                System.out.println("Error : Predictions folder should contain subfolders with models.");
                System.exit(0);
            }
            if (modelFiles[i].length <= 0 && !baseline) {
                System.out.println("Error : No available models in library folder " + folder[i].getName());
                System.exit(0);
            }
            count[i] = modelFiles[i].length;
            if (count[i] != count[0]) {
                System.out.println("Error : predictions folder do not have the same number of models: " + testName[i] + " and " + testName[0]);
                System.exit(0);
            }
            Arrays.sort(modelFiles[i]);
        }
        for (int i = 0; i < numSets; ++i) {
            String extension = folder[i].getName();
            verifyExtension(targets[i], extension);
            for (int j = 0; j < count[i]; ++j) {
                verifyExtension(modelFiles[i][j], extension);
            }
        }
        Model.setTargets(targets, testName, trainIndex, datathresh);
        modelLibrary = loader.load(modelFiles);
    }

    private static void verifyExtension(File f, String extension) {
        String fname = f.getName();
        String fExt = fname.substring(fname.lastIndexOf('.') + 1);
        if (!extension.equals(fExt)) {
            System.out.println("Error: " + fname + "'s extension does not match folder " + extension);
            System.exit(0);
        }
    }

    /**
   * Perform backward elimination on models.
  **/
    public void backwardElimination(int maxIterations) {
        for (int i = 0; i < numModels; i++) addToEnsemble(models[i]);
        if (report) ensemble.report(models[numModels - 1].getName(), allSets);
        updateBestModel();
        for (int iteration = 1; iteration < maxIterations; iteration++) {
            if (stepwiseBagging) populateModelSet(false);
            int bestIndex = 0;
            double currentPerf, bestPerf = 1;
            for (int i = 0; i < numModels; i++) {
                currentPerf = ensemble.trySub(models[i]);
                if (currentPerf <= bestPerf) {
                    bestPerf = currentPerf;
                    bestIndex = i;
                }
            }
            removeFromEnsemble(models[bestIndex]);
            if (report) ensemble.report(models[bestIndex].getName(), allSets);
            updateBestModel();
            remove(bestIndex);
        }
    }

    /**
   * Perform forward selection on models.
   *
   * @param replacement Set to true if we allow ourself to reselect a model.
   * @param maxIterations The maximum number of iterations.
  **/
    public void forwardSelection(boolean replacement, int maxIterations) {
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            if (stepwiseBagging) populateModelSet(false);
            int bestIndex = 0;
            double currentPerf, bestPerf = 1;
            for (int i = 0; i < numModels; i++) {
                currentPerf = ensemble.tryAdd(models[i]);
                if (currentPerf < bestPerf) {
                    bestPerf = currentPerf;
                    bestIndex = i;
                }
            }
            addToEnsemble(models[bestIndex]);
            if (report) ensemble.report(models[bestIndex].getName(), allSets);
            updateBestModel();
            if (reuse) add();
            if (!replacement) remove(bestIndex);
        }
    }

    /**
   * Add models to the ensemble in order of best performance.
   **/
    public void sort(int maxIterations) {
        for (int i = 0; i < maxIterations; i++) {
            addToEnsemble(models[i]);
            if (report) ensemble.report(models[i].getName(), allSets);
        }
        updateBestModel();
    }

    /**
   * Find the shotgun set with optimal performance using sort and then apply
   * forward selection with replacement on this set.
   *
   * @param maxIterations The maximum number of iterations.
   **/
    public void sortAndSelect(int maxIterations) {
        Model ens = new Model();
        double currentPerf, bestPerf = 1;
        int bestIter = 1;
        for (int i = 0; i < numModels; i++) {
            ens.addModel(models[i], false);
            currentPerf = ens.getLoss();
            if (currentPerf < bestPerf) {
                bestPerf = currentPerf;
                bestIter = i + 1;
            }
        }
        sort(bestIter);
        forwardSelection(true, maxIterations);
    }

    /**
   * Do bayesian averaging of all the models in the ensamble
   **/
    public void bayesianAveraging() {
        double weights[] = new double[numModels];
        double maxweight = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numModels; i++) {
            weights[i] = models[i].getLogLikelihood();
            if (weights[i] > maxweight) {
                maxweight = weights[i];
            }
        }
        double weightTotal = 0.0;
        for (int i = 0; i < numModels; ++i) {
            weights[i] = Math.exp(weights[i] - maxweight);
            weightTotal += weights[i];
        }
        if (weightTotal <= 0.0) {
            System.out.println("Error: zero total weight for all models");
            System.exit(1);
        }
        for (int i = 0; i < numModels; ++i) {
            if (weights[i] <= 0.0) continue;
            ensemble.addModel(models[i], allSets, weights[i]);
            if (report) {
                ensemble.report(models[i].getName() + " " + weights[i], allSets);
            }
        }
        updateBestModel();
    }

    /**
   * Report the performance for the baseline model.
   */
    public void reportBaseline() {
        Model baseline = Model.getBaseline();
        if (report) baseline.report(baseline.getName(), allSets);
        updateBestModel();
    }

    /**
   * Add to the ensemble the models in increasing order of increasing performance
   * on the sorted models.
  **/
    public void greatestIncrease(int maxIterations) {
        double[] increase = new double[numModels];
        int[] id = new int[numModels];
        Model md = new Model();
        double oldPerf = 1;
        for (int i = 0; i < numModels; i++) {
            md.addModel(models[i], false);
            increase[i] = oldPerf - md.getLoss();
            id[i] = i;
            oldPerf = md.getLoss();
        }
        for (int i = 0; i < numModels; i++) {
            for (int j = 0; j < numModels - 1 - i; j++) {
                if (increase[j] < increase[j + 1]) {
                    double increasetemp = increase[j];
                    int temp = id[j];
                    increase[j] = increase[j + 1];
                    id[j] = id[j + 1];
                    increase[j + 1] = increasetemp;
                    id[j + 1] = temp;
                }
            }
        }
        for (int i = 0; i < maxIterations; i++) {
            addToEnsemble(models[id[i]]);
            if (report) ensemble.report(models[id[i]].getName(), allSets);
            updateBestModel();
        }
    }

    /**
   * Print the performance of each individual model.
  **/
    public void eachModel(int maxIterations) {
        for (int i = 0; i < maxIterations; i++) {
            if (report) models[i].report(models[i].getName(), allSets);
        }
        updateBestModel();
    }

    /**
   * Stores information about the current model if it is the best model so far
   */
    private void updateBestModel() {
        if (updateBest) {
            if (bestEnsemble == null || ensemble.compareTo(this.bestEnsemble) < 0) {
                this.bestEnsemble = ensemble.copy();
                if (writeCount) this.modelCounts.apply();
            }
        }
    }

    /**
   * Add a model to the ensemble.
   **/
    private void addToEnsemble(Model model) {
        ensemble.addModel(model, allSets);
        if (writeCount) modelCounts.increment(model.getName());
        if (weightDecay) Predictions.decay(); else if (bootstrap) Predictions.updateSeed();
    }

    /**
   * Remove a model from the ensemble.
   **/
    private void removeFromEnsemble(Model model) {
        ensemble.subModel(model, allSets);
        if (writeCount) modelCounts.decrement(model.getName());
        if (weightDecay) Predictions.decay(); else if (bootstrap) Predictions.updateSeed();
    }

    private void remove(int index) {
        numModels--;
        for (int i = index; i < numModels; i++) models[i] = models[i + 1];
    }

    private void add() {
        Model[] newLibrary = new Model[numModels + 1];
        for (int i = 0; i < numModels; i++) newLibrary[i] = models[i];
        newLibrary[numModels++] = ensemble.copy();
        newLibrary[numModels - 1].setName("ensemble" + ensemble.getNumModels());
        models = newLibrary;
    }

    public static double pickP(int maxIterations, int folds, int numBags, double inc) {
        double bestp = 1;
        double bestPerf = 1;
        report = false;
        allSets = false;
        boolean saveCount = writeCount;
        writeCount = true;
        for (double p = inc; p <= 1; p += inc) {
            Model.setFileWriters("" + p, false, allSets);
            double sum = 0;
            if (p > 1 - inc) {
                numBags = 1;
                p = 1;
            }
            for (int i = 0; i < numBags; i++) {
                for (int j = 0; j < folds; j++) {
                    Model.setFolds(folds, j, true);
                    ModelSelection modelSelection = new ModelSelection(p);
                    for (int k = 0; k < modelSelection.models.length; k++) modelSelection.models[k].reload();
                    modelSelection.sortAndSelect(maxIterations);
                    Model.setFolds(folds, j, false);
                    for (int k = 0; k < modelSelection.models.length; k++) modelSelection.models[k].reload();
                    Model test = new Model();
                    for (int k = 0; k < modelSelection.models.length; k++) {
                        test.addModel(modelSelection.models[k], false, modelSelection.modelCounts.getCount(modelSelection.models[k].getName()));
                    }
                    test.report("bag" + i + " fold" + j + " p" + p, allSets);
                    sum += test.getLoss();
                    System.out.println("Bag " + i + " Fold " + j + " " + test.getLoss());
                }
            }
            double loss = sum / (numBags * folds);
            System.out.println(p + " " + loss);
            if (loss < bestPerf) {
                bestp = p;
                bestPerf = loss;
            }
        }
        Model.setFolds(1);
        for (int i = 0; i < modelLibrary.length; i++) modelLibrary[i].reload();
        report = true;
        allSets = true;
        writeCount = saveCount;
        return bestp;
    }

    /**
   * Pre: init() called with library loader that sorted models in decreasing order
   */
    private static double prune(int maxIterations) {
        boolean oldAllSets = allSets;
        boolean oldReport = report;
        boolean oldWriteCount = writeCount;
        boolean oldUpdateBest = updateBest;
        Model[] fullLibrary = modelLibrary;
        allSets = false;
        report = false;
        writeCount = false;
        updateBest = true;
        final int STEPSIZE = 5;
        final int NUMSTEPS = 100 / STEPSIZE;
        double[] scores = new double[NUMSTEPS];
        for (int step = 0; step < NUMSTEPS; ++step) {
            double percent = ((double) ((step + 1) * 5)) / 100.0;
            if (verbose) {
                System.out.println("trying ensemble selection with top " + percent + " of models...");
            }
            int librarySize = (int) (percent * fullLibrary.length);
            modelLibrary = new Model[librarySize];
            for (int i = 0; i < librarySize; ++i) {
                modelLibrary[i] = fullLibrary[i];
            }
            if (report) Model.setFileWriters("prune" + percent, false, allSets);
            ModelSelection selector = new ModelSelection(1.0);
            selector.sortAndSelect(maxIterations);
            scores[step] = selector.bestEnsemble.getLoss();
        }
        double maxDelta = 0.0;
        int bestStep = 0;
        double prevScore = scores[0];
        for (int step = 0; step < NUMSTEPS; ++step) {
            double currScore = scores[step];
            double delta = prevScore - currScore;
            if (delta > maxDelta) {
                bestStep = step;
                maxDelta = delta;
            }
            System.err.println(step + ": loss=" + currScore + ", " + delta);
            prevScore = currScore;
        }
        System.err.println("best: step " + bestStep);
        double bestPercent = (bestStep + 1) * 5.0 / 100.0;
        int size = (int) (bestPercent * fullLibrary.length);
        modelLibrary = new Model[size];
        for (int i = 0; i < size; ++i) {
            modelLibrary[i] = fullLibrary[i];
        }
        allSets = oldAllSets;
        report = oldReport;
        writeCount = oldWriteCount;
        updateBest = oldUpdateBest;
        return bestPercent;
    }

    /**
   * Print the usage of shotgun
   **/
    public static void helpMsg() {
        System.out.println("=============================================================================");
        System.out.println("Shotgun V2.1: Extreme Ensemble Selection Project");
        System.out.println();
        System.out.println("coded by Alex Ksikes, ak107@cornell.edu");
        System.out.println("additional coding by:");
        System.out.println("Geoff Crew, gc97@cornell.edu");
        System.out.println("Alex Niculescu, alexn@cornell.edu");
        System.out.println("Rich Caruana, caruana@cornell.edu");
        System.out.println("Art Munson, mmunson@cs.cornell.edu");
        System.out.println("=============================================================================");
        System.out.println("Usage: ");
        System.out.println("  java -jar shotgun?.jar [options] pred_folder train_name");
        System.out.println();
        System.out.println("Command options: (default -sfr number of models)");
        System.out.println("  -s [int]     -> sort selection");
        System.out.println("  -g [int]     -> greatest increase");
        System.out.println("  -b [int]     -> backward elimination");
        System.out.println("  -f [int]     -> forward selection");
        System.out.println("  -fr [int]    -> forward selection with replacement");
        System.out.println("  -sfr [int]   -> sort selection and procced with fr");
        System.out.println("  -bayes       -> do bayesian averaging");
        System.out.println("  -bm str      -> output performance of best model with name containing str");
        System.out.println("  -x [int]     -> output performance of each model");
        System.out.println("  -baseline    -> output baseline performance (predict mean for all data)");
        System.out.println();
        System.out.println("Performance options: (default -rms)");
        System.out.println("  -acc                 -> accuracy");
        System.out.println("  -rms                 -> root mean square error");
        System.out.println("  -roc                 -> roc area");
        System.out.println("  -all [0..1]          -> weighted combination of acc, rms, and roc");
        System.out.println("  -bep                 -> break even point");
        System.out.println("  -pre                 -> precision");
        System.out.println("  -rec                 -> recall");
        System.out.println("  -fsc                 -> f score");
        System.out.println("  -apr                 -> average precition");
        System.out.println("  -lft                 -> lift [see notes below]");
        System.out.println("  -cst a b c d         -> cost");
        System.out.println("  -nrm float           -> norm");
        System.out.println("  -mxe                 -> mean cross entropy");
        System.out.println("  -slq binwidth seed n -> mean SLAC Q-score after n trials");
        System.out.println("  -ca1                 -> mean absolute probability calibration error with 19 sized bins");
        System.out.println("  -ca2 n               -> mean absolute probability calibration error with n sized bins");
        System.out.println("  -ca3 n               -> average of ca1 and ca2 methods");
        System.out.println("  -ca4 n               -> (9*ca2+rms)/10");
        System.out.println("  -ca5 n               -> mean squared probability calibration error with n sized bins");
        System.out.println("  -ca6 n               -> (ca2+rms)/2");
        System.out.println("  -custom name         -> optimize to the named custom metric");
        System.out.println();
        System.out.println("Other options:");
        System.out.println("  -d   float               -> weight decay");
        System.out.println("  -bsp numbsp numpts seed  -> bootstrappping");
        System.out.println("  -bag bags percent seed   -> bagged models");
        System.out.println("  -stepwise                -> resample models at every hillclimbing step.");
        System.out.println("                              Only meaningful if -bag option is used.");
        System.out.println("  -stepwise-sort-all       -> same as stepwise, except sort initialization");
        System.out.println("                              with all models in library");
        System.out.println("  -ir                      -> incrementally report perf stats after each bag ");
        System.out.println("                              is added.  useful for plots accross # bags used.");
        System.out.println("                              Only meaningful if -bag option is used.");
        System.out.println("  -sbset sbset_percent [top | random seed | external file]");
        System.out.println("                           -> Use only a subset of the models.  Three modes are");
        System.out.println("                              supported:");
        System.out.println("                                top => use top % of models based on performance");
        System.out.println("                                random => use random subset; provide random seed");
        System.out.println("                                external => use top % of models listed in file");
        System.out.println("                                  (one model name per line in file)");
        System.out.println("  -prune                   -> prune model library before hillclimbing");
        System.out.println("  -o   string              -> optional output name");
        System.out.println("  -t   [float|data]        -> set threshold to float or determine from training data");
        System.out.println("  -p   [0..100]            -> percent of data to predict one");
        System.out.println("  -ignore { sub1 sub2... } -> ignore models with any of given substrings");
        System.out.println("  -use { sub1 sub2 ... }   -> use models with any of given substrings");
        System.out.println("  -wp                      -> write the predictions of the best ensemble");
        System.out.println("  -r                       -> output counts for each model");
        System.out.println("  -v                       -> output intermediate steps to screen");
        System.out.println("  -l                       -> output performance in terms of loss");
        System.out.println("  -reuse                   -> allow ensembles to be used as models");
        System.out.println("  -cv folds bags inc seed  -> do cross validation");
        System.out.println("  -autothresh              -> set threshold automatically");
        System.out.println("  -addmetrics c \"arg\"    -> Add custom-defined metrics defined by class c w/");
        System.out.println("                              argument string.");
        System.out.println("  -?                       -> this help message");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  pred_folder -> path of the folder containing folders of predictions");
        System.out.println("  train_name  -> name of the folder to train shotgun on");
        System.out.println();
        System.out.println("Only one Performance option and one Command option can be chosen.");
        System.out.println();
        System.out.println("Custom-defined Metrics:");
        System.out.println("  User defined metrics can be added using -addmetrics. The flag can be ");
        System.out.println("  specified multiple times, each time giving a different class name.  All");
        System.out.println("  custom metric classes must implement the interface ");
        System.out.println("    shotgun.metrics.MetricBundle");
        System.out.println("  One argument string must be specified following the class name and should be");
        System.out.println("  quoted.  For example,");
        System.out.println("    MyMetric \"-m foobar -a sna -b fu\"");
        System.out.println("  This string is passed to the init() method of the class. If no such");
        System.out.println("  string is needed then use \"\" following the class name.");
        System.out.println("  Bootstrapping will not work with custom metrics that rely on identifying the");
        System.out.println("  test set to measure performance. Cross-validation similarly will not work.");
        System.out.println();
        System.out.println("Lift:");
        System.out.println("  You (almost) never want to optimize -lft without also setting -p parameter.");
        System.out.println("  First, there is the danger of falling into a degenerate case where performance");
        System.out.println("  looks very good, but the model / ensemble is very stupidly just predicting");
        System.out.println("  0 for every instance where it is not 100% certain it is positive.  Second,");
        System.out.println("  the lift metric doesn't make much sense without forcing the model / ensemble");
        System.out.println("  to predict p% of the data as positive.\n");
        System.out.println("  Unfortunately, -p globally impacts many builtin metrics. You can measure lift");
        System.out.println("  without impacting the other metrics by using the custom Lift metric; e.g.");
        System.out.println("    -custom \"lft_0.25\" -addmetrics shotgun.metrics.Lift \".25 0.05 ...\"");
        System.out.println("  This optimizes to lift with 25% of data predicted as positive, and also");
        System.out.println("  measures the ensemble's lift with 5% of data predicted positive.");
        System.out.println("==============================================================================");
        System.exit(0);
    }

    /**
   * Checks if a given argument is an integer.
   *
   * @param arg The string to check.
   * @return True if it is an integer, false otherwise.
  **/
    private static boolean isInt(String arg) {
        try {
            Integer.parseInt(arg);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
   * Parses out a custom metric specification from command line and
   * adds to list of customs.
   * @param offset Offset of the current cmd arg. Should be pointing
   * to the class name.
   * @param args The non-empty array of command line arguments.
   * @return The offset for the next unprocessed command line
   * argument.
   */
    private static int parseCustomMetrics(int offset, String[] args) {
        String className = args[offset++];
        String cArg = args[offset++];
        try {
            Predictions.addCustomMetrics(className, cArg);
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            System.exit(1);
        } catch (InstantiationException inst) {
            inst.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException denied) {
            System.err.println(denied);
            denied.printStackTrace();
            System.exit(1);
        }
        return offset;
    }

    /**
   * Entry point to this program.
  **/
    public static void main(String[] args) throws java.io.IOException {
        String cmd = "";
        String perfMode = "";
        int numIterations = -1;
        String output = "";
        ModelFilter modelFilter = new ModelFilter();
        LibraryLoader loader = new LibraryLoader(null);
        int bagReps = 1;
        double bagPercent = 1.0;
        int cvFolds = 0;
        double cvInc = 0;
        boolean writePred = false;
        boolean crossValidate = false;
        boolean sampling = false;
        boolean pruning = false;
        boolean percentThresh = false;
        boolean autothresh = false;
        boolean datathresh = false;
        String option;
        int argNo = 0;
        double[] w = new double[3];
        w[0] = 0.33;
        w[1] = 0.33;
        w[2] = 0.33;
        Predictions.setWeight(w);
        Predictions.setSlacqBinWidth(0.01);
        Predictions.setSlacqSeed(123);
        Predictions.setSlacqTrials(10);
        Predictions.computeFactCache();
        while (argNo < args.length && args[argNo].startsWith("-")) {
            option = args[argNo++].substring(1);
            if (option.equals("acc") || option.equals("rms") || option.equals("roc") || option.equals("bep") || option.equals("pre") || option.equals("rec") || option.equals("fsc") || option.equals("apr") || option.equals("lft") || option.equals("mxe")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
            } else if (option.equals("all")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                double[] weight = new double[3];
                weight[0] = Double.parseDouble(args[argNo++]);
                weight[1] = Double.parseDouble(args[argNo++]);
                weight[2] = Double.parseDouble(args[argNo++]);
                Predictions.setWeight(weight);
            } else if (option.equals("cst")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                double[] cost = new double[4];
                cost[0] = Double.parseDouble(args[argNo++]);
                cost[1] = Double.parseDouble(args[argNo++]);
                cost[2] = Double.parseDouble(args[argNo++]);
                cost[3] = Double.parseDouble(args[argNo++]);
                Predictions.setCost(cost);
            } else if (option.equals("nrm")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setNorm(Double.parseDouble(args[argNo++]));
            } else if (option.equals("slq")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setSlacqBinWidth(Double.parseDouble(args[argNo++]));
                Predictions.setSlacqSeed(Integer.parseInt(args[argNo++]));
                Predictions.setSlacqTrials(Integer.parseInt(args[argNo++]));
            } else if (option.equals("ca1")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
            } else if (option.equals("ca2")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setCalibrationSize(Integer.parseInt(args[argNo++]));
            } else if (option.equals("ca3")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setCalibrationSize(Integer.parseInt(args[argNo++]));
            } else if (option.equals("ca4")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setCalibrationSize(Integer.parseInt(args[argNo++]));
            } else if (option.equals("ca5")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setCalibrationSize(Integer.parseInt(args[argNo++]));
            } else if (option.equals("ca6")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = option;
                Predictions.setCalibrationSize(Integer.parseInt(args[argNo++]));
            } else if (option.equals("custom")) {
                if (!perfMode.equals("")) helpMsg();
                perfMode = args[argNo++];
            } else if (option.equals("s") || option.equals("g") || option.equals("b") || option.equals("f") || option.equals("x") || option.equals("fr") || option.equals("sfr")) {
                if (!cmd.equals("")) helpMsg();
                cmd = option;
                if (isInt(args[argNo])) numIterations = Integer.parseInt(args[argNo++]);
            } else if (option.equals("bayes")) {
                if (!cmd.equals("")) helpMsg();
                cmd = option;
            } else if (option.equals("bm")) {
                if (!cmd.equals("")) helpMsg();
                cmd = option;
                modelFilter.setUseFilter(new String[] { args[argNo++] });
            } else if (option.equals("baseline")) {
                if (!cmd.equals("")) helpMsg();
                cmd = option;
                modelFilter.setUseFilter(new String[] { "_baseline" });
            } else if (option.equals("d")) {
                weightDecay = true;
                Predictions.setDecay(Double.parseDouble(args[argNo++]));
            } else if (option.equals("o")) {
                output = args[argNo++];
            } else if (option.equals("wp")) {
                writePred = true;
            } else if (option.equals("t")) {
                String val = args[argNo++];
                if (val.equals("data")) {
                    datathresh = true;
                } else {
                    Predictions.setThreshold(Double.parseDouble(val));
                }
            } else if (option.equals("p")) {
                Predictions.setPrcdata(Double.parseDouble(args[argNo++]));
                percentThresh = true;
            } else if (option.equals("bsp")) {
                bootstrap = true;
                int numBsp = Integer.parseInt(args[argNo++]);
                int numPts = Integer.parseInt(args[argNo++]);
                long seed = Long.parseLong(args[argNo++]);
                Predictions.setBsp(numBsp, numPts, seed);
            } else if (option.equals("?") || option.equals("help") || option.equals("h")) {
                helpMsg();
            } else if (option.equals("bag")) {
                bagReps = Integer.parseInt(args[argNo++]);
                bagPercent = Double.parseDouble(args[argNo++]);
                r = new Random(Integer.parseInt(args[argNo++]));
            } else if (option.equals("stepwise")) {
                stepwiseBagging = true;
            } else if (option.equals("stepwise-sort-all")) {
                stepwiseBagging = true;
                stepwiseSortInitAll = true;
            } else if (option.equals("ir")) {
                incrementalReporting = true;
            } else if (option.equals("sbset")) {
                if (pruning) {
                    System.out.println("error: -sbset and -prune cannot be used together");
                    System.exit(1);
                }
                sampling = true;
                double percent = Double.parseDouble(args[argNo++]);
                String criteria = args[argNo++];
                if (criteria.equals("random")) {
                    int seed = Integer.parseInt(args[argNo++]);
                    loader = new LibraryLoader(new RandomSampler(percent, seed));
                } else if (criteria.equals("top")) {
                    loader = new LibraryLoader(new TopSampler(percent));
                } else if (criteria.equals("external")) {
                    String file = args[argNo++];
                    loader = new LibraryLoader(new ExternalSampler(percent, file));
                } else {
                    System.out.println("Error: unexpected argument to -sbset option.");
                    System.exit(1);
                }
            } else if (option.equals("prune")) {
                if (sampling) {
                    System.out.println("Error: -sbset and -prune cannot be used together");
                    System.exit(1);
                }
                pruning = true;
                loader = new LibraryLoader(new TopSampler(1.0));
            } else if (option.equals("cv")) {
                crossValidate = true;
                cvFolds = Integer.parseInt(args[argNo++]);
                bagReps = Integer.parseInt(args[argNo++]);
                cvInc = Double.parseDouble(args[argNo++]);
                r = new Random(Integer.parseInt(args[argNo++]));
            } else if (option.equals("r")) {
                writeCount = true;
            } else if (option.equals("v")) {
                Predictions.setVerbose();
                verbose = true;
            } else if (option.equals("autothresh")) {
                Model.setDynamicThreshold(true);
                autothresh = true;
            } else if (option.equals("addmetrics")) {
                argNo = parseCustomMetrics(argNo, args);
            } else if (option.equals("l")) {
                Predictions.setLossMode();
            } else if (option.equals("reuse")) {
                reuse = true;
            } else if (option.equals("use") || option.equals("ignore")) {
                if (!args[argNo++].equals("{")) {
                    System.out.println("Error : " + option + " list must start with \"{ \" and end with \" }\"");
                    System.exit(0);
                }
                int beginarg = argNo;
                int size = 0;
                while (!args[argNo++].equals("}")) {
                    size++;
                    if (argNo > args.length) {
                        System.out.println("Error : " + option + " list must start with \"{ \" and end with \" }\"");
                        System.exit(0);
                    }
                }
                String[] filterModels = new String[size];
                for (int i = 0; i < size; i++) filterModels[i] = args[i + beginarg];
                if (option.equals("use")) modelFilter.setUseFilter(filterModels); else modelFilter.setIgnoreFilter(filterModels);
            } else {
                System.out.println("Error : Unrecognized option " + option);
                System.exit(0);
            }
        }
        if (autothresh && percentThresh) {
            System.out.println("Error: cannot use -autothresh and -p together.");
            System.exit(1);
        }
        if (cmd.equals("")) cmd = "-sfr";
        if (perfMode.equals("")) perfMode = "rms";
        if (args.length < argNo + 2) helpMsg();
        String predsFolder = args[argNo++];
        String trainFolder = args[argNo++];
        updateBest = writePred || writeCount || bagReps != 1;
        boolean setMode = false;
        if (bootstrap) {
            setMode = Predictions.setBspMode(perfMode);
        } else {
            setMode = Predictions.setMode(perfMode);
        }
        if (!setMode) {
            System.out.println("Error: failed to set performance mode to " + perfMode + ".");
            System.exit(1);
        }
        if ((crossValidate || bootstrap) && !Predictions.reorderingAllowed()) {
            System.out.println("Error: one or more metrics does not support bootstrapping or cross-validation");
            System.exit(1);
        }
        if (cmd.equals("baseline") && pruning) {
            System.out.println("Error: -baseline and -prune cannot be used together");
            System.exit(1);
        }
        init(predsFolder, trainFolder, modelFilter, loader, datathresh, cmd.equals("baseline"));
        if (pruning) {
            double amtToKeep = prune(numIterations < 0 ? modelLibrary.length : numIterations);
            System.err.println("using top " + amtToKeep + "% of models");
        }
        Model.setFileWriters(output, writePred, allSets);
        if (crossValidate) {
            int maxIters = numIterations;
            if (numIterations == -1) maxIters = ModelSelection.modelLibrary.length;
            bagPercent = pickP(maxIters, cvFolds, bagReps, cvInc);
        }
        ModelSelection modelSelection[] = new ModelSelection[bagReps];
        for (int currentBag = 0; currentBag < bagReps; currentBag++) {
            if (bagReps != 1) Model.setFileWriters("bag" + currentBag, false, allSets);
            modelSelection[currentBag] = new ModelSelection(bagPercent);
            int maxIters = numIterations;
            if (cmd.equals("b")) {
                if (numIterations == -1 || numIterations > modelSelection[currentBag].numModels) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].backwardElimination(maxIters);
            } else if (cmd.equals("f")) {
                if (numIterations == -1 || numIterations > modelSelection[currentBag].numModels) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].forwardSelection(false, maxIters);
            } else if (cmd.equals("fr")) {
                if (numIterations == -1) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].forwardSelection(true, maxIters);
            } else if (cmd.equals("s")) {
                if (numIterations == -1 || numIterations > modelSelection[currentBag].numModels) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].sort(maxIters);
            } else if (cmd.equals("g")) {
                if (numIterations == -1 || numIterations > modelSelection[currentBag].numModels) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].greatestIncrease(maxIters);
            } else if (cmd.equals("sfr")) {
                if (numIterations == -1) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].sortAndSelect(maxIters);
            } else if (cmd.equals("x")) {
                if (numIterations == -1 || numIterations > modelSelection[currentBag].numModels) maxIters = modelSelection[currentBag].numModels;
                modelSelection[currentBag].eachModel(maxIters);
            } else if (cmd.equals("bm")) {
                modelSelection[currentBag].eachModel(1);
            } else if (cmd.equals("bayes")) {
                modelSelection[currentBag].bayesianAveraging();
            } else if (cmd.equals("baseline")) {
                modelSelection[currentBag].reportBaseline();
            }
        }
        if (writePred || bagReps != 1) {
            Model md = new Model();
            for (int i = 0; i < bagReps; i++) {
                md.addModel(modelSelection[i].bestEnsemble, true, 1);
                if (incrementalReporting) {
                    if (bagReps != 1) {
                        String temp = output + (output.equals("") ? "" : ".") + (new Integer(i)).toString();
                        Model.setFileWriters(temp, writePred, allSets);
                        md.report("final_bag " + bagPercent, allSets);
                    }
                    if (writePred) md.write();
                }
            }
            if (bagReps != 1) {
                Model.setFileWriters(output, writePred, allSets);
                md.report("final_bag " + bagPercent, allSets);
            }
            if (writePred) md.write();
        }
        if (writeCount) {
            String filenames[] = new String[modelLibrary.length];
            for (int i = 0; i < modelLibrary.length; i++) filenames[i] = modelLibrary[i].getName();
            Arrays.sort(filenames);
            for (int i = 0; i < modelLibrary.length; i++) {
                System.out.print(filenames[i]);
                System.out.print(" ");
                for (int j = 0; j < bagReps; j++) {
                    double count;
                    if ((count = modelSelection[j].modelCounts.getWeight(filenames[i])) >= 0) System.out.print(count); else System.out.print("-");
                    System.out.print(" ");
                }
                for (int j = 0; j < modelLibrary.length; j++) {
                    if (modelLibrary[j].getName().equals(filenames[i])) {
                        System.out.println(modelLibrary[i].getPerformance());
                        continue;
                    }
                }
            }
        }
    }
}
