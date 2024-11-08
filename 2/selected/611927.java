package tiled.plugins.tiled;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.io.File;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import tiled.core.Map;
import tiled.core.MapLayer;
import tiled.core.Tile;
import tiled.core.TileLayer;
import tiled.core.TileSet;
import tiled.io.ImageHelper;
import tiled.mapeditor.util.TransparentImageFilter;
import tiled.plugins.MapReaderPlugin;
import tiled.util.Base64;
import tiled.util.Util;

/**
 * Reads tiled Maps
 * 
 * @author mtotz
 */
public class MapReader implements MapReaderPlugin {

    private List<String> errorList;

    private String xmlPath = null;

    /** reads the map */
    public Map readMap(String filename) {
        try {
            xmlPath = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
            String xmlFile = makeUrl(filename);
            URL url = new URL(xmlFile);
            InputStream is = url.openStream();
            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            Map unmarshalledMap = unmarshal(is);
            unmarshalledMap.setFilename(filename);
            return unmarshalledMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** reads the map */
    public Map readMap(InputStream inputStream) {
        try {
            xmlPath = makeUrl(".");
            Map unmarshalledMap = unmarshal(inputStream);
            return unmarshalledMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** all filefilters */
    public FileFilter[] getFilters() {
        return new FileFilter[] { new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".tmx");
            }

            public String getDescription() {
                return "Tiled Map Files (*.tmx)";
            }
        } };
    }

    /** returns the description */
    public String getPluginDescription() {
        return "Mapreader for the Tiled compressed map (TMX) format";
    }

    /** */
    public void setMessageList(List<String> errorList) {
        this.errorList = errorList;
    }

    private String makeUrl(String filename) throws MalformedURLException {
        String url = "";
        if (filename.indexOf("://") > 0 || filename.startsWith("file:")) {
            url = filename;
        } else {
            url = (new File(filename)).toURL().toString();
        }
        return url;
    }

    private int reflectFindMethodByName(Class c, String methodName) {
        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equalsIgnoreCase(methodName)) {
                return i;
            }
        }
        return -1;
    }

    private void reflectInvokeMethod(Object invokeVictim, Method method, String[] args) throws InvocationTargetException, Exception {
        Class[] parameterTypes = method.getParameterTypes();
        Object[] conformingArguments = new Object[parameterTypes.length];
        if (args.length < parameterTypes.length) {
            throw new Exception("Insufficient arguments were supplied");
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].getName().equalsIgnoreCase("int")) {
                conformingArguments[i] = new Integer(args[i]);
            } else if (parameterTypes[i].getName().equalsIgnoreCase("float")) {
                conformingArguments[i] = new Float(args[i]);
            } else if (parameterTypes[i].getName().endsWith("String")) {
                conformingArguments[i] = args[i];
            } else if (parameterTypes[i].getName().equalsIgnoreCase("boolean")) {
                conformingArguments[i] = new Boolean(args[i]);
            } else {
                errorList.add("INFO: Unsupported argument type " + parameterTypes[i].getName() + ", defaulting to java.lang.String");
                conformingArguments[i] = args[i];
            }
        }
        method.invoke(invokeVictim, conformingArguments);
    }

    private void setOrientation(Map map, String o) {
        if (o.equalsIgnoreCase("isometric")) {
            map.setOrientation(Map.MDO_ISO);
        } else if (o.equalsIgnoreCase("orthogonal")) {
            map.setOrientation(Map.MDO_ORTHO);
        } else if (o.equalsIgnoreCase("hexagonal")) {
            map.setOrientation(Map.MDO_HEX);
        } else if (o.equalsIgnoreCase("oblique")) {
            map.setOrientation(Map.MDO_OBLIQUE);
        } else if (o.equalsIgnoreCase("shifted")) {
            map.setOrientation(Map.MDO_SHIFTED);
        } else {
            errorList.add("WARN: Unknown orientation '" + o + "'");
        }
    }

    private String getAttributeValue(Node node, String attribname) {
        NamedNodeMap attributes = node.getAttributes();
        String att = null;
        if (attributes != null) {
            Node attribute = attributes.getNamedItem(attribname);
            if (attribute != null) {
                att = attribute.getNodeValue();
            }
        }
        return att;
    }

    private int getAttribute(Node node, String attribname, int def) {
        String attr = getAttributeValue(node, attribname);
        if (attr != null) {
            return Integer.parseInt(attr);
        } else {
            return def;
        }
    }

    private Object unmarshalClass(Class reflector, Node node) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor cons = null;
        try {
            cons = reflector.getConstructor((Class[]) null);
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
            return null;
        }
        Object o = cons.newInstance((Object[]) null);
        Node n;
        Method[] methods = reflector.getMethods();
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                n = nnm.item(i);
                try {
                    int j = reflectFindMethodByName(reflector, "set" + n.getNodeName());
                    if (j >= 0) {
                        reflectInvokeMethod(o, methods[j], new String[] { n.getNodeValue() });
                    } else {
                        errorList.add("WARN: Unsupported attribute '" + n.getNodeName() + "' on <" + node.getNodeName() + "> tag");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return o;
    }

    private Image unmarshalImage(Node t, String baseDir) throws MalformedURLException, IOException {
        Image img = null;
        String source = getAttributeValue(t, "source");
        if (source != null) {
            if (Util.checkRoot(source)) {
                source = makeUrl(source);
            } else {
                source = baseDir + source;
            }
            img = ImageIO.read(new URL(source));
        } else {
            NodeList nl = t.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeName().equals("data")) {
                    Node cdata = n.getFirstChild();
                    if (cdata == null) {
                        errorList.add("WARN: image <data> tag enclosed no " + "data. (empty data tag)");
                    } else {
                        String sdata = cdata.getNodeValue();
                        img = ImageHelper.bytesToImage(Base64.decode(sdata.trim().toCharArray()));
                    }
                    break;
                }
            }
        }
        return img;
    }

    private TileSet unmarshalTilesetFile(Map map, InputStream in, String filename) throws Exception {
        TileSet set = null;
        Node tsNode;
        Document tsDoc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            tsDoc = builder.parse(in, ".");
        } catch (FileNotFoundException fnf) {
            errorList.add("ERROR: Could not find external tileset file " + filename);
        }
        String xmlPathSave = xmlPath;
        if (filename.indexOf(File.separatorChar) >= 0) {
            xmlPath = filename.substring(0, filename.lastIndexOf(File.separatorChar) + 1);
        }
        NodeList tsNodeList = tsDoc.getElementsByTagName("tileset");
        for (int itr = 0; (tsNode = tsNodeList.item(itr)) != null; itr++) {
            set = unmarshalTileset(map, tsNode);
            if (set.getSource() != null) {
                errorList.add("WARN: Recursive external Tilesets are not supported.");
            }
            set.setSource(filename);
            break;
        }
        xmlPath = xmlPathSave;
        return set;
    }

    private TileSet unmarshalTileset(Map map, Node t) throws Exception {
        String source = getAttributeValue(t, "source");
        String basedir = getAttributeValue(t, "basedir");
        int firstGid = getAttribute(t, "firstgid", 1);
        String tilesetBaseDir = xmlPath;
        if (basedir != null) {
            tilesetBaseDir = basedir;
        }
        if (source != null) {
            String filename = tilesetBaseDir + source;
            TileSet ext;
            try {
                InputStream in = new URL(makeUrl(filename)).openStream();
                ext = unmarshalTilesetFile(map, in, filename);
            } catch (FileNotFoundException fnf) {
                errorList.add("ERROR: Could not find external tileset file " + filename);
                ext = new TileSet();
            }
            ext.setFirstGid(firstGid);
            return ext;
        } else {
            int tileWidth = getAttribute(t, "tilewidth", map != null ? map.getTileWidth() : 0);
            int tileHeight = getAttribute(t, "tileheight", map != null ? map.getTileHeight() : 0);
            int tileSpacing = getAttribute(t, "spacing", 0);
            TileSet set = new TileSet();
            set.setName(getAttributeValue(t, "name"));
            set.setBaseDir(basedir);
            set.setFirstGid(firstGid);
            boolean hasTileElements = false;
            NodeList children = t.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeName().equalsIgnoreCase("tile")) {
                    hasTileElements = true;
                }
            }
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeName().equalsIgnoreCase("tile")) {
                    set.addTile(unmarshalTile(child, tilesetBaseDir));
                } else if (child.getNodeName().equalsIgnoreCase("image")) {
                    String imgSource = getAttributeValue(child, "source");
                    String id = getAttributeValue(child, "id");
                    String transStr = getAttributeValue(child, "trans");
                    if (imgSource != null && id == null) {
                        String sourcePath = imgSource;
                        if (!Util.checkRoot(imgSource)) {
                            sourcePath = tilesetBaseDir + imgSource;
                        }
                        if (transStr != null) {
                            Color color = new Color(Integer.parseInt(transStr, 16));
                            Toolkit tk = Toolkit.getDefaultToolkit();
                            try {
                                Image orig = ImageIO.read(new File(sourcePath));
                                Image trans = tk.createImage(new FilteredImageSource(orig.getSource(), new TransparentImageFilter(color.getRGB())));
                                BufferedImage img = new BufferedImage(trans.getWidth(null), trans.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                                img.getGraphics().drawImage(trans, 0, 0, null);
                                set.importTileBitmap(img, tileWidth, tileHeight, tileSpacing, !hasTileElements);
                                set.setTransparentColor(color);
                                set.setTilesetImageFilename(sourcePath);
                            } catch (IIOException iioe) {
                                errorList.add("ERROR: " + iioe.getMessage() + " (" + sourcePath + ")");
                            }
                        } else {
                            set.importTileBitmap(sourcePath, tileWidth, tileHeight, tileSpacing, !hasTileElements);
                        }
                    } else {
                        set.addImage(unmarshalImage(child, tilesetBaseDir), getAttributeValue(child, "id"));
                    }
                }
            }
            return set;
        }
    }

    private Tile unmarshalTile(Node t, String baseDir) throws Exception {
        Tile tile = null;
        try {
            tile = (Tile) unmarshalClass(Tile.class, t);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Properties tileProps = tile.getProperties();
        NodeList children = t.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equalsIgnoreCase("image")) {
                int id = getAttribute(child, "id", -1);
                if (id < 0) {
                    tile.setImage(unmarshalImage(child, baseDir));
                } else {
                    tile.setImage(id);
                    int rotation = getAttribute(child, "rotation", 0);
                    String flipped_s = getAttributeValue(child, "flipped");
                    boolean flipped = (flipped_s != null && flipped_s.equalsIgnoreCase("true"));
                    int orientation;
                    if (rotation == 90) {
                        orientation = (flipped ? 6 : 4);
                    } else if (rotation == 180) {
                        orientation = (flipped ? 2 : 3);
                    } else if (rotation == 270) {
                        orientation = (flipped ? 5 : 7);
                    } else {
                        orientation = (flipped ? 1 : 0);
                    }
                    tile.setImageOrientation(orientation);
                }
            } else if (child.getNodeName().equalsIgnoreCase("property")) {
                tileProps.setProperty(getAttributeValue(child, "name"), getAttributeValue(child, "value"));
            }
        }
        return tile;
    }

    /**
   * Loads a map layer from a layer node.
   */
    private MapLayer unmarshalLayer(Map map, Node t) throws Exception {
        int layerWidth = getAttribute(t, "width", map.getWidth());
        int layerHeight = getAttribute(t, "height", map.getHeight());
        TileLayer ml = new TileLayer(layerWidth, layerHeight);
        int offsetX = getAttribute(t, "x", 0);
        int offsetY = getAttribute(t, "y", 0);
        String opacity = getAttributeValue(t, "opacity");
        ml.setOffset(offsetX, offsetY);
        ml.setName(getAttributeValue(t, "name"));
        if (opacity != null) {
            ml.setOpacity(Float.parseFloat(opacity));
        }
        Properties mlProps = ml.getProperties();
        for (Node child = t.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeName().equalsIgnoreCase("data")) {
                String encoding = getAttributeValue(child, "encoding");
                if (encoding != null && encoding.equalsIgnoreCase("base64")) {
                    Node cdata = child.getFirstChild();
                    if (cdata == null) {
                        errorList.add("WARN: layer <data> tag enclosed no data. (empty data tag)");
                    } else {
                        char[] enc = cdata.getNodeValue().trim().toCharArray();
                        byte[] dec = Base64.decode(enc);
                        ByteArrayInputStream bais = new ByteArrayInputStream(dec);
                        InputStream is;
                        String comp = getAttributeValue(child, "compression");
                        if (comp != null && comp.equalsIgnoreCase("gzip")) {
                            is = new GZIPInputStream(bais);
                        } else {
                            is = bais;
                        }
                        for (int y = 0; y < ml.getHeight(); y++) {
                            for (int x = 0; x < ml.getWidth(); x++) {
                                int tileId = 0;
                                tileId |= is.read();
                                tileId |= is.read() << 8;
                                tileId |= is.read() << 16;
                                tileId |= is.read() << 24;
                                TileSet ts = map.findTileSetForTileGID(tileId);
                                if (ts != null) {
                                    ml.setTileAt(x, y, ts.getTile(tileId - ts.getFirstGid()));
                                } else {
                                    ml.setTileAt(x, y, map.getNullTile());
                                }
                            }
                        }
                    }
                } else {
                    int x = 0, y = 0;
                    for (Node dataChild = child.getFirstChild(); dataChild != null; dataChild = dataChild.getNextSibling()) {
                        if (dataChild.getNodeName().equalsIgnoreCase("tile")) {
                            int tileId = getAttribute(dataChild, "gid", -1);
                            TileSet ts = map.findTileSetForTileGID(tileId);
                            if (ts != null) {
                                ml.setTileAt(x, y, ts.getTile(tileId - ts.getFirstGid()));
                            } else {
                                ml.setTileAt(x, y, map.getNullTile());
                            }
                            x++;
                            if (x == ml.getWidth()) {
                                x = 0;
                                y++;
                            }
                            if (y == ml.getHeight()) {
                                break;
                            }
                        }
                    }
                }
            } else if (child.getNodeName().equalsIgnoreCase("property")) {
                mlProps.setProperty(getAttributeValue(child, "name"), getAttributeValue(child, "value"));
            }
        }
        return ml;
    }

    private Map buildMap(Document doc) throws Exception {
        Node item, mapNode;
        mapNode = doc.getDocumentElement();
        Map map = null;
        if (!mapNode.getNodeName().equals("map")) {
            throw new Exception("Not a valid tmx map file.");
        }
        int mapWidth = getAttribute(mapNode, "width", 0);
        int mapHeight = getAttribute(mapNode, "height", 0);
        if (mapWidth > 0 && mapHeight > 0) {
            map = new Map(mapWidth, mapHeight);
        } else {
            NodeList l = doc.getElementsByTagName("dimensions");
            for (int i = 0; (item = l.item(i)) != null; i++) {
                if (item.getParentNode() == mapNode) {
                    mapWidth = getAttribute(item, "width", 0);
                    mapHeight = getAttribute(item, "height", 0);
                    if (mapWidth > 0 && mapHeight > 0) {
                        map = new Map(mapWidth, mapHeight);
                    }
                }
            }
        }
        if (map == null) {
            throw new Exception("Couldn't locate map dimensions.");
        }
        String orientation = getAttributeValue(mapNode, "orientation");
        int tileWidth = getAttribute(mapNode, "tilewidth", 0);
        int tileHeight = getAttribute(mapNode, "tileheight", 0);
        if (tileWidth > 0) {
            map.setTileWidth(tileWidth);
        }
        if (tileHeight > 0) {
            map.setTileHeight(tileHeight);
        }
        if (orientation != null) {
            setOrientation(map, orientation);
        } else {
            setOrientation(map, "orthogonal");
        }
        Properties mapProps = map.getProperties();
        for (Node sibs = mapNode.getFirstChild(); sibs != null; sibs = sibs.getNextSibling()) {
            if (sibs.getNodeName().equals("tileset")) {
                map.addTileset(unmarshalTileset(map, sibs));
            } else if (sibs.getNodeName().equals("property")) {
                mapProps.setProperty(getAttributeValue(sibs, "name"), getAttributeValue(sibs, "value"));
            } else if (sibs.getNodeName().equals("layer")) {
                MapLayer layer = unmarshalLayer(map, sibs);
                if (layer != null) {
                    map.addLayer(layer);
                }
            } else if (sibs.getNodeName().equals("objectgroup")) {
            }
        }
        return map;
    }

    private Map unmarshal(InputStream in) throws IOException, Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(in, xmlPath);
            return buildMap(doc);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error while parsing map file: " + e.toString());
        }
    }
}
