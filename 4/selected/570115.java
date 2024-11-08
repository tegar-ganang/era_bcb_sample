package erki.abcpeter.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import erki.abcpeter.BotInterface;
import erki.abcpeter.Parser;
import erki.abcpeter.bots.ErkiTalkBot;
import erki.abcpeter.bots.IrcBot;
import erki.abcpeter.bots.IrcBotWithMultipleChannelSupport;
import erki.abcpeter.msgs.observers.TextMessageObserver;
import erki.abcpeter.msgs.receive.TextMessage;
import erki.abcpeter.msgs.response.RawResponseMessage;
import erki.abcpeter.msgs.response.ResponseMessage;
import erki.abcpeter.util.Logger;

/**
 * This bot can give you op status on an irc channel. It cannot be used with
 * ErkiTalk as ErkiTalk has no ops. If you mistakenly try to load this
 * {@link Parser} with an {@link ErkiTalkBot} it will deactivate itself and log
 * a warning but nothing serious will happen.
 * 
 * @author Edgar Kalkowski
 */
public class GiveOp implements Parser, TextMessageObserver {

    private static final String OPERS_FILE = "config" + System.getProperty("file.separator") + "opers.txt";

    private IrcBot bot;

    private IrcBotWithMultipleChannelSupport bot2;

    LinkedList<String> opers;

    @Override
    public void init(BotInterface bot) {
        if (bot instanceof IrcBot) {
            this.bot = (IrcBot) bot;
            bot.register(this);
        } else if (bot instanceof IrcBotWithMultipleChannelSupport) {
            bot2 = (IrcBotWithMultipleChannelSupport) bot;
            bot.register(this);
        } else {
            Logger.warning(this, "GiveOp parser deactivated as it can only " + "be used with irc!");
        }
    }

    @Override
    public LinkedList<ResponseMessage> inform(TextMessage message) {
        String giveOp;
        if (bot != null) {
            giveOp = "(" + bot.getName() + "|" + bot.getName().toLowerCase() + ")[:,]? ?([gG]i(b|ve|pp?) ?[oO]pp?!?!?|[oO]p!!?!?)";
        } else {
            giveOp = "(" + bot2.getName() + "|" + bot2.getName().toLowerCase() + ")[:,]? ?([gG]i(b|ve|pp?) ?[oO]pp?!?!?|[oO]p!!?!?)";
        }
        if (message.getText().matches(giveOp)) {
            loadOpers();
            if (bot != null) {
                if (opers.contains(message.getName())) {
                    bot.send(new RawResponseMessage("MODE " + bot.getChannel() + " +o " + message.getName(), 100, 1000));
                } else {
                    bot.send(new ResponseMessage("Noe.", 100, 1000));
                }
            } else {
                if (opers.contains(message.getName())) {
                    bot2.send(new RawResponseMessage("MODE " + bot2.getChannel() + " +o " + message.getName(), 100, 1000));
                } else {
                    bot2.send(new ResponseMessage("Noe.", 100, 1000));
                }
            }
        }
        return null;
    }

    private void loadOpers() {
        try {
            BufferedReader fileIn = new BufferedReader(new FileReader(OPERS_FILE));
            String line;
            if ((line = fileIn.readLine()) != null) {
                opers = new LinkedList<String>();
                for (String s : line.split(",")) {
                    opers.add(s);
                }
            }
            Logger.info(this, "Opers for irc loaded: " + opers + ".");
        } catch (FileNotFoundException e) {
            opers = new LinkedList<String>();
            Logger.warning(this, "Opers file not found! Thus no one " + "will get op from me!");
        } catch (IOException e) {
            Logger.error(this, e);
            Logger.warning(this, "The opers file could not be loaded! Thus no " + "one will get op from me!");
            opers = new LinkedList<String>();
        }
    }
}
