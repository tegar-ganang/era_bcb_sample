package com.continuent.tungsten.commons.utils;

import java.lang.reflect.Method;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class SignalUtils {

    public static void main(String[] args) {
        try {
            DiagSignalHandler.install("SIGINT");
            Class<?> wrappedClass = Class.forName(args[0]);
            String wrappedArgs[] = new String[args.length - 1];
            for (int i = 0; i < wrappedArgs.length; i++) {
                wrappedArgs[i] = args[i + 1];
            }
            Class<?>[] argTypes = new Class[1];
            argTypes[0] = wrappedArgs.getClass();
            Method mainMethod = wrappedClass.getMethod("main", argTypes);
            Object[] argValues = new Object[1];
            argValues[0] = wrappedArgs;
            mainMethod.invoke(wrappedClass, argValues);
        } catch (Exception e) {
            System.out.println("AppWrap exception " + e);
        }
    }
}

class DiagSignalHandler implements SignalHandler {

    private SignalHandler oldHandler;

    public static DiagSignalHandler install(String signalName) {
        Signal diagSignal = new Signal(signalName);
        DiagSignalHandler diagHandler = new DiagSignalHandler();
        diagHandler.oldHandler = Signal.handle(diagSignal, diagHandler);
        return diagHandler;
    }

    public void handle(Signal sig) {
        System.out.println("Diagnostic Signal handler called for signal " + sig);
        if (true) return;
        try {
            Thread[] threadArray = new Thread[Thread.activeCount()];
            int numThreads = Thread.enumerate(threadArray);
            System.out.println("Current threads:");
            for (int i = 0; i < numThreads; i++) {
                System.out.println("    " + threadArray[i]);
            }
            if (oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
                oldHandler.handle(sig);
            }
        } catch (Exception e) {
            System.out.println("Signal handler failed, reason " + e);
        }
    }
}
