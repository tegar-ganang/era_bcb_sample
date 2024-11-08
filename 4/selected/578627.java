package tk.bot;

public class BotWhoCommand extends BotBasicInterpreter implements BotCommandInterpreter {

    TKBot bot;

    BotUserDisplay users;

    boolean checking = false;

    boolean starting = false;

    public BotWhoCommand(TKBot bot) {
        this.bot = bot;
        this.users = bot.getGUI().getUserDisplay();
    }

    public void commandPerformed(CommandEvent e) {
        String command = e.getCommand();
        if (e.getType() instanceof BotInfoType) {
            int first = command.indexOf("\"") + 1;
            int last = command.lastIndexOf("\"");
            String content = command.substring(first, last);
            if (checking) {
                if (content.equals("Users in channel " + users.getChannel() + ":")) {
                    starting = true;
                }
            }
        }
    }

    public void update() {
        checking = true;
        bot.addCommand("/who");
    }

    public void install(TKBot bot) {
        (new BotInfoType(bot)).addBotCommandInterpreter(this);
    }

    public void remove(TKBot bot) {
        (new BotInfoType(bot)).removeBotCommandInterpreter(this);
    }

    public boolean isInstalled(TKBot bot) {
        return (new BotInfoType(bot)).containsInterpreter(this);
    }
}
