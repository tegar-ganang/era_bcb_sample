package debugEngine;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import dataTree.*;
import java.util.*;
import java.io.PrintWriter;

/**
 * This class processes incoming JDI events and displays them
 * also it generates  de Jdd DataTree 
 *
 * @version     @(#) EventThread.java 
 */
public class EventThread extends Thread {

    private final VirtualMachine vm;

    private final String[] excludes;

    private final PrintWriter writer;

    static String nextBaseIndent = "";

    private boolean connected = true;

    private boolean vmDied = true;

    private Map traceMap = new HashMap();

    EventThread(VirtualMachine vm, String[] excludes, PrintWriter writer) {
        super("event-handler");
        this.vm = vm;
        this.excludes = excludes;
        this.writer = writer;
    }

    /**
	 * Run the event handling thread.  
	 * As long as we are connected, get event sets off 
	 * the queue and dispatch the events within them.
	 */
    public void run() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    handleEvent(it.nextEvent());
                }
                eventSet.resume();
            } catch (InterruptedException exc) {
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
    }

    /**
	 * Create the desired event requests, and enable 
	 * them so that we will get events.
	 * @param excludes     Class patterns for which we don't want events
	 */
    void setEventRequests() {
        EventRequestManager mgr = vm.eventRequestManager();
        ExceptionRequest excReq = mgr.createExceptionRequest(null, true, true);
        excReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        excReq.enable();
        MethodEntryRequest menr = mgr.createMethodEntryRequest();
        for (int i = 0; i < excludes.length; ++i) {
            menr.addClassExclusionFilter(excludes[i]);
        }
        menr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        menr.enable();
        MethodExitRequest mexr = mgr.createMethodExitRequest();
        for (int i = 0; i < excludes.length; ++i) {
            mexr.addClassExclusionFilter(excludes[i]);
        }
        mexr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        mexr.enable();
        ThreadDeathRequest tdr = mgr.createThreadDeathRequest();
        tdr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        tdr.enable();
        ClassPrepareRequest cpr = mgr.createClassPrepareRequest();
        for (int i = 0; i < excludes.length; ++i) {
            cpr.addClassExclusionFilter(excludes[i]);
        }
        cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        cpr.enable();
    }

    /**
	 * This class keeps context on events in one thread.
	 * In this implementation, context is the indentation prefix.
	 */
    class ThreadTrace {

        final ThreadReference thread;

        final String baseIndent;

        static final String threadDelta = "                     ";

        StringBuffer indent;

        ThreadTrace(ThreadReference thread) {
            this.thread = thread;
            this.baseIndent = nextBaseIndent;
            indent = new StringBuffer(baseIndent);
            nextBaseIndent += threadDelta;
            println("====== " + thread.name() + " ======");
        }

        private void println(String str) {
            writer.print(indent);
            writer.println(str);
        }

        void methodEntryEvent(MethodEntryEvent event) {
            try {
                Vector aux = new Vector();
                MethodCallNode auxNode = new MethodCallNode();
                Hashtable objectReferences = auxNode.getObjectTable();
                println(event.method().name() + "  --  " + event.method().declaringType().name());
                auxNode.set_methodName(event.method().name());
                auxNode.setDeclaringType(event.method().declaringType().name());
                if (thread.frame(0).thisObject() != null) {
                    VariableNode callerVarNode;
                    if (objectReferences.containsKey(new Long(thread.frame(0).thisObject().uniqueID()))) {
                        callerVarNode = (VariableNode) objectReferences.get(new Long(thread.frame(0).thisObject().uniqueID()));
                    } else {
                        String callerName = "This";
                        if (thread.frame(1) != null) {
                            try {
                                List vars = thread.frame(1).visibleVariables();
                                for (int i = 0; i < vars.size(); i++) {
                                    if (thread.frame(1).getValue((LocalVariable) vars.get(i)).equals(thread.frame(0).thisObject())) callerName = ((LocalVariable) vars.get(i)).name();
                                }
                            } catch (Exception e) {
                                System.err.println("JDD Information: Error trying to get visible variables information, class not loaded ");
                            }
                        }
                        callerVarNode = new VariableNode(callerName, thread.frame(0).thisObject().referenceType().name());
                        objectReferences.put(new Long(thread.frame(0).thisObject().uniqueID()), callerVarNode);
                        callerVarNode.set_value(thread.frame(0).thisObject(), objectReferences);
                    }
                    auxNode.setCallerObject(callerVarNode);
                }
                for (int i = 0; i < event.method().arguments().size(); i++) {
                    VariableNode auxVarNode = null;
                    String name = ((LocalVariable) event.method().arguments().get(i)).name();
                    if (thread.frameCount() > 1) if (thread.frame(1) != null) {
                        try {
                            List vars = thread.frame(1).visibleVariables();
                            for (int j = 0; j < vars.size(); j++) {
                                if (thread.frame(1).getValue((LocalVariable) vars.get(j)).equals(thread.frame(0).getValue((LocalVariable) event.method().arguments().get(i)))) name = ((LocalVariable) vars.get(j)).name();
                            }
                        } catch (Exception e) {
                            System.err.println("JDD Information: Error trying to get visble variables information");
                        }
                    }
                    String type = ((LocalVariable) event.method().arguments().get(i)).typeName();
                    Value value = thread.frame(0).getValue((LocalVariable) event.method().arguments().get(i));
                    if (value instanceof PrimitiveValue || value instanceof StringReference) {
                        auxVarNode = new VariableNode(String.valueOf(name), ((LocalVariable) event.method().arguments().get(i)).typeName());
                        auxVarNode.set_value(value, objectReferences);
                        println("              argumento: " + String.valueOf(name) + "  de tipo: " + type + " con valor: " + String.valueOf(value));
                    } else {
                        if (value instanceof ArrayReference || value instanceof ObjectReference) {
                            ObjectReference objAux = (ObjectReference) value;
                            if (objectReferences.containsKey(new Long(objAux.uniqueID()))) {
                                auxVarNode = (VariableNode) objectReferences.get(new Long(objAux.uniqueID()));
                            } else {
                                auxVarNode = new VariableNode(String.valueOf(name), ((LocalVariable) event.method().arguments().get(i)).typeName());
                                objectReferences.put(new Long(objAux.uniqueID()), auxVarNode);
                                auxVarNode.set_value(value, objectReferences);
                                println("              argumento: " + String.valueOf(name) + "  de tipo: " + type + " con valor: " + String.valueOf(value));
                            }
                        } else if (value instanceof VoidValue) {
                            auxVarNode = new VariableNode(String.valueOf(name), ((LocalVariable) event.method().arguments().get(i)).typeName());
                            auxVarNode.set_value(value, objectReferences);
                            println("              argumento: " + String.valueOf(name) + "  de tipo: " + type + " con valor: " + String.valueOf(value));
                        }
                    }
                    if (auxVarNode != null) aux.add(auxVarNode);
                }
                List auxList = event.method().allLineLocations();
                Iterator it = auxList.iterator();
                int[] lines = new int[auxList.size() + 1];
                int element = 0;
                while (it.hasNext()) {
                    lines[element] = ((Location) it.next()).lineNumber() - 2;
                    element++;
                }
                auxNode.setCodeLines(lines);
                auxNode.setSourcePath(event.method().location().sourcePath());
                auxNode.set_arguments(aux);
                DataTree.AddNode(auxNode);
                indent.append("| ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void methodExitEvent(MethodExitEvent event) {
            try {
                String returnName = event.method().name();
                Value returnValue = event.returnValue();
                println("Saliendo de " + returnName + " con valor " + returnValue.toString());
                Hashtable objectReferences = DataTree.getCurrentNode().getObjectTable();
                VariableNode auxVarNode = null;
                if (returnValue instanceof PrimitiveValue || returnValue instanceof StringReference) {
                    auxVarNode = new VariableNode(String.valueOf(returnName), returnValue.type().name());
                    auxVarNode.set_value(returnValue, objectReferences);
                } else {
                    if (returnValue instanceof ArrayReference || returnValue instanceof ObjectReference) {
                        ObjectReference objAux = (ObjectReference) returnValue;
                        if (objectReferences.containsKey(new Long(objAux.uniqueID()))) {
                            auxVarNode = (VariableNode) objectReferences.get(new Long(objAux.uniqueID()));
                        } else {
                            auxVarNode = new VariableNode(String.valueOf(returnName), returnValue.type().name());
                            objectReferences.put(new Long(objAux.uniqueID()), auxVarNode);
                            auxVarNode.set_value(returnValue, objectReferences);
                        }
                    } else if (returnValue instanceof VoidValue) {
                        auxVarNode = new VariableNode(String.valueOf(returnName), returnValue.type().name());
                        auxVarNode.set_value(returnValue, objectReferences);
                    }
                }
                DataTree.getCurrentNode().setReturnValue(auxVarNode);
                DataTree.CompleteNode();
                indent.setLength(indent.length() - 2);
            } catch (Exception e) {
                System.out.println("ERRORR");
            }
        }

        void fieldWatchEvent(ModificationWatchpointEvent event) {
            Field field = event.field();
            Value value = event.valueToBe();
            println("    " + field.name() + " = " + value);
            try {
                MethodCallNode current = DataTree.getCurrentNode();
                Vector args = current.getArguments();
                VariableNode var;
                for (int i = 0; i < args.size(); i++) {
                    var = (VariableNode) args.elementAt(i);
                    if (var.getName().compareTo(field.name()) == 0) {
                        var.setAsModified();
                        break;
                    }
                }
                VariableNode aux = current.getCallerObject();
                args = aux.getFields();
                if (args != null) for (int i = 0; i < args.size(); i++) {
                    var = (VariableNode) args.elementAt(i);
                    if (var != null) if (var.getName().compareTo(field.name()) == 0) {
                        VariableNode auxVar = new VariableNode();
                        auxVar = (VariableNode) var.clone();
                        var.set_value(event.valueToBe(), current.getObjectTable());
                        aux.setAsModified();
                        var.setAsModified();
                        var.setOldValue(auxVar);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error trying to set " + event.field().name() + " to modified in the DataTree Current node");
                e.printStackTrace();
            }
        }

        void exceptionEvent(ExceptionEvent event) {
            println("Exception: " + event.exception() + " catch: " + event.catchLocation());
            EventRequestManager mgr = vm.eventRequestManager();
            StepRequest req = mgr.createStepRequest(thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO);
            req.addCountFilter(1);
            req.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            req.enable();
        }

        void stepEvent(StepEvent event) {
            int cnt = 0;
            indent = new StringBuffer(baseIndent);
            try {
                cnt = thread.frameCount();
            } catch (IncompatibleThreadStateException exc) {
            }
            while (cnt-- > 0) {
                indent.append("| ");
            }
            EventRequestManager mgr = vm.eventRequestManager();
            mgr.deleteEventRequest(event.request());
        }

        void threadDeathEvent(ThreadDeathEvent event) {
            indent = new StringBuffer(baseIndent);
            println("====== " + thread.name() + " end ======");
        }

        public void threadStartEvent(ThreadStartEvent event) {
        }
    }

    /**
	 * Returns the ThreadTrace instance for the specified thread,
	 * creating one if needed.
	 */
    ThreadTrace threadTrace(ThreadReference thread) {
        ThreadTrace trace = (ThreadTrace) traceMap.get(thread);
        if (trace == null) {
            trace = new ThreadTrace(thread);
            traceMap.put(thread, trace);
        }
        return trace;
    }

    /**
	 * Dispatch incoming events
	 */
    private boolean handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            return exceptionEvent((ExceptionEvent) event);
        } else if (event instanceof ModificationWatchpointEvent) {
            return fieldWatchEvent((ModificationWatchpointEvent) event);
        } else if (event instanceof MethodEntryEvent) {
            return methodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            return methodExitEvent((MethodExitEvent) event);
        } else if (event instanceof StepEvent) {
            return stepEvent((StepEvent) event);
        } else if (event instanceof ThreadStartEvent) {
            return threadStartEvent((ThreadStartEvent) event);
        } else if (event instanceof ThreadDeathEvent) {
            return threadDeathEvent((ThreadDeathEvent) event);
        } else if (event instanceof ClassPrepareEvent) {
            return classPrepareEvent((ClassPrepareEvent) event);
        } else if (event instanceof VMStartEvent) {
            return vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            return vmDeathEvent((VMDeathEvent) event);
        } else if (event instanceof VMDisconnectEvent) {
            return vmDisconnectEvent((VMDisconnectEvent) event);
        } else {
            throw new Error("Unexpected event type");
        }
    }

    /***
	 * A VMDisconnectedException has happened while dealing with
	 * another event. We need to flush the event queue, dealing only
	 * with exit events (VMDeath, VMDisconnect) so that we terminate
	 * correctly.
	 */
    synchronized void handleDisconnectedException() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator iter = eventSet.eventIterator();
                while (iter.hasNext()) {
                    Event event = iter.nextEvent();
                    if (event instanceof VMDeathEvent) {
                        vmDeathEvent((VMDeathEvent) event);
                    } else if (event instanceof VMDisconnectEvent) {
                        vmDisconnectEvent((VMDisconnectEvent) event);
                    }
                }
                eventSet.resume();
            } catch (InterruptedException exc) {
            }
        }
    }

    private boolean vmStartEvent(VMStartEvent event) {
        writer.println("-- VM Started --");
        return true;
    }

    private boolean methodEntryEvent(MethodEntryEvent event) {
        threadTrace(event.thread()).methodEntryEvent(event);
        return true;
    }

    private boolean methodExitEvent(MethodExitEvent event) {
        threadTrace(event.thread()).methodExitEvent(event);
        return true;
    }

    private boolean stepEvent(StepEvent event) {
        threadTrace(event.thread()).stepEvent(event);
        return true;
    }

    private boolean threadStartEvent(ThreadStartEvent event) {
        threadTrace(event.thread()).threadStartEvent(event);
        ThreadStartEvent tse = (ThreadStartEvent) event;
        return false;
    }

    private boolean fieldWatchEvent(ModificationWatchpointEvent event) {
        threadTrace(event.thread()).fieldWatchEvent(event);
        return true;
    }

    boolean threadDeathEvent(ThreadDeathEvent event) {
        ThreadTrace trace = (ThreadTrace) traceMap.get(event.thread());
        if (trace != null) {
            trace.threadDeathEvent(event);
        }
        return false;
    }

    /**
	 * A new class has been loaded.  
	 * Set watchpoints on each of its fields
	 * @return 
	 */
    private boolean classPrepareEvent(ClassPrepareEvent event) {
        EventRequestManager mgr = vm.eventRequestManager();
        List fields = event.referenceType().visibleFields();
        for (Iterator it = fields.iterator(); it.hasNext(); ) {
            Field field = (Field) it.next();
            ModificationWatchpointRequest req = mgr.createModificationWatchpointRequest(field);
            for (int i = 0; i < excludes.length; ++i) {
                req.addClassExclusionFilter(excludes[i]);
            }
            req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            req.enable();
        }
        return false;
    }

    private boolean exceptionEvent(ExceptionEvent event) {
        ThreadTrace trace = (ThreadTrace) traceMap.get(event.thread());
        if (trace != null) {
            trace.exceptionEvent(event);
        }
        return true;
    }

    public boolean vmDeathEvent(VMDeathEvent event) {
        vmDied = true;
        writer.println("-- The application exited --");
        return false;
    }

    public boolean vmDisconnectEvent(VMDisconnectEvent event) {
        connected = false;
        if (!vmDied) {
            writer.println("-- The application has been disconnected --");
        }
        return false;
    }
}
