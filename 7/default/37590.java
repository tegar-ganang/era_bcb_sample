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
        VM_Callbacks.notifyStartup();
        VM_Controller.boot();
        String[] mainArgs = null;
        INSTRUCTION[] mainCode = null;
        synchronized (VM_ClassLoader.lock) {
            VM_Class cls = null;
            try {
                cls = VM_Class.forName(args[0]);
            } catch (VM_ResolutionException e) {
                VM.sysWrite(e.getException() + "\n");
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
        }
        VM.debugBreakpoint();
        VM_Magic.invokeMain(mainArgs, mainCode);
    }
}
