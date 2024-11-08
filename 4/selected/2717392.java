package com.bening.smsapp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class NIOTest {

    public static void main(String[] args) {
        try {
            File iFile = new File("D:/Project/MeAndBeningWorkspace/SmsApp/temp/coba.xml");
            FileInputStream fis = new FileInputStream(iFile);
            FileChannel channel = fis.getChannel();
            MappedByteBuffer mbb = channel.map(MapMode.READ_ONLY, 0, iFile.length());
            System.out.println("tes");
        } catch (FileNotFoundException f) {
        } catch (IOException i) {
        }
    }
}
