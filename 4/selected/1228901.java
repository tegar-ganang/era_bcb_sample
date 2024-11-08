package org.zkoss.eclipse.setting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.zkoss.eclipse.util.BundleResourceManager;

/**
 * @author Ian Tsai
 *
 */
public class ImageProvider {

    protected BundleResourceManager resManager;

    private static final Map<String, Image> cache = new LinkedHashMap<String, Image>();

    private Bundle bundle;

    /**
	 * 
	 */
    public ImageProvider() {
        this(ZKStudioPlugin.getDefault().getBundle(), true);
    }

    /**
	 * 
	 * @param bundle
	 */
    public ImageProvider(Bundle bundle, boolean cacheable) {
        resManager = new BundleResourceManager(bundle);
        this.bundle = bundle;
    }

    /**
	 * 
	 * @param relativePath
	 * @return
	 */
    public Image getImage(String relativePath) {
        return getImage(Display.getCurrent(), relativePath);
    }

    /**
	 * 
	 * @param dis
	 * @param relativePath
	 * @return
	 */
    public Image getImage(Display dis, String relativePath) {
        long start = System.currentTimeMillis();
        String key = bundle.getBundleId() + ":" + relativePath;
        Image img = cache.get(key);
        if (img == null) {
            try {
                InputStream in = ImageCache.getInstance().get(relativePath, resManager);
                cache.put(key, img = new Image(dis, in));
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return img;
    }

    /**
	 * 
	 * @author Ian Tsai
	 *
	 */
    private static class ImageCache {

        private static final ImageCache cache = new ImageCache();

        public static ImageCache getInstance() {
            return cache;
        }

        private final Map<String, byte[]> repository;

        private final byte[] buff;

        private ImageCache() {
            repository = new LinkedHashMap<String, byte[]>();
            buff = new byte[1024 * 256];
        }

        /**
		 * 
		 * @param path
		 * @return
		 * @throws IOException 
		 * @throws URISyntaxException 
		 * @throws FileNotFoundException 
		 */
        public synchronized InputStream get(String relativePath, BundleResourceManager resManager) throws FileNotFoundException, URISyntaxException, IOException {
            byte[] ans = null;
            String key = resManager.getBundle().getBundleId() + ":" + relativePath;
            if ((ans = repository.get(key)) == null) {
                FileInputStream in = new FileInputStream(resManager.getNativeResource(relativePath));
                ByteArrayOutputStream dataBank = new ByteArrayOutputStream();
                int offset = 0;
                while ((offset = in.read(buff)) != -1) dataBank.write(buff, 0, offset);
                repository.put(key, ans = dataBank.toByteArray());
                dataBank.close();
            }
            return new ByteArrayInputStream(ans);
        }
    }
}
