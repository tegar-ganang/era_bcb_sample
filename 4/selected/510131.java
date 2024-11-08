package br.com.manish.ahy.fxadmin;

import br.com.manish.ahy.client.exception.OopsException;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class FileUtil {

    public static byte[] readFileAsBytes(String path, Integer start, Integer length) {
        byte[] byteData = null;
        try {
            File file = new File(path);
            DataInputStream dis;
            dis = new DataInputStream(new FileInputStream(file));
            if (dis.available() > Integer.MAX_VALUE) {
                throw new OopsException("Arquivo muito grande pra manipulação, não exagere: " + path);
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream(length);
            byte[] bytes = new byte[length];
            dis.skipBytes(start);
            int readBytes = dis.read(bytes, 0, length);
            os.write(bytes, 0, readBytes);
            byteData = os.toByteArray();
            dis.close();
            os.close();
        } catch (Exception e) {
            throw new OopsException(e, "Problemas ao ler o arquivo [" + path + "].");
        }
        return byteData;
    }

    static final String HEXES = "0123456789ABCDEF";

    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
