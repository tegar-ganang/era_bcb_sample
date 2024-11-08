package client.communication;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import common.messages.MsgTypes;
import common.util.ChannelNameParser;

/**
 * La clase surge de la necesidad de que por un tipo de canal, se pueden mandar
 * distintos tipos de mensajes, pero cada tipo de mensaje solo se puede mandar
 * por un tipo de canal.<BR/> Esta clase puede mantiene asociacion entre los
 * tipos de mensajes y los tipos de canales.<BR/>
 * 
 * La clase sigue el patron singleton.
 * 
 * @author javier
 */
public class ChannelTypeForMsgTypes {

    /**
	 * Hastable que sirve para almacenar los tipo de mensajes y los tipos de
	 * canales. Configurado de la siguiente manera la Key es el MsgType y el
	 * valor interno es el ChannelType.
	 * 
	 * @author Javier
	 */
    private Map<String, String> channelTypeForMsgTypes;

    /**
	 * Instancia del singleton.
	 * 
	 * @author Javier
	 */
    private static ChannelTypeForMsgTypes INSTANCE = new ChannelTypeForMsgTypes();

    /**
	 * Metodo constructor de la clase.
	 * 
	 * @author Javier
	 */
    private ChannelTypeForMsgTypes() {
        channelTypeForMsgTypes = Collections.synchronizedMap(new Hashtable<String, String>());
        channelTypeForMsgTypes.put(MsgTypes.MSG_MOVE_SEND_TYPE, ChannelNameParser.MOVE_CHANNEL_IDENTIFIER);
        channelTypeForMsgTypes.put(MsgTypes.MSG_ROTATE_SEND_TYPE, ChannelNameParser.MOVE_CHANNEL_IDENTIFIER);
        channelTypeForMsgTypes.put(MsgTypes.MSG_CHANGE_PLAYER_STATE_SEND_TYPE, ChannelNameParser.MOVE_CHANNEL_IDENTIFIER);
    }

    /**
	 * Metodo para devolver la instancia de los tipos de canales para los tipos
	 * de mensajes.
	 * 
	 * @return Instancia del singleton.
	 * @author Javier
	 */
    public static ChannelTypeForMsgTypes getInstance() {
        return INSTANCE;
    }

    /**
	 * Metodo para devolver el tipo de canal para el tipo de mensaje.
	 * 
	 * @param msgType
	 *            Tipo de mensaje
	 * @return Tipo de canal para un tipo de mensaje.
	 * @author Javier
	 */
    public String getChannelTypeForMsgType(final String msgType) {
        return this.channelTypeForMsgTypes.get(msgType);
    }

    /**
	 * Utiliza el metodo put del hastable, ingresa el tipo de mensaje y el tipo
	 * de canal y de existir un valor anterior que seria un tipo de canal para
	 * esa clave devuelve el tipo de canal antes ingresado, de lo contrario
	 * devuelve null.
	 * 
	 * @param msgType
	 *            El tipo del mensaje.
	 * @param ChannelType
	 *            El tipo del canal.
	 * @return El tipo de canal para el cual se tenia asociado el tipo de
	 *         mensaje antes de la invocacion del metodo.
	 * @author Javier
	 */
    public String asociate(String msgType, String ChannelType) {
        return this.channelTypeForMsgTypes.put(msgType, ChannelType);
    }

    /**
	 * Utiliza el metodo clear del hastable para limpiarla.
	 * 
	 * @author Javier
	 */
    public void clear() {
        this.channelTypeForMsgTypes.clear();
    }

    /**
	 * Utiliza el metodo remove del hastable para borrar el valor que se
	 * corresponde con la el tipo de canal.
	 * 
	 * @param msgType
	 *            El tipo de mensaje.
	 * @return el tipo de canal que fue borrado para el tipo de mensaje pasado
	 *         como parametro
	 * @author Javier
	 */
    public String removeMsgType(final String msgType) {
        return this.channelTypeForMsgTypes.remove(msgType);
    }

    /**
	 * Metodo para retornar la hastable.
	 * 
	 * @return La hastable channelTypeForMsgTypes
	 * @author Javier
	 */
    public Map<String, String> getChannelTypeForMsgTypes() {
        return channelTypeForMsgTypes;
    }

    /**
	 * Metodo que seteara la hastable interna con una pasada como parametro.
	 * 
	 * @param channelTypeForMsgTypes
	 *            Hastable que se usara para setear la hastable interna.
	 * @author Javier
	 */
    public void setChannelTypeForMsgTypes(Hashtable<String, String> channelTypeForMsgTypes) {
        this.channelTypeForMsgTypes = channelTypeForMsgTypes;
    }
}
