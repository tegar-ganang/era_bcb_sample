package com.astrientlabs.ad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.Image;

public class Ad {

    private String name;

    private String infoURL;

    private long displayTime;

    private byte[] data;

    private Image image;

    public long getDisplayTime() {
        return displayTime;
    }

    public void setDisplayTime(long displayTime) {
        this.displayTime = displayTime;
    }

    public Image getImage() {
        if (image == null) {
            if (data != null) {
                image = Image.createImage(data, 0, data.length);
                data = null;
            }
        }
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getInfoURL() {
        return infoURL;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] toPersistentFormat() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(name);
        dos.writeUTF(infoURL);
        dos.writeLong(displayTime);
        dos.write(data);
        dos.flush();
        return baos.toByteArray();
    }

    public void fromPersistentFormat(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        name = dis.readUTF();
        infoURL = dis.readUTF();
        displayTime = dis.readLong();
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        byte[] ba = new byte[1 * 1024];
        int read;
        while ((read = dis.read(ba)) != -1) {
            temp.write(ba, 0, read);
        }
        data = temp.toByteArray();
    }
}
