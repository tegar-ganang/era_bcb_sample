package infrastructure.database_manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZippedObject {

    private byte[] zippedContent;

    private String objectClass;

    public ZippedObject(Object o) throws Exception {
        if (o != null) {
            objectClass = o.getClass().getName();
            zippedContent = zipObject(o);
        }
    }

    public ZippedObject() {
    }

    public void setObject(Object o) throws Exception {
        objectClass = o.getClass().getName();
        zippedContent = zipObject(o);
    }

    public void setObjectAndSaveToFile(Object o, File f) throws Exception {
        setObject(o);
        FileOutputStream fout = new FileOutputStream(f);
        BufferedOutputStream buffered = new BufferedOutputStream(fout);
        buffered.write(zippedContent);
        buffered.flush();
        buffered.close();
    }

    public Object getOriginal() throws Exception {
        if (zippedContent != null) {
            return unzipObject(zippedContent);
        }
        return null;
    }

    public Object getOriginalFromFile(File f) throws Exception {
        FileInputStream fInput = new FileInputStream(f);
        BufferedInputStream buffered = new BufferedInputStream(fInput);
        byte[] bArray = new byte[fInput.available()];
        buffered.read(bArray);
        buffered.close();
        zippedContent = bArray;
        return getOriginal();
    }

    public int getZippedMemorySize() {
        return zippedContent.length;
    }

    public int getOriginalMemorySize() throws Exception {
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        ObjectOutputStream ois = new ObjectOutputStream(bis);
        ois.writeObject(getOriginal());
        ois.close();
        return bis.toByteArray().length;
    }

    private byte[] zipObject(Object o) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(baos);
        objectStream.writeObject(o);
        objectStream.flush();
        objectStream.close();
        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
        GZIPOutputStream zip = new GZIPOutputStream(zipBaos);
        zip.write(baos.toByteArray());
        zip.flush();
        zip.close();
        return zipBaos.toByteArray();
    }

    private Object unzipObject(byte[] bytes) throws Exception {
        ByteArrayInputStream inBuffer = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        GZIPInputStream gzip = new GZIPInputStream(inBuffer);
        byte[] tmpBuffer = new byte[1024];
        int n;
        while ((n = gzip.read(tmpBuffer)) >= 0) outBuffer.write(tmpBuffer, 0, n);
        gzip.close();
        ByteArrayInputStream input = new ByteArrayInputStream(outBuffer.toByteArray());
        ObjectInputStream objectInput = new ObjectInputStream(input);
        Object o = objectInput.readObject();
        objectInput.close();
        return o;
    }

    public String getObjectClass() {
        return objectClass;
    }
}
