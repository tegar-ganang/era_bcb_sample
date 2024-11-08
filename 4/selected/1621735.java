package ar.com.greeneuron.nzbgui.nzbclient;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Clase que proporciona funciones utiles de IO para escribir los datos de forma que sea compatible con el nzbget
 * 
 * @author AMorelli
 */
class IOUtils {

    /**
	 * Lee todo un archivo
	 * @param file Archivo a leer
	 * @return Archivo leido
	 * @throws IOException
	 */
    public static byte[] readFullFile(File file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            if (in != null) in.close();
        }
    }

    /**
	 * Lee del stream un string
	 * @param in InputStream de entrada
	 * @param length Longitud del texto
	 * @return
	 * @throws IOException
	 */
    public static String readString(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        readFully(in, buffer);
        return decodeString(buffer);
    }

    /**
	 * Lee del stream todos los bytes para llenar el buffer
	 * @param in InputStream de entrada
	 * @param buffer byte array a leer
	 * @throws IOException
	 */
    public static void readFully(InputStream in, byte[] buffer) throws IOException {
        int read = 0;
        while (read < buffer.length) {
            int curr = in.read(buffer, read, buffer.length - read);
            if (curr == -1) throw new EOFException("Se ha alcanzado el fin del stream");
            read += curr;
        }
    }

    /**
	 * Lee del stream un int en forma Little Endian
	 * @param in InputStream de entrada
	 * @return int leido
	 * @throws IOException
	 */
    public static int readInt(InputStream in) throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int b = in.read();
            if (b == -1) throw new EOFException("No se ha podido leer un int. Stream finalizado");
            value |= (b << (i * 8));
        }
        return value;
    }

    /**
	 * Escribe en un stream un int en forma Little Endian
	 * @param out OutputStream de salida
	 * @param value valor a escribir
	 * @throws IOException
	 */
    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write(0xFF & value);
        out.write(0xFF & (value >> 8));
        out.write(0xFF & (value >> 16));
        out.write(0xFF & (value >> 24));
    }

    /**
	 * Lee del stream un long en forma Little Endian
	 * @param in InputStream de entrada
	 * @return long leido
	 * @throws IOException
	 */
    public static long readLong(InputStream in) throws IOException {
        long value = 0;
        int valueLo = readInt(in);
        int valueHi = readInt(in);
        value = ((long) valueHi) << 32;
        value |= (((long) valueLo) & 0xFFFFFFL);
        return value;
    }

    /**
	 * Escribe en un stream un long en forma Little Endian
	 * @param out OutputStream de salida
	 * @param value valor a escribir
	 * @throws IOException
	 */
    public static void writeLong(OutputStream out, long value) throws IOException {
        writeInt(out, (int) value);
        writeInt(out, (int) (value >>> 32));
    }

    /**
	 * Lee del stream un float en forma Little Endian
	 * @param in InputStream de entrada
	 * @return int leido
	 * @throws IOException
	 */
    public static float readFloat(InputStream in) throws IOException {
        return Float.intBitsToFloat(readInt(in));
    }

    /**
	 * Escribe en un stream un float en forma Little Endian
	 * @param out OutputStream de salida
	 * @param value valor a escribir
	 * @throws IOException
	 */
    public static void writeFloat(OutputStream out, float value) throws IOException {
        writeInt(out, Float.floatToIntBits(value));
    }

    /**
	 * Lee del stream un boolean en forma Little Endian
	 * @param in InputStream de entrada
	 * @return int leido
	 * @throws IOException
	 */
    public static boolean readBoolean(InputStream in) throws IOException {
        return readInt(in) != 0;
    }

    /**
	 * Escribe en un stream un boolean en forma Little Endian
	 * @param out OutputStream de salida
	 * @param value valor a escribir
	 * @throws IOException
	 */
    public static void writeBoolean(OutputStream out, boolean value) throws IOException {
        writeInt(out, (value ? -1 : 0));
    }

    /**
	 * Genera un byte array de longitud <code>length</code> que posee el string en ascii
	 * @param str String a colocar en el byte array
	 * @param length Longitud que debe tener el byte array
	 * @return byte array con el string
	 */
    public static byte[] encodeString(final String str, final int length) {
        try {
            byte[] encoded = new byte[length];
            byte[] data = str.getBytes("ascii");
            System.arraycopy(data, 0, encoded, 0, Math.min(data.length, length));
            return encoded;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Genera un string a partit de byte array en ascii
	 * @param data bytes que contienen los caracteres en ascii
	 * @return string
	 */
    public static String decodeString(final byte[] data) {
        try {
            int length = 0;
            while (length < data.length && data[length] != '\0') length++;
            return new String(data, 0, length, "ascii");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
