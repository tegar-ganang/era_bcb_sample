package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.context.DebuggingContext;
import com.bluemarsh.jswat.core.session.Session;
import com.bluemarsh.jswat.core.util.Threads;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.NbBundle;

/**
 * Displays all of the threads in the debuggee.
 *
 * @author Nathan Fiedler
 */
public class ThreadsCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "threads";
    }

    @Override
    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        PrintWriter writer = context.getWriter();
        VirtualMachine vm = session.getConnection().getVM();
        DebuggingContext dc = context.getDebuggingContext();
        ThreadReference current = dc.getThread();
        if (arguments.hasMoreTokens()) {
            long tid = -1;
            String name = arguments.nextToken();
            try {
                tid = Long.parseLong(name);
            } catch (NumberFormatException nfe) {
            }
            List<ThreadReference> threadsList = null;
            Iterator iter = Threads.iterateGroups(vm.topLevelThreadGroups());
            if (tid > -1) {
                while (iter.hasNext()) {
                    ThreadGroupReference group = (ThreadGroupReference) iter.next();
                    if (group.uniqueID() == tid) {
                        threadsList = group.threads();
                        break;
                    }
                }
            } else {
                threadsList = new ArrayList<ThreadReference>();
                Pattern patt = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
                while (iter.hasNext()) {
                    ThreadGroupReference group = (ThreadGroupReference) iter.next();
                    Matcher matcher = patt.matcher(group.name());
                    if (matcher.find()) {
                        threadsList.addAll(group.threads());
                    } else {
                        String idstr = String.valueOf(group.uniqueID());
                        matcher = patt.matcher(idstr);
                        if (matcher.find()) {
                            threadsList.addAll(group.threads());
                        }
                    }
                }
            }
            if (threadsList == null || threadsList.size() == 0) {
                writer.println(NbBundle.getMessage(getClass(), "CTL_threads_noThreadsInGroup"));
            } else if (threadsList.size() > 0) {
                writer.println(printThreads(threadsList.iterator(), "  ", current));
            }
        } else {
            List topGroups = vm.topLevelThreadGroups();
            if (topGroups == null || topGroups.size() == 0) {
                writer.println(NbBundle.getMessage(getClass(), "CTL_threads_noThreads"));
            } else if (topGroups.size() > 0) {
                Iterator iter = topGroups.iterator();
                while (iter.hasNext()) {
                    ThreadGroupReference group = (ThreadGroupReference) iter.next();
                    printGroup(group, current, writer, "");
                }
            }
        }
    }

    /**
     * Print the thread group to the output with each line prefixed
     * by the given string.
     *
     * @param  group    thread group to print.
     * @param  current  current thread.
     * @param  writer   writer to print to.
     * @param  prefix   string to display before each line.
     */
    protected void printGroup(ThreadGroupReference group, ThreadReference current, PrintWriter writer, String prefix) {
        ReferenceType clazz = group.referenceType();
        String id = String.valueOf(group.uniqueID());
        if (clazz == null) {
            writer.println(prefix + id + ' ' + group.name());
        } else {
            writer.println(prefix + id + ' ' + group.name() + " (" + clazz.name() + ')');
        }
        List<ThreadGroupReference> groups = group.threadGroups();
        Iterator<ThreadGroupReference> iter = groups.iterator();
        while (iter.hasNext()) {
            ThreadGroupReference subgrp = iter.next();
            printGroup(subgrp, current, writer, prefix + "  ");
        }
        List<ThreadReference> threads = group.threads();
        writer.print(printThreads(threads.iterator(), prefix + "  ", current));
    }

    /**
     * Print the threads in the given iterator. Indicate which thread is
     * the current one by comparing to the given current.
     *
     * @param  iter     threads iterator.
     * @param  prefix   prefix for each output line.
     * @param  current  current thread.
     * @return  output from printing the threads.
     */
    protected String printThreads(Iterator<ThreadReference> iter, String prefix, ThreadReference current) {
        StringBuilder sb = new StringBuilder(256);
        String starfix = prefix.substring(1);
        while (iter.hasNext()) {
            ThreadReference thrd = iter.next();
            if (thrd.equals(current)) {
                sb.append('*');
                sb.append(starfix);
            } else {
                sb.append(prefix);
            }
            sb.append(thrd.uniqueID());
            sb.append(' ');
            sb.append(thrd.name());
            sb.append(": ");
            sb.append(Threads.threadStatus(thrd));
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean requiresDebuggee() {
        return true;
    }
}
