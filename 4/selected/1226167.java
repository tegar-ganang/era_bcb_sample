package org.oobench.serialization;

import java.io.*;
import org.oobench.common.*;

public class SerializationBenchmark extends AbstractBenchmark {

    private static int minorNumber;

    public int getMajorNumber() {
        return 10;
    }

    public int getMinorNumber() {
        return minorNumber;
    }

    private void serializeWithClass(Class theClass, int count, String comment) {
        for (int c = 0; c < 10; c++) {
            if (c == 9) {
                beginAction(1, "persistence write/read", count, comment);
            }
            String tempFile = ".tmp.archive";
            SerializeClassInterface theInstance = null;
            try {
                theInstance = (SerializeClassInterface) theClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (theInstance == null) {
                System.err.println("error: Couldn't initialize class to " + "be serialized!");
                return;
            }
            reset();
            for (int i = 0; i < count; i++) {
                try {
                    FileOutputStream fout = new FileOutputStream(tempFile);
                    BufferedOutputStream bout = new BufferedOutputStream(fout);
                    ObjectOutputStream oout = new ObjectOutputStream(bout);
                    oout.writeObject(theInstance);
                    oout.flush();
                    oout.close();
                } catch (IOException ioe) {
                    System.err.println("serializing: " + tempFile + ":" + ioe.toString());
                }
                try {
                    FileInputStream fin = new FileInputStream(tempFile);
                    BufferedInputStream bin = new BufferedInputStream(fin);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    theInstance = (SerializeClassInterface) oin.readObject();
                    oin.close();
                } catch (Exception e) {
                    System.err.println("deserializing: " + tempFile + ":" + e.toString());
                    break;
                }
                proceed();
            }
            reset();
            if (c == 9) {
                endAction();
            }
        }
    }

    public static void testSerialization(int count, Class theClass, String description, String[] args, int theMinorNumber, String comment) {
        minorNumber = theMinorNumber;
        SerializationBenchmark bench = new SerializationBenchmark();
        bench.setArguments(args);
        count = bench.getCountWithDefault(count);
        System.out.println("*** Testing " + description);
        bench.serializeWithClass(theClass, count, comment);
    }
}
