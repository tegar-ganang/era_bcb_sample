package tiled.plugins.tiled;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;
import tiled.core.*;
import tiled.io.*;
import tiled.mapeditor.selection.SelectionLayer;
import tiled.util.*;

/**
 * Writes tiled maps  
 */
public class XMLMapWriter implements tiled.io.MapWriter, FileFilter {

    /**
     * Saves a map to an XML file.
     *
     * @param filename the filename of the map file
     */
    public void writeMap(Map map, String filename) throws Exception {
        OutputStream os = new FileOutputStream(filename);
        if (filename.endsWith(".tmx.gz")) {
            os = new GZIPOutputStream(os);
        }
        Writer writer = new OutputStreamWriter(os);
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.startDocument();
        writeMap(map, xmlWriter, filename);
        xmlWriter.endDocument();
        writer.flush();
        if (filename.endsWith(".tmx.gz")) {
            ((GZIPOutputStream) os).finish();
        }
    }

    /**
     * Saves a tileset to an XML file.
     *
     * @param filename the filename of the tileset file
     */
    public void writeTileset(TileSet set, String filename) throws Exception {
        OutputStream os = new FileOutputStream(filename);
        Writer writer = new OutputStreamWriter(os);
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.startDocument();
        writeTileset(set, xmlWriter, filename);
        xmlWriter.endDocument();
        writer.flush();
    }

    public void writeMap(Map map, OutputStream out) throws Exception {
        Writer writer = new OutputStreamWriter(out);
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.startDocument();
        writeMap(map, xmlWriter, "/.");
        xmlWriter.endDocument();
        writer.flush();
    }

    public void writeTileset(TileSet set, OutputStream out) throws Exception {
        Writer writer = new OutputStreamWriter(out);
        XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.startDocument();
        writeTileset(set, xmlWriter, "/.");
        xmlWriter.endDocument();
        writer.flush();
    }

    private void writeMap(Map map, XMLWriter w, String wp) throws IOException {
        try {
            w.startElement("map");
            w.writeAttribute("version", "0.99a");
            switch(map.getOrientation()) {
                case Map.MDO_ORTHO:
                    w.writeAttribute("orientation", "orthogonal");
                    break;
                case Map.MDO_ISO:
                    w.writeAttribute("orientation", "isometric");
                    break;
                case Map.MDO_OBLIQUE:
                    w.writeAttribute("orientation", "oblique");
                    break;
                case Map.MDO_HEX:
                    w.writeAttribute("orientation", "hexagonal");
                    break;
                case Map.MDO_SHIFTED:
                    w.writeAttribute("orientation", "shifted");
                    break;
            }
            w.writeAttribute("width", "" + map.getWidth());
            w.writeAttribute("height", "" + map.getHeight());
            w.writeAttribute("tilewidth", "" + map.getTileWidth());
            w.writeAttribute("tileheight", "" + map.getTileHeight());
            Properties props = map.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }
            int firstgid = 1;
            Iterator itr = map.getTilesets().iterator();
            while (itr.hasNext()) {
                TileSet tileset = (TileSet) itr.next();
                tileset.setFirstGid(firstgid);
                writeTilesetReference(tileset, w, wp);
                firstgid += tileset.getMaxTileId() + 1;
            }
            Iterator ml = map.iterator();
            while (ml.hasNext()) {
                MapLayer layer = (MapLayer) ml.next();
                writeMapLayer(layer, w);
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a reference to an external tileset into a XML document.  In the
     * degenerate case where the tileset is not stored in an external file,
     * writes the contents of the tileset instead.
     */
    private void writeTilesetReference(TileSet set, XMLWriter w, String wp) throws IOException {
        try {
            String source = set.getSource();
            if (source == null) {
                this.writeTileset(set, w, wp);
            } else {
                w.startElement("tileset");
                try {
                    w.writeAttribute("firstgid", "" + set.getFirstGid());
                    w.writeAttribute("source", source.substring(source.lastIndexOf(File.separatorChar) + 1));
                    if (set.getBaseDir() != null) {
                        w.writeAttribute("basedir", set.getBaseDir());
                    }
                } finally {
                    w.endElement();
                }
            }
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private void writeTileset(TileSet set, XMLWriter w, String wp) throws IOException {
        try {
            String tilebmpFile = set.getTilebmpFile();
            String name = set.getName();
            w.startElement("tileset");
            if (name != null) {
                w.writeAttribute("name", name);
            }
            w.writeAttribute("firstgid", "" + set.getFirstGid());
            if (tilebmpFile != null) {
                w.writeAttribute("tilewidth", "" + set.getStandardWidth());
                w.writeAttribute("tileheight", "" + set.getStandardHeight());
            }
            if (set.getBaseDir() != null) {
                w.writeAttribute("basedir", set.getBaseDir());
            }
            if (tilebmpFile != null) {
                w.startElement("image");
                w.writeAttribute("source", getRelativePath(wp, tilebmpFile));
                Color trans = set.getTransparentColor();
                if (trans != null) {
                    w.writeAttribute("trans", Integer.toHexString(trans.getRGB()).substring(2));
                }
                w.endElement();
            } else {
                TiledConfiguration conf = TiledConfiguration.getInstance();
                if (conf.keyHasValue("tmx.save.tileSetImages", "1")) {
                    Iterator<String> ids = set.getImageIds().iterator();
                    while (ids.hasNext()) {
                        String id = ids.next();
                        w.startElement("image");
                        w.writeAttribute("format", "png");
                        w.writeAttribute("id", id);
                        w.startElement("data");
                        w.writeAttribute("encoding", "base64");
                        w.writeCDATA(new String(Base64.encode(ImageHelper.imageToPNG(set.getImageById(id)))));
                        w.endElement();
                        w.endElement();
                    }
                } else if (conf.keyHasValue("tmx.save.embedImages", "0")) {
                    String imgSource = conf.getValue("tmx.save.tileImagePrefix") + "set.png";
                    w.writeAttribute("source", imgSource);
                    String tilesetFilename = (wp.substring(0, wp.lastIndexOf(File.separatorChar) + 1) + imgSource);
                    FileOutputStream fw = new FileOutputStream(new File(tilesetFilename));
                    fw.close();
                }
                if (set.isOneForOne()) {
                    Iterator tileIterator = set.iterator();
                    boolean needWrite = false;
                    if (conf.keyHasValue("tmx.save.embedImages", "1")) {
                        needWrite = true;
                    } else {
                        while (tileIterator.hasNext()) {
                            Tile tile = (Tile) tileIterator.next();
                            if (!tile.getProperties().isEmpty()) {
                                needWrite = true;
                                break;
                            }
                        }
                    }
                    if (needWrite) {
                        tileIterator = set.iterator();
                        while (tileIterator.hasNext()) {
                            Tile tile = (Tile) tileIterator.next();
                            writeTile(tile, w);
                        }
                    }
                }
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes this layer to an XMLWriter. This should be done <b>after</b> the
     * first global ids for the tilesets are determined, in order for the right
     * gids to be written to the layer data.
     */
    private void writeMapLayer(MapLayer l, XMLWriter w) throws IOException {
        try {
            TiledConfiguration conf = TiledConfiguration.getInstance();
            boolean encodeLayerData = conf.keyHasValue("tmx.save.encodeLayerData", "1");
            boolean compressLayerData = conf.keyHasValue("tmx.save.layerCompression", "1") && encodeLayerData;
            Rectangle bounds = l.getBounds();
            if (l.getClass() == SelectionLayer.class) {
                w.startElement("selection");
            } else {
                w.startElement("layer");
            }
            w.writeAttribute("name", l.getName());
            w.writeAttribute("width", "" + bounds.width);
            w.writeAttribute("height", "" + bounds.height);
            if (bounds.x != 0) {
                w.writeAttribute("xoffset", "" + bounds.x);
            }
            if (bounds.y != 0) {
                w.writeAttribute("yoffset", "" + bounds.y);
            }
            if (!l.isVisible()) {
                w.writeAttribute("visible", "0");
            }
            if (l.getOpacity() < 1.0f) {
                w.writeAttribute("opacity", "" + l.getOpacity());
            }
            Properties props = l.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }
            w.startElement("data");
            if (encodeLayerData) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream out;
                w.writeAttribute("encoding", "base64");
                if (compressLayerData) {
                    w.writeAttribute("compression", "gzip");
                    out = new GZIPOutputStream(baos);
                } else {
                    out = baos;
                }
                for (int y = 0; y < l.getHeight(); y++) {
                    for (int x = 0; x < l.getWidth(); x++) {
                        Tile tile = ((TileLayer) l).getTileAt(x, y);
                        int gid = 0;
                        if (tile != null) {
                            gid = tile.getGid();
                        }
                        out.write((gid) & 0x000000FF);
                        out.write((gid >> 8) & 0x000000FF);
                        out.write((gid >> 16) & 0x000000FF);
                        out.write((gid >> 24) & 0x000000FF);
                    }
                }
                if (compressLayerData) {
                    ((GZIPOutputStream) out).finish();
                }
                w.writeCDATA(new String(Base64.encode(baos.toByteArray())));
            } else {
                for (int y = 0; y < l.getHeight(); y++) {
                    for (int x = 0; x < l.getWidth(); x++) {
                        Tile tile = ((TileLayer) l).getTileAt(x, y);
                        int gid = 0;
                        if (tile != null) {
                            gid = tile.getGid();
                        }
                        w.startElement("tile");
                        w.writeAttribute("gid", "" + gid);
                        w.endElement();
                    }
                }
            }
            w.endElement();
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private void writeTile(Tile tile, XMLWriter w) throws IOException {
        try {
            w.startElement("tile");
            int tileId = tile.getId();
            w.writeAttribute("id", "" + tileId);
            Properties props = tile.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                String key = (String) keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }
            Image tileImage = tile.getImage();
            TiledConfiguration conf = TiledConfiguration.getInstance();
            if (tileImage != null && !conf.keyHasValue("tmx.save.tileSetImages", "1")) {
                if (conf.keyHasValue("tmx.save.embedImages", "1") && conf.keyHasValue("tmx.save.tileSetImages", "0")) {
                    w.startElement("image");
                    w.writeAttribute("format", "png");
                    w.startElement("data");
                    w.writeAttribute("encoding", "base64");
                    w.writeCDATA(new String(Base64.encode(ImageHelper.imageToPNG(tileImage))));
                    w.endElement();
                    w.endElement();
                } else if (conf.keyHasValue("tmx.save.tileSetImages", "1")) {
                    w.startElement("image");
                    w.writeAttribute("id", "" + tile.getImageId());
                    int orientation = tile.getImageOrientation();
                    int rotation = 0;
                    boolean flipped = (orientation & 1) == ((orientation & 2) >> 1);
                    if ((orientation & 4) == 4) {
                        rotation = ((orientation & 1) == 1) ? 270 : 90;
                    } else {
                        rotation = ((orientation & 2) == 2) ? 180 : 0;
                    }
                    if (rotation != 0) w.writeAttribute("rotation", "" + rotation);
                    if (flipped) w.writeAttribute("flipped", "true");
                    w.endElement();
                } else {
                    String prefix = conf.getValue("tmx.save.tileImagePrefix");
                    String filename = conf.getValue("tmx.save.maplocation") + prefix + tileId + ".png";
                    w.startElement("image");
                    w.writeAttribute("source", prefix + tileId + ".png");
                    FileOutputStream fw = new FileOutputStream(new File(filename));
                    byte[] data = ImageHelper.imageToPNG(tileImage);
                    fw.write(data, 0, data.length);
                    fw.close();
                    w.endElement();
                }
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the relative path from one file to the other. The function
     * expects absolute paths, relative paths will be converted to absolute
     * using the working directory.
     *
     * @param from the path of the origin file
     * @param to   the path of the destination file
     * @return     the relative path from origin to destination
     */
    public static String getRelativePath(String from, String to) {
        try {
            from = new File(from).getCanonicalPath();
            to = new File(to).getCanonicalPath();
        } catch (IOException e) {
        }
        File fromFile = new File(from);
        File toFile = new File(to);
        List<String> fromParents = new ArrayList<String>();
        List<String> toParents = new ArrayList<String>();
        while (fromFile != null) {
            fromParents.add(0, fromFile.getName());
            fromFile = fromFile.getParentFile();
        }
        while (toFile != null) {
            toParents.add(0, toFile.getName());
            toFile = toFile.getParentFile();
        }
        int shared = 0;
        int maxShared = Math.min(fromParents.size(), toParents.size());
        for (shared = 0; shared < maxShared; shared++) {
            String fromParent = (String) fromParents.get(shared);
            String toParent = (String) toParents.get(shared);
            if (!fromParent.equals(toParent)) {
                break;
            }
        }
        StringBuffer relPathBuf = new StringBuffer();
        for (int i = shared; i < fromParents.size() - 1; i++) {
            relPathBuf.append(".." + File.separator);
        }
        for (int i = shared; i < toParents.size() - 1; i++) {
            relPathBuf.append(toParents.get(i) + File.separator);
        }
        relPathBuf.append(new File(to).getName());
        String relPath = relPathBuf.toString();
        try {
            String absPath = new File(relPath).getCanonicalPath();
            if (!absPath.equals(relPath)) {
                relPath = relPath.replace('\\', '/');
            }
        } catch (Exception e) {
        }
        return relPath;
    }

    /**
     *
     */
    public String getFilter() throws Exception {
        return "*.tmx,*.tsx,*.tmx.gz";
    }

    public String getPluginPackage() {
        return "Tiled internal TMX reader/writer";
    }

    public String getDescription() {
        return "The core Tiled TMX format writer\n" + "\n" + "Tiled Map Editor, (c) 2005\n" + "Adam Turk\n" + "Bjorn Lindeijer";
    }

    public String getName() {
        return "Default Tiled XML (TMX) map writer";
    }

    public boolean accept(File pathname) {
        try {
            String path = pathname.getCanonicalPath();
            if (path.endsWith(".tmx") || path.endsWith(".tsx") || path.endsWith(".tmx.gz")) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public void setErrorStack(Stack<String> es) {
    }

    /** returns a list of available filefilters */
    public FileFilter[] getFilters() {
        return new FileFilter[] { this };
    }
}
