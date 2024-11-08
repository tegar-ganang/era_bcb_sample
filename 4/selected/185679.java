package client.communication.tasks;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import client.communication.ChannelTypeForMsgTypes;
import client.communication.ClientCommunication;
import client.communication.GameContext;
import client.communication.exceptions.SendMessageException;
import com.sun.sgs.client.ClientChannel;
import common.messages.IMessage;

/**
 * Esta clase representa una tarea que ejecuta el env�o de un mensaje a un canal
 * de comunicaci�n. Se entiende canal en el contexto del framework Darkstar,
 * donde en este caso el cliente env�a un mensaje a todos los suscritos.
 * 
 * @author lito
 */
public class TaskChannelSender extends TaskCommunication {

    /** El tipo de los canales a los que esta tarea enviar� los mensajes. */
    private String channelType;

    /** El {@link Logger} para esta clase. */
    private static final Logger LOGGER = Logger.getLogger(TaskChannelSender.class.getName());

    /**
	 * Constructor que inicializa el estado interno con el mensaje pasado como
	 * par�metro. La instancia se configura para enviar a los canales del tipo
	 * {@code aChannelType}.
	 * 
	 * @param aChannelType el tipo de los canales a utilizar
	 * @param msgToSend el mensaje a enviar
	 * 
	 * @author Diego
	 */
    public TaskChannelSender(final String aChannelType, final IMessage msgToSend) {
        super(msgToSend);
        this.setChannelType(aChannelType);
    }

    /**
	 * Constructor que inicializa el estado interno con el mensaje pasado como
	 * par�metro. La instancia se configura para enviar a los canales de tipo
	 * apropiado para {@code msgToSend}.
	 * 
	 * @param msgToSend el mensaje a enviar
	 * 
	 * @see client.communication.ChannelTypeForMsgTypes
	 * 
	 * @author Diego
	 */
    public TaskChannelSender(final IMessage msgToSend) {
        super(msgToSend);
        String chType = ChannelTypeForMsgTypes.getInstance().getChannelTypeForMsgType(msgToSend.getType());
        this.setChannelType(chType);
    }

    /**
	 * Ejecuta la tarea. Para esta clase eso significa enviar el mensaje.
	 * 
	 * @see client.game.task.ITask#execute()
	 * @author Diego
	 */
    @Override
    public void execute() {
        ClientCommunication client = GameContext.getClientCommunication();
        Set<ClientChannel> channels = client.getChannelConteiner().getChannelsOfType(this.getChannelType());
        if (channels == null) throw new NullPointerException("Si pas� esto es porque nos esta arruinando al concurrencia.");
        for (ClientChannel cc : channels) {
            try {
                client.sendEvent(this.getMessage(), cc);
            } catch (SendMessageException e) {
                LOGGER.log(Level.WARNING, "No se pudo enviar el mensaje: {0}", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
	 * Retorna el tipo de los canales a los que esta tarea enviar� los mensajes.
	 * 
	 * @return un {@code String} representando un tipo de canal
	 */
    public final String getChannelType() {
        return channelType;
    }

    /**
	 * Fija el tipo de los canales a los que esta tarea enviar� los mensajes.
	 * 
	 * @param aChannelType un {@code String} representando el tipo de canal a
	 *        utilizar
	 */
    public final void setChannelType(final String aChannelType) {
        this.channelType = aChannelType;
    }

    /**
	 * Sigue el patr�n FactoryMethod. Este m�todo se utiliza para obtener las
	 * nuevas instancias de {@code ChannelSenderTask}.
	 * 
	 * @see client.communication.tasks.TaskCommunication
	 *      #factoryMethod(common.messages.IMessage)
	 * @param msg el mensaje a enviar
	 * @return Una nueva instancia de esta clase, configurada con el mensaje
	 *         pasado como par�metro.
	 * @author Diego
	 */
    @Override
    public TaskCommunication factoryMethod(final IMessage msg) {
        return new TaskChannelSender(msg);
    }
}
