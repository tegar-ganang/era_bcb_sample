package sort;

import java.util.Vector;
import timer.ExecutionTimer;
import timer.TimerRecordFile;

public class Ascending implements Sortinterface {

    Vector<Object> tempmap = new Vector<Object>();

    String buffer[];

    int starindex = 1;

    public void setVector(Vector<Object> vector, int starindex) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.tempmap = vector;
        this.starindex = starindex;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "Ascending", "setVector", t.duration());
    }

    public void setbuffer(String[] buffer) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.buffer = buffer;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "Ascending", "setbuffer", t.duration());
    }

    public void getResult() {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        for (int i = starindex; i < buffer.length; i++) {
            for (int j = starindex; j < buffer.length - 1; j++) {
                String splitbufferj[] = buffer[j].split("\t");
                String splitbufferj1[] = buffer[j + 1].split("\t");
                for (int k = 0; k < tempmap.size(); k++) {
                    String leftString = splitbufferj[Integer.parseInt(tempmap.get(k).toString())].trim().toLowerCase();
                    String rightString = splitbufferj1[Integer.parseInt(tempmap.get(k).toString())].toLowerCase().trim();
                    try {
                        double left = Double.parseDouble(leftString);
                        double right = Double.parseDouble(rightString);
                        if (left > right) {
                            String temp = buffer[j];
                            buffer[j] = buffer[j + 1];
                            buffer[j + 1] = temp;
                            break;
                        } else if (left < right) break;
                    } catch (NumberFormatException e) {
                        if (leftString.compareTo(rightString) > 0) {
                            String temp = buffer[j];
                            buffer[j] = buffer[j + 1];
                            buffer[j + 1] = temp;
                            break;
                        } else if (leftString.compareTo(rightString) < 0) break;
                    }
                }
            }
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "Ascending", "getResult", t.duration());
    }

    public String[] getbuffer() {
        return buffer;
    }

    public static void main(String[] args) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        Vector<Object> vect = new Vector<Object>();
        vect.add("0");
        vect.add("1");
        vect.add("2");
        String[] buffer2 = { "0\tz", "0\ta", "0\te", "0\tw", "0\tj" };
        Ascending asc = new Ascending();
        asc.setbuffer(buffer2);
        asc.setVector(vect, 0);
        asc.getResult();
        buffer2 = asc.getbuffer();
        for (int i = 0; i < buffer2.length; i++) System.out.println(buffer2[i]);
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "Ascending", "main", t.duration());
    }
}
