import java.io.*;
import java.util.*;
import java.text.*;
import borknet_services.core.*;

/**
 * Class to load configuration files.
 * @author Ozafy - ozafy@borknet.org - http://www.borknet.org
 */
public class Clearchan implements Command {

    /**
     * Constructs a Loader
     * @param debug		If we're running in debug.
     */
    public Clearchan() {
    }

    public void parse_command(Core C, Q Bot, DBControl dbc, String numeric, String botnum, String target, String username, String params) {
        String user[] = dbc.getUserRow(username);
        if (user[4].equals("0")) {
            C.cmd_notice(numeric, botnum, username, "You are not AUTH'd.");
            return;
        }
        String auth[] = dbc.getAuthRow(user[4]);
        if (user[5].equals("1") && Integer.parseInt(auth[3]) > 899) {
            try {
                String result[] = params.split("\\s");
                String channel = result[1];
                String t = result[2];
                if (!t.startsWith("#")) throw new ArrayIndexOutOfBoundsException();
                int type = Integer.parseInt(t.substring(1));
                if (type < 1 || type > 4) throw new NumberFormatException();
                String reason = channel + " cleared by " + auth[0] + ".";
                if (result.length > 3) {
                    reason = result[3];
                    for (int m = 4; m < result.length; m++) {
                        reason += " " + result[m];
                    }
                }
                String users[] = dbc.getChannelUsers(channel);
                String userinfo[][] = new String[users.length][];
                String authinfo[][] = new String[users.length][];
                if (users[0].equals("0")) {
                    C.cmd_notice(numeric, botnum, username, "Channel doesn't exist.");
                    return;
                }
                for (int n = 0; n < users.length; n++) {
                    userinfo[n] = dbc.getUserRow(users[n]);
                    authinfo[n] = dbc.getAuthRow(userinfo[n][4]);
                }
                switch(type) {
                    case 1:
                        if (dbc.chanExists(channel)) {
                            dbc.delChan(channel);
                            C.cmd_part(numeric, botnum, channel, reason);
                        }
                        for (int n = 0; n < users.length; n++) {
                            if (Integer.parseInt(authinfo[n][3]) < 2) {
                                C.cmd_kick(numeric, channel, users[n], reason);
                            }
                        }
                        break;
                    case 2:
                        if (dbc.chanExists(channel)) {
                            dbc.delChan(channel);
                            C.cmd_part(numeric, botnum, channel, reason);
                        }
                        for (int n = 0; n < users.length; n++) {
                            if (Integer.parseInt(authinfo[n][3]) < 2) {
                                C.cmd_dis(numeric, users[n], reason);
                            }
                        }
                        break;
                    case 3:
                        if (dbc.chanExists(channel)) {
                            dbc.setChanField(channel, 7, "true");
                            C.cmd_part(numeric, botnum, channel, reason);
                        }
                        for (int n = 0; n < users.length; n++) {
                            if (Integer.parseInt(authinfo[n][3]) < 2) {
                                dbc.addGline("*!" + userinfo[n][2], C.get_time(), "7200", reason, auth[0]);
                            }
                        }
                        break;
                    case 4:
                        if (dbc.chanExists(channel)) {
                            dbc.setChanField(channel, 7, "true");
                            C.cmd_part(numeric, botnum, channel, reason);
                        }
                        for (int n = 0; n < users.length; n++) {
                            if (Integer.parseInt(authinfo[n][3]) < 2) {
                                if (!userinfo[n][7].equals("0.0.0.0")) {
                                    dbc.addGline("*!*@" + userinfo[n][7], C.get_time(), "7200", reason, auth[0]);
                                } else {
                                    dbc.addGline("*!*" + userinfo[n][2].substring(userinfo[n][2].indexOf("@")), C.get_time(), "7200", reason, auth[0]);
                                }
                            }
                        }
                        break;
                }
                C.report(auth[0] + " is clearing " + channel + " with type #" + type + ".");
                C.cmd_notice(numeric, botnum, username, "Done.");
                return;
            } catch (NumberFormatException n) {
                C.cmd_notice(numeric, botnum, username, "#type should be a number between 1 and 4");
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                C.cmd_notice(numeric, botnum, username, "/msg " + Bot.get_nick() + " clearchan <#channel> <#type> [reason]");
                return;
            }
        } else {
            C.cmd_notice(numeric, botnum, username, "This command is either unknown, or you need to be opered up to use it.");
            return;
        }
    }

    public void parse_help(Core C, Q Bot, String numeric, String botnum, String username, int lev) {
        if (lev > 899) {
            String nick = Bot.get_nick();
            C.cmd_notice(numeric, botnum, username, "/msg " + nick + " clearchan <#channel> <#type> [reason]");
            C.cmd_notice(numeric, botnum, username, "Available types are:");
            C.cmd_notice(numeric, botnum, username, "1: Kick all users and remove " + nick + ".");
            C.cmd_notice(numeric, botnum, username, "2: Kill all users and remove " + nick + ".");
            C.cmd_notice(numeric, botnum, username, "3: G-Line all users and suspend " + nick + " from the channel (user@host).");
            C.cmd_notice(numeric, botnum, username, "4: G-Line all users and suspend " + nick + " from the channel (*@ip).");
            C.cmd_notice(numeric, botnum, username, "G-Line duration is 2 hours.");
            C.cmd_notice(numeric, botnum, username, "Default reason is \"#channel cleared by nick\".");
        } else {
            C.cmd_notice(numeric, botnum, username, "This command is either unknown, or you need to be opered up to use it.");
        }
    }

    public void showcommand(Core C, Q Bot, String numeric, String botnum, String username, int lev) {
        if (lev > 899) {
            C.cmd_notice(numeric, botnum, username, "CLEARCHAN           Clear a channel. - level 900.");
        }
    }
}
