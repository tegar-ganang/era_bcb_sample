package se.antimon.colourcontrols;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import javax.sound.midi.MidiDevice;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ColourControl {

    public enum Message {

        CONTROL, NOTE_MONO, NOTE_POLY
    }

    public enum Scan {

        MAX_SHADE, FRACTION
    }

    public enum ColorSet {

        BASE_COLOR, COLOR
    }

    private Color baseColor = Preferences.defaultBaseColor;

    private Color color = Color.green;

    private float[] baseColorC;

    private float[] colorC;

    private Message message = Preferences.defaultMessage;

    private Scan scan = Preferences.defaultScan;

    private float min = 0.0F;

    private float max = 1.0F;

    private float maxPossible = 1000.0F;

    private int channel = 1;

    private int control = 70;

    private int note = 64;

    private float colourDeviance = 0.2F;

    private State state;

    private Sweeper sweeper;

    private double baseLength;

    private double[] v2;

    private MidiDevice.Info deviceInfo = null;

    public class State {

        double totalSum;

        double maximum;
    }

    public void notifyNewSweeper(Sweeper sweeper) {
        if (this.sweeper == null) {
            this.sweeper = sweeper;
        }
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(Color baseColor) {
        this.baseColor = baseColor;
        baseColorC = new float[3];
        baseColor.getColorComponents(baseColorC);
        MyLogger.debug("Base Color: " + baseColor);
        updateBaseLength();
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public MidiDevice.Info getDevice() {
        return deviceInfo;
    }

    public void setDevice(MidiDevice.Info info) {
        deviceInfo = info;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        colorC = new float[3];
        color.getColorComponents(colorC);
        MyLogger.debug("Color: " + color);
        updateBaseLength();
    }

    public int getControl() {
        return control;
    }

    public void setControl(int control) {
        this.control = control;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public Scan getScan() {
        return scan;
    }

    public void setScan(Scan scan) {
        this.scan = scan;
    }

    public ColourControl() {
        baseColorC = new float[3];
        baseColor.getColorComponents(baseColorC);
        colorC = new float[3];
        color.getColorComponents(colorC);
        System.out.println("color:" + colorC[0] + "/" + colorC[1] + "/" + colorC[2]);
        System.out.println("basecolor:" + baseColorC[0] + "/" + baseColorC[1] + "/" + baseColorC[2]);
        updateBaseLength();
    }

    private void updateBaseLength() {
        baseLength = Math.sqrt((colorC[0] - baseColorC[0]) * (colorC[0] - baseColorC[0]) + (colorC[1] - baseColorC[1]) * (colorC[1] - baseColorC[1]) + (colorC[2] - baseColorC[2]) * (colorC[2] - baseColorC[2]));
        v2 = new double[] { colorC[0] - baseColorC[0], colorC[1] - baseColorC[1], colorC[2] - baseColorC[2] };
        System.out.println("Base length=" + baseLength);
    }

    public void resetState() {
        state = new State();
    }

    public void collectionComplete() {
        MidiManager.sendControlMessage(channel - 1, control, (int) (getFinalResult() * 127), deviceInfo);
    }

    public double getFinalResult() {
        if (state == null) {
            return 0.0F;
        }
        switch(scan) {
            case MAX_SHADE:
                return state.maximum;
            case FRACTION:
                return Math.min(state.totalSum, maxPossible) / state.totalSum;
            default:
                throw new IllegalArgumentException("Unknown scan '" + scan + "'");
        }
    }

    private class ColourCubeValue {

        public double position;

        public double distance;

        public ColourCubeValue(float r, float g, float b) {
            double[] v1 = new double[] { r - baseColorC[0], g - baseColorC[1], b - baseColorC[2] };
            double valueLength = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2]);
            if (valueLength == 0.0 || baseLength == 0.0) {
                position = 0.0;
                distance = 0.0;
            } else {
                double angle = Math.acos((v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]) / (valueLength * baseLength));
                position = (valueLength * Math.cos(angle)) / baseLength;
                distance = valueLength * Math.sin(angle);
            }
        }
    }

    public void collect(float[] inC1, double fraction1, float[] inC2, double fraction2) {
        ColourCubeValue cubeValue1 = new ColourCubeValue(inC1[0], inC1[1], inC1[2]);
        double calculatedValue1 = cubeValue1.position;
        double devianceConeValue1 = colourDeviance * cubeValue1.position;
        if (calculatedValue1 < 0.0 || calculatedValue1 > 1.0 || cubeValue1.distance >= devianceConeValue1) {
            calculatedValue1 = 0.0;
        } else if (cubeValue1.distance > devianceConeValue1 / 2.0) {
            calculatedValue1 *= (devianceConeValue1 - cubeValue1.distance) / (devianceConeValue1 / 2.0);
            if (calculatedValue1 > 0.0 && cubeValue1.position > 0.5) {
            }
        }
        if (inC2 != null) {
            ColourCubeValue cubeValue2 = new ColourCubeValue(inC2[0], inC2[1], inC2[2]);
            double devianceConeValue2 = colourDeviance * cubeValue2.position;
            double calculatedValue2 = cubeValue2.position;
            if (calculatedValue2 < 0.0 || calculatedValue2 > 1.0 || cubeValue2.distance >= devianceConeValue2) {
                calculatedValue2 = 0.0;
            } else if (cubeValue2.distance > devianceConeValue2 / 2.0) {
                calculatedValue2 *= (devianceConeValue2 - cubeValue2.distance) / (devianceConeValue2 / 2.0);
                if (calculatedValue2 > 0.0 && cubeValue2.position > 0.5) {
                }
            }
            calculatedValue1 = calculatedValue1 * fraction1 + calculatedValue2 * fraction2;
        }
        switch(scan) {
            case MAX_SHADE:
                if (calculatedValue1 > state.maximum) {
                    state.maximum = calculatedValue1;
                }
                break;
            case FRACTION:
                state.totalSum += calculatedValue1;
                break;
        }
    }

    public void collect(float[] inC) {
        collect(inC, 1.0F, null, 0.0F);
    }

    public Sweeper getSweeper() {
        return sweeper;
    }

    public void setSweeper(Sweeper sweeper) {
        this.sweeper = sweeper;
    }

    public void writeXML(String indent, BufferedWriter writer) throws IOException {
        writer.write(indent + "<colourControl>");
        writer.newLine();
        writer.write(indent + "  <channel>" + getChannel() + "</channel>");
        writer.newLine();
        writer.write(indent + "  <control>" + getControl() + "</control>");
        writer.newLine();
        writer.write(indent + "  <max>" + getMax() + "</max>");
        writer.newLine();
        writer.write(indent + "  <min>" + getMin() + "</min>");
        writer.newLine();
        writer.write(indent + "  <note>" + getNote() + "</note>");
        writer.newLine();
        writer.write(indent + "  <baseColor>");
        writer.newLine();
        writeColourXML(indent + "    ", writer, baseColor);
        writer.write(indent + "  </baseColor>");
        writer.newLine();
        writer.write(indent + "  <color>");
        writer.newLine();
        writeColourXML(indent + "    ", writer, color);
        writer.write(indent + "  </color>");
        writer.newLine();
        writer.write(indent + "  <device>" + (getDevice() != null ? getDevice().getName() : "") + "</device>");
        writer.newLine();
        writer.write(indent + "  <message>" + getMessage() + "</message>");
        writer.newLine();
        writer.write(indent + "  <scan>" + getScan() + "</scan>");
        writer.newLine();
        writer.write(indent + "  <sweeper>" + (getSweeper() != null ? getSweeper().getName() : "") + "</sweeper>");
        writer.newLine();
        writer.write(indent + "</colourControl>");
        writer.newLine();
    }

    public void readXML(Node parentNode) {
        NodeList nodeList = parentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("colourControl")) {
                readXML(node);
            } else if (node.getNodeName().equals("channel")) {
                setChannel(Integer.parseInt(node.getTextContent()));
            } else if (node.getNodeName().equals("control")) {
                setControl(Integer.parseInt(node.getTextContent()));
            } else if (node.getNodeName().equals("max")) {
                setMax(Float.parseFloat(node.getTextContent()));
            } else if (node.getNodeName().equals("min")) {
                setMin(Float.parseFloat(node.getTextContent()));
            } else if (node.getNodeName().equals("note")) {
                setNote(Integer.parseInt(node.getTextContent()));
            } else if (node.getNodeName().equals("baseColor")) {
                setBaseColor(readColorXML(node));
            } else if (node.getNodeName().equals("color")) {
                setColor(readColorXML(node));
            } else if (node.getNodeName().equals("device")) {
                String text = node.getTextContent();
                if (text != null && text.length() > 0) {
                    setDevice(MidiManager.getMidiDeviceInfo(text));
                }
            } else if (node.getNodeName().equals("message")) {
                setMessage(Message.valueOf(node.getTextContent()));
            } else if (node.getNodeName().equals("scan")) {
                setScan(Scan.valueOf(node.getTextContent()));
            } else if (node.getNodeName().equals("sweeper")) {
                String text = node.getTextContent();
                if (text != null && text.length() > 0) {
                    setSweeper(ColourControlsMain.getGUI().findSweeper(text));
                }
            }
        }
    }

    public static void writeColourXML(String indent, BufferedWriter writer, Color color) throws IOException {
        writer.write(indent + "<red>" + color.getRed() + "</red>");
        writer.newLine();
        writer.write(indent + "<green>" + color.getGreen() + "</green>");
        writer.newLine();
        writer.write(indent + "<blue>" + color.getBlue() + "</blue>");
        writer.newLine();
    }

    public static Color readColorXML(Node node) {
        int r = 0;
        int g = 0;
        int b = 0;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("red")) {
                r = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equals("green")) {
                g = Integer.parseInt(child.getTextContent());
            } else if (child.getNodeName().equals("blue")) {
                b = Integer.parseInt(child.getTextContent());
            }
        }
        return new Color(r, g, b);
    }
}
