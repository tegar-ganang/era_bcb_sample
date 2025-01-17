package org.knopflerfish.bundle.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * * Interface for commands to be handled by the console. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */
public class SessionCommandGroup implements CommandGroup {

    static final String NAME = "session";

    static final String help_alias = "alias [<alias>] [<val>] ...  - Set or show aliases";

    static final String help_help = "help [<command group> | all] - Help about specific command group or all command groups";

    static final String help_echo = "echo [-n] ...                - Echo command arguments";

    static final String help_enter = "enter <command group>        - Enter command group mode";

    static final String help_leave = "leave                        - Leave command group mode";

    static final String help_prompt = "prompt <command prompt>      - Set command prompt";

    static final String help_quit = "quit                         - Exit this session";

    static final String help_save = "save [<file>]                - Save current aliases as a property file";

    static final String help_restore = "restore [<file>]             - Restore aliases from a property file or from default aliases";

    static final String help_source = "source <URL>                 - Source commands at URL";

    static final String help_unalias = "unalias <alias name>         - Remove an alias";

    private BundleContext bc;

    public SessionCommandGroup(BundleContext bc) {
        this.bc = bc;
    }

    /**
     * Utility method used for logging.
     * 
     * @param level
     *            log level
     * @param msg
     *            log message
     * @param t
     *            throwable
     */
    void log(int level, String msg, Throwable t) {
        ServiceReference srLog = bc.getServiceReference("org.osgi.service.log.LogService");
        if (srLog != null) {
            LogService sLog = (LogService) bc.getService(srLog);
            if (sLog != null) {
                sLog.log(level, msg, t);
            }
            bc.ungetService(srLog);
        }
    }

    /**
     * Returns the command group name.
     * 
     * @return command name.
     */
    public String getGroupName() {
        return NAME;
    }

    /**
     * Returns short command group help.
     * 
     * @return short command group help.
     */
    public String getShortHelp() {
        return "Session commands built into the console";
    }

    /**
     * Returns long command group help.
     * 
     * @return long command group help.
     */
    public String getLongHelp() {
        return "Available " + NAME + " commands:\n" + "  " + help_alias + "\n" + "  " + help_echo + "\n" + "  " + help_enter + "\n" + "  " + help_help + "\n" + "  " + help_leave + "\n" + "  " + help_prompt + "\n" + "  " + help_quit + "\n" + "  " + help_save + "\n" + "  " + help_restore + "\n" + "  " + help_source + "\n" + "  " + help_unalias + "\n";
    }

    /**
     * Executes the command.
     * 
     * @param args
     *            argument list passed to the command
     * @param out
     *            output device to print result
     * @param in
     *            input for command
     * @return params logical format parameters
     */
    public int execute(String[] args, Reader in, PrintWriter out, Session session) {
        SessionImpl si = (SessionImpl) session;
        String usage = null;
        if (args.length > 0) {
            if ("help".startsWith(args[0])) {
                if (args.length == 1) {
                    if ("".equals(si.currentGroup)) {
                        return help(out, si);
                    }
                    return helpAbout(si.currentGroup, out, si);
                } else if (args.length == 2) {
                    if (args[1].equals("all")) {
                        return help(out, si);
                    }
                    return helpAbout(args[1], out, si);
                }
                usage = help_help;
            } else if ("alias".startsWith(args[0])) {
                if (si == null) {
                    out.println("Alias not available from runCommand method");
                    return 1;
                }
                if (args.length == 1) {
                    for (Enumeration e = si.aliases.keys(); e.hasMoreElements(); ) {
                        String a = (String) e.nextElement();
                        out.println(a + " = " + si.aliases.getString(a));
                    }
                } else if (args.length == 2) {
                    String a = si.aliases.getString(args[1]);
                    if (a != null) {
                        out.println(args[1] + " = " + a);
                    } else {
                        out.println("No alias for: " + args[1]);
                    }
                } else {
                    String[] na = new String[args.length - 2];
                    System.arraycopy(args, 2, na, 0, na.length);
                    si.aliases.put(args[1], na);
                }
                return 0;
            } else if ("enter".startsWith(args[0])) {
                if (args.length == 2) {
                    try {
                        ServiceReference ref = Command.matchCommandGroup(bc, args[1]);
                        if (ref != null) {
                            si.currentGroup = (String) ref.getProperty("groupName");
                        } else {
                            si.currentGroup = NAME;
                        }
                        return 0;
                    } catch (IOException e) {
                        out.println(e.getMessage());
                        return 1;
                    }
                }
                usage = help_enter;
                return 1;
            } else if ("echo".startsWith(args[0])) {
                int pos = 1;
                boolean nl = true;
                if (args.length >= 2 && "-n".equals(args[1])) {
                    nl = false;
                    pos = 2;
                }
                while (pos < args.length) {
                    out.print(args[pos]);
                    if (++pos < args.length) {
                        out.print(" ");
                    }
                }
                if (nl) {
                    out.println();
                }
                return 0;
            } else if ("leave".startsWith(args[0])) {
                if (args.length == 1) {
                    si.currentGroup = "";
                    return 0;
                }
                usage = help_leave;
            } else if ("prompt".startsWith(args[0])) {
                if (args.length == 2) {
                    si.prompt = args[1];
                    return 0;
                }
                usage = help_prompt;
            } else if ("quit".startsWith(args[0])) {
                if (args.length == 1) {
                    si.close();
                    return 0;
                }
                usage = help_quit;
            } else if ("save".startsWith(args[0])) {
                File file = null;
                if (args.length == 1) {
                    file = bc.getDataFile(SessionImpl.ALIAS_SAVE);
                } else if (args.length == 2) {
                    file = new File(args[1]);
                }
                if (file != null) {
                    try {
                        OutputStream p = new FileOutputStream(file);
                        si.aliases.save(p);
                    } catch (IOException e) {
                        out.println("Failed to save aliases: " + e);
                        return 1;
                    }
                    return 0;
                }
                usage = help_save;
            } else if ("restore".startsWith(args[0])) {
                if (args.length == 1) {
                    si.aliases.setDefault();
                    return 0;
                } else if (args.length == 2) {
                    try {
                        InputStream r = new FileInputStream(new File(args[1]));
                        si.aliases.clear();
                        si.aliases.restore(r);
                    } catch (IOException e) {
                        out.println("Failed to restore aliases from " + args[1] + ": " + e);
                        return 1;
                    }
                    return 0;
                }
                usage = help_restore;
            } else if ("source".startsWith(args[0])) {
                if (args.length == 2) {
                    InputStreamReader sin = null;
                    try {
                        URL surl = new URL(args[1]);
                        sin = new InputStreamReader(surl.openStream());
                        SessionImpl ss = new SessionImpl(bc, "source: " + args[1], sin, out, null);
                        ss.prompt = null;
                        ss.start();
                        try {
                            ss.join();
                        } catch (InterruptedException ignore) {
                        }
                        ss.close();
                    } catch (IOException e) {
                        out.println("Failed to source URL: " + e.getMessage());
                        return 1;
                    } finally {
                        if (sin != null) {
                            try {
                                sin.close();
                            } catch (IOException ignore) {
                            }
                        }
                    }
                    return 0;
                }
                usage = help_source;
            } else if ("unalias".startsWith(args[0])) {
                if (si == null) {
                    out.println("Unalias not available from runCommand method");
                    return 1;
                }
                if (args.length == 2) {
                    if (si.aliases.remove(args[1]) != null) {
                        return 0;
                    }
                    return 1;
                }
                usage = help_unalias;
            }
        }
        if (usage != null) {
            usage = "Usage: " + usage;
        } else {
            usage = getLongHelp();
        }
        out.println(usage);
        return -1;
    }

    /**
     * Help about available command groups.
     * 
     * @param out
     *            output device to print result
     */
    int help(PrintWriter out, SessionImpl si) {
        ArrayList cg = new ArrayList();
        cg.add(this);
        ServiceReference[] refs = null;
        try {
            refs = bc.getServiceReferences(Command.COMMAND_GROUP, null);
        } catch (InvalidSyntaxException ignore) {
        }
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                CommandGroup c = (CommandGroup) bc.getService(refs[i]);
                if (c != null) {
                    String n = c.getGroupName();
                    int j;
                    for (j = 0; j < cg.size(); j++) {
                        if (n.compareTo(((CommandGroup) cg.get(j)).getGroupName()) > 0) {
                            break;
                        }
                    }
                    cg.add(j, c);
                }
            }
        }
        out.println("Available command groups (type 'enter' to enter a group):");
        for (Iterator e = cg.iterator(); e.hasNext(); ) {
            CommandGroup c = (CommandGroup) e.next();
            out.println(c.getGroupName() + " - " + c.getShortHelp());
        }
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                bc.ungetService(refs[i]);
            }
        }
        return 0;
    }

    /**
     * Help about a command group.
     * 
     * @param out
     *            output device to print result
     */
    int helpAbout(String group, PrintWriter out, SessionImpl si) {
        try {
            ServiceReference ref = Command.matchCommandGroup(bc, group);
            if (ref != null) {
                CommandGroup c = (CommandGroup) bc.getService(ref);
                if (c != null) {
                    out.print(c.getLongHelp());
                    bc.ungetService(ref);
                    return 0;
                }
            } else {
                out.print(getLongHelp());
                return 0;
            }
        } catch (IOException e) {
            out.println(e.getMessage());
            return 1;
        }
        out.println("No such command group: " + group);
        return 1;
    }
}
