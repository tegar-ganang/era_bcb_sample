package p.s;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 
 */
public final class Resources extends java.util.HashMap<String, Resources.Content> {

    public static final class Content extends Object {

        public final byte[] bits;

        public Content(byte[] bits) {
            super();
            this.bits = bits;
        }
    }

    private static final Resources Map = new Resources();

    public static final byte[] For(String path) {
        return Map.getResource(path);
    }

    private Resources() {
        super();
    }

    public byte[] getResource(String path) {
        Content content = this.get(path);
        if (null != content) {
            return content.bits;
        } else {
            try {
                InputStream resource = this.getClass().getResourceAsStream(path);
                if (null != resource) {
                    try {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        {
                            byte[] iob = new byte[512];
                            int read;
                            while (0 < (read = resource.read(iob, 0, 512))) {
                                buf.write(iob, 0, read);
                            }
                        }
                        content = new Content(buf.toByteArray());
                        this.put(path, content);
                        return content.bits;
                    } finally {
                        resource.close();
                    }
                }
            } catch (Exception any) {
            }
            return null;
        }
    }
}
