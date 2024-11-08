package be.castanea.server;

import be.castanea.common.util.GenericEvent;
import be.castanea.common.JoinReplyEvent;
import be.castanea.common.PartEvent;
import be.castanea.common.util.ChannelEvent;
import be.castanea.common.util.MessageUtil;
import be.castanea.common.util.PlayerEvent;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Castanea
 * 2009
 * @author Geert van Leemputten, Steven Rymenans, Bart Van Hooydonck
 */
public class CastaneaPlayer implements ManagedObject, ClientSessionListener, Serializable {

    private static final long serialVersionUID = 1L;

    private ManagedReference<ClientSession> sessionRef;

    private String name;

    public CastaneaPlayer(ClientSession session) {
        this.name = session.getName();
        this.sessionRef = AppContext.getDataManager().createReference(session);
        AppContext.getDataManager().setBinding(name, this);
        AppContext.getChannelManager().getChannel(CastaneaServer.WORLD).join(session);
    }

    public void send(GenericEvent event) {
        sessionRef.get().send(MessageUtil.encode(event));
    }

    public void receivedMessage(ByteBuffer message) {
        GenericEvent event = MessageUtil.decode(message);
        if (event instanceof ChannelEvent) {
            AppContext.getChannelManager().getChannel(CastaneaServer.WORLD).send(sessionRef.get(), MessageUtil.encode(event));
        } else if (event instanceof PlayerEvent) {
            CastaneaPlayer player = (CastaneaPlayer) AppContext.getDataManager().getBinding(event.getRecipient());
            player.send(event);
        } else {
            throw new RuntimeException("Every event received by the server must implement either ChannelEvent or PlayerEvent.");
        }
    }

    public void disconnected(boolean reason) {
        System.out.println(name + " disconnected");
        PartEvent event = new PartEvent(name, "world");
        AppContext.getChannelManager().getChannel(CastaneaServer.WORLD).send(null, MessageUtil.encode(event));
    }
}
