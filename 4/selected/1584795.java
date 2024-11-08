package com.jgpshell.shell;

/**
 * @author Moez Ben MBarka
 *
 */
public class Usage {

    /**
	 * displays a general help
	 */
    public static void usage(Screen screen) {
        screen.writeln("JGPShell v 0.1");
        screen.writeln("commands list :");
    }

    /**
	 * Display help corresponding to the command cmd and returns true if the command is found
	 * returns false if not.
	 * 
	 * @param screen
	 * @param cmd
	 * @return
	 */
    public static boolean usage(Screen screen, String cmd) {
        if (cmd.equals("/ls")) {
            screen.writeln("/ls : Displays the list of connected readers");
            return true;
        }
        return false;
    }
}
