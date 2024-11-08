package com.epicsagaonline.bukkit.EpicZones;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.HashMap;

@SuppressWarnings({ "ResultOfMethodCallIgnored" })
public class Config {

    private static File file;

    public static boolean enableRadius;

    public static boolean enableHeroChat;

    public static boolean globalZoneDefaultBuild;

    public static boolean globalZoneDefaultDestroy;

    public static boolean globalZoneDefaultEnter;

    public static int zoneTool = 280;

    public static String language = "EN_US";

    public static boolean enableSpout;

    private static final String CONFIG_FILE = "config.yml";

    public static void Init(EpicZones plugin) {
        enableRadius = true;
        enableHeroChat = false;
        globalZoneDefaultBuild = true;
        globalZoneDefaultDestroy = true;
        globalZoneDefaultEnter = true;
        zoneTool = 280;
        language = "EN_US";
        enableSpout = true;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder() + File.separator + CONFIG_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.Write(e.getMessage());
            }
            Save();
        }
    }

    @SuppressWarnings("unchecked")
    public static void Load(EpicZones plugin) {
        Init(plugin);
        if (file.exists()) {
            Yaml yaml = new Yaml();
            HashMap<String, Object> root;
            FileInputStream stream;
            try {
                stream = new FileInputStream(file);
                root = (HashMap<String, Object>) yaml.load(stream);
                enableRadius = Util.getBooleanValueFromHashSet("enableRadius", root);
                enableHeroChat = Util.getBooleanValueFromHashSet("enableHeroChat", root);
                if (Util.getObjectValueFromHashSet("globalZoneDefaultAllow", root) == null) {
                    globalZoneDefaultBuild = Util.getBooleanValueFromHashSet("globalZoneDefaultBuild", root);
                    globalZoneDefaultDestroy = Util.getBooleanValueFromHashSet("globalZoneDefaultDestroy", root);
                    globalZoneDefaultEnter = Util.getBooleanValueFromHashSet("globalZoneDefaultEnter", root);
                } else {
                    globalZoneDefaultBuild = Util.getBooleanValueFromHashSet("globalZoneDefaultAllow", root);
                    globalZoneDefaultDestroy = globalZoneDefaultBuild;
                    globalZoneDefaultEnter = globalZoneDefaultBuild;
                }
                zoneTool = Util.getIntegerValueFromHashSet("zoneTool", root);
                language = Util.getStringValueFromHashSet("language", root);
                enableSpout = Util.getBooleanValueFromHashSet("enableSpout", root);
            } catch (FileNotFoundException e) {
                Log.Write(e.getMessage());
            }
        }
    }

    public static void Save() {
        Yaml yaml = new Yaml();
        HashMap<String, Object> root = new HashMap<String, Object>();
        FileOutputStream stream;
        BufferedWriter writer;
        root.put("enableRadius", enableRadius);
        root.put("enableHeroChat", enableHeroChat);
        root.put("globalZoneDefaultBuild", globalZoneDefaultBuild);
        root.put("globalZoneDefaultDestroy", globalZoneDefaultDestroy);
        root.put("globalZoneDefaultEnter", globalZoneDefaultEnter);
        root.put("zoneTool", zoneTool);
        root.put("language", language);
        root.put("enableSpout", enableSpout);
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
            Log.Write(e.getMessage());
        }
    }
}
