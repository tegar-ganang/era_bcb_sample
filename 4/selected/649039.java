package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public class Profiler extends LispFile {

    static int sleep = 1;

    public static final Primitive _START_PROFILER = new Primitive("%start-profiler", PACKAGE_PROF, false) {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            final LispThread thread = LispThread.currentThread();
            Stream out = getStandardOutput();
            out.freshLine();
            if (profiling) {
                out._writeLine("; Profiler already started.");
            } else {
                if (first == Keyword.TIME) sampling = true; else if (first == Keyword.COUNT_ONLY) sampling = false; else return error(new LispError("%START-PROFILER: argument must be either :TIME or :COUNT-ONLY"));
                LispPackage[] packages = Packages.getAllPackages();
                for (int i = 0; i < packages.length; i++) {
                    LispPackage pkg = packages[i];
                    Symbol[] symbols = pkg.symbols();
                    for (int j = 0; j < symbols.length; j++) {
                        Symbol symbol = symbols[j];
                        LispObject object = symbol.getSymbolFunction();
                        if (object != null) {
                            object.setCallCount(0);
                            object.setHotCount(0);
                            if (object instanceof StandardGenericFunction) {
                                LispObject methods = PACKAGE_MOP.intern("GENERIC-FUNCTION-METHODS").execute(object);
                                while (methods != NIL) {
                                    StandardMethod method = (StandardMethod) methods.CAR();
                                    method.getFunction().setCallCount(0);
                                    method.getFunction().setHotCount(0);
                                    methods = methods.CDR();
                                }
                            }
                        }
                    }
                }
                if (sampling) {
                    sleep = second.intValue();
                    Runnable profilerRunnable = new Runnable() {

                        public void run() {
                            profiling = true;
                            while (profiling) {
                                try {
                                    thread.incrementCallCounts();
                                    Thread.sleep(sleep);
                                } catch (InterruptedException e) {
                                    Debug.trace(e);
                                } catch (ConditionThrowable e) {
                                    break;
                                }
                            }
                        }
                    };
                    Thread t = new Thread(profilerRunnable);
                    t.setPriority(Thread.MAX_PRIORITY);
                    new Thread(profilerRunnable).start();
                }
                out._writeLine("; Profiler started.");
            }
            return thread.nothing();
        }
    };

    public static final Primitive STOP_PROFILER = new Primitive("stop-profiler", PACKAGE_PROF, true) {

        @Override
        public LispObject execute() throws ConditionThrowable {
            Stream out = getStandardOutput();
            out.freshLine();
            if (profiling) {
                profiling = false;
                out._writeLine("; Profiler stopped.");
            } else out._writeLine("; Profiler was not started.");
            out._finishOutput();
            return LispThread.currentThread().nothing();
        }
    };
}
