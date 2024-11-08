package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Library;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.listeners.GetpxListener;
import com.chessclub.simulbot.objects.Advertiser;
import com.chessclub.simulbot.objects.Qtell;

/**
 * This class is designed to handle the "set" command.
 */
public class Set {

    private static int ACCESS_LEVEL = Settings.SIMUL_GIVER;

    private static String lastTeller;

    public static final int GIVER_CODE = 0;

    public static final int MANAGER_CODE = 1;

    public static void set(Tell t) {
        int authorized = Common.isAuthorized(t);
        if (authorized < ACCESS_LEVEL) {
            SimulHandler.getHandler().tell(t.getHandle(), "You are not authorized to issue this command");
            return;
        }
        t.truncateFirstSpace();
        String command = Library.grabUptoFirstSpace(t.getMessage(true));
        t.truncateFirstSpace();
        String argument = Library.grabUptoFirstSpace(t.getMessage(true));
        Qtell qtell = new Qtell(SimulHandler.getChannel());
        if (command.equals("giver")) {
            lastTeller = t.getHandle();
            SimulHandler.getHandler().passGetpxToSet(GIVER_CODE);
            SimulHandler.getHandler().getpx(argument);
        } else if (command.equals("rated")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setRated(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"rated\" was set to \"" + SimulHandler.isRated() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"rated\" was set to \"" + SimulHandler.isRated() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"rated\" to \"" + argument + "\".");
            }
        } else if (command.equals("white")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setGiverWhite(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"white\" was set to \"" + SimulHandler.isGiverWhite() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"white\" was set to \"" + SimulHandler.isGiverWhite() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"white\" to \"" + argument + "\".");
            }
        } else if (command.equals("allowguests")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setAllowGuests(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"allowGuests\" was set to \"" + SimulHandler.getAllowGuests() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"allowGuests\" was set to \"" + SimulHandler.getAllowGuests() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"allowGuests\" to \"" + argument + "\".");
            }
        } else if (command.equals("allowcomps")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setAllowComps(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"allowComps\" was set to \"" + SimulHandler.getAllowComps() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"allowComps\" was set to \"" + SimulHandler.getAllowComps() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"allowComps\" to \"" + argument + "\".");
            }
        } else if (command.equals("maxrating")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setMaxRating(Integer.parseInt(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"maxRating\" was set to \"" + SimulHandler.getMaxRating() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"maxRating\" was set to \"" + SimulHandler.getMaxRating() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"maxRating\" to \"" + argument + "\".");
            }
        } else if (command.equals("maxplayers")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setMaxPlayers(Integer.parseInt(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"maxPlayers\" was set to \"" + SimulHandler.getMaxPlayers() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"maxPlayers\" was set to \"" + SimulHandler.getMaxPlayers() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"maxPlayers\" to \"" + argument + "\".");
            }
        } else if (command.equals("time")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setTime(argument);
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"time\" was set to \"" + SimulHandler.getTime() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"time\" was set to \"" + SimulHandler.getTime() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"time\" to \"" + argument + "\".");
            }
        } else if (command.equals("inc")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setInc(argument);
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"inc\" was set to \"" + SimulHandler.getInc() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"inc\" was set to \"" + SimulHandler.getInc() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"inc\" to \"" + argument + "\".");
            }
        } else if (command.equals("wild")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setWild(argument);
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"wild\" was set to \"" + SimulHandler.getWild() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"wild\" was set to \"" + SimulHandler.getWild() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"wild\" to \"" + argument + "\".");
            }
        } else if (command.equals("channelout")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setChannelOut(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"channelOut\" was set to \"" + SimulHandler.getChannelOut() + "\".");
                if (SimulHandler.getChannelOut() && SimulHandler.getStatus() == 1) {
                    Advertiser.start();
                } else {
                    Advertiser.stop();
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"channelOut\" to \"" + argument + "\".");
            }
        } else if (command.equals("messwinners")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setMessWinners(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"messWinners\" was set to \"" + SimulHandler.getMessWinners() + "\".");
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"messWinners\" to \"" + argument + "\".");
            }
        } else if (command.equals("nomanager")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setNoManager(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"noManager\" was set to \"" + SimulHandler.isNoManager() + "\".");
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"noManager\" to \"" + argument + "\".");
            }
        } else if (command.equals("randcolor")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setRandColor(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"randColor\" was set to \"" + SimulHandler.isRandColor() + "\".");
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"randColor\" to \"" + argument + "\".");
            }
        } else if (command.equals("autoadjud")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setAutoAdjud(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"autoAdjud\" was set to \"" + SimulHandler.isAutoAdjud() + "\".");
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"autoAdjud\" to \"" + argument + "\".");
            }
        } else if (command.equals("lottery")) {
            if (Library.isValidBoolean(argument)) {
                SimulHandler.setLottery(Boolean.parseBoolean(argument));
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"lottery\" was set to \"" + SimulHandler.isLottery() + "\".");
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"lottery\" to \"" + argument + "\".");
            }
        } else if (command.equals("channel")) {
            if (Library.isValidInt(argument)) {
                SimulHandler.setChannel(argument);
                SimulHandler.getHandler().tell(t.getHandle(), "Var \"channel\" was set to \"" + SimulHandler.getChannel() + "\".");
                if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                    qtell.addLine("Var \"channel\" was set to \"" + SimulHandler.getChannel() + "\".");
                }
            } else {
                SimulHandler.getHandler().tell(t.getHandle(), "Cannot set \"channel\" to \"" + argument + "\".");
            }
        } else if (command.equals("manager")) {
            lastTeller = t.getHandle();
            SimulHandler.getHandler().passGetpxToSet(MANAGER_CODE);
            SimulHandler.getHandler().getpx(argument);
        } else {
            SimulHandler.getHandler().tell(t.getHandle(), "I do not have a var \"" + command + "\". Please try again, or tell me \"help vars\" for more information");
        }
        qtell.send();
    }

    public static void handleGetpxForGiver() {
        if (!GetpxListener.isExisting()) {
            SimulHandler.getHandler().tell(lastTeller, GetpxListener.getUser() + " could not be set as the simul giver because he or she doesn't exist");
        } else {
            SimulHandler.setGiver(new Player(GetpxListener.getUser(), GetpxListener.getTitle(), GetpxListener.isLoggedOn(), GetpxListener.isPlaying(), GetpxListener.getRating()));
            SimulHandler.getHandler().qtell(lastTeller, "Var \"giver\" was set to \"" + SimulHandler.getGiver().getHandle() + "\".");
            if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                Qtell qtell = new Qtell(SimulHandler.getChannel());
                qtell.addLine("Var \"giver\" was set to \"" + SimulHandler.getGiver().getHandle() + "\".");
                qtell.send();
            }
            if (!SimulHandler.getGiver().isLoggedOn()) {
                SimulHandler.getHandler().tell(lastTeller, "Simul giver \"" + SimulHandler.getGiver().getHandle() + "\" is not currently logged on.");
            } else if (SimulHandler.getStatus() == Settings.SIMUL_OPEN) {
                SimulHandler.getHandler().tell(SimulHandler.getGiver().getHandle(), "You have been set as the simul giver.");
            }
        }
    }

    public static void handleGetpxForManager() {
        if (!GetpxListener.isExisting()) {
            SimulHandler.getHandler().tell(lastTeller, GetpxListener.getUser() + " could not be set as the simul manager because he or she doesn't exist");
        } else {
            SimulHandler.setManager(GetpxListener.getUser().toLowerCase());
            SimulHandler.getHandler().qtell(lastTeller, "Var \"manager\" was set to \"" + SimulHandler.getManager() + "\".");
            if (SimulHandler.getStatus() == Settings.SIMUL_OPEN && SimulHandler.getChannelOut()) {
                Qtell qtell = new Qtell(SimulHandler.getChannel());
                qtell.addLine("Var \"manager\" was set to \"" + SimulHandler.getManager() + "\".");
                qtell.send();
            }
            if (!GetpxListener.isLoggedOn()) {
                SimulHandler.getHandler().tell(lastTeller, "Simul manager \"" + SimulHandler.getManager() + "\" is not currently logged on.");
            } else {
                SimulHandler.getHandler().tell(SimulHandler.getManager(), "You have been set as the simul manager.");
            }
        }
    }
}
