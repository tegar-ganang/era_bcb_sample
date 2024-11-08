package net.sf.opendf.hades.cal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.sf.opendf.cal.ast.Action;
import net.sf.opendf.cal.ast.Actor;
import net.sf.opendf.cal.ast.Decl;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.PortDecl;
import net.sf.opendf.cal.ast.QID;
import net.sf.opendf.cal.ast.Transition;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Executor;
import net.sf.opendf.cal.i2.InterpreterException;
import net.sf.opendf.cal.i2.Procedure;
import net.sf.opendf.cal.i2.environment.ConstantEnvironment;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.opendf.cal.i2.platform.DefaultTypedPlatform;
import net.sf.opendf.cal.i2.platform.DefaultUntypedPlatform;
import net.sf.opendf.cal.i2.types.Type;
import net.sf.opendf.cal.i2.types.TypeSystem;
import net.sf.opendf.cal.i2.util.ImportHandler;
import net.sf.opendf.cal.i2.util.ImportMapper;
import net.sf.opendf.cal.i2.util.ImportUtil;
import net.sf.opendf.cal.i2.util.Platform;
import net.sf.opendf.cal.interpreter.InputChannel;
import net.sf.opendf.cal.interpreter.InputPort;
import net.sf.opendf.cal.interpreter.OutputChannel;
import net.sf.opendf.cal.interpreter.OutputPort;
import net.sf.opendf.cal.interpreter.util.PriorityUtil;
import net.sf.opendf.hades.des.AbstractDiscreteEventComponent;
import net.sf.opendf.hades.des.AbstractMessageListener;
import net.sf.opendf.hades.des.BasicMessageProducer;
import net.sf.opendf.hades.des.ControlEvent;
import net.sf.opendf.hades.des.EventProcessor;
import net.sf.opendf.hades.des.schedule.PostfireHandler;
import net.sf.opendf.hades.des.schedule.Scheduler;
import net.sf.opendf.hades.des.schedule.SimulationFinalizer;
import net.sf.opendf.hades.des.util.Attributable;
import net.sf.opendf.hades.des.util.LocationMap;
import net.sf.opendf.hades.des.util.OutputBlockRecord;
import net.sf.opendf.hades.des.util.StateChangeEvent;
import net.sf.opendf.hades.des.util.StateChangeListener;
import net.sf.opendf.hades.des.util.StateChangeProvider;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.exception.LocatableException;
import static net.sf.opendf.util.Misc.deepCopy;

/**
 * 
 * @author jwj@acm.org
 */
public class CalInterpreterUntyped extends AbstractDiscreteEventComponent implements EventProcessor, LocationMap, StateChangeProvider {

    private static final Object LAST_FIRED_AT = "lastFiredAt";

    private Map locationMap = new HashMap();

    public Object getContent(Object location) {
        return locationMap.get(location);
    }

    public Set locations() {
        return locationMap.keySet();
    }

    public void initializeState(double t, Scheduler s) {
        try {
            scheduler = s;
            setupAssertionHandling();
            setupPortTypeChecking();
            warnBigBuffers = -1;
            String bufferSizeWarning = System.getProperty("CalBufferWarning");
            if (bufferSizeWarning != null) {
                try {
                    warnBigBuffers = Integer.parseInt(bufferSizeWarning);
                } catch (Exception exc) {
                }
            }
            ignoreBufferBounds = false;
            String ignoreBufferBoundString = System.getProperty("CalBufferIgnoreBounds");
            if (ignoreBufferBoundString != null && ignoreBufferBoundString.trim().toLowerCase().equals("true")) {
                ignoreBufferBounds = true;
            }
            globalTraceOn = false;
            String globalTraceOnString = System.getProperty("CalFiringTrace");
            if (globalTraceOnString != null && globalTraceOnString.trim().toLowerCase().equals("true")) {
                globalTraceOn = true;
            }
            bufferBlockRecord = false;
            String bufferBlockRecordString = System.getProperty("CalBufferBlockRecord");
            if (bufferBlockRecordString != null && bufferBlockRecordString.trim().toLowerCase().equals("true")) {
                bufferBlockRecord = true;
            }
            setupConstantEnvironment();
            outsideEnv = new EnvironmentWrapper(this.instantiationEnv, constantEnv);
            Decl[] decls = actor.getStateVars();
            DynamicEnvironmentFrame constantEnv = new DynamicEnvironmentFrame(outsideEnv);
            constantEnv.bind("this", this, null);
            this.actorEnv = createActorStateEnvironment(constantEnv);
            this.myInterpreter = new Executor(theConfiguration, this.actorEnv);
            hasTraceVar = false;
            hasNDTrackerVar = false;
            if (decls != null) {
                for (int i = 0; i < decls.length; i++) {
                    String var = decls[i].getName();
                    Expression valExpr = decls[i].getInitialValue();
                    TypeExpr te = decls[i].getType();
                    boolean isStateVariable = decls[i].isAssignable() || decls[i].isMutable();
                    Object value = (valExpr == null) ? null : myInterpreter.valueOf(valExpr, this.actorEnv);
                    TypeSystem ts = theConfiguration.getTypeSystem();
                    Type type = (ts != null) ? ts.evaluate(te, myInterpreter) : null;
                    if (isStateVariable) this.actorEnv.bind(var, value, type); else constantEnv.bind(var, value, type);
                    if (traceVarName.equals(var)) hasTraceVar = true;
                    if (nondeterminismTrackerVarName.equals(var)) hasNDTrackerVar = true;
                }
            }
            ai = new ActorInterpreter(actor, theConfiguration, this.actorEnv, inputPortMap, outputPortMap);
            executeInitializer();
            firingCount = 0;
            delayOutput = false;
            blockedOutputChannels = new HashSet();
            checkInvariants();
            checkFinalizer(constantEnv);
            scheduleActor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize actor '" + actor.getName() + "':" + e.getMessage(), e);
        }
    }

    private void checkFinalizer(Environment env) {
        try {
            final Procedure finalizeProcedure = (Procedure) this.actorEnv.getByName(FINALIZE_PROCEDURE);
            scheduler.registerSimulationFinalizer(new SimulationFinalizer() {

                public void finalizeSimulation() {
                    ((Procedure) finalizeProcedure).call(0, myInterpreter);
                }
            });
        } catch (Exception exc) {
        }
    }

    protected DynamicEnvironmentFrame createActorStateEnvironment(Environment parent) {
        return new DynamicEnvironmentFrame(parent);
    }

    private void executeInitializer() {
        Action[] actions = actor.getInitializers();
        for (int i = 0; i < actions.length; i++) {
            ai.actionSetup(actions[i]);
            if (ai.actionEvaluatePrecondition()) {
                ai.actionStep();
                ai.actionComputeOutputs();
                ai.actionClear();
                return;
            } else {
                ai.actionClear();
            }
        }
    }

    public boolean isInitialized() {
        return scheduler != null;
    }

    protected int eventProcessingState;

    protected final int epsActionSelection = 1;

    protected final int epsOutput = 2;

    protected final int epsDelayedOutput = 99;

    public boolean isWeakEvent() {
        return false;
    }

    public boolean processEvent(double time) {
        if (!blockedOutputChannels.isEmpty()) {
            flushBlockedOutputChannels();
            scheduleActor();
            return true;
        } else if (delayOutput) {
            delayOutput = false;
            flushOutputChannels();
            scheduleActor();
            nVoidFirings = 0;
            return true;
        } else {
            Action[] a = eligibleActions[currentState];
            for (int i = 0; i < a.length; i++) {
                try {
                    ai.actionSetup(a[i]);
                    if (ai.actionEvaluatePrecondition()) {
                        if (hasNDTrackerVar) {
                            ai.actionClear();
                            rollbackInputChannels();
                            List activeActions = new ArrayList();
                            for (int j = 0; j < a.length; j++) {
                                ai.actionSetup(a[j]);
                                if (ai.actionEvaluatePrecondition()) {
                                    activeActions.add(a[j]);
                                }
                                ai.actionClear();
                                rollbackInputChannels();
                            }
                            List enabledActions = new ArrayList();
                            for (int j = 0; j < activeActions.size(); j++) {
                                Action a1 = (Action) activeActions.get(j);
                                boolean enabled = true;
                                for (int k = j - 1; enabled && k >= 0; k--) {
                                    Action a2 = (Action) activeActions.get(k);
                                    if (PriorityUtil.isOrdered(a2, a1, actor.getPriorities())) {
                                        enabled = false;
                                    }
                                }
                                if (enabled) {
                                    enabledActions.add(a1);
                                }
                            }
                            if (enabledActions.size() != 1) {
                                String msg = "nondeterminism";
                                for (int j = 0; j < enabledActions.size(); j++) {
                                    if (j > 0) msg += ", ";
                                    Action ea = (Action) enabledActions.get(j);
                                    msg += ea.getID();
                                }
                                Logging.user().warning(msg);
                            }
                            ai.actionSetup(a[i]);
                            ai.actionEvaluatePrecondition();
                        }
                        nVoidFirings = 0;
                        firingCount += 1;
                        if (isTraceOn()) {
                            writeTraceInformation(a, i);
                        }
                        delayOutput = ai.hasDelay();
                        checkActionPreconditions(ai);
                        ai.actionStep();
                        checkActionPostconditions(ai);
                        double delay = ai.computeDelay();
                        ai.actionComputeOutputs();
                        ai.actionClear();
                        currentState = successorState[currentState][i];
                        commitInputChannels();
                        checkInvariants();
                        if (delayOutput) {
                            scheduleActorDelayed(delay);
                        } else {
                            flushOutputChannels();
                            scheduleActor();
                        }
                        animationPostfireHandler.modifiedAnimationState();
                        return true;
                    } else {
                        ai.actionClear();
                        rollbackInputChannels();
                    }
                } catch (Exception exc) {
                    String loc = actor.getName() + "', action " + a[i].getID() + " (" + a[i].getAttribute("text-begin-line") + "-" + a[i].getAttribute("text-end-line") + ")";
                    throw new LocatableException(exc, loc);
                }
            }
            nVoidFirings += 1;
            return false;
        }
    }

    private String actionDescription(Action a) {
        return "" + a.getID() + " (" + a.getAttribute("text-begin-line") + "-" + a.getAttribute("text-end-line") + ")" + ((a.getTag() != null) ? "---action tag: " + a.getTag().toString() : "");
    }

    private String actionDescription(Action[] a) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append("[");
            s.append(actionDescription(a[i]));
            s.append("]");
        }
        return s.toString();
    }

    protected void commitInputChannels() {
        for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
            mic.commit();
        }
    }

    protected void rollbackInputChannels() {
        for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
            mic.rollback();
        }
    }

    protected void flushOutputChannels() {
        for (Iterator i = outputPortMap.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            MosesOutputChannel moc = (MosesOutputChannel) outputPortMap.get(k);
            if (moc.flush()) {
                blockedOutputChannels.add(moc);
            }
        }
    }

    protected void flushBlockedOutputChannels() {
        Set stillBlocked = new HashSet();
        for (Iterator i = blockedOutputChannels.iterator(); i.hasNext(); ) {
            MosesOutputChannel moc = (MosesOutputChannel) i.next();
            if (moc.flush()) {
                stillBlocked.add(moc);
            }
        }
        blockedOutputChannels = stillBlocked;
    }

    protected void blockActor(String port) {
        if (hasTraceVar) {
            Object v = this.actorEnv.getByName(traceVarName);
            if (theConfiguration.booleanValue(v)) {
                Logging.user().info("<block actor='" + actor.getName() + "' port='" + port + "'/>");
            }
        }
        if (!bufferBlockRecord) return;
        blockedStep = scheduler.currentEventCount();
        blockedTime = scheduler.currentTime();
        Collection<OutputBlockRecord> obr = getOBR();
        if (!obr.contains(myOBR)) obr.add(myOBR);
    }

    protected void unblockActor(String port) {
        if (hasTraceVar) {
            Object v = this.actorEnv.getByName(traceVarName);
            if (theConfiguration.booleanValue(v)) {
                Logging.user().info("<unblock actor='" + actor.getName() + "' port='" + port + "'/>");
            }
        }
        if (bufferBlockRecord) {
            Collection<OutputBlockRecord> obr = getOBR();
            obr.remove(myOBR);
        }
    }

    private Collection<OutputBlockRecord> getOBR() {
        Collection<OutputBlockRecord> obr = (Collection<OutputBlockRecord>) scheduler.getProperty("OutputBlockRecords");
        if (obr == null) {
            obr = new HashSet<OutputBlockRecord>();
            scheduler.setProperty("OutputBlockRecords", obr);
        }
        return obr;
    }

    public CalInterpreterUntyped(Actor a, Map outsideEnv) {
        Set<String> undefinedParameters = new HashSet<String>();
        for (Decl parDecl : a.getParameters()) {
            if (!outsideEnv.containsKey(parDecl.getName())) {
                undefinedParameters.add(parDecl.getName());
            }
        }
        if (!undefinedParameters.isEmpty()) {
            String undefs = null;
            for (String s : undefinedParameters) {
                undefs = (undefs == null) ? s : (undefs + ", " + s);
            }
            String actorName = ("".equals(a.getPackage())) ? a.getName() : (a.getPackage() + "." + a.getName());
            throw new InterpreterException("Undefined parameters in actor '" + actorName + "': " + undefs);
        }
        actor = (Actor) deepCopy(a);
        this.instantiationEnv = outsideEnv;
        PortDecl[] pd = actor.getInputPorts();
        inputPortMap = new HashMap();
        for (int i = 0; i < pd.length; i++) {
            MosesInputChannel mic = createInputChannel(pd[i].getName());
            inputPortMap.put(pd[i].getName(), mic);
            inputs.addConnector(pd[i].getName(), mic);
        }
        pd = actor.getOutputPorts();
        outputPortMap = new HashMap();
        for (int i = 0; i < pd.length; i++) {
            MosesOutputChannel moc = createOutputChannel(pd[i].getName());
            outputPortMap.put(pd[i].getName(), moc);
            outputs.addConnector(pd[i].getName(), moc);
        }
        scheduler = null;
        ai = null;
        actions = PriorityUtil.prioritySortActions(actor);
        actionCoverSets = PriorityUtil.computeActionPriorities(actor);
        setupDFA();
        locationMap.put(new Integer(0), this);
        animationPostfireHandler = new AnimationPostfireHandler();
    }

    protected MosesInputChannel createInputChannel(String name) {
        return new MosesInputChannel(name);
    }

    protected MosesOutputChannel createOutputChannel(String name) {
        return new MosesOutputChannel(name);
    }

    /**
	 * Produce the actor state variables as a map from the variable names to their
	 * values.
	 */
    public Map getActorStateVariables() {
        return actorEnv.localBindings();
    }

    /**
	 * Produce the current state of the actor scheduler FSM. Note that in general 
	 * this state corresponds to a set of states, since the FSM may be non-deterministic. 
	 * For deterministic FSMs, the returned set will always contain exactly one element.
	 * <p>
	 * The elements of the set are the names of the states of the FSM, as String objects. 
	 */
    public Set getSchedulerState() {
        if (ndaStateSets == null) {
            return Collections.EMPTY_SET;
        } else {
            return Collections.unmodifiableSet(ndaStateSets[currentState]);
        }
    }

    public Map getInputQueues() {
        Map m = new HashMap();
        for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            m.put(i, ((MosesInputChannel) inputPortMap.get(i)).getTokenList());
        }
        return m;
    }

    /**
	 * Produce the environment containing all variables with actor scope, and in
	 * particular the actor state variables.
	 */
    public Environment getActorEnvironment() {
        return actorEnv;
    }

    public long getFiringCount() {
        return firingCount;
    }

    public String getName() {
        return actor.getName() + "@" + this.hashCode();
    }

    protected double computeSchedulePriority() {
        if (inputPortMap.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        if (tokensQueued == 0) return 101;
        double prio = (nVoidFirings > 99) ? 99 : nVoidFirings;
        prio += (tokensQueued == 0) ? 1.0 : (1.0 / tokensQueued);
        prio += rand.nextDouble() - 0.5;
        return prio;
    }

    private Random rand = new Random();

    protected void scheduleActor() {
        if (!delayOutput && blockedOutputChannels.isEmpty()) {
            scheduler.schedule(scheduler.currentTime(), computeSchedulePriority(), this);
        }
    }

    protected void scheduleActorDelayed(double delay) {
        scheduler.schedule(scheduler.currentTime() + delay, computeSchedulePriority(), this);
    }

    protected void scheduleActorForOutputFlushing() {
        double schedulePriority = computeSchedulePriority();
        scheduler.schedule(scheduler.currentTime(), schedulePriority, this);
    }

    /**
	 * Convert the nondeterministic schedule automaton into a deterministic one (DFA).
	 * The data to represent the DFA is stored in a few private members.
	 * 
	 * @see #currentState
	 * @see #eligibleActions
	 * @see #successorState
	 */
    private void setupDFA() {
        currentState = 0;
        if (actor.getScheduleFSM() == null) {
            eligibleActions = new Action[][] { actions };
            successorState = new int[1][actions.length];
            for (int i = 0; i < actions.length; i++) {
                successorState[0][i] = 0;
            }
            return;
        }
        Set stateSets = new HashSet();
        Set initialState = Collections.singleton(actor.getScheduleFSM().getInitialState());
        stateSets.add(initialState);
        int previousSize = 0;
        while (previousSize != stateSets.size()) {
            previousSize = stateSets.size();
            for (int i = 0; i < actions.length; i++) {
                Set nextStates = new HashSet();
                for (Iterator j = stateSets.iterator(); j.hasNext(); ) {
                    Set s = (Set) j.next();
                    if (isEligibleAction(s, actions[i])) {
                        nextStates.add(computeNextStateSet(s, actions[i]));
                    }
                }
                stateSets.addAll(nextStates);
            }
        }
        ndaStateSets = (Set[]) new ArrayList(stateSets).toArray(new Set[stateSets.size()]);
        for (int i = 0; i < ndaStateSets.length; i++) {
            if (ndaStateSets[i].equals(initialState)) {
                Set s = ndaStateSets[i];
                ndaStateSets[i] = ndaStateSets[0];
                ndaStateSets[0] = s;
            }
        }
        eligibleActions = new Action[ndaStateSets.length][];
        successorState = new int[ndaStateSets.length][];
        for (int i = 0; i < ndaStateSets.length; i++) {
            List ea = new ArrayList();
            List ss = new ArrayList();
            for (int j = 0; j < actions.length; j++) {
                if (isEligibleAction(ndaStateSets[i], actions[j])) {
                    ea.add(actions[j]);
                    ss.add(computeNextStateSet(ndaStateSets[i], actions[j]));
                }
            }
            eligibleActions[i] = (Action[]) ea.toArray(new Action[ea.size()]);
            List ds = Arrays.asList(ndaStateSets);
            successorState[i] = new int[ss.size()];
            for (int j = 0; j < ss.size(); j++) {
                successorState[i][j] = ds.indexOf(ss.get(j));
                assert successorState[i][j] >= 0;
            }
        }
    }

    private boolean isEligibleAction(Set css, Action a) {
        QID tag = a.getTag();
        if (tag != null && css != null) {
            if (actor.getScheduleFSM() == null) return true;
            Transition[] ts = actor.getScheduleFSM().getTransitions();
            for (int i = 0; i < ts.length; i++) {
                Transition t = ts[i];
                if (css.contains(t.getSourceState()) && isPrefixedByTagList(tag, t.getActionTags())) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private Set computeNextStateSet(Set css, Action a) {
        if (css == null) {
            return null;
        }
        if (a == null || a.getTag() == null) {
            return css;
        }
        Set ns = new HashSet();
        QID tag = a.getTag();
        Transition[] ts = actor.getScheduleFSM().getTransitions();
        for (int i = 0; i < ts.length; i++) {
            Transition t = ts[i];
            if (css.contains(t.getSourceState()) && isPrefixedByTagList(tag, t.getActionTags())) {
                ns.add(t.getDestinationState());
            }
        }
        return ns;
    }

    private boolean isPrefixedByTagList(QID tag, QID[] tags) {
        for (int j = 0; j < tags.length; j++) {
            if (tags[j].isPrefixOf(tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTraceOn() {
        if (hasTraceVar) {
            Object v = this.actorEnv.getByName(traceVarName);
            if (theConfiguration.booleanValue(v)) {
                return true;
            }
        }
        if (globalTraceOn) {
            return true;
        }
        return false;
    }

    private void writeTraceInformation(Action[] a, int i) {
        Logging.user().info("<trace actor='" + actor.getName() + "' action='" + a[i].getID() + "'>" + "    " + actionDescription(a[i]) + "\n    states: " + ((ndaStateSets != null) ? ndaStateSets[currentState].toString() : "<empty>") + "\n    position: " + i + "\n    eligible actions:" + actionDescription(a) + "</trace>");
    }

    private void setupPortTypeChecking() {
    }

    private void setupAssertionHandling() {
        String assertions = System.getProperty("EnableAssertions");
        if (assertions != null) {
            assertions = assertions.trim().toLowerCase();
        }
        checkAssertions = false;
        if ("true".equals(assertions) || "on".equals(assertions)) {
            checkAssertions = true;
            AssertionHandler ah = (AssertionHandler) scheduler.getProperty("CalAssertionHandler");
            if (ah == null) {
                ah = new LoggingAssertionHandler();
                scheduler.setProperty("CalAssertionHandler", ah);
            }
            assertionHandler = ah;
        }
    }

    private void checkInvariants() {
        if (!checkAssertions) return;
        Expression[] invariants = actor.getInvariants();
        if (invariants == null) return;
        for (int i = 0; i < invariants.length; i++) {
            Object res = myInterpreter.valueOf(invariants[i]);
            if (!theConfiguration.booleanValue(res)) {
                reportAssertionFailure("Invariant #" + i, "Failed invariant.");
            }
        }
    }

    private void checkActionPreconditions(ActorInterpreter ai) {
        if (!checkAssertions) return;
        Action a = ai.currentAction();
        Expression[] pc = a.getPreconditions();
        if (pc == null) return;
        for (int i = 0; i < pc.length; i++) {
            Object res = ai.evaluateExpression(pc[i]);
            if (!theConfiguration.booleanValue(res)) {
                reportAssertionFailure("Action precondition " + a.getID() + "." + i, "Failed precondition.");
            }
        }
    }

    private void checkActionPostconditions(ActorInterpreter ai) {
        if (!checkAssertions) return;
        Action a = ai.currentAction();
        Expression[] pc = a.getPostconditions();
        if (pc == null) return;
        for (int i = 0; i < pc.length; i++) {
            Object res = ai.evaluateExpression(pc[i]);
            if (!theConfiguration.booleanValue(res)) {
                reportAssertionFailure("Action postcondition " + a.getID() + "." + i, "Failed postcondition.");
            }
        }
    }

    private void reportAssertionFailure(String assertionLocus, String message) {
        AssertionHandler ah = (AssertionHandler) scheduler.getProperty("CalAssertionHandler");
        if (ah == null) return;
        ah.assertionFailed(this, assertionLocus, getFiringCount(), null, message);
    }

    private void setupConstantEnvironment() {
        if (constantEnv == null) {
            ImportHandler[] importHandlers = thePlatform.getImportHandlers(CalInterpreterUntyped.class.getClassLoader());
            ImportMapper[] importMappers = thePlatform.getImportMappers();
            Environment env = ImportUtil.handleImportList(thePlatform.createGlobalEnvironment(), importHandlers, actor.getImports(), importMappers);
            constantEnv = new ConstantEnvironment(env);
        }
    }

    protected Actor actor;

    protected Action[] actions;

    protected Map actionCoverSets;

    protected boolean checkAssertions = false;

    protected AssertionHandler assertionHandler = null;

    protected static final Platform thePlatform = DefaultUntypedPlatform.thePlatform;

    protected static final Configuration theConfiguration = thePlatform.configuration();

    /**
	 * This environment contains the global constants surrounding this actor, including
	 * those resulting from the import clauses.
	 */
    private ConstantEnvironment constantEnv;

    /**
	 * The instantiationEnv map contains the parameters passed into this actor
	 * upon instantiation.
	 */
    private Map instantiationEnv;

    /**
	 * This environment contains all variables defined outside the actor, including the 
	 * bindings for the actor parameters. All bindings in this environment should be 
	 * treated as immutable.
	 * <p>
	 * The reason for this environment to be a member variable is that it is 
	 * built in the constructor, while the actor state variables will not be initialized
	 * until the call to initialize.
	 */
    protected Environment outsideEnv;

    /**
	 * This environment contains all bindings with actor scope. The local frame contains 
	 * the actor state variables.
	 */
    protected DynamicEnvironmentFrame actorEnv;

    private boolean globalTraceOn;

    private boolean hasTraceVar;

    private boolean hasNDTrackerVar;

    private int warnBigBuffers;

    private boolean ignoreBufferBounds;

    private boolean bufferBlockRecord;

    private static final String traceVarName = "_CAL_traceOutput";

    private static final String nondeterminismTrackerVarName = "_CAL_trackND";

    private static final String FINALIZE_PROCEDURE = "__CAL_Finalize";

    protected Scheduler scheduler;

    protected ActorInterpreter ai;

    protected Executor myInterpreter;

    private OutputBlockRecord myOBR = new OutputBlockRecord() {

        public String getComponentName() {
            return getName();
        }

        public Collection<String> getBlockedOutputConnectors() {
            List<String> ports = new ArrayList<String>();
            for (MosesOutputChannel moc : blockedOutputChannels) {
                ports.add(moc.getName());
            }
            return ports;
        }

        public Map<String, Collection<Object>> getBlockingSourceMap() {
            Map<String, Collection<Object>> sources = new HashMap<String, Collection<Object>>();
            for (MosesOutputChannel moc : blockedOutputChannels) {
                sources.put(moc.getName(), moc.getBlockingSources());
            }
            return sources;
        }

        public long getStepNumber() {
            return blockedStep;
        }

        public double getTime() {
            return blockedTime;
        }
    };

    protected AnimationPostfireHandler animationPostfireHandler = new AnimationPostfireHandler();

    /**
	 * 
	 */
    protected boolean delayOutput = false;

    protected Set<MosesOutputChannel> blockedOutputChannels = new HashSet<MosesOutputChannel>();

    protected long blockedStep;

    protected double blockedTime;

    /**
	 * The current scheduler state in the NDA. Each state represents a (non-empty) set 
	 * of states of the original scheduler FSM.
	 */
    protected int currentState;

    /**
	 * Contains for each state an array of Actions (topologically sorted to respect the
	 * priority order) that are eligible in that state.
	 */
    protected Action[][] eligibleActions;

    /**
	 * Identifies for each state, and each action eligible in that state, the follow-up
	 * state.
	 */
    protected int[][] successorState;

    /**
	 * Identifies the set of original schedule FSM states for each state in the NDA.
	 */
    protected Set[] ndaStateSets;

    protected Map<String, MosesInputChannel> inputPortMap;

    protected Map<String, MosesOutputChannel> outputPortMap;

    private int tokensQueued;

    private int nVoidFirings = 0;

    private long firingCount = 0;

    private Set listeners = null;

    private Object lastState;

    public boolean hasStateChangeListeners() {
        return listeners == null || listeners.isEmpty();
    }

    public void addStateChangeListener(StateChangeListener listener) {
        if (listeners == null) listeners = new HashSet();
        listeners.add(listener);
    }

    public void removeStateChangeListener(StateChangeListener listener) {
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) listeners = null;
    }

    public void requestStateInformation() {
        sendStateChangeEvent(new StateChangeEvent(this, "CalActorState", Double.NaN, null, lastState == null ? this.getStateInformation() : lastState));
    }

    public void notifyStateChange(double t, Object a) {
        if (listeners == null) {
            return;
        }
        try {
            StateChangeEvent sce;
            sce = new StateChangeEvent(this, "CalActorState", t, lastState, a);
            lastState = a;
            sendStateChangeEvent(sce);
        } catch (Throwable e) {
        }
    }

    public Object getStateInformation() {
        Map stateMap = new HashMap();
        Map tokenMap = new HashMap();
        for (Iterator i = inputPortMap.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            MosesInputChannel mic = (MosesInputChannel) inputPortMap.get(k);
            tokenMap.put(mic.getName(), mic.getTokenList());
        }
        stateMap.put("tokenMap", tokenMap);
        stateMap.put("currentEvent", new Long(scheduler.currentStrongEventCount()));
        stateMap.put("currentTime", new Double(scheduler.currentTime()));
        stateMap.put("actorState", getActorStateVariables());
        stateMap.put("schedulerState", getSchedulerState());
        stateMap.put("actionsFired", getActionsFired());
        List el = Arrays.asList(eligibleActions[currentState]);
        Map actionAssociations = new HashMap();
        Set enabledActions = new HashSet();
        for (int i = 0; i < actor.getActions().length; i++) {
            Action a = actor.getActions()[i];
            int state = 0;
            if (el.contains(a)) {
                state++;
                ai.actionSetup(a);
                if (ai.actionEvaluateTokenAvailability()) {
                    state++;
                    if (ai.actionEvaluateGuard()) {
                        enabledActions.add(a);
                        state++;
                    }
                }
                ai.actionClear();
                rollbackInputChannels();
            }
            Set cover = new HashSet((Set) actionCoverSets.get(a));
            cover.retainAll(enabledActions);
            boolean isCovered = !cover.isEmpty();
            Object[] info = { new Integer(state), new Boolean(isCovered) };
            actionAssociations.put(a, info);
        }
        stateMap.put("actionMap", actionAssociations);
        return stateMap;
    }

    private List actionsFired = new ArrayList();

    private List getActionsFired() {
        return actionsFired;
    }

    private void sendStateChangeEvent(StateChangeEvent sce) {
        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            StateChangeListener scl = (StateChangeListener) i.next();
            scl.stateChange(sce);
        }
    }

    protected void resetEvents(double t) {
        notifyStateChange(t, this.getStateInformation());
    }

    protected class MosesInputChannel extends AbstractMessageListener implements InputPort, InputChannel, Attributable {

        public String getName() {
            return name;
        }

        public boolean isMultiport() {
            return false;
        }

        public int width() {
            return 1;
        }

        public InputChannel getChannel(int n) {
            if (n != 0) {
                throw new RuntimeException("Getting channel >0 from SingleInputPort.");
            }
            return this;
        }

        public boolean hasAvailable(int n) {
            int avail = tokens.size();
            if ((!ignoreBufferBounds) && bufferSize > 0 && bufferSize < avail) {
                avail = bufferSize;
            }
            return n <= avail;
        }

        public Object get(int n) {
            if (hasAvailable(n + 1)) {
                tokensRead = Math.max(tokensRead, n + 1);
                return tokens.get(n);
            } else {
                throw new RuntimeException("CalInterpreter '" + actor.getName() + "': Read token beyond end of queue.");
            }
        }

        public void commit() {
            boolean full = bufferFull();
            tokens.deleteRange(0, tokensRead);
            tokensQueued -= tokensRead;
            if (warnBigBuffers > 0 && tokens.size() > warnBigBuffers) Logging.user().warning("Channel '" + actor.getName() + "." + name + "', big queue (R): " + (tokens.size() + tokensRead) + " - " + tokensRead);
            tokensRead = 0;
            animationPostfireHandler.modifiedAnimationState();
            if (full && !bufferFull()) {
                notifyControl(ControlEvent.UNBLOCK, this);
            }
        }

        public void rollback() {
            tokensRead = 0;
        }

        public boolean set(Object name, Object value) {
            if (attrBufferSize.equals(name) && value instanceof Integer) {
                bufferSize = ((Integer) value).intValue();
                if (bufferSize == 0) bufferSize = 1;
                return true;
            }
            return false;
        }

        public void message(Object msg, double time, Object source) {
            tokens.add(msg);
            try {
                if (listener != null) {
                    listener.notify(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Uh-oh");
            }
            tokensQueued += 1;
            scheduleActor();
            animationPostfireHandler.modifiedAnimationState();
            if (warnBigBuffers > 0 && tokens.size() > warnBigBuffers) Logging.user().warning("Channel '" + actor.getName() + "." + name + "', big queue (W): " + tokensQueued);
            if (bufferFull()) {
                notifyControl(ControlEvent.BLOCK, this);
            }
        }

        public MosesInputChannel(String name) {
            tokensRead = 0;
            this.name = name;
            tokens = new MyArrayList();
            callbacks = new MyArrayList();
            bufferSize = -1;
            listener = null;
        }

        public List getTokenList() {
            return Collections.unmodifiableList(tokens);
        }

        protected boolean bufferFull() {
            if (ignoreBufferBounds || bufferSize <= 0) return false;
            return tokens.size() > bufferSize;
        }

        public void setTokenListener(TokenListener tl) {
            listener = tl;
        }

        public String toString() {
            return actor.getName() + "." + name;
        }

        protected MyArrayList tokens;

        protected MyArrayList callbacks;

        protected int tokensRead;

        protected String name;

        protected int bufferSize;

        protected TokenListener listener;

        private final String attrBufferSize = "bufferSize";
    }

    protected class MosesOutputChannel extends BasicMessageProducer implements OutputPort, OutputChannel {

        @Override
        public void control(Object ce, Object source) {
            if (ce == ControlEvent.BLOCK) {
                if (source == null) {
                    Logging.dbg().info(portQID() + ": Received anonymous UNBLOCK event. Actor was " + (blocked ? "blocked" : "unblocked") + ".");
                }
                blocked = true;
                if (source != null) {
                    blockingSources.add(source);
                }
                blockActor(getName());
            } else if (ce == ControlEvent.UNBLOCK) {
                if (source == null) {
                    Logging.dbg().info(portQID() + ": Received anonymous UNBLOCK event. Actor was " + (blocked ? "blocked" : "unblocked") + ".");
                }
                if (blocked) {
                    if (source == null) {
                        blockingSources.clear();
                    } else {
                        blockingSources.remove(source);
                    }
                    if (blockingSources.isEmpty()) {
                        blocked = false;
                        unblockActor(getName());
                        scheduleActorForOutputFlushing();
                    }
                } else {
                    Logging.dbg().warning(portQID() + ": Received unblocking event while not blocked.");
                }
            } else {
                Logging.dbg().warning(portQID() + ": Received unidentified control event.");
            }
        }

        private String portQID() {
            return CalInterpreterUntyped.this.getName() + "." + this.getName();
        }

        public OutputChannel getChannel(int n) {
            if (n != 0) {
                throw new RuntimeException("Getting channel >0 from SingleOutputPort.");
            }
            return this;
        }

        public String getName() {
            return name;
        }

        public boolean isMultiport() {
            return false;
        }

        public int width() {
            return 1;
        }

        public Collection<Object> getBlockingSources() {
            return blockingSources;
        }

        public void put(Object a) {
            try {
                if (listener != null) {
                    listener.notify(a);
                }
            } catch (Exception e) {
            }
            tokens.add(a);
        }

        /**
		 * @return True, if blocked.
		 */
        public boolean flush() {
            int n = 0;
            while (!blocked && n < tokens.size()) {
                Object a = tokens.get(n);
                super.notifyMessage(a, scheduler.currentTime(), CalInterpreterUntyped.this);
                n += 1;
            }
            tokens.deleteRange(0, n);
            return blocked;
        }

        public void setTokenListener(TokenListener tl) {
            listener = tl;
        }

        public MosesOutputChannel(String name) {
            this.name = name;
            this.blocked = false;
            listener = null;
        }

        protected TokenListener listener;

        protected String name;

        protected MyArrayList tokens = new MyArrayList();

        protected boolean blocked;

        protected Collection<Object> blockingSources = new HashSet<Object>();
    }

    interface TokenListener {

        void notify(Object token);
    }

    /**
	 * Creating this subclass of ArrayList is unfortunate but necessary, because we want to use the
	 * {@link ArrayList#removeRange removeRange} method of {@link ArrayList}, and it 
	 * happens to be protected for some reason.
	 */
    protected static class MyArrayList extends ArrayList {

        public void deleteRange(int a, int b) {
            removeRange(a, b);
        }

        public MyArrayList() {
            super();
        }
    }

    /**
	 * An animationPostfireHandler records the relevant (to the animation) changes to 
	 * the CalInterpreter during a firing, and registers itself with the scheduler as a
	 * PostfireHandler. During the postfire phase, it emits the corresponding animation
	 * events.
	 * 
	 * @see PostfireHandler
	 * 
	 */
    protected class AnimationPostfireHandler implements PostfireHandler {

        public void postfire() {
            notifyStateChange(scheduler.currentTime(), getStateInformation());
        }

        public void modifiedAnimationState() {
            if (listeners != null) scheduler.addPostfireHandler(this);
        }
    }

    /**
	 * 
	 */
    public interface AssertionHandler {

        void assertionFailed(Object component, Object location, long step, Object assertion, String message);
    }

    class LoggingAssertionHandler implements AssertionHandler {

        public void assertionFailed(Object component, Object location, long step, Object assertion, String message) {
            String s = "Assertion failed. --- ";
            s += "[at: " + component + "::" + location + ", during step #" + step + "]";
            if (message != null) {
                s += ": " + message;
            }
            Logging.user().warning(s);
        }
    }
}
