package server.managed;

import java.io.Serializable;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;
import common.Constants;

public class ClientData implements ManagedObject, Serializable {

    private static final long serialVersionUID = -230952692326230153L;

    private ClientSession session;

    private ManagedReference channelData;

    public ClientData(ClientSession session) {
        this.session = session;
    }

    public ClientSession getSession() {
        return session;
    }

    public ChannelData getChannelData() {
        if (channelData == null) {
            return null;
        }
        return channelData.get(ChannelData.class);
    }

    public void setChannelData(ChannelData value) {
        if (value != null) {
            DataManager dman = AppContext.getDataManager();
            channelData = dman.createReference(value);
        } else {
            channelData = null;
        }
    }

    public static ClientData getClientData(String clientName) {
        try {
            DataManager dman = AppContext.getDataManager();
            return dman.getBinding(Constants.CLIENT_DATA + clientName, ClientData.class);
        } catch (NameNotBoundException ex) {
            return null;
        } catch (ObjectNotFoundException ex) {
            return null;
        }
    }
}
