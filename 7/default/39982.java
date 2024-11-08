import java.net.URL;
import java.net.URLClassLoader;

/**
 * Thread in which user's "main" program runs.
 *
 * @author Bowen Alpern
 * @author Derek Lieber
 */
class MainThread extends Thread {

    private String[] args;

    private VM_Method mainMethod;

    /**
   * Create "main" thread.
   * Taken: args[0]    = name of class containing "main" method
   *        args[1..N] = parameters to pass to "main" method
   */
    MainThread(String args[]) {
        super(args);
        this.args = args;
    }

    public String toString() {
        return "MainThread";
    }

    VM_Method getMainMethod() {
        return mainMethod;
    }

    /**
   * Run "main" thread.
   */
    public void run() {
        VM_Controller.boot();
        VM_ApplicationClassLoader.setPathProperty();
        ClassLoader cl = new VM_ApplicationClassLoader(VM_SystemClassLoader.getVMClassLoader());
        String[] mainArgs = null;
        INSTRUCTION[] mainCode = null;
        VM_Class cls = null;
        try {
            cls = (VM_Class) cl.loadClass(args[0], true).getVMType();
        } catch (ClassNotFoundException e) {
            VM.sysWrite(e + "\n");
            return;
        }
        mainMethod = cls.findMainMethod();
        if (mainMethod == null) {
            VM.sysWrite(cls.getName() + " doesn't have a \"public static void main(String[])\" method to execute\n");
            return;
        }
        mainArgs = new String[args.length - 1];
        for (int i = 0, n = mainArgs.length; i < n; ++i) mainArgs[i] = args[i + 1];
        mainCode = mainMethod.compile();
        VM_Callbacks.notifyStartup();
        VM.debugBreakpoint();
        VM_Magic.invokeMain(mainArgs, mainCode);
    }
}
