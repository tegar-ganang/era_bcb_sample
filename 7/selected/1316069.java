package net.euler.project.problems.generic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

public final class TimedRun {

    /**
	 * @param args
	 */
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (args.length < 1) throw new RuntimeException("Usage: java " + TimedRun.class.getCanonicalName() + " ClassName [args]");
        final ClassLoader cl = TimedRun.class.getClassLoader();
        final Class<?> target = cl.loadClass(args[0]);
        final Method runMe = target.getMethod("main", String[].class);
        final String[] otherArgs = new String[args.length - 1];
        for (int i = 0; i < otherArgs.length; i++) otherArgs[i] = args[i + 1];
        final long now = Calendar.getInstance().getTimeInMillis();
        runMe.invoke(null, new Object[] { otherArgs });
        final long delta = Calendar.getInstance().getTimeInMillis() - now;
        System.out.println("Run time : " + formatTime(delta));
    }

    private static final String formatTime(long millis) {
        short ms = (short) (millis % 1000);
        short s = (short) (((millis - ms) / 1000) % 60);
        short m = (short) (((millis - ms - (1000 * s)) / 60000) % 60);
        short h = (short) ((millis - ms - (1000 * s) - (60000 * m)) / 3600000);
        return shortToString(h, (short) 2) + ":" + shortToString(m, (short) 2) + "'" + shortToString(s, (short) 2) + "\"" + shortToString(ms, (short) 3);
    }

    private static final String shortToString(final short number, final short size) {
        String ret = String.valueOf(number);
        while (ret.length() < size) ret = "0" + ret;
        return ret;
    }
}
