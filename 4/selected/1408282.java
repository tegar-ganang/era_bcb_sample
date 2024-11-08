package com.l2jserver.gameserver.network.serverpackets;

import java.util.List;
import javolution.util.FastList;
import com.l2jserver.gameserver.instancemanager.MailManager;
import com.l2jserver.gameserver.model.entity.Message;

/**
 * @author Migi, DS
 */
public class ExShowSentPostList extends L2GameServerPacket {

    private static final String _S__FE_AC_EXSHOWSENTPOSTLIST = "[S] FE:AC ExShowSentPostList";

    private List<Message> _outbox;

    public ExShowSentPostList(int objectId) {
        _outbox = MailManager.getInstance().getOutbox(objectId);
    }

    @Override
    protected void writeImpl() {
        writeC(0xfe);
        writeH(0xac);
        writeD((int) (System.currentTimeMillis() / 1000));
        if (_outbox != null && _outbox.size() > 0) {
            writeD(_outbox.size());
            for (Message msg : _outbox) {
                writeD(msg.getId());
                writeS(msg.getSubject());
                writeS(msg.getReceiverName());
                writeD(msg.isLocked() ? 0x01 : 0x00);
                writeD(msg.getExpirationSeconds());
                writeD(msg.isUnread() ? 0x01 : 0x00);
                writeD(0x01);
                writeD(msg.hasAttachments() ? 0x01 : 0x00);
            }
        } else {
            writeD(0x00);
        }
        FastList.recycle((FastList<Message>) _outbox);
        _outbox = null;
    }

    @Override
    public String getType() {
        return _S__FE_AC_EXSHOWSENTPOSTLIST;
    }
}
