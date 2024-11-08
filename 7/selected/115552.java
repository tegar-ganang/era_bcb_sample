package thimeeeee.Model;

public class Field {

    private Field[] neighbors;

    private int trace = 0;

    private double timeStamp;

    private boolean changed = true;

    ElementBase[] elements;

    public Field() {
        neighbors = new Field[6];
    }

    public boolean HasChanged() {
        return changed;
    }

    public void UnSetChanged() {
        changed = false;
    }

    public void SetTrace() {
        trace = 20;
    }

    public double GetTimeStamp() {
        return timeStamp;
    }

    public Field[] GetNeighbors() {
        return this.neighbors;
    }

    public Field GetNeighbor(Direction aD) {
        Field temp = null;
        if (aD == Direction.up) {
            temp = neighbors[0];
        }
        if (aD == Direction.down) {
            temp = neighbors[1];
        }
        if (aD == Direction.upleft) {
            temp = neighbors[2];
        }
        if (aD == Direction.upright) {
            temp = neighbors[3];
        }
        if (aD == Direction.downleft) {
            temp = neighbors[4];
        }
        if (aD == Direction.downright) {
            temp = neighbors[5];
        }
        return temp;
    }

    public ElementBase[] GetElements() {
        return elements;
    }

    public void SetNeighbor(Field aF, Direction aD) {
        if (aD == Direction.up) {
            neighbors[0] = aF;
        }
        if (aD == Direction.down) {
            neighbors[1] = aF;
        }
        if (aD == Direction.upleft) {
            neighbors[2] = aF;
        }
        if (aD == Direction.upright) {
            neighbors[3] = aF;
        }
        if (aD == Direction.downleft) {
            neighbors[4] = aF;
        }
        if (aD == Direction.downright) {
            neighbors[5] = aF;
        }
    }

    public void AddElement(ElementBase aE) {
        if (elements == null) {
            elements = new ElementBase[2];
            elements[0] = aE;
        } else {
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] == null) {
                    elements[i] = aE;
                    break;
                }
            }
        }
        aE.MyField = this;
        changed = true;
    }

    public void RemoveElement(ElementBase aE) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == aE) {
                if (i < elements.length - 1) {
                    elements[i] = elements[i + 1];
                    elements[i + 1] = null;
                } else {
                    elements[i] = null;
                }
            }
        }
        changed = true;
    }

    public boolean IsEmpty() {
        boolean temp = true;
        if (elements == null) {
            temp = true;
        } else {
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] != null) temp = false;
            }
        }
        return temp;
    }

    public void Idle(double aTimeStamp) {
        if (this.timeStamp < aTimeStamp) {
            this.timeStamp = aTimeStamp;
            if (trace > 0) trace--;
            if (!this.IsEmpty()) {
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i] != null) elements[i].Idle(aTimeStamp);
                }
            }
        }
    }

    public int GetTrace() {
        return trace;
    }
}
