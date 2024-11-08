package org.jactr.core.module.procedural.six;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jactr.core.buffer.IActivationBuffer;
import org.jactr.core.buffer.event.ActivationBufferEvent;
import org.jactr.core.buffer.event.IActivationBufferListener;
import org.jactr.core.chunk.IChunk;
import org.jactr.core.event.ACTREventDispatcher;
import org.jactr.core.event.IParameterEvent;
import org.jactr.core.logging.Logger;
import org.jactr.core.model.IModel;
import org.jactr.core.model.ModelTerminatedException;
import org.jactr.core.module.AbstractModule;
import org.jactr.core.module.procedural.IConflictSetAssembler;
import org.jactr.core.module.procedural.IProceduralModule;
import org.jactr.core.module.procedural.IProductionInstantiator;
import org.jactr.core.module.procedural.IProductionSelector;
import org.jactr.core.module.procedural.event.IProceduralModuleListener;
import org.jactr.core.module.procedural.event.ProceduralModuleEvent;
import org.jactr.core.module.random.IRandomModule;
import org.jactr.core.module.random.six.DefaultRandomModule;
import org.jactr.core.production.CannotInstantiateException;
import org.jactr.core.production.IInstantiation;
import org.jactr.core.production.IProduction;
import org.jactr.core.production.ISymbolicProduction;
import org.jactr.core.production.VariableBindings;
import org.jactr.core.production.action.IAction;
import org.jactr.core.production.action.IBufferAction;
import org.jactr.core.production.action.ModifyAction;
import org.jactr.core.production.action.RemoveAction;
import org.jactr.core.production.condition.AbstractBufferCondition;
import org.jactr.core.production.condition.ChunkCondition;
import org.jactr.core.production.condition.ChunkTypeCondition;
import org.jactr.core.production.condition.IBufferCondition;
import org.jactr.core.production.condition.ICondition;
import org.jactr.core.production.condition.QueryCondition;
import org.jactr.core.production.condition.VariableCondition;
import org.jactr.core.production.six.DefaultProduction6;
import org.jactr.core.production.six.ISubsymbolicProduction6;
import org.jactr.core.utils.parameter.IParameterized;
import org.jactr.core.utils.parameter.ParameterHandler;

/**
 * default procedural module. It provides extensibility by using
 * {@link IProductionInstantiator}, {@link IProductionSelector}, and
 * {@link IConflictSetAssembler} <b>strict harvesting</b> is implemented by
 * {@link #addProductionInternal(IProduction)}. The {@link IProduction}'s
 * {@link ICondition}s are checked. If they are {@link IBufferCondition}, it
 * checks to see if {@link IActivationBuffer#isStrictHarvestingEnabled()} and if
 * so, ensures that there is an {@link IBufferAction} for that buffer as well,
 * if not, an {@link RemoveAction} is added (and therefore the production
 * explicitly removes the chunk).
 * 
 * @see http://jactr.org/node/132
 * @author harrison
 */
public class DefaultProceduralModule6 extends AbstractModule implements IProceduralModule6, IParameterized {

    /**
   * logger definition
   */
    public static final Log LOGGER = LogFactory.getLog(DefaultProceduralModule6.class);

    private ACTREventDispatcher<IProceduralModule, IProceduralModuleListener> _eventDispatcher;

    protected Map<String, IProduction> _allProductionsByName;

    protected ReentrantReadWriteLock _readWriteLock;

    protected double _productionFiringTime = 0.05;

    protected long _productionsFired = 0;

    protected boolean _breakExpectedUtilityTiesRandomly = true;

    protected double _expectedUtilityNoise;

    protected ProductionUtilityComparator _comparator;

    protected IRandomModule _randomModule;

    private IProductionSelector _selector;

    private IProductionInstantiator _instantiator;

    private IConflictSetAssembler _conflictSetAssembler;

    public DefaultProceduralModule6() {
        super("procedural");
        _allProductionsByName = new TreeMap<String, IProduction>();
        _readWriteLock = new ReentrantReadWriteLock();
        _eventDispatcher = new ACTREventDispatcher<IProceduralModule, IProceduralModuleListener>();
        setProductionSelector(new DefaultProductionSelector());
        setProductionInstantiator(new DefaultProductionInstantiator());
        setConflictSetAssembler(new DefaultConflictSetAssembler(false));
        _comparator = new ProductionUtilityComparator() {

            @Override
            public int compare(IProduction one, IProduction two) {
                int rtn = super.compare(one, two);
                if (rtn != 0) return rtn;
                if (_breakExpectedUtilityTiesRandomly && !((IInstantiation) one).getProduction().equals(((IInstantiation) two).getProduction())) return _randomModule.randomBoolean() ? 1 : -1;
                return 0;
            }
        };
    }

    public void setProductionSelector(IProductionSelector selector) {
        if (_selector != null) _selector.setProceduralModule(null);
        _selector = selector;
        _selector.setProceduralModule(this);
    }

    public IProductionSelector getProductionSelector() {
        return _selector;
    }

    public void setProductionInstantiator(IProductionInstantiator instantiator) {
        if (_instantiator != null) _instantiator.setProceduralModule(null);
        _instantiator = instantiator;
        _instantiator.setProceduralModule(this);
    }

    public IProductionInstantiator getProductionInstantiator() {
        return _instantiator;
    }

    public void setConflictSetAssembler(IConflictSetAssembler assembler) {
        if (_conflictSetAssembler != null) _conflictSetAssembler.setProceduralModule(null);
        _conflictSetAssembler = assembler;
        _conflictSetAssembler.setProceduralModule(this);
    }

    public IConflictSetAssembler getConflictSetAssembler() {
        return _conflictSetAssembler;
    }

    public void addListener(IProceduralModuleListener listener, Executor executor) {
        _eventDispatcher.addListener(listener, executor);
    }

    public void removeListener(IProceduralModuleListener listener) {
        _eventDispatcher.removeListener(listener);
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            _readWriteLock.writeLock().lock();
            for (IProduction production : _allProductionsByName.values()) production.dispose();
            _allProductionsByName.clear();
            _allProductionsByName = null;
            _eventDispatcher.clear();
            _eventDispatcher = null;
        } finally {
            _readWriteLock.writeLock().unlock();
        }
    }

    protected void fireProductionCreated(IProduction production) {
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.PRODUCTION_CREATED, production));
    }

    protected void fireProductionAdded(IProduction production) {
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.PRODUCTION_ADDED, production));
        if (Logger.hasLoggers(getModel())) Logger.log(getModel(), Logger.Stream.PROCEDURAL, "Encoded " + production);
    }

    protected void fireProductionWillFire(IInstantiation instantiation) {
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.PRODUCTION_WILL_FIRE, instantiation));
        if (Logger.hasLoggers(getModel())) Logger.log(getModel(), Logger.Stream.PROCEDURAL, "Can fire " + instantiation);
    }

    protected void fireProductionFired(IInstantiation instantiation) {
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.PRODUCTION_FIRED, instantiation));
        if (Logger.hasLoggers(getModel())) Logger.log(getModel(), Logger.Stream.PROCEDURAL, "Fired " + instantiation);
    }

    protected void fireProductionsMerged(IProduction original, IProduction duplicate) {
        ArrayList<IProduction> prods = new ArrayList<IProduction>();
        prods.add(original);
        prods.add(duplicate);
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.PRODUCTIONS_MERGED, prods));
    }

    protected void fireConflictSetAssembled(Collection<IInstantiation> instances) {
        _eventDispatcher.fire(new ProceduralModuleEvent(this, ProceduralModuleEvent.Type.CONFLICT_SET_ASSEMBLED, instances));
        if (Logger.hasLoggers(getModel())) Logger.log(getModel(), Logger.Stream.PROCEDURAL, "Conflict Set " + instances);
    }

    protected IProduction addProductionInternal(IProduction production) {
        ISymbolicProduction symProd = production.getSymbolicProduction();
        Set<String> bufferNames = new HashSet<String>();
        Set<String> ambiguousBufferNames = new HashSet<String>();
        for (ICondition condition : symProd.getConditions()) if (condition instanceof ChunkTypeCondition) {
            ChunkTypeCondition ctc = (ChunkTypeCondition) condition;
            ctc.getChunkType();
            bufferNames.add(ctc.getBufferName());
        } else if (condition instanceof ChunkCondition) {
            ChunkCondition cc = (ChunkCondition) condition;
            cc.getChunk().getSymbolicChunk().getChunkType();
            bufferNames.add(cc.getBufferName());
        } else if (condition instanceof AbstractBufferCondition) {
            String bufferName = ((AbstractBufferCondition) condition).getBufferName();
            if (condition instanceof VariableCondition) bufferNames.add(bufferName);
            ambiguousBufferNames.add(bufferName);
        }
        Collection<IAction> actions = new ArrayList<IAction>(symProd.getActions());
        IModel model = getModel();
        for (String bufferName : bufferNames) {
            IActivationBuffer buffer = model.getActivationBuffer(bufferName);
            boolean actionFound = false;
            if (buffer.isStrictHarvestingEnabled()) {
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Strict harvesting enabled for " + bufferName + ", checking actions of " + symProd.getName());
                for (IAction action : actions) if (action instanceof IBufferAction && ((IBufferAction) action).getBufferName().equals(bufferName)) {
                    if (action instanceof ModifyAction && !(action instanceof RemoveAction) && ((ModifyAction) action).getSlots().size() == 0) symProd.removeAction(action);
                    actionFound = true;
                    break;
                }
                if (!actionFound) {
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(bufferName + " requires strict harvest but " + symProd.getName() + " doesn't operate on the buffer after the match. Adding a remove");
                    symProd.addAction(new RemoveAction(bufferName));
                }
            }
        }
        _readWriteLock.writeLock().lock();
        String productionName = getSafeName(symProd.getName(), _allProductionsByName);
        symProd.setName(productionName);
        production.encode();
        _allProductionsByName.put(productionName.toLowerCase(), production);
        _readWriteLock.writeLock().unlock();
        fireProductionAdded(production);
        return production;
    }

    public Future<IProduction> addProduction(final IProduction production) {
        Callable<IProduction> callable = new Callable<IProduction>() {

            public IProduction call() throws Exception {
                return addProductionInternal(production);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected IProduction removeProductionInternal(IProduction production) {
        ISymbolicProduction symProd = production.getSymbolicProduction();
        Set<String> bufferNames = new HashSet<String>();
        for (ICondition condition : symProd.getConditions()) if (condition instanceof ChunkTypeCondition) {
            ChunkTypeCondition ctc = (ChunkTypeCondition) condition;
            ctc.getChunkType();
            bufferNames.add(ctc.getBufferName());
        } else if (condition instanceof ChunkCondition) {
            ChunkCondition cc = (ChunkCondition) condition;
            cc.getChunk().getSymbolicChunk().getChunkType();
            bufferNames.add(cc.getBufferName());
        } else if (condition instanceof AbstractBufferCondition) {
            String bufferName = ((AbstractBufferCondition) condition).getBufferName();
            if (condition instanceof VariableCondition) bufferNames.add(bufferName);
        }
        _readWriteLock.writeLock().lock();
        String productionName = symProd.getName();
        _allProductionsByName.remove(productionName.toLowerCase());
        _readWriteLock.writeLock().unlock();
        return production;
    }

    public Future<IProduction> removeProduction(final IProduction production) {
        Callable<IProduction> callable = new Callable<IProduction>() {

            public IProduction call() throws Exception {
                return removeProductionInternal(production);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected IProduction createProductionInternal(String name) {
        IModel model = getModel();
        IProduction production = new DefaultProduction6(model);
        production.getSymbolicProduction().setName(getSafeName(name, _allProductionsByName));
        fireProductionCreated(production);
        return production;
    }

    public Future<IProduction> createProduction(final String name) {
        Callable<IProduction> callable = new Callable<IProduction>() {

            public IProduction call() throws Exception {
                return createProductionInternal(name);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected Collection<IInstantiation> getConflictSetInternal(Collection<IActivationBuffer> buffers) {
        IModel model = getModel();
        Set<IProduction> productions = new TreeSet<IProduction>(new ProductionNameComparator());
        _conflictSetAssembler.getConflictSet(productions);
        if (LOGGER.isDebugEnabled() || Logger.hasLoggers(model)) {
            StringBuilder sb = new StringBuilder("Considering ");
            sb.append(productions.size()).append(" productions for conflict set");
            String message = sb.toString();
            if (LOGGER.isDebugEnabled()) LOGGER.debug(message);
            Logger.log(model, Logger.Stream.PROCEDURAL, message);
        }
        Collection<IInstantiation> keepers = createAndSortInstantiations(productions);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Final conflict set " + keepers);
        fireConflictSetAssembled(keepers);
        return keepers;
    }

    /**
   * iterates over productions, attempting to instantiate each. Those that can
   * be instantiated will be sorted by utility and returned.
   * 
   * @param productions
   * @return
   */
    protected Collection<IInstantiation> createAndSortInstantiations(Collection<IProduction> productions) {
        IModel model = getModel();
        List<IInstantiation> keepers = new ArrayList<IInstantiation>();
        StringBuilder message = new StringBuilder();
        for (IProduction production : productions) try {
            Collection<VariableBindings> provisionalBindings = computeProvisionalBindings(production);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Instantiating " + production);
            Collection<IInstantiation> instantiations = _instantiator.instantiate(production, provisionalBindings);
            for (IInstantiation instantiation : instantiations) {
                double noise = _randomModule.logisticNoise(getExpectedUtilityNoise());
                ISubsymbolicProduction6 p = (ISubsymbolicProduction6) instantiation.getSubsymbolicProduction();
                double utility = p.getExpectedUtility();
                if (Double.isNaN(utility)) utility = p.getUtility();
                if (LOGGER.isDebugEnabled()) LOGGER.debug(production + " utility: " + utility + " noise:" + noise + " expected utility: " + (utility + noise));
                p.setExpectedUtility(utility + noise);
                if (LOGGER.isDebugEnabled() || Logger.hasLoggers(model)) {
                    message.delete(0, message.length());
                    message.append("Instantiated ").append(production).append(" expected utility ");
                    message.append(utility + noise).append(" (").append(noise).append(" noise)");
                    String msg = message.toString();
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(msg);
                    if (Logger.hasLoggers(model)) Logger.log(model, Logger.Stream.PROCEDURAL, msg);
                }
                keepers.add(instantiation);
            }
        } catch (CannotInstantiateException cie) {
            if (LOGGER.isDebugEnabled() || Logger.hasLoggers(model)) {
                String msg = cie.getMessage();
                LOGGER.debug(msg);
                Logger.log(model, Logger.Stream.PROCEDURAL, msg);
            }
        }
        Collections.sort(keepers, _comparator);
        return keepers;
    }

    public Future<Collection<IInstantiation>> getConflictSet(final Collection<IActivationBuffer> buffers) {
        Callable<Collection<IInstantiation>> callable = new Callable<Collection<IInstantiation>>() {

            public Collection<IInstantiation> call() throws Exception {
                return getConflictSetInternal(buffers);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected IProduction getProductionInternal(String name) {
        _readWriteLock.readLock().lock();
        IProduction rtn = _allProductionsByName.get(name.toLowerCase());
        _readWriteLock.readLock().unlock();
        return rtn;
    }

    public Future<IProduction> getProduction(final String name) {
        Callable<IProduction> callable = new Callable<IProduction>() {

            public IProduction call() {
                return getProductionInternal(name);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected IInstantiation selectInstantiationInternal(Collection<IInstantiation> instantiations) {
        return _selector.select(instantiations);
    }

    public Future<IInstantiation> selectInstantiation(final Collection<IInstantiation> instantiations) {
        Callable<IInstantiation> callable = new Callable<IInstantiation>() {

            public IInstantiation call() throws Exception {
                return selectInstantiationInternal(instantiations);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    protected Double fireProductionInternal(IInstantiation instantiation, double firingTime) {
        try {
            IModel model = getModel();
            fireProductionWillFire(instantiation);
            if (LOGGER.isDebugEnabled() || Logger.hasLoggers(model)) {
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Firing " + instantiation);
                Logger.log(model, Logger.Stream.PROCEDURAL, "Firing " + instantiation);
            }
            instantiation.fire(firingTime);
            fireProductionFired(instantiation);
            setNumberOfProductionsFired(_productionsFired + 1);
        } catch (ModelTerminatedException mte) {
            setNumberOfProductionsFired(_productionsFired + 1);
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Model has terminated naturally");
            return Double.NaN;
        }
        return instantiation.getActionLatency();
    }

    public Future<Double> fireProduction(final IInstantiation instantiation, final double firingTime) {
        Callable<Double> callable = new Callable<Double>() {

            public Double call() {
                return fireProductionInternal(instantiation, firingTime);
            }
        };
        return delayedFuture(callable, getExecutor());
    }

    public double getDefaultProductionFiringTime() {
        return _productionFiringTime;
    }

    public void setDefaultProductionFiringTime(double firingTime) {
        double old = _productionFiringTime;
        _productionFiringTime = firingTime;
        if (_eventDispatcher.hasListeners()) _eventDispatcher.fire(new ProceduralModuleEvent(this, DEFAULT_PRODUCTION_FIRING_TIME, old, firingTime));
    }

    @Override
    public void initialize() {
        _randomModule = (IRandomModule) getModel().getModule(IRandomModule.class);
        if (_randomModule == null) _randomModule = DefaultRandomModule.getInstance();
        for (IActivationBuffer buffer : getModel().getActivationBuffers()) buffer.addListener(new IActivationBufferListener() {

            public void chunkMatched(ActivationBufferEvent abe) {
            }

            public void requestAccepted(ActivationBufferEvent abe) {
            }

            public void sourceChunkAdded(ActivationBufferEvent abe) {
            }

            public void sourceChunkRemoved(ActivationBufferEvent abe) {
            }

            public void sourceChunksCleared(ActivationBufferEvent abe) {
            }

            public void statusSlotChanged(ActivationBufferEvent abe) {
            }

            public void parameterChanged(IParameterEvent pe) {
            }
        }, null);
    }

    public double getExpectedUtilityNoise() {
        return _expectedUtilityNoise;
    }

    public long getNumberOfProductionsFired() {
        return _productionsFired;
    }

    public void setExpectedUtilityNoise(double noise) {
        double old = _expectedUtilityNoise;
        _expectedUtilityNoise = noise;
        if (_eventDispatcher.hasListeners()) _eventDispatcher.fire(new ProceduralModuleEvent(this, EXPECTED_UTILITY_NOISE, old, noise));
    }

    public void setNumberOfProductionsFired(long fired) {
        long old = _productionsFired;
        _productionsFired = fired;
        if (_eventDispatcher.hasListeners()) _eventDispatcher.fire(new ProceduralModuleEvent(this, NUMBER_OF_PRODUCTIONS_FIRED, old, fired));
    }

    protected Collection<IProduction> getProductionsInternal() {
        try {
            _readWriteLock.readLock().lock();
            return new ArrayList<IProduction>(_allProductionsByName.values());
        } finally {
            _readWriteLock.readLock().unlock();
        }
    }

    public Future<Collection<IProduction>> getProductions() {
        return delayedFuture(new Callable<Collection<IProduction>>() {

            public Collection<IProduction> call() throws Exception {
                return getProductionsInternal();
            }
        }, getExecutor());
    }

    /**
   * @see org.jactr.core.utils.parameter.IParameterized#getParameter(java.lang.String)
   */
    public String getParameter(String key) {
        if (NUMBER_OF_PRODUCTIONS_FIRED.equalsIgnoreCase(key)) return "" + getNumberOfProductionsFired();
        if (EXPECTED_UTILITY_NOISE.equalsIgnoreCase(key)) return "" + getExpectedUtilityNoise();
        if (DEFAULT_PRODUCTION_FIRING_TIME.equalsIgnoreCase(key)) return "" + getDefaultProductionFiringTime();
        return null;
    }

    /**
   * @see org.jactr.core.utils.parameter.IParameterized#getPossibleParameters()
   */
    public Collection<String> getPossibleParameters() {
        return getSetableParameters();
    }

    /**
   * @see org.jactr.core.utils.parameter.IParameterized#getSetableParameters()
   */
    public Collection<String> getSetableParameters() {
        ArrayList<String> rtn = new ArrayList<String>();
        rtn.add(EXPECTED_UTILITY_NOISE);
        rtn.add(DEFAULT_PRODUCTION_FIRING_TIME);
        rtn.add(NUMBER_OF_PRODUCTIONS_FIRED);
        return rtn;
    }

    /**
   * @see org.jactr.core.utils.parameter.IParameterized#setParameter(java.lang.String,
   *      java.lang.String)
   */
    public void setParameter(String key, String value) {
        if (NUMBER_OF_PRODUCTIONS_FIRED.equalsIgnoreCase(key)) setNumberOfProductionsFired(ParameterHandler.numberInstance().coerce(value).longValue()); else if (EXPECTED_UTILITY_NOISE.equalsIgnoreCase(key)) setExpectedUtilityNoise(ParameterHandler.numberInstance().coerce(value).doubleValue()); else if (DEFAULT_PRODUCTION_FIRING_TIME.equalsIgnoreCase(key)) setDefaultProductionFiringTime(ParameterHandler.numberInstance().coerce(value).doubleValue()); else if (LOGGER.isWarnEnabled()) LOGGER.warn(String.format("%s doesn't recognize %s. Available parameters : %s", getClass().getSimpleName(), key, getSetableParameters()));
    }

    /**
   * in order to handle the iterative nature of the instantiation process in
   * addition to the possibility for multiple sources chunks, provisional
   * bindings must be created for all the chunk permutations. Ugh.
   */
    private Collection<VariableBindings> computeProvisionalBindings(IProduction production) {
        IModel model = getModel();
        Collection<VariableBindings> returnedProvisionalBindings = new ArrayList<VariableBindings>();
        Collection<VariableBindings> provisionalBindings = new ArrayList<VariableBindings>();
        VariableBindings initialBinding = new VariableBindings();
        initialBinding.bind("=model", model);
        provisionalBindings.add(initialBinding);
        for (ICondition condition : production.getSymbolicProduction().getConditions()) if (condition instanceof IBufferCondition && !(condition instanceof QueryCondition)) {
            IActivationBuffer buffer = model.getActivationBuffer(((IBufferCondition) condition).getBufferName());
            Collection<IChunk> sourceChunks = buffer.getSourceChunks();
            if (sourceChunks.size() == 0) continue;
            Map<IChunk, Collection<VariableBindings>> keyedProvisionalBindings = new HashMap<IChunk, Collection<VariableBindings>>();
            for (IChunk source : sourceChunks) {
                Collection<VariableBindings> bindings = provisionalBindings;
                if (keyedProvisionalBindings.size() != 0) bindings = copyBindings(provisionalBindings);
                for (VariableBindings binding : bindings) binding.bind("=" + buffer.getName(), source, buffer);
                keyedProvisionalBindings.put(source, bindings);
            }
            for (Collection<VariableBindings> bindings : keyedProvisionalBindings.values()) if (bindings != provisionalBindings) provisionalBindings.addAll(bindings);
        }
        returnedProvisionalBindings.addAll(provisionalBindings);
        return returnedProvisionalBindings;
    }

    private Collection<VariableBindings> copyBindings(Collection<VariableBindings> src) {
        Collection<VariableBindings> rtn = new ArrayList<VariableBindings>(src.size());
        for (VariableBindings map : src) rtn.add(map.clone());
        return rtn;
    }

    public void reset() {
    }
}
