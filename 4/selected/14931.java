package gov.sns.apps.ema;

import gov.sns.tools.beam.EnergyFinder;
import gov.sns.tools.correlator.Correlation;
import gov.sns.xal.model.probe.Probe;
import gov.sns.ca.ChannelRecord;
import java.util.*;

public class BPMEnergyCalculator implements Runnable {

    /** whether to keep  running */
    volatile boolean keepOn;

    /** flag to check if am already running */
    volatile boolean amRunning = false;

    /** the bpmController that is managing stuff */
    private BPMController bpmController;

    /** the energy  finder for TOF information */
    private EnergyFinder energyFinder;

    /** the active correlation being analzed */
    volatile Correlation activeCorrelation;

    /** the latest aquired correlation waiting to be analzed */
    volatile Correlation latestCorrelation;

    /** the calculation thread */
    private Thread calcThread;

    /** the constructor */
    public BPMEnergyCalculator(Probe probe, BPMController cont) {
        keepOn = false;
        bpmController = cont;
        energyFinder = new EnergyFinder(probe, 402.5);
        calcThread = new Thread(this, "Energy Calculator");
    }

    /** start the energy calculation thread */
    protected void start() {
        keepOn = true;
        if (!amRunning) calcThread.start();
    }

    /** update the latest correlation to work on */
    protected void setCorrelation(Correlation correlation) {
        latestCorrelation = correlation;
    }

    /** what to do to calculate the energy */
    public void run() {
        double diff, energy;
        ChannelRecord c1, c2;
        amRunning = true;
        while (keepOn) {
            if (latestCorrelation != null && latestCorrelation != activeCorrelation) {
                activeCorrelation = latestCorrelation;
                Collection<BPMPair> pairs = bpmController.selectedPairs.values();
                for (BPMPair pair : pairs) {
                    if (activeCorrelation.isCorrelated(pair.getChannel1().getId()) && activeCorrelation.isCorrelated(pair.getChannel2().getId())) {
                        c1 = (ChannelRecord) activeCorrelation.getRecord(pair.getChannel1().getId());
                        c2 = (ChannelRecord) activeCorrelation.getRecord(pair.getChannel2().getId());
                        diff = c2.doubleValue() - c1.doubleValue();
                        energyFinder.initCalc(pair.getLength(), pair.getWGuess());
                        energy = energyFinder.findEnergy(diff);
                        pair.energy = new Double(energy);
                        pair.stats.addSample(energy);
                    }
                }
                try {
                    bpmController.updateBPMTable();
                    Thread.currentThread().sleep(500);
                } catch (Exception ex) {
                    bpmController.dumpErr("Trouble sleeping in the BPM calculator");
                }
            }
        }
    }
}
