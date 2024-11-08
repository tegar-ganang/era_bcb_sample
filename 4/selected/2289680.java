package com.ibm.realtime.flexotask.system;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.ibm.realtime.flexotask.AtomicFlexotask;
import com.ibm.realtime.flexotask.Flexotask;
import com.ibm.realtime.flexotask.FlexotaskGraph;
import com.ibm.realtime.flexotask.FlexotaskInputPort;
import com.ibm.realtime.flexotask.FlexotaskOutputPort;
import com.ibm.realtime.flexotask.FlexotaskRunner;
import com.ibm.realtime.flexotask.FlexotaskTimer;
import com.ibm.realtime.flexotask.TimeAware;
import com.ibm.realtime.flexotask.distribution.FlexotaskChannel;
import com.ibm.realtime.flexotask.distribution.FlexotaskDistributer;
import com.ibm.realtime.flexotask.distribution.FlexotaskDistributerRegistry;
import com.ibm.realtime.flexotask.distribution.FlexotaskDistributionContext;
import com.ibm.realtime.flexotask.distribution.FlexotaskTimerService;
import com.ibm.realtime.flexotask.scheduling.FlexotaskController;
import com.ibm.realtime.flexotask.scheduling.FlexotaskScheduler;
import com.ibm.realtime.flexotask.scheduling.FlexotaskSchedulerRegistry;
import com.ibm.realtime.flexotask.scheduling.FlexotaskSchedulingData;
import com.ibm.realtime.flexotask.scheduling.FlexotaskThreadFactory;
import com.ibm.realtime.flexotask.template.FlexotaskCommunicatorTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskConnectionMode;
import com.ibm.realtime.flexotask.template.FlexotaskConnectionTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskPredicateTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskStableMode;
import com.ibm.realtime.flexotask.template.FlexotaskTaskTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskTemplate;
import com.ibm.realtime.flexotask.template.FlexotaskValidationException;
import com.ibm.realtime.flexotask.tracing.FlexotaskTaskTracer;
import com.ibm.realtime.flexotask.tracing.FlexotaskTracer;
import com.ibm.realtime.flexotask.tracing.FlexotaskTracerFactory;
import com.ibm.realtime.flexotask.util.NanoTime;
import com.ibm.realtime.flexotask.validation.AtomicFlexotaskGuard;
import com.ibm.realtime.flexotask.vm.FlexotaskRoot;
import com.ibm.realtime.flexotask.vm.FlexotaskVMBridge;

/**
 * Not part of the external interface.  Invoked via
 * <b>FlexotaskTemplate.validate()</b>.
 */
public class FlexotaskValidator {

    /** The currently active tracer factory, to be used in all subsequent instantiations until changed
   * with setTracerFactory
   */
    private static FlexotaskTracerFactory tracerFactory = FlexotaskTracerFactory.NULL_FACTORY;

    /** The classloader to be used to resolve classes needed by the validator */
    private static ClassLoader cl = ClassLoader.getSystemClassLoader();

    static {
        FlexotaskRoot.class.getName();
        FlexotaskInputPort[].class.getName();
        FlexotaskInputPortImpl.class.getName();
        FlexotaskOutputPort[].class.getName();
        FlexotaskOutputPortImpl.class.getName();
        Flexotask.class.getName();
        FlexotaskValidationException.class.getName();
        AtomicFlexotask.class.getName();
        TransactionalOperations.class.getName();
        TimeAware.class.getName();
        PortBuffer.class.getName();
        FlexotaskTracer.class.getName();
        FlexotaskTaskTracer.class.getName();
        FlexotaskTracer.NullTracer.class.getName();
        FlexotaskPredicateTemplate.class.getName();
        FlexotaskController.class.getName();
        FlexotaskControllerImpl.class.getName();
        NanoTime.class.getName();
    }

    /**
   * Set the tracer factory to be used in subsequently instantiated graphs.  The factory and the
   *   objects it creates must obey the rules for tracers so as not to perturb execution
   * @param tracerFactory the FlexotaskTracerFactory to be used.  As a bow to security concerns,
   *   the tracer factory must come from the boot classpath in a real Flexotask VM.
   */
    public static void setTracer(FlexotaskTracerFactory tracerFactory) {
        FlexotaskSystemSupport.checkSecurity(tracerFactory, "FlexotaskTracerFactory");
        FlexotaskValidator.tracerFactory = tracerFactory;
    }

    /**
   * Not part of the external interface.  Invoked via
   * <b>FlexotaskTemplate.validate()</b>.
   * <p>Performs validation, which consists of these steps.
   * <ul><li>the requested scheduler is retrieved from the registry.  If it is not there,
   *   validation fails.
   * <li>the specification is checked for internal consistency and rejected if inconsistent
   * <li>the specification is checked for VM safety were it to be run under Flexotask rules, and
   *   adherence to the Flexotask rules by all Flexotasks in the specification is also checked.
   * <li>the specification is passed to the scheduler, which determines if it is schedulable and
   *   optionally updates the FlexotaskSchedulingData provided to it
   * <li>the Runnables implied by the specification are instantiated and a map from
   *   FlexotaskTemplateElement to Runnable is prepared for the scheduler
   * <li>the scheduler is asked to prepare an FlexotaskRunner for the incipient FlexotaskGraph
   * <li>preparation of the Vm and GC for the FlexotaskGraph is completed and an actual
   *   FlexotaskGraph is completed and returned.
   * </ul>
   * @param specification the FlexotaskTemplate to validate
   * @param schedulerKind the kind of scheduler that will run the FlexotaskGraph.  The scheduler
   *   is consulted as part of the validation and instantiation process
   * @param schedulingData information that the scheduler may need to schedule the graph but
   *   that is not provided as timing information within the graph (for example, WCETs).  The
   *   scheduler may update this argument.
   * @param initializationMap a map from task names to initialization parameters
   * @param distributionContext information that may result in the creation of an FlexotaskDistributer
   *   for the graph by the FlexotaskDistributerRegistry.  If null, the FlexotaskDistributer base
   *   class is used which implies a non-distributed graph.
   * @return a FlexotaskGraph if valid
   * @throws FlexotaskValidationException if not valid
   */
    public static FlexotaskGraph validate(FlexotaskTemplate specification, String schedulerKind, FlexotaskSchedulingData schedulingData, Map initializationMap, FlexotaskDistributionContext distributionContext) throws FlexotaskValidationException {
        FlexotaskScheduler scheduler = FlexotaskSchedulerRegistry.getScheduler(schedulerKind);
        if (scheduler == null) {
            throw new FlexotaskValidationException("No " + schedulerKind + " scheduler available");
        }
        FlexotaskDistributer distributer;
        if (distributionContext == null) {
            distributer = new FlexotaskDistributer();
        } else {
            distributer = FlexotaskDistributerRegistry.getDistributer(distributionContext);
            if (distributer == null) {
                throw new FlexotaskValidationException("No distributer factory found for distributer type " + distributionContext.getSystemType());
            }
        }
        tracerFactory.startValidation();
        Object[] toPin;
        Map heapSizes;
        FlexotaskTimerService timer;
        try {
            checkConsistency(specification);
            if (initializationMap == null) {
                initializationMap = new HashMap();
            } else {
                initializationMap = new HashMap(initializationMap);
            }
            for (Iterator iter = specification.getTasks().iterator(); iter.hasNext(); ) {
                FlexotaskTaskTemplate element = (FlexotaskTaskTemplate) iter.next();
                if (element instanceof FlexotaskCommunicatorTemplate) {
                    FlexotaskChannel channel = distributer.getChannel((FlexotaskCommunicatorTemplate) element);
                    initializationMap.put(element.getName(), channel);
                }
            }
            timer = distributer.getTimerService();
            toPin = new CodeValidator(specification).validate(initializationMap, timer);
            if (!scheduler.isSchedulable(specification, schedulingData)) {
                throw new FlexotaskValidationException("Specification rejected by the scheduler: " + scheduler.getRejectionReason());
            }
            heapSizes = scheduler.getHeapSizes(specification, schedulingData);
        } finally {
            tracerFactory.endValidation();
        }
        verbosePinObjects(toPin, "heap objects found during validation");
        Class[] stable = initStableTransient(specification);
        tracerFactory.startInstantiation();
        Map instantiationMap;
        try {
            instantiationMap = instantiate(specification, heapSizes, initializationMap, timer);
        } finally {
            tracerFactory.endInstantiation();
        }
        Map guards = new HashMap();
        for (Iterator iterator = instantiationMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object controller = entry.getValue();
            if (controller instanceof AtomicFlexotaskController) {
                FlexotaskTaskTemplate task = (FlexotaskTaskTemplate) entry.getKey();
                try {
                    Class guardClass = CodeValidator.resolveClass((task.getImplementationClass() + "$Guard"), cl);
                    AtomicFlexotaskGuard guard = (AtomicFlexotaskGuard) guardClass.newInstance();
                    AtomicFlexotaskBase atomicTask = (AtomicFlexotaskBase) ((FlexotaskControllerImpl) controller).root.getTask();
                    guard.setDelegate((AtomicFlexotask) atomicTask);
                    atomicTask.lock = guard;
                    guards.put(task.getName(), guard);
                } catch (Exception e) {
                    throw new FlexotaskValidationException("Guard could not be constructed for atomic Flexotask " + task.getName());
                }
            }
        }
        if (guards.size() > 0) {
            Object[] newPinned = guards.values().toArray();
            verbosePinObjects(newPinned, "guard objects");
            Object[] allPinned = new Object[toPin.length + newPinned.length];
            System.arraycopy(toPin, 0, allPinned, 0, toPin.length);
            System.arraycopy(newPinned, 0, allPinned, toPin.length, newPinned.length);
            toPin = allPinned;
        }
        FlexotaskGraphImpl ans = new FlexotaskGraphImpl(distributer, guards, toPin, stable);
        tracerFactory.startScheduling();
        FlexotaskThreadFactory factory = new FlexotaskThreadFactoryImpl(timer, tracerFactory, ans);
        FlexotaskRunner runner;
        try {
            runner = scheduler.schedule(specification, instantiationMap, schedulingData, factory, initializationMap);
        } finally {
            tracerFactory.endScheduling();
        }
        ans.setRunner(runner);
        return ans;
    }

    static void verbosePinObjects(Object[] toPin, String what) {
        if (toPin.length > 0) {
            if (!FlexotaskSystemSupport.isFlexotaskVM()) {
                System.err.println("Will pin " + toPin.length + " " + what + ".  Classes:");
                for (int i = 0; i < toPin.length; i++) {
                    System.err.println(toPin[i].getClass().getName());
                }
            }
        }
        if (!FlexotaskObjectPinner.pinObjects(toPin)) {
            throw new IllegalStateException("System error pinning " + what);
        }
    }

    /**
   * Check a FlexotaskTemplate for internal consistency.  A consistent specification
   *   is one that would be valid if all the code of all the Flexotasks proves to be valid.
   *   <p>The following consistency rules are enforced (bear in mind that graph inputs and
   *      outputs and communicators are also tasks at this level).
   *   <ol><li>The types must match at the two ends of every connection.
   *   <li>Every communicator must have at least one connection via at least one of its ports.
   *   <li>Every task that isn't a communicator must have at least one connection to each of its
   *     ports.
   *   <li>Every task must have at least one port (input or output)
   *   <li>Anything that isn't checked by the specification graph object constructors (and generally
   *      these don't do a lot of checking) is checked here.  For example, no null values are
   *      allowed except where explicitly allowed.
   *   <li>No task may be referred to in a connection if it isn't in the specification.
   *   </ol>
   * @param specification the FlexotaskTemplate to be checked
   * @throws FlexotaskValidationException if the check finds anything wrong
   */
    private static void checkConsistency(FlexotaskTemplate specification) throws FlexotaskValidationException {
        Map usedTasks = new HashMap();
        for (Iterator iter = specification.getTasks().iterator(); iter.hasNext(); ) {
            FlexotaskTaskTemplate task = (FlexotaskTaskTemplate) iter.next();
            boolean[][] checker = new boolean[2][];
            String[] inputPorts = task.getInputPortTypes();
            String[] outputPorts = task.getOutputPortTypes();
            checker[0] = new boolean[inputPorts.length];
            checker[1] = new boolean[outputPorts.length];
            usedTasks.put(task, checker);
        }
        Set tasks = specification.getTasks();
        for (Iterator iter = specification.getConnections().iterator(); iter.hasNext(); ) {
            FlexotaskConnectionTemplate conn = (FlexotaskConnectionTemplate) iter.next();
            FlexotaskTaskTemplate source = conn.getInput();
            if (!tasks.contains(source)) {
                throw new FlexotaskValidationException("Task " + source.getName() + " is referred to in connection " + conn.getName() + " but is not in the specification");
            }
            int sourcePort = conn.getOutputPortToRead();
            String[] sourcePortTypes = source.getOutputPortTypes();
            if (sourcePort < 0 || sourcePort >= sourcePortTypes.length) {
                throw new FlexotaskValidationException("Invalid task output port index " + sourcePort + " in connection " + conn.getName());
            }
            String sourceType = sourcePortTypes[sourcePort];
            FlexotaskTaskTemplate sink = conn.getOutput();
            if (!tasks.contains(sink)) {
                throw new FlexotaskValidationException("Task " + sink.getName() + " is referred to in connection " + conn.getName() + " but is not in the specification");
            }
            int sinkPort = conn.getInputPortToWrite();
            String[] sinkPortTypes = sink.getInputPortTypes();
            if (sinkPort < 0 || sinkPort >= sinkPortTypes.length) {
                throw new FlexotaskValidationException("Invalid task input port index " + sinkPort + " in connection " + conn.getName());
            }
            String sinkType = sinkPortTypes[sinkPort];
            if (!sourceType.equals(sinkType)) {
                throw new FlexotaskValidationException("Connection " + conn.getName() + " specifies type " + sourceType + " for the source, but type " + sinkType + " for the sink");
            }
            boolean[][] portsUsed = (boolean[][]) usedTasks.get(source);
            portsUsed[1][sourcePort] = true;
            portsUsed = (boolean[][]) usedTasks.get(sink);
            portsUsed[0][sinkPort] = true;
        }
        for (Iterator iter = usedTasks.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            boolean[][] checker = (boolean[][]) entry.getValue();
            FlexotaskTaskTemplate task = (FlexotaskTaskTemplate) entry.getKey();
            if (task instanceof FlexotaskCommunicatorTemplate) {
                boolean ok = false;
                for (int i = 0; i < checker.length; i++) {
                    boolean[] subchecker = checker[i];
                    for (int j = 0; j < subchecker.length; j++) {
                        ok |= subchecker[j];
                    }
                }
                if (!ok) {
                    throw new FlexotaskValidationException("Communicator " + task.getName() + " has no connections");
                }
            } else {
                for (int i = 0; i < checker.length; i++) {
                    boolean[] subchecker = checker[i];
                    for (int j = 0; j < subchecker.length; j++) {
                        if (!subchecker[j]) {
                            throw new FlexotaskValidationException("Specification has no connection for " + ((i == 0) ? "input" : "output") + " port " + j + " of task " + task.getName());
                        }
                    }
                }
            }
        }
    }

    /**
   * Get the default parameter value from an FlexotaskTaskTemplate converted into its appropriate
   *    type using a standard algorithm
   * <p>The algorithm is as follows:
   * <ol><li>If the type is java.lang.String the parameterValue is returned.
   * <li>Otherwise, if the value is null or "", null is returned.
   * <li>Otherwise, if type cannot be resolved to a Class, an exception is thrown.
   * <li>Otherwise, if the value is (), a 0-argument constructor of type is invoked, and an
   *   exception is thrown if none is available.
   * <li>Otherwise, if the value is any string surrounded by parantheses, the parentheses are removed
   *   and a constructor taking a single String argument is invoked, with an exception being thrown
   *   if none is available
   * <li>Otherwise, a static method valueOf(String) as defined in type is invoked, with an exception
   *   being thrown if none is available</ol>
   * @param spec the FlexotaskTaskTemplate whose converted default parameter value is desired
   * @return the converted default parameter value
   * @throws FlexotaskValidationException if the value could not be converted
   */
    private static Object getConvertedParameterValue(FlexotaskTaskTemplate spec) throws FlexotaskValidationException {
        String type = spec.getParameterType();
        String initValue = spec.getParameterValue();
        if (String.class.getName().equals(type)) {
            return initValue;
        }
        if (initValue == null || initValue.length() == 0) {
            return null;
        }
        try {
            Class cType = CodeValidator.resolveClass(type, cl);
            if (initValue.startsWith("(") && initValue.endsWith(")")) {
                String arg = initValue.substring(1, initValue.length() - 1);
                if (arg.length() == 0) {
                    return cType.newInstance();
                }
                Constructor cons = cType.getConstructor(new Class[] { String.class });
                return cons.newInstance(new Object[] { arg });
            }
            Method meth = cType.getMethod("valueOf", new Class[] { String.class });
            return meth.invoke(null, new Object[] { initValue });
        } catch (Exception e) {
            throw new FlexotaskValidationException("Initial value could not be constructed for " + type, e);
        }
    }

    /**
   * Perform instantiation of a valid and presumeably schedulable specification
   * @param specification the FlexotaskTemplate to instantiate
   * @param heapSizes the map from tasks to heap sizes provided by the scheduler
   * @param initializationMap the Map from task names to Objects that will be used as parameter arguments
   *   to the tasks' initialize methods
   * @return an instantiation map to give to the scheduler
   * @exception FlexotaskValidationException if instantiation cannot be achieved due to some error
   */
    private static Map instantiate(FlexotaskTemplate specification, Map heapSizes, Map initializationMap, FlexotaskTimer timer) throws FlexotaskValidationException {
        Map taskMap = new HashMap();
        for (Iterator iter = specification.getTasks().iterator(); iter.hasNext(); ) {
            FlexotaskTaskTemplate task = (FlexotaskTaskTemplate) iter.next();
            Object parameter = initializationMap.get(task.getName());
            taskMap.put(task, instantiateTask(task, (long[]) heapSizes.get(task), parameter, timer));
        }
        Map instantiationMap = new HashMap();
        for (Iterator iter = specification.getConnections().iterator(); iter.hasNext(); ) {
            FlexotaskConnectionTemplate conn = (FlexotaskConnectionTemplate) iter.next();
            FlexotaskTracer tracer = tracerFactory.newConnectionTracer(conn.getName());
            FlexotaskTaskTemplate source = conn.getInput();
            FlexotaskControllerImpl sourceController = (FlexotaskControllerImpl) taskMap.get(source);
            FlexotaskOutputPortImpl sourcePort = (FlexotaskOutputPortImpl) sourceController.root.getOutputPorts()[conn.getOutputPortToRead()];
            FlexotaskTaskTemplate target = conn.getOutput();
            FlexotaskControllerImpl targetController = (FlexotaskControllerImpl) taskMap.get(target);
            FlexotaskInputPortImpl targetPort = (FlexotaskInputPortImpl) targetController.root.getInputPorts()[conn.getInputPortToWrite()];
            FlexotaskConnectionMode mode = conn.getConnectionMode();
            ConnectionDriverImpl driver;
            if (targetController instanceof AtomicFlexotaskController) {
                driver = new AtomicConnectionDriver(sourcePort, sourceController, targetPort, targetController, mode, tracer);
            } else {
                driver = new ConnectionDriverImpl(sourcePort, sourceController, targetPort, targetController, mode, tracer);
            }
            driver.setInputController(sourceController);
            driver.setOutputController(targetController);
            instantiationMap.put(conn, driver);
        }
        for (Iterator iter = taskMap.keySet().iterator(); iter.hasNext(); ) {
            FlexotaskTaskTemplate task = (FlexotaskTaskTemplate) iter.next();
            instantiationMap.put(task, (FlexotaskController) taskMap.get(task));
        }
        return instantiationMap;
    }

    /**
   * Initialize the stable/transient information in the runtime
   * @param template the template to use for this information
   * @return the array of stable classes for recovery at termination or null if the default
   *   policy is used
   * @throws FlexotaskValidationException
   * @throws ClassNotFoundException
   */
    private static Class[] initStableTransient(FlexotaskTemplate template) throws FlexotaskValidationException {
        if (template.getStableMode() == FlexotaskStableMode.DEFAULT) {
            FlexotaskSystemSupport.setTransientPolicy(false);
            return null;
        }
        Set stableClasses = template.getStableClasses();
        Class[] ans = new Class[stableClasses.size()];
        try {
            int index = 0;
            for (Iterator iterator = stableClasses.iterator(); iterator.hasNext(); ) {
                Class aClass = CodeValidator.resolveClass((String) iterator.next(), cl);
                FlexotaskSystemSupport.setStable(aClass, true);
                ans[index++] = aClass;
            }
        } catch (ClassNotFoundException e) {
            for (int i = 0; i < ans.length; i++) {
                if (ans[i] != null) {
                    FlexotaskSystemSupport.setStable(ans[i], false);
                }
            }
            throw new FlexotaskValidationException("Could not find declared stable class: " + e.getMessage());
        }
        FlexotaskSystemSupport.setTransientPolicy(true);
        return ans;
    }

    /**
   * Instantiate a task
   * @param task the FlexotaskTaskTemplate for the task
   * @param heapSizes the initial and maximum heap sizes for the task as a long[2] or null to indicate
   *   use of the defaults
   * @param parameter the parameter to pass the Flexotask's initialize method
    * are by-reference.
   * @param timer the FlexotaskTimer for this graph, to be used if the flexotask is time-aware
   * @return an FlexotaskController for the task and its ports.  This is used both to complete
   *   instantation (the ConnectionDrivers use the ports) and to serve as a runtime controller
   *   for the Flexotask and its private heap
   * @throws FlexotaskValidationException if anything goes wrong
   */
    private static FlexotaskController instantiateTask(FlexotaskTaskTemplate task, long[] heapSizes, Object parameter, FlexotaskTimer timer) throws FlexotaskValidationException {
        Object portInitValue = null;
        if (task instanceof FlexotaskCommunicatorTemplate) {
            portInitValue = getConvertedParameterValue(task);
        } else if (parameter == null) {
            parameter = getConvertedParameterValue(task);
        }
        String[] outputPortClassnames = task.getOutputPortTypes();
        Class[] outputPortClasses = new Class[outputPortClassnames.length];
        Class implClass;
        try {
            implClass = CodeValidator.resolveClass(task.getImplementationClass(), cl);
            for (int i = 0; i < outputPortClasses.length; i++) {
                outputPortClasses[i] = CodeValidator.resolveClass(outputPortClassnames[i], cl);
            }
        } catch (ClassNotFoundException e) {
            throw new FlexotaskValidationException(e.getMessage(), e);
        }
        int numInputPorts = task.getInputPortTypes().length;
        try {
            long oldSpace = 0L;
            if (heapSizes == null) {
                oldSpace = FlexotaskSystemSupport.createMemorySpace(false);
            } else {
                oldSpace = FlexotaskSystemSupport.createMemorySpace(FlexotaskVMBridge.NORMAL_MEMORY, heapSizes[0], heapSizes[1]);
            }
            if (oldSpace == 0L) {
                throw new FlexotaskValidationException("Unable to instantiate private heap for " + task.getName());
            }
            FlexotaskRoot root = new FlexotaskRoot();
            FlexotaskInputPort[] inputPorts = new FlexotaskInputPort[numInputPorts];
            root.setInputPorts(inputPorts);
            for (int i = 0; i < inputPorts.length; i++) {
                inputPorts[i] = new FlexotaskInputPortImpl(task.getInputPortBuffering()[i]);
            }
            FlexotaskOutputPort[] outputPorts = new FlexotaskOutputPort[outputPortClasses.length];
            root.setOutputPorts(outputPorts);
            for (int i = 0; i < outputPorts.length; i++) {
                outputPorts[i] = new FlexotaskOutputPortImpl(outputPortClasses[i], task.getOutputPortBuffering()[i]);
            }
            Flexotask flexotask;
            try {
                flexotask = (Flexotask) implClass.newInstance();
                root.setTask(flexotask);
            } catch (Exception e) {
                throw new FlexotaskValidationException("Could not instantiate Runnable of class " + implClass.getName() + " for task " + task.getName(), e);
            }
            if (flexotask instanceof AtomicFlexotask) {
                TransactionalOperations.registerFields(((AtomicFlexotask) flexotask).getReachableFieldTable());
            }
            if (!task.isWeaklyIsolated()) {
                parameter = FlexotaskSystemSupport.deepClone(parameter, null);
            }
            flexotask.initialize(root.getInputPorts(), root.getOutputPorts(), parameter);
            if (flexotask instanceof TimeAware) {
                ((TimeAware) flexotask).setTimer(timer);
            }
            if (portInitValue != null) {
                portInitValue = FlexotaskSystemSupport.deepClone(portInitValue, null);
                if (root.getInputPorts().length > 0) {
                    ((FlexotaskInputPortImpl) root.getInputPorts()[0]).setValue(portInitValue);
                }
                if (root.getOutputPorts().length > 0) {
                    root.getOutputPorts()[0].setValue(portInitValue);
                }
            }
            FlexotaskSystemSupport.setFlexotaskRoot(root);
            flexotask = null;
            FlexotaskSystemSupport.runOnPublicHeap();
            FlexotaskSystemSupport.collectHeap(root);
            FlexotaskTaskTracer tracer = tracerFactory.newTaskTracer(task.getName());
            if (task instanceof FlexotaskPredicateTemplate) {
                return new FlexotaskPredicateControllerImpl(root, task.getName(), tracer);
            }
            if (root.getTask() instanceof AtomicFlexotask) {
                return new AtomicFlexotaskController(root, task.getName(), tracer);
            } else {
                return new FlexotaskControllerImpl(root, task.getName(), tracer);
            }
        } finally {
            FlexotaskSystemSupport.runOnPublicHeap();
        }
    }

    /**
   * Set the classloader to use.  This method is only public for unit testing and is illegal to call
   *  in an actual FlexotaskVM
   * @param classloader the classloader to use to resolve classes
   */
    public static void setClassLoader(ClassLoader classloader) {
        if (FlexotaskSystemSupport.isFlexotaskVM()) {
            throw new SecurityException("Attempt to alter class loader in a Flexotask VM");
        }
        cl = classloader;
    }
}
