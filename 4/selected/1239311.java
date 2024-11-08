package com.qarks.util.stream;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import com.qarks.util.stream.Base64InputStream;

public class Base64Helper {

    private Base64Helper() {
    }

    public static String convertStringToBase64String(String string) throws IOException {
        return new String(convertStringToBase64String(string.getBytes("UTF-8")), "UTF-8");
    }

    public static String convertBase64StringToString(String string) throws IOException {
        return new String(convertBase64StringToString(string.getBytes("UTF-8")), "UTF-8");
    }

    public static byte[] convertStringToBase64String(byte clearText[]) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream os = new Base64OutputStream(bos, true);
        try {
            os.write(clearText);
        } finally {
            os.close();
        }
        return bos.toByteArray();
    }

    public static byte[] convertBase64StringToString(byte base64[]) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(base64);
        Base64InputStream bis = new Base64InputStream(bais);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte array[] = new byte[2048];
        int read;
        while ((read = bis.read(array)) > -1) {
            bos.write(array, 0, read);
        }
        return bos.toByteArray();
    }
}
