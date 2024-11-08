package org.shell.commands.exceptions;

public class CommandAlreadyLoadedException extends Exception {

    public CommandAlreadyLoadedException(String commandName) {
        super("Command was already loaded: " + commandName + "\nUse overwrite parameter");
    }
}
