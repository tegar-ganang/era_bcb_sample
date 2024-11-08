package jpcsp.HLE.kernel.types;

import jpcsp.Memory;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelMbxInfo extends pspAbstractMemoryMappedStructureVariableLength {

    public final String name;

    public final int attr;

    public int numWaitThreads;

    private int numMessages;

    private int firstMessageAddr;

    public final int uid;

    public int lastMessageAddr;

    public SceKernelMbxInfo(String name, int attr) {
        this.name = name;
        this.attr = attr;
        numWaitThreads = 0;
        numMessages = 0;
        firstMessageAddr = 0;
        lastMessageAddr = 0;
        uid = SceUidManager.getNewUid("ThreadMan-Mbx");
    }

    @Override
    protected void write() {
        super.write();
        writeStringNZ(32, name);
        write32(attr);
        write32(numWaitThreads);
        write32(numMessages);
        write32(firstMessageAddr);
    }

    public int removeMsg(Memory mem) {
        int msgAddr = firstMessageAddr;
        if (msgAddr != 0) {
            SceKernelMsgPacket packet = new SceKernelMsgPacket();
            packet.read(mem, msgAddr);
            firstMessageAddr = packet.nextMsgPacketAddr;
            if (firstMessageAddr == 0) {
                lastMessageAddr = 0;
            }
            packet.nextMsgPacketAddr = 0;
            packet.write(mem);
            numMessages--;
        }
        return msgAddr;
    }

    private void insertMsgAfter(Memory mem, int msgAddr, int refMsgAddr) {
        SceKernelMsgPacket msgPacket = new SceKernelMsgPacket();
        msgPacket.read(mem, msgAddr);
        if (lastMessageAddr == 0) {
            msgPacket.nextMsgPacketAddr = 0;
            firstMessageAddr = msgAddr;
            lastMessageAddr = msgAddr;
        } else if (refMsgAddr == 0) {
            msgPacket.nextMsgPacketAddr = firstMessageAddr;
            firstMessageAddr = msgAddr;
        } else {
            SceKernelMsgPacket refMsgPacket = new SceKernelMsgPacket();
            refMsgPacket.read(mem, refMsgAddr);
            msgPacket.nextMsgPacketAddr = refMsgPacket.nextMsgPacketAddr;
            refMsgPacket.nextMsgPacketAddr = msgAddr;
            refMsgPacket.write(mem);
            if (lastMessageAddr == refMsgAddr) {
                lastMessageAddr = msgAddr;
            }
        }
        msgPacket.write(mem);
        numMessages++;
    }

    public void addMsg(Memory mem, int msgAddr) {
        if (msgAddr != 0) {
            insertMsgAfter(mem, msgAddr, lastMessageAddr);
        }
    }

    public void addMsgByPriority(Memory mem, int msgAddr) {
        if (msgAddr != 0) {
            SceKernelMsgPacket msgPacket = new SceKernelMsgPacket();
            msgPacket.read(mem, msgAddr);
            SceKernelMsgPacket currentMsgPacket = new SceKernelMsgPacket();
            int currentMsgAddr = firstMessageAddr;
            int previousMsgAddr = 0;
            for (int i = 0; i < numMessages; i++) {
                currentMsgPacket.read(mem, currentMsgAddr);
                if (msgPacket.compare(msgPacket, currentMsgPacket) < 0) {
                    break;
                }
                previousMsgAddr = currentMsgAddr;
            }
            insertMsgAfter(mem, msgAddr, previousMsgAddr);
        }
    }

    public boolean hasMessage() {
        return numMessages > 0;
    }
}
