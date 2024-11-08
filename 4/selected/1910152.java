package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public final class Mutex extends AbstractLispObject {

    private boolean inUse;

    @Override
    public LispObject typeOf() {
        return SymbolConstants.MUTEX;
    }

    @Override
    public LispObject classOf() {
        return BuiltInClass.MUTEX;
    }

    @Override
    public LispObject typep(LispObject typeSpecifier) throws ConditionThrowable {
        if (typeSpecifier == SymbolConstants.MUTEX) return T;
        if (typeSpecifier == BuiltInClass.MUTEX) return T;
        return super.typep(typeSpecifier);
    }

    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        synchronized (this) {
            try {
                while (inUse) wait();
                inUse = true;
            } catch (InterruptedException e) {
                notify();
                throw e;
            }
        }
    }

    public synchronized void release() {
        inUse = false;
        notify();
    }

    @Override
    public String writeToString() {
        return unreadableString("MUTEX");
    }

    static {
        try {
            PACKAGE_EXT.export(intern("WITH-MUTEX", PACKAGE_THREADS));
            PACKAGE_EXT.export(intern("WITH-THREAD-LOCK", PACKAGE_THREADS));
        } catch (ConditionThrowable t) {
            Debug.bug();
        }
    }

    private static final Primitive MAKE_MUTEX = new Primitive("make-mutex", PACKAGE_EXT, true, "") {

        @Override
        public LispObject execute() throws ConditionThrowable {
            return new Mutex();
        }
    };

    private static final Primitive GET_MUTEX = new Primitive("get-mutex", PACKAGE_EXT, true, "mutex") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof Mutex) try {
                ((Mutex) arg).acquire();
                return T;
            } catch (InterruptedException e) {
                return error(new LispError("The thread " + LispThread.currentThread().writeToString() + " was interrupted."));
            }
            return error(new TypeError("The value " + arg.writeToString() + " is not a mutex."));
        }
    };

    private static final Primitive RELEASE_MUTEX = new Primitive("release-mutex", PACKAGE_EXT, true, "mutex") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof Mutex) {
                ((Mutex) arg).release();
                return T;
            }
            return error(new TypeError("The value " + arg.writeToString() + " is not a mutex."));
        }
    };
}
