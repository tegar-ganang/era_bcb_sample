package org.cyberaide.set;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.cyberaide.core.CoGObject;
import org.cyberaide.core.CoGObjectsUtil;
import org.cyberaide.core.CoGPrinter;
import org.cyberaide.util.Path;
import org.json.XML;

public class CoGSet implements Iterable<CoGObject>, Serializable {

    static Logger log = Logger.getLogger(CoGSet.class);

    String ATTRIBUTE_SEPARATOR = ":";

    String QUOTE_STRING = "\"";

    String NEWLINE = "\n";

    String META_ASCII = "[metadata]";

    public static final String ASCII = "ascii";

    protected List<CoGObject> pool = new ArrayList<CoGObject>();

    protected CoGObject meta = new CoGObject();

    ;

    protected String filename = "";

    protected String filetype = ASCII;

    private String name;

    public void SetSeperator(String s) {
        ATTRIBUTE_SEPARATOR = s;
    }

    /**
     * Class constructor. Creates an empty set.
     */
    public CoGSet() {
    }

    /**
     * Class constructor. Creates an empty set using the label and the type.
     * 
     * @param label
     *            Label for the set.
     * @param type
     *            Type of the set.
     */
    public CoGSet(String label, String type) {
        this.name = label;
        setMeta("label", label);
        setMeta("type", type);
    }

    public CoGSet(List<CoGObject> pool) {
        for (CoGObject o : pool) this.pool.add(o);
    }

    public CoGSet(CoGObject[] pool) {
        for (CoGObject o : pool) this.pool.add(o);
    }

    /**
     * Class constructor. Creates a set using the label and array of items.
     * 
     * @param label
     *            Label for the set.- No such file or directory
     * 
     * @param set
     *            Array of items to load in the set.
     */
    public CoGSet(String label, CoGObject[] set) {
        setMeta("label", label);
        pool = new ArrayList<CoGObject>(Arrays.asList(set));
    }

    /*******************************************************************
     * end: constructors
     ******************************************************************/
    public boolean isEmpty() {
        return pool.isEmpty();
    }

    /**
     * Add an item to the set.
     * 
     * @param o
     *            Item to be added.
     */
    public void add(CoGObject o) {
        pool.add(o);
    }

    /**
     * Add a set of items of the same type to this set. Union set operation.
     * 
     * @param set
     *            Set to be joined with.
     * @return Resultant set.
     */
    public CoGSet addAll(List<CoGObject> cogObjectList) {
        pool.addAll(cogObjectList);
        return this;
    }

    /**
     * Add a set of items of the same type to this set. Union set operation.
     * 
     * @param set
     *            Set to be joined with.
     * @return Resultant set.
     */
    public void addSet(CoGSet set) {
        this.addAll(set.getList());
    }

    /**
     * Remove all of these items.
     * 
     * @param items
     *            Labels of the items to be removed.
     * @return Resultant set.
     */
    public CoGSet removeAll(String[] items) {
        List<CoGObject> rm = new ArrayList<CoGObject>();
        for (CoGObject o : pool) {
            for (String label : items) {
                if (label.equalsIgnoreCase(o.get("label"))) rm.add(o);
            }
        }
        pool.removeAll(rm);
        return this;
    }

    /**
     * Clear the set.
     * 
     */
    public void clear() {
        pool.clear();
    }

    /**
     * Remove the item from the set.
     * 
     * @param o
     *            Item to be removed.
     * @return true - If the item was found and hence removed false - otherwise
     */
    public boolean remove(CoGObject o) {
        return pool.remove(o);
    }

    /**
     * Remove an item from the set at the specified index.
     * 
     * @param i
     *            Position of the item to be removed.
     * @return CoGObject that was removed.
     *         <p>
     *         null - index was out of range
     */
    public CoGObject remove(int i) {
        CoGObject itemRemoved = null;
        if (i >= 0 && i < pool.size()) {
            itemRemoved = pool.remove(i);
        }
        return itemRemoved;
    }

    public boolean removeByAttributeValue(String attribute, String value) {
        List<CoGObject> remove_set = new ArrayList<CoGObject>();
        for (CoGObject cogObj : pool) {
            if (cogObj.hasKey(attribute) && cogObj.get(attribute).equalsIgnoreCase(value)) remove_set.add(cogObj);
        }
        pool.removeAll(remove_set);
        return (remove_set.size() > 0);
    }

    /**
     * Remove objects that have attributes with the specified values
     * 
     * @param param
     *            Attribute names and values
     * @return Boolean result
     */
    public boolean removeObjects(String... param) {
        List<CoGObject> remove_set = new ArrayList<CoGObject>();
        if (param.length % 2 != 0) {
            log.error("Internal error (wrong number of function parameters).");
            return false;
        }
        for (CoGObject cogObj : pool) {
            boolean delete = true;
            for (int i = 0; i < param.length / 2; i++) {
                if (!(cogObj.hasKey(param[i * 2]) && cogObj.get(param[i * 2]).equalsIgnoreCase(param[i * 2 + 1]))) {
                    delete = false;
                }
            }
            if (delete) {
                remove_set.add(cogObj);
            }
        }
        pool.removeAll(remove_set);
        return (remove_set.size() > 0);
    }

    /**
     * Return i'th item.
     * 
     * @param i
     *            Item index
     * @return Item at the i'th index
     */
    public CoGObject get(int i) {
        if (i >= 0 && i < pool.size()) return pool.get(i);
        return null;
    }

    public CoGSet get(String attribute, String value) {
        CoGSet return_set = new CoGSet();
        for (CoGObject cogObj : pool) {
            if (cogObj.hasKey(attribute) && cogObj.get(attribute).equalsIgnoreCase(value)) return_set.add(cogObj);
        }
        return return_set;
    }

    public CoGSet getFromAttSet(String attribute, String value) {
        CoGSet return_set = new CoGSet();
        for (CoGObject cogObj : pool) {
            if (cogObj.hasKey(attribute) && cogObj.checkAttSet(attribute, value)) return_set.add(cogObj);
        }
        return return_set;
    }

    /**
     * Find objects that have attributes with the specified values
     * 
     * @param param
     *            Attribute names and values
     * @return Set of objects
     */
    public CoGSet getObjects(String... param) {
        if (param.length % 2 != 0) {
            log.error("Internal error (wrong number of function parameters).");
            return null;
        }
        CoGSet return_set = new CoGSet();
        for (CoGObject cogObj : pool) {
            boolean found = true;
            for (int i = 0; i < param.length / 2; i++) {
                if (!(cogObj.hasKey(param[i * 2]) && cogObj.get(param[i * 2]).equalsIgnoreCase(param[i * 2 + 1]))) {
                    found = false;
                }
            }
            if (found) {
                return_set.add(cogObj);
            }
        }
        return return_set;
    }

    /**
     * Find first object with desired attribute value.
     * @param attribute : attribute name for search
     * @param value : attribute value for search
     *
     * @return CoG object
     */
    public CoGObject find(String attribute, String value) {
        for (CoGObject cogObj : pool) {
            if (cogObj.hasKey(attribute) && cogObj.get(attribute).equalsIgnoreCase(value)) return cogObj;
        }
        return null;
    }

    /**
     * Find object that have attributes with the specified values
     * 
     * @param param
     *            Attribute names and values
     * @return Object
     */
    public CoGObject findObject(String... param) {
        List<CoGObject> remove_set = new ArrayList<CoGObject>();
        if (param.length % 2 != 0) {
            log.error("Internal error (wrong number of function parameters).");
            return null;
        }
        for (CoGObject cogObj : pool) {
            boolean found = true;
            for (int i = 0; i < param.length / 2; i++) {
                if (!(cogObj.hasKey(param[i * 2]) && cogObj.get(param[i * 2]).equalsIgnoreCase(param[i * 2 + 1]))) {
                    found = false;
                }
            }
            if (found) {
                return cogObj;
            }
        }
        return null;
    }

    public Iterator<CoGObject> iterator() {
        return pool.iterator();
    }

    /**
     * Return all items in the set.
     * 
     * @return Items of the set.
     */
    public List<CoGObject> getList() {
        return pool;
    }

    public int size() {
        return pool.size();
    }

    /**
     * Remove the set.
     */
    public void removeSet() {
        pool.clear();
    }

    private String attribute(String s) {
        return (s + ATTRIBUTE_SEPARATOR);
    }

    private String attributeValue(String s, String v) {
        return (s + ATTRIBUTE_SEPARATOR + " " + v);
    }

    public CoGObject[] getListAsArray() {
        CoGObject arr[] = new CoGObject[pool.size()];
        pool.toArray(arr);
        return arr;
    }

    /*******************************************************************
     * utility functions
     ******************************************************************/
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (CoGObject obj : pool) {
            sbuf.append(obj.toString());
            sbuf.append("-------------------------------------------\n");
        }
        return sbuf.toString();
    }

    /**
     * Load a set from the file. Assumes that the metadata is at the top of the
     * file.
     * 
     * FIXME this way the files are being opened 2 times. 1st reading the
     * metadata, 2nd reading the objects
     * 
     * TODO reading json, xml
     * 
     * @param filename
     *            File to load the set from.
     * @return Reference to itself.
     */
    public CoGSet loadFromFile(String filename, String format) throws IOException {
        List<CoGObject> list = CoGObjectsUtil.readObject(filename, format);
        if (list != null) {
            pool.addAll(list);
            this.filename = filename;
        }
        return this;
    }

    /**
     * Save the set to a file.
     */
    public void saveToFile() throws IOException {
        String path;
        try {
            path = CoGSetUtil.getFullPath(filename);
        } catch (Exception e) {
            path = CoGSetUtil.getDefaultDirectory() + CoGSetUtil.generateFilename(getMeta("label"), getMeta("id"), ASCII);
        }
        saveToFile(path, filetype, true);
    }

    /**
     * Save the set to a different file.
     * 
     * @param filename
     *            Name of the new file.
     * @param ftype
     *            One of the following: ASCII, JSON, XML.
     * @param overwrite
     *            Overwrite the file, if it already exists.
     */
    public void saveToFile(String filename, String ftype, boolean overwrite) throws IOException {
        log.info(filename);
        if (!ftype.equalsIgnoreCase(ASCII)) {
            throw new IOException("WRITING " + ftype + " IS NOT IMPLEMENTED");
        }
        File ff = new File(filename);
        if (!ff.isAbsolute()) {
            ff = new File(CoGSetUtil.getFullPath(filename));
        }
        if (ff.exists() && !overwrite) throw new IOException("File already exists");
        FileWriter fstream = new FileWriter(ff);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(toString());
        out.close();
    }

    public static CoGSet[] forLabel(String label) throws Exception {
        Properties p = new Properties();
        p.load(Path.getRelativeInputStream("cyberaide.properties"));
        String path = Path.CYBERAIDEHOME + Path.fs + p.getProperty("cyberaide.user.sets", "sets") + Path.fs;
        ArrayList<String> names = new ArrayList<String>();
        for (String file : new File(path).list()) if (file.startsWith(label)) names.add(file);
        ArrayList<CoGSet> sets = new ArrayList<CoGSet>();
        for (String name : names) try {
            sets.add(new CoGSet().loadFromFile(path + name, CoGSet.ASCII));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return (CoGSet[]) sets.toArray();
    }

    /**
     * Returns true if file containing the named set exists in the user home
     * directory.
     */
    public static boolean exists(String name) {
        return true;
    }

    /**
     * Return the meta data.
     * 
     * @param attribute
     *            Meta data field.
     * @return Requested meta data or null, if meta data does not exist.
     */
    public String getMeta(String attribute) {
        return meta.get(attribute);
    }

    /**
     * Set meta data.
     * 
     * @param attribute
     *            Meta data field.
     * @param value
     *            Meta data value.
     */
    public void setMeta(String attribute, String value) {
        meta.set(attribute, value);
    }

    /**
     * Parse metadata from the CoGObject.
     * 
     * @param o
     *            CoGObject hloding metadata for the set.
     */
    protected void parseMeta(CoGObject o) {
        for (String key : o.getKeys()) {
            setMeta(key, o.get(key));
        }
    }

    public boolean hasMeta(String attribute) {
        return getMeta(attribute) != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toAscii() {
        return toAscii(null);
    }

    public String toAscii(String param) {
        if (param != null) {
            String[] args = param.split(",");
            StringBuffer sbuf = new StringBuffer();
            for (CoGObject obj : pool) {
                sbuf.append(obj.toString(args));
                sbuf.append("\n \n");
            }
            return sbuf.toString();
        } else {
            StringBuffer sbuf = new StringBuffer();
            for (CoGObject obj : pool) {
                sbuf.append(obj.toString());
                sbuf.append("\n \n");
            }
            return sbuf.toString();
        }
    }

    public String toJson() {
        return toJson(null);
    }

    public String toJson(String param) {
        try {
            if (param != null) {
                String[] args = param.split(",");
                StringBuffer sbuf = new StringBuffer();
                for (CoGObject obj : pool) {
                    sbuf.append(obj.toJSONObject(args).toString());
                    sbuf.append("\n \n");
                }
                return sbuf.toString();
            } else {
                StringBuffer sbuf = new StringBuffer();
                for (CoGObject obj : pool) {
                    sbuf.append(obj.toJSONObject().toString());
                    sbuf.append("\n \n");
                }
                return sbuf.toString();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String toXml() {
        return toXml(null);
    }

    public String toXml(String param) {
        try {
            if (param != null) {
                String[] args = param.split(",");
                return XML.toString(CoGPrinter.toJSON(getList(), args));
            } else {
                return XML.toString(CoGPrinter.toJSON(getList()));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Load CoGSet from data file.
     * @param filename : data filename
     */
    public void loadDataFile(String filename) {
        try {
            this.clear();
            this.loadFromFile(filename, this.ASCII);
        } catch (Exception e) {
            log.error("Can not open file: " + e.getMessage());
            return;
        }
    }

    /**
     * Save CoGSet to data file.
     * @param filename : data filename
     */
    public void saveDataFile(String filename) {
        try {
            Properties p = new Properties();
            p.load(Path.getRelativeInputStream("cyberaide.properties"));
            String path = Path.CURDIR + p.getProperty("cyberaide.objects", "etc/objects/data/");
            this.saveToFile(path + filename + ".txt", this.ASCII, true);
        } catch (Exception e) {
            log.error("Can not save data file: " + e.getMessage());
            return;
        }
    }
}
