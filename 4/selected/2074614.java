package file;

import java.util.*;
import java.io.*;
import timer.ExecutionTimer;
import timer.TimerRecordFile;
import utility.Input;

public class FileOperation {

    String Path;

    String sortedStringArray[];

    Hashtable hash;

    HashMap hmOriginal = new HashMap();

    Hashtable<Object, Object> hashtableForUppend = new Hashtable<Object, Object>();

    Iterator HashtableIterator;

    Vector indexSortVector = new Vector();

    RandomAccessFile fwriter;

    public FileOperation(RandomAccessFile fwriter) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.fwriter = fwriter;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "FileOperation", t.duration());
    }

    public FileOperation(String Path) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        this.Path = Path;
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "FileOperation", t.duration());
    }

    public boolean addRecordToFile(Hashtable<Object, Object> hash) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        System.out.println("hashtable enterd into add record method = " + hash);
        try {
            fwriter.seek(0);
            String line = fwriter.readLine();
            String[] ArrayForSort = line.split(Input.SEPARATOR);
            for (int i = 0; i < ArrayForSort.length; i++) {
                try {
                    ArrayForSort[i] = hash.get(ArrayForSort[i].toLowerCase()).toString();
                } catch (Exception e) {
                    ArrayForSort[i] = "";
                }
                System.out.println(ArrayForSort[i]);
            }
            System.out.println("Appended record is :");
            fwriter.seek(fwriter.length());
            for (int i = 0; i < ArrayForSort.length; i++) {
                System.out.print(ArrayForSort[i] + Input.SEPARATOR);
                fwriter.writeBytes(ArrayForSort[i] + Input.SEPARATOR);
            }
            sortedStringArray = ArrayForSort;
            fwriter.writeBytes(Input.ENDSEPARATOR);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "addRecordToFile", t.duration());
        return false;
    }

    public synchronized boolean updateRecord(long indexOfLine, Hashtable<Object, Object> hashtableForUppend) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            fwriter.seek(0);
            int s = hashtableForUppend.size();
            String ArrayForSort[] = new String[s];
            int i = 0;
            String line = fwriter.readLine();
            ArrayForSort = line.split(Input.SEPARATOR);
            for (i = 0; i < ArrayForSort.length; i++) {
                try {
                    ArrayForSort[i] = hashtableForUppend.get(ArrayForSort[i].toLowerCase()).toString();
                } catch (Exception e) {
                    ArrayForSort[i] = "";
                }
                System.out.println(ArrayForSort[i]);
            }
            System.out.println("Update record is :");
            int countLine = 1;
            StringBuffer data = new StringBuffer();
            data.append(line + Input.ENDSEPARATOR);
            while ((line = fwriter.readLine()) != null) {
                countLine++;
                data.append(line + "\n");
                if (countLine == (indexOfLine - 1)) {
                    line = fwriter.readLine();
                    String[] tmpArray = line.split(Input.SEPARATOR);
                    for (i = 0; i < ArrayForSort.length; i++) {
                        try {
                            if (ArrayForSort[i].length() != 0) {
                                System.out.print(ArrayForSort[i] + "\t");
                                data.append(ArrayForSort[i] + "\t");
                            } else {
                                System.out.print(tmpArray[i] + "\t");
                                data.append(tmpArray[i] + "\t");
                            }
                        } catch (Exception e) {
                            data.append("" + "\t");
                        }
                    }
                    data.append("\n");
                    countLine++;
                }
            }
            fwriter.seek(0);
            System.out.println("\n" + data.toString());
            fwriter.writeBytes(data.toString());
            fwriter.setLength(data.length());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "updateRecord", t.duration());
        return false;
    }

    public synchronized boolean updateRecord(Hashtable<Object, Object> hashtableForUppend, Hashtable<Object, Object> Condition) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        boolean FLAG = false;
        try {
            int counter = 1;
            FileUtility fu = new FileUtility(fwriter);
            fwriter.seek(0);
            String line = fwriter.readLine();
            StringBuffer data = new StringBuffer();
            data.append(line + Input.ENDSEPARATOR);
            String[] ArrayForSort = line.split(Input.SEPARATOR);
            for (int i = 0; i < ArrayForSort.length; i++) {
                try {
                    ArrayForSort[i] = hashtableForUppend.get(ArrayForSort[i]).toString();
                } catch (Exception e) {
                    ArrayForSort[i] = "";
                }
            }
            while ((line = fwriter.readLine()) != null) {
                counter++;
                System.out.println("The Line is : " + line);
                String[] arr2 = line.split(Input.SEPARATOR);
                Enumeration<Object> hmIterator = Condition.keys();
                try {
                    FLAG = false;
                    while (hmIterator.hasMoreElements()) {
                        String clmName = (String) hmIterator.nextElement();
                        int colindex = fu.getColumnIndex(clmName);
                        String valOfHash = Condition.get(clmName).toString();
                        try {
                            if (arr2[colindex - 1].equals(valOfHash)) {
                                FLAG = true;
                            } else if (Double.parseDouble(arr2[colindex - 1]) == Double.parseDouble((valOfHash))) {
                                FLAG = true;
                            } else {
                                FLAG = false;
                                break;
                            }
                        } catch (Exception e) {
                            FLAG = false;
                        }
                    }
                    if (FLAG == true) {
                        System.out.println("The Record Index is : " + counter);
                        String[] tmpArray = line.split(Input.SEPARATOR);
                        for (int i = 0; i < ArrayForSort.length; i++) {
                            try {
                                if (ArrayForSort[i].length() != 0) {
                                    System.out.print(ArrayForSort[i] + Input.SEPARATOR);
                                    data.append(ArrayForSort[i] + Input.SEPARATOR);
                                } else {
                                    System.out.print(tmpArray[i] + Input.SEPARATOR);
                                    data.append(tmpArray[i] + Input.SEPARATOR);
                                }
                            } catch (Exception e) {
                                data.append("" + Input.SEPARATOR);
                            }
                        }
                        data.append(Input.ENDSEPARATOR);
                    } else data.append(line + Input.ENDSEPARATOR);
                } catch (Exception e) {
                    data.append(line + Input.ENDSEPARATOR);
                    e.printStackTrace();
                }
            }
            fwriter.seek(0);
            System.out.println(Input.SEPARATOR + data.toString());
            fwriter.writeBytes(data.toString());
            fwriter.setLength(data.length());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "updateRecord", t.duration());
        return false;
    }

    public boolean deleteFileRecord(Hashtable<Object, Object> Condition) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        boolean RESULT = false;
        boolean FLAG = false;
        try {
            int counter = 1;
            FileUtility fu = new FileUtility(fwriter);
            fwriter.seek(0);
            String line = fwriter.readLine();
            StringBuffer data = new StringBuffer();
            data.append(line + Input.ENDSEPARATOR);
            while ((line = fwriter.readLine()) != null) {
                counter++;
                System.out.println("The Line is : " + line);
                String[] arr2 = line.split(Input.SEPARATOR);
                Enumeration<Object> hmIterator = Condition.keys();
                try {
                    while (hmIterator.hasMoreElements()) {
                        try {
                            String clmName = (String) hmIterator.nextElement();
                            int colindex = fu.getColumnIndex(clmName);
                            String valOfHash = Condition.get(clmName).toString();
                            if (arr2[colindex - 1].equals(valOfHash)) {
                                FLAG = true;
                            } else if (Double.parseDouble(arr2[colindex - 1]) == Double.parseDouble((valOfHash))) {
                                FLAG = true;
                            } else {
                                FLAG = false;
                                break;
                            }
                        } catch (Exception e) {
                            FLAG = false;
                        }
                    }
                    if (FLAG == true) {
                        System.out.println("Match Found And Record is deleted");
                        RESULT = true;
                    } else {
                        data.append(line + Input.ENDSEPARATOR);
                    }
                } catch (Exception e) {
                }
            }
            if (RESULT) {
                fwriter.seek(0);
                fwriter.writeBytes(data.toString());
                fwriter.setLength(data.length());
                t.end();
                TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "updateReport", t.duration());
                return RESULT;
            }
        } catch (Exception e) {
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "deleteFileRecord", t.duration());
        return false;
    }

    public boolean deleteFileRecord(long indexOfLine) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        try {
            fwriter.seek(0);
            StringBuffer txt = new StringBuffer();
            int c;
            int countLine = 1;
            while (fwriter.read() != -1) {
                fwriter.seek(fwriter.getFilePointer() - 1);
                while (fwriter.read() != '\n') ;
                {
                    fwriter.read();
                    countLine++;
                    long afterLine = fwriter.getFilePointer();
                    if (countLine == indexOfLine) {
                        while (fwriter.read() != '\n') ;
                        while ((c = fwriter.read()) != -1) {
                            txt.append((char) c);
                        }
                        System.out.println(txt.toString());
                        fwriter.seek(afterLine - 1);
                        fwriter.writeBytes(txt.toString());
                        fwriter.setLength(fwriter.getFilePointer());
                    }
                }
            }
            t.end();
            TimerRecordFile timerFile = new TimerRecordFile("combinereport.query", "FileOperation", "deleteFileRecord", t.duration());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("combinereport.query", "FileOperation", "deleteFileRecord", t.duration());
        return false;
    }

    public static void main(String[] args) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        RandomAccessFile fwriter = null;
        try {
            fwriter = new RandomAccessFile("/home/sachin/Reports/Basic_Reports/Master_Reports/4203/4203_mid.txt", "rws");
        } catch (FileNotFoundException e) {
        }
        FileOperation fo = new FileOperation(fwriter);
        Hashtable<Object, Object> hashtableForUppend = new Hashtable<Object, Object>();
        Hashtable<Object, Object> Condition = new Hashtable<Object, Object>();
        Condition.put("pid", "111");
        hashtableForUppend.put("pid", "9");
        Hashtable<Object, Object> deleteRecord = new Hashtable<Object, Object>();
        deleteRecord.put("pid", "111");
        fo.deleteFileRecord(Condition);
        t.end();
        TimerRecordFile timerFile = new TimerRecordFile("file", "FileOperation", "main", t.duration());
    }
}
