package org.timothyb89.jtelirc.karma;

import java.io.File;
import org.timothyb89.jtelirc.listener.command.Command;
import org.timothyb89.jtelirc.message.UserMessage;
import org.timothyb89.jtelirc.server.Server;

/**
 *
 * @author tim
 */
public class KarmaCommand extends Command {

    private KarmaManager manager;

    public KarmaCommand(Server server) {
        super(server);
        manager = new KarmaManager();
        server.addListener(manager);
    }

    @Override
    public String getCommand() {
        return "!karma";
    }

    @Override
    public void execute(UserMessage message, String args) {
        KarmaListener l = manager.getListener(message.getChannel());
        if (l == null) {
            System.err.println("null karma listener");
            return;
        }
        if (args.equalsIgnoreCase("reload")) {
            long ctime = System.currentTimeMillis();
            File dbf = l.getDatabaseFile();
            l.loadDatabase(dbf);
            long ntime = System.currentTimeMillis();
            double dsecs = (((double) ntime - (double) ctime) / 1000d);
            message.reply("Reloaded karma database for " + message.getChannel().getName() + " in " + dsecs + " seconds");
        } else {
            KarmaEntry entry = l.getDatabase().getEntry(args);
            if (entry == null) {
                message.reply("'" + args + "' has no karma.");
            } else {
                int kpp = (int) entry.getKarmaPercentPositive();
                message.reply("'" + args + "' has " + entry.getKarma() + " karma (" + entry.getTotal() + " total, " + kpp + "% positive)");
            }
        }
    }
}
