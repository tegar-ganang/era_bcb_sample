package czestmyr.jjsched;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Handles master console input
 */
public class Console extends Thread {

    Console(CommandParser p, ActionExecutor ae, Macros m) {
        theExecutor = ae;
        theParser = p;
        theMacros = m;
        reader = new BufferedReader(new InputStreamReader(Channels.newInputStream((new FileInputStream(FileDescriptor.in)).getChannel())));
        doContinue = true;
        setName("JJSched console thread");
        setPriority(4);
    }

    /**
	 * Processes the console input by reading System.in
	 */
    public void run() {
        System.out.println("Welcome to jjsched console. Type 'help' for standard command help.");
        while (doContinue) {
            String line = "";
            try {
                line = reader.readLine();
            } catch (Exception e) {
                continue;
            }
            LinkedList<String> tokens = StringParser.parseLine(line);
            ListIterator it = tokens.listIterator(0);
            while (it.hasNext()) {
                String token = (String) it.next();
                if (theMacros.isMacro(token)) {
                    it.remove();
                    Iterator macroit = theMacros.getMacroBody(token).iterator();
                    while (macroit.hasNext()) {
                        it.add((String) macroit.next());
                    }
                }
            }
            Action action = theParser.parse(tokens, "console", "console");
            theExecutor.execute(action);
        }
    }

    /**
	 * Stops the console
	 */
    public void discontinue() {
        doContinue = false;
        this.interrupt();
    }

    private ActionExecutor theExecutor;

    private CommandParser theParser;

    private Macros theMacros;

    private boolean doContinue;

    private BufferedReader reader;
}
