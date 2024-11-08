package client.communication;

import java.net.PasswordAuthentication;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import client.communication.tasks.darkstarevents.TaskDisconnected;
import client.communication.tasks.darkstarevents.TaskLoggedIn;
import client.communication.tasks.darkstarevents.TaskLogginFailed;
import client.communication.tasks.darkstarevents.TaskReconnected;
import client.communication.tasks.darkstarevents.TaskReconnecting;
import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.ClientChannelListener;
import com.sun.sgs.client.ServerSessionListener;
import com.sun.sgs.client.simple.SimpleClient;
import com.sun.sgs.client.simple.SimpleClientListener;
import common.exceptions.MalformedMessageException;
import common.exceptions.UnsopportedMessageException;
import common.messages.IMessage;
import common.messages.MessageFactory;
import common.messages.MsgAbstract;
import common.messages.MsgEmpty;
import common.messages.MsgPlainText;
import common.processors.IProcessor;
import common.processors.MsgProcessorFactory;

/**
 * Esta clase esta pensada para ser el "Listener" del cliente y de todos los
 * canales a los cuales este estara subscripto.
 * 
 * @author lito
 */
public class ClientListener implements ClientChannelListener, SimpleClientListener {

    private ClientCommunication clientCommunication;

    /** El {@link Logger} para esta clase. */
    private static final Logger LOGGER = Logger.getLogger(ClientCommunication.class.getName());

    public ClientListener(ClientCommunication clientCommunication) {
        this.clientCommunication = clientCommunication;
    }

    /**
	 * Cada vez que un mensaje es recivido por el canal <I>channel</I> al cual
	 * se le asocio esta instancia como listener, se dispara este metodo.<BR/>
	 * Para procesar el mensaje internamente, se invoca el metodo
	 * {@link #processIncomeMsg(ByteBuffer)}.
	 * 
	 * @param channel
	 *            El canal por donde provino el mensaje.
	 * 
	 * @param bbMsg
	 *            El mensaje recibido como buffer de bytes.
	 * 
	 * @see ClientChannelListener#receivedMessage( ClientChannel arg0,ByteBuffer
	 *      arg1)
	 */
    @Override
    public final void receivedMessage(final ClientChannel channel, final ByteBuffer bbMsg) {
        this.processIncomeMsg(bbMsg);
    }

    /**
	 * Se crea la instancia de {@link IMessage} corespondiente al mensaje a
	 * travez de {@link MessageFactory}, luego se crea la instancia a
	 * {@link IProcessor} correspondiente (mediante la
	 * {@link MsgProcessorFactory}) al tipo de mensaje para que procese el
	 * mensaje.<BR/> En caso de ocurrir una excepcion del tipo
	 * {@link MalformedMessageException} o {@link UnsopportedMessageException}
	 * se logea la misma con nivel {@link Level#WARNING} a travez del
	 * {@link #LOGGER}.
	 * 
	 * @param bbMsg
	 *            El mensaje a procesar.
	 */
    private void processIncomeMsg(final ByteBuffer bbMsg) {
        try {
            IMessage msg = MessageFactory.getInstance().createMessage(bbMsg);
            MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
        } catch (MalformedMessageException e) {
            LOGGER.log(Level.WARNING, "Exception: {0}", e);
        } catch (UnsopportedMessageException e) {
            LOGGER.log(Level.WARNING, "Exception: {0}", e);
        }
    }

    /**
	 * Solicita a {@link GameContext} la instancia de
	 * {@link ClientCommunication}, a este le solicita su
	 * {@linl ChannelContainer}, y remueve del mismo el canal <I>channel</I>
	 * del cual el sevidor removio a este cliente.
	 * 
	 * @param channel
	 *            El canal del cual el servidor a removido al cliente.
	 * @see ClientChannelListener#leftChannel(ClientChannel arg0)
	 */
    @Override
    public final void leftChannel(final ClientChannel channel) {
        GameContext.getClientCommunication().getChannelConteiner().removeChannel(channel);
    }

    /**
	 * El metodo es invocado por {@link SimpleClient} cuando el servidor agrega
	 * al cliente a la canal <I>channel</I>.<BR/> Internamente, se solicita a
	 * {@link GameContext} la instancia de {@link ClientCommunication}, a este
	 * le pide su {@linl ChannelContainer}, y agrega al mismo el canal
	 * <I>channel</I> al cual el sevidor agrego a este cliente.
	 * 
	 * @param channel
	 *            El canal al que el servidor suscribio a este cliente.
	 * 
	 * @return esta instancia, <I>this</I>.
	 * 
	 * @see ServerSessionListener#joinedChannel(ClientChannel arg0)
	 */
    @Override
    public final ClientChannelListener joinedChannel(final ClientChannel channel) {
        GameContext.getClientCommunication().getChannelConteiner().addChannel(channel);
        return this;
    }

    /**
	 * El metodo se dispara al recibir un mensaje directo dede el servidor.<BR/>
	 * Para procesar el mensaje internamente, se invoca el metodo
	 * {@link #processIncomeMsg(ByteBuffer)}.
	 * 
	 * @param bbMsg
	 *            El mensaje recibido como buffer de bytes.
	 * 
	 * @see ServerSessionListener#receivedMessage(ByteBuffer arg0)
	 */
    @Override
    public final void receivedMessage(final ByteBuffer bbMsg) {
        this.processIncomeMsg(bbMsg);
    }

    /**
	 * Este metodo es disparado por {@link SimpleClient} al momento que se esta
	 * intentando reconectar al serivor darkstar. Internamente:<BR/> <CODE>
	 *  AbstractMessage msg = new EmptyMessage();<BR/>
	 *	msg.setType(ReconnectingTask.ReconnectingTask_TYPE);<BR/>		
	 *	MsgProcessorFactory.getInstance().createProcessor
	 *  (msg.getType()).process(msg);.</CODE>
	 * 
	 * @see ServerSessionListener#reconnecting()
	 */
    @Override
    public void reconnecting() {
        MsgAbstract msg = new MsgEmpty();
        msg.setType(TaskReconnecting.RECONNECTING_TASK_TYPE);
        MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
    }

    /**
	 * Este metodo es disparado por {@link SimpleClient} al momento que se
	 * reconecta al serivor darkstar. Internamente:<BR/> <CODE>
	 *  AbstractMessage msg = new EmptyMessage();<BR/>
	 *	msg.setType(ReconnectedTask.ReconnectedTask_TYPE);<BR/>		
	 *	MsgProcessorFactory.getInstance().createProcessor
	 *		(msg.getType()).process(msg);.</CODE>
	 * 
	 * @see ServerSessionListener#reconnected()
	 */
    @Override
    public void reconnected() {
        MsgAbstract msg = new MsgEmpty();
        msg.setType(TaskReconnected.RECONNECTED_TASK_TYPE);
        MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
    }

    /**
	 * Este metodo es disparado por {@link SimpleClient} al momento que se
	 * deconecta del serivor darkstar. Internamente:<BR/> <CODE>
	 *  AbstractMessage msg = new MsgPlainText(razon);<BR/>
	 *	msg.setType(DisconnectedTask.DisconnectedTask_TYPE);<BR/>		
	 * 	MsgProcessorFactory.getInstance().createProcessor
	 * 	(msg.getType()).process(msg);.</CODE>
	 * 
	 * @param gracefull
	 *            No se utiliza.
	 * 
	 * @param razon
	 *            La justificacion de porque se desconecto.
	 * 
	 * @see ServerSessionListener#disconnected(boolean arg0,String arg1)
	 */
    @Override
    public void disconnected(final boolean gracefull, final String razon) {
        MsgAbstract msg = new MsgPlainText(razon);
        msg.setType(TaskDisconnected.DISCONNECTED_TASK_TYPE);
        MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
    }

    /**
	 * Este metodo es solicitado por {@link SimpleClient} al momento de intentar
	 * logease a un servidor.
	 * 
	 * @return {@code GameContext.getPasswordAuthentication();}
	 * 
	 * @see SimpleClientListener#getPasswordAuthentication()
	 */
    @Override
    public final PasswordAuthentication getPasswordAuthentication() {
        return GameContext.getPasswordAuthentication();
    }

    /**
	 * Este metodo es disparado cuando el cliente se logeo en un servidor
	 * Darkstar.<BR/> Internamente:<BR/> <CODE>
	 *  AbstractMessage msg = new EmptyMessage();<BR/>
	 *	msg.setType(LoggedInTask.LoggedInTask_TYPE);<BR/>		
	 *	MsgProcessorFactory.getInstance().createProcessor
	 *  (msg.getType()).process(msg);.</CODE>
	 * 
	 * @see SimpleClientListener#loggedIn()
	 */
    @Override
    public void loggedIn() {
        MsgAbstract msg = new MsgEmpty();
        msg.setType(TaskLoggedIn.LOGGEDIN_TASK_TYPE);
        MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
    }

    /**
	 * Este metodo es disparado cuando acurre un fallo al logearse de un
	 * servidor Darkstar.<BR/> Internamente:<BR/> <CODE>
	 *  AbstractMessage msg = new MsgPlainText(razon);<BR/>
	 *	msg.setType(LogginFailedTask.LogginFailedTask_TYPE);<BR/>		
	 *	MsgProcessorFactory.getInstance().createProcessor
	 *  (msg.getType()).process(msg);.</CODE>
	 * 
	 * @param razon
	 *            Una explicacion de por que fallo el login.
	 * 
	 * @see SimpleClientListener#loginFailed(String arg0)
	 */
    @Override
    public void loginFailed(final String razon) {
        MsgAbstract msg = new MsgPlainText(razon);
        msg.setType(TaskLogginFailed.LOGGIN_FAILED_TASK_TYPE);
        clientCommunication.resetSimpleClient();
        MsgProcessorFactory.getInstance().createProcessor(msg.getType()).process(msg);
    }
}
