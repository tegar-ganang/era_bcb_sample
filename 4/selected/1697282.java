package com.sun.tools.example.trace;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import java.util.*;
import java.io.PrintWriter;

/**
 * This class processes incoming JDI events and displays them
 *
 * @author Robert Field
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
     * @param watchFields  Do we want to watch assignments to fields
     */
    void setEventRequests(boolean watchFields) {
        EventRequestManager mgr = vm.eventRequestManager();
        ExceptionRequest excReq = mgr.createExceptionRequest(null, true, true);
        excReq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        excReq.enable();
        MethodEntryRequest menr = mgr.createMethodEntryRequest();
        for (int i = 0; i < excludes.length; ++i) {
            menr.addClassExclusionFilter(excludes[i]);
        }
        menr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        menr.enable();
        MethodExitRequest mexr = mgr.createMethodExitRequest();
        for (int i = 0; i < excludes.length; ++i) {
            mexr.addClassExclusionFilter(excludes[i]);
        }
        mexr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        mexr.enable();
        ThreadDeathRequest tdr = mgr.createThreadDeathRequest();
        tdr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        tdr.enable();
        if (watchFields) {
            ClassPrepareRequest cpr = mgr.createClassPrepareRequest();
            for (int i = 0; i < excludes.length; ++i) {
                cpr.addClassExclusionFilter(excludes[i]);
            }
            cpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            cpr.enable();
        }
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
            println(event.method().name() + "  --  " + event.method().declaringType().name());
            indent.append("| ");
        }

        void methodExitEvent(MethodExitEvent event) {
            indent.setLength(indent.length() - 2);
        }

        void fieldWatchEvent(ModificationWatchpointEvent event) {
            Field field = event.field();
            Value value = event.valueToBe();
            println("    " + field.name() + " = " + value);
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
    private void handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
            exceptionEvent((ExceptionEvent) event);
        } else if (event instanceof ModificationWatchpointEvent) {
            fieldWatchEvent((ModificationWatchpointEvent) event);
        } else if (event instanceof MethodEntryEvent) {
            methodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            methodExitEvent((MethodExitEvent) event);
        } else if (event instanceof StepEvent) {
            stepEvent((StepEvent) event);
        } else if (event instanceof ThreadDeathEvent) {
            threadDeathEvent((ThreadDeathEvent) event);
        } else if (event instanceof ClassPrepareEvent) {
            classPrepareEvent((ClassPrepareEvent) event);
        } else if (event instanceof VMStartEvent) {
            vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            vmDeathEvent((VMDeathEvent) event);
        } else if (event instanceof VMDisconnectEvent) {
            vmDisconnectEvent((VMDisconnectEvent) event);
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

    private void vmStartEvent(VMStartEvent event) {
        writer.println("-- VM Started --");
    }

    private void methodEntryEvent(MethodEntryEvent event) {
        threadTrace(event.thread()).methodEntryEvent(event);
    }

    private void methodExitEvent(MethodExitEvent event) {
        threadTrace(event.thread()).methodExitEvent(event);
    }

    private void stepEvent(StepEvent event) {
        threadTrace(event.thread()).stepEvent(event);
    }

    private void fieldWatchEvent(ModificationWatchpointEvent event) {
        threadTrace(event.thread()).fieldWatchEvent(event);
    }

    void threadDeathEvent(ThreadDeathEvent event) {
        ThreadTrace trace = (ThreadTrace) traceMap.get(event.thread());
        if (trace != null) {
            trace.threadDeathEvent(event);
        }
    }

    /**
     * A new class has been loaded.
     * Set watchpoints on each of its fields
     */
    private void classPrepareEvent(ClassPrepareEvent event) {
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
    }

    private void exceptionEvent(ExceptionEvent event) {
        ThreadTrace trace = (ThreadTrace) traceMap.get(event.thread());
        if (trace != null) {
            trace.exceptionEvent(event);
        }
    }

    public void vmDeathEvent(VMDeathEvent event) {
        vmDied = true;
        writer.println("-- The application exited --");
    }

    public void vmDisconnectEvent(VMDisconnectEvent event) {
        connected = false;
        if (!vmDied) {
            writer.println("-- The application has been disconnected --");
        }
    }
}
