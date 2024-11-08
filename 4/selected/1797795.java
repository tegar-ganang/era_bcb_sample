package net.sf.opendf.hades.cal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sf.opendf.cal.ast.Action;
import net.sf.opendf.cal.ast.Actor;
import net.sf.opendf.cal.ast.Decl;
import net.sf.opendf.cal.ast.Expression;
import net.sf.opendf.cal.ast.InputPattern;
import net.sf.opendf.cal.ast.OutputExpression;
import net.sf.opendf.cal.ast.Statement;
import net.sf.opendf.cal.ast.TypeExpr;
import net.sf.opendf.cal.ast.PortDecl;
import net.sf.opendf.cal.i2.Configuration;
import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Executor;
import net.sf.opendf.cal.i2.InterpreterException;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.opendf.cal.i2.environment.Thunk;
import net.sf.opendf.cal.i2.platform.DefaultTypedPlatform;
import net.sf.opendf.cal.i2.types.Type;
import net.sf.opendf.cal.i2.types.TypeSystem;
import net.sf.opendf.cal.i2.util.Platform;
import net.sf.opendf.cal.interpreter.InputChannel;
import net.sf.opendf.cal.interpreter.InputPort;
import net.sf.opendf.cal.interpreter.OutputChannel;
import net.sf.opendf.cal.interpreter.OutputPort;

public class ActorInterpreter {

    protected static final Platform thePlatform = DefaultTypedPlatform.thePlatform;

    protected static final Configuration theConfiguration = thePlatform.configuration();

    /**
	 * Set up the local environment of the specified action. This
	 * method is the only way a new action may be set up for
	 * execution. If completed successfully, the action environment
	 * and the action are stored in the state of this interpreter, and
	 * other methods may operate on them.
	 *
	 * @param action The action to setup.
	 */
    public void actionSetup(Action action) {
        assert envAction == null;
        env = null;
        envAction = null;
        final DynamicEnvironmentFrame local = new DynamicEnvironmentFrame(actorEnv);
        Executor myInterpreter = new Executor(theConfiguration, this.actorEnv);
        TypeSystem ts = theConfiguration.getTypeSystem();
        final InputPattern[] inputPatterns = action.getInputPatterns();
        for (int i = 0; i < inputPatterns.length; i++) {
            final InputPattern inputPattern = inputPatterns[i];
            final String[] vars = inputPattern.getVariables();
            final Expression repExpr = inputPattern.getRepeatExpr();
            if (repExpr == null) {
                for (int j = 0; j < vars.length; j++) {
                    final InputChannel channel = ((InputPort) (inputPortMap.get(inputPattern.getPortname()))).getChannel(0);
                    local.bind(vars[j], new SingleTokenReaderThunk(channel, j), null);
                }
            } else {
                Thunk repExprThunk = new Thunk(repExpr, interpreter, local);
                local.bind(new EnvironmentKey(inputPattern.getPortname()), repExprThunk, null);
                for (int j = 0; j < vars.length; j++) {
                    final InputChannel channel = ((InputPort) (inputPortMap.get(inputPattern.getPortname()))).getChannel(0);
                    local.bind(vars[j], new MultipleTokenReaderThunk(channel, j, vars.length, repExprThunk, configuration), null);
                }
            }
        }
        final Decl[] decls = action.getDecls();
        for (int i = 0; i < decls.length; i++) {
            final Expression v = decls[i].getInitialValue();
            TypeExpr te = decls[i].getType();
            Type type = (ts != null) ? ts.evaluate(te, myInterpreter) : null;
            if (v == null) {
                local.bind(decls[i].getName(), null, type);
            } else {
                local.bind(decls[i].getName(), new Thunk(v, interpreter, local, type), type);
            }
        }
        env = local;
        envAction = action;
    }

    /**
	 * Evaluate the preconditions for the action and return its
	 * result. If this method returns false, some condition required
	 * for the successful completion of the action is not
	 * satisfied. This might be an insufficient number of input
	 * tokens, or some other condition depending on the value of the
	 * tokens or state variables.
	 *
	 * <p> Note that the converse need not hold---depending on the
	 * model of computation, a true return value does not necessarily
	 * imply that the action will successfully execute. It may
	 * represent an <em>approximation</em> to a complete precondition,
	 * in which case the model of computation is not
	 * <em>responsible</em>.
	 *
	 * @return True, if the action precondition was satisfied.
	 * @exception net.sf.opendf.cal.interpreter.InterpreterException If the
	 * evaluation of the guards could not be successfully completed.
	 */
    public boolean actionEvaluatePreconditionOriginal() {
        if (envAction == null) {
            throw new InterpreterException("DataflowActorInterpreter: Must call actionSetup() " + "before calling actionEvaluatePrecondition().");
        }
        final Action action = envAction;
        final InputPattern[] inputPatterns = action.getInputPatterns();
        for (int i = 0; i < inputPatterns.length; i++) {
            final InputPattern inputPattern = inputPatterns[i];
            final InputChannel channel = ((InputPort) (inputPortMap.get(inputPattern.getPortname()))).getChannel(0);
            if (inputPattern.getRepeatExpr() == null) {
                if (!channel.hasAvailable(inputPattern.getVariables().length)) {
                    return false;
                }
            } else {
                int repeatVal = configuration.intValue(env.getByName(new EnvironmentKey(inputPattern.getPortname())));
                if (!channel.hasAvailable(inputPattern.getVariables().length * repeatVal)) {
                    return false;
                }
            }
        }
        final Expression[] guards = action.getGuards();
        for (int i = 0; i < guards.length; i++) {
            final Object g = interpreter.valueOf(guards[i], env);
            if (!configuration.booleanValue(g)) {
                return false;
            }
        }
        return true;
    }

    public boolean actionEvaluateTokenAvailability() {
        if (envAction == null) {
            throw new InterpreterException("DataflowActorInterpreter: Must call actionSetup() " + "before calling actionEvaluatePrecondition().");
        }
        final Action action = envAction;
        final InputPattern[] inputPatterns = action.getInputPatterns();
        for (int i = 0; i < inputPatterns.length; i++) {
            final InputPattern inputPattern = inputPatterns[i];
            final InputChannel channel = ((InputPort) (inputPortMap.get(inputPattern.getPortname()))).getChannel(0);
            if (inputPattern.getRepeatExpr() == null) {
                if (!channel.hasAvailable(inputPattern.getVariables().length)) {
                    return false;
                }
            } else {
                int repeatVal = configuration.intValue(env.getByName(new EnvironmentKey(inputPattern.getPortname())));
                if (!channel.hasAvailable(inputPattern.getVariables().length * repeatVal)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean actionEvaluateGuard() {
        if (envAction == null) {
            throw new InterpreterException("DataflowActorInterpreter: Must call actionSetup() " + "before calling actionEvaluatePrecondition().");
        }
        final Action action = envAction;
        final Expression[] guards = action.getGuards();
        for (int i = 0; i < guards.length; i++) {
            final Object g = interpreter.valueOf(guards[i], env);
            if (!configuration.booleanValue(g)) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Evaluate the preconditions for the action and return its
	 * result. If this method returns false, some condition required
	 * for the successful completion of the action is not
	 * satisfied. This might be an insufficient number of input
	 * tokens, or some other condition depending on the value of the
	 * tokens or state variables.
	 *
	 * <p> Note that the converse need not hold---depending on the
	 * model of computation, a true return value does not necessarily
	 * imply that the action will successfully execute. It may
	 * represent an <em>approximation</em> to a complete precondition,
	 * in which case the model of computation is not
	 * <em>responsible</em>.
	 *
	 * @return True, if the action precondition was satisfied.
	 * @exception net.sf.opendf.cal.interpreter.InterpreterException If the
	 * evaluation of the guards could not be successfully completed.
	 */
    public boolean actionEvaluatePrecondition() {
        return actionEvaluateTokenAvailability() && actionEvaluateGuard();
    }

    /**
	 * Returns true if the current action has a delay specification associated with it.
	 *
	 */
    public boolean hasDelay() {
        return envAction.getDelay() != null;
    }

    public double computeDelay() {
        Expression eDelay = envAction.getDelay();
        if (eDelay == null) return 0;
        Object v = interpreter.valueOf(eDelay, env);
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d < 0) throw new RuntimeException("Delay is negative: " + d);
            return d;
        } else throw new RuntimeException("Delay is not a number: " + v);
    }

    /**
	 *  Execute the action body, potentially changing the value of
	 *  actor state variables and action-scope variables.
	 *
	 * @exception net.sf.opendf.cal.interpreter.InterpreterException If the
	 * action body could not be executed successfully.
	 */
    public void actionStep() {
        if (envAction == null) {
            throw new InterpreterException("DataflowActorInterpreter: Must call actionSetup() " + "before calling actionStep().");
        }
        env.freezeLocal();
        final Action action = envAction;
        final Statement[] body = action.getBody();
        for (int i = 0; i < body.length; i++) {
            int oldStackSize = interpreter.size();
            interpreter.execute(body[i], env);
            assert interpreter.size() == oldStackSize;
        }
    }

    /**
	 * Compute the output tokens and send them to the specified (at
	 * construction time) output channels.
	 *
	 * @see ActorInterpreter
	 */
    public void actionComputeOutputs() {
        if (envAction == null) {
            throw new InterpreterException("DataflowActorInterpreter: Must call actionSetup() " + "before calling actionComputeOutputs().");
        }
        final Action action = envAction;
        final OutputExpression[] outputExpressions = action.getOutputExpressions();
        for (int i = 0; i < outputExpressions.length; i++) {
            final OutputExpression outputExpression = outputExpressions[i];
            final Expression[] expressions = outputExpression.getExpressions();
            final Expression repeatExpr = outputExpression.getRepeatExpr();
            final OutputChannel channel = ((OutputPort) (outputPortMap.get(outputExpression.getPortname()))).getChannel(0);
            if (repeatExpr != null) {
                int repeatValue = configuration.intValue(interpreter.valueOf(repeatExpr, env));
                List[] lists = new List[expressions.length];
                for (int j = 0; j < lists.length; j++) {
                    lists[j] = configuration.getList(interpreter.valueOf(expressions[j], env));
                }
                for (int j = 0; j < repeatValue; j++) {
                    for (int k = 0; k < expressions.length; k++) {
                        channel.put(lists[k].get(j));
                    }
                }
            } else {
                for (int j = 0; j < expressions.length; j++) {
                    channel.put(interpreter.valueOf(expressions[j], env));
                }
            }
        }
    }

    /**
	 * Clear action selection. The reference to the environment is
	 * cleared, too, allowing the system to reclaim any resources
	 * associated with it.
	 */
    public void actionClear() {
        envAction = null;
        env = null;
    }

    /**
	 * Return the current action. This is null if none has been selected.
	 *
	 * @return The current action, null if none.
	 */
    public Action currentAction() {
        return envAction;
    }

    /**
	 * Evaluate expression in current action environment.
	 */
    public Object evaluateExpression(Expression expr) {
        return interpreter.valueOf(expr, env);
    }

    /**
	 * Return the number of actions in this actor.
	 * @return Number of actions.
	 */
    public int nActions() {
        return actor.getActions().length;
    }

    /**
	 * Return the number of initializers in this actor.
	 * @return Number of initializers.
	 */
    public int nInitializers() {
        return actor.getInitializers().length;
    }

    /**
	 * Defines a new actor interpreter for the specified actor.
	 *
	 * @param actor The actor.
	 * @param context The interpretation context.
	 * @param actorEnv  The global environment.
	 * @param inputPortMap Map from input port names to channels.
	 * @param outputPortMap Map from output port names to channels.
	 * @see net.sf.opendf.cal.interpreter.InputChannel
	 * @see net.sf.opendf.cal.interpreter.OutputChannel
	 */
    public ActorInterpreter(final Actor actor, final Configuration configuration, final Environment actorEnv, final Map inputPortMap, final Map outputPortMap) {
        this.actor = actor;
        this.configuration = configuration;
        this.actorEnv = actorEnv;
        this.inputPortMap = inputPortMap;
        this.outputPortMap = outputPortMap;
        this.interpreter = new Executor(configuration, actorEnv);
    }

    private Actor actor;

    private Configuration configuration;

    private Environment actorEnv;

    private Environment env = null;

    private Action envAction = null;

    private Executor interpreter;

    private Map inputPortMap;

    public void setOutputPortMap(Map outputPortMap) {
        this.outputPortMap = outputPortMap;
    }

    private Map outputPortMap;

    /**
	 * A single token reader thunk encapsulates the operation of
	 * reading a single token from a specified channel.  This allows
	 * us to defer the reading operation to the time when the token is
	 * actually needed.
	 */
    private static class SingleTokenReaderThunk implements Environment.VariableContainer {

        public Object value() {
            if (val == this) {
                val = channel.get(index);
                channel = null;
            }
            return val;
        }

        public Object value(final Object[] location) {
            throw new InterpreterException("Indices not yet implemented.");
        }

        public void freeze() {
            if (val == this) {
                val = channel.get(index);
                channel = null;
            }
        }

        public SingleTokenReaderThunk(final InputChannel channel, final int index) {
            this.channel = channel;
            this.index = index;
            val = this;
        }

        private InputChannel channel;

        private int index;

        private Object val;
    }

    private static class MultipleTokenReaderThunk implements Environment.VariableContainer {

        public Object value() {
            freeze();
            return val;
        }

        public Object value(final Object[] location) {
            throw new InterpreterException("Indices not yet implemented.");
        }

        public void freeze() {
            if (val == this) {
                Object repeatVal = repeatExpr.value();
                int length = configuration.intValue(repeatVal);
                List tokens = new ArrayList();
                for (int i = 0; i < length; i++) {
                    tokens.add(channel.get(offset + i * period));
                }
                val = configuration.createList(tokens);
                channel = null;
            }
        }

        public MultipleTokenReaderThunk(InputChannel channel, int offset, int period, Thunk repeatExpr, Configuration configuration) {
            this.channel = channel;
            this.offset = offset;
            this.period = period;
            this.repeatExpr = repeatExpr;
            this.configuration = configuration;
            this.val = this;
        }

        private InputChannel channel;

        private int offset;

        private int period;

        private Thunk repeatExpr;

        private Object val;

        private Configuration configuration;
    }

    private static class EnvironmentKey {

        public EnvironmentKey(Object thingy) {
            this.thingy = thingy;
        }

        public int hashCode() {
            int n = thingy.hashCode();
            return n * n;
        }

        public boolean equals(Object obj) {
            if (obj instanceof EnvironmentKey) {
                return thingy.equals(((EnvironmentKey) obj).thingy);
            } else {
                return false;
            }
        }

        private Object thingy;
    }
}
