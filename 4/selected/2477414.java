package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.context.DebuggingContext;
import com.bluemarsh.jswat.core.session.Session;
import com.bluemarsh.jswat.core.util.Threads;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import java.io.PrintWriter;
import java.util.List;
import org.openide.util.NbBundle;

/**
 * Displays the call stack for one or all threads.
 *
 * @author Nathan Fiedler
 */
public class WhereCommand extends AbstractCommand {

    /**
     * First argument, or empty string if no args.
     */
    protected String arg = "";

    @Override
    public String getName() {
        return "where";
    }

    @Override
    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        PrintWriter writer = context.getWriter();
        VirtualMachine vm = session.getConnection().getVM();
        DebuggingContext dc = context.getDebuggingContext();
        ThreadReference current = dc.getThread();
        if (!arguments.hasMoreTokens()) {
            if (current == null) {
                throw new CommandException(getMessage("ERR_NoThread"));
            } else {
                printStack(current, writer, dc);
            }
        } else {
            arg = arguments.nextToken();
            if (arg.equals("all")) {
                List<ThreadReference> threads = vm.allThreads();
                for (ThreadReference thread : threads) {
                    printStack(thread, writer, dc);
                    writer.println();
                }
            } else {
                ThreadReference thread = Threads.findThread(vm, arg);
                if (thread != null) {
                    printStack(thread, writer, dc);
                } else {
                    throw new CommandException(getMessage("ERR_InvalidThreadID"));
                }
            }
        }
    }

    /**
     * Display the stack frames of the given thread.
     *
     * @param  thread  ThreadReference whose stack is to be printed.
     * @param  writer  writer to print stack to.
     * @param  dc      debugging context.
     * @throws  CommandException
     *          if something goes wrong.
     */
    protected void printStack(ThreadReference thread, PrintWriter writer, DebuggingContext dc) throws CommandException {
        List<StackFrame> stack = getStack(thread);
        boolean threadIsCurrent = false;
        ThreadReference currThrd = dc.getThread();
        if (currThrd != null && currThrd.equals(thread)) {
            threadIsCurrent = true;
        }
        StringBuilder sb = new StringBuilder(256);
        sb.append(getMessage("CTL_where_header", thread.name()));
        sb.append('\n');
        int nFrames = stack.size();
        if (nFrames == 0) {
            sb.append(getMessage("CTL_where_emptyStack"));
            sb.append('\n');
        }
        for (int i = 0; i < nFrames; i++) {
            Location loc = stack.get(i).location();
            if (threadIsCurrent) {
                if (dc.getFrame() == i) {
                    sb.append("* [");
                } else {
                    sb.append("  [");
                }
            } else {
                sb.append("  [");
            }
            sb.append(i);
            sb.append("] ");
            appendFrameDescriptor(loc, sb);
            long pc = loc.codeIndex();
            if (pc != -1) {
                sb.append(", pc = ");
                sb.append(pc);
            }
            sb.append('\n');
        }
        writer.print(sb.toString());
    }

    /**
     * Append a description of the current frame's location.
     * Does not print the "[i]" stack frame number.
     */
    public void appendFrameDescriptor(Location loc, StringBuilder sb) {
        Method method = loc.method();
        sb.append(method.declaringType().name());
        sb.append('.');
        sb.append(method.name());
        sb.append(" (");
        if (method.isNative()) {
            sb.append(getMessage("CTL_where_native"));
        } else if (loc.lineNumber() != -1) {
            try {
                sb.append(loc.sourceName());
            } catch (AbsentInformationException e) {
                sb.append(getMessage("CTL_where_absentInfo"));
            }
            sb.append(':');
            sb.append(loc.lineNumber());
        }
        sb.append(')');
    }

    /**
     * Return a list of {@link StackFrame} objects for passed thread.
     */
    public List<StackFrame> getStack(ThreadReference thread) throws CommandException {
        List<StackFrame> stack = null;
        try {
            stack = thread.frames();
        } catch (IncompatibleThreadStateException itse) {
            throw new CommandException(getMessage("ERR_ThreadNotSuspended"));
        } catch (ObjectCollectedException oce) {
            throw new CommandException(getMessage("ERR_ObjectCollected"));
        }
        if (stack == null) {
            throw new CommandException(getMessage("ERR_IncompatibleThread"));
        }
        return stack;
    }

    @Override
    public boolean requiresDebuggee() {
        return true;
    }

    protected String getMessage(String key) {
        return NbBundle.getMessage(WhereCommand.class, key);
    }

    protected String getMessage(String key, String arg) {
        return NbBundle.getMessage(WhereCommand.class, key, arg);
    }
}
