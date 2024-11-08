package org.RSG.Application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.*;

public class BMP {

    String pxl[];

    BufferedReader buffer;

    DataOutputStream wbuf;

    int pos = 0;

    public BMP(String pix[], int width, int height) {
        this.pxl = pix;
        String file = showSaveWindow();
        if (file == "") {
            System.out.println("File Save Terminated");
            return;
        }
        this.writeImage(file, width, height);
        System.out.println("Save Finished!");
    }

    public void writeImage(String filepath, int width, int height) {
        try {
            this.wbuf = new DataOutputStream(new FileOutputStream(filepath));
            this.wbuf.write((byte) 66);
            this.wbuf.write((byte) 77);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write2((byte) 0, (byte) 0);
            this.write2((byte) 0, (byte) 0);
            this.wbuf.write((byte) 54);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 40);
            this.owrite4(width);
            this.owrite4(height);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 1);
            this.write2((byte) 0, (byte) 24);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.write4((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            this.wbuf.write((byte) 0);
            for (int i = height - 1; i >= 0; i--) {
                int z;
                for (z = 0; z < width; z++) {
                    int red = Integer.parseInt(this.pxl[this.getTileID(z, i)].substring(0, 3));
                    int green = Integer.parseInt(this.pxl[this.getTileID(z, i)].substring(3, 6));
                    int blue = Integer.parseInt(this.pxl[this.getTileID(z, i)].substring(6, 9));
                    this.wbuf.writeByte(blue);
                    this.wbuf.writeByte(green);
                    this.wbuf.writeByte(red);
                }
                if (width % 4 > 0) {
                    for (int j = 0; j < (width % 4); j++) {
                        this.wbuf.write((byte) 0);
                    }
                }
            }
            this.wbuf.close();
        } catch (IOException e) {
            System.out.println("An IOException has occured while Writing BMP File!");
        }
    }

    public String showSaveWindow() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.showSaveDialog(chooser);
        String filepath = chooser.getSelectedFile().getAbsolutePath();
        if (!filepath.substring(filepath.length() - 4, filepath.length()).equalsIgnoreCase(".bmp")) {
            filepath += ".BMP";
        }
        if (new File(filepath).exists()) {
            int res = JOptionPane.showConfirmDialog(new JButton(), "File Already Exists! Are you sure you want to overwrite?", "RainSquared Owl", JOptionPane.OK_CANCEL_OPTION);
            System.out.println(res);
            if (res != 0) {
                return "";
            }
        }
        return filepath;
    }

    public int getTileID(int x, int y) {
        int tileid = y * Application.inFocus.width;
        tileid += x;
        return tileid;
    }

    private void write4(byte one, byte two, byte three, byte four) throws IOException {
        this.wbuf.write(one);
        this.wbuf.write(two);
        this.wbuf.write(three);
        this.wbuf.write(four);
    }

    private void owrite4(int number) throws IOException {
        this.wbuf.write(number >> 24);
        this.wbuf.write(number >> 16);
        this.wbuf.write(number >> 8);
        this.wbuf.write(number);
    }

    private void write2(byte one, byte two) throws IOException {
        this.wbuf.write(one);
        this.wbuf.write(two);
    }
}
