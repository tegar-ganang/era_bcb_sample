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

public class GZIPCompressedObject<E> {

    private byte[] compressedContent;

    private String objectClass;

    public GZIPCompressedObject(E e) throws Exception {
        if (e != null) {
            objectClass = e.getClass().getName();
            compressedContent = compressObject(e);
        }
    }

    public GZIPCompressedObject() {
    }

    public void setObject(E e) throws Exception {
        objectClass = e.getClass().getName();
        compressedContent = compressObject(e);
    }

    public byte[] getByteRepresentation() throws Exception {
        if (compressedContent == null) {
            throw new Exception("No Object wrapped! Please use setObject to put in an Object.");
        }
        return compressedContent;
    }

    public void setObjectAndSaveToFile(E e, File f) throws Exception {
        setObject(e);
        FileOutputStream fout = new FileOutputStream(f);
        BufferedOutputStream buffered = new BufferedOutputStream(fout);
        buffered.write(compressedContent);
        buffered.flush();
        buffered.close();
    }

    @SuppressWarnings("unchecked")
    public E getObject() throws Exception {
        if (compressedContent != null) {
            return (E) unzipObject(compressedContent);
        }
        return null;
    }

    public Object getOriginalFromFile(File f) throws Exception {
        FileInputStream fInput = new FileInputStream(f);
        BufferedInputStream buffered = new BufferedInputStream(fInput);
        byte[] bArray = new byte[fInput.available()];
        buffered.read(bArray);
        buffered.close();
        compressedContent = bArray;
        return getObject();
    }

    public int getCompressedMemorySize() {
        return compressedContent.length;
    }

    public int getDecompressedMemorySize() throws Exception {
        ByteArrayOutputStream bis = new ByteArrayOutputStream();
        ObjectOutputStream ois = new ObjectOutputStream(bis);
        ois.writeObject(getObject());
        ois.close();
        return bis.toByteArray().length;
    }

    private byte[] compressObject(E e) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(baos);
        objectStream.writeObject(e);
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
        E e = (E) objectInput.readObject();
        objectInput.close();
        return e;
    }

    public String getObjectClass() {
        return objectClass;
    }
}
