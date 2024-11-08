package org.magnesia.chalk;

import static org.magnesia.misc.Utils.log;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.magnesia.JImage;
import org.magnesia.Pair;
import org.magnesia.chalk.data.Constants;
import org.magnesia.misc.Utils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class Handler {

    private final String base;

    private int tbwidth = org.magnesia.Constants.THUMBNAIL_WIDTH;

    private String validDirs;

    private boolean ro = true;

    Handler(String b) {
        if (b != null && !"".equals(b)) base = b; else base = Constants.DEFAULT_PATH;
        File f = new File(base);
        if (!f.exists()) f.mkdirs();
    }

    void setAllowedDirectories(String regex) {
        this.validDirs = regex;
    }

    private String getDirectory(String path) {
        if (!path.contains("/")) return path;
        return path.substring(0, path.lastIndexOf("/"));
    }

    void setReadOnly(boolean ro) {
        this.ro = ro;
    }

    private boolean isDirAllowed(String dir) {
        if (validDirs == null || "".equals(validDirs)) return true;
        try {
            return dir.matches(validDirs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void storeImage(String path, String name, byte[] data) {
        if (!isDirAllowed(path) || ro) return;
        try {
            File f = new File(base + "/" + path + "/" + name);
            String extension = name.substring(name.lastIndexOf(".") + 1);
            if (name.contains(".")) name = name.substring(0, name.lastIndexOf("."));
            int i = 0;
            while (f.exists()) {
                i++;
                f = new File(base + "/" + path + "/" + name + "-" + i + "." + extension);
            }
            log("Writing " + f.getCanonicalPath());
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Pair<JImage, byte[]> getImage(String path, int width, boolean download) {
        if (!isDirAllowed(getDirectory(path))) return null;
        try {
            String name = path.substring(path.lastIndexOf("/") + 1);
            if (name.contains(".")) name = name.substring(0, name.lastIndexOf("."));
            File f = new File(base + "/" + path);
            if (f.exists()) {
                byte[] data;
                long time = System.currentTimeMillis();
                FileInputStream fs = new FileInputStream(f);
                if (download) {
                    int read = 0;
                    byte[] chunk = new byte[org.magnesia.Constants.CHUNK_SIZE];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    while ((read = fs.read(chunk)) > 0) {
                        bos.write(chunk, 0, read);
                    }
                    fs.close();
                    data = bos.toByteArray();
                } else {
                    ImageReader reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
                    ImageInputStream iis = ImageIO.createImageInputStream(fs);
                    reader.setInput(iis);
                    BufferedImage orig = reader.read(0);
                    reader.dispose();
                    fs.close();
                    BufferedImage bi = Utils.drawScaled(orig, (width > 0 && width < orig.getWidth()) ? width : orig.getWidth());
                    data = Utils.getData(bi);
                    log("Time needed to load, scale and draw image: " + (System.currentTimeMillis() - time) + "ms");
                }
                return new Pair<JImage, byte[]>(new JImage(path.substring(0, path.lastIndexOf("/")), path.substring(path.lastIndexOf("/") + 1), f.length()), data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    Pair<JImage, byte[]> getThumbnail(String path) {
        return getImage(path, tbwidth, false);
    }

    List<String> listEntries(String path) {
        List<String> entries = new ArrayList<String>();
        File file = new File(base + "/" + path);
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) if (f.isDirectory() && isDirAllowed(path + "/" + f.getName())) entries.add(f.getName());
            Collections.sort(entries);
        }
        return entries;
    }

    List<String> listImages(String path) {
        List<String> entries = new ArrayList<String>();
        if (!isDirAllowed(path)) return entries;
        File files = new File(base + "/" + path);
        if (files.canRead()) {
            for (File f : files.listFiles(new FileFilter() {

                public boolean accept(File arg0) {
                    return Utils.accept(arg0);
                }
            })) if (f.isFile()) entries.add(f.getName());
            Collections.sort(entries);
        }
        return entries;
    }

    boolean createDirectory(String path) {
        if (!isDirAllowed(path) || ro) return false;
        File f = new File(base + "/" + path);
        if (!f.exists()) {
            return f.mkdir();
        }
        return false;
    }

    boolean rename(String old, String newPath) {
        if (!isDirAllowed(getDirectory(old)) || !isDirAllowed(getDirectory(newPath)) || ro) return false;
        File fNew = new File(base + "/" + newPath);
        if (!fNew.exists()) {
            return new File(base + "/" + old).renameTo(fNew);
        }
        return false;
    }

    boolean setThumbnailWidth(int width) {
        if (width >= 50 && width <= 500) {
            tbwidth = width;
            log("New thumbnailwidth " + tbwidth);
        } else {
            log("Tried setting illegal thumbnailwidth " + width);
        }
        return tbwidth == width;
    }

    /**
	 * Rotate or flip the image located at path
	 *
	 * If direction is true the image is either rotated clockwise or
	 * flipped horizontally
	 */
    byte[] rotateOrFlip(String path, boolean rotate, boolean direction) {
        if (!isDirAllowed(getDirectory(path)) || ro) return new byte[] {};
        try {
            path = base + "/" + path;
            File f = new File(path);
            if (f.exists()) {
                FileInputStream fs = new FileInputStream(f);
                ImageReader reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
                ImageInputStream iis = ImageIO.createImageInputStream(fs);
                reader.setInput(iis);
                IIOMetadata meta = reader.getImageMetadata(0);
                IIOMetadata stream = reader.getStreamMetadata();
                BufferedImage orig = reader.read(0);
                reader.dispose();
                fs.close();
                int height = orig.getHeight();
                int width = orig.getWidth();
                BufferedImage bi = null;
                if (rotate) {
                    bi = new BufferedImage(height, width, orig.getType());
                    if (direction) {
                        for (int i = 0; i < width; i++) {
                            for (int j = 0; j < height; j++) {
                                bi.setRGB(height - j - 1, i, orig.getRGB(i, j));
                            }
                        }
                    } else {
                        for (int i = 0; i < width; i++) {
                            for (int j = 0; j < height; j++) {
                                bi.setRGB(j, width - i - 1, orig.getRGB(i, j));
                            }
                        }
                    }
                } else {
                    bi = new BufferedImage(width, height, orig.getType());
                    if (direction) {
                        for (int i = 0; i < width; i++) {
                            for (int j = 0; j < height; j++) {
                                bi.setRGB(width - i - 1, j, orig.getRGB(i, j));
                            }
                        }
                    } else {
                        for (int i = 0; i < width; i++) {
                            for (int j = 0; j < height; j++) {
                                bi.setRGB(i, height - j - 1, orig.getRGB(i, j));
                            }
                        }
                    }
                }
                Utils.writeFile(bi, new File(path), meta, stream);
                bi = Utils.drawScaled(bi, tbwidth);
                byte[] data = Utils.getData(bi);
                return data;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    void setComment(String path, String comment) {
        if (!isDirAllowed(getDirectory(path)) || ro) return;
        try {
            path = base + "/" + path;
            File f = new File(path);
            if (f.exists()) {
                FileInputStream fs = new FileInputStream(f);
                ImageReader reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
                ImageInputStream iis = ImageIO.createImageInputStream(fs);
                reader.setInput(iis);
                IIOImage img = reader.readAll(0, null);
                IIOMetadata meta = img.getMetadata();
                IIOMetadata stream = reader.getStreamMetadata();
                reader.dispose();
                fs.close();
                if (!meta.isReadOnly()) {
                    comment = URLEncoder.encode(comment, "UTF-8");
                    Node n = meta.getAsTree("javax_imageio_1.0");
                    Element root = getCommentNode((Element) n);
                    if (root != null) {
                        root.getParentNode().removeChild(root);
                    }
                    root = new IIOMetadataNode("javax_imageio_1.0");
                    Element e2 = new IIOMetadataNode("Text");
                    Element e3 = new IIOMetadataNode("TextEntry");
                    e3.setAttribute("keyword", "comment");
                    e3.setAttribute("encoding", "unicode");
                    e3.setAttribute("value", comment);
                    e2.appendChild(e3);
                    root.appendChild(e2);
                    meta.mergeTree("javax_imageio_1.0", root);
                    Utils.writeFile(img, f, stream);
                } else {
                    log("Metadata is readonly. Not writing comment!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Element getCommentNode(Element root) {
        try {
            Element e = (Element) root.getElementsByTagName("Text").item(0);
            NodeList nl = e.getElementsByTagName("TextEntry");
            log("Found " + nl.getLength() + " comment node" + (nl.getLength() == 1 ? "" : "s"));
            for (int i = nl.getLength() - 1; i >= 0; i--) {
                Element t = (Element) nl.item(i);
                if ("comment".equals(t.getAttribute("keyword"))) {
                    return t;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    String getComment(String path) {
        if (!isDirAllowed(getDirectory(path))) return "";
        String comment = "";
        try {
            IIOMetadata meta = readMetadata(path);
            if (meta != null) {
                Element e = getCommentNode((Element) meta.getAsTree("javax_imageio_1.0"));
                if (e != null) {
                    comment = URLDecoder.decode(e.getAttribute("value"), "UTF-8");
                }
            }
        } catch (Exception io) {
        }
        return comment;
    }

    private IIOMetadata readMetadata(String path) throws IOException {
        path = base + "/" + path;
        File f = new File(path);
        if (f.exists()) {
            IIOMetadata meta = null;
            ImageReader reader = null;
            FileInputStream fs = new FileInputStream(f);
            try {
                reader = ImageIO.getImageReadersByMIMEType("image/jpeg").next();
                ImageInputStream iis = ImageIO.createImageInputStream(fs);
                reader.setInput(iis);
                meta = reader.getImageMetadata(0);
            } catch (IOException io) {
                throw io;
            } finally {
                if (reader != null) reader.dispose();
                if (fs != null) fs.close();
            }
            return meta;
        }
        return null;
    }

    Map<String, String> getImageInfos(String path) {
        Map<String, String> infos = new HashMap<String, String>();
        if (!isDirAllowed(getDirectory(path))) return infos;
        try {
            IIOMetadata meta = readMetadata(path);
            if (meta != null) {
                for (String contexts : meta.getMetadataFormatNames()) {
                    traverseTree((Element) meta.getAsTree(contexts), infos, contexts);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return infos;
    }

    private void traverseTree(Element e, Map<String, String> entries, String base) {
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                e = (Element) n;
                traverseTree(e, entries, base + "$" + e.getNodeName());
                NamedNodeMap nnm = e.getAttributes();
                for (int j = 0; j < nnm.getLength(); j++) {
                    String key = base + "$" + e.getNodeName() + "#" + nnm.item(j).getNodeName();
                    key = key.replaceFirst(Pattern.quote("javax_imageio_1.0"), "GENERAL");
                    key = key.replaceFirst(Pattern.quote("javax_imageio_jpeg_image_1.0"), "JPEG");
                    entries.put(key, nnm.item(j).getNodeValue());
                }
            }
        }
    }
}
