package br.unb.unbiquitous.ubiquitos.me.uos.connectivity.messages.json;

import java.util.Enumeration;
import java.util.Hashtable;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import br.unb.unbiquitous.ubiquitos.me.uos.connectivity.messages.Message;
import br.unb.unbiquitous.ubiquitos.me.uos.connectivity.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.me.uos.connectivity.messages.ServiceCall.ServiceType;

public class JSONServiceCall extends JSONMessage {

    protected static final String PROP_DRIVER = "driver";

    protected static final String PROP_SERVICE = "service";

    protected static final String PROP_PARAMETERS = "parameters";

    protected static final String PROP_INSTANCE_ID = "instanceId";

    protected static final String PROP_SERVICE_TYPE = "serviceType";

    protected static final String PROP_CHANNELS = "channels";

    protected static final String PROP_CHANNEL_IDS = "channelIDs";

    protected static final String PROP_CHANNEL_TYPE = "channelType";

    public JSONServiceCall(String source) throws JSONException {
        super(source);
    }

    public JSONServiceCall(ServiceCall bean) throws JSONException {
        super((Message) bean);
        this.put(PROP_DRIVER, stringFillHelper(bean.getDriver()));
        this.put(PROP_SERVICE, stringFillHelper(bean.getService()));
        this.put(PROP_PARAMETERS, bean.getParameters());
        this.put(PROP_INSTANCE_ID, bean.getInstanceId());
        this.put(PROP_SERVICE_TYPE, bean.getServiceType());
        this.put(PROP_CHANNELS, bean.getChannels());
        this.put(PROP_CHANNEL_IDS, bean.getChannelIDs());
        this.put(PROP_CHANNEL_TYPE, bean.getChannelType());
    }

    private String stringFillHelper(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    public ServiceCall getAsObject() throws JSONException {
        ServiceCall serviceCall = new ServiceCall();
        serviceCall.setDriver(this.getString(PROP_DRIVER));
        serviceCall.setService(this.getString(PROP_SERVICE));
        serviceCall.setInstanceId(this.optString(PROP_INSTANCE_ID, null));
        String strServiceType = this.optString(PROP_SERVICE_TYPE);
        if (strServiceType != null && !strServiceType.equals("")) {
            serviceCall.setServiceType(ServiceType.valueOf(strServiceType));
        }
        serviceCall.setChannels(this.optInt(PROP_CHANNELS));
        JSONArray jsonCIDs = this.optJSONArray(PROP_CHANNEL_IDS);
        if (jsonCIDs != null) {
            String[] cIDs = new String[jsonCIDs.length()];
            for (int i = 0; i < jsonCIDs.length(); i++) {
                cIDs[i] = jsonCIDs.getString(i);
            }
            serviceCall.setChannelIDs(cIDs);
        }
        serviceCall.setChannelType(this.optString(PROP_CHANNEL_TYPE));
        if (!this.isNull(PROP_PARAMETERS)) {
            Hashtable map = new Hashtable();
            JSONObject obj = (JSONObject) this.get(PROP_PARAMETERS);
            if (obj != null) {
                Enumeration en = obj.keys();
                while (en.hasMoreElements()) {
                    String prop = (String) en.nextElement();
                    map.put(prop, obj.get(prop));
                }
            }
            serviceCall.setParameters(map);
        }
        serviceCall.setError(this.optString(PROP_ERROR));
        return serviceCall;
    }
}
