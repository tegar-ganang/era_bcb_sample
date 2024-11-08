package com.monad.homerun.pkg.upb;

import static org.cdp1802.upb.UPBConstants.ALL_CHANNELS;
import static org.cdp1802.upb.UPBConstants.DEFAULT_DIM_LEVEL;
import static org.cdp1802.upb.UPBConstants.DEFAULT_FADE_RATE;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import org.cdp1802.upb.UPBDeviceI;
import org.cdp1802.upb.UPBLinkDevice;
import org.cdp1802.upb.UPBManager;
import org.cdp1802.upb.UPBMessage;
import org.cdp1802.upb.UPBNetworkI;
import org.cdp1802.upb.UPBProductI;
import org.cdp1802.upb.UPBRoomI;
import com.monad.homerun.base.Value;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.control.Control;
import com.monad.homerun.event.Event;
import com.monad.homerun.model.Model;
import com.monad.homerun.objmgt.impl.DeviceHandlerRegistry;
import com.monad.homerun.objmgt.impl.SimpleObject;
import com.monad.homerun.objmgt.RuntimeContext;
import com.monad.homerun.objmgt.ControlRuntime;

/**
 * UpbDevice is the base class for all UPB devices
 */
public class UpbDevice extends SimpleObject implements UPBDeviceI {

    protected UpbHandler handler = null;

    protected int deviceId = 0;

    protected int networkId = 0;

    protected short type = 0;

    protected int fwVersion = 0;

    protected boolean isPim = false;

    private Map propMap = null;

    private long loadTime = 0L;

    private boolean modelPower = false;

    public UpbDevice() {
        super();
        propMap = new HashMap();
    }

    public void init(Properties props, RuntimeContext context) {
        super.init(props, context);
        loadTime = System.currentTimeMillis();
        String idStr = props.getProperty("deviceId");
        if (GlobalProps.DEBUG) {
            System.out.println("UPB dev IDr: " + idStr);
        }
        deviceId = Integer.parseInt(idStr);
        String netStr = props.getProperty("networkId");
        networkId = Integer.parseInt(netStr);
        String pimStr = props.getProperty("pim");
        isPim = pimStr != null && "true".equals(pimStr);
        handler = (UpbHandler) DeviceHandlerRegistry.getHandler(props.getProperty("handler"));
        start();
        if (GlobalProps.DEBUG) {
            System.out.println("UpbDevice.init exit");
        }
    }

    public int getDeviceID() {
        return deviceId;
    }

    public String getDeviceName() {
        return getName();
    }

    public boolean isPim() {
        return isPim;
    }

    public void start() {
        if (handler != null) {
            if (!handler.addDevice(this)) {
                if (GlobalProps.DEBUG) {
                    System.out.println("start: couldn't add device to handler");
                }
            } else {
                if (modelPower && context != null) {
                    ;
                }
                if (GlobalProps.DEBUG) {
                    System.out.println("start: added device to handler");
                }
            }
        } else {
            if (GlobalProps.DEBUG) {
                System.out.println("start: handler is null");
            }
        }
    }

    public UpbHandler getHandler() {
        return handler;
    }

    public long getLoadTime() {
        return loadTime;
    }

    public String getProperty(String name) {
        return (String) propMap.get(name);
    }

    public void setProperty(String name, String value) {
        propMap.put(name, value);
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public int getFirmwareVersion() {
        return fwVersion;
    }

    public void setFirmwareVersion(int version) {
        this.fwVersion = version;
    }

    public void onEvent(UPBMessage msg) {
        if (context != null) {
            if (GlobalProps.DEBUG) {
                ;
            }
            if (modelSet.contains("power")) {
                if (GlobalProps.DEBUG) {
                }
                Event modEvent = null;
                context.informModel(getDomain(), getName(), "power", modEvent);
            }
        }
    }

    public ControlRuntime getControlRuntime(Control control, Map bindingProps) {
        ControlRuntime ctrlRt = null;
        String ctrlName = control.getControlName();
        if ("binary-switch".equals(ctrlName)) {
            ctrlRt = new UpbControl(control);
        } else if ("dimmer".equals(ctrlName)) {
            ctrlRt = new UpbDimmer(control);
        } else if (GlobalProps.DEBUG) {
            System.out.println("getControlRuntime control: " + ctrlName + " in InsteonDevice - failed");
        }
        return ctrlRt;
    }

    public void addModel(Model model, Properties bindingProps) {
        super.addModel(model, bindingProps);
        if ("power".equals(model.getModelName())) {
            modelPower = true;
        }
    }

    public Map<String, Object> read(List<String> attrs) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (String key : attrs) {
            map.put(key, null);
        }
        return map;
    }

    private class UpbDimmer extends UpbControl {

        public UpbDimmer(Control control) {
            super(control);
        }

        public Map<String, Object> processParameters(Map<String, Object> parameters) {
            String lvlStr = (String) parameters.get("level");
            if (lvlStr == null || lvlStr.length() == 0) {
                lvlStr = "3";
            }
            int level = Integer.parseInt(lvlStr);
            parameters.put("count", new Integer(level / 3));
            return parameters;
        }
    }

    class UpbControl implements ControlRuntime {

        private String name = null;

        Map<String, String> actionMap = null;

        public UpbControl(Control control) {
            name = control.getControlName();
            actionMap = new HashMap<String, String>();
            for (String aName : control.getActionNames()) {
                actionMap.put(aName, control.getActionCode(aName));
            }
        }

        public String getName() {
            return name;
        }

        public boolean doAction(String action, Map<String, Object> parameters) throws IOException {
            String code = actionMap.get(action);
            if (GlobalProps.DEBUG) {
                System.out.println("Hit UpbControl: " + action + " : " + code);
            }
            return handler.processCommand(UpbDevice.this.getName(), code, processParameters(parameters));
        }

        public Map<String, Object> processParameters(Map<String, Object> params) {
            return params;
        }
    }

    public void copyFrom(UPBDeviceI parentDevice) {
    }

    public boolean doesTransmitsLinks() {
        return false;
    }

    public int getChannelCount() {
        return 0;
    }

    public List<UPBLinkDevice> getLinks() {
        return null;
    }

    public int getNetworkID() {
        return networkId;
    }

    public UPBProductI getProductInfo() {
        return handler.getProduct(this);
    }

    public int getReceiveComponentCount() {
        return 0;
    }

    public UPBRoomI getRoom() {
        return null;
    }

    public boolean isDimmable(int theChannel) {
        return false;
    }

    public boolean isStatusQueryable() {
        return false;
    }

    public void receiveMessage(UPBMessage message) {
    }

    public void releaseResources() {
    }

    public void removeLinkDevice(UPBLinkDevice linkDevice) {
    }

    public void setChannelCount(int theCount) {
    }

    public void setDeviceInfo(UPBNetworkI theNetwork, UPBProductI theProduct, int deviceID) {
    }

    public void setDeviceName(String deviceName) {
    }

    public void setDimmable(boolean dimmable, int theChannel) {
    }

    public void setReceiveComponentCount(int theCount) {
    }

    public void setRoom(UPBRoomI room) {
    }

    public void setTransmitsLinks(boolean transmitsLinks) {
    }

    public void updateInternalDeviceLevel(int newLevel, int atFadeRate, int forChannel) {
    }
}
