package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.ContextManager;
import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.util.Threads;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import java.util.Iterator;
import java.util.List;

/**
 * Defines the class that handles the 'where' command.
 *
 * @author  Nathan Fiedler
 */
public class whereCommand extends JSwatCommand {

    /**
     * Perform the 'where' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        if (!session.isActive()) {
            throw new CommandException(Bundle.getString("noActiveSession"));
        }
        ContextManager ctxtMgr = (ContextManager) session.getManager(ContextManager.class);
        ThreadReference current = ctxtMgr.getCurrentThread();
        if (!args.hasMoreTokens()) {
            if (current == null) {
                throw new CommandException(Bundle.getString("noCurrentThread"));
            } else {
                printStack(current, out, ctxtMgr);
            }
        } else {
            String token = args.nextToken();
            if (token.toLowerCase().equals("all")) {
                Iterator iter = session.getVM().allThreads().iterator();
                while (iter.hasNext()) {
                    ThreadReference thread = (ThreadReference) iter.next();
                    out.writeln(thread.name() + ": ");
                    try {
                        printStack(thread, out, ctxtMgr);
                    } catch (CommandException ce) {
                        out.writeln(ce.getMessage());
                    }
                }
            } else {
                VirtualMachine vm = session.getConnection().getVM();
                ThreadReference thread = Threads.getThreadByID(vm, token);
                if (thread != null) {
                    printStack(thread, out, ctxtMgr);
                } else {
                    throw new CommandException(Bundle.getString("invalidThreadID"));
                }
            }
        }
    }

    /**
     * Display the stack frames of the given thread, possibly with
     * program counter information included.
     *
     * @param  thread   ThreadReference whose stack is to be printed.
     * @param  out      Output to print stack to.
     * @param  ctxtMgr  Context manager.
     */
    protected void printStack(ThreadReference thread, Log out, ContextManager ctxtMgr) {
        List stack = null;
        try {
            stack = thread.frames();
        } catch (IncompatibleThreadStateException itse) {
            throw new CommandException(Bundle.getString("threadNotSuspended"), itse);
        } catch (ObjectCollectedException oce) {
            throw new CommandException(Bundle.getString("objectCollected"), oce);
        }
        if (stack == null) {
            throw new CommandException(Bundle.getString("threadNotRunning"));
        }
        int nFrames = stack.size();
        if (nFrames == 0) {
            out.writeln(Bundle.getString("where.emptyStack"));
        }
        boolean threadIsCurrent = false;
        ThreadReference currThrd = ctxtMgr.getCurrentThread();
        if ((currThrd != null) && currThrd.equals(thread)) {
            threadIsCurrent = true;
        }
        StringBuffer buf = new StringBuffer(256);
        for (int i = 0; i < nFrames; i++) {
            StackFrame frame = (StackFrame) stack.get(i);
            Location loc = frame.location();
            Method method = loc.method();
            if (threadIsCurrent) {
                if (ctxtMgr.getCurrentFrame() == i) {
                    buf.append("* [");
                } else {
                    buf.append("  [");
                }
            } else {
                buf.append("  [");
            }
            buf.append(i + 1);
            buf.append("] ");
            buf.append(method.declaringType().name());
            buf.append('.');
            buf.append(method.name());
            buf.append(" (");
            if (method.isNative()) {
                buf.append("native method");
            } else if (loc.lineNumber() != -1) {
                try {
                    buf.append(loc.sourceName());
                } catch (AbsentInformationException e) {
                    buf.append("<unknown>");
                }
                buf.append(':');
                buf.append(loc.lineNumber());
            }
            buf.append(')');
            long pc = loc.codeIndex();
            if (pc != -1) {
                buf.append(", pc = ");
                buf.append(pc);
            }
            buf.append('\n');
        }
        out.write(buf.toString());
    }
}
