package util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A class used to be able to start different main-methods from within a JAR-file.
 * 
 * @author Sebastian Kuerten (sebastian.kuerten@fu-berlin.de)
 * 
 */
public class Launcher {

    /**
	 * @param args
	 *            the fully qualified path to the Class to launch plus the arguments that shall
	 *            be supplied to that program.
	 * @throws ClassNotFoundException
	 *             on error.
	 * @throws SecurityException
	 *             on error.
	 * @throws NoSuchMethodException
	 *             on error.
	 * @throws IllegalAccessException
	 *             on error.
	 * @throws IllegalArgumentException
	 *             on error.
	 * @throws InvocationTargetException
	 *             on error.
	 */
    public static void main(String args[]) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args.length < 1) {
            System.out.println("usage: Launcher <Classname> [<args>]");
            System.exit(1);
        }
        String className = args[0];
        int nargs = args.length - 1;
        String[] runArgs = new String[nargs];
        for (int i = 0; i < nargs; i++) {
            runArgs[i] = args[i + 1];
        }
        System.out.println(String.format("running %s with %d arguments", className, nargs));
        Class<?> program = ClassLoader.getSystemClassLoader().loadClass(className);
        Class<?>[] parameterTypes = new Class[] { args.getClass() };
        Method method = program.getMethod("main", parameterTypes);
        method.invoke(null, new Object[] { runArgs });
    }
}
