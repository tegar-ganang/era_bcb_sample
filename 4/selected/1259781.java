package semaGL;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.imageio.ImageIO;
import data.Net;
import data.Node;
import nehe.TextureReader;

public class FileIO {

    class LoadTextures extends Thread {

        Net net;

        String loc;

        Boolean running = true;

        public LoadTextures(String loc_, Net net_) {
            loc = loc_;
            net = net_;
            System.out.println("LoadTextures.LoadTextures()");
        }

        public void end() {
            running = false;
        }

        @Override
        public void run() {
            System.out.println("LoadTextures.run()");
            for (Node n : net.nNodes) {
                if (!running || !app.isTextures()) return;
                loadTexture(loc + "/" + n.getId() + ".jpg", n);
            }
        }
    }

    /**
	 * load textures thread
	 * @author d
	 *
	 */
    class LoadTexturesUrl extends Thread {

        Net net;

        int size;

        boolean running;

        private String loc;

        public LoadTexturesUrl(String loc_, Net net_, int size_) {
            loc = loc_;
            net = net_;
            size = size_;
        }

        public void end() {
            running = false;
        }

        @Override
        public void run() {
            running = true;
            for (Node n : net.nNodes) {
                if (!running || !app.isTextures()) return;
                try {
                    String name = n.getName();
                    String replaced = name.replaceAll("[/*\\<:>|\" ]", "_");
                    if (!loadTexture(loc + "/" + replaced + ".png", n) && !loadTexture(loc + "/" + replaced + ".jpg", n)) {
                        BufferedImage img = loadTextureUrl(n, size);
                        if (img != null) saveBufferedImage(img, loc + "/" + replaced + ".png");
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    static Net net;

    static ByteBuffer bb;

    public static BufferedImage copyBufImage(BufferedImage source, BufferedImage target) {
        Graphics2D g2 = target.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scalex = (double) target.getWidth() / source.getWidth();
        double scaley = (double) target.getHeight() / source.getHeight();
        AffineTransform xform = AffineTransform.getScaleInstance(scalex, scaley);
        g2.drawRenderedImage(source, xform);
        g2.dispose();
        return target;
    }

    public String jarRead(String filename) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        if (is == null) return null;
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(is, "UTF-8");
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);
        return out.toString();
    }

    public static String fileRead(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        FileChannel fc = fis.getChannel();
        bb = ByteBuffer.allocate((int) fc.size());
        fc.read(bb);
        fc.close();
        fis.close();
        String content = new String(bb.array(), "UTF-8");
        bb = null;
        return content;
    }

    public static void fileWrite(String filename, String outString) {
        try {
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
            BufferedWriter out = new BufferedWriter(os);
            out.write(outString);
            out.close();
        } catch (IOException e) {
        }
    }

    public static GraphicsConfiguration getDefaultConfiguration() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration();
    }

    public static String loadFile(File file_) {
        String file = "";
        try {
            file = fileRead(file_);
        } catch (IOException e) {
            System.out.println("file read error: " + file_);
            return null;
        }
        return file;
    }

    public static boolean loadTexture(String filename, Node node) {
        try {
            node.tex = TextureReader.readTexture(filename);
            if (node.tex != null) {
                node.newTex = true;
                return true;
            } else return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static BufferedImage loadTextureUrl(Node node, int size) throws IOException {
        if (node.hasAttribute("url") && !node.hasTexture()) {
            BufferedImage image = ImageIO.read(new URL(node.getAttribute("url")));
            if (image == null) return null;
            float scalex = size / (float) image.getWidth();
            float scaley = size / (float) image.getHeight();
            float scale = Math.max(scalex, scaley);
            BufferedImage thmb = FuncGL.scale(image, scale, scale);
            BufferedImage img = thmb.getSubimage(0, 0, size, size);
            node.tex = TextureReader.readPixels(img, false);
            if (node.tex != null) {
                node.newTex = true;
            }
            return img;
        } else return null;
    }

    static boolean saveBufferedImage(BufferedImage img, String filename) {
        if (img != null) try {
            ImageIO.write(img, "png", new File(filename));
            return true;
        } catch (IOException e) {
            return false;
        } else return false;
    }

    HttpClient httpClient;

    private SemaParameters app;

    private LoadTexturesUrl t1;

    private LoadTextures t2;

    public FileIO(SemaParameters par) {
        app = par;
        httpClient = new HttpClient();
    }

    public String getPage(String url) {
        String page = httpClient.getPage(url);
        return page;
    }

    public void loadTextures(String loc, Net net_) {
        t2 = new LoadTextures(loc, net_);
        t2.start();
    }

    public void loadTexturesUrl(String loc, Net net, int size) {
        if (t1 != null) {
            t1.end();
        }
        t1 = new LoadTexturesUrl(loc, net, size);
        t1.start();
    }

    public boolean storeStream(String url, String filename) {
        return httpClient.storeStream(url, filename);
    }

    public static String HTMLEntityEncode(String s) {
        StringBuffer buf = new StringBuffer();
        int len = (s == null ? -1 : s.length());
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                buf.append(c);
            } else {
                buf.append("&#" + (int) c + ";");
            }
        }
        return buf.toString();
    }

    public static String getExtension(String f) {
        String ext = null;
        String s = f;
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }
}
