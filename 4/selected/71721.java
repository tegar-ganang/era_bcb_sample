package nakayo.gameserver.network.aion.serverpackets;

import nakayo.gameserver.model.gameobjects.Letter;
import nakayo.gameserver.model.gameobjects.player.Player;
import nakayo.gameserver.model.templates.mail.MailMessage;
import nakayo.gameserver.network.aion.AionConnection;
import nakayo.gameserver.network.aion.MailServicePacket;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author kosyachok
 */
public class SM_MAIL_SERVICE extends MailServicePacket {

    private int serviceId;

    private Player player;

    private Collection<Letter> letters;

    private int mailCount;

    private int unreadCount;

    private boolean hasExpress;

    private int mailMessage;

    private Letter letter;

    private long time;

    private int letterId;

    private int attachmentType;

    public SM_MAIL_SERVICE(int mailCount, int unreadCount, boolean hasExpress) {
        this.serviceId = 0;
        this.mailCount = mailCount;
        this.unreadCount = unreadCount;
        this.hasExpress = hasExpress;
    }

    /**
     * Send mailMessage(ex. Send OK, Mailbox full etc.)
     *
     * @param mailMessage
     */
    public SM_MAIL_SERVICE(MailMessage mailMessage) {
        this.serviceId = 1;
        this.mailMessage = mailMessage.getId();
    }

    /**
     * Send mailbox info
     *
     * @param player
     * @param letters
     */
    public SM_MAIL_SERVICE(Player player, Collection<Letter> letters) {
        this.serviceId = 2;
        this.player = player;
        this.letters = letters;
    }

    /**
     * used when reading letter
     *
     * @param player
     * @param letter
     * @param time
     */
    public SM_MAIL_SERVICE(Player player, Letter letter, long time) {
        this.serviceId = 3;
        this.player = player;
        this.letter = letter;
        this.time = time;
    }

    /**
     * used when getting attached items
     *
     * @param letterId
     * @param attachmentType
     */
    public SM_MAIL_SERVICE(int letterId, int attachmentType) {
        this.serviceId = 5;
        this.letterId = letterId;
        this.attachmentType = attachmentType;
    }

    /**
     * used when deleting letter
     *
     * @param letterId
     */
    public SM_MAIL_SERVICE(int letterId) {
        this.serviceId = 6;
        this.letterId = letterId;
    }

    @Override
    public void writeImpl(AionConnection con, ByteBuffer buf) {
        switch(serviceId) {
            case 0:
                writeMailboxState(buf, mailCount, unreadCount, hasExpress);
                break;
            case 1:
                writeMailMessage(buf, mailMessage);
                break;
            case 2:
                if (letters.size() > 0) writeLettersList(buf, letters, player); else writeEmptyLettersList(buf, player);
                break;
            case 3:
                writeLetterRead(buf, letter, time);
                break;
            case 5:
                writeLetterState(buf, letterId, attachmentType);
                break;
            case 6:
                writeLetterDelete(buf, letterId);
                break;
        }
    }
}
