package mil.army.usace.ehlschlaeger.digitalpopulations.censusgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;
import mil.army.usace.ehlschlaeger.digitalpopulations.censusgen.fittingcriteria.ExpansionFactor;
import mil.army.usace.ehlschlaeger.rgik.core.DataException;
import mil.army.usace.ehlschlaeger.rgik.core.RGIS;
import mil.army.usace.ehlschlaeger.rgik.io.StringOutputStream;
import mil.army.usace.ehlschlaeger.rgik.util.FileUtil;
import mil.army.usace.ehlschlaeger.rgik.util.LogUtil;
import mil.army.usace.ehlschlaeger.rgik.util.MyRandom;
import mil.army.usace.ehlschlaeger.rgik.util.ObjectUtil;

/**
 * Compute the "expansion factor" for household archtypes. "Expansion factor" is
 * defined as the number of clones we will create of the records in the
 * households table.
 * 
 * @see {@link Phase_ComputeMultiplier}
 */
public class Phase_ExpansionFactor {

    protected static Logger log = Logger.getLogger(Phase_InitialPlacement.class.getPackage().getName());

    private static Object cacheFileLock = Phase_ExpansionFactor.class.getName().intern();

    protected Solution soln;

    protected ExpansionFactor phaseData;

    protected RegionData primaryRegion;

    protected int peopleInArchTypes;

    protected Date configTime;

    protected Date householdArchTypesTime;

    protected ArrayList<Integer> regionList;

    protected int naiveExpansion = -1;

    protected Params params = new Params();

    protected long initialSeed;

    protected Random statFactorRnd;

    protected Random userFactorRnd;

    /**
     * Build standard instance. You will generally want to call setParams() and
     * setRandomSource(), then go() to run process.
     * 
     * @param soln
     *            set of archtypes for which we will create realizations
     * @param phaseData
     *            user's preferences for this phase
     * @param primaryRegion
     *            main region map and its table
     * @param peopleInArchTypes
     *            total number of people in all the households in
     *            householdArchTypes. This will be a percentage of the actual
     *            number of people in the region.
     * @param configTime
     *            time at which config files were created. If there are several
     *            input files, then this should be the most recent (latest)
     *            time.
     */
    public Phase_ExpansionFactor(Solution soln, ExpansionFactor phaseData, RegionData primaryRegion, int peopleInArchTypes, Date configTime) {
        this.soln = soln;
        this.phaseData = phaseData;
        this.primaryRegion = primaryRegion;
        this.peopleInArchTypes = peopleInArchTypes;
        this.configTime = configTime;
        this.regionList = new ArrayList<Integer>(primaryRegion.map.makeInventory());
        Collections.sort(regionList);
        long htime = soln.householdSchema.getFile().lastModified();
        long ptime = 0;
        if (soln.populationSchema != null) ptime = soln.populationSchema.getFile().lastModified();
        householdArchTypesTime = new Date(htime > ptime ? htime : ptime);
        setRandomSeed(new Random().nextLong());
    }

    /**
     * Install run-time configuration.
     * 
     * @param params current set of run-time parameters
     */
    public void setParams(Params params) {
        this.params = params;
    }

    /**
     * Change our source of random numbers. Note we can't accept a custom Random
     * here as we need access to the original seed to determine whether our
     * cache file is valid.
     * 
     * @param source
     *            new random number generator
     */
    public void setRandomSeed(long seed) {
        initialSeed = seed;
        statFactorRnd = new Random(seed);
        userFactorRnd = new Random(statFactorRnd.nextLong());
    }

    /**
     * @return "naive" multiplier, as if every archtype would receive the same
     *         number of clones
     */
    public int getNaiveFactor() {
        return naiveExpansion;
    }

    /**
     * Perform the process as currently configured.
     * 
     * @return expansion factors as an array of ints
     * 
     * @throws IOException on any error writing output files
     */
    public int[] go() {
        int[] numRealizations2Make;
        LogUtil.cr(log);
        LogUtil.progress(log, "Starting phase 1: Computing expansion factors for household archtypes.");
        LogUtil.cr(log);
        LogUtil.result(log, "Ratio of households Agg / Pums: %.2f    Ratio of people Agg / Pums: %.2f", primaryRegion.aggregateHouseholds / (float) soln.householdArchTypes.length, primaryRegion.aggregatePopulation / (float) peopleInArchTypes);
        naiveExpansion = (int) Math.round(primaryRegion.aggregatePopulation / (float) peopleInArchTypes);
        if (naiveExpansion < 1) naiveExpansion = 1;
        if (params.getDumpNumberArchtypes()) LogUtil.result(log, "Initial assumption: %d realizations per archtype", naiveExpansion);
        double trust = 100.0;
        if (phaseData != null) {
            trust = phaseData.trust;
        }
        if (trust <= 0) {
            double[] userFactors = makeUserFactors();
            numRealizations2Make = new int[soln.householdArchTypes.length];
            for (int i = 0; i < soln.householdArchTypes.length; i++) numRealizations2Make[i] = (int) Math.round(userFactors[i]);
        } else if (trust >= 100) {
            numRealizations2Make = makeStatFactors();
        } else {
            int[] statFactors = makeStatFactors();
            double[] userFactors = makeUserFactors();
            numRealizations2Make = new int[soln.householdArchTypes.length];
            for (int i = 0; i < soln.householdArchTypes.length; i++) {
                numRealizations2Make[i] = (int) Math.round(trust / 100.0 * statFactors[i] + (1 - trust / 100.0) * userFactors[i]);
            }
        }
        if (params.getDumpNumberArchtypes()) {
            LogUtil.cr(log);
            LogUtil.result(log, dumpNumberArchtypes(numRealizations2Make));
        }
        return numRealizations2Make;
    }

    /**
     * Compute multipliers from user-specified random criteria.
     * 
     * @return array of random multipliers
     */
    protected double[] makeUserFactors() {
        double[] userExpansionFactors = new double[soln.householdArchTypes.length];
        String factorCol = null;
        int factorIdx = -1;
        double factorVal = -1;
        String stdDevCol = null;
        int stdDevIdx = -1;
        double stdDevVal = -1;
        if (phaseData != null) {
            factorCol = phaseData.factorCol;
            if (ObjectUtil.isBlank(factorCol)) factorCol = null;
            stdDevCol = phaseData.stdDevCol;
            if (ObjectUtil.isBlank(stdDevCol)) stdDevCol = null;
            try {
                if (factorCol != null) factorVal = Double.parseDouble(factorCol);
            } catch (NumberFormatException e) {
                factorIdx = soln.householdSchema.findColumn(factorCol);
            }
            try {
                if (stdDevCol != null) stdDevVal = Double.parseDouble(stdDevCol);
            } catch (NumberFormatException e) {
                stdDevIdx = soln.householdSchema.findColumn(stdDevCol);
            }
        }
        if (factorCol == null) Arrays.fill(userExpansionFactors, naiveExpansion); else {
            if (factorIdx < 0) Arrays.fill(userExpansionFactors, factorVal); else {
                for (int i = 0; i < soln.householdArchTypes.length; i++) {
                    userExpansionFactors[i] = soln.householdArchTypes[i].getAttributeValue(factorIdx);
                    if (userExpansionFactors[i] < 0) throw new DataException(String.format("Expansion factor cannot be less than zero:" + " Household archtypes file \"%s\", row %d, column %s.", soln.householdSchema.getFile(), i, stdDevCol));
                }
            }
        }
        if (stdDevCol != null) {
            double[] stddev = new double[userExpansionFactors.length];
            if (stdDevIdx < 0) Arrays.fill(stddev, stdDevVal); else for (int i = 0; i < soln.householdArchTypes.length; i++) {
                stddev[i] = soln.householdArchTypes[i].getAttributeValue(stdDevIdx);
                if (stddev[i] < 0) throw new DataException(String.format("Standard deviation cannot be less than zero:" + " Household archtypes file \"%s\", row %d, column %s.", soln.householdSchema.getFile(), i, stdDevCol));
            }
            for (int i = 0; i < soln.householdArchTypes.length; i++) {
                userExpansionFactors[i] = MyRandom.nextGaussian(userFactorRnd, userExpansionFactors[i], stddev[i]);
                if (userExpansionFactors[i] < 0) userExpansionFactors[i] = 0;
            }
        }
        return userExpansionFactors;
    }

    /**
     * Compute multipliers with values determined by statistics.
     * <P>
     * Loads numRealizations2Make from cache file if possible, else computes it
     * from input data then saves to cache file when done. Note the cache file
     * contains only the optimized values; user's adjusted values (via the
     * <&lt;phase1&gt;> element are computed separately.
     * 
     * @return array of optimal multipliers
     * @throws IOException 
     */
    @SuppressWarnings({ "unchecked", "unused" })
    protected int[] makeStatFactors() {
        File cacheFile = FileUtil.resolve(RGIS.getOutputFolder(), String.format("phase1-cache-%d.bin", initialSeed));
        final String P1P_RESULT = "numRealizations2Make";
        final String P1P_LAST_MODIFIED = "last-modified";
        final String P1P_RANDOM_SEED = "random-seed";
        Date inputTime = householdArchTypesTime.after(configTime) ? householdArchTypesTime : configTime;
        int[] numRealizations2Make = null;
        synchronized (cacheFileLock) {
            while (numRealizations2Make == null) {
                if (cacheFile.exists()) {
                    HashMap<String, Object> props = null;
                    ObjectInputStream in = null;
                    FileLock lock = null;
                    try {
                        FileInputStream fin = new FileInputStream(cacheFile);
                        lock = fin.getChannel().lock(0, Long.MAX_VALUE, true);
                        in = new ObjectInputStream(fin);
                        props = (HashMap<String, Object>) in.readObject();
                        in.close();
                    } catch (IOException e) {
                        log.warning("Unable to read phase-1 cache file: " + ObjectUtil.getMessage(e));
                    } catch (ClassNotFoundException e) {
                        LogUtil.cr(log);
                        log.warning("Unable to read phase-1 cache file: " + ObjectUtil.getMessage(e));
                    } finally {
                        if (lock != null) try {
                            lock.release();
                        } catch (IOException e) {
                        }
                        if (in != null) try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                    if (props != null) {
                        int[] cacheData = (int[]) props.get(P1P_RESULT);
                        Date cacheTime = (Date) props.get(P1P_LAST_MODIFIED);
                        Long cacheSeed = (Long) props.get(P1P_RANDOM_SEED);
                        if (!cacheTime.before(inputTime) && cacheSeed != null && cacheSeed.equals(params.getRandomSeed())) {
                            numRealizations2Make = cacheData;
                            householdArchTypesTime = cacheTime;
                            LogUtil.progress(log, "Loaded results from cache file " + cacheFile.getName());
                        }
                    }
                }
                if (numRealizations2Make == null) {
                    FileOutputStream fout = null;
                    FileLock lock = null;
                    boolean locked = false;
                    try {
                        fout = new FileOutputStream(cacheFile);
                        lock = fout.getChannel().tryLock();
                        locked = (lock != null);
                    } catch (IOException e) {
                        LogUtil.cr(log);
                        log.warning("Unable to write phase-1 cache file: " + ObjectUtil.getMessage(e));
                        if (lock != null) try {
                            lock.release();
                        } catch (IOException e1) {
                        }
                        if (fout != null) {
                            try {
                                fout.close();
                                locked = true;
                            } catch (IOException e1) {
                            }
                        }
                    }
                    try {
                        if (locked) {
                            numRealizations2Make = computeMultiplier();
                            if (fout != null) {
                                HashMap<String, Object> props = new HashMap<String, Object>();
                                props.put(P1P_RESULT, numRealizations2Make);
                                props.put(P1P_LAST_MODIFIED, inputTime);
                                props.put(P1P_RANDOM_SEED, params.getRandomSeed());
                                ObjectOutputStream out = null;
                                try {
                                    out = new ObjectOutputStream(fout);
                                    out.writeObject(props);
                                } catch (Exception e) {
                                    LogUtil.cr(log);
                                    log.warning("Unable to write phase-1 cache file: " + ObjectUtil.getMessage(e));
                                }
                            }
                        }
                    } finally {
                        if (fout != null) {
                            try {
                                fout.close();
                            } catch (IOException e1) {
                            }
                        }
                    }
                }
                if (numRealizations2Make == null) {
                    try {
                        cacheFileLock.wait(2000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        return numRealizations2Make;
    }

    /**
     * Compute numRealizations2Make, which holds the number of times to clone each household
     * archtype to best fit the required statistics.
     */
    protected int[] computeMultiplier() {
        Solution probe = soln.createCopy();
        int sampleTract = regionList.get(0);
        for (int j = 0; j < probe.householdArchTypes.length; j++) {
            for (int i = 0; i < naiveExpansion; i++) probe.addRealization(j, sampleTract);
        }
        Phase_ComputeMultiplier help = new Phase_ComputeMultiplier(probe);
        help.minimumRealizations = (int) Math.floor(0.17 * naiveExpansion);
        if (help.minimumRealizations < 1) help.minimumRealizations = 1;
        help.bestfit = probe.getFit();
        Date startArchCount = new Date();
        Date progressTime = startArchCount;
        boolean checkedAll = false;
        LogUtil.cr(log);
        LogUtil.progress(log, "Starting the process of optimizing numbers of archtypes at " + startArchCount);
        help.printProgressHeader(log);
        help.printProgress(log, 0);
        int delta = 1;
        while (delta < naiveExpansion) delta *= 2;
        if (delta > 1) delta /= 2;
        if (delta > 1) delta /= 2;
        double limit = -1;
        if (params.getPhase1TimeLimit() > 0) limit = 60 * params.getPhase1TimeLimit();
        while (checkedAll == false) {
            Date nowTime = new Date();
            double elapsed = (nowTime.getTime() - startArchCount.getTime()) / 1000.0;
            if (limit > 0 && elapsed > limit) break;
            if (nowTime.getTime() - progressTime.getTime() > 60000) {
                progressTime = nowTime;
                help.printProgress(log, elapsed);
            }
            int archtype = statFactorRnd.nextInt(soln.householdArchTypes.length);
            int lastArchtype = archtype;
            do {
                if (help.tryMore(archtype, delta, sampleTract)) break; else if (help.tryLess(archtype, delta, sampleTract)) break;
                archtype--;
                if (archtype < 0) archtype = probe.householdArchTypes.length - 1;
                if (lastArchtype == archtype) checkedAll = true;
            } while (archtype != lastArchtype);
            if (checkedAll) {
                if (delta > 1) {
                    delta /= 2;
                    checkedAll = false;
                }
            }
        }
        int[] numRealizations2Make = help.getNumberRealizations();
        Date endArchCount = new Date();
        double elapsed = (endArchCount.getTime() - startArchCount.getTime()) / 1000.0;
        help.printProgress(log, elapsed);
        if (!checkedAll) LogUtil.progress(log, "Aborting process: time budget exceeded"); else LogUtil.progress(log, "Ending process: no more moves can be found");
        LogUtil.cr(log);
        LogUtil.result(log, help.dumpFit("Quality of Adjusted Quantities"));
        return numRealizations2Make;
    }

    protected String dumpNumberArchtypes(int[] numRealizations2Make) {
        StringOutputStream sos = new StringOutputStream();
        ArrayList<String> msgs = new ArrayList<String>();
        sos.println("Final expansion factors:");
        int idxWid = 1;
        if (numRealizations2Make.length > 9) idxWid = 1 + (int) Math.floor(Math.log10(numRealizations2Make.length));
        String idxFmt = "%" + idxWid + "d:";
        double max = 0, min = 0;
        for (int i = 0; i < numRealizations2Make.length; i++) {
            max = Math.max(max, numRealizations2Make[i]);
            min = Math.min(min, numRealizations2Make[i]);
        }
        int valWid = 1;
        if (max > 9) valWid = 1 + (int) Math.floor(Math.log10(max));
        if (min < 0) valWid = Math.max(valWid, 2 + (int) Math.floor(Math.log10(-min)));
        String valFmt = " %" + valWid + "d";
        for (int i = 0; i < numRealizations2Make.length; i++) {
            if (i > 0) {
                if (i % 20 == 0) sos.println();
            }
            if (i % 20 == 0) sos.format(idxFmt, i);
            sos.format(valFmt, numRealizations2Make[i]);
            if (numRealizations2Make[i] == 0) msgs.add(String.format("WARNING: Archtype %d has ZERO realizations.", i));
        }
        if (msgs.size() > 0) {
            sos.println();
            sos.println();
            sos.print(ObjectUtil.join(msgs, "\n"));
        }
        return sos.toString();
    }
}
