package com.aionemu.gameserver.network.aion;

import java.nio.ByteBuffer;
import java.util.Collection;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * @author kosyachok
 * 
 */
public abstract class MailServicePacket extends InventoryPacket {

    protected void writeLettersList(ByteBuffer buf, Collection<Letter> letters, Player player) {
        writeC(buf, 2);
        writeD(buf, player.getObjectId());
        writeC(buf, 0);
        writeH(buf, player.getMailbox().getFreeSlots());
        for (Letter letter : letters) {
            writeD(buf, letter.getObjectId());
            writeS(buf, letter.getSenderName());
            writeS(buf, letter.getTitle());
            if (letter.isUnread()) writeC(buf, 0); else writeC(buf, 1);
            if (letter.getAttachedItem() != null) {
                writeD(buf, letter.getAttachedItem().getObjectId());
                writeD(buf, letter.getAttachedItem().getItemTemplate().getTemplateId());
            } else {
                writeD(buf, 0);
                writeD(buf, 0);
            }
            writeD(buf, (int) letter.getAttachedKinah());
            writeD(buf, 0);
            writeC(buf, 0);
        }
    }

    protected void writeEmptyLettersList(ByteBuffer buf, Player player) {
        writeC(buf, 2);
        writeD(buf, player.getObjectId());
        writeH(buf, 0);
        writeC(buf, 0);
    }

    protected void writeMailMessage(ByteBuffer buf, int messageId) {
        writeC(buf, 1);
        writeC(buf, messageId);
    }

    protected void writeMailboxState(ByteBuffer buf, int haveNewMail, int haveUnread) {
        writeC(buf, 0);
        writeC(buf, haveNewMail);
        writeC(buf, 0);
        writeC(buf, haveUnread);
        writeD(buf, 0);
        writeC(buf, 0);
    }

    protected void writeLetterRead(ByteBuffer buf, Letter letter, long time) {
        writeC(buf, 3);
        writeD(buf, letter.getRecipientId());
        writeD(buf, 1);
        writeD(buf, 0);
        writeD(buf, letter.getObjectId());
        writeD(buf, letter.getRecipientId());
        writeS(buf, letter.getSenderName());
        writeS(buf, letter.getTitle());
        writeS(buf, letter.getMessage());
        Item item = letter.getAttachedItem();
        if (item != null) {
            ItemTemplate itemTemplate = item.getItemTemplate();
            writeMailGeneralInfo(buf, item);
            if (itemTemplate.isArmor()) writeArmorInfo(buf, item, false, false, true); else if (itemTemplate.isWeapon()) writeWeaponInfo(buf, item, false, false, false, true); else writeGeneralItemInfo(buf, item, false, true);
        } else {
            writeD(buf, 0);
            writeD(buf, 0);
            writeD(buf, 0);
            writeD(buf, 0);
            writeD(buf, 0);
        }
        writeD(buf, (int) letter.getAttachedKinah());
        writeD(buf, 0);
        writeC(buf, 0);
        writeQ(buf, time / 1000);
        writeC(buf, 0);
    }

    protected void writeLetterState(ByteBuffer buf, int letterId, int attachmentType) {
        writeC(buf, 5);
        writeD(buf, letterId);
        writeC(buf, attachmentType);
        writeC(buf, 1);
    }

    protected void writeLetterDelete(ByteBuffer buf, int letterId) {
        writeC(buf, 6);
        writeD(buf, 0);
        writeD(buf, 0);
        writeD(buf, letterId);
    }
}
