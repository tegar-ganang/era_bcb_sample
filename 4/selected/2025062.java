package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.util.Strings;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Defines the class that handles the 'help' command.
 *
 * @author  Nathan Fiedler
 */
public class helpCommand extends JSwatCommand {

    /** The resource bundle contained in this object. */
    private static ResourceBundle resourceBundle;

    /** The current interactive help category. */
    private String currentCategory;

    static {
        resourceBundle = ResourceBundle.getBundle("com.bluemarsh.jswat.command.help");
    }

    /**
     * User has selected a help category.
     *
     * @param  out      Output to write messages to.
     * @param  cmdman   CommandManager that's calling us.
     * @param  input    Trimmed input from user.
     */
    protected void handleCategorySelection(Log out, CommandManager cmdman, String input) {
        int subnum = -1;
        try {
            subnum = Integer.parseInt(input);
        } catch (NumberFormatException nfe) {
        }
        String subStr = resourceBundle.getString(currentCategory + ".subs");
        String[] subs = Strings.tokenize(subStr);
        if (subnum > 0 && subnum <= subs.length) {
            subnum--;
            if (subs[subnum].charAt(0) == '_') {
                String cmnd = subs[subnum].substring(1);
                out.write(Bundle.getString("help.helpFor"));
                out.write(": ");
                out.writeln(cmnd);
                JSwatCommand command = cmdman.getCommand(cmnd);
                if (command != null) {
                    command.help(out);
                }
                out.writeln(Bundle.getString("help.separator"));
                printFooter(out, currentCategory);
            } else {
                currentCategory = currentCategory + '.' + subs[subnum];
                printCategory(out, currentCategory);
            }
        } else {
            out.writeln(Bundle.getString("help.invalidCategory"));
        }
        cmdman.grabInput(this);
    }

    /**
     * Go up one help category.
     *
     * @param  out      Output to write messages to.
     * @param  cmdman   CommandManager that's calling us.
     */
    protected void handleUp(Log out, CommandManager cmdman) {
        if (currentCategory.equals("top")) {
            out.writeln(Bundle.getString("help.atTopAlready"));
        } else {
            int dot = currentCategory.lastIndexOf('.');
            currentCategory = currentCategory.substring(0, dot);
            printCategory(out, currentCategory);
        }
        cmdman.grabInput(this);
    }

    /**
     * Perform the 'help' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        CommandManager cmdman = (CommandManager) session.getManager(CommandManager.class);
        if (!args.hasMoreTokens()) {
            currentCategory = "top";
            printCategory(out, currentCategory);
            cmdman.grabInput(this);
        } else {
            String argument = args.nextToken();
            JSwatCommand command = cmdman.getCommand(argument, false);
            if (argument.equals("commands")) {
                cmdman.listCommands();
                cmdman.listAliases();
            } else if (cmdman.getAlias(argument) != null) {
                out.writeln(argument + ' ' + Bundle.getString("help.isaAlias"));
                out.writeln(cmdman.getAlias(argument));
            } else if (command != null) {
                command.help(out);
            } else {
                try {
                    resourceBundle.getString("top." + argument + ".1");
                    currentCategory = "top." + argument;
                    printCategory(out, currentCategory);
                    cmdman.grabInput(this);
                } catch (MissingResourceException mre) {
                    throw new CommandException(Bundle.getString("help.unknownCommand"));
                }
            }
        }
    }

    /**
     * Prints the strings for the named help category, preceeded by
     * the standard category header, and followed by the standard
     * category footer.
     *
     * @param  out       output to write messages to.
     * @param  category  help category to display.
     */
    protected void printCategory(Log out, String category) {
        int ii = 1;
        while (true) {
            try {
                String key = "header." + ii;
                String str = resourceBundle.getString(key);
                out.writeln(str);
            } catch (MissingResourceException mre) {
                break;
            }
            ii++;
        }
        out.writeln("");
        ii = 1;
        while (true) {
            try {
                String key = category + '.' + ii;
                String str = resourceBundle.getString(key);
                out.writeln(str);
            } catch (MissingResourceException mre) {
                break;
            }
            ii++;
        }
        out.writeln("");
        printFooter(out, category);
    }

    /**
     * Prints the standard category footer, along with the given
     * category name in square brackets (e.g. "[cat]:").
     *
     * @param  out       Output to write messages to.
     * @param  category  Help category to display.
     */
    protected void printFooter(Log out, String category) {
        int ii = 1;
        while (true) {
            try {
                String key = "footer." + ii;
                String str = resourceBundle.getString(key);
                out.writeln(str);
            } catch (MissingResourceException mre) {
                break;
            }
            ii++;
        }
        out.write("[");
        out.write(category);
        out.writeln("]:");
    }

    /**
     * Called by the CommandManager when new input has been received
     * from the user. This asynchronously follows a call to
     * <code>CommandManager.grabInput()</code>
     *
     * @param  session  JSwat session on which to operate.
     * @param  out      Output to write messages to.
     * @param  cmdman   CommandManager that's calling us.
     * @param  input    Input from user.
     */
    public void receiveInput(Session session, Log out, CommandManager cmdman, String input) {
        input = input.trim();
        if (input.equals("u")) {
            handleUp(out, cmdman);
        } else if (input.equals("m")) {
            printCategory(out, currentCategory);
            cmdman.grabInput(this);
        } else if (input.equals("q")) {
            out.writeln(Bundle.getString("help.doneInteractive"));
        } else {
            handleCategorySelection(out, cmdman, input);
        }
    }
}
