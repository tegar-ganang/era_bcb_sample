package org.bitdrive.network.filelist.impl;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileList {

    private HashMap<String, byte[]> lists;

    public FileList() {
        this.lists = new HashMap<String, byte[]>();
    }

    private byte[] compressData(byte[] data) throws IOException {
        GZIPOutputStream zipOutputStream;
        ByteArrayOutputStream byteOutputStream;
        byteOutputStream = new ByteArrayOutputStream();
        zipOutputStream = new GZIPOutputStream(byteOutputStream);
        zipOutputStream.write(data);
        zipOutputStream.finish();
        return byteOutputStream.toByteArray();
    }

    private byte[] uncomressData(byte[] compressedData) throws IOException {
        int read;
        byte[] uncompressedData = new byte[10 * 1025];
        GZIPInputStream gzipInputStream;
        ByteArrayInputStream arrayInputStream;
        ByteArrayOutputStream arrayOutputStream;
        arrayInputStream = new ByteArrayInputStream(compressedData);
        arrayOutputStream = new ByteArrayOutputStream();
        gzipInputStream = new GZIPInputStream(arrayInputStream);
        while ((read = gzipInputStream.read(uncompressedData)) > 0) {
            arrayOutputStream.write(uncompressedData, 0, read);
        }
        uncompressedData = arrayOutputStream.toByteArray();
        return uncompressedData;
    }

    public void removeFileList(String name) {
        this.lists.remove(name);
    }

    public void addFileList(String name, byte[] data) throws IOException {
        this.lists.put(name, compressData(data));
    }

    public byte[] getFileList(String name) throws IOException {
        byte[] data = this.lists.get(name);
        if (data == null) return null;
        return uncomressData(data);
    }

    private static XStream crateXStream() {
        XStream xstream;
        xstream = new XStream();
        xstream.alias("filelists", FileList.class);
        xstream.alias("name", String.class);
        xstream.alias("data", byte[].class);
        xstream.alias("data", byte[].class);
        xstream.omitField(FileList.class, "IN_BUFFER_MULTIPLIER");
        return xstream;
    }

    public String toXML() {
        XStream xstream = crateXStream();
        return xstream.toXML(this);
    }

    public static FileList fromXML(String xml) {
        XStream xstream = crateXStream();
        return (FileList) xstream.fromXML(xml);
    }

    public static FileList fromXML(InputStream stream) {
        XStream xstream = crateXStream();
        return (FileList) xstream.fromXML(stream);
    }
}
