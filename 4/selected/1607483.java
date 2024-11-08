package de.jassda.jabyba.jdwp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * All constants which are defined in the JDWP.
 * 
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class JDWP {

    public static class CommandSet {

        public static class VirtualMachine {

            public static final int ID = 1;

            public static final int Version = 1;

            public static final int ClassesBySignature = 2;

            public static final int AllClasses = 3;
        }

        public static class ReferenceType {

            public static final int ID = 2;
        }

        public static class ArrayReference {

            public static final int ID = 13;
        }

        public static class ObjectReference {

            public static final int ID = 9;
        }

        public static class ClassType {

            public static final int ID = 3;
        }

        public static class EventRequest {

            public static final int ID = 15;
        }

        public static class Event {

            public static final byte ID = 64;

            public static final byte Composite = 100;
        }
    }

    public static class ClassStatus {

        /**
		 *    */
        public static final int VERIFIED = 1;

        /**
		 *    */
        public static final int PREPARED = 2;

        /**
		 *    */
        public static final int INITIALIZED = 4;

        /**
		 *   */
        public static final int ERROR = 8;
    }

    public static class InvokeOptions {

        /**
		 * otherwise, all threads started.    */
        public static final int INVOKE_SINGLE_THREADED = 0x01;

        /**
		 * otherwise, normal virtual invoke (instance methods only)   */
        public static final int INVOKE_NONVIRTUAL = 0x02;
    }

    public static class ThreadStatus {

        /**
		 *    */
        public static final int ZOMBIE = 0;

        /**
		 *    */
        public static final int RUNNING = 1;

        /**
		 *    */
        public static final int SLEEPING = 2;

        /**
		 *    */
        public static final int MONITOR = 3;

        /**
		 *   */
        public static final int WAIT = 4;
    }

    public static class Tag {

        /**
		 * '[' - an array object (objectID size).    */
        public static final int ARRAY = 91;

        /**
		 * 'B' - a byte value (1 byte).    */
        public static final int BYTE = 66;

        /**
		 * 'C' - a character value (2 bytes).    */
        public static final int CHAR = 67;

        /**
		 * 'L' - an object (objectID size).    */
        public static final int OBJECT = 76;

        /**
		 * 'F' - a float value (4 bytes).    */
        public static final int FLOAT = 70;

        /**
		 * 'D' - a double value (8 bytes).    */
        public static final int DOUBLE = 68;

        /**
		 * 'I' - an int value (4 bytes).    */
        public static final int INT = 73;

        /**
		 * 'J' - a long value (8 bytes).    */
        public static final int LONG = 74;

        /**
		 * 'S' - a short value (2 bytes).    */
        public static final int SHORT = 83;

        /**
		 * 'V' - a void value (no bytes).    */
        public static final int VOID = 86;

        /**
		 * 'Z' - a boolean value (1 byte).    */
        public static final int BOOLEAN = 90;

        /**
		 * 's' - a String object (objectID size).    */
        public static final int STRING = 115;

        /**
		 * 't' - a Thread object (objectID size).    */
        public static final int THREAD = 116;

        /**
		 * 'g' - a ThreadGroup object (objectID size).    */
        public static final int THREAD_GROUP = 103;

        /**
		 * 'l' - a ClassLoader object (objectID size).    */
        public static final int CLASS_LOADER = 108;

        /**
		 * 'c' - a class object object (objectID size).   */
        public static final int CLASS_OBJECT = 99;
    }

    public static class Error {

        /**
		 * No error has occurred.    */
        public static final int NONE = 0;

        /**
		 * Passed thread is null, is not a valid thread or has exited.    */
        public static final int INVALID_THREAD = 10;

        /**
		 * Thread group invalid.    */
        public static final int INVALID_THREAD_GROUP = 11;

        /**
		 * Invalid priority.    */
        public static final int INVALID_PRIORITY = 12;

        /**
		 * If the specified thread has not been suspended by an event.    */
        public static final int THREAD_NOT_SUSPENDED = 13;

        /**
		 * Thread already suspended.    */
        public static final int THREAD_SUSPENDED = 14;

        /**
		 * If this reference type has been unloaded and garbage collected.    */
        public static final int INVALID_OBJECT = 20;

        /**
		 * Invalid class.    */
        public static final int INVALID_CLASS = 21;

        /**
		 * Class has been loaded but not yet prepared.    */
        public static final int CLASS_NOT_PREPARED = 22;

        /**
		 * Invalid method.    */
        public static final int INVALID_METHODID = 23;

        /**
		 * Invalid location.    */
        public static final int INVALID_LOCATION = 24;

        /**
		 * Invalid field.    */
        public static final int INVALID_FIELDID = 25;

        /**
		 * Invalid jframeID.    */
        public static final int INVALID_FRAMEID = 30;

        /**
		 * There are no more Java or JNI frames on the call stack.    */
        public static final int NO_MORE_FRAMES = 31;

        /**
		 * Information about the frame is not available.    */
        public static final int OPAQUE_FRAME = 32;

        /**
		 * Operation can only be performed on current frame.    */
        public static final int NOT_CURRENT_FRAME = 33;

        /**
		 * The variable is not an appropriate type for the function used.    */
        public static final int TYPE_MISMATCH = 34;

        /**
		 * Invalid slot.    */
        public static final int INVALID_SLOT = 35;

        /**
		 * Item already set.    */
        public static final int DUPLICATE = 40;

        /**
		 * Desired element not found.    */
        public static final int NOT_FOUND = 41;

        /**
		 * Invalid monitor.    */
        public static final int INVALID_MONITOR = 50;

        /**
		 * This thread doesn't own the monitor.    */
        public static final int NOT_MONITOR_OWNER = 51;

        /**
		 * The call has been interrupted before completion.    */
        public static final int INTERRUPT = 52;

        /**
		 * The virtual machine attempted to read a class file and determined that the file is malformed or otherwise cannot be interpreted as a class file.    */
        public static final int INVALID_CLASS_FORMAT = 60;

        /**
		 * A circularity has been detected while initializing a class.    */
        public static final int CIRCULAR_CLASS_DEFINITION = 61;

        /**
		 * The verifier detected that a class file, though well formed, contained some sort of internal inconsistency or security problem.    */
        public static final int FAILS_VERIFICATION = 62;

        /**
		 * Adding methods has not been implemented.    */
        public static final int ADD_METHOD_NOT_IMPLEMENTED = 63;

        /**
		 * Schema change has not been implemented.    */
        public static final int SCHEMA_CHANGE_NOT_IMPLEMENTED = 64;

        /**
		 * The state of the thread has been modified, and is now inconsistent.    */
        public static final int INVALID_TYPESTATE = 65;

        /**
		 * A direct superclass is different for the new class version, or the set of directly implemented interfaces is different and canUnrestrictedlyRedefineClasses is false.    */
        public static final int HIERARCHY_CHANGE_NOT_IMPLEMENTED = 66;

        /**
		 * The new class version does not declare a method declared in the old class version and canUnrestrictedlyRedefineClasses is false.    */
        public static final int DELETE_METHOD_NOT_IMPLEMENTED = 67;

        /**
		 * A class file has a version number not supported by this VM.    */
        public static final int UNSUPPORTED_VERSION = 68;

        /**
		 * The class name defined in the new class file is different from the name in the old class object.    */
        public static final int NAMES_DONT_MATCH = 69;

        /**
		 * The new class version has different modifiers and and canUnrestrictedlyRedefineClasses is false.    */
        public static final int CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 70;

        /**
		 * A method in the new class version has different modifiers than its counterpart in the old class version and and canUnrestrictedlyRedefineClasses is false.    */
        public static final int METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;

        /**
		 * The functionality is not implemented in this virtual machine.    */
        public static final int NOT_IMPLEMENTED = 99;

        /**
		 * Invalid pointer.    */
        public static final int NULL_POINTER = 100;

        /**
		 * Desired information is not available.    */
        public static final int ABSENT_INFORMATION = 101;

        /**
		 * The specified event type id is not recognized.    */
        public static final int INVALID_EVENT_TYPE = 102;

        /**
		 * Illegal argument.    */
        public static final int ILLEGAL_ARGUMENT = 103;

        /**
		 * The function needed to allocate memory and no more memory was available for allocation.    */
        public static final int OUT_OF_MEMORY = 110;

        /**
		 * Debugging has not been enabled in this virtual machine. JVMDI cannot be used.    */
        public static final int ACCESS_DENIED = 111;

        /**
		 * The virtual machine is not running.    */
        public static final int VM_DEAD = 112;

        /**
		 * An unexpected internal error has occurred.    */
        public static final int INTERNAL = 113;

        /**
		 * The thread being used to call this function is not attached to the virtual machine. Calls must be made from attached threads.    */
        public static final int UNATTACHED_THREAD = 115;

        /**
		 * object type id or class tag.    */
        public static final int INVALID_TAG = 500;

        /**
		 * Previous invoke not complete.    */
        public static final int ALREADY_INVOKING = 502;

        /**
		 * Index is invalid.    */
        public static final int INVALID_INDEX = 503;

        /**
		 * The length is invalid.    */
        public static final int INVALID_LENGTH = 504;

        /**
		 * The string is invalid.    */
        public static final int INVALID_STRING = 506;

        /**
		 * The class loader is invalid.    */
        public static final int INVALID_CLASS_LOADER = 507;

        /**
		 * The array is invalid.    */
        public static final int INVALID_ARRAY = 508;

        /**
		 * Unable to load the transport.    */
        public static final int TRANSPORT_LOAD = 509;

        /**
		 * Unable to initialize the transport.    */
        public static final int TRANSPORT_INIT = 510;

        /**
		 *    */
        public static final int NATIVE_METHOD = 511;

        /**
		 * The count is invalid.   */
        public static final int INVALID_COUNT = 512;
    }

    public static class StepSize {

        /**
		 * Step by the minimum possible amount (often a bytecode instruction).    */
        public static final int MIN = 0;

        /**
		 * Step to the next source line unless there is no line number information in which case a MIN step is done instead.   */
        public static final int LINE = 1;
    }

    public static class SuspendPolicy {

        /**
		 * Suspend no threads when this event is encountered.    */
        public static final int NONE = 0;

        /**
		 * Suspend the event thread when this event is encountered.    */
        public static final int EVENT_THREAD = 1;

        /**
		 * Suspend all threads when this event is encountered.   */
        public static final int ALL = 2;
    }

    public static class EventKind {

        /**
		 * Never sent across JDWP    */
        public static final int VM_DISCONNECTED = 100;

        /**
		 *    */
        public static final int VM_START = JDWP.EventKind.VM_INIT;

        /**
		 *    */
        public static final int THREAD_DEATH = JDWP.EventKind.THREAD_END;

        /**
		 *    */
        public static final int SINGLE_STEP = 1;

        /**
		 *    */
        public static final int BREAKPOINT = 2;

        /**
		 *    */
        public static final int FRAME_POP = 3;

        /**
		 *    */
        public static final int EXCEPTION = 4;

        /**
		 *    */
        public static final int USER_DEFINED = 5;

        /**
		 *    */
        public static final int THREAD_START = 6;

        /**
		 *    */
        public static final int THREAD_END = 7;

        /**
		 *    */
        public static final int CLASS_PREPARE = 8;

        /**
		 *    */
        public static final int CLASS_UNLOAD = 9;

        /**
		 *    */
        public static final int CLASS_LOAD = 10;

        /**
		 *    */
        public static final int FIELD_ACCESS = 20;

        /**
		 *    */
        public static final int FIELD_MODIFICATION = 21;

        /**
		 *    */
        public static final int EXCEPTION_CATCH = 30;

        /**
		 *    */
        public static final int METHOD_ENTRY = 40;

        /**
		 *    */
        public static final int METHOD_EXIT = 41;

        /**
		 *    */
        public static final int VM_INIT = 90;

        /**
		 *   */
        public static final int VM_DEATH = 99;
    }

    public static class StepDepth {

        /**
		 * Step into any method calls that occur before the end of the step.    */
        public static final int INTO = 0;

        /**
		 * Step over any method calls that occur before the end of the step.    */
        public static final int OVER = 1;

        /**
		 * Step out of the current method.   */
        public static final int OUT = 2;
    }

    public static class SuspendStatus {

        /**
		 *   */
        public static final int SUSPEND_STATUS_SUSPENDED = 0x1;
    }

    public static class TypeTag {

        /**
		 * ReferenceType is a class.    */
        public static final int CLASS = 1;

        /**
		 * ReferenceType is an interface.    */
        public static final int INTERFACE = 2;

        /**
		 * ReferenceType is an array.   */
        public static final int ARRAY = 3;
    }

    public static class Handshake {

        public static final String STR = "JDWP-Handshake";

        private static Object monitor = new Object();

        private static int status = 0;

        public static void perform(Socket connection) throws IOException {
            final DataInputStream in = new DataInputStream(connection.getInputStream());
            final OutputStream out = connection.getOutputStream();
            final int io_error = 1;
            final int hk_error = 2;
            final int done = 4;
            Thread read = new Thread(new Runnable() {

                public void run() {
                    try {
                        synchronized (monitor) {
                            byte[] data = new byte[STR.getBytes().length];
                            in.readFully(data);
                            if (!new String(data).equals(STR)) {
                                status |= hk_error;
                            }
                            monitor.notifyAll();
                            status |= done;
                        }
                    } catch (IOException e) {
                        status |= io_error;
                        e.printStackTrace();
                    }
                }
            });
            Thread write = new Thread(new Runnable() {

                public void run() {
                    try {
                        synchronized (monitor) {
                            out.write(STR.getBytes());
                            out.flush();
                            monitor.notifyAll();
                            status |= done;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        status |= io_error;
                    }
                }
            });
            read.start();
            write.start();
            synchronized (monitor) {
                while ((status & done) == 1) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if ((status & hk_error) == hk_error) {
                    throw new IOException("JDWP-Handshake failed because of invalid string.");
                } else if ((status & io_error) == io_error) {
                    throw new IOException("JDWP-Handshake failed because of io errors.");
                }
            }
        }
    }
}
