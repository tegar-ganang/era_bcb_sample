import vrml.*;
import vrml.field.*;
import vrml.node.*;

public class Console extends VRObject {

    double changeTime;

    boolean active = false;

    SFTime show;

    boolean autoHide = false;

    String[] text;

    int height;

    int curRow = 0;

    int curCol = 0;

    MFString conText;

    float posx = (float) -.15;

    float posy = (float) .03;

    float posz = (float) -.20;

    SFVec3f cursor;

    public void initialize() {
        conText = (MFString) getEventOut("string_changed");
        height = 10;
        text = new String[height];
        for (int i = 0; i < height; i++) {
            text[i] = "";
        }
        show = (SFTime) getEventOut("show");
        cursor = (SFVec3f) getEventOut("cursor");
        super.initialize();
        cortona = false;
        changeTime = System.currentTimeMillis();
    }

    public void processEvent(Event ev) {
        String name = ev.getName();
        ConstField eventValue = ev.getValue();
        double timestamp = ev.getTimeStamp();
        if (name.equals("set_string")) {
            changeTime = timestamp;
            if (autoHide) {
                show.setValue(timestamp);
            }
            if (!active) {
                x = posx;
                y = posy;
                z = posz;
                speed = 1;
                translate(timestamp);
                active = true;
            }
        } else if (name.equals("movement")) {
            if (active && autoHide) {
                posx = oldx;
                posy = oldy;
                posz = oldz;
                x = (float) -.25;
                y = (float) -.17;
                z = (float) -.35;
                speed = 1;
                translate(timestamp);
                active = false;
            }
        } else if (name.equals("writeln")) {
            writeln(((ConstSFString) eventValue).getValue());
        } else if (name.equals("pos")) {
            float[] pos = { 0, 0 };
            ((ConstSFVec2f) eventValue).getValue(pos);
            pos(new Float(pos[0]).intValue(), new Float(pos[1]).intValue());
        } else if (name.equals("write")) {
            write(((ConstSFString) eventValue).getValue());
        } else if (name.equals("clear")) {
            clear();
        } else if (name.equals("autoHide")) {
            autoHide = ((ConstSFBool) eventValue).getValue();
        } else {
            processMovement(name, eventValue, timestamp);
            if (name.equals("set_xyzgo")) {
                posx = x;
                posy = y;
                posz = z;
                active = true;
            }
        }
    }

    public void clear() {
        for (int i = 0; i < height; i++) {
            text[i] = "";
        }
        conText.setValue(text);
        curRow = 0;
        curCol = 0;
        cursor(1000, 1000);
    }

    public void writeln(String s) {
        try {
            if (curRow < height - 1) {
                text[++curRow] = s;
            } else {
                for (int i = 0; i < height - 1; i++) {
                    text[i] = text[i + 1];
                }
                text[height - 1] = s;
            }
            conText.setValue(text);
            curCol = 0;
            cursor(curRow, -2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeat(int row, int col, String s) {
        try {
            text[row] = text[row].substring(0, col) + s;
            conText.setValue(text);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(row);
            System.out.println(col);
            System.out.println(conText);
        }
    }

    public void write(String s) {
        writeat(curRow, curCol, s);
        cursor(curRow, (text[curRow].length()));
    }

    public void pos(int row, int col) {
        cursor(row, col);
        curCol = col;
        curRow = row;
    }

    public void cursor(int row, int col) {
        cursor.setValue(new SFVec3f((float) (.5 + posx + col * .53), (float) (posy - row + .3), posz));
    }
}
