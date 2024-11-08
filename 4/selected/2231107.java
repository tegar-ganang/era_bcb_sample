package client.communication;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import client.communication.exceptions.SendMessageException;
import com.sun.sgs.client.ClientChannel;
import com.sun.sgs.client.simple.SimpleClient;
import common.messages.IMessage;

/**
 * La clase sigue el patron <I>facade</I> para encapsular el comportamiento
 * de la clase {@link SimpleClient}. y asi trabajar de forma transparente con
 * las clases provistas por el paquete common.
 * 
 * @author lito
 */
public class ClientCommunication {

    /** El {@link Logger} para esta clase. */
    private static final Logger LOGGER = Logger.getLogger(ClientCommunication.class.getName());

    /** 
	 * El cliente encapsulado por esta clase.
	 */
    private SimpleClient simpleClient;

    /** 
	 * El handler de mensajes y eventos del cliente.
	 */
    private ClientListener clientListener;

    /** Contenedor de todos los canales a los que estara subscripto
	 *  el cliente. */
    private ChannelConteiner channelConteiner;

    /**
	 * Constructor por defecto, Inicializa las estructuras internas
	 * de la siguiente forma:<BR/>
	 * {@link #channelConteiner} Crea una instancia de la clase provista
	 * por el constructor por defecto {@link ChannelConteiner}.<BR/>
	 * {@link #clientListener} Crea una instancia de la clase provista
	 * por el constructor por defecto {@link ClientListener}.<BR/>
	 * {@link #simpleClient}  Crea una instancia de la clase provista
	 *  por el framework darkstar, pasandole como listener, el
	 *  listener creado anteriormente en el metodo. 
	 */
    public ClientCommunication() {
        this.channelConteiner = new ChannelConteiner();
        this.clientListener = new ClientListener(this);
        this.simpleClient = new SimpleClient(this.clientListener);
    }

    /**
	 * @return El contenedor de los canales a los que esta subscripto el
	 * cliente.
	 */
    public final ChannelConteiner getChannelConteiner() {
        return channelConteiner;
    }

    /**
	 * @param newChannelConteiner El contenedor a setear.
	 */
    public final void setChannelConteiner(final ChannelConteiner newChannelConteiner) {
        this.channelConteiner = newChannelConteiner;
    }

    /** 
	 * @return el cliente encapsulado por la instancia.
	 */
    public final SimpleClient getSimpleclient() {
        return this.simpleClient;
    }

    public final void resetSimpleClient() {
        simpleClient = new SimpleClient(this.clientListener);
    }

    /** 
	 * @param simpleclient el cliente a establecer
	 */
    public final void setSimpleclient(final SimpleClient simpleclient) {
        this.simpleClient = simpleclient;
    }

    /** 
	 * @return el handler de mensajes de la instancia.
	 */
    public final ClientListener getClientListener() {
        return this.clientListener;
    }

    /** 
	 * @param newClientListener el handler de mensajes a establecer
	 */
    public final void setClientListener(final ClientListener newClientListener) {
        this.clientListener = newClientListener;
    }

    /** 
	 * Realiza un LogOut del servidor darkstar al que este conectado
	 * este cliente.<BR/>
	 * Internamente, {@code this.simpleClient.logout(true);} siempre lo
	 * hace de forma <I>gracefull</I>.
	 */
    public void logout() {
        this.simpleClient.logout(true);
    }

    /**
	 * Intenta realizar un LogIn utilizando los datos host y port
	 * seteados en {@link GameContext}.<BR/>
	 * En caso de ocurrir una excepcion (de tipo IO) y no poder
	 * logearse, se deja registro en {@link LOGGER} y se termina
	 * el metodo.
	 */
    public void login() {
        try {
            this.simpleClient.login(GameContext.getProperties());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo Logear: {0}", e);
        }
    }

    /** 
	 * Envia el mensaje por el canal psados como parametros.
	 * 
	 * @param message El mensaje a enviar.
	 * 
	 * @param channel El canal por deonde se enviara el mensaje.
	 * 
	 * @throws SendMessageException Si ocurre un error al enviar
	 * el mensaje.
	 */
    public void sendEvent(final IMessage message, final ClientChannel channel) throws SendMessageException {
        try {
            channel.send(message.toByteBuffer());
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Exception: {0}", ioe);
            throw new SendMessageException("No se pudo enviar el mensaje por el canal " + channel.getName(), ioe, message);
        }
    }

    /** 
	 * Envia el mensaje pasado como parametro a travez de la 
	 * instancia de {@link SimpleClient} que tenga seteado.
	 * 
	 * @param message El mensaje a enviar.
	 * 
	 * @throws SendMessageException Si ocurre un error al enviar
	 * el mensaje.
	 */
    public void send(final IMessage message) throws SendMessageException {
        try {
            this.simpleClient.send(message.toByteBuffer());
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Exception: {0}", ioe);
            throw new SendMessageException("No se pudo enviar el mensaje directo al servidor", ioe, message);
        }
    }
}
