package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.session.Session;
import com.bluemarsh.jswat.core.util.Threads;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.VirtualMachine;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.NbBundle;

/**
 * Displays all of the thread groups in the debuggee.
 *
 * @author Nathan Fiedler
 */
public class ThreadGroupsCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "threadgroups";
    }

    @Override
    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        Session session = context.getSession();
        PrintWriter writer = context.getWriter();
        VirtualMachine vm = session.getConnection().getVM();
        if (arguments.hasMoreTokens()) {
            long tid = -1;
            String name = arguments.nextToken();
            try {
                tid = Long.parseLong(name);
            } catch (NumberFormatException nfe) {
            }
            List<ThreadGroupReference> groupsList = null;
            Iterator<ThreadGroupReference> iter = Threads.iterateGroups(vm.topLevelThreadGroups());
            if (tid > -1) {
                while (iter.hasNext()) {
                    ThreadGroupReference group = iter.next();
                    if (group.uniqueID() == tid) {
                        groupsList = group.threadGroups();
                        break;
                    }
                }
            } else {
                groupsList = new ArrayList<ThreadGroupReference>();
                Pattern patt = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
                while (iter.hasNext()) {
                    ThreadGroupReference group = iter.next();
                    Matcher matcher = patt.matcher(group.name());
                    if (matcher.find()) {
                        groupsList.addAll(group.threadGroups());
                    } else {
                        String idstr = String.valueOf(group.uniqueID());
                        matcher = patt.matcher(idstr);
                        if (matcher.find()) {
                            groupsList.addAll(group.threadGroups());
                        }
                    }
                }
            }
            if (groupsList == null || groupsList.size() == 0) {
                writer.println(NbBundle.getMessage(getClass(), "CTL_threadgroups_NoGroupsInGroup"));
            } else if (groupsList.size() > 0) {
                iter = groupsList.iterator();
                while (iter.hasNext()) {
                    ThreadGroupReference group = iter.next();
                    printGroup(group, writer, "");
                }
            }
        } else {
            List topGroups = vm.topLevelThreadGroups();
            if (topGroups == null || topGroups.size() == 0) {
                writer.println(NbBundle.getMessage(getClass(), "CTL_threadgroups_NoGroups"));
            } else if (topGroups.size() > 0) {
                Iterator iter = topGroups.iterator();
                while (iter.hasNext()) {
                    ThreadGroupReference group = (ThreadGroupReference) iter.next();
                    printGroup(group, writer, "");
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
    private void printGroup(ThreadGroupReference group, PrintWriter writer, String prefix) {
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
            printGroup(subgrp, writer, prefix + "  ");
        }
    }

    @Override
    public boolean requiresDebuggee() {
        return true;
    }
}
