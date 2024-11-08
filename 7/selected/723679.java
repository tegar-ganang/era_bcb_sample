package logic;

import java.awt.Point;

public class JKFlipFlop extends Gate implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private boolean isRisingEdge;

    private int valueOfClockLast = (isRisingEdge ? 0 : 1);

    private int set = 0, j = 1, clk = 2, k = 3, reset = 4;

    public JKFlipFlop(boolean isRisingEdge) {
        super(5, 2, false);
        this.setType("JKFLIPFLOP");
        gateNameID = this.toString().substring(this.toString().indexOf('.') + 1, this.toString().indexOf('@')) + numberOfGates;
        this.setDelay(0);
        pins[numInputs] = 0;
        pins[numInputs + 1] = 1;
        this.isRisingEdge = isRisingEdge;
    }

    public Point getOutputPos(int outputNumber) {
        return new Point(x + width + 6, y + (outputNumber + 1) * (height / (numOutputs + 1)));
    }

    public boolean isRisingEdge() {
        return isRisingEdge;
    }

    public void toggleRising() {
        isRisingEdge = !isRisingEdge;
    }

    public void refresh() {
        if (pins[set] < 1 && pins[reset] == 1) {
            nextStatePins[numInputs] = 0;
            nextStatePins[numInputs + 1] = 1;
        } else if (pins[set] == 1 && pins[reset] < 1) {
            nextStatePins[numInputs] = 1;
            nextStatePins[numInputs + 1] = 0;
        } else if (pins[j] == 0 && pins[k] == 1 && pins[clk] == (isRisingEdge ? 1 : 0) && valueOfClockLast == (isRisingEdge ? 0 : 1)) {
            nextStatePins[numInputs] = 0;
            nextStatePins[numInputs + 1] = 1;
        } else if (pins[j] == 1 && pins[k] == 0 && pins[clk] == (isRisingEdge ? 1 : 0) && valueOfClockLast == (isRisingEdge ? 0 : 1)) {
            nextStatePins[numInputs] = 1;
            nextStatePins[numInputs + 1] = 0;
        } else if (pins[j] == 1 && pins[k] == 1 && pins[clk] == (isRisingEdge ? 1 : 0) && valueOfClockLast == (isRisingEdge ? 0 : 1)) {
            nextStatePins[numInputs] = pins[numInputs + 1];
            nextStatePins[numInputs + 1] = pins[numInputs];
        } else if (pins[j] == 0 && pins[k] == 0 && pins[clk] == (isRisingEdge ? 1 : 0) && valueOfClockLast == (isRisingEdge ? 0 : 1)) {
            nextStatePins[numInputs] = pins[numInputs];
            nextStatePins[numInputs + 1] = pins[numInputs + 1];
        }
        valueOfClockLast = pins[clk];
    }
}
