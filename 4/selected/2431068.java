package rotorsim.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import org.jdom.Element;
import org.jdom.Verifier;
import util.GameMath;
import util.LinearFunction;

public class RSController {

    public enum HeliControl {

        CYCLIC_ROLL("cyclic_roll"), CYCLIC_PITCH("cyclic_pitch"), COLLECTIVE("collective"), TAIL("tail");

        public final String name;

        private HeliControl(String name) {
            this.name = name;
        }

        public static HeliControl getControl(String name) {
            for (HeliControl control : values()) {
                if (control.name.equalsIgnoreCase(name)) {
                    return control;
                }
            }
            return null;
        }
    }

    private Map<String, Float> controlValues;

    private final Map<HeliControl, Integer> axisMap;

    private final Controller input;

    private final Channel[] channels;

    public RSController(Controller input) {
        this.input = input;
        axisMap = new HashMap<HeliControl, Integer>();
        controlValues = new HashMap<String, Float>();
        int channelCount = 0;
        for (Component component : input.getComponents()) {
            if (component.isAnalog()) {
                channelCount++;
            }
        }
        channels = new Channel[channelCount];
        int channel = 0;
        for (int i = 0; i < input.getComponents().length; i++) {
            Component component = input.getComponents()[i];
            if (component.isAnalog()) {
                channels[channel++] = new Channel(i, new LinearFunction(0.5f, 0.5f));
                channelCount++;
            }
        }
    }

    public String getName() {
        StringBuilder name = new StringBuilder();
        String joyName = input.getName();
        for (int i = 0; i < joyName.length(); i++) {
            char c = joyName.charAt(i);
            if (Verifier.isXMLCharacter(c)) {
                name.append(c);
            }
        }
        return name.toString();
    }

    public void clearCalibration() {
        for (Channel channel : channels) {
            channel.clearCalibration();
        }
    }

    public Controller getSource() {
        return input;
    }

    public int getChannelCount() {
        return channels.length;
    }

    public float getChannelValue(int index, boolean autocal) {
        if (autocal) {
            return channels[index].autoCal(input.getComponents()[channels[index].getComponent()].getPollData());
        } else {
            return channels[index].getValue(input.getComponents()[channels[index].getComponent()].getPollData());
        }
    }

    public float getControlValue(HeliControl control) {
        if (axisMap.containsKey(control)) {
            int axis = axisMap.get(control);
            if (axis >= 0) {
                return getChannelValue(axis, false);
            }
        }
        return 0.5f;
    }

    public boolean isReversed(int axis) {
        return channels[axis].reversed;
    }

    public void setReversed(int axis, boolean reversed) {
        channels[axis].setReversed(reversed);
    }

    public void mapControl(int axis, HeliControl control) {
        axisMap.put(control, axis);
    }

    public int getAxisForControl(HeliControl control) {
        if (axisMap.containsKey(control)) {
            return axisMap.get(control);
        }
        return -1;
    }

    public Map<String, Float> getControls() {
        return controlValues;
    }

    public void update() {
        input.poll();
        controlValues.clear();
        for (HeliControl control : HeliControl.values()) {
            controlValues.put(control.name, getControlValue(control));
        }
    }

    public Element toXML() {
        Element root = new Element("controller");
        root.setAttribute("input", getName());
        Element outputs = new Element("outputs");
        for (HeliControl control : axisMap.keySet()) {
            Element ele = new Element("control");
            ele.setText(Integer.toString(axisMap.get(control)));
            ele.setAttribute("type", control.name);
            outputs.addContent(ele);
        }
        Element channelsEle = new Element("channels");
        for (Channel channel : channels) {
            Element axis = new Element("channel");
            axis.setAttribute("component", Integer.toString(channel.getComponent()));
            axis.setAttribute("reversed", Boolean.toString(channel.reversed()));
            axis.setAttribute("min", Float.toString(channel.min));
            axis.setAttribute("max", Float.toString(channel.max));
            channelsEle.addContent(axis);
        }
        root.addContent(outputs);
        root.addContent(channelsEle);
        return root;
    }

    public void loadXML(Element root) {
        int index = 0;
        for (Element channel : (List<Element>) root.getChild("channels").getChildren()) {
            channels[index].setReversed(Boolean.parseBoolean(channel.getAttributeValue("reversed")));
            channels[index].setRange(Float.parseFloat(channel.getAttributeValue("min")), Float.parseFloat(channel.getAttributeValue("max")));
            channels[index].setComponent(Integer.parseInt(channel.getAttributeValue("component")));
            index++;
        }
        for (Element output : (List<Element>) root.getChild("outputs").getChildren()) {
            HeliControl type = HeliControl.getControl(output.getAttributeValue("type"));
            mapControl(Integer.parseInt(output.getText()), type);
        }
    }

    private class Channel {

        private boolean reversed;

        private int component;

        private float min, max;

        public Channel(int component, LinearFunction cal) {
            this.component = component;
            min = -1.0f;
            max = 1.0f;
        }

        public void setComponent(int component) {
            this.component = component;
        }

        public void clearCalibration() {
            max = -1.0f;
            min = 1.0f;
        }

        public void setRange(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public boolean reversed() {
            return reversed;
        }

        public void setReversed(boolean reversed) {
            this.reversed = reversed;
        }

        public int getComponent() {
            return component;
        }

        public float autoCal(float input) {
            min = Math.min(input, min);
            max = Math.max(input, max);
            return getValue(input);
        }

        public float getValue(float input) {
            float val = (input - min) / (max - min);
            val = GameMath.clamp(val, 0.0f, 1.0f);
            return reversed ? (1.0f - val) : val;
        }
    }
}
