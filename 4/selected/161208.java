package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Library;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.listeners.GetpxListener;
import com.chessclub.simulbot.objects.Advertiser;
import com.chessclub.simulbot.timers.NoManagerTimer;

/**
 * This class is designed to handle the "open" command.
 */
public class Open {

    private static int ACCESS_LEVEL = Settings.MANAGER;

    private static String lastTeller;

    private static final int GIVER_DELAY_TIME = 10;

    private static final String CHANNEL_3 = "3";

    private static final String CHANNEL_4 = "4";

    public static void open(Tell t) {
        int authorized = Common.isAuthorized(t);
        t.truncateFirstSpace();
        String simulGiver = Library.grabUptoFirstSpace(t.getMessage(true));
        lastTeller = t.getHandle();
        if (authorized < ACCESS_LEVEL) {
            SimulHandler.getHandler().tell(t.getHandle(), "You are not authorized to issue this command");
        } else if (SimulHandler.getStatus() != Settings.NO_SIMUL) {
            SimulHandler.getHandler().tell(t.getHandle(), "The simul could not be opened because another simul is currently in progress.");
        } else if (simulGiver.equals("") || simulGiver.equals(" ")) {
            if (SimulHandler.getGiver() == null) {
                SimulHandler.getHandler().tell(t.getHandle(), "The simul could not be opened because no simul giver was specified");
                return;
            } else {
                setupEventInfo();
            }
        } else {
            SimulHandler.getHandler().passGetpxToOpen();
            SimulHandler.getHandler().getpx(simulGiver);
        }
    }

    public static void handleGetpx() {
        if (!GetpxListener.isExisting()) {
            SimulHandler.getHandler().tell(lastTeller, "Could not open a simul for " + GetpxListener.getUser() + " because he or she doesn't exist.");
        } else {
            SimulHandler.setGiver(new Player(GetpxListener.getUser(), GetpxListener.getTitle(), GetpxListener.isLoggedOn(), GetpxListener.isPlaying(), GetpxListener.getRating()));
            SimulHandler.setManager(lastTeller);
            setupEventInfo();
        }
    }

    public static void setLastTeller(String newLastTeller) {
        lastTeller = newLastTeller;
    }

    public static void setupEventInfo() {
        SimulHandler.setStatus(Settings.SIMUL_OPEN);
        SimulHandler.getHandler().plusNotify(SimulHandler.getGiver().getHandle());
        SimulHandler.getHandler().qchanplus(SimulHandler.getGiver().getHandle(), SimulHandler.getChannel());
        SimulHandler.getHandler().tell(lastTeller, SimulHandler.getGiver().getHandle() + "'s simul is now open for joining.");
        SimulHandler.getHandler().tell(SimulHandler.getGiver().getHandle(), "A new simul has been opened with you as the giver.");
        if (SimulHandler.isNoManager()) {
            SimulHandler.getHandler().tell(SimulHandler.getGiver().getHandle(), "This simul has been setup without a manager. You will have to tell me \"start\" when you are ready to begin the simul.");
            NoManagerTimer.start(Settings.EVENTS_CHANNEL, GIVER_DELAY_TIME);
        }
        if (SimulHandler.getChannelOut()) {
            SimulHandler.getHandler().tell(CHANNEL_3, Common.buildSimulString());
            SimulHandler.getHandler().tell(CHANNEL_4, Common.buildSimulString());
            if (!(SimulHandler.getChannel().equals(CHANNEL_3) || SimulHandler.getChannel().equals(CHANNEL_4))) {
                SimulHandler.getHandler().tell(SimulHandler.getChannel(), Common.buildSimulString());
            }
            Alarm.sendAlarms();
        }
        setupAdvertising();
        if (!SimulHandler.getGiver().isLoggedOn()) {
            SimulHandler.getHandler().tell(lastTeller, "Simul giver \"" + SimulHandler.getGiver().getHandle() + "\" is not currently logged on.");
        } else if (SimulHandler.getGiver().isPlaying()) {
            SimulHandler.getHandler().tell(lastTeller, "Simul giver \"" + SimulHandler.getGiver().getHandle() + "\" is currently played a game.");
        }
    }

    public static void setupAdvertising() {
        String msg = Common.buildEventString(false);
        SimulHandler.getHandler().qaddevent(msg);
        if (SimulHandler.getChannelOut()) {
            Advertiser.start();
        } else {
            Advertiser.stop();
        }
    }

    public static void closeAdvertising() {
        Advertiser.stop();
        SimulHandler.getHandler().qremoveevent(Settings.EVENT_TYPE);
    }

    public static void disableAdvertising() {
        Advertiser.stop();
        SimulHandler.getHandler().qremoveevent(Settings.EVENT_TYPE);
    }

    public static void enableAdvertising() {
        Advertiser.start();
        SimulHandler.getHandler().qaddevent(Common.buildEventString(false));
    }
}
