package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

/** 
 *   A GATE is an object with two states, open and closed. It is
 *   created with MAKE-GATE. Its state can be opened (OPEN-GATE) or
 *   closed (CLOSE-GATE) and can be explicitly tested with
 *   GATE-OPEN-P. Usually though, a thread awaits the opening of a
 *   gate by WAIT-OPEN-GATE.
 */
public final class Gate extends AbstractLispObject {

    private boolean open;

    Gate(boolean open) {
        this.open = open;
    }

    @Override
    public LispObject typeOf() {
        return SymbolConstants.GATE;
    }

    @Override
    public LispObject classOf() {
        return BuiltInClass.GATE;
    }

    @Override
    public String writeToString() {
        return unreadableString("GATE");
    }

    @Override
    public LispObject typep(LispObject typeSpecifier) throws ConditionThrowable {
        if (typeSpecifier == SymbolConstants.GATE) return T;
        if (typeSpecifier == BuiltInClass.GATE) return T;
        return super.typep(typeSpecifier);
    }

    public boolean isOpen() {
        return open;
    }

    public synchronized void close() {
        open = false;
    }

    public synchronized void open() {
        open = true;
        notifyAll();
    }

    public synchronized void waitForOpen(long timeout) throws InterruptedException {
        if (open) return;
        wait(timeout);
    }

    static final void checkForGate(LispObject arg) throws ConditionThrowable {
        if (arg instanceof Gate) return;
        type_error(arg, SymbolConstants.GATE);
    }

    private static final Primitive MAKE_GATE = new Primitive("make-gate", PACKAGE_EXT, true, "openp", "Creates a gate with initial state OPENP.") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            return new Gate(arg.getBooleanValue());
        }
    };

    private static final Primitive OPEN_GATE_P = new Primitive("open-gate-p", PACKAGE_EXT, true, "gate", "Boolean predicate as to whether GATE is open or not.") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            checkForGate(arg);
            return ((Gate) arg).isOpen() ? T : NIL;
        }
    };

    private static final Primitive OPEN_GATE = new Primitive("open-gate", PACKAGE_EXT, true, "gate", "Makes the state of GATE open.") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            checkForGate(arg);
            ((Gate) arg).open();
            return T;
        }
    };

    private static final Primitive CLOSE_GATE = new Primitive("close-gate", PACKAGE_EXT, true, "gate", "Makes the state of GATE closed.") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            checkForGate(arg);
            ((Gate) arg).close();
            return T;
        }
    };

    private static final Primitive WAIT_OPEN_GATE = new Primitive("wait-open-gate", PACKAGE_EXT, true, "gate &optional timeout", "Wait for GATE to be open with an optional TIMEOUT in ms.") {

        @Override
        public LispObject execute(LispObject gate) throws ConditionThrowable {
            return execute(gate, Fixnum.ZERO);
        }

        @Override
        public LispObject execute(LispObject gate, LispObject timeout) throws ConditionThrowable {
            checkForGate(gate);
            long msecs = LispThread.javaSleepInterval(timeout);
            try {
                ((Gate) gate).waitForOpen(msecs);
                return T;
            } catch (InterruptedException e) {
                return error(new LispError("The thread " + LispThread.currentThread().writeToString() + " was interrupted."));
            }
        }
    };
}
