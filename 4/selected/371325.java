package jp.ne.nifty.iga.midori.shell.cmd.internal;

import jp.ne.nifty.iga.midori.shell.eng.*;
import java.io.*;
import java.util.*;

/**
 * Help command.
 * <br>
 * $Revision: 1.3 $
 */
public class help implements MdShellCommand {

    String name;

    /**
	 * Constructor
	 */
    public help() {
        name = "help";
    }

    /**
	 * Setting command name
	 *
	 * @param name command name
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Get command name
	 *
	 * @return command name
	 */
    public String getName() {
        return name;
    }

    /**
	 * Get command description
	 *
	 * @return command description
	 */
    public String getDescription() {
        return "help: help command.\n\n" + "    usage: help [target]\n" + "      print command help.\n";
    }

    /**
	 * Command execution engine
	 *
	 * @param args command arguments
	 * @param commanThread command thread object
	 * @return result code
	 */
    public int execute(String args[], MdShellCommandThread commandThread) {
        MdShellEnv env = commandThread.getEnv();
        PrintWriter writer = new PrintWriter(commandThread.getOut());
        if (args == null) {
            Iterator it;
            writer.println("Available commands are following./n");
            writer.println("Internal commands:");
            writer.print("\t");
            it = env.getInternalCommandIterator();
            while (it.hasNext()) {
                writer.print((String) it.next() + " ");
            }
            writer.println("\n");
            writer.println("File access commands:");
            writer.print("\t");
            it = env.getFileAccessCommandIterator();
            while (it.hasNext()) {
                writer.print((String) it.next() + " ");
            }
            writer.println("\n");
            writer.println("Network commands:");
            writer.print("\t");
            it = env.getNetworkCommandIterator();
            while (it.hasNext()) {
                writer.print((String) it.next() + " ");
            }
            writer.println("\n");
            writer.println("Extra commands:");
            writer.print("\t");
            it = env.getExtraCommandIterator();
            while (it.hasNext()) {
                writer.print((String) it.next() + " ");
            }
            writer.println("\n");
        } else {
            MdShellCommand com = env.getCommand(args[0]);
            writer.println(com.getDescription());
        }
        writer.flush();
        return 0;
    }
}
