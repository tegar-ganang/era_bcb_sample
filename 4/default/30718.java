import java.util.StringTokenizer;

public class Transition {

    protected State start;

    protected State end;

    protected String read;

    protected String write;

    protected int move;

    public Transition(State start, State end, String read, String write, int move) {
        this.start = start;
        this.end = end;
        this.read = read;
        this.write = write;
        this.move = move;
    }

    public Transition(State start, State end, String read, String write, String move) {
        this.start = start;
        this.end = end;
        this.read = read;
        this.write = write;
        if (move.equals(">")) this.move = 1; else if (move.equals("<")) this.move = -1; else if (move.equals("-")) this.move = 0;
    }

    public void setStart(State start) {
        this.start = start;
    }

    public void setEnd(State end) {
        this.end = end;
    }

    public void setRead(String read) {
        this.read = read;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public void setMove(int move) {
        this.move = move;
    }

    public State getStart() {
        return start;
    }

    public State getEnd() {
        return end;
    }

    public String getRead() {
        return read;
    }

    public String getWrite() {
        return write;
    }

    public int getMove() {
        return move;
    }

    public String getMoveString() {
        String m = ">";
        if (getMove() == -1) m = "<"; else if (getMove() == 0) m = "-"; else if (getMove() == 1) m = ">";
        return m;
    }

    public String getLabel() {
        String m = "";
        return getRead() + "" + getWrite() + "" + getMoveString();
    }

    public String toString() {
        return start.getLabel() + "\t" + end.getLabel() + "\t" + getRead() + "\t" + getWrite() + "\t" + getMoveString();
    }
}
