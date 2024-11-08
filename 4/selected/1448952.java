package file;

import java.io.*;
import java.util.*;
import timer.ExecutionTimer;
import timer.TimerRecordFile;
import utility.Input;

public class SearchRecord {

    String Path;

    Enumeration<Object> hmIterator;

    Hashtable<Object, Object> hash = new Hashtable<Object, Object>();

    boolean FLAG = false;

    private RandomAccessFile swrite;

    public SearchRecord(String Path) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.Path = Path;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "SearchRecord", t.duration());
    }

    public void fileOpen() {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            swrite = new RandomAccessFile(Path, "r");
        } catch (Exception e) {
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "fileOpen", t.duration());
    }

    public void fileClose() {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            swrite.close();
        } catch (Exception e) {
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "fileClose", t.duration());
    }

    public SearchRecord(RandomAccessFile swriter) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.swrite = swriter;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "SearchRecord", t.duration());
    }

    public Hashtable getRecordHash(Hashtable<Object, Object> hash) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        Hashtable<Object, Object> temp = new Hashtable<Object, Object>();
        System.out.println(hash);
        try {
            RandomAccessFile random = new RandomAccessFile(Path, "r");
            FileUtility fu = new FileUtility(random);
            random.readLine();
            while (random.read() != -1) {
                random.seek(random.getFilePointer() - 1);
                String src = random.readLine();
                System.out.println("The Line is : " + src);
                String[] arr2 = src.split(Input.SEPARATOR);
                hmIterator = hash.keys();
                while (hmIterator.hasMoreElements()) {
                    String clmName = (String) hmIterator.nextElement();
                    int colindex = fu.getColumnIndex(clmName);
                    String valOfHash = hash.get(clmName).toString();
                    if (arr2[colindex - 1].equals(valOfHash)) {
                        FLAG = true;
                    } else {
                        FLAG = false;
                        break;
                    }
                }
                if (FLAG == true) {
                    System.out.println(arr2.length);
                    for (int i = 0; i < arr2.length; i++) {
                        temp.put(fu.getColumnName(i + 1), arr2[i]);
                    }
                    return temp;
                }
            }
            random.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordHash", t.duration());
        return null;
    }

    public Hashtable getRecordsetHash(Hashtable<Object, Object> hash) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        Hashtable<Object, Object> temp = new Hashtable<Object, Object>();
        System.out.println(hash);
        try {
            FileUtility fu = new FileUtility(swrite);
            swrite.seek(0);
            swrite.readLine();
            while (swrite.read() != -1) {
                swrite.seek(swrite.getFilePointer() - 1);
                String src = swrite.readLine();
                System.out.println("The Line is : " + src);
                String[] arr2 = src.split(Input.SEPARATOR);
                hmIterator = hash.keys();
                while (hmIterator.hasMoreElements()) {
                    String clmName = (String) hmIterator.nextElement();
                    int colindex = fu.getColumnIndex(clmName);
                    String valOfHash = hash.get(clmName).toString();
                    if (arr2[colindex - 1].equals(valOfHash)) {
                        FLAG = true;
                    } else {
                        FLAG = false;
                        break;
                    }
                }
                if (FLAG == true) {
                    System.out.println(arr2.length);
                    for (int i = 0; i < arr2.length; i++) {
                        temp.put(fu.getColumnName(i + 1), arr2[i]);
                    }
                    return temp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordsetHash", t.duration());
        return null;
    }

    public String[] getRecordSet(Hashtable<Object, Object> map) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        System.out.println(map);
        Vector<Object> fileResultset = new Vector<Object>();
        try {
            RandomAccessFile random = new RandomAccessFile(Path, "r");
            FileUtility fu = new FileUtility(random);
            random.readLine();
            while (random.read() != -1) {
                try {
                    random.seek(random.getFilePointer() - 1);
                    String src = random.readLine();
                    System.out.println("The Line is : " + src);
                    String[] arr2 = src.split(Input.SEPARATOR);
                    hmIterator = map.keys();
                    while (hmIterator.hasMoreElements()) {
                        try {
                            String clmName = (String) hmIterator.nextElement();
                            int colindex = fu.getColumnIndex(clmName);
                            String valOfHash = map.get(clmName).toString();
                            if (arr2[colindex - 1].equals(valOfHash)) {
                                FLAG = true;
                            } else {
                                FLAG = false;
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (FLAG == true) fileResultset.add(src);
                } catch (Exception e) {
                }
            }
            random.close();
        } catch (Exception e) {
        }
        String finalRecordset[] = new String[fileResultset.size()];
        for (int i = 0; i < finalRecordset.length; i++) {
            finalRecordset[i] = fileResultset.get(i).toString();
            System.out.println("The Return record is : " + finalRecordset[i].toString());
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordSet", t.duration());
        return finalRecordset;
    }

    public String[] getRecordArray(Hashtable<Object, Object> map) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        System.out.println(map);
        Vector<Object> fileResultset = new Vector<Object>();
        try {
            FileUtility fu = new FileUtility(swrite);
            swrite.seek(0);
            swrite.readLine();
            while (swrite.read() != -1) {
                try {
                    swrite.seek(swrite.getFilePointer() - 1);
                    String src = swrite.readLine();
                    System.out.println("The Line is : " + src);
                    String[] arr2 = src.split(Input.SEPARATOR);
                    hmIterator = map.keys();
                    while (hmIterator.hasMoreElements()) {
                        try {
                            String clmName = (String) hmIterator.nextElement();
                            int colindex = fu.getColumnIndex(clmName);
                            String valOfHash = map.get(clmName).toString();
                            if (arr2[colindex - 1].equals(valOfHash)) {
                                FLAG = true;
                            } else {
                                FLAG = false;
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (FLAG == true) fileResultset.add(src);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        String finalRecordset[] = new String[fileResultset.size()];
        for (int i = 0; i < finalRecordset.length; i++) {
            finalRecordset[i] = fileResultset.get(i).toString();
            System.out.println("The Return record is : " + finalRecordset[i].toString());
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordArray", t.duration());
        return finalRecordset;
    }

    public Vector<Object> getVectorSet(Hashtable<Object, Object> map) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        System.out.println(map);
        Vector<Object> fileResultset = new Vector<Object>();
        try {
            String src = swrite.readLine();
            String[] colHeader = src.split(Input.SEPARATOR);
            while ((src = swrite.readLine()) != null) {
                try {
                    Hashtable<Object, Object> table = new Hashtable<Object, Object>();
                    System.out.println("The Line is : " + src);
                    String[] arr2 = src.split(Input.SEPARATOR);
                    hmIterator = map.keys();
                    while (hmIterator.hasMoreElements()) {
                        try {
                            int colindex = -1;
                            String clmName = (String) hmIterator.nextElement();
                            for (int i = 0; i < colHeader.length; i++) {
                                if (clmName.equalsIgnoreCase(colHeader[i])) colindex = i;
                            }
                            String valOfHash = map.get(clmName).toString();
                            if (arr2[colindex].equals(valOfHash)) {
                                FLAG = true;
                            } else {
                                table.clear();
                                FLAG = false;
                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (FLAG == true) {
                        for (int i = 0; i < colHeader.length; i++) {
                            try {
                                table.put(colHeader[i], arr2[i]);
                            } catch (Exception e) {
                            }
                        }
                        fileResultset.add(table);
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getVectorSet", t.duration());
        return fileResultset;
    }

    public int getRecordIndex(Hashtable<Object, Object> hash) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.hash.putAll(hash);
        try {
            int counter = 1;
            RandomAccessFile random = new RandomAccessFile(Path, "r");
            random.readLine();
            FileUtility fu = new FileUtility(random);
            while (random.read() != -1) {
                random.seek(random.getFilePointer() - 1);
                String src = random.readLine();
                counter++;
                System.out.println("The Line is : " + src);
                String[] arr2 = src.split(Input.SEPARATOR);
                hmIterator = hash.keys();
                try {
                    while (hmIterator.hasMoreElements()) {
                        String clmName = (String) hmIterator.nextElement();
                        int colindex = fu.getColumnIndex(clmName);
                        String valOfHash = hash.get(clmName).toString();
                        if (arr2[colindex - 1].equals(valOfHash)) {
                            FLAG = true;
                            continue;
                        } else if (Double.parseDouble(arr2[colindex - 1]) == Double.parseDouble((valOfHash))) {
                            FLAG = true;
                            continue;
                        } else {
                            FLAG = false;
                            break;
                        }
                    }
                    if (FLAG == true) {
                        System.out.println("The Record Index is : " + counter);
                        return counter;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            random.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordIndex", t.duration());
        return 0;
    }

    public int getRecordIndexFile(Hashtable<Object, Object> hash) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.hash.putAll(hash);
        try {
            int counter = 1;
            FileUtility fu = new FileUtility(swrite);
            swrite.seek(0);
            String src = swrite.readLine();
            while ((src = swrite.readLine()) != null) {
                counter++;
                System.out.println("The Line is : " + src);
                String[] arr2 = src.split(Input.SEPARATOR);
                hmIterator = hash.keys();
                try {
                    while (hmIterator.hasMoreElements()) {
                        String clmName = (String) hmIterator.nextElement();
                        int colindex = fu.getColumnIndex(clmName);
                        String valOfHash = hash.get(clmName).toString();
                        if (arr2[colindex - 1].equals(valOfHash)) {
                            FLAG = true;
                            break;
                        } else if (Double.parseDouble(arr2[colindex - 1]) == Double.parseDouble((valOfHash))) {
                            FLAG = true;
                            break;
                        }
                    }
                    if (FLAG == true) {
                        System.out.println("The Record Index is : " + counter);
                        return counter;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "getRecordIndexFile", t.duration());
        return 0;
    }

    public static void main(String[] args) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        SearchRecord sr = null;
        try {
            String path = "/home/sachin/Reports/Basic_Reports/Master_Reports/4203/4203_pv.txt";
            sr = new SearchRecord(path);
            sr.fileOpen();
            Hashtable<Object, Object> obj = new Hashtable<Object, Object>();
            obj.put("pv", "4203");
            obj.put("pid", "27");
            obj.put("mid", "9945");
            String[] arraySet = sr.getRecordSet(obj);
            System.out.println("\n\nOut PUT is :");
            for (int i = 0; i < arraySet.length; i++) {
                System.out.println("\n" + arraySet[i]);
            }
            sr.fileClose();
        } catch (Exception e) {
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "SearchRecord", "main", t.duration());
    }
}
