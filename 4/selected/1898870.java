package com.bukkit.epicsaga;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.util.config.Configuration;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

/**
 * A writable configuration, won't be needed soon, as upstream Configuration 
 * implements it.
 * 
 * @author _sir_maniac
 *
 */
public class WritableConfiguration extends Configuration {

    protected Yaml yaml;

    private File file;

    public WritableConfiguration(File file) {
        this(file, new NullRepresenter());
    }

    protected WritableConfiguration(File file, Representer rep) {
        super(file);
        this.file = file;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(rep, options);
    }

    /**
	 * Saves the configuration to disk.
	 * 
	 * All errors are clobbered.
	 * 
	 * @return
	 */
    public boolean save() {
        FileOutputStream stream;
        BufferedWriter writer;
        try {
            stream = new FileOutputStream(file);
            stream.getChannel().truncate(0);
            writer = new BufferedWriter(new OutputStreamWriter(stream));
            try {
                writer.write(yaml.dump(root));
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
	 * Returns a map representing path adding new maps along the way if 
	 *  necessary.  If any node in the path exists and isn't a map, null is 
	 *  returned.
	 * 
	 * @param path path to node (dot notation)
	 * @return null or map
	 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getPath(String path) {
        Map<String, Object> map = root;
        Map<String, Object> ret;
        String[] parts;
        try {
            while (path.contains(".")) {
                parts = path.split("\\.", 2);
                ret = (Map<String, Object>) map.get(parts[0]);
                if (ret == null) {
                    ret = new HashMap<String, Object>();
                    map.put(parts[0], ret);
                }
                map = ret;
                path = parts[1];
            }
            ret = (Map<String, Object>) map.get(path);
            if (ret == null) {
                ret = new HashMap<String, Object>();
                map.put(path, ret);
            }
            return ret;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
	 * Set property at given path.  
	 * 
	 * NOTE: Won't be necessary soon, since bukkit defines this later.
	 * 
	 * @param path path to node (dot notation)
	 * @param val the value to set
	 */
    public void setProperty(String path, Object val) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String mapPath = path.substring(0, index);
            String key = path.substring(index + 1);
            Map<String, Object> map = getPath(mapPath);
            map.put(key, val);
        } else {
            root.put(path, val);
        }
    }

    public void removeProperty(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            String mapPath = path.substring(0, index);
            String key = path.substring(index + 1);
            Map<String, Object> map = getPath(mapPath);
            map.remove(key);
        } else {
            root.remove(path);
        }
    }

    protected static class NullRepresenter extends Representer {

        public NullRepresenter() {
            super();
            this.nullRepresenter = new RepresentNull();
        }

        private class RepresentNull implements Represent {

            public Node representData(Object data) {
                return representScalar(Tag.NULL, "");
            }
        }
    }
}
