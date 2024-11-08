package com.db4o.internal.cs.messages;

public final class MWriteUpdateDeleteMembers extends MsgD implements ServerSideMessage {

    public final boolean processAtServer() {
        synchronized (streamLock()) {
            transaction().writeUpdateDeleteMembers(readInt(), stream().classMetadataForId(readInt()), readInt(), readInt());
        }
        return true;
    }
}
