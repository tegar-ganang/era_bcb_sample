package org.shell.commands;

import java.util.HashSet;
import org.shell.commands.exceptions.CommandAlreadyLoadedException;
import org.shell.commands.exceptions.CommandNotAvailableException;

public class CommandsHolder {

    private static HashSet<Command> commands = new HashSet<Command>();

    public static void addCommand(Command command) throws CommandAlreadyLoadedException {
        addCommand(command, false);
    }

    public static void addCommand(Command command, boolean overwrite) throws CommandAlreadyLoadedException {
        if (command != null) {
            if (isCommandAvailable(command)) {
                if (overwrite) {
                    commands.remove(command);
                    System.out.println("Command: " + command.getCommandName() + " removed");
                } else {
                    throw new CommandAlreadyLoadedException(command.getCommandName());
                }
            }
            commands.add(command);
            System.out.println("Loaded command: '" + command.getCommandName() + "'");
        }
    }

    public static Command getCommand(Command input) throws CommandNotAvailableException {
        if (input != null) {
            for (Command loadedCommand : commands) {
                if (loadedCommand.equals(input)) {
                    return loadedCommand;
                }
            }
            throw new CommandNotAvailableException(input.getCommandName());
        }
        throw new CommandNotAvailableException("<command was 'null'>");
    }

    public static Boolean isCommandAvailable(Command command) {
        return commands.contains(command);
    }

    public static HashSet<Command> getCommands() {
        return commands;
    }

    public static void removeAll() {
        commands.clear();
    }
}
