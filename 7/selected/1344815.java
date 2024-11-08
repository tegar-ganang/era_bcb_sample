package sort;

import timer.ExecutionTimer;
import timer.TimerRecordFile;
import validation.Isnumeric;

public class BufferSorting {

    String split[], strValue[];

    private int index;

    int intValue[];

    public String[] bufferSorting(String buffer[], String str) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        String[] splitStr = buffer[0].split("\t");
        split = new String[splitStr.length];
        strValue = new String[buffer.length - 1];
        intValue = new int[buffer.length - 1];
        split = buffer[0].split("\t");
        for (int i = 0; i < split.length; i++) {
            if (split[i].toLowerCase().equals(str.toLowerCase())) {
                index = i;
            }
        }
        for (int i = 1; i < buffer.length; i++) {
            split = buffer[i].split("\t");
            strValue[i - 1] = split[index];
        }
        for (int i = 0; i < buffer.length - 1; i++) {
            {
            }
            {
                strValue[i] = strValue[i];
            }
        }
        if (intValue[0] != 0) {
            sortIds(intValue);
            String intStr[] = new String[intValue.length];
            for (int j = 0; j < buffer.length - 1; j++) {
                Integer i = new Integer(intValue[j]);
                intStr[j] = i.toString();
            }
            t.end();
            TimerRecordFile timerFile = new TimerRecordFile("sort", "BufferSorting", "bufferSorting", t.duration());
            return intStr;
        } else {
            sortStrings(strValue, strValue.length);
            t.end();
            TimerRecordFile timerFile = new TimerRecordFile("sort", "BufferSorting", "bufferSorting", t.duration());
            return strValue;
        }
    }

    void sortIds(int a[]) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        for (int i = a.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (a[j] > a[j + 1]) {
                    int T = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = T;
                }
            }
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "BufferSorting", "sortIds", t.duration());
    }

    void sortStrings(String[] array, int len) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        int a, b;
        String temp;
        int highSubscript = len - 1;
        for (a = 0; a < highSubscript; ++a) for (b = 0; b < highSubscript; ++b) if (array[b].toLowerCase().compareTo(array[b + 1].toLowerCase()) > 0) {
            temp = array[b];
            array[b] = array[b + 1];
            array[b + 1] = temp;
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "BufferSorting", "sortStrings", t.duration());
    }

    public static void main(String args[]) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        String buffer[] = new String[] { "id\tName\tAddr\tDesig\tPhone\tCas", "1\tshweta\tpune\tsw\t111201\tH", "2\tmegha\tbanglore\thr\t101301\tH", "3\tReena\tDelhi\thw\t101\tH", "4\theena\tpatna\tst\t1123\tG", "5\tvrinda\tkolkata\tst\t10\tG", "6\tkomal\traipur\thw\t11223\tH" };
        String str = "phone";
        BufferSorting bfs = new BufferSorting();
        buffer = bfs.bufferSorting(buffer, str);
        for (int x = 0; x < buffer.length; ++x) System.out.print(buffer[x] + " ");
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("sort", "BufferSorting", "main", t.duration());
    }
}
