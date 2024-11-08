package net.sf.smartcrib.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;
import net.sf.smartcrib.dmx.*;
import net.sf.smartcrib.*;

/**
 * A Spring controller for all the possible SmartCrib commands sent by the client.
 */
@Controller
public class DMXEditorController {

    private static transient Logger logger = Logger.getLogger("smartcrib.history");

    private static final String SWITCH_BUTTON_PREFIX = "dmxDeviceSwitch";

    private static final String DIMMER_DOWN_BUTTON_PREFIX = "dmxDeviceDimmerDown";

    private static final String DIMMER_UP_BUTTON_PREFIX = "dmxDeviceDimmerUp";

    private static final String DIMMER_MIN_BUTTON_PREFIX = "dmxDeviceDimmerMin";

    private static final String DIMMER_MAX_BUTTON_PREFIX = "dmxDeviceDimmerMax";

    /** The persister used to retrieve/store from the data layer. */
    @Autowired
    private SmartCribManager smartCribManager;

    /** For the lights(DMX) page.
     * @return the spring model/view
     */
    @RequestMapping(value = "/dmxDevices.do", method = RequestMethod.GET)
    public ModelAndView dmxDevices() {
        ModelAndView mv = new ModelAndView("dmxDevices");
        SmartCrib smartCrib = smartCribManager.getSmartCribInstance();
        mv.addObject("dmxDevices", smartCrib.getDevices());
        return mv;
    }

    /** For the lights(DMX) commands.
     * @param request the HTTP request
     * @return the spring model/view
     */
    @RequestMapping(value = "/dmxDevices.do", method = RequestMethod.POST)
    public ModelAndView dmxDeviceCommand(HttpServletRequest request) {
        String command = null;
        int step = 10;
        try {
            Enumeration enumeration = request.getParameterNames();
            while (enumeration.hasMoreElements()) {
                String paramName = (String) enumeration.nextElement();
                if (paramName.endsWith(".x") || paramName.endsWith(".y")) {
                    paramName = paramName.substring(0, paramName.length() - 2);
                }
                if (paramName.startsWith(SWITCH_BUTTON_PREFIX)) {
                    int id = Integer.parseInt(paramName.substring(SWITCH_BUTTON_PREFIX.length()));
                    command = "smartcrib.findDeviceById(" + id + ").switchValue()";
                } else if (paramName.startsWith(DIMMER_DOWN_BUTTON_PREFIX)) {
                    int id = Integer.parseInt(paramName.substring(DIMMER_DOWN_BUTTON_PREFIX.length()));
                    command = "smartcrib.findDeviceById(" + id + ").setValue(smartcrib.findDeviceById(" + id + ").getValue() - " + step + ")";
                } else if (paramName.startsWith(DIMMER_UP_BUTTON_PREFIX)) {
                    int id = Integer.parseInt(paramName.substring(DIMMER_UP_BUTTON_PREFIX.length()));
                    command = "smartcrib.findDeviceById(" + id + ").setValue(smartcrib.findDeviceById(" + id + ").getValue() + " + step + ")";
                } else if (paramName.startsWith(DIMMER_MIN_BUTTON_PREFIX)) {
                    int id = Integer.parseInt(paramName.substring(DIMMER_MIN_BUTTON_PREFIX.length()));
                    command = "smartcrib.findDeviceById(" + id + ").setValue(smartcrib.findDeviceById(" + id + ").getMinValue())";
                } else if (paramName.startsWith(DIMMER_MAX_BUTTON_PREFIX)) {
                    int id = Integer.parseInt(paramName.substring(DIMMER_MAX_BUTTON_PREFIX.length()));
                    command = "smartcrib.findDeviceById(" + id + ").setValue(smartcrib.findDeviceById(" + id + ").getMaxValue())";
                }
                if (command != null) {
                    SmartCrib smartCrib = smartCribManager.getSmartCribInstance();
                    try {
                        smartCrib.execute(command);
                    } catch (ScriptException e) {
                    }
                    break;
                }
            }
        } catch (NumberFormatException exc) {
        }
        return dmxDevices();
    }

    @RequestMapping(value = "/dmxDevice.do", method = RequestMethod.GET)
    public ModelAndView dmxDeviceView(@RequestParam(value = "id", required = false) Integer id) {
        ModelAndView mv = new ModelAndView("dmxDevice");
        SmartCrib smartCrib = smartCribManager.getSmartCribInstance();
        DMXDevice device = null;
        if (id != null) {
            device = smartCrib.findDeviceById(id);
        }
        mv.addObject("dmxDevice", device);
        mv.addObject("dmxDeviceTypes", DMXDevice.Type.values());
        return mv;
    }

    @RequestMapping(value = "/dmxDevice.do", method = RequestMethod.POST)
    public ModelAndView dmxDeviceEdit(@RequestParam(value = "id", required = false) Integer id, @RequestParam(value = "controllerAddress", required = false) String controllerAddress, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "description", required = false) String description, @RequestParam(value = "type", required = false) String type, @RequestParam(value = "channel", required = false) Short channel, @RequestParam(value = "minValue", required = false) Integer minValue, @RequestParam(value = "maxValue", required = false) Integer maxValue, HttpServletRequest request) {
        if (request.getParameter("ok") != null) {
            SmartCrib smartCrib = smartCribManager.getSmartCribInstance();
            DMXDevice device = null;
            String logAction = "modified";
            if (id != null) {
                device = smartCrib.findDeviceById(id);
            }
            if (device == null) {
                device = new DMXDevice();
                logAction = "created";
            }
            if (device != null) {
                if (name != null) device.setName(name);
                if (description != null) device.setDescription(null);
                try {
                    device.setType(DMXDevice.Type.valueOf(type));
                } catch (IllegalArgumentException exc) {
                }
                if (controllerAddress != null) device.setControllerAddress(controllerAddress);
                if (channel != null) device.setChannel(channel);
                if (minValue != null) device.setMinValue(minValue);
                if (maxValue != null) device.setMaxValue(maxValue);
                logger.info("Device with id " + device.getId() + " has been " + logAction + ". New name: " + device.getName() + "; description: " + device.getDescription() + "; channel=" + device.getChannel() + "; type=" + device.getType() + "; minValue=" + device.getMinValue() + "; maxValue=" + device.getMaxValue());
                smartCribManager.saveSmartCrib(smartCrib);
            }
        }
        ModelAndView mv = new ModelAndView("dmxDevices");
        SmartCrib smartCrib = smartCribManager.getSmartCribInstance();
        mv.addObject("dmxDevices", smartCrib.getDevices());
        mv.addObject("dmxDeviceTypes", DMXDevice.Type.values());
        return mv;
    }
}
