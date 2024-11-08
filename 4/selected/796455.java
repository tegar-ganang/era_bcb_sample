package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.context.DebuggingContext;
import com.bluemarsh.jswat.core.session.Session;
import com.bluemarsh.jswat.core.util.Threads;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import org.openide.util.NbBundle;

/**
 * Displays information concerning the thread locks.
 *
 * @author Nathan Fiedler
 */
public class ThreadLocksCommand extends AbstractCommand {

    public String getName() {
        return "threadlocks";
    }

    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        PrintWriter writer = context.getWriter();
        VirtualMachine vm = session.getConnection().getVM();
        DebuggingContext dc = context.getDebuggingContext();
        ThreadReference current = dc.getThread();
        if (!arguments.hasMoreTokens()) {
            if (current == null) {
                throw new CommandException(NbBundle.getMessage(getClass(), "ERR_NoThread"));
            } else {
                printThreadLockInfo(current, writer);
            }
        } else {
            String token = arguments.nextToken();
            if (token.equals("all")) {
                List<ThreadReference> threads = vm.allThreads();
                for (ThreadReference thread : threads) {
                    printThreadLockInfo(thread, writer);
                }
            } else {
                ThreadReference thread = Threads.findThread(vm, token);
                if (thread != null) {
                    printThreadLockInfo(thread, writer);
                } else {
                    throw new CommandException(NbBundle.getMessage(getClass(), "ERR_InvalidThreadID"));
                }
            }
        }
    }

    /**
     * Print thread lock information for the given thread.
     *
     * @param  thread  thread for which to display lock info.
     * @param  writer  where thread lock information should go.
     */
    protected void printThreadLockInfo(ThreadReference thread, PrintWriter writer) throws CommandException {
        try {
            VirtualMachine vm = thread.virtualMachine();
            StringBuilder sb = new StringBuilder(512);
            sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_monitorInfo", thread.name()));
            sb.append('\n');
            if (vm.canGetOwnedMonitorInfo()) {
                List<ObjectReference> owned = thread.ownedMonitors();
                if (owned.size() == 0) {
                    sb.append("  ");
                    sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_noMonitors"));
                    sb.append('\n');
                } else {
                    Iterator<ObjectReference> iter = owned.iterator();
                    while (iter.hasNext()) {
                        ObjectReference monitor = iter.next();
                        sb.append("  ");
                        sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_ownedMonitor", monitor.toString()));
                        sb.append('\n');
                    }
                }
            } else {
                sb.append("  ");
                sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_cannotGetOwnedMonitors"));
                sb.append('\n');
            }
            if (vm.canGetCurrentContendedMonitor()) {
                ObjectReference waiting = thread.currentContendedMonitor();
                sb.append("  ");
                if (waiting == null) {
                    sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_notWaiting"));
                } else {
                    sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_waitingFor", waiting.toString()));
                }
                sb.append('\n');
            } else {
                sb.append("  ");
                sb.append(NbBundle.getMessage(getClass(), "CTL_threadlocks_cannotGetContendedMonitor"));
                sb.append('\n');
            }
            writer.print(sb.toString());
        } catch (UnsupportedOperationException uoe) {
            throw new CommandException(NbBundle.getMessage(getClass(), "CTL_threadlocks_unsupported"), uoe);
        } catch (IncompatibleThreadStateException itse) {
            throw new CommandException(NbBundle.getMessage(getClass(), "ERR_ThreadNotSuspended"), itse);
        }
    }

    public boolean requiresDebuggee() {
        return true;
    }
}
