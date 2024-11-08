package edu.simplemqom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Adam
 *
 */
public class CommandLine {

    private BufferedReader keyboard;

    private String command[] = null;

    private int commandNumber[];

    private boolean running = true;

    public CommandLine() {
        keyboard = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Console active.\n" + "Type 'help' for list of commands");
    }

    public boolean run() {
        while (running) {
            try {
                command = splitCommands(getInput());
            } catch (IOException e) {
                e.printStackTrace();
            }
            commandNumber = new int[command.length];
            commandNumber[0] = convert(0);
            functionChooser();
        }
        return false;
    }

    private String getInput() throws IOException {
        System.out.print(">");
        return keyboard.readLine();
    }

    private static String[] splitCommands(String cmd) {
        String[] commands = cmd.trim().split("\\s");
        return commands;
    }

    private int convert(int i) {
        for (Commands command : Commands.values()) {
            if (this.command[i].toUpperCase().equals(command.toString())) {
                return command.ordinal();
            }
        }
        return Commands.NULL.ordinal();
    }

    private void functionChooser() {
        switch(Commands.values()[commandNumber[0]]) {
            case QUIT:
                System.out.println("Console closed...");
                running = false;
                break;
            case HELP:
                help();
                break;
            case VIEWCHANNEL:
                aboutChannel();
                break;
            case MESSAGEBROKER:
                aboutMessageBroker();
                break;
            case NULL:
                System.out.println("Command '" + command[0].toString() + "' unrecognized, please try again.");
            default:
        }
    }

    private void help() {
        if (command.length == 1) {
            System.out.println("viewchannel - lists general" + " information about channel," + " please type help viewchannel for details.");
            System.out.println("messagebroker - lists general information about brokerworkers and opened channels");
            System.out.println("help - provides help about specific commands, to view information about specific command" + "type 'help command'");
            System.out.println("quit - terminates console and whole application");
        } else if (command.length == 2) {
            commandNumber[1] = convert(1);
            switch(Commands.values()[commandNumber[1]]) {
                case HELP:
                    System.out.println("This command provides list of " + "commands avaliable to invoke by the user.");
                    break;
                case VIEWCHANNEL:
                    System.out.println("viewchannel" + "\nProvides general " + "information about channels, " + "due to the argument given. \nExamples:");
                    System.out.println("- viewchannel - provides a list of open channels.");
                    System.out.println("- viewchannel channelName - provides detailed information " + "about channel with the name given, if that channel exists.");
                    break;
                case MESSAGEBROKER:
                    System.out.println("messagebroker" + "\nProvides general information about whole message broker " + "lists open channels list with number of queues inside.");
                    break;
                case QUIT:
                    System.out.println("quit" + "\nTerminates console and message broker application.");
                    break;
                case NULL:
                    System.out.println("No help provided about " + "'" + command[1].toString() + "'");
            }
        } else {
            System.out.println("To many arguments!");
        }
    }

    private void aboutChannel() {
        MessageBrokerController controller = MessageBrokerController.getInstance();
        if (command.length == 1) {
            Set<String> channels = controller.getChannelSet();
            Iterator<String> iterator = channels.iterator();
            System.out.println("===========CHANNELS===========\n");
            while (iterator.hasNext()) {
                String channelName = iterator.next().toString();
                System.out.println(channelName + " queues: " + controller.get(channelName).getQueueSet().size());
            }
            System.out.println("\n=========CHANNELS END=========\n");
            System.out.println("\nTo obtain detailed information about channel please type 'viewchannel channelName'");
        } else if (command.length == 2 && channelCheck()) {
            System.out.println(controller.get(command[1]).getName() + " channel contains:\n");
            System.out.println("Channel workers number: " + controller.get(command[1]).getWorkersPoolSize());
            Set<String> queues = controller.get(command[1]).getQueueSet();
            System.out.println("Channel logger: " + controller.get(command[1]).getLogger().getName() + "\n");
            Iterator<String> iterator = queues.iterator();
            System.out.println("===========QUEUES===========\n");
            while (iterator.hasNext()) {
                String queueName = iterator.next().toString();
                System.out.println(queueName + "\nmessages: " + controller.get(command[1]).getQueue(queueName).getCurrentLength() + "\nmax massages possible: " + controller.get(command[1]).getQueue(queueName).getMaxLength() + "\nqueue type: " + controller.get(command[1]).getQueue(queueName).getType() + "\n");
            }
            System.out.println("=========QUEUES END=========\n");
        } else System.out.println("Wrong channel name. Please try again.");
    }

    public void aboutMessageBroker() {
        System.out.println("Client Handlers maximum number: " + MessageBrokerController.getWorkersPoolSize());
        command[0] = "viewChannel";
        System.out.println("Channel list:");
        aboutChannel();
    }

    public boolean channelCheck() {
        MessageBrokerController controller = MessageBrokerController.getInstance();
        Set<String> channels = controller.getChannelSet();
        Iterator<String> iterator = channels.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(command[1])) {
                return true;
            }
        }
        return false;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine();
        cmd.run();
    }
}

;
