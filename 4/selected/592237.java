package com.walsai.mxbot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jibble.pircbot.*;

public class MXBot extends PircBot {

    public MXBot() throws Exception {
        this.setName("mxbot");
        this.setVerbose(true);
        this.connect("irc.oftc.net");
        this.joinChannel("#chino.org.mx");
    }

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.startsWith(".")) {
            message = message.trim();
            message = message.replaceAll("\\s{2,}", " ");
            String[] commandAndArgs = message.substring(1).split(" ");
            try {
                commandAndArgs[0] = commandAndArgs[0].substring(0, 1).toUpperCase() + commandAndArgs[0].substring(1).toLowerCase();
                Module m = (Module) Class.forName("com.walsai.mxbot.modules." + commandAndArgs[0]).newInstance();
                m.setArgs(commandAndArgs);
                m.setBot(this);
                m.setChannel(channel);
                m.setSender(sender);
                m.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void onServerResponse(int code, String response) {
        if (code == 311 || code == 338) {
            Pattern p = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
            Matcher m = p.matcher(response);
            String ip = "";
            if (m.find()) {
                ip = m.group();
                String user = response.split(" ")[1];
                if (!ip.equals("207.192.75.252")) {
                    this.onMessage(this.getChannels()[0], "", "", "", ".ipseeker " + user + " " + ip);
                }
            }
        }
    }
}
