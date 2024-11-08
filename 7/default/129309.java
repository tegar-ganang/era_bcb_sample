import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import sun.misc.*;

public class Launcher {

    public static void main(String args[]) {
        int num = args.length;
        String classname = args[0];
        String mainArgs[] = new String[num - 1];
        for (int i = 0; i < num - 2; i++) {
            mainArgs[i] = args[i + 1];
        }
        Launcher launcher = new Launcher();
        launcher.startApp(classname, mainArgs);
    }

    public void startApp(String mainClassName, String mainArgs[]) {
        try {
            File path = new File("./");
            sun.misc.CDCAppClassLoader loader = new CDCAppClassLoader(new URL[] { path.toURL() }, null);
            Class[] args1 = { new String[0].getClass() };
            Object[] args2 = { mainArgs };
            Class mainClass = loader.loadClass(mainClassName);
            Method mainMethod = mainClass.getMethod("main", args1);
            mainMethod.invoke(null, args2);
        } catch (InvocationTargetException i) {
            i.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
