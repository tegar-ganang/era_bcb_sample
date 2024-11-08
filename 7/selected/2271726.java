package soc.robot;

import soc.disableDebug.D;
import soc.game.SOCBoard;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCTradeOffer;
import soc.util.CutoffExceededException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

/**
 * Moved the routines that make and
 * consider offers out of the robot
 * brain.
 *
 * @author Robert S. Thomas
 */
public class SOCRobotNegotiator {

    protected static final int WIN_GAME_CUTOFF = 25;

    public static final int REJECT_OFFER = 0;

    public static final int ACCEPT_OFFER = 1;

    public static final int COUNTER_OFFER = 2;

    protected SOCRobotBrain brain;

    protected int strategyType;

    protected SOCGame game;

    protected Stack buildingPlan;

    protected HashMap playerTrackers;

    protected SOCPlayerTracker ourPlayerTracker;

    protected SOCPlayer ourPlayerData;

    protected SOCRobotDM decisionMaker;

    protected boolean[][] isSellingResource;

    protected boolean[][] wantsAnotherOffer;

    protected Vector offersMade;

    protected SOCPossiblePiece[] targetPieces;

    /**
     * constructor
     *
     * @param br  the robot brain
     */
    public SOCRobotNegotiator(SOCRobotBrain br) {
        brain = br;
        strategyType = br.getRobotParameters().getStrategyType();
        playerTrackers = brain.getPlayerTrackers();
        ourPlayerTracker = brain.getOurPlayerTracker();
        ourPlayerData = brain.getOurPlayerData();
        buildingPlan = brain.getBuildingPlan();
        decisionMaker = brain.getDecisionMaker();
        game = brain.getGame();
        isSellingResource = new boolean[SOCGame.MAXPLAYERS][SOCResourceConstants.MAXPLUSONE];
        resetIsSelling();
        wantsAnotherOffer = new boolean[SOCGame.MAXPLAYERS][SOCResourceConstants.MAXPLUSONE];
        resetWantsAnotherOffer();
        offersMade = new Vector();
        targetPieces = new SOCPossiblePiece[SOCGame.MAXPLAYERS];
        resetTargetPieces();
    }

    /**
     * reset target pieces for all players
     */
    public void resetTargetPieces() {
        D.ebugPrintln("*** resetTargetPieces ***");
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
            targetPieces[pn] = null;
        }
    }

    /**
     * set a target piece for a player
     *
     * @param pn  the player number
     * @param piece  the piece that they want to build next
     */
    public void setTargetPiece(int pn, SOCPossiblePiece piece) {
        targetPieces[pn] = piece;
    }

    /**
     * reset offers made
     */
    public void resetOffersMade() {
        offersMade.clear();
    }

    /**
     * add an offer to the offers made list
     *
     * @param offer  the offer
     */
    public void addToOffersMade(SOCTradeOffer offer) {
        if (offer != null) {
            offersMade.add(offer);
        }
    }

    /**
     * reset the isSellingResource array so that
     * if the player has the resource, then he is selling it
     */
    public void resetIsSelling() {
        D.ebugPrintln("*** resetIsSelling (true for every resource the player has) ***");
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                if (game.getPlayer(pn).getResources().getAmount(rsrcType) > 0) {
                    isSellingResource[pn][rsrcType] = true;
                }
            }
        }
    }

    /**
     * reset the wantsAnotherOffer array to all false
     */
    public void resetWantsAnotherOffer() {
        D.ebugPrintln("*** resetWantsAnotherOffer (all false) ***");
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                wantsAnotherOffer[pn][rsrcType] = false;
            }
        }
    }

    /**
     * mark a player as not selling a resource
     *
     * @param pn         the number of the player
     * @param rsrcType   the type of resource
     */
    public void markAsNotSelling(int pn, int rsrcType) {
        D.ebugPrintln("*** markAsNotSelling pn=" + pn + " rsrcType=" + rsrcType);
        isSellingResource[pn][rsrcType] = false;
    }

    /**
     * mark a player as willing to sell a resource
     *
     * @param pn         the number of the player
     * @param rsrcType   the type of resource
     */
    public void markAsSelling(int pn, int rsrcType) {
        D.ebugPrintln("*** markAsSelling pn=" + pn + " rsrcType=" + rsrcType);
        isSellingResource[pn][rsrcType] = true;
    }

    /**
     * mark a player as not wanting another offer
     *
     * @param pn         the number of the player
     * @param rsrcType   the type of resource
     */
    public void markAsNotWantingAnotherOffer(int pn, int rsrcType) {
        D.ebugPrintln("*** markAsNotWantingAnotherOffer pn=" + pn + " rsrcType=" + rsrcType);
        wantsAnotherOffer[pn][rsrcType] = false;
    }

    /**
     * mark a player as wanting another offer
     *
     * @param pn         the number of the player
     * @param rsrcType   the type of resource
     */
    public void markAsWantsAnotherOffer(int pn, int rsrcType) {
        D.ebugPrintln("*** markAsWantsAnotherOffer pn=" + pn + " rsrcType=" + rsrcType);
        wantsAnotherOffer[pn][rsrcType] = true;
    }

    /**
     * @return true if the player is marked as wanting a better offer
     *
     * @param pn         the number of the player
     * @param rsrcType   the type of resource
     */
    public boolean wantsAnotherOffer(int pn, int rsrcType) {
        return wantsAnotherOffer[pn][rsrcType];
    }

    /***
     * make an offer to another player
     *
     * @param targetPiece  the piece that we want to build
     * @return the offer we want to make, or null for no offer
     */
    public SOCTradeOffer makeOffer(SOCPossiblePiece targetPiece) {
        D.ebugPrintln("***** MAKE OFFER *****");
        if (targetPiece == null) {
            return null;
        }
        SOCTradeOffer offer = null;
        SOCResourceSet targetResources = null;
        switch(targetPiece.getType()) {
            case SOCPossiblePiece.CARD:
                targetResources = SOCGame.CARD_SET;
                break;
            case SOCPossiblePiece.ROAD:
                targetResources = SOCGame.ROAD_SET;
                break;
            case SOCPossiblePiece.SETTLEMENT:
                targetResources = SOCGame.SETTLEMENT_SET;
                break;
            case SOCPossiblePiece.CITY:
                targetResources = SOCGame.CITY_SET;
                break;
        }
        SOCResourceSet ourResources = ourPlayerData.getResources();
        D.ebugPrintln("*** targetResources = " + targetResources);
        D.ebugPrintln("*** ourResources = " + ourResources);
        if (ourResources.contains(targetResources)) {
            return offer;
        }
        if (ourResources.getAmount(SOCResourceConstants.UNKNOWN) > 0) {
            D.ebugPrintln("AGG WE HAVE UNKNOWN RESOURCES !!!! %%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            return offer;
        }
        SOCTradeOffer batna = getOfferToBank(targetResources);
        D.ebugPrintln("*** BATNA = " + batna);
        SOCBuildingSpeedEstimate estimate = new SOCBuildingSpeedEstimate(ourPlayerData.getNumbers());
        SOCResourceSet giveResourceSet = new SOCResourceSet();
        SOCResourceSet getResourceSet = new SOCResourceSet();
        int batnaBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
        D.ebugPrintln("*** batnaBuildingTime = " + batnaBuildingTime);
        if (batna != null) {
            batnaBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, batna.getGiveSet(), batna.getGetSet(), estimate);
        }
        D.ebugPrintln("*** batnaBuildingTime = " + batnaBuildingTime);
        int[] rollsPerResource = estimate.getRollsPerResource();
        int[] neededRsrc = new int[5];
        int[] notNeededRsrc = new int[5];
        int neededRsrcCount = 0;
        int notNeededRsrcCount = 0;
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            if (targetResources.getAmount(rsrcType) > 0) {
                neededRsrc[neededRsrcCount] = rsrcType;
                neededRsrcCount++;
            } else {
                notNeededRsrc[notNeededRsrcCount] = rsrcType;
                notNeededRsrcCount++;
            }
        }
        for (int j = neededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[neededRsrc[i]] > rollsPerResource[neededRsrc[i + 1]]) {
                    int tmp = neededRsrc[i];
                    neededRsrc[i] = neededRsrc[i + 1];
                    neededRsrc[i + 1] = tmp;
                }
            }
        }
        if (D.ebugOn) {
            for (int i = 0; i < neededRsrcCount; i++) {
                D.ebugPrintln("NEEDED RSRC: " + neededRsrc[i] + " : " + rollsPerResource[neededRsrc[i]]);
            }
        }
        for (int j = notNeededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[notNeededRsrc[i]] > rollsPerResource[notNeededRsrc[i + 1]]) {
                    int tmp = notNeededRsrc[i];
                    notNeededRsrc[i] = notNeededRsrc[i + 1];
                    notNeededRsrc[i + 1] = tmp;
                }
            }
        }
        if (D.ebugOn) {
            for (int i = 0; i < notNeededRsrcCount; i++) {
                D.ebugPrintln("NOT-NEEDED RSRC: " + notNeededRsrc[i] + " : " + rollsPerResource[notNeededRsrc[i]]);
            }
        }
        boolean[] someoneIsSellingResource = new boolean[SOCResourceConstants.MAXPLUSONE];
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            someoneIsSellingResource[rsrcType] = false;
            for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                if ((pn != ourPlayerData.getPlayerNumber()) && (isSellingResource[pn][rsrcType])) {
                    someoneIsSellingResource[rsrcType] = true;
                    D.ebugPrintln("*** player " + pn + " is selling " + rsrcType);
                    break;
                }
            }
        }
        int getRsrcIdx = neededRsrcCount - 1;
        while ((getRsrcIdx >= 0) && ((ourResources.getAmount(neededRsrc[getRsrcIdx]) >= targetResources.getAmount(neededRsrc[getRsrcIdx])) || (!someoneIsSellingResource[neededRsrc[getRsrcIdx]]))) {
            getRsrcIdx--;
        }
        if (getRsrcIdx >= 0) {
            D.ebugPrintln("*** getRsrc = " + neededRsrc[getRsrcIdx]);
            getResourceSet.add(1, neededRsrc[getRsrcIdx]);
            D.ebugPrintln("*** offer should be null : offer = " + offer);
            int giveRsrcIdx = 0;
            while ((giveRsrcIdx < notNeededRsrcCount) && (offer == null)) {
                D.ebugPrintln("*** ourResources.getAmount(" + notNeededRsrc[giveRsrcIdx] + ") = " + ourResources.getAmount(notNeededRsrc[giveRsrcIdx]));
                if (ourResources.getAmount(notNeededRsrc[giveRsrcIdx]) > 0) {
                    giveResourceSet.clear();
                    giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx]);
                    offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                    D.ebugPrintln("*** offer = " + offer);
                    int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                }
                giveRsrcIdx++;
            }
            D.ebugPrintln("*** ourResources = " + ourResources);
            if (offer == null) {
                int giveRsrcIdx1 = 0;
                while ((giveRsrcIdx1 < neededRsrcCount) && (offer == null)) {
                    D.ebugPrintln("*** ourResources.getAmount(" + neededRsrc[giveRsrcIdx1] + ") = " + ourResources.getAmount(neededRsrc[giveRsrcIdx1]));
                    D.ebugPrintln("*** targetResources.getAmount(" + neededRsrc[giveRsrcIdx1] + ") = " + targetResources.getAmount(neededRsrc[giveRsrcIdx1]));
                    if ((ourResources.getAmount(neededRsrc[giveRsrcIdx1]) > targetResources.getAmount(neededRsrc[giveRsrcIdx1])) && (neededRsrc[giveRsrcIdx1] != neededRsrc[getRsrcIdx])) {
                        giveResourceSet.clear();
                        giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                        int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                        if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                            offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                            D.ebugPrintln("*** offer = " + offer);
                            D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                        }
                    }
                    giveRsrcIdx1++;
                }
            }
            D.ebugPrintln("*** ourResources = " + ourResources);
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            if (offer == null) {
                int giveRsrcIdx1 = 0;
                int giveRsrcIdx2 = 0;
                while ((giveRsrcIdx1 < notNeededRsrcCount) && (offer == null)) {
                    if (ourResources.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) {
                        while ((giveRsrcIdx2 < notNeededRsrcCount) && (offer == null)) {
                            giveResourceSet.clear();
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx2]);
                            if (ourResources.contains(giveResourceSet)) {
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                    offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                    D.ebugPrintln("*** offer = " + offer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            giveRsrcIdx2++;
                        }
                        giveRsrcIdx2 = 0;
                        while ((giveRsrcIdx2 < neededRsrcCount) && (offer == null)) {
                            if (neededRsrc[giveRsrcIdx2] != neededRsrc[getRsrcIdx]) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx2]);
                                if (leftovers.contains(giveResourceSet)) {
                                    int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                    if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                        offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                        D.ebugPrintln("*** offer = " + offer);
                                        D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                    }
                                }
                            }
                            giveRsrcIdx2++;
                        }
                    }
                    giveRsrcIdx1++;
                }
                giveRsrcIdx1 = 0;
                giveRsrcIdx2 = 0;
                while ((giveRsrcIdx1 < neededRsrcCount) && (offer == null)) {
                    if ((leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) && (neededRsrc[giveRsrcIdx1] != neededRsrc[getRsrcIdx])) {
                        while ((giveRsrcIdx2 < notNeededRsrcCount) && (offer == null)) {
                            giveResourceSet.clear();
                            giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx2]);
                            if (leftovers.contains(giveResourceSet)) {
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                    offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                    D.ebugPrintln("*** offer = " + offer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            giveRsrcIdx2++;
                        }
                        giveRsrcIdx2 = 0;
                        while ((giveRsrcIdx2 < neededRsrcCount) && (offer == null)) {
                            if (neededRsrc[giveRsrcIdx2] != neededRsrc[getRsrcIdx]) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx2]);
                                if (leftovers.contains(giveResourceSet)) {
                                    int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                    if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                        offer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                        D.ebugPrintln("*** offer = " + offer);
                                        D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                    }
                                }
                            }
                            giveRsrcIdx2++;
                        }
                    }
                    giveRsrcIdx1++;
                }
            }
        }
        if (offer == null) {
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            int getRsrcIdx2 = notNeededRsrcCount - 1;
            while ((getRsrcIdx2 >= 0) && (!someoneIsSellingResource[neededRsrc[getRsrcIdx2]])) {
                getRsrcIdx2--;
            }
            while ((getRsrcIdx2 >= 0) && (offer == null)) {
                getResourceSet.clear();
                getResourceSet.add(1, notNeededRsrc[getRsrcIdx2]);
                leftovers.add(1, notNeededRsrc[getRsrcIdx2]);
                if (offer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < notNeededRsrcCount) && (offer == null)) {
                        if ((leftovers.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) && (notNeededRsrc[giveRsrcIdx1] != notNeededRsrc[getRsrcIdx2])) {
                            leftovers.subtract(1, notNeededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    offer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** offer = " + offer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            leftovers.add(1, notNeededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                if (offer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < neededRsrcCount) && (offer == null)) {
                        if (leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) {
                            leftovers.subtract(1, neededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    offer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** offer = " + offer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            leftovers.add(1, neededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                leftovers.subtract(1, notNeededRsrc[getRsrcIdx2]);
                getRsrcIdx2--;
            }
        }
        return offer;
    }

    /**
     * aux function for make offer
     */
    protected SOCTradeOffer makeOfferAux(SOCResourceSet giveResourceSet, SOCResourceSet getResourceSet, int neededResource) {
        D.ebugPrintln("**** makeOfferAux ****");
        D.ebugPrintln("giveResourceSet = " + giveResourceSet);
        D.ebugPrintln("getResourceSet = " + getResourceSet);
        SOCTradeOffer offer = null;
        boolean match = false;
        Iterator offersMadeIter = offersMade.iterator();
        while ((offersMadeIter.hasNext() && !match)) {
            SOCTradeOffer pastOffer = (SOCTradeOffer) offersMadeIter.next();
            if ((pastOffer != null) && (pastOffer.getGiveSet().equals(giveResourceSet)) && (pastOffer.getGetSet().equals(getResourceSet))) {
                match = true;
            }
        }
        if (!match) {
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                if (i != ourPlayerData.getPlayerNumber()) {
                    SOCTradeOffer outsideOffer = game.getPlayer(i).getCurrentOffer();
                    if ((outsideOffer != null) && (outsideOffer.getGetSet().equals(giveResourceSet)) && (outsideOffer.getGiveSet().equals(getResourceSet))) {
                        match = true;
                        break;
                    }
                }
            }
        }
        D.ebugPrintln("*** match = " + match);
        if (!match) {
            D.ebugPrintln("* this is a new offer");
            int numOfferedTo = 0;
            boolean[] offeredTo = new boolean[SOCGame.MAXPLAYERS];
            if (game.getCurrentPlayerNumber() == ourPlayerData.getPlayerNumber()) {
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                    D.ebugPrintln("** isSellingResource[" + i + "][" + neededResource + "] = " + isSellingResource[i][neededResource]);
                    if ((i != ourPlayerData.getPlayerNumber()) && isSellingResource[i][neededResource] && (game.getPlayer(i).getResources().getTotal() >= getResourceSet.getTotal())) {
                        SOCPlayerTracker tracker = (SOCPlayerTracker) playerTrackers.get(new Integer(i));
                        if ((tracker != null) && (tracker.getWinGameETA() >= WIN_GAME_CUTOFF)) {
                            numOfferedTo++;
                            offeredTo[i] = true;
                        } else {
                            offeredTo[i] = false;
                        }
                    }
                }
            } else {
                int curpn = game.getCurrentPlayerNumber();
                if (isSellingResource[curpn][neededResource] && (game.getPlayer(curpn).getResources().getTotal() >= getResourceSet.getTotal())) {
                    D.ebugPrintln("** isSellingResource[" + curpn + "][" + neededResource + "] = " + isSellingResource[curpn][neededResource]);
                    SOCPlayerTracker tracker = (SOCPlayerTracker) playerTrackers.get(new Integer(curpn));
                    if ((tracker != null) && (tracker.getWinGameETA() >= WIN_GAME_CUTOFF)) {
                        numOfferedTo++;
                        offeredTo[curpn] = true;
                    }
                }
            }
            D.ebugPrintln("** numOfferedTo = " + numOfferedTo);
            if (numOfferedTo > 0) {
                offer = new SOCTradeOffer(game.getName(), ourPlayerData.getPlayerNumber(), offeredTo, giveResourceSet, getResourceSet);
                boolean acceptable = false;
                for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                    if (offeredTo[pn]) {
                        int offerResponse = considerOffer2(offer, pn);
                        D.ebugPrintln("* considerOffer2(offer, " + pn + ") = " + offerResponse);
                        if (offerResponse == ACCEPT_OFFER) {
                            acceptable = true;
                            break;
                        }
                    }
                }
                if (!acceptable) {
                    offer = null;
                }
            }
        }
        return offer;
    }

    /**
     * another aux function
     * this one returns the number of rolls until we reach
     * the target given a possible offer
     *
     * @param player             our player data
     * @param targetResources    the resources we want
     * @param giveSet            the set of resources we're giving
     * @param getSet             the set of resources we're receiving
     * @param estimate           a SOCBuildingSpeedEstimate for our player
     */
    protected int getETAToTargetResources(SOCPlayer player, SOCResourceSet targetResources, SOCResourceSet giveSet, SOCResourceSet getSet, SOCBuildingSpeedEstimate estimate) {
        SOCResourceSet ourResourcesCopy = player.getResources().copy();
        D.ebugPrintln("*** giveSet = " + giveSet);
        D.ebugPrintln("*** getSet = " + getSet);
        ourResourcesCopy.subtract(giveSet);
        ourResourcesCopy.add(getSet);
        int offerBuildingTime = 1000;
        try {
            SOCResSetBuildTimePair offerBuildingTimePair = estimate.calculateRollsFast(ourResourcesCopy, targetResources, 1000, player.getPortFlags());
            offerBuildingTime = offerBuildingTimePair.getRolls();
        } catch (CutoffExceededException e) {
            ;
        }
        D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
        D.ebugPrintln("*** ourResourcesCopy = " + ourResourcesCopy);
        return (offerBuildingTime);
    }

    /**
     * consider an offer made by another player
     *
     * @param offer  the offer to consider
     * @param receiverNum  the player number of the receiver
     *
     * @return if we want to accept, reject, or make a counter offer
     */
    public int considerOffer2(SOCTradeOffer offer, int receiverNum) {
        D.ebugPrintln("***** CONSIDER OFFER 2 *****");
        int response = REJECT_OFFER;
        SOCPlayer receiverPlayerData = game.getPlayer(receiverNum);
        SOCResourceSet receiverResources = receiverPlayerData.getResources();
        SOCResourceSet rsrcsOut = offer.getGetSet();
        SOCResourceSet rsrcsIn = offer.getGiveSet();
        if ((receiverResources.getAmount(SOCResourceConstants.UNKNOWN) == 0) && (!receiverResources.contains(rsrcsOut))) {
            return response;
        }
        int senderNum = offer.getFrom();
        D.ebugPrintln("senderNum = " + senderNum);
        D.ebugPrintln("receiverNum = " + receiverNum);
        D.ebugPrintln("rsrcs from receiver = " + rsrcsOut);
        D.ebugPrintln("rsrcs to receiver = " + rsrcsIn);
        SOCPossiblePiece receiverTargetPiece = targetPieces[receiverNum];
        D.ebugPrintln("targetPieces[" + receiverNum + "] = " + receiverTargetPiece);
        SOCPlayerTracker receiverPlayerTracker = (SOCPlayerTracker) playerTrackers.get(new Integer(receiverNum));
        if (receiverPlayerTracker == null) {
            return response;
        }
        SOCPlayerTracker senderPlayerTracker = (SOCPlayerTracker) playerTrackers.get(new Integer(senderNum));
        if (senderPlayerTracker == null) {
            return response;
        }
        SOCRobotDM simulator;
        if (receiverTargetPiece == null) {
            Stack receiverBuildingPlan = new Stack();
            simulator = new SOCRobotDM(brain.getRobotParameters(), playerTrackers, receiverPlayerTracker, receiverPlayerData, receiverBuildingPlan);
            if (receiverNum == ourPlayerData.getPlayerNumber()) {
                simulator.planStuff(strategyType);
            } else {
                simulator.planStuff(strategyType);
            }
            if (receiverBuildingPlan.empty()) {
                return response;
            }
            receiverTargetPiece = (SOCPossiblePiece) receiverBuildingPlan.peek();
            targetPieces[receiverNum] = receiverTargetPiece;
        }
        D.ebugPrintln("receiverTargetPiece = " + receiverTargetPiece);
        SOCPossiblePiece senderTargetPiece = targetPieces[senderNum];
        D.ebugPrintln("targetPieces[" + senderNum + "] = " + senderTargetPiece);
        SOCPlayer senderPlayerData = game.getPlayer(senderNum);
        if (senderTargetPiece == null) {
            Stack senderBuildingPlan = new Stack();
            simulator = new SOCRobotDM(brain.getRobotParameters(), playerTrackers, senderPlayerTracker, senderPlayerData, senderBuildingPlan);
            if (senderNum == ourPlayerData.getPlayerNumber()) {
                simulator.planStuff(strategyType);
            } else {
                simulator.planStuff(strategyType);
            }
            if (senderBuildingPlan.empty()) {
                return response;
            }
            senderTargetPiece = (SOCPossiblePiece) senderBuildingPlan.peek();
            targetPieces[senderNum] = senderTargetPiece;
        }
        D.ebugPrintln("senderTargetPiece = " + senderTargetPiece);
        int senderWGETA = senderPlayerTracker.getWinGameETA();
        if (senderWGETA > WIN_GAME_CUTOFF) {
            boolean inARace = false;
            if ((receiverTargetPiece.getType() == SOCPossiblePiece.SETTLEMENT) || (receiverTargetPiece.getType() == SOCPossiblePiece.ROAD)) {
                Enumeration threatsEnum = receiverTargetPiece.getThreats().elements();
                while (threatsEnum.hasMoreElements()) {
                    SOCPossiblePiece threat = (SOCPossiblePiece) threatsEnum.nextElement();
                    if ((threat.getType() == senderTargetPiece.getType()) && (threat.getCoordinates() == senderTargetPiece.getCoordinates())) {
                        inARace = true;
                        break;
                    }
                }
                if (inARace) {
                    D.ebugPrintln("inARace == true (threat from sender)");
                } else if (receiverTargetPiece.getType() == SOCPossiblePiece.SETTLEMENT) {
                    Enumeration conflictsEnum = ((SOCPossibleSettlement) receiverTargetPiece).getConflicts().elements();
                    while (conflictsEnum.hasMoreElements()) {
                        SOCPossibleSettlement conflict = (SOCPossibleSettlement) conflictsEnum.nextElement();
                        if ((senderTargetPiece.getType() == SOCPossiblePiece.SETTLEMENT) && (conflict.getCoordinates() == senderTargetPiece.getCoordinates())) {
                            inARace = true;
                            break;
                        }
                    }
                    if (inARace) {
                        D.ebugPrintln("inARace == true (conflict with sender)");
                    }
                }
            }
            if (!inARace) {
                SOCResourceSet targetResources = null;
                switch(receiverTargetPiece.getType()) {
                    case SOCPossiblePiece.CARD:
                        targetResources = SOCGame.CARD_SET;
                        break;
                    case SOCPossiblePiece.ROAD:
                        targetResources = SOCGame.ROAD_SET;
                        break;
                    case SOCPossiblePiece.SETTLEMENT:
                        targetResources = SOCGame.SETTLEMENT_SET;
                        break;
                    case SOCPossiblePiece.CITY:
                        targetResources = SOCGame.CITY_SET;
                        break;
                }
                SOCBuildingSpeedEstimate estimate = new SOCBuildingSpeedEstimate(receiverPlayerData.getNumbers());
                SOCTradeOffer receiverBatna = getOfferToBank(targetResources);
                D.ebugPrintln("*** receiverBatna = " + receiverBatna);
                int batnaBuildingTime = getETAToTargetResources(receiverPlayerData, targetResources, SOCResourceSet.EMPTY_SET, SOCResourceSet.EMPTY_SET, estimate);
                D.ebugPrintln("*** batnaBuildingTime = " + batnaBuildingTime);
                int offerBuildingTime = getETAToTargetResources(receiverPlayerData, targetResources, rsrcsOut, rsrcsIn, estimate);
                D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                if (offerBuildingTime < batnaBuildingTime) {
                    response = ACCEPT_OFFER;
                } else {
                    response = COUNTER_OFFER;
                }
            }
        }
        return response;
    }

    /**
     * @return a counter offer or null
     *
     * @param originalOffer  the offer given to us
     */
    public SOCTradeOffer makeCounterOffer(SOCTradeOffer originalOffer) {
        D.ebugPrintln("***** MAKE COUNTER OFFER *****");
        SOCTradeOffer counterOffer = null;
        SOCPossiblePiece targetPiece = targetPieces[ourPlayerData.getPlayerNumber()];
        if (targetPiece == null) {
            Stack ourBuildingPlan = buildingPlan;
            if (ourBuildingPlan.empty()) {
                SOCRobotDM simulator;
                D.ebugPrintln("**** our building plan is empty ****");
                simulator = new SOCRobotDM(brain.getRobotParameters(), playerTrackers, ourPlayerTracker, ourPlayerData, ourBuildingPlan);
                simulator.planStuff(strategyType);
            }
            if (ourBuildingPlan.empty()) {
                return counterOffer;
            }
            targetPiece = (SOCPossiblePiece) ourBuildingPlan.peek();
            targetPieces[ourPlayerData.getPlayerNumber()] = targetPiece;
        }
        SOCResourceSet targetResources = null;
        switch(targetPiece.getType()) {
            case SOCPossiblePiece.CARD:
                targetResources = SOCGame.CARD_SET;
                break;
            case SOCPossiblePiece.ROAD:
                targetResources = SOCGame.ROAD_SET;
                break;
            case SOCPossiblePiece.SETTLEMENT:
                targetResources = SOCGame.SETTLEMENT_SET;
                break;
            case SOCPossiblePiece.CITY:
                targetResources = SOCGame.CITY_SET;
                break;
        }
        SOCResourceSet ourResources = ourPlayerData.getResources();
        D.ebugPrintln("*** targetResources = " + targetResources);
        D.ebugPrintln("*** ourResources = " + ourResources);
        if (ourResources.contains(targetResources)) {
            return counterOffer;
        }
        if (ourResources.getAmount(SOCResourceConstants.UNKNOWN) > 0) {
            D.ebugPrintln("AGG WE HAVE UNKNOWN RESOURCES !!!! %%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            return counterOffer;
        }
        SOCTradeOffer batna = getOfferToBank(targetResources);
        D.ebugPrintln("*** BATNA = " + batna);
        SOCBuildingSpeedEstimate estimate = new SOCBuildingSpeedEstimate(ourPlayerData.getNumbers());
        SOCResourceSet giveResourceSet = new SOCResourceSet();
        SOCResourceSet getResourceSet = new SOCResourceSet();
        int batnaBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
        if (batna != null) {
            batnaBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, batna.getGiveSet(), batna.getGetSet(), estimate);
        }
        D.ebugPrintln("*** batnaBuildingTime = " + batnaBuildingTime);
        int[] rollsPerResource = estimate.getRollsPerResource();
        int[] neededRsrc = new int[5];
        int[] notNeededRsrc = new int[5];
        int neededRsrcCount = 0;
        int notNeededRsrcCount = 0;
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            if (targetResources.getAmount(rsrcType) > 0) {
                neededRsrc[neededRsrcCount] = rsrcType;
                neededRsrcCount++;
            } else {
                notNeededRsrc[notNeededRsrcCount] = rsrcType;
                notNeededRsrcCount++;
            }
        }
        for (int j = neededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[neededRsrc[i]] > rollsPerResource[neededRsrc[i + 1]]) {
                    int tmp = neededRsrc[i];
                    neededRsrc[i] = neededRsrc[i + 1];
                    neededRsrc[i + 1] = tmp;
                }
            }
        }
        if (D.ebugOn) {
            for (int i = 0; i < neededRsrcCount; i++) {
                D.ebugPrintln("NEEDED RSRC: " + neededRsrc[i] + " : " + rollsPerResource[neededRsrc[i]]);
            }
        }
        for (int j = notNeededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[notNeededRsrc[i]] > rollsPerResource[notNeededRsrc[i + 1]]) {
                    int tmp = notNeededRsrc[i];
                    notNeededRsrc[i] = notNeededRsrc[i + 1];
                    notNeededRsrc[i + 1] = tmp;
                }
            }
        }
        if (D.ebugOn) {
            for (int i = 0; i < notNeededRsrcCount; i++) {
                D.ebugPrintln("NOT-NEEDED RSRC: " + notNeededRsrc[i] + " : " + rollsPerResource[notNeededRsrc[i]]);
            }
        }
        int getRsrcIdx = neededRsrcCount - 1;
        while ((getRsrcIdx >= 0) && ((ourResources.getAmount(neededRsrc[getRsrcIdx]) >= targetResources.getAmount(neededRsrc[getRsrcIdx])) || (originalOffer.getGiveSet().getAmount(neededRsrc[getRsrcIdx]) == 0))) {
            getRsrcIdx--;
        }
        if (getRsrcIdx >= 0) {
            D.ebugPrintln("*** getRsrc = " + neededRsrc[getRsrcIdx]);
            getResourceSet.add(1, neededRsrc[getRsrcIdx]);
            D.ebugPrintln("*** counterOffer should be null : counterOffer = " + counterOffer);
            int giveRsrcIdx = 0;
            while ((giveRsrcIdx < notNeededRsrcCount) && (counterOffer == null)) {
                D.ebugPrintln("*** ourResources.getAmount(" + notNeededRsrc[giveRsrcIdx] + ") = " + ourResources.getAmount(notNeededRsrc[giveRsrcIdx]));
                if (ourResources.getAmount(notNeededRsrc[giveRsrcIdx]) > 0) {
                    giveResourceSet.clear();
                    giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx]);
                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                }
                giveRsrcIdx++;
            }
            D.ebugPrintln("*** ourResources = " + ourResources);
            if (counterOffer == null) {
                int giveRsrcIdx1 = 0;
                while ((giveRsrcIdx1 < neededRsrcCount) && (counterOffer == null)) {
                    D.ebugPrintln("*** ourResources.getAmount(" + neededRsrc[giveRsrcIdx1] + ") = " + ourResources.getAmount(neededRsrc[giveRsrcIdx1]));
                    D.ebugPrintln("*** targetResources.getAmount(" + neededRsrc[giveRsrcIdx1] + ") = " + targetResources.getAmount(neededRsrc[giveRsrcIdx1]));
                    if ((ourResources.getAmount(neededRsrc[giveRsrcIdx1]) > targetResources.getAmount(neededRsrc[giveRsrcIdx1])) && (neededRsrc[giveRsrcIdx1] != neededRsrc[getRsrcIdx])) {
                        giveResourceSet.clear();
                        giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                        int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                        if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                            counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                            D.ebugPrintln("*** counterOffer = " + counterOffer);
                            D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                        }
                    }
                    giveRsrcIdx1++;
                }
            }
            D.ebugPrintln("*** ourResources = " + ourResources);
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            if (counterOffer == null) {
                int giveRsrcIdx1 = 0;
                int giveRsrcIdx2 = 0;
                while ((giveRsrcIdx1 < notNeededRsrcCount) && (counterOffer == null)) {
                    if (ourResources.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) {
                        while ((giveRsrcIdx2 < notNeededRsrcCount) && (counterOffer == null)) {
                            giveResourceSet.clear();
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx2]);
                            if (ourResources.contains(giveResourceSet)) {
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            giveRsrcIdx2++;
                        }
                        giveRsrcIdx2 = 0;
                        while ((giveRsrcIdx2 < neededRsrcCount) && (counterOffer == null)) {
                            if (neededRsrc[giveRsrcIdx2] != neededRsrc[getRsrcIdx]) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx2]);
                                if (leftovers.contains(giveResourceSet)) {
                                    int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                    if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                        counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                        D.ebugPrintln("*** counterOffer = " + counterOffer);
                                        D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                    }
                                }
                            }
                            giveRsrcIdx2++;
                        }
                    }
                    giveRsrcIdx1++;
                }
                giveRsrcIdx1 = 0;
                giveRsrcIdx2 = 0;
                while ((giveRsrcIdx1 < neededRsrcCount) && (counterOffer == null)) {
                    if ((leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) && (neededRsrc[giveRsrcIdx1] != neededRsrc[getRsrcIdx])) {
                        while ((giveRsrcIdx2 < notNeededRsrcCount) && (counterOffer == null)) {
                            giveResourceSet.clear();
                            giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                            giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx2]);
                            if (leftovers.contains(giveResourceSet)) {
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            giveRsrcIdx2++;
                        }
                        giveRsrcIdx2 = 0;
                        while ((giveRsrcIdx2 < neededRsrcCount) && (counterOffer == null)) {
                            if (neededRsrc[giveRsrcIdx2] != neededRsrc[getRsrcIdx]) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx2]);
                                if (leftovers.contains(giveResourceSet)) {
                                    int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                    if ((offerBuildingTime < batnaBuildingTime) || ((batna != null) && (offerBuildingTime == batnaBuildingTime) && (giveResourceSet.getTotal() < batna.getGiveSet().getTotal()))) {
                                        counterOffer = makeOfferAux(giveResourceSet, getResourceSet, neededRsrc[getRsrcIdx]);
                                        D.ebugPrintln("*** counterOffer = " + counterOffer);
                                        D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                    }
                                }
                            }
                            giveRsrcIdx2++;
                        }
                    }
                    giveRsrcIdx1++;
                }
            }
        }
        if (counterOffer == null) {
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            int getRsrcIdx2 = notNeededRsrcCount - 1;
            while ((getRsrcIdx2 >= 0) && (originalOffer.getGiveSet().getAmount(notNeededRsrc[getRsrcIdx2]) == 0)) {
                getRsrcIdx2--;
            }
            while ((getRsrcIdx2 >= 0) && (counterOffer == null)) {
                getResourceSet.clear();
                getResourceSet.add(1, notNeededRsrc[getRsrcIdx2]);
                leftovers.add(1, notNeededRsrc[getRsrcIdx2]);
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < notNeededRsrcCount) && (counterOffer == null)) {
                        if ((leftovers.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) && (notNeededRsrc[giveRsrcIdx1] != notNeededRsrc[getRsrcIdx2])) {
                            leftovers.subtract(1, notNeededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            leftovers.add(1, notNeededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < neededRsrcCount) && (counterOffer == null)) {
                        if (leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) {
                            leftovers.subtract(1, neededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                }
                            }
                            leftovers.add(1, neededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                leftovers.subtract(1, notNeededRsrc[getRsrcIdx2]);
                getRsrcIdx2--;
            }
        }
        if (counterOffer == null) {
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            int getRsrcIdx2 = notNeededRsrcCount - 1;
            while ((getRsrcIdx2 >= 0) && (originalOffer.getGiveSet().getAmount(notNeededRsrc[getRsrcIdx2]) == 0)) {
                getRsrcIdx2--;
            }
            while ((getRsrcIdx2 >= 0) && (counterOffer == null)) {
                getResourceSet.clear();
                getResourceSet.add(2, notNeededRsrc[getRsrcIdx2]);
                leftovers.add(2, notNeededRsrc[getRsrcIdx2]);
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < notNeededRsrcCount) && (counterOffer == null)) {
                        if ((leftovers.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) && (notNeededRsrc[giveRsrcIdx1] != notNeededRsrc[getRsrcIdx2])) {
                            leftovers.subtract(1, notNeededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            leftovers.add(1, notNeededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < neededRsrcCount) && (counterOffer == null)) {
                        if (leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) {
                            leftovers.subtract(1, neededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                }
                            }
                            leftovers.add(1, neededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                leftovers.subtract(2, notNeededRsrc[getRsrcIdx2]);
                getRsrcIdx2--;
            }
        }
        if (counterOffer == null) {
            SOCResourceSet leftovers = ourResources.copy();
            leftovers.subtract(targetResources);
            D.ebugPrintln("*** leftovers = " + leftovers);
            int getRsrcIdx2 = notNeededRsrcCount - 1;
            while ((getRsrcIdx2 >= 0) && (originalOffer.getGiveSet().getAmount(notNeededRsrc[getRsrcIdx2]) == 0)) {
                getRsrcIdx2--;
            }
            while ((getRsrcIdx2 >= 0) && (counterOffer == null)) {
                getResourceSet.clear();
                getResourceSet.add(3, notNeededRsrc[getRsrcIdx2]);
                leftovers.add(3, notNeededRsrc[getRsrcIdx2]);
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < notNeededRsrcCount) && (counterOffer == null)) {
                        if ((leftovers.getAmount(notNeededRsrc[giveRsrcIdx1]) > 0) && (notNeededRsrc[giveRsrcIdx1] != notNeededRsrc[getRsrcIdx2])) {
                            leftovers.subtract(1, notNeededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, notNeededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                    D.ebugPrintln("*** offerBuildingTime = " + offerBuildingTime);
                                }
                            }
                            leftovers.add(1, notNeededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                if (counterOffer == null) {
                    int giveRsrcIdx1 = 0;
                    while ((giveRsrcIdx1 < neededRsrcCount) && (counterOffer == null)) {
                        if (leftovers.getAmount(neededRsrc[giveRsrcIdx1]) > 0) {
                            leftovers.subtract(1, neededRsrc[giveRsrcIdx1]);
                            if (getOfferToBank(targetResources, leftovers) != null) {
                                giveResourceSet.clear();
                                giveResourceSet.add(1, neededRsrc[giveRsrcIdx1]);
                                int offerBuildingTime = getETAToTargetResources(ourPlayerData, targetResources, giveResourceSet, getResourceSet, estimate);
                                if (offerBuildingTime < batnaBuildingTime) {
                                    counterOffer = makeOfferAux(giveResourceSet, getResourceSet, notNeededRsrc[getRsrcIdx2]);
                                    D.ebugPrintln("*** counterOffer = " + counterOffer);
                                }
                            }
                            leftovers.add(1, neededRsrc[giveRsrcIdx1]);
                        }
                        giveRsrcIdx1++;
                    }
                }
                leftovers.subtract(3, notNeededRsrc[getRsrcIdx2]);
                getRsrcIdx2--;
            }
        }
        return counterOffer;
    }

    /**
     * @return the offer that we'll make to the bank/ports
     *
     * @param targetResources  what resources we want
     * @param ourResources     the resources we have
     */
    public SOCTradeOffer getOfferToBank(SOCResourceSet targetResources, SOCResourceSet ourResources) {
        SOCTradeOffer bankTrade = null;
        if (ourResources.contains(targetResources)) {
            return bankTrade;
        }
        SOCBuildingSpeedEstimate estimate = new SOCBuildingSpeedEstimate(ourPlayerData.getNumbers());
        int[] rollsPerResource = estimate.getRollsPerResource();
        boolean[] ports = ourPlayerData.getPortFlags();
        int[] neededRsrc = new int[5];
        int[] notNeededRsrc = new int[5];
        int neededRsrcCount = 0;
        int notNeededRsrcCount = 0;
        for (int rsrcType = SOCResourceConstants.CLAY; rsrcType <= SOCResourceConstants.WOOD; rsrcType++) {
            if (targetResources.getAmount(rsrcType) > 0) {
                neededRsrc[neededRsrcCount] = rsrcType;
                neededRsrcCount++;
            } else {
                notNeededRsrc[notNeededRsrcCount] = rsrcType;
                notNeededRsrcCount++;
            }
        }
        for (int j = neededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[neededRsrc[i]] > rollsPerResource[neededRsrc[i + 1]]) {
                    int tmp = neededRsrc[i];
                    neededRsrc[i] = neededRsrc[i + 1];
                    neededRsrc[i + 1] = tmp;
                }
            }
        }
        for (int j = notNeededRsrcCount - 1; j >= 0; j--) {
            for (int i = 0; i < j; i++) {
                if (rollsPerResource[notNeededRsrc[i]] > rollsPerResource[notNeededRsrc[i + 1]]) {
                    int tmp = notNeededRsrc[i];
                    notNeededRsrc[i] = notNeededRsrc[i + 1];
                    notNeededRsrc[i + 1] = tmp;
                }
            }
        }
        int getRsrcIdx = neededRsrcCount - 1;
        while (ourResources.getAmount(neededRsrc[getRsrcIdx]) >= targetResources.getAmount(neededRsrc[getRsrcIdx])) {
            getRsrcIdx--;
        }
        int giveRsrcIdx = 0;
        while (giveRsrcIdx < notNeededRsrcCount) {
            int tradeRatio;
            if (ports[notNeededRsrc[giveRsrcIdx]]) {
                tradeRatio = 2;
            } else if (ports[SOCBoard.MISC_PORT]) {
                tradeRatio = 3;
            } else {
                tradeRatio = 4;
            }
            if (ourResources.getAmount(notNeededRsrc[giveRsrcIdx]) >= tradeRatio) {
                SOCResourceSet give = new SOCResourceSet();
                SOCResourceSet get = new SOCResourceSet();
                give.add(tradeRatio, notNeededRsrc[giveRsrcIdx]);
                get.add(1, neededRsrc[getRsrcIdx]);
                boolean[] to = new boolean[SOCGame.MAXPLAYERS];
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                    to[i] = false;
                }
                bankTrade = new SOCTradeOffer(game.getName(), ourPlayerData.getPlayerNumber(), to, give, get);
                return bankTrade;
            } else {
                giveRsrcIdx++;
            }
        }
        giveRsrcIdx = 0;
        while (giveRsrcIdx < neededRsrcCount) {
            int tradeRatio;
            if (ports[neededRsrc[giveRsrcIdx]]) {
                tradeRatio = 2;
            } else if (ports[SOCBoard.MISC_PORT]) {
                tradeRatio = 3;
            } else {
                tradeRatio = 4;
            }
            if (rollsPerResource[neededRsrc[giveRsrcIdx]] >= rollsPerResource[neededRsrc[getRsrcIdx]]) {
                if ((ourResources.getAmount(neededRsrc[giveRsrcIdx]) - targetResources.getAmount(neededRsrc[giveRsrcIdx])) >= tradeRatio) {
                    SOCResourceSet give = new SOCResourceSet();
                    SOCResourceSet get = new SOCResourceSet();
                    give.add(tradeRatio, neededRsrc[giveRsrcIdx]);
                    get.add(1, neededRsrc[getRsrcIdx]);
                    boolean[] to = new boolean[SOCGame.MAXPLAYERS];
                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                        to[i] = false;
                    }
                    bankTrade = new SOCTradeOffer(game.getName(), ourPlayerData.getPlayerNumber(), to, give, get);
                    return bankTrade;
                }
            } else {
                if (ourResources.getAmount(neededRsrc[giveRsrcIdx]) >= tradeRatio) {
                    SOCResourceSet give = new SOCResourceSet();
                    SOCResourceSet get = new SOCResourceSet();
                    give.add(tradeRatio, neededRsrc[giveRsrcIdx]);
                    get.add(1, neededRsrc[getRsrcIdx]);
                    boolean[] to = new boolean[SOCGame.MAXPLAYERS];
                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                        to[i] = false;
                    }
                    bankTrade = new SOCTradeOffer(game.getName(), ourPlayerData.getPlayerNumber(), to, give, get);
                    return bankTrade;
                }
            }
            giveRsrcIdx++;
        }
        return bankTrade;
    }

    /**
     * @return the offer that we'll make to the bank/ports
     *
     * @param targetResources  what resources we want
     */
    public SOCTradeOffer getOfferToBank(SOCResourceSet targetResources) {
        return getOfferToBank(targetResources, ourPlayerData.getResources());
    }
}
