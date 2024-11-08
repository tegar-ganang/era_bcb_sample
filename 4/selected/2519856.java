package org.chon.web.api.res;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.chon.web.api.Resource;
import org.chon.web.api.ServerInfo;
import org.chon.web.api.res.image.ImageTransformer;

public class ImageResource implements Resource {

    public static final Map<String, Point> imageFormats = new HashMap<String, Point>();

    private URL imageUrl;

    private String type;

    private int width;

    private int height;

    private String fmt = null;

    private int resizeCut = 0;

    private InputStream inputStream = null;

    public static Resource create(URL url, String type, int width, int height) {
        return create(url, type, width, height, 0);
    }

    public static Resource create(URL url, String type, int width, int height, int resizeCut) {
        ImageResource img = new ImageResource();
        img.imageUrl = url;
        img.type = type;
        img.width = width;
        img.height = height;
        img.resizeCut = resizeCut;
        return img;
    }

    public static Resource create(URL url, String type) {
        return create(url, type, -1, -1);
    }

    public static Resource create(URL url) {
        return create(url, "jpg");
    }

    @Override
    public void process(ServerInfo si) {
        HttpServletRequest request = si.getRequest();
        HttpServletResponse response = si.getResponse();
        try {
            doImageProcess(request, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doImageProcess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("image/" + type + "");
        Point imgSize = null;
        if (width > 0 || height > 0) {
            imgSize = new Point(width, height);
        }
        if (fmt != null && imageFormats.containsKey(fmt)) {
            imgSize = imageFormats.get(fmt);
        }
        InputStream imageInputStream = inputStream != null ? inputStream : imageUrl.openStream();
        if (imageInputStream == null) {
            throw new RuntimeException("File " + imageUrl + " does not exist!");
        }
        if (imgSize == null) {
            IOUtils.copy(imageInputStream, response.getOutputStream());
        } else {
            byte[] imageBytes = getImageBytes(type, imgSize, imageInputStream);
            response.setContentLength(imageBytes.length);
            response.getOutputStream().write(imageBytes);
        }
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private byte[] getImageBytes(String type, Point imgSize, InputStream image) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageTransformer.scaleImage(image, imgSize.x, imgSize.y, resizeCut != 0 ? resizeCut : ImageTransformer.TYPE_STRECH_CUT, type, bos);
        byte[] image_bytes = bos.toByteArray();
        return image_bytes;
    }

    public static Resource create(InputStream is, String type, int width, int height) {
        return create(is, type, width, height, 0);
    }

    public static Resource create(InputStream is, String type, int width, int height, int resizeCut) {
        ImageResource img = new ImageResource();
        img.inputStream = is;
        img.type = type;
        img.width = width;
        img.height = height;
        img.resizeCut = resizeCut;
        return img;
    }

    public static void addFormats(Map<String, Point> fmts) {
        imageFormats.putAll(fmts);
    }

    public static Resource create(InputStream is, String type, int width, int height, int resizeCut, String format) {
        ImageResource ir = (ImageResource) create(is, type, width, height, resizeCut);
        ir.fmt = format;
        return ir;
    }
}
