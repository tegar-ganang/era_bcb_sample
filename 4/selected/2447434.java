package com.bukkit.epicsaga.EpicZones;

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

public class EpicZonesConfig extends Configuration {

    private static final Yaml yaml;

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    private File file;

    public int mapRadius = 0;

    public boolean defaultEnter;

    public boolean defaultBuild;

    public boolean defaultDestroy;

    public EpicZonesConfig(File file) {
        super(file);
        this.file = file;
        if (file == null) throw new IllegalArgumentException("file cannot be null");
    }

    public void setDefaults() {
        mapRadius = 1000;
        defaultEnter = true;
        defaultBuild = true;
        defaultDestroy = true;
    }

    @Override
    public void load() {
        setDefaults();
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        if (!file.exists()) {
            try {
                file.createNewFile();
                save();
            } catch (IOException e) {
            }
        } else {
            super.load();
            mapRadius = getInt("mapRadius", mapRadius);
            defaultEnter = getBoolean("defaultEnter", true);
            defaultBuild = getBoolean("defaultBuild", true);
            defaultDestroy = getBoolean("defaultDestroy", true);
        }
    }

    /**
     * Save settings to config file. File errors are ignored like load.
     */
    public void save() {
        FileOutputStream stream;
        BufferedWriter writer;
        root.put("mapRadius", mapRadius);
        root.put("defaultEnter", defaultEnter);
        root.put("defaultBuild", defaultBuild);
        root.put("defaultDestroy", defaultDestroy);
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
        }
    }
}
