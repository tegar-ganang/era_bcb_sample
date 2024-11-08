import java.io.*;
import java.util.Vector;

public class FibsCommandProcessor {

    private static final int CMDQSIZE = 100;

    private FibsCommand[] commands;

    private int numberCommands;

    private static String infoAboutComing = null;

    public FibsCommandProcessor() {
        numberCommands = 0;
        commands = new FibsCommand[CMDQSIZE];
    }

    public void clearQueue() {
        numberCommands = 0;
        for (int i = 0; i <= numberCommands; i++) {
            commands[i] = null;
        }
        infoAboutComing = null;
    }

    private void pushCommand(FibsCommand c) {
        for (int i = 0; i <= numberCommands; i++) {
            System.out.println("\t\tc[" + i + "]=" + commands[i]);
        }
        System.out.flush();
        commands[numberCommands++] = c;
    }

    private void popCommand() {
        numberCommands--;
        int i = 0;
        for (i = 0; i < numberCommands; i++) {
            commands[i] = commands[i + 1];
        }
        commands[++i] = null;
    }

    public String ask(String fromWho, String aboutWho) {
        pushCommand((FibsCommand) new AskFibsCommand(fromWho, aboutWho));
        return (null);
    }

    public String list(String fromWho, String aboutWho) {
        pushCommand((FibsCommand) new ListFibsCommand(fromWho, aboutWho));
        return (null);
    }

    public String complain(String fromWho, String aboutWho) {
        pushCommand((FibsCommand) new ComplainFibsCommand(fromWho, aboutWho));
        return (null);
    }

    public String vouch(String fromWho, String aboutWho) {
        pushCommand((FibsCommand) new VouchFibsCommand(fromWho, aboutWho));
        return (null);
    }

    public String withdraw(String fromWho, String aboutWho) {
        pushCommand((FibsCommand) new WithdrawFibsCommand(fromWho, aboutWho));
        return (null);
    }

    public String information(String aboutWho) {
        infoAboutComing = aboutWho;
        return (null);
    }

    public String experience(String experience) {
        if (infoAboutComing == null) {
            return ("Unsolicited experience received queue resetting?\n");
        }
        if (numberCommands == -1) {
            return ("No command in queue to process information\n");
        }
        CommandResult result = ((FibsCommand) commands[0]).setInfo(infoAboutComing, experience);
        if (result.done) {
            this.popCommand();
            infoAboutComing = null;
        }
        return (result.message);
    }

    public String noInformation(String aboutWho) {
        if (infoAboutComing == null) {
            return ("Unsolicited whois failure received\n");
        }
        if (numberCommands == -1) {
            return ("No command in queue to process information\n");
        }
        CommandResult result = commands[0].setInfo(aboutWho, null);
        System.out.println("FCP:noInfo:res.msg=" + result.message);
        System.out.flush();
        System.out.println("FCP:noInfo:res.dun=" + result.done);
        System.out.flush();
        if (result.done) {
            this.popCommand();
            infoAboutComing = null;
        }
        return (result.message);
    }
}
