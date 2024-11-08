package logic;

import java.awt.Point;

public class RSFlipFlop extends Gate implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private int s = 0, r = 1;

    private int timesRefreshed = 0;

    public RSFlipFlop() {
        super(2, 2, false);
        this.setType("RSFLIPFLOP");
        gateNameID = this.toString().substring(this.toString().indexOf('.') + 1, this.toString().indexOf('@')) + numberOfGates;
        this.setDelay(0);
    }

    public Point getOutputPos(int outputNumber) {
        return new Point(x + width + 6, y + (outputNumber + 1) * (height / (numOutputs + 1)));
    }

    public void refresh() {
        if (pins[s] == -1 && pins[r] == -1) {
            pins[numInputs] = 0;
            pins[numInputs + 1] = 1;
            return;
        }
        if (pins[s] == 1 && pins[r] < 1) {
            nextStatePins[numInputs] = 1;
            nextStatePins[numInputs + 1] = 0;
            return;
        }
        if (pins[s] < 1 && pins[r] == 1) {
            nextStatePins[numInputs] = 0;
            nextStatePins[numInputs + 1] = 1;
            return;
        }
        if (pins[s] == 1 && pins[r] == 1 && timesRefreshed % 3 == 0) {
            nextStatePins[numInputs] = pins[numInputs + 1];
            nextStatePins[numInputs + 1] = pins[numInputs];
        }
        timesRefreshed++;
    }
}
