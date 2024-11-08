package PolishNatation.PolishNatation;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class WriteAndReadToFileTest {

    private WriteAndReadToFile writeAndReadToFile;

    @Test
    public void test() {
        String test = "test test tess aaaa";
        String filename = "./filename.txt";
        writeAndReadToFile.writeToFile(filename, test);
        String out = "";
        out = writeAndReadToFile.readFile("./filename.txt", "");
        System.out.println("out  " + out);
    }
}
