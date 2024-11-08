package edu.rice.cs.cunit.record;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import edu.rice.cs.cunit.util.Debug;
import java.io.PrintWriter;

/**
 * This class processes incoming JDI events.
 *
 * @author Mathias Ricken
 */
public class EventThread extends Thread {

    private final VirtualMachine _vm;

    private final PrintWriter _writer;

    private boolean _connected = true;

    private boolean _vmDied = true;

    private IViewAdapter _viewAdapter;

    private MethodEntryRequest _mainMethodEntryRequest;

    private MethodEntryRequest _syncPointBufferEntryRequest;

    private MethodExitRequest _syncPointBufferExitRequest;

    private MethodExitRequest _threadExitExitRequest;

    private EventRequestManager _eventRequestManager;

    private String _mainClassName = "???";

    private String _pullReason = "";

    /**
     * Debug log name.
     */
    public static final String DREV = "record.event.verbose";

    /**
     * Constructor.
     *
     * @param vm     remote VM
     * @param writer writer for debug output
     * @param viewAdapter adapter to view
     */
    EventThread(VirtualMachine vm, PrintWriter writer, IViewAdapter viewAdapter) {
        super("event-handler");
        _vm = vm;
        _writer = writer;
        _viewAdapter = viewAdapter;
    }

    /**
     * Kill the VM.
     */
    public void shutDown() {
        if (_connected) {
            _vm.exit(0);
        }
    }

    /**
     * Run the event handling thread. As long as we are _connected, get event sets off the queue and dispatch the events
     * within them.
     */
    public void run() {
        EventQueue queue = _vm.eventQueue();
        while (_connected) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator it = eventSet.eventIterator();
                while (it.hasNext()) {
                    handleEvent(it.nextEvent());
                }
                eventSet.resume();
            } catch (InterruptedException exc) {
                System.out.println(exc);
            } catch (VMDisconnectedException discExc) {
                System.out.println(discExc);
                handleDisconnectedException();
                break;
            } catch (Throwable t) {
                System.err.println(t);
                t.printStackTrace(System.err);
                shutDown();
            }
        }
    }

    /**
     * Create the desired event requests, and enable them so that we will get events.
     */
    void setEventRequests() {
        _eventRequestManager = _vm.eventRequestManager();
        _mainMethodEntryRequest = _eventRequestManager.createMethodEntryRequest();
        _mainMethodEntryRequest.addClassFilter("*");
        _mainMethodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        _mainMethodEntryRequest.enable();
        _syncPointBufferEntryRequest = _eventRequestManager.createMethodEntryRequest();
        _syncPointBufferEntryRequest.addClassFilter("edu.rice.cs.cunit.SyncPointBuffer");
        _syncPointBufferEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        _syncPointBufferEntryRequest.enable();
        _syncPointBufferExitRequest = _eventRequestManager.createMethodExitRequest();
        _syncPointBufferExitRequest.addClassFilter("edu.rice.cs.cunit.SyncPointBuffer");
        _syncPointBufferExitRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        _syncPointBufferExitRequest.disable();
        _threadExitExitRequest = _eventRequestManager.createMethodExitRequest();
        _threadExitExitRequest.addClassFilter("java.lang.Thread");
        _threadExitExitRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        _threadExitExitRequest.enable();
    }

    /**
     * Dispatch incoming events.
     *
     * @param event debugger event
     */
    private void handleEvent(Event event) {
        if (event instanceof ExceptionEvent) {
        } else if (event instanceof AccessWatchpointEvent) {
        } else if (event instanceof MethodEntryEvent) {
            methodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            methodExitEvent((MethodExitEvent) event);
        } else if (event instanceof StepEvent) {
        } else if (event instanceof ThreadStartEvent) {
        } else if (event instanceof ThreadDeathEvent) {
        } else if (event instanceof ClassPrepareEvent) {
        } else if (event instanceof VMStartEvent) {
            vmStartEvent((VMStartEvent) event);
        } else if (event instanceof VMDeathEvent) {
            vmDeathEvent((VMDeathEvent) event);
        } else if (event instanceof VMDisconnectEvent) {
            vmDisconnectEvent((VMDisconnectEvent) event);
        } else {
            throw new Error("Unexpected event type: " + event.getClass().getName());
        }
    }

    /**
     * A VMDisconnectedException has happened while dealing with another event. We need to flush the event queue,
     * dealing only with exit events (VMDeath, VMDisconnect) so that we terminate correctly.
     */
    void handleDisconnectedException() {
        EventQueue queue = _vm.eventQueue();
        while (_connected) {
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

    /**
     * Debug VM was started.
     *
     * @param event VM start event
     */
    private void vmStartEvent(VMStartEvent event) {
        _writer.println("// -- VM Started --");
        _writer.flush();
        _viewAdapter.vmStartEvent();
        _viewAdapter.updateSynchronizationPoints(false, "VM start");
    }

    /**
     * A method was entered.
     *
     * @param event method entry event
     */
    private void methodEntryEvent(MethodEntryEvent event) {
        Debug.out.println(DREV, "methodEntryEvent");
        if (event.method().toString().endsWith(".main(java.lang.String[])")) {
            _mainMethodEntryRequest.disable();
            _viewAdapter.updateSynchronizationPoints(false, "main entered");
            _viewAdapter.setMainEntered();
        } else if ((event.method().toString().startsWith("edu.rice.cs.cunit.SyncPointBuffer.transfer")) || (event.method().toString().startsWith("edu.rice.cs.cunit.SyncPointBuffer.compactTransfer"))) {
            _writer.println("// Note: Updating sync points. edu.rice.cs.cunit.SyncPointBuffer.transfer or compactTransfer entered by thread id " + event.thread().uniqueID());
            _viewAdapter.updateSynchronizationPoints(true, "push");
        } else if (event.method().toString().startsWith("edu.rice.cs.cunit.SyncPointBuffer.compactImmediateTransfer")) {
            _writer.println("// Note: Updating sync points. edu.rice.cs.cunit.SyncPointBuffer.compactImmediateTransfer entered by thread id " + event.thread().uniqueID());
            _viewAdapter.updateSynchronizationPointsImmediately(true, "immediate push");
        }
    }

    /**
     * A method was exited.
     *
     * @param event method exit event
     */
    private void methodExitEvent(MethodExitEvent event) {
        Debug.out.println(DREV, "methodExitEvent");
        if (event.method().toString().startsWith("java.lang.Thread.exit")) {
            _viewAdapter.updateSynchronizationPoints(false, "User thread exited");
        } else if ((event.method().toString().startsWith("edu.rice.cs.cunit.SyncPointBuffer.add")) || (event.method().toString().startsWith("edu.rice.cs.cunit.SyncPointBuffer.compactAdd"))) {
            _writer.println("// Note: Updating sync points. edu.rice.cs.cunit.SyncPointBuffer.add or compactAdd exited by thread id " + event.thread().uniqueID());
            String lastReason = _pullReason;
            setSyncPointBufferExitRequestEnable(false, "");
            _viewAdapter.updateSynchronizationPoints(false, "delayed pull (" + lastReason + ")");
        }
    }

    /**
     * The VM died.
     *
     * @param event VM death event
     */
    public void vmDeathEvent(VMDeathEvent event) {
        Debug.out.println(DREV, "vmDeathEvent");
        _vmDied = true;
        _writer.println("// -- The application exited --");
        _writer.flush();
    }

    /**
     * The VM got disconnected.
     *
     * @param event VM disconnect event
     */
    public void vmDisconnectEvent(VMDisconnectEvent event) {
        Debug.out.println(DREV, "vmDisconnectEvent");
        _connected = false;
        if (!_vmDied) {
            _writer.println("// -- The application has been disconnected --");
            _writer.flush();
        }
    }

    /**
     * Accessor for the connected flag.
     * @return connected flag
     */
    public boolean isConnected() {
        return _connected;
    }

    /**
     * Accessor for the VM died flag.
     * @return VM died flag
     */
    public boolean isVmDied() {
        return _vmDied;
    }

    /**
     * Returns the name of the main class.
     * @return name of main class
     */
    public String getMainClassName() {
        return _mainClassName;
    }

    /**
     * Enables or disables the SyncPointBuffer method exit event.
     * @param enable true to enable, false to disable
     */
    public void setSyncPointBufferExitRequestEnable(boolean enable, String reason) {
        if (enable) {
            _syncPointBufferExitRequest.enable();
        } else {
            _syncPointBufferExitRequest.disable();
        }
        _pullReason = reason;
    }
}
