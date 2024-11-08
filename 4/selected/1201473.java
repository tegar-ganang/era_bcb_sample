package tk.bot;

import java.awt.*;
import java.awt.event.*;

public class BotSimpleGUI extends Frame implements BotGUI, ActionListener {

    Label send = new Label("Send: ");

    Label commands = new Label("Commands: ");

    TextField field = new TextField(20);

    TextField command = new TextField(20);

    TKBot bot;

    BotTextAdapter display = new BotTextAdapter();

    DefaultInterpreters interpreters;

    public BotSimpleGUI(TKBot bot) {
        super("TK");
        this.bot = bot;
    }

    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        action = BotUtilities.replaceVars(action, bot.getUserTable());
        if (e.getSource().equals(command)) {
            command.setText("");
            if (action.equals("connect")) {
                bot.connect();
            }
            if (action.equals("iconize")) {
                dispose();
                new BotMinimizer(bot);
            }
            if (action.equals("config")) {
                BotManagerEditor editor = new BotManagerEditor("config.bot");
                editor.showFrame();
            }
            if (action.equals("exit")) {
                dispose();
                System.exit(0);
            }
            if (action.equals("disconnect")) {
                bot.disconnect();
            }
            if (action.equals("swing")) {
                bot.setGUI(new BotSwingGUI(bot));
                this.dispose();
            }
            if (action.equals("memory")) {
                Runtime r = Runtime.getRuntime();
                long totalMem = r.totalMemory() / 1024;
                long freeMem = r.freeMemory() / 1024;
                long usedMem = totalMem - freeMem;
                System.out.println("Total Memory: " + totalMem + " kb");
                System.out.println("Free Memory: " + freeMem + " kb");
                System.out.println("Used Memory: " + usedMem + " kb");
            }
            if (action.equals("help")) {
                String[] helpStrings = { "The following bot commands may be used in the command window", "connect\ticonize\tconfig", "exit\tdisconnect\tmemory" };
                for (int i = 0; i < helpStrings.length; i++) {
                    System.out.println(helpStrings[i]);
                }
            }
            if (action.startsWith("activate")) {
                String[] info = BotUtilities.parse(action, " ");
                for (int i = 0; i < bot.commands.length; i++) {
                    if (bot.commands[i].toString().equals(info[1])) {
                        if (info[2].equals("On")) {
                            bot.commands[i].remove(bot);
                            bot.commands[i].install(bot);
                            ((BotCustomCommand) bot.commands[i]).setEnabled(true);
                        }
                        if (info[2].equals("Off")) {
                            bot.commands[i].remove(bot);
                            ((BotCustomCommand) bot.commands[i]).setEnabled(false);
                        }
                    }
                }
            }
        }
        if (e.getSource().equals(field)) {
            field.setText("");
            bot.addCommand(action);
        }
    }

    public void remove(tk.bot.TKBot bot) {
        bot.removeGUI(this);
        interpreters.remove(bot);
    }

    public boolean isInstalled(tk.bot.TKBot bot) {
        if (bot.getGUI() != null) return bot.getGUI().equals(this);
        return false;
    }

    public void install(tk.bot.TKBot bot) {
        bot.autoLogOn = true;
        bot.rawMode = true;
        bot.setGUI(this);
        interpreters = new DefaultInterpreters(bot);
        interpreters.install(bot);
    }

    public boolean useStandard() {
        return false;
    }

    public boolean equals(Object object) {
        return (object instanceof BotSimpleGUI);
    }

    public BotGUI set(TKBot bot) {
        return new BotSimpleGUI(bot);
    }

    public void removeControlCommands() {
    }

    public void init() {
        pack();
        show();
    }

    public BotUserDisplay getUserDisplay() {
        return new BotUserAdapter();
    }

    public BotTextDisplay getEntryArea() {
        return new BotTextWrapper(field);
    }

    public BotTextDisplay getDisplayArea() {
        return display;
    }

    public BotCommandDisplay getCommandDisplay() {
        return new BotCommandAdapter();
    }

    public void appendLine(String line) {
        getDisplayArea().append(line + "\r\n");
    }

    public void append(String line) {
        getDisplayArea().append(line);
    }

    public void append(char c) {
        getDisplayArea().append(c + "");
    }

    public void addGUI() {
        this.setLayout(new GridLayout(2, 2));
        field.addActionListener(this);
        command.addActionListener(this);
        add(send);
        add(field);
        add(commands);
        add(command);
        addWindowListener(new WindowHandler());
    }

    public void addControlCommands() {
    }

    class BotTextAdapter implements BotTextDisplay {

        public String getText() {
            return "";
        }

        public void setText(String line) {
        }

        public void append(String line) {
            System.out.print(line);
        }

        public void clear() {
        }
    }

    class WindowHandler extends WindowAdapter {

        public void windowClosing(WindowEvent e) {
            dispose();
            if (bot.getGUI() instanceof BotSimpleGUI) System.exit(0);
        }
    }

    class BotUserAdapter implements BotUserDisplay {

        public void addUser(BotUser user) {
        }

        public void removeAll() {
        }

        public void removeUser(BotUser user) {
        }

        public void setChannel(String name) {
        }

        public String getChannel() {
            return "none";
        }
    }

    class BotCommandAdapter implements BotCommandDisplay {

        public void addModule(BotModule com) {
        }

        public void removeModule(BotModule com) {
        }

        public BotModule[] getModules() {
            return new BotModule[0];
        }

        public void addModules(BotModule[] mods) {
        }

        public void setModules(BotModule[] mods) {
        }
    }
}
