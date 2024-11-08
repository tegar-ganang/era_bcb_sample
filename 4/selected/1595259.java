package com.runesoft.runeEMU.server;

import java.net.*;
import java.io.*;

public class client extends Player implements Runnable {

    public client(Socket s, int playerID) {
        super(s, playerID);
        this.playerHostname = s.getInetAddress().getHostName();
        try {
            in = s.getInputStream();
            out = s.getOutputStream();
        } catch (Exception _ex) {
            log("error: fetching input/output streams (" + _ex.getMessage() + ")...");
            return;
        }
        outStream = new stream(new byte[bufferSize]);
        outStream.currentOffset = 0;
        inStream = new stream(new byte[bufferSize]);
        inStream.currentOffset = 0;
        readPtr = writePtr = 0;
        buffer = buffer = new byte[bufferSize];
    }

    public void loginComplete() {
        log("login complete..");
        loggedIn = true;
        (new Thread(this)).start();
    }

    public void initialize() {
        pfg = new PlayerFrameGenerator(this, outStream);
        coord = new Coord(-1, -1, 0);
        teleport(3222, 3218, 0);
        if (outStream == null) log("wtf");
        outStream.createFrame(249);
        outStream.writeByteA(1);
        outStream.writeWordBigEndianA(playerID);
        pfg.setChatOptions(0, 0, 0);
        for (int i = 0; i < 25; i++) pfg.setSkillLevel(i, 50, 4000);
        outStream.createFrame(107);
        pfg.setSidebarInterface(1, 3917);
        pfg.setSidebarInterface(2, 638);
        pfg.setSidebarInterface(3, 3213);
        pfg.setSidebarInterface(4, 1644);
        pfg.setSidebarInterface(5, 5608);
        pfg.setSidebarInterface(6, 1151);
        pfg.setSidebarInterface(7, 1);
        pfg.setSidebarInterface(8, 5065);
        pfg.setSidebarInterface(9, 5715);
        pfg.setSidebarInterface(10, 2449);
        pfg.setSidebarInterface(11, 4445);
        pfg.setSidebarInterface(12, 147);
        pfg.setSidebarInterface(13, 6299);
        pfg.setSidebarInterface(0, 2423);
        pfg.setMenuOption("Throw-up on", 1, 0);
        pfg.setMenuOption("Duel", 3, 1);
        pfg.setMenuOption("Trade-with", 4, 1);
        pfg.setMenuOption("Follow", 5, 0);
        setEquipment("shield", 1173);
        setEquipment("hands", 775);
        setEquipment("feet", 1837);
        setEquipment("weapon", 1291);
        PlayerUpdater.updatePlayer(this, outStream);
        flushOutStream();
        resetItems();
        hasBeenInitialized = true;
    }

    public void run() {
        packetSize = 0;
        packetType = -1;
        readPtr = 0;
        writePtr = 0;
        int numBytesInBuffer, offset;
        while (!die) {
            synchronized (this) {
                if (writePtr == readPtr) {
                    try {
                        wait();
                    } catch (InterruptedException _ex) {
                    }
                }
                if (die) return;
                offset = readPtr;
                if (writePtr >= readPtr) numBytesInBuffer = writePtr - readPtr; else numBytesInBuffer = bufferSize - readPtr;
            }
            if (numBytesInBuffer > 0) {
                try {
                    out.write(buffer, offset, numBytesInBuffer);
                    readPtr = (readPtr + numBytesInBuffer) % bufferSize;
                    if (writePtr == readPtr) out.flush();
                } catch (Exception _ex) {
                    kill("buffer write exception - " + _ex.getMessage());
                }
            }
        }
    }

    public void preProcessing() {
        newWalkCmdSteps = 0;
    }

    public void process() {
        PlayerUpdater.updatePlayer(this, outStream);
        PlayerUpdater.updateNPC(this, outStream);
        getNextPlayerMovement();
        flushOutStream();
        while (getPackets()) ;
    }

    public void postProcessing() {
    }

    public void setLook(String target, int id) {
        appearance.set(target, id);
        appearanceUpdateRequired = true;
        updateRequired = true;
    }

    public void setEquipment(String target, int id) {
        equipment.set(target, id);
        appearanceUpdateRequired = true;
        updateRequired = true;
    }

    public void setSkill(String skill, int level) {
    }

    public void addItem(int id, int amount) {
    }

    public void addBankItem(int id, int amount) {
    }

    public void addFriend(String friend) {
    }

    public void addIgnore(String ignore) {
    }

    public void teleport(int x, int y, int heightlevel) {
        teleportToX = x;
        teleportToY = y;
        teleportToHeightLevel = heightlevel;
        playerTeleported = true;
    }

    public void resetItems() {
        Item[] invent = inventory.getItemsResized();
        outStream.createFrameVarSizeWord(53);
        outStream.writeWord(3214);
        outStream.writeWord(invent.length);
        for (int i = 0; i < invent.length; i++) {
            if (invent[i].getAmount() > 254) {
                outStream.writeByte(255);
                outStream.writeDWord_v2(invent[i].getAmount());
            } else outStream.writeByte(invent[i].getAmount());
            outStream.writeWordBigEndianA(invent[i].getID());
        }
        outStream.endFrameVarSizeWord();
    }

    public void resetItems(int itemFrame) {
        Item[] invent = inventory.getItemsResized();
        outStream.createFrameVarSizeWord(53);
        outStream.writeWord(itemFrame);
        outStream.writeWord(invent.length);
        for (int i = 0; i < invent.length; i++) {
            if (invent[i].getAmount() > 254) {
                outStream.writeByte(255);
                outStream.writeDWord_v2(invent[i].getAmount());
            } else outStream.writeByte(invent[i].getAmount());
            outStream.writeWordBigEndianA(invent[i].getID());
        }
        outStream.endFrameVarSizeWord();
    }

    public boolean destructed = false;

    public void destruct() {
        log("killed: " + killMessage + "... ");
        destructed = true;
    }
}
