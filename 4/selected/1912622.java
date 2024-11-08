package jpcsp.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import jpcsp.format.PSF;

public class mksfo {

    public static void makeUMDSFO(String filename, String discID, String discVersion, String systemVersion, String title, int parentalLevel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(0x10000);
        PSF psf = new PSF();
        psf.put("BOOTABLE", 1);
        psf.put("CATEGORY", "UG");
        psf.put("DISC_ID", discID, 16);
        psf.put("DISC_NUMBER", 1);
        psf.put("DISC_TOTAL", 1);
        psf.put("DISC_VERSION", discVersion);
        psf.put("PARENTAL_LEVEL", parentalLevel);
        psf.put("PSP_SYSTEM_VER", systemVersion);
        psf.put("REGION", 32768);
        psf.put("TITLE", title, 128);
        psf.write(buffer);
        byte[] data = new byte[psf.size()];
        buffer.position(0);
        buffer.get(data);
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.close();
    }

    public static void makePBPSFO(String filename, String discID, String discVersion, String systemVersion, String title, int parentalLevel, int sharingId) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(0x10000);
        PSF psf = new PSF();
        psf.put("BOOTABLE", 1);
        psf.put("CATEGORY", "MG");
        psf.put("DISC_ID", discID, 16);
        psf.put("DISC_VERSION", discVersion);
        psf.put("PARENTAL_LEVEL", parentalLevel);
        psf.put("PSP_SYSTEM_VER", systemVersion);
        psf.put("REGION", 32768);
        psf.put("SHARING_ID", sharingId);
        psf.put("TITLE", title, 128);
        psf.write(buffer);
        byte[] data = new byte[psf.size()];
        buffer.position(0);
        buffer.get(data);
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(data);
        fos.close();
    }

    public static PSF read(String filename) throws IOException {
        PSF psf = new PSF();
        RandomAccessFile raf = new RandomAccessFile(filename, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
        psf.read(buffer);
        channel.close();
        raf.close();
        System.out.println(psf);
        return psf;
    }

    public static void main(String[] args) throws IOException {
        makeUMDSFO("PARAM.SFO", "UCES00002", "1.00", "1.00", "RIDGE RACER", 1);
        System.out.println();
        read("PARAM.SFO");
        System.out.println();
        read("RIDGERACER.SFO");
    }
}
