package org.apache.harmony.luni.tests.java.lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class TestLibrary {

    private native String printName();

    boolean checkString() {
        if (printName().equals("TestLibrary")) return true;
        return false;
    }

    TestLibrary() {
        InputStream in = TestLibrary.class.getResourceAsStream("/libTestLibrary.so");
        try {
            File tmp = File.createTempFile("libTestLibrary", "so");
            tmp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tmp);
            while (in.available() > 0) {
                out.write(in.read());
            }
            in.close();
            out.close();
            Runtime.getRuntime().load(tmp.getAbsolutePath());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }
}
