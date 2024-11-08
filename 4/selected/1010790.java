package net.sf.l2j.gameserver.gameserverpackets;

import java.io.IOException;
import net.sf.l2j.gameserver.LoginServerThread;

/**
 * 
 * @author Noctarius
 */
public class RequestLoginRestart extends GameServerBasePacket {

    public RequestLoginRestart() {
        writeC(0x07);
        writeS(LoginServerThread.getInstance().getServerName());
    }

    /**
	 * @see net.sf.l2j.gameserver.gameserverpackets.GameServerBasePacket#getContent()
	 */
    @Override
    public byte[] getContent() throws IOException {
        return getBytes();
    }
}
