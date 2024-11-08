package org.professio.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.professio.net.Cryption;
import org.professio.net.Stream;
import org.professio.util.Calculations;
import org.professio.util.Constants;
import org.professio.managers.PlayerManager;

public abstract class ClientAssistant implements Runnable, Constants {

    private static Stream updateBlock = new Stream(new byte[10000]);

    public static Stream playerProps = new Stream(new byte[100]);

    public Client[] playerList;

    public byte[] playerInListBitmap;

    public boolean chatTextUpdateRequired;

    protected boolean initialized;

    protected boolean appearanceUpdateRequired;

    protected boolean newWalkCmdIsRunning;

    protected boolean printLog;

    protected Socket mySock;

    protected InputStream in;

    protected OutputStream out;

    protected Stream inStream;

    protected Stream outStream;

    private Player myPlayer;

    protected int[] newWalkCmdX;

    protected int[] newWalkCmdY;

    protected int[] travelBackX;

    protected int[] travelBackY;

    protected int newWalkCmdSteps;

    protected int numTravelBackSteps;

    protected int poimiX;

    protected int mapRegionX;

    protected int chatTextEffects;

    protected int chatTextColor;

    protected int mapRegionY;

    protected int poimiY;

    protected int logoutTimer;

    protected byte[] chatText;

    protected byte chatTextSize;

    private byte[] buffer;

    private long[] friendList;

    private int[] walkingQueueX;

    private int[] walkingQueueY;

    private int[] playerAppearance;

    private int wQueueReadPtr;

    private int playerListSize;

    private int timeOutCounter;

    private int sessionID;

    private int wQueueWritePtr;

    private int teleportToX;

    private int teleportToY;

    private int currentX;

    private int readPtr;

    private int writePtr;

    private int currentY;

    private int dir1;

    private int dir2;

    private boolean disconnected;

    private boolean isRunning;

    private boolean didTeleport;

    private boolean mapRegionDidChange;

    private boolean updateRequired;

    private Cryption inStreamDecryption;

    private Cryption outStreamDecryption;

    protected ClientAssistant(final Socket s) {
        playerList = new Client[MAX_PLAYERS];
        playerInListBitmap = new byte[(MAX_PLAYERS + 7) >> 3];
        appearanceUpdateRequired = true;
        newWalkCmdX = newWalkCmdY = travelBackX = travelBackY = new int[WALKING_QUEUE_SIZE];
        chatText = new byte[4096];
        chatTextSize = 0;
        updateRequired = true;
        friendList = new long[100];
        friendList[0] = 25145847;
        dir1 = dir2 = -1;
        walkingQueueX = walkingQueueY = new int[WALKING_QUEUE_SIZE];
        teleportToX = HOME_X;
        teleportToY = HOME_Y;
        mapRegionX = mapRegionY = -1;
        currentX = currentY = 0;
        playerAppearance = new int[13];
        playerAppearance[0] = 0;
        playerAppearance[1] = 7;
        playerAppearance[2] = 25;
        playerAppearance[3] = 29;
        playerAppearance[4] = 35;
        playerAppearance[5] = 39;
        playerAppearance[6] = 44;
        playerAppearance[7] = 14;
        playerAppearance[8] = 7;
        playerAppearance[9] = 8;
        playerAppearance[10] = 9;
        playerAppearance[11] = 5;
        playerAppearance[12] = 0;
        resetWalkingQueue();
        mySock = s;
        try {
            in = s.getInputStream();
            out = s.getOutputStream();
        } catch (Exception e) {
        }
        outStream = new Stream(new byte[BUFFER_SIZE]);
        inStream = new Stream(new byte[BUFFER_SIZE]);
        outStream.currentOffset = 0;
        inStream.currentOffset = 0;
        readPtr = 0;
        writePtr = 0;
        buffer = new byte[BUFFER_SIZE];
    }

    public Player getPlayer() {
        return myPlayer;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public boolean updateRequired() {
        return updateRequired;
    }

    public boolean setUpdateRequired(final boolean b) {
        return updateRequired = b;
    }

    public int getPlayerListSize() {
        return playerListSize;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void resetPlayerListSize() {
        playerListSize = 0;
    }

    public int increasePlayerListSize() {
        return playerListSize++;
    }

    public boolean didTeleported() {
        return didTeleport;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void resetWalkingQueue() {
        wQueueReadPtr = wQueueWritePtr = 0;
        for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
            walkingQueueX[i] = currentX;
            walkingQueueY[i] = currentY;
        }
    }

    public void addToWalkingQueue(final int x, final int y) {
        int next = (wQueueWritePtr + 1) % WALKING_QUEUE_SIZE;
        if (next == wQueueWritePtr) return;
        walkingQueueX[wQueueWritePtr] = x;
        walkingQueueY[wQueueWritePtr] = y;
        wQueueWritePtr = next;
    }

    public int getNextWalkingDirection() {
        if (wQueueReadPtr == wQueueWritePtr) return -1;
        int dir;
        do {
            dir = Calculations.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]);
            if (dir == -1) {
                wQueueReadPtr = (wQueueReadPtr + 1) % WALKING_QUEUE_SIZE;
            } else if ((dir & 1) != 0) {
                resetWalkingQueue();
                return -1;
            }
        } while (dir == -1 && wQueueReadPtr != wQueueWritePtr);
        if (dir == -1) return -1;
        dir >>= 1;
        currentX += Calculations.DIRECTION_DELTAX[dir];
        currentY += Calculations.DIRECTION_DELTAY[dir];
        getPlayer().setX(getPlayer().getX() + Calculations.DIRECTION_DELTAX[dir]);
        getPlayer().setY(getPlayer().getY() + Calculations.DIRECTION_DELTAY[dir]);
        return dir;
    }

    public boolean packetProcess() {
        if (disconnected) return false;
        int packetType = -1;
        int packetSize = 0;
        try {
            if (timeOutCounter++ > 20) {
                destruct();
                return false;
            }
            if (in == null) return false;
            int avail = in.available();
            if (avail == 0) return false;
            if (packetType == -1) {
                packetType = in.read() & 0xff;
                if (inStreamDecryption != null) packetType = packetType - inStreamDecryption.getNextKey() & 0xff;
                packetSize = PACKET_SIZES[packetType];
                avail--;
            }
            if (packetSize == -1) {
                if (avail > 0) {
                    packetSize = in.read() & 0xff;
                    avail--;
                } else {
                    return false;
                }
            }
            if (avail < packetSize) return false;
            fillInStream(packetSize);
            timeOutCounter = 0;
            parseIncomingPackets(packetType, packetSize);
        } catch (Exception e) {
            destruct();
        }
        return true;
    }

    protected void wearItem(final int slot) {
        final int wearSlot = 3;
        getPlayer().wearItem(getPlayer().getItem(slot), getPlayer().getStack(slot), wearSlot);
        setEquipment(getPlayer().getItem(slot), getPlayer().getStack(slot), wearSlot);
        deleteItem(slot);
    }

    protected void openBank() {
        resetItems(5064);
        sendBank();
        outStream.createFrame(248);
        outStream.writeWordA(5292);
        outStream.writeWord(5063);
        flushOutStream();
    }

    private void sendBank() {
        outStream.createFrameVarSizeWord(53);
        outStream.writeWord(5382);
        outStream.writeWord(BANK_SIZE);
        for (int i = 0; i < BANK_SIZE; i++) {
            if (getPlayer().getBankStack(i) > 254) {
                outStream.writeByte(255);
                outStream.writeDWord_v2(getPlayer().getBankStack(i));
            } else {
                outStream.writeByte(getPlayer().getBankStack(i));
            }
            outStream.writeWordBigEndianA(getPlayer().getBankItem(i));
        }
        outStream.endFrameVarSizeWord();
        flushOutStream();
    }

    protected void removeItem(final int slot) {
        final int wearSlot = 3;
        getPlayer().addItem(getPlayer().getWearing(slot), getPlayer().getWearingStack(slot));
        getPlayer().removeItem(slot);
        setEquipment(-1, 0, wearSlot);
        resetItems(3214);
    }

    protected void deleteItem(final int i) {
        getPlayer().deleteItem(i);
        resetItems(3214);
    }

    public void resetItems(final int i) {
        outStream.createFrameVarSizeWord(53);
        outStream.writeWord(i);
        outStream.writeWord(28);
        for (int j = 0; j < 28; j++) {
            if (getPlayer().getStack(j) > 254) {
                outStream.writeByte(255);
                outStream.writeDWord_v2(getPlayer().getStack(j));
            } else {
                outStream.writeByte(getPlayer().getStack(j));
            }
            outStream.writeWordBigEndianA(getPlayer().getItem(j) + 1);
        }
        outStream.endFrameVarSizeWord();
        flushOutStream();
    }

    public void setEquipment(final int wearID, final int amount, final int targetSlot) {
        outStream.createFrameVarSizeWord(34);
        outStream.writeWord(1688);
        outStream.writeByte(targetSlot);
        outStream.writeWord(wearID + 1);
        if (amount > 254) {
            outStream.writeByte(255);
            outStream.writeDWord(amount);
        } else {
            outStream.writeByte(amount);
        }
        outStream.endFrameVarSizeWord();
        updateRequired = true;
        appearanceUpdateRequired = true;
    }

    public void showInterface(final int ID) {
        outStream.createFrame(97);
        outStream.writeWord(ID);
    }

    public void flushOutStream() {
        if (disconnected || outStream.currentOffset == 0) return;
        synchronized (this) {
            int maxWritePtr = (readPtr + BUFFER_SIZE - 2) % BUFFER_SIZE;
            for (int i = 0; i < outStream.currentOffset; i++) {
                buffer[writePtr] = outStream.buffer[i];
                writePtr = (writePtr + 1) % BUFFER_SIZE;
                if (writePtr == maxWritePtr) {
                    destruct();
                    return;
                }
            }
            outStream.currentOffset = 0;
            notify();
        }
    }

    public void playerLog(final String s, final String msg) {
        if (printLog) System.out.println("[" + s + "] " + getPlayer().getName() + ": " + msg);
    }

    public void sendMessage(final String s) {
        playerLog("Message", s);
        outStream.createFrameVarSize(253);
        outStream.writeString(s);
        outStream.endFrameVarSize();
    }

    public void setSidebarInterface(final int slot, final int id) {
        outStream.createFrame(71);
        outStream.writeWord(id);
        outStream.writeByteA(slot);
    }

    public void setSkillLevel(final int skill, int level, int xp) {
        outStream.createFrame(134);
        outStream.writeByte(skill);
        outStream.writeDWord_v1(xp);
        outStream.writeByte(level);
    }

    public void processLogin() {
        long serverSessionKey = 0, clientSessionKey = 0;
        serverSessionKey = ((long) (Math.random() * 99999999D) << 32) + (long) (Math.random() * 99999999D);
        try {
            fillInStream(2);
            if (inStream.readUnsignedByte() != 14) {
                destruct();
                return;
            }
            inStream.readUnsignedByte();
            for (int i = 0; i < 8; i++) out.write(0);
            out.write(0);
            outStream.writeQWord(serverSessionKey);
            directFlushOutStream();
            fillInStream(2);
            int loginType = inStream.readUnsignedByte();
            if (loginType != 16 && loginType != 18) {
                destruct();
                return;
            }
            int loginPacketSize = inStream.readUnsignedByte();
            int loginEncryptPacketSize = loginPacketSize - 40;
            if (loginEncryptPacketSize <= 0) {
                destruct();
                return;
            }
            fillInStream(loginPacketSize);
            if (inStream.readUnsignedByte() != 255 || inStream.readWord() != 317) {
                destruct();
                return;
            }
            inStream.readUnsignedByte();
            for (int i = 0; i < 9; i++) {
                inStream.readDWord();
            }
            loginEncryptPacketSize--;
            if (loginEncryptPacketSize != inStream.readUnsignedByte()) {
                destruct();
                return;
            }
            if (inStream.readUnsignedByte() != 10) {
                destruct();
                return;
            }
            clientSessionKey = inStream.readQWord();
            serverSessionKey = inStream.readQWord();
            inStream.readDWord();
            String playerName = inStream.readString();
            String playerPass = inStream.readString();
            int sessionKey[] = new int[4];
            sessionKey[0] = (int) (clientSessionKey >> 32);
            sessionKey[1] = (int) (clientSessionKey);
            sessionKey[2] = (int) (serverSessionKey >> 32);
            sessionKey[3] = (int) (serverSessionKey);
            inStreamDecryption = new Cryption(sessionKey);
            for (int i = 0; i < 4; i++) sessionKey[i] += 50;
            outStreamDecryption = new Cryption(sessionKey);
            outStream.packetEncryption = outStreamDecryption;
            myPlayer = loadChar(playerName, playerPass);
            if (myPlayer != null) {
                if (!myPlayer.correctPassword(playerPass)) {
                    out.write(3);
                    destruct();
                    return;
                }
            }
            if (PlayerManager.getPlayer(playerName) != null) {
                out.write(5);
                destruct();
                return;
            }
            sessionID = PlayerManager.addPlayer(this);
            if (sessionID == -1) {
                out.write(7);
                destruct();
                return;
            }
            out.write(2);
            out.write(0);
            out.write(0);
            new Thread(this).start();
        } catch (Exception e) {
            destruct();
            return;
        }
    }

    private boolean isFriendOnline(final long l) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            final Client o = PlayerManager.getClient(i);
            if (o == null || o.isDisconnected()) continue;
            if (Calculations.playerNameToInt64(o.getPlayer().getName()) == l) return true;
        }
        return false;
    }

    protected void loadPM() {
        for (int i = 0; i < friendList.length; i++) {
            if (friendList[i] == 0) continue;
            loadPM(friendList[i], isFriendOnline(friendList[i]) ? 1 : 0);
        }
    }

    public void changeWorld(final long l) {
        loadPM(l, isFriendOnline(l) ? 1 : 0);
    }

    private void loadPM(final long l, final int i) {
        outStream.createFrame(50);
        outStream.writeQWord(l);
        outStream.writeByte(i != 0 ? i + 9 : i);
    }

    public void setPrivateMessaging(final int i) {
        outStream.createFrame(221);
        outStream.writeByte(i);
    }

    public boolean hasFriend(final long l) {
        for (int i = 0; i < friendList.length; i++) {
            if (friendList[i] == l) return true;
        }
        return false;
    }

    protected void addFriend(final long l) {
        if (hasFriend(l)) return;
        for (int i = 0; i < friendList.length; i++) {
            if (friendList[i] == 0) {
                friendList[i] = l;
                System.out.println("Added " + l + " in slot " + i);
                return;
            }
        }
    }

    public void run() {
        readPtr = 0;
        writePtr = 0;
        int numBytesInBuffer, offset;
        while (!disconnected) {
            synchronized (this) {
                if (writePtr == readPtr) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                if (disconnected) return;
                offset = readPtr;
                if (writePtr >= readPtr) numBytesInBuffer = writePtr - readPtr; else numBytesInBuffer = BUFFER_SIZE - readPtr;
            }
            if (numBytesInBuffer > 0) {
                try {
                    out.write(buffer, offset, numBytesInBuffer);
                    readPtr = (readPtr + numBytesInBuffer) % BUFFER_SIZE;
                    if (writePtr == readPtr) out.flush();
                } catch (Exception e) {
                    destruct();
                }
            }
        }
    }

    private Player loadChar(final String s, final String s1) {
        try {
            final ObjectInputStream in = new ObjectInputStream(new FileInputStream("./characters/" + s + ".pcf"));
            final Player output = (Player) in.readObject();
            in.close();
            return output;
        } catch (FileNotFoundException e) {
            return Player.newPlayer(s, s1);
        } catch (Exception e) {
        }
        return null;
    }

    public void Update() {
        updateBlock.currentOffset = 0;
        updateThisPlayerMovement(outStream);
        final boolean flag = chatTextUpdateRequired;
        chatTextUpdateRequired = false;
        appendPlayerUpdateBlock(updateBlock);
        chatTextUpdateRequired = flag;
        outStream.writeBits(8, getPlayerListSize());
        int size = getPlayerListSize();
        resetPlayerListSize();
        for (int i = 0; i < size; i++) {
            if (!playerList[i].didTeleported() && withinDistance(playerList[i])) {
                playerList[i].updatePlayerMovement(outStream);
                playerList[i].appendPlayerUpdateBlock(updateBlock);
                playerList[increasePlayerListSize()] = playerList[i];
            } else {
                final int ID = playerList[i].getSessionID();
                playerInListBitmap[ID >> 3] &= ~(1 << (ID & 7));
                outStream.writeBits(1, 1);
                outStream.writeBits(2, 3);
            }
        }
        for (int i = 0; i < MAX_PLAYERS; i++) {
            final Client p1 = PlayerManager.getClient(i);
            if (p1 == null || p1.getPlayer() == null || p1 == this) continue;
            final int ID = p1.getSessionID();
            if ((playerInListBitmap[ID >> 3] & (1 << (ID & 7))) != 0) continue;
            if (!withinDistance(p1)) continue;
            addNewPlayer(p1, outStream, updateBlock);
        }
        if (updateBlock.currentOffset > 0) {
            outStream.writeBits(11, 2047);
            outStream.finishBitAccess();
            outStream.writeBytes(updateBlock.buffer, updateBlock.currentOffset, 0);
        } else {
            outStream.finishBitAccess();
        }
        outStream.endFrameVarSizeWord();
        flushOutStream();
    }

    private void directFlushOutStream() throws IOException {
        out.write(outStream.buffer, 0, outStream.currentOffset);
        outStream.currentOffset = 0;
    }

    private void fillInStream(int forceRead) throws IOException {
        inStream.currentOffset = 0;
        in.read(inStream.buffer, 0, forceRead);
    }

    public boolean withinDistance(final Client otherPlr) {
        if (getPlayer().getHeight() != otherPlr.getPlayer().getHeight()) return false;
        int deltaX = otherPlr.getPlayer().getX() - getPlayer().getX();
        int deltaY = otherPlr.getPlayer().getY() - getPlayer().getY();
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    public void movePlayer(final int x, final int y) {
        teleportToX = x;
        teleportToY = y;
    }

    public void getNextPlayerMovement() {
        mapRegionDidChange = false;
        didTeleport = false;
        dir1 = dir2 = -1;
        if (teleportToX != -1 && teleportToY != -1) {
            mapRegionDidChange = true;
            if (mapRegionX != -1 && mapRegionY != -1) {
                int relX = teleportToX - mapRegionX * 8, relY = teleportToY - mapRegionY * 8;
                if (relX >= 2 * 8 && relX < 11 * 8 && relY >= 2 * 8 && relY < 11 * 8) mapRegionDidChange = false;
            }
            if (mapRegionDidChange) {
                mapRegionX = (teleportToX >> 3) - 6;
                mapRegionY = (teleportToY >> 3) - 6;
                playerListSize = 0;
            }
            currentX = teleportToX - 8 * mapRegionX;
            currentY = teleportToY - 8 * mapRegionY;
            getPlayer().setX(teleportToX);
            getPlayer().setY(teleportToY);
            resetWalkingQueue();
            teleportToX = teleportToY = -1;
            didTeleport = true;
        } else {
            dir1 = getNextWalkingDirection();
            if (dir1 == -1) return;
            if (isRunning) dir2 = getNextWalkingDirection();
            int deltaX = 0, deltaY = 0;
            if (currentX < 2 * 8) {
                deltaX = 4 * 8;
                mapRegionX -= 4;
                mapRegionDidChange = true;
            } else if (currentX >= 11 * 8) {
                deltaX = -4 * 8;
                mapRegionX += 4;
                mapRegionDidChange = true;
            }
            if (currentY < 2 * 8) {
                deltaY = 4 * 8;
                mapRegionY -= 4;
                mapRegionDidChange = true;
            } else if (currentY >= 11 * 8) {
                deltaY = -4 * 8;
                mapRegionY += 4;
                mapRegionDidChange = true;
            }
            if (mapRegionDidChange) {
                currentX += deltaX;
                currentY += deltaY;
                for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
                    walkingQueueX[i] += deltaX;
                    walkingQueueY[i] += deltaY;
                }
            }
        }
    }

    public void updateThisPlayerMovement(final Stream str) {
        if (mapRegionDidChange) {
            str.createFrame(73);
            str.writeWordA(mapRegionX + 6);
            str.writeWord(mapRegionY + 6);
        }
        if (didTeleport) {
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            str.writeBits(1, 1);
            str.writeBits(2, 3);
            str.writeBits(2, getPlayer().getHeight());
            str.writeBits(1, 1);
            str.writeBits(1, (updateRequired) ? 1 : 0);
            str.writeBits(7, currentY);
            str.writeBits(7, currentX);
            return;
        }
        if (dir1 == -1) {
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            if (updateRequired) {
                str.writeBits(1, 1);
                str.writeBits(2, 0);
            } else str.writeBits(1, 0);
        } else {
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            str.writeBits(1, 1);
            if (dir2 == -1) {
                str.writeBits(2, 1);
                str.writeBits(3, Calculations.XLATE_DIRECTION[dir1]);
                if (updateRequired) str.writeBits(1, 1); else str.writeBits(1, 0);
            } else {
                str.writeBits(2, 2);
                str.writeBits(3, Calculations.XLATE_DIRECTION[dir1]);
                str.writeBits(3, Calculations.XLATE_DIRECTION[dir2]);
                if (updateRequired) str.writeBits(1, 1); else str.writeBits(1, 0);
            }
        }
    }

    public void updatePlayerMovement(final Stream str) {
        if (dir1 == -1) {
            if (updateRequired || chatTextUpdateRequired) {
                str.writeBits(1, 1);
                str.writeBits(2, 0);
            } else {
                str.writeBits(1, 0);
            }
        } else if (dir2 == -1) {
            str.writeBits(1, 1);
            str.writeBits(2, 1);
            str.writeBits(3, Calculations.XLATE_DIRECTION[dir1]);
            str.writeBits(1, (updateRequired || chatTextUpdateRequired) ? 1 : 0);
        } else {
            str.writeBits(1, 1);
            str.writeBits(2, 2);
            str.writeBits(3, Calculations.XLATE_DIRECTION[dir1]);
            str.writeBits(3, Calculations.XLATE_DIRECTION[dir2]);
            str.writeBits(1, (updateRequired || chatTextUpdateRequired) ? 1 : 0);
        }
    }

    public void addNewPlayer(final Client plr, final Stream str, final Stream updateBlock) {
        playerInListBitmap[plr.getSessionID() >> 3] |= 1 << (plr.getSessionID() & 7);
        playerList[playerListSize++] = plr;
        str.writeBits(11, plr.getSessionID());
        str.writeBits(1, 1);
        boolean savedFlag = plr.appearanceUpdateRequired;
        boolean savedUpdateRequired = plr.updateRequired();
        plr.appearanceUpdateRequired = true;
        plr.setUpdateRequired(true);
        plr.appendPlayerUpdateBlock(updateBlock);
        plr.appearanceUpdateRequired = savedFlag;
        plr.setUpdateRequired(savedUpdateRequired);
        str.writeBits(1, 1);
        int z = plr.getPlayer().getY() - getPlayer().getY();
        if (z < 0) z += 32;
        str.writeBits(5, z);
        z = plr.getPlayer().getX() - getPlayer().getX();
        if (z < 0) z += 32;
        str.writeBits(5, z);
    }

    protected void appendPlayerAppearance(final Stream str) {
        playerProps.currentOffset = 0;
        playerProps.writeByte(playerAppearance[0]);
        playerProps.writeByte(1);
        if (getPlayer().getWearing(EQUIP_HEAD) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_HEAD));
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_BACK) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_BACK));
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_NECK) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_NECK));
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_WEAPON) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_WEAPON));
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_PLATE) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_PLATE));
        } else {
            playerProps.writeWord(0x100 + playerAppearance[2]);
        }
        if (getPlayer().getWearing(EQUIP_SHIELD) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_SHIELD));
        } else {
            playerProps.writeByte(0);
        }
        if (!Item.isPlate(getPlayer().getWearing(EQUIP_PLATE))) {
            playerProps.writeWord(0x100 + playerAppearance[3]);
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_LEGS) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_LEGS));
        } else {
            playerProps.writeWord(0x100 + playerAppearance[5]);
        }
        if (!Item.isFullHelm(getPlayer().getWearing(EQUIP_HEAD)) && !Item.isFullMask(getPlayer().getWearing(EQUIP_HEAD))) {
            playerProps.writeWord(0x100 + playerAppearance[1]);
        } else {
            playerProps.writeByte(0);
        }
        if (getPlayer().getWearing(EQUIP_HANDS) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_HANDS));
        } else {
            playerProps.writeWord(0x100 + playerAppearance[4]);
        }
        if (getPlayer().getWearing(EQUIP_FEET) >= 0) {
            playerProps.writeWord(0x200 + getPlayer().getWearing(EQUIP_FEET));
        } else {
            playerProps.writeWord(0x100 + playerAppearance[6]);
        }
        if (!Item.isFullHelm(getPlayer().getWearing(EQUIP_HEAD)) && !Item.isFullMask(getPlayer().getWearing(EQUIP_HEAD)) && playerAppearance[0] != 1) {
            playerProps.writeWord(0x100 + playerAppearance[7]);
        } else {
            playerProps.writeByte(0);
        }
        playerProps.writeByte(playerAppearance[8]);
        playerProps.writeByte(playerAppearance[9]);
        playerProps.writeByte(playerAppearance[10]);
        playerProps.writeByte(playerAppearance[11]);
        playerProps.writeByte(playerAppearance[12]);
        playerProps.writeWord(0x328);
        playerProps.writeWord(0x337);
        playerProps.writeWord(0x333);
        playerProps.writeWord(0x334);
        playerProps.writeWord(0x335);
        playerProps.writeWord(0x336);
        playerProps.writeWord(0x338);
        playerProps.writeQWord(Calculations.playerNameToInt64(getPlayer().getName()));
        playerProps.writeByte(3);
        playerProps.writeWord(0);
        str.writeByteC(playerProps.currentOffset);
        str.writeBytes(playerProps.buffer, playerProps.currentOffset, 0);
    }

    private void appendPlayerChatText(final Stream str) {
        str.writeWordBigEndian(((chatTextColor & 0xFF) << 8) + (chatTextEffects & 0xFF));
        str.writeByte(0);
        str.writeByteC(chatTextSize);
        str.writeBytes_reverse(chatText, chatTextSize, 0);
    }

    public void appendPlayerUpdateBlock(final Stream str) {
        if (!updateRequired && !chatTextUpdateRequired) return;
        int updateMask = 0;
        if (chatTextUpdateRequired) updateMask |= 0x80;
        if (appearanceUpdateRequired) updateMask |= 0x10;
        if (updateMask >= 0x100) {
            updateMask |= 0x40;
            str.writeByte(updateMask & 0xFF);
            str.writeByte(updateMask >> 8);
        } else {
            str.writeByte(updateMask);
        }
        if (chatTextUpdateRequired) appendPlayerChatText(str);
        if (appearanceUpdateRequired) appendPlayerAppearance(str);
    }

    public void clearUpdateFlags() {
        updateRequired = false;
        chatTextUpdateRequired = false;
        appearanceUpdateRequired = false;
    }

    public void preProcessing() {
        newWalkCmdSteps = 0;
        if (logoutTimer++ >= 500) destruct();
    }

    public void postProcessing() {
        if (newWalkCmdSteps > 0) {
            int firstX = newWalkCmdX[0], firstY = newWalkCmdY[0];
            int lastDir = 0;
            boolean found = false;
            numTravelBackSteps = 0;
            int ptr = wQueueReadPtr;
            int dir = Calculations.direction(currentX, currentY, firstX, firstY);
            if (dir != -1 && (dir & 1) != 0) {
                do {
                    lastDir = dir;
                    if (--ptr < 0) ptr = WALKING_QUEUE_SIZE - 1;
                    travelBackX[numTravelBackSteps] = walkingQueueX[ptr];
                    travelBackY[numTravelBackSteps++] = walkingQueueY[ptr];
                    dir = Calculations.direction(walkingQueueX[ptr], walkingQueueY[ptr], firstX, firstY);
                    if (lastDir != dir) {
                        found = true;
                        break;
                    }
                } while (ptr != wQueueWritePtr);
            } else {
                wQueueWritePtr = wQueueReadPtr;
                addToWalkingQueue(currentX, currentY);
                if (dir != -1 && (dir & 1) != 0) {
                    for (int i = 0; i < numTravelBackSteps - 1; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                    int wayPointX2 = travelBackX[numTravelBackSteps - 1];
                    int wayPointY2 = travelBackY[numTravelBackSteps - 1];
                    int wayPointX1, wayPointY1;
                    wayPointX1 = numTravelBackSteps == 1 ? currentX : travelBackX[numTravelBackSteps - 2];
                    wayPointY1 = numTravelBackSteps == 1 ? currentY : travelBackY[numTravelBackSteps - 2];
                    dir = Calculations.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
                    if (dir != -1 && (dir & 1) == 0) {
                        dir >>= 1;
                        found = false;
                        int x = wayPointX1, y = wayPointY1;
                        while (x != wayPointX2 || y != wayPointY2) {
                            x += Calculations.DIRECTION_DELTAX[dir];
                            y += Calculations.DIRECTION_DELTAY[dir];
                            if ((Calculations.direction(x, y, firstX, firstY) & 1) == 0) {
                                found = true;
                                break;
                            }
                        }
                        if (found) addToWalkingQueue(wayPointX1, wayPointY1);
                    }
                } else {
                    for (int i = 0; i < numTravelBackSteps; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                }
                for (int i = 0; i < newWalkCmdSteps; i++) {
                    addToWalkingQueue(newWalkCmdX[i], newWalkCmdY[i]);
                }
            }
            isRunning = newWalkCmdIsRunning;
        }
    }

    protected void updateFriends() {
        final long me = Calculations.playerNameToInt64(getPlayer().getName());
        for (int i = 0; i < MAX_PLAYERS; i++) {
            final Client c = PlayerManager.getClient(i);
            if (c == null || c.isDisconnected()) continue;
            if (c.hasFriend(me)) {
                changeWorld(me);
            }
        }
    }

    protected void sendText(final int ID, final String s) {
        outStream.createFrameVarSizeWord(126);
        outStream.writeString(s);
        outStream.writeWordA(ID);
        outStream.endFrameVarSizeWord();
        flushOutStream();
    }

    protected void Logout() {
        outStream.createFrame(109);
        destruct();
    }

    public void destruct() {
        if (mySock == null) return;
        try {
            playerLog("Client", "Player has logged out.");
            disconnected = true;
            if (in != null) in.close();
            if (out != null) out.close();
            mySock.close();
            saveChar();
            updateFriends();
        } catch (Exception e) {
        }
    }

    public void saveChar() {
        try {
            final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./characters/" + getPlayer().getName() + ".pcf"));
            out.writeObject((PlayerProps) getPlayer());
            out.close();
        } catch (Exception e) {
        }
    }

    public abstract void parseIncomingPackets(final int packetType, int packetSize);

    protected abstract void prepareClient();
}
