package net.adrianromero.data.loader;

import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ImageUtils {

    private static char[] HEXCHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** Creates a new instance of ImageUtils */
    private ImageUtils() {
    }

    private static byte[] readStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        byte[] resource = new byte[0];
        int n;
        while ((n = in.read(buffer)) != -1) {
            byte[] b = new byte[resource.length + n];
            System.arraycopy(resource, 0, b, 0, resource.length);
            System.arraycopy(buffer, 0, b, resource.length, n);
            resource = b;
        }
        return resource;
    }

    public static byte[] getBytesFromResource(String sFile) {
        InputStream in = ImageUtils.class.getResourceAsStream(sFile);
        if (in == null) {
            return null;
        } else {
            try {
                return ImageUtils.readStream(in);
            } catch (IOException e) {
                return new byte[0];
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static BufferedImage readImage(String url) {
        try {
            return readImage(new URL(url));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static BufferedImage readImage(URL url) {
        InputStream in = null;
        try {
            URLConnection urlConnection = url.openConnection();
            in = urlConnection.getInputStream();
            return readImage(readStream(in));
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static BufferedImage readImage(byte[] b) {
        if (b == null) {
            return null;
        } else {
            try {
                return ImageIO.read(new ByteArrayInputStream(b));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static byte[] writeImage(BufferedImage img) {
        if (img == null) {
            return null;
        } else {
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ImageIO.write(img, "png", b);
                b.flush();
                b.close();
                return b.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static Object readSerializable(byte[] b) {
        if (b == null) {
            return null;
        } else {
            try {
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b));
                Object obj = in.readObject();
                in.close();
                return obj;
            } catch (ClassNotFoundException eCNF) {
                return null;
            } catch (IOException eIO) {
                return null;
            }
        }
    }

    public static byte[] writeSerializable(Object o) {
        if (o == null) {
            return null;
        } else {
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(b);
                out.writeObject(o);
                out.flush();
                out.close();
                return b.toByteArray();
            } catch (IOException eIO) {
                eIO.printStackTrace();
                return null;
            }
        }
    }

    public static String bytes2hex(byte[] binput) {
        StringBuffer s = new StringBuffer(binput.length * 2);
        for (int i = 0; i < binput.length; i++) {
            byte b = binput[i];
            s.append(HEXCHARS[(b & 0xF0) >> 4]);
            s.append(HEXCHARS[b & 0x0F]);
        }
        return s.toString();
    }
}
