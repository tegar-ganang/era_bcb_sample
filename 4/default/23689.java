import java.io.*;

abstract class OsProcess implements jdpConstants, VM_BaselineConstants {

    /**
   * Abstract method:  check if the system trap is for breakpoint
   * @param status the process status from the system
   * @return true if trapping on breakpoint
   * @exception
   * @see
   */
    public abstract boolean isBreakpointTrap(int status);

    /**
   * Abstract method:  check if the system trap is to be ignored
   * and passed directly to the RVM
   * @param status the process status from the system
   * @return true if trap is to be ignored
   * @exception
   * @see
   */
    public abstract boolean isIgnoredTrap(int status);

    public abstract boolean isIgnoredOtherBreakpointTrap(int status);

    public abstract boolean isLibraryLoadedTrap(int status);

    /**
   * Abstract method:  check if the process has been killed
   * @param status the process status from the system
   * @return true if the process has been killed
   * @exception
   * @see mwait
   */
    public abstract boolean isKilled(int status);

    /**
   * Abstract method:  check if the process has exited normally
   * @param status the process status from the system
   * @return true if the process has exited
   * @exception
   * @see
   */
    public abstract boolean isExit(int status);

    /**
   * Abstract method:  convert to String the process status indicator used by
   * the system
   * @param status the process status from the system
   * @return the status message
   * @exception
   * @see
   */
    public abstract String statusMessage(int status);

    /**
   * Abstract method: destroy the process
   * @param
   * @return
   * @exception
   * @see
   */
    public abstract void mkill();

    /**
   * Abstract method: detach the debugger from the process
   * @param
   * @return
   * @exception
   * @see
   */
    public abstract void mdetach();

    /**
   * Temporary hardware single step for Intel
   */
    public abstract boolean mstep();

    /**
   * Abstract method: wait for the process to return
   * @param
   * @return the process status from the system
   * @exception
   * @see
   */
    public abstract int mwait();

    /**
   * Abstract method: allow all threads in the process to continue execution
   * @param
   * @return
   * @exception
   * @see
   */
    public abstract void mcontinue(int signal);

    /**
   * Abstract method:  create the external process to execute a program
   * @param ProgramName the name of the main program as would be used in a shell
   * @param args the argument list for the program
   * @return the process id from the system
   * @exception
   * @see
   */
    public abstract int createDebugProcess(String ProgramName, String args[]);

    /**
   * Abstract method:  attach to an external process currently running
   * @param processID the system process ID
   * @param args the argument list for the program
   * @return the same process id from the system
   * @exception
   * @see
   */
    public abstract int attachDebugProcess(int processID);

    /**
   * Abstract method:  get the system thread ID
   * @return an array for the current system thread ID
   * @exception
   * @see register.getSystemThreadGPR
   */
    public abstract int[] getSystemThreadId();

    /**
   * Abstract method:  dump the system thread ID
   * @return a String array for the contents of all system threads
   * @exception
   * @see 
   */
    public abstract String listSystemThreads();

    /**
   * Abstract method:  get the index of the VM_Thread currently loaded in the system threads
   * @return an array of VM_Thread
   * @exception
   * @see
   */
    public abstract int[] getVMThreadIDInSystemThread();

    /**
   * to access the process memory
   */
    memory mem;

    /**
   * access for the process registers
   */
    register reg;

    /**
   * map from mem address to boot image bytecode, methods, ...
   */
    BootMap bmap;

    /**
   * global breakpoints
   */
    breakpointList bpset;

    /** 
   * breakpoint for stepping each thread
   */
    breakpointList threadstep;

    /** 
   * breakpoint for stepping each thread by source line
   */
    breakpointList threadstepLine;

    /**
   * Ignore any breakpoint/trap signal not caused by the special trap instruction
   * used by jdp:  jdpConstants.BKPT_INSTRUCTION
   * This is used in starting up the JVM: stack resizing may occur which is normal
   * which also use trap instruction; jdp needs to set a dummy breakpoint to get
   * the main Method so we need to ignore other trap
   */
    boolean ignoreOtherBreakpointTrap = false;

    /**
   * Flag to print only the current instruction or the full stack when stepping
   */
    boolean verbose;

    /**
   * one for each thread, hold the signal pending in the debugger
   */
    int lastSignal[];

    /**
   * Constructor for the external version
   * Create the Java data structure to keep track of the OS process
   * @param ProgramName the name of the main program as would be used in a shell
   * @param args the argument list for the program
   *        use C args convention: arg[0] is the program name, arg[1-n] are the arguments
   * @param mainClass
   * @param classesNeededFilename the list of classes loaded in the boot image
   *        This is to circumvent the problems with class loading and compiling:
   *         (1) the boot image writer uses a different loader than the JDK, and
   *         (2) the compiler generates different code depending on the current
   *        environment.  (Line number will be wrong without this)
   * @param classpath  the java class path to ensure the classes are loaded in the
   *        same order as the boot image.  This causes the TOC to be filled up
   *        in the same order so that the offset to fields are identical between
   *        the TOC in the boot image and the TOC created in the JDK.
   * @return
   * @exception
   * @see
   */
    public OsProcess(String ProgramName, String args[], String mainClass, String classesNeededFilename, String classpath) {
        createDebugProcess(ProgramName, args);
        lastSignal = new int[1];
        lastSignal[0] = 0;
        if (args.length > 1) {
            bmap = loadMap(mainClass, classesNeededFilename, classpath);
        }
        verbose = false;
    }

    /**
   * Load the method map for the boot image
   * This is hard-coded, probably not good for the general case
   * we are expecting a C program that would load the JVM boot image
   * so the first argument arg[1] is assumed to be the name of the JVM boot image
   */
    private BootMap loadMap(String mainClass, String classesNeededFilename, String classpath) {
        try {
            return new BootMapExternal(this, mainClass, classesNeededFilename, classpath);
        } catch (BootMapCorruptException bme) {
            System.out.println(bme);
            bme.printStackTrace();
            System.out.println("ERROR: corrupted method map");
            return new BootMapExternal(this);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.out.println("ERROR: something wrong with method map :) ");
            return new BootMapExternal(this);
        }
    }

    /**
   * Constructor for the external version
   * This version attaches to a currently running process instead of creating one
   * @param processID the system process ID of the running process
   * @param classesNeededFilename the list of classes loaded in the boot image
   *        This is to circumvent the problems with class loading and compiling:
   *         (1) the boot image writer uses a different loader than the JDK, and
   *         (2) the compiler generates different code depending on the current
   *        environment.  (Line number will be wrong without this)
   * @param classpath  the java class path to ensure the classes are loaded in the
   *        same order as the boot image.  This causes the TOC to be filled up
   *        in the same order so that the offset to fields are identical between
   *        the TOC in the boot image and the TOC created in the JDK.
   * @return
   * @exception
   * @see
   */
    public OsProcess(int processID, String mainClass, String classesNeededFilename, String classpath) throws OsProcessException {
        int pid = attachDebugProcess(processID);
        if (pid == -1) throw new OsProcessException("Fail to attach to process");
        lastSignal = new int[1];
        lastSignal[0] = 0;
        bmap = loadMap(mainClass, classesNeededFilename, classpath);
        verbose = false;
    }

    /**
   * Stepping by machine instruction, following branch
   * if there is a user breakpoint at the current address, take care to remove it,
   * step over this address, then put it back
   * @param thread the thread to control
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @param skip_prolog
   * @return  true if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pstep(int thread, int printMode, boolean skip_prolog) {
        boolean stillrunning;
        int addr = reg.hardwareIP();
        boolean over_branch = false;
        breakpoint bpSaved = bpset.lookup(addr);
        threadstep.setStepBreakpoint(thread, over_branch, skip_prolog);
        if (bpSaved != null) bpset.clearBreakpoint(bpSaved);
        stillrunning = continueCheckingForSignal(thread, printMode, false);
        threadstep.clearStepBreakpoint(thread);
        if (stillrunning && bpSaved != null) bpset.setBreakpoint(bpSaved);
        printCurrentStatus(printMode);
        return stillrunning;
    }

    /**
   * Step by source line, over all method calls in current line:
   * To do this correctly, we need to:
   *   -keep track of thread ID in case other threads run the same code
   *   -get the breakpoint for the next valid line
   *   -
   * @param thread the thread to control (currently unused, default current thread)
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @return true if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pstepLine(int thread, int printMode) {
        boolean stillrunning = true;
        boolean lookingForNextLine = true;
        breakpoint bpSaved = bpset.lookup(reg.hardwareIP());
        int orig_thread = reg.hardwareTP();
        int curr_thread = -1;
        boolean success = threadstepLine.setStepLineBreakpoint(thread);
        if (!success) {
            return stillrunning;
        }
        if (bpSaved != null) bpset.clearBreakpoint(bpSaved);
        while (lookingForNextLine) {
            stillrunning = continueCheckingForSignal(thread, printMode, false);
            if (stillrunning) {
                int curr_addr = reg.hardwareIP();
                curr_thread = reg.hardwareTP();
                if (threadstep.isAtStepLineBreakpoint(thread, curr_addr) && (curr_thread != orig_thread)) {
                    threadstep.setStepBreakpoint(thread, false, false);
                    threadstepLine.temporaryClearStepBreakpoint(thread);
                    stillrunning = continueCheckingForSignal(thread, printMode, false);
                    threadstep.clearStepBreakpoint(thread);
                    threadstepLine.restoreStepBreakpoint(thread);
                } else {
                    lookingForNextLine = false;
                }
            } else {
                return stillrunning;
            }
        }
        if (stillrunning) {
            threadstepLine.clearStepBreakpoint(thread);
            if (bpSaved != null) bpset.setBreakpoint(bpSaved);
        }
        return stillrunning;
    }

    /**
   * Stepping by machine instruction, skipping over branch
   * if there is a user breakpoint at the current address, take care to remove it,
   * step over this address, then put it back
   * @param thread the thread to control
   * @return  true if if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pstepOverBranch(int thread) {
        int addr = reg.hardwareIP();
        boolean over_branch = true;
        boolean stillrunning;
        boolean skip_prolog = false;
        breakpoint bpSaved = bpset.lookup(addr);
        threadstep.setStepBreakpoint(thread, over_branch, skip_prolog);
        if (bpSaved != null) bpset.clearBreakpoint(bpSaved);
        stillrunning = continueCheckingForSignal(thread, PRINTASSEMBLY, false);
        threadstep.clearStepBreakpoint(thread);
        if (stillrunning && bpSaved != null) bpset.setBreakpoint(bpSaved);
        return stillrunning;
    }

    /**
   * Common code for pstep, pstepOverBranch, pcontinue
   * @param thread the thread to control
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @return  true if if the process is still running, false if it has exited
   * @exception
   * @see
   */
    private boolean continueCheckingForSignal(int thread, int printMode, boolean allThreads) {
        if (lastSignal[thread] == 0) mcontinue(0); else {
            int sig = lastSignal[thread];
            lastSignal[thread] = 0;
            mcontinue(sig);
        }
        try {
            pwait(allThreads);
        } catch (OsProcessException e) {
            lastSignal[thread] = e.status;
            if (isExit(e.status)) {
                System.out.println("Program has exited.");
                return false;
            } else {
                System.out.println("Unexpected signal: " + statusMessage(e.status));
                System.out.println("...type cont/step to continue with this signal");
            }
        }
        return true;
    }

    /**
   * Print the current position in the code (source or machine)
   * This is for the hardware state, NOT for the context thread
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @return
   * @exception
   * @see
   */
    private void printCurrentStatus(int printMode) {
        switch(printMode) {
            case PRINTNONE:
                break;
            case PRINTASSEMBLY:
                if (verbose) {
                    try {
                        mem.printJVMstack(reg.currentFP(), 4);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                System.out.println(mem.printCurrentInstr());
                break;
            case PRINTSOURCE:
                bmap.printCurrentSourceLine();
                break;
        }
    }

    /**
   *
   * Step by source line, over method call:
   *    go one step forward, then if we are in a different stack frame,
   *    continue till returning to the original stack frame
   * @param thread  the thread to step
   * @return true if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pstepLineOverMethod(int thread) {
        boolean stillrunning = true;
        boolean skip_prolog;
        int addr, orig_frame, curr_frame;
        String orig_line;
        String curr_line;
        orig_frame = mem.findFrameCount();
        stillrunning = pstepLine(thread, PRINTNONE);
        if (!stillrunning) return stillrunning;
        curr_frame = mem.findFrameCount();
        if (orig_frame < curr_frame) {
            stillrunning = pcontinueToReturn(thread, PRINTNONE);
        } else {
            stillrunning = true;
        }
        if (stillrunning) printCurrentStatus(PRINTSOURCE);
        return stillrunning;
    }

    /**
   * Wait until the debuggee stops, check the cause of stopping:
   * <ul>
   * <li> if it's our own breakpoint, return
   * <li> if it's a breakpoint to be ignored by the debugger and 
   *      serviced by the JVM, quietly pass it on
   * <li> if it's something unexpected, raise an exception
   * </ul>
   * @param allThread flag to run all thread or only the current thread
   * @return
   * @exception OsProcessException if the system signal does not belong to the debugger
   * @see
   */
    public void pwait(boolean allThread) throws OsProcessException {
        int addr, inst;
        boolean skipBP = false;
        int status = mwait();
        while (isIgnoredTrap(status) || (skipBP = isLibraryLoadedTrap(status)) || isIgnoredOtherBreakpointTrap(status)) {
            if (skipBP) status = 0;
            mcontinue(status);
            status = mwait();
        }
        if (isBreakpointTrap(status)) {
            bpset.relocateBreakpoint();
            addr = reg.hardwareIP();
            if (bpset.doesBreakpointExist(addr) || threadstep.doesBreakpointExist(addr) || threadstepLine.doesBreakpointExist(addr)) {
                return;
            } else {
                throw new OsProcessException(status);
            }
        } else {
            if (!ignoreOtherBreakpointTrap) throw new OsProcessException(status);
        }
    }

    /**
   * Continue all thread execution
   * <p> To continue and still keep the user breakpoint at this address if any:
   * <ul>
   * <li> clear the user breakpoint but remember it
   * <li> step forward one instruction
   * <li> put this breakpoint back before continue on
   * </ul>
   * <p> Clear all the link breakpoints
   * If in source debugging mode, print the current source line
   * otherwise print the current assembly instruction
   * @param thread the thread to control
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @param allThreads flag set to true if all threads are to continue,
   *                   false if only current thread is to continue
   * @return true if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pcontinue(int thread, int printMode, boolean allThreads) {
        int addr = reg.hardwareIP();
        boolean over_branch = false;
        boolean stillrunning;
        boolean skip_prolog = false;
        breakpoint bpSaved = bpset.lookup(addr);
        if (bpSaved != null) {
            System.out.println("pcontinue: saving current breakpoint " + VM.intAsHexString(addr));
            threadstep.setStepBreakpoint(thread, over_branch, skip_prolog);
            bpset.clearBreakpoint(bpSaved);
            stillrunning = continueCheckingForSignal(thread, PRINTNONE, false);
            threadstep.clearStepBreakpoint(thread);
            bpset.setBreakpoint(bpSaved);
        }
        stillrunning = continueCheckingForSignal(thread, printMode, allThreads);
        return stillrunning;
    }

    /**
   * Continue to the return address from the last branch and link
   * (unless stopped by another breakpoint in between)
   * @param thread the thread to control
   * @param printMode flag to display the current java source line or
   *                  machine instruction
   * @return true if the process is still running, false if it has exited
   * @exception
   * @see
   */
    public boolean pcontinueToReturn(int thread, int printMode) {
        int addr = reg.hardwareIP();
        boolean stillrunning;
        breakpoint bpSaved = bpset.lookup(addr);
        threadstep.setLinkBreakpoint(thread);
        bpset.clearBreakpoint(bpSaved);
        stillrunning = continueCheckingForSignal(thread, printMode, false);
        threadstep.clearStepBreakpoint(thread);
        if (stillrunning && bpSaved != null) bpset.setBreakpoint(bpSaved);
        return stillrunning;
    }

    /**
   * Terminate the current debuggee
   * @param
   * @return
   * @exception
   * @see
   */
    public void pkill() {
        mkill();
        try {
            pwait(true);
        } catch (OsProcessException e) {
            if (isExit(e.status)) System.out.println("Program has exited."); else if (isKilled(e.status)) System.out.println("Program terminated by user.");
        }
    }

    /**
   * Query map
   * @param
   * @return  BootMap of this process
   * @exception
   * @see
   */
    public BootMap bootmap() {
        return bmap;
    }

    private String getJ2NThreadCounts(int threadPointer) throws Exception {
        String result = new String();
        String temp;
        String blanks = "            ";
        if (threadPointer == 0) return null;
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NYieldCount");
            temp = Integer.toString(mem.read(threadPointer + field.getOffset()));
            result += blanks.substring(1, blanks.length() - temp.length()) + temp;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NLockFailureCount");
            temp = Integer.toString(mem.read(threadPointer + field.getOffset()));
            result += blanks.substring(1, blanks.length() - temp.length()) + temp;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NTotalYieldDuration");
            temp = Integer.toString(mem.read(threadPointer + field.getOffset()));
            result += blanks.substring(1, blanks.length() - temp.length()) + temp;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NTotalLockDuration");
            temp = Integer.toString(mem.read(threadPointer + field.getOffset()));
            result += blanks.substring(1, blanks.length() - temp.length()) + temp;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        return result;
    }

    private void zeroJ2NThreadCounts(int threadPointer) throws Exception {
        if (threadPointer == 0) return;
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NYieldCount");
            mem.write(threadPointer + field.getOffset(), 0);
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NLockFailureCount");
            mem.write(threadPointer + field.getOffset(), 0);
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NTotalYieldDuration");
            mem.write(threadPointer + field.getOffset(), 0);
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "J2NTotalLockDuration");
            mem.write(threadPointer + field.getOffset(), 0);
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
    }

    /**
   * Search VM_Scheduler.threads and return all nonzero VM_Thread pointers
   * as an array of VM_Threads pointers
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public int[] getAllThreads() throws Exception {
        int count = 0;
        int threadPointer;
        try {
            VM_Field field = bmap.findVMField("VM_Scheduler", "threads");
            int address = mem.readTOC(field.getOffset());
            int arraySize = mem.read(address + VM.ARRAY_LENGTH_OFFSET);
            VM_Field numThreadsField = bmap.findVMField("VM_Scheduler", "numActiveThreads");
            int numThreadsAddress = mem.addressTOC(numThreadsField.getOffset());
            int numThreads = mem.readsafe(numThreadsAddress) + 1;
            int threadArray[] = new int[numThreads];
            int j = 0;
            for (int i = 1; i < arraySize; i++) {
                threadPointer = mem.read(address + i * 4);
                if (threadPointer != 0) {
                    threadArray[j++] = threadPointer;
                    if (j == numThreads) break;
                }
            }
            return threadArray;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Scheduler.threads, has VM_Scheduler.java been changed?");
        }
    }

    public int getVMThreadForIndex(int index) throws Exception {
        if (index == 0) return 0;
        try {
            VM_Field field = bmap.findVMField("VM_Scheduler", "threads");
            int address = mem.readTOC(field.getOffset());
            int arraySize = mem.read(address + VM.ARRAY_LENGTH_OFFSET);
            return mem.read(address + index * 4);
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Scheduler.threads, has VM_Scheduler.java been changed?");
        }
    }

    public int getIndexForVMThread(int threadPointer) throws Exception {
        if (threadPointer == 0) return 0;
        try {
            VM_Field field = bmap.findVMField("VM_Thread", "threadSlot");
            int address = mem.read(threadPointer + field.getOffset());
            return address;
        } catch (BmapNotFoundException e) {
            throw new Exception("cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?");
        }
    }

    public boolean isValidVMThread(int threadPointer) throws Exception {
        int vmthreads[] = getAllThreads();
        for (int i = 0; i < vmthreads.length; i++) {
            if (threadPointer == vmthreads[i]) {
                return true;
            }
        }
        return false;
    }

    /**
   * 
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public String listAllThreads(boolean byClassName) {
        try {
            String result = "";
            int allThreads[] = getAllThreads();
            result = "All threads: " + allThreads.length + "\n";
            if (byClassName) {
                result += "  ID  VM_Thread   Thread Class\n";
                result += "  -- -----------  ------------\n";
            } else {
                result += "  ID  VM_Thread   top stack frame\n";
                result += "  -- -----------  -----------------\n";
            }
            int activeThread = reg.hardwareTP();
            for (int i = 0; i < allThreads.length; i++) {
                if (allThreads[i] == activeThread) result += " >"; else result += "  ";
                if (byClassName) {
                    VM_Field field = bmap.findVMField("VM_Thread", "threadSlot");
                    int fieldOffset = field.getOffset();
                    int id = mem.read(allThreads[i] + fieldOffset);
                    result += id + " @" + VM.intAsHexString(allThreads[i]) + "  ";
                    result += bmap.addressToClassString(allThreads[i]).substring(1) + "\n";
                } else {
                    result += threadToString(allThreads[i]) + "\n";
                }
            }
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public String listThreadsCounts() {
        try {
            String result = "";
            int allThreads[] = getAllThreads();
            result = "All threads: " + allThreads.length + "\n";
            result += "  ID VM_Thread  yield  failure   total yield   total lck \n";
            result += "  ------------  -----  -------   -----------   --------- \n";
            for (int i = 0; i < allThreads.length; i++) {
                result += "  " + Integer.toHexString(allThreads[i]) + "  ";
                result += getJ2NThreadCounts(allThreads[i]) + "\n";
            }
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void zeroThreadsCounts() {
        try {
            String result = "";
            int allThreads[] = getAllThreads();
            for (int i = 0; i < allThreads.length; i++) {
                zeroJ2NThreadCounts(allThreads[i]);
            }
        } catch (Exception e) {
            System.out.println("zeroThreadsCounts: " + e.getMessage());
        }
    }

    /**
   * Return a string for all the threads in the VM_Scheduler.processors[n].readyQueue
   * for each virtual processor
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public String listReadyThreads() {
        String result = "";
        try {
            VM_Field field = bmap.findVMField("VM_Scheduler", "processors");
            int address = mem.readTOC(field.getOffset());
            int processorCount = mem.read(address + VM.ARRAY_LENGTH_OFFSET);
            field = bmap.findVMField("VM_Processor", "readyQueue");
            int fieldOffset = field.getOffset();
            for (int proc = 1; proc < processorCount; proc++) {
                int procAddress = mem.read(address + proc * 4);
                if (procAddress != 0) {
                    result += "Virtual Processor " + proc + "\n";
                    int queuePointer = mem.read(procAddress + fieldOffset);
                    result += threadQueueToString(queuePointer);
                }
            }
            return result;
        } catch (BmapNotFoundException e) {
            return "ERROR: cannot find VM_Scheduler.processors, has VM_Scheduler been changed?";
        }
    }

    /**
   * Return a string for all the threads in the ready VM_Scheduler.wakeupQueue
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public String listWakeupThreads() {
        String queueName = "wakeupQueue";
        try {
            VM_Field field = bmap.findVMField("VM_Scheduler", queueName);
            int queuePointer = mem.readTOC(field.getOffset());
            return wakeupQueueToString(queuePointer);
        } catch (BmapNotFoundException e) {
            return "ERROR: cannot find VM_Scheduler." + queueName + ", has VM_Scheduler been changed?";
        }
    }

    /**
   * Given a pointer to a VM_ThreadQueue, print the name for each thread in the queue
   * @param queueName currently either the readyQueue or the wakeupQueue
   * @return a string for all the threads in this queue 
   * @exception
   * @see VM_Thread, VM_Scheduler, VM_ThreadQueue, VM_ThreadWakeupQueue
   */
    private String threadQueueToString(int queuePointer) {
        String result = "";
        int count = 0;
        int fieldOffset;
        int thisThreadPointer, headThreadPointer, tailThreadPointer;
        VM_Field field;
        try {
            field = bmap.findVMField("VM_ThreadQueue", "head");
            headThreadPointer = mem.read(queuePointer + field.getOffset());
            field = bmap.findVMField("VM_ThreadQueue", "tail");
            tailThreadPointer = mem.read(queuePointer + field.getOffset());
            thisThreadPointer = headThreadPointer;
            if (thisThreadPointer != 0) {
                result += "   " + threadToString(thisThreadPointer) + "\n";
                count++;
            }
            while (thisThreadPointer != tailThreadPointer) {
                field = bmap.findVMField("VM_Thread", "next");
                thisThreadPointer = mem.read(thisThreadPointer + field.getOffset());
                if (thisThreadPointer != 0) {
                    result += "   " + threadToString(thisThreadPointer) + "\n";
                    count++;
                } else {
                    thisThreadPointer = tailThreadPointer;
                }
            }
        } catch (BmapNotFoundException e) {
            return "ERROR: cannot find VM_ThreadQueue.head or tail, has VM_ThreadQueue been changed?";
        }
        String heading = "";
        heading += "  ID  VM_Thread   top stack frame\n";
        heading += "  -- -----------  -----------------\n";
        return "Threads in queue:  " + count + "\n" + heading + result;
    }

    /**
   * Given a pointer to a VM_ProxyWakeupQueue, print the name for each thread in the queue
   * @param queueName currently either the readyQueue or the wakeupQueue
   * @return a string for all the threads in this queue 
   * @exception
   * @see VM_Thread, VM_Scheduler, VM_ThreadQueue, VM_ThreadWakeupQueue
   */
    private String wakeupQueueToString(int queuePointer) {
        String result = "";
        int count = 0;
        int fieldOffset;
        int thisProxyPointer, thisThreadPointer;
        VM_Field field;
        try {
            field = bmap.findVMField("VM_ProxyWakeupQueue", "head");
            thisProxyPointer = mem.read(queuePointer + field.getOffset());
            while (thisProxyPointer != 0) {
                field = bmap.findVMField("VM_Proxy", "thread");
                thisThreadPointer = mem.read(thisProxyPointer + field.getOffset());
                if (thisThreadPointer != 0) {
                    result += "   " + threadToString(thisThreadPointer) + "\n";
                    count++;
                }
                field = bmap.findVMField("VM_Proxy", "wakeupNext");
                thisProxyPointer = mem.read(thisProxyPointer + field.getOffset());
            }
        } catch (BmapNotFoundException e) {
            return "ERROR: cannot find VM_ThreadQueue.head or tail, has VM_ThreadQueue been changed?";
        }
        String heading = "";
        heading += "  ID  VM_Thread   top stack frame\n";
        heading += "  -- -----------  -----------------\n";
        return "Threads in queue:  " + count + "\n" + heading + result;
    }

    /**
   * List the threads current loaded and running in the Lintel system threads
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public String listGCThreads() {
        String queueName = "collectorQueue";
        try {
            VM_Field field = bmap.findVMField("VM_Scheduler", queueName);
            int queuePointer = mem.readTOC(field.getOffset());
            return "GC threads: \n" + threadQueueToString(queuePointer);
        } catch (BmapNotFoundException e) {
            return "ERROR: cannot find VM_Scheduler." + queueName + ", has VM_Scheduler been changed?";
        }
    }

    /**
   * List the threads current loaded and running in the Lintel system threads
   * @return  
   * @exception
   * @see VM_Thread, VM_Scheduler
   */
    public String listRunThreads() {
        String result = "";
        int runThreads[] = getVMThreadIDInSystemThread();
        try {
            for (int j = 0; j < runThreads.length; j++) {
                runThreads[j] = getVMThreadForIndex(runThreads[j]);
            }
            result += "Running in system threads: " + runThreads.length + "\n";
            result += "  ID  VM_Thread   top stack frame\n";
            result += "  -- -----------  -----------------\n";
            for (int i = 0; i < runThreads.length; i++) {
                if (runThreads[i] != 0) {
                    result += "   " + threadToString(runThreads[i]) + "\n";
                } else {
                    System.out.println("CAUTION:  unexpected null thread in system thread\n");
                }
            }
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String threadToString(int threadPointer) {
        String result = "";
        try {
            int id = mem.read(threadPointer + VM_Entrypoints.threadSlotOffset);
            result += id + " @" + VM.intAsHexString(threadPointer);
            int ip = reg.getVMThreadIP(id);
            int[] savedRegs = reg.getVMThreadGPR(id);
            int pr = savedRegs[reg.regGetNum("PR")];
            int fp = reg.getFPFromPR(pr);
            int compiledMethodID = bmap.getCompiledMethodID(fp, ip);
            if (compiledMethodID == NATIVE_METHOD_ID) {
                result += "  (native procedure)";
            } else {
                VM_Method mth = bmap.findVMMethod(compiledMethodID, true);
                if (mth != null) {
                    String method_name = mth.getName().toString();
                    String method_sig = mth.getDescriptor().toString();
                    result += "  " + method_name + "  " + method_sig;
                } else {
                    result += "  (method ID not set up yet)";
                }
            }
            return result;
        } catch (BmapNotFoundException e) {
            return "cannot find VM_Thread.threadSlot, has VM_Thread.java been changed?";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void enableIgnoreOtherBreakpointTrap() {
        ignoreOtherBreakpointTrap = true;
    }

    public void disableIgnoreOtherBreakpointTrap() {
        ignoreOtherBreakpointTrap = false;
    }
}
