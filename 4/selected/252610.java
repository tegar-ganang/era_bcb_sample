package lejos.nxt.debug;

import lejos.nxt.VM;

class EventRequest {

    /** Suspend no threads when the event occurs */
    static final int SUSPEND_NONE = 0;

    /** Suspend only the thread which generated the event when the event occurs */
    static final int SUSPEND_EVENT_THREAD = 1;

    /** Suspend all threads when the event occurs */
    static final int SUSPEND_ALL = 2;

    static final int STEP_DEPTH_INTO = 0;

    static final int STEP_DEPTH_OVER = 1;

    static final int STEP_DEPTH_OUT = 2;

    static final int STEP_SIZE_MIN = 0;

    static final int STEP_SIZE_LINE = 1;

    private static final EventRequest queueHead = new EventRequest();

    static void clearAll() {
        synchronized (queueHead) {
            queueHead.next = queueHead.previous = queueHead;
        }
    }

    static void clearAllBreakpoints() {
        synchronized (queueHead) {
            for (EventRequest req = queueHead.next; req != queueHead; req = req.next) {
                System.out.println("Process request " + req);
                if (req.eventKind == JDWPConstants.EVENT_BREAKPOINT) req.clear();
            }
        }
    }

    /**
	 * 
	 * @param monitor
	 *            The debug monitor containing the event
	 * @param dst
	 *            The packet stream to write to
	 * @return the suspend policy
	 */
    static int processEvent(DebugInterface monitor, PacketStream dst) {
        Thread thread = monitor.thread;
        int sp = 0;
        int cnt = 0;
        dst.writeByte(0);
        dst.writeInt(0);
        System.out.println("Event " + monitor.typ);
        if (monitor.typ == DebugInterface.DBG_PROGRAM_EXIT) {
            cnt++;
            dst.writeByte(JDWPConstants.EVENT_VM_DEATH);
            dst.writeInt(0);
        } else if (monitor.typ == JDWPDebugServer.DBG_PROGRAM_START) {
            cnt++;
            dst.writeByte(JDWPConstants.EVENT_VM_INIT);
            dst.writeInt(0);
            dst.writeObjectId(JDWPDebugServer.getObjectAddress(thread));
            sp = SUSPEND_ALL;
        }
        synchronized (queueHead) {
            for (EventRequest req = queueHead.next; req != queueHead; req = req.next) {
                if (req.process(monitor, dst)) {
                    cnt++;
                    if (req.suspendPolicy > sp) {
                        sp = req.suspendPolicy;
                    }
                }
            }
        }
        monitor.clearEvent();
        if (cnt != 0) {
            byte[] hdr = new byte[5];
            hdr[0] = (byte) sp;
            hdr[1] = (byte) ((cnt >>> 24) & 0xff);
            hdr[2] = (byte) ((cnt >>> 16) & 0xff);
            hdr[3] = (byte) ((cnt >>> 8) & 0xff);
            hdr[4] = (byte) ((cnt >>> 0) & 0xff);
            dst.send(hdr);
            System.out.println("Event sent");
        }
        if (sp < 2) {
            monitor.resumeProgram();
        }
        if (sp == 1) {
            VM.suspendThread(thread);
        }
        return sp;
    }

    private EventRequest next, previous;

    byte eventKind;

    int nxtEventKind;

    byte suspendPolicy;

    int countFilter, currentCount;

    Thread threadFilter;

    private int methodFilter = -1;

    private int pcFilter = -1;

    private Class<?> exceptionFilter;

    private int exceptionFlags;

    private boolean setBreakpoint;

    private int stepSize = -1;

    private int stepDepth = -1;

    private int[] stepInfo;

    EventRequest() {
        next = previous = this;
    }

    private boolean process(DebugInterface monitor, PacketStream dst) {
        if (monitor.typ != nxtEventKind) return false;
        if (countFilter == currentCount) {
            currentCount = 0;
        } else if (countFilter > 0) {
            currentCount++;
            return false;
        }
        if (threadFilter != null && monitor.thread != threadFilter) return false;
        if (nxtEventKind == DebugInterface.DBG_BREAKPOINT && methodFilter != -1 && (methodFilter != monitor.method || pcFilter != monitor.pc)) return false;
        if (nxtEventKind == DebugInterface.DBG_EXCEPTION) {
            if (exceptionFilter != null && !exceptionFilter.isInstance(monitor.exception)) return false;
            if (monitor.method2 >= 0 ? (exceptionFlags & 1) == 0 : (exceptionFlags & 2) == 0) return false;
        }
        dst.writeByte(eventKind);
        dst.writeInt(JDWPDebugServer.getObjectAddress(this));
        switch(eventKind) {
            case JDWPConstants.EVENT_VM_INIT:
            case JDWPConstants.EVENT_THREAD_DEATH:
            case JDWPConstants.EVENT_THREAD_START:
                dst.writeObjectId(JDWPDebugServer.getObjectAddress(monitor.thread));
                break;
            case JDWPConstants.EVENT_SINGLE_STEP:
                System.out.println("Write step event");
            case JDWPConstants.EVENT_BREAKPOINT:
                dst.writeObjectId(JDWPDebugServer.getObjectAddress(monitor.thread));
                JDWPDebugServer.writeLocation(dst, VM.getVM().getMethod(monitor.method), monitor.pc);
                break;
            case JDWPConstants.EVENT_EXCEPTION:
                dst.writeObjectId(JDWPDebugServer.getObjectAddress(monitor.thread));
                JDWPDebugServer.writeLocation(dst, VM.getVM().getMethod(monitor.method), monitor.pc);
                dst.writeByte(JDWPConstants.OBJECT_TAG);
                dst.writeObjectId(JDWPDebugServer.getObjectAddress(monitor.exception));
                if (monitor.method2 >= 0) JDWPDebugServer.writeLocation(dst, VM.getVM().getMethod(monitor.method2), monitor.pc2); else JDWPDebugServer.writeNoLocation(dst);
                break;
        }
        if (eventKind == JDWPConstants.EVENT_SINGLE_STEP) {
            requestStepInformation(JDWPDebugServer.instance);
        }
        return true;
    }

    short create(PacketStream in) {
        eventKind = in.readByte();
        switch(eventKind) {
            case JDWPConstants.EVENT_VM_DEATH:
                nxtEventKind = DebugInterface.DBG_PROGRAM_EXIT;
                break;
            case JDWPConstants.EVENT_VM_INIT:
                nxtEventKind = JDWPDebugServer.DBG_PROGRAM_START;
                break;
            case JDWPConstants.EVENT_THREAD_START:
                nxtEventKind = DebugInterface.DBG_THREAD_START;
                break;
            case JDWPConstants.EVENT_THREAD_DEATH:
                nxtEventKind = DebugInterface.DBG_THREAD_STOP;
                break;
            case JDWPConstants.EVENT_BREAKPOINT:
                nxtEventKind = DebugInterface.DBG_BREAKPOINT;
                setBreakpoint = true;
                break;
            case JDWPConstants.EVENT_EXCEPTION_CATCH:
            case JDWPConstants.EVENT_EXCEPTION:
                nxtEventKind = DebugInterface.DBG_EXCEPTION;
                break;
            case JDWPConstants.EVENT_SINGLE_STEP:
                nxtEventKind = DebugInterface.DBG_SINGLE_STEP;
                break;
            case JDWPConstants.EVENT_CLASS_LOAD:
            case JDWPConstants.EVENT_CLASS_PREPARE:
            case JDWPConstants.EVENT_CLASS_UNLOAD:
                return 0;
            default:
                return JDWPConstants.INVALID_EVENT_TYPE;
        }
        suspendPolicy = in.readByte();
        int modCount = in.readInt();
        for (int i = 0; i < modCount; i++) {
            int kind = in.readUnsignedByte();
            switch(kind) {
                case 1:
                    countFilter = in.readInt();
                    break;
                case 3:
                    {
                        int threadId = in.readObjectId();
                        Object obj = JDWPDebugServer.memGetReference(0, threadId);
                        if (obj instanceof Thread) {
                            threadFilter = (Thread) obj;
                        } else {
                            return JDWPConstants.INVALID_THREAD;
                        }
                        if (DebugInterface.get().isSystemThread(threadFilter)) {
                            return JDWPConstants.INVALID_THREAD;
                        }
                        if (nxtEventKind == DebugInterface.DBG_SINGLE_STEP && JDWPDebugServer.isStepping(threadFilter)) {
                            return JDWPConstants.DUPLICATE;
                        }
                    }
                    break;
                case 7:
                    in.readByte();
                    in.readClassId();
                    methodFilter = in.readMethodId();
                    pcFilter = (int) in.readLong();
                    System.out.println(methodFilter);
                    System.out.println(pcFilter);
                    break;
                case 8:
                    {
                        int classId = in.readClassId();
                        if (classId != 0) {
                            exceptionFilter = VM.getClass(classId);
                        }
                        exceptionFlags = 0;
                        if (in.readBoolean()) exceptionFlags |= 1;
                        if (in.readBoolean()) exceptionFlags |= 2;
                    }
                    break;
                case 10:
                    int threadId = in.readObjectId();
                    Object obj = JDWPDebugServer.memGetReference(0, threadId);
                    if (obj instanceof Thread) {
                        threadFilter = (Thread) obj;
                    } else {
                        return JDWPConstants.INVALID_THREAD;
                    }
                    if (DebugInterface.get().isSystemThread(threadFilter)) {
                        return JDWPConstants.INVALID_THREAD;
                    }
                    stepSize = in.readInt();
                    stepDepth = in.readInt();
                    if (stepDepth == STEP_DEPTH_OUT) stepSize = STEP_SIZE_MIN;
                    break;
            }
        }
        if (setBreakpoint) Breakpoint.addBreakpoint(methodFilter, pcFilter);
        synchronized (queueHead) {
            next = queueHead.next;
            next.previous = this;
            queueHead.next = this;
            previous = queueHead;
        }
        return 0;
    }

    void requestStepInformation(JDWPDebugServer listener) {
        if (nxtEventKind == DebugInterface.DBG_SINGLE_STEP) {
            VM.VMThread thread = VM.getVM().getVMThread(threadFilter);
            VM.VMStackFrame frame = thread.getStackFrames().get(0);
            VM.VMMethod method = frame.getVMMethod();
            int codeOffset = (method.codeOffset & 0xFFFF) + VM.getVM().getImage().address;
            int bppc = frame.pc - codeOffset;
            if (stepSize == STEP_SIZE_LINE) {
                PacketStream ps = new PacketStream(listener, JDWPConstants.CSET_NXT, JDWPConstants.NXT_STEP_LINE_INFO);
                ps.writeMethodId(method.getMethodNumber());
                ps.writeShort(bppc);
                ps.send();
                ps.waitForReply();
                int nPCs = ps.readInt();
                if (nPCs == 0) {
                    stepSize = STEP_SIZE_MIN;
                    return;
                }
                stepInfo = new int[nPCs];
                for (int i = 0; i < nPCs; i++) {
                    int lpc = ps.readUnsignedShort();
                    stepInfo[i] = lpc;
                }
            }
            if (stepSize == STEP_SIZE_MIN) {
                stepInfo = new int[] { bppc };
            }
            JDWPDebugServer.setThreadRequest(threadFilter, new SteppingRequest(stepDepth, method.getMethodNumber(), stepInfo));
        }
    }

    void clear() {
        if (nxtEventKind == DebugInterface.DBG_SINGLE_STEP) {
            JDWPDebugServer.setThreadRequest(threadFilter, null);
        }
        if (next != null && previous != null) {
            synchronized (queueHead) {
                next.previous = previous;
                previous.next = next;
            }
        }
    }
}
