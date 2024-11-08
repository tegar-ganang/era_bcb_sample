package ru.caffeineim.protocols.icq.packet.sent.icbm;

import ru.caffeineim.protocols.icq.Flap;
import ru.caffeineim.protocols.icq.RawData;
import ru.caffeineim.protocols.icq.Snac;
import ru.caffeineim.protocols.icq.setting.enumerations.MessageChannelEnum;

/**
 * <p>Created by
 *   @author Fabrice Michellonet
 *   @author Samolisov Pavel
 * 	 @author Andreas Rossbacher
 *   @author Artyomov Denis
 *   @author Dmitry Tunin
 */
public abstract class SendMessage extends Flap {

    protected Snac snac;

    protected RawData channel;

    public SendMessage(String userId, MessageChannelEnum messageChannel) {
        super(2);
        snac = new Snac(0x04, 0x06, 0x00, 0x00, 0x00);
        snac.addRawDataToSnac(new RawData(0x00000000, RawData.DWORD_LENGHT));
        snac.addRawDataToSnac(new RawData(0x00000000, RawData.DWORD_LENGHT));
        channel = new RawData(messageChannel.getChannelNumber(), RawData.WORD_LENGHT);
        snac.addRawDataToSnac(channel);
        snac.addRawDataToSnac(new RawData(userId.length(), RawData.BYTE_LENGHT));
        snac.addRawDataToSnac(new RawData(userId));
    }
}
