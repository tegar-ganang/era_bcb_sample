package blueprint4j.utils;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

public abstract class Command {

    private static SimpleDateFormat simple_date_format = new SimpleDateFormat("yyyyMMdd");

    private static SimpleDateFormat simple_date_time_format = new SimpleDateFormat("yyyy_MM_dd_hh_mm");

    public abstract String getGroupName();

    /**
	 * Is this command in a state that it can be safely shutdown
	 */
    public abstract boolean isSafeToStop();

    private boolean isvalidDirectory(String p_directory) throws Exception {
        if (p_directory.length() <= getGroupName().length() && p_directory.indexOf("*") != -1 && getGroupName().substring(0, p_directory.length()).equalsIgnoreCase(p_directory)) {
            return true;
        }
        if (p_directory.length() <= getGroupName().length() && p_directory.indexOf("*") == -1 && getGroupName().equalsIgnoreCase(p_directory)) {
            return true;
        }
        if (findMethod(p_directory) != null) {
            return true;
        }
        return false;
    }

    /**
	 * Does directory exist (does the method or the command exist)
	 */
    public String doesDirectoryExist(String directory) {
        try {
            StringTokenizer arguments = new StringTokenizer(directory, "/");
            String token = arguments.nextToken();
            if (token.indexOf("*") != -1 && token.indexOf("*") <= getGroupName().length() && getGroupName().substring(0, token.indexOf("*")).equalsIgnoreCase(token.substring(0, token.indexOf("*")))) {
                return getGroupName();
            }
            if (token.indexOf("*") == -1 && token.length() <= getGroupName().length() && getGroupName().equalsIgnoreCase(token)) {
                return getGroupName();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private boolean isMethodACommandMethod(Method method) {
        if (method.getName().length() < "command".length() || !method.getName().substring(0, "command".length()).equals("command")) {
            return false;
        }
        if (method.getParameterTypes().length != 2) {
            return false;
        }
        if (!method.getParameterTypes()[0].isArray() || method.getParameterTypes()[0].getName().indexOf("java.lang.String") == -1) {
            return false;
        }
        if (method.getParameterTypes()[1].getName().indexOf("java.io.OutputStream") == -1) {
            return false;
        }
        return true;
    }

    String listCommands() {
        String str = "";
        Method methods[] = getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (isMethodACommandMethod(methods[i])) {
                str += "\t" + methods[i].getName().substring("command".length()) + "\r\n";
            }
        }
        return str;
    }

    protected Method findMethod(String method_name) throws Exception {
        Method methods[] = getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if ((methods[i].getName().equalsIgnoreCase(method_name) || (method_name.indexOf("*") != -1 && methods[i].getName().length() >= method_name.indexOf("*") && methods[i].getName().substring(0, method_name.indexOf("*")).equalsIgnoreCase(method_name.substring(0, method_name.indexOf("*"))))) && (methods[i].getName().length() > "command".length() && methods[i].getName().substring(0, "command".length()).equals("command"))) {
                return methods[i];
            }
        }
        return null;
    }

    /**
	 * This expects arguments in the following manner:<space><argument>exct
	 */
    protected String[] buildArguments(String str) {
        str = " " + str + " ";
        StringTokenizer arguments = new StringTokenizer(str.substring(str.indexOf(" ") + 1));
        String args[] = new String[arguments.countTokens()];
        for (int i = 0; i < args.length; i++) {
            args[i] = arguments.nextToken(" ");
        }
        return args;
    }

    /**
	 * Expects the method -parameters
	 */
    public void invokeCommand(String cmd, OutputStream output_stream) throws Exception {
        String method_name = cmd.substring(0, cmd.indexOf(" "));
        String args[] = buildArguments(cmd.substring(cmd.indexOf(" ") + 1));
        Method methods[] = getClass().getMethods();
        Object objects[] = new Object[2];
        objects[0] = args;
        objects[1] = output_stream;
        if (findMethod("command" + method_name) != null) {
            findMethod("command" + method_name).invoke(this, objects);
        } else {
            commandHelp(null, output_stream);
        }
    }

    /**
	 * This displays a list of all the current methods, and all the classes that are contained
	 */
    public void commandHelp(String args[], OutputStream output_stream) throws Exception {
        output_stream.write(("<" + getGroupName() + "> has the following executables\r\n").getBytes());
        output_stream.write((listCommands() + "\r\n").getBytes());
    }

    /**
	 * This will find all the logs for a) a specific log thread id, b) all thread id's
	 * A date range must be specified, else if defaults to this date
	 */
    public void commandDisplayLogs(String args[], OutputStream output_stream) throws Exception {
        Integer tid = null;
        Date from_date = new java.util.Date((System.currentTimeMillis() - 1000 * 60 * 60));
        Date to_date = new java.util.Date();
        Vector levels = new Vector();
        if (args == null || args.length == 0) {
            args = new String[] { "-help" };
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-help")) {
                output_stream.write(("DisplayLogs\r\n" + "\t-tid              The specified thread id <default to all threads>\r\n" + "\t-from_date        The from date that will be searched from <default to the beginnig of today. The date format is yyyy_MM_dd_hh_mm>\r\n" + "\t-to_date          The to date that will be searched to <default to the current time. The date format is yyyy_MM_dd_hh_mm>\r\n" + "\t-level            The logging level <the options available is [trace][debug][critical]. You can specify all three. Defaults to critical>\r\n" + "\t-current_thread   This Displays the current working thread\r\n" + "\t-help             Displays this help\r\n").getBytes());
                return;
            }
            if (args[i].equalsIgnoreCase("-tid")) {
                tid = new Integer(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-from_date")) {
                try {
                    from_date = simple_date_time_format.parse(args[i + 1]);
                } catch (java.text.ParseException pe) {
                    output_stream.write(("There was an error processing the from date. The correct format is [yyyy_MM_dd_hh_mm]. Your entry was [" + args[i + 1] + "]\r\n").getBytes());
                    return;
                }
            }
            if (args[i].equalsIgnoreCase("-to_date")) {
                try {
                    to_date = simple_date_time_format.parse(args[i + 1]);
                } catch (java.text.ParseException pe) {
                    output_stream.write(("There was an error processing the to date. The correct format is [yyyy_MM_dd_hh_mm]. Your entry was [" + args[i + 1] + "]\r\n").getBytes());
                    return;
                }
            }
            if (args[i].equalsIgnoreCase("-level")) {
                if (args[i + 1].equalsIgnoreCase("trace")) {
                    levels.add(Logging.TRACE);
                }
                if (args[i + 1].equalsIgnoreCase("debug")) {
                    levels.add(Logging.DEBUG);
                }
                if (args[i + 1].equalsIgnoreCase("critical")) {
                    levels.add(Logging.CRITICAL);
                }
            }
            if (args[i].equalsIgnoreCase("-current_thread")) {
                output_stream.write((ThreadId.getCurrentId() + "\r\n").getBytes());
                return;
            }
            if (levels.size() == 0) {
                levels.add(Logging.CRITICAL);
            }
        }
        Logging.Level levels_ar[] = new Logging.Level[levels.size()];
        for (int i = 0; i < levels_ar.length; i++) {
            levels_ar[i] = (Logging.Level) levels.get(i);
        }
    }
}
