package shieh.pnn.nets;

import shieh.pnn.core.AbstractRandomGenerator;
import shieh.pnn.core.ActionAccumulator;
import shieh.pnn.core.ActionList;
import shieh.pnn.core.BiState;
import shieh.pnn.core.ItemSet;
import shieh.pnn.core.Layer;
import shieh.pnn.core.LayerClearer;
import shieh.pnn.core.LayerGated;
import shieh.pnn.core.Network;
import shieh.pnn.core.Param;
import shieh.pnn.core.ProcessorHost;
import shieh.pnn.core.Projection;
import shieh.pnn.core.ProjectionGated;
import shieh.pnn.core.ProjectionType;
import shieh.pnn.core.Signal;
import shieh.pnn.core.SignalAction;
import shieh.pnn.core.SignalList;
import shieh.pnn.core.SignalProcessor;
import shieh.pnn.core.SignalProcessorManual;
import shieh.pnn.core.Simulation;
import shieh.pnn.core.TimerProcessor;
import shieh.pnn.core.WtType;
import shieh.pnn.core.Layer.SigOper;
import shieh.pnn.core.Layer.SigQuant;
import shieh.pnn.wm.LayerPhonemeOutput;
import shieh.pnn.wm.PhonemeSet;
import shieh.pnn.wm.WordSet;

/**
 * Dual representation Divergent-Reconvergent model (DR2)
 * 
 * @author Danke
 */
public class NetDivReconvDualMath extends Network {

    /**
	 * 
	 */
    private static final long serialVersionUID = -473949929262714052L;

    public static final String NAME_DUAL_NET = "DR2 model (Math)";

    public static final String NAME_PHONEME_INPUT_LAYER = "Phoneme Input";

    public static final String NAME_PHONEME_POSTIN_LAYER = "Phoneme Post-Input";

    public static final String NAME_PHONEME_REHEARSE_LAYER = "Phoneme Rehearse";

    public static final String NAME_PHONEME_REPORT_LAYER = "Phoneme Report";

    public static final String NAME_RECALL_STATE = "RECALL_STATE";

    public static final String NAME_RECALL_WAIT_STATE = "RECALL_WAIT_STATE";

    public static final String NAME_BUSY_STATE = "BUSY_STATE";

    public static final String NAME_NOISY_STATE = "NOISY_STATE";

    public static final String NAME_REWRITE_STATE = "REWRITE_STATE";

    public static final String NAME_PSREAD_STATE = "PSREAD_STATE";

    public static final String NAME_CANRECODE_STATE = "CAN_RECODE";

    public static final String NAME_RETRIEVE_END_CHECK = "RETRIEVE_CHECK_TIMER";

    public static final String NAME_RECODE_PROGRAM = "Recode";

    public static final String NAME_REHEARSE_PROGRAM = "Rehearse";

    protected PhonemeSet phonemeSet;

    public NetDivReconvArt netDR;

    public NetSeqRecognizer netRecog;

    public NetPhonStoreMath netPS;

    public LayerGated layerPhonemeInput;

    public Layer layerPhonemePostInput;

    public Layer layerPhonemeRehearse;

    public Layer layerPhonemeReport;

    public LayerClearer clearIOR;

    public Projection projRecogDR, projInSensory, projSensoryIn;

    public ProjectionGated projPIPO, projPRPI, projPRPO, projDRInOut;

    public ProjectionGated projRWaveStopShortcut;

    public BiState rehearseState = null, noisyState = null, recallState = null, recallWaitState = null, rewriteState = null;

    public BiState psreadState = null, canRecode = null;

    protected SignalAction actRetrieve = new SignalAction("RETRIEVE");

    int nPhonemeCapacity = Param.ps_size;

    protected boolean optimalRehearse = Param.canRehearse;

    public ActionAccumulator accOutput = null;

    Signal RECALL = ProcessorHost.getInstance().registerSignal("RECALL");

    Signal TEST_RETRIEVE_END = ProcessorHost.getInstance().registerSignal("TEST RETRIEVE END");

    Signal RETRIEVE_DONE = ProcessorHost.getInstance().registerSignal("RETRIEVE DONE");

    Signal sigNoPhonemeInput, sigNoItemInput, sigRecognizerInactive;

    SignalAction actTestRetrieveEnd, actRetrieveDone;

    public NetDivReconvDualMath(String label, ItemSet items, PhonemeSet phonemeSet) {
        super(label, items);
        this.phonemeSet = phonemeSet;
    }

    public boolean isOptimalRehearse() {
        return optimalRehearse;
    }

    public void setOptimalRehearse(boolean optimalRehearse) {
        this.optimalRehearse = optimalRehearse;
    }

    @Override
    public void initNetwork() {
        super.initNetwork();
        layerPhonemeInput = new LayerGated(this, NAME_PHONEME_INPUT_LAYER, 1);
        layerPhonemeInput.createGroup(phonemeSet.getInputSize() - 1);
        addLayer(layerPhonemeInput, true);
        layerPhonemeInput.getGate().setDefaultState(true);
        layerPhonemePostInput = new Layer(this, NAME_PHONEME_POSTIN_LAYER, 1);
        layerPhonemePostInput.createGroup(phonemeSet.getInputSize() - 1);
        addLayer(layerPhonemePostInput, true);
        layerPhonemeRehearse = new Layer(this, NAME_PHONEME_REHEARSE_LAYER, phonemeSet.getInputSize());
        addLayer(layerPhonemeRehearse, true);
        layerPhonemeReport = new LayerPhonemeOutput(this, NAME_PHONEME_REPORT_LAYER, phonemeSet);
        addLayer(layerPhonemeReport, true);
        recallState = new BiState(this, NAME_RECALL_STATE, true);
        recallWaitState = new BiState(this, NAME_RECALL_WAIT_STATE, true);
        rehearseState = new BiState(this, NAME_BUSY_STATE, true);
        rewriteState = new BiState(this, NAME_REWRITE_STATE, true);
        canRecode = new BiState(this, NAME_CANRECODE_STATE, true);
        noisyState = new BiState(this, NAME_NOISY_STATE, true);
        noisyState.layer.setDecay(1 - Math.pow(Param.significant_input, (double) 1 / 2));
        noisyState.layer.setActBounds(0, 1);
        psreadState = new BiState(this, NAME_PSREAD_STATE, true);
        psreadState.layer.setDecay(1 - Math.pow(Param.significant_input, (double) Param.tick / 400D));
        psreadState.layer.setActBounds(0, 1);
        addProcessor(recallState);
        addProcessor(recallWaitState);
        addProcessor(rehearseState);
        addProcessor(noisyState);
        addProcessor(actRetrieve);
        addProcessor(psreadState);
        addProcessor(canRecode);
        netDR = new NetDivReconvArt(NetDivReconvArt.NAME_DR_NET, (WordSet) items, layerPhonemeRehearse, true);
        netDR.initNetwork();
        clearIOR = new LayerClearer(netDR.layerIOR);
        addProcessor(clearIOR);
        actTestRetrieveEnd = new SignalAction(TEST_RETRIEVE_END);
        actRetrieveDone = new SignalAction(RETRIEVE_DONE, 2);
        addProcessor(actTestRetrieveEnd);
        addProcessor(actRetrieveDone);
        netRecog = new NetSeqRecognizer(NetSeqRecognizer.NAME_SEQRECOG_NET, layerPhonemePostInput, items, true, true);
        netRecog.initNetwork();
        netRecog.layerRetrieve.setNoise(Param.ps_noise);
        netPS = new NetPhonStoreMath(NetPhonStore.NAME_PHONSTORE_NET, layerPhonemePostInput, layerPhonemePostInput, phonemeSet, nPhonemeCapacity);
        netPS.initNetwork();
        addSubnetwork(netDR);
        addSubnetwork(netRecog);
        addSubnetwork(netPS);
        sigNoPhonemeInput = layerPhonemeInput.createStateSignal(SigQuant.ALL, SigOper.EQ, 0D);
        sigRecognizerInactive = netRecog.netSeqEncode.layerRep.createStateSignal(SigQuant.SUM, SigOper.EQ, 0D);
        sigNoItemInput = netDR.layerInput.createStateSignal(SigQuant.ALL, SigOper.EQ, 0D);
        projSensoryIn = new Projection(this, layerPhonemeInput, layerPhonemePostInput, ProjectionType.ONE_TO_ONE);
        projSensoryIn.setWtType(WtType.UNIFORM, 1D);
        projSensoryIn.connect();
        Projection projBusySensory = new Projection(this, rehearseState.layer, layerPhonemeInput.getGate().layer, ProjectionType.ONE_TO_ONE);
        projBusySensory.setWtType(WtType.UNIFORM, -10D);
        projBusySensory.connect();
        Projection projBusyItemInput = new Projection(this, rehearseState.layer, netDR.layerPostInput.getGate().layer, ProjectionType.ONE_TO_ONE);
        projBusyItemInput.setWtType(WtType.UNIFORM, -10D);
        projBusyItemInput.connect();
        Projection projRetrInput = new Projection(this, netRecog.layerRecog, netDR.layerPostInput, ProjectionType.ONE_TO_ONE);
        projRetrInput.setWtType(WtType.UNIFORM, 1D);
        projRetrInput.connect();
        projRecogDR = new Projection(this, netRecog.layerRetrieve, netDR.layerRetrieve, ProjectionType.ONE_TO_ONE);
        projRecogDR.setWtType(WtType.UNIFORM, Param.ps_wt);
        projRecogDR.connect();
        projRecogDR.setGainWt(1);
        netDR.layerRetrieve.setGainParameters(1, 1);
        projPRPI = new ProjectionGated(this, layerPhonemeRehearse, layerPhonemePostInput, ProjectionType.ONE_TO_ONE);
        projPRPI.setWtType(WtType.UNIFORM, 1D);
        projPRPI.getGate().setDefaultState(true);
        projPRPI.connect();
        projPRPO = new ProjectionGated(this, layerPhonemeRehearse, layerPhonemeReport, ProjectionType.ONE_TO_ONE);
        projPRPO.setWtType(WtType.UNIFORM, 1D);
        projPRPO.getGate().setDefaultState(false);
        projPRPO.connect();
        projPIPO = new ProjectionGated(this, layerPhonemePostInput, layerPhonemeReport, ProjectionType.ONE_TO_ONE);
        projPIPO.setWtType(WtType.UNIFORM, 1D);
        projPIPO.getGate().setDefaultState(false);
        projPIPO.connect();
        Projection projWriteInh = new Projection(this, rewriteState.layer, netDR.layerRetrieve, ProjectionType.ALL_TO_ALL);
        projWriteInh.setWtType(WtType.UNIFORM, 10D);
        projWriteInh.connect();
        Projection projPhonNoise = new Projection(this, layerPhonemePostInput, noisyState.layer, ProjectionType.ALL_TO_ALL);
        projPhonNoise.setWtType(WtType.UNIFORM, 10D);
        projPhonNoise.connect();
        Projection projPhonNoise2 = new Projection(this, layerPhonemeInput, noisyState.layer, ProjectionType.ALL_TO_ALL);
        projPhonNoise2.setWtType(WtType.UNIFORM, 10D);
        projPhonNoise2.connect();
        Projection projItemNoise = new Projection(this, netDR.layerPostInput, noisyState.layer, ProjectionType.ALL_TO_ALL);
        projItemNoise.setWtType(WtType.UNIFORM, 10D);
        projItemNoise.connect();
        Projection projPsReadState = new Projection(this, netPS.layerRWave, psreadState.layer, ProjectionType.ALL_TO_ALL);
        projPsReadState.setWtType(WtType.UNIFORM, 10D);
        projPsReadState.connect();
        projDRInOut = new ProjectionGated(this, netDR.layerInput, netDR.layerOutput, ProjectionType.ONE_TO_ONE);
        projDRInOut.setWtType(WtType.UNIFORM, 1D);
        projDRInOut.connect();
        Projection projRecodeProj = new Projection(this, canRecode.layer, projDRInOut.getGate().layer, ProjectionType.ONE_TO_ONE);
        projRecodeProj.setWtType(WtType.UNIFORM, 2D);
        projRecodeProj.connect();
        Projection projRecodeProjInh = new Projection(this, rehearseState.layer, projDRInOut.getGate().layer, ProjectionType.ONE_TO_ONE);
        projRecodeProjInh.setWtType(WtType.UNIFORM, -10D);
        projRecodeProjInh.connect();
        ActionAccumulator accInput = new ActionAccumulator(ActionAccumulator.INPUT_ACT);
        addProcessor(accInput);
        accOutput = new ActionAccumulator(LayerPhonemeOutput.ACTION_PHONOUT_NAME);
        addProcessor(accOutput);
        initSigProcessor();
    }

    protected void initSigProcessor() {
        SignalProcessor recode = initAudiRecode();
        Signal sigRetrieved = netDR.layerRetrieve.createSignal(SigQuant.SUM, SigOper.GREATER, 0D);
        SignalProcessorManual sigProc = new SignalProcessorManual("Training");
        addProgram(sigProc);
        sigProc.addSubprogram(netRecog.getProgram(NetSeqRecognizer.NAME_TRAIN_PROGRAM));
        sigProc = new SignalProcessorManual("Retr-Item");
        addProgram(sigProc);
        sigProc.addRule(Simulation.NAME_SIGNAL_TRIAL_BEGIN, projPRPI.getGate().getOffName());
        sigProc.addRule("RECALL", new ActionList(projPRPO.getGate().getOnName()));
        sigProc.addSubprogram(netDR.getProgram(NetDivReconvMath.NAME_ISR_PROGRAM));
        sigProc = new SignalProcessorManual("Retr-Phon");
        addProgram(sigProc);
        sigProc.addSubprogram(netPS.getProgram(NetPhonStore.NAME_RECALL_PROGRAM));
        sigProc.addRule("RECALL", new ActionList(projPRPI.getCloseActName(), projPIPO.getOpenActName(), projPRPO.getOpenActName()));
        sigProc = new SignalProcessorManual("Retr-Both");
        addProgram(sigProc);
        sigProc.addSubprogram(netRecog.getProgram("Testing"));
        sigProc.addSubprogram(recode);
        sigProc.addRule(Simulation.NAME_SIGNAL_TRIAL_BEGIN, netPS.layerWStarter.getOnAct());
        sigProc.addRule(RECALL, recallWaitState.getOnName());
        sigProc.addRule(new SignalList(recallWaitState.getOnSignal(), rehearseState.getOffSignal(), sigRecognizerInactive, sigNoItemInput, noisyState.getOffSignal()), new ActionList(recallWaitState.getOffName(), recallState.getOnName(), netPS.clearWWave.getActName(), actRetrieve.getActName(), projPRPO.getOpenActName(), projPRPI.getCloseActName()));
        sigProc.addRule("RETRIEVE", new ActionList(clearIOR.getActName(), netPS.clearRInh.getActName(), netPS.layerRWave.getOnActName(), netDR.retrieveInh.getOffName(), netDR.retrieveCtrl.getOnName()));
        sigProc.addRule(new SignalList(netRecog.sigNewItemRecognized, recallState.getOnSignal()), new ActionList(netDR.retrieveCtrl.getOffName(), netPS.clearRWave.getActName()));
        sigProc.addRule(new SignalList(netRecog.sigNewItemRecognized, rehearseState.getOnSignal()), new ActionList(netDR.retrieveCtrl.getOffName(), netPS.clearRWave.getActName()));
        sigProc.addRule(sigRetrieved, new ActionList(netPS.layerRWave.getOnActName(), netDR.retrieveCtrl.getOnName()));
        netPS.setUseWordBoudnarySignal(true);
        sigProc.addRule(netPS.sigWordBoundary, new ActionList(netPS.clearRWave.getAct(), netPS.layerRWave.getOffAct()));
        if (optimalRehearse) {
            SignalProcessorManual rehearseProg = initOptimalRehearse();
            sigProc.addSubprogram(rehearseProg);
        }
    }

    protected SignalProcessorManual initAudiRecode() {
        SignalProcessorManual recodeProg = new SignalProcessorManual(NAME_RECODE_PROGRAM);
        recodeProg.addRule(Param.RECODE, canRecode.getOnName());
        return recodeProg;
    }

    protected SignalProcessorManual initOptimalRehearse() {
        String TIMER_ID = "REHEARSE_TIMER";
        String REHEARSE_ENDER = "REHEARSE_END_CHECK";
        SignalProcessorManual rehearseProg = new SignalProcessorManual(NAME_REHEARSE_PROGRAM);
        TimerProcessor rehearseTimer = new TimerProcessor(TIMER_ID, 2000, true);
        rehearseTimer.setFluctuation(new AbstractRandomGenerator() {

            @Override
            public double getRand() {
                return Network.rand.nextGaussian() * 500;
            }
        });
        TimerProcessor rehearseEnder = new TimerProcessor(REHEARSE_ENDER, Param.tick * 2);
        addProcessor(rehearseTimer);
        addProcessor(rehearseEnder);
        rehearseProg.addRule(Simulation.NAME_SIGNAL_TRIAL_BEGIN, rehearseTimer.getStartName());
        rehearseProg.addRule(new SignalList(rehearseTimer.getGoSignal(), recallState.getOffSignal(), recallWaitState.getOffSignal(), noisyState.getOffSignal(), rehearseState.getOffSignal(), projPRPO.getGate().getOffSignal(), sigNoPhonemeInput, sigNoItemInput, netDR.netProducer.artiBusy.getOffSignal()), new ActionList(rehearseTimer.getOffName(), actRetrieve.getActName(), projPRPO.getCloseActName(), projPRPI.getOpenActName(), netPS.clearWWave.getActName(), netDR.netProducer.artiBlock.getOnName(), psreadState.getOnName(), rehearseState.getOnName(), layerPhonemeInput.getOffActName()));
        rehearseProg.addRule(new SignalList(rehearseState.getOnSignal(), psreadState.getOffSignal(), rewriteState.getOffSignal()), new ActionList(actRetrieveDone.getActName(), rewriteState.getOnName(), netPS.layerWStarter.getOnActName()));
        rehearseProg.addRule(new SignalList(rehearseState.getOnSignal(), RETRIEVE_DONE), new ActionList(netDR.netProducer.artiBlock.getOffName(), netPS.clearPhonemes.getActName(), netDR.retrieveInh.getOnName(), netDR.retrieveCtrl.getOnName()));
        ActionList rewriteDoneAct = new ActionList();
        rewriteDoneAct.add(5, rehearseState.getOffAct());
        rewriteDoneAct.add(5, layerPhonemeInput.getOnAct());
        rewriteDoneAct.add(5, rewriteState.getOffAct());
        rewriteDoneAct.add(5, rehearseTimer.getStartAct());
        rehearseProg.addRule(new SignalList(rewriteState.getOnSignal(), netDR.netProducer.artiDone.getOnSignal()), rewriteDoneAct);
        return rehearseProg;
    }

    @Override
    public void reset() {
        super.reset();
        projRecogDR.setWtType(WtType.UNIFORM, Param.ps_wt);
        projRecogDR.init();
        netRecog.layerRetrieve.setNoise(Param.ps_noise);
    }
}
