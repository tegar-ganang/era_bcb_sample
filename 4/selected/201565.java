package com.jme3.network.serializing.serializers;

import com.jme3.network.message.Message;
import com.jme3.network.message.ZIPCompressedMessage;
import com.jme3.network.serializing.Serializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Serializes ZIP messages.
 *
 * @author Lars Wesselius
 */
public class ZIPSerializer extends Serializer {

    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try {
            ZIPCompressedMessage result = new ZIPCompressedMessage();
            byte[] byteArray = new byte[data.remaining()];
            data.get(byteArray);
            ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(byteArray));
            in.getNextEntry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] tmp = new byte[9012];
            int read;
            while (in.available() > 0 && ((read = in.read(tmp)) > 0)) {
                out.write(tmp, 0, read);
            }
            in.closeEntry();
            out.flush();
            in.close();
            result.setMessage((Message) Serializer.readClassAndObject(ByteBuffer.wrap(out.toByteArray())));
            return (T) result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.toString());
        }
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        if (!(object instanceof ZIPCompressedMessage)) return;
        ZIPCompressedMessage zipMessage = (ZIPCompressedMessage) object;
        Message message = zipMessage.getMessage();
        ByteBuffer tempBuffer = ByteBuffer.allocate(512000);
        Serializer.writeClassAndObject(tempBuffer, message);
        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        ZipOutputStream zipOutput = new ZipOutputStream(byteArrayOutput);
        zipOutput.setLevel(zipMessage.getLevel());
        ZipEntry zipEntry = new ZipEntry("zip");
        zipOutput.putNextEntry(zipEntry);
        zipOutput.write(tempBuffer.array());
        zipOutput.flush();
        zipOutput.closeEntry();
        zipOutput.close();
        buffer.put(byteArrayOutput.toByteArray());
    }
}
