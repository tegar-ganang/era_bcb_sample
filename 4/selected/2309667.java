package ar.edu.unicen.exa.server.grid;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Logger;
import ar.edu.unicen.exa.server.grid.id.IBindingID;
import ar.edu.unicen.exa.server.grid.id.IDManager;
import com.jme.math.Vector3f;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedReference;
import common.messages.IMessage;
import common.util.ChannelNameParser;

/**
 * Representa una zona fisica del mundo({@link IGridStructure}). Esta zona esta
 * delimitada por los bounds. Ademas, esta zona esta asociada a un unico
 * {@code Channel} , es decir hay una correspondencia uno a uno entre celdas
 * y canales({@link Channel}).
 * 
 * @author Sebastián Perruolo &lt;sebastianperruolo at gmail dot com &gt;
 * @encoding UTF-8
 */
public class Cell implements Serializable, IBindingID {

    /**  Para cumplir con la version de la clase Serializable. */
    private static final long serialVersionUID = 1301727798124952702L;

    /** Logger. */
    private static Logger logger = Logger.getLogger(Cell.class.getName());

    /**
	 * Es una referencia {@code ManagedReference} a la {@link IGridStructure}
	 * contenedora de la celda.
	 * 
	 */
    private ManagedReference<IGridStructure> refStructure;

    /**
	 * Es la identificacion unica de una celda. No se debe repetir para niguna
	 * celda de una misma estructura.
	 * 
	 */
    private String id;

    /**
	 * Es una referencia {@code ManagedReference} al {@code Channel} asociado a
	 * la celda.
	 */
    private ManagedReference<Channel> refChannel;

    /**
	 * Determina el espacio circundado por la celda en el espacio fisico del
	 * mundo({@link IGridStructure}).
	 */
    private Rectangle bounds;

    /**
	 * Creador.
	 * 
	 * @param cellBunds límites de la celda.
	 * @param parent Estructura a la que pertenece la celda.
	 */
    public Cell(final Rectangle cellBunds, final IGridStructure parent) {
        IDManager.setNewID(this);
        this.bounds = cellBunds;
        logger.info("Celda " + id + " -> x= " + bounds.getX() + " y= " + bounds.getY());
        if (parent != null) {
            refStructure = AppContext.getDataManager().createReference(parent);
        }
        ChannelManager channelMgr = AppContext.getChannelManager();
        String channelName = ChannelNameParser.MOVE_CHANNEL_IDENTIFIER + '_' + id;
        Channel channel = channelMgr.createChannel(channelName, new ChannelMessageListener(), Delivery.RELIABLE);
        this.setChannel(channel);
    }

    /**
	 * Retorna el identificador de la celda.
	 * 
	 * @return el identificador de esta instancia de celda.
	 */
    @Override
    public final long getId() {
        return Long.parseLong(id);
    }

    /**
	 * Setea el identificador de la celda.
	 * @param anId el identificador de esta instancia de celda.
	 */
    @Override
    public final void setId(final long anId) {
        this.id = Long.toString(anId);
    }

    /**
	 * Retorna la referencia {@code ManagedReference} del canal asociado a la
	 * celda.
	 * 
	 * @return el Channel de esta celda.
	 */
    public final Channel getChannel() {
        return refChannel.get();
    }

    /**
	 * Asocia un canal({@link Channel}) a la celda. Dado que el canal es un
	 * objeto {@code ManagedObject} se debe crear la referencia {@code 
	 * ManagedReference} a ese canal invocando al metodo {@code 
	 * createReference()} del {@code DataManager} .
	 * 
	 * @param cellChannel canal que se debe asociar a la celda.
	 */
    public final void setChannel(final Channel cellChannel) {
        this.refChannel = AppContext.getDataManager().createReference(cellChannel);
    }

    /**
	 * Retorna el espacio circundado por la celda.
	 * 
	 * @return Los límites de esta celda.
	 */
    public final Rectangle getBounds() {
        return bounds;
    }

    /**
	 * Establece el espacio circundado por la celda.
	 * 
	 * @param cellBounds Límites de esta celda.
	 */
    public final void setBounds(final Rectangle cellBounds) {
        this.bounds = cellBounds;
    }

    /**
	 * Retorna la referencia a la {@link IGridStructure} contenedora de la
	 * celda. Dado que la estructura es un objeto {@code ManagedObject} dicha
	 * referencia debe ser de tipo {@code ManagedReference} .
	 * 
	 * @return referencia a la estructura contenedora.
	 */
    public final IGridStructure getStructure() {
        return refStructure.get();
    }

    /**
	 * Subscribe al {@link Player} pasado por parametro {@code ClientSession}
	 * al canal({@link Channel}) contenido por la celda.
	 * 
	 * @param client {@link Player} a subscribir.
	 */
    public final void joinToChannel(final ClientSession client) {
        getChannel().join(client);
    }

    /**
	 * Desubscribe al {@link Player} pasado por parametro {@code 
	 * ClientSession} del canal contenido por la celda.
	 * 
	 * @param client {@link Player} a desuscribir.
	 */
    public final void leaveFromChannel(final ClientSession client) {
        if (client != null) {
            getChannel().leave(client);
        }
    }

    /**
	 * Determina si la posicion dada esta dentro de la celda. Para ello utiliza
	 * la variable de instancia {@link bounds}
	 * 
	 * @return true si la posición dada está dentro de esta celda. 
	 * false en otro caso.
	 * 
	 * @param position posición a evaluar.
	 */
    public final boolean isInside(final Vector3f position) {
        return bounds.contains(position.getX(), position.getZ());
    }

    /**
	 * Envia el mensaje {@code IMessage} del jugador dado a todos los {@link 
	 * Player}s asociados al canal({@link Channel}) contenido por la celda.
	 * 
	 * @param msg mensaje a enviar.
	 * @param player {@link Player} que disparó el mensaje. null si se desea
	 * 		que se envíe el mensaje sin comprobar que el emisor pertenezca al 
	 * 		channel
	 */
    public final void send(final IMessage msg, final ClientSession player) {
        try {
            Iterator<ClientSession> i = getChannel().getSessions();
            StringBuffer logMsg = new StringBuffer(this.getChannel().getName());
            logMsg.append("->Enviando msg ").append(msg.getType()).append(" los usuarios: ");
            boolean empty = true;
            while (i.hasNext()) {
                empty = false;
                logMsg.append(" ");
                logMsg.append(i.next().getName());
            }
            if (empty) {
                logMsg.append("-nadie-");
            }
            logger.info(logMsg.toString());
            getChannel().send(player, msg.toByteBuffer());
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    /**
	 * Indica si este objeto es igual que el objeto recibido por
	 * parámetro.
	 * 
	 * @param obj Objeto a comparar con este objeto.
	 * @return true si el objeto obj es igual que este objeto, 
	 * 		false en otro caso.
	 */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Cell) {
            Cell other = (Cell) obj;
            return this.getId() == other.getId();
        }
        return false;
    }

    /**
	 * Retorna un valor <i>hash code</i> para este objeto.
	 * @return un valor <i>hash code</i> para este objeto.
	 * @see Object#hashCode()
	 */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
