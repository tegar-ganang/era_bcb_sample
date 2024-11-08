package com.turnengine.client.local.message.bean;

import com.javabi.common.io.data.IDataReader;
import com.javabi.common.io.data.IDataWriter;
import com.turnengine.client.local.message.enums.LocalMessageStatus;
import com.turnengine.client.local.message.enums.LocalMessageType;
import java.io.IOException;

/**
 * The Local Message Header Serializer.
 */
public class LocalMessageHeaderSerializer implements ILocalMessageHeaderSerializer {

    @Override
    public ILocalMessageHeader readObject(IDataReader reader) throws IOException {
        int id = reader.readInt();
        int senderId = reader.readInt();
        int recipientId = reader.readInt();
        String subject = reader.readString(true);
        int threadId = reader.readInt();
        long timestamp = reader.readLong();
        LocalMessageStatus status = reader.readEnum(LocalMessageStatus.class, true);
        LocalMessageType type = reader.readEnum(LocalMessageType.class, true);
        ILocalMessageHeader object = new LocalMessageHeader();
        object.setId(id);
        object.setSenderId(senderId);
        object.setRecipientId(recipientId);
        object.setSubject(subject);
        object.setThreadId(threadId);
        object.setTimestamp(timestamp);
        object.setStatus(status);
        object.setType(type);
        return object;
    }

    public void writeObject(IDataWriter writer, ILocalMessageHeader object) throws IOException {
        int id = object.getId();
        int senderId = object.getSenderId();
        int recipientId = object.getRecipientId();
        String subject = object.getSubject();
        int threadId = object.getThreadId();
        long timestamp = object.getTimestamp();
        LocalMessageStatus status = object.getStatus();
        LocalMessageType type = object.getType();
        writer.writeInt(id);
        writer.writeInt(senderId);
        writer.writeInt(recipientId);
        writer.writeString(subject, true);
        writer.writeInt(threadId);
        writer.writeLong(timestamp);
        writer.writeEnum(status, true);
        writer.writeEnum(type, true);
    }
}
