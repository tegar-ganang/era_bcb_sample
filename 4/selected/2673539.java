package com.bluemarsh.jswat.console.commands;

import com.sun.jdi.ThreadReference;
import java.io.PrintWriter;
import org.openide.util.NbBundle;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.core.context.ContextProvider;
import com.bluemarsh.jswat.core.context.DebuggingContext;
import com.sun.jdi.StackFrame;
import java.util.List;

/**
 * Displays the call stack for one or all threads.
 *
 * @author Steve Yegge
 */
public class WhereCommand extends com.bluemarsh.jswat.command.commands.WhereCommand {

    @Override
    protected void printStack(ThreadReference thread, PrintWriter writer, DebuggingContext dc) throws CommandException {
        if (!com.bluemarsh.jswat.console.Main.emulateJDB()) {
            super.printStack(thread, writer, dc);
            return;
        }
        StringBuilder sb = new StringBuilder(1024);
        if (!arg.isEmpty()) {
            sb.append(NbBundle.getMessage(getClass(), "CTL_where_header", thread.name()));
            sb.append('\n');
        }
        List<StackFrame> stack = getStack(thread);
        for (int i = dc.getFrame(), nFrames = stack.size(); i < nFrames; i++) {
            sb.append("  [");
            sb.append(i + 1);
            sb.append("] ");
            appendFrameDescriptor(stack.get(i).location(), sb);
            sb.append("\n");
        }
        int len = sb.length(), end = len - 1;
        String result;
        if ("all".equals(arg) && len > 0 && sb.charAt(end) == '\n') {
            result = sb.substring(0, end);
        } else {
            result = sb.toString();
        }
        writer.print(result);
    }

    /**
     * Utility shared by "up"/"down"/"frame" to emit just the
     * currently active frame of the current thread's stack trace.
     * Assumes we've already validated all the preconditions.
     */
    void displayCurrentFrame(CommandContext context) throws CommandException {
        DebuggingContext dc = ContextProvider.getContext(context.getSession());
        List<StackFrame> stack = getStack(dc.getThread());
        int i = dc.getFrame();
        StringBuilder sb = new StringBuilder(256);
        sb.append("  [").append(i).append("] ");
        appendFrameDescriptor(stack.get(i).location(), sb);
        context.getWriter().println(sb.toString());
    }
}
